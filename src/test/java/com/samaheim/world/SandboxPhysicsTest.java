package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SandboxPhysicsTest {
    @Test
    void movementStopsOutsideCircularObstacle() {
        var result = SandboxPhysics.resolveHorizontal(0f, 0f, 2f, 0f, 0.45f,
                List.of(new SandboxPhysics.CircleBlocker(2f, 0f, 0.55f)));
        assertTrue(result.blocked());
        float dx = result.x() - 2f;
        float dz = result.z();
        assertTrue(Math.sqrt(dx * dx + dz * dz) >= 0.999);
    }

    @Test
    void clearMovementPassesThroughUnchanged() {
        var result = SandboxPhysics.resolveHorizontal(0f, 0f, 1f, 1f, 0.4f,
                List.of(new SandboxPhysics.CircleBlocker(8f, 8f, 1f)));
        assertFalse(result.blocked());
        assertEquals(1f, result.x(), 0.0001f);
        assertEquals(1f, result.z(), 0.0001f);
    }

    @Test
    void fastMovementCannotTunnelThroughTreeSizedBlocker() {
        var result = SandboxPhysics.resolveHorizontal(-4f, 0f, 4f, 0f, 0.42f,
                List.of(new SandboxPhysics.CircleBlocker(0f, 0f, 0.55f)));
        assertTrue(result.blocked());
        assertTrue(result.x() < 0f, "swept movement must remain on the approach side");
        assertTrue(Math.abs(result.x()) >= 0.96f);
    }

    @Test
    void diagonalMovementSlidesAroundObstacleInsteadOfFreezing() {
        var result = SandboxPhysics.resolveHorizontal(-1.4f, -1.0f, 1.2f, 1.8f, 0.42f,
                List.of(new SandboxPhysics.CircleBlocker(0f, 0f, 0.6f)));
        assertTrue(result.blocked());
        assertTrue(result.z() > -0.4f, "collision should retain useful tangential movement");
    }

    @Test
    void overlappingBlockersAreResolvedWithoutNaN() {
        var result = SandboxPhysics.resolveHorizontal(0f, -2f, 0f, 0f, 0.42f,
                List.of(new SandboxPhysics.CircleBlocker(-0.35f, 0f, 0.6f),
                        new SandboxPhysics.CircleBlocker(0.35f, 0f, 0.6f)));
        assertTrue(result.blocked());
        assertTrue(Float.isFinite(result.x()));
        assertTrue(Float.isFinite(result.z()));
    }

    @Test
    void jumpLeavesGroundAndGravityBringsPlayerBack() {
        float eyeHeight = 1.72f;
        float y = eyeHeight;
        float velocity = 0f;
        var first = SandboxPhysics.stepVertical(y, velocity, 0f, eyeHeight, true, 1f / 60f);
        assertFalse(first.grounded());
        assertTrue(first.eyeY() > eyeHeight);
        y = first.eyeY();
        velocity = first.velocityY();
        SandboxPhysics.VerticalResult state = first;
        for (int i = 0; i < 240; i++) {
            state = SandboxPhysics.stepVertical(y, velocity, 0f, eyeHeight, false, 1f / 60f);
            y = state.eyeY();
            velocity = state.velocityY();
        }
        assertTrue(state.grounded());
        assertEquals(eyeHeight, state.eyeY(), 0.001f);
    }

    @Test
    void fallingTracksRaisedTerrain() {
        var state = SandboxPhysics.stepVertical(5f, -2f, 2f, 1.72f, false, 0.05f);
        assertTrue(state.eyeY() >= 3.72f);
    }

    @Test
    void hugeFrameHitchCannotTunnelBelowGround() {
        var state = SandboxPhysics.stepVertical(20f, -50f, 4f, 1.72f, false, 0.8f);
        assertTrue(state.eyeY() >= 5.72f);
    }

    @Test
    void corruptedVerticalInputFailsSafeToGround() {
        var state = SandboxPhysics.stepVertical(Float.NaN, Float.NaN, 3f, 1.72f, true, Float.NaN);
        assertTrue(state.grounded());
        assertEquals(4.72f, state.eyeY(), 0.001f);
    }
}
