import React from 'react';
import { Exercise } from '../types/game';
import { 
  Activity, 
  ArrowUp, 
  ArrowDown, 
  Minus, 
  RotateCcw, 
  Circle, 
  Zap, 
  Mountain 
} from 'lucide-react';

interface ExerciseDisplayProps {
  exercise: Exercise;
  isActive: boolean;
  isCompleted: boolean;
  onComplete?: () => void;
}

const ExerciseDisplay: React.FC<ExerciseDisplayProps> = ({ 
  exercise, 
  isActive, 
  isCompleted, 
  onComplete 
}) => {
  const getIcon = (iconName: string) => {
    const iconMap: Record<string, React.ReactNode> = {
      Activity: <Activity className="w-8 h-8" />,
      ArrowUp: <ArrowUp className="w-8 h-8" />,
      ArrowDown: <ArrowDown className="w-8 h-8" />,
      Minus: <Minus className="w-8 h-8" />,
      RotateCcw: <RotateCcw className="w-8 h-8" />,
      Circle: <Circle className="w-8 h-8" />,
      Zap: <Zap className="w-8 h-8" />,
      Mountain: <Mountain className="w-8 h-8" />
    };
    return iconMap[iconName] || <Activity className="w-8 h-8" />;
  };

  return (
    <div 
      className={`
        relative p-6 rounded-xl border-2 transition-all duration-500 transform
        ${isActive ? 'border-blue-500 bg-blue-50 scale-105 shadow-lg' : 'border-gray-200 bg-white'}
        ${isCompleted ? 'border-green-500 bg-green-50' : ''}
        ${isActive ? 'animate-pulse' : ''}
      `}
    >
      <div className="flex items-center gap-4">
        <div className={`
          p-3 rounded-full transition-colors duration-300
          ${isActive ? 'bg-blue-500 text-white' : isCompleted ? 'bg-green-500 text-white' : 'bg-gray-100 text-gray-600'}
        `}>
          {getIcon(exercise.icon)}
        </div>
        
        <div className="flex-1">
          <h3 className={`text-xl font-bold transition-colors duration-300 ${
            isActive ? 'text-blue-900' : isCompleted ? 'text-green-900' : 'text-gray-900'
          }`}>
            {exercise.name}
          </h3>
          <p className="text-gray-600 text-sm mt-1">{exercise.description}</p>
          
          <div className="flex gap-4 mt-3">
            {exercise.reps && (
              <div className="flex items-center gap-1">
                <span className="text-xs font-medium text-gray-500">REPS:</span>
                <span className="font-bold text-lg">{exercise.reps}</span>
              </div>
            )}
            {exercise.duration && (
              <div className="flex items-center gap-1">
                <span className="text-xs font-medium text-gray-500">TIME:</span>
                <span className="font-bold text-lg">{exercise.duration}s</span>
              </div>
            )}
            {exercise.weight && (
              <div className="flex items-center gap-1">
                <span className="text-xs font-medium text-gray-500">WEIGHT:</span>
                <span className="font-bold text-lg">{exercise.weight} lb</span>
              </div>
            )}
          </div>
        </div>
        
        {isActive && onComplete && (
          <button
            onClick={onComplete}
            className="px-4 py-2 bg-blue-500 text-white rounded-lg hover:bg-blue-600 transition-colors duration-200 font-medium"
          >
            Complete
          </button>
        )}
      </div>
      
      {isCompleted && (
        <div className="absolute inset-0 bg-green-500 bg-opacity-10 rounded-xl pointer-events-none">
          <div className="absolute top-2 right-2">
            <div className="w-6 h-6 bg-green-500 rounded-full flex items-center justify-center">
              <ArrowUp className="w-3 h-3 text-white" />
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ExerciseDisplay;