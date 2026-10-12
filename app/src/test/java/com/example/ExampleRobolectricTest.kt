package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.*
import com.example.engine.PlannedDay
import com.example.engine.PlannedExercise
import com.example.engine.ProgressionModel
import com.example.model.EquipmentType
import com.example.model.MuscleGroup
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var dao: ApexDao
    private lateinit var repository: ApexRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.apexDao()
        repository = ApexRepository(dao)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("APEX Strength", appName)
    }

    @Test
    fun `room schema stores exercises mesocycles workout history and progressive overload`() = runBlocking {
        // 1. Seed exercises
        populateInitialData(dao)
        val exercises = dao.getAllExercisesOnce()
        assertTrue(exercises.isNotEmpty())
        val benchPress = exercises.first { it.name == "Жим штанги лежачи" }
        assertEquals(2.5, benchPress.defaultIncrementKg, 0.01)
        assertEquals("DOUBLE_PROGRESSION", benchPress.progressionType)

        // 2. Verify initial mesocycle & week plans relation
        val activeStructure = dao.getActiveMesocycleWithStructure().first()
        assertNotNull(activeStructure)
        assertEquals(5, activeStructure!!.weekPlans.size)
        assertTrue(activeStructure.weekPlans.last().isDeload)

        // 3. Create structured mesocycle and verify progressive overload recording
        val mesoId = repository.createStructuredMesocycle(
            name = "Силова прогресія Блок",
            totalWeeks = 4,
            model = ProgressionModel.DOUBLE_PROGRESSION,
            plannedDays = listOf(
                PlannedDay(
                    dayTitle = "День 1: Верх",
                    exercises = listOf(
                        PlannedExercise(
                            exerciseName = "Жим штанги лежачи",
                            muscleGroup = "CHEST",
                            equipment = EquipmentType.BARBELL,
                            baseWeightKg = 80.0,
                            baseRepsMin = 8,
                            baseRepsMax = 12,
                            isPriority = true
                        )
                    )
                )
            ),
            priorityMuscles = listOf(MuscleGroup.CHEST)
        )

        val sessions = dao.getSessionsForMesocycleOnce(mesoId)
        assertEquals(4, sessions.size)

        // Complete sets on Week 1 session with progressive overload (82.5 kg x 10)
        val firstSession = sessions.first()
        val sessionDetails = dao.getSessionWithDetailsOnce(firstSession.id)
        assertNotNull(sessionDetails)
        val workoutEx = sessionDetails!!.exercises.first()
        for (set in workoutEx.sets) {
            dao.updateSet(
                set.copy(
                    weightKg = 82.5,
                    reps = 10,
                    rir = 2,
                    isCompleted = true,
                    estimated1RmKg = 82.5 * (1.0 + 10.0 / 30.0),
                    tonnageKg = 825.0
                )
            )
        }
        dao.updateSession(firstSession.copy(isCompleted = true, totalTonnageKg = 825.0 * workoutEx.sets.size))
        repository.recordSessionProgressiveOverload(firstSession.id)

        val overloadLogs = dao.getOverloadLogsForExerciseOnce("Жим штанги лежачи")
        assertTrue(overloadLogs.isNotEmpty())
        val latestLog = overloadLogs.first()
        assertEquals(82.5, latestLog.newWeightKg, 0.01)
        assertEquals(10, latestLog.newReps)

        val updatedSession = dao.getSessionById(firstSession.id)
        assertEquals("PROGRESSED", updatedSession?.progressionStatus)
        assertTrue((updatedSession?.totalWorkingSets ?: 0) >= 3)
    }
}
