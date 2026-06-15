import type { GameState, Player } from './types';
import {
  CANVAS_W, CANVAS_H,
  RINK_X, RINK_Y, RINK_W, RINK_H, CORNER_R,
  PLAYER_R, PUCK_R,
  GOAL_DEPTH, GOAL_H,
  TEAM_COLORS, TEAM_DARK, TEAM_LIGHT, TEAM_NAMES,
} from './constants';
import { GOALS } from './engine';

const ICE_COLOR = '#daf0f7';
const ICE_SHADOW = '#c5e4ef';
const RED_LINE = '#e02020';
const BLUE_LINE = '#1a55d4';
const CREASE_COLOR = 'rgba(100,160,230,0.25)';
const FACEOFF_COLOR = 'rgba(220,30,30,0.7)';
const CENTER_COLOR = 'rgba(220,30,30,0.5)';

function drawRoundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number): void {
  ctx.beginPath();
  ctx.moveTo(x + r, y);
  ctx.lineTo(x + w - r, y);
  ctx.arcTo(x + w, y, x + w, y + r, r);
  ctx.lineTo(x + w, y + h - r);
  ctx.arcTo(x + w, y + h, x + w - r, y + h, r);
  ctx.lineTo(x + r, y + h);
  ctx.arcTo(x, y + h, x, y + h - r, r);
  ctx.lineTo(x, y + r);
  ctx.arcTo(x, y, x + r, y, r);
  ctx.closePath();
}

function drawRink(ctx: CanvasRenderingContext2D): void {
  // Outer boards
  ctx.save();
  drawRoundRect(ctx, RINK_X - 4, RINK_Y - 4, RINK_W + 8, RINK_H + 8, CORNER_R + 4);
  ctx.fillStyle = '#8aa0b0';
  ctx.fill();

  // Ice surface
  drawRoundRect(ctx, RINK_X, RINK_Y, RINK_W, RINK_H, CORNER_R);
  ctx.fillStyle = ICE_COLOR;
  ctx.fill();
  ctx.strokeStyle = '#fff';
  ctx.lineWidth = 3;
  ctx.stroke();

  ctx.restore();

  // Clip everything to rink
  ctx.save();
  drawRoundRect(ctx, RINK_X, RINK_Y, RINK_W, RINK_H, CORNER_R);
  ctx.clip();

  const cx = RINK_X + RINK_W / 2;
  const cy = RINK_Y + RINK_H / 2;

  // Goal creases
  for (const g of GOALS) {
    const side = g.defendingTeam === 0 ? 1 : -1;
    const creaseX = g.defendingTeam === 0 ? RINK_X : RINK_X + RINK_W - 60;
    ctx.beginPath();
    ctx.ellipse(creaseX, cy, 60, 50, 0, -Math.PI / 2, Math.PI / 2, g.defendingTeam === 1);
    ctx.fillStyle = CREASE_COLOR;
    ctx.fill();
  }

  // Red goal lines
  for (const g of GOALS) {
    const lineX = g.defendingTeam === 0 ? RINK_X + 55 : RINK_X + RINK_W - 55;
    ctx.beginPath();
    ctx.moveTo(lineX, RINK_Y + 10);
    ctx.lineTo(lineX, RINK_Y + RINK_H - 10);
    ctx.strokeStyle = RED_LINE;
    ctx.lineWidth = 3;
    ctx.stroke();
  }

  // Blue lines (at 1/3 and 2/3)
  const bl1 = RINK_X + RINK_W * 0.32;
  const bl2 = RINK_X + RINK_W * 0.68;
  for (const bx of [bl1, bl2]) {
    ctx.beginPath();
    ctx.moveTo(bx, RINK_Y);
    ctx.lineTo(bx, RINK_Y + RINK_H);
    ctx.strokeStyle = BLUE_LINE;
    ctx.lineWidth = 6;
    ctx.stroke();
  }

  // Center red line
  ctx.beginPath();
  ctx.moveTo(cx, RINK_Y);
  ctx.lineTo(cx, RINK_Y + RINK_H);
  ctx.strokeStyle = RED_LINE;
  ctx.lineWidth = 3;
  ctx.setLineDash([12, 8]);
  ctx.stroke();
  ctx.setLineDash([]);

  // Center circle
  ctx.beginPath();
  ctx.arc(cx, cy, 60, 0, Math.PI * 2);
  ctx.strokeStyle = CENTER_COLOR;
  ctx.lineWidth = 2.5;
  ctx.stroke();

  // Center dot
  ctx.beginPath();
  ctx.arc(cx, cy, 5, 0, Math.PI * 2);
  ctx.fillStyle = CENTER_COLOR;
  ctx.fill();

  // Face-off circles
  const foPositions = [
    { x: bl1 - 20, y: cy - 100 },
    { x: bl1 - 20, y: cy + 100 },
    { x: bl2 + 20, y: cy - 100 },
    { x: bl2 + 20, y: cy + 100 },
  ];
  for (const fo of foPositions) {
    ctx.beginPath();
    ctx.arc(fo.x, fo.y, 40, 0, Math.PI * 2);
    ctx.strokeStyle = FACEOFF_COLOR;
    ctx.lineWidth = 2;
    ctx.stroke();
    ctx.beginPath();
    ctx.arc(fo.x, fo.y, 4, 0, Math.PI * 2);
    ctx.fillStyle = FACEOFF_COLOR;
    ctx.fill();
  }

  ctx.restore();
}

