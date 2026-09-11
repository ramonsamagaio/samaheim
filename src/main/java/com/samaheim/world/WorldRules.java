package com.samaheim.world;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class WorldRules {
    public static final float PLAYER_RADIUS = 0.42f;

    public enum Biome {
        GREENFIELDS,
        WILDWOOD,
        SUNKEN_MIRE,
        ASHEN_REACH
    }

    public record Point(float x, float z) {
        public float distanceSquared(Point other) {
            Objects.requireNonNull(other, "other");
            float dx = x - other.x;
            float dz = z - other.z;
            return dx * dx + dz * dz;
        }
    }

    public record Obstacle(String id, Point center, float radius) {
        public Obstacle {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(center, "center");
            if (radius <= 0f) {
                throw new IllegalArgumentException("radius must be > 0");
            }
        }
    }

    private WorldRules() {
    }

    public static boolean canOccupy(Point candidate, float actorRadius, float halfExtent, List<Obstacle> obstacles) {
        Objects.requireNonNull(candidate, "candidate");
        Objects.requireNonNull(obstacles, "obstacles");
        if (actorRadius <= 0f || halfExtent <= actorRadius) {
            return false;
        }
        if (Math.abs(candidate.x()) > halfExtent - actorRadius || Math.abs(candidate.z()) > halfExtent - actorRadius) {
            return false;
        }
        for (Obstacle obstacle : obstacles) {
            float combined = actorRadius + obstacle.radius();
            if (candidate.distanceSquared(obstacle.center()) < combined * combined) {
                return false;
            }
        }
        return true;
    }

    public static Point slideMove(Point from, Point desired, float actorRadius, float halfExtent,
                                  List<Obstacle> obstacles) {
        if (canOccupy(desired, actorRadius, halfExtent, obstacles)) {
            return desired;
        }
        Point xOnly = new Point(desired.x(), from.z());
        if (canOccupy(xOnly, actorRadius, halfExtent, obstacles)) {
            return xOnly;
        }
        Point zOnly = new Point(from.x(), desired.z());
        if (canOccupy(zOnly, actorRadius, halfExtent, obstacles)) {
            return zOnly;
        }
        return from;
    }

    public static Point findSafeSpawn(long seed, float halfExtent, List<Obstacle> obstacles) {
        Objects.requireNonNull(obstacles, "obstacles");
        for (int ring = 0; ring <= 14; ring++) {
            float radius = ring * 1.75f;
            int samples = ring == 0 ? 1 : 12 + ring * 4;
            for (int i = 0; i < samples; i++) {
                double phase = ((seed >>> 8) & 0xffffL) / 65535.0 * Math.PI * 2.0;
                double angle = phase + i * Math.PI * 2.0 / samples;
                Point candidate = new Point((float) Math.cos(angle) * radius, (float) Math.sin(angle) * radius);
                if (canOccupy(candidate, PLAYER_RADIUS, halfExtent, obstacles)
                        && slopeDegrees(seed, candidate.x(), candidate.z()) <= 24f) {
                    return candidate;
                }
            }
        }
        return new Point(0f, 0f);
    }

    public static float slopeDegrees(long seed, float x, float z) {
        float sample = 0.6f;
        float dx = WorldMath.height(seed, x + sample, z) - WorldMath.height(seed, x - sample, z);
        float dz = WorldMath.height(seed, x, z + sample) - WorldMath.height(seed, x, z - sample);
        float rise = (float) Math.sqrt(dx * dx + dz * dz);
        return (float) Math.toDegrees(Math.atan(rise / (sample * 2f)));
    }

    public static Biome biome(long seed, float x, float z) {
        float moisture = WorldMath.valueNoise(seed ^ 0x44AF1234L, x * 0.018f, z * 0.018f);
        float corruption = WorldMath.valueNoise(seed ^ 0x7F4A7C15L, x * 0.013f + 19f, z * 0.013f - 7f);
        float altitude = WorldMath.height(seed, x, z);
        if (corruption > 0.43f || altitude > 4.1f) {
            return Biome.ASHEN_REACH;
        }
        if (moisture > 0.38f && altitude < 0.8f) {
            return Biome.SUNKEN_MIRE;
        }
        if (moisture > -0.12f) {
            return Biome.WILDWOOD;
        }
        return Biome.GREENFIELDS;
    }

    public static List<Point> distributePois(long seed, int count, float halfExtent, float minSpacing) {
        if (count < 0 || halfExtent <= 0f || minSpacing < 0f) {
            throw new IllegalArgumentException("invalid distribution parameters");
        }
        List<Point> result = new ArrayList<>();
        int attempts = Math.max(64, count * 80);
        for (int i = 0; i < attempts && result.size() < count; i++) {
            float nx = WorldMath.hash01(seed ^ 0xC13FA9A9L, i, 17) * 2f - 1f;
            float nz = WorldMath.hash01(seed ^ 0x91E10DA5L, 31, i) * 2f - 1f;
            Point p = new Point(nx * (halfExtent - 8f), nz * (halfExtent - 8f));
            if (p.x() * p.x() + p.z() * p.z() < 14f * 14f) {
                continue;
            }
            boolean spaced = true;
            for (Point existing : result) {
                if (p.distanceSquared(existing) < minSpacing * minSpacing) {
                    spaced = false;
                    break;
                }
            }
            if (spaced && slopeDegrees(seed, p.x(), p.z()) <= 32f) {
                result.add(p);
            }
        }
        return List.copyOf(result);
    }
}
