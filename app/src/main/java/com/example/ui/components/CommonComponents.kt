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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.MesocycleEntity
import com.example.engine.AutoregulationEngine
import com.example.engine.WarmupCalculator
import com.example.model.MesocyclePhase
import com.example.model.RirLevel
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun MesocycleProgressRibbon(
    mesocycle: MesocycleEntity,
    onAdvanceWeek: () -> Unit,
    onOpenReview: () -> Unit,
    onOpenStructure: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val currentWeek = mesocycle.currentWeek
    val totalWeeks = mesocycle.totalWeeks

    val phase = when {
        currentWeek == 1 -> MesocyclePhase.INTRO
        currentWeek < totalWeeks - 1 -> MesocyclePhase.ACCUMULATION
        currentWeek == totalWeeks - 1 -> MesocyclePhase.OVERREACH
        else -> MesocyclePhase.DELOAD
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mesocycle_ribbon_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (phase.isDeload) WarningAmber else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = mesocycle.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${mesocycle.programType} • ${mesocycle.totalWeeks} тижнів",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (phase.isDeload) WarningAmber.copy(alpha = 0.2f) else IceCyanContainer,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (phase.isDeload) WarningAmber else IceCyanPrimary
                    )
                ) {
                    Text(
                        text = if (phase.isDeload) "ДЕЛОАД" else "ТИЖДЕНЬ $currentWeek/$totalWeeks",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (phase.isDeload) WarningAmber else IceCyanPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Segmented week progress bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (w in 1..totalWeeks) {
                    val isPast = w < currentWeek
                    val isCurrent = w == currentWeek
                    val isDeloadWeek = w == totalWeeks

                    val color = when {
                        isCurrent && isDeloadWeek -> WarningAmber
                        isCurrent -> IceCyanPrimary
                        isPast -> IceCyanPrimary.copy(alpha = 0.6f)
                        isDeloadWeek -> WarningAmber.copy(alpha = 0.25f)
                        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = phase.titleUk,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (phase.isDeload) WarningAmber else MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Ціль: ${phase.targetRir} • ${phase.subtitleUk}",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onOpenStructure,
                        modifier = Modifier.size(36.dp).testTag("mesocycle_structure_btn")
                    ) {
                        Icon(
                            Icons.Default.Tune,
                            contentDescription = "Структура та прогресія навантаження",
                            tint = IceCyanPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    OutlinedButton(
                        onClick = onOpenReview,
                        modifier = Modifier.testTag("mesocycle_review_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Звіт", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onAdvanceWeek,
                        modifier = Modifier.testTag("advance_week_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Text(if (currentWeek >= totalWeeks) "Завершити" else "+1 Тиждень", fontSize = 11.sp)
                    }
                }
            }

            if (currentWeek == totalWeeks - 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = WarningAmber.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.WarningAmber, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Наступний тиждень — розвантажувальний (Делоад). Підготуйте ЦНС та зв'язки до суперкомпенсації.",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RirBadge(rir: Int, onClick: () -> Unit = {}) {
    val level = RirLevel.fromRir(rir)
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = level.composeColor.copy(alpha = 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, level.composeColor),
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("rir_badge_${rir}")
    ) {
        Text(
            text = level.labelUk,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = level.composeColor
        )
    }
}

@Composable
fun RirMicroSelectorModal(
    currentRir: Int,
    onSelectRir: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("rir_micro_selector_modal")
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Оцінка інтенсивності підходу (RIR)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Скільки чистих повторів залишалося в запасі до повної відмови?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 6.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                RirLevel.values().forEach { level ->
                    val isSelected = level.rir == currentRir
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) level.composeColor.copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceVariant,
                        border = androidx.compose.foundation.BorderStroke(
                            if (isSelected) 2.dp else 1.dp,
                            if (isSelected) level.composeColor else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onSelectRir(level.rir)
                                onDismiss()
                            }
                            .testTag("select_rir_${level.rir}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(14.dp)
                                    .clip(CircleShape)
                                    .background(level.composeColor)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${level.labelUk} (RPE ${level.rpe})",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) level.composeColor else MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = level.descriptionUk,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (isSelected) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = level.composeColor)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Закрити")
                }
            }
        }
    }
}

