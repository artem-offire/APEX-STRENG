package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object (DAO) for Apex Strength.
 * Provides full CRUD (Create, Read, Update, Delete) and transactional relational operations for:
 * 1. Exercises Catalog & Progressive Overload Metadata (`exercises`)
 * 2. Mesocycles & Weekly Progression Structure (`mesocycles`, `mesocycle_week_plans`)
 * 3. Workout History: Sessions, Exercises, Sets & Overload Logs (`workout_sessions`, `workout_exercises`, `workout_sets`, `progressive_overload_logs`)
 * 4. Readiness Check-ins, Body Measurements, Gym Profiles & Schedules
 */
@Dao
interface ApexDao {

    // =========================================================================
    // 1. EXERCISES CATALOG CRUD (CREATE, READ, UPDATE, DELETE)
    // =========================================================================

    // --- CREATE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercises(exercises: List<ExerciseEntity>): List<Long>

    @Upsert
    suspend fun upsertExercise(exercise: ExerciseEntity): Long

    @Upsert
    suspend fun upsertExercises(exercises: List<ExerciseEntity>): List<Long>

    // --- READ ---
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    suspend fun getAllExercisesOnce(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    suspend fun getExerciseById(id: Long): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun getExerciseByIdFlow(id: Long): Flow<ExerciseEntity?>

    @Query("SELECT * FROM exercises WHERE name = :name LIMIT 1")
    suspend fun getExerciseByName(name: String): ExerciseEntity?

    @Query("SELECT * FROM exercises WHERE muscleGroup = :muscleGroup ORDER BY name ASC")
    fun getExercisesByMuscle(muscleGroup: String): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercises WHERE equipment = :equipment ORDER BY name ASC")
    fun getExercisesByEquipment(equipment: String): Flow<List<ExerciseEntity>>

    @Query("""
        SELECT * FROM exercises 
        WHERE name LIKE '%' || :query || '%' 
           OR muscleGroup LIKE '%' || :query || '%' 
           OR instructionsUk LIKE '%' || :query || '%' 
        ORDER BY name ASC
    """)
    fun searchExercises(query: String): Flow<List<ExerciseEntity>>

    @Query("SELECT COUNT(*) FROM exercises")
    fun getExerciseCount(): Flow<Int>

    @Transaction
    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getExercisesWithOverloadHistory(): Flow<List<ExerciseWithOverloadHistory>>

    @Transaction
    @Query("SELECT * FROM exercises WHERE id = :id LIMIT 1")
    fun getExerciseWithOverloadHistoryById(id: Long): Flow<ExerciseWithOverloadHistory?>

    // --- UPDATE ---
    @Update
    suspend fun updateExercise(exercise: ExerciseEntity): Int

    @Update
    suspend fun updateExercises(exercises: List<ExerciseEntity>): Int

    @Query("""
        UPDATE exercises 
        SET defaultIncrementKg = :incrementKg,
            defaultTargetRepsMin = :repsMin,
            defaultTargetRepsMax = :repsMax,
            defaultTargetRir = :targetRir,
            progressionType = :progressionType
        WHERE id = :exerciseId
    """)
    suspend fun updateExerciseProgressionParams(
        exerciseId: Long,
        incrementKg: Double,
        repsMin: Int,
        repsMax: Int,
        targetRir: Int,
        progressionType: String
    )

    // --- DELETE ---
    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity): Int

    @Query("DELETE FROM exercises WHERE id = :exerciseId")
    suspend fun deleteExerciseById(exerciseId: Long)

    @Query("DELETE FROM exercises WHERE name = :name")
    suspend fun deleteExerciseByName(name: String)

    @Query("DELETE FROM exercises")
    suspend fun deleteAllExercises()

    // =========================================================================
    // 2. MESOCYCLES & WEEKLY PLANS CRUD (CREATE, READ, UPDATE, DELETE)
    // =========================================================================

    // --- CREATE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMesocycle(mesocycle: MesocycleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMesocycles(mesocycles: List<MesocycleEntity>): List<Long>

