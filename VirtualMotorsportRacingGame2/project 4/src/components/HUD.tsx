import React from 'react';

interface HUDProps {
  position: number;
  lap: number;
  speed: number;
  isPaused: boolean;
}

const HUD: React.FC<HUDProps> = ({ position, lap, speed, isPaused }) => {
  const getPositionSuffix = (pos: number) => {
    if (pos === 1) return 'st';
    if (pos === 2) return 'nd';
    if (pos === 3) return 'rd';
    return 'th';
  };

  return (
    <>
      <div className="fixed top-8 left-8 bg-black bg-opacity-70 p-6 rounded-lg border-2 border-yellow-500 text-white font-bold">
        <div className="text-lg mb-2">Position: {position}{getPositionSuffix(position)}</div>
        <div className="text-lg mb-4">Lap: {lap} / 3</div>
        <div className="text-lg mb-2">Speed: {speed}%</div>
        <div className="w-40 bg-gray-700 rounded h-4 overflow-hidden">
          <div
            className="bg-yellow-500 h-full transition-all duration-100"
            style={{ width: `${speed}%` }}
          />
        </div>
      </div>

      {isPaused && (
        <div className="fixed inset-0 flex items-center justify-center bg-black bg-opacity-50 pointer-events-none">
          <div className="text-center">
            <h2 className="text-5xl font-bold text-white mb-4">PAUSED</h2>
            <p className="text-2xl text-gray-300">Press P to Resume</p>
          </div>
        </div>
      )}
    </>
  );
};

export default HUD;
