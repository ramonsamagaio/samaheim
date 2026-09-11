package com.samaheim.world;

import java.util.Locale;

public final class TerrainState {
    private static final float EPSILON = 0.0005f;

    private final float halfExtent;
    private final int cells;
    private final int width;
    private final float cellSize;
    private final float maxDelta;
    private final float[] baseHeights;
    private final float[] deltas;

    public TerrainState(long seed, float halfExtent, int cells, float maxDelta) {
        if (halfExtent <= 0f || cells < 2 || maxDelta <= 0f) {
            throw new IllegalArgumentException("Invalid terrain dimensions");
        }
        this.halfExtent = halfExtent;
        this.cells = cells;
        this.width = cells + 1;
        this.cellSize = (halfExtent * 2f) / cells;
        this.maxDelta = maxDelta;
        this.baseHeights = new float[width * width];
        this.deltas = new float[width * width];

        for (int z = 0; z < width; z++) {
            float worldZ = -halfExtent + z * cellSize;
            for (int x = 0; x < width; x++) {
                float worldX = -halfExtent + x * cellSize;
                baseHeights[index(x, z)] = WorldMath.height(seed, worldX, worldZ);
            }
        }
    }

    public int cells() { return cells; }
    public int width() { return width; }
    public float cellSize() { return cellSize; }
    public float halfExtent() { return halfExtent; }
    public float maxDelta() { return maxDelta; }
    public float vertexWorldX(int x) { return -halfExtent + x * cellSize; }
    public float vertexWorldZ(int z) { return -halfExtent + z * cellSize; }

    public float vertexHeight(int x, int z) {
        int i = index(clampIndex(x), clampIndex(z));
        return baseHeights[i] + deltas[i];
    }

    public float originalVertexHeight(int x, int z) {
        return baseHeights[index(clampIndex(x), clampIndex(z))];
    }

    public float sampleHeight(float worldX, float worldZ) {
        float gx = (clamp(worldX, -halfExtent, halfExtent) + halfExtent) / cellSize;
        float gz = (clamp(worldZ, -halfExtent, halfExtent) + halfExtent) / cellSize;
        int x0 = Math.min(cells - 1, Math.max(0, (int) Math.floor(gx)));
        int z0 = Math.min(cells - 1, Math.max(0, (int) Math.floor(gz)));
        int x1 = Math.min(cells, x0 + 1);
        int z1 = Math.min(cells, z0 + 1);
        float tx = clamp(gx - x0, 0f, 1f);
        float tz = clamp(gz - z0, 0f, 1f);
        float a = lerp(vertexHeight(x0, z0), vertexHeight(x1, z0), tx);
        float b = lerp(vertexHeight(x0, z1), vertexHeight(x1, z1), tx);
        return lerp(a, b, tz);
    }

    public int raise(float worldX, float worldZ, float radius, float amount) {
        return addBrush(worldX, worldZ, radius, Math.abs(amount));
    }

    public int lower(float worldX, float worldZ, float radius, float amount) {
        return addBrush(worldX, worldZ, radius, -Math.abs(amount));
    }

    public int level(float worldX, float worldZ, float radius, float targetHeight, float maxStep) {
        if (radius <= 0f || maxStep <= 0f) return 0;
        int changed = 0;
        int minX = gridMin(worldX - radius), maxX = gridMax(worldX + radius);
        int minZ = gridMin(worldZ - radius), maxZ = gridMax(worldZ + radius);
        for (int z = minZ; z <= maxZ; z++) {
            float wz = vertexWorldZ(z);
            for (int x = minX; x <= maxX; x++) {
                float wx = vertexWorldX(x);
                float distance = distance(wx, wz, worldX, worldZ);
                if (distance > radius) continue;
                float weight = brushWeight(distance, radius);
                int i = index(x, z);
                float current = baseHeights[i] + deltas[i];
                float wanted = clamp(targetHeight, baseHeights[i] - maxDelta, baseHeights[i] + maxDelta);
                float difference = wanted - current;
                float step = clamp(difference, -maxStep * weight, maxStep * weight);
                if (Math.abs(step) > EPSILON && setDelta(i, deltas[i] + step)) changed++;
            }
        }
        return changed;
    }

