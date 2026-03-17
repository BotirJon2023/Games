import { Player, Ball } from '../types/game';
import { PLAYER_SPEED, PLAYER_JUMP_FORCE, GROUND_HEIGHT, CANVAS_WIDTH } from '../constants/game';

export class AIController {
  private reactionDelay = 0;
  private targetX = CANVAS_WIDTH * 0.75;

  updateAI(player: Player, ball: Ball) {
    if (this.reactionDelay > 0) {
      this.reactionDelay--;
      return;
    }

    if (ball.isServing) {
      const homeX = CANVAS_WIDTH * 0.75;
      if (Math.abs(player.position.x - homeX) > 5) {
        if (player.position.x < homeX) {
          player.velocity.x = PLAYER_SPEED * 0.7;
        } else {
          player.velocity.x = -PLAYER_SPEED * 0.7;
        }
      }
      return;
    }

    const ballIsComingToAI = ball.position.x > CANVAS_WIDTH / 2 && ball.velocity.x > 0;
    const ballIsFalling = ball.velocity.y > 0;

    if (ballIsComingToAI || ball.position.x > CANVAS_WIDTH / 2) {
      this.targetX = this.predictBallLanding(ball);

      const distanceToTarget = this.targetX - player.position.x;

      if (Math.abs(distanceToTarget) > 15) {
        if (distanceToTarget > 0) {
          player.velocity.x = PLAYER_SPEED * 0.8;
        } else {
          player.velocity.x = -PLAYER_SPEED * 0.8;
        }
      }

      const shouldJump =
        ballIsFalling &&
        Math.abs(distanceToTarget) < 60 &&
        ball.position.y < GROUND_HEIGHT - 50 &&
        player.position.y >= GROUND_HEIGHT;

      if (shouldJump) {
        player.velocity.y = -PLAYER_JUMP_FORCE * 0.9;
        this.reactionDelay = 30;
      }
    } else {
      const homeX = CANVAS_WIDTH * 0.75;
      if (Math.abs(player.position.x - homeX) > 5) {
        if (player.position.x < homeX) {
          player.velocity.x = PLAYER_SPEED * 0.5;
        } else {
          player.velocity.x = -PLAYER_SPEED * 0.5;
        }
      }
    }
  }

  private predictBallLanding(ball: Ball): number {
    let tempY = ball.position.y;
    let tempVelY = ball.velocity.y;
    let tempX = ball.position.x;
    let tempVelX = ball.velocity.x;

    for (let i = 0; i < 100; i++) {
      tempVelY += 0.5;
      tempY += tempVelY;
      tempX += tempVelX;

      if (tempY >= GROUND_HEIGHT - 50) {
        return Math.max(CANVAS_WIDTH / 2 + 20, Math.min(CANVAS_WIDTH - 40, tempX));
      }

      if (tempX < 0 || tempX > CANVAS_WIDTH) {
        break;
      }
    }

    return CANVAS_WIDTH * 0.75;
  }
}
