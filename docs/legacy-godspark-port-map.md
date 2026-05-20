# Legacy Godspark Port Map

This document maps legacy `GodsparkForge` companion-mod systems to their new `GodsparkNeo` fork equivalents.

## Old vs New Architecture

| Old (Companion Mod) | New (Fork) |
|---------------------|------------|
| Reflection-based `ColonyObserver` | Direct MineColonies API access |
| String/keyword `BuildingClassifier` | Direct building type checks, registry IDs |
| Separate mod lifecycle | Integrated into MineColonies core |
| `com.godspark.*` packages | `com.godsparkneo.*` packages |

## Package Mapping

| Legacy Package | New Package | Status |
|----------------|-------------|--------|
| `com.godspark.observer` | `com.godsparkneo.export` | Replaced |
| `com.godspark.pressure` | `com.godsparkneo.pressure` | Port |
| `com.godspark.event` | `com.godsparkneo.event` | Port |
| `com.godspark.memory` | `com.godsparkneo.memory` | Port |
| `com.godspark.prayer` | `com.godsparkneo.prayer` | Port |
| `com.godspark.personality` | `com.godsparkneo.personality` | Port |
| `com.godspark.divine` | `com.godsparkneo.divine` | Port |
| `com.godspark.sacred` | `com.godsparkneo.divine` or `com.godsparkneo.prayer` | Merge |
| `com.godspark.story` | `com.godsparkneo.event` | Merge |
| `com.godspark.ai` | `com.godsparkneo.llm` | Replaced |
| `com.godspark.command` | `com.godsparkneo.command` | Port |
| `com.godspark.block` | `com.godsparkneo.block` (future) | Defer |
| `com.godspark.client` | `com.godsparkneo.client` (future) | Defer |
| `com.godspark.network` | `com.godsparkneo.network` (future) | Defer |
| `com.godspark.registry` | `com.godsparkneo.registry` (future) | Defer |
| `com.godspark.persistence` | `com.godsparkneo.persistence` | Port |

## System Mapping

### Pressure Engine
- Legacy: `PressureEngine`, `PressureSnapshot`, `PressureType`
- New: Same concepts, direct MineColonies data, no reflection
- Domains: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY

### Event System
- Legacy: `EventGenerator` (stateless), `EventStateManager`, `EventQueue`
- New: Same separation of concerns
- Lifecycle: ACTIVE → PERSISTENT → RESOLVED

### Memory System
- Legacy: `MemoryBank`, `MemoryEngine`, `MemoryInfluence`
- New: Same, **with real decay implemented**
- Types: SIGNIFICANT_EVENT, TRAUMA, PATTERN, TRIUMPH, CULTURAL

### Prayer System
- Legacy: `PrayerSeedGenerator`, `PrayerSeedBank`, `PrayerSeed`
- New: Same, direct building type checks for sacred tier
- Channels: PRIVATE, COMMONS, CHURCH, SHRINE, TEMPLE

### Divine Answer
- Legacy: `DivineAnswerInterpreter`, `DivineIntentValidator`, `ValidatedIntent`
- New: Same concepts, integrated into fork
- Results: ORACLE_APPROVED, EFFECT_ELIGIBLE, REJECTED, DOWNGRADED

### World Effects
- Legacy: `PressureModifierManager` + `WorldEffectEngine`
- New: Same, but only FOOD and SECURITY initially
- Defer: COMFORT, HOUSING, INDUSTRY

### Building Classification
- Legacy: Keyword matching on building names (`church`, `temple`, `shrine`, etc.)
- New: Direct `IBuilding` type checks, registry IDs, or building classes
- **Do not** classify by translated display text

## Deferred Systems

These are not in scope until explicitly requested:
- `com.godspark.block` (Prayer Stone, etc.)
- `com.godspark.client` (debug dashboard UI)
- `com.godspark.network` (packet system)
- Expanded miracle domains (COMFORT, HOUSING, INDUSTRY)
- Full LLM HTTP service integration (Phase F+)
