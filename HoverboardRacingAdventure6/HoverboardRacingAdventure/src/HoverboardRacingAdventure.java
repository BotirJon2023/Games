import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Random;

public class HoverboardRacingAdventure extends JPanel implements ActionListener, KeyListener {

    Timer timer = new Timer(16, this);

    final int WIDTH = 1000;
    final int HEIGHT = 700;

    boolean vsComputer = true;

    class Rider {
        double x, y;
        double speed = 0;
        Color color;
        int score = 0;

        Rider(double x, double y, Color color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }

        void draw(Graphics2D g) {

            // Glow Effect
            g.setColor(new Color(color.getRed(),
                    color.getGreen(),
                    color.getBlue(), 80));

            g.fillOval((int)x - 10,
                    (int)y - 10,
                    70,
                    110);

            // Hoverboard
            g.setColor(color);
            g.fillRoundRect((int)x,
                    (int)y,
                    50,
                    100,
                    20,
                    20);

            // Highlight
            g.setColor(Color.WHITE);

            g.fillOval((int)x + 15,
                    (int)y + 15,
                    20,
                    20);
        }
    }

    Rider player1 =
            new Rider(300, 550, Color.CYAN);

    Rider player2 =
            new Rider(600, 550, Color.MAGENTA);

    boolean w, a, s, d;
    boolean up, down, left, right;

    int roadScroll = 0;
    int finishLine = 5000;

    Random random = new Random();

    Rectangle[] boosts = new Rectangle[8];

    public HoverboardRacingAdventure() {

        setPreferredSize(
                new Dimension(WIDTH, HEIGHT));

        setFocusable(true);

        addKeyListener(this);

        for (int i = 0; i < boosts.length; i++) {

            boosts[i] = new Rectangle(
                    random.nextInt(700) + 150,
                    -i * 700,
                    80,
                    30
            );
        }

        String[] options =
                {"Vs Computer", "2 Players"};

        int choice =
                JOptionPane.showOptionDialog(
                        null,
                        "Choose Game Mode",
                        "Hoverboard Racing Adventure",
                        JOptionPane.DEFAULT_OPTION,
                        JOptionPane.INFORMATION_MESSAGE,
                        null,
                        options,
                        options[0]);

        vsComputer = (choice == 0);

        timer.start();
    }

    private void updatePlayer1() {

        if (w)
            player1.speed += 0.2;

        if (s)
            player1.speed -= 0.3;

        if (a)
            player1.x -= 6;

        if (d)
            player1.x += 6;

        player1.speed *= 0.98;

        player1.speed =
                Math.max(0,
                        Math.min(12,
                                player1.speed));

        player1.score += player1.speed;
    }

    private void updatePlayer2() {

        if (vsComputer) {

            if (player2.x < player1.x)
                player2.x += 2;

            else
                player2.x -= 2;

            player2.speed = 8 +
                    random.nextDouble() * 2;

        } else {

            if (up)
                player2.speed += 0.2;

            if (down)
                player2.speed -= 0.3;

            if (left)
                player2.x -= 6;

            if (right)
                player2.x += 6;

            player2.speed *= 0.98;

            player2.speed =
                    Math.max(0,
                            Math.min(12,
                                    player2.speed));
        }

        player2.score += player2.speed;
    }

