import type { GameState, Player, Puck, Vec2, TeamIndex, GoalPost } from './types';
import {
  RINK_X, RINK_Y, RINK_W, RINK_H, CORNER_R,
  PLAYER_R, PLAYER_SPEED, PLAYER_DECEL,
  PUCK_R, PUCK_FRICTION, PUCK_WALL_BOUNCE,
  SHOOT_POWER, PASS_POWER, SHOOT_COOLDOWN, CONTROL_RADIUS, STEAL_RADIUS,
  PERIOD_DURATION, TOTAL_PERIODS, GOAL_CELEBRATION_TIME, PERIOD_END_TIME,
  GOAL_DEPTH, GOAL_H,
} from './constants';

function vec2(x: number, y: number): Vec2 { return { x, y }; }
function dist(a: Vec2, b: Vec2): number { return Math.sqrt((a.x - b.x) ** 2 + (a.y - b.y) ** 2); }
function normalize(v: Vec2): Vec2 {
  const m = Math.sqrt(v.x * v.x + v.y * v.y);
  return m < 0.001 ? vec2(0, 0) : vec2(v.x / m, v.y / m);
}
function add(a: Vec2, b: Vec2): Vec2 { return vec2(a.x + b.x, a.y + b.y); }
function scale(v: Vec2, s: number): Vec2 { return vec2(v.x * s, v.y * s); }

export const GOALS: GoalPost[] = [
  { x: RINK_X - GOAL_DEPTH, y: RINK_Y + RINK_H / 2 - GOAL_H / 2, width: GOAL_DEPTH, height: GOAL_H, defendingTeam: 0 },
  { x: RINK_X + RINK_W,     y: RINK_Y + RINK_H / 2 - GOAL_H / 2, width: GOAL_DEPTH, height: GOAL_H, defendingTeam: 1 },
];

function isInsideRink(x: number, y: number, r = 0): boolean {
  const cx = RINK_X + RINK_W / 2;
  const cy = RINK_Y + RINK_H / 2;
  const hw = RINK_W / 2 - r;
  const hh = RINK_H / 2 - r;
  const dx = Math.abs(x - cx);
  const dy = Math.abs(y - cy);
  if (dx > hw || dy > hh) return false;
  const cr = CORNER_R - r;
  if (dx > hw - cr && dy > hh - cr) {
    const cdx = dx - (hw - cr);
    const cdy = dy - (hh - cr);
    return cdx * cdx + cdy * cdy <= cr * cr;
  }
  return true;
}

function clampToRink(pos: Vec2, vel: Vec2, r: number): { pos: Vec2; vel: Vec2 } {
  let { x, y } = pos;
  let { x: vx, y: vy } = vel;
  const cx = RINK_X + RINK_W / 2;
  const cy = RINK_Y + RINK_H / 2;
  const hw = RINK_W / 2 - r;
  const hh = RINK_H / 2 - r;
  const cr = CORNER_R - r;

  // Simple boundary check with bounce
  if (x - cx < -hw) { x = cx - hw; vx = Math.abs(vx) * PUCK_WALL_BOUNCE; }
  if (x - cx > hw)  { x = cx + hw; vx = -Math.abs(vx) * PUCK_WALL_BOUNCE; }
  if (y - cy < -hh) { y = cy - hh; vy = Math.abs(vy) * PUCK_WALL_BOUNCE; }
  if (y - cy > hh)  { y = cy + hh; vy = -Math.abs(vy) * PUCK_WALL_BOUNCE; }

  // Corner rounding
  const quadX = x > cx ? 1 : -1;
  const quadY = y > cy ? 1 : -1;
  const cornerX = cx + quadX * (hw - cr);
  const cornerY = cy + quadY * (hh - cr);
  const dx = x - cornerX;
  const dy = y - cornerY;
  if (Math.abs(x - cx) > hw - cr && Math.abs(y - cy) > hh - cr) {
    const d = Math.sqrt(dx * dx + dy * dy);
    if (d > cr) {
      const nx = dx / d;
      const ny = dy / d;
      x = cornerX + nx * cr;
      y = cornerY + ny * cr;
      const dot = vx * nx + vy * ny;
      if (dot > 0) {
        vx -= 2 * dot * nx * PUCK_WALL_BOUNCE;
        vy -= 2 * dot * ny * PUCK_WALL_BOUNCE;
      }
    }
  }
  return { pos: vec2(x, y), vel: vec2(vx, vy) };
}

