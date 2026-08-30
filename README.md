# Forged

**A personal, offline-first gym tracker — every Progression Pro feature, no paywall.**

Forged is a precision workout logging app for Android with a chalk-and-iron design language, local analytics, and a planned cloud + AI coaching layer. Built for one lifter, free forever.

<p align="center">
  <a href="https://github.com/AyoubAZIAMIMER/Gym-Tracking-Android-App/stargazers">
    <img src="https://img.shields.io/github/stars/AyoubAZIAMIMER/Gym-Tracking-Android-App?style=for-the-badge&logo=github&logoColor=white&labelColor=0E0D0B&color=FF5A1F" alt="GitHub stars" />
  </a>
  <a href="https://github.com/AyoubAZIAMIMER/Gym-Tracking-Android-App/forks">
    <img src="https://img.shields.io/github/forks/AyoubAZIAMIMER/Gym-Tracking-Android-App?style=for-the-badge&logo=github&logoColor=white&labelColor=0E0D0B&color=9FB6C2" alt="GitHub forks" />
  </a>
  <img src="https://img.shields.io/badge/Phase%201-MVP%20Live-FFC93C?style=for-the-badge&labelColor=0E0D0B" alt="Phase 1 MVP" />
  <a href="LICENSE">
    <img src="https://img.shields.io/badge/License-MIT-FFC93C?style=for-the-badge&labelColor=0E0D0B" alt="License" />
  </a>
</p>

---

## Tech Stack

<p align="center">
  <a href="https://skillicons.dev">
    <img src="https://skillicons.dev/icons?i=kotlin,android,gradle,py,fastapi,docker,postgres,azure&perline=8" alt="Technology icons" />
  </a>
</p>

### Android (live)

<p>
  <img src="https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?style=flat-square&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Jetpack%20Compose-BOM%202025.05-4285F4?style=flat-square&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Material%203-1.3-757575?style=flat-square&logo=materialdesign&logoColor=white" alt="Material 3" />
  <img src="https://img.shields.io/badge/Room-2.6.1-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Room" />
  <img src="https://img.shields.io/badge/Retrofit-2.9.0-3E4348?style=flat-square&logo=retrofit&logoColor=white" alt="Retrofit" />
  <img src="https://img.shields.io/badge/OkHttp-4.12-3E4348?style=flat-square&logo=okhttp&logoColor=white" alt="OkHttp" />
  <img src="https://img.shields.io/badge/Coroutines-1.8.1-0095D5?style=flat-square&logo=kotlin&logoColor=white" alt="Coroutines" />
  <img src="https://img.shields.io/badge/WorkManager-2.9.1-3DDC84?style=flat-square&logo=android&logoColor=white" alt="WorkManager" />
  <img src="https://img.shields.io/badge/Navigation%20Compose-2.8.4-4285F4?style=flat-square&logo=android&logoColor=white" alt="Navigation Compose" />
  <img src="https://img.shields.io/badge/Haze-1.6.10-FF5A1F?style=flat-square&logo=android&logoColor=white" alt="Haze" />
  <img src="https://img.shields.io/badge/Gradle-8.9-02303A?style=flat-square&logo=gradle&logoColor=white" alt="Gradle" />
  <img src="https://img.shields.io/badge/Android%20SDK-35-3DDC84?style=flat-square&logo=android&logoColor=white" alt="Android SDK" />
  <img src="https://img.shields.io/badge/JDK-17-ED8B00?style=flat-square&logo=openjdk&logoColor=white" alt="JDK" />
  <img src="https://img.shields.io/badge/MVVM-Architecture-9FB6C2?style=flat-square&logo=android&logoColor=white" alt="MVVM" />
</p>

### Backend (Phase 3 — scaffolded)

<p>
  <img src="https://img.shields.io/badge/FastAPI-Async-009688?style=flat-square&logo=fastapi&logoColor=white" alt="FastAPI" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=flat-square&logo=postgresql&logoColor=white" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/SQLAlchemy-2.0%20Async-D71F00?style=flat-square&logo=sqlalchemy&logoColor=white" alt="SQLAlchemy" />
  <img src="https://img.shields.io/badge/Pydantic-v2-E92063?style=flat-square&logo=pydantic&logoColor=white" alt="Pydantic" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker&logoColor=white" alt="Docker" />
  <img src="https://img.shields.io/badge/Azure-Deploy-0078D4?style=flat-square&logo=microsoftazure&logoColor=white" alt="Azure" />
