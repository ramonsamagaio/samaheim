from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

def rep(old, new):
    global text
    if old not in text:
        raise SystemExit('anchor not found: ' + old[:180])
    text = text.replace(old, new, 1)

rep('import com.samaheim.game.CombatRules;\n', 'import com.samaheim.game.CombatRules;\nimport com.samaheim.game.EquipmentRules;\n')
rep('import com.samaheim.world.WaterPhysics;\n', 'import com.samaheim.world.WaterPhysics;\nimport com.samaheim.world.WorldMath;\n')
rep('import java.util.HashSet;\n', 'import java.util.EnumSet;\nimport java.util.HashSet;\n')

rep('    private int berries;\n    private int kills;\n',
    '    private int berries;\n    private int ironOre;\n    private int mistResin;\n    private int cinderShard;\n    private int kills;\n    private final Set<EquipmentRules.Weapon> craftedWeapons = EnumSet.noneOf(EquipmentRules.Weapon.class);\n    private EquipmentRules.Weapon equippedWeapon = EquipmentRules.Weapon.FISTS;\n')

rep('        berries = Math.max(0, save.berries);\n        kills = Math.max(0, save.kills);\n        bladeCrafted = save.blade;\n',
    '        berries = Math.max(0, save.berries);\n        ironOre = Math.max(0, save.ironOre);\n        mistResin = Math.max(0, save.mistResin);\n        cinderShard = Math.max(0, save.cinderShard);\n        kills = Math.max(0, save.kills);\n        craftedWeapons.addAll(save.craftedWeapons);\n        if (save.blade) craftedWeapons.add(EquipmentRules.Weapon.WANDERERS_BLADE);\n        equippedWeapon = save.equippedWeapon;\n        if (equippedWeapon != EquipmentRules.Weapon.FISTS && !craftedWeapons.contains(equippedWeapon)) equippedWeapon = EquipmentRules.Weapon.FISTS;\n        bladeCrafted = craftedWeapons.contains(EquipmentRules.Weapon.WANDERERS_BLADE);\n')

rep('        inputManager.addMapping("CraftBlade", new KeyTrigger(KeyInput.KEY_1));\n',
    '        inputManager.addMapping("CraftBlade", new KeyTrigger(KeyInput.KEY_1));\n        inputManager.addMapping("Weapon2", new KeyTrigger(KeyInput.KEY_2));\n        inputManager.addMapping("Weapon3", new KeyTrigger(KeyInput.KEY_3));\n        inputManager.addMapping("Weapon4", new KeyTrigger(KeyInput.KEY_4));\n')
rep('                "Terraform", "ToolMode", "BrushSmaller", "BrushLarger", "CraftBlade", "BuildMode", "RotateBuild",\n',
    '                "Terraform", "ToolMode", "BrushSmaller", "BrushLarger", "CraftBlade", "Weapon2", "Weapon3", "Weapon4", "BuildMode", "RotateBuild",\n')
rep('            case "CraftBlade" -> { if (isPressed) craftBlade(); }\n',
    '            case "CraftBlade" -> { if (isPressed) craftOrEquip(EquipmentRules.Weapon.WANDERERS_BLADE); }\n'
    '            case "Weapon2" -> { if (isPressed) craftOrEquip(EquipmentRules.Weapon.HIGHLAND_MAUL); }\n'
    '            case "Weapon3" -> { if (isPressed) craftOrEquip(EquipmentRules.Weapon.MIST_PIKE); }\n'
    '            case "Weapon4" -> { if (isPressed) craftOrEquip(EquipmentRules.Weapon.CINDER_BLADE); }\n')

old_gather = '''            if ("TREE".equals(kind)) wood += 3;
            else if ("ROCK".equals(kind)) stone += 2;
            else if ("BERRY".equals(kind)) berries += 2;
            else return;
            removedResources.add(id);
            resource.removeFromParent();
            announce("Gathered " + kind.toLowerCase(Locale.ROOT) + ".");
'''
new_gather = '''            if ("TREE".equals(kind)) wood += 3;
            else if ("ROCK".equals(kind)) stone += 2;
            else if ("BERRY".equals(kind)) berries += 2;
            else return;
            Vector3f resourcePosition = resource.getLocalTranslation();
            EquipmentRules.GatherKind gatherKind = EquipmentRules.GatherKind.valueOf(kind);
            EquipmentRules.MaterialYield yield = EquipmentRules.gatherYield(WorldMath.region(seed, resourcePosition.x, resourcePosition.z), gatherKind);
            ironOre += yield.ironOre();
            mistResin += yield.mistResin();
            cinderShard += yield.cinderShard();
            removedResources.add(id);
            resource.removeFromParent();
            String bonus = yield.ironOre() > 0 ? " + iron ore" : yield.mistResin() > 0 ? " + mist resin" : "";
            announce("Gathered " + kind.toLowerCase(Locale.ROOT) + bonus + ".");
'''
rep(old_gather, new_gather)