@Composable
fun IntensityScaleModal(onDismiss: () -> Unit) {
    var selectedRir by remember { mutableStateOf(2) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("intensity_scale_modal")
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Довідник RIR / RPE",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Text(
                    text = "Шкала суб'єктивного сприйняття зусилля (RPE) та повторів у запасі (RIR) за Dr. Mike Israetel & Eric Helms.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Slider
                val currentLevel = RirLevel.fromRir(selectedRir)
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = currentLevel.composeColor.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, currentLevel.composeColor),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${currentLevel.labelUk} • RPE ${currentLevel.rpe}",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                color = currentLevel.composeColor
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentLevel.descriptionUk,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = when (selectedRir) {
                                0 -> "🚨 Стимул максимальний, але втома ЦНС найвища. Використовуйте лише в кінці мезоциклу або на останньому підході ізолюючих вправ."
                                1 -> "🔥 Чудовий гіпертрофічний стимул з керованою втомою. Оптимально для тижнів перевантаження (Overreach)."
                                2 -> "💎 ЗОЛОТИЙ СТАНДАРТ ГІПЕРТРОФІЇ. Максимальний ріст м'язів при мінімальному системному виснаженні зв'язок і ЦНС."
                                3 -> "🌿 Помірне навантаження для фази адаптації або важких базових присідань/тяг."
                                else -> "💤 Розминка або тиждень делоаду. Стимул росту незначний."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Перемістіть повзунок (RIR 0..4):", style = MaterialTheme.typography.labelMedium)
                Slider(
                    value = selectedRir.toFloat(),
                    onValueChange = { selectedRir = it.toInt() },
                    valueRange = 0f..4f,
                    steps = 3,
                    modifier = Modifier.testTag("rir_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зрозуміло")
                }
            }
        }
    }
}

