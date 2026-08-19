// Purpose: Profile sheet — first-run introduction and later edits (Data screen):
//          name, body weight, height, weekly training goal.
//          Weekly goal is a SettingStepper, not a text field — it is 1..7, never typed.
// Inputs: current Profile values; onSave(name, weightKg, heightCm, weeklyGoal)
// Outputs: user-confirmed profile via onSave; onDismiss to skip
package com.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
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
import com.gymtracker.ui.theme.Dim

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
    var weight by remember {
        mutableStateOf(
            initialWeightKg?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
                ?: ""
        )
    }
    var height by remember { mutableStateOf(initialHeightCm?.toString() ?: "") }
    var goal by remember { mutableStateOf(initialWeeklyGoal.coerceIn(1, 7)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(bottom = 28.dp)
                .imePadding(),
        ) {
            SheetTitle(
                "Who's training?",
                "Your name shapes the greeting; body weight and height feed future strength " +
                    "standards; the weekly goal drives the Home ring.",
            )

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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { t ->
                            weight = t.filter { it.isDigit() || it == '.' }.take(6)
                        },
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
            }

            RowRule(Modifier.padding(top = 14.dp))
            SettingRow(
                title = "Weekly goal",
                helper = "Sessions per week",
                control = {
                    SettingStepper(
                        value = "$goal",
                        onDelta = { delta -> goal = (goal + delta).coerceIn(1, 7) },
                    )
                },
            )
            RowRule(Modifier.padding(bottom = 18.dp))

            ForgedCta(
                label = "Save",
                onClick = { onSave(name.trim(), weight.toDoubleOrNull(), height.toIntOrNull(), goal) },
                enabled = name.isNotBlank(),
                modifier = Modifier.padding(horizontal = Dim.screenPadH),
            )
        }
    }
}
