import { useEffect, useRef, useState, useCallback } from 'react';
import { Trophy, RotateCcw, Home, Medal } from 'lucide-react';
import { GameEngine, type GameUpdateState } from '../game/engine';
import type { GameMode, RaceResult } from '../game/types';

interface GameViewProps {
  mode: GameMode;
  onExit: () => void;
  onFinish: (results: RaceResult[]) => void;
}

export function GameView({ mode, onExit, onFinish }: GameViewProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const engineRef = useRef<GameEngine | null>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const [updateState, setUpdateState] = useState<GameUpdateState | null>(null);
  const [showExitConfirm, setShowExitConfirm] = useState(false);

  const handleUpdate = useCallback((state: GameUpdateState) => {
    setUpdateState(state);
  }, []);

  const handleResults = useCallback(
    (results: RaceResult[]) => {
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

    const engine = new GameEngine(canvas, mode, handleResults, handleUpdate);
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

      {/* Top bar with race info */}
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

function formatTime(seconds: number): string {
  const min = Math.floor(seconds / 60);
  const sec = Math.floor(seconds % 60);
  const cs = Math.floor((seconds % 1) * 100);
  return `${min}:${sec.toString().padStart(2, '0')}.${cs.toString().padStart(2, '0')}`;
}

interface ResultsScreenProps {
  results: RaceResult[];
  onPlayAgain: () => void;
  onMenu: () => void;
}

export function ResultsScreen({ results, onPlayAgain, onMenu }: ResultsScreenProps) {
  const winner = results[0];

  return (
    <div className="min-h-screen w-full relative overflow-hidden bg-gradient-to-b from-gray-900 via-gray-800 to-emerald-950 flex items-center justify-center px-4">
      {/* Confetti background */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden">
        {Array.from({ length: 40 }).map((_, i) => (
          <div
            key={i}
            className="absolute w-2 h-3 rounded-sm"
            style={{
              backgroundColor: ['#e63946', '#3a86ff', '#2a9d8f', '#ffbe0b', '#ff006e'][i % 5],
              left: `${Math.random() * 100}%`,
              top: `${Math.random() * 100}%`,
              animation: `confettiFall ${2 + Math.random() * 3}s linear infinite`,
              animationDelay: `${Math.random() * 3}s`,
            }}
          />
        ))}
      </div>

      <div className="relative z-10 max-w-lg w-full">
        {/* Winner trophy */}
        <div className="text-center mb-8 animate-[bounceIn_0.6s_ease-out]">
          <div className="inline-block relative">
            <Trophy className="w-20 h-20 text-yellow-400 drop-shadow-lg" />
            <div className="absolute -inset-4 bg-yellow-400/20 rounded-full blur-2xl" />
          </div>
          <h1 className="text-4xl font-black text-white mt-4 tracking-tight">RACE COMPLETE</h1>
        </div>

        {/* Results list */}
        <div className="space-y-3 mb-8">
          {results.map((result, i) => (
            <div
              key={result.name}
              className={`flex items-center gap-4 p-4 rounded-2xl border transition-all animate-[slideIn_0.5s_ease-out_${i * 0.15}s_both] ${
                i === 0
                  ? 'bg-gradient-to-r from-yellow-500/20 to-yellow-600/10 border-yellow-500/40'
                  : 'bg-white/5 border-white/10'
              }`}
            >
              {/* Position */}
              <div className="flex-shrink-0 w-12 h-12 rounded-xl flex items-center justify-center font-black text-xl"
                style={{
                  backgroundColor: i === 0 ? '#fbbf24' : i === 1 ? '#94a3b8' : i === 2 ? '#b45309' : '#374151',
                  color: i < 3 ? '#000' : '#fff',
                }}
              >
                {i + 1}
              </div>

              {/* Name and color */}
              <div className="flex-shrink-0">
                <div className="w-4 h-4 rounded-full mb-1" style={{ backgroundColor: result.color }} />
                <span className="text-white font-bold">{result.name}</span>
              </div>

              {/* Time */}
              <div className="flex-1 text-right">
                <div className="text-white font-mono font-bold text-lg">{formatTime(result.time)}</div>
                {result.tricks > 0 && (
                  <div className="text-amber-400 text-xs flex items-center justify-end gap-1">
                    <Medal className="w-3 h-3" />
                    {result.tricks} trick pts
                  </div>
                )}
              </div>
            </div>
          ))}
        </div>

        {/* Action buttons */}
        <div className="flex gap-3">
          <button
            onClick={onPlayAgain}
            className="flex-1 flex items-center justify-center gap-2 py-3.5 rounded-2xl bg-emerald-500 text-white font-bold hover:bg-emerald-400 transition-colors active:scale-95"
          >
            <RotateCcw className="w-5 h-5" />
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
        @keyframes confettiFall {
          0% { transform: translateY(-100vh) rotate(0deg); opacity: 1; }
          100% { transform: translateY(100vh) rotate(720deg); opacity: 0.5; }
        }
        @keyframes bounceIn {
          0% { opacity: 0; transform: scale(0.3); }
          50% { opacity: 1; transform: scale(1.1); }
          100% { transform: scale(1); }
        }
        @keyframes slideIn {
          from { opacity: 0; transform: translateX(-30px); }
          to { opacity: 1; transform: translateX(0); }
        }
      `}</style>
    </div>
  );
}
