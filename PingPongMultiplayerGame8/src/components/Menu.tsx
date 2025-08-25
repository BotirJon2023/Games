import React from 'react';
import { Play, Users, Gamepad2 } from 'lucide-react';

interface MenuProps {
  onStartGame: () => void;
}

const Menu: React.FC<MenuProps> = ({ onStartGame }) => {
  return (
    <div className="absolute inset-0 bg-gradient-to-br from-gray-900/95 via-blue-900/20 to-purple-900/30 backdrop-blur-sm flex flex-col items-center justify-center z-40">
      <div className="text-center space-y-8 animate-fade-in">
        {/* Game Title */}
        <div className="space-y-4">
          <div className="flex items-center justify-center gap-4">
            <Gamepad2 size={48} className="text-green-400 animate-bounce" />
            <h1 className="text-6xl font-bold bg-gradient-to-r from-green-400 via-blue-500 to-purple-600 bg-clip-text text-transparent">
              PONG
            </h1>
            <Gamepad2 size={48} className="text-green-400 animate-bounce" style={{ animationDelay: '0.2s' }} />
          </div>
          <p className="text-xl text-gray-300">
            Classic arcade ping-pong with modern effects
          </p>
        </div>

        {/* Game Features */}
        <div className="space-y-4">
          <div className="flex items-center justify-center gap-6 text-gray-400">
            <div className="flex items-center gap-2">
              <Users size={20} className="text-blue-400" />
              <span>Multiplayer</span>
            </div>
            <div className="w-1 h-1 bg-gray-500 rounded-full" />
            <div className="flex items-center gap-2">
              <span>Real Physics</span>
            </div>
            <div className="w-1 h-1 bg-gray-500 rounded-full" />
            <div className="flex items-center gap-2">
              <span>Particle Effects</span>
            </div>
          </div>
        </div>

        {/* Controls */}
        <div className="space-y-6">
          <div className="grid grid-cols-2 gap-8 text-sm">
            <div className="space-y-2">
              <h3 className="text-blue-400 font-semibold text-lg">Player 1</h3>
              <div className="space-y-1 text-gray-300">
                <div className="flex justify-between">
                  <span>Move Up:</span>
                  <kbd className="px-2 py-1 bg-gray-700 rounded text-xs font-mono">W</kbd>
                </div>
                <div className="flex justify-between">
                  <span>Move Down:</span>
                  <kbd className="px-2 py-1 bg-gray-700 rounded text-xs font-mono">S</kbd>
                </div>
              </div>
            </div>
            <div className="space-y-2">
              <h3 className="text-red-400 font-semibold text-lg">Player 2</h3>
              <div className="space-y-1 text-gray-300">
                <div className="flex justify-between">
                  <span>Move Up:</span>
                  <kbd className="px-2 py-1 bg-gray-700 rounded text-xs font-mono">↑</kbd>
                </div>
                <div className="flex justify-between">
                  <span>Move Down:</span>
                  <kbd className="px-2 py-1 bg-gray-700 rounded text-xs font-mono">↓</kbd>
                </div>
              </div>
            </div>
          </div>
        </div>

        {/* Start Button */}
        <button
          onClick={onStartGame}
          className="group flex items-center gap-3 px-8 py-4 bg-gradient-to-r from-green-500 to-blue-600 hover:from-green-400 hover:to-blue-500 text-white font-bold text-xl rounded-xl transition-all duration-300 transform hover:scale-105 hover:shadow-2xl hover:shadow-green-500/25 animate-pulse-slow"
        >
          <Play size={24} className="group-hover:scale-110 transition-transform" />
          Start Game
          <div className="absolute inset-0 rounded-xl bg-white opacity-0 group-hover:opacity-10 transition-opacity" />
        </button>

        <p className="text-gray-500 text-sm animate-pulse">
          Press SPACE to start or click the button above
        </p>
      </div>

      {/* Animated background particles */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        {[...Array(20)].map((_, i) => (
          <div
            key={i}
            className="absolute w-1 h-1 bg-green-400 rounded-full opacity-20 animate-float"
            style={{
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              animationDelay: `${Math.random() * 5}s`,
              animationDuration: `${3 + Math.random() * 4}s`
            }}
          />
        ))}
      </div>
    </div>
  );
};

export default Menu;