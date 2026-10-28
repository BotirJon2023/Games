import { useState } from 'react';
import type { GameMode } from './types';
import { COLORS, TOTAL_LAPS } from './constants';

interface MenuScreenProps {
  onStart: (mode: GameMode) => void;
}

export function MenuScreen({ onStart }: MenuScreenProps) {
  const [selectedMode, setSelectedMode] = useState<GameMode | null>(null);

  return (
    <div className="min-h-screen w-full flex flex-col items-center justify-center relative overflow-hidden">
      {/* Animated background */}
      <div className="absolute inset-0 bg-gradient-to-b from-[#0a0a2e] via-[#16213e] to-[#0f0f23]" />

      {/* Floating particles */}
      <div className="absolute inset-0 overflow-hidden">
        {Array.from({ length: 20 }).map((_, i) => (
          <div
            key={i}
            className="absolute rounded-full"
            style={{
              left: `${(i * 37) % 100}%`,
              top: `${(i * 53) % 100}%`,
              width: `${2 + (i % 3) * 2}px`,
              height: `${2 + (i % 3) * 2}px`,
              background: i % 2 === 0 ? '#ff2d95' : '#00e5ff',
              opacity: 0.3 + (i % 5) * 0.1,
              animation: `float ${3 + (i % 4)}s ease-in-out infinite`,
              animationDelay: `${i * 0.3}s`,
            }}
          />
        ))}
      </div>

      <style>{`
        @keyframes float {
          0%, 100% { transform: translateY(0px); }
          50% { transform: translateY(-30px); }
        }
        @keyframes slideUp {
          from { opacity: 0; transform: translateY(30px); }
          to { opacity: 1; transform: translateY(0); }
        }
        @keyframes glowPulse {
          0%, 100% { filter: drop-shadow(0 0 20px rgba(255, 45, 149, 0.4)); }
          50% { filter: drop-shadow(0 0 40px rgba(255, 45, 149, 0.7)); }
        }
      `}</style>

      <div className="relative z-10 flex flex-col items-center gap-8 px-4" style={{ animation: 'slideUp 0.6s ease-out' }}>
        {/* Title */}
        <div className="text-center" style={{ animation: 'glowPulse 3s ease-in-out infinite' }}>
          <h1 className="text-6xl md:text-7xl font-black tracking-tight">
            <span className="bg-gradient-to-r from-[#ff2d95] to-[#ff6ec7] bg-clip-text text-transparent">
              HOVERBOARD
            </span>
          </h1>
          <h2 className="text-4xl md:text-5xl font-bold tracking-widest mt-2">
            <span className="bg-gradient-to-r from-[#00e5ff] to-[#7df9ff] bg-clip-text text-transparent">
              RACING ADVENTURE
            </span>
          </h2>
          <p className="text-white/50 mt-4 text-lg font-medium tracking-wide">
            {TOTAL_LAPS} Laps · Boost Pads · Neon Circuit
          </p>
        </div>

        {/* Mode selection */}
        <div className="flex flex-col sm:flex-row gap-6 mt-8">
          <ModeCard
            title="VS COMPUTER"
            subtitle="Race against AI"
            color={COLORS.p1.primary}
            glowColor={COLORS.p1.glow}
            icon="bot"
            onClick={() => setSelectedMode('vs-computer')}
            selected={selectedMode === 'vs-computer'}
          />
          <ModeCard
            title="TWO PLAYER"
            subtitle="Race a friend"
            color={COLORS.p2.primary}
            glowColor={COLORS.p2.glow}
            icon="users"
            onClick={() => setSelectedMode('vs-player')}
            selected={selectedMode === 'vs-player'}
          />
        </div>

        {/* Start button */}
        {selectedMode && (
          <button
            onClick={() => onStart(selectedMode)}
            className="mt-6 px-12 py-4 rounded-xl text-xl font-bold transition-all duration-300 hover:scale-105 active:scale-95"
            style={{
              background: 'linear-gradient(135deg, #ff2d95, #00e5ff)',
              color: '#ffffff',
              boxShadow: '0 0 30px rgba(255, 45, 149, 0.5)',
              animation: 'slideUp 0.4s ease-out',
            }}
          >
            START RACE
          </button>
        )}

        {/* Controls info */}
        <div className="mt-4 flex flex-col sm:flex-row gap-8 text-sm">
          <div className="flex flex-col items-center gap-2">
            <div className="flex items-center gap-2" style={{ color: COLORS.p1.primary }}>
              <span className="font-bold">Player 1</span>
            </div>
            <div className="text-white/60 flex gap-3">
              <KeyCap>↑</KeyCap>
              <KeyCap>←</KeyCap>
              <KeyCap>→</KeyCap>
              <KeyCap>↓</KeyCap>
            </div>
            <div className="text-white/40 text-xs">Boost: <KeyCap small>Space</KeyCap></div>
          </div>

          <div className="flex flex-col items-center gap-2">
            <div className="flex items-center gap-2" style={{ color: COLORS.p2.primary }}>
              <span className="font-bold">Player 2</span>
            </div>
            <div className="text-white/60 flex gap-3">
              <KeyCap>W</KeyCap>
              <KeyCap>A</KeyCap>
              <KeyCap>D</KeyCap>
              <KeyCap>S</KeyCap>
            </div>
            <div className="text-white/40 text-xs">Boost: <KeyCap small>Shift</KeyCap></div>
          </div>
        </div>
      </div>
    </div>
  );
}

function ModeCard({
  title,
  subtitle,
  color,
  glowColor,
  icon,
  onClick,
  selected,
}: {
  title: string;
  subtitle: string;
  color: string;
  glowColor: string;
  icon: 'bot' | 'users';
  onClick: () => void;
  selected: boolean;
}) {
  return (
    <button
      onClick={onClick}
      className="relative w-64 h-44 rounded-2xl flex flex-col items-center justify-center gap-3 transition-all duration-300 hover:scale-105 active:scale-95"
      style={{
        background: selected
          ? `linear-gradient(135deg, ${color}30, ${glowColor}20)`
          : 'rgba(255,255,255,0.05)',
        border: `2px solid ${selected ? color : 'rgba(255,255,255,0.15)'}`,
        boxShadow: selected ? `0 0 25px ${color}50` : 'none',
      }}
    >
      {/* Icon */}
      <div className="text-5xl mb-2" style={{ color }}>
        {icon === 'bot' ? '🤖' : '👥'}
      </div>
      <div className="text-xl font-bold tracking-wide" style={{ color }}>
        {title}
      </div>
      <div className="text-white/50 text-sm">{subtitle}</div>

      {selected && (
        <div
          className="absolute -top-2 -right-2 w-6 h-6 rounded-full flex items-center justify-center text-white text-xs font-bold"
          style={{ background: color }}
        >
          ✓
        </div>
      )}
    </button>
  );
}

function KeyCap({ children, small }: { children: React.ReactNode; small?: boolean }) {
  return (
    <span
      className={`inline-flex items-center justify-center rounded-md border border-white/30 bg-white/10 text-white font-bold ${small ? 'text-xs px-2 py-0.5' : 'text-sm w-8 h-8'}`}
    >
      {children}
    </span>
  );
}
