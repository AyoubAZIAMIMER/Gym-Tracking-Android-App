// Purpose: Elegant loading state — a shimmer placeholder that sweeps a soft highlight
//          across muted bars, so a screen fills with structure before its data arrives
//          instead of flashing empty. Theme-aware; one infinite transition per bar.
// Inputs: size/shape via modifier
// Outputs: none (pure visualization)
package com.gymtracker.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.Motion

/** One shimmering placeholder bar. */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(7.dp),
) {
    val base = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
    val highlight = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    val transition = rememberInfiniteTransition(label = "shimmer")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_250, easing = Motion.Plane),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerSweep",
    )
    // a diagonal highlight travels left → right across the bar
    val shift = p * 900f - 300f
    val brush = Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(shift, 0f),
        end = Offset(shift + 300f, 120f),
    )
    Box(modifier.clip(shape).background(brush))
}

/** A single exercise-row placeholder: title bar + shorter subtitle bar. */
@Composable
fun ExerciseRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(
            Modifier.fillMaxWidth(0.7f),
            verticalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            ShimmerBox(Modifier.fillMaxWidth(0.75f).height(14.dp))
            ShimmerBox(Modifier.fillMaxWidth(0.4f).height(11.dp))
        }
    }
}
