package com.samaheim;

import com.jme3.app.SimpleApplication;
import com.jme3.collision.CollisionResult;
import com.jme3.collision.CollisionResults;
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
import com.samaheim.world.BuildingPhysics;
import com.samaheim.world.SandboxPhysics;
import com.samaheim.world.TerrainState;
import com.samaheim.world.TerraformToolSystem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.FloatBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

/** Playable physical sandbox with walkable persistent building pieces. */
public final class SamaheimBuilderGame extends SimpleApplication implements ActionListener {
    private static final float WORLD_HALF = 116f;
    private static final float EYE_HEIGHT = 1.72f;
    private static final float PLAYER_RADIUS = 0.42f;
    private static final float DAY_SECONDS = 900f;
    private static final float TERRAFORM_DISTANCE = 6.2f;

    private final Node resources = new Node("resources");
    private final Node structures = new Node("structures");
    private final Node enemies = new Node("enemies");
    private final Set<String> removedResources = new HashSet<>();
    private final List<BuildRecord> built = new ArrayList<>();

    private TerrainState terrain;
    private Mesh terrainMesh;
    private Geometry terrainGeometry;
    private DirectionalLight sun;
    private AmbientLight ambient;
    private BitmapText hud;
    private BitmapText message;
    private BitmapText objective;

    private long seed;
    private boolean forward;
    private boolean back;
    private boolean left;
    private boolean right;
    private boolean sprint;
    private boolean jumpRequested;
    private boolean grounded = true;
    private float velocityY;
    private float health = 100f;
    private float stamina = 100f;
    private float hunger = 100f;
    private float dayClock = 0.18f;
    private float attackCooldown;
    private float spawnClock;
    private float saveClock;
    private float messageClock;
    private float buildYaw;
    private int wood;
    private int stone;
    private int berries;
    private int kills;
    private boolean hoeCrafted;
    private boolean bladeCrafted;
    private TerraformToolSystem.Mode terrainMode = TerraformToolSystem.Mode.LEVEL;
    private BuildType buildType = BuildType.FLOOR;

    public static void main(String[] args) {
        SamaheimBuilderGame game = new SamaheimBuilderGame();
        AppSettings settings = new AppSettings(true);
        settings.setTitle("Samaheim - Builder Sandbox");
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
        terrain = new TerrainState(seed, WORLD_HALF, 160, 8f);
        if (save != null) restoreState(save);
        rootNode.attachChild(resources);
        rootNode.attachChild(structures);
        rootNode.attachChild(enemies);
        configureInput();
        configureCamera();
        configureLighting();
        buildTerrain();
        spawnWorld();
        restoreBuilds();
        configureHud();
        setPlayerXZ(save == null ? 0f : save.x, save == null ? 0f : save.z);
        announce(save == null ? "Shape the ground, then build a walkable shelter." : "Terrain, harvests and physical builds restored.");
    }

    private void restoreState(SaveData save) {
        terrain.decodeDeltas(save.terrain);
        removedResources.addAll(save.removed);
        built.addAll(save.builds);
        health = save.health;
        stamina = save.stamina;
        hunger = save.hunger;
        dayClock = save.dayClock;
        wood = save.wood;
        stone = save.stone;
        berries = save.berries;
        kills = save.kills;
        hoeCrafted = save.hoe;
        bladeCrafted = save.blade;
        terrainMode = save.mode;
        buildType = save.buildType;
        buildYaw = save.buildYaw;
    }

    private void configureInput() {
        inputManager.addMapping("Forward", new KeyTrigger(KeyInput.KEY_W));
        inputManager.addMapping("Back", new KeyTrigger(KeyInput.KEY_S));
        inputManager.addMapping("Left", new KeyTrigger(KeyInput.KEY_A));
        inputManager.addMapping("Right", new KeyTrigger(KeyInput.KEY_D));
        inputManager.addMapping("Sprint", new KeyTrigger(KeyInput.KEY_LSHIFT));
        inputManager.addMapping("Jump", new KeyTrigger(KeyInput.KEY_SPACE));
        inputManager.addMapping("Interact", new KeyTrigger(KeyInput.KEY_E));
        inputManager.addMapping("Attack", new MouseButtonTrigger(MouseInput.BUTTON_LEFT));
        inputManager.addMapping("CraftHoe", new KeyTrigger(KeyInput.KEY_1));
        inputManager.addMapping("CraftBlade", new KeyTrigger(KeyInput.KEY_2));
        inputManager.addMapping("TerrainMode", new KeyTrigger(KeyInput.KEY_T));
        inputManager.addMapping("Terraform", new KeyTrigger(KeyInput.KEY_G));
        inputManager.addMapping("BuildMode", new KeyTrigger(KeyInput.KEY_B));
        inputManager.addMapping("RotateBuild", new KeyTrigger(KeyInput.KEY_F));
        inputManager.addMapping("Build", new KeyTrigger(KeyInput.KEY_Q));
        inputManager.addMapping("Dismantle", new KeyTrigger(KeyInput.KEY_X));
        inputManager.addMapping("Eat", new KeyTrigger(KeyInput.KEY_R));
        inputManager.addMapping("Save", new KeyTrigger(KeyInput.KEY_F5));
        inputManager.addListener(this, "Forward", "Back", "Left", "Right", "Sprint", "Jump", "Interact", "Attack",
                "CraftHoe", "CraftBlade", "TerrainMode", "Terraform", "BuildMode", "RotateBuild", "Build", "Dismantle", "Eat", "Save");
    }

