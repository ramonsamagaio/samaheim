package com.samaheim;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Cylinder;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.samaheim.game.Inventory;
import com.samaheim.game.ProgressionState;
import com.samaheim.world.WorldMath;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

public final class SamaheimGame extends SimpleApplication implements ActionListener {
    private static final float EYE_HEIGHT = 1.72f;
    private static final float WORLD_HALF_EXTENT = 116f;
    private static final float INTERACT_RANGE = 4.8f;
    private static final float ATTACK_RANGE = 3.6f;
    private static final float DAY_LENGTH_SECONDS = 900f;

    private final Inventory inventory = new Inventory();
    private final ProgressionState progression = new ProgressionState();
    private final Node interactables = new Node("interactables");
    private final Node enemies = new Node("enemies");
    private final Node structures = new Node("structures");

    private final EnumMap<Move, Boolean> movement = new EnumMap<>(Move.class);

    private BitmapText hudText;
    private BitmapText objectiveText;
    private BitmapText messageText;
    private DirectionalLight sun;
    private AmbientLight ambient;

    private long worldSeed;
    private float health = 100f;
    private float stamina = 100f;
    private float hunger = 100f;
    private float dayClock = 0.18f;
    private float attackCooldown;
    private float enemySpawnClock;
    private float autoSaveClock;
    private float messageClock;
    private int campfireKits;
    private int enemySerial;
    private boolean hasBlade;

    public static void main(String[] args) {
        SamaheimGame game = new SamaheimGame();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Samaheim - First Playable");
        settings.setResolution(1600, 900);
        settings.setVSync(true);
        settings.setSamples(4);
        game.setSettings(settings);
        game.setShowSettings(false);
        game.start();
    }

    public SamaheimGame() {
        for (Move move : Move.values()) {
            movement.put(move, false);
        }
    }

    @Override
    public void simpleInitApp() {
        SaveSnapshot snapshot = SaveSnapshot.load();
        worldSeed = snapshot == null ? new Random().nextLong() : snapshot.seed();

        rootNode.attachChild(interactables);
        rootNode.attachChild(enemies);
        rootNode.attachChild(structures);

        configureCamera();
        configureInput();
        configureLighting();
        buildWorld();
        configureHud();

        if (snapshot != null) {
            restore(snapshot);
            announce("Save loaded. The road remembers you.");
        } else {
            placePlayerAt(0f, 0f);
            announce("Gather supplies. The wilds are already watching.");
        }
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(0f);
        flyCam.setZoomSpeed(0f);
        flyCam.setRotationSpeed(2.2f);
        flyCam.setDragToRotate(false);
        cam.setFrustumPerspective(70f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 420f);
    }