</p>

### AI coaching (Phase 4 — planned)

<p>
  <img src="https://img.shields.io/badge/LangGraph-Agent-FF5A1F?style=flat-square&logo=langchain&logoColor=white" alt="LangGraph" />
  <img src="https://img.shields.io/badge/Claude%20API-Coaching-191919?style=flat-square&logo=anthropic&logoColor=white" alt="Claude API" />
  <img src="https://img.shields.io/badge/FAISS-Embeddings-0466C8?style=flat-square&logo=meta&logoColor=white" alt="FAISS" />
  <img src="https://img.shields.io/badge/SQLite%20FTS5-RAG-003B57?style=flat-square&logo=sqlite&logoColor=white" alt="SQLite FTS5" />
</p>

---

## Features

| Area | Highlights |
|------|------------|
| **Onboarding** | First-run profile + pick-your-split picker with per-muscle load bars and honest time estimates |
| **Home (the Slate)** | Live session card while training, today's plan, week strip, next up, recent — state-first, not a dashboard |
| **Workout session** | Full-screen Strike Mode, scrub-to-adjust weight/reps, set tags, supersets, in-app rest timer + live notification with "Log set", plate calculator, live PR detection with an on-screen flash |
| **Finish** | Celebration ladder (closing ring + checkmark, no confetti), rolled-up stats, PR ledger, volume-vs-last delta |
| **Library** | 108 exercises, muscle-target figures, demo photos, custom exercise CRUD, Progression import |
| **History** | Calendar heatmap, workout log, PR stars, repeat workout |
| **Stats** | e1RM trends, weekly volume, plateau detection, PR timeline, training calendar |
| **Body** | Per-muscle readiness map (COOLED → GLOWING) with anatomical front/back figures |
| **Programs** | PPL / Upper-Lower / Bro Split templates, multi-day program editor |
| **Data** | Progression `.pgnbkp` import, JSON + CSV export, profile setup |

> No subscriptions. No premium tier. No on-device ML. AI coaching arrives in Phase 4 as an opt-in tab.

---

## In motion

<p align="center">
  <img src="screenshots/tour.gif" width="220" alt="Tour of Home, Plan, Body and Stats" />
  <img src="screenshots/session-flow.gif" width="220" alt="Starting a session, logging a set, live PR flash" />
</p>

## Screenshots

<p align="center">
  <img src="screenshots/home.png" width="200" alt="Home — the Slate" />
  <img src="screenshots/session.png" width="200" alt="Workout session with rest timer" />
  <img src="screenshots/session_complete.png" width="200" alt="Finish screen — celebration ladder" />
  <img src="screenshots/plan.png" width="200" alt="Plan screen" />
  <img src="screenshots/library.png" width="200" alt="Exercise library" />
  <img src="screenshots/exercise.png" width="200" alt="Exercise detail with demo photo and muscle figure" />
  <img src="screenshots/history.png" width="200" alt="History screen" />
  <img src="screenshots/body.png" width="200" alt="Body readiness map" />
  <img src="screenshots/stats.png" width="200" alt="Stats charts" />
</p>

---

## Star History

<p align="center">
  <a href="https://star-history.com/#AyoubAZIAMIMER/Gym-Tracking-Android-App&Date">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://api.star-history.com/svg?repos=AyoubAZIAMIMER/Gym-Tracking-Android-App&type=Date&theme=dark" />
      <source media="(prefers-color-scheme: light)" srcset="https://api.star-history.com/svg?repos=AyoubAZIAMIMER/Gym-Tracking-Android-App&type=Date" />
      <img alt="Star History Chart" src="https://api.star-history.com/svg?repos=AyoubAZIAMIMER/Gym-Tracking-Android-App&type=Date" width="600" />
    </picture>
  </a>
</p>

