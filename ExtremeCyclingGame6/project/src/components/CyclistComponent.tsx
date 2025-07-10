import React from 'react';
import { Cyclist } from '../types/game';

interface CyclistComponentProps {
  cyclist: Cyclist;
}

const CyclistComponent: React.FC<CyclistComponentProps> = ({ cyclist }) => {
  return (
    <div
      className={`absolute transition-all duration-75 ${
        cyclist.invulnerable ? 'animate-pulse opacity-60' : ''
      }`}
      style={{
        left: `${cyclist.position.x}px`,
        top: `${cyclist.position.y}px`,
        transform: `rotate(${cyclist.rotation}deg)`,
        transformOrigin: 'center center'
      }}
    >
      {/* Cyclist body */}
      <div className="relative">
        {/* Main body */}
        <div className="w-8 h-12 bg-gradient-to-b from-blue-500 to-blue-700 rounded-lg border-2 border-blue-800 relative">
          {/* Helmet */}
          <div className="absolute -top-3 left-1/2 transform -translate-x-1/2 w-6 h-6 bg-red-500 rounded-full border-2 border-red-700" />
          
          {/* Arms */}
          <div className="absolute top-2 -left-2 w-6 h-2 bg-blue-600 rounded-full rotate-12" />
          <div className="absolute top-2 -right-2 w-6 h-2 bg-blue-600 rounded-full -rotate-12" />
        </div>

        {/* Bike */}
        <div className="absolute -bottom-6 left-1/2 transform -translate-x-1/2">
          {/* Frame */}
          <div className="w-12 h-1 bg-gray-800 rounded-full" />
          
          {/* Wheels */}
          <div className="absolute -bottom-2 -left-1 w-4 h-4 bg-gray-900 rounded-full border-2 border-gray-700">
            <div className="absolute inset-1 bg-gray-600 rounded-full animate-spin">
              <div className="absolute inset-0 border-t-2 border-white rounded-full" />
            </div>
          </div>
          <div className="absolute -bottom-2 -right-1 w-4 h-4 bg-gray-900 rounded-full border-2 border-gray-700">
            <div className="absolute inset-1 bg-gray-600 rounded-full animate-spin">
              <div className="absolute inset-0 border-t-2 border-white rounded-full" />
            </div>
          </div>
          
          {/* Pedals */}
          <div className="absolute -bottom-1 left-1/2 transform -translate-x-1/2 w-2 h-2 bg-yellow-500 rounded-full animate-bounce" />
        </div>

        {/* Speed lines when moving fast */}
        {cyclist.velocity.x !== 0 && (
          <div className="absolute top-0 -left-8 opacity-60">
            <div className="w-6 h-0.5 bg-white rounded-full animate-pulse" />
            <div className="w-4 h-0.5 bg-white rounded-full animate-pulse mt-1" />
            <div className="w-5 h-0.5 bg-white rounded-full animate-pulse mt-1" />
          </div>
        )}

        {/* Jump trail */}
        {cyclist.isJumping && (
          <div className="absolute top-8 left-1/2 transform -translate-x-1/2">
            <div className="w-1 h-1 bg-blue-400 rounded-full animate-ping" />
            <div className="w-1 h-1 bg-blue-400 rounded-full animate-ping animation-delay-100 mt-1" />
            <div className="w-1 h-1 bg-blue-400 rounded-full animate-ping animation-delay-200 mt-1" />
          </div>
        )}
      </div>
    </div>
  );
};

export default CyclistComponent;