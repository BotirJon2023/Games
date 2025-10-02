export interface Player {
  id: string;
  name: string;
  position: 'GK' | 'DEF' | 'MID' | 'FWD';
  stats: {
    speed: number;
    strength: number;
    skill: number;
    stamina: number;
    experience: number;
  };
  form: number; // 0-100
  energy: number; // 0-100
  age: number;
  value: number;
  injured: boolean;
}

export interface Team {
  id: string;
  name: string;
  players: Player[];
  formation: string;
  tactics: 'Attacking' | 'Defensive' | 'Balanced';
  morale: number;
  budget: number;
  wins: number;
  draws: number;
  losses: number;
}

export interface Match {
  id: string;
  homeTeam: Team;
  awayTeam: Team;
  homeScore: number;
  awayScore: number;
  events: MatchEvent[];
  minute: number;
  status: 'upcoming' | 'live' | 'finished';
}

export interface MatchEvent {
  minute: number;
  type: 'goal' | 'yellow_card' | 'red_card' | 'substitution' | 'injury';
  player: string;
  team: 'home' | 'away';
  description: string;
}

export interface AIDecision {
  type: 'formation_change' | 'substitution' | 'tactical_change';
  reason: string;
  confidence: number;
  data: any;
}