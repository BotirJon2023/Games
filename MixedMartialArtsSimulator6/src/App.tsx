import React, { useState } from 'react';
import { Fighter, Tournament, WeightDivision } from './types/Fighter';
import { FighterGenerator } from './utils/FighterGenerator';
import { FighterRoster } from './components/FighterRoster';
import { CombatArena } from './components/CombatArena';
import { TournamentBracket } from './components/TournamentBracket';
import { Swords, Users, Trophy, Home, Zap } from 'lucide-react';

type AppView = 'home' | 'roster' | 'arena' | 'tournament';

function App() {
  const [currentView, setCurrentView] = useState<AppView>('home');
  const [selectedFighters, setSelectedFighters] = useState<[Fighter, Fighter] | null>(null);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [fightResults, setFightResults] = useState<any[]>([]);

  const handleFightersSelected = (fighter1: Fighter, fighter2: Fighter) => {
    setSelectedFighters([fighter1, fighter2]);
    setCurrentView('arena');
  };

  const handleFightComplete = (result: any) => {
    setFightResults(prev => [result, ...prev.slice(0, 9)]);
    // Update fighter records
    result.winner.record.wins++;
    result.loser.record.losses++;
    
    if (result.method === 'KO/TKO') {
      result.winner.record.koTko++;
    } else if (result.method === 'Submission') {
      result.winner.record.submissions++;
    }
  };

  const createTournament = (division: WeightDivision) => {
    const fighters = FighterGenerator.generateFighterRoster(division, 8);
    const newTournament: Tournament = {
      id: `tournament_${Date.now()}`,
      name: `${division} Championship`,
      division,
      fighters,
      bracket: { rounds: [], results: [] },
      currentRound: 1,
      isComplete: false
    };
    
    setTournament(newTournament);
    setCurrentView('tournament');
  };

  const renderNavigation = () => (
    <nav className="fixed top-0 left-0 right-0 z-50 bg-black/90 backdrop-blur-sm border-b border-gray-800">
      <div className="max-w-7xl mx-auto px-6 py-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <Swords className="w-8 h-8 text-red-500" />
            <h1 className="text-2xl font-bold text-white">MMA Simulator</h1>
          </div>
          
          <div className="flex items-center space-x-4">
            <button
              onClick={() => setCurrentView('home')}
              className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-colors ${
                currentView === 'home' ? 'bg-blue-600 text-white' : 'text-gray-300 hover:text-white'
              }`}
            >
              <Home className="w-4 h-4" />
              <span>Home</span>
            </button>
            
            <button
              onClick={() => setCurrentView('roster')}
              className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-colors ${
                currentView === 'roster' ? 'bg-blue-600 text-white' : 'text-gray-300 hover:text-white'
              }`}
            >
              <Users className="w-4 h-4" />
              <span>Roster</span>
            </button>
            
            {selectedFighters && (
              <button
                onClick={() => setCurrentView('arena')}
                className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-colors ${
                  currentView === 'arena' ? 'bg-red-600 text-white' : 'text-gray-300 hover:text-white'
                }`}
              >
                <Zap className="w-4 h-4" />
                <span>Arena</span>
              </button>
            )}
            
            {tournament && (
              <button
                onClick={() => setCurrentView('tournament')}
                className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-colors ${
                  currentView === 'tournament' ? 'bg-yellow-600 text-white' : 'text-gray-300 hover:text-white'
                }`}
              >
                <Trophy className="w-4 h-4" />
                <span>Tournament</span>
              </button>
            )}
          </div>
        </div>
      </div>
    </nav>
  );

  const renderHomeView = () => (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-red-900 to-black pt-20">
      {/* Hero Section */}
      <div className="relative overflow-hidden">
        <div className="absolute inset-0">
          <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-red-500/20 rounded-full blur-3xl animate-pulse" />
          <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-blue-500/20 rounded-full blur-3xl animate-pulse" />
        </div>
        
        <div className="relative z-10 text-center py-20 px-6">
          <div className="flex items-center justify-center space-x-4 mb-8">
            <Swords className="w-16 h-16 text-red-500 animate-pulse" />
            <h1 className="text-6xl font-bold text-white">MMA SIMULATOR</h1>
            <Swords className="w-16 h-16 text-red-500 animate-pulse" />
          </div>
          
          <p className="text-2xl text-gray-300 mb-12 max-w-3xl mx-auto">
            Experience the ultimate mixed martial arts simulation with realistic combat mechanics, 
            dynamic fighter AI, and stunning visual effects.
          </p>

          {/* Quick Actions */}
          <div className="grid grid-cols-1 md:grid-cols-3 gap-8 max-w-4xl mx-auto">
            <div 
              onClick={() => setCurrentView('roster')}
              className="bg-gray-800/50 hover:bg-gray-700/50 rounded-xl p-8 cursor-pointer transition-all duration-300 hover:scale-105 border border-gray-700 hover:border-blue-500"
            >
              <Users className="w-12 h-12 text-blue-500 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">Quick Fight</h3>
              <p className="text-gray-400">Select fighters and jump into the octagon</p>
            </div>

            <div 
              onClick={() => createTournament('Lightweight')}
              className="bg-gray-800/50 hover:bg-gray-700/50 rounded-xl p-8 cursor-pointer transition-all duration-300 hover:scale-105 border border-gray-700 hover:border-yellow-500"
            >
              <Trophy className="w-12 h-12 text-yellow-500 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">Tournament</h3>
              <p className="text-gray-400">Create an 8-fighter championship bracket</p>
            </div>

            <div className="bg-gray-800/50 rounded-xl p-8 border border-gray-700">
              <Zap className="w-12 h-12 text-purple-500 mx-auto mb-4" />
              <h3 className="text-2xl font-bold text-white mb-2">Career Mode</h3>
              <p className="text-gray-400">Coming Soon - Build your fighter's legacy</p>
            </div>
          </div>
        </div>
      </div>

      {/* Recent Fight Results */}
      {fightResults.length > 0 && (
        <div className="max-w-6xl mx-auto px-6 py-12">
          <h2 className="text-3xl font-bold text-white mb-8 text-center">Recent Fights</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {fightResults.slice(0, 6).map((result, index) => (
              <div key={index} className="bg-gray-800/50 rounded-lg p-6 border border-gray-700">
                <div className="text-center mb-4">
                  <div className="text-green-400 font-bold text-lg">{result.winner.name}</div>
                  <div className="text-gray-400">defeats</div>
                  <div className="text-red-400 font-bold text-lg">{result.loser.name}</div>
                </div>
                <div className="text-center text-sm text-gray-400">
                  <div>{result.method} • Round {result.round}</div>
                  <div>{result.time}</div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Features Showcase */}
      <div className="bg-black/50 py-20">
        <div className="max-w-6xl mx-auto px-6">
          <h2 className="text-4xl font-bold text-white text-center mb-12">Simulation Features</h2>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
            <div className="text-center">
              <div className="w-16 h-16 bg-red-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <Swords className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Realistic Combat</h3>
              <p className="text-gray-400">Advanced combat engine with striking, grappling, and wrestling</p>
            </div>
            
            <div className="text-center">
              <div className="w-16 h-16 bg-blue-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <Users className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Dynamic Fighters</h3>
              <p className="text-gray-400">Unique fighting styles, stats, and AI behaviors</p>
            </div>
            
            <div className="text-center">
              <div className="w-16 h-16 bg-yellow-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <Trophy className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Tournaments</h3>
              <p className="text-gray-400">Championship brackets across multiple weight divisions</p>
            </div>
            
            <div className="text-center">
              <div className="w-16 h-16 bg-purple-600 rounded-full flex items-center justify-center mx-auto mb-4">
                <Zap className="w-8 h-8 text-white" />
              </div>
              <h3 className="text-xl font-bold text-white mb-2">Live Action</h3>
              <p className="text-gray-400">Real-time fight simulation with visual effects</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );

  return (
    <div className="min-h-screen bg-gray-900">
      {renderNavigation()}
      
      {currentView === 'home' && renderHomeView()}
      
      {currentView === 'roster' && (
        <div className="pt-20">
          <FighterRoster onFightersSelected={handleFightersSelected} />
        </div>
      )}
      
      {currentView === 'arena' && selectedFighters && (
        <div className="pt-20">
          <CombatArena
            fighter1={selectedFighters[0]}
            fighter2={selectedFighters[1]}
            onFightComplete={handleFightComplete}
          />
        </div>
      )}
      
      {currentView === 'tournament' && tournament && (
        <div className="pt-20">
          <TournamentBracket
            tournament={tournament}
            onFightSelected={handleFightersSelected}
          />
        </div>
      )}
    </div>
  );
}

export default App;