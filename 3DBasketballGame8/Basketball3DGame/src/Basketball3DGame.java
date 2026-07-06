import javafx.animation.*;
import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.*;
import javafx.scene.transform.Rotate;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;

public class Basketball3DGame extends Application {

    // Game Constants
    private static final double COURT_WIDTH = 800;
    private static final double COURT_LENGTH = 1200;
    private static final double HOOP_HEIGHT = 305;
    private static final double HOOP_Z = -500;
    private static final double GRAVITY = 0.5;
    private static final double PLAYER_SPEED = 3;
    private static final double JUMP_FORCE = 12;

    // Game State
    private enum GameState { MENU, PLAYING, PAUSED, GAME_OVER }
    private enum Difficulty { EASY, MEDIUM, HARD }
    private GameState currentState = GameState.MENU;
    private Difficulty currentDifficulty = Difficulty.MEDIUM;

    // 3D Scene Components
    private Group root;
    private PerspectiveCamera camera;
    private SubScene subScene;

    // Game Objects
    private Player player1;
    private Player player2;
    private Basketball ball;
    private Hoop hoop;
    private Court court;

    // Game Logic
    private int player1Score = 0;
    private int player2Score = 0;
    private boolean isPlayer2AI = true;
    private int gameTime = 120; // seconds
    private Timeline gameTimer;
    private boolean ballInAir = false;

    // Input Handling
    private Set<KeyCode> pressedKeys = new HashSet<>();

