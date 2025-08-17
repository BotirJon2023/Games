import React, { useEffect, useRef } from 'react';
import { useGameState } from './hooks/useGameState';
import { BaseballField } from './components/BaseballField';
import { Scoreboard } from './components/Scoreboard';
import { GameControls } from './components/GameControls';
import { PitchIndicator } from './components/PitchIndicator';
import { AnimationEffects } from './components/AnimationEffects';
import { Baseline as Baseball } from 'lucide-react';

function App() {
  const {
    state,
    startPitch,
    swing,
    resetCount,
    newGame,
    setDifficulty,
    updateBallPosition,
  } = useGameState();

  const animationRef = useRef<number>();
  const startTimeRef = useRef<number>();

  // Animate ball movement during pitch
  useEffect(() => {
    if (state.pitchInProgress) {
      startTimeRef.current = Date.now();
      
      const animateBall = () => {
        const elapsed = Date.now() - (startTimeRef.current || 0);
        const duration = 2000; // 2 seconds for pitch
        const progress = Math.min(elapsed / duration, 1);
        
        // Calculate ball position with some curve
        const x = 10 + (progress * 80); // Move from left (10%) to right (90%)
        const y = 50 + Math.sin(progress * Math.PI) * 10; // Add some vertical movement
        
        updateBallPosition({ x, y });
        
        if (progress < 1) {
          animationRef.current = requestAnimationFrame(animateBall);
        }
      };
      
      animationRef.current = requestAnimationFrame(animateBall);
    }

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [state.pitchInProgress, updateBallPosition]);

  const handleSwing = () => {
    const swingTime = Date.now() - (startTimeRef.current || Date.now());
    swing(swingTime);
  };

  const canSwing = state.pitchInProgress && !state.isSwinging;

  return (
    <div className="min-h-screen bg-gradient-to-br from-blue-900 via-green-800 to-blue-900 p-4">
      <div className="max-w-7xl mx-auto">
        {/* Header */}
        <header className="text-center mb-8">
          <div className="flex items-center justify-center mb-4">
            <Baseball className="text-white mr-4" size={48} />
            <h1 className="text-5xl font-bold text-white drop-shadow-lg">
              Baseball Batting Simulator
            </h1>
            <Baseball className="text-white ml-4" size={48} />
          </div>
          <p className="text-xl text-blue-200">
            Step up to the plate and test your batting skills!
          </p>
        </header>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Game Field and Controls */}
          <div className="lg:col-span-2 space-y-6">
            <BaseballField
              ballPosition={state.ballPosition}
              isSwinging={state.isSwinging}
              pitchInProgress={state.pitchInProgress}
            />
            
            <GameControls
              onStartPitch={startPitch}
              onSwing={handleSwing}
              onResetCount={resetCount}
              onNewGame={newGame}
              onSetDifficulty={setDifficulty}
              difficulty={state.difficulty}
              pitchInProgress={state.pitchInProgress}
              gameOver={state.gameOver}
              canSwing={canSwing}
            />
          </div>

          {/* Right Column - Scoreboard and Pitch Info */}
          <div className="space-y-6">
            <Scoreboard
              balls={state.balls}
              strikes={state.strikes}
              stats={state.stats}
              difficulty={state.difficulty}
              message={state.message}
            />
            
            <PitchIndicator
              currentPitch={state.currentPitch}
              pitchInProgress={state.pitchInProgress}
            />
          </div>
        </div>

        {/* Footer */}
        <footer className="mt-12 text-center text-blue-200">
          <p className="text-lg">
            Master your timing, watch the ball, and swing for the fences!
          </p>
          <p className="text-sm mt-2 opacity-75">
            Built with React, TypeScript, and Tailwind CSS
          </p>
        </footer>
      </div>

      {/* Animation Effects Overlay */}
      <AnimationEffects
        lastResult={state.message.includes('HOME RUN') ? 'homerun' : 
                   state.message.includes('hit') ? 'hit' :
                   state.message.includes('Strike') ? 'strike' :
                   state.message.includes('Ball') ? 'ball' :
                   state.message.includes('Foul') ? 'foul' : undefined}
        isVisible={true}
      />
    </div>
  );
}

export default App;