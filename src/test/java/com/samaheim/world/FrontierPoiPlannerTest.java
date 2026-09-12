package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class FrontierPoiPlannerTest {
    @Test
    void planningIsDeterministicAndLivesOutsideLegacyCore() {
        List<FrontierPoiPlanner.Poi> a = FrontierPoiPlanner.plan(424242L, 192f);
        List<FrontierPoiPlanner.Poi> b = FrontierPoiPlanner.plan(424242L, 192f);
        assertEquals(a, b);
        assertTrue(a.size() >= 8);
        for (FrontierPoiPlanner.Poi poi : a) {
            assertTrue(Math.max(Math.abs(poi.x()), Math.abs(poi.z())) > 100f);
        }
    }

    @Test
    void themedPoisMatchTheirRegions() {
        List<FrontierPoiPlanner.Poi> pois = FrontierPoiPlanner.plan(987654321L, 192f);
        for (FrontierPoiPlanner.Poi poi : pois) {
            if (poi.type() == FrontierPoiPlanner.PoiType.FORGE_RUIN) assertEquals(WorldMath.Region.IRON_HIGHLANDS, poi.region());
            if (poi.type() == FrontierPoiPlanner.PoiType.FEN_ALTAR) assertEquals(WorldMath.Region.MISTFEN, poi.region());
            if (poi.type() == FrontierPoiPlanner.PoiType.CINDER_SHRINE) assertEquals(WorldMath.Region.ASHEN_REACH, poi.region());
        }
    }

    @Test
    void sitesHaveUsefulDistinctRewardsAndSpacing() {
        List<FrontierPoiPlanner.Poi> pois = FrontierPoiPlanner.plan(1337L, 192f);
        assertFalse(pois.isEmpty());
        assertTrue(FrontierPoiPlanner.reward(FrontierPoiPlanner.PoiType.FORGE_RUIN).ironOre() > 0);
        assertTrue(FrontierPoiPlanner.reward(FrontierPoiPlanner.PoiType.FEN_ALTAR).mistResin() > 0);
        assertTrue(FrontierPoiPlanner.reward(FrontierPoiPlanner.PoiType.CINDER_SHRINE).cinderShard() > 0);
        for (int i = 0; i < pois.size(); i++) for (int j = i + 1; j < pois.size(); j++) {
            float dx = pois.get(i).x() - pois.get(j).x();
            float dz = pois.get(i).z() - pois.get(j).z();
            assertTrue(dx * dx + dz * dz >= 24f * 24f - 0.1f);
        }
    }
}
