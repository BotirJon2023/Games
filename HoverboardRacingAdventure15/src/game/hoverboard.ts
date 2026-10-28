import type { InputState, Vec2 } from './types';
import type { TrackData } from './track';
import {
  isOnTrack,
  checkBoostPadCollision,
  checkObstacleCollision,
  getTrackProgress,
} from './track';
import { PHYSICS } from './constants';
import { add, angleOf, clamp, length, normalize, scale, sub, rotate } from './math';

export interface Particle {
  pos: Vec2;
  vel: Vec2;
  life: number;
  maxLife: number;
  size: number;
  color: string;
}

export class Hoverboard {
  pos: Vec2;
  vel: Vec2 = { x: 0, y: 0 };
  angle: number;
  speed: number = 0;
  energy: number = PHYSICS.energyMax;
  isBoosting: boolean = false;
  boostFlash: number = 0;

  // Race state
  laps: number = 0;
  nextCheckpoint: number = 1;
  finished: boolean = false;
  finishTime: number = 0;
  currentArcLength: number = 0;
  lastArcLength: number = 0;

  // Visual
  trail: Vec2[] = [];
  trailTimer: number = 0;
  particles: Particle[] = [];
  hitFlash: number = 0;
  wobble: number = 0;

  // Identity
  color: string;
  glowColor: string;
  trailColor: string;
  name: string;
  isAI: boolean;

  constructor(
    startPos: Vec2,
    startAngle: number,
    color: string,
    glowColor: string,
    trailColor: string,
    name: string,
    isAI: boolean = false,
  ) {
    this.pos = { ...startPos };
    this.angle = startAngle;
    this.color = color;
    this.glowColor = glowColor;
    this.trailColor = trailColor;
    this.name = name;
    this.isAI = isAI;
  }

