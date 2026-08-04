import { useEffect, useRef, useCallback } from 'react';
import type { RaceState, CameraState, CarInput } from '@/game/types';
import { buildTrack } from '@/game/trackData';
import { makeCar, updateRace, closestSample } from '@/game/physics';
import { drawRace } from '@/game/renderer';
import { carOption, CAR_OPTIONS, type CarColorId } from '@/game/types';
import { TRACKS, type TrackId } from '@/game/trackData';

export interface RaceConfig {
  mode: 'single' | 'vs-player' | 'vs-computer';
  trackId: TrackId;
  player1Color: CarColorId;
  player2Color: CarColorId;
  aiSkill: number;
}

interface Props {
  config: RaceConfig;
  onFinish: (state: RaceState) => void;
  onExit: () => void;
}

const KEYS_P1 = {
  up: ['KeyW', 'ArrowUp'],
  down: ['KeyS', 'ArrowDown'],
  left: ['KeyA', 'ArrowLeft'],
  right: ['KeyD', 'ArrowRight'],
  brake: ['Space'],
};
const KEYS_P2 = {
  up: ['ArrowUp'],
  down: ['ArrowDown'],
  left: ['ArrowLeft'],
  right: ['ArrowRight'],
  brake: ['ShiftRight'],
};

export function GameCanvas({ config, onFinish, onExit }: Props) {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const stateRef = useRef<RaceState | null>(null);
  const camRef = useRef<CameraState>({ x: 0, y: 0, zoom: 1, shake: 0 });
  const keysRef = useRef<Set<string>>(new Set());
  const rafRef = useRef<number>(0);
  const lastRef = useRef<number>(0);
  const finishFiredRef = useRef(false);

  const initRace = useCallback(() => {
    const baseTrack = TRACKS.find((t) => t.id === config.trackId) ?? TRACKS[0];
    const track = buildTrack(baseTrack);
    const cars = [];
    const p1Opt = carOption(config.player1Color);
    cars.push(makeCar('p1', p1Opt.name, p1Opt.id, p1Opt.body, p1Opt.accent, false, 0, -1, track, 'Player 1'));
    if (config.mode === 'vs-player') {
      const p2Opt = carOption(config.player2Color);
      cars.push(makeCar('p2', p2Opt.name, p2Opt.id, p2Opt.body, p2Opt.accent, false, 0, 1, track, 'Player 2'));
    } else if (config.mode === 'vs-computer') {
      const aiOpt = carOption(config.player2Color);
      cars.push(makeCar('ai', 'CPU', aiOpt.id, aiOpt.body, aiOpt.accent, true, config.aiSkill, 1, track, 'Computer'));
    }
    const state: RaceState = {
      track,
      cars,
      tireMarks: [],
      particles: [],
      started: false,
      countdown: 3.2,
      raceTime: 0,
      finishedCars: 0,
      totalLaps: track.laps,
      confettiTimer: 0,
    };
    stateRef.current = state;
    const p = cars[0].pos;
    camRef.current = { x: p.x, y: p.y, zoom: 1.05, shake: 0 };
    finishFiredRef.current = false;
  }, [config]);

  useEffect(() => {
    initRace();
  }, [initRace]);

  const readInput = useCallback((): { p1: CarInput; p2: CarInput } => {
    const keys = keysRef.current;
    const has = (arr: string[]) => arr.some((k) => keys.has(k));
    const p1: CarInput = {
      throttle: has(KEYS_P1.up) ? 1 : 0,
      brake: has(KEYS_P1.down) ? 1 : 0,
      steering: (has(KEYS_P1.left) ? -1 : 0) + (has(KEYS_P1.right) ? 1 : 0),
      handbrake: has(KEYS_P1.brake),
    };
    const p2: CarInput = {
      throttle: has(KEYS_P2.up) ? 1 : 0,
      brake: has(KEYS_P2.down) ? 1 : 0,
      steering: (has(KEYS_P2.left) ? -1 : 0) + (has(KEYS_P2.right) ? 1 : 0),
      handbrake: has(KEYS_P2.brake),
    };
    return { p1, p2 };
  }, []);

  const loop = useCallback((now: number) => {
    const canvas = canvasRef.current;
    const state = stateRef.current;
    if (!canvas || !state) {
      rafRef.current = requestAnimationFrame(loop);
      return;
    }
    const ctx = canvas.getContext('2d')!;
    const dt = Math.min(0.05, (now - lastRef.current) / 1000 || 0.016);
    lastRef.current = now;

    // Apply inputs
    const inputs = readInput();
    state.cars[0].input = inputs.p1;
    if (state.cars.length > 1 && !state.cars[1].isAI) {
      state.cars[1].input = inputs.p2;
    }

    updateRace(state, dt);

    // camera follows leader
    const lead = state.cars.slice().sort((a, b) => a.place - b.place)[0] ?? state.cars[0];
    const cam = camRef.current;
    const lerp = 1 - Math.pow(0.001, dt);
    cam.x += (lead.pos.x - cam.x) * lerp;
    cam.y += (lead.pos.y - cam.y) * lerp;
    cam.shake *= 0.9;
    if (state.started && !state.cars[0].finished && Math.hypot(state.cars[0].vel.x, state.cars[0].vel.y) > 300 && Math.abs(state.cars[0].driftAmount) > 120) {
      cam.shake = Math.min(3, cam.shake + dt * 4);
    }

    drawRace(ctx, state, cam, canvas.width, canvas.height, now / 1000);

    // finish detection
    if (!finishFiredRef.current && state.finishedCars >= state.cars.length) {
      finishFiredRef.current = true;
      setTimeout(() => onFinish(state), 2500);
    }

    rafRef.current = requestAnimationFrame(loop);
  }, [readInput, onFinish]);

  useEffect(() => {
    const onDown = (e: KeyboardEvent) => {
      keysRef.current.add(e.code);
      if (['Space', 'ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight'].includes(e.code)) e.preventDefault();
    };
    const onUp = (e: KeyboardEvent) => keysRef.current.delete(e.code);
    window.addEventListener('keydown', onDown);
    window.addEventListener('keyup', onUp);
    return () => {
      window.removeEventListener('keydown', onDown);
      window.removeEventListener('keyup', onUp);
    };
  }, []);

  useEffect(() => {
    lastRef.current = performance.now();
    rafRef.current = requestAnimationFrame(loop);
    return () => cancelAnimationFrame(rafRef.current);
  }, [loop]);

  // keep canvas sized
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const resize = () => {
      canvas.width = canvas.clientWidth * window.devicePixelRatio;
      canvas.height = canvas.clientHeight * window.devicePixelRatio;
    };
    resize();
    window.addEventListener('resize', resize);
    return () => window.removeEventListener('resize', resize);
  }, []);

  // HUD state via polling (light)
  const hudRef = useRef<HTMLDivElement>(null);
  useEffect(() => {
    let id = 0;
    const tick = () => {
      const state = stateRef.current;
      const hud = hudRef.current;
      if (state && hud) {
        renderHud(hud, state, config);
      }
      id = requestAnimationFrame(tick);
    };
    id = requestAnimationFrame(tick);
    return () => cancelAnimationFrame(id);
  }, [config]);

  return (
    <div className="relative w-full h-full">
      <canvas ref={canvasRef} className="w-full h-full block" />
      <div ref={hudRef} className="pointer-events-none absolute inset-0" />
      <button
        onClick={onExit}
        className="absolute top-4 right-4 px-3 py-1.5 rounded-lg bg-black/50 text-white text-sm font-medium hover:bg-black/70 transition pointer-events-auto"
      >
        Exit Race
      </button>
    </div>
  );
}

