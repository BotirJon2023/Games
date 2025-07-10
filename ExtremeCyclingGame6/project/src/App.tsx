import React, { useState } from 'react';
import GameMenu from './components/GameMenu';
import GamePlay from './components/GamePlay';
import GameOver from './components/GameOver';
import { GameState } from './types/game';

function App() {
  const [gameState, setGameState] = useState<GameState>('menu');
  const [score, setScore] = useState(0);
  const [highScore, setHighScore] = useState(() => {
    const saved = localStorage.getItem('cyclingHighScore');
    return saved ? parseInt(saved) : 0;
  });

  const startGame = () => {
    setGameState('playing');
    setScore(0);
  };

  const endGame = (finalScore: number) => {
    setScore(finalScore);
    if (finalScore > highScore) {
      setHighScore(finalScore);
      localStorage.setItem('cyclingHighScore', finalScore.toString());
    }
    setGameState('gameOver');
  };

  const returnToMenu = () => {
    setGameState('menu');
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-blue-400 via-blue-500 to-blue-700 overflow-hidden">
      {gameState === 'menu' && (
        <GameMenu onStartGame={startGame} highScore={highScore} />
      )}
      {gameState === 'playing' && (
        <GamePlay onGameEnd={endGame} />
      )}
      {gameState === 'gameOver' && (
        <GameOver 
          score={score} 
          highScore={highScore} 
          onReturnToMenu={returnToMenu}
          onPlayAgain={startGame}
        />
      )}
    </div>
  );
}

export default App;