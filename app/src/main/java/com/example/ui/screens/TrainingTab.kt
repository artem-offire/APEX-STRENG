package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.zIndex
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import com.example.ui.components.NotificationsModal
import com.example.ui.theme.*
import com.example.engine.FrequentWorkoutEngine
import com.example.model.FrequentWorkoutItem
import com.example.model.ReadinessBadgeType
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
    val allCatalogExercises by viewModel.allExercises.collectAsState()
    val recoverySummary by viewModel.muscleRecoverySummary.collectAsState()
    val recoveryNotificationsEnabled by viewModel.recoveryNotificationsEnabled.collectAsState()
    val notifications by viewModel.appNotifications.collectAsState()
    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

    var workoutTitleInput by remember { mutableStateOf("") }
    var showMobilityModal by remember { mutableStateOf<Boolean?>(null) } // true=warmup, false=cooldown
    var isHistoryExpanded by remember { mutableStateOf(false) }
    var showNotificationsModal by remember { mutableStateOf(false) }
    var sessionToDeleteFromHistory by remember { mutableStateOf<WorkoutSessionEntity?>(null) }

    val completedSessions = remember(allSessions) {
        allSessions.filter { it.isCompleted }.sortedByDescending { it.dateTimestamp }
    }

    val smartFrequentWorkouts = remember(completedSessions, allCompletedSets, recoverySummary, allCatalogExercises) {
        FrequentWorkoutEngine.buildFrequentWorkouts(
            completedSessions = completedSessions,
            allCompletedSets = allCompletedSets,
            recoverySummary = recoverySummary,
            allCatalogExercises = allCatalogExercises
        )
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
    val isDark = apexIsDark()
    val startWorkout = {
        val finalTitle = workoutTitleInput.trim().ifEmpty { "Силове тренування" }
        viewModel.startCustomWorkout(finalTitle)
    }

    var topBannerText by remember { mutableStateOf<String?>(null) }
    var topBannerIsOn by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var hideBannerJob by remember { mutableStateOf<Job?>(null) }

    val showNotificationToggleBanner: (Boolean) -> Unit = { enabled ->
        topBannerIsOn = enabled
        topBannerText = if (enabled) "Сповіщення увімкнено" else "Сповіщення вимкнено"
        hideBannerJob?.cancel()
        hideBannerJob = coroutineScope.launch {
            delay(2500)
            topBannerText = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 20.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
        // 1. Top Branding & Title
        item(key = "title_section") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "APEX",
                        style = ApexSectionLabel.copy(fontFamily = Unbounded, fontWeight = FontWeight.Bold, fontSize = 13.sp, letterSpacing = 2.3.sp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = currentDateText,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Тренування",
                        style = ApexScreenTitle.copy(fontSize = 28.sp, lineHeight = 32.sp),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .size(46.dp)
                        .clickable { showNotificationsModal = true }
                        .testTag("open_notifications_inbox_header_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Сповіщення програми",
                            tint = if (unreadNotificationsCount > 0) IceCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                        if (unreadNotificationsCount > 0) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(9.dp)
                            ) {}
                        }
                    }
                }
            }
        }

        // 2. Frequent Workouts Section (ЧАСТІ ТРЕНУВАННЯ) — інтелектуальне ранжування за готовністю м'язів
        item(key = "frequent_workouts_section") {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ЧАСТІ ТРЕНУВАННЯ",
                        style = ApexSectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (recoverySummary.readyTodayMuscleGroups.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFFF59E0B).copy(alpha = 0.15f)
                        ) {
                            Text(
                                text = "🔥 Свіжі сьогодні: ${recoverySummary.readyTodayMuscleGroups.size}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF59E0B),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                if (smartFrequentWorkouts.isNotEmpty()) {
                    smartFrequentWorkouts.withIndex().chunked(2).forEach { rowItems ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowItems.forEach { (index, item) ->
                                val borderColor = when (item.badgeType) {
                                    ReadinessBadgeType.PEAK_TODAY -> Color(0xFFF59E0B).copy(alpha = 0.6f)
                                    ReadinessBadgeType.FULLY_READY -> IceCyanPrimary.copy(alpha = 0.4f)
                                    ReadinessBadgeType.RECOVERING -> MaterialTheme.colorScheme.outline
                                }
                                val badgeColor = when (item.badgeType) {
                                    ReadinessBadgeType.PEAK_TODAY -> Color(0xFFF59E0B)
                                    ReadinessBadgeType.FULLY_READY -> Color(0xFF10B981)
                                    ReadinessBadgeType.RECOVERING -> MaterialTheme.colorScheme.onSurfaceVariant
                                }

                                Surface(
                                    onClick = { viewModel.startFrequentWorkout(item) },
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, borderColor),
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 66.dp)
                                        .testTag("frequent_workout_chip_$index")
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Icon(
                                                    imageVector = if (item.isFromHistory) Icons.Default.Replay else Icons.Default.Bolt,
                                                    contentDescription = null,
                                                    tint = badgeColor,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = item.title,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(4.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = item.subtitle,
                                                fontSize = 11.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = badgeColor.copy(alpha = 0.12f)
                                            ) {
                                                Text(
                                                    text = item.badgeText,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = badgeColor,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            if (rowItems.size < 2) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Тут будуть ваші тренування, адаптовані до відновлення м'язів",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 3. Hero Start Workout Card
        item(key = "hero_start_card") {
            val heroBrush = if (isDark) {
                Brush.verticalGradient(listOf(Color(0xFF102326), Color(0xFF0F1A1E)))
            } else {
                Brush.verticalGradient(listOf(Color(0xFFE3F7F4), Color(0xFFF1FAF8)))
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(26.dp))
                    .background(heroBrush)
                    .border(1.dp, if (isDark) Color(0xFF1F3E42) else Color(0xFFBFE6E0), RoundedCornerShape(26.dp))
                    .padding(20.dp)
                    .testTag("today_workout_selector_card"),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ВІЛЬНЕ ТРЕНУВАННЯ",
                    style = ApexSectionLabel,
                    color = MaterialTheme.colorScheme.primary
                )
                OutlinedTextField(
                    value = workoutTitleInput,
                    onValueChange = { workoutTitleInput = it },
                    placeholder = {
                        Text(
                            text = "Назва для тренування",
                            color = TextMutedDark,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    textStyle = LocalTextStyle.current.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = if (isDark) Color(0xFF0B1316) else MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = if (isDark) Color(0xFF0B1316) else MaterialTheme.colorScheme.surface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { startWorkout() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workout_title_input")
                )

                Button(
                    onClick = { startWorkout() },
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp)
                        .testTag("start_free_workout_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Почати тренування",
                        fontFamily = Unbounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }

        // 4. Warmup & Cooldown tools (2 in a row)
        item(key = "warmup_cooldown_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MobilityToolButton(
                    title = "Розминка",
                    subtitle = "5–10 хв",
                    icon = Icons.Default.DirectionsRun,
                    accent = WarningAmber,
                    tint = if (isDark) Color(0xFF1E1710) else Color(0xFFFFF1E4),
                    borderColor = if (isDark) Color(0xFF3A2A18) else Color(0xFFF3D9BF),
                    subtitleColor = if (isDark) Color(0xFFA89480) else Color(0xFF8A6A4A),
                    onClick = { showMobilityModal = true },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("dynamic_warmup_btn")
                )
                MobilityToolButton(
                    title = "Заминка",
                    subtitle = "Розтяжка",
                    icon = Icons.Default.SelfImprovement,
                    accent = Color(0xFF7FB2F0),
                    tint = if (isDark) Color(0xFF0F1824) else Color(0xFFE6F0FC),
                    borderColor = if (isDark) Color(0xFF1C2C40) else Color(0xFFC9DBF2),
                    subtitleColor = if (isDark) Color(0xFF8EA3BC) else Color(0xFF4F6784),
                    onClick = { showMobilityModal = false },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("static_cooldown_btn")
                )
            }
        }

        // 5. Muscle Readiness & Recovery Card (48-72h physiological interval & push notifications)
        item(key = "muscle_recovery_section") {
            MuscleRecoveryCard(
                summary = recoverySummary,
                notificationsEnabled = recoveryNotificationsEnabled,
                unreadNotificationsCount = unreadNotificationsCount,
                onOpenNotificationsModal = { showNotificationsModal = true },
                onToggleNotifications = { enabled ->
                    viewModel.setRecoveryNotificationsEnabled(enabled)
                    showNotificationToggleBanner(enabled)
                },
                onSendTestNotification = { muscle ->
                    viewModel.sendTestRecoveryNotification(muscle)
                },
                onStartWorkoutWithMuscles = { title, _ ->
                    viewModel.startCustomWorkout(title)
                }
            )
        }

        // 6. Collapsible Workout History
        item(key = "history_section") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
                    .padding(6.dp)
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { isHistoryExpanded = !isHistoryExpanded }
                        .padding(start = 10.dp, end = 8.dp, top = 6.dp, bottom = 6.dp)
                        .testTag("toggle_history_expand_btn"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = buildAnnotatedString {
                                append("Історія тренувань ")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) {
                                    append("(${completedSessions.size})")
                                }
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (isHistoryExpanded) {
                                "Натисніть, щоб згорнути"
                            } else {
                                "Згорнуто · натисніть, щоб розгорнути"
                            },
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    val chevronRotation by animateFloatAsState(
                        targetValue = if (isHistoryExpanded) 180f else 0f,
                        label = "history_chevron"
                    )
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(chevronRotation)
                        )
                    }
                }

                if (isHistoryExpanded) {
                    if (completedSessions.isEmpty()) {
                        Text(
                            text = "Ще немає завершених тренувань",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    } else {
                        completedSessions.take(20).forEach { session ->
                            key(session.id) {
                                WorkoutHistoryCard(
                                    session = session,
                                    onRepeat = { viewModel.repeatWorkout(session.id) },
                                    onDelete = { sessionToDeleteFromHistory = session }
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = onOpenCalendar,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "Відкрити в календарі",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            textDecoration = TextDecoration.Underline,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    // Top notification heads-up popup banner
    AnimatedVisibility(
        visible = topBannerText != null,
        enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
        modifier = Modifier
            .align(Alignment.TopCenter)
            .statusBarsPadding()
            .padding(top = 16.dp, start = 20.dp, end = 20.dp)
            .zIndex(100f)
    ) {
        topBannerText?.let { text ->
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                border = BorderStroke(
                    1.dp,
                    if (topBannerIsOn) IceCyanPrimary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline
                ),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .clickable { topBannerText = null }
                    .testTag("top_notification_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (topBannerIsOn) Icons.Default.NotificationsActive else Icons.Default.NotificationsOff,
                        contentDescription = null,
                        tint = if (topBannerIsOn) IceCyanPrimary else Color(0xFFF59E0B),
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = text,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
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

    if (showNotificationsModal) {
        NotificationsModal(
            notifications = notifications,
            onMarkRead = { id -> viewModel.markNotificationRead(id) },
            onMarkAllRead = { viewModel.markAllNotificationsRead() },
            onClearAll = { viewModel.clearAllNotifications() },
            onDismiss = { showNotificationsModal = false }
        )
    }

    sessionToDeleteFromHistory?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDeleteFromHistory = null },
            icon = {
                Icon(
                    imageVector = Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text(
                    text = "Видалити тренування?",
                    fontFamily = Unbounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    lineHeight = 32.sp
                )
            },
            text = {
                Text(
                    text = "Ви дійсно бажаєте видалити «${session.title}»? Цю дію неможливо скасувати.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSession(session.id)
                        sessionToDeleteFromHistory = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.testTag("confirm_delete_history_session_btn")
                ) {
                    Text("Видалити", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDeleteFromHistory = null }) {
                    Text("Скасувати", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        )
    }
}

@Composable
private fun MobilityToolButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accent: Color,
    tint: Color,
    borderColor: Color,
    subtitleColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .heightIn(min = 72.dp)
            .clip(shape)
            .background(
                Brush.linearGradient(
                    0f to tint,
                    0.7f to MaterialTheme.colorScheme.surface
                )
            )
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accent.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = subtitleColor
            )
        }
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
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$dateFormatted · $durationMinutes хв",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (onRepeat != null) {
                OutlinedButton(
                    onClick = onRepeat,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, IceCyanContainerBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier
                        .height(44.dp)
                        .testTag("repeat_history_btn_${session.id}")
                ) {
                    Text(
                        text = "Повторити",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Видалити тренування",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