    @Override
    public void start(Stage primaryStage) {
        root = new Group();

        // Create 3D SubScene
        subScene = new SubScene(root, 1200, 800, true, SceneAntialiasing.BALANCED);
        subScene.setFill(Color.SKYBLUE);

        setupCamera();
        setupLighting();
        createCourt();
        createHoop();
        createPlayers();
        createBall();
        setupInputHandling();

        // Main game loop
        AnimationTimer gameLoop = new AnimationTimer() {
            @Override
            public void handle(long now) {
                update();
            }
        };
        gameLoop.start();

        Scene scene = new Scene(new javafx.scene.Group(subScene));
        primaryStage.setTitle("3D Basketball Championship");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void setupCamera() {
        camera = new PerspectiveCamera(true);
        camera.setTranslateX(0);
        camera.setTranslateY(-400);
        camera.setTranslateZ(800);
        camera.setRotate(-20);
        camera.setFieldOfView(60);
        camera.setNearClip(0.1);
        camera.setFarClip(5000);
        subScene.setCamera(camera);
    }

    private void setupLighting() {
        // Ambient light for base visibility
        AmbientLight ambientLight = new AmbientLight(Color.rgb(80, 80, 80));
        root.getChildren().add(ambientLight);

        // Directional light simulating stadium lights
        PointLight stadiumLight1 = new PointLight(Color.WHITE);
        stadiumLight1.setTranslateX(-300);
        stadiumLight1.setTranslateY(-600);
        stadiumLight1.setTranslateZ(0);
        root.getChildren().add(stadiumLight1);

        PointLight stadiumLight2 = new PointLight(Color.WHITE);
        stadiumLight2.setTranslateX(300);
        stadiumLight2.setTranslateY(-600);
        stadiumLight2.setTranslateZ(0);
        root.getChildren().add(stadiumLight2);
    }

    private void createCourt() {
        court = new Court();
        root.getChildren().add(court.getNode());
    }

    private void createHoop() {
        hoop = new Hoop(0, HOOP_HEIGHT, HOOP_Z);
        root.getChildren().add(hoop.getNode());
    }

    private void createPlayers() {
        // Player 1 (Human) - Red team
        player1 = new Player("Player 1", Color.RED, 0, 0, 400, true);
        root.getChildren().add(player1.getNode());

        // Player 2 (AI or Human) - Blue team
        player2 = new Player("Player 2", Color.BLUE, 0, 0, -400, false);
        root.getChildren().add(player2.getNode());
    }

    private void createBall() {
        ball = new Basketball();
        ball.setPosition(player1.getX(), player1.getY() + 60, player1.getZ());
        root.getChildren().add(ball.getNode());
    }

    private void setupInputHandling() {
        subScene.setOnKeyPressed(e -> pressedKeys.add(e.getCode()));
        subScene.setOnKeyReleased(e -> pressedKeys.remove(e.getCode()));
        subScene.requestFocus();
    }

    private void update() {
        if (currentState != GameState.PLAYING) return;

        handlePlayerInput();
        updateBallPhysics();
        updateAI();
        checkCollisions();
        updateCamera();
    }

    private void handlePlayerInput() {
        // Player 1 Controls (WASD + Space)
        if (pressedKeys.contains(KeyCode.W)) player1.move(0, 0, -PLAYER_SPEED);
        if (pressedKeys.contains(KeyCode.S)) player1.move(0, 0, PLAYER_SPEED);
        if (pressedKeys.contains(KeyCode.A)) player1.move(-PLAYER_SPEED, 0, 0);
        if (pressedKeys.contains(KeyCode.D)) player1.move(PLAYER_SPEED, 0, 0);
        if (pressedKeys.contains(KeyCode.SPACE) && !ballInAir) shootBall(player1);

        // Player 2 Controls (Arrow keys + Enter) - if not AI
        if (!isPlayer2AI) {
            if (pressedKeys.contains(KeyCode.UP)) player2.move(0, 0, -PLAYER_SPEED);
            if (pressedKeys.contains(KeyCode.DOWN)) player2.move(0, 0, PLAYER_SPEED);
            if (pressedKeys.contains(KeyCode.LEFT)) player2.move(-PLAYER_SPEED, 0, 0);
            if (pressedKeys.contains(KeyCode.RIGHT)) player2.move(PLAYER_SPEED, 0, 0);
            if (pressedKeys.contains(KeyCode.ENTER) && !ballInAir) shootBall(player2);
        }
    }

    private void shootBall(Player shooter) {
        ballInAir = true;

        // Calculate trajectory to hoop
        double dx = hoop.getX() - shooter.getX();
        double dy = hoop.getY() - shooter.getY();
        double dz = hoop.getZ() - shooter.getZ();
        double distance = Math.sqrt(dx*dx + dz*dz);

        // Power based on distance
        double power = Math.min(distance / 100.0, 15);
        double height = Math.max(dy / 20.0, 8);

        ball.shoot(dx/distance * power, height, dz/distance * power);
    }

    private void updateBallPhysics() {
        if (!ballInAir) {
            // Ball follows player who has possession
            if (ball.getPossession() == 1) {
                ball.setPosition(player1.getX(), player1.getY() + 60, player1.getZ());
            } else {
                ball.setPosition(player2.getX(), player2.getY() + 60, player2.getZ());
            }
            return;
        }

        ball.updatePhysics();

        // Check if ball reached hoop area
        double distToHoop = Math.sqrt(
                Math.pow(ball.getX() - hoop.getX(), 2) +
                        Math.pow(ball.getZ() - hoop.getZ(), 2)
        );

        if (distToHoop < 30 && Math.abs(ball.getY() - hoop.getY()) < 20) {
            // Score!
            if (ball.getVelocityZ() < 0) { // Moving towards player 1's side
                player2Score += calculatePoints();
            } else {
                player1Score += calculatePoints();
            }
            resetBall();
        }

        // Ball hit ground
        if (ball.getY() <= 0) {
            ball.bounce();
            if (Math.abs(ball.getVelocityY()) < 1) {
                resetBall();
            }
        }
    }

    private int calculatePoints() {
        double dist = Math.sqrt(
                Math.pow(ball.getStartX() - hoop.getX(), 2) +
                        Math.pow(ball.getStartZ() - hoop.getZ(), 2)
        );
        return dist > 600 ? 3 : 2; // 3-pointer or 2-pointer
    }

    private void resetBall() {
        ballInAir = false;
        ball.reset();
        // Alternate possession or give to scorer's opponent
        ball.setPossession(ball.getPossession() == 1 ? 2 : 1);
    }

    private void updateAI() {
        if (!isPlayer2AI) return;

        Player ai = player2;
        double targetX = ball.getX();
        double targetZ = ball.getZ();

        // AI difficulty adjustments
        double speed = PLAYER_SPEED;
        double accuracy = 0.8;

        switch (currentDifficulty) {
            case EASY:
                speed *= 0.6;
                accuracy = 0.4;
                break;
            case MEDIUM:
                speed *= 0.85;
                accuracy = 0.7;
                break;
            case HARD:
                speed *= 1.1;
                accuracy = 0.95;
                break;
        }

        // Move towards ball or optimal position
        if (!ballInAir) {
            if (ball.getPossession() == 2) {
                // Has ball - move to shooting position
                targetZ = HOOP_Z + 200;
                targetX = Math.max(-200, Math.min(200, ball.getX() + (Math.random()-0.5)*100));

                double distToHoop = Math.sqrt(
                        Math.pow(ai.getX() - hoop.getX(), 2) +
                                Math.pow(ai.getZ() - hoop.getZ(), 2)
                );

                if (distToHoop < 400 && Math.random() < accuracy) {
                    shootBall(ai);
                }
            } else {
                // Defend - move between player and hoop
                targetX = (player1.getX() + hoop.getX()) / 2;
                targetZ = (player1.getZ() + hoop.getZ()) / 2;
            }
        }

        // Smooth movement
        double dx = targetX - ai.getX();
        double dz = targetZ - ai.getZ();
        double dist = Math.sqrt(dx*dx + dz*dz);

        if (dist > 5) {
            ai.move(dx/dist * speed, 0, dz/dist * speed);
        }
    }

    private void checkCollisions() {
        // Player-Ball possession
        if (!ballInAir) {
            double d1 = distance(player1, ball);
            double d2 = distance(player2, ball);

            if (d1 < 50) ball.setPossession(1);
            else if (d2 < 50) ball.setPossession(2);
        }
    }

    private double distance(Player p, Basketball b) {
        return Math.sqrt(
                Math.pow(p.getX() - b.getX(), 2) +
                        Math.pow(p.getZ() - b.getZ(), 2)
        );
    }

    private void updateCamera() {
        // Smooth camera follow
        double targetX = (player1.getX() + player2.getX()) / 2;
        double targetZ = (player1.getZ() + player2.getZ()) / 2 + 400;

        camera.setTranslateX(camera.getTranslateX() + (targetX - camera.getTranslateX()) * 0.05);
        camera.setTranslateZ(camera.getTranslateZ() + (targetZ - camera.getTranslateZ()) * 0.05);
    }

    // ==================== 3D OBJECT CLASSES ====================

    class Court {
        private Group node;

        public Court() {
            node = new Group();

            // Main floor
            Box floor = new Box(COURT_WIDTH, 10, COURT_LENGTH);
            PhongMaterial floorMat = new PhongMaterial();
            floorMat.setDiffuseColor(Color.SADDLEBROWN);
            floor.setMaterial(floorMat);
            floor.setTranslateY(5);
            node.getChildren().add(floor);

            // Court markings
            createCourtMarkings();

            // Backboards
            createBackboard(0, HOOP_HEIGHT, HOOP_Z - 50);
            createBackboard(0, HOOP_HEIGHT, -HOOP_Z + 50);
        }

        private void createCourtMarkings() {
            // Center line
            Box centerLine = new Box(COURT_WIDTH, 1, 5);
            centerLine.setMaterial(new PhongMaterial(Color.WHITE));
            centerLine.setTranslateY(11);
            node.getChildren().add(centerLine);

            // Center circle
            Cylinder centerCircle = new Cylinder(60, 1);
            centerCircle.setMaterial(new PhongMaterial(Color.WHITE));
            centerCircle.setTranslateY(11);
            node.getChildren().add(centerCircle);

            // 3-point lines (simplified as boxes)
            Box threePoint1 = new Box(5, 1, 400);
            threePoint1.setMaterial(new PhongMaterial(Color.WHITE));
            threePoint1.setTranslateX(COURT_WIDTH/2 - 100);
            threePoint1.setTranslateZ(HOOP_Z/2);
            threePoint1.setTranslateY(11);
            node.getChildren().add(threePoint1);

            Box threePoint2 = new Box(5, 1, 400);
            threePoint2.setMaterial(new PhongMaterial(Color.WHITE));
            threePoint2.setTranslateX(-COURT_WIDTH/2 + 100);
            threePoint2.setTranslateZ(HOOP_Z/2);
            threePoint2.setTranslateY(11);
            node.getChildren().add(threePoint2);
        }

        private void createBackboard(double x, double y, double z) {
            Box backboard = new Box(90, 60, 5);
            PhongMaterial glass = new PhongMaterial(Color.rgb(200, 220, 255, 0.6));
            glass.setSpecularColor(Color.WHITE);
            backboard.setMaterial(glass);
            backboard.setTranslateX(x);
            backboard.setTranslateY(y);
            backboard.setTranslateZ(z);
            node.getChildren().add(backboard);
        }

        public Group getNode() { return node; }
    }

    class Hoop {
        private Group node;
        private double x, y, z;

        public Hoop(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
            node = new Group();

            // Rim
            Cylinder rim = new Cylinder(23, 5);
            rim.setMaterial(new PhongMaterial(Color.ORANGE));
            rim.setTranslateX(x);
            rim.setTranslateY(y);
            rim.setTranslateZ(z);
            node.getChildren().add(rim);

            // Net (simplified as cone)
            Cylinder net = new Cylinder(23, 10);
            net.setHeight(40);
            PhongMaterial netMat = new PhongMaterial(Color.WHITE);
            netMat.setDiffuseColor(Color.rgb(255, 255, 255, 0.5));
            net.setMaterial(netMat);
            net.setTranslateX(x);
            net.setTranslateY(y + 20);
            net.setTranslateZ(z);
            node.getChildren().add(net);
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public Group getNode() { return node; }
    }

    class Player {
        private Group node;
        private String name;
        private double x, y, z;
        private double velocityY = 0;
        private boolean isJumping = false;
        private boolean isHuman;
        private Color teamColor;
        private Rotate rotateY;

        public Player(String name, Color color, double x, double y, double z, boolean isHuman) {
            this.name = name;
            this.teamColor = color;
            this.x = x; this.y = y; this.z = z;
            this.isHuman = isHuman;
            node = new Group();

            // Body
            Sphere body = new Sphere(25);
            PhongMaterial bodyMat = new PhongMaterial(color);
            body.setMaterial(bodyMat);
            body.setTranslateY(-30);
            node.getChildren().add(body);

            // Head
            Sphere head = new Sphere(15);
            PhongMaterial skinMat = new PhongMaterial(Color.PEACHPUFF);
            head.setMaterial(skinMat);
            head.setTranslateY(-70);
            node.getChildren().add(head);

            // Arms
            Box leftArm = new Box(8, 40, 8);
            leftArm.setMaterial(bodyMat);
            leftArm.setTranslateX(-30);
            leftArm.setTranslateY(-30);
            node.getChildren().add(leftArm);

            Box rightArm = new Box(8, 40, 8);
            rightArm.setMaterial(bodyMat);
            rightArm.setTranslateX(30);
            rightArm.setTranslateY(-30);
            node.getChildren().add(rightArm);

            // Legs
            Box leftLeg = new Box(10, 50, 10);
            leftLeg.setMaterial(new PhongMaterial(Color.DARKGRAY));
            leftLeg.setTranslateX(-12);
            leftLeg.setTranslateY(20);
            node.getChildren().add(leftLeg);

            Box rightLeg = new Box(10, 50, 10);
            rightLeg.setMaterial(new PhongMaterial(Color.DARKGRAY));
            rightLeg.setTranslateX(12);
            rightLeg.setTranslateY(20);
            node.getChildren().add(rightLeg);

            rotateY = new Rotate(0, Rotate.Y_AXIS);
            node.getTransforms().add(rotateY);
            updatePosition();
        }

        public void move(double dx, double dy, double dz) {
            x += dx;
            z += dz;

            // Court boundaries
            x = Math.max(-COURT_WIDTH/2 + 30, Math.min(COURT_WIDTH/2 - 30, x));
            z = Math.max(-COURT_LENGTH/2 + 30, Math.min(COURT_LENGTH/2 - 30, z));

            // Face movement direction
            if (Math.abs(dx) > 0.1 || Math.abs(dz) > 0.1) {
                double angle = Math.toDegrees(Math.atan2(dx, dz));
                rotateY.setAngle(angle);
            }

            updatePosition();
        }

        private void updatePosition() {
            node.setTranslateX(x);
            node.setTranslateY(y);
            node.setTranslateZ(z);
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public Group getNode() { return node; }
    }

    class Basketball {
        private Group node;
        private Sphere ball;
        private double x, y, z;
        private double vx, vy, vz;
        private double startX, startZ;
        private int possession = 1; // 1 or 2

        public Basketball() {
            node = new Group();
            ball = new Sphere(12);

            PhongMaterial ballMat = new PhongMaterial();
            ballMat.setDiffuseColor(Color.ORANGE);
            ballMat.setSpecularColor(Color.WHITE);
            ball.setMaterial(ballMat);

            // Ball lines (simplified)
            node.getChildren().add(ball);
        }

        public void shoot(double vx, double vy, double vz) {
            this.vx = vx;
            this.vy = vy;
            this.vz = vz;
            this.startX = x;
            this.startZ = z;
        }

        public void updatePhysics() {
            x += vx;
            y += vy;
            z += vz;
            vy -= GRAVITY;

            // Air resistance
            vx *= 0.995;
            vz *= 0.995;

            updatePosition();
        }

        public void bounce() {
            vy = -vy * 0.6;
            vx *= 0.8;
            vz *= 0.8;
            y = 12; // Ball radius
        }

        public void reset() {
            vx = vy = vz = 0;
        }

        public void setPosition(double x, double y, double z) {
            this.x = x; this.y = y; this.z = z;
            updatePosition();
        }

        private void updatePosition() {
            node.setTranslateX(x);
            node.setTranslateY(y);
            node.setTranslateZ(z);

            // Rotate ball while moving
            if (Math.abs(vx) > 0.1 || Math.abs(vz) > 0.1) {
                ball.setRotationAxis(Rotate.Z_AXIS);
                ball.setRotate(ball.getRotate() + Math.sqrt(vx*vx + vz*vz) * 2);
            }
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }
        public double getVelocityZ() { return vz; }
        public double getStartX() { return startX; }
        public double getStartZ() { return startZ; }
        public int getPossession() { return possession; }
        public void setPossession(int p) { this.possession = p; }
        public Group getNode() { return node; }
    }

    public static void main(String[] args) {
        launch(args);
    }
}