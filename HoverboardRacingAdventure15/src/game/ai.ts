import type { InputState } from './types';
import type { TrackData } from './track';
import { findClosestTrackPoint } from './track';
import { Hoverboard } from './hoverboard';
import { AI_PARAMS, PHYSICS } from './constants';
import { angleOf, normalize, sub, clamp } from './math';

/**
 * AI controller that steers toward a look-ahead point on the track,
 * uses boost strategically, and avoids obstacles.
 */
export function computeAIInput(
  ai: Hoverboard,
  track: TrackData,
  elapsed: number,
): InputState {
  const input: InputState = {
    accelerate: true,
    brake: false,
    left: false,
    right: false,
    boost: false,
  };

  if (ai.finished) {
    input.accelerate = false;
    return input;
  }

  // Find current track point
  const currentIdx = findClosestTrackPoint(track, ai.pos);
  const lookAheadIdx = (currentIdx + AI_PARAMS.lookAheadDistance) % track.points.length;
  const targetPoint = track.points[lookAheadIdx].pos;

  // Calculate desired heading
  const toTarget = sub(targetPoint, ai.pos);
  const targetAngle = angleOf(toTarget);
  let angleDiff = targetAngle - ai.angle;

  // Normalize angle difference to [-PI, PI]
  while (angleDiff > Math.PI) angleDiff -= Math.PI * 2;
  while (angleDiff < -Math.PI) angleDiff += Math.PI * 2;

  // Steering
  if (angleDiff > AI_PARAMS.steerSmoothing) {
    input.right = true;
  } else if (angleDiff < -AI_PARAMS.steerSmoothing) {
    input.left = true;
  }

  // Brake on sharp turns at high speed
  if (Math.abs(angleDiff) > 1.2 && ai.speed > PHYSICS.maxSpeed * 0.7) {
    input.brake = true;
    input.accelerate = false;
  }

  // Boost on straightaways
  if (Math.abs(angleDiff) < 0.3 && ai.energy > 40 && Math.random() < AI_PARAMS.boostAggression * 0.02) {
    input.boost = true;
  }
  // Sustained boost on long straights
  if (Math.abs(angleDiff) < 0.2 && ai.energy > 50) {
    input.boost = true;
  }

  // Apply slight speed variation to make AI feel more natural
  if (ai.speed > PHYSICS.maxSpeed * AI_PARAMS.speedVariation && !input.boost) {
    input.accelerate = Math.random() > 0.1;
  }

  return input;
}
