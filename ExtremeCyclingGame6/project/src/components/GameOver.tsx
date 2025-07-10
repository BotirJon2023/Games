import React from 'react';
import { RotateCcw, Home, Trophy, Award, Target, Clock, TrendingUp } from 'lucide-react';

interface GameOverProps {
  score: number;
  highScore: number;
  onReturnToMenu: () => void;
  onPlayAgain: () => void;
}

const GameOver: React.FC<GameOverProps> = ({ 
  score, 
  highScore, 
  onReturnToMenu, 
  onPlayAgain 
}) => {
  const isNewHighScore = score === highScore && score > 0;

  return (
    <div className="min-h-screen flex flex-col items-center justify-center relative overflow-hidden">
      {/* Animated background */}
      <div className="absolute inset-0 bg-gradient-to-br from-red-900/50 via-purple-900/50 to-blue-900/50 animate-gradient-shift" />
      
      {/* Falling particles */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-10 left-1/4 w-2 h-2 bg-white/20 rounded-full animate-float"></div>
        <div className="absolute top-20 right-1/3 w-1 h-1 bg-white/30 rounded-full animate-float-delayed"></div>
        <div className="absolute top-40 left-2/3 w-3 h-3 bg-white/10 rounded-full animate-float-slow"></div>
        <div className="absolute top-60 right-1/4 w-2 h-2 bg-white/20 rounded-full animate-float"></div>
      </div>

      {/* Main content */}
      <div className="text-center z-10 px-8 max-w-md">
        {/* Game Over title */}
        <div className="mb-8">
          <h1 className="text-5xl md:text-6xl font-bold text-white mb-2 drop-shadow-lg">
            GAME OVER
          </h1>
          {isNewHighScore && (
            <div className="flex items-center justify-center space-x-2 mb-4">
              <Award className="w-8 h-8 text-yellow-400 animate-bounce" />
              <h2 className="text-2xl font-bold text-yellow-400 animate-pulse">
                NEW HIGH SCORE!
              </h2>
              <Award className="w-8 h-8 text-yellow-400 animate-bounce" />
            </div>
          )}
        </div>

        {/* Score display */}
        <div className="mb-8 space-y-4">
          {/* Current score */}
          <div className="bg-white/10 backdrop-blur-sm rounded-xl p-6 border border-white/20">
            <div className="flex items-center justify-center space-x-2 mb-2">
              <Target className="w-6 h-6 text-blue-400" />
              <span className="text-white/80 font-semibold">Final Score</span>
            </div>
            <div className="text-4xl font-bold text-white">
              {score.toLocaleString()}
            </div>
          </div>

          {/* High score */}
          <div className="bg-yellow-500/10 backdrop-blur-sm rounded-xl p-4 border border-yellow-400/30">
            <div className="flex items-center justify-center space-x-2 mb-2">
              <Trophy className="w-5 h-5 text-yellow-400" />
              <span className="text-yellow-200 font-semibold">High Score</span>
            </div>
            <div className="text-2xl font-bold text-yellow-400">
              {highScore.toLocaleString()}
            </div>
          </div>
        </div>

        {/* Stats breakdown */}
        <div className="mb-8 bg-white/5 backdrop-blur-sm rounded-xl p-4 border border-white/10">
          <h3 className="text-white font-semibold mb-3 flex items-center justify-center space-x-2">
            <TrendingUp className="w-5 h-5" />
            <span>Performance</span>
          </h3>
          <div className="grid grid-cols-2 gap-3 text-sm">
            <div className="text-center">
              <div className="text-white/60">Rank</div>
              <div className="text-white font-bold">
                {score > highScore * 0.8 ? 'Expert' : score > highScore * 0.6 ? 'Advanced' : score > highScore * 0.4 ? 'Skilled' : 'Beginner'}
              </div>
            </div>
            <div className="text-center">
              <div className="text-white/60">Grade</div>
              <div className="text-white font-bold">
                {score > highScore * 0.9 ? 'A+' : score > highScore * 0.7 ? 'A' : score > highScore * 0.5 ? 'B' : score > highScore * 0.3 ? 'C' : 'D'}
              </div>
            </div>
          </div>
        </div>

        {/* Action buttons */}
        <div className="space-y-4">
          <button
            onClick={onPlayAgain}
            className="flex items-center justify-center space-x-3 bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 text-white px-8 py-4 rounded-xl font-bold text-lg transition-all duration-300 transform hover:scale-105 shadow-lg hover:shadow-xl border border-green-400/50 w-full"
          >
            <RotateCcw className="w-5 h-5" />
            <span>PLAY AGAIN</span>
          </button>

          <button
            onClick={onReturnToMenu}
            className="flex items-center justify-center space-x-3 bg-white/10 hover:bg-white/20 text-white px-8 py-4 rounded-xl font-semibold text-lg transition-all duration-300 transform hover:scale-105 backdrop-blur-sm border border-white/20 w-full"
          >
            <Home className="w-5 h-5" />
            <span>MAIN MENU</span>
          </button>
        </div>

        {/* Encouragement message */}
        <div className="mt-8 text-white/60 text-sm">
          {isNewHighScore 
            ? "Incredible! You've set a new record!" 
            : score > highScore * 0.8 
              ? "So close to a new high score! Try again!" 
              : "Keep practicing to improve your score!"}
        </div>
      </div>
    </div>
  );
};

export default GameOver;