package com.example.data

import com.example.engine.DoubleProgressionEngine
import com.example.engine.MesocycleProgressionEngine
import com.example.engine.PlannedDay
import com.example.engine.ProgressionModel
import com.example.engine.VolumeLandmarksCalculator
import com.example.engine.UserVolumeConfig
import com.example.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.roundToInt

class ApexRepository(private val dao: ApexDao) {

    // =========================================================================
    // 1. EXERCISES CRUD
    // =========================================================================
    val allExercises: Flow<List<ExerciseEntity>> = dao.getAllExercises()
    val exercisesWithOverloadHistory: Flow<List<ExerciseWithOverloadHistory>> = dao.getExercisesWithOverloadHistory()

    fun getExercisesByMuscle(muscle: String): Flow<List<ExerciseEntity>> = dao.getExercisesByMuscle(muscle)
    fun getExercisesByEquipment(equipment: String): Flow<List<ExerciseEntity>> = dao.getExercisesByEquipment(equipment)
    fun searchExercises(query: String): Flow<List<ExerciseEntity>> = dao.searchExercises(query)
    fun getExerciseByIdFlow(id: Long): Flow<ExerciseEntity?> = dao.getExerciseByIdFlow(id)
    fun getExerciseWithOverloadHistoryById(id: Long): Flow<ExerciseWithOverloadHistory?> =
        dao.getExerciseWithOverloadHistoryById(id)

    suspend fun getExerciseById(id: Long): ExerciseEntity? = dao.getExerciseById(id)
    suspend fun getExerciseByName(name: String): ExerciseEntity? = dao.getExerciseByName(name)
    suspend fun addExercise(exercise: ExerciseEntity): Long = dao.insertExercise(exercise)
    suspend fun upsertExercise(exercise: ExerciseEntity): Long = dao.upsertExercise(exercise)
    suspend fun updateExercise(exercise: ExerciseEntity): Int = dao.updateExercise(exercise)
    suspend fun updateExerciseProgressionParams(
        exerciseId: Long,
        incrementKg: Double,
        repsMin: Int,
        repsMax: Int,
        targetRir: Int,
        progressionType: String
    ) = dao.updateExerciseProgressionParams(
        exerciseId = exerciseId,
        incrementKg = incrementKg,
        repsMin = repsMin,
        repsMax = repsMax,
        targetRir = targetRir,
        progressionType = progressionType
    )
    suspend fun deleteExercise(exerciseId: Long) = dao.deleteExerciseById(exerciseId)
    suspend fun deleteExerciseEntity(exercise: ExerciseEntity): Int = dao.deleteExercise(exercise)
    suspend fun deleteExerciseByName(name: String) = dao.deleteExerciseByName(name)

    // =========================================================================
    // 2. MESOCYCLES & STRUCTURED PROGRESSION CRUD
    // =========================================================================
    val activeMesocycle: Flow<MesocycleEntity?> = dao.getActiveMesocycle()
    val allMesocycles: Flow<List<MesocycleEntity>> = dao.getAllMesocycles()
    val completedMesocycles: Flow<List<MesocycleEntity>> = dao.getCompletedMesocycles()
    val activeMesocycleWithStructure: Flow<MesocycleWithFullStructure?> = dao.getActiveMesocycleWithStructure()
    val allMesocyclesWithStructure: Flow<List<MesocycleWithFullStructure>> = dao.getAllMesocyclesWithStructure()
    val allProgressiveOverloadLogs: Flow<List<ProgressiveOverloadLogEntity>> = dao.getAllProgressiveOverloadLogs()
    val completedSessionsWithDetails: Flow<List<WorkoutSessionWithDetails>> = dao.getCompletedSessionsWithDetails()
    val allSessionsWithDetails: Flow<List<WorkoutSessionWithDetails>> = dao.getAllSessionsWithDetails()

    fun getMesocycleWithStructure(mesocycleId: Long): Flow<MesocycleWithFullStructure?> =
        dao.getMesocycleWithStructure(mesocycleId)

    fun getWeekPlansForMesocycle(mesocycleId: Long): Flow<List<MesocycleWeekPlanEntity>> =
        dao.getWeekPlansForMesocycle(mesocycleId)

    suspend fun getMesocycleById(id: Long): MesocycleEntity? = dao.getMesocycleById(id)
    suspend fun updateMesocycle(mesocycle: MesocycleEntity): Int = dao.updateMesocycle(mesocycle)
    suspend fun updateMesocycleNameAndGoal(mesocycleId: Long, name: String, goalDescriptionUk: String) =
        dao.updateMesocycleNameAndGoal(mesocycleId, name, goalDescriptionUk)
    suspend fun updateMesocycleWeekPlan(plan: MesocycleWeekPlanEntity): Int = dao.updateMesocycleWeekPlan(plan)
    suspend fun activateMesocycle(mesocycleId: Long) {
        dao.deactivateAllMesocycles()
        dao.setMesocycleActiveStatus(mesocycleId, isActive = true, isCompleted = false)
    }
    suspend fun deleteMesocycleWithHistory(mesocycleId: Long) = dao.deleteMesocycleWithHistory(mesocycleId)

    suspend fun startNewMesocycle(
        name: String,
        totalWeeks: Int = 5,
        programType: String,
        priorityMuscles: List<MuscleGroup>,
        progressionModel: ProgressionModel = ProgressionModel.VOLUME_WAVE
    ): Long {
        // Deactivate any currently active mesocycles
        val current = dao.getActiveMesocycleOnce()
        if (current != null) {
            dao.updateMesocycle(current.copy(isActive = false, isCompleted = true))
        }

        val newMeso = MesocycleEntity(
            name = name,
            totalWeeks = totalWeeks,
            currentWeek = 1,
            startDate = System.currentTimeMillis(),
            isActive = true,
            isCompleted = false,
            programType = programType,
            priorityMuscles = priorityMuscles.joinToString(",") { it.name },
            progressionModel = progressionModel.name,
            deloadWeek = totalWeeks,
            startingRir = 3,
            peakRir = 0,
            weeklySetIncrement = 1,
            weeklyWeightIncrementPercent = 2.5f,
            goalDescriptionUk = progressionModel.titleUk
        )
        val mesoId = dao.insertMesocycle(newMeso)

        // Persist weekly progressive overload structure in Room
        val weekTargets = MesocycleProgressionEngine.calculateWeekProgressionTargets(totalWeeks, progressionModel)
        val weekPlans = weekTargets.map { target ->
            val phaseCode = when {
                target.isDeload -> "DELOAD"
                target.weekNumber == 1 -> "INTRO_MEV"
                target.weekNumber == 2 -> "ACCUMULATION_MAV"
                target.weekNumber == 3 -> "OVERREACH_MRV"
                else -> "PEAK_INTENSITY"
            }
            MesocycleWeekPlanEntity(
                mesocycleId = mesoId,
                weekNumber = target.weekNumber,
                phaseCode = phaseCode,
                phaseNameUk = target.phaseNameUk,
                isDeload = target.isDeload,
                targetRir = target.targetRir,
                setsMultiplier = target.setsMultiplier,
                weightMultiplier = target.weightMultiplier,
                targetRepsMin = target.repsTargetMin,
                targetRepsMax = target.repsTargetMax,
                plannedTotalSets = (52 * target.setsMultiplier).roundToInt(),
                plannedTonnageKg = 22000.0 * target.setsMultiplier * target.weightMultiplier,
                coachGuidelineUk = target.coachGuidelineUk
            )
        }
        dao.insertMesocycleWeekPlans(weekPlans)

        // Generate weekly workouts based on programType
        generateWorkoutsForMesocycle(mesoId, programType)
        return mesoId
    }

