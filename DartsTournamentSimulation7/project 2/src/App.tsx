import React, { useState, useEffect, useRef } from 'react';
import { Play, Users, Trophy, Target, RotateCcw } from 'lucide-react';
import DartBoard from './components/DartBoard';
import TournamentBracket from './components/TournamentBracket';
import PlayerStats from './components/PlayerStats';
import GameControls from './components/GameControls';
import { Player, Tournament, GameState } from './types/tournament';
import { DartGame } from './game/DartGame';

function App() {
  const [gameState, setGameState] = useState<GameState>('setup');
  const [tournament, setTournament] = useState<Tournament | null>(null);
  const [currentGame, setCurrentGame] = useState<DartGame | null>(null);
  const [players, setPlayers] = useState<Player[]>([]);
  const [selectedPlayers, setSelectedPlayers] = useState<number>(8);
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    if (gameState === 'setup') {
      generateRandomPlayers();
    }
  }, [selectedPlayers]);

  const generateRandomPlayers = () => {
    const playerNames = [
      'Alex Thunder', 'Sarah Precision', 'Mike Bullseye', 'Lisa Dartmaster',
      'John Striker', 'Emma Sharpshot', 'David Archer', 'Sophie Targeteer',
      'Ryan Dart-King', 'Rachel Precision', 'Chris Bowman', 'Mia Sharpshooter',
      'Jake Lightning', 'Anna Bullhawk', 'Sam Targetman', 'Grace Dartswoman'
    ];

    const newPlayers: Player[] = [];
    for (let i = 0; i < selectedPlayers; i++) {
      const baseSkill = 0.3 + Math.random() * 0.4; // 30-70% skill level
      newPlayers.push({
        id: i + 1,
        name: playerNames[i],
        skill: baseSkill,
        accuracy: baseSkill + Math.random() * 0.2 - 0.1,
        consistency: baseSkill + Math.random() * 0.15 - 0.075,
        wins: 0,
        losses: 0,
        totalScore: 0,
        averageScore: 0,
        bullseyes: 0,
        trebles: 0,
        doubles: 0
      });
    }
    setPlayers(newPlayers);
  };

  const startTournament = () => {
    if (players.length === 0) return;
    
    const newTournament: Tournament = {
      id: Date.now(),
      players: [...players],
      rounds: [],
      currentRound: 0,
      winner: null,
      status: 'active'
    };
    
    setTournament(newTournament);
    setGameState('tournament');
    
    // Start first round
    startNextRound(newTournament);
  };

  const startNextRound = (tournamentData: Tournament) => {
    const activePlayers = tournamentData.rounds.length === 0 
      ? tournamentData.players 
      : tournamentData.rounds[tournamentData.rounds.length - 1].winners;

    if (activePlayers.length === 1) {
      // Tournament finished
      setTournament(prev => prev ? {
        ...prev,
        winner: activePlayers[0],
        status: 'completed'
      } : null);
      setGameState('finished');
      return;
    }

    const matches = [];
    for (let i = 0; i < activePlayers.length; i += 2) {
      if (i + 1 < activePlayers.length) {
        matches.push({
          id: `${tournamentData.currentRound}-${i / 2}`,
          player1: activePlayers[i],
          player2: activePlayers[i + 1],
          winner: null,
          status: 'pending' as const,
          score1: 501,
          score2: 501
        });
      }
    }

    const newRound = {
      id: tournamentData.currentRound + 1,
      matches,
      winners: [],
      completed: false
    };

    setTournament(prev => prev ? {
      ...prev,
      rounds: [...prev.rounds, newRound],
      currentRound: prev.currentRound + 1
    } : null);

    // Start first match of the round
    if (matches.length > 0) {
      startMatch(matches[0]);
    }
  };

  const startMatch = (match: any) => {
    const game = new DartGame(match.player1, match.player2, canvasRef.current);
    setCurrentGame(game);
    setGameState('playing');
    
    // Simulate the match
    game.playMatch().then((winner) => {
      // Update tournament with match result
      setTournament(prev => {
        if (!prev) return null;
        
        const updatedRounds = [...prev.rounds];
        const currentRoundIndex = updatedRounds.length - 1;
        const currentRound = updatedRounds[currentRoundIndex];
        
        const matchIndex = currentRound.matches.findIndex(m => m.id === match.id);
        if (matchIndex !== -1) {
          currentRound.matches[matchIndex] = {
            ...match,
            winner,
            status: 'completed'
          };
          
          // Check if all matches in round are completed
          const allCompleted = currentRound.matches.every(m => m.status === 'completed');
          if (allCompleted) {
            currentRound.winners = currentRound.matches.map(m => m.winner).filter(Boolean);
            currentRound.completed = true;
            
            // Start next round after a delay
            setTimeout(() => {
              startNextRound({
                ...prev,
                rounds: updatedRounds
              });
            }, 2000);
          } else {
            // Start next match
            const nextMatch = currentRound.matches.find(m => m.status === 'pending');
            if (nextMatch) {
              setTimeout(() => startMatch(nextMatch), 1000);
            }
          }
        }
        
        return {
          ...prev,
          rounds: updatedRounds
        };
      });
    });
  };

  const resetTournament = () => {
    setTournament(null);
    setCurrentGame(null);
    setGameState('setup');
    generateRandomPlayers();
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-900 via-green-800 to-emerald-900">
      {/* Header */}
      <div className="bg-black/20 backdrop-blur-sm border-b border-green-700/50">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div className="flex items-center justify-between h-16">
            <div className="flex items-center space-x-3">
              <Target className="h-8 w-8 text-yellow-400" />
              <h1 className="text-2xl font-bold text-white">
                Darts Tournament Simulator
              </h1>
            </div>
            <div className="flex items-center space-x-4">
              {gameState !== 'setup' && (
                <button
                  onClick={resetTournament}
                  className="flex items-center space-x-2 px-4 py-2 bg-red-600 hover:bg-red-700 text-white rounded-lg transition-colors"
                >
                  <RotateCcw className="h-4 w-4" />
                  <span>Reset</span>
                </button>
              )}
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {gameState === 'setup' && (
          <div className="space-y-8">
            {/* Tournament Setup */}
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-green-700/30">
              <h2 className="text-xl font-semibold text-white mb-4 flex items-center">
                <Users className="h-5 w-5 mr-2" />
                Tournament Setup
              </h2>
              
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-green-100 mb-2">
                    Number of Players
                  </label>
                  <select
                    value={selectedPlayers}
                    onChange={(e) => setSelectedPlayers(Number(e.target.value))}
                    className="w-full p-3 bg-white/10 border border-green-700/50 rounded-lg text-white focus:ring-2 focus:ring-yellow-400 focus:border-transparent"
                  >
                    <option value={4}>4 Players</option>
                    <option value={8}>8 Players</option>
                    <option value={16}>16 Players</option>
                  </select>
                </div>
                
                <button
                  onClick={startTournament}
                  disabled={players.length === 0}
                  className="w-full flex items-center justify-center space-x-2 py-3 px-6 bg-gradient-to-r from-yellow-500 to-orange-500 hover:from-yellow-600 hover:to-orange-600 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105 disabled:opacity-50 disabled:cursor-not-allowed"
                >
                  <Play className="h-5 w-5" />
                  <span>Start Tournament</span>
                </button>
              </div>
            </div>

            {/* Players List */}
            <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-green-700/30">
              <h3 className="text-lg font-semibold text-white mb-4">Tournament Players</h3>
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
                {players.map((player) => (
                  <div key={player.id} className="bg-white/5 rounded-lg p-4 border border-green-700/20">
                    <h4 className="font-medium text-white">{player.name}</h4>
                    <div className="mt-2 space-y-1 text-sm text-green-100">
                      <div>Skill: {Math.round(player.skill * 100)}%</div>
                      <div>Accuracy: {Math.round(player.accuracy * 100)}%</div>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}

        {(gameState === 'tournament' || gameState === 'playing') && tournament && (
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            {/* Dartboard and Game Area */}
            <div className="lg:col-span-2">
              <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-green-700/30">
                <DartBoard ref={canvasRef} />
                {currentGame && (
                  <GameControls 
                    game={currentGame} 
                    gameState={gameState}
                  />
                )}
              </div>
            </div>

            {/* Tournament Info */}
            <div className="space-y-6">
              <TournamentBracket tournament={tournament} />
              {currentGame && (
                <PlayerStats 
                  player1={currentGame.player1} 
                  player2={currentGame.player2}
                />
              )}
            </div>
          </div>
        )}

        {gameState === 'finished' && tournament?.winner && (
          <div className="text-center space-y-8">
            <div className="bg-gradient-to-r from-yellow-500/20 to-orange-500/20 backdrop-blur-sm rounded-xl p-8 border border-yellow-500/30">
              <Trophy className="h-16 w-16 text-yellow-400 mx-auto mb-4" />
              <h2 className="text-4xl font-bold text-white mb-2">Tournament Champion!</h2>
              <h3 className="text-2xl font-semibold text-yellow-400 mb-4">
                {tournament.winner.name}
              </h3>
              <div className="text-green-100">
                <p>Congratulations on winning the tournament!</p>
              </div>
            </div>
            
            <button
              onClick={resetTournament}
              className="flex items-center justify-center space-x-2 mx-auto py-3 px-8 bg-gradient-to-r from-green-600 to-emerald-600 hover:from-green-700 hover:to-emerald-700 text-white font-semibold rounded-lg transition-all duration-300 transform hover:scale-105"
            >
              <Play className="h-5 w-5" />
              <span>New Tournament</span>
            </button>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;