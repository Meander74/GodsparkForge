# Phase 2 — Pressure Engine

## Status
✅ COMPLETE

## Goal
Convert observed colony state into 5 societal pressure metrics on a 0-100 scale.

## Pressure Types
| Type | Formula | Description |
|---|---|---|
| FOOD | `clamp(100 - foodBuildingCount * 20)` | Food production capacity |
| SECURITY | `clamp(100 - guardCount * 25 + (hasActiveRaid ? 30 : 0))` | Defense readiness |
| HOUSING | `citizenCount > housingCapacity ? clamp((citizenCount - housingCapacity) * 30) : 0` | Housing shortage |
| COMFORT | `clamp((10.0 - happiness) * 10.0)` | Citizen happiness inverse |
| INDUSTRY | `clamp(100 - industryBuildingCount * 15)` | Industrial capacity |

## Design Decisions
- **0-100 scale** — intuitive for players and easy to threshold
- **clampPressure() handles NaN/infinite** — uses `Math.round` for robust edge cases
- **getSnapshots() returns Map.copyOf()** — prevents external mutation
- **PressureSnapshot is immutable record** — `colonyId`, `Map<PressureType, Integer>`, `gameTick`
- **PressureType enum** — FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY with display names

## Files
- `src/main/java/com/godspark/pressure/PressureEngine.java`
- `src/main/java/com/godspark/pressure/PressureSnapshot.java`
- `src/main/java/com/godspark/pressure/PressureType.java`

## Tick Interval
- Pressure computation runs every 1200 ticks (60 seconds) after observer scan

## Testing
- `/godspark pressures` displays current pressure values per colony per type
- Pressure values correctly reflect colony state changes
- No NaN or infinite values observed in production

## Known Issues
- Pressure formulas are heuristic — tune after seeing real data
- Building counts depend on heuristic classification (Phase 1 TODO)
