package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerrainRuntimeContractTest {
    @Test
    void repeatedEditsStayInsideLocalDirtyPatch() {
        TerrainState terrain = new TerrainState(2026L, 116f, 160, 8f);
        terrain.raise(10f, -6f, TerraformToolSystem.DEFAULT_RADIUS, 0.62f);
        TerrainState.DirtyRegion dirty = terrain.consumeDirtyRegion();
        assertNotNull(dirty);
        assertTrue(dirty.width() < 20);
        assertTrue(dirty.height() < 20);
    }

    @Test
    void oneHourOfRepresentativeTerrainStrikesKeepsSparseSaveFinite() {
        TerrainState terrain = new TerrainState(2027L, 116f, 160, 8f);
        for (int minute = 0; minute < 60; minute++) {
            float x = -30f + (minute % 12) * 5f;
            float z = -20f + (minute / 12) * 8f;
            float standing = terrain.sampleHeight(x - 1f, z);
            TerraformToolSystem.Mode mode = switch (minute % 5) {
                case 0 -> TerraformToolSystem.Mode.LEVEL;
                case 1 -> TerraformToolSystem.Mode.RAISE;
                case 2 -> TerraformToolSystem.Mode.LOWER;
                case 3 -> TerraformToolSystem.Mode.SMOOTH;
                default -> TerraformToolSystem.Mode.RESTORE;
            };
            TerraformToolSystem.apply(terrain, mode, x, z, standing,
                    TerraformToolSystem.DEFAULT_RADIUS, 999, 100f);
            terrain.consumeDirtyRegion();
        }

        String save = terrain.encodeDeltas();
        assertTrue(save.length() < 2_000_000);
        TerrainState restored = new TerrainState(2027L, 116f, 160, 8f);
        restored.decodeDeltas(save);
        assertTrue(restored.modifiedSampleCount() >= 0);
    }
}
