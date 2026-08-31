// Purpose: Foreground countdown for rest periods, so the timer survives app-switch. Runs past
//          zero rather than stopping — going over the time you set is shown, not hidden.
// Inputs: intents (START seconds / ADD_15 / STOP) via the companion helpers
// Outputs: RestState via companion StateFlow (drives the floating overlay and the in-session
//          readouts) + a live notification
package com.gymtracker.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.VibratorManager
import android.os.Vibrator
import android.os.VibrationEffect
import androidx.core.app.NotificationCompat
import com.gymtracker.MainActivity
import com.gymtracker.R
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.utils.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RestTimerService : Service() {

    data class RestState(val remainingSec: Int, val totalSec: Int)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var ticker: Job? = null

    /**
     * Absolute finish time on the monotonic clock, NOT a countdown of ticks.
     *
     * The old loop was `while (remaining > 0) { delay(1000); remaining-- }`, which counts how
     * many times the coroutine woke up. Background the app and the OS stops waking it on the
     * second, so the timer drifted: it resumed from wherever it had got to instead of from
     * where the clock actually was. elapsedRealtime() keeps running while the device sleeps, so
     * deriving `remaining` from a deadline is self-correcting — a missed tick costs nothing.
     *
     * The same deadline keeps working once you're over: `remaining` just goes negative, and
     * +15s naturally pulls it back across zero with no separate "am I in overtime" state to
     * keep in sync.
     */
    private var deadlineElapsedMs = 0L
    private var total = 0

    /** What the expanded notification names, e.g. "Squat (Barbell) · set 2 of 4" / "100 kg × 8".
     *  Null when the caller didn't supply session context (the manual rest-sheet start). */
    private var setLabel: String? = null
    private var upNext: String? = null

    /** The floating bubble. Null-safe no-op unless the overlay permission is granted. */
    private val bubble by lazy { RestBubbleOverlay(this) }

    private val vibrator: Vibrator? by lazy {
        if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Vibrator::class.java)
        }
    }

    /** Guards against re-buzzing the same second when publish() runs more than once for it. */
    private var lastBuzzedSecond = -1

    /** Fires the one-shot "rest over" alert and flips the notification into overtime exactly
     *  once per crossing — re-arms itself if +15s pulls the timer back under the target. */
    private var overtimeAlerted = false

    /**
     * Positive while counting down, negative once you're over — e.g. -15 means 15 seconds past
     * the time you set. Rounds the countdown up (so the last partial second still reads "1")
     * and the overtime down (so "+0:00" appears the instant you cross zero, not a second late).
     */
    private val remaining: Int
        get() {
            val diffMs = deadlineElapsedMs - SystemClock.elapsedRealtime()
            return if (diffMs >= 0) ((diffMs + 999) / 1000).toInt()
            else -((-diffMs) / 1000).toInt()
        }

    override fun onCreate() {
        super.onCreate()
        // react the instant the app leaves or returns, not on the next one-second tick
        scope.launch {
            AppForeground.visible.collect { if (ticker != null) syncBubble(remaining) }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startTimer(
                intent.getIntExtra(EXTRA_SECONDS, DEFAULT_SECONDS),
                intent.getStringExtra(EXTRA_SET_LABEL),
                intent.getStringExtra(EXTRA_UP_NEXT),
            )
            // Lock-screen "Log set" action — the running app (if any) does the actual logging;
            // this just wakes it up. No-op with nothing listening (app process is dead).
            ACTION_LOG_SET -> _logSetRequests.tryEmit(Unit)
            ACTION_ADD_15 -> if (ticker != null) {
                deadlineElapsedMs += 15_000
                lastBuzzedSecond = -1        // +15s leaves the final-five window
                total = maxOf(total, remaining)
                publish()
                updateNotification()
            }
            // The rest-actions sheet's -15/+15 chips — same deadline shift as ACTION_ADD_15,
            // just signed, so a negative delta can pull time back off a running rest too.
            ACTION_ADJUST -> if (ticker != null) {
                val deltaSeconds = intent.getIntExtra(EXTRA_DELTA_SECONDS, 0)
                deadlineElapsedMs += deltaSeconds * 1_000L
                if (deltaSeconds > 0) lastBuzzedSecond = -1
                total = maxOf(total, remaining)
                publish()
                updateNotification()
            }
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    /**
     * The session screen shows its own inline readout, so this overlay is only for when our UI
     * is gone. No floor at zero any more: it stays up through overtime too, climbing past the
     * time you set for as long as you're away from the app.
     */
    private fun syncBubble(remainingSec: Int) {
        if (!AppForeground.visible.value) {
            bubble.show(remainingSec, total)
        } else {
            bubble.hide()
        }
    }

    /**
     * The last five seconds are felt, not just seen: a tick on each of 5..1 and a firmer buzz at
     * zero. This lives in the service rather than the Compose bubble so it fires whichever bubble
     * is on screen — or neither, when only the notification is up. Respects Settings -> Haptics.
     */
    private fun buzzFinalSeconds(remainingSec: Int) {
        // remainingSec can now run negative in overtime, and a negative number is not > 5 —
        // without the lower bound this would buzz every second for as long as you're over.
        if (remainingSec !in 0..5 || remainingSec == lastBuzzedSecond) return
        lastBuzzedSecond = remainingSec
        if (!WorkoutRepository.get(this).settings().haptics) return
        val v = vibrator?.takeIf { it.hasVibrator() } ?: return
        val effect = if (remainingSec == 0) {
            VibrationEffect.createOneShot(160, VibrationEffect.DEFAULT_AMPLITUDE)
        } else {
            VibrationEffect.createOneShot(28, 90)
        }
        runCatching { v.vibrate(effect) }
    }

    private fun startTimer(seconds: Int, setLabel: String? = null, upNext: String? = null) {
        total = seconds.coerceAtLeast(1)
        this.setLabel = setLabel
        this.upNext = upNext
        lastBuzzedSecond = -1
        overtimeAlerted = false
        deadlineElapsedMs = SystemClock.elapsedRealtime() + total * 1_000L
        createChannels()
        startForegroundWithType()
        publish()
        ticker?.cancel()
        ticker = scope.launch {
            // Ticks on the second the deadline actually falls on, in both directions, so the
            // displayed number never sits a beat behind; a late wake-up just skips a value
            // instead of stretching time. Never stops itself — rest keeps counting once you're
            // over, until Skip, the next set restarts it, or Finish tears the service down.
            while (true) {
                val diffMs = deadlineElapsedMs - SystemClock.elapsedRealtime()
                val delayMs = if (diffMs > 0) {
                    (diffMs % 1_000L).takeIf { it > 0 } ?: 1_000L
                } else {
                    1_000L - ((-diffMs) % 1_000L)
                }
                delay(delayMs)
                publish()
            }
        }
    }

    private fun stopTimer() {
        ticker?.cancel()
        ticker = null
        bubble.hide()
        _state.value = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun publish() {
        val left = remaining
        _state.value = RestState(left, total)
        syncBubble(left)
        buzzFinalSeconds(left)
        if (left < 0) {
            if (!overtimeAlerted) {
                overtimeAlerted = true
                notifyDone()
                updateNotification()   // flips the chronometer from counting down to counting up
            }
        } else {
            overtimeAlerted = false
        }
    }

    private fun startForegroundWithType() {
        // API 34+ requires an explicit foreground service type at start time
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, buildNotification())
        }
    }

    private fun buildNotification(): Notification {
        // one gesture in (UX v6): tapping the notification fires the app straight into the
        // session, same as the widget and the training reminder — not a bare launcher open.
        val openApp = PendingIntent.getActivity(
            this,
            // Distinct request code from the training-reminder notification (code 0) and the
            // widget (code 2) — PendingIntent identity ignores extras, so sharing a code here
            // let the reminder's fire-up-less intent silently clobber this one's EXTRA_FIRE_UP.
            3,
            Intent(this, MainActivity::class.java)
                .putExtra(MainActivity.EXTRA_FIRE_UP, true),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val diffMs = deadlineElapsedMs - SystemClock.elapsedRealtime()
        val overtime = diffMs < 0
        // Reference: lock-screen live timer — "Squat (Barbell) · set 2 of 4" as the line under
        // the countdown, "Up next: 100 kg × 8" in the expanded body. Falls back to the old
        // generic text when the caller has no session context (manual rest-sheet start).
        val line = setLabel ?: if (overtime) "Extra rest — log when you're ready" else null
        val expanded = if (setLabel != null && upNext != null) "$setLabel\nUp next: $upNext" else line
        return NotificationCompat.Builder(this, CHANNEL_TICK)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle(if (overtime) "Over rest" else "Rest timer")
            .apply {
                line?.let { setContentText(it) }
                expanded?.let { setStyle(NotificationCompat.BigTextStyle().bigText(it)) }
            }
            // The system renders the countdown itself from `when`. Re-posting a rebuilt
            // notification every second (which is what this used to do) collapsed the card on
            // each tick, so the +15s / Skip actions were unreachable in practice — and it burned
            // a wakeup per second for something SystemUI can draw for free. Going into overtime
            // is the one moment this gets rebuilt: `when` moves into the past and the chronometer
            // flips from counting down to counting up, so it keeps climbing on its own from there.
            .setWhen(System.currentTimeMillis() + diffMs)
            .setUsesChronometer(true)
            .setChronometerCountDown(!overtime)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            // The accent stays ember even though the app's chrome is chalk: this notification
            // IS the rest clock, so it is data, and chalk would vanish on a light shade. Overtime
            // deepens it to the same red the ring and the in-session readouts turn.
            .setColor(if (overtime) NOTIFICATION_ACCENT_OVERTIME else NOTIFICATION_ACCENT)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "Log set", servicePendingIntent(ACTION_LOG_SET, 3))
            .addAction(0, "+15s", servicePendingIntent(ACTION_ADD_15, 1))
            .build()
    }

    private fun updateNotification() {
        // POST_NOTIFICATIONS may be denied on 13+ — the timer itself keeps running
        try {
            getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, buildNotification())
        } catch (_: SecurityException) {
        }
    }

    private fun notifyDone() {
        val done = NotificationCompat.Builder(this, CHANNEL_DONE)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Rest over")
            .setContentText("Time for the next set.")
            .setAutoCancel(true)
            .setColor(NOTIFICATION_ACCENT)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        try {
            getSystemService(NotificationManager::class.java).notify(DONE_NOTIFICATION_ID, done)
        } catch (_: SecurityException) {
        }
    }

    private fun createChannels() {
        val manager = getSystemService(NotificationManager::class.java)
        // LOW keeps the per-second countdown silent; HIGH lets "rest over" alert
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_TICK, "Rest timer", NotificationManager.IMPORTANCE_LOW)
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_DONE, "Rest finished", NotificationManager.IMPORTANCE_HIGH)
        )
    }

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent =
        PendingIntent.getService(this, requestCode, intent(this, action), PendingIntent.FLAG_IMMUTABLE)

    override fun onDestroy() {
        bubble.hide()
        ticker?.cancel()
        scope.cancel()
        _state.value = null
        super.onDestroy()
    }

    companion object {
        private val _state = MutableStateFlow<RestState?>(null)

        /** Null when idle. The session screen's bubble collects this. */
        val state: StateFlow<RestState?> = _state.asStateFlow()

        private val _logSetRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        /** Fires when the lock-screen/notification "Log set" action is tapped. The ViewModel
         *  collects this while a session is alive and logs the active set — same as tapping
         *  its row in-app. No-op (buffered, then dropped) if nothing is collecting. */
        val logSetRequests: SharedFlow<Unit> = _logSetRequests.asSharedFlow()

        const val DEFAULT_SECONDS = 120

        private const val ACTION_START = "com.gymtracker.rest.START"
        private const val ACTION_ADD_15 = "com.gymtracker.rest.ADD_15"
        private const val ACTION_ADJUST = "com.gymtracker.rest.ADJUST"
        private const val ACTION_STOP = "com.gymtracker.rest.STOP"
        private const val ACTION_LOG_SET = "com.gymtracker.rest.LOG_SET"
        private const val EXTRA_SECONDS = "seconds"
        private const val EXTRA_DELTA_SECONDS = "delta_seconds"
        private const val EXTRA_SET_LABEL = "set_label"
        private const val EXTRA_UP_NEXT = "up_next"
        private const val CHANNEL_TICK = "rest_timer"
        private const val CHANNEL_DONE = "rest_done"
        /** Heat, not chrome — see buildNotification(). */
        private const val NOTIFICATION_ACCENT = 0xFFFF5A1F.toInt()
        /** Same red the ring and the in-session readouts turn once you're over the time you set. */
        private const val NOTIFICATION_ACCENT_OVERTIME = 0xFFFF3320.toInt()

        private const val NOTIFICATION_ID = 42
        private const val DONE_NOTIFICATION_ID = 43

        fun start(
            context: Context,
            seconds: Int = DEFAULT_SECONDS,
            setLabel: String? = null,
            upNext: String? = null,
        ) {
            context.startForegroundService(
                intent(context, ACTION_START)
                    .putExtra(EXTRA_SECONDS, seconds)
                    .putExtra(EXTRA_SET_LABEL, setLabel)
                    .putExtra(EXTRA_UP_NEXT, upNext)
            )
        }

        fun add15(context: Context) {
            context.startService(intent(context, ACTION_ADD_15))
        }

        /** No-op if nothing is running — the sheet only shows these chips while [state] is non-null. */
        fun adjust(context: Context, deltaSeconds: Int) {
            context.startService(intent(context, ACTION_ADJUST).putExtra(EXTRA_DELTA_SECONDS, deltaSeconds))
        }

        fun stop(context: Context) {
            context.startService(intent(context, ACTION_STOP))
        }

        private fun intent(context: Context, action: String) =
            Intent(context, RestTimerService::class.java).setAction(action)
    }
}
