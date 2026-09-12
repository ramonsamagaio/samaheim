from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

def rep(old, new):
    global text
    if old not in text:
        raise SystemExit('anchor not found: ' + old[:200])
    text = text.replace(old, new, 1)

rep('import com.samaheim.world.CaveCharacterPhysics;\n', 'import com.samaheim.world.CaveCharacterPhysics;\nimport com.samaheim.world.FrontierPoiPlanner;\n')
rep('    private final Node enemies = new Node("enemies");\n', '    private final Node enemies = new Node("enemies");\n    private final Node pois = new Node("frontier-pois");\n')
rep('    private final Set<String> removedResources = new HashSet<>();\n', '    private final Set<String> removedResources = new HashSet<>();\n    private final Set<String> lootedPois = new HashSet<>();\n')
rep('        rootNode.attachChild(enemies);\n', '        rootNode.attachChild(enemies);\n        rootNode.attachChild(pois);\n')
rep('        spawnWorld();\n        restoreBuilds();\n', '        spawnWorld();\n        spawnFrontierPois();\n        restoreBuilds();\n')
rep('        removedResources.addAll(save.removed);\n        builds.addAll(save.builds);\n', '        removedResources.addAll(save.removed);\n        lootedPois.addAll(save.lootedPois);\n        builds.addAll(save.builds);\n')

anchor = '''    private void spawnEnemy(Vector3f p, EnemyType type) {
'''
insert = '''    private void spawnFrontierPois() {
        for (FrontierPoiPlanner.Poi poi : FrontierPoiPlanner.plan(seed, WORLD_HALF)) {
            float y = terrain.surfaceHeight(poi.x(), poi.z());
            if (y < SEA_LEVEL + 0.2f) continue;
            Node site = new Node("poi-" + poi.id());
            site.setUserData("kind", "POI");
            site.setUserData("poiId", poi.id());
            site.setUserData("poiType", poi.type().name());
            site.setLocalTranslation(poi.x(), y, poi.z());

            ColorRGBA stoneColor = switch (poi.type()) {
                case FORGE_RUIN -> new ColorRGBA(0.42f, 0.31f, 0.23f, 1f);
                case FEN_ALTAR -> new ColorRGBA(0.17f, 0.39f, 0.30f, 1f);
                case CINDER_SHRINE -> new ColorRGBA(0.46f, 0.18f, 0.10f, 1f);
                case WAYSTONE_CACHE -> new ColorRGBA(0.29f, 0.31f, 0.39f, 1f);
            };
            Geometry plinth = new Geometry("poi-plinth", new Box(2.3f, 0.22f, 2.3f));
            plinth.setMaterial(lit(stoneColor));
            plinth.setLocalTranslation(0f, 0.22f, 0f);
            site.attachChild(plinth);
            for (int i = 0; i < 4; i++) {
                float sx = (i & 1) == 0 ? -1.75f : 1.75f;
                float sz = (i & 2) == 0 ? -1.75f : 1.75f;
                Geometry pillar = new Geometry("poi-pillar", new Box(0.24f, 1.15f + (i % 2) * 0.35f, 0.24f));
                pillar.setMaterial(lit(stoneColor.mult(0.82f)));
                pillar.setLocalTranslation(sx, 1.15f, sz);
                site.attachChild(pillar);
            }
            Geometry cache = new Geometry("poi-cache", new Box(0.56f, 0.38f, 0.42f));
            ColorRGBA cacheColor = lootedPois.contains(poi.id())
                    ? new ColorRGBA(0.18f, 0.16f, 0.14f, 1f)
                    : new ColorRGBA(0.74f, 0.49f, 0.16f, 1f);
            cache.setMaterial(lit(cacheColor));
            cache.setLocalTranslation(0f, 0.62f, 0f);
            site.attachChild(cache);
            pois.attachChild(site);

            if (!lootedPois.contains(poi.id())) {
                EnemyType guardian = poi.type() == FrontierPoiPlanner.PoiType.CINDER_SHRINE ? EnemyType.SKELETON : EnemyType.GOBLIN;
                spawnEnemy(new Vector3f(poi.x() + 3.2f, terrain.surfaceHeight(poi.x() + 3.2f, poi.z()), poi.z()), guardian);
            }
        }
    }

    private void lootPoi(Node site) {
        String id = site.getUserData("poiId");
        if (id == null) return;
        if (lootedPois.contains(id)) { announce("This frontier cache is empty."); return; }
        FrontierPoiPlanner.PoiType type = FrontierPoiPlanner.PoiType.valueOf(site.getUserData("poiType"));
        FrontierPoiPlanner.Reward reward = FrontierPoiPlanner.reward(type);
        wood += reward.wood(); stone += reward.stone(); berries += reward.berries();
        ironOre += reward.ironOre(); mistResin += reward.mistResin(); cinderShard += reward.cinderShard();
        lootedPois.add(id);
        Spatial cacheSpatial = site.getChild("poi-cache");
        if (cacheSpatial instanceof Geometry cache) {
            cache.setMaterial(lit(new ColorRGBA(0.18f, 0.16f, 0.14f, 1f)));
        }
        announce("Looted " + type.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ". Frontier materials recovered.");
    }

'''
rep(anchor, insert + anchor)

