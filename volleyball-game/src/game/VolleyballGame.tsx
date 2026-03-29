import { useEffect, useRef, useState, useCallback } from "react";

const CANVAS_WIDTH = 800;
const CANVAS_HEIGHT = 450;
const GROUND_Y = 380;
const NET_X = CANVAS_WIDTH / 2;
const NET_HEIGHT = 120;
const PLAYER_WIDTH = 40;
const PLAYER_HEIGHT = 60;
const BALL_RADIUS = 16;
const GRAVITY = 0.45;
const JUMP_FORCE = -13;
const PLAYER_SPEED = 5;
const BALL_DAMPING = 0.7;
const MAX_SCORE = 7;

type GameMode = "menu" | "2player" | "vs-computer" | "gameover";

interface Player {
  x: number;
  y: number;
  vx: number;
  vy: number;
  onGround: boolean;
  score: number;
  side: "left" | "right";
  color: string;
  name: string;
  hitting: boolean;
  hitTimer: number;
  facingRight: boolean;
}

interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  spin: number;
  trail: { x: number; y: number }[];
}

function makePlayer(side: "left" | "right", name: string, color: string): Player {
  return {
    x: side === "left" ? 200 : 600,
    y: GROUND_Y - PLAYER_HEIGHT,
    vx: 0,
    vy: 0,
    onGround: true,
    score: 0,
    side,
    color,
    name,
    hitting: false,
    hitTimer: 0,
    facingRight: side === "left",
  };
}

function makeBall(side: "left" | "right"): Ball {
  return {
    x: side === "left" ? 200 : 600,
    y: GROUND_Y - 120,
    vx: side === "left" ? 3 : -3,
    vy: -5,
    spin: 0,
    trail: [],
  };
}

function drawBackground(ctx: CanvasRenderingContext2D) {
  // Sky gradient - NYC blue
  const skyGrad = ctx.createLinearGradient(0, 0, 0, GROUND_Y);
  skyGrad.addColorStop(0, "#87CEEB");
  skyGrad.addColorStop(1, "#B0E0FF");
  ctx.fillStyle = skyGrad;
  ctx.fillRect(0, 0, CANVAS_WIDTH, GROUND_Y);

  // NYC skyline silhouette
  ctx.fillStyle = "rgba(30, 30, 60, 0.55)";
  const buildings = [
    { x: 0, w: 60, h: 120 },
    { x: 65, w: 40, h: 90 },
    { x: 110, w: 50, h: 150 },
    { x: 165, w: 35, h: 100 },
    { x: 205, w: 45, h: 130 },
    { x: 255, w: 55, h: 80 },
    { x: 580, w: 55, h: 80 },
    { x: 640, w: 45, h: 130 },
    { x: 690, w: 35, h: 100 },
    { x: 730, w: 50, h: 150 },
    { x: 785, w: 40, h: 90 },
    { x: 730, w: 60, h: 120 },
  ];
  buildings.forEach((b) => {
    ctx.fillRect(b.x, GROUND_Y - b.h, b.w, b.h);
    // Windows
    ctx.fillStyle = "rgba(255, 255, 180, 0.7)";
    for (let wx = b.x + 6; wx < b.x + b.w - 6; wx += 10) {
      for (let wy = GROUND_Y - b.h + 10; wy < GROUND_Y - 10; wy += 14) {
        if (Math.random() > 0.3) ctx.fillRect(wx, wy, 5, 7);
      }
    }
    ctx.fillStyle = "rgba(30, 30, 60, 0.55)";
  });

  // Ground - sand/concrete court
  const groundGrad = ctx.createLinearGradient(0, GROUND_Y, 0, CANVAS_HEIGHT);
  groundGrad.addColorStop(0, "#D2B48C");
  groundGrad.addColorStop(0.3, "#C19A6B");
  groundGrad.addColorStop(1, "#A0785A");
  ctx.fillStyle = groundGrad;
  ctx.fillRect(0, GROUND_Y, CANVAS_WIDTH, CANVAS_HEIGHT - GROUND_Y);

  // Court lines
  ctx.strokeStyle = "rgba(255,255,255,0.6)";
  ctx.lineWidth = 2;
  ctx.setLineDash([]);
  ctx.beginPath();
  ctx.moveTo(50, GROUND_Y);
  ctx.lineTo(50, GROUND_Y + 10);
  ctx.moveTo(750, GROUND_Y);
  ctx.lineTo(750, GROUND_Y + 10);
  ctx.stroke();

  // Center line dashes
  ctx.setLineDash([8, 6]);
  ctx.beginPath();
  ctx.moveTo(NET_X, GROUND_Y);
  ctx.lineTo(NET_X, CANVAS_HEIGHT);
  ctx.stroke();
  ctx.setLineDash([]);

  // Baseline
  ctx.strokeStyle = "rgba(255,255,255,0.4)";
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.moveTo(50, GROUND_Y + 2);
  ctx.lineTo(750, GROUND_Y + 2);
  ctx.stroke();
}

