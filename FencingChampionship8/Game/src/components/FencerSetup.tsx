import React from 'react';
import { Sword, User } from 'lucide-react';
import { FENCING_STYLES, FAMOUS_FENCERS } from '../data/fencingStyles';
import { FencingStyle } from '../types/game';

interface FencerSetupProps {
  onStartGame: (player1: FencerConfig, player2: FencerConfig) => void;
}

interface FencerConfig {
  name: string;
  style: FencingStyle;
  isAI: boolean;
  difficulty?: 'easy' | 'medium' | 'hard';
}

export const FencerSetup: React.FC<FencerSetupProps> = ({ onStartGame }) => {
  const [player1, setPlayer1] = React.useState<FencerConfig>({
    name: 'Player 1',
    style: FENCING_STYLES[0],
    isAI: false
  });

  const [player2, setPlayer2] = React.useState<FencerConfig>({
    name: FAMOUS_FENCERS[0],
    style: FENCING_STYLES[1],
    isAI: true,
    difficulty: 'medium'
  });

  const handleStartGame = () => {
    onStartGame(player1, player2);
  };

  const getRandomFencer = () => {
    return FAMOUS_FENCERS[Math.floor(Math.random() * FAMOUS_FENCERS.length)];
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-900 to-purple-900 flex items-center justify-center p-4">
      <div className="bg-white rounded-xl shadow-2xl p-8 max-w-4xl w-full">
        <div className="text-center mb-8">
          <div className="flex items-center justify-center mb-4">
            <Sword className="w-12 h-12 text-blue-600 mr-3" />
            <h1 className="text-4xl font-bold text-gray-800">Fencing Championship</h1>
            <Sword className="w-12 h-12 text-blue-600 ml-3 scale-x-[-1]" />
          </div>
          <p className="text-gray-600 text-lg">Configure your fencers and begin the duel!</p>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8">
          {/* Player 1 Configuration */}
          <div className="bg-blue-50 rounded-lg p-6 border-2 border-blue-200">
            <h3 className="text-xl font-bold text-blue-800 mb-4 flex items-center">
              <User className="w-5 h-5 mr-2" />
              Fencer 1
            </h3>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Name</label>
                <input
                  type="text"
                  value={player1.name}
                  onChange={(e) => setPlayer1(prev => ({ ...prev, name: e.target.value }))}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                />
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Weapon Style</label>
                <select
                  value={player1.style.name}
                  onChange={(e) => {
                    const style = FENCING_STYLES.find(s => s.name === e.target.value);
                    if (style) setPlayer1(prev => ({ ...prev, style }));
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                >
                  {FENCING_STYLES.map(style => (
                    <option key={style.name} value={style.name}>{style.name}</option>
                  ))}
                </select>
              </div>

              <div className="bg-white p-3 rounded border">
                <h4 className="font-medium text-gray-700 mb-1">Style Stats:</h4>
                <div className="text-sm text-gray-600 space-y-1">
                  <div>Attack Speed: {player1.style.attackSpeed.toFixed(1)}x</div>
                  <div>Parry Speed: {player1.style.parrySpeed.toFixed(1)}x</div>
                  <div>Reach: {player1.style.reach}cm</div>
                </div>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="player1-ai"
                  checked={player1.isAI}
                  onChange={(e) => setPlayer1(prev => ({ 
                    ...prev, 
                    isAI: e.target.checked,
                    difficulty: e.target.checked ? 'medium' : undefined
                  }))}
                  className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
                />
                <label htmlFor="player1-ai" className="ml-2 block text-sm text-gray-700">
                  AI Controlled
                </label>
              </div>

              {player1.isAI && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">AI Difficulty</label>
                  <select
                    value={player1.difficulty}
                    onChange={(e) => setPlayer1(prev => ({ 
                      ...prev, 
                      difficulty: e.target.value as 'easy' | 'medium' | 'hard' 
                    }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  >
                    <option value="easy">Easy</option>
                    <option value="medium">Medium</option>
                    <option value="hard">Hard</option>
                  </select>
                </div>
              )}
            </div>
          </div>

          {/* Player 2 Configuration */}
          <div className="bg-red-50 rounded-lg p-6 border-2 border-red-200">
            <h3 className="text-xl font-bold text-red-800 mb-4 flex items-center">
              <User className="w-5 h-5 mr-2" />
              Fencer 2
            </h3>
            
            <div className="space-y-4">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Name</label>
                <div className="flex space-x-2">
                  <input
                    type="text"
                    value={player2.name}
                    onChange={(e) => setPlayer2(prev => ({ ...prev, name: e.target.value }))}
                    className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                  />
                  <button
                    onClick={() => setPlayer2(prev => ({ ...prev, name: getRandomFencer() }))}
                    className="px-3 py-2 bg-red-500 text-white rounded-lg hover:bg-red-600 transition-colors"
                  >
                    Random
                  </button>
                </div>
              </div>

              <div>
                <label className="block text-sm font-medium text-gray-700 mb-2">Weapon Style</label>
                <select
                  value={player2.style.name}
                  onChange={(e) => {
                    const style = FENCING_STYLES.find(s => s.name === e.target.value);
                    if (style) setPlayer2(prev => ({ ...prev, style }));
                  }}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                >
                  {FENCING_STYLES.map(style => (
                    <option key={style.name} value={style.name}>{style.name}</option>
                  ))}
                </select>
              </div>

              <div className="bg-white p-3 rounded border">
                <h4 className="font-medium text-gray-700 mb-1">Style Stats:</h4>
                <div className="text-sm text-gray-600 space-y-1">
                  <div>Attack Speed: {player2.style.attackSpeed.toFixed(1)}x</div>
                  <div>Parry Speed: {player2.style.parrySpeed.toFixed(1)}x</div>
                  <div>Reach: {player2.style.reach}cm</div>
                </div>
              </div>

              <div className="flex items-center">
                <input
                  type="checkbox"
                  id="player2-ai"
                  checked={player2.isAI}
                  onChange={(e) => setPlayer2(prev => ({ 
                    ...prev, 
                    isAI: e.target.checked,
                    difficulty: e.target.checked ? 'medium' : undefined
                  }))}
                  className="h-4 w-4 text-red-600 focus:ring-red-500 border-gray-300 rounded"
                />
                <label htmlFor="player2-ai" className="ml-2 block text-sm text-gray-700">
                  AI Controlled
                </label>
              </div>

              {player2.isAI && (
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">AI Difficulty</label>
                  <select
                    value={player2.difficulty}
                    onChange={(e) => setPlayer2(prev => ({ 
                      ...prev, 
                      difficulty: e.target.value as 'easy' | 'medium' | 'hard' 
                    }))}
                    className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-red-500 focus:border-transparent"
                  >
                    <option value="easy">Easy</option>
                    <option value="medium">Medium</option>
                    <option value="hard">Hard</option>
                  </select>
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="mt-8 text-center">
          <button
            onClick={handleStartGame}
            className="bg-gradient-to-r from-blue-600 to-purple-600 hover:from-blue-700 hover:to-purple-700 text-white font-bold py-4 px-8 rounded-xl text-lg transition-all duration-200 transform hover:scale-105 shadow-lg"
          >
            Begin Duel!
          </button>
        </div>

        {/* Game Rules */}
        <div className="mt-6 bg-gray-50 rounded-lg p-4">
          <h4 className="font-semibold text-gray-700 mb-2">Game Rules:</h4>
          <ul className="text-sm text-gray-600 space-y-1">
            <li>• First to 5 points wins the match</li>
            <li>• Successful attacks score 1 point</li>
            <li>• Parrying blocks incoming attacks</li>
            <li>• Failed attacks leave you vulnerable</li>
            <li>• Each round lasts 3 minutes</li>
          </ul>
        </div>
      </div>
    </div>
  );
};