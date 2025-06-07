// Game screen types
export type GameScreen = 
  | 'start' 
  | 'boat-selection' 
  | 'tournament' 
  | 'race' 
  | 'results' 
  | 'leaderboard' 
  | 'customize'
  | 'track-editor';

// Boat related types
export interface BoatStats {
  speed: number;
  acceleration: number;
  handling: number;
  durability: number;
}

export interface Boat {
  id: string;
  name: string;
  image: string;
  stats: BoatStats;
  color: string;
  price?: number;
  unlocked?: boolean;
}

// Race related types
export type RaceStatus = 'preparing' | 'countdown' | 'racing' | 'finished';

export interface RaceResult {
  boatId: string;
  playerName: string;
  position: number;
  time: number;
  isPlayer: boolean;
}

export interface RaceTrack {
  id: string;
  name: string;
  length: number;
  difficulty: 'easy' | 'medium' | 'hard';
  obstacles: Obstacle[];
  background: string;
}

export interface Obstacle {
  type: 'rock' | 'buoy' | 'debris' | 'currentBoost' | 'currentSlow';
  position: { x: number; y: number };
  size: number;
}

// Tournament types
export type TournamentStage = 'qualifying' | 'quarterfinals' | 'semifinals' | 'finals';

export interface TournamentState {
  currentStage: TournamentStage;
  races: {
    qualifying: RaceResult[][];
    quarterfinals: RaceResult[][];
    semifinals: RaceResult[][];
    finals: RaceResult[];
  };
  playerQualified: boolean;
  currentRaceIndex: number;
}

// Weather types
export type WeatherType = 'sunny' | 'cloudy' | 'rainy' | 'stormy';

export interface Weather {
  type: WeatherType;
  windSpeed: number;
  windDirection: number; // 0-359 degrees
  visibility: number; // 0-100
  impact: {
    speedMultiplier: number;
    handlingMultiplier: number;
  };
}

// Player related types
export interface Player {
  name: string;
  selectedBoat: Boat;
  score: number;
  unlockedBoats: string[];
  achievements: Achievement[];
}

export interface Achievement {
  id: string;
  name: string;
  description: string;
  unlocked: boolean;
  icon: string;
}

// Animation states
export interface BoatAnimationState {
  position: { x: number; y: number };
  rotation: number;
  speed: number;
  wobble: number;
}