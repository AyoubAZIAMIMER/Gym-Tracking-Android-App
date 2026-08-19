// Purpose: The redesign's surface vocabulary as modifiers, so no screen hand-rolls a card.
//          Three levels only: ambient ground -> flat section (hairlines) -> ONE hero per screen.
//          Reserve forgeHero for that single hero; everything else is ForgedListRow + hairlines.
// Inputs: ForgeExpression (Surface axis controls blur/alpha), colorScheme
// Outputs: Modifier.forgeGround(), Modifier.forgeHero(), ForgeHairline(), ForgeSectionHeader()
package com.gymtracker.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.LocalForge
import com.gymtracker.ui.theme.forgedPress

/**
 * Ambient ground: Ink + ONE radial ember glow at 50% / -8%, ~12% alpha. One per screen, never two.
 * The prototype's 3 dp dot-grid noise is optional polish — add it as a tiled drawable if you want it.
 */
@Composable
fun Modifier.forgeGround(): Modifier {
    val forge = LocalForge.current
    val bg = MaterialTheme.colorScheme.background
    val glow = forge.palette.action.copy(alpha = 0.12f)
    return this
        .background(bg)
        .drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(glow, Color.Transparent),
                    center = Offset(size.width * 0.5f, -size.height * 0.08f),
                    radius = size.width * 1.1f,
                ),
                size = Size(size.width, size.height),
            )
        }
}

/**
 * THE hero surface — one per screen (Home's "Recommended for today", History's calendar,
 * Plan's Up Next, Recovery's body map, Stats' chart). Everything else stays flat.
 * Surface axis: Flat = opaque, Soft = 90%, Glass = 62% + blur (Modifier.blur on the content behind,
 * or a RenderEffect on API 31+; below 31 fall back to the Soft alpha and skip the blur).
 */
@Composable
fun Modifier.forgeHero(shape: Shape = RoundedCornerShape(Dim.heroRadius)): Modifier {
    val forge = LocalForge.current
    val scheme = MaterialTheme.colorScheme
    return this
        .clip(shape)
        .background(
            Brush.linearGradient(
                listOf(
                    forge.palette.actionContainer.copy(alpha = 0.55f * forge.surfaceAlpha),
                    scheme.surfaceContainer.copy(alpha = forge.surfaceAlpha),
                )
            )
        )
        .border(Dim.hairline, scheme.outline.copy(alpha = 0.55f), shape)
}

/** The 1 dp rule that replaces card stacking. Matches RowRule's divider exactly. */
@Composable
fun ForgeHairline(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Dim.hairline)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    )
}

/** Mono micro-label section header, with optional right-hand link. labelSmall, 11 sp, +1.2. */
@Composable
fun ForgeSectionHeader(
    label: String,
    modifier: Modifier = Modifier,
    linkLabel: String? = null,
    onLink: (() -> Unit)? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = Dim.sectionTop, bottom = Dim.sectionBottom),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.2.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (linkLabel != null && onLink != null) {
            Text(
                text = "$linkLabel ›",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.clickableRow(onLink),
            )
        }
    }
}

/** The handoff shipped this as a stub; wired to the repo's real press physics (0.99 for rows). */
@Composable
private fun Modifier.clickableRow(onClick: () -> Unit): Modifier {
    val interaction = remember { MutableInteractionSource() }
    return this
        .padding(start = 8.dp)
        .forgedPress(interaction, pressedScale = 0.99f)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
}