    public int restore(float worldX, float worldZ, float radius, float strength) {
        if (radius <= 0f || strength <= 0f) return 0;
        int changed = 0;
        int minX = gridMin(worldX - radius), maxX = gridMax(worldX + radius);
        int minZ = gridMin(worldZ - radius), maxZ = gridMax(worldZ + radius);
        for (int z = minZ; z <= maxZ; z++) {
            float wz = vertexWorldZ(z);
            for (int x = minX; x <= maxX; x++) {
                float wx = vertexWorldX(x);
                float distance = distance(wx, wz, worldX, worldZ);
                if (distance > radius) continue;
                int i = index(x, z);
                float next = moveToward(deltas[i], 0f, strength * brushWeight(distance, radius));
                if (Math.abs(next - deltas[i]) > EPSILON && setDelta(i, next)) changed++;
            }
        }
        return changed;
    }

    public String encodeDeltas() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < deltas.length; i++) {
            if (Math.abs(deltas[i]) <= EPSILON) continue;
            if (out.length() > 0) out.append(';');
            out.append(i).append(':').append(String.format(Locale.ROOT, "%.4f", deltas[i]));
        }
        return out.toString();
    }

    public void decodeDeltas(String encoded) {
        if (encoded == null || encoded.isBlank()) return;
        for (String entry : encoded.split(";")) {
            String[] pair = entry.split(":", 2);
            if (pair.length != 2) continue;
            try {
                int i = Integer.parseInt(pair[0]);
                float value = Float.parseFloat(pair[1]);
                if (i >= 0 && i < deltas.length && Float.isFinite(value)) {
                    deltas[i] = clamp(value, -maxDelta, maxDelta);
                }
            } catch (NumberFormatException ignored) {
                // Ignore one corrupt sample rather than rejecting the whole save.
            }
        }
    }

    private int addBrush(float worldX, float worldZ, float radius, float amount) {
        if (radius <= 0f || amount == 0f) return 0;
        int changed = 0;
        int minX = gridMin(worldX - radius), maxX = gridMax(worldX + radius);
        int minZ = gridMin(worldZ - radius), maxZ = gridMax(worldZ + radius);
        for (int z = minZ; z <= maxZ; z++) {
            float wz = vertexWorldZ(z);
            for (int x = minX; x <= maxX; x++) {
                float wx = vertexWorldX(x);
                float distance = distance(wx, wz, worldX, worldZ);
                if (distance > radius) continue;
                int i = index(x, z);
                if (setDelta(i, deltas[i] + amount * brushWeight(distance, radius))) changed++;
            }
        }
        return changed;
    }

    private boolean setDelta(int i, float next) {
        float clamped = clamp(next, -maxDelta, maxDelta);
        if (Math.abs(clamped - deltas[i]) <= EPSILON) return false;
        deltas[i] = clamped;
        return true;
    }

    private int gridMin(float world) { return clampIndex((int) Math.floor((world + halfExtent) / cellSize)); }
    private int gridMax(float world) { return clampIndex((int) Math.ceil((world + halfExtent) / cellSize)); }
    private int clampIndex(int value) { return Math.max(0, Math.min(cells, value)); }
    private int index(int x, int z) { return z * width + x; }

    private static float brushWeight(float distance, float radius) {
        float n = clamp(distance / radius, 0f, 1f);
        float smooth = n * n * (3f - 2f * n);
        return 1f - smooth;
    }

    private static float distance(float ax, float az, float bx, float bz) {
        float dx = ax - bx, dz = az - bz;
        return (float) Math.sqrt(dx * dx + dz * dz);
    }

    private static float moveToward(float value, float target, float amount) {
        return value < target ? Math.min(value + amount, target) : Math.max(value - amount, target);
    }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
}
