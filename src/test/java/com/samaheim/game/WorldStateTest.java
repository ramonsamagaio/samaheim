package com.samaheim.game;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WorldStateTest {
    @Test void snapshotRoundTripPreservesWorldChanges() {
        WorldState state = new WorldState();
        state.depleteResource("tree:12");
        state.defeatUniqueEnemy("ogre:ruin-east");
        state.discoverPoi("ruin:east");
        state.buildStructure("camp:home");
        state.awakenSeal(1);
        state.awakenSeal(3);
        state.clearDungeonRoom();
        state.clearDungeonRoom();
        state.defeatDungeonBoss();

        WorldState restored = WorldState.fromSnapshot(state.snapshot());
        assertTrue(restored.isResourceDepleted("tree:12"));
        assertTrue(restored.isUniqueEnemyDefeated("ogre:ruin-east"));
        assertTrue(restored.isPoiDiscovered("ruin:east"));
        assertTrue(restored.hasStructure("camp:home"));
        assertEquals(2, restored.awakenedSealCount());
        assertEquals(2, restored.dungeonRoomsCleared());
        assertTrue(restored.dungeonBossDefeated());
    }

    @Test void respawnAndStructureRemovalUndoPersistentFlags() {
        WorldState state = new WorldState();
        state.depleteResource("berry:4");
        state.respawnResource("berry:4");
        state.buildStructure("wall:9");
        state.removeStructure("wall:9");
        assertFalse(state.isResourceDepleted("berry:4"));
        assertFalse(state.hasStructure("wall:9"));
    }

    @Test void duplicateSealCannotInflateProgress() {
        WorldState state = new WorldState();
        state.awakenSeal(2);
        state.awakenSeal(2);
        assertEquals(1, state.awakenedSealCount());
    }

    @Test void corruptSnapshotCannotRestoreNegativeDungeonProgress() {
        var snapshot = new WorldState.Snapshot(java.util.Set.of(), java.util.Set.of(), java.util.Set.of(),
                java.util.Set.of(), java.util.Set.of(), -1, false);
        assertThrows(IllegalArgumentException.class, () -> WorldState.fromSnapshot(snapshot));
    }
}
