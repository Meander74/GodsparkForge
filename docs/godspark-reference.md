# Godspark — Reference

## What Godspark Is
- An **observer** of MineColonies colony state via reflection
- A **pressure engine** that converts colony metrics into 0-100 pressure scores
- A **storyteller** that generates event candidates from pressure thresholds
- An **event lifecycle tracker** with ACTIVE → PERSISTENT → RESOLVED states
- A **persistent memory system** that records colony traumas, patterns, and triumphs
- A **memory influence system** that adjusts event thresholds based on colony history
- A **prayer seed generator** that produces colony prayer language from state and sacred site access
- An **AI reflection layer** with async LLM or template fallback
- A **divine answer interpreter** that validates player answers into bounded effects
- A **miracle system** that applies temporary pressure modifiers via validated player intent
- A **colony personality system** that derives deterministic traits from colony history
- A **sacred site system** that tracks Prayer Stone blocks and MineColonies sacred buildings for prayer channel progression

## What Godspark Is Not
- A replacement for MineColonies
- A standalone colony simulation
- An LLM-driven NPC controller
- A mod that runs AI every tick
- A mod that auto-activates world effects (miracles are player-mediated only)

## Architecture
```
MineColonies Simulation
      ↓
ColonyObserver (reflection-based API scanner)
      ↓
ColonySnapshot (17 fields: citizens, buildings, happiness, guards, sacredBuildingCount, gatheringBuildingCount, etc.)
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
PrayerSeedGenerator (pressures + events + memories + personality + sacred sites → prayer seeds)
      ↓
PrayerSeedBank (per-colony storage, dedup by sourceKey, expire, max 20)
      ↓
PersonalityEngine (deterministic traits from history, cached per cycle)
      ↘
PersonalityInfluence (traits → threshold adjustments, prayer tone modulation)
      ↓
Divine Answer Pipeline:
  /godspark answer → DivineAnswerInterpreter → DivineIntent
      ↓
  DivineIntentValidator (ORACLE_APPROVED / EFFECT_ELIGIBLE / REJECTED / DOWNGRADED)
      ↓
  PressureModifierManager.tryApplyFromValidatedIntent() [if EFFECT_ELIGIBLE]
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
│   ├── BuildingCategory.java          — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY, WAREHOUSE, SACRED, OTHER
│   ├── BuildingClassifier.java        — Maps MineColonies building names to categories; isGatheringPlace()
│   ├── ColonyObserver.java           — Reflection-based MineColonies API scanner
│   ├── ColonySnapshot.java           — Immutable colony state record (23 fields incl. sacred/gathering/raid)
│   └── ObservedColony.java           — Tracks snapshot history per colony
├── persistence/
│   └── GodsparkSavedData.java — NBT serialization (v2), backward compat with v1
├── prayer/
│   ├── PrayerChannel.java             — Enum: PRIVATE, COMMONS, CHURCH, SHRINE, TEMPLE (isPublic, isSacred, isMiracleEligible)
│   ├── PrayerSeed.java                — Immutable prayer seed record (13 fields, isPublicPrayer, isMiracleEligible)
│   ├── PrayerSeedBank.java            — Per-colony prayer storage, dedup by sourceKey, expire, max 20
│   ├── PrayerSeedGenerator.java       — Generates prayer seeds with channel/personality/tone
│   ├── PrayerTone.java                — Enum: 8 tones with adverbial phrases, fromTrait() mapping
│   └── PrayerType.java                — Enum: PLEA, VIGIL, THANKS, LAMENT, HOPE
├── pressure/
│   ├── PressureEngine.java            — Computes 5 pressure types, applies modifier deltas
│   ├── PressureModifier.java          — Record: colonyId, pressureType, amount, expiresAtTick, source
│   ├── PressureModifierManager.java   — Server-thread-only, miracle application + expiry, floor 20
│   ├── PressureSnapshot.java          — Immutable pressure values record
│   └── PressureType.java              — Enum: FOOD, SECURITY, HOUSING, COMFORT, INDUSTRY
├── personality/
│   ├── ColonyPersonality.java        — Record: primaryTrait, secondaryTrait, scores, evidence
│   ├── PersonalityEngine.java        — Computes traits from memories/events/buildings/pressures/prayers, cached update
│   ├── PersonalityInfluence.java     — traits → threshold adjustments (±5 per trait, combined clamp -20/+10)
│   └── PersonalityTrait.java         — Enum: 11 traits with descriptions
├── sacred/
│   ├── SacredSiteManager.java        — Singleton, dimension+position keyed, colony-bound sacred anchors
│   ├── SacredSiteType.java           — Enum: PRAYER_STONE(anchorScore=1), MINECOLONIES_SACRED
│   └── SacredSiteRecord.java         — Record: type, pos, dimension, colonyId, registeredAtTick
├── block/
│   └── PrayerStoneBlock.java         — BaseEntityBlock, lightLevel=7, right-click=actionbar message
├── block/entity/
│   └── PrayerStoneBlockEntity.java   — Auto-registers with SacredSiteManager on server load; chunk unload does not unregister
├── registry/
│   ├── GodsparkBlocks.java           — DeferredRegister: PRAYER_STONE
│   ├── GodsparkBlockEntities.java    — DeferredRegister: PRAYER_STONE BlockEntityType
│   └── GodsparkItems.java            — DeferredRegister: PRAYER_STONE BlockItem
├── divine/
│   ├── DivineAnswer.java             — Record: playerId, playerName, colonyId, rawText, submittedAtTick
│   ├── DivineAnswerContext.java       — Immutable colony snapshot for async work, List.copyOf
│   ├── DivineAnswerInterpreter.java   — Orchestrator: template sync, async AI, cooldowns, buildContext()
│   ├── DivineAnswerPromptBuilder.java — Builds LLM system+user messages from context
│   ├── DivineIntent.java             — Record with compact constructor: schemaVersion, colonyId, intentType, domain (nullable), confidence, oracleText, reasonCodes, source
│   ├── DivineIntentValidator.java    — ORACLE_APPROVED / EFFECT_ELIGIBLE / REOWNGRADED / REJECTED
│   ├── IntentType.java               — Enum: ORACLE_ONLY, ANSWER_QUESTION, CREATE_TRIAL, BLESS_COLONY
│   ├── IntentSource.java             — Enum: TEMPLATE, AI, AI_FALLBACK, AI_COOLDOWN, PARSER_FALLBACK
│   ├── TemplateDivineInterpreter.java — Keyword matching with 3-tier sacred prayer gate
│   └── DivineIntentParser.java       — Validates AI JSON, nullable domain, safe fallbacks
├── ai/
│   ├── AiConfig.java                 — Config record: enabled, endpoint, model, timeout, cooldown, maxTokens, temperature
│   ├── AiReflectionService.java      — Async LLM/template reflection with cooldowns
│   └── TemplateReflectionService.java — Template-based fallback for AI reflection
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
- **Building classification uses heuristic lowercase matching** — sacred keywords: church/chapel/temple/shrine/monastery/mystical/altar/sanctuary/oracle/ritual/rune/totem/spirit/divine/sacred/reliquary/obelisk
- **Graveyard is gathering-only, NOT sacred** — counts toward COMMONS channel but not miracle eligibility
- **PrayerChannel progression**: PRIVATE(no voice) -> COMMONS(gathering only) -> CHURCH(1 true sacred building or local Prayer Stone) -> SHRINE(2 true sacred buildings) -> TEMPLE(3+ true sacred buildings)
- **Sacred access inputs are separate** — true MineColonies sacred building count and local Prayer Stone anchor are not merged into one scalar
- **Prayer Stone is an early sacred anchor only** — player-placed, colony-bound within 128 blocks, capped at CHURCH, never SHRINE/TEMPLE
- **SacredSiteManager is server-thread-only** — no world scanning; block entities register on load, block removal unregisters, chunk unload does not unregister
- **Personality from previous cycle** — cached, not same-cycle computation, never affects PressureEngine baselines
- **PersonalityInfluence clamps** — ±5 per trait on thresholds, combined clamp -20/+10, ±20 on memory thresholds
- **PrayerTone uses adverbial phrases** — `Math.floorMod` for phrase selection within tones
- **COMMONS is public but not sacred** — gathering places without sacred infrastructure, not miracle-eligible
- **EventGenerator is stateless** — pure function, testable
- **EventStateManager keyed by colonyId + PressureType** — not StoryEventType (severity changes are one continuing condition)
- **EventQueue owns display/log dedup** — EventStateManager owns lifecycle truth
- **MemoryEngine consumes transitions, not all active events** — avoids generating memories every cycle
- **Dedup by colonyId + memoryType + pressureType** — reinforcement updates intensity/count instead of duplicates
- **No time-based decay yet** — decayRate stored but unused
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

## In-Game Commands
```
/godspark status              — Mod status, detected mods, colony count
/godspark colonies             — List observed colonies with citizen/building counts
/godspark pressures            — Show current pressure values per colony (base + modifier)
/godspark events [N]          — Show active/persistent/resolved events (default 10)
/godspark memories [colonyId] — Show top 20 memories (10 per colony)
/godspark influences           — Show memory threshold adjustments per colony
/godspark prayers [colonyId]  — Show active prayer seeds with channels
/godspark personality [colonyId] — Show colony personality traits, threshold influence, prayer tone
/godspark reflect <colonyId>  — AI or template reflection
/godspark ai status           — AI configuration status
/godspark answer <colonyId> <message> — Player divine answer (permission 2)
/godspark miracles [colonyId] — Show active pressure modifiers, base/effective, cooldowns
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
| 4C | ✅ DONE | Prayer Seed Generation — colony prayer language from state, sacred channels |
| 5A | ✅ DONE | AI Reflection — async LLM/template reflection with cooldowns |
| 5B | ✅ DONE | Divine Answer Interpreter — `/godspark answer` with template + AI paths |
| 5C | ✅ DONE | DivineIntent Validator — ORACLE_APPROVED / EFFECT_ELIGIBLE / REJECTED / DOWNGRADED |
| 5D.1 | ✅ DONE | Pressure Modifier Miracles — Guardian's Vigil, Green Mercy (player-triggered only) |
| 5D.2 | Future | Light World Effects — entity status, crop growth |
| 6A | ✅ DONE | Colony Personality — deterministic traits from history |
| 6A.1 | ✅ DONE | PersonalityInfluence + PrayerTone integration |
| 6A.2 | ✅ DONE | Sacred Access Score + COMMONS channel |
| 6A.2.1 | ✅ DONE | Sacred keyword compatibility patch (graveyard→gathering, 11 new sacred keywords, CHURCH fix) |
| 6A.3 | ✅ DONE | Prayer Stone block + SacredSiteManager + channel progression |
| UI-A | ✅ DONE | Debug Dashboard v1.1 — 6-tab GUI, SimpleChannel network |
| 6B | Future | Colony Intentions — Prayer Stone requests, shrine building integration |

