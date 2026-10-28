export type GameMode = 'vs-player' | 'vs-computer';

export type Screen = 'menu' | 'game' | 'results';

export type ControlScheme = 'arrows' | 'wasd';

export interface Vec2 {
  x: number;
  y: number;
}

export interface TrackPoint {
  pos: Vec2;
  /** Cumulative arc length from the start of the track. */
  arcLength: number;
  /** Tangent direction (unit vector) at this point. */
  tangent: Vec2;
  /** Normal direction (unit vector, pointing outward) at this point. */
  normal: Vec2;
}

export interface BoostPad {
  pos: Vec2;
  angle: number;
  width: number;
  length: number;
  active: boolean;
  cooldownTimer: number;
}

export interface Obstacle {
  pos: Vec2;
  radius: number;
  rotation: number;
  rotationSpeed: number;
  hitTimer: number;
}

export interface RaceState {
  laps: number;
  totalLaps: number;
  nextCheckpoint: number;
  finished: boolean;
  finishTime: number;
  position: number;
}

export interface InputState {
  accelerate: boolean;
  brake: boolean;
  left: boolean;
  right: boolean;
  boost: boolean;
}

export interface GameResult {
  winnerName: string;
  winnerColor: string;
  finishTime: number;
  mode: GameMode;
  p1Laps: number;
  p2Laps: number;
}
