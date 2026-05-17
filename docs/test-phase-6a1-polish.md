# Phase 6A.1 — In-Game Test Cases

## Prerequisites
- Launch PrismLauncher instance: `Create'a Colony`
- Ensure MineColonies is loaded with at least 1 active colony
- Wait for first pressure cycle (~60 seconds / 1200 ticks)

---

## Test 1: Personality Computation
**Command:** `/godspark personality`

**Expected:**
- Lists all colonies with personality label
- Format: `#<id> <name>: <Primary> / <Secondary> | Tone: <Tone>`
- Example: `#1 New Haven: Spiritual / Resilient | Tone: Devout`

**Pass criteria:** No errors, at least 1 colony shown, tone assigned

---

## Test 2: Personality Detail View
**Command:** `/godspark personality 1` (use actual colony ID)

**Expected output includes:**
```
Personality of New Haven (#1):
  Primary: Spiritual
  Secondary: Resilient
  Description: ...
  Prayer Tone: Devout
  Scores: aggression=0, trade=0, expansion=0, spirituality=8
  Threshold Influence:
    COMFORT: -5 (events easier to trigger)
    INDUSTRY: +3 (events harder to trigger)
  Evidence: Spiritual, Resilient, ...
```

**Pass criteria:**
- Shows primary + secondary traits
- Shows prayer tone
- Shows threshold influence with direction labels
- Shows evidence list

**Fail criteria:**
- Missing tone
- Missing threshold influence section
- Shows "none" when traits have non-zero adjustments

---

## Test 3: Prayer Reason Codes Include Personality
**Command:** `/godspark prayers`

**Expected in reason codes:**
- `PERSONALITY_TRAIT_SPIRITUAL` (or whatever primary trait)
- `PERSONALITY_TONE_DEVOUT` (or whatever tone maps to)

**Example prayer line:**
```
[Church Thanks 65][Comfort] At the chapel, New Haven gives thanks with reverence for the peace... (reasons=EVENT_RESOLVED, SACRED_SITE_PRESENT, PUBLIC_PRAYER, PRAYER_CHANNEL_CHURCH, PERSONALITY_TRAIT_SPIRITUAL, PERSONALITY_TONE_DEVOUT)
```

**Pass criteria:** At least one prayer shows `PERSONALITY_TRAIT_*` and `PERSONALITY_TONE_*`

---

## Test 4: Prayer Text Uses Adverbial Phrases
**Command:** `/godspark prayers`

**Check prayer text for each tone:**

| Tone | Example phrase | Example output |
|------|---------------|----------------|
| DEVOUT | "with reverence" | "...gives thanks with reverence for..." |
| DEFIANT | "fiercely" | "...calls fiercely for shields..." |
| FEARFUL | "with trembling voices" | "...whisper, with trembling voices, of..." |
| STOIC | "steadily" | "...dream steadily of workshops..." |
| GRATEFUL | "with thankful hearts" | "...gives thanks with thankful hearts for..." |
| HOPEFUL | "with quiet hope" | "...speak with quiet hope of harvests..." |

**Pass criteria:** Prayer text contains adverbial phrases, NOT awkward adjective insertions like "seeks shelter holy for"

**Fail criteria:**
- "seeks shelter [adjective] for"
- "remembers hunger [adjective]."
- "[Adjective] fears move through" (should be "With trembling voices, fears move through")

---

## Test 5: Event Thresholds Reflect Personality
**Setup:** Create conditions that trigger events (high pressure)

**Command:** `/godspark events`

**Check:**
- If colony has AGGRESSIVE personality → SECURITY events should trigger at lower pressure (threshold -5)
- If colony has RESILIENT personality → events harder to trigger across all types (threshold +2)
- If colony has SPIRITUAL personality → COMFORT events trigger easier (-5), INDUSTRY harder (+3)

**Command:** `/godspark pressures` — note pressure values
**Command:** `/godspark events` — check which events fired

**Pass criteria:** Events fire consistent with personality-adjusted thresholds

---

