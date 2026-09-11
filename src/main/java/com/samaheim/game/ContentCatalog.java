package com.samaheim.game;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class ContentCatalog {
    public enum Resource {
        WOOD,
        STONE,
        BERRY,
        ARCANE_DUST,
        HIDE,
        IRON_ORE,
        IRON_INGOT,
        MIRE_REED,
        ASH_CRYSTAL
    }

    public enum Recipe {
        WANDERER_BLADE,
        CAMPFIRE,
        WORKBENCH,
        HIDE_ARMOR,
        IRON_LONGSWORD,
        ARCANE_BLADE,
        BEDROLL,
        STORAGE_CHEST
    }

    private static final Map<Recipe, Map<Resource, Integer>> COSTS = createCosts();

    private ContentCatalog() {
    }

    public static Map<Resource, Integer> cost(Recipe recipe) {
        Objects.requireNonNull(recipe, "recipe");
        return COSTS.get(recipe);
    }

    public static boolean canCraft(Map<Resource, Integer> inventory, Recipe recipe) {
        Objects.requireNonNull(inventory, "inventory");
        for (Map.Entry<Resource, Integer> entry : cost(recipe).entrySet()) {
            if (inventory.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    public static boolean consume(Map<Resource, Integer> inventory, Recipe recipe) {
        if (!canCraft(inventory, recipe)) {
            return false;
        }
        for (Map.Entry<Resource, Integer> entry : cost(recipe).entrySet()) {
            inventory.put(entry.getKey(), inventory.getOrDefault(entry.getKey(), 0) - entry.getValue());
        }
        return true;
    }

    public static long respawnSeconds(Resource resource) {
        return switch (resource) {
            case BERRY, MIRE_REED -> 600L;
            case WOOD -> 1_800L;
            case STONE -> 2_400L;
            case IRON_ORE, ASH_CRYSTAL -> 3_600L;
            case ARCANE_DUST, HIDE, IRON_INGOT -> 0L;
        };
    }

    private static Map<Recipe, Map<Resource, Integer>> createCosts() {
        EnumMap<Recipe, Map<Resource, Integer>> result = new EnumMap<>(Recipe.class);
        result.put(Recipe.WANDERER_BLADE, recipe(Resource.WOOD, 8, Resource.STONE, 4));
        result.put(Recipe.CAMPFIRE, recipe(Resource.WOOD, 5, Resource.STONE, 3));
        result.put(Recipe.WORKBENCH, recipe(Resource.WOOD, 12, Resource.STONE, 5));
        result.put(Recipe.HIDE_ARMOR, recipe(Resource.HIDE, 8, Resource.WOOD, 4));
        result.put(Recipe.IRON_LONGSWORD, recipe(Resource.IRON_INGOT, 6, Resource.WOOD, 3));
        result.put(Recipe.ARCANE_BLADE, recipe(Resource.IRON_INGOT, 4, Resource.ARCANE_DUST, 12,
                Resource.ASH_CRYSTAL, 2));
        result.put(Recipe.BEDROLL, recipe(Resource.HIDE, 4, Resource.WOOD, 3));
        result.put(Recipe.STORAGE_CHEST, recipe(Resource.WOOD, 10, Resource.IRON_INGOT, 1));
        return Collections.unmodifiableMap(result);
    }

    private static Map<Resource, Integer> recipe(Object... pairs) {
        EnumMap<Resource, Integer> map = new EnumMap<>(Resource.class);
        for (int i = 0; i < pairs.length; i += 2) {
            map.put((Resource) pairs[i], (Integer) pairs[i + 1]);
        }
        return Collections.unmodifiableMap(map);
    }
}
