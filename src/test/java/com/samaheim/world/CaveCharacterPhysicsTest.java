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
    void narrowRadiusIsLessGrotesqueAroundCarvedEdges() {
        VolumetricTerrain terrain = new VolumetricTerrain(777L, 24f, -16f, 18f, 1f, 8);
        float surface = terrain.surfaceHeight(0f, 0f);
        Vector3f center = new Vector3f(0f, surface - 1.8f, 0f);
        terrain.dig(center, 2.25f, 6f);
        float foot = terrain.findFloor(0f, 0f, center.y + 1.4f, 5f);

        assertTrue(Float.isFinite(foot));
        assertTrue(terrain.capsuleClear(0f, foot + 0.04f, 0f, 0.31f, 1.70f));
        assertFalse(terrain.capsuleClear(0f, foot + 0.04f, 0f, 1.05f, 2.8f));
    }
}
