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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import de.idrebenstedt.mytrainingtracker.data.local.AppDatabase
import de.idrebenstedt.mytrainingtracker.data.local.dao.WorkoutExerciseRow
import de.idrebenstedt.mytrainingtracker.data.local.entities.*
import de.idrebenstedt.mytrainingtracker.data.repository.WorkoutRepository
import de.idrebenstedt.mytrainingtracker.ui.theme.MyTrainingTrackerTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repository = WorkoutRepository(AppDatabase.getDatabase(applicationContext).workoutDao())
        setContent { MyTrainingTrackerTheme { TrainingTrackerApp(repository) { finishAndRemoveTask() } } }
    }
}

private enum class HomeTab { WORKOUTS, EXERCISES }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TrainingTrackerApp(repository: WorkoutRepository, onExit: () -> Unit) {
    var workoutsHomeTab by remember { mutableStateOf(HomeTab.WORKOUTS) }
    var openWorkoutId by remember { mutableStateOf<Long?>(null) }
    var showExitConfirmation by remember { mutableStateOf(false) }
    val workouts by repository.allWorkouts.collectAsState(emptyList())
    val openWorkout = workouts.firstOrNull { it.id == openWorkoutId }
    if (openWorkout != null) WorkoutDetail(openWorkout, repository) { openWorkoutId = null }
    else Scaffold(
        topBar = { TopAppBar(title = { Text("My Training Tracker") }, actions = {
            Button(
                onClick = { showExitConfirmation = true },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF8A00), contentColor = Color.Black)
            ) { Text("Close app") }
        }) },
        bottomBar = { NavigationBar {
            NavigationBarItem(workoutsHomeTab == HomeTab.WORKOUTS, { workoutsHomeTab = HomeTab.WORKOUTS }, { Text("▣") }, label = { Text("Workouts") })
            NavigationBarItem(workoutsHomeTab == HomeTab.EXERCISES, { workoutsHomeTab = HomeTab.EXERCISES }, { Text("◉") }, label = { Text("Exercises") })
        } }
    ) { padding -> Box(Modifier.padding(padding)) {
        if (workoutsHomeTab == HomeTab.WORKOUTS) WorkoutList(workouts, repository) { openWorkoutId = it.id } else ExerciseList(repository)
    } }
    if (showExitConfirmation) AlertDialog(
        onDismissRequest = { showExitConfirmation = false },
        title = { Text("Close My Training Tracker?") },
        text = { Text("Your saved workouts and exercises will remain available when you open the app again.") },
        confirmButton = { Button(onClick = onExit) { Text("Close app") } },
        dismissButton = { TextButton(onClick = { showExitConfirmation = false }) { Text("Cancel") } }
    )
}

@Composable
private fun WorkoutList(workouts: List<WorkoutEntity>, repository: WorkoutRepository, onOpen: (WorkoutEntity) -> Unit) {
    val scope = rememberCoroutineScope(); var editing by remember { mutableStateOf<WorkoutEntity?>(null) };
    var adding by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Workouts", "New workout") { adding = true }
        if (workouts.isEmpty()) EmptyState("No workouts yet", "Create a workout to start logging your training.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 16.dp)) {
            items(workouts, key = { it.id }) { workout -> ElevatedCard(onClick = { onOpen(workout) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(workout.name?.ifBlank { "Untitled workout" } ?: "Untitled workout", style = MaterialTheme.typography.titleMedium)
                    Text(formatDate(workout.date))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                        Button({ onOpen(workout) }) { Text("Open") }
                        TextButton({ editing = workout }) { Text("Edit") }
                        TextButton({ scope.launch { repository.deleteWorkout(workout) } }) { Text("Delete") }
                    }
                }
            } }
        }
    }
    if (adding) WorkoutDialog(onDismiss = { adding = false }) { name, date -> scope.launch { repository.insertWorkout(WorkoutEntity(name = name.ifBlank { null }, date = date)); adding = false } }
    editing?.let { workout -> WorkoutDialog(workout, { editing = null }) { name, date -> scope.launch { repository.updateWorkout(workout.copy(name = name.ifBlank { null }, date = date)); editing = null } } }
}

