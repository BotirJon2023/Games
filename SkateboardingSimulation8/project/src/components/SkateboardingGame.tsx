import { useEffect, useRef, useState } from 'react';
import { PhysicsEngine, SkateboardState } from '../utils/physics';
import { GameRenderer } from '../utils/renderer';

export function SkateboardingGame() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [score, setScore] = useState(0);
  const [combo, setCombo] = useState(0);
  const [fps, setFps] = useState(60);
  const [gameRunning, setGameRunning] = useState(true);

  const gameStateRef = useRef<SkateboardState>({
    position: { x: 100, y: 380 },
    velocity: { x: 0, y: 0 },
    rotation: 0,
    angularVelocity: 0,
    isAirborne: false,
    isGrinding: false,
    trickState: {
      currentTrick: 'none',
      progress: 0,
      maxProgress: 100,
    },
  });

  const keysRef = useRef<Set<string>>(new Set());
  const physicsRef = useRef(new PhysicsEngine());
  const rendererRef = useRef<GameRenderer | null>(null);
  const lastTimeRef = useRef(Date.now());
  const frameCountRef = useRef(0);
  const fpsTimeRef = useRef(Date.now());

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    rendererRef.current = new GameRenderer(canvas);
    const physics = physicsRef.current;

    const handleKeyDown = (e: KeyboardEvent) => {
      keysRef.current.add(e.key);

      if (e.key === 'ArrowUp') {
        e.preventDefault();
        gameStateRef.current = physics.handleOllie(gameStateRef.current);
        setCombo((prev) => prev + 1);
        setScore((prev) => prev + 50 * Math.max(1, combo));
      } else if (e.key === 'q' || e.key === 'Q') {
        e.preventDefault();
        gameStateRef.current = physics.handleKickflip(gameStateRef.current);
        setCombo((prev) => prev + 1);
        setScore((prev) => prev + 150 * Math.max(1, combo));
      } else if (e.key === 'w' || e.key === 'W') {
        e.preventDefault();
        gameStateRef.current = physics.handleHeelflip(gameStateRef.current);
        setCombo((prev) => prev + 1);
        setScore((prev) => prev + 150 * Math.max(1, combo));
      } else if (e.key === 'e' || e.key === 'E') {
        e.preventDefault();
        gameStateRef.current = physics.handleBigSpin(gameStateRef.current);
        setCombo((prev) => prev + 1);
        setScore((prev) => prev + 300 * Math.max(1, combo));
      } else if (e.key === ' ') {
        e.preventDefault();
        if (gameStateRef.current.isAirborne) {
          gameStateRef.current = {
            ...gameStateRef.current,
            rotation: Math.round(gameStateRef.current.rotation / (Math.PI * 2)) * (Math.PI * 2),
          };
        }
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      keysRef.current.delete(e.key);

      if (e.key === 'ArrowDown') {
        gameStateRef.current = {
          ...gameStateRef.current,
          velocity: { ...gameStateRef.current.velocity, x: 0 },
        };
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    let animationId: number;

    const gameLoop = () => {
      if (!gameRunning) {
        animationId = requestAnimationFrame(gameLoop);
        return;
      }

      frameCountRef.current++;
      const now = Date.now();
      const deltaTime = now - lastTimeRef.current;
      lastTimeRef.current = now;

      if (now - fpsTimeRef.current >= 1000) {
        setFps(frameCountRef.current);
        frameCountRef.current = 0;
        fpsTimeRef.current = now;
      }

      gameStateRef.current = physics.update(gameStateRef.current, keysRef.current);

      if (!gameStateRef.current.isAirborne && combo > 0) {
        setCombo(0);
      }

      checkCollisions(gameStateRef.current);

      if (rendererRef.current) {
        rendererRef.current.render(gameStateRef.current, score, combo, fps);
      }

      animationId = requestAnimationFrame(gameLoop);
    };

    animationId = requestAnimationFrame(gameLoop);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
      cancelAnimationFrame(animationId);
    };
  }, [gameRunning, score, combo, fps]);

  const checkCollisions = (state: SkateboardState) => {
    const obstacles = [
      { x: 150, y: 320, w: 40, h: 60 },
      { x: 350, y: 330, w: 50, h: 50 },
      { x: 550, y: 310, w: 60, h: 70 },
      { x: 700, y: 325, w: 45, h: 55 },
    ];

    obstacles.forEach((obs) => {
      if (
        state.position.x > obs.x - 20 &&
        state.position.x < obs.x + obs.w &&
        state.position.y > obs.y - 30 &&
        state.position.y < obs.y + obs.h
      ) {
        if (state.isAirborne && combo > 2) {
          setScore((prev) => prev + 200 * combo);
        }
      }
    });
  };

  const handleTogglePause = () => {
    setGameRunning(!gameRunning);
  };

  const handleReset = () => {
    gameStateRef.current = {
      position: { x: 100, y: 380 },
      velocity: { x: 0, y: 0 },
      rotation: 0,
      angularVelocity: 0,
      isAirborne: false,
      isGrinding: false,
      trickState: {
        currentTrick: 'none',
        progress: 0,
        maxProgress: 100,
      },
    };
    setScore(0);
    setCombo(0);
  };

  return (
    <div className="w-full h-screen bg-gray-900 flex flex-col items-center justify-center p-4">
      <div className="w-full max-w-4xl">
        <h1 className="text-4xl font-bold text-white mb-4 text-center">Skateboarding Simulation</h1>

        <div className="flex justify-center mb-4">
          <canvas
            ref={canvasRef}
            width={800}
            height={500}
            className="border-4 border-white rounded-lg shadow-2xl"
          />
        </div>

        <div className="flex justify-center gap-4 mb-6">
          <button
            onClick={handleTogglePause}
            className="px-6 py-2 bg-blue-600 text-white font-bold rounded-lg hover:bg-blue-700 transition"
          >
            {gameRunning ? 'Pause' : 'Resume'}
          </button>
          <button
            onClick={handleReset}
            className="px-6 py-2 bg-red-600 text-white font-bold rounded-lg hover:bg-red-700 transition"
          >
            Reset
          </button>
        </div>

        <div className="grid grid-cols-2 gap-4 text-white text-center">
          <div className="bg-gray-800 p-4 rounded-lg">
            <p className="text-lg font-bold">Current Score</p>
            <p className="text-3xl font-bold text-yellow-400">{score}</p>
          </div>
          <div className="bg-gray-800 p-4 rounded-lg">
            <p className="text-lg font-bold">Combo Multiplier</p>
            <p className="text-3xl font-bold text-orange-400">x{combo}</p>
          </div>
        </div>

        <div className="mt-6 bg-gray-800 p-6 rounded-lg text-white">
          <h2 className="text-xl font-bold mb-4">How to Play</h2>
          <div className="grid grid-cols-2 gap-4 text-sm">
            <div>
              <p className="font-bold mb-2">Movement & Tricks:</p>
              <ul className="space-y-1">
                <li>↑ Arrow: Ollie (50 pts)</li>
                <li>← → Arrows: Move Left/Right</li>
                <li>Q: Kickflip (150 pts)</li>
                <li>W: Heelflip (150 pts)</li>
                <li>E: Big Spin (300 pts)</li>
                <li>Space: Land Trick</li>
              </ul>
            </div>
            <div>
              <p className="font-bold mb-2">Tips:</p>
              <ul className="space-y-1">
                <li>Build combos by performing tricks in the air</li>
                <li>Land tricks successfully to multiply your score</li>
                <li>Avoid staying on ground for long periods</li>
                <li>Perform tricks while airborne to increase combo</li>
                <li>Each successful trick adds to your multiplier</li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
