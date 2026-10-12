package com.example.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import androidx.room.TypeConverter
import com.example.engine.ProgressionModel
import com.example.model.EquipmentType
import com.example.model.MovementPattern
import com.example.model.MuscleGroup

/**
 * Room TypeConverters for domain enums and comma-separated lists used in progressive overload & workout tracking.
 */
class ApexTypeConverters {
    @TypeConverter
    fun fromMuscleGroup(muscleGroup: MuscleGroup?): String? = muscleGroup?.name

    @TypeConverter
    fun toMuscleGroup(value: String?): MuscleGroup? =
        value?.let { MuscleGroup.fromStringOrNull(it) }

    @TypeConverter
    fun fromEquipmentType(equipment: EquipmentType?): String? = equipment?.name

    @TypeConverter
    fun toEquipmentType(value: String?): EquipmentType? =
        value?.let { runCatching { EquipmentType.valueOf(it) }.getOrDefault(EquipmentType.BARBELL) }

    @TypeConverter
    fun fromMovementPattern(pattern: MovementPattern?): String? = pattern?.name

    @TypeConverter
    fun toMovementPattern(value: String?): MovementPattern? =
        value?.let { runCatching { MovementPattern.valueOf(it) }.getOrDefault(MovementPattern.ISOLATION) }

    @TypeConverter
    fun fromProgressionModel(model: ProgressionModel?): String? = model?.name

    @TypeConverter
    fun toProgressionModel(value: String?): ProgressionModel? =
        value?.let { runCatching { ProgressionModel.valueOf(it) }.getOrDefault(ProgressionModel.VOLUME_WAVE) }

    @TypeConverter
    fun fromStringList(list: List<String>?): String =
        list?.joinToString(",") ?: ""

    @TypeConverter
    fun toStringList(data: String?): List<String> =
        data?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
}

/**
 * 1. EXERCISES CATALOG ENTITY
 * Stores biomechanical metadata, muscle group targeting, equipment requirements,
 * and default progressive overload parameters (weight step, rep range, RIR target).
 */
@Entity(
    tableName = "exercises",
    indices = [
        Index(value = ["name"]),
        Index(value = ["muscleGroup"]),
        Index(value = ["equipment"])
    ]
)
data class ExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String, // MuscleGroup.name
    val equipment: String, // EquipmentType.name
    val movementPattern: String, // MovementPattern.name
    val instructionsUk: String,
    val jointSafetyUk: String,
    val substituteNames: String = "", // comma separated
    // Progressive Overload structure metadata
    val secondaryMuscles: String = "",
    val isCompound: Boolean = movementPattern != "ISOLATION" && movementPattern != "CORE",
    val defaultIncrementKg: Double = when (equipment) {
        "BARBELL", "CABLE", "EZ_BAR" -> 2.5
        "DUMBBELL" -> 2.0
        "MACHINE" -> 5.0
        else -> 0.0
    },
    val defaultTargetRepsMin: Int = if (movementPattern == "ISOLATION" || movementPattern == "CORE") 10 else 8,
    val defaultTargetRepsMax: Int = if (movementPattern == "ISOLATION" || movementPattern == "CORE") 15 else 12,
    val defaultTargetRir: Int = if (movementPattern == "ISOLATION" || movementPattern == "CORE") 1 else 2,
    val progressionType: String = "DOUBLE_PROGRESSION" // DOUBLE_PROGRESSION, LINEAR_STRENGTH, VOLUME_WAVE, METABOLIC_PUMP
)

/**
 * 2. MESOCYCLES ENTITY
 * Represents a structured multi-week training block (e.g., 4-6 weeks) with
 * periodization model, RIR progression wave (3 -> 2 -> 1 -> 0 -> Deload), and priority muscles.
 */
@Entity(
    tableName = "mesocycles",
    indices = [
        Index(value = ["isActive"]),
        Index(value = ["startDate"])
    ]
)
data class MesocycleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val totalWeeks: Int = 5,
    val currentWeek: Int = 1,
    val startDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val isCompleted: Boolean = false,
    val programType: String = "Upper / Lower (4 дні)",
    val priorityMuscles: String = "", // comma separated muscle group names
    // Progressive Overload Periodization Structure
    val progressionModel: String = "VOLUME_WAVE", // VOLUME_WAVE, LINEAR_STRENGTH, DOUBLE_PROGRESSION, METABOLIC_PUMP
    val deloadWeek: Int = totalWeeks,
    val startingRir: Int = 3,
    val peakRir: Int = 0,
    val weeklySetIncrement: Int = 1,
    val weeklyWeightIncrementPercent: Float = 2.5f,
    val goalDescriptionUk: String = "Гіпертрофія та прогресивне навантаження"
)

/**
 * 3. MESOCYCLE WEEK PLANS ENTITY
 * Stores week-by-week progressive overload targets within a mesocycle
 * (MEV -> MAV -> MRV -> Deload multipliers for sets, weight, RIR, and target rep ranges).
 */