@Composable
private fun ExerciseList(repository: WorkoutRepository) {
    val exercises by repository.allExercises.collectAsState(emptyList()); val scope = rememberCoroutineScope(); var adding by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<ExerciseEntity?>(null) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Header("Exercise library", "New exercise") { adding = true }
        if (exercises.isEmpty()) EmptyState("Your library is empty", "Add exercises before putting them in a workout.") else LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 16.dp)) {
            items(exercises, key = { it.id }) { exercise -> ElevatedCard(Modifier.fillMaxWidth()) { Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(exercise.name, style = MaterialTheme.typography.titleMedium); Text(exercise.muscleGroups.joinToString().ifEmpty { "No muscle groups" }) }
                TextButton({ editing = exercise }) { Text("Edit") }; TextButton({ scope.launch { repository.deleteExercise(exercise) } }) { Text("Delete") }
            } } }
        }
    }
    if (adding) ExerciseDialog(onDismiss = { adding = false }) { name, groups -> scope.launch { repository.insertExercise(ExerciseEntity(name = name, muscleGroups = groups)); adding = false } }
    editing?.let { exercise -> ExerciseDialog(exercise, { editing = null }) { name, groups -> scope.launch { repository.updateExercise(exercise.copy(name = name, muscleGroups = groups)); editing = null } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutDetail(workout: WorkoutEntity, repository: WorkoutRepository, onBack: () -> Unit) {
    val rows by repository.workoutExercises(workout.id).collectAsState(emptyList()); val exercises by repository.allExercises.collectAsState(emptyList()); val scope = rememberCoroutineScope(); var addLink by remember { mutableStateOf(false) }; var createExercise by remember { mutableStateOf(false) }; var editLink by remember { mutableStateOf<WorkoutExerciseRow?>(null) }
    Scaffold(topBar = { TopAppBar(title = { Column { Text(workout.name ?: "Untitled workout"); Text(formatDate(workout.date), style = MaterialTheme.typography.labelMedium) } }, navigationIcon = { TextButton(onBack) { Text("Back") } }) }) { padding -> Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Exercises", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ createExercise = true }) { Text("New") }
                Button({ addLink = true }, enabled = exercises.isNotEmpty()) { Text("Add existing") }
            }
        }
        if (rows.isEmpty()) EmptyState("No exercises in this workout", "Create a new exercise or add one from your library.")
        else LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 16.dp)) { items(rows, key = { it.id }) { row -> WorkoutExerciseCard(row, repository, { editLink = row }, { scope.launch { repository.deleteWorkoutExercise(WorkoutExerciseEntity(row.id, row.workoutId, row.exerciseId, row.sequenceOrder)) } }) } }
    } }
    if (addLink) WorkoutExerciseDialog(exercises = exercises, onDismiss = { addLink = false }) { exerciseId, order -> scope.launch { repository.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workout.id, exerciseId = exerciseId, sequenceOrder = order)); addLink = false } }
    if (createExercise) ExerciseDialog(onDismiss = { createExercise = false }) { name, groups -> scope.launch {
        val exerciseId = repository.insertExercise(ExerciseEntity(name = name, muscleGroups = groups))
        repository.insertWorkoutExercise(WorkoutExerciseEntity(workoutId = workout.id, exerciseId = exerciseId, sequenceOrder = (rows.maxOfOrNull { it.sequenceOrder } ?: 0) + 1))
        createExercise = false
    } }
    editLink?.let { row -> WorkoutExerciseDialog(WorkoutExerciseEntity(row.id, row.workoutId, row.exerciseId, row.sequenceOrder), exercises, { editLink = null }) { exerciseId, order -> scope.launch { repository.updateWorkoutExercise(WorkoutExerciseEntity(row.id, row.workoutId, exerciseId, order)); editLink = null } } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorkoutExerciseCard(row: WorkoutExerciseRow, repository: WorkoutRepository, onEdit: () -> Unit, onDelete: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val sets by repository.sets(row.id).collectAsState(emptyList()); val scope = rememberCoroutineScope(); var adding by remember { mutableStateOf(false) }; var editing by remember { mutableStateOf<SetEntity?>(null) }
    ElevatedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp).animateContentSize()) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(row.exerciseName, style = MaterialTheme.typography.titleLarge)
                    Text("Exercise #${row.sequenceOrder}", style = MaterialTheme.typography.labelMedium)
                }
                TextButton(onEdit) { Text("Edit") }
                TextButton(onDelete) { Text("Delete") }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(Modifier.height(8.dp))
                    if (sets.isEmpty()) Text("No sets logged") else {
                        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
                            Text("REPS", Modifier.weight(0.22f), style = MaterialTheme.typography.labelSmall)
                            Text("TYPE", Modifier.weight(0.38f), style = MaterialTheme.typography.labelSmall)
                            Text("WEIGHT", Modifier.weight(0.40f), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                    sets.forEach { set -> Column(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(set.repsValue, Modifier.weight(0.22f), style = MaterialTheme.typography.bodyLarge)
                            Text(set.repsLabel?.ifBlank { "normal" } ?: "normal", Modifier.weight(0.38f), style = MaterialTheme.typography.bodyLarge)
                            Text(set.weight?.let { "$it kg" } ?: "—", Modifier.weight(0.40f), style = MaterialTheme.typography.bodyLarge)
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton({ editing = set }) { Text("Edit") }; TextButton({ scope.launch { repository.deleteSet(set) } }) { Text("Delete") } }
                    } }
                    TextButton({ adding = true }, Modifier.align(Alignment.End)) { Text("+ Add set") }
                }
            }
        }
    }
    if (adding) SetDialog(onDismiss = { adding = false }) { reps, label, weight -> scope.launch { repository.insertSet(SetEntity(workoutExerciseId = row.id, repsValue = reps, repsLabel = label.ifBlank { null }, weight = weight)); adding = false } }
    editing?.let { set -> SetDialog(set, { editing = null }) { reps, label, weight -> scope.launch { repository.updateSet(set.copy(repsValue = reps, repsLabel = label.ifBlank { null }, weight = weight)); editing = null } } }
}

