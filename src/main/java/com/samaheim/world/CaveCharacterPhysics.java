package com.samaheim.world;

/**
 * Small deterministic capsule controller used by the cave runtime.
 * Terrain collision comes from the 3D density field, so tunnels and ceilings are real collision surfaces.
 */
public final class CaveCharacterPhysics {
    private CaveCharacterPhysics() { }

    public static HorizontalMove moveHorizontal(VolumetricTerrain terrain, float x, float footY, float z,
                                                float dx, float dz, float radius, float height, float maxStepUp) {
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(distance / 0.18f));
        float sx = dx / steps, sz = dz / steps;
        float px = x, pz = z, py = footY;
        boolean blocked = false;
        for (int i = 0; i < steps; i++) {
            float nx = px + sx, nz = pz + sz;
            if (terrain.capsuleClear(nx, py, nz, radius, height)) {
                px = nx; pz = nz;
                continue;
            }
            boolean stepped = false;
            for (float up = 0.1f; up <= maxStepUp + 0.001f; up += 0.1f) {
                if (terrain.capsuleClear(nx, py + up, nz, radius, height)) {
                    px = nx; pz = nz; py += up; stepped = true; break;
                }
            }
            if (stepped) continue;
            boolean moved = false;
            if (terrain.capsuleClear(nx, py, pz, radius, height)) { px = nx; moved = true; }
            if (terrain.capsuleClear(px, py, nz, radius, height)) { pz = nz; moved = true; }
            if (!moved) blocked = true;
        }
        return new HorizontalMove(px, py, pz, blocked);
    }

    public static VerticalMove moveVertical(VolumetricTerrain terrain, float x, float footY, float z,
                                            float deltaY, float radius, float height) {
        if (Math.abs(deltaY) < 0.00001f) return new VerticalMove(footY, false, false);
        int steps = Math.max(1, (int) Math.ceil(Math.abs(deltaY) / 0.08f));
        float sy = deltaY / steps;
        float y = footY;
        for (int i = 0; i < steps; i++) {
            float next = y + sy;
            if (terrain.capsuleClear(x, next, z, radius, height)) {
                y = next;
            } else {
                return new VerticalMove(y, sy < 0f, sy > 0f);
            }
        }
        return new VerticalMove(y, false, false);
    }

    public static boolean grounded(VolumetricTerrain terrain, float x, float footY, float z, float radius) {
        float y = footY - 0.07f;
        if (terrain.sampleDensity(x, y, z) > VolumetricTerrain.ISO) return true;
        for (int i = 0; i < 8; i++) {
            float a = i * ((float) Math.PI * 2f / 8f);
            float sx = x + (float) Math.cos(a) * radius * 0.8f;
            float sz = z + (float) Math.sin(a) * radius * 0.8f;
            if (terrain.sampleDensity(sx, y, sz) > VolumetricTerrain.ISO) return true;
        }
        return false;
    }

    public record HorizontalMove(float x, float footY, float z, boolean blocked) { }
    public record VerticalMove(float footY, boolean landed, boolean hitCeiling) { }
}
