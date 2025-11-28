import React from 'react';

interface FinishScreenProps {
  position: number;
}

const FinishScreen: React.FC<FinishScreenProps> = ({ position }) => {
  const getPositionSuffix = (pos: number) => {
    if (pos === 1) return 'st';
    if (pos === 2) return 'nd';
    if (pos === 3) return 'rd';
    return 'th';
  };

  const getMedalColor = (pos: number) => {
    if (pos === 1) return 'text-yellow-400';
    if (pos === 2) return 'text-gray-300';
    if (pos === 3) return 'text-orange-400';
    return 'text-gray-400';
  };

  return (
    <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-70 pointer-events-none">
      <div className="text-center">
        <h2 className={`text-6xl font-bold mb-6 ${getMedalColor(position)}`}>RACE FINISHED!</h2>
        <p className="text-4xl font-bold text-white mb-8">
          Your Position: {position}{getPositionSuffix(position)}
        </p>
        <p className="text-2xl text-gray-300">Press R to Restart</p>
      </div>
    </div>
  );
};

export default FinishScreen;