old_attack_start = '''        CombatRules.PlayerAttack attack = CombatRules.playerAttack(bladeCrafted, kind);
        if (!CombatRules.canAttack(stamina, attackCooldown, attack)) {
'''
new_attack_start = '''        boolean armed = equippedWeapon != EquipmentRules.Weapon.FISTS;
        CombatRules.PlayerAttack baseAttack = CombatRules.playerAttack(armed, kind);
        EquipmentRules.AttackTuning tuning = EquipmentRules.tuning(equippedWeapon);
        CombatRules.PlayerAttack attack = new CombatRules.PlayerAttack(
                baseAttack.damage() * tuning.damageMultiplier(),
                baseAttack.range() + tuning.rangeBonus(),
                baseAttack.staminaCost() * tuning.staminaMultiplier(),
                baseAttack.cooldownSeconds() * tuning.cooldownMultiplier(),
                baseAttack.staggerSeconds() * tuning.staggerMultiplier());
        if (!CombatRules.canAttack(stamina, attackCooldown, attack)) {
'''
rep(old_attack_start, new_attack_start)

rep('''        if (hp <= 0f) {
            enemy.removeFromParent();
            kills++;
            announce(kind == CombatRules.AttackKind.HEAVY ? "Heavy strike defeats the enemy." : "Enemy defeated.");
''', '''        if (hp <= 0f) {
            Vector3f defeatedAt = enemy.getLocalTranslation().clone();
            EnemyType defeatedType = EnemyType.valueOf(enemy.getUserData("enemyType"));
            EquipmentRules.MaterialYield drop = EquipmentRules.enemyDrop(WorldMath.region(seed, defeatedAt.x, defeatedAt.z),
                    defeatedType == EnemyType.GOBLIN ? EquipmentRules.EnemyKind.GOBLIN : EquipmentRules.EnemyKind.GRAVEBORN);
            cinderShard += drop.cinderShard();
            enemy.removeFromParent();
            kills++;
            announce((kind == CombatRules.AttackKind.HEAVY ? "Heavy strike defeats the enemy." : "Enemy defeated.")
                    + (drop.cinderShard() > 0 ? " + cinder shard" : ""));
''')

old_craft = '''    private void craftBlade() {
        if (bladeCrafted) { announce("You already carry the Wanderer's Blade."); return; }
        if (wood < 8 || stone < 4) { announce("Blade requires 8 wood + 4 stone."); return; }
        wood -= 8; stone -= 4; bladeCrafted = true;
        announce("Wanderer's Blade crafted.");
    }
'''
new_craft = '''    private void craftOrEquip(EquipmentRules.Weapon weapon) {
        if (craftedWeapons.contains(weapon)) {
            equippedWeapon = weapon;
            announce("Equipped " + weapon.label() + ".");
            return;
        }
        EquipmentRules.Materials materials = new EquipmentRules.Materials(wood, stone, ironOre, mistResin, cinderShard);
        if (!EquipmentRules.canCraft(weapon, materials)) {
            EquipmentRules.Recipe r = EquipmentRules.recipe(weapon);
            announce(weapon.label() + " needs " + r.wood() + " wood, " + r.stone() + " stone, "
                    + r.ironOre() + " iron, " + r.mistResin() + " resin, " + r.cinderShard() + " cinder.");
            return;
        }
        EquipmentRules.Materials after = EquipmentRules.spend(weapon, materials);
        wood = after.wood(); stone = after.stone(); ironOre = after.ironOre(); mistResin = after.mistResin(); cinderShard = after.cinderShard();
        craftedWeapons.add(weapon);
        equippedWeapon = weapon;
        bladeCrafted = craftedWeapons.contains(EquipmentRules.Weapon.WANDERERS_BLADE);
        announce(weapon.label() + " crafted and equipped.");
    }
'''
rep(old_craft, new_craft)

