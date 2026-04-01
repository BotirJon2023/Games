import { useEffect, useRef, useState, useCallback } from "react";

const CANVAS_WIDTH = 800;
const CANVAS_HEIGHT = 500;
const GROUND_Y = 430;
const NET_X = CANVAS_WIDTH / 2;
const NET_HEIGHT = 120;
const NET_TOP = GROUND_Y - NET_HEIGHT;
const PLAYER_WIDTH = 40;
const PLAYER_HEIGHT = 70;
const BALL_RADIUS = 15;
const GRAVITY = 0.4;
const JUMP_FORCE = -12;
const PLAYER_SPEED = 5;
const WINNING_SCORE = 5;

type GameMode = "menu" | "2player" | "vs-computer" | "gameover";

interface Player {
  x: number;
  y: number;
  vy: number;
  onGround: boolean;
  score: number;
  color: string;
  name: string;
  side: "left" | "right";
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

function drawPyramids(ctx: CanvasRenderingContext2D) {
  const gradient = ctx.createLinearGradient(0, 0, 0, CANVAS_HEIGHT);
  gradient.addColorStop(0, "#4a90d9");
  gradient.addColorStop(0.4, "#87ceeb");
  gradient.addColorStop(0.7, "#f4e4b8");
  gradient.addColorStop(1, "#e8c97a");
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

  const sunGradient = ctx.createRadialGradient(650, 80, 5, 650, 80, 60);
  sunGradient.addColorStop(0, "#fff7a0");
  sunGradient.addColorStop(0.5, "#ffd700");
  sunGradient.addColorStop(1, "rgba(255,165,0,0)");
  ctx.fillStyle = sunGradient;
  ctx.beginPath();
  ctx.arc(650, 80, 60, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = "#c8a84b";
  ctx.beginPath();
  ctx.moveTo(200, GROUND_Y);
  ctx.lineTo(340, 200);
  ctx.lineTo(480, GROUND_Y);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "#b8943c";
  ctx.beginPath();
  ctx.moveTo(225, GROUND_Y);
  ctx.lineTo(340, 200);
  ctx.lineTo(340, GROUND_Y);
  ctx.closePath();
  ctx.fill();

  ctx.strokeStyle = "#a07830";
  ctx.lineWidth = 0.5;
  for (let i = 0; i < 8; i++) {
    const t = i / 8;
    const lx = 200 + (340 - 200) * t;
    const ly = GROUND_Y + (200 - GROUND_Y) * t;
    const rx = 480 - (480 - 340) * t;
    ctx.beginPath();
    ctx.moveTo(lx, ly);
    ctx.lineTo(rx, ly);
    ctx.stroke();
  }

  ctx.fillStyle = "#d4b566";
  ctx.beginPath();
  ctx.moveTo(490, GROUND_Y);
  ctx.lineTo(590, 280);
  ctx.lineTo(690, GROUND_Y);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "#c4a455";
  ctx.beginPath();
  ctx.moveTo(510, GROUND_Y);
  ctx.lineTo(590, 280);
  ctx.lineTo(590, GROUND_Y);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "#dcc07a";
  ctx.beginPath();
  ctx.moveTo(560, GROUND_Y);
  ctx.lineTo(620, 340);
  ctx.lineTo(680, GROUND_Y);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "#cdb070";
  ctx.beginPath();
  ctx.moveTo(575, GROUND_Y);
  ctx.lineTo(620, 340);
  ctx.lineTo(620, GROUND_Y);
  ctx.closePath();
  ctx.fill();

  ctx.fillStyle = "#c2955a";
  ctx.fillRect(318, GROUND_Y - 30, 44, 30);
  ctx.fillStyle = "#a07840";
  ctx.fillRect(330, GROUND_Y - 26, 8, 26);
  ctx.fillRect(342, GROUND_Y - 26, 8, 26);
  ctx.fillStyle = "#2c1810";
  ctx.beginPath();
  ctx.arc(340, GROUND_Y - 30, 12, 0, Math.PI, true);
  ctx.fill();
  ctx.fillRect(330, GROUND_Y - 30, 20, 4);
  ctx.fillStyle = "#3c2010";
  ctx.fillRect(315, GROUND_Y - 8, 7, 8);
  ctx.fillRect(338, GROUND_Y - 8, 7, 8);
  ctx.fillRect(360, GROUND_Y - 8, 7, 8);

  ctx.fillStyle = "#8B7355";
  ctx.fillRect(0, GROUND_Y, CANVAS_WIDTH, CANVAS_HEIGHT - GROUND_Y);

  ctx.fillStyle = "#9c8054";
  for (let x = 0; x < CANVAS_WIDTH; x += 60) {
    ctx.beginPath();
    ctx.ellipse(x + 30, GROUND_Y + 5, 30, 5, 0, 0, Math.PI * 2);
    ctx.fill();
  }

  ctx.fillStyle = "rgba(200,170,100,0.3)";
  for (let i = 0; i < 5; i++) {
    ctx.beginPath();
    ctx.ellipse(100 + i * 30, GROUND_Y - 2, 20, 6, Math.PI / 6, 0, Math.PI * 2);
    ctx.fill();
  }
}

function drawNet(ctx: CanvasRenderingContext2D) {
  ctx.strokeStyle = "#ffffff";
  ctx.lineWidth = 4;
  ctx.beginPath();
  ctx.moveTo(NET_X, NET_TOP);
  ctx.lineTo(NET_X, GROUND_Y);
  ctx.stroke();

  ctx.lineWidth = 2;
  ctx.strokeStyle = "rgba(255,255,255,0.6)";
  const cellSize = 15;
  for (let y = NET_TOP; y < GROUND_Y; y += cellSize) {
    for (let x = -30; x <= 30; x += cellSize) {
      if (x === 0) continue;
      ctx.beginPath();
      ctx.moveTo(NET_X + x, y);
      ctx.lineTo(NET_X + x + cellSize, y + cellSize);
      ctx.stroke();
      ctx.beginPath();
      ctx.moveTo(NET_X + x + cellSize, y);
      ctx.lineTo(NET_X + x, y + cellSize);
      ctx.stroke();
    }
  }

  ctx.strokeStyle = "#ffffff";
  ctx.lineWidth = 6;
  ctx.beginPath();
  ctx.moveTo(NET_X - 3, NET_TOP);
  ctx.lineTo(NET_X + 3, NET_TOP);
  ctx.stroke();

  ctx.fillStyle = "#555555";
  ctx.fillRect(NET_X - 3, GROUND_Y - 2, 6, 15);
  ctx.fillRect(NET_X - 3, NET_TOP - 10, 6, 15);
}

function drawPlayer(ctx: CanvasRenderingContext2D, p: Player, time: number) {
  const legSwing = Math.sin(time * 0.15) * 8;
  const armSwing = Math.sin(time * 0.15) * 12;

  ctx.save();
  ctx.translate(p.x, p.y + PLAYER_HEIGHT / 2);

  const isJumping = !p.onGround;

  ctx.fillStyle = p.color === "blue" ? "#1565c0" : "#c62828";
  ctx.beginPath();
  ctx.roundRect(-12, -PLAYER_HEIGHT / 2 + 25, 24, 28, 4);
  ctx.fill();

  ctx.fillStyle = p.color === "blue" ? "#0d47a1" : "#b71c1c";
  ctx.fillRect(-10, -PLAYER_HEIGHT / 2 + 51, 9, 18);
  ctx.fillRect(1, -PLAYER_HEIGHT / 2 + 51, 9, 18);

  ctx.fillStyle = "#f5cba7";
  ctx.beginPath();
  ctx.arc(0, -PLAYER_HEIGHT / 2 + 14, 12, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = p.color === "blue" ? "#1a237e" : "#880e4f";
  ctx.fillRect(-6, -PLAYER_HEIGHT / 2 + 24, 12, 5);

  ctx.fillStyle = "#f5cba7";
  ctx.save();
  ctx.translate(-13, -PLAYER_HEIGHT / 2 + 30);
  ctx.rotate(isJumping ? -0.8 : (armSwing * Math.PI) / 180);
  ctx.fillRect(-4, 0, 8, 20);
  ctx.restore();

  ctx.save();
  ctx.translate(13, -PLAYER_HEIGHT / 2 + 30);
  ctx.rotate(isJumping ? 0.8 : (-armSwing * Math.PI) / 180);
  ctx.fillRect(-4, 0, 8, 20);
  ctx.restore();

  if (!isJumping) {
    ctx.fillStyle = "#f5cba7";
    ctx.save();
    ctx.translate(-7, -PLAYER_HEIGHT / 2 + 53);
    ctx.rotate((legSwing * Math.PI) / 180);
    ctx.fillRect(-4, 0, 8, 18);
    ctx.fillStyle = "#333";
    ctx.fillRect(-5, 15, 10, 5);
    ctx.restore();

    ctx.save();
    ctx.translate(7, -PLAYER_HEIGHT / 2 + 53);
    ctx.rotate((-legSwing * Math.PI) / 180);
    ctx.fillRect(-4, 0, 8, 18);
    ctx.fillStyle = "#333";
    ctx.fillRect(-5, 15, 10, 5);
    ctx.restore();
  } else {
    ctx.fillStyle = "#f5cba7";
    ctx.save();
    ctx.translate(-7, -PLAYER_HEIGHT / 2 + 53);
    ctx.rotate(-0.4);
    ctx.fillRect(-4, 0, 8, 18);
    ctx.fillStyle = "#333";
    ctx.fillRect(-5, 15, 10, 5);
    ctx.restore();

    ctx.save();
    ctx.translate(7, -PLAYER_HEIGHT / 2 + 53);
    ctx.rotate(0.4);
    ctx.fillRect(-4, 0, 8, 18);
    ctx.fillStyle = "#333";
    ctx.fillRect(-5, 15, 10, 5);
    ctx.restore();
  }

  ctx.fillStyle = p.color === "blue" ? "#1565c0" : "#c62828";
  ctx.font = "bold 12px Arial";
  ctx.textAlign = "center";
  ctx.fillText(p.name, 0, -PLAYER_HEIGHT / 2 - 8);

  ctx.restore();
}

function drawBall(ctx: CanvasRenderingContext2D, ball: Ball, time: number) {
  ctx.save();
  ctx.translate(ball.x, ball.y);
  ctx.rotate(time * 0.05 * Math.sign(ball.vx));

  const gradient = ctx.createRadialGradient(-4, -4, 2, 0, 0, BALL_RADIUS);
  gradient.addColorStop(0, "#ffffff");
  gradient.addColorStop(0.3, "#f0f0f0");
  gradient.addColorStop(1, "#cccccc");

  ctx.fillStyle = gradient;
  ctx.beginPath();
  ctx.arc(0, 0, BALL_RADIUS, 0, Math.PI * 2);
  ctx.fill();

  ctx.strokeStyle = "#888888";
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.arc(0, 0, BALL_RADIUS, 0, Math.PI * 2);
  ctx.stroke();

  ctx.strokeStyle = "#aaaaaa";
  ctx.lineWidth = 1;
  for (let i = 0; i < 3; i++) {
    const angle = (i * Math.PI * 2) / 3;
    ctx.beginPath();
    ctx.arc(
      Math.cos(angle) * 4,
      Math.sin(angle) * 4,
      BALL_RADIUS - 2,
      angle - 1,
      angle + 1
    );
    ctx.stroke();
  }

  ctx.restore();

  const shadowWidth = BALL_RADIUS * 1.5;
  const shadowHeight = 5;
  const shadowY = GROUND_Y + 2;
  if (ball.y < GROUND_Y) {
    const distFactor = Math.max(0.1, 1 - (GROUND_Y - ball.y) / 300);
    ctx.save();
    ctx.fillStyle = `rgba(0,0,0,${0.3 * distFactor})`;
    ctx.beginPath();
    ctx.ellipse(ball.x, shadowY, shadowWidth * distFactor, shadowHeight * distFactor, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }
}

function drawUI(
  ctx: CanvasRenderingContext2D,
  p1: Player,
  p2: Player,
  message: string,
  gameMode: GameMode
) {
  ctx.fillStyle = "rgba(0,0,0,0.5)";
  ctx.beginPath();
  ctx.roundRect(20, 12, 120, 50, 8);
  ctx.fill();
  ctx.fillStyle = "#ffffff";
  ctx.font = "bold 14px Arial";
  ctx.textAlign = "center";
  ctx.fillText(p1.name, 80, 30);
  ctx.font = "bold 28px Arial";
  ctx.fillStyle = "#FFD700";
  ctx.fillText(String(p1.score), 80, 55);

  ctx.fillStyle = "rgba(0,0,0,0.5)";
  ctx.beginPath();
  ctx.roundRect(CANVAS_WIDTH - 140, 12, 120, 50, 8);
  ctx.fill();
  ctx.fillStyle = "#ffffff";
  ctx.font = "bold 14px Arial";
  ctx.textAlign = "center";
  ctx.fillText(p2.name, CANVAS_WIDTH - 80, 30);
  ctx.font = "bold 28px Arial";
  ctx.fillStyle = "#FFD700";
  ctx.fillText(String(p2.score), CANVAS_WIDTH - 80, 55);

  ctx.fillStyle = "rgba(0,0,0,0.4)";
  ctx.beginPath();
  ctx.roundRect(CANVAS_WIDTH / 2 - 60, 12, 120, 30, 8);
  ctx.fill();
  ctx.fillStyle = "#ffffff";
  ctx.font = "bold 13px Arial";
  ctx.textAlign = "center";
  ctx.fillText(`First to ${WINNING_SCORE}`, CANVAS_WIDTH / 2, 32);

  if (message) {
    ctx.fillStyle = "rgba(0,0,0,0.6)";
    ctx.beginPath();
    ctx.roundRect(CANVAS_WIDTH / 2 - 150, CANVAS_HEIGHT / 2 - 30, 300, 60, 12);
    ctx.fill();
    ctx.fillStyle = "#FFD700";
    ctx.font = "bold 22px Arial";
    ctx.textAlign = "center";
    ctx.fillText(message, CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 8);
  }

  if (gameMode === "2player") {
    ctx.fillStyle = "rgba(0,0,0,0.5)";
    ctx.beginPath();
    ctx.roundRect(5, CANVAS_HEIGHT - 50, 195, 46, 6);
    ctx.fill();
    ctx.fillStyle = "#aaddff";
    ctx.font = "11px Arial";
    ctx.textAlign = "left";
    ctx.fillText("P1: A/D move  W jump", 12, CANVAS_HEIGHT - 32);
    ctx.fillText("P2: ←/→ move  ↑ jump", 12, CANVAS_HEIGHT - 16);
  } else {
    ctx.fillStyle = "rgba(0,0,0,0.5)";
    ctx.beginPath();
    ctx.roundRect(5, CANVAS_HEIGHT - 34, 195, 30, 6);
    ctx.fill();
    ctx.fillStyle = "#aaddff";
    ctx.font = "11px Arial";
    ctx.textAlign = "left";
    ctx.fillText("P1: A/D move  W jump", 12, CANVAS_HEIGHT - 14);
  }
}

export default function VolleyballGame() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [gameMode, setGameMode] = useState<GameMode>("menu");
  const [winner, setWinner] = useState<string>("");
  const stateRef = useRef({
    player1: null as Player | null,
    player2: null as Player | null,
    ball: null as Ball | null,
    keys: {} as Keys,
    animFrame: 0,
    time: 0,
    message: "",
    messageTimer: 0,
    serving: true,
    serveTimer: 60,
  });

  const initGame = useCallback((mode: GameMode) => {
    const s = stateRef.current;
    s.player1 = {
      x: 160,
      y: GROUND_Y - PLAYER_HEIGHT / 2,
      vy: 0,
      onGround: true,
      score: 0,
      color: "blue",
      name: "Player 1",
      side: "left",
    };
    s.player2 = {
      x: CANVAS_WIDTH - 160,
      y: GROUND_Y - PLAYER_HEIGHT / 2,
      vy: 0,
      onGround: true,
      score: 0,
      color: "red",
      name: mode === "vs-computer" ? "CPU" : "Player 2",
      side: "right",
    };
    s.ball = {
      x: 160,
      y: GROUND_Y - PLAYER_HEIGHT - BALL_RADIUS - 5,
      vx: 3,
      vy: -2,
    };
    s.serving = true;
    s.serveTimer = 90;
    s.message = "Game Start!";
    s.messageTimer = 90;
    s.time = 0;
  }, []);

  const handlePoint = useCallback(
    (scorer: "left" | "right", mode: GameMode) => {
      const s = stateRef.current;
      if (!s.player1 || !s.player2) return;

      if (scorer === "left") {
        s.player1.score++;
        s.message = "Player 1 scores!";
      } else {
        s.player2.score++;
        s.message = mode === "vs-computer" ? "CPU scores!" : "Player 2 scores!";
      }
      s.messageTimer = 120;

      if (s.player1.score >= WINNING_SCORE || s.player2.score >= WINNING_SCORE) {
        const winnerName =
          s.player1.score >= WINNING_SCORE
            ? "Player 1"
            : mode === "vs-computer"
            ? "CPU"
            : "Player 2";
        s.message = `${winnerName} Wins!`;
        s.messageTimer = 9999;
        return winnerName;
      }

      s.player1.x = 160;
      s.player1.y = GROUND_Y - PLAYER_HEIGHT / 2;
      s.player1.vy = 0;
      s.player2.x = CANVAS_WIDTH - 160;
      s.player2.y = GROUND_Y - PLAYER_HEIGHT / 2;
      s.player2.vy = 0;

      if (scorer === "left") {
        s.ball = {
          x: 160,
          y: GROUND_Y - PLAYER_HEIGHT - BALL_RADIUS - 5,
          vx: 3,
          vy: -3,
        };
      } else {
        s.ball = {
          x: CANVAS_WIDTH - 160,
          y: GROUND_Y - PLAYER_HEIGHT - BALL_RADIUS - 5,
          vx: -3,
          vy: -3,
        };
      }
      s.serving = true;
      s.serveTimer = 60;
      return null;
    },
    []
  );

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      stateRef.current.keys[e.key] = true;
      if (
        ["ArrowUp", "ArrowDown", "ArrowLeft", "ArrowRight", " "].includes(e.key)
      ) {
        e.preventDefault();
      }
    };
    const handleKeyUp = (e: KeyboardEvent) => {
      stateRef.current.keys[e.key] = false;
    };
    window.addEventListener("keydown", handleKeyDown);
    window.addEventListener("keyup", handleKeyUp);
    return () => {
      window.removeEventListener("keydown", handleKeyDown);
      window.removeEventListener("keyup", handleKeyUp);
    };
  }, []);

