import React, { useState } from 'react';
import { AnimatePresence } from 'framer-motion';
import StartScreen from './components/StartScreen';
import GameScreen from './components/GameScreen';
import { TournamentProvider } from './context/TournamentContext';
import { BoatSelectionProvider } from './context/BoatSelectionContext';
import { WeatherProvider } from './context/WeatherContext';
import { GameScreen as GameScreenType } from './types/game';

function App() {
  const [currentScreen, setCurrentScreen] = useState<GameScreenType>('start');
  const [playerName, setPlayerName] = useState<string>('');

  const handleStartGame = (name: string) => {
    setPlayerName(name);
    setCurrentScreen('boat-selection');
  };

  const navigateTo = (screen: GameScreenType) => {
    setCurrentScreen(screen);
  };

  return (
    <TournamentProvider>
      <BoatSelectionProvider>
        <WeatherProvider>
          <div className="min-h-screen bg-gradient-to-b from-blue-400 to-blue-600 overflow-hidden relative">
            {/* Water animation background */}
            <div className="absolute inset-0 z-0">
              <div className="wave wave1"></div>
              <div className="wave wave2"></div>
              <div className="wave wave3"></div>
              <div className="wave wave4"></div>
            </div>
            
            <div className="relative z-10 w-full h-full">
              <AnimatePresence mode="wait">
                {currentScreen === 'start' && (
                  <StartScreen onStartGame={handleStartGame} />
                )}
                {currentScreen !== 'start' && (
                  <GameScreen 
                    currentScreen={currentScreen} 
                    navigateTo={navigateTo} 
                    playerName={playerName}
                  />
                )}
              </AnimatePresence>
            </div>
          </div>
        </WeatherProvider>
      </BoatSelectionProvider>
    </TournamentProvider>
  );
}

export default App;