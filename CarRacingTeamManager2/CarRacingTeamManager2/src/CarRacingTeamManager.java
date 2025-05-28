import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Random;

public class CarRacingTeamManager extends JFrame {
    private Team team;
    private RaceTrackPanel raceTrackPanel;
    private TeamManagementPanel teamManagementPanel;
    private JButton startRaceButton;
    private JLabel statusLabel;
    private Timer animationTimer;
    private boolean isRaceRunning;
    private ArrayList<RaceCar> raceCars;
    private int raceDistance = 1000; // Total race distance in arbitrary units
    private int currentLap;
    private int totalLaps = 3;

    public CarRacingTeamManager() {
        setTitle("Car Racing Team Manager");
        setSize(1200, 800);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());
        team = new Team("Player Team", 100000);
        raceCars = new ArrayList<>();
        isRaceRunning = false;
        currentLap = 1;

        // Initialize panels
        raceTrackPanel = new RaceTrackPanel();
        teamManagementPanel = new TeamManagementPanel();
        startRaceButton = new JButton("Start Race");
        statusLabel = new JLabel("Welcome to Car Racing Team Manager!", SwingConstants.CENTER);

        // Layout setup
        add(raceTrackPanel, BorderLayout.CENTER);
        add(teamManagementPanel, BorderLayout.EAST);
        add(startRaceButton, BorderLayout.SOUTH);
        add(statusLabel, BorderLayout.NORTH);

        // Animation timer for car movement
        animationTimer = new Timer(50, e -> {
            if (isRaceRunning) {
                updateRace();
                raceTrackPanel.repaint();
            }
        });

        // Start race button action
        startRaceButton.addActionListener(e -> startRace());

