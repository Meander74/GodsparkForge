# Godspark Mod — Review Package for GPT

## Project Identity

Godspark is a **Forge 1.20.1 MineColonies companion mod** (v0.1.0). It observes colony state via reflection, computes societal pressures (0-100), generates story events, maintains persistent colony memories (traumas/patterns/triumphs), generates prayer seeds, and now has a Phase 5A AI reflection layer.

**Tech**: Java 17, Forge 47.x, MineColonies 1.20.1-1.1.x (mandatory), Create (optional).

**What it is NOT**: A replacement for MineColonies, a standalone sim, an LLM-driven NPC controller, or a mod that runs AI every tick.

---

## Architecture (Data Flow)

```
MineColonies API
    ↓ (reflection)
ColonyObserver.scan() [every 600 ticks]
    ↓
ColonySnapshot (14 fields: citizens, buildings, happiness, guards, etc.)
    ↓
ObservedColony (history of 20 snapshots)
    ↓
PressureEngine.compute() [every 1200 ticks]
    ↓
PressureSnapshot (5 values: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY, 0-100)
    ↓
EventGenerator.generate() [stateless, memory-adjusted thresholds]
    ↓
EventStateManager.processEvents() [ACTIVE → PERSISTENT → RESOLVED]
    ↓
MemoryEngine.generateMemories() [from transitions]
    ↓
MemoryBank (per-colony, dedup, reinforce, trim 50)
    ↓
MemoryInfluence [TRAUMA:-10, PATTERN:-7, SIGNIFICANT:-3, TRIUMPH:+5, cap -20/+10]
    ↓ (feeds back to EventGenerator thresholds)
PrayerSeedGenerator [pressures + events + memories → prayer seeds]
    ↓
PrayerSeedBank (per-colony, dedup by sourceKey, expire, max 20)
    ↓
NEW: AiReflectionService [template or AI → colony reflection]
    ↓
Logs + /godspark commands
```

---

## Pressure Formulas

```
FOOD      = capacityPressure(citizens, foodBuildings, 5.0)
SECURITY  = capacityPressure(citizens, guards, 8.0) + (hasActiveRaid ? 40 : 0)
HOUSING   = housingPressure(citizens, housingCapacity)
COMFORT   = comfortPressure(citizens, happiness)  // target 7.0
INDUSTRY  = capacityPressure(citizens, industryBuildings, 6.0)

capacityPressure: shortageRatio × citizenDemandFactor × 100
citizenDemandFactor: min(1.0, citizens / 10.0) — scales 0→1 as population grows
```

## Event Thresholds

| Pressure | LOW (>N) | MEDIUM (>N) | HIGH (>N) |
|---|---|---|---|
| FOOD | 40 | 70 | 90 |
| SECURITY | 40 | 70 | 90 + activeRaid |
| HOUSING | 25 | 50 | 80 |
| COMFORT | 40 | 70 | 85 |
| INDUSTRY | 40 | 70 | 90 |

---

## Phase Completion Status

| Phase | Status |
|---|---|
| 1 — Forge scaffold, commands | DONE |
| 2 — Pressure Engine | DONE |
| 3 — Story Events (15 types) | DONE |
| 3.5A — Event Lifecycle (ACTIVE/PERSISTENT/RESOLVED) | DONE |
| 3.5B — SavedData Persistence (NBT v2) | DONE |
| 4A — Memory System (colony memories, patterns) | DONE |
| 4B — Memory Influence (threshold adjustments) | DONE |
| 4C — Prayer Seed Generation | DONE |
| **5A — AI Reflection (template + HTTP bridge)** | **JUST BUILT** |
| 5B — Player prayer interpretation | Future |
| 5C — Minor miracles | Future |
| 6 — Civilization Evolution | Future |

---

## What Was Built This Session (5 Commits)

### Commit 1 — Building Classification Stabilization

**Problem**: Building counting used loose keyword matching on toString() output (e.g., `key.contains("guardtower")`). Fragile, false positives, tight coupling to display strings.

