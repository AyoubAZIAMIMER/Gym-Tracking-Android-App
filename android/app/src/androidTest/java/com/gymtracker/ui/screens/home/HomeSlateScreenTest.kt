// Purpose: Compose UI tests for the stateless HomeSlateScreen — the actual rendered Home
//          contract (plain data + callbacks), independent of HomeViewModel/Room
// Inputs: createComposeRule(), synthetic HomeUi fixtures
// Outputs: pass/fail signal for Home's hero CTA, empty-state, and row-tap wiring
package com.gymtracker.ui.screens.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.gymtracker.ui.theme.GymTrackerTheme
import org.junit.Rule
import org.junit.Test

class HomeSlateScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private fun fixture(
        hero: HeroUi = HeroUi(
            readinessTag = "CHEST READY", readinessFreshness = 0.95f,
            sessionName = "Push Day", meta = "3 exercises · about 45 min", ctaLabel = "Start",
        ),
        upcoming: List<UpcomingRowUi> = emptyList(),
        recent: List<RecentSessionUi> = emptyList(),
    ) = HomeUi(
        dateCaption = "Fri 17 Jul",
        weekProgress = 0.5f,
        weekDone = 2,
        weekGoal = 4,
        doneWeekdays = emptySet(),
        hero = hero,
        upcoming = upcoming,
        recent = recent,
    )

    private fun setContent(
        ui: HomeUi,
        onStart: () -> Unit = {},
        onOpenWorkout: (String) -> Unit = {},
    ) {
        composeRule.setContent {
            GymTrackerTheme {
                HomeSlateScreen(
                    ui = ui,
                    heatAt = { androidx.compose.ui.graphics.Color.Red },
                    onStart = onStart,
                    onOpenSettings = {},
                    onOpenPlan = {},
                    onOpenHistory = {},
                    onOpenWorkout = onOpenWorkout,
                )
            }
        }
    }

    @Test
    fun heroRendersSessionNameAndTappingStartInvokesTheCallback() {
        var started = false
        setContent(fixture(), onStart = { started = true })

        composeRule.onNodeWithText("Push Day").assertExists()
        composeRule.onNodeWithText("Start").performClick()

        assert(started) { "onStart was not invoked" }
    }

    @Test
    fun resumeLabelRendersInsteadOfStartWhenASessionIsLive() {
        val ui = fixture(
            hero = HeroUi(
                readinessTag = "CHEST READY", readinessFreshness = 0.95f,
                sessionName = "Push Day", meta = "1 of 6 sets logged", ctaLabel = "Resume",
            ),
        )
        setContent(ui)

        composeRule.onNodeWithText("Resume").assertExists()
        composeRule.onNodeWithText("Start").assertDoesNotExist()
    }

    @Test
    fun nextUpAndRecentAreHiddenWhenEmptyAndTheEmptyStateCopyShowsInstead() {
        setContent(fixture(upcoming = emptyList(), recent = emptyList()))

        composeRule.onNodeWithText("NEXT UP").assertDoesNotExist()
        composeRule.onNodeWithText("RECENT").assertDoesNotExist()
        composeRule.onNodeWithText("Nothing logged yet. Finish a session and your recovery, history " +
            "and stats fill in from there.").assertExists()
    }

    @Test
    fun nextUpRendersItsRowsWhenNonEmpty() {
        setContent(fixture(upcoming = listOf(UpcomingRowUi("Pull", "Back, Biceps", "Next"))))

        composeRule.onNodeWithText("NEXT UP").assertExists()
        composeRule.onNodeWithText("Pull").assertExists()
    }

    @Test
    fun tappingARecentRowReachesOnOpenWorkoutWithItsId() {
        var openedId: String? = null
        setContent(
            fixture(recent = listOf(RecentSessionUi(id = "w-42", name = "Leg Day", caption = "Mon · 58 min", volume = "4,120 kg"))),
            onOpenWorkout = { openedId = it },
        )

        composeRule.onNodeWithText("Leg Day").performClick()

        assert(openedId == "w-42") { "expected onOpenWorkout(\"w-42\"), got $openedId" }
    }
}
