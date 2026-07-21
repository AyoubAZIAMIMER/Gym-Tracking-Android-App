// Purpose: Strike Mode (UX v6 move 1) — full-screen view of the active set: giant
//          numerals readable mid-set, scrub-to-adjust with haptic detents (no keyboard),
//          one STRIKE surface to log. The table stays one gesture away.
// Inputs: the active exercise/set from WorkoutSessionUiState; scrub/strike callbacks
// Outputs: none (pure UI; all mutations flow through WorkoutSessionViewModel)
package com.gymtracker.ui.screens.session

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TableRows
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.domain.Progression
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.utils.OneRM
import com.gymtracker.utils.PlateCalculator
import kotlin.math.abs
import kotlin.math.roundToInt

/** The set currently under the hammer, resolved by the screen from activeSetId. */
data class ActiveStrike(
    val exercise: SessionExercise,
    val set: SessionSet,
    val setIndex: Int,
)

@Composable
fun StrikeModePanel(
    active: ActiveStrike,
    barKg: Double,
    topPadding: Dp,
    onScrubWeight: (exerciseId: Long, setId: Long, steps: Int) -> Unit,
    onScrubReps: (exerciseId: Long, setId: Long, steps: Int) -> Unit,
    onStrike: (exerciseId: Long, setId: Long) -> Unit,
    onOpenTable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // swipe up anywhere (outside the scrub zones) opens the full table
    val panelSwipe = Modifier.pointerInput(Unit) {
        var total = 0f
        detectVerticalDragGestures(
            onDragStart = { total = 0f },
            onVerticalDrag = { change, dy ->
                change.consume()
                total += dy
                if (total < -140.dp.toPx()) {
                    total = 0f
                    onOpenTable()
                }
            },
        )
    }

    // contentKey pins the transition to the set — scrub edits recompose without replaying it
    AnimatedContent(
        targetState = active,
        contentKey = { it.set.id },
        transitionSpec = {
            (slideInVertically(Motion.settle(Motion.STANDARD)) { it / 4 } +
                fadeIn(Motion.settle(Motion.STANDARD))) togetherWith
                (slideOutVertically(Motion.cool()) { -it / 4 } + fadeOut(Motion.cool()))
        },
        label = "strikeSet",
        modifier = modifier
            .fillMaxSize()
            .then(panelSwipe),
    ) { strike ->
        StrikeSet(
            strike = strike,
            barKg = barKg,
            topPadding = topPadding,
            onScrubWeight = { steps -> onScrubWeight(strike.exercise.id, strike.set.id, steps) },
            onScrubReps = { steps -> onScrubReps(strike.exercise.id, strike.set.id, steps) },
            onStrike = { onStrike(strike.exercise.id, strike.set.id) },
            onOpenTable = onOpenTable,
        )
    }
}

