package com.samaheim.ui;

import com.jme3.asset.AssetManager;
import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.material.Material;
import com.jme3.material.RenderState;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Quad;
import com.jme3.renderer.queue.RenderQueue;

/** Compact graphical HUD: status bars, objective card and contextual tool card. */
public final class SamaheimHud {
    private static final float BAR_WIDTH = 210f;
    private final Geometry healthFill;
    private final Geometry staminaFill;
    private final Geometry hungerFill;
    private final BitmapText healthLabel;
    private final BitmapText staminaLabel;
    private final BitmapText hungerLabel;
    private final BitmapText objective;
    private final BitmapText toolTitle;
    private final BitmapText toolDetail;
    private final BitmapText resources;
    private final BitmapText message;
    private final BitmapText crosshair;

    public SamaheimHud(AssetManager assets, Node gui, int width, int height) {
        BitmapFont font = assets.loadFont("Interface/Fonts/Default.fnt");

        panel(assets, gui, 18f, height - 142f, 276f, 122f, new ColorRGBA(0.025f, 0.035f, 0.045f, 0.86f));
        panel(assets, gui, width - 350f, height - 152f, 332f, 132f, new ColorRGBA(0.025f, 0.035f, 0.045f, 0.86f));
        panel(assets, gui, width * 0.5f - 240f, 20f, 480f, 92f, new ColorRGBA(0.02f, 0.025f, 0.035f, 0.90f));

        healthFill = bar(assets, gui, 46f, height - 58f, new ColorRGBA(0.76f, 0.16f, 0.12f, 1f));
        staminaFill = bar(assets, gui, 46f, height - 91f, new ColorRGBA(0.20f, 0.68f, 0.26f, 1f));
        hungerFill = bar(assets, gui, 46f, height - 124f, new ColorRGBA(0.86f, 0.60f, 0.16f, 1f));
        healthLabel = text(font, gui, 22f, 48f, height - 39f);
        staminaLabel = text(font, gui, 22f, 48f, height - 72f);
        hungerLabel = text(font, gui, 22f, 48f, height - 105f);

        objective = text(font, gui, 20f, width - 330f, height - 48f);
        objective.setColor(new ColorRGBA(1f, 0.88f, 0.56f, 1f));
        resources = text(font, gui, 17f, width - 330f, height - 118f);
        resources.setColor(new ColorRGBA(0.78f, 0.82f, 0.86f, 1f));

        toolTitle = text(font, gui, 23f, width * 0.5f - 218f, 92f);
        toolTitle.setColor(new ColorRGBA(0.64f, 0.88f, 1f, 1f));
        toolDetail = text(font, gui, 17f, width * 0.5f - 218f, 61f);
        toolDetail.setColor(new ColorRGBA(0.78f, 0.82f, 0.86f, 1f));
        message = text(font, gui, 18f, width * 0.5f - 300f, 138f);
        crosshair = text(font, gui, 25f, width * 0.5f - 6f, height * 0.5f + 9f);
        crosshair.setText("+");
        crosshair.setColor(new ColorRGBA(0.95f, 0.98f, 1f, 0.92f));
    }

    public void update(float health, float stamina, float hunger, String objectiveText, String resourceText,
                       String tool, String detail) {
        scaleBar(healthFill, health);
        scaleBar(staminaFill, stamina);
        scaleBar(hungerFill, hunger);
        healthLabel.setText("HEALTH  " + Math.round(health));
        staminaLabel.setText("STAMINA  " + Math.round(stamina));
        hungerLabel.setText("HUNGER  " + Math.round(hunger));
        objective.setText("CURRENT GOAL\n" + objectiveText);
        resources.setText(resourceText);
        toolTitle.setText(tool);
        toolDetail.setText(detail);
    }

    public void setMessage(String text) { message.setText(text == null ? "" : text); }
    public void setCrosshairActive(boolean active) { crosshair.setColor(active ? new ColorRGBA(0.72f, 1f, 0.82f, 1f) : new ColorRGBA(0.95f, 0.98f, 1f, 0.92f)); }

    private static Geometry bar(AssetManager assets, Node gui, float x, float y, ColorRGBA color) {
        panel(assets, gui, x, y, BAR_WIDTH, 9f, new ColorRGBA(0.12f, 0.13f, 0.15f, 0.9f));
        Geometry fill = panel(assets, gui, x, y, BAR_WIDTH, 9f, color);
        return fill;
    }

    private static void scaleBar(Geometry bar, float value) {
        bar.setLocalScale(Math.max(0f, Math.min(1f, value / 100f)), 1f, 1f);
    }

    private static Geometry panel(AssetManager assets, Node gui, float x, float y, float width, float height, ColorRGBA color) {
        Geometry geometry = new Geometry("hud-panel", new Quad(width, height));
        Material material = new Material(assets, "Common/MatDefs/Misc/Unshaded.j3md");
        material.setColor("Color", color);
        material.getAdditionalRenderState().setBlendMode(RenderState.BlendMode.Alpha);
        geometry.setMaterial(material);
        geometry.setQueueBucket(RenderQueue.Bucket.Gui);
        geometry.setLocalTranslation(x, y, 0f);
        gui.attachChild(geometry);
        return geometry;
    }

    private static BitmapText text(BitmapFont font, Node gui, float size, float x, float y) {
        BitmapText text = new BitmapText(font);
        text.setSize(size);
        text.setLocalTranslation(x, y, 2f);
        gui.attachChild(text);
        return text;
    }
}
