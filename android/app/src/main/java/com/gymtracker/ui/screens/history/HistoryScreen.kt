// Purpose: History tab — month calendar of training days + tappable workout log
// Inputs: HistoryViewModel state
// Outputs: onOpenWorkout(workoutId) navigation to the workout detail subpage
package com.gymtracker.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedEntrance

@Composable
fun HistoryScreen(
    onOpenWorkout: (String) -> Unit = {},
    vm: HistoryViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    // Forged Motion §10: rows arrive once per screen entry — rememberSaveable keeps
    // tab returns and back-navigation from replaying the entrance
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("History", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = state.monthSummary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            MonthCalendar(state, vm::previousMonth, vm::nextMonth, vm::toggleDay)

            state.rows.forEachIndexed { index, row ->
                GlassSurface(
                    modifier = Modifier.forgedEntrance(index, entered),
                    onClick = { onOpenWorkout(row.workoutId) },
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = row.title,
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = row.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = row.detail,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (row.muscles.isNotEmpty()) {
                            Text(
                                text = row.muscles,
                                style = MaterialTheme.typography.labelMedium,
                                color = GymTheme.colors.hint,
                            )
                        }
                    }
                }
            }
            if (state.rows.isEmpty()) {
                Text(
                    text = "Nothing forged here yet — finish a session and it lands in this list.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GymTheme.colors.hint,
                )
            }

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(88.dp)
            )
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
    GlassSurface {
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
        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f + 0.42f * ratio)
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
