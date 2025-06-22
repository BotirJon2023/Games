import React, { useState } from 'react';
import { Fighter, Tournament } from '../types/Fighter';
import { FighterCard } from './FighterCard';
import { Trophy, Zap, Users, Award } from 'lucide-react';

interface TournamentBracketProps {
  tournament: Tournament;
  onFightSelected: (fighter1: Fighter, fighter2: Fighter) => void;
}

export const TournamentBracket: React.FC<TournamentBracketProps> = ({
  tournament,
  onFightSelected
}) => {
  const [selectedFight, setSelectedFight] = useState<[Fighter, Fighter] | null>(null);

  const generateBracket = (fighters: Fighter[]) => {
    const rounds = [];
    let currentRound = [...fighters];

    while (currentRound.length > 1) {
      rounds.push([...currentRound]);
      const nextRound = [];
      
      for (let i = 0; i < currentRound.length; i += 2) {
        if (i + 1 < currentRound.length) {
          // For demo purposes, randomly advance one fighter
          nextRound.push(Math.random() < 0.5 ? currentRound[i] : currentRound[i + 1]);
        } else {
          nextRound.push(currentRound[i]);
        }
      }
      
      currentRound = nextRound;
    }

    if (currentRound.length === 1) {
      rounds.push(currentRound);
    }

    return rounds;
  };

  const bracketRounds = generateBracket(tournament.fighters);

  const getRoundName = (roundIndex: number, totalRounds: number) => {
    const roundsFromEnd = totalRounds - roundIndex - 1;
    switch (roundsFromEnd) {
      case 0: return 'Champion';
      case 1: return 'Finals';
      case 2: return 'Semifinals';
      case 3: return 'Quarterfinals';
      default: return `Round ${roundIndex + 1}`;
    }
  };

  const handleFightClick = (fighter1: Fighter, fighter2: Fighter) => {
    setSelectedFight([fighter1, fighter2]);
    onFightSelected(fighter1, fighter2);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 p-6">
      {/* Tournament Header */}
      <div className="text-center mb-8">
        <div className="flex items-center justify-center space-x-3 mb-4">
          <Trophy className="w-8 h-8 text-yellow-400" />
          <h1 className="text-4xl font-bold text-white">{tournament.name}</h1>
          <Trophy className="w-8 h-8 text-yellow-400" />
        </div>
        
        <div className="flex items-center justify-center space-x-8 text-lg">
          <div className="flex items-center space-x-2 text-blue-400">
            <Users className="w-5 h-5" />
            <span>{tournament.fighters.length} Fighters</span>
          </div>
          <div className="flex items-center space-x-2 text-green-400">
            <Award className="w-5 h-5" />
            <span>{tournament.division} Division</span>
          </div>
          <div className="flex items-center space-x-2 text-purple-400">
            <Zap className="w-5 h-5" />
            <span>Round {tournament.currentRound}</span>
          </div>
        </div>
      </div>

      {/* Tournament Bracket */}
      <div className="max-w-7xl mx-auto">
        <div className="flex justify-between space-x-4 overflow-x-auto pb-4">
          {bracketRounds.map((round, roundIndex) => (
            <div key={roundIndex} className="flex-shrink-0">
              <h3 className="text-white font-bold text-center mb-4 text-lg">
                {getRoundName(roundIndex, bracketRounds.length)}
              </h3>
              
              <div className="space-y-4">
                {roundIndex === bracketRounds.length - 1 ? (
                  // Champion
                  <div className="relative">
                    <div className="absolute inset-0 bg-gradient-to-br from-yellow-400 to-orange-500 rounded-xl blur opacity-75"></div>
                    <div className="relative bg-gray-900 rounded-xl p-2 border-2 border-yellow-400">
                      <FighterCard 
                        fighter={round[0]} 
                        showStats={false}
                      />
                      <div className="text-center mt-2">
                        <Trophy className="w-6 h-6 text-yellow-400 mx-auto" />
                        <span className="text-yellow-400 font-bold">CHAMPION</span>
                      </div>
                    </div>
                  </div>
                ) : (
                  // Regular rounds
                  round.map((fighter, fighterIndex) => {
                    if (fighterIndex % 2 === 0 && fighterIndex + 1 < round.length) {
                      const opponent = round[fighterIndex + 1];
                      return (
                        <div 
                          key={`${fighter.id}-${opponent.id}`}
                          className="relative group cursor-pointer"
                          onClick={() => handleFightClick(fighter, opponent)}
                        >
                          <div className="space-y-2 p-2 bg-gray-800/50 rounded-lg hover:bg-gray-700/50 transition-colors">
                            <div className="w-64">
                              <FighterCard 
                                fighter={fighter} 
                                showStats={false}
                                isSelected={selectedFight?.[0]?.id === fighter.id && selectedFight?.[1]?.id === opponent.id}
                              />
                            </div>
                            <div className="text-center text-white font-bold">VS</div>
                            <div className="w-64">
                              <FighterCard 
                                fighter={opponent} 
                                showStats={false}
                                isSelected={selectedFight?.[0]?.id === fighter.id && selectedFight?.[1]?.id === opponent.id}
                              />
                            </div>
                          </div>
                          
                          {/* Connection Lines */}
                          {roundIndex < bracketRounds.length - 1 && (
                            <div className="absolute top-1/2 -right-4 w-4 h-0.5 bg-gray-500"></div>
                          )}
                        </div>
                      );
                    }
                    return null;
                  })
                )}
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Fight Selection Info */}
      {selectedFight && (
        <div className="fixed bottom-6 left-1/2 transform -translate-x-1/2 bg-black/90 rounded-lg p-4 text-center">
          <p className="text-white mb-2">
            Selected Fight: <span className="text-blue-400">{selectedFight[0].name}</span> vs <span className="text-red-400">{selectedFight[1].name}</span>
          </p>
          <p className="text-gray-400 text-sm">Click "Enter Arena" to simulate this fight</p>
        </div>
      )}
    </div>
  );
};