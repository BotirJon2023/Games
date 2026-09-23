import type { JPBike } from './types';
import { JPTerrain } from './terrain';
import type { JPControlInput } from './physics';

export function getJPAIInput(
  bike: JPBike,
  terrain: JPTerrain,
  trackLength: number,
  difficulty: number = 0.88
): JPControlInput {
  const input: JPControlInput = {
    accelerate: false, brake: false,
    rotateLeft: false, rotateRight: false, boost: false,
  };

  input.accelerate = true;

  const lookAhead = 60 + Math.abs(bike.vx) * 10;
  const aheadX = bike.x + lookAhead;
  const currentAngle = terrain.getAngleAt(bike.x);
  const aheadAngle = terrain.getAngleAt(Math.min(aheadX, trackLength));
  const currentY = terrain.getHeightAt(bike.x);
  const aheadY = terrain.getHeightAt(Math.min(aheadX, trackLength));
  const heightDiff = currentY - aheadY;

  if (bike.onGround) {
    if (aheadAngle < -0.5 && bike.vx > 8) {
      input.accelerate = Math.random() < difficulty;
    }
    if (heightDiff > 40 && bike.boost > 0.3 && Math.random() < difficulty) {
      input.boost = true;
    }
    if (currentAngle < -0.3 && aheadAngle > 0.1) {
      input.accelerate = true;
      if (bike.boost > 0.5) input.boost = Math.random() < difficulty * 0.7;
    }
    if (bike.boost > 0.6 && Math.abs(currentAngle) < 0.2 && bike.vx < 14) {
      input.boost = Math.random() < difficulty * 0.5;
    }
  } else {
    const groundY = terrain.getHeightAt(bike.x + bike.vx * 10);
    const willLandSoon = bike.y + bike.vy * 10 > groundY - 20;
    const landingX = bike.x + bike.vx * 5;
    const landingAngle = terrain.getAngleAt(Math.min(landingX, trackLength));
    let desiredTilt = landingAngle;
    let currentTilt = bike.tilt;
    while (currentTilt > Math.PI) currentTilt -= Math.PI * 2;
    while (currentTilt < -Math.PI) currentTilt += Math.PI * 2;
    while (desiredTilt > Math.PI) desiredTilt -= Math.PI * 2;
    while (desiredTilt < -Math.PI) desiredTilt += Math.PI * 2;
    const tiltDiff = desiredTilt - currentTilt;
    if (Math.abs(tiltDiff) > 0.08) {
      if (tiltDiff > 0) input.rotateRight = true;
      else input.rotateLeft = true;
    }
    if (!willLandSoon && Math.random() < difficulty * 0.3 && bike.vy < -2) {
      if (Math.random() < 0.5) input.rotateRight = true;
      else input.rotateLeft = true;
    }
  }

  return input;
}
