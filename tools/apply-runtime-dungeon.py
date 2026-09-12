from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

if 'import com.samaheim.game.DungeonRules;' in text:
    print('runtime dungeon already integrated')
    raise SystemExit(0)

def rep(old: str, new: str) -> None:
    global text
    if old not in text:
        raise SystemExit('missing anchor:\n' + old[:220])
    text = text.replace(old, new, 1)

rep(
    'import com.samaheim.game.CombatRules;\nimport com.samaheim.game.EquipmentRules;',
    'import com.samaheim.game.CombatRules;\nimport com.samaheim.game.DungeonRules;\nimport com.samaheim.game.EquipmentRules;'
)

rep(
    '    private static final int TERRAIN_STREAM_RADIUS = 5;\n',
    '    private static final int TERRAIN_STREAM_RADIUS = 5;\n'
    '    private static final String DUNGEON_GENERATED_FLAG = "__dungeon_generated__";\n'
    '    private static final String DUNGEON_CLEARED_FLAG = "__dungeon_cleared__";\n'
    '    private static final String DUNGEON_LOOTED_FLAG = "__dungeon_looted__";\n'
)

rep(
    '    private final Node pois = new Node("frontier-pois");\n',
    '    private final Node pois = new Node("frontier-pois");\n'
    '    private final Node dungeonRoot = new Node("runtime-dungeon");\n'
)

rep(
    '    private boolean bladeCrafted;\n    private ToolMode toolMode = ToolMode.DIG;',
    '    private boolean bladeCrafted;\n'
    '    private boolean inDungeon;\n'
    '    private Vector3f dungeonReturnPoint = new Vector3f();\n'
    '    private ToolMode toolMode = ToolMode.DIG;'
)

rep(
    '        if (save != null) {\n            terrain.decodeEdits(save.edits);\n            restoreState(save);\n        }\n        playerX = save == null ? 0f : Math.clamp(save.x, -WORLD_HALF + 2f, WORLD_HALF - 2f);',
    '        if (save != null) {\n            terrain.decodeEdits(save.edits);\n            restoreState(save);\n        }\n'
    '        ensureRuntimeDungeonCarved();\n'
    '        playerX = save == null ? 0f : Math.clamp(save.x, -WORLD_HALF + 2f, WORLD_HALF - 2f);'
)

rep(
    '        rootNode.attachChild(enemies);\n        rootNode.attachChild(pois);',
    '        rootNode.attachChild(enemies);\n        rootNode.attachChild(pois);\n        rootNode.attachChild(dungeonRoot);'
)

rep(
    '        spawnWorld();\n        spawnFrontierPois();\n        restoreBuilds();',
    '        spawnWorld();\n        spawnFrontierPois();\n        spawnDungeonRuntime();\n        restoreBuilds();'
)

# Insert dungeon runtime directly after POI loot handling.
anchor = '''    private void spawnEnemy(Vector3f p, EnemyType type) {
        Node enemy = new Node("enemy");'''
