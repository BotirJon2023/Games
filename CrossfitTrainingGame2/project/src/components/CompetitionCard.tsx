import React from 'react';
import { Competition } from '../types/game';
import { Clock, Trophy, Zap } from 'lucide-react';

interface CompetitionCardProps {
  competition: Competition;
  onSelect: (competition: Competition) => void;
  isSelected?: boolean;
}

const CompetitionCard: React.FC<CompetitionCardProps> = ({ 
  competition, 
  onSelect, 
  isSelected = false 
}) => {
  const getDifficultyColor = (difficulty: string) => {
    switch (difficulty) {
      case 'Beginner': return 'bg-green-100 text-green-800';
      case 'Intermediate': return 'bg-yellow-100 text-yellow-800';
      case 'Advanced': return 'bg-red-100 text-red-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  };

  const getTypeIcon = (type: string) => {
    switch (type) {
      case 'AMRAP': return <Zap className="w-4 h-4" />;
      case 'FOR_TIME': return <Clock className="w-4 h-4" />;
      case 'TABATA': return <Trophy className="w-4 h-4" />;
      default: return <Trophy className="w-4 h-4" />;
    }
  };

  return (
    <div 
      className={`
        relative bg-white rounded-xl shadow-lg border-2 transition-all duration-300 cursor-pointer
        hover:shadow-xl hover:scale-105 transform
        ${isSelected ? 'border-blue-500 ring-4 ring-blue-200' : 'border-gray-200 hover:border-blue-300'}
      `}
      onClick={() => onSelect(competition)}
    >
      <div className="p-6">
        <div className="flex justify-between items-start mb-3">
          <h3 className="text-xl font-bold text-gray-900">{competition.name}</h3>
          <div className="flex items-center gap-2">
            {getTypeIcon(competition.type)}
            <span className="text-sm font-medium text-gray-600">{competition.type}</span>
          </div>
        </div>
        
        <p className="text-gray-600 mb-4 text-sm leading-relaxed">
          {competition.description}
        </p>
        
        <div className="flex justify-between items-center">
          <div className="flex items-center gap-2">
            <Clock className="w-4 h-4 text-gray-500" />
            <span className="text-sm text-gray-600">
              {Math.floor(competition.duration / 60)} min
            </span>
          </div>
          
          <span className={`px-3 py-1 rounded-full text-xs font-semibold ${getDifficultyColor(competition.difficulty)}`}>
            {competition.difficulty}
          </span>
        </div>
        
        <div className="mt-4 pt-4 border-t border-gray-100">
          <div className="text-xs text-gray-500 mb-2">Exercises:</div>
          <div className="flex flex-wrap gap-1">
            {competition.exercises.slice(0, 3).map((exercise, index) => (
              <span 
                key={exercise.id} 
                className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs"
              >
                {exercise.name}
              </span>
            ))}
            {competition.exercises.length > 3 && (
              <span className="px-2 py-1 bg-gray-100 text-gray-700 rounded text-xs">
                +{competition.exercises.length - 3} more
              </span>
            )}
          </div>
        </div>
      </div>
      
      {isSelected && (
        <div className="absolute inset-0 bg-blue-500 bg-opacity-10 rounded-xl pointer-events-none">
          <div className="absolute top-2 right-2">
            <div className="w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center">
              <Trophy className="w-3 h-3 text-white" />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default CompetitionCard;