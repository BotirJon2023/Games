package org.example;

import javafx.application.Application;
import javafx.scene.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Box;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.scene.layout.StackPane;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.input.MouseEvent;

public class Chess3D extends Application {
    private static final int BOARD_SIZE = 8;
    private static final double TILE_SIZE = 50;
    private static final double BOARD_HEIGHT = 10;

    private double anchorX, anchorY;
    private double anchorAngleX = 0;
    private double anchorAngleY = 0;
    private final Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
    private final Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);

    @Override
    public void start(Stage primaryStage) {
        Group root = new Group();
        Group board = createBoard();
        root.getChildren().add(board);

        // Add rotation transforms
        board.getTransforms().addAll(rotateX, rotateY);

        // Set up the scene
        Scene scene = new Scene(root, 800, 600, true);
        scene.setFill(Color.LIGHTGRAY);

        // Add a camera for 3D effect
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-1000);
        camera.setNearClip(0.1);
        camera.setFarClip(2000.0);
        scene.setCamera(camera);

        // Handle mouse events for rotation
        scene.setOnMousePressed(event -> {
            anchorX = event.getSceneX();
            anchorY = event.getSceneY();
            anchorAngleX = rotateX.getAngle();
            anchorAngleY = rotateY.getAngle();
        });

        scene.setOnMouseDragged(event -> {
            rotateX.setAngle(anchorAngleX - (anchorY - event.getSceneY()));
            rotateY.setAngle(anchorAngleY + (anchorX - event.getSceneX()));
        });

        primaryStage.setTitle("3D Chessboard");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private Group createBoard() {
        Group board = new Group();

        for (int x = 0; x < BOARD_SIZE; x++) {
            for (int z = 0; z < BOARD_SIZE; z++) {
                Box tile = new Box(TILE_SIZE, BOARD_HEIGHT, TILE_SIZE);
                tile.setTranslateX((x - BOARD_SIZE / 2.0) * TILE_SIZE);
                tile.setTranslateZ((z - BOARD_SIZE / 2.0) * TILE_SIZE);
                tile.setTranslateY(-BOARD_HEIGHT / 2);

                // Alternate tile colors
                if ((x + z) % 2 == 0) {
                    tile.setMaterial(new PhongMaterial(Color.WHITE));
                } else {
                    tile.setMaterial(new PhongMaterial(Color.BLACK));
                }

                board.getChildren().add(tile);
            }
        }

        return board;
    }

    public static void main(String[] args) {
        launch(args);
    }
}