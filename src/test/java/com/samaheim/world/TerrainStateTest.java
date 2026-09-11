package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerrainStateTest {
    private static final float EPS = 0.02f;

    @Test
    void raiseAndLowerAreCappedAgainstOriginalTerrain() {
        TerrainState terrain = new TerrainState(42L, 32f, 64, 8f);
        float original = terrain.sampleHeight(0f, 0f);

        for (int i = 0; i < 80; i++) {
            terrain.raise(0f, 0f, 3f, 0.7f);
        }
        assertTrue(terrain.sampleHeight(0f, 0f) <= original + 8.05f);
        assertTrue(terrain.sampleHeight(0f, 0f) >= original + 7.5f);

        for (int i = 0; i < 160; i++) {
            terrain.lower(0f, 0f, 3f, 0.7f);
        }
        assertTrue(terrain.sampleHeight(0f, 0f) >= original - 8.05f);
        assertTrue(terrain.sampleHeight(0f, 0f) <= original - 7.5f);
    }

    @Test
    void brushHasFalloffInsteadOfMovingAWholeSquare() {
        TerrainState terrain = new TerrainState(7L, 32f, 64, 8f);
        float centreBefore = terrain.sampleHeight(0f, 0f);
        float edgeBefore = terrain.sampleHeight(2.5f, 0f);

        terrain.raise(0f, 0f, 3f, 1f);

        float centreDelta = terrain.sampleHeight(0f, 0f) - centreBefore;
        float edgeDelta = terrain.sampleHeight(2.5f, 0f) - edgeBefore;
        assertTrue(centreDelta > 0.6f);
        assertTrue(edgeDelta >= -EPS);
        assertTrue(edgeDelta < centreDelta);
    }

    @Test
    void levelMovesGroundTowardReferenceAltitude() {
        TerrainState terrain = new TerrainState(99L, 32f, 64, 8f);
        float target = terrain.sampleHeight(0f, 0f) + 2f;
        float before = terrain.sampleHeight(1f, 0f);

        for (int i = 0; i < 8; i++) {
            terrain.level(1f, 0f, 2.5f, target, 0.5f);
        }

        float after = terrain.sampleHeight(1f, 0f);
        assertTrue(Math.abs(after - target) < Math.abs(before - target));
    }

    @Test
    void encodedDeltasRoundTrip() {
        TerrainState edited = new TerrainState(1234L, 32f, 64, 8f);
        edited.raise(2f, -3f, 4f, 1.25f);
        edited.lower(-4f, 5f, 2f, 0.75f);
        String encoded = edited.encodeDeltas();
        assertTrue(encoded.length() > 10);

        TerrainState restored = new TerrainState(1234L, 32f, 64, 8f);
        restored.decodeDeltas(encoded);

        assertEquals(edited.sampleHeight(2f, -3f), restored.sampleHeight(2f, -3f), EPS);
        assertEquals(edited.sampleHeight(-4f, 5f), restored.sampleHeight(-4f, 5f), EPS);
    }

    @Test
    void restoreBrushWalksEditsBackTowardGeneratedShape() {
        TerrainState terrain = new TerrainState(55L, 32f, 64, 8f);
        float original = terrain.sampleHeight(0f, 0f);
        terrain.raise(0f, 0f, 3f, 3f);
        float edited = terrain.sampleHeight(0f, 0f);
        assertTrue(edited > original + 1f);

        for (int i = 0; i < 8; i++) {
            terrain.restore(0f, 0f, 3f, 0.6f);
        }
        assertEquals(original, terrain.sampleHeight(0f, 0f), 0.12f);
    }
}
