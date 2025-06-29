import React, { useRef, useEffect, useState, useCallback } from 'react';
import { GameState, Skater, Track } from '../types/game';
import { PhysicsEngine } from '../utils/physics';
import { GameLogic } from '../utils/gameLogic';
import { Renderer } from '../utils/renderer';
import { Play, Pause, RotateCcw, Trophy } from 'lucide-react';

const SpeedSkatingGame: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const gameLoopRef = useRef<number>();
  const lastTimeRef = useRef<number>(0);
  const [gameInitialized, setGameInitialized] = useState(false);
  
  // Game systems
  const [gameState, setGameState] = useState<GameState>({
    isRunning: false,
    isPaused: false,
    gameTime: 0,
    maxLaps: 3,
    skaters: [],
    powerUps: [],
    winner: null
  });

  const [inputs, setInputs] = useState({
    accelerate: false,
    brake: false,
    left: false,
    right: false
  });

  // Game systems refs
  const physicsEngineRef = useRef<PhysicsEngine>();
  const gameLogicRef = useRef<GameLogic>();
  const rendererRef = useRef<Renderer>();
  const trackRef = useRef<Track>();

  // Initialize game
  useEffect(() => {
    if (!canvasRef.current || gameInitialized) return;

    const canvas = canvasRef.current;
    
    // Initialize track
    const track: Track = {
      centerX: 400,
      centerY: 300,
      innerRadius: 150,
      outerRadius: 250,
      width: 100
    };
    trackRef.current = track;

    // Initialize game systems
    physicsEngineRef.current = new PhysicsEngine(track);
    gameLogicRef.current = new GameLogic(track);
    rendererRef.current = new Renderer(canvas, track);

    // Create skaters
    const skaters: Skater[] = [
      gameLogicRef.current.createSkater('player', 'You', 0, true),
      gameLogicRef.current.createSkater('ai1', 'Lightning', 1, false, 0.9),
      gameLogicRef.current.createSkater('ai2', 'Frost', 2, false, 0.8),
      gameLogicRef.current.createSkater('ai3', 'Blaze', 3, false, 0.85),
      gameLogicRef.current.createSkater('ai4', 'Storm', 4, false, 0.75),
      gameLogicRef.current.createSkater('ai5', 'Dash', 5, false, 0.82)
    ];

    setGameState(prev => ({
      ...prev,
      skaters
    }));

    setGameInitialized(true);
  }, [gameInitialized]);

  // Game loop
  const gameLoop = useCallback((currentTime: number) => {
    if (!physicsEngineRef.current || !gameLogicRef.current || !rendererRef.current) return;

    const deltaTime = (currentTime - lastTimeRef.current) / 1000;
    lastTimeRef.current = currentTime;

    if (deltaTime > 0.1) return; // Skip frame if too much time has passed

    setGameState(prevState => {
      if (!prevState.isRunning || prevState.isPaused) return prevState;

      const newState = { ...prevState };
      
      // Update physics for all skaters
      newState.skaters.forEach(skater => {
        physicsEngineRef.current!.updateSkaterPhysics(skater, deltaTime, inputs);
      });

      // Update game logic
      gameLogicRef.current!.updateGameState(newState, deltaTime);

      return newState;
    });

    gameLoopRef.current = requestAnimationFrame(gameLoop);
  }, [inputs]);

  // Render loop
  useEffect(() => {
    if (!rendererRef.current || !gameState.skaters.length) return;

    rendererRef.current.render(gameState);
  }, [gameState]);

  // Start game loop
  useEffect(() => {
    if (gameState.isRunning && !gameState.isPaused) {
      lastTimeRef.current = performance.now();
      gameLoopRef.current = requestAnimationFrame(gameLoop);
    } else {
      if (gameLoopRef.current) {
        cancelAnimationFrame(gameLoopRef.current);
      }
    }

    return () => {
      if (gameLoopRef.current) {
        cancelAnimationFrame(gameLoopRef.current);
      }
    };
  }, [gameState.isRunning, gameState.isPaused, gameLoop]);

  // Keyboard controls
  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      switch (event.code) {
        case 'ArrowUp':
        case 'KeyW':
          setInputs(prev => ({ ...prev, accelerate: true }));
          break;
        case 'ArrowDown':
        case 'KeyS':
          setInputs(prev => ({ ...prev, brake: true }));
          break;
        case 'ArrowLeft':
        case 'KeyA':
          setInputs(prev => ({ ...prev, left: true }));
          break;
        case 'ArrowRight':
        case 'KeyD':
          setInputs(prev => ({ ...prev, right: true }));
          break;
        case 'Space':
          event.preventDefault();
          togglePause();
          break;
      }
    };

    const handleKeyUp = (event: KeyboardEvent) => {
      switch (event.code) {
        case 'ArrowUp':
        case 'KeyW':
          setInputs(prev => ({ ...prev, accelerate: false }));
          break;
        case 'ArrowDown':
        case 'KeyS':
          setInputs(prev => ({ ...prev, brake: false }));
          break;
        case 'ArrowLeft':
        case 'KeyA':
          setInputs(prev => ({ ...prev, left: false }));
          break;
        case 'ArrowRight':
        case 'KeyD':
          setInputs(prev => ({ ...prev, right: false }));
          break;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, []);

  // Handle window resize
  useEffect(() => {
    const handleResize = () => {
      if (rendererRef.current) {
        rendererRef.current.resize();
      }
    };

    window.addEventListener('resize', handleResize);
    return () => window.removeEventListener('resize', handleResize);
  }, []);

  const startGame = () => {
    setGameState(prev => ({ ...prev, isRunning: true, isPaused: false }));
  };

  const togglePause = () => {
    setGameState(prev => ({ ...prev, isPaused: !prev.isPaused }));
  };

  const resetGame = () => {
    if (gameLogicRef.current) {
      gameLogicRef.current.resetGame(gameState);
      setGameState(prev => ({ ...gameState, isRunning: false, isPaused: false }));
    }
  };

  const playerSkater = gameState.skaters.find(s => s.isPlayer);

  return (
    <div className="relative w-full h-screen bg-gray-900 overflow-hidden">
      <canvas
        ref={canvasRef}
        className="w-full h-full"
        style={{ display: 'block' }}
      />
      
      {/* Game Controls */}
      <div className="absolute top-4 left-1/2 transform -translate-x-1/2 flex gap-4 z-10">
        {!gameState.isRunning ? (
          <button
            onClick={startGame}
            className="flex items-center gap-2 px-6 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg font-semibold transition-colors"
          >
            <Play size={20} />
            Start Race
          </button>
        ) : (
          <button
            onClick={togglePause}
            className="flex items-center gap-2 px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-semibold transition-colors"
          >
            {gameState.isPaused ? <Play size={20} /> : <Pause size={20} />}
            {gameState.isPaused ? 'Resume' : 'Pause'}
          </button>
        )}
        
        <button
          onClick={resetGame}
          className="flex items-center gap-2 px-6 py-3 bg-gray-600 hover:bg-gray-700 text-white rounded-lg font-semibold transition-colors"
        >
          <RotateCcw size={20} />
          Reset
        </button>
      </div>

      {/* Controls Instructions */}
      <div className="absolute bottom-4 left-4 bg-black bg-opacity-70 text-white p-4 rounded-lg">
        <h3 className="font-semibold mb-2">Controls:</h3>
        <div className="text-sm space-y-1">
          <div>↑ / W - Accelerate</div>
          <div>↓ / S - Brake</div>
          <div>← / A - Turn Left</div>
          <div>→ / D - Turn Right</div>
          <div>Space - Pause</div>
        </div>
      </div>

      {/* Player Stats */}
      {playerSkater && (
        <div className="absolute bottom-4 right-4 bg-black bg-opacity-70 text-white p-4 rounded-lg">
          <h3 className="font-semibold mb-2 flex items-center gap-2">
            <Trophy size={16} />
            Your Stats
          </h3>
          <div className="text-sm space-y-1">
            <div>Lap: {playerSkater.lap} / {gameState.maxLaps}</div>
            <div>Speed: {Math.round(playerSkater.speed)} km/h</div>
            <div>Best Lap: {playerSkater.bestLapTime > 0 ? `${playerSkater.bestLapTime.toFixed(2)}s` : '--'}</div>
            <div>Stamina: {Math.round(playerSkater.stamina)}%</div>
            {playerSkater.powerUpActive && (
              <div className="text-yellow-400">
                Power-up: {playerSkater.powerUpType} ({playerSkater.powerUpDuration.toFixed(1)}s)
              </div>
            )}
          </div>
        </div>
      )}

      {/* Winner Modal */}
      {gameState.winner && (
        <div className="absolute inset-0 bg-black bg-opacity-80 flex items-center justify-center z-20">
          <div className="bg-white rounded-lg p-8 text-center max-w-md">
            <Trophy size={48} className="mx-auto mb-4 text-yellow-500" />
            <h2 className="text-2xl font-bold mb-2">Race Complete!</h2>
            <p className="text-lg mb-4">
              Winner: <span className="font-semibold" style={{ color: gameState.winner.color }}>
                {gameState.winner.name}
              </span>
            </p>
            <p className="text-gray-600 mb-6">
              Total Time: {(gameState.winner.totalTime / 1000).toFixed(2)}s
            </p>
            <button
              onClick={resetGame}
              className="px-6 py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-lg font-semibold transition-colors"
            >
              Race Again
            </button>
          </div>
        </div>
      )}

      {/* Loading Screen */}
      {!gameInitialized && (
        <div className="absolute inset-0 bg-gray-900 flex items-center justify-center">
          <div className="text-white text-center">
            <div className="text-xl font-semibold mb-2">Loading Speed Skating Game...</div>
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-white mx-auto"></div>
          </div>
        </div>
      )}
    </div>
  );
};

export default SpeedSkatingGame;