function isInGoal(puck: Puck): TeamIndex | null {
  for (const g of GOALS) {
    if (
      puck.pos.x >= g.x && puck.pos.x <= g.x + g.width + PUCK_R &&
      puck.pos.y >= g.y && puck.pos.y <= g.y + g.height
    ) {
      return g.defendingTeam === 0 ? 1 : 0;
    }
  }
  return null;
}

function makePlayer(id: number, team: TeamIndex, role: 'goalie' | 'defender' | 'forward', x: number, y: number, num: number): Player {
  return {
    id, team, role, pos: vec2(x, y), vel: vec2(0, 0),
    facingAngle: team === 0 ? 0 : Math.PI,
    hasPuck: false, isSelected: false, number: num,
    shootCooldown: 0, staminaCooldown: 0,
  };
}

function initialPlayers(): Player[] {
  const cx = RINK_X + RINK_W / 2;
  const cy = RINK_Y + RINK_H / 2;
  return [
    // Team 0 (left/blue)
    makePlayer(0, 0, 'goalie',   RINK_X + 45,            cy,         30),
    makePlayer(1, 0, 'defender', cx - 200,               cy - 90,    4),
    makePlayer(2, 0, 'forward',  cx - 120,               cy + 60,    11),
    // Team 1 (right/red)
    makePlayer(3, 1, 'goalie',   RINK_X + RINK_W - 45,  cy,         30),
    makePlayer(4, 1, 'defender', cx + 200,               cy - 90,    5),
    makePlayer(5, 1, 'forward',  cx + 120,               cy + 60,    17),
  ];
}

export function createGameState(playerMode: 'two_player' | 'vs_computer'): GameState {
  const players = initialPlayers();
  players[0].isSelected = true;
  players[3].isSelected = true;
  return {
    players,
    puck: { pos: vec2(RINK_X + RINK_W / 2, RINK_Y + RINK_H / 2), vel: vec2(0, 0), controlledBy: null },
    scores: [0, 0],
    timeRemaining: PERIOD_DURATION,
    period: 1,
    totalPeriods: TOTAL_PERIODS,
    mode: 'playing',
    playerMode,
    selectedIdx: [0, 0],
    goalCooldown: 0,
    lastScoringTeam: null,
    periodEndCooldown: 0,
  };
}

function resetPositions(state: GameState): void {
  const fresh = initialPlayers();
  for (let i = 0; i < state.players.length; i++) {
    state.players[i].pos = { ...fresh[i].pos };
    state.players[i].vel = vec2(0, 0);
    state.players[i].hasPuck = false;
    state.players[i].facingAngle = fresh[i].facingAngle;
  }
  state.puck.pos = vec2(RINK_X + RINK_W / 2, RINK_Y + RINK_H / 2);
  state.puck.vel = vec2(0, 0);
  state.puck.controlledBy = null;
}

function teamPlayers(state: GameState, team: TeamIndex): Player[] {
  return state.players.filter(p => p.team === team);
}

function getSelected(state: GameState, team: TeamIndex): Player {
  const tp = teamPlayers(state, team);
  return tp[state.selectedIdx[team]];
}

export function switchPlayer(state: GameState, team: TeamIndex): void {
  const tp = teamPlayers(state, team);
  const current = tp[state.selectedIdx[team]];
  current.isSelected = false;
  state.selectedIdx[team] = (state.selectedIdx[team] + 1) % tp.length;
  const next = tp[state.selectedIdx[team]];
  next.isSelected = true;
}

// ─── Puck control logic ──────────────────────────────────────────────────────

function findPuckCarrier(state: GameState): Player | null {
  return state.players.find(p => p.hasPuck) ?? null;
}

function updatePuckControl(state: GameState): void {
  const carrier = findPuckCarrier(state);
  if (carrier) {
    // Move puck with carrier
    const offset = vec2(Math.cos(carrier.facingAngle) * (PLAYER_R + PUCK_R - 2), Math.sin(carrier.facingAngle) * (PLAYER_R + PUCK_R - 2));
    state.puck.pos = add(carrier.pos, offset);
    state.puck.vel = vec2(0, 0);
    state.puck.controlledBy = carrier.id;
    return;
  }
  state.puck.controlledBy = null;

  // Check if any player can pick up the puck
  for (const p of state.players) {
    if (dist(p.pos, state.puck.pos) < CONTROL_RADIUS) {
      // Check no opponent is also within steal range and closer
      const opponents = state.players.filter(o => o.team !== p.team);
      const closerOpponent = opponents.some(o => dist(o.pos, state.puck.pos) < STEAL_RADIUS && dist(o.pos, state.puck.pos) < dist(p.pos, state.puck.pos));
      if (!closerOpponent) {
        p.hasPuck = true;
        state.puck.controlledBy = p.id;
        break;
      }
    }
  }
}

