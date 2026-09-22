export type GameMode = 'vs-cpu' | 'two-players';

export type Screen = 'menu' | 'racing' | 'finished';

export interface Vec2 {
  x: number;
  y: number;
}

export interface Bike {
  id: number;
  name: string;
  color: string;
  isAI: boolean;
  /** Position along the terrain heightmap */
  x: number;
  /** Vertical position on screen */
  y: number;
  /** Horizontal velocity */
  vx: number;
  /** Vertical velocity */
  vy: number;
  /** Wheel rotation in radians */
  wheelRotation: number;
  /** Lean/tilt angle of the bike in radians */
  tilt: number;
  /** Whether on the ground */
  onGround: boolean;
  /** Pedaling cycle for leg animation */
  pedalPhase: number;
  /** Air rotation accumulator */
  airRotation: number;
  /** Number of backflips performed */
  flips: number;
  /** Tricks score */
  trickScore: number;
  /** Whether the rider is ragdolling from crash */
  crashed: boolean;
  /** Crash timer */
  crashTimer: number;
  /** Total distance traveled */
  distance: number;
  /** Lap progress 0-1 */
  progress: number;
  /** Finished race */
  finished: boolean;
  /** Finish time in ms */
  finishTime: number;
  /** Boost charge 0-1 */
  boost: number;
  /** Boost timer */
  boostTimer: number;
  /** Current gear visual */
  gear: number;
  /** Previous position for delta calc */
  prevX: number;
  prevY: number;
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
  type: 'dust' | 'spark' | 'crash' | 'confetti';
  rotation: number;
  rotationSpeed: number;
}

export interface CloudParticle {
  x: number;
  y: number;
  scale: number;
  speed: number;
}

export interface Checkpoint {
  x: number;
  passedBy: number[];
}

export interface RaceResult {
  position: number;
  name: string;
  color: string;
  time: number;
  tricks: number;
  isAI: boolean;
}
