import { Fencer, Position, Particle, CombatEvent } from '../types/game';

export const GAME_CONFIG = {
  canvasWidth: 800,
  canvasHeight: 400,
  roundDuration: 180, // 3 minutes in seconds
  winningScore: 5,
  attackRange: 60,
  parryWindow: 500, // milliseconds
  stunDuration: 1000,
  particleLifetime: 1000
};

export function checkCollision(attacker: Fencer, defender: Fencer): boolean {
  const distance = Math.abs(attacker.position.x - defender.position.x);
  return distance <= attacker.style.reach;
}

export function calculateDamage(attacker: Fencer, defender: Fencer): number {
  if (defender.isParrying) {
    return 0; // Successful parry
  }
  return attacker.style.damage;
}

export function updateFencerPosition(fencer: Fencer, direction: 'left' | 'right' | 'none'): void {
  const speed = 2;
  const leftBoundary = 50;
  const rightBoundary = GAME_CONFIG.canvasWidth - 50;

  switch (direction) {
    case 'left':
      fencer.position.x = Math.max(leftBoundary, fencer.position.x - speed);
      fencer.facing = 'left';
      break;
    case 'right':
      fencer.position.x = Math.min(rightBoundary, fencer.position.x + speed);
      fencer.facing = 'right';
      break;
  }
}

export function createHitParticles(position: Position, color: string): Particle[] {
  const particles: Particle[] = [];
  
  for (let i = 0; i < 8; i++) {
    particles.push({
      id: `particle-${Date.now()}-${i}`,
      position: { ...position },
      velocity: {
        x: (Math.random() - 0.5) * 6,
        y: (Math.random() - 0.5) * 6
      },
      life: GAME_CONFIG.particleLifetime,
      maxLife: GAME_CONFIG.particleLifetime,
      color,
      size: Math.random() * 4 + 2
    });
  }
  
  return particles;
}

export function updateParticles(particles: Particle[], deltaTime: number): Particle[] {
  return particles.map(particle => ({
    ...particle,
    position: {
      x: particle.position.x + particle.velocity.x * deltaTime / 16,
      y: particle.position.y + particle.velocity.y * deltaTime / 16
    },
    velocity: {
      x: particle.velocity.x * 0.98,
      y: particle.velocity.y * 0.98
    },
    life: particle.life - deltaTime
  })).filter(particle => particle.life > 0);
}

export function createCombatEvent(
  attacker: Fencer,
  defender: Fencer,
  action: 'attack' | 'parry' | 'hit' | 'miss',
  damage?: number
): CombatEvent {
  return {
    timestamp: Date.now(),
    attacker: attacker.name,
    defender: defender.name,
    action,
    damage
  };
}

export function generateAIAction(fencer: Fencer, opponent: Fencer): string {
  const distance = Math.abs(fencer.position.x - opponent.position.x);
  const difficulty = fencer.aiDifficulty || 'medium';
  
  let attackChance = 0.3;
  let parryChance = 0.2;
  let moveChance = 0.5;

  switch (difficulty) {
    case 'easy':
      attackChance = 0.2;
      parryChance = 0.1;
      break;
    case 'hard':
      attackChance = 0.4;
      parryChance = 0.3;
      break;
  }

  if (opponent.isAttacking && distance < GAME_CONFIG.attackRange) {
    return Math.random() < parryChance ? 'parry' : 'none';
  }

  if (distance < fencer.style.reach && !opponent.isParrying) {
    return Math.random() < attackChance ? 'attack' : 'none';
  }

  if (distance > 100) {
    return fencer.position.x < opponent.position.x ? 'right' : 'left';
  }

  return Math.random() < moveChance ? (Math.random() < 0.5 ? 'left' : 'right') : 'none';
}