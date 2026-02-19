import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.io.BufferedInputStream;
import java.io.FileInputStream;

import AudioStream.AudioStream;
import sun.audio.*;
import java.io.*;

public class Pong extends JFrame implements KeyListener, ActionListener
{
    private Paddle paddleOne;
    private Paddle paddleTwo;
    private PongPanel panel;
    private int score1;
    private int score2;
    private Timer timer;
    private boolean [] key;
    private boolean playing;
    private boolean twoPlayer;
    private Ball ball;
    private SoundPlayer sound;

    private int height;
    private int width;

    private int difficulty;

    private final static int BALL_STARTX = 255;
    private final static int BALL_STARTY = 190;

    private final static int PADDLE_X = 30;
    private final static int PADDLE2_X = 470;

    /*
     * up=38
     * down=40
     * left=37
     * right=39
     * w=87
     * s=83
     * a=65
     * d=68
     */
    private final static int UPKEY = 38;
    private final static int DOWNKEY = KeyEvent.VK_DOWN;

    private final static int WKEY = 87;
    private final static int SKEY = 83;

    private final static int BORDER_SIZE = 10;

    public Pong ()
    {
        super ("Pong");
        setSize ( 500 , 400 );
        setResizable ( false );
        setDefaultCloseOperation ( JFrame.EXIT_ON_CLOSE );

        sound = new SoundPlayer ();

        playing = false;
        twoPlayer = true;
        score1 = 0;
        score2 = 0;
        difficulty = 1;

        panel = new PongPanel ();
        Container c = getContentPane ();
        c.add ( panel );

        key = new boolean [ 256 ];
        paddleOne = null;
        paddleTwo = null;

        JMenuBar menuBar = new JMenuBar ();
        setJMenuBar ( menuBar );
        menuBar.add ( createGameMenu ());
        menuBar.add ( createSettingsMenu ());

        requestFocus ();
        addKeyListener ( this );
        panel.repaint ();

        timer = new Timer ( 30 , this );

    }

    public static void main ( String [] args )
    {
        Pong p = new Pong ();
        p.setVisible ( true );
    }

    public static void wait ( int ms )
    {
        try {
            Thread.currentThread ().sleep ( ms );
        }
        catch ( InterruptedException e ) {
            e.printStackTrace ();
        }
    }

    public void keyPressed ( KeyEvent e )
    {
        key [ e.getKeyCode ()]= true;
    }

    public void keyReleased ( KeyEvent e )
    {
        key [ e.getKeyCode ()]= false;
    }

    public void keyTyped ( KeyEvent e )
    {
    }

    public void score ( int player )
    {
        timer.stop ();
        if ( player == 1 )
            score2 ++;
        else if ( player == 2 )
            score1 ++;
        sound.playSound ( "point" );
        if (! checkWinner ())
            timer.start ();
        ball.reset ();
        wait ( 1000 );
    }

    public boolean checkWinner ()
    {
        if ( score1 > 11 )
        {
            if ( twoPlayer )
                setTitle ( "Player One Wins!" );
            else
                setTitle ( "YOU LOSE" );
            playing = false;
            timer.stop ();
            sound.playSound ( "winner" );
            if (! twoPlayer )
                sound.playSound ( "lost" );
            return true;
        }
        else if ( score2 > 11 )
        {
            if ( twoPlayer )
                setTitle ( "Player Two Wins!" );
            else
                setTitle ( "YOU WIN!!" );
            playing = false;
            timer.stop ();
            sound.playSound ( "winner" );
            if (! twoPlayer )
                sound.playSound ( "winner" );
            return true;
        }
        return false;
    }

    public void actionPerformed ( ActionEvent e )
    {
        ball.move ();
        if (! twoPlayer )
            compStep ++;
        panel.repaint ();
    }

    public JMenu createGameMenu ()
    {
        JMenu menu = new JMenu ( "Game" );
        menu.add ( newGame ());
        menu.add ( onePlayer ());
        menu.add ( twoPlayer ());
        menu.add ( exit ());
        return menu;

    }