@Composable
private fun StrikeSet(
    strike: ActiveStrike,
    barKg: Double,
    topPadding: Dp,
    onScrubWeight: (Int) -> Unit,
    onScrubReps: (Int) -> Unit,
    onStrike: () -> Unit,
    onOpenTable: () -> Unit,
) {
    val exercise = strike.exercise
    val set = strike.set
    val haptics = LocalHapticFeedback.current
    val density = LocalDensity.current
    val weight = set.effectiveWeightKg
    val reps = set.effectiveReps

    Column(
        Modifier
            .fillMaxSize()
            .padding(top = topPadding, start = 24.dp, end = 24.dp)
            .navigationBarsPadding()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = buildString {
                append("SET ${strike.setIndex + 1} OF ${exercise.sets.size}")
                set.tag?.let { append(" · ${it.label.uppercase()}") }
            },
            style = MaterialTheme.typography.labelSmall,
            color = GymTheme.colors.hint,
            modifier = Modifier.padding(top = 4.dp),
        )

        // the loading call is the set's title, not a footnote (UX v6)
        exercise.plan?.let { plan ->
            val planColor = when (plan.kind) {
                Progression.Kind.INCREASE -> MaterialTheme.colorScheme.primary
                Progression.Kind.DELOAD -> GymTheme.colors.heat.worn
                Progression.Kind.HOLD -> MaterialTheme.colorScheme.secondary
            }
            Row(
                Modifier.padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    when (plan.kind) {
                        Progression.Kind.INCREASE -> Icons.Rounded.TrendingUp
                        Progression.Kind.DELOAD -> Icons.Rounded.TrendingDown
                        Progression.Kind.HOLD -> Icons.Rounded.TrendingFlat
                    },
                    contentDescription = null,
                    modifier = Modifier.size(15.dp),
                    tint = planColor,
                )
                Text(plan.line, style = MaterialTheme.typography.labelMedium, color = planColor)
            }
        }

        Spacer(Modifier.weight(1f))

        // WEIGHT — scrub horizontally; heavier loads scrub heavier (springMass as detent width)
        val massFactor = 1f + ((weight ?: 0.0) / 250.0).toFloat().coerceAtMost(0.6f)
        val weightDetentPx = with(density) { 34.dp.toPx() } * massFactor
        var weightAcc by remember(set.id) { mutableFloatStateOf(0f) }
        Text(
            text = weight?.let(PlateCalculator::fmt) ?: "—",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 96.sp, lineHeight = 100.sp,
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            modifier = Modifier.pointerInput(set.id) {
                detectHorizontalDragGestures { change, dx ->
                    change.consume()
                    weightAcc += dx
                    while (abs(weightAcc) >= weightDetentPx) {
                        val step = if (weightAcc > 0) 1 else -1
                        weightAcc -= step * weightDetentPx
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onScrubWeight(step)
                    }
                }
            },
        )
        Text(
            text = "KILOGRAMS · ‹ SCRUB ›",
            style = MaterialTheme.typography.labelSmall,
            color = GymTheme.colors.hint,
        )

        Spacer(Modifier.height(26.dp))

        // REPS — scrub vertically (up = more)
        val repsDetentPx = with(density) { 30.dp.toPx() }
        var repsAcc by remember(set.id) { mutableFloatStateOf(0f) }
        Text(
            text = "× ${reps ?: "—"}",
            style = MaterialTheme.typography.displayLarge.copy(
                fontSize = 56.sp, lineHeight = 60.sp,
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            modifier = Modifier.pointerInput(set.id) {
                detectVerticalDragGestures { change, dy ->
                    change.consume()
                    repsAcc += dy
                    while (abs(repsAcc) >= repsDetentPx) {
                        val step = if (repsAcc > 0) -1 else 1
                        repsAcc -= (if (repsAcc > 0) 1 else -1) * repsDetentPx
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onScrubReps(step)
                    }
                }
            },
        )
        Text(
            text = "REPS · SCRUB UP / DOWN",
            style = MaterialTheme.typography.labelSmall,
            color = GymTheme.colors.hint,
        )

        Spacer(Modifier.height(20.dp))

        // context: what you did last time, what it was worth, what to load
        val context = buildList {
            if (set.prevWeightKg != null && set.prevReps != null) {
                add("prev ${PlateCalculator.fmt(set.prevWeightKg)}×${set.prevReps}")
                if (set.prevReps > 0) {
                    val e1 = OneRM.estimate(set.prevWeightKg, set.prevReps)
                    add("1RM ${PlateCalculator.fmt((e1 * 2).roundToInt() / 2.0)}")
                }
            }
            weight?.takeIf { it > barKg }?.let {
                add("${PlateCalculator.fmt((it - barKg) / 2)}/side")
            }
        }
        if (context.isNotEmpty()) {
            Text(
                text = context.joinToString("  ·  "),
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFeatureSettings = FONT_FEATURE_TABULAR
                ),
                color = MaterialTheme.colorScheme.secondary,
            )
        }

        Spacer(Modifier.weight(1f))

        // the STRIKE surface — one thumb, one touch, set logged
        val pressSource = remember { MutableInteractionSource() }
        Surface(
            onClick = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onStrike()
            },
            interactionSource = pressSource,
            shape = RoundedCornerShape(34.dp),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .height(116.dp)
                .forgedPress(pressSource),
        ) {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = "LOG SET",
                    style = MaterialTheme.typography.headlineMedium.copy(letterSpacing = 6.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
                Text(
                    text = "rest starts automatically",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                )
            }
        }

        TextButton(onClick = onOpenTable, modifier = Modifier.padding(top = 6.dp)) {
            Icon(
                Icons.Rounded.TableRows,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = GymTheme.colors.hint,
            )
            Text(
                text = "  Full table — swipe up",
                style = MaterialTheme.typography.labelMedium,
                color = GymTheme.colors.hint,
            )
        }
    }
}