@Entity(
    tableName = "mesocycle_week_plans",
    foreignKeys = [
        ForeignKey(
            entity = MesocycleEntity::class,
            parentColumns = ["id"],
            childColumns = ["mesocycleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["mesocycleId"]),
        Index(value = ["mesocycleId", "weekNumber"])
    ]
)
data class MesocycleWeekPlanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mesocycleId: Long,
    val weekNumber: Int,
    val phaseCode: String = "ACCUMULATION", // INTRO_MEV, ACCUMULATION_MAV, OVERREACH_MRV, PEAK_INTENSITY, DELOAD
    val phaseNameUk: String,
    val isDeload: Boolean = false,
    val targetRir: Int = 2,
    val setsMultiplier: Float = 1.0f,
    val weightMultiplier: Float = 1.0f,
    val targetRepsMin: Int = 8,
    val targetRepsMax: Int = 12,
    val plannedTotalSets: Int = 0,
    val plannedTonnageKg: Double = 0.0,
    val coachGuidelineUk: String = ""
)

/**
 * 4. WORKOUT SESSIONS ENTITY (WORKOUT HISTORY HEADER)
 * Stores scheduled and completed workout sessions linked to a mesocycle and week number,
 * along with total tonnage, working sets, PRs achieved, and progressive overload status.
 */
@Entity(
    tableName = "workout_sessions",
    indices = [
        Index(value = ["mesocycleId"]),
        Index(value = ["mesocycleId", "weekNumber"]),
        Index(value = ["dateTimestamp"]),
        Index(value = ["isCompleted"])
    ]
)
data class WorkoutSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mesocycleId: Long,
    val weekNumber: Int,
    val title: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0,
    val totalTonnageKg: Double = 0.0,
    val isCompleted: Boolean = false,
    val readinessScore: Float? = null,
    val notes: String = "",
    val startClassification: String = "UNKNOWN", // USUAL, LATE, EARLY, UNUSUAL_DAY, LATE_NIGHT, AFTER_BREAK, UNKNOWN
    val wellBeingRating: Int? = null, // 1..5 stars post-workout
    val isInterrupted: Boolean = false, // auto-completed on inactivity
    val lastActivityTimestamp: Long = System.currentTimeMillis(),
    // Progressive Overload Session Summary
    val totalWorkingSets: Int = 0,
    val averageRir: Float? = null,
    val prCount: Int = 0,
    val progressionStatus: String = "PLANNED", // PLANNED, PROGRESSED, MAINTAINED, DELOAD, REGRESSED
    val previousSessionTonnageDeltaKg: Double = 0.0
)

/**
 * 5. WORKOUT EXERCISES ENTITY
 * Connects a WorkoutSessionEntity to an ExerciseEntity with session-specific ordering
 * and progressive overload prescription (target sets, reps, weight, RIR, and progression action).
 */
@Entity(
    tableName = "workout_exercises",
    indices = [
        Index(value = ["sessionId"]),
        Index(value = ["exerciseId"]),
        Index(value = ["exerciseName"]),
        Index(value = ["muscleGroup"])
    ]
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val muscleGroup: String,
    val orderIndex: Int,
    val notes: String = "",
    // Progressive Overload Prescription & Comparison
    val targetSets: Int = 3,
    val targetRepsMin: Int = 8,
    val targetRepsMax: Int = 12,
    val targetWeightKg: Double = 0.0,
    val targetRir: Int = 2,
    val previousBestWeightKg: Double = 0.0,
    val previousBestReps: Int = 0,
    val overloadAchieved: Boolean = false,
    val progressionAction: String = "HOLD_WEIGHT_ADD_REPS" // INCREASE_WEIGHT, HOLD_WEIGHT_ADD_REPS, ADD_SET, DELOAD, PLATEAU_ROTATE
)

/**
 * 6. WORKOUT SETS ENTITY
 * Granular set-level execution data (weight, reps, target rep bounds, RIR/RPE, Epley 1RM,
 * tonnage, warmup vs working set, and PR flag) for progressive overload calculation.
 */
@Entity(
    tableName = "workout_sets",
    indices = [
        Index(value = ["workoutExerciseId"]),
        Index(value = ["isCompleted"]),
        Index(value = ["timestamp"])
    ]
)
data class WorkoutSetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val targetRepsMin: Int = 8,
    val targetRepsMax: Int = 12,
    val rir: Int = 2,
    val isWarmup: Boolean = false,
    val isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    // Progressive Overload Set Metrics
    val targetWeightKg: Double = weightKg,
    val rpe: Float = (10 - rir).coerceIn(5, 10).toFloat(),
    val estimated1RmKg: Double = if (reps > 0 && !isWarmup) {
        if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)
    } else 0.0,
    val tonnageKg: Double = if (!isWarmup) weightKg * reps else 0.0,
    val isPersonalRecord: Boolean = false,
    val setType: String = if (isWarmup) "WARMUP" else "WORKING" // WARMUP, WORKING, TOP_SET, BACKOFF_SET, DROP_SET, FAILURE
)

/**
 * 7. PROGRESSIVE OVERLOAD LOGS ENTITY
 * Tracks exercise-by-exercise progressive overload milestones across workout history:
 * load increases (+kg), rep increases (+reps), volume jumps (+sets/tonnage), Epley 1RM records,
 * and next-session autoregulated targets.
 */
