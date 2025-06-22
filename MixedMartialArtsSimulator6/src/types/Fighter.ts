export interface FighterStats {
  striking: number;
  grappling: number;
  wrestling: number;
  cardio: number;
  power: number;
  speed: number;
  defense: number;
  chin: number;
}

export interface Fighter {
  id: string;
  name: string;
  nickname: string;
  age: number;
  weight: number;
  height: number;
  reach: number;
  record: {
    wins: number;
    losses: number;
    draws: number;
    koTko: number;
    submissions: number;
  };
  stats: FighterStats;
  stance: 'Orthodox' | 'Southpaw' | 'Switch';
  fightingStyle: 'Striker' | 'Grappler' | 'Wrestler' | 'All-Around';
  experience: number;
  ranking: number;
  health: number;
  stamina: number;
  maxHealth: number;
  maxStamina: number;
  isActive: boolean;
  trainingCamp?: string;
  sponsors: string[];
  earnings: number;
}

export interface FightResult {
  winner: Fighter;
  loser: Fighter;
  method: 'KO/TKO' | 'Submission' | 'Decision' | 'Disqualification';
  round: number;
  time: string;
  details: string;
}

export interface Tournament {
  id: string;
  name: string;
  division: WeightDivision;
  fighters: Fighter[];
  bracket: TournamentBracket;
  currentRound: number;
  isComplete: boolean;
  winner?: Fighter;
}

export interface TournamentBracket {
  rounds: Fighter[][][];
  results: FightResult[];
}

export type WeightDivision = 
  | 'Flyweight' 
  | 'Bantamweight' 
  | 'Featherweight' 
  | 'Lightweight' 
  | 'Welterweight' 
  | 'Middleweight' 
  | 'Light Heavyweight' 
  | 'Heavyweight';

export interface CombatAction {
  type: 'strike' | 'grapple' | 'defend' | 'rest';
  target: 'head' | 'body' | 'legs' | 'ground';
  technique: string;
  damage: number;
  staminaCost: number;
  success: boolean;
  critical: boolean;
}