function drawNet(ctx: CanvasRenderingContext2D) {
  const netTop = GROUND_Y - NET_HEIGHT;

  // Net post left
  ctx.fillStyle = "#888";
  ctx.beginPath();
  ctx.roundRect(NET_X - 4, netTop - 8, 8, NET_HEIGHT + 8, 3);
  ctx.fill();

  // Net post right shadow
  ctx.fillStyle = "#555";
  ctx.fillRect(NET_X - 4, netTop, 8, 4);

  // Net mesh
  ctx.strokeStyle = "rgba(255,255,255,0.85)";
  ctx.lineWidth = 1.5;

  // Vertical strings
  for (let x = NET_X - 2; x <= NET_X + 2; x += 2) {
    ctx.beginPath();
    ctx.moveTo(x, netTop);
    ctx.lineTo(x, GROUND_Y);
    ctx.stroke();
  }

  // Horizontal strings
  for (let y = netTop; y <= GROUND_Y; y += 12) {
    ctx.beginPath();
    ctx.moveTo(NET_X - 3, y);
    ctx.lineTo(NET_X + 3, y);
    ctx.stroke();
  }

  // Net top tape
  ctx.fillStyle = "#fff";
  ctx.fillRect(NET_X - 4, netTop - 4, 8, 6);
}

