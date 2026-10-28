import type { TrackData } from './track';
import type { Vec2 } from './types';
import { Hoverboard } from './hoverboard';
import { CANVAS_WIDTH, CANVAS_HEIGHT } from './constants';
import { add, scale, sub, angleOf } from './math';

export class Renderer {
  ctx: CanvasRenderingContext2D;
  bgOffset: number = 0;
  mountainOffset: number = 0;

  constructor(ctx: CanvasRenderingContext2D) {
    this.ctx = ctx;
  }

  clear(): void {
    const ctx = this.ctx;
    // Sky gradient background
    const grad = ctx.createLinearGradient(0, 0, 0, CANVAS_HEIGHT);
    grad.addColorStop(0, '#0a0a2e');
    grad.addColorStop(0.4, '#16213e');
    grad.addColorStop(0.7, '#1a1a3e');
  grad.addColorStop(1, '#0f0f23');
    ctx.fillStyle = grad;
    ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
  }

  drawBackground(elapsed: number): void {
    const ctx = this.ctx;

    // Stars
    ctx.save();
    for (let i = 0; i < 80; i++) {
      const x = (i * 137.5) % CANVAS_WIDTH;
      const y = (i * 71.3) % (CANVAS_HEIGHT * 0.5);
      const twinkle = 0.3 + 0.7 * Math.abs(Math.sin(elapsed * 2 + i));
      ctx.fillStyle = `rgba(255, 255, 255, ${twinkle * 0.6})`;
      ctx.beginPath();
      ctx.arc(x, y, 1.2, 0, Math.PI * 2);
      ctx.fill();
    }
    ctx.restore();

    // Distant mountains (parallax)
    ctx.save();
    this.mountainOffset = elapsed * 8;
    ctx.fillStyle = 'rgba(30, 35, 70, 0.6)';
    ctx.beginPath();
    ctx.moveTo(0, CANVAS_HEIGHT * 0.55);
    for (let x = 0; x <= CANVAS_WIDTH + 100; x += 50) {
      const offset = (x + this.mountainOffset) * 0.01;
      const y = CANVAS_HEIGHT * 0.55 - Math.abs(Math.sin(offset)) * 60 - Math.abs(Math.sin(offset * 2.3)) * 30;
      ctx.lineTo(x, y);
    }
    ctx.lineTo(CANVAS_WIDTH, CANVAS_HEIGHT);
    ctx.lineTo(0, CANVAS_HEIGHT);
    ctx.closePath();
    ctx.fill();
    ctx.restore();

    // Closer mountains
    ctx.save();
    ctx.fillStyle = 'rgba(20, 25, 55, 0.7)';
    ctx.beginPath();
    ctx.moveTo(0, CANVAS_HEIGHT * 0.65);
    for (let x = 0; x <= CANVAS_WIDTH + 100; x += 40) {
      const offset = (x + this.mountainOffset * 1.5) * 0.015;
      const y = CANVAS_HEIGHT * 0.65 - Math.abs(Math.sin(offset)) * 45 - Math.abs(Math.sin(offset * 1.7)) * 20;
      ctx.lineTo(x, y);
    }
    ctx.lineTo(CANVAS_WIDTH, CANVAS_HEIGHT);
    ctx.lineTo(0, CANVAS_HEIGHT);
    ctx.closePath();
    ctx.fill();
    ctx.restore();

    // Grid floor effect
    ctx.save();
    ctx.strokeStyle = 'rgba(50, 60, 120, 0.15)';
    ctx.lineWidth = 1;
    const gridOffset = (elapsed * 30) % 40;
    for (let y = CANVAS_HEIGHT * 0.7; y < CANVAS_HEIGHT; y += 20) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(CANVAS_WIDTH, y);
      ctx.stroke();
    }
    for (let x = -gridOffset; x < CANVAS_WIDTH; x += 40) {
      ctx.beginPath();
      ctx.moveTo(x, CANVAS_HEIGHT * 0.7);
      ctx.lineTo(x + (x - CANVAS_WIDTH / 2) * 0.5, CANVAS_HEIGHT);
      ctx.stroke();
    }
    ctx.restore();
  }

  drawTrack(track: TrackData, elapsed: number): void {
    const ctx = this.ctx;
    const pts = track.points;
    const halfW = track.width / 2;

    // Track glow (outer)
    ctx.save();
    ctx.strokeStyle = 'rgba(100, 80, 200, 0.15)';
    ctx.lineWidth = track.width + 20;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(pts[0].pos.x, pts[0].pos.y);
    for (let i = 1; i < pts.length; i++) {
      ctx.lineTo(pts[i].pos.x, pts[i].pos.y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.restore();

    // Track surface
    ctx.save();
    ctx.strokeStyle = '#1c1c38';
    ctx.lineWidth = track.width;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(pts[0].pos.x, pts[0].pos.y);
    for (let i = 1; i < pts.length; i++) {
      ctx.lineTo(pts[i].pos.x, pts[i].pos.y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.restore();

    // Inner track highlight
    ctx.save();
    ctx.strokeStyle = '#252550';
    ctx.lineWidth = track.width - 8;
    ctx.lineCap = 'round';
    ctx.lineJoin = 'round';
    ctx.beginPath();
    ctx.moveTo(pts[0].pos.x, pts[0].pos.y);
    for (let i = 1; i < pts.length; i++) {
      ctx.lineTo(pts[i].pos.x, pts[i].pos.y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.restore();

    // Track edge lines (dashed, animated)
    ctx.save();
    ctx.strokeStyle = 'rgba(255, 255, 100, 0.5)';
    ctx.lineWidth = 2;
    ctx.setLineDash([12, 8]);
    ctx.lineDashOffset = -elapsed * 30;
    ctx.beginPath();
    for (let i = 0; i < pts.length; i++) {
      const p = pts[i];
      const outer = add(p.pos, scale(p.normal, halfW - 2));
      if (i === 0) ctx.moveTo(outer.x, outer.y);
      else ctx.lineTo(outer.x, outer.y);
    }
    ctx.closePath();
    ctx.stroke();

    ctx.beginPath();
    for (let i = 0; i < pts.length; i++) {
      const p = pts[i];
      const inner = add(p.pos, scale(p.normal, -(halfW - 2)));
      if (i === 0) ctx.moveTo(inner.x, inner.y);
      else ctx.lineTo(inner.x, inner.y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.restore();

    // Center line (subtle)
    ctx.save();
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.08)';
    ctx.lineWidth = 1;
    ctx.setLineDash([4, 12]);
    ctx.beginPath();
    ctx.moveTo(pts[0].pos.x, pts[0].pos.y);
    for (let i = 1; i < pts.length; i++) {
      ctx.lineTo(pts[i].pos.x, pts[i].pos.y);
    }
    ctx.closePath();
    ctx.stroke();
    ctx.restore();
  }

  drawStartLine(track: TrackData, elapsed: number): void {
    const ctx = this.ctx;
    const { a, b } = track.startLine;

    ctx.save();
    // Checkered pattern
    const segLen = Math.hypot(b.x - a.x, b.y - a.y);
    const dir = { x: (b.x - a.x) / segLen, y: (b.y - a.y) / segLen };
    const perp = { x: -dir.y, y: dir.x };
    const segments = 10;
    const segWidth = segLen / segments;

    for (let i = 0; i < segments; i++) {
      for (let row = 0; row < 2; row++) {
        const isWhite = (i + row) % 2 === 0;
        ctx.fillStyle = isWhite ? '#ffffff' : '#1a1a2e';
        const p1 = add(a, scale(dir, i * segWidth));
        const p2 = add(p1, scale(dir, segWidth));
        const offset = scale(perp, row * 6 - 3);
        const p1o = add(p1, offset);
        const p2o = add(p2, offset);
        const p3o = add(p2, add(offset, scale(perp, 6)));
        const p4o = add(p1, add(offset, scale(perp, 6)));
        ctx.beginPath();
        ctx.moveTo(p1o.x, p1o.y);
        ctx.lineTo(p2o.x, p2o.y);
        ctx.lineTo(p3o.x, p3o.y);
        ctx.lineTo(p4o.x, p4o.y);
        ctx.closePath();
        ctx.fill();
      }
    }

    // Glow
    ctx.shadowBlur = 15;
    ctx.shadowColor = '#ffffff';
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(a.x, a.y);
    ctx.lineTo(b.x, b.y);
    ctx.stroke();
    ctx.restore();
  }

  drawBoostPads(track: TrackData, elapsed: number): void {
    const ctx = this.ctx;

    for (const pad of track.boostPads) {
      if (!pad.active) continue;

      ctx.save();
      ctx.translate(pad.pos.x, pad.pos.y);
      ctx.rotate(pad.angle);

      const pulse = 0.6 + 0.4 * Math.sin(elapsed * 8);

      // Glow
      ctx.shadowBlur = 20;
      ctx.shadowColor = '#ffd700';

      // Base
      const grad = ctx.createLinearGradient(0, -pad.width / 2, 0, pad.width / 2);
      grad.addColorStop(0, `rgba(255, 200, 0, ${0.3 * pulse})`);
      grad.addColorStop(0.5, `rgba(255, 215, 0, ${0.7 * pulse})`);
      grad.addColorStop(1, `rgba(255, 200, 0, ${0.3 * pulse})`);
      ctx.fillStyle = grad;
      ctx.fillRect(-pad.length / 2, -pad.width / 2, pad.length, pad.width);

      // Arrows
      ctx.shadowBlur = 0;
      ctx.fillStyle = `rgba(255, 255, 200, ${pulse})`;
      const arrowOffset = (elapsed * 40) % 20;
      for (let i = -1; i <= 1; i++) {
        const ax = i * 18 + arrowOffset - 18;
        ctx.beginPath();
        ctx.moveTo(ax - 6, -pad.width / 2 + 5);
        ctx.lineTo(ax + 4, 0);
        ctx.lineTo(ax - 6, pad.width / 2 - 5);
        ctx.lineTo(ax - 3, 0);
        ctx.closePath();
        ctx.fill();
      }

      ctx.restore();
    }
  }

  drawObstacles(track: TrackData, elapsed: number): void {
    const ctx = this.ctx;

    for (const obs of track.obstacles) {
      ctx.save();
      ctx.translate(obs.pos.x, obs.pos.y);
      ctx.rotate(obs.rotation + elapsed * obs.rotationSpeed);

      // Glow
      ctx.shadowBlur = 15;
      ctx.shadowColor = '#ff4444';

      // Hazard diamond shape
      const r = obs.radius;
      const hitColor = obs.hitTimer > 0 ? '#ffaaaa' : '#ff4444';
      ctx.fillStyle = hitColor;
      ctx.beginPath();
      ctx.moveTo(0, -r);
      ctx.lineTo(r, 0);
      ctx.lineTo(0, r);
      ctx.lineTo(-r, 0);
      ctx.closePath();
      ctx.fill();

      // Inner
      ctx.shadowBlur = 0;
      ctx.fillStyle = '#1a1a2e';
      ctx.beginPath();
      ctx.moveTo(0, -r * 0.6);
      ctx.lineTo(r * 0.6, 0);
      ctx.lineTo(0, r * 0.6);
      ctx.lineTo(-r * 0.6, 0);
      ctx.closePath();
      ctx.fill();

      // Exclamation mark
      ctx.fillStyle = '#ff4444';
      ctx.fillRect(-1.5, -r * 0.35, 3, r * 0.45);
      ctx.beginPath();
      ctx.arc(0, r * 0.25, 2, 0, Math.PI * 2);
      ctx.fill();

      ctx.restore();
    }
  }

  drawHoverboard(hb: Hoverboard, elapsed: number): void {
    const ctx = this.ctx;

    // Trail
    if (hb.trail.length > 1) {
      ctx.save();
      for (let i = 0; i < hb.trail.length - 1; i++) {
        const alpha = (i / hb.trail.length) * 0.5;
        const width = (i / hb.trail.length) * 8;
        ctx.strokeStyle = hb.trailColor.replace('0.6', String(alpha));
        ctx.lineWidth = width;
        ctx.lineCap = 'round';
        ctx.beginPath();
        ctx.moveTo(hb.trail[i].x, hb.trail[i].y);
        ctx.lineTo(hb.trail[i + 1].x, hb.trail[i + 1].y);
        ctx.stroke();
      }
      ctx.restore();
    }

    // Particles
    for (const p of hb.particles) {
      const alpha = p.life / p.maxLife;
      ctx.save();
      ctx.globalAlpha = alpha;
      ctx.fillStyle = p.color;
      ctx.shadowBlur = 8;
      ctx.shadowColor = p.color;
      ctx.beginPath();
      ctx.arc(p.pos.x, p.pos.y, p.size * alpha, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    }

    // Hoverboard body
    ctx.save();
    ctx.translate(hb.pos.x, hb.pos.y);
    ctx.rotate(hb.angle + hb.wobble);

    // Hover glow underneath
    ctx.shadowBlur = 25;
    ctx.shadowColor = hb.glowColor;
    ctx.fillStyle = 'rgba(255,255,255,0.05)';
    ctx.beginPath();
    ctx.ellipse(0, 0, 22, 10, 0, 0, Math.PI * 2);
    ctx.fill();

    // Boost flame effect
    if (hb.isBoosting || hb.boostFlash > 0) {
      ctx.shadowBlur = 30;
      ctx.shadowColor = '#ffaa00';
      const flameLen = 15 + Math.random() * 10;
      const grad = ctx.createLinearGradient(-18, 0, -18 - flameLen, 0);
      grad.addColorStop(0, 'rgba(255, 200, 0, 0.9)');
      grad.addColorStop(0.5, 'rgba(255, 100, 0, 0.6)');
      grad.addColorStop(1, 'rgba(255, 50, 0, 0)');
      ctx.fillStyle = grad;
      ctx.beginPath();
      ctx.moveTo(-18, -7);
      ctx.lineTo(-18 - flameLen, 0);
      ctx.lineTo(-18, 7);
      ctx.closePath();
      ctx.fill();
    }

    // Board base (dark)
    ctx.shadowBlur = 0;
    ctx.fillStyle = '#1a1a2e';
    ctx.beginPath();
    ctx.ellipse(0, 0, 20, 8, 0, 0, Math.PI * 2);
    ctx.fill();

    // Board top (colored)
    const boardGrad = ctx.createLinearGradient(0, -8, 0, 8);
    boardGrad.addColorStop(0, hb.glowColor);
    boardGrad.addColorStop(0.5, hb.color);
    boardGrad.addColorStop(1, hb.color);
    ctx.fillStyle = boardGrad;
    ctx.beginPath();
    ctx.ellipse(0, 0, 18, 6, 0, 0, Math.PI * 2);
    ctx.fill();

    // Board stripe
    ctx.fillStyle = 'rgba(255, 255, 255, 0.4)';
    ctx.beginPath();
    ctx.ellipse(0, 0, 14, 2, 0, 0, Math.PI * 2);
    ctx.fill();

    // Front nose highlight
    ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
    ctx.beginPath();
    ctx.ellipse(14, 0, 4, 3, 0, 0, Math.PI * 2);
    ctx.fill();

    // Rider figure
    ctx.shadowBlur = 5;
    ctx.shadowColor = hb.glowColor;
    // Body
    ctx.fillStyle = hb.color;
    ctx.beginPath();
    ctx.ellipse(-2, -5, 7, 9, -0.2, 0, Math.PI * 2);
    ctx.fill();

    // Head
    ctx.shadowBlur = 0;
    ctx.fillStyle = '#f0d0a0';
    ctx.beginPath();
    ctx.arc(0, -12, 4.5, 0, Math.PI * 2);
    ctx.fill();

    // Helmet
    ctx.fillStyle = hb.color;
    ctx.beginPath();
    ctx.arc(0, -13, 5, Math.PI, 0);
    ctx.fill();
    ctx.fillRect(-5, -13, 10, 2);

    // Visor
    ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
    ctx.beginPath();
    ctx.ellipse(1, -12, 3, 2, 0, 0, Math.PI * 2);
    ctx.fill();

    // Hit flash
    if (hb.hitFlash > 0) {
      ctx.globalAlpha = hb.hitFlash * 0.5;
      ctx.fillStyle = '#ffffff';
      ctx.beginPath();
      ctx.ellipse(0, 0, 22, 10, 0, 0, Math.PI * 2);
      ctx.fill();
      ctx.globalAlpha = 1;
    }

    ctx.restore();
  }

  drawCountdown(count: number, scale: number, alpha: number): void {
    const ctx = this.ctx;
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.font = `bold ${Math.floor(120 * scale)}px system-ui, sans-serif`;
    ctx.shadowBlur = 30;
    ctx.shadowColor = '#ff2d95';
    ctx.fillStyle = '#ffffff';
    const text = count > 0 ? String(count) : 'GO!';
    ctx.fillText(text, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2);
    ctx.restore();
  }

  drawHUD(
    players: Hoverboard[],
    elapsed: number,
    totalLaps: number,
    mode: string,
  ): void {
    const ctx = this.ctx;

    // Top bar background
    ctx.save();
    ctx.fillStyle = 'rgba(10, 10, 30, 0.7)';
    ctx.fillRect(0, 0, CANVAS_WIDTH, 50);

    // Time
    ctx.fillStyle = '#ffffff';
    ctx.font = 'bold 18px system-ui, sans-serif';
    ctx.textAlign = 'left';
    ctx.textBaseline = 'middle';
    const mins = Math.floor(elapsed / 60);
    const secs = Math.floor(elapsed % 60);
    const ms = Math.floor((elapsed % 1) * 100);
    ctx.fillText(`Time: ${mins}:${String(secs).padStart(2, '0')}.${String(ms).padStart(2, '0')}`, 20, 25);

    // Mode
    ctx.textAlign = 'center';
    ctx.fillStyle = '#8888aa';
    ctx.font = '14px system-ui, sans-serif';
    ctx.fillText(mode === 'vs-computer' ? 'VS COMPUTER' : 'TWO PLAYER', CANVAS_WIDTH / 2, 25);

    ctx.restore();

    // Player info panels
    for (let i = 0; i < players.length; i++) {
      const hb = players[i];
      const panelX = i === 0 ? 10 : CANVAS_WIDTH - 210;
      const panelY = CANVAS_HEIGHT - 80;

      ctx.save();
      // Panel background
      ctx.fillStyle = 'rgba(10, 10, 30, 0.75)';
      ctx.strokeStyle = hb.color;
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.roundRect(panelX, panelY, 200, 70, 8);
      ctx.fill();
      ctx.stroke();

      // Name
      ctx.fillStyle = hb.color;
      ctx.font = 'bold 14px system-ui, sans-serif';
      ctx.textAlign = 'left';
      ctx.textBaseline = 'top';
      ctx.fillText(hb.name, panelX + 12, panelY + 8);

      // Lap
      ctx.fillStyle = '#ffffff';
      ctx.font = '13px system-ui, sans-serif';
      const displayLaps = Math.min(hb.laps + 1, totalLaps);
      ctx.fillText(`Lap: ${displayLaps}/${totalLaps}`, panelX + 12, panelY + 28);

      // Speed
      ctx.fillStyle = '#aaaacc';
      ctx.fillText(`Speed: ${Math.round(hb.speed * 20)} mph`, panelX + 12, panelY + 46);

      // Energy bar
      const barX = panelX + 100;
      const barY = panelY + 30;
      const barW = 88;
      const barH = 8;
      ctx.fillStyle = 'rgba(255,255,255,0.1)';
      ctx.fillRect(barX, barY, barW, barH);
      const energyPct = hb.energy / 100;
      const energyGrad = ctx.createLinearGradient(barX, 0, barX + barW, 0);
      energyGrad.addColorStop(0, '#00ff88');
      energyGrad.addColorStop(0.6, '#ffdd00');
      energyGrad.addColorStop(1, '#ff4444');
      ctx.fillStyle = energyGrad;
      ctx.fillRect(barX, barY, barW * energyPct, barH);

      // Energy label
      ctx.fillStyle = '#8888aa';
      ctx.font = '10px system-ui, sans-serif';
      ctx.fillText('BOOST', barX, barY - 12);

      // Position indicator
      ctx.fillStyle = '#ffffff';
      ctx.font = 'bold 12px system-ui, sans-serif';
      ctx.textAlign = 'right';
      const sorted = [...players].sort((a, b) => b.getProgress({ totalLength: 1 } as any) - a.getProgress({ totalLength: 1 } as any));
      // Simplified position display
      const pos = i === 0 ? 'P1' : 'P2';
      ctx.fillText(pos, panelX + 190, panelY + 8);

      ctx.restore();
    }
  }

  drawAll(
    track: TrackData,
    players: Hoverboard[],
    elapsed: number,
    countdown: number,
    countdownScale: number,
    countdownAlpha: number,
    totalLaps: number,
    mode: string,
  ): void {
    this.clear();
    this.drawBackground(elapsed);
    this.drawTrack(track, elapsed);
    this.drawStartLine(track, elapsed);
    this.drawBoostPads(track, elapsed);
    this.drawObstacles(track, elapsed);

    for (const hb of players) {
      this.drawHoverboard(hb, elapsed);
    }

    if (countdown >= 0) {
      this.drawCountdown(countdown, countdownScale, countdownAlpha);
    }

    this.drawHUD(players, elapsed, totalLaps, mode);
  }
}
