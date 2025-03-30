import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Random;
import javax.sound.sampled.*;

public class CreepyDollHorrorAdventure extends JFrame {

    private JPanel mainPanel;
    private JTextArea storyTextArea;
    private JButton choice1Button;
    private JButton choice2Button;
    private JLabel imageLabel;
    private BufferedImage currentImage;
    private Clip backgroundMusic;
    private Clip soundEffect;

    private int gameState;
    private Random random = new Random();

    public CreepyDollHorrorAdventure() {
        setTitle("Creepy Doll Horror Adventure");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        mainPanel = new JPanel();
        mainPanel.setLayout(new BorderLayout());

        storyTextArea = new JTextArea();
        storyTextArea.setEditable(false);
        storyTextArea.setLineWrap(true);
        storyTextArea.setWrapStyleWord(true);
        storyTextArea.setFont(new Font("Arial", Font.PLAIN, 16));
        JScrollPane scrollPane = new JScrollPane(storyTextArea);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new FlowLayout());

        choice1Button = new JButton("Choice 1");
        choice2Button = new JButton("Choice 2");

        choice1Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleChoice(1);
            }
        });

        choice2Button.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                handleChoice(2);
            }
        });

        buttonPanel.add(choice1Button);
        buttonPanel.add(choice2Button);
        mainPanel.add(buttonPanel, BorderLayout.SOUTH);

        imageLabel = new JLabel();
        mainPanel.add(imageLabel, BorderLayout.NORTH);

        add(mainPanel);
        gameState = 0;
        startGame();
        playBackgroundMusic("background.wav");
    }

    private void startGame() {
        storyTextArea.setText("You find yourself in a dark, dusty attic. A single flickering light bulb illuminates a creepy doll sitting in the corner. What do you do?");
        choice1Button.setText("Approach the doll.");
        choice2Button.setText("Search for an exit.");
        gameState = 1;
        loadImage("attic.jpg");
    }

    private void handleChoice(int choice) {
        switch (gameState) {
            // ... (previous game states)
            case 25:
                storyTextArea.setText("The doll is too powerful. You cannot harm it.");
                choice1Button.setText("End Game");
                choice2Button.setVisible(false);
                gameState = 8;
                loadImage("powerful.jpg");
                playSoundEffect("powerful.wav");
                break;
            case 26:
                storyTextArea.setText("The darkness consumes you.");
                choice1Button.setText("End Game");
                choice2Button.setVisible(false);
                gameState = 8;
                loadImage("dark_consume.jpg");
                playSoundEffect("dark_consume.wav");
                break;
            case 27:
                if (choice == 1) {
                    storyTextArea.setText("The doll is weakened, but still dangerous.");
                    choice1Button.setText("Continue attacking.");
                    choice2Button.setText("Run.");
                    gameState = 31;
                    loadImage("weakened.jpg");
                } else {
                    storyTextArea.setText("The doll is too fast. It catches you.");
                    choice1Button.setText("End Game");
                    choice2Button.setVisible(false);
                    gameState = 8;
                    loadImage("fast.jpg");
                    playSoundEffect("fast.wav");
                }
                break;
            case 28:
                storyTextArea.setText("You run past the doll. It screams in anger.");
                choice1Button.setText("Continue to the hallway.");
                choice2Button.setText("Search for more weapons.");
                gameState = 32;
                loadImage("angry.jpg");
                break;
            case 29:
                storyTextArea.setText("You attack. The doll is harmed, but it is still strong.");
                choice1Button.setText("Continue attacking.");
                choice2Button.setText("Run.");
                gameState = 33;
                loadImage("strong.jpg");
                break;
            case 30:
                storyTextArea.setText("You have won. You are safe.");
                choice1Button.setText("Restart");
                choice2Button.setVisible(false);
                choice1Button.addActionListener(e -> {
                    gameState = 0;
                    startGame();
                    choice2Button.setVisible(true);
                    playBackgroundMusic("background.wav");
                });
                break;
            case 31:
                storyTextArea.setText("You defeat the doll!");
                choice1Button.setText("You win!");
                choice2Button.setVisible(false);
                gameState = 30;
                loadImage("win3.jpg");
                break;
            case 32:
                storyTextArea.setText("You continue to the hallway, leaving the doll behind.");
                choice1Button.setText("Continue down the hallway.");
                choice2Button.setText("Go back to the attic.");
                gameState = 26;
                loadImage("leave_doll.jpg");
                break;
            case 33:
                storyTextArea.setText("The doll is too strong. It defeats you.");
                choice1Button.setText("End Game");
                choice2Button.setVisible(false);
                gameState = 8;
                loadImage("defeat.jpg");
                playSoundEffect("defeat.wav");
                break;
        }
    }

    private void loadImage(String imageName) {
        try {
            currentImage = ImageIO.read(new File(imageName));
            imageLabel.setIcon(new ImageIcon(currentImage));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void playSoundEffect(String soundFileName) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(soundFileName));
            soundEffect = AudioSystem.getClip();
            soundEffect.open(audioInputStream);
            soundEffect.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    private void playBackgroundMusic(String musicFileName) {
        try {
            AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(musicFileName));
            backgroundMusic = AudioSystem.getClip();
            backgroundMusic.open(audioInputStream);
            backgroundMusic.loop(Clip.LOOP_CONTINUOUSLY);
            backgroundMusic.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                new CreepyDollHorrorAdventure().setVisible(true);
            }
        });
    }
}