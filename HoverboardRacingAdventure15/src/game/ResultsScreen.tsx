import { useEffect, useState } from 'react';
import type { GameResult } from './types';

interface ResultsScreenProps {
  result: GameResult;
  onPlayAgain: () => void;
  onMenu: () => void;
}

export function ResultsScreen({ result, onPlayAgain, onMenu }: ResultsScreenProps) {
  const [show, setShow] = useState(false);

  useEffect(() => {
    const timer = setTimeout(() => setShow(true), 100);
    return () => clearTimeout(timer);
  }, []);

  const mins = Math.floor(result.finishTime / 60);
  const secs = Math.floor(result.finishTime % 60);
  const ms = Math.floor((result.finishTime % 1) * 100);

  return (
    <div className="min-h-screen w-full flex flex-col items-center justify-center relative overflow-hidden">
      <div className="absolute inset-0 bg-gradient-to-b from-[#0a0a2e] via-[#16213e] to-[#0f0f23]" />

      {/* Confetti particles */}
      <div className="absolute inset-0 overflow-hidden">
        {Array.from({ length: 30 }).map((_, i) => (
          <div
            key={i}
            className="absolute rounded-sm"
            style={{
              left: `${(i * 31) % 100}%`,
              top: `${(i * 47) % 100}%`,
              width: `${4 + (i % 3) * 3}px`,
              height: `${4 + (i % 3) * 3}px`,
              background: [result.winnerColor, '#ffd700', '#ffffff', '#00e5ff'][i % 4],
              opacity: 0.4 + (i % 5) * 0.1,
              animation: `confettiFall ${2 + (i % 3)}s ease-in infinite`,
              animationDelay: `${i * 0.15}s`,
            }}
          />
        ))}
      </div>

      <style>{`
        @keyframes confettiFall {
          0% { transform: translateY(-20px) rotate(0deg); opacity: 0; }
          20% { opacity: 1; }
          100% { transform: translateY(400px) rotate(360deg); opacity: 0; }
        }
        @keyframes scaleIn {
          from { opacity: 0; transform: scale(0.8); }
          to { opacity: 1; transform: scale(1); }
        }
      `}</style>

      <div
        className="relative z-10 flex flex-col items-center gap-6 px-4"
        style={{
          animation: 'scaleIn 0.5s ease-out',
          opacity: show ? 1 : 0,
          transition: 'opacity 0.3s',
        }}
      >
        {/* Trophy */}
        <div className="text-7xl mb-2">🏆</div>

        {/* Winner text */}
        <div className="text-center">
          <p className="text-white/50 text-lg font-medium tracking-wide">WINNER</p>
          <h1
            className="text-5xl font-black mt-2"
            style={{
              color: result.winnerColor,
              filter: `drop-shadow(0 0 20px ${result.winnerColor}80)`,
            }}
          >
            {result.winnerName}
          </h1>
        </div>

        {/* Finish time */}
        <div className="bg-white/5 border border-white/15 rounded-xl px-8 py-4 flex flex-col items-center">
          <span className="text-white/40 text-sm tracking-wider">FINISH TIME</span>
          <span className="text-3xl font-bold text-white mt-1 tabular-nums">
            {mins}:{String(secs).padStart(2, '0')}.{String(ms).padStart(2, '0')}
          </span>
        </div>

        {/* Mode badge */}
        <div className="text-white/40 text-sm">
          {result.mode === 'vs-computer' ? 'VS Computer' : 'Two Player'}
        </div>

        {/* Buttons */}
        <div className="flex gap-4 mt-4">
          <button
            onClick={onPlayAgain}
            className="px-8 py-3 rounded-xl text-lg font-bold transition-all duration-300 hover:scale-105 active:scale-95"
            style={{
              background: 'linear-gradient(135deg, #ff2d95, #00e5ff)',
              color: '#ffffff',
              boxShadow: '0 0 20px rgba(255, 45, 149, 0.4)',
            }}
          >
            Play Again
          </button>
          <button
            onClick={onMenu}
            className="px-8 py-3 rounded-xl text-lg font-bold bg-white/10 hover:bg-white/20 text-white border border-white/20 transition-all duration-300 hover:scale-105 active:scale-95"
          >
            Main Menu
          </button>
        </div>
      </div>
    </div>
  );
}
