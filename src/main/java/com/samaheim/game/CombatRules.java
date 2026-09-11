package com.samaheim.game;

import java.util.Objects;

public final class CombatRules {
    public enum Weapon {
        FISTS("Fists", 7f, 8f, 0.72f, 2.2f),
        WANDERER_BLADE("Wanderer's Blade", 22f, 15f, 0.48f, 3.6f),
        IRON_LONGSWORD("Iron Longsword", 31f, 19f, 0.62f, 3.8f),
        ARCANE_BLADE("Arcane Blade", 39f, 23f, 0.56f, 4.0f);

        private final String displayName;
        private final float damage;
        private final float staminaCost;
        private final float cooldown;
        private final float range;

        Weapon(String displayName, float damage, float staminaCost, float cooldown, float range) {
            this.displayName = displayName;
            this.damage = damage;
            this.staminaCost = staminaCost;
            this.cooldown = cooldown;
            this.range = range;
        }

        public String displayName() { return displayName; }
        public float damage() { return damage; }
        public float staminaCost() { return staminaCost; }
        public float cooldown() { return cooldown; }
        public float range() { return range; }
    }

    public enum EnemyArchetype {
        GOBLIN_SCOUT("Goblin Scout", 42f, 7f, 1.25f, 4.8f, 18f),
        GRAVEBORN("Graveborn", 68f, 11f, 1.45f, 3.7f, 22f),
        DIRE_WOLF("Dire Wolf", 54f, 9f, 0.95f, 6.4f, 24f),
        CULTIST("Ash Cultist", 58f, 13f, 1.65f, 4.2f, 26f),
        OGRE("Hill Ogre", 145f, 21f, 2.3f, 2.7f, 28f);

        private final String displayName;
        private final float maxHealth;
        private final float damage;
        private final float attackDelay;
        private final float speed;
        private final float noticeRange;

        EnemyArchetype(String displayName, float maxHealth, float damage, float attackDelay,
                       float speed, float noticeRange) {
            this.displayName = displayName;
            this.maxHealth = maxHealth;
            this.damage = damage;
            this.attackDelay = attackDelay;
            this.speed = speed;
            this.noticeRange = noticeRange;
        }

        public String displayName() { return displayName; }
        public float maxHealth() { return maxHealth; }
        public float damage() { return damage; }
        public float attackDelay() { return attackDelay; }
        public float speed() { return speed; }
        public float noticeRange() { return noticeRange; }
    }

    public record AttackResult(boolean executed, float damage, float remainingStamina, float cooldown,
                               boolean critical, String reason) {
    }

    private CombatRules() {
    }

    public static AttackResult attack(Weapon weapon, float stamina, float currentCooldown, boolean criticalWindow) {
        Objects.requireNonNull(weapon, "weapon");
        if (currentCooldown > 0f) {
            return new AttackResult(false, 0f, stamina, currentCooldown, false, "cooldown");
        }
        if (stamina < weapon.staminaCost()) {
            return new AttackResult(false, 0f, stamina, 0.2f, false, "stamina");
        }
        float damage = weapon.damage() * (criticalWindow ? 1.6f : 1f);
        return new AttackResult(true, damage, Math.max(0f, stamina - weapon.staminaCost()),
                weapon.cooldown(), criticalWindow, "ok");
    }

    public static float applyArmor(float rawDamage, float armor) {
        if (rawDamage <= 0f) {
            return 0f;
        }
        float mitigation = Math.clamp(armor / (armor + 50f), 0f, 0.72f);
        return rawDamage * (1f - mitigation);
    }

    public static float staminaRecovery(float current, float max, float perSecond, float seconds, boolean exhausted) {
        float multiplier = exhausted ? 0.55f : 1f;
        return Math.min(max, Math.max(0f, current) + perSecond * seconds * multiplier);
    }

    public static boolean isBackstab(float facingDotToAttacker) {
        return facingDotToAttacker > 0.55f;
    }
}
