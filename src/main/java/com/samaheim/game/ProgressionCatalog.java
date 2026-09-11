package com.samaheim.game;

import java.util.EnumMap;
import java.util.Map;

public final class ProgressionCatalog {
    private ProgressionCatalog() {}

    public enum Material { WOOD, STONE, IRON, ARCANE_DUST, HIDE, BERRY }
    public enum Recipe { WANDERER_BLADE, CAMPFIRE, WORKBENCH, IRON_SWORD, LEATHER_ARMOR, ARCANE_STAFF }

    private static final Map<Recipe, Map<Material, Integer>> COSTS = new EnumMap<>(Recipe.class);
    static {
        COSTS.put(Recipe.WANDERER_BLADE, cost(Material.WOOD, 8, Material.STONE, 4));
        COSTS.put(Recipe.CAMPFIRE, cost(Material.WOOD, 5, Material.STONE, 3));
        COSTS.put(Recipe.WORKBENCH, cost(Material.WOOD, 12, Material.STONE, 6));
        COSTS.put(Recipe.IRON_SWORD, cost(Material.WOOD, 4, Material.IRON, 10));
        COSTS.put(Recipe.LEATHER_ARMOR, cost(Material.HIDE, 12, Material.IRON, 2));
        COSTS.put(Recipe.ARCANE_STAFF, cost(Material.WOOD, 6, Material.ARCANE_DUST, 14, Material.IRON, 3));
    }

    public static Map<Material, Integer> cost(Recipe recipe) { return COSTS.get(recipe); }

    public static boolean canCraft(Recipe recipe, Map<Material, Integer> inventory, boolean hasWorkbench) {
        if ((recipe == Recipe.IRON_SWORD || recipe == Recipe.LEATHER_ARMOR || recipe == Recipe.ARCANE_STAFF) && !hasWorkbench) return false;
        for (var entry : cost(recipe).entrySet()) {
            if (inventory.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }

    public static int rewardTierForDungeon(int roomsCleared, boolean bossDefeated) {
        if (!bossDefeated) return 0;
        if (roomsCleared >= 9) return 3;
        if (roomsCleared >= 6) return 2;
        if (roomsCleared >= 3) return 1;
        return 0;
    }

    private static Map<Material, Integer> cost(Object... parts) {
        EnumMap<Material, Integer> result = new EnumMap<>(Material.class);
        for (int i = 0; i < parts.length; i += 2) result.put((Material) parts[i], (Integer) parts[i + 1]);
        return Map.copyOf(result);
    }
}
