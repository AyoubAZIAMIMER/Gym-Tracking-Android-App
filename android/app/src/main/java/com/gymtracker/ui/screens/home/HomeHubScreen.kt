// Purpose: Home — the "Dynamic Hub" direction, the one you picked. Adaptive hero that reads
//          readiness, then horizontal rails. Stateless: hand it a HomeUi from your existing
//          HomeViewModel; no business logic moves into the UI layer.
// Inputs: HomeUi, callbacks
// Outputs: HomeHubScreen()
//
// STRUCTURE (top to bottom)
//   1. 2 dp week-progress rail pinned to the very top (sessions done / planned)
//   2. brand row: ForgedWordmark + date caption + 32 dp avatar (opens Settings)
//   3. HERO — "RECOMMENDED FOR TODAY" + readiness tag + session name (Anton 30) + Start CTA
//   4. rail "Ready to train"  — heat-tinted muscle-group cards, coolest first
//   5. rail "Jump back in"    — recent sessions, name + day + volume
//   6. PR watch strip         — lifts within one session of a record (gold, sparingly)
//   7. bottom spacer Dim.listBottomSpacer so the last row clears the floating nav
package com.gymtracker.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.ForgeHairline
import com.gymtracker.ui.components.ForgeSectionHeader
import com.gymtracker.ui.components.ForgedWordmark
import com.gymtracker.ui.components.forgeGround
import com.gymtracker.ui.components.forgeHero
import androidx.compose.ui.text.TextStyle
import com.gymtracker.ui.components.emberBloomPulsing
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.ui.theme.LocalForge

data class HomeUi(
    val greeting: String,               // "Hi, Ayoub"
    val dateCaption: String,            // "Fri 17 Jul · the forge is hot."
    val weekProgress: Float,            // 0f..1f — drives the 2 dp top rail
    val hero: HeroUi,
    val readyToTrain: List<MuscleCardUi>,
    val jumpBackIn: List<RecentSessionUi>,
    val prWatch: List<String> = emptyList(),
    val avatarInitial: String = "",
)

data class HeroUi(
    val eyebrow: String = "RECOMMENDED FOR TODAY",
    val readinessTag: String,           // "legs fresh" — wording from readiness, tinted by heat
    val readinessFreshness: Float,      // 0f..1f -> heat.at()
    val sessionName: String,            // "Squat (Barbell)" or the session title
    val meta: String,                   // "Quads · Glutes · last set 100 × 9"
    val repRange: String? = null,       // "6–10 reps"
    /** "Start", or "Resume" while a session is live — the CTA must not lie about what it does. */
    val ctaLabel: String = "Start",
)

data class MuscleCardUi(val muscle: String, val freshness: Float, val caption: String)

data class RecentSessionUi(val name: String, val caption: String, val volume: String)

