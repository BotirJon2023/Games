import type { JPVec2 } from './types';

export class JPTerrain {
  points: JPVec2[] = [];
  normals: JPVec2[] = [];
  width: number;
  segmentWidth: number;
  readonly worldHeight = 1000;

  constructor(width: number, segmentWidth = 8) {
    this.width = width;
    this.segmentWidth = segmentWidth;
    this.generate();
  }

  generate() {
    this.points = [];
    this.normals = [];
    const segW = this.segmentWidth;
    const numPoints = Math.floor(this.width / segW) + 1;
    const baseY = 520;
    const heights: number[] = [];

    for (let i = 0; i < numPoints; i++) {
      const t = i / numPoints;
      // Mountain trail terrain — steep, winding
      let h = Math.sin(t * Math.PI * 7) * 90;
      h += Math.sin(t * Math.PI * 20 + 1.5) * 35;
      h += Math.sin(t * Math.PI * 55 + 3.2) * 10;
      // Big mountain drops
      h += Math.sin(t * Math.PI * 3 + 0.8) * 70;
      // Stair-step section (like temple steps)
      if (t > 0.35 && t < 0.42) {
        h += Math.floor((t - 0.35) / 0.02) * 15;
      }
      // Overall descent
      h += (1 - t) * 80;
      heights.push(baseY + h);
    }

    // Smooth
    for (let pass = 0; pass < 3; pass++) {
      const smoothed = [...heights];
      for (let i = 1; i < numPoints - 1; i++) {
        smoothed[i] = (heights[i - 1] + 2 * heights[i] + heights[i + 1]) / 4;
      }
      heights.splice(0, heights.length, ...smoothed);
    }

    for (let i = 0; i < numPoints; i++) {
      this.points.push({ x: i * segW, y: heights[i] });
    }

    for (let i = 0; i < numPoints; i++) {
      let dx: number, dy: number;
      if (i === 0) {
        dx = this.points[1].x - this.points[0].x;
        dy = this.points[1].y - this.points[0].y;
      } else if (i === numPoints - 1) {
        dx = this.points[i].x - this.points[i - 1].x;
        dy = this.points[i].y - this.points[i - 1].y;
      } else {
        dx = this.points[i + 1].x - this.points[i - 1].x;
        dy = this.points[i + 1].y - this.points[i - 1].y;
      }
      const len = Math.sqrt(dx * dx + dy * dy) || 1;
      this.normals.push({ x: -dy / len, y: dx / len });
      if (this.normals[i].y > 0) {
        this.normals[i].x *= -1;
        this.normals[i].y *= -1;
      }
    }
  }

  getHeightAt(x: number): number {
    const segW = this.segmentWidth;
    const idx = Math.floor(x / segW);
    if (idx < 0) return this.points[0]?.y ?? 520;
    if (idx >= this.points.length - 1) return this.points[this.points.length - 1]?.y ?? 520;
    const frac = (x - idx * segW) / segW;
    const y0 = this.points[idx].y;
    const y1 = this.points[idx + 1].y;
    const smooth = frac * frac * (3 - 2 * frac);
    return y0 + (y1 - y0) * smooth;
  }

  getAngleAt(x: number): number {
    const segW = this.segmentWidth;
    const idx = Math.floor(x / segW);
    if (idx < 0) return 0;
    if (idx >= this.points.length - 1) return 0;
    const dx = this.points[idx + 1].x - this.points[idx].x;
    const dy = this.points[idx + 1].y - this.points[idx].y;
    return Math.atan2(dy, dx);
  }

  getNormalAt(x: number): JPVec2 {
    const segW = this.segmentWidth;
    const idx = Math.floor(x / segW);
    if (idx < 0) return { x: 0, y: -1 };
    if (idx >= this.normals.length) return { x: 0, y: -1 };
    return this.normals[idx];
  }
}
