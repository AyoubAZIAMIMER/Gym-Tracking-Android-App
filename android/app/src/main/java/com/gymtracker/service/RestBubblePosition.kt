// Purpose: One position for the rest bubble, shared by the in-app Compose bubble and the
//          WindowManager overlay and remembered across launches.
//          They used to track separate offsets, so dragging the bubble inside the app and then
//          leaving teleported it to the overlay's own default — the handoff read as two different
//          objects rather than one bubble following you out.
// Inputs: drags from either bubble (absolute screen pixels, top-left origin)
// Outputs: RestBubblePosition.offset — collected by both, persisted to its own prefs file
package com.gymtracker.service

import android.content.Context
import android.graphics.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object RestBubblePosition {

    /** Screen pixels from the top-left of the display — the one space both bubbles can agree on. */
    data class Pos(val x: Int, val y: Int)

    private const val PREFS = "rest_bubble"
    private const val KEY_X = "x"
    private const val KEY_Y = "y"

    private val _offset = MutableStateFlow<Pos?>(null)
    val offset: StateFlow<Pos?> = _offset.asStateFlow()

    /** Bubble edge length, so callers can clamp without importing the overlay. */
    const val SIZE_DP = 64

    fun ensureLoaded(context: Context) {
        if (_offset.value != null) return
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val screen = screenSize(context)
        val d = context.resources.displayMetrics.density
        val size = (SIZE_DP * d).toInt()
        // default: low on the left, roughly where the bubble used to sit above the session bar
        val fallbackX = (16 * d).toInt()
        val fallbackY = (screen.y - (300 * d)).toInt().coerceAtLeast(0)
        _offset.value = clamp(
            Pos(p.getInt(KEY_X, fallbackX), p.getInt(KEY_Y, fallbackY)),
            screen, size,
        )
    }

    fun set(context: Context, x: Int, y: Int, persist: Boolean) {
        val d = context.resources.displayMetrics.density
        val pos = clamp(Pos(x, y), screenSize(context), (SIZE_DP * d).toInt())
        _offset.value = pos
        if (persist) {
            context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_X, pos.x).putInt(KEY_Y, pos.y).apply()
        }
    }

    /** Keep it on screen — a stored position can outlive a display size change. */
    private fun clamp(pos: Pos, screen: Point, size: Int) = Pos(
        pos.x.coerceIn(0, (screen.x - size).coerceAtLeast(0)),
        pos.y.coerceIn(0, (screen.y - size).coerceAtLeast(0)),
    )

    private fun screenSize(context: Context): Point {
        val m = context.resources.displayMetrics
        return Point(m.widthPixels, m.heightPixels)
    }
}
