import { Team, Player } from '../types/game';

const PLAYER_NAMES = [
  'Smith', 'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis',
  'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson', 'Anderson',
  'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee', 'Perez', 'Thompson',
  'White', 'Harris', 'Sanchez', 'Clark', 'Ramirez', 'Lewis', 'Robinson'
];

const POSITIONS = [
  'Scrum-half', 'Fly-half', 'Centre', 'Centre', 'Winger', 'Winger', 'Fullback'
];

export function generateTeam(name: string, color: string, isHome: boolean): Team {
  const players: Player[] = [];
  
  for (let i = 0; i < 7; i++) {
    const playerName = PLAYER_NAMES[Math.floor(Math.random() * PLAYER_NAMES.length)];
    const position = POSITIONS[i];
    
    players.push({
      id: i,
      name: `${playerName} ${i + 1}`,
      position,
      speed: 70 + Math.random() * 30,
      strength: 70 + Math.random() * 30,
      skill: 70 + Math.random() * 30,
      stamina: 80 + Math.random() * 20,
      currentStamina: 100,
      x: 0,
      y: 0,
      hasBall: false,
      team: isHome ? 'home' : 'away'
    });
  }

  return {
    id: isHome ? 'home' : 'away',
    name,
    color,
    players,
    score: 0
  };
}

export const DEFAULT_TEAMS = {
  home: generateTeam('Lightning Bolts', '#3B82F6', true),
  away: generateTeam('Thunder Hawks', '#EF4444', false)
};