    private void updateBoosts() {

        for (Rectangle boost : boosts) {

            boost.y +=
                    (int)(10 +
                            player1.speed);

            if (boost.y > HEIGHT) {

                boost.y = -700;

                boost.x =
                        random.nextInt(700)
                                + 150;
            }

            Rectangle p1 =
                    new Rectangle(
                            (int)player1.x,
                            (int)player1.y,
                            50,
                            100);

            Rectangle p2 =
                    new Rectangle(
                            (int)player2.x,
                            (int)player2.y,
                            50,
                            100);

            if (boost.intersects(p1)) {

                player1.speed += 3;

                boost.y = -700;
            }

            if (boost.intersects(p2)) {

                player2.speed += 3;

                boost.y = -700;
            }
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        updatePlayer1();
        updatePlayer2();
        updateBoosts();

        roadScroll +=
                (int)(5 + player1.speed);

        if (player1.score >= finishLine ||
                player2.score >= finishLine) {

            timer.stop();

            String winner;

            if (player1.score >
                    player2.score)

                winner =
                        "Player 1 Wins!";

            else

                winner =
                        vsComputer
                                ? "Computer Wins!"
                                : "Player 2 Wins!";

            JOptionPane.showMessageDialog(
                    this,
                    winner);
        }

        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        super.paintComponent(g);

        Graphics2D g2 =
                (Graphics2D) g;

        // Futuristic Sky
        GradientPaint sky =
                new GradientPaint(
                        0,
                        0,
                        new Color(10,10,60),
                        0,
                        HEIGHT,
                        new Color(120,0,180));

        g2.setPaint(sky);
        g2.fillRect(0,0,WIDTH,HEIGHT);

        // Stars
        g2.setColor(Color.WHITE);

        for(int i=0;i<100;i++) {

            g2.fillOval(
                    (i*79)%WIDTH,
                    (i*37)%HEIGHT,
                    2,
                    2);
        }

        // Road
        g2.setColor(
                new Color(40,40,40));

        g2.fillRect(
                120,
                0,
                760,
                HEIGHT);

        // Neon Lines
        g2.setColor(Color.YELLOW);

        for(int y = -(roadScroll%50);
            y < HEIGHT;
            y += 50) {

            g2.fillRect(
                    WIDTH/2-5,
                    y,
                    10,
                    30);
        }

        // Side Glow
        g2.setColor(Color.CYAN);

        g2.fillRect(120,0,8,HEIGHT);

        g2.setColor(Color.MAGENTA);

        g2.fillRect(872,0,8,HEIGHT);

        // Boost pads
        for(Rectangle boost : boosts) {

            g2.setColor(Color.GREEN);

            g2.fillRoundRect(
                    boost.x,
                    boost.y,
                    boost.width,
                    boost.height,
                    15,
                    15);

            g2.setColor(Color.WHITE);

            g2.drawString(
                    "BOOST",
                    boost.x+10,
                    boost.y+20);
        }

        player1.draw(g2);
        player2.draw(g2);

        // HUD
        g2.setFont(
                new Font(
                        "Arial",
                        Font.BOLD,
                        24));

        g2.setColor(Color.WHITE);

        g2.drawString(
                "Player 1: " +
                        player1.score,
                20,
                40);

        g2.drawString(
                (vsComputer ?
                        "Computer: "
                        :
                        "Player 2: ")
                        +
                        player2.score,
                750,
                40);
    }

    @Override
    public void keyPressed(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W -> w = true;
            case KeyEvent.VK_A -> a = true;
            case KeyEvent.VK_S -> s = true;
            case KeyEvent.VK_D -> d = true;

            case KeyEvent.VK_UP -> up = true;
            case KeyEvent.VK_LEFT -> left = true;
            case KeyEvent.VK_DOWN -> down = true;
            case KeyEvent.VK_RIGHT -> right = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {

        switch (e.getKeyCode()) {

            case KeyEvent.VK_W -> w = false;
            case KeyEvent.VK_A -> a = false;
            case KeyEvent.VK_S -> s = false;
            case KeyEvent.VK_D -> d = false;

            case KeyEvent.VK_UP -> up = false;
            case KeyEvent.VK_LEFT -> left = false;
            case KeyEvent.VK_DOWN -> down = false;
            case KeyEvent.VK_RIGHT -> right = false;
        }
    }

    @Override
    public void keyTyped(KeyEvent e) {}

    public static void main(String[] args) {

        JFrame frame =
                new JFrame(
                        "Hoverboard Racing Adventure");

        frame.setDefaultCloseOperation(
                JFrame.EXIT_ON_CLOSE);

        frame.add(
                new HoverboardRacingAdventure());

        frame.pack();

        frame.setLocationRelativeTo(null);

        frame.setVisible(true);
    }
}