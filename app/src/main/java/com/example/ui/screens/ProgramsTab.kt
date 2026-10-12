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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.ExperienceLevel
import com.example.model.MuscleGroup
import com.example.ui.ApexViewModel
import com.example.ui.components.MesocycleBuilderModal
import com.example.ui.components.MesocycleStructureDialog
import com.example.ui.theme.IceCyanContainer
import com.example.ui.theme.IceCyanPrimary
import com.example.ui.theme.SteelBlueSecondary
import com.example.ui.theme.SuccessGreen

data class ProgramTemplate(
    val title: String,
    val splitType: String,
    val daysPerWeek: Int,
    val descriptionUk: String,
    val scientificRationaleUk: String,
    val suggestedWeeks: Int = 5
)

val SCIENTIFIC_PROGRAMS = listOf(
    ProgramTemplate(
        title = "Upper / Lower (Верх / Низ)",
        splitType = "Upper / Lower (4 дні)",
        daysPerWeek = 4,
        descriptionUk = "2 тренування верху тіла та 2 тренування низу тіла на тиждень.",
        scientificRationaleUk = "Частота 2 рази на тиждень на кожен м'яз для максимальної гіпертрофії.",
        suggestedWeeks = 5
    ),
    ProgramTemplate(
        title = "Push / Pull / Legs (PPL)",
        splitType = "Push / Pull / Legs (3–6 днів)",
        daysPerWeek = 3,
        descriptionUk = "Жимові (груди/дельти/трицепс), тягові (спина/біцепс) та ноги.",
        scientificRationaleUk = "Фокус на рухових патернах без перехресного перевантаження зв'язок.",
        suggestedWeeks = 5
    ),
    ProgramTemplate(
        title = "Full Body (Все тіло)",
        splitType = "Full Body (3 дні)",
        daysPerWeek = 3,
        descriptionUk = "Опрацювання основних м'язових груп на кожному тренуванні.",
        scientificRationaleUk = "Висока частота стимуляції синтезу м'язового білка при помірному обсязі.",
        suggestedWeeks = 4
    ),
    ProgramTemplate(
        title = "Arnold Split (Золота ера)",
        splitType = "Arnold Split (5–6 днів)",
        daysPerWeek = 5,
        descriptionUk = "Груди/Спина (антагоністи), Плечі/Руки, Ноги.",
        scientificRationaleUk = "Пампінг та робота антагоністів для досвідчених атлетів.",
        suggestedWeeks = 5
    )
)

