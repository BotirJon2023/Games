import React from 'react';

interface BallProps {
  x: number;
  y: number;
  size: number;
}

const Ball: React.FC<BallProps> = ({ x, y, size }) => {
  return (
    <div
      className="absolute transition-none"
      style={{
        left: x - size / 2,
        top: y - size / 2,
        width: size,
        height: size,
        transform: 'translateZ(0)', // Hardware acceleration
      }}
    >
      {/* Main ball */}
      <div 
        className="absolute inset-0 rounded-full bg-gradient-to-br from-white via-gray-200 to-gray-400 shadow-lg animate-spin-slow"
        style={{
          boxShadow: '0 0 20px #ffffff, 0 0 40px #10b981, inset 0 2px 4px rgba(255,255,255,0.3)',
        }}
      />
      
      {/* Ball highlight */}
      <div 
        className="absolute top-1 left-1 w-2 h-2 bg-white rounded-full opacity-80 blur-[0.5px]"
        style={{
          width: size * 0.2,
          height: size * 0.2,
        }}
      />
      
      {/* Ball trail/glow */}
      <div 
        className="absolute -inset-2 rounded-full bg-green-400 opacity-30 blur-md animate-pulse-fast"
      />
      
      {/* Motion blur effect */}
      <div 
        className="absolute -inset-1 rounded-full bg-white opacity-10 blur-sm"
      />
    </div>
  );
};

export default Ball;