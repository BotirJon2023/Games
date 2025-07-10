import { Obstacle, PowerUp, Particle } from '../types/game';

export const generateObstacle = (
  screenWidth: number, 
  groundLevel: number, 
  level: number
): Obstacle => {
  const types: Obstacle['type'][] = ['rock', 'tree', 'ramp', 'pit', 'spike'];
  const type = types[Math.floor(Math.random() * types.length)];
  
  let width: number;
  let height: number;
  
  switch (type) {
    case 'rock':
      width = 30 + Math.random() * 20;
      height = 20 + Math.random() * 30;
      break;
    case 'tree':
      width = 25 + Math.random() * 15;
      height = 60 + Math.random() * 40;
      break;
    case 'ramp':
      width = 60 + Math.random() * 40;
      height = 30 + Math.random() * 20;
      break;
    case 'pit':
      width = 40 + Math.random() * 30;
      height = 40 + Math.random() * 20;
      break;
    case 'spike':
      width = 20 + Math.random() * 15;
      height = 25 + Math.random() * 20;
      break;
    default:
      width = 30;
      height = 30;
  }
  
  return {
    id: Math.random().toString(36).substr(2, 9),
    position: {
      x: screenWidth + Math.random() * 200,
      y: groundLevel - height
    },
    width,
    height,
    type,
    speed: 5 + level * 0.5
  };
};

export const generatePowerUp = (
  screenWidth: number, 
  groundLevel: number
): PowerUp => {
  const types: PowerUp['type'][] = ['speed', 'shield', 'health', 'jump'];
  const type = types[Math.floor(Math.random() * types.length)];
  
  return {
    id: Math.random().toString(36).substr(2, 9),
    position: {
      x: screenWidth + Math.random() * 300,
      y: groundLevel - 50 - Math.random() * 100
    },
    type,
    collected: false
  };
};

export const generateParticles = (
  position: { x: number; y: number },
  count: number,
  color: string,
  velocityRange: { x: number; y: number }
): Particle[] => {
  const particles: Particle[] = [];
  
  for (let i = 0; i < count; i++) {
    particles.push({
      id: Math.random().toString(36).substr(2, 9),
      position: {
        x: position.x + (Math.random() - 0.5) * 20,
        y: position.y + (Math.random() - 0.5) * 20
      },
      velocity: {
        x: (Math.random() - 0.5) * velocityRange.x * 2,
        y: (Math.random() - 0.5) * velocityRange.y * 2
      },
      life: 1000 + Math.random() * 500,
      maxLife: 1000 + Math.random() * 500,
      color: color,
      size: 2 + Math.random() * 4
    });
  }
  
  return particles;
};

export const checkCollision = (
  rect1: { x: number; y: number; width: number; height: number },
  rect2: { x: number; y: number; width: number; height: number }
): boolean => {
  return (
    rect1.x < rect2.x + rect2.width &&
    rect1.x + rect1.width > rect2.x &&
    rect1.y < rect2.y + rect2.height &&
    rect1.y + rect1.height > rect2.y
  );
};

export const lerp = (start: number, end: number, factor: number): number => {
  return start + (end - start) * factor;
};

export const clamp = (value: number, min: number, max: number): number => {
  return Math.min(Math.max(value, min), max);
};

export const getRandomInRange = (min: number, max: number): number => {
  return Math.random() * (max - min) + min;
};

export const calculateScore = (
  distance: number,
  time: number,
  powerUpsCollected: number,
  obstaclesAvoided: number
): number => {
  const distancePoints = Math.floor(distance * 10);
  const timeBonus = Math.floor(time / 1000) * 5;
  const powerUpBonus = powerUpsCollected * 100;
  const avoidanceBonus = obstaclesAvoided * 50;
  
  return distancePoints + timeBonus + powerUpBonus + avoidanceBonus;
};

export const getDifficultyMultiplier = (level: number): number => {
  return 1 + (level - 1) * 0.2;
};

export const getRandomObstaclePattern = (level: number): string => {
  const patterns = [
    'single',
    'double',
    'triple',
    'mixed',
    'cluster'
  ];
  
  const availablePatterns = patterns.slice(0, Math.min(level + 1, patterns.length));
  return availablePatterns[Math.floor(Math.random() * availablePatterns.length)];
};