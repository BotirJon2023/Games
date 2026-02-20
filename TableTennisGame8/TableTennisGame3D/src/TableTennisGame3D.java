import javafx.animation.*;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.*;
import javafx.scene.paint.*;
import javafx.scene.shape.*;
import javafx.scene.transform.*;
import javafx.scene.effect.*;
import javafx.scene.image.*;
import javafx.scene.media.*;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;
import java.util.*;
import java.util.stream.*;

public class TableTennisGame3D extends Application {

    // Game Constants
    private static final double TABLE_WIDTH = 800;
    private static final double TABLE_HEIGHT = 400;
    private static final double TABLE_DEPTH = 150;
    private static final double NET_HEIGHT = 15;
    private static final double BALL_RADIUS = 10;
    private static final double PADDLE_WIDTH = 20;
    private static final double PADDLE_HEIGHT = 100;
    private static final double PADDLE_DEPTH = 30;
    private static final double PLAYER_SPEED = 8;
    private static final double BALL_SPEED = 12;
    private static final double MAX_SPIN = 5;

    // Game State
    private enum GameState { START, PLAYING, PAUSED, GAME_OVER }
    private GameState gameState = GameState.START;

    // 3D Scene Components
    private Group root3D;
    private PerspectiveCamera camera;
    private AmbientLight ambientLight;
    private PointLight pointLight;

    // Players
    private Player3D player1;
    private Player3D player2;

    // Ball
    private Sphere ball;
    private double ballVelocityX = BALL_SPEED;
    private double ballVelocityY = 0;
    private double ballVelocityZ = 0;
    private double ballSpin = 0;

    // Table
    private Box table;
    private Box net;

    // Score
    private IntegerProperty player1Score = new SimpleIntegerProperty(0);
    private IntegerProperty player2Score = new SimpleIntegerProperty(0);
    private final int WINNING_SCORE = 11;

    // Animation
    private AnimationTimer gameLoop;
    private Timeline serveTimer;
    private Timeline celebrationTimer;

    // Controls
    private Set<KeyCode> activeKeys = new HashSet<>();

    // UI Components
    private Label scoreLabel;
    private Label statusLabel;
    private Label controlsLabel;
    private VBox uiContainer;

    // Player Animation States
    private enum PlayerAnimation { IDLE, MOVING, SWINGING, SERVING, CELEBRATING, DEFEATED }

    // 3D Player Class
    private class Player3D extends Group {
        private Box body;
        private Cylinder head;
        private Box leftArm;
        private Box rightArm;
        private Box leftLeg;
        private Box rightLeg;
        private Box paddle;

        private double x, y, z;
        private double targetX, targetY, targetZ;
        private PlayerAnimation animationState = PlayerAnimation.IDLE;
        private Timeline swingAnimation;
        private Timeline serveAnimation;
        private Rotate bodyRotation;
        private Rotate armRotation;

        private boolean isPlayer1;
        private Color playerColor;

        public Player3D(boolean isPlayer1, double startX, double startY, double startZ) {
            this.isPlayer1 = isPlayer1;
            this.x = startX;
            this.y = startY;
            this.z = startZ;
            this.playerColor = isPlayer1 ? Color.BLUE : Color.RED;

            createBody();
            createHead();
            createArms();
            createLegs();
            createPaddle();
            setupAnimations();

            setTranslateX(x);
            setTranslateY(y);
            setTranslateZ(z);
        }

        private void createBody() {
            body = new Box(40, 120, 30);
            body.setMaterial(new PhongMaterial(playerColor));
            body.setTranslateY(-60);
            getChildren().add(body);

            bodyRotation = new Rotate(0, Rotate.Y_AXIS);
            body.getTransforms().add(bodyRotation);
        }

        private void createHead() {
            head = new Cylinder(20, 25);
            head.setMaterial(new PhongMaterial(Color.BEIGE));
            head.setTranslateY(-140);
            getChildren().add(head);
        }

