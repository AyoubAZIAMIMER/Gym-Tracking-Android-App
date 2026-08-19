# Identity v7 — "NIGHT SESSION" (replaces Molten Forge's colors, keeps its discipline)

_2026-07-19. Owner brief: "change the colours and the forged theme… different colours,
more Apple-like design, fits GYM apps identity more." Supersedes v4/v5 color + voice;
keeps every structural rule that made the identity work (one accent, semantic color
only, named system, no arcade)._

## 0 · The story

Training after dark. A matte-black gym floor, floodlight from above, chalk-white
numbers, and one signal color cutting through: **volt** — the yellow-green that Apple
Workout numerals and Nike Training made the native color of athletic effort. This is
the most gym-recognizable visual language in existence, executed with Apple's
restraint instead of Nike's shouting.

Why this isn't the rejected v3 ("Apple-Fitness palette", killed as soulless): v3 was
three arcade colors with no jobs. Night Session is a *system* — every hue has exactly
one meaning, and the whole thing has a name and a story.

## 1 · The palette (dark = designed theme)

| Role | Color | Meaning |
|---|---|---|
| Floor | `#050607` near-black | OLED matte; Apple Watch heritage |
| Plate steel | `#131518` cards · `#1C1F23` raised | cool iron, no warmth |
| Chalk | `#F5F7F8` text | white chalk on black iron |
| **Volt** | `#D2F542` | THE signal: action, effort, work done, today's targets |
| **Ice** | `#64D2FF` | recovery, time, calm data (iOS cyan) |
| Gold | `#FFD60A` | PRs only — unchanged law |
| Red | `#FF453A` | failure sets, errors, spent muscles (iOS red) |

Set tags move to iOS system colors (W gold · D purple `#BF5AF2` · N orange `#FF9F0A`
· T ice · F red). Completed sets are **volt** — logging work closes the ring; one
signal color for "done", not a separate green.

### Readiness ramp (replaces the forge heat scale, same plumbing)
`HeatScale` stays; its stops become the sports-science convention every athletic
device uses: **volt (ready) → amber `#FFB020` (worn) → orange `#FF7A24` (hot) →
red `#FF453A` (spent)**. Freshness 1f = volt. Recovery map, Home dots, hourglass,
per-muscle bars all inherit automatically.

### Single-hue intensity (Apple Activity move)
Charts stop using the ramp for effort. Weekly-volume bars and session e1RM badges
render in **volt with intensity-scaled alpha** — brighter = harder, like Activity's
bar charts. The ramp is for *readiness*; volt intensity is for *work*.

## 2 · Type

Anton retires. Display/headline/titleLarge become the system grotesk at **Black
weight with tight tracking** (−1 on display sizes) — the Apple-on-Android move:
use the platform face confidently, big and heavy, instead of a poster font.
Stamped `labelSmall` tracking stays. Tabular numerals stay everywhere digits align.

## 3 · Ambience

GlowBackground survives with new physics: the ember furnace becomes **floodlight** —
a faint volt wash rising from the floor (dimmer than the ember was), ice spill
top-left, the gold spark corner gone quiet (0.05). Glass hairlines go neutral white
(no more firelight warm tint). Session still brightens the floor glow (Law §11).

## 4 · Voice (quiet-confident athletic; forge metaphors retired from UI copy)

- "The forge is hot." → **"Lights on."**
- "Fire it up" → **"Start session"** · "Back to the anvil" → **"Resume session"**
- STRIKE → **"LOG SET"** · "AT THE ANVIL" → **"IN SESSION"**
- "SESSION FORGED" → **"SESSION COMPLETE"**
- "Cold metal" discard → **"Nothing logged"**
- Recovery: "READY ←→ SPENT" legend; "Train what's ready."
- Widget/notification: "since last session", "Time to train", "Start now"
- App name stays **Forged** (a forged body, not a forge UI).

## 5 · What does NOT change

One-accent discipline, gold-for-PR law, semantic-color-only law, the motion system
(all tokens, curves, recoil, celebration ladder), the anatomical physique, Strike
Mode's structure, glass/blur architecture, launcher mark geometry (recolored: volt
bar, steel plates, black floor).
