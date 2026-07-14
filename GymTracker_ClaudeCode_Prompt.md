# GymTracker — Claude Code Master Prompt
# Drop this file + your 3 UI reference photos into the project folder, then open with Claude Code

---

## How to start your Claude Code session

```
1. Open terminal in your project folder (the one containing this file + the 3 UI photos)
2. Run: claude
3. Paste this message:

"Read the file GymTracker_ClaudeCode_Prompt.md in full, then read all images in this folder.
Create the full repo structure, AGENTS.md, MEMORY.md, and SKILL.md as specified.
Then start Phase 1."
```

---

## Model routing — critical for your $20 Pro plan

You have Fable 5 included until July 12, 2026 (up to 50% of your weekly limit).
After July 12, Fable 5 requires paid usage credits ($10/M input, $50/M output).
Fable 5 drains the usage pool faster than Opus for equivalent work.
Route tasks to the right model — do not use Fable 5 for everything.

### When to use Fable 5 (before July 12 only)
- Designing the entire database schema and Room entity relationships
- Writing the LangGraph agent graph architecture (graph.py, nodes.py, state.py)
- Reviewing full architecture decisions that span Android + backend
- Generating the exercise seed database (300+ exercises with metadata)
- Writing the analytics engine (all formulas: 1RM, volume, plateau, progressive overload)
- Any task requiring reasoning across the full project simultaneously

### When to use Opus 4.8 (default for complex work after July 12)
- Multi-file refactors across Android and backend
- Debugging complex state issues in Jetpack Compose
- Writing the FastAPI router layer and SQLAlchemy models
- Designing the sync conflict resolution logic
- Reviewing full feature implementations before marking done

### When to use Sonnet 4.6 (default for focused tasks)
- Writing individual Compose screens from a spec
- Adding a single endpoint or DAO method
- Fixing a specific bug with a clear error message
- Generating boilerplate (build.gradle edits, manifest, data classes)
- Writing unit tests for a function already written

### When to use Haiku 4.5 (fast mechanical edits)
- Renaming variables or functions across a file
- Reformatting code to follow conventions
- Adding import statements
- Generating simple data class variants

### How to switch models in Claude Code
Type /model in the Claude Code terminal to switch.
Switch up for architecture sessions. Switch down for execution runs.
Check /usage regularly to monitor your weekly pool.

---

## Project identity

You are building **GymTracker** — a personal Android workout tracking app that replicates
and surpasses all features of the Progression app (free AND pro), with a Python backend
and an agentic AI coaching layer added later. This is a personal-use app with no paywall.

The developer (Ayoub) is a professional Python/AI engineer (FastAPI, PostgreSQL, Docker,
Azure, PyTorch, scikit-learn, LangGraph). He is learning Kotlin/Android.
Keep Android code simple, pedagogical, and well-commented.
All intelligence lives in Python — the Android app is a display and logging layer only.

---

## Repository structure

```
gymtracker/
├── AGENTS.md                  ← Read every session, first
├── MEMORY.md                  ← Project state, progress, decisions
├── GymTracker_ClaudeCode_Prompt.md   ← This file
├── ui_reference_1.png         ← Developer-provided UI reference
├── ui_reference_2.png         ← Developer-provided UI reference
├── ui_reference_3.png         ← Developer-provided UI reference
├── android/
│   └── app/src/main/
│       ├── java/com/gymtracker/
│       │   ├── data/          ← Room DB, DAOs, entities, repositories
│       │   ├── ui/            ← Compose screens and reusable components
│       │   │   ├── screens/
│       │   │   ├── components/
│       │   │   └── theme/     ← Color, typography, shape
│       │   ├── domain/        ← Use cases, pure business logic
│       │   ├── network/       ← Retrofit client (Phase 3)
│       │   └── utils/         ← PlateCalculator, OneRMFormulas, OfflineRules
│       └── res/
├── backend/
│   ├── SKILL.md               ← FastAPI, SQLAlchemy, LangGraph patterns
│   ├── main.py
│   ├── routers/               ← exercises, workouts, analytics, agent
│   ├── models/                ← SQLAlchemy entities + Pydantic schemas
│   ├── agent/                 ← LangGraph: graph.py, nodes.py, tools.py, state.py
│   ├── analytics/             ← Python analytics engine
│   ├── rag/                   ← FAISS + SQLite FTS5
│   └── requirements.txt
├── exercises_db/              ← 300+ exercises as JSON seed data
└── docker-compose.yml
```

---

## AGENTS.md — generate this file exactly

