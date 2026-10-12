package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExerciseEntity
import com.example.engine.*
import com.example.model.EquipmentType
import com.example.model.MuscleGroup
import com.example.ui.theme.*

@Composable
fun MesocycleBuilderModal(
    allExercises: List<ExerciseEntity>,
    onSaveAndActivate: (
        name: String,
        totalWeeks: Int,
        model: ProgressionModel,
        plannedDays: List<PlannedDay>,
        priorityMuscles: List<MuscleGroup>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var currentStep by remember { mutableStateOf(1) } // 1: Params & Model, 2: Structure Days, 3: Progression Preview

    // Step 1: Mesocycle Settings
    var mesocycleName by remember { mutableStateOf("Блок гіпертрофії та сили") }
    var totalWeeks by remember { mutableStateOf(5) }
    var selectedModel by remember { mutableStateOf(ProgressionModel.VOLUME_WAVE) }
    var selectedPriorityMuscles by remember { mutableStateOf(setOf(MuscleGroup.CHEST, MuscleGroup.BACK)) }

    // Step 2: Days & Exercises
    var selectedSplitTemplate by remember { mutableStateOf("UPPER_LOWER") }
    var plannedDays by remember { mutableStateOf(getDefaultSplitDays("UPPER_LOWER")) }
    var exercisePickerTargetDayIndex by remember { mutableStateOf<Int?>(null) }

    // Progression calculation (Step 3)
    val calculatedProgression = remember(plannedDays, totalWeeks, selectedModel) {
        MesocycleProgressionEngine.calculateFullMesocycleProgression(plannedDays, totalWeeks, selectedModel)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(vertical = 12.dp)
                .testTag("mesocycle_builder_dialog")
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Конструктор мезоциклу",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Крок $currentStep з 3: " + when (currentStep) {
                                1 -> "Параметри та модель прогресії"
                                2 -> "Структурування тренувальних днів"
                                else -> "Автоматичний розрахунок навантаження"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = IceCyanPrimary
                        )
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Step Navigation Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StepChip(title = "1. Модель", isActive = currentStep == 1, isCompleted = currentStep > 1, modifier = Modifier.weight(1f)) { currentStep = 1 }
                    StepChip(title = "2. Спліт", isActive = currentStep == 2, isCompleted = currentStep > 2, modifier = Modifier.weight(1f)) { currentStep = 2 }
                    StepChip(title = "3. Прогресія", isActive = currentStep == 3, isCompleted = false, modifier = Modifier.weight(1f)) { currentStep = 3 }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Step Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentStep) {
                        1 -> Step1ModelAndParams(
                            name = mesocycleName,
                            onNameChange = { mesocycleName = it },
                            weeks = totalWeeks,
                            onWeeksChange = { totalWeeks = it },
                            selectedModel = selectedModel,
                            onModelSelect = { selectedModel = it },
                            priorityMuscles = selectedPriorityMuscles,
                            onTogglePriority = { muscle ->
                                selectedPriorityMuscles = if (selectedPriorityMuscles.contains(muscle)) {
                                    selectedPriorityMuscles - muscle
                                } else {
                                    selectedPriorityMuscles + muscle
                                }
                            }
                        )
                        2 -> Step2StructureDays(
                            days = plannedDays,
                            selectedTemplate = selectedSplitTemplate,
                            onSelectTemplate = { tpl ->
                                selectedSplitTemplate = tpl
                                plannedDays = getDefaultSplitDays(tpl)
                            },
                            onAddDay = {
                                plannedDays = plannedDays + PlannedDay("День ${plannedDays.size + 1}: Нове тренування", emptyList())
                            },
                            onRemoveDay = { idx ->
                                if (plannedDays.size > 1) {
                                    plannedDays = plannedDays.filterIndexed { i, _ -> i != idx }
                                }
                            },
                            onUpdateDayTitle = { idx, newTitle ->
                                plannedDays = plannedDays.mapIndexed { i, d -> if (i == idx) d.copy(dayTitle = newTitle) else d }
                            },
                            onOpenExercisePicker = { dayIdx ->
                                exercisePickerTargetDayIndex = dayIdx
                            },
                            onRemoveExercise = { dayIdx, exIdx ->
                                plannedDays = plannedDays.mapIndexed { i, d ->
                                    if (i == dayIdx) d.copy(exercises = d.exercises.filterIndexed { ei, _ -> ei != exIdx })
                                    else d
                                }
                            },
                            onUpdateExerciseWeight = { dayIdx, exIdx, newWeight ->
                                plannedDays = plannedDays.mapIndexed { i, d ->
                                    if (i == dayIdx) d.copy(exercises = d.exercises.mapIndexed { ei, e -> if (ei == exIdx) e.copy(baseWeightKg = newWeight) else e })
                                    else d
                                }
                            }
                        )
                        3 -> Step3ProgressionPreview(
                            summaries = calculatedProgression,
                            totalWeeks = totalWeeks,
                            model = selectedModel
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Bottom Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(onClick = { currentStep-- }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Назад")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = { currentStep++ },
                            modifier = Modifier.testTag("builder_next_step_btn")
                        ) {
                            Text("Далі")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                if (mesocycleName.isNotBlank() && plannedDays.isNotEmpty()) {
                                    onSaveAndActivate(
                                        mesocycleName.trim(),
                                        totalWeeks,
                                        selectedModel,
                                        plannedDays,
                                        selectedPriorityMuscles.toList()
                                    )
                                }
                            },
                            modifier = Modifier.testTag("activate_structured_meso_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Активувати мезоцикл", fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Exercise Picker Dialog for Step 2
    exercisePickerTargetDayIndex?.let { dayIdx ->
        ExercisePickerDialog(
            allExercises = allExercises,
            onSelectExercise = { exName, muscle, defaultWeight ->
                val newEx = PlannedExercise(
                    exerciseName = exName,
                    muscleGroup = muscle,
                    baseWeightKg = defaultWeight,
                    isPriority = selectedPriorityMuscles.any { it.name.equals(muscle, ignoreCase = true) }
                )
                plannedDays = plannedDays.mapIndexed { i, d ->
                    if (i == dayIdx) d.copy(exercises = d.exercises + newEx) else d
                }
                exercisePickerTargetDayIndex = null
            },
            onDismiss = { exercisePickerTargetDayIndex = null }
        )
    }
}

@Composable
private fun StepChip(
    title: String,
    isActive: Boolean,
    isCompleted: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = when {
            isActive -> IceCyanContainer
            isCompleted -> SuccessGreen.copy(alpha = 0.15f)
            else -> MaterialTheme.colorScheme.surfaceVariant
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            when {
                isActive -> IceCyanPrimary
                isCompleted -> SuccessGreen
                else -> Color.Transparent
            }
        ),
        modifier = modifier.clickable { onClick() }
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(vertical = 6.dp),
            textAlign = TextAlign.Center,
            fontSize = 11.sp,
            fontWeight = if (isActive || isCompleted) FontWeight.Bold else FontWeight.Normal,
            color = when {
                isActive -> IceCyanPrimary
                isCompleted -> SuccessGreen
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun Step1ModelAndParams(
    name: String,
    onNameChange: (String) -> Unit,
    weeks: Int,
    onWeeksChange: (Int) -> Unit,
    selectedModel: ProgressionModel,
    onModelSelect: (ProgressionModel) -> Unit,
    priorityMuscles: Set<MuscleGroup>,
    onTogglePriority: (MuscleGroup) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            OutlinedTextField(
                value = name,
                onValueChange = onNameChange,
                label = { Text("Назва мезоциклу") },
                placeholder = { Text("напр. Гіпертрофія: Жимовий фокус") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("meso_name_input")
            )
        }

        item {
            Text("Тривалість мезоциклу (тижнів):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(3, 4, 5, 6, 8).forEach { w ->
                    val isSelected = weeks == w
                    FilterChip(
                        selected = isSelected,
                        onClick = { onWeeksChange(w) },
                        label = { Text("$w тиж.", fontSize = 12.sp) }
                    )
                }
            }
            Text(
                text = "Останній тиждень завжди є запланованим делоадом (відновлення зв'язок та ЦНС).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp
            )
        }

        item {
            Text("Наукова модель прогресії навантаження:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ProgressionModel.values().forEach { model ->
                    val isSelected = selectedModel == model
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) IceCyanContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onModelSelect(model) }
                            .testTag("progression_model_${model.id}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = model.titleUk,
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.onSurface
                                )
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = model.descriptionUk,
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Автор: ${model.scientificAuthorUk}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = SteelBlueSecondary
                            )
                        }
                    }
                }
            }
        }

        item {
            Text("Пріоритетні групи м'язів (+1 додатковий підхід у MAV):", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                MuscleGroup.values().forEach { muscle ->
                    val isSelected = priorityMuscles.contains(muscle)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onTogglePriority(muscle) },
                        label = { Text(muscle.displayNameUk, fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}

@Composable
private fun Step2StructureDays(
    days: List<PlannedDay>,
    selectedTemplate: String,
    onSelectTemplate: (String) -> Unit,
    onAddDay: () -> Unit,
    onRemoveDay: (Int) -> Unit,
    onUpdateDayTitle: (Int, String) -> Unit,
    onOpenExercisePicker: (Int) -> Unit,
    onRemoveExercise: (dayIdx: Int, exIdx: Int) -> Unit,
    onUpdateExerciseWeight: (dayIdx: Int, exIdx: Int, Double) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Text("Шаблон структури спліту:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "UPPER_LOWER" to "Upper / Lower (4 дні)",
                    "PPL" to "Push / Pull / Legs (3 дні)",
                    "FULL_BODY" to "Full Body (3 дні)"
                ).forEach { (key, label) ->
                    FilterChip(
                        selected = selectedTemplate == key,
                        onClick = { onSelectTemplate(key) },
                        label = { Text(label, fontSize = 11.sp) }
                    )
                }
            }
        }

        itemsIndexed(days) { dayIdx, day ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth().testTag("planned_day_card_$dayIdx")
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = day.dayTitle,
                            onValueChange = { onUpdateDayTitle(dayIdx, it) },
                            singleLine = true,
                            textStyle = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = { onRemoveDay(dayIdx) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Видалити день", tint = MaterialTheme.colorScheme.error)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Вправи (${day.exercises.size}):", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))

                    day.exercises.forEachIndexed { exIdx, ex ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.exerciseName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    Text("Базова вага: ${ex.baseWeightKg} кг", fontSize = 10.sp, color = IceCyanPrimary)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onUpdateExerciseWeight(dayIdx, exIdx, (ex.baseWeightKg - 2.5).coerceAtLeast(0.0)) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Remove, contentDescription = "-2.5kg", modifier = Modifier.size(14.dp))
                                    }
                                    Text("${ex.baseWeightKg.toInt()}", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    IconButton(
                                        onClick = { onUpdateExerciseWeight(dayIdx, exIdx, ex.baseWeightKg + 2.5) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "+2.5kg", modifier = Modifier.size(14.dp))
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { onRemoveExercise(dayIdx, exIdx) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = "Прибрати", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { onOpenExercisePicker(dayIdx) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Додати вправу до цього дня", fontSize = 11.sp)
                    }
                }
            }
        }

        item {
            Button(
                onClick = onAddDay,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(6.dp))
                Text("Додати ще один тренувальний день")
            }
        }
    }
}