## Code Conventions
- Java 17, no external dependencies beyond Forge/MineColonies
- Records for immutable data (ColonySnapshot, PressureSnapshot, StoryEvent, EventRecord)
- Final classes for services (ColonyObserver, PressureEngine, EventGenerator, EventStateManager)
- Static service instances in GodsparkMod
- No comments unless explaining non-obvious behavior
- Defensive programming — null checks, safe defaults, try/catch around reflection
- Use `Locale.ROOT` for string case operations
- Use diamond operators for generic constructors

## Current Project State
- Phase 6A.3 complete — Prayer Stone block, SacredSiteManager, channel progression
- All static services initialized in GodsparkMod; blocks/BEs/items registered on mod event bus
- `PressureModifierManager` wired into `PressureEngine` for modifier deltas
- No auto-miracle activation — miracles only via `/godspark answer` after validation
- `DivineIntentValidator` enforces ORACLE_APPROVED / EFFECT_ELIGIBLE / REJECTED / DOWNGRADED
- `DivineAnswerContext` is immutable with `List.copyOf` — safe for async work
- AI cannot self-claim `matchedPublicPrayer` — Java recalculates from context
- Domain inference: null domain + one strongest prayer → inferred with ×0.8 confidence
- Pressure floor of 20: miracles relieve but don't erase consequences
- CREATE_TRIAL is narrative-only — no pressure modifier
- Personality integration: previous-cycle cached traits influence event thresholds (never PressureEngine baselines)
- PrayerTone: 8 tones with adverbial phrases, per-colony based on primary trait
- PrayerChannel: PRIVATE → COMMONS → CHURCH → SHRINE → TEMPLE progression based on sacred access score
- COMMONS: public voice, not miracle-eligible; separates "colony can be heard" from "miracle unlocked"
- Prayer Stone: player-placed local sacred anchor, binds to nearest colony within 128 blocks, capped at CHURCH, recipe torch+dandelion+smooth_stone
- SacredSiteManager: singleton, server-thread-only, no world scanning, BEs self-register on load
- Graveyard classified as gathering-only (COMMONS), not SACRED — no miracle unlock
- BuildingClassifier: 11 new sacred keywords (altar/sanctuary/oracle/ritual/rune/totem/spirit/divine/sacred/reliquary/obelisk)
- Debug Dashboard: 6-tab read-only GUI, `@OnlyIn(Dist.CLIENT)` client classes, OP level 2 gated, 1 req/sec throttle

Phase 6A.3 stabilization correction:
- PrayerChannel progression now reads true sacred building count and local Prayer Stone anchor separately.
- Prayer Stones bind to the nearest valid same-dimension colony within 128 blocks and are capped at CHURCH.
- Multiple Prayer Stones never produce SHRINE or TEMPLE; those require true MineColonies sacred buildings.
- SacredSiteManager is keyed by dimension+position and block break unregisters; chunk unload does not.
