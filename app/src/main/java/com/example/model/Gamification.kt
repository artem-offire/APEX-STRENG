package com.example.model

import com.example.data.WorkoutSessionEntity
import com.example.data.WorkoutSetWithExerciseInfo
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

enum class AchievementCategory(val titleUk: String) {
    WORKOUTS("Тренування"),
    SETS("Робочі підходи"),
    CONSISTENCY("Дисципліна"),
    CUSTOM("Власні рекорди")
}

data class Achievement(
    val id: String,
    val title: String,
    val description: String,
    val iconEmoji: String,
    val xpReward: Int,
    val currentProgress: Int,
    val maxProgress: Int,
    val isUnlocked: Boolean,
    val category: AchievementCategory
) {
    val progressPercent: Float
        get() = if (maxProgress > 0) (currentProgress.toFloat() / maxProgress).coerceIn(0f, 1f) else 1f
}

data class AthleteGamificationState(
    val level: Int,
    val rankTitle: String,
    val currentXp: Int,
    val nextLevelXp: Int,
    val levelBaseXp: Int,
    val levelProgressPercent: Float,
    val totalWorkouts: Int,
    val totalSets: Int,
    val totalTonnageKg: Double,
    val currentStreak: Int,
    val achievements: List<Achievement>,
    val unlockedCount: Int
)

object GamificationEngine {

    private val LEVEL_THRESHOLDS = listOf(
        0 to "Початківець",
        300 to "Атлет",
        700 to "Залізний воїн",
        1300 to "Майстер навантажень",
        2100 to "Титан сили",
        3200 to "Легенда залу",
        4600 to "Apex Чемпіон"
    )

    fun calculateGamificationState(
        sessions: List<WorkoutSessionEntity>,
        completedSets: List<WorkoutSetWithExerciseInfo>,
        customExercisesCount: Int = 0,
        dailyGoalsCompleted: Int = 0
    ): AthleteGamificationState {
        val completedSessions = sessions.filter { it.isCompleted }.sortedBy { it.dateTimestamp }
        val totalWorkouts = completedSessions.size
        val totalSets = completedSets.size
        val totalTonnage = completedSessions.sumOf { it.totalTonnageKg }

        // Streak calculation: streak counts consecutive training days or workouts spaced <= 3 days apart
        val streak = calculateStreak(completedSessions)

        // Calculate raw XP:
        // 150 XP per completed workout
        // 10 XP per completed set
        // 1 XP per 100 kg tonnage
        // 25 XP per streak count
        // 50 XP per completed daily goal in background
        var xp = (totalWorkouts * 150) + (totalSets * 10) + (totalTonnage / 100.0).toInt() + (streak * 25) + (dailyGoalsCompleted * 50)

        // Evaluate achievements
        val achievements = evaluateAchievements(
            totalWorkouts = totalWorkouts,
            totalSets = totalSets,
            totalTonnage = totalTonnage,
            streak = streak,
            customExercisesCount = customExercisesCount,
            dailyGoalsCompleted = dailyGoalsCompleted
        )

        // Add XP rewards from unlocked achievements
        val achievementXp = achievements.filter { it.isUnlocked }.sumOf { it.xpReward }
        val totalXp = xp + achievementXp

        // Calculate Level & Rank
        var currentLevel = 1
        var rankTitle = LEVEL_THRESHOLDS[0].second
        var currentLevelBase = 0
        var nextLevelTarget = LEVEL_THRESHOLDS[1].first

        for (i in LEVEL_THRESHOLDS.indices) {
            val (threshold, title) = LEVEL_THRESHOLDS[i]
            if (totalXp >= threshold) {
                currentLevel = i + 1
                rankTitle = title
                currentLevelBase = threshold
                nextLevelTarget = if (i + 1 < LEVEL_THRESHOLDS.size) {
                    LEVEL_THRESHOLDS[i + 1].first
                } else {
                    threshold + 1500
                }
            } else {
                break
            }
        }

        val levelRange = (nextLevelTarget - currentLevelBase).coerceAtLeast(1)
        val progressInLevel = (totalXp - currentLevelBase).coerceIn(0, levelRange)
        val progressPercent = (progressInLevel.toFloat() / levelRange).coerceIn(0f, 1f)

        return AthleteGamificationState(
            level = currentLevel,
            rankTitle = rankTitle,
            currentXp = totalXp,
            nextLevelXp = nextLevelTarget,
            levelBaseXp = currentLevelBase,
            levelProgressPercent = progressPercent,
            totalWorkouts = totalWorkouts,
            totalSets = totalSets,
            totalTonnageKg = totalTonnage,
            currentStreak = streak,
            achievements = achievements,
            unlockedCount = achievements.count { it.isUnlocked }
        )
    }

    private fun calculateStreak(completedSessions: List<WorkoutSessionEntity>): Int {
        if (completedSessions.isEmpty()) return 0

        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val uniqueDays = completedSessions.map {
            dateFormat.parse(dateFormat.format(Date(it.dateTimestamp)))?.time ?: it.dateTimestamp
        }.distinct().sorted()

        if (uniqueDays.isEmpty()) return 0

        var currentStreak = 1
        val now = System.currentTimeMillis()
        val lastWorkoutTime = uniqueDays.last()
        val daysSinceLast = TimeUnit.MILLISECONDS.toDays(now - lastWorkoutTime)

        // If last workout was more than 4 days ago, streak reset
        if (daysSinceLast > 4) {
            return 0
        }

        for (i in uniqueDays.size - 1 downTo 1) {
            val diffDays = TimeUnit.MILLISECONDS.toDays(uniqueDays[i] - uniqueDays[i - 1])
            if (diffDays in 1..3) {
                currentStreak++
            } else {
                break
            }
        }

        return currentStreak
    }

