package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class WaterPhysicsTest {
    @Test
    void shallowWaterWadesBeforeItBecomesSwimmingDepth() {
        assertFalse(WaterPhysics.isWading(0f, 0.10f));
        assertTrue(WaterPhysics.isWading(0f, 0.55f));
        assertFalse(WaterPhysics.isSwimming(0f, 0.55f));
        assertTrue(WaterPhysics.isSwimming(0f, 1.20f));
    }

    @Test
    void deepWaterSlowsHorizontalMovementButSprintStillMatters() {
        float walk = WaterPhysics.horizontalSpeed(true, false, false, 100f);
        float push = WaterPhysics.horizontalSpeed(true, false, true, 100f);
        float land = WaterPhysics.horizontalSpeed(false, false, false, 100f);

        assertTrue(walk < land);
        assertTrue(push > walk);
        assertTrue(push < 7.1f);
    }

    @Test
    void buoyancyPullsPlayerTowardSurfaceAndControlsCanOverrideIt() {
        float rising = WaterPhysics.nextSwimVelocity(-3f, 0f, -1f, false, false, 0.1f);
        float ascending = WaterPhysics.nextSwimVelocity(-1.2f, 0f, 0f, true, false, 0.1f);
        float descending = WaterPhysics.nextSwimVelocity(-1.2f, 0f, 0f, false, true, 0.1f);

        assertTrue(rising > -1f);
        assertTrue(ascending > 0f);
        assertTrue(descending < 0f);
    }

    @Test
    void swimmingConsumesStaminaAndAirRecoversOutOfWater() {
        float swim = WaterPhysics.nextStamina(80f, true, true, false, 1f);
        float sprintSwim = WaterPhysics.nextStamina(80f, true, true, true, 1f);
        float rested = WaterPhysics.nextStamina(80f, false, false, false, 1f);
        float drowned = WaterPhysics.nextBreath(50f, true, 2f);
        float recovered = WaterPhysics.nextBreath(50f, false, 2f);

        assertTrue(swim < 80f);
        assertTrue(sprintSwim < swim);
        assertTrue(rested > 80f);
        assertTrue(drowned < 50f);
        assertTrue(recovered > 50f);
    }
}
