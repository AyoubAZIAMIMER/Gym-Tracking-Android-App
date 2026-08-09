# Handoff: Forged — gym tracker redesign (Android)

## Overview
Forged is a workout logger built around one job: **log a set in one tap, know what to do next without thinking.**
The redesign covers the full loop — Home (today's session) → Session (set logging + rest) → Done, plus Plan,
Recovery, Stats, History, Exercise Library, Settings, and a lock-screen / ongoing-notification "live" rest timer.

Visual direction: near-black warm-charcoal surfaces, a single hot accent (ember orange), condensed display
numerals (Anton) against a neutral UI sans, mono micro-labels for section headers, and hairline dividers
instead of card stacks. Numbers are the hero; chrome is nearly invisible.

## About the design files
The files in `prototype/` are **design references created in HTML** — a clickable prototype of the intended
look and behaviour. They are **not production code to port**. Recreate these screens in Compose using the
app's existing theme: the prototype has been aligned so every color, type size, radius and curve in it
already exists in `com.gymtracker.ui.theme`. **Start with [INTEGRATION.md](INTEGRATION.md)** — it maps each
prototype value to its symbol and lists the six decisions to make before building.

Open `prototype/Forged Prototype.dc.html` in any browser (keep the sibling `support.js` and `bodyPaths.js`
next to it). Everything is clickable: Start → log sets → rest timer → next exercise → Done, plus the bottom
nav, the ⤢ button (lock-screen live timer) and the avatar (Settings).

## Fidelity
**High fidelity.** Values are measured from the prototype at a 390 × 820 dp viewport (CSS px = dp 1:1).
Recreate pixel-for-pixel, substituting platform behaviour where noted (system status bar, edge-to-edge
insets, haptics) and **preferring the codebase where the two disagree** — e.g. press compression is
`forgedPress`'s 0.97, not the prototype's 0.96. Where `design/IDENTITY_V5.md` and the prototype disagree
(body map anatomy, legend wording), **the repo spec wins**; INTEGRATION.md §6 lists every such case.

---

## Design tokens

**Integrating into Kotlin? Start with [BUILD_ORDER.md](BUILD_ORDER.md)** — it lists the eight
drop-in Compose files in `kotlin/` and the order to add them.

**All tokens already exist in the app.** See **[INTEGRATION.md](INTEGRATION.md)** for the full
prototype → `com.gymtracker.ui.theme` mapping. The short version:

- **Color** — `Color.kt` / `Theme.kt`. Ink `#0E0D0B` bg · SurfaceDark `#1A1815` · SurfaceRaised `#242019`
  · OutlineFaint `#272319` · OutlineDark `#35302A` · AccentPrimary (ember) `#FF5A1F` · AccentPrimaryDim
  `#3A1608` · AccentPrimaryBright `#FFB294` · OnAccentPrimary `#1C0800` · TextPrimary `#F2EDE4` ·
  TextSecondary `#A89F91` · TextHint `#6B6357` · Success `#B8C77A` · PrGold `#FFC93C`.
  Temperature hues come from `GymTheme.colors.heat.at(freshness)` (steel `#8FB4C7` → bronze `#D08A45`
  → ember → red `#FF3320`) — **never hand-picked**, per `design/IDENTITY_V5.md`.
- **Type** — `AppTypography` in `Type.kt`. Anton (`Forge`, one weight) for displayLarge 44 / displaySmall 32
  / headlineLarge 28 / headlineMedium 24 / titleLarge 20; system sans for titleMedium 16 → bodySmall 12;
  mono micro labels = `labelSmall` 11 / tracking 1.2. Numerals get `FONT_FEATURE_TABULAR`.
- **Shape** — `AppShapes`: 8 / 12 / 16 / 24 / 28. Nothing off-scale except intentional pills.
- **Motion** — `Motion.kt` + `design/MOTION.md`: `forgedPress`, `forgedEntrance`, `rollUpValue`,
  `Motion.settle/cool/plane`, springs with damping ≥ 0.85. Add no new curves.
- **Spacing** — the one gap; `kotlin/ui/theme/Dimens.kt` in this bundle fills it.
  Screen padding 22 · row padding 13 · section 18/10 · nav 64 tall, inset 14 · list bottom spacer 112
  · CTA 60 · stepper 38 visual / 48 touch · session bottom bar 228 · scroll fade 22.

Ambient warmth (unchanged from the app's identity): one radial ember glow per screen at
`50% -8%`, ~10–13% alpha, plus a 3 dp dot-grid noise overlay at `#FFF0E1` 3.5%.

---

## Screens

### 1. Home — `data-screen-label="Home"`
**Purpose:** answer "what am I doing today" and start it in one tap.
Layout, top to bottom:
1. 2 dp week-progress rail pinned to the very top (accent fill = sessions done / planned).
2. Status bar row, 46 dp.
3. Brand row: 24 dp barbell mark + "FORGED" (Anton 15 sp, tracking +1.5), right side date caption +
   32 dp circular avatar (opens Settings).
4. **Today block** — micro label "TODAY" / right-aligned readiness tag ("LEGS FRESH", accent);
   session name in Anton 34 sp; meta caption "6 exercises · about 58 min"; then a row with the primary
   **Start** CTA (flex 1, 60 dp tall, radius 16, accent fill, `onAccent` label Anton 20 sp, glow
   `0 0 26 rgba(accent,.45)`) and a 60 dp square secondary "swap session" button (1 dp `hairlineStrong`).
5. **THIS WEEK** — 7 weekday cells, 34 dp square, radius 16: done = green check on `rgba(184,199,122,.14)`;
   today = accent border + `accentContainer`; future = 1 dp hairline outline; skipped = dashed hairline.
   Right side "2 / 4" (Anton 15 sp, accent numerator).
6. **NEXT UP** (link "Full plan" → Plan) — two rows: name + muscle caption, right day + chevron.
7. **RECENT** (link "All history" → History) — session name, day · duration, right total volume in Anton.
8. Floating bottom nav.

### 2. Session — `data-screen-label="Session"`
The screen you stare at between sets. Fixed 3-part column: header (never scrolls) · scrolling middle ·
bottom logging bar (never scrolls). The scroll region fades out over its last 22 dp (mask gradient) so
clipped rows read as "more below".

- **Header:** 2 dp exercise-progress rail; status bar; then row: back ←, elapsed timer (Anton 23 sp),
  rest pill (appears only while resting: `accentContainer` fill, radius 999, Anton 15 sp countdown),
  spacer, ⤢ (shows the lock-screen live timer).
- **Exercise header:** 46 dp rounded-square index badge (radius 14, `accentContainer`, accent Anton 18 sp),
  name (19 sp/700), muscle caption, ⋮ overflow.
- **TODAY / "1 / 4"** micro-label row, then one row per set:
  - *todo*: grey index numeral (Anton 17 sp, `textLow`), "Set n" 15.5 sp, ⋮ at right.
  - *active*: `accentContainer` fill, 3 dp accent left bar, accent index, bold label, "NOW" mono 9.5 sp.
  - *done*: 22 dp green check chip, label `textMid`, right "100 kg × 8" (Anton 17 sp value + 11 sp unit).
  - "＋ Add set" row at the end.
- **LAST · <date>** block: previous session's sets, same numeral treatment, right-aligned "6 days ago".
- **Bottom bar (228 dp):** EFFORT micro label + 5-bar RPE selector (7 × 14 dp bars, filled = accent) +
  effort word ("Easy…All out", or "Not set"); divider; two steppers side by side ("WEIGHT KG" / "REPS",
  − / value Anton 42 sp / +, separated by a 1 dp vertical rule); then a row: 56 dp secondary "notes" button
  and the **Complete set** CTA (flex 1, radius 16, accent, Anton 20 sp, 17 dp padding, accent glow).

**Log-set behaviour:** mark active set done with the current weight/reps/effort → advance to the next
incomplete set → start rest at the default (90 s) → if the exercise is finished, advance to the next
exercise (supersets marked A1/A2 skip rest and go straight to the pair) → when nothing is left, go to Done.

### 3. Rest states
- **In-app, other tabs:** a 48 dp "resting" strip floats 86 dp from the bottom (above the nav): pulsing dot
  in the timer color, "1:28 · Resting · Squat (Barbell)", "Resume ›". Tapping it returns to Session.
- **Timer ring:** background track `rgba(255,255,255,.09)`, progress stroke = `cool #8FB4C7` above 30 s,
  `accent #FF5A1F` at ≤ 30 s, `accentHot #FF3320` in overtime, 5 dp outer glow; centre shows mm:ss
  (Anton, 27 % of ring size) over a "REST"/"OVERTIME" mono caption.
- **Lock screen / ongoing notification (`isLock`):** implement as a **foreground service + ongoing
  notification** (and a Wear/AOD-friendly compact form). Compact chip: timer + brand mark. Expanded:
  brand row + "live" tag, 76 dp ring, "Rest · 1:28", "Squat (Barbell) · set 2 of 4", "Up next: 100 kg × 8",
  and two actions — **Log set** (accent filled) and **+15s** (outlined — matches `RestTimerService`'s existing
  `ADD_15` notification action; never +30s). Tapping the body opens the app to Session.

### 4. Done
Centered celebration: barbell mark, "<Session> forged." in Anton 34 sp on two lines, summary caption
"6 exercises · 24 sets · 16,500 kg moved", then three stats in a 26 dp gap row — new PRs (gold),
minutes (textHi), "▲9% vs last" (green) — and a pill **Done** button (accent, radius 999, 130 dp wide).
Copy is singular-aware ("1 set", "1 minute").

### 5. Plan
Screen title + program name; THIS WEEK strip (same component as Home); **SESSIONS** list of 4 rows —
logged (dimmed, green check at right), today (`accentContainer` fill + accent left bar + "TODAY" micro
label), upcoming ("IN 2 DAYS"). Each row: name 15.5/600, muscles caption, right duration (Anton 17 sp) +
exercise count. Footer row "Exercise library / Browse and swap movements ›".

### 6. Recovery
READINESS block: "70%" (Anton 42 sp with a 20 sp "%"), "ready to train", one-line note, then a 10-segment
readiness bar along the heat scale. Below: front/back body maps with per-muscle heat tints, a heat-scale
legend strip (**`COOLED · READY` ←→ `GLOWING`** per v5 — the prototype's FRESH/FATIGUED wording is stale),
then a **BY MUSCLE** list: dot, name, mini bar, right percentage. Build the figures from
`IDENTITY_V5.md` §2 (real physique, `MuscleBodyMap.kt`), **not** from the prototype's abstract
`bodyPaths.js`; no % pills on the body.

### 7. Stats
Screen title + Week/Month/Year segmented toggle (underline indicator, accent). VOLUME THIS WEEK
"48,210 kg" (Anton 42 sp + 15 sp unit) with "▲12%" delta; three inline stats (sessions / PRs / hours);
WEEKLY VOLUME bar chart, 8 bars, radius 6, current week accent-filled and glowing; a sparkline card
("SQUAT · EST. 1RM", accent 2 dp stroke, end dot, right-aligned current value); PERSONAL RECORDS list
(gold star, lift name, delta, value).

### 8. History (tab)
Two headline stats (sessions this week / total lifted), then sessions grouped by week — group headers are
mono micro labels ("SESSIONS", "LAST WEEK"); each row: 44 dp mono day column, name (+ gold star if it
contained a PR), duration caption, right volume in Anton.

### 9. Exercise Library (tab)
Title + live count ("12 exercises" in the prototype's sample; the real catalog is 108 curated / ~139 rows —
build it as a `FlatRow` directory, never a card gallery), filter chips (All / Legs / Push / Pull / Core — active = accent underline),
then rows: name 15.5/600, "muscle · equipment" caption, right e1RM value (Anton 17 sp) over a mono "e1RM" tag.

### 10. Settings
Profile row (52 dp avatar, name, "7-week streak · since Mar 2026", chevron), then grouped rows under mono
headers: **TRAINING** (Units kg/lb segmented, Weight step 1.25/2.5/5, Plates in your gym chips),
**REST TIMER** (Default rest − 1:30 +, Start on logged set switch, Alert when rest ends switch),
**APP** (Theme Dark/Light/Auto), **DATA**, **ABOUT** (Version 1.4.2, Privacy & terms).
Row anatomy: title 15.5/600, optional 12.5 sp helper line, control right-aligned. Switches are 44 × 26 dp,
accent when on, `surfaceChip` when off.

---

## Interactions & behaviour
- **Navigation:** bottom nav = **Home · History · Plan · Library · Recovery · Stats** — six tabs, matching
  `GlassBottomNav.kt` exactly (20 dp icons, `labelMedium`, active = primary icon+label on a primary-@18%
  20 dp rounded pad, bar radius 32). Session, Done, workout detail, exercise stats and Settings are pushed
  destinations **without** the nav bar; History and Library are tabs and carry **no back arrow**.
- **Tap feedback:** 0.96 scale, 120 ms — applied to every interactive surface (rows, chips, CTAs, steppers).
- **Steppers:** weight ± the user's step (1.25 / 2.5 / 5 kg), clamped ≥ 0; reps ± 1, clamped 1…50.
  Long-press to repeat is expected on Android.
- **Rest timer** counts down from the user's default (90 s, honored from the Progression import) and keeps
  counting **negative** (overtime, "+0:14"). `service/RestTimerService.kt` already provides the foreground
  service + ongoing notification with `START`/`ADD_15`/`STOP`; extend it rather than writing a new one.
- **Haptics:** light tick on stepper, medium on Complete set, double on rest end.
- **Insets:** design assumes a 46 dp status band and a floating nav 14 dp above the bottom edge — draw
  edge-to-edge and add `WindowInsets` to those values rather than hard-coding.

## State model (minimum)
```
screen                      // home | session | recovery | stats | history | settings | plan | library | done | lock
session: { exerciseIndex, logs: Map<exerciseIndex, Set[]>, elapsedSeconds }
Set     : { status: todo|active|done, weight, reps, effort: 1..5|null }
draft   : { weight, reps, effort }          // what the steppers edit
rest    : { active, secondsRemaining, paused, defaultSeconds }
prefs   : { unit: kg|lb, step: 1.25|2.5|5, restDefault, autoStartTimer, alertOnRestEnd, haptics, theme }
```
Timer ticks once per second and drives: rest countdown (when active && !paused) and session elapsed
(when a session is running, including while the lock-screen/notification timer is showing).

## Assets
- **Barbell logo mark** — shipped as code: `kotlin/ui/components/ForgedMark.kt` (Composable) and
  `res/drawable/ic_forged_mark.xml` (vector drawable). Geometry on a 32 × 32 grid: bar 9,14.25 14×3.5 r1.5 ·
  inner plates 6.5/22, 9.5, 3.5×13 r1.6 · outer plates 3/26, 12, 3×8 r1.4.
- **Nav + UI icons** — stroked 24 × 24 paths, 1.9 dp stroke, round caps. Path data in the prototype logic
  (`iconP`): home, calendar, pulse, chart, clock, bars. Swap for your existing icon set if you have one.
- **Body maps** — shipped as code: `kotlin/ui/components/body/MuscleBodyPaths.kt` (35 named muscle groups,
  front + back, generated from `prototype/bodyPaths.js`) rendered by `MuscleBodyMap.kt`, tinted per muscle
  at runtime from `heat.at(freshness)`.
- **Fonts** — Anton is already bundled (`res/font/anton.ttf`, `licenses/OFL-Anton.txt`). Nothing to add.
- No raster images are used anywhere in the design.

## Screenshots
Reference renders of every screen (`screenshots/`), captured from the prototype after it was aligned to the
app's theme — so the hexes you sample out of these PNGs are the ones in `Color.kt`. Defaults shown:
Ember accent · Glass surface · Alive motion · Dark theme.

| File | Screen |
|---|---|
| `01-home.png` | Home — today's session, week strip, next up, recent |
| `02-plan.png` | Plan — week strip + session list + library entry |
| `03-recovery.png` | Recovery — readiness, body maps, by-muscle list |
| `04-stats.png` | Stats — volume, weekly bars, e1RM sparkline, PRs |
| `05-history.png` | History — sessions grouped by week |
| `06-library.png` | Exercise library — filter chips + e1RM rows |
| `07-session-logging.png` | Session — set list + effort + steppers + Complete set |
| `08-session-resting.png` | Session — one set logged, rest pill counting down |
| `09-lock-live-timer.png` | Lock screen / ongoing-notification live rest timer |
| `10-done.png` | Done — session summary |
| `11-settings.png` | Settings — profile, training, rest timer, app |
| `12-settings-scrolled.png` | Settings — data & about groups |

## Files in this bundle

- `BUILD_ORDER.md` — **the Kotlin integration guide.** Eight drop-in Compose files, in order,
  with what each depends on and what will bite you. Read after INTEGRATION.md.
- `kotlin/` — drop-in Compose source, paths mirroring `com.gymtracker`:
  `ui/theme/Dimens.kt`, `ui/theme/ForgeExpression.kt` (Heat / Energy / Surface),
  `ui/components/ForgedMark.kt` (barbell mark), `ui/components/ForgedSurfaces.kt`,
  `ui/components/body/MuscleBodyPaths.kt` + `MuscleBodyMap.kt` (35 named muscle groups),
  `ui/home/HomeHubScreen.kt`, `ui/session/SessionSlateScreen.kt`.
- `res/drawable/ic_forged_mark.xml` — the barbell mark as a vector drawable.
- `prototype/Forged Prototype.dc.html` — clickable prototype (open in a browser; keep siblings).
- `prototype/Forged Redesign.dc.html` — the static screen-by-screen exploration board.
- `prototype/support.js`, `prototype/bodyPaths.js` — runtime + body-map path data.
- `INTEGRATION.md` — prototype → `com.gymtracker.ui.theme` mapping, the six-tab nav spec, the `FlatRow` /
  `ForgedBar` / `ForgedRing` components to reuse, and the eight decisions to make first. **Read it first.**
- `prototype/Forged Redesign.dc.html` also carries the later exploration turns: the Slate and
  one-decision Home layouts, the superset session moment, the two Complete screens, the program
  picker, and the Live Activity rest timer.

## Known gaps / decisions for the team
1. **Accent is settled: Ember `#FF5A1F`** (= `AccentPrimary`, already the app's primary) — every screenshot
   and the prototype default use it. `#FF3320` is only `heat.red` (overtime / just-trained) and `#8FB4C7`
   only `heat.steel` (resting / recovered). No second brand accent.
2. Stepper visuals are 38 dp — the touch target must be padded to 48 dp.
3. Effort (RPE) is optional per set in the prototype; decide whether to require it before Complete set.
4. Supersets are modelled only as an A1/A2 pairing hint — no editor UI exists yet.
5. Empty states (no program, first session, no history) are not designed.
