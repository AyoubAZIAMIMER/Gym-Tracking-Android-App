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
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.data.ExerciseMedia
import com.gymtracker.data.ProgressionImporter
import com.gymtracker.ui.components.EditExerciseSheet
import com.gymtracker.ui.components.ExerciseDemo
import com.gymtracker.ui.components.ExercisePickerSheet
import com.gymtracker.ui.components.ExerciseRowSkeleton
import com.gymtracker.ui.components.ForgedCta
import com.gymtracker.ui.components.ForgedListRow
import com.gymtracker.ui.components.ForgedSectionHeader
import com.gymtracker.ui.components.RowRule
import com.gymtracker.ui.components.SectionRule
import com.gymtracker.ui.components.SheetTitle
import com.gymtracker.ui.components.forgeHero
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.ForgedScreenTitle
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.components.body.MuscleTargetFigure
import com.gymtracker.ui.screens.session.PickerItem
import com.gymtracker.ui.theme.GymTheme
import com.gymtracker.ui.theme.forgedEntrance
import java.net.URLEncoder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLibraryScreen(
    onOpenExercise: (String) -> Unit = {},
    onBack: () -> Unit = {},
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

    // one-line feedback for actions that would otherwise complete silently
    var toast by remember { mutableStateOf<String?>(null) }
    val snackbarHost = remember { SnackbarHostState() }
    LaunchedEffect(toast) {
        toast?.let {
            snackbarHost.showSnackbar(it)
            toast = null
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        snackbarHost = { SnackbarHost(snackbarHost) },
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
        GlowBackground(glowAlpha = 0.10f) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
                    .statusBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // prototype: title left, count as a muted trailing caption — no sentence
                ForgedScreenTitle(
                    title = "Library",
                    trailing = "${rows.size} exercises",
                    onBack = onBack,
                )
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth().padding(horizontal = Dim.screenPadH),
                    placeholder = { Text("Search exercises") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(50),
                )
                Row(
                    Modifier
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = Dim.screenPadH),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // README §9: active filter is an accent underline, not a filled pill
                    filterChips.forEach { chip ->
                        val active = filter == chip
                        Column(
                            Modifier.clickable { filter = chip },
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = chip,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) MaterialTheme.colorScheme.onSurface
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Box(
                                Modifier
                                    .padding(top = 5.dp)
                                    .width(20.dp)
                                    .height(2.dp)
                                    .background(
                                        if (active) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                            )
                        }
                    }
                }
                LazyColumn(
                    // no horizontal contentPadding and no inter-row gap: ForgedListRow carries
                    // Dim.screenPadH itself, and the hairlines must run edge to edge like the
                    // other screens. Bottom is deliberately 130dp, not Dim.listBottomSpacer —
                    // this list sits under a FAB as well as the nav pill
                    // (MEMORY.md: don't "correct" this to 112dp)
                    contentPadding = PaddingValues(bottom = 130.dp),
                ) {
                    // rows is empty only before the first DB emission (an empty library
                    // falls back to starters) — so this uniquely means "still loading"
                    if (rows.isEmpty()) {
                        items(9, key = { "skeleton-$it" }) {
                            ExerciseRowSkeleton(
                                Modifier.padding(horizontal = Dim.screenPadH, vertical = 4.dp)
                            )
                        }
                    }
                    grouped.forEach { (letter, exercises) ->
                        item(key = "letter-$letter") {
                            Box(
                                Modifier
                                    .padding(
                                        start = Dim.screenPadH, top = 18.dp, bottom = 8.dp,
                                    )
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
                            Column(
                                Modifier.forgedEntrance(
                                    staggerIndex[row.id ?: row.name] ?: Int.MAX_VALUE,
                                    entered,
                                )
                            ) {
                                RowRule()
                                ForgedListRow(
                                    title = row.name,
                                    subtitle = row.muscles.ifBlank { "No muscle assigned yet" },
                                    onClick = { selected = row },
                                    verticalPadding = 10.dp,
                                    chevron = true,
                                    trailing = {
                                        if (row.isPlaceholder) {
                                            Pill("IMPORTED", GymTheme.colors.prGold)
                                        } else if (row.isCustom) {
                                            Pill("CUSTOM", MaterialTheme.colorScheme.primary)
                                        }
                                    },
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
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 28.dp),
            ) {
                SheetTitle(row.name, row.equipment.takeIf { it.isNotBlank() })

                if (row.muscles.isNotBlank()) {
                    Row(
                        Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Spacer(Modifier.width(Dim.screenPadH))
                        row.muscles.split(" · ").filter { it.isNotBlank() }.forEach { muscle ->
                            Pill(muscle, MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(Dim.screenPadH))
                    }
                }

                // two-frame demonstration: start ↔ end position of the movement
                val demoFrames = remember(row.name) { ExerciseMedia.imagesFor(context, row.name) }
                if (demoFrames.isNotEmpty()) {
                    Box(Modifier.padding(horizontal = Dim.screenPadH, vertical = 14.dp)) {
                        ExerciseDemo(frames = demoFrames)
                    }
                }

                // anatomical target figure: this exercise's muscles highlighted hot.
                // THE hero of this sheet — hence forgeHero, and nothing else here is a surface.
                val targetMuscles = remember(row.muscles) {
                    row.muscles.split("·", ",")
                        .map { it.trim() }
                        .mapNotNull(ProgressionImporter::canonicalMuscle)
                        .distinct()
                }
                if (targetMuscles.isNotEmpty()) {
                    Box(
                        Modifier
                            .padding(horizontal = Dim.screenPadH)
                            .forgeHero()
                    ) {
                        MuscleTargetFigure(
                            muscles = targetMuscles,
                            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                        )
                    }
                }

                if (row.description.isNotBlank()) {
                    SectionRule(Modifier.padding(top = 18.dp))
                    ForgedSectionHeader("HOW TO", bottomPadding = 4.dp)
                    Text(
                        text = row.description,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(
                            start = Dim.screenPadH, end = Dim.screenPadH, bottom = 4.dp,
                        ),
                    )
                }
                if (row.isPlaceholder) {
                    Text(
                        text = "Imported from Progression without a name. Merge it into the right " +
                            "exercise to attach all its history.",
                        fontSize = 12.5.sp,
                        lineHeight = 18.sp,
                        color = GymTheme.colors.prGold,
                        modifier = Modifier.padding(
                            start = Dim.screenPadH, end = Dim.screenPadH, top = 14.dp,
                        ),
                    )
                }

                Spacer(Modifier.height(18.dp))
                SectionRule()
                ForgedListRow(
                    title = "Watch form videos",
                    subtitle = "Opens a YouTube search",
                    chevron = true,
                    onClick = {
                        val q = URLEncoder.encode("${row.name} exercise form", "UTF-8")
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    "https://www.youtube.com/results?search_query=$q".toUri(),
                                )
                            )
                        }.onFailure {
                            // no browser / YouTube on the device — say so instead of dead-clicking
                            toast = "No app can open video search on this device"
                        }
                    },
                    trailing = {
                        Icon(
                            Icons.Rounded.PlayCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    },
                )
                row.id?.let { id ->
                    RowRule()
                    ForgedListRow(
                        title = "Edit exercise",
                        subtitle = "Name, muscles, equipment, notes",
                        chevron = true,
                        onClick = { editing = row; selected = null },
                        trailing = {
                            Icon(
                                Icons.Rounded.Edit,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = GymTheme.colors.hint,
                            )
                        },
                    )
                    if (row.isPlaceholder) {
                        RowRule()
                        ForgedListRow(
                            title = "Merge into…",
                            subtitle = "Move this placeholder's history onto a real exercise",
                            chevron = true,
                            onClick = { mergeFor = row; selected = null },
                            trailing = {
                                Icon(
                                    Icons.Rounded.MergeType,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = GymTheme.colors.hint,
                                )
                            },
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    ForgedCta(
                        label = "View stats",
                        onClick = { selected = null; onOpenExercise(id) },
                        modifier = Modifier.padding(horizontal = Dim.screenPadH),
                    )
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
                val name = row.name
                row.id?.let { id ->
                    vm.deleteOrArchive(id) { archived ->
                        // archived == it still has logged history, so the rows were kept
                        toast = if (archived) {
                            "$name archived — its logged sets are kept"
                        } else {
                            "$name deleted"
                        }
                    }
                }
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