@Composable private fun Header(title: String, button: String, enabled: Boolean = true, click: () -> Unit) = Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) { Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Button(click, enabled = enabled) { Text(button) } }
@Composable private fun EmptyState(title: String, message: String) = Column(Modifier.fillMaxWidth().padding(vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) { Text(title, style = MaterialTheme.typography.titleMedium); Text(message) }

@Composable
private fun WorkoutDialog(existing: WorkoutEntity? = null, onDismiss: () -> Unit, onSave: (String, Long) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }; var dateText by remember(existing) { mutableStateOf(existing?.let { formatDate(it.date) } ?: formatDate(System.currentTimeMillis())) }
    AlertDialog(onDismiss, title = { Text(if (existing == null) "New workout" else "Edit workout") }, text = { Column { Field("Workout name (optional)", name) { name = it }; Field("Date (yyyy-MM-dd)", dateText) { dateText = it } } }, confirmButton = { Button({ parseDate(dateText)?.let { onSave(name.trim(), it) } }, enabled = parseDate(dateText) != null) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun ExerciseDialog(existing: ExerciseEntity? = null, onDismiss: () -> Unit, onSave: (String, List<String>) -> Unit) {
    var name by remember(existing) { mutableStateOf(existing?.name.orEmpty()) }; var groups by remember(existing) { mutableStateOf(existing?.muscleGroups?.joinToString(", ").orEmpty()) }
    AlertDialog(onDismiss, title = { Text(if (existing == null) "New exercise" else "Edit exercise") }, text = { Column { Field("Exercise name", name) { name = it }; Field("Muscle groups (comma-separated)", groups) { groups = it } } }, confirmButton = { Button({ onSave(name.trim(), groups.split(',').map { it.trim() }.filter { it.isNotEmpty() }) }, enabled = name.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun WorkoutExerciseDialog(existing: WorkoutExerciseEntity? = null, exercises: List<ExerciseEntity>, onDismiss: () -> Unit, onSave: (Long, Int) -> Unit) {
    var exerciseId by remember(existing, exercises) { mutableStateOf(existing?.exerciseId ?: exercises.firstOrNull()?.id ?: 0L) }; var order by remember(existing) { mutableStateOf(existing?.sequenceOrder?.toString() ?: "1") }
    AlertDialog(onDismiss, title = { Text(if (existing == null) "Add exercise" else "Edit exercise") }, text = { Column { Text("Exercise", style = MaterialTheme.typography.labelLarge); exercises.forEach { exercise -> Row(verticalAlignment = Alignment.CenterVertically) { RadioButton(exerciseId == exercise.id, { exerciseId = exercise.id }); Text(exercise.name) } }; Field("Order", order) { order = it } } }, confirmButton = { Button({ onSave(exerciseId, order.toIntOrNull() ?: 1) }, enabled = exerciseId != 0L) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable
private fun SetDialog(existing: SetEntity? = null, onDismiss: () -> Unit, onSave: (String, String, Double?) -> Unit) {
    var reps by remember(existing) { mutableStateOf(existing?.repsValue.orEmpty()) }; var label by remember(existing) { mutableStateOf(existing?.repsLabel.orEmpty()) }; var weight by remember(existing) { mutableStateOf(existing?.weight?.toString().orEmpty()) }
    AlertDialog(onDismiss, title = { Text(if (existing == null) "Add set" else "Edit set") }, text = { Column {
        Text("Set details", style = MaterialTheme.typography.labelLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(reps, { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(label, { label = it }, label = { Text("Label") }, modifier = Modifier.weight(1f), singleLine = true)
            OutlinedTextField(weight, { weight = it }, label = { Text("kg") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        Text("For example: 10 | left side | 22.5", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
    } }, confirmButton = { Button({ onSave(reps.trim(), label.trim(), weight.toDoubleOrNull()) }, enabled = reps.isNotBlank()) { Text("Save") } }, dismissButton = { TextButton(onDismiss) { Text("Cancel") } })
}

@Composable private fun Field(label: String, value: String, change: (String) -> Unit) = OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), singleLine = true)
private fun formatDate(value: Long) = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(value))
private fun parseDate(value: String): Long? = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).apply { isLenient = false }.parse(value)?.time }.getOrNull()
private fun setDescription(set: SetEntity) = buildString { append(set.repsValue); if (!set.repsLabel.isNullOrBlank()) append(" ${set.repsLabel}"); append(" reps"); set.weight?.let { append(" · $it kg") } }
