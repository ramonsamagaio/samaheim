package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorldRulesTest {
    @Test
    void rejectsObstacleOverlapAndSlidesAlongFreeAxis() {
        List<WorldRules.Obstacle> obstacles = List.of(
                new WorldRules.Obstacle("tree", new WorldRules.Point(2f, 0f), 0.7f));
        WorldRules.Point from = new WorldRules.Point(0f, 0f);
        WorldRules.Point blocked = new WorldRules.Point(1.1f, 0f);
        assertFalse(WorldRules.canOccupy(blocked, WorldRules.PLAYER_RADIUS, 30f, obstacles));

        WorldRules.Point desired = new WorldRules.Point(1.1f, 1.5f);
        WorldRules.Point moved = WorldRules.slideMove(from, desired, WorldRules.PLAYER_RADIUS, 30f, obstacles);
        assertTrue(moved.distanceSquared(from) > 0f);
        assertTrue(WorldRules.canOccupy(moved, WorldRules.PLAYER_RADIUS, 30f, obstacles));
    }

    @Test
    void safeSpawnIsInsideWorldAndDeterministic() {
        long seed = 123456789L;
        List<WorldRules.Obstacle> obstacles = List.of(
                new WorldRules.Obstacle("spawn-blocker", new WorldRules.Point(0f, 0f), 3f));
        WorldRules.Point a = WorldRules.findSafeSpawn(seed, 80f, obstacles);
        WorldRules.Point b = WorldRules.findSafeSpawn(seed, 80f, obstacles);
        assertEquals(a, b);
        assertTrue(WorldRules.canOccupy(a, WorldRules.PLAYER_RADIUS, 80f, obstacles));
    }

    @Test
    void poiDistributionMaintainsRequestedSpacingWhenEnoughRoomExists() {
        List<WorldRules.Point> pois = WorldRules.distributePois(42L, 8, 110f, 18f);
        assertEquals(8, pois.size());
        for (int i = 0; i < pois.size(); i++) {
            for (int j = i + 1; j < pois.size(); j++) {
                assertTrue(pois.get(i).distanceSquared(pois.get(j)) >= 18f * 18f);
            }
        }
    }
}
