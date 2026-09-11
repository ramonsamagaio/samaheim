package com.samaheim.world;

/**
 * Deterministic terrain-tool rules shared by gameplay and tests.
 * Resource costs are decided before the heightfield mutates so a failed action
 * can never create free terrain edits.
 */
public final class TerraformToolSystem {
    public static final float DEFAULT_RADIUS = 2.8f;

    private TerraformToolSystem() { }

    public enum Mode {
        LEVEL("Level ground", 0, 4f),
        RAISE("Raise ground", 2, 7f),
        LOWER("Dig ground", 0, 6f),
        SMOOTH("Smooth ground", 0, 5f),
        RESTORE("Restore ground", 0, 3f);

        private final String label;
        private final int stoneCost;
        private final float staminaCost;

        Mode(String label, int stoneCost, float staminaCost) {
            this.label = label;
            this.stoneCost = stoneCost;
            this.staminaCost = staminaCost;
        }

        public String label() { return label; }
        public int stoneCost() { return stoneCost; }
        public float staminaCost() { return staminaCost; }

        public Mode next() {
            Mode[] values = values();
            return values[(ordinal() + 1) % values.length];
        }
    }

    public static Result apply(TerrainState terrain, Mode mode, float worldX, float worldZ,
                               float standingHeight, float radius, int stoneAvailable, float staminaAvailable) {
        if (terrain == null || mode == null) return Result.denied("No terrain tool context.");
        if (!Float.isFinite(worldX) || !Float.isFinite(worldZ) || !Float.isFinite(standingHeight)
                || !Float.isFinite(radius) || radius <= 0f) {
            return Result.denied("Invalid terrain target.");
        }
        if (stoneAvailable < mode.stoneCost()) return Result.denied("Raise ground requires 2 stone.");
        if (staminaAvailable + 0.0001f < mode.staminaCost()) return Result.denied("Too exhausted to shape the ground.");

        int changed = switch (mode) {
            case LEVEL -> terrain.level(worldX, worldZ, radius, standingHeight, 0.65f);
            case RAISE -> terrain.raise(worldX, worldZ, radius, 0.62f);
            case LOWER -> terrain.lower(worldX, worldZ, radius, 0.62f);
            case SMOOTH -> terrain.smooth(worldX, worldZ, radius, 0.35f);
            case RESTORE -> terrain.restore(worldX, worldZ, radius, 0.8f);
        };

        if (changed == 0) return new Result(false, 0, 0, 0f, "The ground cannot move further here.");
        return new Result(true, changed, mode.stoneCost(), mode.staminaCost(),
                mode.label() + " changed " + changed + " terrain samples.");
    }

    public record Result(boolean applied, int changedSamples, int stoneSpent, float staminaSpent, String message) {
        private static Result denied(String message) {
            return new Result(false, 0, 0, 0f, message);
        }
    }
}
