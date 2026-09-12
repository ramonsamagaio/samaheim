package com.samaheim.world;

/** Deterministic macro terrain and frontier-region math. */
public final class WorldMath {
    private static final float LEGACY_HALF_EXTENT = 96f;
    private static final float FRONTIER_BLEND_WIDTH = 22f;

    private WorldMath() {
    }

    public static float height(long seed, float x, float z) {
        float legacy = legacyHeight(seed, x, z);
        float frontierDistance = Math.max(Math.abs(x), Math.abs(z)) - LEGACY_HALF_EXTENT;
        if (frontierDistance <= 0f) return legacy;

        float blend = smootherStep(clamp01(frontierDistance / FRONTIER_BLEND_WIDTH));
        float macro = valueNoise(seed ^ 0x6A09E667F3BCC909L, x * 0.010f, z * 0.010f);
        float moisture = valueNoise(seed ^ 0xBB67AE8584CAA73BL, (x + 311f) * 0.008f, (z - 197f) * 0.008f);
        float relief = valueNoise(seed ^ 0x3C6EF372FE94F82BL, (x - 127f) * 0.013f, (z + 433f) * 0.013f);
        float ridgeNoise = valueNoise(seed ^ 0xA54FF53A5F1D36F1L, x * 0.020f, z * 0.020f);
        float ridge = 1f - Math.abs(ridgeNoise);

        float highlandMask = smootherStep(clamp01((relief - 0.08f) / 0.67f));
        float fenMask = smootherStep(clamp01((moisture - 0.12f) / 0.72f)) * (1f - highlandMask);
        float ashenMask = smootherStep(clamp01((-moisture - 0.16f) / 0.68f)) * (1f - highlandMask);

        float rolling = macro * 1.55f;
        float highlands = highlandMask * (1.2f + ridge * ridge * 5.4f + relief * 1.5f);
        float fen = fenMask * (1.65f + Math.abs(macro) * 0.65f);
        float ashen = ashenMask * (valueNoise(seed ^ 0x510E527FADE682D1L, x * 0.035f, z * 0.035f) * 2.0f
                + (float) Math.sin((x - z) * 0.065f) * 0.75f);
        float frontier = legacy + rolling + highlands - fen + ashen;
        return lerp(legacy, frontier, blend);
    }

    /**
     * Region label for encounter, palette and resource systems. Height transitions remain continuous;
     * this label is intentionally descriptive rather than a hard terrain boundary.
     */
    public static Region region(long seed, float x, float z) {
        if (Math.max(Math.abs(x), Math.abs(z)) <= LEGACY_HALF_EXTENT) return Region.GREENMARCH;
        float moisture = valueNoise(seed ^ 0xBB67AE8584CAA73BL, (x + 311f) * 0.008f, (z - 197f) * 0.008f);
        float relief = valueNoise(seed ^ 0x3C6EF372FE94F82BL, (x - 127f) * 0.013f, (z + 433f) * 0.013f);
        if (relief > 0.22f) return Region.IRON_HIGHLANDS;
        if (moisture > 0.24f) return Region.MISTFEN;
        if (moisture < -0.26f) return Region.ASHEN_REACH;
        return Region.GREENMARCH;
    }

    static float legacyHeight(long seed, float x, float z) {
        double s = seed * 0.00000011920928955078125;
        double broad = Math.sin((x + s) * 0.035) * 2.2 + Math.cos((z - s) * 0.031) * 1.8;
        double medium = Math.sin((x + z) * 0.083 + s) * 0.9;
        double detail = valueNoise(seed, x * 0.055f, z * 0.055f) * 2.1;
        return (float) (broad + medium + detail - 1.1);
    }

    public static float valueNoise(long seed, float x, float z) {
        int x0 = fastFloor(x);
        int z0 = fastFloor(z);
        int x1 = x0 + 1;
        int z1 = z0 + 1;
        float tx = smooth(x - x0);
        float tz = smooth(z - z0);

        float a = hash01(seed, x0, z0) * 2f - 1f;
        float b = hash01(seed, x1, z0) * 2f - 1f;
        float c = hash01(seed, x0, z1) * 2f - 1f;
        float d = hash01(seed, x1, z1) * 2f - 1f;
        return lerp(lerp(a, b, tx), lerp(c, d, tx), tz);
    }

    public static float hash01(long seed, int x, int z) {
        long n = seed;
        n ^= 0x9E3779B97F4A7C15L + x * 0x632BE59BD9B4E019L;
        n = Long.rotateLeft(n, 27);
        n ^= z * 0x85157AF5L;
        n *= 0x94D049BB133111EBL;
        n ^= n >>> 31;
        return (n >>> 40) / (float) (1L << 24);
    }

    private static int fastFloor(float value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private static float smooth(float t) {
        return t * t * (3f - 2f * t);
    }

    private static float smootherStep(float t) {
        return t * t * t * (t * (t * 6f - 15f) + 10f);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public enum Region {
        GREENMARCH,
        IRON_HIGHLANDS,
        MISTFEN,
        ASHEN_REACH
    }
}