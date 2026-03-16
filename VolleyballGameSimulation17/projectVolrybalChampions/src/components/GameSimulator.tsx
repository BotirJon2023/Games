import { useState } from 'react';
import { X } from 'lucide-react';
import VolleyballCourt from './VolleyballCourt';

interface Team {
  id: string;
  name: string;
  color: string;
}

interface GameSimulatorProps {
  team1: Team;
  team2: Team;
  onComplete: (winnerId: string, team1Score: number, team2Score: number) => void;
  onClose: () => void;
}

export default function GameSimulator({
  team1,
  team2,
  onComplete,
  onClose,
}: GameSimulatorProps) {
  const [gameMode, setGameMode] = useState<'vs-ai' | 'two-player' | null>(null);

  if (gameMode === null) {
    return (
      <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 animate-fadeIn">
        <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-md w-full mx-4 animate-slideUp">
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-3xl font-bold text-gray-800">Select Game Mode</h2>
            <button
              onClick={onClose}
              className="text-gray-500 hover:text-gray-700 transition-colors"
            >
              <X className="w-6 h-6" />
            </button>
          </div>

          <div className="space-y-4">
            <div className="p-6 border-2 border-gray-200 rounded-lg bg-gray-50">
              <h3 className="font-bold text-lg mb-2 text-gray-800">{team1.name}</h3>
              <p className="text-sm text-gray-600 mb-4">
                vs <span className="font-bold">{team2.name}</span>
              </p>
            </div>

            <button
              onClick={() => setGameMode('vs-ai')}
              className="w-full px-6 py-4 bg-blue-500 text-white font-bold rounded-lg hover:bg-blue-600 transition-all duration-300 transform hover:scale-105"
            >
              Play vs AI
            </button>

            <button
              onClick={() => setGameMode('two-player')}
              className="w-full px-6 py-4 bg-green-500 text-white font-bold rounded-lg hover:bg-green-600 transition-all duration-300 transform hover:scale-105"
            >
              Two Player Mode
            </button>

            <button
              onClick={onClose}
              className="w-full px-6 py-4 bg-gray-300 text-gray-700 font-bold rounded-lg hover:bg-gray-400 transition"
            >
              Cancel
            </button>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center z-50 animate-fadeIn overflow-y-auto py-8">
      <div className="bg-white rounded-2xl shadow-2xl p-8 max-w-3xl w-full mx-4 animate-slideUp">
        <div className="flex justify-between items-center mb-6">
          <h2 className="text-3xl font-bold text-gray-800">
            {gameMode === 'vs-ai' ? 'Play vs AI' : 'Two Player Game'}
          </h2>
          <button
            onClick={onClose}
            className="text-gray-500 hover:text-gray-700 transition-colors"
          >
            <X className="w-6 h-6" />
          </button>
        </div>

        <VolleyballCourt
          team1Id={team1.id}
          team2Id={team2.id}
          team1Name={team1.name}
          team2Name={team2.name}
          team1Color={team1.color}
          team2Color={team2.color}
          onMatchEnd={(winnerId, team1Score, team2Score) => {
            setTimeout(() => {
              onComplete(winnerId, team1Score, team2Score);
            }, 1000);
          }}
          gameMode={gameMode}
        />
      </div>
    </div>
  );
}