    private void configureCamera() {
        flyCam.setMoveSpeed(0f);
        flyCam.setZoomSpeed(0f);
        flyCam.setRotationSpeed(2.2f);
        flyCam.setDragToRotate(false);
        cam.setFrustumPerspective(70f, (float) cam.getWidth() / cam.getHeight(), 0.05f, 500f);
    }

    private void configureLighting() {
        sun = new DirectionalLight();
        sun.setDirection(new Vector3f(-0.6f, -1f, -0.35f).normalizeLocal());
        sun.setColor(new ColorRGBA(1f, 0.94f, 0.82f, 1f));
        rootNode.addLight(sun);
        ambient = new AmbientLight();
        ambient.setColor(new ColorRGBA(0.27f, 0.30f, 0.37f, 1f));
        rootNode.addLight(ambient);
    }

    private void configureHud() {
        hud = text(20f, 20f, cam.getHeight() - 22f);
        objective = text(21f, 20f, cam.getHeight() - 66f);
        objective.setColor(new ColorRGBA(1f, 0.87f, 0.52f, 1f));
        message = text(19f, 20f, 44f);
        BitmapText crosshair = text(28f, cam.getWidth() * 0.5f - 6f, cam.getHeight() * 0.5f + 8f);
        crosshair.setText("+");
    }

    private BitmapText text(float size, float x, float y) {
        BitmapText out = new BitmapText(assetManager.loadFont("Interface/Fonts/Default.fnt"));
        out.setSize(size); out.setLocalTranslation(x, y, 0f); guiNode.attachChild(out); return out;
    }

    private void buildTerrain() {
        int cells = terrain.cells(); int width = terrain.width();
        float[] pos = new float[width * width * 3]; float[] normals = new float[width * width * 3]; int[] indices = new int[cells * cells * 6];
        int v = 0;
        for (int z = 0; z < width; z++) for (int x = 0; x < width; x++) {
            pos[v * 3] = terrain.vertexWorldX(x); pos[v * 3 + 1] = terrain.vertexHeight(x, z); pos[v * 3 + 2] = terrain.vertexWorldZ(z);
            Vector3f n = terrainNormal(x, z); normals[v * 3] = n.x; normals[v * 3 + 1] = n.y; normals[v * 3 + 2] = n.z; v++;
        }
        int c = 0;
        for (int z = 0; z < cells; z++) for (int x = 0; x < cells; x++) {
            int a = z * width + x, b = a + 1, cc = a + width, d = cc + 1;
            indices[c++] = a; indices[c++] = cc; indices[c++] = b; indices[c++] = b; indices[c++] = cc; indices[c++] = d;
        }
        terrainMesh = new Mesh();
        terrainMesh.setBuffer(VertexBuffer.Type.Position, 3, BufferUtils.createFloatBuffer(pos));
        terrainMesh.setBuffer(VertexBuffer.Type.Normal, 3, BufferUtils.createFloatBuffer(normals));
        terrainMesh.setBuffer(VertexBuffer.Type.Index, 3, BufferUtils.createIntBuffer(indices));
        terrainMesh.setDynamic(); terrainMesh.updateBound();
        terrainGeometry = new Geometry("terrain", terrainMesh); terrainGeometry.setMaterial(lit(new ColorRGBA(0.16f, 0.30f, 0.12f, 1f))); rootNode.attachChild(terrainGeometry);
        terrain.consumeDirtyRegion();
    }

    private Vector3f terrainNormal(int x, int z) {
        int max = terrain.cells(); int xl = Math.max(0, x - 1), xr = Math.min(max, x + 1), zd = Math.max(0, z - 1), zu = Math.min(max, z + 1);
        float dx = terrain.vertexHeight(xl, z) - terrain.vertexHeight(xr, z); float dz = terrain.vertexHeight(x, zd) - terrain.vertexHeight(x, zu);
        return new Vector3f(dx, terrain.cellSize() * 2f, dz).normalizeLocal();
    }

    private void updateTerrainPatch(TerrainState.DirtyRegion region) {
        if (region == null) return;
        FloatBuffer pos = (FloatBuffer) terrainMesh.getBuffer(VertexBuffer.Type.Position).getData();
        FloatBuffer nor = (FloatBuffer) terrainMesh.getBuffer(VertexBuffer.Type.Normal).getData();
        int width = terrain.width(); int minX = Math.max(0, region.minX() - 1), maxX = Math.min(terrain.cells(), region.maxX() + 1);
        int minZ = Math.max(0, region.minZ() - 1), maxZ = Math.min(terrain.cells(), region.maxZ() + 1);
        for (int z = minZ; z <= maxZ; z++) for (int x = minX; x <= maxX; x++) {
            int i = z * width + x; pos.put(i * 3 + 1, terrain.vertexHeight(x, z)); Vector3f n = terrainNormal(x, z);
            nor.put(i * 3, n.x); nor.put(i * 3 + 1, n.y); nor.put(i * 3 + 2, n.z);
        }
        terrainMesh.getBuffer(VertexBuffer.Type.Position).setUpdateNeeded(); terrainMesh.getBuffer(VertexBuffer.Type.Normal).setUpdateNeeded();
        terrainMesh.updateBound(); terrainGeometry.updateModelBound();
    }

