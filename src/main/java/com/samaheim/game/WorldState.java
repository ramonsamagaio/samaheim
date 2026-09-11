package com.samaheim.game;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/** Serializable-friendly mutable state for world changes that must survive reloads. */
public final class WorldState {
    private final Set<String> depletedResources = new HashSet<>();
    private final Set<String> defeatedUniqueEnemies = new HashSet<>();
    private final Set<String> discoveredPois = new HashSet<>();
    private final Set<String> builtStructures = new HashSet<>();
    private final Set<Integer> awakenedSeals = new HashSet<>();
    private int dungeonRoomsCleared;
    private boolean dungeonBossDefeated;

    public void depleteResource(String id) { depletedResources.add(valid(id)); }
    public void respawnResource(String id) { depletedResources.remove(valid(id)); }
    public boolean isResourceDepleted(String id) { return depletedResources.contains(valid(id)); }

    public void defeatUniqueEnemy(String id) { defeatedUniqueEnemies.add(valid(id)); }
    public boolean isUniqueEnemyDefeated(String id) { return defeatedUniqueEnemies.contains(valid(id)); }

    public void discoverPoi(String id) { discoveredPois.add(valid(id)); }
    public boolean isPoiDiscovered(String id) { return discoveredPois.contains(valid(id)); }

    public void buildStructure(String id) { builtStructures.add(valid(id)); }
    public void removeStructure(String id) { builtStructures.remove(valid(id)); }
    public boolean hasStructure(String id) { return builtStructures.contains(valid(id)); }

    public void awakenSeal(int id) {
        if (id < 1 || id > 3) throw new IllegalArgumentException("seal id must be 1..3");
        awakenedSeals.add(id);
    }
    public int awakenedSealCount() { return awakenedSeals.size(); }

    public void clearDungeonRoom() { dungeonRoomsCleared++; }
    public int dungeonRoomsCleared() { return dungeonRoomsCleared; }
    public void defeatDungeonBoss() { dungeonBossDefeated = true; }
    public boolean dungeonBossDefeated() { return dungeonBossDefeated; }

    public Snapshot snapshot() {
        return new Snapshot(Set.copyOf(depletedResources), Set.copyOf(defeatedUniqueEnemies),
                Set.copyOf(discoveredPois), Set.copyOf(builtStructures), Set.copyOf(awakenedSeals),
                dungeonRoomsCleared, dungeonBossDefeated);
    }

    public static WorldState fromSnapshot(Snapshot snapshot) {
        Objects.requireNonNull(snapshot);
        WorldState state = new WorldState();
        state.depletedResources.addAll(snapshot.depletedResources());
        state.defeatedUniqueEnemies.addAll(snapshot.defeatedUniqueEnemies());
        state.discoveredPois.addAll(snapshot.discoveredPois());
        state.builtStructures.addAll(snapshot.builtStructures());
        for (int seal : snapshot.awakenedSeals()) state.awakenSeal(seal);
        if (snapshot.dungeonRoomsCleared() < 0) throw new IllegalArgumentException("negative cleared rooms");
        state.dungeonRoomsCleared = snapshot.dungeonRoomsCleared();
        state.dungeonBossDefeated = snapshot.dungeonBossDefeated();
        return state;
    }

    private static String valid(String id) {
        Objects.requireNonNull(id);
        if (id.isBlank()) throw new IllegalArgumentException("blank world id");
        return id;
    }

    public record Snapshot(Set<String> depletedResources,
                           Set<String> defeatedUniqueEnemies,
                           Set<String> discoveredPois,
                           Set<String> builtStructures,
                           Set<Integer> awakenedSeals,
                           int dungeonRoomsCleared,
                           boolean dungeonBossDefeated) {
        public Snapshot {
            depletedResources = Set.copyOf(depletedResources);
            defeatedUniqueEnemies = Set.copyOf(defeatedUniqueEnemies);
            discoveredPois = Set.copyOf(discoveredPois);
            builtStructures = Set.copyOf(builtStructures);
            awakenedSeals = Set.copyOf(awakenedSeals);
        }
    }
}
