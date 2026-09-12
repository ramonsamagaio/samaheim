package com.samaheim.world;

import com.jme3.math.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Smooth scalar-density terrain. Positive density is solid earth; negative is air.
 * Unlike a heightfield this representation can contain tunnels, ceilings and overhangs.
 */
public final class VolumetricTerrain {
    public static final float DEFAULT_SPACING = 1.0f;
    public static final float ISO = 0f;

    private final long seed;
    private final float halfExtent;
    private final float minY;
    private final float maxY;
    private final float spacing;
    private final int nx;
    private final int ny;
    private final int nz;
    private final float[] density;
    private final List<Edit> edits = new ArrayList<>();
    private final Set<ChunkKey> dirtyChunks = new HashSet<>();
    private final int chunkCells;

    public VolumetricTerrain(long seed, float halfExtent, float minY, float maxY, float spacing, int chunkCells) {
        if (halfExtent <= 8f || minY >= maxY || spacing <= 0.2f || chunkCells < 4) {
            throw new IllegalArgumentException("Invalid volumetric terrain dimensions");
        }
        this.seed = seed;
        this.halfExtent = halfExtent;
        this.minY = minY;
        this.maxY = maxY;
        this.spacing = spacing;
        this.chunkCells = chunkCells;
        this.nx = (int) Math.ceil((halfExtent * 2f) / spacing) + 1;
        this.nz = nx;
        this.ny = (int) Math.ceil((maxY - minY) / spacing) + 1;
        this.density = new float[nx * ny * nz];
        buildBaseDensity();
    }

    private void buildBaseDensity() {
        for (int z = 0; z < nz; z++) {
            float wz = worldZ(z);
            for (int x = 0; x < nx; x++) {
                float wx = worldX(x);
                float surface = WorldMath.height(seed, wx, wz);
                FrontierCaveMath.ColumnProfile caveProfile = FrontierCaveMath.columnProfile(seed, wx, wz);
                for (int y = 0; y < ny; y++) {
                    float wy = worldY(y);
                    float baseDensity = surface - wy;
                    if (!caveProfile.enabled()) {
                        density[index(x, y, z)] = baseDensity;
                        continue;
                    }
                    float caveDensity = FrontierCaveMath.caveDensity(caveProfile, surface - wy);
                    density[index(x, y, z)] = caveDensity < 7.99f ? Math.min(baseDensity, caveDensity) : baseDensity;
                }
            }
        }
    }

    public long seed() { return seed; }
    public float halfExtent() { return halfExtent; }
    public float minY() { return minY; }
    public float maxY() { return maxY; }
    public float spacing() { return spacing; }
    public int nx() { return nx; }
    public int ny() { return ny; }
    public int nz() { return nz; }
    public int chunkCells() { return chunkCells; }
    public int chunkCountX() { return (nx - 1 + chunkCells - 1) / chunkCells; }
    public int chunkCountY() { return (ny - 1 + chunkCells - 1) / chunkCells; }
    public int chunkCountZ() { return (nz - 1 + chunkCells - 1) / chunkCells; }

    public float worldX(int x) { return -halfExtent + x * spacing; }
    public float worldY(int y) { return minY + y * spacing; }
    public float worldZ(int z) { return -halfExtent + z * spacing; }

    public float gridDensity(int x, int y, int z) {
        x = clamp(x, 0, nx - 1); y = clamp(y, 0, ny - 1); z = clamp(z, 0, nz - 1);
        return density[index(x, y, z)];
    }

    public float sampleDensity(float x, float y, float z) {
        if (x <= -halfExtent || x >= halfExtent || z <= -halfExtent || z >= halfExtent || y <= minY) return 4f;
        if (y >= maxY) return -4f;
        float gx = (x + halfExtent) / spacing;
        float gy = (y - minY) / spacing;
        float gz = (z + halfExtent) / spacing;
        int x0 = clamp((int) Math.floor(gx), 0, nx - 2), y0 = clamp((int) Math.floor(gy), 0, ny - 2), z0 = clamp((int) Math.floor(gz), 0, nz - 2);
        int x1 = x0 + 1, y1 = y0 + 1, z1 = z0 + 1;
        float tx = clamp01(gx - x0), ty = clamp01(gy - y0), tz = clamp01(gz - z0);
        float c000 = gridDensity(x0, y0, z0), c100 = gridDensity(x1, y0, z0);
        float c010 = gridDensity(x0, y1, z0), c110 = gridDensity(x1, y1, z0);
        float c001 = gridDensity(x0, y0, z1), c101 = gridDensity(x1, y0, z1);
        float c011 = gridDensity(x0, y1, z1), c111 = gridDensity(x1, y1, z1);
        float c00 = lerp(c000, c100, tx), c10 = lerp(c010, c110, tx);
        float c01 = lerp(c001, c101, tx), c11 = lerp(c011, c111, tx);
        return lerp(lerp(c00, c10, ty), lerp(c01, c11, ty), tz);
    }

