package com.samaheim.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class NightCampRulesTest {
    @Test
    void nightOutsideCampIsMoreDangerousThanDay() {
        float day = NightCampRules.roamingSpawnInterval(false, false);
        float exposedNight = NightCampRules.roamingSpawnInterval(true, false);
        assertTrue(exposedNight < day);
        assertTrue(NightCampRules.hungerDrainMultiplier(true, false) > 1f);
    }

    @Test
    void campfireCreatesARealSafetyAndRecoveryBubble() {
        assertTrue(NightCampRules.warmedByCampfire(7.9f));
        assertFalse(NightCampRules.warmedByCampfire(8.1f));
        assertTrue(NightCampRules.roamingSpawnInterval(true, true)
                > NightCampRules.roamingSpawnInterval(true, false));
        assertTrue(NightCampRules.staminaRecoveryBonus(true, true) > 0f);
        assertTrue(NightCampRules.hungerDrainMultiplier(true, true) < 1f);
    }

    @Test
    void restingRequiresNightWarmthAndNoNearbyThreat() {
        assertTrue(NightCampRules.canRest(true, true, false));
        assertFalse(NightCampRules.canRest(false, true, false));
        assertFalse(NightCampRules.canRest(true, false, false));
        assertFalse(NightCampRules.canRest(true, true, true));
    }

    @Test
    void restSkipsToMorningAndRecoversWithoutCreatingFreeFood() {
        NightCampRules.RestResult result = NightCampRules.rest(45f, 12f, 60f);
        assertEquals(NightCampRules.MORNING_CLOCK, result.dayClock(), 0.0001f);
        assertEquals(75f, result.health(), 0.0001f);
        assertEquals(100f, result.stamina(), 0.0001f);
        assertEquals(52f, result.hunger(), 0.0001f);
    }

    @Test
    void dayClockWrapsForNightDetection() {
        assertTrue(NightCampRules.isNight(0.75f));
        assertTrue(NightCampRules.isNight(1.01f));
        assertFalse(NightCampRules.isNight(0.18f));
    }
}