    suspend fun createStructuredMesocycle(
        name: String,
        totalWeeks: Int,
        model: ProgressionModel,
        plannedDays: List<PlannedDay>,
        priorityMuscles: List<MuscleGroup>
    ): Long {
        val current = dao.getActiveMesocycleOnce()
        if (current != null) {
            dao.updateMesocycle(current.copy(isActive = false, isCompleted = true))
        }

        val newMeso = MesocycleEntity(
            name = name,
            totalWeeks = totalWeeks,
            currentWeek = 1,
            startDate = System.currentTimeMillis(),
            isActive = true,
            isCompleted = false,
            programType = "${model.titleUk} (${plannedDays.size} дні)",
            priorityMuscles = priorityMuscles.joinToString(",") { it.name },
            progressionModel = model.name,
            deloadWeek = totalWeeks,
            startingRir = 3,
            peakRir = 0,
            weeklySetIncrement = 1,
            weeklyWeightIncrementPercent = 2.5f,
            goalDescriptionUk = model.titleUk
        )
        val mesoId = dao.insertMesocycle(newMeso)

        val weekTargets = MesocycleProgressionEngine.calculateWeekProgressionTargets(totalWeeks, model)
        val weekSummaries = MesocycleProgressionEngine.calculateFullMesocycleProgression(plannedDays, totalWeeks, model)
        val allExercises = dao.getAllExercisesOnce()

        // Save MesocycleWeekPlanEntity rows for each week
        val weekPlans = weekTargets.mapIndexed { idx, target ->
            val summary = weekSummaries.getOrNull(idx)
            val phaseCode = when {
                target.isDeload -> "DELOAD"
                target.weekNumber == 1 -> "INTRO_MEV"
                target.weekNumber == 2 -> "ACCUMULATION_MAV"
                target.weekNumber == 3 -> "OVERREACH_MRV"
                else -> "PEAK_INTENSITY"
            }
            MesocycleWeekPlanEntity(
                mesocycleId = mesoId,
                weekNumber = target.weekNumber,
                phaseCode = phaseCode,
                phaseNameUk = target.phaseNameUk,
                isDeload = target.isDeload,
                targetRir = target.targetRir,
                setsMultiplier = target.setsMultiplier,
                weightMultiplier = target.weightMultiplier,
                targetRepsMin = target.repsTargetMin,
                targetRepsMax = target.repsTargetMax,
                plannedTotalSets = summary?.totalSets ?: 0,
                plannedTonnageKg = summary?.estimatedTotalTonnageKg ?: 0.0,
                coachGuidelineUk = target.coachGuidelineUk
            )
        }
        dao.insertMesocycleWeekPlans(weekPlans)

        for (target in weekTargets) {
            val w = target.weekNumber
            for ((dIdx, day) in plannedDays.withIndex()) {
                val sessionTimestamp = System.currentTimeMillis() + ((w - 1) * 7L + dIdx) * 24L * 3600 * 1000
                val sessionId = dao.insertSession(
                    WorkoutSessionEntity(
                        mesocycleId = mesoId,
                        weekNumber = w,
                        title = day.dayTitle,
                        dateTimestamp = sessionTimestamp,
                        isCompleted = false,
                        progressionStatus = if (target.isDeload) "DELOAD" else "PLANNED"
                    )
                )

                for ((eIdx, ex) in day.exercises.withIndex()) {
                    val priorityBonus = if (ex.isPriority && !target.isDeload) 1 else 0
                    val calculatedSets = when {
                        target.isDeload -> 2
                        else -> (3 * target.setsMultiplier).roundToInt().coerceIn(2, 6) + priorityBonus
                    }
                    val rawWeight = ex.baseWeightKg * target.weightMultiplier
                    val roundedWeight = (rawWeight * 2.0).roundToInt() / 2.0

                    val matchedEx = allExercises.find { it.name.equals(ex.exerciseName, ignoreCase = true) }
                    val exId = matchedEx?.id ?: 0L

                    val progressionAction = when {
                        target.isDeload -> "DELOAD"
                        w > 1 && target.weightMultiplier > 1.0f -> "INCREASE_WEIGHT"
                        else -> "HOLD_WEIGHT_ADD_REPS"
                    }

                    val note = "Тиждень $w (${target.phaseNameUk}) • RIR ${target.targetRir} • ${target.coachGuidelineUk}"
                    val weId = dao.insertWorkoutExercise(
                        WorkoutExerciseEntity(
                            sessionId = sessionId,
                            exerciseId = exId,
                            exerciseName = ex.exerciseName,
                            muscleGroup = ex.muscleGroup,
                            orderIndex = eIdx + 1,
                            notes = note,
                            targetSets = calculatedSets,
                            targetRepsMin = target.repsTargetMin,
                            targetRepsMax = target.repsTargetMax,
                            targetWeightKg = roundedWeight,
                            targetRir = target.targetRir,
                            previousBestWeightKg = ex.baseWeightKg,
                            previousBestReps = ex.baseRepsMin,
                            overloadAchieved = false,
                            progressionAction = progressionAction
                        )
                    )

                    for (s in 1..calculatedSets) {
                        dao.insertSet(
                            WorkoutSetEntity(
                                workoutExerciseId = weId,
                                setNumber = s,
                                weightKg = roundedWeight,
                                targetWeightKg = roundedWeight,
                                reps = target.repsTargetMin,
                                targetRepsMin = target.repsTargetMin,
                                targetRepsMax = target.repsTargetMax,
                                rir = target.targetRir,
                                isWarmup = false,
                                isCompleted = false
                            )
                        )
                    }
                }
            }
        }

        return mesoId
    }

    suspend fun advanceMesocycleWeek(mesocycle: MesocycleEntity) {
        if (mesocycle.currentWeek >= mesocycle.totalWeeks) {
            dao.updateMesocycle(mesocycle.copy(isCompleted = true, isActive = false))
        } else {
            val nextWeek = mesocycle.currentWeek + 1
            dao.updateMesocycle(mesocycle.copy(currentWeek = nextWeek))
            // Only generate next week workouts if they don't already exist
            val existingSessions = dao.getSessionsForMesocycleOnce(mesocycle.id)
            val hasNextWeekSessions = existingSessions.any { it.weekNumber == nextWeek }
            if (!hasNextWeekSessions) {
                generateWorkoutsForWeek(mesocycle.id, nextWeek, mesocycle.programType)
            }
        }
    }

    suspend fun completeMesocycle(mesocycle: MesocycleEntity) {
        dao.updateMesocycle(mesocycle.copy(isActive = false, isCompleted = true))
    }

    // =========================================================================
    // 3. WORKOUT HISTORY (SESSIONS, EXERCISES, SETS & OVERLOAD LOGS) CRUD
    // =========================================================================
    fun getSessionsForMesocycle(mesocycleId: Long): Flow<List<WorkoutSessionEntity>> =
        dao.getSessionsForMesocycle(mesocycleId)

