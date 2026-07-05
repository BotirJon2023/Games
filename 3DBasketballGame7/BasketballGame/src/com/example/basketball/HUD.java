package com.example.basketball;

import com.jme3.font.BitmapFont;
import com.jme3.font.BitmapText;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Node;

public class HUD {

    private final BasketballGame app;
    private final Node guiNode;
    private BitmapText score, timer, title, menu;

    public HUD(BasketballGame app, Node guiNode, BitmapFont font) {
        this.app = app;
        this.guiNode = guiNode;

        score = new BitmapText(font, false);
        score.setSize(28);
        score.setColor(ColorRGBA.White);
        score.setLocalTranslation(20, app.getCamera().getHeight() - 20, 0);
        guiNode.attachChild(score);

        timer = new BitmapText(font, false);
        timer.setSize(28);
        timer.setColor(ColorRGBA.Yellow);
        timer.setLocalTranslation(app.getCamera().getWidth() - 160, app.getCamera().getHeight() - 20, 0);
        guiNode.attachChild(timer);

        title = new BitmapText(font, false);
        title.setSize(42);
        title.setColor(ColorRGBA.Cyan);
        title.setLocalTranslation(app.getCamera().getWidth() / 2f - 180, app.getCamera().getHeight() / 2f + 80, 0);

        menu = new BitmapText(font, false);
        menu.setSize(24);
        menu.setColor(ColorRGBA.LightGray);
        menu.setLocalTranslation(app.getCamera().getWidth() / 2f - 280, app.getCamera().getHeight() / 2f, 0);
    }

    public void update(int s1, int s2, float timeRemaining) {
        score.setText("P1 " + s1 + "   |   " + s2 + " P2");
        int t = Math.max(0, Math.round(timeRemaining));
        int m = t / 60;
        int s = t % 60;
        timer.setText(String.format("%d:%02d", m, s));
    }

    public void showMainMenu(GameConfig cfg, LevelManager levels) {
        title.setText("3D Basketball");
        StringBuilder sb = new StringBuilder();
        sb.append("[Enter] Start\n");
        sb.append("Current mode: ").append(cfg.mode).append("\n");
        sb.append("Level: ").append(levels.get(cfg.levelIndex).name).append("\n");
        sb.append("Controls: P1 WASD + Space(Shoot) + E(Action) + Shift(Sprint)\n");
        sb.append("          P2 Arrows + RightCtrl(Shoot) + AltGr(Action) + RightShift\n");
        menu.setText(sb.toString());
        if (!guiNode.hasChild(title)) guiNode.attachChild(title);
        if (!guiNode.hasChild(menu))  guiNode.attachChild(menu);
    }

    public void hideMenu() {
        guiNode.detachChild(title);
        guiNode.detachChild(menu);
    }

    public void showPauseMenu() {
        title.setText("Paused");
        menu.setText("[Enter] Resume");
        if (!guiNode.hasChild(title)) guiNode.attachChild(title);
        if (!guiNode.hasChild(menu))  guiNode.attachChild(menu);
    }
}
