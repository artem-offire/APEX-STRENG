package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.service.RestTimerAudioHelper
import com.example.service.SetCompleteAudioHelper
import com.example.ui.ApexViewModel
import com.example.ui.components.ExercisePickerDialog
import com.example.ui.components.RestTimerSoundDialog
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

    var showVolumeModal by remember { mutableStateOf(false) }
    var showEditNicknameDialog by remember { mutableStateOf(false) }
    var nicknameInput by remember { mutableStateOf("") }
    var showResetConfirmDialog by remember { mutableStateOf(false) }
    var showManageExercisesModal by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var currentSound by remember { mutableStateOf(RestTimerAudioHelper.getSelectedSound(context)) }
    var showSetSoundDialog by remember { mutableStateOf(false) }
    var currentSetSound by remember { mutableStateOf(SetCompleteAudioHelper.getSelectedSound(context)) }

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
                            Text(
                                text = "З нами з жовтня 2026",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
}
