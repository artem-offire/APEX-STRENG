package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.model.Achievement
import com.example.model.AthleteGamificationState
import com.example.ui.theme.*

@Composable
fun AthleteGamificationCard(
    state: AthleteGamificationState,
    nickname: String = "Атлет",
    showLevelInfo: Boolean = true,
    onUpdateNickname: ((String) -> Unit)? = null,
    onViewAchievements: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showEditNicknameDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("athlete_gamification_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header: Level/Avatar, Rank & Streak
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showLevelInfo) {
                        Surface(
                            shape = CircleShape,
                            color = IceCyanContainer,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyanPrimary),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "L${state.level}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    color = IceCyanPrimary
                                )
                            }
                        }
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = IceCyanContainer,
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyanPrimary),
                            modifier = Modifier.size(48.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Default.FitnessCenter,
                                    contentDescription = null,
                                    tint = IceCyanPrimary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val displayName = if (nickname.isNotBlank() && nickname != "Атлет") nickname else "Введіть нікнейм"
                            Text(
                                text = displayName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.clickable {
                                    if (onUpdateNickname != null) showEditNicknameDialog = true
                                }
                            )
                            if (onUpdateNickname != null) {
                                Spacer(modifier = Modifier.width(4.dp))
                                IconButton(
                                    onClick = { showEditNicknameDialog = true },
                                    modifier = Modifier.size(24.dp).testTag("edit_nickname_gamification_btn")
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Ввести нікнейм",
                                        tint = IceCyanPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }
                        if (showLevelInfo) {
                            Text(
                                text = "${state.rankTitle} • ${state.currentXp} XP всього",
                                style = MaterialTheme.typography.bodySmall,
                                color = IceCyanPrimary,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Streak pill
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (state.currentStreak > 0) WarningAmber.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (state.currentStreak > 0) WarningAmber.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (state.currentStreak > 0) "🔥 ${state.currentStreak} в серії" else "🔥 0 днів",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.currentStreak > 0) WarningAmber else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (showLevelInfo) {
                Spacer(modifier = Modifier.height(14.dp))

                // Enhanced Level Progress Scale Section with graduations & remaining XP
                LevelProgressScaleSection(state = state)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Stats grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(
                    title = "ТРЕНУВАНЬ",
                    value = "${state.totalWorkouts}",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "ПІДХОДІВ",
                    value = "${state.totalSets}",
                    modifier = Modifier.weight(1f)
                )
                StatPill(
                    title = "СТРІК",
                    value = "${state.currentStreak} дн",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Achievements summary trigger
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewAchievements() }
                    .testTag("open_achievements_gallery_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = "🏆", fontSize = 18.sp)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Досягнення та трофеї",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${state.unlockedCount} / ${state.achievements.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showEditNicknameDialog && onUpdateNickname != null) {
        EditNicknameDialog(
            currentNickname = nickname,
            onSave = { onUpdateNickname(it) },
            onDismiss = { showEditNicknameDialog = false }
        )
    }
}

@Composable
private fun StatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun LevelProgressScaleSection(
    state: AthleteGamificationState,
    modifier: Modifier = Modifier
) {
    val xpLeft = (state.nextLevelXp - state.currentXp).coerceAtLeast(0)
    val levelTotalXp = (state.nextLevelXp - state.levelBaseXp).coerceAtLeast(1)
    val currentLevelXp = (state.currentXp - state.levelBaseXp).coerceAtLeast(0)
    val percentInt = (state.levelProgressPercent * 100).toInt().coerceIn(0, 100)
    val workoutsLeft = ((xpLeft + 149) / 175).coerceAtLeast(1)

    Column(modifier = modifier.fillMaxWidth()) {
        // Top row: Current Level & XP vs Next Level Target
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = IceCyanPrimary.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.4f))
                ) {
                    Text(
                        text = "Рівень ${state.level}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = IceCyanPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${currentLevelXp} / ${levelTotalXp} XP",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Ціль: Рівень ${state.level + 1}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "🏁", fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Custom Progress Track with Visual Milestones (Шкала)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Background track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        RoundedCornerShape(7.dp)
                    )
            ) {
                // Filled gradient bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = state.levelProgressPercent.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(7.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    IceCyanPrimary.copy(alpha = 0.85f),
                                    Color(0xFF00E5FF)
                                )
                            )
                        )
                )

                // Scale tick markers inside the track (25%, 50%, 75%)
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(0.25f, 0.50f, 0.75f).forEach { fraction ->
                        Box(
                            modifier = Modifier
                                .width(1.5.dp)
                                .height(8.dp)
                                .background(
                                    if (state.levelProgressPercent >= fraction)
                                        Color.White.copy(alpha = 0.6f)
                                    else
                                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
            }

            // Floating progress thumb / percentage pill
            Row(
                modifier = Modifier
                    .fillMaxWidth(fraction = state.levelProgressPercent.coerceIn(0.12f, 0.96f)),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IceCyanPrimary,
                    shadowElevation = 3.dp,
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, Color.White)
                ) {
                    Text(
                        text = "$percentInt%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = IceCyanOnPrimary,
                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Scale graduation labels (Шкала значень XP)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${state.levelBaseXp} XP",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "25%",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "50%",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "75%",
                fontSize = 9.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
            Text(
                text = "${state.nextLevelXp} XP",
                fontSize = 9.sp,
                color = IceCyanPrimary,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Prominent Remaining XP Card ("Скільки залишилося")
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = IceCyanContainer,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = IceCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Залишилося до підвищення:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "≈ $workoutsLeft ${if (workoutsLeft == 1) "тренування" else if (workoutsLeft in 2..4) "тренування" else "тренувань"} за поточного темпу",
                            fontSize = 10.sp,
                            color = SuccessGreen,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = IceCyanContainer
                ) {
                    Text(
                        text = "$xpLeft XP",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Black,
                        color = IceCyanPrimary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun GamificationBanner(
    state: AthleteGamificationState,
    nickname: String = "Атлет",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showHelpDialog by remember { mutableStateOf(false) }
    val xpLeft = (state.nextLevelXp - state.currentXp).coerceAtLeast(0)
    val percentInt = (state.levelProgressPercent * 100).toInt().coerceIn(0, 100)

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("gamification_banner")
    ) {
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = IceCyanContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "L${state.level}",
                                fontWeight = FontWeight.Black,
                                fontSize = 12.sp,
                                color = IceCyanPrimary
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        val headerText = if (nickname.isNotBlank() && nickname != "Атлет") nickname else "Введіть нікнейм"
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.currentXp} XP • $percentInt% до Рівня ${state.level + 1}",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IceCyanContainer.copy(alpha = 0.5f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = "ще $xpLeft XP",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceCyanPrimary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    if (state.currentStreak > 0) {
                        Text(
                            text = "🔥 ${state.currentStreak}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = WarningAmber
                        )
                    }

                    IconButton(
                        onClick = { showHelpDialog = true },
                        modifier = Modifier.size(24.dp).testTag("gamification_info_btn")
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "Що це таке",
                            tint = IceCyanPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini Scale Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = state.levelProgressPercent.coerceIn(0.02f, 1f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(IceCyanPrimary, Color(0xFF00E5FF))
                            )
                        )
                )
            }
        }
    }

    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = IceCyanPrimary)
            },
            title = {
                Text("Що це таке: Рівень атлета та XP", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Це автономна ігрова система мотивації силового прогресу:\n\n" +
                            "• Рівень (L${state.level}): Показує ваш поточний ранг. З кожним тренуванням ранг підвищується.\n" +
                            "• Досвід (XP): Нараховується автоматично за кожен виконаний робочий підхід у залі.\n" +
                            "• Серія (стрік 🔥): Показує кількість тренувальних днів поспіль без пропусків.\n" +
                            "• Досягнення: Натисніть на банер, щоб відкрити список трофеїв та відкритих силових нагород.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(onClick = { showHelpDialog = false }) {
                    Text("Зрозуміло")
                }
            }
        )
    }
}

