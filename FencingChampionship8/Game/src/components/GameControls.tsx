import React from 'react';
import { Play, Pause, RotateCcw, Settings, Sword } from 'lucide-react';
import { GameState } from '../types/game';

interface GameControlsProps {
  gameState: GameState;
  onPlayPause: () => void;
  onRestart: () => void;
  onStartTournament: () => void;
  onShowSettings: () => void;
}

export const GameControls: React.FC<GameControlsProps> = ({
  gameState,
  onPlayPause,
  onRestart,
  onStartTournament,
  onShowSettings
}) => {
  const isPlaying = gameState.gameStatus === 'playing';
  const isFinished = gameState.gameStatus === 'finished';

  return (
    <div className="bg-white rounded-lg shadow-lg p-4 mb-4">
      <div className="flex flex-wrap items-center justify-center gap-3">
        {gameState.gameStatus !== 'tournament' && (
          <>
            <button
              onClick={onPlayPause}
              className={`
                flex items-center px-4 py-2 rounded-lg font-medium transition-all duration-200
                ${isPlaying 
                  ? 'bg-orange-500 hover:bg-orange-600 text-white' 
                  : 'bg-green-500 hover:bg-green-600 text-white'
                }
              `}
            >
              {isPlaying ? <Pause className="w-4 h-4 mr-2" /> : <Play className="w-4 h-4 mr-2" />}
              {isPlaying ? 'Pause' : 'Play'}
            </button>

            <button
              onClick={onRestart}
              className="flex items-center px-4 py-2 bg-blue-500 hover:bg-blue-600 text-white rounded-lg font-medium transition-all duration-200"
            >
              <RotateCcw className="w-4 h-4 mr-2" />
              Restart
            </button>
          </>
        )}

        <button
          onClick={onStartTournament}
          className="flex items-center px-4 py-2 bg-purple-500 hover:bg-purple-600 text-white rounded-lg font-medium transition-all duration-200"
          disabled={gameState.gameStatus === 'tournament'}
        >
          <Sword className="w-4 h-4 mr-2" />
          {gameState.gameStatus === 'tournament' ? 'Tournament Active' : 'Start Tournament'}
        </button>

        <button
          onClick={onShowSettings}
          className="flex items-center px-4 py-2 bg-gray-500 hover:bg-gray-600 text-white rounded-lg font-medium transition-all duration-200"
        >
          <Settings className="w-4 h-4 mr-2" />
          Settings
        </button>
      </div>

      {/* Control Instructions */}
      {gameState.gameStatus === 'playing' && (
        <div className="mt-4 bg-gray-50 rounded-lg p-3">
          <h4 className="font-semibold text-gray-700 mb-2">Controls:</h4>
          <div className="grid grid-cols-2 gap-4 text-sm text-gray-600">
            <div>
              <strong>Player 1:</strong>
              <div>A/D - Move Left/Right</div>
              <div>S - Attack</div>
              <div>W - Parry</div>
            </div>
            <div>
              <strong>Player 2:</strong>
              <div>←/→ - Move Left/Right</div>
              <div>↓ - Attack</div>
              <div>↑ - Parry</div>
            </div>
          </div>
        </div>
      )}

      {/* Combat Log */}
      {gameState.combatLog.length > 0 && (
        <div className="mt-4 bg-gray-50 rounded-lg p-3">
          <h4 className="font-semibold text-gray-700 mb-2">Recent Actions:</h4>
          <div className="max-h-20 overflow-y-auto space-y-1 text-xs text-gray-600">
            {gameState.combatLog.slice(-3).map((event, index) => (
              <div key={index} className="flex justify-between">
                <span>{event.attacker} {event.action}s {event.defender}</span>
                {event.damage && <span className="text-red-600">-{event.damage}</span>}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};