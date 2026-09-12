from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

if 'import com.samaheim.game.EncounterRules;' in text:
    print('enemy variety already integrated')
    raise SystemExit(0)

def rep(old: str, new: str) -> None:
    global text
    if old not in text:
        raise SystemExit('missing anchor:\n' + old[:260])
    text = text.replace(old, new, 1)

rep(
    'import com.samaheim.game.DungeonRules;\nimport com.samaheim.game.EquipmentRules;',
    'import com.samaheim.game.DungeonRules;\nimport com.samaheim.game.EncounterRules;\nimport com.samaheim.game.EquipmentRules;'
)

rep(
'''    private void spawnWorld() {
        Random random = new Random(seed ^ 0x51A9L);
        for (int i = 0; i < 312; i++) spawnResource("tree-" + i, ResourceType.TREE, random, 16f);
        for (int i = 0; i < 224; i++) spawnResource("rock-" + i, ResourceType.ROCK, random, 12f);
        for (int i = 0; i < 120; i++) spawnResource("berry-" + i, ResourceType.BERRY, random, 10f);
        for (int i = 0; i < 14; i++) spawnEnemy(randomSurfacePoint(random, 24f), i < 9 ? EnemyType.GOBLIN : EnemyType.SKELETON);
    }''',
'''    private void spawnWorld() {
        Random random = new Random(seed ^ 0x51A9L);
        for (int i = 0; i < 312; i++) spawnResource("tree-" + i, ResourceType.TREE, random, 16f);
        for (int i = 0; i < 224; i++) spawnResource("rock-" + i, ResourceType.ROCK, random, 12f);
        for (int i = 0; i < 120; i++) spawnResource("berry-" + i, ResourceType.BERRY, random, 10f);
        for (int i = 0; i < 14; i++) {
            Vector3f point = randomSurfacePoint(random, 24f);
            WorldMath.Region region = WorldMath.region(seed, point.x, point.z);
            spawnEnemy(point, enemyType(EncounterRules.forRegion(region, random.nextFloat())));
        }
    }'''
)

rep(
'''            if (!lootedPois.contains(poi.id())) {
                EnemyType guardian = poi.type() == FrontierPoiPlanner.PoiType.CINDER_SHRINE ? EnemyType.SKELETON : EnemyType.GOBLIN;
                spawnEnemy(new Vector3f(poi.x() + 3.2f, terrain.surfaceHeight(poi.x() + 3.2f, poi.z()), poi.z()), guardian);
            }''',
'''            if (!lootedPois.contains(poi.id())) {
                WorldMath.Region region = WorldMath.region(seed, poi.x(), poi.z());
                float roll = WorldMath.hash01(seed ^ 0x1A2B3C4D5E6F7788L, Math.round(poi.x()), Math.round(poi.z()));
                EnemyType guardian = enemyType(EncounterRules.forRegion(region, roll));
                spawnEnemy(new Vector3f(poi.x() + 3.2f, terrain.surfaceHeight(poi.x() + 3.2f, poi.z()), poi.z()), guardian);
            }'''
)

rep(
'''        if (!dungeonCleared()) {
            int index = 0;
            for (Vector3f spawn : DungeonRules.enemySpawns()) {
                float floor = dungeonFloor(spawn);
                Node enemy = spawnEnemy(new Vector3f(spawn.x, floor, spawn.z),
                        index++ % 2 == 0 ? EnemyType.SKELETON : EnemyType.GOBLIN);
                enemy.setUserData("dungeonEnemy", true);
            }
        }''',
'''        if (!dungeonCleared()) {
            List<EncounterRules.EnemyKind> guardians = EncounterRules.dungeonGuardians();
            int index = 0;
            for (Vector3f spawn : DungeonRules.enemySpawns()) {
                float floor = dungeonFloor(spawn);
                EnemyType type = enemyType(guardians.get(index % guardians.size()));
                Node enemy = spawnEnemy(new Vector3f(spawn.x, floor, spawn.z), type);
                enemy.setUserData("dungeonEnemy", true);
                index++;
            }
        }'''
)

