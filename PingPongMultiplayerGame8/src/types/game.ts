export interface Position {
  x: number;
  y: number;
}

export interface Velocity {
  x: number;
  y: number;
}

export interface GameStats {
  player1Score: number;
  player2Score: number;
  ballSpeed: number;
  rallies: number;
}

export type GameState = 'menu' | 'playing' | 'paused' | 'gameOver';

export interface Paddle {
  x: number;
  y: number;
  width: number;
  height: number;
  velocity: number;
}

export interface Ball {
  position: Position;
  velocity: Velocity;
  size: number;
}

export interface GameConfig {
  gameWidth: number;
  gameHeight: number;
  paddleSpeed: number;
  ballInitialSpeed: number;
  winningScore: number;
  speedIncrement: number;
}