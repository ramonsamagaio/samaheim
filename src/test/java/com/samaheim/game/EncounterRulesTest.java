package com.samaheim.game;

import com.samaheim.world.WorldMath;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class EncounterRulesTest {
    @Test
    void frontierRegionsHaveDistinctSignatureEnemies() {
        assertEquals(EncounterRules.EnemyKind.HIGHLAND_BRUTE,
                EncounterRules.forRegion(WorldMath.Region.IRON_HIGHLANDS, 0.1f));
        assertEquals(EncounterRules.EnemyKind.MIRE_STALKER,
                EncounterRules.forRegion(WorldMath.Region.MISTFEN, 0.1f));
        assertEquals(EncounterRules.EnemyKind.ASH_WRAITH,
                EncounterRules.forRegion(WorldMath.Region.ASHEN_REACH, 0.1f));
    }

    @Test
    void greenmarchKeepsTheStarterRoster() {
        assertEquals(EncounterRules.EnemyKind.GOBLIN,
                EncounterRules.forRegion(WorldMath.Region.GREENMARCH, 0.2f));
        assertEquals(EncounterRules.EnemyKind.GRAVEBORN,
                EncounterRules.forRegion(WorldMath.Region.GREENMARCH, 0.9f));
    }

    @Test
    void delveSamplesSeveralCombatProfiles() {
        var guardians = EncounterRules.dungeonGuardians();
        assertEquals(4, guardians.size());
        assertTrue(new HashSet<>(guardians).size() >= 4);
        assertTrue(guardians.contains(EncounterRules.EnemyKind.HIGHLAND_BRUTE));
        assertTrue(guardians.contains(EncounterRules.EnemyKind.MIRE_STALKER));
        assertTrue(guardians.contains(EncounterRules.EnemyKind.ASH_WRAITH));
    }
}
