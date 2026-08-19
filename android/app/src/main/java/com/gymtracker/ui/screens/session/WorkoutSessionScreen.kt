// Purpose: Core workout logging screen — liquid-glass restyle (sets table, drag-adjust, tags,
//          supersets, reorder, rest bubble, inline plate calc, warm-up ramp, stopwatch, finish flow)
// Inputs: WorkoutSessionViewModel state + RestTimerService state
// Outputs: user edits routed to the ViewModel; onFinished() once the workout is saved
package com.gymtracker.ui.screens.session

import android.Manifest
import android.os.Build
import android.os.SystemClock
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DragIndicator
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.StickyNote2
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.TrendingDown
import androidx.compose.material.icons.rounded.TrendingFlat
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.gymtracker.domain.Progression
import com.gymtracker.service.RestTimerService
import com.gymtracker.ui.components.AlertBody
import com.gymtracker.ui.components.ForgedAlert
import com.gymtracker.ui.components.DragNumberField
import com.gymtracker.ui.components.ExercisePickerSheet
import com.gymtracker.ui.components.GlassSurface
import com.gymtracker.ui.components.GlowBackground
import com.gymtracker.ui.components.PlateCalculatorPanel
import com.gymtracker.ui.components.RestActionsSheet
import com.gymtracker.ui.components.rememberOvertimePulse
import com.gymtracker.ui.theme.Dim
import com.gymtracker.ui.theme.FONT_FEATURE_TABULAR
import com.gymtracker.ui.theme.GymTheme
import androidx.compose.ui.text.TextStyle
import com.gymtracker.ui.theme.Anton
import com.gymtracker.ui.theme.Motion
import com.gymtracker.ui.components.ConfettiBurst
import com.gymtracker.ui.components.PrBanner
import com.gymtracker.utils.OneRM
import com.gymtracker.utils.PlateCalculator
import com.gymtracker.utils.TimeFormat
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Shared column widths so the header row and set rows stay aligned
private val IndexColWidth = 22.dp
private val PrevColWidth = 68.dp
private val TagColWidth = 34.dp
private val RpeColWidth = 30.dp
private val CheckColWidth = 34.dp

