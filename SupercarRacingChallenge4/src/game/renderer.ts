import { TRACK_LENGTH, NUM_LANES } from '@/game/types';
import type { GameState, CarState } from '@/game/engine';

const LANE_WIDTH = 100;
const ROAD_WIDTH = LANE_WIDTH * NUM_LANES;

export function renderGame(
  ctx: CanvasRenderingContext2D,
  state: GameState,
  width: number,
  height: number,
  time: number,
): void {
  ctx.clearRect(0, 0, width, height);

  drawBackground(ctx, width, height, time);

  const roadX = width / 2 - ROAD_WIDTH / 2;
  const cameraY = state.cameraY;

  drawRoad(ctx, roadX, ROAD_WIDTH, height);
  drawLaneDividers(ctx, roadX, height, cameraY);
  drawObstacles(ctx, state.obstacles, roadX, height, cameraY, time);
  drawStartLine(ctx, roadX, ROAD_WIDTH, height, cameraY);
  drawFinishLine(ctx, roadX, ROAD_WIDTH, height, cameraY, TRACK_LENGTH);

  for (const car of state.cars) {
    drawCar(ctx, car, roadX, height, cameraY, time);
  }

  drawMinimap(ctx, state, width, height);
  drawHUD(ctx, state, width, height);
  drawCountdown(ctx, state, width, height);
}

function drawBackground(ctx: CanvasRenderingContext2D, width: number, height: number, time: number): void {
  const grad = ctx.createLinearGradient(0, 0, 0, height);
  grad.addColorStop(0, '#08081a');
  grad.addColorStop(0.5, '#0d0d2e');
  grad.addColorStop(1, '#05050f');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, width, height);

  ctx.strokeStyle = 'rgba(0, 240, 255, 0.06)';
  ctx.lineWidth = 1;
  const gridSize = 40;
  const offset = (time * 60) % gridSize;
  for (let x = 0; x < width; x += gridSize) {
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, height);
    ctx.stroke();
  }
  for (let y = -offset; y < height; y += gridSize) {
    ctx.beginPath();
    ctx.moveTo(0, y);
    ctx.lineTo(width, y);
    ctx.stroke();
  }
}

function drawRoad(ctx: CanvasRenderingContext2D, roadX: number, roadWidth: number, height: number): void {
  const grad = ctx.createLinearGradient(roadX, 0, roadX + roadWidth, 0);
  grad.addColorStop(0, '#15152e');
  grad.addColorStop(0.5, '#1a1a38');
  grad.addColorStop(1, '#15152e');
  ctx.fillStyle = grad;
  ctx.fillRect(roadX, 0, roadWidth, height);

  ctx.shadowBlur = 15;
  ctx.lineWidth = 3;
  ctx.strokeStyle = 'rgba(0, 240, 255, 0.8)';
  ctx.shadowColor = 'rgba(0, 240, 255, 0.9)';
  ctx.beginPath();
  ctx.moveTo(roadX, 0);
  ctx.lineTo(roadX, height);
  ctx.stroke();

  ctx.strokeStyle = 'rgba(255, 43, 214, 0.8)';
  ctx.shadowColor = 'rgba(255, 43, 214, 0.9)';
  ctx.beginPath();
  ctx.moveTo(roadX + roadWidth, 0);
  ctx.lineTo(roadX + roadWidth, height);
  ctx.stroke();
  ctx.shadowBlur = 0;
}

function drawLaneDividers(ctx: CanvasRenderingContext2D, roadX: number, height: number, cameraY: number): void {
  const dashLen = 40;
  const gapLen = 30;
  const cycle = dashLen + gapLen;

  ctx.strokeStyle = 'rgba(255, 255, 255, 0.25)';
  ctx.lineWidth = 2;
  ctx.setLineDash([dashLen, gapLen]);

  for (let lane = 1; lane < NUM_LANES; lane++) {
    const x = roadX + LANE_WIDTH * lane;
    ctx.lineDashOffset = cameraY % cycle;
    ctx.beginPath();
    ctx.moveTo(x, 0);
    ctx.lineTo(x, height);
    ctx.stroke();
  }
  ctx.setLineDash([]);
}

function drawObstacles(ctx: CanvasRenderingContext2D, obstacles: { position: number; lane: number; hit: boolean }[], roadX: number, height: number, cameraY: number, time: number): void {
  for (const obs of obstacles) {
    if (obs.hit) continue;
    const screenY = worldToScreenY(obs.position, cameraY, height);
    if (screenY < -40 || screenY > height + 40) continue;

    const x = roadX + LANE_WIDTH * (obs.lane + 0.5);
    const pulse = 0.7 + Math.sin(time * 6 + obs.position * 0.01) * 0.3;

    ctx.save();
    ctx.shadowBlur = 15 * pulse;
    ctx.shadowColor = 'rgba(255, 107, 26, 0.9)';

    // Barrier
    ctx.fillStyle = `rgba(255, 107, 26, ${pulse})`;
    ctx.fillRect(x - 35, screenY - 10, 70, 20);

    // Stripes
    ctx.fillStyle = 'rgba(255, 230, 0, 0.8)';
    for (let sx = -30; sx < 30; sx += 12) {
      ctx.fillRect(x + sx, screenY - 10, 5, 20);
    }
    ctx.restore();
  }
}

