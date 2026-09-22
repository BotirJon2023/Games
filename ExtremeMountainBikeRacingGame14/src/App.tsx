import { useState } from 'react';
import { StartMenu, type GameStyle, type GameMode } from '@/components/StartMenu';
import { GameView, ResultsScreen } from '@/components/GameView';
import { Game3DView, Results3DScreen } from '@/components/Game3DView';
import { GameFPView, ResultsFPScreen } from '@/components/GameFPView';
import type { RaceResult } from '@/game/types';
import type { RaceResult3D } from '@/game3d/types';
import type { FPRaceResult } from '@/gamefp/types';

type Screen = 'menu' | 'racing' | 'finished';

function App() {
  const [screen, setScreen] = useState<Screen>('menu');
  const [style, setStyle] = useState<GameStyle>('2d');
  const [mode, setMode] = useState<GameMode>('vs-cpu');
  const [results2D, setResults2D] = useState<RaceResult[]>([]);
  const [results3D, setResults3D] = useState<RaceResult3D[]>([]);
  const [resultsFP, setResultsFP] = useState<FPRaceResult[]>([]);
  const [gameKey, setGameKey] = useState(0);

  const handleStart = (selectedStyle: GameStyle, selectedMode: GameMode) => {
    setStyle(selectedStyle);
    setMode(selectedMode);
    setScreen('racing');
  };

  const handleFinish2D = (results: RaceResult[]) => {
    setResults2D(results);
    setScreen('finished');
  };

  const handleFinish3D = (results: RaceResult3D[]) => {
    setResults3D(results);
    setScreen('finished');
  };

  const handleFinishFP = (results: FPRaceResult[]) => {
    setResultsFP(results);
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
    if (style === '3d') {
      return (
        <Game3DView
          key={gameKey}
          mode={mode}
          onExit={handleMenu}
          onFinish={handleFinish3D}
        />
      );
    }
    if (style === 'fp') {
      return (
        <GameFPView
          key={gameKey}
          mode={mode}
          onExit={handleMenu}
          onFinish={handleFinishFP}
        />
      );
    }
    return (
      <GameView
        key={gameKey}
        mode={mode}
        onExit={handleMenu}
        onFinish={handleFinish2D}
      />
    );
  }

  if (screen === 'finished') {
    if (style === '3d') {
      return (
        <Results3DScreen
          results={results3D}
          onPlayAgain={handlePlayAgain}
          onMenu={handleMenu}
        />
      );
    }
    if (style === 'fp') {
      return (
        <ResultsFPScreen
          results={resultsFP}
          onPlayAgain={handlePlayAgain}
          onMenu={handleMenu}
        />
      );
    }
    return (
      <ResultsScreen
        results={results2D}
        onPlayAgain={handlePlayAgain}
        onMenu={handleMenu}
      />
    );
  }

  return <StartMenu onStart={handleStart} />;
}

export default App;
