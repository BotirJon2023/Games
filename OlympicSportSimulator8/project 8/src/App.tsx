import React, { useState, useRef, useEffect } from 'react';
import { Zap, Trophy, Target, Wind, Medal } from 'lucide-react';

const OlympicSportSimulator: React.FC = () => {
  const [currentSport, setCurrentSport] = useState<'sprint' | 'longjump' | 'swimming' | 'archery' | 'javelin'>('sprint');
  const [gameState, setGameState] = useState<'menu' | 'playing' | 'result'>('menu');
  const [playerScore, setPlayerScore] = useState(0);
  const [totalScore, setTotalScore] = useState(0);
  const [animationProgress, setAnimationProgress] = useState(0);
  const [powerLevel, setPowerLevel] = useState(50);
  const [sprintTime, setSprintTime] = useState(0);
  const [jumpDistance, setJumpDistance] = useState(0);
  const [swimTime, setSwimTime] = useState(0);
  const [archeryScore, setArcheryScore] = useState(0);
  const [javelinDistance, setJavelinDistance] = useState(0);
  const [sportsCompleted, setSportsCompleted] = useState(0);
  const animationRef = useRef<number | null>(null);
  const startTimeRef = useRef<number | null>(null);

  const sports = [
    { id: 'sprint', name: '100m Sprint', icon: Zap, description: 'Click to dash!' },
    { id: 'longjump', name: 'Long Jump', icon: Target, description: 'Build power then jump!' },
    { id: 'swimming', name: '100m Swimming', icon: Wind, description: 'Hold down to swim!' },
    { id: 'archery', name: 'Archery', icon: Trophy, description: 'Aim and shoot!' },
    { id: 'javelin', name: 'Javelin Throw', icon: Medal, description: 'Power throw!' }
  ];

  useEffect(() => {
    if (gameState === 'playing' && currentSport === 'sprint') {
      const startTime = Date.now();
      startTimeRef.current = startTime;

      const animate = () => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / 10000, 1);
        setAnimationProgress(progress);

        if (progress < 1) {
          animationRef.current = requestAnimationFrame(animate);
        } else {
          const time = 10 + (50 - powerLevel) * 0.08;
          setSprintTime(parseFloat(time.toFixed(2)));
          setPlayerScore(Math.max(0, 100 - time * 3));
          setGameState('result');
        }
      };

      animationRef.current = requestAnimationFrame(animate);
      return () => {
        if (animationRef.current) cancelAnimationFrame(animationRef.current);
      };
    }

    if (gameState === 'playing' && currentSport === 'longjump') {
      const timer = setTimeout(() => {
        const distance = 8 + (powerLevel / 100) * 2;
        setJumpDistance(parseFloat(distance.toFixed(2)));
        setPlayerScore(Math.max(0, distance * 5));
        setGameState('result');
      }, 3000);
      return () => clearTimeout(timer);
    }

    if (gameState === 'playing' && currentSport === 'swimming') {
      const startTime = Date.now();
      startTimeRef.current = startTime;

      const animate = () => {
        const elapsed = Date.now() - startTime;
        const progress = Math.min(elapsed / 12000, 1);
        setAnimationProgress(progress);

        if (progress < 1) {
          animationRef.current = requestAnimationFrame(animate);
        } else {
          const time = 58 + (100 - powerLevel) * 0.2;
          setSwimTime(parseFloat(time.toFixed(2)));
          setPlayerScore(Math.max(0, 100 - time * 0.5));
          setGameState('result');
        }
      };

      animationRef.current = requestAnimationFrame(animate);
      return () => {
        if (animationRef.current) cancelAnimationFrame(animationRef.current);
      };
    }

    if (gameState === 'playing' && currentSport === 'archery') {
      const timer = setTimeout(() => {
        const distance = Math.abs(50 - powerLevel);
        const score = Math.max(0, 100 - distance * 2);
        setArcheryScore(parseFloat(score.toFixed(0)));
        setPlayerScore(score);
        setGameState('result');
      }, 2500);
      return () => clearTimeout(timer);
    }

    if (gameState === 'playing' && currentSport === 'javelin') {
      const timer = setTimeout(() => {
        const distance = 50 + (powerLevel / 100) * 40;
        setJavelinDistance(parseFloat(distance.toFixed(2)));
        setPlayerScore(distance * 0.8);
        setGameState('result');
      }, 3000);
      return () => clearTimeout(timer);
    }
  }, [gameState, currentSport, powerLevel]);

  const startSprint = () => {
    setPowerLevel(50);
    setAnimationProgress(0);
    setGameState('playing');
  };

  const startLongJump = () => {
    setPowerLevel(50);
    setAnimationProgress(0);
    setGameState('playing');
  };

  const startSwimming = () => {
    setPowerLevel(50);
    setAnimationProgress(0);
    setGameState('playing');
  };

  const startArchery = () => {
    setPowerLevel(50);
    setAnimationProgress(0);
    setGameState('playing');
  };

  const startJavelin = () => {
    setPowerLevel(50);
    setAnimationProgress(0);
    setGameState('playing');
  };

  const handlePowerChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setPowerLevel(parseInt(e.target.value));
  };

  const nextSport = () => {
    const completed = sportsCompleted + 1;
    setSportsCompleted(completed);
    setTotalScore(totalScore + playerScore);

    if (completed < sports.length) {
      const sportOrder: ('sprint' | 'longjump' | 'swimming' | 'archery' | 'javelin')[] = ['sprint', 'longjump', 'swimming', 'archery', 'javelin'];
      setCurrentSport(sportOrder[completed]);
      setGameState('menu');
      setPowerLevel(50);
      setAnimationProgress(0);
    } else {
      setGameState('menu');
    }
  };

  const resetGame = () => {
    setGameState('menu');
    setPlayerScore(0);
    setTotalScore(0);
    setSportsCompleted(0);
    setPowerLevel(50);
    setAnimationProgress(0);
    setCurrentSport('sprint');
  };

  const renderSprintAnimation = () => {
    const distance = animationProgress * 400;
    return (
      <div className="relative w-full h-48 bg-gradient-to-b from-blue-100 to-green-100 rounded-lg overflow-hidden border-2 border-blue-300">
        <div className="absolute bottom-0 w-full h-20 bg-green-400"></div>
        <div className="absolute top-0 w-full h-1 bg-white"></div>

        <div
          className="absolute bottom-20 transition-all duration-100"
          style={{
            left: `${distance}px`,
            transform: `scaleX(${animationProgress < 0.5 ? 1 : -1})`,
          }}
        >
          <div className="text-5xl">🏃</div>
          <div className="text-xs mt-2 font-bold">Sprint</div>
        </div>

        <div className="absolute top-2 right-4 text-sm font-bold text-blue-600">
          {(animationProgress * 100).toFixed(1)}m
        </div>
      </div>
    );
  };

  const renderLongJumpAnimation = () => {
    const jumpHeight = Math.sin(animationProgress * Math.PI) * 80;
    const jumpDistance = animationProgress * 400;

    return (
      <div className="relative w-full h-48 bg-gradient-to-b from-blue-100 to-yellow-100 rounded-lg overflow-hidden border-2 border-blue-300">
        <div className="absolute bottom-0 w-full h-20 bg-yellow-600"></div>
        <div className="absolute bottom-20 w-full h-1 bg-green-600"></div>

        <div
          className="absolute transition-all"
          style={{
            left: `${jumpDistance}px`,
            bottom: `${80 + jumpHeight}px`,
            transform: `rotate(${animationProgress * 360}deg)`,
          }}
        >
          <div className="text-4xl">🏃</div>
        </div>

        <div className="absolute top-2 right-4 text-sm font-bold text-blue-600">
          {(animationProgress * 10).toFixed(2)}m
        </div>
      </div>
    );
  };

  const renderSwimmingAnimation = () => {
    const swimProgress = animationProgress * 400;

    return (
      <div className="relative w-full h-48 bg-gradient-to-b from-blue-300 to-blue-600 rounded-lg overflow-hidden border-2 border-blue-800">
        <div className="absolute inset-0 flex items-center justify-center">
          <div
            className="absolute transition-all"
            style={{
              left: `${swimProgress}px`,
              transform: `scaleX(${swimProgress % 50 > 25 ? -1 : 1})`,
            }}
          >
            <div className="text-4xl">🏊</div>
          </div>
        </div>

        <div className="absolute top-2 right-4 text-sm font-bold text-white">
          {(animationProgress * 100).toFixed(1)}m
        </div>
      </div>
    );
  };

  const renderArcheryAnimation = () => {
    const targetOffset = Math.abs(50 - powerLevel) / 5;
    const arrowX = 350 + targetOffset;

    return (
      <div className="relative w-full h-48 bg-gradient-to-r from-green-200 to-blue-200 rounded-lg overflow-hidden border-2 border-green-400">
        <div className="absolute left-12 top-1/2 transform -translate-y-1/2 text-5xl">🏹</div>

        <div className="absolute right-12 top-1/2 transform -translate-y-1/2">
          <div className="relative w-16 h-16 border-4 border-red-500 rounded-full bg-white flex items-center justify-center">
            <div className="w-8 h-8 border-2 border-blue-500 rounded-full"></div>
            <div className="absolute w-2 h-2 bg-yellow-400 rounded-full"></div>
          </div>
        </div>

        <div
          className="absolute top-1/2 transform -translate-y-1/2"
          style={{
            left: `${arrowX}px`,
            opacity: animationProgress > 0.3 ? 1 : 0,
          }}
        >
          <div className="text-2xl">→</div>
        </div>

        <div className="absolute top-2 right-4 text-sm font-bold text-green-700">
          Power: {powerLevel}%
        </div>
      </div>
    );
  };

  const renderJavelinAnimation = () => {
    const throwDistance = animationProgress * 400;
    const throwHeight = Math.sin(animationProgress * Math.PI) * 100;

    return (
      <div className="relative w-full h-48 bg-gradient-to-b from-sky-100 to-green-100 rounded-lg overflow-hidden border-2 border-sky-300">
        <div className="absolute bottom-0 w-full h-20 bg-green-500"></div>

        <div
          className="absolute transition-all"
          style={{
            left: `${throwDistance}px`,
            bottom: `${80 + throwHeight}px`,
            transform: `rotate(${animationProgress * 720}deg)`,
          }}
        >
          <div className="text-3xl">🪡</div>
        </div>

        <div className="absolute top-2 right-4 text-sm font-bold text-sky-700">
          {(animationProgress * 90).toFixed(1)}m
        </div>
      </div>
    );
  };

  const renderAnimation = () => {
    switch (currentSport) {
      case 'sprint':
        return renderSprintAnimation();
      case 'longjump':
        return renderLongJumpAnimation();
      case 'swimming':
        return renderSwimmingAnimation();
      case 'archery':
        return renderArcheryAnimation();
      case 'javelin':
        return renderJavelinAnimation();
      default:
        return null;
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-50 via-white to-orange-50 p-8">
      <style>{`
        @keyframes pulse-glow {
          0%, 100% { box-shadow: 0 0 20px rgba(59, 130, 246, 0.5); }
          50% { box-shadow: 0 0 30px rgba(59, 130, 246, 0.8); }
        }
        .pulse-glow {
          animation: pulse-glow 2s infinite;
        }
        @keyframes bounce-medal {
          0%, 100% { transform: translateY(0); }
          50% { transform: translateY(-20px); }
        }
        .bounce-medal {
          animation: bounce-medal 2s infinite;
        }
      `}</style>

      <div className="max-w-6xl mx-auto">
        <div className="text-center mb-8">
          <h1 className="text-5xl font-black text-transparent bg-clip-text bg-gradient-to-r from-blue-600 via-orange-500 to-red-600 mb-2">
            Olympic Sport Simulator
          </h1>
          <p className="text-gray-600 text-lg">Test your athletic abilities across five Olympic events!</p>
        </div>

        {gameState === 'menu' && (
          <div className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4">
              {sports.map((sport, index) => {
                const Icon = sport.icon;
                const isCompleted = index < sportsCompleted;
                const isCurrent = sport.id === currentSport;
                const isUpcoming = index === sportsCompleted;

                return (
                  <div
                    key={sport.id}
                    className={`p-6 rounded-xl border-2 transition-all cursor-pointer ${
                      isCompleted
                        ? 'bg-green-100 border-green-500'
                        : isUpcoming
                          ? 'bg-blue-100 border-blue-500 pulse-glow'
                          : isCurrent
                            ? 'bg-orange-100 border-orange-500'
                            : 'bg-gray-100 border-gray-300'
                    }`}
                    onClick={() => !isCompleted && setCurrentSport(sport.id as any)}
                  >
                    <Icon className="w-8 h-8 mx-auto mb-2 text-gray-700" />
                    <h3 className="font-bold text-sm">{sport.name}</h3>
                    <p className="text-xs text-gray-600 mt-2">{sport.description}</p>
                    {isCompleted && <p className="text-xs text-green-600 mt-2 font-bold">✓ Completed</p>}
                  </div>
                );
              })}
            </div>

            <div className="bg-white rounded-xl p-8 shadow-xl border-2 border-gray-200">
              <h2 className="text-2xl font-bold mb-6 text-gray-800">{sports[sportsCompleted]?.name}</h2>

              <div className="mb-8">
                <label className="block text-sm font-bold mb-4 text-gray-700">
                  Power Level: {powerLevel}%
                </label>
                <input
                  type="range"
                  min="0"
                  max="100"
                  value={powerLevel}
                  onChange={handlePowerChange}
                  className="w-full h-3 bg-gradient-to-r from-blue-400 to-orange-400 rounded-lg appearance-none cursor-pointer"
                />
              </div>

              <div className="text-center">
                {currentSport === 'sprint' && (
                  <button
                    onClick={startSprint}
                    className="px-8 py-4 bg-gradient-to-r from-blue-500 to-blue-600 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all"
                  >
                    Start Sprint!
                  </button>
                )}
                {currentSport === 'longjump' && (
                  <button
                    onClick={startLongJump}
                    className="px-8 py-4 bg-gradient-to-r from-yellow-500 to-orange-500 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all"
                  >
                    Build Power & Jump!
                  </button>
                )}
                {currentSport === 'swimming' && (
                  <button
                    onClick={startSwimming}
                    className="px-8 py-4 bg-gradient-to-r from-cyan-500 to-blue-500 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all"
                  >
                    Dive & Swim!
                  </button>
                )}
                {currentSport === 'archery' && (
                  <button
                    onClick={startArchery}
                    className="px-8 py-4 bg-gradient-to-r from-green-500 to-teal-500 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all"
                  >
                    Draw & Release!
                  </button>
                )}
                {currentSport === 'javelin' && (
                  <button
                    onClick={startJavelin}
                    className="px-8 py-4 bg-gradient-to-r from-red-500 to-pink-500 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all"
                  >
                    Throw the Javelin!
                  </button>
                )}
              </div>

              {sportsCompleted > 0 && (
                <div className="mt-8 p-4 bg-blue-50 rounded-lg border border-blue-200">
                  <p className="text-sm text-gray-700">
                    <span className="font-bold">Progress:</span> {sportsCompleted} of {sports.length} events completed
                  </p>
                  <p className="text-sm text-gray-700 mt-2">
                    <span className="font-bold">Total Score:</span> {totalScore.toFixed(0)} points
                  </p>
                </div>
              )}
            </div>
          </div>
        )}

        {gameState === 'playing' && (
          <div className="bg-white rounded-xl p-8 shadow-xl border-2 border-orange-300">
            <h2 className="text-3xl font-bold mb-6 text-center text-gray-800">{sports[sportsCompleted]?.name}</h2>
            {renderAnimation()}
          </div>
        )}

        {gameState === 'result' && (
          <div className="bg-white rounded-xl p-8 shadow-xl border-2 border-green-400">
            <div className="text-center mb-8">
              <div className="bounce-medal text-6xl mb-4">🏅</div>
              <h2 className="text-3xl font-bold text-gray-800 mb-2">Event Complete!</h2>
              <p className="text-gray-600 text-lg">{sports[sportsCompleted]?.name}</p>
            </div>

            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-8">
              <div className="bg-blue-50 p-6 rounded-lg border border-blue-200">
                <h3 className="text-lg font-bold text-blue-700 mb-4">Performance</h3>
                {currentSport === 'sprint' && (
                  <>
                    <p className="text-gray-700">Time: <span className="font-bold text-blue-600">{sprintTime}s</span></p>
                  </>
                )}
                {currentSport === 'longjump' && (
                  <>
                    <p className="text-gray-700">Distance: <span className="font-bold text-blue-600">{jumpDistance}m</span></p>
                  </>
                )}
                {currentSport === 'swimming' && (
                  <>
                    <p className="text-gray-700">Time: <span className="font-bold text-blue-600">{swimTime}s</span></p>
                  </>
                )}
                {currentSport === 'archery' && (
                  <>
                    <p className="text-gray-700">Accuracy: <span className="font-bold text-blue-600">{archeryScore.toFixed(0)}/100</span></p>
                  </>
                )}
                {currentSport === 'javelin' && (
                  <>
                    <p className="text-gray-700">Distance: <span className="font-bold text-blue-600">{javelinDistance}m</span></p>
                  </>
                )}
              </div>

              <div className="bg-orange-50 p-6 rounded-lg border border-orange-200">
                <h3 className="text-lg font-bold text-orange-700 mb-4">Score</h3>
                <p className="text-4xl font-black text-orange-600 mb-4">{playerScore.toFixed(0)}</p>
                <p className="text-gray-700">Event Points</p>
              </div>
            </div>

            <div className="space-y-3">
              {sportsCompleted < sports.length - 1 && (
                <button
                  onClick={nextSport}
                  className="w-full px-6 py-3 bg-gradient-to-r from-green-500 to-emerald-600 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all"
                >
                  Next Event →
                </button>
              )}
              {sportsCompleted === sports.length - 1 && (
                <div>
                  <button
                    onClick={nextSport}
                    className="w-full px-6 py-3 bg-gradient-to-r from-gold-400 to-yellow-500 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all mb-3"
                  >
                    See Final Results
                  </button>
                </div>
              )}
              {sportsCompleted === sports.length && (
                <div className="bg-gradient-to-r from-purple-100 to-pink-100 p-6 rounded-lg border-2 border-purple-300 text-center">
                  <h3 className="text-2xl font-bold text-purple-800 mb-2">Games Complete!</h3>
                  <p className="text-3xl font-black text-purple-600 mb-4">Final Score: {totalScore.toFixed(0)}</p>
                  <button
                    onClick={resetGame}
                    className="px-6 py-3 bg-gradient-to-r from-purple-500 to-pink-500 text-white font-bold rounded-lg hover:shadow-lg transform hover:scale-105 transition-all"
                  >
                    Play Again
                  </button>
                </div>
              )}
            </div>
          </div>
        )}
      </div>
    </div>
  );
};

export default OlympicSportSimulator;