function drawGoals(ctx: CanvasRenderingContext2D): void {
  for (const g of GOALS) {
    const isLeft = g.defendingTeam === 0;
    const postX = isLeft ? RINK_X : RINK_X + RINK_W;

    // Net (mesh background)
    ctx.fillStyle = 'rgba(180,200,220,0.4)';
    ctx.fillRect(g.x, g.y, g.width, g.height);

    // Net lines
    ctx.strokeStyle = 'rgba(150,170,190,0.6)';
    ctx.lineWidth = 1;
    const netLines = 6;
    for (let i = 1; i < netLines; i++) {
      const t = (i / netLines) * g.height + g.y;
      ctx.beginPath();
      ctx.moveTo(g.x, t);
      ctx.lineTo(g.x + g.width, t);
      ctx.stroke();
    }

    // Posts (red)
    ctx.strokeStyle = '#dd2222';
    ctx.lineWidth = 4;
    ctx.lineCap = 'round';

    // Top post
    ctx.beginPath();
    ctx.moveTo(postX, g.y);
    ctx.lineTo(isLeft ? g.x : g.x + g.width, g.y);
    ctx.stroke();

    // Bottom post
    ctx.beginPath();
    ctx.moveTo(postX, g.y + g.height);
    ctx.lineTo(isLeft ? g.x : g.x + g.width, g.y + g.height);
    ctx.stroke();

    // Back post
    ctx.beginPath();
    ctx.moveTo(isLeft ? g.x : g.x + g.width, g.y);
    ctx.lineTo(isLeft ? g.x : g.x + g.width, g.y + g.height);
    ctx.stroke();
  }
}

function drawPlayer(ctx: CanvasRenderingContext2D, player: Player): void {
  const { pos, team, isSelected, hasPuck, facingAngle, number, role } = player;
  const color = TEAM_COLORS[team];
  const dark = TEAM_DARK[team];
  const light = TEAM_LIGHT[team];

  // Selection ring
  if (isSelected) {
    ctx.beginPath();
    ctx.arc(pos.x, pos.y, PLAYER_R + 5, 0, Math.PI * 2);
    ctx.strokeStyle = 'rgba(255,255,100,0.85)';
    ctx.lineWidth = 2.5;
    ctx.stroke();
  }

  // Shadow
  ctx.beginPath();
  ctx.ellipse(pos.x + 2, pos.y + 3, PLAYER_R, PLAYER_R * 0.6, 0, 0, Math.PI * 2);
  ctx.fillStyle = 'rgba(0,0,0,0.18)';
  ctx.fill();

  // Body gradient
  const grad = ctx.createRadialGradient(pos.x - 4, pos.y - 4, 2, pos.x, pos.y, PLAYER_R);
  grad.addColorStop(0, light);
  grad.addColorStop(1, dark);
  ctx.beginPath();
  ctx.arc(pos.x, pos.y, PLAYER_R, 0, Math.PI * 2);
  ctx.fillStyle = grad;
  ctx.fill();
  ctx.strokeStyle = dark;
  ctx.lineWidth = 1.5;
  ctx.stroke();

  // Helmet
  ctx.beginPath();
  ctx.arc(pos.x, pos.y - 3, PLAYER_R * 0.65, Math.PI, 0);
  ctx.fillStyle = 'rgba(0,0,0,0.25)';
  ctx.fill();

  // Facing direction indicator (stick)
  ctx.beginPath();
  ctx.moveTo(pos.x, pos.y);
  ctx.lineTo(pos.x + Math.cos(facingAngle) * (PLAYER_R + 8), pos.y + Math.sin(facingAngle) * (PLAYER_R + 8));
  ctx.strokeStyle = '#333';
  ctx.lineWidth = 3;
  ctx.lineCap = 'round';
  ctx.stroke();

  // Jersey number
  ctx.fillStyle = '#fff';
  ctx.font = `bold ${role === 'goalie' ? 11 : 10}px monospace`;
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(String(number), pos.x, pos.y + 3);

  // Goalie mask indicator
  if (role === 'goalie') {
    ctx.beginPath();
    ctx.arc(pos.x, pos.y, PLAYER_R + 2, -0.4, 0.4);
    ctx.strokeStyle = '#ffd700';
    ctx.lineWidth = 2;
    ctx.stroke();
  }

  // Puck possession indicator
  if (hasPuck) {
    ctx.beginPath();
    ctx.arc(pos.x, pos.y - PLAYER_R - 7, 4, 0, Math.PI * 2);
    ctx.fillStyle = '#ffe000';
    ctx.fill();
  }

  ctx.textAlign = 'left';
  ctx.textBaseline = 'alphabetic';
}

