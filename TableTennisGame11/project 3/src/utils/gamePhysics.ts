import { Ball, Paddle, Particle, Difficulty, GameScore } from '../types/game';

const CANVAS_WIDTH = 800;
const CANVAS_HEIGHT = 600;
const PADDLE_WIDTH = 12;
const PADDLE_HEIGHT = 100;
const BALL_RADIUS = 6;
const BASE_BALL_SPEED = 4;

export const createBall = (): Ball => ({
  x: CANVAS_WIDTH / 2,
  y: CANVAS_HEIGHT / 2,
  radius: BALL_RADIUS,
  velocityX: BASE_BALL_SPEED * (Math.random() > 0.5 ? 1 : -1),
  velocityY: (Math.random() - 0.5) * BASE_BALL_SPEED * 2,
  speed: BASE_BALL_SPEED,
});

export const createPlayerPaddle = (): Paddle => ({
  x: 10,
  y: CANVAS_HEIGHT / 2 - PADDLE_HEIGHT / 2,
  width: PADDLE_WIDTH,
  height: PADDLE_HEIGHT,
  velocity: 0,
  speed: 6,
});

export const createComputerPaddle = (): Paddle => ({
  x: CANVAS_WIDTH - PADDLE_WIDTH - 10,
  y: CANVAS_HEIGHT / 2 - PADDLE_HEIGHT / 2,
  width: PADDLE_WIDTH,
  height: PADDLE_HEIGHT,
  velocity: 0,
  speed: 3.5,
});

export const updateBallPhysics = (
  ball: Ball,
  playerPaddle: Paddle,
  computerPaddle: Paddle,
  difficulty: Difficulty,
  onCollision?: (type: 'paddle' | 'wall') => void,
  onScore?: (player: 'player' | 'computer') => void
): { newBall: Ball; hitPaddle: boolean } => {
  let newBall = { ...ball };
  let hitPaddle = false;

  newBall.x += newBall.velocityX;
  newBall.y += newBall.velocityY;

  // Top and bottom wall collision
  if (newBall.y - newBall.radius < 0 || newBall.y + newBall.radius > CANVAS_HEIGHT) {
    newBall.velocityY *= -1;
    newBall.y = Math.max(newBall.radius, Math.min(CANVAS_HEIGHT - newBall.radius, newBall.y));
    onCollision?.('wall');
  }

  // Player paddle collision
  if (
    newBall.x - newBall.radius < playerPaddle.x + playerPaddle.width &&
    newBall.y > playerPaddle.y &&
    newBall.y < playerPaddle.y + playerPaddle.height &&
    newBall.velocityX < 0
  ) {
    hitPaddle = true;
    const collidePoint = newBall.y - (playerPaddle.y + playerPaddle.height / 2);
    const collideNormalized = collidePoint / (playerPaddle.height / 2);
    const bounceAngle = collideNormalized * (Math.PI / 3);

    newBall.velocityX = Math.cos(bounceAngle) * newBall.speed * 1.05;
    newBall.velocityY = Math.sin(bounceAngle) * newBall.speed * 1.05;
    newBall.x = playerPaddle.x + playerPaddle.width + newBall.radius;
    newBall.speed = Math.min(newBall.speed + 0.15, 7);
    onCollision?.('paddle');
  }

  // Computer paddle collision
  if (
    newBall.x + newBall.radius > computerPaddle.x &&
    newBall.y > computerPaddle.y &&
    newBall.y < computerPaddle.y + computerPaddle.height &&
    newBall.velocityX > 0
  ) {
    hitPaddle = true;
    const collidePoint = newBall.y - (computerPaddle.y + computerPaddle.height / 2);
    const collideNormalized = collidePoint / (computerPaddle.height / 2);
    const bounceAngle = collideNormalized * (Math.PI / 3);

    newBall.velocityX = -Math.cos(bounceAngle) * newBall.speed * 1.05;
    newBall.velocityY = Math.sin(bounceAngle) * newBall.speed * 1.05;
    newBall.x = computerPaddle.x - newBall.radius;
    newBall.speed = Math.min(newBall.speed + 0.15, 7);
    onCollision?.('paddle');
  }

  // Scoring
  if (newBall.x < 0) {
    onScore?.('computer');
    return { newBall: createBall(), hitPaddle: false };
  }

  if (newBall.x > CANVAS_WIDTH) {
    onScore?.('player');
    return { newBall: createBall(), hitPaddle: false };
  }

  return { newBall, hitPaddle };
};