function drawStartLine(ctx: CanvasRenderingContext2D, roadX: number, roadWidth: number, height: number, cameraY: number): void {
  const screenY = worldToScreenY(0, cameraY, height);
  if (screenY < -10 || screenY > height + 10) return;
  ctx.fillStyle = 'rgba(255, 255, 255, 0.2)';
  ctx.fillRect(roadX, screenY - 5, roadWidth, 10);
}

function drawFinishLine(ctx: CanvasRenderingContext2D, roadX: number, roadWidth: number, height: number, cameraY: number, trackLength: number): void {
  const screenY = worldToScreenY(trackLength, cameraY, height);
  if (screenY < -20 || screenY > height + 20) return;

  const checkSize = 15;
  for (let r = 0; r < 2; r++) {
    for (let x = 0; x < roadWidth; x += checkSize) {
      const col = Math.floor(x / checkSize);
      ctx.fillStyle = (col + r) % 2 === 0 ? '#ffffff' : '#0a0a1a';
      ctx.fillRect(roadX + x, screenY + r * checkSize - checkSize, checkSize, checkSize);
    }
  }
}

function worldToScreenY(worldY: number, cameraY: number, height: number): number {
  return height - 120 - (worldY - cameraY);
}

function drawCar(ctx: CanvasRenderingContext2D, car: CarState, roadX: number, height: number, cameraY: number, time: number): void {
  const laneX = roadX + LANE_WIDTH * (car.visualLane + 0.5);
  const screenY = worldToScreenY(car.position, cameraY, height);
  if (screenY < -50 || screenY > height + 50) return;

  const wobbleX = Math.sin(car.wobble) * 1.5;
  const cx = laneX + wobbleX;
  const cy = screenY;

  if (car.isBoosting) {
    drawBoostTrail(ctx, cx, cy, car.glowColor);
  }

  const carW = 34;
  const carH = 56;

  ctx.save();
  ctx.shadowBlur = car.hitFlash > 0 ? 30 : 20;
  ctx.shadowColor = car.hitFlash > 0 ? '#ff3333' : car.glowColor;

  // Hit flash makes car red
  const bodyColor = car.hitFlash > 0 ? '#ff3333' : car.color;

  ctx.fillStyle = bodyColor;
  roundRect(ctx, cx - carW / 2, cy - carH / 2, carW, carH, 8);
  ctx.fill();

  ctx.shadowBlur = 0;

  // Accent stripe
  ctx.fillStyle = car.accentColor;
  ctx.fillRect(cx - 3, cy - carH / 2 + 6, 6, carH - 12);

  // Windshield
  ctx.fillStyle = 'rgba(20, 20, 40, 0.85)';
  roundRect(ctx, cx - 11, cy - carH / 2 + 12, 22, 14, 4);
  ctx.fill();

  // Rear window
  ctx.fillStyle = 'rgba(20, 20, 40, 0.6)';
  roundRect(ctx, cx - 10, cy + carH / 2 - 20, 20, 10, 3);
  ctx.fill();

  // Headlights
  ctx.fillStyle = '#ffffff';
  ctx.shadowBlur = 10;
  ctx.shadowColor = '#ffffff';
  ctx.beginPath();
  ctx.arc(cx - 9, cy - carH / 2 + 5, 2.5, 0, Math.PI * 2);
  ctx.arc(cx + 9, cy - carH / 2 + 5, 2.5, 0, Math.PI * 2);
  ctx.fill();

  // Taillights
  ctx.fillStyle = '#ff3333';
  ctx.shadowBlur = 8;
  ctx.shadowColor = '#ff3333';
  ctx.beginPath();
  ctx.arc(cx - 9, cy + carH / 2 - 4, 2.5, 0, Math.PI * 2);
  ctx.arc(cx + 9, cy + carH / 2 - 4, 2.5, 0, Math.PI * 2);
  ctx.fill();
  ctx.restore();
}

function drawBoostTrail(ctx: CanvasRenderingContext2D, x: number, y: number, color: string): void {
  ctx.save();
  for (let i = 0; i < 6; i++) {
    const trailY = y + 28 + i * 16;
    const alpha = (1 - i / 6) * 0.5;
    const width = 26 - i * 3;
    const c = color.replace(/0\.\d+/, String(alpha));
    ctx.fillStyle = c;
    ctx.shadowBlur = 18;
    ctx.shadowColor = color;
    ctx.beginPath();
    ctx.ellipse(x, trailY, width / 2, 9, 0, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.restore();
}

function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number): void {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + w - r, y);
  ctx.quadraticCurveTo(x + w, y, x + w, y + r);
  ctx.lineTo(x + w, y + h - r);
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h);
  ctx.lineTo(x + r, y + h);
  ctx.quadraticCurveTo(x, y + h, x, y + h - r);
  ctx.lineTo(x, y + r);
  ctx.quadraticCurveTo(x, y, x + r, y);
  ctx.closePath();
}

