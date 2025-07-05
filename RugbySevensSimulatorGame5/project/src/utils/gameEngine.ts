import { Player, Team, GameState, GameEvent, FieldDimensions } from '../types/game';

export const FIELD_DIMENSIONS: FieldDimensions = {
  width: 800,
  height: 400,
  tryLineHome: 50,
  tryLineAway: 750,
  centerLine: 400
};

export class GameEngine {
  private gameState: GameState;
  private eventId: number = 0;
  private gameTimer: NodeJS.Timeout | null = null;

  constructor(homeTeam: Team, awayTeam: Team) {
    this.gameState = {
      homeTeam,
      awayTeam,
      currentHalf: 1,
      timeRemaining: 420, // 7 minutes per half
      possession: Math.random() > 0.5 ? 'home' : 'away',
      ballPosition: { x: FIELD_DIMENSIONS.centerLine, y: FIELD_DIMENSIONS.height / 2 },
      isPlaying: false,
      gameLog: [],
      phase: 'kickoff'
    };
    this.initializePlayers();
  }

  private initializePlayers(): void {
    this.gameState.homeTeam.players.forEach((player, index) => {
      player.x = 100 + (index % 3) * 50;
      player.y = 100 + Math.floor(index / 3) * 80;
      player.currentStamina = player.stamina;
      player.hasBall = false;
    });

    this.gameState.awayTeam.players.forEach((player, index) => {
      player.x = 600 + (index % 3) * 50;
      player.y = 100 + Math.floor(index / 3) * 80;
      player.currentStamina = player.stamina;
      player.hasBall = false;
    });
  }

  public startGame(): GameState {
    this.gameState.isPlaying = true;
    this.addEvent('kick', this.gameState.possession, undefined, 
      `${this.gameState.possession === 'home' ? this.gameState.homeTeam.name : this.gameState.awayTeam.name} kicks off!`);
    
    this.gameTimer = setInterval(() => {
      this.updateGame();
    }, 100);

    return { ...this.gameState };
  }

  public pauseGame(): GameState {
    this.gameState.isPlaying = false;
    if (this.gameTimer) {
      clearInterval(this.gameTimer);
      this.gameTimer = null;
    }
    return { ...this.gameState };
  }

  private updateGame(): void {
    if (!this.gameState.isPlaying) return;

    this.gameState.timeRemaining -= 0.1;
    
    if (this.gameState.timeRemaining <= 0) {
      if (this.gameState.currentHalf === 1) {
        this.gameState.currentHalf = 2;
        this.gameState.timeRemaining = 420;
        this.gameState.phase = 'halftime';
        this.addEvent('penalty', this.gameState.possession, undefined, 'Half time!');
      } else {
        this.gameState.phase = 'fulltime';
        this.gameState.isPlaying = false;
        this.addEvent('penalty', this.gameState.possession, undefined, 'Full time!');
        if (this.gameTimer) clearInterval(this.gameTimer);
      }
      return;
    }

    this.simulatePlay();
    this.updatePlayerPositions();
    this.updateStamina();
  }

  private simulatePlay(): void {
    const playChance = Math.random();
    
    if (playChance < 0.02) { // 2% chance of try
      this.scoreTry();
    } else if (playChance < 0.05) { // 3% chance of penalty
      this.awardPenalty();
    } else if (playChance < 0.1) { // 5% chance of turnover
      this.turnover();
    } else if (playChance < 0.15) { // 5% chance of tackle
      this.makeTackle();
    } else if (playChance < 0.25) { // 10% chance of pass
      this.makePass();
    }
  }

  private scoreTry(): void {
    const scoringTeam = this.gameState.possession;
    const team = scoringTeam === 'home' ? this.gameState.homeTeam : this.gameState.awayTeam;
    const scorer = this.getRandomPlayer(team);
    
    team.score += 5;
    this.addEvent('try', scoringTeam, scorer.name, `Try! ${scorer.name} scores for ${team.name}!`);
    
    // Conversion attempt
    setTimeout(() => {
      if (Math.random() > 0.3) { // 70% conversion rate
        team.score += 2;
        this.addEvent('conversion', scoringTeam, scorer.name, `Conversion successful! +2 points`);
      } else {
        this.addEvent('conversion', scoringTeam, scorer.name, `Conversion missed!`);
      }
    }, 1000);

    this.gameState.possession = scoringTeam === 'home' ? 'away' : 'home';
    this.gameState.phase = 'kickoff';
  }

