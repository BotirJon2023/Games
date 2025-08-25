import React from 'react';

interface PaddleProps {
  x: number;
  y: number;
  width: number;
  height: number;
  player: 'player1' | 'player2';
}

const Paddle: React.FC<PaddleProps> = ({ x, y, width, height, player }) => {
  const isPlayer1 = player === 'player1';
  
  return (
    <div
      className={`absolute transition-all duration-75 ease-out rounded-sm ${
        isPlayer1 
          ? 'bg-gradient-to-r from-blue-400 to-blue-600 shadow-lg shadow-blue-400/50' 
          : 'bg-gradient-to-r from-red-400 to-red-600 shadow-lg shadow-red-400/50'
      } animate-pulse-subtle`}
      style={{
        left: x,
        top: y,
        width,
        height,
        transform: 'translateZ(0)', // Hardware acceleration
        boxShadow: `0 0 20px ${isPlayer1 ? '#60a5fa' : '#f87171'}`,
      }}
    >
      {/* Inner glow effect */}
      <div 
        className={`absolute inset-0.5 rounded-sm ${
          isPlayer1 ? 'bg-blue-300' : 'bg-red-300'
        } opacity-30`} 
      />
      
      {/* Paddle segments for visual detail */}
      <div className="absolute inset-0 flex flex-col">
        {[...Array(5)].map((_, index) => (
          <div
            key={index}
            className={`flex-1 border-b border-white/20 ${
              index === 4 ? 'border-b-0' : ''
            }`}
          />
        ))}
      </div>
      
      {/* Movement trail effect */}
      <div 
        className={`absolute -inset-1 rounded-sm opacity-20 ${
          isPlayer1 ? 'bg-blue-400' : 'bg-red-400'
        } blur-sm`}
      />
    </div>
  );
};

export default Paddle;