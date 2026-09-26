import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SupercarRacingChallenge extends JFrame {
    public SupercarRacingChallenge() {
        setTitle("SUPERCAR RACING CHALLENGE - Ultimate Edition");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setResizable(false);
        GamePanel panel = new GamePanel();
        add(panel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SupercarRacingChallenge::new);
    }

    static class GamePanel extends JPanel implements ActionListener, KeyListener {
        static final int WIDTH = 1200;
        static final int HEIGHT = 750;
        static final int TRACK_LAPS = 3;
        static final int TRACK_CENTER_X = WIDTH/2;
        static final int TRACK_CENTER_Y = HEIGHT/2 + 20;
        static final int TRACK_OUTER_RX = 500;
        static final int TRACK_OUTER_RY = 300;
        static final int TRACK_INNER_RX = 320;
        static final int TRACK_INNER_RY = 180;
        static final int ROAD_WIDTH = 180;

        Timer timer;
        int gameState = 0; // 0=menu, 1=racing, 2=finished
        int mode = 0; // 0=vs AI, 1=2P
        Supercar car1, car2;
        List<Point2D> checkpoints;
        List<Point2D> aiWaypoints;
        List<Particle> particles = new ArrayList<>();
        List<SkidMark> skidMarks = new ArrayList<>();
        Random rand = new Random();
        int winner = 0;
        float finishTimer = 0;

        // keys
        boolean[] keys = new boolean[512];

        GamePanel() {
            setPreferredSize(new Dimension(WIDTH, HEIGHT));
            setFocusable(true);
            addKeyListener(this);
            timer = new Timer(16, this); // ~60 FPS

            checkpoints = new ArrayList<>();
            aiWaypoints = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                double ang = Math.toRadians(i * 360.0 / 24);
                double r = (TRACK_OUTER_RX + TRACK_INNER_RX) / 2.0;
                double r2 = (TRACK_OUTER_RY + TRACK_INNER_RY) / 2.0;
                checkpoints.add(new Point2D.Double(
                        TRACK_CENTER_X + Math.cos(ang) * r,
                        TRACK_CENTER_Y + Math.sin(ang) * r2
                ));
                aiWaypoints.add(checkpoints.get(checkpoints.size()-1));
            }

            resetGame();
            timer.start();
        }

        void resetGame() {
            car1 = new Supercar(TRACK_CENTER_X + 40, TRACK_CENTER_Y - TRACK_OUTER_RY + 40, -90, new Color(255, 45, 45), new Color(180, 0, 0));
            car2 = new Supercar(TRACK_CENTER_X - 40, TRACK_CENTER_Y - TRACK_OUTER_RY + 40, -90, new Color(45, 120, 255), new Color(0, 60, 180));
            car1.isPlayer1 = true;
            car2.isPlayer1 = false;
            particles.clear();
            skidMarks.clear();
            winner = 0;
            finishTimer = 0;
            gameState = 0;
        }

        void startRace(int selectedMode) {
            this.mode = selectedMode;
            car1.lap = 0; car1.checkpoint = 0; car1.totalDistance = 0;
            car2.lap = 0; car2.checkpoint = 0; car2.totalDistance = 0;
            car1.reset(TRACK_CENTER_X + 30, TRACK_CENTER_Y - TRACK_OUTER_RY + 55);
            car2.reset(TRACK_CENTER_X - 30, TRACK_CENTER_Y - TRACK_OUTER_RY + 55);
            particles.clear();
            skidMarks.clear();
            gameState = 1;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (gameState == 1) updateGame();
            else if (gameState == 2) {
                finishTimer += 0.016f;
                if (finishTimer > 4 && keys[KeyEvent.VK_SPACE]) resetGame();
            }
            repaint();
        }

        void updateGame() {
            // Player 1 input
            if (keys[KeyEvent.VK_W]) car1.accelerate(0.24f);
            if (keys[KeyEvent.VK_S]) car1.brake(0.18f);
            if (keys[KeyEvent.VK_A]) car1.turn(-1);
            if (keys[KeyEvent.VK_D]) car1.turn(1);
            car1.nitro = keys[KeyEvent.VK_SHIFT];

            // Player 2 or AI
            if (mode == 1) { // 2 players
                if (keys[KeyEvent.VK_UP]) car2.accelerate(0.24f);
                if (keys[KeyEvent.VK_DOWN]) car2.brake(0.18f);
                if (keys[KeyEvent.VK_LEFT]) car2.turn(-1);
                if (keys[KeyEvent.VK_RIGHT]) car2.turn(1);
                car2.nitro = keys[KeyEvent.VK_M];
            } else {
                // AI logic
                car2.aiDrive(aiWaypoints, car1);
            }

            car1.update(this);
            car2.update(this);

            // check laps
            if (car1.lap >= TRACK_LAPS || car2.lap >= TRACK_LAPS) {
                winner = car1.lap >= TRACK_LAPS ? 1 : 2;
                if (car1.lap >= TRACK_LAPS && car2.lap >= TRACK_LAPS) {
                    winner = car1.totalDistance > car2.totalDistance ? 1 : 2;
                }
                gameState = 2;
                finishTimer = 0;
                // explosion particles
                Supercar winCar = winner==1?car1:car2;
                for(int i=0;i<120;i++) particles.add(new Particle(winCar.x, winCar.y, true));
            }

            // particles update
            particles.removeIf(p -> { p.update(); return p.life <= 0; });
            if (skidMarks.size() > 400) skidMarks.subList(0,100).clear();
        }

        boolean isOnTrack(double x, double y) {
            double dx = (x - TRACK_CENTER_X) / TRACK_OUTER_RX;
            double dy = (y - TRACK_CENTER_Y) / TRACK_OUTER_RY;
            double outer = dx*dx + dy*dy;
            double dx2 = (x - TRACK_CENTER_X) / TRACK_INNER_RX;
            double dy2 = (y - TRACK_CENTER_Y) / TRACK_INNER_RY;
            double inner = dx2*dx2 + dy2*dy2;
            return outer <= 1.05 && inner >= 0.88;
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            if (gameState == 0) { drawMenu(g2); return; }

            // Beautiful background
            GradientPaint bg = new GradientPaint(0,0,new Color(20,25,40), 0, HEIGHT, new Color(5,10,20));
            g2.setPaint(bg);
            g2.fillRect(0,0,WIDTH,HEIGHT);

            // Stars
            g2.setColor(new Color(255,255,255,30));
            for(int i=0;i<80;i++){
                int sx = (i* 137) % WIDTH;
                int sy = (i* 271) % (HEIGHT/2);
                g2.fillOval(sx, sy, 2, 2);
            }

            // Grass with texture
            drawGrass(g2);

            // Track outer shadow
            g2.setColor(new Color(0,0,0,80));
            g2.fillOval(TRACK_CENTER_X-TRACK_OUTER_RX-15, TRACK_CENTER_Y-TRACK_OUTER_RY-15, (TRACK_OUTER_RX+15)*2, (TRACK_OUTER_RY+15)*2);

            // Track asphalt
            g2.setColor(new Color(35,35,40));
            g2.fillOval(TRACK_CENTER_X-TRACK_OUTER_RX, TRACK_CENTER_Y-TRACK_OUTER_RY, TRACK_OUTER_RX*2, TRACK_OUTER_RY*2);
            g2.setColor(new Color(55,55,60));
            for(int i=0;i<3;i++){
                g2.setStroke(new BasicStroke(1, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{15,20}, i*10));
                g2.drawOval(TRACK_CENTER_X-TRACK_OUTER_RX+20+i*40, TRACK_CENTER_Y-TRACK_OUTER_RY+20+i*30, (TRACK_OUTER_RX-20-i*40)*2, (TRACK_OUTER_RY-20-i*30)*2);
            }
            g2.setStroke(new BasicStroke());

            // Inner grass
            g2.setColor(new Color(25, 85, 35));
            g2.fillOval(TRACK_CENTER_X-TRACK_INNER_RX, TRACK_CENTER_Y-TRACK_INNER_RY, TRACK_INNER_RX*2, TRACK_INNER_RY*2);

            // Inner curb
            g2.setColor(new Color(200,200,200));
            g2.setStroke(new BasicStroke(6));
            g2.drawOval(TRACK_CENTER_X-TRACK_INNER_RX, TRACK_CENTER_Y-TRACK_INNER_RY, TRACK_INNER_RX*2, TRACK_INNER_RY*2);
            g2.setColor(new Color(220,30,30));
            g2.setStroke(new BasicStroke(6, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 0, new float[]{20,20}, 0));
            g2.drawOval(TRACK_CENTER_X-TRACK_INNER_RX, TRACK_CENTER_Y-TRACK_INNER_RY, TRACK_INNER_RX*2, TRACK_INNER_RY*2);
            g2.setStroke(new BasicStroke());

            // Outer curb
            g2.setColor(Color.WHITE);
            g2.setStroke(new BasicStroke(8));
            g2.drawOval(TRACK_CENTER_X-TRACK_OUTER_RX, TRACK_CENTER_Y-TRACK_OUTER_RY, TRACK_OUTER_RX*2, TRACK_OUTER_RY*2);
            g2.setStroke(new BasicStroke());

            // Start/Finish line
            g2.setColor(Color.WHITE);
            g2.fillRect(TRACK_CENTER_X-5, TRACK_CENTER_Y-TRACK_OUTER_RY, 10, ROAD_WIDTH/2+40);
            for(int y=0;y<ROAD_WIDTH/2+40;y+=20){
                for(int x=0;x<10;x+=10){
                    if((x+y)/10%2==0){
                        g2.setColor(Color.BLACK);
                        g2.fillRect(TRACK_CENTER_X-5+x, TRACK_CENTER_Y-TRACK_OUTER_RY+y, 10, 10);
                    }
                }
            }
            g2.setColor(Color.WHITE);

            // Skid marks
            for(SkidMark s: skidMarks){
                g2.setColor(new Color(0,0,0, s.alpha));
                g2.setStroke(new BasicStroke(2));
                g2.drawLine((int)s.x1, (int)s.y1, (int)s.x2, (int)s.y2);
            }
            g2.setStroke(new BasicStroke());

            // Particles
            for(Particle p: particles) p.draw(g2);

            // Cars with shadow
            car1.drawShadow(g2);
            car2.drawShadow(g2);
            car1.draw(g2);
            car2.draw(g2);

            // HUD
            drawHUD(g2);

            if (gameState == 2) drawFinishScreen(g2);
        }

        void drawGrass(Graphics2D g2){
            g2.setColor(new Color(18, 65, 28));
            g2.fillRect(0,0,WIDTH,HEIGHT);
            // subtle noise
            g2.setColor(new Color(30, 90, 40, 40));
            for(int i=0;i<200;i++){
                int x = rand.nextInt(WIDTH);
                int y = rand.nextInt(HEIGHT);
                g2.fillOval(x,y,3,6);
            }
        }

        void drawHUD(Graphics2D g2){
            g2.setFont(new Font("SansSerif", Font.BOLD, 18));
            // Car1 HUD
            drawCarHUD(g2, 20, 20, car1, "P1 - BLAZE (WASD+SHIFT)", 1);
            // Car2 HUD
            String label = mode==1? "P2 - FROST (ARROWS+M)" : "CPU - FROST (AI)";
            drawCarHUD(g2, WIDTH-280, 20, car2, label, 2);

            // Speed lines when nitro
            if (car1.nitro && car1.speed > 2) {
                g2.setColor(new Color(255,200,50,60));
                for(int i=0;i<5;i++){
                    int x = (int)(car1.x - Math.cos(Math.toRadians(car1.angle))* (20+i*15));
                    int y = (int)(car1.y - Math.sin(Math.toRadians(car1.angle))* (20+i*15));
                    g2.fillOval(x,y,12,3);
                }
            }
        }

        void drawCarHUD(Graphics2D g2, int x, int y, Supercar car, String name, int id){
            Color base = car.baseColor;
            g2.setColor(new Color(0,0,0,160));
            g2.fillRoundRect(x,y,260,88,14,14);
            g2.setColor(base);
            g2.fillRoundRect(x,y,260,6,6,6);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.drawString(name, x+10, y+22);
            g2.setFont(new Font("SansSerif", Font.BOLD, 26));
            g2.drawString(String.format("%03d KM/H", (int)(car.speed*38)), x+10, y+52);
            g2.setFont(new Font("SansSerif", Font.BOLD, 12));
            g2.setColor(new Color(255,255,255,200));
            g2.drawString("LAP " + (car.lap+1) + "/" + TRACK_LAPS + "  CP " + car.checkpoint + "/24", x+10, y+70);

            // nitro bar
            g2.setColor(new Color(255,255,255,60));
            g2.fillRoundRect(x+10, y+76, 240, 6, 3,3);
            g2.setColor(car.nitro && car.nitroAmount>0 ? new Color(80,220,255) : new Color(255,180,40));
            g2.fillRoundRect(x+10, y+76, (int)(240 * (car.nitroAmount/100f)), 6, 3,3);

            if (car.nitroAmount <= 0.1f) {
                g2.setColor(new Color(255,50,50));
                g2.drawString("NITRO EMPTY!", x+150, y+52);
            }
        }

        void drawMenu(Graphics2D g2){
            GradientPaint bg = new GradientPaint(0,0,new Color(10,15,30), 0, HEIGHT, new Color(80,10,20));
            g2.setPaint(bg);
            g2.fillRect(0,0,WIDTH,HEIGHT);

            // Title glow
            g2.setFont(new Font("SansSerif", Font.BOLD, 64));
            String title = "SUPERCAR RACING";
            g2.setColor(new Color(0,0,0,120));
            g2.drawString(title, WIDTH/2 - g2.getFontMetrics().stringWidth(title)/2 + 4, 150+4);
            GradientPaint titleGrad = new GradientPaint(0,130, new Color(255,240,120), WIDTH,130, new Color(255,60,60));
            g2.setPaint(titleGrad);
            g2.drawString(title, WIDTH/2 - g2.getFontMetrics().stringWidth(title)/2, 150);

            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            String sub = "CHALLENGE // ULTIMATE EDITION";
            g2.setColor(new Color(255,255,255,180));
            g2.drawString(sub, WIDTH/2 - g2.getFontMetrics().stringWidth(sub)/2, 190);

            // Buttons
            drawMenuButton(g2, WIDTH/2-200, 280, 400, 70, "1 PLAYER vs COMPUTER [Press 1]", mode==0?new Color(255,200,50):new Color(60,60,80), keys[KeyEvent.VK_1]);
            drawMenuButton(g2, WIDTH/2-200, 370, 400, 70, "2 PLAYERS DUEL [Press 2]", mode==1?new Color(80,200,255):new Color(60,60,80), keys[KeyEvent.VK_2]);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 14));
            g2.setColor(new Color(255,255,255,120));
            String[] controls = {
                    "PLAYER 1: W/S Accelerate/Brake | A/D Steer | LEFT SHIFT Nitro",
                    "PLAYER 2: Arrow Keys | M Nitro  //  AI has adaptive racing line & nitro",
                    "First to " + TRACK_LAPS + " laps wins. Use drift (brake+steer) for faster corners!"
            };
            for(int i=0;i<controls.length;i++) g2.drawString(controls[i], WIDTH/2 - g2.getFontMetrics().stringWidth(controls[i])/2, 500+i*22);

            // Animated cars in menu
            g2.translate(WIDTH/2-120, 620);
            g2.rotate(Math.toRadians(Math.sin(System.currentTimeMillis()/500.0)*5));
            Supercar demo1 = new Supercar(0,0,-5, new Color(255,45,45), new Color(180,0,0));
            demo1.speed = 3.5f;
            demo1.nitro = true;
            demo1.drawShadow(g2); demo1.draw(g2);
            g2.rotate(Math.toRadians(-Math.sin(System.currentTimeMillis()/500.0)*5));
            g2.translate(240, 0);
            g2.rotate(Math.toRadians(Math.sin(System.currentTimeMillis()/600.0)*5+10));
            Supercar demo2 = new Supercar(0,0,-5, new Color(45,120,255), new Color(0,60,180));
            demo2.speed = 3.2f;
            demo2.drawShadow(g2); demo2.draw(g2);
        }

        void drawMenuButton(Graphics2D g2, int x, int y, int w, int h, String txt, Color c, boolean hover){
            if(hover) {
                g2.setColor(new Color(255,255,255,40));
                g2.fillRoundRect(x-4,y-4,w+8,h+8,16,16);
            }
            g2.setColor(c);
            g2.fillRoundRect(x,y,w,h,14,14);
            g2.setColor(new Color(255,255,255,30));
            g2.fillRoundRect(x,y,w,h/2,14,14);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 16));
            g2.drawString(txt, x + w/2 - g2.getFontMetrics().stringWidth(txt)/2, y+h/2+5);
        }

        void drawFinishScreen(Graphics2D g2){
            g2.setColor(new Color(0,0,0,160));
            g2.fillRect(0,0,WIDTH,HEIGHT);
            g2.setFont(new Font("SansSerif", Font.BOLD, 72));
            String win = winner==1? "PLAYER 1 WINS!" : (mode==1? "PLAYER 2 WINS!" : (winner==1?"PLAYER WINS!":"COMPUTER WINS!"));
            Color wc = winner==1? car1.baseColor : car2.baseColor;
            g2.setColor(new Color(0,0,0,100));
            g2.drawString(win, WIDTH/2 - g2.getFontMetrics().stringWidth(win)/2 +4, HEIGHT/2+4);
            g2.setColor(wc);
            g2.drawString(win, WIDTH/2 - g2.getFontMetrics().stringWidth(win)/2, HEIGHT/2);

            g2.setFont(new Font("SansSerif", Font.PLAIN, 20));
            g2.setColor(Color.WHITE);
            String msg = "Press SPACE for Menu | R to Restart Instantly";
            g2.drawString(msg, WIDTH/2 - g2.getFontMetrics().stringWidth(msg)/2, HEIGHT/2+60);
        }

        @Override public void keyPressed(KeyEvent e){
            keys[e.getKeyCode()] = true;
            if (gameState==0){
                if(e.getKeyCode()==KeyEvent.VK_1) startRace(0);
                if(e.getKeyCode()==KeyEvent.VK_2) startRace(1);
            }
            if(e.getKeyCode()==KeyEvent.VK_R && gameState!=0) startRace(mode);
            if(e.getKeyCode()==KeyEvent.VK_SPACE && gameState==2) resetGame();
            if(e.getKeyCode()==KeyEvent.VK_ESCAPE) resetGame();
        }
        @Override public void keyReleased(KeyEvent e){ keys[e.getKeyCode()] = false; }
        @Override public void keyTyped(KeyEvent e){}
    }

    static class Supercar {
        double x,y;
        double angle; // degrees
        float speed = 0;
        Color baseColor, darkColor;
        boolean isPlayer1;
        int lap = 0, checkpoint = 0;
        double totalDistance = 0;
        boolean nitro = false;
        float nitroAmount = 100f;
        float driftFactor = 0;

        Supercar(double x, double y, double angle, Color base, Color dark){
            this.x=x; this.y=y; this.angle=angle;
            this.baseColor=base; this.darkColor=dark;
        }
        void reset(double nx, double ny){ x=nx; y=ny; angle=-90; speed=0; lap=0; checkpoint=0; nitroAmount=100; totalDistance=0; driftFactor=0; }

        void accelerate(float amt){
            float boost = (nitro && nitroAmount>0) ? 1.9f : 1f;
            if(nitro && nitroAmount>0){ nitroAmount -= 0.6f; amt *= 1.7f; }
            speed += amt * 0.08f * boost;
            if(speed> (nitro && nitroAmount>0? 7.5f:5.2f)) speed = nitro && nitroAmount>0? 7.5f:5.2f;
        }
        void brake(float amt){
            speed -= amt * 0.12f;
            if(speed < -2.2f) speed = -2.2f;
            driftFactor = Math.abs(speed)>1 ? driftFactor*0.95f+0.05f : 0;
        }
        void turn(int dir){
            if(Math.abs(speed) < 0.3f) return;
            double turnSpeed = 3.2 * (Math.abs(speed)/5.0) * (1 + driftFactor*0.6);
            if(speed < 0) dir = -dir;
            angle += dir * turnSpeed;
        }

        void aiDrive(List<Point2D> waypoints, Supercar opponent){
            // Follow waypoint racing line
            int next = (checkpoint+2) % waypoints.size();
            Point2D target = waypoints.get(next);
            double dx = target.getX()-x;
            double dy = target.getY()-y;
            double targetAng = Math.toDegrees(Math.atan2(dy,dx));
            double diff = normalizeAngle(targetAng-angle);
            if(Math.abs(diff) > 4) {
                if(diff>0) turn(1); else turn(-1);
            }
            // speed control
            double dist = Math.hypot(dx,dy);
            if(Math.abs(diff) < 25) accelerate(0.22f);
            else { if(speed>3) brake(0.12f); else accelerate(0.12f); }

            // use nitro on straights and if behind
            nitro = Math.abs(diff) < 15 && dist>180 && nitroAmount>20 && (opponent.lap>lap || (opponent.lap==lap && opponent.checkpoint>checkpoint) || speed>3.5);
            if(totalDistance < opponent.totalDistance-20) nitro = true;
        }
        double normalizeAngle(double a){ while(a>180)a-=360; while(a<-180)a+=360; return a; }

        void update(GamePanel panel){
            double nx = x + Math.cos(Math.toRadians(angle)) * speed * 4;
            double ny = y + Math.sin(Math.toRadians(angle)) * speed * 4;

            if(!panel.isOnTrack(nx, ny)){
                speed *= 0.82f; // off track friction
                // push back
                double cx = GamePanel.TRACK_CENTER_X;
                double cy = GamePanel.TRACK_CENTER_Y;
                double angToCenter = Math.atan2(cy-ny, cx-nx);
                nx += Math.cos(angToCenter)*1.5;
                ny += Math.sin(angToCenter)*1.5;
                // smoke when off-track
                if(Math.abs(speed)>1 && panel.rand.nextFloat()<0.5f){
                    panel.particles.add(new Particle(x,y,false));
                }
            }

            // skid marks & smoke when drifting
            if(Math.abs(driftFactor)>0.2 && Math.abs(speed)>2){
                panel.skidMarks.add(new SkidMark(x,y, nx,ny));
                if(panel.rand.nextFloat()<0.6f) panel.particles.add(new Particle(x,y,false));
            }

            // nitro flame
            if(nitro && nitroAmount>0 && speed>1){
                for(int i=0;i<2;i++){
                    double bx = x - Math.cos(Math.toRadians(angle))*22;
                    double by = y - Math.sin(Math.toRadians(angle))*22;
                    panel.particles.add(new Particle(bx,by,true, baseColor));
                }
            }

            x = nx; y = ny;
            speed *= 0.985f; // friction
            if(Math.abs(speed)<0.05) speed=0;
            driftFactor *= 0.96f;

            // checkpoint logic
            Point2D cp = panel.checkpoints.get(checkpoint);
            if(cp.distance(x,y) < 65){
                checkpoint = (checkpoint+1)%panel.checkpoints.size();
                if(checkpoint==0) lap++;
                totalDistance += 1;
            }
            // slow regen nitro
            if(!nitro && nitroAmount<100) nitroAmount += 0.12f;
        }

        void drawShadow(Graphics2D g2){
            g2.setColor(new Color(0,0,0,70));
            Ellipse2D shadow = new Ellipse2D.Double(x-18, y-10, 36, 20);
            AffineTransform old = g2.getTransform();
            g2.rotate(Math.toRadians(angle), x,y);
            g2.fill(shadow);
            g2.setTransform(old);
        }

        void draw(Graphics2D g2){
            AffineTransform old = g2.getTransform();
            g2.translate(x,y);
            g2.rotate(Math.toRadians(angle));

            // Car body - low poly supercar
            // Base
            g2.setColor(darkColor);
            g2.fillRoundRect(-20,-10,40,20,6,6);
            // Main body
            g2.setColor(baseColor);
            g2.fillRoundRect(-18,-9,36,18,6,6);
            // cockpit
            g2.setColor(new Color(20,20,30));
            g2.fillRoundRect(-6,-8,14,16,4,4);
            // windshield shine
            g2.setColor(new Color(180,220,255,160));
            g2.fillRoundRect(-4,-6,10,12,3,3);
            // front nose
            g2.setColor(new Color(255,255,255,90));
            Polygon nose = new Polygon(new int[]{18,12,12}, new int[]{0,-7,7}, 3);
            g2.fill(nose);
            // headlights
            g2.setColor(speed>0.1? new Color(255,255,180) : new Color(80,80,80));
            g2.fillOval(14,-8,4,3);
            g2.fillOval(14,5,4,3);
            // rear lights when braking
            if(speed<0 || driftFactor>0.3){
                g2.setColor(new Color(255,30,30));
                g2.fillOval(-20,-7,3,3);
                g2.fillOval(-20,4,3,3);
            }
            // spoiler
            g2.setColor(darkColor.darker());
            g2.fillRect(-20,-11,3,22);
            // metallic highlight
            g2.setColor(new Color(255,255,255,60));
            g2.fillRoundRect(-16,-8,24,3,2,2);

            // nitro glow ring
            if(nitro && nitroAmount>0 && speed>1){
                g2.setColor(new Color(80,220,255,180));
                g2.setStroke(new BasicStroke(2));
                g2.drawOval(-24,-14,6,28);
                g2.setStroke(new BasicStroke());
            }

            g2.setTransform(old);
        }
    }

    static class Particle {
        double x,y, vx, vy;
        float life, maxLife;
        boolean isFlame;
        Color color;
        Particle(double x, double y, boolean flame){ this(x,y,flame, null); }
        Particle(double x, double y, boolean flame, Color c){
            this.x=x; this.y=y; this.isFlame=flame;
            this.color=c;
            if(flame){
                vx = (Math.random()-0.5)*2;
                vy = (Math.random()-0.5)*2;
                maxLife= life = 18 + (float)Math.random()*12;
            }else{
                vx = (Math.random()-0.5)*1.5;
                vy = (Math.random()-0.5)*1.5;
                maxLife= life = 30 + (float)Math.random()*20;
            }
        }
        void update(){ x+=vx; y+=vy; vx*=0.96; vy*=0.96; life--; }
        void draw(Graphics2D g2){
            float alpha = life/maxLife;
            if(isFlame){
                Color col = color!=null? color : new Color(255, 180, 50);
                if(color==null) {
                    if(alpha>0.6) col = new Color(255,255,200, (int)(alpha*220));
                    else if(alpha>0.3) col = new Color(255,140,30, (int)(alpha*200));
                    else col = new Color(255,60,10, (int)(alpha*160));
                } else {
                    col = new Color(col.getRed(), col.getGreen(), col.getBlue(), (int)(alpha*200));
                }
                g2.setColor(col);
                g2.fillOval((int)x, (int)y, (int)(6*alpha+2), (int)(6*alpha+2));
            }else{
                g2.setColor(new Color(80,80,80, (int)(alpha*90)));
                g2.fillOval((int)x,(int)y, (int)(5*alpha+2),(int)(5*alpha+2));
            }
        }
    }
    static class SkidMark {
        double x1,y1,x2,y2; int alpha;
        SkidMark(double x1,double y1,double x2,double y2){ this.x1=x1; this.y1=y1; this.x2=x2; this.y2=y2; alpha=90; }
    }
}
