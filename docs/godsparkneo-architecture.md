# GodsparkNeo Architecture

## Overview

GodsparkNeo transforms MineColonies from a player-micromanaged colony simulator into a **God Game / Autonomous Colony Simulation**.

Three actors coexist:
1. **MineColonies Native AI** — manages citizens, jobs, buildings, work orders.
2. **External LLM Director** — reads exported colony state and proposes structured plans.
3. **Player as Deity** — answers citizen prayers, grants blessings, performs miracles.

## High-Level Data Flow

```
MineColonies colony state
  ↓
GodsparkNeo State Exporter (read-only, throttled)
  ↓
Structured JSON colony state
  ↓
External LLM Director
  ↓
Structured proposed action (JSON schema)
  ↓
GodsparkNeo Java validator
  ↓
Allowed action executor
  ↓
MineColonies work-order / building / citizen systems
  ↓
Audit log + memory update + prayer update
```

## Key Principles

### LLM Is Read-Only Proposer
- The LLM **may**: summarize, rank, narrate, propose structured actions, interpret prayers, explain bottlenecks, recommend plans.
- The LLM **must not**: run arbitrary commands, execute raw code, mutate the world directly, bypass Java validators, control pathfinding directly, run every tick, bypass work-order systems, override permissions, create unbounded action loops.

### Java Owns Execution
- Java must own: all validation, permission checks, cooldowns, persistence, locks, world mutation, final execution.

### Player as Deity
- The player does not micromanage every build decision.
- The player receives citizen prayers/requests.
- The player answers through bounded blessings, miracles, or guidance.
- Player-facing interaction should eventually be UI/shrine/altar based, not only commands.

## System Modules

### `com.godsparkneo.export`
Read-only colony state exporter. Direct MineColonies API access (no reflection). Throttled, cached, defensive.

### `com.godsparkneo.pressure`
Deterministic colony pressure scoring (FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY). Direct data, no reflection.

### `com.godsparkneo.event`
Stateless event generator + event state manager. Lifecycle: ACTIVE → PERSISTENT → RESOLVED.

### `com.godsparkneo.memory`
Memory bank + memory engine + memory influence. Types: SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL. With real decay.

### `com.godsparkneo.prayer`
Prayer seed generator + prayer seed bank. Channels: PRIVATE, COMMONS, CHURCH, SHRINE, TEMPLE.

### `com.godsparkneo.personality`
Colony personality engine (ported if still desired).

### `com.godsparkneo.divine`
Divine answer interpreter, divine intent validator, validation result types.

### `com.godsparkneo.lock`
Token/lock reservation system. Prevents native AI from fighting LLM high-priority tasks. Soft reservations, auto-expire, persist.

### `com.godsparkneo.action`
Structured action validation and execution. JSON schema in, Java validation, safe MineColonies-native task out.

### `com.godsparkneo.llm`
LLM integration layer (HTTP, async, no blocking server thread).

### `com.godsparkneo.validation`
Shared validation utilities for actions, intents, and world mutations.

### `com.godsparkneo.persistence`
Colony-level Godspark state persistence. Defensive load paths.

### `com.godsparkneo.command`
Debug and player-facing commands.

### `com.godsparkneo.debug`
Debug dashboards, visibility tools.

## Integration Points with MineColonies

| MineColonies System | GodsparkNeo Integration |
|---------------------|-------------------------|
| Colony save/load | Attach Godspark state to colony/world data |
| Work order system | Lock reservation, LLM-proposed build orders |
| Citizen AI | Soft lock consultation (Phase E+) |
| Building registry | Direct type checks instead of string classification |
| Request/resource | Lock on resource requests |
| Event/tick | Throttled exporter, not every tick |

## Persistence Strategy

All long-lived state attaches to MineColonies' existing save mechanisms (colony data, world saved data, level data). No separate external database.

Load paths must:
- Handle missing fields gracefully.
- Not crash on corrupted or legacy data.
- Default to safe empty states.
