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

    @Test
    void rampHeightRisesContinuouslyAlongItsForwardAxis() {
        var ramp = new BuildingPhysics.Ramp(0f, 0f, 0f, 0f);
        float low = BuildingPhysics.rampHeightAt(0f, -1.25f, ramp);
        float mid = BuildingPhysics.rampHeightAt(0f, 0f, ramp);
        float high = BuildingPhysics.rampHeightAt(0f, 1.25f, ramp);
        assertTrue(low < mid);
        assertTrue(mid < high);
        assertTrue(high - low > 0.8f);
    }

    @Test
    void rotatedRampUsesRotatedLocalAxis() {
        var ramp = new BuildingPhysics.Ramp(0f, 0f, 0f, (float) (Math.PI / 2.0));
        float low = BuildingPhysics.rampHeightAt(-1.2f, 0f, ramp);
        float high = BuildingPhysics.rampHeightAt(1.2f, 0f, ramp);
        assertTrue(low < high);
    }

    @Test
    void rampCanProvideContinuousActorSupport() {
        var ramp = new BuildingPhysics.Ramp(0f, 0f, 0f, 0f);
        float first = BuildingPhysics.supportHeight(0f, -1.2f, 0f, 0f, List.of(), List.of(ramp));
        float second = BuildingPhysics.supportHeight(0f, -0.4f, 0f, first, List.of(), List.of(ramp));
        assertTrue(second > first);
    }

    @Test
    void elevatedPieceRequiresNearbyLowerSupport() {
        var floor = new BuildingPhysics.Floor(0f, 0f, 0f, 0f);
        assertTrue(BuildingPhysics.hasSupport(0.5f, 1.25f, 0.5f, 0f, List.of(floor), List.of(), 2.3f));
        assertFalse(BuildingPhysics.hasSupport(8f, 1.25f, 8f, 0f, List.of(floor), List.of(), 2.3f));
    }

    @Test
    void groundLevelPiecesAreAlwaysStructurallySupported() {
        assertTrue(BuildingPhysics.hasSupport(4f, 0.2f, 3f, 0f, List.of(), List.of(), 2.3f));
    }
}
