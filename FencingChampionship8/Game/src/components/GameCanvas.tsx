import React, { useRef, useEffect, useCallback } from 'react';
import { Fencer, Particle, GameState } from '../types/game';
import { GAME_CONFIG } from '../utils/gameLogic';

interface GameCanvasProps {
  gameState: GameState;
  onKeyPress: (key: string) => void;
}

export const GameCanvas: React.FC<GameCanvasProps> = ({ gameState, onKeyPress }) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animationFrameRef = useRef<number>();

  const drawFencer = useCallback((ctx: CanvasRenderingContext2D, fencer: Fencer) => {
    const { position, facing, isAttacking, isParrying, isStunned, style } = fencer;
    
    // Save context for transformations
    ctx.save();
    
    // Draw fencer body
    ctx.fillStyle = style.color;
    ctx.fillRect(position.x - 15, position.y - 60, 30, 60);
    
    // Draw head
    ctx.beginPath();
    ctx.arc(position.x, position.y - 70, 12, 0, Math.PI * 2);
    ctx.fill();
    
    // Draw mask (protective gear)
    ctx.strokeStyle = '#2D3748';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.arc(position.x, position.y - 70, 15, 0, Math.PI * 2);
    ctx.stroke();

    // Draw weapon based on state
    ctx.strokeStyle = '#A0AEC0';
    ctx.lineWidth = 3;
    
    if (isAttacking) {
      // Extended weapon during attack
      const weaponLength = style.reach;
      const weaponX = position.x + (facing === 'right' ? 15 : -15);
      ctx.beginPath();
      ctx.moveTo(weaponX, position.y - 30);
      ctx.lineTo(
        weaponX + (facing === 'right' ? weaponLength : -weaponLength),
        position.y - 35
      );
      ctx.stroke();
      
      // Add glowing effect during attack
      ctx.shadowColor = style.color;
      ctx.shadowBlur = 10;
      ctx.stroke();
      ctx.shadowBlur = 0;
    } else if (isParrying) {
      // Defensive weapon position
      const weaponLength = style.reach * 0.7;
      ctx.beginPath();
      ctx.moveTo(position.x, position.y - 40);
      ctx.lineTo(position.x + (facing === 'right' ? weaponLength * 0.5 : -weaponLength * 0.5), position.y - 60);
      ctx.stroke();
    } else {
      // Normal weapon position
      const weaponLength = style.reach * 0.8;
      ctx.beginPath();
      ctx.moveTo(position.x, position.y - 30);
      ctx.lineTo(
        position.x + (facing === 'right' ? weaponLength : -weaponLength),
        position.y - 25
      );
      ctx.stroke();
    }

    // Draw legs
    ctx.fillStyle = style.color;
    ctx.fillRect(position.x - 8, position.y, 6, 30);
    ctx.fillRect(position.x + 2, position.y, 6, 30);

    // Visual effects for different states
    if (isStunned) {
      // Stunned effect - red overlay
      ctx.fillStyle = 'rgba(255, 0, 0, 0.3)';
      ctx.fillRect(position.x - 20, position.y - 80, 40, 80);
    }

    if (isParrying) {
      // Parry effect - blue glow
      ctx.shadowColor = '#3B82F6';
      ctx.shadowBlur = 15;
      ctx.strokeStyle = '#3B82F6';
      ctx.lineWidth = 5;
      ctx.beginPath();
      ctx.arc(position.x, position.y - 40, 25, 0, Math.PI * 2);
      ctx.stroke();
      ctx.shadowBlur = 0;
    }

    ctx.restore();
  }, []);

  const drawParticles = useCallback((ctx: CanvasRenderingContext2D, particles: Particle[]) => {
    particles.forEach(particle => {
      const alpha = particle.life / particle.maxLife;
      ctx.save();
      ctx.globalAlpha = alpha;
      ctx.fillStyle = particle.color;
      ctx.beginPath();
      ctx.arc(particle.position.x, particle.position.y, particle.size, 0, Math.PI * 2);
      ctx.fill();
      ctx.restore();
    });
  }, []);

  const drawArena = useCallback((ctx: CanvasRenderingContext2D) => {
    // Background
    const gradient = ctx.createLinearGradient(0, 0, 0, GAME_CONFIG.canvasHeight);
    gradient.addColorStop(0, '#1F2937');
    gradient.addColorStop(1, '#374151');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, GAME_CONFIG.canvasWidth, GAME_CONFIG.canvasHeight);

    // Fencing strip
    ctx.fillStyle = '#E5E7EB';
    ctx.fillRect(50, GAME_CONFIG.canvasHeight - 50, GAME_CONFIG.canvasWidth - 100, 20);
    
    // Center line
    ctx.strokeStyle = '#6B7280';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(GAME_CONFIG.canvasWidth / 2, GAME_CONFIG.canvasHeight - 50);
    ctx.lineTo(GAME_CONFIG.canvasWidth / 2, GAME_CONFIG.canvasHeight - 30);
    ctx.stroke();

    // Distance markings
    for (let i = 1; i < 8; i++) {
      const x = (GAME_CONFIG.canvasWidth / 8) * i;
      ctx.beginPath();
      ctx.moveTo(x, GAME_CONFIG.canvasHeight - 45);
      ctx.lineTo(x, GAME_CONFIG.canvasHeight - 35);
      ctx.stroke();
    }

    // Arena walls
    ctx.fillStyle = '#4B5563';
    ctx.fillRect(0, 0, 50, GAME_CONFIG.canvasHeight);
    ctx.fillRect(GAME_CONFIG.canvasWidth - 50, 0, 50, GAME_CONFIG.canvasHeight);
  }, []);

  const drawGameUI = useCallback((ctx: CanvasRenderingContext2D, gameState: GameState) => {
    if (gameState.players.length < 2) return;

    const [player1, player2] = gameState.players;

    // Score display
    ctx.fillStyle = '#1F2937';
    ctx.fillRect(10, 10, 200, 80);
    ctx.fillRect(GAME_CONFIG.canvasWidth - 210, 10, 200, 80);

    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 18px Arial';
    ctx.textAlign = 'left';
    ctx.fillText(player1.name, 15, 30);
    ctx.fillText(`Score: ${player1.score}`, 15, 50);
    ctx.fillText(`Health: ${player1.health}`, 15, 70);

    ctx.textAlign = 'right';
    ctx.fillText(player2.name, GAME_CONFIG.canvasWidth - 15, 30);
    ctx.fillText(`Score: ${player2.score}`, GAME_CONFIG.canvasWidth - 15, 50);
    ctx.fillText(`Health: ${player2.health}`, GAME_CONFIG.canvasWidth - 15, 70);

    // Timer
    ctx.fillStyle = '#1F2937';
    ctx.fillRect(GAME_CONFIG.canvasWidth / 2 - 60, 10, 120, 40);
    ctx.fillStyle = '#FFFFFF';
    ctx.textAlign = 'center';
    ctx.font = 'bold 20px Arial';
    const minutes = Math.floor(gameState.timeRemaining / 60);
    const seconds = Math.floor(gameState.timeRemaining % 60);
    ctx.fillText(`${minutes}:${seconds.toString().padStart(2, '0')}`, GAME_CONFIG.canvasWidth / 2, 35);

    // Game status messages
    if (gameState.gameStatus === 'finished' && gameState.winner) {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
      ctx.fillRect(0, 0, GAME_CONFIG.canvasWidth, GAME_CONFIG.canvasHeight);
      
      ctx.fillStyle = '#FFFFFF';
      ctx.font = 'bold 36px Arial';
      ctx.textAlign = 'center';
      ctx.fillText('VICTORY!', GAME_CONFIG.canvasWidth / 2, GAME_CONFIG.canvasHeight / 2 - 20);
      ctx.font = 'bold 24px Arial';
      ctx.fillText(`${gameState.winner} Wins!`, GAME_CONFIG.canvasWidth / 2, GAME_CONFIG.canvasHeight / 2 + 20);
    }
  }, []);

  const render = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, GAME_CONFIG.canvasWidth, GAME_CONFIG.canvasHeight);

    // Draw arena
    drawArena(ctx);

    // Draw particles
    drawParticles(ctx, gameState.particles);

    // Draw fencers
    gameState.players.forEach(fencer => {
      drawFencer(ctx, fencer);
    });

    // Draw UI overlay
    drawGameUI(ctx, gameState);

    animationFrameRef.current = requestAnimationFrame(render);
  }, [gameState, drawArena, drawParticles, drawFencer, drawGameUI]);

  const handleKeyDown = useCallback((event: KeyboardEvent) => {
    event.preventDefault();
    onKeyPress(event.key.toLowerCase());
  }, [onKeyPress]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (canvas) {
      canvas.focus();
      canvas.addEventListener('keydown', handleKeyDown);
      return () => {
        canvas.removeEventListener('keydown', handleKeyDown);
      };
    }
  }, [handleKeyDown]);

  useEffect(() => {
    animationFrameRef.current = requestAnimationFrame(render);
    return () => {
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
    };
  }, [render]);

  return (
    <canvas
      ref={canvasRef}
      width={GAME_CONFIG.canvasWidth}
      height={GAME_CONFIG.canvasHeight}
      className="border border-gray-300 rounded-lg shadow-lg bg-gray-900 focus:outline-none focus:ring-2 focus:ring-blue-500"
      tabIndex={0}
    />
  );
};