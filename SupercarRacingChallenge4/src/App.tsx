import { useState } from 'react';
import { StartMenu } from '@/components/StartMenu';
import { RaceGame } from '@/components/RaceGame';
import { ResultsScreen } from '@/components/ResultsScreen';
import type { GameMode, RaceResult } from '@/game/types';

type Screen = 'menu' | 'racing' | 'results';

function App() {
  const [screen, setScreen] = useState<Screen>('menu');
  const [mode, setMode] = useState<GameMode>('p2');
  const [car1Index, setCar1Index] = useState(0);
  const [car2Index, setCar2Index] = useState(1);
  const [result, setResult] = useState<RaceResult | null>(null);
  const [raceKey, setRaceKey] = useState(0);

  const handleStart = (m: GameMode, c1: number, c2: number) => {
    setMode(m);
    setCar1Index(c1);
    setCar2Index(c2);
    setRaceKey(k => k + 1);
    setScreen('racing');
  };

  const handleFinish = (r: RaceResult) => {
    setResult(r);
    setScreen('results');
  };

  const handleRestart = () => {
    setRaceKey(k => k + 1);
    setScreen('racing');
  };

  const handleMenu = () => {
    setScreen('menu');
    setResult(null);
  };

  return (
    <div className="w-screen h-screen bg-[#05050f] text-white overflow-hidden">
      {screen === 'menu' && <StartMenu onStart={handleStart} />}
      {screen === 'racing' && (
        <RaceGame
          key={raceKey}
          mode={mode}
          car1Index={car1Index}
          car2Index={car2Index}
          onFinish={handleFinish}
          onExit={handleMenu}
        />
      )}
      {screen === 'results' && result && (
        <ResultsScreen result={result} onRestart={handleRestart} onMenu={handleMenu} />
      )}
    </div>
  );
}

export default App;
