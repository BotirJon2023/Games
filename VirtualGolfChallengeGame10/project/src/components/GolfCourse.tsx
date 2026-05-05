import React, { useEffect, useRef } from 'react';
import { Course, Ball, Player, Particle } from '../types/golf';

interface GolfCourseProps {
  course: Course;
  balls: Map<string, Ball>;
  players: Player[];
  currentPlayer: Player;
  swingPower: number;
  swingAngle: number;
  particles: Particle[];
  onAngleChange: (angle: number) => void;
  onPowerChange: (power: number) => void;
  onSwing: (power: number, angle: number) => void;
  gamePhase: string;
}

const GolfCourse: React.FC<GolfCourseProps> = ({
  course,
  balls,
  players,
  currentPlayer,
  swingPower,
  swingAngle,
  particles,
  onAngleChange,
  onPowerChange,
  onSwing,
  gamePhase,
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animationRef = useRef<number | null>(null);
  const waveOffsetRef = useRef(0);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const drawBackground = () => {
      const gradient = ctx.createLinearGradient(0, 0, 0, 500);
      gradient.addColorStop(0, '#87CEEB');
      gradient.addColorStop(0.3, '#98D8E8');
      gradient.addColorStop(0.5, '#F0E68C');
      gradient.addColorStop(1, '#90EE90');
      ctx.fillStyle = gradient;
      ctx.fillRect(0, 0, 800, 500);

      ctx.fillStyle = '#FFD700';
      ctx.beginPath();
      ctx.arc(700, 60, 40, 0, Math.PI * 2);
      ctx.fill();

      const sunGlow = ctx.createRadialGradient(700, 60, 40, 700, 60, 80);
      sunGlow.addColorStop(0, 'rgba(255, 215, 0, 0.5)');
      sunGlow.addColorStop(1, 'rgba(255, 215, 0, 0)');
      ctx.fillStyle = sunGlow;
      ctx.beginPath();
      ctx.arc(700, 60, 80, 0, Math.PI * 2);
      ctx.fill();

      ctx.fillStyle = '#E0F7FA';
      for (let i = 0; i < 5; i++) {
        const x = 100 + i * 150;
        const y = 30 + Math.sin(waveOffsetRef.current + i) * 5;
        ctx.beginPath();
        ctx.ellipse(x, y, 60, 20, 0, 0, Math.PI * 2);
        ctx.fill();
      }

      for (let i = 0; i < 3; i++) {
        const waveY = 480 + Math.sin(waveOffsetRef.current * 2 + i) * 3;
        ctx.fillStyle = `rgba(0, 150, 200, ${0.3 + i * 0.1})`;
        ctx.beginPath();
        ctx.moveTo(0, waveY);
        for (let x = 0; x <= 800; x += 20) {
          const y = waveY + Math.sin(x * 0.02 + waveOffsetRef.current + i) * 5;
          ctx.lineTo(x, y);
        }
        ctx.lineTo(800, 500);
        ctx.lineTo(0, 500);
        ctx.closePath();
        ctx.fill();
      }

      ctx.fillStyle = '#F5DEB3';
      ctx.fillRect(0, 450, 800, 50);

      ctx.fillStyle = '#DEB887';
      for (let i = 0; i < 15; i++) {
        const x = (i * 60 + waveOffsetRef.current * 10) % 800;
        ctx.beginPath();
        ctx.ellipse(x, 470, 15, 5, 0, 0, Math.PI * 2);
        ctx.fill();
      }
    };

    const drawObstacles = () => {
      course.obstacles.forEach(obstacle => {
        if (obstacle.type === 'water') {
          const gradient = ctx.createLinearGradient(
            obstacle.x,
            obstacle.y,
            obstacle.x,
            obstacle.y + obstacle.height
          );
          gradient.addColorStop(0, '#00CED1');
          gradient.addColorStop(1, '#008B8B');
          ctx.fillStyle = gradient;
          ctx.fillRect(obstacle.x, obstacle.y, obstacle.width, obstacle.height);

          ctx.strokeStyle = 'rgba(255, 255, 255, 0.5)';
          ctx.lineWidth = 2;
          for (let i = 0; i < 3; i++) {
            const waveY = obstacle.y + obstacle.height * (0.25 + i * 0.25);
            ctx.beginPath();
            ctx.moveTo(obstacle.x, waveY);
            for (let x = obstacle.x; x <= obstacle.x + obstacle.width; x += 10) {
              const y = waveY + Math.sin(x * 0.1 + waveOffsetRef.current * 3) * 3;
              ctx.lineTo(x, y);
            }
            ctx.stroke();
          }
        } else if (obstacle.type === 'bunker') {
          const gradient = ctx.createRadialGradient(
            obstacle.x + obstacle.width / 2,
            obstacle.y + obstacle.height / 2,
            0,
            obstacle.x + obstacle.width / 2,
            obstacle.y + obstacle.height / 2,
            Math.max(obstacle.width, obstacle.height) / 2
          );
          gradient.addColorStop(0, '#F4A460');
          gradient.addColorStop(1, '#DEB887');
          ctx.fillStyle = gradient;
          ctx.beginPath();
          ctx.ellipse(
            obstacle.x + obstacle.width / 2,
            obstacle.y + obstacle.height / 2,
            obstacle.width / 2,
            obstacle.height / 2,
            0,
            0,
            Math.PI * 2
          );
          ctx.fill();
        } else if (obstacle.type === 'tree') {
          ctx.fillStyle = '#8B4513';
          ctx.fillRect(
            obstacle.x + obstacle.width / 2 - 5,
            obstacle.y + obstacle.height / 2,
            10,
            obstacle.height / 2
          );

          ctx.fillStyle = '#228B22';
          ctx.beginPath();
          ctx.moveTo(obstacle.x + obstacle.width / 2, obstacle.y);
          ctx.lineTo(obstacle.x, obstacle.y + obstacle.height / 2);
          ctx.lineTo(obstacle.x + obstacle.width, obstacle.y + obstacle.height / 2);
          ctx.closePath();
          ctx.fill();

          ctx.fillStyle = '#32CD32';
          ctx.beginPath();
          ctx.arc(
            obstacle.x + obstacle.width / 2 - 10,
            obstacle.y + obstacle.height / 3,
            12,
            0,
            Math.PI * 2
          );
          ctx.fill();
          ctx.beginPath();
          ctx.arc(
            obstacle.x + obstacle.width / 2 + 10,
            obstacle.y + obstacle.height / 3,
            12,
            0,
            Math.PI * 2
          );
          ctx.fill();
        } else if (obstacle.type === 'rock') {
          ctx.fillStyle = '#696969';
          ctx.beginPath();
          ctx.moveTo(obstacle.x, obstacle.y + obstacle.height);
          ctx.lineTo(obstacle.x + obstacle.width * 0.3, obstacle.y);
          ctx.lineTo(obstacle.x + obstacle.width * 0.7, obstacle.y + obstacle.height * 0.2);
          ctx.lineTo(obstacle.x + obstacle.width, obstacle.y + obstacle.height);
          ctx.closePath();
          ctx.fill();

          ctx.fillStyle = '#808080';
          ctx.beginPath();
          ctx.arc(
            obstacle.x + obstacle.width * 0.4,
            obstacle.y + obstacle.height * 0.5,
            8,
            0,
            Math.PI * 2
          );
          ctx.fill();
        }
      });
    };

    const drawHole = () => {
      ctx.fillStyle = '#1a1a1a';
      ctx.beginPath();
      ctx.arc(course.hole.x, course.hole.y, course.hole.radius, 0, Math.PI * 2);
      ctx.fill();

      ctx.strokeStyle = '#FFD700';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.arc(course.hole.x, course.hole.y, course.hole.radius + 5, 0, Math.PI * 2);
      ctx.stroke();

      ctx.fillStyle = '#FF4500';
      ctx.beginPath();
      ctx.moveTo(course.hole.x, course.hole.y - 40);
      ctx.lineTo(course.hole.x + 5, course.hole.y - 40);
      ctx.lineTo(course.hole.x + 5, course.hole.y - 10);
          ctx.lineTo(course.hole.x, course.hole.y - 10);
          ctx.closePath();
          ctx.fill();

          ctx.fillStyle = '#FFD700';
          ctx.beginPath();
          ctx.moveTo(course.hole.x + 5, course.hole.y - 40);
          ctx.lineTo(course.hole.x + 25, course.hole.y - 35);
          ctx.lineTo(course.hole.x + 5, course.hole.y - 30);
          ctx.closePath();
          ctx.fill();
        };

        const drawTee = () => {
          ctx.fillStyle = '#8B4513';
          ctx.fillRect(course.tee.x - 15, course.tee.y - 3, 30, 6);

          ctx.fillStyle = '#228B22';
          ctx.beginPath();
          ctx.ellipse(course.tee.x, course.tee.y, 20, 10, 0, 0, Math.PI * 2);
          ctx.fill();
        };

        const drawBalls = () => {
          players.forEach(player => {
            const ball = balls.get(player.id);
            if (!ball) return;

            if (ball.trail.length > 1) {
              ctx.strokeStyle = player.color;
              ctx.lineWidth = 2;
              ctx.globalAlpha = 0.3;
              ctx.beginPath();
              ctx.moveTo(ball.trail[0].x, ball.trail[0].y);
              ball.trail.forEach(point => {
                ctx.lineTo(point.x, point.y);
              });
              ctx.lineTo(ball.x, ball.y);
              ctx.stroke();
              ctx.globalAlpha = 1;
            }

            const gradient = ctx.createRadialGradient(
              ball.x - 3,
              ball.y - 3,
              0,
              ball.x,
              ball.y,
              8
            );
            gradient.addColorStop(0, '#FFFFFF');
            gradient.addColorStop(0.5, player.color);
            gradient.addColorStop(1, '#000000');
            ctx.fillStyle = gradient;
            ctx.beginPath();
            ctx.arc(ball.x, ball.y, 8, 0, Math.PI * 2);
            ctx.fill();

            ctx.strokeStyle = player.color;
            ctx.lineWidth = 2;
            ctx.beginPath();
            ctx.arc(ball.x, ball.y, 10, 0, Math.PI * 2);
            ctx.stroke();
          });
        };

        const drawAimingLine = () => {
          if (gamePhase !== 'aiming' || currentPlayer.isComputer) return;

          const currentBall = balls.get(currentPlayer.id);
          if (!currentBall) return;

          const radians = (swingAngle - 90) * Math.PI / 180;
          const lineLength = swingPower * 2;
          const endX = currentBall.x + Math.cos(radians) * lineLength;
          const endY = currentBall.y + Math.sin(radians) * lineLength;

          const gradient = ctx.createLinearGradient(currentBall.x, currentBall.y, endX, endY);
          gradient.addColorStop(0, currentPlayer.color);
          gradient.addColorStop(1, 'rgba(255, 255, 255, 0.5)');

          ctx.strokeStyle = gradient;
          ctx.lineWidth = 3;
          ctx.setLineDash([10, 5]);
          ctx.beginPath();
          ctx.moveTo(currentBall.x, currentBall.y);
          ctx.lineTo(endX, endY);
          ctx.stroke();
          ctx.setLineDash([]);

          ctx.fillStyle = currentPlayer.color;
          ctx.beginPath();
          ctx.arc(endX, endY, 6, 0, Math.PI * 2);
          ctx.fill();
        };

        const drawParticles = () => {
          particles.forEach(particle => {
            ctx.globalAlpha = particle.life;
            ctx.fillStyle = particle.color;
            ctx.beginPath();
            ctx.arc(particle.x, particle.y, particle.size * particle.life, 0, Math.PI * 2);
            ctx.fill();
          });
          ctx.globalAlpha = 1;
        };

        const drawWindIndicator = () => {
          const centerX = 50;
          const centerY = 50;
          const windLength = course.wind.speed * 2;

          ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
          ctx.beginPath();
          ctx.arc(centerX, centerY, 25, 0, Math.PI * 2);
          ctx.fill();

          ctx.strokeStyle = '#333';
          ctx.lineWidth = 2;
          ctx.beginPath();
          ctx.arc(centerX, centerY, 25, 0, Math.PI * 2);
          ctx.stroke();

          const radians = course.wind.direction * Math.PI / 180;
          const endX = centerX + Math.cos(radians) * windLength;
          const endY = centerY + Math.sin(radians) * windLength;

          ctx.strokeStyle = '#FF6B6B';
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.moveTo(centerX, centerY);
          ctx.lineTo(endX, endY);
          ctx.stroke();

          ctx.fillStyle = '#FF6B6B';
          ctx.beginPath();
          ctx.moveTo(endX, endY);
          const arrowAngle = Math.atan2(endY - centerY, endX - centerX);
          ctx.lineTo(
            endX - 10 * Math.cos(arrowAngle - Math.PI / 6),
            endY - 10 * Math.sin(arrowAngle - Math.PI / 6)
          );
          ctx.lineTo(
            endX - 10 * Math.cos(arrowAngle + Math.PI / 6),
            endY - 10 * Math.sin(arrowAngle + Math.PI / 6)
          );
          ctx.closePath();
          ctx.fill();

          ctx.fillStyle = '#333';
          ctx.font = 'bold 10px Arial';
          ctx.textAlign = 'center';
          ctx.fillText(`${course.wind.speed} mph`, centerX, centerY + 40);
        };

        const draw = () => {
          ctx.clearRect(0, 0, 800, 500);
          waveOffsetRef.current += 0.02;

          drawBackground();
          drawTee();
          drawObstacles();
          drawHole();
          drawBalls();
          drawAimingLine();
          drawParticles();
          drawWindIndicator();

          animationRef.current = requestAnimationFrame(draw);
        };

        draw();

        return () => {
          if (animationRef.current) {
            cancelAnimationFrame(animationRef.current);
          }
        };
      }, [course, balls, players, currentPlayer, swingPower, swingAngle, particles, gamePhase]);

      const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
        if (gamePhase !== 'aiming' || currentPlayer.isComputer) return;

        const canvas = canvasRef.current;
        if (!canvas) return;

        const rect = canvas.getBoundingClientRect();
        const x = e.clientX - rect.left;
        const y = e.clientY - rect.top;

        const currentBall = balls.get(currentPlayer.id);
        if (!currentBall) return;

        const dx = x - currentBall.x;
        const dy = y - currentBall.y;
        const angle = Math.atan2(dy, dx) * 180 / Math.PI + 90;

        onAngleChange(angle);
      };

      const handleSwing = () => {
        if (gamePhase !== 'aiming' || currentPlayer.isComputer) return;
        onSwing(swingPower, swingAngle);
      };

      return (
        <div className="relative">
          <canvas
            ref={canvasRef}
            width={800}
            height={500}
            onClick={handleCanvasClick}
            className="rounded-lg shadow-2xl border-4 border-amber-300"
          />

          {gamePhase === 'aiming' && !currentPlayer.isComputer && (
            <div className="absolute bottom-4 left-1/2 transform -translate-x-1/2 bg-white/90 backdrop-blur-sm rounded-lg p-4 shadow-lg">
              <div className="flex items-center gap-6">
                <div className="flex flex-col items-center">
                  <label className="text-sm font-semibold text-gray-700 mb-2">Power</label>
                  <input
                    type="range"
                    min="10"
                    max="100"
                    value={swingPower}
                    onChange={(e) => onPowerChange(Number(e.target.value))}
                    className="w-32 h-2 bg-gradient-to-r from-green-400 via-yellow-400 to-red-400 rounded-lg appearance-none cursor-pointer"
                  />
                  <span className="text-sm font-bold mt-1">{swingPower}%</span>
                </div>

                <div className="flex flex-col items-center">
                  <label className="text-sm font-semibold text-gray-700 mb-2">Angle</label>
                  <input
                    type="range"
                    min="-180"
                    max="180"
                    value={swingAngle}
                    onChange={(e) => onAngleChange(Number(e.target.value))}
                    className="w-32 h-2 bg-gradient-to-r from-blue-400 to-blue-600 rounded-lg appearance-none cursor-pointer"
                  />
                  <span className="text-sm font-bold mt-1">{swingAngle}°</span>
                </div>

                <button
                  onClick={handleSwing}
                  className="px-6 py-3 bg-gradient-to-r from-green-500 to-emerald-600 text-white font-bold rounded-lg shadow-lg hover:from-green-600 hover:to-emerald-700 transform hover:scale-105 transition-all"
                >
                  SWING!
                </button>
              </div>
            </div>
          )}
        </div>
      );
    };

    export default GolfCourse;
