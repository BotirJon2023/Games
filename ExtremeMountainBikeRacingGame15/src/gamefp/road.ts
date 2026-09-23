import type { FPSegment, FPSprite } from './types';

const SEGMENT_LENGTH = 200;
const ROAD_WIDTH = 2200;
const RUMBLE_LENGTH = 4;

export interface SegmentColors {
  road: string;
  grass: string;
  rumble: string;
  lane: string;
  edge: string;
}

const COLORS: Record<string, SegmentColors> = {
  LIGHT: { road: '#7a7a7a', grass: '#4a8a3a', rumble: '#e63946', lane: '#ffffff', edge: '#333' },
  DARK: { road: '#6a6a6a', grass: '#3d7a2d', rumble: '#ffffff', lane: '', edge: '#222' },
  START: { road: '#ffffff', grass: '#4a8a3a', rumble: '#ffffff', lane: '', edge: '#333' },
  FINISH: { road: '#222222', grass: '#4a8a3a', rumble: '#ffffff', lane: '', edge: '#111' },
};

export function getFPSegmentColors(seg: FPSegment): SegmentColors {
  if (seg.index < 5) return COLORS.START;
  const distFromEnd = totalSegments - seg.index;
  if (distFromEnd < 40) {
    return Math.floor(distFromEnd / 4) % 2 === 0 ? COLORS.FINISH : COLORS.LIGHT;
  }
  return seg.color === 'light' ? COLORS.LIGHT : COLORS.DARK;
}

let totalSegments = 0;

export class RoadFP {
  segments: FPSegment[] = [];
  segmentLength: number;
  roadWidth: number;
  totalLength: number;
  trackSegments: number;

  constructor() {
    this.segmentLength = SEGMENT_LENGTH;
    this.roadWidth = ROAD_WIDTH;
    this.trackSegments = 1000;
    this.totalLength = this.trackSegments * this.segmentLength;
    totalSegments = this.trackSegments;
    this.build();
  }

  build() {
    this.segments = [];

    for (let i = 0; i < this.trackSegments; i++) {
      const t = i / this.trackSegments;

      // Curves — winding mountain road
      let curve = Math.sin(t * Math.PI * 7) * 3;
      curve += Math.sin(t * Math.PI * 15 + 1.5) * 1.8;
      curve += Math.sin(t * Math.PI * 28 + 3) * 0.8;
      // Sharp switchback section
      if (t > 0.3 && t < 0.38) curve = -3.5;
      if (t > 0.38 && t < 0.46) curve = 3.5;
      // Long straight near end for final sprint
      if (t > 0.9) curve *= 0.2;
      if (t < 0.04) curve = 0;

      // Hills — dramatic mountain elevation changes
      let y = Math.sin(t * Math.PI * 4) * 1000;
      y += Math.sin(t * Math.PI * 10 + 0.8) * 400;
      y += Math.sin(t * Math.PI * 22 + 2.1) * 150;
      // Big jump ramp
      if (t > 0.5 && t < 0.54) {
        y += Math.sin((t - 0.5) / 0.04 * Math.PI) * 700;
      }
      // Rolling whoops section
      if (t > 0.7 && t < 0.78) {
        y += Math.abs(Math.sin((t - 0.7) / 0.08 * Math.PI * 4)) * 250;
      }

      const seg: FPSegment = {
        index: i,
        z: i * this.segmentLength,
        y,
        curve,
        color: Math.floor(i / RUMBLE_LENGTH) % 2 === 0 ? 'light' : 'dark',
        sprites: [],
      };

      // Add scenery
      if (i > 8 && Math.random() < 0.18) {
        const types: FPSprite['type'][] = ['tree', 'pine', 'bush', 'rock', 'cliff'];
        seg.sprites.push({
          offset: 1.8 + Math.random() * 3,
          side: 1,
          type: types[Math.floor(Math.random() * types.length)],
        });
      }
      if (i > 8 && Math.random() < 0.18) {
        const types: FPSprite['type'][] = ['tree', 'pine', 'bush', 'rock', 'cliff'];
        seg.sprites.push({
          offset: 1.8 + Math.random() * 3,
          side: -1,
          type: types[Math.floor(Math.random() * types.length)],
        });
      }

      // Mile markers every 80 segments
      if (i > 0 && i % 80 === 0) {
        seg.sprites.push({ offset: 2.5, side: 1, type: 'milemarker' });
      }

      // Flags near start and finish
      if (i === 6) {
        seg.sprites.push({ offset: 2, side: 1, type: 'flag' });
        seg.sprites.push({ offset: 2, side: -1, type: 'flag' });
      }

      this.segments.push(seg);
    }
  }

  getSegment(index: number): FPSegment {
    return this.segments[Math.max(0, Math.min(this.segments.length - 1, Math.floor(index)))];
  }

  getSegmentAtZ(z: number): FPSegment {
    return this.getSegment(Math.floor(z / this.segmentLength));
  }

  getTrackLength(): number {
    return this.totalLength;
  }
}
