import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import javax.swing.*;
import java.util.*;
import java.util.List;

public class InputHandling {


    @Override
    public void keyTyped(KeyEvent e) {}

    @Override
    public void keyPressed(KeyEvent e) {
        keys.add(e.getKeyCode());

        // Global toggles
        if (e.getKeyCode() == KeyEvent.VK_V) {
            showFPS = !showFPS;
        }
        if (e.getKeyCode() == KeyEvent.VK_F) {
            // Toggle all FX sets (cycle)
            boolean anyOff = !fxGlow || !fxTrails || !fxParticles || !fxScreenShake;
            if (anyOff) {
                fxGlow = fxTrails = fxParticles = fxScreenShake = true;
            } else {
                fxGlow = !fxGlow;
                fxTrails = !fxTrails;
                fxParticles = !fxParticles;
                fxScreenShake = !fxScreenShake;
            }
        }

        if (state == State.MENU) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER || e.getKeyCode() == KeyEvent.VK_SPACE) {
                aiLeft = false; aiRight = false;
                scoreLeft = scoreRight = 0;
                state = State.COUNTDOWN;
                countdown = 2.2;
                servingDir = 0;
                balls.clear();
                spawnBall(0);
                hud("Get ready...");
            }
            if (e.getKeyCode() == KeyEvent.VK_1) {
                aiLeft = false; aiRight = true;
                scoreLeft = scoreRight = 0;
                state = State.COUNTDOWN;
                countdown = 2.2;
                servingDir = 0;
                balls.clear();
                spawnBall(0);
                hud("Left vs AI");
            }
            if (e.getKeyCode() == KeyEvent.VK_2) {
                aiLeft = true; aiRight = false;
                scoreLeft = scoreRight = 0;
                state = State.COUNTDOWN;
                countdown = 2.2;
                servingDir = 0;
                balls.clear();
                spawnBall(0);
                hud("Right vs AI");
            }
            if (e.getKeyCode() == KeyEvent.VK_3) {
                aiLeft = true; aiRight = true;
                scoreLeft = scoreRight = 0;
                state = State.COUNTDOWN;
                countdown = 2.2;
                servingDir = 0;
                balls.clear();
                spawnBall(0);
                hud("Watch mode");
            }
            if (e.getKeyCode() == KeyEvent.VK_S) {
                state = State.SETTINGS;
            }
            if (e.getKeyCode() == KeyEvent.VK_H) {
                state = State.HELP;
            }
            if (e.getKeyCode() == KeyEvent.VK_T) {
                applyTheme(themeIndex + 1);
            }
        } else if (state == State.PLAYING) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
            if (e.getKeyCode() == KeyEvent.VK_P || e.getKeyCode() == KeyEvent.VK_SPACE) {
                state = State.PAUSED;
                hud("Paused");
            }
        } else if (state == State.PAUSED) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
            if (e.getKeyCode() == KeyEvent.VK_P || e.getKeyCode() == KeyEvent.VK_SPACE) {
                state = State.PLAYING;
                hud("");
            }
        } else if (state == State.GAME_OVER) {
            if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                scoreLeft = scoreRight = 0;
                state = State.COUNTDOWN;
                countdown = 2.2;
                servingDir = 0;
                balls.clear();
                spawnBall(0);
                hud("New Game");
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
        } else if (state == State.COUNTDOWN) {
            if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                countdown = 0.01;
                hud("Serve!");
            }
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
        } else if (state == State.SETTINGS) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
            if (e.getKeyCode() == KeyEvent.VK_T) {
                applyTheme(themeIndex + 1);
            }
            if (e.getKeyCode() == KeyEvent.VK_MINUS || e.getKeyCode() == KeyEvent.VK_SUBTRACT) {
                winScore = clamp(winScore - 1, 1, 21);
            }
            if (e.getKeyCode() == KeyEvent.VK_EQUALS || e.getKeyCode() == KeyEvent.VK_ADD) {
                winScore = clamp(winScore + 1, 1, 21);
            }
            if (e.getKeyCode() == KeyEvent.VK_1) {
                aiLeft = !aiLeft;
            }
            if (e.getKeyCode() == KeyEvent.VK_2) {
                aiRight = !aiRight;
            }
        } else if (state == State.HELP) {
            if (e.getKeyCode() == KeyEvent.VK_ESCAPE) {
                state = State.MENU;
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keys.remove(e.getKeyCode());
    }

    @Override
    public void mouseClicked(MouseEvent e) {}

    @Override
    public void mousePressed(MouseEvent e) {}

    @Override
    public void mouseReleased(MouseEvent e) {}

    @Override
    public void mouseEntered(MouseEvent e) {}

    @Override
    public void mouseExited(MouseEvent e) {}

    @Override
    public void mouseDragged(MouseEvent e) {}

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void focusGained(FocusEvent e) {
        hasFocus = true;
    }

    @Override
    public void focusLost(FocusEvent e) {
        hasFocus = false;
        keys.clear();
    }

    @Override
    public void componentResized(ComponentEvent e) {}

    @Override
    public void componentMoved(ComponentEvent e) {}

    @Override
    public void componentShown(ComponentEvent e) {}

    @Override
    public void componentHidden(ComponentEvent e) {}


}
