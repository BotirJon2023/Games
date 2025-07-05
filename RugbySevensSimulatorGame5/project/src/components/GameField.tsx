import React from 'react';
import { GameState, FIELD_DIMENSIONS } from '../utils/gameEngine';

interface GameFieldProps {
  gameState: GameState;
}

export const GameField: React.FC<GameFieldProps> = ({ gameState }) => {
  const { homeTeam, awayTeam, ballPosition } = gameState;

  return (
    <div className="relative bg-green-600 border-4 border-white rounded-lg overflow-hidden shadow-2xl">
      <svg
        width={FIELD_DIMENSIONS.width}
        height={FIELD_DIMENSIONS.height}
        className="absolute inset-0"
      >
        {/* Field markings */}
        <defs>
          <pattern id="grass" x="0" y="0" width="20" height="20" patternUnits="userSpaceOnUse">
            <rect width="20" height="20" fill="#16A34A" />
            <rect width="10" height="10" fill="#15803D" />
          </pattern>
        </defs>
        
        <rect width="100%" height="100%" fill="url(#grass)" />
        
        {/* Try lines */}
        <line
          x1={FIELD_DIMENSIONS.tryLineHome}
          y1="0"
          x2={FIELD_DIMENSIONS.tryLineHome}
          y2={FIELD_DIMENSIONS.height}
          stroke="white"
          strokeWidth="3"
        />
        <line
          x1={FIELD_DIMENSIONS.tryLineAway}
          y1="0"
          x2={FIELD_DIMENSIONS.tryLineAway}
          y2={FIELD_DIMENSIONS.height}
          stroke="white"
          strokeWidth="3"
        />
        
        {/* Center line */}
        <line
          x1={FIELD_DIMENSIONS.centerLine}
          y1="0"
          x2={FIELD_DIMENSIONS.centerLine}
          y2={FIELD_DIMENSIONS.height}
          stroke="white"
          strokeWidth="2"
          strokeDasharray="10,5"
        />
        
        {/* Goal posts */}
        <g transform={`translate(${FIELD_DIMENSIONS.tryLineHome - 20}, ${FIELD_DIMENSIONS.height / 2})`}>
          <rect x="0" y="-30" width="4" height="60" fill="white" />
          <rect x="16" y="-30" width="4" height="60" fill="white" />
          <rect x="0" y="-32" width="20" height="4" fill="white" />
        </g>
        
        <g transform={`translate(${FIELD_DIMENSIONS.tryLineAway + 16}, ${FIELD_DIMENSIONS.height / 2})`}>
          <rect x="0" y="-30" width="4" height="60" fill="white" />
          <rect x="16" y="-30" width="4" height="60" fill="white" />
          <rect x="0" y="-32" width="20" height="4" fill="white" />
        </g>
      </svg>

      {/* Players */}
      {homeTeam.players.map((player) => (
        <div
          key={`home-${player.id}`}
          className="absolute w-8 h-8 rounded-full border-2 border-white flex items-center justify-center text-xs font-bold text-white transition-all duration-300 ease-in-out transform hover:scale-110"
          style={{
            backgroundColor: homeTeam.color,
            left: `${player.x - 16}px`,
            top: `${player.y - 16}px`,
            boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
            zIndex: player.hasBall ? 20 : 10
          }}
          title={`${player.name} - ${player.position}`}
        >
          {player.id + 1}
        </div>
      ))}

      {awayTeam.players.map((player) => (
        <div
          key={`away-${player.id}`}
          className="absolute w-8 h-8 rounded-full border-2 border-white flex items-center justify-center text-xs font-bold text-white transition-all duration-300 ease-in-out transform hover:scale-110"
          style={{
            backgroundColor: awayTeam.color,
            left: `${player.x - 16}px`,
            top: `${player.y - 16}px`,
            boxShadow: '0 2px 8px rgba(0,0,0,0.3)',
            zIndex: player.hasBall ? 20 : 10
          }}
          title={`${player.name} - ${player.position}`}
        >
          {player.id + 1}
        </div>
      ))}

      {/* Ball */}
      <div
        className="absolute w-4 h-4 bg-yellow-400 rounded-full border-2 border-yellow-600 transition-all duration-200 ease-in-out animate-pulse"
        style={{
          left: `${ballPosition.x - 8}px`,
          top: `${ballPosition.y - 8}px`,
          boxShadow: '0 2px 6px rgba(0,0,0,0.4)',
          zIndex: 25
        }}
      />
    </div>
  );
};