package com.samaheim.world;

/**
 * Deterministic capsule controller used by the graphical cave runtime.
 * Terrain collision comes directly from the 3D density field, so tunnels,
 * ceilings, ledges and player-made excavations are physical surfaces.
 */
public final class CaveCharacterPhysics {
    private static final float HORIZONTAL_SUBSTEP = 0.14f;
    private static final float VERTICAL_SUBSTEP = 0.06f;
    private static final float FOOT_OFFSET = 0.04f;

    private CaveCharacterPhysics() { }

    public static HorizontalMove moveHorizontal(VolumetricTerrain terrain, float x, float footY, float z,
                                                float dx, float dz, float radius, float height, float maxStepUp) {
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(distance / HORIZONTAL_SUBSTEP));
        float sx = dx / steps;
        float sz = dz / steps;
        float px = x;
        float pz = z;
        float py = footY;
        boolean blocked = false;
        boolean stickToGround = grounded(terrain, x, footY, z, radius);

        for (int i = 0; i < steps; i++) {
            float nx = px + sx;
            float nz = pz + sz;
            if (terrain.capsuleClear(nx, py, nz, radius, height)) {
                px = nx;
                pz = nz;
                if (stickToGround) py = snapDown(terrain, px, py, pz, radius, height, maxStepUp);
                continue;
            }

            float steppedY = findStepUp(terrain, nx, py, nz, radius, height, maxStepUp);
            if (Float.isFinite(steppedY)) {
                px = nx;
                pz = nz;
                py = steppedY;
                continue;
            }

            boolean moved = false;
            if (terrain.capsuleClear(nx, py, pz, radius, height)) {
                px = nx;
                moved = true;
            }
            if (terrain.capsuleClear(px, py, nz, radius, height)) {
                pz = nz;
                moved = true;
            }
            if (moved) {
                if (stickToGround) py = snapDown(terrain, px, py, pz, radius, height, maxStepUp);
            } else {
                blocked = true;
            }
        }
        return new HorizontalMove(px, py, pz, blocked);
    }

    private static float findStepUp(VolumetricTerrain terrain, float x, float footY, float z,
                                    float radius, float height, float maxStepUp) {
        if (maxStepUp <= 0f) return Float.NaN;
        float increment = Math.min(0.10f, Math.max(0.05f, terrain.spacing() * 0.10f));
        for (float up = increment; up <= maxStepUp + 0.001f; up += increment) {
            float candidateY = footY + up;
            if (!terrain.capsuleClear(x, candidateY, z, radius, height)) continue;
            float floor = terrain.findFloor(x, z, candidateY + 0.16f, maxStepUp + 0.35f);
            if (!Float.isFinite(floor)) continue;
            float supportedY = floor + FOOT_OFFSET;
            if (supportedY < footY - 0.08f || supportedY > footY + maxStepUp + 0.08f) continue;
            if (terrain.capsuleClear(x, supportedY, z, radius, height)) return supportedY;
        }
        return Float.NaN;
    }

    private static float snapDown(VolumetricTerrain terrain, float x, float footY, float z,
                                  float radius, float height, float maxStep) {
        float snapDistance = Math.max(0.18f, Math.min(0.58f, maxStep + 0.10f));
        float floor = terrain.findFloor(x, z, footY + 0.12f, snapDistance + 0.12f);
        if (!Float.isFinite(floor)) return footY;
        float candidate = floor + FOOT_OFFSET;
        if (candidate > footY + 0.08f || footY - candidate > snapDistance) return footY;
        return terrain.capsuleClear(x, candidate, z, radius, height) ? candidate : footY;
    }

    public static VerticalMove moveVertical(VolumetricTerrain terrain, float x, float footY, float z,
                                            float deltaY, float radius, float height) {
        if (Math.abs(deltaY) < 0.00001f) return new VerticalMove(footY, false, false);
        int steps = Math.max(1, (int) Math.ceil(Math.abs(deltaY) / VERTICAL_SUBSTEP));
        float sy = deltaY / steps;
        float y = footY;
        for (int i = 0; i < steps; i++) {
            float next = y + sy;
            if (terrain.capsuleClear(x, next, z, radius, height)) {
                y = next;
            } else {
                if (sy < 0f) {
                    float floor = terrain.findFloor(x, z, y + 0.10f, Math.max(0.24f, Math.abs(sy) + 0.18f));
                    if (Float.isFinite(floor)) {
                        float landedY = floor + FOOT_OFFSET;
                        if (landedY <= y + 0.08f && terrain.capsuleClear(x, landedY, z, radius, height)) y = landedY;
                    }
                }
                return new VerticalMove(y, sy < 0f, sy > 0f);
            }
        }
        return new VerticalMove(y, false, false);
    }

    public static boolean grounded(VolumetricTerrain terrain, float x, float footY, float z, float radius) {
        float y = footY - 0.07f;
        if (terrain.sampleDensity(x, y, z) > VolumetricTerrain.ISO) return true;
        for (int i = 0; i < 12; i++) {
            float a = i * ((float) Math.PI * 2f / 12f);
            float sx = x + (float) Math.cos(a) * radius * 0.82f;
            float sz = z + (float) Math.sin(a) * radius * 0.82f;
            if (terrain.sampleDensity(sx, y, sz) > VolumetricTerrain.ISO) return true;
        }
        return false;
    }

    public record HorizontalMove(float x, float footY, float z, boolean blocked) { }
    public record VerticalMove(float footY, boolean landed, boolean hitCeiling) { }
}
