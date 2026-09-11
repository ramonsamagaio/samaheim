package com.samaheim.game;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class InventoryTest {
    @Test
    void recipeConsumptionIsAtomic() {
        Inventory inventory = new Inventory();
        inventory.add(Inventory.Item.WOOD, 8);
        inventory.add(Inventory.Item.STONE, 3);

        boolean crafted = inventory.consumeRecipe(Map.of(
                Inventory.Item.WOOD, 8,
                Inventory.Item.STONE, 4));

        assertFalse(crafted);
        assertEquals(8, inventory.get(Inventory.Item.WOOD));
        assertEquals(3, inventory.get(Inventory.Item.STONE));
    }

    @Test
    void successfulRecipeConsumesExactlyTheRequiredItems() {
        Inventory inventory = new Inventory();
        inventory.add(Inventory.Item.WOOD, 10);
        inventory.add(Inventory.Item.STONE, 7);

        assertTrue(inventory.consumeRecipe(Map.of(
                Inventory.Item.WOOD, 8,
                Inventory.Item.STONE, 4)));
        assertEquals(2, inventory.get(Inventory.Item.WOOD));
        assertEquals(3, inventory.get(Inventory.Item.STONE));
    }
}
