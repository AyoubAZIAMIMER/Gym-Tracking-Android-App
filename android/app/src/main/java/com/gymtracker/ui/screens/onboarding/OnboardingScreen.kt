// Purpose: First-run flow — step 1 profile questions, step 2 pick-your-split program picker.
//          Full screen, not a bottom sheet: onboarding is the one moment a first-time user has
//          nothing else to look at, so it earns the extra space (CLAUDE.md forbids onboarding
//          carousels, not a single deliberate two-step setup).
// Inputs: OnboardingViewModel
// Outputs: onDone() once a program is created and active
package com.gymtracker.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.WorkoutRepository
import com.gymtracker.ui.components.ForgedCta
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SettingRow
import com.gymtracker.ui.components.SettingStepper
import com.gymtracker.ui.components.StampText
import com.gymtracker.ui.components.forgeGround
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedPress

@Composable
fun OnboardingScreen(onDone: () -> Unit, vm: OnboardingViewModel = viewModel()) {
    val state by vm.ui.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize().forgeGround().statusBarsPadding()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dim.screenPadH)
                .padding(top = 8.dp, bottom = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            repeat(2) { i ->
                Box(
                    Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= state.step) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant
                        )
                )
            }
        }

        when (state.step) {
            0 -> ProfileStep(onNext = vm::saveProfileAndAdvance)
            else -> PickerStep(
                state = state,
                onSelect = vm::selectTemplate,
                onBack = vm::back,
                onContinue = { vm.finish(onDone) },
            )
        }
    }
}

@Composable
private fun ProfileStep(onNext: (name: String, weightKg: Double?, heightCm: Int?, weeklyGoal: Int) -> Unit) {
    var name by rememberSaveable { mutableStateOf("") }
    var weight by rememberSaveable { mutableStateOf("") }
    var height by rememberSaveable { mutableStateOf("") }
    var goal by rememberSaveable { mutableStateOf(3) }

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        Column(Modifier.padding(horizontal = Dim.screenPadH)) {
            StampText("STEP 1 OF 2")
            Text(
                text = "Who's training?",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 31.sp),
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "Your name shapes the greeting; body weight and height feed future " +
                    "strength standards; the weekly goal drives the Home ring.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 7.dp),
            )

            Column(
                Modifier.padding(top = 22.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { t ->
                            // Same fix as ProfileSheet.kt's identical field: this filter alone
                            // let multiple '.' through, so a value like "70.5.2" passed the
                            // filter, then failed toDoubleOrNull() silently on save. Caught by
                            // live-testing onboarding after the ProfileSheet fix — this is a
                            // separate, duplicated implementation, not a shared composable.
                            val filtered = t.filter { it.isDigit() || it == '.' }
                            val firstDot = filtered.indexOf('.')
                            weight = if (firstDot == -1) filtered else {
                                filtered.take(firstDot + 1) + filtered.substring(firstDot + 1).filter { it != '.' }
                            }.take(6)
                        },
                        modifier = Modifier.weight(1f),
                        label = { Text("Body weight (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = MaterialTheme.shapes.medium,
                    )
                    OutlinedTextField(
                        value = height,
                        onValueChange = { t -> height = t.filter(Char::isDigit).take(3) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Height (cm)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            }

            RowRule(Modifier.padding(top = 18.dp))
            SettingRow(
                title = "Weekly goal",
                helper = "Sessions per week",
                control = {
                    SettingStepper(
                        value = "$goal",
                        onDelta = { delta -> goal = (goal + delta).coerceIn(1, 7) },
                    )
                },
            )
            RowRule()
        }

        Spacer(Modifier.weight(1f))
        ForgedCta(
            label = "Continue",
            onClick = { onNext(name.trim(), weight.toDoubleOrNull(), height.toIntOrNull(), goal) },
            enabled = name.isNotBlank(),
            modifier = Modifier
                .padding(horizontal = Dim.screenPadH)
                .padding(bottom = 24.dp, top = 24.dp)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun PickerStep(
    state: OnboardingUiState,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit,
    onContinue: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = Dim.screenPadH)) {
            StampText("STEP 2 OF 2")
            Text(
                text = "Pick your split",
                style = MaterialTheme.typography.headlineLarge.copy(fontSize = 31.sp),
                modifier = Modifier.padding(top = 6.dp),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "You can change this any time from Plan without losing history.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 7.dp),
            )
        }

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dim.screenPadH, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.previews.forEachIndexed { i, preview ->
                TemplateCard(
                    preview = preview,
                    selected = i == state.selectedIndex,
                    onClick = { onSelect(i) },
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dim.screenPadH)
                .padding(bottom = 20.dp, top = 4.dp)
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val backSource = remember { MutableInteractionSource() }
            Box(
                Modifier
                    .height(Dim.ctaHeight)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .forgedPress(backSource)
                    .clickable(interactionSource = backSource, indication = null, onClick = onBack)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("Back", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            ForgedCta(
                label = "Continue",
                onClick = onContinue,
                enabled = state.previews.isNotEmpty() && !state.saving,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun TemplateCard(
    preview: WorkoutRepository.TemplatePreview,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val shape = MaterialTheme.shapes.large
    Column(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (selected) {
                    Modifier
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, shape)
                } else Modifier
            )
            .forgedPress(source, pressedScale = 0.985f)
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = preview.template.name,
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 22.sp),
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = "${preview.dayCount}-day cycle · ~${preview.estimatedMinutes} min",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            SelectionMark(selected)
        }
        Text(
            text = preview.template.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            preview.muscleLoad.forEach { load ->
                Column(
                    Modifier.weight(load.percent.coerceAtLeast(1).toFloat()),
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(5.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                    Text(
                        text = load.muscle,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.5.sp),
                        color = GymTheme.colors.hint,
                    )
                }
            }
        }
    }
}

/** 22dp mark: filled + check when selected, a bare outline ring otherwise — one selection
 *  signal per card, shared with the border/tint on [TemplateCard] itself. */
@Composable
private fun SelectionMark(selected: Boolean) {
    Box(
        Modifier
            .size(22.dp)
            .clip(CircleShape)
            .then(
                if (selected) {
                    Modifier.background(MaterialTheme.colorScheme.primary)
                } else {
                    Modifier.border(1.5.dp, MaterialTheme.colorScheme.outline, CircleShape)
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (selected) {
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(13.dp),
                tint = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}
