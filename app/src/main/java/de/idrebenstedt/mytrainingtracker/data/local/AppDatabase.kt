package de.idrebenstedt.mytrainingtracker.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import de.idrebenstedt.mytrainingtracker.data.local.converters.Converters
import de.idrebenstedt.mytrainingtracker.data.local.dao.WorkoutDao
import de.idrebenstedt.mytrainingtracker.data.local.entities.ExerciseEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.SetEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutEntity
import de.idrebenstedt.mytrainingtracker.data.local.entities.WorkoutExerciseEntity

@Database(
    entities = [
        ExerciseEntity::class,
        WorkoutEntity::class,
        WorkoutExerciseEntity::class,
        SetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "workout_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
