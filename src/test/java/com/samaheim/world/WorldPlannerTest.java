package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class WorldPlannerTest {
    @Test void planContainsRequiredFirstHourLandmarks() {
        var plan = WorldPlanner.plan(1337L, 116f);
        assertEquals(9, plan.size());
        assertEquals(3, plan.stream().filter(p -> p.type() == WorldPlanner.PoiType.SHRINE).count());
        assertEquals(1, plan.stream().filter(p -> p.type() == WorldPlanner.PoiType.DUNGEON).count());
        assertEquals(2, plan.stream().filter(p -> p.type() == WorldPlanner.PoiType.IRON_CAMP).count());
    }

    @Test void spawnRemainsClearOfProgressionLandmarks() {
        assertTrue(WorldPlanner.spawnIsSafe(WorldPlanner.plan(42L, 116f), 20f));
    }

    @Test void planIsDeterministicAndIdsStayUnique() {
        var first = WorldPlanner.plan(7L, 116f);
        var second = WorldPlanner.plan(7L, 116f);
        assertEquals(first, second);
        var ids = new HashSet<String>();
        first.forEach(p -> assertTrue(ids.add(p.id())));
    }

    @Test void tinyWorldIsRejectedInsteadOfProducingBrokenLayout() {
        assertThrows(IllegalArgumentException.class, () -> WorldPlanner.plan(1L, 50f));
    }
}
