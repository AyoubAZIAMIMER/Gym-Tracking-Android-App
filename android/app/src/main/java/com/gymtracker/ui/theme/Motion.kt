// Purpose: Forged Motion tokens — THE single home of durations, easings, and springs
//          (design/MOTION.md is canonical; screens never hardcode timing or curves)
// Inputs: none (constants) + press interaction state for the forgedPress modifier
// Outputs: Motion.* specs consumed across the UI; Modifier.forgedPress / forgedEntrance
package com.gymtracker.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.min

object Motion {

    // --- timing scale (§3): durations have mass; exits run at ~0.7× ------------
    const val INSTANT = 0
    const val STRIKE = 80        // press compression, checks, detents
    const val FAST = 140         // icon states, chips, small exits
    const val STANDARD = 220     // component default
    const val DELIBERATE = 320   // screen navigation, sheets
    const val SLOW = 480         // progress fills, success cools
    const val FORGE = 900        // reserved: PR flash, session-forged sequence

    const val STAGGER = 30       // per-item list entrance delay
    const val STAGGER_CAP = 8    // only the first items stagger
    const val HANDOFF = 80       // heat transfer delay between elements

    // --- easing (§4): four curves, no raw beziers anywhere else ----------------
    /** The signature: launches hard, lands with exponential calm (Law of Cooling). */
    val Settle: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)
    /** Exits: accelerates away, like heat leaving metal. */
    val Cool: Easing = CubicBezierEasing(0.55f, 0f, 1f, 0.45f)
    /** On-plane moves: tab slides, reorder, the nav pill. */
    val Plane: Easing = CubicBezierEasing(0.65f, 0f, 0.35f, 1f)
    /** Ambient loops only (sine in-out) — never for user-triggered motion. */
    val Temper: Easing = CubicBezierEasing(0.37f, 0f, 0.63f, 1f)
    /** Press compression only — near-linear so contact reads instantly. */
    val StrikeIn: Easing = LinearEasing

    fun <T> settle(durationMillis: Int = STANDARD, delayMillis: Int = 0): FiniteAnimationSpec<T> =
        tween(durationMillis, delayMillis, Settle)

    fun <T> cool(durationMillis: Int = FAST): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Cool)

    fun <T> plane(durationMillis: Int = 180): FiniteAnimationSpec<T> =
        tween(durationMillis, easing = Plane)

    // --- springs (§5): gesture releases & interruptible settles only -----------
    // Law 2: steel doesn't wobble — damping below 0.85 is forbidden in this codebase.
    fun <T> springFirm(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 550f)
    fun <T> springMass(): FiniteAnimationSpec<T> =
        spring(dampingRatio = 0.9f, stiffness = 380f, visibilityThreshold = null)
    fun <T> springHeavy(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
}

/**
 * Law 1 — contact before motion. Compression begins the frame the finger lands
 * (pressed → 80 ms near-linear), release settles home on the signature curve.
 * Buttons compress to 0.97; large cards to 0.985 (bigger mass, smaller give).
 */
@Composable
fun Modifier.forgedPress(
    interactionSource: InteractionSource,
    pressedScale: Float = 0.97f,
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = if (pressed) {
            tween(Motion.STRIKE, easing = Motion.StrikeIn)
        } else {
            Motion.settle(Motion.STANDARD)
        },
        label = "forgedPress",
    )
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * §10 — lists arrive once, then stay put: fade + 8 px rise, 30 ms stagger, first
 * [Motion.STAGGER_CAP] items only. Pass `played=false` exactly once per screen entry
 * (rememberSaveable at the call site keeps tab-returns from replaying it).
 */
@Composable
fun Modifier.forgedEntrance(index: Int, played: Boolean): Modifier {
    val t by animateFloatAsState(
        targetValue = if (played) 1f else 0f,
        animationSpec = Motion.settle(
            Motion.STANDARD,
            delayMillis = Motion.STAGGER * min(index, Motion.STAGGER_CAP),
        ),
        label = "forgedEntrance",
    )
    return graphicsLayer {
        alpha = t
        translationY = (1f - t) * 8.dpToPx(density)
    }
}

private fun Float.dpToPx(density: Float): Float = this * density
private fun Int.dpToPx(density: Float): Float = this * density
