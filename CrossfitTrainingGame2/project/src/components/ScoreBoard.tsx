import React from 'react';
import { Trophy, Target, Timer, Award } from 'lucide-react';

interface ScoreBoardProps {
  score: number;
  currentRound: number;
  completedExercises: number;
  totalExercises: number;
  workoutType: string;
}

const ScoreBoard: React.FC<ScoreBoardProps> = ({
  score,
  currentRound,
  completedExercises,
  totalExercises,
  workoutType
}) => {
  const getScoreLabel = () => {
    switch (workoutType) {
      case 'AMRAP': return 'Rounds + Reps';
      case 'FOR_TIME': return 'Time Elapsed';
      case 'TABATA': return 'Total Points';
      case 'EMOM': return 'Rounds Completed';
      default: return 'Score';
    }
  };

  const progressPercentage = (completedExercises / totalExercises) * 100;

  return (
    <div className="bg-white rounded-xl shadow-lg p-6">
      <div className="flex items-center justify-between mb-6">
        <div className="flex items-center gap-2">
          <Trophy className="w-6 h-6 text-yellow-500" />
          <h3 className="text-lg font-semibold text-gray-800">Score Board</h3>
        </div>
        <div className="px-3 py-1 bg-blue-100 text-blue-800 rounded-full text-sm font-medium">
          {workoutType}
        </div>
      </div>
      
      <div className="grid grid-cols-2 gap-4 mb-6">
        <div className="text-center p-4 bg-blue-50 rounded-lg">
          <div className="flex items-center justify-center gap-1 mb-2">
            <Target className="w-5 h-5 text-blue-500" />
            <span className="text-sm font-medium text-blue-700">{getScoreLabel()}</span>
          </div>
          <div className="text-2xl font-bold text-blue-900">{score}</div>
        </div>
        
        <div className="text-center p-4 bg-green-50 rounded-lg">
          <div className="flex items-center justify-center gap-1 mb-2">
            <Award className="w-5 h-5 text-green-500" />
            <span className="text-sm font-medium text-green-700">Round</span>
          </div>
          <div className="text-2xl font-bold text-green-900">{currentRound}</div>
        </div>
      </div>
      
      <div className="mb-4">
        <div className="flex justify-between items-center mb-2">
          <span className="text-sm font-medium text-gray-600">Progress</span>
          <span className="text-sm text-gray-500">{completedExercises}/{totalExercises}</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-3">
          <div 
            className="bg-gradient-to-r from-blue-500 to-green-500 h-3 rounded-full transition-all duration-500 ease-out"
            style={{ width: `${progressPercentage}%` }}
          ></div>
        </div>
      </div>
      
      {progressPercentage === 100 && (
        <div className="text-center p-3 bg-green-100 rounded-lg">
          <div className="flex items-center justify-center gap-2 text-green-800">
            <Trophy className="w-5 h-5" />
            <span className="font-semibold">Workout Complete!</span>
          </div>
        </div>
      )}
    </div>
  );
};

export default ScoreBoard;