    fun getSessionsForMesocycleWeek(mesocycleId: Long, weekNumber: Int): Flow<List<WorkoutSessionEntity>> =
        dao.getSessionsForMesocycleWeek(mesocycleId, weekNumber)

    fun getSessionsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<WorkoutSessionEntity>> =
        dao.getSessionsByDateRange(startTimestamp, endTimestamp)

    fun getSessionWithDetails(sessionId: Long): Flow<WorkoutSessionWithDetails?> =
        dao.getSessionWithDetails(sessionId)

    val allSessions: Flow<List<WorkoutSessionEntity>> = dao.getAllSessions()
    val completedSessions: Flow<List<WorkoutSessionEntity>> = dao.getCompletedSessions()

    suspend fun getSessionById(id: Long): WorkoutSessionEntity? = dao.getSessionById(id)
    suspend fun getSessionWithDetailsOnce(sessionId: Long): WorkoutSessionWithDetails? =
        dao.getSessionWithDetailsOnce(sessionId)
    suspend fun insertSession(session: WorkoutSessionEntity): Long = dao.insertSession(session)
    suspend fun upsertSession(session: WorkoutSessionEntity): Long = dao.upsertSession(session)
    suspend fun insertCompleteWorkoutSession(
        session: WorkoutSessionEntity,
        exercisesWithSets: List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>
    ): Long = dao.insertCompleteWorkoutSession(session, exercisesWithSets)
    suspend fun updateSession(session: WorkoutSessionEntity) = dao.updateSession(session)
    suspend fun updateSessionTitleAndNotes(
        sessionId: Long,
        title: String,
        notes: String,
        wellBeingRating: Int?
    ) = dao.updateSessionTitleAndNotes(sessionId, title, notes, wellBeingRating)
    suspend fun deleteSession(id: Long) = dao.deleteSessionWithDetails(id)
    suspend fun deleteSessionWithDetails(sessionId: Long) = dao.deleteSessionWithDetails(sessionId)

    // Workout Exercises & Sets CRUD
    fun getExercisesForSession(sessionId: Long): Flow<List<WorkoutExerciseEntity>> =
        dao.getExercisesForSession(sessionId)

    fun getSetsForExercise(exerciseId: Long): Flow<List<WorkoutSetEntity>> =
        dao.getSetsForExercise(exerciseId)

    fun getCompletedSetsForExerciseHistory(exerciseName: String): Flow<List<WorkoutSetWithExerciseInfo>> =
        dao.getCompletedSetsForExerciseHistory(exerciseName)

    fun getOverloadLogsForExercise(exerciseName: String): Flow<List<ProgressiveOverloadLogEntity>> =
        dao.getOverloadLogsForExercise(exerciseName)

    suspend fun addWorkoutExercise(exercise: WorkoutExerciseEntity): Long =
        dao.insertWorkoutExercise(exercise)
    suspend fun updateWorkoutExercise(exercise: WorkoutExerciseEntity): Int =
        dao.updateWorkoutExercise(exercise)
    suspend fun deleteWorkoutExerciseWithSets(workoutExerciseId: Long) =
        dao.deleteWorkoutExerciseWithSets(workoutExerciseId)

    suspend fun addSet(set: WorkoutSetEntity): Long = dao.insertSet(set)
    suspend fun updateSet(set: WorkoutSetEntity) = dao.updateSet(set)
    suspend fun deleteSet(id: Long) = dao.deleteSetById(id)
    suspend fun deleteProgressiveOverloadLogById(id: Long) = dao.deleteProgressiveOverloadLogById(id)

    suspend fun getRecentSetsForExercise(exerciseName: String): List<WorkoutSetEntity> =
        dao.getRecentCompletedSetsForExercise(exerciseName)

    suspend fun getDoubleProgressionAdvice(
        exerciseName: String,
        equipment: EquipmentType,
        targetMin: Int,
        targetMax: Int
    ): DoubleProgressionEngine.ProgressionSuggestion {
        val history = dao.getRecentCompletedSetsForExercise(exerciseName)
        return DoubleProgressionEngine.evaluateProgression(exerciseName, equipment, targetMin, targetMax, history)
    }

