// Purpose: The three expressive axes from the prototype's tweak panel — Heat, Energy, Surface.
//          They exist so the app's *feel* is one setting each, not a pile of per-screen literals.
//          Heat re-tints action; Energy scales motion (Calm == reduce-motion); Surface picks the
//          card treatment. Everything else in the design stays fixed.
// Inputs: user prefs (persist in DataStore), system ANIMATOR_DURATION_SCALE
// Outputs: LocalForge — read it anywhere with ForgeExpression.current
package com.gymtracker.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

enum class Heat { Quenched, Ember, Molten }

enum class Energy { Calm, Alive, Roaring }

enum class SurfaceStyle { Flat, Soft, Glass }

@Immutable
data class ForgePalette(
    /** The action colour. Buttons, active rails, the wordmark accent. */
    val action: Color,
    /** Low-alpha container behind the action (active set rows, badges, chips). */
    val actionContainer: Color,
    /** Label colour that sits ON the action fill. */
    val onAction: Color,
    /** Outer glow used only on the primary CTA and the rest ring. */
    val glow: Color,
)

@Immutable
data class ForgeExpressionState(
    val heat: Heat = Heat.Ember,
    val energy: Energy = Energy.Alive,
    val surface: SurfaceStyle = SurfaceStyle.Soft,
    val dark: Boolean = true,
) {
    val palette: ForgePalette
        get() = when (heat) {
            // heat.steel — cool, restrained. Reads as "recovered".
            Heat.Quenched -> ForgePalette(
                action = Color(0xFF8FB4C7),
                actionContainer = if (dark) Color(0x298FB4C7) else Color(0x388FB4C7),
                onAction = Color(0xFF0C1519),
                glow = Color(0x808FB4C7),
            )
            // AccentPrimary — the shipped default. Do not change without changing Color.kt.
            Heat.Ember -> ForgePalette(
                action = Color(0xFFFF5A1F),
                actionContainer = if (dark) Color(0xFF3A1608) else Color(0x21FF5A1F),
                onAction = Color(0xFF1C0800),
                glow = Color(0x8CFF5A1F),
            )
            // heat.red — hottest. Loudest CTA; do not use for "resting".
            Heat.Molten -> ForgePalette(
                action = Color(0xFFFF3320),
                actionContainer = if (dark) Color(0x2EFF3320) else Color(0x21FF3320),
                onAction = Color(0xFF1C0400),
                glow = Color(0x99FF3320),
            )
        }

    /** Multiply every Motion.* duration by this. Calm == honor reduce-motion, kill ambient loops. */
    val motionScale: Float
        get() = when (energy) {
            Energy.Calm -> 0f       // no ambient loops, no pulses; transitions snap
            Energy.Alive -> 1f
            Energy.Roaring -> 1.25f // longer sweeps, brighter glow pulse
        }

    val ambientLoops: Boolean get() = energy != Energy.Calm

    /** Blur radius for the hero / nav treatment. 0 == no blur, paint an opaque surface. */
    val blurRadiusDp: Int
        get() = when (surface) {
            SurfaceStyle.Flat -> 0
            SurfaceStyle.Soft -> 14
            SurfaceStyle.Glass -> 22
        }

    /** Alpha of the card fill over the Ink ground. */
    val surfaceAlpha: Float
        get() = when (surface) {
            SurfaceStyle.Flat -> 1f
            SurfaceStyle.Soft -> 0.90f
            SurfaceStyle.Glass -> 0.62f
        }
}

val LocalForge = compositionLocalOf { ForgeExpressionState() }

object ForgeExpression {
    val current: ForgeExpressionState
        @Composable @ReadOnlyComposable get() = LocalForge.current
}
