# Samaheim playable building physics loop

This audit scores only runtime behavior wired into the default graphical client. CI/tests are gates, not gameplay credit. No one-hour graphical session was performed in this execution.

## 20 passes

1. Re-evaluated `main` and identified decorative/non-supporting floors as the most damaging physical-sandbox blocker.
2. Replaced structure collision assumptions with oriented rectangular blocker support.
3. Reduced horizontal sweep increment for better anti-tunneling around thin walls.
4. Added rotated wall collision so 90-degree walls block their rendered footprint.
5. Added clearance regression so a thin wall no longer behaves like a large circular phantom obstacle.
6. Added deterministic floor support surfaces.
7. Added maximum step-up protection so nearby high floors cannot teleport an actor upward.
8. Added half-meter build grid snapping.
9. Added quarter-turn build rotation snapping.
10. Created `SamaheimBuilderGame` as the integrated graphical runtime rather than leaving building physics headless.
11. Wired player movement to mixed natural-circle + oriented-wall collision.
12. Wired player vertical support to built floors, making floors actually walkable.
13. Wired enemies to oriented wall collision.
14. Wired enemies to floor support so built decks participate in traversal consistently.
15. Added runtime build rotation input (`F`) and visible HUD rotation state.
16. Persisted build yaw and restored it on load.
17. Kept old four-field build records loadable by defaulting missing yaw to zero.
18. Prevented duplicate floors at the same snap point.
19. Added player-facing dismantling (`X`) with partial material recovery and structure rebuild bookkeeping.
20. Changed early progression text to require placing and walking onto a floor, making the new physical behavior part of the playable first-hour path.

## Evidence gate

- Java 21 compile: passed in CI run 158 before this audit-only commit.
- Regression coverage includes circular sweep, oriented walls, rotated walls, phantom-clearance, jumping/landing, floor support, high-floor step rejection, snapping and duplicate-floor detection.
- This branch must pass CI again after this audit commit before merge.

## Strict score

Estimated actual playable runtime: **2.9/10**.

Credit is only for behavior wired into the default client: mutable persistent terrain, gravity/jumping, obstacle collision, resource harvesting persistence, basic combat/survival, walkable floors, oriented walls, build rotation/snapping, persistent building and dismantling.

The score remains low because there is no instrumented hour-long graphical session, no capsule/height-aware collision, no structural support/collapse, no roofs/doors/stairs, no water/swimming, no chunk streaming, shallow combat/equipment progression, minimal biome/POI variety, no real dungeon loop, limited audio/VFX, and no measured first-hour pacing session.
