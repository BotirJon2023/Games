export interface Vector2 {
  x: number;
  y: number;
}

export interface Paddle {
  x: number;
  y: number;
  width: number;
  height: number;
  velocity: number;
  speed: number;
}

export interface Ball {
  x: number;
  y: number;
  radius: number;
  velocityX: number;
  velocityY: number;
  speed: number;
}

export interface Particle {
  x: number;
  y: number;
  velocityX: number;
  velocityY: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  type: 'spark' | 'trail' | 'impact' | 'score';
}

export interface GameScore {
  player: number;
  computer: number;
  rallies: number;
}

export type GameState = 'menu' | 'playing' | 'paused' | 'gameOver' | 'difficulty';
export type Difficulty = 'easy' | 'medium' | 'hard';

export interface GameData {
  state: GameState;
  difficulty: Difficulty;
  score: GameScore;
  playerPaddle: Paddle;
  computerPaddle: Paddle;
  ball: Ball;
  particles: Particle[];
  gameTime: number;
  isPaused: boolean;
}
