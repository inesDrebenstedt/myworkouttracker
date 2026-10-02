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
        // 1. Create a new exercise first
        composeTestRule.onNodeWithText("Exercises").performClick()
        composeTestRule.onNodeWithText("New exercise").performClick()
        composeTestRule.onNodeWithText("Exercise name").performTextInput("Squats")
        composeTestRule.onNodeWithText("Muscle groups (comma-separated)").performTextInput("Legs, Glutes")
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.onNodeWithText("Squats").assertIsDisplayed()

        // 2. Then go back to Workouts and create one
        composeTestRule.onNodeWithText("Workouts").performClick()
        composeTestRule.onNodeWithText("New workout").performClick()
        composeTestRule.onNodeWithText("Workout name (optional)").performTextInput("Leg Day")
        composeTestRule.onNodeWithText("Save").performClick()

        // 3. Open the new workout and add the previously created exercise
        composeTestRule.onNodeWithText("Leg Day").assertIsDisplayed()
        composeTestRule.onNodeWithText("Open").performClick()
        composeTestRule.onNodeWithText("Add existing").performClick()
        composeTestRule.onNodeWithText("Sissy Squats").performClick()
        // The dialog has an "Order" field, default is 1.
        composeTestRule.onNodeWithText("Order").assertTextContains("1")
        composeTestRule.onNodeWithText("Save").performClick()

        // 4. Verify exercise is in workout and expand it
        composeTestRule.onNodeWithText("Sissy Squats").assertIsDisplayed()
        composeTestRule.onNodeWithText("Exercise #1").assertIsDisplayed()
        // Click the card to expand (based on the clickable ElevatedCard I added)
        composeTestRule.onNodeWithText("Sissy Squats").performClick()

        // 5. Add a set
        composeTestRule.onNodeWithText("+ Add set").assertIsDisplayed()
        composeTestRule.onNodeWithText("+ Add set").performClick()
        
        composeTestRule.onNodeWithText("Reps").performTextInput("10")
        composeTestRule.onNodeWithText("kg").performTextInput("60")
        composeTestRule.onNodeWithText("Save").performClick()

        // 6. Verify set is displayed
        composeTestRule.onNodeWithText("10").assertIsDisplayed()
        composeTestRule.onNodeWithText("60.0 kg").assertIsDisplayed()
    }

    @Test
    fun testTabSwitching() {
        composeTestRule.onNodeWithText("Exercises").performClick()
        composeTestRule.onNodeWithText("Exercise library").assertIsDisplayed()

        composeTestRule.onNodeWithText("Workouts").performClick()
        composeTestRule.onNodeWithText("Workouts").assertIsDisplayed()
    }
}
