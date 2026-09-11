package com.samaheim.game;

/** Small deterministic budget model preventing runaway entity growth during long sessions. */
public final class RuntimeBudget {
    public static final int MAX_ACTIVE_ENEMIES = 24;
    public static final int MAX_ACTIVE_DROPS = 80;
    public static final int MAX_ACTIVE_PARTICLES = 1400;
    public static final int MAX_ACTIVE_STRUCTURES_IN_CELL = 96;

    private RuntimeBudget() {}

    public static boolean canSpawnEnemy(int activeEnemies, boolean nearPlayer, boolean inLoadedCell) {
        return nearPlayer && inLoadedCell && activeEnemies >= 0 && activeEnemies < MAX_ACTIVE_ENEMIES;
    }

    public static int trimDrops(int activeDrops) {
        return Math.clamp(activeDrops, 0, MAX_ACTIVE_DROPS);
    }

    public static int particleBudget(int requested, float frameTimeMs) {
        if (requested <= 0) return 0;
        float factor = frameTimeMs > 25f ? 0.35f : frameTimeMs > 18f ? 0.65f : 1f;
        return Math.min(MAX_ACTIVE_PARTICLES, Math.max(0, Math.round(requested * factor)));
    }

    public static boolean canPlaceInCell(int existingStructures) {
        return existingStructures >= 0 && existingStructures < MAX_ACTIVE_STRUCTURES_IN_CELL;
    }
}
