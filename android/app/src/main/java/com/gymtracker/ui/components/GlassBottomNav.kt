// Purpose: Floating liquid-glass bottom navigation — 4 tabs, prototype metrics
//          (bar radius 32, item radius 20, primary @18% active fill, 20dp ForgedIcons
//          stroke marks, 10.5sp labels that go bold when active).
//          Owner's call 2026-08-08: six tabs cut to four, matching the 4-tab nav the
//          handoff's own Recovery/Plan renders show and Material's max-5 guidance.
//          History and Library are pushed destinations now — reached from Home's
//          "All history" rail and Plan's "Exercise library" row — so they carry back arrows.
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.Motion

@Composable
fun GlassBottomNav(
    current: String?,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(modifier = modifier, shape = RoundedCornerShape(Dim.navBarRadius)) {
        Row(
            // 4 tabs breathe on a 412 dp phone — wider items, more gap
            Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            NavItem("home", "Home", ForgedIcons.Home, current, onSelect)
            NavItem("plan", "Plan", ForgedIcons.Plan, current, onSelect)
            NavItem("recovery", "Body", ForgedIcons.Body, current, onSelect)
            NavItem("stats", "Stats", ForgedIcons.Stats, current, onSelect)
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
            .clip(RoundedCornerShape(Dim.navItemRadius))
            .background(fill)
            .clickable { onSelect(route) }
            .padding(horizontal = 7.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(icon, contentDescription = label, modifier = Modifier.size(Dim.navIcon), tint = tint)
        Text(
            text = label,
            fontSize = 10.5.sp,
            letterSpacing = (-0.1).sp,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
            color = tint,
        )
    }
}