    public boolean isSolid(float x, float y, float z) { return sampleDensity(x, y, z) > ISO; }

    public Vector3f surfaceNormal(float x, float y, float z) {
        float e = spacing * 0.55f;
        float dx = sampleDensity(x + e, y, z) - sampleDensity(x - e, y, z);
        float dy = sampleDensity(x, y + e, z) - sampleDensity(x, y - e, z);
        float dz = sampleDensity(x, y, z + e) - sampleDensity(x, y, z - e);
        Vector3f outward = new Vector3f(-dx, -dy, -dz);
        if (outward.lengthSquared() < 0.00001f) return Vector3f.UNIT_Y.clone();
        return outward.normalizeLocal();
    }

    public Hit raycast(Vector3f origin, Vector3f direction, float maxDistance) {
        Vector3f dir = direction.clone();
        if (dir.lengthSquared() < 0.00001f) return null;
        dir.normalizeLocal();
        float step = Math.max(0.12f, spacing * 0.22f);
        float previousDistance = 0f;
        float previousDensity = sampleDensity(origin.x, origin.y, origin.z);
        for (float distance = step; distance <= maxDistance; distance += step) {
            Vector3f p = origin.add(dir.mult(distance));
            float d = sampleDensity(p.x, p.y, p.z);
            if (previousDensity <= ISO && d > ISO) {
                float lo = previousDistance, hi = distance;
                for (int i = 0; i < 8; i++) {
                    float mid = (lo + hi) * 0.5f;
                    Vector3f mp = origin.add(dir.mult(mid));
                    if (sampleDensity(mp.x, mp.y, mp.z) > ISO) hi = mid; else lo = mid;
                }
                float hitDistance = (lo + hi) * 0.5f;
                Vector3f hit = origin.add(dir.mult(hitDistance));
                return new Hit(hit, surfaceNormal(hit.x, hit.y, hit.z), hitDistance);
            }
            previousDensity = d;
            previousDistance = distance;
        }
        return null;
    }

    public EditResult dig(Vector3f center, float radius, float strength) {
        return applyEdit(new Edit(EditMode.DIG, center.x, center.y, center.z, radius, Math.abs(strength)), true);
    }

    public EditResult add(Vector3f center, float radius, float strength) {
        return applyEdit(new Edit(EditMode.ADD, center.x, center.y, center.z, radius, Math.abs(strength)), true);
    }

