import React from 'react';
import { Team } from '../types/game';
import { Users, Zap, Shield, Star, Battery } from 'lucide-react';

interface TeamStatsProps {
  team: Team;
  title: string;
}

export const TeamStats: React.FC<TeamStatsProps> = ({ team, title }) => {
  const avgSpeed = team.players.reduce((sum, p) => sum + p.speed, 0) / team.players.length;
  const avgStrength = team.players.reduce((sum, p) => sum + p.strength, 0) / team.players.length;
  const avgSkill = team.players.reduce((sum, p) => sum + p.skill, 0) / team.players.length;
  const avgStamina = team.players.reduce((sum, p) => sum + p.currentStamina, 0) / team.players.length;

  return (
    <div className="bg-white rounded-lg shadow-lg p-4">
      <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
        <Users size={20} />
        {title}
      </h3>
      
      <div className="space-y-4">
        {/* Team Overview */}
        <div className="grid grid-cols-2 gap-4">
          <div className="text-center p-3 bg-gradient-to-r from-blue-50 to-blue-100 rounded-lg">
            <div className="text-2xl font-bold text-blue-600">{team.score}</div>
            <div className="text-sm text-blue-800">Points</div>
          </div>
          <div className="text-center p-3 bg-gradient-to-r from-green-50 to-green-100 rounded-lg">
            <div className="text-2xl font-bold text-green-600">{team.players.length}</div>
            <div className="text-sm text-green-800">Players</div>
          </div>
        </div>

        {/* Team Stats */}
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Zap size={16} className="text-yellow-500" />
              <span className="text-sm font-medium">Speed</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-20 bg-gray-200 rounded-full h-2">
                <div
                  className="bg-yellow-500 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${avgSpeed}%` }}
                />
              </div>
              <span className="text-sm font-bold w-8">{Math.round(avgSpeed)}</span>
            </div>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Shield size={16} className="text-red-500" />
              <span className="text-sm font-medium">Strength</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-20 bg-gray-200 rounded-full h-2">
                <div
                  className="bg-red-500 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${avgStrength}%` }}
                />
              </div>
              <span className="text-sm font-bold w-8">{Math.round(avgStrength)}</span>
            </div>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Star size={16} className="text-purple-500" />
              <span className="text-sm font-medium">Skill</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-20 bg-gray-200 rounded-full h-2">
                <div
                  className="bg-purple-500 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${avgSkill}%` }}
                />
              </div>
              <span className="text-sm font-bold w-8">{Math.round(avgSkill)}</span>
            </div>
          </div>

          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <Battery size={16} className="text-green-500" />
              <span className="text-sm font-medium">Stamina</span>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-20 bg-gray-200 rounded-full h-2">
                <div
                  className="bg-green-500 h-2 rounded-full transition-all duration-300"
                  style={{ width: `${avgStamina}%` }}
                />
              </div>
              <span className="text-sm font-bold w-8">{Math.round(avgStamina)}</span>
            </div>
          </div>
        </div>

        {/* Player List */}
        <div className="mt-4">
          <h4 className="text-sm font-semibold mb-2">Squad</h4>
          <div className="space-y-1 max-h-32 overflow-y-auto">
            {team.players.map((player) => (
              <div
                key={player.id}
                className="flex items-center justify-between text-xs p-2 bg-gray-50 rounded hover:bg-gray-100 transition-colors"
              >
                <div>
                  <span className="font-medium">{player.name}</span>
                  <span className="text-gray-500 ml-2">{player.position}</span>
                </div>
                <div className="flex items-center">
                  <Battery
                    size={12}
                    className={
                      player.currentStamina > 60
                        ? 'text-green-500'
                        : player.currentStamina > 30
                        ? 'text-yellow-500'
                        : 'text-red-500'
                    }
                  />
                  <span className="ml-1 w-6 text-right">
                    {Math.round(player.currentStamina)}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
};