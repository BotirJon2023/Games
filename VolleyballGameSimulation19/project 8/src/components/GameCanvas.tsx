import { useEffect, useRef } from 'react';
import { GameState } from '../types/game';
import { COURT_WIDTH, COURT_HEIGHT, GROUND_Y, NET_X, NET_HEIGHT } from '../utils/physics';

interface GameCanvasProps {
  gameState: GameState;
}

export default function GameCanvas({ gameState }: GameCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const render = () => {
      ctx.clearRect(0, 0, COURT_WIDTH, COURT_HEIGHT);

      drawBackground(ctx);
      drawCourt(ctx);
      drawNet(ctx);
      drawPlayer(ctx, gameState.player1, '#FF6B6B', 'P1');
      drawPlayer(ctx, gameState.player2, '#4ECDC4', gameState.playerMode === '1player' ? 'CPU' : 'P2');
      drawBall(ctx, gameState.ball);
      drawScores(ctx, gameState);
    };

    render();
  }, [gameState]);

  return (
    <canvas
      ref={canvasRef}
      width={COURT_WIDTH}
      height={COURT_HEIGHT}
      className="border-4 border-amber-600 rounded-lg shadow-2xl"
    />
  );
}

function drawBackground(ctx: CanvasRenderingContext2D) {
  const gradient = ctx.createLinearGradient(0, 0, 0, COURT_HEIGHT);
  gradient.addColorStop(0, '#87CEEB');
  gradient.addColorStop(1, '#E0F6FF');
  ctx.fillStyle = gradient;
  ctx.fillRect(0, 0, COURT_WIDTH, COURT_HEIGHT);

  ctx.fillStyle = '#FFD700';
  ctx.beginPath();
  ctx.arc(700, 80, 40, 0, Math.PI * 2);
  ctx.fill();

  for (let i = 0; i < 5; i++) {
    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.beginPath();
    ctx.arc(100 + i * 150, 60 + Math.sin(i) * 20, 30 + i * 5, 0, Math.PI * 2);
    ctx.fill();
  }
}

function drawCourt(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = '#F4A460';
  ctx.fillRect(0, GROUND_Y, COURT_WIDTH, COURT_HEIGHT - GROUND_Y);

  ctx.strokeStyle = '#fff';
  ctx.lineWidth = 2;
  ctx.setLineDash([5, 5]);
  ctx.beginPath();
  ctx.moveTo(0, GROUND_Y);
  ctx.lineTo(COURT_WIDTH, GROUND_Y);
  ctx.stroke();
  ctx.setLineDash([]);

  for (let i = 0; i < 10; i++) {
    ctx.fillStyle = `rgba(139, 69, 19, ${0.1 + Math.random() * 0.1})`;
    ctx.beginPath();
    ctx.arc(
      Math.random() * COURT_WIDTH,
      GROUND_Y + Math.random() * 30,
      2 + Math.random() * 3,
      0,
      Math.PI * 2
    );
    ctx.fill();
  }
}

function drawNet(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = '#8B4513';
  ctx.fillRect(NET_X - 3, GROUND_Y - NET_HEIGHT, 6, NET_HEIGHT);

  ctx.strokeStyle = '#fff';
  ctx.lineWidth = 2;
  for (let i = 0; i < 10; i++) {
    ctx.beginPath();
    ctx.moveTo(NET_X - 20, GROUND_Y - NET_HEIGHT + i * 15);
    ctx.lineTo(NET_X + 20, GROUND_Y - NET_HEIGHT + i * 15);
    ctx.stroke();
  }

  for (let i = 0; i < 3; i++) {
    ctx.beginPath();
    ctx.moveTo(NET_X - 20 + i * 20, GROUND_Y - NET_HEIGHT);
    ctx.lineTo(NET_X - 20 + i * 20, GROUND_Y);
    ctx.stroke();
  }

  ctx.fillStyle = '#FF0000';
  ctx.fillRect(NET_X - 25, GROUND_Y - NET_HEIGHT - 10, 50, 10);
}

