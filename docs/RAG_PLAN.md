# RAG Coach — implementation plan (Python, free-tier LLM APIs)

_2026-07-17. Fits the locked architecture (AGENTS.md / MEMORY.md): all AI runs in Python
on the FastAPI backend; Android does no ML and stays fully functional offline; retrieval
is FAISS + SQLite FTS5, no external vector DB; LangGraph (not LangChain chains)._

## 0 · What the RAG coach answers

Three query families, each needing different retrieval:

| Family | Example | Best source |
|---|---|---|
| Exercise knowledge | "How do I keep tension in RDLs?" | curated exercise corpus (vector) |
| Personal history | "What did my bench e1RM do over 3 months?" | SQL/analytics **tools**, not vectors |
| Programming advice | "Should I deload? What's next for squat?" | history stats + knowledge, combined |

Key design decision: **structured questions get SQL tools, fuzzy questions get vectors.**
Forcing workout history through embeddings is the classic gym-app RAG mistake — "my best
squat in May" is a query, not a similarity search. The agent routes.

## 1 · Free LLM API options (state as of early 2026 — re-verify limits before building)

| Provider | Free tier | Notes |
|---|---|---|
| **Groq** | generous RPM/day on Llama-3.3-70B, Llama-4 family | fastest inference anywhere; OpenAI-compatible SDK |
| **Google Gemini** | Flash models free: ~10–15 RPM, hundreds of req/day | biggest daily allowance; OpenAI-compatible endpoint exists |
| **OpenRouter** | models tagged `:free`, ~50 req/day | one API over many models; good as a third fallback |
| **Mistral** | La Plateforme free tier (1 RPM-ish) | fine for dev only |
| **Ollama (local)** | unlimited, free, offline | Llama/Qwen 7–8B on your Mac; zero quota risk; quality ceiling lower |

**Recommendation: Groq primary → Gemini Flash fallback → rule-based offline cards.**
Both expose OpenAI-compatible APIs, so one client class covers them:

```python
# backend/app/llm.py
from openai import AsyncOpenAI

PROVIDERS = [
    dict(name="groq",   base_url="https://api.groq.com/openai/v1",
         key_env="GROQ_API_KEY",   model="llama-3.3-70b-versatile"),
    dict(name="gemini", base_url="https://generativelanguage.googleapis.com/v1beta/openai/",
         key_env="GEMINI_API_KEY", model="gemini-2.5-flash"),
]

async def complete(messages, **kw):
    for p in PROVIDERS:
        try:
            client = AsyncOpenAI(base_url=p["base_url"], api_key=os.environ[p["key_env"]])
            return await client.chat.completions.create(model=p["model"], messages=messages, **kw)
        except (RateLimitError, APIStatusError):
            continue            # quota burned → next provider
    return None                 # caller falls back to rule-based card
```

A day's personal usage (a handful of coach questions) sits comfortably inside Groq's free
tier alone; the chain is insurance, not load-balancing.

## 2 · Embeddings + indexes: all local, all free

- **Embedding model**: `sentence-transformers` with `BAAI/bge-small-en-v1.5` (or
  `all-MiniLM-L6-v2`). Runs on CPU in milliseconds at this corpus size. No API, no quota.
- **Vector index**: FAISS `IndexFlatIP` on normalized vectors. The corpus is ~108 curated
  exercises × a few chunks + any coaching notes — a few hundred vectors. Flat index is
  exact, instant, and serializes to one file next to the DB.
- **Keyword index**: SQLite **FTS5** table over the same chunks (and over workout/exercise
  names for entity linking). Hybrid = FTS5 top-k ∪ FAISS top-k → Reciprocal Rank Fusion.
  Hybrid matters here because gym vocabulary is exact ("RDL", "e1RM", "AMRAP") — pure
  vectors miss acronyms, pure keywords miss "hamstring hinge movement".

```python
# backend/app/rag/ingest.py  (run at startup / on catalog change)
model = SentenceTransformer("BAAI/bge-small-en-v1.5")
chunks = build_chunks()             # exercise: name+muscles+equipment+description+cues
vecs = model.encode([c.text for c in chunks], normalize_embeddings=True)
index = faiss.IndexFlatIP(vecs.shape[1]); index.add(vecs)
faiss.write_index(index, "data/exercises.faiss")
# FTS5: CREATE VIRTUAL TABLE chunks_fts USING fts5(text, chunk_id UNINDEXED)
```

