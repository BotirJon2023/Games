import { useBowlingGame } from './hooks/useBowlingGame';
import { BowlingCanvas } from './components/BowlingCanvas';
import { ScoreBoard } from './components/ScoreBoard';

function App() {
  const {
    gameState,
    startAiming,
    stopAiming,
    throwBall,
    updateAimAngle,
    getTotalScore,
    resetGame,
  } = useBowlingGame();

  const handleMouseDown = () => {
    if (!gameState.isThrowInProgress && !gameState.gameOver) {
      startAiming();
    }
  };

  const handleMouseUp = () => {
    if (gameState.isAiming) {
      stopAiming();
      throwBall();
    }
  };

  const handleMouseMove = (clientX: number, canvasWidth: number) => {
    if (gameState.isAiming) {
      updateAimAngle(clientX, canvasWidth);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 to-gray-800 p-8">
      <div className="max-w-5xl mx-auto">
        <div className="mb-6">
          <h1 className="text-4xl font-bold text-white mb-2 drop-shadow-lg">
            Bowling Simulator
          </h1>
          <p className="text-gray-300">Experience realistic bowling physics</p>
        </div>

        <div className="bg-gray-800 p-6 rounded-xl shadow-2xl mb-6">
          <BowlingCanvas
            gameState={gameState}
            onMouseDown={handleMouseDown}
            onMouseUp={handleMouseUp}
            onMouseMove={handleMouseMove}
            isAiming={gameState.isAiming}
            power={gameState.power}
            angle={gameState.angle}
          />
        </div>

        <div className="mb-6">
          <ScoreBoard
            scores={gameState.scores}
            currentFrameThrows={gameState.currentFrameThrows}
            frameCount={gameState.frameCount}
            totalScore={getTotalScore()}
          />
        </div>

        <div className="flex gap-4 justify-center">
          <button
            onClick={resetGame}
            className="px-8 py-3 bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white font-bold rounded-lg transition-all duration-200 transform hover:scale-105 active:scale-95 shadow-lg"
          >
            New Game
          </button>
          <div className="bg-gray-700 rounded-lg px-6 py-3 text-white font-semibold">
            Frame: {gameState.frameCount + 1}/10
          </div>
        </div>

        <div className="mt-6 bg-gray-700 rounded-lg p-4 text-gray-300 text-sm">
          <p className="mb-2">
            <span className="font-bold text-white">How to Play:</span>
          </p>
          <ul className="space-y-1 ml-4">
            <li>✓ Click and hold your mouse to start aiming</li>
            <li>✓ Move your mouse left and right to adjust the angle</li>
            <li>✓ The power meter will oscillate automatically</li>
            <li>✓ Release when the power is at your desired level</li>
            <li>✓ Watch the physics simulation as pins fall and collide</li>
          </ul>
        </div>
      </div>
    </div>
  );
}

export default App;
