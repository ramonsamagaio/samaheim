package com.samaheim.world;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class SandboxGroundSnapTest {
    @Test void smallDownhillStepStaysGrounded(){
        float eye=1.72f;
        var state=SandboxPhysics.stepVertical(eye,0f,-0.18f,eye,false,1f/60f);
        assertTrue(state.grounded());
        assertEquals(1.54f,state.eyeY(),0.001f);
    }

    @Test void realLedgeStillCausesFall(){
        float eye=1.72f;
        var state=SandboxPhysics.stepVertical(eye,0f,-0.8f,eye,false,1f/60f);
        assertFalse(state.grounded());
        assertTrue(state.eyeY()>0.92f);
    }
}
