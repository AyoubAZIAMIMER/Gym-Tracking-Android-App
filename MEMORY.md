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
_Extracted 2026-07-10 from the 3 reference images (renamed: design/references/ui_reference_1.jpg = home dashboard, design/references/ui_reference_2.jpg = exercise library, design/references/ui_reference_3.jpg = exercise detail — dark-mode screenshots of a German-language gym app)._

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
- **Identity v5 — "Heat Is Data" (2026-07-17, owner: "seems like it has just one color… give it an identity" + "use real human physique")**. Full spec in `design/IDENTITY_V5.md`; v4 stays in force except where widened:
  - v4's "one hot color" becomes "one hot **axis**": the forge heat spectrum — quenched steel `#8FB4C7` → warming bronze `#D08A45` → ember `#FF5A1F` → glowing red `#FF3320`. New hues enter ONLY as temperatures via `forgeHeat(freshness)` in `ui/theme/Color.kt` (single source of truth; screens never hand-pick heat hues). Gold still PRs-only; ember still the action color.
  - **Recovery inverted to the metaphor**: fresh muscle = quenched steel (cooled, ready to strike), just-trained = glowing red. Replaced v4's green/gold/red traffic light, which never belonged to the forge story. Heat legend strip (`COOLED · READY` ↔ `GLOWING`) captions the body map; % pills removed from the body (numbers live in the BY MUSCLE list).
  - **Anatomical physique** replaces the geometric body map: `MuscleBodyMap.kt` fully rewritten — hand-authored vector anatomy in a 100×170 unit space (left side authored, right side mirrored), individual muscle bellies (traps, delts, pecs, biceps/triceps, rectus with cut lines, obliques, lats, erectors, glutes, quads, hams, calves) mapped to the 10 canonical groups; hot muscles emit a stroked halo ("hot metal radiates"). Same paths drive `MuscleTargetFigure` (Library detail: targets at full ember glow).
  - **Stats weekly-volume bars** are heat-mapped by effort (heaviest week glows red, light weeks bronze/steel) — `WeeklyBarChart` in `Charts.kt`.
  - **Session heat badges**: completed non-PR sets now wear a heat-tinted e1RM badge (intensity = set e1RM ÷ all-time best, stored on `SessionSet.intensity` at logging; `OneRmBadge` colors via `forgeHeat`). This also fixes a v4 violation — gold badges were on every completed set; gold is now truly PR-only (★). Warm-ups read steel, top sets glow.
  - **Home heat accents**: plan-preview dots tint by each exercise's primary-muscle freshness (`PlanRow.freshness`, resolved in HomeViewModel via `repo.muscleFreshness()` + `canonicalMuscle`); the "Since last strike" hourglass cools from glowing (0d) to quenched steel (≥3d).
  - **Heat is per-theme** (2026-07-17 later session): `forgeHeat()` replaced by `HeatScale` (`ui/theme/Color.kt`) exposed as `GymTheme.colors.heat` — `DarkHeat` (original stops) vs `LightHeat` (every stop deepened for ≥3:1 on bone paper: steel `#4E7086`, bronze `#9A5A1D`, ember `#C63D08`, red `#B3230F`). All call sites (body maps, Recovery rows/legend, Stats bars, session badge, Home) go through the theme. Light mode playtested: Home/Recovery/Stats verified on emulator.
  - **Home redesign**: the plan card now shows today's targets glowing on the anatomical physique (`MuscleTargetFigure`, 170dp, replaces the muscles text line) — Home answers "what do I strike today?" with a body, not a sentence. Combined with heat dots per exercise + the cooling hourglass tile.
  - Playtested on emulator 2026-07-17 (Recovery, Library detail, Stats, Home dark+light, live session with a 3-set intensity ladder incl. PR gold): renders correctly, screenshots refreshed. v5 rollout complete across both themes.
