package com.samaheim.game;

import java.util.ArrayList;
import java.util.List;

/** Headless pacing model for catching first-hour dead zones and encounter floods. */
public final class FirstHourSoak {
    private FirstHourSoak() {}

    public record Result(int encounters, int nightEncounters, int biomeChanges,
                         int resourceRespawns, int maxDangerTier, List<String> milestones) {}

    public static Result simulate(long seed) {
        int encounters = 0;
        int nightEncounters = 0;
        int biomeChanges = 0;
        int resourceRespawns = 0;
        int maxDangerTier = 0;
        int nearbyEnemies = 0;
        float sinceEncounter = 0f;
        float distanceFromCamp = 18f;
        FirstHourRules.Biome lastBiome = FirstHourRules.biomeAt(seed, 0f, 0f);
        List<String> milestones = new ArrayList<>();

        for (int second = 0; second <= 3600; second++) {
            float minute = second / 60f;
            boolean night = (second / 450) % 2 == 1;
            int seals = minute >= 42f ? 3 : minute >= 30f ? 2 : minute >= 20f ? 1 : 0;
            int danger = FirstHourRules.dangerTier(minute, seals);
            maxDangerTier = Math.max(maxDangerTier, danger);

            float x = (float) Math.sin(second / 170.0) * 95f;
            float z = (float) Math.cos(second / 220.0) * 95f;
            FirstHourRules.Biome biome = FirstHourRules.biomeAt(seed, x, z);
            if (biome != lastBiome) {
                biomeChanges++;
                lastBiome = biome;
            }

            sinceEncounter += 1f;
            if (FirstHourRules.shouldSpawnEncounter(nearbyEnemies, sinceEncounter, distanceFromCamp, night)) {
                encounters++;
                if (night) nightEncounters++;
                nearbyEnemies = Math.min(7, FirstHourRules.encounterFor(biome, danger, seed + second).size());
                sinceEncounter = 0f;
            }
            if (second % 22 == 0 && nearbyEnemies > 0) nearbyEnemies--;
            if (second > 0 && second % 240 == 0) resourceRespawns++;

            if (second == 300) milestones.add("starter-craft");
            if (second == 900) milestones.add("first-poi");
            if (second == 1500) milestones.add("first-seal");
            if (second == 2400) milestones.add("dungeon-entry");
            if (second == 3300) milestones.add("dungeon-boss");
        }

        return new Result(encounters, nightEncounters, biomeChanges, resourceRespawns,
                maxDangerTier, List.copyOf(milestones));
    }
}
