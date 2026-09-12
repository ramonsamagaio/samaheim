# Samaheim

**Samaheim** is an original first-person procedural medieval-fantasy survival game written in Java. Its high-level rhythm is inspired by survival/exploration/building games such as Valheim, but its setting, code, systems, names and assets are original and lean into classic tabletop-fantasy adventure rather than Norse mythology.

## Current state

Pre-alpha graphical sandbox on `main`.

The current playable build includes:

- fully volumetric editable terrain rather than a heightfield-only world
- real 3D digging, tunnels, overhangs and persistent caves
- first-person WASD + mouse traversal with gravity, jumping and cave collision
- health, stamina and hunger
- persistent gathering of wood, stone and berries
- melee combat and roaming enemies
- day/night lighting
- graphical HUD and projected terrain brush preview
- terrain modes for digging, adding earth and smoothing
- floors, walls, ramps, doors and campfires
- build rotation, dismantling and persisted structures
- save/load for terrain edits and world state
- deterministic regression tests and GitHub CI

## Windows playable

You no longer need to use `gradle run` just to play the current build.

Every green Windows build produces an artifact called **Samaheim-Windows** in GitHub Actions. Download it, unzip it and double-click:

```text
Samaheim\Samaheim.exe
```

The Windows package contains its own private Java runtime. The target PC does **not** need Java or Gradle installed.

To build the Windows executable locally from a development checkout with JDK 21 + Gradle available:

```powershell
.\tools\package-windows.ps1
```

Output:

```text
build\jpackage\Samaheim\Samaheim.exe
build\Samaheim-Windows.zip
```

## Current controls

| Input | Action |
| --- | --- |
| WASD | Move |
| Mouse | Look |
| Left Shift | Sprint |
| Space | Jump |
| E | Gather / interact / operate doors |
| Left mouse | Attack |
| Right mouse or G | Use terrain tool |
| T | Cycle terrain tool mode |
| Z / C | Decrease / increase terrain brush |
| 1 | Craft Wanderer's Blade |
| B | Cycle build piece |
| Q | Place build piece |
| F | Rotate build piece |
| X | Dismantle aimed build piece |
| R | Eat berry |
| F5 | Save |

## Development run

Requirements for developers:

- JDK 21
- Gradle 8.10+

```bash
gradle run
```

Tests:

```bash
gradle clean test
```

## Technology

- Java 21
- jMonkeyEngine 3.9.0-stable
- Gradle
- JUnit 5
- jpackage for self-contained Windows app images

## Quality target

The project is not considered complete merely because it launches or because systems exist in isolation. Quality scoring is based on the **actual graphical playable runtime**. CI is a required gate, but headless systems and tests do not receive gameplay credit until they are wired into the build the player actually opens.

A 9/10 score requires evidence comparable to an instrumented graphical play session of roughly one hour without progression-blocking bugs, stuck traversal or obvious repetitive stagnation.

## Save location

The pre-alpha save is written to:

```text
~/.samaheim/save.properties
```

The properties format is intentionally simple during rapid pre-alpha iteration and will be migrated as world persistence grows.