    /**
     * Evaluates and persists Progressive Overload logs & metrics for a completed session.
     * Updates WorkoutSetEntity (1RM, PRs, tonnage), WorkoutExerciseEntity (overloadAchieved, progressionAction),
     * WorkoutSessionEntity (totalWorkingSets, averageRir, prCount, progressionStatus), and inserts ProgressiveOverloadLogEntity rows.
     */
    suspend fun recordSessionProgressiveOverload(sessionId: Long) {
        val session = dao.getSessionById(sessionId) ?: return
        val sessionExercises = dao.getExercisesForSessionOnce(sessionId)
        val allExCatalog = dao.getAllExercisesOnce()

        var sessionPrCount = 0
        var sessionWorkingSets = 0
        var rirSum = 0
        var progressedExercisesCount = 0

        for (we in sessionExercises) {
            val sets = dao.getSetsForExerciseOnce(we.id)
            val completedWorkingSets = sets.filter { it.isCompleted && !it.isWarmup }
            if (completedWorkingSets.isEmpty()) continue

            sessionWorkingSets += completedWorkingSets.size
            rirSum += completedWorkingSets.sumOf { it.rir }

            // Historical sets for this exercise prior to this session
            val allRecentSets = dao.getRecentCompletedSetsForExercise(we.exerciseName)
            val currentSetIds = completedWorkingSets.map { it.id }.toSet()
            val priorSets = allRecentSets.filter { it.id !in currentSetIds }

            val prevBestWeight = priorSets.maxOfOrNull { it.weightKg } ?: we.previousBestWeightKg
            val prevBestRepsAtMax = priorSets.filter { it.weightKg == prevBestWeight }.maxOfOrNull { it.reps } ?: we.previousBestReps
            val prevBest1Rm = priorSets.maxOfOrNull { s ->
                if (s.reps <= 1) s.weightKg else s.weightKg * (1.0 + s.reps / 30.0)
            } ?: if (prevBestWeight > 0 && prevBestRepsAtMax > 0) prevBestWeight * (1.0 + prevBestRepsAtMax / 30.0) else 0.0

            val bestSetThisSession = completedWorkingSets.maxByOrNull { s ->
                if (s.reps <= 1) s.weightKg else s.weightKg * (1.0 + s.reps / 30.0)
            } ?: completedWorkingSets.first()

            val newWeight = bestSetThisSession.weightKg
            val newReps = bestSetThisSession.reps
            val new1Rm = if (newReps <= 1) newWeight else newWeight * (1.0 + newReps / 30.0)
            val newExerciseTonnage = completedWorkingSets.sumOf { it.weightKg * it.reps }
            val avgExRir = completedWorkingSets.map { it.rir }.average().toFloat()

            val isWeightJump = prevBestWeight > 0 && newWeight > prevBestWeight
            val isRepJump = prevBestWeight > 0 && newWeight == prevBestWeight && newReps > prevBestRepsAtMax
            val is1RmRecord = prevBest1Rm > 0 && new1Rm > prevBest1Rm + 0.1
            val isOverloadAchieved = isWeightJump || isRepJump || is1RmRecord || prevBestWeight == 0.0

            // Mark PR on the best set if it beat previous 1RM or weight
            if (isWeightJump || is1RmRecord) {
                sessionPrCount++
                dao.updateSet(
                    bestSetThisSession.copy(
                        isPersonalRecord = true,
                        estimated1RmKg = new1Rm,
                        tonnageKg = newWeight * newReps,
                        rpe = (10 - bestSetThisSession.rir).coerceIn(5, 10).toFloat()
                    )
                )
            }

            if (isOverloadAchieved) {
                progressedExercisesCount++
            }

            val exCatalog = allExCatalog.find { it.name.equals(we.exerciseName, ignoreCase = true) }
            val equip = exCatalog?.equipment?.let {
                runCatching { EquipmentType.valueOf(it) }.getOrDefault(EquipmentType.BARBELL)
            } ?: EquipmentType.BARBELL

            val suggestion = DoubleProgressionEngine.evaluateProgression(
                exerciseName = we.exerciseName,
                equipment = equip,
                targetRepsMin = we.targetRepsMin,
                targetRepsMax = we.targetRepsMax,
                historySets = completedWorkingSets + priorSets
            )

            val overloadType = when {
                suggestion.isPlateau -> "PLATEAU"
                isWeightJump -> "LOAD_PROGRESSION"
                isRepJump -> "REP_PROGRESSION"
                is1RmRecord -> "E1RM_RECORD"
                else -> "MAINTENANCE"
            }

            val nextAction = when {
                suggestion.isPlateau -> "PLATEAU_ROTATE"
                suggestion.shouldIncreaseWeight -> "INCREASE_WEIGHT"
                else -> "HOLD_WEIGHT_ADD_REPS"
            }

            dao.updateWorkoutExercise(
                we.copy(
                    previousBestWeightKg = if (prevBestWeight > 0) prevBestWeight else newWeight,
                    previousBestReps = if (prevBestRepsAtMax > 0) prevBestRepsAtMax else newReps,
                    overloadAchieved = isOverloadAchieved,
                    progressionAction = nextAction
                )
            )

            dao.insertProgressiveOverloadLog(
                ProgressiveOverloadLogEntity(
                    exerciseId = we.exerciseId,
                    exerciseName = we.exerciseName,
                    muscleGroup = we.muscleGroup,
                    sessionId = sessionId,
                    mesocycleId = session.mesocycleId,
                    weekNumber = session.weekNumber,
                    timestamp = System.currentTimeMillis(),
                    previousWeightKg = prevBestWeight,
                    newWeightKg = newWeight,
                    previousReps = prevBestRepsAtMax,
                    newReps = newReps,
                    previousEstimated1RmKg = prevBest1Rm,
                    newEstimated1RmKg = new1Rm,
                    previousExerciseTonnageKg = priorSets.take(completedWorkingSets.size).sumOf { it.weightKg * it.reps },
                    newExerciseTonnageKg = newExerciseTonnage,
                    workingSetsCompleted = completedWorkingSets.size,
                    avgRir = avgExRir,
                    overloadType = overloadType,
                    nextRecommendedWeightKg = suggestion.suggestedWeightKg,
                    nextRecommendedRepsMin = we.targetRepsMin,
                    nextRecommendedRepsMax = we.targetRepsMax,
                    summaryUk = suggestion.explanationUk
                )
            )
        }

        val avgSessionRir = if (sessionWorkingSets > 0) rirSum.toFloat() / sessionWorkingSets else null
        val progressionStatus = when {
            progressedExercisesCount > 0 -> "PROGRESSED"
            sessionWorkingSets > 0 -> "MAINTAINED"
            else -> session.progressionStatus
        }

        // Update session progressive overload summary
        val latestUpdatedSession = dao.getSessionById(sessionId) ?: session
        dao.updateSession(
            latestUpdatedSession.copy(
                totalWorkingSets = sessionWorkingSets,
                averageRir = avgSessionRir,
                prCount = sessionPrCount,
                progressionStatus = progressionStatus
            )
        )
    }

    suspend fun ensureProgressiveOverloadStructureSeeded() {
        val activeMeso = dao.getActiveMesocycleOnce() ?: return
        val existingPlans = dao.getWeekPlansForMesocycleOnce(activeMeso.id)
        if (existingPlans.isEmpty()) {
            val model = runCatching { ProgressionModel.valueOf(activeMeso.progressionModel) }
                .getOrDefault(ProgressionModel.VOLUME_WAVE)
            val targets = MesocycleProgressionEngine.calculateWeekProgressionTargets(activeMeso.totalWeeks, model)
            val plans = targets.map { target ->
                val phaseCode = when {
                    target.isDeload -> "DELOAD"
                    target.weekNumber == 1 -> "INTRO_MEV"
                    target.weekNumber == 2 -> "ACCUMULATION_MAV"
                    target.weekNumber == 3 -> "OVERREACH_MRV"
                    else -> "PEAK_INTENSITY"
                }
                MesocycleWeekPlanEntity(
                    mesocycleId = activeMeso.id,
                    weekNumber = target.weekNumber,
                    phaseCode = phaseCode,
                    phaseNameUk = target.phaseNameUk,
                    isDeload = target.isDeload,
                    targetRir = target.targetRir,
                    setsMultiplier = target.setsMultiplier,
                    weightMultiplier = target.weightMultiplier,
                    targetRepsMin = target.repsTargetMin,
                    targetRepsMax = target.repsTargetMax,
                    plannedTotalSets = (54 * target.setsMultiplier).roundToInt(),
                    plannedTonnageKg = 23000.0 * target.setsMultiplier * target.weightMultiplier,
                    coachGuidelineUk = target.coachGuidelineUk
                )
            }
            dao.insertMesocycleWeekPlans(plans)
        }
    }

    suspend fun substituteWorkoutExercise(
        workoutExerciseId: Long,
        newExercise: ExerciseEntity
    ) {
        val existing = dao.getWorkoutExerciseById(workoutExerciseId) ?: return
        dao.updateWorkoutExercise(
            existing.copy(
                exerciseId = newExercise.id,
                exerciseName = newExercise.name,
                muscleGroup = newExercise.muscleGroup,
                targetRepsMin = newExercise.defaultTargetRepsMin,
                targetRepsMax = newExercise.defaultTargetRepsMax,
                targetRir = newExercise.defaultTargetRir
            )
        )
    }

    // Readiness Checkins
    val latestReadiness: Flow<ReadinessCheckinEntity?> = dao.getLatestReadinessCheckin()
    val readinessHistory: Flow<List<ReadinessCheckinEntity>> = dao.getReadinessHistory()
    suspend fun logReadinessCheckin(checkin: ReadinessCheckinEntity): Long =
        dao.insertReadinessCheckin(checkin)

    // Body Measurements
    val allBodyMeasurements: Flow<List<BodyMeasurementEntity>> = dao.getAllBodyMeasurements()
    suspend fun addBodyMeasurement(measurement: BodyMeasurementEntity): Long =
        dao.insertBodyMeasurement(measurement)
    suspend fun deleteBodyMeasurement(measurement: BodyMeasurementEntity) =
        dao.deleteBodyMeasurement(measurement)

    // Completed sets with exercise details for PR calculation
    val allCompletedSetsWithExercise: Flow<List<WorkoutSetWithExerciseInfo>> =
        dao.getAllCompletedSetsWithExercise()

