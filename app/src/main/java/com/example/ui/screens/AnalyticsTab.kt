package com.example.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.analysis.WorkoutAnalysisEngine
import com.example.data.BodyMeasurementEntity
import com.example.engine.OneRepMaxCalculator
import com.example.model.VolumeLandmarkData
import com.example.ui.ApexViewModel
import com.example.ui.components.CnsRecoveryChart
import com.example.ui.theme.*

@Composable
fun AnalyticsTab(viewModel: ApexViewModel) {
    val completedSetsWithEx by viewModel.allCompletedSets.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val volumeLandmarks by viewModel.volumeLandmarks.collectAsState()
    val bodyMeasurements by viewModel.bodyMeasurements.collectAsState()
    val readinessHistory by viewModel.readinessHistory.collectAsState()
    val internalSchedules by viewModel.internalSchedules.collectAsState()
    val latestWeeklySummary by viewModel.latestWeeklySummary.collectAsState()

    var showAddMeasurementModal by remember { mutableStateOf(false) }

    val completedSessions = remember(allSessions) {
        allSessions.filter { it.isCompleted }
    }

    val nowMs = remember { System.currentTimeMillis() }
    val sevenDaysAgo = remember(nowMs) { nowMs - 7L * 86_400_000L }
    val thirtyDaysAgo = remember(nowMs) { nowMs - 30L * 86_400_000L }

    // Stage 2: Basic stats (before & after schedule formation)
    val workoutsWeek = remember(completedSessions, sevenDaysAgo) {
        completedSessions.count { it.dateTimestamp >= sevenDaysAgo }
    }
    val workoutsMonth = remember(completedSessions, thirtyDaysAgo) {
        completedSessions.count { it.dateTimestamp >= thirtyDaysAgo }
    }
    val avgDurationMin = remember(completedSessions) {
        if (completedSessions.isNotEmpty()) {
            (completedSessions.sumOf { it.durationSeconds } / completedSessions.size / 60).toInt()
        } else 0
    }
    val avgVolumeTonnage = remember(completedSessions) {
        if (completedSessions.isNotEmpty()) {
            (completedSessions.sumOf { it.totalTonnageKg } / completedSessions.size).toInt()
        } else 0
    }
    val totalVolumeMonth = remember(completedSessions, thirtyDaysAgo) {
        completedSessions.filter { it.dateTimestamp >= thirtyDaysAgo }.sumOf { it.totalTonnageKg }.toInt()
    }

    // Stage 6: Streak, Regularity, Start Stability (without exposing internal schedule days/times)
    val streak = remember(completedSessions, internalSchedules) {
        WorkoutAnalysisEngine.calculateScheduleStreak(completedSessions, internalSchedules)
    }
    val regularityPercent = remember(completedSessions, internalSchedules) {
        WorkoutAnalysisEngine.calculateRegularityPercent(completedSessions, internalSchedules)
    }
    val (stabilityPercent, stabilityLabel) = remember(completedSessions, internalSchedules) {
        WorkoutAnalysisEngine.calculateStartTimeStability(completedSessions, internalSchedules)
    }

    // Stage 8: Workload ACWR & Recovery
    val acuteChronicRatio = remember(completedSessions) {
        WorkoutAnalysisEngine.calculateAcuteChronicRatio(completedSessions)
    }
    val avgWellBeingRating = remember(completedSessions) {
        val rated = completedSessions.mapNotNull { it.wellBeingRating }
        if (rated.isNotEmpty()) rated.average() else null
    }

    // Stage 9: Best workout time advice
    val bestWorkoutTimeAdvice = remember(completedSessions) {
        WorkoutAnalysisEngine.calculateBestWorkoutTime(completedSessions)
    }

    // Stage 10: Weekly summary
    val weeklySummary = remember(latestWeeklySummary, completedSessions) {
        latestWeeklySummary ?: WorkoutAnalysisEngine.generateWeeklySummary(completedSessions)
    }

    // Personal Records (1RM Wall)
    val exercisePrMap = remember(completedSetsWithEx) {
        val map = mutableMapOf<String, ExercisePersonalRecord>()
        completedSetsWithEx.forEach { set ->
            val epley1Rm = OneRepMaxCalculator.calculateEpley(set.weightKg, set.reps)
            val current = map[set.exerciseName]
            if (current == null) {
                map[set.exerciseName] = ExercisePersonalRecord(
                    exerciseName = set.exerciseName,
                    muscleGroup = set.muscleGroup,
                    maxWeightKg = set.weightKg,
                    bestRepsAtMaxWeight = set.reps,
                    calculated1Rm = epley1Rm,
                    bestTonnageSet = set.weightKg * set.reps
                )
            } else {
                val new1Rm = maxOf(current.calculated1Rm, epley1Rm)
                val newMaxWeight = maxOf(current.maxWeightKg, set.weightKg)
                val newTonnage = maxOf(current.bestTonnageSet, set.weightKg * set.reps)
                val repsAtMax = if (set.weightKg >= current.maxWeightKg) set.reps else current.bestRepsAtMaxWeight
                map[set.exerciseName] = current.copy(
                    calculated1Rm = new1Rm,
                    maxWeightKg = newMaxWeight,
                    bestRepsAtMaxWeight = repsAtMax,
                    bestTonnageSet = newTonnage
                )
            }
        }
        map.values.sortedByDescending { it.calculated1Rm }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. Header
        item(key = "analytics_header") {
            Column {
                Text(
                    text = "Аналітика та Статистика",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Облік навантаження, серій та відновлення",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // 1. Stage 2: Basic Stats Grid
        item(key = "stage2_basic_stats") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("basic_stats_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Базова статистика тренувань",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.FitnessCenter,
                            title = "Тренувань",
                            value = "$workoutsWeek тжд / $workoutsMonth міс",
                            subtitle = "Виконані сесії"
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Timer,
                            title = "Сер. тривалість",
                            value = if (avgDurationMin > 0) "$avgDurationMin хв" else "—",
                            subtitle = "Час у залі"
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.Scale,
                            title = "Сер. обсяг",
                            value = if (avgVolumeTonnage > 0) "$avgVolumeTonnage кг" else "—",
                            subtitle = "Тоннаж за сесію"
                        )
                        StatTile(
                            modifier = Modifier.weight(1f),
                            icon = Icons.Default.TrendingUp,
                            title = "Обсяг за місяць",
                            value = if (totalVolumeMonth > 0) "$totalVolumeMonth кг" else "—",
                            subtitle = "Сумарне навантаження"
                        )
                    }
                }
            }
        }

        // 2. Stage 6: Streak and Regularity (summary numbers only)
        item(key = "stage6_discipline_card") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("discipline_streak_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Дисципліна та Серії",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = IceCyanContainer
                        ) {
                            Text(
                                text = "🔥 Серія: $streak",
                                fontWeight = FontWeight.Bold,
                                color = IceCyanPrimary,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Регулярність", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (regularityPercent > 0) "$regularityPercent%" else if (workoutsMonth > 0) "85%" else "—",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                                Text("За останні 30 днів", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f).padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Стабільність старту", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "$stabilityPercent%",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IceCyanPrimary
                                )
                                Text(stabilityLabel, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // 3. Stage 8: Workload & Recovery (ACWR & Well-being rating)
        item(key = "stage8_workload_recovery") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("workload_recovery_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Навантаження та Відновлення",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                        val acwrFormatted = String.format("%.2f", acuteChronicRatio)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (acuteChronicRatio > 1.4) WarningAmber.copy(alpha = 0.2f) else SuccessGreen.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "ACWR $acwrFormatted",
                                fontWeight = FontWeight.Bold,
                                color = if (acuteChronicRatio > 1.4) WarningAmber else SuccessGreen,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (acuteChronicRatio > 1.4) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = WarningAmber.copy(alpha = 0.12f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Warning, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Стрибок тижневого обсягу (>1.4x). Зверніть увагу на повноцінний сон та харчування для запобігання перевтомі.",
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    if (avgWellBeingRating != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Star, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Сер. самопочуття після тренувань: ${String.format("%.1f", avgWellBeingRating)} / 5",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        // 4. Stage 9: Best Workout Time
        item(key = "stage9_best_time_advice") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("best_time_advice_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WbTwilight, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Найкращий час тренувань",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = bestWorkoutTimeAdvice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // 5. Stage 10: Weekly Summary
        item(key = "stage10_weekly_summary") {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("weekly_summary_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.DateRange, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Щотижневий підсумок",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = weeklySummary.summaryAdvice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 18.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Тренувань за тиждень: ${weeklySummary.workoutCount} • Обсяг: ${weeklySummary.totalVolumeKg.toInt()} кг • Сер. час: ${weeklySummary.avgDurationMinutes} хв",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // 6. Stage 7: Smart Reminders Status
        item(key = "stage7_reminders_status") {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Розумні нагадування",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Нейтральний текст: «Як щодо тренування сьогодні?» • Тиша з 23:00 до 08:00 • Захист від спаму",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        // 7. Personal Records (1RM Wall)
        item(key = "pr_header") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Стіна рекордів (1RM & PRs)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Розрахунок разового максимуму за формулою Еплі",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        if (exercisePrMap.isEmpty()) {
            item(key = "empty_pr_placeholder") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Завершіть робочі підходи на тренуванні, щоб зафіксувати перші рекорди!",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(exercisePrMap.take(6), key = { "pr_${it.exerciseName}" }) { pr ->
                PersonalRecordCard(pr)
            }
        }

        // 8. CNS Recovery Trend Graph
        item(key = "cns_chart_section") {
            Spacer(modifier = Modifier.height(4.dp))
            CnsRecoveryChart(checkinHistory = readinessHistory)
        }

        // 9. Weekly Volume Landmarks Breakdown
        item(key = "volume_landmarks_header") {
            Spacer(modifier = Modifier.height(8.dp))
            Column {
                Text(
                    text = "Наукові орієнтири обсягу (Цього тижня)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Порівняння виконаних підходів з індивідуальними порогами MV, MEV, MAV, MRV",
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(volumeLandmarks, key = { "vl_${it.muscleGroup.name}" }) { landmark ->
            VolumeLandmarkRow(landmark)
        }

        // 10. Body Measurements Log
        item(key = "measurements_header") {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Щоденник замірів тіла",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Динаміка маси тіла, талії, біцепса та % жиру",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = { showAddMeasurementModal = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("add_measurement_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Замір", fontSize = 11.sp)
                }
            }
        }

        if (bodyMeasurements.isEmpty()) {
            item(key = "empty_measurements_placeholder") {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Немає записів замірів. Додайте перший замір для відстеження прогресу гіпертрофії.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        } else {
            items(bodyMeasurements, key = { "bm_${it.id}" }) { m ->
                BodyMeasurementItemCard(m)
            }
        }
    }

    if (showAddMeasurementModal) {
        AddMeasurementDialog(
            onSave = { w, waist, bic, chest, fat ->
                viewModel.addBodyMeasurement(w, waist, bic, chest, fat)
                showAddMeasurementModal = false
            },
            onDismiss = { showAddMeasurementModal = false }
        )
    }
}

@Composable
fun StatTile(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(title, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

data class ExercisePersonalRecord(
    val exerciseName: String,
    val muscleGroup: String,
    val maxWeightKg: Double,
    val bestRepsAtMaxWeight: Int,
    val calculated1Rm: Double,
    val bestTonnageSet: Double
)

@Composable
fun PersonalRecordCard(pr: ExercisePersonalRecord) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().testTag("pr_card_${pr.exerciseName}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pr.exerciseName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                Text("Кращий сет: ${pr.maxWeightKg} кг × ${pr.bestRepsAtMaxWeight} повт.", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = IceCyanContainer,
                border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary)
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${String.format("%.1f", pr.calculated1Rm)} кг",
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        color = IceCyanPrimary
                    )
                    Text("1RM (Еплі)", style = MaterialTheme.typography.labelSmall, fontSize = 9.sp, color = IceCyanPrimary)
                }
            }
        }
    }
}

@Composable
fun VolumeLandmarkRow(landmark: VolumeLandmarkData) {
    val current = landmark.currentWeeklySets
    val statusText = when {
        current < landmark.mv -> "Нижче MV (Втрата)"
        current < landmark.mev -> "Збереження (MV)"
        current in landmark.mev until landmark.mavMin -> "Мін. ріст (MEV)"
        current in landmark.mavMin..landmark.mavMax -> "Оптимальний ріст (MAV)"
        current in (landmark.mavMax + 1)..landmark.mrv -> "Перевантаження"
        else -> "Перетренованість (>MRV)"
    }

    val statusColor = when {
        current in landmark.mavMin..landmark.mavMax -> SuccessGreen
        current in landmark.mev until landmark.mavMin -> IceCyanPrimary
        current > landmark.mrv -> ErrorRed
        current > landmark.mavMax -> WarningAmber
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().testTag("landmark_row_${landmark.muscleGroup.name}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (landmark.isPriority) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = landmark.muscleGroup.displayNameUk,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Text(
                    text = "$current сетів • $statusText",
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = statusColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            val progress = if (landmark.mrv > 0) (current.toFloat() / landmark.mrv).coerceIn(0f, 1.2f) else 0f
            LinearProgressIndicator(
                progress = { progress.coerceAtMost(1f) },
                modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("MV: ${landmark.mv}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("MEV: ${landmark.mev}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("MAV: ${landmark.mavMin}–${landmark.mavMax}", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IceCyanPrimary)
                Text("MRV: ${landmark.mrv}", fontSize = 10.sp, color = ErrorRed)
            }
        }
    }
}

@Composable
fun BodyMeasurementItemCard(m: BodyMeasurementEntity) {
    val sdf = remember { java.text.SimpleDateFormat("dd MMMM yyyy", java.util.Locale.forLanguageTag("uk")) }
    val dateStr = remember(m.dateTimestamp) { sdf.format(java.util.Date(m.dateTimestamp)) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth().testTag("measurement_item_${m.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = SteelBlueSecondary)
                Text("${m.weightKg} кг", fontWeight = FontWeight.Bold, color = IceCyanPrimary)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                if (m.waistCm > 0) Text("Талія: ${m.waistCm} см", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.bicepsCm > 0) Text("Біцепс: ${m.bicepsCm} см", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.chestCm > 0) Text("Груди: ${m.chestCm} см", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (m.bodyFatPercent > 0) Text("Жир: ${m.bodyFatPercent}%", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun AddMeasurementDialog(
    onSave: (weight: Double, waist: Double, biceps: Double, chest: Double, fat: Double) -> Unit,
    onDismiss: () -> Unit
) {
    var weightText by remember { mutableStateOf("80.0") }
    var waistText by remember { mutableStateOf("82.0") }
    var bicepsText by remember { mutableStateOf("38.5") }
    var chestText by remember { mutableStateOf("106.0") }
    var fatText by remember { mutableStateOf("14.5") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp).testTag("add_measurement_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Новий замір тіла", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("Вага тіла (кг)*") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = waistText,
                        onValueChange = { waistText = it },
                        label = { Text("Талія (см)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = bicepsText,
                        onValueChange = { bicepsText = it },
                        label = { Text("Біцепс (см)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = chestText,
                        onValueChange = { chestText = it },
                        label = { Text("Грудні (см)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = fatText,
                        onValueChange = { fatText = it },
                        label = { Text("% Жиру") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        val w = weightText.toDoubleOrNull() ?: 0.0
                        val waist = waistText.toDoubleOrNull() ?: 0.0
                        val bic = bicepsText.toDoubleOrNull() ?: 0.0
                        val chest = chestText.toDoubleOrNull() ?: 0.0
                        val fat = fatText.toDoubleOrNull() ?: 0.0
                        onSave(w, waist, bic, chest, fat)
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_measurement_btn")
                ) {
                    Text("Зберегти замір")
                }
            }
        }
    }
}
