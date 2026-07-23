import { useRef, useEffect, useState, useCallback } from "react";

// ============================================================
//  TYPES
// ============================================================
type GameMode = "twoPlayer" | "vsComputer";

interface Vec2 {
  x: number;
  y: number;
}

interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  rotation: number;
  trail: Vec2[];
}

interface Player {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  team: number;
  color: string;
  label: string;
  legSwing: number;
}

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  color: string;
  life: number;
  maxLife: number;
  size: number;
}

interface Celebration {
  text: string;
  color: string;
  x: number;
  y: number;
  life: number;
  maxLife: number;
  scale: number;
}

// ============================================================
//  CONSTANTS
// ============================================================
const CANVAS_W = 1000;
const CANVAS_H = 680;
const FIELD_MARGIN = 40;
const FIELD_TOP = 80;
const PLAYER_SPEED = 3.2;
const PLAYER_SPEED_AI = 3.0;
const BALL_FRICTION = 0.985;
const KICK_POWER = 9.0;
const MAX_BALL_SPEED = 14.0;
const MATCH_DURATION = 120;
const GOAL_HEIGHT = 140;

// ============================================================
//  MAIN COMPONENT
// ============================================================
export default function App() {
  const [screen, setScreen] = useState<"menu" | "game">("menu");
  const [mode, setMode] = useState<GameMode>("twoPlayer");
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef<GameState | null>(null);
  const keysRef = useRef<Set<string>>(new Set());
  const animRef = useRef<number>(0);

  // ---- game state ----
  function createInitialState(gameMode: GameMode): GameState {
    const fieldLeft = FIELD_MARGIN;
    const fieldRight = CANVAS_W - FIELD_MARGIN;
    const fieldBottom = CANVAS_H - FIELD_MARGIN;

    const ball: Ball = {
      x: CANVAS_W / 2,
      y: CANVAS_H / 2,
      vx: 0,
      vy: 0,
      radius: 10,
      rotation: 0,
      trail: [],
    };

    const rightColor = gameMode === "vsComputer" ? "#e74c3c" : "#3498db";
    const rightLabel = gameMode === "vsComputer" ? "CPU" : "P2";

    const players: Player[] = [
      mkPlayer(fieldLeft + 80, CANVAS_H / 2, 0, "#229954", "P1"),
      mkPlayer(fieldLeft + 150, CANVAS_H / 2 - 100, 0, "#229954", "P1"),
      mkPlayer(fieldLeft + 150, CANVAS_H / 2 + 100, 0, "#229954", "P1"),
      mkPlayer(fieldRight - 80, CANVAS_H / 2, 1, rightColor, rightLabel),
      mkPlayer(fieldRight - 150, CANVAS_H / 2 - 100, 1, rightColor, rightLabel),
      mkPlayer(fieldRight - 150, CANVAS_H / 2 + 100, 1, rightColor, rightLabel),
    ];

    return {
      ball,
      players,
      particles: [],
      celebrations: [],
      scoreLeft: 0,
      scoreRight: 0,
      matchTime: MATCH_DURATION,
      timeAcc: 0,
      running: true,
      goalScored: false,
      goalResetTimer: 0,
      shakeIntensity: 0,
      shakeX: 0,
      shakeY: 0,
      mode: gameMode,
    };
  }

  function mkPlayer(x: number, y: number, team: number, color: string, label: string): Player {
    return { x, y, vx: 0, vy: 0, radius: 14, team, color, label, legSwing: 0 };
  }

  // ---- start / stop ----
  const startGame = (m: GameMode) => {
    setMode(m);
    setScreen("game");
    stateRef.current = createInitialState(m);
  };

  const backToMenu = () => {
    setScreen("menu");
    stateRef.current = null;
  };

  // ---- keyboard ----
  useEffect(() => {
    const down = (e: KeyboardEvent) => {
      keysRef.current.add(e.key.toLowerCase());
      if (["arrowup", "arrowdown", "arrowleft", "arrowright", " "].includes(e.key.toLowerCase())) {
        e.preventDefault();
      }
    };
    const up = (e: KeyboardEvent) => {
      keysRef.current.delete(e.key.toLowerCase());
    };
    window.addEventListener("keydown", down);
    window.addEventListener("keyup", up);
    return () => {
      window.removeEventListener("keydown", down);
      window.removeEventListener("keyup", up);
    };
  }, []);

  // ---- game loop ----
  const update = useCallback((dt: number) => {
    const s = stateRef.current;
    if (!s) return;

    const fieldLeft = FIELD_MARGIN;
    const fieldRight = CANVAS_W - FIELD_MARGIN;
    const fieldTopY = FIELD_TOP;
    const fieldBottom = CANVAS_H - FIELD_MARGIN;

    if (s.running && !s.goalScored) {
      // timer
      s.timeAcc += dt;
      if (s.timeAcc >= 1) {
        s.timeAcc -= 1;
        s.matchTime--;
        if (s.matchTime <= 0) {
          s.matchTime = 0;
          s.running = false;
        }
      }

      handleInput(s);
      if (s.mode === "vsComputer") updateAI(s);

      // update players
      for (const p of s.players) {
        p.x += p.vx;
        p.y += p.vy;
        p.x = Math.max(fieldLeft + p.radius, Math.min(fieldRight - p.radius, p.x));
        p.y = Math.max(fieldTopY + p.radius, Math.min(fieldBottom - p.radius, p.y));
        const spd = Math.hypot(p.vx, p.vy);
        if (spd > 0.5) p.legSwing += spd * 0.15;
        else p.legSwing *= 0.8;
      }

      // player-ball collision
      for (const p of s.players) {
        const dx = s.ball.x - p.x;
        const dy = s.ball.y - p.y;
        const dist = Math.hypot(dx, dy);
        const minDist = p.radius + s.ball.radius;
        if (dist < minDist && dist > 0) {
          const nx = dx / dist;
          const ny = dy / dist;
          const overlap = minDist - dist;
          s.ball.x += nx * overlap;
          s.ball.y += ny * overlap;
          const pSpeed = Math.hypot(p.vx, p.vy);
          const kick = KICK_POWER * (0.5 + (pSpeed / PLAYER_SPEED) * 0.5);
          s.ball.vx = nx * kick + p.vx * 0.6;
          s.ball.vy = ny * kick + p.vy * 0.6;
          const bs = Math.hypot(s.ball.vx, s.ball.vy);
          if (bs > MAX_BALL_SPEED) {
            s.ball.vx = (s.ball.vx / bs) * MAX_BALL_SPEED;
            s.ball.vy = (s.ball.vy / bs) * MAX_BALL_SPEED;
          }
          for (let i = 0; i < 6; i++) {
            s.particles.push({
              x: s.ball.x,
              y: s.ball.y,
              vx: -nx * 3 + (Math.random() - 0.5) * 4,
              vy: -ny * 3 + (Math.random() - 0.5) * 4,
              color: p.team === 0 ? "100,255,150" : "100,180,255",
              life: 0.5,
              maxLife: 0.5,
              size: 2 + Math.random() * 3,
            });
          }
          p.vx -= nx * 0.5;
          p.vy -= ny * 0.5;
        }
      }

      // player-player collision
      for (let i = 0; i < s.players.length; i++) {
        for (let j = i + 1; j < s.players.length; j++) {
          const a = s.players[i];
          const b = s.players[j];
          const dx = b.x - a.x;
          const dy = b.y - a.y;
          const d = Math.hypot(dx, dy);
          const md = a.radius + b.radius;
          if (d < md && d > 0) {
            const nx = dx / d;
            const ny = dy / d;
            const ov = (md - d) / 2;
            a.x -= nx * ov;
            a.y -= ny * ov;
            b.x += nx * ov;
            b.y += ny * ov;
          }
        }
      }

      // ball update
      s.ball.x += s.ball.vx;
      s.ball.y += s.ball.vy;
      s.ball.vx *= BALL_FRICTION;
      s.ball.vy *= BALL_FRICTION;
      if (Math.abs(s.ball.vx) < 0.05) s.ball.vx = 0;
      if (Math.abs(s.ball.vy) < 0.05) s.ball.vy = 0;
      const bspd = Math.hypot(s.ball.vx, s.ball.vy);
      s.ball.rotation += bspd * 0.08;

      // trail
      s.ball.trail.push({ x: s.ball.x, y: s.ball.y });
      if (s.ball.trail.length > 15) s.ball.trail.shift();

      // walls / goals
      const goalTop = CANVAS_H / 2 - GOAL_HEIGHT / 2;
      const goalBot = goalTop + GOAL_HEIGHT;

      if (s.ball.x - s.ball.radius < fieldLeft) {
        if (s.ball.y > goalTop && s.ball.y < goalBot) {
          scoreGoal(s, "right");
        } else {
          s.ball.x = fieldLeft + s.ball.radius;
          s.ball.vx = -s.ball.vx * 0.7;
          spawnWallParticles(s, s.ball.x, s.ball.y, 1, 0);
        }
      }
      if (s.ball.x + s.ball.radius > fieldRight) {
        if (s.ball.y > goalTop && s.ball.y < goalBot) {
          scoreGoal(s, "left");
        } else {
          s.ball.x = fieldRight - s.ball.radius;
          s.ball.vx = -s.ball.vx * 0.7;
          spawnWallParticles(s, s.ball.x, s.ball.y, -1, 0);
        }
      }
      if (s.ball.y - s.ball.radius < fieldTopY) {
        s.ball.y = fieldTopY + s.ball.radius;
        s.ball.vy = -s.ball.vy * 0.7;
        spawnWallParticles(s, s.ball.x, s.ball.y, 0, 1);
      }
      if (s.ball.y + s.ball.radius > fieldBottom) {
        s.ball.y = fieldBottom - s.ball.radius;
        s.ball.vy = -s.ball.vy * 0.7;
        spawnWallParticles(s, s.ball.x, s.ball.y, 0, -1);
      }
    } else if (s.goalScored) {
      s.goalResetTimer -= dt;
      if (s.goalResetTimer <= 0) {
        resetPositions(s);
        s.goalScored = false;
      }
    }

    // particles
    for (let i = s.particles.length - 1; i >= 0; i--) {
      const p = s.particles[i];
      p.x += p.vx;
      p.y += p.vy;
      p.vx *= 0.95;
      p.vy *= 0.95;
      p.vy += 0.1;
      p.life -= dt;
      if (p.life <= 0) s.particles.splice(i, 1);
    }

    // celebrations
    for (let i = s.celebrations.length - 1; i >= 0; i--) {
      const c = s.celebrations[i];
      c.life -= dt;
      const prog = 1 - c.life / c.maxLife;
      if (prog < 0.3) c.scale = (prog / 0.3) * 1.3;
      else if (prog < 0.4) c.scale = 1.3 - ((prog - 0.3) / 0.1) * 0.3;
      else c.scale = 1.0;
      if (c.life <= 0) s.celebrations.splice(i, 1);
    }

    // shake
    s.shakeIntensity *= 0.9;
    s.shakeX = (Math.random() - 0.5) * s.shakeIntensity;
    s.shakeY = (Math.random() - 0.5) * s.shakeIntensity;
  }, []);

  function handleInput(s: GameState) {
    const keys = keysRef.current;
    const p1 = s.players[0];
    let dx = 0, dy = 0;
    if (keys.has("w")) dy -= 1;
    if (keys.has("s")) dy += 1;
    if (keys.has("a")) dx -= 1;
    if (keys.has("d")) dx += 1;
    setVel(p1, dx, dy, PLAYER_SPEED);

    if (s.mode === "twoPlayer") {
      const p2 = s.players[3];
      dx = 0; dy = 0;
      if (keys.has("arrowup")) dy -= 1;
      if (keys.has("arrowdown")) dy += 1;
      if (keys.has("arrowleft")) dx -= 1;
      if (keys.has("arrowright")) dx += 1;
      setVel(p2, dx, dy, PLAYER_SPEED);
    }
  }

  function setVel(p: Player, dx: number, dy: number, speed: number) {
    if (dx === 0 && dy === 0) {
      p.vx *= 0.7;
      p.vy *= 0.7;
    } else {
      const len = Math.hypot(dx, dy);
      p.vx = (dx / len) * speed;
      p.vy = (dy / len) * speed;
    }
  }

  function updateAI(s: GameState) {
    const fieldRight = CANVAS_W - FIELD_MARGIN;
    const fieldLeft = FIELD_MARGIN;
    const ai0 = s.players[3];
    const ai1 = s.players[4];
    const ai2 = s.players[5];

    let closest = ai0;
    let minD = Math.hypot(ai0.x - s.ball.x, ai0.y - s.ball.y);
    const d1 = Math.hypot(ai1.x - s.ball.x, ai1.y - s.ball.y);
    const d2 = Math.hypot(ai2.x - s.ball.x, ai2.y - s.ball.y);
    if (d1 < minD) { closest = ai1; minD = d1; }
    if (d2 < minD) { closest = ai2; minD = d2; }

    chaseBall(s, closest, fieldLeft);
    if (ai0 !== closest) holdPos(ai0, fieldRight - 120, CANVAS_H / 2);
    if (ai1 !== closest) holdPos(ai1, fieldRight - 200, CANVAS_H / 2 - 100);
    if (ai2 !== closest) holdPos(ai2, fieldRight - 200, CANVAS_H / 2 + 100);
  }

  function chaseBall(s: GameState, p: Player, fieldLeft: number) {
    const dx = s.ball.x - p.x;
    const dy = s.ball.y - p.y;
    const d = Math.hypot(dx, dy);
    if (d < 5) {
      const tx = fieldLeft;
      const ty = CANVAS_H / 2 + (Math.random() - 0.5) * 100;
      const kdx = tx - p.x;
      const kdy = ty - p.y;
      const kl = Math.hypot(kdx, kdy);
      p.vx = (kdx / kl) * PLAYER_SPEED_AI;
      p.vy = (kdy / kl) * PLAYER_SPEED_AI;
    } else {
      const px = s.ball.x + s.ball.vx * 5;
      const py = s.ball.y + s.ball.vy * 5;
      const tx = px - p.x;
      const ty = py - p.y;
      const tl = Math.hypot(tx, ty);
      if (tl > 0) {
        p.vx = (tx / tl) * PLAYER_SPEED_AI;
        p.vy = (ty / tl) * PLAYER_SPEED_AI;
      }
    }
  }

  function holdPos(p: Player, tx: number, ty: number) {
    const dx = tx - p.x;
    const dy = ty - p.y;
    const d = Math.hypot(dx, dy);
    if (d > 15) {
      p.vx = (dx / d) * PLAYER_SPEED_AI * 0.7;
      p.vy = (dy / d) * PLAYER_SPEED_AI * 0.7;
    } else {
      p.vx *= 0.5;
      p.vy *= 0.5;
    }
  }

  function spawnWallParticles(s: GameState, x: number, y: number, dx: number, dy: number) {
    for (let i = 0; i < 5; i++) {
      s.particles.push({
        x, y,
        vx: dx * 2 + (Math.random() - 0.5) * 3,
        vy: dy * 2 + (Math.random() - 0.5) * 3,
        color: "255,255,255",
        life: 0.3, maxLife: 0.3, size: 2 + Math.random() * 2,
      });
    }
  }

  function scoreGoal(s: GameState, side: string) {
    if (side === "left") {
      s.scoreLeft++;
    } else {
      s.scoreRight++;
    }
    s.goalScored = true;
    s.goalResetTimer = 2.5;
    s.shakeIntensity = 15;
    const color = side === "left" ? "34,200,84" : (s.mode === "vsComputer" ? "231,76,60" : "52,152,219");
    for (let i = 0; i < 80; i++) {
      const a = Math.random() * Math.PI * 2;
      const sp = 2 + Math.random() * 6;
      s.particles.push({
        x: s.ball.x, y: s.ball.y,
        vx: Math.cos(a) * sp, vy: Math.sin(a) * sp,
        color, life: 1.5 + Math.random(), maxLife: 1.5 + Math.random(),
        size: 2 + Math.random() * 3,
      });
    }
    s.celebrations.push({
      text: "GOAL!",
      color: side === "left" ? "#22c84e" : (s.mode === "vsComputer" ? "#e74c3c" : "#3498db"),
      x: CANVAS_W / 2, y: CANVAS_H / 2,
      life: 2.5, maxLife: 2.5, scale: 0,
    });
  }

  function resetPositions(s: GameState) {
    const fieldLeft = FIELD_MARGIN;
    const fieldRight = CANVAS_W - FIELD_MARGIN;
    s.ball.x = CANVAS_W / 2;
    s.ball.y = CANVAS_H / 2;
    s.ball.vx = 0;
    s.ball.vy = 0;
    s.ball.trail = [];

    const left = [[fieldLeft + 80, CANVAS_H / 2], [fieldLeft + 150, CANVAS_H / 2 - 100], [fieldLeft + 150, CANVAS_H / 2 + 100]];
    const right = [[fieldRight - 80, CANVAS_H / 2], [fieldRight - 150, CANVAS_H / 2 - 100], [fieldRight - 150, CANVAS_H / 2 + 100]];
    for (let i = 0; i < 3; i++) {
      s.players[i].x = left[i][0]; s.players[i].y = left[i][1];
      s.players[i].vx = 0; s.players[i].vy = 0;
      s.players[i + 3].x = right[i][0]; s.players[i + 3].y = right[i][1];
      s.players[i + 3].vx = 0; s.players[i + 3].vy = 0;
    }
  }

  // ---- render ----
  const render = useCallback((ctx: CanvasRenderingContext2D) => {
    const s = stateRef.current;
    if (!s) return;

    ctx.save();
    ctx.translate(s.shakeX, s.shakeY);

    drawStadium(ctx);
    drawField(ctx);
    drawGoals(ctx);

    // ball trail
    for (let i = 0; i < s.ball.trail.length; i++) {
      const t = s.ball.trail[i];
      const alpha = i / s.ball.trail.length;
      const r = s.ball.radius * alpha * 0.6;
      if (r > 0) {
        ctx.fillStyle = `rgba(255,255,255,${alpha * 0.3})`;
        ctx.beginPath();
        ctx.arc(t.x, t.y, r, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // particles
    for (const p of s.particles) {
      const alpha = Math.max(0, p.life / p.maxLife);
      ctx.fillStyle = `rgba(${p.color},${alpha})`;
      const sz = p.size * alpha;
      if (sz > 0) {
        ctx.beginPath();
        ctx.arc(p.x, p.y, sz, 0, Math.PI * 2);
        ctx.fill();
      }
    }

    // players
    for (const p of s.players) drawPlayer(ctx, p);

    // ball
    drawBall(ctx, s.ball);

    // celebrations
    for (const c of s.celebrations) {
      const alpha = Math.max(0, c.life / c.maxLife);
      ctx.save();
      ctx.translate(c.x, c.y);
      ctx.scale(c.scale, c.scale);
      ctx.font = "bold 80px Arial";
      ctx.textAlign = "center";
      ctx.textBaseline = "middle";
      for (let i = 0; i < 8; i++) {
        ctx.fillStyle = `rgba(${hexToRgb(c.color)},${alpha * 0.12})`;
        ctx.fillText(c.text, -i, 0);
        ctx.fillText(c.text, i, 0);
        ctx.fillText(c.text, 0, -i);
        ctx.fillText(c.text, 0, i);
      }
      ctx.fillStyle = `rgba(${hexToRgb(c.color)},${alpha})`;
      ctx.fillText(c.text, 0, 0);
      ctx.restore();
    }

    drawHUD(ctx, s);

    if (!s.running && s.matchTime <= 0) drawGameOver(ctx, s);

    ctx.restore();
  }, []);

  function drawStadium(ctx: CanvasRenderingContext2D) {
    ctx.fillStyle = "#0f0f14";
    ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);

    // top stands
    const g1 = ctx.createLinearGradient(0, 0, 0, FIELD_TOP);
    g1.addColorStop(0, "#28283c");
    g1.addColorStop(1, "#191923");
    ctx.fillStyle = g1;
    ctx.fillRect(0, 0, CANVAS_W, FIELD_TOP);

    // bottom stands
    const g2 = ctx.createLinearGradient(0, CANVAS_H - FIELD_MARGIN, 0, CANVAS_H);
    g2.addColorStop(0, "#191923");
    g2.addColorStop(1, "#28283c");
    ctx.fillStyle = g2;
    ctx.fillRect(0, CANVAS_H - FIELD_MARGIN, CANVAS_W, FIELD_MARGIN);

    // crowd dots
    let seed = 42;
    const rand = () => { seed = (seed * 9301 + 49297) % 233280; return seed / 233280; };
    ctx.fillStyle = "#505064";
    for (let i = 0; i < 200; i++) {
      ctx.fillRect(rand() * CANVAS_W, 10 + rand() * (FIELD_TOP - 20), 2, 2);
    }
    for (let i = 0; i < 200; i++) {
      ctx.fillRect(rand() * CANVAS_W, CANVAS_H - FIELD_MARGIN + 5 + rand() * (FIELD_MARGIN - 10), 2, 2);
    }

    // floodlights
    drawFloodlight(ctx, 50, 30);
    drawFloodlight(ctx, CANVAS_W - 50, 30);
    drawFloodlight(ctx, 50, CANVAS_H - 20);
    drawFloodlight(ctx, CANVAS_W - 50, CANVAS_H - 20);
  }

  function drawFloodlight(ctx: CanvasRenderingContext2D, x: number, y: number) {
    const g = ctx.createRadialGradient(x, y, 0, x, y, 80);
    g.addColorStop(0, "rgba(255,255,220,0.16)");
    g.addColorStop(1, "rgba(255,255,220,0)");
    ctx.fillStyle = g;
    ctx.beginPath();
    ctx.arc(x, y, 80, 0, Math.PI * 2);
    ctx.fill();
  }

  function drawField(ctx: CanvasRenderingContext2D) {
    const fl = FIELD_MARGIN;
    const fr = CANVAS_W - FIELD_MARGIN;
    const ft = FIELD_TOP;
    const fb = CANVAS_H - FIELD_MARGIN;
    const fw = fr - fl;
    const fh = fb - ft;

    // mow stripes
    const stripes = 12;
    const sw = fw / stripes;
    for (let i = 0; i < stripes; i++) {
      ctx.fillStyle = i % 2 === 0 ? "#228b22" : "#1c781c";
      ctx.fillRect(fl + i * sw, ft, sw, fh);
    }

    // markings
    ctx.strokeStyle = "rgba(255,255,255,0.8)";
    ctx.lineWidth = 3;
    ctx.strokeRect(fl, ft, fw, fh);

    const cx = fl + fw / 2;
    ctx.beginPath();
    ctx.moveTo(cx, ft);
    ctx.lineTo(cx, fb);
    ctx.stroke();

    ctx.beginPath();
    ctx.arc(cx, CANVAS_H / 2, 70, 0, Math.PI * 2);
    ctx.stroke();

    ctx.fillStyle = "#fff";
    ctx.beginPath();
    ctx.arc(cx, CANVAS_H / 2, 3, 0, Math.PI * 2);
    ctx.fill();

    // penalty boxes
    const bh = 200;
    const bt = CANVAS_H / 2 - bh / 2;
    ctx.strokeRect(fl, bt, 100, bh);
    ctx.strokeRect(fr - 100, bt, 100, bh);

    // goal areas
    const sh = 120;
    const st = CANVAS_H / 2 - sh / 2;
    ctx.strokeRect(fl, st, 45, sh);
    ctx.strokeRect(fr - 45, st, 45, sh);

    // corner arcs
    ctx.beginPath(); ctx.arc(fl, ft, 20, 1.5 * Math.PI, 2 * Math.PI); ctx.stroke();
    ctx.beginPath(); ctx.arc(fr, ft, 20, Math.PI, 1.5 * Math.PI); ctx.stroke();
    ctx.beginPath(); ctx.arc(fl, fb, 20, 0, 0.5 * Math.PI); ctx.stroke();
    ctx.beginPath(); ctx.arc(fr, fb, 20, 0.5 * Math.PI, Math.PI); ctx.stroke();
  }

  function drawGoals(ctx: CanvasRenderingContext2D) {
    const fl = FIELD_MARGIN;
    const fr = CANVAS_W - FIELD_MARGIN;
    const gt = CANVAS_H / 2 - GOAL_HEIGHT / 2;
    const gb = gt + GOAL_HEIGHT;

    ctx.strokeStyle = "rgba(255,255,255,0.7)";
    ctx.lineWidth = 3;
    // left
    ctx.beginPath();
    ctx.moveTo(fl, gt); ctx.lineTo(fl - 25, gt);
    ctx.moveTo(fl, gb); ctx.lineTo(fl - 25, gb);
    ctx.moveTo(fl - 25, gt); ctx.lineTo(fl - 25, gb);
    ctx.stroke();
    // right
    ctx.beginPath();
    ctx.moveTo(fr, gt); ctx.lineTo(fr + 25, gt);
    ctx.moveTo(fr, gb); ctx.lineTo(fr + 25, gb);
    ctx.moveTo(fr + 25, gt); ctx.lineTo(fr + 25, gb);
    ctx.stroke();

    // nets
    ctx.strokeStyle = "rgba(255,255,255,0.25)";
    ctx.lineWidth = 1;
    for (let i = 0; i <= 5; i++) {
      const y = gt + (i * GOAL_HEIGHT) / 5;
      ctx.beginPath(); ctx.moveTo(fl - 25, y); ctx.lineTo(fl, y); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(fr, y); ctx.lineTo(fr + 25, y); ctx.stroke();
    }
    for (let i = 0; i <= 5; i++) {
      const xl = fl - 25 + i * 5;
      const xr = fr + i * 5;
      ctx.beginPath(); ctx.moveTo(xl, gt); ctx.lineTo(xl, gb); ctx.stroke();
      ctx.beginPath(); ctx.moveTo(xr, gt); ctx.lineTo(xr, gb); ctx.stroke();
    }
  }

  function drawPlayer(ctx: CanvasRenderingContext2D, p: Player) {
    // shadow
    ctx.fillStyle = "rgba(0,0,0,0.3)";
    ctx.beginPath();
    ctx.ellipse(p.x + 2, p.y + p.radius - 1, p.radius, p.radius * 0.4, 0, 0, Math.PI * 2);
    ctx.fill();

    // legs
    const legOff = Math.sin(p.legSwing) * 4;
    ctx.strokeStyle = "#28283c";
    ctx.lineWidth = 4;
    ctx.lineCap = "round";
    ctx.beginPath();
    ctx.moveTo(p.x - 4, p.y + p.radius - 2);
    ctx.lineTo(p.x - 4 + legOff, p.y + p.radius + 6);
    ctx.moveTo(p.x + 4, p.y + p.radius - 2);
    ctx.lineTo(p.x + 4 - legOff, p.y + p.radius + 6);
    ctx.stroke();

    // body
    const grad = ctx.createRadialGradient(p.x - p.radius / 3, p.y - p.radius / 3, 0, p.x, p.y, p.radius * 1.5);
    grad.addColorStop(0, lighten(p.color, 40));
    grad.addColorStop(1, darken(p.color, 30));
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(p.x, p.y, p.radius, 0, Math.PI * 2);
    ctx.fill();

    // outline
    ctx.strokeStyle = darken(p.color, 50);
    ctx.lineWidth = 2;
    ctx.stroke();

    // team dot
    ctx.fillStyle = p.team === 0 ? "#ffff64" : "#ffc864";
    ctx.beginPath();
    ctx.arc(p.x, p.y - p.radius + 2, 3, 0, Math.PI * 2);
    ctx.fill();

    // label
    ctx.fillStyle = "#fff";
    ctx.font = "bold 9px Arial";
    ctx.textAlign = "center";
    ctx.textBaseline = "top";
    ctx.fillText(p.label, p.x, p.y + p.radius + 8);
  }

  function drawBall(ctx: CanvasRenderingContext2D, b: Ball) {
    // shadow
    ctx.fillStyle = "rgba(0,0,0,0.35)";
    ctx.beginPath();
    ctx.ellipse(b.x + 2, b.y + 4, b.radius, b.radius, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.save();
    ctx.translate(b.x, b.y);
    ctx.rotate(b.rotation);

    const grad = ctx.createRadialGradient(-b.radius / 3, -b.radius / 3, 0, 0, 0, b.radius * 1.5);
    grad.addColorStop(0, "#fafafa");
    grad.addColorStop(1, "#aaaab4");
    ctx.fillStyle = grad;
    ctx.beginPath();
    ctx.arc(0, 0, b.radius, 0, Math.PI * 2);
    ctx.fill();

    // pentagons
    ctx.fillStyle = "#1e1e28";
    drawPentagon(ctx, 0, -8, 5);
    drawPentagon(ctx, -9, 5, 4);
    drawPentagon(ctx, 9, 5, 4);
    drawPentagon(ctx, 0, 11, 4);

    ctx.strokeStyle = "#32323c";
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    ctx.arc(0, 0, b.radius, 0, Math.PI * 2);
    ctx.stroke();

    ctx.restore();
  }

  function drawPentagon(ctx: CanvasRenderingContext2D, cx: number, cy: number, r: number) {
    ctx.beginPath();
    for (let i = 0; i < 5; i++) {
      const a = (i / 5) * Math.PI * 2 - Math.PI / 2;
      const px = cx + Math.cos(a) * r;
      const py = cy + Math.sin(a) * r;
      if (i === 0) ctx.moveTo(px, py);
      else ctx.lineTo(px, py);
    }
    ctx.closePath();
    ctx.fill();
  }

  function drawHUD(ctx: CanvasRenderingContext2D, s: GameState) {
    // score bar
    ctx.fillStyle = "rgba(0,0,0,0.7)";
    roundRect(ctx, CANVAS_W / 2 - 130, 10, 260, 50, 15);
    ctx.fill();

    ctx.font = "bold 28px Arial";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillStyle = "#22c84e";
    ctx.fillText(String(s.scoreLeft).padStart(2, "0"), CANVAS_W / 2 - 70, 36);
    ctx.fillStyle = "#fff";
    ctx.fillText(":", CANVAS_W / 2, 34);
    const rc = s.mode === "vsComputer" ? "#e74c3c" : "#3498db";
    ctx.fillStyle = rc;
    ctx.fillText(String(s.scoreRight).padStart(2, "0"), CANVAS_W / 2 + 70, 36);

    // timer
    ctx.font = "bold 18px Arial";
    ctx.fillStyle = "#fff";
    const m = Math.floor(s.matchTime / 60);
    const sec = s.matchTime % 60;
    ctx.fillText(`${m}:${String(sec).padStart(2, "0")}`, CANVAS_W / 2, 72);

    // mode
    ctx.font = "12px Arial";
    ctx.fillStyle = "#c8c8c8";
    ctx.textAlign = "left";
    ctx.fillText(s.mode === "vsComputer" ? "VS COMPUTER" : "2 PLAYERS", 100, 30);
  }

  function drawGameOver(ctx: CanvasRenderingContext2D, s: GameState) {
    ctx.fillStyle = "rgba(0,0,0,0.65)";
    ctx.fillRect(0, 0, CANVAS_W, CANVAS_H);

    ctx.font = "bold 48px Arial";
    ctx.textAlign = "center";
    ctx.textBaseline = "middle";
    ctx.fillStyle = "#fff";
    let result: string;
    if (s.scoreLeft > s.scoreRight) result = "GREEN TEAM WINS!";
    else if (s.scoreRight > s.scoreLeft) result = s.mode === "vsComputer" ? "COMPUTER WINS!" : "BLUE TEAM WINS!";
    else result = "DRAW!";
    ctx.fillText(result, CANVAS_W / 2, CANVAS_H / 2 - 30);

    ctx.font = "20px Arial";
    ctx.fillStyle = "#c8c8c8";
    ctx.fillText(`Final Score: ${s.scoreLeft} - ${s.scoreRight}`, CANVAS_W / 2, CANVAS_H / 2 + 20);

    ctx.font = "16px Arial";
    ctx.fillStyle = "#b4b4b4";
    ctx.fillText("Click MENU to return", CANVAS_W / 2, CANVAS_H / 2 + 60);
  }

  // ---- helpers ----
  function roundRect(ctx: CanvasRenderingContext2D, x: number, y: number, w: number, h: number, r: number) {
    ctx.beginPath();
    ctx.moveTo(x + r, y);
    ctx.arcTo(x + w, y, x + w, y + h, r);
    ctx.arcTo(x + w, y + h, x, y + h, r);
    ctx.arcTo(x, y + h, x, y, r);
    ctx.arcTo(x, y, x + w, y, r);
    ctx.closePath();
  }

  function hexToRgb(hex: string): string {
    const r = parseInt(hex.slice(1, 3), 16);
    const g = parseInt(hex.slice(3, 5), 16);
    const b = parseInt(hex.slice(5, 7), 16);
    return `${r},${g},${b}`;
  }

  function lighten(hex: string, amt: number): string {
    const r = Math.min(255, parseInt(hex.slice(1, 3), 16) + amt);
    const g = Math.min(255, parseInt(hex.slice(3, 5), 16) + amt);
    const b = Math.min(255, parseInt(hex.slice(5, 7), 16) + amt);
    return `rgb(${r},${g},${b})`;
  }

  function darken(hex: string, amt: number): string {
    const r = Math.max(0, parseInt(hex.slice(1, 3), 16) - amt);
    const g = Math.max(0, parseInt(hex.slice(3, 5), 16) - amt);
    const b = Math.max(0, parseInt(hex.slice(5, 7), 16) - amt);
    return `rgb(${r},${g},${b})`;
  }

  // ---- animation loop ----
  useEffect(() => {
    if (screen !== "game") return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    let last = performance.now();
    const loop = (now: number) => {
      const dt = Math.min((now - last) / 1000, 0.033);
      last = now;
      update(dt);
      render(ctx);
      animRef.current = requestAnimationFrame(loop);
    };
    animRef.current = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(animRef.current);
  }, [screen, update, render]);

  // ---- menu ----
  if (screen === "menu") {
    return <Menu onStart={startGame} />;
  }

  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center gap-4 py-4">
      <div className="flex gap-4 items-center">
        <button
          onClick={backToMenu}
          className="px-5 py-2 bg-gray-700 hover:bg-gray-600 text-white font-bold rounded-lg transition-colors"
        >
          MENU
        </button>
        <span className="text-gray-300 text-sm">
          {mode === "vsComputer" ? "VS COMPUTER — " : "2 PLAYERS — "}
          P1: WASD{mode === "twoPlayer" ? "  |  P2: Arrow Keys" : ""}
        </span>
      </div>
      <canvas
        ref={canvasRef}
        width={CANVAS_W}
        height={CANVAS_H}
        className="rounded-xl shadow-2xl border border-gray-700 max-w-full"
        style={{ imageRendering: "auto" }}
      />
    </div>
  );
}

// ============================================================
//  GAME STATE TYPE
// ============================================================
interface GameState {
  ball: Ball;
  players: Player[];
  particles: Particle[];
  celebrations: Celebration[];
  scoreLeft: number;
  scoreRight: number;
  matchTime: number;
  timeAcc: number;
  running: boolean;
  goalScored: boolean;
  goalResetTimer: number;
  shakeIntensity: number;
  shakeX: number;
  shakeY: number;
  mode: GameMode;
}

// ============================================================
//  MENU COMPONENT
// ============================================================
function Menu({ onStart }: { onStart: (m: GameMode) => void }) {
  const [pulse, setPulse] = useState(0);

  useEffect(() => {
    const id = setInterval(() => setPulse((p) => p + 0.05), 30);
    return () => clearInterval(id);
  }, []);

  const glow = 0.5 + 0.5 * Math.sin(pulse);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center relative overflow-hidden"
      style={{ background: "linear-gradient(180deg,#0a1e0f 0%,#143c1e 100%)" }}>
      {/* decorative field lines */}
      <div className="absolute inset-0 flex items-center justify-center pointer-events-none">
        <div className="w-60 h-60 rounded-full border-2 border-white/10" />
        <div className="absolute w-px h-[80%] bg-white/10" />
      </div>

      {/* title */}
      <h1
        className="text-5xl md:text-6xl font-bold text-white mb-2 tracking-wide"
        style={{
          textShadow: `0 0 ${20 + 30 * glow}px rgba(255,255,255,${0.3 + 0.4 * glow})`,
        }}
      >
        SOCCER SIMULATION
      </h1>
      <p className="text-gray-300 text-lg mb-8">Choose your game mode</p>

      {/* animated ball */}
      <div
        className="mb-10"
        style={{
          transform: `translateX(${Math.sin(pulse * 0.5) * 120}px)`,
        }}
      >
        <SoccerBallIcon size={48} rotation={pulse * 0.3} />
      </div>

      {/* buttons */}
      <div className="flex flex-col gap-4 z-10">
        <MenuButton label="2 PLAYERS" color="#229954" onClick={() => onStart("twoPlayer")} />
        <MenuButton label="VS COMPUTER" color="#e74c3c" onClick={() => onStart("vsComputer")} />
      </div>

      {/* controls hint */}
      <div className="mt-10 text-gray-400 text-sm text-center">
        <p>Player 1: W / A / S / D</p>
        <p>Player 2: Arrow Keys</p>
      </div>
    </div>
  );
}

function MenuButton({ label, color, onClick }: { label: string; color: string; onClick: () => void }) {
  const [hover, setHover] = useState(false);
  return (
    <button
      onClick={onClick}
      onMouseEnter={() => setHover(true)}
      onMouseLeave={() => setHover(false)}
      className="px-12 py-4 rounded-2xl font-bold text-xl text-white transition-all duration-200 border-2 border-white/30 hover:scale-105 hover:shadow-lg"
      style={{
        backgroundColor: hover ? lightenHex(color, 30) : color,
        boxShadow: hover ? `0 4px 20px ${color}66` : "none",
      }}
    >
      {label}
    </button>
  );
}

function lightenHex(hex: string, amt: number): string {
  const r = Math.min(255, parseInt(hex.slice(1, 3), 16) + amt);
  const g = Math.min(255, parseInt(hex.slice(3, 5), 16) + amt);
  const b = Math.min(255, parseInt(hex.slice(5, 7), 16) + amt);
  return `rgb(${r},${g},${b})`;
}

function SoccerBallIcon({ size, rotation }: { size: number; rotation: number }) {
  return (
    <svg width={size} height={size} viewBox="-50 -50 100 100" style={{ transform: `rotate(${rotation}rad)` }}>
      <defs>
        <radialGradient id="ballGrad" cx="-30%" cy="-30%" r="80%">
          <stop offset="0%" stopColor="#fafafa" />
          <stop offset="100%" stopColor="#aaaab4" />
        </radialGradient>
      </defs>
      <circle r="45" fill="url(#ballGrad)" stroke="#32323c" strokeWidth="2" />
      <polygon points="0,-22 -13,-7 -8,8 8,8 13,-7" fill="#1e1e28" />
      <polygon points="-30,7 -38,17 -33,28 -22,28 -17,17" fill="#1e1e28" />
      <polygon points="30,7 38,17 33,28 22,28 17,17" fill="#1e1e28" />
      <polygon points="-12,25 -18,33 -13,42 13,42 18,33 12,25" fill="#1e1e28" />
    </svg>
  );
}
