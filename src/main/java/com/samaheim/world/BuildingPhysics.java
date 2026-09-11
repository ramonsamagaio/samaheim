package com.samaheim.world;

import java.util.List;

/** Deterministic support, snapping and placement rules used by the graphical sandbox. */
public final class BuildingPhysics {
    private static final float SNAP = 0.5f;
    private static final float FLOOR_HALF = 1.4f;
    private static final float FLOOR_TOP = 0.24f;
    private static final float MAX_STEP_UP = 0.48f;
    private static final float RAMP_HALF_WIDTH = 1.25f;
    private static final float RAMP_HALF_LENGTH = 1.5f;
    private static final float RAMP_RISE = 1.25f;

    private BuildingPhysics() {}

    public record Floor(float x, float baseY, float z, float yawRadians) {
        public float topY() { return baseY + FLOOR_TOP; }
    }

    public record Ramp(float x, float baseY, float z, float yawRadians) {}

    public static float snap(float value) {
        if (!Float.isFinite(value)) return 0f;
        return Math.round(value / SNAP) * SNAP;
    }

    public static float snapYaw(float yawRadians) {
        if (!Float.isFinite(yawRadians)) return 0f;
        float quarter = (float) (Math.PI * 0.5);
        return Math.round(yawRadians / quarter) * quarter;
    }

    /** Highest terrain/floor/ramp support the actor can actually occupy from its current foot height. */
    public static float supportHeight(float x, float z, float terrainY, float currentFootY,
                                      List<Floor> floors, List<Ramp> ramps) {
        float best = supportHeight(x, z, terrainY, currentFootY, floors);
        if (ramps == null) return best;
        for (Ramp ramp : ramps) {
            if (ramp == null || !finite(ramp.x(), ramp.baseY(), ramp.z(), ramp.yawRadians())) continue;
            float support = rampHeightAt(x, z, ramp);
            if (!Float.isFinite(support)) continue;
            // Ramps are continuous, so allow a slightly larger vertical delta than a hard step.
            if (support <= currentFootY + 0.72f && support > best) best = support;
        }
        return best;
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

    public static float rampHeightAt(float worldX, float worldZ, Ramp ramp) {
        float cos = (float) Math.cos(ramp.yawRadians());
        float sin = (float) Math.sin(ramp.yawRadians());
        float dx = worldX - ramp.x();
        float dz = worldZ - ramp.z();
        float lx = cos * dx + sin * dz;
        float lz = -sin * dx + cos * dz;
        if (Math.abs(lx) > RAMP_HALF_WIDTH || Math.abs(lz) > RAMP_HALF_LENGTH) return Float.NaN;
        float t = (lz + RAMP_HALF_LENGTH) / (RAMP_HALF_LENGTH * 2f);
        return ramp.baseY() + 0.12f + t * RAMP_RISE;
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

    /** Basic structural rule: elevated pieces need nearby support below them. */
    public static boolean hasSupport(float x, float y, float z, float terrainY,
                                     List<Floor> floors, List<Ramp> ramps, float horizontalReach) {
        if (y <= terrainY + 0.35f) return true;
        float reach2 = horizontalReach * horizontalReach;
        if (floors != null) {
            for (Floor floor : floors) {
                if (floor == null || floor.topY() > y + 0.35f || y - floor.topY() > 2.8f) continue;
                float dx = x - floor.x(), dz = z - floor.z();
                if (dx * dx + dz * dz <= reach2) return true;
            }
        }
        if (ramps != null) {
            for (Ramp ramp : ramps) {
                if (ramp == null) continue;
                float top = ramp.baseY() + RAMP_RISE + 0.12f;
                if (top > y + 0.35f || y - top > 2.8f) continue;
                float dx = x - ramp.x(), dz = z - ramp.z();
                if (dx * dx + dz * dz <= reach2) return true;
            }
        }
        return false;
    }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }
}