    private void spawnWorld() {
        Random r = new Random(seed);
        for (int i = 0; i < 90; i++) spawnResource("tree-" + i, ResourceType.TREE, randomPoint(r, 15f), r.nextFloat(0.8f, 1.3f));
        for (int i = 0; i < 65; i++) spawnResource("rock-" + i, ResourceType.ROCK, randomPoint(r, 12f), r.nextFloat(0.7f, 1.4f));
        for (int i = 0; i < 34; i++) spawnResource("berry-" + i, ResourceType.BERRY, randomPoint(r, 10f), 1f);
        for (int i = 0; i < 10; i++) spawnEnemy(randomPoint(r, 22f), i < 7 ? EnemyType.GOBLIN : EnemyType.SKELETON);
    }

    private Vector3f randomPoint(Random r, float safeRadius) {
        float x, z; do { x = r.nextFloat(-WORLD_HALF + 6f, WORLD_HALF - 6f); z = r.nextFloat(-WORLD_HALF + 6f, WORLD_HALF - 6f); } while (x * x + z * z < safeRadius * safeRadius);
        return new Vector3f(x, terrain.sampleHeight(x, z), z);
    }

    private void spawnResource(String id, ResourceType type, Vector3f p, float scale) {
        if (removedResources.contains(id)) return;
        Node n = new Node(id); n.setUserData("kind", type.name()); n.setUserData("resourceId", id);
        n.setUserData("blockRadius", type == ResourceType.TREE ? 0.55f * scale : type == ResourceType.ROCK ? 0.8f * scale : 0f); n.setLocalTranslation(p);
        if (type == ResourceType.TREE) {
            Geometry trunk = new Geometry("trunk", new Box(0.34f * scale, 1.55f * scale, 0.34f * scale)); trunk.setMaterial(lit(new ColorRGBA(0.25f, 0.13f, 0.06f, 1f))); trunk.setLocalTranslation(0f, 1.55f * scale, 0f);
            Geometry crown = new Geometry("crown", new Sphere(8, 10, 1.35f * scale)); crown.setMaterial(lit(new ColorRGBA(0.08f, 0.23f, 0.08f, 1f))); crown.setLocalTranslation(0f, 3.5f * scale, 0f); n.attachChild(trunk); n.attachChild(crown);
        } else if (type == ResourceType.ROCK) {
            Geometry rock = new Geometry("rock", new Sphere(8, 10, 0.78f * scale)); rock.setLocalScale(1.35f, 0.82f, 1f); rock.setMaterial(lit(new ColorRGBA(0.34f, 0.36f, 0.38f, 1f))); rock.setLocalTranslation(0f, 0.52f * scale, 0f); n.attachChild(rock);
        } else {
            Geometry bush = new Geometry("bush", new Sphere(8, 10, 0.68f)); bush.setMaterial(lit(new ColorRGBA(0.13f, 0.32f, 0.11f, 1f))); bush.setLocalTranslation(0f, 0.55f, 0f); n.attachChild(bush);
        }
        resources.attachChild(n);
    }

    private void spawnEnemy(Vector3f p, EnemyType type) {
        Node enemy = new Node("enemy"); enemy.setUserData("kind", "ENEMY"); enemy.setUserData("enemyType", type.name()); enemy.setUserData("hp", type.hp); enemy.setUserData("attackClock", 0f); enemy.setLocalTranslation(p);
        Geometry body = new Geometry("body", new Box(0.38f, 0.82f, 0.34f)); body.setMaterial(lit(type.color)); body.setLocalTranslation(0f, 0.82f, 0f); enemy.attachChild(body); enemies.attachChild(enemy);
    }

    private Material lit(ColorRGBA color) {
        Material m = new Material(assetManager, "Common/MatDefs/Light/Lighting.j3md"); m.setBoolean("UseMaterialColors", true); m.setColor("Diffuse", color); m.setColor("Ambient", color.mult(0.65f)); m.setColor("Specular", ColorRGBA.White.mult(0.15f)); m.setFloat("Shininess", 6f); return m;
    }

    private Material unshaded(ColorRGBA color) { Material m = new Material(assetManager, "Common/MatDefs/Misc/Unshaded.j3md"); m.setColor("Color", color); return m; }

