package com.example.basket3d;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.Spatial;
import com.jme3.scene.shape.Cylinder;

public class GeometryBuilder {

    public static Spatial capsule(AssetManager am, String name) {
        // JME3 has no Capsule mesh shape — a Cylinder is the closest substitute
        Cylinder c = new Cylinder(8, 8, 0.35f, 1.8f, true);
        Geometry g = new Geometry(name + "_Capsule", c);
        Material m = new Material(am, Materials.LIGHTING);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse", ColorRGBA.randomColor());
        m.setColor("Ambient", ColorRGBA.White.mult(0.1f));
        g.setMaterial(m);
        Node n = new Node(name + "_Node");
        n.attachChild(g);
        g.setLocalTranslation(0, 0.8f, 0);
        return n;
    }
}
