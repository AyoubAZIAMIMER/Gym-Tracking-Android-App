# MEMORY.md — GymTracker Project State

_Update after every work session. This is the project's source of truth._

## Current phase
**Phase 1 — Android MVP** (in progress — **Room data layer live**: owner's real Progression
backup (244 workouts / 5,875 sets / 18 months) imports cleanly, all screens run on real data,
JSON+CSV export verified, sessions persist. Playtested on emulator with screenshots, 0 crashes.
History tab (calendar + log + workout detail + repeat) shipped 2026-07-13.
Next: smart progression prefill, program target editing, measurements/strength standards)

## Architecture (locked)
- Android: Kotlin, Jetpack Compose, Room, Retrofit, WorkManager, MPAndroidChart, Coroutines
- Backend: FastAPI, PostgreSQL, SQLAlchemy 2.0 async, Pydantic v2, Docker, Azure
- AI: LangGraph (not LangChain), Claude API online, rule-based offline fallback
- RAG: FAISS (exercise embeddings) + SQLite FTS5 (workout history) — no external vector DB
- Sync: offline-first, last-write-wins with timestamp, WorkManager background job
- No ML on Android. No TensorFlow Lite. No on-device LLM.

## UI decisions
_Extracted 2026-07-10 from the 3 reference images (renamed: ui_reference_1.jpg = home dashboard, ui_reference_2.jpg = exercise library, ui_reference_3.jpg = exercise detail — dark-mode screenshots of a German-language gym app)._

- **Design language**: dark-first; layered dark surfaces on a near-black background with a subtle indigo glow; Material3 baseline heavily tuned — large corner radii, pill chips, squircle icon tiles, floating bottom nav; number-forward layouts (big bold numerals, grey captions).
- **Color palette (dark = designed theme)**:
  - Background `#0B0C10` · card surface `#16181E` · raised surface `#1F222A` · outline `#2A2E37`
  - Primary indigo `#5B5EF0` with tonal container `#252A52` — consolidates the reference's two competing blues (chip/FAB indigo ~#5B5BF7 and a brighter icon blue) into one accent
  - Secondary teal `#2CC8D4` (timers/durations — used sparingly, like the reference)
  - Success `#3DCB7D` (completed sets) · PR gold `#F5B62E` (from the gold badge star in image 3) · error `#F2555A`
  - Text: primary `#F2F3F7` · secondary `#9AA0AC` · hint `#5D6370`
- **Typography**: system sans (Roboto), heavy weights (Bold/ExtraBold) for numerals and titles, Medium for labels, grey captions. Tabular figures (`tnum`) wherever digits align in columns (sets table, timers).
- **Component style**: cards 24dp radius, inner elements 12–16dp, full pills for chips/buttons; icon-in-tinted-circle stat tiles; section cards with icon+title headers (image 3 pattern); filter chip rows.
- **Spacing density**: airy cards with 16dp screen padding like the reference — except the workout sets table, which is deliberately denser (precision tool: big touch targets, minimal chrome).
- **Dark mode**: dark is default and the designed target; light theme provided and functional (follows system).
- **Navigation pattern**: bottom nav planned — Home / Library / History / Stats (+ separate opt-in Coach tab in Phase 4). This session ships a placeholder Home → Session flow only.
- **Creative decisions beyond reference** (owner-approved 2026-07-10):
  - Weight drag-adjust steps ±2.5 kg (smallest standard plate pair); reps ±1. Deliberate deviation from the spec's ±1 for weight.
  - Supersets via one-tap link button (groups with the exercise below) + indigo bracket visual — not drag-onto-exercise. The drag gesture stays dedicated to reordering.
  - Rest timer = draggable in-app bubble + foreground-service notification (live countdown, +15s/Skip actions). True system-overlay bubble deferred as a later enhancement.
  - PREV column morphs into a gold e1RM badge once a set is completed (hint no longer needed; no layout shift).
  - Completing a set tints the row success-green, auto-advances the active-row highlight, and auto-starts the rest timer.
  - Plate calculator renders inline under the focused weight field with IPF-colored plate graphics (25 red, 20 blue, 15 yellow, 10 green, 5 white, 2.5 black).
