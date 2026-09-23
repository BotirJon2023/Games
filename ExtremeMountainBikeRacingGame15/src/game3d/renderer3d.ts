import type { Segment3D, Sprite3D, Bike3DState, Particle3D, Cloud3D } from './types';
import { Road3D, getSegmentColors } from './road';
import { project3D, getCameraDepth } from './projection';

export class Renderer3D {
  ctx: CanvasRenderingContext2D;
  width: number;
  height: number;
  cameraDepth: number;
  clouds: Cloud3D[] = [];

  constructor(ctx: CanvasRenderingContext2D, width: number, height: number) {
    this.ctx = ctx;
    this.width = width;
    this.height = height;
    this.cameraDepth = getCameraDepth(84, height);
    this.initClouds();
  }

  resize(width: number, height: number) {
    this.width = width;
    this.height = height;
    this.cameraDepth = getCameraDepth(84, height);
  }

  initClouds() {
    this.clouds = [];
    for (let i = 0; i < 12; i++) {
      this.clouds.push({
        x: (Math.random() - 0.5) * 4,
        y: 0.3 + Math.random() * 0.3,
        z: Math.random() * 0.8,
        scale: 0.5 + Math.random() * 1.5,
      });
    }
  }

  drawSky(bike: Bike3DState) {
    const { ctx, width, height } = this;

    // Dynamic sunset gradient
    const grad = ctx.createLinearGradient(0, 0, 0, height * 0.6);
    const speedPct = bike.speed / bike.maxSpeed;

    // Shift toward warmer colors at high speed
    grad.addColorStop(0, '#1a1a4e');
    grad.addColorStop(0.2, '#4a2a6e');
    grad.addColorStop(0.4, '#e85d5d');
    grad.addColorStop(0.55, '#ff9966');
    grad.addColorStop(0.65, '#ffd700');
    grad.addColorStop(1, '#fff4cc');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, width, height * 0.6);

    // Sun
    const sunX = width * 0.5 - bike.steerVisual * 40;
    const sunY = height * 0.35;
    const sunRadius = 60;

    // Sun glow
    const glowGrad = ctx.createRadialGradient(sunX, sunY, 0, sunX, sunY, sunRadius * 2.5);
    glowGrad.addColorStop(0, 'rgba(255, 240, 180, 0.5)');
    glowGrad.addColorStop(0.3, 'rgba(255, 180, 100, 0.3)');
    glowGrad.addColorStop(1, 'rgba(255, 100, 50, 0)');
    ctx.fillStyle = glowGrad;
    ctx.fillRect(0, 0, width, height * 0.6);

    // Sun body with bands
    ctx.fillStyle = '#fff8dc';
    ctx.beginPath();
    ctx.arc(sunX, sunY, sunRadius, 0, Math.PI * 2);
    ctx.fill();

    // Retro sun bands
    ctx.fillStyle = 'rgba(30, 20, 60, 0.5)';
    for (let i = 0; i < 5; i++) {
      const bandY = sunY + 15 + i * 12;
      const bandH = 3 + i * 1.5;
      const w = Math.sqrt(Math.max(0, sunRadius * sunRadius - (bandY - sunY) * (bandY - sunY)));
      if (w > 0) {
        ctx.fillRect(sunX - w, bandY, w * 2, bandH);
      }
    }

    // Mountains on horizon
    ctx.fillStyle = 'rgba(40, 30, 60, 0.6)';
    ctx.beginPath();
    ctx.moveTo(0, height * 0.55);
    for (let x = 0; x <= width; x += 30) {
      const h = 40 + Math.sin(x * 0.02 + bike.position * 0.0001) * 25 + Math.sin(x * 0.05) * 15;
      ctx.lineTo(x, height * 0.55 - h);
    }
    ctx.lineTo(width, height * 0.55);
    ctx.closePath();
    ctx.fill();

