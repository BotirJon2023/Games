import React from 'react';
import { Player } from '../types/golf';

interface GameMessageProps {
  message: string;
  currentPlayer: Player | null;
  strokes: number;
  courseName: string;
  par: number;
}

const GameMessage: React.FC<GameMessageProps> = ({
  message,
  currentPlayer,
  strokes,
  courseName,
  par,
}) => {
  return (
    <div className="bg-white/95 backdrop-blur-sm rounded-xl shadow-lg p-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-4">
          {currentPlayer && (
            <div
              className="w-12 h-12 rounded-full flex items-center justify-center text-white font-bold text-lg shadow-lg"
              style={{ backgroundColor: currentPlayer.color }}
            >
              {currentPlayer.name.charAt(0).toUpperCase()}
            </div>
          )}
          <div>
            <div className="text-sm text-gray-500">{courseName} | Par {par}</div>
            <div className="text-lg font-bold text-gray-800">{message}</div>
          </div>
        </div>

        <div className="flex items-center gap-4">
          <div className="text-center">
            <div className="text-sm text-gray-500">Strokes</div>
            <div className="text-2xl font-bold text-gray-800">{strokes}</div>
          </div>
        </div>
      </div>
    </div>
  );
};

export default GameMessage;