function drawPuck(ctx: CanvasRenderingContext2D, puck: { pos: { x: number; y: number }; vel: { x: number; y: number } }): void {
  const { pos, vel } = puck;

  // Motion trail
  const speed = Math.sqrt(vel.x * vel.x + vel.y * vel.y);
  if (speed > 2) {
    const trailLen = Math.min(speed * 3, 30);
    const grad = ctx.createLinearGradient(
      pos.x, pos.y,
      pos.x - (vel.x / speed) * trailLen,
      pos.y - (vel.y / speed) * trailLen
    );
    grad.addColorStop(0, 'rgba(50,50,50,0.5)');
    grad.addColorStop(1, 'rgba(50,50,50,0)');
    ctx.beginPath();
    ctx.moveTo(pos.x - (vel.y / speed) * PUCK_R * 0.6, pos.y + (vel.x / speed) * PUCK_R * 0.6);
    ctx.lineTo(pos.x - (vel.x / speed) * trailLen, pos.y - (vel.y / speed) * trailLen);
    ctx.lineTo(pos.x + (vel.y / speed) * PUCK_R * 0.6, pos.y - (vel.x / speed) * PUCK_R * 0.6);
    ctx.closePath();
    ctx.fillStyle = grad;
    ctx.fill();
  }

  // Shadow
  ctx.beginPath();
  ctx.ellipse(pos.x + 2, pos.y + 2, PUCK_R, PUCK_R * 0.7, 0, 0, Math.PI * 2);
  ctx.fillStyle = 'rgba(0,0,0,0.2)';
  ctx.fill();

  // Puck body
  ctx.beginPath();
  ctx.arc(pos.x, pos.y, PUCK_R, 0, Math.PI * 2);
  const pg = ctx.createRadialGradient(pos.x - 2, pos.y - 2, 1, pos.x, pos.y, PUCK_R);
  pg.addColorStop(0, '#555');
  pg.addColorStop(1, '#111');
  ctx.fillStyle = pg;
  ctx.fill();
  ctx.strokeStyle = '#000';
  ctx.lineWidth = 1;
  ctx.stroke();
}

function drawHUD(ctx: CanvasRenderingContext2D, state: GameState): void {
  const { scores, timeRemaining, period, totalPeriods } = state;

  // Scoreboard background
  const sbW = 340;
  const sbH = 52;
  const sbX = CANVAS_W / 2 - sbW / 2;
  const sbY = 6;

  ctx.fillStyle = 'rgba(10,20,40,0.92)';
  ctx.beginPath();
  ctx.roundRect(sbX, sbY, sbW, sbH, 10);
  ctx.fill();
  ctx.strokeStyle = 'rgba(255,255,255,0.15)';
  ctx.lineWidth = 1;
  ctx.stroke();

  // Team 0 score
  ctx.fillStyle = TEAM_COLORS[0];
  ctx.font = 'bold 14px sans-serif';
  ctx.textAlign = 'right';
  ctx.fillText(TEAM_NAMES[0], sbX + 130, sbY + 20);
  ctx.font = 'bold 28px monospace';
  ctx.fillText(String(scores[0]), sbX + 130, sbY + 45);

  // Divider
  ctx.fillStyle = 'rgba(255,255,255,0.3)';
  ctx.fillRect(sbX + sbW / 2 - 1, sbY + 6, 2, sbH - 12);

  // Team 1 score
  ctx.fillStyle = TEAM_COLORS[1];
  ctx.font = 'bold 14px sans-serif';
  ctx.textAlign = 'left';
  ctx.fillText(TEAM_NAMES[1], sbX + sbW - 130, sbY + 20);
  ctx.font = 'bold 28px monospace';
  ctx.fillText(String(scores[1]), sbX + sbW - 130, sbY + 45);

  // Time + period (center)
  const mins = Math.floor(timeRemaining / 60).toString().padStart(2, '0');
  const secs = Math.floor(timeRemaining % 60).toString().padStart(2, '0');
  ctx.fillStyle = '#fff';
  ctx.font = 'bold 16px monospace';
  ctx.textAlign = 'center';
  ctx.fillText(`${mins}:${secs}`, sbX + sbW / 2, sbY + 26);
  ctx.font = '11px sans-serif';
  ctx.fillStyle = '#aac';
  ctx.fillText(`PERIOD ${period}/${totalPeriods}`, sbX + sbW / 2, sbY + 44);

  ctx.textAlign = 'left';
}

