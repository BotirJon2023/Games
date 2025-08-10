import React from 'react';
import { DartGame } from '../game/DartGame';
import { GameState } from '../types/tournament';
import { Play, Pause, SkipForward } from 'lucide-react';

interface GameControlsProps {
  game: DartGame;
  gameState: GameState;
}

const GameControls: React.FC<GameControlsProps> = ({ game, gameState }) => {
  if (!game.isActive) {
    return null;
  }

  return (
    <div className="mt-6 p-4 bg-white/5 rounded-lg border border-green-700/20">
      <div className="flex items-center justify-between mb-4">
        <h4 className="text-lg font-semibold text-white">Current Match</h4>
        <div className="text-sm text-green-100">
          {game.currentPlayer?.name}'s turn
        </div>
      </div>
      
      <div className="grid grid-cols-2 gap-4 mb-4">
        <div className="text-center p-3 bg-blue-900/20 rounded-lg">
          <div className="text-white font-semibold">{game.player1.name}</div>
          <div className="text-2xl font-bold text-blue-400">{game.score1}</div>
          <div className="text-xs text-gray-300">remaining</div>
        </div>
        <div className="text-center p-3 bg-red-900/20 rounded-lg">
          <div className="text-white font-semibold">{game.player2.name}</div>
          <div className="text-2xl font-bold text-red-400">{game.score2}</div>
          <div className="text-xs text-gray-300">remaining</div>
        </div>
      </div>

      {game.lastThrow && (
        <div className="text-center p-2 bg-yellow-900/20 rounded border border-yellow-600/30">
          <div className="text-yellow-400 font-medium">
            Last Throw: {game.lastThrow.score}
            {game.lastThrow.multiplier > 1 && (
              <span className="ml-1">
                (×{game.lastThrow.multiplier})
              </span>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default GameControls;