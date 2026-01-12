import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class OlympicSportSimulator extends JPanel implements ActionListener {

    private static final int WIDTH  = 1280;
    private static final int HEIGHT = 720;
    private static final int FPS    = 60;
    private static final int MENU   = 0;
    private static final int RUNNING_100M = 1;
    private static final int LONG_JUMP   = 2;
    private static final int JAVELIN     = 3;
    private static final int ARCHERY     = 4;
    private static final int WEIGHTLIFT  = 5;
    private static final int RESULTS     = 6;
    private static final int CREDITS     = 7;

    private int gameState = MENU;
    private Timer timer;
    private Random rand = ThreadLocalRandom.current();

    // Fonts
    private Font titleFont    = new Font("Segoe UI", Font.BOLD, 72);
    private Font subtitleFont = new Font("Segoe UI", Font.PLAIN, 36);
    private Font buttonFont   = new Font("Segoe UI", Font.BOLD, 28);
    private Font scoreFont    = new Font("Consolas", Font.BOLD, 48);

    // Colors
    private Color bgDark      = new Color(18, 18, 38);
    private Color bgLight     = new Color(32, 38, 60);
    private Color gold        = new Color(255, 215, 0);
    private Color silver      = new Color(192, 192, 192);
    private Color bronze      = new Color(205, 127, 50);
    private Color trackColor  = new Color(60, 80, 40);
    private Color laneColor   = new Color(100, 120, 80);

    // ========================================================================
    //  GAME DATA
    // ========================================================================
    private List<Athlete> athletes = new ArrayList<>();
    private int currentAthleteIndex = 0;
    private int round = 1;

    private double[] scores100m   = new double[8];
    private double[] scoresLong   = new double[8];
    private double[] scoresJavelin = new double[8];
    private double[] scoresArchery = new double[8];
    private double[] scoresLift   = new double[8];

    private double totalPoints[] = new double[8];

    // Animation variables - 100m
    private double[] runnerX = new double[8];
    private double[] runnerSpeed = new double[8];
    private boolean[] finished100m = new boolean[8];
    private double finishLineX = WIDTH - 180;

    // Long jump
    private double jumpProgress = 0;
    private double jumpHeight = 0;
    private boolean jumping = false;
    private double jumpPower = 0;

    // Javelin
    private double javelinX = 200;
    private double javelinY = 400;
    private double javelinAngle = 45;
    private double javelinVelX = 0;
    private double javelinVelY = 0;
    private boolean javelinThrown = false;
    private double throwPower = 0;

    // Archery
    private double arrowX = 180;
    private double arrowY = 360;
    private double arrowAngle = 0;
    private boolean arrowFlying = false;
    private double targetCenterX = WIDTH - 220;
    private double targetCenterY = HEIGHT/2;

    // Weightlifting
    private double barY = 300;
    private double liftPower = 0;
    private boolean lifting = false;
    private int liftStage = 0; // 0 = ground, 1 = knees, 2 = overhead

    // Mouse / keyboard control
    private boolean spacePressed = false;
    private boolean mouseDown = false;
    private int mouseX, mouseY;

    // ========================================================================
    //  ATHLETE CLASS
    // ========================================================================
    static class Athlete {
        String name;
        String country;
        Color color;
        int number;

        Athlete(int num, String n, String c, Color col) {
            this.number = num;
            this.name = n;
            this.country = c;
            this.color = col;
        }

        String getFlagEmoji() {
            if (country.equals("USA")) return "🇺🇸";
            if (country.equals("JPN")) return "🇯🇵";
            if (country.equals("KEN")) return "🇰🇪";
            if (country.equals("GBR")) return "🇬🇧";
            if (country.equals("GER")) return "🇩🇪";
            if (country.equals("CHN")) return "🇨🇳";
            if (country.equals("FRA")) return "🇫🇷";
            if (country.equals("AUS")) return "🇦🇺";
            return "🏳️";
        }
    }

    // ========================================================================
    //  MAIN METHOD & CONSTRUCTOR
    // ========================================================================
    public OlympicSportSimulator() {
        setPreferredSize(new Dimension(WIDTH, HEIGHT));
        setBackground(bgDark);
        setFocusable(true);

        initAthletes();

        timer = new Timer(1000 / FPS, this);
        timer.start();

        addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    spacePressed = true;
                }
                if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                    if (gameState != MENU) gameState = MENU;
                }
            }
            @Override public void keyReleased(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    spacePressed = false;
                }
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) {
                mouseDown = true;
                mouseX = e.getX();
                mouseY = e.getY();
            }
            @Override public void mouseReleased(MouseEvent e) {
                mouseDown = false;
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
            @Override public void mouseDragged(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
            }
        });
    }

    private void initAthletes() {
        athletes.clear();
        athletes.add(new Athlete(1, "Noah Lyles",     "USA", new Color(40,100,220)));
        athletes.add(new Athlete(2, "Kishane Thompson","JAM", new Color(220,40,40)));
        athletes.add(new Athlete(3, "Fred Kerley",     "USA", new Color(30,180,30)));
        athletes.add(new Athlete(4, "Akani Simbine",   "RSA", new Color(255,180,0)));
        athletes.add(new Athlete(5, "Marcell Jacobs",  "ITA", new Color(0,120,200)));
        athletes.add(new Athlete(6, "Letsile Tebogo",  "BOT", new Color(0,180,120)));
        athletes.add(new Athlete(7, "Zharnel Hughes",  "GBR", new Color(20,180,20)));
        athletes.add(new Athlete(8, "Oblique Seville", "JAM", new Color(180,40,180)));
    }

    // ========================================================================
    //  MAIN GAME LOOP (ActionListener)
    // ========================================================================
    @Override
    public void actionPerformed(ActionEvent e) {
        if (gameState == MENU) {
            // nothing special
        }
        else if (gameState == RUNNING_100M) {
            update100m();
        }
        else if (gameState == LONG_JUMP) {
            updateLongJump();
        }
        else if (gameState == JAVELIN) {
            updateJavelin();
        }
        else if (gameState == ARCHERY) {
            updateArchery();
        }
        else if (gameState == WEIGHTLIFT) {
            updateWeightlifting();
        }

        repaint();
    }

    // ========================================================================
    //  100 METERS DASH LOGIC
    // ========================================================================
    private void start100m() {
        Arrays.fill(runnerX, 120.0);
        Arrays.fill(runnerSpeed, 0.0);
        Arrays.fill(finished100m, false);

        for (int i = 0; i < 8; i++) {
            runnerSpeed[i] = 4.8 + rand.nextDouble() * 2.2 - (i * 0.12);
        }

        currentAthleteIndex = 0;
    }

    private void update100m() {
        boolean allFinished = true;

        for (int i = 0; i < 8; i++) {
            if (!finished100m[i]) {
                allFinished = false;

                // simple acceleration + random perturbation
                runnerSpeed[i] += 0.004 + rand.nextDouble() * 0.008 - 0.004;
                runnerSpeed[i] = Math.min(runnerSpeed[i], 11.8);

                runnerX[i] += runnerSpeed[i];

                if (runnerX[i] >= finishLineX) {
                    runnerX[i] = finishLineX;
                    finished100m[i] = true;

                    // record time ~ (distance / avg speed)
                    double time = 9.58 + (i * 0.25) + rand.nextGaussian() * 0.12;
                    scores100m[i] = Math.round(time * 100.0) / 100.0;
                }
            }
        }

        if (allFinished && currentAthleteIndex < 7) {
            currentAthleteIndex++;
            try { Thread.sleep(1800); } catch(Exception ex){}
            start100m(); // next heat (fake parallel heats)
            round++;
        }
        else if (allFinished && currentAthleteIndex >= 7) {
            gameState = LONG_JUMP;
            startLongJump();
        }
    }

    // ========================================================================
    //  LONG JUMP LOGIC
    // ========================================================================
    private void startLongJump() {
        jumpProgress = 0;
        jumpHeight = 0;
        jumping = false;
        jumpPower = 0;
    }

    private void updateLongJump() {
        if (!jumping) {
            if (spacePressed || mouseDown) {
                jumpPower += 0.08;
                jumpPower = Math.min(jumpPower, 1.0);
            } else if (jumpPower > 0.01) {
                jumping = true;
                // convert power → distance + height curve
                double dist = 6.8 + jumpPower * 3.4 + rand.nextGaussian() * 0.4;
                scoresLong[currentAthleteIndex] = Math.round(dist * 100.0) / 100.0;
            }
        } else {
            jumpProgress += 0.025;
            if (jumpProgress < 0.5) {
                jumpHeight = jumpPower * 180 * (1 - Math.pow(2*jumpProgress-1, 2));
            } else {
                jumpHeight *= 0.92;
            }

            if (jumpProgress >= 1.8) {
                jumping = false;
                jumpProgress = 0;
                jumpHeight = 0;
                jumpPower = 0;
                currentAthleteIndex++;

                if (currentAthleteIndex >= 8) {
                    gameState = JAVELIN;
                    startJavelin();
                }
            }
        }
    }

    // ========================================================================
    //  JAVELIN THROW LOGIC
    // ========================================================================
    private void startJavelin() {
        javelinX = 200;
        javelinY = 400;
        javelinAngle = 42 + rand.nextDouble()*12 - 6;
        javelinVelX = 0;
        javelinVelY = 0;
        javelinThrown = false;
        throwPower = 0;
    }

    private void updateJavelin() {
        if (!javelinThrown) {
            if (spacePressed || mouseDown) {
                throwPower += 0.06;
                throwPower = Math.min(throwPower, 1.0);
            }
            else if (throwPower > 0.05) {
                javelinThrown = true;
                double power = 22 + throwPower * 18 + rand.nextGaussian()*2.5;
                double rad = Math.toRadians(javelinAngle);
                javelinVelX = power * Math.cos(rad);
                javelinVelY = -power * Math.sin(rad); // negative = up
            }
        } else {
            javelinVelY += 0.48; // gravity
            javelinX += javelinVelX * 0.4;
            javelinY += javelinVelY * 0.4;

            javelinAngle += 0.6 + javelinVelY * 0.02;

            if (javelinY > 480) {
                javelinY = 480;
                double dist = (javelinX - 180) / 6.2;
                scoresJavelin[currentAthleteIndex] = Math.max(45, Math.round(dist * 100.0)/100.0);
                currentAthleteIndex++;

                if (currentAthleteIndex >= 8) {
                    gameState = ARCHERY;
                    startArchery();
                } else {
                    startJavelin();
                }
            }
        }
    }

    // ========================================================================
    //  ARCHERY LOGIC
    // ========================================================================
    private void startArchery() {
        arrowX = 180;
        arrowY = HEIGHT/2;
        arrowAngle = 0;
        arrowFlying = false;
    }

    private void updateArchery() {
        if (!arrowFlying) {
            // aim with mouse
            double dx = mouseX - 180;
            double dy = mouseY - HEIGHT/2;
            arrowAngle = Math.toDegrees(Math.atan2(dy, dx));

            if (mouseDown || spacePressed) {
                arrowFlying = true;
                double power = 18 + rand.nextDouble()*6;
                double rad = Math.toRadians(arrowAngle);
                double vx = power * Math.cos(rad);
                double vy = power * Math.sin(rad);

                // simulate flight (very simplified)
                double flightTime = (targetCenterX - 180) / vx;
                double drop = 0.5 * 9.81 * flightTime * flightTime * 30;
                double finalY = arrowY + vy * flightTime - drop;

                double distFromCenter = Math.abs(finalY - targetCenterY);
                double score = Math.max(0, 10 - distFromCenter/22);
                scoresArchery[currentAthleteIndex] = Math.round(score * 10.0)/10.0;

                currentAthleteIndex++;
                if (currentAthleteIndex >= 8) {
                    gameState = WEIGHTLIFT;
                    startWeightlifting();
                } else {
                    startArchery();
                }
            }
        }
    }

    // ========================================================================
    //  WEIGHTLIFTING LOGIC (simplified)
    // ========================================================================
    private void startWeightlifting() {
        barY = 300;
        liftPower = 0;
        lifting = false;
        liftStage = 0;
    }

    private void updateWeightlifting() {
        if (!lifting) {
            if (spacePressed || mouseDown) {
                liftPower += 0.09;
                liftPower = Math.min(liftPower, 1.0);
            } else if (liftPower > 0.1) {
                lifting = true;
            }
        } else {
            if (liftStage == 0) {
                barY -= 2.8 + liftPower*3.2;
                if (barY < 180) {
                    liftStage = 1;
                }
            } else if (liftStage == 1) {
                barY -= 1.4 + liftPower*2.0;
                if (barY < 80) {
                    liftStage = 2;
                }
            } else if (liftStage == 2) {
                // hold phase
                if (rand.nextDouble() < 0.03) {
                    // shake / fail chance
                    if (liftPower < 0.65 + rand.nextDouble()*0.2) {
                        liftStage = 3; // fail
                    }
                }
            }

            if (liftStage == 2 && !spacePressed && !mouseDown) {
                // success
                double kg = 180 + liftPower * 120 + rand.nextGaussian()*12;
                scoresLift[currentAthleteIndex] = Math.round(kg);
                currentAthleteIndex++;
                lifting = false;
                liftStage = 0;
                barY = 300;
                liftPower = 0;

                if (currentAthleteIndex >= 8) {
                    calculateFinalResults();
                    gameState = RESULTS;
                } else {
                    startWeightlifting();
                }
            }
        }
    }

    private void calculateFinalResults() {
        Arrays.fill(totalPoints, 0);

        // very naive point system
        for (int i = 0; i < 8; i++) {
            // 100m - lower time = better
            totalPoints[i] += (12.0 - scores100m[i]) * 40;

            // long jump - higher = better
            totalPoints[i] += scoresLong[i] * 18;

            // javelin
            totalPoints[i] += scoresJavelin[i] * 5.5;

            // archery
            totalPoints[i] += scoresArchery[i] * 45;

            // weightlifting
            totalPoints[i] += scoresLift[i] * 0.9;
        }
    }

    // ========================================================================
    //  RENDERING
    // ========================================================================
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(g2);

        if (gameState == MENU) {
            drawMenu(g2);
        }
        else if (gameState == RUNNING_100M) {
            draw100m(g2);
        }
        else if (gameState == LONG_JUMP) {
            drawLongJump(g2);
        }
        else if (gameState == JAVELIN) {
            drawJavelin(g2);
        }
        else if (gameState == ARCHERY) {
            drawArchery(g2);
        }
        else if (gameState == WEIGHTLIFT) {
            drawWeightlifting(g2);
        }
        else if (gameState == RESULTS) {
            drawResults(g2);
        }
        else if (gameState == CREDITS) {
            drawCredits(g2);
        }

        drawUI(g2);
    }

    private void drawBackground(Graphics2D g) {
        // sky gradient
        GradientPaint sky = new GradientPaint(0,0, new Color(60,120,220), 0,HEIGHT/2, new Color(20,50,140));
        g.setPaint(sky);
        g.fillRect(0,0,WIDTH,HEIGHT);

        // stadium stands
        g.setColor(new Color(60,60,80,180));
        g.fillRect(0, HEIGHT-140, WIDTH, 140);

        // track area
        g.setColor(trackColor);
        g.fillRect(80, HEIGHT-340, WIDTH-160, 260);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        FontMetrics fm = g.getFontMetrics();
        String title = "Olympic Sport Simulator";
        int tw = fm.stringWidth(title);
        g.drawString(title, WIDTH/2 - tw/2, 140);

        g.setFont(subtitleFont);
        g.drawString("Press SPACE or CLICK to start!", WIDTH/2 - 280, 240);

        g.setFont(buttonFont);
        g.setColor(gold);
        g.drawString("100m • Long Jump • Javelin • Archery • Weightlifting", WIDTH/2 - 380, 340);

        g.setColor(Color.WHITE);
        g.drawString("(ESC to return to menu)", WIDTH/2 - 180, HEIGHT-80);
    }

    private void draw100m(Graphics2D g) {
        // lanes
        for (int i = 0; i < 8; i++) {
            int y = 160 + i*60;
            g.setColor(laneColor);
            g.fillRect(100, y-25, WIDTH-200, 50);

            // lane number
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.BOLD, 22));
            g.drawString("" + (i+1), 70, y+8);
        }

        // finish line
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(8));
        g.drawLine((int)finishLineX, 120, (int)finishLineX, 580);

        // runners
        for (int i = 0; i < 8; i++) {
            int y = 160 + i*60;
            Athlete a = athletes.get(i);

            // body
            g.setColor(a.color);
            g.fillOval((int)runnerX[i]-20, y-30, 40, 60);

            // head
            g.setColor(Color.PINK);
            g.fillOval((int)runnerX[i]-12, y-45, 24, 24);

            // name
            g.setColor(Color.WHITE);
            g.setFont(new Font("Arial", Font.PLAIN, 16));
            g.drawString(a.name.substring(0, Math.min(10,a.name.length())), (int)runnerX[i]-30, y-55);
        }

        g.setFont(scoreFont);
        g.setColor(gold);
        g.drawString("100m DASH - Heat " + round, 180, 80);
    }

    private void drawLongJump(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont.deriveFont(54f));
        g.drawString("LONG JUMP", 180, 100);

        // runway
        g.setColor(new Color(200,180,120));
        g.fillRect(100, 320, 600, 80);

        // sand pit
        g.setColor(new Color(240,210,160));
        g.fillRect(700, 300, 500, 120);

        Athlete a = athletes.get(currentAthleteIndex);

        // athlete running / jumping
        int runX = 140 + (int)(jumpProgress * 580);
        int runY = 340;

        if (jumping) {
            runX = 700 + (int)(jumpProgress * 400);
            runY = 340 - (int)jumpHeight;
        }

        g.setColor(a.color);
        g.fillOval(runX-20, runY-40, 40, 80);

        g.setColor(Color.WHITE);
        g.setFont(subtitleFont);
        g.drawString(a.name + "  " + a.getFlagEmoji(), 180, 180);

        if (!jumping) {
            g.setColor(gold);
            g.fillRect(180, 420, (int)(400 * jumpPower), 40);
            g.setColor(Color.WHITE);
            g.drawString("HOLD SPACE / CLICK to gain power!", 180, 500);
        } else {
            g.setColor(Color.CYAN);
            g.drawString(String.format("Distance: %.2f m", scoresLong[currentAthleteIndex]), 180, 500);
        }
    }

    private void drawJavelin(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont.deriveFont(54f));
        g.drawString("JAVELIN THROW", 180, 100);

        // runway
        g.setColor(new Color(180,160,100));
        g.fillRect(100, 340, 400, 100);

        Athlete a = athletes.get(currentAthleteIndex);

        // athlete
        g.setColor(a.color);
        g.fillRect(160, 380, 60, 100);

        // javelin
        AffineTransform old = g.getTransform();
        g.translate(javelinX, javelinY);
        g.rotate(Math.toRadians(javelinAngle));

        g.setColor(new Color(220,220,100));
        g.fillRoundRect(-80, -6, 160, 12, 10, 10);

        g.setTransform(old);

        if (!javelinThrown) {
            g.setColor(gold);
            g.fillRect(180, 520, (int)(400 * throwPower), 40);
            g.setColor(Color.WHITE);
            g.drawString("HOLD → RELEASE to throw!", 180, 580);
        } else {
            g.setColor(Color.CYAN);
            g.drawString(String.format("Distance: %.2f m", scoresJavelin[currentAthleteIndex]), 180, 580);
        }

        g.setColor(Color.WHITE);
        g.drawString(a.name + "  " + a.getFlagEmoji(), 180, 180);
    }

    private void drawArchery(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont.deriveFont(54f));
        g.drawString("ARCHERY", 180, 100);

        // bow / archer
        g.setColor(new Color(120,80,40));
        g.fillOval(140, HEIGHT/2 - 60, 80, 120);

        // arrow (when not flying we show aiming line)
        if (!arrowFlying) {
            g.setColor(Color.RED);
            g.setStroke(new BasicStroke(3));
            g.drawLine(180, HEIGHT/2, mouseX, mouseY);
        }

        // target
        int[] radii = {120, 100, 80, 60, 40, 20, 10};
        Color[] colors = {
                new Color(0,180,0), Color.BLACK, new Color(0,120,220),
                Color.BLACK, Color.RED, Color.YELLOW, Color.YELLOW.darker()
        };

        for (int i = 0; i < radii.length; i++) {
            g.setColor(colors[i]);
            g.fillOval((int) (targetCenterX - radii[i]), (int) (targetCenterY - radii[i]), radii[i]*2, radii[i]*2);
        }

        if (arrowFlying) {
            g.setColor(Color.BLACK);
            g.fillOval((int)arrowX-6, (int)arrowY-6, 12, 12);
        }

        Athlete a = athletes.get(currentAthleteIndex);
        g.setColor(Color.WHITE);
        g.drawString(a.name + "  " + a.getFlagEmoji(), 180, 180);
        g.setFont(subtitleFont);
        g.drawString("Aim with MOUSE, click or SPACE to shoot", 180, 580);
    }

    private void drawWeightlifting(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont.deriveFont(48f));
        g.drawString("WEIGHTLIFTING", 180, 100);

        // platform
        g.setColor(new Color(100,80,60));
        g.fillRect(300, 480, 600, 40);

        // barbell
        g.setColor(Color.GRAY);
        g.fillRoundRect(380, (int)barY - 20, 440, 40, 20, 20);

        // weights
        g.setColor(new Color(180,40,40));
        for (int x : new int[]{380, 760}) {
            g.fillOval(x-60, (int)barY-50, 100, 100);
        }

        g.setColor(Color.WHITE);
        Athlete a = athletes.get(currentAthleteIndex);
        g.drawString(a.name + "  " + a.getFlagEmoji(), 180, 180);

        if (!lifting) {
            g.setColor(gold);
            g.fillRect(180, 520, (int)(400 * liftPower), 40);
            g.setColor(Color.WHITE);
            g.drawString("HOLD SPACE / CLICK to lift!", 180, 580);
        } else if (liftStage == 2) {
            g.setColor(Color.GREEN);
            g.drawString("HOLD STEADY! Release when ready.", 180, 580);
        } else if (liftStage == 3) {
            g.setColor(Color.RED);
            g.drawString("FAIL! Bar dropped...", 180, 580);
        } else {
            g.setColor(Color.CYAN);
            g.drawString(String.format("Lift: %d kg", (int)scoresLift[currentAthleteIndex]), 180, 580);
        }
    }

    private void drawResults(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        g.drawString("FINAL MEDAL TABLE", WIDTH/2 - 320, 100);

        // sort athletes by total points
        List<Integer> ranking = new ArrayList<>();
        for (int i = 0; i < 8; i++) ranking.add(i);
        ranking.sort((a,b) -> Double.compare(totalPoints[b], totalPoints[a]));

        g.setFont(subtitleFont);
        for (int pos = 0; pos < 8; pos++) {
            int idx = ranking.get(pos);
            Athlete a = athletes.get(idx);
            double pts = totalPoints[idx];

            int y = 180 + pos*60;

            Color medal = (pos==0)?gold:(pos==1)?silver:bronze;

            g.setColor(medal);
            g.fillOval(140, y-30, 60, 60);

            g.setColor(Color.BLACK);
            g.setFont(scoreFont);
            g.drawString("" + (pos+1), 155, y+18);

            g.setColor(Color.WHITE);
            g.setFont(subtitleFont);
            g.drawString(String.format("%s  %s  %.0f pts", a.name, a.getFlagEmoji(), pts), 240, y+18);
        }

        g.setFont(buttonFont);
        g.setColor(Color.CYAN);
        g.drawString("Press ESC to return to menu", WIDTH/2 - 220, HEIGHT - 60);
    }

    private void drawCredits(Graphics2D g) {
        g.setColor(Color.WHITE);
        g.setFont(titleFont);
        g.drawString("CREDITS", WIDTH/2 - 140, 140);

        g.setFont(subtitleFont);
        int y = 240;
        String[] lines = {
                "Created with Java + Swing",
                "Very simplified Olympic events simulation",
                "Animation & interaction powered by Timer",
                "",
                "Have fun! Press ESC to go back."
        };
        for (String line : lines) {
            g.drawString(line, WIDTH/2 - 280, y);
            y += 60;
        }
    }

    private void drawUI(Graphics2D g) {
        // top bar
        g.setColor(new Color(0,0,0,120));
        g.fillRect(0,0,WIDTH,60);

        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.drawString("Olympic Sport Simulator  •  " + currentDateStr(), 40, 42);
    }

    private String currentDateStr() {
        return java.time.LocalDate.now().toString();
    }

    // ========================================================================
    //  MAIN LAUNCH
    // ========================================================================
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Olympic Sport Simulator");
            OlympicSportSimulator game = new OlympicSportSimulator();
            frame.add(game);
            frame.pack();
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);

            // start first event
            game.gameState = RUNNING_100M;
            game.start100m();
        });
    }
}