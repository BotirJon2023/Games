import { Position, Velocity } from '../types/game';

export const calculateBallCollision = (
  ballPos: Position,
  ballVel: Velocity,
  paddleX: number,
  paddleY: number,
  paddleWidth: number,
  paddleHeight: number
): { newVelocity: Velocity; collision: boolean } => {
  const ballRadius = 6; // Half of ball size
  
  // Check if ball is within paddle bounds
  const withinPaddleY = ballPos.y >= paddleY && ballPos.y <= paddleY + paddleHeight;
  const withinPaddleX = ballPos.x + ballRadius >= paddleX && ballPos.x - ballRadius <= paddleX + paddleWidth;
  
  if (withinPaddleX && withinPaddleY) {
    // Calculate hit position relative to paddle center (-1 to 1)
    const relativeIntersectY = (ballPos.y - (paddleY + paddleHeight / 2)) / (paddleHeight / 2);
    
    // Calculate new velocity based on hit position
    const speed = Math.sqrt(ballVel.x * ballVel.x + ballVel.y * ballVel.y);
    const newVelX = ballVel.x > 0 ? -speed : speed; // Reverse X direction
    const newVelY = relativeIntersectY * speed * 0.8; // Y component based on hit position
    
    return {
      newVelocity: { x: newVelX, y: newVelY },
      collision: true
    };
  }
  
  return {
    newVelocity: ballVel,
    collision: false
  };
};

export const normalizeVector = (velocity: Velocity): Velocity => {
  const magnitude = Math.sqrt(velocity.x * velocity.x + velocity.y * velocity.y);
  return {
    x: velocity.x / magnitude,
    y: velocity.y / magnitude
  };
};

export const clamp = (value: number, min: number, max: number): number => {
  return Math.min(Math.max(value, min), max);
};

export const lerp = (start: number, end: number, factor: number): number => {
  return start + (end - start) * factor;
};

export const distance = (pos1: Position, pos2: Position): number => {
  const dx = pos1.x - pos2.x;
  const dy = pos1.y - pos2.y;
  return Math.sqrt(dx * dx + dy * dy);
};