export const updatePaddlePosition = (
  paddle: Paddle,
  targetY: number,
  difficulty: Difficulty
): Paddle => {
  let speed = paddle.speed;

  if (difficulty === 'easy') {
    speed = 2.5;
  } else if (difficulty === 'hard') {
    speed = 5;
  }

  let newY = paddle.y;
  const distance = targetY - (paddle.y + paddle.height / 2);

  if (Math.abs(distance) > speed) {
    newY += Math.sign(distance) * speed;
  } else {
    newY = targetY - paddle.height / 2;
  }

  newY = Math.max(0, Math.min(CANVAS_HEIGHT - paddle.height, newY));

  return {
    ...paddle,
    y: newY,
    velocity: newY - paddle.y,
  };
};

export const getComputerAITarget = (
  ball: Ball,
  computerPaddle: Paddle,
  difficulty: Difficulty
): number => {
  let targetY = computerPaddle.y + computerPaddle.height / 2;

  if (ball.velocityX > 0) {
    targetY = ball.y;

    if (difficulty === 'easy') {
      targetY += (Math.random() - 0.5) * 150;
    } else if (difficulty === 'medium') {
      targetY += (Math.random() - 0.5) * 80;
    }
  } else {
    targetY = CANVAS_HEIGHT / 2 + (Math.random() - 0.5) * 40;
  }

  return Math.max(0, Math.min(CANVAS_HEIGHT, targetY));
};

export const createParticles = (
  x: number,
  y: number,
  type: 'spark' | 'trail' | 'impact' | 'score',
  velocityX?: number,
  velocityY?: number
): Particle[] => {
  const particles: Particle[] = [];
  let count = 0;
  let color = '#fff';
  let speed = 2;

  switch (type) {
    case 'spark':
      count = 12;
      color = '#FFD700';
      speed = 3;
      break;
    case 'trail':
      count = 3;
      color = '#87CEEB';
      speed = 1;
      break;
    case 'impact':
      count = 20;
      color = '#FF6B6B';
      speed = 2.5;
      break;
    case 'score':
      count = 30;
      color = '#4ECDC4';
      speed = 3;
      break;
  }

  for (let i = 0; i < count; i++) {
    const angle = (i / count) * Math.PI * 2;
    const vel = speed + Math.random() * 1;

    particles.push({
      x,
      y,
      velocityX: Math.cos(angle) * vel + (velocityX || 0) * 0.3,
      velocityY: Math.sin(angle) * vel + (velocityY || 0) * 0.3,
      life: 1,
      maxLife: type === 'trail' ? 0.4 : type === 'score' ? 0.8 : 0.6,
      size: type === 'trail' ? 2 : type === 'score' ? 3 : 4,
      color,
      type,
    });
  }

  return particles;
};

export const updateParticles = (particles: Particle[]): Particle[] => {
  return particles
    .map((p) => ({
      ...p,
      x: p.x + p.velocityX,
      y: p.y + p.velocityY,
      velocityX: p.velocityX * 0.98,
      velocityY: p.velocityY * 0.98 + 0.05,
      life: p.life - 1 / p.maxLife / 60,
    }))
    .filter((p) => p.life > 0);
};

export const checkGameOver = (score: GameScore): boolean => {
  return score.player >= 11 || score.computer >= 11;
};
