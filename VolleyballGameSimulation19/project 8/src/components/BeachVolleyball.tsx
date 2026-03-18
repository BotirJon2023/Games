import { useEffect, useRef, useState, useCallback } from 'react';
import { GameState, PlayerMode, Difficulty } from '../types/game';
import {
  PLAYER_JUMP_FORCE,
  PLAYER_MOVE_SPEED,
  NET_X,
  GROUND_Y,
  updateBallPhysics,
  updatePlayerPhysics,
  checkBallPlayerCollision,
  reflectBallFromPlayer,
} from '../utils/physics';
import { updateAI } from '../utils/ai';
import GameCanvas from './GameCanvas';
import GameMenu from './GameMenu';
import GameOver from './GameOver';
import { Pause, Play } from 'lucide-react';

export default function BeachVolleyball() {
  const [gameState, setGameState] = useState<GameState>(createInitialState());
  const gameLoopRef = useRef<number>();
  const keysPressed = useRef<Set<string>>(new Set());

  const resetBall = useCallback((serving: 1 | 2) => {
    return {
      position: {
        x: serving === 1 ? NET_X - 100 : NET_X + 100,
        y: GROUND_Y - 100,
      },
      velocity: { x: 0, y: 0 },
      radius: 15,
      spinning: 0,
    };
  }, []);

  const handleStartGame = useCallback((playerMode: PlayerMode, difficulty: Difficulty = 'medium') => {
    setGameState({
      ...createInitialState(),
      mode: 'playing',
      playerMode,
      difficulty,
    });
  }, []);

  const handleRestart = useCallback(() => {
    setGameState(prev => ({
      ...createInitialState(),
      mode: 'playing',
      playerMode: prev.playerMode,
      difficulty: prev.difficulty,
    }));
  }, []);

  const handleBackToMenu = useCallback(() => {
    setGameState(createInitialState());
  }, []);

  const togglePause = useCallback(() => {
    setGameState(prev => ({
      ...prev,
      mode: prev.mode === 'paused' ? 'playing' : 'paused',
    }));
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      keysPressed.current.add(e.key.toLowerCase());

      if (e.key === 'Escape') {
        togglePause();
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      keysPressed.current.delete(e.key.toLowerCase());
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [togglePause]);

  useEffect(() => {
    if (gameState.mode !== 'playing') {
      if (gameLoopRef.current) {
        cancelAnimationFrame(gameLoopRef.current);
      }
      return;
    }

    const gameLoop = () => {
      setGameState(prev => {
        const newState = { ...prev };

        handlePlayer1Controls(newState, keysPressed.current);

        if (newState.playerMode === '1player') {
          updateAI(newState.player2, newState.ball, newState.difficulty);
        } else {
          handlePlayer2Controls(newState, keysPressed.current);
        }

        updatePlayerPhysics(newState.player1);
        updatePlayerPhysics(newState.player2);

        if (checkBallPlayerCollision(newState.ball, newState.player1)) {
          reflectBallFromPlayer(newState.ball, newState.player1);
        }

        if (checkBallPlayerCollision(newState.ball, newState.player2)) {
          reflectBallFromPlayer(newState.ball, newState.player2);
        }

        updateBallPhysics(newState.ball);

        if (newState.ball.position.y + newState.ball.radius >= GROUND_Y) {
          if (Math.abs(newState.ball.velocity.y) < 1) {
            if (newState.ball.position.x < NET_X) {
              newState.player2.score++;
              newState.serving = 2;
            } else {
              newState.player1.score++;
              newState.serving = 1;
            }
            newState.ball = resetBall(newState.serving);

            if (newState.player1.score >= newState.maxScore ||
                newState.player2.score >= newState.maxScore) {
              newState.mode = 'gameOver';
            }
          }
        }

        return newState;
      });

      gameLoopRef.current = requestAnimationFrame(gameLoop);
    };

    gameLoopRef.current = requestAnimationFrame(gameLoop);

    return () => {
      if (gameLoopRef.current) {
        cancelAnimationFrame(gameLoopRef.current);
      }
    };
  }, [gameState.mode, resetBall]);

  if (gameState.mode === 'menu') {
    return <GameMenu onStartGame={handleStartGame} />;
  }

  return (
    <div className="min-h-screen bg-gradient-to-b from-sky-400 to-amber-200 flex flex-col items-center justify-center p-4">
      <div className="mb-4 flex items-center gap-4">
        <h1 className="text-4xl font-bold text-white drop-shadow-lg">Beach Volleyball</h1>
        <button
          onClick={togglePause}
          className="bg-white hover:bg-gray-100 text-gray-800 font-bold p-3 rounded-full shadow-lg transition-all"
          title={gameState.mode === 'paused' ? 'Resume' : 'Pause'}
        >
          {gameState.mode === 'paused' ? <Play className="w-6 h-6" /> : <Pause className="w-6 h-6" />}
        </button>
      </div>

      <div className="relative">
        <GameCanvas gameState={gameState} />

        {gameState.mode === 'paused' && (
          <div className="absolute inset-0 bg-black bg-opacity-50 flex items-center justify-center rounded-lg">
            <div className="bg-white rounded-2xl p-8 text-center shadow-2xl">
              <h2 className="text-3xl font-bold text-gray-800 mb-4">Paused</h2>
              <p className="text-gray-600 mb-4">Press ESC to resume</p>
              <button
                onClick={handleBackToMenu}
                className="bg-gray-600 hover:bg-gray-700 text-white font-bold py-3 px-6 rounded-xl transition-all"
              >
                Main Menu
              </button>
            </div>
          </div>
        )}

        {gameState.mode === 'gameOver' && (
          <GameOver
            gameState={gameState}
            onRestart={handleRestart}
            onMenu={handleBackToMenu}
          />
        )}
      </div>

      <div className="mt-6 bg-white rounded-xl p-4 shadow-lg">
        <div className="grid grid-cols-2 gap-8 text-center">
          <div>
            <p className="text-sm text-gray-600 mb-1">Player 1</p>
            <p className="text-xs text-gray-500">W (Jump) | A/D (Move)</p>
          </div>
          <div>
            <p className="text-sm text-gray-600 mb-1">
              {gameState.playerMode === '1player' ? 'Computer' : 'Player 2'}
            </p>
            {gameState.playerMode === '2player' && (
              <p className="text-xs text-gray-500">↑ (Jump) | ←/→ (Move)</p>
            )}
            {gameState.playerMode === '1player' && (
              <p className="text-xs text-gray-500 capitalize">{gameState.difficulty} Mode</p>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function createInitialState(): GameState {
  return {
    mode: 'menu',
    playerMode: '1player',
    difficulty: 'medium',
    player1: {
      position: { x: 100, y: GROUND_Y - 60 },
      velocity: { x: 0, y: 0 },
      width: 40,
      height: 60,
      score: 0,
      isJumping: false,
      direction: 'idle',
    },
    player2: {
      position: { x: 650, y: GROUND_Y - 60 },
      velocity: { x: 0, y: 0 },
      width: 40,
      height: 60,
      score: 0,
      isJumping: false,
      direction: 'idle',
    },
    ball: {
      position: { x: 300, y: GROUND_Y - 100 },
      velocity: { x: 0, y: 0 },
      radius: 15,
      spinning: 0,
    },
    serving: 1,
    maxScore: 11,
  };
}

function handlePlayer1Controls(state: GameState, keys: Set<string>) {
  state.player1.velocity.x = 0;
  state.player1.direction = 'idle';

  if (keys.has('a') || keys.has('arrowleft')) {
    state.player1.velocity.x = -PLAYER_MOVE_SPEED;
    state.player1.direction = 'left';
  }
  if (keys.has('d') || keys.has('arrowright')) {
    state.player1.velocity.x = PLAYER_MOVE_SPEED;
    state.player1.direction = 'right';
  }

  if ((keys.has('w') || keys.has('arrowup')) && !state.player1.isJumping) {
    state.player1.velocity.y = PLAYER_JUMP_FORCE;
    state.player1.isJumping = true;
  }

  state.player1.position.x += state.player1.velocity.x;

  if (state.player1.position.x < 0) {
    state.player1.position.x = 0;
  }
  if (state.player1.position.x + state.player1.width > NET_X - 10) {
    state.player1.position.x = NET_X - 10 - state.player1.width;
  }
}

function handlePlayer2Controls(state: GameState, keys: Set<string>) {
  state.player2.velocity.x = 0;
  state.player2.direction = 'idle';

  if (keys.has('arrowleft')) {
    state.player2.velocity.x = -PLAYER_MOVE_SPEED;
    state.player2.direction = 'left';
  }
  if (keys.has('arrowright')) {
    state.player2.velocity.x = PLAYER_MOVE_SPEED;
    state.player2.direction = 'right';
  }

  if (keys.has('arrowup') && !state.player2.isJumping) {
    state.player2.velocity.y = PLAYER_JUMP_FORCE;
    state.player2.isJumping = true;
  }

  state.player2.position.x += state.player2.velocity.x;

  if (state.player2.position.x < NET_X + 10) {
    state.player2.position.x = NET_X + 10;
  }
  if (state.player2.position.x + state.player2.width > 800) {
    state.player2.position.x = 800 - state.player2.width;
  }
}
