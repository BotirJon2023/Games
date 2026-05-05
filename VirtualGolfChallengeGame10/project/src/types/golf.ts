export interface Player {
  id: string;
  name: string;
  color: string;
  scores: number[];
  totalScore: number;
  isComputer: boolean;
}

export interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  isMoving: boolean;
  trail: { x: number; y: number }[];
}

export interface Hole {
  x: number;
  y: number;
  radius: number;
  par: number;
}

export interface Obstacle {
  x: number;
  y: number;
  width: number;
  height: number;
  type: 'bunker' | 'water' | 'tree' | 'rock';
}

export interface Course {
  id: number;
  name: string;
  par: number;
  tee: { x: number; y: number };
  hole: Hole;
  obstacles: Obstacle[];
  wind: { speed: number; direction: number };
}

export interface GameState {
  players: Player[];
  currentPlayerIndex: number;
  currentHole: number;
  courses: Course[];
  balls: Map<string, Ball>;
  gamePhase: 'aiming' | 'swinging' | 'ball-moving' | 'hole-complete' | 'game-over';
  swingPower: number;
  swingAngle: number;
  strokes: number;
  message: string;
}

export interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
  size: number;
}
