# Samaheim quality loop

## Target

A score of **9/10 or better** means a new player can play for roughly one hour without a progression-blocking bug, without running out of meaningful goals, and without the core loop collapsing into a single repeated action.

This score is deliberately stricter than "it launches".

## Rubric

Each review scores ten areas from 0 to 1:

1. Boot/build reliability
2. First-person controls and traversal
3. World generation and exploration
4. Resource gathering and survival
5. Crafting and equipment progression
6. Combat readability and enemy variety
7. Building and home-base value
8. POIs, dungeons, quests and discovery
9. Save/load and progression integrity
10. First-hour pacing and repetition resistance

## Current review history

### Pass 1 — repository bootstrap — 0.0 -> 3.1

The repository was empty. Added a Java 21/jMonkeyEngine project and a procedural first-person vertical slice with movement, survival meters, gathering, crafting, combat, day/night, enemies, landmarks, arcane seals, campfire placement, respawn, autosave and manual save.

### Pass 2 — progression integrity — 3.1 -> 3.3

Fixed an out-of-order Arcane Seal softlock and added a regression test.

### Pass 3 — compile/CI gate — 3.3 -> 3.8

Java 21 CI resolved jMonkeyEngine, compiled the game and passed the initial deterministic tests. Runtime playability remains a separate gate.

### Pass 4 — spawn safety and terrain traversal

Added deterministic world bounds and slope-speed rules. Very steep slopes become impassable instead of silently allowing mountain-goat movement.

### Pass 5 — collision and anti-clipping

Added circle-obstacle movement resolution with axis sliding, plus regression coverage for obstacle blocking and world-edge clamping. Runtime wiring into the rendered player controller is still required before this earns full traversal credit.

### Pass 6 — combat rules

Added explicit weapon damage, stamina costs and armor mitigation with diminishing returns and a non-zero minimum hit. This establishes measurable combat tiers rather than one opaque damage constant.

### Pass 7 — renewable resource economy

Added bounded respawn windows for berries, trees and rocks so a one-hour session has replenishment rules instead of permanent local depletion.

### Pass 8 — world-state persistence model

Added `WorldState` snapshots covering depleted resources, defeated unique enemies, discovered POIs, built structures, awakened seals and dungeon progress. Added round-trip and corruption regression tests.

### Pass 9 — biome differentiation

Added deterministic Greenwood, Mist Marsh, Ashen Highlands and Arcane Ruins assignment by world seed and position.

### Pass 10 — building validity

Added structure-placement checks for boundaries, terrain slope and blockers. This prevents impossible or overlapping placements at the rules layer.

### Pass 11 — crafting tiers and equipment choices

Added a progression catalog with starter blade/campfire/workbench plus Iron Sword, Leather Armor and Arcane Staff recipes. Advanced equipment is workbench-gated and material-gated.

### Pass 12 — enemy roster and encounter variety

Expanded the deterministic encounter roster to Goblin, Graveborn, Wolf, Cultist and Ogre, with biome-dependent composition and danger-tier scaling.

### Pass 13 — procedural POI distribution

Added a seeded `WorldPlanner` that requires spaced ruins, three shrines, iron camps, an arcane tower and a dungeon while preserving a safe spawn radius. Tiny invalid worlds fail explicitly instead of generating broken progression layouts.

### Pass 14 — dungeon loop

Added dungeon room-count rules, boss-gated completion and reward tiers based on depth cleared.

### Pass 15 — night pressure and camp incentive

Night uses a tighter encounter cadence and a higher enemy cap, while a camp safety radius suppresses encounter spawning near home.

### Pass 16 — first-hour pacing

Added deterministic first-hour milestones for starter craft, first POI, first seal, dungeon entry and dungeon boss to the headless pacing model.

### Pass 17 — runtime budget audit

Added hard caps for active enemies, dropped items, particles and structures per cell. Particle requests degrade with slow frame times rather than growing without bound.

### Pass 18 — failure/recovery integrity

World-state restoration rejects negative dungeon progress and invalid seal IDs; resource/structure flags can be reversed cleanly when resources respawn or structures are removed.

### Pass 19 — 60-minute soak model

Added a deterministic 3,600-second headless simulation asserting encounter cadence, night pressure, biome changes, resource respawns, danger escalation and milestone coverage. This is logic-level evidence only, not a substitute for a real graphical playtest.

### Pass 20 — final audit for this execution — 3.8 -> 5.4 provisional

The codebase is substantially safer to extend because major first-hour systems now have deterministic rules and regression tests. However, several of those rules are not yet wired into `SamaheimGame`'s rendered runtime, physical collision is not visually validated, the dungeon is not yet a playable interior, presentation remains primitive, and no real 60-minute graphical session has been completed.

**Current honest score: 5.4/10, pending green CI for this pass set.** The score must not reach 9 until the runtime uses these systems and a real or equivalent instrumented one-hour session demonstrates stability and sufficient variety.

## Next execution priorities

1. Integrate `FirstHourRules.resolveMovement` and slope limits into the live player controller.
2. Register tree/rock/ruin/building blockers from generated world objects.
3. Move combat, encounter direction and resource respawn onto the deterministic rule layer.
4. Persist `WorldState` through the save format and restore world changes on load.
5. Render biome material/prop differences and consume `WorldPlanner` POIs in `buildWorld()`.
6. Implement an actual dungeon entrance/interior/reward/exit loop.
7. Add runtime instrumentation for crashes, stuck movement, progression state and encounter cadence.
8. Complete a graphical 60-minute soak run before any 9/10 claim.

## Honesty rule

Never raise the score based only on code existing. A feature only earns reliability credit after compilation/tests, and runtime-sensitive claims require actual runtime evidence. If that evidence is unavailable, mark the area unverified rather than pretending it passed.
