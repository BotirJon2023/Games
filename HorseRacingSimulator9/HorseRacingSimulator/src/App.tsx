import { useState, useEffect, useRef } from 'react';
import { Play, Pause, RotateCcw, History } from 'lucide-react';

interface Horse {
  id: number;
  name: string;
  color: string;
  position: number;
  speed: number;
  baseSpeed: number;
  stamina: number;
  finished: boolean;
  finishPosition: number;
  finishTime: number;
  legOffset: number;
  wins: number;
  races: number;
}

interface RaceResult {
  raceNumber: number;
  horses: Horse[];
  timestamp: Date;
}

const TRACK_LENGTH = 1000;
const NUM_LANES = 6;
const LANE_HEIGHT = 100;
const START_X = 60;

function App() {
  const [horses, setHorses] = useState<Horse[]>([]);
  const [raceInProgress, setRaceInProgress] = useState(false);
  const [raceNumber, setRaceNumber] = useState(1);
  const [raceHistory, setRaceHistory] = useState<RaceResult[]>([]);
  const [showHistory, setShowHistory] = useState(false);
  const animationFrameRef = useRef(0);
  const raceTimerRef = useRef<NodeJS.Timeout | null>(null);

  useEffect(() => {
    initializeHorses();
  }, []);

  const initializeHorses = () => {
    const horseNames = [
      'Thunder Bolt',
      'Lightning Storm',
      'Midnight Runner',
      'Golden Arrow',
      'Silver Streak',
      'Blazing Phoenix',
    ];
    const horseColors = [
      '#8B4513',
      '#654321',
      '#000000',
      '#B8860B',
      '#C0C0C0',
      '#B22222',
    ];

    const newHorses: Horse[] = horseNames.map((name, i) => ({
      id: i,
      name,
      color: horseColors[i],
      position: 0,
      speed: 3 + Math.random() * 2,
      baseSpeed: 3 + Math.random() * 2,
      stamina: 0.8 + Math.random() * 0.2,
      finished: false,
      finishPosition: 0,
      finishTime: 0,
      legOffset: 0,
      wins: 0,
      races: 0,
    }));
    setHorses(newHorses);
  };

  const startRace = () => {
    if (raceInProgress) return;

    const resetHorses = horses.map(h => ({
      ...h,
      position: 0,
      finished: false,
      finishPosition: 0,
      finishTime: 0,
      speed: h.baseSpeed,
    }));
    setHorses(resetHorses);
    setRaceInProgress(true);

    let frameCount = 0;
    raceTimerRef.current = setInterval(() => {
      frameCount++;
      setHorses(prevHorses => {
        let allFinished = true;
        const finishTimes = new Map<number, number>();

        const updatedHorses = prevHorses.map(horse => {
          if (horse.finished) return horse;

          allFinished = false;
          const speedVariation = (Math.random() - 0.5) * 0.5;
          const staminaFactor =
            1.0 - (horse.position / TRACK_LENGTH) * (1.0 - horse.stamina);
          let newSpeed =
            horse.baseSpeed * staminaFactor + speedVariation;
          newSpeed = Math.max(1.0, Math.min(newSpeed, horse.baseSpeed * 1.2));

          const newPosition = horse.position + newSpeed;
          let isFinished = false;
          let finishPos = horse.finishPosition;

          if (newPosition >= TRACK_LENGTH && !horse.finished) {
            isFinished = true;
            const currentFinishes = prevHorses.filter(
              h => h.finishPosition > 0
            ).length;
            finishPos = currentFinishes + 1;
            finishTimes.set(horse.id, frameCount);
          }

          return {
            ...horse,
            position: Math.min(newPosition, TRACK_LENGTH),
            speed: newSpeed,
            finished: isFinished,
            finishPosition: finishPos,
            finishTime: isFinished ? frameCount * 0.05 : horse.finishTime,
            legOffset: Math.sin(frameCount * 0.1) * 5,
          };
        });

        if (allFinished) {
          setRaceInProgress(false);
          if (raceTimerRef.current) {
            clearInterval(raceTimerRef.current);
          }

          const sortedHorses = [...updatedHorses].sort(
            (a, b) => a.finishPosition - b.finishPosition
          );

          setRaceHistory(prev => [
            ...prev,
            {
              raceNumber,
              horses: sortedHorses,
              timestamp: new Date(),
            },
          ]);

          const finalHorses = updatedHorses.map(h => {
            const sorted = sortedHorses.find(sh => sh.id === h.id);
            if (sorted && sorted.finishPosition === 1) {
              return { ...h, wins: h.wins + 1 };
            }
            return h;
          });

          const racedHorses = finalHorses.map(h => ({
            ...h,
            races: h.races + 1,
          }));

          setHorses(racedHorses);
          setRaceNumber(prev => prev + 1);
        }

        return updatedHorses;
      });

      animationFrameRef.current++;
    }, 50);
  };

  const resetRace = () => {
    if (raceTimerRef.current) {
      clearInterval(raceTimerRef.current);
    }
    setRaceInProgress(false);
    const resetHorses = horses.map(h => ({
      ...h,
      position: 0,
      finished: false,
      finishPosition: 0,
      finishTime: 0,
      speed: h.baseSpeed,
    }));
    setHorses(resetHorses);
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-slate-900 to-slate-800 p-6">
      <div className="max-w-7xl mx-auto">
        <h1 className="text-5xl font-bold text-white mb-2 text-center">
          Horse Racing Simulator
        </h1>
        <p className="text-gray-400 text-center mb-8">Professional Edition</p>

        <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
          <div className="lg:col-span-3">
            <div className="bg-gradient-to-b from-green-700 to-green-900 rounded-lg shadow-2xl p-6 mb-6">
              <div className="relative bg-gradient-to-b from-green-600 to-green-800 rounded-lg p-4 overflow-hidden">
                {raceInProgress && (
                  <div className="absolute top-4 right-4 flex items-center gap-2 bg-red-500 px-3 py-1 rounded-full animate-pulse">
                    <div className="w-2 h-2 bg-white rounded-full"></div>
                    <span className="text-white text-sm font-bold">LIVE</span>
                  </div>
                )}

                <div className="text-white font-bold mb-2">
                  Race #{raceNumber}
                </div>

                <svg
                  viewBox="0 0 1100 720"
                  className="w-full border-4 border-white rounded"
                >
                  <defs>
                    <pattern
                      id="checkered"
                      x="0"
                      y="0"
                      width="20"
                      height="20"
                      patternUnits="userSpaceOnUse"
                    >
                      <rect x="0" y="0" width="10" height="10" fill="black" />
                      <rect
                        x="10"
                        y="0"
                        width="10"
                        height="10"
                        fill="white"
                      />
                      <rect
                        x="0"
                        y="10"
                        width="10"
                        height="10"
                        fill="white"
                      />
                      <rect
                        x="10"
                        y="10"
                        width="10"
                        height="10"
                        fill="black"
                      />
                    </pattern>
                  </defs>

                  {horses.map((horse, idx) => {
                    const y = 50 + idx * LANE_HEIGHT + LANE_HEIGHT / 2;
                    const laneBg = idx % 2 === 0 ? '#228B22' : '#1a7a1a';

                    return (
                      <g key={horse.id}>
                        <rect
                          x={START_X}
                          y={50 + idx * LANE_HEIGHT}
                          width={TRACK_LENGTH}
                          height={LANE_HEIGHT}
                          fill={laneBg}
                          stroke="white"
                          strokeWidth="2"
                        />

                        <line
                          x1={START_X}
                          y1={50 + (idx + 1) * LANE_HEIGHT}
                          x2={START_X + TRACK_LENGTH}
                          y2={50 + (idx + 1) * LANE_HEIGHT}
                          stroke="white"
                          strokeWidth="2"
                          strokeDasharray="10,10"
                          strokeDashoffset={animationFrameRef.current % 20}
                        />

                        <HorseGraphic
                          x={START_X + horse.position}
                          y={y}
                          color={horse.color}
                          isFinished={horse.finished}
                          legOffset={horse.legOffset}
                          name={horse.name}
                        />
                      </g>
                    );
                  })}

                  <rect
                    x={START_X + TRACK_LENGTH - 40}
                    y={50}
                    width={40}
                    height={NUM_LANES * LANE_HEIGHT}
                    fill="url(#checkered)"
                    stroke="gold"
                    strokeWidth="3"
                  />
                </svg>
              </div>

              <div className="flex gap-4 mt-6 justify-center">
                <button
                  onClick={startRace}
                  disabled={raceInProgress}
                  className="flex items-center gap-2 bg-green-600 hover:bg-green-500 disabled:bg-gray-600 text-white font-bold py-3 px-6 rounded-lg transition-colors"
                >
                  <Play size={20} /> Start Race
                </button>
                <button
                  onClick={resetRace}
                  className="flex items-center gap-2 bg-orange-600 hover:bg-orange-500 text-white font-bold py-3 px-6 rounded-lg transition-colors"
                >
                  <RotateCcw size={20} /> Reset
                </button>
                <button
                  onClick={() => setShowHistory(!showHistory)}
                  className="flex items-center gap-2 bg-blue-600 hover:bg-blue-500 text-white font-bold py-3 px-6 rounded-lg transition-colors"
                >
                  <History size={20} /> History
                </button>
              </div>
            </div>

            {showHistory && raceHistory.length > 0 && (
              <div className="bg-white rounded-lg shadow-lg p-6">
                <h2 className="text-2xl font-bold mb-4">Race History</h2>
                <div className="space-y-4 max-h-64 overflow-y-auto">
                  {raceHistory.map((result, idx) => (
                    <div
                      key={idx}
                      className="border-l-4 border-blue-600 pl-4 py-2"
                    >
                      <h3 className="font-bold text-lg">
                        Race #{result.raceNumber}
                      </h3>
                      <div className="text-sm text-gray-600">
                        {result.timestamp.toLocaleTimeString()}
                      </div>
                      <div className="mt-2 space-y-1">
                        {result.horses.map((h, i) => (
                          <div key={h.id} className="text-sm">
                            <span className="font-semibold">
                              {i + 1}. {h.name}
                            </span>
                            {h.finishTime > 0 && (
                              <span className="text-gray-600">
                                {' '}
                                - {h.finishTime.toFixed(2)}s
                              </span>
                            )}
                          </div>
                        ))}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
          </div>

          <div className="bg-white rounded-lg shadow-lg p-6 h-fit">
            <h2 className="text-2xl font-bold mb-4 text-gray-800">
              Horse Statistics
            </h2>
            <div className="space-y-4 max-h-96 overflow-y-auto">
              {horses.map((horse, idx) => (
                <div key={horse.id} className="pb-4 border-b last:border-b-0">
                  <div className="flex items-center gap-2 mb-2">
                    <div
                      className="w-4 h-4 rounded-full"
                      style={{ backgroundColor: horse.color }}
                    ></div>
                    <h3 className="font-bold text-gray-800">
                      Lane {idx + 1}: {horse.name}
                    </h3>
                  </div>
                  <div className="text-sm text-gray-600 space-y-1">
                    <div>
                      Position:{' '}
                      <span className="font-semibold">
                        {horse.position.toFixed(0)}m
                      </span>
                    </div>
                    <div className="w-full bg-gray-200 rounded-full h-2">
                      <div
                        className="bg-blue-600 h-2 rounded-full transition-all"
                        style={{
                          width: `${(horse.position / TRACK_LENGTH) * 100}%`,
                        }}
                      ></div>
                    </div>
                    <div>
                      Status:{' '}
                      <span
                        className={`font-semibold ${
                          horse.finished
                            ? 'text-green-600'
                            : 'text-orange-600'
                        }`}
                      >
                        {horse.finished
                          ? `FINISHED #${horse.finishPosition}`
                          : 'RACING'}
                      </span>
                    </div>
                    {horse.races > 0 && (
                      <>
                        <div>
                          Wins:{' '}
                          <span className="font-semibold">
                            {horse.wins}/{horse.races}
                          </span>
                        </div>
                        <div>
                          Win Rate:{' '}
                          <span className="font-semibold">
                            {((horse.wins / horse.races) * 100).toFixed(1)}%
                          </span>
                        </div>
                      </>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function HorseGraphic({
  x,
  y,
  color,
  isFinished,
  legOffset,
  name,
}: {
  x: number;
  y: number;
  color: string;
  isFinished: boolean;
  legOffset: number;
  name: string;
}) {
  const bodyWidth = 40;
  const bodyHeight = 25;
  const headSize = 20;

  return (
    <g>
      <circle cx={x} cy={y} r={bodyWidth / 2} fill={color} />
      <circle cx={x + bodyWidth / 2 - 5} cy={y - 5} r={headSize / 2} fill={color} />

      <line
        x1={x - 10}
        y1={y + bodyHeight / 2}
        x2={x - 10}
        y2={y + bodyHeight / 2 + 15 + legOffset}
        stroke={color}
        strokeWidth="3"
      />
      <line
        x1={x - 5}
        y1={y + bodyHeight / 2}
        x2={x - 5}
        y2={y + bodyHeight / 2 + 15 - legOffset}
        stroke={color}
        strokeWidth="3"
      />
      <line
        x1={x + 5}
        y1={y + bodyHeight / 2}
        x2={x + 5}
        y2={y + bodyHeight / 2 + 15 - legOffset}
        stroke={color}
        strokeWidth="3"
      />
      <line
        x1={x + 10}
        y1={y + bodyHeight / 2}
        x2={x + 10}
        y2={y + bodyHeight / 2 + 15 + legOffset}
        stroke={color}
        strokeWidth="3"
      />

      <circle cx={x + bodyWidth / 2 + 5} cy={y - 5} r="2" fill="black" />

      {!isFinished && (
        <polygon points={`${x + bodyWidth / 2 + 15},${y - 10} ${x + bodyWidth / 2 + 35},${y} ${x + bodyWidth / 2 + 20},${y + 10}`} fill="red" opacity="0.6" />
      )}

      <rect
        x={x - 20}
        y={y - 35}
        width="40"
        height="20"
        rx="5"
        fill="rgba(0, 0, 0, 0.7)"
      />
      <text
        x={x}
        y={y - 20}
        textAnchor="middle"
        fill="white"
        fontSize="10"
        fontWeight="bold"
      >
        {name.substring(0, 8)}
      </text>
    </g>
  );
}

export default App;
