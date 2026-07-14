// Purpose: Single-activity Compose host — navigation, floating glass bottom nav, catalog seeding
// Inputs: none
// Outputs: renders Home / Plan / Library / Recovery / Stats / Session / Data / subpages
package com.gymtracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.GlassBottomNav
import com.gymtracker.ui.screens.data.DataScreen
import com.gymtracker.ui.screens.history.HistoryScreen
import com.gymtracker.ui.screens.history.WorkoutDetailScreen
import com.gymtracker.ui.screens.home.HomeScreen
import com.gymtracker.ui.screens.library.ExerciseLibraryScreen
import com.gymtracker.ui.screens.plan.PlanScreen
import com.gymtracker.ui.screens.plan.ProgramEditorScreen
import com.gymtracker.ui.screens.recovery.RecoveryScreen
import com.gymtracker.ui.screens.session.WorkoutSessionScreen
import com.gymtracker.ui.screens.session.WorkoutSessionViewModel
import com.gymtracker.ui.screens.stats.ExerciseStatsScreen
import com.gymtracker.ui.screens.stats.StatsScreen
import com.gymtracker.ui.theme.GymTrackerTheme
import com.gymtracker.ui.theme.Motion
import kotlinx.coroutines.launch

// Forged Motion §11 — space is a workbench: tabs slide on one plane, details lift
// off the bench, the session rises from the bottom like stepping toward the forge.
private val TabOrder = listOf("home", "history", "plan", "library", "recovery", "stats")
private fun tabIndex(route: String?): Int = TabOrder.indexOf(route)
private fun isTab(route: String?): Boolean = tabIndex(route) >= 0

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // one-time seed of the built-in exercise catalog (no-op afterwards),
        // then fill muscles/descriptions for exercises that arrived without them
        lifecycleScope.launch {
            val repo = WorkoutRepository.get(this@MainActivity)
            repo.seedCatalogIfNeeded()
            repo.fillExerciseInfo()
        }
        setContent {
            GymTrackerTheme {
                AppNavHost()
            }
        }
    }
}

@Composable
private fun AppNavHost() {
    val nav = rememberNavController()
    val backStackEntry by nav.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    // Activity-scoped: an in-progress workout survives Back/app-switch (Home offers "Resume")
    val sessionVm: WorkoutSessionViewModel = viewModel()
    val sessionState by sessionVm.ui.collectAsStateWithLifecycle()

    fun startSession(programDayId: String?) {
        sessionVm.prepareStart(programDayId)
        nav.navigate("session")
    }

    Box(Modifier.fillMaxSize()) {
        NavHost(
            navController = nav,
            startDestination = "home",
            modifier = Modifier.fillMaxSize(),
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                when {
                    // tab → tab: 12-px lateral slide on the plane curve, crossfade
                    isTab(from) && isTab(to) -> {
                        val dir = if (tabIndex(to) > tabIndex(from)) 1 else -1
                        fadeIn(Motion.plane(180)) +
                            slideInHorizontally(Motion.plane(180)) { dir * it / 30 }
                    }
                    // stepping toward the forge: session rises from the bottom edge
                    to == "session" -> fadeIn(Motion.settle(Motion.DELIBERATE)) +
                        slideInVertically(Motion.settle(Motion.DELIBERATE)) { it / 8 }
                    // the lift: details rise 16 px off the bench
                    else -> fadeIn(Motion.settle(260)) +
                        slideInVertically(Motion.settle(260)) { it / 40 }
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (isTab(from) && isTab(to)) {
                    val dir = if (tabIndex(to) > tabIndex(from)) 1 else -1
                    fadeOut(Motion.plane(180)) +
                        slideOutHorizontally(Motion.plane(180)) { -dir * it / 30 }
                } else {
                    fadeOut(Motion.cool(160))
                }
            },
            popEnterTransition = { fadeIn(Motion.settle(200)) },
            popExitTransition = {
                // the set-down: exits are cheaper (~0.7×) and cool away
                val from = initialState.destination.route
                val dur = if (from == "session") Motion.STANDARD else 180
                fadeOut(Motion.cool(dur)) +
                    slideOutVertically(Motion.cool(dur)) { if (from == "session") it / 8 else it / 40 }
            },
        ) {
            composable("home") {
                HomeScreen(
                    onStartWorkout = { dayId -> startSession(dayId) },
                    sessionActive = sessionState.sessionActive,
                    onOpenData = { nav.navigate("data") },
                )
            }
            composable("history") {
                HistoryScreen(onOpenWorkout = { id -> nav.navigate("workout/$id") })
            }
            composable("workout/{workoutId}") {
                WorkoutDetailScreen(
                    onBack = { nav.popBackStack() },
                    onOpenExercise = { id -> nav.navigate("exercise/$id") },
                    onRepeat = { workoutId ->
                        sessionVm.prepareRepeat(workoutId)
                        nav.navigate("session")
                    },
                )
            }
            composable("plan") {
                PlanScreen(
                    onStartDay = { dayId -> startSession(dayId) },
                    onOpenProgram = { id -> nav.navigate("program/$id") },
                )
            }
            composable("library") {
                ExerciseLibraryScreen(onOpenExercise = { id -> nav.navigate("exercise/$id") })
            }
            composable("recovery") { RecoveryScreen() }
            composable("stats") {
                StatsScreen(onOpenExercise = { id -> nav.navigate("exercise/$id") })
            }
            composable("exercise/{exerciseId}") {
                ExerciseStatsScreen(onBack = { nav.popBackStack() })
            }
            composable("program/{programId}") {
                ProgramEditorScreen(onBack = { nav.popBackStack() })
            }
            composable("session") {
                WorkoutSessionScreen(onFinished = { nav.popBackStack() }, vm = sessionVm)
            }
            composable("data") { DataScreen(onBack = { nav.popBackStack() }) }
        }
        // session is full-focus; data/exercise/program pages are subpages: no bottom nav
        // (a subpage in the tab back stack would get saved/restored by tab switches)
        val isSubpage = route == "session" || route == "data" ||
            route?.startsWith("exercise/") == true || route?.startsWith("program/") == true ||
            route?.startsWith("workout/") == true
        if (!isSubpage) {
            GlassBottomNav(
                current = route,
                onSelect = { target ->
                    if (target != route) {
                        nav.navigate(target) {
                            popUpTo("home") { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(bottom = 10.dp),
            )
        }
    }
}
