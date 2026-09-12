package com.samaheim.game;

import com.samaheim.world.WorldMath;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EquipmentRulesTest {
    @Test
    void highlandRocksProvideIronAndMistfenBerriesProvideResin() {
        assertEquals(1, EquipmentRules.gatherYield(WorldMath.Region.IRON_HIGHLANDS,
                EquipmentRules.GatherKind.ROCK).ironOre());
        assertEquals(1, EquipmentRules.gatherYield(WorldMath.Region.MISTFEN,
                EquipmentRules.GatherKind.BERRY).mistResin());
        assertEquals(0, EquipmentRules.gatherYield(WorldMath.Region.GREENMARCH,
                EquipmentRules.GatherKind.ROCK).ironOre());
    }

    @Test
    void ashenEnemiesDropCinderAndGravebornDropMore() {
        assertEquals(1, EquipmentRules.enemyDrop(WorldMath.Region.ASHEN_REACH,
                EquipmentRules.EnemyKind.GOBLIN).cinderShard());
        assertEquals(2, EquipmentRules.enemyDrop(WorldMath.Region.ASHEN_REACH,
                EquipmentRules.EnemyKind.GRAVEBORN).cinderShard());
        assertEquals(0, EquipmentRules.enemyDrop(WorldMath.Region.MISTFEN,
                EquipmentRules.EnemyKind.GRAVEBORN).cinderShard());
    }

    @Test
    void advancedWeaponsRequireFrontierMaterials() {
        EquipmentRules.Materials basic = new EquipmentRules.Materials(99, 99, 0, 0, 0);
        assertTrue(EquipmentRules.canCraft(EquipmentRules.Weapon.WANDERERS_BLADE, basic));
        assertFalse(EquipmentRules.canCraft(EquipmentRules.Weapon.HIGHLAND_MAUL, basic));
        assertFalse(EquipmentRules.canCraft(EquipmentRules.Weapon.MIST_PIKE, basic));
        assertFalse(EquipmentRules.canCraft(EquipmentRules.Weapon.CINDER_BLADE, basic));
    }

    @Test
    void craftingSpendsExactlyOneRecipe() {
        EquipmentRules.Materials materials = new EquipmentRules.Materials(10, 10, 8, 8, 8);
        EquipmentRules.Materials after = EquipmentRules.spend(EquipmentRules.Weapon.HIGHLAND_MAUL, materials);
        assertEquals(4, after.wood());
        assertEquals(2, after.stone());
        assertEquals(3, after.ironOre());
        assertEquals(8, after.mistResin());
        assertEquals(8, after.cinderShard());
    }

    @Test
    void weaponsCreateDistinctCombatRoles() {
        EquipmentRules.AttackTuning maul = EquipmentRules.tuning(EquipmentRules.Weapon.HIGHLAND_MAUL);
        EquipmentRules.AttackTuning pike = EquipmentRules.tuning(EquipmentRules.Weapon.MIST_PIKE);
        EquipmentRules.AttackTuning cinder = EquipmentRules.tuning(EquipmentRules.Weapon.CINDER_BLADE);
        assertTrue(maul.damageMultiplier() > cinder.damageMultiplier());
        assertTrue(maul.staggerMultiplier() > pike.staggerMultiplier());
        assertTrue(pike.rangeBonus() > cinder.rangeBonus());
        assertTrue(cinder.cooldownMultiplier() < 1f);
    }
}
