import React, { useState } from 'react';
import { Competition } from './types/game';
import { competitions } from './data/competitions';
import CompetitionCard from './components/CompetitionCard';
import WorkoutGame from './components/WorkoutGame';
import { Dumbbell, Trophy, Zap } from 'lucide-react';

function App() {
  const [selectedCompetition, setSelectedCompetition] = useState<Competition | null>(null);
  const [currentView, setCurrentView] = useState<'menu' | 'game'>('menu');

  const handleCompetitionSelect = (competition: Competition) => {
    setSelectedCompetition(competition);
  };

  const handleStartWorkout = () => {
    if (selectedCompetition) {
      setCurrentView('game');
    }
  };

  const handleBackToMenu = () => {
    setCurrentView('menu');
    setSelectedCompetition(null);
  };

  if (currentView === 'game' && selectedCompetition) {
    return <WorkoutGame competition={selectedCompetition} onBack={handleBackToMenu} />;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-purple-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 py-6">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="p-2 bg-blue-500 rounded-lg">
                <Dumbbell className="w-8 h-8 text-white" />
              </div>
              <div>
                <h1 className="text-3xl font-bold text-gray-900">CrossFit Training Game</h1>
                <p className="text-gray-600">Push your limits, track your progress</p>
              </div>
            </div>
            
            <div className="flex items-center gap-6">
              <div className="text-center">
                <div className="flex items-center gap-1 text-blue-600">
                  <Trophy className="w-5 h-5" />
                  <span className="font-semibold">5</span>
                </div>
                <span className="text-xs text-gray-500">Competitions</span>
              </div>
              <div className="text-center">
                <div className="flex items-center gap-1 text-green-600">
                  <Zap className="w-5 h-5" />
                  <span className="font-semibold">Active</span>
                </div>
                <span className="text-xs text-gray-500">Ready to WOD</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 py-8">
        {/* Welcome Section */}
        <div className="text-center mb-12">
          <h2 className="text-4xl font-bold text-gray-900 mb-4">
            Choose Your <span className="text-blue-600">Challenge</span>
          </h2>
          <p className="text-xl text-gray-600 max-w-2xl mx-auto">
            Select from legendary CrossFit workouts and test your limits. Each competition offers unique challenges and scoring systems.
          </p>
        </div>

        {/* Competition Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6 mb-8">
          {competitions.map((competition) => (
            <CompetitionCard
              key={competition.id}
              competition={competition}
              onSelect={handleCompetitionSelect}
              isSelected={selectedCompetition?.id === competition.id}
            />
          ))}
        </div>

        {/* Start Workout Button */}
        {selectedCompetition && (
          <div className="text-center">
            <div className="bg-white rounded-xl shadow-lg p-6 max-w-md mx-auto">
              <h3 className="text-lg font-semibold text-gray-900 mb-2">
                Ready to start <span className="text-blue-600">{selectedCompetition.name}</span>?
              </h3>
              <p className="text-gray-600 mb-4 text-sm">
                This is a {selectedCompetition.difficulty.toLowerCase()} level {selectedCompetition.type} workout.
              </p>
              <button
                onClick={handleStartWorkout}
                className="w-full px-6 py-3 bg-gradient-to-r from-blue-500 to-purple-600 text-white rounded-lg hover:from-blue-600 hover:to-purple-700 transition-all duration-200 font-semibold shadow-lg hover:shadow-xl transform hover:scale-105"
              >
                Start Workout
              </button>
            </div>
          </div>
        )}

        {/* Stats Section */}
        <div className="mt-16 grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="bg-white rounded-xl shadow-lg p-6 text-center">
            <div className="w-12 h-12 bg-blue-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Trophy className="w-6 h-6 text-blue-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Track Progress</h3>
            <p className="text-gray-600 text-sm">Monitor your performance and see improvements over time</p>
          </div>
          
          <div className="bg-white rounded-xl shadow-lg p-6 text-center">
            <div className="w-12 h-12 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Zap className="w-6 h-6 text-green-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Multiple Formats</h3>
            <p className="text-gray-600 text-sm">AMRAP, For Time, Tabata, and more workout styles</p>
          </div>
          
          <div className="bg-white rounded-xl shadow-lg p-6 text-center">
            <div className="w-12 h-12 bg-purple-100 rounded-full flex items-center justify-center mx-auto mb-4">
              <Dumbbell className="w-6 h-6 text-purple-600" />
            </div>
            <h3 className="text-lg font-semibold text-gray-900 mb-2">Authentic WODs</h3>
            <p className="text-gray-600 text-sm">Experience real CrossFit benchmark workouts</p>
          </div>
        </div>
      </div>
    </div>
  );
}

export default App;