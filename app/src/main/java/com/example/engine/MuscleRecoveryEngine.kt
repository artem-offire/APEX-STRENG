package com.example.engine

import com.example.data.WorkoutSetWithExerciseInfo
import com.example.model.MuscleGroup
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

enum class RecoveryState(val displayNameUk: String) {
    RECOVERED("Відновлено"),       // 100% готовність
    RECOVERING("Відновлюється"),   // 40% - 99%
    EXHAUSTED("Виснажено")        // < 40% (гостра фаза втоми)
}

data class MuscleGroupRecoveryStatus(
    val muscleGroup: MuscleGroup,
    val recoveryHoursNeeded: Int, // Фізіологічний інтервал: 48, 60 або 72 години
    val lastTrainedTimestamp: Long?,
    val setsInLastSession: Int,
    val elapsedHours: Double,
    val hoursRemaining: Double,
    val recoveryPercent: Int, // 0..100
    val state: RecoveryState,
    val becameReadyToday: Boolean = false, // Відновлення завершилося в межах останніх 24 годин (пік суперкомпенсації)
    val nextOptimalWorkoutTimeMs: Long?,
    val formattedNextTimeUk: String,
    val formattedLastTrainedUk: String,
    val recommendationMessageUk: String
)

data class MuscleRecoverySummary(
    val statuses: List<MuscleGroupRecoveryStatus>,
    val readyMuscleGroups: List<MuscleGroup>,
    val readyTodayMuscleGroups: List<MuscleGroup> = emptyList(), // М'язи, що стали доступні саме сьогодні
    val recoveringMuscleGroups: List<MuscleGroup>,
    val recommendedWorkoutTitleUk: String,
    val recommendationSummaryUk: String,
    val nextMuscleReadyTextUk: String?
)

object MuscleRecoveryEngine {

    /**
     * Фіксований фізіологічний інтервал відновлення (48–72 години) згідно з науковими даними
     * гіпертрофії та синтезу м'язового білка (MPS, Dr. Mike Israetel, Brad Schoenfeld):
     * - Великі м'язові групи (глибоке пошкодження волокон та системна втома): 72 години (3 дні)
     * - Середні м'язові групи (грудні, плечі): 60 годин (2.5 дні)
     * - Малі та швидко відновлювані м'язові групи (руки, ікри, прес): 48 годин (2 дні)
     */
    fun getPhysiologicalRecoveryHours(muscleGroup: MuscleGroup): Int {
        return when (muscleGroup) {
            MuscleGroup.BACK -> 72
            MuscleGroup.QUADS -> 72
            MuscleGroup.HAMSTRINGS -> 72
            MuscleGroup.GLUTES -> 72
            MuscleGroup.CHEST -> 60
            MuscleGroup.SHOULDERS -> 60
            MuscleGroup.BICEPS -> 48
            MuscleGroup.TRICEPS -> 48
            MuscleGroup.FOREARMS -> 48
            MuscleGroup.CALVES -> 48
            MuscleGroup.ABS -> 48
            MuscleGroup.OTHER -> 48
        }
    }

    fun computeRecoverySummary(
        completedSets: List<WorkoutSetWithExerciseInfo>,
        nowMs: Long = System.currentTimeMillis()
    ): MuscleRecoverySummary {
        // Group completed working sets by MuscleGroup
        val setsByMuscle = mutableMapOf<MuscleGroup, MutableList<WorkoutSetWithExerciseInfo>>()
        MuscleGroup.values().forEach { setsByMuscle[it] = mutableListOf() }

        for (set in completedSets) {
            if (!set.isCompleted || set.isWarmup) continue
            val groups = MuscleGroup.parseMuscleGroups(set.muscleGroup)
            for (group in groups) {
                setsByMuscle[group]?.add(set)
            }
        }

        // Biological muscle groups for physiological recovery tracking (excluding generic OTHER)
        val targetMuscles = MuscleGroup.values().filter { it != MuscleGroup.OTHER }
        val statuses = targetMuscles.map { muscle ->
            calculateStatusForMuscle(muscle, setsByMuscle[muscle] ?: emptyList(), nowMs)
        }

        val readyGroups = statuses
            .filter { it.state == RecoveryState.RECOVERED }
            .map { it.muscleGroup }

        val readyTodayGroups = statuses
            .filter { it.becameReadyToday }
            .map { it.muscleGroup }

        val recoveringGroups = statuses
            .filter { it.state != RecoveryState.RECOVERED }
            .sortedBy { it.hoursRemaining }
            .map { it.muscleGroup }

        // Recommended workout split based on fully recovered muscles
        val (recommendedTitle, summaryText) = determineRecommendedWorkout(readyGroups, statuses)

        val nextMuscleReadyText = statuses
            .filter { it.state != RecoveryState.RECOVERED && it.hoursRemaining > 0 }
            .minByOrNull { it.hoursRemaining }
            ?.let { nextMuscle ->
                "Наступний на черзі: ${nextMuscle.muscleGroup.displayNameUk} (${nextMuscle.formattedNextTimeUk})"
            }

        return MuscleRecoverySummary(
            statuses = statuses,
            readyMuscleGroups = readyGroups,
            readyTodayMuscleGroups = readyTodayGroups,
            recoveringMuscleGroups = recoveringGroups,
            recommendedWorkoutTitleUk = recommendedTitle,
            recommendationSummaryUk = summaryText,
            nextMuscleReadyTextUk = nextMuscleReadyText
        )
    }

