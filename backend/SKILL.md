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
