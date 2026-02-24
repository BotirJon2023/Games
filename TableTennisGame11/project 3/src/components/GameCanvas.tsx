import React, { useRef, useEffect } from 'react';
import { GameData } from '../types/game';

interface GameCanvasProps {
  gameData: GameData;
  onMouseMove: (y: number) => void;
}

const CANVAS_WIDTH = 800;
const CANVAS_HEIGHT = 600;

export const GameCanvas: React.FC<GameCanvasProps> = ({ gameData, onMouseMove }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const handleMouseMove = (e: MouseEvent) => {
      const rect = canvas.getBoundingClientRect();
      const y = e.clientY - rect.top;
      onMouseMove(y);
    };

    canvas.addEventListener('mousemove', handleMouseMove);
    return () => canvas.removeEventListener('mousemove', handleMouseMove);
  }, [onMouseMove]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear with gradient background
    const gradient = ctx.createLinearGradient(0, 0, 0, CANVAS_HEIGHT);
    gradient.addColorStop(0, '#1a1a2e');
    gradient.addColorStop(1, '#16213e');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

    // Center line (animated dashes)
    ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
    ctx.setLineDash([5, 10]);
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(CANVAS_WIDTH / 2, 0);
    ctx.lineTo(CANVAS_WIDTH / 2, CANVAS_HEIGHT);
    ctx.stroke();
    ctx.setLineDash([]);

    // Player paddle (left)
    drawPaddle(ctx, gameData.playerPaddle, '#00D9FF');

    // Computer paddle (right)
    drawPaddle(ctx, gameData.computerPaddle, '#FF006E');

    // Ball
    drawBall(ctx, gameData.ball);

    // Particles
    gameData.particles.forEach((particle) => {
      drawParticle(ctx, particle);
    });

    // Net effect on edges
    ctx.strokeStyle = 'rgba(100, 200, 255, 0.2)';
    ctx.lineWidth = 2;
    ctx.strokeRect(1, 1, CANVAS_WIDTH - 2, CANVAS_HEIGHT - 2);
  }, [gameData]);

  return (
    <canvas
      ref={canvasRef}
      width={CANVAS_WIDTH}
      height={CANVAS_HEIGHT}
      className="border-4 border-gray-800 rounded-lg shadow-2xl"
    />
  );
};

const drawPaddle = (ctx: CanvasRenderingContext2D, paddle: any, color: string) => {
  // Glow effect
  ctx.shadowColor = color;
  ctx.shadowBlur = 15;

  // Main paddle
  const gradient = ctx.createLinearGradient(paddle.x, paddle.y, paddle.x + paddle.width, paddle.y);
  gradient.addColorStop(0, color);
  gradient.addColorStop(0.5, 'rgba(255, 255, 255, 0.5)');
  gradient.addColorStop(1, color);

  ctx.fillStyle = gradient;
  ctx.fillRect(paddle.x, paddle.y, paddle.width, paddle.height);

  // Border
  ctx.strokeStyle = color;
  ctx.lineWidth = 2;
  ctx.strokeRect(paddle.x, paddle.y, paddle.width, paddle.height);

  ctx.shadowBlur = 0;
};

const drawBall = (ctx: CanvasRenderingContext2D, ball: any) => {
  // Glow
  ctx.shadowColor = '#FFD700';
  ctx.shadowBlur = 20;

  // Main ball with gradient
  const ballGradient = ctx.createRadialGradient(ball.x - 2, ball.y - 2, 0, ball.x, ball.y, ball.radius);
  ballGradient.addColorStop(0, '#FFFF00');
  ballGradient.addColorStop(0.7, '#FFD700');
  ballGradient.addColorStop(1, '#FF8C00');

  ctx.fillStyle = ballGradient;
  ctx.beginPath();
  ctx.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2);
  ctx.fill();

  // Shine effect
  ctx.fillStyle = 'rgba(255, 255, 255, 0.6)';
  ctx.beginPath();
  ctx.arc(ball.x - 2, ball.y - 2, ball.radius * 0.4, 0, Math.PI * 2);
  ctx.fill();

  ctx.shadowBlur = 0;

  // Trail line
  ctx.strokeStyle = 'rgba(255, 215, 0, 0.3)';
  ctx.lineWidth = 1;
  ctx.setLineDash([3, 3]);
  ctx.beginPath();
  ctx.moveTo(ball.x, ball.y);
  ctx.lineTo(
    ball.x - ball.velocityX * 30,
    ball.y - ball.velocityY * 30
  );
  ctx.stroke();
  ctx.setLineDash([]);
};

const drawParticle = (ctx: CanvasRenderingContext2D, particle: any) => {
  const alpha = particle.life;
  ctx.globalAlpha = alpha;

  ctx.fillStyle = particle.color;
  ctx.shadowColor = particle.color;
  ctx.shadowBlur = 8;

  ctx.beginPath();
  ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2);
  ctx.fill();

  ctx.globalAlpha = 1;
  ctx.shadowBlur = 0;
};