function drawPlayer(ctx: CanvasRenderingContext2D, p: Player) {
  ctx.save();
  const cx = p.x + PLAYER_WIDTH / 2;
  const cy = p.y;

  // Shadow
  ctx.fillStyle = "rgba(0,0,0,0.2)";
  ctx.beginPath();
  ctx.ellipse(cx, GROUND_Y + 4, 22, 6, 0, 0, Math.PI * 2);
  ctx.fill();

  // Hit effect
  if (p.hitting) {
    ctx.strokeStyle = p.color;
    ctx.lineWidth = 3;
    ctx.globalAlpha = p.hitTimer / 8;
    ctx.beginPath();
    ctx.arc(cx, cy + PLAYER_HEIGHT / 2, 30 + (8 - p.hitTimer) * 3, 0, Math.PI * 2);
    ctx.stroke();
    ctx.globalAlpha = 1;
  }

  // Body
  const bodyGrad = ctx.createLinearGradient(p.x, cy, p.x + PLAYER_WIDTH, cy + PLAYER_HEIGHT);
  bodyGrad.addColorStop(0, p.color);
  bodyGrad.addColorStop(1, shadeColor(p.color, -30));
  ctx.fillStyle = bodyGrad;
  ctx.beginPath();
  ctx.roundRect(p.x + 4, cy + 22, PLAYER_WIDTH - 8, PLAYER_HEIGHT - 22, [6, 6, 4, 4]);
  ctx.fill();

  // Jersey number
  ctx.fillStyle = "rgba(255,255,255,0.8)";
  ctx.font = "bold 14px Arial";
  ctx.textAlign = "center";
  ctx.fillText(p.side === "left" ? "1" : "2", cx, cy + 44);

  // Head
  ctx.fillStyle = "#FDBCB4";
  ctx.beginPath();
  ctx.arc(cx, cy + 14, 14, 0, Math.PI * 2);
  ctx.fill();

  // Hair
  ctx.fillStyle = p.side === "left" ? "#4A2C0A" : "#1A1A3E";
  ctx.beginPath();
  ctx.arc(cx, cy + 14, 14, Math.PI, Math.PI * 2);
  ctx.fill();

  // Eyes
  const eyeOffX = p.facingRight ? 4 : -4;
  ctx.fillStyle = "#fff";
  ctx.beginPath();
  ctx.ellipse(cx + eyeOffX - 3, cy + 13, 3, 3.5, 0, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.ellipse(cx + eyeOffX + 3, cy + 13, 3, 3.5, 0, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = "#333";
  ctx.beginPath();
  ctx.arc(cx + eyeOffX - 3, cy + 13, 1.5, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.arc(cx + eyeOffX + 3, cy + 13, 1.5, 0, Math.PI * 2);
  ctx.fill();

  // Arms - hitting pose
  ctx.strokeStyle = "#FDBCB4";
  ctx.lineWidth = 7;
  ctx.lineCap = "round";
  if (p.hitting) {
    // Raised arm
    const armDir = p.facingRight ? 1 : -1;
    ctx.beginPath();
    ctx.moveTo(cx - armDir * 6, cy + 28);
    ctx.lineTo(cx + armDir * 18, cy + 16);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx + armDir * 6, cy + 28);
    ctx.lineTo(cx - armDir * 10, cy + 36);
    ctx.stroke();
  } else {
    ctx.beginPath();
    ctx.moveTo(cx - 6, cy + 28);
    ctx.lineTo(cx - 16, cy + 42);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx + 6, cy + 28);
    ctx.lineTo(cx + 16, cy + 42);
    ctx.stroke();
  }

  // Legs
  ctx.strokeStyle = p.color;
  ctx.lineWidth = 9;
  if (!p.onGround) {
    ctx.beginPath();
    ctx.moveTo(cx - 5, cy + PLAYER_HEIGHT - 5);
    ctx.lineTo(cx - 12, cy + PLAYER_HEIGHT + 10);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx + 5, cy + PLAYER_HEIGHT - 5);
    ctx.lineTo(cx + 14, cy + PLAYER_HEIGHT + 10);
    ctx.stroke();
  } else {
    ctx.beginPath();
    ctx.moveTo(cx - 5, cy + PLAYER_HEIGHT - 5);
    ctx.lineTo(cx - 8, GROUND_Y);
    ctx.stroke();
    ctx.beginPath();
    ctx.moveTo(cx + 5, cy + PLAYER_HEIGHT - 5);
    ctx.lineTo(cx + 8, GROUND_Y);
    ctx.stroke();
  }

  // Name label
  ctx.fillStyle = "rgba(0,0,0,0.7)";
  ctx.fillRect(cx - 24, cy - 20, 48, 18);
  ctx.fillStyle = "#fff";
  ctx.font = "bold 11px Arial";
  ctx.textAlign = "center";
  ctx.fillText(p.name, cx, cy - 6);

  ctx.restore();
}

function drawBall(ctx: CanvasRenderingContext2D, ball: Ball) {
  // Trail
  ball.trail.forEach((t, i) => {
    const alpha = (i / ball.trail.length) * 0.35;
    const r = BALL_RADIUS * (i / ball.trail.length) * 0.7;
    ctx.fillStyle = `rgba(255, 220, 50, ${alpha})`;
    ctx.beginPath();
    ctx.arc(t.x, t.y, r, 0, Math.PI * 2);
    ctx.fill();
  });

  // Shadow
  ctx.fillStyle = "rgba(0,0,0,0.25)";
  ctx.beginPath();
  ctx.ellipse(ball.x, GROUND_Y + 5, 14, 5, 0, 0, Math.PI * 2);
  ctx.fill();

  // Ball glow
  const glow = ctx.createRadialGradient(ball.x - 4, ball.y - 4, 2, ball.x, ball.y, BALL_RADIUS + 4);
  glow.addColorStop(0, "rgba(255,255,200,0.3)");
  glow.addColorStop(1, "rgba(255,200,0,0)");
  ctx.fillStyle = glow;
  ctx.beginPath();
  ctx.arc(ball.x, ball.y, BALL_RADIUS + 4, 0, Math.PI * 2);
  ctx.fill();

  // Ball body
  const ballGrad = ctx.createRadialGradient(ball.x - 5, ball.y - 5, 2, ball.x, ball.y, BALL_RADIUS);
  ballGrad.addColorStop(0, "#FFF176");
  ballGrad.addColorStop(0.5, "#FFD700");
  ballGrad.addColorStop(1, "#E65100");
  ctx.fillStyle = ballGrad;
  ctx.beginPath();
  ctx.arc(ball.x, ball.y, BALL_RADIUS, 0, Math.PI * 2);
  ctx.fill();

  // Panel lines
  ctx.strokeStyle = "rgba(180,80,0,0.5)";
  ctx.lineWidth = 1.2;
  ctx.save();
  ctx.translate(ball.x, ball.y);
  ctx.rotate(ball.spin);
  for (let i = 0; i < 5; i++) {
    ctx.beginPath();
    ctx.arc(0, 0, BALL_RADIUS, (i / 5) * Math.PI * 2, (i / 5 + 0.15) * Math.PI * 2);
    ctx.stroke();
  }
  ctx.restore();

  // Highlight
  ctx.fillStyle = "rgba(255,255,255,0.45)";
  ctx.beginPath();
  ctx.ellipse(ball.x - 5, ball.y - 5, 5, 3, -0.5, 0, Math.PI * 2);
  ctx.fill();
}

