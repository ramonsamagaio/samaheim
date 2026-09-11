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

        TerraformToolSystem.Result result = TerraformToolSystem.apply(
                terrain, TerraformToolSystem.Mode.RAISE, 0f, 0f, before,
                TerraformToolSystem.DEFAULT_RADIUS, 1, 100f);

        assertFalse(result.applied());
        assertEquals(0, result.stoneSpent());
        assertEquals(before, terrain.sampleHeight(0f, 0f), 0.001f);
    }

    @Test
    void successfulRaiseChargesResourcesOncePerToolStrike() {
        TerrainState terrain = new TerrainState(5L, 32f, 64, 8f);
        float before = terrain.sampleHeight(0f, 0f);

        TerraformToolSystem.Result result = TerraformToolSystem.apply(
                terrain, TerraformToolSystem.Mode.RAISE, 0f, 0f, before,
                TerraformToolSystem.DEFAULT_RADIUS, 12, 100f);

        assertTrue(result.applied());
        assertTrue(result.changedSamples() > 1);
        assertEquals(2, result.stoneSpent());
        assertEquals(7f, result.staminaSpent(), 0.001f);
        assertTrue(terrain.sampleHeight(0f, 0f) > before);
    }

    @Test
    void exhaustedPlayerCannotTerraform() {
        TerrainState terrain = new TerrainState(8L, 32f, 64, 8f);
        float before = terrain.sampleHeight(0f, 0f);

        TerraformToolSystem.Result result = TerraformToolSystem.apply(
                terrain, TerraformToolSystem.Mode.LOWER, 0f, 0f, before,
                TerraformToolSystem.DEFAULT_RADIUS, 0, 2f);

        assertFalse(result.applied());
        assertEquals(before, terrain.sampleHeight(0f, 0f), 0.001f);
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
    void smoothAndRestoreDoNotChargeStone() {
        TerrainState terrain = new TerrainState(17L, 32f, 64, 8f);
        terrain.raise(0f, 0f, 1f, 2f);

        TerraformToolSystem.Result smooth = TerraformToolSystem.apply(
                terrain, TerraformToolSystem.Mode.SMOOTH, 0f, 0f, terrain.sampleHeight(0f, 0f),
                TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);
        TerraformToolSystem.Result restore = TerraformToolSystem.apply(
                terrain, TerraformToolSystem.Mode.RESTORE, 0f, 0f, terrain.sampleHeight(0f, 0f),
                TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);

        assertEquals(0, smooth.stoneSpent());
        assertEquals(0, restore.stoneSpent());
    }
}
