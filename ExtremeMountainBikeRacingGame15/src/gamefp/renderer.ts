import type { FPBike, FPSegment, FPSprite, FPParticle, FPCloud } from './types';
import { RoadFP, getFPSegmentColors } from './road';
import { projectPoint, getCameraDepth, lerp, clamp, smoothstep } from './projection';

interface ScreenPoint {
  x: number;
  y: number;
  w: number;
  scale: number;
  visible: boolean;
}

export class RendererFP {
  ctx: CanvasRenderingContext2D;
  width: number;
  height: number;
  cameraDepth: number;
  clouds: FPCloud[] = [];
  time: number = 0;

  constructor(ctx: CanvasRenderingContext2D, width: number, height: number) {
    this.ctx = ctx;
    this.width = width;
    this.height = height;
    this.cameraDepth = getCameraDepth();
    this.initClouds();
  }

  resize(width: number, height: number) {
    this.width = width;
    this.height = height;
  }

  initClouds() {
    this.clouds = [];
    for (let i = 0; i < 15; i++) {
      this.clouds.push({
        x: (Math.random() - 0.5) * 3,
        y: 0.2 + Math.random() * 0.4,
        scale: 0.6 + Math.random() * 1.8,
        drift: 0.0002 + Math.random() * 0.0003,
      });
    }
  }

  updateClouds(dt: number) {
    for (const c of this.clouds) {
      c.x += c.drift * dt;
      if (c.x > 2) c.x = -2;
    }
  }

  shadeColor(color: string, percent: number): string {
    const num = parseInt(color.replace('#', ''), 16);
    const r = clamp((num >> 16) + percent, 0, 255);
    const g = clamp(((num >> 8) & 0x00ff) + percent, 0, 255);
    const b = clamp((num & 0x0000ff) + percent, 0, 255);
    return `rgb(${r},${g},${b})`;
  }

  drawSky(bike: FPBike) {
    const { ctx, width, height } = this;

    // Mountain sky — deep blue to warm horizon
    const grad = ctx.createLinearGradient(0, 0, 0, height * 0.65);
    grad.addColorStop(0, '#1e3a5f');
    grad.addColorStop(0.15, '#3a6a8a');
    grad.addColorStop(0.35, '#7ab0c4');
    grad.addColorStop(0.5, '#d4e4d0');
    grad.addColorStop(0.62, '#e8d5a0');
    grad.addColorStop(1, '#c8a870');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height * 0.65);

    // Sun
    const sunX = width * 0.5 - bike.lean * 60;
    const sunY = height * 0.38;
    const sunR = 50;

    const glow = ctx.createRadialGradient(sunX, sunY, 0, sunX, sunY, sunR * 3);
    glow.addColorStop(0, 'rgba(255, 240, 180, 0.4)');
    glow.addColorStop(0.3, 'rgba(255, 200, 120, 0.2)');
    glow.addColorStop(1, 'rgba(255, 150, 80, 0)');
    ctx.fillStyle = glow;
    ctx.fillRect(0, 0, width, height * 0.65);

    ctx.fillStyle = '#fff5d0';
    ctx.beginPath();
    ctx.arc(sunX, sunY, sunR, 0, Math.PI * 2);
    ctx.fill();

