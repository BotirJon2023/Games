import { useState } from 'react';
import { Mountain, Trophy, Cpu, Users, Gauge, Zap } from 'lucide-react';
import type { GameMode } from '../game/types';

interface StartMenuProps {
  onStart: (mode: GameMode) => void;
}

export function StartMenu({ onStart }: StartMenuProps) {
  const [hoveredMode, setHoveredMode] = useState<GameMode | null>(null);

  return (
    <div className="min-h-screen w-full relative overflow-hidden bg-gradient-to-b from-sky-700 via-sky-500 to-emerald-800">
      {/* Animated mountain silhouettes */}
      <div className="absolute inset-0 pointer-events-none">
        <svg className="absolute bottom-0 w-full" viewBox="0 0 1200 400" preserveAspectRatio="none" style={{ height: '60%' }}>
          <polygon points="0,400 150,180 280,250 420,120 580,280 700,160 850,300 1000,140 1200,220 1200,400" fill="#1a3a2a" opacity="0.8" />
          <polygon points="0,400 100,280 250,350 380,220 520,360 680,260 820,340 960,240 1100,320 1200,280 1200,400" fill="#0d2418" opacity="0.9" />
        </svg>
        {/* Sun glow */}
        <div className="absolute top-20 right-1/4 w-40 h-40 rounded-full bg-yellow-200 opacity-30 blur-3xl" />
        <div className="absolute top-24 right-1/4 w-24 h-24 rounded-full bg-yellow-100 opacity-60 blur-xl" />
      </div>

      {/* Floating particles */}
      <div className="absolute inset-0 pointer-events-none">
        {Array.from({ length: 20 }).map((_, i) => (
          <div
            key={i}
            className="absolute rounded-full bg-white opacity-20"
            style={{
              width: `${2 + Math.random() * 4}px`,
              height: `${2 + Math.random() * 4}px`,
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 60}%`,
              animation: `float ${3 + Math.random() * 4}s ease-in-out infinite`,
              animationDelay: `${Math.random() * 3}s`,
            }}
          />
        ))}
      </div>

      {/* Content */}
      <div className="relative z-10 flex flex-col items-center justify-center min-h-screen px-4 py-8">
        {/* Title */}
        <div className="text-center mb-8 animate-[fadeInDown_0.8s_ease-out]">
          <div className="flex items-center justify-center gap-3 mb-3">
            <Mountain className="w-12 h-12 text-emerald-300 drop-shadow-lg" />
            <h1 className="text-5xl md:text-7xl font-black text-white tracking-tight drop-shadow-2xl">
              EXTREME
            </h1>
          </div>
          <h2 className="text-3xl md:text-5xl font-bold text-emerald-200 tracking-widest drop-shadow-lg">
            MOUNTAIN BIKE RACING
          </h2>
          <p className="text-sky-200 mt-4 text-sm md:text-base font-medium tracking-wide">
            Race down treacherous mountain trails at breakneck speed
          </p>
        </div>

        {/* Mode selection */}
        <div className="flex flex-col md:flex-row gap-4 md:gap-6 mb-8 animate-[fadeInUp_0.8s_ease-out_0.2s_both]">
          <ModeCard
            icon={<Cpu className="w-8 h-8" />}
            title="VS COMPUTER"
            subtitle="Race against an AI opponent"
            accent="from-cyan-500 to-blue-600"
            highlighted={hoveredMode === 'vs-cpu'}
            onHover={() => setHoveredMode('vs-cpu')}
            onLeave={() => setHoveredMode(null)}
            onClick={() => onStart('vs-cpu')}
          />
          <ModeCard
            icon={<Users className="w-8 h-8" />}
            title="TWO PLAYERS"
            subtitle="Split-screen local multiplayer"
            accent="from-orange-500 to-red-600"
            highlighted={hoveredMode === 'two-players'}
            onHover={() => setHoveredMode('two-players')}
            onLeave={() => setHoveredMode(null)}
            onClick={() => onStart('two-players')}
          />
        </div>

        {/* Controls reference */}
        <div className="bg-black/30 backdrop-blur-sm rounded-2xl px-6 py-5 max-w-2xl w-full border border-white/10 animate-[fadeInUp_0.8s_ease-out_0.4s_both]">
          <h3 className="text-white font-bold text-sm mb-3 flex items-center gap-2">
            <Gauge className="w-4 h-4 text-emerald-300" />
            CONTROLS
          </h3>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-xs">
            <div>
              <p className="text-red-400 font-bold mb-1.5">Player 1</p>
              <div className="space-y-1 text-sky-100">
                <p><Key>D</Key> / <Key>→</Key> Accelerate</p>
                <p><Key>A</Key> / <Key>←</Key> Brake</p>
                <p><Key>A</Key>/<Key>D</Key> in air Rotate</p>
                <p><Key>Space</Key> / <Key>Shift</Key> Boost</p>
              </div>
            </div>
            <div>
              <p className="text-blue-400 font-bold mb-1.5">Player 2</p>
              <div className="space-y-1 text-sky-100">
                <p><Key>L</Key> Accelerate</p>
                <p><Key>J</Key> Brake</p>
                <p><Key>I</Key>/<Key>K</Key> in air Rotate</p>
                <p><Key>O</Key> / <Key>R-Shift</Key> Boost</p>
              </div>
            </div>
          </div>
          <div className="mt-3 pt-3 border-t border-white/10 flex items-center gap-2 text-xs text-amber-300">
            <Zap className="w-4 h-4" />
            <span>Perform backflips in the air for bonus trick points! Build boost by riding at high speed.</span>
          </div>
        </div>
      </div>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px); opacity: 0.2; }
          50% { transform: translateY(-20px); opacity: 0.4; }
        }
        @keyframes fadeInDown {
          from { opacity: 0; transform: translateY(-30px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes fadeInUp {
          from { opacity: 0; transform: translateY(30px); }
          to { opacity: 1; transform: translateY(0); }
        }
      `}</style>
    </div>
  );
}

function ModeCard({
  icon,
  title,
  subtitle,
  accent,
  highlighted,
  onHover,
  onLeave,
  onClick,
}: {
  icon: React.ReactNode;
  title: string;
  subtitle: string;
  accent: string;
  highlighted: boolean;
  onHover: () => void;
  onLeave: () => void;
  onClick: () => void;
}) {
  return (
    <button
      onMouseEnter={onHover}
      onMouseLeave={onLeave}
      onClick={onClick}
      className={`group relative w-72 rounded-2xl p-6 text-left transition-all duration-300 transform hover:scale-105 hover:-translate-y-1 ${
        highlighted ? 'scale-105 -translate-y-1' : ''
      }`}
    >
      <div className={`absolute inset-0 rounded-2xl bg-gradient-to-br ${accent} opacity-90`} />
      <div className="absolute inset-0 rounded-2xl bg-black/20 group-hover:bg-black/10 transition-colors" />
      <div className="relative z-10">
        <div className="flex items-center gap-3 mb-3">
          <div className="p-2 rounded-lg bg-white/20 backdrop-blur-sm">{icon}</div>
          <h3 className="text-white font-black text-xl tracking-wide">{title}</h3>
        </div>
        <p className="text-white/80 text-sm">{subtitle}</p>
        <div className="mt-4 flex items-center gap-2 text-white/90 text-sm font-bold opacity-0 group-hover:opacity-100 transition-opacity">
          <Trophy className="w-4 h-4" />
          <span>Start Racing</span>
        </div>
      </div>
    </button>
  );
}

function Key({ children }: { children: React.ReactNode }) {
  return (
    <span className="inline-block px-2 py-0.5 rounded bg-white/15 border border-white/20 text-white font-mono text-xs font-bold">
      {children}
    </span>
  );
}
