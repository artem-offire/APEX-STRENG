package com.example.engine

import com.example.model.EquipmentType
import com.example.model.MuscleGroup
import kotlin.math.roundToInt

enum class ProgressionModel(
    val id: String,
    val titleUk: String,
    val shortNameUk: String,
    val descriptionUk: String,
    val scientificAuthorUk: String
) {
    VOLUME_WAVE(
        id = "volume_wave",
        titleUk = "Хвилеподібна прогресія обсягу (MEV → MAV → MRV)",
        shortNameUk = "Хвилеподібний обсяг",
        descriptionUk = "Щотижневе поступове збільшення робочих підходів від мінімального ефективного обсягу до пікового перевантаження зі зниженням RIR від 3 до 0 та обов'язковим делоадом.",
        scientificAuthorUk = "Dr. Mike Israetel (Renaissance Periodization)"
    ),
    LINEAR_STRENGTH(
        id = "linear_strength",
        titleUk = "Лінійна силова прогресія (Зростання ваги)",
        shortNameUk = "Лінійна сила",
        descriptionUk = "Щотижневе планове збільшення робочої ваги на +2.0–2.5% при стабільній кількості підходів і поступовому зменшенні цільових повторів.",
        scientificAuthorUk = "Dr. Brad Schoenfeld & Mark Rippetoe"
    ),
    DOUBLE_PROGRESSION(
        id = "double_progression",
        titleUk = "Подвійна прогресія (Helms Double Progression)",
        shortNameUk = "Подвійна прогресія",
        descriptionUk = "Утримання робочої ваги до заповнення верхньої межі повторів (наприклад, 12 повт.), після чого вага підвищується, а повтори починаються з нижньої межі (8 повт.).",
        scientificAuthorUk = "Dr. Eric Helms (The Muscle and Strength Pyramid)"
    ),
    METABOLIC_PUMP(
        id = "metabolic_pump",
        titleUk = "Метаболічна гіпертрофія (12–15 повт. / Пампінг)",
        shortNameUk = "Метаболічний стрес",
        descriptionUk = "Високоповторний тренінг з прогресивним скороченням пауз відпочинку та збільшенням сумарного тоннажу.",
        scientificAuthorUk = "Dr. Brad Schoenfeld (Mechanisms of Hypertrophy)"
    )
}

data class PlannedExercise(
    val exerciseName: String,
    val muscleGroup: String,
    val equipment: EquipmentType = EquipmentType.BARBELL,
    val baseWeightKg: Double = 50.0,
    val baseRepsMin: Int = 8,
    val baseRepsMax: Int = 12,
    val isPriority: Boolean = false
)

data class PlannedDay(
    val dayTitle: String,
    val exercises: List<PlannedExercise>
)

data class WeekProgressionTarget(
    val weekNumber: Int,
    val isDeload: Boolean,
    val phaseNameUk: String,
    val targetRir: Int,
    val setsMultiplier: Float, // e.g. 1.0 = base, 1.33 = +1 set, 0.67 = deload
    val weightMultiplier: Float, // e.g. 1.0 = 100%, 1.025 = +2.5%
    val repsTargetMin: Int,
    val repsTargetMax: Int,
    val coachGuidelineUk: String
)

data class ExerciseWeekLoad(
    val exerciseName: String,
    val muscleGroup: String,
    val calculatedSets: Int,
    val calculatedWeightKg: Double,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val targetRir: Int,
    val estimatedTonnageKg: Double
)

data class WeekLoadSummary(
    val weekNumber: Int,
    val isDeload: Boolean,
    val phaseNameUk: String,
    val targetRir: Int,
    val daysCount: Int,
    val totalSets: Int,
    val estimatedTotalTonnageKg: Double,
    val intensityDescriptionUk: String,
    val exercisesLoad: List<ExerciseWeekLoad>
)

object MesocycleProgressionEngine {

