package com.samaheim.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ProgressionStateTest {
    @Test
    void firstHourCriticalPathAdvancesInOrder() {
        Inventory inventory = new Inventory();
        ProgressionState state = new ProgressionState();

        assertEquals(ProgressionState.Stage.GATHER_SUPPLIES, state.stage());
        inventory.add(Inventory.Item.WOOD, 8);
        inventory.add(Inventory.Item.STONE, 4);
        state.updateFromInventory(inventory);
        assertEquals(ProgressionState.Stage.CRAFT_BLADE, state.stage());

        state.markBladeCrafted();
        assertTrue(state.bladeCrafted());
        assertEquals(ProgressionState.Stage.DEFEAT_GOBLINS, state.stage());

        state.markGoblinDefeated();
        state.markGoblinDefeated();
        assertEquals(ProgressionState.Stage.DEFEAT_GOBLINS, state.stage());
        state.markGoblinDefeated();
        assertEquals(ProgressionState.Stage.FIND_ARCANE_SEALS, state.stage());

        state.markArcaneSealFound();
        state.markArcaneSealFound();
        assertFalse(state.campBuilt());
        state.markArcaneSealFound();
        assertEquals(ProgressionState.Stage.BUILD_CAMP, state.stage());

        state.markCampBuilt();
        assertTrue(state.campBuilt());
        assertEquals(ProgressionState.Stage.SURVIVE_NIGHT, state.stage());
    }

    @Test
    void completingObjectivesOutOfOrderCannotSoftlockTheQuest() {
        Inventory inventory = new Inventory();
        ProgressionState state = new ProgressionState();

        state.markArcaneSealFound();
        state.markArcaneSealFound();
        state.markArcaneSealFound();
        state.markCampBuilt();
        state.markGoblinDefeated();
        state.markGoblinDefeated();
        state.markGoblinDefeated();
        state.markBladeCrafted();

        inventory.add(Inventory.Item.WOOD, 8);
        inventory.add(Inventory.Item.STONE, 4);
        state.updateFromInventory(inventory);

        assertEquals(ProgressionState.Stage.SURVIVE_NIGHT, state.stage());
    }
}
