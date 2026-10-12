package com.example.model

import com.example.data.WorkoutSessionEntity
import com.example.data.WorkoutSetWithExerciseInfo
import kotlin.math.sqrt

enum class CharacterEvolutionTier(
    val tierNumber: Int,
    val titleUk: String,
    val subtitleUk: String,
    val emojiAvatar: String,
    val minPowerLevel: Int,
    val auraColorHex: Long
) {
    TIER_1(1, "Новачок Залу", "Початок шляху. Тіло звикає до заліза", "🥋", 0, 0xFF78909C),
    TIER_2(2, "Атлет-Штурмовик", "М'язовий корсет зміцнів, техніка відточується", "🥊", 600, 0xFF42A5F5),
    TIER_3(3, "Залізний Воїн", "Потужний рельєф, дисципліна та прогресія ваг", "⚔️", 1500, 0xFF26A69A),
    TIER_4(4, "Apex Берсерк", "Висока щільність м'язів, витривалість титана", "🛡️", 3000, 0xFFFFB300),
    TIER_5(5, "Олімпійський Титан", "Аура сили, непереможні ваги та міць", "⚡", 5000, 0xFFFF7043),
    TIER_6(6, "Apex Легенда", "Вершина фізичної форми. Абсолютна сила", "👑", 8000, 0xFFAB47BC)
}

data class AthleteCharacter(
    val nickname: String = "Атлет",
    val powerLevel: Int,
    val level: Int,
    val currentTier: CharacterEvolutionTier,
    val nextTier: CharacterEvolutionTier?,
    val progressToNextTierPercent: Float,
    val strengthStat: Int,
    val staminaStat: Int,
    val disciplineStat: Int,
    val totalWorkouts: Int,
    val totalSets: Int,
    val totalTonnageKg: Double,
    val streak: Int,
    val battleCryUk: String
)

object CharacterEngine {

    fun computeCharacter(
        sessions: List<WorkoutSessionEntity>,
        completedSets: List<WorkoutSetWithExerciseInfo>,
        streak: Int,
        nickname: String = "Атлет"
    ): AthleteCharacter {
        val completedSessions = sessions.filter { it.isCompleted }
        val totalWorkouts = completedSessions.size
        val totalSets = completedSets.size
        val totalTonnage = completedSessions.sumOf { it.totalTonnageKg }

        // Max weight lifted in any single set
        val maxWeight = completedSets.maxOfOrNull { it.weightKg } ?: 0.0

        // Stats calculation
        val str = ((maxWeight * 2.0) + (totalTonnage / 40.0)).toInt().coerceAtLeast(10)
        val sta = ((totalSets * 12) + (totalWorkouts * 35)).coerceAtLeast(10)
        val disc = ((streak * 90) + (totalWorkouts * 25)).coerceAtLeast(10)

        val totalPower = str + sta + disc

        // Evolution Tier determination
        val tiers = CharacterEvolutionTier.values()
        val currentTier = tiers.lastOrNull { totalPower >= it.minPowerLevel } ?: tiers.first()
        val nextTier = tiers.getOrNull(currentTier.ordinal + 1)

        val progressToNext = if (nextTier != null) {
            val range = (nextTier.minPowerLevel - currentTier.minPowerLevel).toFloat()
            if (range > 0f) {
                ((totalPower - currentTier.minPowerLevel) / range).coerceIn(0f, 1f)
            } else 1f
        } else {
            1f
        }

        val level = (sqrt(totalPower.toDouble() / 15.0)).toInt().coerceAtLeast(1)

        val cries = listOf(
            "Кожен підхід наближає до нової форми!",
            "Залізо не бреше — ти стаєш сильнішим щодня!",
            "Тіло слідує за волею. До нових рекордів!",
            "М'язи адаптуються, сила росте!",
            "Apex форма заряджена на максимум!"
        )
        val cry = cries[totalWorkouts % cries.size]

        return AthleteCharacter(
            nickname = nickname,
            powerLevel = totalPower,
            level = level,
            currentTier = currentTier,
            nextTier = nextTier,
            progressToNextTierPercent = progressToNext,
            strengthStat = str,
            staminaStat = sta,
            disciplineStat = disc,
            totalWorkouts = totalWorkouts,
            totalSets = totalSets,
            totalTonnageKg = totalTonnage,
            streak = streak,
            battleCryUk = cry
        )
    }
}
