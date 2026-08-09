// Purpose: Workout detail subpage — every set of one past session, PR stars, repeat action.
//          Flat sections + hairlines per the redesign; no cards (design/redesign-2026-07 README §4).
// Inputs: WorkoutDetailViewModel (route arg workoutId)
// Outputs: onBack, onOpenExercise(exerciseId), onRepeat(workoutId)
package com.gymtracker.ui.screens.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.StampText
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.utils.PlateCalculator

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit = {},
    onOpenExercise: (String) -> Unit = {},
    onRepeat: (String) -> Unit = {},
    vm: WorkoutDetailViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()

    GlowBackground(glowAlpha = 0.10f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle(state.title, trailing = state.dateLine, onBack = onBack)

            // Anton figures lead, the way History and Stats do — no glass tile row
            SectionRule()
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(
                        start = Dim.screenPadH, end = Dim.screenPadH, top = 18.dp, bottom = 20.dp,
                    ),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                Stat(state.durationText ?: "—", "duration")
                Stat("${state.totalSets}", "sets")
                Stat(state.totalVolume, "kg volume")
                if (state.prCount > 0) Stat("${state.prCount}", "PRs", gold = true)
            }

            if (state.comment.isNotBlank()) {
                SectionRule()
                ForgedSectionHeader("NOTE", bottomPadding = 6.dp)
                Text(
                    text = state.comment,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = Dim.screenPadH, end = Dim.screenPadH, bottom = 16.dp,
                    ),
                )
            }

            SectionRule()
            state.exercises.forEach { ex ->
                ExerciseBlock(ex, onOpenExercise)
            }

            SectionRule()
            RepeatRow(onClick = { onRepeat(vm.workoutId) })

            Spacer(Modifier.navigationBarsPadding().height(Dim.listBottomSpacer))
        }
    }
}

/** One exercise: stamped heading (tappable through to its stats), then its sets as flat rows. */
@Composable
private fun ExerciseBlock(
    ex: WorkoutRepository.DetailExercise,
    onOpenExercise: (String) -> Unit,
) {
    ForgedSectionHeader(
        label = ex.name.uppercase(),
        linkLabel = "Stats",
        onClickLink = { onOpenExercise(ex.exerciseId) },
        bottomPadding = 4.dp,
    )
    if (ex.muscles.isNotEmpty()) {
        Text(
            text = ex.muscles,
            fontSize = 12.sp,
            color = GymTheme.colors.hint,
            modifier = Modifier.padding(start = Dim.screenPadH, end = Dim.screenPadH, bottom = 8.dp),
        )
    }
    var workingIdx = 0
    ex.sets.forEach { set ->
        val warmup = set.tag == "W"
        if (!warmup) workingIdx++
        RowRule()
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dim.screenPadH, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // 44dp mono index column, same grid as the History session rows
            StampText(
                text = if (warmup) "W" else "$workingIdx",
                color = GymTheme.colors.hint,
                modifier = Modifier.width(44.dp),
            )
            Text(
                text = buildString {
                    append(set.weightKg?.let { "${PlateCalculator.fmt(it)} kg" } ?: "BW")
                    set.reps?.let { append(" × $it") }
                    set.tag?.takeIf { it != "W" }?.let { append("  ·  $it") }
                },
                fontSize = 15.sp,
                fontWeight = if (warmup) FontWeight.Normal else FontWeight.SemiBold,
                color = if (warmup) MaterialTheme.colorScheme.onSurfaceVariant
                else MaterialTheme.colorScheme.onSurface,
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

/** Full-width flat action, ember-lettered — the screen's only call to action. */
@Composable
private fun RepeatRow(onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .forgedPress(interaction, pressedScale = 0.99f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Dim.screenPadH, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Rounded.Replay,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Repeat this workout",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

/** Anton figure over a muted caption — the shared stat pair (History §HeadlineStat). */
@Composable
private fun Stat(value: String, label: String, gold: Boolean = false) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = if (gold) GymTheme.colors.prGold else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}