<p align="center">
  <a href="https://github.com/AyoubAZIAMIMER/Gym-Tracking-Android-App">
    <img src="https://img.shields.io/github/stars/AyoubAZIAMIMER/Gym-Tracking-Android-App?label=Stars&logo=github&style=social" alt="GitHub Stars social badge" />
  </a>
</p>

---

## Project Structure

```
Gym-Tracking-Android-App/
├── android/                 # Kotlin + Jetpack Compose app
│   └── app/src/main/java/com/gymtracker/
│       ├── data/            # Room DB, repositories, importers
│       ├── domain/          # Analytics engine (pure Kotlin)
│       ├── ui/              # Screens, components, theme, motion
│       ├── service/         # Rest timer foreground service
│       └── utils/           # 1RM, plate calc, formatting
├── backend/                 # FastAPI scaffold (Phase 3)
├── design/                  # Motion design system (MOTION.md) + UI references
├── exercises_db/            # Exercise catalog assets
├── screenshots/             # App screenshots + GIFs used in this README
├── licenses/                # Third-party license texts (font, exercise data)
├── docs/                    # Project history / original build prompt
├── docker-compose.yml       # Postgres + API (Phase 3)
├── LICENSE                  # MIT
├── AGENTS.md                # Agent / contributor constraints
└── MEMORY.md                # Living project state
```

---

## Getting Started

### Prerequisites

- **JDK 17**
- **Android SDK 35** (API 26+ devices supported)
- Android Studio Ladybug or newer (recommended)

### Build & run

```bash
cd android
./gradlew assembleDebug
```

Install the APK on a device or emulator (the build output lands under the Gradle project cache, not `android/app/build`):

```bash
adb install ~/.gradle-build/forged/GymTracker/app/outputs/apk/debug/app-debug.apk
```

> After large dependency bumps, run `./gradlew clean` first — incremental dexing can mix Compose versions and crash at launch.

### Import your data

1. Open **Data** from the Home screen gear icon.
2. Import a **Progression** `.pgnbkp` backup.
3. Optionally run **Name imported exercises (CSV)** with a Progression CSV export for full exercise names.

---

## Architecture

```mermaid
flowchart LR
    subgraph Android["Android (offline-first)"]
        UI[Compose UI]
        VM[ViewModels]
        Repo[Repositories]
        Room[(Room SQLite)]
        UI --> VM --> Repo --> Room
    end

    subgraph Backend["Backend (Phase 3)"]
        API[FastAPI]
        PG[(PostgreSQL)]
        API --> PG
    end

    subgraph AI["AI Layer (Phase 4)"]
        LG[LangGraph]
        RAG[FAISS + FTS5]
        LG --> RAG
    end

    Repo -.->|WorkManager sync| API
    API -.-> LG
```

| Phase | Status | Scope |
|-------|--------|-------|
| **1** | In progress | Android MVP — logging, history, programs, import/export |
| **2** | Partial | Local analytics — charts, PRs, plateau, calendar heatmap |
| **3** | Scaffolded | FastAPI + PostgreSQL sync, cloud backup |
| **4** | Planned | LangGraph coaching agent, streaming chat, cached cards |

---

## Design

Forged uses the **Chalk & Iron** identity — chalk-white chrome (numerals, primary actions, the F-cut wordmark) on deep charcoal, with warm color spent strictly as data: olive for completed work, blue for a fresh muscle, red for spent/PR badges, gold for a personal record. Home and the tab screens are flat, hairline-ruled sections rather than glass cards; Anton carries the big numerals and the wordmark. A motion system built around steel-like physics still governs every transition (see `design/MOTION.md`), and the finish screen closes each session with a drawn checkmark ring instead of confetti.

---

## Contributing

This is a personal project built for a single owner. Issues and ideas are welcome, but the roadmap follows `MEMORY.md` phase gates — Phase N must pass manual testing before Phase N+1 begins.

---

<p align="center">
  <strong>Chalk on. Iron waits.</strong><br/>
  <sub>Package: <code>com.gymtracker</code> · Display name: <strong>Forged</strong> · v0.1.0</sub>
</p>
