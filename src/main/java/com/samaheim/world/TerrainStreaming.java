package com.samaheim.world;

import java.util.HashSet;
import java.util.Set;

/** Computes the volumetric terrain chunks that should be meshed around the player. */
public final class TerrainStreaming {
    private TerrainStreaming() { }

    public static Set<VolumetricTerrain.ChunkKey> desiredChunks(VolumetricTerrain terrain,
                                                                 float worldX, float worldZ,
                                                                 int columnRadius) {
        if (terrain == null) throw new IllegalArgumentException("terrain is required");
        if (columnRadius < 0) throw new IllegalArgumentException("columnRadius must be >= 0");

        int centerX = chunkX(terrain, worldX);
        int centerZ = chunkZ(terrain, worldZ);
        Set<VolumetricTerrain.ChunkKey> desired = new HashSet<>();

        int minX = Math.max(0, centerX - columnRadius);
        int maxX = Math.min(terrain.chunkCountX() - 1, centerX + columnRadius);
        int minZ = Math.max(0, centerZ - columnRadius);
        int maxZ = Math.min(terrain.chunkCountZ() - 1, centerZ + columnRadius);

        for (int cz = minZ; cz <= maxZ; cz++) {
            for (int cx = minX; cx <= maxX; cx++) {
                int dx = cx - centerX;
                int dz = cz - centerZ;
                if (dx * dx + dz * dz > columnRadius * columnRadius + columnRadius) continue;
                int[] range = terrain.initialChunkYRange(cx, cz);
                for (int cy = range[0]; cy <= range[1]; cy++) {
                    desired.add(new VolumetricTerrain.ChunkKey(cx, cy, cz));
                }
            }
        }
        return Set.copyOf(desired);
    }

    public static int chunkX(VolumetricTerrain terrain, float worldX) {
        float chunkWorldSize = terrain.chunkCells() * terrain.spacing();
        int value = (int) Math.floor((worldX + terrain.halfExtent()) / chunkWorldSize);
        return Math.max(0, Math.min(terrain.chunkCountX() - 1, value));
    }

    public static int chunkZ(VolumetricTerrain terrain, float worldZ) {
        float chunkWorldSize = terrain.chunkCells() * terrain.spacing();
        int value = (int) Math.floor((worldZ + terrain.halfExtent()) / chunkWorldSize);
        return Math.max(0, Math.min(terrain.chunkCountZ() - 1, value));
    }
}
