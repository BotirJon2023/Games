import React from 'react';
import { Play, Pause, RotateCcw, Settings } from 'lucide-react';

interface GameControlsProps {
  isPlaying: boolean;
  onStart: () => void;
  onPause: () => void;
  onReset: () => void;
  onSettings: () => void;
}

export const GameControls: React.FC<GameControlsProps> = ({
  isPlaying,
  onStart,
  onPause,
  onReset,
  onSettings
}) => {
  return (
    <div className="flex items-center gap-4 p-4 bg-white rounded-lg shadow-lg border">
      <button
        onClick={isPlaying ? onPause : onStart}
        className={`flex items-center gap-2 px-6 py-3 rounded-lg font-semibold transition-all duration-200 transform hover:scale-105 ${
          isPlaying
            ? 'bg-red-500 hover:bg-red-600 text-white'
            : 'bg-green-500 hover:bg-green-600 text-white'
        }`}
      >
        {isPlaying ? <Pause size={20} /> : <Play size={20} />}
        {isPlaying ? 'Pause' : 'Start'}
      </button>

      <button
        onClick={onReset}
        className="flex items-center gap-2 px-4 py-3 bg-blue-500 hover:bg-blue-600 text-white rounded-lg font-semibold transition-all duration-200 transform hover:scale-105"
      >
        <RotateCcw size={20} />
        Reset
      </button>

      <button
        onClick={onSettings}
        className="flex items-center gap-2 px-4 py-3 bg-gray-500 hover:bg-gray-600 text-white rounded-lg font-semibold transition-all duration-200 transform hover:scale-105"
      >
        <Settings size={20} />
        Settings
      </button>
    </div>
  );
};