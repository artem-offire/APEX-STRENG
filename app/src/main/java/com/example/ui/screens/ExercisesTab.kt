package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.ExerciseEntity
import com.example.model.EquipmentType
import com.example.model.MovementPattern
import com.example.model.MuscleGroup
import com.example.ui.ApexViewModel
import com.example.ui.theme.IceCyanContainer
import com.example.ui.theme.IceCyanPrimary
import com.example.ui.theme.SteelBlueSecondary

enum class ConnectorFilterTab(val label: String) {
    DISCOVER("Discover"),
    ALL("All"),
    CONNECTED("Connected"),
    AVAILABLE("Available")
}

@Composable
fun ExercisesTab(viewModel: ApexViewModel) {
    val allExercises by viewModel.allExercises.collectAsState()
    val selectedGymProfile by viewModel.selectedGymProfile.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedFilterTab by remember { mutableStateOf(ConnectorFilterTab.DISCOVER) }
    var selectedMuscleCategory by remember { mutableStateOf<String?>("All categories") }
    var showCategoryMenu by remember { mutableStateOf(false) }

    var selectedExerciseForDetails by remember { mutableStateOf<ExerciseEntity?>(null) }
    var exerciseToEdit by remember { mutableStateOf<ExerciseEntity?>(null) }
    var showCreateExerciseDialog by remember { mutableStateOf(false) }
    var exerciseToDelete by remember { mutableStateOf<ExerciseEntity?>(null) }

    // Categories list for the dropdown
    val categories = remember {
        listOf("All categories") + MuscleGroup.values().map { it.displayNameUk }
    }

    // Filter logic
    val filteredExercises = remember(allExercises, searchQuery, selectedFilterTab, selectedMuscleCategory, selectedGymProfile) {
        val availableEquipSet = selectedGymProfile?.availableEquipment?.split(",")?.map { it.trim() }?.toSet()
        val category = selectedMuscleCategory

        allExercises.filter { ex ->
            val exerciseMuscles = MuscleGroup.parseMuscleGroups(ex.muscleGroup)
            val matchesSearch = searchQuery.isBlank() ||
                    ex.name.contains(searchQuery, ignoreCase = true) ||
                    ex.instructionsUk.contains(searchQuery, ignoreCase = true) ||
                    ex.muscleGroup.contains(searchQuery, ignoreCase = true) ||
                    exerciseMuscles.any { it.displayNameUk.contains(searchQuery, ignoreCase = true) } ||
                    (exerciseMuscles.contains(MuscleGroup.FOREARMS) &&
                            (searchQuery.contains("предпліч", ignoreCase = true) || searchQuery.contains("передпліч", ignoreCase = true)))

            val matchesCategory = category == null ||
                    category == "All categories" ||
                    ex.muscleGroup.contains(category, ignoreCase = true) ||
                    exerciseMuscles.any { mg ->
                        mg.displayNameUk.equals(category, ignoreCase = true) ||
                        mg.name.equals(category, ignoreCase = true) ||
                        (mg == MuscleGroup.FOREARMS && (category.contains("передпліч", ignoreCase = true) || category.contains("предпліч", ignoreCase = true)))
                    }

            val isConnectedInGym = availableEquipSet == null || availableEquipSet.contains(ex.equipment)

            val matchesTab = when (selectedFilterTab) {
                ConnectorFilterTab.DISCOVER -> true
                ConnectorFilterTab.ALL -> true
                ConnectorFilterTab.CONNECTED -> isConnectedInGym
                ConnectorFilterTab.AVAILABLE -> isConnectedInGym
            }

            matchesSearch && matchesCategory && matchesTab
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        // Large Page Title + Create Exercise Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Connectors",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            FilledTonalButton(
                onClick = { showCreateExerciseDialog = true },
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.testTag("add_exercise_btn")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Нова вправа", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Subtitle (Perplexity style)
        Text(
            text = "Connect services and training movements so the engine can access and act on your data",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Search Bar (Rounded pill input from screenshot)
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = {
                Text(
                    text = "Search all connectors",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    fontSize = 14.sp
                )
            },
            leadingIcon = {
                Icon(
                    Icons.Default.Search,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = if (searchQuery.isNotEmpty()) {
                {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(
                            Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else null,
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_all_connectors_input")
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Filter Pills Row + "All categories" Dropdown (Exact layout from screenshot)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Pill Filters: Discover, All, Connected, Available
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ConnectorFilterTab.values().forEach { tab ->
                    val isSelected = selectedFilterTab == tab
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .clickable { selectedFilterTab = tab }
                            .testTag("filter_tab_${tab.name}")
                    ) {
                        Text(
                            text = tab.label,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Right Category Dropdown: "All categories ∨"
            Box {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color.Transparent,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.clickable { showCategoryMenu = true }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = selectedMuscleCategory ?: "All categories",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                DropdownMenu(
                    expanded = showCategoryMenu,
                    onDismissRequest = { showCategoryMenu = false }
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category, fontSize = 13.sp) },
                            onClick = {
                                selectedMuscleCategory = category
                                showCategoryMenu = false
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Cards List (Formatted as the 2-column or list cards from the screenshot)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredExercises, key = { it.id }) { exercise ->
                ConnectorCard(
                    exercise = exercise,
                    onClick = { selectedExerciseForDetails = exercise },
                    onDelete = { exerciseToDelete = exercise }
                )
            }
        }
    }

    // Technique Detail Dialog
    selectedExerciseForDetails?.let { exercise ->
        ExerciseTechniqueDialog(
            exercise = exercise,
            onEdit = {
                selectedExerciseForDetails = null
                exerciseToEdit = exercise
            },
            onDelete = {
                selectedExerciseForDetails = null
                exerciseToDelete = exercise
            },
            onDismiss = { selectedExerciseForDetails = null }
        )
    }

    // Create / Edit Exercise Dialog
    if (showCreateExerciseDialog || exerciseToEdit != null) {
        AddOrEditExerciseDialog(
            initialExercise = exerciseToEdit,
            onSave = { savedExercise ->
                viewModel.saveOrUpdateExercise(savedExercise)
                showCreateExerciseDialog = false
                exerciseToEdit = null
            },
            onDismiss = {
                showCreateExerciseDialog = false
                exerciseToEdit = null
            }
        )
    }

    // Delete Confirmation Dialog
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
                    text = "Видалити «${ex.name}» з бази вправ? Її можна буде додати знову у будь-який час.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteExercise(ex.id)
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConnectorCard(
    exercise: ExerciseEntity,
    onClick: () -> Unit,
    onDelete: () -> Unit = {}
) {
    // Generate distinct brand-like icon & color based on movement pattern / equipment
    val (iconBgColor, iconVector) = remember(exercise.equipment, exercise.muscleGroup) {
        when {
            exercise.name.contains("Жим", ignoreCase = true) -> Pair(Color(0xFF1E2024), Icons.Default.FitnessCenter)
            exercise.name.contains("Станова", ignoreCase = true) -> Pair(Color(0xFF0F261E), Icons.Default.TrendingUp)
            exercise.name.contains("Присідання", ignoreCase = true) -> Pair(Color(0xFF1E1B2E), Icons.Default.AccessibilityNew)
            exercise.name.contains("Тяга", ignoreCase = true) -> Pair(Color(0xFF281F19), Icons.Default.SportsGymnastics)
            exercise.equipment.contains("DUMBBELL", ignoreCase = true) -> Pair(Color(0xFF182230), Icons.Default.FitnessCenter)
            exercise.equipment.contains("CABLE", ignoreCase = true) -> Pair(Color(0xFF2E1922), Icons.Default.SwapHoriz)
            else -> Pair(Color(0xFF1E2024), Icons.Default.FitnessCenter)
        }
    }

    val substituteCount = remember(exercise.substituteNames) {
        if (exercise.substituteNames.isBlank()) 0
        else exercise.substituteNames.split(",").size
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("connector_card_${exercise.name}")
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square App/Service Avatar (Matching screenshot)
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Center: Title + Verified Badge + Description
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = exercise.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Small Verified Checkmark circle icon (Perplexity style)
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified",
                        tint = Color.Gray.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )

                    // Optional "+1 variant" badge
                    if (substituteCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "+$substituteCount variant",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // 2-line Description
                Text(
                    text = exercise.instructionsUk,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )

                val cardMuscles = remember(exercise.muscleGroup) {
                    MuscleGroup.parseMuscleGroups(exercise.muscleGroup)
                }
                if (cardMuscles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        cardMuscles.forEach { mg ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = IceCyanContainer.copy(alpha = 0.5f)
                            ) {
                                Text(
                                    text = mg.displayNameUk,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = IceCyanPrimary,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(32.dp).testTag("delete_exercise_btn_${exercise.id}")
            ) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Видалити вправу",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
fun ExerciseTechniqueDialog(
    exercise: ExerciseEntity,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("exercise_technique_dialog")
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        val muscleUk = MuscleGroup.formatMuscleGroupsUk(exercise.muscleGroup)
                        Text(
                            text = "$muscleUk • ${exercise.equipment} • ${exercise.movementPattern}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SteelBlueSecondary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Progressive Overload Metadata Summary
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = IceCyanContainer.copy(alpha = 0.2f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Крок: +${exercise.defaultIncrementKg} кг",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IceCyanPrimary
                        )
                        Text(
                            text = "Повтори: ${exercise.defaultTargetRepsMin}–${exercise.defaultTargetRepsMax}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IceCyanPrimary
                        )
                        Text(
                            text = "Ціль RIR: ${exercise.defaultTargetRir}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = IceCyanPrimary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Instructions
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Покрокова техніка виконання:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(exercise.instructionsUk, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Joint safety
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.HealthAndSafety, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Безпека суглобів та біомеханіка:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(exercise.jointSafetyUk, style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (exercise.substituteNames.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = IceCyanContainer.copy(alpha = 0.25f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SwapHoriz, contentDescription = null, tint = IceCyanPrimary, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Перевірені біомеханічні замінники:", fontWeight = FontWeight.Bold, color = IceCyanPrimary, style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(exercise.substituteNames, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier.weight(1f).testTag("delete_exercise_from_dialog_btn")
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Видалити", fontSize = 11.sp)
                    }

                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f).testTag("edit_exercise_from_dialog_btn")
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Змінити", fontSize = 11.sp)
                    }

                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Закрити", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddOrEditExerciseDialog(
    initialExercise: ExerciseEntity?,
    onSave: (ExerciseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialExercise?.name ?: "") }
    var selectedMuscles by remember {
        mutableStateOf(
            MuscleGroup.parseMuscleGroups(initialExercise?.muscleGroup).toSet().ifEmpty {
                setOf(MuscleGroup.CHEST)
            }
        )
    }
    var selectedEquipment by remember {
        mutableStateOf(
            EquipmentType.values().find { it.name.equals(initialExercise?.equipment, ignoreCase = true) }
                ?: EquipmentType.BARBELL
        )
    }
    var instructions by remember {
        mutableStateOf(initialExercise?.instructionsUk ?: "Контрольована ексцентрика 2–3 сек та фіксація у піковому скороченні.")
    }
    var jointSafety by remember {
        mutableStateOf(initialExercise?.jointSafetyUk ?: "Уникайте ривків та працюйте в природній амплітуді суглобів.")
    }
    var incrementText by remember {
        mutableStateOf((initialExercise?.defaultIncrementKg ?: 2.5).toString())
    }
    var repsMinText by remember {
        mutableStateOf((initialExercise?.defaultTargetRepsMin ?: 8).toString())
    }
    var repsMaxText by remember {
        mutableStateOf((initialExercise?.defaultTargetRepsMax ?: 12).toString())
    }
    var targetRirText by remember {
        mutableStateOf((initialExercise?.defaultTargetRir ?: 2).toString())
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .testTag("add_edit_exercise_dialog")
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (initialExercise == null) "Нова вправа" else "Редагувати вправу",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Закрити")
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Назва вправи*") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("exercise_form_name_input")
                    )
                }

                item {
                    Text("Цільові м'язові групи (можна обрати декілька):", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        MuscleGroup.values().forEach { m ->
                            val isSelected = selectedMuscles.contains(m)
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    selectedMuscles = if (isSelected) {
                                        if (selectedMuscles.size > 1) selectedMuscles - m else selectedMuscles
                                    } else {
                                        selectedMuscles + m
                                    }
                                },
                                label = { Text(m.displayNameUk, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Обладнання:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        EquipmentType.values().forEach { eq ->
                            FilterChip(
                                selected = selectedEquipment == eq,
                                onClick = { selectedEquipment = eq },
                                label = { Text(eq.displayNameUk, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                item {
                    Text("Параметри прогресивного навантаження:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = IceCyanPrimary)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = incrementText,
                            onValueChange = { incrementText = it },
                            label = { Text("Крок (кг)", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = repsMinText,
                            onValueChange = { repsMinText = it },
                            label = { Text("Мін. повт.", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = repsMaxText,
                            onValueChange = { repsMaxText = it },
                            label = { Text("Макс. повт.", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = targetRirText,
                            onValueChange = { targetRirText = it },
                            label = { Text("RIR", fontSize = 10.sp) },
                            singleLine = true,
                            modifier = Modifier.weight(0.8f)
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Техніка виконання") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = jointSafety,
                        onValueChange = { jointSafety = it },
                        label = { Text("Безпека суглобів") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Button(
                        onClick = {
                            if (name.isBlank()) return@Button
                            val entity = ExerciseEntity(
                                id = initialExercise?.id ?: 0L,
                                name = name.trim(),
                                muscleGroup = selectedMuscles.joinToString(",") { it.name },
                                equipment = selectedEquipment.name,
                                movementPattern = initialExercise?.movementPattern ?: MovementPattern.HORIZONTAL_PRESS.name,
                                instructionsUk = instructions.trim().ifEmpty { "Власна вправа" },
                                jointSafetyUk = jointSafety.trim().ifEmpty { "Підконтрольна техніка" },
                                substituteNames = initialExercise?.substituteNames ?: "",
                                defaultIncrementKg = incrementText.toDoubleOrNull() ?: 2.5,
                                defaultTargetRepsMin = repsMinText.toIntOrNull() ?: 8,
                                defaultTargetRepsMax = repsMaxText.toIntOrNull() ?: 12,
                                defaultTargetRir = targetRirText.toIntOrNull()?.coerceIn(0, 4) ?: 2,
                                progressionType = initialExercise?.progressionType ?: "DOUBLE_PROGRESSION"
                            )
                            onSave(entity)
                        },
                        modifier = Modifier.fillMaxWidth().testTag("save_exercise_form_btn")
                    ) {
                        Text(if (initialExercise == null) "Додати вправу" else "Зберегти зміни")
                    }
                }
            }
        }
    }
}

