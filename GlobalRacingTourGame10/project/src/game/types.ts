export type GameMode = 'single' | 'vs-player' | 'vs-computer';

export type CarColorId = 'red' | 'blue' | 'green' | 'orange' | 'purple' | 'cyan';

export interface Vec2 {
  x: number;
  y: number;
}

export type SceneryType =
  | 'tree'
  | 'palm'
  | 'cactus'
  | 'rock'
  | 'mountain'
  | 'building'
  | 'pyramid'
  | 'tent'
  | 'ferris'
  | 'lighthouse'
  | 'sanddune'
  | 'flag'
  | 'snowtree';

export interface SceneryItem {
  pos: Vec2;
  type: SceneryType;
  scale: number;
  side: number; // -1 left, 1 right of road
  hue?: number;
}

export interface TrackTheme {
  ground: string;
  groundDark: string;
  road: string;
  roadDark: string;
  curbA: string;
  curbB: string;
  accent: string;
  skyTop: string;
  skyBottom: string;
}

export interface Track {
  id: string;
  name: string;
  city: string;
  country: string;
  flag: string;
  description: string;
  laps: number;
  difficulty: number; // 1..3
  halfWidth: number;
  controlPoints: Vec2[];
  theme: TrackTheme;
  sceneryTypes: SceneryType[];
  accent: string;
  // Built at runtime by buildTrack()
  samples?: Vec2[];
  normals?: Vec2[];
  cumDist?: number[];
  totalLength?: number;
  leftEdge?: Vec2[];
  rightEdge?: Vec2[];
  checkpoints?: number[]; // sample indices
  scenery?: SceneryItem[];
}

export interface CarInput {
  throttle: number; // 0..1
  brake: number; // 0..1
  steering: number; // -1..1
  handbrake: boolean;
}

export interface Car {
  id: string;
  name: string;
  colorId: CarColorId;
  body: string;
  accent: string;
  pos: Vec2;
  angle: number;
  vel: Vec2;
  input: CarInput;
  lap: number;
  checkpoint: number; // index into track.checkpoints
  lastSample: number;
  lapStart: number;
  bestLap: number; // ms
  lastLap: number; // ms
  finished: boolean;
  finishTime: number;
  place: number;
  driftAmount: number;
  offRoad: boolean;
  isAI: boolean;
  aiSkill: number;
  aiWobble: number;
  progress: number; // 0..1 around the loop
  // visual
  wheelSpin: number;
  brakeLight: number;
  lean: number;
}

export interface TireMark {
  x: number;
  y: number;
  alpha: number;
  angle: number;
  intensity: number;
}

export interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  size: number;
  type: 'smoke' | 'dust' | 'spark' | 'confetti';
  color: string;
}

export interface CameraState {
  x: number;
  y: number;
  zoom: number;
  shake: number;
}

export interface RaceState {
  track: Track;
  cars: Car[];
  tireMarks: TireMark[];
  particles: Particle[];
  started: boolean;
  countdown: number; // seconds remaining, 0 = go
  raceTime: number;
  finishedCars: number;
  totalLaps: number;
  confettiTimer: number;
}

export interface CarOption {
  id: CarColorId;
  name: string;
  body: string;
  accent: string;
}

export const CAR_OPTIONS: CarOption[] = [
  { id: 'red', name: 'Scarlet', body: '#e63946', accent: '#ffd166' },
  { id: 'blue', name: 'Azure', body: '#1d4ed8', accent: '#f1f5f9' },
  { id: 'green', name: 'Emerald', body: '#16a34a', accent: '#bbf7d0' },
  { id: 'orange', name: 'Sunset', body: '#ea580c', accent: '#fef3c7' },
  { id: 'purple', name: 'Amethyst', body: '#7c3aed', accent: '#ddd6fe' },
  { id: 'cyan', name: 'Aqua', body: '#0891b2', accent: '#cffafe' },
];

export function carOption(id: CarColorId): CarOption {
  return CAR_OPTIONS.find((c) => c.id === id) ?? CAR_OPTIONS[0];
}
