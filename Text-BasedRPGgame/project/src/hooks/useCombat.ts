import { useState, useCallback } from 'react';
import { Character, Enemy, Ability } from '../types/game';

export const useCombat = () => {
  const [combatLog, setCombatLog] = useState<string[]>([]);
  const [playerTurn, setPlayerTurn] = useState(true);

  const addToCombatLog = useCallback((message: string) => {
    setCombatLog(prev => [...prev.slice(-4), message]);
  }, []);

  const calculateDamage = useCallback((attacker: Character | Enemy, defender: Character | Enemy, ability?: Ability) => {
    const baseAttack = attacker.attack + (ability?.damage || 0);
    const defense = defender.defense;
    const damage = Math.max(1, Math.floor(baseAttack * (0.8 + Math.random() * 0.4) - defense * 0.5));
    return damage;
  }, []);

  const playerAttack = useCallback((
    character: Character,
    enemy: Enemy,
    ability?: Ability
  ): { character: Character; enemy: Enemy; damage: number } => {
    const damage = calculateDamage(character, enemy, ability);
    const newEnemy = { ...enemy, health: Math.max(0, enemy.health - damage) };
    
    let newCharacter = character;
    if (ability) {
      newCharacter = { ...character, mana: Math.max(0, character.mana - ability.manaCost) };
      addToCombatLog(`${character.name} uses ${ability.name} for ${damage} damage!`);
    } else {
      addToCombatLog(`${character.name} attacks for ${damage} damage!`);
    }

    if (newEnemy.health <= 0) {
      addToCombatLog(`${enemy.name} is defeated!`);
    }

    return { character: newCharacter, enemy: newEnemy, damage };
  }, [calculateDamage, addToCombatLog]);

  const enemyAttack = useCallback((
    enemy: Enemy,
    character: Character
  ): { character: Character; enemy: Enemy; damage: number } => {
    const damage = calculateDamage(enemy, character);
    const newCharacter = { ...character, health: Math.max(0, character.health - damage) };
    
    addToCombatLog(`${enemy.name} attacks ${character.name} for ${damage} damage!`);
    
    if (newCharacter.health <= 0) {
      addToCombatLog(`${character.name} is defeated...`);
    }

    return { character: newCharacter, enemy, damage };
  }, [calculateDamage, addToCombatLog]);

  const resetCombat = useCallback(() => {
    setCombatLog([]);
    setPlayerTurn(true);
  }, []);

  return {
    combatLog,
    playerTurn,
    setPlayerTurn,
    playerAttack,
    enemyAttack,
    resetCombat,
    addToCombatLog
  };
};