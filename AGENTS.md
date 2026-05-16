# Godspark — Agent Context

## Project Overview
Godspark is a **Forge 1.20.1 MineColonies companion mod** that observes colony state, computes societal pressures, and generates deterministic story events. It is an observer/interpreter layer — not a replacement for MineColonies.

## What Godspark Is
- An **observer** of MineColonies colony state via reflection
- A **pressure engine** that converts colony metrics into 0-100 pressure scores
- A **storyteller** that generates event candidates from pressure thresholds
- An **event lifecycle tracker** with ACTIVE → PERSISTENT → RESOLVED states
- A **persistent memory system** that records colony traumas, patterns, and triumphs
- A **memory influence system** that adjusts event thresholds based on colony history
- An **optional local AI reflection layer** (future)

## What Godspark Is Not
- A replacement for MineColonies
- A standalone colony simulation
- An LLM-driven NPC controller
- A mod that runs AI every tick
- A mod that mutates MineColonies state (yet)

## Technical Stack
| Component | Version |
|---|---|
| Minecraft | 1.20.1 |
| Forge | 47.x |
| Java | 17 |
| MineColonies | 1.20.1-1.1.x (mandatory) |
| Create | Optional, detected at runtime |

## Architecture
```
MineColonies Simulation
      ↓
ColonyObserver (reflection-based API scanner)
      ↓
ColonySnapshot (14 fields: citizens, buildings, happiness, guards, etc.)
      ↓
PressureEngine (5 pressure types, 0-100 scale)
      ↓
EventGenerator (stateless, highest-matching event per pressure, memory-adjusted thresholds)
      ↓
EventStateManager (ACTIVE → PERSISTENT → RESOLVED lifecycle)
      ↓
MemoryEngine (transitions → ColonyMemory: SIGNIFICANT_EVENT/TRAUMA/PATTERN/TRIUMPH)
      ↓
MemoryBank (per-colony storage, dedup, reinforce, trim to 50)
      ↓
MemoryInfluence (colony memories → threshold adjustments per PressureType)
      ↘
EventGenerator (memory-adjusted thresholds)
      ↓
EventQueue (6000-tick dedup, 100 max history)
      ↓
PrayerSeedGenerator (pressures + events + memories → prayer seeds)
      ↓
PrayerSeedBank (per-colony storage, dedup by sourceKey, expire, max 20)
      ↓
Logs + /godspark commands
```

## Pressure Formulas
```
FOOD      = capacityPressure(citizens, foodBuildings, 5.0)
SECURITY  = capacityPressure(citizens, guards, 8.0) + (hasActiveRaid ? 40 : 0)
HOUSING   = housingPressure(citizens, housingCapacity)
COMFORT   = comfortPressure(citizens, happiness)  // target 7.0, not 10.0
INDUSTRY  = capacityPressure(citizens, industryBuildings, 6.0)

// capacityPressure: shortage ratio × citizenDemandFactor × 100
// citizenDemandFactor: min(1.0, citizens / 10.0) — scales 0→1 as population grows
// Fresh colonies (1 citizen) start with low pressure; crisis emerges as population outgrows infrastructure
```

## Event Thresholds
| Pressure | LOW (>N) | MEDIUM (>N) | HIGH (>N) |
|---|---|---|---|
| FOOD | 40 | 70 | 90 |
| SECURITY | 40 | 70 | 90 + activeRaid |
| HOUSING | 25 | 50 | 80 |
| COMFORT | 40 | 70 | 85 |
| INDUSTRY | 40 | 70 | 90 |

## Package Structure
```
com.godspark
├── GodsparkMod.java          — Mod entry point, static service instances
├── GodsparkConstants.java    — MOD_ID, VERSION, tick intervals
├── command/
│   └── GodsparkCommands.java — /godspark status|colonies|pressures|events|memories|prayers
├── event/
│   └── GodsparkServerEvents.java — Server tick heartbeat, throttling
├── memory/
│   ├── ColonyMemory.java     — Immutable memory record (12 fields)
│   ├── MemoryBank.java       — Per-colony storage, dedup, reinforce, trim to 50
│   ├── MemoryEngine.java     — Generates memories from event transitions (incl. TRIUMPH)
│   ├── MemoryInfluence.java  — Computes threshold adjustments from colony memories
│   └── MemoryType.java       — Enum: SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL
├── observer/
│   ├── ColonyObserver.java   — Reflection-based MineColonies API scanner
│   ├── ColonySnapshot.java   — Immutable colony state record (14 fields)
│   └── ObservedColony.java   — Tracks snapshot history per colony
├── persistence/
│   └── GodsparkSavedData.java — NBT serialization (v2), backward compat with v1
├── prayer/
│   ├── PrayerSeed.java           — Immutable prayer seed record (13 fields)
│   ├── PrayerSeedBank.java       — Per-colony prayer storage, dedup by sourceKey, expire, max 20
│   ├── PrayerSeedGenerator.java  — Generates prayer seeds from pressures/events/memories
│   └── PrayerType.java           — Enum: PLEA, VIGIL, THANKS, LAMENT, HOPE
├── pressure/
│   ├── PressureEngine.java   — Computes 5 pressure types from snapshots
│   ├── PressureSnapshot.java — Immutable pressure values record
│   └── PressureType.java     — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY
└── story/
    ├── EventGenerator.java       — Stateless: pressures → event candidates (memory-adjusted)
    ├── EventQueue.java           — Dedup (6000 ticks), history (100 max)
    ├── EventRecord.java          — Wraps StoryEvent with lifecycle state
    ├── EventSeverity.java        — Enum: LOW(1), MEDIUM(2), HIGH(3)
    ├── EventState.java           — Enum: ACTIVE(1), PERSISTENT(2), RESOLVED(3)
    ├── EventStateManager.java    — Lifecycle tracking, keyed by colonyId+PressureType
    ├── StoryEvent.java           — Immutable event data record
    └── StoryEventType.java       — 15 event types with thresholds/descriptions
```

