import type { JPBike, JPParticle } from './types';
import { JPTerrain } from './terrain';

const GRAVITY = 0.45;
const GROUND_FRICTION = 0.985;
const AIR_FRICTION = 0.998;
const PEDAL_FORCE = 0.33;
const BOOST_FORCE = 0.62;
const MAX_SPEED = 15;
const BOOST_SPEED = 21;
const AIR_ROTATION_SPEED = 0.09;
const WHEEL_RADIUS = 16;

export interface JPControlInput {
  accelerate: boolean;
  brake: boolean;
  rotateLeft: boolean;
  rotateRight: boolean;
  boost: boolean;
}

export function createJPBike(
  id: number,
  name: string,
  color: string,
  isAI: boolean,
  startX: number,
  terrain: JPTerrain
): JPBike {
  return {
    id, name, color, isAI,
    x: startX,
    y: terrain.getHeightAt(startX) - WHEEL_RADIUS,
    vx: 0, vy: 0,
    wheelRotation: 0, tilt: 0,
    onGround: true,
    pedalPhase: 0, airRotation: 0,
    flips: 0, trickScore: 0,
    crashed: false, crashTimer: 0,
    distance: 0, progress: 0,
    finished: false, finishTime: 0,
    boost: 0, boostTimer: 0,
    prevX: startX, prevY: 0,
  };
}

export function updateJPBike(
  bike: JPBike,
  input: JPControlInput,
  terrain: JPTerrain,
  dt: number,
  particles: JPParticle[],
  trackLength: number
): void {
  if (bike.finished) {
    bike.vx *= 0.96;
    bike.vy *= 0.96;
    bike.x += bike.vx * dt;
    applyGroundCollision(bike, terrain);
    bike.wheelRotation += bike.vx * 0.06 * dt;
    return;
  }

  if (bike.crashed) {
    bike.crashTimer -= dt;
    bike.vx *= 0.9;
    bike.vy += GRAVITY * dt;
    bike.x += bike.vx * dt;
    bike.y += bike.vy * dt;
    bike.tilt += 0.05 * dt;
    applyGroundCollision(bike, terrain);
    if (bike.crashTimer <= 0) {
      bike.crashed = false;
      bike.vx = 0; bike.vy = 0;
      bike.tilt = terrain.getAngleAt(bike.x);
      bike.y = terrain.getHeightAt(bike.x) - WHEEL_RADIUS;
    }
    return;
  }

  bike.prevX = bike.x;
  bike.prevY = bike.y;

  const groundY = terrain.getHeightAt(bike.x);
  const distFromGround = groundY - bike.y - WHEEL_RADIUS;
  const wasOnGround = bike.onGround;
  bike.onGround = distFromGround >= -1;

  if (bike.onGround) {
    const slope = terrain.getAngleAt(bike.x);
    const slopeAccel = Math.sin(slope) * GRAVITY * 2.5;
    bike.vx += slopeAccel * dt;

    if (input.accelerate) {
      let force = PEDAL_FORCE;
      if (input.boost && bike.boost > 0) {
        force = BOOST_FORCE;
        bike.boost -= 0.01 * dt;
        bike.boostTimer = 30;
      }
      bike.vx += force * dt;
      bike.pedalPhase += 0.25 * dt;
    }

    if (input.brake) {
      bike.vx -= PEDAL_FORCE * 0.6 * dt;
      bike.pedalPhase -= 0.1 * dt;
    }

    bike.vx *= Math.pow(GROUND_FRICTION, dt);

    const maxSpd = bike.boostTimer > 0 ? BOOST_SPEED : MAX_SPEED;
    if (bike.vx > maxSpd) bike.vx = maxSpd;
    if (bike.vx < -MAX_SPEED * 0.4) bike.vx = -MAX_SPEED * 0.4;

    let tiltDiff = slope - bike.tilt;
    while (tiltDiff > Math.PI) tiltDiff -= Math.PI * 2;
    while (tiltDiff < -Math.PI) tiltDiff += Math.PI * 2;
    bike.tilt += tiltDiff * 0.2 * dt;

    bike.airRotation = 0;
    bike.y = groundY - WHEEL_RADIUS;
    bike.vy = 0;

    if (input.accelerate && bike.vx > 2 && Math.random() < 0.3) {
      particles.push({
        x: bike.x - bike.vx * 2,
        y: groundY - 2,
        vx: -bike.vx * 0.3 + (Math.random() - 0.5) * 2,
        vy: -Math.random() * 3 - 1,
        life: 30 + Math.random() * 20,
        maxLife: 50,
        size: 3 + Math.random() * 5,
        color: '#a89060',
        type: 'dust',
        rotation: 0, rotationSpeed: 0,
      });
    }

    if (!wasOnGround && Math.abs(bike.vy) > 3) {
      for (let i = 0; i < 8; i++) {
        particles.push({
          x: bike.x + (Math.random() - 0.5) * 30,
          y: groundY,
          vx: (Math.random() - 0.5) * 6,
          vy: -Math.random() * 4 - 2,
          life: 25 + Math.random() * 15,
          maxLife: 40,
          size: 4 + Math.random() * 6,
          color: '#b8a070',
          type: 'dust',
          rotation: 0, rotationSpeed: 0,
        });
      }
      const halfFlips = Math.floor(Math.abs(bike.airRotation) / Math.PI);
      if (halfFlips >= 1) {
        bike.trickScore += halfFlips * 60;
        bike.flips += Math.floor(halfFlips / 2);
      }
      bike.airRotation = 0;
    }
  } else {
    bike.vy += GRAVITY * dt;
    bike.vx *= Math.pow(AIR_FRICTION, dt);

    if (input.rotateLeft) {
      bike.tilt -= AIR_ROTATION_SPEED * dt;
      bike.airRotation -= AIR_ROTATION_SPEED * dt;
    }
    if (input.rotateRight) {
      bike.tilt += AIR_ROTATION_SPEED * dt;
      bike.airRotation += AIR_ROTATION_SPEED * dt;
    }

    const velAngle = Math.atan2(bike.vy, bike.vx);
    if (!input.rotateLeft && !input.rotateRight) {
      let diff = velAngle - bike.tilt;
      while (diff > Math.PI) diff -= Math.PI * 2;
      while (diff < -Math.PI) diff += Math.PI * 2;
      bike.tilt += diff * 0.03 * dt;
    }
  }

  bike.x += bike.vx * dt;
  bike.y += bike.vy * dt;

  if (bike.boostTimer > 0) {
    bike.boostTimer -= dt;
    if (Math.random() < 0.6) {
      particles.push({
        x: bike.x - 10, y: bike.y,
        vx: -bike.vx * 0.2 - Math.random() * 3,
        vy: (Math.random() - 0.5) * 2,
        life: 15 + Math.random() * 10,
        maxLife: 25,
        size: 3 + Math.random() * 4,
        color: '#ff6b00',
        type: 'spark',
        rotation: 0, rotationSpeed: 0.2,
      });
    }
  }

  bike.wheelRotation += bike.vx * 0.06 * dt;

  if (bike.onGround && Math.abs(bike.vx) > 8) {
    bike.boost = Math.min(1, bike.boost + 0.003 * dt);
  }

  bike.distance = bike.x;
  bike.progress = Math.min(1, Math.max(0, bike.x / trackLength));

  if (!bike.onGround && bike.y > groundY - WHEEL_RADIUS + 5) {
    const normalizedTilt = ((bike.tilt % (Math.PI * 2)) + Math.PI * 2) % (Math.PI * 2);
    const isUpsideDown = normalizedTilt > Math.PI * 0.6 && normalizedTilt < Math.PI * 1.4;
    if (isUpsideDown && Math.abs(bike.vy) > 3) {
      crashJPBike(bike, particles);
    } else {
      bike.y = groundY - WHEEL_RADIUS;
      bike.vy = Math.min(bike.vy, 0);
    }
  }

  if (bike.x >= trackLength && !bike.finished) {
    bike.finished = true;
    bike.finishTime = performance.now();
  }
}