@Composable
fun HomeHubScreen(
    ui: HomeUi,
    heatAt: (Float) -> Color,
    onStart: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPlan: () -> Unit,
    onOpenHistory: () -> Unit,
    onOpenRecovery: () -> Unit,
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
                    Text(
                        text = ui.avatarInitial,
                        style = type.labelLarge,
                        color = scheme.onPrimaryContainer,
                    )
                }
            }

            // 3 — hero
            Column(
                Modifier
                    .padding(horizontal = Dim.screenPadH)
                    .padding(top = 20.dp)
                    .fillMaxWidth()
                    .forgeHero()
                    .padding(18.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        ui.hero.eyebrow,
                        style = type.labelSmall.copy(fontSize = 11.sp, letterSpacing = 1.6.sp),
                        color = scheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f),
                    )
                    val tagColor = heatAt(ui.hero.readinessFreshness)
                    Text(
                        ui.hero.readinessTag,
                        style = type.bodySmall,
                        color = tagColor,
                        modifier = Modifier
                            .border(1.dp, tagColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                            .padding(horizontal = 9.dp, vertical = 3.dp),
                    )
                }
                Text(
                    ui.hero.sessionName,
                    style = type.headlineLarge.copy(fontSize = 30.sp),
                    color = scheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    ui.hero.meta,
                    style = type.bodySmall,
                    color = scheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                // Start CTA — accent fill, Anton 20, radius AppShapes.medium, glow from palette.
                // Use your existing ForgedButton if you have one; keep Dim.ctaHeight and forgedPress(0.97).
                Row(Modifier.padding(top = 18.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val startSource = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .weight(1f)
                            .height(Dim.ctaHeight)
                            // the glow is the palette's, so it re-tints with the Heat axis
                            .emberBloomPulsing(forge.palette.action, Dim.ctaRadius)
                            .clip(MaterialTheme.shapes.medium)
                            .background(forge.palette.action)
                            .forgedPress(startSource)
                            .clickable(
                                interactionSource = startSource,
                                indication = null,
                                onClick = onStart,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = ui.hero.ctaLabel,
                            style = type.titleLarge,
                            color = forge.palette.onAction,
                        )
                    }
                    val swapSource = remember { MutableInteractionSource() }
                    Box(
                        Modifier
                            .size(Dim.ctaHeight)
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

            // 4 — Ready to train rail
            // the link says Recovery, so it must go to Recovery — it called onOpenPlan before
            RailHeader("Ready to train", "Recovery ›", onOpenRecovery)
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dim.screenPadH),
                horizontalArrangement = Arrangement.spacedBy(Dim.railGap),
            ) {
                items(ui.readyToTrain, key = { it.muscle }) { card ->
                    val tint = heatAt(card.freshness)
                    val source = remember { MutableInteractionSource() }
                    Column(
                        Modifier
                            .width(Dim.railCardWidth)
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.surfaceContainer)
                            // the card is the same destination as the header link, not decoration
                            .forgedPress(source, pressedScale = 0.985f)
                            .clickable(interactionSource = source, indication = null, onClick = onOpenRecovery)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Box(Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(tint.copy(alpha = 0.22f)))
                        Text(card.muscle, style = type.titleMedium, color = scheme.onSurface)
                        Text(card.caption, style = type.bodySmall, color = tint)
                    }
                }
            }

            // 5 — Jump back in rail
            RailHeader("Jump back in", "All history ›", onOpenHistory)
            LazyRow(
                contentPadding = PaddingValues(horizontal = Dim.screenPadH),
                horizontalArrangement = Arrangement.spacedBy(Dim.railGap),
            ) {
                // name+caption is the only stable identity a rail card has (no id in HomeUi)
                items(ui.jumpBackIn, key = { "${it.name}|${it.caption}" }) { s ->
                    val source = remember { MutableInteractionSource() }
                    Column(
                        Modifier
                            .width(Dim.railCardWidth)
                            .clip(RoundedCornerShape(18.dp))
                            .background(scheme.surfaceContainer)
                            .forgedPress(source, pressedScale = 0.985f)
                            .clickable(interactionSource = source, indication = null, onClick = onOpenHistory)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(s.name, style = type.titleMedium, color = scheme.onSurface)
                        Text(s.caption, style = type.bodySmall, color = scheme.onSurfaceVariant)
                        Text(s.volume, style = type.titleLarge, color = scheme.onSurface)
                    }
                }
            }

            // 6 — PR watch (only when non-empty; gold is PRs and nothing else)
            if (ui.prWatch.isNotEmpty()) {
                Column(Modifier.padding(horizontal = Dim.screenPadH)) {
                    ForgeSectionHeader("PR watch")
                    ui.prWatch.forEach { line ->
                        Text(
                            line,
                            style = type.bodyMedium,
                            color = scheme.onSurface,
                            modifier = Modifier.padding(vertical = Dim.rowPadV),
                        )
                        ForgeHairline()
                    }
                }
            }

            Spacer(Modifier.height(Dim.listBottomSpacer))
        }
    }
}

/** Rail headers are Anton 16 with a quiet link — NOT the mono micro label used for flat sections. */
@Composable
private fun RailHeader(title: String, link: String, onLink: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Dim.screenPadH, end = Dim.screenPadH, top = 24.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp, letterSpacing = 0.5.sp),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        // the handoff declares onLink but never attaches it — with History and Library demoted
        // from tabs, these rail links are their only entry point, so they must actually work
        val source = remember { MutableInteractionSource() }
        Text(
            text = link,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .forgedPress(source, pressedScale = 0.96f)
                .clickable(interactionSource = source, indication = null, onClick = onLink)
                .padding(start = 8.dp, top = 6.dp, bottom = 6.dp),
        )
    }
}
