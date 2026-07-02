package com.example.basket3d;

import com.jme3.app.Application;
import com.jme3.app.SimpleApplication;
import com.jme3.app.state.BaseAppState;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;

public class GameHudAppState extends BaseAppState {

    private final GameState game;
    private BitmapText scoreText;

    public GameHudAppState(GameState game) { this.game = game; }

    @Override
    protected void initialize(Application app) {
        BitmapText t = new BitmapText(app.getAssetManager().loadFont("Interface/Fonts/Default.fnt"));
        t.setSize(28);
        t.setColor(ColorRGBA.White);
        t.setText("0 - 0");
        t.setLocalTranslation(20, app.getCamera().getHeight() - 20, 0);
        scoreText = t;
        // Fix: getGuiNode() is on SimpleApplication, not Application
        ((SimpleApplication) app).getGuiNode().attachChild(t);
    }

    @Override protected void cleanup(Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    @Override
    public void update(float tpf) {
        scoreText.setText(game.getLeftScore() + " - " + game.getRightScore());
    }
}
