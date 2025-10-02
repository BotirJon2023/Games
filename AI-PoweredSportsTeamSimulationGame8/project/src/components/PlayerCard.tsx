import React from 'react';
import { Player } from '../types/game';
import { User, Zap, Shield, Target, Clock, TrendingUp } from 'lucide-react';

interface PlayerCardProps {
  player: Player;
  onClick?: () => void;
  selected?: boolean;
}

export function PlayerCard({ player, onClick, selected }: PlayerCardProps) {
  const getPositionColor = (position: Player['position']) => {
    switch (position) {
      case 'GK': return 'bg-yellow-500';
      case 'DEF': return 'bg-blue-500';
      case 'MID': return 'bg-green-500';
      case 'FWD': return 'bg-red-500';
      default: return 'bg-gray-500';
    }
  };

  const getOverallRating = () => {
    const { speed, strength, skill, stamina, experience } = player.stats;
    return Math.round((speed + strength + skill + stamina + experience) / 5);
  };

  const getStatIcon = (stat: string) => {
    switch (stat) {
      case 'speed': return <Zap className="w-4 h-4" />;
      case 'strength': return <Shield className="w-4 h-4" />;
      case 'skill': return <Target className="w-4 h-4" />;
      case 'stamina': return <Clock className="w-4 h-4" />;
      case 'experience': return <TrendingUp className="w-4 h-4" />;
      default: return <User className="w-4 h-4" />;
    }
  };

  return (
    <div
      className={`bg-white rounded-lg shadow-md p-4 cursor-pointer transition-all duration-200 hover:shadow-lg hover:scale-105 ${
        selected ? 'ring-2 ring-blue-500' : ''
      } ${player.injured ? 'opacity-60' : ''}`}
      onClick={onClick}
    >
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center space-x-3">
          <div className={`w-10 h-10 rounded-full ${getPositionColor(player.position)} flex items-center justify-center text-white font-bold`}>
            {player.position}
          </div>
          <div>
            <h3 className="font-semibold text-gray-800">{player.name}</h3>
            <p className="text-sm text-gray-500">Age: {player.age}</p>
          </div>
        </div>
        <div className="text-right">
          <div className="text-2xl font-bold text-blue-600">{getOverallRating()}</div>
          <div className="text-xs text-gray-500">Overall</div>
        </div>
      </div>

      <div className="grid grid-cols-2 gap-2 mb-3">
        {Object.entries(player.stats).map(([stat, value]) => (
          <div key={stat} className="flex items-center space-x-2">
            {getStatIcon(stat)}
            <span className="text-sm capitalize">{stat}</span>
            <span className="text-sm font-semibold ml-auto">{value}</span>
          </div>
        ))}
      </div>

      <div className="flex justify-between items-center text-sm">
        <div className="flex space-x-4">
          <div>
            <span className="text-gray-500">Form:</span>
            <span className={`ml-1 font-semibold ${player.form > 70 ? 'text-green-600' : player.form > 40 ? 'text-yellow-600' : 'text-red-600'}`}>
              {player.form}%
            </span>
          </div>
          <div>
            <span className="text-gray-500">Energy:</span>
            <span className={`ml-1 font-semibold ${player.energy > 70 ? 'text-green-600' : player.energy > 40 ? 'text-yellow-600' : 'text-red-600'}`}>
              {player.energy}%
            </span>
          </div>
        </div>
        <div className="text-green-600 font-semibold">
          ${(player.value / 1000).toFixed(0)}K
        </div>
      </div>

      {player.injured && (
        <div className="mt-2 bg-red-100 text-red-800 text-xs px-2 py-1 rounded">
          INJURED
        </div>
      )}
    </div>
  );
}