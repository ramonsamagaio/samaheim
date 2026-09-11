package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TerrainTraversalTest {
    @Test
    void flatGroundAllowsFullTravel() {
        TerrainState terrain = new TerrainState(42L, 20f, 40, 8f);
        terrain.level(0f, 0f, 8f, terrain.sampleHeight(0f, 0f), 2f);
        TerrainTraversal.Result result = TerrainTraversal.resolve(terrain, 0f, 0f, 1.5f, 0f);
        assertFalse(result.blocked());
        assertEquals(1.5f, result.x(), 0.03f);
    }

    @Test
    void abruptRaisedShelfBlocksTravel() {
        TerrainState terrain = new TerrainState(7L, 20f, 40, 8f);
        for (int i = 0; i < 10; i++) terrain.raise(2f, 0f, 0.8f, 0.9f);
        TerrainTraversal.Result result = TerrainTraversal.resolve(terrain, 0f, 0f, 3f, 0f);
        assertTrue(result.blocked());
        assertTrue(result.x() < 2.5f);
    }

    @Test
    void smallEditedStepRemainsWalkable() {
        TerrainState terrain = new TerrainState(11L, 20f, 40, 8f);
        terrain.raise(1f, 0f, 1.2f, 0.18f);
        TerrainTraversal.Result result = TerrainTraversal.resolve(terrain, 0f, 0f, 1.4f, 0f);
        assertFalse(result.blocked());
    }

    @Test
    void rejectsNonFiniteInput() {
        TerrainState terrain = new TerrainState(1L, 20f, 40, 8f);
        assertTrue(TerrainTraversal.resolve(terrain, 0f, 0f, Float.NaN, 0f).blocked());
    }
}