- **Identity v7 — "NIGHT SESSION" (2026-07-19, owner: "change the colours and the forged theme… more Apple-like, fits GYM apps identity more")**. Full spec `design/IDENTITY_V7.md`; supersedes v4/v5 colors + forge voice, keeps all structural laws (one accent, semantic color only, motion system, physique, glass architecture):
  - **Palette**: matte near-black `#050607` floor, cool iron cards `#131518`/`#1C1F23`, chalk white `#F5F7F8`; **VOLT `#D2F542`** = the one signal (action, effort, completed work — Success IS volt now); **ICE `#64D2FF`** = recovery/time; gold `#FFD60A` PRs-only (iOS yellow); red `#FF453A` failure/errors; tags = iOS system colors. Identifier names in Color.kt kept (AccentPrimary etc.) so wiring didn't churn.
  - **HeatScale → readiness ramp** (props renamed ready/worn/hot/spent): volt→amber `#FFB020`→orange `#FF7A24`→red — the sports-device convention. Charts/badges no longer use the ramp for effort: **single-hue volt intensity** (alpha by magnitude, Apple Activity style); e1RM badge lerps ice→volt by intensity.
  - **Type**: Anton retired — system grotesk at Black weight, tight negative tracking (display/headline), ExtraBold titleLarge. anton.ttf left in res (unused).
  - **Ambience**: furnace glow → floodlight (volt floor wash 0.12, ice top-left 0.08, gold corner 0.05); glass hairlines neutral white.
  - **Voice de-forged in UI copy** (persistence keys `repforge.db`/prefs/export format untouched): "Lights on." / "Start session" / "Resume session" / "IN SESSION" / "LOG SET" / "SESSION COMPLETE" / "Nothing logged"+"Discard" / Recovery "READY↔SPENT" / widget-notification "since last session"/"Time to train"/"Start now" / "FOCUS MODE" chip / Progression "deload to". App name stays Forged.
  - **Launcher mark v3 "Volt Dumbbell"**: v2 geometry recolored — chalk-steel plates, volt bar (white-hot center), gold PR spark, black floor bg with volt wash.
  - Playtested dark (Home/Recovery/Stats/Strike) + light (Home) on emulator; repo screenshots refreshed. History calendar heatmap inherits volt automatically.
- **Identity v9 — "Ember, Restored" (2026-08-07, owner: "Actually want ember back")**. Full spec
  `design/IDENTITY_V9.md`; reverts the color axis of **Identity v8 "Blue Hour"** (electric indigo
  `#5B5BF7` primary — shipped on branch `redesign/blue-hour` after v7, not previously logged in
  this file) back to v5's ember `#FF5A1F`. Trigger: the owner pointed at the live Claude Design
  project behind `design/redesign-2026-07/`, read directly via the `DesignSync` tool — its own sync
  log showed it was built against `design/IDENTITY_V5.md`, not v8, so its mockup and the shipped
  app had quietly diverged on color. Restored from git history (`git show d9c1dea:.../Color.kt`,
  the exact tree the design project synced against), not reconstructed from prose: ember primary +
  dim/bright/on variants, warm charcoal-iron surfaces, `Success` decoupled back into its own
  tempered-olive literal (v7/v8 had merged it into the one signal color), `HeatScale` restored to
  v5's four-stop curve (field names `ready/worn/hot/spent` kept from v8's API — only values/curve
  moved, so Recovery/Strike Mode/rest-timer call sites needed no changes), warm glass hairlines.
  Motion and the subtle rank ladder were never in question and are untouched. Two real bugs fixed
  while restoring, not just re-colored: Home's readiness tag was rendered in a flat primary color
  regardless of which ready/worn/hot/spent bucket it named (only looked right under v8 by
  coincidence); the "This Week" done-day checkmarks were reading primary instead of the separate
  `success` role the mockup actually shows (olive, distinct from the ember Start button). Launcher
  icon (`ic_launcher_foreground.xml`, `ic_launcher_bg_gradient.xml`) and the home-screen widget
  (`widget_bg.xml`, `widget_forge.xml`) hand-repainted to match (XML art can't reference Kotlin
  theme constants).
  - **Home rebuilt 1:1 from the prototype markup (same session).** The first pass matched only
    color, because it worked from the bundle's *screenshots*; the owner pushed back ("I dont see
    the design and exact details"), and reading the actual prototype HTML
    (`design/redesign-2026-07/prototype/Forged Redesign.dc.html`, Home block ~lines 80-140) exposed
    a dozen structural gaps the screenshots hid. **Read that HTML, not the PNGs, before building any
    remaining screen.** Consequences: (1) **Anton un-retired** — the prototype sets `font-family:
    Anton` on the brand mark, Start CTA and hero numerals; `anton.ttf` was still in `res/font/`
    unused since v7. Restored as `Type.kt`'s `Anton` family, stamped-accent discipline intact (never
    body copy). (2) New `StampLabel` TextStyle — mono 10sp/1.7sp for section labels. (3) **Home has
    NO hero glass card**: flat full-bleed sections split by hairlines. `GlassSurface` still rules
    every other screen — Home is the prototype's deliberate exception. (4) Week cells are 28dp
    squircles (`Dim.weekCellRadius` 10dp), not 34dp circles — `Dimens.kt`'s original 34dp did not
    match the prototype. (5) New `Modifier.emberBloom` in `Glass.kt` reproduces the CTA's
    `box-shadow: 0 0 26px rgba(255,90,31,.45)` by stacking 18 expanding rounded rects — Compose
    ignores `shadow()`'s ambient/spot tint below API 28 (minSdk is 26), and few/fat layers band
    visibly. (6) NEXT UP rows gained chevrons; RECENT shows two rows (new `recentWorkouts(count)`
    + `WorkoutDao.latestN`). (7) "· about N min" comes from `estimatedMinutesFor(dayName)` —
    averages this day's own past runs (5 most recent, 5..240 min guard), null until it has history.
  - **All tab screens rebuilt from the prototype (same session).** Two prototype files ship in the
    bundle and they disagree: `Forged Redesign.dc.html` is a design-*exploration* doc (turns/options,
    stale metrics); `Forged Prototype.dc.html` is the canonical runnable app with `data-screen-label`
    per screen and is what the 12 screenshots came from. **Build from the latter.** Extracted its
    shared vocabulary into `ui/components/ForgedSection.kt` (`ForgedScreenTitle` 28sp/-0.5,
    `SectionRule`/`RowRule`, `StampText`/`StampLabel` mono 11sp/1.2, `ForgedSectionHeader`,
    `ForgedListRow`, `ForgedWeekStrip`, `EffortBars`) and rebuilt Plan, Body, History, Stats,
    Library and Data-as-Settings on it — all flat + hairline-ruled, no hero cards. `GlassSurface`
    now survives only where something is a genuinely distinct plate (session top bar, Stats rank
    cue, exercise-detail hero). New: `ForgedIcons.kt` ports the prototype's own stroke icon set
    (24-unit box, 1.9 stroke, round caps) plus its filled 5-rect barbell brand mark, replacing
    Material's nav icons. `GlowBackground` gained `glowAlpha` and now hangs the ember wash ABOVE the
    content (`at 50% -8%`) with a 3dp dot grain tiled via `ImageShader` (one draw call, not 40k
    dots). New data: `WorkoutDao.latestN`/`latestNamed`, `repo.programDays()`,
    `repo.recentWorkouts(n)`, `repo.estimatedMinutesFor(day)`; `RecoveryViewModel` computes a
    muscle-mass-weighted readiness % + a "what to train" call; `PlanViewModel` gained the week
    strip + session list; `DataViewModel` gained profile name/streak. `EffortBars` (5 rising bars,
    Easy→All out) replaced the RPE tap-cycle chip — it maps 1..5 onto the stored RPE 6..10.
    Not built: the prototype's *dashed* rest-day week cell — the app has no rest-day model
    (programs are a rotation pointer, not a weekday calendar), so it was left out rather than faked.
