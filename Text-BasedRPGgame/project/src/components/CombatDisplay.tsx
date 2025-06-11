import React, { useState, useEffect } from 'react';
import { Character, Enemy, Ability } from '../types/game';
import { useCombat } from '../hooks/useCombat';
import { Sword, Shield, Zap, Heart } from 'lucide-react';

interface CombatDisplayProps {
  character: Character;
  enemy: Enemy;
  onCombatEnd: (victory: boolean, updatedCharacter: Character) => void;
}

export const CombatDisplay: React.FC<CombatDisplayProps> = ({ 
  character, 
  enemy, 
  onCombatEnd 
}) => {
  const [currentCharacter, setCurrentCharacter] = useState(character);
  const [currentEnemy, setCurrentEnemy] = useState(enemy);
  const [selectedAbility, setSelectedAbility] = useState<Ability | null>(null);
  const [isAnimating, setIsAnimating] = useState(false);

  const { 
    combatLog, 
    playerTurn, 
    setPlayerTurn,
    playerAttack,
    enemyAttack,
    resetCombat 
  } = useCombat();

  useEffect(() => {
    resetCombat();
  }, [resetCombat]);

  useEffect(() => {
    if (currentEnemy.health <= 0) {
      // Victory
      const expGained = currentEnemy.experience;
      const goldGained = currentEnemy.gold;
      
      const updatedCharacter = {
        ...currentCharacter,
        experience: currentCharacter.experience + expGained,
        gold: currentCharacter.gold + goldGained
      };

      setTimeout(() => onCombatEnd(true, updatedCharacter), 1500);
      return;
    }

    if (currentCharacter.health <= 0) {
      // Defeat
      setTimeout(() => onCombatEnd(false, currentCharacter), 1500);
      return;
    }

    // Enemy turn
    if (!playerTurn && currentEnemy.health > 0) {
      setTimeout(() => {
        setIsAnimating(true);
        const result = enemyAttack(currentEnemy, currentCharacter);
        setCurrentCharacter(result.character);
        setTimeout(() => {
          setIsAnimating(false);
          setPlayerTurn(true);
        }, 1000);
      }, 1500);
    }
  }, [playerTurn, currentCharacter, currentEnemy, onCombatEnd, enemyAttack]);

  const handleAttack = (ability?: Ability) => {
    if (!playerTurn || isAnimating) return;
    
    if (ability && currentCharacter.mana < ability.manaCost) {
      return; // Not enough mana
    }

    setIsAnimating(true);
    const result = playerAttack(currentCharacter, currentEnemy, ability);
    setCurrentCharacter(result.character);
    setCurrentEnemy(result.enemy);
    
    setTimeout(() => {
      setIsAnimating(false);
      setPlayerTurn(false);
    }, 1000);
  };

  const enemyHealthPercentage = (currentEnemy.health / currentEnemy.maxHealth) * 100;
  const characterHealthPercentage = (currentCharacter.health / currentCharacter.maxHealth) * 100;

  return (
    <div className="bg-black/40 backdrop-blur-lg rounded-xl p-6 border border-purple-500/20">
      <h2 className="text-2xl font-bold text-red-400 mb-6 text-center">Combat!</h2>
      
      {/* Enemy Display */}
      <div className="mb-6 p-4 bg-red-900/20 border border-red-500/30 rounded-lg">
        <div className="flex items-center justify-between mb-2">
          <h3 className="text-xl font-bold text-white">{currentEnemy.name}</h3>
          <span className="text-gray-300">Level {currentEnemy.level}</span>
        </div>
        <p className="text-gray-300 text-sm mb-3">{currentEnemy.description}</p>
        <div className="space-y-2">
          <div className="flex items-center justify-between text-sm">
            <span className="text-red-400">Health</span>
            <span className="text-white">{currentEnemy.health}/{currentEnemy.maxHealth}</span>
          </div>
          <div className="w-full bg-gray-700 rounded-full h-3">
            <div
              className="bg-gradient-to-r from-red-600 to-red-500 h-3 rounded-full transition-all duration-500"
              style={{ width: `${enemyHealthPercentage}%` }}
            />
          </div>
        </div>
      </div>

      {/* Combat Log */}
      <div className="mb-6 p-4 bg-gray-900/50 border border-gray-500/30 rounded-lg h-32 overflow-y-auto">
        <div className="space-y-1">
          {combatLog.map((log, index) => (
            <p key={index} className="text-gray-300 text-sm">{log}</p>
          ))}
        </div>
      </div>

      {/* Character Health */}
      <div className="mb-6 p-4 bg-green-900/20 border border-green-500/30 rounded-lg">
        <div className="flex items-center justify-between text-sm mb-2">
          <span className="text-green-400">Your Health</span>
          <span className="text-white">{currentCharacter.health}/{currentCharacter.maxHealth}</span>
        </div>
        <div className="w-full bg-gray-700 rounded-full h-3">
          <div
            className="bg-gradient-to-r from-green-600 to-green-500 h-3 rounded-full transition-all duration-500"
            style={{ width: `${characterHealthPercentage}%` }}
          />
        </div>
      </div>

      {/* Combat Actions */}
      {playerTurn && currentCharacter.health > 0 && currentEnemy.health > 0 && (
        <div className="space-y-3">
          <p className="text-gold-300 font-medium">Choose your action:</p>
          
          {/* Basic Attack */}
          <button
            onClick={() => handleAttack()}
            disabled={isAnimating}
            className="w-full p-3 bg-red-700/30 hover:bg-red-600/40 border border-red-500/30 hover:border-red-400/50 rounded-lg transition-all duration-200 flex items-center text-white disabled:opacity-50"
          >
            <Sword className="w-5 h-5 mr-3" />
            <span>Attack</span>
          </button>

          {/* Abilities */}
          {currentCharacter.class.abilities.map((ability) => (
            <button
              key={ability.id}
              onClick={() => handleAttack(ability)}
              disabled={isAnimating || currentCharacter.mana < ability.manaCost}
              className="w-full p-3 bg-purple-700/30 hover:bg-purple-600/40 border border-purple-500/30 hover:border-purple-400/50 rounded-lg transition-all duration-200 flex items-center justify-between text-white disabled:opacity-50"
            >
              <div className="flex items-center">
                <Zap className="w-5 h-5 mr-3" />
                <div className="text-left">
                  <div className="font-medium">{ability.name}</div>
                  <div className="text-xs text-gray-300">{ability.description}</div>
                </div>
              </div>
              <span className="text-blue-400 text-sm">{ability.manaCost} MP</span>
            </button>
          ))}
        </div>
      )}

      {/* Combat Status */}
      {!playerTurn && currentCharacter.health > 0 && currentEnemy.health > 0 && (
        <div className="text-center">
          <p className="text-yellow-400 animate-pulse">Enemy is attacking...</p>
        </div>
      )}
    </div>
  );
};