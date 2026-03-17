import { Player, Ball, Vector2D } from '../types/game';
import {
  GROUND_HEIGHT,
  GRAVITY,
  BALL_BOUNCE,
  BALL_FRICTION,
  CANVAS_WIDTH,
  NET_X,
  NET_WIDTH,
  NET_HEIGHT,
  BALL_RADIUS,
} from '../constants/game';

export class PhysicsEngine {
  updatePlayerPhysics(player: Player) {
    player.velocity.y += GRAVITY;
    player.position.y += player.velocity.y;

    if (player.position.y >= GROUND_HEIGHT) {
      player.position.y = GROUND_HEIGHT;
      player.velocity.y = 0;
    }

    player.position.x += player.velocity.x;

    if (player.position.x < player.width / 2) {
      player.position.x = player.width / 2;
    }
    if (player.position.x > CANVAS_WIDTH - player.width / 2) {
      player.position.x = CANVAS_WIDTH - player.width / 2;
    }

    player.velocity.x *= 0.85;
  }

  updateBallPhysics(ball: Ball): { hitGround: boolean; side: 'left' | 'right' | null } {
    let hitGround = false;
    let side: 'left' | 'right' | null = null;

    if (ball.isServing) {
      return { hitGround, side };
    }

    ball.velocity.y += GRAVITY;
    ball.position.x += ball.velocity.x;
    ball.position.y += ball.velocity.y;

    if (ball.position.y + ball.radius >= GROUND_HEIGHT) {
      ball.position.y = GROUND_HEIGHT - ball.radius;
      ball.velocity.y *= -BALL_BOUNCE;
      ball.velocity.x *= BALL_FRICTION;

      if (Math.abs(ball.velocity.y) < 1) {
        ball.velocity.y = 0;
        hitGround = true;
        side = ball.position.x < CANVAS_WIDTH / 2 ? 'left' : 'right';
      }
    }

    if (ball.position.x - ball.radius < 0) {
      ball.position.x = ball.radius;
      ball.velocity.x *= -BALL_BOUNCE;
    }
    if (ball.position.x + ball.radius > CANVAS_WIDTH) {
      ball.position.x = CANVAS_WIDTH - ball.radius;
      ball.velocity.x *= -BALL_BOUNCE;
    }

    if (ball.position.y - ball.radius < 0) {
      ball.position.y = ball.radius;
      ball.velocity.y *= -BALL_BOUNCE;
    }

    this.checkNetCollision(ball);

    return { hitGround, side };
  }

  checkBallPlayerCollision(ball: Ball, player: Player): boolean {
    if (ball.isServing) return false;

    const ballLeft = ball.position.x - ball.radius;
    const ballRight = ball.position.x + ball.radius;
    const ballTop = ball.position.y - ball.radius;
    const ballBottom = ball.position.y + ball.radius;

    const playerLeft = player.position.x - player.width / 2;
    const playerRight = player.position.x + player.width / 2;
    const playerTop = player.position.y - player.height;
    const playerBottom = player.position.y;

    if (
      ballRight > playerLeft &&
      ballLeft < playerRight &&
      ballBottom > playerTop &&
      ballTop < playerBottom
    ) {
      const overlapX = Math.min(ballRight - playerLeft, playerRight - ballLeft);
      const overlapY = Math.min(ballBottom - playerTop, playerBottom - ballTop);

      if (overlapX < overlapY) {
        if (ball.position.x < player.position.x) {
          ball.position.x = playerLeft - ball.radius;
          ball.velocity.x = -Math.abs(ball.velocity.x) * 1.2;
        } else {
          ball.position.x = playerRight + ball.radius;
          ball.velocity.x = Math.abs(ball.velocity.x) * 1.2;
        }
      } else {
        if (ball.position.y < player.position.y - player.height / 2) {
          ball.position.y = playerTop - ball.radius;
          ball.velocity.y = -Math.abs(ball.velocity.y) * 1.1;
          ball.velocity.x += player.velocity.x * 0.3;
        } else {
          ball.position.y = playerBottom + ball.radius;
          ball.velocity.y = Math.abs(ball.velocity.y);
        }
      }

      return true;
    }

    return false;
  }

  private checkNetCollision(ball: Ball) {
    const netLeft = NET_X - NET_WIDTH / 2;
    const netRight = NET_X + NET_WIDTH / 2;
    const netTop = GROUND_HEIGHT - NET_HEIGHT;

    if (
      ball.position.x + ball.radius > netLeft &&
      ball.position.x - ball.radius < netRight &&
      ball.position.y + ball.radius > netTop &&
      ball.position.y - ball.radius < GROUND_HEIGHT
    ) {
      if (Math.abs(ball.position.x - NET_X) < ball.radius) {
        if (ball.position.x < NET_X) {
          ball.position.x = netLeft - ball.radius;
        } else {
          ball.position.x = netRight + ball.radius;
        }
        ball.velocity.x *= -BALL_BOUNCE;
      }

      if (ball.position.y > netTop && ball.position.y < GROUND_HEIGHT) {
        ball.position.y = netTop - ball.radius;
        ball.velocity.y *= -BALL_BOUNCE;
      }
    }
  }

  serveBall(ball: Ball, playerX: number, direction: number) {
    ball.position.x = playerX;
    ball.position.y = GROUND_HEIGHT - 100;
    ball.velocity.x = direction * 8;
    ball.velocity.y = -10;
    ball.isServing = false;
  }
}
