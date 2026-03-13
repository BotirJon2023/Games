import { useState, useEffect, useRef } from 'react';
import { Trophy, RotateCcw } from 'lucide-react';

interface Position {
  x: number;
  y: number;
}

interface Velocity {
  x: number;
  y: number;
}

const VolleyballGame = () => {
  const canvasRef = useRef<HTMLDivElement>(null);
  const [ballPosition, setBallPosition] = useState<Position>({ x: 400, y: 200 });
  const [ballVelocity, setBallVelocity] = useState<Velocity>({ x: 3, y: 0 });
  const [player1Y, setPlayer1Y] = useState(300);
  const [player2Y, setPlayer2Y] = useState(300);
  const [score1, setScore1] = useState(0);
  const [score2, setScore2] = useState(0);
  const [gameActive, setGameActive] = useState(true);
  const [winner, setWinner] = useState<string | null>(null);

  const GRAVITY = 0.3;
  const GROUND_Y = 450;
  const NET_X = 400;
  const PLAYER_SPEED = 15;
  const BOUNCE_DAMPING = 0.7;
  const HIT_POWER = 8;

  const keysPressed = useRef<Set<string>>(new Set());

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      keysPressed.current.add(e.key.toLowerCase());
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
  }, []);

  useEffect(() => {
    if (!gameActive) return;

    const gameLoop = setInterval(() => {
      if (keysPressed.current.has('w') && player1Y > 250) {
        setPlayer1Y(prev => prev - PLAYER_SPEED);
      }
      if (keysPressed.current.has('s') && player1Y < GROUND_Y - 50) {
        setPlayer1Y(prev => prev + PLAYER_SPEED);
      }
      if (keysPressed.current.has('arrowup') && player2Y > 250) {
        setPlayer2Y(prev => prev - PLAYER_SPEED);
      }
      if (keysPressed.current.has('arrowdown') && player2Y < GROUND_Y - 50) {
        setPlayer2Y(prev => prev + PLAYER_SPEED);
      }

      setBallPosition(prev => {
        let newX = prev.x;
        let newY = prev.y;

        setBallVelocity(vel => {
          let newVelX = vel.x;
          let newVelY = vel.y + GRAVITY;

          newX += newVelX;
          newY += newVelY;

          if (newY >= GROUND_Y - 20) {
            newY = GROUND_Y - 20;
            newVelY = -newVelY * BOUNCE_DAMPING;
            newVelX *= 0.95;

            if (Math.abs(newVelY) < 1) {
              if (newX < NET_X) {
                setScore2(s => s + 1);
                resetBall(false);
              } else {
                setScore1(s => s + 1);
                resetBall(true);
              }
            }
          }

          const player1Distance = Math.sqrt(
            Math.pow(newX - 100, 2) + Math.pow(newY - player1Y, 2)
          );
          if (player1Distance < 60 && newX > 50) {
            const angle = Math.atan2(newY - player1Y, newX - 100);
            newVelX = Math.cos(angle) * HIT_POWER;
            newVelY = Math.sin(angle) * HIT_POWER - 3;
          }

          const player2Distance = Math.sqrt(
            Math.pow(newX - 700, 2) + Math.pow(newY - player2Y, 2)
          );
          if (player2Distance < 60 && newX < 750) {
            const angle = Math.atan2(newY - player2Y, newX - 700);
            newVelX = Math.cos(angle) * HIT_POWER;
            newVelY = Math.sin(angle) * HIT_POWER - 3;
          }

          if (newX <= 20 || newX >= 780) {
            newVelX = -newVelX * 0.8;
            newX = newX <= 20 ? 20 : 780;
          }

          return { x: newVelX, y: newVelY };
        });

        return { x: newX, y: newY };
      });
    }, 1000 / 60);

    return () => clearInterval(gameLoop);
  }, [gameActive, player1Y, player2Y]);

  useEffect(() => {
    if (score1 >= 5) {
      setWinner('Player 1');
      setGameActive(false);
    } else if (score2 >= 5) {
      setWinner('Player 2');
      setGameActive(false);
    }
  }, [score1, score2]);

  const resetBall = (leftSide: boolean) => {
    setBallPosition({ x: leftSide ? 200 : 600, y: 200 });
    setBallVelocity({ x: leftSide ? 3 : -3, y: 0 });
  };

  const resetGame = () => {
    setScore1(0);
    setScore2(0);
    setWinner(null);
    setGameActive(true);
    resetBall(true);
  };

  return (
    <div className="flex flex-col items-center justify-center min-h-screen p-8">
      <h1 className="text-5xl font-bold text-white mb-4 drop-shadow-lg">
        Cartoon Volleyball
      </h1>

      <div className="bg-white rounded-lg p-6 mb-4 shadow-2xl">
        <div className="flex gap-8 text-2xl font-bold">
          <div className="text-blue-600">Player 1: {score1}</div>
          <div className="text-red-600">Player 2: {score2}</div>
        </div>
      </div>

      <div
        ref={canvasRef}
        className="relative bg-gradient-to-b from-sky-200 to-amber-100 rounded-lg shadow-2xl overflow-hidden"
        style={{ width: '800px', height: '500px' }}
      >
        <div
          className="absolute bottom-0 w-full bg-gradient-to-b from-green-400 to-green-600"
          style={{ height: '50px' }}
        />

        <div
          className="absolute bg-gradient-to-b from-yellow-600 to-yellow-800"
          style={{
            left: `${NET_X - 5}px`,
            bottom: '50px',
            width: '10px',
            height: '150px',
          }}
        />
        <div
          className="absolute bg-white"
          style={{
            left: `${NET_X - 50}px`,
            bottom: '180px',
            width: '100px',
            height: '40px',
            background: 'repeating-linear-gradient(0deg, white 0px, white 8px, transparent 8px, transparent 16px), repeating-linear-gradient(90deg, white 0px, white 8px, transparent 8px, transparent 16px)',
          }}
        />

        <div
          className="absolute transition-all duration-100"
          style={{
            left: `${ballPosition.x - 20}px`,
            top: `${ballPosition.y - 20}px`,
          }}
        >
          <div className="w-10 h-10 bg-gradient-to-br from-yellow-300 to-orange-400 rounded-full shadow-lg border-4 border-orange-500 animate-spin-slow">
            <div className="absolute inset-2 bg-white rounded-full opacity-30"></div>
          </div>
        </div>

        <div
          className="absolute transition-all duration-100"
          style={{
            left: '50px',
            top: `${player1Y - 40}px`,
          }}
        >
          <div className="relative">
            <div className="w-12 h-12 bg-gradient-to-b from-blue-400 to-blue-600 rounded-full border-4 border-blue-800 shadow-lg">
              <div className="absolute top-2 left-2 w-3 h-3 bg-white rounded-full"></div>
              <div className="absolute top-2 right-2 w-3 h-3 bg-white rounded-full"></div>
              <div className="absolute bottom-2 left-3 w-6 h-2 bg-white rounded-full"></div>
            </div>
            <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 w-8 h-8 bg-gradient-to-b from-blue-300 to-blue-500 rounded-lg border-2 border-blue-800"></div>
            <div className="absolute -bottom-12 -left-2 w-6 h-8 bg-gradient-to-b from-blue-300 to-blue-500 rounded-lg border-2 border-blue-800"></div>
            <div className="absolute -bottom-12 right-2 w-6 h-8 bg-gradient-to-b from-blue-300 to-blue-500 rounded-lg border-2 border-blue-800"></div>
          </div>
        </div>

        <div
          className="absolute transition-all duration-100"
          style={{
            left: '650px',
            top: `${player2Y - 40}px`,
          }}
        >
          <div className="relative">
            <div className="w-12 h-12 bg-gradient-to-b from-red-400 to-red-600 rounded-full border-4 border-red-800 shadow-lg">
              <div className="absolute top-2 left-2 w-3 h-3 bg-white rounded-full"></div>
              <div className="absolute top-2 right-2 w-3 h-3 bg-white rounded-full"></div>
              <div className="absolute bottom-2 left-3 w-6 h-2 bg-white rounded-full"></div>
            </div>
            <div className="absolute -bottom-6 left-1/2 -translate-x-1/2 w-8 h-8 bg-gradient-to-b from-red-300 to-red-500 rounded-lg border-2 border-red-800"></div>
            <div className="absolute -bottom-12 -left-2 w-6 h-8 bg-gradient-to-b from-red-300 to-red-500 rounded-lg border-2 border-red-800"></div>
            <div className="absolute -bottom-12 right-2 w-6 h-8 bg-gradient-to-b from-red-300 to-red-500 rounded-lg border-2 border-red-800"></div>
          </div>
        </div>

        {winner && (
          <div className="absolute inset-0 bg-black bg-opacity-70 flex items-center justify-center">
            <div className="bg-white rounded-2xl p-8 text-center shadow-2xl">
              <Trophy className="w-20 h-20 mx-auto mb-4 text-yellow-500" />
              <h2 className="text-4xl font-bold mb-4 text-gray-800">
                {winner} Wins!
              </h2>
              <button
                onClick={resetGame}
                className="bg-gradient-to-r from-green-500 to-green-600 text-white px-8 py-3 rounded-full font-bold text-xl hover:from-green-600 hover:to-green-700 transition-all flex items-center gap-2 mx-auto shadow-lg"
              >
                <RotateCcw className="w-6 h-6" />
                Play Again
              </button>
            </div>
          </div>
        )}
      </div>

      <div className="mt-6 bg-white rounded-lg p-6 shadow-xl max-w-2xl">
        <h3 className="text-xl font-bold mb-3 text-gray-800">Controls</h3>
        <div className="grid grid-cols-2 gap-4">
          <div className="bg-blue-100 p-4 rounded-lg">
            <h4 className="font-bold text-blue-800 mb-2">Player 1 (Blue)</h4>
            <p className="text-sm text-gray-700">W - Jump Up</p>
            <p className="text-sm text-gray-700">S - Move Down</p>
          </div>
          <div className="bg-red-100 p-4 rounded-lg">
            <h4 className="font-bold text-red-800 mb-2">Player 2 (Red)</h4>
            <p className="text-sm text-gray-700">↑ - Jump Up</p>
            <p className="text-sm text-gray-700">↓ - Move Down</p>
          </div>
        </div>
        <p className="mt-4 text-sm text-gray-600 text-center">
          First to 5 points wins! Hit the ball when it touches your player.
        </p>
      </div>
    </div>
  );
};

export default VolleyballGame;
