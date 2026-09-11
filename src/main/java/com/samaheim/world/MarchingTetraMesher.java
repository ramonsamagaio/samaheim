package com.samaheim.world;

import com.jme3.math.Vector3f;
import com.jme3.scene.Mesh;
import com.jme3.scene.VertexBuffer;
import com.jme3.util.BufferUtils;

import java.util.ArrayList;
import java.util.List;

/** Smooth isosurface extraction using marching tetrahedra. */
public final class MarchingTetraMesher {
    private static final int[][] CORNERS = {
            {0,0,0},{1,0,0},{1,1,0},{0,1,0},
            {0,0,1},{1,0,1},{1,1,1},{0,1,1}
    };
    private static final int[][] TETS = {
            {0,5,1,6},{0,1,2,6},{0,2,3,6},
            {0,3,7,6},{0,7,4,6},{0,4,5,6}
    };

    private MarchingTetraMesher() { }

    public static Mesh buildChunk(VolumetricTerrain terrain, VolumetricTerrain.ChunkKey key) {
        int chunk = terrain.chunkCells();
        int startX = key.x() * chunk, startY = key.y() * chunk, startZ = key.z() * chunk;
        int endX = Math.min(terrain.nx() - 1, startX + chunk);
        int endY = Math.min(terrain.ny() - 1, startY + chunk);
        int endZ = Math.min(terrain.nz() - 1, startZ + chunk);

        List<Float> positions = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        Vector3f[] p = new Vector3f[8];
        float[] d = new float[8];
        for (int z = startZ; z < endZ; z++) {
            for (int y = startY; y < endY; y++) {
                for (int x = startX; x < endX; x++) {
                    boolean anySolid = false, anyAir = false;
                    for (int c = 0; c < 8; c++) {
                        int gx = x + CORNERS[c][0], gy = y + CORNERS[c][1], gz = z + CORNERS[c][2];
                        p[c] = new Vector3f(terrain.worldX(gx), terrain.worldY(gy), terrain.worldZ(gz));
                        d[c] = terrain.gridDensity(gx, gy, gz);
                        if (d[c] > VolumetricTerrain.ISO) anySolid = true; else anyAir = true;
                    }
                    if (!anySolid || !anyAir) continue;
                    for (int[] tet : TETS) polygonizeTet(terrain, p, d, tet, positions, normals, indices);
                }
            }
        }

        if (indices.isEmpty()) return null;
        float[] pos = new float[positions.size()];
        float[] nor = new float[normals.size()];
        int[] idx = new int[indices.size()];
        for (int i = 0; i < pos.length; i++) pos[i] = positions.get(i);
        for (int i = 0; i < nor.length; i++) nor[i] = normals.get(i);
        for (int i = 0; i < idx.length; i++) idx[i] = indices.get(i);

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(nor));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(idx));
        mesh.updateBound();
        mesh.setStatic();
        return mesh;
    }

    private static void polygonizeTet(VolumetricTerrain terrain, Vector3f[] cubeP, float[] cubeD, int[] tet,
                                      List<Float> positions, List<Float> normals, List<Integer> indices) {
        int[] inside = new int[4];
        int[] outside = new int[4];
        int inCount = 0, outCount = 0;
        for (int local = 0; local < 4; local++) {
            int corner = tet[local];
            if (cubeD[corner] > VolumetricTerrain.ISO) inside[inCount++] = corner;
            else outside[outCount++] = corner;
        }
        if (inCount == 0 || inCount == 4) return;

        if (inCount == 1) {
            Vector3f a = interpolate(cubeP[inside[0]], cubeP[outside[0]], cubeD[inside[0]], cubeD[outside[0]]);
            Vector3f b = interpolate(cubeP[inside[0]], cubeP[outside[1]], cubeD[inside[0]], cubeD[outside[1]]);
            Vector3f c = interpolate(cubeP[inside[0]], cubeP[outside[2]], cubeD[inside[0]], cubeD[outside[2]]);
            addTriangle(terrain, a, c, b, positions, normals, indices);
        } else if (inCount == 3) {
            Vector3f a = interpolate(cubeP[outside[0]], cubeP[inside[0]], cubeD[outside[0]], cubeD[inside[0]]);
            Vector3f b = interpolate(cubeP[outside[0]], cubeP[inside[1]], cubeD[outside[0]], cubeD[inside[1]]);
            Vector3f c = interpolate(cubeP[outside[0]], cubeP[inside[2]], cubeD[outside[0]], cubeD[inside[2]]);
            addTriangle(terrain, a, b, c, positions, normals, indices);
        } else {
            Vector3f a = interpolate(cubeP[inside[0]], cubeP[outside[0]], cubeD[inside[0]], cubeD[outside[0]]);
            Vector3f b = interpolate(cubeP[inside[0]], cubeP[outside[1]], cubeD[inside[0]], cubeD[outside[1]]);
            Vector3f c = interpolate(cubeP[inside[1]], cubeP[outside[0]], cubeD[inside[1]], cubeD[outside[0]]);
            Vector3f d = interpolate(cubeP[inside[1]], cubeP[outside[1]], cubeD[inside[1]], cubeD[outside[1]]);
            addTriangle(terrain, a, c, b, positions, normals, indices);
            addTriangle(terrain, b, c, d, positions, normals, indices);
        }
    }

    private static Vector3f interpolate(Vector3f a, Vector3f b, float da, float db) {
        float denom = da - db;
        float t = Math.abs(denom) < 0.00001f ? 0.5f : da / denom;
        t = Math.max(0f, Math.min(1f, t));
        return a.add(b.subtract(a).mult(t));
    }

    private static void addTriangle(VolumetricTerrain terrain, Vector3f a, Vector3f b, Vector3f c,
                                    List<Float> positions, List<Float> normals, List<Integer> indices) {
        Vector3f face = b.subtract(a).cross(c.subtract(a));
        if (face.lengthSquared() < 0.000001f) return;
        Vector3f centroid = a.add(b).addLocal(c).multLocal(1f / 3f);
        Vector3f expected = terrain.surfaceNormal(centroid.x, centroid.y, centroid.z);
        if (face.dot(expected) < 0f) {
            Vector3f temp = b; b = c; c = temp;
        }
        addVertex(terrain, a, positions, normals, indices);
        addVertex(terrain, b, positions, normals, indices);
        addVertex(terrain, c, positions, normals, indices);
    }

    private static void addVertex(VolumetricTerrain terrain, Vector3f p, List<Float> positions,
                                  List<Float> normals, List<Integer> indices) {
        int index = positions.size() / 3;
        positions.add(p.x); positions.add(p.y); positions.add(p.z);
        Vector3f n = terrain.surfaceNormal(p.x, p.y, p.z);
        normals.add(n.x); normals.add(n.y); normals.add(n.z);
        indices.add(index);
    }
}