    private fun calculateStatusForMuscle(
        muscle: MuscleGroup,
        sets: List<WorkoutSetWithExerciseInfo>,
        nowMs: Long
    ): MuscleGroupRecoveryStatus {
        val baseHours = getPhysiologicalRecoveryHours(muscle)

        if (sets.isEmpty()) {
            return MuscleGroupRecoveryStatus(
                muscleGroup = muscle,
                recoveryHoursNeeded = baseHours,
                lastTrainedTimestamp = null,
                setsInLastSession = 0,
                elapsedHours = Double.MAX_VALUE,
                hoursRemaining = 0.0,
                recoveryPercent = 100,
                state = RecoveryState.RECOVERED,
                nextOptimalWorkoutTimeMs = null,
                formattedNextTimeUk = "Готовий зараз",
                formattedLastTrainedUk = "Ще не тренувався",
                recommendationMessageUk = "М'яз повністю свіжий і готовий до прогресивного навантаження."
            )
        }

        // Find latest timestamp
        val sortedSets = sets.sortedByDescending { it.timestamp }
        val latestTimestamp = sortedSets.first().timestamp

        // Count sets in that latest session (within 6 hours of latest set)
        val sessionThreshold = 6 * 3600 * 1000L
        val setsInLatestSession = sortedSets.count { (latestTimestamp - it.timestamp) < sessionThreshold }

        val elapsedMs = (nowMs - latestTimestamp).coerceAtLeast(0L)
        val elapsedHours = elapsedMs / (1000.0 * 3600.0)

        val percent = ((elapsedHours / baseHours.toDouble()) * 100.0)
            .coerceIn(0.0, 100.0)
            .roundToInt()

        val hoursRemaining = (baseHours.toDouble() - elapsedHours).coerceAtLeast(0.0)
        val nextTimeMs = latestTimestamp + (baseHours * 3600 * 1000L)

        val state = when {
            percent >= 100 -> RecoveryState.RECOVERED
            percent >= 40 -> RecoveryState.RECOVERING
            else -> RecoveryState.EXHAUSTED
        }

        val formattedNext = if (state == RecoveryState.RECOVERED) {
            "Готовий зараз"
        } else {
            formatNextOptimalTime(nextTimeMs, nowMs, hoursRemaining)
        }

        val formattedLastTrained = formatLastTrainedTime(latestTimestamp, nowMs)

        val recommendationMsg = when (state) {
            RecoveryState.RECOVERED -> {
                "Суперкомпенсація досягнута (${baseHours} год). Оптимальний час для нового тренування та прогресії!"
            }
            RecoveryState.RECOVERING -> {
                val remainingInt = hoursRemaining.roundToInt().coerceAtLeast(1)
                "Триває активне відновлення білка (~$remainingInt год залишилось). Не перевантажуйте передчасно."
            }
            RecoveryState.EXHAUSTED -> {
                val remainingInt = hoursRemaining.roundToInt().coerceAtLeast(1)
                "Гостра фаза втоми після недавнього тренування ($setsInLatestSession підх.). М'язи потребують спокою ще ~$remainingInt год."
            }
        }

        val becameReadyToday = state == RecoveryState.RECOVERED && (elapsedHours - baseHours.toDouble()) in 0.0..24.0

        return MuscleGroupRecoveryStatus(
            muscleGroup = muscle,
            recoveryHoursNeeded = baseHours,
            lastTrainedTimestamp = latestTimestamp,
            setsInLastSession = setsInLatestSession,
            elapsedHours = elapsedHours,
            hoursRemaining = hoursRemaining,
            recoveryPercent = percent,
            state = state,
            becameReadyToday = becameReadyToday,
            nextOptimalWorkoutTimeMs = nextTimeMs,
            formattedNextTimeUk = formattedNext,
            formattedLastTrainedUk = formattedLastTrained,
            recommendationMessageUk = recommendationMsg
        )
    }