    private void configureInput() {
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("CraftBlade", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("CraftCamp", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("PlaceCamp", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Eat", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("Save", new KeyTrigger(KeyInput.KEY_F5));
        inputManager.addListener(this, "Forward", "Back", "Left", "Right", "Sprint", "Interact", "Attack",
                "CraftBlade", "CraftCamp", "PlaceCamp", "Eat", "Save");
    }

    private void configureLighting() {
        sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.6f, -1f, -0.35f).normalizeLocal());
        sun.setColor(new ColorRGBA(1f, 0.94f, 0.82f, 1f));
        rootNode.addLight(sun);

        ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.28f, 0.31f, 0.38f, 1f));
        rootNode.addLight(ambient);
    }

    private void buildWorld() {
        rootNode.attachChild(createTerrain());
        Random random = new Random(worldSeed);

        for (int i = 0; i < 72; i++) {
            Vector3f p = randomWorldPoint(random, 16f);
            createTree(p.x, p.z, random.nextFloat(0.85f, 1.35f));
        }
        for (int i = 0; i < 50; i++) {
            Vector3f p = randomWorldPoint(random, 12f);
            createRock(p.x, p.z, random.nextFloat(0.7f, 1.5f));
        }
        for (int i = 0; i < 28; i++) {
            Vector3f p = randomWorldPoint(random, 10f);
            createBerryBush(p.x, p.z);
        }

        createArcaneShrine(53f, 42f, 1);
        createArcaneShrine(-58f, 31f, 2);
        createArcaneShrine(18f, -72f, 3);
        createRuinedTower(-36f, -44f);
        createRuinedTower(70f, -24f);

        for (int i = 0; i < 11; i++) {
            Vector3f p = randomWorldPoint(random, 24f);
            spawnEnemy(i < 8 ? EnemyType.GOBLIN : EnemyType.SKELETON, p.x, p.z);
        }
    }

    private Geometry createTerrain() {
        int cells = 80;
        float cellSize = (WORLD_HALF_EXTENT * 2f) / cells;
        int width = cells + 1;
        float[] positions = new float[width * width * 3];
        float[] normals = new float[width * width * 3];
        int[] indices = new int[cells * cells * 6];

        int vertex = 0;
        for (int z = 0; z < width; z++) {
            float worldZ = -WORLD_HALF_EXTENT + z * cellSize;
            for (int x = 0; x < width; x++) {
                float worldX = -WORLD_HALF_EXTENT + x * cellSize;
                float y = WorldMath.height(worldSeed, worldX, worldZ);
                positions[vertex * 3] = worldX;
                positions[vertex * 3 + 1] = y;
                positions[vertex * 3 + 2] = worldZ;

                float hLeft = WorldMath.height(worldSeed, worldX - 0.5f, worldZ);
                float hRight = WorldMath.height(worldSeed, worldX + 0.5f, worldZ);
                float hDown = WorldMath.height(worldSeed, worldX, worldZ - 0.5f);
                float hUp = WorldMath.height(worldSeed, worldX, worldZ + 0.5f);
                Vector3f normal = new Vector3f(hLeft - hRight, 1f, hDown - hUp).normalizeLocal();
                normals[vertex * 3] = normal.x;
                normals[vertex * 3 + 1] = normal.y;
                normals[vertex * 3 + 2] = normal.z;
                vertex++;
            }
        }

        int cursor = 0;
        for (int z = 0; z < cells; z++) {
            for (int x = 0; x < cells; x++) {
                int a = z * width + x;
                int b = a + 1;
                int c = a + width;
                int d = c + 1;
                indices[cursor++] = a;
                indices[cursor++] = c;
                indices[cursor++] = b;
                indices[cursor++] = b;
                indices[cursor++] = c;
                indices[cursor++] = d;
            }
        }

        Mesh mesh = new Mesh();
        mesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        mesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        mesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        mesh.updateBound();
        mesh.setStatic();

        Geometry terrain = new Geometry("terrain", mesh);
        terrain.setMaterial(litMaterial(new ColorRGBA(0.16f, 0.29f, 0.12f, 1f), 4f));
        return terrain;
    }

    private Vector3f randomWorldPoint(Random random, float safeRadius) {
        float x;
        float z;
        do {
            x = random.nextFloat(-WORLD_HALF_EXTENT + 8f, WORLD_HALF_EXTENT - 8f);
            z = random.nextFloat(-WORLD_HALF_EXTENT + 8f, WORLD_HALF_EXTENT - 8f);
        } while (x * x + z * z < safeRadius * safeRadius);
        return new Vector3f(x, WorldMath.height(worldSeed, x, z), z);
    }

    private void createTree(float x, float z, float scale) {
        Node entity = entityNode("TREE", x, z);
        Geometry trunk = new Geometry("trunk", new Box(0.34f * scale, 1.5f * scale, 0.34f * scale));
        trunk.setMaterial(litMaterial(new ColorRGBA(0.26f, 0.13f, 0.06f, 1f), 3f));
        trunk.setLocalTranslation(0f, 1.5f * scale, 0f);
        Geometry crown = new Geometry("crown", new Sphere(10, 12, 1.45f * scale));
        crown.setMaterial(litMaterial(new ColorRGBA(0.08f, 0.22f, 0.08f, 1f), 2f));
        crown.setLocalTranslation(0f, 3.45f * scale, 0f);
        entity.attachChild(trunk);
        entity.attachChild(crown);
        interactables.attachChild(entity);
    }

    private void createRock(float x, float z, float scale) {
        Node entity = entityNode("ROCK", x, z);
        Geometry rock = new Geometry("rock", new Sphere(8, 10, 0.72f * scale));
        rock.setLocalScale(1.35f, 0.82f, 1f);
        rock.setMaterial(litMaterial(new ColorRGBA(0.34f, 0.36f, 0.38f, 1f), 18f));
        rock.setLocalTranslation(0f, 0.52f * scale, 0f);
        entity.attachChild(rock);
        interactables.attachChild(entity);
    }

    private void createBerryBush(float x, float z) {
        Node entity = entityNode("BERRY", x, z);
        Geometry bush = new Geometry("bush", new Sphere(8, 10, 0.68f));
        bush.setMaterial(litMaterial(new ColorRGBA(0.13f, 0.31f, 0.11f, 1f), 2f));
        bush.setLocalTranslation(0f, 0.6f, 0f);
        entity.attachChild(bush);
        for (int i = 0; i < 4; i++) {
            Geometry berry = new Geometry("berry", new Sphere(6, 8, 0.09f));
            berry.setMaterial(unshaded(new ColorRGBA(0.58f, 0.05f, 0.16f, 1f)));
            float angle = i * FastMath.HALF_PI;
            berry.setLocalTranslation(FastMath.cos(angle) * 0.45f, 0.7f + (i % 2) * 0.18f,
                    FastMath.sin(angle) * 0.45f);
            entity.attachChild(berry);
        }
        interactables.attachChild(entity);
    }

    private void createArcaneShrine(float x, float z, int sealId) {
        Node entity = entityNode("SHRINE", x, z);
        entity.setUserData("seal", sealId);
        entity.setUserData("used", false);

        Geometry plinth = new Geometry("plinth", new Box(1.4f, 0.35f, 1.4f));
        plinth.setMaterial(litMaterial(new ColorRGBA(0.25f, 0.24f, 0.29f, 1f), 12f));
        plinth.setLocalTranslation(0f, 0.35f, 0f);
        entity.attachChild(plinth);

        Geometry crystal = new Geometry("crystal", new Box(0.38f, 1.25f, 0.38f));
        crystal.rotate(0f, 0f, FastMath.QUARTER_PI);
        crystal.setLocalTranslation(0f, 1.65f, 0f);
        crystal.setMaterial(unshaded(new ColorRGBA(0.42f, 0.12f, 0.78f, 1f)));
        entity.attachChild(crystal);
        interactables.attachChild(entity);
    }

    private void createRuinedTower(float x, float z) {
        Node tower = new Node("ruined-tower");
        tower.setLocalTranslation(x, WorldMath.height(worldSeed, x, z), z);
        Material stone = litMaterial(new ColorRGBA(0.28f, 0.27f, 0.25f, 1f), 7f);
        for (int i = 0; i < 5; i++) {
            float angle = i * FastMath.TWO_PI / 5f;
            Geometry wall = new Geometry("ruin-wall", new Box(1.25f, 2.8f + i * 0.25f, 0.35f));
            wall.setMaterial(stone);
            wall.setLocalTranslation(FastMath.cos(angle) * 2.1f, 2.8f, FastMath.sin(angle) * 2.1f);
            wall.rotate(0f, -angle, 0f);
            tower.attachChild(wall);
        }
        structures.attachChild(tower);
    }

    private Node entityNode(String kind, float x, float z) {
        Node entity = new Node(kind.toLowerCase());
        entity.setUserData("kind", kind);
        entity.setLocalTranslation(x, WorldMath.height(worldSeed, x, z), z);
        return entity;
    }

    private void spawnEnemy(EnemyType type, float x, float z) {
        Node enemy = entityNode("ENEMY", x, z);
        enemy.setName("enemy-" + (++enemySerial));
        enemy.setUserData("enemyType", type.name());
        enemy.setUserData("hp", type.maxHealth);
        enemy.setUserData("attackClock", 0f);

        Geometry body = new Geometry("body", new Box(type == EnemyType.GOBLIN ? 0.42f : 0.35f,
                type == EnemyType.GOBLIN ? 0.78f : 0.95f, 0.32f));
        body.setMaterial(litMaterial(type.color, 9f));
        body.setLocalTranslation(0f, type == EnemyType.GOBLIN ? 0.78f : 0.95f, 0f);
        enemy.attachChild(body);
        enemies.attachChild(enemy);
    }

    private Material litMaterial(ColorRGBA color, float shininess) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color.mult(0.65f));
        material.setColor("Specular", ColorRGBA.White.mult(0.2f));
        material.setFloat("Shininess", shininess);
        return material;
    }

    private Material unshaded(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }

    private void configureHud() {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hudText = new BitmapText(font);
        hudText.setSize(21f);
        hudText.setLocalTranslation(22f, cam.getHeight() - 24f, 0f);
        guiNode.attachChild(hudText);

        objectiveText = new BitmapText(font);
        objectiveText.setSize(23f);
        objectiveText.setColor(new ColorRGBA(1f, 0.88f, 0.54f, 1f));
        objectiveText.setLocalTranslation(22f, cam.getHeight() - 70f, 0f);
        guiNode.attachChild(objectiveText);

        messageText = new BitmapText(font);
        messageText.setSize(20f);
        messageText.setLocalTranslation(22f, 52f, 0f);
        guiNode.attachChild(messageText);

        BitmapText crosshair = new BitmapText(font);
        crosshair.setText("+");
        crosshair.setSize(28f);
        crosshair.setLocalTranslation(cam.getWidth() / 2f - 7f, cam.getHeight() / 2f + 9f, 0f);
        guiNode.attachChild(crosshair);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "Forward" -> movement.put(Move.FORWARD, isPressed);
            case "Back" -> movement.put(Move.BACK, isPressed);
            case "Left" -> movement.put(Move.LEFT, isPressed);
            case "Right" -> movement.put(Move.RIGHT, isPressed);
            case "Sprint" -> movement.put(Move.SPRINT, isPressed);
            case "Interact" -> {
                if (isPressed) {
                    interact();
                }
            }
            case "Attack" -> {
                if (isPressed) {
                    attack();
                }
            }
            case "CraftBlade" -> {
                if (isPressed) {
                    craftBlade();
                }
            }
            case "CraftCamp" -> {
                if (isPressed) {
                    craftCampfireKit();
                }
            }
            case "PlaceCamp" -> {
                if (isPressed) {
                    placeCampfire();
                }
            }
            case "Eat" -> {
                if (isPressed) {
                    eatBerry();
                }
            }
            case "Save" -> {
                if (isPressed) {
                    saveGame();
                }
            }
            default -> {
            }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        attackCooldown = Math.max(0f, attackCooldown - tpf);
        messageClock = Math.max(0f, messageClock - tpf);
        autoSaveClock += tpf;
        enemySpawnClock += tpf;

        updatePlayer(tpf);
        updateEnemies(tpf);
        updateSurvival(tpf);
        updateDayNight(tpf);
        progression.updateFromInventory(inventory);
        updateHud();

        if (enemySpawnClock >= 70f && enemies.getQuantity() < 18) {
            enemySpawnClock = 0f;
            spawnRoamingEnemy();
        }
        if (autoSaveClock >= 60f) {
            autoSaveClock = 0f;
            saveGameSilently();
        }
        if (messageClock <= 0f) {
            messageText.setText("E gather/use | LMB attack | 1 blade | 2 camp kit | Q place | R eat | F5 save");
        }
        if (health <= 0f) {
            respawn();
        }
    }

    private void updatePlayer(float tpf) {
        Vector3f forward = cam.getDirection().clone();
        forward.y = 0f;
        if (forward.lengthSquared() < 0.0001f) {
            forward.set(0f, 0f, -1f);
        }
        forward.normalizeLocal();
        Vector3f left = new Vector3f(forward.z, 0f, -forward.x);
        Vector3f delta = new Vector3f();

        if (movement.get(Move.FORWARD)) {
            delta.addLocal(forward);
        }
        if (movement.get(Move.BACK)) {
            delta.subtractLocal(forward);
        }
        if (movement.get(Move.LEFT)) {
            delta.addLocal(left);
        }
        if (movement.get(Move.RIGHT)) {
            delta.subtractLocal(left);
        }

        boolean moving = delta.lengthSquared() > 0.001f;
        boolean sprinting = moving && movement.get(Move.SPRINT) && stamina > 2f;
        float speed = sprinting ? 8.5f : 5.1f;
        if (moving) {
            delta.normalizeLocal().multLocal(speed * tpf);
            Vector3f next = cam.getLocation().add(delta);
            next.x = Math.clamp(next.x, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f);
            next.z = Math.clamp(next.z, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f);
            next.y = WorldMath.height(worldSeed, next.x, next.z) + EYE_HEIGHT;
            cam.setLocation(next);
        }

        if (sprinting) {
            stamina = Math.max(0f, stamina - 22f * tpf);
        } else {
            stamina = Math.min(100f, stamina + 16f * tpf);
        }
    }

    private void updateEnemies(float tpf) {
        Vector3f player = cam.getLocation();
        List<Spatial> snapshot = new ArrayList<>(enemies.getChildren());
        for (Spatial spatial : snapshot) {
            if (!(spatial instanceof Node enemy)) {
                continue;
            }
            EnemyType type = EnemyType.valueOf(enemy.getUserData("enemyType"));
            Vector3f toPlayer = player.subtract(enemy.getWorldTranslation());
            toPlayer.y = 0f;
            float distance = toPlayer.length();
            float enemyAttackClock = enemy.<Float>getUserData("attackClock") - tpf;

            if (distance < type.noticeRange && distance > 1.3f) {
                toPlayer.normalizeLocal();
                Vector3f local = enemy.getLocalTranslation().add(toPlayer.mult(type.speed * tpf));
                local.x = Math.clamp(local.x, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
                local.z = Math.clamp(local.z, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
                local.y = WorldMath.height(worldSeed, local.x, local.z);
                enemy.setLocalTranslation(local);
            }

            if (distance <= 1.55f && enemyAttackClock <= 0f) {
                health = Math.max(0f, health - type.damage);
                enemyAttackClock = type.attackDelay;
                announce(type == EnemyType.GOBLIN ? "Goblin blade!" : "A graveborn strikes you!");
            }
            enemy.setUserData("attackClock", enemyAttackClock);
        }
    }

    private void updateSurvival(float tpf) {
        hunger = Math.max(0f, hunger - 0.35f * tpf);
        if (hunger <= 0f) {
            health = Math.max(0f, health - 1.25f * tpf);
        }
    }

    private void updateDayNight(float tpf) {
        dayClock = (dayClock + tpf / DAY_LENGTH_SECONDS) % 1f;
        float angle = dayClock * FastMath.TWO_PI;
        float elevation = FastMath.sin(angle);
        sun.setDirection(new Vector3f(FastMath.cos(angle), -Math.max(0.16f, elevation), -0.35f).normalizeLocal());
        float daylight = Math.clamp(elevation * 0.75f + 0.38f, 0.08f, 1f);
        sun.setColor(new ColorRGBA(1f, 0.88f, 0.72f, 1f).mult(Math.max(0.12f, daylight)));
        ambient.setColor(new ColorRGBA(0.22f, 0.27f, 0.38f, 1f).mult(0.5f + daylight * 0.7f));
        viewPort.setBackgroundColor(new ColorRGBA(0.04f + 0.25f * daylight, 0.06f + 0.34f * daylight,
                0.10f + 0.43f * daylight, 1f));
    }

    private void interact() {
        Node entity = raycastEntity(INTERACT_RANGE);
        if (entity == null) {
            announce("Nothing useful within reach.");
            return;
        }
        String kind = entity.getUserData("kind");
        switch (kind) {
            case "TREE" -> {
                inventory.add(Inventory.Item.WOOD, 3);
                entity.removeFromParent();
                announce("+3 wood");
            }
            case "ROCK" -> {
                inventory.add(Inventory.Item.STONE, 2);
                entity.removeFromParent();
                announce("+2 stone");
            }
            case "BERRY" -> {
                inventory.add(Inventory.Item.BERRY, 2);
                entity.removeFromParent();
                announce("+2 emberberries");
            }
            case "SHRINE" -> activateShrine(entity);
            default -> announce("You study it, but learn nothing yet.");
        }
    }

    private void attack() {
        if (attackCooldown > 0f) {
            return;
        }
        attackCooldown = hasBlade ? 0.48f : 0.7f;
        Node target = raycastEntity(ATTACK_RANGE);
        if (target == null || !"ENEMY".equals(target.getUserData("kind"))) {
            return;
        }
        float damage = hasBlade ? 22f : 7f;
        float hp = target.<Float>getUserData("hp") - damage;
        if (hp <= 0f) {
            EnemyType type = EnemyType.valueOf(target.getUserData("enemyType"));
            target.removeFromParent();
            inventory.add(Inventory.Item.ARCANE_DUST, type == EnemyType.GOBLIN ? 1 : 2);
            if (type == EnemyType.GOBLIN) {
                progression.markGoblinDefeated();
            }
            announce(type.displayName + " defeated. Arcane dust recovered.");
        } else {
            target.setUserData("hp", hp);
            announce("Hit for " + Math.round(damage) + ".");
        }
    }

    private Node raycastEntity(float range) {
        Ray ray = new Ray(cam.getLocation(), cam.getDirection());
        CollisionResults results = new CollisionResults();
        interactables.collideWith(ray, results);
        enemies.collideWith(ray, results);
        for (CollisionResult result : results) {
            if (result.getDistance() > range) {
                break;
            }
            Geometry geometry = result.getGeometry();
            Spatial parent = geometry.getParent();
            if (parent instanceof Node node && node.getUserData("kind") != null) {
                return node;
            }
        }
        return null;
    }

    private void activateShrine(Node shrine) {
        boolean used = Boolean.TRUE.equals(shrine.getUserData("used"));
        if (used) {
            announce("This seal has already yielded its secret.");
            return;
        }
        shrine.setUserData("used", true);
        progression.markArcaneSealFound();
        inventory.add(Inventory.Item.ARCANE_DUST, 3);
        Spatial crystal = shrine.getChild("crystal");
        if (crystal instanceof Geometry geometry) {
            geometry.setMaterial(unshaded(new ColorRGBA(0.08f, 0.45f, 0.52f, 1f)));
        }
        announce("Arcane Seal awakened. The old road opens a little further.");
    }

    private void craftBlade() {
        if (hasBlade) {
            announce("You already carry the Wanderer's Blade.");
            return;
        }
        Map<Inventory.Item, Integer> recipe = Map.of(Inventory.Item.WOOD, 8, Inventory.Item.STONE, 4);
        if (!inventory.consumeRecipe(recipe)) {
            announce("Blade requires 8 wood and 4 stone.");
            return;
        }
        hasBlade = true;
        progression.markBladeCrafted();
        announce("Wanderer's Blade crafted. Damage greatly increased.");
    }

    private void craftCampfireKit() {
        Map<Inventory.Item, Integer> recipe = Map.of(Inventory.Item.WOOD, 5, Inventory.Item.STONE, 3);
        if (!inventory.consumeRecipe(recipe)) {
            announce("Campfire kit requires 5 wood and 3 stone.");
            return;
        }
        campfireKits++;
        announce("Campfire kit ready. Press Q to place.");
    }

    private void placeCampfire() {
        if (campfireKits <= 0) {
            announce("Craft a campfire kit first [2].");
            return;
        }
        Vector3f flat = cam.getDirection().clone();
        flat.y = 0f;
        flat.normalizeLocal();
        Vector3f p = cam.getLocation().add(flat.mult(2.6f));
        p.y = WorldMath.height(worldSeed, p.x, p.z);
        createCampfire(p);
        campfireKits--;
        progression.markCampBuilt();
        announce("Camp established. Warmth pushes the dark back.");
    }

    private void createCampfire(Vector3f p) {
        Node camp = new Node("campfire");
        camp.setLocalTranslation(p);
        Material stone = litMaterial(new ColorRGBA(0.27f, 0.25f, 0.23f, 1f), 8f);
        for (int i = 0; i < 8; i++) {
            float angle = i * FastMath.TWO_PI / 8f;
            Geometry rock = new Geometry("camp-rock", new Sphere(6, 8, 0.18f));
            rock.setMaterial(stone);
            rock.setLocalTranslation(FastMath.cos(angle) * 0.55f, 0.13f, FastMath.sin(angle) * 0.55f);
            camp.attachChild(rock);
        }
        Geometry ember = new Geometry("embers", new Sphere(8, 10, 0.32f));
        ember.setMaterial(unshaded(new ColorRGBA(1f, 0.24f, 0.03f, 1f)));
        ember.setLocalTranslation(0f, 0.23f, 0f);
        camp.attachChild(ember);
        structures.attachChild(camp);

        PointLight glow = new PointLight();
        glow.setColor(new ColorRGBA(1f, 0.35f, 0.08f, 1f));
        glow.setRadius(8f);
        glow.setPosition(p.add(0f, 0.8f, 0f));
        rootNode.addLight(glow);
    }

    private void eatBerry() {
        if (!inventory.consume(Inventory.Item.BERRY, 1)) {
            announce("No emberberries left.");
            return;
        }
        hunger = Math.min(100f, hunger + 28f);
        health = Math.min(100f, health + 16f);
        announce("Emberberry: hunger +28, health +16.");
    }

    private void spawnRoamingEnemy() {
        Random random = new Random(worldSeed ^ Float.floatToIntBits(enemySpawnClock + dayClock) ^ enemySerial * 31L);
        float angle = random.nextFloat() * FastMath.TWO_PI;
        float distance = random.nextFloat(22f, 34f);
        float x = Math.clamp(cam.getLocation().x + FastMath.cos(angle) * distance,
                -WORLD_HALF_EXTENT + 5f, WORLD_HALF_EXTENT - 5f);
        float z = Math.clamp(cam.getLocation().z + FastMath.sin(angle) * distance,
                -WORLD_HALF_EXTENT + 5f, WORLD_HALF_EXTENT - 5f);
        EnemyType type = dayClock > 0.5f && dayClock < 0.92f && random.nextBoolean()
                ? EnemyType.SKELETON : EnemyType.GOBLIN;
        spawnEnemy(type, x, z);
        announce("You hear movement beyond the brush...");
    }

    private void updateHud() {
        String time = dayClock > 0.52f && dayClock < 0.93f ? "Night" : "Day";
        hudText.setText("HP " + Math.round(health) + "   ST " + Math.round(stamina) + "   Hunger "
                + Math.round(hunger) + "   " + time + "\nWood " + inventory.get(Inventory.Item.WOOD)
                + " | Stone " + inventory.get(Inventory.Item.STONE) + " | Berries "
                + inventory.get(Inventory.Item.BERRY) + " | Dust " + inventory.get(Inventory.Item.ARCANE_DUST)
                + (hasBlade ? " | Wanderer's Blade" : " | Bare hands"));
        objectiveText.setText("Quest: " + progression.objectiveText());
    }

    private void placePlayerAt(float x, float z) {
        cam.setLocation(new Vector3f(x, WorldMath.height(worldSeed, x, z) + EYE_HEIGHT, z));
        cam.lookAtDirection(new Vector3f(0.4f, -0.08f, -1f).normalizeLocal(), Vector3f.UNIT_Y);
    }

    private void respawn() {
        health = 100f;
        stamina = 100f;
        hunger = 72f;
        placePlayerAt(0f, 0f);
        int dustLost = inventory.get(Inventory.Item.ARCANE_DUST) / 2;
        inventory.set(Inventory.Item.ARCANE_DUST, inventory.get(Inventory.Item.ARCANE_DUST) - dustLost);
        announce("You wake at the Waystone. Half your arcane dust was lost.");
    }

    private void announce(String text) {
        messageText.setText(text);
        messageClock = 4.5f;
    }

    private void saveGame() {
        if (saveGameSilently()) {
            announce("Game saved.");
        } else {
            announce("Save failed. Check the console for details.");
        }
    }

    private boolean saveGameSilently() {
        SaveSnapshot snapshot = new SaveSnapshot(worldSeed, cam.getLocation().clone(), health, stamina, hunger, dayClock,
                inventory.snapshot(), progression.stage(), progression.defeatedGoblins(), progression.arcaneSeals(),
                hasBlade, progression.campBuilt(), campfireKits);
        try {
            snapshot.save();
            return true;
        } catch (IOException ex) {
            System.err.println("Could not save Samaheim: " + ex.getMessage());
            return false;
        }
    }

    private void restore(SaveSnapshot snapshot) {
        for (Inventory.Item item : Inventory.Item.values()) {
            inventory.set(item, snapshot.inventory().getOrDefault(item, 0));
        }
        health = Math.clamp(snapshot.health(), 1f, 100f);
        stamina = Math.clamp(snapshot.stamina(), 0f, 100f);
        hunger = Math.clamp(snapshot.hunger(), 0f, 100f);
        dayClock = snapshot.dayClock();
        hasBlade = snapshot.hasBlade();
        campfireKits = snapshot.campfireKits();
        progression.restore(snapshot.stage(), snapshot.defeatedGoblins(), snapshot.arcaneSeals(), snapshot.hasBlade(),
                snapshot.campBuilt());
        Vector3f position = snapshot.position();
        position.x = Math.clamp(position.x, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f);
        position.z = Math.clamp(position.z, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f);
        position.y = WorldMath.height(worldSeed, position.x, position.z) + EYE_HEIGHT;
        cam.setLocation(position);
    }

    private enum Move {
        FORWARD,
        BACK,
        LEFT,
        RIGHT,
        SPRINT
    }

    private enum EnemyType {
        GOBLIN("Goblin scout", 42f, 4.8f, 18f, 7f, 1.25f, new ColorRGBA(0.24f, 0.45f, 0.14f, 1f)),
        SKELETON("Graveborn", 68f, 3.7f, 22f, 11f, 1.45f, new ColorRGBA(0.72f, 0.72f, 0.66f, 1f));

        private final String displayName;
        private final float maxHealth;
        private final float speed;
        private final float noticeRange;
        private final float damage;
        private final float attackDelay;
        private final ColorRGBA color;

        EnemyType(String displayName, float maxHealth, float speed, float noticeRange, float damage, float attackDelay,
                  ColorRGBA color) {
            this.displayName = displayName;
            this.maxHealth = maxHealth;
            this.speed = speed;
            this.noticeRange = noticeRange;
            this.damage = damage;
            this.attackDelay = attackDelay;
            this.color = color;
        }
    }

    private record SaveSnapshot(long seed, Vector3f position, float health, float stamina, float hunger, float dayClock,
                                Map<Inventory.Item, Integer> inventory, ProgressionState.Stage stage,
                                int defeatedGoblins, int arcaneSeals, boolean hasBlade, boolean campBuilt,
                                int campfireKits) {
        private static final Path SAVE_PATH = Path.of(System.getProperty("user.home"), ".samaheim", "save.properties");

        static SaveSnapshot load() {
            if (!Files.isRegularFile(SAVE_PATH)) {
                return null;
            }
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(SAVE_PATH)) {
                p.load(in);
                long seed = Long.parseLong(p.getProperty("seed"));
                Vector3f position = new Vector3f(Float.parseFloat(p.getProperty("x")), Float.parseFloat(p.getProperty("y")),
                        Float.parseFloat(p.getProperty("z")));
                EnumMap<Inventory.Item, Integer> inventory = new EnumMap<>(Inventory.Item.class);
                for (Inventory.Item item : Inventory.Item.values()) {
                    inventory.put(item, Integer.parseInt(p.getProperty("inv." + item.name(), "0")));
                }
                return new SaveSnapshot(seed, position, Float.parseFloat(p.getProperty("health", "100")),
                        Float.parseFloat(p.getProperty("stamina", "100")), Float.parseFloat(p.getProperty("hunger", "100")),
                        Float.parseFloat(p.getProperty("dayClock", "0.18")), inventory,
                        ProgressionState.Stage.valueOf(p.getProperty("stage", ProgressionState.Stage.GATHER_SUPPLIES.name())),
                        Integer.parseInt(p.getProperty("goblins", "0")), Integer.parseInt(p.getProperty("seals", "0")),
                        Boolean.parseBoolean(p.getProperty("blade", "false")),
                        Boolean.parseBoolean(p.getProperty("campBuilt", "false")),
                        Integer.parseInt(p.getProperty("campKits", "0")));
            } catch (RuntimeException | IOException ex) {
                System.err.println("Ignoring invalid save: " + ex.getMessage());
                return null;
            }
        }

        void save() throws IOException {
            Files.createDirectories(SAVE_PATH.getParent());
            Properties p = new Properties();
            p.setProperty("seed", Long.toString(seed));
            p.setProperty("x", Float.toString(position.x));
            p.setProperty("y", Float.toString(position.y));
            p.setProperty("z", Float.toString(position.z));
            p.setProperty("health", Float.toString(health));
            p.setProperty("stamina", Float.toString(stamina));
            p.setProperty("hunger", Float.toString(hunger));
            p.setProperty("dayClock", Float.toString(dayClock));
            p.setProperty("stage", stage.name());
            p.setProperty("goblins", Integer.toString(defeatedGoblins));
            p.setProperty("seals", Integer.toString(arcaneSeals));
            p.setProperty("blade", Boolean.toString(hasBlade));
            p.setProperty("campBuilt", Boolean.toString(campBuilt));
            p.setProperty("campKits", Integer.toString(campfireKits));
            for (Map.Entry<Inventory.Item, Integer> entry : inventory.entrySet()) {
                p.setProperty("inv." + entry.getKey().name(), Integer.toString(entry.getValue()));
            }
            try (OutputStream out = Files.newOutputStream(SAVE_PATH)) {
                p.store(out, "Samaheim save");
            }
        }
    }
}
