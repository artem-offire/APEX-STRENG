package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DailyGoalsState
import com.example.ui.theme.*

@Composable
fun DailyGoalsCard(
    state: DailyGoalsState,
    onToggleGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showInfoDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            if (state.isReady && state.completedCount == state.goals.size && state.goals.isNotEmpty()) SuccessGreen else IceCyanPrimary
        ),
        modifier = modifier
            .fillMaxWidth()
            .testTag("daily_goals_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (state.isReady) IceCyanContainer else MaterialTheme.colorScheme.surface,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(if (state.isReady) "🎯" else "⏳", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Цілі на день",
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Text(
                            text = if (state.isReady) "На основі регулярних вправ" else "Формування персонального профілю",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showInfoDialog = true },
                        modifier = Modifier.size(32.dp).testTag("daily_goals_info_btn")
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Що це таке",
                            tint = IceCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    if (state.isReady) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (state.completedCount == state.goals.size) SuccessGreen.copy(alpha = 0.2f) else IceCyanContainer
                        ) {
                            Text(
                                text = "${state.completedCount} з ${state.goals.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (state.completedCount == state.goals.size) SuccessGreen else IceCyanPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (!state.isReady) {
                // Not ready: Show 1-week formation status
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Аналіз регулярності: день ${state.daysActive} з 7",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceCyanPrimary
                            )
                            Text(
                                text = "${((state.daysActive / 7f) * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        LinearProgressIndicator(
                            progress = { (state.daysActive / 7f).coerceIn(0.1f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = IceCyanPrimary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Щоденні цілі автоматично створяться через 7 днів використання на базі вправ, які ви виконуєте найчастіше.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            } else {
                // Ready: List the goals with checkboxes
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.goals.forEach { goal ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (goal.isCompleted) MaterialTheme.colorScheme.surface.copy(alpha = 0.8f) else MaterialTheme.colorScheme.surface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (goal.isCompleted) SuccessGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleGoal(goal.id) }
                                .testTag("daily_goal_${goal.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = { onToggleGoal(goal.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = if (goal.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = null,
                                        tint = if (goal.isCompleted) SuccessGreen else MaterialTheme.colorScheme.outline,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(goal.iconEmoji, fontSize = 12.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = goal.titleUk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (goal.isCompleted) SuccessGreen else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = goal.descriptionUk,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Ціль: ${goal.targetValue}",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = IceCyanPrimary
                                    )
                                }
                            }
                        }
                    }

                    if (state.completedCount == state.goals.size && state.goals.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = SuccessGreen.copy(alpha = 0.15f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Всі цілі на сьогодні виконано! Відмінна дисципліна!",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = IceCyanPrimary)
            },
            title = {
                Text("Що це таке: Цілі на день", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Це розумна адаптивна система щоденних завдань:\n\n" +
                            "• Перші 7 днів: Додаток аналізує вашу тренувальну активність, частоту занять та вправи, які ви виконуєте найчастіше.\n" +
                            "• Після 7 днів: Щодня генеруються 3 персональні мікро-цілі на основі вашого профілю (наприклад, виконати цільовий обсяг підходів, дотриматися пауз або зробити розминку).\n" +
                            "• Мета: Формування залізної звички та регулярності без ризику перетренованості.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) {
                    Text("Зрозуміло")
                }
            }
        )
    }
}
