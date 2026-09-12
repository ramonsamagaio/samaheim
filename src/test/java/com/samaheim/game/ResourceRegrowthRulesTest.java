package com.samaheim.game;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ResourceRegrowthRulesTest {
    @Test
    void dawnCrossingIsDetectedWithoutTriggeringDuringNormalDayMotion() {
        assertTrue(ResourceRegrowthRules.crossedDawn(0.99f, 0.01f));
        assertFalse(ResourceRegrowthRules.crossedDawn(0.20f, 0.21f));
        assertFalse(ResourceRegrowthRules.crossedDawn(0.50f, 0.51f));
    }

    @Test
    void onlyActualHarvestNodesAreEligible() {
        assertTrue(ResourceRegrowthRules.isHarvestedResourceId("tree-12"));
        assertTrue(ResourceRegrowthRules.isHarvestedResourceId("rock-4"));
        assertTrue(ResourceRegrowthRules.isHarvestedResourceId("berry-8"));
        assertFalse(ResourceRegrowthRules.isHarvestedResourceId("__dungeon_cleared__"));
        assertFalse(ResourceRegrowthRules.isHarvestedResourceId("frontier-poi-1"));
    }

    @Test
    void regrowthRestoresAPartialButUsefulShare() {
        assertEquals(0, ResourceRegrowthRules.regrowthCount(0));
        assertEquals(1, ResourceRegrowthRules.regrowthCount(1));
        assertEquals(2, ResourceRegrowthRules.regrowthCount(4));
        assertEquals(5, ResourceRegrowthRules.regrowthCount(20));
    }

    @Test
    void selectionIsDeterministicAndNeverTouchesSpecialFlags() {
        Set<String> removed = new HashSet<>(List.of(
                "tree-1", "tree-2", "rock-4", "berry-8", "berry-9",
                "__dungeon_looted__", "__rested_at_campfire__"));
        List<String> a = ResourceRegrowthRules.selectForRegrowth(removed, 991L, 3L);
        List<String> b = ResourceRegrowthRules.selectForRegrowth(removed, 991L, 3L);
        assertEquals(a, b);
        assertEquals(ResourceRegrowthRules.regrowthCount(5), a.size());
        assertTrue(a.stream().allMatch(ResourceRegrowthRules::isHarvestedResourceId));
    }

    @Test
    void dawnIndexChangesWhichResourcesReturn() {
        Set<String> removed = new HashSet<>();
        for (int i = 0; i < 30; i++) removed.add("tree-" + i);
        assertFalse(ResourceRegrowthRules.selectForRegrowth(removed, 1234L, 1L)
                .equals(ResourceRegrowthRules.selectForRegrowth(removed, 1234L, 2L)));
    }
}