  useEffect(() => {
    if (gameMode !== "2player" && gameMode !== "vs-computer") return;
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    initGame(gameMode);

    const gameLoop = () => {
      const s = stateRef.current;
      if (!s.player1 || !s.player2 || !s.ball) {
        s.animFrame = requestAnimationFrame(gameLoop);
        return;
      }
      s.time++;

      if (s.messageTimer > 0) s.messageTimer--;
      const currentMessage = s.messageTimer > 0 ? s.message : "";

      if (s.serving) {
        s.serveTimer--;
        if (s.serveTimer <= 0) s.serving = false;
      }

      const p1 = s.player1;
      const p2 = s.player2;
      const ball = s.ball;
      const keys = s.keys;

      if (!s.serving || s.serveTimer < 30) {
        if (keys["a"] || keys["A"]) {
          p1.x = Math.max(PLAYER_WIDTH / 2 + 5, p1.x - PLAYER_SPEED);
        }
        if (keys["d"] || keys["D"]) {
          p1.x = Math.min(NET_X - PLAYER_WIDTH / 2 - 5, p1.x + PLAYER_SPEED);
        }
        if ((keys["w"] || keys["W"]) && p1.onGround) {
          p1.vy = JUMP_FORCE;
          p1.onGround = false;
        }

        if (gameMode === "2player") {
          if (keys["ArrowLeft"]) {
            p2.x = Math.max(NET_X + PLAYER_WIDTH / 2 + 5, p2.x - PLAYER_SPEED);
          }
          if (keys["ArrowRight"]) {
            p2.x = Math.min(CANVAS_WIDTH - PLAYER_WIDTH / 2 - 5, p2.x + PLAYER_SPEED);
          }
          if (keys["ArrowUp"] && p2.onGround) {
            p2.vy = JUMP_FORCE;
            p2.onGround = false;
          }
        } else {
          const cpuTarget = ball.x;
          const cpuDiff = cpuTarget - p2.x;
          const cpuSpeed = Math.min(PLAYER_SPEED * 0.85, Math.abs(cpuDiff));
          if (Math.abs(cpuDiff) > 15) {
            p2.x += Math.sign(cpuDiff) * cpuSpeed;
          }
          p2.x = Math.max(NET_X + PLAYER_WIDTH / 2 + 5, Math.min(CANVAS_WIDTH - PLAYER_WIDTH / 2 - 5, p2.x));

          const ballAbove = ball.y < p2.y - 20;
          const ballClose = Math.abs(ball.x - p2.x) < 60;
          if (ballAbove && ballClose && p2.onGround && ball.vy < 0) {
            p2.vy = JUMP_FORCE;
            p2.onGround = false;
          }
        }
      }

      p1.vy += GRAVITY;
      p1.y += p1.vy;
      if (p1.y >= GROUND_Y - PLAYER_HEIGHT / 2) {
        p1.y = GROUND_Y - PLAYER_HEIGHT / 2;
        p1.vy = 0;
        p1.onGround = true;
      }

      p2.vy += GRAVITY;
      p2.y += p2.vy;
      if (p2.y >= GROUND_Y - PLAYER_HEIGHT / 2) {
        p2.y = GROUND_Y - PLAYER_HEIGHT / 2;
        p2.vy = 0;
        p2.onGround = true;
      }

      if (!s.serving) {
        ball.vy += GRAVITY;
        ball.x += ball.vx;
        ball.y += ball.vy;

        if (ball.x - BALL_RADIUS < 0) {
          ball.x = BALL_RADIUS;
          ball.vx = Math.abs(ball.vx);
        }
        if (ball.x + BALL_RADIUS > CANVAS_WIDTH) {
          ball.x = CANVAS_WIDTH - BALL_RADIUS;
          ball.vx = -Math.abs(ball.vx);
        }

        if (ball.y - BALL_RADIUS < 0) {
          ball.y = BALL_RADIUS;
          ball.vy = Math.abs(ball.vy);
        }

        const netBuffer = 5;
        if (
          ball.x + BALL_RADIUS > NET_X - netBuffer &&
          ball.x - BALL_RADIUS < NET_X + netBuffer &&
          ball.y + BALL_RADIUS > NET_TOP &&
          ball.y < GROUND_Y
        ) {
          if (ball.vx > 0) {
            ball.x = NET_X - netBuffer - BALL_RADIUS;
            ball.vx = -Math.abs(ball.vx) * 0.8;
          } else {
            ball.x = NET_X + netBuffer + BALL_RADIUS;
            ball.vx = Math.abs(ball.vx) * 0.8;
          }
          ball.vy *= 0.9;
        }

        const checkCollision = (player: Player) => {
          const px = player.x;
          const py = player.y - PLAYER_HEIGHT / 2;
          const nearX = Math.abs(ball.x - px) < PLAYER_WIDTH / 2 + BALL_RADIUS;
          const nearY = Math.abs(ball.y - (py + PLAYER_HEIGHT / 2)) < PLAYER_HEIGHT / 2 + BALL_RADIUS;

          if (nearX && nearY) {
            const hitDir = player.side === "left" ? 1 : -1;
            const relX = (ball.x - px) / (PLAYER_WIDTH / 2);
            ball.vx = hitDir * (3 + Math.abs(relX) * 4);
            ball.vy = -10 - Math.abs(player.vy) * 0.3;
            ball.y = py - BALL_RADIUS - 2;
          }
        };

        checkCollision(p1);
        checkCollision(p2);

        if (ball.y + BALL_RADIUS > GROUND_Y) {
          if (ball.x < NET_X) {
            const win = handlePoint("right", gameMode);
            if (win) {
              setWinner(win);
              setGameMode("gameover");
              return;
            }
          } else {
            const win = handlePoint("left", gameMode);
            if (win) {
              setWinner(win);
              setGameMode("gameover");
              return;
            }
          }
        }
      } else {
        if (ball.x < NET_X) {
          ball.x = p1.x;
          ball.y = p1.y - PLAYER_HEIGHT / 2 - BALL_RADIUS - 5;
        } else {
          ball.x = p2.x;
          ball.y = p2.y - PLAYER_HEIGHT / 2 - BALL_RADIUS - 5;
        }
      }

      ctx.clearRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
      drawPyramids(ctx);
      drawNet(ctx);
      drawPlayer(ctx, p1, s.time);
      drawPlayer(ctx, p2, s.time);
      drawBall(ctx, ball, s.time);
      drawUI(ctx, p1, p2, currentMessage, gameMode);

      s.animFrame = requestAnimationFrame(gameLoop);
    };

    const s = stateRef.current;
    s.animFrame = requestAnimationFrame(gameLoop);
    return () => {
      cancelAnimationFrame(s.animFrame);
    };
  }, [gameMode, initGame, handlePoint]);

