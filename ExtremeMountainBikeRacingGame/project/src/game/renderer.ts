import type { Bike, Particle, CloudParticle } from './types';
import { Terrain } from './terrain';

const WHEEL_RADIUS = 16;
const BIKE_LENGTH = 52;

export class Renderer {
  ctx: CanvasRenderingContext2D;
  width: number;
  height: number;
  clouds: CloudParticle[] = [];
  mountainsBack: { x: number; y: number; w: number; h: number; color: string }[] = [];
  mountainsMid: { x: number; y: number; w: number; h: number; color: string }[] = [];
  trees: { x: number; scale: number; type: number }[] = [];

  constructor(ctx: CanvasRenderingContext2D, width: number, height: number) {
    this.ctx = ctx;
    this.width = width;
    this.height = height;
    this.initBackground();
  }

  resize(width: number, height: number) {
    this.width = width;
    this.height = height;
  }

  initBackground() {
    // Clouds
    this.clouds = [];
    for (let i = 0; i < 15; i++) {
      this.clouds.push({
        x: Math.random() * 3000,
        y: 50 + Math.random() * 200,
        scale: 0.5 + Math.random() * 1.2,
        speed: 0.2 + Math.random() * 0.4,
      });
    }

    // Background mountains (far)
    this.mountainsBack = [];
    for (let i = 0; i < 30; i++) {
      const w = 300 + Math.random() * 400;
      const h = 200 + Math.random() * 300;
      this.mountainsBack.push({
        x: i * 250 + Math.random() * 100,
        y: 400 + Math.random() * 100,
        w,
        h,
        color: this.shadeColor('#3a4a5a', -10 + Math.random() * 20),
      });
    }

    // Mid mountains
    this.mountainsMid = [];
    for (let i = 0; i < 25; i++) {
      const w = 250 + Math.random() * 350;
      const h = 150 + Math.random() * 200;
      this.mountainsMid.push({
        x: i * 300 + Math.random() * 150,
        y: 450 + Math.random() * 80,
        w,
        h,
        color: this.shadeColor('#2d3a2d', -10 + Math.random() * 20),
      });
    }

    // Trees
    this.trees = [];
    for (let i = 0; i < 60; i++) {
      this.trees.push({
        x: Math.random() * 8000,
        scale: 0.6 + Math.random() * 0.8,
        type: Math.floor(Math.random() * 3),
      });
    }
  }

