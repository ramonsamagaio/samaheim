package com.samaheim.game;

import java.util.Objects;

public final class ProgressionState {
    public enum Stage {
        GATHER_SUPPLIES,
        CRAFT_BLADE,
        DEFEAT_GOBLINS,
        FIND_ARCANE_SEALS,
        BUILD_CAMP,
        SURVIVE_NIGHT
    }

    private Stage stage = Stage.GATHER_SUPPLIES;
    private int defeatedGoblins;
    private int arcaneSeals;
    private boolean bladeCrafted;
    private boolean campBuilt;

    public Stage stage() {
        return stage;
    }

    public int defeatedGoblins() {
        return defeatedGoblins;
    }

    public int arcaneSeals() {
        return arcaneSeals;
    }

    public boolean bladeCrafted() {
        return bladeCrafted;
    }

    public boolean campBuilt() {
        return campBuilt;
    }

    public void updateFromInventory(Inventory inventory) {
        Objects.requireNonNull(inventory, "inventory");
        if (stage == Stage.GATHER_SUPPLIES
                && inventory.has(Inventory.Item.WOOD, 8)
                && inventory.has(Inventory.Item.STONE, 4)) {
            stage = Stage.CRAFT_BLADE;
        }
    }

    public void markBladeCrafted() {
        bladeCrafted = true;
        if (stage == Stage.CRAFT_BLADE) {
            stage = Stage.DEFEAT_GOBLINS;
        }
    }

    public void markGoblinDefeated() {
        defeatedGoblins++;
        if (stage == Stage.DEFEAT_GOBLINS && defeatedGoblins >= 3) {
            stage = Stage.FIND_ARCANE_SEALS;
        }
    }

    public void markArcaneSealFound() {
        arcaneSeals = Math.min(3, arcaneSeals + 1);
        if (stage == Stage.FIND_ARCANE_SEALS && arcaneSeals >= 3) {
            stage = Stage.BUILD_CAMP;
        }
    }

    public void markCampBuilt() {
        campBuilt = true;
        if (stage == Stage.BUILD_CAMP) {
            stage = Stage.SURVIVE_NIGHT;
        }
    }

    public String objectiveText() {
        return switch (stage) {
            case GATHER_SUPPLIES -> "Gather 8 wood and 4 stone";
            case CRAFT_BLADE -> "Craft a Wanderer's Blade [1]";
            case DEFEAT_GOBLINS -> "Defeat 3 goblin scouts (" + defeatedGoblins + "/3)";
            case FIND_ARCANE_SEALS -> "Find the three Arcane Seals (" + arcaneSeals + "/3)";
            case BUILD_CAMP -> "Build a campfire [2], then place it [Q]";
            case SURVIVE_NIGHT -> "Survive, explore and grow stronger";
        };
    }

    public void restore(Stage stage, int defeatedGoblins, int arcaneSeals, boolean bladeCrafted, boolean campBuilt) {
        this.stage = Objects.requireNonNull(stage, "stage");
        this.defeatedGoblins = Math.max(0, defeatedGoblins);
        this.arcaneSeals = Math.clamp(arcaneSeals, 0, 3);
        this.bladeCrafted = bladeCrafted;
        this.campBuilt = campBuilt;
    }
}
