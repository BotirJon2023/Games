import { useReducer, useCallback } from 'react';
import { GameState, GameAction, GameStats, PitchResult, Pitch } from '../types/game';

const initialStats: GameStats = {
  hits: 0,
  atBats: 0,
  homeRuns: 0,
  strikeouts: 0,
  walks: 0,
  battingAverage: 0,
  currentStreak: 0,
  bestStreak: 0,
};

const initialState: GameState = {
  balls: 0,
  strikes: 0,
  isSwinging: false,
  gameOver: false,
  message: 'Click "Start Pitching" to begin!',
  stats: initialStats,
  difficulty: 'medium',
  pitchInProgress: false,
  ballPosition: { x: 50, y: 50 },
  swingTiming: 0,
};

function gameReducer(state: GameState, action: GameAction): GameState {
  switch (action.type) {
    case 'START_PITCH':
      return {
        ...state,
        currentPitch: action.pitch,
        pitchInProgress: true,
        ballPosition: { x: 10, y: 50 },
        message: `${action.pitch.type.charAt(0).toUpperCase() + action.pitch.type.slice(1)} coming!`,
      };

    case 'SWING':
      return {
        ...state,
        isSwinging: true,
        swingTiming: action.timing,
      };

    case 'PITCH_RESULT':
      const newStats = { ...state.stats };
      let newBalls = state.balls;
      let newStrikes = state.strikes;
      let message = '';
      let gameOver = false;

      switch (action.result.type) {
        case 'ball':
          newBalls++;
          message = 'Ball!';
          if (newBalls >= 4) {
            newStats.walks++;
            message = 'Walk! Take your base.';
            newBalls = 0;
            newStrikes = 0;
          }
          break;

        case 'strike':
          newStrikes++;
          message = 'Strike!';
          if (newStrikes >= 3) {
            newStats.strikeouts++;
            newStats.atBats++;
            newStats.currentStreak = 0;
            message = 'Strike out! Better luck next time.';
            gameOver = true;
          }
          break;

        case 'foul':
          if (newStrikes < 2) {
            newStrikes++;
          }
          message = 'Foul ball!';
          break;

        case 'hit':
          newStats.hits++;
          newStats.atBats++;
          newStats.currentStreak++;
          if (newStats.currentStreak > newStats.bestStreak) {
            newStats.bestStreak = newStats.currentStreak;
          }
          message = 'Great hit! Base hit!';
          gameOver = true;
          break;

        case 'homerun':
          newStats.hits++;
          newStats.homeRuns++;
          newStats.atBats++;
          newStats.currentStreak++;
          if (newStats.currentStreak > newStats.bestStreak) {
            newStats.bestStreak = newStats.currentStreak;
          }
          message = 'HOME RUN! Amazing swing!';
          gameOver = true;
          break;
      }

      if (newStats.atBats > 0) {
        newStats.battingAverage = Number((newStats.hits / newStats.atBats).toFixed(3));
      }

      return {
        ...state,
        balls: gameOver ? 0 : newBalls,
        strikes: gameOver ? 0 : newStrikes,
        stats: newStats,
        message,
        gameOver,
        pitchInProgress: false,
        isSwinging: false,
      };

    case 'RESET_COUNT':
      return {
        ...state,
        balls: 0,
        strikes: 0,
        gameOver: false,
        message: 'New at-bat! Click "Start Pitching" to begin.',
        pitchInProgress: false,
        isSwinging: false,
      };

    case 'NEW_GAME':
      return {
        ...initialState,
        difficulty: state.difficulty,
        stats: initialStats,
      };

    case 'SET_DIFFICULTY':
      return {
        ...state,
        difficulty: action.difficulty,
        message: `Difficulty set to ${action.difficulty}. Good luck!`,
      };

    case 'UPDATE_BALL_POSITION':
      return {
        ...state,
        ballPosition: action.position,
      };

    default:
      return state;
  }
}

