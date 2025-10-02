import React, { useState, useEffect } from 'react';
import { Team, Match } from './types/game';
import { generateTeamPlayers } from './utils/playerGenerator';
import { MatchSimulator } from './utils/matchSimulator';
import { AIEngine } from './utils/aiEngine';
import { TeamManagement } from './components/TeamManagement';
import { MatchDisplay } from './components/MatchDisplay';
import { Play, Pause, RotateCcw, Trophy, Users, Calendar } from 'lucide-react';

function App() {
  const [userTeam, setUserTeam] = useState<Team | null>(null);
  const [aiTeam, setAiTeam] = useState<Team | null>(null);
  const [currentMatch, setCurrentMatch] = useState<Match | null>(null);
  const [isSimulating, setIsSimulating] = useState(false);
  const [activeView, setActiveView] = useState<'team' | 'match' | 'league'>('team');
  const [aiEngine] = useState(new AIEngine('medium'));

  useEffect(() => {
    initializeTeams();
  }, []);

  const initializeTeams = () => {
    const userTeamData: Team = {
      id: 'user_team',
      name: 'Your Team',
      players: generateTeamPlayers(),
      formation: '4-4-2',
      tactics: 'Balanced',
      morale: 75,
      budget: 50000000, // 50M
      wins: 0,
      draws: 0,
      losses: 0
    };

    const aiTeamData: Team = {
      id: 'ai_team',
      name: 'AI Opponents',
      players: generateTeamPlayers(),
      formation: '4-3-3',
      tactics: 'Attacking',
      morale: 70,
      budget: 45000000, // 45M
      wins: 0,
      draws: 0,
      losses: 0
    };

    setUserTeam(userTeamData);
    setAiTeam(aiTeamData);
  };

  const startMatch = async () => {
    if (!userTeam || !aiTeam) return;

    setIsSimulating(true);
    setActiveView('match');

    const simulator = new MatchSimulator(userTeam, aiTeam);
    
    const finalMatch = await simulator.simulateMatch((updatedMatch) => {
      setCurrentMatch(updatedMatch);
    });

    // Update teams with match results
    setUserTeam(finalMatch.homeTeam);
    setAiTeam(finalMatch.awayTeam);
    setCurrentMatch(finalMatch);
    setIsSimulating(false);
  };

  const resetSeason = () => {
    initializeTeams();
    setCurrentMatch(null);
    setActiveView('team');
  };

  const handleTeamUpdate = (updatedTeam: Team) => {
    setUserTeam(updatedTeam);
  };

  if (!userTeam || !aiTeam) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100 flex items-center justify-center">
        <div className="text-center">
          <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
          <p className="text-gray-600">Initializing teams...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 to-indigo-100">
      <header className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-3">
              <Trophy className="w-8 h-8 text-blue-600" />
              <h1 className="text-2xl font-bold text-gray-900">AI Sports Manager</h1>
            </div>
            
            <nav className="flex space-x-1">
              {[
                { id: 'team', label: 'Team', icon: Users },
                { id: 'match', label: 'Match', icon: Play },
                { id: 'league', label: 'League', icon: Calendar }
              ].map(({ id, label, icon: Icon }) => (
                <button
                  key={id}
                  onClick={() => setActiveView(id as any)}
                  className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-colors ${
                    activeView === id
                      ? 'bg-blue-500 text-white'
                      : 'text-gray-600 hover:bg-gray-100'
                  }`}
                >
                  <Icon className="w-4 h-4" />
                  <span>{label}</span>
                </button>
              ))}
            </nav>

            <div className="flex items-center space-x-3">
              <button
                onClick={startMatch}
                disabled={isSimulating}
                className="flex items-center space-x-2 bg-green-500 hover:bg-green-600 disabled:bg-gray-400 text-white px-4 py-2 rounded-lg transition-colors"
              >
                {isSimulating ? (
                  <>
                    <Pause className="w-4 h-4" />
                    <span>Simulating...</span>
                  </>
                ) : (
                  <>
                    <Play className="w-4 h-4" />
                    <span>Start Match</span>
                  </>
                )}
              </button>
              
              <button
                onClick={resetSeason}
                className="flex items-center space-x-2 bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded-lg transition-colors"
              >
                <RotateCcw className="w-4 h-4" />
                <span>Reset</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {activeView === 'team' && (
          <div className="space-y-8">
            <TeamManagement team={userTeam} onTeamUpdate={handleTeamUpdate} />
          </div>
        )}

        {activeView === 'match' && (
          <div className="space-y-8">
            {currentMatch ? (
              <MatchDisplay match={currentMatch} />
            ) : (
              <div className="bg-white rounded-lg shadow-lg p-8 text-center">
                <Trophy className="w-16 h-16 text-gray-400 mx-auto mb-4" />
                <h2 className="text-2xl font-bold text-gray-800 mb-2">Ready for Match</h2>
                <p className="text-gray-600 mb-6">
                  Your team is ready to face the AI opponents. Click "Start Match" to begin the simulation.
                </p>
                <div className="grid grid-cols-2 gap-8 max-w-2xl mx-auto">
                  <div className="text-center">
                    <h3 className="text-lg font-semibold text-blue-600">{userTeam.name}</h3>
                    <p className="text-sm text-gray-500">{userTeam.formation} - {userTeam.tactics}</p>
                    <p className="text-sm text-gray-500">Morale: {userTeam.morale}%</p>
                  </div>
                  <div className="text-center">
                    <h3 className="text-lg font-semibold text-red-600">{aiTeam.name}</h3>
                    <p className="text-sm text-gray-500">{aiTeam.formation} - {aiTeam.tactics}</p>
                    <p className="text-sm text-gray-500">Morale: {aiTeam.morale}%</p>
                  </div>
                </div>
              </div>
            )}
          </div>
        )}

        {activeView === 'league' && (
          <div className="bg-white rounded-lg shadow-lg p-6">
            <h2 className="text-2xl font-bold text-gray-800 mb-6">League Table</h2>
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b">
                    <th className="text-left py-3 px-4">Team</th>
                    <th className="text-center py-3 px-4">Played</th>
                    <th className="text-center py-3 px-4">Won</th>
                    <th className="text-center py-3 px-4">Drawn</th>
                    <th className="text-center py-3 px-4">Lost</th>
                    <th className="text-center py-3 px-4">Points</th>
                    <th className="text-center py-3 px-4">Morale</th>
                  </tr>
                </thead>
                <tbody>
                  {[userTeam, aiTeam]
                    .sort((a, b) => (b.wins * 3 + b.draws) - (a.wins * 3 + a.draws))
                    .map((team, index) => (
                    <tr key={team.id} className={index === 0 ? 'bg-green-50' : ''}>
                      <td className="py-3 px-4 font-semibold">{team.name}</td>
                      <td className="text-center py-3 px-4">{team.wins + team.draws + team.losses}</td>
                      <td className="text-center py-3 px-4">{team.wins}</td>
                      <td className="text-center py-3 px-4">{team.draws}</td>
                      <td className="text-center py-3 px-4">{team.losses}</td>
                      <td className="text-center py-3 px-4 font-bold">{team.wins * 3 + team.draws}</td>
                      <td className="text-center py-3 px-4">
                        <span className={`font-semibold ${
                          team.morale > 70 ? 'text-green-600' : 
                          team.morale > 40 ? 'text-yellow-600' : 'text-red-600'
                        }`}>
                          {team.morale}%
                        </span>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}

export default App;