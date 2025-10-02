import React, { useState } from 'react';
import { Team, Player } from '../types/game';
import { PlayerCard } from './PlayerCard';
import { Users, Settings, TrendingUp, DollarSign } from 'lucide-react';

interface TeamManagementProps {
  team: Team;
  onTeamUpdate: (team: Team) => void;
}

export function TeamManagement({ team, onTeamUpdate }: TeamManagementProps) {
  const [selectedPlayer, setSelectedPlayer] = useState<Player | null>(null);
  const [activeTab, setActiveTab] = useState<'squad' | 'tactics' | 'stats'>('squad');

  const formations = ['4-4-2', '4-3-3', '3-5-2', '5-3-2', '3-4-3'];
  const tactics: Team['tactics'][] = ['Attacking', 'Balanced', 'Defensive'];

  const handleFormationChange = (formation: string) => {
    const updatedTeam = { ...team, formation };
    onTeamUpdate(updatedTeam);
  };

  const handleTacticsChange = (newTactics: Team['tactics']) => {
    const updatedTeam = { ...team, tactics: newTactics };
    onTeamUpdate(updatedTeam);
  };

  const getPositionPlayers = (position: Player['position']) => {
    return team.players.filter(p => p.position === position);
  };

  const getTeamStats = () => {
    const totalPlayers = team.players.length;
    const avgAge = team.players.reduce((sum, p) => sum + p.age, 0) / totalPlayers;
    const avgRating = team.players.reduce((sum, p) => {
      const rating = (p.stats.speed + p.stats.strength + p.stats.skill + p.stats.stamina + p.stats.experience) / 5;
      return sum + rating;
    }, 0) / totalPlayers;
    const totalValue = team.players.reduce((sum, p) => sum + p.value, 0);
    const injuredCount = team.players.filter(p => p.injured).length;

    return { avgAge, avgRating, totalValue, injuredCount };
  };

  const stats = getTeamStats();

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <div>
          <h2 className="text-2xl font-bold text-gray-800">{team.name}</h2>
          <div className="flex items-center space-x-4 mt-2 text-sm text-gray-600">
            <span>W: {team.wins}</span>
            <span>D: {team.draws}</span>
            <span>L: {team.losses}</span>
            <span className={`font-semibold ${team.morale > 70 ? 'text-green-600' : team.morale > 40 ? 'text-yellow-600' : 'text-red-600'}`}>
              Morale: {team.morale}%
            </span>
          </div>
        </div>
        <div className="text-right">
          <div className="text-green-600 font-bold text-xl">
            ${(team.budget / 1000000).toFixed(1)}M
          </div>
          <div className="text-sm text-gray-500">Budget</div>
        </div>
      </div>

      <div className="flex space-x-1 mb-6">
        {[
          { id: 'squad', label: 'Squad', icon: Users },
          { id: 'tactics', label: 'Tactics', icon: Settings },
          { id: 'stats', label: 'Stats', icon: TrendingUp }
        ].map(({ id, label, icon: Icon }) => (
          <button
            key={id}
            onClick={() => setActiveTab(id as any)}
            className={`flex items-center space-x-2 px-4 py-2 rounded-lg transition-colors ${
              activeTab === id
                ? 'bg-blue-500 text-white'
                : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
            }`}
          >
            <Icon className="w-4 h-4" />
            <span>{label}</span>
          </button>
        ))}
      </div>

      {activeTab === 'squad' && (
        <div>
          {(['GK', 'DEF', 'MID', 'FWD'] as Player['position'][]).map(position => (
            <div key={position} className="mb-6">
              <h3 className="text-lg font-semibold mb-3 text-gray-800">
                {position === 'GK' ? 'Goalkeepers' : 
                 position === 'DEF' ? 'Defenders' :
                 position === 'MID' ? 'Midfielders' : 'Forwards'}
              </h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {getPositionPlayers(position).map(player => (
                  <PlayerCard
                    key={player.id}
                    player={player}
                    selected={selectedPlayer?.id === player.id}
                    onClick={() => setSelectedPlayer(player)}
                  />
                ))}
              </div>
            </div>
          ))}
        </div>
      )}

      {activeTab === 'tactics' && (
        <div className="space-y-6">
          <div>
            <h3 className="text-lg font-semibold mb-3 text-gray-800">Formation</h3>
            <div className="grid grid-cols-3 gap-3">
              {formations.map(formation => (
                <button
                  key={formation}
                  onClick={() => handleFormationChange(formation)}
                  className={`p-3 rounded-lg border-2 transition-colors ${
                    team.formation === formation
                      ? 'border-blue-500 bg-blue-50 text-blue-700'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  {formation}
                </button>
              ))}
            </div>
          </div>

          <div>
            <h3 className="text-lg font-semibold mb-3 text-gray-800">Tactics</h3>
            <div className="grid grid-cols-3 gap-3">
              {tactics.map(tactic => (
                <button
                  key={tactic}
                  onClick={() => handleTacticsChange(tactic)}
                  className={`p-3 rounded-lg border-2 transition-colors ${
                    team.tactics === tactic
                      ? 'border-blue-500 bg-blue-50 text-blue-700'
                      : 'border-gray-200 hover:border-gray-300'
                  }`}
                >
                  {tactic}
                </button>
              ))}
            </div>
          </div>
        </div>
      )}

      {activeTab === 'stats' && (
        <div className="grid grid-cols-2 md:grid-cols-4 gap-6">
          <div className="bg-blue-50 p-4 rounded-lg">
            <div className="text-2xl font-bold text-blue-600">{stats.avgAge.toFixed(1)}</div>
            <div className="text-sm text-gray-600">Average Age</div>
          </div>
          <div className="bg-green-50 p-4 rounded-lg">
            <div className="text-2xl font-bold text-green-600">{stats.avgRating.toFixed(1)}</div>
            <div className="text-sm text-gray-600">Team Rating</div>
          </div>
          <div className="bg-yellow-50 p-4 rounded-lg">
            <div className="text-2xl font-bold text-yellow-600">${(stats.totalValue / 1000000).toFixed(1)}M</div>
            <div className="text-sm text-gray-600">Squad Value</div>
          </div>
          <div className="bg-red-50 p-4 rounded-lg">
            <div className="text-2xl font-bold text-red-600">{stats.injuredCount}</div>
            <div className="text-sm text-gray-600">Injured Players</div>
          </div>
        </div>
      )}
    </div>
  );
}