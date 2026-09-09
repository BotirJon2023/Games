import { useState } from 'react';
import { StartMenu } from '@/components/StartMenu';
import { GameView, ResultsScreen } from '@/components/GameView';
import type { GameMode, RaceResult, Screen } from '@/game/types';

function App() {
  const [screen, setScreen] = useState<Screen>('menu');
  const [mode, setMode] = useState<GameMode>('vs-cpu');
  const [results, setResults] = useState<RaceResult[]>([]);
  const [gameKey, setGameKey] = useState(0);

  const handleStart = (selectedMode: GameMode) => {
    setMode(selectedMode);
    setScreen('racing');
  };

  const handleFinish = (raceResults: RaceResult[]) => {
    setResults(raceResults);
    setScreen('finished');
  };

  const handlePlayAgain = () => {
    setGameKey((k) => k + 1);
    setScreen('racing');
  };

  const handleMenu = () => {
    setScreen('menu');
  };

  if (screen === 'menu') {
    return <StartMenu onStart={handleStart} />;
  }

  if (screen === 'racing') {
    return (
      <GameView
        key={gameKey}
        mode={mode}
        onExit={handleMenu}
        onFinish={handleFinish}
      />
    );
  }

  if (screen === 'finished') {
    return (
      <ResultsScreen
        results={results}
        onPlayAgain={handlePlayAgain}
        onMenu={handleMenu}
      />
    );
  }

  return <StartMenu onStart={handleStart} />;
}

export default App;
