from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

if 'import com.samaheim.game.NightCampRules;' in text:
    print('night camp runtime already integrated')
    raise SystemExit(0)

def rep(old: str, new: str) -> None:
    global text
    if old not in text:
        raise SystemExit('missing anchor:\n' + old[:280])
    text = text.replace(old, new, 1)

rep(
    'import com.samaheim.game.EncounterRules;\nimport com.samaheim.game.EquipmentRules;',
    'import com.samaheim.game.EncounterRules;\nimport com.samaheim.game.EquipmentRules;\nimport com.samaheim.game.NightCampRules;'
)

rep(
    '    private static final String DUNGEON_LOOTED_FLAG = "__dungeon_looted__";\n',
    '    private static final String DUNGEON_LOOTED_FLAG = "__dungeon_looted__";\n'
    '    private static final String RESTED_AT_CAMPFIRE_FLAG = "__rested_at_campfire__";\n'
)

rep(
'''        lantern.setPosition(cam.getLocation().clone());
        if (!inDungeon && spawnClock > 85f && enemies.getQuantity() < 16) { spawnClock = 0f; spawnRoamingEnemy(); }
        if (saveClock > 75f) { saveClock = 0f; saveGame(); }''',
'''        lantern.setPosition(cam.getLocation().clone());
        boolean night = !inDungeon && NightCampRules.isNight(dayClock);
        boolean warmed = !inDungeon && NightCampRules.warmedByCampfire(nearestCampfireDistance());
        float spawnInterval = NightCampRules.roamingSpawnInterval(night, warmed);
        int enemyCap = night && !warmed ? 20 : 16;
        if (!inDungeon && spawnClock > spawnInterval && enemies.getQuantity() < enemyCap) { spawnClock = 0f; spawnRoamingEnemy(); }
        if (saveClock > 75f) { saveClock = 0f; saveGame(); }'''
)

rep(
'''        stamina = WaterPhysics.nextStamina(stamina, swimming, moving, pushing, tpf);
        updateCameraPosition();''',
'''        stamina = WaterPhysics.nextStamina(stamina, swimming, moving, pushing, tpf);
        if (!inDungeon && !swimming) {
            boolean night = NightCampRules.isNight(dayClock);
            boolean warmed = NightCampRules.warmedByCampfire(nearestCampfireDistance());
            stamina = Math.min(100f, stamina + NightCampRules.staminaRecoveryBonus(night, warmed) * tpf);
        }
        updateCameraPosition();'''
)

rep(
'''    private void updateSurvival(float tpf) {
        hunger = Math.max(0f, hunger - 0.28f * tpf);
        if (hunger <= 0f) health = Math.max(0f, health - 1.0f * tpf);''',
'''    private void updateSurvival(float tpf) {
        boolean night = !inDungeon && NightCampRules.isNight(dayClock);
        boolean warmed = !inDungeon && NightCampRules.warmedByCampfire(nearestCampfireDistance());
        float hungerMultiplier = NightCampRules.hungerDrainMultiplier(night, warmed);
        hunger = Math.max(0f, hunger - 0.28f * hungerMultiplier * tpf);
        if (hunger <= 0f) health = Math.max(0f, health - 1.0f * tpf);'''
)

rep(
'''        Integer buildIndex = raycastBuild(4.8f);
        if (buildIndex != null && buildIndex >= 0 && buildIndex < builds.size() && builds.get(buildIndex).type == BuildType.DOOR) {
            BuildRecord record = builds.get(buildIndex);
            builds.set(buildIndex, record.withOpen(!record.open));
            rebuildStructures();
            announce(record.open ? "Door closed." : "Door opened.");
            return;
        }
        announce("Nothing usable in reach.");''',
'''        Integer buildIndex = raycastBuild(4.8f);
        if (buildIndex != null && buildIndex >= 0 && buildIndex < builds.size()) {
            BuildRecord record = builds.get(buildIndex);
            if (record.type == BuildType.DOOR) {
                builds.set(buildIndex, record.withOpen(!record.open));
                rebuildStructures();
                announce(record.open ? "Door closed." : "Door opened.");
                return;
            }
            if (record.type == BuildType.CAMPFIRE) {
                restAtCampfire(record);
                return;
            }
        }
        announce("Nothing usable in reach.");'''
)

