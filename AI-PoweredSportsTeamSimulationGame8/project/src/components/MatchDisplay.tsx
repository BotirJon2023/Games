import React from 'react';
import { Match, MatchEvent } from '../types/game';
import { Clock, Target, AlertTriangle, UserX, RefreshCw } from 'lucide-react';

interface MatchDisplayProps {
  match: Match;
}

export function MatchDisplay({ match }: MatchDisplayProps) {
  const getEventIcon = (type: MatchEvent['type']) => {
    switch (type) {
      case 'goal': return <Target className="w-4 h-4 text-green-600" />;
      case 'yellow_card': return <AlertTriangle className="w-4 h-4 text-yellow-600" />;
      case 'red_card': return <AlertTriangle className="w-4 h-4 text-red-600" />;
      case 'injury': return <UserX className="w-4 h-4 text-red-600" />;
      case 'substitution': return <RefreshCw className="w-4 h-4 text-blue-600" />;
      default: return <Clock className="w-4 h-4 text-gray-600" />;
    }
  };

  const getStatusColor = () => {
    switch (match.status) {
      case 'live': return 'text-green-600 animate-pulse';
      case 'finished': return 'text-gray-600';
      case 'upcoming': return 'text-blue-600';
      default: return 'text-gray-600';
    }
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-6">
      <div className="text-center mb-6">
        <div className={`text-sm font-semibold mb-2 ${getStatusColor()}`}>
          {match.status === 'live' ? `${match.minute}'` : match.status.toUpperCase()}
        </div>
        
        <div className="flex items-center justify-between">
          <div className="text-right flex-1">
            <h2 className="text-xl font-bold text-gray-800">{match.homeTeam.name}</h2>
            <div className="text-sm text-gray-500">{match.homeTeam.formation}</div>
            <div className="text-xs text-gray-400">{match.homeTeam.tactics}</div>
          </div>
          
          <div className="mx-8">
            <div className="text-4xl font-bold text-gray-800">
              {match.homeScore} - {match.awayScore}
            </div>
          </div>
          
          <div className="text-left flex-1">
            <h2 className="text-xl font-bold text-gray-800">{match.awayTeam.name}</h2>
            <div className="text-sm text-gray-500">{match.awayTeam.formation}</div>
            <div className="text-xs text-gray-400">{match.awayTeam.tactics}</div>
          </div>
        </div>
      </div>

      {match.events.length > 0 && (
        <div className="border-t pt-4">
          <h3 className="text-lg font-semibold mb-3 text-gray-800">Match Events</h3>
          <div className="max-h-64 overflow-y-auto space-y-2">
            {match.events.slice().reverse().map((event, index) => (
              <div key={index} className="flex items-center space-x-3 p-2 bg-gray-50 rounded">
                <div className="text-sm font-semibold text-gray-600 w-8">
                  {event.minute}'
                </div>
                {getEventIcon(event.type)}
                <div className="flex-1">
                  <div className="text-sm font-medium text-gray-800">
                    {event.player}
                  </div>
                  <div className="text-xs text-gray-600">
                    {event.description}
                  </div>
                </div>
                <div className={`text-xs px-2 py-1 rounded ${
                  event.team === 'home' ? 'bg-blue-100 text-blue-800' : 'bg-red-100 text-red-800'
                }`}>
                  {event.team === 'home' ? match.homeTeam.name : match.awayTeam.name}
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {match.status === 'live' && (
        <div className="mt-4 bg-green-50 p-3 rounded">
          <div className="flex items-center justify-center space-x-2">
            <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
            <span className="text-green-700 font-medium">Match in Progress</span>
          </div>
        </div>
      )}
    </div>
  );
}