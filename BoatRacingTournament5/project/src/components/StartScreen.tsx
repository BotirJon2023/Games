import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { Ship, Trophy, Settings, Play } from 'lucide-react';

interface StartScreenProps {
  onStartGame: (playerName: string) => void;
}

const StartScreen: React.FC<StartScreenProps> = ({ onStartGame }) => {
  const [playerName, setPlayerName] = useState('');
  const [nameError, setNameError] = useState('');
  const [showSettings, setShowSettings] = useState(false);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!playerName.trim()) {
      setNameError('Please enter your name');
      return;
    }
    onStartGame(playerName);
  };

  const handleNameChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPlayerName(e.target.value);
    if (e.target.value.trim()) {
      setNameError('');
    }
  };

  const toggleSettings = () => {
    setShowSettings(!showSettings);
  };

  return (
    <motion.div 
      className="flex flex-col items-center justify-center min-h-screen p-4"
      initial={{ opacity: 0 }}
      animate={{ opacity: 1 }}
      exit={{ opacity: 0 }}
      transition={{ duration: 0.5 }}
    >
      <motion.div 
        className="card p-8 w-full max-w-md"
        initial={{ y: 50, opacity: 0 }}
        animate={{ y: 0, opacity: 1 }}
        transition={{ delay: 0.2, type: 'spring', stiffness: 100 }}
      >
        <div className="text-center mb-8">
          <motion.div 
            className="flex justify-center mb-4"
            initial={{ scale: 0.8, opacity: 0 }}
            animate={{ scale: 1, opacity: 1 }}
            transition={{ delay: 0.4, type: 'spring', stiffness: 120 }}
          >
            <Ship size={60} className="text-blue-600" />
          </motion.div>
          <motion.h1 
            className="text-4xl font-bold text-blue-800 mb-2"
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.5 }}
          >
            Boat Racing Tournament
          </motion.h1>
          <motion.p 
            className="text-blue-600"
            initial={{ y: 20, opacity: 0 }}
            animate={{ y: 0, opacity: 1 }}
            transition={{ delay: 0.6 }}
          >
            Race to victory across challenging waters!
          </motion.p>
        </div>

        <motion.form 
          onSubmit={handleSubmit}
          initial={{ y: 20, opacity: 0 }}
          animate={{ y: 0, opacity: 1 }}
          transition={{ delay: 0.7 }}
        >
          <div className="mb-6">
            <label htmlFor="playerName" className="block mb-2 text-sm font-medium text-gray-700">
              Enter Your Name
            </label>
            <input
              type="text"
              id="playerName"
              value={playerName}
              onChange={handleNameChange}
              className={`w-full px-4 py-3 border rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 ${
                nameError ? 'border-red-500' : 'border-gray-300'
              }`}
              placeholder="Captain's name"
            />
            {nameError && <p className="mt-1 text-sm text-red-500">{nameError}</p>}
          </div>

          <div className="space-y-3">
            <button 
              type="submit" 
              className="btn btn-primary w-full flex items-center justify-center"
            >
              <Play size={20} className="mr-2" />
              Start Tournament
            </button>
            
            <button 
              type="button" 
              className="btn btn-secondary w-full flex items-center justify-center"
              onClick={toggleSettings}
            >
              <Settings size={20} className="mr-2" />
              Settings
            </button>
            
            <button 
              type="button" 
              className="btn btn-accent w-full flex items-center justify-center"
            >
              <Trophy size={20} className="mr-2" />
              Leaderboard
            </button>
          </div>
        </motion.form>

        {showSettings && (
          <motion.div 
            className="mt-6 p-4 bg-blue-50 rounded-lg"
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            transition={{ duration: 0.3 }}
          >
            <h3 className="text-lg font-medium text-blue-800 mb-3">Game Settings</h3>
            <div className="space-y-3">
              <div>
                <label className="flex items-center">
                  <input type="checkbox" className="rounded text-blue-600 focus:ring-blue-500 mr-2" />
                  <span>Enable sound effects</span>
                </label>
              </div>
              <div>
                <label className="flex items-center">
                  <input type="checkbox" className="rounded text-blue-600 focus:ring-blue-500 mr-2" />
                  <span>Enable music</span>
                </label>
              </div>
              <div>
                <label className="flex items-center">
                  <input type="checkbox" className="rounded text-blue-600 focus:ring-blue-500 mr-2" defaultChecked />
                  <span>Show tutorials</span>
                </label>
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1">
                  Difficulty
                </label>
                <select className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500">
                  <option>Easy</option>
                  <option>Medium</option>
                  <option>Hard</option>
                </select>
              </div>
            </div>
          </motion.div>
        )}
      </motion.div>

      <motion.p 
        className="mt-8 text-sm text-white text-center"
        initial={{ opacity: 0 }}
        animate={{ opacity: 0.8 }}
        transition={{ delay: 1 }}
      >
        © 2025 Boat Racing Tournament. All rights reserved.
      </motion.p>
    </motion.div>
  );
};

export default StartScreen;