import React from 'react';
import { Tournament } from '../types/tournament';
import { Trophy, Users } from 'lucide-react';

interface TournamentBracketProps {
  tournament: Tournament;
}

const TournamentBracket: React.FC<TournamentBracketProps> = ({ tournament }) => {
  const getRoundName = (roundIndex: number, totalRounds: number) => {
    const remainingRounds = totalRounds - roundIndex;
    switch (remainingRounds) {
      case 1: return 'Final';
      case 2: return 'Semi-Final';
      case 3: return 'Quarter-Final';
      default: return `Round ${roundIndex + 1}`;
    }
  };

  const totalRounds = Math.log2(tournament.players.length);

  return (
    <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-green-700/30">
      <h3 className="text-lg font-semibold text-white mb-4 flex items-center">
        <Trophy className="h-5 w-5 mr-2" />
        Tournament Bracket
      </h3>

      <div className="space-y-6">
        {tournament.rounds.map((round, roundIndex) => (
          <div key={round.id} className="space-y-3">
            <h4 className="text-md font-medium text-yellow-400">
              {getRoundName(roundIndex, totalRounds)}
            </h4>
            
            <div className="space-y-2">
              {round.matches.map((match) => (
                <div
                  key={match.id}
                  className={`p-3 rounded-lg border ${
                    match.status === 'completed'
                      ? 'bg-green-900/30 border-green-600/50'
                      : match.status === 'active'
                      ? 'bg-yellow-900/30 border-yellow-600/50'
                      : 'bg-white/5 border-green-700/20'
                  }`}
                >
                  <div className="flex justify-between items-center">
                    <div className="space-y-1">
                      <div className={`text-sm ${
                        match.winner?.id === match.player1.id 
                          ? 'text-green-400 font-semibold' 
                          : 'text-white'
                      }`}>
                        {match.player1.name}
                      </div>
                      <div className={`text-sm ${
                        match.winner?.id === match.player2.id 
                          ? 'text-green-400 font-semibold' 
                          : 'text-white'
                      }`}>
                        {match.player2.name}
                      </div>
                    </div>
                    
                    <div className="text-right">
                      {match.status === 'completed' && (
                        <div className="text-xs text-green-100">
                          <div>{501 - match.score1}</div>
                          <div>{501 - match.score2}</div>
                        </div>
                      )}
                      {match.status === 'active' && (
                        <div className="text-xs text-yellow-400">
                          Playing...
                        </div>
                      )}
                    </div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        ))}

        {tournament.winner && (
          <div className="mt-6 p-4 bg-gradient-to-r from-yellow-500/20 to-orange-500/20 rounded-lg border border-yellow-500/30">
            <div className="flex items-center justify-center space-x-2">
              <Trophy className="h-5 w-5 text-yellow-400" />
              <span className="text-yellow-400 font-semibold">Champion</span>
            </div>
            <div className="text-center mt-2">
              <div className="text-white font-bold">{tournament.winner.name}</div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default TournamentBracket;