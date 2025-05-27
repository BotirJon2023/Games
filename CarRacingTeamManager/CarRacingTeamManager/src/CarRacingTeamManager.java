import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.Timer;


public class CarRacingTeamManager extends JFrame implements ActionListener {
    private RaceTrackPanel raceTrackPanel;
    private ControlPanel controlPanel;

    public CarRacingTeamManager() {
        setTitle("Car Racing Team Manager");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        raceTrackPanel = new RaceTrackPanel();
        controlPanel = new ControlPanel(raceTrackPanel);

        add(raceTrackPanel, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.EAST);

        setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CarRacingTeamManager::new);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        // Not used in this example.
    }
}

class RaceTrackPanel extends JPanel implements ActionListener {
    private final java.util.List<Car> cars;
    private final Timer timer;
    private final int trackLength = 900;

    public RaceTrackPanel() {
        setBackground(Color.DARK_GRAY);
        cars = new ArrayList<>();
        timer = new Timer(30, this);
        timer.start();
    }

    public void addCar(String name, Color color) {
        Car car = new Car(name, color, cars.size());
        cars.add(car);
    }

    public void startRace() {
        for (Car car : cars) {
            car.startMoving();
        }
    }

    public void stopRace() {
        for (Car car : cars) {
            car.stopMoving();
        }
    }

    public void resetRace() {
        for (Car car : cars) {
            car.reset();
        }
        repaint();
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        for (Car car : cars) {
            car.draw(g);
        }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        for (Car car : cars) {
            car.move(trackLength);
        }
        repaint();
    }
}

class ControlPanel extends JPanel {
    private final RaceTrackPanel raceTrackPanel;
    private final JTextField nameField;
    private final JComboBox<String> colorSelector;
    private final JButton addCarButton;
    private final JButton startButton;
    private final JButton stopButton;
    private final JButton resetButton;

    public ControlPanel(RaceTrackPanel panel) {
        this.raceTrackPanel = panel;
        setLayout(new GridLayout(0, 1, 10, 10));

        nameField = new JTextField();
        colorSelector = new JComboBox<>(new String[]{"Red", "Green", "Blue", "Yellow", "Cyan"});
        addCarButton = new JButton("Add Car");
        startButton = new JButton("Start Race");
        stopButton = new JButton("Stop Race");
        resetButton = new JButton("Reset Race");

        add(new JLabel("Car Name:"));
        add(nameField);
        add(new JLabel("Car Color:"));
        add(colorSelector);
        add(addCarButton);
        add(startButton);
        add(stopButton);
        add(resetButton);

        addCarButton.addActionListener(e -> {
            String name = nameField.getText();
            Color color = getColor((String) colorSelector.getSelectedItem());
            raceTrackPanel.addCar(name, color);
            nameField.setText("");
        });

        startButton.addActionListener(e -> raceTrackPanel.startRace());
        stopButton.addActionListener(e -> raceTrackPanel.stopRace());
        resetButton.addActionListener(e -> raceTrackPanel.resetRace());
    }

    private Color getColor(String colorName) {
        switch (colorName) {
            case "Red": return Color.RED;
            case "Green": return Color.GREEN;
            case "Blue": return Color.BLUE;
            case "Yellow": return Color.YELLOW;
            case "Cyan": return Color.CYAN;
            default: return Color.BLACK;
        }
    }
}

class Car {
    private final String name;
    private final Color color;
    private final int lane;
    private int x;
    private boolean moving;

    public Car(String name, Color color, int lane) {
        this.name = name;
        this.color = color;
        this.lane = lane;
        this.x = 10;
        this.moving = false;
    }

    public void startMoving() {
        moving = true;
    }

    public void stopMoving() {
        moving = false;
    }

    public void reset() {
        x = 10;
        moving = false;
    }

    public void move(int max) {
        if (moving && x < max - 60) {
            x += new Random().nextInt(5);
        }
    }

    public void draw(Graphics g) {
        g.setColor(color);
        g.fillRect(x, 50 + lane * 60, 50, 30);
        g.setColor(Color.WHITE);
        g.drawString(name, x + 5, 70 + lane * 60);
    }
}