    // Gym Profiles
    val allGymProfiles: Flow<List<GymProfileEntity>> = dao.getAllGymProfiles()
    val selectedGymProfile: Flow<GymProfileEntity?> = dao.getSelectedGymProfile()

    suspend fun selectGymProfile(profileId: Long) {
        val profiles = dao.getAllGymProfiles().firstOrNull() ?: return
        profiles.forEach { p ->
            dao.updateGymProfile(p.copy(isSelected = (p.id == profileId)))
        }
    }

    suspend fun addGymProfile(name: String, equipment: String): Long {
        return dao.insertGymProfile(
            GymProfileEntity(name = name, isSelected = false, availableEquipment = equipment)
        )
    }

    // Volume Config & Landmarks
    val userVolumeConfig: Flow<UserVolumeConfigEntity?> = dao.getUserVolumeConfig()

    suspend fun saveVolumeConfig(config: UserVolumeConfigEntity) {
        dao.saveUserVolumeConfig(config)
    }

    // Helper: calculate weekly sets per muscle group for current week
    fun calculateWeeklyVolumeLandmarks(
        config: UserVolumeConfigEntity?,
        completedSets: List<WorkoutSetWithExerciseInfo>
    ): List<VolumeLandmarkData> {
        val parsedConfig = if (config != null) {
            UserVolumeConfig(
                experienceLevel = runCatching { ExperienceLevel.valueOf(config.experienceLevel) }.getOrDefault(ExperienceLevel.INTERMEDIATE),
                sleepFactor = runCatching { SleepFactor.valueOf(config.sleepFactor) }.getOrDefault(SleepFactor.AVERAGE),
                nutritionFactor = runCatching { NutritionFactor.valueOf(config.nutritionFactor) }.getOrDefault(NutritionFactor.MAINTENANCE),
                stressFactor = runCatching { StressFactor.valueOf(config.stressFactor) }.getOrDefault(StressFactor.MODERATE),
                priorityMuscles = config.priorityMuscles.split(",").mapNotNull {
                    MuscleGroup.fromStringOrNull(it.trim())
                }.toSet()
            )
        } else {
            UserVolumeConfig()
        }

        // Tally sets per muscle group
        val counts = mutableMapOf<MuscleGroup, Int>()
        MuscleGroup.values().forEach { counts[it] = 0 }

        // Filter last 7 days completed working sets
        val oneWeekAgo = System.currentTimeMillis() - 7L * 24 * 3600 * 1000
        completedSets.filter { it.timestamp >= oneWeekAgo }.forEach { set ->
            val muscles = MuscleGroup.parseMuscleGroups(set.muscleGroup)
            for (muscle in muscles) {
                counts[muscle] = (counts[muscle] ?: 0) + 1
            }
        }

        return VolumeLandmarksCalculator.calculate(parsedConfig, counts)
    }

    // Data Export & Import (100% Privacy)
    suspend fun exportDataAsJson(): String {
        val json = JSONObject()
        val sessions = dao.getAllSessions().firstOrNull() ?: emptyList()
        val sessionsArray = JSONArray()

        for (s in sessions) {
            val sessionObj = JSONObject()
            sessionObj.put("title", s.title)
            sessionObj.put("dateTimestamp", s.dateTimestamp)
            sessionObj.put("totalTonnageKg", s.totalTonnageKg)
            sessionObj.put("durationSeconds", s.durationSeconds)
            sessionObj.put("isCompleted", s.isCompleted)

            val exercises = dao.getExercisesForSessionOnce(s.id)
            val exercisesArray = JSONArray()
            for (e in exercises) {
                val exObj = JSONObject()
                exObj.put("exerciseName", e.exerciseName)
                exObj.put("muscleGroup", e.muscleGroup)
                val sets = dao.getSetsForExerciseOnce(e.id)
                val setsArray = JSONArray()
                for (set in sets) {
                    val setObj = JSONObject()
                    setObj.put("weightKg", set.weightKg)
                    setObj.put("reps", set.reps)
                    setObj.put("rir", set.rir)
                    setObj.put("isCompleted", set.isCompleted)
                    setsArray.put(setObj)
                }
                exObj.put("sets", setsArray)
                exercisesArray.put(exObj)
            }
            sessionObj.put("exercises", exercisesArray)
            sessionsArray.put(sessionObj)
        }
        json.put("workouts", sessionsArray)

        val measurements = dao.getAllBodyMeasurements().firstOrNull() ?: emptyList()
        val measArray = JSONArray()
        for (m in measurements) {
            val mObj = JSONObject()
            mObj.put("dateTimestamp", m.dateTimestamp)
            mObj.put("weightKg", m.weightKg)
            mObj.put("waistCm", m.waistCm)
            mObj.put("bicepsCm", m.bicepsCm)
            mObj.put("chestCm", m.chestCm)
            mObj.put("bodyFatPercent", m.bodyFatPercent)
            measArray.put(mObj)
        }
        json.put("measurements", measArray)

        return json.toString(2)
    }

    suspend fun exportWorkoutHistoryCsv(): String {
        val sb = StringBuilder()
        sb.append("Дата,Тренування,Вправа,М'язова група,Сет,Вага (кг),Повтори,RIR,Тоннаж (кг)\n")
        val completedSets = dao.getAllCompletedSetsWithExercise().firstOrNull() ?: emptyList()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())

