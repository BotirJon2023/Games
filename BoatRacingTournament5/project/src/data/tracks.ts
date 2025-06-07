import { RaceTrack } from '../types/game';

export const tracks: RaceTrack[] = [
  {
    id: 'harbor-sprint',
    name: 'Harbor Sprint',
    length: 1000,
    difficulty: 'easy',
    obstacles: [
      { type: 'buoy', position: { x: 200, y: 50 }, size: 20 },
      { type: 'buoy', position: { x: 400, y: 150 }, size: 20 },
      { type: 'buoy', position: { x: 600, y: 50 }, size: 20 },
      { type: 'buoy', position: { x: 800, y: 150 }, size: 20 },
      { type: 'currentBoost', position: { x: 300, y: 100 }, size: 40 },
    ],
    background: 'linear-gradient(to right, #0ea5e9, #0284c7)',
  },
  {
    id: 'coastal-challenge',
    name: 'Coastal Challenge',
    length: 1500,
    difficulty: 'medium',
    obstacles: [
      { type: 'rock', position: { x: 250, y: 75 }, size: 30 },
      { type: 'rock', position: { x: 600, y: 125 }, size: 25 },
      { type: 'buoy', position: { x: 400, y: 50 }, size: 20 },
      { type: 'buoy', position: { x: 800, y: 150 }, size: 20 },
      { type: 'buoy', position: { x: 1200, y: 75 }, size: 20 },
      { type: 'currentSlow', position: { x: 700, y: 100 }, size: 60 },
      { type: 'currentBoost', position: { x: 1000, y: 125 }, size: 50 },
    ],
    background: 'linear-gradient(to right, #0c4a6e, #0284c7)',
  },
  {
    id: 'ocean-marathon',
    name: 'Ocean Marathon',
    length: 2000,
    difficulty: 'hard',
    obstacles: [
      { type: 'rock', position: { x: 300, y: 100 }, size: 35 },
      { type: 'rock', position: { x: 800, y: 50 }, size: 30 },
      { type: 'rock', position: { x: 1500, y: 125 }, size: 40 },
      { type: 'debris', position: { x: 500, y: 75 }, size: 25 },
      { type: 'debris', position: { x: 1200, y: 100 }, size: 20 },
      { type: 'buoy', position: { x: 600, y: 150 }, size: 20 },
      { type: 'buoy', position: { x: 1000, y: 50 }, size: 20 },
      { type: 'buoy', position: { x: 1700, y: 125 }, size: 20 },
      { type: 'currentSlow', position: { x: 700, y: 75 }, size: 70 },
      { type: 'currentSlow', position: { x: 1300, y: 150 }, size: 60 },
      { type: 'currentBoost', position: { x: 900, y: 100 }, size: 40 },
      { type: 'currentBoost', position: { x: 1600, y: 75 }, size: 50 },
    ],
    background: 'linear-gradient(to right, #0c4a6e, #075985, #0c4a6e)',
  },
  {
    id: 'island-circuit',
    name: 'Island Circuit',
    length: 1800,
    difficulty: 'medium',
    obstacles: [
      { type: 'rock', position: { x: 200, y: 125 }, size: 30 },
      { type: 'rock', position: { x: 500, y: 75 }, size: 25 },
      { type: 'rock', position: { x: 1200, y: 100 }, size: 35 },
      { type: 'debris', position: { x: 800, y: 50 }, size: 20 },
      { type: 'debris', position: { x: 1500, y: 125 }, size: 25 },
      { type: 'buoy', position: { x: 300, y: 150 }, size: 20 },
      { type: 'buoy', position: { x: 700, y: 100 }, size: 20 },
      { type: 'buoy', position: { x: 1100, y: 75 }, size: 20 },
      { type: 'buoy', position: { x: 1600, y: 150 }, size: 20 },
      { type: 'currentSlow', position: { x: 400, y: 100 }, size: 50 },
      { type: 'currentSlow', position: { x: 1000, y: 125 }, size: 60 },
      { type: 'currentBoost', position: { x: 600, y: 75 }, size: 45 },
      { type: 'currentBoost', position: { x: 1400, y: 100 }, size: 55 },
    ],
    background: 'linear-gradient(to right, #0891b2, #06b6d4, #0891b2)',
  },
];

export const getTrackById = (id: string): RaceTrack | undefined => {
  return tracks.find(track => track.id === id);
};

export const getTrackByDifficulty = (difficulty: 'easy' | 'medium' | 'hard'): RaceTrack[] => {
  return tracks.filter(track => track.difficulty === difficulty);
};

export const getRandomTrack = (): RaceTrack => {
  const randomIndex = Math.floor(Math.random() * tracks.length);
  return tracks[randomIndex];
};