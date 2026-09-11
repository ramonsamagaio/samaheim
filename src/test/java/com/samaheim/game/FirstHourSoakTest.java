package com.samaheim.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FirstHourSoakTest {
    @Test void sixtyMinuteModelMaintainsVarietyWithoutEncounterFlood() {
        FirstHourSoak.Result result = FirstHourSoak.simulate(9918273L);
        assertTrue(result.encounters() >= 30, "too few encounters for an hour");
        assertTrue(result.encounters() <= 100, "encounter flood");
        assertTrue(result.nightEncounters() > 0);
        assertTrue(result.biomeChanges() >= 8, "path should cross several biome cells");
        assertTrue(result.resourceRespawns() >= 10);
        assertEquals(4, result.maxDangerTier());
        assertEquals(5, result.milestones().size());
    }

    @Test void simulationIsDeterministicForSameSeed() {
        assertEquals(FirstHourSoak.simulate(42L), FirstHourSoak.simulate(42L));
    }
}
