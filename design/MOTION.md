# FORGED MOTION
### The Motion Design System for Forged
*Version 1.0 — 2026-07-13 · Owner: Motion Direction · Status: Canonical*

Companion to the **Molten Forge** visual identity (MEMORY.md, "Identity v4"). This document defines how Forged *moves* — before any implementation. Hand it to any designer or engineer; if a motion decision isn't derivable from this document, the document is incomplete and must be amended, not worked around.

---

## 0 · The Premise

Forged is a precision tool for people who lift heavy things repeatedly, on purpose. Its motion must feel like the tool it is: **forged steel operated by a confident hand**. Every element behaves as if it has mass, temperature, and a maker.

The user should never think "nice animation." They should think — without words — *this thing is solid*.

**The metaphor is physical and we commit to it totally:**

| The world of the forge | The motion it dictates |
|---|---|
| A hammer strike lands instantly | Feedback on contact, within one frame |
| Forged steel does not wobble | Critically damped settles; near-zero bounce |
| Hot metal glows; energy is temperature | Emphasis is *heat* (color/glow), not displacement |
| Metal cools by exponential decay | Every settle and exit follows a decay curve |
| A workshop is calm; the forge glows in one corner | At most one ambient animation per screen |
| A smith moves deliberately, never twice | One decisive motion; no flourishes, no repeats |

---

## 1 · Motion Philosophy

**Motion in Forged is a material property, not decoration.** Steel moves the way steel must: it responds instantly to force, travels with momentum, and stops with authority. Heat — not movement — carries emotion: effort, reward, and progress are expressed as temperature.

Three sentences that govern everything:

1. **Contact is instant; travel is earned.** The first frame of any interaction acknowledges the user. Whatever moves afterward moves because physics demands it.
2. **Emotion lives in heat, not in bounce.** When Forged celebrates, it glows hotter. It never jumps, spins, or rains particles.
3. **Rest is silence with one ember.** A resting screen is almost perfectly still — a single, barely perceptible sign of life keeps the forge warm.

### The Four Questions (governance gate)

No animation ships unless it passes all four:

1. **Purpose** — does it communicate state, causality, focus, or progress? (If it only communicates "we can animate," kill it.)
2. **Brand** — could this exact motion exist in a pastel to-do app? (If yes, it isn't ours yet.)
3. **The 10,000th interaction** — after a year of daily use, is it still information, or has it become a toll? (Anything on the set-logging path is executed hundreds of times per session. Err brutally toward speed there.)
4. **Pattern** — is it an instance of a token/pattern below? (One-off effects are debt. Extend the system or drop the idea.)

**Killed by this gate, deliberately** (recorded so nobody re-proposes them): confetti/particle bursts on PR (arcade, fails Q2), pull-to-refresh flame (novelty, fails Q3), springy tab bounces (fails Q2), shake-on-error triple-wobble (toy-like; replaced by the Recoil, §9), animated screen-long hero transitions on every navigation (fails Q3), parallax scrolling cards (fails Q1).

---

## 2 · Motion Principles (the Seven Laws)

**LAW 1 — Contact Before Motion.**
Touch feedback lands within one frame (≤ 16 ms): a surface-darkening and compression *begin* on finger-down, never on finger-up. Latency is the difference between "tool" and "app."

**LAW 2 — Steel Doesn't Wobble.**
Damping ratio ≥ 0.85 everywhere, 1.0 by default. Maximum one overshoot, and *only* when the user physically threw the object (gesture release) — because the momentum is theirs, not ours. UI-initiated motion never overshoots. Springs below 0.85 damping are forbidden in this codebase.

**LAW 3 — The Law of Cooling.**
Everything that settles, exits, or fades follows exponential decay — fast first, asymptotically calm — because that is how metal cools (Newton's law of cooling). This gives Forged its signature curve (`settle`, §4) and means motion feels *inevitable*: launched with force, arriving with certainty. Nothing in Forged eases in gently and eases out gently like a screensaver.

**LAW 4 — Heat Is the Emphasis Channel.**
Attention, success, and celebration are expressed as temperature: ember → gold → white-hot, then cooling. Scale and position stay disciplined (≤ 4% scale changes in feedback; movement reserved for structure). Where another app would bounce a badge, Forged raises its temperature and lets it cool. Cooling *is* the celebration decaying naturally — no cleanup animation needed.

**LAW 5 — One Live Ember.**
At most **one** ambient (idle) animation per screen, owned by the single most important element. All ambient motion pauses during scroll and interaction (the workshop goes quiet when the smith works). Amplitudes stay below conscious perception: ≤ 3% scale, ≤ 8% opacity, periods ≥ 3 s, phases desynchronized.

**LAW 6 — One Axis, One Meaning.**
A transition animates the one property that carries its meaning. Entering content rises *or* fades-with-heat — chosen, not stacked. Never scale + rotate + translate + fade together. Compound motion is reserved exclusively for the two Forge Moments (§8: workout complete, PR).

**LAW 7 — Heat Handoff.**
Focus moves between elements by heat transfer: the finished element cools as the next warms. The user's eye follows temperature, not arrows or bounces. This is Forged's replacement for "attention-seeking" motion.

---

## 3 · Timing Scale (duration tokens)

Durations scale with **mass** (size of the thing moving) and **distance**. Exits run at ~0.7× of the paired entrance — leaving must be cheaper than arriving. Anything on the per-set logging path uses `strike`/`fast` only.

| Token | Duration | Use |
|---|---|---|
| `instant` | 0 ms | State color on contact, selection marks, disabled swaps. Perceived as "same frame." |
| `strike` | 80 ms | Press compression, toggles, checkmark impact, detent ticks |
| `fast` | 140 ms | Icon state changes, chips, small reveals, exit of small elements |
| `standard` | 220 ms | Default for component transitions: card expand, row insert, tab content |
| `deliberate` | 320 ms | Screen-level navigation, sheets, session entry |
| `slow` | 480 ms | Progress fills, chart tempering, success cools |
| `forge` | 900 ms | Reserved: the two Forge Moments only (PR flash-and-cool, session-forged sequence) |

**Choreography constants:** list stagger = 30 ms/item (cap: 8 items, 250 ms total); heat-handoff delay = 80 ms; count-up rolls cap at 600 ms regardless of digit distance.

---

## 4 · Easing Curves

Four named curves. No raw beziers in code — tokens only.

| Token | Curve | Character | Use |
|---|---|---|---|
| `settle` | cubic-bezier(0.16, 1.00, 0.30, 1.00) | Launches hard, lands with exponential calm — the Law of Cooling made visible. **The signature curve.** | All entrances, expansions, returns to rest |
| `cool` | cubic-bezier(0.55, 0.00, 1.00, 0.45) | Accelerates away, like heat leaving metal | All exits, dismissals, collapses |
| `plane` | cubic-bezier(0.65, 0.00, 0.35, 1.00) | Symmetric, weighty | On-plane moves: tab slides, reorder shifts, pill indicator |
| `temper` | sine in-out | Perfectly even breath | Ambient/idle loops only — never for user-triggered motion |

**Heat ramps** (color/glow animations): ignition is `instant`→peak; cooling is `settle` over the paired duration. Temperature never eases in slowly — metal is struck hot.

---

## 5 · Spring Physics

Springs are used **only** where a gesture hands the system a velocity, or where interruption mid-flight must feel continuous. Everything else uses the curve tokens.

| Token | Stiffness | Damping ratio | Use |
|---|---|---|---|
| `springFirm` | 550 | 1.00 | UI-initiated settles that may be interrupted (selection pill, expanding card) |
| `springMass` | 380 | 0.90 | Gesture releases: weight-drag detents, sheet flings, reorder drop. The one place a single, small overshoot may occur — the user's own momentum |
| `springHeavy` | 260 | 1.00 | Large surfaces: bottom sheets, session screen entry |

Hard rules: damping < 0.85 forbidden; more than one visible oscillation forbidden; springs never applied to color/opacity (heat is not springy).

---

## 6 · Animation Hierarchy

When multiple things could move, precedence is:

1. **The user's object** — whatever is under the finger tracks 1:1, always, uninterruptible by anything else.
2. **Structural response** — layout adapting to the user's object (rows parting during reorder).
3. **Feedback** — state confirmations (check strikes, heat flashes).
4. **Ambient** — the one live ember; pauses whenever levels 1–3 are active.

Only one level may be *prominent* at a time. If a celebration (level 3) would collide with active input (level 1), the celebration waits — it never steals the hand.

---

## 7 · Component Behaviors

### 7.1 Buttons — "forged metal"

| State | Behavior |
|---|---|
| Press (down) | Same-frame: surface darkens ~8% (metal under pressure) + compress to **0.97** scale, `strike`/80 ms. Ember gradient tightens toward the press point. Haptic: none yet (contact is visual). |
| Release (commit) | Scale returns via `settle`/220 ms; a single radial highlight blooms from the touch point and cools (300 ms) — the spark of the strike. Haptic: light tick. Action fires on release, so the spark and the result read as cause → effect. |
| Release (cancel — slide off) | Cools back via `cool`/140 ms. No spark, no haptic. Nothing happened, and the motion says so. |
| Disabled | **Perfect stillness.** Cold metal doesn't respond. Transition to/from disabled is an `instant` color swap + 140 ms opacity. No press animation at all when disabled — deadness is the feedback. |
| Loading | No spinner inside buttons. The ember gradient slowly crawls (heat shimmer), `temper`, 1.6 s loop. Label swaps via 140 ms crossfade ("Fire it up" → "Forging…"). |
| Success | Glow flashes to gold and cools over `slow`/480 ms; label swaps under it. One motion, then still. |

### 7.2 Cards & glass panels

- **Expand/collapse:** height via `settle`/`standard`; content fades in *after* 40 ms (structure first, then information). Collapse uses `cool` at 0.7× duration. The card's hairline border warms slightly (+heat) while expanded — the open tool is the hot tool.
- **Tap-to-navigate cards:** press state identical to buttons but compression is 0.985 (bigger mass, smaller give).
- **Reorder (drag):** on lift, the card gains elevation shadow + 1.02 scale over `strike` — *picking up a tool*. Displaced rows part with `springFirm`. On drop, `springMass` settle into the slot; slot's hairline flashes warm once.
- Cards never tilt, never parallax, never idle.

### 7.3 Icons — the living layer

Important icons carry an **idle behavior** governed by Law 5 (one live ember per screen — priority order below decides which one runs):

| Icon | Idle behavior | Spec |
|---|---|---|
| Streak flame (Home) | Flicker: two stacked sine opacities at 3.2 s and 5.1 s periods (incommensurate → organic), 0.92–1.00 range; inner glow drifts ±1 px vertically | Runs when Home is at rest |
| "Fire it up" button | Ember gradient crawl, 6 s `temper` loop, ≤ 6% luminance amplitude | Runs only if it is the screen's primary action and streak flame yields (button outranks flame on workout days) |
| Rest-timer ring (session) | Breathes: glow radius ±2%, 4 s cycle — a resting breath while you rest | Owns the session screen's ember during rest |
| Weekly ring (Home) | Heats only when one workout from goal: glow breathes at 5 s | Replaces flame when active (proximity to goal outranks streak) |
| Tab bar icons | **Never idle.** Navigation is calm, always. On selection: tint heats over `fast`, pill slides via `plane`. | — |

All idles: pause during scroll/touch, stop entirely under reduced-motion, run off one shared clock (§12).

### 7.4 Progress

- **Rings/bars fill** via `settle` over `slow`, led by **the Hot Tip**: a small glow leads the fill's edge like drawn molten metal, and the trail cools behind it (gradient tail, 200 ms decay). The Hot Tip is Forged's signature progress pattern — used by the weekly ring, rest ring, chart line draws, and the History month change.
- **Numbers never swap — they roll.** Odometer roll with `settle`, duration scaled by digit distance, capped 600 ms. Numbers have mass.
- Indeterminate progress: the skeleton sheen (§10), never a spinning circle, anywhere.

### 7.5 Inputs & the weight drag

- Focus: hairline warms `fast`; no floating-label theatrics.
- The ±2.5 kg drag gets **detents**: each increment = a 1-frame visual tick (digit rolls one step) + light haptic tick — turning a machined dial. Release settles to the nearest detent with `springMass`. This is the highest-frequency gesture in the app; it must feel like its best physical analog.

---

## 8 · Progression Motion (the Celebration Ladder)

The reward system is a **temperature ladder**. Each rung earns more heat. Nothing on the ladder uses particles, badges that fly, or sounds. Premium fitness, not arcade — the user's number going up *is* the fireworks; we light it well.

| Rung | Moment | Motion |
|---|---|---|
| 1 | **Set complete** | Check strikes in at `strike` (opacity + 0.9→1.0 scale, `settle`); row's background flashes +12% ember tint and cools over 600 ms; rest ring ignites with the Hot Tip. Haptic: light. Total perceived cost ≈ 0 — this happens 30× per session. |
| 2 | **Exercise complete** | The card cools: completed sets dim to done-state over `standard`; **Heat Handoff** — 80 ms later the next exercise's hairline warms for 400 ms and cools. The eye is *carried* to the next lift. Haptic: none (the set already ticked). |
| 3 | **Workout complete — "Session forged"** | Forge Moment (uses `forge` budget): sheet rises `springHeavy`; the check strikes with a single 2% settle; one spark-line arcs off it (300 ms, cools mid-air); stats count up with 60 ms stagger, odometer rolls; the sheet's glow cools to rest over the last 400 ms. Haptic: medium, once, on the check strike. |
| 4 | **PR — "Forged"** | The apex. At the moment of logging: the set row flashes **white-hot** (#FFF → gold → ember) cooling exponentially over 900 ms; the gold star strikes in at `strike` with one 2% settle; the value digits re-roll in gold before cooling to standard text. Haptic: heavy, once. Everything else on screen is frozen during the flash (Law 6 — compound motion is the exception here and must own the stage). |
| 5 | **Streak extended** | Flame does one strong, single flicker (0.75→1.0 opacity spike, 400 ms cool); streak number rolls up. No haptic — it's ambient news. |
| 6 | **Week complete** | Weekly ring completes with a Hot Tip sweep around the full circumference (600 ms), then the whole ring cools to its done state. |
| 7 | **Level/achievement (future)** | Reserved: follows Rung 3's grammar (strike → single spark → count → cool). No new grammar without amending this document. |

**Ladder invariants:** heat always cools back to rest (celebrations are self-cleaning); a lower rung never outshines a higher one; rungs 3–4 are the only compound animations in the product.

---

## 9 · Feedback & Error States

- **Confirmation** = heat: brief warm tint + cool. Never a toast animation stack.
- **Error — "the Recoil":** cold metal struck wrong rings once. The element recoils 2 px horizontally and stops **dead** (90 ms, hard stop, no oscillation) while the surface tints forge-red and cools over 400 ms. Never the iOS triple-shake — one recoil, absolute stillness after. Haptic: rigid.
- **Destructive confirms:** the confirming button heats to red over `standard` while held — cancellation cools it. Commitment has temperature.
- **Empty states:** static composition; the screen's one ember may live here (e.g., faint ember drift behind "Nothing forged yet").

---

## 10 · Loading & Content Entrance

- **Skeletons — "cold steel":** dark surface shapes with a faint warm sheen sweeping left→right every 1.8 s (`temper`). No pulsing grey blocks, no spinners.
- **Content arrival:** loaded content replaces its skeleton via 140 ms crossfade + 8 px rise (`settle`). Data never pops.
- **Charts temper in:** line charts draw left→right over `slow` led by the Hot Tip; bars rise with 30 ms stagger via `settle`. Axes and labels are already there — the *data* is what's being forged, so only the data moves. Runs once per screen entry, never on tab-return.
- **Lists:** first entry of a screen: items fade + rise 8 px, 30 ms stagger, first 8 items only (rest are under the fold — they arrive as part of scroll). **Never replay** on back-navigation or tab return: replaying an entrance tells the user their place was destroyed. Preserving focus outranks looking alive.

---

## 11 · Navigation & Space

Forged's space is a **workbench**: tabs sit side-by-side on one plane; details are tools lifted off the bench; the session is the forge you step toward.

| Transition | Motion |
|---|---|
| **Tab switch** | On-plane slide: outgoing and incoming move 12 px laterally in the direction of tab-index delta + 180 ms crossfade, `plane`. Nav pill slides on the same curve simultaneously — one gesture, one plane, everything connected. No vertical motion, no scale. |
| **Open detail** (workout, exercise, program) | The lift: incoming surface rises 16 px + fades in over `deliberate` `settle`, parent dims 4% beneath. Where an element persists (exercise name list → detail header), it travels as a shared element — the tool stays in hand. |
| **Return** | The set-down: 0.7× duration, `cool`. Exits are always cheaper. |
| **Enter session** | The biggest move in the app: session surface rises from the bottom edge (`springHeavy`) while the background heats — GlowBackground's ember intensifies ~20% during the transition, as if stepping toward the forge. Reverse (leaving) cools it. |
| **Sheets** | Rise with `springHeavy`; scrim is warm black at 40%, fading `standard`. Dismissal follows the finger 1:1, then `springMass`. |
| **Bottom nav** | Hides on subpages by sliding down 8 px + fade `fast` (it belongs to the bench, and we've lifted off it). |

**Scroll:** stock physics — never hijacked. Top bars gain their blur/shadow via 140 ms fade when content passes beneath. The only scroll-linked depth: GlowBackground's ember moves at 0.97× scroll factor — a hair of distance, never a parallax effect the user can *see*, only one they can feel.

---

## 12 · Performance, Accessibility & Governance

- **Animate only transform, alpha, and color.** Layout-affecting animation is allowed only in expand/collapse, and must be measured (jank budget: 0 dropped frames on a mid-range device).
- **One clock:** all ambient loops derive from a single shared time source so idles batch invalidation and can be globally paused (scroll, interaction, backgrounding, reduced-motion).
- **Reduced motion** (system setting): all movement becomes crossfade; heat/glow remains (temperature is not vestibular); idles stop; celebrations keep their color story minus displacement. Forged degrades to *calmer*, never to *broken*.
- **Battery/perf floor:** idles ≤ 6% amplitude at ≤ 0.5 invalidations/frame; no idle runs off-screen.
- **Governance:** new motion must (a) pass the Four Questions, (b) be expressed in the tokens of §3–5, (c) name its rung/pattern. If it can't, this document gets amended first. The tokens are the API; screens never hardcode durations or curves.

---

## 13 · Implementation Map

> **Reality check:** Forged is built in **Kotlin + Jetpack Compose** (this repo), not Flutter. The system above is platform-agnostic; this section maps it to the actual codebase first. A Flutter mapping follows for completeness (e.g., a future port), since it was explicitly requested.

### 13.1 Jetpack Compose (the codebase)

Create `ui/theme/Motion.kt` — the single home of motion tokens:

- **Durations** as `Int` constants; **easings** as `CubicBezierEasing(0.16f, 1f, 0.3f, 1f)` etc.; **springs** as `spring(dampingRatio, stiffness)` factories (`MotionTokens.settle`, `MotionTokens.springMass`, …).
- **Implicit animation** (`animateColorAsState`, `animateFloatAsState`, `animateDpAsState`) with token specs for heat ramps, press states, tint changes.
- **`AnimatedContent` / `Crossfade`** with token transitions for label swaps and number rolls (custom `ContentTransform` for the odometer: `slideInVertically + fadeIn togetherWith slideOutVertically + fadeOut`, clipped).
- **Navigation transitions**: `NavHost(enterTransition/exitTransition)` per route implementing §11 (tab `plane` slides via `slideInHorizontally(initialOffsetX = 12.dp)` equivalents; details via `fadeIn + slideInVertically(16.dp, settle)`).
- **Shared elements**: `SharedTransitionLayout` + `Modifier.sharedElement()` (Compose 1.7+, already on BOM 2025.05) for list→detail continuity.
- **Lists**: `Modifier.animateItem()` on `LazyColumn` children for reorder/insert (`springFirm`); entrance stagger via per-index `LaunchedEffect` delay driving `animateFloatAsState`, gated by a "first composition of this screen" flag so it never replays.
- **Idle embers**: one `rememberInfiniteTransition` per screen behind a `LocalForgeClock` CompositionLocal; expose `pauseAmbient()` tied to scroll/interaction state; draw glows in `Canvas`/`drawBehind` with radial `Brush` so only alpha/color invalidate.
- **Press physics**: `Modifier.pointerInput` + `Animatable` for contact-frame compression (`graphicsLayer { scaleX/scaleY }`), guaranteeing Law 1 (start on `awaitFirstDown`, not on click).
- **Haptics**: `LocalHapticFeedback` (light/medium ticks); heavy PR haptic via `HapticFeedbackType.LongPress` or `View.performHapticFeedback(CONFIRM)`.
- **Reduced motion**: read `Settings.Global.ANIMATOR_DURATION_SCALE == 0f` → provide `LocalMotionScale`; token factories collapse movement to crossfade when 0.
- **Perf**: transform/alpha only via `graphicsLayer`; heat flashes as `drawBehind` overlays, never recomposing children.

### 13.2 Flutter (requested mapping, for a future port)

- Tokens as a `ForgedMotion` class: `Duration`s, `Cubic(0.16, 1, 0.3, 1)` curves, `SpringDescription`(mass 1, stiffness 380, damping = 2·ζ·√(k·m)).
- **Implicit** (`AnimatedContainer`, `TweenAnimationBuilder`) for heat ramps and state colors; **explicit** (`AnimationController`) only for choreographed rungs 3–4.
- **flutter_animate** for entrance grammar: `.fade(duration: fast).slideY(begin: 0.02, curve: settle)` — its chaining maps 1:1 to our token language; wrap it so screens call `ForgedEntrance()` rather than raw chains.
- **Hero** for shared elements (list→detail names).
- **Rive** only where a state machine genuinely earns it: the anvil-check strike (rung 3) and the flame's flicker states. Everything else is cheaper as code. **No Lottie** — agreed: JSON playback can't be interrupted mid-flight or respond to gesture velocity, and both are core to this system.
- `RepaintBoundary` around ember loops; `TickerMode(enabled: false)` to silence ambients off-screen; respect `MediaQuery.disableAnimations`.

---

## 14 · Quick Reference Card

```
CURVES    settle  (.16,1,.3,1)   enter/expand/rest      cool (.55,0,1,.45)  exits
          plane   (.65,0,.35,1)  tab/reorder slides     temper (sine)       idles only
DURATION  instant 0   strike 80   fast 140   standard 220   deliberate 320   slow 480   forge 900
SPRINGS   firm 550/1.0    mass 380/0.9 (gesture release only)    heavy 260/1.0
LAWS      contact ≤1 frame · no wobble (ζ≥0.85) · exits 0.7× · heat = emphasis
          one ember/screen · one axis · heat handoff carries focus
NEVER     bounce chains · confetti · spinners · triple-shake · replayed entrances
          idle during interaction · raw beziers in code · motion without a rung/pattern
```

*Every movement in Forged answers one question: what would forged steel do?*
