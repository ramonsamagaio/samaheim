package com.samaheim.world;

import com.jme3.math.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class CaveCharacterPhysicsTest {
    @Test
    void playerCanMoveThroughAProperlyExcavatedTunnel() {
        VolumetricTerrain terrain = new VolumetricTerrain(99L, 24f, -16f, 18f, 1f, 8);
        float surface = terrain.surfaceHeight(0f, 0f);
        float tunnelY = surface - 2.2f;
        for (float x = -3f; x <= 5f; x += 2f) terrain.dig(new Vector3f(x, tunnelY, 0f), 2.4f, 6f);
        float foot = terrain.findFloor(-2.5f, 0f, tunnelY + 1.2f, 5f);
        assertTrue(Float.isFinite(foot));
        assertTrue(terrain.capsuleClear(-2.5f, foot + 0.04f, 0f, 0.31f, 1.78f));

        CaveCharacterPhysics.HorizontalMove move = CaveCharacterPhysics.moveHorizontal(terrain, -2.5f, foot + 0.04f, 0f,
                5.5f, 0f, 0.31f, 1.78f, 0.48f);

        assertTrue(move.x() > 1.5f);
    }

    @Test
    void capsuleCannotPassThroughUnexcavatedEarth() {
        VolumetricTerrain terrain = new VolumetricTerrain(123L, 24f, -16f, 18f, 1f, 8);
        float surface = terrain.surfaceHeight(0f, 0f);
        float buriedY = surface - 3f;

        CaveCharacterPhysics.HorizontalMove move = CaveCharacterPhysics.moveHorizontal(terrain, 0f, buriedY, 0f,
                3f, 0f, 0.31f, 1.78f, 0.48f);

        assertFalse(move.x() > 2f);
    }

    @Test
    void narrowRadiusFitsAHumanTunnelThatRejectsTheOldGrotesqueBody() {
        VolumetricTerrain terrain = new VolumetricTerrain(777L, 24f, -16f, 18f, 1f, 8);
        float surface = terrain.surfaceHeight(0f, 0f);
        Vector3f center = new Vector3f(0f, surface - 2.0f, 0f);
        terrain.dig(center, 2.8f, 8f);
        float foot = terrain.findFloor(0f, 0f, center.y + 1.2f, 6f);

        assertTrue(Float.isFinite(foot));
        assertTrue(terrain.capsuleClear(0f, foot + 0.06f, 0f, 0.31f, 1.78f));
        assertFalse(terrain.capsuleClear(0f, foot + 0.06f, 0f, 1.05f, 3.2f));
    }

    @Test
    void groundedMovementAdheresToSmallDownhillChangesInsteadOfHovering() {
        VolumetricTerrain terrain = new VolumetricTerrain(4242L, 24f, -16f, 18f, 1f, 8);
        float startFloor = terrain.surfaceHeight(-2f, 1f);
        float startY = startFloor + 0.04f;

        CaveCharacterPhysics.HorizontalMove move = CaveCharacterPhysics.moveHorizontal(terrain, -2f, startY, 1f,
                1.2f, 0f, 0.31f, 1.78f, 0.48f);
        float floorAtDestination = terrain.findFloor(move.x(), move.z(), move.footY() + 0.3f, 1.2f);

        assertTrue(Float.isFinite(floorAtDestination));
        assertTrue(Math.abs(move.footY() - (floorAtDestination + 0.04f)) < 0.22f);
    }

    @Test
    void largeHorizontalDeltaIsSubsteppedInsteadOfTunnelingThroughSolidGround() {
        VolumetricTerrain terrain = new VolumetricTerrain(8484L, 24f, -16f, 18f, 1f, 8);
        float surface = terrain.surfaceHeight(0f, 0f);
        float tunnelY = surface - 2.3f;
        terrain.dig(new Vector3f(-3f, tunnelY, 0f), 2.3f, 7f);
        float foot = terrain.findFloor(-3f, 0f, tunnelY + 1.3f, 5f);
        assertTrue(Float.isFinite(foot));

        CaveCharacterPhysics.HorizontalMove move = CaveCharacterPhysics.moveHorizontal(terrain, -3f, foot + 0.04f, 0f,
                8f, 0f, 0.31f, 1.78f, 0.48f);

        assertTrue(move.blocked());
        assertTrue(move.x() < 2.5f);
    }

    @Test
    void fallingOntoDensitySurfaceReportsLandingWithoutEmbeddingFeet() {
        VolumetricTerrain terrain = new VolumetricTerrain(1515L, 24f, -16f, 18f, 1f, 8);
        float floor = terrain.surfaceHeight(1f, -1f);
        float start = floor + 1.4f;

        CaveCharacterPhysics.VerticalMove move = CaveCharacterPhysics.moveVertical(terrain, 1f, start, -1f,
                -2.4f, 0.31f, 1.78f);

        assertTrue(move.landed());
        assertTrue(move.footY() >= floor - 0.03f);
        assertTrue(terrain.capsuleClear(1f, move.footY(), -1f, 0.31f, 1.78f));
    }
}
