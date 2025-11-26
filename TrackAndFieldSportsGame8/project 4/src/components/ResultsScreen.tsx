import React from 'react';
import { Trophy, RotateCcw } from 'lucide-react';

interface EventResult {
  eventName: string;
  performance: number;
  score: number;
}

interface ResultsScreenProps {
  results: EventResult[];
  totalScore: number;
  onReset: () => void;
}

export default function ResultsScreen({ results, totalScore, onReset }: ResultsScreenProps) {
  const getPerformanceLabel = (eventName: string, performance: number) => {
    switch (eventName) {
      case '100m Sprint':
        return `${performance.toFixed(2)}s`;
      case 'Long Jump':
        return `${performance.toFixed(2)}m`;
      case 'Javelin Throw':
        return `${performance.toFixed(2)}m`;
      case 'High Jump':
        return `${performance.toFixed(2)}m`;
      default:
        return performance.toFixed(2);
    }
  };

  const getMedalEmoji = (index: number) => {
    const medals = ['🥇', '🥈', '🥉', '🏅'];
    return medals[index] || '🏅';
  };

  const getRanking = (totalScore: number) => {
    if (totalScore >= 3000) return { title: 'Olympic Champion', color: 'gold' };
    if (totalScore >= 2500) return { title: 'Elite Athlete', color: 'silver' };
    if (totalScore >= 2000) return { title: 'Professional Competitor', color: 'amber' };
    if (totalScore >= 1500) return { title: 'Advanced Athlete', color: 'blue' };
    return { title: 'Beginner Athlete', color: 'gray' };
  };

  const ranking = getRanking(totalScore);

  return (
    <div className="max-w-4xl mx-auto">
      <div className="bg-white rounded-lg shadow-xl p-8 text-center mb-8">
        <Trophy className="mx-auto mb-4 text-yellow-500" size={64} />
        <h1 className="text-4xl font-bold text-blue-900 mb-2">Congratulations!</h1>
        <p className="text-2xl text-gray-700 mb-4">You've completed all events!</p>

        <div className="bg-gradient-to-r from-blue-500 to-blue-700 text-white rounded-lg p-6 mb-6">
          <p className="text-lg mb-2">Your Ranking</p>
          <h2 className="text-3xl font-bold mb-4">{ranking.title}</h2>
          <div className="text-5xl font-bold">{totalScore}</div>
          <p className="text-blue-100 mt-2">Total Points</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
        {results.map((result, index) => (
          <div
            key={index}
            className="bg-white rounded-lg shadow-lg p-6 border-t-4 border-blue-500"
          >
            <div className="flex items-start justify-between mb-4">
              <div>
                <h3 className="text-xl font-bold text-blue-900">{result.eventName}</h3>
              </div>
              <div className="text-4xl">{getMedalEmoji(index)}</div>
            </div>

            <div className="space-y-3">
              <div className="flex justify-between items-center">
                <span className="text-gray-600">Performance:</span>
                <span className="text-xl font-semibold text-blue-700">
                  {getPerformanceLabel(result.eventName, result.performance)}
                </span>
              </div>

              <div className="flex justify-between items-center">
                <span className="text-gray-600">Points Earned:</span>
                <span className="text-2xl font-bold text-green-600">{result.score}</span>
              </div>
            </div>
          </div>
        ))}
      </div>

      <div className="bg-blue-50 rounded-lg p-6 mb-8 border-l-4 border-blue-500">
        <h3 className="text-lg font-bold text-blue-900 mb-3">Performance Breakdown</h3>
        <div className="space-y-2">
          {results.map((result, index) => (
            <div key={index} className="flex justify-between items-center">
              <span className="text-gray-700">{result.eventName}</span>
              <div className="flex items-center gap-2">
                <div className="w-40 bg-gray-300 h-2 rounded-full overflow-hidden">
                  <div
                    className="bg-green-500 h-full transition-all duration-300"
                    style={{ width: `${(result.score / 500) * 100}%` }}
                  ></div>
                </div>
                <span className="text-sm font-semibold text-gray-700 w-12">
                  {result.score}
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>

      <div className="flex gap-4">
        <button
          onClick={onReset}
          className="flex-1 bg-blue-600 hover:bg-blue-700 text-white py-4 rounded-lg font-bold flex items-center justify-center gap-2 text-lg transition-all shadow-lg"
        >
          <RotateCcw size={24} />
          Play Again
        </button>

        <button
          className="flex-1 bg-gray-600 hover:bg-gray-700 text-white py-4 rounded-lg font-bold text-lg transition-all shadow-lg"
        >
          Share Results
        </button>
      </div>

      <div className="mt-8 bg-yellow-50 rounded-lg p-6 border border-yellow-200">
        <h3 className="font-bold text-yellow-900 mb-2">Tips for Better Performance</h3>
        <ul className="text-sm text-yellow-800 space-y-1">
          <li>• Sprint: Keep a steady pace throughout the run</li>
          <li>• Long Jump: Balance power with technique for maximum distance</li>
          <li>• Javelin: 45° is the optimal throwing angle</li>
          <li>• High Jump: Build up power gradually as the bar gets higher</li>
        </ul>
      </div>
    </div>
  );
}