    public JMenu createSettingsMenu ()
    {
        JMenu menu = new JMenu ( "Settings" );
        menu.add ( increaseDifficulty ());
        menu.add ( decreaseDifficulty ());
        return menu;

    }

    public JMenuItem newGame ()
    {
        JMenuItem item = new JMenuItem ( "New Game" );
        class MenuItemListener implements ActionListener
        {
            public void actionPerformed ( ActionEvent e )
            {
                resetScores ();
                startGame ();
            }
        }
        ActionListener l = new MenuItemListener ();
        item.addActionListener ( l );
        return item;
    }

    public JMenuItem increaseDifficulty ()
    {
        JMenuItem item = new JMenuItem ( "Increase Difficulty" );
        class MenuItemListener implements ActionListener
        {
            public void actionPerformed ( ActionEvent e )
            {
                if (! twoPlayer && difficulty < 6 )
                    difficulty ++;
            }
        }
        ActionListener l = new MenuItemListener ();
        item.addActionListener ( l );
        return item;
    }

    public JMenuItem decreaseDifficulty ()
    {
        JMenuItem item = new JMenuItem ( "Decrease Difficulty" );
        class MenuItemListener implements ActionListener
        {
            public void actionPerformed ( ActionEvent e )
            {
                if (! twoPlayer && difficulty > 1 )
                    difficulty --;
            }
        }
        ActionListener l = new MenuItemListener ();
        item.addActionListener ( l );
        return item;
    }

    public JMenuItem onePlayer ()
    {
        JMenuItem item = new JMenuItem ( "One Player" );
        class MenuItemListener implements ActionListener
        {
            public void actionPerformed ( ActionEvent e )
            {
                twoPlayer = false;
            }
        }
        ActionListener l = new MenuItemListener ();
        item.addActionListener ( l );
        return item;
    }

    public JMenuItem twoPlayer ()
    {
        JMenuItem item = new JMenuItem ( "Two Player" );
        class MenuItemListener implements ActionListener
        {
            public void actionPerformed ( ActionEvent e )
            {
                twoPlayer = true;
            }
        }
        ActionListener l = new MenuItemListener ();
        item.addActionListener ( l );
        return item;
    }

    public JMenuItem exit ()
    {
        JMenuItem item = new JMenuItem ( "Exit" );
        class MenuItemListener implements ActionListener
        {
            public void actionPerformed ( ActionEvent e )
            {
                System.exit ( 0 );
            }
        }
        ActionListener l = new MenuItemListener ();
        item.addActionListener ( l );
        return item;
    }

    private int compStep;

    public void compMove ( Paddle p , int dif )
    {
        if ( dif == 6 )
            p.moveTo ( ball.getY ());
        else if ( compStep %( 6 - dif )== 0 )
        {
            if ( ball.getY ()< p.getY ())
                p.moveUp ();
            else if ( ball.getY ()> p.getY ()+ p.getLength ())
                p.moveDown ();
            compStep = 0;
        }

    }

    public void startGame ()
    {
        playing = true;
        score1 = 0;
        score2 = 0;
        paddleOne = new Paddle ( 40 , 330 , Color.WHITE );
        paddleTwo = new Paddle ( 40 , 330 , Color.WHITE );
        ball = new Ball ( 5 , new Location ( BALL_STARTX , BALL_STARTY ), 470 , 330 , Color.WHITE );
        ball.reset ();
        timer.start ();
    }

    public void resetScores ()
    {
        score1 = 0;
        score2 = 0;
    }

    public class SoundPlayer
    {
        private String file;

        public SoundPlayer ()
        {
            file = null;
        }

