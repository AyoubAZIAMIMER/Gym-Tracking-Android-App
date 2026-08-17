// Purpose: Session — "The Slate". The screen you stare at between sets. Fixed 3-part column:
//          header (never scrolls) · scrolling set list · logging bar (never scrolls).
//          No nested cards: hairlines separate sections so the eye drops straight down.
// Inputs: SessionUi from your existing SessionViewModel; all logic stays in the VM.
// Outputs: SessionSlateScreen()
//
// THE THREE PLACES EMBER IS ALLOWED, AND ONLY THESE
//   1. the active set row (accentContainer fill + 3 dp left bar)
//   2. the rest pill / rest ring
//   3. the Complete set CTA
// A superset (A1/A2) has no rest — its pill is OUTLINED, not filled. That one colour change is
// the whole superset affordance; do not add a second badge.
package com.gymtracker.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.automirrored.rounded.StickyNote2
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.withTimeoutOrNull
import com.gymtracker.ui.theme.Motion
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.EffortBars
import com.gymtracker.ui.components.ForgeHairline
import androidx.compose.ui.text.TextStyle
import com.gymtracker.ui.components.emberBloomPulsing
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.ui.theme.LocalForge

enum class SetStatus { Todo, Active, Done }

data class SetRowUi(
    val index: Int,
    val status: SetStatus,
    val weightKg: Double? = null,
    val reps: Int? = null,
    /** SessionSet.intensity — e1RM ÷ all-time best. Drives the heat-tinted OneRmBadge. */
    val intensity: Float? = null,
    val isPr: Boolean = false,
)

data class SessionUi(
    val elapsed: String,                 // "34:33"
    val exerciseProgress: Float,         // 0f..1f — the 2 dp top rail
    val exerciseIndexLabel: String,      // "2" in the 46 dp badge
    val exerciseName: String,            // "Squat (Barbell)"
    val muscleCaption: String,           // "Quads & glutes"
    val supersetTag: String? = null,     // "A1" / "A2" — present means NO rest after this set
    val setsLabel: String,               // "1 / 4"
    val sets: List<SetRowUi>,
    val restSeconds: Int? = null,        // null = not resting
    val draftWeight: Double,
    val draftReps: Int,
    val effort: Int? = null,             // 1..5, optional
)

