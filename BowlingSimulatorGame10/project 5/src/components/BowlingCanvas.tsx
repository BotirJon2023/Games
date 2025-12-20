import React, { useRef, useEffect } from 'react';
import { GameState } from '../types/bowling';

interface BowlingCanvasProps {
  gameState: GameState;
  onMouseDown: () => void;
  onMouseUp: () => void;
  onMouseMove: (clientX: number, canvasWidth: number) => void;
  isAiming: boolean;
  power: number;
  angle: number;
}

const LANE_WIDTH = 200;
const LANE_HEIGHT = 500;
const LANE_X = 100;
const LANE_Y = 50;

export const BowlingCanvas: React.FC<BowlingCanvasProps> = ({
  gameState,
  onMouseDown,
  onMouseUp,
  onMouseMove,
  isAiming,
  power,
  angle,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const handleMouseDown = () => onMouseDown();
    const handleMouseUp = () => onMouseUp();
    const handleMouseMove = (e: MouseEvent) =>
      onMouseMove(e.clientX - rect.left, canvas.width);

    canvas.addEventListener('mousedown', handleMouseDown);
    canvas.addEventListener('mouseup', handleMouseUp);
    canvas.addEventListener('mousemove', handleMouseMove);

    return () => {
      canvas.removeEventListener('mousedown', handleMouseDown);
      canvas.removeEventListener('mouseup', handleMouseUp);
      canvas.removeEventListener('mousemove', handleMouseMove);
    };
  }, [onMouseDown, onMouseUp, onMouseMove]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    drawLane(ctx);
    drawPins(ctx, gameState.pins);
    drawBall(ctx, gameState.ball);

    if (isAiming) {
      drawAimingGuide(ctx, angle);
      drawPowerMeter(ctx, power);
    }

    drawInstructions(ctx, gameState.gameOver, gameState.isThrowInProgress);
  }, [gameState, isAiming, power, angle]);

  return (
    <canvas
      ref={canvasRef}
      width={900}
      height={600}
      className="border-4 border-gray-800 rounded-lg bg-gradient-to-b from-amber-700 to-yellow-900 cursor-crosshair"
    />
  );
};

function drawLane(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = '#E8D5C4';
  ctx.fillRect(LANE_X, LANE_Y, LANE_WIDTH, LANE_HEIGHT);

  ctx.strokeStyle = '#D4AF86';
  ctx.lineWidth = 2;
  for (let i = 0; i < LANE_HEIGHT; i += 40) {
    ctx.beginPath();
    ctx.moveTo(LANE_X, LANE_Y + i);
    ctx.lineTo(LANE_X + LANE_WIDTH, LANE_Y + i);
    ctx.stroke();
  }

  ctx.strokeStyle = '#8B5A2B';
  ctx.lineWidth = 3;
  ctx.strokeRect(LANE_X, LANE_Y, LANE_WIDTH, LANE_HEIGHT);

  ctx.strokeStyle = '#A0826D';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(LANE_X + LANE_WIDTH / 2, LANE_Y);
  ctx.lineTo(LANE_X + LANE_WIDTH / 2, LANE_Y + LANE_HEIGHT);
  ctx.stroke();

  const centerX = LANE_X + LANE_WIDTH / 2;
  ctx.fillStyle = '#8B5A2B';
  for (let i = 0; i < 12; i++) {
    const y = LANE_Y + LANE_HEIGHT - 100 - i * 30;
    ctx.beginPath();
    ctx.arc(centerX, y, 2, 0, Math.PI * 2);
    ctx.fill();
  }
}

function drawPins(ctx: CanvasRenderingContext2D, pins: any[]) {
  pins.forEach((pin) => {
    ctx.save();
    ctx.translate(pin.x, pin.y);
    ctx.rotate(pin.rotation);

    if (!pin.fallen) {
      ctx.fillStyle = '#FFFFFF';
      ctx.fillRect(-6, -12, 12, 24);

      ctx.fillStyle = '#FF0000';
      ctx.fillRect(-6, -8, 12, 4);

      ctx.strokeStyle = '#000000';
      ctx.lineWidth = 0.5;
      ctx.strokeRect(-6, -12, 12, 24);
    } else {
      ctx.fillStyle = '#CCCCCC';
      ctx.fillRect(-6, -12, 12, 24);

      ctx.fillStyle = '#FF6666';
      ctx.fillRect(-6, -8, 12, 4);

      ctx.strokeStyle = '#333333';
      ctx.lineWidth = 0.5;
      ctx.strokeRect(-6, -12, 12, 24);
    }

    ctx.restore();
  });
}

