import React, { useEffect, useState } from 'react';

interface AnimationEffectsProps {
  lastResult?: string;
  isVisible: boolean;
}

export const AnimationEffects: React.FC<AnimationEffectsProps> = ({
  lastResult,
  isVisible,
}) => {
  const [showEffect, setShowEffect] = useState(false);

  useEffect(() => {
    if (isVisible && lastResult) {
      setShowEffect(true);
      const timer = setTimeout(() => setShowEffect(false), 2000);
      return () => clearTimeout(timer);
    }
  }, [isVisible, lastResult]);

  if (!showEffect || !lastResult) return null;

  const getEffectStyle = (result: string) => {
    switch (result) {
      case 'homerun':
        return {
          text: '⚾ HOME RUN! ⚾',
          className: 'text-yellow-400 text-6xl font-extrabold animate-bounce',
          background: 'bg-gradient-to-r from-yellow-400 to-orange-500',
        };
      case 'hit':
        return {
          text: '✨ GREAT HIT! ✨',
          className: 'text-green-400 text-4xl font-bold animate-pulse',
          background: 'bg-gradient-to-r from-green-400 to-blue-500',
        };
      case 'strike':
        return {
          text: '❌ STRIKE! ❌',
          className: 'text-red-400 text-3xl font-bold animate-shake',
          background: 'bg-gradient-to-r from-red-400 to-red-600',
        };
      case 'ball':
        return {
          text: '🟢 BALL 🟢',
          className: 'text-blue-400 text-3xl font-bold',
          background: 'bg-gradient-to-r from-blue-400 to-blue-600',
        };
      case 'foul':
        return {
          text: '⚠️ FOUL BALL ⚠️',
          className: 'text-orange-400 text-3xl font-bold',
          background: 'bg-gradient-to-r from-orange-400 to-yellow-500',
        };
      default:
        return {
          text: '',
          className: '',
          background: '',
        };
    }
  };

  const effect = getEffectStyle(lastResult);

  return (
    <div className="fixed inset-0 flex items-center justify-center pointer-events-none z-50">
      <div
        className={`${effect.background} p-8 rounded-2xl shadow-2xl border-4 border-white transform transition-all duration-500 ${
          showEffect ? 'scale-100 opacity-100' : 'scale-0 opacity-0'
        }`}
      >
        <div className={effect.className}>{effect.text}</div>
      </div>
      
      {/* Particle effects for home runs */}
      {lastResult === 'homerun' && showEffect && (
        <div className="fixed inset-0 pointer-events-none">
          {Array.from({ length: 20 }).map((_, i) => (
            <div
              key={i}
              className="absolute w-2 h-2 bg-yellow-400 rounded-full animate-ping"
              style={{
                left: `${Math.random() * 100}%`,
                top: `${Math.random() * 100}%`,
                animationDelay: `${Math.random() * 2}s`,
                animationDuration: `${1 + Math.random()}s`,
              }}
            ></div>
          ))}
        </div>
      )}
    </div>
  );
};