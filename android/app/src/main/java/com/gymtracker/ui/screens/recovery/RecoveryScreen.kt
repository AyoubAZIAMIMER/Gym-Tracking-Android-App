// Purpose: Muscle recovery screen — the body map is the unambiguous hero; per-muscle
//          freshness renders as flat supporting rows with Hot Tip fills (UI refresh 1b)
// Inputs: RecoveryViewModel (real history when imported/logged; sample fallback)
// Outputs: none (read-only screen)
package com.gymtracker.ui.screens.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.FlatRow
import com.gymtracker.ui.components.ForgedBar
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.MuscleBodyMap
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedEntrance
import com.gymtracker.ui.theme.rollUpValue
import kotlin.math.roundToInt

@Composable
fun RecoveryScreen(vm: RecoveryViewModel = viewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()
    // §10: entrance plays once per screen entry, never on tab return
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Body", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = if (state.isSample) {
                    "Sample data — import your history or log workouts to see real freshness."
                } else {
                    "Readiness per muscle group — train what's recovered."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                Modifier.forgedEntrance(0, entered),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                BigStat(
                    value = state.daysSinceLast,
                    label = "days since your\nlast workout",
                    modifier = Modifier.weight(1f),
                )
                BigStat(
                    value = state.freshCount,
                    label = "fresh muscle\ngroups",
                    modifier = Modifier.weight(1f),
                )
            }

            // the hero — the only glass card on this screen's scroll body
            GlassSurface(modifier = Modifier.forgedEntrance(1, entered)) {
                Column {
                    MuscleBodyMap(
                        freshness = state.freshnessByMuscle,
                        modifier = Modifier.padding(top = 14.dp, start = 8.dp, end = 8.dp),
                    )
                    HeatLegend(
                        Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    )
                }
            }

            if (state.items.isNotEmpty()) {
                Text(
                    text = "BY MUSCLE",
                    style = MaterialTheme.typography.labelSmall,
                    color = GymTheme.colors.hint,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column {
                state.items.forEachIndexed { index, item ->
                    MuscleRow(
                        status = item,
                        last = index == state.items.lastIndex,
                        modifier = Modifier.forgedEntrance(index + 2, entered),
                    )
                }
            }

            // clearance for the floating bottom nav (measured, not the OS bar alone)
            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(112.dp)
            )
        }
    }
}

@Composable
private fun BigStat(value: Int?, label: String, modifier: Modifier = Modifier) {
    // §7.4: numbers count up on entry, capped at Motion.COUNT_UP
    val shown = value?.let { rollUpValue(it.toFloat()).roundToInt().toString() } ?: "–"
    GlassSurface(modifier = modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = shown,
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
private fun MuscleRow(
    status: WorkoutRepository.MuscleFreshness,
    last: Boolean,
    modifier: Modifier = Modifier,
) {
    val pct = status.freshnessPercent
    // Identity v5: freshness is a temperature — quenched steel when ready, glowing red
    // when just worked (design/IDENTITY_V5.md)
    val freshColor = GymTheme.colors.heat.at(pct / 100f)
    val rolledPct = rollUpValue(pct.toFloat()).roundToInt()
    FlatRow(modifier = modifier, divider = !last) {
        Column(Modifier.weight(1f)) {
            Text(status.muscle, style = MaterialTheme.typography.titleSmall)
            Text(
                text = "trained ${status.lastTrainedDaysAgo}d ago",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = "$rolledPct%",
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFeatureSettings = FONT_FEATURE_TABULAR
                ),
                color = freshColor,
            )
            ForgedBar(
                progress = pct / 100f,
                color = freshColor,
                modifier = Modifier.width(110.dp),
            )
        }
    }
}

// Heat-scale legend: the body speaks by color alone, this strip is its caption
@Composable
private fun HeatLegend(modifier: Modifier = Modifier) {
    val heat = GymTheme.colors.heat
    Row(
        modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "READY",
            style = MaterialTheme.typography.labelSmall,
            color = heat.ready,
        )
        Box(
            Modifier
                .weight(1f)
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(
                    Brush.horizontalGradient(
                        listOf(heat.ready, heat.worn, heat.hot, heat.spent)
                    )
                )
        )
        Text(
            text = "SPENT",
            style = MaterialTheme.typography.labelSmall,
            color = heat.spent,
        )
    }
}
