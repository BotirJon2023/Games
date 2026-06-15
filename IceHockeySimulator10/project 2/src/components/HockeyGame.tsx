import { useEffect, useRef, useCallback } from 'react';
import { CANVAS_W, CANVAS_H, TEAM_NAMES, TEAM_COLORS } from '../game/constants';
import { createGameState, gameTick, switchPlayer } from '../game/engine';
import { renderFrame, renderPeriodEnd } from '../game/renderer';
import type { PlayerMode, GameState } from '../game/types';

interface Props {
  playerMode: PlayerMode;
  onGameOver: (scores: [number, number]) => void;
  onQuit: () => void;
}

export default function HockeyGame({ playerMode, onGameOver, onQuit }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef<GameState>(createGameState(playerMode));
  const keysRef = useRef<Set<string>>(new Set());
  const rafRef = useRef<number>(0);
  const lastTimeRef = useRef<number>(0);
  const gameOverFiredRef = useRef(false);

  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    keysRef.current.add(e.code);

    // Tab: switch team 0 player
    if (e.code === 'Tab') {
      e.preventDefault();
      switchPlayer(stateRef.current, 0);
    }
    // Shift: switch team 1 player (2-player mode)
    if (e.code === 'ShiftRight' && stateRef.current.playerMode === 'two_player') {
      switchPlayer(stateRef.current, 1);
    }
    // Prevent arrow key scrolling
    if (['ArrowUp','ArrowDown','ArrowLeft','ArrowRight'].includes(e.code)) {
      e.preventDefault();
    }
  }, []);

  const handleKeyUp = useCallback((e: KeyboardEvent) => {
    keysRef.current.delete(e.code);
    // Also remove letter versions for cross-browser
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
    const ctx = canvas.getContext('2d')!;

    const loop = (ts: number) => {
      const dt = Math.min((ts - lastTimeRef.current) / 1000, 0.05);
      lastTimeRef.current = ts;

      const state = stateRef.current;

      // Add key letters to the key set (handle both code and key)
      const keys = keysRef.current;
      gameTick(state, keys, dt * 60);

      if (state.mode === 'period_end') {
        renderFrame(ctx, state);
        renderPeriodEnd(ctx, state);
      } else {
        renderFrame(ctx, state);
      }

      if (state.mode === 'game_over' && !gameOverFiredRef.current) {
        gameOverFiredRef.current = true;
        setTimeout(() => onGameOver(state.scores), 600);
        return;
      }

      rafRef.current = requestAnimationFrame(loop);
    };

    rafRef.current = requestAnimationFrame((ts) => {
      lastTimeRef.current = ts;
      rafRef.current = requestAnimationFrame(loop);
    });

    return () => cancelAnimationFrame(rafRef.current);
  }, [onGameOver]);

  return (
    <div className="flex flex-col items-center min-h-screen bg-gray-950 select-none">
      {/* Top bar */}
      <div className="flex items-center justify-between w-full px-6 py-3 bg-gray-900/80 border-b border-white/10">
        <div className="flex items-center gap-3">
          <div className="w-2 h-2 rounded-full bg-green-400 animate-pulse" />
          <span className="text-white/80 text-sm font-mono tracking-widest uppercase">
            {playerMode === 'two_player' ? '2 Players' : 'vs Computer'}
          </span>
        </div>
        <h1 className="text-white font-bold text-lg tracking-widest uppercase">Ice Hockey</h1>
        <button
          onClick={onQuit}
          className="text-white/50 hover:text-white/90 text-sm font-mono transition-colors px-3 py-1 rounded border border-white/10 hover:border-white/30"
        >
          QUIT
        </button>
      </div>

      {/* Canvas */}
      <div className="flex-1 flex items-center justify-center p-4">
        <div className="relative rounded-xl overflow-hidden shadow-2xl border border-white/10"
          style={{ boxShadow: '0 0 60px rgba(30,100,200,0.3), 0 0 120px rgba(0,0,0,0.6)' }}>
          <canvas
            ref={canvasRef}
            width={CANVAS_W}
            height={CANVAS_H}
            style={{ display: 'block', maxWidth: '100%', height: 'auto' }}
            tabIndex={0}
          />
        </div>
      </div>

      {/* Team legend */}
      <div className="flex gap-8 pb-4">
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded-full" style={{ background: TEAM_COLORS[0] }} />
          <span className="text-white/70 text-xs font-mono">{TEAM_NAMES[0]} (WASD + F)</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="w-4 h-4 rounded-full" style={{ background: TEAM_COLORS[1] }} />
          <span className="text-white/70 text-xs font-mono">
            {playerMode === 'two_player' ? `${TEAM_NAMES[1]} (Arrows + L)` : `${TEAM_NAMES[1]} (CPU)`}
          </span>
        </div>
      </div>
    </div>
  );
}