export const useGameState = () => {
  const [state, dispatch] = useReducer(gameReducer, initialState);

  const startPitch = useCallback(() => {
    const pitchTypes = ['fastball', 'curveball', 'slider', 'changeup'];
    const type = pitchTypes[Math.floor(Math.random() * pitchTypes.length)] as any;
    
    const difficultyModifier = {
      easy: { speedMin: 70, speedMax: 85, accuracy: 0.8 },
      medium: { speedMin: 80, speedMax: 95, accuracy: 0.6 },
      hard: { speedMin: 90, speedMax: 105, accuracy: 0.4 },
    };

    const { speedMin, speedMax, accuracy } = difficultyModifier[state.difficulty];
    const speed = Math.floor(Math.random() * (speedMax - speedMin + 1)) + speedMin;
    
    const pitch: Pitch = {
      type,
      speed,
      accuracy,
      movement: {
        x: (Math.random() - 0.5) * 20,
        y: (Math.random() - 0.5) * 20,
      },
    };

    dispatch({ type: 'START_PITCH', pitch });
  }, [state.difficulty]);

  const swing = useCallback((timing: number) => {
    if (!state.pitchInProgress || state.isSwinging) return;
    
    dispatch({ type: 'SWING', timing });
    
    // Simulate pitch result based on swing timing and pitch characteristics
    setTimeout(() => {
      const result = calculatePitchResult(state.currentPitch!, timing, state.difficulty);
      dispatch({ type: 'PITCH_RESULT', result });
    }, 500);
  }, [state.pitchInProgress, state.isSwinging, state.currentPitch, state.difficulty]);

  const resetCount = useCallback(() => {
    dispatch({ type: 'RESET_COUNT' });
  }, []);

  const newGame = useCallback(() => {
    dispatch({ type: 'NEW_GAME' });
  }, []);

  const setDifficulty = useCallback((difficulty: 'easy' | 'medium' | 'hard') => {
    dispatch({ type: 'SET_DIFFICULTY', difficulty });
  }, []);

  const updateBallPosition = useCallback((position: { x: number; y: number }) => {
    dispatch({ type: 'UPDATE_BALL_POSITION', position });
  }, []);

  return {
    state,
    startPitch,
    swing,
    resetCount,
    newGame,
    setDifficulty,
    updateBallPosition,
  };
};

function calculatePitchResult(pitch: Pitch, swingTiming: number, difficulty: string): PitchResult {
  const perfectTiming = 500; // milliseconds
  const timingDifference = Math.abs(swingTiming - perfectTiming);
  
  // Determine if it's in the strike zone (simplified)
  const isInStrikeZone = Math.random() < pitch.accuracy;
  
  if (!isInStrikeZone) {
    if (swingTiming === 0) {
      return { type: 'ball', speed: pitch.speed, location: { x: 0, y: 0 } };
    } else {
      return { type: 'strike', speed: pitch.speed, location: { x: 0, y: 0 } };
    }
  }

  if (swingTiming === 0) {
    return { type: 'strike', speed: pitch.speed, location: { x: 0, y: 0 } };
  }

  // Calculate hit quality based on timing
  const hitQuality = Math.max(0, 100 - timingDifference * 0.2);
  
  if (hitQuality > 85) {
    return Math.random() < 0.3 
      ? { type: 'homerun', speed: pitch.speed, location: { x: 0, y: 0 } }
      : { type: 'hit', speed: pitch.speed, location: { x: 0, y: 0 } };
  } else if (hitQuality > 50) {
    return { type: 'hit', speed: pitch.speed, location: { x: 0, y: 0 } };
  } else if (hitQuality > 20) {
    return { type: 'foul', speed: pitch.speed, location: { x: 0, y: 0 } };
  } else {
    return { type: 'strike', speed: pitch.speed, location: { x: 0, y: 0 } };
  }
}