        for (set in completedSets) {
            val dateStr = sdf.format(java.util.Date(set.timestamp))
            val tonnage = set.weightKg * set.reps
            sb.append("\"$dateStr\",\"-\",\"${set.exerciseName}\",\"${set.muscleGroup}\",${set.setNumber},${set.weightKg},${set.reps},${set.rir},$tonnage\n")
        }
        return sb.toString()
    }

    // Helper template generator
    private suspend fun generateWorkoutsForMesocycle(mesocycleId: Long, programType: String) {
        generateWorkoutsForWeek(mesocycleId, 1, programType)
    }

    private suspend fun generateWorkoutsForWeek(mesocycleId: Long, week: Int, programType: String) {
        when {
            programType.contains("Upper / Lower", ignoreCase = true) -> {
                createUpperLowerWeek(mesocycleId, week)
            }
            programType.contains("Push / Pull / Legs", ignoreCase = true) -> {
                createPplWeek(mesocycleId, week)
            }
            programType.contains("Full Body", ignoreCase = true) -> {
                createFullBodyWeek(mesocycleId, week)
            }
            programType.contains("Arnold", ignoreCase = true) -> {
                createArnoldWeek(mesocycleId, week)
            }
            else -> {
                createUpperLowerWeek(mesocycleId, week)
            }
        }
    }

    private suspend fun createUpperLowerWeek(mesocycleId: Long, week: Int) {
        val days = listOf(
            "День 1: Верх тіла (Горизонтальний фокус)" to listOf(
                Triple("Жим штанги лежачи", "CHEST", 80.0),
                Triple("Тяга штанги в нахилі до пояса", "BACK", 75.0),
                Triple("Жим гантелей на похилій лаві (30°)", "CHEST", 28.0),
                Triple("Тяга горизонтального блоку до живота", "BACK", 60.0),
                Triple("Махи гантелями через сторони", "SHOULDERS", 12.0)
            ),
            "День 2: Низ тіла (Квадрицепс фокус)" to listOf(
                Triple("Присідання зі штангою на спині", "QUADS", 100.0),
                Triple("Румунська тяга зі штангою (RDL)", "HAMSTRINGS", 90.0),
                Triple("Жим ногами в тренажері 45°", "QUADS", 180.0),
                Triple("Згинання ніг лежачи в тренажері", "HAMSTRINGS", 45.0),
                Triple("Підйом на литки стоячи в тренажері", "CALVES", 50.0)
            ),
            "День 3: Верх тіла (Вертикальний фокус)" to listOf(
                Triple("Підтягування широким хватом", "BACK", 0.0),
                Triple("Жим штанги стоячи (Армійський жим)", "SHOULDERS", 50.0),
                Triple("Тяга верхнього блоку до грудей", "BACK", 65.0),
                Triple("Зведення рук у кросовері", "CHEST", 15.0),
                Triple("Підйом штанги на біцепс стоячи", "BICEPS", 35.0),
                Triple("Розгинання рук на блоці з канатом", "TRICEPS", 25.0),
                Triple("Згинання зап'ясть зі штангою сидячи", "FOREARMS", 25.0)
            ),
            "День 4: Низ тіла (Сідниці & Задня поверхня)" to listOf(
                Triple("Сідничний місток зі штангою (Hip Thrust)", "GLUTES", 110.0),
                Triple("Румунська тяга зі штангою (RDL)", "HAMSTRINGS", 95.0),
                Triple("Розгинання ніг у тренажері сидячи", "QUADS", 60.0),
                Triple("Підйом ніг у висі на турніку", "ABS", 0.0)
            )
        )

        for ((index, day) in days.withIndex()) {
            val sessionId = dao.insertSession(
                WorkoutSessionEntity(
                    mesocycleId = mesocycleId,
                    weekNumber = week,
                    title = day.first,
                    dateTimestamp = System.currentTimeMillis() + index * 24L * 3600 * 1000,
                    isCompleted = false
                )
            )
            for ((eIdx, exData) in day.second.withIndex()) {
                val weId = dao.insertWorkoutExercise(
                    WorkoutExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = 0,
                        exerciseName = exData.first,
                        muscleGroup = exData.second,
                        orderIndex = eIdx + 1
                    )
                )
                // 3 sets per exercise
                for (s in 1..3) {
                    dao.insertSet(
                        WorkoutSetEntity(
                            workoutExerciseId = weId,
                            setNumber = s,
                            weightKg = exData.third,
                            reps = 10,
                            targetRepsMin = 8,
                            targetRepsMax = 12,
                            rir = 2,
                            isWarmup = false,
                            isCompleted = false
                        )
                    )
                }
            }
        }
    }

    private suspend fun createPplWeek(mesocycleId: Long, week: Int) {
        val days = listOf(
            "День 1: Push (Груди, Дельти, Трицепс)" to listOf(
                Triple("Жим штанги лежачи", "CHEST", 80.0),
                Triple("Жим гантелей на похилій лаві (30°)", "CHEST", 28.0),
                Triple("Махи гантелями через сторони", "SHOULDERS", 12.0),
                Triple("Розгинання рук на блоці з канатом", "TRICEPS", 25.0)
            ),
            "День 2: Pull (Спина, Задня дельта, Біцепс)" to listOf(
                Triple("Підтягування широким хватом", "BACK", 0.0),
                Triple("Тяга штанги в нахилі до пояса", "BACK", 75.0),
                Triple("Відведення на задню дельту в кросовері (Face Pull)", "SHOULDERS", 20.0),
                Triple("Підйом штанги на біцепс стоячи", "BICEPS", 35.0)
            ),
            "День 3: Legs (Квадрицепси, Задня поверхня, Сідниці, Литки)" to listOf(
                Triple("Присідання зі штангою на спині", "QUADS", 100.0),
                Triple("Румунська тяга зі штангою (RDL)", "HAMSTRINGS", 90.0),
                Triple("Жим ногами в тренажері 45°", "QUADS", 180.0),
                Triple("Підйом на литки стоячи в тренажері", "CALVES", 50.0)
            )
        )
        insertDays(mesocycleId, week, days)
    }

    private suspend fun createFullBodyWeek(mesocycleId: Long, week: Int) {
        val days = listOf(
            "День 1: Full Body A" to listOf(
                Triple("Присідання зі штангою на спині", "QUADS", 95.0),
                Triple("Жим штанги лежачи", "CHEST", 80.0),
                Triple("Тяга горизонтального блоку до живота", "BACK", 65.0),
                Triple("Махи гантелями через сторони", "SHOULDERS", 12.0)
            ),
            "День 2: Full Body B" to listOf(
                Triple("Румунська тяга зі штангою (RDL)", "HAMSTRINGS", 90.0),
                Triple("Жим штанги стоячи (Армійський жим)", "SHOULDERS", 50.0),
                Triple("Підтягування широким хватом", "BACK", 0.0),
                Triple("Підйом штанги на біцепс стоячи", "BICEPS", 35.0)
            ),
            "День 3: Full Body C" to listOf(
                Triple("Жим ногами в тренажері 45°", "QUADS", 180.0),
                Triple("Жим гантелей на похилій лаві (30°)", "CHEST", 28.0),
                Triple("Тяга штанги в нахилі до пояса", "BACK", 75.0),
                Triple("Розгинання рук на блоці з канатом", "TRICEPS", 25.0)
            )
        )
        insertDays(mesocycleId, week, days)
    }

    private suspend fun createArnoldWeek(mesocycleId: Long, week: Int) {
        val days = listOf(
            "День 1: Груди та Спина (Антагоністи)" to listOf(
                Triple("Жим штанги лежачи", "CHEST", 85.0),
                Triple("Тяга штанги в нахилі до пояса", "BACK", 80.0),
                Triple("Жим гантелей на похилій лаві (30°)", "CHEST", 30.0),
                Triple("Тяга верхнього блоку до грудей", "BACK", 70.0)
            ),
            "День 2: Плечі та Руки" to listOf(
                Triple("Жим штанги стоячи (Армійський жим)", "SHOULDERS", 55.0),
                Triple("Махи гантелями через сторони", "SHOULDERS", 12.0),
                Triple("Підйом штанги на біцепс стоячи", "BICEPS", 38.0),
                Triple("Французький жим з EZ-грифом лежачи", "TRICEPS", 35.0)
            ),
            "День 3: Ноги" to listOf(
                Triple("Присідання зі штангою на спині", "QUADS", 105.0),
                Triple("Румунська тяга зі штангою (RDL)", "HAMSTRINGS", 95.0),
                Triple("Жим ногами в тренажері 45°", "QUADS", 200.0),
                Triple("Підйом на литки стоячи в тренажері", "CALVES", 55.0)
            )
        )
        insertDays(mesocycleId, week, days)
    }

