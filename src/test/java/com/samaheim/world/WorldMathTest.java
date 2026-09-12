package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WorldMathTest {
    @Test
    void terrainIsDeterministicForTheSameSeed() {
        long seed = 42L;
        float first = WorldMath.height(seed, 18.25f, -31.75f);
        float second = WorldMath.height(seed, 18.25f, -31.75f);
        assertEquals(first, second);
    }

    @Test
    void differentSeedsProduceDifferentTerrain() {
        float first = WorldMath.height(42L, 18.25f, -31.75f);
        float second = WorldMath.height(99L, 18.25f, -31.75f);
        assertNotEquals(first, second);
    }

    @Test
    void originalWorldFootprintRemainsBitExactForExistingSaves() {
        long seed = 998877L;
        float[][] points = {
                {0f, 0f}, {95f, 95f}, {-95f, 37f}, {48.25f, -80.75f}, {-12.5f, -95.5f}
        };
        for (float[] point : points) {
            assertEquals(WorldMath.legacyHeight(seed, point[0], point[1]),
                    WorldMath.height(seed, point[0], point[1]));
        }
    }

    @Test
    void frontierBlendsWithoutACliffAtTheOldWorldBoundary() {
        long seed = 4242L;
        float inside = WorldMath.height(seed, 95.9f, 21f);
        float outside = WorldMath.height(seed, 96.1f, 21f);
        assertTrue(Math.abs(outside - inside) < 0.35f);
    }

    @Test
    void expandedFrontierActuallyChangesMacroTerrain() {
        long seed = 73L;
        float x = 170f;
        float z = -143f;
        float legacy = WorldMath.legacyHeight(seed, x, z);
        float frontier = WorldMath.height(seed, x, z);
        assertTrue(Math.abs(frontier - legacy) > 0.05f);
    }

    @Test
    void expandedMapContainsMultipleNamedFrontierRegions() {
        long seed = 42L;
        Set<WorldMath.Region> regions = new HashSet<>();
        for (int z = -180; z <= 180; z += 30) {
            for (int x = -180; x <= 180; x += 30) {
                if (Math.max(Math.abs(x), Math.abs(z)) <= 100) continue;
                regions.add(WorldMath.region(seed, x, z));
            }
        }
        assertTrue(regions.size() >= 3, "frontier should expose at least three region identities");
    }
}