@Composable
fun WorkoutSessionScreen(
    onFinished: () -> Unit,
    /** Leave the session on screen but go back to the app. The workout keeps running. */
    onMinimise: () -> Unit = {},
    vm: WorkoutSessionViewModel = viewModel(),
) {
    val state by vm.ui.collectAsStateWithLifecycle()
    val restState by RestTimerService.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Android 13+ runtime permission for the rest-timer countdown notification
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) { vm.markSessionActive() }

    LaunchedEffect(state.finished) {
        if (state.finished) {
            onFinished()
            vm.consumeFinished()
        }
    }

    // ticking elapsed clock in the top bar
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(1_000)
            nowMillis = System.currentTimeMillis()
        }
    }

    // plate calculator follows whichever weight field currently has focus
    var focusedWeightSetId by remember { mutableStateOf<Long?>(null) }

    // Strike Mode (UX v6): big scrub-to-adjust numbers for one set. The table (the Slate) is
    // the default surface now — Strike is reached by tapping the active row, and left again by
    // swiping up or tapping past the last set.
    var strikeMode by rememberSaveable { mutableStateOf(false) }

    // Rest + stopwatch, together: tapping the live rest readout (Strike caption / Slate pill)
    // opens this rather than acting directly, so "finish rest" and "start the stopwatch" are
    // both one tap away without either one risking an accidental trigger.
    var restActionsSheetOpen by rememberSaveable { mutableStateOf(false) }

    // Hoisted here (not owned by StopwatchRow) so both the top-bar row and the rest-actions
    // sheet read and drive the same running stopwatch instead of two independent ones.
    var stopwatchAccumulatedMs by rememberSaveable { mutableLongStateOf(0L) }
    var stopwatchRunning by rememberSaveable { mutableStateOf(false) }
    var stopwatchStartedAt by rememberSaveable { mutableLongStateOf(0L) }
    var stopwatchNow by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(stopwatchRunning) {
        while (stopwatchRunning) {
            stopwatchNow = SystemClock.elapsedRealtime()
            delay(200)
        }
    }
    val stopwatchShownMs = stopwatchAccumulatedMs +
        if (stopwatchRunning) (stopwatchNow - stopwatchStartedAt).coerceAtLeast(0) else 0
    val onToggleStopwatchRun = {
        if (stopwatchRunning) {
            stopwatchAccumulatedMs += SystemClock.elapsedRealtime() - stopwatchStartedAt
            stopwatchRunning = false
        } else {
            stopwatchStartedAt = SystemClock.elapsedRealtime()
            stopwatchNow = stopwatchStartedAt
            stopwatchRunning = true
        }
    }
    val onResetStopwatch = {
        stopwatchAccumulatedMs = 0L
        stopwatchStartedAt = SystemClock.elapsedRealtime()
        stopwatchNow = stopwatchStartedAt
    }
    val activeStrike = state.activeSetId?.let { activeId ->
        state.exercises.firstNotNullOfOrNull { ex ->
            val idx = ex.sets.indexOfFirst { it.id == activeId }
            if (idx >= 0) ActiveStrike(ex, ex.sets[idx], idx) else null
        }
    } ?: state.exercises.firstNotNullOfOrNull { ex ->
        // un-completing a set leaves activeSetId null — fall back to the first
        // incomplete set so Strike Mode stays reachable
        val idx = ex.sets.indexOfFirst { !it.completed }
        if (idx >= 0) ActiveStrike(ex, ex.sets[idx], idx) else null
    }
    LaunchedEffect(activeStrike == null) {
        // last strike lands → surface the table so Finish is in reach
        if (activeStrike == null) strikeMode = false
    }
    // ...and offer the finish sheet, once. Reaching the end of the plan is the moment the app
    // exists for; it should not be something you have to go hunting for. Dismissing it leaves
    // you on the table (with Finish in the top bar) so you can still add or edit sets.
    var finishOffered by rememberSaveable { mutableStateOf(false) }
    val strikeShowing = strikeMode && activeStrike != null
    // The Slate shows one exercise; default to whichever holds the active set.
    var slateExerciseId by rememberSaveable { mutableStateOf<Long?>(null) }
    val slateExercise = state.exercises.firstOrNull { it.id == slateExerciseId }
        ?: state.exercises.firstOrNull { ex -> ex.sets.any { it.id == state.activeSetId } }
        ?: state.exercises.firstOrNull()
    // the set the logging bar edits: the active one, else the first not yet logged
    val slateDraftSet = slateExercise?.sets?.firstOrNull { it.id == state.activeSetId }
        ?: slateExercise?.sets?.firstOrNull { !it.completed }
        ?: slateExercise?.sets?.lastOrNull()
    // Every set logged: the plan is over. slateExercise falls back to the first exercise, so
    // without this the Slate stayed up with a "Complete set" CTA pointed at an already-logged
    // set — and SessionTopBar is hidden while the Slate shows, so there was no Finish and no way
    // out. Reported 2026-08-17 as "it never ends and it stucks there".
    val allSetsLogged = state.exercises.isNotEmpty() &&
        state.exercises.all { ex -> ex.sets.all { it.completed } }
    val slateShowing = !strikeShowing && slateExercise != null && !allSetsLogged

    LaunchedEffect(allSetsLogged) {
        if (allSetsLogged && !finishOffered && state.completedSets > 0) {
            finishOffered = true
            vm.showFinishSheet(true)
        }
    }

    // Without this, system back popped the whole NavHost and dumped you on Home with a session
    // still live and no explanation. Now it mirrors what is on screen: Strike steps back to the
    // table (its home), and from the table it minimises — same as the header control.
    BackHandler {
        if (strikeShowing) strikeMode = false else onMinimise()
    }
    // the Slate's note + overflow affordances (the legacy table keeps its own inside ExerciseCard)
    var slateNoteDialog by rememberSaveable { mutableStateOf(false) }
    var slateMenu by rememberSaveable { mutableStateOf(false) }

    // drag-to-reorder state; heights tracked per exercise so swaps stay under the finger
    var draggedId by remember { mutableStateOf<Long?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    val itemHeights = remember { mutableStateMapOf<Long, Int>() }
    val spacingPx = with(LocalDensity.current) { 12.dp.toPx() }

    // liquid glass: content is the blur source; top bar / FAB / bubble float over it
    val hazeState = remember { HazeState() }
    var topBarHeightPx by remember { mutableIntStateOf(0) }
    var showStopwatch by remember { mutableStateOf(false) }

    // Forged Motion §11: the forge burns hotter while you're at the anvil
    var forgeLit by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { forgeLit = true }
    val emberHeat by animateFloatAsState(
        targetValue = if (forgeLit) 1.25f else 1f,
        animationSpec = Motion.settle(Motion.DELIBERATE),
        label = "forgeHeat",
    )

    // Law 7 — heat handoff: when an exercise's last set completes, the next one warms
    // 80 ms later and cools again; the eye is carried, not pointed
    var handoffId by remember { mutableStateOf<Long?>(null) }
    val completion = state.exercises.map { ex ->
        ex.id to (ex.sets.isNotEmpty() && ex.sets.all { it.completed })
    }
    var prevCompletion by remember { mutableStateOf(completion.toMap()) }
    LaunchedEffect(completion) {
        val map = completion.toMap()
        val newlyDone = completion.firstOrNull { (id, done) ->
            done && prevCompletion[id] == false
        }?.first
        prevCompletion = map
        if (newlyDone != null) {
            val list = vm.ui.value.exercises
            val next = list.getOrNull(list.indexOfFirst { it.id == newlyDone } + 1)
            if (next != null && map[next.id] == false) {
                delay(Motion.HANDOFF.toLong())
                handoffId = next.id
                delay(400)
                handoffId = null
            }
        }
    }

    // Recoil (§9): finishing with zero completed sets is a strike on cold metal —
    // but one-gesture entry means accidental sessions exist, so recoil now also
    // offers the way out (discard, nothing saved)
    var recoilTick by remember { mutableIntStateOf(0) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    val topPadding = if (topBarHeightPx > 0) {
        with(LocalDensity.current) { topBarHeightPx.toDp() } + 8.dp
    } else {
        120.dp
    }

    // PR reveal: the frame a set logs a new all-time best, a gold trophy banner drops in
    // and confetti falls for ~2 s, with a firm reward buzz. Count-up rides the e1RM label.
    val prCount = state.exercises.sumOf { ex -> ex.sets.count { it.completed && it.isPr } }
    var prevPrCount by remember { mutableIntStateOf(prCount) }
    var prVisible by remember { mutableStateOf(false) }
    var prLabel by remember { mutableStateOf("") }
    val prHaptic = LocalHapticFeedback.current
    LaunchedEffect(prCount) {
        if (prCount > prevPrCount) {
            val best = state.exercises.flatMap { it.sets }
                .filter { it.completed && it.isPr }
                .mapNotNull { s ->
                    val w = s.effectiveWeightKg
                    val r = s.effectiveReps
                    if (w != null && r != null) OneRM.estimate(w, r) else null
                }
                .maxOrNull()
            prLabel = best?.let { "New PR · ${PlateCalculator.fmt(it)} kg" } ?: "New PR"
            prVisible = true
            prHaptic.performHapticFeedback(HapticFeedbackType.LongPress)
            delay(2_200)
            prVisible = false
        }
        prevPrCount = prCount
    }

    GlowBackground(emberHeat = emberHeat) {
        Box(
            Modifier
                .fillMaxSize()
                .imePadding()
        ) {
            // PR celebration floats above everything (confetti + trophy banner)
            if (prVisible) {
                ConfettiBurst(run = true, modifier = Modifier.matchParentSize().zIndex(4f))
            }
            Box(
                Modifier
                    .align(Alignment.TopCenter)
                    .zIndex(5f)
                    .padding(top = topPadding + 8.dp),
            ) {
                PrBanner(visible = prVisible, label = prLabel)
            }
            if (strikeShowing && activeStrike != null) {
                StrikeModePanel(
                    active = activeStrike,
                    barKg = state.barKg,
                    topPadding = topPadding,
                    restSeconds = restState?.remainingSec,
                    onOpenRestActions = { restActionsSheetOpen = true },
                    onScrubWeight = vm::dragWeight,
                    onScrubReps = vm::dragReps,
                    onSetRpe = vm::setRpe,
                    onStrike = vm::toggleCompleted,
                    onOpenTable = { strikeMode = false },
                    modifier = Modifier
                        .fillMaxSize()
                        .hazeSource(hazeState),
                )
            } else if (slateExercise != null) {
                // BUILD_ORDER step 7 — "The Slate" replaces the old multi-exercise table as the
                // non-Strike surface. Exercise paging (the chevrons) preserves the navigation the
                // scrolling table used to give.
                val heat = GymTheme.colors.heat
                val slateIndex = state.exercises.indexOfFirst { it.id == slateExercise.id }
                val activeSetId = state.activeSetId
                SessionSlateScreen(
                    ui = SessionUi(
                        elapsed = TimeFormat.clock(nowMillis - state.startedAtMillis),
                        exerciseProgress = if (state.totalSets > 0) {
                            state.completedSets / state.totalSets.toFloat()
                        } else 0f,
                        exerciseIndexLabel = "${slateIndex + 1}",
                        exerciseName = slateExercise.name,
                        muscleCaption = slateExercise.muscleGroup,
                        setsLabel = "${slateExercise.sets.count { it.completed }} / ${slateExercise.sets.size}",
                        sets = slateExercise.sets.mapIndexed { i, set ->
                            SetRowUi(
                                index = i + 1,
                                status = when {
                                    set.completed -> SetStatus.Done
                                    set.id == activeSetId -> SetStatus.Active
                                    else -> SetStatus.Todo
                                },
                                weightKg = set.effectiveWeightKg,
                                reps = set.effectiveReps,
                                intensity = set.intensity,
                                isPr = set.isPr,
                            )
                        },
                        restSeconds = restState?.remainingSec,
                        draftWeight = slateDraftSet?.effectiveWeightKg ?: 0.0,
                        draftReps = slateDraftSet?.effectiveReps ?: 0,
                        effort = slateDraftSet?.rpe?.let { it - 5 },
                    ),
                    heatAt = { heat.at(it) },
                    // The table is the base now — its chevron leaves the session (it keeps
                    // running); tapping the active row is how you get to Strike from here.
                    onBack = onMinimise,
                    onOpenActiveSet = { strikeMode = true },
                    onOpenRestActions = { restActionsSheetOpen = true },
                    onCompleteSet = {
                        slateDraftSet?.let { vm.toggleCompleted(slateExercise.id, it.id) }
                    },
                    onWeightDelta = { steps ->
                        slateDraftSet?.let { vm.dragWeight(slateExercise.id, it.id, steps) }
                    },
                    onSetWeight = { text ->
                        slateDraftSet?.let { vm.setWeightText(slateExercise.id, it.id, text) }
                    },
                    onSetReps = { text ->
                        slateDraftSet?.let { vm.setRepsText(slateExercise.id, it.id, text) }
                    },
                    onRepsDelta = { delta ->
                        slateDraftSet?.let { vm.dragReps(slateExercise.id, it.id, delta) }
                    },
                    onEffort = { step ->
                        slateDraftSet?.let {
                            // EffortBars hands back 1..5 (or 0 to clear); the model stores RPE 6..10
                            vm.setRpe(slateExercise.id, it.id, if (step <= 0) null else step + 5)
                        }
                    },
                    onEditNote = { slateNoteDialog = true },
                    onExerciseMenu = { slateMenu = true },
                    onPrevExercise = if (slateIndex > 0) {
                        { slateExerciseId = state.exercises[slateIndex - 1].id }
                    } else null,
                    onNextExercise = if (slateIndex < state.exercises.lastIndex) {
                        { slateExerciseId = state.exercises[slateIndex + 1].id }
                    } else null,
                    modifier = Modifier.fillMaxSize().hazeSource(hazeState),
                )
            } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .hazeSource(hazeState),
                contentPadding = PaddingValues(
                    start = 16.dp, end = 16.dp, top = topPadding, bottom = 140.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(state.exercises, key = { _, ex -> ex.id }) { index, exercise ->
                    ExerciseCard(
                        exercise = exercise,
                        isLast = index == state.exercises.lastIndex,
                        handoff = exercise.id == handoffId,
                        activeSetId = state.activeSetId,
                        focusedWeightSetId = focusedWeightSetId,
                        barKg = state.barKg,
                        dragHandle = Modifier.pointerInput(exercise.id) {
                            detectDragGestures(
                                onDragStart = {
                                    draggedId = exercise.id
                                    dragOffsetY = 0f
                                },
                                onDragEnd = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                },
                                onDragCancel = {
                                    draggedId = null
                                    dragOffsetY = 0f
                                },
                            ) { change, dragAmount ->
                                change.consume()
                                dragOffsetY += dragAmount.y
                                // read the freshest list — composition captures go stale mid-drag
                                val list = vm.ui.value.exercises
                                val current = list.indexOfFirst { it.id == exercise.id }
                                if (current == -1) return@detectDragGestures
                                if (dragOffsetY > 0 && current < list.lastIndex) {
                                    val nextH = (itemHeights[list[current + 1].id] ?: 0).toFloat()
                                    if (nextH > 0 && dragOffsetY > (nextH + spacingPx) * 0.55f) {
                                        vm.moveExercise(current, current + 1)
                                        dragOffsetY -= nextH + spacingPx
                                    }
                                } else if (dragOffsetY < 0 && current > 0) {
                                    val prevH = (itemHeights[list[current - 1].id] ?: 0).toFloat()
                                    if (prevH > 0 && -dragOffsetY > (prevH + spacingPx) * 0.55f) {
                                        vm.moveExercise(current, current - 1)
                                        dragOffsetY += prevH + spacingPx
                                    }
                                }
                            }
                        },
                        onWeightChange = { setId, text -> vm.setWeightText(exercise.id, setId, text) },
                        onRepsChange = { setId, text -> vm.setRepsText(exercise.id, setId, text) },
                        onWeightDrag = { setId, steps -> vm.dragWeight(exercise.id, setId, steps) },
                        onRepsDrag = { setId, steps -> vm.dragReps(exercise.id, setId, steps) },
                        onCycleTag = { setId -> vm.cycleTag(exercise.id, setId) },
                        onCycleRpe = { setId -> vm.cycleRpe(exercise.id, setId) },
                        onToggleComplete = { setId -> vm.toggleCompleted(exercise.id, setId) },
                        onWeightFocus = { setId, focused ->
                            focusedWeightSetId = when {
                                focused -> setId
                                focusedWeightSetId == setId -> null
                                else -> focusedWeightSetId
                            }
                        },
                        onAddSet = { vm.addSet(exercise.id) },
                        onGenerateWarmup = { vm.generateWarmupSets(exercise.id) },
                        onToggleSuperset = { vm.toggleSupersetWithNext(exercise.id) },
                        onRemove = { vm.removeExercise(exercise.id) },
                        onSaveNote = { vm.setExerciseNote(exercise.id, it) },
                        modifier = Modifier
                            .onSizeChanged { itemHeights[exercise.id] = it.height }
                            .zIndex(if (draggedId == exercise.id) 1f else 0f)
                            .graphicsLayer {
                                translationY = if (draggedId == exercise.id) dragOffsetY else 0f
                            },
                    )
                }
            }
            }

            if (!slateShowing) SessionTopBar(
                name = state.workoutName,
                elapsedMillis = nowMillis - state.startedAtMillis,
                restSeconds = restState?.remainingSec,
                showStopwatch = showStopwatch,
                onToggleStopwatch = { showStopwatch = !showStopwatch },
                stopwatchShownMs = stopwatchShownMs,
                stopwatchRunning = stopwatchRunning,
                onToggleStopwatchRun = onToggleStopwatchRun,
                onResetStopwatch = onResetStopwatch,
                onFinishClick = {
                    if (state.completedSets == 0) {
                        recoilTick++
                        showDiscardDialog = true
                    } else {
                        vm.showFinishSheet(true)
                    }
                },
                onMinimise = onMinimise,
                recoilTick = recoilTick,
                hazeState = hazeState,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .onSizeChanged { topBarHeightPx = it.height },
            )

            // No floating bubble while the app is open — replaced by the top-bar chip
            // (SessionTopBar) and the live readout on the Strike/Slate surface below.
            // RestBubbleOverlay still floats one once you leave the app; see AppForeground.

            // legacy-table chrome only — the Slate carries its own equivalents
            if (!strikeShowing && !slateShowing) {
                if (activeStrike != null) {
                    GlassSurface(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .navigationBarsPadding()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(50),
                        hazeState = hazeState,
                        onClick = { strikeMode = true },
                    ) {
                        Text(
                            text = "FOCUS MODE",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                GlassSurface(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(16.dp),
                    shape = RoundedCornerShape(50),
                    hazeState = hazeState,
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                    blurTint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                    onClick = { vm.showExercisePicker(true) },
                ) {
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Exercise", style = MaterialTheme.typography.labelLarge, color = Color.White)
                    }
                }
            }
        }
    }

    if (slateNoteDialog && slateExercise != null) {
        var text by remember(slateExercise.note) { mutableStateOf(slateExercise.note) }
        ForgedAlert(
            title = "Machine note",
            onDismissRequest = { slateNoteDialog = false },
            confirmLabel = "Save",
            onConfirm = {
                vm.setExerciseNote(slateExercise.id, text)
                slateNoteDialog = false
            },
            dismissLabel = "Cancel",
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlertBody(
                        "Seat height, pin, grip width — pinned to ${slateExercise.name} every session."
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(120) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        placeholder = { Text("Seat 4 · pin 12 · narrow grip") },
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            },
        )
    }
    if (slateMenu) {
        ForgedAlert(
            title = "Exercise",
            onDismissRequest = { slateMenu = false },
            confirmLabel = "Add exercise",
            onConfirm = {
                slateMenu = false
                vm.showExercisePicker(true)
            },
            dismissLabel = "Edit note",
            onDismissAction = {
                slateMenu = false
                slateNoteDialog = true
            },
            body = { AlertBody("Add another exercise to this session, or edit this one's note.") },
        )
    }
    if (state.showExercisePicker) {
        ExercisePickerSheet(
            items = state.pickerItems,
            onPick = vm::addExercise,
            onDismiss = { vm.showExercisePicker(false) },
        )
    }
    if (showDiscardDialog) {
        ForgedAlert(
            title = "Nothing logged",
            onDismissRequest = { showDiscardDialog = false },
            confirmLabel = "Discard",
            onConfirm = {
                showDiscardDialog = false
                vm.discardSession()
            },
            dismissLabel = "Keep going",
            destructive = true,
            body = { AlertBody("Discard this session? Nothing will be saved.") },
        )
    }
    if (state.showFinishSheet) {
        FinishSummarySheet(
            state = state,
            elapsedMillis = nowMillis - state.startedAtMillis,
            onSave = vm::finishWorkout,
            onDismiss = { vm.showFinishSheet(false) },
        )
    }
    if (restActionsSheetOpen) {
        RestActionsSheet(
            restRemainingSec = restState?.remainingSec,
            restTotalSec = restState?.totalSec ?: 1,
            onFinishRest = { RestTimerService.stop(context) },
            stopwatchElapsedMs = stopwatchShownMs,
            stopwatchRunning = stopwatchRunning,
            onToggleStopwatch = onToggleStopwatchRun,
            onResetStopwatch = onResetStopwatch,
            onDismiss = { restActionsSheetOpen = false },
        )
    }
}

@Composable
private fun SessionTopBar(
    name: String,
    elapsedMillis: Long,
    restSeconds: Int? = null,        // null = not resting; negative = over the time you set
    showStopwatch: Boolean,
    onToggleStopwatch: () -> Unit,
    stopwatchShownMs: Long = 0L,
    stopwatchRunning: Boolean = false,
    onToggleStopwatchRun: () -> Unit = {},
    onResetStopwatch: () -> Unit = {},
    onFinishClick: () -> Unit,
    onMinimise: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
    recoilTick: Int = 0,
) {
    // the Recoil: 2 px, one hard stop, red tint cooling — never a triple shake
    val recoilX = remember { Animatable(0f) }
    val errTint = remember { Animatable(0f) }
    val haptics = LocalHapticFeedback.current
    val errorColor = MaterialTheme.colorScheme.error
    LaunchedEffect(recoilTick) {
        if (recoilTick > 0) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            launch {
                errTint.snapTo(1f)
                errTint.animateTo(0f, tween(400, easing = Motion.Settle))
            }
            recoilX.animateTo(-2f, tween(40, easing = Motion.StrikeIn))
            recoilX.animateTo(0f, tween(50, easing = Motion.StrikeIn))
        }
    }
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp),
        hazeState = hazeState,
    ) {
        Column(
            Modifier
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Escape route. The session is a full-screen flow, so it must offer a way out
                // that is not "Finish" (Apple HIG §escape-routes; found in QA 2026-08-09 —
                // Strike Mode is the default surface and had no exit control at all).
                IconButton(onClick = onMinimise, modifier = Modifier.size(38.dp)) {
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = "Leave session — it keeps running",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(4.dp))
                // the prototype leads the session header with the clock in Anton — the
                // number you glance at mid-set — and demotes the workout name beneath it
                Column(Modifier.weight(1f)) {
                    Text(
                        text = TimeFormat.clock(elapsedMillis),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontFeatureSettings = FONT_FEATURE_TABULAR,
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = name,
                        fontSize = 12.5.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (restSeconds != null) {
                    // Opposite side of the bar from the session clock, on purpose — the two
                    // numbers read as different things. Glance-only here — the same value with
                    // full finish/stopwatch controls is one line down in the Strike caption, and
                    // duplicating a tap target this small next to it would just invite mis-taps.
                    val overtime = restSeconds < 0
                    val warning = restSeconds in 1..5
                    val pulse = if (overtime || warning) rememberOvertimePulse() else 1f
                    val tint = when {
                        overtime -> GymTheme.colors.heat.spent
                        warning -> GymTheme.colors.heat.hot
                        else -> GymTheme.colors.heat.ready
                    }
                    Text(
                        text = "REST " + TimeFormat.signedMmss(restSeconds),
                        style = MaterialTheme.typography.labelSmall,
                        color = tint.copy(alpha = if (overtime || warning) pulse else 1f),
                        modifier = Modifier.padding(end = 10.dp),
                    )
                }
                IconButton(onClick = onToggleStopwatch) {
                    Icon(
                        Icons.Rounded.Timer,
                        contentDescription = "Stopwatch",
                        tint = if (showStopwatch) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Button(
                    onClick = onFinishClick,
                    shape = RoundedCornerShape(50),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                    modifier = Modifier
                        .graphicsLayer { translationX = recoilX.value * density }
                        .border(
                            width = 1.5.dp,
                            color = errorColor.copy(alpha = 0.85f * errTint.value),
                            shape = RoundedCornerShape(50),
                        ),
                ) {
                    Text("Finish", style = MaterialTheme.typography.labelLarge)
                }
            }
            AnimatedVisibility(visible = showStopwatch) {
                StopwatchRow(
                    shownMs = stopwatchShownMs,
                    running = stopwatchRunning,
                    onToggleRun = onToggleStopwatchRun,
                    onReset = onResetStopwatch,
                )
            }
        }
    }
}

// Built-in stopwatch — independent of the workout clock and the rest timer
@Composable
private fun StopwatchRow(
    shownMs: Long,
    running: Boolean,
    onToggleRun: () -> Unit,
    onReset: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // State lives one level up now (WorkoutSessionScreen), shared with the rest-actions sheet —
    // this row is purely a display + controls bound to it, not its own stopwatch.
    Row(
        modifier
            .fillMaxWidth()
            .padding(top = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = "STOPWATCH",
            style = MaterialTheme.typography.labelSmall,
            color = GymTheme.colors.hint,
        )
        Text(
            text = TimeFormat.clock(shownMs),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFeatureSettings = FONT_FEATURE_TABULAR
            ),
            color = MaterialTheme.colorScheme.secondary,
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onToggleRun) {
            Icon(
                if (running) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (running) "Pause stopwatch" else "Start stopwatch",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
        IconButton(onClick = onReset, enabled = shownMs > 0) {
            Icon(
                Icons.Rounded.Refresh,
                contentDescription = "Reset stopwatch",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: SessionExercise,
    isLast: Boolean,
    handoff: Boolean,
    activeSetId: Long?,
    focusedWeightSetId: Long?,
    barKg: Double,
    dragHandle: Modifier,
    onWeightChange: (Long, String) -> Unit,
    onRepsChange: (Long, String) -> Unit,
    onWeightDrag: (Long, Int) -> Unit,
    onRepsDrag: (Long, Int) -> Unit,
    onCycleTag: (Long) -> Unit,
    onCycleRpe: (Long) -> Unit,
    onToggleComplete: (Long) -> Unit,
    onWeightFocus: (Long, Boolean) -> Unit,
    onAddSet: () -> Unit,
    onGenerateWarmup: () -> Unit,
    onToggleSuperset: () -> Unit,
    onRemove: () -> Unit,
    onSaveNote: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val supersetColor = MaterialTheme.colorScheme.primary
    val inSuperset = exercise.supersetGroup != null
    var noteDialog by remember { mutableStateOf(false) }

    // Law 7: the incoming exercise warms while the handoff lasts, then cools
    val handoffColor by animateColorAsState(
        targetValue = if (handoff) MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
        else Color.Transparent,
        animationSpec = if (handoff) Motion.settle(Motion.FAST) else Motion.settle(400),
        label = "handoffWarmth",
    )

    GlassSurface(
        modifier = modifier.border(1.dp, handoffColor, MaterialTheme.shapes.large),
        shape = MaterialTheme.shapes.large,
    ) {
        Column(
            Modifier
                .then(
                    if (inSuperset) Modifier.drawBehind {
                        // primary-tinted bracket along the left edge marks superset membership
                        drawRoundRect(
                            color = supersetColor,
                            topLeft = Offset(0f, 8.dp.toPx()),
                            size = Size(3.dp.toPx(), size.height - 16.dp.toPx()),
                            cornerRadius = CornerRadius(2.dp.toPx()),
                        )
                    } else Modifier
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Rounded.DragIndicator,
                    contentDescription = "Reorder ${exercise.name}",
                    modifier = Modifier
                        .size(28.dp)
                        .then(dragHandle),
                    tint = GymTheme.colors.hint,
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(exercise.name, style = MaterialTheme.typography.titleMedium)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = exercise.muscleGroup,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (inSuperset) {
                            Text(
                                text = "SUPERSET",
                                style = MaterialTheme.typography.labelSmall,
                                color = supersetColor,
                            )
                        }
                    }
                }
                IconButton(onClick = onToggleSuperset, enabled = inSuperset || !isLast) {
                    Icon(
                        Icons.Rounded.Link,
                        contentDescription = "Superset with next exercise",
                        tint = if (inSuperset) supersetColor else GymTheme.colors.hint,
                    )
                }
                Box {
                    var menuOpen by remember { mutableStateOf(false) }
                    IconButton(onClick = { menuOpen = true }) {
                        Icon(
                            Icons.Rounded.MoreVert,
                            contentDescription = "More options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                        DropdownMenuItem(
                            text = { Text("Add warm-up ramp") },
                            onClick = {
                                menuOpen = false
                                onGenerateWarmup()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(if (exercise.note.isBlank()) "Add machine note" else "Edit machine note") },
                            onClick = {
                                menuOpen = false
                                noteDialog = true
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Remove exercise") },
                            onClick = {
                                menuOpen = false
                                onRemove()
                            },
                        )
                    }
                }
            }

            // the loading call — double progression speaks before the first set
            exercise.plan?.let { plan ->
                val planColor = when (plan.kind) {
                    Progression.Kind.INCREASE -> MaterialTheme.colorScheme.primary
                    Progression.Kind.DELOAD -> GymTheme.colors.heat.worn
                    Progression.Kind.HOLD -> MaterialTheme.colorScheme.secondary
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        when (plan.kind) {
                            Progression.Kind.INCREASE -> Icons.Rounded.TrendingUp
                            Progression.Kind.DELOAD -> Icons.Rounded.TrendingDown
                            Progression.Kind.HOLD -> Icons.Rounded.TrendingFlat
                        },
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = planColor,
                    )
                    Text(plan.line, style = MaterialTheme.typography.labelMedium, color = planColor)
                }
            }

            // sticky machine note: seat height, pin, grip — tap to edit
            if (exercise.note.isNotBlank()) {
                Row(
                    Modifier.clickable { noteDialog = true },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        Icons.Rounded.StickyNote2,
                        contentDescription = "Machine note",
                        modifier = Modifier.size(15.dp),
                        tint = GymTheme.colors.hint,
                    )
                    Text(
                        text = exercise.note,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            SetHeaderRow()

            exercise.sets.forEachIndexed { index, set ->
                SetRow(
                    index = index,
                    set = set,
                    isActive = set.id == activeSetId,
                    onWeightChange = { onWeightChange(set.id, it) },
                    onRepsChange = { onRepsChange(set.id, it) },
                    onWeightDrag = { onWeightDrag(set.id, it) },
                    onRepsDrag = { onRepsDrag(set.id, it) },
                    onCycleTag = { onCycleTag(set.id) },
                    onCycleRpe = { onCycleRpe(set.id) },
                    onToggleComplete = { onToggleComplete(set.id) },
                    onWeightFocus = { onWeightFocus(set.id, it) },
                )
                if (focusedWeightSetId == set.id) {
                    PlateCalculatorPanel(
                        targetKg = set.effectiveWeightKg,
                        barKg = barKg,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }

            TextButton(onClick = onAddSet) {
                Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Add set")
            }
        }
    }

    if (noteDialog) {
        var text by remember(exercise.note) { mutableStateOf(exercise.note) }
        ForgedAlert(
            title = "Machine note",
            onDismissRequest = { noteDialog = false },
            confirmLabel = "Save",
            onConfirm = {
                onSaveNote(text)
                noteDialog = false
            },
            dismissLabel = "Cancel",
            body = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AlertBody(
                        "Seat height, pin, grip width — pinned to ${exercise.name} every session."
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { text = it.take(120) },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        placeholder = { Text("Seat 4 · pin 12 · narrow grip") },
                        shape = MaterialTheme.shapes.medium,
                    )
                }
            },
        )
    }
}

@Composable
private fun SetHeaderRow() {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        HeaderLabel("#", Modifier.width(IndexColWidth))
        HeaderLabel("PREV", Modifier.width(PrevColWidth))
        HeaderLabel("KG", Modifier.weight(1.2f))
        HeaderLabel("REPS", Modifier.weight(1f))
        HeaderLabel("RPE", Modifier.width(RpeColWidth))
        HeaderLabel("TAG", Modifier.width(TagColWidth))
        HeaderLabel("", Modifier.width(CheckColWidth))
    }
}

@Composable
private fun HeaderLabel(text: String, modifier: Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        color = GymTheme.colors.hint,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun SetRow(
    index: Int,
    set: SessionSet,
    isActive: Boolean,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
    onWeightDrag: (Int) -> Unit,
    onRepsDrag: (Int) -> Unit,
    onCycleTag: () -> Unit,
    onCycleRpe: () -> Unit,
    onToggleComplete: () -> Unit,
    onWeightFocus: (Boolean) -> Unit,
) {
    val rowShape = MaterialTheme.shapes.small
    val haptics = LocalHapticFeedback.current
    val ember = MaterialTheme.colorScheme.primary
    val gold = GymTheme.colors.prGold

    // Forged Motion §8: done-state cools in; completion flashes heat once and cools
    // (rung 1); a PR goes white-hot → gold → ember over `forge` (rung 4, Law of Cooling).
    val doneBg by animateColorAsState(
        targetValue = if (set.completed) GymTheme.colors.successDim else Color.Transparent,
        animationSpec = Motion.settle(Motion.STANDARD),
        label = "doneBg",
    )
    val heat = remember { Animatable(0f) }
    val prHeat = remember { Animatable(0f) }
    var wasCompleted by remember { mutableStateOf(set.completed) }
    LaunchedEffect(set.completed) {
        if (set.completed && !wasCompleted) {
            if (set.isPr) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                prHeat.snapTo(1f)
                prHeat.animateTo(0f, tween(900, easing = Motion.Settle))
            } else {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                heat.snapTo(1f)
                heat.animateTo(0f, tween(600, easing = Motion.Settle))
            }
        }
        wasCompleted = set.completed
    }

    Row(
        Modifier
            .fillMaxWidth()
            .clip(rowShape)
            .background(doneBg)
            .drawBehind {
                if (heat.value > 0f) drawRect(ember.copy(alpha = 0.12f * heat.value))
                if (prHeat.value > 0f) {
                    val t = prHeat.value
                    drawRect(gold.copy(alpha = 0.35f * t))
                    drawRect(Color.White.copy(alpha = 0.5f * t * t))
                }
            }
            .border(
                width = 1.dp,
                color = if (isActive && !set.completed) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                } else {
                    Color.Transparent
                },
                shape = rowShape,
            )
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${index + 1}",
            modifier = Modifier.width(IndexColWidth),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        // PREV becomes the e1RM badge once the set is done — the hint has served its purpose
        Box(Modifier.width(PrevColWidth), contentAlignment = Alignment.Center) {
            val weight = set.effectiveWeightKg
            val reps = set.effectiveReps
            if (set.completed && weight != null && reps != null && reps > 0) {
                OneRmBadge(OneRM.estimate(weight, reps), isPr = set.isPr, intensity = set.intensity)
            } else {
                Text(
                    text = if (set.prevWeightKg != null && set.prevReps != null) {
                        "${PlateCalculator.fmt(set.prevWeightKg)}×${set.prevReps}"
                    } else {
                        "—"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFeatureSettings = FONT_FEATURE_TABULAR
                    ),
                    color = GymTheme.colors.hint,
                )
            }
        }

        DragNumberField(
            value = set.weightText,
            // progression suggestion outranks last session as the hint (PREV column
            // still shows the actuals, so both stories stay visible)
            hint = (set.suggestedWeightKg ?: set.prevWeightKg)?.let(PlateCalculator::fmt) ?: "kg",
            onValueChange = onWeightChange,
            onDragStep = onWeightDrag,
            modifier = Modifier.weight(1.2f),
            keyboardType = KeyboardType.Decimal,
            onFocusChanged = onWeightFocus,
        )
        DragNumberField(
            value = set.repsText,
            hint = (set.suggestedReps ?: set.prevReps)?.toString() ?: "reps",
            onValueChange = onRepsChange,
            onDragStep = onRepsDrag,
            modifier = Modifier.weight(1f),
            keyboardType = KeyboardType.Number,
        )
        RpeChip(set.rpe, onClick = onCycleRpe, modifier = Modifier.width(RpeColWidth))
        SetTagChip(set.tag, onClick = onCycleTag, modifier = Modifier.width(TagColWidth))
        CompleteCheck(set.completed, onToggle = onToggleComplete, modifier = Modifier.width(CheckColWidth))
    }
}

@Composable
private fun OneRmBadge(oneRm: Double, isPr: Boolean = false, intensity: Float? = null) {
    val rounded = (oneRm * 2).roundToInt() / 2.0 // nearest 0.5 kg
    // Identity v7: gold is for PRs only — ordinary sets scale ice → volt by intensity
    // (e1RM ÷ all-time best): brighter signal = harder set, Apple Activity style
    val color = if (isPr) {
        GymTheme.colors.prGold
    } else {
        androidx.compose.ui.graphics.lerp(
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.primary,
            intensity ?: 0.6f,
        )
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = color.copy(alpha = if (isPr) 0.28f else 0.16f),
    ) {
        Text(
            text = if (isPr) "★ ${PlateCalculator.fmt(rounded)}" else "1RM ${PlateCalculator.fmt(rounded)}",
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall.copy(
                fontFeatureSettings = FONT_FEATURE_TABULAR
            ),
            color = color,
        )
    }
}

@Composable
private fun SetTagChip(tag: SetTag?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val color = when (tag) {
        SetTag.WARMUP -> GymTheme.colors.tagWarmup
        SetTag.DROPSET -> GymTheme.colors.tagDropset
        SetTag.NEGATIVE -> GymTheme.colors.tagNegative
        SetTag.TEMPO -> GymTheme.colors.tagTempo
        SetTag.FAILURE -> GymTheme.colors.tagFailure
        null -> GymTheme.colors.hint
    }
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (tag != null) color.copy(alpha = 0.16f) else Color.Transparent)
                .border(
                    width = 1.dp,
                    color = if (tag != null) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = tag?.letter ?: "–",
                style = MaterialTheme.typography.labelLarge,
                color = color,
            )
        }
    }
}

// Optional effort rating (RPE 6-10). Tap-cycles like SetTagChip; "–" = not set.
@Composable
private fun RpeChip(rpe: Int?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (rpe != null) MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)
                    else Color.Transparent
                )
                .border(
                    width = 1.dp,
                    color = if (rpe != null) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = rpe?.toString() ?: "–",
                style = MaterialTheme.typography.labelMedium,
                color = if (rpe != null) MaterialTheme.colorScheme.secondary else GymTheme.colors.hint,
            )
        }
    }
}

@Composable
private fun CompleteCheck(completed: Boolean, onToggle: () -> Unit, modifier: Modifier = Modifier) {
    // the check strikes in: opacity lands at `strike`, scale settles home — no wobble
    val t by animateFloatAsState(
        targetValue = if (completed) 1f else 0f,
        animationSpec = Motion.settle(Motion.STANDARD),
        label = "checkStrike",
    )
    val bg by animateColorAsState(
        targetValue = if (completed) GymTheme.colors.success else Color.Transparent,
        animationSpec = Motion.settle(Motion.FAST),
        label = "checkBg",
    )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(30.dp)
                .clip(CircleShape)
                .background(bg)
                .border(
                    width = 1.5.dp,
                    color = if (completed) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = CircleShape,
                )
                .clickable(onClick = onToggle),
            contentAlignment = Alignment.Center,
        ) {
            if (t > 0.01f) {
                Icon(
                    Icons.Rounded.Check,
                    contentDescription = "Set completed",
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer {
                            alpha = t
                            scaleX = 0.6f + 0.4f * t
                            scaleY = 0.6f + 0.4f * t
                        },
                    tint = Color.White,
                )
            }
        }
    }
}