function drawPlayer(ctx: CanvasRenderingContext2D, player: any, color: string, label: string) {
  const x = player.position.x;
  const y = player.position.y;

  ctx.save();

  const bodyColor = color;
  const skinColor = '#FFD1A1';

  ctx.fillStyle = bodyColor;
  ctx.fillRect(x + 10, y + 20, 20, 25);

  ctx.fillStyle = skinColor;
  ctx.beginPath();
  ctx.arc(x + 20, y + 10, 10, 0, Math.PI * 2);
  ctx.fill();

  ctx.fillStyle = '#000';
  ctx.beginPath();
  ctx.arc(x + 17, y + 8, 2, 0, Math.PI * 2);
  ctx.fill();
  ctx.beginPath();
  ctx.arc(x + 23, y + 8, 2, 0, Math.PI * 2);
  ctx.fill();

  ctx.strokeStyle = bodyColor;
  ctx.lineWidth = 4;
  ctx.lineCap = 'round';

  if (player.isJumping) {
    ctx.beginPath();
    ctx.moveTo(x + 10, y + 25);
    ctx.lineTo(x, y + 15);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(x + 30, y + 25);
    ctx.lineTo(x + 40, y + 15);
    ctx.stroke();
  } else {
    ctx.beginPath();
    ctx.moveTo(x + 10, y + 25);
    ctx.lineTo(x + 5, y + 40);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(x + 30, y + 25);
    ctx.lineTo(x + 35, y + 40);
    ctx.stroke();
  }

  ctx.beginPath();
  ctx.moveTo(x + 10, y + 45);
  ctx.lineTo(x + 5, y + 60);
  ctx.stroke();

  ctx.beginPath();
  ctx.moveTo(x + 30, y + 45);
  ctx.lineTo(x + 35, y + 60);
  ctx.stroke();

  ctx.fillStyle = '#000';
  ctx.font = 'bold 12px Arial';
  ctx.textAlign = 'center';
  ctx.fillText(label, x + 20, y - 5);

  ctx.restore();
}

function drawBall(ctx: CanvasRenderingContext2D, ball: any) {
  const gradient = ctx.createRadialGradient(
    ball.position.x - 5,
    ball.position.y - 5,
    0,
    ball.position.x,
    ball.position.y,
    ball.radius
  );
  gradient.addColorStop(0, '#FFE135');
  gradient.addColorStop(0.7, '#FFC700');
  gradient.addColorStop(1, '#FF9500');

  ctx.fillStyle = gradient;
  ctx.beginPath();
  ctx.arc(ball.position.x, ball.position.y, ball.radius, 0, Math.PI * 2);
  ctx.fill();

  ctx.strokeStyle = '#FF9500';
  ctx.lineWidth = 2;
  ctx.beginPath();
  ctx.arc(ball.position.x, ball.position.y, ball.radius - 2, 0, Math.PI * 2);
  ctx.stroke();

  ctx.strokeStyle = 'rgba(255, 149, 0, 0.6)';
  ctx.lineWidth = 1;
  for (let i = 0; i < 3; i++) {
    const angle = (ball.spinning + i * 120) * (Math.PI / 180);
    ctx.beginPath();
    ctx.moveTo(
      ball.position.x + Math.cos(angle) * ball.radius,
      ball.position.y + Math.sin(angle) * ball.radius
    );
    ctx.lineTo(
      ball.position.x - Math.cos(angle) * ball.radius,
      ball.position.y - Math.sin(angle) * ball.radius
    );
    ctx.stroke();
  }
}

function drawScores(ctx: CanvasRenderingContext2D, gameState: GameState) {
  ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
  ctx.fillRect(COURT_WIDTH / 2 - 100, 10, 200, 50);

  ctx.fillStyle = '#FF6B6B';
  ctx.font = 'bold 32px Arial';
  ctx.textAlign = 'center';
  ctx.fillText(gameState.player1.score.toString(), COURT_WIDTH / 2 - 40, 45);

  ctx.fillStyle = '#fff';
  ctx.fillText('-', COURT_WIDTH / 2, 45);

  ctx.fillStyle = '#4ECDC4';
  ctx.fillText(gameState.player2.score.toString(), COURT_WIDTH / 2 + 40, 45);
}
