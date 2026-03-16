import { Trophy } from 'lucide-react';
import MatchCard from './MatchCard';

interface Team {
  id: string;
  name: string;
  color: string;
}

interface Match {
  id: string;
  team1_id: string;
  team2_id: string;
  team1_score: number;
  team2_score: number;
  winner_id: string | null;
  round: string;
  match_number: number;
  status: string;
}

interface TournamentBracketProps {
  teams: Team[];
  matches: Match[];
  onPlayMatch: (matchId: string) => void;
  champion: Team | null;
}

export default function TournamentBracket({
  teams,
  matches,
  onPlayMatch,
  champion,
}: TournamentBracketProps) {
  const semifinals = matches.filter((m) => m.round === 'semifinal');
  const final = matches.find((m) => m.round === 'final');

  const getTeamById = (id: string) => teams.find((t) => t.id === id);

  return (
    <div className="w-full max-w-7xl mx-auto p-8">
      <div className="mb-12 text-center">
        <h1 className="text-5xl font-bold text-gray-800 mb-4 animate-fadeIn">
          Volleyball Championship
        </h1>
        <div className="h-1 w-32 bg-gradient-to-r from-blue-500 to-blue-600 mx-auto rounded-full" />
      </div>

      <div className="flex flex-col lg:flex-row items-center justify-center gap-8 lg:gap-12">
        <div className="flex flex-col gap-6">
          <h2 className="text-2xl font-bold text-gray-700 text-center mb-2">Semi-Finals</h2>
          {semifinals.map((match) => {
            const team1 = getTeamById(match.team1_id);
            const team2 = getTeamById(match.team2_id);
            if (!team1 || !team2) return null;

            return (
              <MatchCard
                key={match.id}
                team1Name={team1.name}
                team2Name={team2.name}
                team1Color={team1.color}
                team2Color={team2.color}
                team1Score={match.team1_score}
                team2Score={match.team2_score}
                winnerId={match.winner_id}
                team1Id={team1.id}
                team2Id={team2.id}
                status={match.status}
                onPlayMatch={() => onPlayMatch(match.id)}
                round={match.round}
              />
            );
          })}
        </div>

        <div className="hidden lg:flex items-center">
          <div className="flex flex-col items-center gap-4">
            <div className="w-24 h-1 bg-gray-300 rounded" />
            <div className="text-4xl text-gray-400">→</div>
            <div className="w-24 h-1 bg-gray-300 rounded" />
          </div>
        </div>

        <div className="flex flex-col items-center gap-6">
          <h2 className="text-2xl font-bold text-gray-700 text-center mb-2">Final</h2>
          {final && (
            <>
              {(() => {
                const team1 = getTeamById(final.team1_id);
                const team2 = getTeamById(final.team2_id);
                if (!team1 || !team2) return null;

                return (
                  <MatchCard
                    key={final.id}
                    team1Name={team1.name}
                    team2Name={team2.name}
                    team1Color={team1.color}
                    team2Color={team2.color}
                    team1Score={final.team1_score}
                    team2Score={final.team2_score}
                    winnerId={final.winner_id}
                    team1Id={team1.id}
                    team2Id={team2.id}
                    status={final.status}
                    onPlayMatch={() => onPlayMatch(final.id)}
                    round={final.round}
                  />
                );
              })()}
            </>
          )}
        </div>
      </div>

      {champion && (
        <div className="mt-16 text-center animate-fadeIn">
          <div className="inline-block p-8 bg-gradient-to-r from-yellow-400 via-yellow-500 to-yellow-600 rounded-2xl shadow-2xl transform hover:scale-105 transition-transform duration-300">
            <Trophy className="w-20 h-20 text-white mx-auto mb-4 animate-bounce" />
            <h2 className="text-4xl font-bold text-white mb-2">Champion</h2>
            <div className="flex items-center justify-center gap-3">
              <div
                className="w-8 h-8 rounded-full border-4 border-white"
                style={{ backgroundColor: champion.color }}
              />
              <p className="text-3xl font-bold text-white">{champion.name}</p>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
