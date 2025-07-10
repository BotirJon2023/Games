import React from 'react';
import { Obstacle } from '../types/game';

interface ObstacleComponentProps {
  obstacle: Obstacle;
}

const ObstacleComponent: React.FC<ObstacleComponentProps> = ({ obstacle }) => {
  const renderObstacle = () => {
    switch (obstacle.type) {
      case 'rock':
        return (
          <div className="relative">
            <div 
              className="bg-gradient-to-br from-gray-400 to-gray-600 rounded-lg border-2 border-gray-700 shadow-lg"
              style={{ 
                width: `${obstacle.width}px`, 
                height: `${obstacle.height}px`,
                transform: 'rotate(5deg)'
              }}
            >
              <div className="absolute inset-2 bg-gray-500 rounded opacity-50" />
              <div className="absolute top-1 left-1 w-2 h-2 bg-gray-300 rounded-full" />
            </div>
          </div>
        );
      
      case 'tree':
        return (
          <div className="relative">
            {/* Trunk */}
            <div 
              className="bg-gradient-to-r from-yellow-800 to-yellow-900 rounded-lg border border-yellow-700"
              style={{ 
                width: `${obstacle.width * 0.3}px`, 
                height: `${obstacle.height * 0.4}px`,
                marginLeft: `${obstacle.width * 0.35}px`,
                marginTop: `${obstacle.height * 0.6}px`
              }}
            />
            {/* Leaves */}
            <div 
              className="absolute top-0 left-0 bg-gradient-to-b from-green-400 to-green-600 rounded-full border-2 border-green-700"
              style={{ 
                width: `${obstacle.width}px`, 
                height: `${obstacle.height * 0.8}px`
              }}
            >
              <div className="absolute inset-2 bg-green-500 rounded-full opacity-60" />
              <div className="absolute top-1 left-2 w-3 h-3 bg-green-300 rounded-full" />
              <div className="absolute top-3 right-2 w-2 h-2 bg-green-300 rounded-full" />
            </div>
          </div>
        );
      
      case 'ramp':
        return (
          <div 
            className="bg-gradient-to-r from-yellow-600 to-yellow-800 border-2 border-yellow-700 shadow-lg"
            style={{ 
              width: `${obstacle.width}px`, 
              height: `${obstacle.height}px`,
              clipPath: 'polygon(0% 100%, 100% 0%, 100% 100%)'
            }}
          >
            <div className="absolute inset-1 bg-yellow-700 opacity-50" />
          </div>
        );
      
      case 'pit':
        return (
          <div 
            className="bg-gradient-to-b from-gray-800 to-black border-2 border-gray-900 rounded-lg overflow-hidden"
            style={{ 
              width: `${obstacle.width}px`, 
              height: `${obstacle.height}px`
            }}
          >
            <div className="absolute inset-0 bg-gradient-to-r from-transparent via-gray-700 to-transparent opacity-30" />
            <div className="absolute bottom-0 left-0 right-0 h-1 bg-red-800" />
          </div>
        );
      
      case 'spike':
        return (
          <div className="relative">
            <div 
              className="bg-gradient-to-t from-red-600 to-red-800 border-2 border-red-900 shadow-lg"
              style={{ 
                width: `${obstacle.width}px`, 
                height: `${obstacle.height}px`,
                clipPath: 'polygon(50% 0%, 0% 100%, 100% 100%)'
              }}
            >
              <div className="absolute inset-1 bg-red-700 opacity-50" />
            </div>
            {/* Danger glow */}
            <div 
              className="absolute inset-0 bg-red-500 rounded-full opacity-20 animate-pulse"
              style={{ 
                width: `${obstacle.width + 10}px`, 
                height: `${obstacle.height + 10}px`,
                left: '-5px',
                top: '-5px'
              }}
            />
          </div>
        );
      
      default:
        return (
          <div 
            className="bg-gray-500 rounded border border-gray-600"
            style={{ 
              width: `${obstacle.width}px`, 
              height: `${obstacle.height}px`
            }}
          />
        );
    }
  };

  return (
    <div
      className="absolute transition-all duration-100"
      style={{
        left: `${obstacle.position.x}px`,
        top: `${obstacle.position.y}px`,
        zIndex: 20
      }}
    >
      {renderObstacle()}
    </div>
  );
};

export default ObstacleComponent;