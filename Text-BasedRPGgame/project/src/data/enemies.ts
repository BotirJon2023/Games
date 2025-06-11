import { Enemy } from '../types/game';

export const enemies: Enemy[] = [
  {
    id: 'goblin',
    name: 'Goblin Warrior',
    level: 1,
    health: 60,
    maxHealth: 60,
    attack: 12,
    defense: 5,
    speed: 10,
    experience: 25,
    gold: 15,
    description: 'A small but fierce goblin wielding a rusty blade'
  },
  {
    id: 'orc',
    name: 'Orc Berserker',
    level: 3,
    health: 100,
    maxHealth: 100,
    attack: 20,
    defense: 12,
    speed: 8,
    experience: 75,
    gold: 40,
    description: 'A massive orc with bulging muscles and bloodshot eyes'
  },
  {
    id: 'skeleton',
    name: 'Undead Skeleton',
    level: 2,
    health: 80,
    maxHealth: 80,
    attack: 15,
    defense: 8,
    speed: 6,
    experience: 50,
    gold: 25,
    description: 'Ancient bones held together by dark magic'
  },
  {
    id: 'dragon',
    name: 'Ancient Dragon',
    level: 10,
    health: 300,
    maxHealth: 300,
    attack: 45,
    defense: 25,
    speed: 15,
    experience: 500,
    gold: 1000,
    description: 'A legendary wyrm with scales like obsidian and eyes like molten gold'
  },
  {
    id: 'troll',
    name: 'Forest Troll',
    level: 5,
    health: 150,
    maxHealth: 150,
    attack: 25,
    defense: 18,
    speed: 5,
    experience: 125,
    gold: 75,
    description: 'A hulking beast covered in moss and wielding a massive club'
  }
];