// ─── Shoot / Pass ────────────────────────────────────────────────────────────

export function shootPuck(state: GameState, team: TeamIndex, isPowerShot = true): void {
  const carrier = findPuckCarrier(state);
  if (!carrier || carrier.team !== team) return;
  if (carrier.shootCooldown > 0) return;

  carrier.hasPuck = false;
  carrier.shootCooldown = SHOOT_COOLDOWN;
  const power = isPowerShot ? SHOOT_POWER : PASS_POWER;
  state.puck.vel = scale(vec2(Math.cos(carrier.facingAngle), Math.sin(carrier.facingAngle)), power);
  state.puck.controlledBy = null;
}

// ─── Body check ──────────────────────────────────────────────────────────────

function resolvePlayerCollisions(state: GameState): void {
  const { players, puck } = state;
  for (let i = 0; i < players.length; i++) {
    for (let j = i + 1; j < players.length; j++) {
      const a = players[i];
      const b = players[j];
      const d = dist(a.pos, b.pos);
      const minD = PLAYER_R * 2;
      if (d < minD && d > 0.001) {
        const nx = (b.pos.x - a.pos.x) / d;
        const ny = (b.pos.y - a.pos.y) / d;
        const overlap = (minD - d) / 2;
        a.pos.x -= nx * overlap;
        a.pos.y -= ny * overlap;
        b.pos.x += nx * overlap;
        b.pos.y += ny * overlap;

        // Transfer velocity
        const rav = (a.vel.x - b.vel.x) * nx + (a.vel.y - b.vel.y) * ny;
        if (rav > 0) {
          a.vel.x -= rav * nx * 0.7;
          a.vel.y -= rav * ny * 0.7;
          b.vel.x += rav * nx * 0.7;
          b.vel.y += rav * ny * 0.7;
        }

        // Puck steal on collision between different teams
        if (a.team !== b.team) {
          if (a.hasPuck && dist(b.pos, puck.pos) < STEAL_RADIUS + 4) {
            a.hasPuck = false;
          } else if (b.hasPuck && dist(a.pos, puck.pos) < STEAL_RADIUS + 4) {
            b.hasPuck = false;
          }
        }
      }
    }
  }
}

// ─── Human player movement ───────────────────────────────────────────────────

function movePlayer(player: Player, dx: number, dy: number): void {
  if (dx === 0 && dy === 0) return;
  const len = Math.sqrt(dx * dx + dy * dy);
  player.vel.x += (dx / len) * PLAYER_SPEED * 0.4;
  player.vel.y += (dy / len) * PLAYER_SPEED * 0.4;
  const speed = Math.sqrt(player.vel.x ** 2 + player.vel.y ** 2);
  if (speed > PLAYER_SPEED) {
    player.vel.x = (player.vel.x / speed) * PLAYER_SPEED;
    player.vel.y = (player.vel.y / speed) * PLAYER_SPEED;
  }
  player.facingAngle = Math.atan2(dy, dx);
}

export function applyHumanInput(state: GameState, keys: Set<string>): void {
  if (state.mode !== 'playing') return;

  // Team 0: WASD
  const sel0 = getSelected(state, 0);
  let dx0 = 0, dy0 = 0;
  if (keys.has('KeyW') || keys.has('w')) dy0 -= 1;
  if (keys.has('KeyS') || keys.has('s')) dy0 += 1;
  if (keys.has('KeyA') || keys.has('a')) dx0 -= 1;
  if (keys.has('KeyD') || keys.has('d')) dx0 += 1;
  movePlayer(sel0, dx0, dy0);

  if (keys.has('KeyF') || keys.has('f')) {
    shootPuck(state, 0, true);
  }
  if (keys.has('KeyG') || keys.has('g')) {
    shootPuck(state, 0, false);
  }

  // Team 1: Arrow keys (two_player only)
  if (state.playerMode === 'two_player') {
    const sel1 = getSelected(state, 1);
    let dx1 = 0, dy1 = 0;
    if (keys.has('ArrowUp')) dy1 -= 1;
    if (keys.has('ArrowDown')) dy1 += 1;
    if (keys.has('ArrowLeft')) dx1 -= 1;
    if (keys.has('ArrowRight')) dx1 += 1;
    movePlayer(sel1, dx1, dy1);

    if (keys.has('NumpadEnter') || keys.has('Enter') || keys.has('Slash') || keys.has('l') || keys.has('KeyL')) {
      shootPuck(state, 1, true);
    }
    if (keys.has('Period') || keys.has('.')) {
      shootPuck(state, 1, false);
    }
  }
}

