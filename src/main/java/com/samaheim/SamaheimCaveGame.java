package com.samaheim;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
import com.jme3.input.KeyInput;
import com.jme3.input.MouseInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.input.controls.MouseButtonTrigger;
import com.jme3.light.AmbientLight;
import com.jme3.light.DirectionalLight;
import com.jme3.light.PointLight;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.math.FastMath;
import com.jme3.math.Ray;
import com.jme3.math.Vector3f;
import com.jme3.renderer.queue.RenderQueue;
import com.jme3.scene.Geometry;
import com.jme3.scene.Mesh;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.VertexBuffer;
import com.jme3.scene.shape.Box;
import com.jme3.scene.shape.Sphere;
import com.jme3.system.AppSettings;
import com.jme3.util.BufferUtils;
import com.samaheim.ui.SamaheimHud;
import com.samaheim.world.BuildingPhysics;
import com.samaheim.world.CaveCharacterPhysics;
import com.samaheim.world.MarchingTetraMesher;
import com.samaheim.world.SandboxPhysics;
import com.samaheim.world.VolumetricTerrain;
import com.samaheim.world.WaterPhysics;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/**
 * Main graphical sandbox. Terrain is fully volumetric: the player can dig tunnels,
 * create overhangs and enter caves instead of merely changing a heightfield.
 */
public final class SamaheimCaveGame extends SimpleApplication implements ActionListener {
    private static final float WORLD_HALF = 96f;
    private static final float MIN_Y = -24f;
    private static final float MAX_Y = 28f;
    private static final float SEA_LEVEL = -2.35f;
    private static final float PLAYER_RADIUS = 0.31f;
    private static final float PLAYER_HEIGHT = 1.78f;
    private static final float EYE_HEIGHT = 1.62f;
    private static final float TERRAFORM_REACH = 8.5f;
    private static final float GRAVITY = 19.5f;
    private static final float JUMP_SPEED = 6.4f;
    private static final float DAY_SECONDS = 900f;
    private static final int CHUNK_CELLS = 16;

    private final Node terrainRoot = new Node("volumetric-terrain");
    private final Node resources = new Node("resources");
    private final Node structures = new Node("structures");
    private final Node enemies = new Node("enemies");
    private final Map<VolumetricTerrain.ChunkKey, Geometry> terrainChunks = new HashMap<>();
    private final Set<String> removedResources = new HashSet<>();
    private final List<BuildRecord> builds = new ArrayList<>();

    private VolumetricTerrain terrain;
    private Material terrainMaterial;
    private Geometry brushRing;
    private Mesh brushMesh;
    private SamaheimHud hud;
    private DirectionalLight sun;
    private AmbientLight ambient;
    private PointLight lantern;
    private VolumetricTerrain.Hit brushHit;

    private long seed;
    private float playerX;
    private float playerZ;
    private float footY;
    private float velocityY;
    private float health = 100f;
    private float stamina = 100f;
    private float hunger = 100f;
    private float breath = 100f;
    private float dayClock = 0.18f;
    private float attackCooldown;
    private float spawnClock;
    private float saveClock;
    private float messageClock;
    private float buildYaw;
    private float brushRadius = 2.6f;
    private int wood;
    private int stone;
    private int berries;
    private int kills;
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean sprint;
    private boolean jumpRequested;
    private boolean jumpHeld;
    private boolean swimDown;
    private boolean grounded = true;
    private boolean bladeCrafted;
    private ToolMode toolMode = ToolMode.DIG;
    private BuildType buildType = BuildType.FLOOR;

    public static void main(String[] args) {
        SamaheimCaveGame game = new SamaheimCaveGame();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Samaheim - Cave Sandbox");
        settings.setResolution(1600, 900);
        settings.setVSync(true);
        settings.setSamples(4);
        game.setSettings(settings);
        game.setShowSettings(false);
        game.start();
    }

    @Override
    public void simpleInitApp() {
        SaveData save = SaveData.load();
        seed = save == null ? new Random().nextLong() : save.seed;
        terrain = new VolumetricTerrain(seed, WORLD_HALF, MIN_Y, MAX_Y, VolumetricTerrain.DEFAULT_SPACING, CHUNK_CELLS);
        if (save != null) {
            terrain.decodeEdits(save.edits);
            restoreState(save);
        }

        rootNode.attachChild(terrainRoot);
        rootNode.attachChild(resources);
        rootNode.attachChild(structures);
        rootNode.attachChild(enemies);
        configureCamera();
        configureInput();
        configureLighting();
        configureTerrainMaterial();
        configureWaterSurface();
        buildInitialTerrain();
        spawnWorld();
        restoreBuilds();
        configureBrushPreview();
        hud = new SamaheimHud(assetManager, guiNode, cam.getWidth(), cam.getHeight());

        playerX = save == null ? 0f : Math.clamp(save.x, -WORLD_HALF + 2f, WORLD_HALF - 2f);
        playerZ = save == null ? 0f : Math.clamp(save.z, -WORLD_HALF + 2f, WORLD_HALF - 2f);
        float surface = terrain.surfaceHeight(playerX, playerZ);
        footY = save == null ? surface + 0.04f : Math.max(save.footY, surface + 0.04f);
        if (!terrain.capsuleClear(playerX, footY, playerZ, PLAYER_RADIUS, PLAYER_HEIGHT)) footY = surface + 0.08f;
        updateCameraPosition();
        announce("Terrain tool equipped. Lowlands now hold water; Space rises and Ctrl dives while swimming.");
    }

    private void restoreState(SaveData save) {
        health = clamp100(save.health);
        stamina = clamp100(save.stamina);
        hunger = clamp100(save.hunger);
        dayClock = Math.max(0f, Math.min(1f, save.dayClock));
        wood = Math.max(0, save.wood);
        stone = Math.max(0, save.stone);
        berries = Math.max(0, save.berries);
        kills = Math.max(0, save.kills);
        bladeCrafted = save.blade;
        toolMode = save.toolMode;
        buildType = save.buildType;
        buildYaw = BuildingPhysics.snapYaw(save.buildYaw);
        brushRadius = Math.max(1.2f, Math.min(4.5f, save.brushRadius));
        removedResources.addAll(save.removed);
        builds.addAll(save.builds);
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(0f);
        flyCam.setZoomSpeed(0f);
        flyCam.setRotationSpeed(2.15f);
        flyCam.setDragToRotate(false);
        cam.setFrustumPerspective(72f, (float) cam.getWidth() / cam.getHeight(), 0.04f, 420f);
    }

