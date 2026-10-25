package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import com.example.model.MuscleGroup
import com.example.model.MuscleHead
import com.example.model.ScienceArticle
import com.example.model.ScienceKnowledgeBase
import com.example.service.RestTimerAudioHelper
import com.example.service.SetCompleteAudioHelper
import com.example.ui.ApexViewModel
import com.example.ui.components.AchievementsModal
import com.example.ui.components.ExercisePickerDialog
import com.example.ui.components.RestTimerSoundDialog
import com.example.ui.components.ScienceArticleModal
import com.example.ui.components.ScientificVolumeModal
import com.example.ui.components.SetCompleteSoundDialog
import com.example.ui.theme.*

@Composable
fun ProfileTab(viewModel: ApexViewModel) {
    val context = LocalContext.current

    val volumeConfig by viewModel.userVolumeConfig.collectAsState()
    val isDarkTheme by viewModel.isDarkThemeOverride.collectAsState()
    val userNickname by viewModel.userNickname.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val gamificationState by viewModel.gamificationState.collectAsState()
    val readScienceArticleIds by viewModel.readScienceArticleIds.collectAsState()
    val advancedMuscleMode by viewModel.advancedMuscleMode.collectAsState()

    var showVolumeModal by remember { mutableStateOf(false) }
    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showManageExercisesModal by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var currentSound by remember { mutableStateOf(RestTimerAudioHelper.getSelectedSound(context)) }
    var showSetSoundDialog by remember { mutableStateOf(false) }
    var currentSetSound by remember { mutableStateOf(SetCompleteAudioHelper.getSelectedSound(context)) }
    var selectedArticle by remember { mutableStateOf<ScienceArticle?>(null) }
    var showAchievementsModal by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        // 1. Page Title
        item(key = "profile_title_section") {
            Text(
                text = "Профіль",
                style = ApexScreenTitle,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        // 2. User Profile Card
        item(key = "user_profile_card") {
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("user_profile_card")
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = IceCyanContainer,
                            border = BorderStroke(1.dp, IceCyanContainerBorder),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = userNickname.trim().take(1).uppercase().ifEmpty { "А" },
                                    fontFamily = Unbounded,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp,
                                    color = IceCyanPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = userNickname,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Рівень ${gamificationState.level} · ${gamificationState.currentXp} XP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IceCyanPrimary
                                )
                            }
                            if (readScienceArticleIds.size >= 6) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFF59E0B).copy(alpha = 0.15f),
                                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                                    modifier = Modifier
                                        .clickable { showAchievementsModal = true }
                                        .testTag("science_master_profile_badge")
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                    ) {
                                        Text(
                                            text = "🎓 Знавець теорії",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFF59E0B)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable { showAchievementsModal = true }
                                .testTag("open_achievements_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.EmojiEvents,
                                    contentDescription = "Досягнення",
                                    tint = WarningAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = MaterialTheme.colorScheme.background,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .size(44.dp)
                                .clickable {
                                    nicknameInput = userNickname
                                    showEditNicknameDialog = true
                                }
                                .testTag("open_edit_nickname_btn")
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редагувати ім'я",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. Section: ТРЕНУВАННЯ
        item(key = "section_training") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ТРЕНУВАННЯ",
                    style = ApexSectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("manage_exercises_card")
                ) {
                    Column {
                        // Row 1: База вправ
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showManageExercisesModal = true }
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("open_manage_exercises_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = IceCyanContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MenuBook,
                                            contentDescription = null,
                                            tint = IceCyanPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "База вправ",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${allExercises.size} вправ у каталозі",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 62.dp)
                        )

                        // Row 2: Фактори відновлення
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showVolumeModal = true }
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("open_volume_settings_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = IceCyanContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Tune,
                                            contentDescription = null,
                                            tint = IceCyanPrimary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Фактори відновлення",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Сон, харчування, стрес, м'язи",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 62.dp)
                        )

                        // Row 3: Продвинутий режим (поділ мʼязів на пучки)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .toggleable(
                                    value = advancedMuscleMode,
                                    role = Role.Switch,
                                    onValueChange = { viewModel.setAdvancedMuscleMode(it) }
                                )
                                .heightIn(min = 72.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("advanced_muscle_mode_toggle"),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (advancedMuscleMode) IceCyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.AccountTree,
                                        contentDescription = null,
                                        tint = if (advancedMuscleMode) IceCyanOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Продвинутий режим",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Детальний поділ мʼязів на пучки",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (advancedMuscleMode) IceCyanPrimary else MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    text = if (advancedMuscleMode) "Увімкнено" else "Вимкнено",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (advancedMuscleMode) IceCyanOnPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))
                if (advancedMuscleMode) {
                    AdvancedMuscleModeInfoCard()
                } else {
                    Text(
                        text = "Зараз мʼязи обираються загальними групами: «Дельти», «Біцепс», «Спина». Увімкніть продвинутий режим, щоб розділити їх на окремі пучки.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }

        // Section: НАУКОВА БАЗА ЗНАНЬ
        item(key = "section_science_knowledge_base") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "НАУКОВА БАЗА ЗНАНЬ",
                        style = ApexSectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (readScienceArticleIds.size >= 6) Color(0xFFF59E0B).copy(alpha = 0.15f) else IceCyanContainer,
                        border = BorderStroke(1.dp, if (readScienceArticleIds.size >= 6) Color(0xFFF59E0B).copy(alpha = 0.5f) else IceCyanContainerBorder)
                    ) {
                        Text(
                            text = if (readScienceArticleIds.size >= 6) "🎓 6/6 · Знавець теорії" else "🎓 ${readScienceArticleIds.size}/6 вивчено",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (readScienceArticleIds.size >= 6) Color(0xFFF59E0B) else IceCyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (readScienceArticleIds.size >= 6) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF59E0B).copy(alpha = 0.12f)),
                        border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAchievementsModal = true }
                            .testTag("science_master_banner")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "🎓", fontSize = 28.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Бейдж «Знавець теорії» здобуто!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp,
                                    color = Color(0xFFF59E0B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Ви вивчили всі 6 наукових принципів (+300 XP бонус та особливий статус).",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("science_articles_card")
                ) {
                    Column {
                        ScienceKnowledgeBase.articles.forEachIndexed { index, article ->
                            val isArticleRead = readScienceArticleIds.contains(article.id)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedArticle = article }
                                    .heightIn(min = 68.dp)
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                                    .testTag("open_article_${article.id}"),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    val iconVector = when (article.iconName) {
                                        "TrendingUp" -> Icons.Default.TrendingUp
                                        "Speed" -> Icons.Default.Speed
                                        "HourglassBottom" -> Icons.Default.HourglassBottom
                                        "Assessment" -> Icons.Default.Assessment
                                        "BatteryChargingFull" -> Icons.Default.BatteryChargingFull
                                        else -> Icons.Default.AutoMode
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isArticleRead) Color(0xFF10B981).copy(alpha = 0.15f) else IceCyanContainer,
                                        border = if (isArticleRead) BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)) else null,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = if (isArticleRead) Icons.Default.Check else iconVector,
                                                contentDescription = null,
                                                tint = if (isArticleRead) Color(0xFF10B981) else IceCyanPrimary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = article.titleUk,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = article.subtitleUk,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    if (isArticleRead) {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.15f),
                                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.35f))
                                        ) {
                                            Text(
                                                text = "✓ Прочитано",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF10B981),
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant
                                        ) {
                                            Text(
                                                text = "+50 XP",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = null,
                                        tint = TextMutedDark,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            if (index < ScienceKnowledgeBase.articles.size - 1) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.padding(start = 62.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Section: ЗВУКИ
        item(key = "section_sounds") {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ЗВУКИ",
                    style = ApexSectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_complete_sound_card")
                ) {
                    Column {
                        // Row 1: Завершення підходу
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSetSoundDialog = true }
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("select_set_sound_profile_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WarningAmberContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Notifications,
                                            contentDescription = null,
                                            tint = WarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Завершення підходу",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${currentSetSound.titleUk} · ${currentSetSound.descriptionUk}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(start = 62.dp)
                        )

                        // Row 2: Таймер відпочинку
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showSoundDialog = true }
                                .heightIn(min = 64.dp)
                                .padding(horizontal = 14.dp, vertical = 12.dp)
                                .testTag("select_rest_sound_profile_btn"),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = WarningAmberContainer,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = null,
                                            tint = WarningAmber,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Таймер відпочинку",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${currentSound.titleUk} · ${currentSound.descriptionUk}",
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = TextMutedDark,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }

        // 5. Section: ТЕМА
        item(key = "section_theme") {
            val isDark = isDarkTheme != false
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "ТЕМА",
                    style = ApexSectionLabel,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("theme_selection_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { viewModel.toggleDarkMode() }
                            .heightIn(min = 64.dp)
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isDark) Color(0xFF1B2330) else Color(0xFFFFF4D6),
                                modifier = Modifier.size(36.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isDark) Icons.Default.DarkMode else Icons.Default.LightMode,
                                        contentDescription = null,
                                        tint = if (isDark) Color(0xFFA5B4FC) else Color(0xFFD99A0B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = if (isDark) "Темна тема" else "Світла тема",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Switch(
                            checked = isDark,
                            onCheckedChange = { viewModel.toggleDarkMode() },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = IceCyanOnPrimary,
                                checkedTrackColor = IceCyanPrimary,
                                uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                uncheckedTrackColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.testTag("theme_dark_switch")
                        )
                    }
                }
            }
        }

        // 6. Danger Zone: Reset All Progress
        item(key = "reset_progress_card") {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = ErrorRedContainer,
                border = BorderStroke(1.dp, ErrorRedBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "НЕБЕЗПЕЧНА ЗОНА",
                        style = ApexSectionLabel,
                        color = ErrorRed
                    )
                    Text(
                        text = "Видалить усі тренування, підходи та налаштування без можливості відновлення.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedButton(
                        onClick = { showResetConfirmDialog = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, ErrorRedStrongBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        modifier = Modifier
                            .padding(top = 6.dp)
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("reset_all_progress_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Скинути весь прогрес",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }

    if (showVolumeModal) {
        ScientificVolumeModal(
            currentConfig = volumeConfig,
            onSaveConfig = { exp, sleep, nut, stress, priorities ->
                viewModel.saveVolumeSettings(exp, sleep, nut, stress, priorities)
            },
            onDismiss = { showVolumeModal = false }
        )
    }

    if (showEditNicknameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNicknameDialog = false },
            title = {
                Text(
                    text = "Зміна імені користувача",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Column(modifier = Modifier.imePadding()) {
                    Text(
                        text = "Введіть ваше ім'я або псевдонім (до 25 символів):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nicknameInput,
                        onValueChange = { if (it.length <= 25) nicknameInput = it },
                        singleLine = true,
                        placeholder = { Text("Наприклад: Атлет") },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                val clean = nicknameInput.trim().ifEmpty { "Атлет" }
                                viewModel.setUserNickname(clean)
                                showEditNicknameDialog = false
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("nickname_edit_field")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = nicknameInput.trim().ifEmpty { "Атлет" }
                        viewModel.setUserNickname(clean)
                        showEditNicknameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    modifier = Modifier.testTag("save_nickname_confirm_btn")
                ) {
                    Text("Зберегти", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditNicknameDialog = false },
                    modifier = Modifier.testTag("cancel_nickname_edit_btn")
                ) {
                    Text("Скасувати")
                }
            }
        )
    }

    if (showResetConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showResetConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Warning,
                    contentDescription = null,
                    tint = ErrorRed,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Скинути весь прогрес?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            },
            text = {
                Text(
                    text = "Ви впевнені, що хочете видалити всі завершені тренування та історію ваг? Цю дію неможливо скасувати.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetAllProgress {
                            Toast.makeText(
                                context,
                                "Весь прогрес тренувань успішно скинуто",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        showResetConfirmDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ErrorRed,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_reset_all_progress_btn")
                ) {
                    Text("Так, скинути все", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmDialog = false },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Скасувати")
                }
            },
            shape = RoundedCornerShape(18.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    if (showSoundDialog) {
        RestTimerSoundDialog(
            currentSound = currentSound,
            onSoundSelected = {
                currentSound = it
                RestTimerAudioHelper.setSelectedSound(context, it)
            },
            onDismiss = { showSoundDialog = false }
        )
    }

    if (showSetSoundDialog) {
        SetCompleteSoundDialog(
            currentSound = currentSetSound,
            onSoundSelected = {
                currentSetSound = it
                SetCompleteAudioHelper.setSelectedSound(context, it)
            },
            onDismiss = { showSetSoundDialog = false }
        )
    }

    if (showManageExercisesModal) {
        ExercisePickerDialog(
            allExercises = allExercises,
            onSelectExercise = { _, _, _ -> },
            onDeleteExercise = { ex -> viewModel.deleteExercise(ex.id) },
            onDismiss = { showManageExercisesModal = false }
        )
    }

    if (selectedArticle != null) {
        val article = selectedArticle!!
        ScienceArticleModal(
            article = article,
            isRead = readScienceArticleIds.contains(article.id),
            onMarkRead = {
                viewModel.markScienceArticleRead(article.id)
            },
            onDismiss = { selectedArticle = null }
        )
    }

    if (showAchievementsModal) {
        AchievementsModal(
            achievements = gamificationState.achievements,
            totalXp = gamificationState.currentXp,
            onDismiss = { showAchievementsModal = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AdvancedMuscleModeInfoCard() {
    val isDark = apexIsDark()
    val heads = { group: MuscleGroup -> MuscleHead.headsFor(group).map { it.displayNameUk } }
    val armHeads = MuscleHead.headsFor(MuscleGroup.BICEPS).map {
        if (it == MuscleHead.BRACHIALIS) it.displayNameUk else "Біцепс: ${it.displayNameUk.lowercase()}"
    } + MuscleHead.headsFor(MuscleGroup.TRICEPS).map { "Тріцепс: ${it.displayNameUk.lowercase()}" }
    val groups = listOf(
        "Дельти" to heads(MuscleGroup.SHOULDERS),
        "Груди" to heads(MuscleGroup.CHEST),
        "Спина" to heads(MuscleGroup.BACK),
        "Руки" to armHeads
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isDark) Color(0xFF0F1C1E) else Color(0xFFE3F7F4),
        border = BorderStroke(1.dp, if (isDark) Color(0xFF1F3E42) else Color(0xFFBFE6E0)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("advanced_muscle_mode_info")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "${MuscleHead.advancedSelectableCount}",
                    fontFamily = Unbounded,
                    fontWeight = FontWeight.Bold,
                    fontSize = 30.sp,
                    lineHeight = 30.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Доступно більше мʼязів для вибору",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Замість 11 загальних груп",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text(
                text = "Програма рахує обсяг і відновлення окремо для кожного пучка. Наприклад, жим над головою вантажить передню дельту, а розведення в сторони середню.",
                fontSize = 13.sp,
                lineHeight = 19.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                groups.forEach { (name, parts) ->
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = name,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            parts.forEach { part ->
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, IceCyanContainerBorder)
                                ) {
                                    Text(
                                        text = part,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isDark) IceCyanOnContainer else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "І ще 6 груп: квадрицепси, задня поверхня стегна, сідниці, литки, прес, передпліччя",
                fontSize = 12.sp,
                color = TextMutedDark
            )
        }
    }
}
