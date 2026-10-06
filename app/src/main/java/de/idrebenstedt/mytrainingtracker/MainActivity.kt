package de.idrebenstedt.mytrainingtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import de.idrebenstedt.mytrainingtracker.data.local.AppDatabase
import de.idrebenstedt.mytrainingtracker.data.local.dao.WorkoutExerciseRow
import de.idrebenstedt.mytrainingtracker.data.local.entities.*
import de.idrebenstedt.mytrainingtracker.data.repository.WorkoutRepository
import de.idrebenstedt.mytrainingtracker.ui.WorkoutViewModel
import de.idrebenstedt.mytrainingtracker.ui.theme.MyTrainingTrackerTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = WorkoutRepository(AppDatabase.getDatabase(applicationContext).workoutDao())
        setContent {
            MyTrainingTrackerTheme {
                val viewModel: WorkoutViewModel = viewModel(
                    factory = WorkoutViewModel.provideFactory(repository)
                )
                TrainingTrackerApp(
                    viewModel = viewModel,
                    onExit = { finishAndRemoveTask() },
                )
            }
        }
    }
}

private enum class HomeTab { WORKOUTS, EXERCISES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingTrackerApp(viewModel: WorkoutViewModel, onExit: () -> Unit) {
    var workoutsHomeTab by rememberSaveable { mutableStateOf(HomeTab.WORKOUTS) }
    var openWorkoutId by rememberSaveable { mutableStateOf<Long?>(null) }
    var showExitConfirmation by rememberSaveable { mutableStateOf(false) }

    val workouts by viewModel.allWorkouts.collectAsState()