function drawMinimap(ctx: CanvasRenderingContext2D, state: GameState, width: number, height: number): void {
  const mapX = width - 40;
  const mapY = 20;
  const mapH = height - 140;
  const mapW = 14;

  ctx.fillStyle = 'rgba(20, 20, 50, 0.6)';
  ctx.fillRect(mapX, mapY, mapW, mapH);
  ctx.strokeStyle = 'rgba(0, 240, 255, 0.4)';
  ctx.lineWidth = 1;
  ctx.strokeRect(mapX, mapY, mapW, mapH);

  // Obstacle dots
  for (const obs of state.obstacles) {
    if (obs.hit) continue;
    const ratio = obs.position / TRACK_LENGTH;
    const dotY = mapY + mapH - ratio * mapH;
    ctx.fillStyle = 'rgba(255, 107, 26, 0.6)';
    ctx.fillRect(mapX + 3, dotY - 1, mapW - 6, 2);
  }

  for (const car of state.cars) {
    const ratio = car.position / TRACK_LENGTH;
    const dotY = mapY + mapH - ratio * mapH;
    ctx.fillStyle = car.color;
    ctx.shadowBlur = 6;
    ctx.shadowColor = car.glowColor;
    ctx.beginPath();
    ctx.arc(mapX + mapW / 2, dotY, 3.5, 0, Math.PI * 2);
    ctx.fill();
  }
  ctx.shadowBlur = 0;
}

function drawHUD(ctx: CanvasRenderingContext2D, state: GameState, width: number, height: number): void {
  for (let i = 0; i < state.cars.length; i++) {
    const car = state.cars[i];
    const isLeft = i === 0;
    const x = isLeft ? 30 : width - 30;
    const align = isLeft ? 'left' : 'right';

    ctx.textAlign = align;
    ctx.font = '700 18px Orbitron, sans-serif';
    ctx.fillStyle = car.color;
    ctx.shadowBlur = 8;
    ctx.shadowColor = car.glowColor;
    const label = car.isAI ? `CPU: ${car.name}` : `P${i + 1}: ${car.name}`;
    ctx.fillText(label, x, 30);

    ctx.font = '600 14px Rajdhani, sans-serif';
    ctx.fillStyle = '#ffffff';
    ctx.shadowBlur = 0;
    ctx.fillText(`${Math.round(car.speed * 0.8)} km/h`, x, 50);

    // Boost bar
    const barW = 120;
    const barX = isLeft ? 30 : width - 30 - barW;
    const barY = 58;
    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.fillRect(barX, barY, barW, 6);
    ctx.fillStyle = car.isBoosting ? '#ffe600' : car.color;
    ctx.shadowBlur = car.isBoosting ? 10 : 0;
    ctx.shadowColor = car.isBoosting ? '#ffe600' : car.glowColor;
    ctx.fillRect(barX, barY, barW * (car.boost / 100), 6);
    ctx.shadowBlur = 0;

    // Progress
    ctx.font = '500 12px Rajdhani, sans-serif';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
    ctx.fillText(`${Math.round((car.position / TRACK_LENGTH) * 100)}%`, x, 78);

    if (car.finished) {
      ctx.font = '900 16px Orbitron, sans-serif';
      ctx.fillStyle = '#0fff8b';
      ctx.shadowBlur = 10;
      ctx.shadowColor = 'rgba(15, 255, 139, 0.8)';
      ctx.fillText('FINISHED!', x, 98);
      ctx.shadowBlur = 0;
    }
  }

  if (state.racing && !state.finished) {
    ctx.textAlign = 'center';
    ctx.font = '700 22px Orbitron, sans-serif';
    ctx.fillStyle = '#ffffff';
    ctx.shadowBlur = 8;
    ctx.shadowColor = 'rgba(0, 240, 255, 0.6)';
    ctx.fillText(formatTime(state.elapsedTime), width / 2, 35);
    ctx.shadowBlur = 0;
  }
}

function drawCountdown(ctx: CanvasRenderingContext2D, state: GameState, width: number, height: number): void {
  if (state.countdown <= 0 || state.racing) return;
  const count = Math.ceil(state.countdown - 0.5);
  const text = count > 0 ? String(count) : 'GO!';

  ctx.save();
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.font = '900 100px Orbitron, sans-serif';
  ctx.fillStyle = count > 0 ? '#00f0ff' : '#0fff8b';
  ctx.shadowBlur = 30;
  ctx.shadowColor = count > 0 ? 'rgba(0, 240, 255, 0.8)' : 'rgba(15, 255, 139, 0.8)';
  ctx.fillText(text, width / 2, height / 2);
  ctx.restore();
}

function formatTime(seconds: number): string {
  const s = Math.floor(seconds);
  const ms = Math.floor((seconds - s) * 100);
  return `${String(Math.floor(s / 60)).padStart(2, '0')}:${String(s % 60).padStart(2, '0')}.${String(ms).padStart(2, '0')}`;
}
