// Purpose: Per-set comment sheet — free text plus the 8 predefined tags (Warmup..Dropset)
// Inputs: the active set's current comment/tag
// Outputs: onDone(comment, tag) once, when the sheet closes
package com.gymtracker.ui.screens.session

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.SheetTitle
import com.gymtracker.ui.theme.Dim

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetCommentSheet(
    exerciseName: String,
    initialComment: String,
    initialTag: SetTag?,
    onDismiss: () -> Unit,
    onDone: (comment: String, tag: SetTag?) -> Unit,
) {
    var comment by remember(initialComment) { mutableStateOf(initialComment) }
    var tag by remember(initialTag) { mutableStateOf(initialTag) }

    ModalBottomSheet(
        onDismissRequest = { onDone(comment, tag); onDismiss() },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            SheetTitle(title = "Set comment", subtitle = exerciseName)
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it.take(140) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dim.screenPadH),
                minLines = 2,
                placeholder = { Text("How did it feel? What changed?") },
                shape = MaterialTheme.shapes.medium,
            )
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dim.screenPadH, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SetTag.entries.forEach { t ->
                    TagChip(
                        tag = t,
                        selected = tag == t,
                        onClick = { tag = if (tag == t) null else t },
                    )
                }
            }
            TextButton(
                onClick = { onDone(comment, tag); onDismiss() },
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(end = Dim.screenPadH, bottom = 20.dp),
            ) { Text("Done", fontSize = 16.sp) }
        }
    }
}

@Composable
private fun TagChip(tag: SetTag, selected: Boolean, onClick: () -> Unit) {
    val color = setTagColor(tag)
    val scheme = MaterialTheme.colorScheme
    Box(
        Modifier
            .clip(MaterialTheme.shapes.large)
            .background(if (selected) color.copy(alpha = 0.16f) else scheme.surface)
            .border(
                1.dp,
                if (selected) color else scheme.outline,
                MaterialTheme.shapes.large,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color),
            )
            Text(
                text = tag.label,
                fontSize = 14.sp,
                color = if (selected) scheme.onSurface else scheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}
