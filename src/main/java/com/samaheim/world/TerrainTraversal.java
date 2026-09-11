package com.samaheim.world;

/** Deterministic traversal rules for walking over the mutable heightfield. */
public final class TerrainTraversal {
    private static final float MAX_STEP_UP = 0.48f;
    private static final float MAX_STEP_DOWN = 0.72f;
    private static final float MAX_WALK_SLOPE = 52f;
    private static final float SAMPLE_SPACING = 0.28f;

    private TerrainTraversal() {}

    public record Result(float x, float z, float groundY, boolean blocked, boolean stepped) {}

    public static Result resolve(TerrainState terrain, float fromX, float fromZ, float toX, float toZ) {
        if (terrain == null || !finite(fromX, fromZ, toX, toZ)) {
            return new Result(fromX, fromZ, 0f, true, false);
        }
        float startGround = terrain.sampleHeight(fromX, fromZ);
        float dx = toX - fromX;
        float dz = toZ - fromZ;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        int steps = Math.max(1, (int) Math.ceil(distance / SAMPLE_SPACING));
        float x = fromX;
        float z = fromZ;
        float previousGround = startGround;
        boolean stepped = false;

        for (int i = 1; i <= steps; i++) {
            float t = i / (float) steps;
            float sx = fromX + dx * t;
            float sz = fromZ + dz * t;
            float ground = terrain.sampleHeight(sx, sz);
            float delta = ground - previousGround;
            float slope = terrain.slopeDegrees(sx, sz, 0.55f);
            if (delta > MAX_STEP_UP || delta < -MAX_STEP_DOWN || slope > MAX_WALK_SLOPE) {
                return new Result(x, z, previousGround, true, stepped);
            }
            if (Math.abs(delta) > 0.12f) stepped = true;
            x = sx;
            z = sz;
            previousGround = ground;
        }
        return new Result(x, z, previousGround, false, stepped);
    }

    public static float maxStepUp() { return MAX_STEP_UP; }
    public static float maxStepDown() { return MAX_STEP_DOWN; }
    public static float maxWalkSlope() { return MAX_WALK_SLOPE; }

    private static boolean finite(float... values) {
        for (float value : values) if (!Float.isFinite(value)) return false;
        return true;
    }
}
