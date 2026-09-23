import type { Bike3DState } from './types';
import { Road3D } from './road';
import { limit, accelerate } from './projection';

const ACCEL = 0.5;
const BRAKING = -1.2;
const DECEL = -0.25;
const OFF_ROAD_DECEL = -1.5;
const OFF_ROAD_LIMIT = 200;
const MAX_SPEED = 12000;
const BOOST_SPEED = 16000;
const CENTRIFUGAL = 0.3;
const STEER_SPEED = 1.2;
const STEER_MAX = 1.0;

export interface ControlInput3D {
  left: boolean;
  right: boolean;
  accelerate: boolean;
  brake: boolean;
  boost: boolean;
}

export function createBike3D(
  id: number,
  name: string,
  color: string,
  isAI: boolean,
  startPos: number
): Bike3DState {
  return {
    id,
    name,
    color,
    isAI,
    position: startPos,
    playerX: 0,
    speed: 0,
    maxSpeed: MAX_SPEED,
    boost: 0,
    boostTimer: 0,
    progress: 0,
    finished: false,
    finishTime: 0,
    distance: 0,
    lap: 1,
    crashes: 0,
    crashTimer: 0,
    bounce: 0,
    steerVisual: 0,
    airborne: 0,
    trickScore: 0,
    aiTargetX: 0,
    aiAggression: 0.85 + Math.random() * 0.1,
  };
}

export function updateBike3D(
  bike: Bike3DState,
  input: ControlInput3D,
  road: Road3D,
  dt: number,
  trackLength: number
): void {
  if (bike.finished) {
    bike.speed = accelerate(bike.speed, DECEL * 2, dt);
    if (bike.speed < 0) bike.speed = 0;
    bike.position += bike.speed * dt;
    return;
  }

  const seg = road.getSegmentAtZ(bike.position);
  const speedPercent = bike.speed / bike.maxSpeed;
  const dx = dt * 2 * speedPercent;

  // Steering
  if (input.left) {
    bike.playerX -= dx * STEER_SPEED * (0.5 + speedPercent * 0.5);
    bike.steerVisual = limit(bike.steerVisual - dt * 5, -1, 0);
  } else if (input.right) {
    bike.playerX += dx * STEER_SPEED * (0.5 + speedPercent * 0.5);
    bike.steerVisual = limit(bike.steerVisual + dt * 5, 0, 1);
  } else {
    bike.steerVisual *= 0.85;
  }

  // Centrifugal force on curves
  bike.playerX -= dx * speedPercent * seg.curve * CENTRIFUGAL;

  // Speed logic
  const maxSpd = bike.boostTimer > 0 ? BOOST_SPEED : bike.maxSpeed;
  if (input.accelerate) {
    let accel = ACCEL;
    if (input.boost && bike.boost > 0) {
      accel = ACCEL * 2.5;
      bike.boost = Math.max(0, bike.boost - 0.008 * dt);
      bike.boostTimer = 30;
    }
    bike.speed = accelerate(bike.speed, accel, dt);
  } else if (input.brake) {
    bike.speed = accelerate(bike.speed, BRAKING, dt);
  } else {
    bike.speed = accelerate(bike.speed, DECEL, dt);
  }

  // Clamp speed
  if (bike.speed > maxSpd) bike.speed = maxSpd;
  if (bike.speed < 0) bike.speed = 0;

  // Off-road penalty
  if ((bike.playerX < -1 || bike.playerX > 1) && bike.speed > OFF_ROAD_LIMIT) {
    bike.speed = accelerate(bike.speed, OFF_ROAD_DECEL, dt);
  }

  // Clamp player position (allow off-road but with limits)
  bike.playerX = limit(bike.playerX, -2.5, 2.5);

  // Update position
  bike.position += bike.speed * dt;
  while (bike.position >= trackLength) {
    bike.position -= trackLength;
    bike.lap++;
  }

  // Boost timer decay
  if (bike.boostTimer > 0) {
    bike.boostTimer -= dt;
  }

  // Boost charge from speed
  if (bike.speed > bike.maxSpeed * 0.7) {
    bike.boost = Math.min(1, bike.boost + 0.003 * dt);
  }

  // Bounce animation based on speed
  bike.bounce = Math.abs(bike.speed / bike.maxSpeed) * 3;

  // Airborne detection (hills)
  const nextSeg = road.getSegmentAtZ(bike.position + 500);
  const heightDiff = nextSeg.p1.world.y - seg.p1.world.y;
  if (heightDiff < -200 && bike.speed > bike.maxSpeed * 0.5) {
    bike.airborne = Math.min(1, bike.airborne + dt * 0.05);
  } else {
    bike.airborne = Math.max(0, bike.airborne - dt * 0.03);
  }

  // Progress
  bike.progress = Math.min(1, bike.position / trackLength);
  bike.distance = bike.position;

  // Check finish (single lap for now)
  if (bike.progress >= 0.999 && !bike.finished) {
    bike.finished = true;
    bike.finishTime = performance.now();
  }
}

export function getAIInput3D(
  bike: Bike3DState,
  road: Road3D,
  trackLength: number,
  otherBikes: Bike3DState[]
): ControlInput3D {
  const input: ControlInput3D = {
    left: false,
    right: false,
    accelerate: true,
    brake: false,
    boost: false,
  };

  // Always accelerate
  input.accelerate = true;

  // Look ahead for curves
  const lookAhead = 15;
  const currentSeg = road.getSegmentAtZ(bike.position);
  let futureCurve = 0;
  for (let i = 1; i <= lookAhead; i++) {
    const seg = road.getSegmentAtZ(bike.position + i * road.segmentLength);
    futureCurve += seg.curve;
  }

  // Steer based on upcoming curves
  if (futureCurve > 1) {
    input.right = true;
    // Aim toward right side for left-curving road
    bike.aiTargetX = 0.3;
  } else if (futureCurve < -1) {
    input.left = true;
    bike.aiTargetX = -0.3;
  } else {
    // Center
    if (bike.playerX > 0.1) input.left = true;
    else if (bike.playerX < -0.1) input.right = true;
    bike.aiTargetX = 0;
  }

  // Adjust for sharp curves
  const sharpCurve = Math.abs(currentSeg.curve);
  if (sharpCurve > 2 && bike.speed > bike.maxSpeed * 0.75) {
    input.brake = Math.random() < bike.aiAggression * 0.3;
  }

  // Use boost on straight sections
  if (Math.abs(futureCurve) < 0.5 && bike.boost > 0.4 && Math.random() < bike.aiAggression * 0.3) {
    input.boost = true;
  }

  // Check if behind - be more aggressive
  const otherProgress = Math.max(...otherBikes.filter(b => b.id !== bike.id).map(b => b.progress));
  if (bike.progress < otherProgress - 0.02) {
    if (bike.boost > 0.2) input.boost = Math.random() < bike.aiAggression * 0.5;
  }

  return input;
}
