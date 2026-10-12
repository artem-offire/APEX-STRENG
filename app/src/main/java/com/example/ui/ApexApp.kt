package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.screens.*
import com.example.ui.theme.IceCyanContainer
import com.example.ui.theme.IceCyanPrimary
import kotlinx.coroutines.launch

enum class ApexTab(
    val titleUk: String,
    val icon: ImageVector,
    val testTag: String
) {
    TRAINING("Тренування", Icons.Default.FitnessCenter, "tab_training"),
    CALENDAR("Календар", Icons.Default.CalendarMonth, "tab_calendar"),
    PROFILE("Профіль", Icons.Default.Person, "tab_profile")
}

@Composable
fun ApexApp(viewModel: ApexViewModel) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { ApexTab.values().size })
    val coroutineScope = rememberCoroutineScope()
    val activeWorkoutSessionId by viewModel.activeWorkoutSessionId.collectAsState()

    if (activeWorkoutSessionId != null) {
        ActiveWorkoutScreen(
            sessionId = activeWorkoutSessionId!!,
            viewModel = viewModel,
            onFinishOrClose = {
                coroutineScope.launch {
                    pagerState.scrollToPage(0)
                }
            }
        )
    } else {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        HorizontalDivider(
                            thickness = 1.dp,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ApexTab.values().forEachIndexed { index, tab ->
                                val isSelected = pagerState.currentPage == index
                                val activeBg = if (isSelected) IceCyanContainer else Color.Transparent
                                val contentColor = if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.onSurfaceVariant

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 6.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(activeBg)
                                        .clickable {
                                            coroutineScope.launch {
                                                pagerState.animateScrollToPage(index)
                                            }
                                        }
                                        .padding(vertical = 8.dp, horizontal = 12.dp)
                                        .testTag(tab.testTag),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = tab.icon,
                                            contentDescription = tab.titleUk,
                                            tint = contentColor,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = tab.titleUk,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = contentColor
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                HorizontalPager(
                    state = pagerState,
                    beyondViewportPageCount = 1,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    when (ApexTab.values()[page]) {
                        ApexTab.TRAINING -> TrainingTab(
                            viewModel = viewModel,
                            onStartWorkout = { sessionId ->
                                viewModel.startWorkout(sessionId)
                            },
                            onOpenPrograms = {},
                            onOpenCalendar = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(ApexTab.CALENDAR.ordinal)
                                }
                            }
                        )
                        ApexTab.CALENDAR -> CalendarTab(
                            viewModel = viewModel,
                            onStartWorkout = { sessionId ->
                                viewModel.startWorkout(sessionId)
                            }
                        )
                        ApexTab.PROFILE -> ProfileTab(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