**Solution**:
- New enum `BuildingCategory` (FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY, WAREHOUSE, OTHER)
- New class `BuildingClassifier` with:
  - **Primary**: Map of 55 MineColonies registry-name identifiers → categories (e.g., `"restaurant" → FOOD`, `"guardtower" → SECURITY`)
  - **Secondary**: Keyword fallback matching for unrecognized types
  - **Tertiary**: Log unknown building types once via `LOGGED_UNKNOWNS` HashSet
- Extracts identifier from keys by splitting on `:` (registry name) or `.` (translation key)
- `ColonyObserver` refactored: 4 keyword-list methods replaced by single `countByCategory(colony, categories...)`

**Files**: `BuildingCategory.java`, `BuildingClassifier.java`, `ColonyObserver.java` (modified)

### Commit 2 — Playtest Summary Logging

**Added**: `logColonySummary()` in `GodsparkServerEvents` — runs at end of each pressure cycle (every 1200 ticks). Prints one compact block per colony:

```
[Godspark Summary] Colony #1 (New Haven): citizens=12 buildings=8 housing=10 happy=7.4 raid=false | foodBuildings=1 guards=0 industryBuildings=3 warehouse=1
  PRESSURES: Food=50 Security=65 Housing=0 Comfort=0 Industry=30
  EVENTS: Food_MEDIUM(ACTIVE) | Security_LOW(ACTIVE)
  MEMORIES: Food:Trauma(60) | Security:Pattern(45)
  PRAYERS: PLEA(70) | VIGIL(55)
```

### Commit 3 — Template Reflection Service

**New command**: `/godspark reflect <colonyId>`

**New record**: `AiReflection( schemaVersion, colonyId, colonyName, dominantPressure, mood, intensity, reflection, oracleText, reasonCodes, tags, confidence )`

**`TemplateReflectionService`** — deterministic, zero-AI, pure Java:
- Finds dominant pressure (highest value across 5 types)
- Classifies severity by threshold ranges
- Computes mood: `{HIGH→"Crisis"/"Desperate", MEDIUM→"Uneasy"/"Anxious"/"Cautious", LOW→"Concerned"/"Wary"/"Relieved", none→"Stable"/"Hopeful"}`
- Builds reflection narrative from event descriptions + memory references + trauma/pattern/triumph modifiers
- Builds oracle text from top prayer seed content
- Generates reasonCodes (e.g., `FOOD_PRESSURE_HIGH`, `FOOD_TRAUMA`, `FOOD_INFLUENCE_LOWER`)
- Generates tags (e.g., `["food", "anxious", "trauma", "pattern", "scarred"]`)

### Commit 4 — AI HTTP Bridge

**`AiConfig`**: Config record — `enabled=false` by default, endpoint `http://127.0.0.1:8080/v1/chat/completions`, timeout 10s, cooldown 12000 ticks (10 min), maxTokens 256, temperature 0.7.

**`AiClient`**: Wraps `java.net.http.HttpClient` — async POST to OpenAI-compatible endpoint, Gson for JSON parsing, extracts `choices[0].message.content`.

**`AiPromptBuilder`**: Builds system prompt (JSON schema instructions) + user prompt (full colony data dump: citizens, buildings, pressures, events, memories, prayers, influences).

**`AiResponseParser`**: Validates LLM JSON — normalizes pressure names (case-insensitive match to enum), normalizes mood (case-insensitive match to known values + fallback keyword extraction), clamps intensity 0-100 and confidence 0.0-1.0, extracts string lists safely.

**`AiReflectionService`** (orchestrator):
```
if (!config.enabled) → TemplateReflectionService
if (on cooldown) → TemplateReflectionService
try:
    call AiClient → parse JSON → return
catch (timeout / HTTP failure / parse failure):
    log warning → fallback to TemplateReflectionService
```

**Wired**: `AI_REFLECTION_SERVICE` instance added to `GodsparkMod`, `/godspark status` shows actual AI state, `/godspark reflect` delegates through orchestrator.

### Commit 5 — JSON AI Reflection (merged with Commit 4)