insert = '''    private void ensureRuntimeDungeonCarved() {
        if (removedResources.contains(DUNGEON_GENERATED_FLAG)) return;
        for (Vector3f center : DungeonRules.carveCenters()) {
            terrain.dig(center, DungeonRules.CARVE_RADIUS, DungeonRules.CARVE_STRENGTH);
        }
        removedResources.add(DUNGEON_GENERATED_FLAG);
    }

    private void spawnDungeonRuntime() {
        dungeonRoot.detachAllChildren();
        if (!dungeonCleared()) {
            int index = 0;
            for (Vector3f spawn : DungeonRules.enemySpawns()) {
                float floor = dungeonFloor(spawn);
                Node enemy = spawnEnemy(new Vector3f(spawn.x, floor, spawn.z),
                        index++ % 2 == 0 ? EnemyType.SKELETON : EnemyType.GOBLIN);
                enemy.setUserData("dungeonEnemy", true);
            }
        }

        Vector3f chestPoint = DungeonRules.chestPoint();
        float chestFloor = dungeonFloor(chestPoint);
        Node chest = new Node("dungeon-chest");
        chest.setUserData("kind", "DUNGEON_CHEST");
        chest.setLocalTranslation(chestPoint.x, chestFloor, chestPoint.z);
        Geometry chestBody = new Geometry("dungeon-chest-body", new Box(0.72f, 0.48f, 0.52f));
        chestBody.setMaterial(lit(dungeonLooted()
                ? new ColorRGBA(0.16f, 0.14f, 0.13f, 1f)
                : dungeonCleared() ? new ColorRGBA(0.84f, 0.58f, 0.17f, 1f)
                : new ColorRGBA(0.32f, 0.22f, 0.12f, 1f)));
        chestBody.setLocalTranslation(0f, 0.52f, 0f);
        chest.attachChild(chestBody);
        dungeonRoot.attachChild(chest);

        Vector3f exitPoint = DungeonRules.exitPoint();
        float exitFloor = dungeonFloor(exitPoint);
        Node exit = new Node("dungeon-exit");
        exit.setUserData("kind", "DUNGEON_EXIT");
        exit.setLocalTranslation(exitPoint.x, exitFloor, exitPoint.z);
        Geometry exitStone = new Geometry("dungeon-exit-stone", new Box(0.68f, 1.18f, 0.26f));
        exitStone.setMaterial(lit(new ColorRGBA(0.24f, 0.49f, 0.62f, 1f)));
        exitStone.setLocalTranslation(0f, 1.18f, 0f);
        exit.attachChild(exitStone);
        Geometry rune = new Geometry("dungeon-exit-rune", new Sphere(8, 12, 0.31f));
        rune.setMaterial(unshaded(new ColorRGBA(0.22f, 0.88f, 1f, 1f)));
        rune.setLocalTranslation(0f, 1.25f, -0.31f);
        exit.attachChild(rune);
        dungeonRoot.attachChild(exit);
    }

    private float dungeonFloor(Vector3f point) {
        float floor = terrain.findFloor(point.x, point.z, DungeonRules.CENTER_Y + DungeonRules.CARVE_RADIUS + 1.2f,
                DungeonRules.CARVE_RADIUS * 2.7f);
        return Float.isFinite(floor) ? floor + 0.05f : DungeonRules.CENTER_Y - DungeonRules.CARVE_RADIUS + 0.8f;
    }

    private boolean dungeonCleared() { return removedResources.contains(DUNGEON_CLEARED_FLAG); }
    private boolean dungeonLooted() { return removedResources.contains(DUNGEON_LOOTED_FLAG); }

    private int countDungeonEnemies() {
        int count = 0;
        for (Spatial spatial : enemies.getChildren()) {
            if (spatial instanceof Node enemy && Boolean.TRUE.equals(enemy.getUserData("dungeonEnemy"))) count++;
        }
        return count;
    }

    private void enterDungeon(Node waystone) {
        Vector3f returnPoint = waystone.getLocalTranslation();
        dungeonReturnPoint = new Vector3f(returnPoint.x, returnPoint.y + 0.08f, returnPoint.z);
        Vector3f entry = DungeonRules.entryPoint();
        playerX = entry.x;
        playerZ = entry.z;
        footY = dungeonFloor(entry);
        velocityY = 0f;
        inDungeon = true;
        refreshTerrainStreaming(true);
        updateCameraPosition();
        announce(dungeonCleared() ? "The Waystone opens the cleared Delve." : "The Waystone pulls you into the Delve. Clear the chamber and claim its relic cache.");
    }

    private void leaveDungeon() {
        if (!inDungeon) return;
        playerX = dungeonReturnPoint.x;
        playerZ = dungeonReturnPoint.z;
        footY = Math.max(dungeonReturnPoint.y, terrain.surfaceHeight(playerX, playerZ) + 0.05f);
        velocityY = 0f;
        inDungeon = false;
        refreshTerrainStreaming(true);
        updateCameraPosition();
        announce("You return through the Waystone.");
    }

    private void lootDungeonChest() {
        if (dungeonLooted()) { announce("The Delve relic cache is empty."); return; }
        if (!DungeonRules.chestUnlocked(countDungeonEnemies(), dungeonCleared())) {
            announce("The relic cache is sealed while its guardians live.");
            return;
        }
        removedResources.add(DUNGEON_CLEARED_FLAG);
        removedResources.add(DUNGEON_LOOTED_FLAG);
        DungeonRules.Reward reward = DungeonRules.reward();
        wood += reward.wood(); stone += reward.stone(); berries += reward.berries();
        ironOre += reward.ironOre(); mistResin += reward.mistResin(); cinderShard += reward.cinderShard();
        spawnDungeonRuntime();
        announce("Delve cleared. Relic cache claimed: frontier materials recovered.");
    }

    private Node spawnEnemy(Vector3f p, EnemyType type) {
        Node enemy = new Node("enemy");'''
if anchor not in text:
    raise SystemExit('missing spawnEnemy anchor')
text = text.replace(anchor, insert, 1)

rep(
    '        enemies.attachChild(enemy);\n    }\n\n    private Material lit(ColorRGBA color) {',
    '        enemies.attachChild(enemy);\n        return enemy;\n    }\n\n    private Material lit(ColorRGBA color) {'
)

