package com.example.analysis

import com.example.data.InternalScheduleEntity
import com.example.data.WeeklySummaryEntity
import com.example.data.WorkoutSessionEntity
import java.util.Calendar

enum class StartClassification(val titleUk: String) {
    USUAL("Звичний час"),
    LATE("Пізніше за звичне"),
    EARLY("Раніше за звичне"),
    UNUSUAL_DAY("Поза графіком"),
    LATE_NIGHT("Пізній вечір"),
    AFTER_BREAK("Після перерви"),
    UNKNOWN("Базовий старт")
}

data class StartContext(
    val currentTimeMs: Long,
    val currentWeekday: Int, // Calendar.SUNDAY..Calendar.SATURDAY
    val currentMinutesOfDay: Int,
    val classification: StartClassification,
    val streak: Int,
    val daysSinceLastWorkout: Int,
    val recentTrainedMuscles48h: List<String>,
    val currentWorkoutMuscles: List<String>,
    val acuteChronicRatio: Double,
    val isScheduleFormed: Boolean
)

data class StartReactionMessage(
    val ruleId: String,
    val title: String,
    val body: String,
    val iconEmoji: String,
    val isWarning: Boolean = false,
    val suggestedAction: String? = null
)

interface StartRule {
    val ruleId: String
    fun matches(ctx: StartContext): Boolean
    fun message(ctx: StartContext): StartReactionMessage
}

class UsualTimeRule : StartRule {
    override val ruleId = "rule_usual_time"
    override fun matches(ctx: StartContext): Boolean =
        ctx.isScheduleFormed && ctx.classification == StartClassification.USUAL

    override fun message(ctx: StartContext): StartReactionMessage =
        StartReactionMessage(
            ruleId = ruleId,
            title = "Чудовий темп!",
            body = if (ctx.streak > 1) {
                "Тренування розпочато у звичний для вас час. Ваша активна серія: ${ctx.streak} тренувань поспіль без пропусків!"
            } else {
                "Тренування розпочато у звичний для вас час. Продуктивного сета та високої концентрації!"
            },
            iconEmoji = "🔥"
        )
}

class LateStartRule : StartRule {
    override val ruleId = "rule_late_start"
    override fun matches(ctx: StartContext): Boolean =
        ctx.isScheduleFormed && ctx.classification == StartClassification.LATE

    override fun message(ctx: StartContext): StartReactionMessage =
        StartReactionMessage(
            ruleId = ruleId,
            title = "Початок пізніше звичного",
            body = "Сьогодні ти почав пізніше, ніж зазвичай. Якщо час обмежений, можна обрати компактнішу версію тренування або трохи скоротити паузи між підходами.",
            iconEmoji = "⏱️",
            suggestedAction = "Компактна версія"
        )
}

class LateNightRule : StartRule {
    override val ruleId = "rule_late_night"
    override fun matches(ctx: StartContext): Boolean =
        ctx.classification == StartClassification.LATE_NIGHT

    override fun message(ctx: StartContext): StartReactionMessage =
        StartReactionMessage(
            ruleId = ruleId,
            title = "Пізнє тренування",
            body = "Зараз пізній вечір. Радимо зосередитися на помірних вагах та акуратній техніці, щоб не збуджувати нервову систему перед сном.",
            iconEmoji = "🌙",
            isWarning = true,
            suggestedAction = "Помірні ваги"
        )
}

class AfterBreakRule : StartRule {
    override val ruleId = "rule_after_break"
    override fun matches(ctx: StartContext): Boolean =
        ctx.classification == StartClassification.AFTER_BREAK

    override fun message(ctx: StartContext): StartReactionMessage =
        StartReactionMessage(
            ruleId = ruleId,
            title = "Повернення після перерви",
            body = "Перше тренування після тривалої паузи: розпочніть з полегшеної інтенсивності та запасу у підходах (RIR 2–3) для швидкого відновлення м'язів.",
            iconEmoji = "🌱",
            suggestedAction = "Полегшена програма"
        )
}

fun formatMuscleGroupUk(muscle: String): String = when (muscle.trim().uppercase()) {
    "CHEST" -> "Грудні"
    "BACK" -> "Спина"
    "QUADS" -> "Квадрицепси"
    "HAMSTRINGS" -> "Задня поверхня стегна"
    "SHOULDERS" -> "Дельти (Плечі)"
    "BICEPS" -> "Біцепс"
    "TRICEPS" -> "Тріцепс"
    "FOREARMS" -> "Передпліччя"
    "GLUTES" -> "Сідниці"
    "CALVES" -> "Литки"
    "ABS" -> "Прес"
    "LEGS" -> "Ноги"
    else -> muscle
}

