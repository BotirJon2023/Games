export interface Position {
  x: number;
  y: number;
}

export interface Fencer {
  id: string;
  name: string;
  position: Position;
  health: number;
  maxHealth: number;
  score: number;
  isAttacking: boolean;
  isParrying: boolean;
  isStunned: boolean;
  stunDuration: number;
  facing: 'left' | 'right';
  style: FencingStyle;
  aiDifficulty?: 'easy' | 'medium' | 'hard';
  sprite: SpriteState;
}

export interface SpriteState {
  currentFrame: number;
  animationState: 'idle' | 'attack' | 'parry' | 'hit' | 'victory';
  frameTimer: number;
}

export interface FencingStyle {
  name: string;
  attackSpeed: number;
  parrySpeed: number;
  reach: number;
  damage: number;
  color: string;
}

export interface GameState {
  players: Fencer[];
  currentRound: number;
  timeRemaining: number;
  gameStatus: 'menu' | 'playing' | 'paused' | 'finished' | 'tournament';
  winner?: string;
  tournament?: Tournament;
  particles: Particle[];
  combatLog: CombatEvent[];
}

export interface Tournament {
  participants: string[];
  bracket: TournamentMatch[];
  currentMatch: number;
  champion?: string;
}

export interface TournamentMatch {
  player1: string;
  player2: string;
  winner?: string;
  round: number;
}

export interface Particle {
  id: string;
  position: Position;
  velocity: Position;
  life: number;
  maxLife: number;
  color: string;
  size: number;
}

export interface CombatEvent {
  timestamp: number;
  attacker: string;
  defender: string;
  action: 'attack' | 'parry' | 'hit' | 'miss';
  damage?: number;
}

export interface GameConfig {
  canvasWidth: number;
  canvasHeight: number;
  roundDuration: number;
  winningScore: number;
  fencingStyles: FencingStyle[];
}