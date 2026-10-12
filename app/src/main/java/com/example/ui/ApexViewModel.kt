package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.engine.*
import com.example.model.*
import com.example.notification.MuscleRecoveryNotificationHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

class ApexViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application, viewModelScope)
    private val repository = ApexRepository(db.apexDao())
    val audioHaptic = AudioHapticFeedback(application)

    init {
        MuscleRecoveryNotificationHelper.createNotificationChannel(application)
        viewModelScope.launch {
            db.apexDao().deleteHomeGymProfile()
            repository.cleanupEmptyCancelledSessions()
            repository.ensureProgressiveOverloadStructureSeeded()

            allCompletedSets.collect { sets ->
                val summary = MuscleRecoveryEngine.computeRecoverySummary(sets)
                MuscleRecoveryNotificationHelper.scheduleMuscleRecoveryAlarms(application, summary.statuses)
            }
        }
    }

    // Data streams
    val activeMesocycle: StateFlow<MesocycleEntity?> = repository.activeMesocycle
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMesocycles: StateFlow<List<MesocycleEntity>> = repository.allMesocycles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMesocycleWithStructure: StateFlow<MesocycleWithFullStructure?> = repository.activeMesocycleWithStructure
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allMesocyclesWithStructure: StateFlow<List<MesocycleWithFullStructure>> = repository.allMesocyclesWithStructure
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeMesocycleWeekPlans: StateFlow<List<MesocycleWeekPlanEntity>> = activeMesocycle
        .flatMapLatest { meso ->
            if (meso != null) repository.getWeekPlansForMesocycle(meso.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allProgressiveOverloadLogs: StateFlow<List<ProgressiveOverloadLogEntity>> = repository.allProgressiveOverloadLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedSessionsWithDetails: StateFlow<List<WorkoutSessionWithDetails>> = repository.completedSessionsWithDetails
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val exercisesWithOverloadHistory: StateFlow<List<ExerciseWithOverloadHistory>> = repository.exercisesWithOverloadHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<ExerciseEntity>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestReadiness: StateFlow<ReadinessCheckinEntity?> = repository.latestReadiness
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val readinessHistory: StateFlow<List<ReadinessCheckinEntity>> = repository.readinessHistory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bodyMeasurements: StateFlow<List<BodyMeasurementEntity>> = repository.allBodyMeasurements
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val gymProfiles: StateFlow<List<GymProfileEntity>> = repository.allGymProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedGymProfile: StateFlow<GymProfileEntity?> = repository.selectedGymProfile
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val userVolumeConfig: StateFlow<UserVolumeConfigEntity?> = repository.userVolumeConfig
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allCompletedSets: StateFlow<List<WorkoutSetWithExerciseInfo>> = repository.allCompletedSetsWithExercise
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSessions: StateFlow<List<WorkoutSessionEntity>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Daily Micro-Goals state (runs in background analyzing regular exercises, tonnage & consistency)
    private val _manualCheckedGoals = MutableStateFlow<Set<String>>(emptySet())
    val dailyGoals: StateFlow<DailyGoalsState> = combine(
        allSessions,
        allCompletedSets,
        _manualCheckedGoals
    ) { sessions, sets, checked ->
        DailyGoalsEngine.computeDailyGoals(
            sessions = sessions,
            completedSets = sets,
            manualCheckedGoalIds = checked
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        DailyGoalsEngine.computeDailyGoals(emptyList(), emptyList(), emptySet())
    )

    fun toggleDailyGoal(goalId: String) {
        val current = _manualCheckedGoals.value
        _manualCheckedGoals.value = if (current.contains(goalId)) current - goalId else current + goalId
    }

    val gamificationState: StateFlow<AthleteGamificationState> = combine(
        allSessions,
        allCompletedSets,
        allExercises,
        dailyGoals
    ) { sessions, sets, exercises, goals ->
        val customCount = exercises.count { it.instructionsUk == "Власна вправа" }
        GamificationEngine.calculateGamificationState(
            sessions = sessions,
            completedSets = sets,
            customExercisesCount = customCount,
            dailyGoalsCompleted = goals.completedCount
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        GamificationEngine.calculateGamificationState(emptyList(), emptyList(), 0, 0)
    )

    // User Nickname persistence
    private val userPrefs = application.getSharedPreferences("apex_user_prefs", android.content.Context.MODE_PRIVATE)
    private val _userNickname = MutableStateFlow(userPrefs.getString("user_nickname", "Атлет") ?: "Атлет")
    val userNickname: StateFlow<String> = _userNickname.asStateFlow()

    fun setUserNickname(nickname: String) {
        val clean = nickname.trim().ifEmpty { "Атлет" }
        _userNickname.value = clean
        userPrefs.edit().putString("user_nickname", clean).apply()
    }

    // Music Integration Persistence (YouTube Music & Spotify)
    private val _preferredMusicPlatform = MutableStateFlow(userPrefs.getString("preferred_music_platform", "YOUTUBE_MUSIC") ?: "YOUTUBE_MUSIC")
    val preferredMusicPlatform: StateFlow<String> = _preferredMusicPlatform.asStateFlow()

    fun setPreferredMusicPlatform(platform: String) {
        val clean = platform.trim().ifEmpty { "YOUTUBE_MUSIC" }
        _preferredMusicPlatform.value = clean
        userPrefs.edit().putString("preferred_music_platform", clean).apply()
    }

    private val _customMusicPlaylistUrl = MutableStateFlow(userPrefs.getString("custom_music_playlist_url", "") ?: "")
    val customMusicPlaylistUrl: StateFlow<String> = _customMusicPlaylistUrl.asStateFlow()

    fun setCustomMusicPlaylistUrl(url: String) {
        val clean = url.trim()
        _customMusicPlaylistUrl.value = clean
        userPrefs.edit().putString("custom_music_playlist_url", clean).apply()
    }

    private val _customSpotifyPlaylistUrl = MutableStateFlow(userPrefs.getString("custom_spotify_playlist_url", "") ?: "")
    val customSpotifyPlaylistUrl: StateFlow<String> = _customSpotifyPlaylistUrl.asStateFlow()

    fun setCustomSpotifyPlaylistUrl(url: String) {
        val clean = url.trim()
        _customSpotifyPlaylistUrl.value = clean
        userPrefs.edit().putString("custom_spotify_playlist_url", clean).apply()
    }

    // Athlete RPG Character state
    val athleteCharacter: StateFlow<AthleteCharacter> = combine(
        allSessions,
        allCompletedSets,
        gamificationState,
        _userNickname
    ) { sessions, sets, game, nickname ->
        CharacterEngine.computeCharacter(
            sessions = sessions,
            completedSets = sets,
            streak = game.currentStreak,
            nickname = nickname
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CharacterEngine.computeCharacter(emptyList(), emptyList(), 0, _userNickname.value)
    )

    // Active mesocycle sessions
    val currentMesocycleSessions: StateFlow<List<WorkoutSessionEntity>> = activeMesocycle
        .flatMapLatest { meso ->
            if (meso != null) repository.getSessionsForMesocycle(meso.id)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Calculated volume landmarks based on user config & completed weekly sets
    val volumeLandmarks: StateFlow<List<VolumeLandmarkData>> = combine(
        userVolumeConfig,
        allCompletedSets
    ) { config, sets ->
        repository.calculateWeeklyVolumeLandmarks(config, sets)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Muscle Recovery & Readiness State (48-72h physiological interval)
    private val _recoveryNotificationsEnabled = MutableStateFlow(
        MuscleRecoveryNotificationHelper.isRecoveryNotificationEnabled(getApplication())
    )
    val recoveryNotificationsEnabled: StateFlow<Boolean> = _recoveryNotificationsEnabled.asStateFlow()

    fun setRecoveryNotificationsEnabled(enabled: Boolean) {
        _recoveryNotificationsEnabled.value = enabled
        MuscleRecoveryNotificationHelper.setRecoveryNotificationEnabled(getApplication(), enabled)
        if (enabled) {
            scheduleRecoveryAlarms()
        }
    }

    val muscleRecoverySummary: StateFlow<MuscleRecoverySummary> = allCompletedSets
        .map { sets -> MuscleRecoveryEngine.computeRecoverySummary(sets) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            MuscleRecoveryEngine.computeRecoverySummary(emptyList())
        )

    fun sendTestRecoveryNotification(muscle: MuscleGroup = MuscleGroup.CHEST) {
        MuscleRecoveryNotificationHelper.sendTestNotification(getApplication(), muscle)
    }

    fun scheduleRecoveryAlarms() {
        val summary = muscleRecoverySummary.value
        MuscleRecoveryNotificationHelper.scheduleMuscleRecoveryAlarms(getApplication(), summary.statuses)
    }

    // Active Workout state
    private val _activeWorkoutSessionId = MutableStateFlow<Long?>(null)
    val activeWorkoutSessionId: StateFlow<Long?> = _activeWorkoutSessionId.asStateFlow()

    private val _activeWorkoutExercisesWithSets = MutableStateFlow<List<WorkoutExerciseWithSets>>(emptyList())
    val activeWorkoutExercisesWithSets: StateFlow<List<WorkoutExerciseWithSets>> = _activeWorkoutExercisesWithSets.asStateFlow()

    private val _activeWorkoutElapsedSeconds = MutableStateFlow<Long>(0)
    val activeWorkoutElapsedSeconds: StateFlow<Long> = _activeWorkoutElapsedSeconds.asStateFlow()

    private var workoutTimerJob: Job? = null

    // Rest timer state backed by background service
    private val _preferredRestDuration = MutableStateFlow<Int>(90)
    val preferredRestDuration: StateFlow<Int> = _preferredRestDuration.asStateFlow()

    val restSecondsLeft: StateFlow<Int> = com.example.service.RestTimerManager.restSecondsLeft
    val restTotalSeconds: StateFlow<Int> = com.example.service.RestTimerManager.restTotalSeconds
    val isRestActive: StateFlow<Boolean> = com.example.service.RestTimerManager.isRestActive

    fun setPreferredRestDuration(seconds: Int) {
        _preferredRestDuration.value = seconds.coerceIn(45, 180)
    }

    // Dark theme override (default to true for Obsidian & Mint theme)
    val isDarkThemeOverride = MutableStateFlow<Boolean?>(true)

    // Progression advice cache for active workout exercises
    private val _progressionAdviceMap = MutableStateFlow<Map<String, DoubleProgressionEngine.ProgressionSuggestion>>(emptyMap())
    val progressionAdviceMap: StateFlow<Map<String, DoubleProgressionEngine.ProgressionSuggestion>> = _progressionAdviceMap.asStateFlow()

    // Analysis System: Internal Schedule, Trigger reactions, Inactivity notifications
    val internalSchedules: StateFlow<List<InternalScheduleEntity>> = db.apexDao().getAllInternalSchedules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWeeklySummary: StateFlow<WeeklySummaryEntity?> = db.apexDao().getLatestWeeklySummary()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _activeStartReactions = MutableStateFlow<List<com.example.analysis.StartReactionMessage>>(emptyList())
    val activeStartReactions: StateFlow<List<com.example.analysis.StartReactionMessage>> = _activeStartReactions.asStateFlow()

    private val _inactivityNotice = MutableStateFlow<String?>(null)
    val inactivityNotice: StateFlow<String?> = _inactivityNotice.asStateFlow()

    private var lastUserActivityTimestamp: Long = System.currentTimeMillis()

    fun dismissStartReaction(ruleId: String) {
        _activeStartReactions.value = _activeStartReactions.value.filter { it.ruleId != ruleId }
    }

    fun clearInactivityNotice() {
        _inactivityNotice.value = null
    }

    fun toggleDarkMode() {
        isDarkThemeOverride.value = when (isDarkThemeOverride.value) {
            true -> false
            false -> true
            null -> false
        }
    }

    fun startQuickExerciseWorkout(exerciseName: String) {
        viewModelScope.launch {
            val cleanName = exerciseName.trim().ifEmpty { "Жим лежачи" }
            val allEx = db.apexDao().getAllExercises().firstOrNull() ?: emptyList()
            val matched = allEx.find {
                it.name.equals(cleanName, ignoreCase = true) ||
                    it.name.contains(cleanName, ignoreCase = true) ||
                    cleanName.contains(it.name, ignoreCase = true)
            }
            val canonicalName = matched?.name ?: when {
                cleanName.contains("Жим лежачи", ignoreCase = true) -> "Жим штанги лежачи"
                cleanName.contains("Присідання", ignoreCase = true) -> "Присідання зі штангою"
                cleanName.contains("Тяга блока", ignoreCase = true) -> "Вертикальна тяга верхнього блоку"
                else -> cleanName
            }
            val muscleStr = matched?.muscleGroup ?: when {
                canonicalName.contains("Жим", ignoreCase = true) -> "CHEST"
                canonicalName.contains("Присідання", ignoreCase = true) -> "QUADS"
                canonicalName.contains("Тяга", ignoreCase = true) -> "BACK"
                else -> "CHEST"
            }
            val muscleEnum = runCatching { MuscleGroup.valueOf(muscleStr) }.getOrDefault(MuscleGroup.CHEST)
            val equipEnum = matched?.equipment?.let {
                runCatching { EquipmentType.valueOf(it) }.getOrNull()
            } ?: EquipmentType.BARBELL

            val lastWeight = allCompletedSets.value
                .filter { it.exerciseName.equals(canonicalName, ignoreCase = true) }
                .maxByOrNull { it.timestamp }?.weightKg ?: when {
                canonicalName.contains("Жим", ignoreCase = true) -> 60.0
                canonicalName.contains("Присідання", ignoreCase = true) -> 80.0
                else -> 50.0
            }

            startTodayWorkout(
                title = "Силове тренування",
                selectedMuscles = listOf(muscleEnum),
                exercises = listOf(
                    PlannedExercise(
                        exerciseName = canonicalName,
                        muscleGroup = muscleStr,
                        equipment = equipEnum,
                        baseWeightKg = lastWeight,
                        baseRepsMin = matched?.defaultTargetRepsMin ?: 8,
                        baseRepsMax = matched?.defaultTargetRepsMax ?: 12,
                        isPriority = true
                    )
                )
            )
        }
    }

    // Workout operations
    fun startWorkout(sessionId: Long) {
        _activeWorkoutSessionId.value = sessionId
        _activeWorkoutElapsedSeconds.value = 0
        lastUserActivityTimestamp = System.currentTimeMillis()
        workoutTimerJob?.cancel()
        workoutTimerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000)
                _activeWorkoutElapsedSeconds.value += 1

                // Stage 1 Auto-completion: if no activity for > 25 minutes, auto-finish as interrupted
                val inactiveMs = System.currentTimeMillis() - lastUserActivityTimestamp
                if (inactiveMs > 25 * 60 * 1000L && _activeWorkoutSessionId.value == sessionId) {
                    autoFinishWorkoutOnInactivity(sessionId)
                    break
                }
            }
        }
        loadActiveWorkoutData(sessionId)

        // Stage 4 & 5: Classify start & trigger reaction rules
        viewModelScope.launch {
            val allCompleted = db.apexDao().getAllSessionsOnce().filter { it.isCompleted }
            val schedules = db.apexDao().getInternalSchedulesOnce()
            val nowMs = System.currentTimeMillis()
            val classification = com.example.analysis.WorkoutAnalysisEngine.classifyStart(nowMs, allCompleted, schedules)

            // Collect muscles involved in current workout
            val sessionExercises = db.apexDao().getExercisesForSessionOnce(sessionId)
            val currentMuscles = sessionExercises.map { it.muscleGroup }.distinct()

            // Collect muscles trained in past 48 hours
            val twoDaysAgo = nowMs - (48L * 3600_000L)
            val recentCompleted48h = allCompleted.filter { it.dateTimestamp >= twoDaysAgo }
            val trainedMuscles48h = mutableListOf<String>()
            for (rc in recentCompleted48h) {
                val exList = db.apexDao().getExercisesForSessionOnce(rc.id)
                trainedMuscles48h.addAll(exList.map { it.muscleGroup })
            }

            val acuteChronic = com.example.analysis.WorkoutAnalysisEngine.calculateAcuteChronicRatio(allCompleted, nowMs)
            val streak = com.example.analysis.WorkoutAnalysisEngine.calculateScheduleStreak(allCompleted, schedules, nowMs)
            val lastSession = allCompleted.maxByOrNull { it.dateTimestamp }
            val daysSinceLast = if (lastSession != null) ((nowMs - lastSession.dateTimestamp) / 86_400_000L).toInt() else 0

            val cal = java.util.Calendar.getInstance()
            val startContext = com.example.analysis.StartContext(
                currentTimeMs = nowMs,
                currentWeekday = cal.get(java.util.Calendar.DAY_OF_WEEK),
                currentMinutesOfDay = cal.get(java.util.Calendar.HOUR_OF_DAY) * 60 + cal.get(java.util.Calendar.MINUTE),
                classification = classification,
                streak = streak,
                daysSinceLastWorkout = daysSinceLast,
                recentTrainedMuscles48h = trainedMuscles48h.distinct(),
                currentWorkoutMuscles = currentMuscles,
                acuteChronicRatio = acuteChronic,
                isScheduleFormed = com.example.analysis.WorkoutAnalysisEngine.isScheduleFormed(allCompleted, nowMs)
            )

            val reactions = com.example.analysis.WorkoutAnalysisEngine.evaluateStartRules(startContext)
            _activeStartReactions.value = reactions

            // Save trigger logs
            for (r in reactions) {
                db.apexDao().insertTriggerLog(
                    TriggerLogEntity(
                        ruleId = r.ruleId,
                        messageText = "${r.title}: ${r.body}",
                        userReaction = "VIEWED"
                    )
                )
            }

            // Save startClassification to session
            val session = db.apexDao().getSessionById(sessionId)
            if (session != null) {
                db.apexDao().updateSession(session.copy(startClassification = classification.name))
            }
        }
    }

    private fun autoFinishWorkoutOnInactivity(sessionId: Long) {
        viewModelScope.launch {
            workoutTimerJob?.cancel()
            val duration = _activeWorkoutElapsedSeconds.value
            val exercises = db.apexDao().getExercisesForSessionOnce(sessionId)
            var totalTonnage = 0.0
            var completedCount = 0

            for (we in exercises) {
                val sets = db.apexDao().getSetsForExerciseOnce(we.id)
                for (set in sets) {
                    if (set.isCompleted && !set.isWarmup) {
                        totalTonnage += set.weightKg * set.reps
                        completedCount++
                    }
                }
            }

            if (completedCount == 0 || totalTonnage <= 0.0) {
                deleteSession(sessionId)
            } else {
                val session = db.apexDao().getSessionById(sessionId)
                if (session != null) {
                    db.apexDao().updateSession(
                        session.copy(
                            isCompleted = true,
                            isInterrupted = true,
                            durationSeconds = duration,
                            totalTonnageKg = totalTonnage,
                            notes = (session.notes + " [Автозавершено через бездіяльність]").trim()
                        )
                    )
                    repository.recordSessionProgressiveOverload(sessionId)
                }
            }
            cancelRestTimer()
            _activeWorkoutSessionId.value = null
            _activeWorkoutExercisesWithSets.value = emptyList()
            _activeWorkoutElapsedSeconds.value = 0
            _inactivityNotice.value = "Сесію автозавершено та коректно збережено через відсутність активності понад 25 хвилин."
        }
    }

    private fun loadActiveWorkoutData(sessionId: Long) {
        viewModelScope.launch {
            val exercises = db.apexDao().getExercisesForSessionOnce(sessionId)
            val fullList = mutableListOf<WorkoutExerciseWithSets>()
            val adviceMap = mutableMapOf<String, DoubleProgressionEngine.ProgressionSuggestion>()

            for (we in exercises) {
                val sets = db.apexDao().getSetsForExerciseOnce(we.id)
                fullList.add(WorkoutExerciseWithSets(we, sets))

                // Fetch double progression advice for this exercise
                val allEx = db.apexDao().getAllExercises().firstOrNull() ?: emptyList()
                val exMeta = allEx.find { it.name == we.exerciseName }
                val equip = exMeta?.equipment?.let {
                    runCatching { EquipmentType.valueOf(it) }.getOrNull()
                } ?: EquipmentType.BARBELL

                val advice = repository.getDoubleProgressionAdvice(
                    exerciseName = we.exerciseName,
                    equipment = equip,
                    targetMin = sets.firstOrNull()?.targetRepsMin ?: 8,
                    targetMax = sets.firstOrNull()?.targetRepsMax ?: 12
                )
                adviceMap[we.exerciseName] = advice
            }
            _activeWorkoutExercisesWithSets.value = fullList
            _progressionAdviceMap.value = adviceMap
        }
    }

    fun completeSet(setId: Long, rir: Int, weightKg: Double, reps: Int) {
        lastUserActivityTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            val currentList = _activeWorkoutExercisesWithSets.value
            for (item in currentList) {
                val match = item.sets.find { it.id == setId }
                if (match != null) {
                    val e1rm = if (reps > 0 && !match.isWarmup) {
                        if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)
                    } else 0.0
                    val updated = match.copy(
                        weightKg = weightKg,
                        reps = reps,
                        rir = rir,
                        rpe = (10 - rir).coerceIn(5, 10).toFloat(),
                        estimated1RmKg = e1rm,
                        tonnageKg = if (!match.isWarmup) weightKg * reps else 0.0,
                        isCompleted = true,
                        timestamp = System.currentTimeMillis()
                    )
                    db.apexDao().updateSet(updated)
                    com.example.service.SetCompleteAudioHelper.playSetCompleteSound(getApplication())
                    // Auto-start rest timer using user configured rest time (60-120s)
                    startRestTimer(_preferredRestDuration.value)
                    break
                }
            }
            _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
        }
    }

    fun uncompleteSet(setId: Long) {
        lastUserActivityTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            val currentList = _activeWorkoutExercisesWithSets.value
            for (item in currentList) {
                val match = item.sets.find { it.id == setId }
                if (match != null) {
                    val updated = match.copy(isCompleted = false)
                    db.apexDao().updateSet(updated)
                    break
                }
            }
            _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
        }
    }

    fun updateSetParameters(setId: Long, weightKg: Double, reps: Int, rir: Int) {
        lastUserActivityTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            val currentList = _activeWorkoutExercisesWithSets.value
            for (item in currentList) {
                val match = item.sets.find { it.id == setId }
                if (match != null) {
                    val e1rm = if (reps > 0 && !match.isWarmup) {
                        if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)
                    } else 0.0
                    val updated = match.copy(
                        weightKg = weightKg,
                        reps = reps,
                        rir = rir,
                        rpe = (10 - rir).coerceIn(5, 10).toFloat(),
                        estimated1RmKg = e1rm,
                        tonnageKg = if (!match.isWarmup) weightKg * reps else 0.0
                    )
                    db.apexDao().updateSet(updated)
                    break
                }
            }
            _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
        }
    }

    fun addSetToExercise(workoutExerciseId: Long) {
        lastUserActivityTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            val currentSets = db.apexDao().getSetsForExerciseOnce(workoutExerciseId)
            val lastSet = currentSets.lastOrNull()
            val newSet = WorkoutSetEntity(
                workoutExerciseId = workoutExerciseId,
                setNumber = currentSets.size + 1,
                weightKg = lastSet?.weightKg ?: 60.0,
                reps = lastSet?.reps ?: 10,
                targetRepsMin = lastSet?.targetRepsMin ?: 8,
                targetRepsMax = lastSet?.targetRepsMax ?: 12,
                rir = lastSet?.rir ?: 2,
                isWarmup = false,
                isCompleted = false
            )
            db.apexDao().insertSet(newSet)
            _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
        }
    }

    fun removeSet(setId: Long) {
        lastUserActivityTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            db.apexDao().deleteSetById(setId)
            _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
        }
    }

    fun substituteExercise(workoutExerciseId: Long, newExercise: ExerciseEntity) {
        lastUserActivityTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            repository.substituteWorkoutExercise(workoutExerciseId, newExercise)
            _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
        }
    }

    fun addCustomExerciseToWorkout(
        sessionId: Long,
        exerciseName: String,
        muscleGroup: String,
        setsCount: Int = 3,
        initialWeight: Double = 40.0,
        initialReps: Int = 10
    ) {
        lastUserActivityTimestamp = System.currentTimeMillis()
        viewModelScope.launch {
            val cleanName = exerciseName.trim()
            if (cleanName.isEmpty()) return@launch

            val existingSessionExercises = db.apexDao().getExercisesForSessionOnce(sessionId)
            val orderIdx = existingSessionExercises.size

            // Ensure it exists in general exercises catalogue for future suggestions
            val allEx = db.apexDao().getAllExercises().firstOrNull() ?: emptyList()
            var matchedEx = allEx.find { it.name.equals(cleanName, ignoreCase = true) }
            if (matchedEx == null) {
                val newExId = db.apexDao().insertExercise(
                    ExerciseEntity(
                        name = cleanName,
                        muscleGroup = muscleGroup,
                        equipment = EquipmentType.BARBELL.name,
                        movementPattern = MovementPattern.HORIZONTAL_PRESS.name,
                        instructionsUk = "Власна вправа",
                        jointSafetyUk = "Підконтрольна техніка"
                    )
                )
                matchedEx = db.apexDao().getExerciseById(newExId)
            }

            val workoutExId = db.apexDao().insertWorkoutExercise(
                WorkoutExerciseEntity(
                    sessionId = sessionId,
                    exerciseId = matchedEx?.id ?: 0L,
                    exerciseName = cleanName,
                    muscleGroup = muscleGroup,
                    orderIndex = orderIdx,
                    notes = ""
                )
            )

            val sets = (1..setsCount.coerceIn(1, 10)).map { num ->
                WorkoutSetEntity(
                    workoutExerciseId = workoutExId,
                    setNumber = num,
                    weightKg = initialWeight,
                    reps = initialReps,
                    targetRepsMin = 8,
                    targetRepsMax = 12,
                    rir = 2,
                    isWarmup = false,
                    isCompleted = false
                )
            }
            db.apexDao().insertSets(sets)

            loadActiveWorkoutData(sessionId)
        }
    }

    fun renameWorkoutExercise(workoutExerciseId: Long, newName: String, newMuscleGroup: String? = null) {
        viewModelScope.launch {
            val cleanName = newName.trim()
            if (cleanName.isEmpty()) return@launch

            val currentList = _activeWorkoutExercisesWithSets.value
            val match = currentList.find { it.exercise.id == workoutExerciseId }?.exercise
            if (match != null) {
                val updated = match.copy(
                    exerciseName = cleanName,
                    muscleGroup = newMuscleGroup ?: match.muscleGroup
                )
                db.apexDao().updateWorkoutExercise(updated)
                _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
            }
        }
    }

    fun removeWorkoutExercise(workoutExerciseId: Long) {
        viewModelScope.launch {
            repository.deleteWorkoutExerciseWithSets(workoutExerciseId)
            _activeWorkoutSessionId.value?.let { loadActiveWorkoutData(it) }
        }
    }

    fun startCustomWorkout(title: String = "Вільне тренування") {
        viewModelScope.launch {
            val activeMeso = db.apexDao().getActiveMesocycleOnce()
            val mesoId = if (activeMeso != null) {
                activeMeso.id
            } else {
                db.apexDao().insertMesocycle(
                    MesocycleEntity(
                        name = "Власний тренувальний блок",
                        totalWeeks = 5,
                        currentWeek = 1,
                        programType = "Індивідуальні тренування",
                        priorityMuscles = ""
                    )
                )
            }

            val sessionId = db.apexDao().insertSession(
                WorkoutSessionEntity(
                    mesocycleId = mesoId,
                    weekNumber = activeMeso?.currentWeek ?: 1,
                    title = title,
                    dateTimestamp = System.currentTimeMillis()
                )
            )

            startWorkout(sessionId)
        }
    }

    fun startTodayWorkout(
        title: String,
        selectedMuscles: List<MuscleGroup>,
        exercises: List<PlannedExercise>
    ) {
        viewModelScope.launch {
            val activeMeso = db.apexDao().getActiveMesocycleOnce()
            val mesoId = if (activeMeso != null) {
                activeMeso.id
            } else {
                db.apexDao().insertMesocycle(
                    MesocycleEntity(
                        name = "Вільний тренувальний блок",
                        totalWeeks = 5,
                        currentWeek = 1,
                        programType = "Вільний вибір атлета",
                        priorityMuscles = selectedMuscles.joinToString(",") { it.name }
                    )
                )
            }

            val sessionId = db.apexDao().insertSession(
                WorkoutSessionEntity(
                    mesocycleId = mesoId,
                    weekNumber = activeMeso?.currentWeek ?: 1,
                    title = title,
                    dateTimestamp = System.currentTimeMillis()
                )
            )

            val allEx = db.apexDao().getAllExercises().firstOrNull() ?: emptyList()

            for ((idx, ex) in exercises.withIndex()) {
                val matched = allEx.find { it.name.equals(ex.exerciseName, ignoreCase = true) }
                val exId = matched?.id ?: 0L

                val workoutExId = db.apexDao().insertWorkoutExercise(
                    WorkoutExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = exId,
                        exerciseName = ex.exerciseName,
                        muscleGroup = ex.muscleGroup,
                        orderIndex = idx + 1
                    )
                )

                val defaultSets = (1..3).map { setNum ->
                    WorkoutSetEntity(
                        workoutExerciseId = workoutExId,
                        setNumber = setNum,
                        weightKg = ex.baseWeightKg,
                        reps = ex.baseRepsMin,
                        targetRepsMin = ex.baseRepsMin,
                        targetRepsMax = ex.baseRepsMax,
                        rir = 2,
                        isCompleted = false
                    )
                }
                db.apexDao().insertSets(defaultSets)
            }

            startWorkout(sessionId)
        }
    }

    fun scheduleWorkout(
        title: String,
        dateTimestamp: Long,
        exerciseNames: List<Pair<String, String>> = emptyList(),
        notes: String = "",
        recurrence: WorkoutRecurrence = WorkoutRecurrence.NONE
    ) {
        viewModelScope.launch {
            val activeMeso = activeMesocycle.value
            val mesoId = if (activeMeso != null) {
                activeMeso.id
            } else {
                db.apexDao().insertMesocycle(
                    MesocycleEntity(
                        name = "Власний тренувальний блок",
                        totalWeeks = 5,
                        currentWeek = 1,
                        programType = "Індивідуальні тренування",
                        priorityMuscles = ""
                    )
                )
            }

            val timestamps = when (recurrence) {
                WorkoutRecurrence.NONE -> listOf(dateTimestamp)
                WorkoutRecurrence.WEEKLY_4_WEEKS -> (0..3).map { i -> dateTimestamp + i * 7L * 86_400_000L }
                WorkoutRecurrence.EVERY_2_DAYS_2_WEEKS -> (0..6).map { i -> dateTimestamp + i * 2L * 86_400_000L }
                WorkoutRecurrence.DAILY_1_WEEK -> (0..6).map { i -> dateTimestamp + i * 1L * 86_400_000L }
            }

            val allEx = if (exerciseNames.isNotEmpty()) {
                db.apexDao().getAllExercises().firstOrNull() ?: emptyList()
            } else emptyList()

            timestamps.forEach { ts ->
                val sessionId = db.apexDao().insertSession(
                    WorkoutSessionEntity(
                        mesocycleId = mesoId,
                        weekNumber = activeMeso?.currentWeek ?: 1,
                        title = title.trim().ifEmpty { "Заплановане тренування" },
                        dateTimestamp = ts,
                        isCompleted = false,
                        notes = notes
                    )
                )

                if (exerciseNames.isNotEmpty()) {
                    exerciseNames.forEachIndexed { idx, (exName, muscle) ->
                        val matched = allEx.find { it.name.equals(exName, ignoreCase = true) }
                        val exId = matched?.id ?: 0L
                        val workoutExId = db.apexDao().insertWorkoutExercise(
                            WorkoutExerciseEntity(
                                sessionId = sessionId,
                                exerciseId = exId,
                                exerciseName = exName,
                                muscleGroup = muscle,
                                orderIndex = idx
                            )
                        )
                        val defaultSets = (1..3).map { setNum ->
                            WorkoutSetEntity(
                                workoutExerciseId = workoutExId,
                                setNumber = setNum,
                                weightKg = 50.0,
                                reps = 10,
                                targetRepsMin = 8,
                                targetRepsMax = 12,
                                rir = 2,
                                isCompleted = false
                            )
                        }
                        db.apexDao().insertSets(defaultSets)
                    }
                }
            }
        }
    }

    fun scheduleWorkoutFromSession(
        sourceSessionId: Long,
        dateTimestamp: Long,
        customTitle: String? = null,
        notes: String = "",
        recurrence: WorkoutRecurrence = WorkoutRecurrence.NONE
    ) {
        viewModelScope.launch {
            val sourceSession = db.apexDao().getSessionById(sourceSessionId) ?: return@launch
            val activeMeso = activeMesocycle.value
            val mesoId = if (activeMeso != null) {
                activeMeso.id
            } else {
                db.apexDao().insertMesocycle(
                    MesocycleEntity(
                        name = "Власний тренувальний блок",
                        totalWeeks = 5,
                        currentWeek = 1,
                        programType = "Індивідуальні тренування",
                        priorityMuscles = ""
                    )
                )
            }

            val finalTitle = customTitle?.trim()?.ifEmpty { sourceSession.title } ?: sourceSession.title
            val sourceExercises = db.apexDao().getExercisesForSessionOnce(sourceSessionId)

            val timestamps = when (recurrence) {
                WorkoutRecurrence.NONE -> listOf(dateTimestamp)
                WorkoutRecurrence.WEEKLY_4_WEEKS -> (0..3).map { i -> dateTimestamp + i * 7L * 86_400_000L }
                WorkoutRecurrence.EVERY_2_DAYS_2_WEEKS -> (0..6).map { i -> dateTimestamp + i * 2L * 86_400_000L }
                WorkoutRecurrence.DAILY_1_WEEK -> (0..6).map { i -> dateTimestamp + i * 1L * 86_400_000L }
            }

            timestamps.forEach { ts ->
                val newSessionId = db.apexDao().insertSession(
                    WorkoutSessionEntity(
                        mesocycleId = mesoId,
                        weekNumber = activeMeso?.currentWeek ?: 1,
                        title = finalTitle,
                        dateTimestamp = ts,
                        isCompleted = false,
                        durationSeconds = 0,
                        totalTonnageKg = 0.0,
                        notes = notes
                    )
                )

                for (ex in sourceExercises) {
                    val newWorkoutExId = db.apexDao().insertWorkoutExercise(
                        WorkoutExerciseEntity(
                            sessionId = newSessionId,
                            exerciseId = ex.exerciseId,
                            exerciseName = ex.exerciseName,
                            muscleGroup = ex.muscleGroup,
                            orderIndex = ex.orderIndex
                        )
                    )

                    val sourceSets = db.apexDao().getSetsForExerciseOnce(ex.id)
                    val newSets = if (sourceSets.isNotEmpty()) {
                        sourceSets.map { s ->
                            WorkoutSetEntity(
                                workoutExerciseId = newWorkoutExId,
                                setNumber = s.setNumber,
                                weightKg = s.weightKg,
                                reps = s.reps,
                                targetRepsMin = s.targetRepsMin,
                                targetRepsMax = s.targetRepsMax,
                                rir = s.rir,
                                isWarmup = s.isWarmup,
                                isCompleted = false,
                                timestamp = System.currentTimeMillis()
                            )
                        }
                    } else {
                        (1..3).map { setNum ->
                            WorkoutSetEntity(
                                workoutExerciseId = newWorkoutExId,
                                setNumber = setNum,
                                weightKg = 50.0,
                                reps = 10,
                                targetRepsMin = 8,
                                targetRepsMax = 12,
                                rir = 2,
                                isCompleted = false
                            )
                        }
                    }
                    db.apexDao().insertSets(newSets)
                }
            }
        }
    }

    fun deleteSession(sessionId: Long) {
        viewModelScope.launch {
            repository.deleteSessionWithDetails(sessionId)
        }
    }

    fun updateWorkoutHistorySession(
        sessionId: Long,
        title: String,
        notes: String,
        wellBeingRating: Int?
    ) {
        viewModelScope.launch {
            repository.updateSessionTitleAndNotes(
                sessionId = sessionId,
                title = title.trim().ifEmpty { "Тренування" },
                notes = notes.trim(),
                wellBeingRating = wellBeingRating
            )
        }
    }

    fun repeatWorkout(sessionId: Long) {
        viewModelScope.launch {
            val sourceSession = db.apexDao().getSessionById(sessionId) ?: return@launch
            val activeMeso = db.apexDao().getActiveMesocycleOnce()
            val mesoId = if (activeMeso != null) {
                activeMeso.id
            } else {
                db.apexDao().insertMesocycle(
                    MesocycleEntity(
                        name = "Власний тренувальний блок",
                        totalWeeks = 5,
                        currentWeek = 1,
                        programType = "Індивідуальні тренування",
                        priorityMuscles = ""
                    )
                )
            }

            val newSessionId = db.apexDao().insertSession(
                WorkoutSessionEntity(
                    mesocycleId = mesoId,
                    weekNumber = activeMeso?.currentWeek ?: sourceSession.weekNumber,
                    title = sourceSession.title,
                    dateTimestamp = System.currentTimeMillis(),
                    isCompleted = false,
                    durationSeconds = 0,
                    totalTonnageKg = 0.0,
                    notes = ""
                )
            )

            val sourceExercises = db.apexDao().getExercisesForSessionOnce(sessionId)
            for (ex in sourceExercises) {
                val newWorkoutExId = db.apexDao().insertWorkoutExercise(
                    WorkoutExerciseEntity(
                        sessionId = newSessionId,
                        exerciseId = ex.exerciseId,
                        exerciseName = ex.exerciseName,
                        muscleGroup = ex.muscleGroup,
                        orderIndex = ex.orderIndex
                    )
                )

                val sourceSets = db.apexDao().getSetsForExerciseOnce(ex.id)
                val newSets = if (sourceSets.isNotEmpty()) {
                    sourceSets.map { s ->
                        WorkoutSetEntity(
                            workoutExerciseId = newWorkoutExId,
                            setNumber = s.setNumber,
                            weightKg = s.weightKg,
                            reps = s.reps,
                            targetRepsMin = s.targetRepsMin,
                            targetRepsMax = s.targetRepsMax,
                            rir = s.rir,
                            isWarmup = s.isWarmup,
                            isCompleted = false,
                            timestamp = System.currentTimeMillis()
                        )
                    }
                } else {
                    (1..3).map { setNum ->
                        WorkoutSetEntity(
                            workoutExerciseId = newWorkoutExId,
                            setNumber = setNum,
                            weightKg = 40.0,
                            reps = 10,
                            targetRepsMin = 8,
                            targetRepsMax = 12,
                            rir = 2,
                            isCompleted = false
                        )
                    }
                }
                db.apexDao().insertSets(newSets)
            }

            startWorkout(newSessionId)
        }
    }

    fun getSessionFlow(sessionId: Long): kotlinx.coroutines.flow.Flow<WorkoutSessionEntity?> {
        return db.apexDao().getSessionByIdFlow(sessionId)
    }

    fun updateWorkoutTitle(sessionId: Long, newTitle: String) {
        viewModelScope.launch {
            val session = db.apexDao().getSessionById(sessionId)
            if (session != null && newTitle.isNotBlank()) {
                db.apexDao().updateSession(session.copy(title = newTitle.trim()))
            }
        }
    }

    fun finishWorkout(
        sessionId: Long,
        notes: String = "",
        title: String? = null,
        wellBeingRating: Int? = null
    ) {
        viewModelScope.launch {
            workoutTimerJob?.cancel()
            val duration = _activeWorkoutElapsedSeconds.value
            val exercises = db.apexDao().getExercisesForSessionOnce(sessionId)
            var totalTonnage = 0.0
            var completedCount = 0

            for (we in exercises) {
                val sets = db.apexDao().getSetsForExerciseOnce(we.id)
                for (set in sets) {
                    if (set.isCompleted && !set.isWarmup) {
                        totalTonnage += set.weightKg * set.reps
                        completedCount++
                    }
                }
            }

            // CRITICAL: If no sets were completed or tonnage is 0, this is an aborted/cancelled session!
            // It MUST NOT be saved into history. Delete it completely.
            if (completedCount == 0 || totalTonnage <= 0.0) {
                deleteSession(sessionId)
                _activeWorkoutSessionId.value = null
                _activeWorkoutExercisesWithSets.value = emptyList()
                _activeWorkoutElapsedSeconds.value = 0
                return@launch
            }

            val session = db.apexDao().getSessionById(sessionId)
            if (session != null) {
                db.apexDao().updateSession(
                    session.copy(
                        title = if (!title.isNullOrBlank()) title.trim() else session.title,
                        isCompleted = true,
                        durationSeconds = duration,
                        totalTonnageKg = totalTonnage,
                        notes = notes,
                        wellBeingRating = wellBeingRating
                    )
                )
                repository.recordSessionProgressiveOverload(sessionId)
            }

            audioHaptic.playFinishTone()
            cancelRestTimer()
            _activeWorkoutSessionId.value = null
            _activeWorkoutExercisesWithSets.value = emptyList()

            // Background recalculation of internal schedule and weekly summary
            launch(Dispatchers.IO) {
                val allCompleted = db.apexDao().getAllSessionsOnce().filter { it.isCompleted }
                val newSchedules = com.example.analysis.WorkoutAnalysisEngine.recomputeInternalSchedule(allCompleted)
                db.apexDao().deleteAllInternalSchedules()
                db.apexDao().insertInternalSchedules(newSchedules)

                val summary = com.example.analysis.WorkoutAnalysisEngine.generateWeeklySummary(allCompleted)
                db.apexDao().insertWeeklySummary(summary)
            }
        }
    }

    fun cancelWorkout() {
        val currentId = _activeWorkoutSessionId.value
        workoutTimerJob?.cancel()
        cancelRestTimer()
        _activeWorkoutSessionId.value = null
        _activeWorkoutExercisesWithSets.value = emptyList()
        _activeWorkoutElapsedSeconds.value = 0
        if (currentId != null) {
            deleteSession(currentId)
        }
    }

    // Rest Timer operations backed by foreground service
    fun startRestTimer(seconds: Int) {
        com.example.service.RestTimerManager.start(getApplication(), seconds)
    }

    fun adjustRestTimer(deltaSeconds: Int) {
        com.example.service.RestTimerManager.adjust(getApplication(), deltaSeconds)
    }

    fun cancelRestTimer() {
        com.example.service.RestTimerManager.cancel(getApplication())
    }

    // Mesocycle management
    fun advanceMesocycleWeek() {
        viewModelScope.launch {
            val meso = activeMesocycle.value ?: return@launch
            repository.advanceMesocycleWeek(meso)
        }
    }

    fun completeMesocycle() {
        viewModelScope.launch {
            val meso = activeMesocycle.value ?: return@launch
            repository.completeMesocycle(meso)
        }
    }

    fun startNewMesocycle(
        name: String,
        totalWeeks: Int,
        programType: String,
        priorityMuscles: List<MuscleGroup>
    ) {
        viewModelScope.launch {
            repository.startNewMesocycle(name, totalWeeks, programType, priorityMuscles)
        }
    }

    fun createStructuredMesocycle(
        name: String,
        totalWeeks: Int,
        model: ProgressionModel,
        plannedDays: List<PlannedDay>,
        priorityMuscles: List<MuscleGroup>
    ) {
        viewModelScope.launch {
            repository.createStructuredMesocycle(
                name = name,
                totalWeeks = totalWeeks,
                model = model,
                plannedDays = plannedDays,
                priorityMuscles = priorityMuscles
            )
        }
    }

    // Readiness Checkin
    fun logReadiness(
        sleep: Int,
        doms: Int,
        energy: Int,
        motivation: Int
    ) {
        viewModelScope.launch {
            val result = AutoregulationEngine.calculateReadiness(sleep, doms, energy, motivation)
            repository.logReadinessCheckin(
                ReadinessCheckinEntity(
                    sleepQuality = sleep,
                    domsSoreness = doms,
                    energyLevel = energy,
                    motivation = motivation,
                    cnsScore = result.cnsScore,
                    autoregulationMessage = result.recommendationUk
                )
            )
        }
    }

    // Volume Config
    fun saveVolumeSettings(
        exp: ExperienceLevel,
        sleep: SleepFactor,
        nutrition: NutritionFactor,
        stress: StressFactor,
        priorities: Set<MuscleGroup>
    ) {
        viewModelScope.launch {
            repository.saveVolumeConfig(
                UserVolumeConfigEntity(
                    id = 1,
                    experienceLevel = exp.name,
                    sleepFactor = sleep.name,
                    nutritionFactor = nutrition.name,
                    stressFactor = stress.name,
                    priorityMuscles = priorities.joinToString(",") { it.name }
                )
            )
        }
    }

    // Body Measurements
    fun addBodyMeasurement(
        weightKg: Double,
        waistCm: Double,
        bicepsCm: Double,
        chestCm: Double,
        bodyFat: Double
    ) {
        viewModelScope.launch {
            repository.addBodyMeasurement(
                BodyMeasurementEntity(
                    weightKg = weightKg,
                    waistCm = waistCm,
                    bicepsCm = bicepsCm,
                    chestCm = chestCm,
                    bodyFatPercent = bodyFat
                )
            )
        }
    }

    // Gym Profiles
    fun selectGymProfile(profileId: Long) {
        viewModelScope.launch {
            repository.selectGymProfile(profileId)
        }
    }

    fun addGymProfile(name: String, equipment: String) {
        viewModelScope.launch {
            repository.addGymProfile(name, equipment)
        }
    }

    fun deleteGymProfile(profileId: Long) {
        viewModelScope.launch {
            db.apexDao().deleteGymProfileById(profileId)
        }
    }

    // Mesocycle CRUD extensions
    fun updateMesocycleDetails(mesocycleId: Long, name: String, goalDescriptionUk: String) {
        viewModelScope.launch {
            val cleanName = name.trim().ifEmpty { "Тренувальний блок" }
            repository.updateMesocycleNameAndGoal(mesocycleId, cleanName, goalDescriptionUk.trim())
        }
    }

    fun activateMesocycle(mesocycleId: Long) {
        viewModelScope.launch {
            repository.activateMesocycle(mesocycleId)
        }
    }

    fun deleteMesocycle(mesocycleId: Long) {
        viewModelScope.launch {
            repository.deleteMesocycleWithHistory(mesocycleId)
        }
    }

    // Exercises CRUD management (Create, Update, Delete)
    fun saveOrUpdateExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            val cleanName = exercise.name.trim()
            if (cleanName.isEmpty()) return@launch
            repository.upsertExercise(exercise.copy(name = cleanName))
        }
    }

    fun updateExerciseProgressionParams(
        exerciseId: Long,
        incrementKg: Double,
        repsMin: Int,
        repsMax: Int,
        targetRir: Int,
        progressionType: String
    ) {
        viewModelScope.launch {
            repository.updateExerciseProgressionParams(
                exerciseId = exerciseId,
                incrementKg = incrementKg,
                repsMin = repsMin,
                repsMax = repsMax,
                targetRir = targetRir,
                progressionType = progressionType
            )
        }
    }

    fun deleteExercise(exerciseId: Long) {
        viewModelScope.launch {
            repository.deleteExercise(exerciseId)
        }
    }

    fun deleteExerciseByName(name: String) {
        viewModelScope.launch {
            repository.deleteExerciseByName(name)
        }
    }

    // Data Export
    suspend fun exportJson(): String = repository.exportDataAsJson()
    suspend fun exportCsv(): String = repository.exportWorkoutHistoryCsv()

    // Reset All User Progress
    fun resetAllProgress(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _activeWorkoutSessionId.value = null
            _activeWorkoutExercisesWithSets.value = emptyList()
            _activeWorkoutElapsedSeconds.value = 0
            repository.resetAllProgress()
            onComplete()
        }
    }
}

data class WorkoutExerciseWithSets(
    val exercise: WorkoutExerciseEntity,
    val sets: List<WorkoutSetEntity>
)
