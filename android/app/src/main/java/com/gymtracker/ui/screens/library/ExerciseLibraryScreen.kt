// Purpose: Exercise library — search/filter/browse + create, edit, merge placeholders,
//          per-exercise description, muscles and YouTube form-video link
// Inputs: LibraryViewModel (seeded catalog + user data)
// Outputs: onOpenExercise(id) navigation; CRUD via the ViewModel
package com.gymtracker.ui.screens.library

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MergeType
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.ExerciseMedia
import com.gymtracker.data.ProgressionImporter
import com.gymtracker.ui.components.EditExerciseSheet
import com.gymtracker.ui.components.ExerciseDemo
import com.gymtracker.ui.components.ExercisePickerSheet
import com.gymtracker.ui.components.ExerciseRowSkeleton
import com.gymtracker.ui.components.FlatRow
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.MuscleTargetFigure
import com.gymtracker.ui.screens.session.PickerItem
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedEntrance
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onOpenExercise: (String) -> Unit = {},
    vm: LibraryViewModel = viewModel(),
) {
    val rows by vm.rows.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("All") }
    var selected by remember { mutableStateOf<LibRow?>(null) }
    var editing by remember { mutableStateOf<LibRow?>(null) }
    var creating by remember { mutableStateOf(false) }
    var mergeFor by remember { mutableStateOf<LibRow?>(null) }

    val filterChips = remember(rows) {
        listOf("All") + rows.flatMap { it.muscles.split(" · ") }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .sorted()
    }
    val filtered = rows.filter { row ->
        val matchesQuery = query.isBlank() ||
            row.name.contains(query, ignoreCase = true) ||
            row.muscles.contains(query, ignoreCase = true)
        val matchesFilter = filter == "All" || row.muscles.contains(filter, ignoreCase = true)
        matchesQuery && matchesFilter
    }
    val grouped = filtered.groupBy { it.name.firstOrNull()?.uppercaseChar() ?: '#' }.toSortedMap()
    // §10: rows rise in once per screen entry (first STAGGER_CAP only), never on tab return
    var entered by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { entered = true }
    val staggerIndex = remember(filtered) {
        filtered.mapIndexed { i, r -> (r.id ?: r.name) to i }.toMap()
    }

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { creating = true },
                // sits above the floating bottom nav overlay
                modifier = Modifier.padding(bottom = 76.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Exercise")
            }
        },
    ) { scaffoldPadding ->
        GlowBackground {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Exercises", style = MaterialTheme.typography.headlineLarge)
                Text(
                    text = "${rows.size} exercises · tap one for details, form videos and stats.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search exercises") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                )
                Row(
                    Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    filterChips.forEach { chip ->
                        val active = filter == chip
                        Box(
                            Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (active) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)
                                )
                                .clickable { filter = chip }
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 130.dp),
                ) {
                    // rows is empty only before the first DB emission (an empty library
                    // falls back to starters) — so this uniquely means "still loading"
                    if (rows.isEmpty()) {
                        items(9, key = { "skeleton-$it" }) {
                            ExerciseRowSkeleton()
                        }
                    }
                    grouped.forEach { (letter, exercises) ->
                        item(key = "letter-$letter") {
                            Box(
                                Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = letter.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        // a directory, not a card gallery: flat dense rows, hairline dividers
                        items(exercises, key = { it.id ?: it.name }) { row ->
                            FlatRow(
                                modifier = Modifier.forgedEntrance(
                                    staggerIndex[row.id ?: row.name] ?: Int.MAX_VALUE,
                                    entered,
                                ),
                                onClick = { selected = row },
                                verticalPadding = 9.dp,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(row.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        text = row.muscles.ifBlank { "No muscle assigned yet" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (row.isPlaceholder) {
                                    Pill("IMPORTED", GymTheme.colors.prGold)
                                } else if (row.isCustom) {
                                    Pill("CUSTOM", MaterialTheme.colorScheme.primary)
                                }
                                Icon(
                                    Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = GymTheme.colors.hint,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // --- detail sheet -------------------------------------------------------------
    selected?.let { row ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(row.name, style = MaterialTheme.typography.headlineMedium)
                if (row.muscles.isNotBlank() || row.equipment.isNotBlank()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        row.muscles.split(" · ").filter { it.isNotBlank() }.forEach { muscle ->
                            Pill(muscle, MaterialTheme.colorScheme.primary)
                        }
                        if (row.equipment.isNotBlank()) {
                            Pill(row.equipment, MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
                // two-frame demonstration: start ↔ end position of the movement
                val demoFrames = remember(row.name) { ExerciseMedia.imagesFor(context, row.name) }
                if (demoFrames.isNotEmpty()) {
                    ExerciseDemo(frames = demoFrames)
                }
                // anatomical target figure: this exercise's muscles highlighted hot
                val targetMuscles = row.muscles.split("·", ",")
                    .map { it.trim() }
                    .mapNotNull(ProgressionImporter::canonicalMuscle)
                    .distinct()
                if (targetMuscles.isNotEmpty()) {
                    GlassSurface(shape = MaterialTheme.shapes.large) {
                        MuscleTargetFigure(
                            muscles = targetMuscles,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        )
                    }
                }
                if (row.description.isNotBlank()) {
                    Text(
                        text = row.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (row.isPlaceholder) {
                    Text(
                        text = "Imported from Progression without a name. Merge it into the right " +
                            "exercise to attach all its history.",
                        style = MaterialTheme.typography.bodySmall,
                        color = GymTheme.colors.prGold,
                    )
                }

                OutlinedButton(
                    onClick = {
                        val q = URLEncoder.encode("${row.name} exercise form", "UTF-8")
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://www.youtube.com/results?search_query=$q".toUri(),
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(50),
                ) {
                    Icon(Icons.Rounded.PlayCircle, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Watch form videos on YouTube")
                }

                row.id?.let { id ->
                    Button(
                        onClick = {
                            selected = null
                            onOpenExercise(id)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(50),
                    ) {
                        Icon(Icons.Rounded.BarChart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("View stats")
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = {
                                editing = row
                                selected = null
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(50),
                        ) {
                            Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Edit")
                        }
                        if (row.isPlaceholder) {
                            OutlinedButton(
                                onClick = {
                                    mergeFor = row
                                    selected = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(50),
                            ) {
                                Icon(Icons.Rounded.MergeType, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Merge into…")
                            }
                        }
                    }
                }
            }
        }
    }

    // --- create / edit -------------------------------------------------------------
    if (creating) {
        EditExerciseSheet(
            title = "New exercise",
            onSave = { name, muscles, equipment, description ->
                vm.saveExercise(null, name, muscles, equipment, description)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    editing?.let { row ->
        EditExerciseSheet(
            title = "Edit exercise",
            initialName = row.name,
            initialMuscles = row.muscles,
            initialEquipment = row.equipment,
            initialDescription = row.description,
            canDelete = true,
            onSave = { name, muscles, equipment, description ->
                row.id?.let { vm.saveExercise(it, name, muscles, equipment, description) }
                editing = null
            },
            onDelete = {
                row.id?.let { id -> vm.deleteOrArchive(id) { } }
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    // --- merge picker ----------------------------------------------------------------
    mergeFor?.let { placeholder ->
        ExercisePickerSheet(
            items = rows
                .filter { it.id != null && !it.isPlaceholder && it.id != placeholder.id }
                .map { PickerItem(it.id, it.name, it.muscles) },
            onPick = { target ->
                val from = placeholder.id
                val to = target.dbExerciseId
                if (from != null && to != null) vm.merge(from, to)
                mergeFor = null
            },
            onDismiss = { mergeFor = null },
        )
    }
}

@Composable
private fun Pill(label: String, tint: Color) {
    Box(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(tint.copy(alpha = 0.16f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = tint,
        )
    }
}
