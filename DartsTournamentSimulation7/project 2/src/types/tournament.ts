export interface Player {
  id: number;
  name: string;
  skill: number;        // 0-1, overall skill level
  accuracy: number;     // 0-1, how accurate throws are
  consistency: number;  // 0-1, how consistent performance is
  wins: number;
  losses: number;
  totalScore: number;
  averageScore: number;
  bullseyes: number;
  trebles: number;
  doubles: number;
}

export interface Match {
  id: string;
  player1: Player;
  player2: Player;
  winner: Player | null;
  status: 'pending' | 'active' | 'completed';
  score1: number;
  score2: number;
  throws?: DartThrow[];
}

export interface Round {
  id: number;
  matches: Match[];
  winners: Player[];
  completed: boolean;
}

export interface Tournament {
  id: number;
  players: Player[];
  rounds: Round[];
  currentRound: number;
  winner: Player | null;
  status: 'setup' | 'active' | 'completed';
}

export interface DartThrow {
  x: number;
  y: number;
  score: number;
  multiplier: number;
  section: string;
  player: Player;
  timestamp: number;
}

export interface DartboardPosition {
  x: number;
  y: number;
  score: number;
  multiplier: number;
  section: string;
}

export type GameState = 'setup' | 'tournament' | 'playing' | 'finished';

export interface GameSettings {
  startingScore: number;
  gameMode: '301' | '501' | '701';
  doubleOut: boolean;
  dartsPerTurn: number;
}