rep('''        Integer buildIndex = raycastBuild(4.8f);
''', '''        Node poi = raycastNode(pois, 5.4f);
        if (poi != null) { lootPoi(poi); return; }
        Integer buildIndex = raycastBuild(4.8f);
''')

rep('''        if (craftedWeapons.size() < 2) return "Explore a frontier region and craft an advanced weapon [2-4]";
        return "Sandbox open: master frontier gear, caves, water, combat and building";
''', '''        if (craftedWeapons.size() < 2) return "Explore a frontier region and craft an advanced weapon [2-4]";
        if (lootedPois.size() < 2) return "Find and loot 2 frontier sites (" + lootedPois.size() + "/2)";
        return "Sandbox open: master frontier gear, caches, caves, water, combat and building";
''')

rep('''                toolMode, buildType, buildYaw, brushRadius, terrain.encodeEdits(),
                new HashSet<>(removedResources), new ArrayList<>(builds));
''', '''                toolMode, buildType, buildYaw, brushRadius, terrain.encodeEdits(),
                new HashSet<>(removedResources), new HashSet<>(lootedPois), new ArrayList<>(builds));
''')
rep('''        private final Set<String> removed;
        private final List<BuildRecord> builds;
''', '''        private final Set<String> removed;
        private final Set<String> lootedPois;
        private final List<BuildRecord> builds;
''')
rep('''                 ToolMode toolMode, BuildType buildType, float buildYaw, float brushRadius, String edits, Set<String> removed, List<BuildRecord> builds) {
''', '''                 ToolMode toolMode, BuildType buildType, float buildYaw, float brushRadius, String edits,
                 Set<String> removed, Set<String> lootedPois, List<BuildRecord> builds) {
''')
rep('''            this.toolMode = toolMode; this.buildType = buildType;
            this.buildYaw = buildYaw; this.brushRadius = brushRadius; this.edits = edits; this.removed = removed; this.builds = builds;
''', '''            this.toolMode = toolMode; this.buildType = buildType;
            this.buildYaw = buildYaw; this.brushRadius = brushRadius; this.edits = edits; this.removed = removed;
            this.lootedPois = lootedPois; this.builds = builds;
''')
rep('''                List<BuildRecord> builds = new ArrayList<>();
''', '''                Set<String> lootedPois = new HashSet<>();
                String lootedText = p.getProperty("lootedPois", "");
                if (!lootedText.isBlank()) for (String id : lootedText.split(";")) if (!id.isBlank()) lootedPois.add(id);
                List<BuildRecord> builds = new ArrayList<>();
''')
rep('''                        p.getProperty("edits", ""), removed, builds);
''', '''                        p.getProperty("edits", ""), removed, lootedPois, builds);
''')
rep('''            p.setProperty("brushRadius", Float.toString(brushRadius)); p.setProperty("edits", edits); p.setProperty("removed", String.join(";", removed));
''', '''            p.setProperty("brushRadius", Float.toString(brushRadius)); p.setProperty("edits", edits); p.setProperty("removed", String.join(";", removed));
            p.setProperty("lootedPois", String.join(";", lootedPois));
''')

path.write_text(text, encoding='utf-8')
print('frontier POIs integrated')
