from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

if 'import com.samaheim.game.ResourceRegrowthRules;' in text:
    print('resource regrowth already integrated')
    raise SystemExit(0)

def rep(old: str, new: str) -> None:
    global text
    if old not in text:
        raise SystemExit('missing anchor:\n' + old[:300])
    text = text.replace(old, new, 1)

rep(
    'import com.samaheim.game.NightCampRules;\n',
    'import com.samaheim.game.NightCampRules;\nimport com.samaheim.game.ResourceRegrowthRules;\n'
)

rep(
    '    private float dayClock = 0.18f;\n',
    '    private float dayClock = 0.18f;\n    private long dawnIndex;\n'
)

rep(
    '        dayClock = Math.max(0f, Math.min(1f, save.dayClock));\n',
    '        dayClock = Math.max(0f, Math.min(1f, save.dayClock));\n        dawnIndex = Math.max(0L, save.dawnIndex);\n'
)

old_spawn_world = '''    private void spawnWorld() {
        Random random = new Random(seed ^ 0x51A9L);
        for (int i = 0; i < 312; i++) spawnResource("tree-" + i, ResourceType.TREE, random, 16f);
        for (int i = 0; i < 224; i++) spawnResource("rock-" + i, ResourceType.ROCK, random, 12f);
        for (int i = 0; i < 120; i++) spawnResource("berry-" + i, ResourceType.BERRY, random, 10f);
        for (int i = 0; i < 14; i++) {
            Vector3f point = randomSurfacePoint(random, 24f);
            WorldMath.Region region = WorldMath.region(seed, point.x, point.z);
            spawnEnemy(point, enemyType(EncounterRules.forRegion(region, random.nextFloat())));
        }
    }

    private void spawnResource(String id, ResourceType type, Random random, float safeRadius) {
        if (removedResources.contains(id)) return;
        Vector3f p = randomSurfacePoint(random, safeRadius);
        float scale = random.nextFloat(0.82f, 1.28f);'''
new_spawn_world = '''    private void spawnWorld() {
        Random random = new Random(seed ^ 0x51A9L);
        spawnResources(random);
        for (int i = 0; i < 14; i++) {
            Vector3f point = randomSurfacePoint(random, 24f);
            WorldMath.Region region = WorldMath.region(seed, point.x, point.z);
            spawnEnemy(point, enemyType(EncounterRules.forRegion(region, random.nextFloat())));
        }
    }

    private void spawnResources(Random random) {
        for (int i = 0; i < 312; i++) spawnResource("tree-" + i, ResourceType.TREE, random, 16f);
        for (int i = 0; i < 224; i++) spawnResource("rock-" + i, ResourceType.ROCK, random, 12f);
        for (int i = 0; i < 120; i++) spawnResource("berry-" + i, ResourceType.BERRY, random, 10f);
    }

    private void rebuildResources() {
        resources.detachAllChildren();
        Random random = new Random(seed ^ 0x51A9L);
        spawnResources(random);
    }

    private void spawnResource(String id, ResourceType type, Random random, float safeRadius) {
        Vector3f p = randomSurfacePoint(random, safeRadius);
        float scale = random.nextFloat(0.82f, 1.28f);
        if (removedResources.contains(id)) return;'''
rep(old_spawn_world, new_spawn_world)

old_daynight = '''    private void updateDayNight(float tpf) {
        dayClock = (dayClock + tpf / DAY_SECONDS) % 1f;
        float angle = dayClock * FastMath.TWO_PI;'''
new_daynight = '''    private void updateDayNight(float tpf) {
        float previousClock = dayClock;
        dayClock = (dayClock + tpf / DAY_SECONDS) % 1f;
        if (ResourceRegrowthRules.crossedDawn(previousClock, dayClock)) {
            dawnIndex++;
            regrowHarvestedResources();
        }
        float angle = dayClock * FastMath.TWO_PI;'''
rep(old_daynight, new_daynight)