data class MuscleTargetSlot(
    val slotName: String,
    val muscleGroup: String,
    val defaultWeightKg: Double,
    val targetSets: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetRir: Int,
    val quantityGuidelineUk: String,
    val howToPerformUk: String,
    val upToWhatPointUk: String
)

    private suspend fun createChestBicepsSplitWeek(mesocycleId: Long, week: Int) {
        val days = listOf(
            "День 1: Груди та Біцепс" to listOf(
                MuscleTargetSlot(
                    slotName = "Грудні м'язи (Базовий горизонтальний жим)",
                    muscleGroup = "CHEST",
                    defaultWeightKg = 70.0,
                    targetSets = 3,
                    targetRepsMin = 6,
                    targetRepsMax = 10,
                    targetRir = 2,
                    quantityGuidelineUk = "3–4 робочі підходи по 6–10 повторів",
                    howToPerformUk = "Будь-який базовий жим (штанга, гантелі або хаммер). Опуск 2–3 сек (контроль ексцентрики), лікті ~60° до корпусу.",
                    upToWhatPointUk = "Зупинятися за 1–2 повтори до відмови (RIR 1–2), не відриваючи таз від лави."
                ),
                MuscleTargetSlot(
                    slotName = "Грудні м'язи (Похилий жим 30° / Верх грудей)",
                    muscleGroup = "CHEST",
                    defaultWeightKg = 26.0,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 12,
                    targetRir = 2,
                    quantityGuidelineUk = "3 підходи по 8–12 повторів",
                    howToPerformUk = "Жим на похилій лаві 30° (гантелі, штанга або Сміт). Повне розтягнення ключичної частини грудних.",
                    upToWhatPointUk = "Зупинятися за 2 повтори до відмови (RIR 2), зберігаючи контроль ліктів."
                ),
                MuscleTargetSlot(
                    slotName = "Грудні м'язи (Ізоляційне зведення)",
                    muscleGroup = "CHEST",
                    defaultWeightKg = 15.0,
                    targetSets = 3,
                    targetRepsMin = 10,
                    targetRepsMax = 15,
                    targetRir = 1,
                    quantityGuidelineUk = "2–3 підходи по 10–15 повторів",
                    howToPerformUk = "Кросовер, метелик (Pec-Deck) або розведення. Фіксація ліктів, пауза 1 сек у піковому скороченні.",
                    upToWhatPointUk = "Робити майже до відмови (RIR 1), фокусуючись на максимальному печінні м'яза."
                ),
                MuscleTargetSlot(
                    slotName = "Біцепс (Базове двоголове згинання)",
                    muscleGroup = "BICEPS",
                    defaultWeightKg = 30.0,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 10,
                    targetRir = 2,
                    quantityGuidelineUk = "3 підходи по 8–10 повторів",
                    howToPerformUk = "Згинання стоячи зі штангою, EZ-грифом чи гантелями. Лікті строго зафіксовані біля боків, без розгойдування.",
                    upToWhatPointUk = "Зупинятися за 1–2 повтори до відмови (RIR 1–2), без допомоги спини."
                ),
                MuscleTargetSlot(
                    slotName = "Біцепс (Згинання в розтягнутій позиції)",
                    muscleGroup = "BICEPS",
                    defaultWeightKg = 12.0,
                    targetSets = 3,
                    targetRepsMin = 10,
                    targetRepsMax = 12,
                    targetRir = 1,
                    quantityGuidelineUk = "2–3 підходи по 10–12 повторів",
                    howToPerformUk = "Згинання на похилій лаві 45-60° або на нижньому блоці з супінацією (розвертанням кисті назовні).",
                    upToWhatPointUk = "Працювати до RIR 1 (глибоке локальне виснаження довгої головки біцепса)."
                )
            ),
            "День 2: Спина та Тріцепс" to listOf(
                MuscleTargetSlot(
                    slotName = "Спина (Вертикальна тяга до грудей)",
                    muscleGroup = "BACK",
                    defaultWeightKg = 60.0,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 10,
                    targetRir = 2,
                    quantityGuidelineUk = "3–4 підходи по 8–10 повторів",
                    howToPerformUk = "Підтягування або верхній блок будь-яким хватом. Рух починати зведенням лопаток, лікті тягнути до тазу.",
                    upToWhatPointUk = "Зупинятися за 1–2 повтори до втрати амплітуди (RIR 1–2)."
                ),
                MuscleTargetSlot(
                    slotName = "Спина (Горизонтальна тяга до пояса)",
                    muscleGroup = "BACK",
                    defaultWeightKg = 65.0,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 12,
                    targetRir = 2,
                    quantityGuidelineUk = "3–4 підходи по 8–12 повторів",
                    howToPerformUk = "Тяга в нахилі (штанга, гантель або горизонтальний блок). Спина рівна, максимальне зведення лопаток у кінці.",
                    upToWhatPointUk = "RIR 1–2 (зупинятися до того, як почне підключатися інерція корпусу)."
                ),
                MuscleTargetSlot(
                    slotName = "Тріцепс (Базовий розгинальний рух)",
                    muscleGroup = "TRICEPS",
                    defaultWeightKg = 30.0,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 10,
                    targetRir = 2,
                    quantityGuidelineUk = "3 підходи по 8–10 повторів",
                    howToPerformUk = "Французький жим, відтискання на брусах або жим вузьким хватом. Опускання за верхівку голови.",
                    upToWhatPointUk = "Зупинятися за 2 повтори до відмови (RIR 2) для захисту ліктьових зв'язок."
                ),
                MuscleTargetSlot(
                    slotName = "Тріцепс (Ізоляційне розгинання на блоці)",
                    muscleGroup = "TRICEPS",
                    defaultWeightKg = 25.0,
                    targetSets = 3,
                    targetRepsMin = 10,
                    targetRepsMax = 15,
                    targetRir = 1,
                    quantityGuidelineUk = "3 підходи по 10–15 повторів",
                    howToPerformUk = "Розгинання рук на блоці з канатом або прямою планкою. Повне розгинання в ліктях у нижній точці.",
                    upToWhatPointUk = "Працювати до RIR 1 (майже до відмови та максимального печіння)."
                ),
                MuscleTargetSlot(
                    slotName = "Передпліччя (Згинання зап'ясть або зворотний хват)",
                    muscleGroup = "FOREARMS",
                    defaultWeightKg = 25.0,
                    targetSets = 3,
                    targetRepsMin = 12,
                    targetRepsMax = 15,
                    targetRir = 1,
                    quantityGuidelineUk = "3 підходи по 12–15 повторів",
                    howToPerformUk = "Згинання зап'ясть сидячи на лаві або згинання зі штангою зворотним хватом (брахіорадіаліс).",
                    upToWhatPointUk = "Працювати до RIR 1 (до сильного печіння та пампу в передпліччях)."
                )
            ),
            "День 3: Плечі та Прес" to listOf(
                MuscleTargetSlot(
                    slotName = "Плечі (Вертикальний жим над головою)",
                    muscleGroup = "SHOULDERS",
                    defaultWeightKg = 45.0,
                    targetSets = 3,
                    targetRepsMin = 6,
                    targetRepsMax = 10,
                    targetRir = 2,
                    quantityGuidelineUk = "3 підходи по 6–10 повторів",
                    howToPerformUk = "Жим стоячи чи сидячи (штанга, гантелі або тренажер). Прес напружений, без прогину в попереку.",
                    upToWhatPointUk = "Зупинятися за 2 повтори до відмови (RIR 2) задля стабільності плечових суглобів."
                ),
                MuscleTargetSlot(
                    slotName = "Середня дельта (Махи / Відведення в сторони)",
                    muscleGroup = "SHOULDERS",
                    defaultWeightKg = 12.0,
                    targetSets = 4,
                    targetRepsMin = 12,
                    targetRepsMax = 15,
                    targetRir = 1,
                    quantityGuidelineUk = "3–4 підходи по 12–15 повторів",
                    howToPerformUk = "Махи через сторони гантелями, в кросовері чи тренажері. Лікті трохи зігнуті та ведуть рух.",
                    upToWhatPointUk = "Працювати близько до повної відмови (RIR 0–1). Дозволено мікро-чітінг лише на останньому сеті."
                ),
                MuscleTargetSlot(
                    slotName = "Задня дельта (Горизонтальне розведення)",
                    muscleGroup = "SHOULDERS",
                    defaultWeightKg = 20.0,
                    targetSets = 3,
                    targetRepsMin = 12,
                    targetRepsMax = 15,
                    targetRir = 1,
                    quantityGuidelineUk = "3 підходи по 12–15 повторів",
                    howToPerformUk = "Face pull на блоці або зворотні розведення у метелику. Пальці на рівні вух, зведення лопаток.",
                    upToWhatPointUk = "RIR 1 (до відчуття паління в задніх пучках дельт)."
                ),
                MuscleTargetSlot(
                    slotName = "Прес (Динамічне скручування / Підйом ніг)",
                    muscleGroup = "ABS",
                    defaultWeightKg = 0.0,
                    targetSets = 3,
                    targetRepsMin = 12,
                    targetRepsMax = 20,
                    targetRir = 1,
                    quantityGuidelineUk = "3 підходи по 12–20 повторів",
                    howToPerformUk = "Підйом ніг у висі, скручування на блоці «Молитва» чи римському стільці. Обов'язкове підкручування тазу.",
                    upToWhatPointUk = "До сильного печіння в пресі (RIR 1), не задіюючи шию."
                )
            ),
            "День 4: Ноги (Квадрицепси, Задня поверхня, Сідниці, Литки)" to listOf(
                MuscleTargetSlot(
                    slotName = "Квадрицепси (Базовий присідальний рух)",
                    muscleGroup = "QUADS",
                    defaultWeightKg = 90.0,
                    targetSets = 3,
                    targetRepsMin = 6,
                    targetRepsMax = 10,
                    targetRir = 2,
                    quantityGuidelineUk = "3–4 підходи по 6–10 повторів",
                    howToPerformUk = "Присідання зі штангою, гак-присідання чи жим ногами. Плавний опуск 2-3с, коліна в площині носків.",
                    upToWhatPointUk = "Зупинятися за 2 повтори до відмови (RIR 2) для безпеки попереку."
                ),
                MuscleTargetSlot(
                    slotName = "Задня поверхня стегна (Тазово-домінантний рух)",
                    muscleGroup = "HAMSTRINGS",
                    defaultWeightKg = 80.0,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 10,
                    targetRir = 2,
                    quantityGuidelineUk = "3 підходи по 8–10 повторів",
                    howToPerformUk = "Румунська тяга (RDL) зі штангою чи гантелями. Відведення тазу назад до натягу задньої поверхні стегна.",
                    upToWhatPointUk = "RIR 2 (спина строго пряма, рух припиняється, якщо починає згинатися поперек)."
                ),
                MuscleTargetSlot(
                    slotName = "Задня поверхня стегна (Ізоляційне згинання)",
                    muscleGroup = "HAMSTRINGS",
                    defaultWeightKg = 40.0,
                    targetSets = 3,
                    targetRepsMin = 10,
                    targetRepsMax = 12,
                    targetRir = 1,
                    quantityGuidelineUk = "3 підходи по 10–12 повторів",
                    howToPerformUk = "Згинання гомілок лежачи чи сидячи в тренажері. Таз зафіксований, повільне повернення.",
                    upToWhatPointUk = "Працювати до RIR 1 (майже до відмови)."
                ),
                MuscleTargetSlot(
                    slotName = "Литки (Підйоми на носки)",
                    muscleGroup = "CALVES",
                    defaultWeightKg = 50.0,
                    targetSets = 3,
                    targetRepsMin = 12,
                    targetRepsMax = 15,
                    targetRir = 1,
                    quantityGuidelineUk = "3–4 підходи по 12–15 повторів",
                    howToPerformUk = "Стоячи або в тренажері. Обов'язкова пауза 2 сек у розтягнутій нижній точці перед підйомом.",
                    upToWhatPointUk = "До повного виснаження (RIR 0–1)."
                )
            )
        )
        insertSlotDays(mesocycleId, week, days)
    }

    private suspend fun insertSlotDays(
        mesocycleId: Long,
        week: Int,
        days: List<Pair<String, List<MuscleTargetSlot>>>
    ) {
        for ((index, day) in days.withIndex()) {
            val sessionId = dao.insertSession(
                WorkoutSessionEntity(
                    mesocycleId = mesocycleId,
                    weekNumber = week,
                    title = day.first,
                    dateTimestamp = System.currentTimeMillis() + index * 24L * 3600 * 1000,
                    isCompleted = false
                )
            )
            for ((eIdx, slot) in day.second.withIndex()) {
                val fullGuideline = "📊 Кількість: ${slot.quantityGuidelineUk} | ⚙️ Як робити: ${slot.howToPerformUk} | 🛑 До якого моменту: ${slot.upToWhatPointUk}"
                val weId = dao.insertWorkoutExercise(
                    WorkoutExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = 0,
                        exerciseName = slot.slotName,
                        muscleGroup = slot.muscleGroup,
                        orderIndex = eIdx + 1,
                        notes = fullGuideline
                    )
                )
                for (s in 1..slot.targetSets) {
                    dao.insertSet(
                        WorkoutSetEntity(
                            workoutExerciseId = weId,
                            setNumber = s,
                            weightKg = slot.defaultWeightKg,
                            reps = slot.targetRepsMin,
                            targetRepsMin = slot.targetRepsMin,
                            targetRepsMax = slot.targetRepsMax,
                            rir = slot.targetRir,
                            isWarmup = false,
                            isCompleted = false
                        )
                    )
                }
            }
        }
    }

    private suspend fun insertDays(
        mesocycleId: Long,
        week: Int,
        days: List<Pair<String, List<Triple<String, String, Double>>>>
    ) {
        for ((index, day) in days.withIndex()) {
            val sessionId = dao.insertSession(
                WorkoutSessionEntity(
                    mesocycleId = mesocycleId,
                    weekNumber = week,
                    title = day.first,
                    dateTimestamp = System.currentTimeMillis() + index * 24L * 3600 * 1000,
                    isCompleted = false
                )
            )
            for ((eIdx, exData) in day.second.withIndex()) {
                val weId = dao.insertWorkoutExercise(
                    WorkoutExerciseEntity(
                        sessionId = sessionId,
                        exerciseId = 0,
                        exerciseName = exData.first,
                        muscleGroup = exData.second,
                        orderIndex = eIdx + 1
                    )
                )
                for (s in 1..3) {
                    dao.insertSet(
                        WorkoutSetEntity(
                            workoutExerciseId = weId,
                            setNumber = s,
                            weightKg = exData.third,
                            reps = 10,
                            targetRepsMin = 8,
                            targetRepsMax = 12,
                            rir = 2,
                            isWarmup = false,
                            isCompleted = false
                        )
                    )
                }
            }
        }
    }

    suspend fun resetAllProgress() {
        dao.resetAllProgress()
    }

    suspend fun cleanupEmptyCancelledSessions() {
        dao.cleanupEmptyCancelledSessions()
    }
}