function drawBall(ctx: CanvasRenderingContext2D, ball: any) {
  const gradient = ctx.createRadialGradient(
    ball.x - 4,
    ball.y - 4,
    0,
    ball.x,
    ball.y,
    ball.radius
  );
  gradient.addColorStop(0, '#4D9FFF');
  gradient.addColorStop(1, '#0066CC');

  ctx.fillStyle = gradient;
  ctx.beginPath();
  ctx.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2);
  ctx.fill();

  ctx.strokeStyle = '#003D99';
  ctx.lineWidth = 1.5;
  ctx.stroke();

  ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
  ctx.beginPath();
  ctx.arc(ball.x - 4, ball.y - 4, 4, 0, Math.PI * 2);
  ctx.fill();

  ctx.save();
  ctx.translate(ball.x, ball.y);
  ctx.rotate(ball.rotation);
  ctx.strokeStyle = 'rgba(0, 0, 0, 0.2)';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.arc(0, 0, ball.radius, 0, Math.PI * 2);
  ctx.stroke();
  ctx.restore();
}

function drawAimingGuide(ctx: CanvasRenderingContext2D, angle: number) {
  const startX = LANE_X + LANE_WIDTH / 2;
  const startY = LANE_Y + LANE_HEIGHT - 50;
  const endX = startX + angle * 10;
  const endY = LANE_Y + 100;

  ctx.strokeStyle = 'rgba(255, 255, 0, 0.6)';
  ctx.lineWidth = 2;
  ctx.setLineDash([5, 5]);
  ctx.beginPath();
  ctx.moveTo(startX, startY);
  ctx.lineTo(endX, endY);
  ctx.stroke();
  ctx.setLineDash([]);

  ctx.strokeStyle = 'rgba(255, 255, 0, 0.8)';
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.arc(startX, startY, 20, 0, Math.PI * 2);
  ctx.stroke();
}

function drawPowerMeter(ctx: CanvasRenderingContext2D, power: number) {
  const meterX = 30;
  const meterY = 150;
  const meterWidth = 35;
  const meterHeight = 200;

  ctx.fillStyle = 'rgba(40, 40, 40, 0.8)';
  ctx.fillRect(meterX, meterY, meterWidth, meterHeight);

  const powerHeight = (power / 100) * meterHeight;
  const hue = power > 70 ? 0 : power > 40 ? 45 : 120;
  ctx.fillStyle = `hsl(${hue}, 100%, 50%)`;
  ctx.fillRect(
    meterX,
    meterY + meterHeight - powerHeight,
    meterWidth,
    powerHeight
  );

  ctx.strokeStyle = 'rgba(255, 255, 255, 0.8)';
  ctx.lineWidth = 2;
  ctx.strokeRect(meterX, meterY, meterWidth, meterHeight);

  ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
  ctx.font = 'bold 12px Arial';
  ctx.textAlign = 'center';
  ctx.fillText('POWER', meterX + meterWidth / 2, meterY - 10);
  ctx.fillText(`${power}%`, meterX + meterWidth / 2, meterY + meterHeight + 20);
}

function drawInstructions(
  ctx: CanvasRenderingContext2D,
  gameOver: boolean,
  isThrowInProgress: boolean
) {
  ctx.fillStyle = 'rgba(255, 255, 255, 0.95)';
  ctx.font = 'bold 14px Arial';
  ctx.textAlign = 'center';

  if (gameOver) {
    ctx.font = 'bold 32px Arial';
    ctx.fillStyle = '#FFD700';
    ctx.fillText('GAME OVER!', 450, 300);

    ctx.font = 'bold 20px Arial';
    ctx.fillStyle = '#FFFFFF';
    ctx.fillText('Press "New Game" to play again', 450, 340);
  } else if (!isThrowInProgress) {
    ctx.font = 'bold 14px Arial';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
    ctx.fillText(
      'Click and hold to aim, move mouse to adjust angle, release to throw',
      450,
      25
    );
  } else {
    ctx.font = 'bold 16px Arial';
    ctx.fillStyle = 'rgba(255, 255, 0, 0.9)';
    ctx.fillText('Throwing in progress...', 450, 25);
  }
}
