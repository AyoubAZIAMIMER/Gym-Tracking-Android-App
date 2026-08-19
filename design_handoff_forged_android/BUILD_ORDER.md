# Build order — drop the redesign into the Kotlin app

You're integrating into `AyoubAZIAMIMER/Gym-Tracking-Android-App` @ `master`, package
`com.gymtracker`. Everything below is **UI layer only** — no ViewModel, Repository, Room or
service logic changes. Every screen composable here is stateless and takes a UI model, so you
wire it to the existing ViewModel and delete nothing.

Read [INTEGRATION.md](INTEGRATION.md) once before step 1. It maps every prototype value to a
symbol that already exists in `ui.theme` and lists the decisions to make first.

---

## What's in `kotlin/`

Paths mirror the repo, so you can copy each file to
`android/app/src/main/java/com/gymtracker/<same path>`.

| File | What it is | Depends on |
|---|---|---|
| `ui/theme/Dimens.kt` | The spacing scale the repo doesn't have. `Dim.*` | — |
| `ui/theme/ForgeExpression.kt` | Heat / Energy / Surface as one `CompositionLocal`. The three expressive axes. | — |
| `ui/components/ForgedMark.kt` | The barbell mark + FORGED lockup. Replaces the crossed-diamond spark. | — |
| `ui/components/ForgedSurfaces.kt` | `Modifier.forgeGround()`, `Modifier.forgeHero()`, `ForgeHairline()`, `ForgeSectionHeader()` | Dimens, ForgeExpression |
| `ui/components/body/MuscleBodyPaths.kt` | Generated anatomical path data, 35 named muscle groups, front + back. | — |
| `ui/components/body/MuscleBodyMap.kt` | Canvas body map, tinted per muscle by freshness. `HeatLegend()`. | MuscleBodyPaths |
| `ui/home/HomeHubScreen.kt` | Home — the Dynamic Hub direction you picked. Hero + rails. | all of the above |
| `ui/session/SessionSlateScreen.kt` | Session — The Slate. Header / set list / logging bar. | all of the above |

Plus `res/drawable/ic_forged_mark.xml` — the same barbell mark as a vector drawable, for the
launcher icon, the ongoing rest notification, and anywhere you need a `Drawable` not a Composable.

## Order

**1 · Tokens.** Copy `ui/theme/Dimens.kt`. Nothing else in `ui/theme` changes — `Color.kt`,
`Type.kt`, `Shape.kt`, `Motion.kt` already carry every value the redesign uses.

**2 · Expression.** Copy `ui/theme/ForgeExpression.kt` and provide it at the top of your theme:

```kotlin
CompositionLocalProvider(
    LocalForge provides ForgeExpressionState(
        heat = prefs.heat,        // default Heat.Ember  — this IS AccentPrimary
        energy = prefs.energy,    // default Energy.Alive
        surface = prefs.surface,  // default SurfaceStyle.Soft
        dark = isDark,
    )
) { GymTheme { content() } }
```

Persist the three in DataStore next to the existing prefs and expose them in Settings under
**APP**, below Theme. `Energy.Calm` must also be forced on when `ANIMATOR_DURATION_SCALE == 0`.

**3 · Mark.** Copy `ForgedMark.kt` and `res/drawable/ic_forged_mark.xml`; swap the old spark
everywhere it appears (Home header, Done screen, rest notification, launcher icon).

**4 · Surfaces.** Copy `ForgedSurfaces.kt`. Then the rule that fixes the "every tab looks the
same" problem: **one `forgeHero()` per screen, everything else flat.** Home's hero, History's
calendar, Plan's Up Next, Recovery's body map, Stats' chart — that's the complete list. Every
other row is the existing `FlatRow`.

**5 · Body map.** Copy both files under `ui/components/body/`. Call it with your readiness data:

```kotlin
MuscleBodyMap(
    side = BodySide.Front,
    freshness = readiness,                  // slug -> 0f just worked .. 1f cooled
    heatAt = { GymTheme.colors.heat.at(it) },
    silhouette = MaterialTheme.colorScheme.surfaceVariant,
    outline = MaterialTheme.colorScheme.background,
)
```

Slugs are stable: `chest, obliques, abs, biceps, triceps, trapezius, deltoids, adductors,
quadriceps, tibialis, calves, forearm` (front) and `trapezius, deltoids, upper-back, triceps,
lower-back, forearm, gluteal, adductors, hamstring, calves` (back). `head, hair, hands, feet,
ankles, knees, neck` draw as silhouette and are never tinted.

**6 · Home.** Copy `HomeHubScreen.kt`, build a `HomeUi` in `HomeViewModel`. This is the file that
resolves INTEGRATION.md §6.7 — the hub hero replaces the old hero card, and `MuscleTargetFigure`
moves to the "Ready to train" rail as heat-tinted muscle cards.

**7 · Session.** Copy `SessionSlateScreen.kt`, build a `SessionUi` in `SessionViewModel`. The
`intensity` field on `SetRowUi` is your existing `SessionSet.intensity` — it drives the e1RM badge.

**8 · Remaining screens.** Plan, Recovery, Stats, History, Library, Settings are specified in
[README.md](README.md) §Screens with measured values and shown in `screenshots/`. They need no
new components — they're `forgeGround()` + one `forgeHero()` + `FlatRow`s + the section header.

---

## Things that will bite you

- **Commented-out `.clickable(...).forgedPress()`** — every interactive surface in these files has
  its click modifier commented, because your `forgedPress` signature is the source of truth.
  Uncomment and wire them; don't change `forgedPress`'s 0.97 scale to the prototype's 0.96.
- **`ForgedSurfaces.clickableRow`** is a stub. Replace it with your real clickable+press modifier.
- **Blur.** `SurfaceStyle.Glass` wants a real backdrop blur. `Modifier.blur` blurs content, not
  backdrop — use a `RenderEffect` on API 31+, and below 31 fall back to `SurfaceStyle.Soft`'s
  opaque 90% fill rather than shipping a flat-looking glass.
- **Anton has one weight.** Never apply `fontWeight` to a `Forge` style; it fakes a bold.
- **Tabular numerals.** Add `FONT_FEATURE_TABULAR` to every timer, weight, volume and stat numeral
  or the steppers will jitter as digits change.
- **Six tabs.** Home · History · Plan · Library · Recovery · Stats. History and Library are tabs,
  so they carry no back arrow. See INTEGRATION.md §2b.
- **Heat is data.** `heat.at(freshness)` is the only way a screen picks a temperature hue. Gold is
  PRs only. Olive is "set completed". `#FF3320` is overtime and errors.
