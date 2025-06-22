import React from 'react';
import { Fighter } from '../types/Fighter';
import { User, Award, Zap, Shield, Swords, Heart } from 'lucide-react';

interface FighterCardProps {
  fighter: Fighter;
  isSelected?: boolean;
  onClick?: () => void;
  showStats?: boolean;
}

export const FighterCard: React.FC<FighterCardProps> = ({
  fighter,
  isSelected = false,
  onClick,
  showStats = true
}) => {
  const getStyleColor = (style: string) => {
    switch (style) {
      case 'Striker': return 'from-red-500 to-orange-500';
      case 'Grappler': return 'from-blue-500 to-cyan-500';
      case 'Wrestler': return 'from-green-500 to-emerald-500';
      case 'All-Around': return 'from-purple-500 to-pink-500';
      default: return 'from-gray-500 to-gray-600';
    }
  };

  const healthPercentage = (fighter.health / fighter.maxHealth) * 100;
  const staminaPercentage = (fighter.stamina / fighter.maxStamina) * 100;

  return (
    <div
      onClick={onClick}
      className={`
        relative bg-gray-900 rounded-xl p-4 transition-all duration-300 cursor-pointer
        ${isSelected ? 'ring-2 ring-blue-400 shadow-lg shadow-blue-400/30' : 'hover:shadow-lg'}
        ${onClick ? 'hover:scale-105' : ''}
      `}
    >
      {/* Fighter Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center space-x-3">
          <div className={`w-12 h-12 rounded-full bg-gradient-to-br ${getStyleColor(fighter.fightingStyle)} flex items-center justify-center`}>
            <User className="w-6 h-6 text-white" />
          </div>
          <div>
            <h3 className="text-white font-bold text-lg">{fighter.name}</h3>
            <p className="text-gray-400 text-sm">"{fighter.nickname}"</p>
          </div>
        </div>
        <div className="text-right">
          <div className="flex items-center text-yellow-400 mb-1">
            <Award className="w-4 h-4 mr-1" />
            <span className="text-sm">#{fighter.ranking}</span>
          </div>
          <div className="text-xs text-gray-400">{fighter.fightingStyle}</div>
        </div>
      </div>

      {/* Fighter Info */}
      <div className="grid grid-cols-2 gap-4 mb-3 text-sm">
        <div>
          <span className="text-gray-400">Record:</span>
          <div className="text-white font-medium">
            {fighter.record.wins}-{fighter.record.losses}-{fighter.record.draws}
          </div>
        </div>
        <div>
          <span className="text-gray-400">Age:</span>
          <div className="text-white font-medium">{fighter.age} years</div>
        </div>
        <div>
          <span className="text-gray-400">Weight:</span>
          <div className="text-white font-medium">{fighter.weight} lbs</div>
        </div>
        <div>
          <span className="text-gray-400">Reach:</span>
          <div className="text-white font-medium">{fighter.reach}"</div>
        </div>
      </div>

      {/* Health and Stamina Bars */}
      <div className="space-y-2 mb-3">
        <div className="flex items-center space-x-2">
          <Heart className="w-4 h-4 text-red-400" />
          <div className="flex-1 bg-gray-700 rounded-full h-2">
            <div 
              className="bg-red-500 h-2 rounded-full transition-all duration-500"
              style={{ width: `${healthPercentage}%` }}
            />
          </div>
          <span className="text-xs text-gray-400">{fighter.health}/{fighter.maxHealth}</span>
        </div>
        
        <div className="flex items-center space-x-2">
          <Zap className="w-4 h-4 text-yellow-400" />
          <div className="flex-1 bg-gray-700 rounded-full h-2">
            <div 
              className="bg-yellow-500 h-2 rounded-full transition-all duration-500"
              style={{ width: `${staminaPercentage}%` }}
            />
          </div>
          <span className="text-xs text-gray-400">{fighter.stamina}/{fighter.maxStamina}</span>
        </div>
      </div>

      {/* Stats */}
      {showStats && (
        <div className="grid grid-cols-4 gap-2 text-xs">
          <div className="text-center">
            <Swords className="w-4 h-4 text-red-400 mx-auto mb-1" />
            <div className="text-white font-medium">{fighter.stats.striking}</div>
            <div className="text-gray-400">STR</div>
          </div>
          <div className="text-center">
            <div className="w-4 h-4 bg-blue-400 rounded mx-auto mb-1"></div>
            <div className="text-white font-medium">{fighter.stats.grappling}</div>
            <div className="text-gray-400">GRP</div>
          </div>
          <div className="text-center">
            <div className="w-4 h-4 bg-green-400 rounded mx-auto mb-1"></div>
            <div className="text-white font-medium">{fighter.stats.wrestling}</div>
            <div className="text-gray-400">WRS</div>
          </div>
          <div className="text-center">
            <Shield className="w-4 h-4 text-purple-400 mx-auto mb-1" />
            <div className="text-white font-medium">{fighter.stats.defense}</div>
            <div className="text-gray-400">DEF</div>
          </div>
        </div>
      )}

      {/* Status Indicator */}
      <div className="absolute top-2 right-2">
        <div className={`w-3 h-3 rounded-full ${fighter.isActive ? 'bg-green-400' : 'bg-red-400'}`} />
      </div>
    </div>
  );
};