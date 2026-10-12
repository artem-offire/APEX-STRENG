package com.example.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.service.RestTimerAudioHelper
import com.example.service.RestTimerSound
import com.example.ui.theme.IceCyanContainer
import com.example.ui.theme.IceCyanPrimary

@Composable
fun RestTimerSoundDialog(
    currentSound: RestTimerSound,
    onSoundSelected: (RestTimerSound) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selected by remember { mutableStateOf(currentSound) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .testTag("rest_sound_dialog")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = IceCyanContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Звук таймера відпочинку",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Оберіть сигнал завершення підходу",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(RestTimerSound.values()) { sound ->
                        val isSelected = sound == selected
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) IceCyanContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selected = sound
                                    RestTimerAudioHelper.playPreviewSound(context, sound)
                                }
                                .testTag("sound_option_${sound.id}")
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    RadioButton(
                                        selected = isSelected,
                                        onClick = {
                                            selected = sound
                                            RestTimerAudioHelper.playPreviewSound(context, sound)
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "${sound.iconEmoji} ${sound.titleUk}",
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp,
                                            color = if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = sound.descriptionUk,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                FilledTonalIconButton(
                                    onClick = {
                                        RestTimerAudioHelper.playPreviewSound(context, sound)
                                    },
                                    modifier = Modifier.size(32.dp).testTag("preview_sound_${sound.id}")
                                ) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Прослухати",
                                        modifier = Modifier.size(16.dp),
                                        tint = IceCyanPrimary
                                    )
                                }
                            }
                        }
                    }
                }

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
                            RestTimerAudioHelper.setSelectedSound(context, selected)
                            onSoundSelected(selected)
                            onDismiss()
                        },
                        modifier = Modifier.testTag("save_rest_sound_btn")
                    ) {
                        Text("Зберегти")
                    }
                }
            }
        }
    }
}
