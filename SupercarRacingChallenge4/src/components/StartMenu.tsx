import { useState } from 'react';
import { Zap, Cpu, Users, ChevronRight } from 'lucide-react';
import { CARS, type GameMode } from '@/game/types';

interface StartMenuProps {
  onStart: (mode: GameMode, car1Index: number, car2Index: number) => void;
}

export function StartMenu({ onStart }: StartMenuProps) {
  const [mode, setMode] = useState<GameMode>('p2');
  const [car1Index, setCar1Index] = useState(0);
  const [car2Index, setCar2Index] = useState(1);

  return (
    <div className="min-h-screen w-full flex flex-col items-center justify-center px-4 py-8 overflow-hidden relative scanline">
      {/* Animated background grid */}
      <div
        className="absolute inset-0 opacity-30"
        style={{
          backgroundImage: 'linear-gradient(rgba(0,240,255,0.1) 1px, transparent 1px), linear-gradient(90deg, rgba(0,240,255,0.1) 1px, transparent 1px)',
          backgroundSize: '50px 50px',
          animation: 'grid-scroll 1s linear infinite',
        }}
      />

      {/* Glow orbs */}
      <div className="absolute top-1/4 left-1/4 w-96 h-96 rounded-full opacity-20 blur-3xl" style={{ background: 'radial-gradient(circle, #00f0ff, transparent 70%)' }} />
      <div className="absolute bottom-1/4 right-1/4 w-96 h-96 rounded-full opacity-20 blur-3xl" style={{ background: 'radial-gradient(circle, #ff2bd6, transparent 70%)' }} />

      <div className="relative z-10 w-full max-w-3xl flex flex-col items-center gap-6">
        {/* Title */}
        <div className="text-center">
          <div className="flex items-center justify-center gap-3 mb-2">
            <Zap className="w-10 h-10 neon-text-cyan" />
            <h1 className="font-display font-black text-4xl sm:text-6xl animate-title-glow tracking-wider">
              <span className="neon-text-cyan">SUPER</span>
              <span className="neon-text-pink">CAR</span>
            </h1>
            <Zap className="w-10 h-10 neon-text-pink" />
          </div>
          <h2 className="font-display font-bold text-2xl sm:text-4xl neon-text-green tracking-[0.3em]">
            RACING CHALLENGE
          </h2>
          <p className="text-slate-400 mt-3 text-sm sm:text-base tracking-widest uppercase">
            Neon Velocity · Dual Lane Combat
          </p>
        </div>

        {/* Mode selection */}
        <div className="w-full max-w-md">
          <p className="text-center text-slate-300 text-sm uppercase tracking-widest mb-3 font-display">Select Mode</p>
          <div className="grid grid-cols-2 gap-3">
            <ModeButton
              active={mode === 'p2'}
              onClick={() => setMode('p2')}
              icon={<Users className="w-6 h-6" />}
              label="2 PLAYERS"
              color="cyan"
            />
            <ModeButton
              active={mode === 'cpu'}
              onClick={() => setMode('cpu')}
              icon={<Cpu className="w-6 h-6" />}
              label="VS COMPUTER"
              color="pink"
            />
          </div>
        </div>

        {/* Car selection */}
        <div className="w-full max-w-2xl">
          <p className="text-center text-slate-300 text-sm uppercase tracking-widest mb-3 font-display">Choose Your Supercars</p>
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <CarSelector
              label="PLAYER 1"
              color="cyan"
              selectedIndex={car1Index}
              onSelect={setCar1Index}
              disabledIndex={car2Index}
            />
            <CarSelector
              label={mode === 'cpu' ? 'COMPUTER' : 'PLAYER 2'}
              color="pink"
              selectedIndex={car2Index}
              onSelect={setCar2Index}
              disabledIndex={car1Index}
            />
          </div>
        </div>

        {/* Controls hint */}
        <div className="w-full max-w-2xl grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
          <div className="border border-cyan-500/30 rounded-lg p-3 bg-slate-900/40">
            <p className="neon-text-cyan font-display font-bold mb-1">PLAYER 1</p>
            <p className="text-slate-400">Lanes: A / D · Boost: W</p>
          </div>
          <div className="border border-pink-500/30 rounded-lg p-3 bg-slate-900/40">
            <p className="neon-text-pink font-display font-bold mb-1">{mode === 'cpu' ? 'COMPUTER' : 'PLAYER 2'}</p>
            <p className="text-slate-400">{mode === 'cpu' ? 'AI dodges & boosts' : 'Lanes: J / L · Boost: I'}</p>
          </div>
        </div>

        {/* Start button */}
        <button
          onClick={() => onStart(mode, car1Index, car2Index)}
          className="group relative px-12 py-4 rounded-xl bg-gradient-to-r from-cyan-500/20 to-pink-500/20 border-2 border-cyan-400/60 hover:border-cyan-300 transition-all duration-300 hover:scale-105"
          style={{ boxShadow: '0 0 30px rgba(0, 240, 255, 0.3)' }}
        >
          <span className="font-display font-black text-xl tracking-widest neon-text-cyan flex items-center gap-2">
            START RACE
            <ChevronRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
          </span>
        </button>
      </div>
    </div>
  );
}

