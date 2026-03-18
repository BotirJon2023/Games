import { Difficulty, PlayerMode } from '../types/game';
import { Trophy, Users, Cpu } from 'lucide-react';

interface GameMenuProps {
  onStartGame: (playerMode: PlayerMode, difficulty?: Difficulty) => void;
}

export default function GameMenu({ onStartGame }: GameMenuProps) {
  return (
    <div className="min-h-screen bg-gradient-to-b from-sky-400 to-amber-200 flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl shadow-2xl p-8 max-w-md w-full">
        <div className="text-center mb-8">
          <h1 className="text-5xl font-bold text-amber-600 mb-2 flex items-center justify-center gap-2">
            <Trophy className="w-12 h-12" />
            Beach Volleyball
          </h1>
          <p className="text-gray-600">Choose your game mode</p>
        </div>

        <div className="space-y-6">
          <div className="bg-gradient-to-r from-blue-50 to-blue-100 rounded-2xl p-6 border-2 border-blue-300">
            <h2 className="text-2xl font-bold text-blue-900 mb-4 flex items-center gap-2">
              <Users className="w-6 h-6" />
              Two Players
            </h2>
            <button
              onClick={() => onStartGame('2player')}
              className="w-full bg-blue-600 hover:bg-blue-700 text-white font-bold py-4 px-6 rounded-xl transition-all transform hover:scale-105 shadow-lg"
            >
              Start 2 Player Game
            </button>
            <p className="text-sm text-blue-700 mt-3 text-center">
              Player 1: W/A/D | Player 2: Arrow Keys
            </p>
          </div>

          <div className="bg-gradient-to-r from-green-50 to-green-100 rounded-2xl p-6 border-2 border-green-300">
            <h2 className="text-2xl font-bold text-green-900 mb-4 flex items-center gap-2">
              <Cpu className="w-6 h-6" />
              Vs Computer
            </h2>
            <div className="space-y-3">
              <DifficultyButton
                difficulty="easy"
                label="Easy"
                color="green"
                onClick={() => onStartGame('1player', 'easy')}
              />
              <DifficultyButton
                difficulty="medium"
                label="Medium"
                color="yellow"
                onClick={() => onStartGame('1player', 'medium')}
              />
              <DifficultyButton
                difficulty="hard"
                label="Hard"
                color="orange"
                onClick={() => onStartGame('1player', 'hard')}
              />
              <DifficultyButton
                difficulty="expert"
                label="Expert"
                color="red"
                onClick={() => onStartGame('1player', 'expert')}
              />
            </div>
            <p className="text-sm text-green-700 mt-3 text-center">
              Controls: W/A/D or Arrow Keys
            </p>
          </div>
        </div>

        <div className="mt-6 text-center text-sm text-gray-600">
          <p className="font-semibold mb-2">How to Play:</p>
          <p>Jump: W or ↑ | Move: A/D or ←/→</p>
          <p className="mt-1">First to 11 points wins!</p>
        </div>
      </div>
    </div>
  );
}

interface DifficultyButtonProps {
  difficulty: Difficulty;
  label: string;
  color: string;
  onClick: () => void;
}

function DifficultyButton({ label, color, onClick }: DifficultyButtonProps) {
  const colorMap: Record<string, string> = {
    green: 'bg-green-500 hover:bg-green-600',
    yellow: 'bg-yellow-500 hover:bg-yellow-600',
    orange: 'bg-orange-500 hover:bg-orange-600',
    red: 'bg-red-600 hover:bg-red-700',
  };

  return (
    <button
      onClick={onClick}
      className={`w-full ${colorMap[color]} text-white font-bold py-3 px-6 rounded-lg transition-all transform hover:scale-105 shadow-md`}
    >
      {label}
    </button>
  );
}
