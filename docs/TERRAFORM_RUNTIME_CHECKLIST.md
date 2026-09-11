# Terraform runtime checklist

Use this when testing the current main build locally.

1. Craft the Mason's Hoe with 5 wood + 2 stone.
2. Cycle all five modes with `T`: Level, Raise, Cut, Smooth, Restore.
3. Aim down at terrain and confirm the colored marker follows the visible ground.
4. Raise ground with fewer than 2 stone and confirm the terrain does not change.
5. Raise ground with enough stone and confirm exactly 2 stone are consumed per successful strike.
6. Exhaust stamina and confirm terrain actions are rejected without spending resources.
7. Level a rough patch from different standing heights and confirm the plane follows the player's reference altitude.
8. Smooth a lumpy patch and verify small bumps soften without deleting a large raised mound.
9. Cut a trench and verify the player cannot walk directly up near-vertical edited slopes.
10. Level a camp footprint and confirm camp placement changes from rejected to accepted.
11. Perform repeated edits and watch for visible frame hitches; normal edits should update only a local mesh patch.
12. Save with F5, restart, and verify all coarse and fine terrain edits return.
13. Cycle to Restore and confirm edited ground trends back toward the generated shape.
14. Verify nearby trees, rocks and enemies snap to the edited ground rather than floating.
15. Play for at least 20 minutes around one heavily edited base and note clipping, collision, camera and mesh artifacts.

Do not raise the whole-game quality score based on this checklist unless the checks are actually performed in runtime.
