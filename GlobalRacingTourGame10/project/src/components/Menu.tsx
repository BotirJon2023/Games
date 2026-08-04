import { useState } from 'react';
import { Globe, Cpu, Users, User, MapPin, ChevronRight, Trophy, Gauge, Zap } from 'lucide-react';
import { TRACKS, type TrackId } from '@/game/trackData';
import { CAR_OPTIONS, type CarColorId, type GameMode } from '@/game/types';
import { carOption } from '@/game/types';

interface Props {
  onStart: (config: {
    mode: GameMode;
    trackId: TrackId;
    player1Color: CarColorId;
    player2Color: CarColorId;
    aiSkill: number;
  }) => void;
}

export function Menu({ onStart }: Props) {
  const [mode, setMode] = useState<GameMode>('vs-computer');
  const [trackId, setTrackId] = useState<TrackId>(TRACKS[0].id as TrackId);
  const [p1, setP1] = useState<CarColorId>('red');
  const [p2, setP2] = useState<CarColorId>('blue');
  const [aiSkill, setAiSkill] = useState(0.7);
  const [showCarSelect, setShowCarSelect] = useState(false);

  const track = TRACKS.find((t) => t.id === trackId)!;

  return (
    <div className="min-h-screen w-full overflow-y-auto bg-gradient-to-br from-slate-950 via-slate-900 to-slate-800 text-white">
      {/* hero header */}
      <div className="relative overflow-hidden">
        <div className="absolute inset-0 opacity-20" style={{ background: 'radial-gradient(circle at 30% 20%, #38bdf8 0%, transparent 50%), radial-gradient(circle at 70% 60%, #f97316 0%, transparent 50%)' }} />
        <div className="relative max-w-6xl mx-auto px-6 pt-12 pb-8">
          <div className="flex items-center gap-3 mb-2">
            <Globe className="w-8 h-8 text-sky-400" />
            <span className="text-sm uppercase tracking-[0.3em] text-sky-400">Global Tour</span>
          </div>
          <h1 className="text-5xl sm:text-6xl font-black tracking-tight">
            Global Racing Tour
          </h1>
          <p className="mt-3 text-slate-300 text-lg max-w-xl">
            Race across five iconic world circuits with realistic drift physics, tire marks, and live camera action.
          </p>
        </div>
      </div>

      <div className="max-w-6xl mx-auto px-6 pb-16 grid lg:grid-cols-2 gap-8">
        {/* left: mode + track */}
        <div className="space-y-6">
          <section>
            <h2 className="text-xs uppercase tracking-widest text-slate-400 mb-3">Game Mode</h2>
            <div className="grid grid-cols-3 gap-3">
              <ModeCard active={mode === 'single'} onClick={() => setMode('single')} icon={<User className="w-5 h-5" />} label="Solo" sub="Time Trial" />
              <ModeCard active={mode === 'vs-computer'} onClick={() => setMode('vs-computer')} icon={<Cpu className="w-5 h-5" />} label="vs CPU" sub="Race the AI" />
              <ModeCard active={mode === 'vs-player'} onClick={() => setMode('vs-player')} icon={<Users className="w-5 h-5" />} label="2 Player" sub="Same keyboard" />
            </div>
          </section>

          {mode === 'vs-computer' && (
            <section className="rounded-2xl bg-white/5 border border-white/10 p-4">
              <div className="flex items-center gap-2 mb-3">
                <Gauge className="w-4 h-4 text-sky-400" />
                <span className="text-sm font-semibold">AI Difficulty</span>
                <span className="ml-auto text-sm text-sky-300 font-mono">{Math.round(aiSkill * 100)}%</span>
              </div>
              <input
                type="range" min={0.3} max={1} step={0.05} value={aiSkill}
                onChange={(e) => setAiSkill(parseFloat(e.target.value))}
                className="w-full accent-sky-400"
              />
              <div className="flex justify-between text-[10px] text-slate-400 mt-1">
                <span>Casual</span><span>Pro</span>
              </div>
            </section>
          )}

          <section>
            <h2 className="text-xs uppercase tracking-widest text-slate-400 mb-3">Select Circuit</h2>
            <div className="space-y-2">
              {TRACKS.map((t) => (
                <button
                  key={t.id}
                  onClick={() => setTrackId(t.id as TrackId)}
                  className={`w-full text-left rounded-xl p-4 border transition group ${
                    trackId === t.id
                      ? 'bg-white/10 border-sky-400 ring-1 ring-sky-400'
                      : 'bg-white/5 border-white/10 hover:bg-white/[0.08] hover:border-white/20'
                  }`}
                >
                  <div className="flex items-center gap-3">
                    <div className="w-10 h-10 rounded-lg flex items-center justify-center text-xs font-bold" style={{ background: t.accent + '33', color: t.accent }}>
                      {t.flag}
                    </div>
                    <div className="flex-1">
                      <div className="font-semibold">{t.name}</div>
                      <div className="text-xs text-slate-400 flex items-center gap-1">
                        <MapPin className="w-3 h-3" /> {t.city}, {t.country}
                      </div>
                    </div>
                    <div className="flex gap-0.5">
                      {[1, 2, 3].map((i) => (
                        <span key={i} className={`w-1.5 h-4 rounded-sm ${i <= t.difficulty ? '' : 'bg-white/10'}`} style={i <= t.difficulty ? { background: t.accent } : {}} />
                      ))}
                    </div>
                    {trackId === t.id && <ChevronRight className="w-4 h-4 text-sky-400" />}
                  </div>
                  <p className="text-xs text-slate-400 mt-2">{t.description}</p>
                </button>
              ))}
            </div>
          </section>
        </div>

        {/* right: car select + start */}
        <div className="space-y-6">
          <section className="rounded-2xl bg-white/5 border border-white/10 p-5">
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xs uppercase tracking-widest text-slate-400">Drivers</h2>
              <button onClick={() => setShowCarSelect((s) => !s)} className="text-xs text-sky-400 hover:text-sky-300">
                {showCarSelect ? 'Done' : 'Change colors'}
              </button>
            </div>

            {!showCarSelect ? (
              <div className="space-y-3">
                <DriverRow label={mode === 'single' ? 'You' : 'Player 1'} car={carOption(p1)} />
                {mode !== 'single' && (
                  <DriverRow label={mode === 'vs-computer' ? 'Computer' : 'Player 2'} car={carOption(p2)} />
                )}
                <p className="text-xs text-slate-500 pt-1">
                  {mode === 'vs-player'
                    ? 'P1: WASD + Space (handbrake). P2: Arrow keys + Right Shift.'
                    : 'Drive with WASD/Arrows. Space = handbrake for drifts.'}
                </p>
              </div>
            ) : (
              <div className="space-y-4">
                <ColorPicker label="Player 1" value={p1} onChange={setP1} />
                {mode !== 'single' && <ColorPicker label={mode === 'vs-computer' ? 'Computer' : 'Player 2'} value={p2} onChange={setP2} />}
              </div>
            )}
          </section>

          <section className="rounded-2xl bg-white/5 border border-white/10 p-5">
            <div className="flex items-center gap-2 mb-3">
              <Trophy className="w-4 h-4 text-amber-400" />
              <h2 className="text-xs uppercase tracking-widest text-slate-400">Race Preview</h2>
            </div>
            <div className="rounded-xl p-4" style={{ background: `linear-gradient(135deg, ${track.theme.skyTop}, ${track.theme.skyBottom})` }}>
              <div className="text-white/90 font-semibold">{track.name}</div>
              <div className="flex items-center gap-4 mt-2 text-white/80 text-sm">
                <span className="flex items-center gap-1"><MapPin className="w-3.5 h-3.5" /> {track.country}</span>
                <span className="flex items-center gap-1"><Zap className="w-3.5 h-3.5" /> {track.laps} laps</span>
              </div>
            </div>
            <button
              onClick={() => onStart({ mode, trackId, player1Color: p1, player2Color: p2, aiSkill })}
              className="mt-5 w-full py-4 rounded-xl bg-gradient-to-r from-sky-500 to-cyan-400 text-slate-950 font-black text-lg tracking-wide hover:from-sky-400 hover:to-cyan-300 transition shadow-lg shadow-sky-500/30 active:scale-[0.99]"
            >
              START RACE
            </button>
          </section>
        </div>
      </div>
    </div>
  );
}

