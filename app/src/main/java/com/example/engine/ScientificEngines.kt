package com.example.engine

import android.content.Context
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.example.data.WorkoutSetEntity
import com.example.model.*
import kotlin.math.roundToInt

object VolumeLandmarksCalculator {
    fun calculate(
        config: UserVolumeConfig,
        weeklySetsMap: Map<MuscleGroup, Int>
    ): List<VolumeLandmarkData> {
        val expMultiplier = config.experienceLevel.multiplier
        val recoveryMultiplier = config.sleepFactor.factor * config.nutritionFactor.factor * config.stressFactor.factor

        return MuscleGroup.values().filter { it != MuscleGroup.OTHER }.map { muscle ->
            val isPriority = config.priorityMuscles.contains(muscle)
            val priorityMultiplier = if (isPriority) 1.25f else 0.85f

            val totalMod = expMultiplier * recoveryMultiplier * priorityMultiplier

            val mv = (muscle.baselineMv * expMultiplier).roundToInt().coerceAtLeast(0)
            val mev = (muscle.baselineMev * totalMod).roundToInt().coerceAtLeast(mv)
            val mavMin = (muscle.baselineMavMin * totalMod).roundToInt().coerceAtLeast(mev)
            val mavMax = (muscle.baselineMavMax * totalMod).roundToInt().coerceAtLeast(mavMin)
            val mrv = (muscle.baselineMrv * totalMod).roundToInt().coerceAtLeast(mavMax)

            val currentSets = weeklySetsMap[muscle] ?: 0

            VolumeLandmarkData(
                muscleGroup = muscle,
                mv = mv,
                mev = mev,
                mavMin = mavMin,
                mavMax = mavMax,
                mrv = mrv,
                currentWeeklySets = currentSets,
                isPriority = isPriority
            )
        }
    }
}

data class UserVolumeConfig(
    val experienceLevel: ExperienceLevel = ExperienceLevel.INTERMEDIATE,
    val sleepFactor: SleepFactor = SleepFactor.AVERAGE,
    val nutritionFactor: NutritionFactor = NutritionFactor.MAINTENANCE,
    val stressFactor: StressFactor = StressFactor.MODERATE,
    val priorityMuscles: Set<MuscleGroup> = setOf(MuscleGroup.CHEST, MuscleGroup.BACK)
)

object DoubleProgressionEngine {
    data class ProgressionSuggestion(
        val shouldIncreaseWeight: Boolean,
        val suggestedWeightKg: Double,
        val suggestedReps: Int,
        val explanationUk: String,
        val isPlateau: Boolean = false,
        val plateauMessageUk: String? = null
    )

    fun evaluateProgression(
        exerciseName: String,
        equipment: EquipmentType,
        targetRepsMin: Int,
        targetRepsMax: Int,
        historySets: List<WorkoutSetEntity>
    ): ProgressionSuggestion {
        if (historySets.isEmpty()) {
            return ProgressionSuggestion(
                shouldIncreaseWeight = false,
                suggestedWeightKg = 0.0,
                suggestedReps = targetRepsMin,
                explanationUk = "Перша сесія з цією вправою. Виберіть комфортну робочу вагу з запасом 2 RIR."
            )
        }

        val lastWorkingSet = historySets.first()
        val currentWeight = lastWorkingSet.weightKg
        val currentReps = lastWorkingSet.reps

        // Weight increment based on equipment
        val weightStep = when (equipment) {
            EquipmentType.BARBELL -> 2.5
            EquipmentType.DUMBBELL -> 2.0
            EquipmentType.CABLE -> 2.5
            EquipmentType.MACHINE -> 5.0
            EquipmentType.EZ_BAR -> 2.5
            EquipmentType.BODYWEIGHT -> 0.0
        }

        // Check for plateau (last 3 sessions have identical weight and reps <= targetRepsMin)
        val isPlateau = historySets.size >= 4 &&
                historySets.take(3).all { it.weightKg == currentWeight && it.reps <= targetRepsMin }

        if (isPlateau) {
            return ProgressionSuggestion(
                shouldIncreaseWeight = false,
                suggestedWeightKg = currentWeight,
                suggestedReps = currentReps,
                explanationUk = "Виявлено плато: вага і повтори не зростають 3 сесії поспіль.",
                isPlateau = true,
                plateauMessageUk = "⚠️ Науковий протокол виходу з плато:\n1. Тимчасовий делоад (-50% підходів на цей м'яз);\n2. Зміна робочого діапазону (наприклад, з 8-10 на 5-8 або 12-15);\n3. Ротація на біомеханічний аналог з тим самим вектором сили."
            )
        }

        // Check if user hit the top of rep range
        if (currentReps >= targetRepsMax && lastWorkingSet.rir >= 1) {
            val nextWeight = currentWeight + weightStep
            return ProgressionSuggestion(
                shouldIncreaseWeight = true,
                suggestedWeightKg = nextWeight,
                suggestedReps = targetRepsMin,
                explanationUk = "🎯 Прогресивне навантаження! Досягнуто верх діапазону ($currentReps повт. при RIR ${lastWorkingSet.rir}). Збільште вагу на +$weightStep кг та почніть з $targetRepsMin повторів."
            )
        } else {
            val targetRepsNext = (currentReps + 1).coerceAtMost(targetRepsMax)
            return ProgressionSuggestion(
                shouldIncreaseWeight = false,
                suggestedWeightKg = currentWeight,
                suggestedReps = targetRepsNext,
                explanationUk = "Залишайте вагу $currentWeight кг. Ціль на сьогодні — додати 1 повтор ($targetRepsNext повт.) при RIR 1–2."
            )
        }
    }
}

