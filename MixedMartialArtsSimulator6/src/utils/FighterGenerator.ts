import { Fighter, FighterStats, WeightDivision } from '../types/Fighter';

const firstNames = [
  'Anderson', 'Jon', 'Georges', 'Conor', 'Khabib', 'Daniel', 'Israel', 'Francis',
  'Amanda', 'Valentina', 'Rose', 'Joanna', 'Holly', 'Miesha', 'Ronda', 'Cyborg',
  'Jose', 'Max', 'Dustin', 'Tony', 'Charles', 'Justin', 'Colby', 'Kamaru',
  'Robert', 'Yoel', 'Paulo', 'Jared', 'Derek', 'Anthony', 'Stipe', 'Junior',
  'Alexander', 'Petr', 'Aljamain', 'Sean', 'Marlon', 'Cory', 'Pedro', 'Raphael'
];

const lastNames = [
  'Silva', 'Jones', 'St-Pierre', 'McGregor', 'Nurmagomedov', 'Cormier', 'Adesanya', 'Ngannou',
  'Nunes', 'Shevchenko', 'Namajunas', 'Jedrzejczyk', 'Holm', 'Tate', 'Rousey', 'Santos',
  'Aldo', 'Holloway', 'Poirier', 'Ferguson', 'Oliveira', 'Gaethje', 'Covington', 'Usman',
  'Whittaker', 'Romero', 'Costa', 'Cannonier', 'Brunson', 'Smith', 'Miocic', 'Santos',
  'Volkanovski', 'Yan', 'Sterling', 'OMalley', 'Moraes', 'Sandhagen', 'Munhoz', 'Assuncao'
];

const nicknames = [
  'The Spider', 'Bones', 'Rush', 'The Notorious', 'The Eagle', 'DC', 'The Last Stylebender',
  'The Predator', 'The Lioness', 'Bullet', 'Thug Rose', 'Joanna Violence', 'The Preacher\'s Daughter',
  'Cupcake', 'Rowdy', 'Cyborg', 'Scarface', 'Blessed', 'The Diamond', 'El Cucuy',
  'Do Bronx', 'The Highlight', 'Chaos', 'The Nigerian Nightmare', 'The Reaper', 'Soldier of God',
  'The Eraser', 'The Killa Gorilla', 'Action Man', 'Lionheart', 'DC', 'Cigano',
  'The Great', 'No Mercy', 'Funk Master', 'Sugar', 'Magic', 'The Sandman', 'Young Punisher', 'Pitbull'
];

const fightingStyles = ['Striker', 'Grappler', 'Wrestler', 'All-Around'] as const;
const stances = ['Orthodox', 'Southpaw', 'Switch'] as const;

const weightDivisions: Record<WeightDivision, { min: number; max: number }> = {
  'Flyweight': { min: 115, max: 125 },
  'Bantamweight': { min: 125, max: 135 },
  'Featherweight': { min: 135, max: 145 },
  'Lightweight': { min: 145, max: 155 },
  'Welterweight': { min: 155, max: 170 },
  'Middleweight': { min: 170, max: 185 },
  'Light Heavyweight': { min: 185, max: 205 },
  'Heavyweight': { min: 205, max: 265 }
};

export class FighterGenerator {
  private static getRandomElement<T>(array: T[]): T {
    return array[Math.floor(Math.random() * array.length)];
  }

  private static generateStats(style: string, experience: number): FighterStats {
    const baseStats = {
      striking: Math.floor(Math.random() * 40) + 40,
      grappling: Math.floor(Math.random() * 40) + 40,
      wrestling: Math.floor(Math.random() * 40) + 40,
      cardio: Math.floor(Math.random() * 40) + 40,
      power: Math.floor(Math.random() * 40) + 40,
      speed: Math.floor(Math.random() * 40) + 40,
      defense: Math.floor(Math.random() * 40) + 40,
      chin: Math.floor(Math.random() * 40) + 40
    };

    // Adjust stats based on fighting style
    switch (style) {
      case 'Striker':
        baseStats.striking += 15;
        baseStats.power += 10;
        baseStats.speed += 10;
        break;
      case 'Grappler':
        baseStats.grappling += 15;
        baseStats.wrestling += 10;
        baseStats.cardio += 10;
        break;
      case 'Wrestler':
        baseStats.wrestling += 15;
        baseStats.cardio += 10;
        baseStats.defense += 10;
        break;
      case 'All-Around':
        // Balanced fighter gets small bonuses across the board
        Object.keys(baseStats).forEach(key => {
          baseStats[key as keyof FighterStats] += 5;
        });
        break;
    }

    // Apply experience bonuses
    const experienceBonus = Math.floor(experience / 5);
    Object.keys(baseStats).forEach(key => {
      baseStats[key as keyof FighterStats] = Math.min(
        100,
        baseStats[key as keyof FighterStats] + experienceBonus
      );
    });

    return baseStats;
  }

  static generateFighter(division: WeightDivision, customName?: string): Fighter {
    const weightRange = weightDivisions[division];
    const weight = Math.floor(Math.random() * (weightRange.max - weightRange.min)) + weightRange.min;
    const height = Math.floor(Math.random() * 20) + (division === 'Heavyweight' ? 72 : 65);
    const reach = height + Math.floor(Math.random() * 10) - 5;
    const age = Math.floor(Math.random() * 15) + 20;
    const experience = Math.floor(Math.random() * 25) + 5;
    const fightingStyle = this.getRandomElement(fightingStyles);
    
    const wins = Math.floor(Math.random() * experience) + 5;
    const losses = Math.floor(Math.random() * Math.max(1, wins / 3));
    const draws = Math.floor(Math.random() * 3);
    const koTko = Math.floor(wins * 0.4 * Math.random());
    const submissions = Math.floor(wins * 0.3 * Math.random());

    const stats = this.generateStats(fightingStyle, experience);
    const maxHealth = 100 + Math.floor(stats.chin / 2);
    const maxStamina = 100 + Math.floor(stats.cardio / 2);

    return {
      id: `fighter_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`,
      name: customName || `${this.getRandomElement(firstNames)} ${this.getRandomElement(lastNames)}`,
      nickname: this.getRandomElement(nicknames),
      age,
      weight,
      height,
      reach,
      record: { wins, losses, draws, koTko, submissions },
      stats,
      stance: this.getRandomElement(stances),
      fightingStyle,
      experience,
      ranking: Math.floor(Math.random() * 15) + 1,
      health: maxHealth,
      stamina: maxStamina,
      maxHealth,
      maxStamina,
      isActive: true,
      sponsors: [],
      earnings: Math.floor(Math.random() * 1000000) + 50000,
      trainingCamp: undefined
    };
  }

  static generateFighterRoster(division: WeightDivision, count: number = 16): Fighter[] {
    const fighters: Fighter[] = [];
    const usedNames = new Set<string>();

    for (let i = 0; i < count; i++) {
      let fighter: Fighter;
      let attempts = 0;

      do {
        fighter = this.generateFighter(division);
        attempts++;
      } while (usedNames.has(fighter.name) && attempts < 50);

      usedNames.add(fighter.name);
      fighters.push(fighter);
    }

    // Sort by ranking
    return fighters.sort((a, b) => a.ranking - b.ranking);
  }
}