// ─── AI ──────────────────────────────────────────────────────────────────────

function aiMoveToward(player: Player, target: Vec2, speed = 1.0): void {
  const dx = target.x - player.pos.x;
  const dy = target.y - player.pos.y;
  const d = Math.sqrt(dx * dx + dy * dy);
  if (d < 2) return;
  movePlayer(player, dx, dy);
  const s = Math.sqrt(player.vel.x ** 2 + player.vel.y ** 2);
  if (s > 0) {
    player.vel.x = (player.vel.x / s) * Math.min(s, PLAYER_SPEED * speed);
    player.vel.y = (player.vel.y / s) * Math.min(s, PLAYER_SPEED * speed);
  }
}

function aiGoalieUpdate(goalie: Player, puck: Puck, goal: GoalPost): void {
  const goalCX = goal.x + goal.width / 2;
  const goalCY = goal.y + goal.height / 2;
  const side = goalie.team === 0 ? 1 : -1;

  // Goalie stays in front of goal, moves vertically to track puck
  const targetX = goalCX + side * 30;
  const targetY = Math.max(goal.y + 15, Math.min(goal.y + goal.height - 15, puck.pos.y));

  aiMoveToward(goalie, vec2(targetX, targetY), 0.85);
}

function updateAI(state: GameState): void {
  const aiTeam: TeamIndex = 1;
  const playerTeam: TeamIndex = 0;
  const aiGoal = GOALS.find(g => g.defendingTeam === aiTeam)!;
  const targetGoal = GOALS.find(g => g.defendingTeam === playerTeam)!;
  const puck = state.puck;

  const aiPlayers = teamPlayers(state, aiTeam);
  const goalie = aiPlayers.find(p => p.role === 'goalie')!;
  const fielders = aiPlayers.filter(p => p.role !== 'goalie');

  // Goalie behavior
  aiGoalieUpdate(goalie, puck, aiGoal);

  const carrier = findPuckCarrier(state);

  fielders.forEach((player, i) => {
    if (carrier && carrier.id === player.id) {
      // Has puck — move toward opponent goal and shoot
      const goalCX = targetGoal.x + targetGoal.width / 2;
      const goalCY = targetGoal.y + targetGoal.height / 2;
      aiMoveToward(player, vec2(goalCX, goalCY), 1.0);

      const dToGoal = dist(player.pos, vec2(goalCX, goalCY));
      if (dToGoal < 180 && player.shootCooldown === 0) {
        // Aim toward goal with slight jitter
        const jitter = (Math.random() - 0.5) * 0.5;
        player.facingAngle = Math.atan2(goalCY - player.pos.y + jitter * 30, goalCX - player.pos.x);
        shootPuck(state, aiTeam, true);
      }
    } else if (!carrier || carrier.team === playerTeam) {
      // Chase puck
      const separationOffset = i === 0 ? vec2(-20, -30) : vec2(-20, 30);
      const target = vec2(puck.pos.x + separationOffset.x, puck.pos.y + separationOffset.y);
      aiMoveToward(player, target, 1.0);
    } else {
      // Teammate has puck, position for pass
      const offX = i === 0 ? 60 : -60;
      const offY = i === 0 ? -60 : 60;
      const targetGoalCX = targetGoal.x + targetGoal.width / 2;
      aiMoveToward(player, vec2(targetGoalCX + offX, RINK_Y + RINK_H / 2 + offY), 0.7);
    }
  });

  // Auto-switch selected to player with puck
  const aiCarrier = aiPlayers.find(p => p.hasPuck);
  if (aiCarrier) {
    const idx = aiPlayers.indexOf(aiCarrier);
    if (idx !== -1) {
      aiPlayers.forEach(p => p.isSelected = false);
      aiCarrier.isSelected = true;
      state.selectedIdx[1] = idx;
    }
  }
}

// ─── Physics update ──────────────────────────────────────────────────────────

