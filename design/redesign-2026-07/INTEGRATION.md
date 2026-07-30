# Integration map — prototype → RepForge code

**Read this before writing any Compose.** The prototype now uses **only values that already exist in
`com.gymtracker.ui.theme`**. Nothing here asks you to add a new color, font, or curve. If you find
yourself typing a hex literal into a screen, you've gone off-map — go back to the token.

Repo: `AyoubAZIAMIMER/Gym-Tracking-Android-App` @ `master` · theme at
`android/app/src/main/java/com/gymtracker/ui/theme/`

---

## 1 · Color — every prototype value → existing symbol

Screens must read these through `MaterialTheme.colorScheme` / `GymTheme.colors`, never from `Color.kt` directly
(the header in `Color.kt` says so).

| Prototype (dark) | Symbol | ColorScheme / GymTheme slot |
|---|---|---|
| `#0E0D0B` screen bg | `Ink` | `colorScheme.background` / `.surface` |
| `#1A1815` session bottom bar, cards | `SurfaceDark` | `colorScheme.surfaceContainer` |
| `#242019` steppers, chips, avatar, plate tags | `SurfaceRaised` | `colorScheme.surfaceVariant` |
| `#272319` hairlines, row dividers | `OutlineFaint` | `colorScheme.outlineVariant` |
| `#35302A` bottom-bar top edge, secondary button border | `OutlineDark` | `colorScheme.outline` |
| `#FF5A1F` accent, CTA fill, active rail | `AccentPrimary` | `colorScheme.primary` |
| `#3A1608` active-set fill, badges, rest strip | `AccentPrimaryDim` | `colorScheme.primaryContainer` |
| `#FFB294` "NOW" tag, on-container text | `AccentPrimaryBright` | `colorScheme.onPrimaryContainer` |
| `#1C0800` label on the accent CTA | `OnAccentPrimary` | `colorScheme.onPrimary` |
| `#F2EDE4` titles, values | `TextPrimary` | `colorScheme.onSurface` |
| `#A89F91` labels, captions, secondary values | `TextSecondary` | `colorScheme.onSurfaceVariant` |
| `#6B6357` units, timestamps, todo numerals | `TextHint` | `GymTheme.colors.hint` |
| `#B8C77A` completed sets, done days | `Success` | `GymTheme.colors.success` |
| `#FFC93C` PR stars only | `PrGold` | `GymTheme.colors.prGold` |
| `#8FB4C7` resting timer, FRESH legend, readiness | `DarkHeat.steel` | `GymTheme.colors.heat.at(1f)` |
| `#D08A45` mid-recovery | `DarkHeat.bronze` | `heat.at(~0.45f)` |
| `#FF3320` overtime, FATIGUED legend | `DarkHeat.red` | `heat.at(0f)` |
| nav glass fill / hairline | `GlassTintDark`, `GlassOutlineDark`, `GlassHighlightDark` | `GymTheme.colors.glass*` |

**Light theme** uses the repo's daylight-workshop tokens verbatim: `PaperLight #F4EFE6` bg,
`SurfaceRaisedLight #EDE5D6`, `SurfaceLight #FDFAF3`, `TextPrimaryLight #1B150E`,
`TextSecondaryLight #6C6152`, `TextHintLight #A19684`, `OutlineLight #DCD2BF`,
`AccentContainerLight #FFDCC9`, `OnAccentContainerLight #571B00`. In light mode the accent deepens to
`AccentPrimaryLight #C63D08` and heat uses `LightHeat` — the prototype's Light mode shows this.

### Heat is data — the rule you can get wrong
`forgeHeat`/`heat.at(freshness)` is the **only** way a screen picks a temperature hue.
freshness `1f` = fully recovered = quenched steel; `0f` = just worked = glowing red.
Applies to: Recovery (readiness figure, body-map muscle tints, legend), rest timer stroke,
Stats effort-encoding bars. It does **not** apply to the week strip, nav, or completed-set checks —
those are fixed roles (`Success`, `primary`).
Fixed: gold is PRs only; olive is "set completed"; `ActivityPink/ErrorRed #FF4B36` is errors and failure sets.

## 2 · Typography — prototype sizes → `AppTypography` slots