class MuscleRecoveryWarningRule : StartRule {
    override val ruleId = "rule_muscle_recovery_48h"
    override fun matches(ctx: StartContext): Boolean {
        if (ctx.currentWorkoutMuscles.isEmpty() || ctx.recentTrainedMuscles48h.isEmpty()) return false
        val overlap = ctx.currentWorkoutMuscles.intersect(ctx.recentTrainedMuscles48h.toSet())
        return overlap.isNotEmpty()
    }

    override fun message(ctx: StartContext): StartReactionMessage {
        val overlap = ctx.currentWorkoutMuscles.intersect(ctx.recentTrainedMuscles48h.toSet())
        val names = overlap.map { formatMuscleGroupUk(it) }.joinToString(", ")
        return StartReactionMessage(
            ruleId = ruleId,
            title = "М'язове відновлення",
            body = "Увага: групи м'язів ($names) уже отримували навантаження менше 48 годин тому. Враховуйте накопичену втому та не працюйте в повну відмову.",
            iconEmoji = "⚠️",
            isWarning = true
        )
    }
}

class WorkloadSpikeRule : StartRule {
    override val ruleId = "rule_workload_spike"
    override fun matches(ctx: StartContext): Boolean =
        ctx.acuteChronicRatio > 1.4

    override fun message(ctx: StartContext): StartReactionMessage {
        val percent = ((ctx.acuteChronicRatio - 1.0) * 100).toInt()
        return StartReactionMessage(
            ruleId = ruleId,
            title = "Різке зростання навантаження",
            body = "Тижневий обсяг перевищує середній за місяць на $percent%. Контролюйте сон та харчування, щоб уникнути перетренованості.",
            iconEmoji = "📈",
            isWarning = true
        )
    }
}

object WorkoutAnalysisEngine {

    val startRules: List<StartRule> = listOf(
        LateNightRule(),
        AfterBreakRule(),
        LateStartRule(),
        UsualTimeRule(),
        MuscleRecoveryWarningRule(),
        WorkloadSpikeRule()
    )

    fun isScheduleFormed(completedSessions: List<WorkoutSessionEntity>, nowMs: Long = System.currentTimeMillis()): Boolean {
        if (completedSessions.size < 4) return false
        val earliest = completedSessions.minOfOrNull { it.dateTimestamp } ?: return false
        val daysSpan = (nowMs - earliest) / (86_400_000L)
        return daysSpan >= 14
    }

    fun recomputeInternalSchedule(
        completedSessions: List<WorkoutSessionEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): List<InternalScheduleEntity> {
        val fourWeeksAgo = nowMs - (28L * 86_400_000L)
        val recentSessions = completedSessions.filter { it.isCompleted && it.dateTimestamp >= fourWeeksAgo }

        if (recentSessions.isEmpty()) return emptyList()

        val cal = Calendar.getInstance()
        val schedules = mutableListOf<InternalScheduleEntity>()

        // Analyze each weekday: Calendar.SUNDAY (1) .. Calendar.SATURDAY (7)
        for (weekday in 1..7) {
            val sessionsOnDay = recentSessions.filter {
                cal.timeInMillis = it.dateTimestamp
                cal.get(Calendar.DAY_OF_WEEK) == weekday
            }

            if (sessionsOnDay.isEmpty()) continue

            // Determine how many distinct weeks had at least one session on this weekday
            val distinctWeeks = sessionsOnDay.map {
                cal.timeInMillis = it.dateTimestamp
                val weekOfYear = cal.get(Calendar.WEEK_OF_YEAR)
                val year = cal.get(Calendar.YEAR)
                "$year-$weekOfYear"
            }.distinct().size

            // Rule: weekday where person trained >= 3 of the last 4 weeks
            if (distinctWeeks >= 3) {
                val confidence = if (distinctWeeks >= 4) "HIGH" else "MEDIUM"

                val startMinutesList = sessionsOnDay.map {
                    cal.timeInMillis = it.dateTimestamp
                    cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                }.sorted()

                val durationsList = sessionsOnDay.map {
                    val durMin = (it.durationSeconds / 60).toInt()
                    if (durMin > 0) durMin else 45
                }.sorted()

                val expectedStart = median(startMinutesList)
                val expectedDuration = median(durationsList)

                // Spread/tolerance: calculate average absolute deviation from median
                val avgDeviation = if (startMinutesList.isNotEmpty()) {
                    startMinutesList.map { Math.abs(it - expectedStart) }.average().toInt()
                } else 35

                // Clamped tolerance between 20 min and 60 min
                val toleranceMin = avgDeviation.coerceIn(20, 60)

                schedules.add(
                    InternalScheduleEntity(
                        weekday = weekday,
                        expectedStartMinutes = expectedStart,
                        toleranceMin = toleranceMin,
                        expectedDurationMinutes = expectedDuration,
                        confidence = confidence,
                        updatedAt = nowMs
                    )
                )
            }
        }

        return schedules
    }

