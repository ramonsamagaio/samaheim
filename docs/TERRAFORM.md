# Samaheim terrain system

The current terrain loop deliberately follows the *feel* of Valheim rather than copying its assets or code.

## Runtime behavior

- Generated heightfield is sampled at roughly 0.75 m spacing.
- Coarse raise/dig edits are limited to +/-8 m from the generated terrain.
- Fine leveling is limited to +/-1 m from the coarse surface, so the hoe cannot replace digging or filling.
- Level mode uses the player's standing altitude as its reference plane.
- Raise costs 2 stone per successful strike.
- Every terrain strike consumes stamina; failed strikes consume nothing.
- Smooth mode softens small local bumps without erasing major cliffs.
- Restore mode walks edited terrain back toward its generated shape.
- The terrain target is selected by raycasting the visible ground, with a short forward fallback when the camera ray is nearly horizontal.
- A colored world marker previews the active terrain target and mode.
- Camp placement rejects ground that is too steep or uneven, making leveling useful rather than cosmetic.
- Player traversal checks edited slope and step height.

## Performance model

TerrainState records the smallest dirty vertex rectangle for every edit. The playable client updates only that region plus a one-vertex normal border instead of rebuilding the complete terrain mesh after every tool strike.

## Save integrity

Both coarse sculpting and fine leveling are saved sparsely. Loading a terrain snapshot replaces existing edits instead of accidentally stacking them, malformed individual entries are ignored, and legacy single-layer saves remain accepted.

## Current limitations

This is still pre-alpha. The system does not yet have chunked infinite terrain, terrain texture painting/path creation, vegetation regrowth rules, structural foundation simulation, water interaction, cave subtraction, or multiplayer replication. Those are future gates before the terrain/building loop can be rated close to production quality.
