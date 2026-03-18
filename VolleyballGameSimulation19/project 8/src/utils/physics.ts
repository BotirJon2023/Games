import { Vector2D, Player, Ball } from '../types/game';

export const GRAVITY = 0.6;
export const COURT_WIDTH = 800;
export const COURT_HEIGHT = 400;
export const GROUND_Y = 350;
export const NET_X = COURT_WIDTH / 2;
export const NET_HEIGHT = 150;
export const PLAYER_JUMP_FORCE = -15;
export const PLAYER_MOVE_SPEED = 5;
export const BALL_BOUNCE_DAMPENING = 0.7;

export function checkCollision(rect1: { x: number; y: number; width: number; height: number },
                                rect2: { x: number; y: number; width: number; height: number }): boolean {
  return rect1.x < rect2.x + rect2.width &&
         rect1.x + rect1.width > rect2.x &&
         rect1.y < rect2.y + rect2.height &&
         rect1.y + rect1.height > rect2.y;
}

export function checkBallPlayerCollision(ball: Ball, player: Player): boolean {
  const closestX = Math.max(player.position.x, Math.min(ball.position.x, player.position.x + player.width));
  const closestY = Math.max(player.position.y, Math.min(ball.position.y, player.position.y + player.height));

  const distanceX = ball.position.x - closestX;
  const distanceY = ball.position.y - closestY;

  return (distanceX * distanceX + distanceY * distanceY) < (ball.radius * ball.radius);
}

export function reflectBallFromPlayer(ball: Ball, player: Player): void {
  const ballCenterX = ball.position.x;
  const playerCenterX = player.position.x + player.width / 2;

  const hitPosition = (ballCenterX - playerCenterX) / (player.width / 2);

  const baseAngle = -Math.PI / 2;
  const maxAngleDeviation = Math.PI / 3;
  const angle = baseAngle + hitPosition * maxAngleDeviation;

  const speed = Math.sqrt(ball.velocity.x ** 2 + ball.velocity.y ** 2);
  const newSpeed = Math.max(speed * 1.1, 10);

  ball.velocity.x = Math.sin(angle) * newSpeed;
  ball.velocity.y = Math.cos(angle) * newSpeed;

  if (player.velocity.x !== 0) {
    ball.velocity.x += player.velocity.x * 0.3;
  }

  ball.spinning = hitPosition * 10;
}

export function updateBallPhysics(ball: Ball): void {
  ball.velocity.y += GRAVITY;
  ball.position.x += ball.velocity.x;
  ball.position.y += ball.velocity.y;

  if (ball.position.y + ball.radius > GROUND_Y) {
    ball.position.y = GROUND_Y - ball.radius;
    ball.velocity.y *= -BALL_BOUNCE_DAMPENING;
    ball.velocity.x *= 0.9;
    ball.spinning *= 0.8;
  }

  if (ball.position.x - ball.radius < 0 || ball.position.x + ball.radius > COURT_WIDTH) {
    ball.velocity.x *= -0.9;
    ball.position.x = ball.position.x < COURT_WIDTH / 2 ? ball.radius : COURT_WIDTH - ball.radius;
  }

  ball.spinning *= 0.98;
}

export function updatePlayerPhysics(player: Player): void {
  player.velocity.y += GRAVITY;
  player.position.y += player.velocity.y;

  if (player.position.y + player.height >= GROUND_Y) {
    player.position.y = GROUND_Y - player.height;
    player.velocity.y = 0;
    player.isJumping = false;
  }
}
