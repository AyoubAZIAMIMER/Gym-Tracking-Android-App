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

/**
 * The action colour, NOT the data heat scale (that is [HeatScale] in Color.kt).
 *
 * Chalk is the default as of v10: with the chrome cooled to chalk-on-iron, warm colour belongs
 * to data alone — the muscle map, the recovery ramp, PR gold. Ember stayed selectable because
 * some people want the hot button; it just is not what the app is any more.
 */
enum class Heat { Chalk, Quenched, Ember, Molten }

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
    /** Text/icon colour that sits on [actionContainer] — eyebrows, the avatar, chips. */
    val onActionContainer: Color,
    /** Outer glow used only on the primary CTA and the rest ring. */
    val glow: Color,
    /**
     * Bloom strength for that glow. A chalk CTA needs roughly half of what ember does: the same
     * alpha on a near-white fill reads as a blown-out halo rather than heat coming off metal.
     */
    val glowIntensity: Float,
)

@Immutable
data class ForgeExpressionState(
    val heat: Heat = Heat.Chalk,
    val energy: Energy = Energy.Alive,
    val surface: SurfaceStyle = SurfaceStyle.Soft,
    val dark: Boolean = true,
) {
    val palette: ForgePalette
        get() = when (heat) {
            // Chalk & Iron (v10 default) — the CTA is bone-white on iron, so nothing in the
            // chrome competes with the heat scale. On paper it inverts to ink-on-bone.
            Heat.Chalk -> ForgePalette(
                action = if (dark) Chalk else InkAction,
                actionContainer = if (dark) ChalkContainer else Color(0x1A1B150E),
                onAction = if (dark) OnChalk else PaperLight,
                onActionContainer = if (dark) Chalk else InkAction,
                // a chalk halo, not a coloured glow: the CTA gains weight without gaining hue
                glow = if (dark) Color(0x40EDE6D8) else Color(0x331B150E),
                glowIntensity = 0.20f,
            )
            // heat.steel — cool, restrained. Reads as "recovered".
            Heat.Quenched -> ForgePalette(
                action = Color(0xFF8FB4C7),
                actionContainer = if (dark) Color(0x298FB4C7) else Color(0x388FB4C7),
                onAction = Color(0xFF0C1519),
                onActionContainer = Color(0xFFBFD6E0),
                glow = Color(0x808FB4C7),
                glowIntensity = 0.38f,
            )
            // AccentPrimary — the shipped default. Do not change without changing Color.kt.
            Heat.Ember -> ForgePalette(
                action = Color(0xFFFF5A1F),
                actionContainer = if (dark) Color(0xFF3A1608) else Color(0x21FF5A1F),
                onAction = Color(0xFF1C0800),
                onActionContainer = Color(0xFFFFB294),
                glow = Color(0x8CFF5A1F),
                glowIntensity = 0.45f,
            )
            // heat.red — hottest. Loudest CTA; do not use for "resting".
            Heat.Molten -> ForgePalette(
                action = Color(0xFFFF3320),
                actionContainer = if (dark) Color(0x2EFF3320) else Color(0x21FF3320),
                onAction = Color(0xFF1C0400),
                onActionContainer = Color(0xFFFFA898),
                glow = Color(0x99FF3320),
                glowIntensity = 0.5f,
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
