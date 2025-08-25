import React, { useState, useEffect, useCallback, useRef } from 'react';
import GameBoard from './GameBoard';
import ScoreBoard from './ScoreBoard';
import Menu from './Menu';
import { GameState, GameStats, Position, Velocity } from '../types/game';
import { playSound } from '../utils/audio';

const PADDLE_HEIGHT = 100;
const PADDLE_WIDTH = 12;
const BALL_SIZE = 12;
const INITIAL_BALL_SPEED = 4;
const SPEED_INCREASE = 0.3;
const WINNING_SCORE = 11;

interface GameProps {}

const Game: React.FC<GameProps> = () => {
  const [gameState, setGameState] = useState<GameState>('menu');
  const [stats, setStats] = useState<GameStats>({
    player1Score: 0,
    player2Score: 0,
    ballSpeed: INITIAL_BALL_SPEED,
    rallies: 0
  });

  const [ballPosition, setBallPosition] = useState<Position>({ x: 400, y: 300 });
  const [ballVelocity, setBallVelocity] = useState<Velocity>({ x: INITIAL_BALL_SPEED, y: 2 });
  const [paddle1Y, setPaddle1Y] = useState(250);
  const [paddle2Y, setPaddle2Y] = useState(250);

  const gameAreaRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<number>();
  const keysPressed = useRef<Set<string>>(new Set());
  
  const GAME_WIDTH = 800;
  const GAME_HEIGHT = 600;

  // Sound effects
  const playPaddleHit = () => playSound(220, 0.1, 'square');
  const playWallHit = () => playSound(180, 0.1, 'sawtooth');
  const playScore = () => playSound(440, 0.3, 'sine');
  const playGameOver = () => playSound(330, 0.5, 'triangle');

  // Reset ball to center with random direction
  const resetBall = useCallback(() => {
    const direction = Math.random() > 0.5 ? 1 : -1;
    const angle = (Math.random() - 0.5) * Math.PI / 3; // Random angle between -60 and 60 degrees
    
    setBallPosition({ x: GAME_WIDTH / 2, y: GAME_HEIGHT / 2 });
    setBallVelocity({
      x: Math.cos(angle) * INITIAL_BALL_SPEED * direction,
      y: Math.sin(angle) * INITIAL_BALL_SPEED
    });
    setStats(prev => ({ ...prev, ballSpeed: INITIAL_BALL_SPEED }));
  }, []);

  // Initialize game
  const startGame = useCallback(() => {
    setGameState('playing');
    setStats({
      player1Score: 0,
      player2Score: 0,
      ballSpeed: INITIAL_BALL_SPEED,
      rallies: 0
    });
    setPaddle1Y(GAME_HEIGHT / 2 - PADDLE_HEIGHT / 2);
    setPaddle2Y(GAME_HEIGHT / 2 - PADDLE_HEIGHT / 2);
    resetBall();
  }, [resetBall]);

  // Handle keyboard input
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      keysPressed.current.add(e.key.toLowerCase());
      
      if (e.key === 'Escape') {
        setGameState(prev => prev === 'playing' ? 'paused' : prev === 'paused' ? 'playing' : prev);
      }
      if (e.key === ' ' && gameState === 'menu') {
        startGame();
      }
      if (e.key === 'r' && gameState === 'gameOver') {
        setGameState('menu');
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
  }, [gameState, startGame]);

  // Paddle movement
  useEffect(() => {
    const movePaddles = () => {
      const PADDLE_SPEED = 6;
      
      setPaddle1Y(prev => {
        let newY = prev;
        if (keysPressed.current.has('w')) newY -= PADDLE_SPEED;
        if (keysPressed.current.has('s')) newY += PADDLE_SPEED;
        return Math.max(0, Math.min(GAME_HEIGHT - PADDLE_HEIGHT, newY));
      });

      setPaddle2Y(prev => {
        let newY = prev;
        if (keysPressed.current.has('arrowup')) newY -= PADDLE_SPEED;
        if (keysPressed.current.has('arrowdown')) newY += PADDLE_SPEED;
        return Math.max(0, Math.min(GAME_HEIGHT - PADDLE_HEIGHT, newY));
      });
    };

    const interval = setInterval(movePaddles, 16); // ~60fps
    return () => clearInterval(interval);
  }, []);

  // Ball physics and collision detection
  useEffect(() => {
    if (gameState !== 'playing') return;

    const updateBall = () => {
      setBallPosition(prevPos => {
        setBallVelocity(prevVel => {
          let newX = prevPos.x + prevVel.x;
          let newY = prevPos.y + prevVel.y;
          let newVelX = prevVel.x;
          let newVelY = prevVel.y;

          // Top and bottom wall collisions
          if (newY <= BALL_SIZE / 2 || newY >= GAME_HEIGHT - BALL_SIZE / 2) {
            newVelY = -newVelY;
            newY = Math.max(BALL_SIZE / 2, Math.min(GAME_HEIGHT - BALL_SIZE / 2, newY));
            playWallHit();
          }

          // Paddle collisions
          const paddle1Left = PADDLE_WIDTH;
          const paddle1Right = PADDLE_WIDTH + PADDLE_WIDTH;
          const paddle2Left = GAME_WIDTH - PADDLE_WIDTH * 2;
          const paddle2Right = GAME_WIDTH - PADDLE_WIDTH;

          // Player 1 paddle collision
          if (
            newX - BALL_SIZE / 2 <= paddle1Right &&
            newX + BALL_SIZE / 2 >= paddle1Left &&
            newY >= paddle1Y &&
            newY <= paddle1Y + PADDLE_HEIGHT &&
            newVelX < 0
          ) {
            const hitPosition = (newY - (paddle1Y + PADDLE_HEIGHT / 2)) / (PADDLE_HEIGHT / 2);
            newVelX = Math.abs(newVelX) * (1 + SPEED_INCREASE);
            newVelY = hitPosition * stats.ballSpeed * 0.8;
            newX = paddle1Right + BALL_SIZE / 2;
            playPaddleHit();
            setStats(prev => ({ 
              ...prev, 
              ballSpeed: prev.ballSpeed + SPEED_INCREASE,
              rallies: prev.rallies + 1
            }));
          }

          // Player 2 paddle collision
          if (
            newX + BALL_SIZE / 2 >= paddle2Left &&
            newX - BALL_SIZE / 2 <= paddle2Right &&
            newY >= paddle2Y &&
            newY <= paddle2Y + PADDLE_HEIGHT &&
            newVelX > 0
          ) {
            const hitPosition = (newY - (paddle2Y + PADDLE_HEIGHT / 2)) / (PADDLE_HEIGHT / 2);
            newVelX = -Math.abs(newVelX) * (1 + SPEED_INCREASE);
            newVelY = hitPosition * stats.ballSpeed * 0.8;
            newX = paddle2Left - BALL_SIZE / 2;
            playPaddleHit();
            setStats(prev => ({ 
              ...prev, 
              ballSpeed: prev.ballSpeed + SPEED_INCREASE,
              rallies: prev.rallies + 1
            }));
          }

          // Scoring
          if (newX < 0) {
            setStats(prev => {
              const newScore = prev.player2Score + 1;
              return { ...prev, player2Score: newScore };
            });
            playScore();
            setTimeout(resetBall, 1000);
            return prevVel;
          }

          if (newX > GAME_WIDTH) {
            setStats(prev => {
              const newScore = prev.player1Score + 1;
              return { ...prev, player1Score: newScore };
            });
            playScore();
            setTimeout(resetBall, 1000);
            return prevVel;
          }

          setBallPosition({ x: newX, y: newY });
          return { x: newVelX, y: newVelY };
        });

        return prevPos;
      });
    };

    animationRef.current = requestAnimationFrame(function animate() {
      updateBall();
      if (gameState === 'playing') {
        animationRef.current = requestAnimationFrame(animate);
      }
    });

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [gameState, resetBall, stats.ballSpeed, paddle1Y, paddle2Y]);

  // Check for game over
  useEffect(() => {
    if (stats.player1Score >= WINNING_SCORE || stats.player2Score >= WINNING_SCORE) {
      setGameState('gameOver');
      playGameOver();
    }
  }, [stats.player1Score, stats.player2Score]);

  return (
    <div className="min-h-screen flex flex-col items-center justify-center p-4">
      <div className="text-center mb-6">
        <h1 className="text-6xl font-bold bg-gradient-to-r from-green-400 via-blue-500 to-purple-600 bg-clip-text text-transparent mb-2">
          PONG ARENA
        </h1>
        <p className="text-gray-400 text-lg">
          Classic multiplayer ping-pong with modern effects
        </p>
      </div>

      <ScoreBoard 
        player1Score={stats.player1Score}
        player2Score={stats.player2Score}
        rallies={stats.rallies}
        ballSpeed={stats.ballSpeed}
      />

      <div 
        ref={gameAreaRef}
        className="relative bg-black border-2 border-green-500 rounded-lg shadow-2xl shadow-green-500/20 mb-6"
        style={{ width: GAME_WIDTH, height: GAME_HEIGHT }}
      >
        {gameState === 'menu' && <Menu onStartGame={startGame} />}
        
        {gameState === 'paused' && (
          <div className="absolute inset-0 bg-black/80 flex items-center justify-center z-50">
            <div className="text-center">
              <h2 className="text-4xl font-bold text-white mb-4">PAUSED</h2>
              <p className="text-gray-400">Press ESC to resume</p>
            </div>
          </div>
        )}

        {gameState === 'gameOver' && (
          <div className="absolute inset-0 bg-black/90 flex items-center justify-center z-50">
            <div className="text-center">
              <h2 className="text-5xl font-bold text-yellow-400 mb-4">GAME OVER</h2>
              <p className="text-2xl text-white mb-2">
                {stats.player1Score >= WINNING_SCORE ? 'Player 1 Wins!' : 'Player 2 Wins!'}
              </p>
              <p className="text-xl text-gray-400 mb-6">
                Final Score: {stats.player1Score} - {stats.player2Score}
              </p>
              <p className="text-gray-400">Press R to return to menu</p>
            </div>
          </div>
        )}

        <GameBoard
          ballPosition={ballPosition}
          paddle1Y={paddle1Y}
          paddle2Y={paddle2Y}
          gameWidth={GAME_WIDTH}
          gameHeight={GAME_HEIGHT}
          paddleHeight={PADDLE_HEIGHT}
          paddleWidth={PADDLE_WIDTH}
          ballSize={BALL_SIZE}
        />
      </div>

      <div className="text-center text-gray-400 space-y-2">
        <div className="flex flex-col sm:flex-row gap-4 sm:gap-8 justify-center">
          <div>
            <span className="font-semibold text-blue-400">Player 1:</span> W/S to move
          </div>
          <div>
            <span className="font-semibold text-red-400">Player 2:</span> ↑/↓ to move
          </div>
        </div>
        <div className="text-sm">
          <span className="font-semibold">Controls:</span> SPACE to start • ESC to pause • R to restart
        </div>
      </div>
    </div>
  );
};

export default Game;