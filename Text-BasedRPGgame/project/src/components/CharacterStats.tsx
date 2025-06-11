import React from 'react';
import { Character } from '../types/game';
import { Heart, Zap, Sword, Shield, Coins } from 'lucide-react';

interface CharacterStatsProps {
  character: Character;
}

export const CharacterStats: React.FC<CharacterStatsProps> = ({ character }) => {
  const healthPercentage = (character.health / character.maxHealth) * 100;
  const manaPercentage = (character.mana / character.maxMana) * 100;
  const expPercentage = (character.experience / character.experienceToNext) * 100;

  return (
    <div className="bg-black/40 backdrop-blur-lg rounded-xl p-6 border border-purple-500/20">
      <div className="flex items-center justify-between mb-4">
        <div>
          <h2 className="text-xl font-bold text-white">{character.name}</h2>
          <p className="text-purple-300">{character.class.name} - Level {character.level}</p>
        </div>
        <div className="flex items-center text-gold-400">
          <Coins className="w-4 h-4 mr-1" />
          <span className="font-semibold">{character.gold}</span>
        </div>
      </div>

      <div className="space-y-3">
        {/* Health Bar */}
        <div className="space-y-1">
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center text-red-400">
              <Heart className="w-4 h-4 mr-1" />
              <span>Health</span>
            </div>
            <span className="text-white">{character.health}/{character.maxHealth}</span>
          </div>
          <div className="w-full bg-gray-700 rounded-full h-2">
            <div
              className="bg-gradient-to-r from-red-500 to-red-400 h-2 rounded-full transition-all duration-500"
              style={{ width: `${healthPercentage}%` }}
            />
          </div>
        </div>

        {/* Mana Bar */}
        <div className="space-y-1">
          <div className="flex items-center justify-between text-sm">
            <div className="flex items-center text-blue-400">
              <Zap className="w-4 h-4 mr-1" />
              <span>Mana</span>
            </div>
            <span className="text-white">{character.mana}/{character.maxMana}</span>
          </div>
          <div className="w-full bg-gray-700 rounded-full h-2">
            <div
              className="bg-gradient-to-r from-blue-500 to-blue-400 h-2 rounded-full transition-all duration-500"
              style={{ width: `${manaPercentage}%` }}
            />
          </div>
        </div>

        {/* Experience Bar */}
        <div className="space-y-1">
          <div className="flex items-center justify-between text-sm">
            <span className="text-purple-400">Experience</span>
            <span className="text-white">{character.experience}/{character.experienceToNext}</span>
          </div>
          <div className="w-full bg-gray-700 rounded-full h-2">
            <div
              className="bg-gradient-to-r from-purple-500 to-purple-400 h-2 rounded-full transition-all duration-500"
              style={{ width: `${expPercentage}%` }}
            />
          </div>
        </div>

        {/* Combat Stats */}
        <div className="grid grid-cols-2 gap-4 mt-4 pt-4 border-t border-purple-500/20">
          <div className="flex items-center text-red-400">
            <Sword className="w-4 h-4 mr-2" />
            <span className="text-sm">Attack: {character.attack}</span>
          </div>
          <div className="flex items-center text-yellow-400">
            <Shield className="w-4 h-4 mr-2" />
            <span className="text-sm">Defense: {character.defense}</span>
          </div>
        </div>
      </div>
    </div>
  );
};