@Entity(
    tableName = "progressive_overload_logs",
    indices = [
        Index(value = ["exerciseName"]),
        Index(value = ["muscleGroup"]),
        Index(value = ["sessionId"]),
        Index(value = ["mesocycleId"]),
        Index(value = ["timestamp"])
    ]
)
data class ProgressiveOverloadLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val exerciseId: Long = 0,
    val exerciseName: String,
    val muscleGroup: String,
    val sessionId: Long,
    val mesocycleId: Long = 0,
    val weekNumber: Int = 1,
    val timestamp: Long = System.currentTimeMillis(),
    val previousWeightKg: Double = 0.0,
    val newWeightKg: Double,
    val previousReps: Int = 0,
    val newReps: Int,
    val previousEstimated1RmKg: Double = 0.0,
    val newEstimated1RmKg: Double,
    val previousExerciseTonnageKg: Double = 0.0,
    val newExerciseTonnageKg: Double = 0.0,
    val workingSetsCompleted: Int = 0,
    val avgRir: Float = 2.0f,
    val overloadType: String, // LOAD_PROGRESSION, REP_PROGRESSION, VOLUME_PROGRESSION, E1RM_RECORD, MAINTENANCE, DELOAD, PLATEAU
    val nextRecommendedWeightKg: Double,
    val nextRecommendedRepsMin: Int = 8,
    val nextRecommendedRepsMax: Int = 12,
    val summaryUk: String
)

/**
 * ROOM RELATIONAL POJOs (@Embedded & @Relation)
 * Enables single-transaction hierarchical queries for Mesocycles, Workout History, and Exercises.
 */
data class WorkoutExerciseWithSetsRelation(
    @Embedded val workoutExercise: WorkoutExerciseEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "workoutExerciseId"
    )
    val sets: List<WorkoutSetEntity>
)

data class WorkoutSessionWithDetails(
    @Embedded val session: WorkoutSessionEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "sessionId"
    )
    val exercises: List<WorkoutExerciseWithSetsRelation>
)

data class MesocycleWithFullStructure(
    @Embedded val mesocycle: MesocycleEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "mesocycleId"
    )
    val weekPlans: List<MesocycleWeekPlanEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "mesocycleId"
    )
    val sessions: List<WorkoutSessionEntity>
)

data class ExerciseWithOverloadHistory(
    @Embedded val exercise: ExerciseEntity,
    @Relation(
        parentColumn = "name",
        entityColumn = "exerciseName"
    )
    val overloadLogs: List<ProgressiveOverloadLogEntity>
)

@Entity(tableName = "internal_schedules")
data class InternalScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String = "default_user",
    val weekday: Int, // Calendar.MONDAY (2) .. Calendar.SUNDAY (1)
    val expectedStartMinutes: Int, // minutes from 00:00, e.g. 19*60 = 1140
    val toleranceMin: Int, // window in minutes, e.g. 35
    val expectedDurationMinutes: Int, // median duration in minutes
    val confidence: String, // "HIGH", "MEDIUM", "LOW"
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "trigger_logs")
data class TriggerLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val ruleId: String,
    val timestamp: Long = System.currentTimeMillis(),
    val messageText: String,
    val userReaction: String = "VIEWED" // VIEWED, ACCEPTED, DISMISSED
)

@Entity(tableName = "reminder_logs")
data class ReminderLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateKey: String, // "yyyy-MM-dd"
    val weekday: Int,
    val sentTimestamp: Long = System.currentTimeMillis(),
    val wasIgnored: Boolean = true
)

@Entity(tableName = "weekly_summaries")
data class WeeklySummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val weekStartTimestamp: Long,
    val workoutCount: Int,
    val totalVolumeKg: Double,
    val avgDurationMinutes: Int,
    val previousWeekWorkoutCount: Int = 0,
    val summaryAdvice: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "readiness_checkins")
data class ReadinessCheckinEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val sleepQuality: Int, // 1-5
    val domsSoreness: Int, // 1-5 (1=none, 5=extreme)
    val energyLevel: Int, // 1-5
    val motivation: Int, // 1-5
    val cnsScore: Float, // 0.0 - 10.0
    val autoregulationMessage: String
)

@Entity(tableName = "body_measurements")
data class BodyMeasurementEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val weightKg: Double,
    val waistCm: Double = 0.0,
    val bicepsCm: Double = 0.0,
    val chestCm: Double = 0.0,
    val bodyFatPercent: Double = 0.0
)

@Entity(tableName = "gym_profiles")
data class GymProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val isSelected: Boolean = false,
    val availableEquipment: String = "BARBELL,DUMBBELL,CABLE,MACHINE,BODYWEIGHT,EZ_BAR"
)

@Entity(tableName = "user_volume_config")
data class UserVolumeConfigEntity(
    @PrimaryKey val id: Int = 1,
    val experienceLevel: String = "INTERMEDIATE",
    val sleepFactor: String = "AVERAGE",
    val nutritionFactor: String = "MAINTENANCE",
    val stressFactor: String = "MODERATE",
    val priorityMuscles: String = "CHEST,BACK" // comma-separated
)
