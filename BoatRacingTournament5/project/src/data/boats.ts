import { Boat } from '../types/game';
import { Anchor, Sailboat, Ship, PackageSearch as Yacht } from 'lucide-react';

export const boatIcons = {
  Anchor,
  Sailboat,
  Ship,
  Yacht,
};

export const boats: Boat[] = [
  {
    id: 'speedster',
    name: 'Speedster',
    image: 'Yacht',
    stats: {
      speed: 9,
      acceleration: 8,
      handling: 7,
      durability: 5
    },
    color: '#ef4444', // red
    unlocked: true
  },
  {
    id: 'cruiser',
    name: 'Cruiser',
    image: 'Ship',
    stats: {
      speed: 6,
      acceleration: 5,
      handling: 8,
      durability: 9
    },
    color: '#3b82f6', // blue
    unlocked: true
  },
  {
    id: 'agile',
    name: 'Agility Pro',
    image: 'Sailboat',
    stats: {
      speed: 7,
      acceleration: 7,
      handling: 9,
      durability: 6
    },
    color: '#10b981', // green
    unlocked: true
  },
  {
    id: 'balanced',
    name: 'Balanced',
    image: 'Anchor',
    stats: {
      speed: 7,
      acceleration: 7,
      handling: 7,
      durability: 7
    },
    color: '#f59e0b', // amber
    unlocked: true
  },
  {
    id: 'performer',
    name: 'High Performer',
    image: 'Yacht',
    stats: {
      speed: 10,
      acceleration: 9,
      handling: 6,
      durability: 4
    },
    color: '#8b5cf6', // purple
    price: 1000,
    unlocked: false
  },
  {
    id: 'titan',
    name: 'Titan',
    image: 'Ship',
    stats: {
      speed: 5,
      acceleration: 4,
      handling: 6,
      durability: 10
    },
    color: '#64748b', // slate
    price: 800,
    unlocked: false
  },
  {
    id: 'phantom',
    name: 'Phantom',
    image: 'Sailboat',
    stats: {
      speed: 8,
      acceleration: 10,
      handling: 8,
      durability: 3
    },
    color: '#ec4899', // pink
    price: 1200,
    unlocked: false
  },
  {
    id: 'allrounder',
    name: 'All-Rounder Pro',
    image: 'Anchor',
    stats: {
      speed: 8,
      acceleration: 8,
      handling: 8,
      durability: 8
    },
    color: '#0ea5e9', // sky
    price: 1500,
    unlocked: false
  }
];

export const getBoatById = (id: string): Boat | undefined => {
  return boats.find(boat => boat.id === id);
};

export const getUnlockedBoats = (): Boat[] => {
  return boats.filter(boat => boat.unlocked);
};

export const getLockedBoats = (): Boat[] => {
  return boats.filter(boat => !boat.unlocked);
};

export const calculateBoatPerformance = (boat: Boat): number => {
  // Calculate overall performance based on stats
  const { speed, acceleration, handling, durability } = boat.stats;
  // Different weights for different stats
  return (speed * 0.4) + (acceleration * 0.3) + (handling * 0.2) + (durability * 0.1);
};

export const getBoatIcon = (iconName: string) => {
  const IconComponent = boatIcons[iconName as keyof typeof boatIcons];
  if (!IconComponent) {
    return boatIcons.Sailboat;
  }
  return IconComponent;
};