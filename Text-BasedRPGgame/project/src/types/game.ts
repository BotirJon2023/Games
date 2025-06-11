export interface Character {
  id: string;
  name: string;
  class: CharacterClass;
  level: number;
  experience: number;
  experienceToNext: number;
  health: number;
  maxHealth: number;
  mana: number;
  maxMana: number;
  attack: number;
  defense: number;
  speed: number;
  gold: number;
  inventory: InventoryItem[];
  equipment: Equipment;
}

export interface CharacterClass {
  name: string;
  description: string;
  baseStats: {
    health: number;
    mana: number;
    attack: number;
    defense: number;
    speed: number;
  };
  abilities: Ability[];
}

export interface Ability {
  id: string;
  name: string;
  description: string;
  manaCost: number;
  damage?: number;
  healing?: number;
  effect?: string;
  cooldown: number;
}

export interface Enemy {
  id: string;
  name: string;
  level: number;
  health: number;
  maxHealth: number;
  attack: number;
  defense: number;
  speed: number;
  experience: number;
  gold: number;
  description: string;
}

export interface InventoryItem {
  id: string;
  name: string;
  type: 'weapon' | 'armor' | 'consumable' | 'misc';
  description: string;
  value: number;
  stats?: {
    attack?: number;
    defense?: number;
    health?: number;
    mana?: number;
  };
  quantity: number;
}

export interface Equipment {
  weapon?: InventoryItem;
  armor?: InventoryItem;
  accessory?: InventoryItem;
}

export interface GameState {
  character: Character | null;
  currentScene: string;
  chapter: number;
  storyProgress: number;
  inCombat: boolean;
  currentEnemy: Enemy | null;
  gameLog: string[];
}

export interface StoryScene {
  id: string;
  title: string;
  text: string;
  choices: Choice[];
  autoProgress?: boolean;
  enemy?: Enemy;
}

export interface Choice {
  text: string;
  action: string;
  requirement?: {
    stat: string;
    value: number;
  };
}