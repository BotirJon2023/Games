import { useEffect, useRef, useState } from 'react';
import { GameState } from '../types/game';
import { MinecraftRenderer } from '../utils/renderer';
import { PhysicsEngine } from '../utils/physics';
import { Controls } from '../utils/controls';
import { AIController } from '../utils/ai';
import { createInitialGameState, handleScore } from '../utils/gameState';
import { PhysicsEngine as PhysicsEngineClass } from '../utils/physics';
import { CANVAS_WIDTH, CANVAS_HEIGHT } from '../constants/game';
import { Menu } from './Menu';
import { GameOver } from './GameOver';
import { Pause } from 'lucide-react';

export function VolleyballGame() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [gameMode, setGameMode] = useState<'menu' | 'playing' | 'gameover'>('menu');
  const [selectedMode, setSelectedMode] = useState<'pvp' | 'pvc'>('pvc');
  const [winner, setWinner] = useState<1 | 2 | null>(null);
  const [scores, setScores] = useState({ player1: 0, player2: 0 });
  const gameStateRef = useRef<GameState | null>(null);
  const controlsRef = useRef<Controls | null>(null);
  const aiRef = useRef<AIController | null>(null);
  const animationFrameRef = useRef<number | null>(null);

  useEffect(() => {
    if (gameMode !== 'playing') return;

    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const renderer = new MinecraftRenderer(ctx);
    const physics = new PhysicsEngine();
    const controls = new Controls();
    const ai = new AIController();

    controlsRef.current = controls;
    aiRef.current = ai;

    const gameState = createInitialGameState(selectedMode);
    gameStateRef.current = gameState;

    let lastServeKeyState = false;
    let lastPauseKeyState = false;

    const gameLoop = () => {
      if (!gameStateRef.current) return;

      const state = gameStateRef.current;

      if (!state.isPaused) {
        controls.updatePlayer1(state.player1);

        if (selectedMode === 'pvp') {
          controls.updatePlayer2(state.player2);
        } else {
          ai.updateAI(state.player2, state.ball);
        }

        physics.updatePlayerPhysics(state.player1);
        physics.updatePlayerPhysics(state.player2);

        const { hitGround, side } = physics.updateBallPhysics(state.ball);

        physics.checkBallPlayerCollision(state.ball, state.player1);
        physics.checkBallPlayerCollision(state.ball, state.player2);

        if (hitGround && side) {
          const result = handleScore(state, side);
          setScores({ player1: state.player1.score, player2: state.player2.score });

          if (result.winner) {
            setWinner(result.winner);
            setGameMode('gameover');
            return;
          }
        }

        const spacePressed = controls.isSpacePressed();
        if (spacePressed && !lastServeKeyState && state.ball.isServing) {
          const servingPlayer = state.servingPlayer === 1 ? state.player1 : state.player2;
          const direction = state.servingPlayer === 1 ? 1 : -1;
          physics.serveBall(state.ball, servingPlayer.position.x, direction);
        }
        lastServeKeyState = spacePressed;
      }

      const pausePressed = controls.isPausePressed();
      if (pausePressed && !lastPauseKeyState) {
        state.isPaused = !state.isPaused;
      }
      lastPauseKeyState = pausePressed;

      renderer.clear();
      renderer.drawGround();
      renderer.drawNet();
      renderer.drawBlockyPlayer(state.player1);
      renderer.drawBlockyPlayer(state.player2);
      renderer.drawBlockyBall(state.ball);
      renderer.drawScore(state.player1.score, state.player2.score);

      if (state.ball.isServing) {
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.font = 'bold 24px monospace';
        ctx.textAlign = 'center';
        ctx.fillText('PRESS SPACE TO SERVE', CANVAS_WIDTH / 2, 150);
      }

      if (state.isPaused) {
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
        ctx.fillStyle = 'white';
        ctx.font = 'bold 48px monospace';
        ctx.textAlign = 'center';
        ctx.fillText('PAUSED', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2);
        ctx.font = 'bold 20px monospace';
        ctx.fillText('Press ESC or P to resume', CANVAS_WIDTH / 2, CANVAS_HEIGHT / 2 + 50);
      }

      animationFrameRef.current = requestAnimationFrame(gameLoop);
    };

    gameLoop();

    return () => {
      if (animationFrameRef.current) {
        cancelAnimationFrame(animationFrameRef.current);
      }
      controls.cleanup();
    };
  }, [gameMode, selectedMode]);

  const handleSelectMode = (mode: 'pvp' | 'pvc') => {
    setSelectedMode(mode);
    setGameMode('playing');
    setWinner(null);
    setScores({ player1: 0, player2: 0 });
  };

  const handleRestart = () => {
    setGameMode('playing');
    setWinner(null);
    setScores({ player1: 0, player2: 0 });
  };

  const handleMenu = () => {
    setGameMode('menu');
    setWinner(null);
    setScores({ player1: 0, player2: 0 });
  };

  const handlePause = () => {
    if (gameStateRef.current) {
      gameStateRef.current.isPaused = !gameStateRef.current.isPaused;
    }
  };

  return (
    <div className="relative w-full h-screen bg-gray-900 flex items-center justify-center">
      <div className="relative">
        <canvas
          ref={canvasRef}
          width={CANVAS_WIDTH}
          height={CANVAS_HEIGHT}
          className="border-4 border-gray-800 shadow-2xl"
        />

        {gameMode === 'playing' && (
          <button
            onClick={handlePause}
            className="absolute top-4 right-4 bg-gray-800 hover:bg-gray-700 text-white p-2 rounded-lg shadow-lg transition-all"
          >
            <Pause size={24} />
          </button>
        )}
      </div>

      {gameMode === 'menu' && <Menu onSelectMode={handleSelectMode} />}

      {gameMode === 'gameover' && winner && (
        <GameOver
          winner={winner}
          player1Score={scores.player1}
          player2Score={scores.player2}
          onRestart={handleRestart}
          onMenu={handleMenu}
        />
      )}
    </div>
  );
}