function drawControls(ctx: CanvasRenderingContext2D, state: GameState): void {
  const y = RINK_Y + RINK_H + 14;
  ctx.font = '11px monospace';
  ctx.fillStyle = 'rgba(255,255,255,0.65)';
  ctx.textAlign = 'left';
  ctx.fillText('P1: WASD Move  |  F Shoot  |  G Pass  |  Tab Switch', RINK_X, y);
  if (state.playerMode === 'two_player') {
    ctx.textAlign = 'right';
    ctx.fillText('P2: Arrow Move  |  L Shoot  |  . Pass  |  Shift Switch', RINK_X + RINK_W, y);
  } else {
    ctx.textAlign = 'right';
    ctx.fillStyle = TEAM_COLORS[1];
    ctx.fillText('CPU TEAM', RINK_X + RINK_W, y);
  }
  ctx.textAlign = 'left';
}

function drawGoalFlash(ctx: CanvasRenderingContext2D, state: GameState): void {
  if (state.mode !== 'goal_scored') return;
  const team = state.lastScoringTeam!;
  const alpha = Math.min(1, state.goalCooldown / 30) * 0.4;

  ctx.fillStyle = TEAM_COLORS[team].replace(')', `, ${alpha})`).replace('rgb', 'rgba');
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);

  ctx.fillStyle = '#fff';
  ctx.font = 'bold 64px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';

  const pulse = 1 + Math.sin(Date.now() / 120) * 0.05;
  ctx.save();
  ctx.translate(CANVAS_W / 2, CANVAS_H / 2 - 20);
  ctx.scale(pulse, pulse);
  ctx.fillStyle = TEAM_COLORS[team];
  ctx.fillText('GOAL!', 0, 0);
  ctx.restore();

  ctx.font = 'bold 24px sans-serif';
  ctx.fillStyle = '#fff';
  ctx.fillText(TEAM_NAMES[team] + ' scores!', CANVAS_W / 2, CANVAS_H / 2 + 35);

  ctx.font = '18px sans-serif';
  ctx.fillStyle = 'rgba(255,255,255,0.8)';
  ctx.fillText(`Score: ${state.scores[0]} - ${state.scores[1]}`, CANVAS_W / 2, CANVAS_H / 2 + 65);

  ctx.textAlign = 'left';
  ctx.textBaseline = 'alphabetic';
}

function drawBackground(ctx: CanvasRenderingContext2D): void {
  const grad = ctx.createLinearGradient(0, 0, 0, CANVAS_H);
  grad.addColorStop(0, '#0a1628');
  grad.addColorStop(1, '#0d2040');
  ctx.fillStyle = grad;
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);
}

export function renderFrame(ctx: CanvasRenderingContext2D, state: GameState): void {
  ctx.clearRect(0, 0, CANVAS_W, CANVAS_H);
  drawBackground(ctx);
  drawRink(ctx);
  drawGoals(ctx);

  // Players (sorted so goalies render under fielders)
  const sorted = [...state.players].sort((a, b) => {
    if (a.role === 'goalie' && b.role !== 'goalie') return -1;
    if (b.role === 'goalie' && a.role !== 'goalie') return 1;
    return 0;
  });
  for (const p of sorted) drawPlayer(ctx, p);

  drawPuck(ctx, state.puck);
  drawHUD(ctx, state);
  drawControls(ctx, state);

  if (state.mode === 'goal_scored') drawGoalFlash(ctx, state);
}

export function renderPeriodEnd(ctx: CanvasRenderingContext2D, state: GameState): void {
  ctx.fillStyle = 'rgba(0,0,0,0.7)';
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);
  ctx.fillStyle = '#fff';
  ctx.font = 'bold 48px sans-serif';
  ctx.textAlign = 'center';
  ctx.textBaseline = 'middle';
  ctx.fillText(`END OF PERIOD ${state.period}`, CANVAS_W / 2, CANVAS_H / 2 - 20);
  ctx.font = '24px sans-serif';
  ctx.fillStyle = 'rgba(255,255,255,0.8)';
  ctx.fillText(`${TEAM_NAMES[0]} ${state.scores[0]}  —  ${state.scores[1]} ${TEAM_NAMES[1]}`, CANVAS_W / 2, CANVAS_H / 2 + 30);
  ctx.textAlign = 'left';
  ctx.textBaseline = 'alphabetic';
}
