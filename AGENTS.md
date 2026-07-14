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
