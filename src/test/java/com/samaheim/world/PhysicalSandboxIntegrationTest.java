package com.samaheim.world;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class PhysicalSandboxIntegrationTest {
    @Test
    void playerCannotWalkThroughTreeSizedBlocker() {
        SandboxPhysics.HorizontalResult hit = SandboxPhysics.resolveHorizontal(
                0f, 0f, 2f, 0f, 0.42f,
                List.of(new SandboxPhysics.CircleBlocker(1.4f, 0f, 0.55f)));
        assertTrue(hit.blocked());
        float dx = hit.x() - 1.4f;
        float dz = hit.z();
        assertTrue(dx * dx + dz * dz >= 0.96f * 0.96f);
    }

    @Test
    void jumpLeavesEditedGroundAndReturnsToNewSurface() {
        TerrainState terrain = new TerrainState(17L, 24f, 48, 8f);
        terrain.raise(0f, 0f, 3f, 1.5f);
        float ground = terrain.sampleHeight(0f, 0f);
        float eyeHeight = 1.72f;
        float y = ground + eyeHeight;
        float velocity = 0f;
        boolean grounded = true;
        SandboxPhysics.VerticalResult first = SandboxPhysics.stepVertical(y, velocity, ground, eyeHeight, true, 1f / 60f);
        assertFalse(first.grounded());
        y = first.eyeY(); velocity = first.velocityY(); grounded = first.grounded();
        for (int i = 0; i < 180 && !grounded; i++) {
            SandboxPhysics.VerticalResult next = SandboxPhysics.stepVertical(y, velocity, ground, eyeHeight, false, 1f / 60f);
            y = next.eyeY(); velocity = next.velocityY(); grounded = next.grounded();
        }
        assertTrue(grounded);
        assertTrue(Math.abs(y - (ground + eyeHeight)) < 0.02f);
    }

    @Test
    void leveledTerrainCanBecomeBuildable() {
        TerrainState terrain = new TerrainState(321L, 24f, 48, 8f);
        float target = terrain.sampleHeight(0f, 0f);
        for (int i = 0; i < 12; i++) terrain.level(0f, 0f, 3.5f, target, 0.65f);
        for (int i = 0; i < 6; i++) terrain.smooth(0f, 0f, 3.5f, 0.4f);
        assertTrue(terrain.isBuildable(0f, 0f, 1.4f, 24f, 0.95f));
    }

    @Test
    void raisedGroundCostsResourcesThroughActualToolSystem() {
        TerrainState terrain = new TerrainState(777L, 24f, 48, 8f);
        TerraformToolSystem.Result denied = TerraformToolSystem.apply(
                terrain, TerraformToolSystem.Mode.RAISE, 0f, 0f,
                terrain.sampleHeight(0f, 0f), TerraformToolSystem.DEFAULT_RADIUS, 0, 100f);
        assertFalse(denied.applied());
        TerraformToolSystem.Result allowed = TerraformToolSystem.apply(
                terrain, TerraformToolSystem.Mode.RAISE, 0f, 0f,
                terrain.sampleHeight(0f, 0f), TerraformToolSystem.DEFAULT_RADIUS, 10, 100f);
        assertTrue(allowed.applied());
        assertTrue(allowed.stoneSpent() > 0);
        assertTrue(allowed.staminaSpent() > 0f);
    }
}
