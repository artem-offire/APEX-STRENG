package com.example.model

import androidx.compose.ui.graphics.Color

enum class WorkoutRecurrence(val titleUk: String) {
    NONE("Не повторювати (одноразово)"),
    WEEKLY_4_WEEKS("Щотижня у цей день (4 тижні)"),
    EVERY_2_DAYS_2_WEEKS("Кожні 2 дні (протягом 2 тижнів)"),
    DAILY_1_WEEK("Щодня (протягом 1 тижня)")
}

enum class MuscleGroup(val displayNameUk: String, val displayNameEn: String) {
    CHEST("Грудні", "Chest"),
    BACK("Спина (Найширші)", "Back / Lats"),
    QUADS("Квадрицепси", "Quadriceps"),
    HAMSTRINGS("Задня поверхня стегна", "Hamstrings"),
    SHOULDERS("Дельти (Плечі)", "Shoulders / Delts"),
    BICEPS("Біцепс", "Biceps"),
    TRICEPS("Тріцепс", "Triceps"),
    FOREARMS("Передпліччя", "Forearms"),
    GLUTES("Сідниці", "Glutes"),
    CALVES("Литки", "Calves"),
    ABS("Прес (Коре)", "Abs / Core");

    // Standard baseline volume landmarks (weekly sets) per Dr. Mike Israetel
    val baselineMv: Int get() = when (this) {
        CHEST -> 6; BACK -> 8; QUADS -> 6; HAMSTRINGS -> 4
        SHOULDERS -> 6; BICEPS -> 4; TRICEPS -> 4; FOREARMS -> 4; GLUTES -> 4; CALVES -> 6; ABS -> 0
    }
    val baselineMev: Int get() = when (this) {
        CHEST -> 10; BACK -> 12; QUADS -> 8; HAMSTRINGS -> 6
        SHOULDERS -> 8; BICEPS -> 8; TRICEPS -> 6; FOREARMS -> 6; GLUTES -> 6; CALVES -> 8; ABS -> 4
    }
    val baselineMavMin: Int get() = when (this) {
        CHEST -> 12; BACK -> 14; QUADS -> 12; HAMSTRINGS -> 10
        SHOULDERS -> 12; BICEPS -> 10; TRICEPS -> 10; FOREARMS -> 8; GLUTES -> 8; CALVES -> 12; ABS -> 8
    }
    val baselineMavMax: Int get() = when (this) {
        CHEST -> 20; BACK -> 22; QUADS -> 18; HAMSTRINGS -> 16
        SHOULDERS -> 20; BICEPS -> 18; TRICEPS -> 16; FOREARMS -> 14; GLUTES -> 14; CALVES -> 18; ABS -> 16
    }
    val baselineMrv: Int get() = when (this) {
        CHEST -> 24; BACK -> 26; QUADS -> 22; HAMSTRINGS -> 20
        SHOULDERS -> 24; BICEPS -> 22; TRICEPS -> 20; FOREARMS -> 18; GLUTES -> 18; CALVES -> 22; ABS -> 20
    }

    companion object {
        fun fromStringOrNull(value: String): MuscleGroup? {
            val trimmed = value.trim()
            val upper = trimmed.uppercase()
            return values().find {
                it.name == upper ||
                it.displayNameUk.equals(trimmed, ignoreCase = true) ||
                (it == FOREARMS && (upper.contains("ПРЕДПЛІЧ") || upper.contains("ПЕРЕДПЛІЧ") || upper.contains("FOREARM")))
            }
        }
    }
}

enum class EquipmentType(val displayNameUk: String) {
    BARBELL("Штанга"),
    DUMBBELL("Гантелі"),
    CABLE("Блоковий тренажер"),
    MACHINE("Тренажер"),
    BODYWEIGHT("Власна вага"),
    EZ_BAR("EZ-гриф")
}

enum class MovementPattern(val displayNameUk: String) {
    HORIZONTAL_PRESS("Горизонтальний жим"),
    VERTICAL_PRESS("Вертикальний жим"),
    HORIZONTAL_PULL("Горизонтальна тяга"),
    VERTICAL_PULL("Вертикальна тяга"),
    SQUAT("Присідання"),
    HIP_HINGE("Тазово-домінантний рух"),
    ISOLATION("Ізоляція"),
    CORE("Стабілізація / Прес")
}

enum class RirLevel(
    val rir: Int,
    val rpe: Double,
    val labelUk: String,
    val descriptionUk: String,
    val colorHex: Long
) {
    RIR_0(0, 10.0, "RIR 0", "Відмова (0 повторів у запасі)", 0xFFFF3B30),
    RIR_1(1, 9.0, "RIR 1", "Важкий (1 повтор у запасі)", 0xFFFF9500),
    RIR_2(2, 8.0, "RIR 2", "Оптимум гіпертрофії (2 в запасі)", 0xFF34C759),
    RIR_3(3, 7.0, "RIR 3", "Помірний (3 в запасі)", 0xFF00C7BE),
    RIR_4_PLUS(4, 6.0, "RIR 4+", "Легкий / Розминка (4+ в запасі)", 0xFF8E8E93);

    val composeColor: Color get() = Color(colorHex)

    companion object {
        fun fromRir(rir: Int): RirLevel = when {
            rir <= 0 -> RIR_0
            rir == 1 -> RIR_1
            rir == 2 -> RIR_2
            rir == 3 -> RIR_3
            else -> RIR_4_PLUS
        }
    }
}

enum class ExperienceLevel(val displayNameUk: String, val multiplier: Float) {
    BEGINNER("Початківець (<1 року)", 0.75f),
    INTERMEDIATE("Середній (1–3 роки)", 1.0f),
    ADVANCED("Просунутий (3+ роки)", 1.2f)
}

enum class SleepFactor(val displayNameUk: String, val factor: Float) {
    EXCELLENT("≥ 8 годин якісного сну", 1.05f),
    AVERAGE("6–7 годин сну", 1.0f),
    POOR("< 6 годин (недосипання)", 0.82f)
}

enum class NutritionFactor(val displayNameUk: String, val factor: Float) {
    SURPLUS("Профіцит калорій (масонабір)", 1.1f),
    MAINTENANCE("Підтримка ваги", 1.0f),
    DEFICIT("Дефіцит калорій (сушка / схуднення)", 0.85f)
}

enum class StressFactor(val displayNameUk: String, val factor: Float) {
    LOW("Низький стрес", 1.05f),
    MODERATE("Помірний стрес", 1.0f),
    HIGH("Високий стрес / перевтома", 0.85f)
}

enum class MesocyclePhase(val titleUk: String, val subtitleUk: String, val targetRir: String, val isDeload: Boolean) {
    INTRO("Ввідний тиждень", "Адаптація зв'язок та ЦНС", "RIR 3–4", false),
    ACCUMULATION("Фаза накопичення", "Прогресивне зростання обсягу", "RIR 2–3", false),
    OVERREACH("Пікове навантаження", "Максимальний стимул (Overreach)", "RIR 1–2", false),
    PEAK_INTENSITY("Максимальна інтенсивність", "Близько до відмови перед делоадом", "RIR 0–1", false),
    DELOAD("Делоад (Розвантаження)", "Зниження обсягу на 50%, відновлення ЦНС", "RIR 4–5", true)
}

data class VolumeLandmarkData(
    val muscleGroup: MuscleGroup,
    val mv: Int,
    val mev: Int,
    val mavMin: Int,
    val mavMax: Int,
    val mrv: Int,
    val currentWeeklySets: Int = 0,
    val isPriority: Boolean = false
)