    fun classifyStart(
        nowMs: Long,
        completedSessions: List<WorkoutSessionEntity>,
        internalSchedules: List<InternalScheduleEntity>
    ): StartClassification {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinutes = hour * 60 + cal.get(Calendar.MINUTE)
        val currentWeekday = cal.get(Calendar.DAY_OF_WEEK)

        // Rule: Late night (> 22:00 or < 05:00)
        if (hour >= 22 || hour < 5) {
            return StartClassification.LATE_NIGHT
        }

        // Rule: Break >= 7 days
        val lastCompleted = completedSessions.filter { it.isCompleted }.maxByOrNull { it.dateTimestamp }
        if (lastCompleted != null) {
            val daysSince = (nowMs - lastCompleted.dateTimestamp) / 86_400_000L
            if (daysSince >= 7) {
                return StartClassification.AFTER_BREAK
            }
        }

        val scheduleFormed = isScheduleFormed(completedSessions, nowMs)
        if (!scheduleFormed) {
            return StartClassification.UNKNOWN
        }

        val daySchedule = internalSchedules.find { it.weekday == currentWeekday }
        if (daySchedule == null || daySchedule.confidence == "LOW") {
            return StartClassification.UNUSUAL_DAY
        }

        val expected = daySchedule.expectedStartMinutes
        val tol = daySchedule.toleranceMin

        return when {
            currentMinutes > expected + tol -> StartClassification.LATE
            currentMinutes < expected - tol -> StartClassification.EARLY
            else -> StartClassification.USUAL
        }
    }

    fun evaluateStartRules(ctx: StartContext): List<StartReactionMessage> {
        val triggered = mutableListOf<StartReactionMessage>()
        for (rule in startRules) {
            if (rule.matches(ctx)) {
                triggered.add(rule.message(ctx))
            }
        }
        return triggered
    }

    fun calculateRegularityPercent(
        completedSessions: List<WorkoutSessionEntity>,
        internalSchedules: List<InternalScheduleEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): Int {
        if (internalSchedules.isEmpty()) return 0
        val oneMonthAgo = nowMs - (30L * 86_400_000L)
        val cal = Calendar.getInstance()

        // Count how many scheduled days occurred in the past 30 days
        var totalScheduledDays = 0
        var attendedScheduledDays = 0

        val scheduledWeekdays = internalSchedules.map { it.weekday }.toSet()
        if (scheduledWeekdays.isEmpty()) return 0

        for (dayOffset in 0..29) {
            val dayMs = nowMs - (dayOffset * 86_400_000L)
            cal.timeInMillis = dayMs
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
            if (scheduledWeekdays.contains(dayOfWeek)) {
                totalScheduledDays++
                val hasSession = completedSessions.any {
                    it.isCompleted && Math.abs(it.dateTimestamp - dayMs) < 43_200_000L
                }
                if (hasSession) attendedScheduledDays++
            }
        }

        return if (totalScheduledDays > 0) {
            ((attendedScheduledDays.toDouble() / totalScheduledDays) * 100).toInt().coerceIn(0, 100)
        } else 0
    }