```markdown
# AGENTS.md — GymTracker

## Read this file at the start of every Claude Code session, before touching any code.

## Project in one sentence
Personal Android gym tracker (Progression-clone, all features free) + Python backend
+ LangGraph AI coaching agent. No paywall. No subscriptions. Built for one user.

## Model routing (check this before every task)
- Fable 5: architecture-wide decisions, schema design, LangGraph graph — only if before July 12 2026
- Opus 4.8: multi-file work, complex debugging, feature review — default after July 12
- Sonnet 4.6: single screens, single endpoints, individual bug fixes — default for focused tasks
- Haiku 4.5: mechanical edits, boilerplate, imports, renames
Use /model to switch. Check /usage before long sessions.

## Hard constraints — never violate
1. Android does NO machine learning. No TensorFlow Lite. No on-device LLM.
2. All AI runs in Python on the backend.
3. App must be fully functional offline: logging, history, local analytics, cached coaching cards.
4. No paywall, no subscription gate, no premium tier. Everything works for the owner.
5. UI: clean, minimal, functional. NO AI branding anywhere. No "Powered by Claude" labels.
   No chat bubbles on main screens. Coach screen is a separate tab, opt-in.
6. Never build Phase N+1 until Phase N passes manual testing by the developer.
7. Always read MEMORY.md before starting work.
8. Always update MEMORY.md after completing a feature or making a decision.

## Coding standards
- Kotlin: MVVM + Repository. ViewModels expose StateFlow. Compose collects state.
- Python: FastAPI async. Pydantic v2. SQLAlchemy 2.0 async. Repository pattern.
- No raw DB calls in ViewModels or route handlers. Everything through repositories.
- Comments explain WHY, not WHAT.
- Every new file: 3-line header (purpose / inputs / outputs).

## UI philosophy
The 3 reference images in the project folder are starting points, not strict rules.
Extract from them: layout structure, spacing density, color palette, component style.
Then be creative — surprise the developer with a design that is better than the reference
while staying true to the core spirit: minimal, fast, distraction-free, gym-focused.
No AI gimmicks. No excessive animations. No onboarding carousels.
The UI should feel like a precision tool, not a lifestyle app.

## When UI reference images are present
1. Read all images in the project folder at session start.
2. Extract: layout, color palette, typography weight, component shapes, spacing density.
3. Identify the design language (Material3 baseline? Custom? Dark-first? etc.)
4. Log findings in MEMORY.md under "UI decisions" before writing any Compose code.
5. Implement with creative freedom — you can improve on the reference.
6. Note every significant UI decision in MEMORY.md.
```

---

## MEMORY.md — generate this file exactly

