// Purpose: Workout detail subpage — every set of one past session, PR stars, repeat action.
//          Flat sections + hairlines per the redesign; no cards (design/redesign-2026-07 README §4).
// Inputs: WorkoutDetailViewModel (route arg workoutId)
// Outputs: onBack, onOpenExercise(exerciseId), onRepeat(workoutId)
package com.gymtracker.ui.screens.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.AlertBody
import com.gymtracker.ui.components.ForgedAlert
import com.gymtracker.ui.components.ForgedBlock
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.PrBanner
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.emberBloomPulsing
import com.gymtracker.ui.components.forgeHero
import com.gymtracker.ui.components.rememberEntered
import com.gymtracker.ui.components.StampText
import com.gymtracker.ui.screens.session.SetTag
import com.gymtracker.ui.screens.session.setTagColor
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.ui.theme.rollUpValue
import com.gymtracker.utils.Formats
import com.gymtracker.utils.PlateCalculator
import kotlin.math.roundToInt

@Composable
fun WorkoutDetailScreen(
    onBack: () -> Unit = {},
    onOpenExercise: (String) -> Unit = {},
    onRepeat: (String) -> Unit = {},
    vm: WorkoutDetailViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val entered = rememberEntered()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    // The old finish sheet's reveal, now the top of this same screen: mark strikes in, then
    // the PR banner, once — LaunchedEffect(Unit) survives the state reloads that editing a set
    // or saving a comment trigger, so the confetti doesn't replay on every small edit.
    var celebrateVisible by remember { mutableStateOf(false) }
    val celebrateT by animateFloatAsState(
        targetValue = if (celebrateVisible) 1f else 0f,
        animationSpec = Motion.settle(Motion.STANDARD),
        label = "celebrateStrike",
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(state.justFinished) {
        if (state.justFinished) {
            celebrateVisible = true
            haptic.performHapticFeedback(
                if (state.prCount > 0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
            )
        }
    }

    GlowBackground(glowAlpha = 0.10f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle(state.title, trailing = state.dateLine, onBack = onBack)

            // ui-ux-pro-max pass, 2026-08-21: this screen is the app's one biggest emotional
            // payoff (finishing a session) and previously had no hero at all — the celebratory
            // mark floated over bare background, then a *separate* flat stat row forced 5 figures
            // into a horizontal scroll (an anti-pattern: content-priority, no horizontal-scroll).
            // Now: one forgeHero surface — matching every sibling tab's one-hero rule — holds the
            // reveal AND the stats together, wrapping onto a second line instead of scrolling.
            ForgedBlock(0, entered) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = Dim.screenPadH, vertical = 14.dp)
                        .forgeHero()
                        .padding(18.dp),
                ) {
                    if (state.justFinished) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            CompletionRing(entered = celebrateVisible, modifier = Modifier.padding(bottom = 4.dp))
                            Text(
                                text = "forged.",
                                style = MaterialTheme.typography.displayLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.graphicsLayer { alpha = celebrateT },
                            )
                            if (state.prCount > 0) {
                                Spacer(Modifier.height(10.dp))
                                PrBanner(
                                    visible = celebrateVisible,
                                    label = "${state.prCount} new PR${if (state.prCount > 1) "s" else ""}",
                                )
                            }
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SUMMARY",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.6.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f),
                        )
                        EditToggle(editing = state.editing, onClick = vm::toggleEditing)
                    }
                    val setsRolled = rollUpValue(state.totalSets.toFloat())
                    val volumeRolled = rollUpValue(state.totalVolumeNumeric.toFloat())
                    val caloriesRolled = rollUpValue(state.calories.toFloat())
                    val prRolled = rollUpValue(state.prCount.toFloat())
                    val deltaRolled = state.volumeDeltaPercent?.let { rollUpValue(it.toFloat()) }
                    FlowRow(
                        modifier = Modifier.padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Stat(state.durationText ?: "—", "duration")
                        Stat("${setsRolled.roundToInt()}", "sets")
                        Stat(Formats.volumeKg(volumeRolled.toDouble()), "kg volume")
                        if (state.calories > 0) Stat("~${caloriesRolled.roundToInt()}", "calories")
                        if (state.prCount > 0) Stat("${prRolled.roundToInt()}", "PRs", gold = true)
                        deltaRolled?.let { d ->
                            val positive = d >= 0
                            Stat(
                                value = "${if (positive) "▲" else "▼"}${kotlin.math.abs(d.roundToInt())}%",
                                label = "vs last",
                                gold = false,
                                valueColor = if (positive) GymTheme.colors.success else MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                }
            }

            if (state.prRows.isNotEmpty()) {
                ForgedBlock(0, entered) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Dim.screenPadH, vertical = 4.dp)
                            .forgeHero()
                            .padding(horizontal = 18.dp, vertical = 6.dp),
                    ) {
                        state.prRows.forEachIndexed { i, row ->
                            if (i > 0) RowRule()
                            PrLedgerRow(row)
                        }
                    }
                }
            }

            SectionRule()
            CommentRow(comment = state.comment, onClick = vm::openCommentDialog)

            SectionRule()
            state.exercises.forEachIndexed { i, ex ->
                ForgedBlock(i + 1, entered) {
                    ExerciseBlock(
                        ex = ex,
                        editing = state.editing,
                        onOpenExercise = onOpenExercise,
                        onUpdateSet = vm::updateSet,
                        onRemoveSet = vm::removeSet,
                        onRemoveExercise = { vm.removeExercise(ex.exerciseId) },
                    )
                }
            }

            if (state.musclesSplit.size > 1) {
                ForgedBlock(state.exercises.size + 1, entered) {
                    SectionRule()
                    MuscleSplitSection(state.musclesSplit, state.totalSets)
                }
            }

            ForgedBlock(state.exercises.size + 2, entered) {
                SectionRule()
                RepeatRow(onClick = { onRepeat(vm.workoutId) })
                if (state.editing) {
                    RowRule()
                    DeleteWorkoutRow(onClick = { confirmDelete = true })
                }
            }

            if (state.justFinished) {
                SectionRule()
                DoneButton(onClick = onBack, modifier = Modifier.padding(Dim.screenPadH))
            }

            Spacer(Modifier.navigationBarsPadding().height(Dim.listBottomSpacer))
        }
    }

    if (confirmDelete) {
        ForgedAlert(
            title = "Delete workout?",
            onDismissRequest = { confirmDelete = false },
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; vm.deleteWorkout() },
            dismissLabel = "Cancel",
            destructive = true,
            body = { AlertBody("\"${state.title}\" and everything logged in it will be removed. This can't be undone.") },
        )
    }

    if (state.commentDialogOpen) {
        var text by remember(state.comment) { mutableStateOf(state.comment) }
        ForgedAlert(
            title = "Session comment",
            onDismissRequest = vm::closeCommentDialog,
            confirmLabel = "Save",
            onConfirm = { vm.updateComment(text) },
            dismissLabel = "Cancel",
            body = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(280) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("How did the session go?") },
                    shape = MaterialTheme.shapes.medium,
                )
            },
        )
    }
}

