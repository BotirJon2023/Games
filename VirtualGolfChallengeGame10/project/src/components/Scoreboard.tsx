import React from 'react';
import { Player } from '../types/golf';

interface ScoreboardProps {
  players: Player[];
  currentHole: number;
  totalHoles: number;
  gamePhase: string;
}

const Scoreboard: React.FC<ScoreboardProps> = ({
  players,
  currentHole,
  totalHoles,
  gamePhase,
}) => {
  const getScoreLabel = (score: number, par: number) => {
    const diff = score - par;
    if (score === 1) return { label: 'Hole in One!', color: 'text-yellow-400' };
    if (diff === -3) return { label: 'Albatross', color: 'text-yellow-300' };
    if (diff === -2) return { label: 'Eagle', color: 'text-emerald-400' };
    if (diff === -1) return { label: 'Birdie', color: 'text-green-400' };
    if (diff === 0) return { label: 'Par', color: 'text-blue-400' };
    if (diff === 1) return { label: 'Bogey', color: 'text-orange-400' };
    if (diff === 2) return { label: 'Double Bogey', color: 'text-red-400' };
    return { label: `+${diff}`, color: 'text-red-500' };
  };

  const winner = gamePhase === 'game-over'
    ? players.reduce((min, p) => p.totalScore < min.totalScore ? p : min, players[0])
    : null;

  return (
    <div className="bg-gradient-to-br from-slate-800 to-slate-900 rounded-xl shadow-2xl p-6 text-white">
      <div className="flex items-center justify-between mb-4">
        <h2 className="text-2xl font-bold bg-gradient-to-r from-amber-400 to-orange-400 bg-clip-text text-transparent">
          Scoreboard
        </h2>
        <div className="text-sm bg-slate-700 px-3 py-1 rounded-full">
          Hole {currentHole + 1} / {totalHoles}
        </div>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full">
          <thead>
            <tr className="border-b border-slate-600">
              <th className="text-left py-2 px-3 text-slate-400">Player</th>
              {Array.from({ length: totalHoles }, (_, i) => (
                <th key={i} className="text-center py-2 px-2 text-slate-400 text-sm">
                  {i + 1}
                </th>
              ))}
              <th className="text-center py-2 px-3 text-slate-400">Total</th>
            </tr>
          </thead>
          <tbody>
            {players.map((player, idx) => (
              <tr
                key={player.id}
                className={`border-b border-slate-700 ${winner?.id === player.id ? 'bg-amber-500/20' : ''}`}
              >
                <td className="py-3 px-3">
                  <div className="flex items-center gap-2">
                    <div
                      className="w-4 h-4 rounded-full shadow-lg"
                      style={{ backgroundColor: player.color }}
                    />
                    <span className="font-semibold">{player.name}</span>
                    {player.isComputer && (
                      <span className="text-xs bg-blue-500 px-2 py-0.5 rounded-full">AI</span>
                    )}
                  </div>
                </td>
                {Array.from({ length: totalHoles }, (_, i) => (
                  <td key={i} className="text-center py-3 px-2">
                    {player.scores[i] !== undefined ? (
                      <div className="flex flex-col items-center">
                        <span className="font-bold">{player.scores[i]}</span>
                        {player.scores[i] <= 5 && (
                          <span className={`text-xs ${getScoreLabel(player.scores[i], 3).color}`}>
                            {getScoreLabel(player.scores[i], 3).label}
                          </span>
                        )}
                      </div>
                    ) : (
                      <span className="text-slate-600">-</span>
                    )}
                  </td>
                ))}
                <td className="text-center py-3 px-3">
                  <span className={`font-bold text-lg ${winner?.id === player.id ? 'text-amber-400' : 'text-white'}`}>
                    {player.totalScore}
                  </span>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      {gamePhase === 'game-over' && winner && (
        <div className="mt-6 text-center">
          <div className="inline-block bg-gradient-to-r from-amber-400 to-orange-500 text-slate-900 px-8 py-4 rounded-xl shadow-lg">
            <div className="text-2xl font-bold">Winner!</div>
            <div className="text-lg">{winner.name}</div>
            <div className="text-sm mt-1">Score: {winner.totalScore}</div>
          </div>
        </div>
      )}
    </div>
  );
};

export default Scoreboard;
