import React, { useRef, useEffect, useState } from 'react';
import { Play, Pause, RotateCcw } from 'lucide-react';

interface Player {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  team: 'red' | 'blue';
  radius: number;
  hasBall: boolean;
  stamina: number;
  direction: number;
}

interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
}

interface GameState {
  players: Player[];
  ball: Ball;
  score: { red: number; blue: number };
  possession: 'red' | 'blue';
  gameTime: number;
  gameActive: boolean;
  quarter: number;
  lastTry: string;
}

const RugbyGameSimulation: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [gameState, setGameState] = useState<GameState>({
    players: [],
    ball: { x: 400, y: 250, vx: 0, vy: 0, radius: 8 },
    score: { red: 0, blue: 0 },
    possession: 'red',
    gameTime: 0,
    gameActive: false,
    quarter: 1,
    lastTry: ''
  });

  const gameRef = useRef<GameState>(gameState);
  const animationRef = useRef<number | null>(null);

  // Initialize game
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Create players
    const players: Player[] = [];
    const positions = [
      { x: 100, y: 100 }, { x: 100, y: 200 }, { x: 100, y: 300 }, { x: 100, y: 400 },
      { x: 150, y: 150 }, { x: 150, y: 250 }, { x: 150, y: 350 },
      { x: 200, y: 200 }, { x: 200, y: 300 },
      { x: 250, y: 250 },
      { x: 600, y: 100 }, { x: 600, y: 200 }, { x: 600, y: 300 }, { x: 600, y: 400 },
      { x: 550, y: 150 }, { x: 550, y: 250 }, { x: 550, y: 350 },
      { x: 500, y: 200 }, { x: 500, y: 300 },
      { x: 450, y: 250 }
    ];

    positions.forEach((pos, idx) => {
      players.push({
        id: idx,
        x: pos.x,
        y: pos.y,
        vx: 0,
        vy: 0,
        team: idx < 10 ? 'red' : 'blue',
        radius: 12,
        hasBall: idx === 0,
        stamina: 100,
        direction: idx < 10 ? 0 : Math.PI
      });
    });

    const newGameState: GameState = {
      players,
      ball: { x: 100, y: 250, vx: 0, vy: 0, radius: 8 },
      score: { red: 0, blue: 0 },
      possession: 'red',
      gameTime: 0,
      gameActive: true,
      quarter: 1,
      lastTry: ''
    };

    setGameState(newGameState);
    gameRef.current = newGameState;
  }, []);

  // Game simulation
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const fieldWidth = canvas.width;
    const fieldHeight = canvas.height;

    const updateGame = () => {
      const state = gameRef.current;
      if (!state.gameActive) return;

      // Update game time
      state.gameTime += 1 / 60;
      if (state.gameTime > 1200) {
        if (state.quarter < 4) {
          state.quarter++;
          state.gameTime = 0;
        } else {
          state.gameActive = false;
        }
      }

      // Update player AI and movement
      state.players.forEach((player) => {
        const ballPlayer = state.players.find(p => p.hasBall);
        const isBallOwner = player.hasBall;
        const targetX = isBallOwner ? (player.team === 'red' ? fieldWidth - 50 : 50) : ballPlayer?.x || state.ball.x;
        const targetY = ballPlayer?.y || state.ball.y;

        // Move towards target
        const dx = targetX - player.x;
        const dy = targetY - player.y;
        const distance = Math.sqrt(dx * dx + dy * dy);

        if (distance > 5) {
          const speed = isBallOwner ? 2.5 : 2;
          player.vx = (dx / distance) * speed;
          player.vy = (dy / distance) * speed;
          player.direction = Math.atan2(dy, dx);
        } else {
          player.vx *= 0.9;
          player.vy *= 0.9;
        }

        // Update position
        player.x += player.vx;
        player.y += player.vy;

        // Boundary collision
        if (player.y - player.radius < 0) player.y = player.radius;
        if (player.y + player.radius > fieldHeight) player.y = fieldHeight - player.radius;
        if (player.x - player.radius < 0) player.x = player.radius;
        if (player.x + player.radius > fieldWidth) player.x = fieldWidth - player.radius;

        // Stamina management
        player.stamina = Math.min(100, player.stamina + 0.1);
        if (Math.abs(player.vx) > 0.5 || Math.abs(player.vy) > 0.5) {
          player.stamina = Math.max(0, player.stamina - 0.2);
        }

        // Player collision
        state.players.forEach((other) => {
          if (other.id !== player.id) {
            const cdx = other.x - player.x;
            const cdy = other.y - player.y;
            const cdist = Math.sqrt(cdx * cdx + cdy * cdy);
            const minDist = player.radius + other.radius;

            if (cdist < minDist) {
              const overlap = minDist - cdist;
              const angle = Math.atan2(cdy, cdx);
              player.x -= Math.cos(angle) * overlap / 2;
              player.y -= Math.sin(angle) * overlap / 2;
              other.x += Math.cos(angle) * overlap / 2;
              other.y += Math.sin(angle) * overlap / 2;

              // Ball transfer on tackle
              if (player.hasBall && other.team !== player.team && other.stamina > 30) {
                player.hasBall = false;
                other.hasBall = true;
                state.possession = other.team;
              }
            }
          }
        });
      });

      // Ball physics
      if (state.ball) {
        const ballPlayer = state.players.find(p => p.hasBall);

        if (ballPlayer) {
          state.ball.x = ballPlayer.x + Math.cos(ballPlayer.direction) * 15;
          state.ball.y = ballPlayer.y + Math.sin(ballPlayer.direction) * 15;
          state.ball.vx = ballPlayer.vx * 0.8;
          state.ball.vy = ballPlayer.vy * 0.8;
        } else {
          state.ball.vx *= 0.98;
          state.ball.vy *= 0.98;
          state.ball.x += state.ball.vx;
          state.ball.y += state.ball.vy;

          // Ball boundary collision
          if (state.ball.y - state.ball.radius < 0 || state.ball.y + state.ball.radius > fieldHeight) {
            state.ball.vy *= -0.8;
            state.ball.y = Math.max(state.ball.radius, Math.min(fieldHeight - state.ball.radius, state.ball.y));
          }
        }

        // Try detection (touchdown)
        if (state.ball.x < state.ball.radius) {
          state.score.blue += 5;
          state.lastTry = 'Blue Team Try!';
          state.possession = 'red';
          state.players.forEach(p => p.hasBall = false);
          state.players[0].hasBall = true;
          state.ball.x = 100;
          state.ball.y = 250;
        } else if (state.ball.x > fieldWidth - state.ball.radius) {
          state.score.red += 5;
          state.lastTry = 'Red Team Try!';
          state.possession = 'blue';
          state.players.forEach(p => p.hasBall = false);
          state.players[10].hasBall = true;
          state.ball.x = fieldWidth - 100;
          state.ball.y = 250;
        }
      }

      setGameState({ ...state });
      gameRef.current = state;
    };

    const animate = () => {
      updateGame();

      // Clear canvas
      ctx.fillStyle = '#2d5016';
      ctx.fillRect(0, 0, fieldWidth, fieldHeight);

      // Draw field lines
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
      ctx.lineWidth = 2;
      for (let i = 0; i < fieldWidth; i += 40) {
        ctx.beginPath();
        ctx.moveTo(i, 0);
        ctx.lineTo(i, fieldHeight);
        ctx.stroke();
      }

      // Draw center line
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.5)';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(fieldWidth / 2, 0);
      ctx.lineTo(fieldWidth / 2, fieldHeight);
      ctx.stroke();

      // Draw try zones
      ctx.fillStyle = 'rgba(255, 0, 0, 0.1)';
      ctx.fillRect(0, 0, 40, fieldHeight);
      ctx.fillStyle = 'rgba(0, 0, 255, 0.1)';
      ctx.fillRect(fieldWidth - 40, 0, 40, fieldHeight);

      // Draw players
      gameRef.current.players.forEach((player) => {
        ctx.fillStyle = player.team === 'red' ? '#ef4444' : '#3b82f6';
        ctx.beginPath();
        ctx.arc(player.x, player.y, player.radius, 0, Math.PI * 2);
        ctx.fill();

        // Player number
        ctx.fillStyle = 'white';
        ctx.font = 'bold 10px Arial';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(player.id.toString(), player.x, player.y);

        // Stamina bar
        ctx.fillStyle = 'red';
        ctx.fillRect(player.x - 15, player.y - 25, 30, 3);
        ctx.fillStyle = 'green';
        ctx.fillRect(player.x - 15, player.y - 25, (player.stamina / 100) * 30, 3);

        // Ball indicator
        if (player.hasBall) {
          ctx.strokeStyle = 'yellow';
          ctx.lineWidth = 3;
          ctx.beginPath();
          ctx.arc(player.x, player.y, player.radius + 5, 0, Math.PI * 2);
          ctx.stroke();
        }
      });

      // Draw ball
      if (gameRef.current.ball) {
        ctx.fillStyle = '#8b7355';
        ctx.beginPath();
        const ball = gameRef.current.ball;
        ctx.ellipse(ball.x, ball.y, ball.radius * 2, ball.radius, 0, 0, Math.PI * 2);
        ctx.fill();
        ctx.strokeStyle = '#fff';
        ctx.lineWidth = 1;
        ctx.stroke();
      }

      animationRef.current = requestAnimationFrame(animate);
    };

    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, []);

  const handlePlayPause = () => {
    gameRef.current.gameActive = !gameRef.current.gameActive;
    setGameState({ ...gameRef.current });
  };

  const handleReset = () => {
    const players: Player[] = [];
    const positions = [
      { x: 100, y: 100 }, { x: 100, y: 200 }, { x: 100, y: 300 }, { x: 100, y: 400 },
      { x: 150, y: 150 }, { x: 150, y: 250 }, { x: 150, y: 350 },
      { x: 200, y: 200 }, { x: 200, y: 300 },
      { x: 250, y: 250 },
      { x: 600, y: 100 }, { x: 600, y: 200 }, { x: 600, y: 300 }, { x: 600, y: 400 },
      { x: 550, y: 150 }, { x: 550, y: 250 }, { x: 550, y: 350 },
      { x: 500, y: 200 }, { x: 500, y: 300 },
      { x: 450, y: 250 }
    ];

    positions.forEach((pos, idx) => {
      players.push({
        id: idx,
        x: pos.x,
        y: pos.y,
        vx: 0,
        vy: 0,
        team: idx < 10 ? 'red' : 'blue',
        radius: 12,
        hasBall: idx === 0,
        stamina: 100,
        direction: idx < 10 ? 0 : Math.PI
      });
    });

    const newGameState: GameState = {
      players,
      ball: { x: 100, y: 250, vx: 0, vy: 0, radius: 8 },
      score: { red: 0, blue: 0 },
      possession: 'red',
      gameTime: 0,
      gameActive: true,
      quarter: 1,
      lastTry: ''
    };

    setGameState(newGameState);
    gameRef.current = newGameState;
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="w-full h-screen bg-gray-900 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-6xl">
        <div className="mb-4 text-center">
          <h1 className="text-4xl font-bold text-white mb-2">Rugby Game Simulation</h1>
          <div className="flex justify-between items-center bg-gray-800 rounded-lg p-4 mb-4">
            <div className="text-2xl font-bold text-red-500">Red: {gameState.score.red}</div>
            <div className="text-center">
              <div className="text-xl text-white">Q{gameState.quarter} - {formatTime(gameState.gameTime)}</div>
              <div className="text-sm text-gray-400">Possession: {gameState.possession === 'red' ? 'Red Team' : 'Blue Team'}</div>
              {gameState.lastTry && <div className="text-lg text-yellow-400 font-bold">{gameState.lastTry}</div>}
            </div>
            <div className="text-2xl font-bold text-blue-500">Blue: {gameState.score.blue}</div>
          </div>
        </div>

        <canvas
          ref={canvasRef}
          width={800}
          height={500}
          className="w-full border-4 border-white rounded-lg shadow-2xl"
        />

        <div className="flex gap-4 mt-4 justify-center">
          <button
            onClick={handlePlayPause}
            className="flex items-center gap-2 bg-green-600 hover:bg-green-700 text-white font-bold py-2 px-6 rounded-lg transition"
          >
            {gameState.gameActive ? <Pause size={20} /> : <Play size={20} />}
            {gameState.gameActive ? 'Pause' : 'Play'}
          </button>
          <button
            onClick={handleReset}
            className="flex items-center gap-2 bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-6 rounded-lg transition"
          >
            <RotateCcw size={20} />
            Reset Game
          </button>
        </div>

        <div className="mt-4 bg-gray-800 rounded-lg p-4 text-white text-sm">
          <h3 className="font-bold mb-2">Game Info:</h3>
          <ul className="space-y-1 text-gray-300">
            <li>• Red players (left): 10 | Blue players (right): 10</li>
            <li>• Try = 5 points (reach opponent's end zone)</li>
            <li>• Yellow ring = Player with ball</li>
            <li>• Green bar = Player stamina</li>
            <li>• Game has 4 quarters of 20 minutes each</li>
          </ul>
        </div>
      </div>
    </div>
  );
};

export default RugbyGameSimulation;
