import React from 'react';
import { Play, RotateCcw, Settings, Target } from 'lucide-react';

interface GameControlsProps {
  onStartPitch: () => void;
  onSwing: () => void;
  onResetCount: () => void;
  onNewGame: () => void;
  onSetDifficulty: (difficulty: 'easy' | 'medium' | 'hard') => void;
  difficulty: string;
  pitchInProgress: boolean;
  gameOver: boolean;
  canSwing: boolean;
}

export const GameControls: React.FC<GameControlsProps> = ({
  onStartPitch,
  onSwing,
  onResetCount,
  onNewGame,
  onSetDifficulty,
  difficulty,
  pitchInProgress,
  gameOver,
  canSwing,
}) => {
  return (
    <div className="bg-white p-6 rounded-xl shadow-2xl border-2 border-gray-300">
      <h3 className="text-2xl font-bold text-gray-800 mb-6 text-center">Game Controls</h3>
      
      {/* Main Action Buttons */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <button
          onClick={onStartPitch}
          disabled={pitchInProgress || gameOver}
          className={`flex items-center justify-center px-6 py-4 rounded-lg font-bold text-lg transition-all duration-200 ${
            pitchInProgress || gameOver
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
              : 'bg-blue-600 text-white hover:bg-blue-700 transform hover:scale-105 shadow-lg'
          }`}
        >
          <Play className="mr-2" size={24} />
          Start Pitching
        </button>

        <button
          onClick={onSwing}
          disabled={!canSwing}
          className={`flex items-center justify-center px-6 py-4 rounded-lg font-bold text-lg transition-all duration-200 ${
            !canSwing
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
              : 'bg-green-600 text-white hover:bg-green-700 transform hover:scale-105 shadow-lg'
          }`}
        >
          <Target className="mr-2" size={24} />
          Swing!
        </button>
      </div>

      {/* Secondary Controls */}
      <div className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
        <button
          onClick={onResetCount}
          className="flex items-center justify-center px-4 py-3 rounded-lg font-semibold bg-yellow-600 text-white hover:bg-yellow-700 transition-all duration-200 transform hover:scale-105"
        >
          <RotateCcw className="mr-2" size={20} />
          New At-Bat
        </button>

        <button
          onClick={onNewGame}
          className="flex items-center justify-center px-4 py-3 rounded-lg font-semibold bg-red-600 text-white hover:bg-red-700 transition-all duration-200 transform hover:scale-105"
        >
          <Settings className="mr-2" size={20} />
          New Game
        </button>
      </div>

      {/* Difficulty Selection */}
      <div className="border-t pt-4">
        <label className="block text-sm font-medium text-gray-700 mb-3">
          Select Difficulty Level:
        </label>
        <div className="grid grid-cols-3 gap-2">
          {(['easy', 'medium', 'hard'] as const).map((level) => (
            <button
              key={level}
              onClick={() => onSetDifficulty(level)}
              className={`py-2 px-4 rounded-lg font-semibold text-sm transition-all duration-200 ${
                difficulty === level
                  ? level === 'easy'
                    ? 'bg-green-600 text-white shadow-lg'
                    : level === 'medium'
                    ? 'bg-yellow-600 text-white shadow-lg'
                    : 'bg-red-600 text-white shadow-lg'
                  : 'bg-gray-200 text-gray-700 hover:bg-gray-300'
              }`}
            >
              {level.charAt(0).toUpperCase() + level.slice(1)}
            </button>
          ))}
        </div>
      </div>

      {/* Game Instructions */}
      <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-200">
        <h4 className="font-semibold text-blue-900 mb-2">How to Play:</h4>
        <ul className="text-sm text-blue-800 space-y-1">
          <li>• Click "Start Pitching" to begin each pitch</li>
          <li>• Click "Swing!" when the ball is in the strike zone</li>
          <li>• Timing is crucial - swing at the right moment!</li>
          <li>• Try to get hits and avoid strikeouts</li>
        </ul>
      </div>
    </div>
  );
};