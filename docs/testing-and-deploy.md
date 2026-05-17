# Testing and Deploy

## Build Commands

```powershell
# Set Java 17
$env:JAVA_HOME = "C:\Program Files\Java\jdk-17"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Build
.\gradlew build --no-daemon

# Deploy to test instance
.\deploy.ps1
```

## Test Instance

```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony
```

## Deploy Script

`deploy.ps1` copies the built JAR from `build/libs/godspark-0.1.0.jar` to the test instance's `minecraft/mods` folder.

## Log Location

```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony\minecraft\logs\latest.log
```

Search for: `[Godspark]`, `[Godspark Divine]`, `[Godspark Miracle]`, `Exception`, `NullPointerException`

## SavedData Location

```
<world>/data/godspark_data.dat
```

## In-Game Commands

| Command | Permission | Description |
|---|---|---|
| `/godspark status` | 0 | Mod status, detected mods, colony count |
| `/godspark colonies` | 0 | List observed colonies with citizen/building counts |
| `/godspark pressures` | 0 | Show current pressure values per colony |
| `/godspark events [N]` | 0 | Show active/persistent/resolved events (default 10) |
| `/godspark memories [colonyId]` | 0 | Show colony memories |
| `/godspark influences` | 0 | Show memory threshold adjustments per colony |
| `/godspark prayers [colonyId]` | 0 | Show active prayer seeds with channels |
| `/godspark reflect <colonyId>` | 0 | AI/template reflection |
| `/godspark ai status` | 0 | AI configuration status |
| `/godspark answer <colonyId> <message>` | 2 | Player answers colony prayer, triggers divine interpretation |
| `/godspark miracles [colonyId]` | 0 | Show active pressure modifiers, expiry, cooldowns |

## Smoke Test Checklist

### A. Startup / Status

- [ ] Mod loads without crashes
- [ ] MineColonies detected: true
- [ ] `/godspark status` works
- [ ] `/godspark ai status` shows AI disabled or config

### B. Core Pipeline

- [ ] `/godspark colonies` lists colony after creation
- [ ] `/godspark pressures` shows 5 values (0-100, no NaN)
- [ ] `/godspark events` shows events (if pressure above thresholds)
- [ ] `/godspark memories` shows memories (after persistent events)
- [ ] `/godspark influences` does not crash (even with no memories)
- [ ] `/godspark prayers` shows prayer seeds with channels

### C. Divine Answer — `/godspark answer`

- [ ] `/godspark answer 1 stand firm through the raid` → structured response
- [ ] `/godspark answer 1 we need food` → FOOD domain detected
- [ ] `/godspark answer 1 what is happening` → ANSWER_QUESTION type
- [ ] Response shows IntentType, ValidationResult, Domain, Source, validation notes

### D. Validator Edge Cases

- [ ] No sacred building → ORACLE_APPROVED or DOWNGRADED, no effect
- [ ] Sacred building but wrong domain prayer → DOWNGRADED
- [ ] Matching domain prayer + crisis → possible EFFECT_ELIGIBLE
- [ ] Long message (>500 chars) → truncated cleanly
- [ ] Invalid colony ID → "Colony #N not found"

### E. Pressure Modifiers — `/godspark miracles`

- [ ] `/godspark miracles` → "No active miracle modifiers" before any
- [ ] After valid BLESS_COLONY with EFFECT_ELIGIBLE: modifier shown
- [ ] `/godspark miracles 1` shows `base=X → effective=Y`
- [ ] `/godspark pressures` shows modified values matching modifier
- [ ] After expiry: modifier gone, pressures return to base
- [ ] Cooldown displayed correctly

### F. Error Cases

- [ ] `/godspark answer` → Brigadier error (missing args)
- [ ] `/godspark answer 1` → Brigadier error (missing message)
- [ ] `/godspark answer 0 test` → "Colony #0 not found"
- [ ] `/godspark answer 999 test` → "Colony #999 not found"
- [ ] No exceptions in latest.log during any command

### G. Reflection

- [ ] `/godspark reflect 1` returns template reflection
- [ ] No server freeze (async call returns safely)

## Troubleshooting

1. **Mod not loading**: Check `latest.log` for `[Godspark]` entries
2. **No colonies detected**: Create a MineColonies colony first, wait 30s for scan
3. **No pressures**: Wait 60s for pressure computation cycle
4. **No events**: Increase colony pressure (reduce food, guards, etc.)
5. **No memories**: Wait for events to become PERSISTENT (3+ cycles)
6. **No EFFECT_ELIGIBLE**: Need sacred building + matching public prayer + crisis signal
7. **Modifier not showing**: Check `/godspark miracles` and `latest.log` for `[Godspark Miracle]`
8. **Crash on restart**: Check `godspark_data.dat` NBT structure compatibility
9. **Command hang**: Ensure async callbacks use `source.getServer().execute()`
10. **Pressure floor not working**: Verify `PressureModifierManager.getModifiedPressure()` clamps to 20