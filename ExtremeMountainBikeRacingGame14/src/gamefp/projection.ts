export interface FPCamera {
  x: number;
  y: number;
  z: number;
  depth: number;
}

export interface ProjectedPoint {
  screenX: number;
  screenY: number;
  scale: number;
  visible: boolean;
}

export function projectPoint(
  worldX: number,
  worldY: number,
  worldZ: number,
  cameraX: number,
  cameraY: number,
  cameraZ: number,
  cameraDepth: number,
  width: number,
  height: number
): ProjectedPoint {
  const dx = worldX - cameraX;
  const dy = worldY - cameraY;
  const dz = worldZ - cameraZ;

  if (dz <= 0) {
    return { screenX: 0, screenY: 0, scale: 0, visible: false };
  }

  const scale = cameraDepth / dz;
  const screenX = width / 2 + dx * scale;
  const screenY = height / 2 - dy * scale;

  return { screenX, screenY, scale, visible: true };
}

export function getCameraDepth(): number {
  // Field of view ~80 degrees
  return 1 / Math.tan((80 / 2) * Math.PI / 180);
}

export function lerp(a: number, b: number, t: number): number {
  return a + (b - a) * t;
}

export function clamp(v: number, min: number, max: number): number {
  return Math.max(min, Math.min(v, max));
}

export function smoothstep(edge0: number, edge1: number, x: number): number {
  const t = clamp((x - edge0) / (edge1 - edge0), 0, 1);
  return t * t * (3 - 2 * t);
}