function applyGroundCollision(bike: JPBike, terrain: JPTerrain): void {
  const groundY = terrain.getHeightAt(bike.x);
  if (bike.y + WHEEL_RADIUS > groundY) {
    bike.y = groundY - WHEEL_RADIUS;
    bike.vy = Math.min(bike.vy, 0);
    bike.onGround = true;
    bike.tilt = terrain.getAngleAt(bike.x);
  }
}

function crashJPBike(bike: JPBike, particles: JPParticle[]): void {
  bike.crashed = true;
  bike.crashTimer = 60;
  bike.vy = -5;
  bike.vx *= 0.3;

  for (let i = 0; i < 15; i++) {
    particles.push({
      x: bike.x + (Math.random() - 0.5) * 40,
      y: bike.y - 10,
      vx: (Math.random() - 0.5) * 8,
      vy: -Math.random() * 6 - 2,
      life: 30 + Math.random() * 20,
      maxLife: 50,
      size: 3 + Math.random() * 5,
      color: ['#ff4444', '#ffaa00', '#ffffff'][Math.floor(Math.random() * 3)],
      type: 'crash',
      rotation: Math.random() * Math.PI * 2,
      rotationSpeed: (Math.random() - 0.5) * 0.3,
    });
  }
}

export function updateJPParticles(particles: JPParticle[], dt: number): void {
  for (let i = particles.length - 1; i >= 0; i--) {
    const p = particles[i];
    p.x += p.vx * dt;
    p.y += p.vy * dt;
    if (p.type === 'dust') {
      p.vy += 0.1 * dt;
      p.vx *= 0.97;
      p.size *= 1.02;
    } else if (p.type === 'spark') {
      p.vy += 0.15 * dt;
    } else if (p.type === 'crash') {
      p.vy += 0.2 * dt;
      p.vx *= 0.95;
      p.rotation += p.rotationSpeed * dt;
    } else if (p.type === 'confetti') {
      p.vy += 0.08 * dt;
      p.vx *= 0.99;
      p.rotation += p.rotationSpeed * dt;
    } else if (p.type === 'sakura') {
      p.vy += 0.03 * dt;
      p.rotation += p.rotationSpeed * dt;
    }
    p.life -= dt;
    if (p.life <= 0) particles.splice(i, 1);
  }
}
