// Purpose: Home — the "Slate" direction: flat hairline-ruled sections, zero cards, matching
//          the vocabulary Plan already uses. Stateless: hand it a HomeUi from your existing
//          HomeViewModel; no business logic moves into the UI layer.
// Inputs: HomeUi, callbacks
// Outputs: HomeSlateScreen()
//
// STRUCTURE (top to bottom)
//   1. 2 dp week-progress rail pinned to the very top (sessions done / planned)
//   2. brand row: ForgedWordmark + date caption + 32 dp avatar (opens Settings)
//   3. TODAY        — readiness stamp + session name + meta + Start CTA + swap
//   4. THIS WEEK    — Anton "done / goal" + ForgedWeekStrip
//   5. NEXT UP      — upcoming program days, hidden when empty
//   6. RECENT       — last logged sessions, hidden when empty
//   7. bottom spacer Dim.listBottomSpacer so the last row clears the floating nav
package com.gymtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.ForgedCta
import com.gymtracker.ui.components.ForgedListRow
import com.gymtracker.ui.components.ForgedMark
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.ForgedWeekStrip
import com.gymtracker.ui.components.ForgedWordmark
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.StampText
import com.gymtracker.ui.components.forgeGround
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.ui.theme.LocalForge
import java.time.DayOfWeek

data class HomeUi(
    val dateCaption: String,            // "Fri 17 Jul · the forge is hot."
    val weekProgress: Float,            // 0f..1f — drives the 2 dp top rail
    val weekDone: Int,
    val weekGoal: Int,
    val doneWeekdays: Set<DayOfWeek>,
    val hero: HeroUi,
    val upcoming: List<UpcomingRowUi>,
    val recent: List<RecentSessionUi>,
    val avatarInitial: String = "",
)

data class HeroUi(
    val readinessTag: String,           // "QUADS WORN" — uppercase, tinted by heat
    val readinessFreshness: Float,      // 0f..1f -> heat.at()
    val sessionName: String,            // "Squat (Barbell)" or the session title
    val meta: String,                   // "Quads · Glutes · last set 100 × 9"
    /** "Start", or "Resume" while a session is live — the CTA must not lie about what it does. */
    val ctaLabel: String = "Start",
)

data class UpcomingRowUi(val name: String, val muscles: String, val dayLabel: String)

data class RecentSessionUi(val id: String, val name: String, val caption: String, val volume: String)

