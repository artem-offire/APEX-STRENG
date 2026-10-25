package com.example

import com.example.model.MuscleGroup
import com.example.model.MuscleHead
import org.junit.Assert.*
import org.junit.Test

class MuscleHeadTest {

    @Test
    fun testHeadsBelongToTheirGroup() {
        MuscleGroup.values().forEach { group ->
            MuscleHead.headsFor(group).forEach { assertEquals(group, it.group) }
        }
        assertTrue(MuscleHead.headsFor(MuscleGroup.HAMSTRINGS).isEmpty())
        assertEquals(3, MuscleHead.headsFor(MuscleGroup.SHOULDERS).size)
    }

    @Test
    fun testCatalogExercisesMapToHeads() {
        assertEquals(
            listOf(MuscleHead.CHEST_UPPER),
            MuscleHead.forExercise("Жим гантелей на похилій лаві (30°)", MuscleGroup.CHEST)
        )
        assertEquals(
            listOf(MuscleHead.DELTS_SIDE),
            MuscleHead.forExercise("Махи гантелями через сторони", MuscleGroup.SHOULDERS)
        )
        assertEquals(
            listOf(MuscleHead.DELTS_REAR),
            MuscleHead.forExercise("Відведення на задню дельту в кросовері (Face Pull)", MuscleGroup.SHOULDERS)
        )
        assertEquals(
            listOf(MuscleHead.DELTS_FRONT, MuscleHead.DELTS_SIDE),
            MuscleHead.forExercise("Жим штанги стоячи (Армійський жим)", MuscleGroup.SHOULDERS)
        )
        assertEquals(
            listOf(MuscleHead.BRACHIALIS),
            MuscleHead.forExercise("Молоткові згинання з гантелями (Хаммер)", MuscleGroup.BICEPS)
        )
        assertEquals(
            listOf(MuscleHead.TRICEPS_LONG),
            MuscleHead.forExercise("Французький жим з EZ-грифом лежачи", MuscleGroup.TRICEPS)
        )
    }

    @Test
    fun testUnknownGroupHasNoHeads() {
        assertTrue(MuscleHead.forExercise("Будь-що", null).isEmpty())
        assertTrue(MuscleHead.forExercise("Будь-що", MuscleGroup.OTHER).isEmpty())
    }
}
