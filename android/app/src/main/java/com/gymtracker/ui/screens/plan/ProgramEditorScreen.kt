// Purpose: Edit one program — add/remove days and exercises, set active, delete
// Inputs: ProgramEditorViewModel (route arg programId)
// Outputs: onBack navigation
package com.gymtracker.ui.screens.plan

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.ExercisePickerSheet
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.utils.Formats

@Composable
fun ProgramEditorScreen(
    onBack: () -> Unit = {},
    vm: ProgramEditorViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    LaunchedEffect(state.deleted) { if (state.deleted) onBack() }

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
                    text = state.detail?.program?.name ?: "Program",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = vm::toggleActive) {
                    Text(
                        text = if (state.isActive) "ACTIVE ✓" else "Set active",
                        color = if (state.isActive) GymTheme.colors.success
                        else MaterialTheme.colorScheme.primary,
                    )
                }
            }

            state.detail?.days?.forEach { day ->
                GlassSurface {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                day.day.name,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = { vm.deleteDay(day.day.id) }) {
                                Icon(
                                    Icons.Rounded.Delete,
                                    contentDescription = "Delete ${day.day.name}",
                                    modifier = Modifier.size(18.dp),
                                    tint = GymTheme.colors.hint,
                                )
                            }
                        }
                        if (day.exercises.isEmpty()) {
                            Text(
                                text = "No exercises yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        day.exercises.forEach { pe ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    Modifier
                                        .size(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        pe.exercise?.name ?: "Unknown exercise",
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = "${pe.row.targetSets} × ${Formats.repRange(pe.row.repMin, pe.row.repMax)}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { vm.removeExercise(pe.row.id) }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove",
                                        modifier = Modifier.size(16.dp),
                                        tint = GymTheme.colors.hint,
                                    )
                                }
                            }
                        }
                        TextButton(onClick = { vm.openPicker(day.day.id) }) {
                            Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add exercise")
                        }
                    }
                }
            }

            TextButton(onClick = vm::addDay) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Text("Add day")
            }

            TextButton(onClick = vm::deleteProgram) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text("Delete program", color = MaterialTheme.colorScheme.error)
            }

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(24.dp)
            )
        }
    }

    if (state.pickerForDayId != null) {
        ExercisePickerSheet(
            items = state.pickerItems,
            onPick = vm::addExercise,
            onDismiss = vm::closePicker,
        )
    }
}
