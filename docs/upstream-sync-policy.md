# Upstream Sync Policy

Because GodsparkNeo is a MineColonies fork, preserving upstream mergeability is critical.

## Rules

1. **Keep upstream remote:**
   ```bash
   git remote add upstream https://github.com/ldtteam/minecolonies.git
   ```

2. **Keep Godspark changes isolated** where possible.
   - Additive classes preferred.
   - Small integration points preferred.
   - Avoid modifying upstream files unless necessary.

3. **Avoid mass formatting** upstream files.
   - Do not reformat entire classes.
   - Keep diffs minimal and reviewable.

4. **Avoid package-wide renames** of upstream code.
   - Do not rename `com.minecolonies.*` packages.
   - Do not rename `com.ldtteam.*` packages.

5. **Avoid runtime mod-id rename.**
   - Keep `minecolonies` as the runtime mod ID.

6. **Document every upstream file modified for Godspark.**
   - Add a comment near the change: `// GodsparkNeo: <reason>`
   - List modified files in AGENTS.md or this doc.

## Sync Workflow

```bash
# Fetch latest upstream
git fetch upstream

# Checkout main
git checkout main

# Merge upstream version/1.21
git merge upstream/version/1.21

# Resolve conflicts
git mergetool

# Build and test
.\gradlew build --no-daemon

# Commit merge
git commit
```

## Modified Upstream Files (Track Here)

| File | Reason | Phase |
|------|--------|-------|
| `README.md` | GodsparkNeo branding | B |
| `AGENTS.md` | Project agent briefing | B |
| *(none yet)* | | |

## Branches

| Branch | Purpose |
|--------|---------|
| `main` | GodsparkNeo fork main. Merges from `upstream/version/1.21`. |
| `upstream/version-main-sync` | Optional tracking branch for upstream HEAD. |
| `feature/*` | Feature branches. Rebase on `main` before PR. |

## Merge Conflict Strategy

When merging upstream:
1. Prefer upstream's code for shared files unless the Godspark change is essential.
2. If a Godspark integration point conflicts, re-apply the integration minimally.
3. If upstream refactored a class Godspark depends on, update the integration point.
4. Build and test after every merge before pushing.
