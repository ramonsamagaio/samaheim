package com.samaheim.world;

/** Deterministic movement rules for wading, swimming, buoyancy and breath. */
public final class WaterPhysics {
    public static final float WADING_DEPTH = 0.14f;
    public static final float SWIM_DEPTH = 1.02f;
    public static final float FLOAT_DEPTH = 1.18f;

    private WaterPhysics() { }

    public static float depth(float footY, float waterLevel) {
        return Math.max(0f, waterLevel - footY);
    }

    public static boolean isWading(float footY, float waterLevel) {
        float depth = depth(footY, waterLevel);
        return depth >= WADING_DEPTH && depth < SWIM_DEPTH;
    }

    public static boolean isSwimming(float footY, float waterLevel) {
        return depth(footY, waterLevel) >= SWIM_DEPTH;
    }

    public static float horizontalSpeed(boolean swimming, boolean wading, boolean sprinting, float stamina) {
        boolean canPush = sprinting && stamina > 3f;
        if (swimming) return canPush ? 4.15f : 3.0f;
        if (wading) return canPush ? 4.35f : 3.35f;
        return canPush ? 7.1f : 4.6f;
    }

    public static float nextSwimVelocity(float footY, float waterLevel, float velocityY,
                                         boolean ascend, boolean descend, float tpf) {
        float targetFootY = waterLevel - FLOAT_DEPTH;
        float error = targetFootY - footY;
        float acceleration = clamp(error * 8.2f - velocityY * 3.1f, -8.5f, 11.5f);
        if (ascend) acceleration += 8.0f;
        if (descend) acceleration -= 8.5f;
        return clamp(velocityY + acceleration * tpf, -3.6f, 4.1f);
    }

    public static float nextStamina(float stamina, boolean swimming, boolean moving, boolean pushing, float tpf) {
        float value = stamina;
        if (swimming) {
            float drain = moving ? (pushing ? 17f : 9f) : 3.5f;
            value -= drain * tpf;
        } else if (moving && pushing) {
            value -= 20f * tpf;
        } else {
            value += 15f * tpf;
        }
        return clamp(value, 0f, 100f);
    }

    public static float nextBreath(float breath, boolean headUnderwater, float tpf) {
        float value = headUnderwater ? breath - 8f * tpf : breath + 28f * tpf;
        return clamp(value, 0f, 100f);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
