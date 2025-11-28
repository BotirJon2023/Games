import React, { useEffect, useRef, useState } from 'react';
import { GameEngine } from '../engine/GameEngine';
import { GameState } from '../types/GameTypes';
import Menu from './Menu';
import HUD from './HUD';
import FinishScreen from './FinishScreen';

const RacingGame: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const gameEngineRef = useRef<GameEngine | null>(null);
  const [gameState, setGameState] = useState<GameState>('MENU');
  const [playerPosition, setPlayerPosition] = useState(1);
  const [playerLap, setPlayerLap] = useState(0);
  const [playerSpeed, setPlayerSpeed] = useState(0);
  const [isPaused, setIsPaused] = useState(false);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const gameEngine = new GameEngine(canvas);
    gameEngineRef.current = gameEngine;

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === ' ' && gameState === 'MENU') {
        e.preventDefault();
        setGameState('RACING');
        gameEngine.startRace();
      } else if (e.key === 'p' || e.key === 'P') {
        e.preventDefault();
        if (gameState === 'RACING') {
          setIsPaused(!isPaused);
          gameEngine.setPaused(!isPaused);
        }
      } else if (e.key === 'r' || e.key === 'R') {
        if (gameState === 'FINISHED') {
          setGameState('RACING');
          setPlayerPosition(1);
          setPlayerLap(0);
          setPlayerSpeed(0);
          setIsPaused(false);
          gameEngine.reset();
          gameEngine.startRace();
        }
      } else {
        gameEngine.handleKeyDown(e.key);
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      gameEngine.handleKeyUp(e.key);
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    const gameLoop = setInterval(() => {
      if (gameState === 'RACING' && !isPaused) {
        gameEngine.update();
        setPlayerPosition(gameEngine.getPlayerPosition());
        setPlayerLap(gameEngine.getPlayerLap());
        setPlayerSpeed(gameEngine.getPlayerSpeed());

        if (gameEngine.isRaceFinished()) {
          setGameState('FINISHED');
        }
      }

      gameEngine.render(gameState, isPaused);
    }, 1000 / 60);

    return () => {
      clearInterval(gameLoop);
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [gameState, isPaused]);

  return (
    <div className="w-full h-screen flex flex-col items-center justify-center bg-slate-900">
      <canvas
        ref={canvasRef}
        width={1200}
        height={800}
        className="border-4 border-yellow-500 shadow-2xl"
      />

      {gameState === 'MENU' && <Menu />}
      {gameState === 'RACING' && (
        <HUD
          position={playerPosition}
          lap={playerLap}
          speed={playerSpeed}
          isPaused={isPaused}
        />
      )}
      {gameState === 'FINISHED' && <FinishScreen position={playerPosition} />}
    </div>
  );
};

export default RacingGame;
