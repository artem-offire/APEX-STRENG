package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.WorkoutSessionEntity
import com.example.model.WorkoutRecurrence
import com.example.ui.ApexViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun CalendarTab(
    viewModel: ApexViewModel,
    onStartWorkout: (Long) -> Unit
) {
    val allSessions by viewModel.allSessions.collectAsState()

    val todayCalendar = remember { Calendar.getInstance() }
    val todayYear = todayCalendar.get(Calendar.YEAR)
    val todayMonth = todayCalendar.get(Calendar.MONTH)
    val todayDay = todayCalendar.get(Calendar.DAY_OF_MONTH)
    val todayKey = remember {
        String.format(Locale.US, "%04d-%02d-%02d", todayYear, todayMonth + 1, todayDay)
    }

    var displayedYear by remember { mutableStateOf(todayYear) }
    var displayedMonth by remember { mutableStateOf(todayMonth) }
    var selectedDateKey by remember { mutableStateOf(todayKey) }
    var showScheduleDialog by remember { mutableStateOf(false) }

    val sessionsByDate = remember(allSessions) {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        allSessions.groupBy { session ->
            dateFormat.format(Date(session.dateTimestamp))
        }
    }

    val currentMonthSessions = remember(allSessions, displayedYear, displayedMonth) {
        val cal = Calendar.getInstance()
        allSessions.filter { session ->
            cal.timeInMillis = session.dateTimestamp
            cal.get(Calendar.YEAR) == displayedYear && cal.get(Calendar.MONTH) == displayedMonth
        }
    }
    val completedInMonthCount = remember(currentMonthSessions) {
        currentMonthSessions.count { it.isCompleted }
    }
    val plannedInMonthCount = remember(currentMonthSessions) {
        currentMonthSessions.count { !it.isCompleted }
    }
    val totalMinutesMonth = remember(currentMonthSessions) {
        currentMonthSessions.filter { it.isCompleted }.sumOf { (it.durationSeconds / 60).coerceAtLeast(1) }
    }

    val monthNamesUk = listOf(
        "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
        "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
    )

    val weekdayHeaders = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Нд")

    val calendarDays = remember(displayedYear, displayedMonth) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, displayedYear)
            set(Calendar.MONTH, displayedMonth)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDayDow = cal.get(Calendar.DAY_OF_WEEK)
        val startEmptySlots = (firstDayDow + 5) % 7

        val list = mutableListOf<CalendarDayItem?>()
        for (i in 0 until startEmptySlots) {
            list.add(null)
        }
        for (day in 1..daysInMonth) {
            val key = String.format(Locale.US, "%04d-%02d-%02d", displayedYear, displayedMonth + 1, day)
            list.add(
                CalendarDayItem(
                    dayNumber = day,
                    dateKey = key,
                    isToday = (displayedYear == todayYear && displayedMonth == todayMonth && day == todayDay)
                )
            )
        }
        list
    }

    val selectedDaySessions = sessionsByDate[selectedDateKey] ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .testTag("calendar_screen"),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Top Month Title & Navigation Buttons
        item(key = "calendar_top_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = buildAnnotatedString {
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onBackground,
                                fontWeight = FontWeight.ExtraBold
                            )
                        ) {
                            append(monthNamesUk[displayedMonth])
                        }
                        append(" ")
                        withStyle(
                            SpanStyle(
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        ) {
                            append("$displayedYear")
                        }
                    },
                    fontSize = 28.sp
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                if (displayedMonth == 0) {
                                    displayedMonth = 11
                                    displayedYear -= 1
                                } else {
                                    displayedMonth -= 1
                                }
                            }
                            .testTag("prev_month_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronLeft,
                                contentDescription = "Попередній місяць",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                if (displayedMonth == 11) {
                                    displayedMonth = 0
                                    displayedYear += 1
                                } else {
                                    displayedMonth += 1
                                }
                            }
                            .testTag("next_month_btn")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Наступний місяць",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. Three Month Stat Cards (виконано, заплановано, у залі)
        item(key = "calendar_stats_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "$completedInMonthCount",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = IceCyanPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "виконано",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "$plannedInMonthCount",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WarningAmber
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "заплановано",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.weight(1.1f)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = if (totalMinutesMonth >= 60) {
                                "${totalMinutesMonth / 60}г ${totalMinutesMonth % 60}хв"
                            } else {
                                "$totalMinutesMonth хв"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "у залі",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        // 3. Calendar Grid Card + Legend
        item(key = "calendar_grid_card") {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Weekday headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        weekdayHeaders.forEach { dayName ->
                            Text(
                                text = dayName,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Days Grid
                    val rows = calendarDays.chunked(7)
                    rows.forEach { week ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            for (i in 0 until 7) {
                                val item = week.getOrNull(i)
                                if (item != null) {
                                    val isSelected = item.dateKey == selectedDateKey
                                    val daySessions = sessionsByDate[item.dateKey] ?: emptyList()
                                    val hasCompleted = daySessions.any { it.isCompleted }
                                    val hasPlanned = daySessions.any { !it.isCompleted }

                                    val cellBg = when {
                                        hasCompleted -> IceCyanContainer
                                        isSelected -> MaterialTheme.colorScheme.surfaceVariant
                                        else -> Color.Transparent
                                    }
                                    val cellBorderColor = when {
                                        hasCompleted && isSelected -> IceCyanPrimary
                                        hasCompleted -> IceCyanPrimary.copy(alpha = 0.7f)
                                        isSelected -> IceCyanPrimary.copy(alpha = 0.5f)
                                        else -> Color.Transparent
                                    }
                                    val textColor = when {
                                        hasCompleted -> IceCyanPrimary
                                        isSelected -> MaterialTheme.colorScheme.onSurface
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .padding(2.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(cellBg)
                                            .border(
                                                width = if (cellBorderColor != Color.Transparent) 1.2.dp else 0.dp,
                                                color = cellBorderColor,
                                                shape = RoundedCornerShape(10.dp)
                                            )
                                            .clickable { selectedDateKey = item.dateKey }
                                            .testTag("calendar_day_${item.dateKey}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = "${item.dayNumber}",
                                                fontSize = 13.sp,
                                                fontWeight = if (hasCompleted || isSelected) FontWeight.Bold else FontWeight.Medium,
                                                color = textColor
                                            )

                                            if (hasCompleted || hasPlanned) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                                                    if (hasCompleted) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(IceCyanPrimary)
                                                        )
                                                    }
                                                    if (hasPlanned) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(4.dp)
                                                                .clip(CircleShape)
                                                                .background(WarningAmber)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))
                    Spacer(modifier = Modifier.height(10.dp))

                    // Legend Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(IceCyanPrimary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "виконано",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(WarningAmber)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "заплановано",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // 4. Selected Day Details Header & "+ Запланувати" Button
        item(key = "selected_day_header") {
            val formattedSelectedDate = remember(selectedDateKey) {
                formatDateKeyToUk(selectedDateKey)
            }
            val countLabel = when (selectedDaySessions.size) {
                0 -> "Немає занять"
                1 -> "1 заняття"
                2, 3, 4 -> "${selectedDaySessions.size} заняття"
                else -> "${selectedDaySessions.size} занять"
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formattedSelectedDate,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = countLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IceCyanContainer,
                    border = BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.5f)),
                    modifier = Modifier
                        .clickable { showScheduleDialog = true }
                        .testTag("schedule_workout_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = IceCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Запланувати",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceCyanPrimary
                        )
                    }
                }
            }
        }

        // 5. Sessions List for Selected Day
        if (selectedDaySessions.isNotEmpty()) {
            items(selectedDaySessions, key = { it.id }) { session ->
                var menuExpanded by remember { mutableStateOf(false) }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("session_card_${session.id}")
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (session.isCompleted) IceCyanContainer else WarningAmberContainer,
                                    modifier = Modifier.size(38.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = if (session.isCompleted) Icons.Default.Check else Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = if (session.isCompleted) IceCyanPrimary else WarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = session.title,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    if (session.isCompleted) {
                                        val durationMin = (session.durationSeconds / 60).coerceAtLeast(1)
                                        Text(
                                            text = "Виконано · $durationMin хв",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = IceCyanPrimary
                                        )
                                    } else {
                                        Text(
                                            text = "Заплановане тренування",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = WarningAmber
                                        )
                                    }
                                }
                            }

                            Box {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clickable { menuExpanded = true }
                                        .testTag("delete_session_btn_${session.id}")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MoreHoriz,
                                            contentDescription = "Меню тренування",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = menuExpanded,
                                    onDismissRequest = { menuExpanded = false }
                                ) {
                                    if (session.isCompleted) {
                                        DropdownMenuItem(
                                            text = { Text("Повторити тренування") },
                                            leadingIcon = { Icon(Icons.Default.Replay, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                viewModel.repeatWorkout(session.id)
                                            }
                                        )
                                    } else {
                                        DropdownMenuItem(
                                            text = { Text("Розпочати зараз") },
                                            leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                            onClick = {
                                                menuExpanded = false
                                                onStartWorkout(session.id)
                                            }
                                        )
                                    }
                                    DropdownMenuItem(
                                        text = { Text("Видалити", color = MaterialTheme.colorScheme.error) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        },
                                        onClick = {
                                            menuExpanded = false
                                            viewModel.deleteSession(session.id)
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (session.isCompleted) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = IceCyanContainer,
                                border = BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.repeatWorkout(session.id) }
                                    .testTag("repeat_calendar_session_${session.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 10.dp),
                                    horizontalArrangement = Arrangement.Center,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Replay,
                                        contentDescription = null,
                                        tint = IceCyanPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Повторити тренування",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IceCyanPrimary
                                    )
                                }
                            }
                        } else {
                            Button(
                                onClick = { onStartWorkout(session.id) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IceCyanPrimary,
                                    contentColor = IceCyanOnPrimary
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(42.dp)
                                    .testTag("start_planned_session_${session.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Розпочати тренування",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        } else {
            item(key = "empty_day_sessions") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "На цей день тренувань не заплановано",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    if (showScheduleDialog) {
        ScheduleWorkoutModal(
            dateKey = selectedDateKey,
            userSessions = allSessions,
            onSchedule = { title, sourceSessionId, selectedTime, recurrence ->
                val parts = selectedDateKey.split("-").mapNotNull { it.toIntOrNull() }
                val timestamp = if (parts.size == 3) {
                    Calendar.getInstance().apply {
                        set(Calendar.YEAR, parts[0])
                        set(Calendar.MONTH, parts[1] - 1)
                        set(Calendar.DAY_OF_MONTH, parts[2])
                        if (selectedTime != null) {
                            val tParts = selectedTime.split(":").mapNotNull { it.toIntOrNull() }
                            if (tParts.size == 2) {
                                set(Calendar.HOUR_OF_DAY, tParts[0])
                                set(Calendar.MINUTE, tParts[1])
                            } else {
                                set(Calendar.HOUR_OF_DAY, 12)
                                set(Calendar.MINUTE, 0)
                            }
                        } else {
                            set(Calendar.HOUR_OF_DAY, 12)
                            set(Calendar.MINUTE, 0)
                        }
                        set(Calendar.SECOND, 0)
                    }.timeInMillis
                } else System.currentTimeMillis()

                val timeNotes = if (selectedTime != null) "Час: $selectedTime" else ""

                if (sourceSessionId != null) {
                    viewModel.scheduleWorkoutFromSession(
                        sourceSessionId = sourceSessionId,
                        dateTimestamp = timestamp,
                        customTitle = title,
                        notes = timeNotes,
                        recurrence = recurrence
                    )
                } else {
                    viewModel.scheduleWorkout(
                        title = title,
                        dateTimestamp = timestamp,
                        notes = timeNotes,
                        recurrence = recurrence
                    )
                }
                showScheduleDialog = false
            },
            onDismiss = { showScheduleDialog = false }
        )
    }
}

private data class CalendarDayItem(
    val dayNumber: Int,
    val dateKey: String,
    val isToday: Boolean
)

private fun formatDateKeyToUk(dateKey: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(dateKey) ?: return dateKey
        val formatter = SimpleDateFormat("EEEE, d MMMM", Locale("uk", "UA"))
        formatter.format(date).replaceFirstChar { it.uppercase() }
    } catch (e: Exception) {
        dateKey
    }
}

@Composable
fun ScheduleWorkoutModal(
    dateKey: String,
    userSessions: List<WorkoutSessionEntity>,
    onSchedule: (title: String, sourceSessionId: Long?, selectedTime: String?, recurrence: WorkoutRecurrence) -> Unit,
    onDismiss: () -> Unit
) {
    var workoutTitle by remember { mutableStateOf("") }
    var selectedSessionId by remember { mutableStateOf<Long?>(null) }
    var selectedTime by remember { mutableStateOf<String?>(null) }
    var selectedRecurrence by remember { mutableStateOf(WorkoutRecurrence.NONE) }

    val userCustomWorkouts = remember(userSessions) {
        userSessions.filter { it.isCompleted }.distinctBy { it.title }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("schedule_modal")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Запланувати тренування",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Text(
                    text = formatDateKeyToUk(dateKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = IceCyanPrimary,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(14.dp))

                if (userCustomWorkouts.isNotEmpty()) {
                    Text(
                        text = "Оберіть зі збережених тренувань:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 160.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        userCustomWorkouts.take(5).forEach { session ->
                            val isSelected = selectedSessionId == session.id
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) IceCyanContainer else MaterialTheme.colorScheme.surfaceVariant,
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.outline
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedSessionId = session.id
                                        workoutTitle = session.title
                                    }
                                    .testTag("select_free_workout_${session.id}")
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = session.title,
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (isSelected) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = IceCyanPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = workoutTitle,
                    onValueChange = {
                        workoutTitle = it
                        selectedSessionId = null
                    },
                    label = { Text("Назва тренування") },
                    placeholder = { Text("Силове тренування") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("schedule_title_input")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Час тренування:",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val quickTimes = listOf("07:00", "09:00", "12:00", "17:00", "18:30", "20:00")
                    quickTimes.forEach { time ->
                        val isChosen = selectedTime == time
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isChosen) IceCyanContainer else MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(
                                1.dp,
                                if (isChosen) IceCyanPrimary else MaterialTheme.colorScheme.outline
                            ),
                            modifier = Modifier.clickable {
                                selectedTime = if (isChosen) null else time
                            }
                        ) {
                            Text(
                                text = time,
                                fontSize = 11.sp,
                                fontWeight = if (isChosen) FontWeight.Bold else FontWeight.Normal,
                                color = if (isChosen) IceCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = {
                        val finalTitle = workoutTitle.trim().ifEmpty { "Силове тренування" }
                        onSchedule(finalTitle, selectedSessionId, selectedTime, selectedRecurrence)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("confirm_schedule_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Запланувати",
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
