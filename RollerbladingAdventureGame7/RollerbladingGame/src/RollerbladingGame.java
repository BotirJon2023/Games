import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;

public class RollerbladingGame {

    public static void main(String[] args) {
        JFrame frame = new JFrame("Rollerblading Adventure");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        try {
            GamePanel gamePanel = new GamePanel();
            frame.add(gamePanel);
            frame.pack();
            frame.setResizable(false);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            gamePanel.startGameThread();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class GamePanel extends JPanel implements Runnable, KeyListener {
        // Screen settings
        final int screenWidth = 800;
        final int screenHeight = 600;

        // Game thread
        Thread gameThread;
        final int FPS = 60;

        // Game state
        public Player player;
        public ArrayList<Obstacle> obstacles;

        private boolean isRunning;
        private long score;
        private long lastObstacleTime;
        private long obstacleSpawnDelay = 1000; // in milliseconds

        // Sound
        private Sound collisionSound;
        private Sound scoreSound;

        public GamePanel() throws IOException {
            this.setPreferredSize(new Dimension(screenWidth, screenHeight));
            this.setBackground(Color.BLACK);
            this.setDoubleBuffered(true);
            this.setFocusable(true);
            this.addKeyListener(this);

            this.player = new Player(screenWidth / 2, screenHeight - 100, 64, 64);
            this.obstacles = new ArrayList<>();
            this.score = 0;
            this.lastObstacleTime = System.currentTimeMillis();

            // Load sounds
            this.collisionSound = new Sound("collision.wav");
            this.scoreSound = new Sound("score.wav");
        }

        public void startGameThread() {
            gameThread = new Thread(this);
            isRunning = true;
            gameThread.start();
        }

        @Override
        public void run() {
            double drawInterval = 1_000_000_000 / FPS;
            double delta = 0;
            long lastTime = System.nanoTime();
            long currentTime;
            long timer = 0;
            int drawCount = 0;

            while (isRunning) {
                currentTime = System.nanoTime();
                delta += (currentTime - lastTime) / drawInterval;
                timer += (currentTime - lastTime);
                lastTime = currentTime;

                if (delta >= 1) {
                    update();
                    repaint();
                    delta--;
                    drawCount++;
                }

                if (timer >= 1_000_000_000) {
                    // System.out.println("FPS: " + drawCount);
                    drawCount = 0;
                    timer = 0;
                }
            }
        }

        public void update() {
            if (!isRunning) return;

            player.update();

            long now = System.currentTimeMillis();
            if (now - lastObstacleTime > obstacleSpawnDelay) {
                spawnObstacle();
                lastObstacleTime = now;

                if (obstacleSpawnDelay > 300) {
                    obstacleSpawnDelay -= 10;
                }
            }

            Iterator<Obstacle> iterator = obstacles.iterator();
            while (iterator.hasNext()) {
                Obstacle obstacle = iterator.next();
                obstacle.update();

                if (player.getBounds().intersects(obstacle.getBounds())) {
                    collisionSound.play();
                    System.out.println("Game Over! Score: " + score);
                    isRunning = false;
                }

                if (obstacle.getY() > screenHeight) {
                    iterator.remove();
                    score++;
                    scoreSound.play();
                }
            }
        }

        private void spawnObstacle() {
            int x = ThreadLocalRandom.current().nextInt(screenWidth - 50);
            int y = -50;
            int type = ThreadLocalRandom.current().nextInt(3);
            obstacles.add(new Obstacle(x, y, type));
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;

            player.draw(g2);

            for (Obstacle obstacle : obstacles) {
                obstacle.draw(g2);
            }

            g2.setColor(Color.WHITE);
            g2.setFont(g2.getFont().deriveFont(30F));
            g2.drawString("Score: " + score, 10, 30);

            if (!isRunning) {
                g2.setColor(new Color(0, 0, 0, 150));
                g2.fillRect(0, 0, screenWidth, screenHeight);
                g2.setColor(Color.RED);
                g2.setFont(g2.getFont().deriveFont(60F));
                g2.drawString("Game Over", screenWidth / 2 - 150, screenHeight / 2 - 30);
            }

            g2.dispose();
        }

        @Override
        public void keyPressed(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                player.setDirection(Player.Direction.LEFT);
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                player.setDirection(Player.Direction.RIGHT);
            }
        }

        @Override
        public void keyReleased(KeyEvent e) {
            if (e.getKeyCode() == KeyEvent.VK_LEFT && player.getDirection() == Player.Direction.LEFT) {
                player.setDirection(Player.Direction.NONE);
            }
            if (e.getKeyCode() == KeyEvent.VK_RIGHT && player.getDirection() == Player.Direction.RIGHT) {
                player.setDirection(Player.Direction.NONE);
            }
        }

        @Override
        public void keyTyped(KeyEvent e) {}
    }

    static class Player {
        private int x, y, width, height;
        private int speed = 5;
        private Direction direction = Direction.NONE;

        private BufferedImage[] animationFrames;
        private int currentFrame = 0;
        private long lastFrameUpdate;
        private final long frameDuration = 100; // ms per frame

        public enum Direction {
            LEFT, RIGHT, NONE
        }

        public Player(int x, int y, int width, int height) throws IOException {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            loadAnimationFrames();
        }

        private void loadAnimationFrames() throws IOException {
            animationFrames = new BufferedImage[4];
            try {
                animationFrames[0] = ImageIO.read(getClass().getResource("/res/skater_frame1.png"));
                animationFrames[1] = ImageIO.read(getClass().getResource("/res/skater_frame2.png"));
                animationFrames[2] = ImageIO.read(getClass().getResource("/res/skater_frame3.png"));
                animationFrames[3] = ImageIO.read(getClass().getResource("/res/skater_frame4.png"));
            } catch (IOException e) {
                System.err.println("Error loading player animation frames.");
                e.printStackTrace();
            }
        }

        public void update() {
            if (direction == Direction.LEFT) {
                x -= speed;
            } else if (direction == Direction.RIGHT) {
                x += speed;
            }

            if (x < 0) x = 0;
            if (x > 800 - width) x = 800 - width;

            if (System.currentTimeMillis() - lastFrameUpdate > frameDuration) {
                currentFrame = (currentFrame + 1) % animationFrames.length;
                lastFrameUpdate = System.currentTimeMillis();
            }
        }

        public void draw(Graphics2D g2) {
            if (animationFrames != null && animationFrames.length > 0) {
                g2.drawImage(animationFrames[currentFrame], x, y, width, height, null);
            } else {
                g2.fillRect(x, y, width, height);
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public void setDirection(Direction dir) {
            this.direction = dir;
        }

        public Direction getDirection() {
            return this.direction;
        }
    }

    static class Obstacle {
        private int x, y, width, height, speed;
        private BufferedImage image;

        public Obstacle(int x, int y, int type) {
            this.x = x;
            this.y = y;
            this.speed = 3;

            try {
                String imagePath = "";
                switch (type) {
                    case 0:
                        imagePath = "/res/rock.png";
                        this.width = 50;
                        this.height = 50;
                        break;
                    case 1:
                        imagePath = "/res/puddle.png";
                        this.width = 70;
                        this.height = 30;
                        break;
                    case 2:
                        imagePath = "/res/cone.png";
                        this.width = 40;
                        this.height = 60;
                        break;
                    default:
                        imagePath = "/res/rock.png";
                        this.width = 50;
                        this.height = 50;
                        break;
                }
                this.image = ImageIO.read(getClass().getResource(imagePath));
            } catch (IOException e) {
                System.err.println("Error loading obstacle image.");
                e.printStackTrace();
            }
        }

        public void update() {
            y += speed;
        }

        public void draw(Graphics2D g2) {
            if (image != null) {
                g2.drawImage(image, x, y, width, height, null);
            } else {
                g2.fillRect(x, y, width, height);
            }
        }

        public Rectangle getBounds() {
            return new Rectangle(x, y, width, height);
        }

        public int getY() {
            return y;
        }
    }

    static class Sound {
        private Clip clip;
        private URL soundURL;

        public Sound(String filename) {
            try {
                soundURL = getClass().getResource("/res/" + filename);
                if (soundURL == null) {
                    File file = new File("res/" + filename);
                    if (file.exists()) {
                        soundURL = file.toURI().toURL();
                    } else {
                        System.err.println("Sound file not found: " + filename);
                        return;
                    }
                }

                AudioInputStream ais = AudioSystem.getAudioInputStream(soundURL);
                clip = AudioSystem.getClip();
                clip.open(ais);
            } catch (Exception e) {
                System.err.println("Error setting up sound: " + filename);
                e.printStackTrace();
            }
        }

        public void play() {
            if (clip != null) {
                try {
                    clip.stop();
                    clip.setFramePosition(0);
                    clip.start();
                } catch (Exception e) {
                    System.err.println("Error playing sound.");
                    e.printStackTrace();
                }
            }
        }
    }
}