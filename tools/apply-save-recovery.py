from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

if 'import com.samaheim.game.SaveFileRecovery;' in text:
    print('save recovery already integrated')
    raise SystemExit(0)

text = text.replace('import com.samaheim.game.ResourceRegrowthRules;\n',
                    'import com.samaheim.game.ResourceRegrowthRules;\nimport com.samaheim.game.SaveFileRecovery;\n', 1)
text = text.replace('import java.io.InputStream;\n', '', 1)
text = text.replace('import java.io.OutputStream;\n', '', 1)

start = text.index('        static SaveData load() {')
end = text.index('        void save() throws IOException {', start)
new_load = '''        static SaveData load() {
            Path backup = SaveFileRecovery.backupPath(PATH);
            for (Path candidate : SaveFileRecovery.candidates(PATH)) {
                if (!Files.isRegularFile(candidate)) continue;
                try {
                    SaveData loaded = parse(SaveFileRecovery.loadProperties(candidate));
                    if (candidate.equals(backup)) {
                        SaveFileRecovery.restoreBackup(PATH);
                        System.err.println("Recovered cave save from backup after primary failure.");
                    }
                    return loaded;
                } catch (RuntimeException | IOException ex) {
                    System.err.println("Invalid cave save " + candidate.getFileName() + ": " + ex.getMessage());
                }
            }
            return null;
        }

        private static SaveData parse(Properties p) {
            Set<String> removed = new HashSet<>();
            String removedText = p.getProperty("removed", "");
            if (!removedText.isBlank()) for (String id : removedText.split(";")) if (!id.isBlank()) removed.add(id);
            Set<String> lootedPois = new HashSet<>();
            String lootedText = p.getProperty("lootedPois", "");
            if (!lootedText.isBlank()) for (String id : lootedText.split(";")) if (!id.isBlank()) lootedPois.add(id);
            List<BuildRecord> builds = new ArrayList<>();
            String buildsText = p.getProperty("builds", "");
            if (!buildsText.isBlank()) for (String item : buildsText.split(";")) if (!item.isBlank()) builds.add(BuildRecord.decode(item));
            boolean oldBlade = Boolean.parseBoolean(p.getProperty("blade", "false"));
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
                    Long.parseLong(p.getProperty("dawnIndex", "0")),
                    Integer.parseInt(p.getProperty("wood", "0")), Integer.parseInt(p.getProperty("stone", "0")),
                    Integer.parseInt(p.getProperty("berries", "0")), Integer.parseInt(p.getProperty("ironOre", "0")),
                    Integer.parseInt(p.getProperty("mistResin", "0")), Integer.parseInt(p.getProperty("cinderShard", "0")),
                    Integer.parseInt(p.getProperty("kills", "0")), oldBlade, equipped, crafted,
                    ToolMode.valueOf(p.getProperty("toolMode", ToolMode.DIG.name())), BuildType.valueOf(p.getProperty("buildType", BuildType.FLOOR.name())),
                    Float.parseFloat(p.getProperty("buildYaw", "0")), Float.parseFloat(p.getProperty("brushRadius", "2.6")),
                    p.getProperty("edits", ""), removed, lootedPois, builds);
        }

'''
text = text[:start] + new_load + text[end:]

old_tail = '''            StringBuilder buildText = new StringBuilder();
            for (BuildRecord record : builds) { if (buildText.length() > 0) buildText.append(';'); buildText.append(record.encode()); }
            p.setProperty("builds", buildText.toString());
            try (OutputStream output = Files.newOutputStream(PATH)) { p.store(output, "Samaheim volumetric cave save"); }
        }'''
new_tail = '''            StringBuilder buildText = new StringBuilder();
            for (BuildRecord record : builds) { if (buildText.length() > 0) buildText.append(';'); buildText.append(record.encode()); }
            p.setProperty("builds", buildText.toString());
            SaveFileRecovery.storeAtomic(p, PATH, "Samaheim volumetric cave save");
        }'''
if old_tail not in text:
    raise SystemExit('missing save tail')
text = text.replace(old_tail, new_tail, 1)

path.write_text(text, encoding='utf-8')
print('integrated atomic saves and backup recovery')
