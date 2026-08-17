// Purpose: Foreground countdown for rest periods, so the timer survives app-switch
// Inputs: intents (START seconds / ADD_15 / STOP) via the companion helpers
// Outputs: RestState via companion StateFlow (drives the in-app bubble) + a live notification
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
import androidx.core.app.NotificationCompat
import com.gymtracker.R
import com.gymtracker.utils.TimeFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
     */
    private var deadlineElapsedMs = 0L
    private var total = 0

    /** The floating bubble. Null-safe no-op unless the overlay permission is granted. */
    private val bubble by lazy { RestBubbleOverlay(this) }

    private val remaining: Int
        get() = (((deadlineElapsedMs - SystemClock.elapsedRealtime()) + 999) / 1000)
            .coerceAtLeast(0).toInt()

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
            ACTION_START -> startTimer(intent.getIntExtra(EXTRA_SECONDS, DEFAULT_SECONDS))
            ACTION_ADD_15 -> if (ticker != null) {
                deadlineElapsedMs += 15_000
                total = maxOf(total, remaining)
                publish()
                updateNotification()
            }
            ACTION_STOP -> stopTimer()
        }
        return START_NOT_STICKY
    }

    /**
     * Exactly one bubble at a time. The session screen draws its own Compose bubble, so the
     * overlay is for when our UI is gone — showing both put two timers on screen at once.
     */
    private fun syncBubble(remainingSec: Int) {
        if (remainingSec > 0 && !AppForeground.visible.value) {
            bubble.show(remainingSec, total)
        } else {
            bubble.hide()
        }
    }

    private fun startTimer(seconds: Int) {
        total = seconds.coerceAtLeast(1)
        deadlineElapsedMs = SystemClock.elapsedRealtime() + total * 1_000L
        createChannels()
        startForegroundWithType()
        publish()
        ticker?.cancel()
        ticker = scope.launch {
            // tick on the second the deadline actually falls on, so the displayed number never
            // sits a beat behind; a late wake-up just skips a value instead of stretching time
            while (true) {
                val leftMs = deadlineElapsedMs - SystemClock.elapsedRealtime()
                if (leftMs <= 0) break
                delay((leftMs % 1_000L).takeIf { it > 0 } ?: 1_000L)
                if (deadlineElapsedMs - SystemClock.elapsedRealtime() <= 0) break
                // in-app bubble only; the notification draws its own countdown
                publish()
            }
            publish()
            notifyDone()
            stopTimer()
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
        val openApp = packageManager.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }
        return NotificationCompat.Builder(this, CHANNEL_TICK)
            .setSmallIcon(R.drawable.ic_stat_timer)
            .setContentTitle("Rest timer")
            // The system renders the countdown itself from `when`. Re-posting a rebuilt
            // notification every second (which is what this used to do) collapsed the card on
            // each tick, so the +15s / Skip actions were unreachable in practice — and it burned
            // a wakeup per second for something SystemUI can draw for free.
            .setWhen(System.currentTimeMillis() + (deadlineElapsedMs - SystemClock.elapsedRealtime()))
            .setUsesChronometer(true)
            .setChronometerCountDown(true)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            // The accent stays ember even though the app's chrome is chalk: this notification
            // IS the rest clock, so it is data, and chalk would vanish on a light shade.
            .setColor(NOTIFICATION_ACCENT)
            .setCategory(NotificationCompat.CATEGORY_STOPWATCH)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(0, "+15s", servicePendingIntent(ACTION_ADD_15, 1))
            .addAction(0, "Skip", servicePendingIntent(ACTION_STOP, 2))
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

        const val DEFAULT_SECONDS = 120

        private const val ACTION_START = "com.gymtracker.rest.START"
        private const val ACTION_ADD_15 = "com.gymtracker.rest.ADD_15"
        private const val ACTION_STOP = "com.gymtracker.rest.STOP"
        private const val EXTRA_SECONDS = "seconds"
        private const val CHANNEL_TICK = "rest_timer"
        private const val CHANNEL_DONE = "rest_done"
        /** Heat, not chrome — see buildNotification(). */
        private const val NOTIFICATION_ACCENT = 0xFFFF5A1F.toInt()

        private const val NOTIFICATION_ID = 42
        private const val DONE_NOTIFICATION_ID = 43

        fun start(context: Context, seconds: Int = DEFAULT_SECONDS) {
            context.startForegroundService(
                intent(context, ACTION_START).putExtra(EXTRA_SECONDS, seconds)
            )
        }

        fun add15(context: Context) {
            context.startService(intent(context, ACTION_ADD_15))
        }

        fun stop(context: Context) {
            context.startService(intent(context, ACTION_STOP))
        }

        private fun intent(context: Context, action: String) =
            Intent(context, RestTimerService::class.java).setAction(action)
    }
}
