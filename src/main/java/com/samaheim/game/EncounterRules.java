package com.samaheim.game;

import com.samaheim.world.WorldMath;

import java.util.List;

/** Deterministic enemy roster selection for regional encounters and the Delve. */
public final class EncounterRules {
    private EncounterRules() { }

    public static EnemyKind forRegion(WorldMath.Region region, float roll) {
        float r = Math.max(0f, Math.min(0.9999f, roll));
        return switch (region) {
            case GREENMARCH -> r < 0.68f ? EnemyKind.GOBLIN : EnemyKind.GRAVEBORN;
            case IRON_HIGHLANDS -> r < 0.58f ? EnemyKind.HIGHLAND_BRUTE : EnemyKind.GRAVEBORN;
            case MISTFEN -> r < 0.72f ? EnemyKind.MIRE_STALKER : EnemyKind.GOBLIN;
            case ASHEN_REACH -> r < 0.72f ? EnemyKind.ASH_WRAITH : EnemyKind.GRAVEBORN;
        };
    }

    public static List<EnemyKind> dungeonGuardians() {
        return List.of(
                EnemyKind.GRAVEBORN,
                EnemyKind.MIRE_STALKER,
                EnemyKind.HIGHLAND_BRUTE,
                EnemyKind.ASH_WRAITH);
    }

    public enum EnemyKind {
        GOBLIN,
        GRAVEBORN,
        HIGHLAND_BRUTE,
        MIRE_STALKER,
        ASH_WRAITH
    }
}
