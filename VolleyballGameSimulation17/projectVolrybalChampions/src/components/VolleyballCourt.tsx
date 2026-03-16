import { useEffect, useRef, useState } from 'react';
import { VolleyballGame, GameState } from '../lib/courtGame';
import { Pause, Play, RotateCcw } from 'lucide-react';

interface VolleyballCourtProps {
  team1Id: string;
  team2Id: string;
  team1Name: string;
  team2Name: string;
  team1Color: string;
  team2Color: string;
  onMatchEnd: (winnerId: string, team1Score: number, team2Score: number) => void;
  gameMode: 'vs-ai' | 'two-player';
}

export default function VolleyballCourt({
  team1Id,
  team2Id,
  team1Name,
  team2Name,
  team1Color,
  team2Color,
  onMatchEnd,
  gameMode,
}: VolleyballCourtProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const gameRef = useRef<VolleyballGame | null>(null);
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [isPaused, setIsPaused] = useState(false);
  const animationRef = useRef<number>();
  const keysPressed = useRef<{ [key: string]: boolean }>({});

  const COURT_WIDTH = 800;
  const COURT_HEIGHT = 400;
  const NET_HEIGHT = 150;

  useEffect(() => {
    gameRef.current = new VolleyballGame(
      team1Id,
      team2Id,
      setGameState,
      false,
      gameMode === 'vs-ai'
    );

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
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [team1Id, team2Id, gameMode]);

  useEffect(() => {
    const gameLoop = () => {
      if (!isPaused && gameRef.current) {
        if (keysPressed.current['w'] || keysPressed.current['arrowup']) {
          gameRef.current.updatePlayerPosition('player1', -6);
        }
        if (keysPressed.current['s'] || keysPressed.current['arrowdown']) {
          gameRef.current.updatePlayerPosition('player1', 6);
        }
        if (gameMode === 'two-player') {
          if (keysPressed.current['i']) {
            gameRef.current.updatePlayerPosition('player2', -6);
          }
          if (keysPressed.current['k']) {
            gameRef.current.updatePlayerPosition('player2', 6);
          }
        }

        gameRef.current.update();
        const state = gameRef.current.getGameState();

        if (state.isGameOver) {
          if (state.winner && gameRef.current) {
            gameRef.current.stop();
            onMatchEnd(state.winner, state.team1Score, state.team2Score);
          }
        }
      }

      animationRef.current = requestAnimationFrame(gameLoop);
    };

    animationRef.current = requestAnimationFrame(gameLoop);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [isPaused, gameMode, onMatchEnd]);

  useEffect(() => {
    if (!canvasRef.current || !gameState) return;

    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.fillStyle = '#1a472a';
    ctx.fillRect(0, 0, COURT_WIDTH, COURT_HEIGHT);

    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.setLineDash([10, 10]);
    ctx.moveTo(0, NET_HEIGHT);
    ctx.lineTo(COURT_WIDTH, NET_HEIGHT);
    ctx.stroke();
    ctx.setLineDash([]);

    ctx.fillStyle = '#ffffff';
    ctx.lineWidth = 2;
    ctx.strokeRect(10, 10, COURT_WIDTH / 2 - 15, COURT_HEIGHT - 20);
    ctx.strokeRect(COURT_WIDTH / 2 + 5, 10, COURT_WIDTH / 2 - 15, COURT_HEIGHT - 20);

    for (let i = 0; i < COURT_HEIGHT; i += 30) {
      ctx.fillRect(COURT_WIDTH / 2 - 1, i, 2, 20);
    }

    ctx.fillStyle = '#ff6b6b';
    ctx.fillRect(
      gameState.player1.x,
      gameState.player1.y,
      gameState.player1.width,
      gameState.player1.height
    );

    ctx.fillStyle = '#4ecdc4';
    ctx.fillRect(
      gameState.player2.x,
      gameState.player2.y,
      gameState.player2.width,
      gameState.player2.height
    );

    ctx.fillStyle = '#ffd700';
    ctx.beginPath();
    ctx.arc(gameState.ball.x, gameState.ball.y, gameState.ball.radius, 0, Math.PI * 2);
    ctx.fill();
    ctx.strokeStyle = '#ffffff';
    ctx.lineWidth = 1;
    ctx.stroke();
  }, [gameState]);

  const handleStartGame = () => {
    if (gameRef.current && !gameState?.gameStarted) {
      gameRef.current.startGame();
    }
  };

  const handleReset = () => {
    gameRef.current = new VolleyballGame(
      team1Id,
      team2Id,
      setGameState,
      false,
      gameMode === 'vs-ai'
    );
    setGameState(gameRef.current.getGameState());
    setIsPaused(false);
  };

  return (
    <div className="w-full max-w-2xl mx-auto p-6 bg-white rounded-lg shadow-lg">
      <div className="mb-4 flex justify-between items-center">
        <div className="text-center flex-1">
          <h3 className="font-bold text-lg" style={{ color: team1Color }}>
            {team1Name}
          </h3>
          <p className="text-3xl font-bold">{gameState?.team1Score || 0}</p>
          <p className="text-sm text-gray-600">Sets: {gameState?.team1Sets || 0}</p>
        </div>
        <div className="px-4 text-center">
          <p className="text-sm text-gray-600">Set {gameState?.currentSet || 1}</p>
        </div>
        <div className="text-center flex-1">
          <h3 className="font-bold text-lg" style={{ color: team2Color }}>
            {team2Name}
          </h3>
          <p className="text-3xl font-bold">{gameState?.team2Score || 0}</p>
          <p className="text-sm text-gray-600">Sets: {gameState?.team2Sets || 0}</p>
        </div>
      </div>

      <canvas
        ref={canvasRef}
        width={COURT_WIDTH}
        height={COURT_HEIGHT}
        className="w-full border-4 border-gray-800 rounded-lg mb-6 bg-green-900"
      />

      <div className="mb-6 text-center">
        {!gameState?.gameStarted ? (
          <button
            onClick={handleStartGame}
            className="flex items-center justify-center gap-2 px-6 py-3 bg-blue-500 text-white font-bold rounded-lg hover:bg-blue-600 transition mx-auto"
          >
            <Play className="w-5 h-5" />
            Start Game
          </button>
        ) : gameState?.isGameOver ? (
          <div className="text-2xl font-bold text-green-600 mb-4">
            {gameState.winner === team1Id ? team1Name : team2Name} Wins!
          </div>
        ) : (
          <div className="flex justify-center gap-4">
            <button
              onClick={() => setIsPaused(!isPaused)}
              className="flex items-center gap-2 px-4 py-2 bg-yellow-500 text-white font-bold rounded-lg hover:bg-yellow-600 transition"
            >
              {isPaused ? <Play className="w-4 h-4" /> : <Pause className="w-4 h-4" />}
              {isPaused ? 'Resume' : 'Pause'}
            </button>
            <button
              onClick={handleReset}
              className="flex items-center gap-2 px-4 py-2 bg-gray-500 text-white font-bold rounded-lg hover:bg-gray-600 transition"
            >
              <RotateCcw className="w-4 h-4" />
              Reset
            </button>
          </div>
        )}
      </div>

      <div className="bg-gray-100 p-4 rounded-lg text-sm">
        <p className="mb-2 font-semibold">Controls:</p>
        <p>
          <span className="font-bold">Player 1 (Red):</span> W/S or Arrow Up/Down to move
        </p>
        {gameMode === 'two-player' && (
          <p>
            <span className="font-bold">Player 2 (Cyan):</span> I/K to move
          </p>
        )}
        {gameMode === 'vs-ai' && (
          <p>
            <span className="font-bold">Player 2 (Cyan):</span> AI controlled
          </p>
        )}
      </div>
    </div>
  );
}
