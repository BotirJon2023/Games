import React from 'react';
import { Player } from '../types/tournament';
import { Target, TrendingUp, Award } from 'lucide-react';

interface PlayerStatsProps {
  player1: Player;
  player2: Player;
}

const PlayerStats: React.FC<PlayerStatsProps> = ({ player1, player2 }) => {
  const StatCard = ({ player, isLeft }: { player: Player; isLeft: boolean }) => (
    <div className={`p-4 rounded-lg ${
      isLeft ? 'bg-blue-900/30 border-blue-600/50' : 'bg-red-900/30 border-red-600/50'
    } border`}>
      <h4 className="font-semibold text-white mb-3">{player.name}</h4>
      
      <div className="space-y-2 text-sm">
        <div className="flex items-center justify-between">
          <span className="text-gray-300">Skill Level</span>
          <span className="text-white font-medium">
            {Math.round(player.skill * 100)}%
          </span>
        </div>
        
        <div className="flex items-center justify-between">
          <span className="text-gray-300">Accuracy</span>
          <span className="text-white font-medium">
            {Math.round(player.accuracy * 100)}%
          </span>
        </div>
        
        <div className="flex items-center justify-between">
          <span className="text-gray-300">Consistency</span>
          <span className="text-white font-medium">
            {Math.round(player.consistency * 100)}%
          </span>
        </div>
        
        <div className="flex items-center justify-between">
          <span className="text-gray-300">Tournament W/L</span>
          <span className="text-white font-medium">
            {player.wins}/{player.losses}
          </span>
        </div>
        
        <div className="flex items-center justify-between">
          <span className="text-gray-300">Bullseyes</span>
          <span className="text-yellow-400 font-medium flex items-center">
            <Target className="h-3 w-3 mr-1" />
            {player.bullseyes}
          </span>
        </div>
        
        <div className="flex items-center justify-between">
          <span className="text-gray-300">Trebles</span>
          <span className="text-green-400 font-medium">
            {player.trebles}
          </span>
        </div>
        
        <div className="flex items-center justify-between">
          <span className="text-gray-300">Doubles</span>
          <span className="text-blue-400 font-medium">
            {player.doubles}
          </span>
        </div>
      </div>
    </div>
  );

  return (
    <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-green-700/30">
      <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
        <TrendingUp className="h-5 w-5 mr-2" />
        Player Statistics
      </h3>
      
      <div className="space-y-4">
        <StatCard player={player1} isLeft={true} />
        <div className="text-center">
          <div className="text-yellow-400 font-semibold">VS</div>
        </div>
        <StatCard player={player2} isLeft={false} />
      </div>
    </div>
  );
};

export default PlayerStats;