- **Design handoff v2 — `design_handoff_forged_android/` (2026-08-08, owner re-exported from Claude
  Design and said "use it")**. Supersedes `design/redesign-2026-07/`: same 12 screenshots and
  prototypes, but now ships **real Kotlin** (`kotlin/`) plus `BUILD_ORDER.md`. Read BUILD_ORDER
  first — it sequences the integration and its "Things that will bite you" list is accurate.
  Landed steps 1-4 + 6:
  - `ui/theme/ForgeExpression.kt` — **three expressive axes** as one CompositionLocal: **Heat**
    (Quenched/Ember/Molten — re-tints the action colour; Ember IS AccentPrimary), **Energy**
    (Calm/Alive/Roaring — a motion multiplier; Calm is forced when `ANIMATOR_DURATION_SCALE == 0`,
    wired in `GymTrackerTheme`), **Surface** (Flat/Soft/Glass — blur radius + card alpha). Read via
    `LocalForge` / `ForgeExpression.current`.
  - `ui/components/ForgedMark.kt` — the canonical barbell mark + `ForgedWordmark`. Replaced the
    hand-ported `ForgedIcons.Barbell` (same geometry, one source now). Also
    `res/drawable/ic_forged_mark.xml` — its `android:tint="?attr/colorPrimary"` **fails to link in
    a Compose-only app** (no AppCompat theme attrs); changed to the literal ember, tint at use site.
  - `ui/components/ForgedSurfaces.kt` — `forgeGround()` / `forgeHero()` / `ForgeHairline()` /
    `ForgeSectionHeader()`. The law: **one `forgeHero()` per screen, everything else flat** (Home's
    hero, History's calendar, Plan's Up Next, Recovery's body map, Stats' chart — that is the
    complete list). Its `clickableRow` shipped as a stub; wired to the repo's real `forgedPress`.
  - **Home is now the "Dynamic Hub"** (`ui/screens/home/HomeHubScreen.kt`, stateless, takes a
    `HomeUi`): 2dp week rail → brand row → hero (RECOMMENDED FOR TODAY + readiness tag + Anton 30
    session name + glowing Start) → "Ready to train" rail → "Jump back in" rail → PR watch. This
    **replaces yesterday's flat-sections Home** and resolves INTEGRATION.md §6.7. `HomeScreen.kt`
    is now just the stateful wrapper that builds `HomeUi`.
  - Real bug found by the rail: "Ready to train" listed only muscles with freshness rows, i.e. the
    ones just trained, at 0%, under a heading claiming they were ready. `muscleFreshness()` only
    looks back 14 days, so an *absent* group is recovered, not unknown. Added
    `ProgressionImporter.CANONICAL_MUSCLES` and the VM now spans all 10, defaulting missing ones to
    100%. Same bug, same fix, in `RecoveryViewModel` — training only legs read as "0% ready to
    train" for the whole body.
  - Finished the rest of BUILD_ORDER the following pass: **step 5** (the regenerated anatomical
    map, `ui/components/body/`; it is slug-keyed and draws ONE side, so Recovery places Front and
    Back side by side — added `slugFreshness()` to expand the repo's 10 canonical groups onto its
    ~35 slugs; the old `MuscleBodyMap` stays because `MuscleTargetFigure` in Library still uses
    it), **step 7** (`SessionSlateScreen` is now the non-Strike session surface; it is a fixed
    3-part column that owns the screen, so `SessionTopBar`, the FOCUS MODE chip and the add-
    exercise FAB stand down while it shows, and chevrons in its header replace the exercise
    navigation the scrolling table used to give), and **step 8** (one `forgeHero()` each on Plan's
    Up Next, History's calendar, Recovery's body map, Stats' weekly chart).
  - **The three axes are live and persisted** — `repo.expression()`/`saveExpression()` in the same
    SharedPreferences as every other pref (not a second DataStore), hoisted to `MainActivity` so a
    change re-themes instantly, with a segmented control per axis under Settings → APP. Critically,
    `GymTrackerTheme` now derives `colorScheme.primary/onPrimary/primaryContainer` from
    `forge.palette`, so **Heat re-tints the whole app**, not just the handoff's own components.
    Ember's palette equals the shipped `AccentPrimary`, so the default is byte-identical.
  - **Fidelity pass against the v2 handoff's own reference PNGs (2026-08-08).** The earlier
    step-8 screens had been built from the *older* `design/redesign-2026-07/` prototype markup, so
    they drifted. Corrections, all sourced from `design_handoff_forged_android/README.md` §Screens
    + `screenshots/`:
    - **Anton is now the type scale**, not a per-screen opt-in: `Type.kt` puts it on
      displayLarge 44 / displaySmall 32 / headlineLarge 28 / headlineMedium 24 / titleLarge 20 and
      the screens use those roles. **Never set `fontWeight` on an Anton style** — one weight, it
      synthesises a fake bold.
    - **Body map reverted to the repo's v5 anatomy.** The handoff contradicts itself: BUILD_ORDER
      step 5 says copy its generated `body/MuscleBodyPaths.kt`, but README §Fidelity says that where
      IDENTITY_V5 and the prototype disagree on *body anatomy* "the repo spec wins", and §6 says
      build from `MuscleBodyMap.kt` "not the prototype's abstract bodyPaths.js". §Fidelity is the
      tie-breaker. `ui/components/body/` deleted.
    - **The Energy axis was dead code** — `motionScale` was computed and never read. `Motion.scale`
      now multiplies every settle/cool/plane duration (Calm → 0 = snap), and `ambientLoops` gates
      the rest-timer breath and confetti.
    - Screen fixes: **Plan** loses its hero — session rows carry state instead (mono eyebrow
      LOGGED·WED / TODAY / IN 2 DAYS, today = primaryContainer + 3dp accent rail, logged dimmed with
      an olive check, duration Anton 17sp over exercise count), sorted chronologically not by
      rotation order. **Recovery**: 42sp numeral, 10-segment readiness bar ramping COOLED→GLOWING,
      legend wording to `COOLED · READY` ←→ `GLOWING` (README calls the prototype's FRESH/FATIGUED
      stale), BY MUSCLE rows dot + name + bar + %. **Stats**: Week/Month/Year toggle with real
      period aggregation in the VM, 8 bars with **only the current week accent-filled** (volume is
      not a temperature, so no heat ramp), flat sections not a card. **Settings**: the whole spec —
      TRAINING (Units, Weight step), REST TIMER (Default rest stepper, 2 switches), APP (Theme,
      Haptics, the 3 axes), DATA, ABOUT — with a new `ui/components/SettingRow.kt` whose segmented
      controls are **inline text with an accent underline, not filled pills**, and 44×26dp switches.
      **History** groups by week under mono headers with a 44dp mono day column. **Library** filter
      chips to underline.
  - **The prototype's motion layer (2026-08-08).** Earlier passes read the prototype markup for
    *structure* only and never opened its CSS/JS animation layer, so several behaviours were
    missing. `Forged Prototype.dc.html` lines 20-34 hold the keyframes; the JS gates them on
    `const motionOn = energy !== 'Calm'`. What it actually specifies, and where it now lives:
    | Prototype | Status |
    |---|---|
    | `.tap:active{transform:scale(.96)}` 120ms | `forgedPress` 0.97 — README §Fidelity says prefer the codebase, so 0.97 stands |
    | `screenIn .34s` (opacity + 10px rise + .994 scale) | NavHost transitions, `Motion.settle` |
    | `muscFade .55s`, staggered `0.05 + i*0.045`s per muscle group | body map reveal, staggered 45ms |
    | `heatBreath` 0.62→1 opacity on hot muscles, staggered | body map, gated on `ambientLoops` |
    | `rowSettle .28s` scale 1.028→1 on the newly-active set | Slate `SetRow`, via `springMass` |
    | `checkPop .3s` scale .55→1.12→1 on a logged set | Slate e1RM badge, via `springMass` |
    | `ringGlow 1.7s infinite` — **Roaring only** | `Modifier.emberBloomPulsing` on the CTAs |
    | `Calm` → `.screen *{animation:none}` | `Motion.scale = 0` + `ambientLoops = false` |
    Its overshoot curves (`cubic-bezier(.2,.9,.25,1.05/1.15)`) are deliberately NOT ported —
    README says "add no new curves" and MOTION.md §5 forbids damping < 0.85, so `springMass`
    (damping 0.9) is the sanctioned equivalent.
    **Still not implemented**: `timerAlarm .9s` (rest overtime pulse) needs the rest timer to count
    negative, which `RestTimerService` does not yet do; and `floatY`/`shimmer` have no call site
    beyond the existing `SteelSheen`.
- **UX v6 adversarial hardening (2026-07-19, owner: "challenge everything before confirming")** — self-review found 4 real defects, all fixed + playtested: (1) widget vs notification PendingIntents shared an identity (matching ignores extras) → widget uses requestCode 2; (2) `standard` launchMode let a warm widget tap stack a second MainActivity with a second empty session VM → `singleTask` + `onNewIntent` (fire-up state lives on the activity, `removeExtra` guards rotation; consume-LAST in the LaunchedEffect — consuming first cancels the effect's own suspend work, which briefly regressed the deep link); (3) zero-set sessions were un-exitable (Finish only recoils) — now recoil + "Cold metal" dialog → `vm.discardSession()` (nothing saved), essential once entry is one gesture; (4) un-completing the last set stranded users out of Strike Mode (activeSetId stays null) → `activeStrike` falls back to the first incomplete set. Verified: cold + warm (onNewIntent) deep links land in Strike Mode; discard returns to Home with no phantom session.
- **UX v6 moves 2+3 SHIPPED (2026-07-19, owner: "continue implementing everything")**:
  - **Now Card** (Home leads with state): `LiveSessionCard` (AT THE ANVIL · ticking elapsed · sets struck · current exercise · one-tap resume; fed by `LiveSessionInfo` snapshot derived in MainActivity from the activity-scoped session VM) → `ForgedTodayCard` (SESSION FORGED: physique with today's muscles glowing, volume/sets/minutes via new `repo.todayForged()`, plan demoted below) → `PlanCard` (default; its IN PROGRESS variant removed — the live card owns that state now). Rest-day verdict intentionally merged into PlanCard's heat dots.
  - **One gesture in**: `MainActivity.EXTRA_FIRE_UP` — widget tap and the reminder notification's new "Fire it up" action both launch straight into the session (next program day auto-resolved, Strike Mode takes over); consumed-once guard vs rotation; if a session is already active it just returns to it. Verified via the exact intent (`am start --ez fire_up true`) → cold start landed in Strike Mode set 1.
  - **Race fix (pre-existing, exposed by progression's slower template build)**: `prepareStart`/`prepareRepeat`/`loadFreshSession` async state replacement was wiping `sessionActive` set by the screen's `markSessionActive()` — all three now preserve the current flag. This is why "IN PROGRESS" could silently fail to show.
  - Not live-tested: ForgedTodayCard render (needs a finished workout today; no workout-delete exists so testing would pollute real history — code-reviewed instead). Emulator launcher lost the widget after a crash (re-add by hand); widget deep link itself verified.
- **UX v6 move 1 — Strike Mode SHIPPED (2026-07-17, owner: "build move 1")**: `ui/screens/session/StrikeMode.kt` + splice in `WorkoutSessionScreen.kt`. Full-screen active set (resolved from `activeSetId`): Anton 96sp weight with horizontal scrub (2.5 kg detents, haptic tick per detent, detent width grows with load — springMass as feel), 56sp reps with vertical scrub, progression call as the set's title, context line (prev · e1RM · plates/side), single STRIKE surface (logs via `toggleCompleted`, rest auto-starts, AnimatedContent slides the next set in, `contentKey = set.id` so scrubs don't replay the transition). Table one swipe-up away (root vertical-drag >140dp) or via bottom TextButton; table mode gets a bottom-center STRIKE MODE chip back; add-exercise FAB hidden during strike; last set completing auto-surfaces the table for Finish. Playtested full loop on emulator (scrub both axes, strike→set 2, rest ring, table round-trip). Moves 2 (Now Card) + 3 (one-gesture-in) still open.
- **UX v6 direction — "First Strike" (2026-07-17, proposed, owner not yet committed)**: `design/UX_V6_FIRST_STRIKE.md`. Apple *product principles* (not Apple visuals — palette v3 rejection stands) fused with Molten Forge. Story: "pocket → first set in one gesture; during the set, one number at a time, no keyboard; everything else cools." Three moves in order: (1) **Strike Mode** — full-screen active set, giant Anton numerals, scrub-to-adjust with 2.5 kg haptic detents + springMass weighting, single STRIKE surface, table one swipe away; (2) **Now Card** — Home's hero morphs by state (plan day / mid-session / just finished / rest day); (3) **One gesture in** — widget & notification deep-link straight into Strike Mode set 1. Kill list documented (Liquid Metal reskin, full tab collapse, dial widget, voice logging). Concept sheet rendered; awaiting owner verdict before building.
- **Smart progression + machine notes (2026-07-17, owner picked #1+#2 from the "what's missing" list)**:
  - **Double progression** (`domain/Progression.kt`, pure+testable): program-day sessions compare the last two sessions' working sets against the day's rep range — all sets ≥ repMax → +2.5 kg (aim repMin); a sub-repMin set two sessions running at the same top weight → 5% deload rounded to 2.5; else hold with a reason. Plan computed in `sessionTemplateFromDay`; INCREASE/DELOAD repoint every set's *hint* (new `SessionSet.suggestedWeightKg/Reps`, `effective*` and hint-materialization fall back suggestion→prev; PREV column still shows last actuals). Exercise card shows the call as a colored line (increase=ember ▲, hold=steel →, deload=bronze ▼, forge voice). Non-program sessions unchanged (no range, no call).
  - **Machine notes**: `ExerciseEntity.note` (DB v3, migration 2→3) — sticky per-exercise note (seat/pin/grip) shown under the session card header, editable via card menu or tapping the note; persists via `repo.setExerciseNote`.
- **Launcher mark v2 — "Molten Dumbbell" (2026-07-17, owner: "emphasize a Gym app, be more divergent")**: replaces the v1 anvil. A 45° dumbbell fresh from the coals — warm-steel plates (two-tone for depth), bar glowing white-hot at center (vector gradient), gold PR spark + trailing ember kept from v1. Same furnace-gradient background and monochrome layer. `drawable/ic_launcher_foreground.xml`; candidates B "Loaded Sleeve" (plates end-on, ember core) and C "Kettlebell Crucible" were designed and rejected (B reads as a target at small size, C fussy when shrunk). Verified on emulator launcher.
- **RAG coach plan** (2026-07-17): `docs/RAG_PLAN.md` — Python/FastAPI RAG with free-tier LLMs (Groq → Gemini Flash fallback chain via OpenAI-compatible clients → rule-based offline), local sentence-transformers embeddings + FAISS flat + FTS5 hybrid retrieval, LangGraph router (knowledge→vectors, history→SQL tools, never text-to-SQL), CoachingCard schema cached to Room for offline. Build order §6; not yet implemented (Phase-4 gated).
- **Retention layer (2026-07-17, owner picked from the "what's missing" list)**:
  - **Launcher widget** (`widget/ForgeWidgetProvider.kt`, RemoteViews — no Glance dependency): dark iron plate, Anton streak number + gold flame, "Xd since last strike" tinted by `DarkHeat` (widget is always the night forge regardless of system theme). Data pushed on save/import/app-open via `requestUpdate()` (hooks in `WorkoutRepository.saveSession`/`importProgression` + MainActivity); 6h `updatePeriodMillis` as fallback. Tap opens the app. **"Add launcher widget" ActionCard in Data screen** uses `requestPinAppWidget` (the widget-picker drag is the only other path). Verified live on emulator: real data (7-week streak, 3d steel).
  - **Training reminder** (`service/TrainingReminderWorker.kt`, WorkManager periodic, anchored ~18:00 with `ExistingPeriodicWorkPolicy.KEEP` — UPDATE would re-shift the anchor on every app open and never fire): notifies only when has-data ∧ not-trained-today ∧ weekly-goal-unmet; copy = "The forge is cooling / {next day} is waiting — Nd since your last strike"; own channel `training_reminders` (opt-out lives there). MainActivity now requests POST_NOTIFICATIONS once on 13+ (also benefits the rest timer). Verified: fired with real data ("Lower 1 is waiting — 3 days").
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
- **Design handoff — "Forged redesign" bundle (2026-07-30, external design tool, added as reference only — not yet built)**: `design/redesign-2026-07/` holds a prototype handoff (README, clickable HTML prototype, 12 screenshots, `INTEGRATION.md`) plus `tokens/Dimens.kt`, copied into `ui/theme/Dimens.kt` — the one token file the repo was missing (Color/Type/Shape/Motion already existed; the handoff reuses them, no new colors/fonts/curves added). `INTEGRATION.md` maps every prototype value to an existing `com.gymtracker.ui.theme` symbol and was checked against real code before merging (not taken on faith — an earlier draft of this same handoff had a fabricated integration doc and a conflicting palette in a different package namespace, caught and rejected): `GlassBottomNav.kt` really does ship six tabs (Home · History · Plan · Library · Recovery("Body") · Stats) and `RestTimerService.kt` really uses `+15s`, not the 4-tab/+30s guesses the first draft made. Four open decisions flagged in `INTEGRATION.md` §6, to be settled before any Compose gets written: (1) **set tags** — W/D/N/T/F chips exist in `Color.kt` but the prototype has no set-tag UI; add or leave unused. (2) **body map** — follow `IDENTITY_V5.md`'s anatomical `MuscleBodyMap.kt` spec, not the prototype's older abstract `bodyPaths.js` shapes; take only layout/legend/BY MUSCLE list from the prototype. (3) **OneRmBadge** — prototype set rows show a plain "100 kg × 8" and are missing the heat-tinted e1RM badge the app already ships on every completed set. (4) **Home direction (biggest gap)** — shipped Home leads with `MuscleTargetFigure`/weekly ring/streak/hourglass; the prototype's Home leads with a Start CTA + week strip instead. No screens implemented from this bundle yet.
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
| 2026-08-19 | Sonnet 5 | **Lock-screen live rest timer — "Log set" + rich content, per `design_handoff_forged_android/screenshots/09-lock-live-timer.png`.** Owner pointed at 3 handoff screenshots (Stats, lock timer, Done) — Stats (`04-stats.png`) and Done (`10-done.png`) already matched the handoff (VolumeHeadline hero + PR list; FinishSummarySheet's mark/"forged."/stats row), so the only real gap was the notification. `RestTimerService`: `start()` now takes optional `setLabel`/`upNext` extras, shown as contentText + `BigTextStyle` ("Squat (Barbell) · set 2 of 4" / "Up next: 100 kg × 8") — kept the existing system chronometer for the countdown digits rather than a custom RemoteViews ring, since the code's own history documents that rebuilding the notification every second to redraw a custom view collapses the expanded card and makes actions unreachable (the exact bug `updateNotification()`'s comment already fixed once). Notification actions changed **Skip → Log set** (primary) + **+15s** (Skip stays reachable in-app via the existing rest sheet). New `RestTimerService.logSetRequests` `SharedFlow`, emitted on the action tap; `WorkoutSessionViewModel` collects it and completes the active set exactly like tapping its row (guarded against re-firing on an already-completed set). `toggleCompleted()` now computes the label/upNext for the *next* active set and passes it into `RestTimerService.start()`. Verified live on emulator: expanded notification showed "Romanian Deadlift · set 2 of 3 / Up next: 55 kg × 11", tapping **Log set** advanced to set 3/3 with a fresh countdown, and the in-app session screen reflected the logged set (55 kg × 11) with zero desync. 0 crashes. |
| 2026-08-07 | Sonnet 5 | **Identity v9 "Ember, Restored"** — reverted v8 Blue Hour's indigo primary back to v5's ember, after the owner checked the live Claude Design project behind `design/redesign-2026-07/` (via the `DesignSync` tool) and found its sync log named `IDENTITY_V5.md` as its source, not v8. Restored `Color.kt` from `git show d9c1dea` (the exact tree the design project synced against): ember primary, warm surfaces, separate olive `Success`, v5's `HeatScale` curve (v8's `ready/worn/hot/spent` field names kept so call sites didn't change). Fixed two real bugs surfaced by the revert: Home's readiness tag was always primary-colored regardless of its actual bucket; "This Week" done-checkmarks should read the `success` role, not primary (matches the mockup's olive checkmarks). Repainted launcher icon + widget XML (can't reference Kotlin constants). Wrote `design/IDENTITY_V9.md`. Not yet compiled/playtested — next step. |
| 2026-07-15 | — | **README.md**: project overview, shields.io + skillicons tech badges (Kotlin/Compose/Room/FastAPI/LangGraph/etc.), GitHub star/fork badges, star-history.com star counting diagram, features table, structure, build steps, mermaid architecture diagram |
| — | — | Project initialized |
| 2026-07-11 | Fable 5 (+ Sonnet subagent for Gradle/res boilerplate) | Repo scaffold + AGENTS.md/MEMORY.md/backend SKILL.md; UI extraction from 3 refs logged; theme (Color/Type/Shape/Theme); utils (OneRM, PlateCalculator, TimeFormat); WorkoutSessionScreen + ViewModel + DragNumberField/RestTimerBubble/PlateCalculatorPanel/ExercisePickerSheet/FinishSummarySheet; RestTimerService (FGS); HomeScreen placeholder + MainActivity nav; toolchain installed (JDK 17, Gradle 8.9 wrapper, SDK 35); `assembleDebug` GREEN → app-debug.apk |
| 2026-07-11 | Fable 5 | Owner manually tested session screen v1 ✓. Liquid-glass restyle: Haze 1.6.10 + BOM 2025.05.01, Glass.kt (GlassSurface/GlowBackground), glass top bar/FAB/rest bubble, translucent cards. Added: Home dashboard (weekly ring, streak, Mo–Su strip, quick-start, stat tiles — sample data), built-in stopwatch (top bar), warm-up ramp generator (exercise menu). `assembleDebug` GREEN |
| 2026-07-11 | Fable 5 | Crash fix + rebrand + emulator playtest. Root cause of "keeps stopping": stale incremental dex after BOM bump (`Composer.shouldExecute` NoSuchMethodError) → fixed by clean rebuild. Rebranded to **RepForge** + new adaptive icon. Added: glass bottom nav, ExerciseLibraryScreen, RecoveryScreen (muscle freshness), Home plan preview, activity-scoped session VM (Back-safe, Resume workout). Fixed: LocalContentColor in Glass containers (dark-mode black titles), theme-aware washes (light mode). Playtested end-to-end on API 37 emulator via adb (16 screenshots): set completion/1RM/rest bubble+notification/plate calc/tags/stopwatch/warm-up ramp/back-resume/finish/save — 0 crashes |
| 2026-07-15 | Fable 5 | **UI refresh per design/UI_REFRESH_PROMPT.md ("it's repetitive").** (1a) **Motion coverage is now ALL SIX TABS** — Plan/Library/Recovery/Stats wired into Motion.kt (previously only Home/History/session): `forgedEntrance` stagger on every tab's first entry (rememberSaveable, never replays), `forgedPress` on every tappable row via new `FlatRow`, Up Next button press physics; new `Motion.rollUpValue` (COUNT_UP=600 token) rolls History's month header (re-rolls on month page), Recovery's big stats + per-muscle %; new `ForgedBar` (linear Hot Tip fill) on Recovery muscle rows; charts already tempered (phase 2). (1b) **Hierarchy = hero card + flat lists**: new `components/FlatRow.kt` (dense row, hairline divider, press physics, no chrome) — History log, Plan programs, Library's 139 rows, Recovery by-muscle, Stats PR/most-trained all flattened; heroes stay glass (History calendar, Plan Up Next, Recovery body map, Stats charts w/ tightened ChartCard padding 16→12/13); Plan templates demoted to quiet translucent cards (no glass). (1c) **Clipping fixed**: History/Recovery/Stats/Plan bottom spacer 88→112 dp + navigationBarsPadding (Library's 130 contentPadding was already correct) — verified by scrolling every tab to true end, last row clears the nav pill. Fresh screenshots saved to screenshots/*.png. 0 crashes |
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
