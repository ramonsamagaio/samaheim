package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class TerrainStreamingTest {
    @Test
    void desiredChunksStayInsideWorldAndNearPlayer() {
        VolumetricTerrain terrain = new VolumetricTerrain(42L, 96f, -24f, 28f, 1f, 16);
        Set<VolumetricTerrain.ChunkKey> chunks = TerrainStreaming.desiredChunks(terrain, 0f, 0f, 4);

        assertFalse(chunks.isEmpty());
        int centerX = TerrainStreaming.chunkX(terrain, 0f);
        int centerZ = TerrainStreaming.chunkZ(terrain, 0f);
        assertTrue(chunks.stream().allMatch(key -> key.x() >= 0 && key.x() < terrain.chunkCountX()));
        assertTrue(chunks.stream().allMatch(key -> key.z() >= 0 && key.z() < terrain.chunkCountZ()));
        assertTrue(chunks.stream().allMatch(key -> Math.abs(key.x() - centerX) <= 4));
        assertTrue(chunks.stream().allMatch(key -> Math.abs(key.z() - centerZ) <= 4));
    }

    @Test
    void streamingWindowMovesWhenPlayerCrossesChunkColumns() {
        VolumetricTerrain terrain = new VolumetricTerrain(73L, 192f, -24f, 28f, 1f, 16);
        Set<VolumetricTerrain.ChunkKey> origin = TerrainStreaming.desiredChunks(terrain, 0f, 0f, 4);
        Set<VolumetricTerrain.ChunkKey> east = TerrainStreaming.desiredChunks(terrain, 80f, 0f, 4);

        assertNotEquals(origin, east);
        int eastColumn = TerrainStreaming.chunkX(terrain, 80f);
        assertTrue(east.stream().anyMatch(key -> key.x() == eastColumn));
    }

    @Test
    void worldEdgeClampsStreamingInsteadOfRequestingInvalidChunks() {
        VolumetricTerrain terrain = new VolumetricTerrain(99L, 96f, -24f, 28f, 1f, 16);
        Set<VolumetricTerrain.ChunkKey> edge = TerrainStreaming.desiredChunks(terrain, 95f, 95f, 6);

        assertFalse(edge.isEmpty());
        assertTrue(edge.stream().allMatch(key -> key.x() >= 0 && key.x() < terrain.chunkCountX()));
        assertTrue(edge.stream().allMatch(key -> key.z() >= 0 && key.z() < terrain.chunkCountZ()));
    }
}
