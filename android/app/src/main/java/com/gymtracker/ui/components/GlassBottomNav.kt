// Purpose: Floating liquid-glass bottom navigation (Home / Library / Recovery)
// Inputs: current route
// Outputs: onSelect(route) navigation events
package com.gymtracker.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.Motion

@Composable
fun GlassBottomNav(
    current: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(32.dp)) {
        Row(
            // 6 tabs: tighter than the 5-tab layout so "Recovery" still fits a 412 dp phone
            Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            NavItem("home", "Home", Icons.Rounded.Home, current, onSelect)
            NavItem("history", "History", Icons.Rounded.History, current, onSelect)
            NavItem("plan", "Plan", Icons.Rounded.CalendarMonth, current, onSelect)
            NavItem("library", "Library", Icons.Rounded.FitnessCenter, current, onSelect)
            NavItem("recovery", "Recovery", Icons.Rounded.MonitorHeart, current, onSelect)
            NavItem("stats", "Stats", Icons.Rounded.BarChart, current, onSelect)
        }
    }
}

@Composable
private fun NavItem(
    route: String,
    label: String,
    icon: ImageVector,
    current: String?,
    onSelect: (String) -> Unit,
) {
    val active = current == route
    // Forged Motion: selection heats in on the plane curve — no snap, no bounce
    val tint by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = Motion.plane(Motion.FAST),
        label = "navTint",
    )
    val fill by animateColorAsState(
        targetValue = if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
        else Color.Transparent,
        animationSpec = Motion.plane(Motion.FAST),
        label = "navFill",
    )
    Column(
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(fill)
            .clickable { onSelect(route) }
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp), tint = tint)
        Text(label, style = MaterialTheme.typography.labelMedium, color = tint)
    }
}
