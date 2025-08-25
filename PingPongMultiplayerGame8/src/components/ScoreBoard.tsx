import React from 'react';
import { Trophy, Zap, Target } from 'lucide-react';

interface ScoreBoardProps {
  player1Score: number;
  player2Score: number;
  rallies: number;
  ballSpeed: number;
}

const ScoreBoard: React.FC<ScoreBoardProps> = ({
  player1Score,
  player2Score,
  rallies,
  ballSpeed
}) => {
  return (
    <div className="flex items-center justify-center gap-8 mb-6 p-4 bg-gradient-to-r from-gray-900/50 via-gray-800/50 to-gray-900/50 rounded-xl border border-gray-700 backdrop-blur-sm">
      {/* Player 1 Score */}
      <div className="text-center">
        <div className="flex items-center justify-center gap-2 mb-2">
          <div className="w-3 h-3 bg-blue-400 rounded-full animate-pulse" />
          <span className="text-blue-400 font-semibold text-lg">Player 1</span>
        </div>
        <div className="text-4xl font-bold text-white bg-gradient-to-b from-blue-400 to-blue-600 bg-clip-text text-transparent">
          {player1Score.toString().padStart(2, '0')}
        </div>
      </div>

      {/* Center Stats */}
      <div className="flex flex-col items-center gap-3">
        <div className="flex items-center gap-4 text-sm">
          <div className="flex items-center gap-1 text-green-400">
            <Target size={16} />
            <span>{rallies}</span>
            <span className="text-gray-500 text-xs">rallies</span>
          </div>
          <div className="flex items-center gap-1 text-yellow-400">
            <Zap size={16} />
            <span>{ballSpeed.toFixed(1)}</span>
            <span className="text-gray-500 text-xs">speed</span>
          </div>
        </div>
        
        {/* VS indicator */}
        <div className="flex items-center gap-2">
          <div className="w-8 h-0.5 bg-gradient-to-r from-blue-400 to-transparent" />
          <span className="text-gray-400 font-bold text-sm">VS</span>
          <div className="w-8 h-0.5 bg-gradient-to-l from-red-400 to-transparent" />
        </div>
      </div>

      {/* Player 2 Score */}
      <div className="text-center">
        <div className="flex items-center justify-center gap-2 mb-2">
          <span className="text-red-400 font-semibold text-lg">Player 2</span>
          <div className="w-3 h-3 bg-red-400 rounded-full animate-pulse" />
        </div>
        <div className="text-4xl font-bold text-white bg-gradient-to-b from-red-400 to-red-600 bg-clip-text text-transparent">
          {player2Score.toString().padStart(2, '0')}
        </div>
      </div>
    </div>
  );
};

export default ScoreBoard;