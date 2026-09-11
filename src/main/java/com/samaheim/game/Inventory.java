package com.samaheim.game;

import java.util.EnumMap;
import java.util.Map;

public final class Inventory {
    public enum Item {
        WOOD,
        STONE,
        BERRY,
        ARCANE_DUST
    }

    private final EnumMap<Item, Integer> quantities = new EnumMap<>(Item.class);

    public Inventory() {
        for (Item item : Item.values()) {
            quantities.put(item, 0);
        }
    }

    public int get(Item item) {
        return quantities.get(item);
    }

    public void add(Item item, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        quantities.merge(item, amount, Integer::sum);
    }

    public boolean has(Item item, int amount) {
        return get(item) >= amount;
    }

    public boolean consume(Item item, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        if (!has(item, amount)) {
            return false;
        }
        quantities.put(item, get(item) - amount);
        return true;
    }

    public boolean consumeRecipe(Map<Item, Integer> recipe) {
        for (Map.Entry<Item, Integer> entry : recipe.entrySet()) {
            if (!has(entry.getKey(), entry.getValue())) {
                return false;
            }
        }
        for (Map.Entry<Item, Integer> entry : recipe.entrySet()) {
            consume(entry.getKey(), entry.getValue());
        }
        return true;
    }

    public Map<Item, Integer> snapshot() {
        return Map.copyOf(quantities);
    }

    public void set(Item item, int amount) {
        if (amount < 0) {
            throw new IllegalArgumentException("amount must be >= 0");
        }
        quantities.put(item, amount);
    }
}
