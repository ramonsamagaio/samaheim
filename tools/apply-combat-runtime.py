from pathlib import Path

path = Path('src/main/java/com/samaheim/SamaheimCaveGame.java')
text = path.read_text(encoding='utf-8')


def replace_once(old: str, new: str) -> None:
    global text
    if old not in text:
        raise SystemExit(f'anchor not found:\n{old[:220]}')
    text = text.replace(old, new, 1)

replace_once(
    'import com.samaheim.ui.SamaheimHud;\n',
    'import com.samaheim.game.CombatRules;\nimport com.samaheim.ui.SamaheimHud;\n'
)

replace_once(
    '        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));\n',
    '        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));\n'
    '        inputManager.addMapping("HeavyAttack", new MouseButtonTrigger(MouseInput.BUTTON_MIDDLE));\n'
)

replace_once(
    '        inputManager.addListener(this, "Forward", "Back", "Left", "Right", "Sprint", "Jump", "SwimDown", "Interact", "Attack",\n'
    '                "Terraform", "ToolMode", "BrushSmaller", "BrushLarger", "CraftBlade", "BuildMode", "RotateBuild",\n',
    '        inputManager.addListener(this, "Forward", "Back", "Left", "Right", "Sprint", "Jump", "SwimDown", "Interact", "Attack", "HeavyAttack",\n'
    '                "Terraform", "ToolMode", "BrushSmaller", "BrushLarger", "CraftBlade", "BuildMode", "RotateBuild",\n'
)

replace_once(
    '        enemy.setUserData("hp", type.hp);\n'
    '        enemy.setUserData("attackClock", 0f);\n',
    '        enemy.setUserData("hp", type.hp);\n'
    '        enemy.setUserData("attackCooldown", 0f);\n'
    '        enemy.setUserData("attackWindup", 0f);\n'
    '        enemy.setUserData("staggerClock", 0f);\n'
)

replace_once(
    '            case "Attack" -> { if (isPressed) attack(); }\n',
    '            case "Attack" -> { if (isPressed) attack(CombatRules.AttackKind.LIGHT); }\n'
    '            case "HeavyAttack" -> { if (isPressed) attack(CombatRules.AttackKind.HEAVY); }\n'
)

old_update = '''    private void updateEnemies(float tpf) {
        Vector3f player = new Vector3f(playerX, footY, playerZ);
        for (Spatial spatial : new ArrayList<>(enemies.getChildren())) {
            if (!(spatial instanceof Node enemy)) continue;
            EnemyType type = EnemyType.valueOf(enemy.getUserData("enemyType"));
            Vector3f p = enemy.getLocalTranslation();
            Vector3f to = player.subtract(p);
            float verticalDifference = Math.abs(to.y);
            to.y = 0f;
            float distance = to.length();
            float clock = enemy.<Float>getUserData("attackClock") - tpf;
            if (distance < type.notice && distance > 1.35f && verticalDifference < 4.5f) {
                to.normalizeLocal();
                CaveCharacterPhysics.HorizontalMove move = CaveCharacterPhysics.moveHorizontal(terrain, p.x, p.y, p.z,
                        to.x * type.speed * tpf, to.z * type.speed * tpf, 0.28f, 1.55f, 0.38f);
                float floor = terrain.findFloor(move.x(), move.z(), move.footY() + 0.45f, 1.4f);
                float y = Float.isFinite(floor) ? floor : move.footY();
                enemy.setLocalTranslation(move.x(), y, move.z());
            }
            if (distance <= 1.45f && verticalDifference < 1.7f && clock <= 0f) {
                health = Math.max(0f, health - type.damage);
                clock = type.delay;
                announce(type.label + " hits you.");
            }
            enemy.setUserData("attackClock", clock);
        }
    }
'''
new_update = '''    private void updateEnemies(float tpf) {
        Vector3f player = new Vector3f(playerX, footY, playerZ);
        for (Spatial spatial : new ArrayList<>(enemies.getChildren())) {
            if (!(spatial instanceof Node enemy)) continue;
            EnemyType type = EnemyType.valueOf(enemy.getUserData("enemyType"));
            CombatRules.EnemyAttack attack = CombatRules.enemyAttack(combatArchetype(type));
            CombatRules.EnemyState state = enemyCombatState(enemy);
            Vector3f p = enemy.getLocalTranslation();
            Vector3f to = player.subtract(p);
            float verticalDifference = Math.abs(to.y);
            to.y = 0f;
            float distance = to.length();
            boolean targetInRange = distance <= attack.range() && verticalDifference < 1.7f;

            if (targetInRange && state.cooldown() <= 0f && state.windup() <= 0f && state.stagger() <= 0f) {
                state = CombatRules.beginEnemyWindup(state, attack);
            }
            boolean committed = state.windup() > 0f || state.stagger() > 0f;
            CombatRules.EnemyTick tick = CombatRules.tickEnemy(state, attack, tpf, targetInRange);
            state = tick.state();

            if (!committed && distance < type.notice && distance > attack.range() * 0.88f && verticalDifference < 4.5f) {
                to.normalizeLocal();
                CaveCharacterPhysics.HorizontalMove move = CaveCharacterPhysics.moveHorizontal(terrain, p.x, p.y, p.z,
                        to.x * type.speed * tpf, to.z * type.speed * tpf, 0.28f, 1.55f, 0.38f);
                float floor = terrain.findFloor(move.x(), move.z(), move.footY() + 0.45f, 1.4f);
                float y = Float.isFinite(floor) ? floor : move.footY();
                enemy.setLocalTranslation(move.x(), y, move.z());
            }

            if (tick.strike()) {
                health = Math.max(0f, health - attack.damage());
                announce(type.label + " hits for " + Math.round(attack.damage()) + ".");
            }
            setEnemyCombatState(enemy, state);
            updateEnemyTelegraph(enemy, type, state, attack);
        }
    }

    private CombatRules.EnemyArchetype combatArchetype(EnemyType type) {
        return type == EnemyType.GOBLIN ? CombatRules.EnemyArchetype.GOBLIN : CombatRules.EnemyArchetype.GRAVEBORN;
    }

    private CombatRules.EnemyState enemyCombatState(Node enemy) {
        return new CombatRules.EnemyState(
                enemy.<Float>getUserData("attackCooldown"),
                enemy.<Float>getUserData("attackWindup"),
                enemy.<Float>getUserData("staggerClock"));
    }

    private void setEnemyCombatState(Node enemy, CombatRules.EnemyState state) {
        enemy.setUserData("attackCooldown", state.cooldown());
        enemy.setUserData("attackWindup", state.windup());
        enemy.setUserData("staggerClock", state.stagger());
    }

    private void updateEnemyTelegraph(Node enemy, EnemyType type, CombatRules.EnemyState state, CombatRules.EnemyAttack attack) {
        Spatial spatial = enemy.getChild("body");
        if (!(spatial instanceof Geometry body)) return;
        float telegraph = CombatRules.telegraphIntensity(state.windup(), attack);
        float stagger = Math.min(1f, state.stagger() * 2.4f);
        ColorRGBA color = new ColorRGBA(
                Math.min(1f, type.color.r * (1f - telegraph) + telegraph),
                Math.min(1f, type.color.g * (1f - telegraph) + 0.18f * telegraph + 0.18f * stagger),
                Math.min(1f, type.color.b * (1f - telegraph) + 0.04f * telegraph + 0.62f * stagger),
                1f);
        body.getMaterial().setColor("Diffuse", color);
        body.getMaterial().setColor("Ambient", color.mult(0.62f));
        float pulse = 1f + telegraph * 0.14f;
        body.setLocalScale(pulse, 1f, pulse);
    }
'''
replace_once(old_update, new_update)