    // Distant mountain ranges (3 layers for depth)
    this.drawMountainRange(0.55, 'rgba(50, 60, 80, 0.5)', bike, 0.015, 35, 15);
    this.drawMountainRange(0.58, 'rgba(35, 45, 65, 0.6)', bike, 0.02, 28, 12);
    this.drawMountainRange(0.62, 'rgba(20, 30, 45, 0.7)', bike, 0.03, 20, 8);
  }

  drawMountainRange(yPercent: number, color: string, bike: FPBike, freq: number, baseH: number, varH: number) {
    const { ctx, width, height } = this;
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.moveTo(0, height * yPercent);
    for (let x = 0; x <= width; x += 20) {
      const h = baseH + Math.sin(x * freq + bike.position * 0.00005) * varH + Math.sin(x * freq * 3) * (varH * 0.4);
      ctx.lineTo(x, height * yPercent - h);
    }
    ctx.lineTo(width, height * yPercent);
    ctx.closePath();
    ctx.fill();
  }

  drawClouds(bike: FPBike) {
    const { ctx, width, height } = this;
    for (const c of this.clouds) {
      const cx = width * 0.5 + c.x * width * 0.35 - bike.lean * 40;
      const cy = height * (0.08 + c.y * 0.2);
      const s = c.scale * 18;
      ctx.fillStyle = 'rgba(255, 250, 240, 0.6)';
      ctx.beginPath();
      ctx.arc(cx, cy, s, 0, Math.PI * 2);
      ctx.arc(cx + s * 0.8, cy - s * 0.3, s * 1.3, 0, Math.PI * 2);
      ctx.arc(cx + s * 1.8, cy, s * 1.1, 0, Math.PI * 2);
      ctx.arc(cx + s * 0.9, cy + s * 0.5, s, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  projectSegment(
    seg: FPSegment,
    nextSeg: FPSegment,
    cameraX: number,
    cameraY: number,
    cameraZ: number,
    roadWidth: number
  ): { p1: ScreenPoint; p2: ScreenPoint } {
    const p1Proj = projectPoint(0, seg.y, seg.z, cameraX, cameraY, cameraZ, this.cameraDepth, this.width, this.height);
    const p2Proj = projectPoint(0, nextSeg.y, nextSeg.z, cameraX, cameraY, cameraZ, this.cameraDepth, this.width, this.height);

    return {
      p1: {
        x: p1Proj.screenX,
        y: p1Proj.screenY,
        w: roadWidth * p1Proj.scale,
        scale: p1Proj.scale,
        visible: p1Proj.visible,
      },
      p2: {
        x: p2Proj.screenX,
        y: p2Proj.screenY,
        w: roadWidth * p2Proj.scale,
        scale: p2Proj.scale,
        visible: p2Proj.visible,
      },
    };
  }

  drawRoad(
    road: RoadFP,
    bike: FPBike,
    baseSegmentIndex: number,
    drawDistance: number,
    cameraX: number,
    cameraY: number,
    cameraZ: number
  ) {
    const { ctx, width, height } = this;
    const segments = road.segments;
    const segmentsPerDraw = Math.min(drawDistance, segments.length);

    let maxY = height;

    for (let n = 0; n < segmentsPerDraw; n++) {
      const segIdx = (baseSegmentIndex + n) % segments.length;
      const seg = segments[segIdx];
      const nextSeg = segments[(segIdx + 1) % segments.length];

      const { p1, p2 } = this.projectSegment(seg, nextSeg, cameraX, cameraY, cameraZ, road.roadWidth);

      if (!p1.visible || !p2.visible || p2.y >= maxY || p2.y >= p1.y) continue;

      this.drawSegmentQuad(seg, p1, p2, road.roadWidth, bike);
      maxY = p2.y;
    }
  }

  drawSegmentQuad(seg: FPSegment, p1: ScreenPoint, p2: ScreenPoint, roadWidth: number, bike: FPBike) {
    const { ctx } = this;
    const colors = getFPSegmentColors(seg);

    // Grass / ground
    ctx.fillStyle = colors.grass;
    ctx.fillRect(0, p2.y, this.width, p1.y - p2.y + 1);

    // Road edges (dirt/gravel strip)
    const edgeW1 = p1.w * 0.12;
    const edgeW2 = p2.w * 0.12;
    ctx.fillStyle = '#8a7a5a';
    this.quad(p1.x - p1.w - edgeW1, p1.y, p1.x - p1.w, p1.y, p2.x - p2.w, p2.y, p2.x - p2.w - edgeW2, p2.y);
    this.quad(p1.x + p1.w + edgeW1, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x + p2.w + edgeW2, p2.y);

    // Rumble strips
    const rumbleW1 = p1.w * 0.06;
    const rumbleW2 = p2.w * 0.06;
    ctx.fillStyle = colors.rumble;
    this.quad(p1.x - p1.w - rumbleW1, p1.y, p1.x - p1.w, p1.y, p2.x - p2.w, p2.y, p2.x - p2.w - rumbleW2, p2.y);
    this.quad(p1.x + p1.w + rumbleW1, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x + p2.w + rumbleW2, p2.y);

    // Road surface
    ctx.fillStyle = colors.road;
    this.quad(p1.x - p1.w, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x - p2.w, p2.y);

    // Center lane markings
    if (seg.color === 'light' && colors.lane) {
      const laneW1 = p1.w * 0.015;
      const laneW2 = p2.w * 0.015;
      ctx.fillStyle = colors.lane;
      this.quad(p1.x - laneW1, p1.y, p1.x + laneW1, p1.y, p2.x + laneW2, p2.y, p2.x - laneW2, p2.y);
    }

    // Road texture — subtle tire tracks
    if (p1.w > 20) {
      ctx.fillStyle = 'rgba(0,0,0,0.08)';
      const trackOffset = p1.w * 0.25;
      this.quad(p1.x - trackOffset - 3, p1.y, p1.x - trackOffset + 3, p1.y, p2.x - trackOffset * (p2.w / p1.w) + 2, p2.y, p2.x - trackOffset * (p2.w / p1.w) - 2, p2.y);
      this.quad(p1.x + trackOffset - 3, p1.y, p1.x + trackOffset + 3, p1.y, p2.x + trackOffset * (p2.w / p1.w) - 2, p2.y, p2.x + trackOffset * (p2.w / p1.w) + 2, p2.y);
    }
  }

  quad(x1: number, y1: number, x2: number, y2: number, x3: number, y3: number, x4: number, y4: number) {
    const { ctx } = this;
    ctx.beginPath();
    ctx.moveTo(x1, y1);
    ctx.lineTo(x2, y2);
    ctx.lineTo(x3, y3);
    ctx.lineTo(x4, y4);
    ctx.closePath();
    ctx.fill();
  }

  drawSprites(
    road: RoadFP,
    bike: FPBike,
    baseSegmentIndex: number,
    drawDistance: number,
    cameraX: number,
    cameraY: number,
    cameraZ: number
  ) {
    const { ctx, width } = this;
    const segments = road.segments;

    // Draw from far to near
    for (let n = drawDistance - 1; n >= 0; n--) {
      const segIdx = (baseSegmentIndex + n) % segments.length;
      const seg = segments[segIdx];

      const proj = projectPoint(0, seg.y, seg.z, cameraX, cameraY, cameraZ, this.cameraDepth, width, this.height);
      if (!proj.visible) continue;

      for (const sprite of seg.sprites) {
        const spriteWorldX = sprite.offset * sprite.side * road.roadWidth;
        const spriteProj = projectPoint(spriteWorldX, seg.y, seg.z, cameraX, cameraY, cameraZ, this.cameraDepth, width, this.height);

        if (!spriteProj.visible) continue;

        const scale = proj.scale;
        const sx = spriteProj.screenX;
        const sy = spriteProj.screenY;

        if (sx < -300 || sx > width + 300) continue;

        const drawScale = scale * road.roadWidth * 0.0035;
        this.drawFPSprite(sprite, sx, sy, drawScale, bike);
      }
    }
  }

  drawFPSprite(sprite: FPSprite, x: number, y: number, scale: number, bike: FPBike) {
    const { ctx } = this;

    switch (sprite.type) {
      case 'pine': {
        const trunkW = Math.max(1, scale * 0.12);
        const trunkH = Math.max(1, scale * 0.3);
        const fw = Math.max(3, scale * 0.7);
        const fh = Math.max(4, scale * 1.0);
        ctx.fillStyle = '#3a2818';
        ctx.fillRect(x - trunkW / 2, y - trunkH, trunkW, trunkH);
        ctx.fillStyle = '#1a4a1a';
        ctx.beginPath();
        ctx.moveTo(x, y - trunkH - fh);
        ctx.lineTo(x - fw * 0.5, y - trunkH - fh * 0.3);
        ctx.lineTo(x - fw * 0.3, y - trunkH - fh * 0.35);
        ctx.lineTo(x - fw * 0.65, y - trunkH);
        ctx.lineTo(x + fw * 0.65, y - trunkH);
        ctx.lineTo(x + fw * 0.3, y - trunkH - fh * 0.35);
        ctx.lineTo(x + fw * 0.5, y - trunkH - fh * 0.3);
        ctx.closePath();
        ctx.fill();
        ctx.fillStyle = '#2d5a2d';
        ctx.beginPath();
        ctx.moveTo(x, y - trunkH - fh);
        ctx.lineTo(x - fw * 0.15, y - trunkH - fh * 0.5);
        ctx.lineTo(x + fw * 0.05, y - trunkH - fh * 0.45);
        ctx.closePath();
        ctx.fill();
        break;
      }
      case 'tree': {
        const trunkW = Math.max(1, scale * 0.1);
        const trunkH = Math.max(1, scale * 0.2);
        const cs = Math.max(3, scale * 0.55);
        ctx.fillStyle = '#3d2817';
        ctx.fillRect(x - trunkW / 2, y - trunkH, trunkW, trunkH);
        ctx.fillStyle = '#2d5a2d';
        ctx.beginPath();
        ctx.arc(x, y - trunkH - cs * 0.4, cs * 0.6, 0, Math.PI * 2);
        ctx.arc(x - cs * 0.4, y - trunkH - cs * 0.2, cs * 0.5, 0, Math.PI * 2);
        ctx.arc(x + cs * 0.4, y - trunkH - cs * 0.2, cs * 0.5, 0, Math.PI * 2);
        ctx.arc(x, y - trunkH - cs * 0.7, cs * 0.45, 0, Math.PI * 2);
        ctx.fill();
        ctx.fillStyle = '#3a6a3a';
        ctx.beginPath();
        ctx.arc(x - cs * 0.2, y - trunkH - cs * 0.6, cs * 0.25, 0, Math.PI * 2);
        ctx.fill();
        break;
      }
      case 'bush': {
        const s = Math.max(2, scale * 0.4);
        ctx.fillStyle = '#2d4a2d';
        ctx.beginPath();
        ctx.arc(x, y - s * 0.3, s * 0.6, 0, Math.PI * 2);
        ctx.arc(x + s * 0.5, y - s * 0.2, s * 0.45, 0, Math.PI * 2);
        ctx.arc(x - s * 0.5, y - s * 0.2, s * 0.45, 0, Math.PI * 2);
        ctx.fill();
        break;
      }
      case 'rock': {
        const s = Math.max(3, scale * 0.5);
        ctx.fillStyle = '#5a5550';
        ctx.beginPath();
        ctx.moveTo(x - s * 0.6, y);
        ctx.lineTo(x - s * 0.7, y - s * 0.35);
        ctx.lineTo(x - s * 0.3, y - s * 0.65);
        ctx.lineTo(x + s * 0.2, y - s * 0.55);
        ctx.lineTo(x + s * 0.55, y - s * 0.25);
        ctx.lineTo(x + s * 0.5, y);
        ctx.closePath();
        ctx.fill();
        ctx.fillStyle = '#6a6560';
        ctx.beginPath();
        ctx.moveTo(x - s * 0.3, y - s * 0.65);
        ctx.lineTo(x + s * 0.1, y - s * 0.45);
        ctx.lineTo(x - s * 0.1, y - s * 0.35);
        ctx.closePath();
        ctx.fill();
        break;
      }
      case 'cliff': {
        const s = Math.max(5, scale * 0.8);
        ctx.fillStyle = '#4a4035';
        ctx.beginPath();
        ctx.moveTo(x - s * 0.5, y);
        ctx.lineTo(x - s * 0.4, y - s * 1.5);
        ctx.lineTo(x - s * 0.1, y - s * 1.8);
        ctx.lineTo(x + s * 0.3, y - s * 1.3);
        ctx.lineTo(x + s * 0.5, y);
        ctx.closePath();
        ctx.fill();
        // Striations
        ctx.strokeStyle = 'rgba(0,0,0,0.2)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(x - s * 0.2, y - s * 0.3);
        ctx.lineTo(x - s * 0.15, y - s * 1.4);
        ctx.moveTo(x + s * 0.1, y - s * 0.2);
        ctx.lineTo(x + s * 0.15, y - s * 1.1);
        ctx.stroke();
        break;
      }
      case 'flag': {
        const poleH = Math.max(8, scale * 1.8);
        const flagW = Math.max(4, scale * 0.6);
        ctx.fillStyle = '#999';
        ctx.fillRect(x - 1, y - poleH, 2, poleH);
        const wave = Math.sin(bike.position * 0.002 + x * 0.01) * 4;
        ctx.fillStyle = '#e63946';
        ctx.beginPath();
        ctx.moveTo(x + 2, y - poleH);
        ctx.lineTo(x + 2 + flagW, y - poleH + 4 + wave);
        ctx.lineTo(x + 2, y - poleH + 10);
        ctx.closePath();
        ctx.fill();
        break;
      }
      case 'milemarker': {
        const poleH = Math.max(6, scale * 0.8);
        const mw = Math.max(4, scale * 0.5);
        ctx.fillStyle = '#666';
        ctx.fillRect(x - 1, y - poleH, 2, poleH);
        ctx.fillStyle = '#ff8800';
        ctx.fillRect(x - mw / 2, y - poleH, mw, mw * 0.5);
        break;
      }
    }
  }

  drawOtherBikes(
    road: RoadFP,
    bike: FPBike,
    otherBikes: FPBike[],
    cameraX: number,
    cameraY: number,
    cameraZ: number
  ) {
    const { ctx, width } = this;

    for (const other of otherBikes) {
      if (other.id === bike.id) continue;

      // Only draw bikes ahead
      let relZ = other.position - bike.position;
      if (relZ < 0) continue;

      const seg = road.getSegmentAtZ(other.position);
      const otherWorldX = other.playerX * road.roadWidth;
      const proj = projectPoint(otherWorldX, seg.y, other.position, cameraX, cameraY, cameraZ, this.cameraDepth, width, this.height);

      if (!proj.visible) continue;

      const sx = proj.screenX;
      const sy = proj.screenY;
      const scale = proj.scale * road.roadWidth * 0.004;

      if (sx < -100 || sx > width + 100 || scale < 0.01) continue;

      // Draw a simplified biker from behind
      this.drawBikerFromBehind(sx, sy, scale, other);
    }
  }

  drawBikerFromBehind(x: number, y: number, scale: number, bike: FPBike) {
    const { ctx } = this;
    const s = scale;

    ctx.save();
    ctx.translate(x, y);
    ctx.scale(s, s);

    const color = bike.color;
    const jerseyColor = this.shadeColor(color, 20);
    const darkColor = this.shadeColor(color, -30);

    // Shadow
    ctx.fillStyle = 'rgba(0,0,0,0.25)';
    ctx.beginPath();
    ctx.ellipse(0, 2, 18, 4, 0, 0, Math.PI * 2);
    ctx.fill();

    // Rear wheel
    ctx.fillStyle = '#1a1a1a';
    ctx.beginPath();
    ctx.arc(0, 0, 14, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#555';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(0, 0, 11, 0, Math.PI * 2);
    ctx.stroke();

    // Body/torso from behind
    ctx.fillStyle = jerseyColor;
    ctx.beginPath();
    ctx.roundRect ? ctx.roundRect(-12, -35, 24, 28, 6) : ctx.rect(-12, -35, 24, 28);
    ctx.fill();

    // Shoulders
    ctx.fillStyle = this.shadeColor(color, 10);
    ctx.beginPath();
    ctx.arc(-12, -33, 7, 0, Math.PI * 2);
    ctx.arc(12, -33, 7, 0, Math.PI * 2);
    ctx.fill();

    // Head/helmet
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(0, -42, 9, 0, Math.PI * 2);
    ctx.fill();
    // Helmet stripe
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(-7, -42);
    ctx.lineTo(7, -42);
    ctx.stroke();

    // Arms
    ctx.strokeStyle = '#d4a574';
    ctx.lineWidth = 5;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(-10, -30);
    ctx.lineTo(-16, -18);
    ctx.moveTo(10, -30);
    ctx.lineTo(16, -18);
    ctx.stroke();

    // Number plate
    ctx.fillStyle = '#fff';
    ctx.fillRect(-5, -25, 10, 8);
    ctx.fillStyle = '#222';
    ctx.font = 'bold 7px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(String(bike.id), 0, -19);

    ctx.restore();
  }

  /**
   * Draws the first-person cockpit: handlebars, hands, forearms, and the bike frame.
   * This creates the immersive "you are the rider" view.
   */
  drawCockpit(bike: FPBike) {
    const { ctx, width, height } = this;

    const cx = width / 2;
    const baseY = height - 20;

    // Shake offset at high speed or off-road
    const shakeX = bike.handlebarShake > 0 ? (Math.random() - 0.5) * bike.handlebarShake * 2 : 0;
    const shakeY = bike.handlebarShake > 0 ? (Math.random() - 0.5) * bike.handlebarShake : 0;

    // Bounce offset
    const bounceY = Math.sin(this.time * 0.3) * bike.bounce;

    // Lean offset (shift handlebars left/right when steering)
    const leanOffset = bike.lean * 40;

    // Airborne — handlebars rise up
    const airOffset = bike.airborne * 30;

    ctx.save();
    ctx.translate(cx + leanOffset + shakeX, baseY - bounceY - airOffset + shakeY);

    // --- BIKE FRAME (visible at bottom) ---
    const frameColor = this.shadeColor(bike.color, -20);
    const darkFrame = this.shadeColor(bike.color, -50);

    // Top tube / frame crossbar
    ctx.strokeStyle = frameColor;
    ctx.lineWidth = 12;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(-80, -30);
    ctx.lineTo(80, -30);
    ctx.stroke();

    // Frame tube down to steering
    ctx.beginPath();
    ctx.moveTo(-50, -30);
    ctx.lineTo(-30, 15);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(50, -30);
    ctx.lineTo(30, 15);
    ctx.stroke();

    // Fork
    ctx.strokeStyle = darkFrame;
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.moveTo(-40, -20);
    ctx.lineTo(-55, 30);
    ctx.moveTo(40, -20);
    ctx.lineTo(55, 30);
    ctx.stroke();

    // --- HANDLEBARS ---
    const barY = -45;
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 10;
    ctx.lineCap = 'round';

    // Main bar
    ctx.beginPath();
    ctx.moveTo(-70, barY + 5);
    ctx.lineTo(-30, barY);
    ctx.lineTo(30, barY);
    ctx.lineTo(70, barY + 5);
    ctx.stroke();

    // Bar supports
    ctx.lineWidth = 5;
    ctx.beginPath();
    ctx.moveTo(-35, barY + 2);
    ctx.lineTo(-35, -25);
    ctx.moveTo(35, barY + 2);
    ctx.lineTo(35, -25);
    ctx.stroke();

    // --- GRIPS ---
    const gripColor = '#1a1a1a';
    const gripHighlight = '#333';
    // Left grip
    ctx.fillStyle = gripColor;
    ctx.beginPath();
    ctx.roundRect ? ctx.roundRect(-78, barY, 18, 14, 4) : ctx.rect(-78, barY, 18, 14);
    ctx.fill();
    ctx.fillStyle = gripHighlight;
    ctx.fillRect(-75, barY + 2, 12, 3);
    // Right grip
    ctx.fillStyle = gripColor;
    ctx.beginPath();
    ctx.roundRect ? ctx.roundRect(60, barY, 18, 14, 4) : ctx.rect(60, barY, 18, 14);
    ctx.fill();
    ctx.fillStyle = gripHighlight;
    ctx.fillRect(63, barY + 2, 12, 3);

    // --- BRAKE LEVERS ---
    ctx.strokeStyle = '#555';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(-70, barY + 12);
    ctx.lineTo(-85, barY + 18);
    ctx.moveTo(70, barY + 12);
    ctx.lineTo(85, barY + 18);
    ctx.stroke();

    // --- HANDS ---
    const skinColor = '#d4a574';
    const skinShade = '#b89060';
    // Left hand gripping bar
    ctx.fillStyle = skinColor;
    ctx.beginPath();
    ctx.ellipse(-68, barY + 7, 12, 9, -0.2, 0, Math.PI * 2);
    ctx.fill();
    // Fingers wrapping
    ctx.fillStyle = skinShade;
    ctx.beginPath();
    ctx.ellipse(-72, barY + 10, 6, 4, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.ellipse(-65, barY + 11, 5, 3, 0, 0, Math.PI * 2);
    ctx.fill();

    // Right hand
    ctx.fillStyle = skinColor;
    ctx.beginPath();
    ctx.ellipse(68, barY + 7, 12, 9, 0.2, 0, Math.PI * 2);
    ctx.fill();
    ctx.fillStyle = skinShade;
    ctx.beginPath();
    ctx.ellipse(72, barY + 10, 6, 4, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.ellipse(65, barY + 11, 5, 3, 0, 0, Math.PI * 2);
    ctx.fill();

    // --- FOREARMS (coming from bottom of screen) ---
    const jerseyColor = this.shadeColor(bike.color, 25);
    const jerseyShade = this.shadeColor(bike.color, 5);
    ctx.strokeStyle = jerseyColor;
    ctx.lineWidth = 18;
    ctx.lineCap = 'round';

    // Left forearm
    ctx.beginPath();
    ctx.moveTo(-68, barY + 14);
    ctx.lineTo(-95, 50);
    ctx.stroke();
    // Right forearm
    ctx.beginPath();
    ctx.moveTo(68, barY + 14);
    ctx.lineTo(95, 50);
    ctx.stroke();

    // Forearm shading
    ctx.strokeStyle = jerseyShade;
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.moveTo(-68, barY + 16);
    ctx.lineTo(-92, 45);
    ctx.moveTo(68, barY + 16);
    ctx.lineTo(92, 45);
    ctx.stroke();

    // --- INSTRUMENT DISPLAY on handlebar center ---
    const displayY = barY - 18;
    ctx.fillStyle = '#0a0a1a';
    ctx.beginPath();
    ctx.roundRect ? ctx.roundRect(-22, displayY - 8, 44, 16, 3) : ctx.rect(-22, displayY - 8, 44, 16);
    ctx.fill();
    ctx.strokeStyle = '#333';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.roundRect ? ctx.roundRect(-22, displayY - 8, 44, 16, 3) : ctx.rect(-22, displayY - 8, 44, 16);
    ctx.stroke();

    // Digital speed display
    const speedKmh = Math.floor(bike.speed * 0.05);
    ctx.fillStyle = '#00ff66';
    ctx.font = 'bold 11px monospace';
    ctx.textAlign = 'center';
    ctx.fillText(`${speedKmh}`, -5, displayY + 2);
    ctx.fillStyle = '#666';
    ctx.font = '6px monospace';
    ctx.fillText('km/h', 12, displayY + 2);

    // Boost indicator on display
    if (bike.boostTimer > 0) {
      ctx.fillStyle = '#ff8800';
      ctx.fillRect(-20, displayY + 4, bike.boost * 40, 3);
    }

    ctx.restore();
  }

  drawWindEffect(bike: FPBike) {
    const { ctx, width, height } = this;
    const windStrength = bike.windEffect;
    if (windStrength < 0.3) return;

    const intensity = (windStrength - 0.3) * 1.5;
    ctx.strokeStyle = `rgba(255, 255, 255, ${intensity * 0.12})`;
    ctx.lineWidth = 1.5;

    // Wind streaks rushing past
    for (let i = 0; i < 30; i++) {
      const yBase = height * 0.4 + Math.random() * height * 0.4;
      const xStart = width * 0.5 + (Math.random() - 0.5) * width * 0.8;
      const streakLen = 40 + Math.random() * 80 * intensity;

      ctx.beginPath();
      ctx.moveTo(xStart, yBase);
      ctx.lineTo(xStart - streakLen, yBase + (Math.random() - 0.5) * 8);
      ctx.stroke();
    }

    // Edge blur at very high speed
    if (intensity > 0.8) {
      const edgeGrad = ctx.createLinearGradient(0, 0, 0, height);
      edgeGrad.addColorStop(0, 'rgba(255,255,255,0)');
      edgeGrad.addColorStop(0.4, `rgba(255,255,255,${intensity * 0.04})`);
      edgeGrad.addColorStop(0.6, `rgba(255,255,255,${intensity * 0.04})`);
      edgeGrad.addColorStop(1, 'rgba(255,255,255,0)');
      ctx.fillStyle = edgeGrad;
      ctx.fillRect(0, 0, width, height);
    }
  }

  drawSpeedFlash(bike: FPBike) {
    const { ctx, width, height } = this;
    if (bike.boostTimer <= 0) return;

    const intensity = Math.min(1, bike.boostTimer / 30);
    const grad = ctx.createRadialGradient(width / 2, height / 2, 100, width / 2, height / 2, width);
    grad.addColorStop(0, 'rgba(255, 100, 0, 0)');
    grad.addColorStop(0.7, 'rgba(255, 100, 0, 0)');
    grad.addColorStop(1, `rgba(255, 80, 0, ${intensity * 0.25})`);
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
  }

  drawParticles(particles: FPParticle[]) {
    const { ctx } = this;
    for (const p of particles) {
      const alpha = Math.max(0, p.life / p.maxLife);
      if (p.type === 'dust') {
        ctx.fillStyle = `rgba(140, 110, 70, ${alpha * 0.4})`;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size * alpha, 0, Math.PI * 2);
        ctx.fill();
      } else if (p.type === 'leaf') {
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.life * 0.08);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = alpha * 0.7;
        ctx.beginPath();
        ctx.ellipse(0, 0, p.size, p.size * 0.4, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.restore();
        ctx.globalAlpha = 1;
      } else if (p.type === 'spark') {
        ctx.fillStyle = `rgba(255, ${150 + Math.random() * 100}, 0, ${alpha})`;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size, 0, Math.PI * 2);
        ctx.fill();
      } else if (p.type === 'confetti') {
        ctx.save();
        ctx.translate(p.x, p.y);
        ctx.rotate(p.life * 0.1);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = alpha;
        ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
        ctx.restore();
        ctx.globalAlpha = 1;
      }
    }
  }

  drawVignette() {
    const { ctx, width, height } = this;
    const grad = ctx.createRadialGradient(width / 2, height / 2, height * 0.3, width / 2, height / 2, height * 0.8);
    grad.addColorStop(0, 'rgba(0,0,0,0)');
    grad.addColorStop(1, 'rgba(0,0,0,0.35)');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
  }

  drawAirborneEffect(bike: FPBike) {
    if (bike.airborne < 0.1) return;
    const { ctx, width, height } = this;
    const intensity = bike.airborne;
    ctx.fillStyle = `rgba(180, 220, 255, ${intensity * 0.1})`;
    ctx.fillRect(0, 0, width, height);

    // "AIR" text
    ctx.fillStyle = `rgba(100, 200, 255, ${intensity * 0.8})`;
    ctx.font = 'bold 24px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('AIR TIME!', width / 2, height * 0.3);
  }
}
