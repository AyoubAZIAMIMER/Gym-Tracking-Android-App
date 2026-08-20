// Purpose: Edit one program — add/remove days and exercises, set active, delete.
//          Flat sections + hairlines per the redesign; day names are stamped headings.
// Inputs: ProgramEditorViewModel (route arg programId)
// Outputs: onBack navigation
package com.gymtracker.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.AlertBody
import com.gymtracker.ui.components.EditProgramExerciseSheet
import com.gymtracker.ui.components.ExerciseOverflowMenu
import com.gymtracker.ui.components.ExercisePickerSheet
import com.gymtracker.ui.components.ForgedAlert
import com.gymtracker.ui.components.ForgedBlock
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.rememberEntered
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.utils.Formats

@Composable
fun ProgramEditorScreen(
    onBack: () -> Unit = {},
    vm: ProgramEditorViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    var confirmDelete by remember { mutableStateOf(false) }
    val entered = rememberEntered()
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

    GlowBackground(glowAlpha = 0.10f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle(state.detail?.program?.name ?: "Program", onBack = onBack)

            ForgedBlock(0, entered) {
            SectionRule()
            // Active is a state, not a button — it reads as a row you can flip
            ActionRow(
                label = if (state.isActive) "Active program" else "Set as active program",
                tint = if (state.isActive) GymTheme.colors.success
                else MaterialTheme.colorScheme.primary,
                trailing = if (state.isActive) "✓" else null,
                onClick = vm::toggleActive,
            )
            }

            state.detail?.days?.forEachIndexed { dayIndex, day ->
                ForgedBlock(dayIndex + 1, entered) {
                SectionRule()
                ForgedSectionHeader(
                    label = day.day.name.uppercase(),
                    bottomPadding = 4.dp,
                    trailing = {
                        IconButton(
                            onClick = { vm.deleteDay(day.day.id) },
                            modifier = Modifier.size(28.dp),
                        ) {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = "Delete ${day.day.name}",
                                modifier = Modifier.size(17.dp),
                                tint = GymTheme.colors.hint,
                            )
                        }
                    },
                )
                if (day.exercises.isEmpty()) {
                    Text(
                        text = "No exercises yet.",
                        fontSize = 13.sp,
                        color = GymTheme.colors.hint,
                        modifier = Modifier.padding(
                            start = Dim.screenPadH, end = Dim.screenPadH, bottom = 6.dp,
                        ),
                    )
                }
                day.exercises.forEach { pe ->
                    RowRule()
                    ProgramExerciseRow(
                        pe = pe,
                        onToggleSuperset = { vm.toggleSuperset(pe.row.id) },
                        onEdit = { vm.openEditTarget(pe.row.id) },
                        onReplace = { vm.openReplace(pe.row.id) },
                        onRemove = { vm.removeExercise(pe.row.id) },
                    )
                }
                RowRule()
                ActionRow(
                    label = "Add exercise",
                    tint = MaterialTheme.colorScheme.primary,
                    leadingIcon = true,
                    onClick = { vm.openPicker(day.day.id) },
                )
                }
            }

            SectionRule()
            ActionRow(
                label = "Add day",
                tint = MaterialTheme.colorScheme.primary,
                leadingIcon = true,
                onClick = vm::addDay,
            )
            RowRule()
            ActionRow(
                label = "Delete program",
                tint = MaterialTheme.colorScheme.error,
                onClick = { confirmDelete = true },
            )

            Spacer(Modifier.navigationBarsPadding().height(Dim.listBottomSpacer))
        }
    }

    if (state.pickerForDayId != null) {
        ExercisePickerSheet(
            items = state.pickerItems,
            onPick = vm::addExercise,
            onDismiss = vm::closePicker,
        )
    }

    if (state.replacingExerciseId != null) {
        ExercisePickerSheet(
            items = state.pickerItems,
            onPick = vm::confirmReplace,
            onDismiss = vm::closeReplace,
            title = "Replace exercise",
            subtitle = "Pick a replacement — sets and rep range carry over.",
        )
    }

    state.editingExerciseId?.let { editingId ->
        val pe = state.detail?.days
            ?.flatMap { it.exercises }
            ?.firstOrNull { it.row.id == editingId }
        if (pe != null) {
            EditProgramExerciseSheet(
                exerciseName = pe.exercise?.name ?: "Exercise",
                initialSets = pe.row.targetSets,
                initialRepMin = pe.row.repMin,
                initialRepMax = pe.row.repMax,
                onSave = vm::saveEditTarget,
                onDismiss = vm::closeEditTarget,
            )
        }
    }

    if (confirmDelete) {
        // Deleting a program throws away its days and exercise rows — confirm first
        ForgedAlert(
            title = "Delete program?",
            onDismissRequest = { confirmDelete = false },
            confirmLabel = "Delete",
            onConfirm = { confirmDelete = false; vm.deleteProgram() },
            dismissLabel = "Cancel",
            destructive = true,
            body = {
                AlertBody(
                    "\"${state.detail?.program?.name ?: "This program"}\" and all its days will be " +
                        "removed. Logged workouts are kept."
                )
            },
        )
    }
}

/** One program exercise: dot/superset-bracket, name + target, ⋮ → the shared
 *  Superset/Edit/Replace/Remove menu — same vocabulary the live session uses. */
@Composable
private fun ProgramExerciseRow(
    pe: WorkoutRepository.ProgramExerciseDetail,
    onToggleSuperset: () -> Unit,
    onEdit: () -> Unit,
    onReplace: () -> Unit,
    onRemove: () -> Unit,
) {
    val inSuperset = pe.row.supersetGroup != null
    val supersetColor = MaterialTheme.colorScheme.primary
    Row(
        Modifier
            .fillMaxWidth()
            .then(
                if (inSuperset) Modifier.drawBehind {
                    drawRoundRect(
                        color = supersetColor,
                        topLeft = Offset(0f, 8.dp.toPx()),
                        size = Size(3.dp.toPx(), size.height - 16.dp.toPx()),
                        cornerRadius = CornerRadius(2.dp.toPx()),
                    )
                } else Modifier
            )
            .padding(start = Dim.screenPadH, end = 8.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).padding(vertical = 9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = pe.exercise?.name ?: "Unknown exercise",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (inSuperset) {
                    Text(
                        text = "  SUPERSET",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = supersetColor,
                    )
                }
            }
            Text(
                text = "${pe.row.targetSets} × " + Formats.repRange(pe.row.repMin, pe.row.repMax),
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Box {
            var menuOpen by remember { mutableStateOf(false) }
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Rounded.MoreVert,
                    contentDescription = "More options for ${pe.exercise?.name ?: "exercise"}",
                    tint = GymTheme.colors.hint,
                )
            }
            ExerciseOverflowMenu(
                expanded = menuOpen,
                onDismiss = { menuOpen = false },
                inSuperset = inSuperset,
                onToggleSuperset = onToggleSuperset,
                editLabel = "Edit target",
                onEdit = onEdit,
                onReplace = onReplace,
                onRemove = onRemove,
            )
        }
    }
}

/** Flat full-width action row — the redesign's replacement for a stray TextButton. */
@Composable
private fun ActionRow(
    label: String,
    tint: androidx.compose.ui.graphics.Color,
    onClick: () -> Unit,
    leadingIcon: Boolean = false,
    trailing: String? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .forgedPress(interaction, pressedScale = 0.99f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Dim.screenPadH, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (leadingIcon) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = tint,
            )
        }
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = tint,
            modifier = Modifier.weight(1f),
        )
        if (trailing != null) Text(trailing, fontSize = 15.sp, color = tint)
    }
}
