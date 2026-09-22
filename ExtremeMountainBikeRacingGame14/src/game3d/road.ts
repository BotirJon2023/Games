import type { Segment3D, Sprite3D } from './types';

const SEGMENT_LENGTH = 200;
const ROAD_WIDTH = 2000;
const RUMBLE_LENGTH = 3;

const COLORS = {
  LIGHT: { road: '#6b6b6b', grass: '#3a7a3a', rumble: '#e63946', lane: '#ffffff' },
  DARK: { road: '#5a5a5a', grass: '#2d6a2d', rumble: '#ffffff', lane: '' },
  START: { road: '#ffffff', grass: '#3a7a3a', rumble: '#ffffff', lane: '' },
  FINISH: { road: '#1a1a1a', grass: '#3a7a3a', rumble: '#ffffff', lane: '' },
};

export type SegmentColors = { road: string; grass: string; rumble: string; lane: string };

export function getSegmentColors(seg: Segment3D): SegmentColors {
  if (seg.index < 5) return COLORS.START;
  // Checkered near finish
  const distFromEnd = totalSegments - seg.index;
  if (distFromEnd < 40) {
    return Math.floor(distFromEnd / 4) % 2 === 0 ? COLORS.FINISH : COLORS.LIGHT;
  }
  return seg.color === 'light' ? COLORS.LIGHT : COLORS.DARK;
}

let totalSegments = 0;

export class Road3D {
  segments: Segment3D[] = [];
  totalLength: number;
  segmentLength: number;
  roadWidth: number;
  trackSegments: number;

  constructor() {
    this.segmentLength = SEGMENT_LENGTH;
    this.roadWidth = ROAD_WIDTH;
    this.trackSegments = 800;
    this.totalLength = this.trackSegments * this.segmentLength;
    totalSegments = this.trackSegments;
    this.build();
  }

  build() {
    this.segments = [];

    // Build road with curves, hills, and scenery
    let curve = 0;
    let hillY = 0;

    for (let i = 0; i < this.trackSegments; i++) {
      const seg: Segment3D = {
        index: i,
        p1: { world: { x: 0, y: 0, z: 0 }, camera: { x: 0, y: 0, z: 0 }, screen: { x: 0, y: 0, w: 0, scale: 0 } },
        p2: { world: { x: 0, y: 0, z: 0 }, camera: { x: 0, y: 0, z: 0 }, screen: { x: 0, y: 0, w: 0, scale: 0 } },
        curve: 0,
        color: Math.floor(i / RUMBLE_LENGTH) % 2 === 0 ? 'light' : 'dark',
        clip: 0,
        sprites: [],
      };

      // Curve patterns
      const t = i / this.trackSegments;
      // Gentle S-curves
      curve = Math.sin(t * Math.PI * 8) * 2.5;
      // Add sharper turns
      curve += Math.sin(t * Math.PI * 16 + 1) * 1.5;
      // Long sweeping curve near end
      if (t > 0.85) curve = 1.5;
      if (t < 0.05) curve = 0;

      seg.curve = curve;

      // Hills
      hillY = Math.sin(t * Math.PI * 5) * 800;
      hillY += Math.sin(t * Math.PI * 12 + 0.5) * 300;
      // Big jump ramp
      if (t > 0.4 && t < 0.45) {
        hillY += Math.sin((t - 0.4) / 0.05 * Math.PI) * 600;
      }
      // Small bump
      if (t > 0.65 && t < 0.68) {
        hillY += Math.sin((t - 0.65) / 0.03 * Math.PI) * 200;
      }

      seg.p1.world = { x: 0, y: hillY, z: i * this.segmentLength };
      seg.p2.world = { x: 0, y: hillY, z: (i + 1) * this.segmentLength };

      // Add scenery sprites
      if (i > 10 && Math.random() < 0.15) {
        const types: Sprite3D['type'][] = ['tree', 'bush', 'rock', 'flag'];
        const type = types[Math.floor(Math.random() * types.length)];
        seg.sprites.push({
          offset: 1.5 + Math.random() * 2,
          type,
          side: 1,
        });
      }
      if (i > 10 && Math.random() < 0.15) {
        const types: Sprite3D['type'][] = ['tree', 'bush', 'rock'];
        const type = types[Math.floor(Math.random() * types.length)];
        seg.sprites.push({
          offset: 1.5 + Math.random() * 2,
          type,
          side: -1,
        });
      }

      // Signs every 100 segments
      if (i > 0 && i % 100 === 0) {
        seg.sprites.push({ offset: 2.5, type: 'sign', side: 1 });
      }

      this.segments.push(seg);
    }
  }

  getSegment(index: number): Segment3D {
    return this.segments[Math.max(0, Math.min(this.segments.length - 1, Math.floor(index)))];
  }

  getSegmentAtZ(z: number): Segment3D {
    return this.getSegment(Math.floor(z / this.segmentLength));
  }

  getTrackLength(): number {
    return this.totalLength;
  }
}
