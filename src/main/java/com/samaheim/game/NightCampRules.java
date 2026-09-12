package com.samaheim.game;

/** Pure tuning for night pressure, campfire safety and resting. */
public final class NightCampRules {
    public static final float CAMPFIRE_WARMTH_RADIUS = 8.0f;
    public static final float MORNING_CLOCK = 0.12f;

    private NightCampRules() { }

    public static boolean isNight(float dayClock) {
        float clock = normalize(dayClock);
        return clock >= 0.52f || clock < 0.03f;
    }

    public static boolean warmedByCampfire(float distance) {
        return Float.isFinite(distance) && distance <= CAMPFIRE_WARMTH_RADIUS;
    }

    public static float roamingSpawnInterval(boolean night, boolean warmed) {
        if (night && !warmed) return 38f;
        if (night) return 92f;
        return warmed ? 110f : 85f;
    }

    public static float hungerDrainMultiplier(boolean night, boolean warmed) {
        if (night && !warmed) return 1.35f;
        if (warmed) return 0.72f;
        return 1f;
    }

    public static float staminaRecoveryBonus(boolean night, boolean warmed) {
        return warmed ? (night ? 7.5f : 4.0f) : 0f;
    }

    public static boolean canRest(boolean night, boolean warmed, boolean threatened) {
        return night && warmed && !threatened;
    }

    public static RestResult rest(float health, float stamina, float hunger) {
        return new RestResult(
                Math.min(100f, health + 30f),
                100f,
                Math.max(0f, hunger - 8f),
                MORNING_CLOCK);
    }

    private static float normalize(float value) {
        float v = value % 1f;
        return v < 0f ? v + 1f : v;
    }

    public record RestResult(float health, float stamina, float hunger, float dayClock) { }
}