@Composable
fun HomeSlateScreen(
    ui: HomeUi,
    heatAt: (Float) -> Color,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenWorkout: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val forge = LocalForge.current
    val scheme = MaterialTheme.colorScheme
    val type = MaterialTheme.typography

    Box(modifier.fillMaxSize().forgeGround()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

            // 1 — week progress rail
            Box(Modifier.fillMaxWidth().height(2.dp).background(scheme.outlineVariant)) {
                Box(
                    Modifier
                        .fillMaxWidth(ui.weekProgress)
                        .height(2.dp)
                        .background(forge.palette.action)
                )
            }

            Spacer(Modifier.height(Dim.statusBand)) // replace with WindowInsets.statusBars

            // 2 — brand row
            Row(
                Modifier.fillMaxWidth().padding(horizontal = Dim.screenPadH),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ForgedWordmark(tint = forge.palette.action, labelColor = scheme.onSurface)
                Spacer(Modifier.weight(1f))
                Text(ui.dateCaption, style = type.bodySmall, color = scheme.onSurfaceVariant)
                Spacer(Modifier.width(12.dp))
                val avatarSource = remember { MutableInteractionSource() }
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(scheme.surfaceVariant)
                        .forgedPress(avatarSource, pressedScale = 0.94f)
                        .clickable(
                            interactionSource = avatarSource,
                            indication = null,
                            onClick = onOpenSettings,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    // no name saved yet (first run, or the profile sheet was skipped) — a blank
                    // circle reads as a broken avatar, so fall back to the mark
                    if (ui.avatarInitial.isBlank()) {
                        ForgedMark(size = 15.dp, tint = scheme.onSurfaceVariant)
                    } else {
                        Text(
                            text = ui.avatarInitial,
                            style = type.labelLarge,
                            color = scheme.onPrimaryContainer,
                        )
                    }
                }
            }

            // 3 — TODAY
            SectionRule()
            ForgedSectionHeader(
                label = "TODAY",
                trailing = { StampText(ui.hero.readinessTag, color = heatAt(ui.hero.readinessFreshness)) },
            )
            Column(Modifier.padding(horizontal = Dim.screenPadH)) {
                // Anton is reserved for numbers, the wordmark and the CTA — a session name is
                // prose, not a stat, so it gets a plain bold face instead of headlineLarge.
                Text(
                    text = ui.hero.sessionName,
                    style = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.7).sp),
                    color = scheme.onSurface,
                )
                Text(
                    ui.hero.meta,
                    style = type.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Row(
                    Modifier.padding(top = 18.dp, bottom = Dim.sectionTop),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ForgedCta(label = ui.hero.ctaLabel, onClick = onStart, modifier = Modifier.weight(1f))
                    val swapSource = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .size(Dim.ctaSecondary)
                            .clip(MaterialTheme.shapes.medium)
                            .border(1.dp, scheme.outline, MaterialTheme.shapes.medium)
                            .forgedPress(swapSource)
                            .clickable(
                                interactionSource = swapSource,
                                indication = null,
                                onClick = onOpenPlan,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Rounded.SwapHoriz,
                            contentDescription = "Swap session — open Plan",
                            tint = scheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // 4 — THIS WEEK
            SectionRule()
            ForgedSectionHeader(
                label = "THIS WEEK",
                trailing = {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = "${ui.weekDone}",
                            style = TextStyle(fontFamily = Anton, fontSize = 15.sp),
                            color = scheme.onSurface,
                        )
                        Text(
                            text = " / ${ui.weekGoal}",
                            style = type.bodySmall,
                            color = scheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 1.dp),
                        )
                    }
                },
            )
            ForgedWeekStrip(ui.doneWeekdays)

            // 5 — NEXT UP. Hidden entirely when empty: a heading over blank space read as
            // half-rendered (same rule the old rails followed).
            if (ui.upcoming.isNotEmpty()) {
                SectionRule()
                ForgedSectionHeader("NEXT UP")
                ui.upcoming.forEach { item ->
                    RowRule()
                    ForgedListRow(
                        title = item.name,
                        subtitle = item.muscles,
                        chevron = true,
                        onClick = onOpenPlan,
                        trailing = {
                            Text(item.dayLabel, style = type.bodySmall, color = scheme.onSurfaceVariant)
                        },
                    )
                }
            }

            // 6 — RECENT
            if (ui.recent.isNotEmpty()) {
                SectionRule()
                ForgedSectionHeader(label = "RECENT", linkLabel = "All history", onClickLink = onOpenHistory)
                ui.recent.forEach { s ->
                    RowRule()
                    ForgedListRow(
                        title = s.name,
                        subtitle = s.caption,
                        onClick = { onOpenWorkout(s.id) },
                        trailing = { Text(s.volume, style = type.titleLarge, color = scheme.onSurface) },
                    )
                }
            }

            // Nothing logged yet: say so once, instead of two empty section headers over blank space.
            if (ui.upcoming.isEmpty() && ui.recent.isEmpty()) {
                Text(
                    text = "Nothing logged yet. Finish a session and your recovery, history " +
                        "and stats fill in from there.",
                    style = type.bodyMedium,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = Dim.screenPadH, end = Dim.screenPadH, top = 26.dp,
                    ),
                )
            }

            Spacer(Modifier.height(Dim.listBottomSpacer))
        }
    }
}
