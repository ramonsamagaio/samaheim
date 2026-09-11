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

Biggest blockers after the pass: no runtime validation yet, simplistic collision, limited crafting/building, no real dungeon interior, primitive presentation.

### Pass 2 — progression integrity — 3.1 -> 3.3

Found a genuine softlock: a player could activate all three Arcane Seals before reaching the seal quest, then arrive at that quest with no usable seals left. Reworked the progression machine so already-completed objectives advance automatically when their prerequisite stage is reached. Added a regression test for out-of-order completion.

## Mandatory remaining passes

The loop must continue for up to 20 passes, stopping early only after the project is honestly at least 9/10 and has evidence supporting that score.

High-priority review order:

3. Compile/CI gate and dependency correctness
4. Spawn safety and terrain traversal
5. Tree/rock/ruin collision and anti-clipping
6. Combat hit feedback, telegraphing and death edge cases
7. Resource economy and one-hour depletion/regrowth
8. Save/load world-state integrity
9. Biome differentiation
10. Building placement and structural usefulness
11. Crafting tiers and equipment choices
12. Enemy roster and encounter variety
13. Procedural POI distribution
14. Real dungeon loop with entrance/reward/exit
15. Night pressure and rest/home incentive
16. Quest pacing and guidance without hand-holding
17. Performance/allocation audit
18. Failure/recovery and save corruption resilience
19. 60-minute scripted soak-test checklist
20. Final quality audit against the full rubric

## Honesty rule

Never raise the score based only on code existing. A feature only earns reliability credit after compilation/tests, and runtime-sensitive claims require actual runtime evidence. If that evidence is unavailable, mark the area unverified rather than pretending it passed.