@Composable
fun SessionSlateScreen(
    ui: SessionUi,
    heatAt: (Float) -> Color,
    onBack: () -> Unit,
    onCompleteSet: () -> Unit,
    onWeightDelta: (Int) -> Unit,
    /** Tap the weight to type it — no forced increments. */
    onEditWeight: () -> Unit,
    onEditReps: () -> Unit,
    onRepsDelta: (Int) -> Unit,
    onEffort: (Int) -> Unit,
    onEditNote: () -> Unit = {},
    onExerciseMenu: () -> Unit = {},
    onPrevExercise: (() -> Unit)? = null,
    onNextExercise: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val forge = LocalForge.current
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Column(modifier.fillMaxSize().background(scheme.background)) {

        // ---- header (fixed) -------------------------------------------------------------
        Box(Modifier.fillMaxWidth().height(2.dp).background(scheme.outlineVariant)) {
            Box(Modifier.fillMaxWidth(ui.exerciseProgress).height(2.dp).background(forge.palette.action))
        }
        Spacer(Modifier.height(Dim.statusBand))

        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dim.screenPadH),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.clickable(onClick = onBack),
            )
            Text(ui.elapsed, style = type.headlineMedium, color = scheme.onSurface)

            if (ui.restSeconds != null) {
                val superset = ui.supersetTag != null
                // Superset => outlined pill (no rest). Straight set => ember fill.
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .then(
                            if (superset) Modifier.border(1.dp, scheme.outline, RoundedCornerShape(50))
                            else Modifier.background(forge.palette.action)
                        )
                        .padding(horizontal = 11.dp, vertical = 5.dp)
                ) {
                    Text(
                        formatRest(ui.restSeconds),
                        style = type.titleLarge.copy(fontSize = 15.sp),
                        color = if (superset) scheme.onSurfaceVariant else forge.palette.onAction,
                    )
                }
            }
            Spacer(Modifier.weight(1f))
            // exercise paging — the multi-exercise table used to provide this by scrolling
            Icon(
                Icons.Rounded.ChevronLeft,
                contentDescription = "Previous exercise",
                tint = if (onPrevExercise != null) scheme.onSurfaceVariant else scheme.outline,
                modifier = Modifier.clickable(enabled = onPrevExercise != null) {
                    onPrevExercise?.invoke()
                },
            )
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = "Next exercise",
                tint = if (onNextExercise != null) scheme.onSurfaceVariant else scheme.outline,
                modifier = Modifier.clickable(enabled = onNextExercise != null) {
                    onNextExercise?.invoke()
                },
            )
        }

        // exercise header
        Row(
            Modifier.fillMaxWidth().padding(horizontal = Dim.screenPadH, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(Dim.exerciseBadge)
                    .clip(RoundedCornerShape(14.dp))
                    .background(forge.palette.actionContainer),
                contentAlignment = Alignment.Center,
            ) { Text(ui.exerciseIndexLabel, style = type.titleLarge, color = forge.palette.action) }

            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(ui.exerciseName, style = type.titleMedium.copy(fontSize = 19.sp), color = scheme.onSurface)
                    ui.supersetTag?.let {
                        Text(
                            it,
                            style = type.labelSmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier
                                .border(1.dp, scheme.outline, RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                }
                Text(ui.muscleCaption, style = type.bodyMedium, color = scheme.onSurfaceVariant)
            }
            IconButton(onClick = onExerciseMenu) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "Exercise options",
                    tint = scheme.onSurfaceVariant,
                )
            }
        }

        // ---- set list (scrolls) ----------------------------------------------------------
        LazyColumn(Modifier.weight(1f).padding(horizontal = Dim.screenPadH)) {
            itemsIndexed(ui.sets) { _, set ->
                SetRow(set, heatAt)
                ForgeHairline()
            }
        }

        // ---- logging bar (fixed, Dim.sessionBottomBar tall) --------------------------------
        Column(
            Modifier
                .fillMaxWidth()
                .background(scheme.surfaceContainer)
                .padding(horizontal = Dim.screenPadH, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // EFFORT — the shared 5-bar selector (StrikeMode uses the same one). The handoff's
            // inline version drew static Boxes and never invoked onEffort.
            EffortBars(
                rpe = ui.effort?.let { it + 5 },
                onSelect = { rpe -> onEffort(rpe?.minus(5) ?: 0) },
                modifier = Modifier.fillMaxWidth(),
            )

            ForgeHairline()

            // Steppers — visual 38 dp, touch target padded to 48 dp. Weight step is the user's
            // pref (default 2.5 kg); reps ±1 clamped 1..50. Long-press repeats.
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                Stepper(
                    label = "WEIGHT KG",
                    value = trimZeros(ui.draftWeight),
                    modifier = Modifier.weight(1f),
                    onTapValue = onEditWeight,
                ) { onWeightDelta(it) }
                Box(Modifier.width(1.dp).height(72.dp).background(scheme.outlineVariant))
                Stepper(
                    label = "REPS",
                    value = ui.draftReps.toString(),
                    modifier = Modifier.weight(1f),
                    onTapValue = onEditReps,
                ) { onRepsDelta(it) }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val noteSource = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .size(56.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .border(1.dp, scheme.outline, MaterialTheme.shapes.medium)
                        .forgedPress(noteSource)
                        .clickable(
                            interactionSource = noteSource,
                            indication = null,
                            onClick = onEditNote,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.AutoMirrored.Rounded.StickyNote2,
                        contentDescription = "Exercise note",
                        tint = scheme.onSurfaceVariant,
                    )
                }

                val ctaSource = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .weight(1f)
                        .height(Dim.ctaHeight)
                        .emberBloomPulsing(forge.palette.action, Dim.ctaRadius)
                        .clip(MaterialTheme.shapes.medium)
                        .background(forge.palette.action)
                        .forgedPress(ctaSource)
                        .clickable(
                            interactionSource = ctaSource,
                            indication = null,
                            onClick = onCompleteSet,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Complete set",
                        style = type.titleLarge,
                        color = forge.palette.onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun SetRow(set: SetRowUi, heatAt: (Float) -> Color) {
    val forge = LocalForge.current
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography
    val active = set.status == SetStatus.Active
    // prototype `rowSettle .28s`: the row that becomes active lands from 1.028 → 1. The overshoot
    // curve there is a bezier; the codebase's springMass is the sanctioned equivalent (MOTION §5).
    val settle by animateFloatAsState(
        targetValue = if (active) 1f else 1.028f,
        animationSpec = Motion.springMass(),
        label = "rowSettle",
    )

    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (active) Modifier.graphicsLayer { scaleX = settle; scaleY = settle } else Modifier
            )
            .then(if (active) Modifier.clip(MaterialTheme.shapes.small).background(forge.palette.actionContainer) else Modifier)
            .padding(vertical = Dim.rowPadV, horizontal = if (active) 10.dp else 0.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (active) Box(Modifier.width(3.dp).height(22.dp).background(forge.palette.action))

        Text(
            set.index.toString().padStart(2, '0'),
            style = type.titleLarge.copy(fontSize = 17.sp),
            color = when (set.status) {
                SetStatus.Active -> forge.palette.action
                SetStatus.Done -> scheme.onSurfaceVariant
                SetStatus.Todo -> scheme.onSurfaceVariant.copy(alpha = 0.55f)
            },
        )
        Text(
            "Set ${set.index}",
            style = type.bodyMedium.copy(fontSize = 15.5.sp),
            color = if (set.status == SetStatus.Todo) scheme.onSurfaceVariant else scheme.onSurface,
            modifier = Modifier.weight(1f),
        )

        when (set.status) {
            SetStatus.Active -> Text("NOW", style = type.labelSmall, color = forge.palette.action)
            SetStatus.Done -> Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // prototype `checkPop .3s`: the logged marker pops in past 1 and settles back
                var popped by remember(set.index) { mutableStateOf(false) }
                LaunchedEffect(set.index) { popped = true }
                val pop by animateFloatAsState(
                    targetValue = if (popped) 1f else 0.55f,
                    animationSpec = Motion.springMass(),
                    label = "checkPop",
                )
                // e1RM badge — heat-tinted by intensity. Warm-ups read steel, top sets glow.
                set.intensity?.let { i ->
                    Box(
                        Modifier
                            .graphicsLayer { scaleX = pop; scaleY = pop }
                            .clip(RoundedCornerShape(6.dp))
                            .background(heatAt(1f - i.coerceIn(0f, 1f)).copy(alpha = 0.18f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) { Text("e1RM", style = type.labelSmall, color = heatAt(1f - i.coerceIn(0f, 1f))) }
                }
                Text(
                    "${trimZeros(set.weightKg ?: 0.0)} kg × ${set.reps ?: 0}",
                    style = type.titleLarge.copy(fontSize = 17.sp),
                    color = scheme.onSurface,
                )
            }
            SetStatus.Todo -> Text("⋮", color = scheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun Stepper(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    /** Tap the number to type a value outright — the steppers are a convenience, not a limit. */
    onTapValue: (() -> Unit)? = null,
    onDelta: (Int) -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = scheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            StepperButton("−") { onDelta(-1) }
            val valueSource = remember { MutableInteractionSource() }
            Text(
                text = value,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 42.sp),
                color = scheme.onSurface,
                modifier = if (onTapValue == null) Modifier else Modifier
                    .forgedPress(valueSource, pressedScale = 0.96f)
                    .clickable(interactionSource = valueSource, indication = null, onClick = onTapValue)
                    .padding(vertical = 6.dp),
            )
            StepperButton("+") { onDelta(1) }
        }
    }
}

@Composable
private fun StepperButton(glyph: String, onClick: () -> Unit) {
    // 38 dp visual, 48 dp touch — pad, don't grow the circle.
    // The handoff shipped the click commented out (BUILD_ORDER "things that will bite you");
    // long-press repeats every 90 ms, which README §Interactions expects on Android.
    val source = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    Box(
        Modifier
            .size(48.dp)
            .forgedPress(source)
            .pointerInput(onClick) {
                awaitEachGesture {
                    awaitFirstDown()
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onClick()
                    // hold past 400 ms and it repeats every 90 ms until release
                    if (withTimeoutOrNull(400) { waitForUpOrCancellation() } == null) {
                        while (withTimeoutOrNull(90) { waitForUpOrCancellation() } == null) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onClick()
                        }
                    }
                }
            }
            .padding(5.dp)
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) { Text(glyph, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface) }
}

private fun formatRest(s: Int): String {
    val sign = if (s < 0) "+" else ""
    val a = kotlin.math.abs(s)
    return "$sign${a / 60}:${(a % 60).toString().padStart(2, '0')}"
}

private fun effortWord(e: Int?): String = when (e) {
    1 -> "Easy"; 2 -> "Moderate"; 3 -> "Hard"; 4 -> "Very hard"; 5 -> "All out"; else -> "Not set"
}

private fun trimZeros(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else v.toString().trimEnd('0').trimEnd('.')
