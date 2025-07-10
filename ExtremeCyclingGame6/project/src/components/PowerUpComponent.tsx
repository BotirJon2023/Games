import React from 'react';
import { PowerUp } from '../types/game';
import { Zap, Shield, Heart, TrendingUp } from 'lucide-react';

interface PowerUpComponentProps {
  powerUp: PowerUp;
}

const PowerUpComponent: React.FC<PowerUpComponentProps> = ({ powerUp }) => {
  const getPowerUpIcon = () => {
    switch (powerUp.type) {
      case 'speed':
        return <Zap className="w-6 h-6 text-yellow-300" />;
      case 'shield':
        return <Shield className="w-6 h-6 text-blue-300" />;
      case 'health':
        return <Heart className="w-6 h-6 text-red-300" />;
      case 'jump':
        return <TrendingUp className="w-6 h-6 text-green-300" />;
      default:
        return <Zap className="w-6 h-6 text-white" />;
    }
  };

  const getPowerUpColor = () => {
    switch (powerUp.type) {
      case 'speed':
        return 'from-yellow-400 to-orange-500';
      case 'shield':
        return 'from-blue-400 to-blue-600';
      case 'health':
        return 'from-red-400 to-red-600';
      case 'jump':
        return 'from-green-400 to-green-600';
      default:
        return 'from-gray-400 to-gray-600';
    }
  };

  const getGlowColor = () => {
    switch (powerUp.type) {
      case 'speed':
        return 'shadow-yellow-400/50';
      case 'shield':
        return 'shadow-blue-400/50';
      case 'health':
        return 'shadow-red-400/50';
      case 'jump':
        return 'shadow-green-400/50';
      default:
        return 'shadow-gray-400/50';
    }
  };

  return (
    <div
      className="absolute transition-all duration-100 z-30"
      style={{
        left: `${powerUp.position.x}px`,
        top: `${powerUp.position.y}px`
      }}
    >
      {/* Power-up container */}
      <div className={`
        relative w-12 h-12 rounded-full 
        bg-gradient-to-br ${getPowerUpColor()}
        border-2 border-white/50
        shadow-lg ${getGlowColor()}
        animate-bounce
        transform hover:scale-110
        transition-all duration-300
      `}>
        {/* Inner glow */}
        <div className="absolute inset-1 bg-white/20 rounded-full animate-pulse" />
        
        {/* Icon */}
        <div className="absolute inset-0 flex items-center justify-center">
          {getPowerUpIcon()}
        </div>
        
        {/* Rotating ring */}
        <div className="absolute inset-0 rounded-full border-2 border-white/30 animate-spin" style={{ animationDuration: '2s' }}>
          <div className="absolute top-0 left-1/2 transform -translate-x-1/2 w-1 h-1 bg-white rounded-full" />
          <div className="absolute bottom-0 left-1/2 transform -translate-x-1/2 w-1 h-1 bg-white rounded-full" />
        </div>
        
        {/* Sparkle effects */}
        <div className="absolute -top-1 -left-1 w-2 h-2 bg-white rounded-full animate-ping" />
        <div className="absolute -bottom-1 -right-1 w-2 h-2 bg-white rounded-full animate-ping" style={{ animationDelay: '0.5s' }} />
        <div className="absolute -top-1 -right-1 w-1 h-1 bg-yellow-300 rounded-full animate-ping" style={{ animationDelay: '1s' }} />
        <div className="absolute -bottom-1 -left-1 w-1 h-1 bg-yellow-300 rounded-full animate-ping" style={{ animationDelay: '1.5s' }} />
      </div>
      
      {/* Power-up type label */}
      <div className="absolute -bottom-6 left-1/2 transform -translate-x-1/2 whitespace-nowrap">
        <div className="bg-black/50 text-white text-xs px-2 py-1 rounded-full font-semibold capitalize">
          {powerUp.type}
        </div>
      </div>
    </div>
  );
};

export default PowerUpComponent;