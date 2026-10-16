package com.example

import com.example.data.WorkoutSetWithExerciseInfo
import com.example.engine.MuscleRecoveryEngine
import com.example.engine.RecoveryState
import com.example.model.MuscleGroup
import org.junit.Assert.*
import org.junit.Test

class MuscleRecoveryEngineTest {

    @Test
    fun testPhysiologicalRecoveryHours() {
        assertEquals(72, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.BACK))
        assertEquals(72, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.QUADS))
        assertEquals(72, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.HAMSTRINGS))
        assertEquals(72, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.GLUTES))

        assertEquals(60, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.CHEST))
        assertEquals(60, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.SHOULDERS))

        assertEquals(48, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.BICEPS))
        assertEquals(48, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.TRICEPS))
        assertEquals(48, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.ABS))
        assertEquals(48, MuscleRecoveryEngine.getPhysiologicalRecoveryHours(MuscleGroup.OTHER))
    }

    @Test
    fun testUntrainedMusclesAreFullyRecoveredAndExcludesOther() {
        val summary = MuscleRecoveryEngine.computeRecoverySummary(emptyList())
        assertEquals(11, summary.readyMuscleGroups.size)
        assertTrue(summary.recoveringMuscleGroups.isEmpty())
        assertFalse("OTHER should be excluded from physiological recovery card", summary.readyMuscleGroups.contains(MuscleGroup.OTHER))

        val chest = summary.statuses.first { it.muscleGroup == MuscleGroup.CHEST }
        assertEquals(100, chest.recoveryPercent)
        assertEquals(RecoveryState.RECOVERED, chest.state)
        assertEquals(0.0, chest.hoursRemaining, 0.01)

        // Verify parsing for OTHER
        assertEquals(MuscleGroup.OTHER, MuscleGroup.fromStringOrNull("Інше"))
        assertEquals(MuscleGroup.OTHER, MuscleGroup.fromStringOrNull("OTHER"))
        assertEquals(MuscleGroup.OTHER, MuscleGroup.fromStringOrNull("різне"))
    }

    @Test
    fun testRecentlyTrainedMuscleIsExhaustedThenRecovered() {
        val now = 1000000000000L
        val justTrainedSet = WorkoutSetWithExerciseInfo(
            id = 1,
            workoutExerciseId = 1,
            setNumber = 1,
            weightKg = 100.0,
            reps = 8,
            targetRepsMin = 8,
            targetRepsMax = 10,
            rir = 2,
            isWarmup = false,
            isCompleted = true,
            timestamp = now - (2 * 3600 * 1000L), // 2 hours ago
            exerciseName = "Жим лежачи",
            muscleGroup = "CHEST"
        )

        val summary2h = MuscleRecoveryEngine.computeRecoverySummary(listOf(justTrainedSet), now)
        val chest2h = summary2h.statuses.first { it.muscleGroup == MuscleGroup.CHEST }
        assertEquals(RecoveryState.EXHAUSTED, chest2h.state)
        assertTrue(chest2h.recoveryPercent < 10)
        assertEquals(58.0, chest2h.hoursRemaining, 0.1)

        // 30 hours later (50% of 60h)
        val summary30h = MuscleRecoveryEngine.computeRecoverySummary(listOf(justTrainedSet), now + (28 * 3600 * 1000L))
        val chest30h = summary30h.statuses.first { it.muscleGroup == MuscleGroup.CHEST }
        assertEquals(RecoveryState.RECOVERING, chest30h.state)
        assertEquals(50, chest30h.recoveryPercent)

        // 60 hours later (fully recovered)
        val summary60h = MuscleRecoveryEngine.computeRecoverySummary(listOf(justTrainedSet), now + (58 * 3600 * 1000L))
        val chest60h = summary60h.statuses.first { it.muscleGroup == MuscleGroup.CHEST }
        assertEquals(RecoveryState.RECOVERED, chest60h.state)
        assertEquals(100, chest60h.recoveryPercent)
        assertEquals(0.0, chest60h.hoursRemaining, 0.01)
    }

    @Test
    fun testExerciseWithMultipleMusclesEngagesAllOfThem() {
        val now = 1000000000000L
        val multiMuscleSet = WorkoutSetWithExerciseInfo(
            id = 10,
            workoutExerciseId = 2,
            setNumber = 1,
            weightKg = 80.0,
            reps = 10,
            targetRepsMin = 8,
            targetRepsMax = 12,
            rir = 2,
            isWarmup = false,
            isCompleted = true,
            timestamp = now - (1 * 3600 * 1000L), // 1 hour ago
            exerciseName = "Жим лежачи на похилій",
            muscleGroup = "CHEST,TRICEPS,SHOULDERS"
        )

        val summary = MuscleRecoveryEngine.computeRecoverySummary(listOf(multiMuscleSet), now)
        val chest = summary.statuses.first { it.muscleGroup == MuscleGroup.CHEST }
        val triceps = summary.statuses.first { it.muscleGroup == MuscleGroup.TRICEPS }
        val shoulders = summary.statuses.first { it.muscleGroup == MuscleGroup.SHOULDERS }
        val back = summary.statuses.first { it.muscleGroup == MuscleGroup.BACK }

        assertEquals(RecoveryState.EXHAUSTED, chest.state)
        assertEquals(RecoveryState.EXHAUSTED, triceps.state)
        assertEquals(RecoveryState.EXHAUSTED, shoulders.state)
        assertEquals(RecoveryState.RECOVERED, back.state)
    }
}
