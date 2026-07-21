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
    vm: DataViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
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

    GlowBackground {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text("Data", style = MaterialTheme.typography.headlineLarge)
            }
            Text(
                text = "Import your Progression backup; export everything as JSON or per-set CSV.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            GlassSurface {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    CountStat("${state.workouts}", "workouts")
                    CountStat("${state.sets}", "sets")
                    CountStat("${state.exercises}", "exercises")
                }
            }

            ActionCard(
                icon = Icons.Rounded.Person,
                title = "Profile",
                subtitle = "Name, body weight, height, weekly goal",
                enabled = !state.busy,
            ) { showProfile = true }

            ActionCard(
                icon = Icons.Rounded.FileDownload,
                title = "Import Progression backup",
                subtitle = ".pgnbkp / JSON — history + your programs, safe to re-run",
                enabled = !state.busy,
            ) { importLauncher.launch(arrayOf("*/*")) }

            ActionCard(
                icon = Icons.Rounded.Edit,
                title = "Name imported exercises (CSV)",
                subtitle = "Progression CSV export — recovers real names by matching timestamps",
                enabled = !state.busy,
            ) { csvNamesLauncher.launch(arrayOf("*/*")) }

            ActionCard(
                icon = Icons.Rounded.FileUpload,
                title = "Export JSON",
                subtitle = "Full backup of the RepForge database",
                enabled = !state.busy,
            ) { jsonLauncher.launch("repforge-backup.json") }

            ActionCard(
                icon = Icons.Rounded.TableChart,
                title = "Export CSV (per set)",
                subtitle = "completed_at, workout, exercise, weight, reps, tag, e1RM",
                enabled = !state.busy,
            ) { csvLauncher.launch("repforge-sets.csv") }

            val context = LocalContext.current
            val widgetManager = remember { context.getSystemService(AppWidgetManager::class.java) }
            if (widgetManager?.isRequestPinAppWidgetSupported == true) {
                ActionCard(
                    icon = Icons.Rounded.Widgets,
                    title = "Add launcher widget",
                    subtitle = "Streak + days since your last session, on the home screen",
                    enabled = true,
                ) {
                    widgetManager.requestPinAppWidget(
                        ComponentName(context, ForgeWidgetProvider::class.java), null, null,
                    )
                }
            }

            if (state.busy) {
                SteelSheen(Modifier.fillMaxWidth())
            }
            state.message?.let { message ->
                GlassSurface {
                    Text(
                        text = message,
                        modifier = Modifier.padding(14.dp),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
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
