import React, { useState } from 'react';
import { Play, User, Bot } from 'lucide-react';

interface GameSetupProps {
  onStartGame: (playerNames: string[], computerOpponent: boolean) => void;
}

const GameSetup: React.FC<GameSetupProps> = ({ onStartGame }) => {
  const [playerCount, setPlayerCount] = useState(2);
  const [playerNames, setPlayerNames] = useState(['Player 1', 'Player 2']);
  const [computerOpponent, setComputerOpponent] = useState(false);

  const handlePlayerCountChange = (count: number) => {
    setPlayerCount(count);
    const newNames = Array.from({ length: count }, (_, i) => `Player ${i + 1}`);
    setPlayerNames(newNames);
    setComputerOpponent(false);
  };

  const handlePlayerNameChange = (index: number, name: string) => {
    const newNames = [...playerNames];
    newNames[index] = name;
    setPlayerNames(newNames);
  };

  const handleStartGame = () => {
    const names = computerOpponent
      ? playerNames.slice(0, playerCount)
      : playerNames;
    onStartGame(names, computerOpponent);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-sky-400 via-cyan-300 to-teal-400 flex items-center justify-center p-4">
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <div className="absolute top-10 left-10 w-32 h-32 bg-yellow-300 rounded-full opacity-80 animate-pulse" />
        <div className="absolute top-20 right-20 w-24 h-24 bg-white rounded-full opacity-60" />
        <div className="absolute bottom-40 left-1/4 w-40 h-16 bg-white/40 rounded-full" />
        <div className="absolute bottom-20 right-1/3 w-48 h-20 bg-white/30 rounded-full" />

        <div className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-amber-200 to-transparent" />

        <div className="absolute bottom-0 left-0 right-0 h-20 bg-gradient-to-t from-cyan-600/50 to-transparent">
          <svg className="w-full h-full" viewBox="0 0 800 80" preserveAspectRatio="none">
            <path
              d="M0,40 Q100,20 200,40 T400,40 T600,40 T800,40 L800,80 L0,80 Z"
              fill="rgba(0,150,200,0.3)"
              className="animate-pulse"
            />
          </svg>
        </div>
      </div>

      <div className="relative bg-white/95 backdrop-blur-sm rounded-2xl shadow-2xl p-8 max-w-md w-full">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold bg-gradient-to-r from-emerald-600 to-teal-600 bg-clip-text text-transparent mb-2">
            Virtual Golf Challenge
          </h1>
          <p className="text-gray-600">Seaside Edition</p>
        </div>

        <div className="space-y-6">
          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-3">
              Number of Players
            </label>
            <div className="flex gap-2">
              {[1, 2, 3, 4].map((count) => (
                <button
                  key={count}
                  onClick={() => handlePlayerCountChange(count)}
                  className={`flex-1 py-3 rounded-lg font-semibold transition-all ${
                    playerCount === count
                      ? 'bg-gradient-to-r from-emerald-500 to-teal-500 text-white shadow-lg'
                      : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                  }`}
                >
                  {count}
                </button>
              ))}
            </div>
          </div>

          <div>
            <label className="block text-sm font-semibold text-gray-700 mb-3">
              Player Names
            </label>
            <div className="space-y-2">
              {playerNames.map((name, index) => (
                <div key={index} className="flex items-center gap-2">
                  <div className="w-8 h-8 rounded-full flex items-center justify-center text-white font-bold text-sm"
                    style={{ backgroundColor: ['#FF6B6B', '#4ECDC4', '#FFE66D', '#95E1D3'][index] }}>
                    {index + 1}
                  </div>
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => handlePlayerNameChange(index, e.target.value)}
                    className="flex-1 px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-emerald-500 focus:border-transparent"
                    placeholder={`Player ${index + 1}`}
                  />
                </div>
              ))}
            </div>
          </div>

          <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div className="flex items-center gap-3">
              <Bot className="w-6 h-6 text-blue-500" />
              <div>
                <div className="font-semibold text-gray-700">Computer Opponent</div>
                <div className="text-sm text-gray-500">Play against AI</div>
              </div>
            </div>
            <button
              onClick={() => setComputerOpponent(!computerOpponent)}
              className={`w-14 h-8 rounded-full transition-all ${
                computerOpponent
                  ? 'bg-gradient-to-r from-blue-500 to-cyan-500'
                  : 'bg-gray-300'
              }`}
            >
              <div
                className={`w-6 h-6 bg-white rounded-full shadow-md transform transition-all ${
                  computerOpponent ? 'translate-x-7' : 'translate-x-1'
                }`}
              />
            </button>
          </div>

          <button
            onClick={handleStartGame}
            className="w-full py-4 bg-gradient-to-r from-emerald-500 to-teal-500 text-white font-bold rounded-xl shadow-lg hover:from-emerald-600 hover:to-teal-600 transform hover:scale-105 transition-all flex items-center justify-center gap-2"
          >
            <Play className="w-5 h-5" />
            Start Game
          </button>
        </div>

        <div className="mt-6 text-center text-sm text-gray-500">
          <p>9 holes | Par 3-5 | Wind effects</p>
        </div>
      </div>
    </div>
  );
};

export default GameSetup;
