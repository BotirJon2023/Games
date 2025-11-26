import React, { useState, useRef, useEffect } from 'react';
import { Play } from 'lucide-react';

interface LongJumpEventProps {
  onComplete: (eventName: string, performance: number, score: number) => void;
}

export default function LongJumpEvent({ onComplete }: LongJumpEventProps) {
  const [power, setPower] = useState(50);
  const [isJumping, setIsJumping] = useState(false);
  const [jumpData, setJumpData] = useState({ distance: 0, height: 0 });
  const [isComplete, setIsComplete] = useState(false);
  const animationRef = useRef<number>();
  const startTimeRef = useRef<number>(0);

  useEffect(() => {
    if (!isJumping) return;

    const animate = () => {
      const elapsed = Date.now() - startTimeRef.current;
      const progress = Math.min(elapsed / 1500, 1);

      const powerFactor = power / 100;
      const distance = 5 + powerFactor * 3.5 + (Math.random() * 0.5);
      const height = Math.sin(progress * Math.PI) * (3 + powerFactor * 2);

      setJumpData({
        distance: distance * progress,
        height: Math.max(0, height),
      });

      if (progress >= 1) {
        setIsJumping(false);
        setIsComplete(true);
        setJumpData({
          distance: distance,
          height: 0,
        });
      } else {
        animationRef.current = requestAnimationFrame(animate);
      }
    };

    startTimeRef.current = Date.now();
    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) cancelAnimationFrame(animationRef.current);
    };
  }, [isJumping, power]);

  const handleJump = () => {
    setIsJumping(true);
    setIsComplete(false);
  };

  const handleFinish = () => {
    const finalDistance = jumpData.distance;
    const score = Math.round(finalDistance * 150);
    onComplete('Long Jump', finalDistance, score);
  };

  const runupPosition = isJumping ? 70 : 20;
  const jumpX = runupPosition + jumpData.distance * 10;
  const jumpY = 200 - jumpData.height * 20;

  return (
    <div className="bg-white rounded-lg shadow-xl p-8 max-w-4xl mx-auto">
      <h2 className="text-3xl font-bold text-blue-900 mb-6">Long Jump</h2>

      <div className="mb-8 bg-gradient-to-b from-blue-100 to-yellow-100 rounded-lg p-8 border-4 border-gray-400">
        <svg width="100%" height="300" viewBox="0 0 800 300" className="bg-white rounded">
          <rect x="0" y="220" width="150" height="80" fill="#8B4513" />
          <line x1="150" y1="220" x2="150" y2="300" stroke="#666" strokeWidth="2" />

          <rect x="150" y="220" width="650" height="80" fill="#DEB887" />

          {[150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750].map((x, i) => (
            <text
              key={x}
              x={x}
              y="295"
              textAnchor="middle"
              fontSize="10"
              fill="#666"
            >
              {(i - 1)}m
            </text>
          ))}

          <g transform={`translate(${jumpX}, ${jumpY})`}>
            <circle cx="0" cy="0" r="6" fill="#FF6B6B" />
            <line x1="0" y1="6" x2="0" y2="20" stroke="#FF6B6B" strokeWidth="2" />

            {isJumping && jumpData.height > 0 ? (
              <>
                <line
                  x1="0"
                  y1="8"
                  x2="-12"
                  y2="-5"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
                <line
                  x1="0"
                  y1="8"
                  x2="12"
                  y2="-5"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
                <line
                  x1="0"
                  y1="20"
                  x2="-15"
                  y2="35"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
                <line
                  x1="0"
                  y1="20"
                  x2="15"
                  y2="35"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
              </>
            ) : (
              <>
                <line
                  x1="0"
                  y1="8"
                  x2="-10"
                  y2="18"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
                <line
                  x1="0"
                  y1="8"
                  x2="10"
                  y2="0"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
                <line
                  x1="0"
                  y1="20"
                  x2="-12"
                  y2="30"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
                <line
                  x1="0"
                  y1="20"
                  x2="12"
                  y2="30"
                  stroke="#FF6B6B"
                  strokeWidth="2"
                />
              </>
            )}
          </g>

          {isComplete && (
            <line
              x1="150"
              y1="220"
              x2={jumpX}
              y2="220"
              stroke="#FF0000"
              strokeWidth="2"
              strokeDasharray="5,5"
            />
          )}
        </svg>

        <div className="mt-4 flex justify-between text-lg font-semibold">
          <div>Distance: {jumpData.distance.toFixed(2)}m</div>
          <div>Height: {jumpData.height.toFixed(2)}m</div>
        </div>
      </div>

      <div className="mb-6">
        <label className="block text-lg font-semibold mb-2">Power: {power}%</label>
        <input
          type="range"
          min="0"
          max="100"
          value={power}
          onChange={(e) => setPower(Number(e.target.value))}
          disabled={isJumping || isComplete}
          className="w-full h-3 bg-gray-200 rounded-lg appearance-none cursor-pointer"
        />
      </div>

      <div className="space-y-4">
        {!isComplete ? (
          <button
            onClick={handleJump}
            disabled={isJumping}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white py-3 rounded-lg font-bold flex items-center justify-center gap-2 text-lg transition-all"
          >
            <Play size={24} />
            {isJumping ? 'Jumping...' : 'Jump!'}
          </button>
        ) : (
          <div>
            <div className="bg-green-50 border-2 border-green-500 rounded-lg p-4 mb-4">
              <p className="text-center text-xl font-bold text-green-700">
                Distance: {jumpData.distance.toFixed(2)}m
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
