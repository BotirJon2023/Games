import { Trophy, RotateCcw, Home, Zap } from 'lucide-react';
import type { RaceResult } from '@/game/types';
import { CARS } from '@/game/types';

interface ResultsScreenProps {
  result: RaceResult;
  onRestart: () => void;
  onMenu: () => void;
}

export function ResultsScreen({ result, onRestart, onMenu }: ResultsScreenProps) {
  const winnerCar = CARS.find(c => c.name === result.winnerName) || CARS[0];

  return (
    <div className="min-h-screen w-full flex flex-col items-center justify-center px-4 py-8 overflow-hidden relative scanline">
      {/* Background glow */}
      <div className="absolute inset-0 opacity-30">
        <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-[600px] h-[600px] rounded-full blur-3xl" style={{ background: `radial-gradient(circle, ${winnerCar.bodyColor}40, transparent 70%)` }} />
      </div>

      {/* Confetti / speed lines */}
      <div className="absolute inset-0 pointer-events-none">
        {Array.from({ length: 20 }).map((_, i) => (
          <div
            key={i}
            className="absolute h-0.5 w-20 opacity-20"
            style={{
              top: `${Math.random() * 100}%`,
              left: `${Math.random() * 100}%`,
              background: `linear-gradient(90deg, transparent, ${i % 2 === 0 ? '#00f0ff' : '#ff2bd6'}, transparent)`,
              animation: `float-up ${2 + Math.random() * 3}s ease-out infinite`,
              animationDelay: `${Math.random() * 2}s`,
            }}
          />
        ))}
      </div>

      <div className="relative z-10 w-full max-w-lg flex flex-col items-center gap-6">
        {/* Trophy */}
        <div className="animate-count-bounce">
          <Trophy className="w-20 h-20" style={{ color: winnerCar.bodyColor, filter: `drop-shadow(0 0 20px ${winnerCar.glowColor})` }} />
        </div>

        {/* Winner text */}
        <div className="text-center animate-slide-in-left">
          <p className="text-slate-400 uppercase tracking-widest text-sm font-display">Winner</p>
          <h1 className="font-display font-black text-4xl sm:text-5xl mt-1" style={{ color: winnerCar.bodyColor, textShadow: `0 0 20px ${winnerCar.glowColor}, 0 0 40px ${winnerCar.glowColor}` }}>
            {result.winnerName}
          </h1>
          <p className="text-slate-300 mt-2 text-lg">
            Player {result.winnerLane + 1} takes the victory!
          </p>
        </div>

        {/* Stats */}
        <div className="w-full grid grid-cols-2 gap-3 animate-slide-in-right">
          <StatCard label="WINNER TIME" value={formatTime(result.winnerTime)} color={winnerCar.bodyColor} highlight />
          <StatCard label="RUNNER UP" value={formatTime(result.loserTime)} color="#94a3b8" />
        </div>

        <div className="w-full bg-slate-900/50 border border-slate-700/50 rounded-xl p-4 flex items-center justify-center gap-3">
          <Zap className="w-5 h-5 neon-text-yellow" />
          <span className="text-slate-300 text-sm">
            {result.mode === 'cpu' ? 'You raced against the Computer' : 'Head-to-head battle complete'}
          </span>
        </div>

        {/* Actions */}
        <div className="flex gap-3 w-full">
          <button
            onClick={onMenu}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl border-2 border-slate-600/50 text-slate-300 hover:border-slate-400 hover:text-white transition-all font-display font-bold tracking-widest text-sm"
          >
            <Home className="w-4 h-4" />
            MENU
          </button>
          <button
            onClick={onRestart}
            className="flex-1 flex items-center justify-center gap-2 py-3 rounded-xl border-2 border-cyan-400/60 text-cyan-300 hover:border-cyan-300 transition-all font-display font-bold tracking-widest text-sm neon-border-cyan"
          >
            <RotateCcw className="w-4 h-4" />
            RACE AGAIN
          </button>
        </div>
      </div>
    </div>
  );
}

function StatCard({ label, value, color, highlight }: { label: string; value: string; color: string; highlight?: boolean }) {
  return (
    <div className={`rounded-xl p-4 border ${highlight ? 'border-current' : 'border-slate-700/50'} bg-slate-900/50`} style={highlight ? { borderColor: color + '60', boxShadow: `0 0 15px ${color}30` } : {}}>
      <p className="text-slate-400 uppercase tracking-widest text-xs font-display">{label}</p>
      <p className="font-display font-black text-2xl mt-1" style={{ color, textShadow: highlight ? `0 0 10px ${color}80` : 'none' }}>
        {value}
      </p>
    </div>
  );
}

function formatTime(ms: number): string {
  const seconds = ms / 1000;
  const s = Math.floor(seconds);
  const ms2 = Math.floor((seconds - s) * 100);
  return `${Math.floor(s / 60)}'${String(s % 60).padStart(2, '0')}"${String(ms2).padStart(2, '0')}`;
}
