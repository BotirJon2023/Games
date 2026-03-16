export interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
}

export interface Player {
  x: number;
  y: number;
  width: number;
  height: number;
  isComputer: boolean;
  teamId: string;
}

export interface GameState {
  ball: Ball;
  player1: Player;
  player2: Player;
  team1Score: number;
  team2Score: number;
  currentSet: number;
  team1Sets: number;
  team2Sets: number;
  isGameOver: boolean;
  winner: string | null;
  gameStarted: boolean;
}

const COURT_WIDTH = 800;
const COURT_HEIGHT = 400;
const GRAVITY = 0.4;
const BALL_RADIUS = 8;
const PLAYER_WIDTH = 15;
const PLAYER_HEIGHT = 60;
const NET_HEIGHT = 150;

export class VolleyballGame {
  private gameState: GameState;
  private team1Id: string;
  private team2Id: string;
  private onStateUpdate: (state: GameState) => void;
  private gameRunning: boolean = true;
  private lastBallOwner: string | null = null;
  private ballTouchCount: number = 0;

  constructor(
    team1Id: string,
    team2Id: string,
    onStateUpdate: (state: GameState) => void,
    player1IsComputer: boolean = false,
    player2IsComputer: boolean = true
  ) {
    this.team1Id = team1Id;
    this.team2Id = team2Id;
    this.onStateUpdate = onStateUpdate;

    this.gameState = {
      ball: {
        x: COURT_WIDTH / 2,
        y: NET_HEIGHT - 20,
        vx: 0,
        vy: 0,
        radius: BALL_RADIUS,
      },
      player1: {
        x: 100,
        y: COURT_HEIGHT / 2 - PLAYER_HEIGHT / 2,
        width: PLAYER_WIDTH,
        height: PLAYER_HEIGHT,
        isComputer: player1IsComputer,
        teamId: team1Id,
      },
      player2: {
        x: COURT_WIDTH - 100 - PLAYER_WIDTH,
        y: COURT_HEIGHT / 2 - PLAYER_HEIGHT / 2,
        width: PLAYER_WIDTH,
        height: PLAYER_HEIGHT,
        isComputer: player2IsComputer,
        teamId: team2Id,
      },
      team1Score: 0,
      team2Score: 0,
      currentSet: 1,
      team1Sets: 0,
      team2Sets: 0,
      isGameOver: false,
      winner: null,
      gameStarted: false,
    };
  }

  startGame() {
    this.gameState.gameStarted = true;
    this.gameState.ball.vx = 3;
    this.gameState.ball.vy = -4;
    this.onStateUpdate({ ...this.gameState });
  }

  updatePlayerPosition(playerId: 'player1' | 'player2', dy: number) {
    const player = this.gameState[playerId];
    player.y = Math.max(0, Math.min(COURT_HEIGHT - PLAYER_HEIGHT, player.y + dy));
  }

  update() {
    if (!this.gameState.gameStarted || this.gameState.isGameOver) return;

    this.updateBall();
    this.updateAIPlayers();
    this.checkCollisions();
    this.checkOutOfBounds();

    this.onStateUpdate({ ...this.gameState });
  }

  private updateBall() {
    this.gameState.ball.vy += GRAVITY;

    this.gameState.ball.x += this.gameState.ball.vx;
    this.gameState.ball.y += this.gameState.ball.vy;

    if (this.gameState.ball.y + BALL_RADIUS > COURT_HEIGHT) {
      this.gameState.ball.y = COURT_HEIGHT - BALL_RADIUS;
      this.gameState.ball.vy *= -0.3;
    }

    if (this.gameState.ball.y - BALL_RADIUS < 0) {
      this.gameState.ball.y = BALL_RADIUS;
      this.gameState.ball.vy *= -0.8;
    }
  }

  private updateAIPlayers() {
    if (this.gameState.player1.isComputer) {
      this.moveAIPlayer('player1');
    }
    if (this.gameState.player2.isComputer) {
      this.moveAIPlayer('player2');
    }
  }

  private moveAIPlayer(playerId: 'player1' | 'player2') {
    const player = this.gameState[playerId];
    const ballCenterY = this.gameState.ball.y;
    const playerCenterY = player.y + PLAYER_HEIGHT / 2;

    const aiSpeed = 2.5;

    if (ballCenterY < playerCenterY - 15) {
      player.y = Math.max(0, player.y - aiSpeed);
    } else if (ballCenterY > playerCenterY + 15) {
      player.y = Math.min(COURT_HEIGHT - PLAYER_HEIGHT, player.y + aiSpeed);
    }
  }

  private checkCollisions() {
    const ball = this.gameState.ball;

    this.checkPlayerCollision('player1');
    this.checkPlayerCollision('player2');

    if (Math.abs(ball.x - COURT_WIDTH / 2) < 5 && ball.y < NET_HEIGHT + 20) {
      ball.vx *= -0.7;
      ball.x = COURT_WIDTH / 2 + (ball.vx > 0 ? 5 : -5);
    }
  }

  private checkPlayerCollision(playerId: 'player1' | 'player2') {
    const player = this.gameState[playerId];
    const ball = this.gameState.ball;

    const closestX = Math.max(player.x, Math.min(ball.x, player.x + player.width));
    const closestY = Math.max(player.y, Math.min(ball.y, player.y + player.height));

    const distX = ball.x - closestX;
    const distY = ball.y - closestY;
    const distance = Math.sqrt(distX * distX + distY * distY);

    if (distance < BALL_RADIUS) {
      const angle = Math.atan2(distY, distX);
      ball.vx = Math.cos(angle) * 7;
      ball.vy = Math.sin(angle) * 7 - 3;

      ball.x = closestX + Math.cos(angle) * (BALL_RADIUS + 1);
      ball.y = closestY + Math.sin(angle) * (BALL_RADIUS + 1);

      this.lastBallOwner = player.teamId;
      this.ballTouchCount = 0;
    }
  }

  private checkOutOfBounds() {
    const ball = this.gameState.ball;

    if (ball.x < 0 || ball.x > COURT_WIDTH || ball.y > COURT_HEIGHT + 50) {
      this.scorePoint();
    }
  }

  private scorePoint() {
    if (this.gameState.ball.x < COURT_WIDTH / 2) {
      this.gameState.team2Score++;
    } else {
      this.gameState.team1Score++;
    }

    this.ballTouchCount = 0;
    this.resetBall();
    this.checkSetWin();
  }

  private resetBall() {
    this.gameState.ball.x = COURT_WIDTH / 2;
    this.gameState.ball.y = NET_HEIGHT - 20;
    this.gameState.ball.vx = Math.random() > 0.5 ? 2 : -2;
    this.gameState.ball.vy = -3;
  }

  private checkSetWin() {
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
        this.gameState.winner = this.team1Id;
      } else if (this.gameState.team2Sets === 3) {
        this.gameState.isGameOver = true;
        this.gameState.winner = this.team2Id;
      } else {
        this.gameState.currentSet++;
        this.gameState.team1Score = 0;
        this.gameState.team2Score = 0;
        this.resetBall();
      }
    }
  }

  getGameState(): GameState {
    return { ...this.gameState };
  }

  stop() {
    this.gameRunning = false;
  }
}