    private void configureInput() {
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("SwimDown", new KeyTrigger(KeyInput.KEY_LCONTROL));
        inputManager.addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("Terraform", new MouseButtonTrigger(MouseInput.BUTTON_RIGHT), new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("ToolMode", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("BrushSmaller", new KeyTrigger(KeyInput.KEY_Z));
        inputManager.addMapping("BrushLarger", new KeyTrigger(KeyInput.KEY_C));
        inputManager.addMapping("CraftBlade", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("BuildMode", new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping("RotateBuild", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Build", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Dismantle", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("Eat", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("Save", new KeyTrigger(KeyInput.KEY_F5));
        inputManager.addListener(this, "Forward", "Back", "Left", "Right", "Sprint", "Jump", "SwimDown", "Interact", "Attack",
                "Terraform", "ToolMode", "BrushSmaller", "BrushLarger", "CraftBlade", "BuildMode", "RotateBuild",
                "Build", "Dismantle", "Eat", "Save");
    }

    private void configureLighting() {
        sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.6f, -1f, -0.35f).normalizeLocal());
        sun.setColor(new ColorRGBA(1f, 0.94f, 0.82f, 1f));
        rootNode.addLight(sun);
        ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.24f, 0.27f, 0.34f, 1f));
        rootNode.addLight(ambient);
        lantern = new PointLight();
        lantern.setColor(new ColorRGBA(1f, 0.72f, 0.44f, 1f));
        lantern.setRadius(11f);
        rootNode.addLight(lantern);
    }

    private void configureTerrainMaterial() {
        terrainMaterial = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        terrainMaterial.setBoolean("UseMaterialColors", true);
        terrainMaterial.setColor("Diffuse", new ColorRGBA(0.19f, 0.29f, 0.12f, 1f));
        terrainMaterial.setColor("Ambient", new ColorRGBA(0.12f, 0.18f, 0.08f, 1f));
        terrainMaterial.setColor("Specular", new ColorRGBA(0.10f, 0.10f, 0.08f, 1f));
        terrainMaterial.setFloat("Shininess", 3f);
    }

    private void configureWaterSurface() {
        Geometry water = new Geometry("water-surface", new Box(WORLD_HALF - 0.6f, 0.025f, WORLD_HALF - 0.6f));
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.055f, 0.28f, 0.39f, 0.63f));
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.getAdditionalRenderState().setDepthWrite(false);
        water.setMaterial(material);
        water.setQueueBucket(RenderQueue.Bucket.Transparent);
        water.setLocalTranslation(0f, SEA_LEVEL, 0f);
        rootNode.attachChild(water);
    }

    private void buildInitialTerrain() {
        for (int cz = 0; cz < terrain.chunkCountZ(); cz++) {
            for (int cx = 0; cx < terrain.chunkCountX(); cx++) {
                int[] range = terrain.initialChunkYRange(cx, cz);
                for (int cy = range[0]; cy <= range[1]; cy++) rebuildChunk(new VolumetricTerrain.ChunkKey(cx, cy, cz));
            }
        }
    }

    private void rebuildChunk(VolumetricTerrain.ChunkKey key) {
        Geometry old = terrainChunks.remove(key);
        if (old != null) old.removeFromParent();
        Mesh mesh = MarchingTetraMesher.buildChunk(terrain, key);
        if (mesh == null) return;
        Geometry geometry = new Geometry("terrain-" + key.x() + '-' + key.y() + '-' + key.z(), mesh);
        geometry.setMaterial(terrainMaterial);
        terrainRoot.attachChild(geometry);
        terrainChunks.put(key, geometry);
    }

