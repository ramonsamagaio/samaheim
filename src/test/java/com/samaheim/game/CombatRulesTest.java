package com.samaheim.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CombatRulesTest {
    @Test
    void attackConsumesStaminaAndHonorsCooldown() {
        CombatRules.AttackResult first = CombatRules.attack(CombatRules.Weapon.WANDERER_BLADE, 40f, 0f, false);
        assertTrue(first.executed());
        assertEquals(25f, first.remainingStamina(), 0.001f);
        assertEquals(22f, first.damage(), 0.001f);

        CombatRules.AttackResult blocked = CombatRules.attack(CombatRules.Weapon.WANDERER_BLADE,
                first.remainingStamina(), first.cooldown(), false);
        assertFalse(blocked.executed());
        assertEquals("cooldown", blocked.reason());
    }

    @Test
    void criticalAndArmorProduceBoundedDamage() {
        CombatRules.AttackResult critical = CombatRules.attack(CombatRules.Weapon.IRON_LONGSWORD, 100f, 0f, true);
        assertTrue(critical.damage() > CombatRules.Weapon.IRON_LONGSWORD.damage());
        float armored = CombatRules.applyArmor(critical.damage(), 50f);
        assertTrue(armored > 0f);
        assertTrue(armored < critical.damage());
    }

    @Test
    void insufficientStaminaCannotSwing() {
        CombatRules.AttackResult result = CombatRules.attack(CombatRules.Weapon.ARCANE_BLADE, 2f, 0f, false);
        assertFalse(result.executed());
        assertEquals("stamina", result.reason());
    }
}
