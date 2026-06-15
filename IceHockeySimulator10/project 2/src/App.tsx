import { useState } from 'react';
import HockeyGame from './components/HockeyGame';
import type { PlayerMode } from './game/types';
import { TEAM_NAMES, TEAM_COLORS, TOTAL_PERIODS, PERIOD_DURATION } from './game/constants';

type Screen = 'menu' | 'playing' | 'results';

interface ResultData {
  scores: [number, number];
  playerMode: PlayerMode;
}

export default function App() {
  const [screen, setScreen] = useState<Screen>('menu');
  const [playerMode, setPlayerMode] = useState<PlayerMode>('vs_computer');
  const [result, setResult] = useState<ResultData | null>(null);

  function startGame(mode: PlayerMode) {
    setPlayerMode(mode);
    setScreen('playing');
  }

  function handleGameOver(scores: [number, number]) {
    setResult({ scores, playerMode });
    setScreen('results');
  }

  if (screen === 'playing') {
    return (
      <HockeyGame
        playerMode={playerMode}
        onGameOver={handleGameOver}
        onQuit={() => setScreen('menu')}
      />
    );
  }

  if (screen === 'results' && result) {
    const winner = result.scores[0] > result.scores[1] ? 0 : result.scores[1] > result.scores[0] ? 1 : -1;
    return (
      <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center">
        <div className="w-full max-w-lg mx-auto px-6">
          {/* Trophy area */}
          <div className="text-center mb-8">
            <div className="text-7xl mb-4" style={{ fontFamily: 'serif' }}>🏒</div>
            <h2 className="text-white/50 text-sm font-mono tracking-widest uppercase mb-2">Final Score</h2>
            {winner === -1 ? (
              <h1 className="text-3xl font-bold text-white">TIE GAME!</h1>
            ) : (
              <h1 className="text-3xl font-bold" style={{ color: TEAM_COLORS[winner] }}>
                {TEAM_NAMES[winner]} WINS!
              </h1>
            )}
          </div>

          {/* Score box */}
          <div className="flex items-center justify-center gap-0 mb-8 rounded-2xl overflow-hidden border border-white/10">
            <div className="flex-1 py-8 text-center" style={{ background: TEAM_COLORS[0] + '22', borderRight: '1px solid rgba(255,255,255,0.1)' }}>
              <div className="text-xs font-mono tracking-widest uppercase mb-2" style={{ color: TEAM_COLORS[0] }}>
                {TEAM_NAMES[0]}
              </div>
              <div className="text-6xl font-bold text-white">{result.scores[0]}</div>
            </div>
            <div className="px-6 py-8 text-white/30 text-2xl font-bold bg-white/5">—</div>
            <div className="flex-1 py-8 text-center" style={{ background: TEAM_COLORS[1] + '22', borderLeft: '1px solid rgba(255,255,255,0.1)' }}>
              <div className="text-xs font-mono tracking-widest uppercase mb-2" style={{ color: TEAM_COLORS[1] }}>
                {TEAM_NAMES[1]}
              </div>
              <div className="text-6xl font-bold text-white">{result.scores[1]}</div>
            </div>
          </div>

          {/* Periods info */}
          <p className="text-center text-white/30 text-sm mb-8 font-mono">
            {TOTAL_PERIODS} periods × {PERIOD_DURATION / 60} minutes
          </p>

          {/* Actions */}
          <div className="flex flex-col gap-3">
            <button
              onClick={() => startGame(result.playerMode)}
              className="w-full py-4 rounded-xl font-bold text-white text-lg tracking-wide transition-all duration-200 hover:scale-[1.02] active:scale-[0.98]"
              style={{ background: `linear-gradient(135deg, ${TEAM_COLORS[0]}, ${TEAM_COLORS[1]})` }}
            >
              PLAY AGAIN
            </button>
            <button
              onClick={() => setScreen('menu')}
              className="w-full py-3 rounded-xl font-semibold text-white/70 text-base tracking-wide bg-white/5 hover:bg-white/10 border border-white/10 transition-all duration-200"
            >
              Main Menu
            </button>
          </div>
        </div>
      </div>
    );
  }

  // Menu screen
  return (
    <div className="min-h-screen bg-gray-950 flex flex-col items-center justify-center relative overflow-hidden">
      {/* Background rink lines decoration */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden opacity-5">
        <div className="absolute left-1/2 top-0 bottom-0 w-0.5 bg-red-400" />
        <div className="absolute left-1/3 top-0 bottom-0 w-1.5 bg-blue-400" />
        <div className="absolute left-2/3 top-0 bottom-0 w-1.5 bg-blue-400" />
        <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 w-40 h-40 rounded-full border-2 border-red-400" />
      </div>

      <div className="w-full max-w-md mx-auto px-6 z-10">
        {/* Logo */}
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="w-10 h-1 rounded" style={{ background: TEAM_COLORS[0] }} />
            <span className="text-4xl select-none">🏒</span>
            <div className="w-10 h-1 rounded" style={{ background: TEAM_COLORS[1] }} />
          </div>
          <h1 className="text-5xl font-black text-white tracking-tight mb-2">
            ICE HOCKEY
          </h1>
          <p className="text-white/40 text-sm font-mono tracking-widest uppercase">Simulator</p>
        </div>

        {/* Game info */}
        <div className="flex justify-center gap-6 mb-10">
          <div className="text-center">
            <div className="text-white text-2xl font-bold">{TOTAL_PERIODS}</div>
            <div className="text-white/40 text-xs font-mono uppercase tracking-wide">Periods</div>
          </div>
          <div className="w-px bg-white/10" />
          <div className="text-center">
            <div className="text-white text-2xl font-bold">3v3</div>
            <div className="text-white/40 text-xs font-mono uppercase tracking-wide">Players</div>
          </div>
          <div className="w-px bg-white/10" />
          <div className="text-center">
            <div className="text-white text-2xl font-bold">{PERIOD_DURATION / 60}m</div>
            <div className="text-white/40 text-xs font-mono uppercase tracking-wide">Per Period</div>
          </div>
        </div>

        {/* Mode selection */}
        <div className="space-y-3 mb-10">
          <button
            onClick={() => startGame('vs_computer')}
            className="w-full group relative py-5 px-6 rounded-2xl text-white font-bold text-lg tracking-wide overflow-hidden transition-all duration-200 hover:scale-[1.02] active:scale-[0.98] border border-white/10 hover:border-white/20"
            style={{ background: 'linear-gradient(135deg, #0f2a5a 0%, #1a3a7a 100%)' }}
          >
            <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity"
              style={{ background: 'linear-gradient(135deg, rgba(26,107,212,0.3), rgba(26,107,212,0.1))' }} />
            <div className="relative flex items-center justify-between">
              <div className="text-left">
                <div className="text-lg font-bold">vs Computer</div>
                <div className="text-sm font-normal text-white/50 mt-0.5">Single player — battle AI</div>
              </div>
              <div className="text-2xl opacity-60">CPU</div>
            </div>
          </button>

          <button
            onClick={() => startGame('two_player')}
            className="w-full group relative py-5 px-6 rounded-2xl text-white font-bold text-lg tracking-wide overflow-hidden transition-all duration-200 hover:scale-[1.02] active:scale-[0.98] border border-white/10 hover:border-white/20"
            style={{ background: 'linear-gradient(135deg, #3a0f10 0%, #6a1515 100%)' }}
          >
            <div className="absolute inset-0 opacity-0 group-hover:opacity-100 transition-opacity"
              style={{ background: 'linear-gradient(135deg, rgba(212,42,26,0.3), rgba(212,42,26,0.1))' }} />
            <div className="relative flex items-center justify-between">
              <div className="text-left">
                <div className="text-lg font-bold">2 Players</div>
                <div className="text-sm font-normal text-white/50 mt-0.5">Local co-op — same keyboard</div>
              </div>
              <div className="text-2xl opacity-60">P1+P2</div>
            </div>
          </button>
        </div>

        {/* Controls reference */}
        <div className="rounded-xl bg-white/5 border border-white/10 p-4 space-y-3">
          <h3 className="text-white/50 text-xs font-mono uppercase tracking-widest text-center">Controls</h3>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <div className="text-xs font-mono text-white/30 mb-1.5" style={{ color: TEAM_COLORS[0] }}>
                {TEAM_NAMES[0]} (P1)
              </div>
              <div className="space-y-1 text-xs text-white/60 font-mono">
                <div><span className="text-white/40">Move</span> · WASD</div>
                <div><span className="text-white/40">Shoot</span> · F key</div>
                <div><span className="text-white/40">Pass</span> · G key</div>
                <div><span className="text-white/40">Switch</span> · Tab</div>
              </div>
            </div>
            <div>
              <div className="text-xs font-mono mb-1.5" style={{ color: TEAM_COLORS[1] }}>
                {TEAM_NAMES[1]} (P2)
              </div>
              <div className="space-y-1 text-xs text-white/60 font-mono">
                <div><span className="text-white/40">Move</span> · Arrows</div>
                <div><span className="text-white/40">Shoot</span> · L key</div>
                <div><span className="text-white/40">Pass</span> · . key</div>
                <div><span className="text-white/40">Switch</span> · R-Shift</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
