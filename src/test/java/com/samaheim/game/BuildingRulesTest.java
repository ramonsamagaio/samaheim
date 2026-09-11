package com.samaheim.game;

import com.samaheim.world.WorldRules;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildingRulesTest {
    @Test
    void rejectsBlockedAndDistantPlacement() {
        WorldRules.Point point = new WorldRules.Point(8f, 8f);
        List<WorldRules.Obstacle> blockers = List.of(
                new WorldRules.Obstacle("rock", point, 0.8f));
        assertFalse(BuildingRules.validate(BuildingRules.Piece.CAMPFIRE, point, 42L, 100f, blockers, 2f).allowed());
        assertFalse(BuildingRules.validate(BuildingRules.Piece.CAMPFIRE,
                new WorldRules.Point(14f, 14f), 42L, 100f, List.of(), 8f).allowed());
    }

    @Test
    void campAndBedEnableRespawnOnlyWhenCloseEnough() {
        assertTrue(BuildingRules.supportsRespawn(true, true, 5f));
        assertFalse(BuildingRules.supportsRespawn(true, false, 5f));
        assertFalse(BuildingRules.supportsRespawn(true, true, 12f));
    }
}
