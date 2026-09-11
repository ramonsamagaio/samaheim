package com.samaheim.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TerrainRestoreEffortTest {
    @Test void untouchedGroundRefusesRestoreWithoutStaminaSpend(){
        TerrainState terrain=new TerrainState(88L,32f,64,8f);
        var r=TerraformToolSystem.apply(terrain,TerraformToolSystem.Mode.RESTORE,0f,0f,terrain.sampleHeight(0f,0f),2.8f,0,100f);
        assertFalse(r.applied());
        assertEquals(0f,r.staminaSpent(),0.0001f);
    }
    @Test void heavierEditsRequireAtLeastAsMuchRestoreStamina(){
        TerrainState light=new TerrainState(88L,32f,64,8f); light.raise(0f,0f,2f,0.6f);
        TerrainState heavy=new TerrainState(88L,32f,64,8f); heavy.raise(0f,0f,2f,3.2f);
        var lr=TerraformToolSystem.apply(light,TerraformToolSystem.Mode.RESTORE,0f,0f,light.sampleHeight(0f,0f),2.8f,0,100f);
        var hr=TerraformToolSystem.apply(heavy,TerraformToolSystem.Mode.RESTORE,0f,0f,heavy.sampleHeight(0f,0f),2.8f,0,100f);
        assertTrue(lr.applied()&&hr.applied());
        assertTrue(hr.staminaSpent()>=lr.staminaSpent());
    }
}
