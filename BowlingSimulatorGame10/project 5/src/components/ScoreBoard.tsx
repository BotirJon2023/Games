import React from 'react';

interface ScoreBoardProps {
  scores: number[];
  currentFrameThrows: number[];
  frameCount: number;
  totalScore: number;
}

export const ScoreBoard: React.FC<ScoreBoardProps> = ({
  scores,
  currentFrameThrows,
  frameCount,
  totalScore,
}) => {
  const getFrameDisplay = (frameIndex: number, throws: number[]): string[] => {
    if (frameIndex < 9) {
      if (throws.length === 0) return ['', ''];
      if (throws[0] === 10) return ['X', ''];
      if (throws.length === 2 && throws[0] + throws[1] === 10)
        return [throws[0].toString(), '/'];
      if (throws.length === 1) return [throws[0].toString(), ''];
      return [throws[0].toString(), throws[1].toString()];
    } else {
      return throws.map((t) => (t === 10 ? 'X' : t.toString()));
    }
  };

  const getThrowsForFrame = (frameIndex: number): number[] => {
    if (frameIndex === frameCount) return currentFrameThrows;
    if (frameIndex > frameCount) return [];

    let throwIndex = 0;
    for (let i = 0; i < frameIndex; i++) {
      if (i < 9) {
        if (scores[i] >= 10 && i === 0) {
          throwIndex += 1;
        } else if (i > 0) {
          throwIndex += 2;
        }
      } else {
        throwIndex += 3;
      }
    }

    if (frameIndex === 9) {
      const remainingThrows = [];
      for (let i = throwIndex; i < 21; i++) {
        const tempFrameScore = scores[frameIndex];
        if (tempFrameScore > 0 || currentFrameThrows.length > 0) break;
      }
      return currentFrameThrows;
    }

    return [];
  };

  return (
    <div className="w-full bg-gray-800 p-6 rounded-lg">
      <div className="grid grid-cols-10 gap-2 mb-6">
        {scores.map((score, index) => (
          <div key={index} className="bg-gray-900 rounded p-3 text-center">
            <div className="text-xs font-bold text-gray-400 mb-1">
              Frame {index + 1}
            </div>
            <div className="flex justify-between gap-2 mb-2">
              {getFrameDisplay(index, getThrowsForFrame(index)).map(
                (display, i) => (
                  <div
                    key={i}
                    className="bg-gray-700 rounded px-2 py-1 min-w-[20px]"
                  >
                    <span className="text-white font-bold text-sm">
                      {display}
                    </span>
                  </div>
                )
              )}
            </div>
            {score > 0 && (
              <div className="text-lg font-bold text-yellow-400">{score}</div>
            )}
          </div>
        ))}
      </div>

      <div className="bg-gradient-to-r from-yellow-500 to-yellow-600 rounded-lg p-4 text-center">
        <div className="text-gray-800 text-sm font-bold mb-1">TOTAL SCORE</div>
        <div className="text-5xl font-bold text-white drop-shadow-lg">
          {totalScore}
        </div>
      </div>
    </div>
  );
};
