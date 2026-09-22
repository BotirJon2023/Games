import { useEffect, useRef, useState, useCallback } from 'react';
import { Home } from 'lucide-react';
import { GameEngine3D } from '@/game3d/engine3d';
import type { GameMode3D, RaceResult3D, GameUpdateState3D } from '@/game3d/types';

interface Game3DViewProps {
  mode: GameMode3D;
  onExit: () => void;
  onFinish: (results: RaceResult3D[]) => void;
}

export function Game3DView({ mode, onExit, onFinish }: Game3DViewProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const engineRef = useRef<GameEngine3D | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [updateState, setUpdateState] = useState<GameUpdateState3D | null>(null);
  const [showExitConfirm, setShowExitConfirm] = useState(false);

  const handleUpdate = useCallback((state: GameUpdateState3D) => {
    setUpdateState(state);
  }, []);

  const handleResults = useCallback(
    (results: RaceResult3D[]) => {
      onFinish(results);
    },
    [onFinish]
  );

  useEffect(() => {
    const canvas = canvasRef.current;
    const container = containerRef.current;
    if (!canvas || !container) return;

    const resize = () => {
      const rect = container.getBoundingClientRect();
      canvas.width = rect.width;
      canvas.height = rect.height;
      if (engineRef.current) {
        engineRef.current.renderer.resize(rect.width, rect.height);
      }
    };

    resize();
    window.addEventListener('resize', resize);

    const engine = new GameEngine3D(canvas, mode, handleResults, handleUpdate);
    engineRef.current = engine;
    engine.start();

    return () => {
      engine.stop();
      window.removeEventListener('resize', resize);
    };
  }, [mode, handleResults, handleUpdate]);

  return (
    <div className="fixed inset-0 bg-black overflow-hidden" ref={containerRef}>
      <canvas ref={canvasRef} className="block w-full h-full" />

      {/* Top bar */}
      <div className="absolute top-0 left-0 right-0 z-10 pointer-events-none">
        <div className="flex justify-between items-start p-3">
          {/* Timer */}
          <div className="bg-black/50 backdrop-blur-sm rounded-xl px-4 py-2 border border-white/10">
            <span className="text-white font-bold text-lg font-mono">
              {updateState ? formatTime(updateState.elapsed) : '0:00.00'}
            </span>
          </div>

          {/* Race positions */}
          {updateState && (
            <div className="flex gap-2">
              {updateState.bikes
                .sort((a, b) => b.progress - a.progress)
                .map((bike, i) => (
                  <div
                    key={bike.id}
                    className="bg-black/50 backdrop-blur-sm rounded-lg px-3 py-1.5 border border-white/10 flex items-center gap-2"
                  >
                    <span className="text-white/60 text-xs font-bold">{i + 1}</span>
                    <div className="w-3 h-3 rounded-full" style={{ backgroundColor: bike.color }} />
                    <span className="text-white text-xs font-semibold">{bike.name}</span>
                    <span className="text-white/40 text-xs">{Math.floor(bike.progress * 100)}%</span>
                  </div>
                ))}
            </div>
          )}

          {/* Exit button */}
          <button
            onClick={() => setShowExitConfirm(true)}
            className="pointer-events-auto bg-black/50 backdrop-blur-sm rounded-xl px-3 py-2 border border-white/10 text-white/70 hover:text-white hover:bg-black/70 transition-colors"
          >
            <Home className="w-5 h-5" />
          </button>
        </div>
      </div>

      {/* Controls hint (bottom center, fades after race starts) */}
      {updateState && !updateState.racing && (
        <div className="absolute bottom-20 left-1/2 -translate-x-1/2 z-10 pointer-events-none">
          <div className="bg-black/50 backdrop-blur-sm rounded-xl px-5 py-3 border border-white/10 text-center">
            <p className="text-white text-sm font-semibold">
              {mode === 'vs-cpu' ? 'W/↑ to accelerate · A,D to steer · Space to boost' : 'P1: WASD+Space · P2: IJKL+O'}
            </p>
          </div>
        </div>
      )}

      {/* Exit confirmation */}
      {showExitConfirm && (
        <div className="absolute inset-0 z-20 flex items-center justify-center bg-black/60 backdrop-blur-sm">
          <div className="bg-gray-900 rounded-2xl p-6 max-w-sm w-full mx-4 border border-white/10">
            <h3 className="text-white text-xl font-bold mb-2">Quit Race?</h3>
            <p className="text-gray-400 text-sm mb-6">Your current progress will be lost.</p>
            <div className="flex gap-3">
              <button
                onClick={() => setShowExitConfirm(false)}
                className="flex-1 py-2.5 rounded-xl bg-white/10 text-white font-semibold hover:bg-white/20 transition-colors"
              >
                Keep Racing
              </button>
              <button
                onClick={onExit}
                className="flex-1 py-2.5 rounded-xl bg-red-500/80 text-white font-semibold hover:bg-red-500 transition-colors"
              >
                Quit
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

interface Results3DScreenProps {
  results: RaceResult3D[];
  onPlayAgain: () => void;
  onMenu: () => void;
}

export function Results3DScreen({ results, onPlayAgain, onMenu }: Results3DScreenProps) {
  return (
    <div className="min-h-screen w-full relative overflow-hidden bg-gradient-to-b from-indigo-950 via-purple-900 to-orange-950 flex items-center justify-center px-4">
      {/* Confetti */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {Array.from({ length: 50 }).map((_, i) => (
          <div
            key={i}
            className="absolute w-2 h-3 rounded-sm"
            style={{
              backgroundColor: ['#e63946', '#3a86ff', '#2a9d8f', '#ffbe0b', '#ff006e'][i % 5],
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              animation: `confettiFall3D ${2 + Math.random() * 3}s linear infinite`,
              animationDelay: `${Math.random() * 3}s`,
            }}
          />
        ))}
      </div>

      {/* Sunset glow */}
      <div className="absolute top-1/3 left-1/2 -translate-x-1/2 w-96 h-96 rounded-full bg-orange-500/20 blur-3xl pointer-events-none" />

      <div className="relative z-10 max-w-lg w-full">
        {/* Trophy */}
        <div className="text-center mb-8 animate-[bounceIn3D_0.6s_ease-out]">
          <div className="inline-block relative">
            <div className="text-7xl font-black text-transparent bg-clip-text bg-gradient-to-b from-yellow-300 to-orange-500 drop-shadow-lg">
              FINISH
            </div>
            <div className="absolute -inset-4 bg-yellow-400/10 rounded-full blur-2xl" />
          </div>
          <h1 className="text-2xl font-bold text-white/80 mt-2 tracking-widest">RACE RESULTS</h1>
        </div>

        {/* Results list */}
        <div className="space-y-3 mb-8">
          {results.map((result, i) => (
            <div
              key={result.name}
              className={`flex items-center gap-4 p-4 rounded-2xl border transition-all animate-[slideIn3D_0.5s_ease-out_${i * 0.15}s_both] ${
                i === 0
                  ? 'bg-gradient-to-r from-yellow-500/25 to-orange-600/15 border-yellow-500/40'
                  : 'bg-white/5 border-white/10'
              }`}
            >
              <div
                className="flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center font-black text-xl"
                style={{
                  backgroundColor: i === 0 ? '#fbbf24' : i === 1 ? '#94a3b8' : i === 2 ? '#b45309' : '#374151',
                  color: i < 3 ? '#000' : '#fff',
                }}
              >
                {i + 1}
              </div>

              <div className="flex-shrink-0">
                <div className="w-4 h-4 rounded-full mb-1" style={{ backgroundColor: result.color }} />
                <span className="text-white font-bold">{result.name}</span>
              </div>

              <div className="flex-1 text-right">
                <div className="text-white font-mono font-bold text-lg">{formatTime(result.time)}</div>
                <div className="text-orange-400 text-xs">Top: {result.topSpeed} km/h</div>
              </div>
            </div>
          ))}
        </div>

        {/* Buttons */}
        <div className="flex gap-3">
          <button
            onClick={onPlayAgain}
            className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl bg-gradient-to-r from-orange-500 to-red-500 text-white font-bold hover:from-orange-400 hover:to-red-400 transition-all active:scale-95"
          >
            Race Again
          </button>
          <button
            onClick={onMenu}
            className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl bg-white/10 text-white font-bold hover:bg-white/20 transition-colors active:scale-95"
          >
            <Home className="w-5 h-5" />
            Main Menu
          </button>
        </div>
      </div>

      <style>{`
        @keyframes confettiFall3D {
          0% { transform: translateY(-100vh) rotate(0deg); opacity: 1; }
          100% { transform: translateY(100vh) rotate(720deg); opacity: 0.5; }
        }
        @keyframes bounceIn3D {
          0% { opacity: 0; transform: scale(0.3); }
          50% { opacity: 1; transform: scale(1.1); }
          100% { transform: scale(1); }
        }
        @keyframes slideIn3D {
          from { opacity: 0; transform: translateX(-30px); }
          to { opacity: 1; transform: translateX(0); }
        }
      `}</style>
    </div>
  );
}

function formatTime(seconds: number): string {
  const min = Math.floor(seconds / 60);
  const sec = Math.floor(seconds % 60);
  const cs = Math.floor((seconds % 1) * 100);
  return `${min}:${sec.toString().padStart(2, '0')}.${cs.toString().padStart(2, '0')}`;
}