    @Override
    public void onAction(String name, boolean pressed, float tpf) {
        switch (name) {
            case "Forward" -> forward = pressed; case "Back" -> back = pressed; case "Left" -> left = pressed; case "Right" -> right = pressed; case "Sprint" -> sprint = pressed;
            case "Jump" -> { if (pressed) jumpRequested = true; } case "Interact" -> { if (pressed) interact(); } case "Attack" -> { if (pressed) attack(); }
            case "CraftHoe" -> { if (pressed) craftHoe(); } case "CraftBlade" -> { if (pressed) craftBlade(); }
            case "TerrainMode" -> { if (pressed && hoeCrafted) { terrainMode = terrainMode.next(); announce("Terrain: " + terrainMode.label()); } }
            case "Terraform" -> { if (pressed) terraform(); } case "BuildMode" -> { if (pressed) { buildType = buildType.next(); announce("Build: " + buildType.label); } }
            case "RotateBuild" -> { if (pressed) { buildYaw = BuildingPhysics.snapYaw(buildYaw + FastMath.HALF_PI); announce("Build rotation: " + Math.round(buildYaw * FastMath.RAD_TO_DEG) + " deg"); } }
            case "Build" -> { if (pressed) placeBuild(); } case "Dismantle" -> { if (pressed) dismantle(); } case "Eat" -> { if (pressed) eat(); }
            case "Save" -> { if (pressed) { saveGame(); announce("World saved."); } } default -> { }
        }
    }

    @Override
    public void simpleUpdate(float tpf) {
        attackCooldown = Math.max(0f, attackCooldown - tpf); messageClock = Math.max(0f, messageClock - tpf); spawnClock += tpf; saveClock += tpf;
        updatePlayer(tpf); updateEnemies(tpf); updateSurvival(tpf); updateDayNight(tpf); updateHud();
        if (spawnClock > 75f && enemies.getQuantity() < 16) { spawnClock = 0f; Random r = new Random(seed ^ System.nanoTime()); spawnEnemy(randomPoint(r, 20f), dayClock > 0.55f ? EnemyType.SKELETON : EnemyType.GOBLIN); }
        if (saveClock > 60f) { saveClock = 0f; saveGame(); } if (health <= 0f) respawn();
        if (messageClock <= 0f) message.setText("WASD | Space jump | E gather | LMB attack | 1/2 craft | T/G terrain | B mode | F rotate | Q build | X dismantle | R eat");
    }

    private void updatePlayer(float tpf) {
        Vector3f f = cam.getDirection().clone(); f.y = 0f; if (f.lengthSquared() < 0.001f) f.set(0f, 0f, -1f); f.normalizeLocal(); Vector3f l = new Vector3f(f.z, 0f, -f.x);
        Vector3f move = new Vector3f(); if (forward) move.addLocal(f); if (back) move.subtractLocal(f); if (left) move.addLocal(l); if (right) move.subtractLocal(l);
        boolean moving = move.lengthSquared() > 0.001f; boolean running = moving && sprint && stamina > 1f && grounded; float speed = running ? 8.2f : 5.0f;
        Vector3f current = cam.getLocation().clone(); float targetX = current.x, targetZ = current.z;
        if (moving) {
            move.normalizeLocal().multLocal(speed * tpf); targetX = Math.clamp(current.x + move.x, -WORLD_HALF + 2f, WORLD_HALF - 2f); targetZ = Math.clamp(current.z + move.z, -WORLD_HALF + 2f, WORLD_HALF - 2f);
            if (terrain.slopeDegrees(targetX, targetZ, 0.75f) > 58f) { targetX = current.x; targetZ = current.z; }
            SandboxPhysics.HorizontalResult horizontal = SandboxPhysics.resolveHorizontalMixed(current.x, current.z, targetX, targetZ, PLAYER_RADIUS, circleBlockers(), boxBlockers());
            targetX = horizontal.x(); targetZ = horizontal.z();
        }
        float terrainY = terrain.sampleHeight(targetX, targetZ); float currentFoot = current.y - EYE_HEIGHT;
        float supportY = BuildingPhysics.supportHeight(targetX, targetZ, terrainY, currentFoot, floorSupports());
        SandboxPhysics.VerticalResult vertical = SandboxPhysics.stepVertical(current.y, velocityY, supportY, EYE_HEIGHT, jumpRequested, tpf);
        jumpRequested = false; velocityY = vertical.velocityY(); grounded = vertical.grounded(); cam.setLocation(new Vector3f(targetX, vertical.eyeY(), targetZ));
        if (running) stamina = Math.max(0f, stamina - 21f * tpf); else stamina = Math.min(100f, stamina + (grounded ? 16f : 7f) * tpf);
    }

    private List<SandboxPhysics.CircleBlocker> circleBlockers() {
        List<SandboxPhysics.CircleBlocker> out = new ArrayList<>();
        for (Spatial s : resources.getChildren()) { Float radius = s.getUserData("blockRadius"); if (radius != null && radius > 0f) { Vector3f p = s.getWorldTranslation(); out.add(new SandboxPhysics.CircleBlocker(p.x, p.z, radius)); } }
        for (BuildRecord record : built) if (record.type == BuildType.CAMPFIRE) out.add(new SandboxPhysics.CircleBlocker(record.x, record.z, 0.65f));
        return out;
    }

    private List<SandboxPhysics.BoxBlocker> boxBlockers() {
        List<SandboxPhysics.BoxBlocker> out = new ArrayList<>();
        for (BuildRecord record : built) if (record.type == BuildType.WALL) out.add(new SandboxPhysics.BoxBlocker(record.x, record.z, 1.4f, 0.16f, record.yaw));
        return out;
    }

    private List<BuildingPhysics.Floor> floorSupports() {
        List<BuildingPhysics.Floor> out = new ArrayList<>();
        for (BuildRecord record : built) if (record.type == BuildType.FLOOR) out.add(new BuildingPhysics.Floor(record.x, record.y, record.z, record.yaw));
        return out;
    }

