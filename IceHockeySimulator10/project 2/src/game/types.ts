export interface Vec2 {
  x: number;
  y: number;
}

export type TeamIndex = 0 | 1;
export type PlayerRole = 'goalie' | 'defender' | 'forward';
export type GameMode = 'menu' | 'playing' | 'goal_scored' | 'period_end' | 'game_over';
export type PlayerMode = 'two_player' | 'vs_computer';

export interface Player {
  id: number;
  team: TeamIndex;
  role: PlayerRole;
  pos: Vec2;
  vel: Vec2;
  facingAngle: number;
  hasPuck: boolean;
  isSelected: boolean;
  number: number;
  shootCooldown: number;
  staminaCooldown: number;
}

export interface Puck {
  pos: Vec2;
  vel: Vec2;
  controlledBy: number | null;
}

export interface GoalPost {
  x: number;
  y: number;
  width: number;
  height: number;
  defendingTeam: TeamIndex;
}

export interface GameState {
  players: Player[];
  puck: Puck;
  scores: [number, number];
  timeRemaining: number;
  period: number;
  totalPeriods: number;
  mode: GameMode;
  playerMode: PlayerMode;
  selectedIdx: [number, number];
  goalCooldown: number;
  lastScoringTeam: TeamIndex | null;
  periodEndCooldown: number;
}
