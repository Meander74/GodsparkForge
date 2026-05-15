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

## In-Game Commands

| Command | Description |
|---|---|
| `/godspark status` | Mod status, detected mods, colony count |
| `/godspark colonies` | List observed colonies with citizen/building counts |
| `/godspark pressures` | Show current pressure values per colony |
| `/godspark events` | Show active/persistent/resolved events (default 10) |
| `/godspark events <N>` | Show N recent events (1-50) |
| `/godspark memories` | Show top 20 memories across all colonies |
| `/godspark memories <colonyId>` | Show top 10 memories for specific colony |

## Log Location

```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony\minecraft\logs\latest.log
```

## SavedData Location

```
<world>/data/godspark_data.dat
```

## Test Checklist

### Phase 1 (Observer)
- [ ] Mod loads without crashes
- [ ] MineColonies detected: true
- [ ] `/godspark status` works
- [ ] `/godspark colonies` lists colonies after creation

### Phase 2 (Pressure)
- [ ] `/godspark pressures` shows non-zero values
- [ ] Pressure values change with colony state

### Phase 3 (Events)
- [ ] Events generated when pressure exceeds thresholds
- [ ] `/godspark events` shows events
- [ ] No duplicate events within 5-minute cooldown

### Phase 3.5 (Persistence)
- [ ] Events persist across game restarts
- [ ] Active and resolved events correctly restored
- [ ] No data loss during shutdown

### Phase 4A (Memory)
- [ ] Memories generated from PERSISTENT events
- [ ] TRAUMA memories for HIGH severity events
- [ ] PATTERN memories for recurring pressure types
- [ ] `/godspark memories` shows memories
- [ ] Memories persist across game restarts
- [ ] Reinforcement works (intensity increases)

## Troubleshooting

1. **Mod not loading**: Check `latest.log` for `[Godspark]` entries
2. **No colonies detected**: Create a MineColonies colony first
3. **No pressures**: Wait 60 seconds for pressure computation cycle
4. **No events**: Increase colony pressure (reduce food, guards, etc.)
5. **No memories**: Wait for events to become PERSISTENT (3+ cycles)
6. **Crash on restart**: Check `godspark_data.dat` NBT structure compatibility

## Git Identity

```
Suttawat <suttawat@users.noreply.github.com>
```