## Test 6: Effective Threshold in Event Logs
**Check server log** (latest.log in instance folder)

**Look for:**
```
[Godspark Event] [MEDIUM][Security] New Haven: ... (pressure=68, threshold=65)
```

**Expected:** `threshold` should be the **effective** threshold (base + personality + memory adjustments), NOT the base threshold from StoryEventType enum.

**Pass criteria:** threshold value differs from base enum value when personality/memory adjustments exist

---

## Test 7: Colony Summary Shows Personality
**Check server log** for `[Godspark Summary]` lines

**Expected:**
```
[Godspark Summary] Colony #1 (New Haven): citizens=10 buildings=15 ...
  PRESSURES: Food=30 Security=45 ...
  EVENTS: Security_MEDIUM(ACTIVE)
  MEMORIES: Security:Trauma(15)
  PRAYERS: Church:Thanks(65)
  PERSONALITY: Spiritual/Resilient
```

**Pass criteria:** PERSONALITY line appears in summary with trait names

---

## Test 8: Temple Channel for Spiritual Colonies
**Setup:** Colony with SPIRITUAL primary trait and 2+ sacred buildings

**Command:** `/godspark prayers`

**Expected:** Channel shows `Temple` (not just `Church`)

**Pass criteria:**
- 0 sacred → PRIVATE
- 1 sacred → CHURCH
- 2 true sacred buildings → SHRINE
- 3+ true sacred buildings → TEMPLE
- Prayer Stone only → CHURCH, never SHRINE/TEMPLE

---

## Test 9: No Same-Cycle Feedback
**Method:**
1. Note current personality: `/godspark personality 1`
2. Create a pressure spike (e.g., trigger raid, remove food buildings)
3. Wait ONE pressure cycle (60 seconds)
4. Check events immediately after cycle

**Expected:** Events in cycle N use personality from cycle N-1 (cached), not personality computed from current cycle's observations.

**Pass criteria:** Personality changes lag by one cycle (observable in logs — personality update timestamp should be AFTER event generation timestamp)

---

## Test 10: Personality Does NOT Affect Pressure
**Command:** `/godspark pressures`

**Check:**
- Pressure values should be identical regardless of personality
- Only event thresholds and prayer text/intensity change

**Method:**
1. Record pressure values
2. Change colony buildings to shift personality
3. Wait for pressure cycle
4. Compare pressure values — should be same for same conditions

**Pass criteria:** Pressure values unchanged by personality

---

## Test 11: Dashboard UI (Optional)
**Command:** `/godspark ui`

**Expected:** Dashboard opens, shows 6 tabs
- Status tab should show colony count
- Colonies tab should list colonies
- Pressures tab should show values
- Events tab should show active events
- Memories tab should show memories
- Prayers tab should show prayers with reason codes

**Pass criteria:** All tabs render, no crashes

---

## Regression Tests
**Command:** `/godspark status` — mod loads, AI status shown
**Command:** `/godspark colonies` — colonies listed
**Command:** `/godspark answer 1 "The colony shall prosper"` — divine answer works
**Command:** `/godspark miracles` — miracle system works
**Command:** `/godspark reflect 1` — AI reflection works (if AI enabled)
**Command:** `/godspark influences` — memory influence shown

**Pass criteria:** All existing commands still work

---

## Known Edge Cases to Watch
1. **Integer.MIN_VALUE** — extremely unlikely but `Math.floorMod` should handle it
2. **No personality data yet** — first cycle before `updateFromObservations()` runs → prayers should use STOIC tone (default)
3. **Colony with no sacred buildings** → PRIVATE channel, personality still affects text
4. **All thresholds at 0** — effective threshold clamped to minimum 0
5. **Combined clamp overflow** — memory -20 + personality -5 = -25 → clamped to -20

---

## Quick Smoke Test (5 minutes)
```
/godspark status
/godspark personality
/godspark personality 1
/godspark pressures
/godspark events
/godspark prayers
/godspark ui
```

If all 7 commands return valid output without errors → Phase 6A.1 is stable.