old_attack = '''    private void attack() {
        if (attackCooldown > 0f) return;
        attackCooldown = bladeCrafted ? 0.48f : 0.72f;
        Node enemy = raycastNode(enemies, 3.6f);
        if (enemy == null) return;
        float damage = bladeCrafted ? 22f : 7f;
        float hp = enemy.<Float>getUserData("hp") - damage;
        if (hp <= 0f) {
            enemy.removeFromParent();
            kills++;
            announce("Enemy defeated.");
        } else {
            enemy.setUserData("hp", hp);
            announce("Hit for " + Math.round(damage) + ".");
        }
    }
'''
new_attack = '''    private void attack(CombatRules.AttackKind kind) {
        if (WaterPhysics.isSwimming(footY, SEA_LEVEL)) {
            announce("You cannot swing effectively while swimming.");
            return;
        }
        CombatRules.PlayerAttack attack = CombatRules.playerAttack(bladeCrafted, kind);
        if (!CombatRules.canAttack(stamina, attackCooldown, attack)) {
            if (attackCooldown <= 0f) announce("Too exhausted to attack.");
            return;
        }
        stamina = CombatRules.spendStamina(stamina, attack);
        attackCooldown = attack.cooldownSeconds();
        Node enemy = raycastNode(enemies, attack.range());
        if (enemy == null) return;

        float hp = enemy.<Float>getUserData("hp") - attack.damage();
        if (hp <= 0f) {
            enemy.removeFromParent();
            kills++;
            announce(kind == CombatRules.AttackKind.HEAVY ? "Heavy strike defeats the enemy." : "Enemy defeated.");
        } else {
            enemy.setUserData("hp", hp);
            CombatRules.EnemyState state = CombatRules.applyStagger(enemyCombatState(enemy), attack.staggerSeconds());
            setEnemyCombatState(enemy, state);
            announce((kind == CombatRules.AttackKind.HEAVY ? "Heavy hit for " : "Hit for ") + Math.round(attack.damage()) + ".");
        }
    }
'''
replace_once(old_attack, new_attack)

replace_once(
    '        else detail = "RMB/G use   T mode   Z/C size     |     B build piece   Q place   F rotate   X remove";\n',
    '        else detail = "LMB light attack   MMB heavy attack     |     RMB/G terrain   T mode   B build   Q place";\n'
)

path.write_text(text, encoding='utf-8')
print('combat runtime integrated')
