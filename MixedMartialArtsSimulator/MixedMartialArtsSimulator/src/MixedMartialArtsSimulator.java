import javafx.application.Application;
import javafx.stage.Stage;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.animation.AnimationTimer;
import javafx.scene.control.TextArea;
import javafx.scene.layout.VBox;
import java.util.Random;

public class MixedMartialArtsSimulator extends Application {
    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 600;
    private static final int CANVAS_WIDTH = 600;
    private static final int CANVAS_HEIGHT = 400;
    private static final int FIGHTER_SIZE = 50;
    private static final int ARENA_WIDTH = CANVAS_WIDTH - 100;
    private static final int ARENA_HEIGHT = CANVAS_HEIGHT - 100;
    private static final double ATTACK_COOLDOWN = 1.0; // Seconds
    private static final int MAX_LOG_LINES = 10;

    private Fighter fighter1;
    private Fighter fighter2;
    private TextArea fightLog;
    private Random random = new Random();
    private long lastAttackTime1 = 0;
    private long lastAttackTime2 = 0;
    private boolean fightOver = false;
    private String winner = null;

    // Animation variables
    private double fighter1X = 100;
    private double fighter1Y = CANVAS_HEIGHT / 2;
    private double fighter2X = CANVAS_WIDTH - 100;
    private double fighter2Y = CANVAS_HEIGHT / 2;
    private boolean fighter1Attacking = false;
    private boolean fighter2Attacking = false;
    private double attackAnimationProgress = 0;
    private boolean attackDirectionForward = true;

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        // Initialize fighters
        fighter1 = new Fighter("Fighter 1", 100, 10, 5, Color.RED);
        fighter2 = new Fighter("Fighter 2", 100, 8, 6, Color.BLUE);

        // Set up the UI
        VBox root = new VBox();
        Canvas canvas = new Canvas(CANVAS_WIDTH, CANVAS_HEIGHT);
        fightLog = new TextArea();
        fightLog.setEditable(false);
        fightLog.setPrefHeight(200);
        fightLog.setFont(Font.font("Monospace", 12));
        root.getChildren().addAll(canvas, fightLog);

        Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
        primaryStage.setTitle("MMA Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Start the animation loop
        GraphicsContext gc = canvas.getGraphicsContext2D();
        new AnimationTimer() {
            @Override
            public void handle(long currentNanoTime) {
                update(currentNanoTime);
                render(gc);
            }
        }.start();

        logMessage("Fight started between " + fighter1.getName() + " and " + fighter2.getName() + "!");
    }

    private void update(long currentNanoTime) {
        if (fightOver) return;

        // Convert nanoTime to seconds
        double currentTime = currentNanoTime / 1_000_000_000.0;

        // Update fighter positions (simple AI to move towards opponent)
        updateFighterPosition(fighter1, fighter2X, fighter2Y);
        updateFighterPosition(fighter2, fighter1X, fighter1Y);

        // Handle attacks
        if (currentTime - lastAttackTime1 / 1_000_000_000.0 >= ATTACK_COOLDOWN && !fighter1Attacking) {
            if (isInRange(fighter1X, fighter1Y, fighter2X, fighter2Y)) {
                fighter1Attacking = true;
                attackAnimationProgress = 0;
                lastAttackTime1 = currentNanoTime;
            }
        }

        if (currentTime - lastAttackTime2 / 1_000_000_000.0 >= ATTACK_COOLDOWN && !fighter2Attacking) {
            if (isInRange(fighter2X, fighter2Y, fighter1X, fighter1Y)) {
                fighter2Attacking = true;
                attackAnimationProgress = 0;
                lastAttackTime2 = currentNanoTime;
            }
        }

        // Update attack animations
        if (fighter1Attacking || fighter2Attacking) {
            attackAnimationProgress += 0.05;
            if (attackAnimationProgress >= 1.0) {
                if (attackDirectionForward) {
                    attackDirectionForward = false;
                } else {
                    // Complete the attack
                    if (fighter1Attacking) {
                        performAttack(fighter1, fighter2);
                        fighter1Attacking = false;
                    }
                    if (fighter2Attacking) {
                        performAttack(fighter2, fighter1);
                        fighter2Attacking = false;
                    }
                    attackDirectionForward = true;
                    attackAnimationProgress = 0;
                }
            }
        }

        // Check for fight end
        if (fighter1.getHealth() <= 0 || fighter2.getHealth() <= 0) {
            fightOver = true;
            winner = fighter1.getHealth() > 0 ? fighter1.getName() : fighter2.getName();
            logMessage("Fight over! " + winner + " wins!");
        }
    }

