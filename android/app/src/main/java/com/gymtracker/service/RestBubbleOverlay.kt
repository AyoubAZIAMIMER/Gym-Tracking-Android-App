// Purpose: The rest-timer bubble as a floating overlay, so it survives leaving the app.
//          RestTimerBubble (Compose) only exists inside the session screen; switch apps or go
//          Home and it disappears, leaving only the notification. This draws the same idea in a
//          WindowManager overlay: a chalk ring that empties as the rest runs down, over whatever
//          you are looking at. Draggable; tap returns to the session.
// Inputs: show(seconds remaining, total) / hide(), driven by RestTimerService
// Outputs: a TYPE_APPLICATION_OVERLAY window (only when the user has granted the permission)
package com.gymtracker.service

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.Typeface
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import kotlin.math.abs
import kotlin.math.roundToInt

class RestBubbleOverlay(private val context: Context) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private var view: BubbleView? = null
    private var params: WindowManager.LayoutParams? = null

    fun show(remainingSec: Int, totalSec: Int) {
        if (!canDraw(context)) return
        val v = view ?: BubbleView(context).also { fresh ->
            val size = (SIZE_DP * context.resources.displayMetrics.density).roundToInt()
            val lp = WindowManager.LayoutParams(
                size,
                size,
                if (Build.VERSION.SDK_INT >= 26) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                },
                // not focusable: the bubble must never steal typing from the app underneath
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT,
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (16 * context.resources.displayMetrics.density).roundToInt()
                y = (160 * context.resources.displayMetrics.density).roundToInt()
            }
            params = lp
            fresh.setOnTouchListener(DragTap(lp))
            runCatching { wm.addView(fresh, lp) }
            view = fresh
        }
        v.update(remainingSec, totalSec)
    }

    fun hide() {
        view?.let { runCatching { wm.removeView(it) } }
        view = null
        params = null
    }

    /** Drag to reposition; a tap that never really moved reopens the session. */
    private inner class DragTap(private val lp: WindowManager.LayoutParams) : View.OnTouchListener {
        private var downX = 0f
        private var downY = 0f
        private var startX = 0
        private var startY = 0
        private var moved = false

        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View, e: MotionEvent): Boolean {
            when (e.action) {
                MotionEvent.ACTION_DOWN -> {
                    downX = e.rawX; downY = e.rawY
                    startX = lp.x; startY = lp.y
                    moved = false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - downX
                    val dy = e.rawY - downY
                    if (abs(dx) > TOUCH_SLOP || abs(dy) > TOUCH_SLOP) moved = true
                    lp.x = startX + dx.roundToInt()
                    lp.y = startY + dy.roundToInt()
                    runCatching { wm.updateViewLayout(v, lp) }
                }
                MotionEvent.ACTION_UP -> if (!moved) {
                    context.packageManager
                        .getLaunchIntentForPackage(context.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        ?.let { runCatching { context.startActivity(it) } }
                }
            }
            return true
        }
    }

    /** Chalk ring on iron, emptying clockwise; goes ember in the last ten seconds. */
    private class BubbleView(context: Context) : View(context) {
        private var remaining = 0
        private var total = 1

        private val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = IRON }
        private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TRACK; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CHALK; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = CHALK
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        fun update(remainingSec: Int, totalSec: Int) {
            remaining = remainingSec.coerceAtLeast(0)
            total = totalSec.coerceAtLeast(1)
            invalidate()
        }

        override fun onDraw(canvas: Canvas) {
            val w = width.toFloat()
            val h = height.toFloat()
            val stroke = w * 0.085f
            track.strokeWidth = stroke
            ring.strokeWidth = stroke
            label.textSize = w * 0.27f

            val r = (minOf(w, h) - stroke) / 2f
            val cx = w / 2f
            val cy = h / 2f
            canvas.drawCircle(cx, cy, r, plate)

            val box = RectF(cx - r, cy - r, cx + r, cy + r)
            canvas.drawArc(box, -90f, 360f, false, track)
            // last ten seconds run hot — heat is data, and this is the clock
            ring.color = if (remaining <= 10) EMBER else CHALK
            label.color = ring.color
            canvas.drawArc(box, -90f, 360f * (remaining / total.toFloat()).coerceIn(0f, 1f), false, ring)

            val text = "%d:%02d".format(remaining / 60, remaining % 60)
            val baseline = cy - (label.descent() + label.ascent()) / 2f
            canvas.drawText(text, cx, baseline, label)
        }
    }

    companion object {
        private const val SIZE_DP = 76
        private const val TOUCH_SLOP = 12f
        private val IRON = Color.parseColor("#F21A1918")
        private val TRACK = Color.parseColor("#332C2A27")
        private val CHALK = Color.parseColor("#FFEDE6D8")
        private val EMBER = Color.parseColor("#FFFF5A1F")

        /** The overlay permission is a settings toggle, not a runtime dialog. */
        fun canDraw(context: Context): Boolean =
            Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)
    }
}