anchor = '''    private void spawnRoamingEnemy() {
        Random random = new Random(seed ^ Float.floatToIntBits(dayClock) ^ kills * 131L);'''
insert = '''    private void regrowHarvestedResources() {
        List<String> returning = ResourceRegrowthRules.selectForRegrowth(removedResources, seed, dawnIndex);
        if (returning.isEmpty()) return;
        removedResources.removeAll(returning);
        rebuildResources();
        if (!inDungeon) announce("Dawn renews the wilds. " + returning.size() + " harvested resource sites return.");
    }

    private void spawnRoamingEnemy() {
        Random random = new Random(seed ^ Float.floatToIntBits(dayClock) ^ kills * 131L);'''
rep(anchor, insert)

old_save_call = '''        SaveData data = new SaveData(seed, saveX, saveY, saveZ, health, stamina, hunger, dayClock, wood, stone, berries,
                ironOre, mistResin, cinderShard, kills, bladeCrafted, equippedWeapon, EnumSet.copyOf(craftedWeapons),'''
new_save_call = '''        SaveData data = new SaveData(seed, saveX, saveY, saveZ, health, stamina, hunger, dayClock, dawnIndex, wood, stone, berries,
                ironOre, mistResin, cinderShard, kills, bladeCrafted, equippedWeapon, EnumSet.copyOf(craftedWeapons),'''
rep(old_save_call, new_save_call)

rep(
    '        private final float dayClock;\n        private final int wood;',
    '        private final float dayClock;\n        private final long dawnIndex;\n        private final int wood;'
)

old_ctor_sig = '''        SaveData(long seed, float x, float footY, float z, float health, float stamina, float hunger, float dayClock,
                 int wood, int stone, int berries, int ironOre, int mistResin, int cinderShard, int kills, boolean blade,'''
new_ctor_sig = '''        SaveData(long seed, float x, float footY, float z, float health, float stamina, float hunger, float dayClock, long dawnIndex,
                 int wood, int stone, int berries, int ironOre, int mistResin, int cinderShard, int kills, boolean blade,'''
rep(old_ctor_sig, new_ctor_sig)

rep(
    '            this.hunger = hunger; this.dayClock = dayClock; this.wood = wood; this.stone = stone; this.berries = berries;\n',
    '            this.hunger = hunger; this.dayClock = dayClock; this.dawnIndex = dawnIndex; this.wood = wood; this.stone = stone; this.berries = berries;\n'
)

old_load = '''                        Float.parseFloat(p.getProperty("health", "100")), Float.parseFloat(p.getProperty("stamina", "100")),
                        Float.parseFloat(p.getProperty("hunger", "100")), Float.parseFloat(p.getProperty("dayClock", "0.18")),
                        Integer.parseInt(p.getProperty("wood", "0")), Integer.parseInt(p.getProperty("stone", "0")),'''
new_load = '''                        Float.parseFloat(p.getProperty("health", "100")), Float.parseFloat(p.getProperty("stamina", "100")),
                        Float.parseFloat(p.getProperty("hunger", "100")), Float.parseFloat(p.getProperty("dayClock", "0.18")),
                        Long.parseLong(p.getProperty("dawnIndex", "0")),
                        Integer.parseInt(p.getProperty("wood", "0")), Integer.parseInt(p.getProperty("stone", "0")),'''
rep(old_load, new_load)

rep(
    '            p.setProperty("dayClock", Float.toString(dayClock)); p.setProperty("wood", Integer.toString(wood)); p.setProperty("stone", Integer.toString(stone));\n',
    '            p.setProperty("dayClock", Float.toString(dayClock)); p.setProperty("dawnIndex", Long.toString(dawnIndex));\n            p.setProperty("wood", Integer.toString(wood)); p.setProperty("stone", Integer.toString(stone));\n'
)

path.write_text(text, encoding='utf-8')
print('integrated deterministic resource regrowth and save stability')
