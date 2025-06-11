import { useState, useCallback } from 'react';
import { GameState, Character, Enemy } from '../types/game';

const initialGameState: GameState = {
  character: null,
  currentScene: 'intro',
  chapter: 1,
  storyProgress: 0,
  inCombat: false,
  currentEnemy: null,
  gameLog: []
};

export const useGameState = () => {
  const [gameState, setGameState] = useState<GameState>(initialGameState);

  const updateCharacter = useCallback((updates: Partial<Character>) => {
    setGameState(prev => ({
      ...prev,
      character: prev.character ? { ...prev.character, ...updates } : null
    }));
  }, []);

  const addToLog = useCallback((message: string) => {
    setGameState(prev => ({
      ...prev,
      gameLog: [...prev.gameLog.slice(-9), message] // Keep last 10 messages
    }));
  }, []);

  const startCombat = useCallback((enemy: Enemy) => {
    setGameState(prev => ({
      ...prev,
      inCombat: true,
      currentEnemy: { ...enemy }
    }));
    addToLog(`Combat begins with ${enemy.name}!`);
  }, [addToLog]);

  const endCombat = useCallback((victory: boolean) => {
    setGameState(prev => ({
      ...prev,
      inCombat: false,
      currentEnemy: null
    }));
    addToLog(victory ? 'Victory!' : 'Defeat...');
  }, [addToLog]);

  const changeScene = useCallback((sceneId: string) => {
    setGameState(prev => ({
      ...prev,
      currentScene: sceneId,
      storyProgress: prev.storyProgress + 1
    }));
  }, []);

  const createCharacter = useCallback((character: Character) => {
    setGameState(prev => ({
      ...prev,
      character
    }));
    addToLog(`${character.name} the ${character.class.name} begins their adventure!`);
  }, [addToLog]);

  return {
    gameState,
    setGameState,
    updateCharacter,
    addToLog,
    startCombat,
    endCombat,
    changeScene,
    createCharacter
  };
};