    private void updateEnemies(float tpf) {
        Vector3f player = cam.getLocation();
        for (Spatial spatial : new ArrayList<>(enemies.getChildren())) {
            if (!(spatial instanceof Node enemy)) continue; EnemyType type = EnemyType.valueOf(enemy.getUserData("enemyType")); Vector3f p = enemy.getLocalTranslation(); Vector3f to = player.subtract(p); to.y = 0f;
            float distance = to.length(); float clock = enemy.<Float>getUserData("attackClock") - tpf;
            if (distance < type.notice && distance > 1.35f) {
                to.normalizeLocal(); float nx = p.x + to.x * type.speed * tpf, nz = p.z + to.z * type.speed * tpf;
                SandboxPhysics.HorizontalResult hit = SandboxPhysics.resolveHorizontalMixed(p.x, p.z, nx, nz, 0.36f, circleBlockers(), boxBlockers());
                float ground = terrain.sampleHeight(hit.x(), hit.z()); float support = BuildingPhysics.supportHeight(hit.x(), hit.z(), ground, p.y, floorSupports()); enemy.setLocalTranslation(hit.x(), support, hit.z());
            }
            if (distance <= 1.55f && clock <= 0f) { health = Math.max(0f, health - type.damage); clock = type.delay; announce(type.label + " hits you."); }
            enemy.setUserData("attackClock", clock);
        }
    }

    private void updateSurvival(float tpf) { hunger = Math.max(0f, hunger - 0.32f * tpf); if (hunger <= 0f) health = Math.max(0f, health - 1.1f * tpf); }

    private void updateDayNight(float tpf) {
        dayClock = (dayClock + tpf / DAY_SECONDS) % 1f; float angle = dayClock * FastMath.TWO_PI, elevation = FastMath.sin(angle); float daylight = Math.clamp(elevation * 0.75f + 0.38f, 0.08f, 1f);
        sun.setDirection(new Vector3f(FastMath.cos(angle), -Math.max(0.16f, elevation), -0.35f).normalizeLocal()); sun.setColor(new ColorRGBA(1f, 0.88f, 0.72f, 1f).mult(Math.max(0.12f, daylight)));
        ambient.setColor(new ColorRGBA(0.22f, 0.27f, 0.38f, 1f).mult(0.5f + daylight * 0.7f)); viewPort.setBackgroundColor(new ColorRGBA(0.04f + 0.25f * daylight, 0.06f + 0.34f * daylight, 0.10f + 0.43f * daylight, 1f));
    }

    private void craftHoe() { if (hoeCrafted) { announce("You already have a terrain hoe."); return; } if (wood < 5 || stone < 2) { announce("Hoe requires 5 wood + 2 stone."); return; } wood -= 5; stone -= 2; hoeCrafted = true; announce("Terrain hoe crafted."); }
    private void craftBlade() { if (bladeCrafted) { announce("You already have a blade."); return; } if (wood < 8 || stone < 4) { announce("Blade requires 8 wood + 4 stone."); return; } wood -= 8; stone -= 4; bladeCrafted = true; announce("Wanderer's Blade crafted."); }

    private void terraform() {
        if (!hoeCrafted) { announce("Craft the hoe first [1]."); return; } Vector3f target = terrainTarget(); if (target == null) { announce("Aim at reachable ground."); return; }
        float standing = terrain.sampleHeight(cam.getLocation().x, cam.getLocation().z); TerraformToolSystem.Result result = TerraformToolSystem.apply(terrain, terrainMode, target.x, target.z, standing, TerraformToolSystem.DEFAULT_RADIUS, stone, stamina);
        if (!result.applied()) { announce(result.message()); return; } stone -= result.stoneSpent(); stamina = Math.max(0f, stamina - result.staminaSpent()); updateTerrainPatch(terrain.consumeDirtyRegion()); snapNaturalObjects(target.x, target.z, TerraformToolSystem.DEFAULT_RADIUS + 1.5f); announce(result.message());
    }

    private Vector3f terrainTarget() {
        CollisionResults results = new CollisionResults(); terrainGeometry.collideWith(new Ray(cam.getLocation(), cam.getDirection()), results);
        if (results.size() > 0) { CollisionResult hit = results.getClosestCollision(); if (hit.getDistance() <= TERRAFORM_DISTANCE) return hit.getContactPoint().clone(); }
        Vector3f flat = cam.getDirection().clone(); flat.y = 0f; if (flat.lengthSquared() < 0.001f) return null; flat.normalizeLocal(); Vector3f p = cam.getLocation().add(flat.mult(TERRAFORM_DISTANCE * 0.72f)); p.y = terrain.sampleHeight(p.x, p.z); return p;
    }

    private void snapNaturalObjects(float x, float z, float radius) { float r2 = radius * radius; snapNode(resources, x, z, r2); snapNode(enemies, x, z, r2); }
    private void snapNode(Node parent, float x, float z, float r2) { for (Spatial s : parent.getChildren()) { Vector3f p = s.getLocalTranslation(); float dx = p.x - x, dz = p.z - z; if (dx * dx + dz * dz <= r2) s.setLocalTranslation(p.x, terrain.sampleHeight(p.x, p.z), p.z); } }