/** Blank → a low-emphasis prompt ("Tap to comment your workout!" — the reference's own copy).
 *  Present → the note itself, still tappable to revise. Either way, one row, one behaviour. */
@Composable
private fun CommentRow(comment: String, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Column(
        Modifier
            .fillMaxWidth()
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Dim.screenPadH, vertical = 14.dp),
    ) {
        if (comment.isBlank()) {
            Text(
                text = "Tap to comment your workout",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            ForgedSectionHeader("NOTE", bottomPadding = 6.dp)
            Text(
                text = comment,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** Set-count share per primary muscle group — a segmented bar, not a pie: the flat/hairline
 *  language the rest of this screen already speaks, same information the reference's donut
 *  chart carries. */
@Composable
private fun MuscleSplitSection(split: List<MuscleShare>, totalSets: Int) {
    ForgedSectionHeader("SPLIT", trailing = { Text("$totalSets sets", fontSize = 13.sp, color = GymTheme.colors.hint) })
    val colors = listOf(
        MaterialTheme.colorScheme.primary,
        GymTheme.colors.tagPaused,
        GymTheme.colors.tagDropset,
        GymTheme.colors.tagForced,
        GymTheme.colors.tagPartial,
        GymTheme.colors.tagNegative,
    )
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dim.screenPadH)
            .height(10.dp)
            .clip(MaterialTheme.shapes.small),
    ) {
        split.forEachIndexed { i, share ->
            Box(
                Modifier
                    .weight(share.fraction.coerceAtLeast(0.01f))
                    .fillMaxHeight()
                    .background(colors[i % colors.size]),
            )
        }
    }
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dim.screenPadH, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        split.forEachIndexed { i, share ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(colors[i % colors.size]),
                )
                Text(
                    text = share.muscle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp).weight(1f),
                )
                Text(
                    text = "${share.sets}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EditToggle(editing: Boolean, onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            if (editing) Icons.Rounded.Close else Icons.Rounded.Edit,
            contentDescription = if (editing) "Done editing" else "Edit workout",
            tint = if (editing) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun DeleteWorkoutRow(onClick: () -> Unit) {
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
            Icons.Rounded.Delete,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.error,
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = "Delete workout",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

/** The one filled CTA on this screen, shown only fresh off a finish — same treatment the old
 *  finish sheet's "Save workout" pill had. Repeat/Delete above stay flat text rows on purpose:
 *  Done is the primary action here, everything else is secondary (ui-ux-pro-max `primary-action`). */
@Composable
private fun DoneButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val ember = MaterialTheme.colorScheme.primary
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier
            .fillMaxWidth()
            .emberBloomPulsing(ember, 24.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(ember)
            .forgedPress(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "Done",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
            color = MaterialTheme.colorScheme.onPrimary,
        )
    }
}

/** One exercise: stamped heading (tappable through to its stats), then its sets as flat rows.
 *  In edit mode the heading gains a remove action and each set becomes weight/reps fields. */
@Composable
private fun ExerciseBlock(
    ex: WorkoutRepository.DetailExercise,
    editing: Boolean,
    onOpenExercise: (String) -> Unit,
    onUpdateSet: (String, Double?, Int?) -> Unit,
    onRemoveSet: (String) -> Unit,
    onRemoveExercise: () -> Unit,
) {
    ForgedSectionHeader(
        label = ex.name.uppercase(),
        linkLabel = if (editing) null else "Stats",
        onClickLink = if (editing) null else ({ onOpenExercise(ex.exerciseId) }),
        bottomPadding = 4.dp,
        trailing = if (editing) {
            {
                IconButton(onClick = onRemoveExercise, modifier = Modifier.size(28.dp)) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = "Remove ${ex.name} from this workout",
                        modifier = Modifier.size(17.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                }
            }
        } else null,
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
        if (editing) {
            EditableSetRow(
                indexLabel = if (warmup) "W" else "$workingIdx",
                set = set,
                onUpdate = { w, r -> onUpdateSet(set.id, w, r) },
                onRemove = { onRemoveSet(set.id) },
            )
        } else {
            Column(Modifier.padding(horizontal = Dim.screenPadH, vertical = 10.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
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
                        },
                        fontSize = 15.sp,
                        fontWeight = if (warmup) FontWeight.Normal else FontWeight.SemiBold,
                        color = if (warmup) MaterialTheme.colorScheme.onSurfaceVariant
                        else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    SetTag.fromLetter(set.tag)?.takeIf { it != SetTag.WARMUP }?.let { tag ->
                        val tagColor = setTagColor(tag)
                        Text(
                            text = tag.label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = tagColor,
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(tagColor.copy(alpha = 0.14f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        )
                    }
                    if (set.isPr) {
                        Icon(
                            Icons.Rounded.Star,
                            contentDescription = "Personal record",
                            modifier = Modifier.size(16.dp),
                            tint = GymTheme.colors.prGold,
                        )
                    }
                }
                if (set.comment.isNotBlank()) {
                    Text(
                        text = set.comment,
                        fontSize = 12.sp,
                        color = GymTheme.colors.hint,
                        modifier = Modifier.padding(start = 44.dp, top = 2.dp),
                    )
                }
            }
        }
    }
}

/** Weight/reps as small text fields, committed on blur — not on every keystroke, so a
 *  half-typed number never round-trips through the database. */
@Composable
private fun EditableSetRow(
    indexLabel: String,
    set: WorkoutRepository.DetailSet,
    onUpdate: (Double?, Int?) -> Unit,
    onRemove: () -> Unit,
) {
    var weightText by remember(set.id) { mutableStateOf(set.weightKg?.let(PlateCalculator::fmt) ?: "") }
    var repsText by remember(set.id) { mutableStateOf(set.reps?.toString() ?: "") }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dim.screenPadH, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StampText(text = indexLabel, color = GymTheme.colors.hint, modifier = Modifier.width(28.dp))
        OutlinedTextField(
            value = weightText,
            onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
            modifier = Modifier
                .width(84.dp)
                .onFocusChanged { if (!it.isFocused) onUpdate(weightText.toDoubleOrNull(), repsText.toIntOrNull()) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            label = { Text("kg", fontSize = 11.sp) },
        )
        Text("×", color = GymTheme.colors.hint)
        OutlinedTextField(
            value = repsText,
            onValueChange = { repsText = it.filter { c -> c.isDigit() }.take(3) },
            modifier = Modifier
                .width(64.dp)
                .onFocusChanged { if (!it.isFocused) onUpdate(weightText.toDoubleOrNull(), repsText.toIntOrNull()) },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            label = { Text("reps", fontSize = 11.sp) },
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "Remove this set",
                modifier = Modifier.size(18.dp),
                tint = GymTheme.colors.hint,
            )
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
private fun Stat(value: String, label: String, gold: Boolean = false, valueColor: Color? = null) {
    Column {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 22.sp,
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip,
            color = valueColor ?: if (gold) GymTheme.colors.prGold else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

/**
 * The workout-complete reveal: a ring closes over `Motion.FORGE` (900 ms), then a checkmark
 * draws inside it once the ring is most of the way shut. Replaces the old scale-fade mark —
 * per design/MOTION.md §8 rung 3 ("strike → single settle → count → cool") and the redesign
 * board's explicit "no confetti, no particles" rule: the ring closing IS the celebration.
 */
@Composable
private fun CompletionRing(entered: Boolean, modifier: Modifier = Modifier, ringSize: Dp = 72.dp) {
    val progress by animateFloatAsState(
        targetValue = if (entered) 1f else 0f,
        animationSpec = Motion.settle(Motion.FORGE),
        label = "completionRing",
    )
    val ringColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.outlineVariant
    Box(modifier.size(ringSize), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = size.minDimension * 0.08f
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            drawArc(
                color = ringColor,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 0.999f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )
            // checkmark draws only once the ring is mostly closed — a spark-line arcing off
            // the strike, per MOTION.md's rung-3 grammar
            val checkT = ((progress - 0.6f) / 0.4f).coerceIn(0f, 1f)
            if (checkT > 0f) {
                val w = size.width
                val h = size.height
                val start = Offset(w * 0.28f, h * 0.52f)
                val mid = Offset(w * 0.44f, h * 0.68f)
                val end = Offset(w * 0.74f, h * 0.34f)
                val checkStroke = stroke * 0.85f
                if (checkT <= 0.5f) {
                    val t = checkT / 0.5f
                    drawLine(ringColor, start, lerp(start, mid, t), strokeWidth = checkStroke, cap = StrokeCap.Round)
                } else {
                    drawLine(ringColor, start, mid, strokeWidth = checkStroke, cap = StrokeCap.Round)
                    val t = (checkT - 0.5f) / 0.5f
                    drawLine(ringColor, mid, lerp(mid, end, t), strokeWidth = checkStroke, cap = StrokeCap.Round)
                }
            }
        }
    }
}

@Composable
private fun PrLedgerRow(row: PrRowUi) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(MaterialTheme.shapes.extraSmall)
                .background(GymTheme.colors.prGold.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Rounded.Star,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = GymTheme.colors.prGold,
            )
        }
        Text(
            text = row.exerciseName,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 11.dp).weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${row.oldValue} kg",
            fontSize = 12.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textDecoration = TextDecoration.LineThrough,
        )
        Text("→", fontSize = 11.sp, color = GymTheme.colors.hint, modifier = Modifier.padding(horizontal = 6.dp))
        Text(
            text = "${row.newValue} kg",
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
            color = GymTheme.colors.prGold,
        )
    }
}
