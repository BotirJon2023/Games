import React from 'react';

interface BaseballFieldProps {
  ballPosition: { x: number; y: number };
  isSwinging: boolean;
  pitchInProgress: boolean;
}

export const BaseballField: React.FC<BaseballFieldProps> = ({
  ballPosition,
  isSwinging,
  pitchInProgress,
}) => {
  return (
    <div className="relative w-full h-96 bg-gradient-to-b from-green-400 to-green-600 rounded-lg overflow-hidden shadow-2xl border-4 border-brown-600">
      {/* Pitcher's Mound */}
      <div className="absolute top-20 left-1/2 transform -translate-x-1/2 w-16 h-8 bg-amber-700 rounded-full opacity-80"></div>
      
      {/* Home Plate */}
      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 w-6 h-6 bg-white rotate-45 border-2 border-gray-300"></div>
      
      {/* Batter's Box */}
      <div className="absolute bottom-4 left-1/2 transform -translate-x-1/2 translate-x-8 w-12 h-16 border-2 border-white border-dashed opacity-60"></div>
      
      {/* Strike Zone */}
      <div className="absolute bottom-12 left-1/2 transform -translate-x-1/2 w-16 h-20 border-2 border-yellow-400 border-dashed opacity-50"></div>
      
      {/* Baseball */}
      {pitchInProgress && (
        <div
          className={`absolute w-4 h-4 bg-white rounded-full border-2 border-red-600 transition-all duration-1000 ease-in-out transform ${
            pitchInProgress ? 'animate-pulse' : ''
          }`}
          style={{
            left: `${ballPosition.x}%`,
            top: `${ballPosition.y}%`,
            boxShadow: '2px 2px 4px rgba(0,0,0,0.3)',
          }}
        >
          <div className="absolute inset-0 border-r border-red-600 transform rotate-45"></div>
          <div className="absolute inset-0 border-r border-red-600 transform -rotate-45"></div>
        </div>
      )}
      
      {/* Bat */}
      <div
        className={`absolute bottom-16 right-1/2 transform translate-x-1/2 w-1 h-24 bg-amber-800 origin-bottom transition-transform duration-200 ${
          isSwinging ? 'rotate-45 scale-110' : 'rotate-12'
        }`}
        style={{
          background: 'linear-gradient(to top, #8B4513 0%, #D2691E 100%)',
          boxShadow: '1px 1px 3px rgba(0,0,0,0.4)',
        }}
      ></div>
      
      {/* Batter */}
      <div className="absolute bottom-6 right-1/2 transform translate-x-6 w-8 h-16 bg-blue-600 rounded-t-full">
        <div className="w-4 h-4 bg-pink-300 rounded-full mx-auto mt-1"></div>
        <div className="w-6 h-3 bg-blue-700 mx-auto mt-1 rounded"></div>
      </div>
      
      {/* Background Elements */}
      <div className="absolute top-4 right-4 w-16 h-8 bg-amber-600 rounded opacity-40"></div>
      <div className="absolute top-4 left-4 w-12 h-12 bg-green-800 rounded-full opacity-30"></div>
      
      {/* Grass Texture */}
      <div className="absolute inset-0 opacity-20">
        {Array.from({ length: 20 }).map((_, i) => (
          <div
            key={i}
            className="absolute w-1 h-8 bg-green-800"
            style={{
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              transform: `rotate(${Math.random() * 360}deg)`,
            }}
          ></div>
        ))}
      </div>
    </div>
  );
};