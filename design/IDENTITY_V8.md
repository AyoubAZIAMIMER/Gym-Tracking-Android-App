# Identity v8 — "BLUE HOUR" (supersedes v7 Night Session)

_2026-07-21. Owner brief: new reference set (Movo Gym Tracker) → "take inspiration…
rethink all the sections… rebuild from the ground up… UI/UX-driven." Owner picked the
**Blue Hour** direction from the mockup and confirmed: electric indigo interface, subtle
gamification only. Keeps every structural discipline that made the identity work (one
accent, semantic-color-only, named system, gold-for-PR law, the motion system)._

## 0 · The story — cold machine, hot body

The gym after dark, at blue hour. The **interface runs one calm electric indigo** — the
current running through the machine: navigation, actions, active state, work done. **Heat
is reserved for the one place effort actually lives: your muscles.** Gold is only ever
what you've earned. Three signals, three jobs, nothing decorative.

Why this isn't the rejected "Apple-Fitness palette" (killed as soulless): that was arcade
colors with no jobs. Blue Hour is a *system* — every hue has exactly one meaning, a name,
and a story. It takes Movo's **discipline**, not its skin.

## 1 · The palette (dark = the designed theme)

| Role | Color | Job |
|---|---|---|
| Floor | `#06070B` blue-biased near-black | OLED ground; everything leans a half-step to indigo |
| Graphite | `#12141B` cards · `#191C25` raised | cool graphite, picked neutrals not default grey |
| Chalk | `#F3F5FB` text | white chalk, a half-step cool |
| **Electric Indigo** | `#5B5BF7` (dim `#1B1D3A` · bright `#8A8CFF`) | THE interface signal: action, nav, active, work done |
| **Ember ramp** | `#C98A4E → #FF7A2F → #FF4D3D` | THE body only: muscle readiness/fatigue |
| Gold | `#FFCB45` | the earned — PRs & top rank only |
| Ice (cyan) | `#64D2FF` | calm time/data (rest, durations, avg line), sparingly |
| Red | `#FF453A` | failure sets, errors |

On the indigo action surface, text is **chalk white** (`OnAccentPrimary`), not near-black.
Completed sets are **indigo** — logging work closes the ring in the one signal color.

### Readiness ramp (the ONLY warm color — and only on the body)
`HeatScale` unchanged in plumbing; its meaning sharpened. Fully recovered **snaps to
indigo** (cool machine "ready"); anything worked graduates through a warm ember sweep
tan → orange → red. The snap avoids a muddy blue→tan lerp. Recovery ("Body") map, Home
readiness dots, the hourglass, per-muscle bars all inherit automatically.

### Single-hue intensity (Apple Activity move) — kept
Weekly-volume bars render in **indigo with intensity-scaled alpha** — brighter = harder.
The ramp is for *readiness*; indigo-intensity is for *work*.

## 2 · Type — kept from v7
Platform grotesk (SF Pro Display / Roboto) at **Black weight, tight tracking** for
display/headline; system weights below. Tabular numerals wherever digits align.

## 3 · Ambience — quieter
`GlowBackground` survives: the floor floodlight is now a **faint indigo wash** (alpha
~0.10, brighter mid-session), cyan spill top-left, gold corner near-silent. Deliberately
more graphite than glow. Glass hairlines stay neutral white.

## 4 · Gamification — SUBTLE only (owner decision)
Rank ladder from the refs — **Wood → Bronze → Silver → Gold → Olympian** — kept as a
*quiet progress cue* (a single understated pill / "N to next"), driven by real logged
volume. The "Start → Now" physique comparison is welcome on Stats. **No badges, stars, or
loud level-up theater** — serious-lifter tone.

## 5 · Voice
Blue-hour athletic, calm-confident. "Lights on." · "Start session" · "Log set" · "In
session" · "Session complete." Forge metaphors retired from UI copy. App name stays
**Forged** (a forged body, not a forge UI).

## 6 · What does NOT change
One-accent discipline, gold-for-PR law, semantic-color-only law, the motion system (all
tokens, curves, recoil, celebration ladder), the anatomical physique, Strike Mode's
structure, glass/blur architecture, the real-data spine (244 workouts / 5,875 sets).

## 7 · Implementation note
The theme is fully tokenized — screens read `MaterialTheme.colorScheme.*` and
`GymTheme.colors.*`, never raw hex. So the whole identity lives in `theme/Color.kt`
(+ one literal in `Theme.kt`, ambience in `components/Glass.kt`). Flipping those flips the
app. Structural moves (icon-in-tinted-tile language, "Body" screen, rank cue, exercise
section-card detail) layer on top. Mockup: `design/` → published "Blue Hour" artifact.
