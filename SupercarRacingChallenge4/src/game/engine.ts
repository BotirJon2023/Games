import { CARS, TRACK_LENGTH, NUM_LANES, type GameMode, type RaceResult, type Obstacle } from '@/game/types';

export interface CarState {
  lane: number;
  visualLane: number;
  color: string;
  glowColor: string;
  accentColor: string;
  name: string;
  position: number;
  speed: number;
  topSpeed: number;
  acceleration: number;
  boost: number;
  isBoosting: boolean;
  boostCooldown: number;
  finished: boolean;
  finishTime: number;
  isAI: boolean;
  wobble: number;
  aiBoostTimer?: number;
  aiAggroTimer?: number;
  aiLaneTimer?: number;
  hitFlash: number;
}

export interface GameState {
  cars: CarState[];
  obstacles: Obstacle[];
  mode: GameMode;
  countdown: number;
  racing: boolean;
  finished: boolean;
  startTime: number;
  elapsedTime: number;
  cameraY: number;
}

export interface InputState {
  left: boolean;
  right: boolean;
  boost: boolean;
}

export function createInitialState(mode: GameMode): GameState {
  return {
    cars: [
      makeCar(1, CARS[0], false),
      makeCar(2, CARS[1], mode === 'cpu'),
    ],
    obstacles: generateObstacles(),
    mode,
    countdown: 3.5,
    racing: false,
    finished: false,
    startTime: 0,
    elapsedTime: 0,
    cameraY: 0,
  };
}

function makeCar(lane: number, config: typeof CARS[0], isAI: boolean): CarState {
  return {
    lane,
    visualLane: lane,
    color: config.bodyColor,
    glowColor: config.glowColor,
    accentColor: config.accentColor,
    name: config.name,
    position: 0,
    speed: 0,
    topSpeed: isAI ? 310 + Math.random() * 30 : 320,
    acceleration: 140,
    boost: 100,
    isBoosting: false,
    boostCooldown: 0,
    finished: false,
    finishTime: 0,
    isAI,
    wobble: 0,
    hitFlash: 0,
  };
}

function generateObstacles(): Obstacle[] {
  const obstacles: Obstacle[] = [];
  const count = 28;
  const minGap = TRACK_LENGTH / count;
  for (let i = 0; i < count; i++) {
    const position = 400 + i * minGap + Math.random() * (minGap * 0.5);
    const lane = Math.floor(Math.random() * NUM_LANES);
    obstacles.push({ position, lane, hit: false });
  }
  return obstacles;
}

export function updateGame(state: GameState, dt: number, inputs: InputState[]): void {
  if (state.countdown > 0) {
    state.countdown -= dt;
    if (state.countdown <= 0) {
      state.countdown = 0;
      state.racing = true;
      state.startTime = performance.now();
    }
    return;
  }

  if (state.finished) return;

  state.elapsedTime += dt;

  let allFinished = true;
  for (let i = 0; i < state.cars.length; i++) {
    const car = state.cars[i];
    if (car.finished) continue;
    allFinished = false;

    let input: InputState;
    if (car.isAI) {
      input = getAIInput(car, state, dt);
    } else {
      input = inputs[i] || { left: false, right: false, boost: false };
    }

    updateCar(car, input, dt);
    checkObstacleCollision(car, state.obstacles);
  }

  state.cameraY = Math.max(state.cars[0].position, state.cars[1].position);

  if (allFinished) {
    state.finished = true;
  }
}

