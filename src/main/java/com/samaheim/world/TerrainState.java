package com.samaheim.world;

import java.util.Locale;

/** Editable generated terrain with coarse sculpting plus a fine leveling layer. */
public final class TerrainState {
    private static final float EPSILON = 0.0005f;
    private static final float MAX_LEVEL_OFFSET = 1.0f;
    private static final float TARGET_SAMPLE_SPACING = 1.0f;

    private final float halfExtent;
    private final int cells;
    private final int width;
    private final float cellSize;
    private final float maxDelta;
    private final float[] baseHeights;
    private final float[] sculptDeltas;
    private final float[] levelOffsets;

    public TerrainState(long seed, float halfExtent, int minimumCells, float maxDelta) {
        if (halfExtent <= 0f || minimumCells < 2 || maxDelta <= 0f) {
            throw new IllegalArgumentException("Invalid terrain dimensions");
        }
        this.halfExtent = halfExtent;
        int oneMeterCells = (int) Math.ceil((halfExtent * 2f) / TARGET_SAMPLE_SPACING);
        this.cells = Math.max(minimumCells, oneMeterCells);
        this.width = cells + 1;
        this.cellSize = (halfExtent * 2f) / cells;
        this.maxDelta = maxDelta;
        this.baseHeights = new float[width * width];
        this.sculptDeltas = new float[width * width];
        this.levelOffsets = new float[width * width];

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
    public float maxLevelOffset() { return MAX_LEVEL_OFFSET; }
    public float vertexWorldX(int x) { return -halfExtent + x * cellSize; }
    public float vertexWorldZ(int z) { return -halfExtent + z * cellSize; }

    public float vertexHeight(int x, int z) {
        int i = index(clampIndex(x), clampIndex(z));
        return baseHeights[i] + sculptDeltas[i] + levelOffsets[i];
    }

    public float originalVertexHeight(int x, int z) {
        return baseHeights[index(clampIndex(x), clampIndex(z))];
    }

    public float sculptedVertexHeight(int x, int z) {
        int i = index(clampIndex(x), clampIndex(z));
        return baseHeights[i] + sculptDeltas[i];
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
        return sculptBrush(worldX, worldZ, radius, Math.abs(amount));
    }

    public int lower(float worldX, float worldZ, float radius, float amount) {
        return sculptBrush(worldX, worldZ, radius, -Math.abs(amount));
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
                float sculpted = baseHeights[i] + sculptDeltas[i];
                float wantedFinal = clamp(targetHeight, sculpted - MAX_LEVEL_OFFSET, sculpted + MAX_LEVEL_OFFSET);
                float wantedOffset = wantedFinal - sculpted;
                float difference = wantedOffset - levelOffsets[i];
                float step = clamp(difference, -maxStep * weight, maxStep * weight);
                if (Math.abs(step) > EPSILON && setLevelOffset(i, levelOffsets[i] + step)) changed++;
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
                float amount = strength * brushWeight(distance, radius);
                float nextSculpt = moveToward(sculptDeltas[i], 0f, amount);
                float nextLevel = moveToward(levelOffsets[i], 0f, amount);
                boolean sculptChanged = Math.abs(nextSculpt - sculptDeltas[i]) > EPSILON && setSculptDelta(i, nextSculpt);
                boolean levelChanged = Math.abs(nextLevel - levelOffsets[i]) > EPSILON && setLevelOffset(i, nextLevel);
                if (sculptChanged || levelChanged) changed++;
            }
        }
        return changed;
    }

    public String encodeDeltas() {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sculptDeltas.length; i++) {
            if (Math.abs(sculptDeltas[i]) > EPSILON) append(out, 's', i, sculptDeltas[i]);
            if (Math.abs(levelOffsets[i]) > EPSILON) append(out, 'l', i, levelOffsets[i]);
        }
        return out.toString();
    }

    public void decodeDeltas(String encoded) {
        if (encoded == null || encoded.isBlank()) return;
        for (String entry : encoded.split(";")) {
            String[] parts = entry.split(":");
            try {
                if (parts.length == 3) {
                    int i = Integer.parseInt(parts[1]);
                    float value = Float.parseFloat(parts[2]);
                    if (i < 0 || i >= sculptDeltas.length || !Float.isFinite(value)) continue;
                    if ("s".equals(parts[0])) sculptDeltas[i] = clamp(value, -maxDelta, maxDelta);
                    else if ("l".equals(parts[0])) levelOffsets[i] = clamp(value, -MAX_LEVEL_OFFSET, MAX_LEVEL_OFFSET);
                } else if (parts.length == 2) {
                    int i = Integer.parseInt(parts[0]);
                    float value = Float.parseFloat(parts[1]);
                    if (i >= 0 && i < sculptDeltas.length && Float.isFinite(value)) sculptDeltas[i] = clamp(value, -maxDelta, maxDelta);
                }
            } catch (NumberFormatException ignored) {
                // Ignore an individual damaged terrain sample instead of losing the save.
            }
        }
    }

    private int sculptBrush(float worldX, float worldZ, float radius, float amount) {
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
                float weight = brushWeight(distance, radius);
                if (setSculptDelta(i, sculptDeltas[i] + amount * weight)) {
                    // A coarse raise/dig establishes a new real surface at this sample.
                    levelOffsets[i] = moveToward(levelOffsets[i], 0f, Math.abs(amount) * weight);
                    changed++;
                }
            }
        }
        return changed;
    }

    private boolean setSculptDelta(int i, float next) {
        float clamped = clamp(next, -maxDelta, maxDelta);
        if (Math.abs(clamped - sculptDeltas[i]) <= EPSILON) return false;
        sculptDeltas[i] = clamped;
        return true;
    }

    private boolean setLevelOffset(int i, float next) {
        float clamped = clamp(next, -MAX_LEVEL_OFFSET, MAX_LEVEL_OFFSET);
        if (Math.abs(clamped - levelOffsets[i]) <= EPSILON) return false;
        levelOffsets[i] = clamped;
        return true;
    }

    private static void append(StringBuilder out, char layer, int index, float value) {
        if (out.length() > 0) out.append(';');
        out.append(layer).append(':').append(index).append(':').append(String.format(Locale.ROOT, "%.4f", value));
    }

    private int gridMin(float world) { return clampIndex((int) Math.floor((world + halfExtent) / cellSize)); }
    private int gridMax(float world) { return clampIndex((int) Math.ceil((world + halfExtent) / cellSize)); }
    private int clampIndex(int value) { return Math.max(0, Math.min(cells, value)); }
    private int index(int x, int z) { return z * width + x; }

    private static float brushWeight(float distance, float radius) {
        float n = clamp(distance / radius, 0f, 1f);
        if (n <= 0.35f) return 1f;
        float edge = (n - 0.35f) / 0.65f;
        float smooth = edge * edge * (3f - 2f * edge);
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
