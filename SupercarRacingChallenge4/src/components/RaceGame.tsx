import { useEffect, useRef, useState, useCallback } from 'react';
import { createInitialState, updateGame, getRaceResult, type GameState, type InputState } from '@/game/engine';
import { renderGame } from '@/game/renderer';
import { CARS, type GameMode, type RaceResult } from '@/game/types';

interface RaceGameProps {
  mode: GameMode;
  car1Index: number;
  car2Index: number;
  onFinish: (result: RaceResult) => void;
  onExit: () => void;
}

export function RaceGame({ mode, car1Index, car2Index, onFinish, onExit }: RaceGameProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef<GameState>(createInitialState(mode));
  const keysRef = useRef<Set<string>>(new Set());
  const rafRef = useRef<number>(0);
  const lastTimeRef = useRef<number>(0);
  const [finished, setFinished] = useState(false);

  // Apply car selection
  useEffect(() => {
    const state = stateRef.current;
    const c1 = CARS[car1Index];
    const c2 = CARS[car2Index];
    state.cars[0].color = c1.bodyColor;
    state.cars[0].glowColor = c1.glowColor;
    state.cars[0].accentColor = c1.accentColor;
    state.cars[0].name = c1.name;
    state.cars[1].color = c2.bodyColor;
    state.cars[1].glowColor = c2.glowColor;
    state.cars[1].accentColor = c2.accentColor;
    state.cars[1].name = c2.name;
  }, [car1Index, car2Index]);

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    keysRef.current.add(e.key);
    // Prevent scrolling with arrow keys
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', ' '].includes(e.key)) {
      e.preventDefault();
    }
  }, []);

  const handleKeyUp = useCallback((e: KeyboardEvent) => {
    keysRef.current.delete(e.key);
  }, []);

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [handleKeyDown, handleKeyUp]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const resize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    resize();
    window.addEventListener('resize', resize);

    const loop = (time: number) => {
      if (lastTimeRef.current === 0) lastTimeRef.current = time;
      const dt = Math.min(0.05, (time - lastTimeRef.current) / 1000);
      lastTimeRef.current = time;

      const state = stateRef.current;

      // Build inputs from pressed keys
      const inputs: InputState[] = [
        readInput(keysRef.current, 1),
        readInput(keysRef.current, 2),
      ];

      updateGame(state, dt, inputs);
      renderGame(ctx, state, canvas.width, canvas.height, time / 1000);

      if (state.finished && !finished) {
        setFinished(true);
        setTimeout(() => onFinish(getRaceResult(state)), 2500);
      }

      rafRef.current = requestAnimationFrame(loop);
    };

    rafRef.current = requestAnimationFrame(loop);

    return () => {
      cancelAnimationFrame(rafRef.current);
      window.removeEventListener('resize', resize);
      lastTimeRef.current = 0;
    };
  }, [finished, onFinish]);

  return (
    <div className="relative w-screen h-screen overflow-hidden bg-[#05050f]">
      <canvas ref={canvasRef} className="block w-full h-full" />

      {/* Exit button */}
      <button
        onClick={onExit}
        className="absolute top-4 left-1/2 -translate-x-1/2 z-20 px-4 py-1.5 rounded-lg border border-slate-600/50 bg-slate-900/60 text-slate-400 hover:text-white hover:border-slate-400 transition-all text-xs font-display tracking-widest"
      >
        EXIT RACE
      </button>

      {/* Boost hint */}
      <div className="absolute bottom-6 left-1/2 -translate-x-1/2 z-20 text-center">
        <p className="text-slate-500 text-xs tracking-widest uppercase">
          Switch lanes to dodge barriers · Hold <span className="neon-text-yellow font-bold">BOOST</span> for nitro
        </p>
      </div>
    </div>
  );
}

function readInput(keys: Set<string>, player: number): InputState {
  if (player === 1) {
    return {
      left: keys.has('a') || keys.has('A') || keys.has('ArrowLeft'),
      right: keys.has('d') || keys.has('D') || keys.has('ArrowRight'),
      boost: keys.has('w') || keys.has('W') || keys.has('ArrowUp'),
    };
  }
  return {
    left: keys.has('j') || keys.has('J'),
    right: keys.has('l') || keys.has('L'),
    boost: keys.has('i') || keys.has('I'),
  };
}
