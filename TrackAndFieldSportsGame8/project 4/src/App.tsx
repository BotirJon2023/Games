import React, { useState, useEffect, useRef } from 'react';
import { Play, RotateCcw, Trophy } from 'lucide-react';
import SprintEvent from './events/SprintEvent';
import LongJumpEvent from './events/LongJumpEvent';
import JavelinEvent from './events/JavelinEvent';
import HighJumpEvent from './events/HighJumpEvent';
import ResultsScreen from './components/ResultsScreen';

type EventType = 'menu' | 'sprint' | 'longJump' | 'javelin' | 'highJump' | 'results';

interface EventResult {
  eventName: string;
  performance: number;
  score: number;
}

function App() {
  const [currentEvent, setCurrentEvent] = useState<EventType>('menu');
  const [results, setResults] = useState<EventResult[]>([]);
  const [totalScore, setTotalScore] = useState(0);

  const handleEventComplete = (eventName: string, performance: number, score: number) => {
    const newResult: EventResult = { eventName, performance, score };
    const newResults = [...results, newResult];
    setResults(newResults);
    setTotalScore(totalScore + score);

    if (newResults.length >= 4) {
      setCurrentEvent('results');
    } else {
      setCurrentEvent('menu');
    }
  };

  const resetGame = () => {
    setCurrentEvent('menu');
    setResults([]);
    setTotalScore(0);
  };

  return (
    <div className="min-h-screen bg-gradient-to-b from-sky-400 to-sky-200">
      <header className="bg-blue-900 text-white py-6 shadow-lg">
        <div className="max-w-6xl mx-auto px-4">
          <h1 className="text-4xl font-bold">🏅 Track & Field Championship</h1>
          <p className="text-blue-100 mt-2">Master all four sports to become a champion!</p>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-4 py-8">
        {currentEvent === 'menu' && (
          <MenuScreen onSelectEvent={setCurrentEvent} completedEvents={results.length} />
        )}

        {currentEvent === 'sprint' && (
          <SprintEvent onComplete={handleEventComplete} />
        )}

        {currentEvent === 'longJump' && (
          <LongJumpEvent onComplete={handleEventComplete} />
        )}

        {currentEvent === 'javelin' && (
          <JavelinEvent onComplete={handleEventComplete} />
        )}

        {currentEvent === 'highJump' && (
          <HighJumpEvent onComplete={handleEventComplete} />
        )}

        {currentEvent === 'results' && (
          <ResultsScreen results={results} totalScore={totalScore} onReset={resetGame} />
        )}
      </main>

      {currentEvent !== 'menu' && currentEvent !== 'results' && (
        <div className="fixed top-4 right-4">
          <button
            onClick={() => setCurrentEvent('menu')}
            className="bg-red-500 hover:bg-red-600 text-white px-4 py-2 rounded-lg font-semibold flex items-center gap-2 shadow-lg"
          >
            <RotateCcw size={18} />
            Back to Menu
          </button>
        </div>
      )}
    </div>
  );
}

function MenuScreen({ onSelectEvent, completedEvents }: { onSelectEvent: (event: EventType) => void; completedEvents: number }) {
  return (
    <div className="space-y-8">
      <div className="text-center mb-8">
        <h2 className="text-3xl font-bold text-blue-900 mb-2">Events Progress</h2>
        <p className="text-xl text-gray-700">{completedEvents} / 4 Events Completed</p>
        <div className="w-full bg-gray-300 h-4 rounded-full mt-4 max-w-md mx-auto overflow-hidden">
          <div
            className="bg-green-500 h-full transition-all duration-300"
            style={{ width: `${(completedEvents / 4) * 100}%` }}
          ></div>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <EventCard
          title="100m Sprint"
          description="Run as fast as you can! Click to start and try to beat the best time."
          icon="🏃"
          onClick={() => onSelectEvent('sprint')}
          disabled={completedEvents > 0 && completedEvents < 4}
        />
        <EventCard
          title="Long Jump"
          description="Adjust power and timing to jump the farthest distance possible."
          icon="🦘"
          onClick={() => onSelectEvent('longJump')}
          disabled={completedEvents > 1 && completedEvents < 4}
        />
        <EventCard
          title="Javelin Throw"
          description="Choose your angle and power to throw the javelin as far as you can."
          icon="🔱"
          onClick={() => onSelectEvent('javelin')}
          disabled={completedEvents > 2 && completedEvents < 4}
        />
        <EventCard
          title="High Jump"
          description="Build up speed and jump over the bar at the highest possible height."
          icon="🤸"
          onClick={() => onSelectEvent('highJump')}
          disabled={completedEvents > 3 && completedEvents < 4}
        />
      </div>
    </div>
  );
}

function EventCard({
  title,
  description,
  icon,
  onClick,
  disabled,
}: {
  title: string;
  description: string;
  icon: string;
  onClick: () => void;
  disabled: boolean;
}) {
  return (
    <button
      onClick={onClick}
      disabled={disabled}
      className={`bg-white rounded-lg shadow-lg p-6 text-left transition-all duration-300 ${
        disabled
          ? 'opacity-50 cursor-not-allowed'
          : 'hover:shadow-xl hover:scale-105 cursor-pointer'
      }`}
    >
      <div className="text-4xl mb-4">{icon}</div>
      <h3 className="text-2xl font-bold text-blue-900 mb-2">{title}</h3>
      <p className="text-gray-600 mb-4">{description}</p>
      <div className="flex items-center gap-2 text-blue-600 font-semibold">
        <Play size={18} />
        {disabled ? 'Complete in order' : 'Start Event'}
      </div>
    </button>
  );
}

export default App;
