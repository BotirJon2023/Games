package org.example;

import com.sun.j3d.utils.geometry.Box;
import com.sun.j3d.utils.universe.SimpleUniverse;
import javax.media.j3d.*;
import javax.vecmath.*;
import java.awt.*;
import com.sun.j3d.utils.geometry.Sphere;

public class WalkingPerson3D extends JFrame {

    public WalkingPerson3D() {
        // Set up the window
        setTitle("3D Walking Person");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Create a Canvas3D for rendering
        Canvas3D canvas = new Canvas3D(SimpleUniverse.getPreferredConfiguration());
        getContentPane().add(canvas);

        // Create a simple universe
        SimpleUniverse universe = new SimpleUniverse(canvas);
        BranchGroup scene = createSceneGraph();
        universe.getViewingPlatform().setNominalViewingTransform();
        universe.addBranchGraph(scene);
    }

    private BranchGroup createSceneGraph() {
        // Root of the scene graph
        BranchGroup objRoot = new BranchGroup();

        // Transform group for the person
        TransformGroup personTransform = new TransformGroup();
        personTransform.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        objRoot.addChild(personTransform);

        // Create body parts (simple shapes)
        Appearance appearance = new Appearance();
        ColoringAttributes ca = new ColoringAttributes(new Color3f(Color.BLUE), ColoringAttributes.SHADE_GOURAUD);
        appearance.setColoringAttributes(ca);

        // Head
        Sphere head = new Sphere(0.2f, appearance);
        Transform3D headTransform = new Transform3D();
        headTransform.setTranslation(new Vector3f(0f, 1.5f, 0f));
        TransformGroup headGroup = new TransformGroup(headTransform);
        headGroup.addChild(head);
        personTransform.addChild(headGroup);

        // Torso
        Box torso = new Box(0.3f, 0.5f, 0.2f, appearance);
        Transform3D torsoTransform = new Transform3D();
        torsoTransform.setTranslation(new Vector3f(0f, 1.0f, 0f));
        TransformGroup torsoGroup = new TransformGroup(torsoTransform);
        torsoGroup.addChild(torso);
        personTransform.addChild(torsoGroup);

        // Left Leg
        Box leftLeg = new Box(0.1f, 0.5f, 0.1f, appearance);
        Transform3D leftLegTransform = new Transform3D();
        leftLegTransform.setTranslation(new Vector3f(-0.15f, 0.5f, 0f));
        TransformGroup leftLegGroup = new TransformGroup(leftLegTransform);
        leftLegGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        leftLegGroup.addChild(leftLeg);
        personTransform.addChild(leftLegGroup);

        // Right Leg
        Box rightLeg = new Box(0.1f, 0.5f, 0.1f, appearance);
        Transform3D rightLegTransform = new Transform3D();
        rightLegTransform.setTranslation(new Vector3f(0.15f, 0.5f, 0f));
        TransformGroup rightLegGroup = new TransformGroup(rightLegTransform);
        rightLegGroup.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
        rightLegGroup.addChild(rightLeg);
        personTransform.addChild(rightLegGroup);

        // Walking animation (simple leg swing)
        Alpha alpha = new Alpha(-1, 2000); // Infinite loop, 2 seconds per cycle
        RotationInterpolator leftLegRot = new RotationInterpolator(alpha, leftLegGroup, new Transform3D(),
                0.0f, (float) Math.PI / 4); // Swing 45 degrees
        leftLegRot.setSchedulingBounds(new BoundingSphere(new Point3d(0, 0, 0), 1000));
        personTransform.addChild(leftLegRot);

        RotationInterpolator rightLegRot = new RotationInterpolator(alpha, rightLegGroup, new Transform3D(),
                0.0f, -(float) Math.PI / 4); // Opposite swing
        rightLegRot.setSchedulingBounds(new BoundingSphere(new Point3d(0, 0, 0), 1000));
        personTransform.addChild(rightLegRot);

        // Add a light source
        DirectionalLight light = new DirectionalLight(new Color3f(Color.WHITE), new Vector3f(0f, -1f, -1f));
        light.setInfluencingBounds(new BoundingSphere(new Point3d(0, 0, 0), 1000));
        objRoot.addChild(light);

        return objRoot;
    }

    public static void main(String[] args) {
        WalkingPerson3D frame = new WalkingPerson3D();
        frame.setVisible(true);
    }
}