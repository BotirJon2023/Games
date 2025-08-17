export interface GameStats {
  hits: number;
  atBats: number;
  homeRuns: number;
  strikeouts: number;
  walks: number;
  battingAverage: number;
  currentStreak: number;
  bestStreak: number;
}

export interface PitchResult {
  type: 'ball' | 'strike' | 'hit' | 'homerun' | 'foul';
  speed: number;
  location: { x: number; y: number };
}

export interface Pitch {
  type: 'fastball' | 'curveball' | 'slider' | 'changeup';
  speed: number;
  accuracy: number;
  movement: { x: number; y: number };
}

export interface GameState {
  balls: number;
  strikes: number;
  currentPitch?: Pitch;
  isSwinging: boolean;
  gameOver: boolean;
  message: string;
  stats: GameStats;
  difficulty: 'easy' | 'medium' | 'hard';
  pitchInProgress: boolean;
  ballPosition: { x: number; y: number };
  swingTiming: number;
}

export type GameAction = 
  | { type: 'START_PITCH'; pitch: Pitch }
  | { type: 'SWING'; timing: number }
  | { type: 'PITCH_RESULT'; result: PitchResult }
  | { type: 'RESET_COUNT' }
  | { type: 'NEW_GAME' }
  | { type: 'SET_DIFFICULTY'; difficulty: 'easy' | 'medium' | 'hard' }
  | { type: 'UPDATE_BALL_POSITION'; position: { x: number; y: number } };