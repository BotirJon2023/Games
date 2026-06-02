# Football Management Simulation — Java/JavaFX

A football (soccer) simulation game for **2 players** or **vs Computer**, with animated pitch, players, ball physics, live scoreboard, and match commentary.

---

## Requirements

- **Java 17+** with **JavaFX 17+** installed
- Or use a JDK bundle that includes JavaFX (e.g. [Liberica JDK Full](https://bell-sw.com/pages/downloads/) or [Zulu FX](https://www.azul.com/downloads/))

---

## How to Compile & Run

### Option A — Liberica / Zulu (JavaFX bundled)

```bash
cd FootballSimulation/src
javac FootballSimulation.java
java FootballSimulation
```

### Option B — OpenJDK + separate JavaFX SDK

1. Download [JavaFX SDK](https://gluonhq.com/products/javafx/) and unzip it.
2. Set the path:

```bash
export FX=/path/to/javafx-sdk/lib

cd FootballSimulation/src
javac --module-path $FX --add-modules javafx.controls,javafx.graphics FootballSimulation.java
java  --module-path $FX --add-modules javafx.controls,javafx.graphics FootballSimulation
```

---

## Controls

| Action          | Team A (Red)     | Team B (Blue)    |
|-----------------|------------------|------------------|
| Move            | `W A S D`        | `↑ ↓ ← →`       |
| Shoot           | `E` or `Space`   | `/`              |

> In **vs Computer** mode, Team B is controlled by the AI.

---

## Features

- **Main Menu** with animated mini-pitch and mode selection
- **Team Select** — 5 teams per side, each with a unique color
- **Animated Pitch** — grass stripes, penalty boxes, centre circle, corner arcs
- **11 Players per team** — goalkeeper, 4 defenders, 3 midfielders, 3 forwards
- **Ball physics** — friction, wall bouncing, player collision deflection, speed glow
- **Rotating ball** with pentagon pattern
- **AI players** — defenders track the ball, midfielders press, GK follows the Y axis
- **Computer AI** — attackers chase and shoot autonomously
- **Goal detection** with celebration overlay, score flash animation, and confetti
- **Live match commentary** cycling every 4 seconds
- **Match clock** — 90 minutes (each real second = 0.5 match minutes)
- **Pause / Resume** support
- **Half-time** and **Full-time** announcements
- **Result screen** with confetti animation and trophy
