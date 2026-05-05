import React, { useState } from 'react';
import { useGolfGame } from './hooks/useGolfGame';
import GameSetup from './components/GameSetup';
import GolfCourse from './components/GolfCourse';
import Scoreboard from './components/Scoreboard';
import GameMessage from './components/GameMessage';
import { RotateCcw } from 'lucide-react';

function App() {
  const {
    gameState,
    particles,
    startGame,
    swing,
    updateSwingPower,
    updateSwingAngle,
  } = useGolfGame();

  const [showSetup, setShowSetup] = useState(true);

  const handleStartGame = (playerNames: string[], computerOpponent: boolean) => {
    startGame(playerNames, computerOpponent);
    setShowSetup(false);
  };

  const handleRestart = () => {
    setShowSetup(true);
  };

  if (showSetup || !gameState) {
    return <GameSetup onStartGame={handleStartGame} />;
  }

  const currentCourse = gameState.courses[gameState.currentHole];
  const currentPlayer = gameState.players[gameState.currentPlayerIndex];

  return (
    <div className="min-h-screen bg-gradient-to-br from-sky-300 via-cyan-200 to-teal-300 p-4">
      <div className="max-w-6xl mx-auto space-y-4">
        <div className="flex items-center justify-between">
          <h1 className="text-3xl font-bold bg-gradient-to-r from-emerald-600 to-teal-600 bg-clip-text text-transparent">
            Virtual Golf Challenge
          </h1>
          <button
            onClick={handleRestart}
            className="flex items-center gap-2 px-4 py-2 bg-white/90 rounded-lg shadow hover:bg-white transition-all"
          >
            <RotateCcw className="w-4 h-4" />
            New Game
          </button>
        </div>

        <GameMessage
          message={gameState.message}
          currentPlayer={currentPlayer}
          strokes={gameState.strokes}
          courseName={currentCourse.name}
          par={currentCourse.par}
        />

        <div className="flex gap-4">
          <div className="flex-1">
            <GolfCourse
              course={currentCourse}
              balls={gameState.balls}
              players={gameState.players}
              currentPlayer={currentPlayer}
              swingPower={gameState.swingPower}
              swingAngle={gameState.swingAngle}
              particles={particles}
              onAngleChange={updateSwingAngle}
              onPowerChange={updateSwingPower}
              onSwing={swing}
              gamePhase={gameState.gamePhase}
            />
          </div>

          <div className="w-80">
            <Scoreboard
              players={gameState.players}
              currentHole={gameState.currentHole}
              totalHoles={gameState.courses.length}
              gamePhase={gameState.gamePhase}
            />
          </div>
        </div>

        <div className="bg-white/90 backdrop-blur-sm rounded-xl shadow-lg p-4">
          <h3 className="font-bold text-gray-700 mb-2">How to Play</h3>
          <div className="grid grid-cols-3 gap-4 text-sm text-gray-600">
            <div>
              <span className="font-semibold text-emerald-600">1. Aim:</span> Click on the course to set direction
            </div>
            <div>
              <span className="font-semibold text-blue-600">2. Power:</span> Adjust the power slider
            </div>
            <div>
              <span className="font-semibold text-orange-600">3. Swing:</span> Click the SWING button
            </div>
          </div>
          <div className="mt-3 text-xs text-gray-500">
            Avoid water hazards (ball resets), bunkers (slows ball), trees and rocks (bounces ball). Watch the wind!
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;
