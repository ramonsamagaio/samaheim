package com.samaheim.world;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MarchingTetraMesherTest {
    @Test
    void excavatedSubterraneanChunkProducesVisibleInteriorSurface() {
        VolumetricTerrain terrain = new VolumetricTerrain(4343L, 24f, -18f, 18f, 1f, 8);
        float surface = terrain.surfaceHeight(0f, 0f);
        Vector3f center = new Vector3f(0f, surface - 6f, 0f);
        VolumetricTerrain.EditResult result = terrain.dig(center, 3.4f, 8f);
        assertTrue(result.changedSamples() > 0);

        VolumetricTerrain.ChunkKey key = result.dirtyChunks().stream()
                .filter(candidate -> candidate.y() <= 1)
                .findFirst()
                .orElse(result.dirtyChunks().iterator().next());
        Mesh mesh = MarchingTetraMesher.buildChunk(terrain, key);

        assertNotNull(mesh);
        assertTrue(mesh.getTriangleCount() > 0);
    }
}
