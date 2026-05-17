# context-mode — MANDATORY routing rules

context-mode MCP tools available. Rules protect context window from flooding. One unrouted command dumps 56 KB into context.

## Think in Code — MANDATORY

Analyze/count/filter/compare/search/parse/transform data: **write code** via `context-mode_ctx_execute(language, code)`, `console.log()` only the answer. Do NOT read raw data into context. PROGRAM the analysis, not COMPUTE it. Pure JavaScript — Node.js built-ins only (`fs`, `path`, `child_process`). `try/catch`, handle `null`/`undefined`. One script replaces ten tool calls.

## BLOCKED — do NOT attempt

### curl / wget — BLOCKED
Shell `curl`/`wget` intercepted and blocked. Do NOT retry.
Use: `context-mode_ctx_fetch_and_index(url, source)` or `context-mode_ctx_execute(language: "javascript", code: "const r = await fetch(...)")`

### Inline HTTP — BLOCKED
`fetch('http`, `requests.get(`, `requests.post(`, `http.get(`, `http.request(` — intercepted. Do NOT retry.
Use: `context-mode_ctx_execute(language, code)` — only stdout enters context

### Direct web fetching — BLOCKED
Use: `context-mode_ctx_fetch_and_index(url, source)` then `context-mode_ctx_search(queries)`

## REDIRECTED — use sandbox

### Shell (>20 lines output)
Shell ONLY for: `git`, `mkdir`, `rm`, `mv`, `cd`, `ls`, `npm install`, `pip install`.
Otherwise: `context-mode_ctx_batch_execute(commands, queries)` or `context-mode_ctx_execute(language: "shell", code: "...")`

### File reading (for analysis)
Reading to **edit** → reading correct. Reading to **analyze/explore/summarize** → `context-mode_ctx_execute_file(path, language, code)`.

### grep / search (large results)
Use `context-mode_ctx_execute(language: "shell", code: "grep ...")` in sandbox.

## Tool selection

0. **MEMORY**: `context-mode_ctx_search(sort: "timeline")` — after resume, check prior context before asking user.
1. **GATHER**: `context-mode_ctx_batch_execute(commands, queries)` — runs all commands, auto-indexes, returns search. ONE call replaces 30+. Each command: `{label: "header", command: "..."}`.
2. **FOLLOW-UP**: `context-mode_ctx_search(queries: ["q1", "q2", ...])` — all questions as array, ONE call (default relevance mode).
3. **PROCESSING**: `context-mode_ctx_execute(language, code)` | `context-mode_ctx_execute_file(path, language, code)` — sandbox, only stdout enters context.
4. **WEB**: `context-mode_ctx_fetch_and_index(url, source)` then `context-mode_ctx_search(queries)` — raw HTML never enters context.
5. **INDEX**: `context-mode_ctx_index(content, source)` — store in FTS5 for later search.

## Graphify Bridge

Graphify knowledge graph is pre-indexed into context-mode FTS5 for unified search:
- `source: "graphify-report"` — Architecture report, community analysis, node summaries
- `source: "graphify-graph"` — Full graph structure (nodes, edges, relationships)
- `source: "graphify-ast"` — AST-level code relationships
- `source: "graphify-manifest"` — Codebase metadata, file inventory

**Routing**: For architecture/component relationship questions, add `source: "graphify-graph"` to `ctx_search`. For code-level dependency traces, add `source: "graphify-ast"`. Rebuild graph with `graphify update` after significant code changes, then re-index.

## Parallel I/O batches

For multi-URL fetches or multi-API calls, **always** include `concurrency: N` (1-8):

- `context-mode_ctx_batch_execute(commands: [3+ network commands], concurrency: 5)` — gh, curl, dig, docker inspect, multi-region cloud queries
- `context-mode_ctx_fetch_and_index(requests: [{url, source}, ...], concurrency: 5)` — multi-URL batch fetch

**Use concurrency 4-8** for I/O-bound work (network calls, API queries). **Keep concurrency 1** for CPU-bound (npm test, build, lint) or commands sharing state (ports, lock files, same-repo writes).

GitHub API rate-limit: cap at 4 for `gh` calls.

## Output

Terse like caveman. Technical substance exact. Only fluff die.
Drop: articles, filler (just/really/basically), pleasantries, hedging. Fragments OK. Short synonyms. Code unchanged.
Pattern: [thing] [action] [reason]. [next step]. Auto-expand for: security warnings, irreversible actions, user confusion.
Write artifacts to FILES — never inline. Return: file path + 1-line description.
Descriptive source labels for `search(source: "label")`.

