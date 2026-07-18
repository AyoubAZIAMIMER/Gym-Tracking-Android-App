# UI Refresh & Motion Completion — Prompt for Claude Fable 5

*Paste this whole file as the session prompt. Read AGENTS.md, MEMORY.md, and design/MOTION.md before writing any code — this task is governed by all three.*

---

## 0 · Required reading, in order

1. `AGENTS.md` — hard constraints (no AI gimmicks, precision-tool feel, MVVM, etc.)
2. `MEMORY.md` — current project state
3. `design/MOTION.md` — the canonical motion system. **This document already answers most of "how should this animate." Your job is largely to finish implementing it, not invent a new one.**
4. `screenshots/home.png`, `history.png`, `plan.png`, `library.png`, `recovery.png`, `stats.png` — current shipped state of all six tabs. Look at all six before writing code.
5. `design/references/ui_reference_1-3.jpg` — original visual inspiration (for context only, not a target to re-copy).

## 1 · Why this session exists

The developer's own words: *"it's repetitive, I don't know why."* Two root causes were found by comparing the screenshots against the codebase — fix both.

### 1a. Motion.kt is built but barely wired in

`ui/theme/Motion.kt` fully implements the MOTION.md token system: `Settle`/`Cool`/`Plane`/`Temper` curves, the duration scale, `springFirm`/`springMass`/`springHeavy`, and helpers including `forgedPress` and `forgedEntrance`. But it's only actually used in `HomeScreen.kt` and the session flow (`WorkoutSessionScreen.kt`, `FinishSummarySheet.kt`). **`PlanScreen.kt`, `ExerciseLibraryScreen.kt`, `RecoveryScreen.kt`, and `StatsScreen.kt` use zero motion** — no press feedback, no entrance stagger, no progress fills. `HistoryScreen.kt` has partial coverage. Five of six tabs are visually static. This is almost certainly the real source of "repetitive" — the app doesn't feel *dead* screen-to-screen, it *is* dead on 5 of 6 screens.

**Fix:** wire the existing Motion.kt system into every screen that's missing it. Do not add new tokens unless MOTION.md genuinely has no answer — if you think you need a new curve, duration, or spring, stop and check §3–5 of MOTION.md again first.

### 1b. Every screen reuses one undifferentiated card component

Home, History, Plan, Library, Recovery, and Stats all stack the same dark rounded-rect card — same padding, same border, same "bold title left / grey caption / value right" row — with zero hierarchy between a screen's hero content and its repeated list rows. Nothing marks Home as the hub, Library as a fast directory, or Stats as data-dense. The result: screens blur together on sight, independent of animation.

**Fix (no new accent color — hierarchy comes from surface treatment, spacing, and motion, not hue):**
- **History** — the calendar stays the hero (raised/glass surface); the workout-log rows below it should read as a flatter, denser list, not a second stack of full cards.
- **Plan** — "Up Next" keeps full card weight; "Your programs" and "Templates" should be visually lighter (less chrome) so they don't compete with the primary action.
- **Library** — 139 rows of exercises is a directory, not a card gallery. Flatten the row treatment (thin divider or reduced padding/border instead of a bordered card per row); reserve full card chrome for empty states and the exercise detail view.
- **Recovery** — the muscle body map is already the single strongest visual in the app. Let it stay the unambiguous hero; flatten the per-muscle rows beneath it so they read as supporting detail, not a competing card stack.
- **Stats** — the charts are the hero surfaces already; tighten the repeated card chrome around them so the charts read as the content, not "a card that happens to contain a chart."

### 1c. Bottom-nav clipping bug (fix regardless of the above)