See AiResponseParser above. The response parser handles:
- `dominantPressure` → normalized to PressureType display name ("Food", "Security", etc.)
- `mood` → normalized to known mood vocabulary ("Crisis", "Uneasy", "Anxious", etc.)
- Missing JSON fields → safe defaults
- Out-of-range values → clamped
- Non-JSON response → `extractJson()` finds first `{...}` in the raw string
- All failures → clean fallback to template

---

## Package Structure (30 Java Files)

```
com.godspark/
├── GodsparkMod.java              — Entry point, 10 static service instances
├── GodsparkConstants.java        — MOD_ID, VERSION, tick intervals
├── ai/                           — NEW PACKAGE (6 files)
│   ├── AiReflection.java         — Record (12 fields)
│   ├── AiConfig.java             — Config record
│   ├── AiClient.java             — HTTP client (java.net.http)
│   ├── AiPromptBuilder.java      — System + user prompt construction
│   ├── AiResponseParser.java     — JSON validation + normalization
│   ├── AiReflectionService.java  — Orchestrator (config → template or AI)
│   └── TemplateReflectionService.java — Deterministic template reflection
├── command/
│   └── GodsparkCommands.java     — 8 commands (status, colonies, pressures, events, memories, influences, prayers, reflect)
├── event/
│   └── GodsparkServerEvents.java — Tick heartbeat, lifecycle, summary logging
├── memory/
│   ├── ColonyMemory.java         — Record (12 fields, incl. decayRate — unused)
│   ├── MemoryBank.java           — Per-colony storage, dedup, reinforce, trim 50
│   ├── MemoryEngine.java         — Transitions → memories (incl. TRIUMPH)
│   ├── MemoryInfluence.java      — Threshold adjustments
│   └── MemoryType.java           — Enum: SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL
├── observer/
│   ├── ColonyObserver.java       — Reflection-based MineColonies scanner
│   ├── ColonySnapshot.java       — Record (14 fields)
│   ├── ObservedColony.java       — Snapshot history (20 max)
│   ├── BuildingCategory.java     — NEW: Enum (7 values)
│   └── BuildingClassifier.java   — NEW: Registry-name mapping + keyword fallback
├── persistence/
│   └── GodsparkSavedData.java    — NBT serialization v2
├── prayer/
│   ├── PrayerSeed.java           — Record (13 fields)
│   ├── PrayerSeedBank.java       — Per-colony, dedup, expire, max 20
│   ├── PrayerSeedGenerator.java  — Pressures + events + memories → seeds
│   └── PrayerType.java           — Enum: PLEA, VIGIL, THANKS, LAMENT, HOPE
├── pressure/
│   ├── PressureEngine.java       — 5 pressure types computation
│   ├── PressureSnapshot.java     — Record (colonyId, values map, gameTick)
│   └── PressureType.java         — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY
└── story/
    ├── EventGenerator.java       — Stateless: pressures → event candidates
    ├── EventQueue.java           — 6000-tick dedup, 100 max
    ├── EventRecord.java          — Wraps StoryEvent with lifecycle state
    ├── EventSeverity.java        — Enum: LOW(1), MEDIUM(2), HIGH(3)
    ├── EventState.java           — Enum: ACTIVE(1), PERSISTENT(2), RESOLVED(3)
    ├── EventStateManager.java    — Lifecycle tracking
    ├── StoryEvent.java           — Record (9 fields)
    └── StoryEventType.java       — Enum: 15 event types with thresholds
```

---

## Code Conventions

- Java 17, no external deps beyond Forge/MineColonies/Gson
- **Records** for immutable data (ColonySnapshot, PressureSnapshot, StoryEvent, EventRecord, ColonyMemory, PrayerSeed, AiReflection, AiConfig)
- **Final classes** for all services (cannot be extended)
- **Static service instances** in GodsparkMod — global singletons
- **Defensive reflection**: all methods return safe defaults on failure (0, false, 5.0, 7.0), wrapped in try/catch
- **Locale.ROOT** for all `toLowerCase()` — avoids locale-sensitive bugs
- **Diamond operators** for generic constructors
- **Map.copyOf()** for getters returning collections — prevents external mutation
- **No comments** unless explaining non-obvious behavior
- **clampPressure()** handles NaN/Infinity explicitly

