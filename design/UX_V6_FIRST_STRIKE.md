# UX v6 — "First Strike" (Apple product principles, Forged skin)

_2026-07-17. Owner brief: "see what to do next for UI/UX, be divergent, Apple-like
product design." This evolves UX only — Identity v5 (heat, physique, Anton, forge
voice) is untouched and non-negotiable. "Apple-like" here means Apple's product
principles, explicitly NOT Apple's visual language (the Apple-Fitness palette was
already rejected in v3 as soulless)._

## 0 · The product story (one sentence)

**From pocket to first set in one gesture; during the set, one number at a time,
no keyboard; everything else cools until you need it.**

Every proposal below either serves this sentence or was killed.

## 1 · The Apple principles, translated to the forge

| Apple principle | Forged translation |
|---|---|
| Lead with the one thing that matters now | The app knows its state machine: rest day / plan day / mid-session / just finished. Show that, demote the rest. |
| Direct manipulation, not forms | You never type at the anvil. Scrub, drag, swipe — the keyboard is a failure state mid-workout. |
| Progressive disclosure | The full sets table, history, stats exist one swipe away — not on the primary surface. |
| Physicality | Heavier loads feel heavier (`springMass` exists in Motion.kt and is barely used). Completing a set is a strike, not a checkbox. |
| Opinionated defaults | Smart progression already makes the loading call — the UI should *lead* with it, not display it. |
| Continuity, no dead ends | Widget → first set. Finish → tomorrow's readiness. Every end is the next beginning. |

## 2 · The three moves (build order)

### Move 1 — STRIKE MODE (the big one; session screen)
A focused, full-screen view of **the active set only**:
- Giant Anton numerals: weight center-stage, reps beneath — readable from the floor
  mid-set, sweat in your eyes.
- **Scrub to adjust**: horizontal drag on the weight scrubs in 2.5 kg detents
  (haptic tick per detent, `springMass` weighting — 140 kg scrubs heavier than 40).
  Vertical drag on reps. No keyboard, ever. (Long-press = type, as escape hatch.)
- **One thumb action**: the bottom third is a single STRIKE surface — press to
  complete the set. Rest ring wraps the screen edge afterwards; next set slides in
  when it expires (Law 7 handoff already specifies this warmth).
- The classic table remains one swipe (or a toggle) away — Strike Mode is the
  default *during* a session, the table is for planning/reviewing.
- Progression line becomes the header: "+2.5 kg — you cleared 10s" IS the set's
  title, not a footnote.
- Kill-tests: zero keyboard appearances in a normal session; set logged with
  exactly one touch when accepting the plan.

### Move 2 — NOW CARD (Home leads with state)
Home's first element morphs by state (one composable, four states):
- **Plan day, not started**: today's session — physique glowing, progression calls
  summarized ("Squat +2.5 · the rest holds"), one button.
- **Mid-session**: live card — current exercise, set n/m, elapsed, one tap back
  into Strike Mode. (Session already survives app-switch; it deserves a hero, not
  a label swap on the button.)
- **Just finished** (same day): the forged summary — volume, PRs, muscles struck
  glowing on the physique, tomorrow's earliest-ready muscle group.
- **Rest day**: the recovery read — coolest muscles + "ready to strike" verdict,
  drawn from the freshness model Home already loads.
The weekly ring/strip/tiles stay, below the fold of attention.

### Move 3 — ONE GESTURE IN (continuity layer; mostly wiring)
- Widget tap → if today is a plan day, deep-link **directly into Strike Mode**
  set 1 (session pre-built), not to Home.
- Reminder notification gains a "Fire it up" action button — the notification IS
  the start button.
- Cold-open on a plan day: session pre-warmed in the background so "Fire it up"
  is instant (template already builds in <100 ms; just do it eagerly).
- Metric: taps from pocket to first loggable set — today ~4, target 1.

## 3 · The kill list (divergent options considered and rejected)

- **"Liquid Metal" material overhaul** (specular scroll-sweeps, heat bloom):
  texture, not product. Park it; steal single effects only where they serve focus
  (e.g. non-active content cools/dims in Strike Mode).
- **Full tab collapse** (one-surface app, browse layer behind a single coal):
  right instinct, too destructive to shipped muscle memory — Now Card achieves
  the same "lead with now" without demolishing navigation.
- **Watch-style crown/dial widget for weight**: cute, but scrubbing on the number
  itself is more direct — a dial is UI *about* the number; scrubbing IS the number.
- **Voice logging**: gym noise, AirPods edge cases, AI-gimmick smell. Dead.

## 4 · Honest risks

- Strike Mode changes the muscle memory of the one screen used mid-workout —
  ship it as the default with a persistent, obvious toggle to the table; if the
  owner reaches for the table three sessions running, the default was wrong.
- Scrub detents need real-device haptic tuning (emulator can't judge feel).
- Supersets need a Strike Mode answer (alternate the focused set A/B) — design
  before build, not during.

## 5 · Acceptance

- One-touch set logging when accepting the plan; zero keyboards per session.
- Pocket → first set in one gesture from widget/notification.
- Home answers "what now?" without a single tap in all four states.
- Every existing Identity v5 rule still holds (heat semantics, gold = PR only,
  one idle per screen, tokens-only motion).