  if (gameMode === "menu") {
    return (
      <div
        style={{
          width: "100vw",
          height: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "linear-gradient(180deg, #1a3a5c 0%, #4a90d9 40%, #f4e4b8 100%)",
          fontFamily: "Arial, sans-serif",
        }}
      >
        <div
          style={{
            background: "rgba(0,0,0,0.7)",
            borderRadius: 20,
            padding: "40px 60px",
            textAlign: "center",
            color: "#fff",
            boxShadow: "0 8px 32px rgba(0,0,0,0.5)",
          }}
        >
          <h1
            style={{
              fontSize: 42,
              fontWeight: "bold",
              color: "#FFD700",
              textShadow: "2px 2px 8px rgba(0,0,0,0.5)",
              marginBottom: 8,
            }}
          >
            Volleyball
          </h1>
          <p
            style={{
              fontSize: 18,
              color: "#f4e4b8",
              marginBottom: 32,
              fontStyle: "italic",
            }}
          >
            at the Giza Pyramids
          </p>

          <div style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <button
              onClick={() => setGameMode("2player")}
              style={{
                padding: "14px 40px",
                fontSize: 20,
                fontWeight: "bold",
                background: "linear-gradient(135deg, #1565c0, #42a5f5)",
                color: "#fff",
                border: "none",
                borderRadius: 12,
                cursor: "pointer",
                boxShadow: "0 4px 12px rgba(0,0,0,0.3)",
              }}
            >
              2 Players
            </button>
            <button
              onClick={() => setGameMode("vs-computer")}
              style={{
                padding: "14px 40px",
                fontSize: 20,
                fontWeight: "bold",
                background: "linear-gradient(135deg, #c62828, #ef5350)",
                color: "#fff",
                border: "none",
                borderRadius: 12,
                cursor: "pointer",
                boxShadow: "0 4px 12px rgba(0,0,0,0.3)",
              }}
            >
              vs Computer
            </button>
          </div>

          <div
            style={{
              marginTop: 32,
              fontSize: 13,
              color: "rgba(255,255,255,0.6)",
              lineHeight: 1.8,
            }}
          >
            <p>
              <strong style={{ color: "#aaddff" }}>Player 1:</strong> A/D to
              move, W to jump
            </p>
            <p>
              <strong style={{ color: "#ffaaaa" }}>Player 2:</strong> ←/→ to
              move, ↑ to jump
            </p>
            <p style={{ marginTop: 8 }}>
              First to {WINNING_SCORE} points wins!
            </p>
          </div>
        </div>
      </div>
    );
  }

