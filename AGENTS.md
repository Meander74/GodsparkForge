# Godspark — Agent Context

## Project Overview
Godspark is a **Forge 1.20.1 MineColonies companion mod** that observes colony state, computes societal pressures, and generates deterministic story events. It is an observer/interpreter layer — not a replacement for MineColonies.

## What Godspark Is
- An **observer** of MineColonies colony state via reflection
- A **pressure engine** that converts colony metrics into 0-100 pressure scores
- A **storyteller** that generates event candidates from pressure thresholds
- An **event lifecycle tracker** with ACTIVE → PERSISTENT → RESOLVED states
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
EventGenerator (stateless, highest-matching event per pressure)
      ↓
EventStateManager (ACTIVE → PERSISTENT → RESOLVED lifecycle)
      ↓
EventQueue (6000-tick dedup, 100 max history)
      ↓
Logs + /godspark commands
```

## Pressure Formulas
```
FOOD      = clamp(100 - foodBuildingCount * 20)
SECURITY  = clamp(100 - guardCount * 25 + (hasActiveRaid ? 30 : 0))
HOUSING   = citizenCount > housingCapacity ? clamp((citizenCount - housingCapacity) * 30) : 0
COMFORT   = clamp((10.0 - happiness) * 10.0)  // assumes happiness 0-10
INDUSTRY  = clamp(100 - industryBuildingCount * 15)
```

## Event Thresholds
| Pressure | LOW (>N) | MEDIUM (>N) | HIGH (>N) |
|---|---|---|---|
| FOOD | 40 | 70 | 90 |
| SECURITY | 40 | 70 | 90 + activeRaid |
| HOUSING | 0 | 50 | 80 |
| COMFORT | 40 | 70 | 85 |
| INDUSTRY | 40 | 70 | 90 |

## Package Structure
```
com.godspark
├── GodsparkMod.java          — Mod entry point, static service instances
├── GodsparkConstants.java    — MOD_ID, VERSION, tick intervals
├── command/
│   └── GodsparkCommands.java — /godspark status|colonies|pressures|events
├── event/
│   └── GodsparkServerEvents.java — Server tick heartbeat, throttling
├── observer/
│   ├── ColonyObserver.java   — Reflection-based MineColonies API scanner
│   ├── ColonySnapshot.java   — Immutable colony state record (14 fields)
│   └── ObservedColony.java   — Tracks snapshot history per colony
├── pressure/
│   ├── PressureEngine.java   — Computes 5 pressure types from snapshots
│   ├── PressureSnapshot.java — Immutable pressure values record
│   └── PressureType.java     — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY
└── story/
    ├── EventGenerator.java       — Stateless: pressures → event candidates
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
- **Locale.ROOT for all toLowerCase()** — avoids locale-sensitive bugs
- **clampPressure handles NaN/infinite, uses Math.round** — robust edge case handling
- **getSnapshots() returns Map.copyOf()** — prevents external mutation

## Tick Intervals
- Observer scan: every 600 ticks (30 seconds)
- Pressure compute + event generation: every 1200 ticks (60 seconds)
- Event dedup cooldown: 6000 ticks (5 minutes)
- Max snapshot history: 20 per colony

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
```

## Development Phases
| Phase | Status | Description |
|---|---|---|
| 1 | ✅ DONE | Forge scaffold, MineColonies detection, commands |
| 2 | ✅ DONE | Pressure Engine — 5 pressure types, 0-100 scale |
| 3 | ✅ DONE | Story Events — 15 event types, EventQueue dedup |
| 3.5A | ✅ DONE | Event Lifecycle State — ACTIVE/PERSISTENT/RESOLVED |
| 3.5B | ⏭️ Next | SavedData Persistence — events survive restarts |
| 4 | Future | Memory System — colony memories, persistent history |
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
- Pressure formulas are heuristic — tune after seeing real data
- No SavedData persistence yet — events lost on restart (Phase 3.5B)
- No commit history — repo at C:\ root, no commits made yet
- Happiness scale assumption (0-10) not verified against actual MineColonies API
