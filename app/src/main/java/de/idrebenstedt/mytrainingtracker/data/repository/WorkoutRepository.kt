package de.idrebenstedt.mytrainingtracker.data.repository

import de.idrebenstedt.mytrainingtracker.data.local.dao.WorkoutDao
import de.idrebenstedt.mytrainingtracker.data.local.dao.WorkoutExerciseRow
import de.idrebenstedt.mytrainingtracker.data.local.entities.ExerciseEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.SetEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

class WorkoutRepository(private val workoutDao: WorkoutDao) {

    val allExercises: Flow<List<ExerciseEntity>> = workoutDao.getAllExercises()
    val allWorkouts: Flow<List<WorkoutEntity>> = workoutDao.getAllWorkouts()

    suspend fun insertExercise(exercise: ExerciseEntity): Long = workoutDao.insertExercise(exercise)

    suspend fun updateExercise(exercise: ExerciseEntity) {
        workoutDao.updateExercise(exercise)
    }

    suspend fun deleteExercise(exercise: ExerciseEntity) {
        workoutDao.deleteExercise(exercise)
    }

    suspend fun insertWorkout(workout: WorkoutEntity): Long {
        return workoutDao.insertWorkout(workout)
    }

    suspend fun updateWorkout(workout: WorkoutEntity) = workoutDao.updateWorkout(workout)
    suspend fun deleteWorkout(workout: WorkoutEntity) = workoutDao.deleteWorkout(workout)

    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long {
        return workoutDao.insertWorkoutExercise(workoutExercise)
    }

    fun workoutExercises(workoutId: Long): Flow<List<WorkoutExerciseRow>> =
        workoutDao.getWorkoutExercises(workoutId)

    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExerciseEntity) =
        workoutDao.updateWorkoutExercise(workoutExercise)

    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExerciseEntity) =
        workoutDao.deleteWorkoutExercise(workoutExercise)

    suspend fun insertSet(set: SetEntity): Long {
        return workoutDao.insertSet(set)
    }

    fun sets(workoutExerciseId: Long): Flow<List<SetEntity>> = workoutDao.getSets(workoutExerciseId)
    suspend fun updateSet(set: SetEntity) = workoutDao.updateSet(set)
    suspend fun deleteSet(set: SetEntity) = workoutDao.deleteSet(set)
}
