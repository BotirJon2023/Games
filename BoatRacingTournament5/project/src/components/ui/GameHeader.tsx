import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Ship, Menu, X, Home, Trophy, Sailboat, Settings, Map } from 'lucide-react';
import { GameScreen } from '../../types/game';

interface GameHeaderProps {
  title: string;
  playerName: string;
  onNavigate: (screen: GameScreen) => void;
  currentScreen: GameScreen;
}

const GameHeader: React.FC<GameHeaderProps> = ({ 
  title, 
  playerName,
  onNavigate,
  currentScreen 
}) => {
  const [menuOpen, setMenuOpen] = useState(false);

  const toggleMenu = () => {
    setMenuOpen(!menuOpen);
  };

  const handleNavigate = (screen: GameScreen) => {
    onNavigate(screen);
    setMenuOpen(false);
  };

  const isActiveScreen = (screen: GameScreen) => {
    return currentScreen === screen;
  };

  return (
    <header className="bg-blue-700 text-white shadow-md relative z-20">
      <div className="container-game py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center">
            <Ship className="mr-2" size={28} />
            <h1 className="text-xl font-bold">{title}</h1>
          </div>
          
          <div className="hidden md:flex items-center space-x-6">
            <button 
              onClick={() => handleNavigate('boat-selection')}
              className={`flex items-center ${isActiveScreen('boat-selection') ? 'text-yellow-300' : 'hover:text-blue-200'}`}
            >
              <Sailboat size={18} className="mr-1" />
              <span>Boats</span>
            </button>
            
            <button 
              onClick={() => handleNavigate('tournament')}
              className={`flex items-center ${isActiveScreen('tournament') ? 'text-yellow-300' : 'hover:text-blue-200'}`}
            >
              <Trophy size={18} className="mr-1" />
              <span>Tournament</span>
            </button>
            
            <button 
              onClick={() => handleNavigate('leaderboard')}
              className={`flex items-center ${isActiveScreen('leaderboard') ? 'text-yellow-300' : 'hover:text-blue-200'}`}
            >
              <Trophy size={18} className="mr-1" />
              <span>Leaderboard</span>
            </button>
            
            <button 
              onClick={() => handleNavigate('track-editor')}
              className={`flex items-center ${isActiveScreen('track-editor') ? 'text-yellow-300' : 'hover:text-blue-200'}`}
            >
              <Map size={18} className="mr-1" />
              <span>Track Editor</span>
            </button>
            
            <button 
              onClick={() => handleNavigate('customize')}
              className={`flex items-center ${isActiveScreen('customize') ? 'text-yellow-300' : 'hover:text-blue-200'}`}
            >
              <Settings size={18} className="mr-1" />
              <span>Customize</span>
            </button>
          </div>
          
          <div className="flex items-center">
            <div className="hidden md:block mr-4">
              <span className="font-medium">{playerName}</span>
            </div>
            
            <button 
              onClick={toggleMenu}
              className="md:hidden p-2 rounded-full hover:bg-blue-600 transition-colors"
            >
              {menuOpen ? <X size={24} /> : <Menu size={24} />}
            </button>
          </div>
        </div>
      </div>
      
      {/* Mobile Menu */}
      {menuOpen && (
        <motion.div 
          className="absolute top-full left-0 right-0 bg-blue-700 shadow-lg md:hidden z-30"
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          exit={{ opacity: 0, y: -20 }}
          transition={{ duration: 0.2 }}
        >
          <div className="container-game py-4">
            <div className="flex items-center justify-between mb-4 border-b border-blue-600 pb-2">
              <span className="font-medium">{playerName}</span>
            </div>
            
            <nav className="space-y-3">
              <button 
                onClick={() => handleNavigate('boat-selection')}
                className={`flex items-center p-2 w-full rounded-lg ${
                  isActiveScreen('boat-selection') ? 'bg-blue-600 text-white' : 'hover:bg-blue-600'
                }`}
              >
                <Sailboat size={18} className="mr-3" />
                <span>Select Boat</span>
              </button>
              
              <button 
                onClick={() => handleNavigate('tournament')}
                className={`flex items-center p-2 w-full rounded-lg ${
                  isActiveScreen('tournament') ? 'bg-blue-600 text-white' : 'hover:bg-blue-600'
                }`}
              >
                <Trophy size={18} className="mr-3" />
                <span>Tournament</span>
              </button>
              
              <button 
                onClick={() => handleNavigate('leaderboard')}
                className={`flex items-center p-2 w-full rounded-lg ${
                  isActiveScreen('leaderboard') ? 'bg-blue-600 text-white' : 'hover:bg-blue-600'
                }`}
              >
                <Trophy size={18} className="mr-3" />
                <span>Leaderboard</span>
              </button>
              
              <button 
                onClick={() => handleNavigate('track-editor')}
                className={`flex items-center p-2 w-full rounded-lg ${
                  isActiveScreen('track-editor') ? 'bg-blue-600 text-white' : 'hover:bg-blue-600'
                }`}
              >
                <Map size={18} className="mr-3" />
                <span>Track Editor</span>
              </button>
              
              <button 
                onClick={() => handleNavigate('customize')}
                className={`flex items-center p-2 w-full rounded-lg ${
                  isActiveScreen('customize') ? 'bg-blue-600 text-white' : 'hover:bg-blue-600'
                }`}
              >
                <Settings size={18} className="mr-3" />
                <span>Customize</span>
              </button>
              
              <button 
                onClick={() => handleNavigate('start')}
                className="flex items-center p-2 w-full rounded-lg hover:bg-blue-600"
              >
                <Home size={18} className="mr-3" />
                <span>Back to Start</span>
              </button>
            </nav>
          </div>
        </motion.div>
      )}
    </header>
  );
};

export default GameHeader;