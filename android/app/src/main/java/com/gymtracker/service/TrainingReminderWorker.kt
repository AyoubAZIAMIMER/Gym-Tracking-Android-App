// Purpose: Evening training nudge — fires ~18:00 only when today has no workout and
//          the weekly goal isn't met yet; silent otherwise (opt-out via its channel)
// Inputs: WorkoutRepository (home stats, weekly goal, next program day)
// Outputs: one notification tapping into MainActivity; scheduled from MainActivity
package com.gymtracker.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gymtracker.MainActivity
import com.gymtracker.R
import com.gymtracker.data.WorkoutRepository
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.concurrent.TimeUnit

class TrainingReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val repo = WorkoutRepository.get(ctx)
        val stats = repo.homeStats()
        // Nudge only when it's useful: has history, hasn't trained today, week unfinished
        if (!stats.hasData) return Result.success()
        if (stats.lastWorkoutDaysAgo == 0) return Result.success()
        if (stats.workoutsThisWeek >= repo.profile().weeklyGoal) return Result.success()

        val days = stats.lastWorkoutDaysAgo ?: return Result.success()
        val next = repo.nextProgramDay()
        val text = buildString {
            if (next != null) append("${next.day.name} is waiting — ")
            append(if (days == 1) "1 day since your last session." else "$days days since your last session.")
        }

        val manager = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(
            NOTIFICATION_ID,
            NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_widget_flame)
                .setContentTitle("Time to train")
                .setContentText(text)
                .setAutoCancel(true)
                .setContentIntent(
                    PendingIntent.getActivity(
                        ctx, 0,
                        Intent(ctx, MainActivity::class.java),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                )
                // one gesture in (UX v6): the notification IS the start button
                .addAction(
                    0, "Start now",
                    PendingIntent.getActivity(
                        ctx, 1,
                        Intent(ctx, MainActivity::class.java)
                            .putExtra(MainActivity.EXTRA_FIRE_UP, true),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    ),
                )
                .build(),
        )
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "training_reminders"
        private const val NOTIFICATION_ID = 41
        private const val WORK_NAME = "training_reminder"

        /** Idempotent: KEEP preserves the original 18:00 anchor across app launches. */
        fun schedule(context: Context) {
            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID, "Training reminders", NotificationManager.IMPORTANCE_DEFAULT
                ).apply { description = "Evening reminder on days you haven't trained" }
            )

            val now = LocalDateTime.now()
            var firstRun = now.toLocalDate().atTime(LocalTime.of(18, 0))
            if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<TrainingReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(Duration.between(now, firstRun))
                    .build(),
            )
        }
    }
}
