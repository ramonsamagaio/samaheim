# Samaheim

**Samaheim** is an original first-person procedural medieval-fantasy survival game written in Java. Its high-level rhythm is inspired by survival/exploration/building games such as Valheim, but its setting, code, systems, names and assets are original and lean into classic tabletop-fantasy adventure rather than Norse mythology.

## Current state

Pre-alpha first playable on branch `work/first-playable` / PR #1.

The current vertical slice already includes:

- seeded procedural terrain
- first-person WASD + mouse traversal
- health, stamina and hunger
- wood, stone, berry and arcane-dust gathering/economy
- crafting a first weapon and campfire kit
- goblin and graveborn combat
- roaming enemy reinforcement
- day/night lighting cycle
- ruins and three Arcane Seal exploration objectives
- quest/progression state machine
- campfire placement
- death/respawn loop
- manual save (`F5`) and autosave
- deterministic unit tests and GitHub CI

## Controls

| Input | Action |
| --- | --- |
| WASD | Move |
| Mouse | Look |
| Left Shift | Sprint |
| E | Gather / interact |
| Left mouse | Attack |
| 1 | Craft Wanderer's Blade |
| 2 | Craft campfire kit |
| Q | Place campfire kit |
| R | Eat emberberry |
| F5 | Save |

## First-hour critical path

1. Gather 8 wood and 4 stone.
2. Craft the Wanderer's Blade.
3. Defeat three goblin scouts.
4. Explore the world and awaken three Arcane Seals.
5. Build a campfire.
6. Continue surviving, exploring and fighting through the day/night cycle.

The progression system deliberately tolerates completing objectives out of order so exploration cannot accidentally softlock the quest chain.

## Running locally

Requirements:

- JDK 21
- Gradle 8.10+ (until a Gradle wrapper is committed)

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

## Quality target

The project is not considered "done" merely because it launches. The working target is **9/10 or better** against a strict first-hour rubric: stable build, reliable traversal, exploration, survival, progression, combat, building, save/load integrity, pacing and resistance to repetition.

See [`docs/QUALITY_LOOP.md`](docs/QUALITY_LOOP.md) for the 20-pass review plan and current score history.

## Save location

The prototype writes a small properties-based save to:

```text
~/.samaheim/save.properties
```

This format is intentionally simple during pre-alpha and will be migrated once world-state persistence becomes richer.
