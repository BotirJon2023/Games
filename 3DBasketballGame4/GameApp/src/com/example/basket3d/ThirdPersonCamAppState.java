package com.example.basket3d;

import com.jme3.app.Application;
import com.jme3.app.state.BaseAppState;
import com.jme3.math.Vector3f;
import com.jme3.renderer.Camera;

public class ThirdPersonCamAppState extends BaseAppState {

    private final Player player;
    private final Camera cam;

    public ThirdPersonCamAppState(Player player, Camera cam) {
        this.player = player;
        this.cam    = cam;
    }

    @Override protected void initialize(Application app) {}
    @Override protected void cleanup(Application app) {}
    @Override protected void onEnable() {}
    @Override protected void onDisable() {}

    @Override
    public void update(float tpf) {
        Vector3f pos  = player.getNode().getWorldTranslation();
        Vector3f back = player.getForward().negate().normalizeLocal();
        Vector3f camPos = pos.add(back.mult(4f)).add(0, 2f, 0);
        cam.setLocation(camPos);
        cam.lookAt(pos.add(0, 1.2f, 0), Vector3f.UNIT_Y);
    }
}