    if (openWorkoutId != null) {
        val openWorkout = workouts.firstOrNull { it.id == openWorkoutId }
        if (openWorkout != null) {
            WorkoutDetail(
                workout = openWorkout,
                viewModel = viewModel,
                onBack = { openWorkoutId = null },
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("Loading...") },
                        navigationIcon = {
                            TextButton(
                                onClick = { openWorkoutId = null },
                                modifier = Modifier.testTag("DetailBackButton")
                            ) { Text("Back") }
                        },
                    )
                },
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(Modifier.testTag("LoadingSpinner"))
                }
            }
        }
    } else {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("My Training Tracker") },
                    actions = {
                        Button(
                            onClick = { showExitConfirmation = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF8A00),
                                contentColor = Color.Black,
                            ),
                            modifier = Modifier.testTag("ExitButton")
                        ) {
                            Text("Close app")
                        }
                    },
                )
            },
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = workoutsHomeTab == HomeTab.WORKOUTS,
                        onClick = { workoutsHomeTab = HomeTab.WORKOUTS },
                        icon = { Text("▣") },
                        label = { Text("Workouts") },
                        modifier = Modifier.testTag("WorkoutsTab")
                    )
                    NavigationBarItem(
                        selected = workoutsHomeTab == HomeTab.EXERCISES,
                        onClick = { workoutsHomeTab = HomeTab.EXERCISES },
                        icon = { Text("◉") },
                        label = { Text("Exercises") },
                        modifier = Modifier.testTag("ExercisesTab")
                    )
                }
            },
        ) { padding ->
            Box(Modifier.padding(padding)) {
                if (workoutsHomeTab == HomeTab.WORKOUTS) {
                    WorkoutList(
                        workouts = workouts,
                        viewModel = viewModel,
                        onOpen = { openWorkoutId = it.id },
                    )
                } else {
                    ExerciseList(viewModel = viewModel)
                }
            }
        }
    }

    if (showExitConfirmation) {
        AlertDialog(
            onDismissRequest = { showExitConfirmation = false },
            title = { Text("Close My Training Tracker?") },
            text = { Text("Your saved workouts and exercises will remain available when you open the app again.") },
            confirmButton = {
                Button(
                    onClick = onExit,
                    modifier = Modifier.testTag("ConfirmExitButton")
                ) { Text("Close app") }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmation = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun WorkoutList(
    workouts: List<WorkoutEntity>,
    viewModel: WorkoutViewModel,
    onOpen: (WorkoutEntity) -> Unit,
) {
    var editing by remember { mutableStateOf<WorkoutEntity?>(null) }
    var adding by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Header(
            title = "Workouts",
            button = "New workout",
            click = { adding = true },
        )
        if (workouts.isEmpty()) {
            EmptyState(
                title = "No workouts yet",
                message = "Create a workout to start logging your training.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp),
                modifier = Modifier.testTag("WorkoutList")
            ) {
                items(workouts, key = { it.id }) { workout ->
                    ElevatedCard(
                        onClick = { onOpen(workout) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("WorkoutCard_${workout.name}"),
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(
                                text = workout.name?.ifBlank { "Untitled workout" } ?: "Untitled workout",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(text = formatDate(workout.date))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Button(
                                    onClick = { onOpen(workout) },
                                    modifier = Modifier.testTag("OpenWorkout_${workout.name}")
                                ) {
                                    Text("Open")
                                }
                                TextButton(onClick = { editing = workout }) {
                                    Text("Edit")
                                }
                                TextButton(
                                    onClick = { viewModel.deleteWorkout(workout) },
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        WorkoutDialog(
            onDismiss = { adding = false },
            onSave = { name, date ->
                viewModel.addWorkout(name.ifBlank { null }, date)
                adding = false
            },
        )
    }

    editing?.let { workout ->
        WorkoutDialog(
            existing = workout,
            onDismiss = { editing = null },
            onSave = { name, date ->
                viewModel.updateWorkout(workout.copy(name = name.ifBlank { null }, date = date))
                editing = null
            },
        )
    }
}

@Composable
private fun ExerciseList(viewModel: WorkoutViewModel) {
    val exercises by viewModel.allExercises.collectAsState()
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ExerciseEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Header(
            title = "Exercise library",
            button = "New exercise",
            click = { adding = true },
        )
        if (exercises.isEmpty()) {
            EmptyState(
                title = "Your library is empty",
                message = "Add exercises before putting them in a workout.",
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 16.dp),
                modifier = Modifier.testTag("ExerciseList")
            ) {
                items(exercises, key = { it.id }) { exercise ->
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("ExerciseCard_${exercise.name}")
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = exercise.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = exercise.muscleGroups.joinToString().ifEmpty { "No muscle groups" },
                                )
                            }
                            TextButton(onClick = { editing = exercise }) {
                                Text("Edit")
                            }
                            TextButton(
                                onClick = { viewModel.deleteExercise(exercise) },
                            ) {
                                Text("Delete")
                            }
                        }
                    }
                }
            }
        }
    }

    if (adding) {
        ExerciseDialog(
            onDismiss = { adding = false },
            onSave = { name, groups ->
                viewModel.addExercise(name, groups)
                adding = false
            },
        )
    }

    editing?.let { exercise ->
        ExerciseDialog(
            existing = exercise,
            onDismiss = { editing = null },
            onSave = { name, groups ->
                viewModel.updateExercise(exercise.copy(name = name, muscleGroups = groups))
                editing = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDetail(
    workout: WorkoutEntity,
    viewModel: WorkoutViewModel,
    onBack: () -> Unit,
) {
    val rows by viewModel.workoutExercises(workout.id).collectAsState()
    val exercises by viewModel.allExercises.collectAsState()
    var addLink by rememberSaveable { mutableStateOf(false) }
    var createExercise by rememberSaveable { mutableStateOf(false) }
    var editLink by remember { mutableStateOf<WorkoutExerciseRow?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(text = workout.name ?: "Untitled workout")
                        Text(
                            text = formatDate(workout.date),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                },
                navigationIcon = {
                    TextButton(onClick = onBack, modifier = Modifier.testTag("BackToWorkouts")) { Text("Back") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Exercises",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { createExercise = true }) {
                        Text("New")
                    }
                    Button(
                        onClick = { addLink = true },
                        enabled = exercises.isNotEmpty(),
                        modifier = Modifier.testTag("AddExistingExerciseButton")
                    ) {
                        Text("Add existing")
                    }
                }
            }

            if (rows.isEmpty()) {
                EmptyState(
                    title = "No exercises in this workout",
                    message = "Create a new exercise or add one from your library.",
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(top = 16.dp),
                    modifier = Modifier.testTag("WorkoutExerciseList")
                ) {
                    items(rows, key = { it.id }) { row ->
                        WorkoutExerciseCard(
                            row = row,
                            viewModel = viewModel,
                            onEdit = { editLink = row },
                            onDelete = {
                                viewModel.deleteWorkoutExercise(
                                    WorkoutExerciseEntity(
                                        id = row.id,
                                        workoutId = row.workoutId,
                                        exerciseId = row.exerciseId,
                                        sequenceOrder = row.sequenceOrder,
                                    ),
                                )
                            },
                        )
                    }
                }
            }
        }
    }

    if (addLink) {
        WorkoutExerciseDialog(
            exercises = exercises,
            onDismiss = { addLink = false },
            onSave = { exerciseId, order ->
                viewModel.addWorkoutExercise(workout.id, exerciseId, order)
                addLink = false
            },
        )
    }

    if (createExercise) {
        ExerciseDialog(
            onDismiss = { createExercise = false },
            onSave = { name, groups ->
                val nextOrder = (rows.maxOfOrNull { it.sequenceOrder } ?: 0) + 1
                viewModel.createAndAddExerciseToWorkout(workout.id, name, groups, nextOrder)
                createExercise = false
            },
        )
    }

    editLink?.let { row ->
        WorkoutExerciseDialog(
            existing = WorkoutExerciseEntity(
                id = row.id,
                workoutId = row.workoutId,
                exerciseId = row.exerciseId,
                sequenceOrder = row.sequenceOrder,
            ),
            exercises = exercises,
            onDismiss = { editLink = null },
            onSave = { exerciseId, order ->
                viewModel.updateWorkoutExercise(
                    WorkoutExerciseEntity(
                        id = row.id,
                        workoutId = row.workoutId,
                        exerciseId = exerciseId,
                        sequenceOrder = order,
                    ),
                )
                editLink = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutExerciseCard(
    row: WorkoutExerciseRow,
    viewModel: WorkoutViewModel,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val sets by viewModel.sets(row.id).collectAsState()
    var adding by rememberSaveable { mutableStateOf(false) }
    var editing by remember { mutableStateOf<SetEntity?>(null) }

    ElevatedCard(
        onClick = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("WorkoutExerciseCard_${row.exerciseName}"),
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize(),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = row.exerciseName,
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        text = "Exercise #${row.sequenceOrder}",
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                )
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = onEdit) {
                    Text("Edit")
                }
                TextButton(onClick = onDelete) {
                    Text("Delete")
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    if (sets.isEmpty()) {
                        Text(text = "No sets logged")
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 2.dp),
                        ) {
                            Text(
                                text = "REPS",
                                modifier = Modifier.weight(0.22f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                text = "TYPE",
                                modifier = Modifier.weight(0.38f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                            Text(
                                text = "WEIGHT",
                                modifier = Modifier.weight(0.40f),
                                style = MaterialTheme.typography.labelSmall,
                            )
                        }
                    }
                    sets.forEach { set ->
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = set.repsValue,
                                    modifier = Modifier.weight(0.22f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = set.repsLabel?.ifBlank { "normal" } ?: "normal",
                                    modifier = Modifier.weight(0.38f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                                Text(
                                    text = set.weight?.let { "$it kg" } ?: "—",
                                    modifier = Modifier.weight(0.40f),
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                            ) {
                                TextButton(onClick = { editing = set }) {
                                    Text("Edit")
                                }
                                TextButton(
                                    onClick = { viewModel.deleteSet(set) },
                                ) {
                                    Text("Delete")
                                }
                            }
                        }
                    }
                    TextButton(
                        onClick = { adding = true },
                        modifier = Modifier
                            .align(Alignment.End)
                            .testTag("AddSetButton"),
                    ) {
                        Text("+ Add set")
                    }
                }
            }
        }
    }

    if (adding) {
        SetDialog(
            onDismiss = { adding = false },
            onSave = { reps, label, weight ->
                viewModel.addSet(row.id, reps, label.ifBlank { null }, weight)
                adding = false
            },
        )
    }

    editing?.let { set ->
        SetDialog(
            existing = set,
            onDismiss = { editing = null },
            onSave = { reps, label, weight ->
                viewModel.updateSet(
                    set.copy(
                        repsValue = reps,
                        repsLabel = label.ifBlank { null },
                        weight = weight,
                    ),
                )
                editing = null
            },
        )
    }
}

@Composable
private fun Header(
    title: String,
    button: String,
    enabled: Boolean = true,
    click: () -> Unit,
) = Row(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.SpaceBetween,
    verticalAlignment = Alignment.CenterVertically,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
    )
    Button(
        onClick = click,
        enabled = enabled,
        modifier = Modifier.testTag("New${title.replace(" ", "")}Button")
    ) {
        Text(text = button)
    }
}

@Composable
private fun EmptyState(title: String, message: String) = Column(
    modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 40.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
    )
    Text(text = message)
}

@Composable
private fun WorkoutDialog(
    existing: WorkoutEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, Long) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var dateText by remember(existing) {
        mutableStateOf(
            existing?.let { formatDate(it.date) } ?: formatDate(System.currentTimeMillis())
        )
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "New workout" else "Edit workout")
        },
        text = {
            Column {
                Field(
                    label = "Workout name (optional)",
                    value = name,
                    modifier = Modifier.testTag("WorkoutNameInput")
                ) {
                    name = it
                }
                Field(
                    label = "Date (yyyy-MM-dd)",
                    value = dateText,
                    modifier = Modifier.testTag("WorkoutDateInput")
                ) {
                    dateText = it
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { parseDate(dateText)?.let { onSave(name.trim(), it) } },
                enabled = parseDate(dateText) != null,
                modifier = Modifier.testTag("SaveWorkoutButton")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("CancelWorkoutButton")) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun ExerciseDialog(
    existing: ExerciseEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, List<String>) -> Unit,
) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }
    var groups by remember(existing) {
        mutableStateOf(existing?.muscleGroups?.joinToString(", ").orEmpty())
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "New exercise" else "Edit exercise")
        },
        text = {
            Column {
                Field(
                    label = "Exercise name",
                    value = name,
                    modifier = Modifier.testTag("ExerciseNameInput")
                ) {
                    name = it
                }
                Field(
                    label = "Muscle groups (comma-separated)",
                    value = groups,
                    modifier = Modifier.testTag("MuscleGroupsInput")
                ) {
                    groups = it
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        groups.split(',').map { it.trim() }.filter { it.isNotEmpty() },
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("SaveExerciseButton")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("CancelExerciseButton")) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun WorkoutExerciseDialog(
    existing: WorkoutExerciseEntity? = null,
    exercises: List<ExerciseEntity>,
    onDismiss: () -> Unit,
    onSave: (Long, Int) -> Unit,
) {
    var exerciseId by remember(existing, exercises) {
        mutableStateOf(existing?.exerciseId ?: exercises.firstOrNull()?.id ?: 0L)
    }
    var order by remember(existing) { mutableStateOf(existing?.sequenceOrder?.toString() ?: "1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Add exercise" else "Edit exercise")
        },
        text = {
            Column {
                Text(
                    text = "Exercise",
                    style = MaterialTheme.typography.labelLarge,
                )
                exercises.forEach { exercise ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = exerciseId == exercise.id,
                                onClick = { exerciseId = exercise.id },
                                role = Role.RadioButton
                            )
                            .testTag("ExerciseOption_${exercise.name}")
                            .padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = exerciseId == exercise.id,
                            onClick = null // Handled by row selectable
                        )
                        Text(text = exercise.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                Field(
                    label = "Order",
                    value = order,
                    modifier = Modifier.testTag("ExerciseOrderInput")
                ) {
                    order = it
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(exerciseId, order.toIntOrNull() ?: 1) },
                enabled = exerciseId != 0L,
                modifier = Modifier.testTag("SaveWorkoutExerciseButton")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("CancelWorkoutExerciseButton")) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun SetDialog(
    existing: SetEntity? = null,
    onDismiss: () -> Unit,
    onSave: (String, String, Double?) -> Unit,
) {
    var reps by remember(existing) { mutableStateOf(existing?.repsValue.orEmpty()) }
    var label by remember(existing) { mutableStateOf(existing?.repsLabel.orEmpty()) }
    var weight by remember(existing) { mutableStateOf(existing?.weight?.toString().orEmpty()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (existing == null) "Add set" else "Edit set")
        },
        text = {
            Column {
                Text(
                    text = "Set details",
                    style = MaterialTheme.typography.labelLarge,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("Reps") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("RepsInput"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = label,
                        onValueChange = { label = it },
                        label = { Text("Label") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("LabelInput"),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("kg") },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("WeightInput"),
                        singleLine = true,
                    )
                }
                Text(
                    text = "For example: 10 | left side | 22.5",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(reps.trim(), label.trim(), weight.toDoubleOrNull()) },
                enabled = reps.isNotBlank(),
                modifier = Modifier.testTag("SaveSetButton")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("CancelSetButton")) {
                Text("Cancel")
            }
        },
    )
}

@Composable
private fun Field(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    change: (String) -> Unit,
) = OutlinedTextField(
    value = value,
    onValueChange = change,
    label = { Text(text = label) },
    modifier = modifier
        .fillMaxWidth()
        .padding(vertical = 4.dp),
    singleLine = true,
)

private fun formatDate(value: Long) =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(value))

private fun parseDate(value: String): Long? = runCatching {
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }
        .parse(value)?.time
}.getOrNull()

private fun setDescription(set: SetEntity) = buildString {
    append(set.repsValue)
    if (!set.repsLabel.isNullOrBlank()) append(" ${set.repsLabel}")
    append(" reps")
    set.weight?.let { append(" · $it kg") }
}