  private awardPenalty(): void {
    const opposingTeam = this.gameState.possession === 'home' ? 'away' : 'home';
    this.gameState.possession = opposingTeam;
    this.addEvent('penalty', opposingTeam, undefined, `Penalty awarded to ${opposingTeam === 'home' ? this.gameState.homeTeam.name : this.gameState.awayTeam.name}`);
  }

  private turnover(): void {
    const newPossession = this.gameState.possession === 'home' ? 'away' : 'home';
    const player = this.getRandomPlayer(newPossession === 'home' ? this.gameState.homeTeam : this.gameState.awayTeam);
    
    this.gameState.possession = newPossession;
    this.addEvent('turnover', newPossession, player.name, `Turnover! ${player.name} wins possession`);
  }

  private makeTackle(): void {
    const defendingTeam = this.gameState.possession === 'home' ? this.gameState.awayTeam : this.gameState.homeTeam;
    const tackler = this.getRandomPlayer(defendingTeam);
    
    this.addEvent('tackle', defendingTeam.id as 'home' | 'away', tackler.name, `${tackler.name} makes a tackle!`);
  }

  private makePass(): void {
    const team = this.gameState.possession === 'home' ? this.gameState.homeTeam : this.gameState.awayTeam;
    const passer = this.getRandomPlayer(team);
    
    this.addEvent('pass', this.gameState.possession, passer.name, `${passer.name} makes a pass`);
  }

  private updatePlayerPositions(): void {
    const allPlayers = [...this.gameState.homeTeam.players, ...this.gameState.awayTeam.players];
    
    allPlayers.forEach(player => {
      // Simulate player movement based on game situation
      const targetX = this.gameState.ballPosition.x + (Math.random() - 0.5) * 100;
      const targetY = this.gameState.ballPosition.y + (Math.random() - 0.5) * 100;
      
      const dx = targetX - player.x;
      const dy = targetY - player.y;
      const distance = Math.sqrt(dx * dx + dy * dy);
      
      if (distance > 5) {
        const moveSpeed = (player.speed / 100) * 2;
        player.x += (dx / distance) * moveSpeed;
        player.y += (dy / distance) * moveSpeed;
        
        // Keep players within field bounds
        player.x = Math.max(10, Math.min(FIELD_DIMENSIONS.width - 10, player.x));
        player.y = Math.max(10, Math.min(FIELD_DIMENSIONS.height - 10, player.y));
      }
    });

    // Update ball position based on possession
    const possessingTeam = this.gameState.possession === 'home' ? this.gameState.homeTeam : this.gameState.awayTeam;
    const ballCarrier = this.getRandomPlayer(possessingTeam);
    this.gameState.ballPosition = { x: ballCarrier.x, y: ballCarrier.y };
  }

  private updateStamina(): void {
    const allPlayers = [...this.gameState.homeTeam.players, ...this.gameState.awayTeam.players];
    
    allPlayers.forEach(player => {
      player.currentStamina = Math.max(0, player.currentStamina - 0.1);
      if (player.currentStamina < 20) {
        // Player is tired, reduce effectiveness
        player.speed *= 0.9;
        player.skill *= 0.95;
      }
    });
  }

  private getRandomPlayer(team: Team): Player {
    return team.players[Math.floor(Math.random() * team.players.length)];
  }

  private addEvent(type: GameEvent['type'], team: 'home' | 'away', player?: string, message?: string): void {
    this.gameState.gameLog.unshift({
      id: this.eventId++,
      type,
      team,
      player,
      message: message || `${type} event`,
      timestamp: 420 - this.gameState.timeRemaining
    });

    if (this.gameState.gameLog.length > 50) {
      this.gameState.gameLog = this.gameState.gameLog.slice(0, 50);
    }
  }

  public getGameState(): GameState {
    return { ...this.gameState };
  }
}