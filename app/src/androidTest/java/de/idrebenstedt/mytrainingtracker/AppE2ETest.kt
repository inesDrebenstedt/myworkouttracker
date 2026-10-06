package de.idrebenstedt.mytrainingtracker

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppE2ETest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun testFullCreateWorkoutWithExerciseFlow() {
        // 1. Create a new exercise in the library
        composeTestRule.onNodeWithTag("ExercisesTab").performClick()
        composeTestRule.onNodeWithTag("NewExerciselibraryButton").performClick()
        composeTestRule.onNodeWithTag("ExerciseNameInput").performTextInput("Sissy Squats")
        composeTestRule.onNodeWithTag("MuscleGroupsInput").performTextInput("Legs, Glutes")
        composeTestRule.onNodeWithTag("SaveExerciseButton").performClick()
        
        // Match unique tag - no ambiguity with shared text
        composeTestRule.onNodeWithTag("ExerciseCard_Sissy Squats").assertIsDisplayed()

        // 2. Create a new workout
        composeTestRule.onNodeWithTag("WorkoutsTab").performClick()
        composeTestRule.onNodeWithTag("NewWorkoutsButton").performClick()
        composeTestRule.onNodeWithTag("WorkoutNameInput").performTextInput("Leg Day")
        composeTestRule.onNodeWithTag("SaveWorkoutButton").performClick()

        // 3. Open the workout. The unique tag WorkoutCard_Leg Day targets only the card.
        composeTestRule.onNodeWithTag("WorkoutCard_Leg Day").assertIsDisplayed()
        composeTestRule.onNodeWithTag("OpenWorkout_Leg Day").performClick()
        
        // 4. Add the existing exercise to the workout
        composeTestRule.onNodeWithTag("AddExistingExerciseButton").performClick()
        composeTestRule.onNodeWithTag("ExerciseOption_Sissy Squats").performClick()
        composeTestRule.onNodeWithTag("ExerciseOrderInput").assertTextContains("1")
        composeTestRule.onNodeWithTag("SaveWorkoutExerciseButton").performClick()

        // 5. Expand exercise and add a set
        composeTestRule.onNodeWithTag("WorkoutExerciseCard_Sissy Squats").assertIsDisplayed()
        composeTestRule.onNodeWithTag("WorkoutExerciseCard_Sissy Squats").performClick()

        composeTestRule.onNodeWithTag("AddSetButton").performClick()
        composeTestRule.onNodeWithTag("RepsInput").performTextInput("10")
        composeTestRule.onNodeWithTag("WeightInput").performTextInput("60")
        composeTestRule.onNodeWithTag("SaveSetButton").performClick()

        // 6. Verify set is displayed
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("60.0 kg").assertIsDisplayed()
    }

    @Test
    fun testTabSwitching() {
        composeTestRule.onNodeWithTag("WorkoutsTab").performClick()
        composeTestRule.onNodeWithTag("WorkoutList").assertIsDisplayed()

        composeTestRule.onNodeWithTag("ExercisesTab").performClick()
        composeTestRule.onNodeWithTag("ExerciseList").assertIsDisplayed()
    }
}