    private void configureBrushPreview() {
        brushMesh = new Mesh();
        brushMesh.setMode(Mesh.Mode.LineLoop);
        brushMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(new float[64 * 3]));
        brushMesh.updateBound();
        brushRing = new Geometry("terrain-brush-preview", brushMesh);
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", new ColorRGBA(0.35f, 0.92f, 1f, 0.96f));
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        material.getAdditionalRenderState().setLineWidth(3f);
        brushRing.setMaterial(material);
        brushRing.setQueueBucket(RenderQueue.Bucket.Transparent);
        brushRing.setCullHint(Spatial.CullHint.Always);
        rootNode.attachChild(brushRing);
    }

    private void spawnWorld() {
        Random random = new Random(seed ^ 0x51A9L);
        for (int i = 0; i < 78; i++) spawnResource("tree-" + i, ResourceType.TREE, random, 16f);
        for (int i = 0; i < 56; i++) spawnResource("rock-" + i, ResourceType.ROCK, random, 12f);
        for (int i = 0; i < 30; i++) spawnResource("berry-" + i, ResourceType.BERRY, random, 10f);
        for (int i = 0; i < 10; i++) spawnEnemy(randomSurfacePoint(random, 24f), i < 7 ? EnemyType.GOBLIN : EnemyType.SKELETON);
    }

    private void spawnResource(String id, ResourceType type, Random random, float safeRadius) {
        if (removedResources.contains(id)) return;
        Vector3f p = randomSurfacePoint(random, safeRadius);
        float scale = random.nextFloat(0.82f, 1.28f);
        Node node = new Node(id);
        node.setUserData("kind", type.name());
        node.setUserData("resourceId", id);
        node.setUserData("blockRadius", type == ResourceType.TREE ? 0.29f * scale : type == ResourceType.ROCK ? 0.48f * scale : 0f);
        node.setUserData("blockHeight", type == ResourceType.TREE ? 4.6f * scale : type == ResourceType.ROCK ? 1.15f * scale : 0f);
        node.setLocalTranslation(p);
        if (type == ResourceType.TREE) {
            Geometry trunk = new Geometry("trunk", new Box(0.28f * scale, 1.55f * scale, 0.28f * scale));
            trunk.setMaterial(lit(new ColorRGBA(0.25f, 0.13f, 0.06f, 1f)));
            trunk.setLocalTranslation(0f, 1.55f * scale, 0f);
            Geometry crown = new Geometry("crown", new Sphere(8, 10, 1.25f * scale));
            crown.setMaterial(lit(new ColorRGBA(0.08f, 0.23f, 0.08f, 1f)));
            crown.setLocalTranslation(0f, 3.35f * scale, 0f);
            node.attachChild(trunk); node.attachChild(crown);
        } else if (type == ResourceType.ROCK) {
            Geometry rock = new Geometry("rock", new Sphere(8, 10, 0.66f * scale));
            rock.setLocalScale(1.32f, 0.78f, 1f);
            rock.setMaterial(lit(new ColorRGBA(0.35f, 0.36f, 0.37f, 1f)));
            rock.setLocalTranslation(0f, 0.45f * scale, 0f);
            node.attachChild(rock);
        } else {
            Geometry bush = new Geometry("bush", new Sphere(8, 10, 0.62f));
            bush.setMaterial(lit(new ColorRGBA(0.13f, 0.32f, 0.11f, 1f)));
            bush.setLocalTranslation(0f, 0.52f, 0f);
            node.attachChild(bush);
        }
        resources.attachChild(node);
    }

    private Vector3f randomSurfacePoint(Random random, float safeRadius) {
        Vector3f fallback = null;
        for (int attempt = 0; attempt < 240; attempt++) {
            float x = random.nextFloat(-WORLD_HALF + 6f, WORLD_HALF - 6f);
            float z = random.nextFloat(-WORLD_HALF + 6f, WORLD_HALF - 6f);
            if (x * x + z * z < safeRadius * safeRadius) continue;
            float y = terrain.surfaceHeight(x, z);
            fallback = new Vector3f(x, y, z);
            if (y >= SEA_LEVEL + 0.30f) return fallback;
        }
        return fallback == null ? new Vector3f(safeRadius + 2f, terrain.surfaceHeight(safeRadius + 2f, 0f), 0f) : fallback;
    }

    private void spawnEnemy(Vector3f p, EnemyType type) {
        Node enemy = new Node("enemy");
        enemy.setUserData("kind", "ENEMY");
        enemy.setUserData("enemyType", type.name());
        enemy.setUserData("hp", type.hp);
        enemy.setUserData("attackClock", 0f);
        enemy.setLocalTranslation(p);
        Geometry body = new Geometry("body", new Box(0.34f, 0.78f, 0.30f));
        body.setMaterial(lit(type.color));
        body.setLocalTranslation(0f, 0.78f, 0f);
        enemy.attachChild(body);
        enemies.attachChild(enemy);
    }

    private Material lit(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md");
        material.setBoolean("UseMaterialColors", true);
        material.setColor("Diffuse", color);
        material.setColor("Ambient", color.mult(0.62f));
        material.setColor("Specular", ColorRGBA.White.mult(0.12f));
        material.setFloat("Shininess", 5f);
        return material;
    }

    private Material unshaded(ColorRGBA color) {
        Material material = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        return material;
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "Forward" -> forward = isPressed;
            case "Back" -> back = isPressed;
            case "Left" -> left = isPressed;
            case "Right" -> right = isPressed;
            case "Sprint" -> sprint = isPressed;
            case "Jump" -> { jumpHeld = isPressed; if (isPressed) jumpRequested = true; }
            case "SwimDown" -> swimDown = isPressed;
            case "Interact" -> { if (isPressed) interact(); }
            case "Attack" -> { if (isPressed) attack(); }
            case "Terraform" -> { if (isPressed) terraform(); }
            case "ToolMode" -> { if (isPressed) cycleToolMode(); }
            case "BrushSmaller" -> { if (isPressed) changeBrush(-0.35f); }
            case "BrushLarger" -> { if (isPressed) changeBrush(0.35f); }
            case "CraftBlade" -> { if (isPressed) craftBlade(); }
            case "BuildMode" -> { if (isPressed) { buildType = buildType.next(); announce("Build: " + buildType.label); } }
            case "RotateBuild" -> { if (isPressed) { buildYaw = BuildingPhysics.snapYaw(buildYaw + FastMath.HALF_PI); announce("Build rotation: " + Math.round(buildYaw * FastMath.RAD_TO_DEG) + " degrees"); } }
            case "Build" -> { if (isPressed) placeBuild(); }
            case "Dismantle" -> { if (isPressed) dismantle(); }
            case "Eat" -> { if (isPressed) eat(); }
            case "Save" -> { if (isPressed) { saveGame(); announce("World saved."); } }
            default -> { }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        float frame = Math.min(tpf, 0.05f);
        attackCooldown = Math.max(0f, attackCooldown - frame);
        messageClock = Math.max(0f, messageClock - frame);
        spawnClock += frame;
        saveClock += frame;
        updatePlayer(frame);
        updateEnemies(frame);
        updateSurvival(frame);
        updateDayNight(frame);
        updateBrushPreview();
        updateHud();
        lantern.setPosition(cam.getLocation().clone());
        if (spawnClock > 85f && enemies.getQuantity() < 16) { spawnClock = 0f; spawnRoamingEnemy(); }
        if (saveClock > 75f) { saveClock = 0f; saveGame(); }
        if (health <= 0f) respawn();
        if (messageClock <= 0f) hud.setMessage("");
    }

    private void updatePlayer(float tpf) {
        boolean swimming = WaterPhysics.isSwimming(footY, SEA_LEVEL);
        boolean wading = WaterPhysics.isWading(footY, SEA_LEVEL);
        Vector3f forwardFlat = flatForward();
        Vector3f leftFlat = new Vector3f(forwardFlat.z, 0f, -forwardFlat.x);
        Vector3f wish = new Vector3f();
        if (forward) wish.addLocal(forwardFlat);
        if (back) wish.subtractLocal(forwardFlat);
        if (left) wish.addLocal(leftFlat);
        if (right) wish.subtractLocal(leftFlat);
        boolean moving = wish.lengthSquared() > 0.001f;
        boolean pushing = moving && sprint && stamina > 3f;
        float speed = WaterPhysics.horizontalSpeed(swimming, wading, pushing, stamina);
        if (moving) {
            wish.normalizeLocal().multLocal(speed * tpf);
            CaveCharacterPhysics.HorizontalMove volumeMove = CaveCharacterPhysics.moveHorizontal(terrain, playerX, footY, playerZ,
                    wish.x, wish.z, PLAYER_RADIUS, PLAYER_HEIGHT, 0.48f);
            SandboxPhysics.HorizontalResult obstacleMove = SandboxPhysics.resolveCapsuleHorizontalMixed(playerX, playerZ,
                    volumeMove.x(), volumeMove.z(), PLAYER_RADIUS, volumeMove.footY(), PLAYER_HEIGHT,
                    resourceBlockers(), structureBlockers());
            playerX = obstacleMove.x();
            playerZ = obstacleMove.z();
            footY = volumeMove.footY();
        }

        swimming = WaterPhysics.isSwimming(footY, SEA_LEVEL);
        grounded = !swimming && (CaveCharacterPhysics.grounded(terrain, playerX, footY, playerZ, PLAYER_RADIUS) || onBuildSupport());
        if (swimming) {
            velocityY = WaterPhysics.nextSwimVelocity(footY, SEA_LEVEL, velocityY, jumpHeld, swimDown, tpf);
            grounded = false;
        } else {
            if (jumpRequested && grounded) {
                velocityY = JUMP_SPEED;
                grounded = false;
            }
            if (!grounded) velocityY -= GRAVITY * tpf; else if (velocityY < 0f) velocityY = 0f;
        }
        jumpRequested = false;

        float previousY = footY;
        CaveCharacterPhysics.VerticalMove vertical = CaveCharacterPhysics.moveVertical(terrain, playerX, footY, playerZ,
                velocityY * tpf, PLAYER_RADIUS, PLAYER_HEIGHT);
        footY = vertical.footY();
        if (vertical.landed()) { velocityY = 0f; grounded = true; }
        if (vertical.hitCeiling()) velocityY = Math.min(0f, velocityY);
        if (!swimming) landOnBuildSupport(previousY);

        stamina = WaterPhysics.nextStamina(stamina, swimming, moving, pushing, tpf);
        updateCameraPosition();
    }

    private void updateCameraPosition() {
        cam.setLocation(new Vector3f(playerX, footY + EYE_HEIGHT, playerZ));
    }

    private boolean onBuildSupport() {
        float support = buildSupportHeight(playerX, playerZ, footY + 0.3f);
        return Float.isFinite(support) && Math.abs(footY - support) <= 0.09f;
    }

    private void landOnBuildSupport(float previousY) {
        if (velocityY > 0f) return;
        float support = buildSupportHeight(playerX, playerZ, previousY + 0.4f);
        if (!Float.isFinite(support)) return;
        if (previousY >= support - 0.06f && footY <= support + 0.08f) {
            footY = support;
            velocityY = 0f;
            grounded = true;
        }
    }

    private float buildSupportHeight(float x, float z, float currentFootY) {
        float terrainFloor = terrain.findFloor(x, z, currentFootY + 0.35f, 4f);
        if (!Float.isFinite(terrainFloor)) terrainFloor = MIN_Y;
        float support = BuildingPhysics.supportHeight(x, z, terrainFloor, currentFootY, floorSupports(), rampSupports());
        return support <= MIN_Y + 0.01f ? Float.NaN : support;
    }

    private Vector3f flatForward() {
        Vector3f direction = cam.getDirection().clone();
        direction.y = 0f;
        if (direction.lengthSquared() < 0.00001f) direction.set(0f, 0f, -1f);
        return direction.normalizeLocal();
    }

    private void updateEnemies(float tpf) {
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

    private void updateSurvival(float tpf) {
        hunger = Math.max(0f, hunger - 0.28f * tpf);
        if (hunger <= 0f) health = Math.max(0f, health - 1.0f * tpf);
        boolean headUnderwater = footY + EYE_HEIGHT < SEA_LEVEL - 0.06f;
        breath = WaterPhysics.nextBreath(breath, headUnderwater, tpf);
        if (breath <= 0f) health = Math.max(0f, health - 8f * tpf);
    }

    private void updateDayNight(float tpf) {
        dayClock = (dayClock + tpf / DAY_SECONDS) % 1f;
        float angle = dayClock * FastMath.TWO_PI;
        float elevation = FastMath.sin(angle);
        float daylight = Math.clamp(elevation * 0.75f + 0.38f, 0.07f, 1f);
        sun.setDirection(new Vector3f(FastMath.cos(angle), -Math.max(0.16f, elevation), -0.35f).normalizeLocal());
        sun.setColor(new ColorRGBA(1f, 0.88f, 0.72f, 1f).mult(Math.max(0.11f, daylight)));
        ambient.setColor(new ColorRGBA(0.20f, 0.24f, 0.31f, 1f).mult(0.42f + daylight * 0.72f));
        viewPort.setBackgroundColor(new ColorRGBA(0.03f + 0.24f * daylight, 0.05f + 0.32f * daylight,
                0.08f + 0.40f * daylight, 1f));
    }

    private void cycleToolMode() {
        toolMode = toolMode.next();
        announce("Terrain tool: " + toolMode.label + ". No resource cost.");
    }

    private void changeBrush(float delta) {
        brushRadius = Math.max(1.2f, Math.min(4.5f, brushRadius + delta));
        announce("Brush radius: " + String.format(Locale.ROOT, "%.1f", brushRadius) + "m");
    }

    private void terraform() {
        if (brushHit == null) { announce("Aim at earth within reach."); return; }
        Vector3f center;
        VolumetricTerrain.EditResult result;
        if (toolMode == ToolMode.DIG) {
            center = brushHit.point().subtract(brushHit.normal().mult(brushRadius * 0.48f));
            result = terrain.dig(center, brushRadius, brushRadius * 1.65f);
        } else if (toolMode == ToolMode.ADD) {
            center = brushHit.point().add(brushHit.normal().mult(brushRadius * 0.32f));
            result = terrain.add(center, brushRadius, brushRadius * 1.55f);
        } else {
            center = brushHit.point().subtract(brushHit.normal().mult(0.12f));
            result = terrain.smooth(center, brushRadius, 0.55f);
        }
        if (result.changedSamples() == 0) { announce("No earth changed here."); return; }
        for (VolumetricTerrain.ChunkKey key : result.dirtyChunks()) rebuildChunk(key);
        resettleNearbyObjects(center, brushRadius + 2.5f);
        announce(toolMode.label + " • " + result.changedSamples() + " density samples • free");
    }

    private void updateBrushPreview() {
        brushHit = terrain.raycast(cam.getLocation(), cam.getDirection(), TERRAFORM_REACH);
        if (brushHit == null) {
            brushRing.setCullHint(Spatial.CullHint.Always);
            hud.setCrosshairActive(false);
            return;
        }
        brushRing.setCullHint(Spatial.CullHint.Inherit);
        hud.setCrosshairActive(true);
        Vector3f n = brushHit.normal().normalize();
        Vector3f tangent = Math.abs(n.dot(Vector3f.UNIT_Y)) > 0.92f ? n.cross(Vector3f.UNIT_X) : n.cross(Vector3f.UNIT_Y);
        tangent.normalizeLocal();
        Vector3f bitangent = n.cross(tangent).normalizeLocal();
        FloatBuffer buffer = (FloatBuffer) brushMesh.getBuffer(VertexBuffer.Type.Position).getData();
        Vector3f origin = brushHit.point().add(n.mult(0.035f));
        for (int i = 0; i < 64; i++) {
            float a = i * FastMath.TWO_PI / 64f;
            Vector3f p = origin.add(tangent.mult(FastMath.cos(a) * brushRadius)).addLocal(bitangent.mult(FastMath.sin(a) * brushRadius));
            buffer.put(i * 3, p.x); buffer.put(i * 3 + 1, p.y); buffer.put(i * 3 + 2, p.z);
        }
        brushMesh.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded();
        brushMesh.updateBound();
        brushRing.updateModelBound();
        ColorRGBA color = switch (toolMode) {
            case DIG -> new ColorRGBA(0.28f, 0.86f, 1f, 0.98f);
            case ADD -> new ColorRGBA(0.48f, 1f, 0.52f, 0.98f);
            case SMOOTH -> new ColorRGBA(1f, 0.78f, 0.34f, 0.98f);
        };
        brushRing.getMaterial().setColor("Color", color);
    }

    private void resettleNearbyObjects(Vector3f center, float radius) {
        float r2 = radius * radius;
        for (Spatial spatial : resources.getChildren()) {
            Vector3f p = spatial.getLocalTranslation();
            float dx = p.x - center.x, dz = p.z - center.z;
            if (dx * dx + dz * dz > r2) continue;
            float floor = terrain.findFloor(p.x, p.z, p.y + 2f, 10f);
            if (Float.isFinite(floor)) spatial.setLocalTranslation(p.x, floor, p.z);
        }
        for (Spatial spatial : enemies.getChildren()) {
            Vector3f p = spatial.getLocalTranslation();
            float dx = p.x - center.x, dz = p.z - center.z;
            if (dx * dx + dz * dz > r2) continue;
            float floor = terrain.findFloor(p.x, p.z, p.y + 1.5f, 8f);
            if (Float.isFinite(floor)) spatial.setLocalTranslation(p.x, floor, p.z);
        }
    }

    private void interact() {
        Node resource = raycastNode(resources, 4.8f);
        if (resource != null) {
            String kind = resource.getUserData("kind");
            String id = resource.getUserData("resourceId");
            if ("TREE".equals(kind)) wood += 3;
            else if ("ROCK".equals(kind)) stone += 2;
            else if ("BERRY".equals(kind)) berries += 2;
            else return;
            removedResources.add(id);
            resource.removeFromParent();
            announce("Gathered " + kind.toLowerCase(Locale.ROOT) + ".");
            return;
        }
        Integer buildIndex = raycastBuild(4.8f);
        if (buildIndex != null && buildIndex >= 0 && buildIndex < builds.size() && builds.get(buildIndex).type == BuildType.DOOR) {
            BuildRecord record = builds.get(buildIndex);
            builds.set(buildIndex, record.withOpen(!record.open));
            rebuildStructures();
            announce(record.open ? "Door closed." : "Door opened.");
            return;
        }
        announce("Nothing usable in reach.");
    }

    private void attack() {
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

    private Node raycastNode(Node root, float range) {
        CollisionResults results = new CollisionResults();
        root.collideWith(new Ray(cam.getLocation(), cam.getDirection()), results);
        for (CollisionResult hit : results) {
            if (hit.getDistance() > range) break;
            Spatial parent = hit.getGeometry().getParent();
            if (parent instanceof Node node && node.getUserData("kind") != null) return node;
        }
        return null;
    }

    private Integer raycastBuild(float range) {
        CollisionResults results = new CollisionResults();
        structures.collideWith(new Ray(cam.getLocation(), cam.getDirection()), results);
        if (results.size() == 0 || results.getClosestCollision().getDistance() > range) return null;
        Spatial parent = results.getClosestCollision().getGeometry().getParent();
        return parent instanceof Node node ? node.getUserData("buildIndex") : null;
    }

    private void craftBlade() {
        if (bladeCrafted) { announce("You already carry the Wanderer's Blade."); return; }
        if (wood < 8 || stone < 4) { announce("Blade requires 8 wood + 4 stone."); return; }
        wood -= 8; stone -= 4; bladeCrafted = true;
        announce("Wanderer's Blade crafted.");
    }

    private void placeBuild() {
        Vector3f target;
        if (brushHit != null && brushHit.normal().y > 0.28f) target = brushHit.point().clone();
        else target = cam.getLocation().add(flatForward().mult(3.2f));
        target.x = BuildingPhysics.snap(target.x);
        target.z = BuildingPhysics.snap(target.z);
        float floor = terrain.findFloor(target.x, target.z, target.y + 2.5f, 7f);
        if (!Float.isFinite(floor)) { announce("No stable floor under this build point."); return; }
        target.y = floor;
        if (wood < buildType.wood || stone < buildType.stone) { announce(buildType.label + " needs " + buildType.wood + " wood + " + buildType.stone + " stone."); return; }
        if (buildType == BuildType.FLOOR && BuildingPhysics.floorsOverlap(target.x, target.z, floorSupports())) { announce("A floor already occupies this snap point."); return; }
        wood -= buildType.wood;
        stone -= buildType.stone;
        BuildRecord record = new BuildRecord(buildType, target.x, target.y, target.z, BuildingPhysics.snapYaw(buildYaw), false);
        builds.add(record);
        createBuild(record, builds.size() - 1);
        announce(buildType.label + " placed.");
    }

    private void dismantle() {
        Integer index = raycastBuild(4.5f);
        if (index == null || index < 0 || index >= builds.size()) { announce("Aim at a build piece to dismantle it."); return; }
        BuildRecord record = builds.remove((int) index);
        wood += Math.max(1, record.type.wood / 2);
        stone += record.type.stone / 2;
        rebuildStructures();
        announce("Dismantled " + record.type.label + ".");
    }

    private void rebuildStructures() {
        structures.detachAllChildren();
        restoreBuilds();
    }

    private void restoreBuilds() {
        for (int i = 0; i < builds.size(); i++) createBuild(builds.get(i), i);
    }

    private void createBuild(BuildRecord record, int index) {
        Node node = new Node("build-" + record.type.name().toLowerCase(Locale.ROOT));
        node.setUserData("buildIndex", index);
        node.setUserData("buildType", record.type.name());
        node.setLocalTranslation(record.x, record.y, record.z);
        node.rotate(0f, record.yaw, 0f);
        if (record.type == BuildType.FLOOR) {
            Geometry geometry = new Geometry("floor", new Box(1.4f, 0.12f, 1.4f));
            geometry.setMaterial(lit(new ColorRGBA(0.34f, 0.20f, 0.09f, 1f)));
            geometry.setLocalTranslation(0f, 0.12f, 0f);
            node.attachChild(geometry);
        } else if (record.type == BuildType.WALL) {
            Geometry geometry = new Geometry("wall", new Box(1.4f, 1.25f, 0.14f));
            geometry.setMaterial(lit(new ColorRGBA(0.31f, 0.18f, 0.08f, 1f)));
            geometry.setLocalTranslation(0f, 1.25f, 0f);
            node.attachChild(geometry);
        } else if (record.type == BuildType.RAMP) {
            for (int i = 0; i < 6; i++) {
                Geometry step = new Geometry("ramp-step", new Box(1.25f, 0.10f, 0.24f));
                step.setMaterial(lit(new ColorRGBA(0.36f, 0.21f, 0.09f, 1f)));
                float t = i / 5f;
                step.setLocalTranslation(0f, 0.11f + t * 1.25f, -1.25f + i * 0.5f);
                node.attachChild(step);
            }
        } else if (record.type == BuildType.DOOR) {
            Geometry leftPost = new Geometry("door-post", new Box(0.14f, 1.25f, 0.14f));
            Geometry rightPost = new Geometry("door-post", new Box(0.14f, 1.25f, 0.14f));
            leftPost.setMaterial(lit(new ColorRGBA(0.28f, 0.15f, 0.07f, 1f)));
            rightPost.setMaterial(leftPost.getMaterial());
            leftPost.setLocalTranslation(-1.18f, 1.25f, 0f);
            rightPost.setLocalTranslation(1.18f, 1.25f, 0f);
            node.attachChild(leftPost); node.attachChild(rightPost);
            Geometry door = new Geometry("door-panel", new Box(0.72f, 1.05f, 0.10f));
            door.setMaterial(lit(new ColorRGBA(0.38f, 0.22f, 0.09f, 1f)));
            door.setLocalTranslation(record.open ? 1.0f : 0f, 1.05f, record.open ? 0.8f : 0f);
            if (record.open) door.rotate(0f, FastMath.HALF_PI, 0f);
            node.attachChild(door);
        } else {
            for (int i = 0; i < 7; i++) {
                float a = i * FastMath.TWO_PI / 7f;
                Geometry rock = new Geometry("camp-rock", new Sphere(6, 8, 0.17f));
                rock.setMaterial(lit(new ColorRGBA(0.28f, 0.26f, 0.23f, 1f)));
                rock.setLocalTranslation(FastMath.cos(a) * 0.48f, 0.13f, FastMath.sin(a) * 0.48f);
                node.attachChild(rock);
            }
            Geometry ember = new Geometry("ember", new Sphere(8, 10, 0.28f));
            ember.setMaterial(unshaded(new ColorRGBA(1f, 0.25f, 0.03f, 1f)));
            ember.setLocalTranslation(0f, 0.22f, 0f);
            node.attachChild(ember);
        }
        structures.attachChild(node);
    }

    private List<BuildingPhysics.Floor> floorSupports() {
        List<BuildingPhysics.Floor> out = new ArrayList<>();
        for (BuildRecord record : builds) if (record.type == BuildType.FLOOR) out.add(new BuildingPhysics.Floor(record.x, record.y, record.z, record.yaw));
        return out;
    }

    private List<BuildingPhysics.Ramp> rampSupports() {
        List<BuildingPhysics.Ramp> out = new ArrayList<>();
        for (BuildRecord record : builds) if (record.type == BuildType.RAMP) out.add(new BuildingPhysics.Ramp(record.x, record.y, record.z, record.yaw));
        return out;
    }

    private List<SandboxPhysics.HeightCircleBlocker> resourceBlockers() {
        List<SandboxPhysics.HeightCircleBlocker> out = new ArrayList<>();
        for (Spatial spatial : resources.getChildren()) {
            Float radius = spatial.getUserData("blockRadius");
            Float height = spatial.getUserData("blockHeight");
            if (radius == null || height == null || radius <= 0f) continue;
            Vector3f p = spatial.getLocalTranslation();
            out.add(new SandboxPhysics.HeightCircleBlocker(p.x, p.z, radius, p.y, p.y + height));
        }
        return out;
    }

    private List<SandboxPhysics.HeightBoxBlocker> structureBlockers() {
        List<SandboxPhysics.HeightBoxBlocker> out = new ArrayList<>();
        for (BuildRecord record : builds) {
            if (record.type == BuildType.WALL) out.add(new SandboxPhysics.HeightBoxBlocker(record.x, record.z, 1.4f, 0.14f, record.yaw, record.y, record.y + 2.5f));
            else if (record.type == BuildType.DOOR && !record.open) out.add(new SandboxPhysics.HeightBoxBlocker(record.x, record.z, 0.76f, 0.10f, record.yaw, record.y, record.y + 2.1f));
            else if (record.type == BuildType.CAMPFIRE) out.add(new SandboxPhysics.HeightBoxBlocker(record.x, record.z, 0.48f, 0.48f, record.yaw, record.y, record.y + 0.45f));
        }
        return out;
    }

    private void eat() {
        if (berries <= 0) { announce("No berries."); return; }
        berries--;
        hunger = Math.min(100f, hunger + 26f);
        health = Math.min(100f, health + 12f);
        announce("Berry eaten.");
    }

    private void spawnRoamingEnemy() {
        Random random = new Random(seed ^ Float.floatToIntBits(dayClock) ^ kills * 131L);
        float angle = random.nextFloat() * FastMath.TWO_PI;
        float distance = random.nextFloat(18f, 30f);
        float x = Math.clamp(playerX + FastMath.cos(angle) * distance, -WORLD_HALF + 4f, WORLD_HALF - 4f);
        float z = Math.clamp(playerZ + FastMath.sin(angle) * distance, -WORLD_HALF + 4f, WORLD_HALF - 4f);
        float y = terrain.surfaceHeight(x, z);
        if (y < SEA_LEVEL + 0.25f) return;
        spawnEnemy(new Vector3f(x, y, z), random.nextBoolean() ? EnemyType.GOBLIN : EnemyType.SKELETON);
    }

    private void updateHud() {
        boolean swimming = WaterPhysics.isSwimming(footY, SEA_LEVEL);
        boolean wading = WaterPhysics.isWading(footY, SEA_LEVEL);
        String objective = objectiveText();
        String resourcesText = "WOOD  " + wood + "    STONE  " + stone + "    BERRIES  " + berries + "    KILLS  " + kills;
        if (breath < 99.5f) resourcesText += "    BREATH  " + Math.round(breath);
        String tool = "TERRAIN • " + toolMode.label.toUpperCase(Locale.ROOT) + " • " + String.format(Locale.ROOT, "%.1fm", brushRadius);
        String detail;
        if (swimming) detail = "SWIMMING • Space rise   Ctrl dive   Shift push     |     stamina drains in deep water";
        else if (wading) detail = "WADING • movement slowed     |     RMB/G terrain   B build   Q place   X remove";
        else detail = "RMB/G use   T mode   Z/C size     |     B build piece   Q place   F rotate   X remove";
        hud.update(health, stamina, hunger, objective, resourcesText, tool, detail);
    }

    private String objectiveText() {
        long digs = terrain.edits().stream().filter(edit -> edit.mode() == VolumetricTerrain.EditMode.DIG).count();
        if (digs < 3) return "Carve a tunnel into a hillside (" + digs + "/3 digs)";
        if (builds.stream().noneMatch(record -> record.type == BuildType.FLOOR)) return "Gather wood and place a floor with Q";
        if (!bladeCrafted) return "Craft the Wanderer's Blade [1]";
        if (kills < 4) return "Defeat 4 creatures (" + kills + "/4)";
        return "Sandbox open: excavate caves, cross water, build, explore and survive";
    }

    private void respawn() {
        health = 100f; stamina = 100f; hunger = 70f; breath = 100f; velocityY = 0f;
        playerX = 0f; playerZ = 0f; footY = terrain.surfaceHeight(0f, 0f) + 0.06f;
        updateCameraPosition();
        announce("You wake at the Waystone.");
    }

    private void announce(String text) {
        if (hud != null) hud.setMessage(text);
        messageClock = 4.5f;
    }

    private void saveGame() {
        SaveData data = new SaveData(seed, playerX, footY, playerZ, health, stamina, hunger, dayClock, wood, stone, berries,
                kills, bladeCrafted, toolMode, buildType, buildYaw, brushRadius, terrain.encodeEdits(),
                new HashSet<>(removedResources), new ArrayList<>(builds));
        try { data.save(); } catch (IOException ex) { System.err.println("Save failed: " + ex.getMessage()); }
    }

    private static float clamp100(float value) { return Math.max(0f, Math.min(100f, value)); }

    private enum ToolMode {
        DIG("Dig"), ADD("Add Earth"), SMOOTH("Smooth");
        private final String label;
        ToolMode(String label) { this.label = label; }
        ToolMode next() { ToolMode[] all = values(); return all[(ordinal() + 1) % all.length]; }
    }

    private enum ResourceType { TREE, ROCK, BERRY }

    private enum EnemyType {
        GOBLIN("Goblin", 42f, 3.9f, 17f, 7f, 1.2f, new ColorRGBA(0.24f, 0.45f, 0.14f, 1f)),
        SKELETON("Graveborn", 68f, 3.2f, 20f, 11f, 1.45f, new ColorRGBA(0.72f, 0.72f, 0.66f, 1f));
        private final String label;
        private final float hp;
        private final float speed;
        private final float notice;
        private final float damage;
        private final float delay;
        private final ColorRGBA color;
        EnemyType(String label, float hp, float speed, float notice, float damage, float delay, ColorRGBA color) {
            this.label = label; this.hp = hp; this.speed = speed; this.notice = notice; this.damage = damage; this.delay = delay; this.color = color;
        }
    }

    private enum BuildType {
        FLOOR("Floor", 3, 0), WALL("Wall", 4, 0), RAMP("Ramp", 4, 0), DOOR("Door", 5, 0), CAMPFIRE("Campfire", 2, 3);
        private final String label;
        private final int wood;
        private final int stone;
        BuildType(String label, int wood, int stone) { this.label = label; this.wood = wood; this.stone = stone; }
        BuildType next() { BuildType[] all = values(); return all[(ordinal() + 1) % all.length]; }
    }

    private record BuildRecord(BuildType type, float x, float y, float z, float yaw, boolean open) {
        BuildRecord withOpen(boolean value) { return new BuildRecord(type, x, y, z, yaw, value); }
        String encode() { return type.name() + ',' + x + ',' + y + ',' + z + ',' + yaw + ',' + open; }
        static BuildRecord decode(String text) {
            String[] p = text.split(",");
            if (p.length < 4) throw new IllegalArgumentException("Invalid build record");
            float yaw = p.length >= 5 ? Float.parseFloat(p[4]) : 0f;
            boolean open = p.length >= 6 && Boolean.parseBoolean(p[5]);
            return new BuildRecord(BuildType.valueOf(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]), BuildingPhysics.snapYaw(yaw), open);
        }
    }

    private static final class SaveData {
        private static final Path PATH = Path.of(System.getProperty("user.home"), ".samaheim", "cave-save.properties");
        private final long seed;
        private final float x;
        private final float footY;
        private final float z;
        private final float health;
        private final float stamina;
        private final float hunger;
        private final float dayClock;
        private final int wood;
        private final int stone;
        private final int berries;
        private final int kills;
        private final boolean blade;
        private final ToolMode toolMode;
        private final BuildType buildType;
        private final float buildYaw;
        private final float brushRadius;
        private final String edits;
        private final Set<String> removed;
        private final List<BuildRecord> builds;

        SaveData(long seed, float x, float footY, float z, float health, float stamina, float hunger, float dayClock,
                 int wood, int stone, int berries, int kills, boolean blade, ToolMode toolMode, BuildType buildType,
                 float buildYaw, float brushRadius, String edits, Set<String> removed, List<BuildRecord> builds) {
            this.seed = seed; this.x = x; this.footY = footY; this.z = z; this.health = health; this.stamina = stamina;
            this.hunger = hunger; this.dayClock = dayClock; this.wood = wood; this.stone = stone; this.berries = berries;
            this.kills = kills; this.blade = blade; this.toolMode = toolMode; this.buildType = buildType;
            this.buildYaw = buildYaw; this.brushRadius = brushRadius; this.edits = edits; this.removed = removed; this.builds = builds;
        }

        static SaveData load() {
            if (!Files.isRegularFile(PATH)) return null;
            Properties p = new Properties();
            try (InputStream input = Files.newInputStream(PATH)) {
                p.load(input);
                Set<String> removed = new HashSet<>();
                String removedText = p.getProperty("removed", "");
                if (!removedText.isBlank()) for (String id : removedText.split(";")) if (!id.isBlank()) removed.add(id);
                List<BuildRecord> builds = new ArrayList<>();
                String buildsText = p.getProperty("builds", "");
                if (!buildsText.isBlank()) for (String item : buildsText.split(";")) if (!item.isBlank()) builds.add(BuildRecord.decode(item));
                return new SaveData(Long.parseLong(p.getProperty("seed")), Float.parseFloat(p.getProperty("x", "0")),
                        Float.parseFloat(p.getProperty("footY", "0")), Float.parseFloat(p.getProperty("z", "0")),
                        Float.parseFloat(p.getProperty("health", "100")), Float.parseFloat(p.getProperty("stamina", "100")),
                        Float.parseFloat(p.getProperty("hunger", "100")), Float.parseFloat(p.getProperty("dayClock", "0.18")),
                        Integer.parseInt(p.getProperty("wood", "0")), Integer.parseInt(p.getProperty("stone", "0")),
                        Integer.parseInt(p.getProperty("berries", "0")), Integer.parseInt(p.getProperty("kills", "0")),
                        Boolean.parseBoolean(p.getProperty("blade", "false")), ToolMode.valueOf(p.getProperty("toolMode", ToolMode.DIG.name())),
                        BuildType.valueOf(p.getProperty("buildType", BuildType.FLOOR.name())), Float.parseFloat(p.getProperty("buildYaw", "0")),
                        Float.parseFloat(p.getProperty("brushRadius", "2.6")), p.getProperty("edits", ""), removed, builds);
            } catch (RuntimeException | IOException ex) {
                System.err.println("Ignoring invalid cave save: " + ex.getMessage());
                return null;
            }
        }

        void save() throws IOException {
            Files.createDirectories(PATH.getParent());
            Properties p = new Properties();
            p.setProperty("seed", Long.toString(seed));
            p.setProperty("x", Float.toString(x)); p.setProperty("footY", Float.toString(footY)); p.setProperty("z", Float.toString(z));
            p.setProperty("health", Float.toString(health)); p.setProperty("stamina", Float.toString(stamina)); p.setProperty("hunger", Float.toString(hunger));
            p.setProperty("dayClock", Float.toString(dayClock)); p.setProperty("wood", Integer.toString(wood)); p.setProperty("stone", Integer.toString(stone));
            p.setProperty("berries", Integer.toString(berries)); p.setProperty("kills", Integer.toString(kills)); p.setProperty("blade", Boolean.toString(blade));
            p.setProperty("toolMode", toolMode.name()); p.setProperty("buildType", buildType.name()); p.setProperty("buildYaw", Float.toString(buildYaw));
            p.setProperty("brushRadius", Float.toString(brushRadius)); p.setProperty("edits", edits); p.setProperty("removed", String.join(";", removed));
            StringBuilder buildText = new StringBuilder();
            for (BuildRecord record : builds) { if (buildText.length() > 0) buildText.append(';'); buildText.append(record.encode()); }
            p.setProperty("builds", buildText.toString());
            try (OutputStream output = Files.newOutputStream(PATH)) { p.store(output, "Samaheim volumetric cave save"); }
        }
    }
}
