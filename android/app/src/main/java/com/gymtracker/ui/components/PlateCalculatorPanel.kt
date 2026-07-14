// Purpose: Inline plate breakdown shown under a focused weight field
// Inputs: target weight (kg); bar/plates use PlateCalculator defaults until Settings exists
// Outputs: visual plate stack (IPF colors) + "Bar 20 + 2×20 + 2×2.5 per side" text
package com.gymtracker.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtracker.utils.PlateCalculator

@Composable
fun PlateCalculatorPanel(
    targetKg: Double?,
    modifier: Modifier = Modifier,
    barKg: Double = PlateCalculator.DEFAULT_BAR_KG,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = Color.White.copy(alpha = 0.05f), // glass wash — sits on a glass card
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    Icons.Rounded.FitnessCenter, contentDescription = null,
                    modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "PLATE CALCULATOR",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "bar ${PlateCalculator.fmt(barKg)} kg",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            val loadout = targetKg?.let { PlateCalculator.forTarget(it, barKg = barKg) }
            when {
                targetKg == null -> Text(
                    text = "Enter a weight to see the plates to load.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                loadout == null -> Text(
                    text = "Target is below the ${PlateCalculator.fmt(barKg)} kg bar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> {
                    if (loadout.platesPerSide.isNotEmpty()) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // bar stub, then one bar per plate on a side
                            Box(
                                Modifier
                                    .size(width = 18.dp, height = 8.dp)
                                    .clip(RoundedCornerShape(2.dp))
                                    .background(MaterialTheme.colorScheme.outline)
                            )
                            loadout.platesPerSide.forEach { plate ->
                                Box(
                                    Modifier
                                        .size(width = 12.dp, height = plateHeight(plate))
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(plateColor(plate))
                                )
                            }
                        }
                    }
                    Text(
                        text = PlateCalculator.describe(loadout),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (loadout.leftoverKg > 0.01) {
                        Text(
                            text = "${PlateCalculator.fmt(loadout.leftoverKg)} kg can't be loaded with standard plates",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

// IPF plate colors, slightly muted for the dark UI (logged UI decision)
private fun plateColor(plate: Double): Color = when {
    plate >= 25.0 -> Color(0xFFD64545)   // red
    plate >= 20.0 -> Color(0xFF3B6FD4)   // blue
    plate >= 15.0 -> Color(0xFFE3B341)   // yellow
    plate >= 10.0 -> Color(0xFF3F9D63)   // green
    plate >= 5.0 -> Color(0xFFC9CDD6)    // white
    plate >= 2.5 -> Color(0xFF6A6E76)    // black, lifted so it reads on dark bg
    else -> Color(0xFF9AA0AC)
}

private fun plateHeight(plate: Double): Dp = when {
    plate >= 25.0 -> 44.dp
    plate >= 20.0 -> 40.dp
    plate >= 15.0 -> 34.dp
    plate >= 10.0 -> 28.dp
    plate >= 5.0 -> 22.dp
    plate >= 2.5 -> 16.dp
    else -> 12.dp
}
