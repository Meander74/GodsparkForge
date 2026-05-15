# Create'a Colony Test Instance

## Instance Path

```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony
```

## Mods Directory

```
C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony\minecraft\mods
```

## Purpose

This instance serves as the **rich integration/playtest environment** for Godspark. It is:

- A realistic Forge 1.20.1 modpack
- Contains MineColonies + Create ecosystem
- Used for compatibility testing
- Used for performance testing
- Used for future pressure/event testing

It is **not** the only development base. Development should happen in a clean Forge + MineColonies instance first, then Godspark is deployed here for integration testing.

## Instance Versions

| Mod | Version |
|-----|---------|
| Minecraft | 1.20.1 |
| Forge | 47.4.18 |
| MineColonies | 1.20.1-1.1.1197 |
| Structurize | 1.20.1-1.0.804 |
| BlockUI | 1.20.1-1.0.193 |
| Domum Ornamentum | 1.20.1-1.0.296 |
| MultiPiston | 1.20-1.2.43 |
| Create | 1.20.1-6.0.8 |

## Deploy Procedure

1. Build the mod:
   ```powershell
   .\gradlew.bat build
   ```

2. Deploy to instance:
   ```powershell
   .\deploy.ps1
   ```

3. Launch Create'a Colony in PrismLauncher.

## First Test

Before deploying Godspark:

1. Launch Create'a Colony **without** Godspark
2. Confirm MineColonies and Create work
3. Create a test colony in MineColonies
4. Deploy Godspark jar
5. Relaunch and run `/godspark status`

## Expected First Success

### Log Output

```
[Godspark] Godspark initialized
[Godspark] Version: 0.1.0
[Godspark] Minecraft: 1.20.1 | Forge: 47.x
[Godspark] MineColonies detected: true
[Godspark] Create detected: true
```

### Command: `/godspark status`

```
Godspark Status:
  Loaded: true
  Version: 0.1.0
  Minecraft: 1.20.1
  Loader: Forge
  MineColonies detected: true
  Create detected: true
  Observed colonies: 0
  Pressure engine: idle
  AI bridge: disabled
```

### Command: `/godspark colonies`

Before creating a colony:
```
Observed colonies: 0
No colonies detected yet. Create a colony in MineColonies first.
```

After creating a colony (eventual):
```
Observed colonies:
  - ID: 1 | Name: New Haven | Citizens: 12 | Buildings: 8
```

### Command: `/godspark pressures`

```
Pressures (placeholder):
  food_pressure: 0
  security_pressure: 0
  housing_pressure: 0
  comfort_pressure: 0
  industry_pressure: 0
```

## Troubleshooting

If Godspark crashes in Create'a Colony:

1. **Retest in a clean dev instance** to determine if the issue is:
   - Godspark itself
   - MineColonies integration
   - Modpack conflict
   - Create addon conflict
   - Version mismatch
   - Performance mod interaction

2. Check the latest log:
   ```
   C:\Users\Suttawat\AppData\Roaming\PrismLauncher\instances\Create'a Colony\minecraft\logs\latest.log
   ```

3. Look for `[Godspark]` prefixed log entries.
