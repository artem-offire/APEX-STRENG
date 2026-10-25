package com.example.engine

import com.example.data.ExerciseEntity
import com.example.data.WorkoutSessionEntity
import com.example.data.WorkoutSetWithExerciseInfo
import com.example.model.FrequentWorkoutItem
import com.example.model.MuscleGroup
import com.example.model.ReadinessBadgeType

object FrequentWorkoutEngine {

    /**
     * Формує список швидких/частих тренувань на основі поточної фізіологічної готовності м'язів,
     * з найвищим пріоритетом для тих тренувань та м'язових груп, які досягли суперкомпенсації саме сьогодні.
     */
    fun buildFrequentWorkouts(
        completedSessions: List<WorkoutSessionEntity>,
        allCompletedSets: List<WorkoutSetWithExerciseInfo>,
        recoverySummary: MuscleRecoverySummary,
        allCatalogExercises: List<ExerciseEntity>
    ): List<FrequentWorkoutItem> {
        val readyTodayMuscles = recoverySummary.readyTodayMuscleGroups.toSet()
        val allReadyMuscles = recoverySummary.readyMuscleGroups.toSet()

        // 1. Згрупувати завершені сети за назвою тренування (через відповідні сесії)
        val sessionById = completedSessions.associateBy { it.id }
        val setsBySessionId = allCompletedSets.groupBy { setInfo ->
            // Знаходимо сесію за часом або зв'язком
            completedSessions.find { s ->
                s.isCompleted && Math.abs(s.dateTimestamp - setInfo.timestamp) < 43_200_000L
            }?.id
        }

        // Збираємо унікальні минулі тренування
        val uniqueHistoryMap = mutableMapOf<String, WorkoutSessionEntity>()
        for (session in completedSessions) {
            val cleanTitle = session.title.trim()
            if (cleanTitle.isNotEmpty() && !uniqueHistoryMap.containsKey(cleanTitle)) {
                uniqueHistoryMap[cleanTitle] = session
            }
        }

        val historyItems = mutableListOf<FrequentWorkoutItem>()

        for ((title, session) in uniqueHistoryMap) {
            val sessionSets = setsBySessionId[session.id] ?: emptyList()
            val musclesInWorkout = sessionSets
                .flatMap { MuscleGroup.parseMuscleGroups(it.muscleGroup) }
                .distinct()
                .filter { it != MuscleGroup.OTHER }

            // Якщо не вдалося витягти групи з сетів, пробуємо спарсити з назви
            val targetMuscles = if (musclesInWorkout.isNotEmpty()) {
                musclesInWorkout
            } else {
                guessMusclesFromTitle(title)
            }

            val hasReadyToday = targetMuscles.any { readyTodayMuscles.contains(it) }
            val isAllReady = targetMuscles.isNotEmpty() && targetMuscles.all { allReadyMuscles.contains(it) }

            val badgeType = when {
                hasReadyToday -> ReadinessBadgeType.PEAK_TODAY
                isAllReady -> ReadinessBadgeType.FULLY_READY
                else -> ReadinessBadgeType.RECOVERING
            }

            val badgeText = when (badgeType) {
                ReadinessBadgeType.PEAK_TODAY -> "🔥 Свіжі сьогодні"
                ReadinessBadgeType.FULLY_READY -> "⚡ 100% готові"
                ReadinessBadgeType.RECOVERING -> "⏳ Відновлення"
            }

            val musclesLabel = if (targetMuscles.isNotEmpty()) {
                targetMuscles.take(3).joinToString(", ") { it.displayNameUk }
            } else {
                "Комплексне"
            }

            historyItems.add(
                FrequentWorkoutItem(
                    id = "history_${session.id}",
                    title = title,
                    subtitle = musclesLabel,
                    targetMuscles = targetMuscles,
                    badgeType = badgeType,
                    badgeText = badgeText,
                    isFromHistory = true,
                    sourceSessionId = session.id
                )
            )
        }

        // 2. Смарт-шаблони для м'язів, що стали доступні сьогодні або готові
        val smartReadyItems = mutableListOf<FrequentWorkoutItem>()

        // Якщо є м'язи, що стали свіжими сьогодні, але для них немає тренування в топі історії
        if (readyTodayMuscles.isNotEmpty()) {
            val templatesForToday = generateSmartSplitsForMuscles(readyTodayMuscles.toList(), isPeakToday = true)
            smartReadyItems.addAll(templatesForToday)
        }

        // Якщо взагалі мало варіантів (новий користувач або мало історії), додаємо спліти з готових м'язів
        if (historyItems.size < 3 && allReadyMuscles.isNotEmpty()) {
            val templatesForReady = generateSmartSplitsForMuscles(allReadyMuscles.toList(), isPeakToday = false)
            for (tpl in templatesForReady) {
                if (smartReadyItems.none { it.title == tpl.title }) {
                    smartReadyItems.add(tpl)
                }
            }
        }

        // 3. Об'єднуємо та сортуємо:
        // Пріоритет: PEAK_TODAY (1) -> FULLY_READY (2) -> RECOVERING (3)
        val combined = (smartReadyItems + historyItems).distinctBy { it.title.trim().lowercase() }

        val sorted = combined.sortedWith(
            compareBy<FrequentWorkoutItem> { item ->
                when (item.badgeType) {
                    ReadinessBadgeType.PEAK_TODAY -> 0
                    ReadinessBadgeType.FULLY_READY -> 1
                    ReadinessBadgeType.RECOVERING -> 2
                }
            }.thenByDescending { it.isFromHistory }
        )

        return sorted.take(6)
    }

