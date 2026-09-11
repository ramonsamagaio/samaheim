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
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.samaheim.game.Inventory;
import com.samaheim.game.ProgressionState;
import com.samaheim.world.TerrainState;
import com.samaheim.world.TerraformToolSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;

/** Current playable client with persistent, Valheim-style editable terrain. */
public final class SamaheimTerraformGame extends SimpleApplication implements ActionListener {
    private static final float EYE_HEIGHT = 1.72f;
    private static final float WORLD_HALF_EXTENT = 116f;
    private static final int TERRAIN_CELLS = 160;
    private static final float TERRAIN_MAX_DELTA = 8f;
    private static final float TERRAFORM_DISTANCE = 6.2f;
    private static final float INTERACT_RANGE = 4.8f;
    private static final float ATTACK_RANGE = 3.6f;
    private static final float DAY_LENGTH_SECONDS = 900f;

    private final Inventory inventory = new Inventory();
    private final ProgressionState progression = new ProgressionState();
    private final Node interactables = new Node("interactables");
    private final Node enemies = new Node("enemies");
    private final Node structures = new Node("structures");
    private final EnumMap<Move, Boolean> movement = new EnumMap<>(Move.class);

    private TerrainState terrain;
    private Geometry terrainGeometry;
    private Mesh terrainMesh;
    private Geometry terraformMarker;
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
    private boolean hasHoe;
    private TerraformMode terraformMode = TerraformMode.LEVEL;

    public static void main(String[] args) {
        SamaheimTerraformGame game = new SamaheimTerraformGame();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Samaheim - Terraform Build");
        settings.setResolution(1600, 900);
        settings.setVSync(true);
        settings.setSamples(4);
        game.setSettings(settings);
        game.setShowSettings(false);
        game.start();
    }

    public SamaheimTerraformGame() {
        for (Move move : Move.values()) movement.put(move, false);
    }

