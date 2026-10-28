import type { BoostPad, Obstacle, TrackPoint, Vec2 } from './types';
import {
  BOOST_PAD_POSITIONS,
  OBSTACLE_POSITIONS,
  TRACK_CONTROL_POINTS,
  TRACK_WIDTH,
} from './constants';
import {
  angleOf,
  add,
  distToSegment,
  generateSpline,
  length,
  normalize,
  scale,
  sub,
} from './math';

export interface TrackData {
  centerPoints: Vec2[];
  points: TrackPoint[];
  totalLength: number;
  width: number;
  boostPads: BoostPad[];
  obstacles: Obstacle[];
  startLine: { a: Vec2; b: Vec2; pos: Vec2 };
  /** Number of checkpoint segments for lap validation. */
  checkpointCount: number;
}

export function buildTrack(): TrackData {
  const centerPoints = generateSpline(TRACK_CONTROL_POINTS, 24);

  // Build track points with arc length, tangent, and normal
  const points: TrackPoint[] = [];
  let totalLength = 0;

  for (let i = 0; i < centerPoints.length; i++) {
    const pos = centerPoints[i];
    const next = centerPoints[(i + 1) % centerPoints.length];
    const prev = centerPoints[(i - 1 + centerPoints.length) % centerPoints.length];

    const tangent = normalize({ x: next.x - prev.x, y: next.y - prev.y });
    const normal = { x: -tangent.y, y: tangent.x };

    points.push({ pos, arcLength: totalLength, tangent, normal });

    const segLen = length(sub(next, pos));
    totalLength += segLen;
  }

  // Start line — at the first track point, spanning the track width
  const start = points[0];
  const halfWidth = TRACK_WIDTH / 2;
  const startLine = {
    a: add(start.pos, scale(start.normal, halfWidth)),
    b: add(start.pos, scale(start.normal, -halfWidth)),
    pos: start.pos,
  };

  // Boost pads
  const boostPads: BoostPad[] = BOOST_PAD_POSITIONS.map((bp) => {
    // Find the nearest track point to determine orientation
    let nearestIdx = 0;
    let nearestDist = Infinity;
    for (let i = 0; i < points.length; i++) {
      const d = Math.hypot(bp.x - points[i].pos.x, bp.y - points[i].pos.y);
      if (d < nearestDist) {
        nearestDist = d;
        nearestIdx = i;
      }
    }
    return {
      pos: { ...bp },
      angle: angleOf(points[nearestIdx].tangent),
      width: 40,
      length: 55,
      active: true,
      cooldownTimer: 0,
    };
  });

  // Obstacles
  const obstacles: Obstacle[] = OBSTACLE_POSITIONS.map((pos, i) => ({
    pos: { ...pos },
    radius: 16,
    rotation: 0,
    rotationSpeed: 0.02 + (i % 3) * 0.01,
    hitTimer: 0,
  }));

  return {
    centerPoints,
    points,
    totalLength,
    width: TRACK_WIDTH,
    boostPads,
    obstacles,
    startLine,
    checkpointCount: 4,
  };
}

/**
 * Finds the closest track point index to a position.
 */
export function findClosestTrackPoint(track: TrackData, pos: Vec2): number {
  let nearest = 0;
  let nearestDist = Infinity;
  for (let i = 0; i < track.points.length; i++) {
    const d = Math.hypot(pos.x - track.points[i].pos.x, pos.y - track.points[i].pos.y);
    if (d < nearestDist) {
      nearestDist = d;
      nearest = i;
    }
  }
  return nearest;
}

/**
 * Returns the distance from a position to the track center line.
 * Negative = left of center, positive = right of center (relative to track direction).
 */
export function distanceFromCenter(track: TrackData, pos: Vec2): number {
  const idx = findClosestTrackPoint(track, pos);
  const tp = track.points[idx];
  const toPos = sub(pos, tp.pos);
  return toPos.x * tp.normal.x + toPos.y * tp.normal.y;
}

/** Returns whether a position is on the track (within half-width). */
export function isOnTrack(track: TrackData, pos: Vec2): boolean {
  const idx = findClosestTrackPoint(track, pos);
  const tp = track.points[idx];
  const d = Math.hypot(pos.x - tp.pos.x, pos.y - tp.pos.y);
  return d < track.width / 2;
}

/**
 * Finds the nearest point on the center line and returns its arc length.
 */
export function getArcLength(track: TrackData, pos: Vec2): number {
  const idx = findClosestTrackPoint(track, pos);
  return track.points[idx].arcLength;
}

/**
 * Finds the nearest point on the center line and returns its index and the
 * "progress" offset (0..1) toward the next point.
 */
export function getTrackProgress(
  track: TrackData,
  pos: Vec2,
): { index: number; progress: number; arcLength: number } {
  const idx = findClosestTrackPoint(track, pos);
  const tp = track.points[idx];
  const next = track.points[(idx + 1) % track.points.length];
  const segVec = sub(next.pos, tp.pos);
  const segLen = length(segVec);
  if (segLen < 0.0001) return { index: idx, progress: 0, arcLength: tp.arcLength };
  const toPos = sub(pos, tp.pos);
  const t = Math.max(0, Math.min(1, (toPos.x * segVec.x + toPos.y * segVec.y) / (segLen * segLen)));
  return { index: idx, progress: t, arcLength: tp.arcLength + t * segLen };
}

/** Checks if a position is within a boost pad's bounds. */
export function checkBoostPadCollision(
  track: TrackData,
  pos: Vec2,
): BoostPad | null {
  for (const pad of track.boostPads) {
    if (!pad.active) continue;
    const d = Math.hypot(pos.x - pad.pos.x, pos.y - pad.pos.y);
    if (d < pad.length / 2 + 10) {
      return pad;
    }
  }
  return null;
}

/** Checks if a position collides with any obstacle. Returns the obstacle or null. */
export function checkObstacleCollision(
  track: TrackData,
  pos: Vec2,
  radius: number,
): Obstacle | null {
  for (const obs of track.obstacles) {
    const d = Math.hypot(pos.x - obs.pos.x, pos.y - obs.pos.y);
    if (d < obs.radius + radius) {
      return obs;
    }
  }
  return null;
}

/** Checks if a position is near the start line (for lap counting). */
export function isNearStartLine(track: TrackData, pos: Vec2): boolean {
  return distToSegment(pos, track.startLine.a, track.startLine.b) < 30;
}
