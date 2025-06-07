import React, { createContext, useContext, useState, ReactNode } from 'react';
import { TournamentState, TournamentStage, RaceResult } from '../types/game';

interface TournamentContextType {
  tournamentState: TournamentState;
  advanceToNextRace: () => void;
  addRaceResult: (results: RaceResult[]) => void;
  resetTournament: () => void;
}

const TournamentContext = createContext<TournamentContextType | undefined>(undefined);

export const useTournament = () => {
  const context = useContext(TournamentContext);
  if (!context) {
    throw new Error('useTournament must be used within a TournamentProvider');
  }
  return context;
};

interface TournamentProviderProps {
  children: ReactNode;
}

const initialTournamentState: TournamentState = {
  currentStage: 'qualifying',
  races: {
    qualifying: [],
    quarterfinals: [],
    semifinals: [],
    finals: []
  },
  playerQualified: true,
  currentRaceIndex: 0
};

export const TournamentProvider: React.FC<TournamentProviderProps> = ({ children }) => {
  const [tournamentState, setTournamentState] = useState<TournamentState>(initialTournamentState);

  const advanceToNextRace = () => {
    const { currentStage, currentRaceIndex } = tournamentState;
    
    // Determine if we need to move to the next stage or just the next race
    let newStage: TournamentStage = currentStage;
    let newRaceIndex = currentRaceIndex;
    
    // Logic for determining next race or stage
    switch (currentStage) {
      case 'qualifying':
        // Qualifying has 2 races
        if (currentRaceIndex === 1) {
          newStage = 'quarterfinals';
          newRaceIndex = 0;
        } else {
          newRaceIndex++;
        }
        break;
      
      case 'quarterfinals':
        // Quarterfinals has 1 race
        newStage = 'semifinals';
        newRaceIndex = 0;
        break;
      
      case 'semifinals':
        // Semifinals has 1 race
        newStage = 'finals';
        newRaceIndex = 0;
        break;
      
      case 'finals':
        // Finals is the end, reset to start of tournament
        newStage = 'qualifying';
        newRaceIndex = 0;
        break;
    }
    
    setTournamentState(prev => ({
      ...prev,
      currentStage: newStage,
      currentRaceIndex: newRaceIndex
    }));
  };

  const addRaceResult = (results: RaceResult[]) => {
    const { currentStage, currentRaceIndex } = tournamentState;
    
    setTournamentState(prev => {
      const updatedRaces = { ...prev.races };
      
      // Update the appropriate race results array
      switch (currentStage) {
        case 'qualifying':
          if (!updatedRaces.qualifying[currentRaceIndex]) {
            updatedRaces.qualifying[currentRaceIndex] = [];
          }
          updatedRaces.qualifying[currentRaceIndex] = results;
          break;
        
        case 'quarterfinals':
          if (!updatedRaces.quarterfinals[currentRaceIndex]) {
            updatedRaces.quarterfinals[currentRaceIndex] = [];
          }
          updatedRaces.quarterfinals[currentRaceIndex] = results;
          break;
        
        case 'semifinals':
          if (!updatedRaces.semifinals[currentRaceIndex]) {
            updatedRaces.semifinals[currentRaceIndex] = [];
          }
          updatedRaces.semifinals[currentRaceIndex] = results;
          break;
        
        case 'finals':
          updatedRaces.finals = results;
          break;
      }
      
      // Check if player qualified (position 1-3)
      const playerResult = results.find(r => r.isPlayer);
      const playerQualified = playerResult ? playerResult.position <= 3 : false;
      
      return {
        ...prev,
        races: updatedRaces,
        playerQualified
      };
    });
  };

  const resetTournament = () => {
    setTournamentState(initialTournamentState);
  };

  return (
    <TournamentContext.Provider value={{ 
      tournamentState, 
      advanceToNextRace, 
      addRaceResult,
      resetTournament
    }}>
      {children}
    </TournamentContext.Provider>
  );
};