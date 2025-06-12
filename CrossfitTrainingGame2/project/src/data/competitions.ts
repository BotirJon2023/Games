import { Competition } from '../types/game';

export const competitions: Competition[] = [
  {
    id: 'murph',
    name: 'Murph',
    type: 'FOR_TIME',
    description: 'Complete for time: 1 mile run, 100 pull-ups, 200 push-ups, 300 air squats, 1 mile run',
    duration: 3600, // 60 minutes
    difficulty: 'Advanced',
    exercises: [
      { id: 'run1', name: '1 Mile Run', description: 'Start with a mile run', reps: 1, icon: 'Activity' },
      { id: 'pullups', name: 'Pull-ups', description: 'Dead hang pull-ups', reps: 100, icon: 'ArrowUp' },
      { id: 'pushups', name: 'Push-ups', description: 'Standard push-ups', reps: 200, icon: 'Minus' },
      { id: 'squats', name: 'Air Squats', description: 'Bodyweight squats', reps: 300, icon: 'ArrowDown' },
      { id: 'run2', name: '1 Mile Run', description: 'Finish with a mile run', reps: 1, icon: 'Activity' }
    ]
  },
  {
    id: 'fran',
    name: 'Fran',
    type: 'FOR_TIME',
    description: '21-15-9 Thrusters (95/65 lb) and Pull-ups',
    duration: 900, // 15 minutes
    difficulty: 'Intermediate',
    exercises: [
      { id: 'thrusters', name: 'Thrusters', description: 'Barbell thrusters', reps: 45, weight: 95, icon: 'ArrowUp' },
      { id: 'pullups', name: 'Pull-ups', description: 'Dead hang pull-ups', reps: 45, icon: 'ArrowUp' }
    ]
  },
  {
    id: 'cindy',
    name: 'Cindy',
    type: 'AMRAP',
    description: 'AMRAP 20: 5 Pull-ups, 10 Push-ups, 15 Air Squats',
    duration: 1200, // 20 minutes
    difficulty: 'Beginner',
    exercises: [
      { id: 'pullups', name: 'Pull-ups', description: 'Dead hang pull-ups', reps: 5, icon: 'ArrowUp' },
      { id: 'pushups', name: 'Push-ups', description: 'Standard push-ups', reps: 10, icon: 'Minus' },
      { id: 'squats', name: 'Air Squats', description: 'Bodyweight squats', reps: 15, icon: 'ArrowDown' }
    ]
  },
  {
    id: 'annie',
    name: 'Annie',
    type: 'FOR_TIME',
    description: '50-40-30-20-10 Double Unders and Sit-ups',
    duration: 1200, // 20 minutes
    difficulty: 'Intermediate',
    exercises: [
      { id: 'doubleunders', name: 'Double Unders', description: 'Jump rope double unders', reps: 150, icon: 'RotateCcw' },
      { id: 'situps', name: 'Sit-ups', description: 'Abmat sit-ups', reps: 150, icon: 'Circle' }
    ]
  },
  {
    id: 'tabata-power',
    name: 'Tabata Power',
    type: 'TABATA',
    description: '8 rounds of 20s work, 10s rest: Burpees, Mountain Climbers, Jump Squats, Push-ups',
    duration: 960, // 16 minutes
    difficulty: 'Advanced',
    exercises: [
      { id: 'burpees', name: 'Burpees', description: 'Full burpees', duration: 20, icon: 'Zap' },
      { id: 'mountainclimbers', name: 'Mountain Climbers', description: 'Fast mountain climbers', duration: 20, icon: 'Mountain' },
      { id: 'jumpsquats', name: 'Jump Squats', description: 'Explosive jump squats', duration: 20, icon: 'ArrowUp' },
      { id: 'pushups', name: 'Push-ups', description: 'Standard push-ups', duration: 20, icon: 'Minus' }
    ]
  }
];