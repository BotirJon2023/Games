import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class Rendering {



    @Override
    protected void paintComponent(Graphics gOuter) {
        super.paintComponent(gOuter);

        if (backBuffer == null || backBuffer.getWidth() != VIRTUAL_WIDTH || backBuffer.getHeight() != VIRTUAL_HEIGHT) {
            initBuffers();
        }

        Graphics2D g = backBuffer.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Trails layer fade
        if (fxTrails) {
            // draw a semi-transparent rect over trails to fade
            Composite oldC = trailG.getComposite();
            trailG.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, state == State.PLAYING ? 0.15f : 0.3f));
            trailG.setColor(themes.get(themeIndex).bg);
            trailG.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            trailG.setComposite(oldC);
        } else {
            // clear trails layer
            trailG.setComposite(AlphaComposite.Src);
            trailG.setColor(themes.get(themeIndex).bg);
            trailG.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
            trailG.setComposite(AlphaComposite.SrcOver);
        }

        // Base background
        g.setColor(themes.get(themeIndex).bg);
        g.fillRect(0, 0, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);

        // Screen shake
        double sx = fxScreenShake ? (rng.nextDouble()*2-1) * shakeMag : 0;
        double sy = fxScreenShake ? (rng.nextDouble()*2-1) * shakeMag : 0;
        g.translate(sx, sy);

        // Middle dashed divider
        g.setColor(themes.get(themeIndex).mid);
        int cx = VIRTUAL_WIDTH / 2;
        for (int y = 20; y < VIRTUAL_HEIGHT; y += 32) {
            g.fillRect(cx - 2, y, 4, 22);
        }

        // Scores
        g.setColor(themes.get(themeIndex).hud);
        drawCenteredString(g, Integer.toString(scoreLeft), new Rectangle((int)(VIRTUAL_WIDTH*0.5 - 140), 20, 120, 60), new Font("SansSerif", Font.BOLD, 48));
        drawCenteredString(g, Integer.toString(scoreRight), new Rectangle((int)(VIRTUAL_WIDTH*0.5 + 20), 20, 120, 60), new Font("SansSerif", Font.BOLD, 48));

        // Power-ups
        for (PowerUp p : powerUps) {
            p.draw(g);
        }

        // Paddles
        leftPaddle.draw(g);
        rightPaddle.draw(g);

        // Balls and their trails
        for (Ball b : balls) {
            if (fxTrails) {
                b.drawTrail(trailG);
            }
            b.draw(g);
        }

        // Particles
        for (Particle p : particles) {
            p.draw(g);
        }

        // state overlays
        switch (state) {
            case MENU:
                drawMenu(g);
                break;
            case COUNTDOWN:
                drawCountdown(g);
                break;
            case PAUSED:
                drawPause(g);
                break;
            case SETTINGS:
                drawSettings(g);
                break;
            case HELP:
                drawHelp(g);
                break;
            case GAME_OVER:
                drawGameOver(g);
                break;
            default:
                break;
        }

        // HUD message
        if (hudAlpha > 0.02) {
            g.setColor(new Color(1f,1f,1f,(float)Math.min(0.85, hudAlpha)));
            drawCenteredString(g, hudMessage, new Rectangle(0, VIRTUAL_HEIGHT - 44, VIRTUAL_WIDTH, 28), new Font("SansSerif", Font.BOLD, 18));
        }

        // overlay trails layer
        if (fxTrails) {
            g.drawImage(trailLayer, 0, 0, null);
        }

        // FPS
        if (showFPS) {
            g.setColor(Color.GRAY);
            g.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g.drawString(fpsValue + " FPS", 8, 14);
        }

        g.dispose();

        // Letterbox scaling to panel
        Graphics2D g2 = (Graphics2D) gOuter;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        Dimension sz = getSize();
        double scale = Math.min(sz.getWidth() / VIRTUAL_WIDTH, sz.getHeight() / VIRTUAL_HEIGHT);
        int drawW = (int)(VIRTUAL_WIDTH * scale);
        int drawH = (int)(VIRTUAL_HEIGHT * scale);
        int x = (sz.width - drawW) / 2;
        int y = (sz.height - drawH) / 2;
        g2.setColor(themes.get(themeIndex).bg);
        g2.fillRect(0, 0, sz.width, sz.height);
        g2.drawImage(backBuffer, x, y, drawW, drawH, null);
    }

    private void drawMenu(Graphics2D g) {
        g.setColor(Color.WHITE);
        drawCenteredString(g, "PING-PONG", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.22), VIRTUAL_WIDTH, 80), new Font("SansSerif", Font.BOLD, 64));
        g.setColor(new Color(0xCCDDEE));
        drawCenteredString(g, "Multiplayer (Local)", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.32), VIRTUAL_WIDTH, 40), new Font("SansSerif", Font.PLAIN, 22));

        int y = (int)(VIRTUAL_HEIGHT*0.46);
        g.setColor(new Color(0x9aaabb));
        drawCenteredString(g, "Enter: Start 2-Player", new Rectangle(0, y, VIRTUAL_WIDTH, 22), new Font("SansSerif", Font.PLAIN, 18)); y += 26;
        drawCenteredString(g, "1: Left vs AI", new Rectangle(0, y, VIRTUAL_WIDTH, 22), new Font("SansSerif", Font.PLAIN, 18)); y += 26;
        drawCenteredString(g, "2: Right vs AI", new Rectangle(0, y, VIRTUAL_WIDTH, 22), new Font("SansSerif", Font.PLAIN, 18)); y += 26;
        drawCenteredString(g, "3: AI vs AI (Watch)", new Rectangle(0, y, VIRTUAL_WIDTH, 22), new Font("SansSerif", Font.PLAIN, 18)); y += 26;
        drawCenteredString(g, "S: Settings   H: Help   T: Theme   F: Toggle FX   V: FPS", new Rectangle(0, y, VIRTUAL_WIDTH, 22), new Font("SansSerif", Font.PLAIN, 18)); y += 26;
    }

    private void drawPause(Graphics2D g) {
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Paused", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.35), VIRTUAL_WIDTH, 80), new Font("SansSerif", Font.BOLD, 48));
        g.setColor(new Color(0x9aaabb));
        drawCenteredString(g, "Space or P: Resume    Esc: Menu", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.45), VIRTUAL_WIDTH, 60), new Font("SansSerif", Font.PLAIN, 18));
    }

    private void drawGameOver(Graphics2D g) {
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Game Over", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.35), VIRTUAL_WIDTH, 80), new Font("SansSerif", Font.BOLD, 48));
        String winner = scoreLeft > scoreRight ? "Left Player Wins!" : "Right Player Wins!";
        drawCenteredString(g, winner, new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.42), VIRTUAL_WIDTH, 60), new Font("SansSerif", Font.BOLD, 28));
        g.setColor(new Color(0x9aaabb));
        drawCenteredString(g, "Enter: Restart   Esc: Menu", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.5), VIRTUAL_WIDTH, 60), new Font("SansSerif", Font.PLAIN, 18));
    }

    private void drawCountdown(Graphics2D g) {
        // 3-2-1 countdown scaling
        double t = Math.max(0, countdown);
        String num = t > 1.3 ? "3" : t > 0.6 ? "2" : "1";
        double p = t > 1.3 ? (t - 1.3) / 0.9 : t > 0.6 ? (t - 0.6) / 0.7 : (t / 0.6);
        double scale = 1 + backOut(1 - p, 1.70158) * 0.4;

        g.setColor(Color.WHITE);
        Font base = new Font("SansSerif", Font.BOLD, 90);
        FontMetrics fm = g.getFontMetrics(base);
        int w = fm.stringWidth(num);
        int h = fm.getAscent();
        int x = VIRTUAL_WIDTH / 2 - (int)(w * scale / 2);
        int y = (int)(VIRTUAL_HEIGHT * 0.35) + (int)(h * scale / 2);

        AffineTransform old = g.getTransform();
        g.translate(VIRTUAL_WIDTH / 2.0, VIRTUAL_HEIGHT * 0.35);
        g.scale(scale, scale);
        g.setFont(base);
        g.drawString(num, -w / 2, h / 2);
        g.setTransform(old);

        g.setColor(new Color(0x9aaabb));
        drawCenteredString(g, "Space: Serve early", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.5), VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 18));
    }

    private void drawSettings(Graphics2D g) {
        g.setColor(Color.WHITE);
        drawCenteredString(g, "Settings", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.3), VIRTUAL_WIDTH, 80), new Font("SansSerif", Font.BOLD, 48));
        g.setColor(new Color(0x9aaabb));
        int y = (int)(VIRTUAL_HEIGHT*0.4);
        drawCenteredString(g, "Theme (T): " + (themeIndex+1) + "/" + themes.size(), new Rectangle(0, y, VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 20)); y+=28;
        drawCenteredString(g, "Win Score (-/+): " + winScore, new Rectangle(0, y, VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 20)); y+=28;
        drawCenteredString(g, "Left AI (1): " + (aiLeft?"ON":"OFF"), new Rectangle(0, y, VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 20)); y+=28;
        drawCenteredString(g, "Right AI (2): " + (aiRight?"ON":"OFF"), new Rectangle(0, y, VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 20)); y+=28;
        drawCenteredString(g, "Visual FX (F): " + fxStates(), new Rectangle(0, y, VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 20)); y+=28;
        drawCenteredString(g, "FPS (V): " + (showFPS?"ON":"OFF"), new Rectangle(0, y, VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 20)); y+=28;
        drawCenteredString(g, "Esc: Back", new Rectangle(0, y, VIRTUAL_WIDTH, 30), new Font("SansSerif", Font.PLAIN, 20));
    }

    private String fxStates() {
        StringBuilder sb = new StringBuilder();
        if (fxGlow) sb.append("Glow ");
        if (fxTrails) sb.append("Trails ");
        if (fxParticles) sb.append("Particles ");
        if (fxScreenShake) sb.append("Shake");
        return sb.toString().trim();
    }

    private void drawHelp(Graphics2D g) {
        g.setColor(Color.WHITE);
        drawCenteredString(g, "How to Play", new Rectangle(0, (int)(VIRTUAL_HEIGHT*0.28), VIRTUAL_WIDTH, 60), new Font("SansSerif", Font.BOLD, 40));
        g.setColor(new Color(0x9aaabb));
        int y = (int)(VIRTUAL_HEIGHT*0.35);
        drawCenteredString(g, "Left: W/S   Right: Up/Down", new Rectangle(0, y, VIRTUAL_WIDTH, 28), new Font("SansSerif", Font.PLAIN, 18)); y+=26;
        drawCenteredString(g, "Space: Serve / Pause", new Rectangle(0, y, VIRTUAL_WIDTH, 28), new Font("SansSerif", Font.PLAIN, 18)); y+=26;
        drawCenteredString(g, "First to Win Score wins the game.", new Rectangle(0, y, VIRTUAL_WIDTH, 28), new Font("SansSerif", Font.PLAIN, 18)); y+=26;
        drawCenteredString(g, "Esc: Back", new Rectangle(0, y, VIRTUAL_WIDTH, 28), new Font("SansSerif", Font.PLAIN, 18));
    }

    private void drawCenteredString(Graphics2D g, String text, Rectangle rect, Font font) {
        Font old = g.getFont();
        g.setFont(font);
        FontMetrics metrics = g.getFontMetrics(font);
        int x = rect.x + (rect.width - metrics.stringWidth(text)) / 2;
        int y = rect.y + ((rect.height - metrics.getHeight()) / 2) + metrics.getAscent();
        g.drawString(text, x, y);
        g.setFont(old);
    }

    private double backOut(double t, double s) {
        t = clamp(t, 0, 1);
        double inv = 1 - t;
        return 1 - (inv*inv*((s+1)*inv - s));
    }

}
