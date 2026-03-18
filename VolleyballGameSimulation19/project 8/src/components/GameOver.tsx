import { Trophy, RotateCcw, Home } from 'lucide-react';
import { GameState } from '../types/game';

interface GameOverProps {
  gameState: GameState;
  onRestart: () => void;
  onMenu: () => void;
}

export default function GameOver({ gameState, onRestart, onMenu }: GameOverProps) {
  const winner = gameState.player1.score >= gameState.maxScore ? 1 : 2;
  const winnerName = winner === 1 ? 'Player 1' : gameState.playerMode === '1player' ? 'Computer' : 'Player 2';
  const winnerColor = winner === 1 ? 'text-red-500' : 'text-teal-500';
  const bgColor = winner === 1 ? 'from-red-100 to-red-50' : 'from-teal-100 to-teal-50';

  return (
    <div className="absolute inset-0 bg-black bg-opacity-60 flex items-center justify-center z-10">
      <div className={`bg-gradient-to-b ${bgColor} rounded-3xl shadow-2xl p-8 max-w-md w-full mx-4 transform animate-bounce-in`}>
        <div className="text-center">
          <Trophy className={`w-24 h-24 ${winnerColor} mx-auto mb-4`} />
          <h2 className="text-4xl font-bold text-gray-800 mb-2">Game Over!</h2>
          <p className={`text-3xl font-bold ${winnerColor} mb-6`}>
            {winnerName} Wins!
          </p>

          <div className="bg-white rounded-xl p-6 mb-6 shadow-inner">
            <div className="flex justify-around text-center">
              <div>
                <p className="text-gray-600 text-sm mb-1">Player 1</p>
                <p className="text-4xl font-bold text-red-500">{gameState.player1.score}</p>
              </div>
              <div className="flex items-center">
                <span className="text-3xl text-gray-400">-</span>
              </div>
              <div>
                <p className="text-gray-600 text-sm mb-1">
                  {gameState.playerMode === '1player' ? 'Computer' : 'Player 2'}
                </p>
                <p className="text-4xl font-bold text-teal-500">{gameState.player2.score}</p>
              </div>
            </div>
          </div>

          <div className="space-y-3">
            <button
              onClick={onRestart}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-4 px-6 rounded-xl transition-all transform hover:scale-105 shadow-lg flex items-center justify-center gap-2"
            >
              <RotateCcw className="w-5 h-5" />
              Play Again
            </button>
            <button
              onClick={onMenu}
              className="w-full bg-gray-600 hover:bg-gray-700 text-white font-bold py-4 px-6 rounded-xl transition-all transform hover:scale-105 shadow-lg flex items-center justify-center gap-2"
            >
              <Home className="w-5 h-5" />
              Main Menu
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
