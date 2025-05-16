import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class VirtualSportsClubManagement extends JFrame {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;

    private Club club;
    private JPanel mainPanel;
    private CardLayout cardLayout;
    private JTextArea outputArea;
    private JLabel animationLabel;
    private ImageIcon[] trainingAnimationFrames;
    private ImageIcon[] matchAnimationFrames;
    private int currentFrame = 0;
    private Timer animationTimer;

    public VirtualSportsClubManagement() {
        super("Virtual Sports Club Management");
        setSize(WIDTH, HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        initializeAnimations();
        club = new Club("Champion FC");
        initializeUI();

        // Add some sample players
        club.addPlayer(new Player("John Doe", 22, "Forward", 78, 150000));
        club.addPlayer(new Player("Mike Smith", 25, "Midfielder", 82, 200000));
        club.addPlayer(new Player("David Johnson", 29, "Defender", 75, 120000));
        club.addPlayer(new Player("Sarah Williams", 21, "Forward", 80, 180000));
        club.addPlayer(new Player("Emma Brown", 23, "Goalkeeper", 85, 250000));
    }

    private void initializeAnimations() {
        // In a real application, you would load actual animation frames
        // Here we'll create placeholder animations
        trainingAnimationFrames = new ImageIcon[10];
        matchAnimationFrames = new ImageIcon[15];

        for (int i = 0; i < trainingAnimationFrames.length; i++) {
            trainingAnimationFrames[i] = createPlaceholderIcon(Color.BLUE, "Training " + (i+1));
        }

        for (int i = 0; i < matchAnimationFrames.length; i++) {
            matchAnimationFrames[i] = createPlaceholderIcon(Color.GREEN, "Match " + (i+1));
        }
    }

    private ImageIcon createPlaceholderIcon(Color color, String text) {
        BufferedImage image = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, 200, 200);
        g.setColor(Color.WHITE);
        g.setFont(new Font("Arial", Font.BOLD, 16));
        g.drawString(text, 20, 100);
        g.dispose();
        return new ImageIcon(image);
    }

    private void initializeUI() {
        cardLayout = new CardLayout();
        mainPanel = new JPanel(cardLayout);

        // Main menu panel
        JPanel menuPanel = new JPanel(new GridLayout(5, 1, 10, 10));
        JButton viewPlayersBtn = new JButton("View Players");
        JButton trainPlayersBtn = new JButton("Train Players");
        JButton playMatchBtn = new JButton("Play Match");
        JButton manageClubBtn = new JButton("Manage Club");
        JButton exitBtn = new JButton("Exit");

        viewPlayersBtn.addActionListener(e -> showPlayers());
        trainPlayersBtn.addActionListener(e -> startTrainingAnimation());
        playMatchBtn.addActionListener(e -> startMatchAnimation());
        manageClubBtn.addActionListener(e -> showClubInfo());
        exitBtn.addActionListener(e -> System.exit(0));

        menuPanel.add(viewPlayersBtn);
        menuPanel.add(trainPlayersBtn);
        menuPanel.add(playMatchBtn);
        menuPanel.add(manageClubBtn);
        menuPanel.add(exitBtn);

        // Players panel
        JPanel playersPanel = new JPanel(new BorderLayout());
        outputArea = new JTextArea();
        outputArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(outputArea);
        JButton backBtn = new JButton("Back to Menu");
        backBtn.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        playersPanel.add(scrollPane, BorderLayout.CENTER);
        playersPanel.add(backBtn, BorderLayout.SOUTH);

        // Animation panel
        JPanel animationPanel = new JPanel(new BorderLayout());
        animationLabel = new JLabel();
        animationLabel.setHorizontalAlignment(JLabel.CENTER);
        JButton stopAnimationBtn = new JButton("Stop Animation");
        stopAnimationBtn.addActionListener(e -> stopAnimation());

        animationPanel.add(animationLabel, BorderLayout.CENTER);
        animationPanel.add(stopAnimationBtn, BorderLayout.SOUTH);

        // Club info panel
        JPanel clubPanel = new JPanel(new BorderLayout());
        JTextArea clubInfoArea = new JTextArea();
        clubInfoArea.setEditable(false);
        JScrollPane clubScrollPane = new JScrollPane(clubInfoArea);
        JButton backClubBtn = new JButton("Back to Menu");
        backClubBtn.addActionListener(e -> cardLayout.show(mainPanel, "menu"));

        JPanel clubActionsPanel = new JPanel(new GridLayout(1, 3));
        JButton hirePlayerBtn = new JButton("Hire Random Player");
        JButton firePlayerBtn = new JButton("Release Player");
        JButton upgradeFacilitiesBtn = new JButton("Upgrade Facilities");

        hirePlayerBtn.addActionListener(e -> {
            club.hireRandomPlayer();
            updateClubInfo(clubInfoArea);
        });

        firePlayerBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(this, "Enter player name to release:");
            if (name != null && !name.trim().isEmpty()) {
                club.releasePlayer(name);
                updateClubInfo(clubInfoArea);
            }
        });

        upgradeFacilitiesBtn.addActionListener(e -> {
            club.upgradeFacilities();
            updateClubInfo(clubInfoArea);
        });

        clubActionsPanel.add(hirePlayerBtn);
        clubActionsPanel.add(firePlayerBtn);
        clubActionsPanel.add(upgradeFacilitiesBtn);

        clubPanel.add(clubScrollPane, BorderLayout.CENTER);
        clubPanel.add(clubActionsPanel, BorderLayout.NORTH);
        clubPanel.add(backClubBtn, BorderLayout.SOUTH);

        // Add panels to main panel
        mainPanel.add(menuPanel, "menu");
        mainPanel.add(playersPanel, "players");
        mainPanel.add(animationPanel, "animation");
        mainPanel.add(clubPanel, "club");

        add(mainPanel);
        updateClubInfo(clubInfoArea);
    }

    private void showPlayers() {
        outputArea.setText("");
        outputArea.append("===== PLAYER ROSTER =====\n\n");
        for (Player player : club.getPlayers()) {
            outputArea.append(player.toString() + "\n\n");
        }
        outputArea.append("Total players: " + club.getPlayers().size() + "\n");
        outputArea.append("Total salary: $" + club.getTotalSalary() + " per week\n");
        cardLayout.show(mainPanel, "players");
    }

    private void startTrainingAnimation() {
        currentFrame = 0;
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentFrame < trainingAnimationFrames.length) {
                    animationLabel.setIcon(trainingAnimationFrames[currentFrame]);
                    currentFrame++;
                } else {
                    stopAnimation();
                    club.trainPlayers();
                    JOptionPane.showMessageDialog(VirtualSportsClubManagement.this,
                            "Training completed! Player skills improved!");
                }
            }
        }, 0, 300);
        cardLayout.show(mainPanel, "animation");
    }

    private void startMatchAnimation() {
        currentFrame = 0;
        animationTimer = new Timer();
        animationTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (currentFrame < matchAnimationFrames.length) {
                    animationLabel.setIcon(matchAnimationFrames[currentFrame]);
                    currentFrame++;
                } else {
                    stopAnimation();
                    String result = club.playMatch();
                    JOptionPane.showMessageDialog(VirtualSportsClubManagement.this,
                            "Match finished!\n" + result);
                }
            }
        }, 0, 200);
        cardLayout.show(mainPanel, "animation");
    }

    private void stopAnimation() {
        if (animationTimer != null) {
            animationTimer.cancel();
            animationTimer = null;
        }
    }

    private void showClubInfo() {
        cardLayout.show(mainPanel, "club");
    }

    private void updateClubInfo(JTextArea area) {
        area.setText(club.getClubInfo());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            VirtualSportsClubManagement game = new VirtualSportsClubManagement();
            game.setVisible(true);
        });
    }
}

