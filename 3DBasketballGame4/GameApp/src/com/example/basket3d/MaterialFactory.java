package com.example.basket3d;

import com.jme3.asset.AssetManager;
import com.jme3.material.Material;
import com.jme3.material.Materials;
import com.jme3.math.ColorRGBA;

public class MaterialFactory {

    public static Material basketball(AssetManager am) {
        Material m = new Material(am, Materials.LIGHTING);
        m.setBoolean("UseMaterialColors", true);
        m.setColor("Diffuse",  new ColorRGBA(0.9f, 0.45f, 0.2f, 1f));
        m.setColor("Ambient",  new ColorRGBA(0.7f, 0.35f, 0.15f, 1f));
        m.setColor("Specular", ColorRGBA.White.mult(0.05f));
        m.setFloat("Shininess", 2f);
        return m;
    }
}
