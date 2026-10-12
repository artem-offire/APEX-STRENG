package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.WorkoutSessionEntity
import com.example.ui.ApexViewModel
import com.example.ui.components.MobilityTimerModal
import com.example.ui.components.MuscleRecoveryCard
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TrainingTab(
    viewModel: ApexViewModel,
    onStartWorkout: (Long) -> Unit,
    onOpenPrograms: () -> Unit = {},
    onOpenCalendar: () -> Unit = {}
) {
    val allSessions by viewModel.allSessions.collectAsState()
    val allCompletedSets by viewModel.allCompletedSets.collectAsState()
    val recoverySummary by viewModel.muscleRecoverySummary.collectAsState()
    val recoveryNotificationsEnabled by viewModel.recoveryNotificationsEnabled.collectAsState()

    var workoutTitleInput by remember { mutableStateOf("") }
    var showMobilityModal by remember { mutableStateOf<Boolean?>(null) } // true=warmup, false=cooldown
    var isHistoryExpanded by remember { mutableStateOf(false) }

    val completedSessions = remember(allSessions) {
        allSessions.filter { it.isCompleted }.sortedByDescending { it.dateTimestamp }
    }

    val frequentExercises = remember(allCompletedSets) {
        val defaultChips = listOf("Жим лежачи", "Присідання", "Тяга блока")
        val userTop = allCompletedSets
            .groupBy { it.exerciseName }
            .entries
            .sortedByDescending { it.value.size }
            .map { it.key }
        (defaultChips + userTop).distinct().take(6)
    }

    val currentDateText = remember {
        try {
            val sdf = SimpleDateFormat("EEEE, d MMMM", Locale("uk", "UA"))
            sdf.format(Date()).replaceFirstChar { it.uppercase() }
        } catch (e: Exception) {
            "Неділя, 11 жовтня"
        }
    }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Branding & Title
        item(key = "title_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "APEX",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = IceCyanPrimary,
                    letterSpacing = 1.6.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = currentDateText,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Тренування",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 2. Hero Start Workout Card
        item(key = "hero_start_card") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("today_workout_selector_card")
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = workoutTitleInput,
                        onValueChange = { workoutTitleInput = it },
                        placeholder = {
                            Text(
                                text = "Назва для тренування",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                fontSize = 14.sp
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.background,
                            unfocusedContainerColor = MaterialTheme.colorScheme.background,
                            focusedBorderColor = IceCyanPrimary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val finalTitle = workoutTitleInput.trim().ifEmpty { "Силове тренування" }
                                viewModel.startCustomWorkout(finalTitle)
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("workout_title_input")
                    )

                    Button(
                        onClick = {
                            val finalTitle = workoutTitleInput.trim().ifEmpty { "Силове тренування" }
                            viewModel.startCustomWorkout(finalTitle)
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = IceCyanPrimary,
                            contentColor = IceCyanOnPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .testTag("start_free_workout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Почати тренування",
                            tint = IceCyanOnPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }
            }
        }

        // 3. Muscle Readiness & Recovery Card (48-72h physiological interval & push notifications)
        item(key = "muscle_recovery_section") {
            MuscleRecoveryCard(
                summary = recoverySummary,
                notificationsEnabled = recoveryNotificationsEnabled,
                onToggleNotifications = { enabled ->
                    viewModel.setRecoveryNotificationsEnabled(enabled)
                },
                onSendTestNotification = { muscle ->
                    viewModel.sendTestRecoveryNotification(muscle)
                },
                onStartWorkoutWithMuscles = { title, _ ->
                    viewModel.startCustomWorkout(title)
                }
            )
        }

        // 4. Warmup & Cooldown Cards (2 in a row)
        item(key = "warmup_cooldown_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Warmup Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showMobilityModal = true }
                        .testTag("dynamic_warmup_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsRun,
                                    contentDescription = "Розминка",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Розминка",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "5–10 хв",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Cooldown Card
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .weight(1f)
                        .clickable { showMobilityModal = false }
                        .testTag("static_cooldown_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.SelfImprovement,
                                    contentDescription = "Заминка",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Заминка",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Розтяжка",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Frequent Exercises Section (ЧАСТІ ВПРАВИ)
        item(key = "frequent_exercises_section") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ЧАСТІ ВПРАВИ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    frequentExercises.forEachIndexed { index, exName ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .clickable { viewModel.startQuickExerciseWorkout(exName) }
                                .testTag("frequent_exercise_chip_$index")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Replay,
                                    contentDescription = null,
                                    tint = IceCyanPrimary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = exName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }
        }

        // 5. Collapsible Workout History Card
        item(key = "history_header") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isHistoryExpanded = !isHistoryExpanded }
                    .testTag("toggle_history_expand_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Історія тренувань (${completedSessions.size})",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (isHistoryExpanded) {
                                    "Розгорнуто · натисніть, щоб згорнути"
                                } else {
                                    "Згорнуто · натисніть, щоб розгорнути"
                                },
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (isHistoryExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isHistoryExpanded) {
            if (completedSessions.isEmpty()) {
                item(key = "empty_history") {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Ще немає завершених тренувань",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(
                    items = completedSessions.take(20),
                    key = { "session_${it.id}" }
                ) { session ->
                    WorkoutHistoryCard(
                        session = session,
                        onRepeat = { viewModel.repeatWorkout(session.id) },
                        onDelete = { viewModel.deleteSession(session.id) }
                    )
                }
            }
        }
    }

    showMobilityModal?.let { isWarmup ->
        MobilityTimerModal(
            isWarmup = isWarmup,
            onDismiss = { showMobilityModal = null }
        )
    }
}

@Composable
fun WorkoutHistoryCard(
    session: WorkoutSessionEntity,
    onRepeat: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    val durationMinutes = (session.durationSeconds / 60).coerceAtLeast(1)
    val sdf = remember { SimpleDateFormat("d MMMM, HH:mm", Locale("uk", "UA")) }
    val dateFormatted = remember(session.dateTimestamp) {
        sdf.format(Date(session.dateTimestamp))
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateFormatted · $durationMinutes хв",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onRepeat != null) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = IceCyanContainer,
                        border = BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .clickable { onRepeat() }
                            .testTag("repeat_history_btn_${session.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                tint = IceCyanPrimary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Повторити",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IceCyanPrimary
                            )
                        }
                    }
                }

                if (onDelete != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Видалити тренування",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
