// Purpose: Create/edit an exercise — name, muscle multi-select, equipment, description.
//          Shares the sheet vocabulary (SheetTitle / ForgedCta) with the other sheets.
// Inputs: optional initial values (null = create mode)
// Outputs: onSave(name, muscles display string, equipment, description); onDelete for existing
package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.data.ProgressionImporter
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.forgedPress

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditExerciseSheet(
    title: String,
    initialName: String = "",
    initialMuscles: String = "",
    initialEquipment: String = "",
    initialDescription: String = "",
    canDelete: Boolean = false,
    onSave: (name: String, muscles: String, equipment: String, description: String) -> Unit,
    onDelete: () -> Unit = {},
    onDismiss: () -> Unit,
) {
    // one source of truth for the group list — the importer's, so a saved exercise always
    // buckets into the same 10 groups the body map and Recovery read
    val canonicalMuscles = ProgressionImporter.CANONICAL_MUSCLES
    var name by remember { mutableStateOf(initialName) }
    var equipment by remember { mutableStateOf(initialEquipment) }
    var description by remember { mutableStateOf(initialDescription) }
    val selected = remember {
        initialMuscles.split("·").map { it.trim() }
            .filter { it in canonicalMuscles }
            .toMutableStateList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(bottom = 28.dp)
                .imePadding(),
        ) {
            SheetTitle(title)

            Column(
                Modifier.padding(horizontal = Dim.screenPadH),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
            }

            ForgedSectionHeader("MUSCLES", bottomPadding = 8.dp)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Spacer(Modifier.width(Dim.screenPadH - 8.dp))
                canonicalMuscles.forEach { muscle ->
                    MuscleChip(
                        label = muscle,
                        active = muscle in selected,
                        onToggle = {
                            if (muscle in selected) selected.remove(muscle) else selected.add(muscle)
                        },
                    )
                }
                Spacer(Modifier.width(Dim.screenPadH - 8.dp))
            }

            Column(
                Modifier.padding(start = Dim.screenPadH, end = Dim.screenPadH, top = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = equipment,
                    onValueChange = { equipment = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Equipment (optional)") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Description (optional)") },
                    minLines = 2,
                    shape = MaterialTheme.shapes.medium,
                )
            }

            Spacer(Modifier.height(18.dp))
            ForgedCta(
                label = "Save",
                onClick = { onSave(name, selected.joinToString(" · "), equipment, description) },
                enabled = name.isNotBlank(),
                modifier = Modifier.padding(horizontal = Dim.screenPadH),
            )
            if (canDelete) {
                Spacer(Modifier.height(6.dp))
                RowRule()
                ForgedListRow(
                    title = "Delete exercise",
                    subtitle = "Archived instead if it already has logged sets",
                    onClick = onDelete,
                    trailing = {
                        Text(
                            text = "Delete",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    },
                )
            }
        }
    }
}

/** Pill, ember when on — the one place a filled pill is right (multi-select, not navigation). */
@Composable
private fun MuscleChip(label: String, active: Boolean, onToggle: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            .padding(vertical = 2.dp)
            .clip(RoundedCornerShape(50))
            .background(
                if (active) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
            )
            .forgedPress(source, pressedScale = 0.96f)
            .clickable(interactionSource = source, indication = null, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = if (active) MaterialTheme.colorScheme.onPrimary
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
