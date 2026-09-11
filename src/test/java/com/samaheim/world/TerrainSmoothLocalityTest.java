package com.samaheim.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TerrainSmoothLocalityTest {
    @Test void smoothingLeavesDistantEditedTerrainUntouched(){
        TerrainState terrain=new TerrainState(123L,64f,128,8f);
        terrain.raise(0f,0f,1.2f,2f);
        terrain.raise(35f,35f,1.2f,2f);
        float farBefore=terrain.sampleHeight(35f,35f);
        terrain.consumeDirtyRegion();
        terrain.smooth(0f,0f,2.8f,0.4f);
        assertEquals(farBefore,terrain.sampleHeight(35f,35f),0.0001f);
        TerrainState.DirtyRegion dirty=terrain.consumeDirtyRegion();
        assertNotNull(dirty);
        assertTrue(dirty.sampleCount()<400,"smooth should dirty only a local patch");
    }
}