Confirmed in code: `HistoryScreen.kt`, `RecoveryScreen.kt`, and `StatsScreen.kt` only call `.navigationBarsPadding()`, which accounts for the OS system bar but **not** for the app's own floating `GlassBottomNav`. In every screenshot of those three tabs, the last visible row/section (workout log entry, Hamstrings row, Recent PRs) is physically clipped behind the nav pill. `ExerciseLibraryScreen.kt` already does this correctly — `LazyColumn(contentPadding = PaddingValues(bottom = 130.dp))`. Apply that same pattern (measure the real rendered height of `GlassBottomNav` + margin, don't guess) to History, Recovery, and Stats. Audit Plan too while you're in there.

**Acceptance check:** on every tab, scroll to the true end of content — the last row must be fully visible above the floating nav, not sliced by it.

## 2 · About "Duolingo-style animation"

The developer asked for this by name. **Do not import Duolingo's visual vocabulary** — MOTION.md already explicitly kills confetti/particle bursts, springy bounces, and mascot-style flourishes as "arcade" (§0, the killed-ideas list). That rejection is deliberate brand policy: "could this exact motion exist in a pastel to-do app? If yes, it isn't ours yet." Reversing that isn't in scope here.

What *is* in scope: Duolingo's app doesn't feel good because it bounces — it feels good because **almost nothing on screen is static**, feedback is instant, and progress has visible momentum. That's exactly what §8 of MOTION.md ("the Celebration Ladder") and §7.4 ("Progress") already specify, just not yet built everywhere:

- **Instant contact feedback** (Law 1, `forgedPress`) on every tappable row/card, not just Home's.
- **The Hot Tip progress fill** (§7.4) — apply to Stats' weekly-volume bars and session-duration line chart (draw in left→right on first entry, per §10), and to Recovery's per-muscle percentage bars/rings.
- **Numbers roll, they don't swap** (§7.4, odometer) — History's "7 workouts · 59,672 kg", Recovery's per-muscle %, Stats' summary figures should count up on entry, capped at 600 ms regardless of digit distance.
- **List entrance stagger** (§10) — first 8 items fade + rise 8px, 30ms apart, on first screen entry only, never replayed on tab-return. This alone will make Plan/Library/Recovery/Stats stop feeling like static screenshots.
- **The Celebration Ladder rungs 1–6** (§8) should already be live from the session flow — verify PR/streak/week-complete moments actually fire on real data paths, since Recovery and Stats are downstream of the same events.

If you finish this list, the app will feel like Duolingo in the way that matters (alive, responsive, rewarding) without looking like it.

## 3 · Hard constraints (non-negotiable, copied from MOTION.md / AGENTS.md)

- No confetti, no particle bursts, no bounce beyond the specified springs (damping ≥ 0.85), no mascots, no new accent color.
- No AI branding anywhere on these screens.
- Animate only `transform`, `alpha`, and `color`. Layout-affecting animation only in expand/collapse, and it must be measured (0 dropped frames on a mid-range device).
- Tokens only — no raw durations, beziers, or spring constants hardcoded in a screen file. If MOTION.md doesn't have the token you need, that's a signal to re-read MOTION.md, not to invent one inline.
- Respect reduced-motion: movement collapses to crossfade, heat/color story stays, idles stop.
- One ambient idle animation per screen, max (§ Law 5) — don't add a second "live ember" to a screen that already has one (e.g. don't add idle motion to Recovery's body map on top of anything already breathing there).

## 4 · Deliverable

- Code changes across `ui/screens/{history,plan,library,recovery,stats}/*.kt` and `ui/components/*.kt` as needed, following the patterns already established in `HomeScreen.kt` and `Motion.kt` — match the existing system, don't parallel it.
- Fix the bottom-clipping bug on History, Recovery, Stats (and confirm Plan is clean).
- Update `MEMORY.md` per AGENTS.md rule 8: log what changed, under "UI decisions" or a new dated entry — specifically note which screens now have full motion coverage so the next session doesn't have to re-diagnose this.
- Playtest on the emulator and take fresh screenshots before declaring done (per AGENTS.md rule 6: manual testing before moving on).
