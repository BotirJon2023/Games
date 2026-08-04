import type { Car, RaceState, Track, CameraState, SceneryItem, Particle } from './types';

export function drawRace(
  ctx: CanvasRenderingContext2D,
  state: RaceState,
  cam: CameraState,
  canvasW: number,
  canvasH: number,
  t: number
): void {
  const track = state.track;
  ctx.save();
  drawSky(ctx, track, canvasW, canvasH);

  ctx.save();
  applyCamera(ctx, cam, canvasW, canvasH);
  drawGround(ctx, track);
  drawRoad(ctx, track);
  drawSceneryBase(ctx, track);
  drawStartLine(ctx, track);
  drawTireMarks(ctx, state.tireMarks);
  drawCheckpoints(ctx, track, t);
  drawSceneryTop(ctx, track);
  state.cars.forEach((c) => drawCar(ctx, c));
  drawParticles(ctx, state.particles);
  ctx.restore();

  ctx.restore();
}

function drawSky(ctx: CanvasRenderingContext2D, track: Track, w: number, h: number): void {
  const g = ctx.createLinearGradient(0, 0, 0, h);
  g.addColorStop(0, track.theme.skyTop);
  g.addColorStop(1, track.theme.skyBottom);
  ctx.fillStyle = g;
  ctx.fillRect(0, 0, w, h);
}

function applyCamera(ctx: CanvasRenderingContext2D, cam: CameraState, w: number, h: number): void {
  const sx = (Math.random() - 0.5) * cam.shake;
  const sy = (Math.random() - 0.5) * cam.shake;
  ctx.translate(w / 2 + sx, h / 2 + sy);
  ctx.scale(cam.zoom, cam.zoom);
  ctx.translate(-cam.x, -cam.y);
}

function drawGround(ctx: CanvasRenderingContext2D, track: Track): void {
  const s = track.samples!;
  let minX = Infinity, minY = Infinity, maxX = -Infinity, maxY = -Infinity;
  for (const p of s) {
    minX = Math.min(minX, p.x); minY = Math.min(minY, p.y);
    maxX = Math.max(maxX, p.x); maxY = Math.max(maxY, p.y);
  }
  const pad = 600;
  ctx.fillStyle = track.theme.ground;
  ctx.fillRect(minX - pad, minY - pad, maxX - minX + pad * 2, maxY - minY + pad * 2);
}

function drawRoad(ctx: CanvasRenderingContext2D, track: Track): void {
  const s = track.samples!;
  const n = s.length;
  const hw = track.halfWidth;

  // Road surface
  ctx.beginPath();
  for (let i = 0; i <= n; i++) {
    const p = track.leftEdge![i % n];
    if (i === 0) ctx.moveTo(p.x, p.y);
    else ctx.lineTo(p.x, p.y);
  }
  for (let i = n; i >= 0; i--) {
    const p = track.rightEdge![i % n];
    ctx.lineTo(p.x, p.y);
  }
  ctx.closePath();
  ctx.fillStyle = track.theme.road;
  ctx.fill();

  // Subtle road shading
  ctx.save();
  ctx.clip();
  ctx.lineWidth = 3;
  ctx.strokeStyle = track.theme.roadDark;
  for (let i = 0; i < n; i += 1) {
    const p = s[i];
    const nm = track.normals![i];
    ctx.globalAlpha = i % 2 === 0 ? 0.05 : 0;
    ctx.beginPath();
    ctx.moveTo(p.x + nm.x * hw, p.y + nm.y * hw);
    ctx.lineTo(p.x - nm.x * hw, p.y - nm.y * hw);
    ctx.stroke();
  }
  ctx.globalAlpha = 1;
  ctx.restore();

  // Curbs
  for (const side of [-1, 1]) {
    for (let i = 0; i < n; i++) {
      const a = i;
      const b = (i + 1) % n;
      const nm = track.normals![i];
      const color = i % 8 < 4 ? track.theme.curbA : track.theme.curbB;
      const lw = 6;
      ctx.beginPath();
      const pa = { x: s[a].x + nm.x * side * (hw - lw / 2), y: s[a].y + nm.y * side * (hw - lw / 2) };
      const pb = { x: s[b].x + track.normals![b].x * side * (hw - lw / 2), y: s[b].y + track.normals![b].y * side * (hw - lw / 2) };
      ctx.moveTo(pa.x, pa.y);
      ctx.lineTo(pb.x, pb.y);
      ctx.strokeStyle = color;
      ctx.lineWidth = lw;
      ctx.stroke();
    }
  }

  // Center dashed line
  ctx.save();
  ctx.lineWidth = 3;
  ctx.strokeStyle = track.theme.curbB;
  ctx.globalAlpha = 0.5;
  ctx.setLineDash([22, 22]);
  ctx.beginPath();
  for (let i = 0; i <= n; i++) {
    const p = s[i % n];
    if (i === 0) ctx.moveTo(p.x, p.y);
    else ctx.lineTo(p.x, p.y);
  }
  ctx.stroke();
  ctx.restore();
}