## Key Design Decisions
- **Reflection for MineColonies API** — compile-time types incomplete, runtime access via `getMethod()`/`invoke()`
- **All reflection methods defensive** — return safe defaults on failure (0, false, 5.0)
- **Building classification uses heuristic lowercase matching** — marked TODO for proper type checks
- **EventGenerator is stateless** — pure function, testable
- **EventStateManager keyed by colonyId + PressureType** — not StoryEventType (severity changes are one continuing condition)
- **EventQueue owns display/log dedup** — EventStateManager owns lifecycle truth
- **MemoryEngine consumes transitions, not all active events** — avoids generating memories every cycle
- **Dedup by colonyId + memoryType + pressureType** — reinforcement updates intensity/count instead of duplicates
- **No time-based decay yet** — decayRate stored but unused; Phase 4B focuses on threshold influence not decay
- **Memory influence is gentle** — TRAUMA: -10, PATTERN: -7, SIGNIFICANT_EVENT: -3, TRIUMPH: +5, capped at -20/+10 per PressureType
- **Population-scaled pressure** — fresh colonies (1 citizen) start with low pressure; crisis emerges as population outgrows infrastructure
- **Locale.ROOT for all toLowerCase()** — avoids locale-sensitive bugs
- **clampPressure handles NaN/infinite, uses Math.round** — robust edge case handling
- **getSnapshots() returns Map.copyOf()** — prevents external mutation

## Tick Intervals
- Observer scan: every 600 ticks (30 seconds)
- Pressure compute + event generation: every 1200 ticks (60 seconds)
- Event dedup cooldown: 6000 ticks (5 minutes)
- Max snapshot history: 20 per colony
- Max resolved events: 500
- Max memories per colony: 50

## Build & Deploy
```powershell
# Build
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
.\gradlew build --no-daemon

# Deploy to PrismLauncher
.\deploy.ps1
```

## Test Instance
```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony
```

## In-Game Commands
```
/godspark status       — Mod status, detected mods, colony count
/godspark colonies     — List observed colonies with citizen/building counts
/godspark pressures    — Show current pressure values per colony
/godspark events       — Show active/persistent/resolved events (default 10)
/godspark events <N>   — Show N recent events (1-50)
/godspark memories          — Show top 20 memories across all colonies
/godspark memories <colonyId> — Show top 10 memories for specific colony
/godspark influences        — Show memory threshold adjustments per colony
/godspark prayers           — Show active prayer seeds across all colonies
/godspark prayers <colonyId> — Show prayer seeds for specific colony
```

## Development Phases
| Phase | Status | Description |
|---|---|---|
| 1 | ✅ DONE | Forge scaffold, MineColonies detection, commands |
| 2 | ✅ DONE | Pressure Engine — 5 pressure types, 0-100 scale |
| 3 | ✅ DONE | Story Events — 15 event types, EventQueue dedup |
| 3.5A | ✅ DONE | Event Lifecycle State — ACTIVE/PERSISTENT/RESOLVED |
| 3.5B | ✅ DONE | SavedData Persistence — events survive restarts |
| 4A | ✅ DONE | Memory System — colony memories, pattern detection |
| 4B | ✅ DONE | Memory Influence — memories affect event thresholds, TRIUMPH memories |
| 4C | ✅ DONE | Prayer Seed Generation — colony prayer language from state |
| 5 | Future | AI Reflection — local llama.cpp narrative |
| 6 | Future | Civilization Evolution — pressures unlock directions |

## Code Conventions
- Java 17, no external dependencies beyond Forge/MineColonies
- Records for immutable data (ColonySnapshot, PressureSnapshot, StoryEvent, EventRecord)
- Final classes for services (ColonyObserver, PressureEngine, EventGenerator, EventStateManager)
- Static service instances in GodsparkMod
- No comments unless explaining non-obvious behavior
- Defensive programming — null checks, safe defaults, try/catch around reflection
- Use `Locale.ROOT` for string case operations
- Use diamond operators for generic constructors

## Known Issues / TODOs
- Building classification uses heuristic string matching — replace with proper MineColonies building type checks
- Pressure formulas rebalanced — tune constants (5.0/8.0/6.0 citizens per building) after playtesting
- Comfort target happiness (7.0) not verified against actual MineColonies API
- Memory decay not yet implemented — decayRate stored but unused
- Phase 4C complete — prayer seeds generated from pressures, events, and memories
- Phase 5 (AI Reflection) — not yet started
- Phase 6 (Civilization Evolution) — not yet started

## Current Project State
- Last commit: Phase 4A/4B memory system
- Prayer seed generation (Phase 4C) implemented: PrayerSeedGenerator, PrayerSeedBank, PrayerType
- /godspark prayers command available
- Memory influence: Integrated into EventGenerator, thresholds adjust based on colony memories
- TRIUMPH memories: Implemented for high-severity resolved events
- All static services initialized in GodsparkMod: COLONY_OBSERVER, PRESSURE_ENGINE, EVENT_GENERATOR, EVENT_QUEUE, EVENT_STATE_MANAGER, MEMORY_BANK, MEMORY_ENGINE, MEMORY_INFLUENCE, PRAYER_SEED_GENERATOR, PRAYER_SEED_BANK
