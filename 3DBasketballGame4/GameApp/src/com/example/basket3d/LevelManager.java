package com.example.basket3d;

public class LevelManager {

    private final float reactionTime;
    private final float shootBias;
    private final float aimJitter;

    public LevelManager(GameApp.Difficulty diff) {
        switch (diff) {
            case EASY:
                reactionTime = 0.8f; shootBias = 0.35f; aimJitter = 0.5f;  break;
            case MEDIUM:
                reactionTime = 0.5f; shootBias = 0.5f;  aimJitter = 0.25f; break;
            default:
                reactionTime = 0.25f; shootBias = 0.7f; aimJitter = 0.1f;  break;
        }
    }

    public float getReactionTime() { return reactionTime; }
    public float getShootBias()    { return shootBias; }
    public float getAimJitter()    { return aimJitter; }
}
