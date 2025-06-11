import React from 'react';
import { ScrollText } from 'lucide-react';

interface GameLogProps {
  logs: string[];
}

export const GameLog: React.FC<GameLogProps> = ({ logs }) => {
  return (
    <div className="bg-black/40 backdrop-blur-lg rounded-xl p-4 border border-purple-500/20">
      <div className="flex items-center mb-3">
        <ScrollText className="w-4 h-4 mr-2 text-gold-400" />
        <h3 className="text-sm font-medium text-gold-400">Adventure Log</h3>
      </div>
      <div className="space-y-1 max-h-40 overflow-y-auto">
        {logs.length === 0 ? (
          <p className="text-gray-400 text-sm italic">Your adventure begins...</p>
        ) : (
          logs.map((log, index) => (
            <p key={index} className="text-gray-300 text-sm leading-relaxed">
              {log}
            </p>
          ))
        )}
      </div>
    </div>
  );
};