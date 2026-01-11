import React, { useState, useEffect, useRef } from 'react';
import { Play, Pause, RotateCcw, Volume2, VolumeX } from 'lucide-react';

interface Player {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  team: 'home' | 'away';
  speed: number;
  isSelected: boolean;
}

interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  rotation: number;
}

const HandballGame: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [gameState, setGameState] = useState<'idle' | 'playing' | 'paused'>('idle');
  const [homeScore, setHomeScore] = useState(0);
  const [awayScore, setAwayScore] = useState(0);
  const [time, setTime] = useState(0);
  const [isMuted, setIsMuted] = useState(false);
  const gameStateRef = useRef<'idle' | 'playing' | 'paused'>('idle');
  const playersRef = useRef<Player[]>([]);
  const ballRef = useRef<Ball>({
    x: 400,
    y: 300,
    vx: 0,
    vy: 0,
    radius: 8,
    rotation: 0,
  });
  const scoreRef = useRef({ home: 0, away: 0 });
  const timeRef = useRef(0);
  const selectedPlayerRef = useRef<number | null>(null);

  useEffect(() => {
    initializeGame();
  }, []);

  const initializeGame = () => {
    const players: Player[] = [];

    for (let i = 0; i < 6; i++) {
      players.push({
        id: i,
        x: 100 + i * 40,
        y: 150 + Math.random() * 100,
        vx: 0,
        vy: 0,
        radius: 12,
        team: 'home',
        speed: 2 + Math.random(),
        isSelected: false,
      });

      players.push({
        id: 100 + i,
        x: 700 - i * 40,
        y: 150 + Math.random() * 100,
        vx: 0,
        vy: 0,
        radius: 12,
        team: 'away',
        speed: 2 + Math.random(),
        isSelected: false,
      });
    }

    playersRef.current = players;
    ballRef.current = { x: 400, y: 300, vx: 0, vy: 0, radius: 8, rotation: 0 };
    scoreRef.current = { home: 0, away: 0 };
    timeRef.current = 0;
    selectedPlayerRef.current = null;
    setHomeScore(0);
    setAwayScore(0);
    setTime(0);
  };

  const startGame = () => {
    setGameState('playing');
    gameStateRef.current = 'playing';
    if (ballRef.current.vx === 0 && ballRef.current.vy === 0) {
      ballRef.current.vx = 3;
      ballRef.current.vy = 2;
    }
  };

  const pauseGame = () => {
    setGameState('paused');
    gameStateRef.current = 'paused';
  };

  const resetGame = () => {
    setGameState('idle');
    gameStateRef.current = 'idle';
    initializeGame();
  };

  const playSound = (frequency: number, duration: number) => {
    if (isMuted) return;
    try {
      const audioContext = new (window.AudioContext || (window as any).webkitAudioContext)();
      const oscillator = audioContext.createOscillator();
      const gainNode = audioContext.createGain();

      oscillator.connect(gainNode);
      gainNode.connect(audioContext.destination);

      oscillator.frequency.value = frequency;
      oscillator.type = 'sine';

      gainNode.gain.setValueAtTime(0.3, audioContext.currentTime);
      gainNode.gain.exponentialRampToValueAtTime(0.01, audioContext.currentTime + duration);

      oscillator.start(audioContext.currentTime);
      oscillator.stop(audioContext.currentTime + duration);
    } catch (e) {
      console.log('Audio context not available');
    }
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const gameLoop = setInterval(() => {
      if (gameStateRef.current !== 'playing') {
        renderGame(ctx, canvas);
        return;
      }

      timeRef.current += 0.016;
      setTime(Math.floor(timeRef.current));

      updatePhysics();
      updateAI();
      checkCollisions();
      checkGoals();
      renderGame(ctx, canvas);
    }, 16);

    return () => clearInterval(gameLoop);
  }, [isMuted]);

  const updatePhysics = () => {
    const ball = ballRef.current;
    const canvas = canvasRef.current;
    if (!canvas) return;

    ball.x += ball.vx;
    ball.y += ball.vy;
    ball.vx *= 0.99;
    ball.vy *= 0.99;
    ball.rotation += ball.vx * 0.1;

    if (ball.x - ball.radius < 0 || ball.x + ball.radius > canvas.width) {
      ball.vx *= -0.85;
      ball.x = Math.max(ball.radius, Math.min(canvas.width - ball.radius, ball.x));
    }

    if (ball.y - ball.radius < 0 || ball.y + ball.radius > canvas.height) {
      ball.vy *= -0.85;
      ball.y = Math.max(ball.radius, Math.min(canvas.height - ball.radius, ball.y));
    }

    playersRef.current.forEach((player) => {
      player.x += player.vx;
      player.y += player.vy;
      player.vx *= 0.95;
      player.vy *= 0.95;

      if (player.x - player.radius < 0 || player.x + player.radius > canvas.width) {
        player.vx *= -0.8;
        player.x = Math.max(player.radius, Math.min(canvas.width - player.radius, player.x));
      }

      if (player.y - player.radius < 0 || player.y + player.radius > canvas.height) {
        player.vy *= -0.8;
        player.y = Math.max(player.radius, Math.min(canvas.height - player.radius, player.y));
      }
    });
  };

  const updateAI = () => {
    const ball = ballRef.current;

    playersRef.current.forEach((player) => {
      const ballDist = Math.hypot(ball.x - player.x, ball.y - player.y);

      if (ballDist < 150) {
        const angle = Math.atan2(ball.y - player.y, ball.x - player.x);
        player.vx = Math.cos(angle) * player.speed;
        player.vy = Math.sin(angle) * player.speed;
      } else {
        if (Math.random() < 0.02) {
          player.vx = (Math.random() - 0.5) * player.speed;
          player.vy = (Math.random() - 0.5) * player.speed;
        }
      }

      playersRef.current.forEach((other) => {
        if (player.id !== other.id) {
          const dist = Math.hypot(other.x - player.x, other.y - player.y);
          if (dist < 40 && dist > 0) {
            const angle = Math.atan2(other.y - player.y, other.x - player.x);
            player.vx -= Math.cos(angle) * 0.5;
            player.vy -= Math.sin(angle) * 0.5;
          }
        }
      });
    });
  };

  const checkCollisions = () => {
    const ball = ballRef.current;

    playersRef.current.forEach((player) => {
      const dist = Math.hypot(ball.x - player.x, ball.y - player.y);
      if (dist < ball.radius + player.radius) {
        const angle = Math.atan2(ball.y - player.y, ball.x - player.x);
        const speed = Math.hypot(player.vx, player.vy) + 3;

        ball.vx = Math.cos(angle) * speed;
        ball.vy = Math.sin(angle) * speed;

        ball.x = player.x + Math.cos(angle) * (ball.radius + player.radius);
        ball.y = player.y + Math.sin(angle) * (ball.radius + player.radius);

        playSound(400 + Math.random() * 200, 0.1);
      }
    });

    for (let i = 0; i < playersRef.current.length; i++) {
      for (let j = i + 1; j < playersRef.current.length; j++) {
        const p1 = playersRef.current[i];
        const p2 = playersRef.current[j];
        const dist = Math.hypot(p2.x - p1.x, p2.y - p1.y);

        if (dist < p1.radius + p2.radius) {
          const angle = Math.atan2(p2.y - p1.y, p2.x - p1.x);
          const overlap = (p1.radius + p2.radius - dist) / 2;

          p1.x -= Math.cos(angle) * overlap;
          p1.y -= Math.sin(angle) * overlap;
          p2.x += Math.cos(angle) * overlap;
          p2.y += Math.sin(angle) * overlap;

          const vx1 = p1.vx, vy1 = p1.vy;
          p1.vx = p2.vx * 0.5;
          p1.vy = p2.vy * 0.5;
          p2.vx = vx1 * 0.5;
          p2.vy = vy1 * 0.5;
        }
      }
    }
  };

  const checkGoals = () => {
    const ball = ballRef.current;
    const canvas = canvasRef.current;
    if (!canvas) return;

    if (ball.x - ball.radius < -20) {
      scoreRef.current.away += 1;
      setAwayScore(scoreRef.current.away);
      playSound(800, 0.3);
      resetBall();
    } else if (ball.x + ball.radius > canvas.width + 20) {
      scoreRef.current.home += 1;
      setHomeScore(scoreRef.current.home);
      playSound(600, 0.3);
      resetBall();
    }
  };

  const resetBall = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    ballRef.current.x = canvas.width / 2;
    ballRef.current.y = canvas.height / 2;
    ballRef.current.vx = (Math.random() - 0.5) * 4;
    ballRef.current.vy = (Math.random() - 0.5) * 4;
  };

  const renderGame = (ctx: CanvasRenderingContext2D, canvas: HTMLCanvasElement) => {
    ctx.fillStyle = '#1a472a';
    ctx.fillRect(0, 0, canvas.width, canvas.height);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.3)';
    ctx.lineWidth = 2;
    ctx.setLineDash([10, 10]);
    ctx.beginPath();
    ctx.moveTo(canvas.width / 2, 0);
    ctx.lineTo(canvas.width / 2, canvas.height);
    ctx.stroke();
    ctx.setLineDash([]);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.5)';
    ctx.lineWidth = 3;
    ctx.strokeRect(20, 80, 80, 240);
    ctx.strokeRect(canvas.width - 100, 80, 80, 240);

    playersRef.current.forEach((player) => {
      ctx.fillStyle = player.team === 'home' ? '#ff4444' : '#4444ff';
      ctx.beginPath();
      ctx.arc(player.x, player.y, player.radius, 0, Math.PI * 2);
      ctx.fill();

      if (selectedPlayerRef.current === player.id) {
        ctx.strokeStyle = '#ffff00';
        ctx.lineWidth = 3;
        ctx.beginPath();
        ctx.arc(player.x, player.y, player.radius + 5, 0, Math.PI * 2);
        ctx.stroke();
      }

      ctx.fillStyle = 'white';
      ctx.font = 'bold 10px Arial';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillText(`${player.id % 100}`, player.x, player.y);

      const speed = Math.hypot(player.vx, player.vy);
      if (speed > 0.5) {
        ctx.strokeStyle = 'rgba(255, 255, 255, 0.5)';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.arc(player.x, player.y, player.radius + 8, 0, Math.PI * 2);
        ctx.stroke();
      }
    });

    const ball = ballRef.current;
    const gradient = ctx.createRadialGradient(ball.x - 3, ball.y - 3, 0, ball.x, ball.y, ball.radius);
    gradient.addColorStop(0, '#ffff88');
    gradient.addColorStop(1, '#ff9900');
    ctx.fillStyle = gradient;
    ctx.beginPath();
    ctx.arc(ball.x, ball.y, ball.radius, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = '#ff6600';
    ctx.lineWidth = 2;
    for (let i = 0; i < 8; i++) {
      const angle = (ball.rotation + (i / 8) * Math.PI * 2) % (Math.PI * 2);
      const x1 = ball.x + Math.cos(angle) * (ball.radius - 2);
      const y1 = ball.y + Math.sin(angle) * (ball.radius - 2);
      const x2 = ball.x + Math.cos(angle) * ball.radius;
      const y2 = ball.y + Math.sin(angle) * ball.radius;
      ctx.beginPath();
      ctx.moveTo(x1, y1);
      ctx.lineTo(x2, y2);
      ctx.stroke();
    }

    const ballSpeed = Math.hypot(ball.vx, ball.vy);
    if (ballSpeed > 1) {
      ctx.strokeStyle = 'rgba(255, 200, 0, 0.4)';
      ctx.lineWidth = 1;
      for (let i = 0; i < 5; i++) {
        const offset = (i + 1) * 3;
        ctx.beginPath();
        ctx.arc(ball.x - ball.vx * offset * 0.3, ball.y - ball.vy * offset * 0.3, ball.radius + offset, 0, Math.PI * 2);
        ctx.stroke();
      }
    }
  };

  const handleCanvasClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const rect = canvas.getBoundingClientRect();
    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    let clickedPlayer: Player | null = null;
    for (const player of playersRef.current) {
      const dist = Math.hypot(player.x - x, player.y - y);
      if (dist < player.radius + 10) {
        clickedPlayer = player;
        break;
      }
    }

    if (clickedPlayer) {
      selectedPlayerRef.current = clickedPlayer.id;
      const angle = Math.atan2(ballRef.current.y - clickedPlayer.y, ballRef.current.x - clickedPlayer.x);
      clickedPlayer.vx = Math.cos(angle) * 5;
      clickedPlayer.vy = Math.sin(angle) * 5;
      playSound(300, 0.1);
    }
  };

  return (
    <div className="w-full h-screen bg-gradient-to-br from-slate-900 to-slate-800 flex flex-col items-center justify-center p-4">
      <style>{`
        @keyframes pulse-glow {
          0%, 100% { text-shadow: 0 0 10px rgba(255, 255, 255, 0.5); }
          50% { text-shadow: 0 0 20px rgba(255, 255, 255, 0.8); }
        }
        @keyframes slide-in {
          from { opacity: 0; transform: translateY(-20px); }
          to { opacity: 1; transform: translateY(0); }
        }
        .score-display { animation: pulse-glow 2s infinite; }
        .header { animation: slide-in 0.6s ease-out; }
      `}</style>

      <div className="header mb-6 text-center">
        <h1 className="text-5xl font-black text-white mb-2">HANDBALL CHAMPIONSHIP</h1>
        <p className="text-blue-300 text-lg">Click on players to control them - move cursor near ball to aim</p>
      </div>

      <div className="flex gap-8 mb-4 items-center">
        <div className="text-center">
          <div className="text-6xl font-black text-red-400 score-display">{homeScore}</div>
          <p className="text-red-300 text-lg font-bold mt-2">HOME</p>
        </div>

        <div className="text-center">
          <div className="text-2xl font-bold text-white bg-slate-700 px-6 py-3 rounded-lg">
            {Math.floor(time / 60)}:{String(Math.floor(time % 60)).padStart(2, '0')}
          </div>
        </div>

        <div className="text-center">
          <div className="text-6xl font-black text-blue-400 score-display">{awayScore}</div>
          <p className="text-blue-300 text-lg font-bold mt-2">AWAY</p>
        </div>
      </div>

      <div className="relative mb-6 border-4 border-white rounded-lg overflow-hidden shadow-2xl">
        <canvas
          ref={canvasRef}
          width={800}
          height={600}
          onClick={handleCanvasClick}
          className="bg-green-900 cursor-crosshair block"
        />
      </div>

      <div className="flex gap-3">
        {gameState === 'idle' && (
          <button
            onClick={startGame}
            className="flex items-center gap-2 px-6 py-3 bg-green-500 hover:bg-green-600 text-white font-bold rounded-lg transition-all transform hover:scale-105"
          >
            <Play size={20} /> START GAME
          </button>
        )}

        {gameState === 'playing' && (
          <button
            onClick={pauseGame}
            className="flex items-center gap-2 px-6 py-3 bg-yellow-500 hover:bg-yellow-600 text-white font-bold rounded-lg transition-all transform hover:scale-105"
          >
            <Pause size={20} /> PAUSE
          </button>
        )}

        {gameState === 'paused' && (
          <button
            onClick={startGame}
            className="flex items-center gap-2 px-6 py-3 bg-green-500 hover:bg-green-600 text-white font-bold rounded-lg transition-all transform hover:scale-105"
          >
            <Play size={20} /> RESUME
          </button>
        )}

        <button
          onClick={resetGame}
          className="flex items-center gap-2 px-6 py-3 bg-red-500 hover:bg-red-600 text-white font-bold rounded-lg transition-all transform hover:scale-105"
        >
          <RotateCcw size={20} /> RESET
        </button>

        <button
          onClick={() => setIsMuted(!isMuted)}
          className="flex items-center gap-2 px-6 py-3 bg-slate-600 hover:bg-slate-700 text-white font-bold rounded-lg transition-all transform hover:scale-105"
        >
          {isMuted ? <VolumeX size={20} /> : <Volume2 size={20} />}
          {isMuted ? 'MUTED' : 'SOUND'}
        </button>
      </div>

      <p className="text-slate-400 text-sm mt-6 text-center">
        Click on red or blue players to control them and aim at the ball for more power!
      </p>
    </div>
  );
};

export default HandballGame;
