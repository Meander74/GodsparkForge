# GodsparkNeo Roadmap

## Phase A — Repository Migration & Clean Upstream Build ✅

- [x] Preserve legacy companion mod on `legacy/forge-1.20.1-companion`
- [x] Import MineColonies `upstream/version/1.21`
- [x] Tag baseline `baseline-minecolonies-1.21.1`
- [x] Build passes unmodified
- [x] Tag `godsparkneo-phase-a-upstream-build-pass`

## Phase B — Minimal Branding / Docs

- [x] Rewrite README.md with GodsparkNeo identity
- [x] Create AGENTS.md with coding agent briefing
- [x] Create docs/ with architecture, roadmap, legacy port map, upstream sync policy
- [ ] Tag `godsparkneo-phase-b-docs-complete`

## Phase C — Read-Only State Exporter ✅

- [x] New package `com.godsparkneo.export`
- [x] `/godspark export <colonyId>` command (OP level 2)
- [x] Direct MineColonies API (no reflection)
- [x] Versioned JSON output (schema v1, UTC timestamp)
- [x] Colony metadata, citizens (job, skills, happiness, food stats), buildings, work orders, threats, resource summary
- [x] Defensive null handling for all data paths
- [x] File output only (no JSON in chat)
- [x] Build passes: `BUILD SUCCESSFUL`
- [x] Tag `godsparkneo-phase-c-state-exporter`

## Phase D — Port Deterministic Pressure / Memory / Prayer

- [ ] Port `PressureEngine`, `PressureSnapshot`, `PressureType`
- [ ] Port `EventGenerator`, `EventStateManager`
- [ ] Port `MemoryBank`, `MemoryEngine`, `MemoryInfluence` (with real decay)
- [ ] Port `PrayerSeed`, `PrayerSeedBank`, `PrayerSeedGenerator`
- [ ] Port `PersonalityEngine` if desired
- [ ] Persistence via MineColonies save data
- [ ] Tag `feature/pressure-port`

## Phase E — Lock / Token Reservation System

- [ ] New package `com.godsparkneo.lock`
- [ ] Lock model: ACTIVE, RELEASED, EXPIRED, FAILED
- [ ] Target types: WORK_ORDER, BUILD_SITE, BUILDING
- [ ] Persist or safe-expire across save/reload
- [ ] Debug command shows active locks
- [ ] Tag `feature/lock-state`

## Phase F — First Validated Autopilot Action

- [ ] JSON action schema v1
- [ ] External LLM proposes one action
- [ ] Java validates schema, bounds, permissions, resources
- [ ] Creates safe MineColonies-native work order
- [ ] Lock prevents conflict
- [ ] Audit log
- [ ] Tag `feature/llm-action-validator`

## Phase G — Deity UI & Blessings

- [ ] Port prayer/sacred access concepts
- [ ] FOOD / SECURITY blessings only
- [ ] DivineIntent validation
- [ ] Commands or debug UI initially
- [ ] Long-term: shrine/altar blocks and UI
- [ ] Tag `feature/deity-ui`

## Post-Phase G

- [ ] Expand lock integration into citizen job selection and builder assignment
- [ ] Expand LLM action types (building placement, upgrade queue)
- [ ] Expand miracle domains (only if explicitly scoped)
- [ ] Async HTTP LLM service integration
- [ ] Colony-level Godspark state UI
