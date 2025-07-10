import React from 'react';
import { Play, Trophy, Settings, Info } from 'lucide-react';

interface GameMenuProps {
  onStartGame: () => void;
  highScore: number;
}

const GameMenu: React.FC<GameMenuProps> = ({ onStartGame, highScore }) => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center relative overflow-hidden">
      {/* Animated Background Elements */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-20 left-10 w-16 h-16 bg-white/10 rounded-full animate-float"></div>
        <div className="absolute top-40 right-20 w-12 h-12 bg-white/5 rounded-full animate-float-delayed"></div>
        <div className="absolute bottom-32 left-1/4 w-20 h-20 bg-white/10 rounded-full animate-float-slow"></div>
        <div className="absolute bottom-20 right-10 w-8 h-8 bg-white/15 rounded-full animate-float"></div>
      </div>

      {/* Main Content */}
      <div className="text-center z-10 px-8">
        <div className="mb-8">
          <h1 className="text-6xl md:text-8xl font-bold text-white mb-4 tracking-wider drop-shadow-lg">
            EXTREME
          </h1>
          <h2 className="text-4xl md:text-6xl font-bold text-yellow-300 mb-2 tracking-wider drop-shadow-lg">
            CYCLING
          </h2>
          <p className="text-lg text-white/80 mb-8 max-w-md mx-auto">
            Push your limits in the most thrilling cycling adventure ever created!
          </p>
        </div>

        {/* Stats Display */}
        <div className="mb-8 bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-white/20">
          <div className="flex items-center justify-center space-x-2 mb-2">
            <Trophy className="w-6 h-6 text-yellow-300" />
            <span className="text-white font-semibold">High Score</span>
          </div>
          <div className="text-3xl font-bold text-yellow-300">
            {highScore.toLocaleString()}
          </div>
        </div>

        {/* Action Buttons */}
        <div className="space-y-4">
          <button
            onClick={onStartGame}
            className="flex items-center justify-center space-x-3 bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 text-white px-8 py-4 rounded-xl font-bold text-xl transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-xl border border-green-400/50"
          >
            <Play className="w-6 h-6" />
            <span>START GAME</span>
          </button>

          <div className="flex flex-col sm:flex-row gap-4 justify-center">
            <button className="flex items-center justify-center space-x-2 bg-white/10 hover:bg-white/20 text-white px-6 py-3 rounded-xl font-semibold transition-all duration-300 transform hover:scale-105 backdrop-blur-sm border border-white/20">
              <Settings className="w-5 h-5" />
              <span>Settings</span>
            </button>
            <button className="flex items-center justify-center space-x-2 bg-white/10 hover:bg-white/20 text-white px-6 py-3 rounded-xl font-semibold transition-all duration-300 transform hover:scale-105 backdrop-blur-sm border border-white/20">
              <Info className="w-5 h-5" />
              <span>How to Play</span>
            </button>
          </div>
        </div>

        {/* Controls Info */}
        <div className="mt-12 text-sm text-white/70 space-y-2">
          <p>Use SPACEBAR to jump • LEFT/RIGHT arrows to steer</p>
          <p>Collect power-ups • Avoid obstacles • Beat your high score!</p>
        </div>
      </div>
    </div>
  );
};

export default GameMenu;