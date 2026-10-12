package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import com.example.model.AthleteCharacter
import com.example.model.CharacterEvolutionTier
import com.example.ui.theme.*

@Composable
fun AthleteCharacterCard(
    character: AthleteCharacter,
    onUpdateNickname: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var showCharacterSheet by remember { mutableStateOf(false) }
    var showEditNicknameDialog by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(character.currentTier.auraColorHex)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { showCharacterSheet = true }
            .testTag("athlete_character_card")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Character Avatar Badge with glowing border
                    Surface(
                        shape = CircleShape,
                        color = Color(character.currentTier.auraColorHex).copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color(character.currentTier.auraColorHex)),
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = character.currentTier.emojiAvatar,
                                fontSize = 28.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val nickDisplay = if (character.nickname == "Атлет" || character.nickname.isBlank()) "Введіть нікнейм" else character.nickname
                            Text(
                                text = nickDisplay,
                                fontWeight = FontWeight.Black,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .clickable { showEditNicknameDialog = true }
                                    .testTag("character_nickname_text")
                            )
                            IconButton(
                                onClick = { showEditNicknameDialog = true },
                                modifier = Modifier.size(28.dp).testTag("edit_nickname_card_btn")
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Ввести свій нікнейм",
                                    tint = IceCyanPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(2.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color(character.currentTier.auraColorHex),
                                modifier = Modifier.padding(1.dp)
                            ) {
                                Text(
                                    text = "Рівень ${character.level}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Text(
                            text = "Бойова міць: ${character.powerLevel} Сили",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IceCyanPrimary
                        )

                        Text(
                            text = "Ранг: ${character.currentTier.titleUk} (${character.currentTier.emojiAvatar})",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }

                IconButton(
                    onClick = { showCharacterSheet = true },
                    modifier = Modifier.size(32.dp).testTag("open_character_sheet_btn")
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Деталі персонажа",
                        tint = IceCyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress to next evolution
            character.nextTier?.let { next ->
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Трансформація в ${next.emojiAvatar} ${next.titleUk}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "${(character.progressToNextTierPercent * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceCyanPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = { character.progressToNextTierPercent },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = Color(next.auraColorHex),
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    )
                }
            } ?: run {
                Text(
                    text = "👑 Максимальна форма еволюції досягнута!",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = WarningAmber
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini stats badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatPill(emoji = "🗡️", label = "Сила", value = "${character.strengthStat}", modifier = Modifier.weight(1f))
                StatPill(emoji = "⚡", label = "Витривалість", value = "${character.staminaStat}", modifier = Modifier.weight(1f))
                StatPill(emoji = "🛡️", label = "Дисципліна", value = "${character.disciplineStat}", modifier = Modifier.weight(1f))
            }
        }
    }

    if (showCharacterSheet) {
        CharacterSheetDialog(
            character = character,
            onUpdateNickname = onUpdateNickname,
            onDismiss = { showCharacterSheet = false }
        )
    }

    if (showEditNicknameDialog) {
        EditNicknameDialog(
            currentNickname = character.nickname,
            onSave = onUpdateNickname,
            onDismiss = { showEditNicknameDialog = false }
        )
    }
}

@Composable
private fun StatPill(emoji: String, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 11.sp)
            Spacer(modifier = Modifier.width(4.dp))
            Column {
                Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun CharacterSheetDialog(
    character: AthleteCharacter,
    onUpdateNickname: (String) -> Unit = {},
    onDismiss: () -> Unit
) {
    var showEditNicknameDialog by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("character_sheet_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Прокачка персонажа",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Avatar Display
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(character.currentTier.auraColorHex).copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(3.dp, Color(character.currentTier.auraColorHex)),
                        modifier = Modifier.size(80.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(character.currentTier.emojiAvatar, fontSize = 42.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val dialogNick = if (character.nickname == "Атлет" || character.nickname.isBlank()) "Введіть нікнейм" else character.nickname
                        Text(
                            text = dialogNick,
                            fontWeight = FontWeight.Black,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.clickable { showEditNicknameDialog = true }
                        )
                        IconButton(
                            onClick = { showEditNicknameDialog = true },
                            modifier = Modifier.size(28.dp).testTag("edit_nickname_dialog_btn")
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Змінити нікнейм",
                                tint = IceCyanPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Text(
                        text = "Рівень ${character.level} • Бойова сила: ${character.powerLevel}",
                        fontWeight = FontWeight.Bold,
                        color = IceCyanPrimary,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "Ранг: ${character.currentTier.emojiAvatar} ${character.currentTier.titleUk}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = IceCyanContainer.copy(alpha = 0.4f),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = "«${character.battleCryUk}»",
                            fontSize = 11.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Характеристики персонажа:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = IceCyanPrimary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    AttributeRow("🗡️ СИЛА (STR)", "${character.strengthStat}", "Росте з робочими вагами та сумарним тоннажем (${character.totalTonnageKg.toInt()} кг)")
                    AttributeRow("⚡ ВИТРИВАЛІСТЬ (STA)", "${character.staminaStat}", "Росте з кожним виконаним підходом (${character.totalSets} підходів)")
                    AttributeRow("🛡️ ДИСЦИПЛІНА (DISC)", "${character.disciplineStat}", "Росте за серію занять (${character.streak} дн.) та ${character.totalWorkouts} тренувань")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Шлях еволюції:",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = IceCyanPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    CharacterEvolutionTier.values().forEach { tier ->
                        val isReached = character.powerLevel >= tier.minPowerLevel
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Surface(
                                shape = CircleShape,
                                color = if (isReached) Color(tier.auraColorHex) else MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(tier.emojiAvatar, fontSize = 16.sp)
                                }
                            }
                            Text(
                                text = "T${tier.tierNumber}",
                                fontSize = 9.sp,
                                fontWeight = if (isReached) FontWeight.Bold else FontWeight.Normal,
                                color = if (isReached) IceCyanPrimary else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Зрозуміло")
                }
            }
        }
    }

    if (showEditNicknameDialog) {
        EditNicknameDialog(
            currentNickname = character.nickname,
            onSave = onUpdateNickname,
            onDismiss = { showEditNicknameDialog = false }
        )
    }
}

@Composable
private fun AttributeRow(name: String, value: String, desc: String) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(value, fontWeight = FontWeight.Black, fontSize = 13.sp, color = IceCyanPrimary)
            }
            Text(desc, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun EditNicknameDialog(
    currentNickname: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var nicknameInput by remember { mutableStateOf(currentNickname) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("edit_nickname_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Ваш нікнейм",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Введіть псевдонім або ім'я вашого персонажа:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = nicknameInput,
                    onValueChange = { if (it.length <= 25) nicknameInput = it },
                    label = { Text("Нікнейм") },
                    placeholder = { Text("напр. Залізний Артем") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("nickname_input_field")
                )
                Spacer(modifier = Modifier.height(18.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Скасувати")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val clean = nicknameInput.trim().ifEmpty { "Атлет" }
                            onSave(clean)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_nickname_btn")
                    ) {
                        Text("Зберегти")
                    }
                }
            }
        }
    }
}
