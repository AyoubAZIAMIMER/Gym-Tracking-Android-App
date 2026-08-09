// Purpose: Plan tab — rebuilt to the redesign prototype's Plan screen: 28sp title with the
//          active program as a trailing caption, THIS WEEK strip, SESSIONS list (the active
//          program's days), then an "Exercise library" hand-off row. Flat + hairline-ruled,
//          no hero card. Programs/templates keep their existing behaviour, restyled — the
//          prototype mocked fewer features than the app actually ships.
// Inputs: PlanViewModel
// Outputs: onStartDay(dayId), onOpenProgram(programId) navigation events
package com.gymtracker.ui.screens.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.ForgedAlert
import com.gymtracker.ui.components.ForgedBlock
import com.gymtracker.ui.components.ForgedListRow
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.ForgedWeekStrip
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.rememberEntered
import com.gymtracker.ui.components.emberBloom
import com.gymtracker.ui.components.forgeHero
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.StampLabel
import com.gymtracker.ui.theme.forgedPress

@Composable
fun PlanScreen(
    onStartDay: (String) -> Unit = {},
    onOpenProgram: (String) -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    vm: PlanViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val entered = rememberEntered()
    var showCreateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refresh() }

    GlowBackground(glowAlpha = 0.11f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            ForgedScreenTitle("Plan", trailing = state.activeProgramName)

            // THIS WEEK — same strip as Home, so the two tabs agree at a glance
            ForgedBlock(0, entered) {
            SectionRule()
            ForgedSectionHeader(
                label = "THIS WEEK",
                bottomPadding = 12.dp,
                trailing = {
                    Text(
                        text = "${state.workoutsThisWeek} / ${state.weeklyGoal}",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            ForgedWeekStrip(state.doneWeekdays)

            // SESSIONS — the active program's days, each startable
            SectionRule()
            ForgedSectionHeader("SESSIONS")
            if (state.sessions.isEmpty()) {
                EmptyLine("No active program — add one below")
            } else {
                state.sessions.forEach { session ->
                    RowRule()
                    PlanSessionRowView(session, onStartDay = { onStartDay(session.dayId) })
                }
            }

            // the prototype's hand-off row into the library
            SectionRule()
            ForgedListRow(
                title = "Exercise library",
                subtitle = "Browse and swap movements",
                onClick = onOpenLibrary,
                titleSize = 16f,
                verticalPadding = 15.dp,
                chevron = true,
            )

            // programs directory — kept from the shipped app, restyled flat
            SectionRule()
            ForgedSectionHeader(
                label = "YOUR PROGRAMS",
                linkLabel = "New",
                onClickLink = { showCreateDialog = true },
            )
            state.programs.forEach { program ->
                val isActive = program.id == state.activeProgramId
                RowRule()
                ForgedListRow(
                    title = program.name,
                    subtitle = if (isActive) "Active — drives your Home plan" else "Tap to edit",
                    onClick = { onOpenProgram(program.id) },
                    chevron = true,
                ) {
                    if (isActive) {
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(GymTheme.colors.success.copy(alpha = 0.16f))
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Text(
                                text = "ACTIVE",
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                fontWeight = FontWeight.Bold,
                                color = GymTheme.colors.success,
                            )
                        }
                    }
                }
            }
            if (state.programs.isEmpty()) EmptyLine("No programs yet — add one from a template")
            }

            // templates stay a horizontal shelf: they are a picker, not part of the plan
            ForgedBlock(1, entered) {
            SectionRule()
            ForgedSectionHeader("TEMPLATES")
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Dim.screenPadH),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.templates.forEach { template ->
                    Box(
                        Modifier
                            .width(230.dp)
                            .clip(MaterialTheme.shapes.large)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.55f)),
                    ) {
                        Column(
                            Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(template.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${template.days.size} days",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = template.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            TextButton(onClick = { vm.addTemplate(template) }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(Modifier.width(6.dp))
                                Text("Add to my programs")
                            }
                        }
                    }
                }
            }

            }

            Spacer(Modifier.navigationBarsPadding().height(Dim.listBottomSpacer))
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        ForgedAlert(
            title = "New program",
            onDismissRequest = { showCreateDialog = false },
            confirmLabel = "Create",
            confirmEnabled = name.isNotBlank(),
            onConfirm = {
                showCreateDialog = false
                vm.createProgram(name) { id -> onOpenProgram(id) }
            },
            dismissLabel = "Cancel",
            body = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. My PPL") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
            },
        )
    }
}

@Composable
private fun EmptyLine(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = Dim.screenPadH, vertical = 12.dp),
    )
}

/**
 * The design carries session state in the row itself: a mono eyebrow (LOGGED · WED / TODAY /
 * IN 2 DAYS), today filled with `primaryContainer` behind a 3dp accent rail, logged rows dimmed
 * with an olive check, and duration over exercise count on the right.
 */
@Composable
private fun PlanSessionRowView(row: PlanSessionRow, onStartDay: () -> Unit) {
    val today = row.state == SessionState.Today
    val logged = row.state == SessionState.Logged
    val interaction = remember { MutableInteractionSource() }
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (today) Modifier.background(MaterialTheme.colorScheme.primaryContainer) else Modifier)
            .forgedPress(interaction, pressedScale = 0.99f)
            .clickable(interactionSource = interaction, indication = null, onClick = onStartDay)
            .padding(end = Dim.screenPadH, top = 12.dp, bottom = 12.dp)
            .alpha(if (logged) 0.55f else 1f),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // the accent rail is the whole "this is today" affordance — no second badge
        Box(
            Modifier
                .width(3.dp)
                .height(46.dp)
                .background(if (today) MaterialTheme.colorScheme.primary else Color.Transparent)
        )
        Spacer(Modifier.width(Dim.screenPadH - 3.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = row.eyebrow,
                style = StampLabel.copy(fontSize = 9.5.sp),
                color = if (today) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = row.name,
                fontSize = 15.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (row.muscles.isNotBlank()) {
                Text(
                    text = row.muscles,
                    fontSize = 12.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (logged) {
            Icon(
                Icons.Rounded.CheckCircle,
                contentDescription = "Logged",
                tint = GymTheme.colors.success,
                modifier = Modifier.size(22.dp),
            )
        } else {
            Column(horizontalAlignment = Alignment.End) {
                row.estimatedMinutes?.let {
                    Text(
                        text = "$it min",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 17.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "${row.exerciseCount} exercises",
                    fontSize = 11.5.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(6.dp))
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = GymTheme.colors.hint,
            )
        }
    }
}