@Composable
fun AchievementsModal(
    achievements: List<Achievement>,
    totalXp: Int,
    onDismiss: () -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }

    val filteredList = remember(selectedFilter, achievements) {
        when (selectedFilter) {
            "UNLOCKED" -> achievements.filter { it.isUnlocked }
            "IN_PROGRESS" -> achievements.filter { !it.isUnlocked }
            else -> achievements
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (achievements.size <= 1) Modifier.wrapContentHeight()
                    else Modifier.fillMaxHeight(0.85f)
                )
                .padding(vertical = 12.dp)
                .testTag("achievements_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Досягнення атлета",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (achievements.size == 1) {
                                val item = achievements.first()
                                if (item.isUnlocked) "Отримано (1 з 1) • $totalXp XP"
                                else "Прогрес: ${item.currentProgress}/${item.maxProgress} • $totalXp XP"
                            } else {
                                "Відкрито ${achievements.count { it.isUnlocked }} з ${achievements.size} • $totalXp XP"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = IceCyanPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                if (achievements.size > 1) {
                    Spacer(modifier = Modifier.height(12.dp))

                    // Filter buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedFilter == "ALL",
                            onClick = { selectedFilter = "ALL" },
                            label = { Text("Всі (${achievements.size})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "UNLOCKED",
                            onClick = { selectedFilter = "UNLOCKED" },
                            label = { Text("Отримані (${achievements.count { it.isUnlocked }})", fontSize = 11.sp) }
                        )
                        FilterChip(
                            selected = selectedFilter == "IN_PROGRESS",
                            onClick = { selectedFilter = "IN_PROGRESS" },
                            label = { Text("В процесі (${achievements.count { !it.isUnlocked }})", fontSize = 11.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                } else {
                    Spacer(modifier = Modifier.height(16.dp))
                }

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = if (achievements.size <= 1) Modifier.wrapContentHeight() else Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { item ->
                        AchievementItemCard(achievement = item)
                    }
                }
            }
        }
    }
}

@Composable
fun AchievementItemCard(achievement: Achievement) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (achievement.isUnlocked) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (achievement.isUnlocked) SuccessGreen.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = Modifier.fillMaxWidth().testTag("achievement_card_${achievement.id}")
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = if (achievement.isUnlocked) SuccessGreen.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (achievement.isUnlocked) SuccessGreen else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(text = achievement.iconEmoji, fontSize = 18.sp)
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = achievement.title,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (achievement.isUnlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = achievement.description,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (achievement.isUnlocked) SuccessGreen.copy(alpha = 0.15f) else IceCyanContainer
                ) {
                    Text(
                        text = "+${achievement.xpReward} XP",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (achievement.isUnlocked) SuccessGreen else IceCyanPrimary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Progress Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LinearProgressIndicator(
                    progress = { achievement.progressPercent },
                    modifier = Modifier
                        .weight(1f)
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = if (achievement.isUnlocked) SuccessGreen else IceCyanPrimary,
                    trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Text(
                    text = if (achievement.isUnlocked) "Виконано ✓" else "${achievement.currentProgress} / ${achievement.maxProgress}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (achievement.isUnlocked) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun WorkoutCompletedCelebrationDialog(
    earnedXp: Int,
    state: AthleteGamificationState,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .testTag("workout_celebration_dialog")
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "🎉", fontSize = 42.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Тренування завершено!",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "+$earnedXp XP зароблено",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = SuccessGreen
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Рівень ${state.level}: ${state.rankTitle}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { state.levelProgressPercent },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = IceCyanPrimary,
                            trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${state.currentXp} XP (ще ${(state.nextLevelXp - state.currentXp).coerceAtLeast(0)} XP до наступного рівня)",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (state.currentStreak > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "🔥 Серія триває: ${state.currentStreak} занять!",
                        fontWeight = FontWeight.Bold,
                        color = WarningAmber,
                        fontSize = 13.sp
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("celebration_continue_btn"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Чудово!", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
