package com.samaheim.world;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarchingTetraMesherTest {
    @Test
    void excavatedSubterraneanChunksProduceVisibleInteriorSurface() {
        VolumetricTerrain terrain = new VolumetricTerrain(4343L, 24f, -18f, 18f, 1f, 8);
        float surface = terrain.surfaceHeight(0f, 0f);
        Vector3f center = new Vector3f(0f, surface - 6f, 0f);
        VolumetricTerrain.EditResult result = terrain.dig(center, 3.4f, 8f);
        assertTrue(result.changedSamples() > 0);

        Mesh visibleInterior = null;
        for (VolumetricTerrain.ChunkKey key : result.dirtyChunks()) {
            Mesh candidate = MarchingTetraMesher.buildChunk(terrain, key);
            if (candidate != null && candidate.getTriangleCount() > 0) {
                visibleInterior = candidate;
                break;
            }
        }

        assertNotNull(visibleInterior);
        assertTrue(visibleInterior.getTriangleCount() > 0);
    }
}
