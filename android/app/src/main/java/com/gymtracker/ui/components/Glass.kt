// Purpose: Liquid-glass design system — GlassSurface (frosted panel) + GlowBackground (ambient glows)
// Inputs: optional HazeState; GlowBackground publishes LocalBackgroundHaze so EVERY panel gets real blur
// Outputs: reusable glass containers used by every screen
package com.gymtracker.ui.components

import androidx.compose.animation.core.RepeatMode
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
import android.graphics.Bitmap
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.gymtracker.ui.theme.Energy
import com.gymtracker.ui.theme.LocalForge
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
        // specular top highlight — the "liquid" part of the glass, neutral white (v7)
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.White.copy(alpha = 0.06f),
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
 * The prototype's film grain: a single dot on a 3dp tile, repeated by the shader so the whole
 * screen costs one draw call. Null on the (impossible) zero-size case.
 */
@Composable
private fun rememberGrainBrush(): Brush? {
    val density = LocalDensity.current
    return remember(density) {
        val tile = with(density) { 3.dp.toPx() }.toInt().coerceAtLeast(2)
        val bitmap = Bitmap.createBitmap(tile, tile, Bitmap.Config.ARGB_8888)
        // rgba(255,240,225,.035) — a warm speck, barely there
        bitmap.setPixel(0, 0, android.graphics.Color.argb(9, 255, 240, 225))
        ShaderBrush(ImageShader(bitmap.asImageBitmap(), TileMode.Repeated, TileMode.Repeated))
    }
}

/**
 * Forge ambience behind every screen: the ember light hangs above the work (brighter
 * mid-session) with a quenched-steel spill top-left and a 3dp dot grain over everything,
 * exactly as the prototype paints it. Deliberately quiet — more iron than glow. It is a
 * `hazeSource`, so glass panels above it show genuinely blurred color — that is what makes
 * the glass read as liquid instead of grey translucency.
 */
@Composable
fun GlowBackground(
    modifier: Modifier = Modifier,
    emberHeat: Float = 1f,   // §11: the light lifts brighter during a session (~1.25)
    glowAlpha: Float = 0.12f, // per-screen, per the prototype: Home .12 … Settings .09, Done .22
    content: @Composable BoxScope.() -> Unit,
) {
    val background = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val current = MaterialTheme.colorScheme.primary     // ember — the one hot color
    val ice = MaterialTheme.colorScheme.secondary        // quenched steel — calm spill
    val backgroundHaze = remember { HazeState() }
    val grain = rememberGrainBrush()

    Box(modifier.fillMaxSize().background(background)) {
        // glow layer = blur source (must be its own layer, behind all content)
        Box(
            Modifier
                .matchParentSize()
                .hazeSource(backgroundHaze)
                .drawBehind {
                    // Prototype: `radial-gradient(120% 26% at 50% -8%, ember .12, transparent 60%)`
                    // — the forge light hangs ABOVE the work, it does not rise off the floor.
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(
                                current.copy(alpha = (glowAlpha * emberHeat).coerceIn(0f, 0.22f)),
                                Color.Transparent,
                            ),
                            center = Offset(size.width * 0.50f, -size.height * 0.08f),
                            radius = size.width * 0.95f,
                        )
                    )
                    // steel spill top-left — only Recovery/Body asks for it, but at 0.05 it
                    // reads as depth rather than a second signal anywhere else
                    drawRect(
                        Brush.radialGradient(
                            colors = listOf(ice.copy(alpha = 0.05f), Color.Transparent),
                            center = Offset(size.width * 0.10f, -size.height * 0.04f),
                            radius = size.width * 0.70f,
                        )
                    )
                    // 3dp dot grain — the prototype's `background-size:3px 3px` film texture.
                    // Tiled through a shader, so it costs one draw instead of ~40k dots.
                    grain?.let { drawRect(brush = it) }
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

/**
 * Ember bloom behind a hot surface — the prototype's `box-shadow: 0 0 26px rgba(255,90,31,.45)`
 * on the Start CTA. Compose has no colored box-shadow below API 28 (`shadow()`'s ambient/spot
 * tints are ignored there), so this stacks a few expanding rounded rects with a falling alpha:
 * identical on every API level, no blur pass, no extra layer.
 */
@Composable
fun Modifier.emberBloomPulsing(
    color: Color,
    cornerRadius: Dp,
    spread: Dp = 26.dp,
    intensity: Float = 0.45f,
): Modifier {
    // prototype `ringGlow 1.7s ease-in-out infinite`, applied to the accent CTA only when
    // Energy is Roaring. Calm/Alive get the static bloom.
    val forge = LocalForge.current
    if (forge.energy != Energy.Roaring) return emberBloom(color, cornerRadius, spread, intensity)
    val pulse = rememberInfiniteTransition(label = "ringGlow")
    val t by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1_700, easing = Motion.Temper),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "ringGlowT",
    )
    // .45 → .80 alpha and a wider spread at the peak, matching the keyframe's drop-shadow ramp
    return emberBloom(
        color = color,
        cornerRadius = cornerRadius,
        spread = spread * (1f + 0.45f * t),
        intensity = intensity + 0.35f * t,
    )
}

fun Modifier.emberBloom(
    color: Color,
    cornerRadius: Dp,
    spread: Dp = 26.dp,
    intensity: Float = 0.45f,
    layers: Int = 18,
): Modifier = drawBehind {
    val spreadPx = spread.toPx()
    val radiusPx = cornerRadius.toPx()
    // Each ring carries only its slice of the falloff — alpha compounds as they stack, so
    // per-layer alpha is divided by the layer count (few, fat layers read as visible bands).
    val perLayer = 3f / layers
    // outermost (faintest) first so the tighter, hotter rings paint over it
    for (i in layers downTo 1) {
        val t = i / layers.toFloat()            // 1f = outer edge of the bloom
        val grow = spreadPx * t
        val alpha = intensity * (1f - t) * (1f - t) * perLayer
        if (alpha <= 0.001f) continue
        drawRoundRect(
            color = color.copy(alpha = alpha),
            topLeft = Offset(-grow, -grow),
            size = Size(size.width + grow * 2f, size.height + grow * 2f),
            cornerRadius = CornerRadius(radiusPx + grow),
        )
    }
}