```markdown
# MEMORY.md — GymTracker Project State

_Update after every work session. This is the project's source of truth._

## Current phase
**Phase 1 — Android MVP** (not started)

## Architecture (locked)
- Android: Kotlin, Jetpack Compose, Room, Retrofit, WorkManager, MPAndroidChart, Coroutines
- Backend: FastAPI, PostgreSQL, SQLAlchemy 2.0 async, Pydantic v2, Docker, Azure
- AI: LangGraph (not LangChain), Claude API online, rule-based offline fallback
- RAG: FAISS (exercise embeddings) + SQLite FTS5 (workout history) — no external vector DB
- Sync: offline-first, last-write-wins with timestamp, WorkManager background job
- No ML on Android. No TensorFlow Lite. No on-device LLM.

## UI decisions
_Populated from 3 reference images at session start. Fill this section before writing Compose._
- Color palette: TBD (extract from images)
- Typography: TBD
- Component style: TBD
- Dark mode: TBD
- Navigation pattern: TBD
- Creative decisions beyond reference: TBD

## Feature checklist

### Phase 1 — Android MVP (offline-first)
#### Data layer
- [ ] Room schema: ExerciseEntity, WorkoutEntity, SetEntity, ProgramEntity,
      TemplateEntity, MeasurementEntity, CoachingCardEntity
- [ ] DAOs: ExerciseDao, WorkoutDao, SetDao, ProgramDao
- [ ] Repositories wiring DAOs to ViewModels
- [ ] 300+ exercises seeded from JSON on first launch
- [ ] Data export: full JSON + CSV (per-set, not just totals)
- [ ] Data import: JSON restore

#### Workout session (core screen — most important)
- [ ] Pre-filled sets from last session (weight + reps as greyed hints)
- [ ] Drag handle on weight/reps fields (drag up/down to adjust)
- [ ] Set completion checkbox (tap → done, auto-advance to next set)
- [ ] Set tagging: W warmup, D dropset, N negative, T tempo, F failure
- [ ] Superset grouping (drag exercise onto another)
- [ ] Floating rest timer overlay bubble (survives app-switch)
- [ ] Built-in stopwatch
- [ ] Plate calculator (inline: shows which plates to load for target weight)
- [ ] 1RM badge per set (Epley >5 reps, Brzycki ≤5 reps)
- [ ] Add exercise mid-workout (modal library sheet)
- [ ] Finish workout → session summary → prompt for comment

#### Other screens
- [ ] Home screen (today's program, quick-start, recent workouts)
- [ ] Exercise library (300+ exercises, filter by muscle group + equipment)
- [ ] Custom exercise creation (name, muscle group, equipment, notes)
- [ ] Program builder (multi-week programs with day structure)
- [ ] Workout templates (reusable day definitions)
- [ ] History screen (calendar heatmap + workout list)
- [ ] Timeline view per exercise (all sets ever, scrollable)
- [ ] PR detection (auto-flag new personal records with celebration)
- [ ] Workout notes and session comments
- [ ] Plate configuration (set bar weight + available plates)
- [ ] Dark mode support
- [ ] Health Connect integration
- [ ] Settings (kg/lbs toggle, bar weight, plate setup, data management)

### Phase 2 — Analytics (all Progression Pro features, unlocked)
- [ ] Per-exercise progress graph (1RM over time, MPAndroidChart)
- [ ] Volume load chart (sets × reps × weight per week)
- [ ] Frequency heatmap (muscle groups × days)
- [ ] Plateau detection indicator (4-session window, no 1RM improvement)
- [ ] Strength standards comparison (bodyweight ratio benchmarks)
- [ ] Session duration and rest time trends
- [ ] Total volume by muscle group (weekly and monthly)
- [ ] PR timeline across all exercises
- [ ] Advanced CSV export (per-set with timestamp, exercise, weight, reps, 1RM)
- [ ] Body measurements tracker (weight, measurements over time)

### Phase 3 — Backend + cloud sync
- [ ] FastAPI scaffold + PostgreSQL schema (mirrors Room schema)
- [ ] JWT authentication
- [ ] Sync endpoints with conflict resolution (last-write-wins by updated_at)
- [ ] WorkManager background sync on Android
- [ ] Cloud backup and restore
- [ ] Docker Compose setup + Azure deployment

### Phase 4 — Agentic AI coaching layer
- [ ] LangGraph graph: router → retriever → analyst → planner → responder
- [ ] Tools: get_recent_sessions, estimate_1rm, detect_plateau,
      suggest_next_weight, search_exercises (FAISS)
- [ ] SQLite FTS5 on workout notes and exercise descriptions
- [ ] FastAPI /agent/chat streaming endpoint (Server-Sent Events)
- [ ] Android coach screen: chat UI + offline banner + cached coaching cards
- [ ] Progressive overload engine (rule-based + ML hybrid)
- [ ] Overtraining risk score (volume + frequency signals)
- [ ] PR prediction (linear regression on 1RM curve)
- [ ] Natural language workout summaries (Claude API, cached to Room)

## Known issues / blockers
_None yet_

## Session log
| Date | Model used | Work done |
|------|-----------|-----------|
| — | — | Project initialized |
```

---

## SKILL.md (backend/) — generate this file exactly

```markdown
# SKILL.md — Backend Patterns

## FastAPI async endpoint
```python
@router.post("/workouts", response_model=WorkoutResponse, status_code=201)
async def create_workout(
    payload: WorkoutCreate,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user),
) -> WorkoutResponse:
    return await workout_repo.create(db, payload, user_id=current_user.id)
```

## SQLAlchemy 2.0 async pattern
```python
async def get_by_id(self, db: AsyncSession, id: UUID) -> Exercise | None:
    result = await db.execute(select(Exercise).where(Exercise.id == id))
    return result.scalar_one_or_none()
```

## Pydantic v2 schema
```python
class WorkoutCreate(BaseModel):
    template_id: UUID | None = None
    started_at: datetime
    notes: str = ""
    sets: list[SetCreate]
```

## Analytics formulas
- 1RM Epley (>5 reps):   weight * (1 + reps / 30)
- 1RM Brzycki (≤5 reps): weight * 36 / (37 - reps)
- Volume load:            sum(weight * reps) per session
- Progressive overload:   last 2 sessions hit all target reps → suggest +2.5kg compound, +1.25kg isolation
- Plateau:                no 1RM improvement over last 4 sessions of same exercise
- Weekly volume:          sum all volume loads in 7-day window per muscle group

## LangGraph node pattern
```python
from typing import TypedDict

class AgentState(TypedDict):
    user_id: str
    query: str
    retrieved_sessions: list
    analysis: dict
    plan: dict
    response: str

def analyst_node(state: AgentState) -> AgentState:
    """Runs analytics on retrieved sessions. Input: state with retrieved_sessions. Output: state + analysis."""
    sessions = state["retrieved_sessions"]
    analysis = {
        "estimated_1rm": estimate_1rm(sessions),
        "plateau_detected": detect_plateau(sessions),
        "weekly_volume": compute_volume(sessions),
    }
    return {**state, "analysis": analysis}
