import React, { useState, useRef, useEffect } from 'react';
import { Play } from 'lucide-react';

interface HighJumpEventProps {
  onComplete: (eventName: string, performance: number, score: number) => void;
}

export default function HighJumpEvent({ onComplete }: HighJumpEventProps) {
  const [power, setPower] = useState(50);
  const [isJumping, setIsJumping] = useState(false);
  const [jumpData, setJumpData] = useState({
    x: 200,
    y: 250,
    height: 0,
    cleared: false,
  });
  const [isComplete, setIsComplete] = useState(false);
  const animationRef = useRef<number>();
  const startTimeRef = useRef<number>(0);
  const barHeightRef = useRef(1.5);

  useEffect(() => {
    if (!isJumping) return;

    const animate = () => {
      const elapsed = Date.now() - startTimeRef.current;
      const duration = 1200;
      const progress = Math.min(elapsed / duration, 1);

      const powerFactor = power / 100;
      const maxHeight = 1.0 + powerFactor * 0.8;
      const height = Math.sin(progress * Math.PI) * maxHeight * 80;

      const x = 200 + progress * 200;
      const y = 250 - height;

      const cleared = height > barHeightRef.current * 80 - 20;

      setJumpData({
        x,
        y,
        height: Math.max(0, height),
        cleared,
      });

      if (progress >= 1) {
        setIsJumping(false);
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
  }, [isJumping, power]);

  const handleJump = () => {
    setIsJumping(true);
    setIsComplete(false);
    setJumpData({ x: 200, y: 250, height: 0, cleared: false });
  };

  const handleFinish = () => {
    const finalHeight = barHeightRef.current;
    const score = Math.round(finalHeight * 350 + (jumpData.cleared ? 100 : 0));
    onComplete('High Jump', finalHeight, score);
  };

  const handleIncreaseBar = () => {
    if (!isComplete) {
      barHeightRef.current = Math.min(barHeightRef.current + 0.1, 2.5);
      setIsComplete(false);
    }
  };

  const barY = 250 - barHeightRef.current * 80;

  return (
    <div className="bg-white rounded-lg shadow-xl p-8 max-w-4xl mx-auto">
      <h2 className="text-3xl font-bold text-blue-900 mb-6">High Jump</h2>

      <div className="mb-8 bg-gradient-to-b from-blue-100 to-green-100 rounded-lg p-8 border-4 border-gray-400">
        <svg width="100%" height="350" viewBox="0 0 800 350" className="bg-white rounded">
          <rect x="0" y="280" width="800" height="70" fill="#34A048" />

          <line x1="150" y1="250" x2="150" y2="280" stroke="#333" strokeWidth="3" />
          <line x1="650" y1="250" x2="650" y2="280" stroke="#333" strokeWidth="3" />

          <line
            x1="150"
            y1={barY}
            x2="650"
            y2={barY}
            stroke={jumpData.cleared && isComplete ? '#FFD700' : '#FF4444'}
            strokeWidth="4"
            opacity={isComplete ? 1 : 0.7}
          />

          <circle cx="200" cy="250" r="7" fill="#FF6B6B" />
          <line x1="200" y1="257" x2="200" y2="280" stroke="#FF6B6B" strokeWidth="2" />

          {isJumping && jumpData.height > 0 ? (
            <>
              <line
                x1={jumpData.x}
                y1={jumpData.y}
                x2={jumpData.x - 10}
                y2={jumpData.y - 20}
                stroke="#FF6B6B"
                strokeWidth="2"
              />
              <line
                x1={jumpData.x}
                y1={jumpData.y}
                x2={jumpData.x + 10}
                y2={jumpData.y - 20}
                stroke="#FF6B6B"
                strokeWidth="2"
              />
              <line
                x1={jumpData.x}
                y1={jumpData.y + 17}
                x2={jumpData.x - 15}
                y2={jumpData.y + 35}
                stroke="#FF6B6B"
                strokeWidth="2"
              />
              <line
                x1={jumpData.x}
                y1={jumpData.y + 17}
                x2={jumpData.x + 15}
                y2={jumpData.y + 35}
                stroke="#FF6B6B"
                strokeWidth="2"
              />

              <circle cx={jumpData.x} cy={jumpData.y - 8} r="6" fill="#FF6B6B" />
            </>
          ) : (
            <>
              <line x1="200" y1="257" x2="200" y2="280" stroke="#FF6B6B" strokeWidth="2" />
              <line
                x1="200"
                y1="259"
                x2="185"
                y2="270"
                stroke="#FF6B6B"
                strokeWidth="2"
              />
              <line
                x1="200"
                y1="259"
                x2="215"
                y2="270"
                stroke="#FF6B6B"
                strokeWidth="2"
              />
            </>
          )}

          {isComplete && !jumpData.cleared && (
            <text x="400" y="100" textAnchor="middle" fontSize="32" fill="#FF0000" fontWeight="bold">
              Bar Knocked Down!
            </text>
          )}

          {isComplete && jumpData.cleared && (
            <text x="400" y="100" textAnchor="middle" fontSize="32" fill="#00AA00" fontWeight="bold">
              Success!
            </text>
          )}

          <text x="700" y="150" fontSize="14" fill="#666" textAnchor="end">
            {barHeightRef.current.toFixed(2)}m
          </text>
        </svg>

        <div className="mt-4 text-center text-lg font-semibold">
          Bar Height: {barHeightRef.current.toFixed(2)}m
          {isComplete && (
            <div
              className={`mt-2 text-xl font-bold ${
                jumpData.cleared ? 'text-green-600' : 'text-red-600'
              }`}
            >
              {jumpData.cleared ? 'Cleared!' : 'Missed!'}
            </div>
          )}
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
          disabled={isJumping}
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
          <div className="space-y-3">
            {jumpData.cleared && barHeightRef.current < 2.5 ? (
              <button
                onClick={handleIncreaseBar}
                className="w-full bg-purple-600 hover:bg-purple-700 text-white py-3 rounded-lg font-bold text-lg transition-all"
              >
                Raise Bar and Try Again
              </button>
            ) : (
              <button
                onClick={handleFinish}
                className="w-full bg-green-600 hover:bg-green-700 text-white py-3 rounded-lg font-bold text-lg transition-all"
              >
                {jumpData.cleared ? 'Finish Challenge' : 'Try Again'}
              </button>
            )}
          </div>
        )}
      </div>
    </div>
  );
}
