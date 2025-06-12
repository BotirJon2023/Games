export interface Exercise {
  id: string;
  name: string;
  description: string;
  reps?: number;
  duration?: number;
  weight?: number;
  icon: string;
}

export interface Competition {
  id: string;
  name: string;
  type: 'AMRAP' | 'EMOM' | 'FOR_TIME' | 'TABATA' | 'CHIPPER';
  description: string;
  duration: number; // in seconds
  exercises: Exercise[];
  difficulty: 'Beginner' | 'Intermediate' | 'Advanced';
}

export interface GameState {
  currentCompetition: Competition | null;
  isActive: boolean;
  isPaused: boolean;
  timeRemaining: number;
  currentRound: number;
  score: number;
  completedExercises: string[];
}

export interface PlayerStats {
  totalWorkouts: number;
  totalTime: number;
  bestScores: Record<string, number>;
  achievements: string[];
}