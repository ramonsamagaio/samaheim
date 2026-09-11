package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerraformToolSystemTest {
    @Test
    void raisingWithoutStoneNeverMutatesTerrain() {
        TerrainState terrain = new TerrainState(5L, 32f, 64, 8f);
        float before = terrain.sampleHeight(0f, 0f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.RAISE,
                0f, 0f, before, TerraformToolSystem.DEFAULT_RADIUS, 1, 100f);
        assertFalse(result.applied());
        assertEquals(0, result.stoneSpent());
        assertEquals(before, terrain.sampleHeight(0f, 0f), 0.001f);
    }

    @Test
    void successfulDefaultRaiseChargesResourcesOncePerStrike() {
        TerrainState terrain = new TerrainState(5L, 32f, 64, 8f);
        float before = terrain.sampleHeight(0f, 0f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.RAISE,
                0f, 0f, before, TerraformToolSystem.DEFAULT_RADIUS, 12, 100f);
        assertTrue(result.applied());
        assertTrue(result.changedSamples() > 1);
        assertEquals(2, result.stoneSpent());
        assertTrue(result.staminaSpent() >= 7f);
        assertTrue(terrain.sampleHeight(0f, 0f) > before);
    }

    @Test
    void wideRaiseCostsMoreStoneAndStaminaThanDefaultBrush() {
        TerrainState defaultTerrain = new TerrainState(21L, 32f, 64, 8f);
        float before = defaultTerrain.sampleHeight(0f, 0f);
        TerraformToolSystem.Result normal = TerraformToolSystem.apply(defaultTerrain, TerraformToolSystem.Mode.RAISE,
                0f, 0f, before, TerraformToolSystem.DEFAULT_RADIUS, 20, 100f);
        TerrainState wideTerrain = new TerrainState(21L, 32f, 64, 8f);
        TerraformToolSystem.Result wide = TerraformToolSystem.apply(wideTerrain, TerraformToolSystem.Mode.RAISE,
                0f, 0f, before, 4.2f, 20, 100f);
        assertTrue(wide.applied());
        assertTrue(wide.stoneSpent() > normal.stoneSpent());
        assertTrue(wide.staminaSpent() > normal.staminaSpent());
    }

    @Test
    void absurdBrushRadiusIsClampedInsteadOfEditingWholeWorld() {
        TerrainState terrain = new TerrainState(33L, 32f, 64, 8f);
        float before = terrain.sampleHeight(0f, 0f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.LOWER,
                0f, 0f, before, 500f, 0, 100f);
        assertTrue(result.applied());
        assertTrue(result.changedSamples() < terrain.width() * terrain.width() / 3);
    }

    @Test
    void exhaustedPlayerCannotTerraform() {
        TerrainState terrain = new TerrainState(8L, 32f, 64, 8f);
        float before = terrain.sampleHeight(0f, 0f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.LOWER,
                0f, 0f, before, TerraformToolSystem.DEFAULT_RADIUS, 0, 2f);
        assertFalse(result.applied());
        assertEquals(0f, result.staminaSpent(), 0.001f);
        assertEquals(before, terrain.sampleHeight(0f, 0f), 0.001f);
    }

    @Test
    void cappedTerrainDoesNotChargeAFailedRaise() {
        TerrainState terrain = new TerrainState(44L, 32f, 64, 1.2f);
        float reference = terrain.sampleHeight(0f, 0f);
        for (int i = 0; i < 30; i++) terrain.raise(0f, 0f, TerraformToolSystem.DEFAULT_RADIUS, 1f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.RAISE,
                0f, 0f, reference, TerraformToolSystem.DEFAULT_RADIUS, 50, 100f);
        if (!result.applied()) {
            assertEquals(0, result.stoneSpent());
            assertEquals(0f, result.staminaSpent(), 0.001f);
        }
    }

    @Test
    void levelingUsesStandingAltitudeAsReference() {
        TerrainState terrain = new TerrainState(99L, 32f, 64, 8f);
        float standing = terrain.sampleHeight(-2f, 0f);
        float beforeError = Math.abs(terrain.sampleHeight(1.5f, 0f) - standing);
        for (int i = 0; i < 5; i++) {
            TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.LEVEL, 1.5f, 0f, standing,
                    TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);
        }
        float afterError = Math.abs(terrain.sampleHeight(1.5f, 0f) - standing);
        assertTrue(afterError <= beforeError + 0.001f);
    }

    @Test
    void smoothingDoesNotMakeRoughGroundWorse() {
        TerrainState terrain = new TerrainState(77L, 32f, 64, 8f);
        terrain.raise(0f, 0f, 1.2f, 2.5f);
        float roughBefore = terrain.heightVariation(0f, 0f, 2f);
        for (int i = 0; i < 4; i++) {
            TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.SMOOTH, 0f, 0f,
                    terrain.sampleHeight(0f, 0f), TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);
        }
        assertTrue(terrain.heightVariation(0f, 0f, 2f) <= roughBefore + 0.02f);
    }

    @Test
    void smoothAndRestoreDoNotChargeStone() {
        TerrainState terrain = new TerrainState(17L, 32f, 64, 8f);
        terrain.raise(0f, 0f, 1f, 2f);
        TerraformToolSystem.Result smooth = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.SMOOTH,
                0f, 0f, terrain.sampleHeight(0f, 0f), TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);
        TerraformToolSystem.Result restore = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.RESTORE,
                0f, 0f, terrain.sampleHeight(0f, 0f), TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);
        assertEquals(0, smooth.stoneSpent());
        assertEquals(0, restore.stoneSpent());
    }

    @Test
    void invalidTargetCannotMutateTerrainOrSpendResources() {
        TerrainState terrain = new TerrainState(1L, 32f, 64, 8f);
        String before = terrain.encodeDeltas();
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.RAISE,
                Float.NaN, 0f, 0f, 2.8f, 99, 99f);
        assertFalse(result.applied());
        assertEquals(0, result.stoneSpent());
        assertEquals(0f, result.staminaSpent(), 0.001f);
        assertEquals(before, terrain.encodeDeltas());
    }

    @Test
    void worldOutsideTargetIsRejectedRatherThanClampedToEdge() {
        TerrainState terrain = new TerrainState(1L, 32f, 64, 8f);
        String before = terrain.encodeDeltas();
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.LOWER,
                1000f, 0f, 0f, 2.8f, 0, 100f);
        assertFalse(result.applied());
        assertEquals(before, terrain.encodeDeltas());
    }

    @Test
    void alreadySmoothGroundDoesNotWasteStamina() {
        TerrainState terrain = new TerrainState(13L, 32f, 64, 8f);
        float h = terrain.sampleHeight(0f, 0f);
        for (int i = 0; i < 20; i++) terrain.level(0f, 0f, 3f, h, 1f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.SMOOTH,
                0f, 0f, h, TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);
        if (!result.applied()) assertEquals(0f, result.staminaSpent(), 0.001f);
    }

    @Test
    void steeperCoarseWorkCostsMoreStamina() {
        TerrainState flat = new TerrainState(22L, 32f, 64, 8f);
        float h = flat.sampleHeight(0f, 0f);
        for (int i = 0; i < 10; i++) flat.level(0f, 0f, 4f, h, 1f);
        TerraformToolSystem.Result easy = TerraformToolSystem.apply(flat, TerraformToolSystem.Mode.LOWER,
                0f, 0f, h, 2.8f, 0, 100f);

        TerrainState steep = new TerrainState(22L, 32f, 64, 8f);
        steep.raise(1.2f, 0f, 1f, 5f);
        TerraformToolSystem.Result hard = TerraformToolSystem.apply(steep, TerraformToolSystem.Mode.LOWER,
                0f, 0f, steep.sampleHeight(0f, 0f), 2.8f, 0, 100f);
        assertTrue(easy.applied());
        assertTrue(hard.applied());
        assertTrue(hard.staminaSpent() >= easy.staminaSpent());
    }
}
