import type { Bike } from './types';
import { Terrain } from './terrain';
import type { ControlInput } from './physics';

/**
 * AI controller that looks ahead at the terrain and makes intelligent decisions.
 * The AI anticipates jumps, adjusts speed for landings, and uses boost strategically.
 */
export function getAIInput(
  bike: Bike,
  terrain: Terrain,
  trackLength: number,
  difficulty: number = 0.85
): ControlInput {
  const input: ControlInput = {
    accelerate: false,
    brake: false,
    rotateLeft: false,
    rotateRight: false,
    boost: false,
  };

  // Always accelerate
  input.accelerate = true;

  // Look ahead at terrain
  const lookAhead = 60 + Math.abs(bike.vx) * 10;
  const aheadX = bike.x + lookAhead;
  const currentAngle = terrain.getAngleAt(bike.x);
  const aheadAngle = terrain.getAngleAt(Math.min(aheadX, trackLength));
  const currentY = terrain.getHeightAt(bike.x);
  const aheadY = terrain.getHeightAt(Math.min(aheadX, trackLength));
  const heightDiff = currentY - aheadY; // positive = uphill ahead

  if (bike.onGround) {
    // Steep uphill - sometimes brake to avoid flipping backwards
    if (aheadAngle < -0.5 && bike.vx > 8) {
      input.accelerate = Math.random() < difficulty;
    }

    // Big drop ahead - use boost if available
    if (heightDiff > 40 && bike.boost > 0.3 && Math.random() < difficulty) {
      input.boost = true;
    }

    // Approaching a ramp (steep uphill then downhill) - keep accelerating
    if (currentAngle < -0.3 && aheadAngle > 0.1) {
      input.accelerate = true;
      if (bike.boost > 0.5) input.boost = Math.random() < difficulty * 0.7;
    }
  } else {
    // In the air - auto-rotate to land safely
    const groundY = terrain.getHeightAt(bike.x + bike.vx * 10);
    const willLandSoon = bike.y + bike.vy * 10 > groundY - 20;

    // Calculate desired tilt for landing (match terrain angle)
    const landingX = bike.x + bike.vx * 5;
    const landingAngle = terrain.getAngleAt(Math.min(landingX, trackLength));
    let desiredTilt = landingAngle;

    // Normalize current tilt
    let currentTilt = bike.tilt;
    while (currentTilt > Math.PI) currentTilt -= Math.PI * 2;
    while (currentTilt < -Math.PI) currentTilt += Math.PI * 2;
    while (desiredTilt > Math.PI) desiredTilt -= Math.PI * 2;
    while (desiredTilt < -Math.PI) desiredTilt += Math.PI * 2;

    const tiltDiff = desiredTilt - currentTilt;

    if (Math.abs(tiltDiff) > 0.08) {
      if (tiltDiff > 0) {
        input.rotateRight = true;
      } else {
        input.rotateLeft = true;
      }
    }

    // Attempt tricks if high enough and confident
    if (!willLandSoon && Math.random() < difficulty * 0.3 && bike.vy < -2) {
      // Sometimes do a flip for style
      if (Math.random() < 0.5) {
        input.rotateRight = true;
      } else {
        input.rotateLeft = true;
      }
    }
  }

  // Use boost on flat/downhill sections
  if (bike.onGround && bike.boost > 0.6 && Math.abs(currentAngle) < 0.2 && bike.vx < MAX_SPEED_REF) {
    input.boost = Math.random() < difficulty * 0.5;
  }

  return input;
}

const MAX_SPEED_REF = 14;
