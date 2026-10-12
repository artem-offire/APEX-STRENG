package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MesocycleEntity
import com.example.data.MesocycleWeekPlanEntity
import com.example.data.ProgressiveOverloadLogEntity
import com.example.data.WorkoutSessionEntity
import com.example.ui.theme.*
import kotlin.math.roundToInt

@Composable
fun MesocycleStructureDialog(
    mesocycle: MesocycleEntity,
    sessions: List<WorkoutSessionEntity>,
    weekPlans: List<MesocycleWeekPlanEntity> = emptyList(),
    overloadLogs: List<ProgressiveOverloadLogEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    var selectedWeekTab by remember { mutableStateOf(mesocycle.currentWeek) }
    var selectedSectionTab by remember { mutableStateOf(0) } // 0 = По тижнях, 1 = Журнал прогресії (Room), 2 = Схема БД Room

    val sessionsByWeek = remember(sessions) {
        sessions.groupBy { it.weekNumber }
    }

    val weekPlanByWeek = remember(weekPlans) {
        weekPlans.associateBy { it.weekNumber }
    }

    val weeksCount = mesocycle.totalWeeks

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.90f)
                .padding(vertical = 12.dp)
                .testTag("mesocycle_structure_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Структура та прогресія блоку",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${mesocycle.name} • Модель: ${mesocycle.progressionModel}",
                            style = MaterialTheme.typography.bodySmall,
                            color = IceCyanPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Top View Switcher (Weekly Plan vs Overload Logs vs Room Schema)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedSectionTab == 0,
                        onClick = { selectedSectionTab = 0 },
                        label = { Text("План тижнів", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarViewWeek, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedSectionTab == 1,
                        onClick = { selectedSectionTab = 1 },
                        label = { Text("Прогресія (${overloadLogs.size})", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                    FilterChip(
                        selected = selectedSectionTab == 2,
                        onClick = { selectedSectionTab = 2 },
                        label = { Text("Схема Room БД", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Default.Storage, contentDescription = null, modifier = Modifier.size(15.dp))
                        }
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                when (selectedSectionTab) {
                    0 -> {
                        // Current Phase Ribbon
                        val activePlan = weekPlanByWeek[selectedWeekTab]
                        val isCurrentWeekDeload = activePlan?.isDeload ?: (selectedWeekTab == weeksCount)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentWeekDeload) SuccessGreen.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isCurrentWeekDeload) SuccessGreen else IceCyanPrimary.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = activePlan?.phaseNameUk ?: "Тиждень $selectedWeekTab з $weeksCount",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        val phaseDescription = activePlan?.coachGuidelineUk?.ifBlank { null } ?: when {
                                            selectedWeekTab == weeksCount -> "Делоад • Зниження обсягу на 40% для суперкомпенсації"
                                            selectedWeekTab == 1 -> "Фаза адаптації (MEV) • RIR 3 • Базове навантаження"
                                            selectedWeekTab == 2 -> "Акумуляція обсягу (MAV) • RIR 2 • +2.5% ваги"
                                            selectedWeekTab == 3 -> "Перевантаження (MAV High) • RIR 1 • +5.0% ваги"
                                            else -> "Піковий обсяг (MRV) • RIR 0–1 • Максимальний стрес"
                                        }
                                        Text(
                                            text = phaseDescription,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isCurrentWeekDeload) SuccessGreen else IceCyanContainer
                                    ) {
                                        Text(
                                            text = if (isCurrentWeekDeload) "DELOAD" else "RIR ${activePlan?.targetRir ?: (3 - (selectedWeekTab - 1)).coerceAtLeast(0)}",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = if (isCurrentWeekDeload) Color.White else IceCyanPrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                if (activePlan != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val setsPct = (activePlan.setsMultiplier * 100).roundToInt()
                                        val weightPct = (activePlan.weightMultiplier * 100).roundToInt()
                                        ProgressionMetricChip(
                                            label = "Обсяг сетів",
                                            value = "$setsPct%",
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProgressionMetricChip(
                                            label = "Інтенсивність",
                                            value = "$weightPct%",
                                            modifier = Modifier.weight(1f)
                                        )
                                        ProgressionMetricChip(
                                            label = "Повтори",
                                            value = "${activePlan.targetRepsMin}–${activePlan.targetRepsMax}",
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Weeks Tabs
                        Row(
                            modifier = Modifier.horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            for (w in 1..weeksCount) {
                                val isSelected = selectedWeekTab == w
                                val isDeload = (w == weeksCount)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { selectedWeekTab = w },
                                    label = {
                                        Text(
                                            text = if (isDeload) "W$w (Делоад)" else "Тиждень $w",
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Week Sessions & Load Targets
                        val weekSessions = sessionsByWeek[selectedWeekTab] ?: emptyList()

                        if (weekSessions.isNotEmpty()) {
                            Text(
                                text = "Тренувальні сесії тижня $selectedWeekTab (${weekSessions.size} занять):",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(weekSessions) { session ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            if (session.isCompleted) SuccessGreen.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(12.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                                Icon(
                                                    if (session.isCompleted) Icons.Default.CheckCircle else Icons.Default.FitnessCenter,
                                                    contentDescription = null,
                                                    tint = if (session.isCompleted) SuccessGreen else IceCyanPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column {
                                                    Text(session.title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                                    val detailsText = if (session.isCompleted) {
                                                        val prBadge = if (session.prCount > 0) " • 🏆 PR: ${session.prCount}" else ""
                                                        "Виконано • ${session.durationSeconds / 60} хв • ${session.totalTonnageKg.roundToInt()} кг$prBadge"
                                                    } else {
                                                        "Заплановане заняття • Статус: ${session.progressionStatus}"
                                                    }
                                                    Text(
                                                        text = detailsText,
                                                        fontSize = 11.sp,
                                                        color = if (session.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }

                                            if (session.isCompleted) {
                                                Text("✓", fontSize = 14.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Сесії для тижня $selectedWeekTab будуть згенеровані під час переходу",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    1 -> {
                        // Progressive Overload Logs stored in Room
                        Text(
                            text = "Історія прогресивного навантаження (progressive_overload_logs):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        if (overloadLogs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Завершіть тренування, щоб зафіксувати прогресію ваги та повторів.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(overloadLogs) { log ->
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.35f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = log.exerciseName,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Surface(
                                                    shape = RoundedCornerShape(6.dp),
                                                    color = IceCyanContainer
                                                ) {
                                                    Text(
                                                        text = log.overloadType,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = IceCyanPrimary,
                                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = "Було: ${log.previousWeightKg} кг × ${log.previousReps} → Стало: ${log.newWeightKg} кг × ${log.newReps} (1RM: ${String.format("%.1f", log.newEstimated1RmKg)} кг)",
                                                fontSize = 11.sp,
                                                color = SuccessGreen,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = log.summaryUk,
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    2 -> {
                        // Room Database Schema Overview
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                SchemaEntityInfoCard(
                                    tableName = "exercises (ExerciseEntity)",
                                    pkInfo = "PK: id (autoGenerate) • Індекси: name, muscleGroup, equipment",
                                    fieldsInfo = "name, muscleGroup, secondaryMuscles, equipment, movementPattern, isCompound, defaultIncrementKg, defaultTargetRepsMin/Max, defaultTargetRir, progressionType"
                                )
                            }
                            item {
                                SchemaEntityInfoCard(
                                    tableName = "mesocycles (MesocycleEntity)",
                                    pkInfo = "PK: id • Індекси: isActive, startDate",
                                    fieldsInfo = "name, totalWeeks, currentWeek, programType, priorityMuscles, progressionModel, deloadWeek, startingRir, peakRir, weeklySetIncrement, weeklyWeightIncrementPercent"
                                )
                            }
                            item {
                                SchemaEntityInfoCard(
                                    tableName = "mesocycle_week_plans (MesocycleWeekPlanEntity)",
                                    pkInfo = "PK: id • FK: mesocycleId → mesocycles(id) CASCADE",
                                    fieldsInfo = "weekNumber, phaseCode, phaseNameUk, isDeload, targetRir, setsMultiplier, weightMultiplier, targetRepsMin/Max, plannedTotalSets, plannedTonnageKg"
                                )
                            }
                            item {
                                SchemaEntityInfoCard(
                                    tableName = "workout_sessions (WorkoutSessionEntity)",
                                    pkInfo = "PK: id • Індекси: mesocycleId, weekNumber, dateTimestamp, isCompleted",
                                    fieldsInfo = "mesocycleId, weekNumber, title, durationSeconds, totalTonnageKg, totalWorkingSets, averageRir, prCount, progressionStatus, previousSessionTonnageDeltaKg"
                                )
                            }
                            item {
                                SchemaEntityInfoCard(
                                    tableName = "workout_exercises (WorkoutExerciseEntity)",
                                    pkInfo = "PK: id • Індекси: sessionId, exerciseId, exerciseName, muscleGroup",
                                    fieldsInfo = "sessionId, exerciseId, orderIndex, targetSets, targetRepsMin/Max, targetWeightKg, targetRir, previousBestWeightKg/Reps, overloadAchieved, progressionAction"
                                )
                            }
                            item {
                                SchemaEntityInfoCard(
                                    tableName = "workout_sets (WorkoutSetEntity)",
                                    pkInfo = "PK: id • Індекси: workoutExerciseId, isCompleted, timestamp",
                                    fieldsInfo = "workoutExerciseId, setNumber, weightKg, targetWeightKg, reps, targetRepsMin/Max, rir, rpe, estimated1RmKg, tonnageKg, isPersonalRecord, setType"
                                )
                            }
                            item {
                                SchemaEntityInfoCard(
                                    tableName = "progressive_overload_logs (ProgressiveOverloadLogEntity)",
                                    pkInfo = "PK: id • Індекси: exerciseName, muscleGroup, sessionId, mesocycleId, timestamp",
                                    fieldsInfo = "previousWeightKg/newWeightKg, previousReps/newReps, previousEstimated1RmKg/newEstimated1RmKg, overloadType, nextRecommendedWeightKg, summaryUk"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressionMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = IceCyanPrimary)
        }
    }
}

@Composable
private fun SchemaEntityInfoCard(
    tableName: String,
    pkInfo: String,
    fieldsInfo: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.TableChart,
                    contentDescription = null,
                    tint = IceCyanPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = tableName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = IceCyanPrimary
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = pkInfo,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = fieldsInfo,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 15.sp
            )
        }
    }
}
