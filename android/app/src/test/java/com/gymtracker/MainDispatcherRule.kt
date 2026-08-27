// Purpose: JUnit4 rule swapping Dispatchers.Main for a test dispatcher in ViewModel tests
// Inputs: none (StandardTestDispatcher by default)
// Outputs: Dispatchers.Main set/reset around each test method
package com.gymtracker

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description

// Unconfined, not Standard: ViewModel init{} blocks fire coroutines on Dispatchers.Main
// the instant the ViewModel is constructed (before any runTest{} body runs), and their
// scheduler is this rule's, not runTest's own. Unconfined runs them eagerly inline —
// e.g. HomeViewModel's init collect{} — instead of needing them shared with runTest's
// scheduler for an explicit advanceUntilIdle() to reach them.
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    private val dispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        kotlinx.coroutines.Dispatchers.setMain(dispatcher)
    }

    override fun finished(description: Description) {
        kotlinx.coroutines.Dispatchers.resetMain()
    }
}
