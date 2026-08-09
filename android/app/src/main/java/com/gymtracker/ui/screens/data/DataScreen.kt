// Purpose: Data screen — import a Progression backup, export the database as JSON or per-set CSV
// Inputs: DataViewModel state; Storage Access Framework pickers (no permissions needed)
// Outputs: user-triggered import/export actions
package com.gymtracker.ui.screens.data

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.TableChart
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gymtracker.ui.components.ForgedListRow
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.theme.Anton
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import com.gymtracker.ui.components.ForgedSwitch
import com.gymtracker.ui.components.SegmentedOptions
import com.gymtracker.ui.components.SettingRow
import com.gymtracker.ui.components.SettingStepper
import com.gymtracker.utils.PlateCalculator
import com.gymtracker.utils.TimeFormat
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.Energy
import com.gymtracker.ui.theme.Heat
import com.gymtracker.ui.theme.SurfaceStyle
import com.gymtracker.ui.theme.forgedPress
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.ProfileSheet
import com.gymtracker.ui.components.SteelSheen
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.widget.ForgeWidgetProvider

@Composable
fun DataScreen(
    onBack: () -> Unit = {},
    expression: Triple<Heat, Energy, SurfaceStyle> = Triple(Heat.Chalk, Energy.Alive, SurfaceStyle.Soft),
    onExpressionChange: (Heat, Energy, SurfaceStyle) -> Unit = { _, _, _ -> },
    vm: DataViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val settings = state.settings
    var showProfile by remember { mutableStateOf(false) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(vm::importFrom) }
    val csvNamesLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri -> uri?.let(vm::importCsvNames) }
    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri -> uri?.let(vm::exportJson) }
    val csvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/comma-separated-values")
    ) { uri -> uri?.let(vm::exportCsv) }

    GlowBackground(glowAlpha = 0.09f) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding(),
        ) {
            // back arrow sits inline with the title, as the prototype's Settings header does
            Row(
                Modifier.padding(start = 8.dp, end = Dim.screenPadH, top = 4.dp, bottom = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    text = "Settings",
                    fontSize = 28.sp,
                    letterSpacing = (-0.5).sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            // identity row: 52dp ember-dim disc with an Anton initial
            SectionRule()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clickable { showProfile = true }
                    .padding(horizontal = Dim.screenPadH, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(15.dp),
            ) {
                Box(
                    Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = state.profileName.firstOrNull()?.uppercase() ?: "?",
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 21.sp),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        text = state.profileName.ifBlank { "Add your name" },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = buildString {
                            if (state.streakWeeks > 0) append("${state.streakWeeks}-week streak · ")
                            append("${state.workouts} workouts · ${state.sets} sets")
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
                Icon(
                    Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = GymTheme.colors.hint,
                )
            }

            // TRAINING / REST TIMER / APP, per handoff README §10
            SectionRule()
            ForgedSectionHeader("TRAINING")
            SettingRow(
                title = "Units",
                helper = "Applies to every logged lift and your whole history.",
            ) {
                SegmentedOptions(
                    options = listOf("kg", "lb"),
                    selected = settings.units,
                    onSelect = { vm.saveSettings(settings.copy(units = it)) },
                )
            }
            RowRule()
            SettingRow(
                title = "Weight step",
                helper = "What the – and + buttons change by.",
            ) {
                SegmentedOptions(
                    options = listOf("1.25", "2.5", "5"),
                    selected = PlateCalculator.fmt(settings.weightStepKg),
                    onSelect = {
                        vm.saveSettings(settings.copy(weightStepKg = it.toDouble()))
                    },
                )
            }

            SectionRule()
            ForgedSectionHeader("REST TIMER")
            SettingRow(title = "Default rest") {
                SettingStepper(
                    value = TimeFormat.clock(settings.restSeconds * 1000L),
                    onDelta = { step ->
                        vm.saveSettings(settings.copy(restSeconds = settings.restSeconds + step * 15))
                    },
                )
            }
            RowRule()
            SettingRow(
                title = "Start on logged set",
                helper = "Timer runs the moment you complete a set.",
            ) {
                ForgedSwitch(
                    checked = settings.startRestOnLog,
                    onChange = { vm.saveSettings(settings.copy(startRestOnLog = it)) },
                )
            }
            RowRule()
            SettingRow(
                title = "Alert when rest ends",
                helper = "Vibrate and sound, even with the screen off.",
            ) {
                ForgedSwitch(
                    checked = settings.alertOnRestEnd,
                    onChange = { vm.saveSettings(settings.copy(alertOnRestEnd = it)) },
                )
            }

            SectionRule()
            ForgedSectionHeader("APP")
            SettingRow(title = "Theme") {
                SegmentedOptions(
                    options = listOf("Dark", "Light", "Auto"),
                    selected = settings.theme,
                    onSelect = { vm.saveSettings(settings.copy(theme = it)) },
                )
            }
            RowRule()
            SettingRow(title = "Haptics", helper = "Ticks on steppers, a buzz on rest end.") {
                ForgedSwitch(
                    checked = settings.haptics,
                    onChange = { vm.saveSettings(settings.copy(haptics = it)) },
                )
            }
            // the three expression axes (ForgeExpression) sit under APP, below Theme
            val (heat, energy, surface) = expression
            RowRule()
            SettingRow(title = "Action colour", helper = "Chrome only — the heat scale on Body is data.") {
                SegmentedOptions(
                    options = Heat.entries.map { it.name },
                    selected = heat.name,
                    onSelect = { onExpressionChange(Heat.valueOf(it), energy, surface) },
                )
            }
            RowRule()
            SettingRow(title = "Energy", helper = "How much the interface moves.") {
                SegmentedOptions(
                    options = Energy.entries.map { it.name },
                    selected = energy.name,
                    onSelect = { onExpressionChange(heat, Energy.valueOf(it), surface) },
                )
            }
            RowRule()
            SettingRow(title = "Surface", helper = "How solid cards feel.") {
                SegmentedOptions(
                    options = SurfaceStyle.entries.map { it.name },
                    selected = surface.name,
                    onSelect = { onExpressionChange(heat, energy, SurfaceStyle.valueOf(it)) },
                )
            }

            SectionRule()
            ForgedSectionHeader("DATA")
            RowRule()
            ForgedListRow(
                title = "Import Progression backup",
                subtitle = ".pgnbkp / JSON — history + programs, safe to re-run",
                onClick = { if (!state.busy) importLauncher.launch(arrayOf("*/*")) },
                chevron = true,
            )
            RowRule()
            ForgedListRow(
                title = "Name imported exercises (CSV)",
                subtitle = "Recovers real names by matching timestamps",
                onClick = { if (!state.busy) csvNamesLauncher.launch(arrayOf("*/*")) },
                chevron = true,
            )
            RowRule()
            ForgedListRow(
                title = "Export JSON",
                subtitle = "Full backup of the Forged database",
                onClick = { if (!state.busy) jsonLauncher.launch("repforge-backup.json") },
                chevron = true,
            )
            RowRule()
            ForgedListRow(
                title = "Export CSV (per set)",
                subtitle = "completed_at, workout, exercise, weight, reps, tag, e1RM",
                onClick = { if (!state.busy) csvLauncher.launch("repforge-sets.csv") },
                chevron = true,
            )

            val context = LocalContext.current
            val widgetManager = remember { context.getSystemService(AppWidgetManager::class.java) }
            if (widgetManager?.isRequestPinAppWidgetSupported == true) {
                SectionRule()
                ForgedSectionHeader("EXTRAS")
                RowRule()
                ForgedListRow(
                    title = "Add launcher widget",
                    subtitle = "Streak + days since your last session, on the home screen",
                    onClick = {
                        widgetManager.requestPinAppWidget(
                            ComponentName(context, ForgeWidgetProvider::class.java), null, null,
                        )
                    },
                    chevron = true,
                )
            }

            if (state.busy) {
                SteelSheen(Modifier.fillMaxWidth())
            }
            SectionRule()
            ForgedSectionHeader("ABOUT")
            RowRule()
            SettingRow(title = "Version") {
                Text(
                    text = "1.4.2",
                    style = MaterialTheme.typography.titleLarge.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.message?.let { message ->
                Text(
                    text = message,
                    modifier = Modifier.padding(horizontal = Dim.screenPadH, vertical = 14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Text(
                text = "Progression's built-in exercises are referenced by number only in the backup, " +
                    "so they arrive as \"Exercise #N\" placeholders. Renaming them lands with " +
                    "custom-exercise editing.",
                style = MaterialTheme.typography.labelMedium,
                color = GymTheme.colors.hint,
            )

            Spacer(
                Modifier
                    .navigationBarsPadding()
                    .height(88.dp)
            )
        }
    }

    if (showProfile) {
        val p = vm.profile()
        ProfileSheet(
            initialName = p.name,
            initialWeightKg = p.bodyWeightKg,
            initialHeightCm = p.heightCm,
            initialWeeklyGoal = p.weeklyGoal,
            onSave = { name, weightKg, heightCm, weeklyGoal ->
                vm.saveProfile(name, weightKg, heightCm, weeklyGoal)
                showProfile = false
            },
            onDismiss = { showProfile = false },
        )
    }
}

@Composable
private fun CountStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge.copy(fontFeatureSettings = FONT_FEATURE_TABULAR),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    GlassSurface(onClick = if (enabled) onClick else ({})) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    icon, contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Icon(
                Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = GymTheme.colors.hint,
            )
        }
    }
}