object AutoregulationEngine {
    data class ReadinessResult(
        val cnsScore: Float, // 0.0 to 10.0
        val isLowReadiness: Boolean,
        val recommendationUk: String,
        val weightAdjustmentFactor: Float, // e.g. 0.90 for -10%
        val setReduction: Int // 0 or 1
    )

    fun calculateReadiness(
        sleepQuality: Int, // 1-5
        domsSoreness: Int, // 1-5 (5 = worst soreness)
        energyLevel: Int, // 1-5
        motivation: Int // 1-5
    ): ReadinessResult {
        // Soreness is inverted: 5 soreness = 1 recovery point
        val sorenessScore = (6 - domsSoreness).coerceIn(1, 5)

        // Raw sum 4 to 20 -> converted to 0.0 to 10.0 scale
        val rawSum = sleepQuality + sorenessScore + energyLevel + motivation
        val cnsScore = ((rawSum - 4) / 16.0f * 10.0f).coerceIn(0.0f, 10.0f)

        return when {
            cnsScore < 4.5f -> ReadinessResult(
                cnsScore = cnsScore,
                isLowReadiness = true,
                recommendationUk = "🚨 Критична втома ЦНС (${String.format("%.1f", cnsScore)}/10). Рекомендовано знизити робочі ваги на 10% та скоротити 1 підхід у кожній вправі або провести активне відновлення.",
                weightAdjustmentFactor = 0.90f,
                setReduction = 1
            )
            cnsScore < 6.0f -> ReadinessResult(
                cnsScore = cnsScore,
                isLowReadiness = true,
                recommendationUk = "⚠️ Помірна втома ЦНС (${String.format("%.1f", cnsScore)}/10). Працюйте суворо в межах RIR 2-3, не доходячи до відмови. За необхідності знизьте вагу на 5%.",
                weightAdjustmentFactor = 0.95f,
                setReduction = 0
            )
            cnsScore >= 8.5f -> ReadinessResult(
                cnsScore = cnsScore,
                isLowReadiness = false,
                recommendationUk = "⚡ Відмінна готовність ЦНС (${String.format("%.1f", cnsScore)}/10)! Відновлення максимальне. Ідеальний день для спроби прогресії ваги або повторів.",
                weightAdjustmentFactor = 1.0f,
                setReduction = 0
            )
            else -> ReadinessResult(
                cnsScore = cnsScore,
                isLowReadiness = false,
                recommendationUk = "✅ Оптимальний стан ЦНС (${String.format("%.1f", cnsScore)}/10). Працюйте за наміченим планом згідно з фазою мезоциклу.",
                weightAdjustmentFactor = 1.0f,
                setReduction = 0
            )
        }
    }
}

object WarmupCalculator {
    data class WarmupStage(
        val stageNumber: Int,
        val percentOfWork: Int,
        val weightKg: Double,
        val reps: Int,
        val purposeUk: String,
        val restSeconds: Int
    )

    fun calculatePyramid(workingWeightKg: Double, isBarbell: Boolean = true): List<WarmupStage> {
        val barWeight = if (isBarbell) 20.0 else 0.0

        val stage1Weight = if (isBarbell) 20.0 else roundToGymPlate(workingWeightKg * 0.25)
        val stage2Weight = roundToGymPlate(workingWeightKg * 0.45).coerceAtLeast(stage1Weight)
        val stage3Weight = roundToGymPlate(workingWeightKg * 0.65).coerceAtLeast(stage2Weight)
        val stage4Weight = roundToGymPlate(workingWeightKg * 0.85).coerceAtLeast(stage3Weight)

        return listOf(
            WarmupStage(1, if (isBarbell) 20 else 25, stage1Weight, 10, "Мобілізація суглобів та синовіальної рідини", 60),
            WarmupStage(2, 45, stage2Weight, 5, "Нейром'язова активація рухових одиниць", 60),
            WarmupStage(3, 65, stage3Weight, 3, "Швидкість руху та траєкторія грифу", 90),
            WarmupStage(4, 85, stage4Weight, 1, "Потенціація ЦНС без накопичення лактату", 120)
        )
    }

    private fun roundToGymPlate(weight: Double): Double {
        return (weight / 2.5).roundToInt() * 2.5
    }
}

object OneRepMaxCalculator {
    // Epley Formula: 1RM = Weight * (1 + Reps / 30)
    fun calculateEpley(weightKg: Double, reps: Int): Double {
        if (reps <= 0) return 0.0
        if (reps == 1) return weightKg
        return weightKg * (1.0 + reps / 30.0)
    }

    // Brzycki Formula: 1RM = Weight / (1.0278 - 0.0278 * Reps)
    fun calculateBrzycki(weightKg: Double, reps: Int): Double {
        if (reps <= 0) return 0.0
        if (reps == 1) return weightKg
        val denom = 1.0278 - 0.0278 * reps
        return if (denom > 0) weightKg / denom else weightKg
    }
}

class AudioHapticFeedback(private val context: Context) {
    private var toneGenerator: ToneGenerator? = try {
        ToneGenerator(AudioManager.STREAM_NOTIFICATION, 90)
    } catch (_: Exception) {
        null
    }

    fun playTick() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP, 80)
        vibrate(35)
    }

    fun playFinishTone() {
        toneGenerator?.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 350)
        vibrate(300)
    }

    fun playSetComplete() {
        toneGenerator?.startTone(ToneGenerator.TONE_PROP_ACK, 120)
        vibrate(60)
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                v?.vibrate(durationMs)
            }
        } catch (_: Exception) {}
    }
}
