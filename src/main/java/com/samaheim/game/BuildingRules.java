package com.samaheim.game;

import com.samaheim.world.WorldRules;

import java.util.List;
import java.util.Objects;

public final class BuildingRules {
    public enum Piece {
        CAMPFIRE(0.7f, true),
        WORKBENCH(1.0f, true),
        BEDROLL(0.9f, true),
        STORAGE_CHEST(0.65f, true),
        WOOD_WALL(1.1f, false),
        WOOD_FLOOR(1.15f, false);

        private final float footprintRadius;
        private final boolean requiresGround;

        Piece(float footprintRadius, boolean requiresGround) {
            this.footprintRadius = footprintRadius;
            this.requiresGround = requiresGround;
        }

        public float footprintRadius() { return footprintRadius; }
        public boolean requiresGround() { return requiresGround; }
    }

    public record Placement(boolean allowed, String reason) {
    }

    private BuildingRules() {
    }

    public static Placement validate(Piece piece, WorldRules.Point point, long seed, float worldHalfExtent,
                                     List<WorldRules.Obstacle> blockers, float distanceFromPlayer) {
        Objects.requireNonNull(piece, "piece");
        Objects.requireNonNull(point, "point");
        Objects.requireNonNull(blockers, "blockers");
        if (distanceFromPlayer > 5.5f) {
            return new Placement(false, "too-far");
        }
        if (Math.abs(point.x()) > worldHalfExtent - 2f || Math.abs(point.z()) > worldHalfExtent - 2f) {
            return new Placement(false, "world-edge");
        }
        if (piece.requiresGround() && WorldRules.slopeDegrees(seed, point.x(), point.z()) > 18f) {
            return new Placement(false, "slope");
        }
        if (!WorldRules.canOccupy(point, piece.footprintRadius(), worldHalfExtent, blockers)) {
            return new Placement(false, "blocked");
        }
        return new Placement(true, "ok");
    }

    public static float comfortRadius(Piece piece) {
        return switch (piece) {
            case CAMPFIRE -> 7.5f;
            case WORKBENCH -> 5.5f;
            case BEDROLL -> 3.5f;
            case STORAGE_CHEST -> 0f;
            case WOOD_WALL, WOOD_FLOOR -> 1.5f;
        };
    }

    public static boolean supportsRespawn(boolean hasCampfire, boolean hasBedroll, float separation) {
        return hasCampfire && hasBedroll && separation <= 8f;
    }
}
