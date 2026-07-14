// Purpose: Numeric input with vertical drag-to-adjust — gym-glove friendly, hint shows last session's value
// Inputs: current text, hint, keyboard type; drag steps and text changes go out as events
// Outputs: onValueChange(text) on typing, onDragStep(+1/-1) per ~22dp of drag, onFocusChanged
package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme

@Composable
fun DragNumberField(
    value: String,
    hint: String,
    onValueChange: (String) -> Unit,
    onDragStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Decimal,
    onFocusChanged: (Boolean) -> Unit = {},
) {
    var focused by remember { mutableStateOf(false) }
    var dragAccum by remember { mutableFloatStateOf(0f) }
    val focusManager = LocalFocusManager.current
    // Forged Motion §7.5: each increment is a machined detent — one haptic tick per step
    val haptics = LocalHapticFeedback.current
    val hintColor = GymTheme.colors.hint
    val fieldStyle = MaterialTheme.typography.titleMedium.copy(
        textAlign = TextAlign.Center,
        fontFeatureSettings = FONT_FEATURE_TABULAR,
    )

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .border(
                width = 1.dp,
                color = if (focused) MaterialTheme.colorScheme.primary else Color.Transparent,
                shape = MaterialTheme.shapes.small,
            )
            .pointerInput(Unit) {
                // drag up = increment, drag down = decrement; one step per ~22dp
                detectVerticalDragGestures(onDragStart = { dragAccum = 0f }) { change, dragAmount ->
                    change.consume()
                    dragAccum += dragAmount
                    val stepPx = 22.dp.toPx()
                    while (dragAccum <= -stepPx) {
                        onDragStep(1)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        dragAccum += stepPx
                    }
                    while (dragAccum >= stepPx) {
                        onDragStep(-1)
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        dragAccum -= stepPx
                    }
                }
            }
            .heightIn(min = 44.dp),
        contentAlignment = Alignment.Center,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp)
                .onFocusChanged { state ->
                    focused = state.isFocused
                    onFocusChanged(state.isFocused)
                },
            singleLine = true,
            textStyle = fieldStyle.copy(color = MaterialTheme.colorScheme.onSurface),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focusManager.moveFocus(FocusDirection.Next) }),
            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.Center) {
                    if (value.isEmpty()) {
                        Text(
                            text = hint,
                            modifier = Modifier.fillMaxWidth(),
                            style = fieldStyle,
                            color = hintColor,
                        )
                    }
                    innerTextField()
                }
            },
        )
        // subtle affordance for the drag gesture
        Column(Modifier.align(Alignment.CenterEnd).padding(end = 2.dp)) {
            Icon(
                Icons.Rounded.KeyboardArrowUp, contentDescription = null,
                modifier = Modifier.size(10.dp), tint = hintColor,
            )
            Icon(
                Icons.Rounded.KeyboardArrowDown, contentDescription = null,
                modifier = Modifier.size(10.dp), tint = hintColor,
            )
        }
    }
}