function shadeColor(hex: string, amount: number): string {
  const num = parseInt(hex.replace("#", ""), 16);
  const r = Math.max(0, Math.min(255, (num >> 16) + amount));
  const g = Math.max(0, Math.min(255, ((num >> 8) & 0xff) + amount));
  const b = Math.max(0, Math.min(255, (num & 0xff) + amount));
  return `rgb(${r},${g},${b})`;
}

function drawHUD(ctx: CanvasRenderingContext2D, p1: Player, p2: Player, mode: string) {
  // Score panel background
  ctx.fillStyle = "rgba(0,0,0,0.55)";
  ctx.beginPath();
  ctx.roundRect(CANVAS_WIDTH / 2 - 100, 10, 200, 54, 12);
  ctx.fill();

  // NYC crown
  ctx.fillStyle = "#FFD700";
  ctx.font = "bold 11px Arial";
  ctx.textAlign = "center";
  ctx.fillText("🏐 NYC VOLLEYBALL", CANVAS_WIDTH / 2, 26);

  // Scores
  ctx.font = "bold 28px Arial";
  ctx.fillStyle = p1.color;
  ctx.textAlign = "right";
  ctx.fillText(String(p1.score), CANVAS_WIDTH / 2 - 18, 54);
  ctx.fillStyle = "#fff";
  ctx.textAlign = "center";
  ctx.font = "bold 20px Arial";
  ctx.fillText(":", CANVAS_WIDTH / 2, 52);
  ctx.font = "bold 28px Arial";
  ctx.fillStyle = p2.color;
  ctx.textAlign = "left";
  ctx.fillText(String(p2.score), CANVAS_WIDTH / 2 + 18, 54);

  // Player name labels
  ctx.font = "12px Arial";
  ctx.fillStyle = p1.color;
  ctx.textAlign = "left";
  ctx.fillText(p1.name, 14, 28);
  ctx.fillStyle = p2.color;
  ctx.textAlign = "right";
  ctx.fillText(p2.name, CANVAS_WIDTH - 14, 28);

  // Mode badge
  ctx.fillStyle = "rgba(255,255,255,0.15)";
  ctx.beginPath();
  ctx.roundRect(CANVAS_WIDTH - 110, 10, 100, 22, 6);
  ctx.fill();
  ctx.fillStyle = "#fff";
  ctx.font = "10px Arial";
  ctx.textAlign = "center";
  ctx.fillText(mode === "vs-computer" ? "VS COMPUTER" : "2 PLAYERS", CANVAS_WIDTH - 60, 25);
}

function drawControls(ctx: CanvasRenderingContext2D, mode: string) {
  ctx.fillStyle = "rgba(0,0,0,0.45)";
  ctx.beginPath();
  ctx.roundRect(8, CANVAS_HEIGHT - 52, 180, 44, 8);
  ctx.fill();
  ctx.fillStyle = "rgba(255,255,255,0.85)";
  ctx.font = "10px Arial";
  ctx.textAlign = "left";
  ctx.fillText("Player 1: A/D=Move  W=Jump", 16, CANVAS_HEIGHT - 36);
  ctx.fillText("Space/S = Hit", 16, CANVAS_HEIGHT - 22);

  if (mode === "2player") {
    ctx.fillStyle = "rgba(0,0,0,0.45)";
    ctx.beginPath();
    ctx.roundRect(CANVAS_WIDTH - 188, CANVAS_HEIGHT - 52, 180, 44, 8);
    ctx.fill();
    ctx.fillStyle = "rgba(255,255,255,0.85)";
    ctx.font = "10px Arial";
    ctx.textAlign = "right";
    ctx.fillText("Player 2: ←/→=Move  ↑=Jump", CANVAS_WIDTH - 16, CANVAS_HEIGHT - 36);
    ctx.fillText("Enter/↓ = Hit", CANVAS_WIDTH - 16, CANVAS_HEIGHT - 22);
  }
}

