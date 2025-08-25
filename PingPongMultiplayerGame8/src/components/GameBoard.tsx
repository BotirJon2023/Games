import React, { useRef, useEffect } from 'react';
import Paddle from './Paddle';
import Ball from './Ball';
import ParticleSystem from './ParticleSystem';
import { Position } from '../types/game';

interface GameBoardProps {
  ballPosition: Position;
  paddle1Y: number;
  paddle2Y: number;
  gameWidth: number;
  gameHeight: number;
  paddleHeight: number;
  paddleWidth: number;
  ballSize: number;
}

const GameBoard: React.FC<GameBoardProps> = ({
  ballPosition,
  paddle1Y,
  paddle2Y,
  gameWidth,
  gameHeight,
  paddleHeight,
  paddleWidth,
  ballSize
}) => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  
  // Draw background effects
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    canvas.width = gameWidth;
    canvas.height = gameHeight;
    
    const drawBackground = () => {
      // Clear canvas
      ctx.clearRect(0, 0, gameWidth, gameHeight);
      
      // Draw center line
      ctx.strokeStyle = '#10b981';
      ctx.lineWidth = 2;
      ctx.setLineDash([10, 10]);
      ctx.beginPath();
      ctx.moveTo(gameWidth / 2, 0);
      ctx.lineTo(gameWidth / 2, gameHeight);
      ctx.stroke();
      ctx.setLineDash([]);
      
      // Draw center circle
      ctx.strokeStyle = '#10b981';
      ctx.lineWidth = 2;
      ctx.beginPath();
      ctx.arc(gameWidth / 2, gameHeight / 2, 50, 0, Math.PI * 2);
      ctx.stroke();
      
      // Draw corner arcs
      const arcRadius = 30;
      const corners = [
        { x: 0, y: 0, startAngle: 0, endAngle: Math.PI / 2 },
        { x: gameWidth, y: 0, startAngle: Math.PI / 2, endAngle: Math.PI },
        { x: gameWidth, y: gameHeight, startAngle: Math.PI, endAngle: 3 * Math.PI / 2 },
        { x: 0, y: gameHeight, startAngle: 3 * Math.PI / 2, endAngle: 2 * Math.PI }
      ];
      
      ctx.strokeStyle = '#065f46';
      ctx.lineWidth = 1;
      corners.forEach(corner => {
        ctx.beginPath();
        ctx.arc(corner.x, corner.y, arcRadius, corner.startAngle, corner.endAngle);
        ctx.stroke();
      });
    };
    
    drawBackground();
  }, [gameWidth, gameHeight]);
  
  return (
    <div className="relative w-full h-full">
      {/* Background canvas for effects */}
      <canvas
        ref={canvasRef}
        className="absolute inset-0 pointer-events-none"
        style={{ width: '100%', height: '100%' }}
      />
      
      {/* Game elements */}
      <div className="relative w-full h-full">
        {/* Paddles */}
        <Paddle
          x={paddleWidth}
          y={paddle1Y}
          width={paddleWidth}
          height={paddleHeight}
          player="player1"
        />
        
        <Paddle
          x={gameWidth - paddleWidth * 2}
          y={paddle2Y}
          width={paddleWidth}
          height={paddleHeight}
          player="player2"
        />
        
        {/* Ball */}
        <Ball
          x={ballPosition.x}
          y={ballPosition.y}
          size={ballSize}
        />
        
        {/* Particle system for effects */}
        <ParticleSystem />
        
        {/* Glow effects */}
        <div className="absolute inset-0 pointer-events-none">
          {/* Top and bottom glow lines */}
          <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-transparent via-green-400 to-transparent opacity-60" />
          <div className="absolute bottom-0 left-0 w-full h-0.5 bg-gradient-to-r from-transparent via-green-400 to-transparent opacity-60" />
          
          {/* Side glow effects */}
          <div className="absolute left-0 top-0 w-0.5 h-full bg-gradient-to-b from-transparent via-blue-400 to-transparent opacity-40" />
          <div className="absolute right-0 top-0 w-0.5 h-full bg-gradient-to-b from-transparent via-red-400 to-transparent opacity-40" />
        </div>
      </div>
    </div>
  );
};

export default GameBoard;