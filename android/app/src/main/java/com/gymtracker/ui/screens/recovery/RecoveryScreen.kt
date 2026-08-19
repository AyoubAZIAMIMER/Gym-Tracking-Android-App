// Purpose: Recovery / "Body" — rebuilt to the redesign prototype: one weighted READINESS
//          numeral in Anton over a heat bar, FRONT/BACK figures side by side, the
//          FRESH→FATIGUED legend, a BY MUSCLE list, and an ember-railed "what to train"
//          call. Flat and hairline-ruled — the body is the hero, not a card.
// Inputs: RecoveryViewModel (real history when imported/logged; sample fallback)
// Outputs: onStartDay(dayId) when the call strip is tapped
package com.gymtracker.ui.screens.recovery

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.ForgedBar
import com.gymtracker.ui.components.ForgedBlock
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.body.BodySide
import com.gymtracker.ui.components.body.MuscleBodyMap
import com.gymtracker.ui.components.body.slugFreshness
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.rememberEntered
import com.gymtracker.ui.components.forgeHero
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.StampLabel
import com.gymtracker.ui.theme.rollUpValue
import kotlin.math.roundToInt

@Composable
fun RecoveryScreen(
    onStartDay: (String) -> Unit = {},
    vm: RecoveryViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val entered = rememberEntered()
    LaunchedEffect(Unit) { vm.refresh() }
    val heat = GymTheme.colors.heat

    GlowBackground(glowAlpha = 0.10f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle(
                title = "Body",
                trailing = if (state.isSample) "Sample data" else null,
            )

            // READINESS — one number, weighted, in the heat colour it actually reports
            ForgedBlock(0, entered) {
            SectionRule()
            ForgedSectionHeader(
                label = "READINESS",
                bottomPadding = 0.dp,
                trailing = {
                    Text(
                        text = "WEIGHTED BY MUSCLE SIZE",
                        style = StampLabel.copy(fontSize = 9.5.sp, letterSpacing = 1.1.sp),
                        color = GymTheme.colors.hint,
                    )
                },
            )
            val readiness = state.readinessPercent
            val rolled = rollUpValue(readiness.toFloat()).roundToInt()
            Row(
                Modifier.padding(start = Dim.screenPadH, end = Dim.screenPadH, top = 10.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$rolled",
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = 42.sp,
                            lineHeight = 42.sp,
                            fontFeatureSettings = FONT_FEATURE_TABULAR,
                        ),
                        color = heat.at(readiness / 100f),
                    )
                    Text(
                        text = "%",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
                        color = heat.at(readiness / 100f),
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Column(Modifier.weight(1f).padding(bottom = 6.dp)) {
                    Text(
                        text = "ready to train",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = state.readinessNote,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp),
                    )
                }
            }
            // 10 segments, each lit in its own position on the heat ramp — the design reads
            // readiness as a scale you climb, not a single bar filling
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dim.screenPadH, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val lit = (readiness / 10f).roundToInt()
                repeat(10) { i ->
                    Box(
                        Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                // the ramp runs COOLED (left) → GLOWING (right), matching the
                                // legend underneath; heat.at(1f) is "ready", so index inverts
                                if (i < lit) heat.at(1f - i / 9f)
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
            }

            }

            // FRONT / BACK figures, captioned in stamped mono
            ForgedBlock(1, entered) {
            SectionRule()
            // Owner's call: use the anatomy shipped with the Claude Design handoff
            // (ui/components/body/), not the repo's v5 physique. It draws one side, so front
            // and back sit side by side and this screen supplies the FRONT/BACK captions.
            // expands 10 canonical groups over ~40 slugs — recompute only when the data moves
            val slugs = remember(state.freshnessByMuscle) { slugFreshness(state.freshnessByMuscle) }
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dim.screenPadH, vertical = 18.dp)
                    .forgeHero()
                    .padding(vertical = 18.dp, horizontal = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                listOf(BodySide.Front to "FRONT", BodySide.Back to "BACK").forEach { (side, caption) ->
                    Column(
                        Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        MuscleBodyMap(
                            side = side,
                            freshness = slugs,
                            heatAt = { heat.at(it) },
                            silhouette = MaterialTheme.colorScheme.surfaceVariant,
                            outline = MaterialTheme.colorScheme.background,
                        )
                        Text(
                            text = caption,
                            style = StampLabel.copy(fontSize = 9.5.sp, letterSpacing = 1.5.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 9.dp),
                        )
                    }
                }
            }
            // FRESH ←→ FATIGUED — the body speaks in colour, this is its caption
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dim.screenPadH, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text("COOLED · READY", style = StampLabel, color = heat.ready)
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(heat.ready, heat.worn, heat.hot, heat.spent)
                            )
                        )
                )
                Text("GLOWING", style = StampLabel, color = heat.spent)
            }
            }

            // BY MUSCLE
            ForgedBlock(2, entered) {
            SectionRule()
            ForgedSectionHeader(
                label = "BY MUSCLE",
                trailing = {
                    Text(
                        text = "FRESH",
                        style = StampLabel.copy(fontSize = 9.5.sp, letterSpacing = 1.1.sp),
                        color = GymTheme.colors.hint,
                    )
                },
            )
            state.items.forEach { item ->
                RowRule()
                MuscleRow(item)
            }
            }

            // the call: ember-dim plate with a 3dp ember rail, exactly as the prototype frames it
            state.callTitle?.let { title ->
                SectionRule()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(enabled = state.callDayId != null) {
                            state.callDayId?.let(onStartDay)
                        }
                        .padding(horizontal = Dim.screenPadH, vertical = 15.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    // the rail itself
                    Box(
                        Modifier
                            .width(3.dp)
                            .height(38.dp)
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        state.callBody?.let {
                            Text(
                                text = it,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 2.dp),
                            )
                        }
                    }
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.navigationBarsPadding().height(Dim.listBottomSpacer))
        }
    }
}

@Composable
private fun MuscleRow(status: WorkoutRepository.MuscleFreshness) {
    val pct = status.freshnessPercent
    // heat is data: freshness is a temperature, never a hand-picked hue (INTEGRATION.md)
    val freshColor = GymTheme.colors.heat.at(pct / 100f)
    val rolledPct = rollUpValue(pct.toFloat()).roundToInt()
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dim.screenPadH, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // the dot carries the temperature; the name stays neutral so the list reads as a list
        Box(
            Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(freshColor)
        )
        Text(
            text = status.muscle,
            fontSize = 15.5.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        ForgedBar(progress = pct / 100f, color = freshColor, modifier = Modifier.width(96.dp))
        Text(
            text = "$rolledPct%",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 15.sp,
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            color = freshColor,
        )
    }
}