function clamp(v: number, min: number, max: number) {
  return Math.max(min, Math.min(max, v));
}

export default function VolleyballGame() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [gameMode, setGameMode] = useState<GameMode>("menu");
  const [winner, setWinner] = useState<string>("");
  const stateRef = useRef<{
    p1: Player;
    p2: Player;
    ball: Ball;
    keys: Set<string>;
    serving: "left" | "right";
    rallyActive: boolean;
    frameCount: number;
    pointDelay: number;
    mode: GameMode;
    particles: { x: number; y: number; vx: number; vy: number; color: string; life: number }[];
  } | null>(null);
  const animFrameRef = useRef<number>(0);

  const startGame = useCallback((mode: "2player" | "vs-computer") => {
    const p1 = makePlayer("left", "Player 1", "#FF6B35");
    const p2 = makePlayer("right", mode === "vs-computer" ? "CPU" : "Player 2", "#4ECDC4");
    stateRef.current = {
      p1,
      p2,
      ball: makeBall("left"),
      keys: new Set(),
      serving: "left",
      rallyActive: false,
      frameCount: 0,
      pointDelay: 0,
      mode,
      particles: [],
    };
    setGameMode(mode);
    setWinner("");
  }, []);

  useEffect(() => {
    if (gameMode !== "2player" && gameMode !== "vs-computer") return;

    const canvas = canvasRef.current!;
    const ctx = canvas.getContext("2d")!;
    const state = stateRef.current!;

    const handleKeyDown = (e: KeyboardEvent) => {
      state.keys.add(e.key);
      if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", " "].includes(e.key)) {
        e.preventDefault();
      }
    };
    const handleKeyUp = (e: KeyboardEvent) => state.keys.delete(e.key);

    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);

    function spawnParticles(x: number, y: number, color: string, count = 10) {
      for (let i = 0; i < count; i++) {
        state.particles.push({
          x,
          y,
          vx: (Math.random() - 0.5) * 8,
          vy: (Math.random() - 0.5) * 8 - 4,
          color,
          life: 30,
        });
      }
    }

    function tryHit(player: Player, ball: Ball, power = 1): boolean {
      const bx = ball.x;
      const by = ball.y;
      const px = player.x + PLAYER_WIDTH / 2;
      const py = player.y + PLAYER_HEIGHT / 2;
      const dist = Math.hypot(bx - px, by - py);
      if (dist < PLAYER_WIDTH + BALL_RADIUS + 8) {
        const angle = Math.atan2(by - py, bx - px);
        const speed = 10 * power;
        ball.vx = Math.cos(angle) * speed;
        ball.vy = Math.sin(angle) * speed - 4;
        ball.spin = ball.vx * 0.1;

        // Push away from net
        if (player.side === "left" && ball.vx < 2) ball.vx = 5 + Math.random() * 3;
        if (player.side === "right" && ball.vx > -2) ball.vx = -(5 + Math.random() * 3);

        player.hitting = true;
        player.hitTimer = 8;
        spawnParticles(ball.x, ball.y, "#FFD700", 8);
        return true;
      }
      return false;
    }

    function computerAI(p2: Player, ball: Ball) {
      const targetX = ball.x - PLAYER_WIDTH / 2;
      const diff = targetX - p2.x;
      const speed = PLAYER_SPEED * 0.78;

      if (Math.abs(diff) > 5) {
        p2.vx = diff > 0 ? speed : -speed;
        p2.facingRight = diff > 0;
      } else {
        p2.vx = 0;
      }

      // Jump if ball is coming and low enough to hit
      const ballOnRightSide = ball.x > NET_X + 10;
      const ballApproaching = ball.vx < 0 || (ball.x > NET_X + 50 && ball.y < GROUND_Y - 30);
      if (ballOnRightSide && ballApproaching && p2.onGround && ball.y < GROUND_Y - 60) {
        if (Math.abs(ball.x - (p2.x + PLAYER_WIDTH / 2)) < 100) {
          p2.vy = JUMP_FORCE;
          p2.onGround = false;
        }
      }

      // Hit the ball
      if (ballOnRightSide) {
        tryHit(p2, ball, 1.05);
      }
    }

    function awardPoint(scorer: "left" | "right") {
      const s = state;
      if (scorer === "left") {
        s.p1.score++;
        spawnParticles(200, 300, s.p1.color, 20);
      } else {
        s.p2.score++;
        spawnParticles(600, 300, s.p2.color, 20);
      }

      if (s.p1.score >= MAX_SCORE || s.p2.score >= MAX_SCORE) {
        const win = s.p1.score >= MAX_SCORE ? s.p1.name : s.p2.name;
        setWinner(win);
        setGameMode("gameover");
        return;
      }

      s.serving = scorer;
      s.rallyActive = false;
      s.pointDelay = 80;
      s.ball = makeBall(scorer);
      s.p1.x = 160;
      s.p1.y = GROUND_Y - PLAYER_HEIGHT;
      s.p1.vx = 0; s.p1.vy = 0; s.p1.onGround = true;
      s.p2.x = 560;
      s.p2.y = GROUND_Y - PLAYER_HEIGHT;
      s.p2.vx = 0; s.p2.vy = 0; s.p2.onGround = true;
    }

    function updatePhysics() {
      const s = state;
      const { p1, p2, ball, keys } = s;

      if (s.pointDelay > 0) {
        s.pointDelay--;
        return;
      }

      s.frameCount++;

      // --- Player 1 input ---
      if (keys.has("a") || keys.has("A")) { p1.vx = -PLAYER_SPEED; p1.facingRight = false; }
      else if (keys.has("d") || keys.has("D")) { p1.vx = PLAYER_SPEED; p1.facingRight = true; }
      else p1.vx = 0;

      if ((keys.has("w") || keys.has("W")) && p1.onGround) {
        p1.vy = JUMP_FORCE;
        p1.onGround = false;
      }
      if (keys.has(" ") || keys.has("s") || keys.has("S")) {
        tryHit(p1, ball);
      }

      // --- Player 2 input or AI ---
      if (s.mode === "2player") {
        if (keys.has("ArrowLeft")) { p2.vx = -PLAYER_SPEED; p2.facingRight = false; }
        else if (keys.has("ArrowRight")) { p2.vx = PLAYER_SPEED; p2.facingRight = true; }
        else p2.vx = 0;

        if (keys.has("ArrowUp") && p2.onGround) {
          p2.vy = JUMP_FORCE;
          p2.onGround = false;
        }
        if (keys.has("Enter") || keys.has("ArrowDown")) {
          tryHit(p2, ball);
        }
      } else {
        computerAI(p2, ball);
      }

      // Facing direction based on ball
      if (p1.vx === 0) p1.facingRight = ball.x > p1.x;
      if (p2.vx === 0) p2.facingRight = ball.x > p2.x;

      // Apply gravity & move players
      for (const p of [p1, p2]) {
        p.vy += GRAVITY;
        p.x += p.vx;
        p.y += p.vy;

        if (p.y >= GROUND_Y - PLAYER_HEIGHT) {
          p.y = GROUND_Y - PLAYER_HEIGHT;
          p.vy = 0;
          p.onGround = true;
        }

        if (p.hitTimer > 0) p.hitTimer--;
        if (p.hitTimer === 0) p.hitting = false;
      }

      // Constrain players to their side
      p1.x = clamp(p1.x, 10, NET_X - PLAYER_WIDTH - 6);
      p2.x = clamp(p2.x, NET_X + 6, CANVAS_WIDTH - PLAYER_WIDTH - 10);

      // Ball physics
      ball.vy += GRAVITY;
      ball.x += ball.vx;
      ball.y += ball.vy;
      ball.spin += ball.vx * 0.015;

      // Ball trail
      ball.trail.push({ x: ball.x, y: ball.y });
      if (ball.trail.length > 10) ball.trail.shift();

      // Ball vs ground
      if (ball.y + BALL_RADIUS >= GROUND_Y) {
        ball.y = GROUND_Y - BALL_RADIUS;
        ball.vy *= -BALL_DAMPING;
        ball.vx *= 0.85;
        if (Math.abs(ball.vy) < 1) ball.vy = 0;
        spawnParticles(ball.x, GROUND_Y, "#D2B48C", 4);

        // Who gets the point
        setTimeout(() => {
          if (ball.x < NET_X) awardPoint("right");
          else awardPoint("left");
        }, 400);
      }

      // Ball vs walls
      if (ball.x - BALL_RADIUS < 0) { ball.x = BALL_RADIUS; ball.vx *= -0.7; }
      if (ball.x + BALL_RADIUS > CANVAS_WIDTH) { ball.x = CANVAS_WIDTH - BALL_RADIUS; ball.vx *= -0.7; }
      if (ball.y - BALL_RADIUS < 0) { ball.y = BALL_RADIUS; ball.vy *= -0.7; }

      // Ball vs net
      const netLeft = NET_X - 4;
      const netRight = NET_X + 4;
      const netTop = GROUND_Y - NET_HEIGHT;
      if (ball.x + BALL_RADIUS > netLeft && ball.x - BALL_RADIUS < netRight && ball.y + BALL_RADIUS > netTop) {
        if (ball.vy > 0 && ball.y - BALL_RADIUS < netTop + 10) {
          ball.y = netTop - BALL_RADIUS;
          ball.vy *= -0.6;
        } else {
          if (ball.x < NET_X) { ball.x = netLeft - BALL_RADIUS; ball.vx *= -0.6; }
          else { ball.x = netRight + BALL_RADIUS; ball.vx *= -0.6; }
        }
      }

      // Particles
      for (let i = state.particles.length - 1; i >= 0; i--) {
        const pt = state.particles[i];
        pt.x += pt.vx;
        pt.y += pt.vy;
        pt.vy += 0.3;
        pt.life--;
        if (pt.life <= 0) state.particles.splice(i, 1);
      }
    }

    function render() {
      ctx.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

      // Background rendering stored separately so we can cache it
      drawBackground(ctx);
      drawNet(ctx);
      drawPlayer(ctx, state.p1);
      drawPlayer(ctx, state.p2);
      drawBall(ctx, state.ball);

      // Particles
      for (const pt of state.particles) {
        ctx.globalAlpha = pt.life / 30;
        ctx.fillStyle = pt.color;
        ctx.beginPath();
        ctx.arc(pt.x, pt.y, 4, 0, Math.PI * 2);
        ctx.fill();
      }
      ctx.globalAlpha = 1;

      drawHUD(ctx, state.p1, state.p2, state.mode);
      drawControls(ctx, state.mode);

      // Point delay message
      if (state.pointDelay > 40) {
        ctx.fillStyle = "rgba(0,0,0,0.55)";
        ctx.beginPath();
        ctx.roundRect(CANVAS_WIDTH / 2 - 80, CANVAS_HEIGHT / 2 - 24, 160, 48, 10);
        ctx.fill();
        ctx.fillStyle = "#FFD700";
        ctx.font = "bold 22px Arial";
        ctx.textAlign = "center";
        ctx.fillText("POINT!", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 8);
      }
    }

    let running = true;
    function loop() {
      if (!running) return;
      updatePhysics();
      render();
      animFrameRef.current = requestAnimationFrame(loop);
    }

    loop();

    return () => {
      running = false;
      cancelAnimationFrame(animFrameRef.current);
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
    };
  }, [gameMode]);

  return (
    <div style={{ fontFamily: "Arial, sans-serif", background: "#1a1a2e", minHeight: "100vh", display: "flex", flexDirection: "column", alignItems: "center", justifyContent: "center" }}>
      {gameMode === "menu" && (
        <div style={{ textAlign: "center", color: "#fff" }}>
          <div style={{ fontSize: 52, marginBottom: 8 }}>🏐</div>
          <h1 style={{ fontSize: 36, fontWeight: "bold", color: "#FFD700", textShadow: "0 0 20px rgba(255,215,0,0.5)", marginBottom: 4 }}>
            NYC VOLLEYBALL
          </h1>
          <p style={{ color: "#aaa", marginBottom: 32, fontSize: 14 }}>Animated · Physics-based · New York Style</p>
          <div style={{ display: "flex", gap: 16, justifyContent: "center", flexWrap: "wrap" }}>
            <button
              onClick={() => startGame("2player")}
              style={{ padding: "14px 32px", background: "linear-gradient(135deg, #FF6B35, #FF4500)", border: "none", borderRadius: 10, color: "#fff", fontSize: 18, fontWeight: "bold", cursor: "pointer", boxShadow: "0 4px 20px rgba(255,107,53,0.4)" }}
            >
              👥 2 Players
            </button>
            <button
              onClick={() => startGame("vs-computer")}
              style={{ padding: "14px 32px", background: "linear-gradient(135deg, #4ECDC4, #00B4D8)", border: "none", borderRadius: 10, color: "#fff", fontSize: 18, fontWeight: "bold", cursor: "pointer", boxShadow: "0 4px 20px rgba(78,205,196,0.4)" }}
            >
              🤖 vs Computer
            </button>
          </div>
          <div style={{ marginTop: 32, background: "rgba(255,255,255,0.07)", borderRadius: 12, padding: "16px 24px", display: "inline-block", textAlign: "left" }}>
            <p style={{ color: "#FFD700", fontWeight: "bold", marginBottom: 8 }}>Controls</p>
            <p style={{ color: "#ccc", fontSize: 13 }}>Player 1: <b style={{ color: "#FF6B35" }}>A/D</b> = Move &nbsp; <b style={{ color: "#FF6B35" }}>W</b> = Jump &nbsp; <b style={{ color: "#FF6B35" }}>Space/S</b> = Hit</p>
            <p style={{ color: "#ccc", fontSize: 13, marginTop: 4 }}>Player 2: <b style={{ color: "#4ECDC4" }}>←/→</b> = Move &nbsp; <b style={{ color: "#4ECDC4" }}>↑</b> = Jump &nbsp; <b style={{ color: "#4ECDC4" }}>Enter/↓</b> = Hit</p>
            <p style={{ color: "#888", fontSize: 12, marginTop: 8 }}>First to {MAX_SCORE} points wins!</p>
          </div>
        </div>
      )}

      {(gameMode === "2player" || gameMode === "vs-computer") && (
        <div style={{ position: "relative" }}>
          <canvas
            ref={canvasRef}
            width={CANVAS_WIDTH}
            height={CANVAS_HEIGHT}
            style={{ display: "block", borderRadius: 12, boxShadow: "0 8px 40px rgba(0,0,0,0.7)", border: "2px solid rgba(255,215,0,0.3)", maxWidth: "100%", maxHeight: "80vh" }}
          />
          <button
            onClick={() => setGameMode("menu")}
            style={{ position: "absolute", top: 12, right: 56, padding: "6px 14px", background: "rgba(0,0,0,0.6)", border: "1px solid rgba(255,255,255,0.3)", borderRadius: 6, color: "#fff", fontSize: 12, cursor: "pointer" }}
          >
            Menu
          </button>
        </div>
      )}

      {gameMode === "gameover" && (
        <div style={{ textAlign: "center", color: "#fff" }}>
          <div style={{ fontSize: 56, marginBottom: 8 }}>🏆</div>
          <h2 style={{ fontSize: 40, fontWeight: "bold", color: "#FFD700", textShadow: "0 0 30px rgba(255,215,0,0.6)", marginBottom: 8 }}>
            {winner} WINS!
          </h2>
          <p style={{ color: "#aaa", marginBottom: 28, fontSize: 16 }}>
            {stateRef.current?.p1.score} – {stateRef.current?.p2.score}
          </p>
          <div style={{ display: "flex", gap: 14, justifyContent: "center" }}>
            <button
              onClick={() => startGame(stateRef.current?.mode === "vs-computer" ? "vs-computer" : "2player")}
              style={{ padding: "12px 28px", background: "linear-gradient(135deg, #FFD700, #FF8C00)", border: "none", borderRadius: 10, color: "#1a1a2e", fontSize: 16, fontWeight: "bold", cursor: "pointer" }}
            >
              Play Again
            </button>
            <button
              onClick={() => setGameMode("menu")}
              style={{ padding: "12px 28px", background: "rgba(255,255,255,0.1)", border: "1px solid rgba(255,255,255,0.3)", borderRadius: 10, color: "#fff", fontSize: 16, cursor: "pointer" }}
            >
              Main Menu
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
