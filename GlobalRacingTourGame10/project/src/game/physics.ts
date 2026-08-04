import type { Car, Track, Vec2, CarInput, RaceState } from './types';

const ENGINE = 760;        // accel force
const REVERSE = 320;
const BRAKE = 1400;
const MAX_SPEED = 560;     // px/s
const MAX_REVERSE = 180;
const DRAG = 0.0009;
const ROLLING = 18;
const GRIP = 6.4;          // lateral grip (higher = less slide)
const HANDBRAKE_GRIP = 1.4;
const TURN_RATE = 3.1;     // rad/s at full lock
const OFFROAD_GRIP = 2.0;
const OFFROAD_MAX = 240;

export function createInput(): CarInput {
  return { throttle: 0, brake: 0, steering: 0, handbrake: false };
}

export function makeCar(
  id: string,
  name: string,
  colorId: import('./types').CarColorId,
  body: string,
  accent: string,
  isAI: boolean,
  aiSkill: number,
  startOffset: number,
  track: Track,
  name2?: string
): Car {
  const car: Car = {
    id,
    name: name2 ?? name,
    colorId,
    body,
    accent,
    pos: { x: 0, y: 0 },
    angle: 0,
    vel: { x: 0, y: 0 },
    input: createInput(),
    lap: 0,
    checkpoint: 0,
    lastSample: 0,
    lapStart: 0,
    bestLap: 0,
    lastLap: 0,
    finished: false,
    finishTime: 0,
    place: 0,
    driftAmount: 0,
    offRoad: false,
    isAI,
    aiSkill,
    aiWobble: Math.random() * Math.PI * 2,
    progress: 0,
    wheelSpin: 0,
    brakeLight: 0,
    lean: 0,
  };
  placeCarAtStart(car, startOffset, track);
  return car;
}

export function placeCarAtStart(car: Car, offset: number, track: Track): void {
  const s = track.samples!;
  const i = 2;
  const p = s[i];
  const dir = tangentAt(track, i);
  const n = track.normals![i];
  const lateral = offset * 22;
  car.pos = { x: p.x + n.x * lateral, y: p.y + n.y * lateral };
  car.angle = Math.atan2(dir.y, dir.x);
  car.vel = { x: 0, y: 0 };
  car.lap = 0;
  car.checkpoint = 0;
  car.lastSample = i;
  car.lapStart = 0;
  car.finished = false;
  car.place = 0;
  car.progress = 0;
  car.driftAmount = 0;
}

function tangentAt(track: Track, i: number): Vec2 {
  const s = track.samples!;
  const n = s.length;
  const a = s[(i - 1 + n) % n];
  const b = s[(i + 1) % n];
  const dx = b.x - a.x;
  const dy = b.y - a.y;
  const len = Math.hypot(dx, dy) || 1;
  return { x: dx / len, y: dy / len };
}

export function updateCar(car: Car, track: Track, dt: number, raceTime: number): void {
  if (car.finished) {
    car.input.throttle = 0;
    car.input.brake = 0.35;
    car.input.steering = 0;
  }
  if (car.isAI) updateAI(car, track);

  applyPhysics(car, track, dt);
  updateLapLogic(car, track, raceTime);
  updateVisuals(car, dt);
}

function applyPhysics(car: Car, track: Track, dt: number): void {
  const forward: Vec2 = { x: Math.cos(car.angle), y: Math.sin(car.angle) };
  const right: Vec2 = { x: -Math.sin(car.angle), y: Math.cos(car.angle) };

  let forwardSpeed = car.vel.x * forward.x + car.vel.y * forward.y;
  const lateralSpeed = car.vel.x * right.x + car.vel.y * right.y;

  const inp = car.input;
  let accel = 0;
  if (forwardSpeed > 5 || Math.abs(forwardSpeed) < 5) {
    if (inp.throttle > 0) accel += ENGINE * inp.throttle;
    if (inp.brake > 0) {
      if (forwardSpeed > 10) accel -= BRAKE * inp.brake;
      else accel -= REVERSE * inp.brake;
    }
  } else if (inp.throttle > 0) {
    accel += ENGINE * inp.throttle;
  }

  const speed = Math.hypot(car.vel.x, car.vel.y);
  const grip = inp.handbrake ? HANDBRAKE_GRIP : car.offRoad ? OFFROAD_GRIP : GRIP;
  const lateralFric = lateralSpeed * grip;

  let maxSpeed = car.offRoad ? OFFROAD_MAX : MAX_SPEED;
  if (forwardSpeed > maxSpeed) forwardSpeed = maxSpeed;
  if (forwardSpeed < -MAX_REVERSE) forwardSpeed = -MAX_REVERSE;

  forwardSpeed += accel * dt;
  forwardSpeed -= forwardSpeed * DRAG * speed;
  forwardSpeed -= Math.sign(forwardSpeed) * ROLLING * dt;
  if (Math.abs(forwardSpeed) < 1) forwardSpeed = 0;

  const newLateral = lateralSpeed - lateralFric * dt;
  const newForward = forwardSpeed;

  let newAngle = car.angle;
  const steer = inp.steering * TURN_RATE * dt;
  const speedFactor = Math.min(1, Math.abs(newForward) / 80);
  const dirSign = newForward >= 0 ? 1 : -1;
  newAngle += steer * speedFactor * dirSign;

  const nf = { x: Math.cos(newAngle), y: Math.sin(newAngle) };
  const nr = { x: -Math.sin(newAngle), y: Math.cos(newAngle) };
  car.vel.x = nf.x * newForward + nr.x * newLateral;
  car.vel.y = nf.y * newForward + nr.y * newLateral;
  car.pos.x += car.vel.x * dt;
  car.pos.y += car.vel.y * dt;
  car.angle = newAngle;

  car.driftAmount = Math.abs(lateralSpeed);
}

