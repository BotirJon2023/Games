import java.util.*;

public class BowlingLeagueSimulator1 {

    public static void main(String[] args) throws InterruptedException {
        Game game = new Game();
        game.start();
    }
}

// Player class
class Player {
    private String name;
    private List<Frame> frames = new ArrayList<>();
    private int totalScore;

    public Player(String name) {
        this.name = name;
        for (int i = 0; i < 10; i++) {
            frames.add(new Frame(i == 9));
        }
    }

    public String getName() {
        return name;
    }

    public List<Frame> getFrames() {
        return frames;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public void calculateScore() {
        totalScore = 0;
        for (int i = 0; i < frames.size(); i++) {
            Frame frame = frames.get(i);
            totalScore += frame.getFrameScore(frames, i);
        }
    }
}

// Frame class
class Frame {
    private Integer firstRoll = null;
    private Integer secondRoll = null;
    private Integer thirdRoll = null;
    private boolean isLastFrame;

    public Frame(boolean isLastFrame) {
        this.isLastFrame = isLastFrame;
    }

    public void roll(Random rand) {
        firstRoll = rand.nextInt(11);
        if (firstRoll == 10 && !isLastFrame) {
            secondRoll = 0;
            return;
        }

        int pinsLeft = 10 - firstRoll;
        secondRoll = rand.nextInt(pinsLeft + 1);

        if (isLastFrame && (firstRoll == 10 || firstRoll + secondRoll == 10)) {
            int thirdPinsLeft = firstRoll == 10 ? 10 : 10 - secondRoll;
            thirdRoll = rand.nextInt(thirdPinsLeft + 1);
        }
    }

    public Integer getFirstRoll() {
        return firstRoll;
    }

    public Integer getSecondRoll() {
        return secondRoll;
    }

    public Integer getThirdRoll() {
        return thirdRoll;
    }

    public boolean isStrike() {
        return firstRoll != null && firstRoll == 10;
    }

    public boolean isSpare() {
        return firstRoll != null && secondRoll != null &&
                firstRoll + secondRoll == 10 && !isStrike();
    }

    public int getFrameScore(List<Frame> frames, int index) {
        if (firstRoll == null) return 0;

        if (isStrike()) {
            int bonus = getStrikeBonus(frames, index);
            return 10 + bonus;
        } else if (isSpare()) {
            int bonus = getSpareBonus(frames, index);
            return 10 + bonus;
        } else {
            return firstRoll + (secondRoll == null ? 0 : secondRoll) +
                    (thirdRoll == null ? 0 : thirdRoll);
        }
    }

    private int getStrikeBonus(List<Frame> frames, int index) {
        int bonus = 0;
        if (index + 1 < frames.size()) {
            Frame next = frames.get(index + 1);
            bonus += next.firstRoll != null ? next.firstRoll : 0;
            if (next.isStrike() && index + 2 < frames.size()) {
                bonus += frames.get(index + 2).firstRoll != null ? frames.get(index + 2).firstRoll : 0;
            } else {
                bonus += next.secondRoll != null ? next.secondRoll : 0;
            }
        }
        return bonus;
    }

    private int getSpareBonus(List<Frame> frames, int index) {
        if (index + 1 < frames.size()) {
            Frame next = frames.get(index + 1);
            return next.firstRoll != null ? next.firstRoll : 0;
        }
        return 0;
    }
}

// Game class
class Game {
    private List<Player> players = new ArrayList<>();
    private Random rand = new Random();

    public Game() {
        players.add(new Player("Alice"));
        players.add(new Player("Bob"));
        players.add(new Player("Charlie"));
        players.add(new Player("Diana"));
    }

    public void start() throws InterruptedException {
        for (int frame = 0; frame < 10; frame++) {
            System.out.println("=== Frame " + (frame + 1) + " ===");
            for (Player player : players) {
                Frame f = player.getFrames().get(frame);
                animateRoll(player.getName(), frame + 1);
                f.roll(rand);
                printFrameResults(player, f, frame + 1);
                player.calculateScore();
                Thread.sleep(500);
            }
            printScoreBoard();
            Thread.sleep(1000);
        }
        declareWinner();
    }

    private void animateRoll(String player, int frame) throws InterruptedException {
        System.out.print(player + " is rolling");
        for (int i = 0; i < 5; i++) {
            System.out.print(".");
            Thread.sleep(200);
        }
        System.out.println();
    }

    private void printFrameResults(Player player, Frame f, int frameNumber) {
        System.out.printf("%s (Frame %d): ", player.getName(), frameNumber);
        System.out.print("Roll 1: " + f.getFirstRoll() + " ");
        System.out.print("Roll 2: " + f.getSecondRoll());
        if (f.getThirdRoll() != null) {
            System.out.print(" Roll 3: " + f.getThirdRoll());
        }
        System.out.println();
    }

    private void printScoreBoard() {
        System.out.println("\n=== Scoreboard ===");
        for (Player p : players) {
            System.out.printf("%-10s: %3d |", p.getName(), p.getTotalScore());
            for (Frame f : p.getFrames()) {
                System.out.print(" " + formatFrame(f) + " ");
            }
            System.out.println();
        }
        System.out.println("==================\n");
    }

    private String formatFrame(Frame f) {
        if (f.getFirstRoll() == null) return "__";
        if (f.isStrike()) return "X ";
        if (f.isSpare()) return f.getFirstRoll() + "/";
        return f.getFirstRoll() + "," + (f.getSecondRoll() != null ? f.getSecondRoll() : "_");
    }

    private void declareWinner() {
        Player winner = players.stream().max(Comparator.comparing(Player::getTotalScore)).get();
        System.out.println("🏆 Winner: " + winner.getName() + " with " + winner.getTotalScore() + " points!");
    }
}
