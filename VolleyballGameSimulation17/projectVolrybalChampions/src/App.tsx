import { useEffect, useState } from 'react';
import { supabase } from './lib/supabase';
import TournamentBracket from './components/TournamentBracket';
import GameSimulator from './components/GameSimulator';
import { RefreshCw, Plus } from 'lucide-react';

interface Team {
  id: string;
  name: string;
  color: string;
  wins: number;
  losses: number;
}

interface Match {
  id: string;
  tournament_id: string;
  team1_id: string;
  team2_id: string;
  team1_score: number;
  team2_score: number;
  winner_id: string | null;
  round: string;
  match_number: number;
  status: string;
}

interface Tournament {
  id: string;
  name: string;
  status: string;
}

function App() {
  const [teams, setTeams] = useState<Team[]>([]);
  const [matches, setMatches] = useState<Match[]>([]);
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [activeMatch, setActiveMatch] = useState<Match | null>(null);
  const [showSimulator, setShowSimulator] = useState(false);
  const [champion, setChampion] = useState<Team | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadTournament();
  }, []);

  const loadTournament = async () => {
    setLoading(true);
    const { data: tournaments } = await supabase
      .from('tournaments')
      .select('*')
      .order('created_at', { ascending: false })
      .limit(1)
      .maybeSingle();

    if (tournaments) {
      setTournament(tournaments);
      await loadTeams();
      await loadMatches(tournaments.id);

      const { data: finalMatch } = await supabase
        .from('matches')
        .select('*')
        .eq('tournament_id', tournaments.id)
        .eq('round', 'final')
        .eq('status', 'completed')
        .maybeSingle();

      if (finalMatch && finalMatch.winner_id) {
        const { data: championTeam } = await supabase
          .from('teams')
          .select('*')
          .eq('id', finalMatch.winner_id)
          .single();
        setChampion(championTeam);
      }
    }
    setLoading(false);
  };

  const loadTeams = async () => {
    const { data } = await supabase.from('teams').select('*');
    if (data) setTeams(data);
  };

  const loadMatches = async (tournamentId: string) => {
    const { data } = await supabase
      .from('matches')
      .select('*')
      .eq('tournament_id', tournamentId)
      .order('match_number');
    if (data) setMatches(data);
  };

  const initializeTournament = async () => {
    await supabase.from('teams').delete().neq('id', '00000000-0000-0000-0000-000000000000');
    await supabase.from('matches').delete().neq('id', '00000000-0000-0000-0000-000000000000');
    await supabase.from('tournaments').delete().neq('id', '00000000-0000-0000-0000-000000000000');

    const defaultTeams = [
      { name: 'Thunder Strikers', color: '#3B82F6' },
      { name: 'Wave Warriors', color: '#10B981' },
      { name: 'Fire Phoenix', color: '#EF4444' },
      { name: 'Storm Titans', color: '#F59E0B' },
    ];

    const { data: insertedTeams } = await supabase
      .from('teams')
      .insert(defaultTeams)
      .select();

    if (!insertedTeams) return;

    const { data: newTournament } = await supabase
      .from('tournaments')
      .insert({ name: 'Volleyball Championship 2024', status: 'in_progress' })
      .select()
      .single();

    if (!newTournament) return;

    const semifinalMatches = [
      {
        tournament_id: newTournament.id,
        team1_id: insertedTeams[0].id,
        team2_id: insertedTeams[1].id,
        round: 'semifinal',
        match_number: 1,
        status: 'pending',
      },
      {
        tournament_id: newTournament.id,
        team1_id: insertedTeams[2].id,
        team2_id: insertedTeams[3].id,
        round: 'semifinal',
        match_number: 2,
        status: 'pending',
      },
    ];

    await supabase.from('matches').insert(semifinalMatches);

    const finalMatch = {
      tournament_id: newTournament.id,
      team1_id: insertedTeams[0].id,
      team2_id: insertedTeams[2].id,
      round: 'final',
      match_number: 3,
      status: 'pending',
    };

    await supabase.from('matches').insert(finalMatch);

    await loadTournament();
  };

  const handlePlayMatch = async (matchId: string) => {
    const match = matches.find((m) => m.id === matchId);
    if (!match) return;

    await supabase
      .from('matches')
      .update({ status: 'in_progress' })
      .eq('id', matchId);

    setActiveMatch(match);
    setShowSimulator(true);
  };

  const handleMatchComplete = async (
    winnerId: string,
    team1Score: number,
    team2Score: number
  ) => {
    if (!activeMatch) return;

    await supabase
      .from('matches')
      .update({
        winner_id: winnerId,
        team1_score: team1Score,
        team2_score: team2Score,
        status: 'completed',
      })
      .eq('id', activeMatch.id);

    if (activeMatch.round === 'semifinal') {
      const allSemifinals = matches.filter((m) => m.round === 'semifinal');
      const otherSemifinal = allSemifinals.find((m) => m.id !== activeMatch.id);

      if (otherSemifinal?.winner_id) {
        const finalMatch = matches.find((m) => m.round === 'final');
        if (finalMatch) {
          await supabase
            .from('matches')
            .update({
              team1_id: otherSemifinal.winner_id,
              team2_id: winnerId,
            })
            .eq('id', finalMatch.id);
        }
      }
    }

    if (activeMatch.round === 'final') {
      const { data: championTeam } = await supabase
        .from('teams')
        .select('*')
        .eq('id', winnerId)
        .single();
      setChampion(championTeam);

      await supabase
        .from('tournaments')
        .update({ status: 'completed' })
        .eq('id', tournament?.id);
    }

    setShowSimulator(false);
    setActiveMatch(null);
    if (tournament) {
      await loadMatches(tournament.id);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-blue-50 flex items-center justify-center">
        <div className="text-2xl font-semibold text-gray-600 animate-pulse">
          Loading...
        </div>
      </div>
    );
  }

  if (!tournament) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-blue-50 flex items-center justify-center p-8">
        <div className="text-center">
          <h1 className="text-5xl font-bold text-gray-800 mb-6">
            Volleyball Championship
          </h1>
          <p className="text-xl text-gray-600 mb-8">
            Initialize a new tournament with 4 teams
          </p>
          <button
            onClick={initializeTournament}
            className="flex items-center gap-2 px-8 py-4 bg-gradient-to-r from-blue-500 to-blue-600 text-white text-lg font-semibold rounded-lg shadow-lg hover:from-blue-600 hover:to-blue-700 transition-all duration-300 transform hover:scale-105 mx-auto"
          >
            <Plus className="w-6 h-6" />
            Start New Tournament
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-blue-50 py-8">
      <div className="max-w-7xl mx-auto px-4">
        <div className="flex justify-end mb-4">
          <button
            onClick={initializeTournament}
            className="flex items-center gap-2 px-4 py-2 bg-white text-gray-700 font-semibold rounded-lg shadow hover:shadow-md transition-all duration-300 border-2 border-gray-200 hover:border-blue-400"
          >
            <RefreshCw className="w-4 h-4" />
            New Tournament
          </button>
        </div>

        <TournamentBracket
          teams={teams}
          matches={matches}
          onPlayMatch={handlePlayMatch}
          champion={champion}
        />
      </div>

      {showSimulator && activeMatch && (
        <GameSimulator
          team1={teams.find((t) => t.id === activeMatch.team1_id)!}
          team2={teams.find((t) => t.id === activeMatch.team2_id)!}
          onComplete={handleMatchComplete}
          onClose={() => {
            setShowSimulator(false);
            setActiveMatch(null);
          }}
        />
      )}
    </div>
  );
}

export default App;
