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
    private static final float CLEARANCE_DENSITY = 0.18f;
    private static final float AXIS_EPSILON = 0.00001f;

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
            if (capsuleClear(terrain, nx, py, nz, radius, height)) {
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
            if (Math.abs(sx) > AXIS_EPSILON && capsuleClear(terrain, nx, py, pz, radius, height)) {
                px = nx;
                moved = true;
            }
            if (Math.abs(sz) > AXIS_EPSILON && capsuleClear(terrain, px, py, nz, radius, height)) {
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

    public static boolean capsuleClear(VolumetricTerrain terrain, float x, float footY, float z,
                                       float radius, float height) {
        if (radius <= 0f || height < radius * 2f) return false;
        if (x < -terrain.halfExtent() + radius || x > terrain.halfExtent() - radius
                || z < -terrain.halfExtent() + radius || z > terrain.halfExtent() - radius) return false;

        float bottomCenter = footY + radius;
        float topCenter = footY + height - radius;
        float startY = footY + 0.08f;
        float endY = footY + height + 0.04f;
        int verticalSamples = Math.max(4, (int) Math.ceil(height / 0.45f));

        for (int i = 0; i <= verticalSamples; i++) {
            float y = startY + (endY - startY) * i / verticalSamples;
            float ringRadius = radius;
            if (y < bottomCenter) {
                float dy = bottomCenter - y;
                ringRadius = (float) Math.sqrt(Math.max(0f, radius * radius - dy * dy));
            } else if (y > topCenter) {
                float dy = y - topCenter;
                ringRadius = (float) Math.sqrt(Math.max(0f, radius * radius - dy * dy));
            }

            if (terrain.sampleDensity(x, y, z) > CLEARANCE_DENSITY) return false;
            if (ringRadius < 0.035f) continue;
            for (int ring = 0; ring < 8; ring++) {
                float angle = ring * ((float) Math.PI * 2f / 8f);
                float sx = x + (float) Math.cos(angle) * ringRadius;
                float sz = z + (float) Math.sin(angle) * ringRadius;
                if (terrain.sampleDensity(sx, y, sz) > CLEARANCE_DENSITY) return false;
            }
        }
        return true;
    }

    private static float findStepUp(VolumetricTerrain terrain, float x, float footY, float z,
                                    float radius, float height, float maxStepUp) {
        if (maxStepUp <= 0f) return Float.NaN;
        float increment = Math.min(0.10f, Math.max(0.05f, terrain.spacing() * 0.10f));
        for (float up = increment; up <= maxStepUp + 0.001f; up += increment) {
            float candidateY = footY + up;
            if (!capsuleClear(terrain, x, candidateY, z, radius, height)) continue;
            float floor = terrain.findFloor(x, z, candidateY + 0.18f, maxStepUp + 0.40f);
            if (!Float.isFinite(floor)) continue;
            float supportedY = floor + FOOT_OFFSET;
            if (supportedY < footY - 0.08f || supportedY > footY + maxStepUp + 0.08f) continue;
            if (capsuleClear(terrain, x, supportedY, z, radius, height)) return supportedY;
        }
        return Float.NaN;
    }

    private static float snapDown(VolumetricTerrain terrain, float x, float footY, float z,
                                  float radius, float height, float maxStep) {
        float snapDistance = Math.max(0.18f, Math.min(0.58f, maxStep + 0.10f));
        float floor = terrain.findFloor(x, z, footY + 0.16f, snapDistance + 0.18f);
        if (!Float.isFinite(floor)) return footY;
        float candidate = floor + FOOT_OFFSET;
        if (candidate > footY + 0.08f || footY - candidate > snapDistance) return footY;
        return capsuleClear(terrain, x, candidate, z, radius, height) ? candidate : footY;
    }

    public static VerticalMove moveVertical(VolumetricTerrain terrain, float x, float footY, float z,
                                            float deltaY, float radius, float height) {
        if (Math.abs(deltaY) < 0.00001f) return new VerticalMove(footY, false, false);
        int steps = Math.max(1, (int) Math.ceil(Math.abs(deltaY) / VERTICAL_SUBSTEP));
        float sy = deltaY / steps;
        float y = footY;
        for (int i = 0; i < steps; i++) {
            float next = y + sy;

            if (sy < 0f) {
                float floor = terrain.findFloor(x, z, y + 0.28f, 0.85f);
                if (Float.isFinite(floor) && next <= floor + FOOT_OFFSET) {
                    float landedY = firstClearLanding(terrain, x, z, floor, y, radius, height);
                    return new VerticalMove(landedY, true, false);
                }
            }

            if (capsuleClear(terrain, x, next, z, radius, height)) {
                y = next;
                continue;
            }

            if (sy < 0f) {
                float floor = terrain.findFloor(x, z, y + 0.36f, 0.95f);
                if (Float.isFinite(floor)) y = firstClearLanding(terrain, x, z, floor, y, radius, height);
                return new VerticalMove(y, true, false);
            }
            return new VerticalMove(y, false, true);
        }
        return new VerticalMove(y, false, false);
    }

    private static float firstClearLanding(VolumetricTerrain terrain, float x, float z, float floor, float fallbackY,
                                           float radius, float height) {
        for (float offset = FOOT_OFFSET; offset <= 0.18f; offset += 0.02f) {
            float candidate = floor + offset;
            if (candidate <= fallbackY + 0.12f && capsuleClear(terrain, x, candidate, z, radius, height)) return candidate;
        }
        return fallbackY;
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
