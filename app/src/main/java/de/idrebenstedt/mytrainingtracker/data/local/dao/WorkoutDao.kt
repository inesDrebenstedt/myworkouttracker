package de.idrebenstedt.mytrainingtracker.data.local.dao

import androidx.room.*
import de.idrebenstedt.mytrainingtracker.data.local.entities.ExerciseEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.SetEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutExerciseEntity
import kotlinx.coroutines.flow.Flow

data class WorkoutExerciseRow(
    val id: Long,
    val workoutId: Long,
    val exerciseId: Long,
    val exerciseName: String,
    val sequenceOrder: Int
)

@Dao
interface WorkoutDao {

    // Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExercise(exercise: ExerciseEntity): Long

    @Update
    suspend fun updateExercise(exercise: ExerciseEntity)

    @Delete
    suspend fun deleteExercise(exercise: ExerciseEntity)

    @Query("SELECT * FROM exercises ORDER BY name ASC")
    fun getAllExercises(): Flow<List<ExerciseEntity>>

    // Workouts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(workout: WorkoutEntity): Long

    @Update
    suspend fun updateWorkout(workout: WorkoutEntity)

    @Delete
    suspend fun deleteWorkout(workout: WorkoutEntity)

    @Query("SELECT * FROM workouts ORDER BY date DESC")
    fun getAllWorkouts(): Flow<List<WorkoutEntity>>

    // Workout Exercises
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercise(workoutExercise: WorkoutExerciseEntity): Long

    @Update
    suspend fun updateWorkoutExercise(workoutExercise: WorkoutExerciseEntity)

    @Delete
    suspend fun deleteWorkoutExercise(workoutExercise: WorkoutExerciseEntity)

    @Query("""SELECT we.id, we.workoutId, we.exerciseId, e.name AS exerciseName, we.sequenceOrder
        FROM workout_exercises we INNER JOIN exercises e ON e.id = we.exerciseId
        WHERE we.workoutId = :workoutId ORDER BY we.sequenceOrder ASC""")
    fun getWorkoutExercises(workoutId: Long): Flow<List<WorkoutExerciseRow>>

    // Sets
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSet(set: SetEntity): Long

    @Update
    suspend fun updateSet(set: SetEntity)

    @Delete
    suspend fun deleteSet(set: SetEntity)

    @Query("SELECT * FROM sets WHERE workoutExerciseId = :workoutExerciseId ORDER BY id ASC")
    fun getSets(workoutExerciseId: Long): Flow<List<SetEntity>>
}
