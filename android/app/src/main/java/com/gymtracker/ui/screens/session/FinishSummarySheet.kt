// Purpose: End-of-workout summary sheet — animated check, key stats, session comment
// Inputs: session state + elapsed time
// Outputs: onSave(comment) → ViewModel.finishWorkout; onDismiss keeps training
package com.gymtracker.ui.screens.session

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion
import com.gymtracker.utils.Formats
import com.gymtracker.utils.PlateCalculator
import com.gymtracker.utils.TimeFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinishSummarySheet(
    state: WorkoutSessionUiState,
    elapsedMillis: Long,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var comment by remember { mutableStateOf("") }
    // Forge Moment (rung 3): the check strikes in and settles — steel doesn't wobble;
    // the stats arrive 60 ms apart, carrying the eye down the sheet
    var checkVisible by remember { mutableStateOf(false) }
    val checkT by animateFloatAsState(
        targetValue = if (checkVisible) 1f else 0f,
        animationSpec = Motion.settle(Motion.STANDARD),
        label = "checkStrike",
    )
    LaunchedEffect(Unit) { checkVisible = true }

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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                Modifier
                    .size(64.dp)
                    .graphicsLayer {
                        alpha = checkT
                        scaleX = 0.85f + 0.15f * checkT
                        scaleY = 0.85f + 0.15f * checkT
                    }
                    .clip(CircleShape)
                    .background(GymTheme.colors.successDim),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.Check, contentDescription = null,
                    modifier = Modifier.size(32.dp), tint = GymTheme.colors.success,
                )
            }
            Text("Session complete", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = state.workoutName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                StaggeredStat(0, checkVisible, "Duration", TimeFormat.clock(elapsedMillis), Modifier.weight(1f))
                StaggeredStat(1, checkVisible, "Sets", "${state.completedSets}/${state.totalSets}", Modifier.weight(1f))
                StaggeredStat(2, checkVisible, "Volume", "${Formats.volumeKg(state.totalVolumeKg)} kg", Modifier.weight(1f))
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("How did it go? (session comment)") },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
            )
            Button(
                onClick = { onSave(comment) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(50),
            ) {
                Text("Save workout", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun StaggeredStat(
    index: Int,
    visible: Boolean,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val t by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = Motion.settle(Motion.STANDARD, delayMillis = 80 + 60 * index),
        label = "statStagger",
    )
    SummaryStat(
        label = label,
        value = value,
        modifier = modifier.graphicsLayer {
            alpha = t
            translationY = (1f - t) * 8.dp.toPx()
        },
    )
}

@Composable
private fun SummaryStat(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            Modifier.padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFeatureSettings = FONT_FEATURE_TABULAR
                ),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
