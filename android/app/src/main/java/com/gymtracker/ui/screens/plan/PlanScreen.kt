// Purpose: Plan tab — active program's next day, your programs, prebuilt templates
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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.theme.GymTheme

@Composable
fun PlanScreen(
    onStartDay: (String) -> Unit = {},
    onOpenProgram: (String) -> Unit = {},
    vm: PlanViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { vm.refresh() }

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Plan", style = MaterialTheme.typography.headlineLarge)
            Text(
                text = "Programs structure your week — pick a template or build your own.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            state.next?.let { next ->
                GlassSurface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "UP NEXT · ${next.programName.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = GymTheme.colors.hint,
                        )
                        Text(next.day.name, style = MaterialTheme.typography.titleLarge)
                        Button(
                            onClick = { onStartDay(next.day.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(50),
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Start ${next.day.name}")
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Your programs",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { showCreateDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = null)
                    Text("New")
                }
            }

            if (state.programs.isEmpty()) {
                GlassSurface {
                    Text(
                        text = "No programs yet — add one from the templates below.",
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            state.programs.forEach { program ->
                val isActive = program.id == state.activeProgramId
                GlassSurface(onClick = { onOpenProgram(program.id) }) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(program.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (isActive) "Active — drives your Home plan" else "Tap to edit",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (isActive) GymTheme.colors.success
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (isActive) {
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(GymTheme.colors.success.copy(alpha = 0.16f))
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            ) {
                                Text(
                                    "ACTIVE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = GymTheme.colors.success,
                                )
                            }
                        }
                        Icon(
                            Icons.Rounded.ChevronRight,
                            contentDescription = null,
                            tint = GymTheme.colors.hint,
                        )
                    }
                }
            }

            Text("Templates", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                state.templates.forEach { template ->
                    GlassSurface(modifier = Modifier.width(230.dp)) {
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
                                Text("Add to my programs")
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(88.dp)
            )
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New program") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("e.g. My PPL") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showCreateDialog = false
                    vm.createProgram(name) { id -> onOpenProgram(id) }
                }) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            },
        )
    }
}
