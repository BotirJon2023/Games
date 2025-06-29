export interface Position {
  x: number;
  y: number;
}

export interface Velocity {
  x: number;
  y: number;
}

export interface Skater {
  id: string;
  name: string;
  position: Position;
  velocity: Velocity;
  speed: number;
  maxSpeed: number;
  acceleration: number;
  stamina: number;
  maxStamina: number;
  color: string;
  angle: number;
  lap: number;
  lapTime: number;
  bestLapTime: number;
  totalTime: number;
  isPlayer: boolean;
  aiDifficulty: number;
  powerUpActive: boolean;
  powerUpType: string | null;
  powerUpDuration: number;
}

export interface Track {
  centerX: number;
  centerY: number;
  innerRadius: number;
  outerRadius: number;
  width: number;
}

export interface PowerUp {
  id: string;
  type: 'speed' | 'stamina' | 'slip';
  position: Position;
  active: boolean;
  duration: number;
}

export interface GameState {
  isRunning: boolean;
  isPaused: boolean;
  gameTime: number;
  maxLaps: number;
  skaters: Skater[];
  powerUps: PowerUp[];
  winner: Skater | null;
}

export interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
}