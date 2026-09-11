# Samaheim quality loop

## Target

A score of **9/10 or better** means a new player can play for roughly one hour in the actual graphical build without a progression-blocking bug, without becoming stuck, and without the experience collapsing into obvious repetition.

## Scoring rule

The score measures the **playable game**, not the number of classes, tests, systems or planned features in the repository.

- Headless rules do not count as gameplay until wired into runtime.
- Unit tests can prove logic but cannot prove that the game feels good or is visually playable.
- A feature existing in source code earns no gameplay credit if the player cannot use it in the running build.
- Compilation/CI is a required gate, not a large score bonus.
- Runtime-sensitive claims require runtime evidence.
- 9/10 is impossible without an instrumented or human graphical session of roughly one hour.

## Recalibration

The previous 3.8/10 and 5.4/10 assessments were inflated by counting prototype feature presence and headless rule systems as if they represented a mature playable game. They did not.

**Correct baseline before the terrain pass: 1.0/10.**

At that point Samaheim was an embryonic first-person prototype with primitive geometry, shallow combat/building, tiny content depth, no validated one-hour loop and no Valheim-class terrain interaction.

## Current focus: runtime terrain deformation

The terrain pass is intentionally aimed at one of the systems that gives Valheim its physical sandbox identity.

Implemented in source:

1. Generated terrain now has an immutable original heightfield plus mutable per-sample deltas.
2. Terrain edits are capped at +/-8 m relative to each sample's original generated height.
3. Circular brushes use smooth falloff instead of editing a square block.
4. Level mode moves the target ground toward the altitude under the player's feet.
5. Raise mode costs stone and raises ground locally.
6. Cut mode lowers ground locally.
7. Restore mode blends edits back toward the original generated terrain.
8. The editable grid is 160x160 cells over the current world instead of the earlier coarse static 80x80 terrain.
9. The live player and enemies query the mutable heightfield for ground height.
10. The terrain render mesh is rebuilt after a deformation so the visual surface follows the authoritative heightfield.
11. Nearby natural props/enemies are re-snapped after local terrain edits; placed structures are intentionally not moved with the earth.
12. Terrain deltas are serialized into the existing save file and reloaded with the world seed.
13. Automated tests cover deformation caps, falloff, leveling, restore and save-data round trips.

Controls in the terraform build:

- `3`: craft Mason's Hoe (5 wood, 2 stone)
- `T`: cycle Level / Raise / Cut / Restore
- `G`: apply the selected terrain operation in front of the player
- Raise Ground consumes 2 stone per successful operation

## Score policy for this pass

Do not move the project far above **1.x/10** simply because this terrain system compiles. The score can increase meaningfully only after the runtime interaction is visually tested and additional core systems reach comparable depth.

Major blockers remain:

- actual physical collision against trees, rocks, ruins and placed structures
- proper character controller, gravity, jumping and fall behavior
- tool/equipment selection and first-person animation
- combat depth, hit reactions, blocking/dodging and enemy telegraphs
- building pieces, snapping, support/stability and shelter value
- persistent harvested resources and placed structures
- large procedural world/chunk streaming rather than the current small bounded map
- real biome transitions and resource progression
- dungeon interiors and boss loop
- audio, VFX, animation, models, UI and presentation
- multiplayer/server architecture
- one-hour graphical soak test

## Iteration workflow

For every development execution:

1. Run/evaluate the actual current game state where possible.
2. Assign a strict score based on what the player can really do.
3. Identify the most damaging blocker versus the Valheim-like target.
4. Implement a concrete runtime improvement.
5. Add regression tests for deterministic logic.
6. Compile/test in CI.
7. Re-evaluate.
8. If below 9/10, continue within the same execution, up to 20 improvement passes.
9. Work is not considered delivered until it is present on `main`.

## Honesty rule

The repository can contain sophisticated architecture while the game is still 1/10. Score the game the player experiences, not the engineering hidden underneath it.
