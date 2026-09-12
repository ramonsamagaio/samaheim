package com.samaheim.world;

import java.util.ArrayList;
import java.util.List;

/** Deterministic region-aware POI placement and reward catalog. */
public final class FrontierPoiPlanner {
    private static final float LEGACY_HALF = 96f;
    private FrontierPoiPlanner() { }

    public static List<Poi> plan(long seed, float worldHalf) {
        List<Poi> out = new ArrayList<>();
        PoiType[] types = { PoiType.FORGE_RUIN, PoiType.FEN_ALTAR, PoiType.CINDER_SHRINE, PoiType.WAYSTONE_CACHE };
        int serial = 0;
        for (PoiType type : types) {
            for (int copy = 0; copy < 3; copy++) {
                Poi poi = find(seed, worldHalf, type, serial++, out);
                if (poi != null) out.add(poi);
            }
        }
        return List.copyOf(out);
    }

    private static Poi find(long seed, float worldHalf, PoiType type, int serial, List<Poi> existing) {
        long salt = 0x9E3779B97F4A7C15L ^ (long) type.ordinal() * 0x632BE59BD9B4E019L ^ serial * 0x85157AF5L;
        float span = Math.max(20f, worldHalf - LEGACY_HALF - 12f);
        for (int attempt = 0; attempt < 500; attempt++) {
            float rx = WorldMath.hash01(seed ^ salt, attempt * 13 + 7, serial * 31 + 5);
            float rz = WorldMath.hash01(seed ^ (salt >>> 1), attempt * 17 + 11, serial * 37 + 3);
            float x = signedFrontierCoordinate(rx, span, attempt % 2 == 0);
            float z = signedFrontierCoordinate(rz, span, attempt % 2 != 0);
            x = Math.max(-worldHalf + 8f, Math.min(worldHalf - 8f, x));
            z = Math.max(-worldHalf + 8f, Math.min(worldHalf - 8f, z));
            if (Math.max(Math.abs(x), Math.abs(z)) <= LEGACY_HALF + 10f) continue;
            WorldMath.Region region = WorldMath.region(seed, x, z);
            if (!type.accepts(region)) continue;
            if (tooClose(x, z, existing, 24f)) continue;
            return new Poi(type.name().toLowerCase() + '-' + serial, type, region, x, z, reward(type));
        }
        return null;
    }

    private static float signedFrontierCoordinate(float unit, float span, boolean forceOuter) {
        float sign = unit < 0.5f ? -1f : 1f;
        float t = unit < 0.5f ? unit * 2f : (unit - 0.5f) * 2f;
        float distance = forceOuter ? LEGACY_HALF + 12f + t * span : (t * 2f - 1f) * (LEGACY_HALF + span);
        return forceOuter ? sign * distance : distance;
    }

    private static boolean tooClose(float x, float z, List<Poi> existing, float minDistance) {
        float min2 = minDistance * minDistance;
        for (Poi poi : existing) {
            float dx = x - poi.x, dz = z - poi.z;
            if (dx * dx + dz * dz < min2) return true;
        }
        return false;
    }

    public static Reward reward(PoiType type) {
        return switch (type) {
            case FORGE_RUIN -> new Reward(3, 5, 3, 0, 0, 0);
            case FEN_ALTAR -> new Reward(2, 2, 0, 4, 0, 2);
            case CINDER_SHRINE -> new Reward(2, 4, 0, 0, 4, 1);
            case WAYSTONE_CACHE -> new Reward(7, 5, 1, 1, 1, 4);
        };
    }

    public enum PoiType {
        FORGE_RUIN(WorldMath.Region.IRON_HIGHLANDS),
        FEN_ALTAR(WorldMath.Region.MISTFEN),
        CINDER_SHRINE(WorldMath.Region.ASHEN_REACH),
        WAYSTONE_CACHE(null);

        private final WorldMath.Region preferred;
        PoiType(WorldMath.Region preferred) { this.preferred = preferred; }
        boolean accepts(WorldMath.Region region) { return preferred == null || preferred == region; }
    }

    public record Poi(String id, PoiType type, WorldMath.Region region, float x, float z, Reward reward) { }
    public record Reward(int wood, int stone, int ironOre, int mistResin, int cinderShard, int berries) { }
}
