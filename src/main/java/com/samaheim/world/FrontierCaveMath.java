package com.samaheim.world;

/**
 * Cheap deterministic signed field for natural frontier caves.
 * Negative values are cave air. Positive values leave the base terrain solid.
 * The legacy world footprint is deliberately excluded for save compatibility.
 */
public final class FrontierCaveMath {
    public static final float CAVE_START_EXTENT = 118f;
    public static final float MAX_CAVE_DEPTH = 16.5f;
    private static final ColumnProfile DISABLED = new ColumnProfile(false, 8f, 0f, 0f, false, 8f, 0f);

    private FrontierCaveMath() { }

    public static boolean columnMayContainCaves(float x, float z) {
        return Math.max(Math.abs(x), Math.abs(z)) > CAVE_START_EXTENT;
    }

    /** Precomputes all expensive horizontal trig once for a whole X/Z density column. */
    public static ColumnProfile columnProfile(long seed, float x, float z) {
        if (!columnMayContainCaves(x, z)) return DISABLED;

        double phase = seed * 0.00000017320508075688773;
        float corridor = Math.abs((float) Math.sin((x + phase) * 0.071)
                + 0.68f * (float) Math.cos((z - phase) * 0.063)
                + 0.34f * (float) Math.sin((x + z) * 0.037 + phase * 1.7));
        float chamber = (float) Math.sin((x - phase) * 0.029)
                * (float) Math.cos((z + phase) * 0.033);

        float corridorThreshold = 0.36f + Math.max(0f, chamber) * 0.16f;
        float horizontalSigned = (corridor - corridorThreshold) * 3.1f;
        float centerDepth = 7.2f
                + 1.9f * (float) Math.sin((x - z) * 0.026 + phase)
                + 0.8f * (float) Math.cos(z * 0.041 - phase);
        float verticalRadius = 1.25f + Math.max(0f, chamber) * 1.45f;

        float entranceSignal = (float) Math.sin((x + phase) * 0.043)
                * (float) Math.cos((z - phase) * 0.039);
        boolean entrance = entranceSignal > 0.86f && corridor < 0.22f;
        float entranceHorizontal = entrance ? (corridor - 0.20f) * 3.5f : 8f;
        float entranceCenterDepth = entrance
                ? 2.4f + 0.55f * (float) Math.sin((x + z) * 0.05 + phase)
                : 0f;
        return new ColumnProfile(true, horizontalSigned, centerDepth, verticalRadius,
                entrance, entranceHorizontal, entranceCenterDepth);
    }

    public static float caveDensity(ColumnProfile profile, float depth) {
        if (!profile.enabled || depth < 0.45f || depth > MAX_CAVE_DEPTH) return 8f;
        float verticalSigned = Math.abs(depth - profile.centerDepth) - profile.verticalRadius;
        float cave = Math.max(profile.horizontalSigned, verticalSigned);
        if (profile.entrance) {
            float entranceVertical = Math.abs(depth - profile.entranceCenterDepth) - 2.35f;
            cave = Math.min(cave, Math.max(profile.entranceHorizontal, entranceVertical));
        }
        return cave;
    }

    public static float caveDensity(long seed, float x, float y, float z, float surfaceY) {
        return caveDensity(columnProfile(seed, x, z), surfaceY - y);
    }

    public record ColumnProfile(boolean enabled, float horizontalSigned, float centerDepth, float verticalRadius,
                                boolean entrance, float entranceHorizontal, float entranceCenterDepth) { }
}