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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
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
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.rememberEntered
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
    val entered = rememberEntered()
    var confirmDelete by remember { mutableStateOf(false) }
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    GlowBackground(glowAlpha = 0.10f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle(state.title, trailing = state.dateLine, onBack = onBack)

            // Anton figures lead, the way History and Stats do — no glass tile row
            ForgedBlock(0, entered) {
                SectionRule()
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            start = Dim.screenPadH,
                            end = Dim.screenPadH,
                            top = 18.dp,
                            bottom = 20.dp,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(26.dp),
                ) {
                    Stat(state.durationText ?: "—", "duration")
                    Stat("${state.totalSets}", "sets")
                    Stat(state.totalVolume, "kg volume")
                    if (state.prCount > 0) Stat("${state.prCount}", "PRs", gold = true)
                    Spacer(Modifier.weight(1f))
                    EditToggle(editing = state.editing, onClick = vm::toggleEditing)
                }
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

            ForgedBlock(state.exercises.size + 1, entered) {
                SectionRule()
                RepeatRow(onClick = { onRepeat(vm.workoutId) })
                if (state.editing) {
                    RowRule()
                    DeleteWorkoutRow(onClick = { confirmDelete = true })
                }
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
