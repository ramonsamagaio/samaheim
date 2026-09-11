package com.samaheim.game;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FirstHourRulesTest {
    @Test void movementStopsAtSolidObstacle() {
        var from = new FirstHourRules.Vec2(0f, 0f);
        var obstacle = new FirstHourRules.CircleObstacle(new FirstHourRules.Vec2(2f, 0f), 0.8f);
        var result = FirstHourRules.resolveMovement(from, new FirstHourRules.Vec2(2f, 0f), 0.45f, 100f, List.of(obstacle));
        assertEquals(from, result);
    }

    @Test void movementSlidesAlongObstacleWhenOneAxisIsFree() {
        var from = new FirstHourRules.Vec2(0f, 0f);
        var obstacle = new FirstHourRules.CircleObstacle(new FirstHourRules.Vec2(1f, 0f), 0.7f);
        var result = FirstHourRules.resolveMovement(from, new FirstHourRules.Vec2(1f, 1f), 0.45f, 100f, List.of(obstacle));
        assertEquals(0f, result.x(), 0.001f);
        assertEquals(1f, result.z(), 0.001f);
    }

    @Test void worldBoundsClampMovement() {
        var result = FirstHourRules.resolveMovement(new FirstHourRules.Vec2(9f, 9f), new FirstHourRules.Vec2(5f, 5f), 0.5f, 10f, List.of());
        assertEquals(9.5f, result.x(), 0.001f);
        assertEquals(9.5f, result.z(), 0.001f);
    }

    @Test void steepSlopesBecomeImpassable() {
        assertEquals(0f, FirstHourRules.slopeSpeedMultiplier(1.4f));
        assertTrue(FirstHourRules.slopeSpeedMultiplier(0.5f) > 0f);
    }

    @Test void weaponTierProgressionIncreasesDamageAndCost() {
        assertTrue(FirstHourRules.weaponDamage(FirstHourRules.Weapon.IRON_SWORD) > FirstHourRules.weaponDamage(FirstHourRules.Weapon.WANDERER_BLADE));
        assertTrue(FirstHourRules.staminaCost(FirstHourRules.Weapon.IRON_SWORD) > FirstHourRules.staminaCost(FirstHourRules.Weapon.WANDERER_BLADE));
    }

    @Test void armorHasDiminishingReturnsAndCannotNullifyHits() {
        float light = FirstHourRules.applyArmor(30f, 20f);
        float heavy = FirstHourRules.applyArmor(30f, 200f);
        assertTrue(heavy < light);
        assertTrue(heavy >= 1f);
    }

    @Test void biomeGenerationIsDeterministic() {
        assertEquals(FirstHourRules.biomeAt(1234L, 50f, -20f), FirstHourRules.biomeAt(1234L, 50f, -20f));
    }

    @Test void biomeEncountersAreDeterministicAndScale() {
        var low = FirstHourRules.encounterFor(FirstHourRules.Biome.GREENWOOD, 0, 9L);
        var high = FirstHourRules.encounterFor(FirstHourRules.Biome.GREENWOOD, 4, 9L);
        assertEquals(1, low.size());
        assertTrue(high.size() > low.size());
        assertEquals(high, FirstHourRules.encounterFor(FirstHourRules.Biome.GREENWOOD, 4, 9L));
    }

    @Test void renewableResourcesHaveBoundedRespawnTimes() {
        assertTrue(FirstHourRules.resourceRespawnSeconds("BERRY") < FirstHourRules.resourceRespawnSeconds("TREE"));
        assertTrue(FirstHourRules.resourceRespawnSeconds("TREE") <= 900f);
    }

    @Test void structuresRejectSteepTerrainAndBlockers() {
        var p = new FirstHourRules.Vec2(5f, 5f);
        assertFalse(FirstHourRules.canPlaceStructure(p, 1f, 100f, List.of(), 0.8f));
        assertFalse(FirstHourRules.canPlaceStructure(p, 1f, 100f,
                List.of(new FirstHourRules.CircleObstacle(new FirstHourRules.Vec2(5.5f, 5f), 1f)), 0.1f));
        assertTrue(FirstHourRules.canPlaceStructure(p, 1f, 100f, List.of(), 0.1f));
    }

    @Test void dungeonRequiresAllRoomsAndBoss() {
        assertFalse(FirstHourRules.dungeonComplete(5, 5, false));
        assertFalse(FirstHourRules.dungeonComplete(4, 5, true));
        assertTrue(FirstHourRules.dungeonComplete(5, 5, true));
    }

    @Test void dangerEscalatesAcrossFirstHour() {
        assertEquals(0, FirstHourRules.dangerTier(0f, 0));
        assertEquals(2, FirstHourRules.dangerTier(31f, 0));
        assertEquals(4, FirstHourRules.dangerTier(60f, 3));
    }

    @Test void campCreatesSafetyBubbleAndNightTightensEncounterCadence() {
        assertFalse(FirstHourRules.shouldSpawnEncounter(0, 100f, 5f, true));
        assertFalse(FirstHourRules.shouldSpawnEncounter(7, 100f, 20f, true));
        assertTrue(FirstHourRules.shouldSpawnEncounter(0, 40f, 20f, true));
        assertFalse(FirstHourRules.shouldSpawnEncounter(0, 40f, 20f, false));
    }
}
