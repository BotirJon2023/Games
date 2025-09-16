import React from 'react';
import { Trophy, Users, Crown } from 'lucide-react';
import { Tournament, TournamentMatch } from '../types/game';

interface TournamentBracketProps {
  tournament: Tournament;
  onMatchSelect: (matchIndex: number) => void;
  currentMatch: number;
}

export const TournamentBracket: React.FC<TournamentBracketProps> = ({
  tournament,
  onMatchSelect,
  currentMatch
}) => {
  const getRoundMatches = (round: number): TournamentMatch[] => {
    return tournament.bracket.filter(match => match.round === round);
  };

  const getRoundName = (round: number): string => {
    const roundMatches = getRoundMatches(round);
    if (roundMatches.length === 1) return 'Final';
    if (roundMatches.length === 2) return 'Semi-Final';
    if (roundMatches.length === 4) return 'Quarter-Final';
    return `Round ${round}`;
  };

  const totalRounds = Math.max(...tournament.bracket.map(m => m.round));

  return (
    <div className="bg-white rounded-lg shadow-xl p-6 max-w-6xl mx-auto">
      <div className="flex items-center justify-center mb-6">
        <Trophy className="w-8 h-8 text-yellow-500 mr-3" />
        <h2 className="text-3xl font-bold text-gray-800">Fencing Championship</h2>
      </div>

      {tournament.champion && (
        <div className="bg-gradient-to-r from-yellow-400 to-yellow-600 rounded-lg p-4 mb-6 text-center">
          <Crown className="w-12 h-12 text-white mx-auto mb-2" />
          <h3 className="text-2xl font-bold text-white">Champion: {tournament.champion}</h3>
        </div>
      )}

      <div className="grid grid-cols-1 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {Array.from({ length: totalRounds }, (_, roundIndex) => {
          const round = roundIndex + 1;
          const matches = getRoundMatches(round);
          
          return (
            <div key={round} className="space-y-3">
              <h3 className="text-lg font-semibold text-gray-700 text-center bg-gray-100 py-2 rounded">
                {getRoundName(round)}
              </h3>
              
              {matches.map((match, index) => {
                const matchIndex = tournament.bracket.findIndex(m => 
                  m.player1 === match.player1 && m.player2 === match.player2 && m.round === match.round
                );
                const isCurrentMatch = matchIndex === currentMatch;
                const isCompleted = !!match.winner;
                
                return (
                  <div
                    key={`${round}-${index}`}
                    className={`
                      border rounded-lg p-3 cursor-pointer transition-all duration-200
                      ${isCurrentMatch ? 'border-blue-500 bg-blue-50 shadow-md' : ''}
                      ${isCompleted ? 'border-green-500 bg-green-50' : 'border-gray-300 bg-white hover:bg-gray-50'}
                    `}
                    onClick={() => onMatchSelect(matchIndex)}
                  >
                    <div className="space-y-2">
                      <div className={`
                        flex items-center justify-between text-sm
                        ${match.winner === match.player1 ? 'font-bold text-green-600' : 'text-gray-600'}
                      `}>
                        <Users className="w-4 h-4 mr-1" />
                        <span className="truncate">{match.player1}</span>
                        {match.winner === match.player1 && <Crown className="w-4 h-4 text-yellow-500" />}
                      </div>
                      
                      <div className="text-center text-xs text-gray-400">VS</div>
                      
                      <div className={`
                        flex items-center justify-between text-sm
                        ${match.winner === match.player2 ? 'font-bold text-green-600' : 'text-gray-600'}
                      `}>
                        <Users className="w-4 h-4 mr-1" />
                        <span className="truncate">{match.player2}</span>
                        {match.winner === match.player2 && <Crown className="w-4 h-4 text-yellow-500" />}
                      </div>
                    </div>
                    
                    {isCurrentMatch && !isCompleted && (
                      <div className="mt-2 text-xs text-blue-600 font-medium text-center">
                        Click to Play
                      </div>
                    )}
                  </div>
                );
              })}
            </div>
          );
        })}
      </div>

      <div className="mt-6 text-center">
        <div className="text-sm text-gray-600">
          Tournament Progress: {tournament.bracket.filter(m => m.winner).length} / {tournament.bracket.length} matches completed
        </div>
      </div>
    </div>
  );
};