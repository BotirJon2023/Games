import { useState, useCallback } from 'react';
import { Menu } from '@/components/Menu';
import { GameCanvas, type RaceConfig } from '@/components/GameCanvas';
import { Results } from '@/components/Results';
import type { RaceState } from '@/game/types';

type Screen = 'menu' | 'race' | 'results';

export default function App() {
  const [screen, setScreen] = useState<Screen>('menu');
  const [config, setConfig] = useState<RaceConfig | null>(null);
  const [result, setResult] = useState<RaceState | null>(null);
  const [raceKey, setRaceKey] = useState(0);

  const startRace = useCallback((cfg: RaceConfig) => {
    setConfig(cfg);
    setResult(null);
    setRaceKey((k) => k + 1);
    setScreen('race');
  }, []);

  const finishRace = useCallback((state: RaceState) => {
    setResult(state);
    setScreen('results');
  }, []);

  const restart = useCallback(() => {
    setResult(null);
    setRaceKey((k) => k + 1);
    setScreen('race');
  }, []);

  const toMenu = useCallback(() => {
    setScreen('menu');
    setResult(null);
  }, []);

  if (screen === 'menu' || !config) {
    return <Menu onStart={startRace} />;
  }

  if (screen === 'results' && result) {
    return <Results state={result} onRestart={restart} onMenu={toMenu} />;
  }

  return (
    <div className="w-screen h-screen overflow-hidden bg-slate-950">
      <GameCanvas key={raceKey} config={config} onFinish={finishRace} onExit={toMenu} />
    </div>
  );
}
