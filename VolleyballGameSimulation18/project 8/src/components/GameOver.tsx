import { Trophy, RotateCcw, Home } from 'lucide-react';

interface GameOverProps {
  winner: 1 | 2;
  player1Score: number;
  player2Score: number;
  onRestart: () => void;
  onMenu: () => void;
}

export function GameOver({ winner, player1Score, player2Score, onRestart, onMenu }: GameOverProps) {
  return (
    <div className="absolute inset-0 flex items-center justify-center bg-black/70 backdrop-blur-sm">
      <div className="bg-white rounded-xl p-12 text-center shadow-2xl max-w-md">
        <div className="mb-6">
          <Trophy size={64} className={`mx-auto ${winner === 1 ? 'text-blue-500' : 'text-red-500'}`} />
        </div>

        <h2 className="text-4xl font-bold mb-4" style={{ fontFamily: 'monospace' }}>
          GAME OVER
        </h2>

        <div className={`text-6xl font-bold mb-6 ${winner === 1 ? 'text-blue-500' : 'text-red-500'}`}>
          PLAYER {winner} WINS!
        </div>

        <div className="text-2xl mb-8 font-mono">
          {player1Score} - {player2Score}
        </div>

        <div className="flex gap-4 justify-center">
          <button
            onClick={onRestart}
            className="bg-green-600 hover:bg-green-700 text-white px-6 py-3 rounded-lg font-bold transition-all transform hover:scale-105 shadow-lg flex items-center gap-2"
          >
            <RotateCcw size={20} />
            PLAY AGAIN
          </button>

          <button
            onClick={onMenu}
            className="bg-gray-600 hover:bg-gray-700 text-white px-6 py-3 rounded-lg font-bold transition-all transform hover:scale-105 shadow-lg flex items-center gap-2"
          >
            <Home size={20} />
            MENU
          </button>
        </div>
      </div>
    </div>
  );
}
