import { Player } from '../types/game';

const firstNames = [
  'Alex', 'Marcus', 'David', 'James', 'Michael', 'Robert', 'John', 'Daniel',
  'Carlos', 'Luis', 'Diego', 'Pablo', 'Antonio', 'Jose', 'Manuel', 'Rafael',
  'Pierre', 'Jean', 'Antoine', 'Olivier', 'Thomas', 'Nicolas', 'Julien',
  'Marco', 'Andrea', 'Francesco', 'Giovanni', 'Alessandro', 'Matteo',
  'Kevin', 'Ryan', 'Connor', 'Sean', 'Patrick', 'Brian', 'Tyler', 'Jake'
];

const lastNames = [
  'Johnson', 'Williams', 'Brown', 'Jones', 'Garcia', 'Miller', 'Davis',
  'Rodriguez', 'Martinez', 'Hernandez', 'Lopez', 'Gonzalez', 'Wilson',
  'Anderson', 'Thomas', 'Taylor', 'Moore', 'Jackson', 'Martin', 'Lee',
  'Perez', 'Thompson', 'White', 'Harris', 'Sanchez', 'Clark', 'Ramirez',
  'Lewis', 'Robinson', 'Walker', 'Young', 'Allen', 'King', 'Wright',
  'Scott', 'Torres', 'Nguyen', 'Hill', 'Flores', 'Green', 'Adams'
];

export function generateRandomPlayer(position: Player['position']): Player {
  const firstName = firstNames[Math.floor(Math.random() * firstNames.length)];
  const lastName = lastNames[Math.floor(Math.random() * lastNames.length)];
  
  // Position-based stat generation
  const baseStats = getPositionBaseStats(position);
  const age = 18 + Math.floor(Math.random() * 17); // 18-34 years old
  
  // Age affects stats
  const ageMultiplier = age < 25 ? 0.8 + (age - 18) * 0.02 : 
                       age < 30 ? 1.0 : 
                       1.0 - (age - 30) * 0.03;

  const stats = {
    speed: Math.max(20, Math.min(99, Math.floor(baseStats.speed * ageMultiplier * (0.8 + Math.random() * 0.4)))),
    strength: Math.max(20, Math.min(99, Math.floor(baseStats.strength * ageMultiplier * (0.8 + Math.random() * 0.4)))),
    skill: Math.max(20, Math.min(99, Math.floor(baseStats.skill * ageMultiplier * (0.8 + Math.random() * 0.4)))),
    stamina: Math.max(20, Math.min(99, Math.floor(baseStats.stamina * ageMultiplier * (0.8 + Math.random() * 0.4)))),
    experience: Math.max(10, Math.min(99, Math.floor((age - 18) * 4 + Math.random() * 20)))
  };

  const overallRating = (stats.speed + stats.strength + stats.skill + stats.stamina + stats.experience) / 5;
  const value = Math.floor(overallRating * 100000 * (1 + Math.random() * 0.5));

  return {
    id: `player_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
    name: `${firstName} ${lastName}`,
    position,
    stats,
    form: 70 + Math.floor(Math.random() * 30),
    energy: 90 + Math.floor(Math.random() * 10),
    age,
    value,
    injured: Math.random() < 0.05 // 5% chance of injury
  };
}

function getPositionBaseStats(position: Player['position']) {
  switch (position) {
    case 'GK':
      return { speed: 45, strength: 75, skill: 85, stamina: 70 };
    case 'DEF':
      return { speed: 65, strength: 85, skill: 70, stamina: 80 };
    case 'MID':
      return { speed: 75, strength: 70, skill: 85, stamina: 90 };
    case 'FWD':
      return { speed: 85, strength: 70, skill: 80, stamina: 75 };
    default:
      return { speed: 70, strength: 70, skill: 70, stamina: 70 };
  }
}

export function generateTeamPlayers(): Player[] {
  const players: Player[] = [];
  
  // Generate goalkeepers (2)
  for (let i = 0; i < 2; i++) {
    players.push(generateRandomPlayer('GK'));
  }
  
  // Generate defenders (6)
  for (let i = 0; i < 6; i++) {
    players.push(generateRandomPlayer('DEF'));
  }
  
  // Generate midfielders (6)
  for (let i = 0; i < 6; i++) {
    players.push(generateRandomPlayer('MID'));
  }
  
  // Generate forwards (4)
  for (let i = 0; i < 4; i++) {
    players.push(generateRandomPlayer('FWD'));
  }
  
  return players;
}