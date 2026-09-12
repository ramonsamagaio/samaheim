package com.samaheim.game;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/** Deterministic first-hour resource renewal rules. */
public final class ResourceRegrowthRules {
    private ResourceRegrowthRules() { }

    public static boolean crossedDawn(float previousClock, float currentClock) {
        float previous = normalize(previousClock);
        float current = normalize(currentClock);
        return previous > 0.82f && current < 0.18f;
    }

    public static boolean isHarvestedResourceId(String id) {
        return id != null && (id.startsWith("tree-") || id.startsWith("rock-") || id.startsWith("berry-"));
    }

    public static int regrowthCount(int harvestedCount) {
        if (harvestedCount <= 0) return 0;
        return Math.min(harvestedCount, Math.max(2, (int) Math.ceil(harvestedCount * 0.22f)));
    }

    public static List<String> selectForRegrowth(Set<String> removed, long seed, long dawnIndex) {
        if (removed == null || removed.isEmpty()) return List.of();
        List<String> candidates = new ArrayList<>();
        for (String id : removed) if (isHarvestedResourceId(id)) candidates.add(id);
        if (candidates.isEmpty()) return List.of();
        candidates.sort(Comparator.comparingLong(id -> mix(seed, dawnIndex, id)));
        int count = regrowthCount(candidates.size());
        return List.copyOf(candidates.subList(0, count));
    }

    public static long mix(long seed, long dawnIndex, String id) {
        long value = seed ^ (dawnIndex * 0x9E3779B97F4A7C15L) ^ id.hashCode();
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return value;
    }

    private static float normalize(float value) {
        float result = value % 1f;
        return result < 0f ? result + 1f : result;
    }
}
