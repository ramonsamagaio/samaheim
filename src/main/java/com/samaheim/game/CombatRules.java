package com.samaheim.game;

/** Pure combat tuning shared by the graphical runtime and regression tests. */
public final class CombatRules {
    private CombatRules() { }

    public static PlayerAttack playerAttack(boolean blade, AttackKind kind) {
        if (!blade) {
            return kind == AttackKind.HEAVY
                    ? new PlayerAttack(12f, 3.25f, 18f, 0.92f, 0.58f)
                    : new PlayerAttack(7f, 3.0f, 4f, 0.72f, 0.18f);
        }
        return kind == AttackKind.HEAVY
                ? new PlayerAttack(42f, 4.0f, 26f, 0.94f, 1.15f)
                : new PlayerAttack(22f, 3.6f, 8f, 0.48f, 0.42f);
    }

    public static boolean canAttack(float stamina, float cooldown, PlayerAttack attack) {
        return cooldown <= 0f && stamina + 0.0001f >= attack.staminaCost;
    }

    public static float spendStamina(float stamina, PlayerAttack attack) {
        return Math.max(0f, stamina - attack.staminaCost);
    }

    public static EnemyAttack enemyAttack(EnemyArchetype type) {
        return switch (type) {
            case GOBLIN -> new EnemyAttack(7f, 1.45f, 0.44f, 1.16f);
            case GRAVEBORN -> new EnemyAttack(11f, 1.52f, 0.72f, 1.42f);
            case HIGHLAND_BRUTE -> new EnemyAttack(18f, 1.78f, 0.96f, 1.82f);
            case MIRE_STALKER -> new EnemyAttack(8f, 1.62f, 0.26f, 0.78f);
            case ASH_WRAITH -> new EnemyAttack(15f, 2.08f, 0.56f, 1.18f);
        };
    }

    public static EnemyState beginEnemyWindup(EnemyState state, EnemyAttack attack) {
        if (state.cooldown > 0f || state.windup > 0f || state.stagger > 0f) return state;
        return new EnemyState(state.cooldown, attack.windupSeconds, state.stagger);
    }

    public static EnemyState applyStagger(EnemyState state, float staggerSeconds) {
        float stagger = Math.max(state.stagger, Math.max(0f, staggerSeconds));
        return new EnemyState(state.cooldown, 0f, stagger);
    }

    public static EnemyTick tickEnemy(EnemyState state, EnemyAttack attack, float tpf, boolean targetInRange) {
        float dt = Math.max(0f, tpf);
        float cooldown = Math.max(0f, state.cooldown - dt);
        float stagger = Math.max(0f, state.stagger - dt);
        float windup = state.windup;
        boolean strike = false;

        if (stagger > 0f) return new EnemyTick(new EnemyState(cooldown, 0f, stagger), false);
        if (windup > 0f) {
            float next = windup - dt;
            if (next <= 0f) {
                strike = targetInRange;
                cooldown = attack.recoverySeconds;
                windup = 0f;
            } else {
                windup = next;
            }
        }
        return new EnemyTick(new EnemyState(cooldown, windup, stagger), strike);
    }

    public static float telegraphIntensity(float windup, EnemyAttack attack) {
        if (windup <= 0f || attack.windupSeconds <= 0f) return 0f;
        return Math.max(0f, Math.min(1f, 1f - windup / attack.windupSeconds));
    }

    public enum AttackKind { LIGHT, HEAVY }
    public enum EnemyArchetype { GOBLIN, GRAVEBORN, HIGHLAND_BRUTE, MIRE_STALKER, ASH_WRAITH }

    public record PlayerAttack(float damage, float range, float staminaCost, float cooldownSeconds,
                               float staggerSeconds) { }
    public record EnemyAttack(float damage, float range, float windupSeconds, float recoverySeconds) { }
    public record EnemyState(float cooldown, float windup, float stagger) { }
    public record EnemyTick(EnemyState state, boolean strike) { }
}
