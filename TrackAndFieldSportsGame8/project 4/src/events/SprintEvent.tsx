import React, { useState, useRef, useEffect } from 'react';
import { Play } from 'lucide-react';

interface SprintEventProps {
  onComplete: (eventName: string, performance: number, score: number) => void;
}

export default function SprintEvent({ onComplete }: SprintEventProps) {
  const [isRunning, setIsRunning] = useState(false);
  const [distance, setDistance] = useState(0);
  const [time, setTime] = useState(0);
  const [isComplete, setIsComplete] = useState(false);
  const animationRef = useRef<number>();
  const startTimeRef = useRef<number>(0);

  useEffect(() => {
    if (!isRunning) return;

    const animate = () => {
      const elapsed = Date.now() - startTimeRef.current;
      const currentTime = elapsed / 1000;
      setTime(currentTime);

      const maxDistance = 100;
      const acceleration = 8 - (currentTime * 0.3);
      let currentDistance = Math.min(
        maxDistance,
        3 * currentTime * currentTime + (currentTime * (10 + Math.random() * 2))
      );

      setDistance(currentDistance);

      if (currentDistance >= maxDistance || currentTime >= 12) {
        setIsRunning(false);
        setIsComplete(true);
      } else {
        animationRef.current = requestAnimationFrame(animate);
      }
    };

    startTimeRef.current = Date.now();
    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) cancelAnimationFrame(animationRef.current);
    };
  }, [isRunning]);

  const handleStart = () => {
    setIsRunning(true);
    setDistance(0);
    setTime(0);
    setIsComplete(false);
  };

  const handleFinish = () => {
    const finalTime = Math.max(9.5, time);
    const score = Math.max(0, Math.round(1000 - (finalTime - 9.5) * 100));
    onComplete('100m Sprint', finalTime, score);
  };

  const runnerPosition = (distance / 100) * 80;
  const legSwing = Math.sin(time * 15) * 15;
  const armSwing = Math.cos(time * 15) * 20;

  return (
    <div className="bg-white rounded-lg shadow-xl p-8 max-w-4xl mx-auto">
      <h2 className="text-3xl font-bold text-blue-900 mb-6">100m Sprint</h2>

      <div className="mb-8 bg-gradient-to-b from-blue-100 to-green-100 rounded-lg p-8 border-4 border-gray-400">
        <svg width="100%" height="300" viewBox="0 0 800 300" className="bg-white rounded">
          <defs>
            <linearGradient id="trackGradient" x1="0%" y1="0%" x2="100%" y2="0%">
              <stop offset="0%" stopColor="#8B4513" />
              <stop offset="100%" stopColor="#A0522D" />
            </linearGradient>
          </defs>

          <rect x="0" y="120" width="800" height="80" fill="url(#trackGradient)" />

          {[0, 100, 200, 300, 400, 500, 600, 700].map((x) => (
            <line
              key={x}
              x1={x}
              y1="120"
              x2={x}
              y2="200"
              stroke="white"
              strokeWidth="2"
            />
          ))}

          {isRunning || isComplete ? (
            <g transform={`translate(${runnerPosition + 40}, 120)`}>
              <circle cx="0" cy="10" r="8" fill="#FF6B6B" />

              <line x1="0" y1="18" x2="0" y2="35" stroke="#FF6B6B" strokeWidth="2" />

              <line
                x1="0"
                y1="20"
                x2={Math.cos((Math.PI / 180) * (45 + armSwing)) * 15}
                y2={20 + Math.sin((Math.PI / 180) * (45 + armSwing)) * 15}
                stroke="#FF6B6B"
                strokeWidth="2"
              />
              <line
                x1="0"
                y1="20"
                x2={Math.cos((Math.PI / 180) * (225 - armSwing)) * 15}
                y2={20 + Math.sin((Math.PI / 180) * (225 - armSwing)) * 15}
                stroke="#FF6B6B"
                strokeWidth="2"
              />

              <line
                x1="0"
                y1="35"
                x2={Math.cos((Math.PI / 180) * (45 - legSwing)) * 20}
                y2={35 + Math.sin((Math.PI / 180) * (45 - legSwing)) * 20}
                stroke="#FF6B6B"
                strokeWidth="2"
              />
              <line
                x1="0"
                y1="35"
                x2={Math.cos((Math.PI / 180) * (225 + legSwing)) * 20}
                y2={35 + Math.sin((Math.PI / 180) * (225 + legSwing)) * 20}
                stroke="#FF6B6B"
                strokeWidth="2"
              />
            </g>
          ) : (
            <g transform="translate(50, 120)">
              <circle cx="0" cy="10" r="8" fill="#FF6B6B" />
              <line x1="0" y1="18" x2="0" y2="35" stroke="#FF6B6B" strokeWidth="2" />
              <line x1="0" y1="20" x2="15" y2="10" stroke="#FF6B6B" strokeWidth="2" />
              <line x1="0" y1="20" x2="-15" y2="30" stroke="#FF6B6B" strokeWidth="2" />
              <line x1="0" y1="35" x2="12" y2="50" stroke="#FF6B6B" strokeWidth="2" />
              <line x1="0" y1="35" x2="-12" y2="50" stroke="#FF6B6B" strokeWidth="2" />
            </g>
          )}

          <line x1="750" y1="100" x2="750" y2="200" stroke="#FFD700" strokeWidth="3" />
          <line x1="740" y1="100" x2="760" y2="100" stroke="#FFD700" strokeWidth="3" />
        </svg>

        <div className="mt-4 flex justify-between text-lg font-semibold">
          <div>Distance: {distance.toFixed(1)}m</div>
          <div>Time: {time.toFixed(2)}s</div>
        </div>
      </div>

      <div className="space-y-4">
        {!isComplete ? (
          <button
            onClick={handleStart}
            disabled={isRunning}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white py-3 rounded-lg font-bold flex items-center justify-center gap-2 text-lg transition-all"
          >
            <Play size={24} />
            {isRunning ? 'Running...' : 'Start Sprint'}
          </button>
        ) : (
          <div>
            <div className="bg-green-50 border-2 border-green-500 rounded-lg p-4 mb-4">
              <p className="text-center text-xl font-bold text-green-700">
                Finished! Time: {time.toFixed(2)}s
              </p>
            </div>
            <button
              onClick={handleFinish}
              className="w-full bg-green-600 hover:bg-green-700 text-white py-3 rounded-lg font-bold text-lg transition-all"
            >
              Submit Result
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
