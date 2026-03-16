import { Trophy, Play } from 'lucide-react';

interface MatchCardProps {
  team1Name: string;
  team2Name: string;
  team1Color: string;
  team2Color: string;
  team1Score: number;
  team2Score: number;
  winnerId: string | null;
  team1Id: string;
  team2Id: string;
  status: string;
  onPlayMatch: () => void;
  round: string;
}

export default function MatchCard({
  team1Name,
  team2Name,
  team1Color,
  team2Color,
  team1Score,
  team2Score,
  winnerId,
  team1Id,
  team2Id,
  status,
  onPlayMatch,
  round,
}: MatchCardProps) {
  const isCompleted = status === 'completed';
  const isInProgress = status === 'in_progress';

  return (
    <div className="bg-white rounded-lg shadow-lg p-4 min-w-[280px] border-2 border-gray-200 hover:border-blue-400 transition-all duration-300 transform hover:scale-105">
      <div className="text-xs font-semibold text-gray-500 mb-3 text-center uppercase tracking-wide">
        {round === 'semifinal' ? 'Semi-Final' : 'Final'}
      </div>

      <div className="space-y-2">
        <div
          className={`flex items-center justify-between p-3 rounded-lg transition-all duration-300 ${
            winnerId === team1Id ? 'ring-2 ring-yellow-400 shadow-lg' : ''
          }`}
          style={{ backgroundColor: `${team1Color}20` }}
        >
          <div className="flex items-center gap-2">
            <div
              className="w-4 h-4 rounded-full"
              style={{ backgroundColor: team1Color }}
            />
            <span className="font-semibold text-gray-800">{team1Name}</span>
          </div>
          <div className="flex items-center gap-2">
            {winnerId === team1Id && (
              <Trophy className="w-5 h-5 text-yellow-500 animate-bounce" />
            )}
            <span className="text-2xl font-bold text-gray-900">{team1Score}</span>
          </div>
        </div>

        <div
          className={`flex items-center justify-between p-3 rounded-lg transition-all duration-300 ${
            winnerId === team2Id ? 'ring-2 ring-yellow-400 shadow-lg' : ''
          }`}
          style={{ backgroundColor: `${team2Color}20` }}
        >
          <div className="flex items-center gap-2">
            <div
              className="w-4 h-4 rounded-full"
              style={{ backgroundColor: team2Color }}
            />
            <span className="font-semibold text-gray-800">{team2Name}</span>
          </div>
          <div className="flex items-center gap-2">
            {winnerId === team2Id && (
              <Trophy className="w-5 h-5 text-yellow-500 animate-bounce" />
            )}
            <span className="text-2xl font-bold text-gray-900">{team2Score}</span>
          </div>
        </div>
      </div>

      {!isCompleted && (
        <button
          onClick={onPlayMatch}
          disabled={isInProgress}
          className={`mt-4 w-full flex items-center justify-center gap-2 px-4 py-2 rounded-lg font-semibold transition-all duration-300 ${
            isInProgress
              ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
              : 'bg-gradient-to-r from-blue-500 to-blue-600 text-white hover:from-blue-600 hover:to-blue-700 shadow-md hover:shadow-lg'
          }`}
        >
          <Play className="w-4 h-4" />
          {isInProgress ? 'Match in Progress...' : 'Play Match'}
        </button>
      )}
    </div>
  );
}
