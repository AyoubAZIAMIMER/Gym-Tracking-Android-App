// Purpose: Compose UI tests for the stateless SessionSlateScreen — the live-set logging
//          contract (plain data + callbacks), independent of WorkoutSessionViewModel/Room
// Inputs: createComposeRule(), synthetic SessionUi/SetRowUi fixtures
// Outputs: pass/fail signal for the active-set label, Complete-set CTA, and weight stepper wiring
package com.gymtracker.ui.screens.session

import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.gymtracker.ui.theme.GymTrackerTheme
import org.junit.Rule
import org.junit.Test

class SessionSlateScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fixture(
        sets: List<SetRowUi> = listOf(SetRowUi(index = 1, status = SetStatus.Active)),
        supersetTag: String? = null,
    ) = SessionUi(
        elapsed = "12:34",
        exerciseProgress = 0.3f,
        exerciseIndexLabel = "1",
        exerciseName = "Bench Press (Barbell)",
        muscleCaption = "Chest & triceps",
        supersetTag = supersetTag,
        setsLabel = "1 / 3",
        sets = sets,
        draftWeight = 60.0,
        draftReps = 8,
    )

    private fun setContent(
        ui: SessionUi,
        onCompleteSet: () -> Unit = {},
        onWeightDelta: (Int) -> Unit = {},
    ) {
        composeRule.setContent {
            GymTrackerTheme {
                SessionSlateScreen(
                    ui = ui,
                    heatAt = { androidx.compose.ui.graphics.Color.Red },
                    onBack = {},
                    onCompleteSet = onCompleteSet,
                    onWeightDelta = onWeightDelta,
                    onSetWeight = {},
                    onSetReps = {},
                    onRepsDelta = {},
                    onEffort = {},
                )
            }
        }
    }

    @Test
    fun theActiveSetRowReadsNow() {
        setContent(fixture(sets = listOf(SetRowUi(index = 1, status = SetStatus.Active))))

        composeRule.onNodeWithText("NOW").assertExists()
    }

    @Test
    fun tappingCompleteSetInvokesTheCallback() {
        var completed = false
        setContent(fixture(), onCompleteSet = { completed = true })

        composeRule.onNodeWithText("Complete set").performClick()

        assert(completed) { "onCompleteSet was not invoked" }
    }

    @Test
    fun tappingTheWeightStepperPlusAndMinusReportTheCorrectSign() {
        val deltas = mutableListOf<Int>()
        setContent(fixture(), onWeightDelta = { deltas += it })

        // WEIGHT is the first stepper in the logging bar, REPS the second — both render a
        // "+"/"−" pair, so the weight stepper's buttons are index 0 in each match list.
        composeRule.onAllNodesWithText("+")[0].performTouchInput { click() }
        composeRule.onAllNodesWithText("−")[0].performTouchInput { click() }

        assert(deltas == listOf(1, -1)) { "expected [1, -1], got $deltas" }
    }

    @Test
    fun aSupersetTagRendersNextToTheExerciseName() {
        setContent(fixture(supersetTag = "A1"))

        composeRule.onNodeWithText("A1").assertExists()
    }
}
