package com.samaheim.world;

import java.util.List;

/** Deterministic support, snapping and placement rules used by the graphical sandbox. */
public final class BuildingPhysics {
    private static final float SNAP = 0.5f;
    private static final float FLOOR_HALF = 1.4f;
    private static final float FLOOR_TOP = 0.24f;
    private static final float MAX_STEP_UP = 0.48f;

    private BuildingPhysics() {}

    public record Floor(float x, float baseY, float z, float yawRadians) {
        public float topY() { return baseY + FLOOR_TOP; }
    }

    public static float snap(float value) {
        if (!Float.isFinite(value)) return 0f;
        return Math.round(value / SNAP) * SNAP;
    }

    public static float snapYaw(float yawRadians) {
        if (!Float.isFinite(yawRadians)) return 0f;
        float quarter = (float) (Math.PI * 0.5);
        return Math.round(yawRadians / quarter) * quarter;
    }

    /** Highest terrain/floor support the actor can actually step onto from its current foot height. */
    public static float supportHeight(float x, float z, float terrainY, float currentFootY, List<Floor> floors) {
        float best = terrainY;
        if (floors == null) return best;
        for (Floor floor : floors) {
            if (floor == null || !finite(floor.x(), floor.baseY(), floor.z(), floor.yawRadians())) continue;
            if (!insideFloor(x, z, floor)) continue;
            float top = floor.topY();
            if (top <= currentFootY + MAX_STEP_UP && top > best) best = top;
        }
        return best;
    }

    public static boolean insideFloor(float worldX, float worldZ, Floor floor) {
        float cos = (float) Math.cos(floor.yawRadians());
        float sin = (float) Math.sin(floor.yawRadians());
        float dx = worldX - floor.x();
        float dz = worldZ - floor.z();
        float lx = cos * dx + sin * dz;
        float lz = -sin * dx + cos * dz;
        return Math.abs(lx) <= FLOOR_HALF && Math.abs(lz) <= FLOOR_HALF;
    }

    public static boolean floorsOverlap(float x, float z, List<Floor> floors) {
        if (floors == null) return false;
        for (Floor floor : floors) {
            if (floor != null && Math.abs(x - floor.x()) < 0.35f && Math.abs(z - floor.z()) < 0.35f) return true;
        }
        return false;
    }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }
}
