package com.samaheim.world;

import com.samaheim.game.FirstHourRules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Deterministic high-level world layout used to keep progression landmarks reachable and spaced. */
public final class WorldPlanner {
    private WorldPlanner() {}

    public enum PoiType { RUIN, SHRINE, DUNGEON, IRON_CAMP, ARCANE_TOWER }
    public record Poi(String id, PoiType type, FirstHourRules.Vec2 position) {}

    public static List<Poi> plan(long seed, float halfExtent) {
        if (halfExtent < 70f) throw new IllegalArgumentException("world too small for first-hour layout");
        Random random = new Random(seed ^ 0x51A9D4E2L);
        List<Poi> result = new ArrayList<>();
        add(result, random, halfExtent, PoiType.RUIN, 2, 24f, 13f);
        add(result, random, halfExtent, PoiType.SHRINE, 3, 34f, 18f);
        add(result, random, halfExtent, PoiType.IRON_CAMP, 2, 44f, 18f);
        add(result, random, halfExtent, PoiType.ARCANE_TOWER, 1, 58f, 20f);
        add(result, random, halfExtent, PoiType.DUNGEON, 1, 62f, 24f);
        return List.copyOf(result);
    }

    private static void add(List<Poi> out, Random random, float halfExtent, PoiType type,
                            int count, float minSpawnDistance, float minPoiDistance) {
        int attempts = 0;
        while (count > 0 && attempts++ < 6000) {
            float x = random.nextFloat(-halfExtent + 8f, halfExtent - 8f);
            float z = random.nextFloat(-halfExtent + 8f, halfExtent - 8f);
            FirstHourRules.Vec2 p = new FirstHourRules.Vec2(x, z);
            if (p.distanceSquared(new FirstHourRules.Vec2(0f, 0f)) < minSpawnDistance * minSpawnDistance) continue;
            boolean overlaps = out.stream().anyMatch(existing ->
                    existing.position().distanceSquared(p) < minPoiDistance * minPoiDistance);
            if (overlaps) continue;
            out.add(new Poi(type.name().toLowerCase() + ":" + out.size(), type, p));
            count--;
        }
        if (count > 0) throw new IllegalStateException("could not place required POIs");
    }

    public static boolean spawnIsSafe(List<Poi> pois, float radius) {
        FirstHourRules.Vec2 origin = new FirstHourRules.Vec2(0f, 0f);
        return pois.stream().noneMatch(p -> p.position().distanceSquared(origin) < radius * radius);
    }
}
