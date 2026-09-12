package com.samaheim.game;

import com.samaheim.world.WorldMath;

/** Deterministic weapon crafting and regional material progression. */
public final class EquipmentRules {
    private EquipmentRules() { }

    public static Recipe recipe(Weapon weapon) {
        return switch (weapon) {
            case FISTS -> new Recipe(0, 0, 0, 0, 0);
            case WANDERERS_BLADE -> new Recipe(8, 4, 0, 0, 0);
            case HIGHLAND_MAUL -> new Recipe(6, 8, 5, 0, 0);
            case MIST_PIKE -> new Recipe(10, 5, 2, 5, 0);
            case CINDER_BLADE -> new Recipe(8, 8, 3, 2, 5);
        };
    }

    public static AttackTuning tuning(Weapon weapon) {
        return switch (weapon) {
            case FISTS -> new AttackTuning(1f, 0f, 1f, 1f, 1f);
            case WANDERERS_BLADE -> new AttackTuning(1f, 0f, 1f, 1f, 1f);
            case HIGHLAND_MAUL -> new AttackTuning(1.34f, -0.25f, 1.22f, 1.20f, 1.65f);
            case MIST_PIKE -> new AttackTuning(0.88f, 1.35f, 0.88f, 0.92f, 0.72f);
            case CINDER_BLADE -> new AttackTuning(1.18f, 0.35f, 1.06f, 0.84f, 1.10f);
        };
    }

    public static MaterialYield gatherYield(WorldMath.Region region, GatherKind kind) {
        if (region == WorldMath.Region.IRON_HIGHLANDS && kind == GatherKind.ROCK) {
            return new MaterialYield(1, 0, 0);
        }
        if (region == WorldMath.Region.MISTFEN && kind == GatherKind.BERRY) {
            return new MaterialYield(0, 1, 0);
        }
        return MaterialYield.NONE;
    }

    public static MaterialYield enemyDrop(WorldMath.Region region, EnemyKind kind) {
        if (region != WorldMath.Region.ASHEN_REACH) return MaterialYield.NONE;
        return kind == EnemyKind.GRAVEBORN
                ? new MaterialYield(0, 0, 2)
                : new MaterialYield(0, 0, 1);
    }

    public static boolean canCraft(Weapon weapon, Materials materials) {
        Recipe recipe = recipe(weapon);
        return materials.wood >= recipe.wood
                && materials.stone >= recipe.stone
                && materials.ironOre >= recipe.ironOre
                && materials.mistResin >= recipe.mistResin
                && materials.cinderShard >= recipe.cinderShard;
    }

    public static Materials spend(Weapon weapon, Materials materials) {
        if (!canCraft(weapon, materials)) return materials;
        Recipe r = recipe(weapon);
        return new Materials(materials.wood - r.wood, materials.stone - r.stone,
                materials.ironOre - r.ironOre, materials.mistResin - r.mistResin,
                materials.cinderShard - r.cinderShard);
    }

    public enum Weapon {
        FISTS("Fists"),
        WANDERERS_BLADE("Wanderer's Blade"),
        HIGHLAND_MAUL("Highland Maul"),
        MIST_PIKE("Mist Pike"),
        CINDER_BLADE("Cinder Blade");

        private final String label;
        Weapon(String label) { this.label = label; }
        public String label() { return label; }
    }

    public enum GatherKind { TREE, ROCK, BERRY }
    public enum EnemyKind { GOBLIN, GRAVEBORN }

    public record Recipe(int wood, int stone, int ironOre, int mistResin, int cinderShard) { }
    public record AttackTuning(float damageMultiplier, float rangeBonus, float staminaMultiplier,
                               float cooldownMultiplier, float staggerMultiplier) { }
    public record MaterialYield(int ironOre, int mistResin, int cinderShard) {
        public static final MaterialYield NONE = new MaterialYield(0, 0, 0);
    }
    public record Materials(int wood, int stone, int ironOre, int mistResin, int cinderShard) { }
}