rep('        String resourcesText = "WOOD  " + wood + "    STONE  " + stone + "    BERRIES  " + berries + "    KILLS  " + kills;\n',
    '        String resourcesText = "WOOD " + wood + "  STONE " + stone + "  IRON " + ironOre + "  RESIN " + mistResin + "  CINDER " + cinderShard + "  KILLS " + kills;\n')
rep('        String tool = "TERRAIN • " + toolMode.label.toUpperCase(Locale.ROOT) + " • " + String.format(Locale.ROOT, "%.1fm", brushRadius);\n',
    '        String tool = equippedWeapon.label().toUpperCase(Locale.ROOT) + "  |  TERRAIN " + toolMode.label.toUpperCase(Locale.ROOT) + " " + String.format(Locale.ROOT, "%.1fm", brushRadius);\n')
rep('        else detail = "LMB light attack   MMB heavy attack     |     RMB/G terrain   T mode   B build   Q place";\n',
    '        else detail = "LMB light  MMB heavy   1-4 craft/equip weapons   |   RMB/G terrain   B build   Q place";\n')
rep('        if (!bladeCrafted) return "Craft the Wanderer\'s Blade [1]";\n        if (kills < 4) return "Defeat 4 creatures (" + kills + "/4)";\n        return "Sandbox open: explore the larger streamed frontier, cross water, excavate caves and build";\n',
    '        if (!craftedWeapons.contains(EquipmentRules.Weapon.WANDERERS_BLADE)) return "Craft the Wanderer\'s Blade [1]";\n        if (kills < 4) return "Defeat 4 creatures (" + kills + "/4)";\n        if (craftedWeapons.size() < 2) return "Explore a frontier region and craft an advanced weapon [2-4]";\n        return "Sandbox open: master frontier gear, caves, water, combat and building";\n')

rep('''        SaveData data = new SaveData(seed, playerX, footY, playerZ, health, stamina, hunger, dayClock, wood, stone, berries,
                kills, bladeCrafted, toolMode, buildType, buildYaw, brushRadius, terrain.encodeEdits(),
                new HashSet<>(removedResources), new ArrayList<>(builds));
''', '''        SaveData data = new SaveData(seed, playerX, footY, playerZ, health, stamina, hunger, dayClock, wood, stone, berries,
                ironOre, mistResin, cinderShard, kills, bladeCrafted, equippedWeapon, EnumSet.copyOf(craftedWeapons),
                toolMode, buildType, buildYaw, brushRadius, terrain.encodeEdits(),
                new HashSet<>(removedResources), new ArrayList<>(builds));
''')

rep('''        private final int berries;
        private final int kills;
        private final boolean blade;
''', '''        private final int berries;
        private final int ironOre;
        private final int mistResin;
        private final int cinderShard;
        private final int kills;
        private final boolean blade;
        private final EquipmentRules.Weapon equippedWeapon;
        private final Set<EquipmentRules.Weapon> craftedWeapons;
''')

rep('''        SaveData(long seed, float x, float footY, float z, float health, float stamina, float hunger, float dayClock,
                 int wood, int stone, int berries, int kills, boolean blade, ToolMode toolMode, BuildType buildType,
                 float buildYaw, float brushRadius, String edits, Set<String> removed, List<BuildRecord> builds) {
            this.seed = seed; this.x = x; this.footY = footY; this.z = z; this.health = health; this.stamina = stamina;
            this.hunger = hunger; this.dayClock = dayClock; this.wood = wood; this.stone = stone; this.berries = berries;
            this.kills = kills; this.blade = blade; this.toolMode = toolMode; this.buildType = buildType;
''', '''        SaveData(long seed, float x, float footY, float z, float health, float stamina, float hunger, float dayClock,
                 int wood, int stone, int berries, int ironOre, int mistResin, int cinderShard, int kills, boolean blade,
                 EquipmentRules.Weapon equippedWeapon, Set<EquipmentRules.Weapon> craftedWeapons,
                 ToolMode toolMode, BuildType buildType, float buildYaw, float brushRadius, String edits, Set<String> removed, List<BuildRecord> builds) {
            this.seed = seed; this.x = x; this.footY = footY; this.z = z; this.health = health; this.stamina = stamina;
            this.hunger = hunger; this.dayClock = dayClock; this.wood = wood; this.stone = stone; this.berries = berries;
            this.ironOre = ironOre; this.mistResin = mistResin; this.cinderShard = cinderShard;
            this.kills = kills; this.blade = blade; this.equippedWeapon = equippedWeapon; this.craftedWeapons = craftedWeapons;
            this.toolMode = toolMode; this.buildType = buildType;
''')

