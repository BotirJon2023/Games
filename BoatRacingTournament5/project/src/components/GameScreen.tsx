import React from 'react';
import { motion } from 'framer-motion';
import BoatSelectionScreen from './game/BoatSelectionScreen';
import RaceScreen from './game/RaceScreen';
import TournamentScreen from './game/TournamentScreen';
import ResultsScreen from './game/ResultsScreen';
import LeaderboardScreen from './game/LeaderboardScreen';
import CustomizeScreen from './game/CustomizeScreen';
import TrackEditorScreen from './game/TrackEditorScreen';
import GameHeader from './ui/GameHeader';
import { GameScreen as GameScreenType } from '../types/game';
import { useTournament } from '../context/TournamentContext';

interface GameScreenProps {
  currentScreen: GameScreenType;
  navigateTo: (screen: GameScreenType) => void;
  playerName: string;
}

const GameScreen: React.FC<GameScreenProps> = ({ 
  currentScreen, 
  navigateTo,
  playerName 
}) => {
  const { tournamentState } = useTournament();

  const renderScreen = () => {
    switch (currentScreen) {
      case 'boat-selection':
        return <BoatSelectionScreen onSelect={() => navigateTo('tournament')} />;
      case 'tournament':
        return (
          <TournamentScreen 
            onStartRace={() => navigateTo('race')} 
            currentStage={tournamentState.currentStage}
            currentRaceIndex={tournamentState.currentRaceIndex}
          />
        );
      case 'race':
        return <RaceScreen onRaceFinish={() => navigateTo('results')} />;
      case 'results':
        return (
          <ResultsScreen 
            onContinue={() => {
              // If we're at the end of the tournament, go to leaderboard
              if (tournamentState.currentStage === 'finals' && 
                  tournamentState.currentRaceIndex === 0) {
                navigateTo('leaderboard');
              } else {
                navigateTo('tournament');
              }
            }} 
          />
        );
      case 'leaderboard':
        return <LeaderboardScreen onPlayAgain={() => navigateTo('boat-selection')} />;
      case 'customize':
        return <CustomizeScreen onBack={() => navigateTo('boat-selection')} />;
      case 'track-editor':
        return <TrackEditorScreen onBack={() => navigateTo('tournament')} />;
      default:
        return <div>Unknown screen</div>;
    }
  };

  const getPageTitle = () => {
    switch (currentScreen) {
      case 'boat-selection': return 'Select Your Boat';
      case 'tournament': return 'Tournament Overview';
      case 'race': return 'Race';
      case 'results': return 'Race Results';
      case 'leaderboard': return 'Tournament Leaderboard';
      case 'customize': return 'Customize Your Boat';
      case 'track-editor': return 'Track Editor';
      default: return 'Boat Racing Tournament';
    }
  };

  return (
    <motion.div 
      className="min-h-screen flex flex-col"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.3 }}
    >
      <GameHeader 
        title={getPageTitle()} 
        playerName={playerName}
        onNavigate={navigateTo}
        currentScreen={currentScreen}
      />
      
      <main className="flex-grow container-game py-6">
        {renderScreen()}
      </main>
    </motion.div>
  );
};

export default GameScreen;