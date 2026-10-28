import { useEffect, useRef, useCallback } from 'react';
import { Renderer } from './renderer';
import { buildTrack, type TrackData } from './track';
import { Hoverboard } from './hoverboard';
import { InputManager } from './input';
import { computeAIInput } from './ai';
import {
  CANVAS_WIDTH,
  CANVAS_HEIGHT,
  COLORS,
  PHYSICS,
  TOTAL_LAPS,
  START_POSITION_OFFSET,
} from './constants';
import type { GameMode, GameResult } from './types';
import { angleOf, sub } from './math';

interface GameCanvasProps {
  mode: GameMode;
  onFinish: (result: GameResult) => void;
  onExit: () => void;
}

export function GameCanvas({ mode, onFinish, onExit }: GameCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const onFinishRef = useRef(onFinish);
  onFinishRef.current = onFinish;

  const startGame = useCallback(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const track = buildTrack();
    const renderer = new Renderer(ctx);
    const input = new InputManager();

    // Create players
    const startAngle = angleOf(sub(track.points[1].pos, track.points[0].pos));

    const p1 = new Hoverboard(
      { x: START_POSITION_OFFSET.x - 25, y: START_POSITION_OFFSET.y + 20 },
      startAngle,
      COLORS.p1.primary,
      COLORS.p1.glow,
      COLORS.p1.trail,
      COLORS.p1.name,
      false,
    );

    const p2Color = mode === 'vs-computer' ? COLORS.ai : COLORS.p2;
    const p2 = new Hoverboard(
      { x: START_POSITION_OFFSET.x - 25, y: START_POSITION_OFFSET.y - 20 },
      startAngle,
      p2Color.primary,
      p2Color.glow,
      p2Color.trail,
      p2Color.name,
      mode === 'vs-computer',
    );

    const players = [p1, p2];

    // Countdown
    let countdown = 3;
    let countdownTimer = 0;
    let countdownScale = 1;
    let countdownAlpha = 1;
    let gameStarted = false;
    let finished = false;

    let lastTime = performance.now();
    let elapsed = 0;
    let animationId = 0;

    // Update boost pad cooldowns
    const updateBoostPads = (dt: number) => {
      for (const pad of track.boostPads) {
        if (!pad.active) {
          pad.cooldownTimer -= dt;
          if (pad.cooldownTimer <= 0) {
            pad.active = true;
          }
        }
      }
      for (const obs of track.obstacles) {
        if (obs.hitTimer > 0) {
          obs.hitTimer -= dt;
        }
        obs.rotation += obs.rotationSpeed;
      }
    };

    const gameLoop = (now: number) => {
      const dt = Math.min((now - lastTime) / 1000, 0.05);
      lastTime = now;

      if (gameStarted && !finished) {
        elapsed += dt;
      }

      // Countdown logic
      if (!gameStarted) {
        countdownTimer += dt;
        if (countdownTimer >= 1) {
          countdownTimer = 0;
          countdown--;
          countdownScale = 1.5;
          countdownAlpha = 1;
          if (countdown < 0) {
            gameStarted = true;
          }
        }
        countdownScale = Math.max(1, countdownScale - dt * 1.5);
        countdownAlpha = Math.max(0, countdownAlpha - dt * 0.5);
      }

      // Update players
      if (gameStarted) {
        const p1Input = input.getPlayer1Input();
        p1.update(p1Input, track, dt, elapsed, TOTAL_LAPS);

        if (mode === 'vs-computer') {
          const p2Input = computeAIInput(p2, track, elapsed);
          p2.update(p2Input, track, dt, elapsed, TOTAL_LAPS);
        } else {
          const p2Input = input.getPlayer2Input();
          p2.update(p2Input, track, dt, elapsed, TOTAL_LAPS);
        }

        updateBoostPads(dt);

        // Check finish
        if (!finished && (p1.finished || p2.finished)) {
          finished = true;
          const winner = p1.finished ? p1 : p2;
          setTimeout(() => {
            onFinishRef.current({
              winnerName: winner.name,
              winnerColor: winner.color,
              finishTime: winner.finishTime,
              mode,
              p1Laps: p1.laps,
              p2Laps: p2.laps,
            });
          }, 2000);
        }
      }

      // Render
      const cdValue = gameStarted ? -1 : countdown;
      renderer.drawAll(track, players, elapsed, cdValue, countdownScale, countdownAlpha, TOTAL_LAPS, mode);

      animationId = requestAnimationFrame(gameLoop);
    };

    animationId = requestAnimationFrame(gameLoop);

    return () => {
      cancelAnimationFrame(animationId);
      input.destroy();
    };
  }, [mode]);

  useEffect(() => {
    const cleanup = startGame();
    return cleanup;
  }, [startGame]);

  return (
    <div className="flex flex-col items-center gap-4">
      <div className="w-full max-w-[1100px] flex justify-between items-center px-2">
        <button
          onClick={onExit}
          className="px-4 py-2 rounded-lg bg-white/10 hover:bg-white/20 text-white text-sm font-medium transition-colors border border-white/20"
        >
          Exit Race
        </button>
        <div className="text-white/60 text-sm">
          {mode === 'vs-computer' ? (
            <span>P1: Arrow Keys + Space to boost</span>
          ) : (
            <span className="flex gap-6">
              <span style={{ color: COLORS.p1.primary }}>P1: Arrows + Space</span>
              <span style={{ color: COLORS.p2.primary }}>P2: WASD + Shift</span>
            </span>
          )}
        </div>
        <div className="w-24" />
      </div>
      <canvas
        ref={canvasRef}
        width={CANVAS_WIDTH}
        height={CANVAS_HEIGHT}
        className="rounded-xl shadow-2xl border border-white/10 max-w-full"
        style={{ imageRendering: 'auto' }}
      />
    </div>
  );
}
