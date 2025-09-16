import React, { useState } from 'react';
import { FencerSetup } from './components/FencerSetup';
import { GameCanvas } from './components/GameCanvas';
import { GameControls } from './components/GameControls';
import { GameStats } from './components/GameStats';
import { TournamentBracket } from './components/TournamentBracket';
import { useGameEngine } from './hooks/useGameEngine';

function App() {
  const {
    gameState,
    initializeGame,
    createTournament,
    pauseGame,
    restartGame,
    handleKeyPress,
    playTournamentMatch
  } = useGameEngine();

  const [showSettings, setShowSettings] = useState(false);

  const handleStartGame = (player1: any, player2: any) => {
    initializeGame(player1, player2);
  };

  const handleStartTournament = () => {
    createTournament();
  };

  const handleMatchSelect = (matchIndex: number) => {
    playTournamentMatch(matchIndex);
  };

  if (gameState.gameStatus === 'menu' || gameState.players.length === 0) {
    return <FencerSetup onStartGame={handleStartGame} />;
  }

  if (gameState.gameStatus === 'tournament' && gameState.tournament) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-purple-900 to-blue-900 p-4">
        <div className="max-w-7xl mx-auto">
          <div className="mb-6">
            <button
              onClick={() => window.location.reload()}
              className="bg-gray-500 hover:bg-gray-600 text-white px-4 py-2 rounded-lg transition-colors"
            >
              Back to Menu
            </button>
          </div>
          
          <TournamentBracket
            tournament={gameState.tournament}
            onMatchSelect={handleMatchSelect}
            currentMatch={gameState.tournament.currentMatch}
          />
          
          {gameState.tournament.champion && (
            <div className="mt-6 text-center">
              <button
                onClick={() => window.location.reload()}
                className="bg-blue-500 hover:bg-blue-600 text-white px-6 py-3 rounded-lg font-bold text-lg transition-colors"
              >
                Start New Tournament
              </button>
            </div>
          )}
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-blue-900 p-4">
      <div className="max-w-7xl mx-auto">
        <div className="grid grid-cols-1 xl:grid-cols-4 gap-6">
          {/* Main Game Area */}
          <div className="xl:col-span-3 space-y-4">
            <GameControls
              gameState={gameState}
              onPlayPause={pauseGame}
              onRestart={restartGame}
              onStartTournament={handleStartTournament}
              onShowSettings={() => setShowSettings(!showSettings)}
            />
            
            <div className="flex justify-center">
              <GameCanvas
                gameState={gameState}
                onKeyPress={handleKeyPress}
              />
            </div>

            {showSettings && (
              <div className="bg-white rounded-lg shadow-lg p-4">
                <h3 className="text-lg font-semibold text-gray-800 mb-3">Game Settings</h3>
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
                  <div>
                    <h4 className="font-medium text-gray-700">Weapon Styles:</h4>
                    <ul className="text-gray-600 mt-1">
                      <li>• <strong>Foil:</strong> Balanced, quick attacks</li>
                      <li>• <strong>Épée:</strong> Powerful, longer reach</li>
                      <li>• <strong>Sabre:</strong> Fast, aggressive style</li>
                      <li>• <strong>Master:</strong> Elite, all-around excellence</li>
                    </ul>
                  </div>
                  <div>
                    <h4 className="font-medium text-gray-700">Combat Mechanics:</h4>
                    <ul className="text-gray-600 mt-1">
                      <li>• Timing is crucial for successful attacks</li>
                      <li>• Parrying requires precise timing</li>
                      <li>• Being stunned leaves you vulnerable</li>
                      <li>• Distance and weapon reach matter</li>
                    </ul>
                  </div>
                </div>
              </div>
            )}
          </div>

          {/* Stats Sidebar */}
          <div className="xl:col-span-1">
            <GameStats gameState={gameState} />
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;