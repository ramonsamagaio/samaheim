package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class TerraformProductivityTest {
    @Test
    void nearlyCappedRaiseDoesNotSpendLikeFullBrush() {
        TerrainState terrain = new TerrainState(44L, 32f, 64, 1.2f);
        for (int i = 0; i < 18; i++) terrain.raise(0f, 0f, 2.2f, 0.7f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.RAISE,
                0f, 0f, terrain.sampleHeight(0f, 0f), TerraformToolSystem.DEFAULT_RADIUS, 20, 100f);
        if (result.applied()) {
            assertTrue(result.stoneSpent() <= 2);
            assertTrue(result.staminaSpent() < 9f);
        }
    }

    @Test
    void productiveDefaultRaiseStillChargesAtLeastOneStone() {
        TerrainState terrain = new TerrainState(9L, 32f, 64, 8f);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, TerraformToolSystem.Mode.RAISE,
                0f, 0f, terrain.sampleHeight(0f, 0f), TerraformToolSystem.DEFAULT_RADIUS, 20, 100f);
        assertTrue(result.applied());
        assertTrue(result.stoneSpent() >= 1);
        assertTrue(result.staminaSpent() > 0f);
    }
}