```

## Android ↔ backend sync contract
```
Android sends: WorkoutSyncPayload
  { local_id: UUID, sets: SetData[], updated_at: ISO8601 }

Server returns: WorkoutSyncResponse
  { server_id: UUID, conflict: bool, resolved_workout: WorkoutData }

Conflict rule: server.updated_at > client.updated_at → server wins
```

## Plate calculator logic (also in Android utils/)
```python
def plates_for_weight(target_kg: float, bar_kg: float, available_plates: list[float]) -> list[float]:
    """Returns list of plates to load per side."""
    per_side = (target_kg - bar_kg) / 2
    plates_used = []
    for plate in sorted(available_plates, reverse=True):
        while per_side >= plate:
            plates_used.append(plate)
            per_side -= plate
    return plates_used
```
```

---

## Phase 1 build instructions

### Step 1 — Read UI reference images first
Before writing any Compose code, read all 3 images in the project folder.
Document findings in MEMORY.md → "UI decisions".
You have creative freedom — improve on the reference while keeping its spirit.

### Step 2 — Android project scaffold
```
Package: com.gymtracker
Min SDK: 26 (Android 8.0)
Target SDK: 35
Language: Kotlin
UI: Jetpack Compose with Material3
Build: Gradle Kotlin DSL
```

### Step 3 — build.gradle.kts dependencies
```kotlin
// Compose + Material3
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material:material-icons-extended")
implementation("androidx.navigation:navigation-compose:2.7.x")

// Room
implementation("androidx.room:room-runtime:2.6.x")
implementation("androidx.room:room-ktx:2.6.x")
ksp("androidx.room:room-compiler:2.6.x")

// Networking (add now, wire in Phase 3)
implementation("com.squareup.retrofit2:retrofit:2.9.x")
implementation("com.squareup.retrofit2:converter-gson:2.9.x")
implementation("com.squareup.okhttp3:logging-interceptor:4.x")

// Charts
implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

// Coroutines + ViewModel
implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.x")
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.x")

// WorkManager (wire in Phase 3)
implementation("androidx.work:work-runtime-ktx:2.9.x")

// Health Connect
implementation("androidx.health.connect:connect-client:1.1.x")
```

### Step 4 — Screen build order (do not skip ahead)
1. Theme setup (colors from UI images, typography, dark mode)
2. WorkoutSessionScreen (most used, most complex — build this first)
3. HomeScreen
4. ExerciseLibraryScreen
5. HistoryScreen
6. ProgramBuilderScreen
7. AnalyticsScreen (placeholder in Phase 1)
8. SettingsScreen

### Step 5 — WorkoutSessionScreen exact requirements
This is the screen used every single gym session. Get it right.

Must have:
- Exercise list, reorderable with drag handles
- Per exercise: sets table with columns: #, prev (hint), weight, reps, tag, ✓
- Previous values pre-filled as greyed placeholder text
- Drag handle on weight AND reps fields: drag up = +1, drag down = -1
- Set tag button: cycles W → D → N → T → F → (none) on tap
- Superset grouping via drag-and-drop (exercises grouped with visual bracket)
- Floating rest timer: persists when leaving app (use foreground service + bubble)
- 1RM badge shown immediately after a set is marked complete
- Plate calculator: appears when weight field is focused (bottom sheet or inline)
  Shows: "Bar (20kg) + 2×10 + 2×5 per side" etc.
- Add exercise FAB → exercise library as modal bottom sheet
- Top bar: workout name, elapsed time, finish button
- Finish workout → animated summary sheet → comment field → save

### Step 6 — PlateCalculator and OneRM utilities (utils/)
Implement these in Kotlin AND Python (backend/analytics/).
Both must use the same formulas documented in SKILL.md.
Kotlin versions are used offline. Python versions are used in the agent.

---

## Session startup checklist

Every Claude Code session on this project:
1. /model → switch to appropriate model for today's tasks (see routing table above)
2. Read AGENTS.md
3. Read MEMORY.md → find "Current phase" and "Known issues"
4. Read UI reference images if any new ones were added
5. Continue from where MEMORY.md session log ends
6. After work: update MEMORY.md (session log + feature checklist + any new decisions)
7. /usage → check how much of the weekly pool was consumed this session

---

## After July 12, 2026 — model fallback plan

Fable 5 will require paid usage credits ($10/M input, $50/M output).
Default to Opus 4.8 for complex work. Sonnet 4.6 for single-task work.
Architecture decisions already made in Phase 1 do not need to be revisited with Fable.
The SKILL.md and MEMORY.md files carry enough context that Opus handles everything well.
