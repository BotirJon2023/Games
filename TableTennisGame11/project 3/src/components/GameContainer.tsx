import React, { useState, useEffect, useCallback, useRef } from 'react';
import { GameData, GameState, Difficulty, Ball, Paddle, Particle } from '../types/game';
import { GameCanvas } from './GameCanvas';
import { GameUI } from './GameUI';
import {
  createBall,
  createPlayerPaddle,
  createComputerPaddle,
  updateBallPhysics,
  updatePaddlePosition,
  getComputerAITarget,
  createParticles,
  updateParticles,
  checkGameOver,
} from '../utils/gamePhysics';

const CANVAS_WIDTH = 800;
const CANVAS_HEIGHT = 600;

export const GameContainer: React.FC = () => {
  const [gameData, setGameData] = useState<GameData>({
    state: 'menu',
    difficulty: 'medium',
    score: { player: 0, computer: 0, rallies: 0 },
    playerPaddle: createPlayerPaddle(),
    computerPaddle: createComputerPaddle(),
    ball: createBall(),
    particles: [],
    gameTime: 0,
    isPaused: false,
  });

  const [playerPaddleY, setPlayerPaddleY] = useState(CANVAS_HEIGHT / 2);
  const [soundEnabled, setSoundEnabled] = useState(true);
  const gameLoopRef = useRef<number>();
  const lastRallyRef = useRef<number>(0);
  const soundsRef = useRef<{ [key: string]: boolean }>({});

  const playSound = useCallback((type: 'paddle' | 'wall' | 'score') => {
    if (!soundEnabled) return;

    const context = new (window.AudioContext || (window as any).webkitAudioContext)();
    const now = context.currentTime;

    switch (type) {
      case 'paddle': {
        const osc = context.createOscillator();
        const gain = context.createGain();
        osc.connect(gain);
        gain.connect(context.destination);
        osc.frequency.setValueAtTime(600, now);
        osc.frequency.setValueAtTime(800, now + 0.05);
        gain.gain.setValueAtTime(0.2, now);
        gain.gain.setValueAtTime(0, now + 0.05);
        osc.start(now);
        osc.stop(now + 0.05);
        break;
      }
      case 'wall': {
        const osc = context.createOscillator();
        const gain = context.createGain();
        osc.connect(gain);
        gain.connect(context.destination);
        osc.frequency.setValueAtTime(400, now);
        osc.frequency.setValueAtTime(300, now + 0.03);
        gain.gain.setValueAtTime(0.15, now);
        gain.gain.setValueAtTime(0, now + 0.03);
        osc.start(now);
        osc.stop(now + 0.03);
        break;
      }
      case 'score': {
        const osc = context.createOscillator();
        const gain = context.createGain();
        osc.connect(gain);
        gain.connect(context.destination);
        osc.frequency.setValueAtTime(1000, now);
        osc.frequency.setValueAtTime(1200, now + 0.1);
        gain.gain.setValueAtTime(0.25, now);
        gain.gain.setValueAtTime(0, now + 0.1);
        osc.start(now);
        osc.stop(now + 0.1);
        break;
      }
    }
  }, [soundEnabled]);

  const startGame = useCallback((difficulty: Difficulty) => {
    setGameData((prev) => ({
      ...prev,
      state: 'playing',
      difficulty,
      score: { player: 0, computer: 0, rallies: 0 },
      playerPaddle: createPlayerPaddle(),
      computerPaddle: createComputerPaddle(),
      ball: createBall(),
      particles: [],
      gameTime: 0,
      isPaused: false,
    }));
    lastRallyRef.current = 0;
  }, []);

  const selectDifficulty = useCallback((difficulty: Difficulty) => {
    setGameData((prev) => ({
      ...prev,
      state: 'difficulty',
      difficulty,
    }));
  }, []);

  const togglePause = useCallback(() => {
    setGameData((prev) =>
      prev.state === 'playing'
        ? { ...prev, isPaused: !prev.isPaused }
        : prev
    );
  }, []);

  const restartGame = useCallback(() => {
    startGame(gameData.difficulty);
  }, [gameData.difficulty, startGame]);

  const handleMouseMove = useCallback((y: number) => {
    const canvasTop = 0;
    const relativeY = y - canvasTop;
    setPlayerPaddleY(relativeY);
  }, []);

  useEffect(() => {
    const handleKeyPress = (e: KeyboardEvent) => {
      if (e.code === 'Space') {
        e.preventDefault();
        if (gameData.state === 'playing') {
          togglePause();
        }
      }
    };

    window.addEventListener('keydown', handleKeyPress);
    return () => window.removeEventListener('keydown', handleKeyPress);
  }, [gameData.state, togglePause]);

  useEffect(() => {
    if (gameData.state !== 'playing' || gameData.isPaused) {
      return;
    }

    const gameLoop = () => {
      setGameData((prevGameData) => {
        const newGameData = { ...prevGameData };

        // Update ball physics
        const { newBall, hitPaddle } = updateBallPhysics(
          newGameData.ball,
          newGameData.playerPaddle,
          newGameData.computerPaddle,
          newGameData.difficulty,
          (type) => {
            playSound(type === 'paddle' ? 'paddle' : 'wall');
          },
          (player) => {
            playSound('score');
            if (player === 'player') {
              newGameData.score.player += 1;
            } else {
              newGameData.score.computer += 1;
            }

            if (checkGameOver(newGameData.score)) {
              newGameData.state = 'gameOver';
            }
          }
        );

        newGameData.ball = newBall;

        // Create particles on paddle hit
        if (hitPaddle) {
          const newParticles = createParticles(
            newGameData.ball.x,
            newGameData.ball.y,
            'spark',
            newGameData.ball.velocityX,
            newGameData.ball.velocityY
          );
          newGameData.particles.push(...newParticles);
        }

        // Trail particles
        if (Math.random() > 0.7) {
          const trailParticles = createParticles(
            newGameData.ball.x,
            newGameData.ball.y,
            'trail'
          );
          newGameData.particles.push(...trailParticles);
        }

        // Update paddle positions
        newGameData.playerPaddle = updatePaddlePosition(
          newGameData.playerPaddle,
          playerPaddleY,
          newGameData.difficulty
        );

        const computerTarget = getComputerAITarget(
          newGameData.ball,
          newGameData.computerPaddle,
          newGameData.difficulty
        );

        newGameData.computerPaddle = updatePaddlePosition(
          newGameData.computerPaddle,
          computerTarget,
          newGameData.difficulty
        );

        // Track rallies
        if (
          newGameData.ball.velocityX === 0 &&
          newGameData.ball.velocityY === 0
        ) {
          lastRallyRef.current = 0;
        } else if (lastRallyRef.current === 0) {
          lastRallyRef.current = 1;
          newGameData.score.rallies += 1;
        }

        // Update particles
        newGameData.particles = updateParticles(newGameData.particles);

        // Update game time
        newGameData.gameTime += 1 / 60;

        return newGameData;
      });

      gameLoopRef.current = requestAnimationFrame(gameLoop);
    };

    gameLoopRef.current = requestAnimationFrame(gameLoop);

    return () => {
      if (gameLoopRef.current) {
        cancelAnimationFrame(gameLoopRef.current);
      }
    };
  }, [gameData.state, gameData.isPaused, playSound, playerPaddleY]);

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-950 flex flex-col items-center justify-center p-4 gap-6">
      <GameCanvas gameData={gameData} onMouseMove={handleMouseMove} />

      <GameUI
        gameState={gameData.state}
        gameScore={gameData.score}
        difficulty={gameData.difficulty}
        isPaused={gameData.isPaused}
        onStartGame={startGame}
        onPauseToggle={togglePause}
        onRestart={restartGame}
        onSelectDifficulty={selectDifficulty}
        soundEnabled={soundEnabled}
        onToggleSound={() => setSoundEnabled(!soundEnabled)}
      />

      {gameData.state === 'playing' && (
        <div className="text-xs text-gray-500 mt-4">
          FPS: {Math.round(1000 / 16)} | Ball Speed: {gameData.ball.speed.toFixed(1)}
        </div>
      )}
    </div>
  );
};
