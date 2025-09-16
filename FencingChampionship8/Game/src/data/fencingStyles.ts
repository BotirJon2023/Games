import { FencingStyle } from '../types/game';

export const FENCING_STYLES: FencingStyle[] = [
  {
    name: 'Foil',
    attackSpeed: 1.2,
    parrySpeed: 1.0,
    reach: 80,
    damage: 1,
    color: '#3B82F6'
  },
  {
    name: 'Épée',
    attackSpeed: 0.9,
    parrySpeed: 0.8,
    reach: 85,
    damage: 1,
    color: '#EF4444'
  },
  {
    name: 'Sabre',
    attackSpeed: 1.4,
    parrySpeed: 1.1,
    reach: 75,
    damage: 1,
    color: '#10B981'
  },
  {
    name: 'Master',
    attackSpeed: 1.6,
    parrySpeed: 1.3,
    reach: 90,
    damage: 1,
    color: '#F59E0B'
  }
];

export const FAMOUS_FENCERS = [
  'Alessandro Zanardi',
  'Valentina Vezzali',
  'Mariel Zagunis',
  'Philippe Boisse',
  'Laura Flessel',
  'Aldo Nadi',
  'Ramon Fonst',
  'Helene Mayer',
  'Christian d\'Oriola',
  'Edoardo Mangiarotti'
];