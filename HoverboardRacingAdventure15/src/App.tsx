import { useState } from 'react';
import { MenuScreen } from './game/MenuScreen';
import { GameCanvas } from './game/GameCanvas';
import { ResultsScreen } from './game/ResultsScreen';
import type { GameMode, GameResult, Screen } from './game/types';

function App() {
  const [screen, setScreen] = useState<Screen>('menu');
  const [mode, setMode] = useState<GameMode>('vs-computer');
  const [result, setResult] = useState<GameResult | null>(null);
  const [raceKey, setRaceKey] = useState(0);

  const handleStart = (selectedMode: GameMode) => {
    setMode(selectedMode);
    setScreen('game');
  };

  const handleFinish = (res: GameResult) => {
    setResult(res);
    setScreen('results');
  };

  const handlePlayAgain = () => {
    setRaceKey((k) => k + 1);
    setResult(null);
    setScreen('game');
  };

  const handleMenu = () => {
    setResult(null);
    setScreen('menu');
  };

  const handleExit = () => {
    setScreen('menu');
  };

  return (
    <div className="min-h-screen w-full bg-[#0a0a2e]">
      {screen === 'menu' && <MenuScreen onStart={handleStart} />}
      {screen === 'game' && (
        <div className="min-h-screen w-full flex items-center justify-center py-6 bg-[#0a0a2e]">
          <GameCanvas
            key={raceKey}
            mode={mode}
            onFinish={handleFinish}
            onExit={handleExit}
          />
        </div>
      )}
      {screen === 'results' && result && (
        <ResultsScreen result={result} onPlayAgain={handlePlayAgain} onMenu={handleMenu} />
      )}
    </div>
  );
}

export default App;
