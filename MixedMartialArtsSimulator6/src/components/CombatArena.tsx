import React, { useState, useEffect } from 'react';
import { Fighter, CombatAction } from '../types/Fighter';
import { CombatEngine } from '../utils/CombatEngine';
import { FighterCard } from './FighterCard';
import { Play, Pause, RotateCcw, Volume2 } from 'lucide-react';

interface CombatArenaProps {
  fighter1: Fighter;
  fighter2: Fighter;
  onFightComplete: (result: any) => void;
}

export const CombatArena: React.FC<CombatArenaProps> = ({
  fighter1,
  fighter2,
  onFightComplete
}) => {
  const [isSimulating, setIsSimulating] = useState(false);
  const [currentRound, setCurrentRound] = useState(1);
  const [roundTime, setRoundTime] = useState(300);
  const [fightLog, setFightLog] = useState<string[]>([]);
  const [lastAction, setLastAction] = useState<CombatAction | null>(null);
  const [crowdNoise, setCrowdNoise] = useState(50);

  const startFight = () => {
    setIsSimulating(true);
    setFightLog([]);
    setCurrentRound(1);
    setRoundTime(300);

    // Reset fighters
    fighter1.health = fighter1.maxHealth;
    fighter1.stamina = fighter1.maxStamina;
    fighter2.health = fighter2.maxHealth;
    fighter2.stamina = fighter2.maxStamina;

    simulateFightRealTime();
  };

  const simulateFightRealTime = () => {
    const interval = setInterval(() => {
      if (fighter1.health <= 0 || fighter2.health <= 0 || currentRound > 3) {
        clearInterval(interval);
        setIsSimulating(false);
        
        const result = CombatEngine.simulateFight(fighter1, fighter2);
        onFightComplete(result);
        return;
      }

      // Simulate one exchange
      const attacker = Math.random() < 0.5 ? fighter1 : fighter2;
      const defender = attacker === fighter1 ? fighter2 : fighter1;
      
      const action = generateQuickAction(attacker, defender);
      executeActionWithAnimation(action, attacker, defender);
      
      setRoundTime(prev => {
        if (prev <= 0) {
          setCurrentRound(curr => curr + 1);
          return 300;
        }
        return prev - Math.random() * 10 - 5;
      });

    }, 1000); // One action per second for dramatic effect
  };

  const generateQuickAction = (attacker: Fighter, defender: Fighter): CombatAction => {
    const techniques = ['Jab', 'Cross', 'Hook', 'Low Kick', 'Takedown', 'Clinch'];
    const technique = techniques[Math.floor(Math.random() * techniques.length)];
    
    const success = Math.random() < 0.6;
    const damage = success ? Math.floor(Math.random() * 15 + 5) : 0;
    
    return {
      type: technique.includes('Takedown') ? 'grapple' : 'strike',
      target: 'head',
      technique,
      damage,
      staminaCost: 10,
      success,
      critical: success && Math.random() < 0.1
    };
  };

  const executeActionWithAnimation = (
    action: CombatAction,
    attacker: Fighter, 
    defender: Fighter
  ) => {
    setLastAction(action);
    
    if (action.success) {
      defender.health = Math.max(0, defender.health - action.damage);
      attacker.stamina = Math.max(0, attacker.stamina - action.staminaCost);
      
      const logEntry = `${attacker.name} lands ${action.technique} for ${action.damage} damage!`;
      setFightLog(prev => [logEntry, ...prev.slice(0, 9)]);
      
      // Increase crowd noise on big hits
      if (action.damage > 10 || action.critical) {
        setCrowdNoise(Math.min(100, crowdNoise + 20));
      }
    } else {
      const logEntry = `${attacker.name} misses ${action.technique}`;
      setFightLog(prev => [logEntry, ...prev.slice(0, 9)]);
    }

    // Decay crowd noise
    setTimeout(() => {
      setCrowdNoise(Math.max(30, crowdNoise - 5));
    }, 2000);
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-900 via-gray-800 to-black relative overflow-hidden">
      {/* Arena Background Effects */}
      <div className="absolute inset-0">
        <div className="absolute top-0 left-1/4 w-96 h-96 bg-red-500/20 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-0 right-1/4 w-96 h-96 bg-blue-500/20 rounded-full blur-3xl animate-pulse" />
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-yellow-500/10 rounded-full blur-2xl" />
      </div>

      {/* Arena UI */}
      <div className="relative z-10 p-6">
        {/* Fight Header */}
        <div className="text-center mb-8">
          <h1 className="text-4xl font-bold text-white mb-2">COMBAT ARENA</h1>
          <div className="flex items-center justify-center space-x-6 text-xl">
            <span className="text-red-400">Round {currentRound}</span>
            <span className="text-yellow-400">{formatTime(roundTime)}</span>
            <div className="flex items-center space-x-2">
              <Volume2 className="w-5 h-5 text-white" />
              <div className="w-20 bg-gray-700 rounded-full h-2">
                <div 
                  className="bg-green-500 h-2 rounded-full transition-all duration-500"
                  style={{ width: `${crowdNoise}%` }}
                />
              </div>
            </div>
          </div>
        </div>

        {/* Fighters Display */}
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-8">
          <div className={`transition-all duration-500 ${lastAction?.technique && Math.random() < 0.5 ? 'animate-pulse' : ''}`}>
            <FighterCard fighter={fighter1} showStats={false} />
          </div>
          <div className={`transition-all duration-500 ${lastAction?.technique && Math.random() < 0.5 ? 'animate-pulse' : ''}`}>
            <FighterCard fighter={fighter2} showStats={false} />
          </div>
        </div>

        {/* VS Section with Action Display */}
        <div className="text-center mb-8 relative">
          <div className="text-6xl font-bold text-white mb-4 animate-pulse">VS</div>
          {lastAction && (
            <div className="bg-black/50 rounded-lg p-4 max-w-md mx-auto">
              <div className={`text-2xl font-bold mb-2 ${lastAction.success ? 'text-green-400' : 'text-red-400'}`}>
                {lastAction.technique}
                {lastAction.critical && <span className="text-yellow-400 ml-2">CRITICAL!</span>}
              </div>
              {lastAction.success && (
                <div className="text-white">
                  Damage: {lastAction.damage}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Fight Controls */}
        <div className="flex justify-center space-x-4 mb-8">
          {!isSimulating ? (
            <button
              onClick={startFight}
              className="flex items-center space-x-2 bg-green-600 hover:bg-green-700 px-6 py-3 rounded-lg text-white font-bold transition-colors"
            >
              <Play className="w-5 h-5" />
              <span>START FIGHT</span>
            </button>
          ) : (
            <button
              onClick={() => setIsSimulating(false)}
              className="flex items-center space-x-2 bg-red-600 hover:bg-red-700 px-6 py-3 rounded-lg text-white font-bold transition-colors"
            >
              <Pause className="w-5 h-5" />
              <span>STOP FIGHT</span>
            </button>
          )}
          
          <button
            onClick={() => window.location.reload()}
            className="flex items-center space-x-2 bg-gray-600 hover:bg-gray-700 px-6 py-3 rounded-lg text-white font-bold transition-colors"
          >
            <RotateCcw className="w-5 h-5" />
            <span>RESET</span>
          </button>
        </div>

        {/* Fight Log */}
        <div className="max-w-2xl mx-auto bg-black/50 rounded-lg p-4">
          <h3 className="text-white font-bold mb-4">FIGHT LOG</h3>
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {fightLog.map((log, index) => (
              <div 
                key={index} 
                className={`text-sm p-2 rounded ${
                  log.includes('lands') ? 'bg-green-900/50 text-green-300' : 'bg-gray-800/50 text-gray-300'
                }`}
              >
                {log}
              </div>
            ))}
            {fightLog.length === 0 && (
              <div className="text-gray-500 text-center py-4">
                Fight log will appear here...
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};