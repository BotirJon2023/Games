import React from 'react';
import { Difficulty, GameState, GameScore } from '../types/game';
import { Play, Pause, RotateCcw, Volume2, VolumeX } from 'lucide-react';

interface GameUIProps {
  gameState: GameState;
  gameScore: GameScore;
  difficulty: Difficulty;
  isPaused: boolean;
  onStartGame: (difficulty: Difficulty) => void;
  onPauseToggle: () => void;
  onRestart: () => void;
  onSelectDifficulty: (difficulty: Difficulty) => void;
  soundEnabled: boolean;
  onToggleSound: () => void;
}

export const GameUI: React.FC<GameUIProps> = ({
  gameState,
  gameScore,
  difficulty,
  isPaused,
  onStartGame,
  onPauseToggle,
  onRestart,
  onSelectDifficulty,
  soundEnabled,
  onToggleSound,
}) => {
  return (
    <div className="flex flex-col items-center gap-6 w-full">
      {/* Score Display */}
      {(gameState === 'playing' || gameState === 'paused') && (
        <div className="flex justify-between items-center w-full px-4">
          <div className="text-center flex-1">
            <div className="text-6xl font-bold text-cyan-400 drop-shadow-lg">
              {gameScore.player}
            </div>
            <p className="text-sm text-gray-400 mt-1">YOU</p>
          </div>

          <div className="flex flex-col items-center gap-2 px-4">
            <p className="text-xs text-gray-500">Rallies</p>
            <p className="text-lg font-semibold text-gray-400">{gameScore.rallies}</p>
            <p className="text-xs text-gray-500">{difficulty.toUpperCase()}</p>
          </div>

          <div className="text-center flex-1">
            <div className="text-6xl font-bold text-pink-500 drop-shadow-lg">
              {gameScore.computer}
            </div>
            <p className="text-sm text-gray-400 mt-1">AI</p>
          </div>
        </div>
      )}

      {/* Menu */}
      {gameState === 'menu' && (
        <div className="text-center space-y-8 w-full">
          <div className="space-y-2">
            <h1 className="text-5xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-pink-500">
              TABLE TENNIS
            </h1>
            <p className="text-gray-400">Challenge the AI • Master the Paddle</p>
          </div>

          <button
            onClick={() => onSelectDifficulty('easy')}
            className="w-full max-w-xs mx-auto block px-6 py-3 bg-gradient-to-r from-cyan-500 to-cyan-600 hover:from-cyan-400 hover:to-cyan-500 text-white font-semibold rounded-lg transition-all transform hover:scale-105 active:scale-95 shadow-lg"
          >
            START GAME
          </button>

          <div className="space-y-2 text-sm text-gray-400">
            <p>Use mouse to move your paddle</p>
            <p>Click SPACE to pause</p>
          </div>
        </div>
      )}

      {/* Difficulty Selection */}
      {gameState === 'difficulty' && (
        <div className="text-center space-y-6 w-full">
          <h2 className="text-3xl font-bold text-white">Select Difficulty</h2>

          <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 max-w-md mx-auto">
            {(['easy', 'medium', 'hard'] as const).map((level) => (
              <button
                key={level}
                onClick={() => onStartGame(level)}
                className={`px-6 py-4 rounded-lg font-semibold transition-all transform hover:scale-105 active:scale-95 ${
                  difficulty === level
                    ? 'bg-gradient-to-r from-cyan-500 to-cyan-600 text-white shadow-lg'
                    : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                }`}
              >
                {level.charAt(0).toUpperCase() + level.slice(1)}
              </button>
            ))}
          </div>

          <p className="text-xs text-gray-500">
            {difficulty === 'easy' && 'AI moves slowly - Perfect for beginners'}
            {difficulty === 'medium' && 'Balanced gameplay - Good challenge'}
            {difficulty === 'hard' && 'AI is very fast - Expert only!'}
          </p>
        </div>
      )}

      {/* Game Over */}
      {gameState === 'gameOver' && (
        <div className="text-center space-y-6 w-full">
          <div className="space-y-2">
            <h2 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-cyan-400 to-pink-500">
              {gameScore.player > gameScore.computer ? 'YOU WIN!' : 'GAME OVER!'}
            </h2>
            <p className="text-xl text-gray-300">
              Final Score: {gameScore.player} - {gameScore.computer}
            </p>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 max-w-md mx-auto">
            <button
              onClick={() => onRestart()}
              className="px-6 py-3 bg-gradient-to-r from-cyan-500 to-cyan-600 hover:from-cyan-400 hover:to-cyan-500 text-white font-semibold rounded-lg transition-all transform hover:scale-105 active:scale-95 shadow-lg flex items-center justify-center gap-2"
            >
              <RotateCcw size={18} /> Retry
            </button>

            <button
              onClick={() => onSelectDifficulty('easy')}
              className="px-6 py-3 bg-gradient-to-r from-pink-500 to-pink-600 hover:from-pink-400 hover:to-pink-500 text-white font-semibold rounded-lg transition-all transform hover:scale-105 active:scale-95 shadow-lg"
            >
              Back to Menu
            </button>
          </div>
        </div>
      )}

      {/* Controls */}
      {(gameState === 'playing' || gameState === 'paused') && (
        <div className="flex gap-3 justify-center flex-wrap">
          <button
            onClick={onPauseToggle}
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-all flex items-center gap-2 shadow-lg"
            title="Press SPACE to toggle"
          >
            {isPaused ? (
              <>
                <Play size={18} /> Resume
              </>
            ) : (
              <>
                <Pause size={18} /> Pause
              </>
            )}
          </button>

          <button
            onClick={onToggleSound}
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-all flex items-center gap-2 shadow-lg"
          >
            {soundEnabled ? (
              <>
                <Volume2 size={18} /> On
              </>
            ) : (
              <>
                <VolumeX size={18} /> Off
              </>
            )}
          </button>

          <button
            onClick={() => onRestart()}
            className="px-4 py-2 bg-gray-700 hover:bg-gray-600 text-white rounded-lg transition-all flex items-center gap-2 shadow-lg"
          >
            <RotateCcw size={18} /> New Game
          </button>
        </div>
      )}

      {/* Pause Overlay */}
      {isPaused && gameState === 'playing' && (
        <div className="fixed inset-0 bg-black bg-opacity-60 flex items-center justify-center z-50">
          <div className="text-center space-y-6 bg-gray-900 bg-opacity-95 p-8 rounded-lg border border-cyan-500">
            <h2 className="text-4xl font-bold text-cyan-400">PAUSED</h2>
            <p className="text-gray-400">Press SPACE or click Resume to continue</p>
            <button
              onClick={onPauseToggle}
              className="px-8 py-3 bg-gradient-to-r from-cyan-500 to-cyan-600 hover:from-cyan-400 hover:to-cyan-500 text-white font-semibold rounded-lg transition-all transform hover:scale-105 active:scale-95"
            >
              Resume Game
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
