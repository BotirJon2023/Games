export interface Team {
  id: string;
  name: string;
  color: string;
}

export interface GameState {
  team1Score: number;
  team2Score: number;
  currentSet: number;
  team1Sets: number;
  team2Sets: number;
  isGameOver: boolean;
  winner: string | null;
}

export class VolleyballSimulator {
  private gameState: GameState;
  private team1: Team;
  private team2: Team;
  private onScoreUpdate: (state: GameState) => void;

  constructor(team1: Team, team2: Team, onScoreUpdate: (state: GameState) => void) {
    this.team1 = team1;
    this.team2 = team2;
    this.onScoreUpdate = onScoreUpdate;
    this.gameState = {
      team1Score: 0,
      team2Score: 0,
      currentSet: 1,
      team1Sets: 0,
      team2Sets: 0,
      isGameOver: false,
      winner: null,
    };
  }

  async simulateMatch(): Promise<GameState> {
    while (!this.gameState.isGameOver) {
      await this.simulateRally();
      await new Promise(resolve => setTimeout(resolve, 800));
    }
    return this.gameState;
  }

  private async simulateRally() {
    const scoringTeam = Math.random() > 0.5 ? 1 : 2;

    if (scoringTeam === 1) {
      this.gameState.team1Score++;
    } else {
      this.gameState.team2Score++;
    }

    const setWinScore = this.gameState.currentSet === 5 ? 15 : 25;
    const minWinMargin = 2;

    if (
      (this.gameState.team1Score >= setWinScore &&
        this.gameState.team1Score - this.gameState.team2Score >= minWinMargin) ||
      (this.gameState.team2Score >= setWinScore &&
        this.gameState.team2Score - this.gameState.team1Score >= minWinMargin)
    ) {
      if (this.gameState.team1Score > this.gameState.team2Score) {
        this.gameState.team1Sets++;
      } else {
        this.gameState.team2Sets++;
      }

      if (this.gameState.team1Sets === 3) {
        this.gameState.isGameOver = true;
        this.gameState.winner = this.team1.id;
      } else if (this.gameState.team2Sets === 3) {
        this.gameState.isGameOver = true;
        this.gameState.winner = this.team2.id;
      } else {
        this.gameState.currentSet++;
        this.gameState.team1Score = 0;
        this.gameState.team2Score = 0;
      }
    }

    this.onScoreUpdate({ ...this.gameState });
  }
}
