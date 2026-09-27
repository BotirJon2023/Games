export type Screen = 'menu' | 'racing' | 'results';
export type GameMode = 'p2' | 'cpu';

export interface RaceResult {
  winnerName: string;
  winnerLane: number;
  winnerTime: number;
  loserName: string;
  loserLane: number;
  loserTime: number;
  mode: GameMode;
}

export interface CarConfig {
  name: string;
  bodyColor: string;
  glowColor: string;
  accentColor: string;
}

export const CARS: CarConfig[] = [
  { name: 'CYBER', bodyColor: '#00f0ff', glowColor: 'rgba(0,240,255,0.7)', accentColor: '#0099bb' },
  { name: 'INFERNO', bodyColor: '#ff2bd6', glowColor: 'rgba(255,43,214,0.7)', accentColor: '#aa1a8e' },
  { name: 'VENOM', bodyColor: '#0fff8b', glowColor: 'rgba(15,255,139,0.7)', accentColor: '#0aaa55' },
  { name: 'SOLAR', bodyColor: '#ffe600', glowColor: 'rgba(255,230,0,0.7)', accentColor: '#aa9900' },
  { name: 'BLAZE', bodyColor: '#ff6b1a', glowColor: 'rgba(255,107,26,0.7)', accentColor: '#aa4400' },
];

export const TRACK_LENGTH = 3500;
export const NUM_LANES = 4;

export interface Obstacle {
  position: number;
  lane: number;
  hit: boolean;
}
