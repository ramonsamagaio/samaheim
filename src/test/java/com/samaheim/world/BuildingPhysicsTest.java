package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BuildingPhysicsTest {
    @Test
    void gridSnapUsesHalfMeterSteps() {
        assertEquals(2.5f, BuildingPhysics.snap(2.31f), 0.0001f);
        assertEquals(-1.5f, BuildingPhysics.snap(-1.49f), 0.0001f);
    }

    @Test
    void yawSnapsToQuarterTurns() {
        assertEquals(Math.PI / 2.0, BuildingPhysics.snapYaw(1.4f), 0.0001);
        assertEquals(Math.PI, BuildingPhysics.snapYaw(3.0f), 0.0001);
    }

    @Test
    void floorRaisesSupportWhenStepIsReachable() {
        var floor = new BuildingPhysics.Floor(0f, 0f, 0f, 0f);
        assertEquals(0.24f, BuildingPhysics.supportHeight(0f, 0f, 0f, 0f, List.of(floor)), 0.0001f);
    }

    @Test
    void highFloorDoesNotTeleportActorUpward() {
        var floor = new BuildingPhysics.Floor(0f, 2f, 0f, 0f);
        assertEquals(0f, BuildingPhysics.supportHeight(0f, 0f, 0f, 0f, List.of(floor)), 0.0001f);
    }

    @Test
    void actorOutsideFloorUsesTerrain() {
        var floor = new BuildingPhysics.Floor(0f, 0f, 0f, 0f);
        assertEquals(0.1f, BuildingPhysics.supportHeight(3f, 0f, 0.1f, 0.1f, List.of(floor)), 0.0001f);
    }

    @Test
    void overlapDetectionOnlyRejectsSameGridSlot() {
        var floors = List.of(new BuildingPhysics.Floor(1f, 0f, 1f, 0f));
        assertTrue(BuildingPhysics.floorsOverlap(1.1f, 1.1f, floors));
        assertFalse(BuildingPhysics.floorsOverlap(2f, 2f, floors));
    }
}
