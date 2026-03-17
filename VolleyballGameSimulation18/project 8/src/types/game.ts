export interface Vector2D {
  x: number;
  y: number;
}

export interface Player {
  position: Vector2D;
  velocity: Vector2D;
  width: number;
  height: number;
  color: string;
  score: number;
}

export interface Ball {
  position: Vector2D;
  velocity: Vector2D;
  radius: number;
  isServing: boolean;
}

export interface GameState {
  player1: Player;
  player2: Player;
  ball: Ball;
  isPlaying: boolean;
  isPaused: boolean;
  gameMode: 'pvp' | 'pvc';
  servingPlayer: 1 | 2;
  maxScore: number;
}

export type GameMode = 'menu' | 'playing' | 'gameover';