        setVisible(true);
    }

    private void startRace() {
        if (team.getCars().isEmpty()) {
            statusLabel.setText("Add at least one car to the team before racing!");
            return;
        }
        if (!isRaceRunning) {
            isRaceRunning = true;
            raceCars.clear();
            for (Car car : team.getCars()) {
                raceCars.add(new RaceCar(car, team.getDrivers().get(team.getCars().indexOf(car))));
            }
            currentLap = 1;
            statusLabel.setText("Race started! Lap " + currentLap + "/" + totalLaps);
            animationTimer.start();
            startRaceButton.setText("Stop Race");
        } else {
            stopRace();
        }
    }

    private void stopRace() {
        isRaceRunning = false;
        animationTimer.stop();
        startRaceButton.setText("Start Race");
        statusLabel.setText("Race stopped. Prepare for the next race!");
    }

    private void updateRace() {
        boolean raceFinished = true;
        for (RaceCar raceCar : raceCars) {
            if (raceCar.distance < raceDistance) {
                raceCar.updatePosition();
                raceFinished = false;
            }
        }
        if (raceFinished && currentLap < totalLaps) {
            currentLap++;
            statusLabel.setText("Lap " + currentLap + "/" + totalLaps);
            for (RaceCar raceCar : raceCars) {
                raceCar.distance = 0; // Reset distance for new lap
            }
        } else if (raceFinished) {
            endRace();
        }
    }

    private void endRace() {
        isRaceRunning = false;
        animationTimer.stop();
        startRaceButton.setText("Start Race");
        raceCars.sort((c1, c2) -> Double.compare(c2.distance, c1.distance));
        StringBuilder result = new StringBuilder("Race Finished! Results:\n");
        for (int i = 0; i < raceCars.size(); i++) {
            result.append(i + 1).append(": ").append(raceCars.get(i).car.name)
                    .append(" (").append(raceCars.get(i).driver.name).append(")\n");
            if (i == 0) {
                team.addBudget(50000); // Winner prize
            } else if (i == 1) {
                team.addBudget(30000); // Second place
            } else if (i == 2) {
                team.addBudget(10000); // Third place
            }
        }
        statusLabel.setText("Race ended! Winner: " + raceCars.get(0).car.name);
        teamManagementPanel.updateTeamInfo();
        JOptionPane.showMessageDialog(this, result.toString(), "Race Results", JOptionPane.INFORMATION_MESSAGE);
    }

    class RaceTrackPanel extends JPanel {
        private int trackWidth = 800;
        private int trackHeight = 600;
        private int trackX = 100;
        private int trackY = 100;
        private int laneHeight = 50;

        public RaceTrackPanel() {
            setPreferredSize(new Dimension(900, 700));
            setBackground(Color.GREEN);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw track (oval shape)
            g2d.setColor(Color.GRAY);
            g2d.fillOval(trackX, trackY, trackWidth, trackHeight);
            g2d.setColor(Color.WHITE);
            g2d.fillOval(trackX + 50, trackY + 50, trackWidth - 100, trackHeight - 100);

            // Draw lanes
            g2d.setColor(Color.BLACK);
            for (int i = 1; i <= 4; i++) {
                g2d.drawOval(trackX + i * 10, trackY + i * 10, trackWidth - i * 20, trackHeight - i * 20);
            }

            // Draw cars
            if (isRaceRunning) {
                for (int i = 0; i < raceCars.size(); i++) {
                    RaceCar raceCar = raceCars.get(i);
                    double progress = raceCar.distance / (double) raceDistance;
                    double angle = 2 * Math.PI * progress;
                    int laneOffset = i * laneHeight;
                    int x = (int) (trackX + trackWidth / 2 + (trackWidth / 2 - laneOffset) * Math.cos(angle));
                    int y = (int) (trackY + trackHeight / 2 + (trackHeight / 2 - laneOffset) * Math.sin(angle));
                    g2d.setColor(raceCar.car.color);
                    g2d.fillRect(x, y, 30, 15);
                    g2d.setColor(Color.BLACK);
                    g2d.drawString(raceCar.car.name, x, y - 5);
                }
            }
        }
    }

    class TeamManagementPanel extends JPanel {
        private JLabel teamInfoLabel;
        private JTextField carNameField;
        private JTextField driverNameField;
        private JTextField driverSkillField;
        private JButton addCarButton;
        private JButton addDriverButton;
        private JButton upgradeCarButton;
        private JComboBox<String> carSelection;

        public TeamManagementPanel() {
            setPreferredSize(new Dimension(300, 700));
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createTitledBorder("Team Management"));

            teamInfoLabel = new JLabel();
            carNameField = new JTextField("Car " + (team.getCars().size() + 1), 15);
            driverNameField = new JTextField("Driver " + (team.getDrivers().size() + 1), 15);
            driverSkillField = new JTextField("50", 5);
            addCarButton = new JButton("Add Car");
            addDriverButton = new JButton("Add Driver");
            upgradeCarButton = new JButton("Upgrade Selected Car");
            carSelection = new JComboBox<>();

            updateTeamInfo();
            updateCarSelection();

            add(new JLabel("Team Budget:"));
            add(teamInfoLabel);
            add(Box.createVerticalStrut(10));
            add(new JLabel("Car Name:"));
            add(carNameField);
            add(new JLabel("Driver Name:"));
            add(driverNameField);
            add(new JLabel("Driver Skill (1-100):"));
            add(driverSkillField);
            add(addCarButton);
            add(addDriverButton);
            add(new JLabel("Select Car to Upgrade:"));
            add(carSelection);
            add(upgradeCarButton);

            addCarButton.addActionListener(e -> addCar());
            addDriverButton.addActionListener(e -> addDriver());
            upgradeCarButton.addActionListener(e -> upgradeCar());
        }

        private void updateTeamInfo() {
            teamInfoLabel.setText("<html>Team: " + team.getName() + "<br>Budget: $" + team.getBudget() +
                    "<br>Cars: " + team.getCars().size() + "<br>Drivers: " + team.getDrivers().size() + "</html>");
        }

        private void updateCarSelection() {
            carSelection.removeAllItems();
            for (Car car : team.getCars()) {
                carSelection.addItem(car.name);
            }
        }

        private void addCar() {
            if (team.getBudget() >= 20000) {
                String carName = carNameField.getText();
                if (!carName.isEmpty()) {
                    team.addCar(new Car(carName, 50, Color.RED));
                    team.deductBudget(20000);
                    updateTeamInfo();
                    updateCarSelection();
                    statusLabel.setText("Car " + carName + " added to the team!");
                    carNameField.setText("Car " + (team.getCars().size() + 1));
                } else {
                    statusLabel.setText("Please enter a valid car name!");
                }
            } else {
                statusLabel.setText("Insufficient budget to add a car!");
            }
        }

        private void addDriver() {
            String driverName = driverNameField.getText();
            try {
                int skill = Integer.parseInt(driverSkillField.getText());
                if (skill >= 1 && skill <= 100 && !driverName.isEmpty()) {
                    if (team.getBudget() >= 10000) {
                        team.addDriver(new Driver(driverName, skill));
                        team.deductBudget(10000);
                        updateTeamInfo();
                        statusLabel.setText("Driver " + driverName + " added to the team!");
                        driverNameField.setText("Driver " + (team.getDrivers().size() + 1));
                        driverSkillField.setText("50");
                    } else {
                        statusLabel.setText("Insufficient budget to hire a driver!");
                    }
                } else {
                    statusLabel.setText("Invalid driver name or skill (1-100)!");
                }
            } catch (NumberFormatException e) {
                statusLabel.setText("Driver skill must be a number!");
            }
        }

        private void upgradeCar() {
            if (carSelection.getSelectedIndex() >= 0) {
                if (team.getBudget() >= 15000) {
                    Car car = team.getCars().get(carSelection.getSelectedIndex());
                    car.upgrade();
                    team.deductBudget(15000);
                    updateTeamInfo();
                    statusLabel.setText("Car " + car.name + " upgraded! New performance: " + car.performance);
                } else {
                    statusLabel.setText("Insufficient budget to upgrade car!");
                }
            } else {
                statusLabel.setText("Select a car to upgrade!");
            }
        }
    }

    class Team {
        private String name;
        private int budget;
        private ArrayList<Car> cars;
        private ArrayList<Driver> drivers;

        public Team(String name, int budget) {
            this.name = name;
            this.budget = budget;
            cars = new ArrayList<>();
            drivers = new ArrayList<>();
        }

        public String getName() {
            return name;
        }

        public int getBudget() {
            return budget;
        }

        public void addBudget(int amount) {
            budget += amount;
        }

        public void deductBudget(int amount) {
            budget -= amount;
        }

        public ArrayList<Car> getCars() {
            return cars;
        }

        public ArrayList<Driver> getDrivers() {
            return drivers;
        }

        public void addCar(Car car) {
            cars.add(car);
        }

        public void addDriver(Driver driver) {
            drivers.add(driver);
        }
    }

    class Car {
        private String name;
        private int performance;
        private Color color;

        public Car(String name, int performance, Color color) {
            this.name = name;
            this.performance = performance;
            this.color = color;
        }

        public void upgrade() {
            performance = Math.min(100, performance + 10);
        }
    }

    class Driver {
        private String name;
        private int skill;

        public Driver(String name, int skill) {
            this.name = name;
            this.skill = Math.max(1, Math.min(100, skill));
        }
    }

    class RaceCar {
        private Car car;
        private Driver driver;
        private double distance;
        private Random random;

        public RaceCar(Car car, Driver driver) {
            this.car = car;
            this.driver = driver;
            this.distance = 0;
            this.random = new Random();
        }

        public void updatePosition() {
            double speed = (car.performance + driver.skill) / 20.0;
            speed += random.nextDouble() * 2 - 1; // Random variation
            distance += speed;
            distance = Math.min(distance, raceDistance);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(CarRacingTeamManager::new);
    }
}