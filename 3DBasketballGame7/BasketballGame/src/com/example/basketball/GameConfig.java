package com.example.basketball;

public class GameConfig {
    public enum Mode { PVE, PVP }
    public Mode mode = Mode.PVE;
    public int levelIndex = 0;
    public int matchLengthSeconds = 180;
}