class Club {
    private String name;
    private List<Player> players;
    private int facilitiesLevel;
    private int budget;
    private int wins;
    private int losses;
    private int draws;

    public Club(String name) {
        this.name = name;
        this.players = new ArrayList<>();
        this.facilitiesLevel = 1;
        this.budget = 5000000; // $5 million starting budget
        this.wins = 0;
        this.losses = 0;
        this.draws = 0;
    }

    public void addPlayer(Player player) {
        players.add(player);
    }

    public void trainPlayers() {
        Random rand = new Random();
        for (Player player : players) {
            // Better facilities give better training results
            int improvement = rand.nextInt(3) + facilitiesLevel;
            player.improveSkill(improvement);

            // Younger players improve more
            if (player.getAge() < 25) {
                player.improveSkill(1);
            }
        }
    }

    public String playMatch() {
        Random rand = new Random();
        int teamSkill = getTeamSkill();
        int opponentSkill = rand.nextInt(30) + 70; // Opponent between 70-100

        // Facilities give a small bonus
        teamSkill += facilitiesLevel * 2;

        // Random factor
        teamSkill += rand.nextInt(20) - 10;

        if (teamSkill > opponentSkill + 10) {
            wins++;
            budget += 500000; // $500k prize money for win
            return "Your team " + name + " won the match!\n" +
                    "Your skill: " + teamSkill + " vs Opponent: " + opponentSkill;
        } else if (teamSkill < opponentSkill - 10) {
            losses++;
            budget += 100000; // $100k for loss
            return "Your team " + name + " lost the match.\n" +
                    "Your skill: " + teamSkill + " vs Opponent: " + opponentSkill;
        } else {
            draws++;
            budget += 250000; // $250k for draw
            return "The match ended in a draw.\n" +
                    "Your skill: " + teamSkill + " vs Opponent: " + opponentSkill;
        }
    }