    private void interact() {
        Node n = raycastNode(resources, 4.8f); if (n == null) { announce("Nothing to gather in reach."); return; } String kind = n.getUserData("kind"); String id = n.getUserData("resourceId");
        if ("TREE".equals(kind)) wood += 3; else if ("ROCK".equals(kind)) stone += 2; else if ("BERRY".equals(kind)) berries += 2; else return;
        removedResources.add(id); n.removeFromParent(); announce("Gathered " + kind.toLowerCase(Locale.ROOT) + ".");
    }

    private void attack() {
        if (attackCooldown > 0f) return; attackCooldown = bladeCrafted ? 0.48f : 0.72f; Node enemy = raycastNode(enemies, 3.6f); if (enemy == null) return;
        float hp = enemy.<Float>getUserData("hp") - (bladeCrafted ? 22f : 7f); if (hp <= 0f) { enemy.removeFromParent(); kills++; stone += 1; announce("Enemy defeated."); } else enemy.setUserData("hp", hp);
    }

    private Node raycastNode(Node root, float range) {
        CollisionResults results = new CollisionResults(); root.collideWith(new Ray(cam.getLocation(), cam.getDirection()), results);
        for (CollisionResult hit : results) { if (hit.getDistance() > range) break; Spatial p = hit.getGeometry().getParent(); if (p instanceof Node n && n.getUserData("kind") != null) return n; } return null;
    }

    private void placeBuild() {
        Vector3f f = cam.getDirection().clone(); f.y = 0f; if (f.lengthSquared() < 0.001f) return; f.normalizeLocal(); Vector3f p = cam.getLocation().add(f.mult(3.2f));
        p.x = BuildingPhysics.snap(p.x); p.z = BuildingPhysics.snap(p.z); p.y = terrain.sampleHeight(p.x, p.z); float yaw = BuildingPhysics.snapYaw(buildYaw);
        if (!terrain.isBuildable(p.x, p.z, buildType.footprint, buildType.maxSlope, buildType.maxVariation)) { announce("Level the ground before building here."); return; }
        if (wood < buildType.wood || stone < buildType.stone) { announce(buildType.label + " needs " + buildType.wood + " wood + " + buildType.stone + " stone."); return; }
        if (buildType == BuildType.FLOOR && BuildingPhysics.floorsOverlap(p.x, p.z, floorSupports())) { announce("A floor already occupies this snap point."); return; }
        if (blockedPlacement(p.x, p.z, buildType)) { announce("Something blocks this build spot."); return; }
        wood -= buildType.wood; stone -= buildType.stone; BuildRecord record = new BuildRecord(buildType, p.x, p.y, p.z, yaw); built.add(record); createBuild(record); announce(buildType.label + " placed.");
    }

    private boolean blockedPlacement(float x, float z, BuildType type) {
        if (type == BuildType.FLOOR) return false;
        for (SandboxPhysics.CircleBlocker b : circleBlockers()) { float dx = x - b.x(), dz = z - b.z(); float radius = type == BuildType.CAMPFIRE ? 0.75f : 0.45f; if (dx * dx + dz * dz < (radius + b.radius()) * (radius + b.radius())) return true; }
        return false;
    }

    private void dismantle() {
        CollisionResults results = new CollisionResults(); structures.collideWith(new Ray(cam.getLocation(), cam.getDirection()), results);
        if (results.size() == 0 || results.getClosestCollision().getDistance() > 4.5f) { announce("Aim at a nearby build piece to dismantle it."); return; }
        Spatial parent = results.getClosestCollision().getGeometry().getParent(); if (!(parent instanceof Node node)) return;
        Integer buildIndex = node.getUserData("buildIndex"); if (buildIndex == null || buildIndex < 0 || buildIndex >= built.size()) return;
        BuildRecord record = built.get(buildIndex); wood += Math.max(1, record.type.wood / 2); stone += record.type.stone / 2; built.remove((int) buildIndex); rebuildStructures(); announce("Dismantled " + record.type.label + ". Some materials recovered.");
    }

    private void rebuildStructures() { structures.detachAllChildren(); restoreBuilds(); }
    private void restoreBuilds() { for (int i = 0; i < built.size(); i++) createBuild(built.get(i), i); }
    private void createBuild(BuildRecord record) { createBuild(record, built.indexOf(record)); }

