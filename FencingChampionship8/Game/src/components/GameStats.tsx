import React from 'react';
import { BarChart, Clock, Target, Zap } from 'lucide-react';
import { GameState, CombatEvent } from '../types/game';

interface GameStatsProps {
  gameState: GameState;
}

export const GameStats: React.FC<GameStatsProps> = ({ gameState }) => {
  const calculateStats = () => {
    const [player1, player2] = gameState.players;
    if (!player1 || !player2) return null;

    const player1Events = gameState.combatLog.filter(e => e.attacker === player1.name);
    const player2Events = gameState.combatLog.filter(e => e.attacker === player2.name);

    const player1Hits = player1Events.filter(e => e.action === 'hit').length;
    const player2Hits = player2Events.filter(e => e.action === 'hit').length;

    const player1Attacks = player1Events.filter(e => e.action === 'attack' || e.action === 'hit').length;
    const player2Attacks = player2Events.filter(e => e.action === 'attack' || e.action === 'hit').length;

    const player1Accuracy = player1Attacks > 0 ? (player1Hits / player1Attacks * 100) : 0;
    const player2Accuracy = player2Attacks > 0 ? (player2Hits / player2Attacks * 100) : 0;

    const player1Parries = gameState.combatLog.filter(e => e.defender === player1.name && e.action === 'parry').length;
    const player2Parries = gameState.combatLog.filter(e => e.defender === player2.name && e.action === 'parry').length;

    return {
      player1: {
        hits: player1Hits,
        attacks: player1Attacks,
        accuracy: player1Accuracy,
        parries: player1Parries
      },
      player2: {
        hits: player2Hits,
        attacks: player2Attacks,
        accuracy: player2Accuracy,
        parries: player2Parries
      }
    };
  };

  const stats = calculateStats();
  
  if (!stats || gameState.players.length < 2) {
    return (
      <div className="bg-white rounded-lg shadow-lg p-4">
        <h3 className="text-lg font-semibold text-gray-800 mb-3 flex items-center">
          <BarChart className="w-5 h-5 mr-2" />
          Game Statistics
        </h3>
        <p className="text-gray-600">Start a match to see statistics</p>
      </div>
    );
  }

  const [player1, player2] = gameState.players;

  return (
    <div className="bg-white rounded-lg shadow-lg p-4">
      <h3 className="text-lg font-semibold text-gray-800 mb-4 flex items-center">
        <BarChart className="w-5 h-5 mr-2" />
        Match Statistics
      </h3>

      <div className="grid grid-cols-2 gap-4">
        <div className="bg-blue-50 rounded-lg p-3">
          <h4 className="font-semibold text-blue-800 mb-2">{player1.name}</h4>
          <div className="space-y-2 text-sm">
            <div className="flex items-center justify-between">
              <span className="flex items-center">
                <Target className="w-4 h-4 mr-1 text-blue-600" />
                Score
              </span>
              <span className="font-bold">{player1.score}</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Hits</span>
              <span>{stats.player1.hits}</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Attacks</span>
              <span>{stats.player1.attacks}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="flex items-center">
                <Zap className="w-4 h-4 mr-1 text-yellow-600" />
                Accuracy
              </span>
              <span>{stats.player1.accuracy.toFixed(1)}%</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Parries</span>
              <span>{stats.player1.parries}</span>
            </div>
          </div>
          
          {/* Health Bar */}
          <div className="mt-3">
            <div className="text-xs text-gray-600 mb-1">Health</div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-blue-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${(player1.health / player1.maxHealth) * 100}%` }}
              />
            </div>
          </div>
        </div>

        <div className="bg-red-50 rounded-lg p-3">
          <h4 className="font-semibold text-red-800 mb-2">{player2.name}</h4>
          <div className="space-y-2 text-sm">
            <div className="flex items-center justify-between">
              <span className="flex items-center">
                <Target className="w-4 h-4 mr-1 text-red-600" />
                Score
              </span>
              <span className="font-bold">{player2.score}</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Hits</span>
              <span>{stats.player2.hits}</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Attacks</span>
              <span>{stats.player2.attacks}</span>
            </div>
            <div className="flex items-center justify-between">
              <span className="flex items-center">
                <Zap className="w-4 h-4 mr-1 text-yellow-600" />
                Accuracy
              </span>
              <span>{stats.player2.accuracy.toFixed(1)}%</span>
            </div>
            <div className="flex items-center justify-between">
              <span>Parries</span>
              <span>{stats.player2.parries}</span>
            </div>
          </div>

          {/* Health Bar */}
          <div className="mt-3">
            <div className="text-xs text-gray-600 mb-1">Health</div>
            <div className="w-full bg-gray-200 rounded-full h-2">
              <div
                className="bg-red-600 h-2 rounded-full transition-all duration-300"
                style={{ width: `${(player2.health / player2.maxHealth) * 100}%` }}
              />
            </div>
          </div>
        </div>
      </div>

      {/* Round Information */}
      <div className="mt-4 bg-gray-50 rounded-lg p-3">
        <div className="flex items-center justify-between text-sm">
          <div className="flex items-center text-gray-600">
            <Clock className="w-4 h-4 mr-1" />
            Round {gameState.currentRound}
          </div>
          <div className="text-gray-800 font-medium">
            Time: {Math.floor(gameState.timeRemaining / 60)}:
            {Math.floor(gameState.timeRemaining % 60).toString().padStart(2, '0')}
          </div>
        </div>
        
        {gameState.gameStatus === 'finished' && (
          <div className="mt-2 text-center">
            <div className="text-lg font-bold text-green-600">
              {gameState.winner} Wins the Match!
            </div>
          </div>
        )}
      </div>
    </div>
  );
};