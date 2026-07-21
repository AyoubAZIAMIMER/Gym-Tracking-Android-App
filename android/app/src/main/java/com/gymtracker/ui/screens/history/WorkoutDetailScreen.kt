// Purpose: Workout detail subpage — every set of one past session, PR stars, repeat action
// Inputs: WorkoutDetailViewModel (route arg workoutId)
// Outputs: onBack, onOpenExercise(exerciseId), onRepeat(workoutId)
package com.gymtracker.ui.screens.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.utils.PlateCalculator

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit = {},
    onOpenExercise: (String) -> Unit = {},
    onRepeat: (String) -> Unit = {},
    vm: WorkoutDetailViewModel = viewModel(),
) {
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = state.title,
                    style = MaterialTheme.typography.headlineMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = state.dateLine,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GlassSurface {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    Stat(state.durationText ?: "—", "duration")
                    Stat("${state.totalSets}", "sets")
                    Stat(state.totalVolume, "kg volume")
                    if (state.prCount > 0) Stat("★ ${state.prCount}", "PRs", gold = true)
                }
            }

            if (state.comment.isNotBlank()) {
                GlassSurface {
                    Text(
                        text = state.comment,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }

            state.exercises.forEach { ex -> ExerciseCard(ex, onOpenExercise) }

            GlassSurface(onClick = { onRepeat(vm.workoutId) }) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        Icons.Rounded.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Repeat this workout",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(24.dp)
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    ex: WorkoutRepository.DetailExercise,
    onOpenExercise: (String) -> Unit,
) {
    GlassSurface(onClick = { onOpenExercise(ex.exerciseId) }) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(ex.name, style = MaterialTheme.typography.titleMedium)
            if (ex.muscles.isNotEmpty()) {
                Text(
                    text = ex.muscles,
                    style = MaterialTheme.typography.labelMedium,
                    color = GymTheme.colors.hint,
                )
            }
            var workingIdx = 0
            ex.sets.forEach { set ->
                val warmup = set.tag == "W"
                if (!warmup) workingIdx++
                val dim = MaterialTheme.colorScheme.onSurfaceVariant
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (warmup) "W" else "$workingIdx",
                        style = MaterialTheme.typography.labelMedium,
                        color = GymTheme.colors.hint,
                        modifier = Modifier.width(24.dp),
                    )
                    Text(
                        text = buildString {
                            append(set.weightKg?.let { "${PlateCalculator.fmt(it)} kg" } ?: "BW")
                            set.reps?.let { append(" × $it") }
                            set.tag?.takeIf { it != "W" }?.let { append("  ·  $it") }
                        },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFeatureSettings = FONT_FEATURE_TABULAR
                        ),
                        color = if (warmup) dim else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    if (set.isPr) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Personal record",
                            modifier = Modifier.size(16.dp),
                            tint = GymTheme.colors.prGold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String, gold: Boolean = false) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = FONT_FEATURE_TABULAR),
            color = if (gold) GymTheme.colors.prGold else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
