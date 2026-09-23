import type { FPBike } from './types';
import { RoadFP } from './road';
import { clamp } from './projection';

const ACCEL = 0.6;
const BRAKING = -1.5;
const DECEL = -0.3;
const OFF_ROAD_DECEL = -2.0;
const OFF_ROAD_LIMIT = 150;
const MAX_SPEED = 14000;
const BOOST_SPEED = 19000;
const CENTRIFUGAL = 0.35;
const STEER_SPEED = 1.0;

export interface FPControlInput {
  left: boolean;
  right: boolean;
  accelerate: boolean;
  brake: boolean;
  boost: boolean;
}

export function createFPBike(
  id: number,
  name: string,
  color: string,
  isAI: boolean,
  startPos: number
): FPBike {
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
    lean: 0,
    bounce: 0,
    handlebarShake: 0,
    windEffect: 0,
    airborne: 0,
    aiAggression: 0.82 + Math.random() * 0.12,
    topSpeed: 0,
  };
}

export function updateFPBike(
  bike: FPBike,
  input: FPControlInput,
  road: RoadFP,
  dt: number,
  trackLength: number
): void {
  if (bike.finished) {
    bike.speed = Math.max(0, bike.speed + DECEL * 2 * dt);
    bike.position += bike.speed * dt;
    return;
  }

  const seg = road.getSegmentAtZ(bike.position);
  const speedPercent = bike.speed / bike.maxSpeed;
  const dx = dt * 2 * speedPercent;

  // Steering
  const steerAmount = STEER_SPEED * (0.4 + speedPercent * 0.6);
  if (input.left) {
    bike.playerX -= dx * steerAmount;
    bike.lean = clamp(bike.lean - dt * 4, -1, 0);
  } else if (input.right) {
    bike.playerX += dx * steerAmount;
    bike.lean = clamp(bike.lean + dt * 4, 0, 1);
  } else {
    bike.lean *= 0.82;
  }

  // Centrifugal force on curves
  bike.playerX -= dx * speedPercent * seg.curve * CENTRIFUGAL;

  // Speed logic
  const maxSpd = bike.boostTimer > 0 ? BOOST_SPEED : bike.maxSpeed;
  if (input.accelerate) {
    let accel = ACCEL;
    if (input.boost && bike.boost > 0) {
      accel = ACCEL * 2.8;
      bike.boost = Math.max(0, bike.boost - 0.007 * dt);
      bike.boostTimer = 30;
    }
    bike.speed += accel * dt;
  } else if (input.brake) {
    bike.speed += BRAKING * dt;
  } else {
    bike.speed += DECEL * dt;
  }

  bike.speed = clamp(bike.speed, 0, maxSpd);

  // Off-road penalty
  const offRoad = Math.abs(bike.playerX) > 1;
  if (offRoad && bike.speed > OFF_ROAD_LIMIT) {
    bike.speed += OFF_ROAD_DECEL * dt;
    bike.handlebarShake = 3;
  } else {
    bike.handlebarShake *= 0.85;
  }

  // Clamp lateral position
  bike.playerX = clamp(bike.playerX, -2.8, 2.8);

  // Update position
  bike.position += bike.speed * dt;
  while (bike.position >= trackLength) {
    bike.position -= trackLength;
  }

  // Boost timer decay
  if (bike.boostTimer > 0) bike.boostTimer -= dt;

  // Boost charge
  if (bike.speed > bike.maxSpeed * 0.65) {
    bike.boost = Math.min(1, bike.boost + 0.0035 * dt);
  }

  // Bounce based on speed (suspension effect)
  bike.bounce = speedPercent * 4;

  // Vibration at very high speed
  if (bike.speed > bike.maxSpeed * 0.85) {
    bike.handlebarShake = Math.max(bike.handlebarShake, 1.5);
  }

  // Wind effect increases with speed
  bike.windEffect = speedPercent;

  // Airborne detection on hill crests
  const nextSeg = road.getSegmentAtZ(bike.position + 600);
  const heightDiff = nextSeg.y - seg.y;
  if (heightDiff < -250 && bike.speed > bike.maxSpeed * 0.4) {
    bike.airborne = Math.min(1, bike.airborne + dt * 0.06);
  } else {
    bike.airborne = Math.max(0, bike.airborne - dt * 0.04);
  }

  // Track top speed
  bike.topSpeed = Math.max(bike.topSpeed, bike.speed);

  // Progress
  bike.progress = Math.min(1, bike.position / trackLength);
  bike.distance = bike.position;

  // Finish
  if (bike.progress >= 0.999 && !bike.finished) {
    bike.finished = true;
    bike.finishTime = performance.now();
  }
}

export function getFPAIInput(
  bike: FPBike,
  road: RoadFP,
  trackLength: number,
  otherBikes: FPBike[]
): FPControlInput {
  const input: FPControlInput = {
    left: false,
    right: false,
    accelerate: true,
    brake: false,
    boost: false,
  };

  input.accelerate = true;

  // Look ahead for curves
  const lookAhead = 20;
  let futureCurve = 0;
  for (let i = 1; i <= lookAhead; i++) {
    const seg = road.getSegmentAtZ(bike.position + i * road.segmentLength);
    futureCurve += seg.curve * (1 - i / lookAhead); // weight closer segments more
  }

  // Steer toward center of upcoming curve
  if (futureCurve > 1.5) {
    input.right = true;
  } else if (futureCurve < -1.5) {
    input.left = true;
  } else if (futureCurve > 0.5) {
    if (bike.playerX < 0.3) input.right = true;
  } else if (futureCurve < -0.5) {
    if (bike.playerX > -0.3) input.left = true;
  } else {
    // Re-center
    if (bike.playerX > 0.15) input.left = true;
    else if (bike.playerX < -0.15) input.right = true;
  }

  // Brake on sharp curves at high speed
  const currentCurve = Math.abs(road.getSegmentAtZ(bike.position).curve);
  if (currentCurve > 2.5 && bike.speed > bike.maxSpeed * 0.7) {
    input.brake = Math.random() < bike.aiAggression * 0.4;
    input.accelerate = !input.brake;
  }

  // Boost on straights
  if (Math.abs(futureCurve) < 0.4 && bike.boost > 0.35) {
    input.boost = Math.random() < bike.aiAggression * 0.35;
  }

  // Catch-up logic
  const others = otherBikes.filter((b) => b.id !== bike.id);
  const maxOtherProgress = others.length > 0 ? Math.max(...others.map((b) => b.progress)) : 0;
  if (bike.progress < maxOtherProgress - 0.03) {
    if (bike.boost > 0.15) input.boost = Math.random() < bike.aiAggression * 0.6;
  }

  return input;
}
