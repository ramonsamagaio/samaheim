package com.samaheim.game;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Deterministic gameplay rules that are intentionally independent of rendering.
 * Keeping these rules pure makes the first-hour loop regression-testable before
 * wiring them into the jMonkeyEngine presentation layer.
 */
public final class FirstHourRules {
    private FirstHourRules() {}

    public enum Biome { GREENWOOD, MIST_MARSH, ASHEN_HIGHLANDS, ARCANE_RUINS }
    public enum Enemy { GOBLIN, GRAVEBORN, WOLF, CULTIST, OGRE }
    public enum Weapon { BARE_HANDS, WANDERER_BLADE, IRON_SWORD, ARCANE_STAFF }

    public record Vec2(float x, float z) {
        public float distanceSquared(Vec2 other) {
            float dx = x - other.x;
            float dz = z - other.z;
            return dx * dx + dz * dz;
        }
        public Vec2 add(Vec2 delta) { return new Vec2(x + delta.x, z + delta.z); }
    }

    public record CircleObstacle(Vec2 center, float radius) {
        public CircleObstacle {
            Objects.requireNonNull(center);
            if (radius <= 0f) throw new IllegalArgumentException("radius must be positive");
        }
    }

    public static Vec2 resolveMovement(Vec2 from, Vec2 desiredDelta, float playerRadius,
                                       float worldHalfExtent, List<CircleObstacle> obstacles) {
        Vec2 candidate = clamp(from.add(desiredDelta), playerRadius, worldHalfExtent);
        if (!collides(candidate, playerRadius, obstacles)) return candidate;

        Vec2 xOnly = clamp(new Vec2(from.x + desiredDelta.x, from.z), playerRadius, worldHalfExtent);
        if (!collides(xOnly, playerRadius, obstacles)) return xOnly;

        Vec2 zOnly = clamp(new Vec2(from.x, from.z + desiredDelta.z), playerRadius, worldHalfExtent);
        if (!collides(zOnly, playerRadius, obstacles)) return zOnly;
        return from;
    }

    private static Vec2 clamp(Vec2 p, float radius, float halfExtent) {
        float min = -halfExtent + radius;
        float max = halfExtent - radius;
        return new Vec2(Math.clamp(p.x, min, max), Math.clamp(p.z, min, max));
    }

    public static boolean collides(Vec2 p, float playerRadius, List<CircleObstacle> obstacles) {
        for (CircleObstacle o : obstacles) {
            float r = playerRadius + o.radius;
            if (p.distanceSquared(o.center) < r * r) return true;
        }
        return false;
    }

    public static float slopeSpeedMultiplier(float risePerMeter) {
        float s = Math.abs(risePerMeter);
        if (s >= 1.35f) return 0f;
        if (s >= 0.85f) return 0.35f;
        if (s >= 0.50f) return 0.65f;
        return 1f;
    }

    public static float weaponDamage(Weapon weapon) {
        return switch (weapon) {
            case BARE_HANDS -> 5f;
            case WANDERER_BLADE -> 18f;
            case IRON_SWORD -> 29f;
            case ARCANE_STAFF -> 24f;
        };
    }

    public static float staminaCost(Weapon weapon) {
        return switch (weapon) {
            case BARE_HANDS -> 4f;
            case WANDERER_BLADE -> 9f;
            case IRON_SWORD -> 13f;
            case ARCANE_STAFF -> 16f;
        };
    }

    public static float applyArmor(float rawDamage, float armor) {
        if (rawDamage <= 0f) return 0f;
        float reduction = Math.clamp(armor / (armor + 50f), 0f, 0.75f);
        return Math.max(1f, rawDamage * (1f - reduction));
    }

    public static Biome biomeAt(long seed, float x, float z) {
        long h = seed ^ (long) Math.floor(x / 42f) * 0x9E3779B97F4A7C15L
                ^ (long) Math.floor(z / 42f) * 0xC2B2AE3D27D4EB4FL;
        h ^= h >>> 30; h *= 0xBF58476D1CE4E5B9L; h ^= h >>> 27; h *= 0x94D049BB133111EBL; h ^= h >>> 31;
        int value = Math.floorMod((int) h, 100);
        if (value < 46) return Biome.GREENWOOD;
        if (value < 68) return Biome.MIST_MARSH;
        if (value < 87) return Biome.ASHEN_HIGHLANDS;
        return Biome.ARCANE_RUINS;
    }

    public static List<Enemy> encounterFor(Biome biome, int dangerTier, long roll) {
        int count = Math.clamp(1 + dangerTier / 2, 1, 5);
        List<Enemy> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int r = Math.floorMod((int) (roll + i * 31L), 100);
            Enemy enemy = switch (biome) {
                case GREENWOOD -> r < 55 ? Enemy.GOBLIN : (r < 88 ? Enemy.WOLF : Enemy.OGRE);
                case MIST_MARSH -> r < 55 ? Enemy.GRAVEBORN : (r < 88 ? Enemy.CULTIST : Enemy.OGRE);
                case ASHEN_HIGHLANDS -> r < 48 ? Enemy.WOLF : (r < 84 ? Enemy.CULTIST : Enemy.OGRE);
                case ARCANE_RUINS -> r < 55 ? Enemy.CULTIST : (r < 90 ? Enemy.GRAVEBORN : Enemy.OGRE);
            };
            result.add(enemy);
        }
        return result;
    }

    public static float resourceRespawnSeconds(String kind) {
        return switch (kind) {
            case "BERRY" -> 240f;
            case "TREE" -> 720f;
            case "ROCK" -> 900f;
            default -> throw new IllegalArgumentException("unknown resource: " + kind);
        };
    }

    public static boolean canPlaceStructure(Vec2 position, float radius, float worldHalfExtent,
                                            List<CircleObstacle> blockers, float terrainSlope) {
        if (Math.abs(position.x) + radius > worldHalfExtent || Math.abs(position.z) + radius > worldHalfExtent) return false;
        if (Math.abs(terrainSlope) > 0.58f) return false;
        return !collides(position, radius, blockers);
    }

    public static int dungeonRoomCount(int tier) { return Math.clamp(3 + tier * 2, 3, 11); }
    public static boolean dungeonComplete(int roomsCleared, int totalRooms, boolean bossDefeated) {
        return totalRooms >= 3 && roomsCleared >= totalRooms && bossDefeated;
    }

    public static int dangerTier(float minutesPlayed, int sealsAwakened) {
        int timeTier = (int) (minutesPlayed / 15f);
        return Math.clamp(Math.max(timeTier, sealsAwakened), 0, 4);
    }

    public static boolean shouldSpawnEncounter(int nearbyEnemies, float secondsSinceLastEncounter,
                                               float distanceFromCamp, boolean night) {
        int cap = night ? 7 : 5;
        float cooldown = night ? 34f : 52f;
        return nearbyEnemies < cap && secondsSinceLastEncounter >= cooldown && distanceFromCamp > 8f;
    }
}
