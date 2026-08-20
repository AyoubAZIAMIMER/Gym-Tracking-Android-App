// Purpose: The one exercise-management menu — Superset / Edit / Replace / Remove — shared by
//          the Program editor and the live session so managing exercises feels like the same
//          tool in both places instead of two menus that drifted apart.
// Inputs: current superset state + callbacks; onReplace null hides that item entirely
// Outputs: ExerciseOverflowMenu()
package com.gymtracker.ui.components

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.RemoveCircleOutline
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ExerciseOverflowMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    inSuperset: Boolean,
    onToggleSuperset: () -> Unit,
    editLabel: String,
    // false for the last exercise in the list with nothing to pair with — greys the item out
    // instead of leaving it clickable-but-silently-inert
    supersetEnabled: Boolean = true,
    onEdit: () -> Unit,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier,
    // null hides the item — the live session disables Replace once a set is completed
    onReplace: (() -> Unit)? = null,
    // the session's "Add warm-up ramp" — a divider, then whatever's passed here
    extraItems: (@Composable ColumnScope.() -> Unit)? = null,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss, modifier = modifier) {
        DropdownMenuItem(
            text = { Text(if (inSuperset) "Remove from superset" else "Superset with next") },
            leadingIcon = {
                Icon(
                    if (inSuperset) Icons.Rounded.LinkOff else Icons.Rounded.Link,
                    contentDescription = null,
                    tint = if (inSuperset) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            enabled = inSuperset || supersetEnabled,
            onClick = { onDismiss(); onToggleSuperset() },
        )
        DropdownMenuItem(
            text = { Text(editLabel) },
            leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
            onClick = { onDismiss(); onEdit() },
        )
        if (onReplace != null) {
            DropdownMenuItem(
                text = { Text("Replace") },
                leadingIcon = { Icon(Icons.Rounded.SwapHoriz, contentDescription = null) },
                onClick = { onDismiss(); onReplace() },
            )
        }
        DropdownMenuItem(
            text = { Text("Remove", color = MaterialTheme.colorScheme.error) },
            leadingIcon = {
                Icon(
                    Icons.Rounded.RemoveCircleOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            onClick = { onDismiss(); onRemove() },
        )
        extraItems?.invoke(this)
    }
}
