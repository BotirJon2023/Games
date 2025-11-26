import React, { useState, useRef, useEffect } from 'react';
import { Play } from 'lucide-react';

interface JavelinEventProps {
  onComplete: (eventName: string, performance: number, score: number) => void;
}

export default function JavelinEvent({ onComplete }: JavelinEventProps) {
  const [angle, setAngle] = useState(45);
  const [power, setPower] = useState(50);
  const [isThrowing, setIsThrowing] = useState(false);
  const [throwData, setThrowData] = useState({ x: 100, y: 200, distance: 0 });
  const [isComplete, setIsComplete] = useState(false);
  const animationRef = useRef<number>();
  const startTimeRef = useRef<number>(0);

  useEffect(() => {
    if (!isThrowing) return;

    const animate = () => {
      const elapsed = Date.now() - startTimeRef.current;
      const progress = Math.min(elapsed / 1200, 1);

      const angleRad = (angle * Math.PI) / 180;
      const powerFactor = power / 100;
      const speed = 15 + powerFactor * 10;

      const x = 100 + Math.cos(angleRad) * speed * progress * 20;
      let y = 200 - Math.sin(angleRad) * speed * progress * 20 + progress * progress * 50;
      y = Math.max(y, 200);

      setThrowData({
        x,
        y,
        distance: (x - 100) / 10,
      });

      if (progress >= 1) {
        setIsThrowing(false);
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
  }, [isThrowing, angle, power]);

  const handleThrow = () => {
    setIsThrowing(true);
    setIsComplete(false);
  };

  const handleFinish = () => {
    const finalDistance = throwData.distance;
    const score = Math.round(finalDistance * 15);
    onComplete('Javelin Throw', finalDistance, score);
  };

  const javelinAngle = Math.atan2(200 - throwData.y, throwData.x - 100);

  return (
    <div className="bg-white rounded-lg shadow-xl p-8 max-w-4xl mx-auto">
      <h2 className="text-3xl font-bold text-blue-900 mb-6">Javelin Throw</h2>

      <div className="mb-8 bg-gradient-to-b from-blue-100 to-green-100 rounded-lg p-8 border-4 border-gray-400">
        <svg width="100%" height="320" viewBox="0 0 800 320" className="bg-white rounded">
          <rect x="0" y="220" width="800" height="100" fill="#34A048" />

          {[100, 150, 200, 250, 300, 350, 400, 450, 500, 550, 600, 650, 700, 750].map((x, i) => (
            <g key={x}>
              <line x1={x} y1="220" x2={x} y2="235" stroke="#999" strokeWidth="1" />
              <text x={x} y="250" textAnchor="middle" fontSize="10" fill="#666">
                {i * 5}m
              </text>
            </g>
          ))}

          <circle cx="100" cy="200" r="8" fill="#FF6B6B" />
          <line x1="100" y1="208" x2="100" y2="220" stroke="#FF6B6B" strokeWidth="2" />

          {isThrowing ? (
            <g transform={`translate(${throwData.x}, ${throwData.y}) rotate(${(javelinAngle * 180) / Math.PI})`}>
              <line x1="-30" y1="0" x2="30" y2="0" stroke="#FFD700" strokeWidth="3" />
              <polygon points="30,0 40,-3 40,3" fill="#FF4444" />
            </g>
          ) : (
            <>
              <line x1="100" y1="190" x2="115" y2="175" stroke="#FFD700" strokeWidth="3" />
              <polygon points="115,175 120,172 118,180" fill="#FF4444" />
            </>
          )}

          {isComplete && (
            <>
              <line
                x1="100"
                y1="220"
                x2={throwData.x}
                y2="220"
                stroke="#FF0000"
                strokeWidth="2"
                strokeDasharray="5,5"
              />
              <circle cx={throwData.x} cy="220" r="4" fill="#FF0000" />
            </>
          )}
        </svg>

        <div className="mt-4 text-center text-lg font-semibold">
          Distance: {throwData.distance.toFixed(2)}m
        </div>
      </div>

      <div className="mb-6 space-y-4">
        <div>
          <label className="block text-lg font-semibold mb-2">
            Launch Angle: {angle}°
          </label>
          <input
            type="range"
            min="15"
            max="75"
            value={angle}
            onChange={(e) => setAngle(Number(e.target.value))}
            disabled={isThrowing || isComplete}
            className="w-full h-3 bg-gray-200 rounded-lg appearance-none cursor-pointer"
          />
          <div className="text-sm text-gray-600 mt-1">Optimal angle: 45°</div>
        </div>

        <div>
          <label className="block text-lg font-semibold mb-2">Power: {power}%</label>
          <input
            type="range"
            min="0"
            max="100"
            value={power}
            onChange={(e) => setPower(Number(e.target.value))}
            disabled={isThrowing || isComplete}
            className="w-full h-3 bg-gray-200 rounded-lg appearance-none cursor-pointer"
          />
        </div>
      </div>

      <div className="space-y-4">
        {!isComplete ? (
          <button
            onClick={handleThrow}
            disabled={isThrowing}
            className="w-full bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white py-3 rounded-lg font-bold flex items-center justify-center gap-2 text-lg transition-all"
          >
            <Play size={24} />
            {isThrowing ? 'Throwing...' : 'Throw!'}
          </button>
        ) : (
          <div>
            <div className="bg-green-50 border-2 border-green-500 rounded-lg p-4 mb-4">
              <p className="text-center text-xl font-bold text-green-700">
                Distance: {throwData.distance.toFixed(2)}m
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
