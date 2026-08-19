// Purpose: History tab — month calendar of training days + tappable workout log
// Inputs: HistoryViewModel state
// Outputs: onOpenWorkout(workoutId) navigation to the workout detail subpage
package com.gymtracker.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.components.ForgedListRow
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.forgeHero
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.theme.Dim
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.text.font.FontWeight
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.StampLabel
import com.gymtracker.ui.theme.forgedPress
import java.time.LocalDate
import java.util.Locale
import com.gymtracker.ui.theme.forgedEntrance
import com.gymtracker.ui.theme.rollUpValue
import com.gymtracker.utils.Formats
import kotlin.math.roundToInt

@Composable
fun HistoryScreen(
    onOpenWorkout: (String) -> Unit = {},
    onBack: () -> Unit = {},
    vm: HistoryViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    // Forged Motion §10: rows arrive once per screen entry — rememberSaveable keeps
    // tab returns and back-navigation from replaying the entrance
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    GlowBackground(glowAlpha = 0.10f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            // pushed destination now, not a tab — so it carries a back arrow
            ForgedScreenTitle("History", onBack = onBack)

            // the prototype leads with two Anton figures, not a sentence
            SectionRule()
            Row(
                Modifier.padding(
                    start = Dim.screenPadH, end = Dim.screenPadH, top = 18.dp, bottom = 20.dp,
                ),
                horizontalArrangement = Arrangement.spacedBy(26.dp),
            ) {
                val rolledWorkouts = rollUpValue(state.monthWorkouts.toFloat()).roundToInt()
                val rolledVolume = rollUpValue(state.monthVolumeKg.toFloat())
                HeadlineStat("$rolledWorkouts", null, "sessions this month")
                HeadlineStat(Formats.volumeKg(rolledVolume.toDouble()), "kg", "lifted this month")
            }

            SectionRule()
            MonthCalendar(state, vm::previousMonth, vm::nextMonth, vm::toggleDay)

            // README §8: sessions grouped by week under mono headers, each row led by a
            // 44dp mono day column. Rows arrive newest-first, so the first group is "SESSIONS".
            SectionRule()
            var lastBucket: String? = null
            state.rows.forEach { row ->
                val bucket = weekBucketLabel(row.day, state.monthLabel)
                if (bucket != lastBucket) {
                    ForgedSectionHeader(bucket)
                    lastBucket = bucket
                }
                RowRule()
                HistorySessionRow(row, onClick = { onOpenWorkout(row.workoutId) })
            }
            if (state.rows.isEmpty()) {
                Text(
                    text = "Nothing here yet — finish a session and it lands in this list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymTheme.colors.hint,
                    modifier = Modifier.padding(horizontal = Dim.screenPadH, vertical = 12.dp),
                )
            }

            Spacer(Modifier.navigationBarsPadding().height(Dim.listBottomSpacer))
        }
    }
}

@Composable
private fun MonthCalendar(
    state: HistoryUiState,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDayTap: (Int) -> Unit,
) {
    // BUILD_ORDER step 4: the calendar is History's ONE hero; the log below stays flat
    Box(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = Dim.screenPadH)
            .forgeHero()
    ) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPrev, enabled = state.canGoPrev) {
                    Icon(
                        Icons.Rounded.ChevronLeft,
                        contentDescription = "Previous month",
                        tint = if (state.canGoPrev) MaterialTheme.colorScheme.onSurfaceVariant
                        else GymTheme.colors.hint.copy(alpha = 0.4f),
                    )
                }
                Text(
                    text = state.monthLabel,
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onNext, enabled = state.canGoNext) {
                    Icon(
                        Icons.Rounded.ChevronRight,
                        contentDescription = "Next month",
                        tint = if (state.canGoNext) MaterialTheme.colorScheme.onSurfaceVariant
                        else GymTheme.colors.hint.copy(alpha = 0.4f),
                    )
                }
            }

            Row(Modifier.fillMaxWidth()) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach { d ->
                    Text(
                        text = d,
                        style = MaterialTheme.typography.labelMedium,
                        color = GymTheme.colors.hint,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            Spacer(Modifier.height(4.dp))

            val cells: List<Int?> =
                List(state.leadingBlanks) { null } + (1..state.daysInMonth).map { it }
            cells.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { day -> DayCell(day, state, onDayTap) }
                    repeat(7 - week.size) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun RowScope.DayCell(
    day: Int?,
    state: HistoryUiState,
    onDayTap: (Int) -> Unit,
) {
    if (day == null) {
        Spacer(Modifier.weight(1f))
        return
    }
    val volume = state.volumeByDay[day] ?: 0.0
    val trained = volume > 0.0
    // heavier day → stronger volt fill, so the month reads as a heat map
    val ratio = if (state.maxDayVolume > 0) (volume / state.maxDayVolume).toFloat() else 0f
    val fill = if (trained) {
        // heavier day = hotter, from the heat scale (not the chrome colour)
        GymTheme.colors.heat.hot.copy(alpha = 0.16f + 0.42f * ratio)
    } else Color.Transparent
    val selected = state.selectedDay == day
    val isToday = state.todayDay == day
    Box(
        Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(3.dp)
            .clip(CircleShape)
            .background(fill)
            .then(
                when {
                    selected -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    isToday -> Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        CircleShape,
                    )
                    else -> Modifier
                }
            )
            .then(if (trained) Modifier.clickable { onDayTap(day) } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$day",
            style = MaterialTheme.typography.labelMedium,
            color = if (trained) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Prototype stat pair: an Anton figure with a small unit, over a muted caption. */
@Composable
private fun HeadlineStat(value: String, unit: String?, caption: String) {
    Column {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontFeatureSettings = FONT_FEATURE_TABULAR,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
            if (unit != null) {
                Text(
                    text = unit,
                    fontSize = 11.sp,
                    color = GymTheme.colors.hint,
                    modifier = Modifier.padding(start = 3.dp, bottom = 2.dp),
                )
            }
        }
        Text(
            text = caption,
            fontSize = 11.5.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 1.dp),
        )
    }
}

/**
 * Week bucket for the grouped list. The calendar already scopes the view to one month, so
 * buckets are "this week" vs how many weeks back within it — enough to break a long month
 * into readable groups without inventing a second date model.
 */
private fun weekBucketLabel(dayOfMonth: Int, monthLabel: String): String {
    val today = LocalDate.now()
    val sameMonth = monthLabel.startsWith(
        today.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
    )
    if (!sameMonth) return "SESSIONS"
    val weeksBack = ((today.dayOfMonth - dayOfMonth) / 7)
    return when {
        weeksBack <= 0 -> "SESSIONS"
        weeksBack == 1 -> "LAST WEEK"
        else -> "$weeksBack WEEKS AGO"
    }
}

/** 44dp mono day column · name (+ gold star on a PR) · duration caption · Anton volume. */
@Composable
private fun HistorySessionRow(row: HistoryListRow, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .forgedPress(interaction, pressedScale = 0.99f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = Dim.screenPadH, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = row.day.toString().padStart(2, '0'),
            style = StampLabel.copy(fontSize = 13.sp, letterSpacing = 0.5.sp),
            color = GymTheme.colors.hint,
            modifier = Modifier.width(44.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                text = row.title,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = row.subtitle,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        Text(
            text = row.detail.substringAfter("·").trim().ifBlank { row.detail },
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 17.sp,
                fontFeatureSettings = FONT_FEATURE_TABULAR,
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