        private void createArms() {
            leftArm = new Box(15, 70, 20);
            leftArm.setMaterial(new PhongMaterial(playerColor.darker()));
            leftArm.setTranslateX(-30);
            leftArm.setTranslateY(-90);
            leftArm.setTranslateZ(0);
            getChildren().add(leftArm);

            rightArm = new Box(15, 70, 20);
            rightArm.setMaterial(new PhongMaterial(playerColor.darker()));
            rightArm.setTranslateX(30);
            rightArm.setTranslateY(-90);
            rightArm.setTranslateZ(0);
            getChildren().add(rightArm);

            armRotation = new Rotate(0, Rotate.Z_AXIS);
            rightArm.getTransforms().add(armRotation);
        }

        private void createLegs() {
            leftLeg = new Box(20, 80, 25);
            leftLeg.setMaterial(new PhongMaterial(Color.BLACK));
            leftLeg.setTranslateX(-15);
            leftLeg.setTranslateY(10);
            getChildren().add(leftLeg);

            rightLeg = new Box(20, 80, 25);
            rightLeg.setMaterial(new PhongMaterial(Color.BLACK));
            rightLeg.setTranslateX(15);
            rightLeg.setTranslateY(10);
            getChildren().add(rightLeg);
        }

        private void createPaddle() {
            paddle = new Box(PADDLE_WIDTH, PADDLE_HEIGHT, PADDLE_DEPTH);
            PhongMaterial paddleMaterial = new PhongMaterial(
                    isPlayer1 ? Color.LIGHTBLUE : Color.LIGHTCORAL
            );
            paddleMaterial.setSpecularColor(Color.WHITE);
            paddleMaterial.setSpecularPower(32);
            paddle.setMaterial(paddleMaterial);

            // Position paddle in hand
            paddle.setTranslateX(isPlayer1 ? 50 : -50);
            paddle.setTranslateY(-100);
            paddle.setTranslateZ(20);

            // Add rubber surface
            Box rubber = new Box(PADDLE_WIDTH - 2, PADDLE_HEIGHT - 10, 5);
            rubber.setMaterial(new PhongMaterial(Color.DARKRED));
            rubber.setTranslateZ(-PADDLE_DEPTH/2 - 3);
            paddle.getChildren().add(rubber);

            getChildren().add(paddle);
        }

        private void setupAnimations() {
            // Swing animation
            swingAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(armRotation.angleProperty(), 0),
                            new KeyValue(paddle.translateZProperty(), 20)
                    ),
                    new KeyFrame(Duration.millis(150),
                            new KeyValue(armRotation.angleProperty(), -45),
                            new KeyValue(paddle.translateZProperty(), 40)
                    ),
                    new KeyFrame(Duration.millis(300),
                            new KeyValue(armRotation.angleProperty(), 0),
                            new KeyValue(paddle.translateZProperty(), 20)
                    )
            );

