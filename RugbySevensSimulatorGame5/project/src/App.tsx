import React, { useState, useEffect, useCallback } from 'react';
import { GameEngine } from './utils/gameEngine';
import { DEFAULT_TEAMS } from './utils/teamGenerator';
import { GameState } from './types/game';
import { GameField } from './components/GameField';
import { GameControls } from './components/GameControls';
import { ScoreBoard } from './components/ScoreBoard';
import { GameLog } from './components/GameLog';
import { TeamStats } from './components/TeamStats';

function App() {
  const [gameEngine, setGameEngine] = useState<GameEngine | null>(null);
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [gameUpdateInterval, setGameUpdateInterval] = useState<NodeJS.Timeout | null>(null);

  const initializeGame = useCallback(() => {
    const engine = new GameEngine(DEFAULT_TEAMS.home, DEFAULT_TEAMS.away);
    setGameEngine(engine);
    setGameState(engine.getGameState());
  }, []);

  useEffect(() => {
    initializeGame();
  }, [initializeGame]);

  useEffect(() => {
    if (gameEngine && gameState?.isPlaying) {
      const interval = setInterval(() => {
        setGameState(gameEngine.getGameState());
      }, 100);
      setGameUpdateInterval(interval);
      
      return () => {
        if (interval) clearInterval(interval);
      };
    } else if (gameUpdateInterval) {
      clearInterval(gameUpdateInterval);
      setGameUpdateInterval(null);
    }
  }, [gameEngine, gameState?.isPlaying]);

  const handleStart = () => {
    if (gameEngine) {
      const newState = gameEngine.startGame();
      setGameState(newState);
    }
  };

  const handlePause = () => {
    if (gameEngine) {
      const newState = gameEngine.pauseGame();
      setGameState(newState);
    }
  };

  const handleReset = () => {
    if (gameUpdateInterval) {
      clearInterval(gameUpdateInterval);
      setGameUpdateInterval(null);
    }
    initializeGame();
  };

  const handleSettings = () => {
    alert('Settings panel coming soon! This would allow you to customize teams, game duration, and other settings.');
  };

  if (!gameState) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-400 to-blue-600 flex items-center justify-center">
        <div className="text-white text-xl font-bold">Loading Rugby Sevens Simulator...</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-400 via-blue-500 to-purple-600 p-4">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <div className="text-center mb-6">
          <h1 className="text-4xl font-bold text-white mb-2 shadow-text">
            Rugby Sevens Simulator
          </h1>
          <p className="text-white/80 text-lg">
            Experience the intensity of professional rugby sevens action
          </p>
        </div>

        {/* Score Board */}
        <div className="mb-6">
          <ScoreBoard gameState={gameState} />
        </div>

        {/* Game Controls */}
        <div className="mb-6 flex justify-center">
          <GameControls
            isPlaying={gameState.isPlaying}
            onStart={handleStart}
            onPause={handlePause}
            onReset={handleReset}
            onSettings={handleSettings}
          />
        </div>

        {/* Main Game Area */}
        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          {/* Game Field */}
          <div className="lg:col-span-2">
            <div className="bg-white p-4 rounded-lg shadow-2xl">
              <GameField gameState={gameState} />
            </div>
          </div>

          {/* Side Panels */}
          <div className="space-y-6">
            <TeamStats team={gameState.homeTeam} title={gameState.homeTeam.name} />
            <TeamStats team={gameState.awayTeam} title={gameState.awayTeam.name} />
          </div>

          {/* Game Log */}
          <div>
            <GameLog events={gameState.gameLog} />
          </div>
        </div>

        {/* Footer */}
        <div className="mt-8 text-center text-white/60 text-sm">
          <p>Rugby Sevens Simulator - Experience the thrill of professional rugby action</p>
          <p className="mt-1">Built with React, TypeScript, and Tailwind CSS</p>
        </div>
      </div>

      <style jsx>{`
        .shadow-text {
          text-shadow: 2px 2px 4px rgba(0, 0, 0, 0.3);
        }
      `}</style>
    </div>
  );
}

export default App;