    @Override
    public void simpleInitApp() {
        SaveData save = SaveData.load();
        worldSeed = save == null ? new Random().nextLong() : save.seed();
        terrain = new TerrainState(worldSeed, WORLD_HALF_EXTENT, TERRAIN_CELLS, TERRAIN_MAX_DELTA);
        if (save != null) terrain.decodeDeltas(save.terrainDeltas());

        rootNode.attachChild(interactables);
        rootNode.attachChild(enemies);
        rootNode.attachChild(structures);
        configureCamera();
        configureInput();
        configureLighting();
        buildWorld();
        createTerraformMarker();
        configureHud();

        if (save != null) {
            restore(save);
            announce("Save loaded. The changed earth remembers you.");
        } else {
            placePlayerAt(0f, 0f);
            announce("Gather supplies. Craft the Mason's Hoe [3] to shape the land.");
        }
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(0f);
        flyCam.setZoomSpeed(0f);
        flyCam.setRotationSpeed(2.2f);
        flyCam.setDragToRotate(false);
        cam.setFrustumPerspective(70f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 500f);
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
        inputManager.addMapping("CraftHoe", new KeyTrigger(KeyInput.KEY_3));
        inputManager.addMapping("CycleTerraform", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("Terraform", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("PlaceCamp", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Eat", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("Save", new KeyTrigger(KeyInput.KEY_F5));
        inputManager.addListener(this, "Forward", "Back", "Left", "Right", "Sprint", "Interact", "Attack",
                "CraftBlade", "CraftCamp", "CraftHoe", "CycleTerraform", "Terraform", "PlaceCamp", "Eat", "Save");
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
        rebuildTerrainMesh();
        Random random = new Random(worldSeed);
        for (int i = 0; i < 82; i++) {
            Vector3f p = randomWorldPoint(random, 16f);
            createTree(p.x, p.z, random.nextFloat(0.85f, 1.35f));
        }
        for (int i = 0; i < 58; i++) {
            Vector3f p = randomWorldPoint(random, 12f);
            createRock(p.x, p.z, random.nextFloat(0.7f, 1.5f));
        }
        for (int i = 0; i < 32; i++) {
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

    private void rebuildTerrainMesh() {
        if (terrainGeometry != null) terrainGeometry.removeFromParent();
        int cells = terrain.cells();
        int width = terrain.width();
        float[] positions = new float[width * width * 3];
        float[] normals = new float[width * width * 3];
        int[] indices = new int[cells * cells * 6];

        int vertex = 0;
        for (int z = 0; z < width; z++) {
            for (int x = 0; x < width; x++) {
                float wx = terrain.vertexWorldX(x);
                float wz = terrain.vertexWorldZ(z);
                positions[vertex * 3] = wx;
                positions[vertex * 3 + 1] = terrain.vertexHeight(x, z);
                positions[vertex * 3 + 2] = wz;
                Vector3f n = terrainNormal(x, z);
                normals[vertex * 3] = n.x;
                normals[vertex * 3 + 1] = n.y;
                normals[vertex * 3 + 2] = n.z;
                vertex++;
            }
        }

        int cursor = 0;
        for (int z = 0; z < cells; z++) {
            for (int x = 0; x < cells; x++) {
                int a = z * width + x, b = a + 1, c = a + width, d = c + 1;
                indices[cursor++] = a; indices[cursor++] = c; indices[cursor++] = b;
                indices[cursor++] = b; indices[cursor++] = c; indices[cursor++] = d;
            }
        }

        terrainMesh = new Mesh();
        terrainMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(positions));
        terrainMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        terrainMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        terrainMesh.updateBound();
        terrainMesh.setDynamic();
        terrainGeometry = new Geometry("mutable-terrain", terrainMesh);
        terrainGeometry.setMaterial(litMaterial(new ColorRGBA(0.16f, 0.29f, 0.12f, 1f), 4f));
        rootNode.attachChild(terrainGeometry);
        terrain.consumeDirtyRegion();
    }

    private Vector3f terrainNormal(int x, int z) {
        int cells = terrain.cells();
        int xl = Math.max(0, x - 1), xr = Math.min(cells, x + 1);
        int zd = Math.max(0, z - 1), zu = Math.min(cells, z + 1);
        float dx = terrain.vertexHeight(xl, z) - terrain.vertexHeight(xr, z);
        float dz = terrain.vertexHeight(x, zd) - terrain.vertexHeight(x, zu);
        return new Vector3f(dx, terrain.cellSize() * 2f, dz).normalizeLocal();
    }

    private void updateTerrainPatch(TerrainState.DirtyRegion region) {
        if (region == null || terrainMesh == null) return;
        FloatBuffer positions = (FloatBuffer) terrainMesh.getBuffer(VertexBuffer.Type.Position).getData();
        FloatBuffer normals = (FloatBuffer) terrainMesh.getBuffer(VertexBuffer.Type.Normal).getData();
        int width = terrain.width();
        int minX = Math.max(0, region.minX() - 1);
        int minZ = Math.max(0, region.minZ() - 1);
        int maxX = Math.min(terrain.cells(), region.maxX() + 1);
        int maxZ = Math.min(terrain.cells(), region.maxZ() + 1);

        for (int z = minZ; z <= maxZ; z++) {
            for (int x = minX; x <= maxX; x++) {
                int vertex = z * width + x;
                positions.put(vertex * 3 + 1, terrain.vertexHeight(x, z));
                Vector3f n = terrainNormal(x, z);
                normals.put(vertex * 3, n.x);
                normals.put(vertex * 3 + 1, n.y);
                normals.put(vertex * 3 + 2, n.z);
            }
        }
        terrainMesh.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
        terrainMesh.getBuffer(VertexBuffer.Type.Normal).setUpdateNeeded();
        terrainMesh.updateBound();
        terrainGeometry.updateModelBound();
    }

    private Vector3f randomWorldPoint(Random random, float safeRadius) {
        float x, z;
        do {
            x = random.nextFloat(-WORLD_HALF_EXTENT + 8f, WORLD_HALF_EXTENT - 8f);
            z = random.nextFloat(-WORLD_HALF_EXTENT + 8f, WORLD_HALF_EXTENT - 8f);
        } while (x * x + z * z < safeRadius * safeRadius);
        return new Vector3f(x, ground(x, z), z);
    }

    private float ground(float x, float z) {
        return terrain.sampleHeight(x, z);
    }

    private Node entityNode(String kind, float x, float z) {
        Node entity = new Node(kind.toLowerCase());
        entity.setUserData("kind", kind);
        entity.setLocalTranslation(x, ground(x, z), z);
        return entity;
    }

    private void createTree(float x, float z, float scale) {
        Node entity = entityNode("TREE", x, z);
        Geometry trunk = new Geometry("trunk", new Box(0.34f * scale, 1.5f * scale, 0.34f * scale));
        trunk.setMaterial(litMaterial(new ColorRGBA(0.26f, 0.13f, 0.06f, 1f), 3f));
        trunk.setLocalTranslation(0f, 1.5f * scale, 0f);
        Geometry crown = new Geometry("crown", new Sphere(10, 12, 1.45f * scale));
        crown.setMaterial(litMaterial(new ColorRGBA(0.08f, 0.22f, 0.08f, 1f), 2f));
        crown.setLocalTranslation(0f, 3.45f * scale, 0f);
        entity.attachChild(trunk); entity.attachChild(crown); interactables.attachChild(entity);
    }

    private void createRock(float x, float z, float scale) {
        Node entity = entityNode("ROCK", x, z);
        Geometry rock = new Geometry("rock", new Sphere(8, 10, 0.72f * scale));
        rock.setLocalScale(1.35f, 0.82f, 1f);
        rock.setMaterial(litMaterial(new ColorRGBA(0.34f, 0.36f, 0.38f, 1f), 18f));
        rock.setLocalTranslation(0f, 0.52f * scale, 0f);
        entity.attachChild(rock); interactables.attachChild(entity);
    }

    private void createBerryBush(float x, float z) {
        Node entity = entityNode("BERRY", x, z);
        Geometry bush = new Geometry("bush", new Sphere(8, 10, 0.68f));
        bush.setMaterial(litMaterial(new ColorRGBA(0.13f, 0.31f, 0.11f, 1f), 2f));
        bush.setLocalTranslation(0f, 0.6f, 0f); entity.attachChild(bush);
        for (int i = 0; i < 4; i++) {
            Geometry berry = new Geometry("berry", new Sphere(6, 8, 0.09f));
            berry.setMaterial(unshaded(new ColorRGBA(0.58f, 0.05f, 0.16f, 1f)));
            float angle = i * FastMath.HALF_PI;
            berry.setLocalTranslation(FastMath.cos(angle) * 0.45f, 0.7f + (i % 2) * 0.18f, FastMath.sin(angle) * 0.45f);
            entity.attachChild(berry);
        }
        interactables.attachChild(entity);
    }

    private void createArcaneShrine(float x, float z, int sealId) {
        Node entity = entityNode("SHRINE", x, z);
        entity.setUserData("seal", sealId); entity.setUserData("used", false);
        Geometry plinth = new Geometry("plinth", new Box(1.4f, 0.35f, 1.4f));
        plinth.setMaterial(litMaterial(new ColorRGBA(0.25f, 0.24f, 0.29f, 1f), 12f));
        plinth.setLocalTranslation(0f, 0.35f, 0f); entity.attachChild(plinth);
        Geometry crystal = new Geometry("crystal", new Box(0.38f, 1.25f, 0.38f));
        crystal.rotate(0f, 0f, FastMath.QUARTER_PI); crystal.setLocalTranslation(0f, 1.65f, 0f);
        crystal.setMaterial(unshaded(new ColorRGBA(0.42f, 0.12f, 0.78f, 1f))); entity.attachChild(crystal);
        interactables.attachChild(entity);
    }

    private void createRuinedTower(float x, float z) {
        Node tower = new Node("ruined-tower");
        tower.setLocalTranslation(x, ground(x, z), z);
        Material stone = litMaterial(new ColorRGBA(0.28f, 0.27f, 0.25f, 1f), 7f);
        for (int i = 0; i < 5; i++) {
            float angle = i * FastMath.TWO_PI / 5f;
            Geometry wall = new Geometry("ruin-wall", new Box(1.25f, 2.8f + i * 0.25f, 0.35f));
            wall.setMaterial(stone); wall.setLocalTranslation(FastMath.cos(angle) * 2.1f, 2.8f, FastMath.sin(angle) * 2.1f);
            wall.rotate(0f, -angle, 0f); tower.attachChild(wall);
        }
        structures.attachChild(tower);
    }

    private void spawnEnemy(EnemyType type, float x, float z) {
        Node enemy = entityNode("ENEMY", x, z);
        enemy.setName("enemy-" + (++enemySerial));
        enemy.setUserData("enemyType", type.name()); enemy.setUserData("hp", type.maxHealth); enemy.setUserData("attackClock", 0f);
        Geometry body = new Geometry("body", new Box(type == EnemyType.GOBLIN ? 0.42f : 0.35f,
                type == EnemyType.GOBLIN ? 0.78f : 0.95f, 0.32f));
        body.setMaterial(litMaterial(type.color, 9f));
        body.setLocalTranslation(0f, type == EnemyType.GOBLIN ? 0.78f : 0.95f, 0f); enemy.attachChild(body); enemies.attachChild(enemy);
    }

    private Material litMaterial(ColorRGBA color, float shininess) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true); material.setColor("Diffuse", color);
        material.setColor("Ambient", color.mult(0.65f)); material.setColor("Specular", ColorRGBA.White.mult(0.2f));
        material.setFloat("Shininess", shininess); return material;
    }

    private Material unshaded(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color); return material;
    }

    private void createTerraformMarker() {
        terraformMarker = new Geometry("terraform-marker", new Sphere(8, 12, 0.16f));
        terraformMarker.setMaterial(unshaded(terraformColor()));
        terraformMarker.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(terraformMarker);
    }

    private void configureHud() {
        BitmapFont font = assetManager.loadFont("Interface/Fonts/Default.fnt");
        hudText = new BitmapText(font); hudText.setSize(21f); hudText.setLocalTranslation(22f, cam.getHeight() - 24f, 0f); guiNode.attachChild(hudText);
        objectiveText = new BitmapText(font); objectiveText.setSize(23f); objectiveText.setColor(new ColorRGBA(1f, 0.88f, 0.54f, 1f));
        objectiveText.setLocalTranslation(22f, cam.getHeight() - 70f, 0f); guiNode.attachChild(objectiveText);
        messageText = new BitmapText(font); messageText.setSize(20f); messageText.setLocalTranslation(22f, 52f, 0f); guiNode.attachChild(messageText);
        BitmapText crosshair = new BitmapText(font); crosshair.setText("+"); crosshair.setSize(28f);
        crosshair.setLocalTranslation(cam.getWidth() / 2f - 7f, cam.getHeight() / 2f + 9f, 0f); guiNode.attachChild(crosshair);
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "Forward" -> movement.put(Move.FORWARD, isPressed);
            case "Back" -> movement.put(Move.BACK, isPressed);
            case "Left" -> movement.put(Move.LEFT, isPressed);
            case "Right" -> movement.put(Move.RIGHT, isPressed);
            case "Sprint" -> movement.put(Move.SPRINT, isPressed);
            case "Interact" -> { if (isPressed) interact(); }
            case "Attack" -> { if (isPressed) attack(); }
            case "CraftBlade" -> { if (isPressed) craftBlade(); }
            case "CraftCamp" -> { if (isPressed) craftCampfireKit(); }
            case "CraftHoe" -> { if (isPressed) craftHoe(); }
            case "CycleTerraform" -> { if (isPressed) cycleTerraformMode(); }
            case "Terraform" -> { if (isPressed) terraform(); }
            case "PlaceCamp" -> { if (isPressed) placeCampfire(); }
            case "Eat" -> { if (isPressed) eatBerry(); }
            case "Save" -> { if (isPressed) saveGame(); }
            default -> { }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        attackCooldown = Math.max(0f, attackCooldown - tpf); messageClock = Math.max(0f, messageClock - tpf);
        autoSaveClock += tpf; enemySpawnClock += tpf;
        updatePlayer(tpf); updateEnemies(tpf); updateSurvival(tpf); updateDayNight(tpf); updateTerraformMarker();
        progression.updateFromInventory(inventory); updateHud();
        if (enemySpawnClock >= 70f && enemies.getQuantity() < 18) { enemySpawnClock = 0f; spawnRoamingEnemy(); }
        if (autoSaveClock >= 60f) { autoSaveClock = 0f; saveGameSilently(); }
        if (messageClock <= 0f) {
            messageText.setText("E gather | LMB attack | 1 blade | 2 camp | 3 hoe | T terrain mode | G shape ground | F5 save");
        }
        if (health <= 0f) respawn();
    }

    private void updatePlayer(float tpf) {
        Vector3f forward = cam.getDirection().clone(); forward.y = 0f;
        if (forward.lengthSquared() < 0.0001f) forward.set(0f, 0f, -1f);
        forward.normalizeLocal(); Vector3f left = new Vector3f(forward.z, 0f, -forward.x); Vector3f delta = new Vector3f();
        if (movement.get(Move.FORWARD)) delta.addLocal(forward);
        if (movement.get(Move.BACK)) delta.subtractLocal(forward);
        if (movement.get(Move.LEFT)) delta.addLocal(left);
        if (movement.get(Move.RIGHT)) delta.subtractLocal(left);
        boolean moving = delta.lengthSquared() > 0.001f;
        boolean sprinting = moving && movement.get(Move.SPRINT) && stamina > 2f;
        float speed = sprinting ? 8.5f : 5.1f;
        if (moving) {
            delta.normalizeLocal().multLocal(speed * tpf);
            Vector3f current = cam.getLocation(); Vector3f next = current.add(delta);
            next.x = Math.clamp(next.x, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f);
            next.z = Math.clamp(next.z, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f);
            float currentGround = ground(current.x, current.z); float nextGround = ground(next.x, next.z);
            float slope = terrain.slopeDegrees(next.x, next.z, 0.75f);
            if (nextGround - currentGround <= 0.72f && slope <= 55f) {
                next.y = nextGround + EYE_HEIGHT; cam.setLocation(next);
            }
        }
        if (sprinting) stamina = Math.max(0f, stamina - 22f * tpf); else stamina = Math.min(100f, stamina + 16f * tpf);
    }

    private void updateEnemies(float tpf) {
        Vector3f player = cam.getLocation();
        for (Spatial spatial : new ArrayList<>(enemies.getChildren())) {
            if (!(spatial instanceof Node enemy)) continue;
            EnemyType type = EnemyType.valueOf(enemy.getUserData("enemyType"));
            Vector3f toPlayer = player.subtract(enemy.getWorldTranslation()); toPlayer.y = 0f;
            float distance = toPlayer.length(); float clock = enemy.<Float>getUserData("attackClock") - tpf;
            if (distance < type.noticeRange && distance > 1.3f) {
                toPlayer.normalizeLocal(); Vector3f local = enemy.getLocalTranslation().add(toPlayer.mult(type.speed * tpf));
                local.x = Math.clamp(local.x, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
                local.z = Math.clamp(local.z, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
                local.y = ground(local.x, local.z); enemy.setLocalTranslation(local);
            }
            if (distance <= 1.55f && clock <= 0f) {
                health = Math.max(0f, health - type.damage); clock = type.attackDelay;
                announce(type == EnemyType.GOBLIN ? "Goblin blade!" : "A graveborn strikes you!");
            }
            enemy.setUserData("attackClock", clock);
        }
    }

    private void updateSurvival(float tpf) {
        hunger = Math.max(0f, hunger - 0.35f * tpf);
        if (hunger <= 0f) health = Math.max(0f, health - 1.25f * tpf);
    }

    private void updateDayNight(float tpf) {
        dayClock = (dayClock + tpf / DAY_LENGTH_SECONDS) % 1f;
        float angle = dayClock * FastMath.TWO_PI, elevation = FastMath.sin(angle);
        sun.setDirection(new Vector3f(FastMath.cos(angle), -Math.max(0.16f, elevation), -0.35f).normalizeLocal());
        float daylight = Math.clamp(elevation * 0.75f + 0.38f, 0.08f, 1f);
        sun.setColor(new ColorRGBA(1f, 0.88f, 0.72f, 1f).mult(Math.max(0.12f, daylight)));
        ambient.setColor(new ColorRGBA(0.22f, 0.27f, 0.38f, 1f).mult(0.5f + daylight * 0.7f));
        viewPort.setBackgroundColor(new ColorRGBA(0.04f + 0.25f * daylight, 0.06f + 0.34f * daylight, 0.10f + 0.43f * daylight, 1f));
    }

    private void craftHoe() {
        if (hasHoe) { announce("You already carry the Mason's Hoe."); return; }
        Map<Inventory.Item, Integer> recipe = Map.of(Inventory.Item.WOOD, 5, Inventory.Item.STONE, 2);
        if (!inventory.consumeRecipe(recipe)) { announce("Mason's Hoe requires 5 wood and 2 stone."); return; }
        hasHoe = true; announce("Mason's Hoe crafted. T changes mode; G shapes ground.");
    }

    private void cycleTerraformMode() {
        if (!hasHoe) { announce("Craft the Mason's Hoe first [3]."); return; }
        terraformMode = terraformMode.next();
        terraformMarker.setMaterial(unshaded(terraformColor()));
        announce("Terrain mode: " + terraformMode.label);
    }

    private Vector3f terraformTarget() {
        if (terrainGeometry != null) {
            Ray ray = new Ray(cam.getLocation(), cam.getDirection());
            CollisionResults hits = new CollisionResults();
            terrainGeometry.collideWith(ray, hits);
            CollisionResult closest = hits.getClosestCollision();
            if (closest != null && closest.getDistance() <= TERRAFORM_DISTANCE) {
                Vector3f p = closest.getContactPoint().clone();
                p.x = Math.clamp(p.x, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
                p.z = Math.clamp(p.z, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
                return p;
            }
        }
        Vector3f forward = cam.getDirection().clone(); forward.y = 0f;
        if (forward.lengthSquared() < 0.001f) return null;
        forward.normalizeLocal();
        Vector3f p = cam.getLocation().add(forward.mult(4.2f));
        p.x = Math.clamp(p.x, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
        p.z = Math.clamp(p.z, -WORLD_HALF_EXTENT + 2f, WORLD_HALF_EXTENT - 2f);
        p.y = ground(p.x, p.z);
        return p;
    }

    private void updateTerraformMarker() {
        if (terraformMarker == null || !hasHoe) {
            if (terraformMarker != null) terraformMarker.setCullHint(Spatial.CullHint.Always);
            return;
        }
        Vector3f target = terraformTarget();
        if (target == null) {
            terraformMarker.setCullHint(Spatial.CullHint.Always);
            return;
        }
        terraformMarker.setCullHint(Spatial.CullHint.Never);
        terraformMarker.setLocalTranslation(target.x, target.y + 0.12f, target.z);
    }

    private ColorRGBA terraformColor() {
        return switch (terraformMode) {
            case LEVEL -> new ColorRGBA(0.92f, 0.82f, 0.28f, 1f);
            case RAISE -> new ColorRGBA(0.35f, 0.82f, 0.30f, 1f);
            case LOWER -> new ColorRGBA(0.85f, 0.28f, 0.22f, 1f);
            case SMOOTH -> new ColorRGBA(0.26f, 0.72f, 0.88f, 1f);
            case RESTORE -> new ColorRGBA(0.72f, 0.48f, 0.90f, 1f);
        };
    }

    private void terraform() {
        if (!hasHoe) { announce("Craft the Mason's Hoe first [3]."); return; }
        Vector3f target = terraformTarget();
        if (target == null) { announce("Aim at nearby ground."); return; }

        float standingHeight = ground(cam.getLocation().x, cam.getLocation().z);
        TerraformToolSystem.Result result = TerraformToolSystem.apply(
                terrain,
                TerraformToolSystem.Mode.valueOf(terraformMode.name()),
                target.x,
                target.z,
                standingHeight,
                TerraformToolSystem.DEFAULT_RADIUS,
                inventory.get(Inventory.Item.STONE),
                stamina);

        if (!result.applied()) { announce(result.message()); return; }
        if (result.stoneSpent() > 0) inventory.consume(Inventory.Item.STONE, result.stoneSpent());
        stamina = Math.max(0f, stamina - result.staminaSpent());

        TerrainState.DirtyRegion dirty = terrain.consumeDirtyRegion();
        updateTerrainPatch(dirty);
        snapNaturalObjects(target.x, target.z, TerraformToolSystem.DEFAULT_RADIUS + 1.3f);
        Vector3f player = cam.getLocation(); player.y = ground(player.x, player.z) + EYE_HEIGHT; cam.setLocation(player);
        announce(result.message());
    }

    private void snapNaturalObjects(float x, float z, float radius) {
        snapChildren(interactables, x, z, radius); snapChildren(enemies, x, z, radius);
    }

    private void snapChildren(Node parent, float x, float z, float radius) {
        float radiusSq = radius * radius;
        for (Spatial spatial : parent.getChildren()) {
            Vector3f p = spatial.getLocalTranslation(); float dx = p.x - x, dz = p.z - z;
            if (dx * dx + dz * dz <= radiusSq) spatial.setLocalTranslation(p.x, ground(p.x, p.z), p.z);
        }
    }

    private void interact() {
        Node entity = raycastEntity(INTERACT_RANGE);
        if (entity == null) { announce("Nothing useful within reach."); return; }
        String kind = entity.getUserData("kind");
        switch (kind) {
            case "TREE" -> { inventory.add(Inventory.Item.WOOD, 3); entity.removeFromParent(); announce("+3 wood"); }
            case "ROCK" -> { inventory.add(Inventory.Item.STONE, 2); entity.removeFromParent(); announce("+2 stone"); }
            case "BERRY" -> { inventory.add(Inventory.Item.BERRY, 2); entity.removeFromParent(); announce("+2 emberberries"); }
            case "SHRINE" -> activateShrine(entity);
            default -> announce("You study it, but learn nothing yet.");
        }
    }

    private void attack() {
        if (attackCooldown > 0f) return;
        attackCooldown = hasBlade ? 0.48f : 0.7f;
        Node target = raycastEntity(ATTACK_RANGE);
        if (target == null || !"ENEMY".equals(target.getUserData("kind"))) return;
        float damage = hasBlade ? 22f : 7f; float hp = target.<Float>getUserData("hp") - damage;
        if (hp <= 0f) {
            EnemyType type = EnemyType.valueOf(target.getUserData("enemyType")); target.removeFromParent();
            inventory.add(Inventory.Item.ARCANE_DUST, type == EnemyType.GOBLIN ? 1 : 2);
            if (type == EnemyType.GOBLIN) progression.markGoblinDefeated();
            announce(type.displayName + " defeated. Arcane dust recovered.");
        } else { target.setUserData("hp", hp); announce("Hit for " + Math.round(damage) + "."); }
    }

    private Node raycastEntity(float range) {
        Ray ray = new Ray(cam.getLocation(), cam.getDirection()); CollisionResults results = new CollisionResults();
        interactables.collideWith(ray, results); enemies.collideWith(ray, results);
        for (CollisionResult result : results) {
            if (result.getDistance() > range) break;
            Geometry geometry = result.getGeometry(); Spatial parent = geometry.getParent();
            if (parent instanceof Node node && node.getUserData("kind") != null) return node;
        }
        return null;
    }

    private void activateShrine(Node shrine) {
        if (Boolean.TRUE.equals(shrine.getUserData("used"))) { announce("This seal has already yielded its secret."); return; }
        shrine.setUserData("used", true); progression.markArcaneSealFound(); inventory.add(Inventory.Item.ARCANE_DUST, 3);
        Spatial crystal = shrine.getChild("crystal");
        if (crystal instanceof Geometry geometry) geometry.setMaterial(unshaded(new ColorRGBA(0.08f, 0.45f, 0.52f, 1f)));
        announce("Arcane Seal awakened.");
    }

    private void craftBlade() {
        if (hasBlade) { announce("You already carry the Wanderer's Blade."); return; }
        Map<Inventory.Item, Integer> recipe = Map.of(Inventory.Item.WOOD, 8, Inventory.Item.STONE, 4);
        if (!inventory.consumeRecipe(recipe)) { announce("Blade requires 8 wood and 4 stone."); return; }
        hasBlade = true; progression.markBladeCrafted(); announce("Wanderer's Blade crafted.");
    }

    private void craftCampfireKit() {
        Map<Inventory.Item, Integer> recipe = Map.of(Inventory.Item.WOOD, 5, Inventory.Item.STONE, 3);
        if (!inventory.consumeRecipe(recipe)) { announce("Campfire kit requires 5 wood and 3 stone."); return; }
        campfireKits++; announce("Campfire kit ready. Press Q to place.");
    }

    private void placeCampfire() {
        if (campfireKits <= 0) { announce("Craft a campfire kit first [2]."); return; }
        Vector3f flat = cam.getDirection().clone(); flat.y = 0f; flat.normalizeLocal();
        Vector3f p = cam.getLocation().add(flat.mult(2.6f)); p.y = ground(p.x, p.z);
        if (!terrain.isBuildable(p.x, p.z, 1.15f, 20f, 0.85f)) {
            announce("Ground is too rough for a camp. Level it with the hoe.");
            return;
        }
        createCampfire(p); campfireKits--; progression.markCampBuilt(); announce("Camp established.");
    }

    private void createCampfire(Vector3f p) {
        Node camp = new Node("campfire"); camp.setLocalTranslation(p);
        Material stone = litMaterial(new ColorRGBA(0.27f, 0.25f, 0.23f, 1f), 8f);
        for (int i = 0; i < 8; i++) {
            float angle = i * FastMath.TWO_PI / 8f; Geometry rock = new Geometry("camp-rock", new Sphere(6, 8, 0.18f));
            rock.setMaterial(stone); rock.setLocalTranslation(FastMath.cos(angle) * 0.55f, 0.13f, FastMath.sin(angle) * 0.55f); camp.attachChild(rock);
        }
        Geometry ember = new Geometry("embers", new Sphere(8, 10, 0.32f)); ember.setMaterial(unshaded(new ColorRGBA(1f, 0.24f, 0.03f, 1f)));
        ember.setLocalTranslation(0f, 0.23f, 0f); camp.attachChild(ember); structures.attachChild(camp);
        PointLight glow = new PointLight(); glow.setColor(new ColorRGBA(1f, 0.35f, 0.08f, 1f)); glow.setRadius(8f); glow.setPosition(p.add(0f, 0.8f, 0f)); rootNode.addLight(glow);
    }

    private void eatBerry() {
        if (!inventory.consume(Inventory.Item.BERRY, 1)) { announce("No emberberries left."); return; }
        hunger = Math.min(100f, hunger + 28f); health = Math.min(100f, health + 16f); announce("Emberberry eaten.");
    }

    private void spawnRoamingEnemy() {
        Random random = new Random(worldSeed ^ Float.floatToIntBits(dayClock) ^ enemySerial * 31L);
        float angle = random.nextFloat() * FastMath.TWO_PI, distance = random.nextFloat(22f, 34f);
        float x = Math.clamp(cam.getLocation().x + FastMath.cos(angle) * distance, -WORLD_HALF_EXTENT + 5f, WORLD_HALF_EXTENT - 5f);
        float z = Math.clamp(cam.getLocation().z + FastMath.sin(angle) * distance, -WORLD_HALF_EXTENT + 5f, WORLD_HALF_EXTENT - 5f);
        EnemyType type = dayClock > 0.5f && dayClock < 0.92f && random.nextBoolean() ? EnemyType.SKELETON : EnemyType.GOBLIN;
        spawnEnemy(type, x, z); announce("You hear movement beyond the brush...");
    }

    private void updateHud() {
        String time = dayClock > 0.52f && dayClock < 0.93f ? "Night" : "Day";
        String tool = hasHoe ? " | Hoe:" + terraformMode.label : " | No terrain tool";
        float slope = terrain.slopeDegrees(cam.getLocation().x, cam.getLocation().z, 0.75f);
        hudText.setText("HP " + Math.round(health) + "   ST " + Math.round(stamina) + "   Hunger " + Math.round(hunger) + "   " + time
                + "\nWood " + inventory.get(Inventory.Item.WOOD) + " | Stone " + inventory.get(Inventory.Item.STONE) + " | Berries "
                + inventory.get(Inventory.Item.BERRY) + " | Dust " + inventory.get(Inventory.Item.ARCANE_DUST)
                + (hasBlade ? " | Blade" : " | Bare hands") + tool
                + " | Slope " + Math.round(slope) + "deg | Edited " + terrain.modifiedSampleCount());
        objectiveText.setText("Quest: " + progression.objectiveText());
    }

    private void placePlayerAt(float x, float z) {
        cam.setLocation(new Vector3f(x, ground(x, z) + EYE_HEIGHT, z));
        cam.lookAtDirection(new Vector3f(0.4f, -0.08f, -1f).normalizeLocal(), Vector3f.UNIT_Y);
    }

    private void respawn() {
        health = 100f; stamina = 100f; hunger = 72f; placePlayerAt(0f, 0f);
        int lost = inventory.get(Inventory.Item.ARCANE_DUST) / 2;
        inventory.set(Inventory.Item.ARCANE_DUST, inventory.get(Inventory.Item.ARCANE_DUST) - lost);
        announce("You wake at the Waystone. Half your arcane dust was lost.");
    }

    private void announce(String text) { messageText.setText(text); messageClock = 4.5f; }
    private void saveGame() { if (saveGameSilently()) announce("Game saved, including terrain edits."); else announce("Save failed."); }

    private boolean saveGameSilently() {
        SaveData data = new SaveData(worldSeed, cam.getLocation().clone(), health, stamina, hunger, dayClock, inventory.snapshot(),
                progression.stage(), progression.defeatedGoblins(), progression.arcaneSeals(), hasBlade, progression.campBuilt(),
                campfireKits, hasHoe, terraformMode, terrain.encodeDeltas());
        try { data.save(); return true; } catch (IOException ex) { System.err.println("Could not save Samaheim: " + ex.getMessage()); return false; }
    }

    private void restore(SaveData save) {
        for (Inventory.Item item : Inventory.Item.values()) inventory.set(item, save.inventory().getOrDefault(item, 0));
        health = Math.clamp(save.health(), 1f, 100f); stamina = Math.clamp(save.stamina(), 0f, 100f); hunger = Math.clamp(save.hunger(), 0f, 100f);
        dayClock = save.dayClock(); hasBlade = save.hasBlade(); hasHoe = save.hasHoe(); terraformMode = save.terraformMode(); campfireKits = save.campfireKits();
        progression.restore(save.stage(), save.defeatedGoblins(), save.arcaneSeals(), save.hasBlade(), save.campBuilt());
        Vector3f position = save.position(); position.x = Math.clamp(position.x, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f);
        position.z = Math.clamp(position.z, -WORLD_HALF_EXTENT + 3f, WORLD_HALF_EXTENT - 3f); position.y = ground(position.x, position.z) + EYE_HEIGHT; cam.setLocation(position);
        if (terraformMarker != null) terraformMarker.setMaterial(unshaded(terraformColor()));
    }

    private enum Move { FORWARD, BACK, LEFT, RIGHT, SPRINT }

    private enum TerraformMode {
        LEVEL("Level"), RAISE("Raise"), LOWER("Cut"), SMOOTH("Smooth"), RESTORE("Restore");
        private final String label;
        TerraformMode(String label) { this.label = label; }
        TerraformMode next() { TerraformMode[] values = values(); return values[(ordinal() + 1) % values.length]; }
    }

    private enum EnemyType {
        GOBLIN("Goblin scout", 42f, 4.8f, 18f, 7f, 1.25f, new ColorRGBA(0.24f, 0.45f, 0.14f, 1f)),
        SKELETON("Graveborn", 68f, 3.7f, 22f, 11f, 1.45f, new ColorRGBA(0.72f, 0.72f, 0.66f, 1f));
        private final String displayName; private final float maxHealth; private final float speed; private final float noticeRange;
        private final float damage; private final float attackDelay; private final ColorRGBA color;
        EnemyType(String displayName, float maxHealth, float speed, float noticeRange, float damage, float attackDelay, ColorRGBA color) {
            this.displayName = displayName; this.maxHealth = maxHealth; this.speed = speed; this.noticeRange = noticeRange;
            this.damage = damage; this.attackDelay = attackDelay; this.color = color;
        }
    }

    private record SaveData(long seed, Vector3f position, float health, float stamina, float hunger, float dayClock,
                            Map<Inventory.Item, Integer> inventory, ProgressionState.Stage stage, int defeatedGoblins,
                            int arcaneSeals, boolean hasBlade, boolean campBuilt, int campfireKits, boolean hasHoe,
                            TerraformMode terraformMode, String terrainDeltas) {
        private static final Path SAVE_PATH = Path.of(System.getProperty("user.home"), ".samaheim", "save.properties");

        static SaveData load() {
            if (!Files.isRegularFile(SAVE_PATH)) return null;
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(SAVE_PATH)) {
                p.load(in); long seed = Long.parseLong(p.getProperty("seed"));
                Vector3f position = new Vector3f(Float.parseFloat(p.getProperty("x", "0")), Float.parseFloat(p.getProperty("y", "0")), Float.parseFloat(p.getProperty("z", "0")));
                EnumMap<Inventory.Item, Integer> inventory = new EnumMap<>(Inventory.Item.class);
                for (Inventory.Item item : Inventory.Item.values()) inventory.put(item, Integer.parseInt(p.getProperty("inv." + item.name(), "0")));
                TerraformMode mode;
                try { mode = TerraformMode.valueOf(p.getProperty("terraformMode", TerraformMode.LEVEL.name())); }
                catch (IllegalArgumentException ex) { mode = TerraformMode.LEVEL; }
                return new SaveData(seed, position, Float.parseFloat(p.getProperty("health", "100")), Float.parseFloat(p.getProperty("stamina", "100")),
                        Float.parseFloat(p.getProperty("hunger", "100")), Float.parseFloat(p.getProperty("dayClock", "0.18")), inventory,
                        ProgressionState.Stage.valueOf(p.getProperty("stage", ProgressionState.Stage.GATHER_SUPPLIES.name())),
                        Integer.parseInt(p.getProperty("goblins", "0")), Integer.parseInt(p.getProperty("seals", "0")),
                        Boolean.parseBoolean(p.getProperty("blade", "false")), Boolean.parseBoolean(p.getProperty("campBuilt", "false")),
                        Integer.parseInt(p.getProperty("campKits", "0")), Boolean.parseBoolean(p.getProperty("hoe", "false")), mode,
                        p.getProperty("terrainDeltas", ""));
            } catch (RuntimeException | IOException ex) {
                System.err.println("Ignoring invalid save: " + ex.getMessage()); return null;
            }
        }

        void save() throws IOException {
            Files.createDirectories(SAVE_PATH.getParent()); Properties p = new Properties();
            p.setProperty("seed", Long.toString(seed)); p.setProperty("x", Float.toString(position.x)); p.setProperty("y", Float.toString(position.y)); p.setProperty("z", Float.toString(position.z));
            p.setProperty("health", Float.toString(health)); p.setProperty("stamina", Float.toString(stamina)); p.setProperty("hunger", Float.toString(hunger)); p.setProperty("dayClock", Float.toString(dayClock));
            p.setProperty("stage", stage.name()); p.setProperty("goblins", Integer.toString(defeatedGoblins)); p.setProperty("seals", Integer.toString(arcaneSeals));
            p.setProperty("blade", Boolean.toString(hasBlade)); p.setProperty("campBuilt", Boolean.toString(campBuilt)); p.setProperty("campKits", Integer.toString(campfireKits));
            p.setProperty("hoe", Boolean.toString(hasHoe)); p.setProperty("terraformMode", terraformMode.name()); p.setProperty("terrainDeltas", terrainDeltas);
            for (Map.Entry<Inventory.Item, Integer> entry : inventory.entrySet()) p.setProperty("inv." + entry.getKey().name(), Integer.toString(entry.getValue()));
            try (OutputStream out = Files.newOutputStream(SAVE_PATH)) { p.store(out, "Samaheim save"); }
        }
    }
}