function drawStartLine(ctx: CanvasRenderingContext2D, track: Track): void {
  const s = track.samples!;
  const i = 2;
  const p = s[i];
  const nm = track.normals![i];
  const hw = track.halfWidth;
  const dx = -nm.y;
  const dy = nm.x;
  ctx.save();
  ctx.translate(p.x, p.y);
  ctx.rotate(Math.atan2(dy, dx));
  const w = 22;
  const h = hw * 2;
  for (let r = 0; r < 2; r++) {
    for (let c = 0; c < 8; c++) {
      const bw = w / 2;
      const bh = h / 8;
      ctx.fillStyle = (r + c) % 2 === 0 ? '#f8fafc' : '#1e293b';
      ctx.fillRect(-w / 2 + r * bw, -h / 2 + c * bh, bw, bh);
    }
  }
  ctx.restore();
}

function drawCheckpoints(ctx: CanvasRenderingContext2D, track: Track, _t: number): void {
  // (decorative checkpoints omitted to reduce clutter)
  void track; void _t;
}

function drawTireMarks(ctx: CanvasRenderingContext2D, marks: import('./types').TireMark[]): void {
  ctx.save();
  for (const m of marks) {
    ctx.globalAlpha = m.alpha * 0.5 * m.intensity;
    ctx.fillStyle = '#1e1b2e';
    ctx.save();
    ctx.translate(m.x, m.y);
    ctx.rotate(m.angle);
    ctx.fillRect(-2, -1.5, 4, 3);
    ctx.restore();
  }
  ctx.globalAlpha = 1;
  ctx.restore();
}

function drawSceneryBase(ctx: CanvasRenderingContext2D, track: Track): void {
  const items = track.scenery ?? [];
  for (const item of items) {
    if (item.type === 'mountain' || item.type === 'sanddune' || item.type === 'building' || item.type === 'pyramid') {
      drawSceneryItem(ctx, item);
    }
  }
}

function drawSceneryTop(ctx: CanvasRenderingContext2D, track: Track): void {
  const items = track.scenery ?? [];
  for (const item of items) {
    if (item.type !== 'mountain' && item.type !== 'sanddune' && item.type !== 'building' && item.type !== 'pyramid') {
      drawSceneryItem(ctx, item);
    }
  }
}

function drawSceneryItem(ctx: CanvasRenderingContext2D, item: SceneryItem): void {
  ctx.save();
  ctx.translate(item.pos.x, item.pos.y);
  ctx.scale(item.scale, item.scale);
  switch (item.type) {
    case 'tree': drawTree(ctx); break;
    case 'palm': drawPalm(ctx); break;
    case 'cactus': drawCactus(ctx); break;
    case 'rock': drawRock(ctx); break;
    case 'mountain': drawMountain(ctx); break;
    case 'building': drawBuilding(ctx); break;
    case 'pyramid': drawPyramid(ctx); break;
    case 'tent': drawTent(ctx); break;
    case 'ferris': drawFerris(ctx); break;
    case 'lighthouse': drawLighthouse(ctx); break;
    case 'sanddune': drawDune(ctx); break;
    case 'flag': drawFlag(ctx); break;
    case 'snowtree': drawSnowTree(ctx); break;
  }
  ctx.restore();
}

function drawTree(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#3f2d1d';
  ctx.fillRect(-3, 0, 6, 16);
  ctx.fillStyle = '#1f5e2a';
  ctx.beginPath(); ctx.arc(0, -6, 14, 0, Math.PI * 2); ctx.fill();
  ctx.fillStyle = '#2d7a3f';
  ctx.beginPath(); ctx.arc(-5, -12, 9, 0, Math.PI * 2); ctx.fill();
  ctx.beginPath(); ctx.arc(6, -10, 8, 0, Math.PI * 2); ctx.fill();
}

