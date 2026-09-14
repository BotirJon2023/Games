# Extreme Mountain Bike Racing Game

A dependency-free Java2D racing game with a procedural mountain trail, bike physics,
computer opponent, local two-player mode, camera movement, dust particles, a finish
line, and a polished HUD.

## Run

Requires Java 11 or newer:

```bash
javac ExtremeMountainBikeRacingGame.java
java ExtremeMountainBikeRacingGame
```

## Controls

- **Menu:** `1` for solo vs CPU, `2` for local two-player
- **Player 1:** `W` pedal, `A/D` lean, `S` brake
- **Player 2:** arrow keys or `I/J/K/L`
- **Race:** `R` restart, `Esc` return to menu

The game is intentionally a single source file so it is easy to copy, compile, and
extend. The visual layers and physics are all generated at runtime; no art assets or
third-party libraries are required.