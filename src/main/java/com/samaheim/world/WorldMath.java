package com.samaheim.world;

public final class WorldMath {
    private WorldMath() {
    }

    public static float height(long seed, float x, float z) {
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

    private static float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }
}