  update(
    input: InputState,
    track: TrackData,
    dt: number,
    elapsed: number,
    totalLaps: number,
  ): void {
    if (this.finished) {
      // Coast to a stop
      this.vel = scale(this.vel, PHYSICS.friction);
      this.pos = add(this.pos, this.vel);
      this.speed *= 0.95;
      this.updateParticles(dt);
      return;
    }

    const onTrack = isOnTrack(track, this.pos);

    // Determine boost availability
    this.isBoosting = input.boost && this.energy > PHYSICS.energyMinToBoost && onTrack;

    if (this.isBoosting) {
      this.energy = Math.max(0, this.energy - PHYSICS.boostDrain);
      this.boostFlash = 1;
    } else {
      this.energy = Math.min(PHYSICS.energyMax, this.energy + PHYSICS.energyRegen);
      this.boostFlash = Math.max(0, this.boostFlash - 0.05);
    }

    // Acceleration
    const maxSpeed = this.isBoosting
      ? PHYSICS.boostMaxSpeed
      : onTrack
        ? PHYSICS.maxSpeed
        : PHYSICS.offTrackMaxSpeed;

    if (input.accelerate) {
      const accel = this.isBoosting ? PHYSICS.boostAcceleration : PHYSICS.acceleration;
      const force = { x: Math.cos(this.angle) * accel, y: Math.sin(this.angle) * accel };
      this.vel = add(this.vel, force);
    }

    if (input.brake) {
      const decel = PHYSICS.brakeForce;
      const brakeForce = { x: -Math.cos(this.angle) * decel, y: -Math.sin(this.angle) * decel };
      this.vel = add(this.vel, brakeForce);
    }

    // Steering — more effective at speed
    const speedFactor = clamp(this.speed / 2, 0.3, 1);
    const turnAmount = PHYSICS.turnSpeed * speedFactor * (this.isBoosting ? 0.85 : 1);
    if (input.left) this.angle -= turnAmount;
    if (input.right) this.angle += turnAmount;

    // Apply friction
    const friction = onTrack ? PHYSICS.friction : PHYSICS.offTrackFriction;
    this.vel = scale(this.vel, friction);

    // Clamp speed
    const currentSpeed = length(this.vel);
    if (currentSpeed > maxSpeed) {
      this.vel = scale(normalize(this.vel), maxSpeed);
    }

    // Move
    this.pos = add(this.pos, this.vel);
    this.speed = length(this.vel);

    // Wobble effect at high speed
    this.wobble = Math.sin(elapsed * 30) * (this.speed / PHYSICS.maxSpeed) * 0.03;

    // Boost pad collision
    const pad = checkBoostPadCollision(track, this.pos);
    if (pad && pad.active) {
      this.vel = scale(normalize(this.vel || { x: Math.cos(this.angle), y: Math.sin(this.angle) }), PHYSICS.boostMaxSpeed);
      this.energy = Math.min(PHYSICS.energyMax, this.energy + 30);
      this.boostFlash = 1;
      pad.active = false;
      pad.cooldownTimer = 3;
      // Boost particles
      for (let i = 0; i < 12; i++) {
        this.spawnParticle(pad.pos, '#ffd700', 2 + Math.random() * 2, 0.8);
      }
    }

    // Obstacle collision
    const obs = checkObstacleCollision(track, this.pos, 14);
    if (obs && obs.hitTimer <= 0) {
      const away = normalize(sub(this.pos, obs.pos));
      this.pos = add(obs.pos, scale(away, obs.radius + 15));
      this.vel = scale(away, PHYSICS.collisionBounce * this.speed);
      this.hitFlash = 1;
      obs.hitTimer = 1;
      // Collision particles
      for (let i = 0; i < 10; i++) {
        const angle = Math.random() * Math.PI * 2;
        this.spawnParticle(
          this.pos,
          '#ff4444',
          2 + Math.random() * 2,
          0.5,
          { x: Math.cos(angle) * 2, y: Math.sin(angle) * 2 },
        );
      }
    }

    this.hitFlash = Math.max(0, this.hitFlash - 0.04);

    // Track progress / lap counting
    const progress = getTrackProgress(track, this.pos);
    this.lastArcLength = this.currentArcLength;
    this.currentArcLength = progress.arcLength;

    // Lap detection: when arc length wraps from near-total back to near-0
    if (this.lastArcLength > track.totalLength * 0.75 && this.currentArcLength < track.totalLength * 0.15) {
      this.laps++;
      if (this.laps >= totalLaps) {
        this.finished = true;
        this.finishTime = elapsed;
      }
    }

    // Trail
    this.trailTimer += dt;
    if (this.trailTimer > 0.02) {
      this.trailTimer = 0;
      this.trail.push({ ...this.pos });
      if (this.trail.length > 25) this.trail.shift();
    }

    // Boost particles
    if (this.isBoosting && Math.random() < 0.8) {
      const back = {
        x: this.pos.x - Math.cos(this.angle) * 18,
        y: this.pos.y - Math.sin(this.angle) * 18,
      };
      this.spawnParticle(back, this.glowColor, 3 + Math.random() * 3, 0.4);
    }

    // Speed particles (small dust)
    if (this.speed > 3 && Math.random() < 0.3) {
      const back = {
        x: this.pos.x - Math.cos(this.angle) * 14,
        y: this.pos.y - Math.sin(this.angle) * 14,
      };
      this.spawnParticle(back, 'rgba(255,255,255,0.5)', 1 + Math.random(), 0.3);
    }

    this.updateParticles(dt);
  }

  private spawnParticle(
    pos: Vec2,
    color: string,
    size: number,
    life: number,
    vel?: Vec2,
  ): void {
    this.particles.push({
      pos: { ...pos },
      vel: vel ?? {
        x: (Math.random() - 0.5) * 2,
        y: (Math.random() - 0.5) * 2,
      },
      life,
      maxLife: life,
      size,
      color,
    });
  }

  private updateParticles(dt: number): void {
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.pos = add(p.pos, p.vel);
      p.vel = scale(p.vel, 0.95);
      p.life -= dt;
      if (p.life <= 0) {
        this.particles.splice(i, 1);
      }
    }
  }

  getProgress(track: TrackData): number {
    return (this.laps * track.totalLength + this.currentArcLength) / (track.totalLength);
  }
}