    private fun guessMusclesFromTitle(title: String): List<MuscleGroup> {
        val lower = title.lowercase()
        val result = mutableListOf<MuscleGroup>()
        if (lower.contains("груд") || lower.contains("жим") || lower.contains("верх")) {
            result.add(MuscleGroup.CHEST)
        }
        if (lower.contains("спин") || lower.contains("тяг") || lower.contains("верх")) {
            result.add(MuscleGroup.BACK)
        }
        if (lower.contains("ног") || lower.contains("прис") || lower.contains("низ")) {
            result.add(MuscleGroup.QUADS)
            result.add(MuscleGroup.HAMSTRINGS)
        }
        if (lower.contains("плеч") || lower.contains("дельт")) {
            result.add(MuscleGroup.SHOULDERS)
        }
        if (lower.contains("рук") || lower.contains("біцепс")) {
            result.add(MuscleGroup.BICEPS)
        }
        if (lower.contains("трицепс")) {
            result.add(MuscleGroup.TRICEPS)
        }
        return if (result.isNotEmpty()) result else listOf(MuscleGroup.CHEST, MuscleGroup.BACK)
    }

    private fun generateSmartSplitsForMuscles(
        muscles: List<MuscleGroup>,
        isPeakToday: Boolean
    ): List<FrequentWorkoutItem> {
        val badge = if (isPeakToday) ReadinessBadgeType.PEAK_TODAY else ReadinessBadgeType.FULLY_READY
        val badgeText = if (isPeakToday) "🔥 Свіжі сьогодні" else "⚡ 100% готові"
        val prefix = if (isPeakToday) "⭐ " else ""

        val items = mutableListOf<FrequentWorkoutItem>()

        val hasPush = muscles.contains(MuscleGroup.CHEST) || muscles.contains(MuscleGroup.SHOULDERS) || muscles.contains(MuscleGroup.TRICEPS)
        val hasPull = muscles.contains(MuscleGroup.BACK) || muscles.contains(MuscleGroup.BICEPS)
        val hasLegs = muscles.contains(MuscleGroup.QUADS) || muscles.contains(MuscleGroup.HAMSTRINGS) || muscles.contains(MuscleGroup.GLUTES)

        if (hasPush) {
            val target = listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS).filter { muscles.contains(it) }
            val names = target.joinToString(" + ") { it.displayNameUk }
            items.add(
                FrequentWorkoutItem(
                    id = "smart_push_${if (isPeakToday) "today" else "ready"}",
                    title = "${prefix}Жим: $names",
                    subtitle = "База + ізоляція · 4 вправи",
                    targetMuscles = if (target.isNotEmpty()) target else listOf(MuscleGroup.CHEST, MuscleGroup.SHOULDERS),
                    badgeType = badge,
                    badgeText = badgeText,
                    isFromHistory = false,
                    suggestedExerciseNames = listOf("Жим штанги лежачи", "Жим гантелей на похилій лаві", "Махи гантелями в сторони", "Французький жим")
                )
            )
        }

        if (hasPull) {
            val target = listOf(MuscleGroup.BACK, MuscleGroup.BICEPS).filter { muscles.contains(it) }
            val names = target.joinToString(" + ") { it.displayNameUk }
            items.add(
                FrequentWorkoutItem(
                    id = "smart_pull_${if (isPeakToday) "today" else "ready"}",
                    title = "${prefix}Тяга: $names",
                    subtitle = "Ширина + товщина спини · 4 вправи",
                    targetMuscles = if (target.isNotEmpty()) target else listOf(MuscleGroup.BACK, MuscleGroup.BICEPS),
                    badgeType = badge,
                    badgeText = badgeText,
                    isFromHistory = false,
                    suggestedExerciseNames = listOf("Тяга верхнього блоку", "Тяга штанги в нахилі", "Підтягування", "Підйом штанги на біцепс")
                )
            )
        }

        if (hasLegs) {
            val target = listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS, MuscleGroup.GLUTES, MuscleGroup.CALVES).filter { muscles.contains(it) }
            val names = target.joinToString(" + ") { it.displayNameUk }
            items.add(
                FrequentWorkoutItem(
                    id = "smart_legs_${if (isPeakToday) "today" else "ready"}",
                    title = "${prefix}Низ тіла: $names",
                    subtitle = "Квадрицепси + задній ланцюг · 4 вправи",
                    targetMuscles = if (target.isNotEmpty()) target else listOf(MuscleGroup.QUADS, MuscleGroup.HAMSTRINGS),
                    badgeType = badge,
                    badgeText = badgeText,
                    isFromHistory = false,
                    suggestedExerciseNames = listOf("Присідання зі штангою", "Жим ногами в тренажері", "Румунська тяга", "Підйом на ікри стоячи")
                )
            )
        }

        return items
    }
}