@Composable
private fun Step3ProgressionPreview(
    summaries: List<WeekLoadSummary>,
    totalWeeks: Int,
    model: ProgressionModel
) {
    var selectedWeekIndex by remember { mutableStateOf(0) }
    val currentWeekSummary = summaries.getOrNull(selectedWeekIndex)

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Автоматичний розрахунок прогресії",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall,
                    color = IceCyanPrimary
                )
                Text(
                    text = "Модель: ${model.shortNameUk} • $totalWeeks тижнів",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Weeks tabs
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            summaries.forEachIndexed { idx, summary ->
                val isSelected = selectedWeekIndex == idx
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedWeekIndex = idx },
                    label = {
                        Text(
                            text = if (summary.isDeload) "Тижд. ${summary.weekNumber} (Делоад)" else "Тиждень ${summary.weekNumber}",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Week Details Card
        currentWeekSummary?.let { summary ->
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (summary.isDeload) SuccessGreen.copy(alpha = 0.10f) else MaterialTheme.colorScheme.surfaceVariant
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (summary.isDeload) SuccessGreen else IceCyanPrimary.copy(alpha = 0.5f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = summary.phaseNameUk,
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleSmall,
                            color = if (summary.isDeload) SuccessGreen else IceCyanPrimary
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Text(
                                text = "Цільовий RIR: ${summary.targetRir}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = summary.intensityDescriptionUk,
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ПІДХОДІВ", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${summary.totalSets}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.weight(1.2f)
                        ) {
                            Column(modifier = Modifier.padding(6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("ОРІЄНТ. ЧАС", fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("${summary.totalSets * 3} хв", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Розраховані робочі ваги та підходи:", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(6.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(summary.exercisesLoad) { load ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(load.exerciseName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text(
                                    text = "${load.calculatedSets} підходи • ${load.targetRepsMin}–${load.targetRepsMax} повт. • RIR ${load.targetRir}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "${load.calculatedWeightKg} кг",
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp,
                                color = IceCyanPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExercisePickerDialog(
    allExercises: List<ExerciseEntity>,
    onSelectExercise: (name: String, muscle: String, defaultWeight: Double) -> Unit,
    onDeleteExercise: ((ExerciseEntity) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("ALL") }
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    val filteredList = remember(searchQuery, selectedMuscle, allExercises) {
        allExercises.filter { ex ->
            val matchMuscle = if (selectedMuscle == "ALL") true else ex.muscleGroup.equals(selectedMuscle, ignoreCase = true)
            val matchSearch = if (searchQuery.isBlank()) true else ex.name.contains(searchQuery, ignoreCase = true)
            matchMuscle && matchSearch
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Вибір вправи", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text("Пошук вправи") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("ALL" to "Всі", "CHEST" to "Груди", "BACK" to "Спина", "QUADS" to "Квадрицепс", "HAMSTRINGS" to "Біцепс стегна", "SHOULDERS" to "Плечі", "BICEPS" to "Біцепс", "TRICEPS" to "Тріцепс", "FOREARMS" to "Передпліччя", "ABS" to "Прес", "CALVES" to "Литки").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedMuscle == key,
                            onClick = { selectedMuscle = key },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredList, key = { it.id }) { ex ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectExercise(ex.name, ex.muscleGroup, 50.0)
                                }
                                .testTag("exercise_picker_item_${ex.id}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(ex.name, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    val muscleUk = when (ex.muscleGroup.uppercase()) {
                                        "CHEST" -> "Груди"
                                        "BACK" -> "Спина"
                                        "QUADS" -> "Квадрицепс"
                                        "HAMSTRINGS" -> "Біцепс стегна"
                                        "SHOULDERS" -> "Плечі"
                                        "BICEPS" -> "Біцепс"
                                        "TRICEPS" -> "Тріцепс"
                                        "FOREARMS" -> "Передпліччя"
                                        "ABS" -> "Прес"
                                        "CALVES" -> "Литки"
                                        else -> ex.muscleGroup
                                    }
                                    Text(muscleUk, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (onDeleteExercise != null) {
                                        IconButton(
                                            onClick = { exerciseToDelete = ex },
                                            modifier = Modifier.size(36.dp).testTag("delete_exercise_btn_${ex.id}")
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Видалити вправу",
                                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }

                                    IconButton(
                                        onClick = { onSelectExercise(ex.name, ex.muscleGroup, 50.0) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Add,
                                            contentDescription = "Обрати вправу",
                                            tint = IceCyanPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    exerciseToDelete?.let { ex ->
        AlertDialog(
            onDismissRequest = { exerciseToDelete = null },
            icon = {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
            },
            title = {
                Text(
                    text = "Видалити вправу?",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            text = {
                Text(
                    text = "Ви впевнені, що хочете видалити «${ex.name}» зі списку вправ?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteExercise?.invoke(ex)
                        exerciseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.testTag("confirm_delete_exercise_btn")
                ) {
                    Text("Видалити", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { exerciseToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Скасувати")
                }
            },
            shape = RoundedCornerShape(18.dp)
        )
    }
}

fun getDefaultSplitDays(template: String): List<PlannedDay> {
    return when (template) {
        "PPL" -> listOf(
            PlannedDay(
                dayTitle = "День 1: Push (Жимовий день)",
                exercises = listOf(
                    PlannedExercise("Жим штанги лежачи", "CHEST", EquipmentType.BARBELL, 80.0),
                    PlannedExercise("Жим гантелей під кутом 30°", "CHEST", EquipmentType.DUMBBELL, 28.0),
                    PlannedExercise("Махи гантелями через сторони", "SHOULDERS", EquipmentType.DUMBBELL, 12.0),
                    PlannedExercise("Розгинання на блоці донизу", "TRICEPS", EquipmentType.CABLE, 25.0)
                )
            ),
            PlannedDay(
                dayTitle = "День 2: Pull (Тяговий день)",
                exercises = listOf(
                    PlannedExercise("Підтягування широким хватом", "BACK", EquipmentType.BODYWEIGHT, 0.0),
                    PlannedExercise("Тяга штанги в нахилі", "BACK", EquipmentType.BARBELL, 70.0),
                    PlannedExercise("Face Pull на блоці", "SHOULDERS", EquipmentType.CABLE, 20.0),
                    PlannedExercise("Згинання рук зі штангою", "BICEPS", EquipmentType.BARBELL, 35.0),
                    PlannedExercise("Згинання зап'ясть зі штангою", "FOREARMS", EquipmentType.BARBELL, 25.0)
                )
            ),
            PlannedDay(
                dayTitle = "День 3: Legs (День ніг)",
                exercises = listOf(
                    PlannedExercise("Присідання зі штангою", "QUADS", EquipmentType.BARBELL, 95.0),
                    PlannedExercise("Румунська тяга з гантелями", "HAMSTRINGS", EquipmentType.DUMBBELL, 32.0),
                    PlannedExercise("Жим ногами в тренажері", "QUADS", EquipmentType.MACHINE, 160.0),
                    PlannedExercise("Підйом на носки стоячи", "CALVES", EquipmentType.MACHINE, 50.0)
                )
            )
        )

        "FULL_BODY" -> listOf(
            PlannedDay(
                dayTitle = "День 1: Full Body A",
                exercises = listOf(
                    PlannedExercise("Присідання зі штангою", "QUADS", EquipmentType.BARBELL, 90.0),
                    PlannedExercise("Жим штанги лежачи", "CHEST", EquipmentType.BARBELL, 75.0),
                    PlannedExercise("Тяга штанги в нахилі", "BACK", EquipmentType.BARBELL, 70.0),
                    PlannedExercise("Махи гантелями", "SHOULDERS", EquipmentType.DUMBBELL, 12.0)
                )
            ),
            PlannedDay(
                dayTitle = "День 2: Full Body B",
                exercises = listOf(
                    PlannedExercise("Румунська тяга", "HAMSTRINGS", EquipmentType.BARBELL, 85.0),
                    PlannedExercise("Жим гантелей сидячи", "SHOULDERS", EquipmentType.DUMBBELL, 22.0),
                    PlannedExercise("Тяга верхнього блоку", "BACK", EquipmentType.CABLE, 60.0),
                    PlannedExercise("Згинання на біцепс", "BICEPS", EquipmentType.DUMBBELL, 16.0)
                )
            ),
            PlannedDay(
                dayTitle = "День 3: Full Body C",
                exercises = listOf(
                    PlannedExercise("Жим ногами", "QUADS", EquipmentType.MACHINE, 150.0),
                    PlannedExercise("Жим гантелей 30°", "CHEST", EquipmentType.DUMBBELL, 26.0),
                    PlannedExercise("Тяга горизонтального блоку", "BACK", EquipmentType.CABLE, 55.0),
                    PlannedExercise("Французький жим", "TRICEPS", EquipmentType.BARBELL, 28.0)
                )
            )
        )

        else -> listOf(
            // Upper / Lower
            PlannedDay(
                dayTitle = "День 1: Верх тіла (Горизонтальний фокус)",
                exercises = listOf(
                    PlannedExercise("Жим штанги лежачи", "CHEST", EquipmentType.BARBELL, 80.0),
                    PlannedExercise("Тяга штанги в нахилі", "BACK", EquipmentType.BARBELL, 75.0),
                    PlannedExercise("Жим гантелей під кутом 30°", "CHEST", EquipmentType.DUMBBELL, 28.0),
                    PlannedExercise("Тяга горизонтального блоку", "BACK", EquipmentType.CABLE, 60.0),
                    PlannedExercise("Махи гантелями через сторони", "SHOULDERS", EquipmentType.DUMBBELL, 12.0)
                )
            ),
            PlannedDay(
                dayTitle = "День 2: Низ тіла (Квадрицепс фокус)",
                exercises = listOf(
                    PlannedExercise("Присідання зі штангою", "QUADS", EquipmentType.BARBELL, 100.0),
                    PlannedExercise("Румунська тяга зі штангою", "HAMSTRINGS", EquipmentType.BARBELL, 90.0),
                    PlannedExercise("Жим ногами в тренажері", "QUADS", EquipmentType.MACHINE, 180.0),
                    PlannedExercise("Згинання ніг лежачи", "HAMSTRINGS", EquipmentType.MACHINE, 45.0),
                    PlannedExercise("Підйом на носки", "CALVES", EquipmentType.MACHINE, 50.0)
                )
            ),
            PlannedDay(
                dayTitle = "День 3: Верх тіла (Вертикальний фокус)",
                exercises = listOf(
                    PlannedExercise("Підтягування широким хватом", "BACK", EquipmentType.BODYWEIGHT, 0.0),
                    PlannedExercise("Жим штанги стоячи (Армійський)", "SHOULDERS", EquipmentType.BARBELL, 50.0),
                    PlannedExercise("Тяга верхнього блоку", "BACK", EquipmentType.CABLE, 65.0),
                    PlannedExercise("Згинання на біцепс зі штангою", "BICEPS", EquipmentType.BARBELL, 35.0),
                    PlannedExercise("Розгинання на блоці з канатом", "TRICEPS", EquipmentType.CABLE, 25.0),
                    PlannedExercise("Згинання рук зі штангою зворотним хватом", "FOREARMS", EquipmentType.BARBELL, 28.0)
                )
            ),
            PlannedDay(
                dayTitle = "День 4: Низ тіла (Сідниці & Задня поверхня)",
                exercises = listOf(
                    PlannedExercise("Сідничний місток зі штангою", "GLUTES", EquipmentType.BARBELL, 110.0),
                    PlannedExercise("Румунська тяга зі штангою", "HAMSTRINGS", EquipmentType.BARBELL, 95.0),
                    PlannedExercise("Розгинання ніг сидячи", "QUADS", EquipmentType.MACHINE, 60.0),
                    PlannedExercise("Підйом ніг у висі", "ABS", EquipmentType.BODYWEIGHT, 0.0)
                )
            )
        )
    }
}
