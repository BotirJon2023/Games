import { Player, Ball, Difficulty } from '../types/game';
import { PLAYER_JUMP_FORCE, PLAYER_MOVE_SPEED, GROUND_Y, NET_X } from './physics';

interface AIConfig {
  reactionTime: number;
  accuracy: number;
  jumpTiming: number;
  aggressiveness: number;
}

const AI_CONFIGS: Record<Difficulty, AIConfig> = {
  easy: {
    reactionTime: 15,
    accuracy: 0.5,
    jumpTiming: 0.4,
    aggressiveness: 0.3,
  },
  medium: {
    reactionTime: 10,
    accuracy: 0.7,
    jumpTiming: 0.6,
    aggressiveness: 0.5,
  },
  hard: {
    reactionTime: 5,
    accuracy: 0.85,
    jumpTiming: 0.8,
    aggressiveness: 0.7,
  },
  expert: {
    reactionTime: 2,
    accuracy: 0.95,
    jumpTiming: 0.95,
    aggressiveness: 0.9,
  },
};

export function updateAI(player: Player, ball: Ball, difficulty: Difficulty): void {
  const config = AI_CONFIGS[difficulty];

  if (ball.position.x > NET_X && ball.velocity.x > 0) {
    const targetX = predictBallLanding(ball, config.accuracy);
    const playerCenterX = player.position.x + player.width / 2;
    const distance = targetX - playerCenterX;

    if (Math.abs(distance) > config.reactionTime) {
      if (distance > 0) {
        player.velocity.x = PLAYER_MOVE_SPEED * config.aggressiveness;
        player.direction = 'right';
      } else {
        player.velocity.x = -PLAYER_MOVE_SPEED * config.aggressiveness;
        player.direction = 'left';
      }
    } else {
      player.velocity.x = 0;
      player.direction = 'idle';
    }

    const shouldJump = shouldAIJump(player, ball, config);
    if (shouldJump && !player.isJumping) {
      player.velocity.y = PLAYER_JUMP_FORCE;
      player.isJumping = true;
    }
  } else {
    const homeX = NET_X + 150;
    const playerCenterX = player.position.x + player.width / 2;
    const distance = homeX - playerCenterX;

    if (Math.abs(distance) > 20) {
      if (distance > 0) {
        player.velocity.x = PLAYER_MOVE_SPEED * 0.5;
        player.direction = 'right';
      } else {
        player.velocity.x = -PLAYER_MOVE_SPEED * 0.5;
        player.direction = 'left';
      }
    } else {
      player.velocity.x = 0;
      player.direction = 'idle';
    }
  }

  player.position.x += player.velocity.x;

  if (player.position.x < NET_X + 10) {
    player.position.x = NET_X + 10;
  }
  if (player.position.x + player.width > 800) {
    player.position.x = 800 - player.width;
  }
}

function predictBallLanding(ball: Ball, accuracy: number): number {
  const tempBall = {
    position: { ...ball.position },
    velocity: { ...ball.velocity },
  };

  for (let i = 0; i < 100; i++) {
    tempBall.velocity.y += 0.6;
    tempBall.position.x += tempBall.velocity.x;
    tempBall.position.y += tempBall.velocity.y;

    if (tempBall.position.y >= GROUND_Y - 20) {
      const randomOffset = (Math.random() - 0.5) * 100 * (1 - accuracy);
      return tempBall.position.x + randomOffset;
    }
  }

  return ball.position.x;
}

function shouldAIJump(player: Player, ball: Ball, config: AIConfig): boolean {
  if (player.isJumping) return false;

  const distanceX = Math.abs(ball.position.x - (player.position.x + player.width / 2));
  const distanceY = ball.position.y - player.position.y;

  if (distanceX < 60 && distanceY < 100 && distanceY > -50) {
    return Math.random() < config.jumpTiming;
  }

  return false;
}
