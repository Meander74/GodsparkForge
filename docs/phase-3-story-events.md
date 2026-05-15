# Phase 3 — Story Events

## Status
✅ COMPLETE

## Goal
Generate deterministic story event candidates from pressure thresholds, with display/log deduplication.

## Event Thresholds
| Pressure | LOW (>N) | MEDIUM (>N) | HIGH (>N) |
|---|---|---|---|
| FOOD | 40 | 70 | 90 |
| SECURITY | 40 | 70 | 90 + activeRaid |
| HOUSING | 0 | 50 | 80 |
| COMFORT | 40 | 70 | 85 |
| INDUSTRY | 40 | 70 | 90 |

## 15 Event Types
5 pressure types × 3 severity levels = 15 unique `StoryEventType` constants.

Each event type has: `pressureType`, `severity`, `threshold`, `description`, `requiresActiveRaid`.

Only `SECURITY_HIGH` requires an active raid.

## Architecture
```
PressureSnapshot + ObservedColony
       ↓
EventGenerator (stateless pure function)
       ↓
List<StoryEvent> (one per pressure type per colony, highest matching severity)
       ↓
EventQueue (6000-tick dedup, 100 max history)
       ↓
Logs + /godspark events command
```

## Key Design Decisions
- **EventGenerator is stateless** — pure function, testable, no side effects
- **findHighestMatchingEvent()** — streams all StoryEventType values, filters by matching pressure type, pressure value exceeding threshold, and active raid requirement, picks max severity
- **EventQueue owns display/log dedup** — separate from lifecycle tracking
- **Cooldown: 6000 ticks** (5 minutes) per (colonyId + StoryEventType) pair
- **Max history: 100 events** (FIFO via Deque)
- **StoryEvent is immutable record** — `eventType`, `colonyId`, `colonyName`, `pressureType`, `severity`, `pressureValue`, `threshold`, `description`, `gameTick`

## Files
- `src/main/java/com/godspark/story/EventGenerator.java`
- `src/main/java/com/godspark/story/EventQueue.java`
- `src/main/java/com/godspark/story/StoryEvent.java`
- `src/main/java/com/godspark/story/StoryEventType.java`
- `src/main/java/com/godspark/story/EventSeverity.java`

## Testing
- `/godspark events` shows active and recently resolved events
- Event dedup prevents spam (same event type per colony within 5 minutes)
- Events correctly match pressure thresholds
- No duplicate events observed in logs
