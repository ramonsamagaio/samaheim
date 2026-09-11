package com.samaheim.world;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

final class WorldMathTest {
    @Test
    void terrainIsDeterministicForTheSameSeed() {
        long seed = 42L;
        float first = WorldMath.height(seed, 18.25f, -31.75f);
        float second = WorldMath.height(seed, 18.25f, -31.75f);
        assertEquals(first, second);
    }

    @Test
    void differentSeedsProduceDifferentTerrain() {
        float first = WorldMath.height(42L, 18.25f, -31.75f);
        float second = WorldMath.height(99L, 18.25f, -31.75f);
        assertNotEquals(first, second);
    }
}