rep(
    '        Node poi = raycastNode(pois, 5.4f);\n        if (poi != null) { lootPoi(poi); return; }',
    '        Node poi = raycastNode(pois, 5.4f);\n'
    '        if (poi != null) {\n'
    '            FrontierPoiPlanner.PoiType type = FrontierPoiPlanner.PoiType.valueOf(poi.getUserData("poiType"));\n'
    '            String id = poi.getUserData("poiId");\n'
    '            if (type == FrontierPoiPlanner.PoiType.WAYSTONE_CACHE && lootedPois.contains(id)) { enterDungeon(poi); return; }\n'
    '            lootPoi(poi);\n'
    '            return;\n'
    '        }\n'
    '        Node dungeonObject = raycastNode(dungeonRoot, 5.4f);\n'
    '        if (dungeonObject != null) {\n'
    '            String kind = dungeonObject.getUserData("kind");\n'
    '            if ("DUNGEON_CHEST".equals(kind)) { lootDungeonChest(); return; }\n'
    '            if ("DUNGEON_EXIT".equals(kind)) { leaveDungeon(); return; }\n'
    '        }'
)

old_kill = '''            enemy.removeFromParent();
            kills++;
            announce((kind == CombatRules.AttackKind.HEAVY ? "Heavy strike defeats the enemy." : "Enemy defeated.")
                    + (drop.cinderShard() > 0 ? " + cinder shard" : ""));'''
new_kill = '''            boolean wasDungeonEnemy = Boolean.TRUE.equals(enemy.getUserData("dungeonEnemy"));
            enemy.removeFromParent();
            kills++;
            if (wasDungeonEnemy && countDungeonEnemies() == 0) {
                removedResources.add(DUNGEON_CLEARED_FLAG);
                announce("The last Delve guardian falls. The relic cache is unsealed.");
            } else {
                announce((kind == CombatRules.AttackKind.HEAVY ? "Heavy strike defeats the enemy." : "Enemy defeated.")
                        + (drop.cinderShard() > 0 ? " + cinder shard" : ""));
            }'''
rep(old_kill, new_kill)

rep(
    '        if (spawnClock > 85f && enemies.getQuantity() < 16) { spawnClock = 0f; spawnRoamingEnemy(); }',
    '        if (!inDungeon && spawnClock > 85f && enemies.getQuantity() < 16) { spawnClock = 0f; spawnRoamingEnemy(); }'
)

rep(
    '        if (lootedPois.size() < 2) return "Find and loot 2 frontier sites (" + lootedPois.size() + "/2)";\n        return "Sandbox open: master frontier gear, caches, caves, water, combat and building";',
    '        if (lootedPois.size() < 2) return "Find and loot 2 frontier sites (" + lootedPois.size() + "/2)";\n'
    '        if (!dungeonLooted()) return inDungeon ? "Clear the Delve guardians, claim the relic cache, then find the exit" : "Use a looted Waystone Cache again to enter the Delve";\n'
    '        return "Sandbox open: master frontier gear, caches, the Delve, caves, water, combat and building";'
)

rep(
    '        health = 100f; stamina = 100f; hunger = 70f; breath = 100f; velocityY = 0f;\n        playerX = 0f; playerZ = 0f;',
    '        health = 100f; stamina = 100f; hunger = 70f; breath = 100f; velocityY = 0f;\n'
    '        inDungeon = false;\n'
    '        playerX = 0f; playerZ = 0f;'
)

old_save = '''    private void saveGame() {
        SaveData data = new SaveData(seed, playerX, footY, playerZ, health, stamina, hunger, dayClock, wood, stone, berries,
                ironOre, mistResin, cinderShard, kills, bladeCrafted, equippedWeapon, EnumSet.copyOf(craftedWeapons),
                toolMode, buildType, buildYaw, brushRadius, terrain.encodeEdits(),
                new HashSet<>(removedResources), new HashSet<>(lootedPois), new ArrayList<>(builds));'''
new_save = '''    private void saveGame() {
        float saveX = inDungeon ? dungeonReturnPoint.x : playerX;
        float saveY = inDungeon ? dungeonReturnPoint.y : footY;
        float saveZ = inDungeon ? dungeonReturnPoint.z : playerZ;
        SaveData data = new SaveData(seed, saveX, saveY, saveZ, health, stamina, hunger, dayClock, wood, stone, berries,
                ironOre, mistResin, cinderShard, kills, bladeCrafted, equippedWeapon, EnumSet.copyOf(craftedWeapons),
                toolMode, buildType, buildYaw, brushRadius, terrain.encodeEdits(),
                new HashSet<>(removedResources), new HashSet<>(lootedPois), new ArrayList<>(builds));'''
rep(old_save, new_save)

path.write_text(text, encoding='utf-8')
print('integrated runtime dungeon into SamaheimCaveGame')