    private void updateFighterPosition(Fighter fighter, double targetX, double targetY) {
        double dx = targetX - (fighter == fighter1 ? fighter1X : fighter2X);
        double dy = targetY - (fighter == fighter1 ? fighter1Y : fighter2Y);
        double distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > FIGHTER_SIZE) {
            double speed = fighter.getSpeed();
            double moveX = (dx / distance) * speed * 0.1;
            double moveY = (dy / distance) * speed * 0.1;

            if (fighter == fighter1) {
                fighter1X += moveX;
                fighter1Y += moveY;
                // Keep within arena bounds
                fighter1X = Math.max(50, Math.min(fighter1X, CANVAS_WIDTH - 50));
                fighter1Y = Math.max(50, Math.min(fighter1Y, CANVAS_HEIGHT - 50));
            } else {
                fighter2X += moveX;
                fighter2Y += moveY;
                fighter2X = Math.max(50, Math.min(fighter2X, CANVAS_WIDTH - 50));
                fighter2Y = Math.max(50, Math.min(fighter2Y, CANVAS_HEIGHT - 50));
            }
        }
    }

    private boolean isInRange(double x1, double y1, double x2, double y2) {
        double distance = Math.sqrt(Math.pow(x2 - x1, 2) + Math.pow(y2 - y1, 2));
        return distance <= FIGHTER_SIZE * 1.5;
    }

    private void performAttack(Fighter attacker, Fighter defender) {
        int damage = calculateDamage(attacker.getStrength());
        defender.takeDamage(damage);
        logMessage(attacker.getName() + " attacks " + defender.getName() + " for " + damage + " damage!");
        logMessage(defender.getName() + " health: " + defender.getHealth());
    }

    private int calculateDamage(int strength) {
        return strength + random.nextInt(5);
    }

    private void render(GraphicsContext gc) {
        // Clear canvas
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

        // Draw arena
        gc.setFill(Color.GRAY);
        gc.fillRect(50, 50, ARENA_WIDTH, ARENA_HEIGHT);

        // Draw fighters
        drawFighter(gc, fighter1, fighter1X, fighter1Y, fighter1Attacking);
        drawFighter(gc, fighter2, fighter2X, fighter2Y, fighter2Attacking);

        // Draw health bars
        drawHealthBar(gc, fighter1, 50, 20);
        drawHealthBar(gc, fighter2, CANVAS_WIDTH - 150, 20);

        // Draw fight result
        if (fightOver) {
            gc.setFill(Color.WHITE);
            gc.setFont(Font.font("Arial", 30));
            gc.fillText("Fight Over! " + winner + " Wins!", CANVAS_WIDTH / 2 - 150, CANVAS_HEIGHT / 2);
        }
    }

    private void drawFighter(GraphicsContext gc, Fighter fighter, double x, double y, boolean isAttacking) {
        gc.setFill(fighter.getColor());
        if (isAttacking) {
            double offset = attackDirectionForward ? attackAnimationProgress * 20 : 20 - attackAnimationProgress * 20;
            gc.fillRect(x - FIGHTER_SIZE / 2 + offset, y - FIGHTER_SIZE / 2, FIGHTER_SIZE, FIGHTER_SIZE);
        } else {
            gc.fillRect(x - FIGHTER_SIZE / 2, y - FIGHTER_SIZE / 2, FIGHTER_SIZE, FIGHTER_SIZE);
        }
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText(fighter.getName(), x - FIGHTER_SIZE / 2, y - FIGHTER_SIZE / 2 - 10);
    }

    private void drawHealthBar(GraphicsContext gc, Fighter fighter, double x, double y) {
        gc.setFill(Color.RED);
        gc.fillRect(x, y, 100, 10);
        gc.setFill(Color.GREEN);
        double healthWidth = (fighter.getHealth() / 100.0) * 100;
        gc.fillRect(x, y, healthWidth, 10);
        gc.setStroke(Color.BLACK);
        gc.strokeRect(x, y, 100, 10);
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 12));
        gc.fillText(fighter.getName() + ": " + fighter.getHealth() + "/100", x, y - 5);
    }

    private void logMessage(String message) {
        String currentText = fightLog.getText();
        String[] lines = currentText.split("\n");
        if (lines.length >= MAX_LOG_LINES) {
            StringBuilder newText = new StringBuilder();
            for (int i = 1; i < lines.length; i++) {
                newText.append(lines[i]).append("\n");
            }
            newText.append(message);
            fightLog.setText(newText.toString());
        } else {
            fightLog.appendText(message + "\n");
        }
    }

    // Fighter class to encapsulate fighter properties
    private static class Fighter {
        private String name;
        private double health;
        private int strength;
        private int speed;
        private Color color;

        public Fighter(String name, double health, int strength, int speed, Color color) {
            this.name = name;
            this.health = health;
            this.strength = strength;
            this.speed = speed;
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public double getHealth() {
            return health;
        }

        public int getStrength() {
            return strength;
        }

        public int getSpeed() {
            return speed;
        }

        public Color getColor() {
            return color;
        }

        public void takeDamage(int damage) {
            health = Math.max(0, health - damage);
        }
    }

    // Additional methods for extended functionality
    private void resetFight() {
        fighter1 = new Fighter("Fighter 1", 100, 10, 5, Color.RED);
        fighter2 = new Fighter("Fighter 2", 100, 8, 6, Color.BLUE);
        fighter1X = 100;
        fighter1Y = CANVAS_HEIGHT / 2;
        fighter2X = CANVAS_WIDTH - 100;
        fighter2Y = CANVAS_HEIGHT / 2;
        fightOver = false;
        winner = null;
        fightLog.clear();
        logMessage("New fight started between " + fighter1.getName() + " and " + fighter2.getName() + "!");
    }

    private void applySpecialMove(Fighter attacker, Fighter defender) {
        if (random.nextDouble() < 0.1) { // 10% chance for special move
            int damage = calculateDamage(attacker.getStrength() * 2);
            defender.takeDamage(damage);
            logMessage(attacker.getName() + " lands a SPECIAL MOVE on " + defender.getName() + " for " + damage + " damage!");
        }
    }

    private void updateSpecialMoves(long currentNanoTime) {
        double currentTime = currentNanoTime / 1_000_000_000.0;
        if (currentTime - lastAttackTime1 / 1_000_000_000.0 >= ATTACK_COOLDOWN * 2 && !fighter1Attacking) {
            if (isInRange(fighter1X, fighter1Y, fighter2X, fighter2Y)) {
                applySpecialMove(fighter1, fighter2);
                lastAttackTime1 = currentNanoTime;
            }
        }
        if (currentTime - lastAttackTime2 / 1_000_000_000.0 >= ATTACK_COOLDOWN * 2 && !fighter2Attacking) {
            if (isInRange(fighter2X, fighter2Y, fighter1X, fighter1Y)) {
                applySpecialMove(fighter2, fighter1);
                lastAttackTime2 = currentNanoTime;
            }
        }
    }

    // Extended rendering methods for visual effects
    private void drawBackgroundEffects(GraphicsContext gc) {
        gc.setFill(Color.DARKGRAY);
        gc.fillOval(50, 50, 20, 20);
        gc.fillOval(ARENA_WIDTH + 30, 50, 20, 20);
        gc.fillOval(50, ARENA_HEIGHT + 30, 20, 20);
        gc.fillOval(ARENA_WIDTH + 30, ARENA_HEIGHT + 30, 20, 20);
    }

    private void drawAttackEffect(GraphicsContext gc, double x, double y) {
        gc.setFill(Color.YELLOW);
        gc.fillOval(x - 10, y - 10, 20, 20);
    }

    // Extended update methods for AI and movement
    private void updateFighterAI(Fighter fighter, double targetX, double targetY) {
        if (random.nextDouble() < 0.05) { // Random movement for realism
            double newX = (fighter == fighter1 ? fighter1X : fighter2X) + (random.nextDouble() - 0.5) * 10;
            double newY = (fighter == fighter1 ? fighter1Y : fighter2Y) + (random.nextDouble() - 0.5) * 10;
            if (fighter == fighter1) {
                fighter1X = Math.max(50, Math.min(newX, CANVAS_WIDTH - 50));
                fighter1Y = Math.max(50, Math.min(newY, CANVAS_HEIGHT - 50));
            } else {
                fighter2X = Math.max(50, Math.min(newX, CANVAS_WIDTH - 50));
                fighter2Y = Math.max(50, Math.min(newY, CANVAS_HEIGHT - 50));
            }
        }
    }

    // Extended methods for fight mechanics
    private void checkStamina(Fighter fighter) {
        // Placeholder for stamina mechanics (not implemented for simplicity)
    }

    private void applyDefense(Fighter defender) {
        if (random.nextDouble() < 0.2) { // 20% chance to block
            logMessage(defender.getName() + " blocks the attack!");
            defender.takeDamage(0);
        }
    }

    // Additional rendering for UI polish
    private void drawArenaBorder(GraphicsContext gc) {
        gc.setStroke(Color.WHITE);
        gc.setLineWidth(5);
        gc.strokeRect(50, 50, ARENA_WIDTH, ARENA_HEIGHT);
    }

    private void drawFightTimer(GraphicsContext gc, long currentNanoTime) {
        double time = currentNanoTime / 1_000_000_000.0;
        gc.setFill(Color.WHITE);
        gc.setFont(Font.font("Arial", 14));
        gc.fillText("Time: " + String.format("%.1f", time) + "s", CANVAS_WIDTH - 100, CANVAS_HEIGHT - 20);
    }

    // Main update method with additional mechanics
    private void updateGameState(long currentNanoTime) {
        updateFighterAI(fighter1, fighter2X, fighter2Y);
        updateFighterAI(fighter2, fighter1X, fighter1Y);
        updateSpecialMoves(currentNanoTime);
        checkStamina(fighter1);
        checkStamina(fighter2);
    }

    // Main render method with all visual components
    private void renderAll(GraphicsContext gc, long currentNanoTime) {
        drawBackgroundEffects(gc);
        drawArenaBorder(gc);
        drawFightTimer(gc, currentNanoTime);
        render(gc); // Call original render
    }
}