@Composable
fun ProgramsTab(
    viewModel: ApexViewModel,
    onWorkoutReady: () -> Unit
) {
    var showBuilderModal by remember { mutableStateOf(false) }
    var showWizard by remember { mutableStateOf(false) }
    var showStructureDialog by remember { mutableStateOf(false) }
    var selectedTemplateForDetails by remember { mutableStateOf<ProgramTemplate?>(null) }
    var mesocycleToEdit by remember { mutableStateOf<com.example.data.MesocycleEntity?>(null) }
    var mesocycleToDelete by remember { mutableStateOf<com.example.data.MesocycleEntity?>(null) }

    val activeMesocycle by viewModel.activeMesocycle.collectAsState()
    val allMesocycles by viewModel.allMesocycles.collectAsState()
    val activeWeekPlans by viewModel.activeMesocycleWeekPlans.collectAsState()
    val overloadLogs by viewModel.allProgressiveOverloadLogs.collectAsState()
    val allSessions by viewModel.allSessions.collectAsState()
    val allExercises by viewModel.allExercises.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Active Mesocycle Info Card
        if (activeMesocycle != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("active_meso_structure_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "АКТИВНИЙ МЕЗОЦИКЛ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = IceCyanPrimary
                                )
                                Text(
                                    text = activeMesocycle!!.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                if (activeMesocycle!!.goalDescriptionUk.isNotBlank()) {
                                    Text(
                                        text = activeMesocycle!!.goalDescriptionUk,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = IceCyanContainer
                                ) {
                                    Text(
                                        text = "Тиждень ${activeMesocycle!!.currentWeek}/${activeMesocycle!!.totalWeeks}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IceCyanPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { mesocycleToEdit = activeMesocycle },
                                    modifier = Modifier.size(34.dp).testTag("edit_active_mesocycle_btn")
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Редагувати мезоцикл", modifier = Modifier.size(16.dp))
                                }
                                IconButton(
                                    onClick = { mesocycleToDelete = activeMesocycle },
                                    modifier = Modifier.size(34.dp).testTag("delete_active_mesocycle_btn")
                                ) {
                                    Icon(
                                        Icons.Default.DeleteOutline,
                                        contentDescription = "Видалити мезоцикл",
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showStructureDialog = true },
                            modifier = Modifier.fillMaxWidth().testTag("view_active_structure_btn"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Переглянути структуру та прогресію навантаження", fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        // Wizard & Builder Banner
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("wizard_banner_card"),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, IceCyanPrimary)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Конструктор мезоциклів",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = IceCyanPrimary
                            )
                            Text(
                                text = "Створення та структурування блоку з автоматичним розрахунком прогресії навантаження (Israetel, Helms, Linear).",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(
                            Icons.Default.Engineering,
                            contentDescription = null,
                            tint = IceCyanPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = { showBuilderModal = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_mesocycle_builder_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Створити та структурувати блок", fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showWizard = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("open_program_wizard_btn"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Швидкий Wizard мезоциклу", fontSize = 12.sp)
                    }
                }
            }
        }

        // Program Catalog Header
        item {
            Text(
                text = "Наукові готові програми тренувань",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        items(SCIENTIFIC_PROGRAMS) { program ->
            ProgramTemplateCard(
                template = program,
                isActive = activeMesocycle?.programType?.contains(program.title.substringBefore("(").trim()) == true,
                onStartProgram = {
                    viewModel.startNewMesocycle(
                        name = "Блок: ${program.title.substringBefore("(").trim()}",
                        totalWeeks = program.suggestedWeeks,
                        programType = program.splitType,
                        priorityMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.BACK)
                    )
                    onWorkoutReady()
                },
                onDetailsClick = {
                    selectedTemplateForDetails = program
                }
            )
        }

        // Saved Mesocycles History (Read, Update, Activate, Delete)
        if (allMesocycles.size > 1) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Історія та збережені мезоцикли",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(allMesocycles, key = { "saved_meso_${it.id}" }) { meso ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("saved_meso_card_${meso.id}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(meso.name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                if (meso.isActive) {
                                    Surface(shape = RoundedCornerShape(4.dp), color = IceCyanContainer) {
                                        Text(
                                            text = "АКТИВНИЙ",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = IceCyanPrimary,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${meso.programType} • Тиждень ${meso.currentWeek}/${meso.totalWeeks}",
                                fontSize = 11.sp,
                                color = SteelBlueSecondary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (!meso.isActive) {
                                TextButton(
                                    onClick = { viewModel.activateMesocycle(meso.id) },
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text("Активувати", fontSize = 11.sp)
                                }
                            }
                            IconButton(
                                onClick = { mesocycleToEdit = meso },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Редагувати", modifier = Modifier.size(16.dp))
                            }
                            IconButton(
                                onClick = { mesocycleToDelete = meso },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Default.DeleteOutline,
                                    contentDescription = "Видалити",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Mesocycle Dialog
    mesocycleToEdit?.let { meso ->
        var editedName by remember(meso) { mutableStateOf(meso.name) }
        var editedGoal by remember(meso) { mutableStateOf(meso.goalDescriptionUk) }
        AlertDialog(
            onDismissRequest = { mesocycleToEdit = null },
            title = { Text("Редагувати мезоцикл", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Назва блоку") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editedGoal,
                        onValueChange = { editedGoal = it },
                        label = { Text("Ціль / Опис прогресії") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateMesocycleDetails(meso.id, editedName, editedGoal)
                        mesocycleToEdit = null
                    }
                ) {
                    Text("Зберегти")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mesocycleToEdit = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Delete Mesocycle Confirmation Dialog
    mesocycleToDelete?.let { meso ->
        AlertDialog(
            onDismissRequest = { mesocycleToDelete = null },
            title = { Text("Видалити мезоцикл?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Видалити блок «${meso.name}» та пов'язані з ним заплановані тижні?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMesocycle(meso.id)
                        mesocycleToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Видалити")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { mesocycleToDelete = null }) {
                    Text("Скасувати")
                }
            }
        )
    }

    // Mesocycle Builder Modal (With Automatic Progression Calculation)
    if (showBuilderModal) {
        MesocycleBuilderModal(
            allExercises = allExercises,
            onSaveAndActivate = { name, weeks, model, plannedDays, priorities ->
                viewModel.createStructuredMesocycle(name, weeks, model, plannedDays, priorities)
                showBuilderModal = false
                onWorkoutReady()
            },
            onDismiss = { showBuilderModal = false }
        )
    }

    // Active Mesocycle Structure Dialog
    if (showStructureDialog && activeMesocycle != null) {
        val mesoSessions = allSessions.filter { it.mesocycleId == activeMesocycle!!.id }
        MesocycleStructureDialog(
            mesocycle = activeMesocycle!!,
            sessions = mesoSessions,
            weekPlans = activeWeekPlans,
            overloadLogs = overloadLogs,
            onDismiss = { showStructureDialog = false }
        )
    }

    // Wizard Modal
    if (showWizard) {
        MesocycleWizardModal(
            onGenerate = { name, weeks, split, priorities ->
                viewModel.startNewMesocycle(name, weeks, split, priorities)
                showWizard = false
                onWorkoutReady()
            },
            onDismiss = { showWizard = false }
        )
    }

    // Program Details Dialog
    selectedTemplateForDetails?.let { template ->
        ProgramDetailsDialog(
            template = template,
            onStart = {
                viewModel.startNewMesocycle(
                    name = "Блок: ${template.title.substringBefore("(").trim()}",
                    totalWeeks = template.suggestedWeeks,
                    programType = template.splitType,
                    priorityMuscles = listOf(MuscleGroup.CHEST, MuscleGroup.BACK)
                )
                selectedTemplateForDetails = null
                onWorkoutReady()
            },
            onDismiss = { selectedTemplateForDetails = null }
        )
    }
}

@Composable
fun ProgramTemplateCard(
    template: ProgramTemplate,
    isActive: Boolean,
    onStartProgram: () -> Unit,
    onDetailsClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("program_card_${template.title}"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        border = if (isActive) androidx.compose.foundation.BorderStroke(1.5.dp, IceCyanPrimary) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = template.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${template.daysPerWeek} тренувань на тиждень • ${template.suggestedWeeks} тижнів блоку",
                        style = MaterialTheme.typography.labelSmall,
                        color = SteelBlueSecondary
                    )
                }

                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = IceCyanContainer
                    ) {
                        Text(
                            text = "АКТИВНА",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = IceCyanPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = template.descriptionUk,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDetailsClick,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Огляд", fontSize = 12.sp)
                }

                Button(
                    onClick = onStartProgram,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(if (isActive) "Перезапустити" else "Обрати програму", fontSize = 11.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MesocycleWizardModal(
    onGenerate: (name: String, weeks: Int, split: String, priorities: List<MuscleGroup>) -> Unit,
    onDismiss: () -> Unit
) {
    var blockName by remember { mutableStateOf("Гіпертрофічний блок 1") }
    var selectedWeeks by remember { mutableStateOf(5) }
    var selectedSplitIndex by remember { mutableStateOf(0) }
    val splits = listOf(
        "Upper / Lower (4 дні)" to "Верх/Низ (4 дні)",
        "Push / Pull / Legs (3–6 днів)" to "PPL (3 дні)",
        "Full Body (3 дні)" to "Full Body (3 дні)",
        "Arnold Split (5–6 днів)" to "Arnold (5 днів)"
    )
    var selectedPriorities by remember {
        mutableStateOf(setOf(MuscleGroup.CHEST, MuscleGroup.BACK))
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f)
                .padding(8.dp)
                .testTag("wizard_modal")
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
                    Text(
                        text = "Конструктор мезоциклу",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрити")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        OutlinedTextField(
                            value = blockName,
                            onValueChange = { blockName = it },
                            label = { Text("Назва мезоциклу") },
                            modifier = Modifier.fillMaxWidth().testTag("wizard_name_input")
                        )
                    }

                    item {
                        Text("Тип спліту:", fontWeight = FontWeight.SemiBold)
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            splits.forEachIndexed { idx, pair ->
                                val isSelected = idx == selectedSplitIndex
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isSelected) IceCyanContainer.copy(alpha = 0.35f) else MaterialTheme.colorScheme.surfaceVariant,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (isSelected) IceCyanPrimary else MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { selectedSplitIndex = idx }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = { selectedSplitIndex = idx }
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(pair.second, fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text("Тривалість мезоциклу (з розвантажувальним тижнем Делоад):", fontWeight = FontWeight.SemiBold)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(4, 5, 6).forEach { w ->
                                FilterChip(
                                    selected = selectedWeeks == w,
                                    onClick = { selectedWeeks = w },
                                    label = { Text("$w тижнів (останній делоад)", fontSize = 11.sp) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    item {
                        Text("Пріоритетні групи м'язів (Фокус MAV):", fontWeight = FontWeight.SemiBold)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            MuscleGroup.values().forEach { muscle ->
                                val isSelected = selectedPriorities.contains(muscle)
                                FilterChip(
                                    selected = isSelected,
                                    onClick = {
                                        selectedPriorities = if (isSelected) selectedPriorities - muscle else selectedPriorities + muscle
                                    },
                                    label = { Text(muscle.displayNameUk, fontSize = 11.sp) }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val chosenSplit = splits[selectedSplitIndex].first
                        onGenerate(blockName, selectedWeeks, chosenSplit, selectedPriorities.toList())
                    },
                    modifier = Modifier.fillMaxWidth().testTag("generate_mesocycle_btn")
                ) {
                    Text("Згенерувати та активувати")
                }
            }
        }
    }
}

@Composable
fun ProgramDetailsDialog(
    template: ProgramTemplate,
    onStart: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(template.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(template.descriptionUk, style = MaterialTheme.typography.bodyMedium)

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = IceCyanContainer.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IceCyanPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("🔬 Наукове обґрунтування:", fontWeight = FontWeight.Bold, color = IceCyanPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(template.scientificRationaleUk, style = MaterialTheme.typography.bodySmall)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Запустити цей план")
                }
            }
        }
    }
}
