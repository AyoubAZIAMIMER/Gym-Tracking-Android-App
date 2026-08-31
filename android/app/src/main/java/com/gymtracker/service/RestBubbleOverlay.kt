// Purpose: The rest-timer bubble — the only one. The session screen shows the same countdown
//          inline once you're looking at it, so this WindowManager overlay only needs to exist
//          for when you're not: switch apps or go Home and it appears; come back and it's gone.
//          52 dp ring, same heat ramp as the in-session readouts, and it keeps counting — into
//          a slow red pulse — if you go over the time you set. Draggable; tap returns to the
//          session.
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
import android.os.SystemClock
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import com.gymtracker.MainActivity
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import com.gymtracker.utils.TimeFormat

class RestBubbleOverlay(private val context: Context) {

    private val wm = context.getSystemService(WindowManager::class.java)
    private var view: BubbleView? = null
    private var params: WindowManager.LayoutParams? = null
    private var hiding = false

    fun show(remainingSec: Int, totalSec: Int) {
        if (!canDraw(context)) return
        RestBubblePosition.ensureLoaded(context)
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
                // lay out against the whole display, not just the area below the status bar —
                // otherwise the same stored position lands ~136 px lower here than in-app
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT,
            ).apply {
                // wherever the in-app bubble was last left, so leaving the app does not move it
                gravity = Gravity.TOP or Gravity.START
                RestBubblePosition.offset.value?.let { x = it.x; y = it.y }
            }
            params = lp
            fresh.setOnTouchListener(DragTap(lp))
            runCatching { wm.addView(fresh, lp) }
            view = fresh
            // fading and scaling the bubble in is what sells it as the same object that was
            // just riding along inside the app, not a new window popping up over your shoulder
            if (animationsEnabled(context)) {
                fresh.alpha = 0f
                fresh.scaleX = ENTER_SCALE
                fresh.scaleY = ENTER_SCALE
                fresh.animate()
                    .alpha(1f).scaleX(1f).scaleY(1f)
                    .setDuration(ENTER_MS)
                    .setInterpolator(DecelerateInterpolator(1.6f))
                    .start()
            }
        }
        if (hiding) {
            hiding = false
            v.animate().cancel()
            v.alpha = 1f
            v.scaleX = 1f
            v.scaleY = 1f
        }
        // a drag inside the app while we were hidden must land before the window is shown again
        params?.let { lp ->
            RestBubblePosition.offset.value?.let { pos ->
                if (lp.x != pos.x || lp.y != pos.y) {
                    lp.x = pos.x; lp.y = pos.y
                    runCatching { wm.updateViewLayout(v, lp) }
                }
            }
        }
        v.update(remainingSec, totalSec)
    }

    // mirrors the enter fade so the bubble reads as leaving the screen rather than the app
    // vanishing out from under it; `hiding` lets a fast re-entry reclaim this same window
    // instead of racing a second addView while the exit animation is still playing.
    fun hide() {
        val v = view ?: return
        if (!animationsEnabled(context)) {
            runCatching { wm.removeView(v) }
            view = null
            params = null
            hiding = false
            return
        }
        hiding = true
        v.animate()
            .alpha(0f).scaleX(EXIT_SCALE).scaleY(EXIT_SCALE)
            .setDuration(EXIT_MS)
            .setInterpolator(AccelerateInterpolator(1.4f))
            .withEndAction {
                if (!hiding) return@withEndAction   // show() reclaimed it mid-exit
                runCatching { wm.removeView(v) }
                view = null
                params = null
                hiding = false
            }
            .start()
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
                    RestBubblePosition.set(context, lp.x, lp.y, persist = false)
                }
                MotionEvent.ACTION_UP -> if (moved) {
                    RestBubblePosition.set(context, lp.x, lp.y, persist = true)
                } else {
                    // Same EXTRA_FIRE_UP deep link the widget and both notifications use — a
                    // bare launcher intent just reopens whatever nav route is on screen, which
                    // is Home (not the session) if the user got here via in-app Minimise.
                    val intent = Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        .putExtra(MainActivity.EXTRA_FIRE_UP, true)
                    runCatching { context.startActivity(intent) }
                }
            }
            return true
        }
    }

    /**
     * 52 dp ring, 4 dp stroke, 64 dp overall, sweeping counter-clockwise from twelve o'clock, on
     * the same heat ramp (quenched steel -> ember -> red, flat spent-red once you're over) as the
     * in-session readouts. Same numbers, same rhythm — it should read as the bubble following
     * you out of the app, not a second timer.
     */
    private class BubbleView(context: Context) : View(context) {
        private var remaining = 0
        private var total = 1

        private val plate = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = GLASS }
        private val hairline = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = OUTLINE; style = Paint.Style.STROKE
        }
        private val track = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = TRACK; style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }
        private val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }
        private val halo = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE; strokeCap = Paint.Cap.ROUND
        }
        private val label = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }

        fun update(remainingSec: Int, totalSec: Int) {
            // Negative now means overtime — see onDraw(). No floor here any more.
            remaining = remainingSec
            total = totalSec.coerceAtLeast(1)
            invalidate()
        }

        /** Same ramp as the in-session readouts: >50% steel->ember, below that ember->red. Only
         *  used before zero — overtime is a flat HEAT_SPENT, see onDraw(). */
        private fun ringColour(fraction: Float): Int = if (fraction > 0.5f) {
            lerpColor(HEAT_HOT, HEAT_READY, (fraction - 0.5f) / 0.5f)
        } else {
            lerpColor(HEAT_SPENT, HEAT_HOT, (fraction / 0.5f).coerceIn(0f, 1f))
        }

        override fun onDraw(canvas: Canvas) {
            val d = context.resources.displayMetrics.density
            val cx = width / 2f
            val cy = height / 2f
            val overtime = remaining < 0

            // The last five seconds pulse quick and sharp; overtime pulses slow and gentle — it
            // has to stay bearable for however long you're actually over, not just five seconds.
            val finalWindow = remaining in 1..5
            if ((finalWindow || overtime) && animationsEnabled(context)) {
                val period = if (finalWindow) 520L else 1_400L
                val amp = if (finalWindow) 0.08f else 0.05f
                val phase = (SystemClock.uptimeMillis() % period) / period.toFloat()
                val wave = kotlin.math.sin(phase * 2f * Math.PI).toFloat()
                canvas.scale(1f + amp * wave, 1f + amp * wave, cx, cy)
                postInvalidateOnAnimation()
            }

            // the glass plate: 64 dp round-rect, matching GlassSurface's 32 dp corner
            val plateR = 32f * d
            canvas.drawCircle(cx, cy, plateR, plate)
            hairline.strokeWidth = 1f * d
            canvas.drawCircle(cx, cy, plateR - 0.5f * d, hairline)

            val stroke = 4f * d
            track.strokeWidth = stroke
            ring.strokeWidth = stroke
            val r = (52f * d - stroke) / 2f
            val box = RectF(cx - r, cy - r, cx + r, cy + r)

            canvas.drawArc(box, -90f, 360f, false, track)

            // Overtime holds the ring fully lit instead of shrinking it further — it already
            // told you time was up once; the second lap reads as "still over", not "hurry".
            val fraction = if (overtime) 1f else (remaining / total.toFloat()).coerceIn(0f, 1f)
            val colour = if (overtime) HEAT_SPENT else ringColour(fraction)
            val finalCountdown = remaining in 1..5
            val glowing = finalCountdown || overtime
            if (glowing) {
                halo.strokeWidth = stroke * 2.4f
                halo.color = (colour and 0x00FFFFFF) or (0x59 shl 24)   // ~35% alpha
                canvas.drawArc(box, -90f, -360f * fraction, false, halo)
            }
            ring.color = colour
            canvas.drawArc(box, -90f, -360f * fraction, false, ring)

            label.textSize = 14f * d
            label.color = if (glowing) colour else CHALK
            canvas.drawText(
                TimeFormat.signedMmss(remaining), cx, cy - (label.descent() + label.ascent()) / 2f, label,
            )
        }

        /**
         * Oklab interpolation, because that is what Compose's Color lerp does and this bubble
         * must match it. Interpolating ember -> steel in plain RGB passes through grey: at ~0.8
         * of the way the ring came out #A3A4A9 while the in-app one was still clearly blue.
         */
        private fun lerpColor(from: Int, to: Int, t: Float): Int {
            val f = t.coerceIn(0f, 1f)
            val a = toOklab(from)
            val b = toOklab(to)
            return fromOklab(
                a[0] + (b[0] - a[0]) * f,
                a[1] + (b[1] - a[1]) * f,
                a[2] + (b[2] - a[2]) * f,
            )
        }

        private fun srgbToLinear(c: Float): Float =
            if (c <= 0.04045f) c / 12.92f else ((c + 0.055f) / 1.055f).toDouble().pow(2.4).toFloat()

        private fun linearToSrgb(c: Float): Float =
            if (c <= 0.0031308f) c * 12.92f
            else (1.055f * c.toDouble().pow(1.0 / 2.4) - 0.055).toFloat()

        private fun toOklab(color: Int): FloatArray {
            val r = srgbToLinear(((color shr 16) and 0xFF) / 255f)
            val g = srgbToLinear(((color shr 8) and 0xFF) / 255f)
            val b = srgbToLinear((color and 0xFF) / 255f)
            val l = cbrt(0.4122214708f * r + 0.5363325363f * g + 0.0514459929f * b)
            val m = cbrt(0.2119034982f * r + 0.6806995451f * g + 0.1073969566f * b)
            val s2 = cbrt(0.0883024619f * r + 0.2817188376f * g + 0.6299787005f * b)
            return floatArrayOf(
                0.2104542553f * l + 0.7936177850f * m - 0.0040720468f * s2,
                1.9779984951f * l - 2.4285922050f * m + 0.4505937099f * s2,
                0.0259040371f * l + 0.7827717662f * m - 0.8086757660f * s2,
            )
        }

        private fun fromOklab(okL: Float, okA: Float, okB: Float): Int {
            val l = (okL + 0.3963377774f * okA + 0.2158037573f * okB).let { it * it * it }
            val m = (okL - 0.1055613458f * okA - 0.0638541728f * okB).let { it * it * it }
            val s2 = (okL - 0.0894841775f * okA - 1.2914855480f * okB).let { it * it * it }
            fun ch(v: Float) = (linearToSrgb(v).coerceIn(0f, 1f) * 255f).roundToInt()
            return (0xFF shl 24) or
                (ch(4.0767416621f * l - 3.3077115913f * m + 0.2309699292f * s2) shl 16) or
                (ch(-1.2684380046f * l + 2.6097574011f * m - 0.3413193965f * s2) shl 8) or
                ch(-0.0041960863f * l - 0.7034186147f * m + 1.7076147010f * s2)
        }

        private fun cbrt(v: Float): Float = v.toDouble().pow(1.0 / 3.0).toFloat()
    }

    companion object {
        // 52 dp ring + 6 dp padding either side, matching the in-session readouts' spec
        private const val SIZE_DP = 64
        private const val TOUCH_SLOP = 12f
        private const val ENTER_MS = 180L
        private const val EXIT_MS = 140L
        private const val ENTER_SCALE = 0.82f
        private const val EXIT_SCALE = 0.82f
        // v10 tokens, mirrored from ui/theme/Color.kt — this file cannot read the Compose theme
        private val GLASS = Color.parseColor("#E61A1918")    // GlassSurface over an unknown backdrop
        private val OUTLINE = Color.parseColor("#332C2A27")  // OutlineDark, faint
        private val TRACK = Color.parseColor("#FF201F1D")    // outlineVariant
        private val CHALK = Color.parseColor("#FFEDE6D8")    // onSurface
        private val HEAT_READY = Color.parseColor("#FF8FB4C7")
        private val HEAT_HOT = Color.parseColor("#FFFF5A1F")
        private val HEAT_SPENT = Color.parseColor("#FFFF3320")

        /** The overlay permission is a settings toggle, not a runtime dialog. */
        fun canDraw(context: Context): Boolean =
            Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(context)

        /** Reduce-motion is not a preference we get to override, so the pulse and the
         *  enter/exit fade both honour it. */
        private fun animationsEnabled(context: Context): Boolean = runCatching {
            Settings.Global.getFloat(
                context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
            ) != 0f
        }.getOrDefault(true)
    }
}
