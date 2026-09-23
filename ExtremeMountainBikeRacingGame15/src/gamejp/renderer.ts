import type { JPBike, JPParticle, JPSakura, JPBuilding } from './types';
import { JPTerrain } from './terrain';

const WHEEL_RADIUS = 16;
const BIKE_LENGTH = 52;

export class JPRenderer {
  ctx: CanvasRenderingContext2D;
  width: number;
  height: number;
  sakura: JPSakura[] = [];
  buildings: JPBuilding[] = [];
  bambooPositions: { x: number; scale: number }[] = [];
  time: number = 0;

  constructor(ctx: CanvasRenderingContext2D, width: number, height: number) {
    this.ctx = ctx;
    this.width = width;
    this.height = height;
    this.initScenery();
  }

  resize(width: number, height: number) {
    this.width = width;
    this.height = height;
  }

  initScenery() {
    // Falling cherry blossom petals
    this.sakura = [];
    for (let i = 0; i < 40; i++) {
      this.sakura.push({
        x: Math.random() * 3000,
        y: Math.random() * 800,
        vx: -0.5 + Math.random() * 0.3,
        vy: 0.3 + Math.random() * 0.5,
        rotation: Math.random() * Math.PI * 2,
        rotationSpeed: (Math.random() - 0.5) * 0.04,
        size: 3 + Math.random() * 4,
        petalCount: 5,
      });
    }

    // Japanese buildings along the track
    this.buildings = [];
    const types: JPBuilding['type'][] = ['pagoda', 'torii', 'shrine', 'temple', 'torii', 'pagoda'];
    for (let i = 0; i < 30; i++) {
      this.buildings.push({
        x: 200 + i * 250 + Math.random() * 100,
        type: types[i % types.length],
        scale: 0.6 + Math.random() * 0.6,
      });
    }

    // Bamboo groves
    this.bambooPositions = [];
    for (let i = 0; i < 50; i++) {
      this.bambooPositions.push({
        x: Math.random() * 7000,
        scale: 0.5 + Math.random() * 0.8,
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

  drawSky(cameraX: number) {
    const { ctx, width, height } = this;
    // Japanese dawn sky — soft pink to blue
    const grad = ctx.createLinearGradient(0, 0, 0, height);
    grad.addColorStop(0, '#d4a5c9');
    grad.addColorStop(0.15, '#e8c4d8');
    grad.addColorStop(0.35, '#f0d8e0');
    grad.addColorStop(0.5, '#c8d8e8');
    grad.addColorStop(0.7, '#a8c8d8');
    grad.addColorStop(1, '#88b8c8');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height);
  }

  drawSun(cameraX: number) {
    const { ctx, width } = this;
    const sunX = width * 0.7 - cameraX * 0.03;
    const sunY = 100;
    // Japanese red sun
    const glow = ctx.createRadialGradient(sunX, sunY, 0, sunX, sunY, 100);
    glow.addColorStop(0, 'rgba(255, 80, 80, 0.4)');
    glow.addColorStop(0.5, 'rgba(255, 120, 100, 0.15)');
    glow.addColorStop(1, 'rgba(255, 150, 100, 0)');
    ctx.fillStyle = glow;
    ctx.beginPath();
    ctx.arc(sunX, sunY, 100, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#e63946';
    ctx.beginPath();
    ctx.arc(sunX, sunY, 38, 0, Math.PI * 2);
    ctx.fill();
  }

  drawMtFuji(cameraX: number) {
    const { ctx, width, height } = this;
    // Mt. Fuji in the background (parallax 0.15)
    const fujiX = width * 0.4 - cameraX * 0.15;
    const fujiBaseY = height * 0.52;
    const fujiW = 400;
    const fujiH = 280;

    // Mountain body
    ctx.fillStyle = '#5a6a8a';
    ctx.beginPath();
    ctx.moveTo(fujiX - fujiW, fujiBaseY);
    ctx.lineTo(fujiX - fujiW * 0.3, fujiBaseY - fujiH * 0.5);
    ctx.lineTo(fujiX, fujiBaseY - fujiH);
    ctx.lineTo(fujiX + fujiW * 0.3, fujiBaseY - fujiH * 0.55);
    ctx.lineTo(fujiX + fujiW, fujiBaseY);
    ctx.closePath();
    ctx.fill();

    // Snow cap
    ctx.fillStyle = '#f0f0f5';
    ctx.beginPath();
    ctx.moveTo(fujiX - fujiW * 0.22, fujiBaseY - fujiH * 0.78);
    ctx.lineTo(fujiX - fujiW * 0.15, fujiBaseY - fujiH * 0.7);
    ctx.lineTo(fujiX - fujiW * 0.08, fujiBaseY - fujiH * 0.75);
    ctx.lineTo(fujiX, fujiBaseY - fujiH);
    ctx.lineTo(fujiX + fujiW * 0.08, fujiBaseY - fujiH * 0.78);
    ctx.lineTo(fujiX + fujiW * 0.15, fujiBaseY - fujiH * 0.72);
    ctx.lineTo(fujiX + fujiW * 0.22, fujiBaseY - fujiH * 0.8);
    ctx.lineTo(fujiX + fujiW * 0.18, fujiBaseY - fujiH * 0.74);
    ctx.lineTo(fujiX + fujiW * 0.1, fujiBaseY - fujiH * 0.76);
    ctx.lineTo(fujiX, fujiBaseY - fujiH * 0.82);
    ctx.lineTo(fujiX - fujiW * 0.1, fujiBaseY - fujiH * 0.76);
    ctx.lineTo(fujiX - fujiW * 0.18, fujiBaseY - fujiH * 0.74);
    ctx.closePath();
    ctx.fill();

    // Second smaller peak
    ctx.fillStyle = '#4a5a7a';
    ctx.beginPath();
    ctx.moveTo(fujiX + fujiW * 0.8, fujiBaseY);
    ctx.lineTo(fujiX + fujiW * 1.2, fujiBaseY - fujiH * 0.5);
    ctx.lineTo(fujiX + fujiW * 1.5, fujiBaseY - fujiH * 0.35);
    ctx.lineTo(fujiX + fujiW * 1.8, fujiBaseY);
    ctx.closePath();
    ctx.fill();

    // Distant green mountains
    ctx.fillStyle = '#3a5a4a';
    ctx.beginPath();
    ctx.moveTo(0, fujiBaseY);
    for (let x = 0; x <= width; x += 50) {
      const h = 60 + Math.sin(x * 0.01 + cameraX * 0.001) * 30;
      ctx.lineTo(x, fujiBaseY - h);
    }
    ctx.lineTo(width, fujiBaseY);
    ctx.closePath();
    ctx.fill();
  }

  drawHills(cameraX: number) {
    const { ctx, width, height } = this;
    // Mid-layer hills with green forests
    ctx.fillStyle = '#2d4a3d';
    ctx.beginPath();
    ctx.moveTo(0, height * 0.58);
    for (let x = 0; x <= width; x += 40) {
      const wx = x + cameraX * 0.3;
      const h = 50 + Math.sin(wx * 0.008) * 30 + Math.sin(wx * 0.02) * 15;
      ctx.lineTo(x, height * 0.58 - h);
    }
    ctx.lineTo(width, height * 0.58);
    ctx.closePath();
    ctx.fill();
  }

  drawSakuraTrees(cameraX: number) {
    const { ctx } = this;
    // Cherry blossom trees in mid-background (parallax 0.5)
    for (let i = 0; i < 25; i++) {
      const treeX = i * 280 - cameraX * 0.5;
      if (treeX < -150 || treeX > this.width + 150) continue;
      const treeY = this.height * 0.6;
      const s = 0.8 + (i % 3) * 0.2;

      // Trunk
      ctx.fillStyle = '#3d2817';
      ctx.fillRect(treeX - 4 * s, treeY - 30 * s, 8 * s, 35 * s);

      // Branches
      ctx.strokeStyle = '#3d2817';
      ctx.lineWidth = 3 * s;
      ctx.beginPath();
      ctx.moveTo(treeX, treeY - 25 * s);
      ctx.lineTo(treeX - 20 * s, treeY - 45 * s);
      ctx.moveTo(treeX, treeY - 20 * s);
      ctx.lineTo(treeX + 18 * s, treeY - 40 * s);
      ctx.stroke();

      // Blossom canopy (pink clusters)
      const pink1 = '#ffb7c5';
      const pink2 = '#ffc9d6';
      const pink3 = '#ffa5b8';

      ctx.fillStyle = pink1;
      ctx.beginPath();
      ctx.arc(treeX, treeY - 50 * s, 28 * s, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = pink2;
      ctx.beginPath();
      ctx.arc(treeX - 18 * s, treeY - 45 * s, 22 * s, 0, Math.PI * 2);
      ctx.arc(treeX + 18 * s, treeY - 42 * s, 24 * s, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = pink3;
      ctx.beginPath();
      ctx.arc(treeX - 8 * s, treeY - 60 * s, 18 * s, 0, Math.PI * 2);
      ctx.arc(treeX + 10 * s, treeY - 55 * s, 16 * s, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  drawBamboo(cameraX: number) {
    const { ctx } = this;
    for (const bamboo of this.bambooPositions) {
      const x = bamboo.x - cameraX * 0.6;
      if (x < -50 || x > this.width + 50) continue;
      const y = this.height * 0.62;
      const s = bamboo.scale;
      // Bamboo stalk
      ctx.fillStyle = '#4a7a3a';
      ctx.fillRect(x - 3 * s, y - 80 * s, 6 * s, 80 * s);
      // Segments
      ctx.strokeStyle = '#3a6a2a';
      ctx.lineWidth = 1.5;
      for (let seg = 0; seg < 5; seg++) {
        ctx.beginPath();
        ctx.moveTo(x - 3 * s, y - seg * 16 * s);
        ctx.lineTo(x + 3 * s, y - seg * 16 * s);
        ctx.stroke();
      }
      // Leaves
      ctx.fillStyle = '#5a8a4a';
      for (let l = 0; l < 4; l++) {
        const ly = y - 60 * s - l * 12 * s;
        ctx.beginPath();
        ctx.ellipse(x - 10 * s, ly, 8 * s, 3 * s, -0.4, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.ellipse(x + 10 * s, ly, 8 * s, 3 * s, 0.4, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  }

  drawBuildings(cameraX: number) {
    const { ctx } = this;
    for (const b of this.buildings) {
      const x = b.x - cameraX * 0.6;
      if (x < -200 || x > this.width + 200) continue;
      const y = this.height * 0.64;
      const s = b.scale;

      switch (b.type) {
        case 'pagoda':
          this.drawPagoda(x, y, s);
          break;
        case 'torii':
          this.drawTorii(x, y, s);
          break;
        case 'shrine':
          this.drawShrine(x, y, s);
          break;
        case 'temple':
          this.drawTemple(x, y, s);
          break;
      }
    }
  }

  drawPagoda(x: number, y: number, s: number) {
    const { ctx } = this;
    // 5-tier pagoda
    const tiers = 5;
    const baseW = 50 * s;
    const tierH = 20 * s;

    ctx.fillStyle = '#8a4a2a';
    ctx.fillRect(x - 10 * s, y - 20 * s, 20 * s, 20 * s); // base

    for (let i = 0; i < tiers; i++) {
      const tierY = y - 20 * s - i * tierH;
      const w = baseW * (1 - i * 0.12);

      // Roof (curved eaves)
      ctx.fillStyle = '#3a2a1a';
      ctx.beginPath();
      ctx.moveTo(x - w, tierY);
      ctx.quadraticCurveTo(x - w * 0.5, tierY - tierH * 0.6, x - w * 0.4, tierY - tierH * 0.8);
      ctx.lineTo(x + w * 0.4, tierY - tierH * 0.8);
      ctx.quadraticCurveTo(x + w * 0.5, tierY - tierH * 0.6, x + w, tierY);
      ctx.closePath();
      ctx.fill();

      // Body
      ctx.fillStyle = '#d4a070';
      ctx.fillRect(x - w * 0.3, tierY - tierH * 0.7, w * 0.6, tierH * 0.5);
    }

    // Finial spire
    ctx.fillStyle = '#c0a040';
    ctx.beginPath();
    ctx.moveTo(x, y - 20 * s - tiers * tierH);
    ctx.lineTo(x - 3 * s, y - 20 * s - tiers * tierH - 15 * s);
    ctx.lineTo(x + 3 * s, y - 20 * s - tiers * tierH - 15 * s);
    ctx.closePath();
    ctx.fill();
  }

  drawTorii(x: number, y: number, s: number) {
    const { ctx } = this;
    const w = 60 * s;
    const h = 70 * s;
    const pillarW = 8 * s;

    // Traditional red torii gate
    ctx.fillStyle = '#c0392b';

    // Pillars
    ctx.fillRect(x - w / 2 - pillarW / 2, y - h, pillarW, h);
    ctx.fillRect(x + w / 2 - pillarW / 2, y - h, pillarW, h);

    // Top beam (kasagi) — curved
    ctx.beginPath();
    ctx.moveTo(x - w / 2 - 15 * s, y - h);
    ctx.quadraticCurveTo(x - w / 2 - 5 * s, y - h - 10 * s, x - w / 2, y - h - 8 * s);
    ctx.lineTo(x + w / 2, y - h - 8 * s);
    ctx.quadraticCurveTo(x + w / 2 + 5 * s, y - h - 10 * s, x + w / 2 + 15 * s, y - h);
    ctx.lineTo(x + w / 2 + 12 * s, y - h + 4 * s);
    ctx.lineTo(x - w / 2 - 12 * s, y - h + 4 * s);
    ctx.closePath();
    ctx.fill();

    // Second beam (nuki)
    ctx.fillRect(x - w / 2 - 5 * s, y - h + 15 * s, w + 10 * s, 6 * s);

    // Center tablet
    ctx.fillStyle = '#1a1a1a';
    ctx.fillRect(x - 8 * s, y - h + 8 * s, 16 * s, 10 * s);
  }

  drawShrine(x: number, y: number, s: number) {
    const { ctx } = this;
    const w = 50 * s;
    const h = 40 * s;

    // Base platform
    ctx.fillStyle = '#5a4a3a';
    ctx.fillRect(x - w / 2 - 5 * s, y - 5 * s, w + 10 * s, 8 * s);

    // Pillars
    ctx.fillStyle = '#c0392b';
    ctx.fillRect(x - w / 2, y - h, 6 * s, h);
    ctx.fillRect(x + w / 2 - 6 * s, y - h, 6 * s, h);

    // Roof
    ctx.fillStyle = '#2a2a2a';
    ctx.beginPath();
    ctx.moveTo(x - w / 2 - 8 * s, y - h);
    ctx.quadraticCurveTo(x, y - h - 15 * s, x + w / 2 + 8 * s, y - h);
    ctx.lineTo(x + w / 2 + 5 * s, y - h + 5 * s);
    ctx.lineTo(x - w / 2 - 5 * s, y - h + 5 * s);
    ctx.closePath();
    ctx.fill();

    // Body wall
    ctx.fillStyle = '#e8d5b8';
    ctx.fillRect(x - w / 2 + 5 * s, y - h + 15 * s, w - 10 * s, h - 20 * s);

    // Door
    ctx.fillStyle = '#3a2a1a';
    ctx.fillRect(x - 8 * s, y - h + 22 * s, 16 * s, h - 27 * s);
  }

  drawTemple(x: number, y: number, s: number) {
    const { ctx } = this;
    const w = 65 * s;
    const h = 35 * s;

    // Body
    ctx.fillStyle = '#d4a070';
    ctx.fillRect(x - w / 2, y - h, w, h);

    // Roof — wide, curved, tiled
    ctx.fillStyle = '#4a5a6a';
    ctx.beginPath();
    ctx.moveTo(x - w / 2 - 12 * s, y - h);
    ctx.quadraticCurveTo(x - w / 2 - 4 * s, y - h - 12 * s, x - w / 2 + 5 * s, y - h - 10 * s);
    ctx.lineTo(x + w / 2 - 5 * s, y - h - 10 * s);
    ctx.quadraticCurveTo(x + w / 2 + 4 * s, y - h - 12 * s, x + w / 2 + 12 * s, y - h);
    ctx.lineTo(x + w / 2 + 8 * s, y - h + 4 * s);
    ctx.lineTo(x - w / 2 - 8 * s, y - h + 4 * s);
    ctx.closePath();
    ctx.fill();

    // Roof tile lines
    ctx.strokeStyle = '#3a4a5a';
    ctx.lineWidth = 1 * s;
    for (let i = 0; i < 4; i++) {
      ctx.beginPath();
      ctx.moveTo(x - w / 2 - 10 * s + i * 3 * s, y - h - 2 * s);
      ctx.lineTo(x + w / 2 + 10 * s - i * 3 * s, y - h - 2 * s);
      ctx.stroke();
    }

    // Windows
    ctx.fillStyle = '#2a2a2a';
    ctx.fillRect(x - w / 2 + 8 * s, y - h + 12 * s, 10 * s, 12 * s);
    ctx.fillRect(x + w / 2 - 18 * s, y - h + 12 * s, 10 * s, 12 * s);

    // Door
    ctx.fillStyle = '#8a4a2a';
    ctx.fillRect(x - 7 * s, y - 15 * s, 14 * s, 15 * s);
  }

  drawTerrain(terrain: JPTerrain, cameraX: number, cameraY: number, viewWidth: number, viewHeight: number) {
    const { ctx } = this;
    const segW = terrain.segmentWidth;
    const startIdx = Math.max(0, Math.floor((cameraX - 50) / segW));
    const endIdx = Math.min(terrain.points.length - 1, Math.ceil((cameraX + viewWidth + 50) / segW));

    // Ground fill
    ctx.beginPath();
    ctx.moveTo(terrain.points[startIdx].x - cameraX, viewHeight);
    for (let i = startIdx; i <= endIdx; i++) {
      ctx.lineTo(terrain.points[i].x - cameraX, terrain.points[i].y - cameraY);
    }
    ctx.lineTo(terrain.points[endIdx].x - cameraX, viewHeight);
    ctx.closePath();

    // Japanese mountain earth — reddish-brown with green top layer
    const groundGrad = ctx.createLinearGradient(0, 350 - cameraY, 0, viewHeight);
    groundGrad.addColorStop(0, '#4a6a3a');
    groundGrad.addColorStop(0.15, '#3a5a2a');
    groundGrad.addColorStop(0.3, '#6a4a2a');
    groundGrad.addColorStop(0.6, '#5a3a1a');
    groundGrad.addColorStop(1, '#3a2a12');
    ctx.fillStyle = groundGrad;
    ctx.fill();

    // Trail surface — packed earth path
    ctx.beginPath();
    ctx.moveTo(terrain.points[startIdx].x - cameraX, terrain.points[startIdx].y - cameraY);
    for (let i = startIdx; i <= endIdx; i++) {
      ctx.lineTo(terrain.points[i].x - cameraX, terrain.points[i].y - cameraY);
    }
    ctx.strokeStyle = '#8a6a4a';
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
    ctx.strokeStyle = '#a88a6a';
    ctx.lineWidth = 2;
    ctx.stroke();

    // Grass/moss tufts
    ctx.fillStyle = '#3a5a2a';
    for (let i = startIdx; i <= endIdx; i += 4) {
      const px = terrain.points[i].x - cameraX;
      const py = terrain.points[i].y - cameraY;
      ctx.fillRect(px, py - 3, 2, 4);
      ctx.fillRect(px + 3, py - 2, 2, 3);
    }

    // Small rocks on trail
    ctx.fillStyle = '#888';
    for (let i = startIdx; i <= endIdx; i += 15) {
      const px = terrain.points[i].x - cameraX;
      const py = terrain.points[i].y - cameraY;
      ctx.beginPath();
      ctx.arc(px + (i % 7) * 3, py + 3, 2, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  drawSakuraPetals(cameraX: number) {
    const { ctx } = this;
    for (const petal of this.sakura) {
      const px = ((petal.x - cameraX * 0.3) % 3000 + 3000) % 3000 - 200;
      const py = petal.y;
      ctx.save();
      ctx.translate(px, py);
      ctx.rotate(petal.rotation);
      ctx.fillStyle = 'rgba(255, 183, 197, 0.7)';
      // 5-petal cherry blossom shape
      for (let i = 0; i < 5; i++) {
        ctx.beginPath();
        ctx.ellipse(petal.size * 0.6, 0, petal.size * 0.5, petal.size * 0.35, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.rotate((Math.PI * 2) / 5);
      }
      // Center
      ctx.fillStyle = '#ffeb80';
      ctx.beginPath();
      ctx.arc(0, 0, petal.size * 0.2, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }
  }

  updateSakura(dt: number) {
    for (const petal of this.sakura) {
      petal.x += petal.vx * dt;
      petal.y += petal.vy * dt;
      petal.rotation += petal.rotationSpeed * dt;
      // Sway
      petal.x += Math.sin(petal.y * 0.02) * 0.3 * dt;
      if (petal.y > 800) {
        petal.y = -20;
        petal.x = Math.random() * 3000;
      }
    }
  }

  drawBike(bike: JPBike, cameraX: number, cameraY: number) {
    const { ctx } = this;
    const bx = bike.x - cameraX;
    const by = bike.y - cameraY;

    ctx.save();
    ctx.translate(bx, by);
    ctx.rotate(bike.tilt);

    const mainColor = bike.color;
    const darkColor = this.shadeColor(mainColor, -40);
    const frameColor = this.shadeColor(mainColor, -20);

    // Shadow
    if (bike.onGround) {
      ctx.save();
      ctx.rotate(-bike.tilt);
      ctx.fillStyle = 'rgba(0,0,0,0.2)';
      ctx.beginPath();
      ctx.ellipse(0, WHEEL_RADIUS + 4, BIKE_LENGTH * 0.4, 5, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    // Wheels
    this.drawWheel(-BIKE_LENGTH * 0.35, WHEEL_RADIUS, bike.wheelRotation);
    this.drawWheel(BIKE_LENGTH * 0.35, WHEEL_RADIUS, bike.wheelRotation);

    // Frame
    ctx.strokeStyle = frameColor;
    ctx.lineWidth = 5;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(-BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.lineTo(-5, WHEEL_RADIUS - 15);
    ctx.lineTo(5, WHEEL_RADIUS - 28);
    ctx.lineTo(-10, WHEEL_RADIUS - 8);
    ctx.lineTo(-BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(5, WHEEL_RADIUS - 28);
    ctx.lineTo(BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(-5, WHEEL_RADIUS - 15);
    ctx.lineTo(BIKE_LENGTH * 0.2, WHEEL_RADIUS);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(BIKE_LENGTH * 0.2, WHEEL_RADIUS - 5);
    ctx.lineTo(BIKE_LENGTH * 0.35, WHEEL_RADIUS);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(BIKE_LENGTH * 0.2, WHEEL_RADIUS - 5);
    ctx.lineTo(BIKE_LENGTH * 0.22, WHEEL_RADIUS - 22);
    ctx.lineWidth = 4;
    ctx.stroke();

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

    // Boost flame
    if (bike.boostTimer > 0) {
      const flameLen = 15 + Math.random() * 10;
      const fx = -BIKE_LENGTH * 0.4;
      const grad = ctx.createLinearGradient(fx, 0, fx - flameLen, 0);
      grad.addColorStop(0, 'rgba(255, 100, 0, 0.8)');
      grad.addColorStop(0.5, 'rgba(255, 180, 0, 0.5)');
      grad.addColorStop(1, 'rgba(255, 255, 100, 0)');
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.moveTo(fx, -8);
      ctx.lineTo(fx - flameLen, 0);
      ctx.lineTo(fx, 8);
      ctx.closePath();
      ctx.fill();

      ctx.fillStyle = 'rgba(255, 255, 200, 0.6)';
      ctx.beginPath();
      ctx.moveTo(fx, -4);
      ctx.lineTo(fx - flameLen * 0.5, 0);
      ctx.lineTo(fx, 4);
      ctx.closePath();
      ctx.fill();
    }

    ctx.restore();
  }

  drawWheel(wx: number, wy: number, rotation: number) {
    const { ctx } = this;
    ctx.fillStyle = '#1a1a1a';
    ctx.beginPath();
    ctx.arc(wx, wy, WHEEL_RADIUS, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#555';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.arc(wx, wy, WHEEL_RADIUS - 4, 0, Math.PI * 2);
    ctx.stroke();
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
    ctx.fillStyle = '#666';
    ctx.beginPath();
    ctx.arc(wx, wy, 4, 0, Math.PI * 2);
    ctx.fill();
  }

  drawPedals(phase: number, cy: number) {
    const { ctx } = this;
    const crankRadius = 8;
    ctx.strokeStyle = '#444';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(Math.cos(phase) * crankRadius, cy + Math.sin(phase) * crankRadius);
    ctx.lineTo(Math.cos(phase + Math.PI) * crankRadius, cy + Math.sin(phase + Math.PI) * crankRadius);
    ctx.stroke();

    ctx.fillStyle = '#333';
    ctx.save();
    ctx.translate(Math.cos(phase) * crankRadius, cy + Math.sin(phase) * crankRadius);
    ctx.fillRect(-4, -2, 8, 4);
    ctx.restore();
    ctx.save();
    ctx.translate(Math.cos(phase + Math.PI) * crankRadius, cy + Math.sin(phase + Math.PI) * crankRadius);
    ctx.fillRect(-4, -2, 8, 4);
    ctx.restore();

    ctx.strokeStyle = '#777';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(0, cy, 6, 0, Math.PI * 2);
    ctx.stroke();
  }

  drawRider(bike: JPBike, wheelY: number, color: string) {
    const { ctx } = this;
    const jerseyColor = this.shadeColor(color, 20);
    const skinColor = '#d4a574';
    const pantsColor = '#2a2a3a';
    const seatY = wheelY - 30;
    const handleY = wheelY - 22;
    const leanOffset = bike.vx > 0 ? -3 : 0;
    const legPhase = bike.pedalPhase;
    const hipX = -3;
    const hipY = seatY - 2;

    // Legs
    ctx.strokeStyle = pantsColor;
    ctx.lineWidth = 7;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(hipX + Math.cos(legPhase + Math.PI) * 8 + leanOffset, hipY + 8);
    ctx.lineTo(hipX + Math.cos(legPhase + Math.PI) * 14, wheelY - 8 + Math.sin(legPhase + Math.PI) * 6);
    ctx.stroke();

    ctx.strokeStyle = this.shadeColor(pantsColor, 15);
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(hipX + Math.cos(legPhase) * 8 + leanOffset, hipY + 8);
    ctx.lineTo(hipX + Math.cos(legPhase) * 14, wheelY - 8 + Math.sin(legPhase) * 6);
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

    // Arms
    const handX = BIKE_LENGTH * 0.22;
    const handY = handleY;
    ctx.strokeStyle = skinColor;
    ctx.lineWidth = 5;
    ctx.beginPath();
    ctx.moveTo(torsoTopX, torsoTopY);
    ctx.lineTo(torsoTopX + 8, torsoTopY + 5);
    ctx.lineTo(handX, handY);
    ctx.stroke();

    ctx.fillStyle = '#222';
    ctx.beginPath();
    ctx.arc(handX, handY, 3.5, 0, Math.PI * 2);
    ctx.fill();

    // Head + helmet
    const headX = torsoTopX + 4;
    const headY = torsoTopY - 8;
    ctx.strokeStyle = skinColor;
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(torsoTopX, torsoTopY);
    ctx.lineTo(headX - 2, headY + 2);
    ctx.stroke();

    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(headX, headY, 9, Math.PI, Math.PI * 2);
    ctx.lineTo(headX + 9, headY + 3);
    ctx.lineTo(headX - 9, headY + 3);
    ctx.closePath();
    ctx.fill();

    // Helmet visor
    ctx.fillStyle = this.shadeColor(color, -30);
    ctx.beginPath();
    ctx.moveTo(headX - 9, headY + 1);
    ctx.lineTo(headX - 12, headY + 4);
    ctx.lineTo(headX - 9, headY + 4);
    ctx.closePath();
    ctx.fill();

    // Rising sun stripe on helmet
    ctx.fillStyle = '#e63946';
    ctx.fillRect(headX - 7, headY - 6, 14, 3);
    ctx.fillStyle = '#fff';
    ctx.beginPath();
    ctx.arc(headX, headY - 4.5, 2, 0, Math.PI * 2);
    ctx.fill();

    // Face
    ctx.fillStyle = skinColor;
    ctx.fillRect(headX - 7, headY + 2, 10, 4);

    // Number plate
    ctx.fillStyle = '#fff';
    ctx.fillRect(hipX - 6, hipY - 6, 5, 8);
    ctx.fillStyle = '#222';
    ctx.font = 'bold 6px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(String(bike.id), hipX - 3.5, hipY);
  }

  drawCrashedRider(wheelY: number, color: string) {
    const { ctx } = this;
    ctx.save();
    ctx.translate(-20, wheelY - 40);
    ctx.rotate(Math.random() * 0.3 - 0.15);
    ctx.strokeStyle = this.shadeColor(color, 20);
    ctx.lineWidth = 8;
    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(15, 5);
    ctx.stroke();
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(0, 0, 7, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#d4a574';
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(5, 2);
    ctx.lineTo(0, 12);
    ctx.moveTo(10, 4);
    ctx.lineTo(18, -5);
    ctx.stroke();
    ctx.strokeStyle = '#2a2a3a';
    ctx.lineWidth = 6;
    ctx.beginPath();
    ctx.moveTo(15, 5);
    ctx.lineTo(25, 0);
    ctx.lineTo(22, 15);
    ctx.stroke();
    ctx.restore();
  }

  drawParticles(particles: JPParticle[], cameraX: number, cameraY: number) {
    const { ctx } = this;
    for (const p of particles) {
      const px = p.x - cameraX;
      const py = p.y - cameraY;
      const alpha = Math.max(0, p.life / p.maxLife);

      if (p.type === 'dust') {
        ctx.fillStyle = `rgba(168, 144, 96, ${alpha * 0.6})`;
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
      } else if (p.type === 'sakura') {
        ctx.save();
        ctx.translate(px, py);
        ctx.rotate(p.rotation);
        ctx.fillStyle = p.color;
        ctx.globalAlpha = alpha * 0.7;
        for (let i = 0; i < 5; i++) {
          ctx.beginPath();
          ctx.ellipse(p.size * 0.6, 0, p.size * 0.5, p.size * 0.35, 0, 0, Math.PI * 2);
          ctx.fill();
          ctx.rotate((Math.PI * 2) / 5);
        }
        ctx.restore();
        ctx.globalAlpha = 1;
      }
    }
  }

  drawFinishLine(terrain: JPTerrain, cameraX: number, cameraY: number, trackLength: number) {
    const { ctx } = this;
    const fx = trackLength - cameraX;
    const fy = terrain.getHeightAt(trackLength) - cameraY;
    const poleH = 80;

    // Torii gate style finish
    ctx.fillStyle = '#c0392b';
    ctx.fillRect(fx - 2, fy - poleH, 4, poleH);
    ctx.fillRect(fx + 30, fy - poleH, 4, poleH);

    // Top beam
    ctx.beginPath();
    ctx.moveTo(fx - 15, fy - poleH);
    ctx.quadraticCurveTo(fx + 16, fy - poleH - 10, fx + 47, fy - poleH);
    ctx.lineTo(fx + 45, fy - poleH + 4);
    ctx.lineTo(fx - 13, fy - poleH + 4);
    ctx.closePath();
    ctx.fill();

    // Banner
    ctx.fillStyle = 'rgba(255, 215, 0, 0.9)';
    ctx.fillRect(fx - 20, fy - poleH - 25, 70, 18);
    ctx.fillStyle = '#000';
    ctx.font = 'bold 12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('GOAL', fx + 15, fy - poleH - 12);
  }

  drawStartLine(terrain: JPTerrain, cameraX: number, cameraY: number) {
    const { ctx } = this;
    const sx = 50 - cameraX;
    const sy = terrain.getHeightAt(50) - cameraY;

    // Start torii
    ctx.fillStyle = '#c0392b';
    ctx.fillRect(sx - 3, sy - 60, 6, 60);
    ctx.fillRect(sx + 27, sy - 60, 6, 60);
    ctx.beginPath();
    ctx.moveTo(sx - 18, sy - 60);
    ctx.quadraticCurveTo(sx + 15, sy - 70, sx + 48, sy - 60);
    ctx.lineTo(sx + 46, sy - 56);
    ctx.lineTo(sx - 16, sy - 56);
    ctx.closePath();
    ctx.fill();

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 9px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('START', sx + 15, sy - 48);
  }
}