    private fun evaluateAchievements(
        totalWorkouts: Int,
        totalSets: Int,
        totalTonnage: Double,
        streak: Int,
        customExercisesCount: Int,
        dailyGoalsCompleted: Int = 0
    ): List<Achievement> {
        val tonnageInt = totalTonnage.toInt()

        return listOf(
            Achievement(
                id = "habits_consistency",
                title = "Майстер регулярності",
                description = "Виконати денну ціль регулярних вправ",
                iconEmoji = "🎯",
                xpReward = 200,
                currentProgress = dailyGoalsCompleted.coerceAtMost(3),
                maxProgress = 3,
                isUnlocked = dailyGoalsCompleted >= 1,
                category = AchievementCategory.CONSISTENCY
            ),
            Achievement(
                id = "first_workout",
                title = "Перший крок",
                description = "Завершити перше тренування у застосунку",
                iconEmoji = "🥉",
                xpReward = 100,
                currentProgress = totalWorkouts.coerceAtMost(1),
                maxProgress = 1,
                isUnlocked = totalWorkouts >= 1,
                category = AchievementCategory.WORKOUTS
            ),
            Achievement(
                id = "five_workouts",
                title = "Залізний ритм",
                description = "Виконати 5 тренувань",
                iconEmoji = "🥈",
                xpReward = 200,
                currentProgress = totalWorkouts.coerceAtMost(5),
                maxProgress = 5,
                isUnlocked = totalWorkouts >= 5,
                category = AchievementCategory.WORKOUTS
            ),
            Achievement(
                id = "fifteen_workouts",
                title = "Дисципліна титана",
                description = "Виконати 15 повноцінних занять",
                iconEmoji = "🥇",
                xpReward = 400,
                currentProgress = totalWorkouts.coerceAtMost(15),
                maxProgress = 15,
                isUnlocked = totalWorkouts >= 15,
                category = AchievementCategory.WORKOUTS
            ),
            Achievement(
                id = "thirty_workouts",
                title = "Apex Ветеран",
                description = "Завершити 30 тренувань",
                iconEmoji = "👑",
                xpReward = 800,
                currentProgress = totalWorkouts.coerceAtMost(30),
                maxProgress = 30,
                isUnlocked = totalWorkouts >= 30,
                category = AchievementCategory.WORKOUTS
            ),
            Achievement(
                id = "sets_50",
                title = "50 підходів",
                description = "Виконати 50 якісних робочих підходів",
                iconEmoji = "⚡",
                xpReward = 150,
                currentProgress = totalSets.coerceAtMost(50),
                maxProgress = 50,
                isUnlocked = totalSets >= 50,
                category = AchievementCategory.SETS
            ),
            Achievement(
                id = "sets_150",
                title = "Майстер підходів",
                description = "Виконати 150 якісних робочих підходів",
                iconEmoji = "🛡️",
                xpReward = 350,
                currentProgress = totalSets.coerceAtMost(150),
                maxProgress = 150,
                isUnlocked = totalSets >= 150,
                category = AchievementCategory.SETS
            ),
            Achievement(
                id = "sets_300",
                title = "Залізний резерв",
                description = "Виконати 300 якісних робочих підходів",
                iconEmoji = "💎",
                xpReward = 750,
                currentProgress = totalSets.coerceAtMost(300),
                maxProgress = 300,
                isUnlocked = totalSets >= 300,
                category = AchievementCategory.SETS
            ),
            Achievement(
                id = "streak_3",
                title = "Вогонь серії",
                description = "Підтримувати тренувальну серію з 3 занять",
                iconEmoji = "🔥",
                xpReward = 150,
                currentProgress = streak.coerceAtMost(3),
                maxProgress = 3,
                isUnlocked = streak >= 3,
                category = AchievementCategory.CONSISTENCY
            ),
            Achievement(
                id = "streak_7",
                title = "Непереможний",
                description = "Підтримувати серію з 7 занять поспіль",
                iconEmoji = "⚔️",
                xpReward = 350,
                currentProgress = streak.coerceAtMost(7),
                maxProgress = 7,
                isUnlocked = streak >= 7,
                category = AchievementCategory.CONSISTENCY
            ),
            Achievement(
                id = "tonnage_10k",
                title = "10 тонн заліза",
                description = "Підняти сумарно 10 000 кг тоннажу",
                iconEmoji = "🎯",
                xpReward = 200,
                currentProgress = tonnageInt.coerceAtMost(10000),
                maxProgress = 10000,
                isUnlocked = tonnageInt >= 10000,
                category = AchievementCategory.WORKOUTS
            ),
            Achievement(
                id = "tonnage_50k",
                title = "50 тонн сталі",
                description = "Підняти сумарно 50 000 кг тоннажу",
                iconEmoji = "🏆",
                xpReward = 450,
                currentProgress = tonnageInt.coerceAtMost(50000),
                maxProgress = 50000,
                isUnlocked = tonnageInt >= 50000,
                category = AchievementCategory.WORKOUTS
            ),
            Achievement(
                id = "custom_path",
                title = "Свій шлях",
                description = "Створити або вписати власну вправу",
                iconEmoji = "✏️",
                xpReward = 100,
                currentProgress = customExercisesCount.coerceAtMost(1),
                maxProgress = 1,
                isUnlocked = customExercisesCount >= 1,
                category = AchievementCategory.CUSTOM
            )
        )
    }
}
