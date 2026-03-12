import React, { useState, useEffect, useRef } from 'react';
import { Play, Pause, RotateCcw } from 'lucide-react';

interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
}

interface Player {
  id: number;
  x: number;
  y: number;
  width: number;
  height: number;
  team: 'left' | 'right';
}

interface GameState {
  ball: Ball;
  players: Player[];
  leftScore: number;
  rightScore: number;
  isRunning: boolean;
  gameTime: number;
  lastHitBy: string;
  ballTrail: Array<{ x: number; y: number; opacity: number }>;
}

const VolleyballGameSimulation: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const gameStateRef = useRef<GameState>({
    ball: {
      x: 400,
      y: 150,
      vx: 0,
      vy: 0,
      radius: 8,
    },
    players: [
      { id: 1, x: 50, y: 250, width: 40, height: 60, team: 'left' },
      { id: 2, x: 150, y: 200, width: 40, height: 60, team: 'left' },
      { id: 3, x: 650, y: 250, width: 40, height: 60, team: 'right' },
      { id: 4, x: 750, y: 200, width: 40, height: 60, team: 'right' },
    ],
    leftScore: 0,
    rightScore: 0,
    isRunning: false,
    gameTime: 0,
    lastHitBy: '',
    ballTrail: [],
  });

  const [gameState, setGameState] = useState<GameState>(gameStateRef.current);
  const animationFrameRef = useRef<number>();
  const keysPressed = useRef<{ [key: string]: boolean }>({});

  const CANVAS_WIDTH = 800;
  const CANVAS_HEIGHT = 400;
  const NET_X = CANVAS_WIDTH / 2;
  const GRAVITY = 0.3;
  const DAMPING = 0.98;
  const FRICTION = 0.02;

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      keysPressed.current[e.key.toLowerCase()] = true;
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      keysPressed.current[e.key.toLowerCase()] = false;
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, []);

  const updateGame = () => {
    setGameState((prevState) => {
      const state = { ...prevState };
      const ball = { ...state.ball };
      const players = state.players.map((p) => ({ ...p }));

      ball.vy += GRAVITY;
      ball.vx *= (1 - FRICTION);
      ball.x += ball.vx;
      ball.y += ball.vy;

      if (ball.x - ball.radius < 0) {
        state.rightScore += 1;
        ball.x = CANVAS_WIDTH / 2;
        ball.y = 150;
        ball.vx = 0;
        ball.vy = 0;
      }

      if (ball.x + ball.radius > CANVAS_WIDTH) {
        state.leftScore += 1;
        ball.x = CANVAS_WIDTH / 2;
        ball.y = 150;
        ball.vx = 0;
        ball.vy = 0;
      }

      if (ball.y + ball.radius > CANVAS_HEIGHT) {
        ball.y = CANVAS_HEIGHT - ball.radius;
        ball.vy *= -0.75;
        ball.vx *= 0.98;
      }

      if (ball.y - ball.radius < 0) {
        ball.y = ball.radius;
        ball.vy *= -0.8;
      }

      players.forEach((player) => {
        let moved = false;

        if (player.team === 'left') {
          if (keysPressed.current['w'] && player.y > 0) {
            player.y -= 5;
            moved = true;
          }
          if (keysPressed.current['s'] && player.y + player.height < CANVAS_HEIGHT) {
            player.y += 5;
            moved = true;
          }
          if (keysPressed.current['a'] && player.x > 0) {
            player.x -= 5;
            moved = true;
          }
          if (keysPressed.current['d'] && player.x + player.width < NET_X - 20) {
            player.x += 5;
            moved = true;
          }
        } else {
          if (keysPressed.current['arrowright']) {
            if (player.x + player.width < CANVAS_WIDTH) {
              player.x += 5;
              moved = true;
            }
          }
          if (keysPressed.current['arrowleft']) {
            if (player.x > NET_X + 20) {
              player.x -= 5;
              moved = true;
            }
          }
          if (keysPressed.current['arrowup'] && player.y > 0) {
            player.y -= 5;
            moved = true;
          }
          if (keysPressed.current['arrowdown'] && player.y + player.height < CANVAS_HEIGHT) {
            player.y += 5;
            moved = true;
          }
        }

        const dx = ball.x - (player.x + player.width / 2);
        const dy = ball.y - (player.y + player.height / 2);
        const distance = Math.sqrt(dx * dx + dy * dy);
        const minDistance = ball.radius + Math.max(player.width, player.height) / 2;

        if (distance < minDistance) {
          const angle = Math.atan2(dy, dx);
          const speed = Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
          const newSpeed = Math.max(8, speed + 4);

          ball.vx = Math.cos(angle) * newSpeed;
          ball.vy = Math.sin(angle) * newSpeed - 3;

          const overlap = minDistance - distance;
          ball.x += Math.cos(angle) * overlap;
          ball.y += Math.sin(angle) * overlap;

          state.lastHitBy = player.team;
        }
      });

      state.ball = ball;
      state.players = players;

      if (state.isRunning) {
        state.gameTime += 1;
      }

      state.ballTrail = [
        ...state.ballTrail,
        { x: ball.x, y: ball.y, opacity: 1 },
      ].slice(-20);

      state.ballTrail = state.ballTrail.map((point) => ({
        ...point,
        opacity: point.opacity * 0.95,
      }));

      return state;
    });
  };

  useEffect(() => {
    if (!gameState.isRunning) return;

    const interval = setInterval(() => {
      updateGame();
    }, 1000 / 60);

    return () => clearInterval(interval);
  }, [gameState.isRunning]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.fillStyle = '#1a1a2e';
    ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);

    ctx.strokeStyle = '#00ff88';
    ctx.lineWidth = 2;
    ctx.setLineDash([5, 5]);
    ctx.beginPath();
    ctx.moveTo(NET_X, 0);
    ctx.lineTo(NET_X, CANVAS_HEIGHT);
    ctx.stroke();
    ctx.setLineDash([]);

    ctx.strokeStyle = '#00ff88';
    ctx.lineWidth = 1;
    ctx.strokeRect(10, 10, CANVAS_WIDTH - 20, CANVAS_HEIGHT - 20);

    gameState.ballTrail.forEach((point) => {
      ctx.fillStyle = `rgba(255, 200, 87, ${point.opacity * 0.5})`;
      ctx.beginPath();
      ctx.arc(point.x, point.y, 4, 0, Math.PI * 2);
      ctx.fill();
    });

    ctx.fillStyle = '#ffc857';
    ctx.beginPath();
    ctx.arc(gameState.ball.x, gameState.ball.y, gameState.ball.radius, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#ffb400';
    ctx.lineWidth = 1.5;
    ctx.stroke();

    gameState.players.forEach((player) => {
      ctx.fillStyle = player.team === 'left' ? '#00ccff' : '#ff006e';
      ctx.fillRect(player.x, player.y, player.width, player.height);

      ctx.strokeStyle = player.team === 'left' ? '#00ffff' : '#ff3366';
      ctx.lineWidth = 2;
      ctx.strokeRect(player.x, player.y, player.width, player.height);

      ctx.fillStyle = '#000';
      ctx.font = 'bold 10px Arial';
      ctx.textAlign = 'center';
      ctx.fillText(
        player.id.toString(),
        player.x + player.width / 2,
        player.y + player.height / 2 + 3
      );
    });

    ctx.fillStyle = '#00ff88';
    ctx.font = 'bold 24px Arial';
    ctx.textAlign = 'center';
    ctx.fillText(gameState.leftScore.toString(), CANVAS_WIDTH / 4, 40);
    ctx.fillText(gameState.rightScore.toString(), (CANVAS_WIDTH * 3) / 4, 40);

    ctx.font = 'bold 16px Arial';
    ctx.fillText('Left (WASD)', CANVAS_WIDTH / 4, 65);
    ctx.fillText('Right (Arrows)', (CANVAS_WIDTH * 3) / 4, 65);

    ctx.font = 'bold 14px Arial';
    const minutes = Math.floor(gameState.gameTime / 3600);
    const seconds = Math.floor((gameState.gameTime % 3600) / 60);
    const frames = gameState.gameTime % 60;
    ctx.fillText(
      `${minutes}:${seconds.toString().padStart(2, '0')}:${frames.toString().padStart(2, '0')}`,
      CANVAS_WIDTH / 2,
      CANVAS_HEIGHT - 10
    );
  }, [gameState]);

  const toggleGame = () => {
    setGameState((prev) => ({
      ...prev,
      isRunning: !prev.isRunning,
    }));
  };

  const resetGame = () => {
    const newState: GameState = {
      ball: {
        x: 400,
        y: 150,
        vx: 0,
        vy: 0,
        radius: 8,
      },
      players: [
        { id: 1, x: 50, y: 250, width: 40, height: 60, team: 'left' },
        { id: 2, x: 150, y: 200, width: 40, height: 60, team: 'left' },
        { id: 3, x: 650, y: 250, width: 40, height: 60, team: 'right' },
        { id: 4, x: 750, y: 200, width: 40, height: 60, team: 'right' },
      ],
      leftScore: 0,
      rightScore: 0,
      isRunning: false,
      gameTime: 0,
      lastHitBy: '',
      ballTrail: [],
    };
    setGameState(newState);
    gameStateRef.current = newState;
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex flex-col items-center justify-center p-4">
      <div className="max-w-2xl w-full">
        <h1 className="text-4xl font-bold text-center text-cyan-400 mb-2">
          Volleyball Game Simulation
        </h1>
        <p className="text-center text-cyan-300 mb-6 text-sm">
          Physics-based volleyball with smooth animations
        </p>

        <div className="bg-slate-800 rounded-lg shadow-2xl overflow-hidden border-2 border-cyan-500 mb-6">
          <canvas
            ref={canvasRef}
            width={CANVAS_WIDTH}
            height={CANVAS_HEIGHT}
            className="w-full bg-slate-900"
          />
        </div>

        <div className="grid grid-cols-2 gap-4 mb-6">
          <div className="bg-blue-900/40 border border-cyan-400 rounded-lg p-4 text-center">
            <p className="text-cyan-300 text-sm mb-2">Left Team Score</p>
            <p className="text-3xl font-bold text-cyan-400">{gameState.leftScore}</p>
          </div>
          <div className="bg-pink-900/40 border border-pink-400 rounded-lg p-4 text-center">
            <p className="text-pink-300 text-sm mb-2">Right Team Score</p>
            <p className="text-3xl font-bold text-pink-400">{gameState.rightScore}</p>
          </div>
        </div>

        <div className="flex gap-4 mb-6">
          <button
            onClick={toggleGame}
            className="flex-1 bg-gradient-to-r from-cyan-500 to-cyan-600 hover:from-cyan-600 hover:to-cyan-700 text-white font-bold py-3 rounded-lg flex items-center justify-center gap-2 transition-all transform hover:scale-105"
          >
            {gameState.isRunning ? (
              <>
                <Pause size={20} /> Pause
              </>
            ) : (
              <>
                <Play size={20} /> Start
              </>
            )}
          </button>
          <button
            onClick={resetGame}
            className="flex-1 bg-gradient-to-r from-slate-600 to-slate-700 hover:from-slate-700 hover:to-slate-800 text-white font-bold py-3 rounded-lg flex items-center justify-center gap-2 transition-all transform hover:scale-105"
          >
            <RotateCcw size={20} /> Reset
          </button>
        </div>

        <div className="bg-slate-700/50 border border-cyan-400 rounded-lg p-4">
          <h3 className="text-cyan-300 font-bold mb-3">Controls</h3>
          <div className="grid grid-cols-2 gap-3 text-sm text-cyan-200">
            <div>
              <p className="font-semibold text-cyan-400">Left Team (Player 1-2)</p>
              <p>W/A/S/D - Move</p>
            </div>
            <div>
              <p className="font-semibold text-pink-400">Right Team (Player 3-4)</p>
              <p>Arrow Keys - Move</p>
            </div>
          </div>
          <p className="text-xs text-cyan-300 mt-3">
            Ball bounces off players with realistic physics. Score when ball goes out of bounds on opponent side!
          </p>
        </div>
      </div>
    </div>
  );
};

export default VolleyballGameSimulation;
