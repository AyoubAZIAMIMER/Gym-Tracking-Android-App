# Identity v9 — "Ember, Restored" (reverts v8 "Blue Hour" color only)

_2026-08-07. Owner brief: pointed at the live Claude Design project behind
`design/redesign-2026-07/` and, once shown that its palette traced back to the pre-v7 ember
identity, said plainly: "Actually want ember back." Supersedes v8's color axis only — every other
v7/v8 decision (type, motion, subtle gamification) stands._

## 0 · Why this happened

`design/redesign-2026-07/` is a Claude Design project export. Its own sync log recorded that it was
built against `design/IDENTITY_V5.md` — where ember `#FF5A1F` was the primary/action color — not
the current v8 "Blue Hour" doc. The app had since moved through two later, deliberate pivots (v7
"Night Session" → volt green, v8 → electric indigo), so the mockup and the shipped app quietly
diverged on the one thing a mockup can't fake: color. The owner chose to keep what the design tool
actually produced rather than have it re-interpreted into indigo.

## 1 · What changes

The primary/action signal reverts from indigo `#5B5BF7` to ember `#FF5A1F`, and everything that was
built to key off it in v5 comes back with it:

| Role | v8 "Blue Hour" | v9 (restored) |
|---|---|---|
| Primary / action / nav / work-done | Indigo `#5B5BF7` | **Ember `#FF5A1F`** |
| Success / completed (separate from primary) | *(merged into primary)* | **Tempered olive `#B8C77A`** |
| Heat scale "ready" (fully recovered) | Indigo (snapped to primary) | **Quenched steel `#8FB4C7`** (own literal) |
| Heat scale "hot" (working) | Orange (mid-ramp) | **Ember** — same token as primary, same as v5 |
| Floor / surfaces | Blue-biased near-black | Warm charcoal iron |
| Glass hairlines | Neutral white | Warm firelight tint |

`HeatScale`'s field names (`ready`/`worn`/`hot`/`spent`) and its `GymTheme.colors.heat` API are
unchanged from v8 — only the values and the `.at()` curve (restored to v5's thirds) moved, so every
existing call site (Recovery, Strike Mode's deload color, the rest timer ring) kept working
untouched.

Two real bugs surfaced and got fixed while restoring this, not just re-colored:
- Home's readiness tag ("QUADS WORN") was always rendered in the flat primary color regardless of
  which of the four buckets it named — it only looked right under v8 because `heat.ready` happened
  to equal primary. It now resolves its color from the actual heat-scale bucket.
- The "This Week" done-day checkmarks were reading the primary color; the mockup shows them as
  olive, i.e. the `success` role — separated from primary again for exactly this reason.

## 2 · Type — Anton returns as the forge voice

Reading the prototype's actual markup (not just its screenshots) showed it sets `font-family:Anton`
in four places on Home: the FORGED brand mark (15px/1.8 tracking), the Start CTA (20px), the week
counter, and the RECENT volume numerals. v7 retired Anton with the volt pivot; v9 restores it,
because Anton belongs to the same v4/v5 forge identity ember does — reverting one without the other
was incoherent.

The discipline from v4 still holds: **Anton is a stamped accent, never body copy.** One weight,
never paired with bold. Brand mark, CTA label, hero numerals only. Everything else stays v7's
platform grotesk. Section labels take a third voice the prototype introduced — a **stamped
monospace** (`StampLabel`: 11sp, 1.2sp tracking, Bold) for "TODAY" / "THIS WEEK" / "NEXT UP" /
"RECENT".

## 3 · What does NOT change

The motion system (all tokens/curves/recoil/celebration ladder), the anatomical physique, Strike
Mode's structure, glass/blur architecture, the subtle rank ladder (Wood→Olympian,
`domain/Rank.kt`), PR gold's role (the exact literal reverts to v5's `#FFC93C`, a small refinement
drift, not a role change — gold-for-PR stays law).

## 4 · Home is flat, not glass

The prototype's Home has **no hero card**: sections are separated by hairlines
(`border-top: 1px`), full-bleed, with 22dp horizontal padding. `HomeScreen.kt` was rebuilt to match
it 1:1 — including the 28dp/10dp-radius squircle week cells, the ember bloom on the Start CTA
(`Modifier.emberBloom`, since Compose has no colored box-shadow below API 28), chevrons on NEXT UP
rows, and two RECENT rows. **Every tab screen followed** — Plan, Body, History, Stats, Library and
Settings are all flat now, so `GlassSurface` survives only where an object is genuinely a distinct
plate: the session top bar, the Stats rank cue, exercise-detail heroes.

**Known unimplemented detail**: the prototype's week strip shows a *dashed* cell for a rest day.
The app has no rest-day model — programs are a rotation pointer, not a weekday-mapped calendar —
so that state is not derivable and was left out rather than faked.

## 5 · The shared screen vocabulary

Reading the *canonical* prototype (`prototype/Forged Prototype.dc.html` — the runnable one with
`data-screen-label` per screen, which is what the 12 screenshots were captured from) showed every
screen is the same four parts. Those are now real components in `ui/components/ForgedSection.kt`:

| Part | Component | Spec |
|---|---|---|
| Screen title | `ForgedScreenTitle` | 28sp/-0.5, optional muted trailing caption |
| Section divider | `SectionRule` / `RowRule` | 1dp `outlineVariant`, rows at 55% alpha |
| Stamped label | `StampText` / `StampLabel` | mono 11sp, 1.2sp tracking, Bold |
| Section heading | `ForgedSectionHeader` | stamp + trailing link or slot |
| List row | `ForgedListRow` | 15.5sp title, 12.5sp caption, optional chevron |
| Week strip | `ForgedWeekStrip` | 28dp squircles, `success` when logged |
| Effort selector | `EffortBars` | 5 rising bars (8/11/14/17/20dp), Easy→All out |

**Note the exploration doc lies.** `prototype/Forged Redesign.dc.html` is a design-exploration
file of turns and options; its Home differs from the shipped prototype (10sp/1.7 labels vs
11sp/1.2, no "Full plan" link, ember readiness tag vs quenched steel). Build from *Forged
Prototype.dc.html*.

## 6 · Icons

The prototype draws its own stroke set rather than using Material's — `ForgedIcons.kt` ports them
verbatim from its `iconP` table: one 24-unit box, 1.9 stroke, round caps and joins, no fill, for
Home/History/Plan/Library(barbell)/Body(pulse)/Stats. The brand mark is its filled `barbell()`
5-rect glyph. This is why the nav no longer looks like stock Android.

## 7 · Ambience

`GlowBackground` matches the prototype's per-screen wash: ember hangs **above** the work
(`at 50% -8%`), not rising off the floor, at a per-screen `glowAlpha` (Home .12 · Plan .11 ·
Body/History/Library .10 · Settings .09 · Done .22). Over it sits a 3dp dot grain
(`rgba(255,240,225,.035)`), tiled through an `ImageShader` so the whole texture is one draw call.

## 8 · Implementation note

Same as v8 §7: the whole identity lives in `theme/Color.kt` (+ one literal in `Theme.kt`, ambience
comments in `components/Glass.kt`). The only places that couldn't read the token and needed manual
edits were baked-in XML art that can't reference Kotlin constants: the launcher icon
(`res/drawable/ic_launcher_foreground.xml`, `ic_launcher_bg_gradient.xml`) and the home-screen
widget background (`res/drawable/widget_bg.xml`, `res/layout/widget_forge.xml`).

Source of truth for the restored literal values: commit `d9c1dea` ("Identity v5") — the exact tree
the Claude Design project synced against — not a reconstruction from this document's prose.