        public void playSound ( String name )
        {
            final String s = name;
            new Thread () {
                public void run ()
                {
                    try {
                        FileInputStream fis = new FileInputStream ( s + ".mp3" );
                        AudioStream as = new AudioStream ( fis );
                        Object AudioPlayer = null;
                        AudioPlayer.player.start ( as );
                    }
                    catch ( Exception e )
                    {
                        System.out.println ( "Problem playing file " + s + ".mp3" );
                        System.out.println ( e );
                    }
                }
            }.start ();
        }
    }

    public class PongPanel extends JPanel
    {
        public PongPanel ()
        {
            setBackground ( Color.BLACK );
        }

        public void paintComponent ( Graphics g )
        {
            super.paintComponent ( g );
            g.setColor ( Color.DARK_GRAY );
            g.fillRect ( 20 , 40 , 460 , 10 );
            g.fillRect ( 20 , 40 , 10 , 300 );
            g.fillRect ( 480 , 40 , 10 , 300 );
            g.fillRect ( 20 , 330 , 460 , 10 );
            Font hugeFont = new Font ( "Helvetica" , Font.BOLD , 40 );
            g.setFont ( hugeFont );
            g.drawString ( score1 + "" , 125 , 40 );
            g.drawString ( score2 + "" , 375 , 40 );
            if (! twoPlayer )
            {
                Font af = new Font ( "Helvetica" , Font.BOLD , 12 );
                g.setFont ( af );
                if ( difficulty < 6 )
                    g.drawString ( "Computer Level" + difficulty , 180 , 40 );
                else
                    g.drawString ( "Computer Level 666" , 180 , 40 );
            }
            if ( playing )
            {
                if ( key [ UPKEY ])
                    paddleTwo.moveUp ();
                if ( key [ DOWNKEY ])
                    paddleTwo.moveDown ();
                if ( twoPlayer )
                {
                    if ( key [ WKEY ])
                        paddleOne.moveUp ();
                    if ( key [ SKEY ])
                        paddleOne.moveDown ();
                }
                else
                {
                    compMove ( paddleOne , difficulty );
                }
                g.setColor ( paddleOne.getColor ());
                g.fillRect ( PADDLE_X , paddleOne.getY (), paddleOne.WIDTH , paddleOne.getLength ());

                g.setColor ( paddleTwo.getColor ());
                g.fillRect ( PADDLE2_X , paddleTwo.getY (), paddleTwo.WIDTH , paddleTwo.getLength ());
                g.setColor ( ball.getColor ());
                g.fillRect ( ball.getX (), ball.getY (), ball.getRadius ()* 2 , ball.getRadius ()* 2 );

            }
        }
    }

    public class Paddle
    {
        private Color color;
        private int yLoc;
        private int screenHeight;
        private int size;
        public final static int INCREMENT = 6;
        public final static int WIDTH = 10;

        public Paddle ()
        {
            size = 0;
            screenHeight = 0;
            yLoc = 0;
            color = Color.WHITE;
        }

        public Paddle ( int s , int screen , Color c )
        {
            size = s;
            screenHeight = screen;
            yLoc = BALL_STARTY -( s / 2 );
            color = c;
        }

        public int getY ()
        {
            return yLoc;
        }

        public void setColor ( Color c )
        {
            color = c;
        }

        public Color getColor ()
        {
            return color;
        }

        public int getLength ()
        {
            return size;
        }

        public void moveUp ()
        {
            if ( validPaddleLoc ( yLoc - INCREMENT ))
                yLoc -= INCREMENT;
            else if ( yLoc - INCREMENT < 0 )
                yLoc = 0;
        }

        public void moveDown ()
        {
            if ( validPaddleLoc ( yLoc + INCREMENT ))
                yLoc += INCREMENT;
            else if ( yLoc + size + INCREMENT > screenHeight )
                yLoc = screenHeight - size;
        }

        public void moveTo ( int y )
        {
            if ( validPaddleLoc ( y ))
                yLoc = y;
            else if ( y + size > screenHeight )
                yLoc = screenHeight - size;
            else if ( y < 0 )
                yLoc = 0;
        }

