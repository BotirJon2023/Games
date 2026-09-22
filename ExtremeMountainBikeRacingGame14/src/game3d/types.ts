export interface Segment3D {
  index: number;
  p1: { world: { x: number; y: number; z: number }; camera: { x: number; y: number; z: number }; screen: { x: number; y: number; w: number; scale: number } };
  p2: { world: { x: number; y: number; z: number }; camera: { x: number; y: number; z: number }; screen: { x: number; y: number; w: number; scale: number } };
  curve: number;
  color: 'light' | 'dark';
  clip: number;
  sprites: Sprite3D[];
}

export interface Sprite3D {
  offset: number;
  type: 'tree' | 'rock' | 'sign' | 'flag' | 'bush';
  side: -1 | 1;
}

export interface Bike3DState {
  id: number;
  name: string;
  color: string;
  isAI: boolean;
  position: number; // distance along track
  playerX: number; // lateral position -1 to 1
  speed: number;
  maxSpeed: number;
  boost: number;
  boostTimer: number;
  progress: number;
  finished: boolean;
  finishTime: number;
  distance: number;
  lap: number;
  crashes: number;
  crashTimer: number;
  bounce: number;
  steerVisual: number;
  airborne: number;
  trickScore: number;
  // AI state
  aiTargetX: number;
  aiAggression: number;
}

export type GameMode3D = 'vs-cpu' | 'two-players';

export interface RaceResult3D {
  position: number;
  name: string;
  color: string;
  time: number;
  topSpeed: number;
  isAI: boolean;
}

export interface GameUpdateState3D {
  countdown: number;
  racing: boolean;
  elapsed: number;
  bikes: {
    id: number;
    name: string;
    color: string;
    progress: number;
    speed: number;
    boost: number;
    finished: boolean;
    isAI: boolean;
  }[];
}

export interface Particle3D {
  x: number;
  y: number;
  z: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  type: 'dust' | 'spark' | 'confetti';
}

export interface Cloud3D {
  x: number;
  y: number;
  z: number;
  scale: number;
}
