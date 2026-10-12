package com.example.ui.components

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.ExerciseEntity
import com.example.data.MesocycleEntity
import com.example.data.UserVolumeConfigEntity
import com.example.data.WorkoutSessionEntity
import com.example.engine.VolumeLandmarksCalculator
import com.example.engine.UserVolumeConfig
import com.example.model.*
import com.example.ui.theme.*

@Composable
fun MesocycleReviewModal(
    mesocycle: MesocycleEntity,
    sessions: List<WorkoutSessionEntity>,
    onCompleteAndStartNew: () -> Unit,
    onDismiss: () -> Unit
) {
    val completedSessions = sessions.filter { it.isCompleted }
    val totalTonnage = completedSessions.sumOf { it.totalTonnageKg }
    val totalSeconds = completedSessions.sumOf { it.durationSeconds }
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.85f)
                .padding(8.dp)
                .testTag("mesocycle_review_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Звіт мезоциклу",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = IceCyanPrimary
                        )
                        Text(
                            text = mesocycle.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats grid
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatCard(
                        title = "Виконано",
                        value = "${if (sessions.isNotEmpty()) (completedSessions.size * 100 / sessions.size) else 0}%",
                        icon = Icons.Default.CheckCircle,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Тренувань",
                        value = "${completedSessions.size} / ${sessions.size}",
                        icon = Icons.Default.CalendarToday,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        title = "Час у залі",
                        value = "${hours}г ${minutes}хв",
                        icon = Icons.Default.Timer,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Deload advisory
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = WarningAmber.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, WarningAmber.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Verified, contentDescription = null, tint = WarningAmber, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Науковий висновок періодизації:",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleSmall,
                                color = WarningAmber
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Після ${mesocycle.totalWeeks} тижнів кумулятивного навантаження рівень втоми ЦНС та мікропошкоджень сполучної тканини досяг піку. Рекомендовано провести повноцінний делоад (зниження обсягу на 50%) перед переходом до наступного тренувального блоку.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        onCompleteAndStartNew()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_next_mesocycle_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Завершити та розпочати новий мезоцикл")
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
            Text(title, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScientificVolumeModal(
    currentConfig: UserVolumeConfigEntity?,
    onSaveConfig: (
        ExperienceLevel,
        SleepFactor,
        NutritionFactor,
        StressFactor,
        Set<MuscleGroup>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedExp by remember {
        mutableStateOf(
            runCatching { ExperienceLevel.valueOf(currentConfig?.experienceLevel ?: "") }
                .getOrDefault(ExperienceLevel.INTERMEDIATE)
        )
    }
    var selectedSleep by remember {
        mutableStateOf(
            runCatching { SleepFactor.valueOf(currentConfig?.sleepFactor ?: "") }
                .getOrDefault(SleepFactor.AVERAGE)
        )
    }
    var selectedNutrition by remember {
        mutableStateOf(
            runCatching { NutritionFactor.valueOf(currentConfig?.nutritionFactor ?: "") }
                .getOrDefault(NutritionFactor.MAINTENANCE)
        )
    }
    var selectedStress by remember {
        mutableStateOf(
            runCatching { StressFactor.valueOf(currentConfig?.stressFactor ?: "") }
                .getOrDefault(StressFactor.MODERATE)
        )
    }
    var priorityMuscles by remember {
        mutableStateOf(
            currentConfig?.priorityMuscles?.split(",")?.mapNotNull {
                MuscleGroup.fromStringOrNull(it.trim())
            }?.toSet() ?: setOf(MuscleGroup.CHEST, MuscleGroup.BACK)
        )
    }
    var showExplanation by remember { mutableStateOf(true) }

    val liveLandmarks = remember(selectedExp, selectedSleep, selectedNutrition, selectedStress, priorityMuscles) {
        VolumeLandmarksCalculator.calculate(
            UserVolumeConfig(
                experienceLevel = selectedExp,
                sleepFactor = selectedSleep,
                nutritionFactor = selectedNutrition,
                stressFactor = selectedStress,
                priorityMuscles = priorityMuscles
            ),
            emptyMap()
        )
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.96f)
                .fillMaxHeight(0.92f)
                .padding(8.dp)
                .testTag("scientific_volume_modal")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Орієнтири обсягу (Volume Landmarks)",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Модель Dr. Mike Israetel (RP Strength)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Explanation card: "Що це таке і як це працює?"
                    item {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = IceCyanContainer.copy(alpha = 0.35f),
                            border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.4f)),
                            modifier = Modifier.fillMaxWidth().testTag("volume_landmarks_explanation_card")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { showExplanation = !showExplanation },
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.Default.Info,
                                            contentDescription = null,
                                            tint = IceCyanPrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Що це таке і як це працює?",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                    IconButton(
                                        onClick = { showExplanation = !showExplanation },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            if (showExplanation) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                            contentDescription = if (showExplanation) "Приховати" else "Показати",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                if (showExplanation) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "Volume Landmarks (Орієнтири обсягу) — це наукова система дозування навантаження Dr. Mike Israetel (RP Strength). Вона визначає точну кількість робочих підходів на тиждень для росту м'язів без перетренованості:",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 17.sp
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Row(verticalAlignment = Alignment.Top) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                Text(
                                                    "MV",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.onSurface,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Обсяг збереження (Maintenance Volume)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text("Мінімум підходів, щоб утримати наявні м'язи. Використовується на сушці або для другорядних м'язів.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.Top) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = SteelBlueContainer,
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                Text(
                                                    "MEV",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = SteelBlueSecondary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Мінімальний ефективний обсяг (Min Effective Volume)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                                                Text("Поріг, з якого м'язи взагалі починають рости. Нижче цього обсягу гіпертрофії немає.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.Top) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = IceCyanPrimary.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                Text(
                                                    "MAV",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = IceCyanPrimary,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Оптимальний обсяг росту (Max Adaptive Volume)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = IceCyanPrimary)
                                                Text("«Золотий діапазон» найкращого росту (напр. 15-25 підходів на тиждень). Тут прогрес найшвидший.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }

                                        Row(verticalAlignment = Alignment.Top) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                                modifier = Modifier.padding(top = 1.dp)
                                            ) {
                                                Text(
                                                    "MRV",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 11.sp,
                                                    color = MaterialTheme.colorScheme.error,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text("Максимальний відновлюваний обсяг (Max Recoverable Volume)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = MaterialTheme.colorScheme.error)
                                                Text("Стеля відновлення. Перевищення веде до перетренованості, хронічної втоми та втрати м'язів.", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        text = "⚡ Як це працює: оберіть ваш сон, харчування та стрес. При недосипанні або дефіциті калорій межі безпечного навантаження автоматично зменшуються, захищаючи організм від травм.",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        lineHeight = 15.sp
                                    )
                                }
                            }
                        }
                    }

                    // Factor 1: Experience
                    item {
                        Text("Рівень тренованості:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            ExperienceLevel.values().forEach { exp ->
                                FilterChip(
                                    selected = selectedExp == exp,
                                    onClick = { selectedExp = exp },
                                    label = { Text(exp.displayNameUk.substringBefore("("), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Factor 2: Sleep
                    item {
                        Text("Якість сну:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            SleepFactor.values().forEach { sleep ->
                                FilterChip(
                                    selected = selectedSleep == sleep,
                                    onClick = { selectedSleep = sleep },
                                    label = { Text(sleep.displayNameUk.substringBefore("("), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Factor 3: Nutrition
                    item {
                        Text("Харчування / Калорійність:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            NutritionFactor.values().forEach { nut ->
                                FilterChip(
                                    selected = selectedNutrition == nut,
                                    onClick = { selectedNutrition = nut },
                                    label = { Text(nut.displayNameUk.substringBefore("("), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Factor 4: Stress
                    item {
                        Text("Рівень стресу в житті:", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StressFactor.values().forEach { stress ->
                                FilterChip(
                                    selected = selectedStress == stress,
                                    onClick = { selectedStress = stress },
                                    label = { Text(stress.displayNameUk.substringBefore("("), fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Factor 5: Priority Muscle Groups
                    item {
                        Text("Пріоритетні групи м'язів (Фокус MAV/MRV):", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
                        Text("Обрані групи отримують максимальний обсяг росту, інші переводяться в режим збереження (MV/MEV).", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MuscleGroup.values().forEach { muscle ->
                                val isPriority = priorityMuscles.contains(muscle)
                                FilterChip(
                                    selected = isPriority,
                                    onClick = {
                                        priorityMuscles = if (isPriority) priorityMuscles - muscle else priorityMuscles + muscle
                                    },
                                    label = { Text(muscle.displayNameUk, fontSize = 11.sp) },
                                    leadingIcon = if (isPriority) {
                                        { Icon(Icons.Default.Star, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(14.dp)) }
                                    } else null
                                )
                            }
                        }
                    }

                    // Live calculated landmarks table
                    item {
                        Text("Персональні нормативи сетів / тиждень:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = IceCyanPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    items(liveLandmarks) { item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (item.isPriority) {
                                        Icon(Icons.Default.Star, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                    }
                                    Text(
                                        text = item.muscleGroup.displayNameUk,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("MV: ${item.mv}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("MEV: ${item.mev}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("MAV: ${item.mavMin}-${item.mavMax}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = IceCyanPrimary)
                                    Text("MRV: ${item.mrv}", fontSize = 11.sp, color = ErrorRed)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        onSaveConfig(selectedExp, selectedSleep, selectedNutrition, selectedStress, priorityMuscles)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().testTag("save_volume_settings_btn")
                ) {
                    Text("Зберегти нормативи")
                }
            }
        }
    }
}

@Composable
fun ExerciseSubstitutionModal(
    currentExerciseName: String,
    muscleGroup: String,
    allExercises: List<ExerciseEntity>,
    onSubstituteSelected: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val candidates = remember(currentExerciseName, muscleGroup, allExercises) {
        allExercises.filter { it.name != currentExerciseName && it.muscleGroup == muscleGroup }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("exercise_substitution_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Біомеханічна заміна",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Заміна зайнятого снаряда на еквівалент з тим самим вектором сили",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text("Поточна вправа: $currentExerciseName", fontWeight = FontWeight.SemiBold, color = IceCyanPrimary)

                Spacer(modifier = Modifier.height(10.dp))

                if (candidates.isEmpty()) {
                    Text("Немає доступних аналогів для цієї групи м'язів.", style = MaterialTheme.typography.bodySmall)
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(candidates) { exercise ->
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onSubstituteSelected(exercise)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(exercise.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                        Text("${exercise.equipment} • ${exercise.movementPattern}", style = MaterialTheme.typography.bodySmall, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = IceCyanPrimary)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Скасувати")
                }
            }
        }
    }
}