## Session Continuity

Skills, roles, and decisions persist for the entire session. Do not abandon them as the conversation grows.

## Memory

Session history is persistent and searchable. On resume, search BEFORE asking the user:

| Need | Command |
|------|---------|
| What did we decide? | `context-mode_ctx_search(queries: ["decision"], source: "decision", sort: "timeline")` |
| What constraints exist? | `context-mode_ctx_search(queries: ["constraint"], source: "constraint")` |

DO NOT ask "what were we working on?" — SEARCH FIRST.
If search returns 0 results, proceed as a fresh session.

## ctx commands

| Command | Action |
|---------|--------|
| `ctx stats` | Call `stats` MCP tool, display full output verbatim |
| `ctx doctor` | Call `doctor` MCP tool, run returned shell command, display as checklist |
| `ctx upgrade` | Call `upgrade` MCP tool, run returned shell command, display as checklist |
| `ctx purge` | Call `purge` MCP tool with confirm: true. Warns before wiping knowledge base. |

After /clear or /compact: knowledge base and session stats preserved. Use `ctx purge` to start fresh.

---

# Godspark — Agent Context

## What
- Forge 1.20.1 MineColonies companion mod that observes colony state, computes societal pressures, and generates deterministic story events.
- **NOT**: a replacement for MineColonies, a standalone simulation, or an LLM-driven NPC controller.

## Tech Stack
Minecraft 1.20.1 + Forge 47.x + Java 17 + MineColonies 1.20.1-1.1.x (Create optional)

## Architecture (flow)
```
MineColonies → ColonyObserver → PressureEngine → EventGenerator
                                                      ↓
                                            EventStateManager → MemoryEngine → MemoryBank
                                                      ↓                        ↓
                                            MemoryInfluence → PersonalityInfluence → PrayerSeedGenerator → PrayerSeedBank
                                                      ↓                                    ↓
                                            PersonalityEngine                    SacredSiteManager (Prayer Stone)
                                                      ↓
                                                 EventQueue → /godspark commands
```

## Build & Deploy
```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"; .\gradlew build --no-daemon; .\deploy.ps1
```
Test instance: `C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony`

## In-Game Commands
```
/godspark status      — Mod status, colony count
/godspark colonies    — List colonies with citizens/buildings
/godspark pressures   — Show pressure values per colony
/godspark events [N]  — Show recent events (default 10)
/godspark memories    — Show colony memories
/godspark influences  — Show memory threshold adjustments
/godspark prayers     — Show active prayer seeds
/godspark personality — Show colony personality traits + prayer tone
/godspark miracles    — Show active pressure modifiers
/godspark answer      — Player divine answer (permission 2)
/godspark ui          — Open debug dashboard
```

## Available Tools
- **graphify → context-mode bridge** — Codebase knowledge graph indexed into FTS5:
  - `ctx_search(queries: ["..."], source: "graphify-graph")` — architecture relationships
  - `ctx_search(queries: ["..."], source: "graphify-ast")` — code-level dependencies
  - `ctx_search(queries: ["..."], source: "graphify-report")` — community summaries
  - Rebuild: `graphify update "C:\Users\Suttawat\GodsparkForge"` then re-index after code changes
- **context-mode** — Context window optimizer MCP server:
  - `ctx stats` — Show context savings
  - `ctx doctor` — Run diagnostics
  - `ctx execute` / `ctx search` / `ctx batch_execute` — Sandbox tools (keep context lean)
  - See routing rules at top of this file for mandatory usage patterns

## TODOs
- Building classification uses string matching (need proper type checks)
- Sacred keywords: altar/sanctuary/oracle/ritual/rune/totem/spirit/divine/sacred/reliquary/obelisk; graveyard moved to gathering-only
- CHURCH threshold fix: score >= 4 for SHRINE (was >= 3)
- Pressure formulas need tuning with real data
- Memory decay not implemented (decayRate stored but unused)
- Phase 5D.2: Light World Effects not started
- Phase 6B: Colony Intentions not started
- Phase 6C: Civilization Evolution not started

## Reference
Detailed architecture, pressure formulas, package structure, design decisions: `docs/godspark-reference.md` (indexed via context-mode — search with `context-mode_ctx_search` before reading)
