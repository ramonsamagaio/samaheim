package com.samaheim.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeBudgetTest {
    @Test void enemyCapPreventsRunawayGrowth() {
        assertTrue(RuntimeBudget.canSpawnEnemy(RuntimeBudget.MAX_ACTIVE_ENEMIES - 1, true, true));
        assertFalse(RuntimeBudget.canSpawnEnemy(RuntimeBudget.MAX_ACTIVE_ENEMIES, true, true));
        assertFalse(RuntimeBudget.canSpawnEnemy(2, false, true));
    }

    @Test void dropsAreHardCapped() {
        assertEquals(RuntimeBudget.MAX_ACTIVE_DROPS, RuntimeBudget.trimDrops(500));
        assertEquals(0, RuntimeBudget.trimDrops(-5));
    }

    @Test void particlesScaleDownOnSlowFrames() {
        int fast = RuntimeBudget.particleBudget(1000, 12f);
        int slow = RuntimeBudget.particleBudget(1000, 30f);
        assertTrue(slow < fast);
        assertTrue(fast <= RuntimeBudget.MAX_ACTIVE_PARTICLES);
    }

    @Test void structureCellCapIsEnforced() {
        assertTrue(RuntimeBudget.canPlaceInCell(RuntimeBudget.MAX_ACTIVE_STRUCTURES_IN_CELL - 1));
        assertFalse(RuntimeBudget.canPlaceInCell(RuntimeBudget.MAX_ACTIVE_STRUCTURES_IN_CELL));
    }
}
