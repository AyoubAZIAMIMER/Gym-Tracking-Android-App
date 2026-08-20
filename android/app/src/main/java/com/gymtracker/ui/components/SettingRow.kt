// Purpose: Settings row vocabulary from the handoff (README §10) — title 15.5/600, optional
//          12.5sp helper line, control right-aligned. Segmented options are inline text with
//          an accent underline (NOT filled pills); switches are 44 × 26 dp.
// Inputs: title/helper + one control
// Outputs: SettingRow, SegmentedOptions, ForgedSwitch, SettingStepper
package com.gymtracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.Motion
import com.gymtracker.ui.theme.forgedPress

@Composable
fun SettingRow(
    title: String,
    modifier: Modifier = Modifier,
    helper: String? = null,
    onClick: (() -> Unit)? = null,
    control: (@Composable RowScope.() -> Unit)? = null,
) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .forgedPress(interaction, pressedScale = 0.99f)
                        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                } else Modifier
            )
            .padding(horizontal = Dim.screenPadH, vertical = 14.dp),
        // Top, not CenterVertically: a control centered against a title+2-line-helper column
        // floats below the title instead of reading level with it (Action colour's wrapped
        // caption was the case that showed it). Top holds for every row regardless of how
        // many lines the helper wraps to.
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (!helper.isNullOrBlank()) {
                Text(
                    text = helper,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
        control?.invoke(this)
    }
}

/** Inline options with an accent underline on the active one. */
@Composable
fun SegmentedOptions(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        options.forEach { option ->
            val active = option == selected
            val source = remember { MutableInteractionSource() }
            Column(
                Modifier
                    .forgedPress(source, pressedScale = 0.96f)
                    .clickable(interactionSource = source, indication = null) { onSelect(option) },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = option,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                    color = if (active) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(
                    Modifier
                        .padding(top = 4.dp)
                        .width(18.dp)
                        .height(2.dp)
                        .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                )
            }
        }
    }
}

/** 44 × 26 dp, accent when on, surfaceVariant when off. */
@Composable
fun ForgedSwitch(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val track by animateColorAsState(
        if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = Motion.plane(Motion.FAST),
        label = "switchTrack",
    )
    val knobX by animateDpAsState(
        if (checked) 20.dp else 2.dp,
        animationSpec = Motion.settle(Motion.FAST),
        label = "switchKnob",
    )
    Box(
        modifier
            .size(width = 44.dp, height = 26.dp)
            .clip(RoundedCornerShape(50))
            .background(track)
            .clickable { onChange(!checked) },
    ) {
        Box(
            Modifier
                .offset(x = knobX, y = 3.dp)
                .size(20.dp)
                .clip(CircleShape)
                .background(
                    if (checked) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
        )
    }
}

/** − value + , for default rest and anything else with a numeric step. */
@Composable
fun SettingStepper(
    value: String,
    onDelta: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        StepButton("–") { onDelta(-1) }
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        StepButton("+") { onDelta(1) }
    }
}

@Composable
private fun StepButton(label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    Box(
        Modifier
            // 30dp visual, padded out to a 48dp touch target
            .size(48.dp)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .forgedPress(source),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(label, fontSize = 17.sp, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
