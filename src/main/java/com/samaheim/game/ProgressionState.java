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
    private boolean suppliesGathered;
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
        suppliesGathered |= inventory.has(Inventory.Item.WOOD, 8) && inventory.has(Inventory.Item.STONE, 4);
        advanceSatisfiedObjectives();
    }

    public void markBladeCrafted() {
        bladeCrafted = true;
        advanceSatisfiedObjectives();
    }

    public void markGoblinDefeated() {
        defeatedGoblins++;
        advanceSatisfiedObjectives();
    }

    public void markArcaneSealFound() {
        arcaneSeals = Math.min(3, arcaneSeals + 1);
        advanceSatisfiedObjectives();
    }

    public void markCampBuilt() {
        campBuilt = true;
        advanceSatisfiedObjectives();
    }

    private void advanceSatisfiedObjectives() {
        boolean advanced;
        do {
            advanced = switch (stage) {
                case GATHER_SUPPLIES -> advanceWhen(suppliesGathered, Stage.CRAFT_BLADE);
                case CRAFT_BLADE -> advanceWhen(bladeCrafted, Stage.DEFEAT_GOBLINS);
                case DEFEAT_GOBLINS -> advanceWhen(defeatedGoblins >= 3, Stage.FIND_ARCANE_SEALS);
                case FIND_ARCANE_SEALS -> advanceWhen(arcaneSeals >= 3, Stage.BUILD_CAMP);
                case BUILD_CAMP -> advanceWhen(campBuilt, Stage.SURVIVE_NIGHT);
                case SURVIVE_NIGHT -> false;
            };
        } while (advanced);
    }

    private boolean advanceWhen(boolean condition, Stage next) {
        if (!condition) {
            return false;
        }
        stage = next;
        return true;
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
        this.suppliesGathered = stage != Stage.GATHER_SUPPLIES;
        this.defeatedGoblins = Math.max(0, defeatedGoblins);
        this.arcaneSeals = Math.clamp(arcaneSeals, 0, 3);
        this.bladeCrafted = bladeCrafted;
        this.campBuilt = campBuilt;
        advanceSatisfiedObjectives();
    }
}
