import React from 'react';
import { Clock, Play, Pause, Square } from 'lucide-react';

interface GameTimerProps {
  timeRemaining: number;
  isActive: boolean;
  isPaused: boolean;
  formatTime: (seconds: number) => string;
  onStart: () => void;
  onPause: () => void;
  onResume: () => void;
  onStop: () => void;
}

const GameTimer: React.FC<GameTimerProps> = ({
  timeRemaining,
  isActive,
  isPaused,
  formatTime,
  onStart,
  onPause,
  onResume,
  onStop
}) => {
  const getTimerColor = () => {
    if (timeRemaining <= 60) return 'text-red-500';
    if (timeRemaining <= 300) return 'text-yellow-500';
    return 'text-blue-500';
  };

  return (
    <div className="bg-white rounded-xl shadow-lg p-6 text-center">
      <div className="flex items-center justify-center gap-2 mb-4">
        <Clock className="w-6 h-6 text-gray-600" />
        <h3 className="text-lg font-semibold text-gray-800">Timer</h3>
      </div>
      
      <div className={`text-6xl font-mono font-bold mb-6 transition-colors duration-300 ${getTimerColor()}`}>
        {formatTime(timeRemaining)}
      </div>
      
      <div className="flex justify-center gap-3">
        {!isActive ? (
          <button
            onClick={onStart}
            className="flex items-center gap-2 px-6 py-3 bg-green-500 text-white rounded-lg hover:bg-green-600 transition-colors duration-200 font-medium"
          >
            <Play className="w-5 h-5" />
            Start
          </button>
        ) : (
          <>
            <button
              onClick={isPaused ? onResume : onPause}
              className="flex items-center gap-2 px-6 py-3 bg-yellow-500 text-white rounded-lg hover:bg-yellow-600 transition-colors duration-200 font-medium"
            >
              {isPaused ? <Play className="w-5 h-5" /> : <Pause className="w-5 h-5" />}
              {isPaused ? 'Resume' : 'Pause'}
            </button>
            <button
              onClick={onStop}
              className="flex items-center gap-2 px-4 py-3 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors duration-200 font-medium"
            >
              <Square className="w-5 h-5" />
              Stop
            </button>
          </>
        )}
      </div>
      
      {timeRemaining <= 60 && isActive && (
        <div className="mt-4 text-red-500 font-semibold animate-bounce">
          Final Minute!
        </div>
      )}
    </div>
  );
};

export default GameTimer;