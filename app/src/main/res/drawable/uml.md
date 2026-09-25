classDiagram
class MainActivity {
+onCreate(Bundle)
}

    class TrainingTrackerApp {
        <<Composable>>
        -HomeTab tab
        -Long? openWorkoutId
        +TrainingTrackerApp(repository, onExit)
    }

    class WorkoutRepository {
        +Flow~List~ExerciseEntity~~ allExercises
        +Flow~List~WorkoutEntity~~ allWorkouts
        +insertExercise(ExerciseEntity)
        +insertWorkout(WorkoutEntity)
        +workoutExercises(Long) Flow~List~WorkoutExerciseRow~~
        +sets(Long) Flow~List~SetEntity~~
    }

    class AppDatabase {
        <<Abstract>>
        +workoutDao() WorkoutDao
        +getDatabase(Context) AppDatabase$
    }

    class WorkoutDao {
        <<Interface>>
        +getAllExercises() Flow
        +insertExercise() Long
        +insertWorkout() Long
        +insertSet() Long
    }

    class ExerciseEntity {
        +Long id
        +String name
        +List~String~ muscleGroups
    }

    class WorkoutEntity {
        +Long id
        +Long date
        +String? name
    }

    class WorkoutExerciseEntity {
        +Long id
        +Long workoutId
        +Long exerciseId
        +Int sequenceOrder
    }

    class SetEntity {
        +Long id
        +Long workoutExerciseId
        +String repsValue
        +String? repsLabel
        +Double? weight
    }

    class WorkoutExerciseRow {
        <<Data Object>>
        +Long id
        +String exerciseName
        +Int sequenceOrder
    }

    %% Relationships
    MainActivity ..> WorkoutRepository : instantiates
    TrainingTrackerApp --> WorkoutRepository : consumes
    WorkoutRepository --> WorkoutDao : delegates
    AppDatabase --> WorkoutDao : contains
    
    WorkoutEntity "1" --o "*" WorkoutExerciseEntity : parent (FK)
    ExerciseEntity "1" --o "*" WorkoutExerciseEntity : parent (FK)
    WorkoutExerciseEntity "1" --o "*" SetEntity : parent (FK)
    WorkoutExerciseRow ..> WorkoutExerciseEntity : UI projection