old_load_return = '''                return new SaveData(Long.parseLong(p.getProperty("seed")), Float.parseFloat(p.getProperty("x", "0")),
                        Float.parseFloat(p.getProperty("footY", "0")), Float.parseFloat(p.getProperty("z", "0")),
                        Float.parseFloat(p.getProperty("health", "100")), Float.parseFloat(p.getProperty("stamina", "100")),
                        Float.parseFloat(p.getProperty("hunger", "100")), Float.parseFloat(p.getProperty("dayClock", "0.18")),
                        Integer.parseInt(p.getProperty("wood", "0")), Integer.parseInt(p.getProperty("stone", "0")),
                        Integer.parseInt(p.getProperty("berries", "0")), Integer.parseInt(p.getProperty("kills", "0")),
                        Boolean.parseBoolean(p.getProperty("blade", "false")), ToolMode.valueOf(p.getProperty("toolMode", ToolMode.DIG.name())),
                        BuildType.valueOf(p.getProperty("buildType", BuildType.FLOOR.name())), Float.parseFloat(p.getProperty("buildYaw", "0")),
                        Float.parseFloat(p.getProperty("brushRadius", "2.6")), p.getProperty("edits", ""), removed, builds);
'''
new_load_return = '''                boolean oldBlade = Boolean.parseBoolean(p.getProperty("blade", "false"));
                Set<EquipmentRules.Weapon> crafted = EnumSet.noneOf(EquipmentRules.Weapon.class);
                String craftedText = p.getProperty("craftedWeapons", "");
                if (!craftedText.isBlank()) for (String item : craftedText.split(",")) if (!item.isBlank()) crafted.add(EquipmentRules.Weapon.valueOf(item));
                if (oldBlade) crafted.add(EquipmentRules.Weapon.WANDERERS_BLADE);
                EquipmentRules.Weapon equipped = EquipmentRules.Weapon.valueOf(p.getProperty("equippedWeapon",
                        oldBlade ? EquipmentRules.Weapon.WANDERERS_BLADE.name() : EquipmentRules.Weapon.FISTS.name()));
                return new SaveData(Long.parseLong(p.getProperty("seed")), Float.parseFloat(p.getProperty("x", "0")),
                        Float.parseFloat(p.getProperty("footY", "0")), Float.parseFloat(p.getProperty("z", "0")),
                        Float.parseFloat(p.getProperty("health", "100")), Float.parseFloat(p.getProperty("stamina", "100")),
                        Float.parseFloat(p.getProperty("hunger", "100")), Float.parseFloat(p.getProperty("dayClock", "0.18")),
                        Integer.parseInt(p.getProperty("wood", "0")), Integer.parseInt(p.getProperty("stone", "0")),
                        Integer.parseInt(p.getProperty("berries", "0")), Integer.parseInt(p.getProperty("ironOre", "0")),
                        Integer.parseInt(p.getProperty("mistResin", "0")), Integer.parseInt(p.getProperty("cinderShard", "0")),
                        Integer.parseInt(p.getProperty("kills", "0")), oldBlade, equipped, crafted,
                        ToolMode.valueOf(p.getProperty("toolMode", ToolMode.DIG.name())), BuildType.valueOf(p.getProperty("buildType", BuildType.FLOOR.name())),
                        Float.parseFloat(p.getProperty("buildYaw", "0")), Float.parseFloat(p.getProperty("brushRadius", "2.6")),
                        p.getProperty("edits", ""), removed, builds);
'''
rep(old_load_return, new_load_return)

rep('''            p.setProperty("berries", Integer.toString(berries)); p.setProperty("kills", Integer.toString(kills)); p.setProperty("blade", Boolean.toString(blade));
            p.setProperty("toolMode", toolMode.name());''', '''            p.setProperty("berries", Integer.toString(berries)); p.setProperty("ironOre", Integer.toString(ironOre));
            p.setProperty("mistResin", Integer.toString(mistResin)); p.setProperty("cinderShard", Integer.toString(cinderShard));
            p.setProperty("kills", Integer.toString(kills)); p.setProperty("blade", Boolean.toString(blade));
            p.setProperty("equippedWeapon", equippedWeapon.name());
            p.setProperty("craftedWeapons", craftedWeapons.stream().map(Enum::name).sorted().reduce((a, b) -> a + "," + b).orElse(""));
            p.setProperty("toolMode", toolMode.name());''')

path.write_text(text, encoding='utf-8')
print('equipment progression integrated')