Anton is `Forge`, one weight — never apply a bold `fontWeight` to it. Add
`fontFeatureSettings = FONT_FEATURE_TABULAR` to every timer, weight, volume and stat numeral.

| Prototype | Slot | Value |
|---|---|---|
| stepper numerals (44) | `displayLarge` | Forge 44/48, +0.5 |
| Done headline (32) | `displaySmall` | Forge 32/36, +0.5 |
| screen titles (28) | `headlineLarge` | Forge 28/34, +0.5 |
| session elapsed timer (24) | `headlineMedium` | Forge 24/30, +0.5 |
| exercise name, kg values (20) | `titleLarge` | Forge 20/26, +0.4 |
| row titles (16 bold) | `titleMedium` | sans Bold 16/22 |
| body / subtitles (14) | `bodyMedium` | sans 14/20 |
| captions (12) | `bodySmall` | sans 12/16 |
| mono micro labels (11, tracking 1.2) | `labelSmall` | Medium 11/14, +1.2 |

Two prototype numerals fall between slots — set-row values and list-row volumes render at Anton 17.
Use `titleLarge` (20) if you want to stay purely on the scale, or copy it down to 17.sp locally; don't
add a new global slot for it.

## 2b · Bottom nav — six tabs, not four
`GlassBottomNav.kt` is the source of truth and it ships **six** tabs in this exact order:

```
Home · History · Plan · Library · Recovery · Stats
Icons.Rounded: Home · History · CalendarMonth · FitnessCenter · MonitorHeart · BarChart
```

The prototype now matches. Measured values, all from `GlassBottomNav.kt`:
bar = `GlassSurface` @ `RoundedCornerShape(32.dp)` (**32, not `AppShapes.extraLarge` 28** — the nav is
deliberately off-scale), row padding 8 h / 8 v, item gap 2, item pad 7 h / 8 v, item radius **20**,
icon 20 dp, label `labelMedium`, active tint `colorScheme.primary` on a `primary @ 18% alpha` fill,
inactive `onSurfaceVariant`, both cross-fading on `Motion.plane(Motion.FAST)`.
Six labels are tight on a 390 dp phone (the code comment notes it was tuned for 412) — keep the label at
`labelMedium` and let the items flex; don't drop to 4 tabs and don't abbreviate "Recovery".

**Consequence for the redesign:** History and Library are **top-level tabs**, so they carry no back arrow.
Only Session, Done, Workout detail, Exercise stats, Settings/Data are pushed destinations (no nav bar).
Home's "All history" / "Full plan" / "Exercise library" links are tab jumps, not pushes.

## 2c · Screens already have a flat-list vocabulary — use it
`components/FlatRow.kt` exists precisely for the prototype's hairline rows: `verticalPadding = 11.dp`
default, `horizontal = 4.dp`, 1 dp `outlineVariant @ 55% alpha` divider, `forgedPress(pressedScale = 0.99f)`,
clipped to `shapes.small`. **Every list row in the redesign is a `FlatRow`** — Home's next-up/recent,
Plan's programs, Library's ~139 exercise rows, Recovery's by-muscle, History's log, Stats' PR list.
Reserve `GlassSurface` for the one hero per screen (History calendar, Plan "Up Next", Recovery body map,
Stats charts). The prototype's rows use 13 dp padding; **prefer FlatRow's 11 dp** rather than re-tuning it.

Progress bars and rings are also already built: `ForgedBar` (linear Hot Tip fill) for Recovery's per-muscle
bars and the week rail, `ForgedRing` for the rest timer and Home's weekly goal, `Motion.rollUpValue` for
every stat numeral. Do not hand-roll any of these.

## 3 · Shape — all radii are on `AppShapes`
`extraSmall 8` plate tags · `small 12` steppers, set rows, input fields · `medium 16` exercise badge,
CTAs, weekday cells, rest strip · `large 24` big cards · `extraLarge 28` bottom nav.
Pills (Done button, rest pill) are `RoundedCornerShape(50)` / `CircleShape` — intentional, off-scale.

## 4 · Motion — use `Motion.*`, add nothing
`design/MOTION.md` is canonical and already covers everything the prototype does. Mapping:

| Prototype motion | Use |
|---|---|
| screen enter (fade + rise) | `Modifier.forgedEntrance(index, played)` — 8 dp rise, 30 ms stagger, cap 8 |
| tap compression | `Modifier.forgedPress(interactionSource)` — 0.97 buttons, 0.985 large cards |
| session/exercise progress fill | `Motion.settle(Motion.DELIBERATE)` |
| set-row settle on log | `Motion.springFirm()` — damping ≥ 0.85, never wobble |
| check pop | `Motion.settle(Motion.STRIKE)` |
| rest ring tick | `tween(1000, easing = LinearEasing)` |
| ring color cross-fade | `Motion.settle(Motion.SLOW)` |
| overtime pulse | `Motion.Temper`, ~900 ms (`Motion.FORGE`), ambient loop only |
| stat numbers on entry | `rollUpValue(target)` — capped `Motion.COUNT_UP` |
| screen nav | `Motion.DELIBERATE` in, ×0.7 out (`Motion.cool()`) |

The prototype's **Calm** energy setting = reduce-motion. Honor `ANIMATOR_DURATION_SCALE`.
The prototype's press scale was 0.96; **the codebase's 0.97 wins** — don't change `forgedPress`.

## 5 · The one file this bundle adds
`tokens/Dimens.kt` — the repo has `Color`/`Type`/`Shape`/`Motion` but no spacing scale, and the
prototype's layout depends on consistent padding. Drop it in `ui/theme/` if you want it; otherwise the
numbers are all in `../README.md`.

## 6 · Gaps between prototype and repo — decide before building
1. **Set tags** — `Color.kt` defines W/D/N/T/F chips (`TagWarmup`…`TagFailure`). The prototype has **no**
   set-tag UI. Either add the chips to the set row or leave the tokens unused; don't invent a third pattern.
2. **Body map** — `IDENTITY_V5.md` §2 specifies a real human physique in `MuscleBodyMap.kt` (100×170 space,
   named bellies, halo above ~35% heat, **no % pills on the body**). The prototype's `bodyPaths.js` is the
   older abstract map. **Follow the repo spec, not the prototype's shapes** — take only the layout, legend,
   and BY MUSCLE list from the prototype.
3. **Readiness copy** — v5 legend wording is `COOLED · READY` ←→ `GLOWING`; the prototype still says
   `FRESH` ←→ `FATIGUED`. Use the v5 wording.
4. **Lock-screen timer** — `service/RestTimerService.kt` already exists (foreground service + ongoing
   notification, `START` / `ADD_15` / `STOP` intents). Its notification action is **`+15s`**, so the
   prototype's lock screen now says +15s too — do not ship a +30s button. What's missing is only the
   richer expanded layout the prototype shows (ring, "set 2 of 4", "Up next", **Log set** action). Note
   `MEMORY.md`: an in-progress session survives Back/app-switch but **not** a process kill — a lock-screen
   Log set action has to survive process death, so this needs session persistence first.
5. **Effort/RPE** — optional per set in the prototype; decide whether Complete set requires it. Note the app
   already stores `SessionSet.intensity` (e1RM ÷ all-time best) and renders a heat-tinted `OneRmBadge` on
   every completed set — the prototype's set rows show a plain "100 kg × 8" and are **missing that badge**.
   Add it: warm-ups read steel, top sets glow, gold ★ is PR-only.

7. **Home has diverged.** The shipped Home (per `MEMORY.md` v5) leads with `MuscleTargetFigure` (170 dp
   anatomical figure showing today's targets glowing), a `ForgedRing` weekly-goal ring, a flame streak tile,
   a cooling "Since last strike" hourglass, and a "Hi, {name}" greeting from the first-run profile. The
   prototype's Home instead leads with a Start CTA + week strip and has none of those. **Decide which Home
   wins before building** — this is the biggest gap in the bundle, not a detail.

8. **Weight step** — prototype offers 1.25 / 2.5 / 5 kg; the shipped default is ±2.5 kg (owner-approved as
   "smallest standard plate pair"). Keep 2.5 as the default option.
6. **Empty states** — not designed (no program, first session, no history).