    private void createBuild(BuildRecord record, int index) {
        Node n = new Node("build-" + record.type.name().toLowerCase(Locale.ROOT)); n.setUserData("buildType", record.type.name()); n.setUserData("buildIndex", index); n.setLocalTranslation(record.x, record.y, record.z); n.rotate(0f, record.yaw, 0f);
        if (record.type == BuildType.FLOOR) {
            Geometry g = new Geometry("floor", new Box(1.4f, 0.12f, 1.4f)); g.setMaterial(lit(new ColorRGBA(0.34f, 0.20f, 0.09f, 1f))); g.setLocalTranslation(0f, 0.12f, 0f); n.attachChild(g);
        } else if (record.type == BuildType.WALL) {
            Geometry g = new Geometry("wall", new Box(1.4f, 1.25f, 0.16f)); g.setMaterial(lit(new ColorRGBA(0.31f, 0.18f, 0.08f, 1f))); g.setLocalTranslation(0f, 1.25f, 0f); n.attachChild(g);
        } else {
            for (int i = 0; i < 7; i++) { float a = i * FastMath.TWO_PI / 7f; Geometry rock = new Geometry("camp-rock", new Sphere(6, 8, 0.17f)); rock.setMaterial(lit(new ColorRGBA(0.28f, 0.26f, 0.23f, 1f))); rock.setLocalTranslation(FastMath.cos(a) * 0.48f, 0.13f, FastMath.sin(a) * 0.48f); n.attachChild(rock); }
            Geometry ember = new Geometry("ember", new Sphere(8, 10, 0.28f)); ember.setMaterial(unshaded(new ColorRGBA(1f, 0.25f, 0.03f, 1f))); ember.setLocalTranslation(0f, 0.22f, 0f); n.attachChild(ember);
            PointLight light = new PointLight(); light.setColor(new ColorRGBA(1f, 0.35f, 0.08f, 1f)); light.setRadius(7f); light.setPosition(new Vector3f(record.x, record.y + 0.8f, record.z)); rootNode.addLight(light);
        }
        structures.attachChild(n);
    }

    private void eat() { if (berries <= 0) { announce("No berries."); return; } berries--; hunger = Math.min(100f, hunger + 26f); health = Math.min(100f, health + 12f); announce("Berry eaten."); }

    private void updateHud() {
        String time = dayClock > 0.52f && dayClock < 0.93f ? "Night" : "Day";
        hud.setText("HP " + Math.round(health) + "  ST " + Math.round(stamina) + "  Hunger " + Math.round(hunger) + "  " + time + "\nWood " + wood + " | Stone " + stone + " | Berries " + berries + " | Kills " + kills + " | " + (grounded ? "Grounded" : "Airborne") + " | Terrain:" + terrainMode.label() + " | Build:" + buildType.label + " @" + Math.round(buildYaw * FastMath.RAD_TO_DEG));
        objective.setText(objectiveText());
    }

    private String objectiveText() {
        if (!hoeCrafted) return "Goal: gather 5 wood + 2 stone and craft terrain hoe [1].";
        if (floorSupports().isEmpty()) return "Goal: level a site and place a floor [B/Q], then walk onto it.";
        if (!bladeCrafted) return "Goal: build a wall, then gather 8 wood + 4 stone for blade [2].";
        if (kills < 5) return "Goal: survive and defeat 5 enemies (" + kills + "/5).";
        return "Sandbox open: terraform, build, dismantle, explore and survive.";
    }

    private void setPlayerXZ(float x, float z) { x = Math.clamp(x, -WORLD_HALF + 2f, WORLD_HALF - 2f); z = Math.clamp(z, -WORLD_HALF + 2f, WORLD_HALF - 2f); cam.setLocation(new Vector3f(x, terrain.sampleHeight(x, z) + EYE_HEIGHT, z)); cam.lookAtDirection(new Vector3f(0.4f, -0.08f, -1f).normalizeLocal(), Vector3f.UNIT_Y); velocityY = 0f; grounded = true; }
    private void respawn() { health = 100f; stamina = 100f; hunger = 70f; setPlayerXZ(0f, 0f); announce("You wake at the Waystone."); }
    private void announce(String text) { if (message != null) message.setText(text); messageClock = 4.5f; }

    private void saveGame() {
        SaveData data = new SaveData(seed, cam.getLocation().x, cam.getLocation().z, health, stamina, hunger, dayClock, wood, stone, berries, kills, hoeCrafted, bladeCrafted, terrainMode, buildType, buildYaw, terrain.encodeDeltas(), new HashSet<>(removedResources), new ArrayList<>(built));
        try { data.save(); } catch (IOException ex) { System.err.println("Save failed: " + ex.getMessage()); }
    }

    private enum ResourceType { TREE, ROCK, BERRY }
    private enum EnemyType {
        GOBLIN("Goblin", 42f, 4.1f, 17f, 7f, 1.2f, new ColorRGBA(0.24f, 0.45f, 0.14f, 1f)), SKELETON("Graveborn", 68f, 3.4f, 20f, 11f, 1.45f, new ColorRGBA(0.72f, 0.72f, 0.66f, 1f));
        private final String label; private final float hp; private final float speed; private final float notice; private final float damage; private final float delay; private final ColorRGBA color;
        EnemyType(String label, float hp, float speed, float notice, float damage, float delay, ColorRGBA color) { this.label = label; this.hp = hp; this.speed = speed; this.notice = notice; this.damage = damage; this.delay = delay; this.color = color; }
    }
    private enum BuildType {
        FLOOR("Floor", 3, 0, 1.45f, 24f, 0.95f), WALL("Wall", 4, 0, 0.8f, 22f, 0.8f), CAMPFIRE("Campfire", 2, 3, 1.0f, 20f, 0.75f);
        private final String label; private final int wood; private final int stone; private final float footprint; private final float maxSlope; private final float maxVariation;
        BuildType(String label, int wood, int stone, float footprint, float maxSlope, float maxVariation) { this.label = label; this.wood = wood; this.stone = stone; this.footprint = footprint; this.maxSlope = maxSlope; this.maxVariation = maxVariation; }
        BuildType next() { BuildType[] all = values(); return all[(ordinal() + 1) % all.length]; }
    }

