# Identity v5 — "Heat Is Data" (Molten Forge, full spectrum)

_2026-07-17. Evolves Identity v4 (MEMORY.md). v4's rules stay in force except where this
document explicitly widens them. Owner brief: "seems like it has just one color… use
whatever colours you want, just give it an identity" + "use real human physique to show
the muscles hit."_

## 0 · Diagnosis

v4 was right to kill random accent colors, but it over-corrected: with ember as the only
hue, every screen renders as charcoal + orange and the identity reads as *monochrome*,
not *opinionated*. A real forge is not one color — steel moves through a temperature
spectrum, and a smith reads that color to know what the metal is ready for. That spectrum
is the missing color system.

## 1 · The rule that changes

**Color = temperature, and temperature = data.** v4's "one hot color" becomes "one hot
*axis*." New hues may only enter a screen as points on the heat scale (or the existing
fixed roles below) — never as decoration, never as a new arbitrary accent.

### The heat scale (interpolated, never hand-picked)

| Temperature | Hex | Meaning in data |
|---|---|---|
| Quenched steel | `#8FB4C7` | fully recovered · calm/resting · structure |
| Warming bronze | `#D08A45` | mid-recovery · moderate effort |
| Ember | `#FF5A1F` | working · the action color (unchanged primary) |
| Glowing red | `#FF3320` | just trained · fatigued · maximal effort |
| White-hot | `#FFE3C2` | momentary flashes only (highlights, strike moments) |

`forgeHeat(freshness)` in `ui/theme/Color.kt` is the single source of truth — screens
never pick a heat hue by hand, they ask the function.

### Unchanged fixed roles (from v4)
- Ember `#FF5A1F` stays the primary/action color.
- Gold `#FFC93C` stays **PRs only**.
- Tempered olive stays "set completed." Forge red stays errors/failure sets.
- Anton display type, furnace glow background, forge voice, warm glass — all unchanged.

### Where heat applies (the "more color" mandate)
- **Recovery**: the whole screen is a heat reading. Fresh muscle = quenched steel
  (cool blue — *ready to strike*), just-trained = still glowing. This inverts v4's
  green/gold/red traffic light, which was the one part of v4 that never belonged to
  the metaphor.
- **Body maps**: muscles tinted by `forgeHeat`; hot muscles get a physical glow halo.
- **Charts (Stats)**: effort-encoding marks (volume bars, intensity) may use vertical
  heat gradients; time/duration lines stay quenched steel.
- **Session**: working-set intensity may warm toward red at top effort.

### What stays forbidden
Rainbow category palettes, per-muscle-group arbitrary hues, cheerful pastels, any hue
that isn't a temperature or an existing fixed role. The test is still v4's: "could this
exact color choice exist in a pastel to-do app?"

## 2 · The physique (replaces the geometric body map)

The v4 map was rounded-rect blobs — honest about being abstract, but the owner wants a
*human physique*, and the best-in-class apps (Fitbod, Hevy) earn their body maps with
real anatomy. New spec, drawn as vector paths in `MuscleBodyMap.kt` (100×170 unit space,
front + back figures, mirrored-symmetric):

- A muscular male silhouette (≈7.7 head heights) as the base plate, near-invisible
  (onSurface at ~7%), like an unlit forging blank.
- Individual muscle bellies as separate shapes: traps, front/rear delts, pecs, biceps,
  triceps, forearm mass (silhouette only), rectus abs with cut lines, obliques, lats,
  spinal erectors, glutes, quads with medialis teardrop, hamstrings, gastrocnemius,
  shins. Each maps to one of the 10 canonical groups.
- Tint = `forgeHeat(freshness)`; muscles past ~35% heat also emit a soft halo — hot
  metal glows, cold metal doesn't.
- **No % pills on the body** (v4's pills covered the anatomy). Exact numbers live in
  the BY MUSCLE list; the body communicates by color alone, read against a heat-scale
  legend strip under the figures.
- `MuscleTargetFigure` (exercise detail) uses the same anatomy: target muscles at full
  ember glow, the rest cold.

## 3 · Copy consequence

"Strike where the metal is ready" now finally agrees with the visuals: quenched
(blue, cooled) = ready to strike again; glowing (red) = still cooling, leave it on
the rack. Legend labels: `COOLED · READY` ←→ `GLOWING`.