  shadeColor(color: string, percent: number): string {
    const num = parseInt(color.replace('#', ''), 16);
    const r = Math.min(255, Math.max(0, (num >> 16) + percent));
    const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00ff) + percent));
    const b = Math.min(255, Math.max(0, (num & 0x0000ff) + percent));
    return `rgb(${r},${g},${b})`;
  }

  drawSky() {
    const { ctx, width, height } = this;
    const gradient = ctx.createLinearGradient(0, 0, 0, height);
    gradient.addColorStop(0, '#4a7ba6');
    gradient.addColorStop(0.3, '#7ba8c8');
    gradient.addColorStop(0.6, '#a8c8d8');
    gradient.addColorStop(1, '#d8e4ec');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, width, height);
  }

  drawSun(cameraX: number) {
    const { ctx, width } = this;
    const sunX = width * 0.75 - cameraX * 0.02;
    const sunY = 90;
    // Sun glow
    const glowGrad = ctx.createRadialGradient(sunX, sunY, 0, sunX, sunY, 120);
    glowGrad.addColorStop(0, 'rgba(255, 240, 200, 0.6)');
    glowGrad.addColorStop(0.5, 'rgba(255, 220, 150, 0.2)');
    glowGrad.addColorStop(1, 'rgba(255, 200, 100, 0)');
    ctx.fillStyle = glowGrad;
    ctx.beginPath();
    ctx.arc(sunX, sunY, 120, 0, Math.PI * 2);
    ctx.fill();
    // Sun body
    ctx.fillStyle = '#fff8e0';
    ctx.beginPath();
    ctx.arc(sunX, sunY, 35, 0, Math.PI * 2);
    ctx.fill();
  }

  drawClouds(cameraX: number) {
    const { ctx, width } = this;
    for (const cloud of this.clouds) {
      const x = ((cloud.x - cameraX * 0.15) % (width + 400) + width + 400) % (width + 400) - 200;
      const y = cloud.y;
      const s = cloud.scale;
      ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
      ctx.beginPath();
      ctx.arc(x, y, 25 * s, 0, Math.PI * 2);
      ctx.arc(x + 25 * s, y - 10 * s, 30 * s, 0, Math.PI * 2);
      ctx.arc(x + 55 * s, y, 25 * s, 0, Math.PI * 2);
      ctx.arc(x + 30 * s, y + 8 * s, 22 * s, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  drawMountains(cameraX: number) {
    const { ctx, width, height } = this;

    // Far mountains (parallax 0.2)
    ctx.fillStyle = '#3a4a5a';
    for (const m of this.mountainsBack) {
      const x = m.x - cameraX * 0.2;
      if (x + m.w < -100 || x > width + 100) continue;
      const baseY = height * 0.5;
      ctx.fillStyle = m.color;
      ctx.beginPath();
      ctx.moveTo(x, baseY);
      ctx.lineTo(x + m.w * 0.3, baseY - m.h * 0.7);
      ctx.lineTo(x + m.w * 0.5, baseY - m.h);
      ctx.lineTo(x + m.w * 0.7, baseY - m.h * 0.6);
      ctx.lineTo(x + m.w, baseY);
      ctx.closePath();
      ctx.fill();
      // Snow cap
      ctx.fillStyle = 'rgba(240, 245, 250, 0.7)';
      ctx.beginPath();
      ctx.moveTo(x + m.w * 0.42, baseY - m.h * 0.85);
      ctx.lineTo(x + m.w * 0.5, baseY - m.h);
      ctx.lineTo(x + m.w * 0.58, baseY - m.h * 0.85);
      ctx.lineTo(x + m.w * 0.55, baseY - m.h * 0.8);
      ctx.lineTo(x + m.w * 0.5, baseY - m.h * 0.82);
      ctx.lineTo(x + m.w * 0.45, baseY - m.h * 0.8);
      ctx.closePath();
      ctx.fill();
    }

    // Mid mountains (parallax 0.4)
    for (const m of this.mountainsMid) {
      const x = m.x - cameraX * 0.4;
      if (x + m.w < -100 || x > width + 100) continue;
      const baseY = height * 0.55;
      ctx.fillStyle = m.color;
      ctx.beginPath();
      ctx.moveTo(x, baseY);
      ctx.lineTo(x + m.w * 0.35, baseY - m.h * 0.6);
      ctx.lineTo(x + m.w * 0.5, baseY - m.h);
      ctx.lineTo(x + m.w * 0.65, baseY - m.h * 0.5);
      ctx.lineTo(x + m.w, baseY);
      ctx.closePath();
      ctx.fill();
    }
  }

  drawTrees(cameraX: number, groundY: number) {
    const { ctx } = this;
    for (const tree of this.trees) {
      const x = tree.x - cameraX * 0.6;
      if (x < -100 || x > this.width + 100) continue;
      const y = groundY + 30;
      const s = tree.scale;
      // Trunk
      ctx.fillStyle = '#3d2817';
      ctx.fillRect(x - 3 * s, y - 20 * s, 6 * s, 25 * s);
      // Foliage
      const greens = ['#1a3a1a', '#2d4a2d', '#3a5a3a'];
      ctx.fillStyle = greens[tree.type];
      ctx.beginPath();
      ctx.moveTo(x, y - 60 * s);
      ctx.lineTo(x - 20 * s, y - 25 * s);
      ctx.lineTo(x - 15 * s, y - 25 * s);
      ctx.lineTo(x - 25 * s, y - 5 * s);
      ctx.lineTo(x + 25 * s, y - 5 * s);
      ctx.lineTo(x + 15 * s, y - 25 * s);
      ctx.lineTo(x + 20 * s, y - 25 * s);
      ctx.closePath();
      ctx.fill();
    }
  }

  drawTerrain(terrain: Terrain, cameraX: number, cameraY: number, viewWidth: number, viewHeight: number) {
    const { ctx } = this;
    const segW = terrain.segmentWidth;
    const startIdx = Math.max(0, Math.floor((cameraX - 50) / segW));
    const endIdx = Math.min(terrain.points.length - 1, Math.ceil((cameraX + viewWidth + 50) / segW));

    // Fill below terrain surface
    ctx.beginPath();
    ctx.moveTo(terrain.points[startIdx].x - cameraX, viewHeight);
    for (let i = startIdx; i <= endIdx; i++) {
      ctx.lineTo(terrain.points[i].x - cameraX, terrain.points[i].y - cameraY);
    }
    ctx.lineTo(terrain.points[endIdx].x - cameraX, viewHeight);
    ctx.closePath();

    // Gradient fill for ground
    const groundGrad = ctx.createLinearGradient(0, 300 - cameraY, 0, viewHeight);
    groundGrad.addColorStop(0, '#4a6b3a');
    groundGrad.addColorStop(0.3, '#3a5a2a');
    groundGrad.addColorStop(0.6, '#2d4520');
    groundGrad.addColorStop(1, '#1a2d12');
    ctx.fillStyle = groundGrad;
    ctx.fill();

    // Terrain surface line (dirt trail)
    ctx.beginPath();
    ctx.moveTo(terrain.points[startIdx].x - cameraX, terrain.points[startIdx].y - cameraY);
    for (let i = startIdx; i <= endIdx; i++) {
      ctx.lineTo(terrain.points[i].x - cameraX, terrain.points[i].y - cameraY);
    }
    ctx.strokeStyle = '#6b4a2a';
    ctx.lineWidth = 5;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.stroke();

    // Lighter trail line
    ctx.beginPath();
    ctx.moveTo(terrain.points[startIdx].x - cameraX, terrain.points[startIdx].y - cameraY + 2);
    for (let i = startIdx; i <= endIdx; i++) {
      ctx.lineTo(terrain.points[i].x - cameraX, terrain.points[i].y - cameraY + 2);
    }
    ctx.strokeStyle = '#8b6a4a';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Grass tufts on terrain
    ctx.fillStyle = '#3a5a2a';
    for (let i = startIdx; i <= endIdx; i += 3) {
      const px = terrain.points[i].x - cameraX;
      const py = terrain.points[i].y - cameraY;
      ctx.fillRect(px, py - 3, 2, 4);
      ctx.fillRect(px + 3, py - 2, 2, 3);
    }
  }

  drawBike(bike: Bike, cameraX: number, cameraY: number) {
    const { ctx } = this;
    const bx = bike.x - cameraX;
    const by = bike.y - cameraY;

    ctx.save();
    ctx.translate(bx, by);
    ctx.rotate(bike.tilt);

    const mainColor = bike.color;
    const darkColor = this.shadeColor(mainColor, -40);
    const frameColor = this.shadeColor(mainColor, -20);

    // Shadow on ground
    if (bike.onGround) {
      ctx.save();
      ctx.rotate(-bike.tilt);
      ctx.fillStyle = 'rgba(0,0,0,0.2)';
      ctx.beginPath();
      ctx.ellipse(0, WHEEL_RADIUS + 4, BIKE_LENGTH * 0.4, 5, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    // Rear wheel
    this.drawWheel(-BIKE_LENGTH * 0.35, WHEEL_RADIUS, bike.wheelRotation);
    // Front wheel
    this.drawWheel(BIKE_LENGTH * 0.35, WHEEL_RADIUS, bike.wheelRotation);

    // Frame
    ctx.strokeStyle = frameColor;
    ctx.lineWidth = 5;
    ctx.lineCap = 'round';

    // Main triangle frame
    ctx.beginPath();
    ctx.moveTo(-BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.lineTo(-5, WHEEL_RADIUS - 15);
    ctx.lineTo(5, WHEEL_RADIUS - 28);
    ctx.lineTo(-10, WHEEL_RADIUS - 8);
    ctx.lineTo(-BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.stroke();

    // Top tube to seat
    ctx.beginPath();
    ctx.moveTo(5, WHEEL_RADIUS - 28);
    ctx.lineTo(BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.stroke();

    // Down tube to front
    ctx.beginPath();
    ctx.moveTo(-5, WHEEL_RADIUS - 15);
    ctx.lineTo(BIKE_LENGTH * 0.2, WHEEL_RADIUS);
    ctx.stroke();

    // Fork
    ctx.beginPath();
    ctx.moveTo(BIKE_LENGTH * 0.2, WHEEL_RADIUS - 5);
    ctx.lineTo(BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.stroke();

    // Handlebar
    ctx.beginPath();
    ctx.moveTo(BIKE_LENGTH * 0.2, WHEEL_RADIUS - 5);
    ctx.lineTo(BIKE_LENGTH * 0.22, WHEEL_RADIUS - 22);
    ctx.lineWidth = 4;
    ctx.stroke();

    // Handlebar grips
    ctx.fillStyle = '#222';
    ctx.beginPath();
    ctx.arc(BIKE_LENGTH * 0.22, WHEEL_RADIUS - 22, 4, 0, Math.PI * 2);
    ctx.fill();

    // Seat
    ctx.fillStyle = darkColor;
    ctx.beginPath();
    ctx.ellipse(-2, WHEEL_RADIUS - 30, 10, 4, 0, 0, Math.PI * 2);
    ctx.fill();

    // Pedals
    this.drawPedals(bike.pedalPhase, WHEEL_RADIUS - 8);

    // Rider
    if (!bike.crashed) {
      this.drawRider(bike, WHEEL_RADIUS, mainColor);
    } else {
      this.drawCrashedRider(WHEEL_RADIUS, mainColor);
    }

    // Boost flame effect
    if (bike.boostTimer > 0) {
      this.drawBoostFlame(bike);
    }

    ctx.restore();
  }

  drawWheel(wx: number, wy: number, rotation: number) {
    const { ctx } = this;
    // Tire
    ctx.fillStyle = '#1a1a1a';
    ctx.beginPath();
    ctx.arc(wx, wy, WHEEL_RADIUS, 0, Math.PI * 2);
    ctx.fill();
    // Rim
    ctx.strokeStyle = '#555';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(wx, wy, WHEEL_RADIUS - 4, 0, Math.PI * 2);
    ctx.stroke();
    // Spokes
    ctx.save();
    ctx.translate(wx, wy);
    ctx.rotate(rotation);
    ctx.strokeStyle = '#888';
    ctx.lineWidth = 1.5;
    for (let i = 0; i < 6; i++) {
      ctx.rotate(Math.PI / 3);
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.lineTo(WHEEL_RADIUS - 5, 0);
      ctx.stroke();
    }
    ctx.restore();
    // Hub
    ctx.fillStyle = '#666';
    ctx.beginPath();
    ctx.arc(wx, wy, 4, 0, Math.PI * 2);
    ctx.fill();
  }

  drawPedals(phase: number, cy: number) {
    const { ctx } = this;
    const crankRadius = 8;
    const pedal1X = Math.cos(phase) * crankRadius;
    const pedal1Y = cy + Math.sin(phase) * crankRadius;
    const pedal2X = Math.cos(phase + Math.PI) * crankRadius;
    const pedal2Y = cy + Math.sin(phase + Math.PI) * crankRadius;

    // Crank arm
    ctx.strokeStyle = '#444';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(pedal1X, pedal1Y);
    ctx.lineTo(pedal2X, pedal2Y);
    ctx.stroke();

    // Pedals
    ctx.fillStyle = '#333';
    ctx.save();
    ctx.translate(pedal1X, pedal1Y);
    ctx.fillRect(-4, -2, 8, 4);
    ctx.restore();
    ctx.save();
    ctx.translate(pedal2X, pedal2Y);
    ctx.fillRect(-4, -2, 8, 4);
    ctx.restore();

    // Chainring
    ctx.strokeStyle = '#777';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(0, cy, 6, 0, Math.PI * 2);
    ctx.stroke();
  }

  drawRider(bike: Bike, wheelY: number, color: string) {
    const { ctx } = this;
    const helmetColor = color;
    const jerseyColor = this.shadeColor(color, 20);
    const skinColor = '#d4a574';
    const pantsColor = '#2a2a3a';

    const seatY = wheelY - 30;
    const handleY = wheelY - 22;

    // Determine lean based on acceleration
    const leanOffset = bike.vx > 0 ? -3 : 0;

    // Legs (pedaling animation)
    const legPhase = bike.pedalPhase;
    const hipX = -3;
    const hipY = seatY - 2;

    // Back leg
    const backKneeX = hipX + Math.cos(legPhase + Math.PI) * 8 + leanOffset;
    const backKneeY = hipY + 8 + Math.abs(Math.sin(legPhase + Math.PI)) * 4;
    const backFootX = hipX + Math.cos(legPhase + Math.PI) * 14;
    const backFootY = wheelY - 8 + Math.sin(legPhase + Math.PI) * 6;

    // Front leg
    const frontKneeX = hipX + Math.cos(legPhase) * 8 + leanOffset;
    const frontKneeY = hipY + 8 + Math.abs(Math.sin(legPhase)) * 4;
    const frontFootX = hipX + Math.cos(legPhase) * 14;
    const frontFootY = wheelY - 8 + Math.sin(legPhase) * 6;

    // Pants - back leg
    ctx.strokeStyle = pantsColor;
    ctx.lineWidth = 7;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(backKneeX, backKneeY);
    ctx.lineTo(backFootX, backFootY);
    ctx.stroke();

    // Pants - front leg
    ctx.strokeStyle = this.shadeColor(pantsColor, 15);
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(frontKneeX, frontKneeY);
    ctx.lineTo(frontFootX, frontFootY);
    ctx.stroke();

    // Torso
    const torsoTopX = 5 + leanOffset;
    const torsoTopY = seatY - 18;
    ctx.strokeStyle = jerseyColor;
    ctx.lineWidth = 10;
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(torsoTopX, torsoTopY);
    ctx.stroke();

    // Arms reaching to handlebar
    const shoulderX = torsoTopX;
    const shoulderY = torsoTopY;
    const handX = BIKE_LENGTH * 0.22;
    const handY = handleY;

    ctx.strokeStyle = skinColor;
    ctx.lineWidth = 5;
    ctx.beginPath();
    ctx.moveTo(shoulderX, shoulderY);
    ctx.lineTo(shoulderX + 8, shoulderY + 5);
    ctx.lineTo(handX, handY);
    ctx.stroke();

    // Gloves
    ctx.fillStyle = '#222';
    ctx.beginPath();
    ctx.arc(handX, handY, 3.5, 0, Math.PI * 2);
    ctx.fill();

    // Head with helmet
    const headX = torsoTopX + 4;
    const headY = torsoTopY - 8;

    // Neck
    ctx.strokeStyle = skinColor;
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(torsoTopX, torsoTopY);
    ctx.lineTo(headX - 2, headY + 2);
    ctx.stroke();

    // Helmet
    ctx.fillStyle = helmetColor;
    ctx.beginPath();
    ctx.arc(headX, headY, 9, Math.PI, Math.PI * 2);
    ctx.lineTo(headX + 9, headY + 3);
    ctx.lineTo(headX - 9, headY + 3);
    ctx.closePath();
    ctx.fill();
    // Helmet visor
    ctx.fillStyle = this.shadeColor(helmetColor, -30);
    ctx.beginPath();
    ctx.moveTo(headX - 9, headY + 1);
    ctx.lineTo(headX - 12, headY + 4);
    ctx.lineTo(headX - 9, headY + 4);
    ctx.closePath();
    ctx.fill();
    // Helmet stripe
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(headX - 6, headY - 5);
    ctx.lineTo(headX + 6, headY - 5);
    ctx.stroke();

    // Face (small visible part)
    ctx.fillStyle = skinColor;
    ctx.fillRect(headX - 7, headY + 2, 10, 4);

    // Number plate on back
    ctx.fillStyle = '#fff';
    ctx.fillRect(hipX - 6, hipY - 6, 5, 8);
    ctx.fillStyle = '#222';
    ctx.font = 'bold 6px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(String(bike.id), hipX - 3.5, hipY);
  }

  drawCrashedRider(wheelY: number, color: string) {
    const { ctx } = this;
    // Tumbled rider
    ctx.save();
    ctx.translate(-20, wheelY - 40);
    ctx.rotate(Math.random() * 0.3 - 0.15);

    // Body
    ctx.strokeStyle = this.shadeColor(color, 20);
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(15, 5);
    ctx.stroke();

    // Head
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(0, 0, 7, 0, Math.PI * 2);
    ctx.fill();

    // Arms spread
    ctx.strokeStyle = '#d4a574';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(5, 2);
    ctx.lineTo(0, 12);
    ctx.moveTo(10, 4);
    ctx.lineTo(18, -5);
    ctx.stroke();

    // Legs
    ctx.strokeStyle = '#2a2a3a';
    ctx.lineWidth = 6;
    ctx.beginPath();
    ctx.moveTo(15, 5);
    ctx.lineTo(25, 0);
    ctx.lineTo(22, 15);
    ctx.stroke();

    ctx.restore();
  }

  drawBoostFlame(bike: Bike) {
    const { ctx } = this;
    const flameLength = 15 + Math.random() * 10;
    const fx = -BIKE_LENGTH * 0.4;

    // Outer flame
    const grad = ctx.createLinearGradient(fx, 0, fx - flameLength, 0);
    grad.addColorStop(0, 'rgba(255, 100, 0, 0.8)');
    grad.addColorStop(0.5, 'rgba(255, 180, 0, 0.5)');
    grad.addColorStop(1, 'rgba(255, 255, 100, 0)');
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.moveTo(fx, -8);
    ctx.lineTo(fx - flameLength, 0);
    ctx.lineTo(fx, 8);
    ctx.closePath();
    ctx.fill();

    // Inner flame
    ctx.fillStyle = 'rgba(255, 255, 200, 0.6)';
    ctx.beginPath();
    ctx.moveTo(fx, -4);
    ctx.lineTo(fx - flameLength * 0.5, 0);
    ctx.lineTo(fx, 4);
    ctx.closePath();
    ctx.fill();
  }

  drawParticles(particles: Particle[], cameraX: number, cameraY: number) {
    const { ctx } = this;
    for (const p of particles) {
      const px = p.x - cameraX;
      const py = p.y - cameraY;
      const alpha = Math.max(0, p.life / p.maxLife);

      if (p.type === 'dust') {
        ctx.fillStyle = `rgba(160, 130, 90, ${alpha * 0.6})`;
        ctx.beginPath();
        ctx.arc(px, py, p.size, 0, Math.PI * 2);
        ctx.fill();
      } else if (p.type === 'spark') {
        ctx.fillStyle = `rgba(255, ${150 + Math.random() * 100}, 0, ${alpha})`;
        ctx.beginPath();
        ctx.arc(px, py, p.size, 0, Math.PI * 2);
        ctx.fill();
      } else if (p.type === 'crash') {
        ctx.save();
        ctx.translate(px, py);
        ctx.rotate(p.rotation);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = alpha;
        ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size);
        ctx.restore();
        ctx.globalAlpha = 1;
      } else if (p.type === 'confetti') {
        ctx.save();
        ctx.translate(px, py);
        ctx.rotate(p.rotation);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = alpha;
        ctx.fillRect(-p.size / 2, -p.size / 4, p.size, p.size / 2);
        ctx.restore();
        ctx.globalAlpha = 1;
      }
    }
  }

  drawFinishLine(terrain: Terrain, cameraX: number, cameraY: number, trackLength: number) {
    const { ctx, height } = this;
    const fx = trackLength - cameraX;
    const fy = terrain.getHeightAt(trackLength) - cameraY;

    // Checkered pattern flag
    const flagW = 60;
    const flagH = 40;
    const poleH = 80;

    // Pole
    ctx.fillStyle = '#333';
    ctx.fillRect(fx - 2, fy - poleH, 4, poleH);

    // Flag
    for (let row = 0; row < 4; row++) {
      for (let col = 0; col < 6; col++) {
        ctx.fillStyle = (row + col) % 2 === 0 ? '#000' : '#fff';
        ctx.fillRect(fx + col * 10, fy - poleH + row * 10, 10, 10);
      }
    }

    // Banner
    ctx.fillStyle = 'rgba(255, 215, 0, 0.9)';
    ctx.fillRect(fx - 40, fy - poleH - 25, 80, 20);
    ctx.fillStyle = '#000';
    ctx.font = 'bold 14px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('FINISH', fx, fy - poleH - 10);
  }

  drawStartLine(terrain: Terrain, cameraX: number, cameraY: number) {
    const { ctx } = this;
    const sx = 50 - cameraX;
    const sy = terrain.getHeightAt(50) - cameraY;

    // Gate posts
    ctx.fillStyle = '#e74c3c';
    ctx.fillRect(sx - 3, sy - 60, 6, 60);
    ctx.fillRect(sx + 30, sy - 60, 6, 60);

    // Banner
    ctx.fillStyle = '#e74c3c';
    ctx.fillRect(sx - 3, sy - 60, 39, 15);
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 10px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('START', sx + 17, sy - 49);
  }

  updateClouds(dt: number) {
    for (const cloud of this.clouds) {
      cloud.x += cloud.speed * dt;
    }
  }
}