- **Liquid glass restyle (2026-07-11, owner-requested)**:
  - Haze 1.6.10 provides real backdrop blur on API 31+ (automatic translucent fallback below).
  - `GlowBackground` puts indigo/violet radial glows behind every screen (glass needs color behind it to read as glass).
  - `GlassSurface` = blur or translucent wash + gradient hairline border + specular top highlight; tokens live in ExtendedColors (glassTint/glassTintBlur/glassHighlight/glassOutline, dark + light variants).
  - Top bar, FAB, and rest bubble blur the content scrolling beneath them (hazeSource on the LazyColumn, hazeEffect on the overlays); exercise cards use the translucent wash — panels inside the blur source cannot blur it.
  - Bottom sheets stay near-opaque: they render in a separate window layer that Haze cannot capture behind.
  - Compose BOM bumped 2024.10.01 → 2025.05.01 for Haze compatibility; material-icons-extended pinned at 1.7.8 (final release — icons left the BOM after Compose 1.7).
- **Identity v4 — "Molten Forge" (2026-07-13, owner: "the app feels soulless / AI gimmick" → full identity, owner-picked over brutalist + varsity options)**. The rules:
  - ONE hot color: **ember orange `#FF5A1F`** (primary). **Molten gold `#FFC93C` is for PRs only**, never decoration. Everything else is metal: charcoal iron bg `#0E0D0B`, smoked-steel cards `#1A1815`, **quenched steel `#9FB6C2`** as the cool secondary, warm off-white text `#F2EDE4`, tempered-olive success `#B8C77A`.
  - `GlowBackground` = furnace light: ember glow rises from the BOTTOM (standing over coals), faint gold top-right, steel top-left. Glass highlights/hairlines warmed (`#FFE3C2`/`#FFB68A` tints, not white).
  - **Type**: bundled **Anton** (`res/font/anton.ttf`, OFL in `licenses/`) = the forge voice — display/headline/titleLarge ONLY (one weight; never pair with bold). Body stays system grotesk. labelSmall letterSpacing 1.2sp for stamped-label feel.
  - **Logo/icon**: anvil struck so hard a gold spark flies off (ember slit on the waist), charcoal→ember-heat launcher gradient.
  - **Voice (sprinkled, not soaked)**: "The forge is hot." / "Fire it up" / "Back to the anvil" / "Session forged" / PR stat = "forged" / Recovery: "strike where the metal is ready".
  - Light theme = daylight workshop: bone paper `#F4EFE6`, deepened ember `#C63D08`.
- **Palette v3 (2026-07-12, superseded by v4, Apple-Fitness-inspired)**: near-black `#060708`;
  primary volt green `#C6F432`, secondary cyan `#2ED9FF`, energy pink `#FF2D55`.
- **Rebrand (2026-07-11, owner-requested)**: display name **RepForge** (package stays `com.gymtracker`).
  New launcher icon: three ascending white rep-bars + gold PR dot on a diagonal indigo gradient
  (adaptive vector, includes monochrome layer for themed icons).
- **Fitbod-inspired additions (2026-07-11, owner-requested)**: floating glass bottom nav
  (Home / Library / Recovery); Recovery screen with per-muscle freshness % (linear 72h model,
  sample data); Home plan preview (targeted muscles + per-exercise sets×reps·kg);
  "Resume workout" state. Session ViewModel is now **activity-scoped**: Back/app-switch no longer
  discards an in-progress workout (Home shows IN PROGRESS → Resume; reset after save).
- **Round 2 (2026-07-11 evening, owner-requested)**:
  - **Muscle body map** on Recovery: stylized front+back figures (Canvas), regions tinted by
    freshness with % pills — geometric-minimal rather than anatomical (fits the design language).
  - **Glass amplified**: GlowBackground's glow layer is now a `hazeSource`; `LocalBackgroundHaze`
    lets every GlassSurface do real backdrop blur (API 31+) — cards included; glows brightened.
  - **Data screen** (gear on Home): Progression import + JSON/CSV export via SAF; no bottom nav
    on subpages (tab save/restore would resurrect them — learned the hard way).
  - Imported Progression prefs are honored: rest timer 90 s, bar weight for the plate calculator.
  - Session prefill now = real last workout (warm-ups excluded from hints); finishing a workout
    persists to Room and feeds the next session's hints.
