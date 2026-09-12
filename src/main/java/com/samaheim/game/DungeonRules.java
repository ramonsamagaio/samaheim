package com.samaheim.game;

import com.jme3.math.Vector3f;

import java.util.List;

/** Deterministic first playable dungeon contract shared by runtime and tests. */
public final class DungeonRules {
    public static final float CENTER_Y = -13.0f;
    public static final float CARVE_RADIUS = 4.6f;
    public static final float CARVE_STRENGTH = 8.0f;

    private DungeonRules() { }

    public static List<Vector3f> carveCenters() {
        return List.of(
                new Vector3f(-7f, CENTER_Y, 0f),
                new Vector3f(-3.5f, CENTER_Y, 0f),
                new Vector3f(0f, CENTER_Y, 0f),
                new Vector3f(3.5f, CENTER_Y, 0f),
                new Vector3f(7f, CENTER_Y, 0f),
                new Vector3f(10f, CENTER_Y, 3.2f),
                new Vector3f(10f, CENTER_Y, 6.4f));
    }

    public static List<Vector3f> enemySpawns() {
        return List.of(
                new Vector3f(-1.2f, CENTER_Y + 2.5f, 1.2f),
                new Vector3f(3.0f, CENTER_Y + 2.5f, -1.4f),
                new Vector3f(7.8f, CENTER_Y + 2.5f, 1.1f),
                new Vector3f(9.8f, CENTER_Y + 2.5f, 5.2f));
    }

    public static Vector3f entryPoint() { return new Vector3f(-7f, CENTER_Y + 2.8f, 0f); }
    public static Vector3f chestPoint() { return new Vector3f(10f, CENTER_Y + 2.8f, 6.2f); }
    public static Vector3f exitPoint() { return new Vector3f(-8.6f, CENTER_Y + 2.8f, 0f); }

    public static boolean chestUnlocked(int livingDungeonEnemies, boolean cleared) {
        return cleared || livingDungeonEnemies <= 0;
    }

    public static Reward reward() { return new Reward(4, 4, 4, 6, 6, 5); }

    public record Reward(int wood, int stone, int ironOre, int mistResin, int cinderShard, int berries) { }
}
