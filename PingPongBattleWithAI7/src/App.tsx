import React, { useEffect, useRef, useState } from 'react';
import { Play, Pause, RotateCcw, Settings, Trophy, Zap } from 'lucide-react';

interface GameState {
  isPlaying: boolean;
  isPaused: boolean;
  score: { player: number; ai: number };
  gameOver: boolean;
  winner: 'player' | 'ai' | null;
  difficulty: 'easy' | 'medium' | 'hard';
}

interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  trail: { x: number; y: number; alpha: number }[];
}

interface Paddle {
  x: number;
  y: number;
  width: number;
  height: number;
  speed: number;
  targetY?: number;
}

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
  size: number;
}

function App() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const animationRef = useRef<number>();
  const [gameState, setGameState] = useState<GameState>({
    isPlaying: false,
    isPaused: false,
    score: { player: 0, ai: 0 },
    gameOver: false,
    winner: null,
    difficulty: 'medium'
  });

  const gameRef = useRef({
    ball: {
      x: 400,
      y: 300,
      vx: 5,
      vy: 3,
      radius: 8,
      trail: []
    } as Ball,
    playerPaddle: {
      x: 50,
      y: 250,
      width: 15,
      height: 100,
      speed: 8
    } as Paddle,
    aiPaddle: {
      x: 735,
      y: 250,
      width: 15,
      height: 100,
      speed: 6,
      targetY: 250
    } as Paddle,
    particles: [] as Particle[],
    keys: {
      up: false,
      down: false
    },
    lastTime: 0,
    canvas: null as HTMLCanvasElement | null,
    ctx: null as CanvasRenderingContext2D | null
  });

  // Initialize game
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    gameRef.current.canvas = canvas;
    gameRef.current.ctx = ctx;

    // Set canvas size
    canvas.width = 800;
    canvas.height = 600;

    // Keyboard event listeners
    const handleKeyDown = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'ArrowUp':
        case 'w':
        case 'W':
          gameRef.current.keys.up = true;
          e.preventDefault();
          break;
        case 'ArrowDown':
        case 's':
        case 'S':
          gameRef.current.keys.down = true;
          e.preventDefault();
          break;
        case ' ':
          if (gameState.isPlaying && !gameState.gameOver) {
            setGameState(prev => ({ ...prev, isPaused: !prev.isPaused }));
          }
          e.preventDefault();
          break;
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      switch (e.key) {
        case 'ArrowUp':
        case 'w':
        case 'W':
          gameRef.current.keys.up = false;
          break;
        case 'ArrowDown':
        case 's':
        case 'S':
          gameRef.current.keys.down = false;
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [gameState.isPlaying, gameState.gameOver]);

  // Game loop
  useEffect(() => {
    if (!gameState.isPlaying || gameState.isPaused || gameState.gameOver) {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
      return;
    }

    const gameLoop = (currentTime: number) => {
      const deltaTime = currentTime - gameRef.current.lastTime;
      gameRef.current.lastTime = currentTime;

      updateGame(deltaTime);
      renderGame();

      animationRef.current = requestAnimationFrame(gameLoop);
    };

    animationRef.current = requestAnimationFrame(gameLoop);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [gameState.isPlaying, gameState.isPaused, gameState.gameOver]);

  const updateGame = (deltaTime: number) => {
    const { ball, playerPaddle, aiPaddle, particles, keys } = gameRef.current;
    const canvas = gameRef.current.canvas!;

    // Update player paddle
    if (keys.up && playerPaddle.y > 0) {
      playerPaddle.y -= playerPaddle.speed;
    }
    if (keys.down && playerPaddle.y < canvas.height - playerPaddle.height) {
      playerPaddle.y += playerPaddle.speed;
    }

    // Update AI paddle with different difficulty behaviors
    updateAI();

    // Update ball
    ball.x += ball.vx;
    ball.y += ball.vy;

    // Add trail effect
    ball.trail.push({ x: ball.x, y: ball.y, alpha: 1 });
    if (ball.trail.length > 10) {
      ball.trail.shift();
    }
    ball.trail.forEach((point, index) => {
      point.alpha = index / ball.trail.length;
    });

    // Ball collision with top and bottom walls
    if (ball.y <= ball.radius || ball.y >= canvas.height - ball.radius) {
      ball.vy = -ball.vy;
      createImpactParticles(ball.x, ball.y, '#60a5fa');
    }

    // Ball collision with paddles
    checkPaddleCollision(playerPaddle, 'player');
    checkPaddleCollision(aiPaddle, 'ai');

    // Ball out of bounds (scoring)
    if (ball.x < -ball.radius) {
      // AI scores
      setGameState(prev => {
        const newScore = { ...prev.score, ai: prev.score.ai + 1 };
        const gameOver = newScore.ai >= 5;
        return {
          ...prev,
          score: newScore,
          gameOver,
          winner: gameOver ? 'ai' : null
        };
      });
      resetBall('left');
      createScoreParticles(100, canvas.height / 2, '#ef4444');
    } else if (ball.x > canvas.width + ball.radius) {
      // Player scores
      setGameState(prev => {
        const newScore = { ...prev.score, player: prev.score.player + 1 };
        const gameOver = newScore.player >= 5;
        return {
          ...prev,
          score: newScore,
          gameOver,
          winner: gameOver ? 'player' : null
        };
      });
      resetBall('right');
      createScoreParticles(700, canvas.height / 2, '#22c55e');
    }

    // Update particles
    updateParticles();
  };

  const updateAI = () => {
    const { ball, aiPaddle } = gameRef.current;
    const canvas = gameRef.current.canvas!;
    
    let aiSpeed = aiPaddle.speed;
    let prediction = ball.y;

    // Adjust AI behavior based on difficulty
    switch (gameState.difficulty) {
      case 'easy':
        aiSpeed = 3;
        // Simple following with delay
        prediction = ball.y + (Math.random() - 0.5) * 100;
        break;
      case 'medium':
        aiSpeed = 5;
        // Predict ball position with some error
        if (ball.vx > 0) {
          const timeToReach = (aiPaddle.x - ball.x) / ball.vx;
          prediction = ball.y + ball.vy * timeToReach * 0.8;
        }
        break;
      case 'hard':
        aiSpeed = 7;
        // Accurate prediction
        if (ball.vx > 0) {
          const timeToReach = (aiPaddle.x - ball.x) / ball.vx;
          prediction = ball.y + ball.vy * timeToReach;
        }
        break;
    }

    // Move AI paddle towards predicted position
    const paddleCenter = aiPaddle.y + aiPaddle.height / 2;
    const diff = prediction - paddleCenter;
    
    if (Math.abs(diff) > 5) {
      if (diff > 0 && aiPaddle.y < canvas.height - aiPaddle.height) {
        aiPaddle.y += Math.min(aiSpeed, diff);
      } else if (diff < 0 && aiPaddle.y > 0) {
        aiPaddle.y += Math.max(-aiSpeed, diff);
      }
    }
  };

  const checkPaddleCollision = (paddle: Paddle, side: 'player' | 'ai') => {
    const { ball } = gameRef.current;
    
    if (ball.x + ball.radius > paddle.x && 
        ball.x - ball.radius < paddle.x + paddle.width &&
        ball.y + ball.radius > paddle.y && 
        ball.y - ball.radius < paddle.y + paddle.height) {
      
      // Calculate hit position on paddle (0 to 1)
      const hitPos = (ball.y - paddle.y) / paddle.height;
      
      // Reverse horizontal velocity and add spin based on hit position
      ball.vx = -ball.vx * 1.05; // Slight speed increase
      ball.vy = (hitPos - 0.5) * 10; // Add vertical spin
      
      // Clamp ball speed
      const maxSpeed = 12;
      if (Math.abs(ball.vx) > maxSpeed) {
        ball.vx = ball.vx > 0 ? maxSpeed : -maxSpeed;
      }
      if (Math.abs(ball.vy) > maxSpeed) {
        ball.vy = ball.vy > 0 ? maxSpeed : -maxSpeed;
      }
      
      // Position ball outside paddle to prevent sticking
      if (side === 'player') {
        ball.x = paddle.x + paddle.width + ball.radius;
      } else {
        ball.x = paddle.x - ball.radius;
      }
      
      // Create impact particles
      const color = side === 'player' ? '#22c55e' : '#ef4444';
      createImpactParticles(ball.x, ball.y, color);
    }
  };

  const createImpactParticles = (x: number, y: number, color: string) => {
    const { particles } = gameRef.current;
    
    for (let i = 0; i < 8; i++) {
      particles.push({
        x,
        y,
        vx: (Math.random() - 0.5) * 10,
        vy: (Math.random() - 0.5) * 10,
        life: 30,
        maxLife: 30,
        color,
        size: Math.random() * 4 + 2
      });
    }
  };

  const createScoreParticles = (x: number, y: number, color: string) => {
    const { particles } = gameRef.current;
    
    for (let i = 0; i < 20; i++) {
      particles.push({
        x,
        y,
        vx: (Math.random() - 0.5) * 15,
        vy: (Math.random() - 0.5) * 15,
        life: 60,
        maxLife: 60,
        color,
        size: Math.random() * 6 + 3
      });
    }
  };

  const updateParticles = () => {
    const { particles } = gameRef.current;
    
    for (let i = particles.length - 1; i >= 0; i--) {
      const particle = particles[i];
      particle.x += particle.vx;
      particle.y += particle.vy;
      particle.vx *= 0.98;
      particle.vy *= 0.98;
      particle.life--;
      
      if (particle.life <= 0) {
        particles.splice(i, 1);
      }
    }
  };

  const renderGame = () => {
    const ctx = gameRef.current.ctx!;
    const canvas = gameRef.current.canvas!;
    const { ball, playerPaddle, aiPaddle, particles } = gameRef.current;

    // Clear canvas with gradient background
    const gradient = ctx.createLinearGradient(0, 0, canvas.width, canvas.height);
    gradient.addColorStop(0, '#0f172a');
    gradient.addColorStop(1, '#1e293b');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    // Draw center line
    ctx.strokeStyle = '#334155';
    ctx.lineWidth = 2;
    ctx.setLineDash([10, 10]);
    ctx.beginPath();
    ctx.moveTo(canvas.width / 2, 0);
    ctx.lineTo(canvas.width / 2, canvas.height);
    ctx.stroke();
    ctx.setLineDash([]);

    // Draw ball trail
    ball.trail.forEach((point, index) => {
      ctx.globalAlpha = point.alpha * 0.5;
      ctx.fillStyle = '#60a5fa';
      ctx.beginPath();
      ctx.arc(point.x, point.y, ball.radius * point.alpha, 0, Math.PI * 2);
      ctx.fill();
    });
    ctx.globalAlpha = 1;

    // Draw ball with glow effect
    const ballGradient = ctx.createRadialGradient(ball.x, ball.y, 0, ball.x, ball.y, ball.radius * 2);
    ballGradient.addColorStop(0, '#ffffff');
    ballGradient.addColorStop(0.7, '#60a5fa');
    ballGradient.addColorStop(1, 'transparent');
    ctx.fillStyle = ballGradient;
    ctx.beginPath();
    ctx.arc(ball.x, ball.y, ball.radius * 2, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = '#ffffff';
    ctx.beginPath();
    ctx.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2);
    ctx.fill();

    // Draw paddles with gradient
    const drawPaddle = (paddle: Paddle, color1: string, color2: string) => {
      const paddleGradient = ctx.createLinearGradient(paddle.x, paddle.y, paddle.x + paddle.width, paddle.y);
      paddleGradient.addColorStop(0, color1);
      paddleGradient.addColorStop(1, color2);
      ctx.fillStyle = paddleGradient;
      ctx.fillRect(paddle.x, paddle.y, paddle.width, paddle.height);
      
      // Add highlight
      ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
      ctx.fillRect(paddle.x, paddle.y, paddle.width / 3, paddle.height);
    };

    drawPaddle(playerPaddle, '#22c55e', '#16a34a');
    drawPaddle(aiPaddle, '#ef4444', '#dc2626');

    // Draw particles
    particles.forEach(particle => {
      const alpha = particle.life / particle.maxLife;
      ctx.globalAlpha = alpha;
      ctx.fillStyle = particle.color;
      ctx.beginPath();
      ctx.arc(particle.x, particle.y, particle.size * alpha, 0, Math.PI * 2);
      ctx.fill();
    });
    ctx.globalAlpha = 1;

    // Draw score
    ctx.font = 'bold 48px Arial';
    ctx.textAlign = 'center';
    ctx.fillStyle = '#22c55e';
    ctx.fillText(gameState.score.player.toString(), canvas.width / 4, 80);
    ctx.fillStyle = '#ef4444';
    ctx.fillText(gameState.score.ai.toString(), (canvas.width * 3) / 4, 80);

    // Draw difficulty indicator
    ctx.font = '16px Arial';
    ctx.fillStyle = '#94a3b8';
    ctx.textAlign = 'center';
    ctx.fillText(`AI: ${gameState.difficulty.toUpperCase()}`, canvas.width / 2, canvas.height - 20);
  };

  const resetBall = (direction: 'left' | 'right') => {
    const { ball } = gameRef.current;
    const canvas = gameRef.current.canvas!;
    
    ball.x = canvas.width / 2;
    ball.y = canvas.height / 2;
    ball.vx = direction === 'left' ? -5 : 5;
    ball.vy = (Math.random() - 0.5) * 6;
    ball.trail = [];
  };

  const startGame = () => {
    setGameState(prev => ({
      ...prev,
      isPlaying: true,
      isPaused: false,
      gameOver: false,
      winner: null,
      score: { player: 0, ai: 0 }
    }));
    resetBall(Math.random() > 0.5 ? 'left' : 'right');
    gameRef.current.particles = [];
  };

  const pauseGame = () => {
    setGameState(prev => ({ ...prev, isPaused: !prev.isPaused }));
  };

  const resetGame = () => {
    setGameState({
      isPlaying: false,
      isPaused: false,
      score: { player: 0, ai: 0 },
      gameOver: false,
      winner: null,
      difficulty: gameState.difficulty
    });
    resetBall('right');
    gameRef.current.particles = [];
  };

  const changeDifficulty = () => {
    const difficulties: ('easy' | 'medium' | 'hard')[] = ['easy', 'medium', 'hard'];
    const currentIndex = difficulties.indexOf(gameState.difficulty);
    const nextIndex = (currentIndex + 1) % difficulties.length;
    setGameState(prev => ({ ...prev, difficulty: difficulties[nextIndex] }));
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-900 via-blue-900 to-slate-900 flex flex-col items-center justify-center p-4">
      <div className="bg-slate-800/50 backdrop-blur-sm rounded-2xl p-6 shadow-2xl border border-slate-700">
        <div className="text-center mb-6">
          <h1 className="text-4xl font-bold text-white mb-2 flex items-center justify-center gap-3">
            <Zap className="text-yellow-400" />
            Ping-Pong Battle AI
            <Trophy className="text-yellow-400" />
          </h1>
          <p className="text-slate-300">Use ↑↓ or W/S keys to control your paddle</p>
        </div>

        <div className="relative">
          <canvas
            ref={canvasRef}
            className="border-2 border-slate-600 rounded-lg shadow-lg bg-slate-900"
            style={{ maxWidth: '100%', height: 'auto' }}
          />
          
          {gameState.gameOver && (
            <div className="absolute inset-0 bg-black/70 backdrop-blur-sm rounded-lg flex items-center justify-center">
              <div className="text-center text-white">
                <h2 className="text-3xl font-bold mb-4">
                  {gameState.winner === 'player' ? '🎉 You Win!' : '🤖 AI Wins!'}
                </h2>
                <p className="text-xl mb-6">
                  Final Score: {gameState.score.player} - {gameState.score.ai}
                </p>
                <button
                  onClick={startGame}
                  className="bg-blue-600 hover:bg-blue-700 text-white px-6 py-3 rounded-lg font-semibold transition-colors"
                >
                  Play Again
                </button>
              </div>
            </div>
          )}

          {gameState.isPaused && !gameState.gameOver && (
            <div className="absolute inset-0 bg-black/50 backdrop-blur-sm rounded-lg flex items-center justify-center">
              <div className="text-center text-white">
                <Pause className="w-16 h-16 mx-auto mb-4" />
                <h2 className="text-2xl font-bold">Game Paused</h2>
                <p className="text-slate-300 mt-2">Press Space to continue</p>
              </div>
            </div>
          )}
        </div>

        <div className="flex flex-wrap gap-4 justify-center mt-6">
          {!gameState.isPlaying ? (
            <button
              onClick={startGame}
              className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-lg font-semibold transition-colors flex items-center gap-2"
            >
              <Play className="w-5 h-5" />
              Start Game
            </button>
          ) : (
            <button
              onClick={pauseGame}
              disabled={gameState.gameOver}
              className="bg-yellow-600 hover:bg-yellow-700 disabled:bg-gray-600 text-white px-6 py-3 rounded-lg font-semibold transition-colors flex items-center gap-2"
            >
              <Pause className="w-5 h-5" />
              {gameState.isPaused ? 'Resume' : 'Pause'}
            </button>
          )}
          
          <button
            onClick={resetGame}
            className="bg-red-600 hover:bg-red-700 text-white px-6 py-3 rounded-lg font-semibold transition-colors flex items-center gap-2"
          >
            <RotateCcw className="w-5 h-5" />
            Reset
          </button>
          
          <button
            onClick={changeDifficulty}
            className="bg-purple-600 hover:bg-purple-700 text-white px-6 py-3 rounded-lg font-semibold transition-colors flex items-center gap-2"
          >
            <Settings className="w-5 h-5" />
            AI: {gameState.difficulty.charAt(0).toUpperCase() + gameState.difficulty.slice(1)}
          </button>
        </div>

        <div className="mt-6 text-center text-slate-400 text-sm">
          <p>First to 5 points wins! • Press Space to pause during game</p>
          <p className="mt-1">AI difficulty affects speed and prediction accuracy</p>
        </div>
      </div>
    </div>
  );
}

export default App;