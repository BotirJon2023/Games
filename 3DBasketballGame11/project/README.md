# 3D Basketball Championship

A beautiful 3D basketball game with realistic physics, built with Three.js and Cannon.js.

## Features

- **Stunning 3D Graphics**: Realistic basketball court, hoop, and ball with dynamic lighting and shadows
- **Realistic Physics**: Ball trajectory simulation using Cannon.js physics engine
- **Multiple Game Modes**:
  - 2 Player (local multiplayer)
  - VS Computer (with adjustable AI difficulty)
- **10 Competition Levels**: From Rookie Court to Ultimate Champion
- **Progressive Difficulty**: Increasing challenge with wind effects, distance, and time limits
- **Global Leaderboard**: Compete for the top scores stored in Supabase

## Controls

### Player 1
- **A/D**: Adjust shot angle
- **W/S**: Adjust power
- **SPACE**: Shoot

### Player 2 (2-Player Mode)
- **Arrow Left/Right**: Adjust shot angle
- **Arrow Up/Down**: Adjust power
- **ENTER**: Shoot

### General
- **ESC**: Pause game

## Levels

1. Rookie Court - Beginner friendly
2. Street Ball - Light wind effects
3. Campus Arena - Increased challenge
4. City League - Stronger wind
5. State Finals - Professional difficulty
6. National Cup - National competition level
7. World Tournament - International challenge
8. Olympic Stage - Olympic-level difficulty
9. Legend Arena - Near impossible
10. Ultimate Champion - For true masters

## Technologies

- **Three.js** - 3D graphics rendering
- **Cannon.js** - Physics simulation
- **Vite** - Build tool and dev server
- **Supabase** - Database and leaderboard

## Running the Game

```bash
npm install
npm run dev
```

## Building for Production

```bash
npm run build
```

The built files will be in the `dist` folder, ready for deployment.
