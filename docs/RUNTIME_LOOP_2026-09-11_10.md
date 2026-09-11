# Samaheim runtime loop - physical controller pass

Scoring rule: only behavior wired into the graphical `SamaheimSandboxGame` receives gameplay credit. CI, tests and documentation are gates/evidence, not gameplay points.

Baseline entering this run: **2.2/10** strict playable-runtime score.

## 20 passes

1. Re-evaluated main and identified end-position-only obstacle collision as the most damaging traversal defect.
2. Replaced end-position obstacle resolution with swept/substep horizontal movement in `SandboxPhysics`, already called by the graphical player.
3. Limited each horizontal collision sample to 0.35 m to prevent sprint/frame-hitch tunneling through tree-sized blockers.
4. Added iterative depenetration for overlapping blockers so dense trees/build pieces do not leave the player embedded.
5. Preserved tangential displacement during depenetration so contact behaves as sliding instead of a hard movement freeze.
6. Added null-blocker tolerance so runtime collision cannot crash on an absent blocker collection.
7. Added invalid-coordinate fail-safe handling so corrupted movement state returns to the previous position instead of propagating NaN.
8. Re-evaluated vertical integration and kept frame time capped to 50 ms to prevent hitch-driven floor tunneling.
9. Tightened grounded tolerance to reduce camera jitter around edited terrain seams.
10. Explicitly zeroed downward velocity while grounded to prevent accumulated gravity from causing repeated ground snaps.
11. Kept jump impulse gated by grounded state so repeated Space presses cannot air-jump.
12. Added safe recovery for invalid vertical state, returning the runtime to terrain height rather than poisoning the camera transform.
13. Added regression coverage for high-speed tunneling through a tree-sized blocker.
14. Added regression coverage for diagonal collision preserving useful sliding movement.
15. Added regression coverage for overlapping blockers producing finite positions.
16. Added regression coverage for a severe frame hitch never dropping the player below terrain.
17. Re-evaluated terrain tool targeting and rejected world-outside targets rather than silently clamping edits to the map edge.
18. Added smoothing no-op detection so already-smooth terrain does not consume player stamina.
19. Added slope/roughness effort scaling so coarse work on steep terrain and fine work on rough terrain cost more stamina in the playable tool.
20. Added regression coverage for outside-world rejection, smoothing no-op charging, and terrain-effort scaling.

## Runtime-visible result

The current graphical build directly calls both modified systems. Player/enemy horizontal collision therefore receives swept collision immediately, gravity/jump uses the hardened vertical integration immediately, and the terrain hoe uses the revised effort/no-op rules immediately.

No claim of a one-hour graphical soak is made. This environment cannot operate the rendered game interactively, so 9/10 remains forbidden.

## Strict score after this run

**2.5/10**, pending green CI and merge to main. The increase is intentionally small because the build still lacks a robust capsule/step controller, structural support/collapse, water/swimming, richer snapping, world streaming, deeper progression/combat, enemy persistence and a verified long graphical session.

## Next runtime blockers

1. Replace circle-only blockers with shape-aware capsule/AABB collision for walls and build pieces.
2. Add explicit step-up/step-down handling rather than relying mostly on slope sampling.
3. Add build orientation/snapping and structural support rules to the graphical placement path.
4. Persist active enemy/world encounter state.
5. Instrument a real graphical soak session before allowing major score increases.
