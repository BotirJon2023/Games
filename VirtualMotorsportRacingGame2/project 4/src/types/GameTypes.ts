export type GameState = 'MENU' | 'RACING' | 'PAUSED' | 'FINISHED';

export interface Vector2 {
  x: number;
  y: number;
}

export interface Car {
  x: number;
  y: number;
  vx: number;
  vy: number;
  angle: number;
  speed: number;
  maxSpeed: number;
  width: number;
  height: number;
  color: string;
  lapCount: number;
}

export interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  lifetime: number;
  maxLifetime: number;
  color: string;
  size: number;
}

export interface PowerUp {
  x: number;
  y: number;
  collected: boolean;
  size: number;
  angle: number;
}

export interface RaceStats {
  position: number;
  lap: number;
  speed: number;
  totalLaps: number;
}
