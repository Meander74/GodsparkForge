# Phase 4 — Memory System Plan

## Status
Phase 4A: ✅ COMPLETE
Phase 4B: ⏭️ Planned
Phase 4C: ️ Planned

## Overview
Give each colony a persistent, semantic memory of its history — not just raw snapshots, but interpreted memories that can influence future events and narrative.

## Phase 4A — Persistent Colony Memories (COMPLETE)

### Goal
Generate memories from event transitions, store them persistently, view via command.

### Memory Types
| Type | Trigger | Example |
|---|---|---|
| SIGNIFICANT_EVENT | ACTIVE → PERSISTENT transition | "Food shortage risk has persisted in New Haven." |
| TRAUMA | HIGH severity + persistent | "New Haven remembers the threat of famine." |
| PATTERN | 3+ memories for same pressure type | "Food insecurity has become a recurring pattern." |
| TRIUMPH | (future) Resolved after persistent | "The colony overcame a housing crisis." |
| CULTURAL | (future) Repeated needs/desires | "The colony increasingly values communal food security." |

### ColonyMemory Record
| Field | Type | Description |
|---|---|---|
| colonyId | int | Colony ID |
| colonyName | String | Colony name |
| memoryType | MemoryType | Type of memory |
| pressureType | PressureType | Related pressure |
| severity | EventSeverity | Event severity at creation |
| content | String | Memory description |
| intensity | int | 0-100 intensity score |
| reinforcementCount | int | Times reinforced |
| createdAtTick | long | Creation tick |
| lastRecalledTick | long | Last access tick |
| lastReinforcedTick | long | Last reinforcement tick |
| decayRate | int | Future decay rate (unused in 4A) |

### Intensity Formula
```
base from severity:
  LOW = 25, MEDIUM = 50, HIGH = 75

+ persistence bonus: min(25, persistenceCount * 5)

+ state bonus:
  PERSISTENT +10
  RESOLVED after persistent +5

clamp 0-100
```

### Memory Generation Rules
1. **PERSISTENT transition** → SIGNIFICANT_EVENT memory
2. **HIGH severity + persistent** → TRAUMA memory
3. **3+ memories for same pressure type** → PATTERN memory
4. **RESOLVED event with persistenceCount >= 3** → SIGNIFICANT_EVENT memory

### MemoryBank
- Per-colony storage: `Map<Integer, Deque<ColonyMemory>>`
- Max 50 memories per colony (FIFO with importance trimming)
- Dedup key: `colonyId + memoryType + pressureType`
- Reinforcement: intensity += 10, reinforcementCount++, update timestamps
- Sorted by intensity desc, lastReinforcedTick desc

### Persistence
- `DATA_VERSION = 2` (incremented from Phase 3.5B's v1)
- Backward compat: v1 saves load with no memories
- NBT structure: `memories` list with all ColonyMemory fields

### Commands
- `/godspark memories` — Show top 20 memories across all colonies
- `/godspark memories <colonyId>` — Show top 10 memories for specific colony

### Files Created
- `src/main/java/com/godspark/memory/MemoryType.java`
- `src/main/java/com/godspark/memory/ColonyMemory.java`
- `src/main/java/com/godspark/memory/MemoryBank.java`
- `src/main/java/com/godspark/memory/MemoryEngine.java`

### Files Modified
- `src/main/java/com/godspark/GodsparkMod.java` — Added MEMORY_BANK, MEMORY_ENGINE
- `src/main/java/com/godspark/event/GodsparkServerEvents.java` — Wired memory generation
- `src/main/java/com/godspark/command/GodsparkCommands.java` — Added /godspark memories
- `src/main/java/com/godspark/persistence/GodsparkSavedData.java` — Bumped to v2, memory NBT

### What Phase 4A Does NOT Do
- No memory influence on event generation
- No time-based decay (decayRate stored but unused)
- No prayer seed generation
- No TRIUMPH or CULTURAL memory types

## Phase 4B — Memory Influence on Events (PLANNED)

### Goal
Memories gently influence future event generation without creating feedback loops.

### Proposed Influence
- Memory intensity 80+ → +5 to related pressure event severity
- Memory intensity 60+ → lowers threshold by 5
- Very small influence — raw colony state still dominates

### Decay Model
- Daily decay (every 24000 ticks): `intensity -= decayRate`
- Event-triggered reinforcement: related events boost intensity

### Files to Modify
- `src/main/java/com/godspark/story/EventGenerator.java` — Memory-aware generation
- `src/main/java/com/godspark/memory/MemoryEngine.java` — Add decay logic

## Phase 4C — Prayer Seed Generation (PLANNED)

### Goal
Generate prayer seeds from memory + pressure combinations.

### Example
```
Food trauma + high food pressure:
"The people pray for granaries that will not fail them again."
```

### Dependencies
- Stable memory bank (Phase 4A)
- Stable event state (Phase 3.5)
- Some memory influence (Phase 4B)
