import React, { useState, useEffect } from 'react';
import { Fighter, WeightDivision } from '../types/Fighter';
import { FighterGenerator } from '../utils/FighterGenerator';
import { FighterCard } from './FighterCard';
import { Users, Plus, Filter, Search, Trophy } from 'lucide-react';

interface FighterRosterProps {
  onFightersSelected: (fighter1: Fighter, fighter2: Fighter) => void;
}

export const FighterRoster: React.FC<FighterRosterProps> = ({ onFightersSelected }) => {
  const [fighters, setFighters] = useState<Fighter[]>([]);
  const [selectedDivision, setSelectedDivision] = useState<WeightDivision>('Lightweight');
  const [selectedFighters, setSelectedFighters] = useState<Fighter[]>([]);
  const [searchTerm, setSearchTerm] = useState('');
  const [filterStyle, setFilterStyle] = useState('All');

  const divisions: WeightDivision[] = [
    'Flyweight', 'Bantamweight', 'Featherweight', 'Lightweight',
    'Welterweight', 'Middleweight', 'Light Heavyweight', 'Heavyweight'
  ];

  const fightingStyles = ['All', 'Striker', 'Grappler', 'Wrestler', 'All-Around'];

  useEffect(() => {
    const roster = FighterGenerator.generateFighterRoster(selectedDivision, 20);
    setFighters(roster);
    setSelectedFighters([]);
  }, [selectedDivision]);

  const filteredFighters = fighters.filter(fighter => {
    const matchesSearch = fighter.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
                         fighter.nickname.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesStyle = filterStyle === 'All' || fighter.fightingStyle === filterStyle;
    return matchesSearch && matchesStyle;
  });

  const handleFighterSelect = (fighter: Fighter) => {
    if (selectedFighters.includes(fighter)) {
      setSelectedFighters(selectedFighters.filter(f => f.id !== fighter.id));
    } else if (selectedFighters.length < 2) {
      setSelectedFighters([...selectedFighters, fighter]);
    } else {
      setSelectedFighters([selectedFighters[1], fighter]);
    }
  };

  const handleCreateFight = () => {
    if (selectedFighters.length === 2) {
      onFightersSelected(selectedFighters[0], selectedFighters[1]);
    }
  };

  const generateNewRoster = () => {
    const newRoster = FighterGenerator.generateFighterRoster(selectedDivision, 20);
    setFighters(newRoster);
    setSelectedFighters([]);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-blue-900 to-gray-900 p-6">
      {/* Header */}
      <div className="text-center mb-8">
        <div className="flex items-center justify-center space-x-3 mb-4">
          <Users className="w-8 h-8 text-blue-400" />
          <h1 className="text-4xl font-bold text-white">Fighter Roster</h1>
          <Trophy className="w-8 h-8 text-yellow-400" />
        </div>
        <p className="text-gray-400 text-lg">Select two fighters to create an epic matchup</p>
      </div>

      {/* Controls */}
      <div className="max-w-6xl mx-auto mb-8">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          {/* Division Selector */}
          <div>
            <label className="block text-white font-medium mb-2">Weight Division</label>
            <select
              value={selectedDivision}
              onChange={(e) => setSelectedDivision(e.target.value as WeightDivision)}
              className="w-full bg-gray-800 text-white rounded-lg px-4 py-2 border border-gray-600 focus:border-blue-500 focus:outline-none"
            >
              {divisions.map(division => (
                <option key={division} value={division}>{division}</option>
              ))}
            </select>
          </div>

          {/* Style Filter */}
          <div>
            <label className="block text-white font-medium mb-2">Fighting Style</label>
            <select
              value={filterStyle}
              onChange={(e) => setFilterStyle(e.target.value)}
              className="w-full bg-gray-800 text-white rounded-lg px-4 py-2 border border-gray-600 focus:border-blue-500 focus:outline-none"
            >
              {fightingStyles.map(style => (
                <option key={style} value={style}>{style}</option>
              ))}
            </select>
          </div>

          {/* Search */}
          <div>
            <label className="block text-white font-medium mb-2">Search Fighters</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-4 h-4 text-gray-400" />
              <input
                type="text"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                placeholder="Fighter name..."
                className="w-full bg-gray-800 text-white rounded-lg pl-10 pr-4 py-2 border border-gray-600 focus:border-blue-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Actions */}
          <div className="space-y-2">
            <button
              onClick={generateNewRoster}
              className="w-full flex items-center justify-center space-x-2 bg-purple-600 hover:bg-purple-700 px-4 py-2 rounded-lg text-white font-medium transition-colors"
            >
              <Plus className="w-4 h-4" />
              <span>New Roster</span>
            </button>
            
            <button
              onClick={handleCreateFight}
              disabled={selectedFighters.length !== 2}
              className={`w-full flex items-center justify-center space-x-2 px-4 py-2 rounded-lg font-medium transition-colors ${
                selectedFighters.length === 2
                  ? 'bg-green-600 hover:bg-green-700 text-white'
                  : 'bg-gray-700 text-gray-400 cursor-not-allowed'
              }`}
            >
              <Trophy className="w-4 h-4" />
              <span>Create Fight</span>
            </button>
          </div>
        </div>

        {/* Selected Fighters Display */}
        {selectedFighters.length > 0 && (
          <div className="bg-gray-800/50 rounded-lg p-4 mb-6">
            <h3 className="text-white font-bold mb-4 flex items-center">
              <Filter className="w-5 h-5 mr-2" />
              Selected Fighters ({selectedFighters.length}/2)
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
              {selectedFighters.map((fighter, index) => (
                <div key={fighter.id} className="relative">
                  <FighterCard 
                    fighter={fighter} 
                    isSelected={true}
                    onClick={() => handleFighterSelect(fighter)}
                  />
                  <div className="absolute top-2 left-2 bg-blue-600 text-white px-2 py-1 rounded text-xs font-bold">
                    Fighter {index + 1}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>

      {/* Fighters Grid */}
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
          {filteredFighters.map(fighter => (
            <FighterCard
              key={fighter.id}
              fighter={fighter}
              isSelected={selectedFighters.includes(fighter)}
              onClick={() => handleFighterSelect(fighter)}
            />
          ))}
        </div>

        {filteredFighters.length === 0 && (
          <div className="text-center py-12">
            <Users className="w-16 h-16 text-gray-600 mx-auto mb-4" />
            <p className="text-gray-400 text-xl">No fighters match your criteria</p>
            <p className="text-gray-500">Try adjusting your filters or search term</p>
          </div>
        )}
      </div>
    </div>
  );
};