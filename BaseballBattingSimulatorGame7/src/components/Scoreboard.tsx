import React from 'react';
import { GameStats } from '../types/game';

interface ScoreboardProps {
  balls: number;
  strikes: number;
  stats: GameStats;
  difficulty: string;
  message: string;
}

export const Scoreboard: React.FC<ScoreboardProps> = ({
  balls,
  strikes,
  stats,
  difficulty,
  message,
}) => {
  return (
    <div className="bg-gradient-to-r from-gray-900 to-gray-800 text-white p-6 rounded-xl shadow-2xl border-4 border-yellow-500">
      {/* Main Scoreboard Header */}
      <div className="text-center mb-6">
        <h2 className="text-3xl font-bold text-yellow-400 mb-2">SCOREBOARD</h2>
        <div className="flex justify-center space-x-8 text-2xl font-mono">
          <div className="bg-red-600 px-4 py-2 rounded">
            <span className="text-sm block">BALLS</span>
            <span className="text-3xl font-bold">{balls}</span>
          </div>
          <div className="bg-yellow-600 px-4 py-2 rounded">
            <span className="text-sm block">STRIKES</span>
            <span className="text-3xl font-bold">{strikes}</span>
          </div>
        </div>
      </div>

      {/* Game Message */}
      <div className="text-center mb-6">
        <div className="bg-blue-900 p-4 rounded-lg border-2 border-blue-400">
          <p className="text-lg font-semibold text-blue-200">{message}</p>
        </div>
      </div>

      {/* Player Statistics */}
      <div className="grid grid-cols-2 gap-4 mb-4">
        <div className="bg-green-800 p-3 rounded-lg">
          <h3 className="text-lg font-bold text-green-200 mb-2">Batting Stats</h3>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span>At Bats:</span>
              <span className="font-bold">{stats.atBats}</span>
            </div>
            <div className="flex justify-between">
              <span>Hits:</span>
              <span className="font-bold text-green-300">{stats.hits}</span>
            </div>
            <div className="flex justify-between">
              <span>Home Runs:</span>
              <span className="font-bold text-yellow-300">{stats.homeRuns}</span>
            </div>
            <div className="flex justify-between">
              <span>Average:</span>
              <span className="font-bold text-blue-300">{stats.battingAverage.toFixed(3)}</span>
            </div>
          </div>
        </div>

        <div className="bg-purple-800 p-3 rounded-lg">
          <h3 className="text-lg font-bold text-purple-200 mb-2">Performance</h3>
          <div className="space-y-1 text-sm">
            <div className="flex justify-between">
              <span>Strikeouts:</span>
              <span className="font-bold text-red-300">{stats.strikeouts}</span>
            </div>
            <div className="flex justify-between">
              <span>Walks:</span>
              <span className="font-bold text-blue-300">{stats.walks}</span>
            </div>
            <div className="flex justify-between">
              <span>Current Streak:</span>
              <span className="font-bold text-yellow-300">{stats.currentStreak}</span>
            </div>
            <div className="flex justify-between">
              <span>Best Streak:</span>
              <span className="font-bold text-orange-300">{stats.bestStreak}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Difficulty Indicator */}
      <div className="text-center">
        <div className={`inline-block px-4 py-2 rounded-full text-sm font-bold ${
          difficulty === 'easy' ? 'bg-green-600 text-green-100' :
          difficulty === 'medium' ? 'bg-yellow-600 text-yellow-100' :
          'bg-red-600 text-red-100'
        }`}>
          Difficulty: {difficulty.toUpperCase()}
        </div>
      </div>
    </div>
  );
};