// Purpose: Create/edit an exercise — name, muscle multi-select, equipment, description
// Inputs: optional initial values (null = create mode)
// Outputs: onSave(name, muscles display string, equipment, description); onDelete for existing
package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

private val CanonicalMuscles = listOf(
    "Chest", "Back", "Shoulders", "Biceps", "Triceps",
    "Abs", "Glutes", "Quads", "Hamstrings", "Calves",
)

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
    var name by remember { mutableStateOf(initialName) }
    var equipment by remember { mutableStateOf(initialEquipment) }
    var description by remember { mutableStateOf(initialDescription) }
    val selected = remember {
        initialMuscles.split("·").map { it.trim() }.filter { it in CanonicalMuscles }.toMutableStateList()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
            )
            Text(
                text = "Muscles",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CanonicalMuscles.forEach { muscle ->
                    val active = muscle in selected
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(50))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                            )
                            .clickable {
                                if (active) selected.remove(muscle) else selected.add(muscle)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    ) {
                        Text(
                            text = muscle,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (active) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            OutlinedTextField(
                value = equipment,
                onValueChange = { equipment = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Equipment (optional)") },
                singleLine = true,
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Description (optional)") },
                minLines = 2,
            )
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        onSave(name, selected.joinToString(" · "), equipment, description)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(50),
                enabled = name.isNotBlank(),
            ) { Text("Save") }
            if (canDelete) {
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "Delete (archives if it has history)",
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
