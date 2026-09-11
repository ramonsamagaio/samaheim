package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerrainStateTest {
    private static final float EPS = 0.03f;

    @Test
    void raiseAndLowerAreCappedAgainstOriginalTerrain() {
        TerrainState terrain = new TerrainState(42L, 32f, 64, 8f);
        float original = terrain.sampleHeight(0f, 0f);

        for (int i = 0; i < 80; i++) terrain.raise(0f, 0f, 3f, 0.7f);
        assertTrue(terrain.sampleHeight(0f, 0f) <= original + 8.05f);
        assertTrue(terrain.sampleHeight(0f, 0f) >= original + 7.5f);

        for (int i = 0; i < 160; i++) terrain.lower(0f, 0f, 3f, 0.7f);
        assertTrue(terrain.sampleHeight(0f, 0f) >= original - 8.05f);
        assertTrue(terrain.sampleHeight(0f, 0f) <= original - 7.5f);
    }

    @Test
    void fineLevelingCannotReplaceCoarseRaiseOrDig() {
        TerrainState terrain = new TerrainState(91L, 32f, 64, 8f);
        float sculpted = terrain.sampleHeight(0f, 0f);
        float absurdTarget = sculpted + 20f;

        for (int i = 0; i < 60; i++) terrain.level(0f, 0f, 3f, absurdTarget, 0.5f);

        float finalHeight = terrain.sampleHeight(0f, 0f);
        assertTrue(finalHeight <= sculpted + terrain.maxLevelOffset() + 0.04f);
        assertTrue(finalHeight >= sculpted + 0.82f);
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
    void editingResolutionIsFineEnoughForSmallBuildingFootprints() {
        TerrainState terrain = new TerrainState(7L, 32f, 16, 8f);
        assertTrue(terrain.cellSize() <= 0.76f);
    }

    @Test
    void levelMovesGroundTowardReferenceAltitude() {
        TerrainState terrain = new TerrainState(99L, 32f, 64, 8f);
        float target = terrain.sampleHeight(0f, 0f) + 0.8f;
        float before = terrain.sampleHeight(1f, 0f);

        for (int i = 0; i < 8; i++) terrain.level(1f, 0f, 2.5f, target, 0.5f);

        float after = terrain.sampleHeight(1f, 0f);
        assertTrue(Math.abs(after - target) < Math.abs(before - target));
    }

    @Test
    void smoothingReducesSmallScaleHeightVariation() {
        TerrainState terrain = new TerrainState(123L, 32f, 64, 8f);
        terrain.raise(0f, 0f, 1.1f, 0.9f);
        float before = terrain.heightVariation(0f, 0f, 2f);

        for (int i = 0; i < 8; i++) terrain.smooth(0f, 0f, 2.5f, 0.35f);

        float after = terrain.heightVariation(0f, 0f, 2f);
        assertTrue(after < before);
    }

    @Test
    void dirtyRegionTracksOnlyEditedPatchAndThenClears() {
        TerrainState terrain = new TerrainState(888L, 32f, 64, 8f);
        assertNull(terrain.consumeDirtyRegion());

        terrain.raise(3f, -4f, 2f, 0.7f);
        TerrainState.DirtyRegion region = terrain.consumeDirtyRegion();

        assertNotNull(region);
        assertTrue(region.sampleCount() > 0);
        assertTrue(region.sampleCount() < terrain.width() * terrain.width() / 4);
        assertNull(terrain.consumeDirtyRegion());
    }

    @Test
    void buildabilityRejectsRoughGroundAndAcceptsLeveledPatch() {
        TerrainState terrain = new TerrainState(421L, 32f, 64, 8f);
        terrain.raise(0f, 0f, 1f, 4f);
        assertFalse(terrain.isBuildable(0f, 0f, 2f, 12f, 0.65f));

        float target = terrain.sampleHeight(2f, 0f);
        for (int i = 0; i < 12; i++) {
            terrain.lower(0f, 0f, 1.2f, 0.5f);
            terrain.level(0f, 0f, 2.5f, target, 0.5f);
            terrain.smooth(0f, 0f, 2.5f, 0.35f);
        }

        assertTrue(terrain.heightVariation(0f, 0f, 2f) < 4f);
    }

    @Test
    void slopeMetricIncreasesWhenACliffIsSculpted() {
        TerrainState terrain = new TerrainState(17L, 32f, 64, 8f);
        float before = terrain.slopeDegrees(1.2f, 0f, 0.75f);
        for (int i = 0; i < 8; i++) terrain.raise(0f, 0f, 1.1f, 0.8f);
        float after = terrain.slopeDegrees(1.2f, 0f, 0.75f);
        assertTrue(after > before);
    }

    @Test
    void encodedDeltasRoundTripBothLayers() {
        TerrainState edited = new TerrainState(1234L, 32f, 64, 8f);
        edited.raise(2f, -3f, 4f, 1.25f);
        edited.lower(-4f, 5f, 2f, 0.75f);
        edited.level(2f, -3f, 2.5f, edited.sampleHeight(2f, -3f) + 0.7f, 0.7f);
        String encoded = edited.encodeDeltas();
        assertTrue(encoded.contains("s:"));
        assertTrue(encoded.contains("l:"));

        TerrainState restored = new TerrainState(1234L, 32f, 64, 8f);
        restored.decodeDeltas(encoded);

        assertEquals(edited.sampleHeight(2f, -3f), restored.sampleHeight(2f, -3f), EPS);
        assertEquals(edited.sampleHeight(-4f, 5f), restored.sampleHeight(-4f, 5f), EPS);
    }

    @Test
    void decodingNewSaveReplacesRatherThanStacksOldEdits() {
        TerrainState terrain = new TerrainState(31L, 32f, 64, 8f);
        terrain.raise(0f, 0f, 3f, 2f);

        TerrainState source = new TerrainState(31L, 32f, 64, 8f);
        source.lower(5f, 5f, 2f, 1f);
        terrain.decodeDeltas(source.encodeDeltas());

        assertEquals(source.sampleHeight(0f, 0f), terrain.sampleHeight(0f, 0f), EPS);
        assertEquals(source.sampleHeight(5f, 5f), terrain.sampleHeight(5f, 5f), EPS);
    }

    @Test
    void damagedSaveEntriesAreIgnoredWithoutLosingValidSamples() {
        TerrainState terrain = new TerrainState(1L, 32f, 64, 8f);
        int index = 3 * terrain.width() + 3;
        float before = terrain.vertexHeight(3, 3);

        terrain.decodeDeltas("garbage;s:" + index + ":2.5;l:not-a-number:2;s:999999:NaN");

        assertEquals(before + 2.5f, terrain.vertexHeight(3, 3), EPS);
    }

    @Test
    void legacyTerrainSaveStillLoads() {
        TerrainState terrain = new TerrainState(1L, 32f, 64, 8f);
        float before = terrain.vertexHeight(3, 3);
        int index = 3 * terrain.width() + 3;
        terrain.decodeDeltas(index + ":2.5");
        assertEquals(before + 2.5f, terrain.vertexHeight(3, 3), EPS);
    }

    @Test
    void restoreBrushWalksBothEditLayersBackTowardGeneratedShape() {
        TerrainState terrain = new TerrainState(55L, 32f, 64, 8f);
        float original = terrain.sampleHeight(0f, 0f);
        terrain.raise(0f, 0f, 3f, 3f);
        terrain.level(0f, 0f, 3f, terrain.sampleHeight(0f, 0f) + 0.8f, 0.8f);
        float edited = terrain.sampleHeight(0f, 0f);
        assertTrue(edited > original + 1f);

        for (int i = 0; i < 12; i++) terrain.restore(0f, 0f, 3f, 0.6f);
        assertEquals(original, terrain.sampleHeight(0f, 0f), 0.15f);
    }

    @Test
    void invalidBrushInputsDoNotPoisonTerrain() {
        TerrainState terrain = new TerrainState(55L, 32f, 64, 8f);
        float before = terrain.sampleHeight(0f, 0f);
        assertEquals(0, terrain.raise(Float.NaN, 0f, 3f, 1f));
        assertEquals(0, terrain.lower(0f, Float.POSITIVE_INFINITY, 3f, 1f));
        assertEquals(0, terrain.level(0f, 0f, -1f, 2f, 1f));
        assertEquals(before, terrain.sampleHeight(0f, 0f), EPS);
    }
}