    // Closer mountains
    ctx.fillStyle = 'rgba(25, 20, 45, 0.7)';
    ctx.beginPath();
    ctx.moveTo(0, height * 0.58);
    for (let x = 0; x <= width; x += 40) {
      const h = 30 + Math.sin(x * 0.015 + 2) * 20 + Math.sin(x * 0.04) * 10;
      ctx.lineTo(x, height * 0.58 - h);
    }
    ctx.lineTo(width, height * 0.58);
    ctx.closePath();
    ctx.fill();
  }

  drawClouds(bike: Bike3DState) {
    const { ctx, width, height } = this;
    for (const cloud of this.clouds) {
      const cx = width * 0.5 + cloud.x * width * 0.3 - bike.steerVisual * 30;
      const cy = height * (0.1 + cloud.y * 0.15);
      const s = cloud.scale * 15;
      ctx.fillStyle = 'rgba(255, 200, 160, 0.5)';
      ctx.beginPath();
      ctx.arc(cx, cy, s, 0, Math.PI * 2);
      ctx.arc(cx + s * 0.8, cy - s * 0.3, s * 1.2, 0, Math.PI * 2);
      ctx.arc(cx + s * 1.6, cy, s, 0, Math.PI * 2);
      ctx.arc(cx + s * 0.8, cy + s * 0.4, s * 0.9, 0, Math.PI * 2);
      ctx.fill();
    }
  }

  drawRoad(
    road: Road3D,
    bike: Bike3DState,
    baseSegmentIndex: number,
    drawDistance: number,
    cameraX: number,
    cameraY: number,
    cameraZ: number
  ) {
    const { ctx, width, height } = this;
    const segments = road.segments;

    // Reset clip values
    for (let i = 0; i < segments.length; i++) {
      segments[i].clip = height;
    }

    // Project and draw from back to front
    let maxy = height;

    for (let n = 0; n < drawDistance; n++) {
      const segIdx = (baseSegmentIndex + n) % segments.length;
      const seg = segments[segIdx];
      const isLooped = baseSegmentIndex + n >= segments.length;

      // Project this segment
      const camZ = cameraZ - (isLooped ? road.totalLength : 0);
      project3D(seg.p1, cameraX - bike.playerX * road.roadWidth, cameraY, camZ, this.cameraDepth, width, height, road.roadWidth);
      project3D(seg.p2, cameraX - bike.playerX * road.roadWidth, cameraY, camZ, this.cameraDepth, width, height, road.roadWidth);

      seg.clip = maxy;

      // Skip if behind camera
      if (seg.p1.camera.z <= 0 || seg.p2.screen.y >= seg.p1.screen.y || seg.p2.screen.y >= maxy) {
        continue;
      }

      // Draw this segment
      this.drawSegment(seg, road, bike);
      maxy = seg.p2.screen.y;
    }
  }

  drawSegment(seg: Segment3D, road: Road3D, _bike: Bike3DState) {
    const { ctx } = this;
    const colors = getSegmentColors(seg);

    const p1 = seg.p1.screen;
    const p2 = seg.p2.screen;

    // Grass
    ctx.fillStyle = colors.grass;
    ctx.fillRect(0, p2.y, this.width, p1.y - p2.y);

    // Road
    const r1 = p1.w / Math.max(1, 6);
    const r2 = p2.w / Math.max(1, 6);
    const l1 = p1.w / Math.max(1, 40);
    const l2 = p2.w / Math.max(1, 40);

    // Rumble strips
    ctx.fillStyle = colors.rumble;
    this.polygon(p1.x - p1.w - r1, p1.y, p1.x - p1.w, p1.y, p2.x - p2.w, p2.y, p2.x - p2.w - r2, p2.y);
    this.polygon(p1.x + p1.w + r1, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x + p2.w + r2, p2.y);

    // Road surface
    ctx.fillStyle = colors.road;
    this.polygon(p1.x - p1.w, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x - p2.w, p2.y);

    // Lane markers
    if (seg.color === 'light' && colors.lane) {
      ctx.fillStyle = colors.lane;
      this.polygon(p1.x - l1, p1.y, p1.x + l1, p1.y, p2.x + l2, p2.y, p2.x - l2, p2.y);
    }

    // Off-road texture (dirt strip on edges)
    ctx.fillStyle = 'rgba(120, 90, 50, 0.3)';
    this.polygon(p1.x - p1.w - r1, p1.y, p1.x - p1.w, p1.y, p2.x - p2.w, p2.y, p2.x - p2.w - r2, p2.y);
    this.polygon(p1.x + p1.w + r1, p1.y, p1.x + p1.w, p1.y, p2.x + p2.w, p2.y, p2.x + p2.w + r2, p2.y);
  }

  polygon(x1: number, y1: number, x2: number, y2: number, x3: number, y3: number, x4: number, y4: number) {
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
    road: Road3D,
    bike: Bike3DState,
    baseSegmentIndex: number,
    drawDistance: number,
    cameraX: number,
    cameraY: number,
    cameraZ: number
  ) {
    const { ctx, width, height } = this;
    const segments = road.segments;

    // Draw from back to front
    for (let n = drawDistance - 1; n >= 0; n--) {
      const segIdx = (baseSegmentIndex + n) % segments.length;
      const seg = segments[segIdx];

      if (seg.p1.camera.z <= 0) continue;

      for (const sprite of seg.sprites) {
        const spriteScale = seg.p1.screen.scale;
        const spriteX = seg.p1.screen.x + spriteScale * sprite.offset * road.roadWidth * sprite.side;
        const spriteY = seg.p1.screen.y;

        if (spriteX < -200 || spriteX > width + 200) continue;

        this.drawSprite(sprite, spriteX, spriteY, spriteScale * road.roadWidth * 0.05, bike);
      }
    }
  }

  drawSprite(sprite: Sprite3D, x: number, y: number, scale: number, bike: Bike3DState) {
    const { ctx } = this;

    switch (sprite.type) {
      case 'tree': {
        const trunkW = Math.max(1, scale * 0.15);
        const trunkH = Math.max(1, scale * 0.4);
        const foliageW = Math.max(2, scale * 0.8);
        const foliageH = Math.max(2, scale * 1.2);
        // Trunk
        ctx.fillStyle = '#3d2817';
        ctx.fillRect(x - trunkW / 2, y - trunkH, trunkW, trunkH);
        // Foliage (pine tree shape)
        ctx.fillStyle = '#1a3a1a';
        ctx.beginPath();
        ctx.moveTo(x, y - trunkH - foliageH);
        ctx.lineTo(x - foliageW / 2, y - trunkH - foliageH * 0.3);
        ctx.lineTo(x - foliageW * 0.35, y - trunkH - foliageH * 0.4);
        ctx.lineTo(x - foliageW * 0.7, y - trunkH);
        ctx.lineTo(x + foliageW * 0.7, y - trunkH);
        ctx.lineTo(x + foliageW * 0.35, y - trunkH - foliageH * 0.4);
        ctx.lineTo(x + foliageW / 2, y - trunkH - foliageH * 0.3);
        ctx.closePath();
        ctx.fill();
        // Highlight
        ctx.fillStyle = '#2d5a2d';
        ctx.beginPath();
        ctx.moveTo(x, y - trunkH - foliageH);
        ctx.lineTo(x - foliageW * 0.2, y - trunkH - foliageH * 0.5);
        ctx.lineTo(x + foliageW * 0.1, y - trunkH - foliageH * 0.45);
        ctx.closePath();
        ctx.fill();
        break;
      }
      case 'bush': {
        const s = Math.max(2, scale * 0.5);
        ctx.fillStyle = '#2d4a2d';
        ctx.beginPath();
        ctx.arc(x, y - s * 0.3, s * 0.6, 0, Math.PI * 2);
        ctx.arc(x + s * 0.4, y - s * 0.2, s * 0.5, 0, Math.PI * 2);
        ctx.arc(x - s * 0.4, y - s * 0.2, s * 0.5, 0, Math.PI * 2);
        ctx.fill();
        break;
      }
      case 'rock': {
        const s = Math.max(2, scale * 0.6);
        ctx.fillStyle = '#5a5a5a';
        ctx.beginPath();
        ctx.moveTo(x - s * 0.5, y);
        ctx.lineTo(x - s * 0.6, y - s * 0.4);
        ctx.lineTo(x - s * 0.2, y - s * 0.7);
        ctx.lineTo(x + s * 0.3, y - s * 0.6);
        ctx.lineTo(x + s * 0.5, y - s * 0.3);
        ctx.lineTo(x + s * 0.4, y);
        ctx.closePath();
        ctx.fill();
        ctx.fillStyle = '#6a6a6a';
        ctx.beginPath();
        ctx.moveTo(x - s * 0.2, y - s * 0.7);
        ctx.lineTo(x + s * 0.1, y - s * 0.5);
        ctx.lineTo(x - s * 0.1, y - s * 0.4);
        ctx.closePath();
        ctx.fill();
        break;
      }
      case 'flag': {
        const poleH = Math.max(5, scale * 1.5);
        const flagW = Math.max(3, scale * 0.8);
        ctx.fillStyle = '#888';
        ctx.fillRect(x, y - poleH, 2, poleH);
        // Flag wave based on bike speed
        const wave = Math.sin(bike.position * 0.001) * 3;
        ctx.fillStyle = '#e63946';
        ctx.beginPath();
        ctx.moveTo(x + 2, y - poleH);
        ctx.lineTo(x + 2 + flagW, y - poleH + 5 + wave);
        ctx.lineTo(x + 2, y - poleH + 12);
        ctx.closePath();
        ctx.fill();
        break;
      }
      case 'sign': {
        const poleH = Math.max(10, scale * 2);
        const signW = Math.max(8, scale * 1.5);
        const signH = Math.max(6, scale * 0.8);
        ctx.fillStyle = '#666';
        ctx.fillRect(x - 1, y - poleH, 3, poleH);
        ctx.fillStyle = '#ff6b00';
        ctx.fillRect(x - signW / 2, y - poleH, signW, signH);
        ctx.fillStyle = '#fff';
        ctx.font = `bold ${Math.max(6, scale * 0.4)}px sans-serif`;
        ctx.textAlign = 'center';
        ctx.fillText('!', x, y - poleH + signH * 0.7);
        break;
      }
    }
  }

  drawBike3D(bike: Bike3DState, isPlayerView: boolean, otherBikes: Bike3DState[], road: Road3D, cameraZ: number, cameraY: number) {
    const { ctx, width, height } = this;

    if (isPlayerView) {
      // Draw other bikes ahead on the road
      for (const other of otherBikes) {
        if (other.id === bike.id) continue;
        const relPos = other.position - bike.position;
        if (relPos < 0) continue; // behind

        const seg = road.getSegmentAtZ(other.position);
        if (!seg || seg.p1.camera.z <= 0) continue;

        const scale = seg.p1.screen.scale;
        const screenX = seg.p1.screen.x + other.playerX * road.roadWidth * scale;
        const screenY = seg.p1.screen.y;
        const bikeScale = Math.max(0.05, scale * road.roadWidth * 0.015);

        if (screenX < -100 || screenX > width + 100) continue;

        this.drawBikerSprite(screenX, screenY, bikeScale, other, false);
      }

      // Draw player's own bike at bottom of screen (third-person behind view)
      const playerY = height - 140 - bike.airborne * 30 + Math.sin(bike.bounce * 10) * 2;
      const playerX = width / 2 + bike.steerVisual * 15;

      // Shadow
      ctx.fillStyle = 'rgba(0,0,0,0.3)';
      ctx.beginPath();
      ctx.ellipse(playerX, playerY + 55, 35, 8, 0, 0, Math.PI * 2);
      ctx.fill();

      this.drawBikerSprite(playerX, playerY, 1.0, bike, true);
    } else {
      // Side view for split-screen mini-map style
      this.drawBikerSprite(width / 2, height - 100, 0.8, bike, true);
    }
  }

  drawBikerSprite(x: number, y: number, scale: number, bike: Bike3DState, isPlayer: boolean) {
    const { ctx } = this;
    const s = scale;

    ctx.save();
    ctx.translate(x, y);
    ctx.scale(s, s);

    const color = bike.color;
    const darkColor = this.shadeColor(color, -40);
    const frameColor = this.shadeColor(color, -15);

    // Wheel rotation based on speed
    const wheelRot = bike.position * 0.01;

    // Rear wheel
    this.drawWheel3D(-22, 30, 16, wheelRot);
    // Front wheel (offset by steering)
    const frontWheelOffset = 22 + bike.steerVisual * 3;
    this.drawWheel3D(frontWheelOffset, 30, 16, wheelRot);

    // Frame
    ctx.strokeStyle = frameColor;
    ctx.lineWidth = 4;
    ctx.lineCap = 'round';

    // Main frame
    ctx.beginPath();
    ctx.moveTo(-22, 30);
    ctx.lineTo(-3, 15);
    ctx.lineTo(3, 0);
    ctx.lineTo(-8, 12);
    ctx.lineTo(-22, 30);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(3, 0);
    ctx.lineTo(frontWheelOffset, 30);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(-3, 15);
    ctx.lineTo(frontWheelOffset - 5, 30);
    ctx.stroke();

    // Fork
    ctx.beginPath();
    ctx.moveTo(frontWheelOffset - 5, 25);
    ctx.lineTo(frontWheelOffset, 30);
    ctx.stroke();

    // Handlebar
    ctx.beginPath();
    ctx.moveTo(frontWheelOffset - 5, 25);
    ctx.lineTo(frontWheelOffset - 3, 8);
    ctx.lineWidth = 3;
    ctx.stroke();

    // Seat
    ctx.fillStyle = darkColor;
    ctx.beginPath();
    ctx.ellipse(-1, -4, 8, 3, 0, 0, Math.PI * 2);
    ctx.fill();

    // Rider
    this.drawRider3D(bike, 30, color, isPlayer);

    // Boost flame
    if (bike.boostTimer > 0) {
      const flameLen = 12 + Math.random() * 8;
      const fx = -28;
      const grad = ctx.createLinearGradient(fx, 0, fx - flameLen, 0);
      grad.addColorStop(0, 'rgba(255, 100, 0, 0.8)');
      grad.addColorStop(0.5, 'rgba(255, 180, 0, 0.5)');
      grad.addColorStop(1, 'rgba(255, 255, 100, 0)');
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.moveTo(fx, -6);
      ctx.lineTo(fx - flameLen, 0);
      ctx.lineTo(fx, 6);
      ctx.closePath();
      ctx.fill();

      ctx.fillStyle = 'rgba(255, 255, 200, 0.6)';
      ctx.beginPath();
      ctx.moveTo(fx, -3);
      ctx.lineTo(fx - flameLen * 0.5, 0);
      ctx.lineTo(fx, 3);
      ctx.closePath();
      ctx.fill();
    }

    ctx.restore();
  }

  drawWheel3D(wx: number, wy: number, radius: number, rotation: number) {
    const { ctx } = this;
    ctx.fillStyle = '#1a1a1a';
    ctx.beginPath();
    ctx.arc(wx, wy, radius, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#555';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(wx, wy, radius - 3, 0, Math.PI * 2);
    ctx.stroke();
    // Spokes
    ctx.save();
    ctx.translate(wx, wy);
    ctx.rotate(rotation);
    ctx.strokeStyle = '#888';
    ctx.lineWidth = 1;
    for (let i = 0; i < 6; i++) {
      ctx.rotate(Math.PI / 3);
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.lineTo(radius - 4, 0);
      ctx.stroke();
    }
    ctx.restore();
    ctx.fillStyle = '#666';
    ctx.beginPath();
    ctx.arc(wx, wy, 3, 0, Math.PI * 2);
    ctx.fill();
  }

  drawRider3D(bike: Bike3DState, wheelY: number, color: string, isPlayer: boolean) {
    const { ctx } = this;
    const jerseyColor = this.shadeColor(color, 25);
    const skinColor = '#d4a574';
    const pantsColor = '#2a2a3a';

    const seatY = wheelY - 34;
    const handleY = wheelY - 22;
    const lean = bike.steerVisual * 3;

    // Legs
    const pedalPhase = bike.position * 0.01;
    const hipX = -2 + lean;
    const hipY = seatY - 2;

    // Back leg
    ctx.strokeStyle = pantsColor;
    ctx.lineWidth = 6;
    ctx.lineCap = 'round';
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(hipX + Math.cos(pedalPhase + Math.PI) * 6 + lean, hipY + 6);
    ctx.lineTo(hipX + Math.cos(pedalPhase + Math.PI) * 12, wheelY - 8 + Math.sin(pedalPhase + Math.PI) * 5);
    ctx.stroke();

    // Front leg
    ctx.strokeStyle = this.shadeColor(pantsColor, 12);
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(hipX + Math.cos(pedalPhase) * 6 + lean, hipY + 6);
    ctx.lineTo(hipX + Math.cos(pedalPhase) * 12, wheelY - 8 + Math.sin(pedalPhase) * 5);
    ctx.stroke();

    // Torso
    const torsoTopX = 4 + lean;
    const torsoTopY = seatY - 16;
    ctx.strokeStyle = jerseyColor;
    ctx.lineWidth = 9;
    ctx.beginPath();
    ctx.moveTo(hipX, hipY);
    ctx.lineTo(torsoTopX, torsoTopY);
    ctx.stroke();

    // Arms
    const handX = 22 + lean;
    const handY = handleY;
    ctx.strokeStyle = skinColor;
    ctx.lineWidth = 4;
    ctx.beginPath();
    ctx.moveTo(torsoTopX, torsoTopY);
    ctx.lineTo(torsoTopX + 6, torsoTopY + 4);
    ctx.lineTo(handX, handY);
    ctx.stroke();

    // Glove
    ctx.fillStyle = '#222';
    ctx.beginPath();
    ctx.arc(handX, handY, 3, 0, Math.PI * 2);
    ctx.fill();

    // Head + helmet
    const headX = torsoTopX + 3;
    const headY = torsoTopY - 7;
    ctx.fillStyle = color;
    ctx.beginPath();
    ctx.arc(headX, headY, 8, Math.PI, Math.PI * 2);
    ctx.lineTo(headX + 8, headY + 2);
    ctx.lineTo(headX - 8, headY + 2);
    ctx.closePath();
    ctx.fill();
    // Visor
    ctx.fillStyle = this.shadeColor(color, -30);
    ctx.beginPath();
    ctx.moveTo(headX - 8, headY);
    ctx.lineTo(headX - 10, headY + 3);
    ctx.lineTo(headX - 8, headY + 3);
    ctx.closePath();
    ctx.fill();
    // Stripe
    ctx.strokeStyle = '#fff';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.moveTo(headX - 5, headY - 4);
    ctx.lineTo(headX + 5, headY - 4);
    ctx.stroke();

    // Face
    ctx.fillStyle = skinColor;
    ctx.fillRect(headX - 6, headY + 1, 8, 3);

    // Number plate
    ctx.fillStyle = '#fff';
    ctx.fillRect(hipX - 5, hipY - 5, 4, 7);
    ctx.fillStyle = '#222';
    ctx.font = 'bold 5px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(String(bike.id), hipX - 3, hipY);
  }

  drawParticles3D(particles: Particle3D[], bike: Bike3DState) {
    const { ctx } = this;
    for (const p of particles) {
      const alpha = Math.max(0, p.life / p.maxLife);
      if (p.type === 'dust') {
        ctx.fillStyle = `rgba(140, 110, 70, ${alpha * 0.5})`;
        ctx.beginPath();
        ctx.arc(p.x, p.y, p.size * alpha, 0, Math.PI * 2);
        ctx.fill();
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

  drawSpeedLines(bike: Bike3DState) {
    const { ctx, width, height } = this;
    const speedPct = bike.speed / bike.maxSpeed;
    if (speedPct < 0.5) return;

    const intensity = (speedPct - 0.5) * 2;
    ctx.strokeStyle = `rgba(255, 255, 255, ${intensity * 0.15})`;
    ctx.lineWidth = 2;

    // Speed lines emanating from center
    for (let i = 0; i < 20; i++) {
      const angle = (Math.random() - 0.5) * Math.PI;
      const startR = 200 + Math.random() * 100;
      const len = 30 + Math.random() * 50 * intensity;
      const cx = width / 2;
      const cy = height * 0.55;
      const x1 = cx + Math.cos(angle) * startR;
      const y1 = cy + Math.sin(angle) * startR * 0.5;
      const x2 = cx + Math.cos(angle) * (startR + len);
      const y2 = cy + Math.sin(angle) * (startR + len) * 0.5;
      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();
    }
  }

  shadeColor(color: string, percent: number): string {
    const num = parseInt(color.replace('#', ''), 16);
    const r = Math.min(255, Math.max(0, (num >> 16) + percent));
    const g = Math.min(255, Math.max(0, ((num >> 8) & 0x00ff) + percent));
    const b = Math.min(255, Math.max(0, (num & 0x0000ff) + percent));
    return `rgb(${r},${g},${b})`;
  }
}
