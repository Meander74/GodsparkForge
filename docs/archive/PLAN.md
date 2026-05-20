# Godspark - Next Steps Consultation

## Current Project State

**Last commit:** ac57316 - Phase 4A/4B Memory System
**Status:** Working, tested in-game

## What's Built (1-4B Complete)

| Phase | Description | Status |
|-------|-------------|--------|
| 1 | Observer - MineColonies API scanner via reflection | ✅ |
| 2 | Pressure Engine - 5 types (FOOD/SECURITY/HOUSING/COMFORT/INDUSTRY) | ✅ |
| 3 | Story Events - 15 event types with EventQueue dedup | ✅ |
| 3.5A | Event Lifecycle - ACTIVE → PERSISTENT → RESOLVED | ✅ |
| 3.5B | SavedData Persistence (NBT v2) | ✅ |
| 4A | Memory System - ColonyMemory, MemoryBank, MemoryEngine | ✅ |
| 4B | Memory Influence - thresholds adjust based on colony history | ✅ |

## Tech Stack
- Minecraft 1.20.1 + Forge 47.x
- Java 17
- MineColonies 1.20.1-1.1.x (required)
- Create (optional, detected at runtime)

## Architecture
```
MineColonies → ColonyObserver → PressureEngine → EventGenerator
                                                      ↓
                                            EventStateManager (lifecycle)
                                                      ↓
                                            MemoryEngine → MemoryBank
                                                      ↓
                                            MemoryInfluence → EventGenerator
                                                      ↓
                                                 EventQueue → /godspark
```

## Current Commands
- `/godspark status` - Mod status
- `/godspark colonies` - List colonies
- `/godspark pressures` - Show pressures
- `/godspark events` - Show events (1-50)
- `/godspark memories` - Show memories
- `/godspark influences` - Show memory threshold adjustments

## Future Phases (Not Started)

### Phase 4C: Prayer Seed Generation
- Not defined yet
- Need to determine what this should do

### Phase 5: AI Reflection
- Local llama.cpp narrative
- Complex feature requiring design

### Phase 6: Civilization Evolution
- Pressures unlock directions
- Long-term vision

## Known TODOs
- Building classification uses heuristic string matching (need proper MineColonies types)
- Pressure formulas may need tuning after playtesting
- Memory decay not implemented (decayRate stored but unused)
- Comfort target happiness (7.0) not verified against actual API

## Questions for Consultation

1. **Phase 4C Design**: What should "Prayer Seed Generation" do? Ideas:
   - Generate prayer/ritual events based on colony state?
   - Create blessing system that affects colony behavior?
   - Something else?

2. **Memory Enhancement**: Now that memory exists, what else could we do?
   - Memory-based NPC dialogue?
   - Historical record in chat?
   - Memory decay system?

3. **Performance**: Any concerns about the tick loop (600 ticks scan, 1200 ticks compute)?

4. **Integration**: Should we expose more data to other mods via capability?

5. **What's the highest-impact next feature to build?**

---

## Project Files Reference
- AGENTS.md - Full agent context
- docs/phase-*-*.md - Phase documentation
- src/main/java/com/godspark/ - Source code (23 files)