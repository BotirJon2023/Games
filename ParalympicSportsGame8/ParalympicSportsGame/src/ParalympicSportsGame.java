import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;
import javax.swing.Timer;

public class ParalympicSportsGame extends JPanel implements ActionListener, KeyListener {
    // ---------------------- App & Loop ----------------------
    private static final int BASE_W = 1000;
    private static final int BASE_H = 600;
    private static final int TARGET_FPS = 60;
    private static final int MS_PER_FRAME = 1000 / TARGET_FPS;

    private final Timer timer = new Timer(MS_PER_FRAME, this);

    // Scaling for large-UI mode
    private boolean largeUI = false;
    private float uiScale = 1.0f; // 1.0 or 1.25 or 1.5

    // High contrast mode
    private boolean highContrast = false;

    // Random source
    private final Random rng = new Random();

    // Time tracking
    private long tick = 0;

    // State machine
    private enum GameState { MENU, INTRO, PLAYING, PAUSED, RESULTS, CONTROLS }
    private GameState state = GameState.MENU;

    // Sports registry
    private final java.util.List<Sport> sports = new ArrayList<>();
    private int sportIndex = 0;
    private Sport currentSport;

    // Input handling & controls (remappable)
    private Controls controls = new Controls();
    private int remapIndex = -1; // -1 means none; otherwise index into key names

    // Score system
    private Scoreboard scoreboard = new Scoreboard();

    // Particles
    private final java.util.List<Particle> particles = new ArrayList<>();

    // Fade transitions
    private float fade = 1f; // 1=opaque, 0=clear
    private int fadeDir = -1; // -1 out, +1 in

    public ParalympicSportsGame() {
        setPreferredSize(new Dimension(BASE_W, BASE_H));
        setFocusable(true);
        setBackground(new Color(12, 14, 18));
        addKeyListener(this);
        buildSports();
        switchSport(0);
        timer.start();
    }

    // Build the sports list
    private void buildSports() {
        sports.add(new WheelchairSprint());
        sports.add(new Goalball());
        sports.add(new Boccia());
        sports.add(new SittingVolleyball());
        sports.add(new ParaSwimming());
    }

    private void switchSport(int idx) {
        sportIndex = (idx + sports.size()) % sports.size();
        currentSport = sports.get(sportIndex);
        currentSport.reset(rng);
    }

    // ---------------------- Loop ----------------------
    @Override
    public void actionPerformed(ActionEvent e) {
        tick++;
        uiScale = largeUI ? 1.35f : 1.0f;
        if (state == GameState.PLAYING) {
            currentSport.update(this, rng, controls, tick);
            addParticles(currentSport.emitParticles());
            if (currentSport.isOver()) {
                scoreboard.submit(currentSport.getName(), currentSport.getScore());
                state = GameState.RESULTS;
                startFadeIn();
            }
        }
        updateParticles();
        stepFade();
        repaint();
    }

    private void addParticles(java.util.List<Particle> list) {
        if (list == null) return;
        particles.addAll(list);
        if (particles.size() > 500) {
            particles.subList(0, particles.size()-500).clear();
        }
    }

    private void updateParticles() {
        for (int i = particles.size()-1; i >= 0; i--) {
            Particle p = particles.get(i);
            p.life -= 1;
            p.x += p.vx;
            p.y += p.vy;
            p.vy += p.gravity;
            if (p.life <= 0) particles.remove(i);
        }
    }

    private void startFadeIn() { fade = 0f; fadeDir = +1; }
    private void startFadeOut() { fade = 1f; fadeDir = -1; }
    private void stepFade() {
        if (fadeDir == 0) return;
        fade += 0.05f * fadeDir;
        if (fade <= 0f) { fade = 0f; fadeDir = 0; }
        if (fade >= 1f) { fade = 1f; fadeDir = 0; }
    }

    // ---------------------- Paint ----------------------
    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        drawBackground(g);

        // Depending on state
        switch (state) {
            case MENU -> drawMenu(g);
            case INTRO -> drawIntro(g);
            case PLAYING -> drawPlaying(g);
            case PAUSED -> drawPaused(g);
            case RESULTS -> drawResults(g);
            case CONTROLS -> drawControls(g);
        }

        // Particles on top
        drawParticles(g);

