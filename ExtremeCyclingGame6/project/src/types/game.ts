export type GameState = 'menu' | 'playing' | 'gameOver';

export interface Position {
  x: number;
  y: number;
}

export interface Cyclist {
  position: Position;
  velocity: Position;
  isJumping: boolean;
  isGrounded: boolean;
  rotation: number;
  health: number;
  invulnerable: boolean;
}

export interface Obstacle {
  id: string;
  position: Position;
  width: number;
  height: number;
  type: 'rock' | 'tree' | 'ramp' | 'pit' | 'spike';
  speed: number;
}

export interface PowerUp {
  id: string;
  position: Position;
  type: 'speed' | 'shield' | 'health' | 'jump';
  collected: boolean;
}

export interface Particle {
  id: string;
  position: Position;
  velocity: Position;
  life: number;
  maxLife: number;
  color: string;
  size: number;
}

export interface GameConfig {
  gravity: number;
  jumpPower: number;
  baseSpeed: number;
  maxSpeed: number;
  acceleration: number;
  friction: number;
  groundLevel: number;
}

export interface GameStats {
  score: number;
  distance: number;
  timeElapsed: number;
  level: number;
  combo: number;
}

export interface ActivePowerUp {
  type: string;
  timeRemaining: number;
  maxTime: number;
}