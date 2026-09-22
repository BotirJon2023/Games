export type GameModeFP = 'vs-cpu' | 'two-players';

export interface FPSegment {
  index: number;
  z: number;
  y: number; // hill height
  curve: number;
  color: 'light' | 'dark';
  sprites: FPSprite[];
}

export interface FPSprite {
  offset: number; // lateral offset from road center
  side: -1 | 1;
  type: 'tree' | 'rock' | 'pine' | 'bush' | 'flag' | 'cliff' | 'milemarker';
}

export interface FPBike {
  id: number;
  name: string;
  color: string;
  isAI: boolean;
  position: number; // z along track
  playerX: number; // lateral -1 to 1
  speed: number;
  maxSpeed: number;
  boost: number;
  boostTimer: number;
  progress: number;
  finished: boolean;
  finishTime: number;
  distance: number;
  // Visual state
  lean: number; // -1 left, 1 right
  bounce: number; // vertical bounce from speed/bumps
  handlebarShake: number; // vibration at high speed or off-road
  windEffect: number; // wind strength visual
  airborne: number; // 0-1 when going over hill crest
  // AI
  aiAggression: number;
  // Stats
  topSpeed: number;
}

export interface FPRaceResult {
  position: number;
  name: string;
  color: string;
  time: number;
  topSpeed: number;
  isAI: boolean;
}

export interface FPGUdateState {
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

export interface FPParticle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  type: 'wind' | 'dust' | 'leaf' | 'spark' | 'confetti';
}

export interface FPCloud {
  x: number;
  y: number;
  scale: number;
  drift: number;
}
