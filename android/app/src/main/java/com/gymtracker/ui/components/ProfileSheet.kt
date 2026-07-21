// Purpose: Profile sheet — first-run introduction and later edits (Data screen):
//          name, body weight, height, weekly training goal
// Inputs: current Profile values; onSave(name, weightKg, heightCm, weeklyGoal)
// Outputs: user-confirmed profile via onSave; onDismiss to skip
package com.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    initialName: String = "",
    initialWeightKg: Double? = null,
    initialHeightCm: Int? = null,
    initialWeeklyGoal: Int = 3,
    onSave: (name: String, weightKg: Double?, heightCm: Int?, weeklyGoal: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }
    var weight by remember { mutableStateOf(initialWeightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "") }
    var height by remember { mutableStateOf(initialHeightCm?.toString() ?: "") }
    var goal by remember { mutableStateOf(initialWeeklyGoal.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .imePadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Who's training?", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = "Your name shapes the greeting; body weight and height feed " +
                    "future strength standards; the weekly goal drives the Home ring.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name") },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weight,
                    onValueChange = { t -> weight = t.filter { it.isDigit() || it == '.' }.take(6) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Body weight (kg)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = MaterialTheme.shapes.medium,
                )
                OutlinedTextField(
                    value = height,
                    onValueChange = { t -> height = t.filter(Char::isDigit).take(3) },
                    modifier = Modifier.weight(1f),
                    label = { Text("Height (cm)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = MaterialTheme.shapes.medium,
                )
            }
            OutlinedTextField(
                value = goal,
                onValueChange = { t -> goal = t.filter(Char::isDigit).take(1) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Weekly goal (workouts per week)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = MaterialTheme.shapes.medium,
            )
            Button(
                onClick = {
                    onSave(
                        name.trim(),
                        weight.toDoubleOrNull(),
                        height.toIntOrNull(),
                        goal.toIntOrNull()?.coerceIn(1, 7) ?: 3,
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text("Save", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
