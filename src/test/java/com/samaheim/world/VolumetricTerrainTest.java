package com.samaheim.world;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class VolumetricTerrainTest {
    private static final float EPS = 0.08f;

    @Test
    void diggingCreatesAirBelowTheOriginalSurface() {
        VolumetricTerrain terrain = terrain();
        float surface = terrain.surfaceHeight(0f, 0f);
        Vector3f center = new Vector3f(0f, surface - 2.2f, 0f);
        assertTrue(terrain.isSolid(center.x, center.y, center.z));

        VolumetricTerrain.EditResult result = terrain.dig(center, 3.2f, 6f);

        assertTrue(result.changedSamples() > 0);
        assertFalse(terrain.isSolid(center.x, center.y, center.z));
        assertTrue(terrain.isSolid(center.x, center.y - 4.2f, center.z));
    }

    @Test
    void addEarthCanRefillAExcavatedVolume() {
        VolumetricTerrain terrain = terrain();
        float surface = terrain.surfaceHeight(1f, -2f);
        Vector3f center = new Vector3f(1f, surface - 1.8f, -2f);
        terrain.dig(center, 2.8f, 7f);
        assertFalse(terrain.isSolid(center.x, center.y, center.z));

        terrain.add(center, 2.8f, 8f);

        assertTrue(terrain.isSolid(center.x, center.y, center.z));
    }

    @Test
    void editsRoundTripWithoutFlatteningCavesIntoAHeightfield() {
        VolumetricTerrain edited = terrain();
        float surface = edited.surfaceHeight(0f, 0f);
        edited.dig(new Vector3f(0f, surface - 2f, 0f), 3f, 6f);
        edited.dig(new Vector3f(2.5f, surface - 2.2f, 0f), 2.6f, 5f);
        edited.add(new Vector3f(-5f, surface + 0.5f, 1f), 1.8f, 3.5f);
        String encoded = edited.encodeEdits();

        VolumetricTerrain restored = terrain();
        restored.decodeEdits(encoded);

        assertEquals(edited.sampleDensity(0f, surface - 2f, 0f), restored.sampleDensity(0f, surface - 2f, 0f), EPS);
        assertEquals(edited.sampleDensity(2.5f, surface - 2.2f, 0f), restored.sampleDensity(2.5f, surface - 2.2f, 0f), EPS);
        assertEquals(3, restored.edits().size());
    }

    @Test
    void raycastTargetsTheActualDensitySurface() {
        VolumetricTerrain terrain = terrain();
        float surface = terrain.surfaceHeight(0f, 0f);
        Vector3f origin = new Vector3f(0f, surface + 5f, 0f);

        VolumetricTerrain.Hit hit = terrain.raycast(origin, new Vector3f(0f, -1f, 0f), 10f);

        assertNotNull(hit);
        assertEquals(surface, hit.point().y, 0.2f);
        assertTrue(hit.normal().y > 0.7f);
    }

    @Test
    void dirtyChunksIncludeSubterraneanChunksTouchedByTunnel() {
        VolumetricTerrain terrain = terrain();
        float surface = terrain.surfaceHeight(0f, 0f);
        VolumetricTerrain.EditResult result = terrain.dig(new Vector3f(0f, surface - 7f, 0f), 3.5f, 8f);

        assertFalse(result.dirtyChunks().isEmpty());
        assertTrue(result.dirtyChunks().stream().anyMatch(key -> key.y() <= 1));
    }

    private static VolumetricTerrain terrain() {
        return new VolumetricTerrain(8128L, 24f, -18f, 18f, 1f, 8);
    }
}
