import { Trophy, Medal, Clock, RotateCcw, Home, Flag } from 'lucide-react';
import type { RaceState } from '@/game/types';
import { carOption } from '@/game/types';

interface Props {
  state: RaceState;
  onRestart: () => void;
  onMenu: () => void;
}

export function Results({ state, onRestart, onMenu }: Props) {
  const ranked = [...state.cars].sort((a, b) => a.place - b.place);
  const winner = ranked[0];
  const wOpt = carOption(winner.colorId);

  const fmt = (ms: number) => {
    if (!ms) return '--';
    const s = Math.floor(ms / 1000);
    const m = Math.floor(s / 60);
    const cs = Math.floor((ms % 1000) / 10);
    return `${m}:${(s % 60).toString().padStart(2, '0')}.${cs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="min-h-screen w-full flex items-center justify-center bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 text-white p-6">
      <div className="w-full max-w-lg">
        <div className="text-center mb-8">
          <div className="inline-flex items-center justify-center w-20 h-20 rounded-full bg-gradient-to-br from-amber-400 to-amber-600 shadow-lg shadow-amber-500/30 mb-4">
            <Trophy className="w-10 h-10 text-white" />
          </div>
          <p className="text-sm uppercase tracking-[0.3em] text-amber-400">Race Complete</p>
          <h1 className="text-4xl font-black mt-1">{wOpt.name} Wins!</h1>
          <p className="text-slate-400 mt-1 flex items-center justify-center gap-1.5">
            <Flag className="w-4 h-4" /> {state.track.name} · {state.track.country}
          </p>
        </div>

        <div className="space-y-2 mb-6">
          {ranked.map((c, i) => {
            const opt = carOption(c.colorId);
            const medal = i === 0 ? 'gold' : i === 1 ? 'silver' : 'bronze';
            return (
              <div
                key={c.id}
                className={`flex items-center gap-3 rounded-xl p-4 border ${
                  i === 0 ? 'bg-amber-500/10 border-amber-500/40' : 'bg-white/5 border-white/10'
                }`}
              >
                <div className={`w-8 h-8 rounded-full flex items-center justify-center font-bold text-sm ${
                  medal === 'gold' ? 'bg-amber-400 text-amber-950' :
                  medal === 'silver' ? 'bg-slate-300 text-slate-800' :
                  'bg-amber-700 text-amber-100'
                }`}>
                  {i + 1}
                </div>
                <div className="w-7 h-7 rounded-md flex items-center justify-center" style={{ background: opt.body }}>
                  <div className="w-4 h-2 rounded-sm" style={{ background: opt.accent }} />
                </div>
                <div className="flex-1">
                  <div className="font-semibold">{c.name}</div>
                  <div className="text-xs text-slate-400 flex items-center gap-3">
                    <span className="flex items-center gap-1"><Clock className="w-3 h-3" /> Finish: {fmt(c.finishTime)}</span>
                  </div>
                </div>
                <div className="text-right">
                  <div className="text-xs text-slate-400">Best Lap</div>
                  <div className="font-mono text-sm text-sky-300">{fmt(c.bestLap)}</div>
                </div>
              </div>
            );
          })}
        </div>

        <div className="flex gap-3">
          <button
            onClick={onRestart}
            className="flex-1 py-3.5 rounded-xl bg-gradient-to-r from-sky-500 to-cyan-400 text-slate-950 font-bold hover:from-sky-400 hover:to-cyan-300 transition flex items-center justify-center gap-2"
          >
            <RotateCcw className="w-5 h-5" /> Race Again
          </button>
          <button
            onClick={onMenu}
            className="flex-1 py-3.5 rounded-xl bg-white/10 border border-white/15 font-semibold hover:bg-white/15 transition flex items-center justify-center gap-2"
          >
            <Home className="w-5 h-5" /> Main Menu
          </button>
        </div>
      </div>
    </div>
  );
}