    fun calculateScheduleStreak(
        completedSessions: List<WorkoutSessionEntity>,
        internalSchedules: List<InternalScheduleEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): Int {
        val scheduledWeekdays = internalSchedules.map { it.weekday }.toSet()
        if (scheduledWeekdays.isEmpty()) {
            return completedSessions.count { it.isCompleted }
        }

        val cal = Calendar.getInstance()
        var streak = 0
        for (dayOffset in 0..60) {
            val dayMs = nowMs - (dayOffset * 86_400_000L)
            cal.timeInMillis = dayMs
            val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)

            if (scheduledWeekdays.contains(dayOfWeek)) {
                val hasSession = completedSessions.any {
                    it.isCompleted && Math.abs(it.dateTimestamp - dayMs) < 43_200_000L
                }
                if (hasSession) {
                    streak++
                } else if (dayOffset > 0) { // Don't break if today hasn't finished yet
                    break
                }
            }
        }
        return streak
    }

    fun calculateStartTimeStability(
        completedSessions: List<WorkoutSessionEntity>,
        internalSchedules: List<InternalScheduleEntity>
    ): Pair<Int, String> {
        val cal = Calendar.getInstance()
        val deviations = mutableListOf<Int>()

        for (session in completedSessions.filter { it.isCompleted }.take(15)) {
            cal.timeInMillis = session.dateTimestamp
            val weekday = cal.get(Calendar.DAY_OF_WEEK)
            val schedule = internalSchedules.find { it.weekday == weekday }
            if (schedule != null) {
                val startMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
                val diff = Math.abs(startMin - schedule.expectedStartMinutes)
                deviations.add(diff)
            }
        }

        if (deviations.isEmpty()) return 85 to "Формується"

        val avgDev = deviations.average().toInt()
        val stabilityPercent = (100 - (avgDev * 1.5).toInt()).coerceIn(40, 98)
        val label = when {
            avgDev <= 25 -> "Висока (±${avgDev} хв)"
            avgDev <= 50 -> "Помірна (±${avgDev} хв)"
            else -> "Вільний графік (±${avgDev} хв)"
        }
        return stabilityPercent to label
    }

    fun calculateAcuteChronicRatio(
        completedSessions: List<WorkoutSessionEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): Double {
        val sevenDaysAgo = nowMs - (7L * 86_400_000L)
        val twentyEightDaysAgo = nowMs - (28L * 86_400_000L)

        val acuteLoad = completedSessions.filter { it.isCompleted && it.dateTimestamp >= sevenDaysAgo }
            .sumOf { it.totalTonnageKg }

        val chronicLoadTotal = completedSessions.filter { it.isCompleted && it.dateTimestamp >= twentyEightDaysAgo }
            .sumOf { it.totalTonnageKg }

        val chronicWeeklyAvg = chronicLoadTotal / 4.0
        return if (chronicWeeklyAvg > 0) acuteLoad / chronicWeeklyAvg else 1.0
    }

    fun calculateBestWorkoutTime(
        completedSessions: List<WorkoutSessionEntity>
    ): String {
        val cal = Calendar.getInstance()
        val morningSessions = mutableListOf<WorkoutSessionEntity>()
        val afternoonSessions = mutableListOf<WorkoutSessionEntity>()
        val eveningSessions = mutableListOf<WorkoutSessionEntity>()

        for (s in completedSessions.filter { it.isCompleted }) {
            cal.timeInMillis = s.dateTimestamp
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            when {
                hour < 12 -> morningSessions.add(s)
                hour in 12..17 -> afternoonSessions.add(s)
                else -> eveningSessions.add(s)
            }
        }

        val windows = listOf(
            Triple("Ранкові", morningSessions.size, morningSessions),
            Triple("Денні", afternoonSessions.size, afternoonSessions),
            Triple("Вечірні", eveningSessions.size, eveningSessions)
        ).filter { it.second >= 5 }

        // Condition from Stage 9: formed only if >= 5 workouts in windows
        if (windows.size < 2) {
            return "Збираємо дані для визначення найкращого часу тренувань (потрібно від 5 сесій у різних частинах дня)."
        }

        // Compare average tonnage and well-being
        val best = windows.maxByOrNull { triple ->
            val avgTonnage = triple.third.map { it.totalTonnageKg }.average()
            val avgRating = triple.third.mapNotNull { it.wellBeingRating }.average().let { if (it.isNaN()) 3.5 else it }
            avgTonnage * (avgRating / 3.0)
        }

        return when (best?.first) {
            "Вечірні" -> "Вечірні тренування в тебе найвдаліші: стабільно вищий обсяг навантаження та краще самопочуття."
            "Ранкові" -> "Ранкові тренування показують найкращу продуктивність та свіжість сил."
            "Денні" -> "Денні тренування демонструють найвищий тоннус та ефективність відновлення."
            else -> "Рівномірна продуктивність у різний час доби."
        }
    }

    fun generateWeeklySummary(
        completedSessions: List<WorkoutSessionEntity>,
        nowMs: Long = System.currentTimeMillis()
    ): WeeklySummaryEntity {
        val sevenDaysAgo = nowMs - (7L * 86_400_000L)
        val fourteenDaysAgo = nowMs - (14L * 86_400_000L)

        val thisWeekSessions = completedSessions.filter { it.isCompleted && it.dateTimestamp >= sevenDaysAgo }
        val prevWeekSessions = completedSessions.filter {
            it.isCompleted && it.dateTimestamp in fourteenDaysAgo until sevenDaysAgo
        }

        val thisCount = thisWeekSessions.size
        val prevCount = prevWeekSessions.size
        val totalVolume = thisWeekSessions.sumOf { it.totalTonnageKg }
        val prevVolume = prevWeekSessions.sumOf { it.totalTonnageKg }
        val avgDurationMin = if (thisCount > 0) {
            (thisWeekSessions.sumOf { it.durationSeconds } / thisCount / 60).toInt()
        } else 0

        val adviceBuilder = StringBuilder()
        when {
            thisCount > prevCount && prevCount > 0 ->
                adviceBuilder.append("Цього тижня на ${thisCount - prevCount} тренування більше, ніж минулого! Чудовий ріст дисципліни. ")
            thisCount < prevCount && thisCount > 0 ->
                adviceBuilder.append("Цього тижня на ${prevCount - thisCount} тренування менше. Сплануйте відпочинок і повертайтесь у стрій. ")
            thisCount > 0 ->
                adviceBuilder.append("Виконано $thisCount якісних тренувань за останні 7 днів. ")
            else ->
                adviceBuilder.append("За останні 7 днів тренувань не зафіксовано. Час для активного старту! ")
        }

        if (totalVolume > prevVolume && prevVolume > 0) {
            val diff = ((totalVolume - prevVolume) / prevVolume * 100).toInt()
            adviceBuilder.append("Загальний тоннаж зріс на $diff%. ")
        }

        val bestTimeTip = calculateBestWorkoutTime(completedSessions)
        if (!bestTimeTip.startsWith("Збираємо дані")) {
            adviceBuilder.append(bestTimeTip)
        }

        return WeeklySummaryEntity(
            weekStartTimestamp = sevenDaysAgo,
            workoutCount = thisCount,
            totalVolumeKg = totalVolume,
            avgDurationMinutes = avgDurationMin,
            previousWeekWorkoutCount = prevCount,
            summaryAdvice = adviceBuilder.toString().trim(),
            createdAt = nowMs
        )
    }

    fun checkReminderEligibility(
        nowMs: Long,
        internalSchedules: List<InternalScheduleEntity>,
        completedSessions: List<WorkoutSessionEntity>,
        recentRemindersToday: Int,
        consecutiveIgnoredCount: Int
    ): String? {
        val cal = Calendar.getInstance().apply { timeInMillis = nowMs }
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val currentMinutes = hour * 60 + cal.get(Calendar.MINUTE)
        val weekday = cal.get(Calendar.DAY_OF_WEEK)

        // Stage 7: Quiet hours at night (23:00 to 08:00)
        if (hour >= 23 || hour < 8) return null

        // Max 1 reminder per day
        if (recentRemindersToday >= 1) return null

        // If user ignored 3 times in a row for this weekday -> disable
        if (consecutiveIgnoredCount >= 3) return null

        val schedule = internalSchedules.find { it.weekday == weekday && it.confidence != "LOW" } ?: return null

        // Check if expected_start + tolerance_min has passed
        if (currentMinutes >= schedule.expectedStartMinutes + schedule.toleranceMin) {
            // Check if user already trained today
            val todayStart = cal.apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val alreadyTrained = completedSessions.any { it.dateTimestamp >= todayStart }
            if (!alreadyTrained) {
                // Neutral text, never mentioning the internal hours
                return "Як щодо тренування сьогодні?"
            }
        }

        return null
    }

    private fun median(list: List<Int>): Int {
        if (list.isEmpty()) return 0
        val sorted = list.sorted()
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) / 2
    }
}
