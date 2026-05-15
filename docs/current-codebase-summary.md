# Current Codebase Summary

## Project
**Godspark** — Forge 1.20.1 MineColonies companion mod

## Version
0.1.0

## Total Files
23 Java files, ~2,000+ lines of code

## Package Breakdown

| Package | Files | Lines | Purpose |
|---|---|---|---|
| `com.godspark` | 2 | ~60 | Mod entry point, constants |
| `com.godspark.command` | 1 | ~210 | Brigadier commands |
| `com.godspark.event` | 1 | ~130 | Server tick lifecycle |
| `com.godspark.memory` | 4 | ~350 | Memory system (Phase 4A) |
| `com.godspark.observer` | 3 | ~310 | MineColonies reflection scanner |
| `com.godspark.persistence` | 1 | ~380 | NBT SavedData persistence |
| `com.godspark.pressure` | 3 | ~100 | Pressure computation |
| `com.godspark.story` | 8 | ~450 | Event lifecycle system |

## Completed Phases

| Phase | Status | Description |
|---|---|---|
| 1 | ✅ DONE | Forge scaffold, MineColonies detection, commands |
| 2 | ✅ DONE | Pressure Engine — 5 pressure types, 0-100 scale |
| 3 | ✅ DONE | Story Events — 15 event types, EventQueue dedup |
| 3.5A | ✅ DONE | Event Lifecycle State — ACTIVE/PERSISTENT/RESOLVED |
| 3.5B | ✅ DONE | SavedData Persistence — events survive restarts |
| 4A | ✅ DONE | Memory System — colony memories, pattern detection |
| 4B | ⏭️ Next | Memory Influence — memories affect event generation |
| 4C | ⏭️ Future | Prayer Seed Generation |
| 5 | Future | AI Reflection — local llama.cpp narrative |
| 6 | Future | Civilization Evolution — pressures unlock directions |

## Key Classes

### Entry Point
- **GodsparkMod** — Static service instances, Forge event bus registration

### Observer Layer
- **ColonyObserver** — Reflection-based MineColonies API scanner (250 lines)
- **ColonySnapshot** — Immutable record: colonyId, name, citizens, buildings, happiness, guards, etc.
- **ObservedColony** — Tracks latest snapshot + history of 20

### Pressure Layer
- **PressureEngine** — Computes 5 pressure types (0-100) from ColonySnapshot
- **PressureSnapshot** — Immutable record: colonyId, Map<PressureType, Integer>, gameTick
- **PressureType** — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY

### Story Layer
- **EventGenerator** — Stateless: PressureSnapshot + ObservedColony → List<StoryEvent>
- **EventQueue** — 6000-tick dedup, 100 max history
- **EventStateManager** — Lifecycle: ACTIVE → PERSISTENT → RESOLVED
- **EventRecord** — Wraps StoryEvent with state, persistenceCount, missingCycles
- **StoryEventType** — 15 event types (5 pressures × 3 severities)

### Memory Layer (Phase 4A)
- **MemoryEngine** — Generates memories from event transitions
- **MemoryBank** — Per-colony storage, dedup, reinforce, trim to 50
- **ColonyMemory** — Record: colonyId, name, type, pressure, intensity, content, etc.
- **MemoryType** — Enum: SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL

### Persistence
- **GodsparkSavedData** — NBT serialization (v2), backward compat with v1
- Stores: activeEvents, resolvedEvents, memories

### Lifecycle
- **GodsparkServerEvents** — ServerStarted/Stopping/Stopped/Tick event handlers

### Commands
- **GodsparkCommands** — /godspark status|colonies|pressures|events|memories

## Dependencies
- Forge 47.x (required)
- MineColonies 1.20.1-1.1.x (required, accessed via reflection)
- Create (optional, detected at runtime)
- No external libraries beyond Forge/MineColonies

## Build
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
.\gradlew build --no-daemon
.\deploy.ps1
```

## Test Instance
```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony
```

## Git
- No commits made yet (repo at C:\ root)
- Identity: `Suttawat <suttawat@users.noreply.github.com>`

## Known Issues / TODOs
- Building classification uses heuristic string matching — replace with proper MineColonies building type checks
- Pressure formulas are heuristic — tune after seeing real data
- Happiness scale assumption (0-10) not verified against actual MineColonies API
- No memory decay yet (Phase 4B)
- No memory influence on events yet (Phase 4B)
- No prayer seed generation yet (Phase 4C)
- No TRIUMPH or CULTURAL memory types yet (Phase 4A uses only SIGNIFICANT_EVENT, TRAUMA, PATTERN)
