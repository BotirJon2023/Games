import { CharacterClass } from '../types/game';

export const characterClasses: CharacterClass[] = [
  {
    name: 'Warrior',
    description: 'A mighty fighter with exceptional strength and defensive capabilities. Masters of melee combat.',
    baseStats: {
      health: 120,
      mana: 40,
      attack: 18,
      defense: 15,
      speed: 8
    },
    abilities: [
      {
        id: 'power_strike',
        name: 'Power Strike',
        description: 'A devastating attack that deals 150% damage',
        manaCost: 10,
        damage: 25,
        cooldown: 2
      },
      {
        id: 'defensive_stance',
        name: 'Defensive Stance',
        description: 'Reduce incoming damage by 50% for 3 turns',
        manaCost: 15,
        effect: 'defense_boost',
        cooldown: 4
      }
    ]
  },
  {
    name: 'Mage',
    description: 'A master of arcane arts with powerful spells and magical abilities. Weak in physical combat but devastating with magic.',
    baseStats: {
      health: 80,
      mana: 120,
      attack: 10,
      defense: 8,
      speed: 12
    },
    abilities: [
      {
        id: 'fireball',
        name: 'Fireball',
        description: 'Launch a blazing fireball dealing significant magic damage',
        manaCost: 20,
        damage: 35,
        cooldown: 1
      },
      {
        id: 'heal',
        name: 'Heal',
        description: 'Restore health using magical energy',
        manaCost: 25,
        healing: 40,
        cooldown: 3
      }
    ]
  },
  {
    name: 'Rogue',
    description: 'A swift and cunning assassin who strikes from the shadows. High speed and critical hit chance.',
    baseStats: {
      health: 90,
      mana: 70,
      attack: 15,
      defense: 10,
      speed: 18
    },
    abilities: [
      {
        id: 'backstab',
        name: 'Backstab',
        description: 'A precise strike with high critical hit chance',
        manaCost: 12,
        damage: 20,
        effect: 'critical_chance',
        cooldown: 2
      },
      {
        id: 'shadow_step',
        name: 'Shadow Step',
        description: 'Become untargetable for one turn',
        manaCost: 18,
        effect: 'dodge',
        cooldown: 5
      }
    ]
  }
];