@Composable
fun RestTimerFloatingOverlay(
    secondsLeft: Int,
    totalSeconds: Int,
    onAdjust: (Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = if (totalSeconds > 0) secondsLeft.toFloat() / totalSeconds else 0f
    val minutes = secondsLeft / 60
    val secs = secondsLeft % 60
    val timeFormatted = String.format("%02d:%02d", minutes, secs)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyanPrimary),
        shadowElevation = 8.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("rest_timer_floating_overlay")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(42.dp)) {
                    CircularProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxSize(),
                        color = IceCyanPrimary,
                        trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                        strokeWidth = 3.dp
                    )
                    Icon(
                        Icons.Default.HourglassTop,
                        contentDescription = null,
                        tint = IceCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = "Відпочинок: $timeFormatted",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = IceCyanPrimary
                    )
                    Text(
                        text = "Відновлення фосфокреатину (АТФ)",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(
                    onClick = { onAdjust(30) },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("rest_add_30s")
                ) {
                    Text("+30с", fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = onCancel,
                    modifier = Modifier.size(32.dp).testTag("rest_cancel_btn")
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Скасувати", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun WarmupCalculatorModal(onDismiss: () -> Unit) {
    var workingWeightText by remember { mutableStateOf("100") }
    var isBarbell by remember { mutableStateOf(true) }

    val weightDouble = workingWeightText.toDoubleOrNull() ?: 100.0
    val pyramid = remember(weightDouble, isBarbell) {
        WarmupCalculator.calculatePyramid(weightDouble, isBarbell)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("warmup_calculator_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Підвідна розминка",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Text(
                    text = "Наукова піраміда підведення без накопичення лактату та втоми перед робочими сетами.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = workingWeightText,
                    onValueChange = { workingWeightText = it },
                    label = { Text("Робоча вага (кг)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("warmup_weight_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isBarbell,
                        onClick = { isBarbell = true },
                        label = { Text("Штанга (гриф 20 кг)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isBarbell,
                        onClick = { isBarbell = false },
                        label = { Text("Гантелі / Блок") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    pyramid.forEach { stage ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
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
                                            Text(
                                                text = "${stage.stageNumber}",
                                                color = IceCyanPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = "${stage.weightKg} кг × ${stage.reps} повт. (${stage.percentOfWork}%)",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = stage.purposeUk,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Text(
                                    text = "${stage.restSeconds}с відп.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SteelBlueSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Готово")
                }
            }
        }
    }
}

@Composable
fun ReadinessCheckinModal(
    onSave: (sleep: Int, doms: Int, energy: Int, motivation: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sleep by remember { mutableStateOf(4) }
    var doms by remember { mutableStateOf(2) } // 1=no soreness, 5=high
    var energy by remember { mutableStateOf(4) }
    var motivation by remember { mutableStateOf(4) }

    val liveResult = remember(sleep, doms, energy, motivation) {
        AutoregulationEngine.calculateReadiness(sleep, doms, energy, motivation)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("readiness_checkin_modal")
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Оцінка готовності ЦНС",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Text(
                    text = "Щоденний чек-ін для авторегуляції навантаження перед тренуванням.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(14.dp))

                RatingSelector(title = "1. Якість та тривалість сну", value = sleep, onValueChange = { sleep = it })
                RatingSelector(title = "2. Рівень м'язового болю (DOMS)", value = doms, onValueChange = { doms = it }, hint = "(1 = немає, 5 = сильний біль)")
                RatingSelector(title = "3. Рівень енергії в тілі", value = energy, onValueChange = { energy = it })
                RatingSelector(title = "4. Настрій та мотивація", value = motivation, onValueChange = { motivation = it })

                Spacer(modifier = Modifier.height(10.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (liveResult.isLowReadiness) WarningAmber.copy(alpha = 0.15f) else IceCyanContainer.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (liveResult.isLowReadiness) WarningAmber else IceCyanPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Бал готовності ЦНС: ${String.format("%.1f", liveResult.cnsScore)} / 10.0",
                            fontWeight = FontWeight.Bold,
                            color = if (liveResult.isLowReadiness) WarningAmber else IceCyanPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = liveResult.recommendationUk,
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        onSave(sleep, doms, energy, motivation)
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_readiness_btn")
                ) {
                    Text("Зафіксувати чек-ін")
                }
            }
        }
    }
}

@Composable
private fun RatingSelector(
    title: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    hint: String = ""
) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            if (hint.isNotEmpty()) {
                Text(hint, style = MaterialTheme.typography.bodySmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            for (i in 1..5) {
                val isSelected = i == value
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onValueChange(i) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "$i",
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) IceCyanOnPrimary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MobilityTimerModal(
    isWarmup: Boolean, // true = dynamic warmup, false = static cooldown
    onDismiss: () -> Unit
) {
    val steps = if (isWarmup) {
        listOf(
            "Кола плечима та розминка ротаторної манжети" to 45,
            "Розкриття грудного відділу на колінах (Thoracic extension)" to 45,
            "Мобілізація кульшових суглобів («90/90»)" to 60,
            "Глибокий присід з розведенням колін ліктями" to 45,
            "Динамічний випад з ротацією корпусу («World's Greatest Stretch»)" to 60
        )
    } else {
        listOf(
            "Декомпресія хребта: пасивний вис на турніку" to 45,
            "Поза голуба для сідничних м'язів (ліва / права нога)" to 60,
            "Розтягнення найширших м'язів біля стійки" to 45,
            "Дихальний протокол 4-7-8 для активації парасимпатики" to 90
        )
    }

    var currentStepIndex by remember { mutableStateOf(0) }
    var secondsLeft by remember { mutableStateOf(steps[0].second) }
    var isRunning by remember { mutableStateOf(false) }

    LaunchedEffect(isRunning, currentStepIndex, secondsLeft) {
        if (isRunning && secondsLeft > 0) {
            delay(1000)
            secondsLeft -= 1
            if (secondsLeft == 0 && currentStepIndex < steps.size - 1) {
                currentStepIndex += 1
                secondsLeft = steps[currentStepIndex].second
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("mobility_timer_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (isWarmup) "⚡ Динамічна розминка" else "🧘 Заминка та декомпресія",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (isWarmup) "Підготовка зв'язок, синовіальної рідини та ЦНС" else "Зниження кортизолу, відновлення хребта та дихання",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                val currentStep = steps[currentStepIndex]
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = IceCyanContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Крок ${currentStepIndex + 1} з ${steps.size}",
                            style = MaterialTheme.typography.labelSmall,
                            color = IceCyanPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = currentStep.first,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = String.format("%02d:%02d", secondsLeft / 60, secondsLeft % 60),
                            fontSize = 42.sp,
                            fontWeight = FontWeight.Black,
                            color = IceCyanPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isRunning) "Пауза" else "Старт")
                    }

                    OutlinedButton(
                        onClick = {
                            if (currentStepIndex < steps.size - 1) {
                                currentStepIndex += 1
                                secondsLeft = steps[currentStepIndex].second
                            }
                        },
                        enabled = currentStepIndex < steps.size - 1,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Наступний")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                TextButton(onClick = onDismiss) {
                    Text("Закрити")
                }
            }
        }
    }
}