Corpus sources, in order of value:
1. `exercises_db/exercise_catalog.json` — the 108 curated entries (already in-repo).
2. A `docs/knowledge/` folder of short markdown notes you curate over time (form cues,
   progression rules, deload heuristics). This becomes the coach's "voice" — and it's
   the highest-leverage place to invest, because it's *your* training philosophy.
3. Later: imported coaching cards, so old advice is retrievable.

## 3 · The LangGraph agent

Small graph, deterministic edges — not an open-ended tool loop:

```
        ┌────────┐
q ────► │ router │  (one cheap LLM call classifies: knowledge / history / mixed)
        └───┬────┘
   ┌────────┼──────────┐
   ▼        ▼          ▼
retrieve  history    both
(hybrid    tools    (parallel)
 RAG)    (SQL/stats)
   └────────┼──────────┘
            ▼
        ┌────────┐     ┌─────────────┐
        │generate│ ──► │ card builder│ ──► JSON CoachingCard
        └────────┘     └─────────────┘
```

- **history tools** are plain Python functions over the synced workout DB (Postgres
  server-side): `best_e1rm(exercise, window)`, `volume_by_week(muscle)`,
  `last_sessions(exercise, n)`, `freshness()`. The LLM never sees raw tables — tools
  return small typed dicts. This is both safer and cheaper than text-to-SQL.
- **generate** gets: the question, retrieved chunks (with ids), tool outputs, and a
  system prompt enforcing the Forged voice (restrained, no hype) + "cite chunk ids you
  used". Citations let the app show "based on: Romanian Deadlift notes".
- **card builder** validates the output against a Pydantic schema
  (`CoachingCard{title, body, kind, citations[], generated_at}`); a malformed generation
  retries once, then falls back to rule-based.
- **Offline / quota-exhausted path**: the rule-based generator (already planned in the
  architecture) produces cards from the same history tools — progression stalls,
  imbalances (push/pull volume ratio), freshness suggestions. The app can't tell the
  difference; cards are cards.

## 4 · API surface (FastAPI)

```
POST /coach/ask        {question}            → CoachingCard (+ sources)
GET  /coach/cards      ?since=…              → cards generated server-side (weekly digest)
POST /sync/…           (existing plan)       → history the tools run against
```

Android side (Phase-4 Coach tab, opt-in per AGENTS.md): Retrofit call, render the card,
**persist every card to `CoachingCardEntity`** so the Coach tab works offline with the
last N cards cached. No streaming needed for v1 — cards arrive whole.

## 5 · Quota & cost discipline

- **Cache aggressively**: hash(question + relevant-history-version) → card, in Postgres.
  Asking "how's my bench?" twice in a week costs one LLM call.
- **Router is the only always-on call**; keep it tiny (single label output, ~20 tokens).
  If even that's too much, replace with regex/keyword routing first — upgrade later.
- **Batch server-side digests** (one weekly "state of the forge" card) instead of many
  small calls.
- Track usage in a `llm_calls` table (provider, tokens, latency) from day one — free
  tiers change, and you'll want the data when deciding whether to pay.

## 6 · Build order (each step is testable alone)

1. **Ingest + hybrid retrieval** (pure Python, no LLM): script builds FAISS + FTS5 from
   the catalog; a pytest asserts "romanian deadlift cue" retrieves the RDL chunk. 
2. **History tools** over the dev database (import your real JSON export into Postgres
   via the existing backend models when they land — or start with SQLite locally).
3. **`/coach/ask` with Groq only**, no graph — retrieval → prompt → card. Prove quality.
4. **LangGraph router + tools** — split the three query families.
5. **Fallback chain + cache + usage table.**
6. **Android Coach tab** (Phase 4 gate: only after Phases 1–3 pass your manual testing,
   per AGENTS.md rule 6).

## 7 · Risks / honest caveats

- Free-tier limits drift constantly; the provider table above is a snapshot. The
  OpenAI-compatible client makes swapping a config change, not a rewrite.
- 7B-class local models via Ollama are noticeably weaker at programming advice; keep
  them as a dev convenience, not the product path.
- Don't let the coach prescribe medical anything — system prompt guardrail + card kinds
  limited to training topics.
- The knowledge folder is the moat. Retrieval over 108 catalog blurbs alone will feel
  thin; ten good markdown notes about *your* progression rules will feel like a coach.
