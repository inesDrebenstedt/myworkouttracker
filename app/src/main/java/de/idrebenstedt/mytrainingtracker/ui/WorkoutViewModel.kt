package de.idrebenstedt.mytrainingtracker.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import de.idrebenstedt.mytrainingtracker.data.local.entities.ExerciseEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.SetEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutExerciseEntity
import de.idrebenstedt.mytrainingtracker.data.repository.WorkoutRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkoutViewModel(private val repository: WorkoutRepository) : ViewModel() {

    val allWorkouts: StateFlow<List<WorkoutEntity>> = repository.allWorkouts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExercises: StateFlow<List<ExerciseEntity>> = repository.allExercises
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun workoutExercises(workoutId: Long) = repository.workoutExercises(workoutId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun sets(workoutExerciseId: Long) = repository.sets(workoutExerciseId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun addWorkout(name: String?, date: Long) {
        viewModelScope.launch {
            repository.insertWorkout(WorkoutEntity(name = name, date = date))
        }
    }

    fun updateWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            repository.updateWorkout(workout)
        }
    }

    fun deleteWorkout(workout: WorkoutEntity) {
        viewModelScope.launch {
            repository.deleteWorkout(workout)
        }
    }

    fun addExercise(name: String, muscleGroups: List<String>) {
        viewModelScope.launch {
            repository.insertExercise(ExerciseEntity(name = name, muscleGroups = muscleGroups))
        }
    }

    fun updateExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repository.updateExercise(exercise)
        }
    }

    fun deleteExercise(exercise: ExerciseEntity) {
        viewModelScope.launch {
            repository.deleteExercise(exercise)
        }
    }

    fun addWorkoutExercise(workoutId: Long, exerciseId: Long, sequenceOrder: Int) {
        viewModelScope.launch {
            repository.insertWorkoutExercise(
                WorkoutExerciseEntity(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    sequenceOrder = sequenceOrder
                )
            )
        }
    }

    fun createAndAddExerciseToWorkout(workoutId: Long, name: String, muscleGroups: List<String>, sequenceOrder: Int) {
        viewModelScope.launch {
            val exerciseId = repository.insertExercise(ExerciseEntity(name = name, muscleGroups = muscleGroups))
            repository.insertWorkoutExercise(
                WorkoutExerciseEntity(
                    workoutId = workoutId,
                    exerciseId = exerciseId,
                    sequenceOrder = sequenceOrder
                )
            )
        }
    }

    fun updateWorkoutExercise(workoutExercise: WorkoutExerciseEntity) {
        viewModelScope.launch {
            repository.updateWorkoutExercise(workoutExercise)
        }
    }

    fun deleteWorkoutExercise(workoutExercise: WorkoutExerciseEntity) {
        viewModelScope.launch {
            repository.deleteWorkoutExercise(workoutExercise)
        }
    }

    fun addSet(workoutExerciseId: Long, repsValue: String, repsLabel: String?, weight: Double?) {
        viewModelScope.launch {
            repository.insertSet(
                SetEntity(
                    workoutExerciseId = workoutExerciseId,
                    repsValue = repsValue,
                    repsLabel = repsLabel,
                    weight = weight
                )
            )
        }
    }

    fun updateSet(set: SetEntity) {
        viewModelScope.launch {
            repository.updateSet(set)
        }
    }

    fun deleteSet(set: SetEntity) {
        viewModelScope.launch {
            repository.deleteSet(set)
        }
    }

    companion object {
        fun provideFactory(repository: WorkoutRepository): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return WorkoutViewModel(repository) as T
            }
        }
    }
}
