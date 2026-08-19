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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.ConfettiBurst
import com.gymtracker.ui.components.ForgedMark
import com.gymtracker.ui.components.emberBloom
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.components.PrBanner
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion
import com.gymtracker.ui.theme.rollUpValue
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
    val prCount = remember(state) {
        state.exercises.sumOf { ex -> ex.sets.count { it.completed && it.isPr } }
    }
    // Forge Moment (rung 3): the check strikes in and settles — steel doesn't wobble;
    // the stats arrive 60 ms apart, carrying the eye down the sheet
    var checkVisible by remember { mutableStateOf(false) }
    val checkT by animateFloatAsState(
        targetValue = if (checkVisible) 1f else 0f,
        animationSpec = Motion.settle(Motion.STANDARD),
        label = "checkStrike",
    )
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(Unit) {
        checkVisible = true
        // reward buzz: firmer when the session set personal records
        haptic.performHapticFeedback(
            if (prCount > 0) HapticFeedbackType.LongPress else HapticFeedbackType.TextHandleMove
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Box(Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // the prototype's Done moment: the mark, then the workout's name "forged."
            ForgedMark(
                size = 40.dp,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.graphicsLayer {
                    alpha = checkT
                    scaleX = 0.85f + 0.15f * checkT
                    scaleY = 0.85f + 0.15f * checkT
                },
            )
            Text(
                text = "${state.workoutName}\nforged.",
                style = MaterialTheme.typography.displayLarge,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            PrBanner(
                visible = checkVisible && prCount > 0,
                label = "$prCount new PR${if (prCount > 1) "s" else ""}",
            )
            // §7.4: the headline number rolls up from zero on entry
            val volRolled = rollUpValue(state.totalVolumeKg.toFloat()).toDouble()
            Row(horizontalArrangement = Arrangement.spacedBy(26.dp)) {
                StaggeredStat(0, checkVisible, "duration", TimeFormat.clock(elapsedMillis))
                StaggeredStat(1, checkVisible, "sets", "${state.completedSets}/${state.totalSets}")
                StaggeredStat(
                    2, checkVisible, "new PRs", "$prCount",
                    color = if (prCount > 0) GymTheme.colors.prGold else null,
                )
                StaggeredStat(3, checkVisible, "kg", Formats.volumeKg(volRolled))
            }
            OutlinedTextField(
                value = comment,
                onValueChange = { comment = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("How did it go? (session comment)") },
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
            )
            val ember = MaterialTheme.colorScheme.primary
            Box(
                Modifier
                    .emberBloom(ember, 24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(ember)
                    .clickable { onSave(comment) }
                    .padding(horizontal = 40.dp, vertical = 15.dp),
            ) {
                Text(
                    text = "Save workout",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
            ConfettiBurst(run = checkVisible, modifier = Modifier.matchParentSize())
        }
    }
}

@Composable
private fun StaggeredStat(
    index: Int,
    visible: Boolean,
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color? = null,
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
        color = color ?: MaterialTheme.colorScheme.onSurface,
        modifier = modifier.graphicsLayer {
            alpha = t
            translationY = (1f - t) * 8.dp.toPx()
        },
    )
}

@Composable
private fun SummaryStat(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    // no plate behind the number — the prototype sets these bare, Anton over a caption
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            color = color,
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
