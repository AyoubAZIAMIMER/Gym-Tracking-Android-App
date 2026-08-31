// Purpose: Modal bottom sheet for adding an exercise mid-workout.
//          Flat rows + hairlines, same grid as the screens behind it — no nested cards.
// Inputs: picker items (database exercises when available, starters otherwise), search query
// Outputs: onPick(item) / onDismiss
package com.gymtracker.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.screens.session.PickerItem
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.GymTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerSheet(
    items: List<PickerItem>,
    onPick: (PickerItem) -> Unit,
    onDismiss: () -> Unit,
    // the same sheet doubles as the Replace picker — only the copy changes
    title: String = "Add exercise",
    subtitle: String = "Pick from your library.",
) {
    var query by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(bottom = 24.dp)) {
            SheetTitle(title, subtitle)

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dim.screenPadH),
                placeholder = { Text("Search exercises", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = RoundedCornerShape(50),
            )

            val results = remember(items, query) {
                items.filter {
                    it.name.contains(query, ignoreCase = true) ||
                        it.muscleGroup.contains(query, ignoreCase = true)
                }
            }
            if (results.isEmpty()) {
                Text(
                    text = "No exercise matches \"$query\".",
                    fontSize = 13.sp,
                    color = GymTheme.colors.hint,
                    modifier = Modifier.padding(horizontal = Dim.screenPadH, vertical = 20.dp),
                )
            }
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 420.dp)
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.Top,
            ) {
                // index folded into the key: two starter items (no dbExerciseId) sharing a
                // name would otherwise collide and crash LazyColumn with "Key ... already used"
                itemsIndexed(results, key = { i, it -> "$i-${it.dbExerciseId ?: it.name}" }) { _, exercise ->
                    RowRule()
                    ForgedListRow(
                        title = exercise.name,
                        subtitle = exercise.muscleGroup,
                        onClick = { onPick(exercise) },
                        trailing = {
                            Icon(
                                Icons.Rounded.Add,
                                contentDescription = "Add ${exercise.name}",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        },
                    )
                }
            }
        }
    }
}
