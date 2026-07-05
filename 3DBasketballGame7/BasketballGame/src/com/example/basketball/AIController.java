package com.example.basketball;

import com.jme3.app.state.BaseAppState;
import com.jme3.math.FastMath;
import com.jme3.math.Vector3f;

public class AIController extends BaseAppState {

    private final PlayerController pawn;
    private final PlayerController opponent;
    private final GameWorld world;
    private LevelManager.Level level;

    private Vector3f targetPos = new Vector3f(0, 0, 6f);
    private float thinkTimer = 0f;

    public AIController(PlayerController pawn, PlayerController opponent, GameWorld world, LevelManager.Level level) {
        this.pawn = pawn;
        this.opponent = opponent;
        this.world = world;
        this.level = level;
    }

    public PlayerController getPawn() { return pawn; }

    public void setDifficulty(LevelManager.Level lvl) { this.level = lvl; }

    @Override
    protected void initialize(com.jme3.app.Application app) {}

    @Override
    public void update(float tpf) {
        thinkTimer -= tpf;
        if (thinkTimer <= 0) {
            thinkTimer = FastMath.interpolateLinear(level.aiReaction, 0.15f, 0.5f);
            chooseBehavior();
        }

        Vector3f pos = pawn.getNode().getWorldTranslation();
        Vector3f dir = targetPos.subtract(pos);
        dir.y = 0;
        if (dir.length() > 0.1f) {
            dir.normalizeLocal();
            pawn.getNode().move(dir.mult(3.8f * tpf));
            pawn.getNode().lookAt(pos.add(dir), Vector3f.UNIT_Y);
        }

        // Pick up loose ball
        if (world.getBall() != null && world.getBall().getOwner() == null) {
            float d = world.getBall().getPosition().distance(pawn.getNode().getWorldTranslation());
            if (d < 1.2f) {
                pawn.takePossession(world.getBall());
            }
        }

        // Attempt shot if AI has the ball
        if (world.getBall() != null && world.getBall().getOwner() == pawn) {
            Vector3f me = pawn.getNode().getWorldTranslation().add(0, 1.6f, 0);
            boolean attackNorth = me.z > 0;
            Vector3f hoop = new Vector3f(0, 3.05f, attackNorth ? -8.4f : 8.4f);
            float dist = me.distance(new Vector3f(hoop.x, me.y, hoop.z));
            if (dist < 7.5f || FastMath.nextRandomFloat() < 0.03f + 0.2f * level.aiAggression) {
                float spread = FastMath.interpolateLinear(1f - level.aiShotAccuracy, 0.9f, 0.12f);
                Vector3f noisyHoop = hoop.add(
                        (FastMath.nextRandomFloat() - 0.5f) * spread,
                        (FastMath.nextRandomFloat() - 0.5f) * spread * 0.6f,
                        (FastMath.nextRandomFloat() - 0.5f) * spread
                );
                float angleDeg = 51f + (FastMath.nextRandomFloat() - 0.5f) * 6f;
                Vector3f impulse = ShotUtil.computeShotImpulse(
                        me, noisyHoop, dist, angleDeg, world.getBall().getMass(), 9.81f, 0.85f);
                world.getBall().shoot(impulse);
            }
        }

        // FIX: update pawn controller so dribble animation and ball-follow logic run
        pawn.update(tpf);
    }

    private void chooseBehavior() {
        Vector3f me = pawn.getNode().getWorldTranslation();
        boolean onDefense = world.getBall() != null && world.getBall().getOwner() != pawn;
        if (onDefense && world.getBall().getOwner() == opponent) {
            boolean defendNorth = opponent.getNode().getWorldTranslation().z < 0;
            Vector3f ownHoop = defendNorth
                    ? new Vector3f(0, 3.05f, -8.4f)
                    : new Vector3f(0, 3.05f, 8.4f);
            Vector3f toHoop = ownHoop.subtract(opponent.getNode().getWorldTranslation()).normalizeLocal();
            targetPos = opponent.getNode().getWorldTranslation().add(
                    toHoop.mult(FastMath.interpolateLinear(level.aiAggression, 0.8f, 2.2f)));
        } else {
            float x = (FastMath.nextRandomFloat() - 0.5f) * 5.5f;
            float z = me.z > 0
                    ?  2f + FastMath.nextRandomFloat() * 3f
                    : -2f - FastMath.nextRandomFloat() * 3f;
            targetPos = new Vector3f(x, 0, z);
        }
    }

    @Override protected void cleanup(com.jme3.app.Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}
}
