import React from 'react';
import { GameEvent } from '../types/game';
import { Activity, Target, AlertTriangle, Users, Send, Zap } from 'lucide-react';

interface GameLogProps {
  events: GameEvent[];
}

export const GameLog: React.FC<GameLogProps> = ({ events }) => {
  const getEventIcon = (type: GameEvent['type']) => {
    switch (type) {
      case 'try':
        return <Target className="text-green-500" size={16} />;
      case 'conversion':
        return <Target className="text-blue-500" size={16} />;
      case 'penalty':
        return <AlertTriangle className="text-yellow-500" size={16} />;
      case 'turnover':
        return <Zap className="text-purple-500" size={16} />;
      case 'tackle':
        return <Users className="text-red-500" size={16} />;
      case 'pass':
        return <Send className="text-blue-400" size={16} />;
      default:
        return <Activity className="text-gray-500" size={16} />;
    }
  };

  const formatTime = (timestamp: number) => {
    const mins = Math.floor(timestamp / 60);
    const secs = Math.floor(timestamp % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="bg-white rounded-lg shadow-lg p-4">
      <h3 className="text-lg font-bold mb-4 flex items-center gap-2">
        <Activity size={20} />
        Game Log
      </h3>
      
      <div className="max-h-80 overflow-y-auto space-y-2">
        {events.length === 0 ? (
          <div className="text-gray-500 text-center py-8">
            <Activity size={32} className="mx-auto mb-2 opacity-50" />
            <p>Game events will appear here...</p>
          </div>
        ) : (
          events.map((event) => (
            <div
              key={event.id}
              className="flex items-start gap-3 p-3 bg-gray-50 rounded-lg hover:bg-gray-100 transition-colors duration-200"
            >
              <div className="mt-0.5">{getEventIcon(event.type)}</div>
              <div className="flex-1">
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-xs font-medium text-gray-500">
                    {formatTime(event.timestamp)}
                  </span>
                  <span
                    className={`w-2 h-2 rounded-full ${
                      event.team === 'home' ? 'bg-blue-500' : 'bg-red-500'
                    }`}
                  />
                </div>
                <p className="text-sm text-gray-800">{event.message}</p>
                {event.player && (
                  <p className="text-xs text-gray-600 mt-1">Player: {event.player}</p>
                )}
              </div>
            </div>
          ))
        )}
      </div>
    </div>
  );
};