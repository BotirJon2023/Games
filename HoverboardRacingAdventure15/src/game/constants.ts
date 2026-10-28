import type { Vec2 } from './types';

export const CANVAS_WIDTH = 1100;
export const CANVAS_HEIGHT = 650;

export const TRACK_WIDTH = 95;
export const TOTAL_LAPS = 3;

export const COLORS = {
  p1: {
    primary: '#ff2d95',
    glow: '#ff6ec7',
    trail: 'rgba(255, 45, 149, 0.6)',
    name: 'Player 1',
  },
  p2: {
    primary: '#00e5ff',
    glow: '#7df9ff',
    trail: 'rgba(0, 229, 255, 0.6)',
    name: 'Player 2',
  },
  ai: {
    primary: '#00e5ff',
    glow: '#7df9ff',
    trail: 'rgba(0, 229, 255, 0.6)',
    name: 'Computer',
  },
} as const;

export const PHYSICS = {
  acceleration: 0.18,
  brakeForce: 0.28,
  friction: 0.965,
  maxSpeed: 5.5,
  boostMaxSpeed: 9.5,
  boostAcceleration: 0.35,
  turnSpeed: 0.045,
  offTrackFriction: 0.92,
  offTrackMaxSpeed: 2.8,
  collisionBounce: 0.5,
  energyMax: 100,
  energyRegen: 0.25,
  boostDrain: 0.9,
  energyMinToBoost: 15,
} as const;

export const AI_PARAMS = {
  lookAheadDistance: 4,
  steerSmoothing: 0.12,
  boostAggression: 0.65,
  speedVariation: 0.92,
  reactionDelay: 0,
} as const;

/** Control points for the track center line — a flowing circuit. */
export const TRACK_CONTROL_POINTS: Vec2[] = [
  { x: 180, y: 380 },
  { x: 170, y: 180 },
  { x: 350, y: 90 },
  { x: 580, y: 110 },
  { x: 720, y: 220 },
  { x: 660, y: 360 },
  { x: 820, y: 430 },
  { x: 930, y: 320 },
  { x: 980, y: 500 },
  { x: 760, y: 560 },
  { x: 500, y: 520 },
  { x: 310, y: 540 },
  { x: 150, y: 470 },
];

export const BOOST_PAD_POSITIONS: Vec2[] = [
  { x: 400, y: 95 },
  { x: 670, y: 370 },
  { x: 950, y: 500 },
  { x: 300, y: 540 },
];

export const OBSTACLE_POSITIONS: Vec2[] = [
  { x: 250, y: 280 },
  { x: 550, y: 130 },
  { x: 720, y: 290 },
  { x: 870, y: 370 },
  { x: 630, y: 490 },
  { x: 380, y: 450 },
];

export const START_POSITION_OFFSET: Vec2 = { x: 180, y: 380 };