    public EditResult smooth(Vector3f center, float radius, float strength) {
        if (radius <= 0.25f || strength <= 0f) return new EditResult(0, Set.of());
        int minX = gridX(center.x - radius), maxX = gridX(center.x + radius);
        int minY = gridY(center.y - radius), maxY = gridY(center.y + radius);
        int minZ = gridZ(center.z - radius), maxZ = gridZ(center.z + radius);
        List<Integer> touched = new ArrayList<>();
        List<Float> values = new ArrayList<>();
        for (int z = minZ; z <= maxZ; z++) for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            float wx = worldX(x), wy = worldY(y), wz = worldZ(z);
            float distance = distance(wx, wy, wz, center.x, center.y, center.z);
            if (distance > radius) continue;
            float sum = 0f; int count = 0;
            for (int dz = -1; dz <= 1; dz++) for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                if (dx == 0 && dy == 0 && dz == 0) continue;
                sum += gridDensity(x + dx, y + dy, z + dz); count++;
            }
            float avg = sum / count;
            float falloff = smoothFalloff(distance / radius);
            int idx = index(x, y, z);
            touched.add(idx); values.add(lerp(density[idx], avg, clamp01(strength * falloff)));
        }
        for (int i = 0; i < touched.size(); i++) density[touched.get(i)] = values.get(i);
        markDirty(center.x, center.y, center.z, radius + spacing * 2f);
        if (!touched.isEmpty()) edits.add(new Edit(EditMode.SMOOTH, center.x, center.y, center.z, radius, strength));
        return new EditResult(touched.size(), consumeDirtyChunks());
    }

    private EditResult applyEdit(Edit edit, boolean record) {
        if (edit.radius <= 0.25f || edit.strength <= 0f) return new EditResult(0, Set.of());
        int minX = gridX(edit.x - edit.radius), maxX = gridX(edit.x + edit.radius);
        int minY = gridY(edit.y - edit.radius), maxY = gridY(edit.y + edit.radius);
        int minZ = gridZ(edit.z - edit.radius), maxZ = gridZ(edit.z + edit.radius);
        int changed = 0;
        float sign = edit.mode == EditMode.ADD ? 1f : -1f;
        for (int z = minZ; z <= maxZ; z++) for (int y = minY; y <= maxY; y++) for (int x = minX; x <= maxX; x++) {
            float distance = distance(worldX(x), worldY(y), worldZ(z), edit.x, edit.y, edit.z);
            if (distance > edit.radius) continue;
            float falloff = smoothFalloff(distance / edit.radius);
            int idx = index(x, y, z);
            float before = density[idx];
            float after = clamp(before + sign * edit.strength * falloff, -8f, 8f);
            if (Math.abs(after - before) > 0.0001f) { density[idx] = after; changed++; }
        }
        if (changed > 0) {
            markDirty(edit.x, edit.y, edit.z, edit.radius + spacing * 2f);
            if (record) edits.add(edit);
        }
        return new EditResult(changed, consumeDirtyChunks());
    }

    public boolean capsuleClear(float x, float footY, float z, float radius, float height) {
        if (x < -halfExtent + radius || x > halfExtent - radius || z < -halfExtent + radius || z > halfExtent - radius) return false;
        int verticalSamples = Math.max(4, (int) Math.ceil(height / 0.45f));
        for (int i = 0; i <= verticalSamples; i++) {
            float y = footY + 0.08f + height * i / verticalSamples;
            if (sampleDensity(x, y, z) > 0.12f) return false;
            for (int ring = 0; ring < 8; ring++) {
                float a = ring * ((float) Math.PI * 2f / 8f);
                float sx = x + (float) Math.cos(a) * radius;
                float sz = z + (float) Math.sin(a) * radius;
                if (sampleDensity(sx, y, sz) > 0.12f) return false;
            }
        }
        return true;
    }

    public float findFloor(float x, float z, float fromY, float maxDrop) {
        float step = Math.max(0.08f, spacing * 0.12f);
        float top = Math.min(fromY, maxY - step);
        float previousY = top;
        float previous = sampleDensity(x, previousY, z);
        for (float y = top - step; y >= Math.max(minY, top - maxDrop); y -= step) {
            float current = sampleDensity(x, y, z);
            if (previous <= ISO && current > ISO) {
                float lo = y, hi = previousY;
                for (int i = 0; i < 7; i++) {
                    float mid = (lo + hi) * 0.5f;
                    if (sampleDensity(x, mid, z) > ISO) lo = mid; else hi = mid;
                }
                return (lo + hi) * 0.5f;
            }
            previous = current; previousY = y;
        }
        return Float.NaN;
    }

    public float surfaceHeight(float x, float z) {
        float y = Math.min(maxY - spacing, WorldMath.height(seed, x, z) + spacing * 4f);
        float floor = findFloor(x, z, y, maxY - minY);
        return Float.isNaN(floor) ? minY + 1f : floor;
    }

    public Set<ChunkKey> consumeDirtyChunks() {
        if (dirtyChunks.isEmpty()) return Set.of();
        Set<ChunkKey> out = Set.copyOf(dirtyChunks);
        dirtyChunks.clear();
        return out;
    }

    private void markDirty(float x, float y, float z, float radius) {
        int minX = clamp((gridX(x - radius)) / chunkCells, 0, chunkCountX() - 1);
        int maxX = clamp((gridX(x + radius)) / chunkCells, 0, chunkCountX() - 1);
        int minY = clamp((gridY(y - radius)) / chunkCells, 0, chunkCountY() - 1);
        int maxY = clamp((gridY(y + radius)) / chunkCells, 0, chunkCountY() - 1);
        int minZ = clamp((gridZ(z - radius)) / chunkCells, 0, chunkCountZ() - 1);
        int maxZ = clamp((gridZ(z + radius)) / chunkCells, 0, chunkCountZ() - 1);
        for (int cz = minZ; cz <= maxZ; cz++) for (int cy = minY; cy <= maxY; cy++) for (int cx = minX; cx <= maxX; cx++) dirtyChunks.add(new ChunkKey(cx, cy, cz));
    }

    public int[] initialChunkYRange(int cx, int cz) {
        int startX = cx * chunkCells, endX = Math.min(nx - 1, startX + chunkCells);
        int startZ = cz * chunkCells, endZ = Math.min(nz - 1, startZ + chunkCells);
        float minSurface = Float.POSITIVE_INFINITY, maxSurface = Float.NEGATIVE_INFINITY;
        for (int z = startZ; z <= endZ; z += Math.max(1, chunkCells / 4)) for (int x = startX; x <= endX; x += Math.max(1, chunkCells / 4)) {
            float surface = WorldMath.height(seed, worldX(x), worldZ(z));
            minSurface = Math.min(minSurface, surface); maxSurface = Math.max(maxSurface, surface);
        }
        float chunkMinX = worldX(startX);
        float chunkMaxX = worldX(endX);
        float chunkMinZ = worldZ(startZ);
        float chunkMaxZ = worldZ(endZ);
        float farX = Math.max(Math.abs(chunkMinX), Math.abs(chunkMaxX));
        float farZ = Math.max(Math.abs(chunkMinZ), Math.abs(chunkMaxZ));
        if (Math.max(farX, farZ) > FrontierCaveMath.CAVE_START_EXTENT) {
            minSurface -= FrontierCaveMath.MAX_CAVE_DEPTH + spacing * 2f;
        }
        for (Edit edit : edits) {
            float nearestX = clamp(edit.x, chunkMinX, chunkMaxX);
            float nearestZ = clamp(edit.z, chunkMinZ, chunkMaxZ);
            float dx = edit.x - nearestX;
            float dz = edit.z - nearestZ;
            if (dx * dx + dz * dz > edit.radius * edit.radius) continue;
            minSurface = Math.min(minSurface, edit.y - edit.radius - spacing);
            maxSurface = Math.max(maxSurface, edit.y + edit.radius + spacing);
        }
        int minChunk = clamp(gridY(minSurface - spacing * 2f) / chunkCells, 0, chunkCountY() - 1);
        int maxChunk = clamp(gridY(maxSurface + spacing * 2f) / chunkCells, 0, chunkCountY() - 1);
        return new int[]{minChunk, maxChunk};
    }

    public String encodeEdits() {
        StringBuilder out = new StringBuilder();
        for (Edit edit : edits) {
            if (out.length() > 0) out.append('|');
            out.append(edit.mode.name()).append(',').append(fmt(edit.x)).append(',').append(fmt(edit.y)).append(',').append(fmt(edit.z))
                    .append(',').append(fmt(edit.radius)).append(',').append(fmt(edit.strength));
        }
        return out.toString();
    }

    public void decodeEdits(String encoded) {
        if (encoded == null || encoded.isBlank()) return;
        for (String item : encoded.split("\\|")) {
            String[] p = item.split(",");
            if (p.length != 6) continue;
            try {
                Edit edit = new Edit(EditMode.valueOf(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]), Float.parseFloat(p[4]), Float.parseFloat(p[5]));
                if (!Float.isFinite(edit.x) || !Float.isFinite(edit.y) || !Float.isFinite(edit.z) || edit.radius <= 0f || edit.radius > 12f) continue;
                if (edit.mode == EditMode.SMOOTH) {
                    smooth(new Vector3f(edit.x, edit.y, edit.z), edit.radius, edit.strength);
                    if (!edits.isEmpty()) edits.remove(edits.size() - 1);
                    edits.add(edit);
                } else {
                    applyEdit(edit, false); edits.add(edit);
                }
            } catch (IllegalArgumentException ignored) {
                // Skip one damaged edit without rejecting the world.
            }
        }
        dirtyChunks.clear();
    }

    public List<Edit> edits() { return Collections.unmodifiableList(edits); }

    private int gridX(float x) { return clamp(Math.round((x + halfExtent) / spacing), 0, nx - 1); }
    private int gridY(float y) { return clamp(Math.round((y - minY) / spacing), 0, ny - 1); }
    private int gridZ(float z) { return clamp(Math.round((z + halfExtent) / spacing), 0, nz - 1); }
    private int index(int x, int y, int z) { return (z * ny + y) * nx + x; }
    private static int clamp(int value, int min, int max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp(float value, float min, float max) { return Math.max(min, Math.min(max, value)); }
    private static float clamp01(float value) { return Math.max(0f, Math.min(1f, value)); }
    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
    private static float distance(float ax, float ay, float az, float bx, float by, float bz) { float dx = ax - bx, dy = ay - by, dz = az - bz; return (float) Math.sqrt(dx * dx + dy * dy + dz * dz); }
    private static float smoothFalloff(float normalizedDistance) { float t = clamp01(normalizedDistance); float smooth = t * t * (3f - 2f * t); return 1f - smooth; }
    private static String fmt(float value) { return String.format(Locale.ROOT, "%.3f", value); }

    public enum EditMode { DIG, ADD, SMOOTH }
    public record Hit(Vector3f point, Vector3f normal, float distance) { }
    public record ChunkKey(int x, int y, int z) { }
    public record Edit(EditMode mode, float x, float y, float z, float radius, float strength) { }
    public record EditResult(int changedSamples, Set<ChunkKey> dirtyChunks) { }
}
