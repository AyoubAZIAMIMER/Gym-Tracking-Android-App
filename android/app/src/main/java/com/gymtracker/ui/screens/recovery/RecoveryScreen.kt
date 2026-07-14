// Purpose: Muscle recovery screen — Fitbod-style body map + per-muscle freshness bars
// Inputs: RecoveryViewModel (real history when imported/logged; sample fallback)
// Outputs: none (read-only screen)
package com.gymtracker.ui.screens.recovery

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.MuscleBodyMap
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme

@Composable
fun RecoveryScreen(vm: RecoveryViewModel = viewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Recovery", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = if (state.isSample) {
                    "Sample data — import your history or log workouts to see real freshness."
                } else {
                    "Muscles cool between sessions — strike where the metal is ready."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                BigStat(
                    value = state.daysSinceLast?.toString() ?: "–",
                    label = "days since your\nlast workout",
                    modifier = Modifier.weight(1f),
                )
                BigStat(
                    value = "${state.freshCount}",
                    label = "fresh muscle\ngroups",
                    modifier = Modifier.weight(1f),
                )
            }

            GlassSurface {
                MuscleBodyMap(
                    freshness = state.freshnessByMuscle,
                    modifier = Modifier.padding(vertical = 14.dp, horizontal = 8.dp),
                )
            }

            state.items.forEach { MuscleRow(it) }

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(88.dp) // clearance for the floating bottom nav
            )
        }
    }
}

@Composable
private fun BigStat(value: String, label: String, modifier: Modifier = Modifier) {
    GlassSurface(modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = FONT_FEATURE_TABULAR
                ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun MuscleRow(status: WorkoutRepository.MuscleFreshness) {
    val pct = status.freshnessPercent
    val freshColor = when {
        pct >= 80 -> GymTheme.colors.success
        pct >= 40 -> GymTheme.colors.prGold
        else -> MaterialTheme.colorScheme.error
    }
    GlassSurface {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(status.muscle, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "trained ${status.lastTrainedDaysAgo}d ago",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "$pct%",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFeatureSettings = FONT_FEATURE_TABULAR
                    ),
                    color = freshColor,
                )
                LinearProgressIndicator(
                    progress = { pct / 100f },
                    modifier = Modifier.width(110.dp),
                    color = freshColor,
                    trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                )
            }
        }
    }
}
