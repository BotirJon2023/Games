package com.example.basket3d;

import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;

public class AIController extends BaseAppState {

    private final Player ai;
    private final BallControl ball;
    private final Hoop leftHoop, rightHoop;
    private final LevelManager level;
    private final GameState game;

    private float thinkTimer = 0f;
    private final Vector3f target = new Vector3f();

    private enum State { IDLE, MOVE_TO_BALL, DRIVE, SHOOT, DEFEND }
    private State state = State.IDLE;

    public AIController(Player ai, BallControl ball, Hoop left, Hoop right, LevelManager level, GameState game) {
        this.ai = ai;
        this.ball = ball;
        this.leftHoop  = left;
        this.rightHoop = right;
        this.level = level;
        this.game = game;
    }

    @Override protected void initialize(com.jme3.app.Application app) {}
    @Override protected void cleanup(com.jme3.app.Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    @Override
    public void update(float tpf) {
        thinkTimer -= tpf;
        if (thinkTimer <= 0f) {
            decide();
            thinkTimer = level.getReactionTime();
        }
        act(tpf);
        ai.update(tpf);
    }

    private void decide() {
        Vector3f aiPos   = ai.getNode().getWorldTranslation();
        Vector3f ballPos = ball.getNode().getWorldTranslation();
        boolean holding  = ball.isHeld();
        boolean closeToBall = aiPos.distance(ballPos) < 1.2f;

        if (!holding) {
            state = closeToBall ? State.DRIVE : State.MOVE_TO_BALL;
            target.set(ballPos);
        } else {
            float distToHoop = aiPos.distance(game.getOpponentHoopPosOf(ai));
            if (distToHoop < 4.5f && Math.random() < level.getShootBias()) {
                state = State.SHOOT;
            } else {
                state = State.DRIVE;
                target.set(game.getOpponentHoopPosOf(ai));
            }
        }
    }

    private void act(float tpf) {
        switch (state) {
            case MOVE_TO_BALL:
            case DRIVE:
                // Fix: Vector3f.setY() returns void — zero Y via direct field assignment
                Vector3f diff = target.subtract(ai.getNode().getWorldTranslation());
                diff.y = 0;
                ai.setMoveDir(diff.normalizeLocal());
                break;
            case SHOOT:
                shoot();
                state = State.DEFEND;
                break;
            case DEFEND:
            case IDLE:
                ai.setMoveDir(Vector3f.ZERO);
                break;
        }
    }

    private void shoot() {
        Vector3f hoop = game.getOpponentHoopPosOf(ai);
        float power = 2.7f + level.getAimJitter() * (float) (Math.random() - 0.5) * 0.3f;
        float arc   = 0.55f + level.getAimJitter() * (float) (Math.random() - 0.5) * 0.1f;
        ai.playShoot();
        ball.shootToward(hoop, power, arc);
        ai.endShoot();
    }
}