    public void hireRandomPlayer() {
        if (budget < 1000000) {
            throw new IllegalStateException("Not enough budget to hire new players");
        }

        Random rand = new Random();
        String[] names = {"James", "Robert", "Michael", "William", "David",
                "Richard", "Joseph", "Thomas", "Charles", "Christopher",
                "Jessica", "Jennifer", "Amanda", "Lisa", "Sarah",
                "Nicole", "Emily", "Elizabeth", "Megan", "Lauren"};
        String[] positions = {"Goalkeeper", "Defender", "Midfielder", "Forward"};

        String name = names[rand.nextInt(names.length)] + " " +
                names[rand.nextInt(names.length)] + "son";
        int age = rand.nextInt(10) + 18; // 18-27
        String position = positions[rand.nextInt(positions.length)];
        int skill = rand.nextInt(20) + 65; // 65-84
        int salary = (skill * 1000) + rand.nextInt(50000);

        Player player = new Player(name, age, position, skill, salary);
        players.add(player);
        budget -= salary * 10; // Pay 10 weeks salary as signing bonus
    }

    public void releasePlayer(String playerName) {
        Player toRemove = null;
        for (Player player : players) {
            if (player.getName().equalsIgnoreCase(playerName)) {
                toRemove = player;
                break;
            }
        }

        if (toRemove != null) {
            players.remove(toRemove);
            // Pay 4 weeks salary as severance
            budget -= toRemove.getWeeklySalary() * 4;
        } else {
            throw new IllegalArgumentException("Player not found: " + playerName);
        }
    }

    public void upgradeFacilities() {
        int cost = facilitiesLevel * 1000000;
        if (budget >= cost) {
            budget -= cost;
            facilitiesLevel++;
        } else {
            throw new IllegalStateException("Not enough budget to upgrade facilities");
        }
    }

    private int getTeamSkill() {
        if (players.isEmpty()) return 0;

        int total = 0;
        for (Player player : players) {
            total += player.getSkill();
        }
        return total / players.size();
    }

    public int getTotalSalary() {
        int total = 0;
        for (Player player : players) {
            total += player.getWeeklySalary();
        }
        return total;
    }

    public String getClubInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("===== CLUB INFORMATION =====\n\n");
        sb.append("Club Name: ").append(name).append("\n");
        sb.append("Facilities Level: ").append(facilitiesLevel).append("\n");
        sb.append("Budget: $").append(budget).append("\n");
        sb.append("Record: ").append(wins).append("W-").append(losses)
                .append("L-").append(draws).append("D\n\n");
        sb.append("Number of Players: ").append(players.size()).append("\n");
        sb.append("Total Weekly Salary: $").append(getTotalSalary()).append("\n\n");
        sb.append("Top 5 Players:\n");

        if (!players.isEmpty()) {
            List<Player> sortedPlayers = new ArrayList<>(players);
            sortedPlayers.sort((p1, p2) -> p2.getSkill() - p1.getSkill());

            int count = Math.min(5, sortedPlayers.size());
            for (int i = 0; i < count; i++) {
                sb.append(i+1).append(". ").append(sortedPlayers.get(i).getName())
                        .append(" (").append(sortedPlayers.get(i).getPosition())
                        .append(") - Skill: ").append(sortedPlayers.get(i).getSkill())
                        .append("\n");
            }
        }

        return sb.toString();
    }

    // Getters
    public List<Player> getPlayers() { return players; }
    public String getName() { return name; }
    public int getBudget() { return budget; }
}

class Player {
    private String name;
    private int age;
    private String position;
    private int skill; // 0-100 scale
    private int weeklySalary;

    public Player(String name, int age, String position, int skill, int weeklySalary) {
        this.name = name;
        this.age = age;
        this.position = position;
        this.skill = Math.min(100, Math.max(0, skill)); // Ensure skill is 0-100
        this.weeklySalary = weeklySalary;
    }

    public void improveSkill(int amount) {
        skill = Math.min(100, skill + amount);
    }

    @Override
    public String toString() {
        return "Name: " + name + "\n" +
                "Age: " + age + "\n" +
                "Position: " + position + "\n" +
                "Skill: " + skill + "/100\n" +
                "Weekly Salary: $" + weeklySalary;
    }

    // Getters
    public String getName() { return name; }
    public int getAge() { return age; }
    public String getPosition() { return position; }
    public int getSkill() { return skill; }
    public int getWeeklySalary() { return weeklySalary; }
}