function ModeCard({ active, onClick, icon, label, sub }: { active: boolean; onClick: () => void; icon: React.ReactNode; label: string; sub: string }) {
  return (
    <button
      onClick={onClick}
      className={`rounded-xl p-4 border text-left transition ${
        active ? 'bg-sky-500/15 border-sky-400 ring-1 ring-sky-400' : 'bg-white/5 border-white/10 hover:bg-white/10'
      }`}
    >
      <div className={active ? 'text-sky-400' : 'text-slate-300'}>{icon}</div>
      <div className="font-semibold mt-2">{label}</div>
      <div className="text-[11px] text-slate-400">{sub}</div>
    </button>
  );
}

function DriverRow({ label, car }: { label: string; car: import('@/game/types').CarOption }) {
  return (
    <div className="flex items-center gap-3 rounded-lg bg-black/20 p-3">
      <div className="w-9 h-9 rounded-lg flex items-center justify-center" style={{ background: car.body }}>
        <div className="w-5 h-3 rounded-sm" style={{ background: car.accent }} />
      </div>
      <div>
        <div className="text-xs text-slate-400">{label}</div>
        <div className="font-semibold">{car.name}</div>
      </div>
    </div>
  );
}

function ColorPicker({ label, value, onChange }: { label: string; value: CarColorId; onChange: (c: CarColorId) => void }) {
  return (
    <div>
      <div className="text-sm text-slate-300 mb-2">{label}</div>
      <div className="flex flex-wrap gap-2">
        {CAR_OPTIONS.map((c) => (
          <button
            key={c.id}
            onClick={() => onChange(c.id)}
            className={`w-12 h-12 rounded-xl flex items-center justify-center border-2 transition ${value === c.id ? 'border-white scale-110' : 'border-transparent hover:border-white/40'}`}
            style={{ background: c.body }}
            title={c.name}
          >
            <div className="w-6 h-3 rounded-sm" style={{ background: c.accent }} />
          </button>
        ))}
      </div>
    </div>
  );
}
