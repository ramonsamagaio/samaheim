package com.samaheim.game;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class DungeonRulesTest {
    @Test
    void carveLayoutFormsOverlappingWalkableChain() {
        List<Vector3f> centers = DungeonRules.carveCenters();
        assertTrue(centers.size() >= 6);
        for (int i = 1; i < centers.size(); i++) {
            assertTrue(centers.get(i).distance(centers.get(i - 1)) < DungeonRules.CARVE_RADIUS * 1.5f);
        }
    }

    @Test
    void chestStaysLockedUntilEncounterIsCleared() {
        assertFalse(DungeonRules.chestUnlocked(3, false));
        assertFalse(DungeonRules.chestUnlocked(1, false));
        assertTrue(DungeonRules.chestUnlocked(0, false));
        assertTrue(DungeonRules.chestUnlocked(4, true));
    }

    @Test
    void dungeonRewardAdvancesEveryFrontierMaterial() {
        DungeonRules.Reward reward = DungeonRules.reward();
        assertTrue(reward.ironOre() > 0);
        assertTrue(reward.mistResin() > 0);
        assertTrue(reward.cinderShard() > 0);
        assertTrue(reward.wood() > 0);
        assertTrue(reward.stone() > 0);
    }
}
