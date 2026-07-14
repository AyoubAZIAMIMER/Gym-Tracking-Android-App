// Purpose: Corner radii — big rounded corners per the UI references
// Inputs: none (constants)
// Outputs: AppShapes used by Theme.kt
package com.gymtracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),    // set rows, input fields
    medium = RoundedCornerShape(16.dp),   // inner cards, panels
    large = RoundedCornerShape(24.dp),    // exercise cards
    extraLarge = RoundedCornerShape(28.dp),
)
