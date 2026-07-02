package com.example.basket3d;

import com.jme3.app.state.BaseAppState;
import com.jme3.input.InputManager;
import com.jme3.input.KeyInput;
import com.jme3.input.controls.ActionListener;
import com.jme3.input.controls.KeyTrigger;
import com.jme3.math.Vector3f;

// InputManager has no isKeyDown() — we track pressed/released state ourselves via onAction
public class TwoPlayerInput extends BaseAppState implements ActionListener {

    private final InputManager input;
    private final Player p1;
    private final Player p2;
    private final BallControl ball;
    private final GameState game;

    private final Vector3f p1Dir = new Vector3f();
    private final Vector3f p2Dir = new Vector3f();

    private boolean p1Up, p1Down, p1Left, p1Right;
    private boolean p2Up, p2Down, p2Left, p2Right;

    public TwoPlayerInput(InputManager input, Player p1, Player p2, BallControl ball, GameState game) {
        this.input = input;
        this.p1 = p1;
        this.p2 = p2;
        this.ball = ball;
        this.game = game;
    }

    @Override
    protected void initialize(com.jme3.app.Application app) {
        input.setCursorVisible(false);
        addMappings();
    }
    @Override protected void cleanup(com.jme3.app.Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    private void addMappings() {
        input.addMapping("P1_Up",    new KeyTrigger(KeyInput.KEY_W));
        input.addMapping("P1_Down",  new KeyTrigger(KeyInput.KEY_S));
        input.addMapping("P1_Left",  new KeyTrigger(KeyInput.KEY_A));
        input.addMapping("P1_Right", new KeyTrigger(KeyInput.KEY_D));
        input.addMapping("P1_Shoot", new KeyTrigger(KeyInput.KEY_SPACE));
        input.addMapping("P1_Pass",  new KeyTrigger(KeyInput.KEY_Q));

        if (p2 != null) {
            input.addMapping("P2_Up",    new KeyTrigger(KeyInput.KEY_UP));
            input.addMapping("P2_Down",  new KeyTrigger(KeyInput.KEY_DOWN));
            input.addMapping("P2_Left",  new KeyTrigger(KeyInput.KEY_LEFT));
            input.addMapping("P2_Right", new KeyTrigger(KeyInput.KEY_RIGHT));
            input.addMapping("P2_Shoot", new KeyTrigger(KeyInput.KEY_RCONTROL));
            input.addMapping("P2_Pass",  new KeyTrigger(KeyInput.KEY_RSHIFT));
        }

        input.addListener(this, "P1_Up", "P1_Down", "P1_Left", "P1_Right", "P1_Shoot", "P1_Pass");
        if (p2 != null)
            input.addListener(this, "P2_Up", "P2_Down", "P2_Left", "P2_Right", "P2_Shoot", "P2_Pass");
    }

    @Override
    public void onAction(String name, boolean isPressed, float tpf) {
        switch (name) {
            case "P1_Up":    p1Up    = isPressed; break;
            case "P1_Down":  p1Down  = isPressed; break;
            case "P1_Left":  p1Left  = isPressed; break;
            case "P1_Right": p1Right = isPressed; break;
            case "P1_Shoot": if (isPressed) doShoot(p1); break;
            case "P1_Pass":  if (isPressed) ball.drop(); break;
            case "P2_Up":    p2Up    = isPressed; break;
            case "P2_Down":  p2Down  = isPressed; break;
            case "P2_Left":  p2Left  = isPressed; break;
            case "P2_Right": p2Right = isPressed; break;
            case "P2_Shoot": if (isPressed && p2 != null) doShoot(p2); break;
            case "P2_Pass":  if (isPressed) ball.drop(); break;
        }
    }

    @Override
    public void update(float tpf) {
        p1Dir.set(0, 0, 0);
        if (p1Up)    p1Dir.addLocal(0, 0, -1);
        if (p1Down)  p1Dir.addLocal(0, 0,  1);
        if (p1Left)  p1Dir.addLocal(-1, 0, 0);
        if (p1Right) p1Dir.addLocal( 1, 0, 0);
        p1.setMoveDir(p1Dir.normalizeLocal());

        if (p2 != null) {
            p2Dir.set(0, 0, 0);
            if (p2Up)    p2Dir.addLocal(0, 0, -1);
            if (p2Down)  p2Dir.addLocal(0, 0,  1);
            if (p2Left)  p2Dir.addLocal(-1, 0, 0);
            if (p2Right) p2Dir.addLocal( 1, 0, 0);
            p2.setMoveDir(p2Dir.normalizeLocal());
        }

        p1.update(tpf);
        if (p2 != null) p2.update(tpf);

        if (ball.isHeld()) {
            if (p2 != null) ball.updateFollow(p2);
            ball.updateFollow(p1);
        }
    }

    private void doShoot(Player p) {
        Vector3f targetHoop = game.isLeftTeam(p) ? game.getRightHoopPos() : game.getLeftHoopPos();
        p.playShoot();
        ball.shootToward(targetHoop, 2.9f, 0.55f);
        p.endShoot();
    }
}