function updatePhysics(state: GameState, dt: number): void {
  for (const p of state.players) {
    p.vel.x *= PLAYER_DECEL;
    p.vel.y *= PLAYER_DECEL;
    p.pos.x += p.vel.x;
    p.pos.y += p.vel.y;

    if (!isInsideRink(p.pos.x, p.pos.y, PLAYER_R)) {
      const clamped = clampToRink(p.pos, p.vel, PLAYER_R);
      p.pos = clamped.pos;
      p.vel = scale(clamped.vel, 0.3);
    }

    if (p.shootCooldown > 0) p.shootCooldown--;
  }

  // Puck physics (only when not controlled)
  const carrier = findPuckCarrier(state);
  if (!carrier) {
    state.puck.vel.x *= PUCK_FRICTION;
    state.puck.vel.y *= PUCK_FRICTION;
    state.puck.pos.x += state.puck.vel.x;
    state.puck.pos.y += state.puck.vel.y;

    // Check goal entry first
    const scoringTeam = isInGoal(state.puck);
    if (scoringTeam !== null) {
      state.scores[scoringTeam]++;
      state.lastScoringTeam = scoringTeam;
      state.mode = 'goal_scored';
      state.goalCooldown = GOAL_CELEBRATION_TIME * 60;
      return;
    }

    // Allow puck to slide into goal area openings
    const inLeftGoal = state.puck.pos.x < RINK_X && state.puck.pos.y >= GOALS[0].y && state.puck.pos.y <= GOALS[0].y + GOALS[0].height;
    const inRightGoal = state.puck.pos.x > RINK_X + RINK_W && state.puck.pos.y >= GOALS[1].y && state.puck.pos.y <= GOALS[1].y + GOALS[1].height;

    if (!inLeftGoal && !inRightGoal) {
      const clamped = clampToRink(state.puck.pos, state.puck.vel, PUCK_R);
      state.puck.pos = clamped.pos;
      state.puck.vel = clamped.vel;
    } else {
      // Inside goal — clamp to goal walls
      const goal = inLeftGoal ? GOALS[0] : GOALS[1];
      if (state.puck.pos.y < goal.y + PUCK_R) { state.puck.pos.y = goal.y + PUCK_R; state.puck.vel.y *= -0.5; }
      if (state.puck.pos.y > goal.y + goal.height - PUCK_R) { state.puck.pos.y = goal.y + goal.height - PUCK_R; state.puck.vel.y *= -0.5; }
      if (inLeftGoal && state.puck.pos.x < goal.x + PUCK_R) { state.puck.pos.x = goal.x + PUCK_R; state.puck.vel.x *= -0.5; }
      if (inRightGoal && state.puck.pos.x > goal.x + goal.width - PUCK_R) { state.puck.pos.x = goal.x + goal.width - PUCK_R; state.puck.vel.x *= -0.5; }
    }
  }
}

// ─── Main tick ───────────────────────────────────────────────────────────────

export function gameTick(state: GameState, keys: Set<string>, dt: number): void {
  if (state.mode === 'goal_scored') {
    state.goalCooldown -= 1;
    if (state.goalCooldown <= 0) {
      resetPositions(state);
      if (state.timeRemaining <= 0 && state.period >= state.totalPeriods) {
        state.mode = 'game_over';
      } else {
        state.mode = 'playing';
      }
    }
    return;
  }

  if (state.mode === 'period_end') {
    state.periodEndCooldown -= 1;
    if (state.periodEndCooldown <= 0) {
      if (state.period >= state.totalPeriods) {
        state.mode = 'game_over';
      } else {
        state.period++;
        state.timeRemaining = PERIOD_DURATION;
        resetPositions(state);
        state.mode = 'playing';
      }
    }
    return;
  }

  if (state.mode !== 'playing') return;

  // Timer
  state.timeRemaining -= dt;
  if (state.timeRemaining <= 0) {
    state.timeRemaining = 0;
    if (state.period < state.totalPeriods) {
      state.mode = 'period_end';
      state.periodEndCooldown = PERIOD_END_TIME * 60;
    } else {
      state.mode = 'game_over';
    }
    return;
  }

  applyHumanInput(state, keys);
  if (state.playerMode === 'vs_computer') updateAI(state);

  resolvePlayerCollisions(state);
  updatePhysics(state, dt);
  updatePuckControl(state);

  // Auto-switch team 0 selected to puck carrier
  const carrier0 = teamPlayers(state, 0).find(p => p.hasPuck);
  if (carrier0) {
    const tp = teamPlayers(state, 0);
    tp.forEach(p => p.isSelected = false);
    carrier0.isSelected = true;
    state.selectedIdx[0] = tp.indexOf(carrier0);
  }
  if (state.playerMode === 'two_player') {
    const carrier1 = teamPlayers(state, 1).find(p => p.hasPuck);
    if (carrier1) {
      const tp = teamPlayers(state, 1);
      tp.forEach(p => p.isSelected = false);
      carrier1.isSelected = true;
      state.selectedIdx[1] = tp.indexOf(carrier1);
    }
  }
}

export { teamPlayers, getSelected };
