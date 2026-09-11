# Runtime quality pass 2

Strict baseline entering this execution: approximately **1.0/10**. The score is based on the graphical playable runtime only.

Twenty focused passes were applied/evaluated in this execution:

1. Verified `main` was still missing the already-green terrain PR and merged PR #3 into `main`.
2. Re-evaluated the runtime score without granting credit for headless architecture.
3. Identified physical sandbox feel as the dominant blocker after mutable terrain.
4. Added deterministic horizontal obstacle resolution for the next character-controller integration.
5. Added deterministic gravity/jump stepping for the next character-controller integration.
6. Added collision regression coverage for circular world blockers.
7. Added jump/gravity regression coverage.
8. Kept the physics helper out of the runtime score because it is not yet wired to the graphical client.
9. Clamped terrain brush size to a practical gameplay range.
10. Made raise-ground stone cost scale with brush area.
11. Made terrain-tool stamina scale with brush area.
12. Made coarse raise strength respond to local slope, reducing extreme spike formation.
13. Made dig strength respond to local slope, reducing harsh vertical cuts.
14. Made leveling strength respond to local roughness.
15. Made smoothing strength respond to local roughness.
16. Retuned restore strength for less abrupt terrain popping.
17. Improved player-facing terrain action feedback.
18. Added regression coverage for wide-brush resource pressure.
19. Added regression coverage for absurd-radius clamping and atomic failed actions.
20. Added regression coverage that smoothing does not make rough ground worse and prepared the branch for CI/main delivery.

## Runtime-visible changes in this execution

The existing graphical terrain tool calls `TerraformToolSystem.apply`, so passes 9-17 change the playable runtime directly: brush bounds, stone/stamina pressure, slope-sensitive sculpting, roughness-sensitive leveling/smoothing and action feedback.

The new `SandboxPhysics` code is deliberately **not** counted as a runtime-visible improvement yet. It must be wired into `SamaheimTerraformGame` before it can earn gameplay score.

## Strict score after this pass

Provisional: **1.4/10**, contingent on green CI. This is a small increase because the terrain tool already exists in the runtime and its interaction rules became more coherent, but the project is still an embryonic prototype.

Remaining dominant blockers: physical collision wired into runtime, gravity/jumping, persistent building pieces, structure collision/support, harvested-world persistence, larger/chunked world, biome/resource progression, combat depth, art/audio/presentation, and an actual graphical one-hour validation session.
