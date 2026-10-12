package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ExerciseEntity
import com.example.data.WorkoutSetWithExerciseInfo
import com.example.engine.PlannedExercise
import com.example.model.MuscleGroup
import com.example.ui.theme.*

@Composable
fun TodayWorkoutSelectorCard(
    allExercises: List<ExerciseEntity>,
    completedSets: List<WorkoutSetWithExerciseInfo>,
    onStartWorkout: (title: String, muscles: List<MuscleGroup>, exercises: List<PlannedExercise>) -> Unit,
    onStartBlankWorkout: (title: String) -> Unit,
    onDeleteExercise: (Long) -> Unit = {},
    onOpenCalendar: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var workoutTitle by remember { mutableStateOf("Вільне тренування") }
    var plannedExercises by remember { mutableStateOf<List<PlannedExercise>>(emptyList()) }
    var showExercisePicker by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("today_workout_selector_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyanPrimary)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = CircleShape,
                        color = IceCyanContainer,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("⚡", fontSize = 16.sp)
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Вільне тренування",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "100% свобода вибору: вкажіть назву та вправи",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                IconButton(
                    onClick = { showInfoDialog = true },
                    modifier = Modifier.size(36.dp).testTag("free_workout_info_btn")
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Що це таке",
                        tint = IceCyanPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Direct inline explanation banner
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = IceCyanContainer.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth().clickable { showInfoDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = IceCyanPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Що це таке: швидкий запуск будь-якого тренування без жорсткої програми. Вкажіть назву та додайте потрібні вправи.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Workout Title Input
            OutlinedTextField(
                value = workoutTitle,
                onValueChange = { workoutTitle = it },
                label = { Text("Назва тренування") },
                placeholder = { Text("Введіть назву тренування") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("free_workout_title_input")
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Planned Exercises Section
            if (plannedExercises.isNotEmpty()) {
                Text(
                    text = "Заплановані вправи (${plannedExercises.size}):",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = IceCyanPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    plannedExercises.forEachIndexed { index, ex ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "${index + 1}.",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = IceCyanPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = ex.exerciseName,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 13.sp
                                        )
                                        Text(
                                            text = "${ex.muscleGroup} • базово ${ex.baseWeightKg} кг",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        plannedExercises = plannedExercises.toMutableList().also { it.removeAt(index) }
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Видалити", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showExercisePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(vertical = 6.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Додати ще вправу", fontSize = 12.sp)
                }
            } else {
                // Empty Exercises note + add button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Вправи не обрано",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Ви зможете додавати будь-які вправи вже під час самого тренування",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        FilledTonalButton(
                            onClick = { showExercisePicker = true },
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Обрати", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main CTA: Start Free Workout
            Button(
                onClick = {
                    val finalTitle = workoutTitle.trim().ifEmpty { "Вільне тренування" }
                    if (plannedExercises.isNotEmpty()) {
                        onStartWorkout(finalTitle, emptyList(), plannedExercises)
                    } else {
                        onStartBlankWorkout(finalTitle)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("start_free_workout_main_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = IceCyanPrimary)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = IceCyanOnPrimary, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "ПОЧАТИ ТРЕНУВАННЯ",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = IceCyanOnPrimary
                )
            }
        }
    }

    if (showExercisePicker) {
        ExercisePickerDialog(
            allExercises = allExercises,
            onSelectExercise = { exName, muscle, weight ->
                val newEx = PlannedExercise(
                    exerciseName = exName,
                    muscleGroup = muscle,
                    baseWeightKg = weight
                )
                plannedExercises = plannedExercises + newEx
                showExercisePicker = false
            },
            onDeleteExercise = { ex ->
                onDeleteExercise(ex.id)
                plannedExercises = plannedExercises.filter { it.exerciseName != ex.name }
            },
            onDismiss = { showExercisePicker = false }
        )
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            icon = {
                Icon(Icons.Default.Info, contentDescription = null, tint = IceCyanPrimary)
            },
            title = {
                Text("Що це таке: Вільне тренування", fontWeight = FontWeight.Bold)
            },
            text = {
                Text(
                    text = "Це режим вільного старту без прив'язки до фіксованої програми чи розкладу:\n\n" +
                            "• 100% Гнучкість: Вкажіть будь-яку назву або залиште стандартну.\n" +
                            "• Вправи на вибір: Додайте вправи заздалегідь або обирайте їх прямо під час тренування в залі.\n" +
                            "• Автономний облік: Усі виконані підходи, робочі ваги та час відпочинку автоматично записуються в базу, приносять XP та враховуються в аналітиці.",
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                Button(onClick = { showInfoDialog = false }) {
                    Text("Зрозуміло")
                }
            }
        )
    }
}
