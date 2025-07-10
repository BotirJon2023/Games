import React from 'react';
import { Heart, Zap, Shield, TrendingUp, Trophy, Timer, Gauge } from 'lucide-react';
import { Cyclist, GameStats, ActivePowerUp } from '../types/game';

interface GameHUDProps {
  gameStats: GameStats;
  cyclist: Cyclist;
  activePowerUps: ActivePowerUp[];
  gameSpeed: number;
}

const GameHUD: React.FC<GameHUDProps> = ({ 
  gameStats, 
  cyclist, 
  activePowerUps, 
  gameSpeed 
}) => {
  const formatTime = (milliseconds: number): string => {
    const seconds = Math.floor(milliseconds / 1000);
    const minutes = Math.floor(seconds / 60);
    const remainingSeconds = seconds % 60;
    return `${minutes}:${remainingSeconds.toString().padStart(2, '0')}`;
  };

  const getHealthColor = (health: number): string => {
    if (health > 70) return 'bg-green-500';
    if (health > 30) return 'bg-yellow-500';
    return 'bg-red-500';
  };

  const getHealthTextColor = (health: number): string => {
    if (health > 70) return 'text-green-400';
    if (health > 30) return 'text-yellow-400';
    return 'text-red-400';
  };

  return (
    <>
      {/* Top HUD */}
      <div className="absolute top-4 left-4 right-4 z-50">
        <div className="flex justify-between items-start">
          {/* Left side - Stats */}
          <div className="space-y-2">
            {/* Score */}
            <div className="bg-black/30 backdrop-blur-sm rounded-lg px-4 py-2 border border-white/20">
              <div className="flex items-center space-x-2">
                <Trophy className="w-5 h-5 text-yellow-400" />
                <span className="text-white font-bold text-xl">
                  {gameStats.score.toLocaleString()}
                </span>
              </div>
            </div>

            {/* Level and Distance */}
            <div className="flex space-x-2">
              <div className="bg-black/30 backdrop-blur-sm rounded-lg px-3 py-1 border border-white/20">
                <div className="text-xs text-white/70">Level</div>
                <div className="text-white font-bold">{gameStats.level}</div>
              </div>
              <div className="bg-black/30 backdrop-blur-sm rounded-lg px-3 py-1 border border-white/20">
                <div className="text-xs text-white/70">Distance</div>
                <div className="text-white font-bold">{Math.floor(gameStats.distance)}m</div>
              </div>
            </div>
          </div>

          {/* Right side - Time and Speed */}
          <div className="space-y-2">
            {/* Time */}
            <div className="bg-black/30 backdrop-blur-sm rounded-lg px-4 py-2 border border-white/20">
              <div className="flex items-center space-x-2">
                <Timer className="w-5 h-5 text-blue-400" />
                <span className="text-white font-bold text-lg">
                  {formatTime(gameStats.timeElapsed)}
                </span>
              </div>
            </div>

            {/* Speed */}
            <div className="bg-black/30 backdrop-blur-sm rounded-lg px-3 py-1 border border-white/20">
              <div className="flex items-center space-x-2">
                <Gauge className="w-4 h-4 text-green-400" />
                <span className="text-white font-bold">{gameSpeed.toFixed(1)} km/h</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Bottom HUD */}
      <div className="absolute bottom-4 left-4 right-4 z-50">
        <div className="flex justify-between items-end">
          {/* Left side - Health */}
          <div className="space-y-2">
            {/* Health Bar */}
            <div className="bg-black/30 backdrop-blur-sm rounded-lg p-3 border border-white/20">
              <div className="flex items-center space-x-2 mb-2">
                <Heart className={`w-5 h-5 ${getHealthTextColor(cyclist.health)}`} />
                <span className="text-white font-semibold">Health</span>
              </div>
              <div className="w-32 h-3 bg-gray-700 rounded-full overflow-hidden">
                <div 
                  className={`h-full transition-all duration-300 ${getHealthColor(cyclist.health)}`}
                  style={{ width: `${cyclist.health}%` }}
                />
              </div>
              <div className="text-xs text-white/70 mt-1">
                {cyclist.health}/100
              </div>
            </div>

            {/* Combo */}
            {gameStats.combo > 0 && (
              <div className="bg-gradient-to-r from-yellow-500/20 to-orange-500/20 backdrop-blur-sm rounded-lg px-3 py-2 border border-yellow-400/30">
                <div className="flex items-center space-x-2">
                  <TrendingUp className="w-4 h-4 text-yellow-400" />
                  <span className="text-yellow-400 font-bold">
                    {gameStats.combo}x Combo!
                  </span>
                </div>
              </div>
            )}
          </div>

          {/* Right side - Active Power-ups */}
          {activePowerUps.length > 0 && (
            <div className="space-y-2">
              <div className="text-xs text-white/70 text-right">Active Power-ups</div>
              {activePowerUps.map((powerUp, index) => (
                <div 
                  key={index}
                  className="bg-black/30 backdrop-blur-sm rounded-lg p-2 border border-white/20 min-w-[120px]"
                >
                  <div className="flex items-center space-x-2">
                    {powerUp.type === 'speed' && <Zap className="w-4 h-4 text-yellow-400" />}
                    {powerUp.type === 'shield' && <Shield className="w-4 h-4 text-blue-400" />}
                    {powerUp.type === 'jump' && <TrendingUp className="w-4 h-4 text-green-400" />}
                    <span className="text-white text-sm font-semibold capitalize">
                      {powerUp.type}
                    </span>
                  </div>
                  <div className="w-full h-1 bg-gray-700 rounded-full mt-1 overflow-hidden">
                    <div 
                      className="h-full bg-gradient-to-r from-green-400 to-yellow-400 transition-all duration-100"
                      style={{ 
                        width: `${(powerUp.timeRemaining / powerUp.maxTime) * 100}%` 
                      }}
                    />
                  </div>
                  <div className="text-xs text-white/70 mt-1">
                    {(powerUp.timeRemaining / 1000).toFixed(1)}s
                  </div>
                </div>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* Invulnerability overlay */}
      {cyclist.invulnerable && (
        <div className="absolute inset-0 bg-red-500/20 animate-pulse pointer-events-none z-40" />
      )}
    </>
  );
};

export default GameHUD;