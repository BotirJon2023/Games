import javax.sound.sampled.*;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

public class GhostlyApparitionHorror extends JPanel implements ActionListener, KeyListener {
    private Timer timer;
    private ArrayList<Ghost> ghosts;
    private Player player;
    private Random random;
    private boolean flickering;
    private int flickerCounter;
    private int playerHealth;
    private boolean gameOver;

    public GhostlyApparitionHorror() {
        timer = new Timer(100, this);
        ghosts = new ArrayList<>();
        player = new Player(300, 250);
        random = new Random();
        flickering = false;
        flickerCounter = 0;
        playerHealth = 3;
        gameOver = false;

        for (int i = 0; i < 7; i++) {
            ghosts.add(new Ghost(random.nextInt(600), random.nextInt(500)));
        }

        timer.start();
        setFocusable(true);
        addKeyListener(this);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2d = (Graphics2D) g;

        if (flickering && flickerCounter % 2 == 0) {
            g2d.setColor(Color.BLACK);
            g2d.fillRect(0, 0, getWidth(), getHeight());
            return;
        }

        g2d.setColor(Color.BLACK);
        g2d.fillRect(0, 0, getWidth(), getHeight());

        for (Ghost ghost : ghosts) {
            ghost.draw(g2d);
        }

        player.draw(g2d);

        g2d.setColor(Color.WHITE);
        g2d.drawString("Health: " + playerHealth, 10, 20);

        if (gameOver) {
            g2d.setFont(new Font("Arial", Font.BOLD, 50));
            g2d.setColor(Color.RED);
            g2d.drawString("GAME OVER", 200, 250);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (!gameOver) {
            for (Ghost ghost : ghosts) {
                ghost.update();
                if (ghost.touches(player)) {
                    playerHealth--;
                    playSound("scare.wav");
                    if (playerHealth <= 0) {
                        gameOver = true;
                        timer.stop();
                    }
                }
            }
        }

        if (random.nextInt(100) > 95) {
            flickering = true;
            flickerCounter = 10;
        }

        if (flickering) {
            flickerCounter--;
            if (flickerCounter <= 0) {
                flickering = false;
            }
        }

        repaint();
    }

    private void playSound(String soundFile) {
        try {
            File file = new File(soundFile);
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(file);
            Clip clip = AudioSystem.getClip();
            clip.open(audioStream);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!gameOver) {
            int key = e.getKeyCode();
            if (key == KeyEvent.VK_LEFT) {
                player.move(-10, 0);
            } else if (key == KeyEvent.VK_RIGHT) {
                player.move(10, 0);
            } else if (key == KeyEvent.VK_UP) {
                player.move(0, -10);
            } else if (key == KeyEvent.VK_DOWN) {
                player.move(0, 10);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("Ghostly Apparition Horror");
        GhostlyApparitionHorror game = new GhostlyApparitionHorror();

        frame.setSize(600, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(game);
        frame.setVisible(true);
    }
}

class Ghost {
    private int x, y;
    private float transparency;
    private boolean fadingIn;
    private Random random;

    public Ghost(int x, int y) {
        this.x = x;
        this.y = y;
        this.transparency = 0.1f;
        this.fadingIn = true;
        this.random = new Random();
    }

    public void update() {
        if (fadingIn) {
            transparency += 0.02f;
            if (transparency >= 1.0f) {
                fadingIn = false;
            }
        } else {
            transparency -= 0.02f;
            if (transparency <= 0.1f) {
                x = random.nextInt(600);
                y = random.nextInt(500);
                transparency = 0.1f;
                fadingIn = true;
            }
        }
    }

    public boolean touches(Player player) {
        return Math.abs(player.getX() - x) < 40 && Math.abs(player.getY() - y) < 50;
    }

    public void draw(Graphics2D g2d) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, transparency));
        g2d.setColor(Color.WHITE);
        g2d.fillOval(x, y, 80, 100);
    }
}

class Player {
    private int x, y;

    public Player(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void move(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public void draw(Graphics2D g2d) {
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.setColor(Color.RED);
        g2d.fillRect(x, y, 30, 30);
    }
}