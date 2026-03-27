import { useEffect, useRef, useState, useCallback } from "react";

const CANVAS_WIDTH = 800;
const CANVAS_HEIGHT = 500;
const GROUND_Y = 420;
const NET_X = CANVAS_WIDTH / 2;
const NET_HEIGHT = 120;
const PLAYER_RADIUS = 28;
const BALL_RADIUS = 18;
const GRAVITY = 0.45;
const PLAYER_SPEED = 5.5;
const JUMP_FORCE = -13;
const MAX_SCORE = 7;

interface Player {
  x: number;
  y: number;
  vy: number;
  vx: number;
  onGround: boolean;
  score: number;
  color: string;
  name: string;
  side: "left" | "right";
  hitting: boolean;
  hitTimer: number;
}

interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
}

interface Keys {
  [key: string]: boolean;
}

type GameMode = "menu" | "2player" | "vscomputer" | "gameover";

function drawBeachBackground(ctx: CanvasRenderingContext2D, time: number) {
  // Sky gradient
  const skyGrad = ctx.createLinearGradient(0, 0, 0, GROUND_Y);
  skyGrad.addColorStop(0, "#1a6fb5");
  skyGrad.addColorStop(0.5, "#4db8f5");
  skyGrad.addColorStop(1, "#87CEEB");
  ctx.fillStyle = skyGrad;
  ctx.fillRect(0, 0, CANVAS_WIDTH, GROUND_Y);

  // Sun
  const sunX = 680;
  const sunY = 60;
  const sunGlow = ctx.createRadialGradient(sunX, sunY, 5, sunX, sunY, 50);
  sunGlow.addColorStop(0, "rgba(255,230,100,1)");
  sunGlow.addColorStop(0.5, "rgba(255,200,50,0.7)");
  sunGlow.addColorStop(1, "rgba(255,180,0,0)");
  ctx.fillStyle = sunGlow;
  ctx.beginPath();
  ctx.arc(sunX, sunY, 50, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = "#FFE566";
  ctx.beginPath();
  ctx.arc(sunX, sunY, 28, 0, Math.PI * 2);
  ctx.fill();

  // Clouds
  const clouds = [
    { x: 80, y: 55, w: 90 },
    { x: 250, y: 40, w: 70 },
    { x: 450, y: 65, w: 80 },
  ];
  clouds.forEach((cloud) => {
    const cx = cloud.x + Math.sin(time * 0.0003 + cloud.x) * 8;
    ctx.fillStyle = "rgba(255,255,255,0.85)";
    ctx.beginPath();
    ctx.ellipse(cx, cloud.y, cloud.w, 22, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.ellipse(cx - 20, cloud.y + 6, 35, 18, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.ellipse(cx + 25, cloud.y + 5, 38, 17, 0, 0, Math.PI * 2);
    ctx.fill();
  });

  // Ocean - animated waves
  const oceanGrad = ctx.createLinearGradient(0, GROUND_Y - 80, 0, GROUND_Y);
  oceanGrad.addColorStop(0, "#0077cc");
  oceanGrad.addColorStop(1, "#005599");
  ctx.fillStyle = oceanGrad;
  ctx.beginPath();
  ctx.moveTo(0, GROUND_Y - 60);
  for (let x = 0; x <= CANVAS_WIDTH; x += 10) {
    const wave1 = Math.sin((x * 0.02) + time * 0.002) * 8;
    const wave2 = Math.sin((x * 0.04) + time * 0.003) * 4;
    ctx.lineTo(x, GROUND_Y - 60 + wave1 + wave2);
  }
  ctx.lineTo(CANVAS_WIDTH, CANVAS_HEIGHT);
  ctx.lineTo(0, CANVAS_HEIGHT);
  ctx.closePath();
  ctx.fill();

  // Wave highlights
  ctx.strokeStyle = "rgba(255,255,255,0.4)";
  ctx.lineWidth = 2;
  for (let w = 0; w < 3; w++) {
    ctx.beginPath();
    for (let x = 0; x <= CANVAS_WIDTH; x += 10) {
      const wave = Math.sin((x * 0.02) + time * 0.002 + w * 1.5) * 6;
      if (x === 0) ctx.moveTo(x, GROUND_Y - 60 + wave + w * 5);
      else ctx.lineTo(x, GROUND_Y - 60 + wave + w * 5);
    }
    ctx.stroke();
  }

  // Sand
  const sandGrad = ctx.createLinearGradient(0, GROUND_Y - 20, 0, CANVAS_HEIGHT);
  sandGrad.addColorStop(0, "#f4c877");
  sandGrad.addColorStop(0.3, "#e8b65a");
  sandGrad.addColorStop(1, "#d4934a");
  ctx.fillStyle = sandGrad;
  ctx.fillRect(0, GROUND_Y - 15, CANVAS_WIDTH, CANVAS_HEIGHT - GROUND_Y + 15);

  // Sand texture dots
  ctx.fillStyle = "rgba(180,130,60,0.3)";
  for (let i = 0; i < 60; i++) {
    const sx = (i * 97) % CANVAS_WIDTH;
    const sy = GROUND_Y + 5 + ((i * 37) % 60);
    ctx.beginPath();
    ctx.arc(sx, sy, 1.5, 0, Math.PI * 2);
    ctx.fill();
  }

  // Palm tree (decorative)
  drawPalmTree(ctx, 50, GROUND_Y - 5);
  drawPalmTree(ctx, 750, GROUND_Y - 5);

  // Seagulls
  drawSeagulls(ctx, time);
}

function drawPalmTree(ctx: CanvasRenderingContext2D, x: number, y: number) {
  ctx.strokeStyle = "#7a5230";
  ctx.lineWidth = 8;
  ctx.lineCap = "round";
  ctx.beginPath();
  ctx.moveTo(x, y);
  ctx.quadraticCurveTo(x + 10, y - 60, x + 5, y - 110);
  ctx.stroke();

  const leaves = [
    { angle: -60, len: 55 },
    { angle: -30, len: 50 },
    { angle: 0, len: 45 },
    { angle: 30, len: 52 },
    { angle: 60, len: 48 },
    { angle: 90, len: 42 },
  ];
  ctx.strokeStyle = "#2d7a2d";
  ctx.lineWidth = 5;
  leaves.forEach((leaf) => {
    ctx.beginPath();
    const rad = (leaf.angle - 90) * (Math.PI / 180);
    ctx.moveTo(x + 5, y - 110);
    ctx.lineTo(
      x + 5 + Math.cos(rad) * leaf.len,
      y - 110 + Math.sin(rad) * leaf.len
    );
    ctx.stroke();
  });
}

function drawSeagulls(ctx: CanvasRenderingContext2D, time: number) {
  const birds = [
    { bx: 150, by: 80, phase: 0 },
    { bx: 320, by: 100, phase: 1.5 },
    { bx: 500, by: 70, phase: 3 },
  ];
  ctx.strokeStyle = "rgba(50,50,50,0.7)";
  ctx.lineWidth = 1.5;
  birds.forEach((bird) => {
    const bx = bird.bx + Math.sin(time * 0.0004 + bird.phase) * 20;
    const by = bird.by + Math.sin(time * 0.0008 + bird.phase) * 5;
    const wingAmp = Math.sin(time * 0.006 + bird.phase) * 8;
    ctx.beginPath();
    ctx.moveTo(bx - 12, by);
    ctx.quadraticCurveTo(bx - 6, by - wingAmp, bx, by);
    ctx.quadraticCurveTo(bx + 6, by - wingAmp, bx + 12, by);
    ctx.stroke();
  });
}

function drawNet(ctx: CanvasRenderingContext2D) {
  // Net pole
  ctx.strokeStyle = "#ccc";
  ctx.lineWidth = 4;
  ctx.beginPath();
  ctx.moveTo(NET_X, GROUND_Y);
  ctx.lineTo(NET_X, GROUND_Y - NET_HEIGHT - 10);
  ctx.stroke();

  // Net band top
  ctx.strokeStyle = "#fff";
  ctx.lineWidth = 6;
  ctx.beginPath();
  ctx.moveTo(NET_X - 3, GROUND_Y - NET_HEIGHT);
  ctx.lineTo(NET_X + 3, GROUND_Y - NET_HEIGHT);
  ctx.stroke();

  // Net mesh
  ctx.strokeStyle = "rgba(255,255,255,0.7)";
  ctx.lineWidth = 1;
  const netLeft = NET_X - 3;
  const netRight = NET_X + 3;
  const netTop = GROUND_Y - NET_HEIGHT;
  const netBottom = GROUND_Y;

  // Vertical lines
  for (let nx = netLeft; nx <= netRight; nx += 6) {
    ctx.beginPath();
    ctx.moveTo(nx, netTop);
    ctx.lineTo(nx, netBottom);
    ctx.stroke();
  }
  // Horizontal lines
  for (let ny = netTop; ny <= netBottom; ny += 10) {
    ctx.beginPath();
    ctx.moveTo(netLeft, ny);
    ctx.lineTo(netRight, ny);
    ctx.stroke();
  }

  // Net white bands
  ctx.fillStyle = "white";
  ctx.fillRect(netLeft - 2, netTop - 3, netRight - netLeft + 4, 6);
  ctx.fillRect(netLeft - 2, netBottom - 3, netRight - netLeft + 4, 6);
}

function drawPlayer(ctx: CanvasRenderingContext2D, player: Player) {
  const { x, y, color, hitting } = player;

  // Shadow
  ctx.fillStyle = "rgba(0,0,0,0.2)";
  ctx.beginPath();
  ctx.ellipse(x, GROUND_Y + 4, PLAYER_RADIUS * 0.9, 8, 0, 0, Math.PI * 2);
  ctx.fill();

  // Body (swimsuit)
  const bodyGrad = ctx.createRadialGradient(x - 5, y - 5, 2, x, y, PLAYER_RADIUS);
  bodyGrad.addColorStop(0, lightenColor(color, 40));
  bodyGrad.addColorStop(1, color);
  ctx.fillStyle = bodyGrad;
  ctx.beginPath();
  ctx.arc(x, y, PLAYER_RADIUS, 0, Math.PI * 2);
  ctx.fill();

  // Body highlight
  ctx.fillStyle = "rgba(255,255,255,0.2)";
  ctx.beginPath();
  ctx.ellipse(x - 6, y - 8, 10, 7, -0.5, 0, Math.PI * 2);
  ctx.fill();

  // Face
  ctx.fillStyle = "#FDBCB4";
  ctx.beginPath();
  ctx.arc(x, y - 6, 13, 0, Math.PI * 2);
  ctx.fill();

  // Eyes
  ctx.fillStyle = "#333";
  ctx.beginPath();
  ctx.arc(x - 4, y - 8, 2, 0, Math.PI * 2);
  ctx.arc(x + 4, y - 8, 2, 0, Math.PI * 2);
  ctx.fill();

  // Smile
  ctx.strokeStyle = "#333";
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.arc(x, y - 4, 5, 0.2, Math.PI - 0.2);
  ctx.stroke();

  // Arm animation when hitting
  if (hitting) {
    const armAngle = player.side === "left" ? -0.5 : 0.5;
    ctx.strokeStyle = "#FDBCB4";
    ctx.lineWidth = 5;
    ctx.lineCap = "round";
    ctx.beginPath();
    ctx.moveTo(x, y);
    ctx.lineTo(
      x + Math.cos(armAngle) * 30,
      y + Math.sin(armAngle) * -25
    );
    ctx.stroke();
  }

  // Player name label
  ctx.fillStyle = "rgba(0,0,0,0.6)";
  ctx.font = "bold 11px Arial";
  ctx.textAlign = "center";
  ctx.fillText(player.name, x, y + PLAYER_RADIUS + 14);
}

function lightenColor(hex: string, amount: number): string {
  const num = parseInt(hex.replace("#", ""), 16);
  const r = Math.min(255, (num >> 16) + amount);
  const g = Math.min(255, ((num >> 8) & 0xff) + amount);
  const b = Math.min(255, (num & 0xff) + amount);
  return `rgb(${r},${g},${b})`;
}

function drawBall(ctx: CanvasRenderingContext2D, ball: Ball) {
  // Shadow
  ctx.fillStyle = "rgba(0,0,0,0.15)";
  ctx.beginPath();
  ctx.ellipse(ball.x, GROUND_Y + 5, BALL_RADIUS * 0.8, 5, 0, 0, Math.PI * 2);
  ctx.fill();

  // Ball
  const ballGrad = ctx.createRadialGradient(
    ball.x - 5, ball.y - 5, 2,
    ball.x, ball.y, BALL_RADIUS
  );
  ballGrad.addColorStop(0, "#ffffff");
  ballGrad.addColorStop(0.4, "#DDEEFF");
  ballGrad.addColorStop(1, "#88BBEE");
  ctx.fillStyle = ballGrad;
  ctx.beginPath();
  ctx.arc(ball.x, ball.y, BALL_RADIUS, 0, Math.PI * 2);
  ctx.fill();

  // Ball seam lines
  ctx.strokeStyle = "rgba(100,150,200,0.4)";
  ctx.lineWidth = 1.5;
  ctx.beginPath();
  ctx.arc(ball.x, ball.y, BALL_RADIUS, 0.2, Math.PI + 0.2);
  ctx.stroke();
  ctx.beginPath();
  ctx.arc(ball.x, ball.y, BALL_RADIUS, -Math.PI / 2 + 0.2, Math.PI / 2 - 0.2);
  ctx.stroke();

  // Shine
  ctx.fillStyle = "rgba(255,255,255,0.5)";
  ctx.beginPath();
  ctx.ellipse(ball.x - 5, ball.y - 6, 5, 3, -0.5, 0, Math.PI * 2);
  ctx.fill();
}

function drawScore(ctx: CanvasRenderingContext2D, p1: Player, p2: Player) {
  // Score panel
  ctx.fillStyle = "rgba(0,0,0,0.4)";
  ctx.beginPath();
  ctx.roundRect(CANVAS_WIDTH / 2 - 100, 8, 200, 40, 8);
  ctx.fill();

  ctx.fillStyle = "#fff";
  ctx.font = "bold 26px Arial";
  ctx.textAlign = "center";
  ctx.fillText(`${p1.score}`, CANVAS_WIDTH / 2 - 35, 38);
  ctx.fillText(":", CANVAS_WIDTH / 2, 38);
  ctx.fillText(`${p2.score}`, CANVAS_WIDTH / 2 + 35, 38);

  // Player labels under score
  ctx.font = "11px Arial";
  ctx.fillStyle = p1.color;
  ctx.fillText(p1.name, CANVAS_WIDTH / 2 - 35, 52);
  ctx.fillStyle = p2.color;
  ctx.fillText(p2.name, CANVAS_WIDTH / 2 + 35, 52);
}

function drawControls(ctx: CanvasRenderingContext2D, mode: GameMode) {
  ctx.fillStyle = "rgba(0,0,0,0.35)";
  ctx.font = "12px Arial";
  ctx.textAlign = "left";

  if (mode === "2player") {
    ctx.fillText("P1: A/D Move, W Jump", 8, 480);
    ctx.fillText("P2: ←/→ Move, ↑ Jump", 520, 480);
  } else {
    ctx.fillText("Player: A/D Move, W Jump", 8, 480);
    ctx.fillText("Computer controls right side", 480, 480);
  }
}

function initBall(lastScorer: "left" | "right" | null): Ball {
  const side = lastScorer === "right" ? "left" : "right";
  return {
    x: side === "left" ? 200 : 600,
    y: GROUND_Y - 120,
    vx: side === "left" ? 2 : -2,
    vy: -6,
  };
}

function initPlayer(side: "left" | "right", name: string, color: string): Player {
  return {
    x: side === "left" ? 200 : 600,
    y: GROUND_Y - PLAYER_RADIUS,
    vy: 0,
    vx: 0,
    onGround: true,
    score: 0,
    color,
    name,
    side,
    hitting: false,
    hitTimer: 0,
  };
}

interface VolleyballGameProps {
  mode: "2player" | "vscomputer";
  onBack: () => void;
}

export default function VolleyballGame({ mode, onBack }: VolleyballGameProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef({
    p1: initPlayer("left", "Player 1", "#e74c3c"),
    p2: initPlayer("right", mode === "2player" ? "Player 2" : "Computer", "#2980b9"),
    ball: initBall(null),
    keys: {} as Keys,
    time: 0,
    paused: false,
    winner: null as string | null,
    serving: true,
    serveTimer: 60,
  });
  const animRef = useRef<number>(0);
  const [winner, setWinner] = useState<string | null>(null);

  const resetRound = useCallback((scorer: "left" | "right") => {
    const s = stateRef.current;
    s.ball = initBall(scorer);
    s.p1.x = 200; s.p1.y = GROUND_Y - PLAYER_RADIUS; s.p1.vy = 0; s.p1.vx = 0; s.p1.onGround = true; s.p1.hitting = false;
    s.p2.x = 600; s.p2.y = GROUND_Y - PLAYER_RADIUS; s.p2.vy = 0; s.p2.vx = 0; s.p2.onGround = true; s.p2.hitting = false;
    s.serving = true;
    s.serveTimer = 90;
  }, []);

  useEffect(() => {
    const handleKey = (e: KeyboardEvent, down: boolean) => {
      stateRef.current.keys[e.code] = down;
      if (["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", "Space"].includes(e.code)) {
        e.preventDefault();
      }
    };
    window.addEventListener("keydown", (e) => handleKey(e, true));
    window.addEventListener("keyup", (e) => handleKey(e, false));
    return () => {
      window.removeEventListener("keydown", (e) => handleKey(e, true));
      window.removeEventListener("keyup", (e) => handleKey(e, false));
    };
  }, []);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d")!;

    const updatePlayer = (p: Player, left: boolean, right: boolean, jump: boolean) => {
      if (left) p.vx = Math.max(p.vx - 1.5, -PLAYER_SPEED);
      else if (right) p.vx = Math.min(p.vx + 1.5, PLAYER_SPEED);
      else p.vx *= 0.75;

      if (jump && p.onGround) {
        p.vy = JUMP_FORCE;
        p.onGround = false;
      }

      p.vy += GRAVITY;
      p.x += p.vx;
      p.y += p.vy;

      // Ground collision
      if (p.y >= GROUND_Y - PLAYER_RADIUS) {
        p.y = GROUND_Y - PLAYER_RADIUS;
        p.vy = 0;
        p.onGround = true;
      }

      // Side constraints based on side
      if (p.side === "left") {
        p.x = Math.max(PLAYER_RADIUS + 5, Math.min(NET_X - PLAYER_RADIUS - 5, p.x));
      } else {
        p.x = Math.max(NET_X + PLAYER_RADIUS + 5, Math.min(CANVAS_WIDTH - PLAYER_RADIUS - 5, p.x));
      }

      // Hit timer
      if (p.hitTimer > 0) p.hitTimer--;
      else p.hitting = false;
    };

    const computeAI = (p: Player, ball: Ball) => {
      // AI logic: move toward ball
      const targetX = ball.x;
      const diff = targetX - p.x;
      const moveRight = diff > 20;
      const moveLeft = diff < -20;
      const jump = ball.vy < 0 && ball.y < GROUND_Y - 100 && Math.abs(ball.x - p.x) < 100 && p.onGround;
      updatePlayer(p, moveLeft, moveRight, jump);
    };

    const checkBallPlayerCollision = (p: Player, ball: Ball) => {
      const dx = ball.x - p.x;
      const dy = ball.y - p.y;
      const dist = Math.sqrt(dx * dx + dy * dy);
      const minDist = PLAYER_RADIUS + BALL_RADIUS;
      if (dist < minDist) {
        const nx = dx / dist;
        const ny = dy / dist;
        const relVx = ball.vx - p.vx;
        const relVy = ball.vy - p.vy;
        const dot = relVx * nx + relVy * ny;
        if (dot < 0) {
          const impulse = -dot * 1.5;
          ball.vx += impulse * nx;
          ball.vy += impulse * ny;
          // Make sure it goes up
          if (ball.vy > -3) ball.vy = -8;
          // Cap speed
          const speed = Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
          if (speed > 18) {
            ball.vx = (ball.vx / speed) * 18;
            ball.vy = (ball.vy / speed) * 18;
          }
          // Push ball out
          ball.x = p.x + nx * (minDist + 2);
          ball.y = p.y + ny * (minDist + 2);
          p.hitting = true;
          p.hitTimer = 12;
        }
      }
    };

    const gameLoop = () => {
      const s = stateRef.current;
      s.time++;

      if (!s.paused && !s.winner) {
        // Serve countdown
        if (s.serving) {
          s.serveTimer--;
          if (s.serveTimer <= 0) s.serving = false;
        }

        if (!s.serving) {
          // Player 1 controls: A/D/W
          const keys = s.keys;
          updatePlayer(
            s.p1,
            keys["KeyA"] || keys["ArrowLeft"] && false,
            keys["KeyD"] || keys["ArrowRight"] && false,
            keys["KeyW"] || keys["Space"] && false
          );
          // Fix: only A/D/W for p1, arrows for p2
          updatePlayer(
            s.p1,
            keys["KeyA"],
            keys["KeyD"],
            keys["KeyW"]
          );

          if (mode === "2player") {
            updatePlayer(s.p2, keys["ArrowLeft"], keys["ArrowRight"], keys["ArrowUp"]);
          } else {
            computeAI(s.p2, s.ball);
          }

          // Ball physics
          s.ball.vy += GRAVITY;
          s.ball.x += s.ball.vx;
          s.ball.y += s.ball.vy;

          // Ball-player collisions
          checkBallPlayerCollision(s.p1, s.ball);
          checkBallPlayerCollision(s.p2, s.ball);

          // Net collision
          const netLeft = NET_X - 4;
          const netRight = NET_X + 4;
          const netTop = GROUND_Y - NET_HEIGHT;
          if (
            s.ball.x + BALL_RADIUS > netLeft &&
            s.ball.x - BALL_RADIUS < netRight &&
            s.ball.y + BALL_RADIUS > netTop
          ) {
            if (s.ball.x < NET_X) {
              s.ball.x = netLeft - BALL_RADIUS - 1;
              s.ball.vx = Math.abs(s.ball.vx) * -1.1;
            } else {
              s.ball.x = netRight + BALL_RADIUS + 1;
              s.ball.vx = Math.abs(s.ball.vx) * 1.1;
            }
            s.ball.vy *= 0.8;
          }

          // Wall collisions
          if (s.ball.x - BALL_RADIUS < 0) {
            s.ball.x = BALL_RADIUS;
            s.ball.vx = Math.abs(s.ball.vx);
          }
          if (s.ball.x + BALL_RADIUS > CANVAS_WIDTH) {
            s.ball.x = CANVAS_WIDTH - BALL_RADIUS;
            s.ball.vx = -Math.abs(s.ball.vx);
          }

          // Ball ceiling
          if (s.ball.y - BALL_RADIUS < 0) {
            s.ball.y = BALL_RADIUS;
            s.ball.vy = Math.abs(s.ball.vy);
          }

          // Ball hits ground → score
          if (s.ball.y + BALL_RADIUS >= GROUND_Y) {
            s.ball.y = GROUND_Y - BALL_RADIUS;
            const scorer = s.ball.x < NET_X ? "right" : "left";
            if (scorer === "left") {
              s.p1.score++;
              if (s.p1.score >= MAX_SCORE) {
                s.winner = s.p1.name;
                setWinner(s.p1.name);
              } else {
                resetRound("right");
              }
            } else {
              s.p2.score++;
              if (s.p2.score >= MAX_SCORE) {
                s.winner = s.p2.name;
                setWinner(s.p2.name);
              } else {
                resetRound("left");
              }
            }
          }
        }
      }

      // Draw
      drawBeachBackground(ctx, s.time);
      drawNet(ctx);
      drawPlayer(ctx, s.p1);
      drawPlayer(ctx, s.p2);
      drawBall(ctx, s.ball);
      drawScore(ctx, s.p1, s.p2);
      drawControls(ctx, mode);

      // Serve message
      if (s.serving && !s.winner) {
        ctx.fillStyle = "rgba(0,0,0,0.5)";
        ctx.fillRect(CANVAS_WIDTH / 2 - 110, CANVAS_HEIGHT / 2 - 30, 220, 60);
        ctx.fillStyle = "#fff";
        ctx.font = "bold 20px Arial";
        ctx.textAlign = "center";
        ctx.fillText("Get Ready!", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 6);
      }

      // Winner overlay
      if (s.winner) {
        ctx.fillStyle = "rgba(0,0,0,0.6)";
        ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        ctx.fillStyle = "#FFE566";
        ctx.font = "bold 42px Arial";
        ctx.textAlign = "center";
        ctx.fillText("🏆 " + s.winner + " Wins!", CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 - 20);
        ctx.fillStyle = "#fff";
        ctx.font = "20px Arial";
        ctx.fillText("Final Score: " + s.p1.score + " - " + s.p2.score, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 25);
      }

      animRef.current = requestAnimationFrame(gameLoop);
    };

    animRef.current = requestAnimationFrame(gameLoop);
    return () => cancelAnimationFrame(animRef.current);
  }, [mode, resetRound]);

  const handleRestart = () => {
    const s = stateRef.current;
    s.p1 = initPlayer("left", "Player 1", "#e74c3c");
    s.p2 = initPlayer("right", mode === "2player" ? "Player 2" : "Computer", "#2980b9");
    s.ball = initBall(null);
    s.winner = null;
    s.serving = true;
    s.serveTimer = 90;
    setWinner(null);
  };

  return (
    <div className="relative flex flex-col items-center">
      <canvas
        ref={canvasRef}
        width={CANVAS_WIDTH}
        height={CANVAS_HEIGHT}
        style={{ borderRadius: "12px", boxShadow: "0 8px 32px rgba(0,0,0,0.4)", border: "3px solid rgba(255,255,255,0.2)" }}
      />
      <div className="flex gap-4 mt-4">
        <button
          onClick={handleRestart}
          className="px-5 py-2 bg-yellow-400 hover:bg-yellow-300 text-black font-bold rounded-lg shadow transition"
        >
          Restart Game
        </button>
        <button
          onClick={onBack}
          className="px-5 py-2 bg-white/20 hover:bg-white/30 text-white font-bold rounded-lg shadow transition"
        >
          Main Menu
        </button>
      </div>
      {winner && (
        <div className="mt-3 text-yellow-300 font-bold text-xl animate-bounce">
          {winner} is the Champion!
        </div>
      )}
    </div>
  );
}
