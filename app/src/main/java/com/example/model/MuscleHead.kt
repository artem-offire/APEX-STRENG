package com.example.model

/**
 * Пучки (головки) мʼязів для «Продвинутого режиму».
 * У звичайному режимі мʼязи обираються 11 загальними групами [MuscleGroup],
 * у продвинутому кожна група ділиться на окремі пучки.
 */
enum class MuscleHead(val group: MuscleGroup, val displayNameUk: String) {
    CHEST_UPPER(MuscleGroup.CHEST, "Верхня"),
    CHEST_MIDDLE(MuscleGroup.CHEST, "Середня"),
    CHEST_LOWER(MuscleGroup.CHEST, "Нижня"),

    BACK_LATS(MuscleGroup.BACK, "Найширші"),
    BACK_TRAPS(MuscleGroup.BACK, "Трапеція"),
    BACK_RHOMBOIDS(MuscleGroup.BACK, "Ромбоподібні"),
    BACK_ERECTORS(MuscleGroup.BACK, "Розгиначі"),

    QUADS_RECTUS(MuscleGroup.QUADS, "Прямий"),
    QUADS_LATERAL(MuscleGroup.QUADS, "Латеральний"),
    QUADS_MEDIAL(MuscleGroup.QUADS, "Медіальний"),

    DELTS_FRONT(MuscleGroup.SHOULDERS, "Передня"),
    DELTS_SIDE(MuscleGroup.SHOULDERS, "Середня"),
    DELTS_REAR(MuscleGroup.SHOULDERS, "Задня"),

    BICEPS_LONG(MuscleGroup.BICEPS, "Довга голівка"),
    BICEPS_SHORT(MuscleGroup.BICEPS, "Коротка голівка"),
    BRACHIALIS(MuscleGroup.BICEPS, "Брахіаліс"),

    TRICEPS_LONG(MuscleGroup.TRICEPS, "Довга"),
    TRICEPS_LATERAL(MuscleGroup.TRICEPS, "Латеральна"),
    TRICEPS_MEDIAL(MuscleGroup.TRICEPS, "Медіальна"),

    GLUTE_MAX(MuscleGroup.GLUTES, "Великий"),
    GLUTE_MED(MuscleGroup.GLUTES, "Середній"),

    CALVES_GASTROC(MuscleGroup.CALVES, "Литковий"),
    CALVES_SOLEUS(MuscleGroup.CALVES, "Камбалоподібний"),

    ABS_RECTUS(MuscleGroup.ABS, "Прямий"),
    ABS_OBLIQUES(MuscleGroup.ABS, "Косі");

    companion object {
        fun headsFor(group: MuscleGroup): List<MuscleHead> = values().filter { it.group == group }

        /** Скільки мʼязів доступно для вибору в продвинутому режимі (пучки + групи без поділу). */
        val advancedSelectableCount: Int
            get() {
                val split = values().map { it.group }.toSet()
                val unsplit = MuscleGroup.values().count { it != MuscleGroup.OTHER && it !in split }
                return values().size + unsplit
            }

        /**
         * Визначає, які пучки навантажує вправа, за її назвою та основною групою.
         * Для невідомих вправ повертає всі пучки групи.
         */
        fun forExercise(name: String, group: MuscleGroup?): List<MuscleHead> {
            if (group == null) return emptyList()
            val n = name.lowercase()
            fun has(vararg keys: String) = keys.any { n.contains(it) }
            val heads = when (group) {
                MuscleGroup.CHEST -> when {
                    has("похил", "нахил", "incline") -> listOf(CHEST_UPPER)
                    has("брус", "dips", "знижен") -> listOf(CHEST_LOWER)
                    else -> listOf(CHEST_MIDDLE)
                }
                MuscleGroup.BACK -> when {
                    has("шраг", "трапец") -> listOf(BACK_TRAPS)
                    has("гіперекст", "станов") -> listOf(BACK_ERECTORS)
                    has("підтяг", "верхнього блок", "вертикал") -> listOf(BACK_LATS)
                    has("в нахилі", "горизонтал", "до живота", "до пояса") -> listOf(BACK_LATS, BACK_RHOMBOIDS)
                    else -> listOf(BACK_LATS)
                }
                MuscleGroup.QUADS -> when {
                    has("розгинання ніг") -> listOf(QUADS_RECTUS)
                    else -> listOf(QUADS_RECTUS, QUADS_LATERAL, QUADS_MEDIAL)
                }
                MuscleGroup.SHOULDERS -> when {
                    has("задн", "face pull", "до обличчя") -> listOf(DELTS_REAR)
                    has("махи", "сторон", "розведен") -> listOf(DELTS_SIDE)
                    has("жим") -> listOf(DELTS_FRONT, DELTS_SIDE)
                    else -> listOf(DELTS_FRONT)
                }
                MuscleGroup.BICEPS -> when {
                    has("молотк", "хаммер", "зворотним хватом") -> listOf(BRACHIALIS)
                    has("похил", "incline") -> listOf(BICEPS_LONG)
                    has("скотт", "концентр") -> listOf(BICEPS_SHORT)
                    else -> listOf(BICEPS_LONG, BICEPS_SHORT)
                }
                MuscleGroup.TRICEPS -> when {
                    has("француз", "над головою") -> listOf(TRICEPS_LONG)
                    has("блоц", "канат") -> listOf(TRICEPS_LATERAL, TRICEPS_MEDIAL)
                    else -> listOf(TRICEPS_LONG, TRICEPS_LATERAL, TRICEPS_MEDIAL)
                }
                MuscleGroup.GLUTES -> when {
                    has("відведен", "абдук") -> listOf(GLUTE_MED)
                    else -> listOf(GLUTE_MAX)
                }
                MuscleGroup.CALVES -> when {
                    has("сидяч") -> listOf(CALVES_SOLEUS)
                    else -> listOf(CALVES_GASTROC)
                }
                MuscleGroup.ABS -> when {
                    has("кос", "скручування з поворотом", "бічн") -> listOf(ABS_OBLIQUES)
                    else -> listOf(ABS_RECTUS)
                }
                else -> emptyList()
            }
            return heads
        }
    }
}
