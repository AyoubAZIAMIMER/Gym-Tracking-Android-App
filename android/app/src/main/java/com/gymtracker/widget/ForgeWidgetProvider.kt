// Purpose: Home-screen widget — week streak + days since last strike in forge heat
//          colors; the identity on the launcher, pulling the smith back to the anvil
// Inputs: WorkoutRepository home stats (fetched off the main thread via goAsync)
// Outputs: RemoteViews on the launcher; taps open MainActivity
package com.gymtracker.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.compose.ui.graphics.toArgb
import com.gymtracker.MainActivity
import com.gymtracker.R
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.theme.DarkHeat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ForgeWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        // Room read must leave the broadcast's main thread; goAsync keeps us alive
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val views = buildViews(context)
                appWidgetIds.forEach { appWidgetManager.updateAppWidget(it, views) }
            } finally {
                pending.finish()
            }
        }
    }

    private suspend fun buildViews(context: Context): RemoteViews {
        val stats = WorkoutRepository.get(context).homeStats()
        val days = stats.lastWorkoutDaysAgo
        // The widget is a physical iron plate — always the night forge, regardless of
        // system theme, so heat reads from the dark scale
        val heat = DarkHeat.at(((days ?: 3) / 3f).coerceIn(0f, 1f)).toArgb()

        return RemoteViews(context.packageName, R.layout.widget_forge).apply {
            setTextViewText(R.id.widget_streak, "${stats.streakWeeks}")
            setTextViewText(
                R.id.widget_since,
                when (days) {
                    null -> "no strikes yet — fire it up"
                    0 -> "struck today"
                    1 -> "1d since last strike"
                    else -> "${days}d since last strike"
                },
            )
            setTextColor(R.id.widget_since, heat)
            setInt(R.id.widget_heat_dot, "setColorFilter", heat)
            // one gesture in (UX v6): the widget doesn't open the app, it fires it up
            setOnClickPendingIntent(
                R.id.widget_root,
                PendingIntent.getActivity(
                    context, 0,
                    Intent(context, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_FIRE_UP, true),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                ),
            )
        }
    }

    companion object {
        /** Push fresh stats to any placed widgets (after save/import/app open). */
        fun requestUpdate(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                ComponentName(context, ForgeWidgetProvider::class.java)
            )
            if (ids.isEmpty()) return
            context.sendBroadcast(
                Intent(AppWidgetManager.ACTION_APPWIDGET_UPDATE)
                    .setComponent(ComponentName(context, ForgeWidgetProvider::class.java))
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
            )
        }
    }
}