    @Upsert
    suspend fun upsertMesocycle(mesocycle: MesocycleEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMesocycleWeekPlan(plan: MesocycleWeekPlanEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMesocycleWeekPlans(plans: List<MesocycleWeekPlanEntity>): List<Long>

    @Transaction
    suspend fun createMesocycleWithWeekPlans(
        mesocycle: MesocycleEntity,
        weekPlans: List<MesocycleWeekPlanEntity>
    ): Long {
        if (mesocycle.isActive) {
            deactivateAllMesocycles()
        }
        val mesocycleId = insertMesocycle(mesocycle)
        if (weekPlans.isNotEmpty()) {
            insertMesocycleWeekPlans(weekPlans.map { it.copy(mesocycleId = mesocycleId) })
        }
        return mesocycleId
    }

    // --- READ ---
    @Query("SELECT * FROM mesocycles WHERE isActive = 1 LIMIT 1")
    fun getActiveMesocycle(): Flow<MesocycleEntity?>

    @Query("SELECT * FROM mesocycles WHERE isActive = 1 LIMIT 1")
    suspend fun getActiveMesocycleOnce(): MesocycleEntity?

    @Query("SELECT * FROM mesocycles WHERE id = :id LIMIT 1")
    suspend fun getMesocycleById(id: Long): MesocycleEntity?

    @Query("SELECT * FROM mesocycles WHERE id = :id LIMIT 1")
    fun getMesocycleByIdFlow(id: Long): Flow<MesocycleEntity?>

    @Query("SELECT * FROM mesocycles ORDER BY startDate DESC")
    fun getAllMesocycles(): Flow<List<MesocycleEntity>>

    @Query("SELECT * FROM mesocycles ORDER BY startDate DESC")
    suspend fun getAllMesocyclesOnce(): List<MesocycleEntity>

    @Query("SELECT * FROM mesocycles WHERE isCompleted = 1 ORDER BY startDate DESC")
    fun getCompletedMesocycles(): Flow<List<MesocycleEntity>>

    @Transaction
    @Query("SELECT * FROM mesocycles WHERE isActive = 1 LIMIT 1")
    fun getActiveMesocycleWithStructure(): Flow<MesocycleWithFullStructure?>

    @Transaction
    @Query("SELECT * FROM mesocycles WHERE id = :mesocycleId LIMIT 1")
    fun getMesocycleWithStructure(mesocycleId: Long): Flow<MesocycleWithFullStructure?>

    @Transaction
    @Query("SELECT * FROM mesocycles WHERE id = :mesocycleId LIMIT 1")
    suspend fun getMesocycleWithStructureOnce(mesocycleId: Long): MesocycleWithFullStructure?

    @Transaction
    @Query("SELECT * FROM mesocycles ORDER BY startDate DESC")
    fun getAllMesocyclesWithStructure(): Flow<List<MesocycleWithFullStructure>>

    @Query("SELECT * FROM mesocycle_week_plans WHERE mesocycleId = :mesocycleId ORDER BY weekNumber ASC")
    fun getWeekPlansForMesocycle(mesocycleId: Long): Flow<List<MesocycleWeekPlanEntity>>

    @Query("SELECT * FROM mesocycle_week_plans WHERE mesocycleId = :mesocycleId ORDER BY weekNumber ASC")
    suspend fun getWeekPlansForMesocycleOnce(mesocycleId: Long): List<MesocycleWeekPlanEntity>

    @Query("SELECT * FROM mesocycle_week_plans WHERE mesocycleId = :mesocycleId AND weekNumber = :weekNumber LIMIT 1")
    suspend fun getWeekPlanForMesocycleWeek(mesocycleId: Long, weekNumber: Int): MesocycleWeekPlanEntity?

    // --- UPDATE ---
    @Update
    suspend fun updateMesocycle(mesocycle: MesocycleEntity): Int

    @Update
    suspend fun updateMesocycleWeekPlan(plan: MesocycleWeekPlanEntity): Int

    @Query("UPDATE mesocycles SET currentWeek = :currentWeek WHERE id = :mesocycleId")
    suspend fun updateMesocycleCurrentWeek(mesocycleId: Long, currentWeek: Int)

    @Query("UPDATE mesocycles SET name = :name, goalDescriptionUk = :goalDescriptionUk WHERE id = :mesocycleId")
    suspend fun updateMesocycleNameAndGoal(mesocycleId: Long, name: String, goalDescriptionUk: String)

    @Query("UPDATE mesocycles SET isActive = :isActive, isCompleted = :isCompleted WHERE id = :mesocycleId")
    suspend fun setMesocycleActiveStatus(mesocycleId: Long, isActive: Boolean, isCompleted: Boolean)

    @Query("UPDATE mesocycles SET isActive = 0")
    suspend fun deactivateAllMesocycles()

    // --- DELETE ---
    @Delete
    suspend fun deleteMesocycle(mesocycle: MesocycleEntity): Int

    @Query("DELETE FROM mesocycles WHERE id = :mesocycleId")
    suspend fun deleteMesocycleById(mesocycleId: Long)

    @Delete
    suspend fun deleteMesocycleWeekPlan(plan: MesocycleWeekPlanEntity): Int

    @Query("DELETE FROM mesocycle_week_plans WHERE mesocycleId = :mesocycleId")
    suspend fun deleteWeekPlansForMesocycle(mesocycleId: Long)

    @Query("DELETE FROM mesocycle_week_plans")
    suspend fun deleteAllMesocycleWeekPlans()

    @Query("DELETE FROM mesocycles")
    suspend fun deleteAllMesocycles()

    @Query("""
        DELETE FROM workout_sets WHERE workoutExerciseId IN (
            SELECT we.id FROM workout_exercises we
            INNER JOIN workout_sessions s ON we.sessionId = s.id
            WHERE s.mesocycleId = :mesocycleId
        )
    """)
    suspend fun deleteSetsForMesocycle(mesocycleId: Long)

    @Query("""
        DELETE FROM workout_exercises WHERE sessionId IN (
            SELECT id FROM workout_sessions WHERE mesocycleId = :mesocycleId
        )
    """)
    suspend fun deleteExercisesForMesocycle(mesocycleId: Long)

    @Query("DELETE FROM workout_sessions WHERE mesocycleId = :mesocycleId")
    suspend fun deleteSessionsForMesocycle(mesocycleId: Long)

    @Query("DELETE FROM progressive_overload_logs WHERE mesocycleId = :mesocycleId")
    suspend fun deleteOverloadLogsForMesocycle(mesocycleId: Long)

    @Transaction
    suspend fun deleteMesocycleWithHistory(mesocycleId: Long) {
        deleteSetsForMesocycle(mesocycleId)
        deleteExercisesForMesocycle(mesocycleId)
        deleteSessionsForMesocycle(mesocycleId)
        deleteOverloadLogsForMesocycle(mesocycleId)
        deleteWeekPlansForMesocycle(mesocycleId)
        deleteMesocycleById(mesocycleId)
    }

    // =========================================================================
    // 3. WORKOUT HISTORY (SESSIONS) CRUD (CREATE, READ, UPDATE, DELETE)
    // =========================================================================

    // --- CREATE ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: WorkoutSessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSessions(sessions: List<WorkoutSessionEntity>): List<Long>

    @Upsert
    suspend fun upsertSession(session: WorkoutSessionEntity): Long

    @Transaction
    suspend fun insertCompleteWorkoutSession(
        session: WorkoutSessionEntity,
        exercisesWithSets: List<Pair<WorkoutExerciseEntity, List<WorkoutSetEntity>>>
    ): Long {
        val sessionId = insertSession(session)
        for ((exercise, sets) in exercisesWithSets) {
            val workoutExerciseId = insertWorkoutExercise(exercise.copy(sessionId = sessionId))
            if (sets.isNotEmpty()) {
                insertSets(sets.map { it.copy(workoutExerciseId = workoutExerciseId) })
            }
        }
        return sessionId
    }

    // --- READ ---
    @Query("SELECT * FROM workout_sessions WHERE mesocycleId = :mesocycleId ORDER BY weekNumber ASC, id ASC")
    fun getSessionsForMesocycle(mesocycleId: Long): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE mesocycleId = :mesocycleId ORDER BY weekNumber ASC, id ASC")
    suspend fun getSessionsForMesocycleOnce(mesocycleId: Long): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE mesocycleId = :mesocycleId AND weekNumber = :weekNumber ORDER BY id ASC")
    fun getSessionsForMesocycleWeek(mesocycleId: Long, weekNumber: Int): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateTimestamp DESC")
    fun getAllSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions ORDER BY dateTimestamp DESC")
    suspend fun getAllSessionsOnce(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY dateTimestamp DESC")
    fun getCompletedSessions(): Flow<List<WorkoutSessionEntity>>

    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY dateTimestamp DESC")
    suspend fun getCompletedSessionsOnce(): List<WorkoutSessionEntity>

    @Query("SELECT * FROM workout_sessions WHERE dateTimestamp BETWEEN :startTimestamp AND :endTimestamp ORDER BY dateTimestamp DESC")
    fun getSessionsByDateRange(startTimestamp: Long, endTimestamp: Long): Flow<List<WorkoutSessionEntity>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE isCompleted = 1 ORDER BY dateTimestamp DESC")
    fun getCompletedSessionsWithDetails(): Flow<List<WorkoutSessionWithDetails>>

    @Transaction
    @Query("SELECT * FROM workout_sessions ORDER BY dateTimestamp DESC")
    fun getAllSessionsWithDetails(): Flow<List<WorkoutSessionWithDetails>>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    fun getSessionWithDetails(sessionId: Long): Flow<WorkoutSessionWithDetails?>

    @Transaction
    @Query("SELECT * FROM workout_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionWithDetailsOnce(sessionId: Long): WorkoutSessionWithDetails?

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    fun getSessionByIdFlow(id: Long): Flow<WorkoutSessionEntity?>

    @Query("SELECT * FROM workout_sessions WHERE id = :id LIMIT 1")
    suspend fun getSessionById(id: Long): WorkoutSessionEntity?

    // --- UPDATE ---
    @Update
    suspend fun updateSession(session: WorkoutSessionEntity): Int

    @Query("""
        UPDATE workout_sessions 
        SET title = :title, 
            notes = :notes, 
            wellBeingRating = :wellBeingRating 
        WHERE id = :sessionId
    """)
    suspend fun updateSessionTitleAndNotes(
        sessionId: Long,
        title: String,
        notes: String,
        wellBeingRating: Int?
    )

    @Query("""
        UPDATE workout_sessions 
        SET isCompleted = :isCompleted,
            durationSeconds = :durationSeconds,
            totalTonnageKg = :totalTonnageKg,
            totalWorkingSets = :totalWorkingSets,
            averageRir = :averageRir,
            prCount = :prCount,
            progressionStatus = :progressionStatus
        WHERE id = :sessionId
    """)
    suspend fun updateSessionCompletionMetrics(
        sessionId: Long,
        isCompleted: Boolean,
        durationSeconds: Long,
        totalTonnageKg: Double,
        totalWorkingSets: Int,
        averageRir: Float?,
        prCount: Int,
        progressionStatus: String
    )

    // --- DELETE ---
    @Delete
    suspend fun deleteSessionEntity(session: WorkoutSessionEntity): Int

    @Query("DELETE FROM workout_sessions WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Query("""
        DELETE FROM workout_sets WHERE workoutExerciseId IN (
            SELECT id FROM workout_exercises WHERE sessionId = :sessionId
        )
    """)
    suspend fun deleteSetsForSession(sessionId: Long)

    @Query("DELETE FROM workout_exercises WHERE sessionId = :sessionId")
    suspend fun deleteExercisesForSession(sessionId: Long)

    @Transaction
    suspend fun deleteSessionWithDetails(sessionId: Long) {
        deleteSetsForSession(sessionId)
        deleteExercisesForSession(sessionId)
        deleteOverloadLogsForSession(sessionId)
        deleteSession(sessionId)
    }

    // =========================================================================
    // 4. WORKOUT EXERCISES & SETS CRUD (CREATE, READ, UPDATE, DELETE)
    // =========================================================================

    // --- WORKOUT EXERCISES ---
    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    fun getExercisesForSession(sessionId: Long): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises WHERE sessionId = :sessionId ORDER BY orderIndex ASC")
    suspend fun getExercisesForSessionOnce(sessionId: Long): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM workout_exercises WHERE id = :id LIMIT 1")
    suspend fun getWorkoutExerciseById(id: Long): WorkoutExerciseEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(exercise: WorkoutExerciseEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>): List<Long>

    @Update
    suspend fun updateWorkoutExercise(exercise: WorkoutExerciseEntity): Int

    @Update
    suspend fun updateWorkoutExercises(exercises: List<WorkoutExerciseEntity>): Int

    @Delete
    suspend fun deleteWorkoutExerciseEntity(exercise: WorkoutExerciseEntity): Int

    @Query("DELETE FROM workout_exercises WHERE id = :id")
    suspend fun deleteWorkoutExercise(id: Long)

    @Transaction
    suspend fun deleteWorkoutExerciseWithSets(workoutExerciseId: Long) {
        deleteSetsForExercise(workoutExerciseId)
        deleteWorkoutExercise(workoutExerciseId)
    }

    // --- WORKOUT SETS ---
    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    fun getSetsForExercise(workoutExerciseId: Long): Flow<List<WorkoutSetEntity>>

    @Query("SELECT * FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY setNumber ASC")
    suspend fun getSetsForExerciseOnce(workoutExerciseId: Long): List<WorkoutSetEntity>

    @Query("SELECT * FROM workout_sets WHERE id = :id LIMIT 1")
    suspend fun getSetById(id: Long): WorkoutSetEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: WorkoutSetEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSets(sets: List<WorkoutSetEntity>): List<Long>

    @Update
    suspend fun updateSet(set: WorkoutSetEntity): Int

    @Update
    suspend fun updateSets(sets: List<WorkoutSetEntity>): Int

    @Delete
    suspend fun deleteSet(set: WorkoutSetEntity): Int

    @Query("DELETE FROM workout_sets WHERE id = :id")
    suspend fun deleteSetById(id: Long)

    @Query("DELETE FROM workout_sets WHERE workoutExerciseId = :workoutExerciseId")
    suspend fun deleteSetsForExercise(workoutExerciseId: Long)

    // Recent sets history for an exercise (for Double Progression & PRs)
    @Query("""
        SELECT ws.* FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        INNER JOIN workout_sessions s ON we.sessionId = s.id
        WHERE we.exerciseName = :exerciseName AND ws.isCompleted = 1 AND ws.isWarmup = 0
        ORDER BY ws.timestamp DESC LIMIT 20
    """)
    suspend fun getRecentCompletedSetsForExercise(exerciseName: String): List<WorkoutSetEntity>

    // All completed working sets across all sessions for PR tracking
    @Query("""
        SELECT ws.*, we.exerciseName, we.muscleGroup FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        WHERE ws.isCompleted = 1 AND ws.isWarmup = 0
        ORDER BY ws.timestamp DESC
    """)
    fun getAllCompletedSetsWithExercise(): Flow<List<WorkoutSetWithExerciseInfo>>

    @Query("""
        SELECT ws.*, we.exerciseName, we.muscleGroup FROM workout_sets ws
        INNER JOIN workout_exercises we ON ws.workoutExerciseId = we.id
        WHERE we.exerciseName = :exerciseName AND ws.isCompleted = 1 AND ws.isWarmup = 0
        ORDER BY ws.timestamp DESC
    """)
    fun getCompletedSetsForExerciseHistory(exerciseName: String): Flow<List<WorkoutSetWithExerciseInfo>>

    // =========================================================================
    // 5. PROGRESSIVE OVERLOAD LOGS CRUD (CREATE, READ, UPDATE, DELETE)
    // =========================================================================
    @Query("SELECT * FROM progressive_overload_logs ORDER BY timestamp DESC")
    fun getAllProgressiveOverloadLogs(): Flow<List<ProgressiveOverloadLogEntity>>

    @Query("SELECT * FROM progressive_overload_logs ORDER BY timestamp DESC")
    suspend fun getAllProgressiveOverloadLogsOnce(): List<ProgressiveOverloadLogEntity>

    @Query("SELECT * FROM progressive_overload_logs WHERE exerciseName = :exerciseName ORDER BY timestamp DESC")
    fun getOverloadLogsForExercise(exerciseName: String): Flow<List<ProgressiveOverloadLogEntity>>

    @Query("SELECT * FROM progressive_overload_logs WHERE exerciseName = :exerciseName ORDER BY timestamp DESC")
    suspend fun getOverloadLogsForExerciseOnce(exerciseName: String): List<ProgressiveOverloadLogEntity>

    @Query("SELECT * FROM progressive_overload_logs WHERE exerciseName = :exerciseName ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLatestOverloadLogForExercise(exerciseName: String): ProgressiveOverloadLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressiveOverloadLog(log: ProgressiveOverloadLogEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressiveOverloadLogs(logs: List<ProgressiveOverloadLogEntity>): List<Long>

    @Update
    suspend fun updateProgressiveOverloadLog(log: ProgressiveOverloadLogEntity): Int

    @Delete
    suspend fun deleteProgressiveOverloadLog(log: ProgressiveOverloadLogEntity): Int

    @Query("DELETE FROM progressive_overload_logs WHERE id = :id")
    suspend fun deleteProgressiveOverloadLogById(id: Long)

    @Query("DELETE FROM progressive_overload_logs WHERE sessionId = :sessionId")
    suspend fun deleteOverloadLogsForSession(sessionId: Long)

    @Query("DELETE FROM progressive_overload_logs")
    suspend fun deleteAllProgressiveOverloadLogs()

    // =========================================================================
    // 6. READINESS, MEASUREMENTS, GYM PROFILES & SCHEDULE LOGS
    // =========================================================================
    @Query("SELECT * FROM readiness_checkins ORDER BY dateTimestamp DESC LIMIT 1")
    fun getLatestReadinessCheckin(): Flow<ReadinessCheckinEntity?>

    @Query("SELECT * FROM readiness_checkins ORDER BY dateTimestamp DESC LIMIT 30")
    fun getReadinessHistory(): Flow<List<ReadinessCheckinEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReadinessCheckin(checkin: ReadinessCheckinEntity): Long

    // Body Measurements
    @Query("SELECT * FROM body_measurements ORDER BY dateTimestamp DESC")
    fun getAllBodyMeasurements(): Flow<List<BodyMeasurementEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBodyMeasurement(measurement: BodyMeasurementEntity): Long

    @Update
    suspend fun updateBodyMeasurement(measurement: BodyMeasurementEntity): Int

    @Delete
    suspend fun deleteBodyMeasurement(measurement: BodyMeasurementEntity)

    // Gym Profiles
    @Query("SELECT * FROM gym_profiles")
    fun getAllGymProfiles(): Flow<List<GymProfileEntity>>

    @Query("SELECT * FROM gym_profiles WHERE isSelected = 1 LIMIT 1")
    fun getSelectedGymProfile(): Flow<GymProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGymProfile(profile: GymProfileEntity): Long

    @Update
    suspend fun updateGymProfile(profile: GymProfileEntity)

    @Query("DELETE FROM gym_profiles WHERE id = :id")
    suspend fun deleteGymProfileById(id: Long)

    @Query("DELETE FROM gym_profiles WHERE name LIKE '%Домашній зал%' OR name LIKE '%Мінімум спорядження%'")
    suspend fun deleteHomeGymProfile()

    // User Volume Config
    @Query("SELECT * FROM user_volume_config WHERE id = 1 LIMIT 1")
    fun getUserVolumeConfig(): Flow<UserVolumeConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveUserVolumeConfig(config: UserVolumeConfigEntity)

    // Reset operations
    @Query("DELETE FROM workout_sets")
    suspend fun deleteAllWorkoutSets()

    @Query("DELETE FROM workout_exercises")
    suspend fun deleteAllWorkoutExercises()

    @Query("DELETE FROM workout_sessions")
    suspend fun deleteAllWorkoutSessions()

    @Query("DELETE FROM readiness_checkins")
    suspend fun deleteAllReadinessCheckins()

    @Query("DELETE FROM body_measurements")
    suspend fun deleteAllBodyMeasurements()

    // Internal Schedules
    @Query("SELECT * FROM internal_schedules ORDER BY weekday ASC")
    fun getAllInternalSchedules(): Flow<List<InternalScheduleEntity>>

    @Query("SELECT * FROM internal_schedules ORDER BY weekday ASC")
    suspend fun getInternalSchedulesOnce(): List<InternalScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInternalSchedules(schedules: List<InternalScheduleEntity>)

    @Query("DELETE FROM internal_schedules")
    suspend fun deleteAllInternalSchedules()

    // Trigger Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTriggerLog(log: TriggerLogEntity): Long

    @Query("SELECT * FROM trigger_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTriggerLogs(limit: Int = 50): Flow<List<TriggerLogEntity>>

    @Query("DELETE FROM trigger_logs")
    suspend fun deleteAllTriggerLogs()

    // Reminder Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminderLog(log: ReminderLogEntity): Long

    @Query("SELECT * FROM reminder_logs WHERE weekday = :weekday ORDER BY sentTimestamp DESC LIMIT 5")
    suspend fun getRecentReminderLogs(weekday: Int): List<ReminderLogEntity>

    @Query("DELETE FROM reminder_logs")
    suspend fun deleteAllReminderLogs()

    // Weekly Summaries
    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartTimestamp DESC LIMIT 1")
    fun getLatestWeeklySummary(): Flow<WeeklySummaryEntity?>

    @Query("SELECT * FROM weekly_summaries ORDER BY weekStartTimestamp DESC")
    fun getAllWeeklySummaries(): Flow<List<WeeklySummaryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklySummary(summary: WeeklySummaryEntity): Long

    @Query("DELETE FROM weekly_summaries")
    suspend fun deleteAllWeeklySummaries()

    @Transaction
    suspend fun resetAllProgress() {
        deleteAllProgressiveOverloadLogs()
        deleteAllWorkoutSets()
        deleteAllWorkoutExercises()
        deleteAllWorkoutSessions()
        deleteAllReadinessCheckins()
        deleteAllMesocycleWeekPlans()
        deleteAllMesocycles()
        deleteAllBodyMeasurements()
        deleteAllInternalSchedules()
        deleteAllTriggerLogs()
        deleteAllReminderLogs()
        deleteAllWeeklySummaries()
    }

    // Cleanup empty / cancelled sessions (0 kg / abandoned)
    @Query("""
        DELETE FROM workout_sets WHERE workoutExerciseId IN (
            SELECT we.id FROM workout_exercises we
            INNER JOIN workout_sessions s ON we.sessionId = s.id
            WHERE s.totalTonnageKg = 0 OR s.totalTonnageKg IS NULL
        )
    """)
    suspend fun deleteSetsForZeroTonnageSessions()

    @Query("""
        DELETE FROM workout_exercises WHERE sessionId IN (
            SELECT s.id FROM workout_sessions s
            WHERE s.totalTonnageKg = 0 OR s.totalTonnageKg IS NULL
        )
    """)
    suspend fun deleteExercisesForZeroTonnageSessions()

    @Query("DELETE FROM workout_sessions WHERE totalTonnageKg = 0 OR totalTonnageKg IS NULL")
    suspend fun deleteZeroTonnageSessions()

    @Transaction
    suspend fun cleanupEmptyCancelledSessions() {
        deleteSetsForZeroTonnageSessions()
        deleteExercisesForZeroTonnageSessions()
        deleteZeroTonnageSessions()
    }
}

data class WorkoutSetWithExerciseInfo(
    val id: Long,
    val workoutExerciseId: Long,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val targetRepsMin: Int,
    val targetRepsMax: Int,
    val rir: Int,
    val isWarmup: Boolean,
    val isCompleted: Boolean,
    val timestamp: Long,
    val targetWeightKg: Double = weightKg,
    val rpe: Float = (10 - rir).coerceIn(5, 10).toFloat(),
    val estimated1RmKg: Double = if (reps > 0 && !isWarmup) {
        if (reps == 1) weightKg else weightKg * (1.0 + reps / 30.0)
    } else 0.0,
    val tonnageKg: Double = if (!isWarmup) weightKg * reps else 0.0,
    val isPersonalRecord: Boolean = false,
    val setType: String = if (isWarmup) "WARMUP" else "WORKING",
    val exerciseName: String,
    val muscleGroup: String
)