  if (gameMode === "gameover") {
    return (
      <div
        style={{
          width: "100vw",
          height: "100vh",
          display: "flex",
          alignItems: "center",
          justifyContent: "center",
          background: "linear-gradient(180deg, #1a3a5c 0%, #4a90d9 40%, #f4e4b8 100%)",
          fontFamily: "Arial, sans-serif",
        }}
      >
        <div
          style={{
            background: "rgba(0,0,0,0.8)",
            borderRadius: 20,
            padding: "40px 60px",
            textAlign: "center",
            color: "#fff",
            boxShadow: "0 8px 32px rgba(0,0,0,0.5)",
          }}
        >
          <h1
            style={{
              fontSize: 48,
              fontWeight: "bold",
              color: "#FFD700",
              textShadow: "2px 2px 8px rgba(0,0,0,0.5)",
              marginBottom: 8,
            }}
          >
            {winner} Wins!
          </h1>
          <p style={{ fontSize: 22, color: "#f4e4b8", marginBottom: 32 }}>
            Game Over
          </p>
          <div style={{ display: "flex", gap: 16, justifyContent: "center" }}>
            <button
              onClick={() => {
                setWinner("");
                setGameMode("menu");
              }}
              style={{
                padding: "14px 32px",
                fontSize: 18,
                fontWeight: "bold",
                background: "linear-gradient(135deg, #2e7d32, #66bb6a)",
                color: "#fff",
                border: "none",
                borderRadius: 12,
                cursor: "pointer",
              }}
            >
              Main Menu
            </button>
            <button
              onClick={() => {
                setWinner("");
                const mode = gameMode;
                setGameMode("menu");
                setTimeout(() => setGameMode(mode as "2player" | "vs-computer"), 50);
              }}
              style={{
                padding: "14px 32px",
                fontSize: 18,
                fontWeight: "bold",
                background: "linear-gradient(135deg, #1565c0, #42a5f5)",
                color: "#fff",
                border: "none",
                borderRadius: 12,
                cursor: "pointer",
              }}
            >
              Play Again
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div
      style={{
        width: "100vw",
        height: "100vh",
        display: "flex",
        flexDirection: "column",
        alignItems: "center",
        justifyContent: "center",
        background: "#1a1a2e",
      }}
    >
      <div style={{ position: "relative" }}>
        <canvas
          ref={canvasRef}
          width={CANVAS_WIDTH}
          height={CANVAS_HEIGHT}
          style={{
            display: "block",
            borderRadius: 12,
            boxShadow: "0 8px 32px rgba(0,0,0,0.6)",
          }}
        />
        <button
          onClick={() => {
            cancelAnimationFrame(stateRef.current.animFrame);
            setGameMode("menu");
          }}
          style={{
            position: "absolute",
            top: 12,
            right: 12,
            padding: "6px 14px",
            fontSize: 13,
            background: "rgba(0,0,0,0.5)",
            color: "#fff",
            border: "1px solid rgba(255,255,255,0.3)",
            borderRadius: 8,
            cursor: "pointer",
          }}
        >
          Menu
        </button>
      </div>
    </div>
  );
}