function updateLapLogic(car: Car, track: Track, raceTime: number): void {
  const s = track.samples!;
  const n = s.length;
  const idx = closestSample(car.pos, track, car.lastSample);
  car.lastSample = idx;
  car.progress = idx / n;

  const checks = track.checkpoints!;
  const next = (car.checkpoint + 1) % checks.length;
  const targetIdx = checks[next];
  if (idx === targetIdx || nearSample(idx, targetIdx, n, 2)) {
    car.checkpoint = next;
    if (next === 0) {
      const lapTime = raceTime - car.lapStart;
      if (car.lap > 0) {
        if (car.bestLap === 0 || lapTime < car.bestLap) car.bestLap = lapTime;
        car.lastLap = lapTime;
      }
      car.lap++;
      car.lapStart = raceTime;
      if (car.lap > track.laps && !car.finished) {
        car.finished = true;
        car.finishTime = raceTime;
      }
    }
  }
}

function nearSample(a: number, b: number, n: number, tol: number): boolean {
  let d = Math.abs(a - b);
  if (d > n / 2) d = n - d;
  return d <= tol;
}

export function closestSample(pos: Vec2, track: Track, hint: number): number {
  const s = track.samples!;
  const n = s.length;
  let best = hint;
  let bestD = Infinity;
  for (let off = -8; off <= 8; off++) {
    const i = (hint + off + n) % n;
    const d = (s[i].x - pos.x) ** 2 + (s[i].y - pos.y) ** 2;
    if (d < bestD) { bestD = d; best = i; }
  }
  for (let i = 0; i < n; i += 4) {
    const d = (s[i].x - pos.x) ** 2 + (s[i].y - pos.y) ** 2;
    if (d < bestD) { bestD = d; best = i; }
  }
  return best;
}

function updateVisuals(car: Car, dt: number): void {
  const speed = Math.hypot(car.vel.x, car.vel.y);
  car.wheelSpin += speed * dt * 0.02;
  const targetLean = -car.input.steering * Math.min(1, speed / 300) * 0.14;
  car.lean += (targetLean - car.lean) * Math.min(1, dt * 8);
  const targetBrake = car.input.brake > 0 && speed > 20 ? 1 : 0;
  car.brakeLight += (targetBrake - car.brakeLight) * Math.min(1, dt * 10);
}

export function computeOffRoad(car: Car, track: Track): boolean {
  const s = track.samples!;
  const i = car.lastSample;
  const n = track.normals![i];
  const p = s[i];
  const dx = car.pos.x - p.x;
  const dy = car.pos.y - p.y;
  const lateral = Math.abs(dx * n.x + dy * n.y);
  return lateral > track.halfWidth - 6;
}

function updateAI(car: Car, track: Track): void {
  const s = track.samples!;
  const n = s.length;
  const lookAhead = 7 + Math.floor(car.aiSkill * 5);
  const target = s[(car.lastSample + lookAhead) % n];
  const dx = target.x - car.pos.x;
  const dy = target.y - car.pos.y;
  const desired = Math.atan2(dy, dx);
  let diff = desired - car.angle;
  while (diff > Math.PI) diff -= Math.PI * 2;
  while (diff < -Math.PI) diff += Math.PI * 2;

  car.input.steering = Math.max(-1, Math.min(1, diff * 2.2));
  car.aiWobble += 0.05;
  car.input.steering += Math.sin(car.aiWobble) * (1 - car.aiSkill) * 0.12;

  const speed = Math.hypot(car.vel.x, car.vel.y);
  const sharp = Math.abs(diff) > 0.5;
  if (sharp && speed > 280) {
    car.input.throttle = 0.4;
    car.input.brake = 0.5;
  } else {
    car.input.throttle = 1;
    car.input.brake = 0;
  }
  car.input.handbrake = sharp && speed > 220 && car.aiSkill > 0.6 && Math.abs(diff) > 0.9;
}

