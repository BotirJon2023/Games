package com.example.basket3d;

import com.jme3.asset.AssetManager;
import com.jme3.bullet.collision.shapes.BoxCollisionShape;
import com.jme3.bullet.collision.shapes.CollisionShape;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.math.Vector3f;
import com.jme3.scene.Geometry;
import com.jme3.scene.shape.Box;

public class DemoGeometries {

    public static Geometry makeCourtFloor(AssetManager am) {
        Box b = new Box(15f, 0.1f, 8f);
        Geometry g = new Geometry("Court", b);
        Material m = new Material(am, Materials.LIGHTING);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", new ColorRGBA(0.7f, 0.5f, 0.2f, 1f));
        m.setColor("Ambient", new ColorRGBA(0.6f, 0.4f, 0.2f, 1f));
        g.setMaterial(m);
        g.setLocalTranslation(0, -0.1f, 0);
        return g;
    }

    public static CollisionShape makeCourtCollision() {
        return new BoxCollisionShape(new Vector3f(15f, 0.1f, 8f));
    }
}