    private record BuildRecord(BuildType type, float x, float y, float z, float yaw) {
        String encode() { return type.name() + "," + x + "," + y + "," + z + "," + yaw; }
        static BuildRecord decode(String text) {
            String[] p = text.split(","); float yaw = p.length >= 5 ? Float.parseFloat(p[4]) : 0f;
            return new BuildRecord(BuildType.valueOf(p[0]), Float.parseFloat(p[1]), Float.parseFloat(p[2]), Float.parseFloat(p[3]), BuildingPhysics.snapYaw(yaw));
        }
    }

    private static final class SaveData {
        private static final Path PATH = Path.of(System.getProperty("user.home"), ".samaheim", "sandbox-save.properties");
        private final long seed; private final float x; private final float z; private final float health; private final float stamina; private final float hunger; private final float dayClock; private final int wood; private final int stone; private final int berries; private final int kills; private final boolean hoe; private final boolean blade; private final TerraformToolSystem.Mode mode; private final BuildType buildType; private final float buildYaw; private final String terrain; private final Set<String> removed; private final List<BuildRecord> builds;
        SaveData(long seed, float x, float z, float health, float stamina, float hunger, float dayClock, int wood, int stone, int berries, int kills, boolean hoe, boolean blade, TerraformToolSystem.Mode mode, BuildType buildType, float buildYaw, String terrain, Set<String> removed, List<BuildRecord> builds) {
            this.seed = seed; this.x = x; this.z = z; this.health = health; this.stamina = stamina; this.hunger = hunger; this.dayClock = dayClock; this.wood = wood; this.stone = stone; this.berries = berries; this.kills = kills; this.hoe = hoe; this.blade = blade; this.mode = mode; this.buildType = buildType; this.buildYaw = buildYaw; this.terrain = terrain; this.removed = removed; this.builds = builds;
        }
        static SaveData load() {
            if (!Files.isRegularFile(PATH)) return null; Properties p = new Properties();
            try (InputStream in = Files.newInputStream(PATH)) {
                p.load(in); Set<String> removed = new HashSet<>(); String removedText = p.getProperty("removed", ""); if (!removedText.isBlank()) for (String id : removedText.split(";")) if (!id.isBlank()) removed.add(id);
                List<BuildRecord> builds = new ArrayList<>(); String buildText = p.getProperty("builds", ""); if (!buildText.isBlank()) for (String entry : buildText.split(";")) if (!entry.isBlank()) builds.add(BuildRecord.decode(entry));
                return new SaveData(Long.parseLong(p.getProperty("seed")), Float.parseFloat(p.getProperty("x", "0")), Float.parseFloat(p.getProperty("z", "0")), Float.parseFloat(p.getProperty("health", "100")), Float.parseFloat(p.getProperty("stamina", "100")), Float.parseFloat(p.getProperty("hunger", "100")), Float.parseFloat(p.getProperty("day", "0.18")), Integer.parseInt(p.getProperty("wood", "0")), Integer.parseInt(p.getProperty("stone", "0")), Integer.parseInt(p.getProperty("berries", "0")), Integer.parseInt(p.getProperty("kills", "0")), Boolean.parseBoolean(p.getProperty("hoe", "false")), Boolean.parseBoolean(p.getProperty("blade", "false")), TerraformToolSystem.Mode.valueOf(p.getProperty("mode", "LEVEL")), BuildType.valueOf(p.getProperty("buildType", "FLOOR")), BuildingPhysics.snapYaw(Float.parseFloat(p.getProperty("buildYaw", "0"))), p.getProperty("terrain", ""), removed, builds);
            } catch (RuntimeException | IOException ex) { System.err.println("Ignoring invalid sandbox save: " + ex.getMessage()); return null; }
        }
        void save() throws IOException {
            Files.createDirectories(PATH.getParent()); Properties p = new Properties(); p.setProperty("seed", Long.toString(seed)); p.setProperty("x", Float.toString(x)); p.setProperty("z", Float.toString(z)); p.setProperty("health", Float.toString(health)); p.setProperty("stamina", Float.toString(stamina)); p.setProperty("hunger", Float.toString(hunger)); p.setProperty("day", Float.toString(dayClock)); p.setProperty("wood", Integer.toString(wood)); p.setProperty("stone", Integer.toString(stone)); p.setProperty("berries", Integer.toString(berries)); p.setProperty("kills", Integer.toString(kills)); p.setProperty("hoe", Boolean.toString(hoe)); p.setProperty("blade", Boolean.toString(blade)); p.setProperty("mode", mode.name()); p.setProperty("buildType", buildType.name()); p.setProperty("buildYaw", Float.toString(buildYaw)); p.setProperty("terrain", terrain); p.setProperty("removed", String.join(";", removed));
            StringBuilder b = new StringBuilder(); for (BuildRecord record : builds) { if (b.length() > 0) b.append(';'); b.append(record.encode()); } p.setProperty("builds", b.toString()); try (OutputStream out = Files.newOutputStream(PATH)) { p.store(out, "Samaheim builder sandbox"); }
        }
    }
}
