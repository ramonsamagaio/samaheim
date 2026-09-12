package com.samaheim.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CombatRulesTest {
    @Test
    void heavyBladeAttackTradesStaminaAndRecoveryForDamageAndStagger() {
        CombatRules.PlayerAttack light = CombatRules.playerAttack(true, CombatRules.AttackKind.LIGHT);
        CombatRules.PlayerAttack heavy = CombatRules.playerAttack(true, CombatRules.AttackKind.HEAVY);

        assertTrue(heavy.damage() > light.damage());
        assertTrue(heavy.staminaCost() > light.staminaCost());
        assertTrue(heavy.cooldownSeconds() > light.cooldownSeconds());
        assertTrue(heavy.staggerSeconds() > light.staggerSeconds());
        assertTrue(heavy.range() >= light.range());
    }

    @Test
    void attacksRequireEnoughStaminaAndSpendItDeterministically() {
        CombatRules.PlayerAttack heavy = CombatRules.playerAttack(true, CombatRules.AttackKind.HEAVY);
        assertFalse(CombatRules.canAttack(10f, 0f, heavy));
        assertFalse(CombatRules.canAttack(100f, 0.2f, heavy));
        assertTrue(CombatRules.canAttack(100f, 0f, heavy));
        assertTrue(CombatRules.spendStamina(100f, heavy) < 100f);
    }

    @Test
    void enemyWindupDoesNotDealInstantDamage() {
        CombatRules.EnemyAttack attack = CombatRules.enemyAttack(CombatRules.EnemyArchetype.GRAVEBORN);
        CombatRules.EnemyState ready = new CombatRules.EnemyState(0f, 0f, 0f);
        CombatRules.EnemyState winding = CombatRules.beginEnemyWindup(ready, attack);

        CombatRules.EnemyTick early = CombatRules.tickEnemy(winding, attack, attack.windupSeconds() * 0.45f, true);
        assertFalse(early.strike());
        assertTrue(early.state().windup() > 0f);

        CombatRules.EnemyTick resolved = CombatRules.tickEnemy(early.state(), attack, attack.windupSeconds(), true);
        assertTrue(resolved.strike());
        assertTrue(resolved.state().cooldown() > 0f);
    }

    @Test
    void movingOutOfRangeDuringWindupAvoidsTheStrike() {
        CombatRules.EnemyAttack attack = CombatRules.enemyAttack(CombatRules.EnemyArchetype.GOBLIN);
        CombatRules.EnemyState winding = CombatRules.beginEnemyWindup(new CombatRules.EnemyState(0f, 0f, 0f), attack);
        CombatRules.EnemyTick resolved = CombatRules.tickEnemy(winding, attack, attack.windupSeconds() + 0.01f, false);
        assertFalse(resolved.strike());
        assertTrue(resolved.state().cooldown() > 0f);
    }

    @Test
    void staggerCancelsAnEnemyWindup() {
        CombatRules.EnemyAttack attack = CombatRules.enemyAttack(CombatRules.EnemyArchetype.GRAVEBORN);
        CombatRules.EnemyState winding = CombatRules.beginEnemyWindup(new CombatRules.EnemyState(0f, 0f, 0f), attack);
        CombatRules.EnemyState staggered = CombatRules.applyStagger(winding, 1.1f);
        CombatRules.EnemyTick tick = CombatRules.tickEnemy(staggered, attack, 0.2f, true);

        assertFalse(tick.strike());
        assertTrue(tick.state().stagger() > 0f);
        assertTrue(tick.state().windup() <= 0f);
    }

    @Test
    void frontierArchetypesCreateDifferentCombatProblems() {
        CombatRules.EnemyAttack brute = CombatRules.enemyAttack(CombatRules.EnemyArchetype.HIGHLAND_BRUTE);
        CombatRules.EnemyAttack stalker = CombatRules.enemyAttack(CombatRules.EnemyArchetype.MIRE_STALKER);
        CombatRules.EnemyAttack wraith = CombatRules.enemyAttack(CombatRules.EnemyArchetype.ASH_WRAITH);

        assertTrue(brute.damage() > stalker.damage());
        assertTrue(brute.windupSeconds() > stalker.windupSeconds());
        assertTrue(stalker.recoverySeconds() < brute.recoverySeconds());
        assertTrue(wraith.range() > brute.range());
        assertTrue(wraith.damage() > stalker.damage());
    }
}
