from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')

replacements = [
    ('        if (WaterPhysics.isSwimming(footY, SEA_LEVEL)) {', '        if (!inDungeon && WaterPhysics.isSwimming(footY, SEA_LEVEL)) {'),
    ('        boolean swimming = WaterPhysics.isSwimming(footY, SEA_LEVEL);\n        boolean wading = WaterPhysics.isWading(footY, SEA_LEVEL);',
     '        boolean swimming = !inDungeon && WaterPhysics.isSwimming(footY, SEA_LEVEL);\n        boolean wading = !inDungeon && WaterPhysics.isWading(footY, SEA_LEVEL);'),
    ('        swimming = WaterPhysics.isSwimming(footY, SEA_LEVEL);', '        swimming = !inDungeon && WaterPhysics.isSwimming(footY, SEA_LEVEL);'),
    ('        boolean headUnderwater = footY + EYE_HEIGHT < SEA_LEVEL - 0.06f;', '        boolean headUnderwater = !inDungeon && footY + EYE_HEIGHT < SEA_LEVEL - 0.06f;'),
]

changed = False
for old, new in replacements:
    if new in text:
        continue
    if old not in text:
        raise SystemExit('missing water-state anchor: ' + old)
    text = text.replace(old, new, 1)
    changed = True

# updateHud has the same pair as updatePlayer, so patch any remaining raw pair once more.
old_pair = '        boolean swimming = WaterPhysics.isSwimming(footY, SEA_LEVEL);\n        boolean wading = WaterPhysics.isWading(footY, SEA_LEVEL);'
new_pair = '        boolean swimming = !inDungeon && WaterPhysics.isSwimming(footY, SEA_LEVEL);\n        boolean wading = !inDungeon && WaterPhysics.isWading(footY, SEA_LEVEL);'
if old_pair in text:
    text = text.replace(old_pair, new_pair, 1)
    changed = True

if changed:
    path.write_text(text, encoding='utf-8')
    print('dungeon now bypasses surface-water state')
else:
    print('dungeon water-state fix already present')