---

## Known TODOs / Open Issues

1. **Building classification map may be incomplete** — logs unknowns at runtime; the map covers 55 known types but modpacks/addons may add more
2. **Pressure formulas need tuning with real data** — formulas designed on paper, not validated against live colony data
3. **Memory decay not implemented** — `decayRate` field stored on every memory but never applied. No memories ever fade
4. **Industry building count double-counts warehouses** — `getSafeIndustryBuildingCount()` counts both INDUSTRY + WAREHOUSE categories
5. **Warehouse count is tracked but not used** — `warehouseCount` in ColonySnapshot is stored but never fed into any pressure formula
6. **Template reflection mood mapping is somewhat arbitrary** — trauma/triumph modifiers are reasonable but not tuned
7. **AI blocks the command thread** — `CompletableFuture.get(timeout)` blocks the server thread briefly (acceptable for single-player with local LLM, problematic for servers)
8. **AiConfig is hardcoded DEFAULT** — no config file loading. User must edit source to enable AI
9. **No test coverage** — zero unit tests, integration tests, or automated validation scripts
10. **Phase 5B (player prayer interpretation) and 5C (minor miracles) not started**
11. **Phase 6 (Civilization Evolution) not started** — would introduce CivilizationPath enum, directional evolution from dominant pressures

---

## Review Prompts for GPT

Please review the codebase above and answer:

### Architecture & Design
1. Is the architecture sound for a Forge mod? The entire pipeline runs every 1200 ticks on the server thread — any concurrency concerns?
2. The `BuildingClassifier` uses a static `REGISTRY_MAP` of 55 entries. Is this the right approach vs. runtime discovery or a data-driven config?
3. The `TemplateReflectionService` combines mood computation, severity classification, and narrative generation in one class. Should these concerns be separated?
4. The `AiReflectionService` orchestrator pattern (config check → cooldown → AI attempt → fallback to template) — is this defensively sound?

### Pressure & Event Logic
5. Pressure formulas use `citizenDemandFactor = min(1.0, citizens/10.0)`. This means a fresh 1-citizen colony has 10% factor. Is this sensible for early-game feel?
6. Memory influence caps at -20/+10 per PressureType. Is this gentle enough? Too gentle? Should it compound differently (e.g., multiplicative instead of additive)?
7. The `EventStateManager` resolves events after 2 cycles of missing pressure. Is 2 cycles (2 minutes) too aggressive?

### AI Reflection Layer
8. The AI system prompt asks for structured JSON. Is the prompt robust enough for smaller local models (7B-13B)?
9. `AiResponseParser` normalizes moods by keyword matching. Is the fallback robust enough for an LLM that misbehaves?
10. The cooldown is 12000 ticks (10 minutes). Is this the right balance for an on-demand reflection tool?
11. The `extractJson()` method finds the first `{` and last `}` — what if the LLM outputs multiple JSON blocks or wraps in markdown code fences?

### Game Balance & Feel
12. The mood system maps pressure severity + memory presence to mood labels. Does the vocabulary (Crisis/Uneasy/Anxious/Concerned/Stable/Hopeful) cover the emotional range well for a colony story?
13. What pressure ranges should produce "interesting" colony stories (not boringly stable, not perpetually doomed)?
14. Should `warehouseCount` feed into any pressure formula (e.g., INDUSTRY or COMFORT)?

### Next Steps
15. Before Phase 5B (player prayer interpretation), should the team fix memory decay first? Or is it more important to see how reflections feel without decay?
16. For Phase 6 (Civilization Evolution), what's the minimum viable scope? A CivilizationPath enum + a tag per colony? Structure unlocks? In-game visual effects?
17. What's the most impactful single change that would improve player experience immediately?

---

## Files for Reference

All 30 Java source files are under:
`C:\Users\Suttawat\GodsparkForge\src\main\java\com\godspark\`

Full documentation is at:
`C:\Users\Suttawat\GodsparkForge\docs\godspark-reference.md`
