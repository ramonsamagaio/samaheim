package com.samaheim.world;

/** Deterministic terrain-tool rules used directly by the playable client. */
public final class TerraformToolSystem {
    public static final float DEFAULT_RADIUS = 2.8f;
    public static final float MIN_RADIUS = 1.6f;
    public static final float MAX_RADIUS = 4.2f;
    private static final float SMOOTH_NOOP_ROUGHNESS = 0.035f;

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
                               float standingHeight, float requestedRadius, int stoneAvailable, float staminaAvailable) {
        if (terrain == null || mode == null) return Result.denied("No terrain tool context.");
        if (!Float.isFinite(worldX) || !Float.isFinite(worldZ) || !Float.isFinite(standingHeight)
                || !Float.isFinite(requestedRadius) || requestedRadius <= 0f) {
            return Result.denied("Invalid terrain target.");
        }
        float half = terrain.halfExtent();
        if (Math.abs(worldX) > half || Math.abs(worldZ) > half) return Result.denied("That ground is outside the world.");

        float radius = clamp(requestedRadius, MIN_RADIUS, MAX_RADIUS);
        float areaScale = clamp((radius * radius) / (DEFAULT_RADIUS * DEFAULT_RADIUS), 0.5f, 2.25f);
        int stoneCost = mode == Mode.RAISE ? Math.max(1, Math.round(mode.stoneCost() * areaScale)) : 0;
        float slope = terrain.slopeDegrees(worldX, worldZ, Math.max(0.7f, radius * 0.35f));
        float roughness = terrain.heightVariation(worldX, worldZ, Math.max(0.8f, radius * 0.45f));

        if (mode == Mode.SMOOTH && roughness <= SMOOTH_NOOP_ROUGHNESS) {
            return Result.denied("The ground is already smooth here.");
        }

        float effort = 1f;
        if (mode == Mode.RAISE || mode == Mode.LOWER) effort += clamp(slope / 70f, 0f, 0.28f);
        if (mode == Mode.LEVEL || mode == Mode.SMOOTH) effort += clamp(roughness / 4f, 0f, 0.22f);
        float staminaCost = mode.staminaCost() * (0.72f + 0.28f * areaScale) * effort;

        if (stoneAvailable < stoneCost) {
            return Result.denied("Raise ground needs " + stoneCost + " stone for this brush size.");
        }
        if (staminaAvailable + 0.0001f < staminaCost) {
            return Result.denied("Too exhausted to shape the ground.");
        }

        int changed = switch (mode) {
            case LEVEL -> terrain.level(worldX, worldZ, radius, standingHeight,
                    clamp(0.52f + roughness * 0.08f, 0.52f, 0.78f));
            case RAISE -> terrain.raise(worldX, worldZ, radius,
                    clamp(0.58f - slope * 0.0025f, 0.42f, 0.60f));
            case LOWER -> terrain.lower(worldX, worldZ, radius,
                    clamp(0.60f - slope * 0.002f, 0.44f, 0.62f));
            case SMOOTH -> terrain.smooth(worldX, worldZ, radius,
                    clamp(0.28f + roughness * 0.05f, 0.28f, 0.48f));
            case RESTORE -> terrain.restore(worldX, worldZ, radius, 0.72f);
        };

        if (changed == 0) return new Result(false, 0, 0, 0f, "The ground cannot move further here.");
        String detail = switch (mode) {
            case LEVEL -> " leveled around your footing.";
            case RAISE -> " packed into a stable mound.";
            case LOWER -> " cut into the earth.";
            case SMOOTH -> " softened the sharp edges.";
            case RESTORE -> " pulled toward its original shape.";
        };
        return new Result(true, changed, stoneCost, staminaCost, mode.label() + detail);
    }

    private static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    public record Result(boolean applied, int changedSamples, int stoneSpent, float staminaSpent, String message) {
        private static Result denied(String message) {
            return new Result(false, 0, 0, 0f, message);
        }
    }
}
