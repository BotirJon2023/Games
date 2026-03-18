export interface Vector2D {
  x: number;
  y: number;
}

export interface Player {
  position: Vector2D;
  velocity: Vector2D;
  width: number;
  height: number;
  score: number;
  isJumping: boolean;
  direction: 'left' | 'right' | 'idle';
}

export interface Ball {
  position: Vector2D;
  velocity: Vector2D;
  radius: number;
  spinning: number;
}

export type GameMode = 'menu' | 'playing' | 'paused' | 'gameOver';
export type PlayerMode = '1player' | '2player';
export type Difficulty = 'easy' | 'medium' | 'hard' | 'expert';

export interface GameState {
  mode: GameMode;
  playerMode: PlayerMode;
  difficulty: Difficulty;
  player1: Player;
  player2: Player;
  ball: Ball;
  serving: 1 | 2;
  maxScore: number;
}