anchor = '''    private void eat() {
        if (berries <= 0) { announce("No berries."); return; }
        berries--;
        hunger = Math.min(100f, hunger + 26f);
        health = Math.min(100f, health + 12f);
        announce("Berry eaten.");
    }

    private void spawnRoamingEnemy() {'''
insert = '''    private void eat() {
        if (berries <= 0) { announce("No berries."); return; }
        berries--;
        hunger = Math.min(100f, hunger + 26f);
        health = Math.min(100f, health + 12f);
        announce("Berry eaten.");
    }

    private float nearestCampfireDistance() {
        float nearest = Float.POSITIVE_INFINITY;
        for (BuildRecord record : builds) {
            if (record.type != BuildType.CAMPFIRE) continue;
            float dx = record.x - playerX;
            float dz = record.z - playerZ;
            nearest = Math.min(nearest, (float) Math.sqrt(dx * dx + dz * dz));
        }
        return nearest;
    }

    private boolean nearbyThreat() {
        for (Spatial spatial : enemies.getChildren()) {
            Vector3f p = spatial.getLocalTranslation();
            float dx = p.x - playerX;
            float dz = p.z - playerZ;
            float dy = Math.abs(p.y - footY);
            if (dx * dx + dz * dz <= 12f * 12f && dy < 4f) return true;
        }
        return false;
    }

    private void restAtCampfire(BuildRecord campfire) {
        float dx = campfire.x - playerX;
        float dz = campfire.z - playerZ;
        float distance = (float) Math.sqrt(dx * dx + dz * dz);
        boolean night = NightCampRules.isNight(dayClock);
        boolean warmed = NightCampRules.warmedByCampfire(distance);
        boolean threatened = nearbyThreat();
        if (!NightCampRules.canRest(night, warmed, threatened)) {
            if (!night) announce("Rest becomes available at night.");
            else if (threatened) announce("Enemies are too close to rest.");
            else announce("Move closer to the campfire to rest.");
            return;
        }
        NightCampRules.RestResult result = NightCampRules.rest(health, stamina, hunger);
        health = result.health();
        stamina = result.stamina();
        hunger = result.hunger();
        dayClock = result.dayClock();
        spawnClock = 0f;
        removedResources.add(RESTED_AT_CAMPFIRE_FLAG);
        saveGame();
        announce("You rest by the fire. Morning breaks, wounds mend, and hunger still matters.");
    }

    private void spawnRoamingEnemy() {'''
rep(anchor, insert)

rep(
'''        String detail;
        if (swimming) detail = "SWIMMING • Space rise   Ctrl dive   Shift push     |     stamina drains in deep water";
        else if (wading) detail = "WADING • movement slowed     |     RMB/G terrain   B build   Q place   X remove";
        else detail = "LMB light  MMB heavy   1-4 craft/equip weapons   |   RMB/G terrain   B build   Q place";''',
'''        String detail;
        boolean night = !inDungeon && NightCampRules.isNight(dayClock);
        boolean warmed = !inDungeon && NightCampRules.warmedByCampfire(nearestCampfireDistance());
        if (swimming) detail = "SWIMMING • Space rise   Ctrl dive   Shift push     |     stamina drains in deep water";
        else if (night && warmed) detail = "NIGHT • CAMPFIRE WARMTH • safer spawns + stamina recovery • aim at fire + E to rest";
        else if (night) detail = "NIGHT • EXPOSED • hostile roaming pressure increased • build a campfire for safety";
        else if (wading) detail = "WADING • movement slowed     |     RMB/G terrain   B build   Q place   X remove";
        else detail = "LMB light  MMB heavy   1-4 craft/equip weapons   |   RMB/G terrain   B build   Q place";'''
)

rep(
'''        if (!dungeonLooted()) return inDungeon ? "Clear the Delve guardians, claim the relic cache, then find the exit" : "Use a looted Waystone Cache again to enter the Delve";
        return "Sandbox open: master frontier gear, caches, the Delve, caves, water, combat and building";''',
'''        if (!dungeonLooted()) return inDungeon ? "Clear the Delve guardians, claim the relic cache, then find the exit" : "Use a looted Waystone Cache again to enter the Delve";
        if (!removedResources.contains(RESTED_AT_CAMPFIRE_FLAG)) return "Build a campfire, survive until night, then aim at the fire and press E to rest";
        return "Sandbox open: master frontier gear, caches, the Delve, night survival, caves, water, combat and building";'''
)

path.write_text(text, encoding='utf-8')
print('integrated night pressure and campfire rest into SamaheimCaveGame')