    private fun formatNextOptimalTime(targetMs: Long, nowMs: Long, hoursRemaining: Double): String {
        val remHours = hoursRemaining.roundToInt()
        if (remHours <= 0) return "Готовий зараз"
        if (remHours < 24) {
            val timeSdf = SimpleDateFormat("HH:mm", Locale("uk", "UA"))
            return "Сьогодні о ${timeSdf.format(Date(targetMs))} (~$remHours год)"
        }

        val targetCal = Calendar.getInstance().apply { timeInMillis = targetMs }
        val nowCal = Calendar.getInstance().apply { timeInMillis = nowMs }

        val dayDiff = targetCal.get(Calendar.DAY_OF_YEAR) - nowCal.get(Calendar.DAY_OF_YEAR)
        val timeSdf = SimpleDateFormat("HH:mm", Locale("uk", "UA"))
        val formattedTime = timeSdf.format(Date(targetMs))

        return when (dayDiff) {
            1 -> "Завтра о $formattedTime (~$remHours год)"
            2 -> "Післязавтра о $formattedTime"
            else -> {
                val daySdf = SimpleDateFormat("EEEE, HH:mm", Locale("uk", "UA"))
                daySdf.format(Date(targetMs)).replaceFirstChar { it.uppercase() }
            }
        }
    }

    private fun formatLastTrainedTime(timestamp: Long, nowMs: Long): String {
        val diffHours = ((nowMs - timestamp) / (1000.0 * 3600.0)).roundToInt()
        if (diffHours < 1) return "Щойно"
        if (diffHours < 24) return "$diffHours год тому"
        val days = diffHours / 24
        if (days == 1) return "Вчора"
        return "$days дн. тому"
    }

    private fun determineRecommendedWorkout(
        readyGroups: List<MuscleGroup>,
        statuses: List<MuscleGroupRecoveryStatus>
    ): Pair<String, String> {
        val hasChest = readyGroups.contains(MuscleGroup.CHEST)
        val hasBack = readyGroups.contains(MuscleGroup.BACK)
        val hasQuads = readyGroups.contains(MuscleGroup.QUADS)
        val hasHamstrings = readyGroups.contains(MuscleGroup.HAMSTRINGS)
        val hasShoulders = readyGroups.contains(MuscleGroup.SHOULDERS)
        val hasTriceps = readyGroups.contains(MuscleGroup.TRICEPS)
        val hasBiceps = readyGroups.contains(MuscleGroup.BICEPS)

        // Split synergistic evaluation
        return when {
            hasChest && (hasTriceps || hasShoulders) -> {
                "Жим: Грудні та Дельти" to
                    "Грудні та штовхаючі м'язи повністю відновлені. Ідеальний день для жимового тренування."
            }
            hasBack && hasBiceps -> {
                "Тяга: Спина та Біцепс" to
                    "Найширші та біцепс готові до важкої тяги. Суперкомпенсація досягла піку."
            }
            hasQuads || hasHamstrings -> {
                "День ніг: Квадрицепси та Сідниці" to
                    "Нижня частина тіла відпочила 72 години і готова до максимального навантаження."
            }
            readyGroups.isNotEmpty() -> {
                val names = readyGroups.take(3).joinToString(", ") { it.displayNameUk }
                "Тренування: $names" to
                    "Ці групи повністю пройшли 48–72 год відновлення і готові до нової стимуляції."
            }
            else -> {
                val nextReady = statuses.minByOrNull { it.hoursRemaining }
                val nextName = nextReady?.muscleGroup?.displayNameUk ?: "м'язи"
                val hours = nextReady?.hoursRemaining?.roundToInt() ?: 12
                "День активного відновлення" to
                    "Всі ключові групи м'язів ще перебувають у процесі регенерації. Наступним буде готовий $nextName (~$hours год)."
            }
        }
    }
}
