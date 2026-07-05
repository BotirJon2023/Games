package com.example.basketball;

import com.jme3.asset.AssetManager;
import com.jme3.math.Vector3f;

public class PlayerFactory {

    public static PlayerController createHumanPlayer(AssetManager am, GameWorld world, String name, Vector3f spawn) {
        PlayerController p = new PlayerController(name, world);
        p.loadModel(am, "Models/Player.glb");
        p.getNode().setLocalTranslation(spawn);
        p.setHuman(true);
        return p;
    }

    public static PlayerController createAIPawn(AssetManager am, GameWorld world, String name, Vector3f spawn) {
        PlayerController p = new PlayerController(name, world);
        p.loadModel(am, "Models/Player.glb");
        p.getNode().setLocalTranslation(spawn);
        p.setHuman(false);
        return p;
    }
}
