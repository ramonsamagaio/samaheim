package com.samaheim.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class TerraformBrushDepthTest {
    @Test void widerRaiseMovesMoreAreaButLessCenterHeightPerStrike(){
        TerrainState narrow=new TerrainState(333L,40f,80,8f);
        TerrainState wide=new TerrainState(333L,40f,80,8f);
        float before=narrow.sampleHeight(0f,0f);
        TerraformToolSystem.Result nr=TerraformToolSystem.apply(narrow,TerraformToolSystem.Mode.RAISE,0f,0f,before,1.6f,30,100f);
        TerraformToolSystem.Result wr=TerraformToolSystem.apply(wide,TerraformToolSystem.Mode.RAISE,0f,0f,before,4.2f,30,100f);
        assertTrue(nr.applied()&&wr.applied());
        assertTrue(wr.changedSamples()>nr.changedSamples());
        float narrowRise=narrow.sampleHeight(0f,0f)-before;
        float wideRise=wide.sampleHeight(0f,0f)-before;
        assertTrue(wideRise<narrowRise,"wide brushes should spread material instead of making a taller spike per click");
    }
}