function ModeButton({ active, onClick, icon, label, color }: { active: boolean; onClick: () => void; icon: React.ReactNode; label: string; color: string }) {
  const colorClass = color === 'cyan' ? 'neon-border-cyan neon-text-cyan' : 'neon-border-pink neon-text-pink';
  const inactiveClass = 'border-slate-700 text-slate-500';

  return (
    <button
      onClick={onClick}
      className={`flex flex-col items-center gap-2 py-4 rounded-xl border-2 transition-all duration-300 ${active ? `${colorClass} bg-slate-900/60 scale-105` : `${inactiveClass} bg-slate-900/20 hover:border-slate-500`}`}
    >
      {icon}
      <span className="font-display font-bold text-sm tracking-wider">{label}</span>
    </button>
  );
}

function CarSelector({ label, color, selectedIndex, onSelect, disabledIndex }: { label: string; color: string; selectedIndex: number; onSelect: (i: number) => void; disabledIndex: number }) {
  const accent = color === 'cyan' ? 'neon-text-cyan' : 'neon-text-pink';
  return (
    <div className="border border-slate-700/50 rounded-xl p-3 bg-slate-900/40">
      <p className={`${accent} font-display font-bold text-xs tracking-widest mb-2`}>{label}</p>
      <div className="flex gap-2 flex-wrap">
        {CARS.map((car, i) => {
          const isSelected = i === selectedIndex;
          const isDisabled = i === disabledIndex;
          return (
            <button
              key={i}
              disabled={isDisabled}
              onClick={() => onSelect(i)}
              className={`relative w-12 h-12 rounded-lg border-2 transition-all duration-200 ${isSelected ? 'scale-110' : 'opacity-50 hover:opacity-80'} ${isDisabled ? 'cursor-not-allowed opacity-20' : ''}`}
              style={{
                backgroundColor: car.bodyColor + '30',
                borderColor: isSelected ? car.bodyColor : 'transparent',
                boxShadow: isSelected ? `0 0 15px ${car.glowColor}` : 'none',
              }}
              title={car.name}
            >
              <div className="absolute inset-0 flex items-center justify-center">
                <div className="w-6 h-9 rounded" style={{ backgroundColor: car.bodyColor, boxShadow: `0 0 8px ${car.glowColor}` }} />
              </div>
            </button>
          );
        })}
      </div>
      <p className="text-center mt-2 font-display text-sm" style={{ color: CARS[selectedIndex].bodyColor, textShadow: `0 0 8px ${CARS[selectedIndex].glowColor}` }}>
        {CARS[selectedIndex].name}
      </p>
    </div>
  );
}