            // Serve animation
            serveAnimation = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(paddle.translateYProperty(), -100),
                            new KeyValue(paddle.translateZProperty(), 20)
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(paddle.translateYProperty(), -120),
                            new KeyValue(paddle.translateZProperty(), 40)
                    ),
                    new KeyFrame(Duration.millis(400),
                            new KeyValue(paddle.translateYProperty(), -100),
                            new KeyValue(paddle.translateZProperty(), 20)
                    )
            );
        }

        public void move(double deltaX, double deltaY, double deltaZ) {
            double newX = x + deltaX;
            double newY = y + deltaY;
            double newZ = z + deltaZ;

            // Boundary checking
            double tableHalf = TABLE_WIDTH / 2;
            double playerBoundary = tableHalf - 100;

            if (isPlayer1) {
                newX = Math.max(-playerBoundary, Math.min(-50, newX));
            } else {
                newX = Math.min(playerBoundary, Math.max(50, newX));
            }

            newY = Math.max(-TABLE_HEIGHT/2 + 50, Math.min(TABLE_HEIGHT/2 - 50, newY));
            newZ = Math.max(-TABLE_DEPTH/2 + 50, Math.min(TABLE_DEPTH/2 - 50, newZ));

            x = newX;
            y = newY;
            z = newZ;

            setTranslateX(x);
            setTranslateY(y);
            setTranslateZ(z);

            // Update body rotation based on movement
            if (deltaX != 0 || deltaZ != 0) {
                double angle = Math.toDegrees(Math.atan2(deltaZ, deltaX));
                bodyRotation.setAngle(angle);
            }

            animationState = (deltaX != 0 || deltaY != 0 || deltaZ != 0) ?
                    PlayerAnimation.MOVING : PlayerAnimation.IDLE;
        }

        public void swing() {
            if (animationState != PlayerAnimation.SWINGING) {
                animationState = PlayerAnimation.SWINGING;
                swingAnimation.play();
                swingAnimation.setOnFinished(e -> animationState = PlayerAnimation.IDLE);
            }
        }

        public void serve() {
            animationState = PlayerAnimation.SERVING;
            serveAnimation.play();
            serveAnimation.setOnFinished(e -> animationState = PlayerAnimation.IDLE);
        }

        public void celebrate() {
            animationState = PlayerAnimation.CELEBRATING;
            Timeline celebration = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(bodyRotation.angleProperty(), 0)
                    ),
                    new KeyFrame(Duration.millis(200),
                            new KeyValue(bodyRotation.angleProperty(), 360)
                    )
            );
            celebration.setCycleCount(3);
            celebration.play();
            celebration.setOnFinished(e -> animationState = PlayerAnimation.IDLE);
        }

        public Box getPaddle() {
            return paddle;
        }

        public double getX() { return x; }
        public double getY() { return y; }
        public double getZ() { return z; }

        public Point3D getPaddlePosition() {
            return new Point3D(
                    x + paddle.getTranslateX(),
                    y + paddle.getTranslateY(),
                    z + paddle.getTranslateZ()
            );
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            setup3DScene();
            setupGameObjects();
            setupUI();
            setupEventHandlers();
            setupGameLoop();

            Scene scene = new Scene(createSceneGraph(), 1200, 800, true, SceneAntialiasing.BALANCED);
            scene.setFill(Color.LIGHTGRAY);

            primaryStage.setTitle("3D Table Tennis Championship");
            primaryStage.setScene(scene);
            primaryStage.show();

            startGame();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private Group createSceneGraph() {
        Group root = new Group();

        // 3D SubScene
        SubScene subScene3D = new SubScene(root3D, 1000, 800, true, SceneAntialiasing.BALANCED);
        subScene3D.setCamera(camera);
        subScene3D.setFill(Color.LIGHTBLUE);

        // UI Overlay
        StackPane overlay = new StackPane();
        overlay.getChildren().addAll(subScene3D, uiContainer);

        root.getChildren().add(overlay);
        return root;
    }

    private void setup3DScene() {
        root3D = new Group();

        // Camera setup
        camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-800);
        camera.setTranslateY(-100);
        camera.setRotationAxis(Rotate.X_AXIS);
        camera.setRotate(-20);

        // Lighting
        ambientLight = new AmbientLight(Color.WHITE);
        ambientLight.setLightOn(true);

        pointLight = new PointLight(Color.WHITE);
        pointLight.setTranslateX(200);
        pointLight.setTranslateY(-200);
        pointLight.setTranslateZ(-400);

        root3D.getChildren().addAll(ambientLight, pointLight);

        // Floor
        Box floor = new Box(2000, 10, 2000);
        PhongMaterial floorMat = new PhongMaterial(Color.DARKGRAY);
        floorMat.setDiffuseMap(createCheckerPattern());
        floor.setMaterial(floorMat);
        floor.setTranslateY(200);
        root3D.getChildren().add(floor);

        // Background walls
        createBackgroundWalls();
    }

    private void createBackgroundWalls() {
        // Back wall
        Box backWall = new Box(2000, 1000, 10);
        backWall.setMaterial(new PhongMaterial(Color.LIGHTBLUE.brighter()));
        backWall.setTranslateZ(1000);
        backWall.setTranslateY(-400);
        root3D.getChildren().add(backWall);

        // Side walls with audience
        for (int i = 0; i < 20; i++) {
            Box audience = new Box(30, 100, 20);
            audience.setMaterial(new PhongMaterial(
                    Color.rgb(new Random().nextInt(200, 255),
                            new Random().nextInt(150, 220),
                            new Random().nextInt(100, 200))
            ));
            audience.setTranslateX(800 + i * 40);
            audience.setTranslateY(-50);
            audience.setTranslateZ(300);
            root3D.getChildren().add(audience);

            Box audience2 = new Box(30, 100, 20);
            audience2.setMaterial(new PhongMaterial(
                    Color.rgb(new Random().nextInt(200, 255),
                            new Random().nextInt(150, 220),
                            new Random().nextInt(100, 200))
            ));
            audience2.setTranslateX(-800 - i * 40);
            audience2.setTranslateY(-50);
            audience2.setTranslateZ(300);
            root3D.getChildren().add(audience2);
        }
    }

    private ImagePattern createCheckerPattern() {
        int size = 100;
        WritableImage img = new WritableImage(size, size);
        PixelWriter pw = img.getPixelWriter();

        for (int x = 0; x < size; x++) {
            for (int y = 0; y < size; y++) {
                Color color = ((x / 20) + (y / 20)) % 2 == 0 ?
                        Color.GRAY : Color.LIGHTGRAY;
                pw.setColor(x, y, color);
            }
        }

        return new ImagePattern(img, 0, 0, size, size, false);
    }

    private void setupGameObjects() {
        // Create table
        table = new Box(TABLE_WIDTH, 20, TABLE_DEPTH);
        PhongMaterial tableMat = new PhongMaterial(Color.DARKGREEN);
        tableMat.setSpecularColor(Color.WHITE);
        tableMat.setSpecularPower(64);
        table.setMaterial(tableMat);
        table.setTranslateY(-TABLE_HEIGHT/2);
        root3D.getChildren().add(table);

        // Add table lines
        addTableLines();

        // Create net
        net = new Box(10, NET_HEIGHT, TABLE_DEPTH);
        net.setMaterial(new PhongMaterial(Color.WHITE));
        net.setTranslateY(-TABLE_HEIGHT/2 - NET_HEIGHT/2);
        root3D.getChildren().add(net);

        // Create players
        player1 = new Player3D(true, -TABLE_WIDTH/2 + 100, -50, 0);
        player2 = new Player3D(false, TABLE_WIDTH/2 - 100, -50, 0);
        root3D.getChildren().addAll(player1, player2);

        // Create ball
        ball = new Sphere(BALL_RADIUS);
        PhongMaterial ballMat = new PhongMaterial(Color.ORANGE);
        ballMat.setSpecularColor(Color.WHITE);
        ballMat.setSpecularPower(128);
        ball.setMaterial(ballMat);
        resetBall();
        root3D.getChildren().add(ball);

        // Add shadows
        addShadows();
    }

    private void addTableLines() {
        // White border
        Box border = new Box(TABLE_WIDTH + 40, 5, TABLE_DEPTH + 40);
        border.setMaterial(new PhongMaterial(Color.WHITE));
        border.setTranslateY(-TABLE_HEIGHT/2 - 10);
        root3D.getChildren().add(border);

        // Center line
        Box centerLine = new Box(5, 5, TABLE_DEPTH);
        centerLine.setMaterial(new PhongMaterial(Color.WHITE));
        centerLine.setTranslateY(-TABLE_HEIGHT/2 - 10);
        root3D.getChildren().add(centerLine);

        // Service area markers
        for (int i = -1; i <= 1; i += 2) {
            Box marker = new Box(10, 5, 5);
            marker.setMaterial(new PhongMaterial(Color.WHITE));
            marker.setTranslateX(i * TABLE_WIDTH/4);
            marker.setTranslateY(-TABLE_HEIGHT/2 - 10);
            marker.setTranslateZ(TABLE_DEPTH/4);
            root3D.getChildren().add(marker);
        }
    }

    private void addShadows() {
        DropShadow shadow = new DropShadow();
        shadow.setRadius(20);
        shadow.setColor(Color.BLACK);

        for (Node node : root3D.getChildren()) {
            if (node instanceof Player3D || node instanceof Sphere || node instanceof Box) {
                node.setEffect(shadow);
            }
        }
    }

    private void setupUI() {
        // Score display
        scoreLabel = new Label();
        scoreLabel.setFont(Font.font("Arial", FontWeight.BOLD, 48));
        scoreLabel.setTextFill(Color.WHITE);
        scoreLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 20px;");
        scoreLabel.textProperty().bind(
                new SimpleStringProperty("SCORE: ").concat(player1Score.asString())
                        .concat(" - ").concat(player2Score.asString())
        );

        // Status display
        statusLabel = new Label("PRESS SPACE TO START");
        statusLabel.setFont(Font.font("Arial", FontWeight.BOLD, 32));
        statusLabel.setTextFill(Color.YELLOW);
        statusLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 15px;");

        // Controls display
        controlsLabel = new Label(
                "PLAYER 1: W/A/S/D + Q/E for vertical\n" +
                        "PLAYER 2: ARROWS + PG UP/DN\n" +
                        "SPACE: Start/Pause | R: Reset | F: Swing\n" +
                        "1/2: Change Camera | +/-: Zoom"
        );
        controlsLabel.setFont(Font.font("Arial", FontWeight.NORMAL, 16));
        controlsLabel.setTextFill(Color.WHITE);
        controlsLabel.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-padding: 10px;");

        uiContainer = new VBox(20);
        uiContainer.setAlignment(Pos.TOP_CENTER);
        uiContainer.setPadding(new Insets(20));
        uiContainer.getChildren().addAll(scoreLabel, statusLabel, controlsLabel);
        uiContainer.setPickOnBounds(false);
    }

    private void setupEventHandlers() {
        // Key handlers
        EventHandler<javafx.scene.input.KeyEvent> keyPressed = e -> {
            activeKeys.add(e.getCode());

            if (e.getCode() == KeyCode.SPACE) {
                if (gameState == GameState.START || gameState == GameState.PAUSED) {
                    gameState = GameState.PLAYING;
                    statusLabel.setText("GAME ON!");
                } else if (gameState == GameState.PLAYING) {
                    gameState = GameState.PAUSED;
                    statusLabel.setText("PAUSED");
                }
            }

            if (e.getCode() == KeyCode.R) {
                resetGame();
            }

            if (e.getCode() == KeyCode.F) {
                player1.swing();
            }

            if (e.getCode() == KeyCode.ENTER) {
                player2.swing();
            }

            // Camera controls
            if (e.getCode() == KeyCode.DIGIT1) {
                camera.setTranslateX(-300);
                camera.setTranslateY(-100);
                camera.setTranslateZ(-600);
            }

            if (e.getCode() == KeyCode.DIGIT2) {
                camera.setTranslateX(300);
                camera.setTranslateY(-100);
                camera.setTranslateZ(-600);
            }

            if (e.getCode() == KeyCode.ADD) {
                camera.setTranslateZ(camera.getTranslateZ() + 50);
            }

            if (e.getCode() == KeyCode.SUBTRACT) {
                camera.setTranslateZ(camera.getTranslateZ() - 50);
            }
        };

        EventHandler<javafx.scene.input.KeyEvent> keyReleased = e -> {
            activeKeys.remove(e.getCode());
        };

        root3D.setOnKeyPressed(keyPressed);
        root3D.setOnKeyReleased(keyReleased);
    }

    private void setupGameLoop() {
        gameLoop = new AnimationTimer() {
            private long lastUpdate = 0;

            @Override
            public void handle(long now) {
                if (lastUpdate == 0) {
                    lastUpdate = now;
                    return;
                }

                double elapsedSeconds = (now - lastUpdate) / 1_000_000_000.0;
                lastUpdate = now;

                if (gameState == GameState.PLAYING) {
                    handleInput(elapsedSeconds);
                    updateBall(elapsedSeconds);
                    checkCollisions();
                    updateAI();
                    checkScore();
                }

                updateCamera();
            }
        };
    }

    private void handleInput(double deltaTime) {
        // Player 1 controls (WASD + QE for vertical)
        double player1Speed = PLAYER_SPEED * deltaTime * 60;
        double dx1 = 0, dy1 = 0, dz1 = 0;

        if (activeKeys.contains(KeyCode.W)) dy1 -= player1Speed;
        if (activeKeys.contains(KeyCode.S)) dy1 += player1Speed;
        if (activeKeys.contains(KeyCode.A)) dx1 -= player1Speed;
        if (activeKeys.contains(KeyCode.D)) dx1 += player1Speed;
        if (activeKeys.contains(KeyCode.Q)) dz1 -= player1Speed;
        if (activeKeys.contains(KeyCode.E)) dz1 += player1Speed;

        if (dx1 != 0 || dy1 != 0 || dz1 != 0) {
            player1.move(dx1, dy1, dz1);
        }

        if (activeKeys.contains(KeyCode.F)) {
            player1.swing();
        }

        // Player 2 controls (Arrow keys + PgUp/PgDn)
        double player2Speed = PLAYER_SPEED * deltaTime * 60;
        double dx2 = 0, dy2 = 0, dz2 = 0;

        if (activeKeys.contains(KeyCode.UP)) dy2 -= player2Speed;
        if (activeKeys.contains(KeyCode.DOWN)) dy2 += player2Speed;
        if (activeKeys.contains(KeyCode.LEFT)) dx2 -= player2Speed;
        if (activeKeys.contains(KeyCode.RIGHT)) dx2 += player2Speed;
        if (activeKeys.contains(KeyCode.PAGE_UP)) dz2 -= player2Speed;
        if (activeKeys.contains(KeyCode.PAGE_DOWN)) dz2 += player2Speed;

        if (dx2 != 0 || dy2 != 0 || dz2 != 0) {
            player2.move(dx2, dy2, dz2);
        }

        if (activeKeys.contains(KeyCode.ENTER)) {
            player2.swing();
        }
    }

    private void updateBall(double deltaTime) {
        // Apply velocity
        double newX = ball.getTranslateX() + ballVelocityX * deltaTime * 60;
        double newY = ball.getTranslateY() + ballVelocityY * deltaTime * 60;
        double newZ = ball.getTranslateZ() + ballVelocityZ * deltaTime * 60;

        // Apply spin effect
        ballVelocityY += ballSpin * deltaTime;
        ballVelocityZ += ballSpin * 0.5 * deltaTime;

        // Gravity effect
        ballVelocityY += 0.5 * deltaTime * 60;

        // Air resistance
        ballVelocityX *= 0.999;
        ballVelocityY *= 0.999;
        ballVelocityZ *= 0.999;

        ball.setTranslateX(newX);
        ball.setTranslateY(newY);
        ball.setTranslateZ(newZ);

        // Ball rotation
        ball.setRotationAxis(Rotate.Y_AXIS);
        ball.setRotate(ball.getRotate() + ballVelocityX * 2);
    }

    private void checkCollisions() {
        double ballX = ball.getTranslateX();
        double ballY = ball.getTranslateY();
        double ballZ = ball.getTranslateZ();

        // Table boundaries
        double halfWidth = TABLE_WIDTH / 2;
        double halfDepth = TABLE_DEPTH / 2;
        double tableTop = -TABLE_HEIGHT / 2;

        // Floor collision
        if (ballY > 0) {
            ballVelocityY = -Math.abs(ballVelocityY) * 0.7;
            ball.setTranslateY(-1);
        }

        // Table collision
        if (ballX > -halfWidth && ballX < halfWidth &&
                ballZ > -halfDepth && ballZ < halfDepth &&
                ballY > tableTop - BALL_RADIUS && ballY < tableTop + 20) {

            ballVelocityY = -Math.abs(ballVelocityY) * 0.9;
            ball.setTranslateY(tableTop - BALL_RADIUS);

            // Add bounce sound effect simulation
            ballSpin = (Math.random() - 0.5) * MAX_SPIN * 2;
        }

        // Net collision
        if (Math.abs(ballX) < 5 && ballY < tableTop + NET_HEIGHT) {
            ballVelocityX = -ballVelocityX * 0.8;
            ballVelocityZ = -ballVelocityZ * 0.8;
        }

        // Wall collisions
        if (Math.abs(ballX) > halfWidth + 50) {
            ballVelocityX = -ballVelocityX * 0.9;
        }

        if (Math.abs(ballZ) > halfDepth + 50) {
            ballVelocityZ = -ballVelocityZ * 0.9;
        }

        // Paddle collisions
        checkPaddleCollision(player1);
        checkPaddleCollision(player2);
    }

    private void checkPaddleCollision(Player3D player) {
        Point3D paddlePos = player.getPaddlePosition();
        double dx = ball.getTranslateX() - paddlePos.getX();
        double dy = ball.getTranslateY() - paddlePos.getY();
        double dz = ball.getTranslateZ() - paddlePos.getZ();

        double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);
        double collisionDistance = BALL_RADIUS + PADDLE_WIDTH / 2;

        if (distance < collisionDistance) {
            // Calculate reflection
            double normalX = dx / distance;
            double normalY = dy / distance;
            double normalZ = dz / distance;

            // Dot product of velocity and normal
            double dotProduct = ballVelocityX * normalX + ballVelocityY * normalY + ballVelocityZ * normalZ;

            // Reflection formula: v' = v - 2*(v·n)*n
            ballVelocityX = ballVelocityX - 2 * dotProduct * normalX;
            ballVelocityY = ballVelocityY - 2 * dotProduct * normalY;
            ballVelocityZ = ballVelocityZ - 2 * dotProduct * normalZ;

            // Add paddle power
            double power = 1.5;
            ballVelocityX *= power;
            ballVelocityY *= power * 0.8;
            ballVelocityZ *= power;

            // Add spin based on paddle movement
            ballSpin = (Math.random() - 0.5) * MAX_SPIN;

            // Trigger swing animation
            player.swing();

            // Add hit effect
            addHitEffect(paddlePos);
        }
    }

    private void addHitEffect(Point3D position) {
        Sphere effect = new Sphere(5);
        effect.setMaterial(new PhongMaterial(Color.YELLOW));
        effect.setTranslateX(position.getX());
        effect.setTranslateY(position.getY());
        effect.setTranslateZ(position.getZ());
        root3D.getChildren().add(effect);

        // Animate and remove effect
        Timeline fadeOut = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(effect.scaleXProperty(), 1),
                        new KeyValue(effect.scaleYProperty(), 1),
                        new KeyValue(effect.scaleZProperty(), 1),
                        new KeyValue(((PhongMaterial)effect.getMaterial()).diffuseColorProperty(), Color.YELLOW)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(effect.scaleXProperty(), 3),
                        new KeyValue(effect.scaleYProperty(), 3),
                        new KeyValue(effect.scaleZProperty(), 3),
                        new KeyValue(((PhongMaterial)effect.getMaterial()).diffuseColorProperty(), Color.TRANSPARENT)
                )
        );

        fadeOut.setOnFinished(e -> root3D.getChildren().remove(effect));
        fadeOut.play();
    }

    private void updateAI() {
        // Simple AI for player 2 (can be enhanced)
        if (gameState == GameState.PLAYING) {
            double ballX = ball.getTranslateX();
            double ballY = ball.getTranslateY();

            // Move towards ball with some randomness
            if (Math.random() > 0.3) {
                double targetX = ballX - 100;
                double targetY = ballY;

                double dx = targetX - player2.getX();
                double dy = targetY - player2.getY();

                // Normalize and apply speed
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance > 10) {
                    dx = dx / distance * PLAYER_SPEED * 0.02;
                    dy = dy / distance * PLAYER_SPEED * 0.02;
                    player2.move(dx, dy, 0);
                }
            }

            // Random swing
            if (Math.random() > 0.95 && ballX > 0) {
                player2.swing();
            }
        }
    }

    private void checkScore() {
        double ballX = ball.getTranslateX();

        // Score conditions
        if (ballX > TABLE_WIDTH/2 + 100) {
            player1Score.set(player1Score.get() + 1);
            player1.celebrate();
            resetBall();
            checkGameOver();
        } else if (ballX < -TABLE_WIDTH/2 - 100) {
            player2Score.set(player2Score.get() + 1);
            player2.celebrate();
            resetBall();
            checkGameOver();
        }
    }

    private void checkGameOver() {
        if (player1Score.get() >= WINNING_SCORE) {
            gameState = GameState.GAME_OVER;
            statusLabel.setText("PLAYER 1 WINS!");
            triggerVictoryEffects(player1);
        } else if (player2Score.get() >= WINNING_SCORE) {
            gameState = GameState.GAME_OVER;
            statusLabel.setText("PLAYER 2 WINS!");
            triggerVictoryEffects(player2);
        }
    }

    private void triggerVictoryEffects(Player3D winner) {
        // Confetti effect
        for (int i = 0; i < 50; i++) {
            Box confetti = new Box(5, 5, 5);
            confetti.setMaterial(new PhongMaterial(
                    Color.hsb(Math.random() * 360, 1, 1)
            ));

            confetti.setTranslateX(winner.getX() + (Math.random() - 0.5) * 200);
            confetti.setTranslateY(winner.getY() - 200);
            confetti.setTranslateZ(winner.getZ() + (Math.random() - 0.5) * 200);

            root3D.getChildren().add(confetti);

            // Animate confetti
            Timeline fall = new Timeline(
                    new KeyFrame(Duration.ZERO,
                            new KeyValue(confetti.translateYProperty(), confetti.getTranslateY())
                    ),
                    new KeyFrame(Duration.seconds(2),
                            new KeyValue(confetti.translateYProperty(), 100),
                            new KeyValue(confetti.rotateProperty(), 360 * 5)
                    )
            );

            fall.setOnFinished(e -> root3D.getChildren().remove(confetti));
            fall.setDelay(Duration.seconds(Math.random()));
            fall.play();
        }
    }

    private void resetBall() {
        ball.setTranslateX(0);
        ball.setTranslateY(-TABLE_HEIGHT/2 - 50);
        ball.setTranslateZ(0);

        // Random serve direction
        ballVelocityX = (Math.random() > 0.5 ? BALL_SPEED : -BALL_SPEED) * 0.5;
        ballVelocityY = -BALL_SPEED * 0.7;
        ballVelocityZ = (Math.random() - 0.5) * BALL_SPEED * 0.3;
        ballSpin = 0;
    }

    private void resetGame() {
        player1Score.set(0);
        player2Score.set(0);
        gameState = GameState.START;
        statusLabel.setText("PRESS SPACE TO START");
        resetBall();

        // Reset player positions
        player1.move(-TABLE_WIDTH/2 + 100 - player1.getX(), -50 - player1.getY(), -player1.getZ());
        player2.move(TABLE_WIDTH/2 - 100 - player2.getX(), -50 - player2.getY(), -player2.getZ());
    }

    private void updateCamera() {
        // Smooth camera follow (optional)
        if (activeKeys.contains(KeyCode.C)) {
            double targetX = ball.getTranslateX() * 0.5;
            double targetZ = ball.getTranslateZ() - 800;

            camera.setTranslateX(camera.getTranslateX() + (targetX - camera.getTranslateX()) * 0.05);
            camera.setTranslateZ(camera.getTranslateZ() + (targetZ - camera.getTranslateZ()) * 0.05);
        }
    }

    private void startGame() {
        gameLoop.start();
        root3D.requestFocus();
    }

    public static void main(String[] args) {
        launch(args);
    }
}