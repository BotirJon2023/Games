import React from 'react';
import { CharacterCreation } from './components/CharacterCreation';
import { CharacterStats } from './components/CharacterStats';
import { StoryDisplay } from './components/StoryDisplay';
import { CombatDisplay } from './components/CombatDisplay';
import { GameLog } from './components/GameLog';
import { useGameState } from './hooks/useGameState';
import { storyScenes } from './data/storyScenes';
import { enemies } from './data/enemies';

function App() {
  const {
    gameState,
    createCharacter,
    changeScene,
    startCombat,
    endCombat,
    updateCharacter,
    addToLog
  } = useGameState();

  const handleChoice = (action: string) => {
    addToLog(`You chose: ${action}`);
    
    // Handle different actions
    switch (action) {
      case 'combat_goblin':
        const goblin = enemies.find(e => e.id === 'goblin');
        if (goblin) {
          startCombat(goblin);
        }
        break;
      case 'forest_deeper':
        changeScene('forest_deeper');
        break;
      case 'tavern_info':
        changeScene('tavern_info');
        break;
      case 'forest_entrance':
        changeScene('forest_entrance');
        break;
      case 'forest_path':
        changeScene('forest_path');
        break;
      default:
        // Default to next scene progression
        const nextScenes = ['tavern_info', 'forest_entrance', 'forest_path', 'forest_deeper'];
        const currentIndex = nextScenes.indexOf(gameState.currentScene);
        if (currentIndex >= 0 && currentIndex < nextScenes.length - 1) {
          changeScene(nextScenes[currentIndex + 1]);
        }
        break;
    }
  };

  const handleCombatEnd = (victory: boolean, updatedCharacter: any) => {
    if (victory) {
      addToLog(`Victory! Gained ${updatedCharacter.experience - gameState.character!.experience} experience and ${updatedCharacter.gold - gameState.character!.gold} gold.`);
      updateCharacter(updatedCharacter);
      changeScene('victory_goblin');
    } else {
      addToLog('You were defeated...');
      // Could implement respawn logic here
    }
    endCombat(victory);
  };

  // Character creation phase
  if (!gameState.character) {
    return <CharacterCreation onCharacterCreate={createCharacter} />;
  }

  // Combat phase
  if (gameState.inCombat && gameState.currentEnemy) {
    return (
      <div className="min-h-screen bg-gradient-to-b from-purple-900 via-indigo-900 to-black p-4">
        <div className="max-w-4xl mx-auto">
          <div className="grid lg:grid-cols-3 gap-6">
            <div className="lg:col-span-2">
              <CombatDisplay
                character={gameState.character}
                enemy={gameState.currentEnemy}
                onCombatEnd={handleCombatEnd}
              />
            </div>
            <div className="space-y-4">
              <CharacterStats character={gameState.character} />
              <GameLog logs={gameState.gameLog} />
            </div>
          </div>
        </div>
      </div>
    );
  }

  // Main game phase
  const currentScene = storyScenes.find(scene => scene.id === gameState.currentScene) || storyScenes[0];

  return (
    <div className="min-h-screen bg-gradient-to-b from-purple-900 via-indigo-900 to-black p-4">
      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-transparent bg-clip-text bg-gradient-to-r from-gold-400 to-amber-300">
            The Whispering Woods
          </h1>
          <p className="text-purple-300 mt-2">Chapter {gameState.chapter}</p>
        </div>

        <div className="grid lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2">
            <StoryDisplay
              scene={currentScene}
              onChoice={handleChoice}
            />
          </div>
          <div className="space-y-4">
            <CharacterStats character={gameState.character} />
            <GameLog logs={gameState.gameLog} />
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;