function renderHud(hud: HTMLDivElement, state: RaceState, config: RaceConfig) {
  const fmt = (ms: number) => {
    if (!ms) return '--:--';
    const s = Math.floor(ms / 1000);
    const m = Math.floor(s / 60);
    return `${m}:${(s % 60).toString().padStart(2, '0')}`;
  };
  const car = state.cars[0];
  const lapsLeft = Math.max(0, state.totalLaps - car.lap);
  const lapDisplay = Math.min(car.lap + 1, state.totalLaps);

  const cards = state.cars.map((c) => {
    const isP1 = c.id === 'p1';
    const isP2 = c.id === 'p2';
    const label = isP1 ? 'P1' : isP2 ? 'P2' : 'CPU';
    const speed = Math.round(Math.hypot(c.vel.x, c.vel.y) / 4);
    return `
      <div class="rounded-xl bg-black/55 backdrop-blur px-3 py-2 text-white" style="border-left:4px solid ${c.body}">
        <div class="text-[10px] uppercase tracking-wider opacity-70">${label} · P${c.place}</div>
        <div class="text-xl font-bold leading-none mt-0.5">${speed}<span class="text-xs opacity-60 ml-1">km/h</span></div>
        <div class="text-[10px] mt-1 opacity-80">Lap ${Math.min(c.lap + 1, state.totalLaps)}/${state.totalLaps}</div>
      </div>`;
  }).join('');

  const countdown = !state.started && state.countdown > 0
    ? `<div class="absolute inset-0 flex items-center justify-center"><div class="text-8xl font-black text-white drop-shadow-2xl" style="text-shadow:0 4px 24px rgba(0,0,0,0.6)">${Math.ceil(state.countdown - 0.2) <= 0 ? 'GO!' : Math.ceil(state.countdown - 0.2)}</div></div>`
    : '';

  hud.innerHTML = `
    <div class="absolute top-4 left-4 flex gap-2">${cards}</div>
    <div class="absolute top-4 left-1/2 -translate-x-1/2 rounded-xl bg-black/55 backdrop-blur px-4 py-2 text-white text-center">
      <div class="text-[10px] uppercase tracking-widest opacity-70">${state.track.flag} ${state.track.city}</div>
      <div class="text-lg font-bold leading-tight">Lap ${lapDisplay}/${state.totalLaps}</div>
    </div>
    ${countdown}
  `;
}
