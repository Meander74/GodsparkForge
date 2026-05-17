# Experiment: UI-A Colony Grouping/Filtering

**Branch:** `experiment/ui-a-colony-grouping`
**Status:** Active experiment — do not merge to main without approval.

## Goal
Make Events, Memories, and Prayers easier to browse per colony, similar to Pressures.

## Scope
- Client-side colony selector/filter on the debug dashboard (`/godspark ui`)
- Filters Events, Memories, Prayers, and Pressures tabs by selected colony
- "All" option preserves current aggregate behavior

## Non-Goals
- No network payload changes (all entry types already carry colonyId)
- No persistence changes (GodsparkSavedData, NBT, schemaVersion untouched)
- No command output changes (/godspark commands unchanged)
- No new commands
- No server tick work
- No AI, world effects, or gameplay changes

## Implementation
- Colony filter bar rendered below tabs, populated from UiSnapshot colony data
- Each panel reads `GodsparkClientState.getSelectedColonyId()` and filters entries client-side
- `selectedColonyId == -1` means "All" (current behavior)
- Selected colony resets to "All" if it disappears from snapshot
- `[EXPERIMENT]` label in dashboard header

## Rollback
Delete branch: `git branch -D experiment/ui-a-colony-grouping`
