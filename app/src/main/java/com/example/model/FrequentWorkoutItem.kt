package com.example.model

enum class ReadinessBadgeType {
    PEAK_TODAY,     // 🔥 Стали доступні сьогодні (пік суперкомпенсації)
    FULLY_READY,    // ⚡ 100% готові до тренування
    RECOVERING      // ⏳ Частково відновлюються
}

data class FrequentWorkoutItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetMuscles: List<MuscleGroup>,
    val badgeType: ReadinessBadgeType,
    val badgeText: String,
    val isFromHistory: Boolean,
    val sourceSessionId: Long? = null,
    val suggestedExerciseNames: List<String> = emptyList()
)
