package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrontierCaveMathTest {
    @Test
    void legacyWorldColumnsNeverReceiveNaturalCaves() {
        assertFalse(FrontierCaveMath.columnMayContainCaves(0f, 0f));
        assertFalse(FrontierCaveMath.columnMayContainCaves(95f, -95f));
        assertEquals(8f, FrontierCaveMath.caveDensity(42L, 95f, -7f, -95f, 0f));
    }

    @Test
    void caveFieldIsDeterministicAndContainsRealAirVolumes() {
        long seed = 42L;
        float surface = WorldMath.height(seed, 120f, -50f);
        float y = surface - 5f;
        float first = FrontierCaveMath.caveDensity(seed, 120f, y, -50f, surface);
        float second = FrontierCaveMath.caveDensity(seed, 120f, y, -50f, surface);

        assertEquals(first, second);
        assertTrue(first < 0f, "known frontier corridor should be cave air");
    }

    @Test
    void volumetricTerrainActuallyContainsTheNaturalCaveAir() {
        long seed = 42L;
        VolumetricTerrain terrain = new VolumetricTerrain(seed, 128f, -24f, 28f, 1f, 16);
        float surface = WorldMath.height(seed, 120f, -50f);
        float caveDensity = terrain.sampleDensity(120f, surface - 5f, -50f);
        float solidNeighbor = terrain.sampleDensity(120f, surface - 5f, -20f);

        assertTrue(caveDensity < VolumetricTerrain.ISO, "frontier corridor must be traversable air in the real density field");
        assertTrue(solidNeighbor > VolumetricTerrain.ISO, "nearby underground terrain should remain solid");
    }

    @Test
    void frontierChunkRangeIncludesSubterraneanCaveDepth() {
        VolumetricTerrain terrain = new VolumetricTerrain(42L, 128f, -24f, 28f, 1f, 16);
        int[] frontier = terrain.initialChunkYRange(15, 4);
        int[] legacy = terrain.initialChunkYRange(8, 8);

        assertTrue(frontier[0] <= legacy[0]);
        assertTrue(frontier[1] >= frontier[0]);
    }
}