        // Fade overlay
        if (fade > 0f) {
            Composite prev = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.min(0.85f, fade*0.85f)));
            g.setColor(new Color(0,0,0));
            g.fillRect(0,0,getWidth(),getHeight());
            g.setComposite(prev);
        }
    }

    private void drawBackground(Graphics2D g) {
        if (highContrast) {
            g.setColor(Color.BLACK);
            g.fillRect(0,0,getWidth(),getHeight());
            return;
        }
        // smooth gradient background
        GradientPaint gp = new GradientPaint(0,0,new Color(16,18,22), 0,getHeight(), new Color(32,36,44));
        g.setPaint(gp);
        g.fillRect(0,0,getWidth(),getHeight());
        // soft grid
        g.setStroke(new BasicStroke(1f));
        g.setColor(new Color(255,255,255,20));
        for (int x=0;x<getWidth();x+=50) g.drawLine(x,0,x,getHeight());
        for (int y=0;y<getHeight();y+=50) g.drawLine(0,y,getWidth(),y);
    }

    private void drawMenu(Graphics2D g) {
        int cx = getWidth()/2;
        int cy = getHeight()/2;
        String title = "Paralympic Sports Game";
        drawTitle(g, title, cx, 100);

        // Sport carousel
        int cardW = (int)(260 * uiScale), cardH = (int)(140 * uiScale);
        int gap = (int)(30 * uiScale);
        int y = cy - cardH/2;
        for (int i=-2;i<=2;i++) {
            int idx = (sportIndex + i + sports.size()) % sports.size();
            int x = cx + i*(cardW+gap) - cardW/2;
            drawSportCard(g, sports.get(idx), x, y, cardW, cardH, i==0);
        }

        int hintY = getHeight()-120;
        drawHint(g, "←/→ switch sport   Enter: Start   C: Controls   H: High-Contrast   U: Large-UI", cx, hintY);
        drawScoreSummary(g, cx, hintY+30);
    }

    private void drawIntro(Graphics2D g) {
        int cx = getWidth()/2;
        int cy = getHeight()/2;
        drawTitle(g, currentSport.getName(), cx, 120);
        String[] lines = currentSport.getIntroLines();
        int y = 200;
        for (String line : lines) {
            drawBody(g, line, cx, y);
            y += (int)(28*uiScale);
        }
        drawHint(g, "Space: Play   Esc: Menu", cx, getHeight()-100);
    }

    private void drawPlaying(Graphics2D g) {
        currentSport.draw(this, g, highContrast, uiScale, tick);
        drawTopBar(g);
    }

    private void drawPaused(Graphics2D g) {
        drawPlaying(g);
        drawCenterBox(g, "Paused", new String[]{"Space: Resume","R: Restart Event","Esc: Menu"});
    }

    private void drawResults(Graphics2D g) {
        currentSport.drawResults(this, g, highContrast, uiScale, tick);
        drawCenterBox(g, "Event Complete!", new String[]{"Enter: Next Event","R: Replay","Esc: Menu"});
    }

    private void drawControls(Graphics2D g) {
        int cx = getWidth()/2;
        drawTitle(g, "Controls & Accessibility", cx, 100);
        String[] keys = controls.describe();
        int y = 160;
        for (int i=0;i<keys.length;i++) {
            boolean editing = (remapIndex == i);
            String label = (editing ? "[Press a key] " : "") + keys[i];
            drawBody(g, label, cx, y);
            y+= (int)(26*uiScale);
        }
        drawBody(g, "H: Toggle High Contrast   U: Toggle Large UI", cx, y+10);
        drawHint(g, "Esc: Back to Menu | Press number (1-"+(keys.length)+") to remap", cx, getHeight()-100);
    }

    private void drawTopBar(Graphics2D g) {
        g.setColor(new Color(0,0,0,120));
        g.fillRoundRect(10,10, getWidth()-20, (int)(36*uiScale), (int)(18*uiScale), (int)(18*uiScale));
        g.setColor(highContrast? Color.WHITE : new Color(230,240,255));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14*uiScale));
        String txt = String.format("Event: %s   Score: %d   Best: %d   Total: %d   [Space: Pause]",
                currentSport.getName(), currentSport.getScore(), scoreboard.getBest(currentSport.getName()), scoreboard.getTotal());
        g.drawString(txt, 22, (int)(10+24*uiScale));
    }

    private void drawCenterBox(Graphics2D g, String title, String[] lines) {
        int w = (int)(420*uiScale), h = (int)((80 + lines.length*24)*uiScale);
        int x = getWidth()/2 - w/2;
        int y = getHeight()/2 - h/2;
        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(x,y,w,h, (int)(18*uiScale), (int)(18*uiScale));
        g.setColor(highContrast? Color.WHITE : new Color(200,220,255));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 22*uiScale));
        g.drawString(title, x+20, y+30);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 16*uiScale));
        int yy = y+56;
        for (String line: lines) { g.drawString(line, x+20, yy); yy += (int)(22*uiScale); }
    }

    private void drawParticles(Graphics2D g) {
        for (Particle p: particles) {
            float a = Math.max(0, Math.min(1, p.life / p.maxLife));
            Composite prev = g.getComposite();
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, a));
            g.setColor(p.color);
            g.fill(new Ellipse2D.Float(p.x, p.y, p.size, p.size));
            g.setComposite(prev);
        }
    }

    private void drawTitle(Graphics2D g, String text, int cx, int y) {
        g.setColor(highContrast? Color.WHITE : new Color(240,245,255));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 36*uiScale));
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, cx - w/2, y);
    }

    private void drawBody(Graphics2D g, String text, int cx, int y) {
        g.setColor(highContrast? Color.WHITE : new Color(210,225,245));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 18*uiScale));
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, cx - w/2, y);
    }

    private void drawHint(Graphics2D g, String text, int cx, int y) {
        g.setColor(highContrast? Color.WHITE : new Color(180,200,230));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 14*uiScale));
        int w = g.getFontMetrics().stringWidth(text);
        g.drawString(text, cx - w/2, y);
    }

    private void drawSportCard(Graphics2D g, Sport s, int x, int y, int w, int h, boolean highlight) {
        g.setColor(new Color(0,0,0, highlight? 200:140));
        g.fillRoundRect(x, y, w, h, (int)(22*uiScale), (int)(22*uiScale));
        g.setColor(highlight? (highContrast? Color.WHITE : new Color(230,240,255)) : new Color(180,190,210));
        g.setFont(g.getFont().deriveFont(highlight? Font.BOLD: Font.PLAIN, (highlight? 22:18)*uiScale));
        String name = s.getName();
        int sw = g.getFontMetrics().stringWidth(name);
        g.drawString(name, x + w/2 - sw/2, y + (int)(32*uiScale));
        // thumbnail
        g.setColor(highContrast? Color.WHITE : new Color(120,140,200));
        g.setStroke(new BasicStroke(2f));
        s.drawThumbnail(g, x+10, y+ (int)(44*uiScale), w-20, h- (int)(54*uiScale));
    }

    private void drawScoreSummary(Graphics2D g, int cx, int y) {
        g.setColor(highContrast? Color.WHITE : new Color(210,225,245));
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 16*uiScale));
        String txt = String.format("Total Score: %d | Gold: %d  Silver: %d  Bronze: %d", scoreboard.getTotal(), scoreboard.gold, scoreboard.silver, scoreboard.bronze);
        int w = g.getFontMetrics().stringWidth(txt);
        g.drawString(txt, cx - w/2, y);
    }

    // ---------------------- Input ----------------------
    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        int kc = e.getKeyCode();
        if (state == GameState.CONTROLS && remapIndex >= 0) {
            controls.remap(remapIndex, kc);
            remapIndex = -1;
            repaint();
            return;
        }
        if (kc == KeyEvent.VK_U) { largeUI = !largeUI; repaint(); }
        if (kc == KeyEvent.VK_H) { highContrast = !highContrast; repaint(); }

        switch (state) {
            case MENU -> handleMenuKey(kc);
            case INTRO -> handleIntroKey(kc);
            case PLAYING -> handlePlayingKey(kc, true);
            case PAUSED -> handlePausedKey(kc);
            case RESULTS -> handleResultsKey(kc);
            case CONTROLS -> handleControlsKey(kc);
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        int kc = e.getKeyCode();
        if (state == GameState.PLAYING) handlePlayingKey(kc, false);
    }

    private void handleMenuKey(int kc) {
        if (kc == KeyEvent.VK_LEFT) { switchSport(sportIndex-1); repaint(); }
        if (kc == KeyEvent.VK_RIGHT){ switchSport(sportIndex+1); repaint(); }
        if (kc == KeyEvent.VK_ENTER){ state = GameState.INTRO; startFadeOut(); }
        if (kc == KeyEvent.VK_C){ state = GameState.CONTROLS; }
    }

    private void handleIntroKey(int kc) {
        if (kc == KeyEvent.VK_ESCAPE) { state = GameState.MENU; }
        if (kc == KeyEvent.VK_SPACE) { state = GameState.PLAYING; currentSport.reset(rng); startFadeOut(); }
    }

    private void handlePlayingKey(int kc, boolean pressed) {
        if (kc == KeyEvent.VK_SPACE && pressed) { state = GameState.PAUSED; return; }
        if (kc == KeyEvent.VK_ESCAPE && pressed) { state = GameState.MENU; return; }
        currentSport.onKey(controls, kc, pressed);
    }

    private void handlePausedKey(int kc) {
        if (kc == KeyEvent.VK_SPACE) { state = GameState.PLAYING; }
        if (kc == KeyEvent.VK_R) { currentSport.reset(rng); state = GameState.PLAYING; }
        if (kc == KeyEvent.VK_ESCAPE) { state = GameState.MENU; }
    }

    private void handleResultsKey(int kc) {
        if (kc == KeyEvent.VK_R) { currentSport.reset(rng); state = GameState.PLAYING; }
        if (kc == KeyEvent.VK_ENTER) {
            switchSport(sportIndex+1);
            state = GameState.INTRO;
        }
        if (kc == KeyEvent.VK_ESCAPE) { state = GameState.MENU; }
    }

    private void handleControlsKey(int kc) {
        if (kc == KeyEvent.VK_ESCAPE) { state = GameState.MENU; }
        if (kc >= KeyEvent.VK_1 && kc <= KeyEvent.VK_9) {
            int idx = kc - KeyEvent.VK_1;
            if (idx < controls.size()) remapIndex = idx;
        }
    }

    // ---------------------- Entities & Helpers ----------------------
    static class Particle {
        float x,y,vx,vy,gravity,size,life,maxLife; Color color;
        Particle(float x,float y,float vx,float vy,float gravity,float size,float life, Color c){
            this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.gravity=gravity;this.size=size;this.life=life;this.maxLife=life;this.color=c;
        }
    }

    interface Sport {
        String getName();
        void reset(Random rng);
        void update(ParalympicSportsGame app, Random rng, Controls ctrl, long tick);
        void draw(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick);
        void drawThumbnail(Graphics2D g, int x, int y, int w, int h);
        void drawResults(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick);
        String[] getIntroLines();
        int getScore();
        boolean isOver();
        void onKey(Controls ctrl, int keyCode, boolean pressed);
        java.util.List<Particle> emitParticles();
    }

    // Controls class with remapping capability
    static class Controls {
        // default bindings
        int LEFT = KeyEvent.VK_A;
        int RIGHT = KeyEvent.VK_D;
        int UP = KeyEvent.VK_W;
        int DOWN = KeyEvent.VK_S;
        int ACTION = KeyEvent.VK_J;     // primary
        int ACTION2 = KeyEvent.VK_K;    // secondary
        int ACTION3 = KeyEvent.VK_L;    // tertiary
        int MOD = KeyEvent.VK_SHIFT;    // modifier

        private final String[] names = new String[]{"Left","Right","Up","Down","Action","Action2","Action3","Modifier"};
        private final int[] codes = new int[]{LEFT,RIGHT,UP,DOWN,ACTION,ACTION2,ACTION3,MOD};

        boolean downLeft, downRight, downUp, downDown, downAction, downAction2, downAction3, downMod;

        int size(){ return names.length; }

        String[] describe(){
            String[] arr = new String[names.length];
            for (int i=0;i<names.length;i++) arr[i] = (i+1)+") "+names[i]+": "+KeyEvent.getKeyText(codes[i]);
            return arr;
        }
        void remap(int index, int keyCode){
            codes[index] = keyCode;
            LEFT=codes[0]; RIGHT=codes[1]; UP=codes[2]; DOWN=codes[3];
            ACTION=codes[4]; ACTION2=codes[5]; ACTION3=codes[6]; MOD=codes[7];
        }
        void setPressed(int keyCode, boolean pressed){
            if (keyCode==LEFT) downLeft=pressed;
            if (keyCode==RIGHT) downRight=pressed;
            if (keyCode==UP) downUp=pressed;
            if (keyCode==DOWN) downDown=pressed;
            if (keyCode==ACTION) downAction=pressed;
            if (keyCode==ACTION2) downAction2=pressed;
            if (keyCode==ACTION3) downAction3=pressed;
            if (keyCode==MOD) downMod=pressed;
        }
    }

    static class Scoreboard {
        private final Map<String,Integer> best = new LinkedHashMap<>();
        int gold=0,silver=0,bronze=0;
        int total=0;
        void submit(String sport, int score){
            total += score;
            int b = best.getOrDefault(sport, 0);
            if (score > b) best.put(sport, score);
            // simple medals by thresholds
            if (score >= 90) gold++; else if (score >= 70) silver++; else if (score >= 50) bronze++;
        }
        int getTotal(){ return total; }
        int getBest(String sport){ return best.getOrDefault(sport, 0); }
    }

    // ---------------------- Sport 1: Wheelchair Sprint ----------------------
    class WheelchairSprint implements Sport {
        private java.util.List<Rect> obstacles;
        private float laneX = 200; // player's x
        private float speed = 6f;
        private float y = getHeight()-120;
        private float distance; // meters
        private float stamina = 100f;
        private int score;
        private boolean over;
        private java.util.List<Particle> burst = new ArrayList<>();

        @Override public String getName() { return "Wheelchair Sprint"; }
        @Override public void reset(Random rng){
            obstacles = new ArrayList<>();
            float x=200; float gap=220; distance=0; stamina=100; speed=6; score=0; over=false;
            for (int i=0;i<40;i++) { // build course
                float oy = (float)(getHeight()-120 - i*gap);
                float ox = 120 + rng.nextInt(5)*120;
                obstacles.add(new Rect(ox, oy, 80, 20));
            }
        }
        @Override public void update(ParalympicSportsGame app, Random rng, Controls c, long t){
            burst.clear();
            if (over) return;
            // Input lane changing
            float laneSpeed = 8f;
            if (c.downLeft) laneX -= laneSpeed;
            if (c.downRight) laneX += laneSpeed;
            laneX = Math.max(80, Math.min(getWidth()-80, laneX));

            // Push rhythm (Action keys). Holding modifier gives a short boost but drains stamina
            if (c.downAction) speed += 0.12f;
            if (c.downAction2) speed += 0.12f;
            if (c.downMod) { speed += 0.2f; stamina -= 0.4f; }
            speed = Math.max(2f, Math.min(16f, speed*0.99f + 0.02f));
            stamina = Math.max(0f, Math.min(100f, stamina + (c.downAction||c.downAction2? -0.15f: +0.05f)));

            // Scroll world
            float scroll = speed;
            for (Rect r: obstacles) r.y += scroll;
            distance += scroll*0.06f; // meters rough

            // Collisions
            for (Rect r: obstacles) {
                if (r.y>y-10 && r.y<y+10 && Math.abs(r.x - laneX) < 60) {
                    speed *= 0.7f;
                    stamina -= 8f;
                    for (int i=0;i<8;i++) burst.add(new Particle(laneX,y, (rng.nextFloat()-0.5f)*6f, -rng.nextFloat()*4f, 0.25f, 6f, 30f, new Color(250,80,80)));
                }
            }
            // End condition
            if (distance >= 400) { // 400m sprint
                over = true;
                score = (int)Math.max(0, 120 - (int)(t/60) + (int)stamina/2);
            } else if (stamina <= 0) {
                over = true; score = (int)Math.max(0, distance/4);
            }
        }
        @Override public void draw(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){
            // Track lanes
            g.setColor(hc? Color.DARK_GRAY : new Color(60,65,80));
            g.fillRect(60, 80, getWidth()-120, getHeight()-160);
            g.setColor(hc? Color.WHITE : new Color(220,225,245,200));
            for (int x=120; x<getWidth()-120; x+=120) g.drawLine(x,80,x,getHeight()-80);

            // Obstacles
            g.setColor(hc? Color.YELLOW : new Color(255,200,80));
            for (Rect r: obstacles) g.fillRoundRect((int)r.x-40,(int)r.y-10,80,20,10,10);

            // Athlete (wheelchair simplified)
            int py = (int)y;
            g.setColor(hc? Color.WHITE : new Color(130,180,255));
            g.fillOval((int)laneX-28, py-24, 56, 56);
            g.setColor(hc? Color.LIGHT_GRAY : new Color(80,120,210));
            g.fillOval((int)laneX-40, py-6, 26, 26);
            g.fillOval((int)laneX+14, py-6, 26, 26);

            // HUD
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14*ui));
            g.setColor(hc? Color.WHITE: new Color(230,240,255));
            g.drawString(String.format("Distance: %.1fm", distance), 80, 60);
            drawBar(g, 260, 45, 160, 12, stamina/100f, hc? Color.WHITE : new Color(140,230,170));
            g.drawString("Stamina", 260, 40);
        }
        @Override public void drawThumbnail(Graphics2D g, int x, int y, int w, int h){
            g.drawRect(x,y,w,h);
            g.drawLine(x+w/4,y+h/2,x+w*3/4,y+h/2);
            g.fillOval(x+w/2-20,y+h/2-20,40,40);
        }
        @Override public void drawResults(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){
            drawResultsCard(g, "400m Sprint Complete", score, ui, hc);
        }
        @Override public String[] getIntroLines(){
            return new String[]{
                    "Push (J/K) to accelerate. Hold Shift for a short burst (drains stamina).",
                    "Use A/D to switch lanes and avoid track bumps.",
                    "Reach 400m!"
            }; }
        @Override public int getScore(){ return score; }
        @Override public boolean isOver(){ return over; }
        @Override public void onKey(Controls ctrl, int keyCode, boolean pressed){ ctrl.setPressed(keyCode, pressed); }
        @Override public java.util.List<Particle> emitParticles(){ return burst; }
    }

    // ---------------------- Sport 2: Goalball ----------------------
    class Goalball implements Sport {
        private float ballX, ballY, vx, vy;
        private float playerX = getWidth()/2f;
        private boolean over;
        private int score;
        private int saves;
        private int misses;
        private int rounds = 8;
        private long lastCueTick;
        private java.util.List<Particle> burst = new ArrayList<>();

        @Override public String getName(){ return "Goalball"; }
        @Override public void reset(Random rng){
            ballX=getWidth()/2f; ballY=120; vx=(rng.nextFloat()-0.5f)*4f; vy=3f; over=false; score=0; saves=0; misses=0; lastCueTick=0;
        }
        @Override public void update(ParalympicSportsGame app, Random rng, Controls c, long t){
            burst.clear();
            if (over) return;
            // Audio cue replacement: periodic visual cue pulses when ball is released
            if (t - lastCueTick > 60) { // every ~1s
                lastCueTick = t;
                for (int i=0;i<6;i++) burst.add(new Particle(ballX, ballY, (rng.nextFloat()-0.5f)*2f, -rng.nextFloat()*1.5f, 0.06f, 6f, 36f, new Color(200,220,255)));
            }
            // Player moves by LEFT/RIGHT or using Action keys
            float speed = 7f;
            if (c.downLeft) playerX -= speed;
            if (c.downRight) playerX += speed;
            playerX = Math.max(80, Math.min(getWidth()-80, playerX));

            // Ball physics
            ballX += vx; ballY += vy; vy += 0.06f; // low gravity
            if (ballX < 80 || ballX > getWidth()-80) vx *= -1;

            // Check reach bottom line (goal)
            if (ballY > getHeight()-120) {
                // save if player close
                if (Math.abs(playerX - ballX) < 60) { saves++; score += 12; vy = -3.6f; ballY = getHeight()-120; for (int i=0;i<10;i++) burst.add(new Particle(playerX, getHeight()-120, (rng.nextFloat()-0.5f)*3f, -rng.nextFloat()*2f, 0.1f, 5f, 28f, new Color(120,220,160))); }
                else { misses++; score -= 4; vy=-3.2f; ballY = getHeight()-120; for (int i=0;i<12;i++) burst.add(new Particle(ballX, getHeight()-120, (rng.nextFloat()-0.5f)*3f, -rng.nextFloat()*2f, 0.1f, 5f, 28f, new Color(250,110,110))); }
                if (saves+misses >= rounds) over=true;
            }
        }
        @Override public void draw(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){
            // Court
            g.setColor(hc? Color.DARK_GRAY : new Color(50,60,75));
            g.fillRect(80, 100, getWidth()-160, getHeight()-200);
            g.setColor(hc? Color.WHITE : new Color(220,230,245));
            g.drawRect(80, 100, getWidth()-160, getHeight()-200);
            g.drawLine(80, getHeight()-120, getWidth()-80, getHeight()-120);

            // Player bar (defender)
            g.setColor(hc? Color.WHITE : new Color(150,210,255));
            g.fillRoundRect((int)playerX-80, getHeight()-130, 160, 20, 10, 10);

            // Ball
            g.setColor(hc? Color.YELLOW : new Color(255,210,120));
            g.fillOval((int)ballX-10, (int)ballY-10, 20, 20);

            // HUD
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14*ui));
            g.setColor(hc? Color.WHITE : new Color(230,240,255));
            g.drawString("Saves: "+saves+"  Misses: "+misses+"  Rounds: "+(saves+misses)+"/"+rounds, 90, 80);
        }
        @Override public void drawThumbnail(Graphics2D g, int x, int y, int w, int h){
            g.drawOval(x+w/2-14, y+h/2-14, 28, 28);
            g.drawLine(x+10, y+h-20, x+w-10, y+h-20);
        }
        @Override public void drawResults(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){ drawResultsCard(g, "Goalball Result", score, ui, hc); }
        @Override public String[] getIntroLines(){ return new String[]{
                "Dodge-and-save: Move with A/D. Track the rolling ball and block it.",
                "Visual pulses mark the ball's movement as an audio cue analog.",
                "8 rounds. Each save +12, miss -4."}; }
        @Override public int getScore(){ return Math.max(0, score); }
        @Override public boolean isOver(){ return over; }
        @Override public void onKey(Controls ctrl, int keyCode, boolean pressed){ ctrl.setPressed(keyCode, pressed); }
        @Override public java.util.List<Particle> emitParticles(){ return burst; }
    }

    // ---------------------- Sport 3: Boccia ----------------------
    class Boccia implements Sport {
        private float jackX, jackY; // target ball
        private java.util.List<Point2D.Float> balls; // player balls
        private int throwsLeft;
        private boolean over;
        private int score;
        private float wind;
        private java.util.List<Particle> burst = new ArrayList<>();

        @Override public String getName(){ return "Boccia"; }
        @Override public void reset(Random rng){
            jackX = getWidth()/2f; jackY = getHeight()/2f - 40;
            balls = new ArrayList<>();
            throwsLeft = 6; over=false; score=0; wind = (rng.nextFloat()-0.5f)*0.8f;
        }
        @Override public void update(ParalympicSportsGame app, Random rng, Controls c, long t){
            burst.clear();
            if (over) return;
            // Throw when Action pressed
            if (c.downAction) {
                // angle with LEFT/RIGHT, power with UP/DOWN, modifier for finesse
            }
        }
        @Override public void draw(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){
            // Court
            g.setColor(hc? Color.DARK_GRAY : new Color(54,68,84));
            g.fillRect(100, 110, getWidth()-200, getHeight()-220);
            g.setColor(hc? Color.WHITE : new Color(220,230,245));
            g.drawRect(100,110,getWidth()-200,getHeight()-220);
            g.drawOval((int)jackX-6,(int)jackY-6,12,12);

            // Aim guidance & controls hint
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 14*ui));
            g.setColor(hc? Color.WHITE : new Color(220,235,255));
            g.drawString("Aim ←/→  Power ↑/↓  Throw J  Fine-tune with Shift  Wind: "+String.format("%.2f", wind), 110, 90);

            // Draw player balls
            g.setColor(hc? Color.YELLOW : new Color(255,160,160));
            for (Point2D.Float p: balls) g.fillOval((int)p.x-8,(int)p.y-8,16,16);

            // HUD
            g.setColor(hc? Color.WHITE : new Color(230,240,255));
            g.drawString("Throws left: "+throwsLeft+"  Score: "+score, getWidth()-300, 90);
        }

        // Simple firing model state
        float aimAngle = -1.2f; // radians
        float power = 8f;
        float ballVX, ballVY; boolean ballInAir=false; float bx,by;

        @Override public void onKey(Controls ctrl, int keyCode, boolean pressed){
            ctrl.setPressed(keyCode, pressed);
            if (pressed && keyCode == ctrl.ACTION && !ballInAir && throwsLeft>0) {
                // Fire
                float pow = power * (ctrl.downMod? 0.75f: 1f);
                bx = getWidth()/2f; by = getHeight()-140f;
                ballVX = (float)Math.cos(aimAngle)*pow + wind;
                ballVY = (float)Math.sin(aimAngle)*pow * -1f;
                ballInAir = true;
                throwsLeft--;
            }
            if (!pressed) return;
            if (keyCode == ctrl.LEFT) aimAngle -= ctrl.downMod? 0.04f: 0.08f;
            if (keyCode == ctrl.RIGHT) aimAngle += ctrl.downMod? 0.04f: 0.08f;
            if (keyCode == ctrl.UP) power = Math.min(14f, power + (ctrl.downMod? 0.2f: 0.4f));
            if (keyCode == ctrl.DOWN) power = Math.max(4f, power - (ctrl.downMod? 0.2f: 0.4f));
        }

        @Override public void update(ParalympicSportsGame app, Random rng, Controls c, long t){
            // Apply flight
            if (ballInAir){
                bx += ballVX; by += ballVY; ballVY += 0.25f; // gravity
                if (by >= getHeight()-120) { // land
                    by = getHeight()-120;
                    balls.add(new Point2D.Float(bx,by));
                    ballInAir=false;
                    float d = (float)Point2D.distance(bx,by, jackX, jackY);
                    int pts = Math.max(0, 40 - (int)d/6);
                    score += pts;
                }
            }
            if (throwsLeft==0 && !ballInAir) over=true;
        }

        @Override public void drawThumbnail(Graphics2D g, int x, int y, int w, int h){
            g.drawOval(x+w/2-8, y+h/2-8, 16, 16);
            g.drawOval(x+w/2-2, y+h/2-2, 4, 4);
        }
        @Override public void drawResults(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){ drawResultsCard(g, "Boccia Result", score, ui, hc); }
        @Override public String[] getIntroLines(){ return new String[]{
                "Precision toss toward the jack (small white ball).",
                "Aim with A/D, Power with W/S, Throw with J. Shift for fine control.",
                "6 throws. Closer is better."}; }
        @Override public int getScore(){ return Math.max(0, score); }
        @Override public boolean isOver(){ return over; }
        @Override public java.util.List<Particle> emitParticles(){ return burst; }
    }

    // ---------------------- Sport 4: Sitting Volleyball ----------------------
    class SittingVolleyball implements Sport {
        private float ballX, ballY, vx, vy;
        private boolean over;
        private int score;
        private int touches;
        private java.util.List<Particle> burst = new ArrayList<>();

        @Override public String getName(){ return "Sitting Volleyball"; }
        @Override public void reset(Random rng){
            ballX = getWidth()/2f; ballY = getHeight()/2f; vx = 3f; vy = -5f; over=false; score=0; touches=0;
        }
        @Override public void update(ParalympicSportsGame app, Random rng, Controls c, long t){
            burst.clear();
            if (over) return;
            ballX += vx; ballY += vy; vy += 0.22f;
            if (ballX < 120 || ballX > getWidth()-120) vx *= -1;

            // Bump controls - keep ball aloft
            if ((c.downAction || c.downAction2 || c.downAction3) && Math.abs(ballY - (getHeight()-180)) < 26) {
                vy = -6.8f; // bump up
                vx += (c.downLeft? -1.2f: 0) + (c.downRight? 1.2f: 0);
                touches++; score += 5; for (int i=0;i<8;i++) burst.add(new Particle(ballX, ballY, (rng.nextFloat()-0.5f)*3f, -rng.nextFloat()*3f, 0.1f, 5.5f, 28f, new Color(230,240,255)));
            }

            if (ballY > getHeight()-120) { over=true; score += touches*2; }
        }
        @Override public void draw(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){
            // Court with net
            g.setColor(hc? Color.DARK_GRAY : new Color(48,62,78));
            g.fillRect(120, 120, getWidth()-240, getHeight()-240);
            g.setColor(hc? Color.WHITE : new Color(220,230,245));
            g.drawRect(120, 120, getWidth()-240, getHeight()-240);
            g.drawLine(getWidth()/2, 120, getWidth()/2, getHeight()-120);

            // Ball
            g.setColor(hc? Color.YELLOW : new Color(255,215,120));
            g.fillOval((int)ballX-10, (int)ballY-10, 20, 20);

            // Players (seated) silhouettes
            g.setColor(hc? Color.WHITE : new Color(150,210,255));
            g.fillRect(getWidth()/2 - 180, getHeight()-160, 80, 8);
            g.fillRect(getWidth()/2 + 100, getHeight()-160, 80, 8);

            // HUD
            g.setColor(hc? Color.WHITE : new Color(230,240,255));
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14*ui));
            g.drawString("Keep the ball up with J/K/L. A/D adds side control. Touches: "+touches+"  Score: "+score, 130, 100);
        }
        @Override public void drawThumbnail(Graphics2D g, int x, int y, int w, int h){
            g.drawOval(x+w/2-8, y+h/2-8, 16, 16);
            g.drawLine(x+w/2, y+10, x+w/2, y+h-10);
        }
        @Override public void drawResults(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){ drawResultsCard(g, "Sitting Volleyball Result", score, ui, hc); }
        @Override public String[] getIntroLines(){ return new String[]{
                "Bump the ball (J/K/L) near the floor to keep it aloft.",
                "Use A/D to nudge sideways. Each touch adds points.",
                "Ball landing ends the round."}; }
        @Override public int getScore(){ return Math.max(0, score); }
        @Override public boolean isOver(){ return over; }
        @Override public void onKey(Controls ctrl, int keyCode, boolean pressed){ ctrl.setPressed(keyCode, pressed); }
        @Override public java.util.List<Particle> emitParticles(){ return burst; }
    }

    // ---------------------- Sport 5: Para Swimming ----------------------
    class ParaSwimming implements Sport {
        private float swimmerX = 180;
        private float swimmerY = getHeight()/2f;
        private float vx = 0;
        private float stamina = 100f;
        private int laps = 2; // there and back
        private boolean backLeg = false; // change direction
        private boolean over;
        private int score;
        private java.util.List<Particle> burst = new ArrayList<>();

        @Override public String getName(){ return "Para Swimming"; }
        @Override public void reset(Random rng){
            swimmerX = 180; swimmerY = getHeight()/2f; vx=0; stamina=100; laps=2; backLeg=false; over=false; score=0; burst.clear();
        }
        @Override public void update(ParalympicSportsGame app, Random rng, Controls c, long t){
            burst.clear();
            if (over) return;
            // Rhythm taps: J/K alternate yields best speed; holding is less effective
            float accel = 0.0f;
            if (c.downAction ^ c.downAction2) accel = 0.35f; // one pressed
            if (c.downAction && c.downAction2) accel = 0.15f; // both held
            if (c.downMod) { accel += 0.12f; stamina -= 0.3f; }
            vx = Math.max(0, Math.min(8f, vx*0.98f + accel));
            stamina = Math.max(0f, Math.min(100f, stamina + (accel>0? -0.15f: +0.06f)));

            swimmerX += vx * (backLeg? -1f: +1f);

            // turn at walls
            if (!backLeg && swimmerX > getWidth()-200) { backLeg = true; laps--; sparkle(swimmerX, swimmerY, new Color(170,220,255)); }
            if (backLeg && swimmerX < 180) { backLeg = false; laps--; sparkle(swimmerX, swimmerY, new Color(170,220,255)); }

            if (laps <= 0 || stamina <= 0) { over = true; score = (int)(vx*20 + stamina); }
        }
        private void sparkle(float x,float y, Color c){ for (int i=0;i<12;i++) burst.add(new Particle(x,y,(rng.nextFloat()-0.5f)*2f,(rng.nextFloat()-0.5f)*2f,0.02f,4f,30f,c)); }
        @Override public void draw(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){
            // Pool
            g.setColor(hc? Color.DARK_GRAY : new Color(30,60,90));
            g.fillRect(160, 120, getWidth()-320, getHeight()-240);
            g.setColor(hc? Color.WHITE : new Color(220,230,245));
            for (int y=140; y<getHeight()-140; y+=40) g.drawLine(160, y, getWidth()-160, y);

            // Lanes
            for (int x=220; x<getWidth()-220; x+=200) {
                g.setColor(hc? Color.GRAY : new Color(100,140,200));
                g.drawLine(x, 120, x, getHeight()-120);
            }

            // Swimmer (simple capsule)
            int sx = (int)swimmerX, sy = (int)swimmerY;
            g.setColor(hc? Color.WHITE : new Color(200,230,255));
            g.fillRoundRect(sx-30, sy-12, 60, 24, 12, 12);
            g.fillOval(sx+12, sy-10, 18, 18);

            // Direction arrow
            g.setColor(hc? Color.YELLOW : new Color(255,220,130));
            if (!backLeg) g.fillPolygon(new int[]{sx+40,sx+60,sx+40}, new int[]{sy-14,sy,sy+14}, 3);
            else g.fillPolygon(new int[]{sx-40,sx-60,sx-40}, new int[]{sy-14,sy,sy+14}, 3);

            // HUD
            g.setFont(g.getFont().deriveFont(Font.BOLD, 14*ui));
            g.setColor(hc? Color.WHITE : new Color(230,240,255));
            g.drawString("Alternate J/K to swim. Shift for a brief kick (uses stamina). Laps left: "+Math.max(0,laps)+"  Speed: "+String.format("%.1f", vx), 170, 100);
            drawBar(g, getWidth()-320, 90, 140, 12, stamina/100f, hc? Color.WHITE : new Color(140,230,170));
            g.drawString("Stamina", getWidth()-320, 85);
        }
        @Override public void drawThumbnail(Graphics2D g, int x, int y, int w, int h){
            g.drawRect(x,y,w,h);
            g.drawLine(x+10,y+h/2,x+w-10,y+h/2);
            g.drawOval(x+w/2-10,y+h/2-10,20,20);
        }
        @Override public void drawResults(ParalympicSportsGame app, Graphics2D g, boolean hc, float ui, long tick){ drawResultsCard(g, "Para Swimming Result", score, ui, hc); }
        @Override public String[] getIntroLines(){ return new String[]{
                "Swim there and back. Alternate J/K for best rhythm.",
                "Shift gives a quick kick but drains stamina.",
                "Finish the laps before stamina runs out."}; }
        @Override public int getScore(){ return Math.max(0, score); }
        @Override public boolean isOver(){ return over; }
        @Override public void onKey(Controls ctrl, int keyCode, boolean pressed){ ctrl.setPressed(keyCode, pressed); }
        @Override public java.util.List<Particle> emitParticles(){ return burst; }
    }

    // ---------------------- Common UI helpers ----------------------
    static class Rect { float x,y,w,h; Rect(float x,float y,float w,float h){this.x=x;this.y=y;this.w=w;this.h=h;} }

    static void drawBar(Graphics2D g, int x, int y, int w, int h, float t, Color c){
        t = Math.max(0, Math.min(1, t));
        g.setColor(new Color(0,0,0,160));
        g.fillRoundRect(x,y,w,h, h,h);
        g.setColor(c);
        int ww = (int)(w*t);
        g.fillRoundRect(x,y,ww,h, h,h);
        g.setColor(new Color(255,255,255,40));
        g.drawRoundRect(x,y,w,h, h,h);
    }

    static void drawResultsCard(Graphics2D g, String title, int score, float ui, boolean hc){
        int w = (int)(460*ui), h = (int)(160*ui);
        int x = 270, y = 180;
        g.setColor(new Color(0,0,0,180));
        g.fillRoundRect(x,y,w,h, (int)(24*ui),(int)(24*ui));
        g.setColor(hc? Color.WHITE : new Color(210,225,245));
        g.setFont(g.getFont().deriveFont(Font.BOLD, 22*ui));
        g.drawString(title, x+20, y+36);
        g.setFont(g.getFont().deriveFont(Font.PLAIN, 16*ui));
        g.drawString("Score: "+score, x+20, y+70);
        g.drawString("Enter: Next | R: Replay | Esc: Menu", x+20, y+100);
    }

    // ---------------------- Main ----------------------
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Paralympic Sports Game (Swing)");
            ParalympicSportsGame app = new ParalympicSportsGame();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setContentPane(app);
            f.pack();
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }
}
