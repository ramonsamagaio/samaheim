package com.samaheim.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TerraformFeedbackTest {
    @Test void successfulRaiseReportsStoneAndStaminaCost(){
        TerrainState terrain=new TerrainState(8L,32f,64,8f);
        var result=TerraformToolSystem.apply(terrain,TerraformToolSystem.Mode.RAISE,0f,0f,terrain.sampleHeight(0f,0f),2.8f,20,100f);
        assertTrue(result.applied());
        assertTrue(result.message().contains("stone"));
        assertTrue(result.message().contains("stamina"));
    }
    @Test void digReportsStaminaWithoutPretendingToSpendStone(){
        TerrainState terrain=new TerrainState(8L,32f,64,8f);
        var result=TerraformToolSystem.apply(terrain,TerraformToolSystem.Mode.LOWER,0f,0f,terrain.sampleHeight(0f,0f),2.8f,0,100f);
        assertTrue(result.applied());
        assertFalse(result.message().contains("stone"));
        assertTrue(result.message().contains("stamina"));
    }
}
