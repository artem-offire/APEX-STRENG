package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ReadinessCheckinEntity
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

enum class ChartMetric(
    val labelUk: String,
    val color: Color,
    val maxVal: Float
) {
    CNS_SCORE("Бал ЦНС (0–10)", IceCyanPrimary, 10f),
    SLEEP("Сон (1–5)", Color(0xFFB388FF), 5f),
    ENERGY("Енергія (1–5)", Color(0xFFFFD700), 5f),
    DOMS("Біль DOMS (1–5)", Color(0xFFFF5252), 5f),
    MOTIVATION("Мотивація (1–5)", Color(0xFF00E676), 5f)
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun CnsRecoveryChart(
    checkinHistory: List<ReadinessCheckinEntity>,
    modifier: Modifier = Modifier
) {
    // Sort chronologically ascending for plotting
    val sortedHistory = remember(checkinHistory) {
        checkinHistory.sortedBy { it.dateTimestamp }
    }

    var selectedMetric by remember { mutableStateOf(ChartMetric.CNS_SCORE) }
    var showAllMetrics by remember { mutableStateOf(false) }
    var selectedPointIndex by remember { mutableStateOf<Int?>(null) }

    // Animation progress for smooth path reveal
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(sortedHistory.size, selectedMetric) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 800))
    }

    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cns_recovery_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = IceCyanContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.ShowChart,
                                contentDescription = null,
                                tint = IceCyanPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Динаміка відновлення ЦНС",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Тренди сну, болю DOMS, енергії та готовності",
                            style = MaterialTheme.typography.bodySmall,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                FilterChip(
                    selected = showAllMetrics,
                    onClick = { showAllMetrics = !showAllMetrics },
                    label = { Text(if (showAllMetrics) "Всі шари" else "Одиночний", fontSize = 11.sp) },
                    leadingIcon = {
                        Icon(
                            if (showAllMetrics) Icons.Default.Layers else Icons.Default.Timeline,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Metric Selector Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChartMetric.values().forEach { metric ->
                    val isSelected = selectedMetric == metric
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMetric = metric },
                        label = {
                            Text(
                                text = metric.labelUk.substringBefore(" "),
                                fontSize = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        leadingIcon = {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(metric.color)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (sortedHistory.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Зробіть свій перший чек-ін готовності, щоб побачити графік!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Tooltip info bar if point selected
                val activePoint = selectedPointIndex?.let { sortedHistory.getOrNull(it) }
                    ?: sortedHistory.lastOrNull()

                if (activePoint != null) {
                    val sdf = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.forLanguageTag("uk")) }
                    val dateFormatted = remember(activePoint.dateTimestamp) {
                        sdf.format(Date(activePoint.dateTimestamp))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surface,
                        border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = dateFormatted,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SteelBlueSecondary
                                )
                                Text(
                                    text = "ЦНС: ${String.format("%.1f", activePoint.cnsScore)}/10 • Сон: ${activePoint.sleepQuality}/5 • Біль: ${activePoint.domsSoreness}/5",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = IceCyanPrimary
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (activePoint.cnsScore >= 6f) SuccessGreen.copy(alpha = 0.2f) else WarningAmber.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = if (activePoint.cnsScore >= 8f) "ГОТОВИЙ" else if (activePoint.cnsScore >= 6f) "ОПТИМУМ" else "ВТОМА",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activePoint.cnsScore >= 6f) SuccessGreen else WarningAmber
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // Custom Jetpack Compose Canvas Chart
                val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                val labelTextColor = MaterialTheme.colorScheme.onSurfaceVariant

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .testTag("cns_chart_canvas")
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(sortedHistory) {
                                detectTapGestures { offset ->
                                    val count = sortedHistory.size
                                    if (count > 0) {
                                        val leftPadding = 80f
                                        val rightPadding = 40f
                                        val chartWidth = size.width - leftPadding - rightPadding
                                        val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth
                                        val relX = (offset.x - leftPadding).coerceIn(0f, chartWidth)
                                        val nearestIdx = (relX / stepX).toInt().coerceIn(0, count - 1)
                                        selectedPointIndex = nearestIdx
                                    }
                                }
                            }
                            .pointerInput(sortedHistory) {
                                detectDragGestures { change, _ ->
                                    val count = sortedHistory.size
                                    if (count > 0) {
                                        val leftPadding = 80f
                                        val rightPadding = 40f
                                        val chartWidth = size.width - leftPadding - rightPadding
                                        val stepX = if (count > 1) chartWidth / (count - 1) else chartWidth
                                        val relX = (change.position.x - leftPadding).coerceIn(0f, chartWidth)
                                        val nearestIdx = (relX / stepX).toInt().coerceIn(0, count - 1)
                                        selectedPointIndex = nearestIdx
                                    }
                                }
                            }
                    ) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height

                        val leftPad = 80f
                        val rightPad = 40f
                        val topPad = 25f
                        val bottomPad = 50f

                        val plotWidth = canvasWidth - leftPad - rightPad
                        val plotHeight = canvasHeight - topPad - bottomPad

                        // 1. Draw horizontal reference grid lines and labels (0, 2.5, 5, 7.5, 10)
                        val gridSteps = listOf(0f, 2.5f, 5.0f, 7.5f, 10.0f)
                        gridSteps.forEach { scoreVal ->
                            val yRatio = scoreVal / 10f
                            val yPos = topPad + plotHeight * (1f - yRatio)

                            // Grid line
                            drawLine(
                                color = gridColor,
                                start = Offset(leftPad, yPos),
                                end = Offset(leftPad + plotWidth, yPos),
                                strokeWidth = 1f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f))
                            )

                            // Label
                            val textLayout = textMeasurer.measure(
                                text = AnnotatedString(String.format("%.1f", scoreVal)),
                                style = TextStyle(fontSize = 10.sp, color = labelTextColor)
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(leftPad - textLayout.size.width - 10f, yPos - textLayout.size.height / 2f)
                            )
                        }

                        // 2. Draw Scientific Zones shading (Optimal: 8-10 in subtle green, Fatigue: <4.5 in subtle red)
                        val optimalY = topPad + plotHeight * (1f - 0.8f)
                        val fatigueY = topPad + plotHeight * (1f - 0.45f)

                        // Optimal zone highlight
                        drawRect(
                            color = Color(0xFF00E676).copy(alpha = 0.04f),
                            topLeft = Offset(leftPad, topPad),
                            size = androidx.compose.ui.geometry.Size(plotWidth, optimalY - topPad)
                        )
                        // Fatigue zone highlight
                        drawRect(
                            color = Color(0xFFFF5252).copy(alpha = 0.05f),
                            topLeft = Offset(leftPad, fatigueY),
                            size = androidx.compose.ui.geometry.Size(plotWidth, (topPad + plotHeight) - fatigueY)
                        )

                        val dataCount = sortedHistory.size
                        if (dataCount == 0) return@Canvas

                        val stepX = if (dataCount > 1) plotWidth / (dataCount - 1) else plotWidth

                        // Helper function to draw a line series
                        fun drawSeries(
                            values: List<Float>,
                            maxValue: Float,
                            strokeColor: Color,
                            isPrimaryFill: Boolean = false
                        ) {
                            if (values.isEmpty()) return

                            val points = values.mapIndexed { idx, v ->
                                val normV = (v / maxValue).coerceIn(0f, 1f)
                                val x = if (dataCount > 1) leftPad + idx * stepX else leftPad + plotWidth / 2f
                                val y = topPad + plotHeight * (1f - normV)
                                Offset(x, y)
                            }

                            val animatedCount = (points.size * animationProgress.value).toInt().coerceAtLeast(1)
                            val subPoints = points.take(animatedCount)

                            // Path with Bezier smoothing
                            val path = Path()
                            val fillPath = Path()

                            if (subPoints.isNotEmpty()) {
                                path.moveTo(subPoints.first().x, subPoints.first().y)
                                fillPath.moveTo(subPoints.first().x, topPad + plotHeight)
                                fillPath.lineTo(subPoints.first().x, subPoints.first().y)

                                for (i in 0 until subPoints.size - 1) {
                                    val p0 = subPoints[i]
                                    val p1 = subPoints[i + 1]
                                    val cx = (p0.x + p1.x) / 2f
                                    path.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                    fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                                }

                                fillPath.lineTo(subPoints.last().x, topPad + plotHeight)
                                fillPath.close()

                                // Fill under primary line
                                if (isPrimaryFill) {
                                    drawPath(
                                        path = fillPath,
                                        brush = Brush.verticalGradient(
                                            colors = listOf(
                                                strokeColor.copy(alpha = 0.35f),
                                                strokeColor.copy(alpha = 0.0f)
                                            ),
                                            startY = topPad,
                                            endY = topPad + plotHeight
                                        )
                                    )
                                }

                                // Stroke
                                drawPath(
                                    path = path,
                                    color = strokeColor,
                                    style = Stroke(
                                        width = if (isPrimaryFill) 3.5f else 2f,
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                // Data points dots
                                subPoints.forEachIndexed { i, pt ->
                                    val isSelected = selectedPointIndex == i
                                    drawCircle(
                                        color = if (isSelected) Color.White else strokeColor,
                                        radius = if (isSelected) 6f else 4f,
                                        center = pt
                                    )
                                    if (isSelected) {
                                        drawCircle(
                                            color = strokeColor.copy(alpha = 0.4f),
                                            radius = 12f,
                                            center = pt
                                        )
                                    }
                                }
                            }
                        }

                        // Draw series based on mode
                        if (showAllMetrics) {
                            drawSeries(sortedHistory.map { it.sleepQuality.toFloat() }, 5f, ChartMetric.SLEEP.color)
                            drawSeries(sortedHistory.map { it.domsSoreness.toFloat() }, 5f, ChartMetric.DOMS.color)
                            drawSeries(sortedHistory.map { it.energyLevel.toFloat() }, 5f, ChartMetric.ENERGY.color)
                            drawSeries(sortedHistory.map { it.cnsScore }, 10f, ChartMetric.CNS_SCORE.color, isPrimaryFill = true)
                        } else {
                            when (selectedMetric) {
                                ChartMetric.CNS_SCORE -> drawSeries(sortedHistory.map { it.cnsScore }, 10f, ChartMetric.CNS_SCORE.color, isPrimaryFill = true)
                                ChartMetric.SLEEP -> drawSeries(sortedHistory.map { it.sleepQuality.toFloat() }, 5f, ChartMetric.SLEEP.color, isPrimaryFill = true)
                                ChartMetric.DOMS -> drawSeries(sortedHistory.map { it.domsSoreness.toFloat() }, 5f, ChartMetric.DOMS.color, isPrimaryFill = true)
                                ChartMetric.ENERGY -> drawSeries(sortedHistory.map { it.energyLevel.toFloat() }, 5f, ChartMetric.ENERGY.color, isPrimaryFill = true)
                                ChartMetric.MOTIVATION -> drawSeries(sortedHistory.map { it.motivation.toFloat() }, 5f, ChartMetric.MOTIVATION.color, isPrimaryFill = true)
                            }
                        }

                        // Draw X-Axis Date labels (Day of week / day number)
                        val dayFormat = SimpleDateFormat("dd.MM", Locale.getDefault())
                        sortedHistory.forEachIndexed { idx, checkin ->
                            val x = if (dataCount > 1) leftPad + idx * stepX else leftPad + plotWidth / 2f
                            val dateLabel = dayFormat.format(Date(checkin.dateTimestamp))

                            val textLayout = textMeasurer.measure(
                                text = AnnotatedString(dateLabel),
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = if (selectedPointIndex == idx) IceCyanPrimary else labelTextColor,
                                    fontWeight = if (selectedPointIndex == idx) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                            drawText(
                                textLayoutResult = textLayout,
                                topLeft = Offset(x - textLayout.size.width / 2f, topPad + plotHeight + 10f)
                            )
                        }

                        // Selected indicator vertical line
                        selectedPointIndex?.let { selIdx ->
                            if (selIdx in 0 until dataCount) {
                                val x = if (dataCount > 1) leftPad + selIdx * stepX else leftPad + plotWidth / 2f
                                drawLine(
                                    color = IceCyanPrimary.copy(alpha = 0.6f),
                                    start = Offset(x, topPad),
                                    end = Offset(x, topPad + plotHeight),
                                    strokeWidth = 1.5f,
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f))
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Scientific Zone Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                ZoneIndicator(color = SuccessGreen, label = "8.0–10.0 Готовність")
                ZoneIndicator(color = IceCyanPrimary, label = "6.0–7.9 Оптимум")
                ZoneIndicator(color = WarningAmber, label = "4.5–5.9 Втома")
                ZoneIndicator(color = ErrorRed, label = "<4.5 Делоад")
            }
        }
    }
}

@Composable
private fun ZoneIndicator(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