- **Progression-style additions beyond the original checklist (2026-07-11)**:
  - Warm-up ramp generator per exercise (menu → bar×10, 40%×8, 60%×5, 80%×3, rounded to 2.5 kg, W-tagged, prepended).
  - Home dashboard: weekly goal ring + flame streak, Mo–Su day strip, quick-start card, 3 stat tiles (all sample data until Room).

## Feature checklist

### Phase 1 — Android MVP (offline-first)
#### Data layer
- [x] Room schema: ExerciseEntity, WorkoutEntity, SetEntity _(ProgramEntity, TemplateEntity, MeasurementEntity, CoachingCardEntity pending — programs/measurements not needed yet)_
- [x] DAOs: ExerciseDao, WorkoutDao, SetDao _(ProgramDao pending)_
- [x] Repositories wiring DAOs to ViewModels _(WorkoutRepository singleton — home stats, freshness, templates, save, import/export)_
- [x] Exercises seeded from JSON on first launch _(108 curated with descriptions/muscles/equipment — quality over the 300 target; assets/exercise_catalog.json, mirrored in exercises_db/)_
- [x] Data export: full JSON + CSV (per-set, not just totals) _(verified: 5,876-line CSV with e1RM)_
- [x] Data import: **Progression .pgnbkp** (sessions/sets/customs/prefs; built-ins become "Exercise #N" placeholders) _(restore from RepForge's own JSON export still pending)_

#### Workout session (core screen — most important)
_Built 2026-07-11 UI-first with in-memory sample data; Room wiring + device test pending._
- [x] Pre-filled sets from last session (weight + reps as greyed hints) _(sample data until Room)_
- [x] Drag handle on weight/reps fields (drag up/down to adjust) _(weight ±2.5 kg, reps ±1)_
- [x] Set completion checkbox (tap → done, auto-advance to next set)
- [x] Set tagging: W warmup, D dropset, N negative, T tempo, F failure
- [x] Superset grouping _(link button + indigo bracket — owner-approved deviation from drag-onto)_
- [x] Floating rest timer overlay bubble (survives app-switch) _(in-app bubble + foreground-service notification; system-wide overlay deferred)_
- [x] Built-in stopwatch _(toggle in session top bar: start/pause/reset, elapsedRealtime-based so it survives app-switch)_
- [x] Plate calculator (inline: shows which plates to load for target weight)
- [x] 1RM badge per set (Epley >5 reps, Brzycki ≤5 reps) _(shown in PREV column once set completes)_
- [x] Add exercise mid-workout (modal library sheet) _(starter list of 18 until seeding)_
- [x] Finish workout → session summary → prompt for comment _(persistence pending Room)_

#### Other screens
- [x] Home screen (today's program, quick-start, recent workouts) _(weekly ring, streak, Mo–Su strip, quick-start, stat tiles — sample data until Room)_
- [x] Exercise library (300+ exercises, filter by muscle group + equipment) _(screen built: search + muscle chips + A-Z sections + detail sheet; 18 starters — 300+ seed and equipment filter arrive with the data layer)_
- [x] Custom exercise creation (name, muscle group, equipment, notes) _(+ edit, archive/delete, and merge-placeholder-into-named for imported history)_
- [x] Program builder (multi-week programs with day structure) _(single-cycle day list — weeks dimension pending; add/delete days & exercises, set active, rotation pointer)_
- [x] Workout templates (reusable day definitions) _(prebuilt: PPL, Upper/Lower, Bro Split, Full Body; program days act as templates)_
- [x] History screen (calendar heatmap + workout list) _(+ workout detail with PR stars and "Repeat this workout"; 6th bottom-nav tab)_
- [ ] Timeline view per exercise (all sets ever, scrollable)
- [ ] PR detection (auto-flag new personal records with celebration)
- [ ] Workout notes and session comments
- [ ] Plate configuration (set bar weight + available plates)
- [x] Dark mode support _(dark = designed default; light theme derived, follows system)_
- [ ] Health Connect integration
- [ ] Settings (kg/lbs toggle, bar weight, plate setup, data management)

### Phase 2 — Analytics (all Progression Pro features, unlocked)
_Pulled forward 2026-07-12 at owner request; charts are custom Compose Canvas (MPAndroidChart dep unused — glass-styled, theme-aware)._
- [x] Per-exercise progress graph (1RM over time) _(smooth line + gold all-time-best dot, ExerciseStatsScreen)_
- [x] Volume load chart (sets × reps × weight per week) _(12-week bars + dashed 4-week avg + Δ% chip, Stats tab)_
- [ ] Frequency heatmap (muscle groups × days) _(training-day calendar heatmap shipped instead; muscle×day needs exercise renames/muscles)_
- [x] Plateau detection indicator (4-session window, no 1RM improvement) _(badge on exercise page)_
- [ ] Strength standards comparison (bodyweight ratio benchmarks) _(needs bodyweight — measurements tracker first)_
- [x] Session duration and rest time trends _(duration line chart; rest trends pending — rest not persisted per set)_
- [ ] Total volume by muscle group (weekly and monthly) _(needs muscle assignments on imported exercises)_
- [x] PR timeline across all exercises _(weight PRs, baseline excluded, Stats tab)_
- [x] Advanced CSV export (per-set with timestamp, exercise, weight, reps, 1RM)
- [ ] Body measurements tracker (weight, measurements over time)

#### Beyond Progression (added 2026-07-12)
- [x] e1RM trend badge: least-squares slope over last 90 days (kg/week, ▲/▼)
- [x] Progressive-overload "READY: +2.5 kg" badge (SKILL.md rule: 2 sessions same top weight, reps held)
- [x] GitHub-style training calendar heatmap (volume quartile intensity, last 20 weeks)
- [x] Most-trained list (30-day volume) → per-exercise stats deep link (also from Library sheet)

### Phase 3 — Backend + cloud sync
- [ ] FastAPI scaffold + PostgreSQL schema (mirrors Room schema)
- [ ] JWT authentication
- [ ] Sync endpoints with conflict resolution (last-write-wins by updated_at)
- [ ] WorkManager background sync on Android
- [ ] Cloud backup and restore
- [ ] Docker Compose setup + Azure deployment

### Phase 4 — Agentic AI coaching layer
- [ ] LangGraph graph: router → retriever → analyst → planner → responder
- [ ] Tools: get_recent_sessions, estimate_1rm, detect_plateau,
      suggest_next_weight, search_exercises (FAISS)
- [ ] SQLite FTS5 on workout notes and exercise descriptions
- [ ] FastAPI /agent/chat streaming endpoint (Server-Sent Events)
- [ ] Android coach screen: chat UI + offline banner + cached coaching cards
- [ ] Progressive overload engine (rule-based + ML hybrid)
- [ ] Overtraining risk score (volume + frequency signals)
- [ ] PR prediction (linear regression on 1RM curve)
- [ ] Natural language workout summaries (Claude API, cached to Room)

## Known issues / blockers
- ~~Imported built-ins are "Exercise #N" placeholders~~ **SOLVED 2026-07-13**: "Name imported
  exercises (CSV)" on the Data screen recovers real names from a Progression CSV export by
  timestamp matching (UTC-offset auto-calibrated, ±2 s + weight/reps). Owner's data: 37/38
  named (26 renamed, 11 token-merged into muscle-tagged catalog entries); 1 low-volume id
  left → rename via Library → Edit.
- Restore from RepForge's own JSON export not implemented yet (import only parses Progression format).
- Program exercise targets are fixed at add time (template values, imported ranges, or 3×8-12 manually) — target editing UI pending; programs are single-cycle (no week periodization yet).
- Progression's per-movement plans can vary per set (e.g. 3 ranged + 1 AMRAP); import takes the first ranged plan for the whole movement.
- An in-progress session survives Back/app-switch but not a process kill (finished sessions persist).
- Exercise drag-to-reorder does not auto-scroll when dragging past the viewport edge.
- Bottom sheets are near-opaque by design (separate window layer, Haze can't blur behind them).
- Dev-machine note: the local router's DNS is flaky — if Gradle fails resolving a repo host, just re-run the build (artifacts cache monotonically).
- IMPORTANT build note: after big dependency bumps run `./gradlew clean` — AGP incremental dexing
  produced a mixed-version dex (launch crash: `NoSuchMethodError: Composer.shouldExecute`) on 2026-07-11.
- Gson must stay pinned ≥2.10 (converter-gson drags in 2.8.5 which lacks `JsonParser.parseString`).

## Session log
| Date | Model used | Work done |
|------|-----------|-----------|
| 2026-07-15 | — | **README.md**: project overview, shields.io + skillicons tech badges (Kotlin/Compose/Room/FastAPI/LangGraph/etc.), GitHub star/fork badges, star-history.com star counting diagram, features table, structure, build steps, mermaid architecture diagram |
| — | — | Project initialized |
| 2026-07-11 | Fable 5 (+ Sonnet subagent for Gradle/res boilerplate) | Repo scaffold + AGENTS.md/MEMORY.md/backend SKILL.md; UI extraction from 3 refs logged; theme (Color/Type/Shape/Theme); utils (OneRM, PlateCalculator, TimeFormat); WorkoutSessionScreen + ViewModel + DragNumberField/RestTimerBubble/PlateCalculatorPanel/ExercisePickerSheet/FinishSummarySheet; RestTimerService (FGS); HomeScreen placeholder + MainActivity nav; toolchain installed (JDK 17, Gradle 8.9 wrapper, SDK 35); `assembleDebug` GREEN → app-debug.apk |
| 2026-07-11 | Fable 5 | Owner manually tested session screen v1 ✓. Liquid-glass restyle: Haze 1.6.10 + BOM 2025.05.01, Glass.kt (GlassSurface/GlowBackground), glass top bar/FAB/rest bubble, translucent cards. Added: Home dashboard (weekly ring, streak, Mo–Su strip, quick-start, stat tiles — sample data), built-in stopwatch (top bar), warm-up ramp generator (exercise menu). `assembleDebug` GREEN |
| 2026-07-11 | Fable 5 | Crash fix + rebrand + emulator playtest. Root cause of "keeps stopping": stale incremental dex after BOM bump (`Composer.shouldExecute` NoSuchMethodError) → fixed by clean rebuild. Rebranded to **RepForge** + new adaptive icon. Added: glass bottom nav, ExerciseLibraryScreen, RecoveryScreen (muscle freshness), Home plan preview, activity-scoped session VM (Back-safe, Resume workout). Fixed: LocalContentColor in Glass containers (dark-mode black titles), theme-aware washes (light mode). Playtested end-to-end on API 37 emulator via adb (16 screenshots): set completion/1RM/rest bubble+notification/plate calc/tags/stopwatch/warm-up ramp/back-resume/finish/save — 0 crashes |
| 2026-07-14 | Fable 5 | **Exercise demonstration photos + first-run profile.** Owner asked to "copy" training.fit images (copyrighted) → bundled the **public-domain free-exercise-db** instead (Unlicense; licenses/UNLICENSE-free-exercise-db.txt): matched **149/149** catalog+curated names via token match + ~90-entry hand alias table (script in scratchpad; e.g. "Dumbbell Side Raise"→"Side Lateral Raise", "Machine Row"→"Leverage High Row", "Pec Deck"→"Butterfly"), downloaded 216 photos → `assets/exercise_media/` (13 MB, sips-resized 640px) + `assets/media_map.json` (tokenKey→paths). New `ExerciseMedia` loader + `ExerciseDemo` composable: start↔end frames crossfade every 1.4 s (settle) on a white photo card in the Library detail sheet, above the MuscleTargetFigure. APK 21→36 MB. **Profile (first-run)**: repo Profile prefs (name/bodyWeightKg/heightCm/weeklyGoal, KEY_PROFILE_*, weekly_goal default 3); `ProfileSheet` ("Who's at the anvil?") auto-opens on Home until name saved (dismissible), editable via Data→Profile card; Home greeting "Hi, {name}" (fallback "smith"), weekly ring/tiles driven by profile weeklyGoal. Verified e2e on emulator (profile saved → "Hi, Ayoub"; Bulgarian Split Squat sheet shows photo+figure; adb quirk: keyboard-open shifts sheet coords — fill fields one at a time). 0 crashes |
| 2026-07-14 | Fable 5 | **Renamed to "Forged" + muscle-target figures + curated muscle fill + forge tile icons.** app_name → **Forged** (owner authorized; package stays com.gymtracker; anvil icon kept — verified in drawer). New `MuscleTargetFigure` in MuscleBodyMap.kt: front+back anatomical figure with the exercise's target muscles hot red + heat halo (training.fit-style concept, own artwork → no licensing, works offline incl. custom exercises); shown in Library detail sheet (muscles canonicalized via ProgressionImporter.canonicalMuscle). New `data/ExerciseInfo.kt`: ~50 curated ExRx/ACE-consensus entries (owner's exact imported names: Smith Machine Bench Press, Bent-Over Machine T-Bar Row, Machine Preacher Curl, Dumbbell Side Raise, Machine Hyperextension, Ab Machine, Decline bench crunches, …) matched by CsvNamer.tokenKey; `repo.fillExerciseInfo()` (idempotent, blank-only, runs at every launch + after CSV naming) fills muscles/equipment/description. Verified: previously-blank imported exercises all show muscles, T-Bar Row figure renders (back+biceps hot). Home tiles: EventAvailable/History/LocalFireDepartment → **Hardware (hammer) "Strikes this week" / HourglassBottom "Since last strike" / Whatshot "Week streak"**. 0 crashes. NOTE: emulator DB now has test junk (a saved test workout + 100 kg Smith PR) — diverges from owner's phone data; pm clear + re-import before data-sensitive playtests |
| 2026-07-14 | Fable 5 | **Forged Motion phase 2 (Hot Tip, handoff, Recoil, sheens, forge heat).** Charts.kt: LineChart tempers in left→right over `slow` led by the **Hot Tip** (white-gold radial at the leading edge, clipRect draw; caught on camera mid-draw); WeeklyBarChart bars rise staggered + avg line fades in last; both once-per-entry via rememberSaveable. New `ForgedRing` (Hot Tip progress ring) replaces Home's CircularProgressIndicator. **Law 7 heat handoff**: completing an exercise's last set warms the next card's border 80 ms later for 400 ms (verified frame-perfect on emulator). **Recoil**: Finish with 0 completed sets = 2 px hard-stop recoil + red border cooling 400 ms + heavy haptic, sheet refuses to open (functionally verified). `SteelSheen` (warm sheen sweep, 1.8 s temper) replaces DataScreen's LinearProgressIndicator — no spinners anywhere. GlowBackground gained `emberHeat` param: the session screen animates the bottom ember 1→1.25× on entry (the forge burns hotter at the anvil). Rest bubble time rolls (AnimatedContent slide) only on >2 s jumps (+15 s/skip); ticking stays calm. Build first-try green, 0 crashes |
| 2026-07-14 | Fable 5 | **Forged Motion implemented (phase 1: tokens + set-logging path + navigation).** New `ui/theme/Motion.kt` = the ONLY home of durations/easings/springs (settle/cool/plane/temper, strike 80→forge 900, springs ζ≥0.85) + `Modifier.forgedPress` (contact-frame compression: buttons 0.97, glass cards 0.985 via GlassSurface) + `Modifier.forgedEntrance` (30 ms stagger, rememberSaveable = never replays). Wired: NavHost transitions (tab plane slides ±12 px w/ direction from TabOrder, subpage lift, session rises from bottom, pop = cool at 0.7×); set completion = check strike + row heat-flash cooling 600 ms + tick haptic; **live PR detection** (SessionSet.isPr; VM baseline from new `repo.bestE1rmByExercise()`, warm-ups/first-evers excluded, baseline ratchets per PR) → white-hot→gold overlay 900 ms + heavy haptic + gold "★ e1RM" badge; nav tint heats (animateColorAsState plane/fast); rest ring breathes ±2% 4 s (session's one ember); Home streak flame flickers on 3.2 s+5.1 s sines (Home's one ember); weight-drag detents haptic-tick per 2.5 kg step; FinishSummarySheet de-bounced (MediumBouncy → settle, Law 2) + stats stagger 60 ms. Playtested on emulator: PR flow verified end-to-end (★ 126.5 badge on forced 100 kg set), animations visibly mid-flight between frames, tab/nav clean, **0 crashes**. Emulator test session NOT saved (user data untouched) |
| 2026-07-13 | Fable 5 | **Forged Motion design system (document-first, no code yet).** Owner asked for a premium Motion Design System ("motion as identity, mass and purpose, never playful/bouncy"). Wrote canonical spec `design/MOTION.md`: Seven Laws (contact ≤1 frame; ζ≥0.85 "steel doesn't wobble"; Law of Cooling = expo-decay settles; heat = emphasis channel; one live ember/screen; one axis; heat handoff), duration tokens (strike 80 → forge 900), 4 easing curves (settle/cool/plane/temper), 3 springs (firm/mass/heavy), celebration ladder (set→PR white-hot flash), Recoil error, Hot Tip progress, bench navigation model, Four Questions governance gate. Published interactive live-demo version as artifact (https://claude.ai/code/artifact/a957d447-aab8-4910-8ee5-612690b4b9a9). NOTES: owner consistently calls the app **"Forged"** now (rename from RepForge pending owner confirmation — strings.xml one-liner); owner's prompt said Flutter but app is Compose → spec targets Compose (`ui/theme/Motion.kt` plan) with Flutter appendix. Implementation not started — awaiting owner review of the system |
| 2026-07-13 | Fable 5 | **"Molten Forge" identity (v4).** Owner: app felt soulless/AI-generic → offered 3 directions (forge / brutalist / varsity), owner picked Molten Forge at full depth. Rebuilt Color.kt (ember/iron/gold/quenched-steel, dark + workshop-light), bundled Anton for display type, furnace-light GlowBackground (ember rises from bottom), warmed glass hairlines, new anvil-and-spark launcher icon + heat-gradient bg, forge voice in key copy (Fire it up / Session forged / forged PRs / recovery-as-cooling). Verified on emulator light+dark: Home/History/Recovery/Stats/Session all cohesive (calendar reads as glowing coals, stats charts as embers, recovery as cooling metal). Needed `clean assembleDebug` (known stale-dex). 0 crashes |
| 2026-07-13 | Fable 5 | **History tab (calendar + workout log + detail).** 6th bottom-nav tab (nav paddings tightened to fit 412 dp). HistoryScreen: Monday-first month grid, volume-scaled volt heat dots, today outline, tap-to-filter day, month paging clamped to [first workout, now]; list rows with duration/sets/volume/muscles. WorkoutDetailScreen (`workout/{id}` subpage): totals card (duration/sets/volume/★PR count), per-exercise sets with dimmed W warm-ups + gold PR stars (e1RM beats all prior history, first-ever excluded), comment card, tap exercise → stats, **Repeat this workout** → prefilled session (`templateFromWorkout` generalizes latest-template; `prepareRepeat` on session VM). New DAO: workouts `between/byId/earliestStart`, sets `forWorkouts/forExercisesBefore`. No schema change. Playtested on owner data (light+dark): July shows 6 workouts/58,862 kg, May 2026 11 workouts, day filter, PR stars on Lower 2, repeat opens live session, tab-switch doesn't resurrect subpage. 0 crashes |
| 2026-07-13 | Fable 5 | **Program import (owner's ABAB + Upper Lower V1).** ProgressionImporter now parses backup `programs` (weeks→routines→workout→movements with rep ranges; AMRAP → 0/0, `Formats.repRange` renders it). Stable ids (program/workout/movement UUIDs) → re-imports upsert. Import path switched to IGNORE inserts + a persisted alias map (prefs) recording merges, so re-importing never undoes CSV-naming merges and program refs resolve through aliases; mergeExercise also reassigns program_exercises. Verified fresh flow: "…44 exercises, 2 programs ✓" → CSV naming → Plan shows ABAB + Upper Lower V1 with real names (Squat (Barbell) 4×6-10 … Machine Calf Raise 4×AMRAP). 0 crashes |
| 2026-07-13 | Fable 5 | **CSV auto-naming (owner's real export).** CsvNamer: tolerant parser (Progression's Date + Set Timestamp split columns, Weight Unit, BOM, quoted fields), UTC-offset auto-calibration (−14h…+14h scan → found +1 h), exact ±2 s matching with weight/reps tie-breakers, ≥3-vote/80% winners, token-key merges ("Barbell Squat"="Squat (Barbell)" → inherits muscles). Verified full user flow on a fresh AVD: seed → import backup (244/5875) → CSV naming → "Named 26 + merged 11" (offline simulation predicted identical results). Home/Recovery/Library now show real names; Recovery tracks 7+ muscle groups. Fixed: unnamed-left counter counted by id prefix instead of name. 0 crashes |
| 2026-07-12 | Fable 5 | **Exercise catalog + CRUD + merge, programs + templates, palette v3.** DB v2 migration (description column, program tables). Seeded 108-exercise catalog (descriptions/muscles/equipment) + YouTube form-video links in detail sheets. Exercise create/edit/archive/merge — merging "Exercise #53"→"Lateral Raise (Dumbbell)" verified: 161 sets attached, PRs renamed in Stats automatically. Plan tab (5-tab nav): prebuilt PPL/Upper-Lower/Bro-Split/Full-Body templates, program editor (days/exercises/active), Home shows active program's next day, sessions start from program days with rep-range hints, finishing rotates the day pointer. Recolored to Apple-Fitness palette (volt/cyan/pink on near-black). Emulator-verified end-to-end, 0 crashes. Fixed: best-e1RM FP formatting, Library FAB overlap, picker subtitle |
| 2026-07-12 | Fable 5 | **Analytics (Phase 2 pull-forward, owner-requested).** AnalyticsEngine (pure Kotlin, mirrors SKILL.md): e1RM/volume series, weekly volume, weight-PR timeline, plateau (4-session), 90-day trend slope, overload readiness, duration series, calendar volume, top exercises. Compose-Canvas chart kit (LineChart w/ gold max dot, WeeklyBarChart w/ 4-wk avg dashed line + Δ% chip, CalendarHeatmap). New Stats tab (4-tab nav) + ExerciseStatsScreen (badges, tiles, charts, recent sessions) reachable from Stats top-list and Library sheet. Fixed: FP-noise volume formatting (Formats.volumeKg), another stale-dex build failure (clean rebuild). Emulator-verified on real data: weekly volume ▲15%, PR list, Exercise #230 page (124.55 best e1RM, 343 sets, 258,930 kg, all 3 badges) — 0 crashes |
| 2026-07-11 | Fable 5 | **Room data layer + Progression import/export + body map + glass v2.** Analyzed owner's .pgnbkp (244 sessions / 5,875 sets / Jan-2025→today). Built: Entities/DAOs/GymDb, WorkoutRepository (import, stats, freshness, templates, save, JSON+CSV export), ProgressionImporter (customs, "Exercise #N" placeholders, WARMUP→W, lbs→kg, prefs: rest 90 s + bar 20 kg), Data screen (SAF import/export), Home/Recovery/Library ViewModels on real data, session prefill from last real workout + persistence on finish, MuscleBodyMap (front/back figures + % pills), LocalBackgroundHaze (real blur on all cards). Fixed: gson pin 2.10.1, tab save/restore resurrecting the Data subpage. Emulator-verified with owner's real backup: import summary exact, Home 4/3+6-wk streak real, Recovery real freshness, session "Lower 2" prefilled, rest 1:30, exports pulled+validated (5,876-line CSV) — 0 crashes |
