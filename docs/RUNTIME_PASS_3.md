# Runtime pass 3: physical sandbox

Strict scoring rule: only behavior wired into the default graphical application receives gameplay credit. The default `application.mainClass` is now `com.samaheim.SamaheimSandboxGame`.

## 20 passes completed

1. Promoted the physical sandbox client to the default graphical runtime.
2. Wired gravity into player movement instead of snapping the camera to terrain every frame.
3. Wired Space jump with grounded state and ballistic return to the current terrain surface.
4. Made vertical physics sample the mutable terrain so raised/lowered ground changes landing height.
5. Added player-radius collision against live tree trunks.
6. Added player collision against live rock nodes.
7. Added player collision against built walls/campfires.
8. Kept terrain slope rejection in the physical movement path.
9. Made roaming enemies resolve movement against the same live blocker set.
10. Kept terrain editing targeted through the rendered terrain raycast/fallback reach logic.
11. Kept TerraformToolSystem resource and stamina costs in the graphical runtime.
12. Kept dirty-region terrain mesh updates rather than full mesh rebuilds per edit.
13. Added a placeable floor build type with resource cost.
14. Added a placeable wall build type that becomes a collision blocker.
15. Added a placeable campfire build type with stone/wood cost, light, and collision footprint.
16. Gated building by terrain slope and height variation so leveling has gameplay purpose.
17. Rejected build placement that overlaps live blockers.
18. Persisted mutable terrain edits in the physical sandbox save.
19. Persisted harvested resource IDs so gathered trees/rocks/berries do not respawn on reload.
20. Persisted placed structures plus player/resources/tool/build state across reloads.

## Evidence

- PR #5 CI run #80 completed successfully with compile and test green before merge.
- Regression coverage includes player/blocker collision, jumping and landing on edited terrain, terrain-to-building buildability, and TerraformToolSystem material/stamina costs.
- PR #5 was merged to `main` as commit `9cbdcd8ecbe6416402c88e9ab893c7cbeb471461`.

## Strict score

**2.2 / 10** for the actual playable runtime.

This score intentionally remains low. The runtime now has a materially more physical sandbox loop, but there is no evidence from an approximately one-hour graphical session. Major blockers remain: real 3D character controller/capsule and step handling, structural support/collapse, water/swimming/shore interaction, richer building pieces and snapping, chunk/world streaming, persistent enemy/world simulation, stronger combat, equipment progression, biome/resource progression, dungeon gameplay, audiovisual presentation, and an instrumented long graphical soak.

Stop reason for this execution: **20-pass cap reached**, not quality threshold reached.