function getAIInput(car: CarState, state: GameState, dt: number): InputState {
  const other = state.cars.find(c => c !== car)!;
  const behind = car.position < other.position;
  const gap = Math.abs(car.position - other.position);

  car.aiAggroTimer = (car.aiAggroTimer ?? 0) + dt;
  car.aiLaneTimer = (car.aiLaneTimer ?? 0) + dt;

  // Boost logic
  if (behind && gap > 200 && car.boost > 30 && car.boostCooldown <= 0) {
    car.aiBoostTimer = 1.2;
    car.boostCooldown = 4;
  }
  if (car.aiBoostTimer && car.aiBoostTimer > 0) {
    car.aiBoostTimer -= dt;
  }

  // Obstacle avoidance — check ahead for obstacles in current lane
  let moveLeft = false;
  let moveRight = false;

  const lookahead = car.position + 250;
  const nearbyObstacle = state.obstacles.find(
    o => !o.hit && o.lane === car.lane && o.position > car.position && o.position < lookahead
  );

  if (nearbyObstacle && (car.aiLaneTimer ?? 0) > 0.3) {
    car.aiLaneTimer = 0;
    // Prefer moving toward center, or away from obstacle
    const options: number[] = [];
    if (car.lane > 0) options.push(car.lane - 1);
    if (car.lane < NUM_LANES - 1) options.push(car.lane + 1);
    if (options.length > 0) {
      const target = options[Math.floor(Math.random() * options.length)];
      if (target < car.lane) moveLeft = true;
      else moveRight = true;
    }
  }

  return {
    left: moveLeft,
    right: moveRight,
    boost: !!car.aiBoostTimer && car.aiBoostTimer > 0,
  };
}

function updateCar(car: CarState, input: InputState, dt: number): void {
  if (car.boostCooldown > 0) car.boostCooldown -= dt;
  if (car.hitFlash > 0) car.hitFlash -= dt;

  // Lane switching
  if (input.left && car.lane > 0) {
    car.lane--;
  }
  if (input.right && car.lane < NUM_LANES - 1) {
    car.lane++;
  }

  // Smooth visual lane interpolation
  const laneDiff = car.lane - car.visualLane;
  car.visualLane += laneDiff * Math.min(1, dt * 12);

  // Boost
  if (input.boost && car.boost > 0 && car.boostCooldown <= 0) {
    car.isBoosting = true;
  }
  if (!input.boost || car.boost <= 0) {
    car.isBoosting = false;
  }

  if (car.isBoosting) {
    car.boost = Math.max(0, car.boost - 35 * dt);
  } else {
    car.boost = Math.min(100, car.boost + 12 * dt);
  }

  const targetSpeed = car.isBoosting ? car.topSpeed * 1.45 : car.topSpeed;

  if (car.speed < targetSpeed) {
    const accelMul = car.isBoosting ? 3 : 1;
    car.speed = Math.min(targetSpeed, car.speed + car.acceleration * accelMul * dt);
  } else {
    car.speed = Math.max(targetSpeed, car.speed - 80 * dt);
  }

  car.position += car.speed * dt;
  car.wobble += dt * 8;

  if (car.position >= TRACK_LENGTH) {
    car.position = TRACK_LENGTH;
    car.finished = true;
    car.finishTime = performance.now();
  }
}

function checkObstacleCollision(car: CarState, obstacles: Obstacle[]): void {
  for (const obs of obstacles) {
    if (obs.hit) continue;
    if (obs.lane !== car.lane) continue;
    const dist = Math.abs(obs.position - car.position);
    if (dist < 35) {
      obs.hit = true;
      car.speed *= 0.35;
      car.hitFlash = 0.5;
    }
  }
}

export function getRaceResult(state: GameState): RaceResult {
  const [c1, c2] = state.cars;
  const p1Time = c1.finishTime - state.startTime;
  const p2Time = c2.finishTime - state.startTime;
  const p1Won = c1.finishTime <= c2.finishTime;

  return {
    winnerName: p1Won ? c1.name : c2.name,
    winnerLane: p1Won ? c1.lane : c2.lane,
    winnerTime: p1Won ? p1Time : p2Time,
    loserName: p1Won ? c2.name : c1.name,
    loserLane: p1Won ? c2.lane : c1.lane,
    loserTime: p1Won ? p2Time : p1Time,
    mode: state.mode,
  };
}
