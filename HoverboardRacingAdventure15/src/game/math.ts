import type { Vec2 } from './types';

export function add(a: Vec2, b: Vec2): Vec2 {
  return { x: a.x + b.x, y: a.y + b.y };
}

export function sub(a: Vec2, b: Vec2): Vec2 {
  return { x: a.x - b.x, y: a.y - b.y };
}

export function scale(v: Vec2, s: number): Vec2 {
  return { x: v.x * s, y: v.y * s };
}

export function length(v: Vec2): number {
  return Math.sqrt(v.x * v.x + v.y * v.y);
}

export function normalize(v: Vec2): Vec2 {
  const len = length(v);
  if (len < 0.0001) return { x: 0, y: 0 };
  return { x: v.x / len, y: v.y / len };
}

export function dist(a: Vec2, b: Vec2): number {
  return length(sub(a, b));
}

export function angleOf(v: Vec2): number {
  return Math.atan2(v.y, v.x);
}

export function rotate(v: Vec2, angle: number): Vec2 {
  const cos = Math.cos(angle);
  const sin = Math.sin(angle);
  return { x: v.x * cos - v.y * sin, y: v.x * sin + v.y * cos };
}

export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

export function clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(max, v));
}

export function approach(current: number, target: number, delta: number): number {
  if (current < target) return Math.min(current + delta, target);
  if (current > target) return Math.max(current - delta, target);
  return current;
}

/** Catmull-Rom interpolation between p1 and p2, with p0 and p3 as neighbors. */
function catmullRom(p0: Vec2, p1: Vec2, p2: Vec2, p3: Vec2, t: number): Vec2 {
  const t2 = t * t;
  const t3 = t2 * t;
  return {
    x: 0.5 * (
      2 * p1.x +
      (-p0.x + p2.x) * t +
      (2 * p0.x - 5 * p1.x + 4 * p2.x - p3.x) * t2 +
      (-p0.x + 3 * p1.x - 3 * p2.x + p3.x) * t3
    ),
    y: 0.5 * (
      2 * p1.y +
      (-p0.y + p2.y) * t +
      (2 * p0.y - 5 * p1.y + 4 * p2.y - p3.y) * t2 +
      (-p0.y + 3 * p1.y - 3 * p2.y + p3.y) * t3
    ),
  };
}

/**
 * Generates a closed-loop smooth spline from control points.
 * Returns a dense list of points along the curve.
 */
export function generateSpline(controlPoints: Vec2[], segmentsPerControl: number = 20): Vec2[] {
  const n = controlPoints.length;
  const points: Vec2[] = [];

  for (let i = 0; i < n; i++) {
    const p0 = controlPoints[(i - 1 + n) % n];
    const p1 = controlPoints[i];
    const p2 = controlPoints[(i + 1) % n];
    const p3 = controlPoints[(i + 2) % n];

    for (let s = 0; s < segmentsPerControl; s++) {
      const t = s / segmentsPerControl;
      points.push(catmullRom(p0, p1, p2, p3, t));
    }
  }
  return points;
}

/** Returns true if point p is within distance of the line segment a-b. */
export function distToSegment(p: Vec2, a: Vec2, b: Vec2): number {
  const ab = sub(b, a);
  const abLen2 = ab.x * ab.x + ab.y * ab.y;
  if (abLen2 < 0.0001) return dist(p, a);
  let t = ((p.x - a.x) * ab.x + (p.y - a.y) * ab.y) / abLen2;
  t = clamp(t, 0, 1);
  const proj = add(a, scale(ab, t));
  return dist(p, proj);
}
