export interface Player {
  id: number;
  name: string;
  position: string;
  speed: number;
  strength: number;
  skill: number;
  stamina: number;
  currentStamina: number;
  x: number;
  y: number;
  hasBall: boolean;
  team: 'home' | 'away';
}

export interface Team {
  id: string;
  name: string;
  color: string;
  players: Player[];
  score: number;
}

export interface GameState {
  homeTeam: Team;
  awayTeam: Team;
  currentHalf: 1 | 2;
  timeRemaining: number;
  possession: 'home' | 'away';
  ballPosition: { x: number; y: number };
  isPlaying: boolean;
  gameLog: GameEvent[];
  phase: 'kickoff' | 'play' | 'scrum' | 'lineout' | 'conversion' | 'halftime' | 'fulltime';
}

export interface GameEvent {
  id: number;
  type: 'try' | 'conversion' | 'penalty' | 'turnover' | 'tackle' | 'pass' | 'kick';
  team: 'home' | 'away';
  player?: string;
  message: string;
  timestamp: number;
}

export interface FieldDimensions {
  width: number;
  height: number;
  tryLineHome: number;
  tryLineAway: number;
  centerLine: number;
}