export function updateRace(state: RaceState, dt: number): void {
  if (!state.started) {
    state.countdown -= dt;
    if (state.countdown <= 0) {
      state.countdown = 0;
      state.started = true;
      state.raceTime = 0;
      state.cars.forEach((c) => (c.lapStart = 0));
    }
    return;
  }

  state.raceTime += dt;
  state.cars.forEach((c) => {
    if (!c.finished) {
      c.offRoad = computeOffRoad(c, state.track);
      updateCar(c, state.track, dt, state.raceTime);
    } else {
      updateCar(c, state.track, dt, state.raceTime);
    }
  });

  state.finishedCars = state.cars.filter((c) => c.finished).length;

  if (state.finishedCars >= state.cars.length) {
    state.confettiTimer += dt;
  }

  updatePlacements(state);
  updateParticles(state, dt);
  updateTireMarks(state, dt);
}

function updatePlacements(state: RaceState): void {
  const ranked = [...state.cars].sort((a, b) => {
    if (a.finished && b.finished) return a.finishTime - b.finishTime;
    if (a.finished) return -1;
    if (b.finished) return 1;
    const pa = a.lap + a.progress;
    const pb = b.lap + b.progress;
    return pb - pa;
  });
  ranked.forEach((c, i) => (c.place = i + 1));
}

function updateParticles(state: RaceState, dt: number): void {
  state.particles = state.particles.filter((p) => {
    p.life -= dt;
    p.x += p.vx * dt;
    p.y += p.vy * dt;
    p.vx *= 0.96;
    p.vy *= 0.96;
    return p.life > 0;
  });

  state.cars.forEach((c) => {
    const speed = Math.hypot(c.vel.x, c.vel.y);
    if (c.driftAmount > 70 && speed > 120 && Math.random() < 0.6) {
      state.particles.push({
        x: c.pos.x + (Math.random() - 0.5) * 14,
        y: c.pos.y + (Math.random() - 0.5) * 14,
        vx: (Math.random() - 0.5) * 30,
        vy: (Math.random() - 0.5) * 30,
        life: 0.7 + Math.random() * 0.4,
        maxLife: 1.1,
        size: 6 + Math.random() * 6,
        type: c.offRoad ? 'dust' : 'smoke',
        color: c.offRoad ? '#caa45a' : '#cbd5e1',
      });
    }
    if (c.finished && state.confettiTimer < 6 && Math.random() < 0.5) {
      const colors = ['#ef4444', '#fbbf24', '#22d3ee', '#84cc16', '#f472b6', '#a78bfa'];
      state.particles.push({
        x: c.pos.x + (Math.random() - 0.5) * 30,
        y: c.pos.y + (Math.random() - 0.5) * 30,
        vx: (Math.random() - 0.5) * 120,
        vy: -80 - Math.random() * 80,
        life: 1.5,
        maxLife: 1.5,
        size: 5 + Math.random() * 4,
        type: 'confetti',
        color: colors[Math.floor(Math.random() * colors.length)],
      });
    }
  });

  if (state.particles.length > 300) {
    state.particles.splice(0, state.particles.length - 300);
  }
}

function updateTireMarks(state: RaceState, dt: number): void {
  state.cars.forEach((c) => {
    const speed = Math.hypot(c.vel.x, c.vel.y);
    if (c.driftAmount > 80 && speed > 140 && !c.offRoad) {
      for (let w = 0; w < 2; w++) {
        const off = w === 0 ? -10 : 10;
        const rx = -Math.sin(c.angle) * off;
        const ry = Math.cos(c.angle) * off;
        state.tireMarks.push({
          x: c.pos.x + rx,
          y: c.pos.y + ry,
          alpha: 1,
          angle: c.angle,
          intensity: Math.min(1, c.driftAmount / 250),
        });
      }
    }
  });
  state.tireMarks = state.tireMarks.filter((m) => {
    m.alpha -= dt * 0.18;
    return m.alpha > 0;
  });
  if (state.tireMarks.length > 1200) {
    state.tireMarks.splice(0, state.tireMarks.length - 1200);
  }
}

export function trackProgressFraction(car: Car, track: Track): number {
  return (car.lap + car.progress) / track.laps;
}
