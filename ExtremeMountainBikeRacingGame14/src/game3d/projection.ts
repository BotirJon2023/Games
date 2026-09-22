import type { Segment3D } from './types';

/**
 * 3D projection: projects world coordinates to screen coordinates.
 * Based on classic pseudo-3D road rendering technique.
 */

export interface Camera {
  x: number; // lateral offset
  y: number; // height above road
  z: number; // depth
  focalLength: number;
}

export function project3D(
  p: Segment3D['p1'],
  cameraX: number,
  cameraY: number,
  cameraZ: number,
  cameraDepth: number,
  width: number,
  height: number,
  roadWidth: number
): void {
  const transX = p.world.x - cameraX;
  const transY = p.world.y - cameraY;
  const transZ = p.world.z - cameraZ;

  p.camera.x = transX;
  p.camera.y = transY;
  p.camera.z = transZ;
  p.screen.scale = cameraDepth / Math.max(0.1, transZ);

  const projectedX = transX * p.screen.scale;
  const projectedY = transY * p.screen.scale;
  const projectedW = roadWidth * p.screen.scale;

  p.screen.x = Math.round(width / 2 + projectedX);
  p.screen.y = Math.round(height / 2 - projectedY);
  p.screen.w = Math.round(projectedW);
}

export function getCameraDepth(focalLength: number, height: number): number {
  return 1 / Math.tan((focalLength / 2) * Math.PI / 180);
}

export function interpolateScreenY(
  p1: Segment3D['p1'],
  p2: Segment3D['p1']
): number {
  // For sorting/drawing purposes
  return (p1.screen.y + p2.screen.y) / 2;
}

export function easeIn(a: number, b: number, percent: number): number {
  return a + (b - a) * Math.pow(percent, 2);
}

export function easeOut(a: number, b: number, percent: number): number {
  return a + (b - a) * (1 - Math.pow(1 - percent, 2));
}

export function percentRemaining(n: number, total: number): number {
  return (n % total) / total;
}

export function increase(start: number, increment: number, max: number): number {
  let result = start + increment;
  while (result >= max) result -= max;
  while (result < 0) result += max;
  return result;
}

export function interpolate(a: number, b: number, percent: number): number {
  return a + (b - a) * percent;
}

export function accelerate(current: number, accel: number, dt: number): number {
  return current + accel * dt;
}

export function limit(value: number, min: number, max: number): number {
  return Math.max(min, Math.min(value, max));
}
