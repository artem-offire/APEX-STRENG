package com.example.model

import com.example.data.WorkoutSessionEntity
import com.example.data.WorkoutSetWithExerciseInfo
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class DailyGoalItem(
    val id: String,
    val titleUk: String,
    val descriptionUk: String,
    val exerciseName: String?,
    val targetValue: String,
    val isCompleted: Boolean,
    val iconEmoji: String
)

data class DailyGoalsState(
    val isReady: Boolean,
    val daysActive: Int,
    val requiredDays: Int = 7,
    val goals: List<DailyGoalItem>,
    val completedCount: Int,
    val statusMessageUk: String
)

object DailyGoalsEngine {

    fun computeDailyGoals(
        sessions: List<WorkoutSessionEntity>,
        completedSets: List<WorkoutSetWithExerciseInfo>,
        manualCheckedGoalIds: Set<String>
    ): DailyGoalsState {
        val completedSessions = sessions.filter { it.isCompleted }
        if (completedSessions.isEmpty()) {
            return DailyGoalsState(
                isReady = false,
                daysActive = 1,
                requiredDays = 7,
                goals = emptyList(),
                completedCount = 0,
                statusMessageUk = "День 1 з 7: виконуйте тренування для аналізу ваших регулярних вправ."
            )
        }

        val now = System.currentTimeMillis()
        val firstSessionTime = completedSessions.minOf { it.dateTimestamp }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val distinctDays = completedSessions.map { dateFormat.format(Date(it.dateTimestamp)) }.distinct().size
        val daysElapsed = TimeUnit.MILLISECONDS.toDays(now - firstSessionTime).toInt() + 1
        val daysActive = maxOf(distinctDays, daysElapsed).coerceIn(1, 7)

        val isReady = daysElapsed >= 7 || distinctDays >= 7

        if (!isReady) {
            val daysLeft = 7 - daysActive
            return DailyGoalsState(
                isReady = false,
                daysActive = daysActive,
                requiredDays = 7,
                goals = emptyList(),
                completedCount = 0,
                statusMessageUk = "День $daysActive з 7: аналіз ваших регулярних вправ. Персональні цілі відкриються через $daysLeft дн."
            )
        }

        // 1 week reached! Find top repeated exercises
        val exerciseCounts = completedSets
            .filter { it.exerciseName.isNotBlank() }
            .groupBy { it.exerciseName }
            .mapValues { entry ->
                val sets = entry.value
                val maxW = sets.maxOfOrNull { it.weightKg } ?: 0.0
                val totalReps = sets.sumOf { it.reps }
                Triple(sets.size, maxW, totalReps)
            }
            .toList()
            .sortedByDescending { it.second.first }

        val todayStr = dateFormat.format(Date(now))
        val todaySets = completedSets.filter {
            dateFormat.format(Date(it.timestamp)) == todayStr
        }

        val goals = mutableListOf<DailyGoalItem>()

        if (exerciseCounts.isNotEmpty()) {
            val (fav1, stats1) = exerciseCounts[0]
            val todaySetsFav1 = todaySets.filter { it.exerciseName == fav1 }.size
            val isFav1Done = todaySetsFav1 >= 3 || manualCheckedGoalIds.contains("goal_rep_1")
            goals.add(
                DailyGoalItem(
                    id = "goal_rep_1",
                    titleUk = "Закріпити $fav1",
                    descriptionUk = "Ваша найчастіша вправа. Виконайте щонайменше 3 підходи сьогодні.",
                    exerciseName = fav1,
                    targetValue = "3 підходи (робоча вага: ${stats1.second.toInt()} кг)",
                    isCompleted = isFav1Done,
                    iconEmoji = "🎯"
                )
            )
        }

        if (exerciseCounts.size > 1) {
            val (fav2, _) = exerciseCounts[1]
            val todaySetsFav2 = todaySets.filter { it.exerciseName == fav2 }.size
            val isFav2Done = todaySetsFav2 >= 2 || manualCheckedGoalIds.contains("goal_rep_2")
            goals.add(
                DailyGoalItem(
                    id = "goal_rep_2",
                    titleUk = "Технічний сет: $fav2",
                    descriptionUk = "Регулярна вправа. Виконайте мінімум 2 якісних робочих підходи.",
                    exerciseName = fav2,
                    targetValue = "2 підходи",
                    isCompleted = isFav2Done,
                    iconEmoji = "⚡"
                )
            )
        }

        // Volume goal based on athlete history
        val avgTonnage = if (completedSessions.isNotEmpty()) {
            (completedSessions.sumOf { it.totalTonnageKg } / completedSessions.size).toInt()
        } else 1000
        val targetTonnage = (avgTonnage * 0.7).toInt().coerceAtLeast(500)
        val todayTonnage = todaySets.sumOf { it.weightKg * it.reps }.toInt()
        val isTonnageDone = todayTonnage >= targetTonnage || manualCheckedGoalIds.contains("goal_tonnage")

        goals.add(
            DailyGoalItem(
                id = "goal_tonnage",
                titleUk = "Денний цільовий тоннаж",
                descriptionUk = "Підняти не менше $targetTonnage кг сумарного тоннажу за тренування.",
                exerciseName = null,
                targetValue = "$targetTonnage кг (зараз: $todayTonnage кг)",
                isCompleted = isTonnageDone,
                iconEmoji = "🏋️"
            )
        )

        val completedCount = goals.count { it.isCompleted }

        return DailyGoalsState(
            isReady = true,
            daysActive = daysActive,
            requiredDays = 7,
            goals = goals,
            completedCount = completedCount,
            statusMessageUk = "Цілі сформовано на основі ваших найчастіших вправ за тиждень!"
        )
    }
}
