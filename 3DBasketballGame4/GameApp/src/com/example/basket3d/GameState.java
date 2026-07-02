package com.example.basket3d;

import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;

public class GameState extends BaseAppState {

    private int leftScore  = 0;
    private int rightScore = 0;
    private final Vector3f leftHoopPos  = new Vector3f(0, 3.05f, -7f);
    private final Vector3f rightHoopPos = new Vector3f(0, 3.05f,  7f);

    @Override protected void initialize(com.jme3.app.Application app) {}
    @Override protected void cleanup(com.jme3.app.Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    public void scoreLeft()  { leftScore++; }
    public void scoreRight() { rightScore++; }

    public int getLeftScore()  { return leftScore; }
    public int getRightScore() { return rightScore; }

    public Vector3f getLeftHoopPos()  { return leftHoopPos; }
    public Vector3f getRightHoopPos() { return rightHoopPos; }

    public boolean isLeftTeam(Player p) {
        return "P1".equals(p.getNode().getName());
    }

    public Vector3f getOpponentHoopPosOf(Player p) {
        return isLeftTeam(p) ? rightHoopPos : leftHoopPos;
    }
}
