import React from 'react';
import { GameState } from '../types/game';
import { Clock, Trophy } from 'lucide-react';

interface ScoreBoardProps {
  gameState: GameState;
}

export const ScoreBoard: React.FC<ScoreBoardProps> = ({ gameState }) => {
  const { homeTeam, awayTeam, currentHalf, timeRemaining, phase } = gameState;
  
  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="bg-gradient-to-r from-blue-900 to-purple-900 text-white p-6 rounded-lg shadow-2xl">
      <div className="flex items-center justify-between">
        {/* Home Team */}
        <div className="flex items-center gap-4">
          <div
            className="w-12 h-12 rounded-full flex items-center justify-center text-2xl font-bold"
            style={{ backgroundColor: homeTeam.color }}
          >
            {homeTeam.name.charAt(0)}
          </div>
          <div>
            <h3 className="text-xl font-bold">{homeTeam.name}</h3>
            <div className="text-3xl font-black text-yellow-400">{homeTeam.score}</div>
          </div>
        </div>

        {/* Game Info */}
        <div className="text-center">
          <div className="flex items-center gap-2 justify-center mb-2">
            <Clock size={20} />
            <span className="text-lg font-semibold">{formatTime(timeRemaining)}</span>
          </div>
          <div className="text-sm opacity-80">Half {currentHalf}</div>
          <div className="text-xs mt-1 px-2 py-1 bg-white/20 rounded capitalize">
            {phase}
          </div>
        </div>

        {/* Away Team */}
        <div className="flex items-center gap-4">
          <div className="text-right">
            <h3 className="text-xl font-bold">{awayTeam.name}</h3>
            <div className="text-3xl font-black text-yellow-400">{awayTeam.score}</div>
          </div>
          <div
            className="w-12 h-12 rounded-full flex items-center justify-center text-2xl font-bold"
            style={{ backgroundColor: awayTeam.color }}
          >
            {awayTeam.name.charAt(0)}
          </div>
        </div>
      </div>

      {/* Winner Display */}
      {phase === 'fulltime' && (
        <div className="mt-4 text-center">
          <div className="flex items-center justify-center gap-2 text-yellow-400">
            <Trophy size={24} />
            <span className="text-xl font-bold">
              {homeTeam.score > awayTeam.score
                ? `${homeTeam.name} Wins!`
                : homeTeam.score < awayTeam.score
                ? `${awayTeam.name} Wins!`
                : 'Draw!'}
            </span>
            <Trophy size={24} />
          </div>
        </div>
      )}
    </div>
  );
};