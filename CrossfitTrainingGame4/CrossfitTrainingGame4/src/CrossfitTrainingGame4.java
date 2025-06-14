import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class CrossfitTrainingGame4 extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int GROUND_LEVEL = 500;
    private static final int WORKOUT_DURATION = 60; // seconds

    private GamePanel gamePanel;
    private Timer gameTimer;
    private int score = 0;
    private int timeRemaining = WORKOUT_DURATION;
    private Workout currentWorkout;
    private Athlete athlete;
    private ArrayList<Workout> workouts = new ArrayList<>();
    private Random random = new Random();
    private boolean gameRunning = false;

    public CrossfitTrainingGame4() {
        super("Crossfit Training Game");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);

        initializeWorkouts();
        athlete = new Athlete();

        gamePanel = new GamePanel();
        add(gamePanel);

        addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (gameRunning) {
                    handleKeyPress(e.getKeyCode());
                } else if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    startGame();
                }
            }
        });

        setupTimer();
    }

    private void initializeWorkouts() {
        workouts.add(new Deadlift());
        workouts.add(new Burpee());
        workouts.add(new DoubleUnders());
        workouts.add(new BoxJump());
        workouts.add(new WallBall());
    }

    private void setupTimer() {
        gameTimer = new Timer(1000, e -> {
            if (gameRunning) {
                timeRemaining--;
                if (timeRemaining <= 0) {
                    endGame();
                }
                gamePanel.repaint();
            }
        });
    }

    private void startGame() {
        score = 0;
        timeRemaining = WORKOUT_DURATION;
        gameRunning = true;
        selectRandomWorkout();
        gameTimer.start();
        gamePanel.repaint();
    }

    private void endGame() {
        gameRunning = false;
        gameTimer.stop();
        JOptionPane.showMessageDialog(this, "Workout Complete!\nFinal Score: " + score);
    }

    private void selectRandomWorkout() {
        if (currentWorkout != null) {
            currentWorkout.reset();
        }
        currentWorkout = workouts.get(random.nextInt(workouts.size()));
        currentWorkout.start();
    }

    private void handleKeyPress(int keyCode) {
        if (currentWorkout != null) {
            boolean success = currentWorkout.handleInput(keyCode);
            if (success) {
                score += currentWorkout.getPoints();
                athlete.performAction(currentWorkout.getAnimation());
                selectRandomWorkout();
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CrossfitTrainingGame4 game = new CrossfitTrainingGame4();
            game.setVisible(true);
        });
    }

    class GamePanel extends JPanel {
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            // Draw background
            g.setColor(new Color(135, 206, 235));
            g.fillRect(0, 0, WIDTH, GROUND_LEVEL);
            g.setColor(new Color(34, 139, 34));
            g.fillRect(0, GROUND_LEVEL, WIDTH, HEIGHT - GROUND_LEVEL);

            // Draw athlete
            athlete.draw(g);

            // Draw current workout
            if (currentWorkout != null) {
                currentWorkout.draw(g);
            }

            // Draw score and timer
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.BOLD, 24));
            g.drawString("Score: " + score, 20, 30);
            g.drawString("Time: " + timeRemaining, WIDTH - 120, 30);

            // Draw instructions if game not running
            if (!gameRunning) {
                g.setFont(new Font("Arial", Font.BOLD, 36));
                g.drawString("CROSSFIT TRAINING GAME", WIDTH/2 - 200, HEIGHT/2 - 50);
                g.setFont(new Font("Arial", Font.PLAIN, 24));
                g.drawString("Press SPACE to start workout", WIDTH/2 - 150, HEIGHT/2 + 20);
            }
        }
    }

    abstract class Workout {
        protected String name;
        protected int points;
        protected String animation;
        protected int x, y;
        protected boolean active = false;

        public abstract boolean handleInput(int keyCode);
        public abstract void draw(Graphics g);

        public void start() {
            active = true;
            x = WIDTH / 2;
            y = GROUND_LEVEL - 100;
        }

        public void reset() {
            active = false;
        }

        public String getName() {
            return name;
        }

        public int getPoints() {
            return points;
        }

        public String getAnimation() {
            return animation;
        }
    }

    class Deadlift extends Workout {
        private int barbellHeight = 20;
        private boolean lifted = false;

        public Deadlift() {
            name = "Deadlift";
            points = 10;
            animation = "deadlift";
        }

        @Override
        public boolean handleInput(int keyCode) {
            if (keyCode == KeyEvent.VK_DOWN && !lifted) {
                barbellHeight = 60;
                lifted = true;
                return false;
            } else if (keyCode == KeyEvent.VK_UP && lifted) {
                barbellHeight = 20;
                lifted = false;
                return true;
            }
            return false;
        }

        @Override
        public void draw(Graphics g) {
            // Draw barbell
            g.setColor(Color.GRAY);
            g.fillRect(x - 100, y + barbellHeight, 200, 10);
            // Draw plates
            g.setColor(Color.BLUE);
            g.fillOval(x - 110, y + barbellHeight - 15, 30, 40);
            g.fillOval(x + 80, y + barbellHeight - 15, 30, 40);

            // Draw instructions
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("DEADLIFT: Press DOWN then UP", x - 100, y - 10);
        }

        @Override
        public void reset() {
            super.reset();
            barbellHeight = 20;
            lifted = false;
        }
    }

    class Burpee extends Workout {
        private int step = 0;
        private int animationCounter = 0;

        public Burpee() {
            name = "Burpee";
            points = 15;
            animation = "burpee";
        }

        @Override
        public boolean handleInput(int keyCode) {
            if (step == 0 && keyCode == KeyEvent.VK_DOWN) {
                step = 1;
                return false;
            } else if (step == 1 && keyCode == KeyEvent.VK_RIGHT) {
                step = 2;
                return false;
            } else if (step == 2 && keyCode == KeyEvent.VK_UP) {
                return true;
            }
            return false;
        }

        @Override
        public void draw(Graphics g) {
            // Draw burpee steps visualization
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("BURPEE:", x - 100, y - 10);

            if (step == 0) {
                g.drawString("1. Press DOWN to squat", x - 80, y + 20);
            } else if (step == 1) {
                g.drawString("2. Press RIGHT to pushup", x - 80, y + 20);
            } else if (step == 2) {
                g.drawString("3. Press UP to jump", x - 80, y + 20);
            }

            // Draw animation
            animationCounter++;
            if (animationCounter > 30) animationCounter = 0;

            if (step == 0) {
                drawStanding(g);
            } else if (step == 1) {
                if (animationCounter < 15) {
                    drawSquat(g);
                } else {
                    drawPushup(g);
                }
            } else {
                drawJump(g);
            }
        }

        private void drawStanding(Graphics g) {
            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 50, 30, 30); // head
            g.fillRect(x - 20, y - 20, 40, 40); // body
            g.fillRect(x - 30, y + 20, 15, 40); // left leg
            g.fillRect(x + 15, y + 20, 15, 40); // right leg
            g.fillRect(x - 40, y - 10, 20, 15); // left arm
            g.fillRect(x + 20, y - 10, 20, 15); // right arm
        }

        private void drawSquat(Graphics g) {
            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 30, 30, 30); // head
            g.fillRect(x - 20, y, 40, 20); // body
            g.fillRect(x - 30, y + 20, 15, 30); // left leg
            g.fillRect(x + 15, y + 20, 15, 30); // right leg
            g.fillRect(x - 40, y, 20, 15); // left arm
            g.fillRect(x + 20, y, 20, 15); // right arm
        }

        private void drawPushup(Graphics g) {
            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y + 10, 30, 30); // head
            g.fillRect(x - 20, y + 40, 40, 20); // body
            g.fillRect(x - 30, y + 60, 15, 20); // left leg
            g.fillRect(x + 15, y + 60, 15, 20); // right leg
            g.fillRect(x - 40, y + 30, 20, 15); // left arm
            g.fillRect(x + 20, y + 30, 20, 15); // right arm
        }

        private void drawJump(Graphics g) {
            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 70, 30, 30); // head
            g.fillRect(x - 20, y - 40, 40, 40); // body
            g.fillRect(x - 30, y, 15, 30); // left leg
            g.fillRect(x + 15, y, 15, 30); // right leg
            g.fillRect(x - 50, y - 30, 30, 15); // left arm
            g.fillRect(x + 20, y - 30, 30, 15); // right arm
        }

        @Override
        public void reset() {
            super.reset();
            step = 0;
            animationCounter = 0;
        }
    }

    class DoubleUnders extends Workout {
        private int ropeAngle = 0;
        private int rotationSpeed = 10;
        private int jumpState = 0; // 0=ground, 1=up, 2=down

        public DoubleUnders() {
            name = "Double Unders";
            points = 20;
            animation = "jump_rope";
        }

        @Override
        public boolean handleInput(int keyCode) {
            if (keyCode == KeyEvent.VK_SPACE) {
                if (jumpState == 0) {
                    jumpState = 1;
                    return false;
                } else if (jumpState == 1) {
                    jumpState = 2;
                    return false;
                } else if (jumpState == 2) {
                    jumpState = 0;
                    return true;
                }
            }
            return false;
        }

        @Override
        public void draw(Graphics g) {
            // Update rope animation
            ropeAngle = (ropeAngle + rotationSpeed) % 360;

            // Draw instructions
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("DOUBLE UNDERS: Press SPACE 3 times", x - 150, y - 30);

            // Draw athlete
            int yOffset = 0;
            if (jumpState == 1) yOffset = -40;
            else if (jumpState == 2) yOffset = -20;

            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 50 + yOffset, 30, 30); // head
            g.fillRect(x - 20, y - 20 + yOffset, 40, 40); // body

            // Draw jump rope
            g.setColor(Color.BLACK);
            int ropeX1 = x + (int)(50 * Math.cos(Math.toRadians(ropeAngle)));
            int ropeY1 = y + (int)(50 * Math.sin(Math.toRadians(ropeAngle)));
            int ropeX2 = x + (int)(50 * Math.cos(Math.toRadians(ropeAngle + 180)));
            int ropeY2 = y + (int)(50 * Math.sin(Math.toRadians(ropeAngle + 180)));

            g.drawLine(x, y + yOffset, ropeX1, ropeY1);
            g.drawLine(x, y + yOffset, ropeX2, ropeY2);

            // Draw hands
            g.setColor(new Color(200, 150, 0));
            g.fillRect(x - 30, y - 10 + yOffset, 10, 10);
            g.fillRect(x + 20, y - 10 + yOffset, 10, 10);
        }

        @Override
        public void reset() {
            super.reset();
            ropeAngle = 0;
            jumpState = 0;
        }
    }

    class BoxJump extends Workout {
        private int boxHeight = 60;
        private int jumpProgress = 0;
        private boolean jumping = false;

        public BoxJump() {
            name = "Box Jump";
            points = 15;
            animation = "box_jump";
        }

        @Override
        public boolean handleInput(int keyCode) {
            if (keyCode == KeyEvent.VK_SPACE && !jumping) {
                jumping = true;
                new Thread(() -> {
                    for (int i = 0; i <= 100; i += 5) {
                        jumpProgress = i;
                        gamePanel.repaint();
                        try {
                            Thread.sleep(30);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    jumping = false;
                    jumpProgress = 0;
                    gamePanel.repaint();
                }).start();
                return true;
            }
            return false;
        }

        @Override
        public void draw(Graphics g) {
            // Draw box
            g.setColor(new Color(139, 69, 19));
            g.fillRect(x - 60, GROUND_LEVEL - boxHeight, 120, boxHeight);

            // Draw athlete
            int jumpY = (int)(GROUND_LEVEL - (boxHeight * jumpProgress / 100.0));

            g.setColor(new Color(200, 150, 0));
            if (jumpProgress < 50) {
                // Jumping up
                g.fillOval(x - 15, jumpY - 50, 30, 30); // head
                g.fillRect(x - 20, jumpY - 20, 40, 40); // body
                g.fillRect(x - 30, jumpY + 20, 15, 30); // left leg
                g.fillRect(x + 15, jumpY + 20, 15, 30); // right leg
                g.fillRect(x - 40, jumpY - 10, 20, 15); // left arm
                g.fillRect(x + 20, jumpY - 10, 20, 15); // right arm
            } else {
                // On top of box
                g.fillOval(x - 15, jumpY - 30, 30, 30); // head
                g.fillRect(x - 20, jumpY, 40, 30); // body
                g.fillRect(x - 30, jumpY + 30, 15, 20); // left leg
                g.fillRect(x + 15, jumpY + 30, 15, 20); // right leg
                g.fillRect(x - 40, jumpY + 10, 20, 15); // left arm
                g.fillRect(x + 20, jumpY + 10, 20, 15); // right arm
            }

            // Draw instructions
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("BOX JUMP: Press SPACE", x - 60, GROUND_LEVEL - boxHeight - 10);
        }

        @Override
        public void reset() {
            super.reset();
            jumpProgress = 0;
            jumping = false;
        }
    }

    class WallBall extends Workout {
        private int ballY = GROUND_LEVEL - 100;
        private boolean ballThrown = false;
        private int targetHeight = 200;

        public WallBall() {
            name = "Wall Ball";
            points = 12;
            animation = "wall_ball";
        }

        @Override
        public boolean handleInput(int keyCode) {
            if (keyCode == KeyEvent.VK_UP && !ballThrown) {
                ballThrown = true;
                new Thread(() -> {
                    // Throw ball up
                    for (int i = 0; i < targetHeight; i += 5) {
                        ballY = GROUND_LEVEL - 100 - i;
                        gamePanel.repaint();
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    // Ball comes down
                    for (int i = targetHeight; i >= 0; i -= 5) {
                        ballY = GROUND_LEVEL - 100 - i;
                        gamePanel.repaint();
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }

                    ballThrown = false;
                    gamePanel.repaint();
                }).start();
                return true;
            }
            return false;
        }

        @Override
        public void draw(Graphics g) {
            // Draw wall
            g.setColor(Color.DARK_GRAY);
            g.fillRect(WIDTH - 50, GROUND_LEVEL - targetHeight - 50, 30, targetHeight + 100);

            // Draw target
            g.setColor(Color.RED);
            g.fillOval(WIDTH - 40, GROUND_LEVEL - targetHeight - 30, 20, 20);

            // Draw ball
            g.setColor(Color.BLUE);
            g.fillOval(x - 15, ballY, 30, 30);

            // Draw athlete
            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, GROUND_LEVEL - 100, 30, 30); // head
            g.fillRect(x - 20, GROUND_LEVEL - 70, 40, 40); // body

            // Arms position based on ball state
            if (ballThrown) {
                g.fillRect(x - 40, GROUND_LEVEL - 60, 20, 15); // left arm
                g.fillRect(x + 20, GROUND_LEVEL - 60, 20, 15); // right arm
            } else {
                g.fillRect(x - 40, GROUND_LEVEL - 80, 20, 15); // left arm
                g.fillRect(x + 20, GROUND_LEVEL - 80, 20, 15); // right arm
            }

            g.fillRect(x - 30, GROUND_LEVEL - 30, 15, 30); // left leg
            g.fillRect(x + 15, GROUND_LEVEL - 30, 15, 30); // right leg

            // Draw instructions
            g.setColor(Color.BLACK);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString("WALL BALL: Press UP to throw", x - 80, GROUND_LEVEL - 120);
        }

        @Override
        public void reset() {
            super.reset();
            ballY = GROUND_LEVEL - 100;
            ballThrown = false;
        }
    }

    class Athlete {
        private String currentAction = "idle";
        private int animationFrame = 0;
        private int x = 100;
        private int y = GROUND_LEVEL - 100;
        private ScheduledExecutorService animationExecutor;

        public void performAction(String action) {
            currentAction = action;
            animationFrame = 0;

            if (animationExecutor != null) {
                animationExecutor.shutdownNow();
            }

            animationExecutor = Executors.newSingleThreadScheduledExecutor();
            animationExecutor.scheduleAtFixedRate(() -> {
                animationFrame++;
                gamePanel.repaint();
                if (animationFrame > 10) {
                    currentAction = "idle";
                    animationExecutor.shutdown();
                }
            }, 0, 100, TimeUnit.MILLISECONDS);
        }

        public void draw(Graphics g) {
            if (currentAction.equals("idle")) {
                drawIdle(g);
            } else if (currentAction.equals("deadlift")) {
                drawDeadlift(g);
            } else if (currentAction.equals("burpee")) {
                drawBurpee(g);
            } else if (currentAction.equals("jump_rope")) {
                drawJumpRope(g);
            } else if (currentAction.equals("box_jump")) {
                drawBoxJump(g);
            } else if (currentAction.equals("wall_ball")) {
                drawWallBall(g);
            }
        }

        private void drawIdle(Graphics g) {
            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 50, 30, 30); // head
            g.fillRect(x - 20, y - 20, 40, 40); // body
            g.fillRect(x - 30, y + 20, 15, 30); // left leg
            g.fillRect(x + 15, y + 20, 15, 30); // right leg
            g.fillRect(x - 40, y - 10, 20, 15); // left arm
            g.fillRect(x + 20, y - 10, 20, 15); // right arm
        }

        private void drawDeadlift(Graphics g) {
            int liftHeight = Math.min(animationFrame * 3, 30);

            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 50 + liftHeight, 30, 30); // head
            g.fillRect(x - 20, y - 20 + liftHeight, 40, 40); // body

            // Arms holding barbell
            g.fillRect(x - 40, y - 10 + liftHeight, 20, 10);
            g.fillRect(x + 20, y - 10 + liftHeight, 20, 10);

            // Legs slightly bent
            g.fillRect(x - 30, y + 20 + liftHeight, 15, 30 - liftHeight/2);
            g.fillRect(x + 15, y + 20 + liftHeight, 15, 30 - liftHeight/2);

            // Draw barbell
            g.setColor(Color.GRAY);
            g.fillRect(x - 50, y + liftHeight, 100, 5);
            g.setColor(Color.BLUE);
            g.fillOval(x - 55, y - 5 + liftHeight, 15, 15);
            g.fillOval(x + 40, y - 5 + liftHeight, 15, 15);
        }

        private void drawBurpee(Graphics g) {
            int frame = animationFrame % 10;
            int squatHeight = frame < 5 ? frame * 5 : (10 - frame) * 5;

            g.setColor(new Color(200, 150, 0));

            if (frame < 3 || frame > 7) {
                // Standing or squatting
                g.fillOval(x - 15, y - 50 + squatHeight, 30, 30); // head
                g.fillRect(x - 20, y - 20 + squatHeight, 40, 40); // body
                g.fillRect(x - 30, y + 20 + squatHeight, 15, 30 - squatHeight/5);
                g.fillRect(x + 15, y + 20 + squatHeight, 15, 30 - squatHeight/5);
                g.fillRect(x - 40, y - 10 + squatHeight, 20, 15);
                g.fillRect(x + 20, y - 10 + squatHeight, 20, 15);
            } else {
                // Pushup position
                g.fillOval(x - 15, y - 20 + squatHeight, 30, 30); // head
                g.fillRect(x - 20, y + 10 + squatHeight, 40, 30); // body
                g.fillRect(x - 30, y + 40 + squatHeight, 15, 20);
                g.fillRect(x + 15, y + 40 + squatHeight, 15, 20);
                g.fillRect(x - 40, y + 20 + squatHeight, 20, 15);
                g.fillRect(x + 20, y + 20 + squatHeight, 20, 15);
            }
        }

        private void drawJumpRope(Graphics g) {
            int frame = animationFrame % 6;
            int jumpHeight = frame < 3 ? frame * 5 : (6 - frame) * 5;

            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 50 - jumpHeight, 30, 30); // head
            g.fillRect(x - 20, y - 20 - jumpHeight, 40, 40); // body
            g.fillRect(x - 30, y + 20 - jumpHeight, 15, 30);
            g.fillRect(x + 15, y + 20 - jumpHeight, 15, 30);

            // Draw rope
            g.setColor(Color.BLACK);
            int angle = (animationFrame * 60) % 360;
            int ropeX1 = x + (int)(40 * Math.cos(Math.toRadians(angle)));
            int ropeY1 = y + (int)(40 * Math.sin(Math.toRadians(angle))) - jumpHeight;
            int ropeX2 = x + (int)(40 * Math.cos(Math.toRadians(angle + 180)));
            int ropeY2 = y + (int)(40 * Math.sin(Math.toRadians(angle + 180))) - jumpHeight;

            g.drawLine(x, y - jumpHeight, ropeX1, ropeY1);
            g.drawLine(x, y - jumpHeight, ropeX2, ropeY2);

            // Hands holding rope
            g.setColor(new Color(200, 150, 0));
            g.fillRect(ropeX1 - 5, ropeY1 - 5, 10, 10);
            g.fillRect(ropeX2 - 5, ropeY2 - 5, 10, 10);
        }

        private void drawBoxJump(Graphics g) {
            int frame = Math.min(animationFrame, 5);
            int jumpHeight = frame * 10;

            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 50 - jumpHeight, 30, 30); // head
            g.fillRect(x - 20, y - 20 - jumpHeight, 40, 40); // body

            if (frame < 3) {
                // Jumping up
                g.fillRect(x - 30, y + 20 - jumpHeight, 15, 30);
                g.fillRect(x + 15, y + 20 - jumpHeight, 15, 30);
                g.fillRect(x - 40, y - 10 - jumpHeight, 20, 15);
                g.fillRect(x + 20, y - 10 - jumpHeight, 20, 15);
            } else {
                // Landing on box
                g.fillRect(x - 30, y + 20 - jumpHeight, 15, 20);
                g.fillRect(x + 15, y + 20 - jumpHeight, 15, 20);
                g.fillRect(x - 40, y + 10 - jumpHeight, 20, 15);
                g.fillRect(x + 20, y + 10 - jumpHeight, 20, 15);
            }
        }

        private void drawWallBall(Graphics g) {
            int frame = animationFrame % 8;
            int throwHeight = frame < 4 ? frame * 5 : (8 - frame) * 5;

            g.setColor(new Color(200, 150, 0));
            g.fillOval(x - 15, y - 50, 30, 30); // head
            g.fillRect(x - 20, y - 20, 40, 40); // body
            g.fillRect(x - 30, y + 20, 15, 30); // left leg
            g.fillRect(x + 15, y + 20, 15, 30); // right leg

            // Arms throwing
            g.fillRect(x - 40, y - 20 + throwHeight, 20, 15);
            g.fillRect(x + 20, y - 20 + throwHeight, 20, 15);

            // Draw ball
            if (frame < 4) {
                g.setColor(Color.BLUE);
                g.fillOval(x - 10, y - 30 + throwHeight * 2, 20, 20);
            }
        }
    }
}