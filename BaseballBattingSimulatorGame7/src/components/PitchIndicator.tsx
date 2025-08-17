import React from 'react';
import { Pitch } from '../types/game';

interface PitchIndicatorProps {
  currentPitch?: Pitch;
  pitchInProgress: boolean;
}

export const PitchIndicator: React.FC<PitchIndicatorProps> = ({
  currentPitch,
  pitchInProgress,
}) => {
  if (!currentPitch || !pitchInProgress) {
    return (
      <div className="bg-gray-100 p-4 rounded-lg border border-gray-300">
        <h4 className="font-semibold text-gray-700 mb-2">Pitch Information</h4>
        <p className="text-gray-500">No pitch in progress</p>
      </div>
    );
  }

  const getPitchDescription = (type: string) => {
    switch (type) {
      case 'fastball':
        return { name: 'Fastball', description: 'High speed, straight trajectory', color: 'text-red-600' };
      case 'curveball':
        return { name: 'Curveball', description: 'Slower speed, curved path', color: 'text-blue-600' };
      case 'slider':
        return { name: 'Slider', description: 'Medium speed, late break', color: 'text-purple-600' };
      case 'changeup':
        return { name: 'Changeup', description: 'Deceptively slow, straight', color: 'text-green-600' };
      default:
        return { name: 'Unknown', description: '', color: 'text-gray-600' };
    }
  };

  const pitchInfo = getPitchDescription(currentPitch.type);

  return (
    <div className="bg-white p-4 rounded-lg border-2 border-gray-300 shadow-lg">
      <h4 className="font-bold text-gray-800 mb-3">Incoming Pitch</h4>
      <div className="space-y-2">
        <div className="flex items-center justify-between">
          <span className="font-semibold">Type:</span>
          <span className={`font-bold ${pitchInfo.color}`}>{pitchInfo.name}</span>
        </div>
        <div className="flex items-center justify-between">
          <span className="font-semibold">Speed:</span>
          <span className="font-mono text-lg">{currentPitch.speed} mph</span>
        </div>
        <div className="mt-3 p-2 bg-gray-50 rounded">
          <p className="text-sm text-gray-600">{pitchInfo.description}</p>
        </div>
        
        {/* Speed Indicator Bar */}
        <div className="mt-3">
          <div className="flex justify-between text-xs text-gray-500 mb-1">
            <span>Slow</span>
            <span>Fast</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div
              className={`h-2 rounded-full transition-all duration-500 ${
                currentPitch.speed > 95
                  ? 'bg-red-500'
                  : currentPitch.speed > 85
                  ? 'bg-yellow-500'
                  : 'bg-green-500'
              }`}
              style={{ width: `${Math.min((currentPitch.speed / 110) * 100, 100)}%` }}
            ></div>
          </div>
        </div>
      </div>
    </div>
  );
};