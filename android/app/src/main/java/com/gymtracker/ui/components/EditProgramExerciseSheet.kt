// Purpose: Modal bottom sheet — edit one program exercise's target sets + rep range.
//          Same stepper the app already uses (SettingStepper), same sheet shell as
//          ExercisePickerSheet — no new input pattern for one small form.
// Inputs: current targetSets/repMin/repMax
// Outputs: onSave(sets, repMin, repMax) / onDismiss
package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.theme.Dim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProgramExerciseSheet(
    exerciseName: String,
    initialSets: Int,
    initialRepMin: Int,
    initialRepMax: Int,
    onSave: (sets: Int, repMin: Int, repMax: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var sets by remember { mutableStateOf(initialSets) }
    var repMin by remember { mutableStateOf(initialRepMin) }
    var repMax by remember { mutableStateOf(initialRepMax) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            Modifier
                .padding(horizontal = Dim.screenPadH, vertical = 8.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            SheetTitle(exerciseName, "Target sets and rep range for this day.")

            EditTargetRow("Sets", "$sets") { d -> sets = (sets + d).coerceIn(1, 10) }
            EditTargetRow("Rep min", "$repMin") { d -> repMin = (repMin + d).coerceIn(1, repMax) }
            EditTargetRow("Rep max", "$repMax") { d -> repMax = (repMax + d).coerceIn(repMin, 50) }

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onSave(sets, repMin, repMax) }
                    .padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun EditTargetRow(label: String, value: String, onDelta: (Int) -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.titleMedium)
        SettingStepper(value, onDelta)
    }
}
