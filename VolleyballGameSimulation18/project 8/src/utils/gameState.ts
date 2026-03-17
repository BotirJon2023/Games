import { GameState } from '../types/game';
import {
  CANVAS_WIDTH,
  GROUND_HEIGHT,
  PLAYER_WIDTH,
  PLAYER_HEIGHT,
  BALL_RADIUS,
  MAX_SCORE,
  COLORS,
} from '../constants/game';

export function createInitialGameState(gameMode: 'pvp' | 'pvc'): GameState {
  return {
    player1: {
      position: { x: CANVAS_WIDTH * 0.25, y: GROUND_HEIGHT },
      velocity: { x: 0, y: 0 },
      width: PLAYER_WIDTH,
      height: PLAYER_HEIGHT,
      color: COLORS.player1,
      score: 0,
    },
    player2: {
      position: { x: CANVAS_WIDTH * 0.75, y: GROUND_HEIGHT },
      velocity: { x: 0, y: 0 },
      width: PLAYER_WIDTH,
      height: PLAYER_HEIGHT,
      color: COLORS.player2,
      score: 0,
    },
    ball: {
      position: { x: CANVAS_WIDTH * 0.25, y: GROUND_HEIGHT - 100 },
      velocity: { x: 0, y: 0 },
      radius: BALL_RADIUS,
      isServing: true,
    },
    isPlaying: true,
    isPaused: false,
    gameMode,
    servingPlayer: 1,
    maxScore: MAX_SCORE,
  };
}

export function resetBall(state: GameState) {
  const servingPlayerPos =
    state.servingPlayer === 1 ? state.player1.position.x : state.player2.position.x;

  state.ball.position.x = servingPlayerPos;
  state.ball.position.y = GROUND_HEIGHT - 100;
  state.ball.velocity.x = 0;
  state.ball.velocity.y = 0;
  state.ball.isServing = true;
}

export function handleScore(
  state: GameState,
  side: 'left' | 'right'
): { winner: 1 | 2 | null } {
  if (side === 'left') {
    state.player2.score++;
    state.servingPlayer = 2;
  } else {
    state.player1.score++;
    state.servingPlayer = 1;
  }

  resetBall(state);

  if (state.player1.score >= state.maxScore) {
    return { winner: 1 };
  } else if (state.player2.score >= state.maxScore) {
    return { winner: 2 };
  }

  return { winner: null };
}