function drawSnowTree(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#5a3f2a';
  ctx.fillRect(-3, 0, 6, 14);
  ctx.fillStyle = '#0f4a2a';
  ctx.beginPath();
  ctx.moveTo(0, -28); ctx.lineTo(-14, -6); ctx.lineTo(14, -6); ctx.closePath(); ctx.fill();
  ctx.beginPath();
  ctx.moveTo(0, -20); ctx.lineTo(-11, -2); ctx.lineTo(11, -2); ctx.closePath(); ctx.fill();
  ctx.fillStyle = '#f8fafc';
  ctx.beginPath();
  ctx.moveTo(0, -28); ctx.lineTo(-6, -18); ctx.lineTo(6, -18); ctx.closePath(); ctx.fill();
}

function drawPalm(ctx: CanvasRenderingContext2D): void {
  ctx.strokeStyle = '#7a5a32';
  ctx.lineWidth = 5;
  ctx.beginPath();
  ctx.moveTo(0, 18);
  ctx.quadraticCurveTo(-4, 0, 0, -18);
  ctx.stroke();
  ctx.fillStyle = '#16a34a';
  for (let i = 0; i < 5; i++) {
    const a = (i / 5) * Math.PI * 2 - Math.PI / 2;
    ctx.save();
    ctx.translate(0, -18);
    ctx.rotate(a);
    ctx.beginPath();
    ctx.ellipse(14, 0, 16, 4, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }
}

function drawCactus(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#3a7d3a';
  ctx.beginPath();
  ctx.roundRect(-6, -24, 12, 42, 6);
  ctx.fill();
  ctx.beginPath();
  ctx.roundRect(-16, -8, 10, 18, 5);
  ctx.fill();
  ctx.beginPath();
  ctx.roundRect(6, -14, 10, 20, 5);
  ctx.fill();
}

function drawRock(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#6b7280';
  ctx.beginPath();
  ctx.moveTo(-14, 8); ctx.lineTo(-8, -8); ctx.lineTo(4, -12); ctx.lineTo(14, -2); ctx.lineTo(12, 10);
  ctx.closePath();
  ctx.fill();
  ctx.fillStyle = '#9ca3af';
  ctx.beginPath();
  ctx.moveTo(-8, -8); ctx.lineTo(4, -12); ctx.lineTo(2, -4); ctx.closePath();
  ctx.fill();
}

function drawMountain(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#4b5563';
  ctx.beginPath();
  ctx.moveTo(-40, 20); ctx.lineTo(-10, -30); ctx.lineTo(14, -8); ctx.lineTo(38, 16);
  ctx.closePath(); ctx.fill();
  ctx.fillStyle = '#f8fafc';
  ctx.beginPath();
  ctx.moveTo(-10, -30); ctx.lineTo(-2, -16); ctx.lineTo(-14, -10); ctx.closePath(); ctx.fill();
  ctx.beginPath();
  ctx.moveTo(14, -8); ctx.lineTo(22, 2); ctx.lineTo(8, 2); ctx.closePath(); ctx.fill();
}

function drawDune(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#d9a85a';
  ctx.beginPath();
  ctx.moveTo(-50, 20); ctx.quadraticCurveTo(-20, -18, 10, -6); ctx.quadraticCurveTo(30, 2, 50, 20);
  ctx.closePath(); ctx.fill();
  ctx.fillStyle = '#c89844';
  ctx.beginPath();
  ctx.moveTo(-50, 20); ctx.quadraticCurveTo(-24, -6, 0, 6); ctx.lineTo(0, 20); ctx.closePath(); ctx.fill();
}

function drawBuilding(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#374151';
  ctx.fillRect(-14, -34, 28, 52);
  ctx.fillStyle = '#1f2937';
  ctx.fillRect(-14, -34, 28, 6);
  ctx.fillStyle = '#fde68a';
  for (let r = 0; r < 5; r++) {
    for (let c = 0; c < 3; c++) {
      if (Math.random() > 0.3 || (r + c) % 2 === 0) {
        ctx.fillRect(-11 + c * 8, -26 + r * 8, 5, 5);
      }
    }
  }
}

function drawPyramid(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#c89844';
  ctx.beginPath();
  ctx.moveTo(-30, 20); ctx.lineTo(0, -36); ctx.lineTo(30, 20); ctx.closePath(); ctx.fill();
  ctx.fillStyle = '#a87a34';
  ctx.beginPath();
  ctx.moveTo(0, -36); ctx.lineTo(30, 20); ctx.lineTo(4, 20); ctx.closePath(); ctx.fill();
}

function drawTent(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#dc2626';
  ctx.beginPath();
  ctx.moveTo(-16, 16); ctx.lineTo(0, -18); ctx.lineTo(16, 16); ctx.closePath(); ctx.fill();
  ctx.fillStyle = '#f8fafc';
  ctx.beginPath();
  ctx.moveTo(-16, 16); ctx.lineTo(0, -18); ctx.lineTo(-2, 16); ctx.closePath(); ctx.fill();
}

function drawFerris(ctx: CanvasRenderingContext2D): void {
  ctx.strokeStyle = '#9ca3af';
  ctx.lineWidth = 2;
  ctx.beginPath(); ctx.arc(0, -20, 22, 0, Math.PI * 2); ctx.stroke();
  for (let i = 0; i < 8; i++) {
    const a = (i / 8) * Math.PI * 2;
    ctx.beginPath();
    ctx.moveTo(0, -20);
    ctx.lineTo(Math.cos(a) * 22, -20 + Math.sin(a) * 22);
    ctx.stroke();
    ctx.fillStyle = ['#ef4444', '#fbbf24', '#22d3ee', '#84cc16'][i % 4];
    ctx.beginPath();
    ctx.arc(Math.cos(a) * 22, -20 + Math.sin(a) * 22, 4, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.fillStyle = '#4b5563';
  ctx.fillRect(-2, 0, 4, 16);
}

function drawLighthouse(ctx: CanvasRenderingContext2D): void {
  ctx.fillStyle = '#f8fafc';
  ctx.fillRect(-7, -28, 14, 30);
  ctx.fillStyle = '#ef4444';
  ctx.fillRect(-7, -22, 14, 5);
  ctx.fillRect(-7, -10, 14, 5);
  ctx.fillStyle = '#fbbf24';
  ctx.beginPath();
  ctx.moveTo(-9, -34); ctx.lineTo(9, -34); ctx.lineTo(7, -28); ctx.lineTo(-7, -28); ctx.closePath();
  ctx.fill();
  ctx.fillStyle = '#1e293b';
  ctx.fillRect(-2, 2, 4, 10);
}

function drawFlag(ctx: CanvasRenderingContext2D): void {
  ctx.strokeStyle = '#4b5563';
  ctx.lineWidth = 2;
  ctx.beginPath(); ctx.moveTo(0, 16); ctx.lineTo(0, -20); ctx.stroke();
  ctx.fillStyle = '#ef4444';
  ctx.beginPath();
  ctx.moveTo(0, -20); ctx.lineTo(16, -16); ctx.lineTo(0, -12); ctx.closePath(); ctx.fill();
}

function drawCar(ctx: CanvasRenderingContext2D, car: Car): void {
  const speed = Math.hypot(car.vel.x, car.vel.y);
  ctx.save();
  ctx.translate(car.pos.x, car.pos.y);
  // shadow
  ctx.save();
  ctx.rotate(car.angle);
  ctx.fillStyle = 'rgba(0,0,0,0.28)';
  ctx.beginPath();
  ctx.roundRect(-22, -13, 44, 26, 6);
  ctx.fill();
  ctx.restore();

  ctx.rotate(car.angle);
  ctx.rotate(car.lean * 0.4);

  // body
  ctx.fillStyle = car.body;
  ctx.beginPath();
  ctx.roundRect(-21, -12, 42, 24, 7);
  ctx.fill();

  // hood taper
  ctx.fillStyle = shade(car.body, -18);
  ctx.beginPath();
  ctx.roundRect(6, -10, 14, 20, 4);
  ctx.fill();

  // roof / cabin
  ctx.fillStyle = shade(car.body, 14);
  ctx.beginPath();
  ctx.roundRect(-12, -9, 18, 18, 4);
  ctx.fill();

  // windshield
  ctx.fillStyle = 'rgba(180,220,255,0.85)';
  ctx.beginPath();
  ctx.roundRect(2, -8, 6, 16, 2);
  ctx.fill();
  // rear window
  ctx.fillStyle = 'rgba(180,220,255,0.55)';
  ctx.beginPath();
  ctx.roundRect(-11, -7, 4, 14, 2);
  ctx.fill();

  // racing stripe
  ctx.fillStyle = car.accent;
  ctx.fillRect(-21, -2.5, 42, 1.5);
  ctx.fillRect(-21, 1, 42, 1.5);

  // headlight
  ctx.fillStyle = '#fef9c3';
  ctx.beginPath(); ctx.arc(19, -8, 2.2, 0, Math.PI * 2); ctx.fill();
  ctx.beginPath(); ctx.arc(19, 8, 2.2, 0, Math.PI * 2); ctx.fill();

  // brake lights
  const bl = car.brakeLight;
  if (bl > 0.02) {
    ctx.fillStyle = `rgba(255,40,40,${0.4 + bl * 0.6})`;
    ctx.beginPath(); ctx.arc(-20, -8, 2.2, 0, Math.PI * 2); ctx.fill();
    ctx.beginPath(); ctx.arc(-20, 8, 2.2, 0, Math.PI * 2); ctx.fill();
  }

  // wheels with spin
  drawWheel(ctx, -13, -12, car.wheelSpin, speed);
  drawWheel(ctx, 13, -12, car.wheelSpin, speed);
  drawWheel(ctx, -13, 12, car.wheelSpin, speed);
  drawWheel(ctx, 13, 12, car.wheelSpin, speed);

  // number circle
  ctx.restore();
  ctx.save();
  ctx.translate(car.pos.x, car.pos.y);
  ctx.fillStyle = 'rgba(255,255,255,0.9)';
  ctx.beginPath(); ctx.arc(0, -16, 0, 0, Math.PI * 2); ctx.fill();
  ctx.restore();
}

function drawWheel(ctx: CanvasRenderingContext2D, x: number, y: number, spin: number, speed: number): void {
  ctx.save();
  ctx.translate(x, y);
  ctx.fillStyle = '#0f172a';
  ctx.beginPath(); ctx.arc(0, 0, 4.5, 0, Math.PI * 2); ctx.fill();
  // blur when fast
  if (speed > 250) {
    ctx.fillStyle = 'rgba(15,23,42,0.4)';
    ctx.beginPath();
    ctx.ellipse(0, 0, 5.5, 3, 0, 0, Math.PI * 2);
    ctx.fill();
  } else {
    ctx.strokeStyle = '#475569';
    ctx.lineWidth = 1.2;
    const spokes = 4;
    for (let i = 0; i < spokes; i++) {
      const a = spin + (i / spokes) * Math.PI * 2;
      ctx.beginPath();
      ctx.moveTo(0, 0);
      ctx.lineTo(Math.cos(a) * 3.5, Math.sin(a) * 3.5);
      ctx.stroke();
    }
  }
  ctx.restore();
}

function drawParticles(ctx: CanvasRenderingContext2D, particles: Particle[]): void {
  for (const p of particles) {
    const a = Math.max(0, p.life / p.maxLife);
    ctx.globalAlpha = a;
    if (p.type === 'confetti') {
      ctx.fillStyle = p.color;
      ctx.save();
      ctx.translate(p.x, p.y);
      ctx.rotate(p.life * 6);
      ctx.fillRect(-p.size / 2, -p.size / 2, p.size, p.size * 0.6);
      ctx.restore();
    } else {
      ctx.fillStyle = p.color;
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size * (p.type === 'smoke' ? 1.6 : 1), 0, Math.PI * 2);
      ctx.fill();
    }
  }
  ctx.globalAlpha = 1;
}

export function shade(hex: string, percent: number): string {
  const c = hex.replace('#', '');
  const num = parseInt(c, 16);
  let r = (num >> 16) & 0xff;
  let g = (num >> 8) & 0xff;
  let b = num & 0xff;
  const f = percent / 100;
  r = Math.max(0, Math.min(255, Math.round(r + r * f)));
  g = Math.max(0, Math.min(255, Math.round(g + g * f)));
  b = Math.max(0, Math.min(255, Math.round(b + b * f)));
  return `rgb(${r},${g},${b})`;
}
