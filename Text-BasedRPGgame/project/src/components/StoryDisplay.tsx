import React from 'react';
import { StoryScene, Choice } from '../types/game';
import { useTypewriter } from '../hooks/useTypewriter';

interface StoryDisplayProps {
  scene: StoryScene;
  onChoice: (action: string) => void;
}

export const StoryDisplay: React.FC<StoryDisplayProps> = ({ scene, onChoice }) => {
  const { displayText, isComplete, skipAnimation } = useTypewriter(scene.text, 30);

  return (
    <div className="bg-black/40 backdrop-blur-lg rounded-xl p-6 border border-purple-500/20">
      <h2 className="text-2xl font-bold text-gold-300 mb-4">{scene.title}</h2>
      
      <div 
        className="text-gray-100 text-lg leading-relaxed mb-6 min-h-[120px] cursor-pointer"
        onClick={!isComplete ? skipAnimation : undefined}
      >
        {displayText}
        {!isComplete && <span className="animate-pulse">|</span>}
      </div>

      {!isComplete && (
        <p className="text-sm text-gray-400 italic mb-4">Click to skip animation...</p>
      )}

      {isComplete && (
        <div className="space-y-3">
          <p className="text-gold-300 font-medium mb-3">What do you do?</p>
          {scene.choices.map((choice, index) => (
            <button
              key={index}
              onClick={() => onChoice(choice.action)}
              className="w-full text-left p-4 bg-gray-800/30 hover:bg-gray-700/40 border border-purple-500/20 hover:border-gold-400/50 rounded-lg transition-all duration-200 group"
            >
              <span className="text-white group-hover:text-gold-300 transition-colors">
                {choice.text}
              </span>
              {choice.requirement && (
                <div className="text-xs text-gray-400 mt-1">
                  Requires {choice.requirement.stat}: {choice.requirement.value}
                </div>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
};