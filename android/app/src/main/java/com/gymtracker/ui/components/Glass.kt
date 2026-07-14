// Purpose: Liquid-glass design system — GlassSurface (frosted panel) + GlowBackground (ambient glows)
// Inputs: optional HazeState; GlowBackground publishes LocalBackgroundHaze so EVERY panel gets real blur
// Outputs: reusable glass containers used by every screen
package com.gymtracker.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.Motion
import com.gymtracker.ui.theme.forgedPress
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource

// Published by GlowBackground: lets any GlassSurface blur the ambient glow layer
// without prop drilling. Null outside a GlowBackground (e.g. the app-level nav bar).
val LocalBackgroundHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * Frosted glass panel with real backdrop blur (API 31+; translucent fallback below).
 * Blur target priority: explicit [hazeState] (e.g. the session list under the top bar),
 * else [LocalBackgroundHaze] (the ambient glow layer), else a plain translucent wash.
 */
@Composable
fun GlassSurface(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    hazeState: HazeState? = null,
    tint: Color = GymTheme.colors.glassTint,
    blurTint: Color = GymTheme.colors.glassTintBlur,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val effectiveHaze = hazeState ?: LocalBackgroundHaze.current
    val borderBrush = Brush.linearGradient(
        listOf(GymTheme.colors.glassHighlight, GymTheme.colors.glassOutline)
    )
    val background = MaterialTheme.colorScheme.background
    // Forged Motion, Law 1: clickable glass compresses on contact (cards give 0.985)
    val pressInteraction = remember { MutableInteractionSource() }
    Box(
        modifier
            .then(
                if (onClick != null) Modifier.forgedPress(pressInteraction, pressedScale = 0.985f)
                else Modifier
            )
            .clip(shape)
            .then(
                if (effectiveHaze != null) {
                    Modifier.hazeEffect(
                        state = effectiveHaze,
                        style = HazeStyle(
                            backgroundColor = background,
                            tints = listOf(HazeTint(blurTint)),
                            blurRadius = 28.dp,
                            noiseFactor = 0.02f,
                            fallbackTint = HazeTint(tint),
                        ),
                    )
                } else {
                    Modifier.background(tint)
                }
            )
            .border(1.dp, borderBrush, shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = pressInteraction,
                        indication = LocalIndication.current,
                        onClick = onClick,
                    )
                } else Modifier
            )
    ) {
        // specular top highlight — the "liquid" part of the glass, warmed like firelight
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color(0xFFFFE3C2).copy(alpha = 0.08f),
                        0.4f to Color.Transparent,
                    )
                )
        )
        // custom container ⇒ we must provide content color ourselves (material Surface
        // normally does this; without it, unstyled Text defaults to black)
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
            content()
        }
    }
}

/**
 * Forge-light ambience behind every screen: the main ember glow rises from the bottom
 * (standing over the coals), a faint molten-gold spark light sits top-right, and a cool
 * quenched-steel wash balances the top-left. The glow layer is a `hazeSource`, so glass
 * panels above it show genuinely blurred color — that is what makes the glass read as
 * liquid instead of grey translucency.
 */
@Composable
fun GlowBackground(
    modifier: Modifier = Modifier,
    emberHeat: Float = 1f,   // §11: the forge burns hotter during a session (~1.25)
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val ember = MaterialTheme.colorScheme.primary
    val steel = MaterialTheme.colorScheme.secondary
    val gold = MaterialTheme.colorScheme.tertiary
    val backgroundHaze = remember { HazeState() }

    Box(modifier.fillMaxSize().background(background)) {
        // glow layer = blur source (must be its own layer, behind all content)
        Box(
            Modifier
                .matchParentSize()
                .hazeSource(backgroundHaze)
                .drawBehind {
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                ember.copy(alpha = (0.30f * emberHeat).coerceIn(0f, 0.42f)),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.50f, size.height * 1.06f),
                            radius = size.width * 1.05f,
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(gold.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.92f, size.height * 0.04f),
                            radius = size.width * 0.65f,
                        )
                    )
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(steel.copy(alpha = 0.10f), Color.Transparent),
                            center = Offset(size.width * 0.04f, size.height * 0.10f),
                            radius = size.width * 0.75f,
                        )
                    )
                }
        )
        CompositionLocalProvider(
            LocalContentColor provides onBackground,
            LocalBackgroundHaze provides backgroundHaze,
        ) {
            content()
        }
    }
}

/**
 * Indeterminate loading, forge-style (Forged Motion §10 — no spinners, anywhere):
 * cold steel with a faint warm sheen sweeping through every 1.8 s on the temper curve.
 */
@Composable
fun SteelSheen(modifier: Modifier = Modifier) {
    val sweep = rememberInfiniteTransition(label = "steelSheen")
    val x by sweep.animateFloat(
        initialValue = -0.4f,
        targetValue = 1.4f,
        animationSpec = infiniteRepeatable(tween(1_800, easing = Motion.Temper)),
        label = "sheenX",
    )
    val warm = GymTheme.colors.glassHighlight
    Box(
        modifier
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .drawBehind {
                drawRect(
                    Brush.horizontalGradient(
                        0f to Color.Transparent,
                        0.5f to warm.copy(alpha = 0.55f),
                        1f to Color.Transparent,
                        startX = (x - 0.3f) * size.width,
                        endX = (x + 0.3f) * size.width,
                    )
                )
            }
    )
}
