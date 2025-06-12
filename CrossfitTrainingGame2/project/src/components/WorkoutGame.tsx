import React, { useState, useEffect } from 'react';
import { Competition, GameState } from '../types/game';
import { useGameTimer } from '../hooks/useGameTimer';
import GameTimer from './GameTimer';
import ExerciseDisplay from './ExerciseDisplay';
import ScoreBoard from './ScoreBoard';
import { ArrowLeft, RefreshCw } from 'lucide-react';

interface WorkoutGameProps {
  competition: Competition;
  onBack: () => void;
}

const WorkoutGame: React.FC<WorkoutGameProps> = ({ competition, onBack }) => {
  const [gameState, setGameState] = useState<GameState>({
    currentCompetition: competition,
    isActive: false,
    isPaused: false,
    timeRemaining: competition.duration,
    currentRound: 1,
    score: 0,
    completedExercises: []
  });

  const [currentExerciseIndex, setCurrentExerciseIndex] = useState(0);
  const [showCelebration, setShowCelebration] = useState(false);

  const timer = useGameTimer(competition.duration);

  useEffect(() => {
    setGameState(prev => ({
      ...prev,
      isActive: timer.isActive,
      isPaused: timer.isPaused,
      timeRemaining: timer.timeRemaining
    }));
  }, [timer.isActive, timer.isPaused, timer.timeRemaining]);

  useEffect(() => {
    if (timer.isFinished) {
      handleWorkoutComplete();
    }
  }, [timer.isFinished]);

  const handleExerciseComplete = () => {
    const exerciseId = competition.exercises[currentExerciseIndex].id;
    
    setGameState(prev => ({
      ...prev,
      completedExercises: [...prev.completedExercises, exerciseId],
      score: prev.score + 1
    }));

    // Move to next exercise or complete round
    if (currentExerciseIndex < competition.exercises.length - 1) {
      setCurrentExerciseIndex(prev => prev + 1);
    } else {
      // Complete round
      setGameState(prev => ({
        ...prev,
        currentRound: prev.currentRound + 1
      }));
      setCurrentExerciseIndex(0);
      
      if (competition.type === 'FOR_TIME') {
        handleWorkoutComplete();
      }
    }
  };

  const handleWorkoutComplete = () => {
    timer.stop();
    setShowCelebration(true);
    setTimeout(() => setShowCelebration(false), 3000);
  };

  const handleRestart = () => {
    timer.reset();
    setGameState({
      currentCompetition: competition,
      isActive: false,
      isPaused: false,
      timeRemaining: competition.duration,
      currentRound: 1,
      score: 0,
      completedExercises: []
    });
    setCurrentExerciseIndex(0);
    setShowCelebration(false);
  };

  const currentExercise = competition.exercises[currentExerciseIndex];

  return (
    <div className="min-h-screen bg-gray-50 p-4">
      {showCelebration && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black bg-opacity-50">
          <div className="bg-white rounded-xl p-8 text-center animate-bounce">
            <div className="text-6xl mb-4">🎉</div>
            <h2 className="text-2xl font-bold text-gray-900 mb-2">Workout Complete!</h2>
            <p className="text-gray-600">Great job crushing {competition.name}!</p>
          </div>
        </div>
      )}

      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-6">
          <button
            onClick={onBack}
            className="flex items-center gap-2 px-4 py-2 text-gray-600 hover:text-gray-900 transition-colors duration-200"
          >
            <ArrowLeft className="w-5 h-5" />
            Back to Competitions
          </button>
          
          <div className="text-center">
            <h1 className="text-3xl font-bold text-gray-900">{competition.name}</h1>
            <p className="text-gray-600">{competition.type} • {competition.difficulty}</p>
          </div>
          
          <button
            onClick={handleRestart}
            className="flex items-center gap-2 px-4 py-2 text-blue-600 hover:text-blue-800 transition-colors duration-200"
          >
            <RefreshCw className="w-5 h-5" />
            Restart
          </button>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 space-y-6">
            <GameTimer
              timeRemaining={timer.timeRemaining}
              isActive={timer.isActive}
              isPaused={timer.isPaused}
              formatTime={timer.formatTime}
              onStart={timer.start}
              onPause={timer.pause}
              onResume={timer.resume}
              onStop={timer.stop}
            />
            
            <div className="bg-white rounded-xl shadow-lg p-6">
              <h3 className="text-xl font-bold text-gray-900 mb-4">Current Exercise</h3>
              <ExerciseDisplay
                exercise={currentExercise}
                isActive={timer.isActive && !timer.isPaused}
                isCompleted={gameState.completedExercises.includes(currentExercise.id)}
                onComplete={timer.isActive ? handleExerciseComplete : undefined}
              />
            </div>
            
            <div className="bg-white rounded-xl shadow-lg p-6">
              <h3 className="text-xl font-bold text-gray-900 mb-4">All Exercises</h3>
              <div className="grid gap-3">
                {competition.exercises.map((exercise, index) => (
                  <ExerciseDisplay
                    key={exercise.id}
                    exercise={exercise}
                    isActive={index === currentExerciseIndex && timer.isActive && !timer.isPaused}
                    isCompleted={gameState.completedExercises.includes(exercise.id)}
                  />
                ))}
              </div>
            </div>
          </div>
          
          <div>
            <ScoreBoard
              score={gameState.score}
              currentRound={gameState.currentRound}
              completedExercises={gameState.completedExercises.length}
              totalExercises={competition.exercises.length}
              workoutType={competition.type}
            />
          </div>
        </div>
      </div>
    </div>
  );
};

export default WorkoutGame;