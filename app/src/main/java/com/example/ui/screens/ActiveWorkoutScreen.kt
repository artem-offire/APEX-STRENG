package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExerciseEntity
import com.example.data.WorkoutSetEntity
import com.example.engine.DoubleProgressionEngine
import com.example.model.MuscleGroup
import com.example.ui.ApexViewModel
import com.example.ui.WorkoutExerciseWithSets
import com.example.ui.components.ExerciseSubstitutionModal
import com.example.ui.components.WorkoutMusicLauncherDialog
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActiveWorkoutScreen(
    sessionId: Long,
    viewModel: ApexViewModel,
    onFinishOrClose: () -> Unit,
    onMinimize: () -> Unit = {}
) {
    BackHandler { onMinimize() }
    val activeExercisesWithSets by viewModel.activeWorkoutExercisesWithSets.collectAsState()
    val elapsedSeconds by viewModel.activeWorkoutElapsedSeconds.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()
    val restSecondsLeft by viewModel.restSecondsLeft.collectAsState()
    val restTotalSeconds by viewModel.restTotalSeconds.collectAsState()
    val isRestActive by viewModel.isRestActive.collectAsState()
    val preferredRestDuration by viewModel.preferredRestDuration.collectAsState()
    val adviceMap by viewModel.progressionAdviceMap.collectAsState()
    val customYtPlaylistUrl by viewModel.customMusicPlaylistUrl.collectAsState()
    val customSpotifyPlaylistUrl by viewModel.customSpotifyPlaylistUrl.collectAsState()
    val preferredMusicPlatform by viewModel.preferredMusicPlatform.collectAsState()
    val activeStartReactions by viewModel.activeStartReactions.collectAsState()

    val currentSession = allSessions.find { it.id == sessionId }

    var showRirModal by remember { mutableStateOf(false) }
    var showMusicDialog by remember { mutableStateOf(false) }
    var showAddExerciseDialog by remember { mutableStateOf(false) }
    var showEditWorkoutTitleDialog by remember { mutableStateOf(false) }
    var workoutTitleInput by remember { mutableStateOf("") }
    var showCancelConfirmDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var editingExerciseItem by remember { mutableStateOf<WorkoutExerciseWithSets?>(null) }
    var editingExerciseNewName by remember { mutableStateOf("") }
    var editingExerciseSelectedMuscles by remember { mutableStateOf<Set<MuscleGroup>>(emptySet()) }
    var exerciseItemToDelete by remember { mutableStateOf<WorkoutExerciseWithSets?>(null) }
    var replacingExerciseItem by remember { mutableStateOf<WorkoutExerciseWithSets?>(null) }

    val allActiveSets = remember(activeExercisesWithSets) {
        activeExercisesWithSets.flatMap { it.sets }
    }

    val allWorkoutSetsCompleted = remember(allActiveSets) {
        allActiveSets.isNotEmpty() && allActiveSets.all { it.isCompleted }
    }

    val liveTonnage = remember(allActiveSets) {
        allActiveSets.filter { it.isCompleted && !it.isWarmup }.sumOf { it.weightKg * it.reps }
    }
    val completedSetsCount = remember(allActiveSets) {
        allActiveSets.count { it.isCompleted && !it.isWarmup }
    }
    val totalWorkingSetsCount = remember(allActiveSets) {
        allActiveSets.count { !it.isWarmup }
    }

    val currentExercise = remember(activeExercisesWithSets) {
        activeExercisesWithSets.firstOrNull { item -> item.sets.any { !it.isCompleted } }?.exercise
            ?: activeExercisesWithSets.firstOrNull()?.exercise
    }

    val isCurrentCompound = remember(currentExercise) {
        val name = currentExercise?.exerciseName?.lowercase() ?: ""
        val isIsolation = name.contains("біцепс") || name.contains("підйом") ||
                name.contains("розведен") || name.contains("махи") ||
                name.contains("трицепс") || name.contains("розгинання") ||
                name.contains("згинання") || name.contains("литки") ||
                name.contains("ікри") || name.contains("прес") ||
                name.contains("скручування") || name.contains("молотки") ||
                name.contains("пуловер") || name.contains("планка")
        !isIsolation
    }

    val listState = rememberLazyListState()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 10.dp, end = 12.dp, top = 10.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onMinimize,
                            modifier = Modifier
                                .size(40.dp)
                                .testTag("minimize_workout_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Згорнути тренування",
                                tint = IceCyanPrimary,
                                modifier = Modifier.size(28.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Left: Title + Pencil & Live Subtitle (● 12:48 · 1 з 3 підх. · 600 кг)
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    workoutTitleInput = currentSession?.title ?: "Силове тренування"
                                    showEditWorkoutTitleDialog = true
                                }
                                .padding(end = 8.dp)
                                .testTag("active_workout_title_btn")
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = currentSession?.title ?: "Силове тренування",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Змінити назву тренування",
                                    tint = TextMutedDark,
                                    modifier = Modifier.size(15.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(7.dp)
                                        .clip(CircleShape)
                                        .background(IceCyanPrimary)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = formatDuration(elapsedSeconds),
                                    fontFamily = Unbounded,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IceCyanPrimary
                                )
                                Text(
                                    text = "   $completedSetsCount з $totalWorkingSetsCount підх.   ${liveTonnage.toInt()} кг",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        // Right: Music Icon Button, More Menu Button (⋮), and Finish Button (Завершити)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // Dedicated Music Button in Top Bar (User Decision 1)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .clickable { showMusicDialog = true }
                                    .testTag("open_yt_music_btn")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.MusicNote,
                                        contentDescription = "Музика для тренування",
                                        tint = IceCyanPrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }

                            // More Menu Button (⋮)
                            Box {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Color.Transparent,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable { showMoreMenu = true }
                                        .testTag("active_workout_more_btn")
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.MoreVert,
                                            contentDescription = "Додаткові дії",
                                            tint = SteelBlueOnContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Додати вправу") },
                                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showAddExerciseDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Музика для тренування") },
                                        leadingIcon = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showMusicDialog = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Шкала інтенсивності (RIR)") },
                                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                        onClick = {
                                            showMoreMenu = false
                                            showRirModal = true
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = { Text("Скасувати тренування", color = ErrorRed) },
                                        leadingIcon = {
                                            Icon(
                                                Icons.Default.Close,
                                                contentDescription = null,
                                                tint = ErrorRed
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            showCancelConfirmDialog = true
                                        }
                                    )
                                }
                            }

                            // Finish Button (Завершити)
                            Button(
                                onClick = {
                                    viewModel.finishWorkout(
                                        sessionId = sessionId,
                                        notes = ""
                                    )
                                    onFinishOrClose()
                                },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = IceCyanPrimary,
                                    contentColor = IceCyanOnPrimary
                                ),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 0.dp),
                                modifier = Modifier
                                    .height(44.dp)
                                    .testTag("finish_workout_header_btn")
                            ) {
                                Text(
                                    text = "Завершити",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        thickness = 1.dp,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        },
        bottomBar = {
            if (allWorkoutSetsCompleted && !isRestActive) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    tonalElevation = 2.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("workout_all_completed_bar")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.finishWorkout(
                                    sessionId = sessionId,
                                    notes = ""
                                )
                                onFinishOrClose()
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = IceCyanPrimary,
                                contentColor = IceCyanOnPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("finish_workout_from_bar_btn")
                        ) {
                            Text(
                                text = "Всі вправи виконано! Завершити 🎉",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            } else {
                WorkoutRestTimerBar(
                    isRestActive = isRestActive,
                    secondsLeft = restSecondsLeft,
                    totalSeconds = restTotalSeconds,
                    preferredDuration = preferredRestDuration,
                    onSelectDuration = { secs ->
                        viewModel.setPreferredRestDuration(secs)
                        if (isRestActive) {
                            viewModel.startRestTimer(secs)
                        }
                    },
                    onStartRest = { viewModel.startRestTimer(preferredRestDuration) },
                    onAdd15s = {
                        if (isRestActive) {
                            viewModel.adjustRestTimer(15)
                            viewModel.setPreferredRestDuration(preferredRestDuration + 15)
                        } else {
                            viewModel.setPreferredRestDuration(preferredRestDuration + 15)
                        }
                    },
                    onMinus15s = {
                        if (isRestActive) {
                            viewModel.adjustRestTimer(-15)
                            viewModel.setPreferredRestDuration(preferredRestDuration - 15)
                        } else {
                            viewModel.setPreferredRestDuration(preferredRestDuration - 15)
                        }
                    },
                    onSkipRest = { viewModel.cancelRestTimer() },
                    isCompoundExercise = isCurrentCompound
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(top = 14.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Reaction / Overload Warning Banner (matching Page 4 PDF)
            val firstReaction = activeStartReactions.firstOrNull()
            if (firstReaction != null) {
                item(key = "reaction_${firstReaction.ruleId}") {
                    VolumeOverloadWarningBanner(
                        title = firstReaction.title,
                        subtitle = firstReaction.body,
                        onDismiss = { viewModel.dismissStartReaction(firstReaction.ruleId) }
                    )
                }
            }

            // 2. Empty Exercises State
            if (activeExercisesWithSets.isEmpty()) {
                item(key = "empty_workout_exercises") {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = IceCyanPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Додайте першу вправу",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Оберіть вправу з бази або введіть власну назву",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // 3. Exercise Cards
            items(
                items = activeExercisesWithSets,
                key = { it.exercise.id }
            ) { item ->
                val exerciseName = item.exercise.exerciseName
                val muscleGroup = item.exercise.muscleGroup
                val sets = item.sets
                val advice = adviceMap[exerciseName]

                ExerciseWorkoutCard(
                    exerciseName = exerciseName,
                    muscleGroup = muscleGroup,
                    sets = sets,
                    advice = advice,
                    onToggleCompleteSet = { set, weight, reps, rir ->
                        if (set.isCompleted) {
                            viewModel.uncompleteSet(set.id)
                        } else {
                            viewModel.completeSet(
                                setId = set.id,
                                rir = rir,
                                weightKg = weight,
                                reps = reps
                            )
                        }
                    },
                    onUpdateValues = { set, weight, reps, rir ->
                        viewModel.updateSetParameters(
                            setId = set.id,
                            weightKg = weight,
                            reps = reps,
                            rir = rir
                        )
                    },
                    onAddSet = {
                        viewModel.addSetToExercise(item.exercise.id)
                    },
                    onDeleteSet = { set ->
                        viewModel.removeSet(set.id)
                    },
                    onEditName = {
                        editingExerciseItem = item
                        editingExerciseNewName = exerciseName
                        editingExerciseSelectedMuscles = MuscleGroup.parseMuscleGroups(item.exercise.muscleGroup).toSet().ifEmpty { setOf(MuscleGroup.CHEST) }
                    },
                    onReplaceExercise = {
                        replacingExerciseItem = item
                    },
                    onDeleteExercise = {
                        exerciseItemToDelete = item
                    },
                    onOpenRirInfo = { showRirModal = true }
                )
            }

            // 4. Dashed "+ Додати вправу" Button at Bottom
            item(key = "add_exercise_dashed_btn") {
                val borderColor = MaterialTheme.colorScheme.outlineVariant
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { showAddExerciseDialog = true }
                        .testTag("add_custom_exercise_bottom_btn"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawRoundRect(
                            color = borderColor,
                            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx()),
                            style = Stroke(
                                width = 1.5.dp.toPx(),
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 10f), 0f)
                            )
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = IceCyanPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Додати вправу",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    if (showRirModal) {
        RirScaleInfoModal(onDismiss = { showRirModal = false })
    }

    if (showMusicDialog) {
        WorkoutMusicLauncherDialog(
            preferredPlatform = preferredMusicPlatform,
            onSelectPlatform = { viewModel.setPreferredMusicPlatform(it) },
            customYtPlaylistUrl = customYtPlaylistUrl,
            onSaveCustomYtPlaylistUrl = { viewModel.setCustomMusicPlaylistUrl(it) },
            customSpotifyPlaylistUrl = customSpotifyPlaylistUrl,
            onSaveCustomSpotifyPlaylistUrl = { viewModel.setCustomSpotifyPlaylistUrl(it) },
            onDismiss = { showMusicDialog = false }
        )
    }

    if (showAddExerciseDialog) {
        AddWorkoutExercisePickerDialog(
            allExercises = allExercises,
            onSelectExercise = { exName, muscleGroup, weightKg, repsCount ->
                viewModel.addCustomExerciseToWorkout(
                    sessionId = sessionId,
                    exerciseName = exName,
                    muscleGroup = muscleGroup,
                    setsCount = 3,
                    initialWeight = weightKg,
                    initialReps = repsCount
                )
            },
            onDeleteExercise = { ex -> viewModel.deleteExercise(ex.id) },
            onDismiss = { showAddExerciseDialog = false }
        )
    }

    replacingExerciseItem?.let { targetItem ->
        ExerciseSubstitutionModal(
            currentExerciseName = targetItem.exercise.exerciseName,
            muscleGroup = targetItem.exercise.muscleGroup,
            allExercises = allExercises,
            onSubstituteSelected = { newExercise ->
                viewModel.substituteExercise(targetItem.exercise.id, newExercise)
                replacingExerciseItem = null
            },
            onDismiss = { replacingExerciseItem = null }
        )
    }

    if (showEditWorkoutTitleDialog) {
        AlertDialog(
            onDismissRequest = { showEditWorkoutTitleDialog = false },
            title = { Text("Змінити назву тренування", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = workoutTitleInput,
                    onValueChange = { workoutTitleInput = it },
                    label = { Text("Назва тренування") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_workout_title_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateWorkoutTitle(sessionId, workoutTitleInput)
                        showEditWorkoutTitleDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    modifier = Modifier.testTag("confirm_workout_title_btn")
                ) {
                    Text("Зберегти", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditWorkoutTitleDialog = false }) {
                    Text("Скасувати")
                }
            }
        )
    }

    if (editingExerciseItem != null) {
        AlertDialog(
            onDismissRequest = { editingExerciseItem = null },
            title = { Text("Редагувати вправу", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    OutlinedTextField(
                        value = editingExerciseNewName,
                        onValueChange = { editingExerciseNewName = it },
                        label = { Text("Назва вправи") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_exercise_name_input")
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Цільові групи м'язів (можна декілька):",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MuscleGroup.values().forEach { mg ->
                                val isSelected = editingExerciseSelectedMuscles.contains(mg)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        editingExerciseSelectedMuscles = if (isSelected) {
                                            if (editingExerciseSelectedMuscles.size > 1) editingExerciseSelectedMuscles - mg else editingExerciseSelectedMuscles
                                        } else {
                                            editingExerciseSelectedMuscles + mg
                                        }
                                    },
                                    label = { Text(mg.displayNameUk, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = IceCyanPrimary.copy(alpha = 0.2f),
                                        selectedLabelColor = IceCyanPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val item = editingExerciseItem
                        if (item != null && editingExerciseNewName.isNotBlank()) {
                            val muscleString = editingExerciseSelectedMuscles.joinToString(",") { it.name }
                            viewModel.updateWorkoutExerciseDetails(
                                workoutExerciseId = item.exercise.id,
                                newName = editingExerciseNewName,
                                newMuscleGroup = muscleString
                            )
                        }
                        editingExerciseItem = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    modifier = Modifier.testTag("confirm_edit_exercise_btn")
                ) {
                    Text("Зберегти", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingExerciseItem = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

    if (exerciseItemToDelete != null) {
        AlertDialog(
            onDismissRequest = { exerciseItemToDelete = null },
            title = { Text("Видалити вправу?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Видалити «${exerciseItemToDelete?.exercise?.exerciseName}» та всі її підходи з поточного тренування?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        exerciseItemToDelete?.let { viewModel.removeWorkoutExercise(it.exercise.id) }
                        exerciseItemToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_delete_exercise_btn")
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                TextButton(onClick = { exerciseItemToDelete = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

    if (showCancelConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCancelConfirmDialog = false },
            title = { Text("Скасувати тренування?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Вийти з активного тренування без завершення?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCancelConfirmDialog = false
                        viewModel.cancelWorkout()
                        onFinishOrClose()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    modifier = Modifier.testTag("confirm_cancel_workout_btn")
                ) {
                    Text("Так, вийти")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirmDialog = false }) {
                    Text("Продовжити")
                }
            }
        )
    }
}

@Composable
private fun VolumeOverloadWarningBanner(
    title: String,
    subtitle: String,
    onDismiss: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = WarningAmberContainer,
        border = BorderStroke(1.dp, WarningAmberBorder),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("start_reaction_banner")
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = WarningAmber,
                    modifier = Modifier
                        .padding(top = 1.dp)
                        .size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WarningAmberLight
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = Color(0xFFC9B49E)
                    )
                }
            }
            IconButton(
                onClick = onDismiss,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Закрити попередження",
                    tint = Color(0xFFA08B74),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExerciseWorkoutCard(
    exerciseName: String,
    muscleGroup: String,
    sets: List<WorkoutSetEntity>,
    advice: DoubleProgressionEngine.ProgressionSuggestion?,
    onToggleCompleteSet: (WorkoutSetEntity, Double, Int, Int) -> Unit,
    onUpdateValues: (WorkoutSetEntity, Double, Int, Int) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (WorkoutSetEntity) -> Unit,
    onEditName: () -> Unit,
    onReplaceExercise: () -> Unit,
    onDeleteExercise: () -> Unit,
    onOpenRirInfo: () -> Unit
) {
    val firstSet = sets.firstOrNull()
    val repsMin = firstSet?.targetRepsMin ?: 8
    val repsMax = firstSet?.targetRepsMax ?: 12
    var menuExpanded by remember { mutableStateOf(false) }

    val targetMuscles = remember(muscleGroup) {
        MuscleGroup.parseMuscleGroups(muscleGroup).ifEmpty {
            listOfNotNull(MuscleGroup.fromStringOrNull(muscleGroup))
        }
    }

    val firstUncompletedSetId = remember(sets) {
        sets.firstOrNull { !it.isCompleted }?.id
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("exercise_card_$exerciseName")
    ) {
        Column(modifier = Modifier.padding(start = 14.dp, end = 14.dp, top = 16.dp, bottom = 10.dp)) {
            // 1. Exercise Title + "..." Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = exerciseName,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )

                Box {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.Transparent,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { menuExpanded = true }
                            .testTag("exercise_menu_btn_$exerciseName")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.MoreHoriz,
                                contentDescription = "Меню вправи",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Замінити вправу") },
                            leadingIcon = { Icon(Icons.Default.SwapHoriz, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onReplaceExercise()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Редагувати вправу") },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                            onClick = {
                                menuExpanded = false
                                onEditName()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Видалити вправу", color = ErrorRed) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = null,
                                    tint = ErrorRed
                                )
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteExercise()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 2. Badges Row (Muscle Groups + Session Status)
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                targetMuscles.forEach { mg ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = IceCyanContainer
                    ) {
                        Text(
                            text = mg.displayNameUk,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = IceCyanPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                val statusLabel = when {
                    advice == null -> "Перша сесія"
                    advice.isPlateau -> "Плато"
                    advice.shouldIncreaseWeight -> "Час підвищити вагу"
                    advice.suggestedWeightKg > 0.0 -> "Подвійна прогресія"
                    else -> "Перша сесія"
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = statusLabel,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SteelBlueOnContainer,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Two Target Boxes (ЦІЛЬ: 3–4 × 8–12 | ЗАПАС: RIR 1–2)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "ЦІЛЬ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${sets.size}–${(sets.size + 1).coerceAtMost(5)} × $repsMin–$repsMax",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onOpenRirInfo() }
                ) {
                    Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(
                            text = "ЗАПАС",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMutedDark,
                            letterSpacing = 0.6.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "RIR 1–2",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = WarningAmber
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 4. Tip / Advice Box with Lightbulb
            val tipText = advice?.explanationUk
                ?: "Перша сесія. Почніть з комфортної ваги на $repsMin–$repsMax повторів."
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = IceCyanPrimary,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = tipText,
                    fontSize = 13.sp,
                    color = if (apexIsDark()) Color(0xFF9FB0B6) else MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 19.sp
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // 5. Table Header: СЕТ | КГ | ПОВТ. | RIR | ✓
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "СЕТ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(38.dp)
                )
                Text(
                    text = "КГ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "ПОВТ.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "RIR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMutedDark,
                    letterSpacing = 0.5.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(56.dp)
                )
                Spacer(modifier = Modifier.width(44.dp))
            }

            Spacer(modifier = Modifier.height(4.dp))

            // 6. Set Rows
            sets.forEach { set ->
                val isCurrentTarget = set.id == firstUncompletedSetId
                Spacer(modifier = Modifier.height(6.dp))
                ExerciseSetRow(
                    set = set,
                    isCurrentTarget = isCurrentTarget,
                    onToggleComplete = onToggleCompleteSet,
                    onUpdate = onUpdateValues,
                    onDelete = { onDeleteSet(set) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 7. "+ Додати підхід" Row Button
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onAddSet() }
                    .testTag("add_set_btn_$exerciseName")
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = IceCyanPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Додати підхід",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = IceCyanPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseSetRow(
    set: WorkoutSetEntity,
    isCurrentTarget: Boolean,
    onToggleComplete: (WorkoutSetEntity, Double, Int, Int) -> Unit,
    onUpdate: (WorkoutSetEntity, Double, Int, Int) -> Unit,
    onDelete: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    var weightText by remember(set.id, set.weightKg) {
        val formatted = if (set.weightKg % 1.0 == 0.0) set.weightKg.toInt().toString() else set.weightKg.toString()
        mutableStateOf(formatted)
    }
    var repsText by remember(set.id, set.reps) { mutableStateOf(set.reps.toString()) }
    var rirValue by remember(set.id, set.rir) { mutableStateOf(set.rir) }
    var showRirMenu by remember { mutableStateOf(false) }

    val rowShape = RoundedCornerShape(14.dp)
    val rowBgColor = when {
        set.isCompleted -> MaterialTheme.colorScheme.primaryContainer
        isCurrentTarget -> MaterialTheme.colorScheme.background
        else -> Color.Transparent
    }
    val inputTextColor = if (set.isCompleted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(rowBgColor)
            .then(
                if (isCurrentTarget && !set.isCompleted) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, rowShape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .testTag("set_row_${set.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set Number Badge
        Box(
            modifier = Modifier.width(38.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(9.dp),
                color = when {
                    set.isCompleted -> IceCyanPrimary
                    isCurrentTarget -> MaterialTheme.colorScheme.onSurface
                    else -> MaterialTheme.colorScheme.surfaceVariant
                },
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${set.setNumber}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when {
                            set.isCompleted -> IceCyanOnPrimary
                            isCurrentTarget -> MaterialTheme.colorScheme.background
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        // Weight Field (КГ)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val fieldBg = if (set.isCompleted) Color.Transparent else MaterialTheme.colorScheme.surface
            val fieldBorder = if (set.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = fieldBg,
                border = if (fieldBorder != Color.Transparent) BorderStroke(1.dp, fieldBorder) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = weightText,
                        onValueChange = {
                            weightText = it
                            val w = it.toDoubleOrNull() ?: set.weightKg
                            val r = repsText.toIntOrNull() ?: set.reps
                            onUpdate(set, w, r, rirValue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Next
                        ),
                        cursorBrush = SolidColor(IceCyanPrimary),
                        textStyle = TextStyle(
                            fontFamily = Manrope,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = inputTextColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("weight_input_${set.id}")
                    )
                }
            }
        }

        // Reps Field (ПОВТ.)
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val fieldBg = if (set.isCompleted) Color.Transparent else MaterialTheme.colorScheme.surface
            val fieldBorder = if (set.isCompleted) Color.Transparent else MaterialTheme.colorScheme.outlineVariant
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = fieldBg,
                border = if (fieldBorder != Color.Transparent) BorderStroke(1.dp, fieldBorder) else null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    BasicTextField(
                        value = repsText,
                        onValueChange = {
                            repsText = it
                            val w = weightText.toDoubleOrNull() ?: set.weightKg
                            val r = it.toIntOrNull() ?: set.reps
                            onUpdate(set, w, r, rirValue)
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        cursorBrush = SolidColor(IceCyanPrimary),
                        textStyle = TextStyle(
                            fontFamily = Manrope,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            color = inputTextColor
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("reps_input_${set.id}")
                    )
                }
            }
        }

        // RIR Amber Pill Selector
        Box(
            modifier = Modifier.width(56.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = WarningAmberContainer,
                border = BorderStroke(1.dp, WarningAmberBorder),
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .fillMaxWidth()
                    .height(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { showRirMenu = true }
                    .testTag("rir_selector_${set.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$rirValue",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = WarningAmberLight
                    )
                }
            }
            DropdownMenu(
                expanded = showRirMenu,
                onDismissRequest = { showRirMenu = false }
            ) {
                listOf(0, 1, 2, 3, 4).forEach { rirOpt ->
                    DropdownMenuItem(
                        text = { Text("RIR $rirOpt (${10 - rirOpt} RPE)") },
                        onClick = {
                            rirValue = rirOpt
                            val w = weightText.toDoubleOrNull() ?: set.weightKg
                            val r = repsText.toIntOrNull() ?: set.reps
                            onUpdate(set, w, r, rirValue)
                            showRirMenu = false
                        }
                    )
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Видалити підхід", color = ErrorRed) },
                    leadingIcon = {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, tint = ErrorRed)
                    },
                    onClick = {
                        showRirMenu = false
                        onDelete()
                    }
                )
            }
        }

        // Complete Checkmark Square Button
        Box(
            modifier = Modifier.width(44.dp),
            contentAlignment = Alignment.CenterEnd
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (set.isCompleted) IceCyanPrimary else Color.Transparent,
                border = if (!set.isCompleted) BorderStroke(1.5.dp, Color(0xFF33434B)) else null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable {
                        val w = weightText.toDoubleOrNull() ?: set.weightKg
                        val r = repsText.toIntOrNull() ?: set.reps
                        onToggleComplete(set, w, r, rirValue)
                    }
                    .testTag("complete_set_btn_${set.id}")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = if (set.isCompleted) "Скасувати підхід ${set.setNumber}" else "Позначити підхід ${set.setNumber} виконаним",
                        tint = if (set.isCompleted) IceCyanOnPrimary else Color(0xFF4A5A63),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutRestTimerBar(
    isRestActive: Boolean,
    secondsLeft: Int,
    totalSeconds: Int,
    preferredDuration: Int,
    onSelectDuration: (Int) -> Unit,
    onStartRest: () -> Unit,
    onAdd15s: () -> Unit,
    onMinus15s: () -> Unit,
    onSkipRest: () -> Unit,
    isCompoundExercise: Boolean = true
) {
    val displayedRemaining = if (isRestActive) secondsLeft else preferredDuration
    val displayedTotal = if (isRestActive) totalSeconds.coerceAtLeast(1) else preferredDuration.coerceAtLeast(1)
    val progress = if (isRestActive) {
        (secondsLeft.toFloat() / displayedTotal.toFloat()).coerceIn(0f, 1f)
    } else {
        1f
    }

    val isDark = apexIsDark()
    val cardShape = RoundedCornerShape(24.dp)
    val stepperColor = if (isDark) Color(0xFF1F2A31) else MaterialTheme.colorScheme.surfaceVariant

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 18.dp, shape = cardShape, ambientColor = Color.Black, spotColor = Color.Black)
                .clip(cardShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(1.dp, if (isDark) Color(0xFF22303A) else MaterialTheme.colorScheme.outline, cardShape)
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Row: ВІДПОЧИНОК 1:12 / 1:30  |  [−15] [+15] [⏭]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ВІДПОЧИНОК",
                        style = ApexSectionLabel,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = buildAnnotatedString {
                            append(formatMinSec(displayedRemaining))
                            withStyle(SpanStyle(fontSize = 15.sp, color = TextMutedDark)) {
                                append(" / ${formatMinSec(displayedTotal)}")
                            }
                        },
                        fontFamily = Unbounded,
                        fontWeight = FontWeight.Bold,
                        fontSize = 30.sp,
                        lineHeight = 34.sp,
                        maxLines = 1,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                RestStepperButton(
                    label = "−15",
                    a11yLabel = "Мінус 15 секунд",
                    color = stepperColor,
                    onClick = onMinus15s,
                    modifier = Modifier.testTag("rest_minus_15s")
                )
                RestStepperButton(
                    label = "+15",
                    a11yLabel = "Плюс 15 секунд",
                    color = stepperColor,
                    onClick = onAdd15s,
                    modifier = Modifier.testTag("rest_plus_15s")
                )

                // Teal Skip / Start Button
                Surface(
                    onClick = { if (isRestActive) onSkipRest() else onStartRest() },
                    shape = RoundedCornerShape(14.dp),
                    color = IceCyanPrimary,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag(if (isRestActive) "skip_rest_next_set_btn" else "start_rest_timer_btn")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isRestActive) Icons.Default.SkipNext else Icons.Default.PlayArrow,
                            contentDescription = if (isRestActive) "Пропустити відпочинок" else "Почати відпочинок",
                            tint = IceCyanOnPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Progress Line
            LinearProgressIndicator(
                progress = { progress },
                color = IceCyanPrimary,
                trackColor = if (isDark) Color(0xFF22303A) else MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
                gapSize = 0.dp,
                drawStopIndicator = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
            )

            // Evidence-based Presets Row (60 с | 90 с | 2 хв | 3 хв | 4 хв | 5 хв)
            val restPresets = listOf(
                60 to "60 с",
                90 to "90 с",
                120 to "2 хв",
                180 to "3 хв",
                240 to "4 хв",
                300 to "5 хв"
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                restPresets.forEach { (preset, label) ->
                    val isSelected = preferredDuration == preset
                    val isRecommended = if (isCompoundExercise) preset in 120..180 else preset in 60..90
                    Surface(
                        onClick = { onSelectDuration(preset) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else if (isDark) Color(0xFF161F25) else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) IceCyanContainerBorder else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .height(38.dp)
                            .testTag("rest_preset_${preset}s")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isRecommended && !isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(IceCyanPrimary.copy(alpha = 0.8f))
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                            }
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Scientific recommendation note
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isDark) Color(0xFF101C20) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF1C343B) else MaterialTheme.colorScheme.outlineVariant),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lightbulb,
                        contentDescription = null,
                        tint = IceCyanPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = if (isCompoundExercise) {
                            "Базова вправа (Schoenfeld, 2016): рекомендовано 2–3+ хв відпочинку для максимальної сили та гіпертрофії"
                        } else {
                            "Ізоляція: 1–2 хв достатньо для повного локального відновлення м'язових волокон"
                        },
                        fontSize = 10.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun RestStepperButton(
    label: String,
    a11yLabel: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = color,
        modifier = modifier
            .size(48.dp)
            .semantics { contentDescription = a11yLabel }
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun RirScaleInfoModal(onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("rir_info_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Шкала RIR (Повторів у запасі)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "RIR показує, скільки повторів ви ще могли б виконати з чистою технікою до відмови:",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                listOf(
                    "RIR 0 (10 RPE)" to "Повна м'язова відмова — жодного повтору більше.",
                    "RIR 1 (9 RPE)" to "В запасі рівно 1 повтор. Високий стимул гіпертрофії.",
                    "RIR 2 (8 RPE)" to "Оптимальна робоча зона (2 повтори в запасі).",
                    "RIR 3 (7 RPE)" to "Помірне навантаження (3 повтори в запасі, старт циклу).",
                    "RIR 4 (6 RPE)" to "Легка розминка або розвантажувальний тиждень (Делоад)."
                ).forEach { (title, desc) ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(title, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = WarningAmber)
                            Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Зрозуміло", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun AddWorkoutExercisePickerDialog(
    allExercises: List<ExerciseEntity>,
    onSelectExercise: (String, String, Double, Int) -> Unit,
    onDeleteExercise: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf<MuscleGroup?>(null) }
    var customExerciseName by remember { mutableStateOf("") }
    var initialWeightInput by remember { mutableStateOf("60") }
    var initialRepsInput by remember { mutableStateOf("10") }

    val filtered = remember(allExercises, searchQuery, selectedMuscle) {
        allExercises.filter { ex ->
            val matchesMuscle = selectedMuscle == null || ex.muscleGroup == selectedMuscle?.name
            val matchesSearch = searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
            matchesMuscle && matchesSearch
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 600.dp)
                .padding(4.dp)
                .testTag("add_exercise_picker_dialog")
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Додати вправу",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        customExerciseName = it
                    },
                    label = { Text("Пошук або власна назва вправи") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("custom_exercise_name_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedMuscle == null,
                        onClick = { selectedMuscle = null },
                        label = { Text("Усі", fontSize = 11.sp) }
                    )
                    MuscleGroup.values().forEach { mg ->
                        FilterChip(
                            selected = selectedMuscle == mg,
                            onClick = { selectedMuscle = if (selectedMuscle == mg) null else mg },
                            label = { Text(mg.displayNameUk, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = initialWeightInput,
                        onValueChange = { initialWeightInput = it },
                        label = { Text("Робоча вага (кг)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = initialRepsInput,
                        onValueChange = { initialRepsInput = it },
                        label = { Text("Повторів") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .heightIn(max = 240.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(filtered, key = { it.id }) { ex ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val w = initialWeightInput.toDoubleOrNull() ?: 50.0
                                    val r = initialRepsInput.toIntOrNull() ?: 10
                                    onSelectExercise(ex.name, ex.muscleGroup, w, r)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val mName = runCatching {
                                        MuscleGroup.valueOf(ex.muscleGroup).displayNameUk
                                    }.getOrDefault(ex.muscleGroup)
                                    Text(
                                        text = mName,
                                        fontSize = 11.sp,
                                        color = IceCyanPrimary
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (ex.instructionsUk == "Власна вправа") {
                                        IconButton(
                                            onClick = { onDeleteExercise(ex) },
                                            modifier = Modifier.size(28.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.DeleteOutline,
                                                contentDescription = "Видалити",
                                                tint = ErrorRed,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                    Icon(
                                        Icons.Default.AddCircleOutline,
                                        contentDescription = "Обрати",
                                        tint = IceCyanPrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val nameToAdd = customExerciseName.trim().ifEmpty { "Жим штанги лежачи" }
                        val muscleToAdd = (selectedMuscle ?: MuscleGroup.CHEST).name
                        val w = initialWeightInput.toDoubleOrNull() ?: 50.0
                        val r = initialRepsInput.toIntOrNull() ?: 10
                        onSelectExercise(nameToAdd, muscleToAdd, w, r)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = IceCyanPrimary,
                        contentColor = IceCyanOnPrimary
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_add_custom_exercise_btn")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Додати до тренування", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

fun formatMinSec(totalSec: Int): String {
    val mins = totalSec / 60
    val secs = totalSec % 60
    return String.format("%02d:%02d", mins, secs)
}

fun formatDuration(seconds: Long): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", mins, secs)
}
