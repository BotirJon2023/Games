import React, { createContext, useContext, useState, ReactNode } from 'react';
import { Boat } from '../types/game';
import { boats } from '../data/boats';

interface BoatSelectionContextType {
  selectedBoat: Boat | null;
  selectBoat: (boat: Boat) => void;
}

const BoatSelectionContext = createContext<BoatSelectionContextType | undefined>(undefined);

export const useBoatSelection = () => {
  const context = useContext(BoatSelectionContext);
  if (!context) {
    throw new Error('useBoatSelection must be used within a BoatSelectionProvider');
  }
  return context;
};

interface BoatSelectionProviderProps {
  children: ReactNode;
}

export const BoatSelectionProvider: React.FC<BoatSelectionProviderProps> = ({ children }) => {
  const defaultBoat = boats.find(boat => boat.unlocked) || null;
  const [selectedBoat, setSelectedBoat] = useState<Boat | null>(defaultBoat);

  const selectBoat = (boat: Boat) => {
    setSelectedBoat(boat);
  };

  return (
    <BoatSelectionContext.Provider value={{ selectedBoat, selectBoat }}>
      {children}
    </BoatSelectionContext.Provider>
  );
};