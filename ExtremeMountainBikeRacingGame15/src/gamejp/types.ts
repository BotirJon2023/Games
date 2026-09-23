export type GameModeJP = 'vs-cpu' | 'two-players';

export interface JPBike {
  id: number;
  name: string;
  color: string;
  isAI: boolean;
  x: number;
  y: number;
  vx: number;
  vy: number;
  wheelRotation: number;
  tilt: number;
  onGround: boolean;
  pedalPhase: number;
  airRotation: number;
  flips: number;
  trickScore: number;
  crashed: boolean;
  crashTimer: number;
  distance: number;
  progress: number;
  finished: boolean;
  finishTime: number;
  boost: number;
  boostTimer: number;
  prevX: number;
  prevY: number;
}

export interface JPParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  type: 'dust' | 'spark' | 'sakura' | 'crash' | 'confetti';
  rotation: number;
  rotationSpeed: number;
}

export interface JPSakura {
  x: number;
  y: number;
  vx: number;
  vy: number;
  rotation: number;
  rotationSpeed: number;
  size: number;
  petalCount: number;
}

export interface JPBuilding {
  x: number;
  type: 'pagoda' | 'torii' | 'shrine' | 'temple';
  scale: number;
}

export interface JPRaceResult {
  position: number;
  name: string;
  color: string;
  time: number;
  tricks: number;
  isAI: boolean;
}

export interface JPUpdateState {
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
    flips: number;
    trickScore: number;
    crashed: boolean;
    finished: boolean;
    isAI: boolean;
  }[];
}

export interface JPVec2 {
  x: number;
  y: number;
}