rep(
'''    private Node spawnEnemy(Vector3f p, EnemyType type) {
        Node enemy = new Node("enemy");
        enemy.setUserData("kind", "ENEMY");
        enemy.setUserData("enemyType", type.name());
        enemy.setUserData("hp", type.hp);
        enemy.setUserData("attackCooldown", 0f);
        enemy.setUserData("attackWindup", 0f);
        enemy.setUserData("staggerClock", 0f);
        enemy.setLocalTranslation(p);
        Geometry body = new Geometry("body", new Box(0.34f, 0.78f, 0.30f));
        body.setMaterial(lit(type.color));
        body.setLocalTranslation(0f, 0.78f, 0f);
        enemy.attachChild(body);
        enemies.attachChild(enemy);
        return enemy;
    }''',
'''    private Node spawnEnemy(Vector3f p, EnemyType type) {
        Node enemy = new Node("enemy");
        enemy.setUserData("kind", "ENEMY");
        enemy.setUserData("enemyType", type.name());
        enemy.setUserData("hp", type.hp);
        enemy.setUserData("attackCooldown", 0f);
        enemy.setUserData("attackWindup", 0f);
        enemy.setUserData("staggerClock", 0f);
        enemy.setLocalTranslation(p);

        float halfHeight = switch (type) {
            case HIGHLAND_BRUTE -> 1.02f;
            case MIRE_STALKER -> 0.58f;
            case ASH_WRAITH -> 0.68f;
            default -> 0.78f;
        };
        Geometry body;
        if (type == EnemyType.ASH_WRAITH) {
            body = new Geometry("body", new Sphere(10, 14, 0.58f));
            body.setLocalScale(0.72f, 1.38f, 0.72f);
            body.setLocalTranslation(0f, 1.25f, 0f);
            Geometry core = new Geometry("wraith-core", new Sphere(8, 10, 0.18f));
            core.setMaterial(unshaded(new ColorRGBA(1f, 0.38f, 0.08f, 1f)));
            core.setLocalTranslation(0f, 1.28f, -0.34f);
            enemy.attachChild(core);
        } else {
            float halfWidth = type == EnemyType.HIGHLAND_BRUTE ? 0.52f : type == EnemyType.MIRE_STALKER ? 0.27f : 0.34f;
            float halfDepth = type == EnemyType.HIGHLAND_BRUTE ? 0.44f : type == EnemyType.MIRE_STALKER ? 0.25f : 0.30f;
            body = new Geometry("body", new Box(halfWidth, halfHeight, halfDepth));
            body.setLocalTranslation(0f, halfHeight, 0f);
        }
        body.setMaterial(lit(type.color));
        enemy.attachChild(body);
        enemies.attachChild(enemy);
        return enemy;
    }

    private EnemyType enemyType(EncounterRules.EnemyKind kind) {
        return switch (kind) {
            case GOBLIN -> EnemyType.GOBLIN;
            case GRAVEBORN -> EnemyType.SKELETON;
            case HIGHLAND_BRUTE -> EnemyType.HIGHLAND_BRUTE;
            case MIRE_STALKER -> EnemyType.MIRE_STALKER;
            case ASH_WRAITH -> EnemyType.ASH_WRAITH;
        };
    }

    private EquipmentRules.EnemyKind dropKind(EnemyType type) {
        return switch (type) {
            case GOBLIN, MIRE_STALKER -> EquipmentRules.EnemyKind.GOBLIN;
            case SKELETON, HIGHLAND_BRUTE, ASH_WRAITH -> EquipmentRules.EnemyKind.GRAVEBORN;
        };
    }'''
)

rep(
'''    private CombatRules.EnemyArchetype combatArchetype(EnemyType type) {
        return type == EnemyType.GOBLIN ? CombatRules.EnemyArchetype.GOBLIN : CombatRules.EnemyArchetype.GRAVEBORN;
    }''',
'''    private CombatRules.EnemyArchetype combatArchetype(EnemyType type) {
        return switch (type) {
            case GOBLIN -> CombatRules.EnemyArchetype.GOBLIN;
            case SKELETON -> CombatRules.EnemyArchetype.GRAVEBORN;
            case HIGHLAND_BRUTE -> CombatRules.EnemyArchetype.HIGHLAND_BRUTE;
            case MIRE_STALKER -> CombatRules.EnemyArchetype.MIRE_STALKER;
            case ASH_WRAITH -> CombatRules.EnemyArchetype.ASH_WRAITH;
        };
    }'''
)

rep(
'''            EquipmentRules.MaterialYield drop = EquipmentRules.enemyDrop(WorldMath.region(seed, defeatedAt.x, defeatedAt.z),
                    defeatedType == EnemyType.GOBLIN ? EquipmentRules.EnemyKind.GOBLIN : EquipmentRules.EnemyKind.GRAVEBORN);''',
'''            EquipmentRules.MaterialYield drop = EquipmentRules.enemyDrop(
                    WorldMath.region(seed, defeatedAt.x, defeatedAt.z), dropKind(defeatedType));'''
)

rep(
'''        if (y < SEA_LEVEL + 0.25f) return;
        spawnEnemy(new Vector3f(x, y, z), random.nextBoolean() ? EnemyType.GOBLIN : EnemyType.SKELETON);''',
'''        if (y < SEA_LEVEL + 0.25f) return;
        WorldMath.Region region = WorldMath.region(seed, x, z);
        spawnEnemy(new Vector3f(x, y, z), enemyType(EncounterRules.forRegion(region, random.nextFloat())));'''
)

rep(
'''    private enum EnemyType {
        GOBLIN("Goblin", 42f, 3.9f, 17f, 7f, 1.2f, new ColorRGBA(0.24f, 0.45f, 0.14f, 1f)),
        SKELETON("Graveborn", 68f, 3.2f, 20f, 11f, 1.45f, new ColorRGBA(0.72f, 0.72f, 0.66f, 1f));''',
'''    private enum EnemyType {
        GOBLIN("Goblin", 42f, 3.9f, 17f, 7f, 1.2f, new ColorRGBA(0.24f, 0.45f, 0.14f, 1f)),
        SKELETON("Graveborn", 68f, 3.2f, 20f, 11f, 1.45f, new ColorRGBA(0.72f, 0.72f, 0.66f, 1f)),
        HIGHLAND_BRUTE("Highland Brute", 118f, 2.35f, 22f, 18f, 1.82f, new ColorRGBA(0.48f, 0.30f, 0.17f, 1f)),
        MIRE_STALKER("Mire Stalker", 48f, 4.75f, 24f, 8f, 0.78f, new ColorRGBA(0.13f, 0.48f, 0.35f, 1f)),
        ASH_WRAITH("Ash Wraith", 60f, 4.15f, 26f, 15f, 1.18f, new ColorRGBA(0.72f, 0.20f, 0.08f, 1f));'''
)

path.write_text(text, encoding='utf-8')
print('integrated regional enemy variety into SamaheimCaveGame')
