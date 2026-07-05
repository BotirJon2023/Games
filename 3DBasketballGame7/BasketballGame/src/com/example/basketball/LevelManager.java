package com.example.basketball;

import java.util.ArrayList;
import java.util.List;

public class LevelManager {

    public static class Level {
        public final String name;
        public final float aiShotAccuracy;
        public final float aiAggression;
        public final float aiReaction;
        public final float aimAssist;
        public final float staminaDrain;

        public Level(String name, float aiShotAccuracy, float aiAggression, float aiReaction, float aimAssist, float staminaDrain) {
            this.name = name;
            this.aiShotAccuracy = aiShotAccuracy;
            this.aiAggression = aiAggression;
            this.aiReaction = aiReaction;
            this.aimAssist = aimAssist;
            this.staminaDrain = staminaDrain;
        }
    }

    private final List<Level> levels = new ArrayList<>();

    public LevelManager() {
        levels.add(new Level("Rookie",   0.35f, 0.3f,  0.35f, 0.35f, 0.7f));
        levels.add(new Level("Semi-Pro", 0.5f,  0.45f, 0.5f,  0.25f, 0.9f));
        levels.add(new Level("Pro",      0.65f, 0.65f, 0.65f, 0.18f, 1.1f));
        levels.add(new Level("All-Star", 0.78f, 0.8f,  0.8f,  0.12f, 1.2f));
        levels.add(new Level("Legend",   0.9f,  0.95f, 0.95f, 0.08f, 1.35f));
    }

    public Level get(int idx) {
        return levels.get(Math.max(0, Math.min(idx, levels.size() - 1)));
    }

    public int size() { return levels.size(); }

    public List<Level> all() { return levels; }
}