        public boolean validPaddleLoc ( int y )
        {
            return y >= BORDER_SIZE && y + size <= screenHeight - BORDER_SIZE;
        }
    }

    public class Ball
    {
        private Color co;
        private int radius;
        private Location loc;
        private int screenWidth;
        private int screenHeight;
        private int xdirection;
        private int ydirection;
        private int xInc;
        private int yInc;
        public static final int RIGHT = 1;
        public static final int LEFT = -1;
        public static final int DOWN = 1;
        public static final int UP = -1;

        private boolean moving;

        public Ball ()
        {
            radius = 0;
            co = Color.WHITE;
            loc = new Location ();
            screenWidth = 0;
            screenHeight = 0;
            xdirection = RIGHT;
            ydirection = UP;
            xInc = 0;
            yInc = 0;
            moving = false;
        }

        public Ball ( int r , Location l , int sw , int sh , Color c )
        {
            radius = r;
            co = c;
            loc = l;
            screenWidth = sw;
            screenHeight = sh;
            xInc = 2;
            yInc = 2;
            moving = false;
            reset ();
        }

        public void reset ()
        {
            loc.setX ( BALL_STARTX );
            loc.setY ( BALL_STARTY );
            xdirection = ( Math.random ()> 0.5 ? RIGHT : LEFT );
            ydirection = ( Math.random ()> 0.5 ? UP : DOWN );
        }

        public void move ()
        {
            moving = true;
            loc.setX ( loc.getX ()+ xdirection * xInc );
            loc.setY ( loc.getY ()+ ydirection * yInc );
            checkPaddle ();
            checkWall ();
        }

        public void checkWall ()
        {
            if ( loc.getX ()+ radius * 2 > screenWidth - BORDER_SIZE )
            {
                score ( 1 );
            }
            if ( loc.getX ()< BORDER_SIZE )
            {
                score ( 2 );
            }
            if ( loc.getY ()+ radius * 2 > screenHeight - BORDER_SIZE )
            {
                ydirection = UP;
                sound.playSound ( "wall" );
            }
            if ( loc.getY ()< BORDER_SIZE )
            {
                ydirection = DOWN;
                sound.playSound ( "wall" );
            }
        }

        public void checkPaddle ()
        {
            if ( xdirection == RIGHT )
            {
                if ( loc.getX ()+ radius * 2 > PADDLE2_X && loc.getX ()+ radius * 2 < PADDLE2_X + Paddle.WIDTH )
                {
                    if ( loc.getY ()+ radius > paddleTwo.getY () && loc.getY ()< paddleTwo.getY ()+ paddleTwo.getLength ())
                    {
                        xdirection = LEFT;
                        sound.playSound ( "paddle" );
                    }
                }
            }
            else if ( xdirection == LEFT )
            {
                if ( loc.getX ()< PADDLE_X + Paddle.WIDTH && loc.getX ()> PADDLE_X )
                {
                    if ( loc.getY ()+ radius > paddleOne.getY () && loc.getY ()< paddleOne.getY ()+ paddleOne.getLength ())
                    {
                        xdirection = RIGHT;
                        sound.playSound ( "paddle" );
                    }
                }
            }
        }

        public int getX ()
        {
            return loc.getX ();
        }

        public int getY ()
        {
            return loc.getY ();
        }

        public int getRadius ()
        {
            return radius;
        }

        public Color getColor ()
        {
            return co;
        }
    }

    public class Location
    {
        private int x;
        private int y;

        public Location ()
        {
            x = 0;
            y = 0;
        }

        public Location ( int a , int b )
        {
            x = a;
            y = b;
        }

        public void setX ( int xLoc )
        {
            x = xLoc;
        }

        public void setY ( int yLoc )
        {
            y = yLoc;
        }

        public int getX ()
        {
            return x;
        }

        public int getY ()
        {
            return y;
        }
    }
}