// Purpose: Flat list row — the deliberate anti-card. Directories and supporting lists
//          (Library rows, History log, Recovery muscles, Plan programs) use this so
//          each screen's hero surface stays the only glass card in sight (UI refresh 1b).
// Inputs: optional onClick (gets Law-1 press physics), divider toggle
// Outputs: a dense row with a hairline divider; no border, no blur, no chrome
package com.gymtracker.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.forgedPress

@Composable
fun FlatRow(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    divider: Boolean = true,
    verticalPadding: Dp = 11.dp,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .then(
                    if (onClick != null) {
                        Modifier
                            .clip(MaterialTheme.shapes.small)
                            .forgedPress(interaction, pressedScale = 0.99f)
                            .clickable(
                                interactionSource = interaction,
                                indication = LocalIndication.current,
                                onClick = onClick,
                            )
                    } else Modifier
                )
                .padding(horizontal = 4.dp, vertical = verticalPadding),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
        if (divider) {
            HorizontalDivider(
                thickness = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
            )
        }
    }
}