    fun calculateWeekProgressionTargets(
        totalWeeks: Int,
        model: ProgressionModel
    ): List<WeekProgressionTarget> {
        val targets = mutableListOf<WeekProgressionTarget>()

        for (w in 1..totalWeeks) {
            val isDeload = (w == totalWeeks)

            when (model) {
                ProgressionModel.VOLUME_WAVE -> {
                    if (isDeload) {
                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = true,
                                phaseNameUk = "Делоад (Відновлення ЦНС)",
                                targetRir = 4,
                                setsMultiplier = 0.60f, // -40% sets
                                weightMultiplier = 0.85f, // -15% weight
                                repsTargetMin = 8,
                                repsTargetMax = 10,
                                coachGuidelineUk = "Зменште вагу на 15% та скоротіть кількість підходів удвічі. Працюйте з запасом 4 RIR для скидання втоми."
                            )
                        )
                    } else {
                        val progressFraction = (w - 1).toFloat() / (totalWeeks - 1).coerceAtLeast(1)
                        val setsMult = when (w) {
                            1 -> 1.0f // MEV (3 sets)
                            2 -> 1.25f // MAV low (3-4 sets)
                            3 -> 1.4f // MAV high (4 sets)
                            else -> 1.6f // MRV (4-5 sets)
                        }
                        val weightMult = 1.0f + (progressFraction * 0.075f) // up to +7.5%
                        val rir = (3 - (progressFraction * 3).roundToInt()).coerceIn(0, 3)

                        val phase = when (w) {
                            1 -> "Вступний тиждень (MEV)"
                            2 -> "Акумуляція обсягу (MAV)"
                            3 -> "Перевантаження (MAV High)"
                            else -> "Пікове навантаження (MRV)"
                        }

                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = false,
                                phaseNameUk = phase,
                                targetRir = rir,
                                setsMultiplier = setsMult,
                                weightMultiplier = weightMult,
                                repsTargetMin = 8,
                                repsTargetMax = 12,
                                coachGuidelineUk = "Цільовий RIR: $rir. Заплановане збільшення ваги: +${String.format("%.1f", (weightMult - 1.0f) * 100)}%."
                            )
                        )
                    }
                }

                ProgressionModel.LINEAR_STRENGTH -> {
                    if (isDeload) {
                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = true,
                                phaseNameUk = "Делоад (Силове розвантаження)",
                                targetRir = 4,
                                setsMultiplier = 0.60f,
                                weightMultiplier = 0.85f,
                                repsTargetMin = 5,
                                repsTargetMax = 5,
                                coachGuidelineUk = "Легкі сингли та сети на техніку. Зниження навантаження на зв'язки."
                            )
                        )
                    } else {
                        val weightMult = 1.0f + ((w - 1) * 0.025f) // +2.5% every week
                        val repsMin = (8 - (w - 1)).coerceAtLeast(4)
                        val repsMax = (repsMin + 2)
                        val rir = if (w == totalWeeks - 1) 1 else 2

                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = false,
                                phaseNameUk = "Силова хвиля (Тиждень $w)",
                                targetRir = rir,
                                setsMultiplier = 1.0f,
                                weightMultiplier = weightMult,
                                repsTargetMin = repsMin,
                                repsTargetMax = repsMax,
                                coachGuidelineUk = "Збільште вагу на +${String.format("%.1f", (w - 1) * 2.5)}% від бази. Цільовий діапазон: $repsMin–$repsMax повторів."
                            )
                        )
                    }
                }

                ProgressionModel.DOUBLE_PROGRESSION -> {
                    if (isDeload) {
                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = true,
                                phaseNameUk = "Активне відновлення (Делоад)",
                                targetRir = 4,
                                setsMultiplier = 0.60f,
                                weightMultiplier = 0.85f,
                                repsTargetMin = 8,
                                repsTargetMax = 8,
                                coachGuidelineUk = "Знизьте інтенсивність перед наступним важким циклом."
                            )
                        )
                    } else {
                        val targetReps = (8 + (w - 1) * 2).coerceAtMost(12)
                        val weightMult = if (w >= 4) 1.05f else 1.0f
                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = false,
                                phaseNameUk = "Накопичення повторів (Ціль: $targetReps повт.)",
                                targetRir = 2,
                                setsMultiplier = 1.0f,
                                weightMultiplier = weightMult,
                                repsTargetMin = targetReps - 1,
                                repsTargetMax = targetReps,
                                coachGuidelineUk = "Прагніть зафіксувати $targetReps повторів у всіх підходах при RIR 2."
                            )
                        )
                    }
                }

                ProgressionModel.METABOLIC_PUMP -> {
                    if (isDeload) {
                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = true,
                                phaseNameUk = "Делоад / Скидання лактату",
                                targetRir = 3,
                                setsMultiplier = 0.60f,
                                weightMultiplier = 0.85f,
                                repsTargetMin = 10,
                                repsTargetMax = 12,
                                coachGuidelineUk = "Помірний пампінг без відмови."
                            )
                        )
                    } else {
                        val setsMult = 1.0f + ((w - 1) * 0.20f)
                        targets.add(
                            WeekProgressionTarget(
                                weekNumber = w,
                                isDeload = false,
                                phaseNameUk = "Метаболічний стрес (Тиждень $w)",
                                targetRir = 1,
                                setsMultiplier = setsMult,
                                weightMultiplier = 1.0f + ((w - 1) * 0.015f),
                                repsTargetMin = 12,
                                repsTargetMax = 15,
                                coachGuidelineUk = "Висока щільність тренінгу, скорочені паузи відпочинку (60-75с)."
                            )
                        )
                    }
                }
            }
        }

        return targets
    }

    fun calculateFullMesocycleProgression(
        days: List<PlannedDay>,
        totalWeeks: Int,
        model: ProgressionModel
    ): List<WeekLoadSummary> {
        val weekTargets = calculateWeekProgressionTargets(totalWeeks, model)

        return weekTargets.map { target ->
            val exercisesLoad = mutableListOf<ExerciseWeekLoad>()
            var weekTotalSets = 0
            var weekTotalTonnage = 0.0

            for (day in days) {
                for (ex in day.exercises) {
                    val priorityBonus = if (ex.isPriority && !target.isDeload) 1 else 0
                    val baseSets = 3
                    val calculatedSets = when {
                        target.isDeload -> 2
                        else -> (baseSets * target.setsMultiplier).roundToInt().coerceIn(2, 6) + priorityBonus
                    }

                    // Round weight to nearest 0.5 kg
                    val rawWeight = ex.baseWeightKg * target.weightMultiplier
                    val roundedWeight = (rawWeight * 2.0).roundToInt() / 2.0
                    val avgReps = (target.repsTargetMin + target.repsTargetMax) / 2.0
                    val estimatedTonnage = roundedWeight * avgReps * calculatedSets

                    weekTotalSets += calculatedSets
                    weekTotalTonnage += estimatedTonnage

                    exercisesLoad.add(
                        ExerciseWeekLoad(
                            exerciseName = ex.exerciseName,
                            muscleGroup = ex.muscleGroup,
                            calculatedSets = calculatedSets,
                            calculatedWeightKg = roundedWeight,
                            targetRepsMin = target.repsTargetMin,
                            targetRepsMax = target.repsTargetMax,
                            targetRir = target.targetRir,
                            estimatedTonnageKg = estimatedTonnage
                        )
                    )
                }
            }

            WeekLoadSummary(
                weekNumber = target.weekNumber,
                isDeload = target.isDeload,
                phaseNameUk = target.phaseNameUk,
                targetRir = target.targetRir,
                daysCount = days.size,
                totalSets = weekTotalSets,
                estimatedTotalTonnageKg = weekTotalTonnage,
                intensityDescriptionUk = target.coachGuidelineUk,
                exercisesLoad = exercisesLoad
            )
        }
    }
}
