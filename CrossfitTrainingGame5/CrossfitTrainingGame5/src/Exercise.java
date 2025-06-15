import java.util.Objects;

public class Exercise {
    private String name;
    private int repetitions;
    private String description;
    // Add more properties like:
    // private int durationSeconds;
    // private String requiredInputSequence; // e.g., "Press S, then J, then K"
    // private String animationKey; // Key to trigger specific animation for this exercise

    private int currentProgress; // How many reps done, or how much time passed for current exercise
    private boolean completed; // Whether this exercise is done

    public Exercise(String name, int repetitions, String description, int initialProgress, int initialDuration) {
        this.name = name;
        this.repetitions = repetitions;
        this.description = description;
        this.currentProgress = initialProgress; // Not used in this simple demo, but for tracking
        this.completed = false;
    }

    // --- Methods for game logic ---
    public void performRep() {
        if (!completed) {
            currentProgress++;
            if (currentProgress >= repetitions) {
                completed = true;
                System.out.println("Exercise '" + name + "' completed!");
            }
        }
    }

    // --- Getters ---
    public String getName() { return name; }
    public int getRepetitions() { return repetitions; }
    public String getDescription() { return description; }
    public int getCurrentProgress() { return currentProgress; }
    public boolean isCompleted() { return completed; }

    // --- Setters (if needed, e.g., for resetting) ---
    public void setCompleted(boolean completed) { this.completed = completed; }
    public void resetProgress() { this.currentProgress = 0; this.completed = false; }

    @Override
    public String toString() {
        return "Exercise{" +
                "name='" + name + '\'' +
                ", repetitions=" + repetitions +
                ", description='" + description + '\'' +
                ", currentProgress=" + currentProgress +
                ", completed=" + completed +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Exercise exercise = (Exercise) o;
        return repetitions == exercise.repetitions &&
                currentProgress == exercise.currentProgress &&
                completed == exercise.completed &&
                Objects.equals(name, exercise.name) &&
                Objects.equals(description, exercise.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, repetitions, description, currentProgress, completed);
    }
}