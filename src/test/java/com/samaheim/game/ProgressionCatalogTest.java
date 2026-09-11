package com.samaheim.game;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;

import static org.junit.jupiter.api.Assertions.*;

class ProgressionCatalogTest {
    @Test void advancedCraftingRequiresWorkbench() {
        var inventory = new EnumMap<ProgressionCatalog.Material, Integer>(ProgressionCatalog.Material.class);
        inventory.put(ProgressionCatalog.Material.WOOD, 99);
        inventory.put(ProgressionCatalog.Material.IRON, 99);
        assertFalse(ProgressionCatalog.canCraft(ProgressionCatalog.Recipe.IRON_SWORD, inventory, false));
        assertTrue(ProgressionCatalog.canCraft(ProgressionCatalog.Recipe.IRON_SWORD, inventory, true));
    }

    @Test void starterCraftsRemainAvailableBeforeWorkbench() {
        var inventory = new EnumMap<ProgressionCatalog.Material, Integer>(ProgressionCatalog.Material.class);
        inventory.put(ProgressionCatalog.Material.WOOD, 8);
        inventory.put(ProgressionCatalog.Material.STONE, 4);
        assertTrue(ProgressionCatalog.canCraft(ProgressionCatalog.Recipe.WANDERER_BLADE, inventory, false));
    }

    @Test void missingMaterialBlocksCraft() {
        var inventory = new EnumMap<ProgressionCatalog.Material, Integer>(ProgressionCatalog.Material.class);
        inventory.put(ProgressionCatalog.Material.WOOD, 99);
        assertFalse(ProgressionCatalog.canCraft(ProgressionCatalog.Recipe.CAMPFIRE, inventory, false));
    }

    @Test void dungeonRewardsScaleWithCompletionDepth() {
        assertEquals(0, ProgressionCatalog.rewardTierForDungeon(8, false));
        assertEquals(1, ProgressionCatalog.rewardTierForDungeon(3, true));
        assertEquals(2, ProgressionCatalog.rewardTierForDungeon(6, true));
        assertEquals(3, ProgressionCatalog.rewardTierForDungeon(9, true));
    }
}
