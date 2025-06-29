import { Skater, GameState, PowerUp, Track } from '../types/game';

export class GameLogic {
  private track: Track;

  constructor(track: Track) {
    this.track = track;
  }

  createSkater(
    id: string, 
    name: string, 
    startPosition: number, 
    isPlayer: boolean = false,
    aiDifficulty: number = 0.8
  ): Skater {
    const angle = (startPosition * Math.PI * 2) / 8; // 8 starting positions
    const radius = (this.track.innerRadius + this.track.outerRadius) / 2;
    
    return {
      id,
      name,
      position: {
        x: this.track.centerX + Math.cos(angle) * radius,
        y: this.track.centerY + Math.sin(angle) * radius
      },
      velocity: { x: 0, y: 0 },
      speed: 0,
      maxSpeed: isPlayer ? 300 : 280 + (Math.random() * 40),
      acceleration: isPlayer ? 1.2 : 1.0 + (Math.random() * 0.4),
      stamina: 100,
      maxStamina: 100,
      color: this.generateSkaterColor(startPosition, isPlayer),
      angle: angle + Math.PI / 2,
      lap: 0,
      lapTime: 0,
      bestLapTime: 0,
      totalTime: 0,
      isPlayer,
      aiDifficulty,
      powerUpActive: false,
      powerUpType: null,
      powerUpDuration: 0
    };
  }

  private generateSkaterColor(position: number, isPlayer: boolean): string {
    if (isPlayer) return '#FF4444';
    
    const colors = [
      '#4444FF', '#44FF44', '#FFFF44', '#FF44FF', 
      '#44FFFF', '#FFA500', '#800080', '#008000'
    ];
    return colors[position % colors.length];
  }

  createPowerUp(id: string, type: 'speed' | 'stamina' | 'slip'): PowerUp {
    const angle = Math.random() * Math.PI * 2;
    const radius = this.track.innerRadius + Math.random() * this.track.width;
    
    return {
      id,
      type,
      position: {
        x: this.track.centerX + Math.cos(angle) * radius,
        y: this.track.centerY + Math.sin(angle) * radius
      },
      active: true,
      duration: 5000 // 5 seconds
    };
  }

  checkPowerUpCollisions(gameState: GameState): void {
    gameState.skaters.forEach(skater => {
      gameState.powerUps.forEach(powerUp => {
        if (!powerUp.active) return;

        const distance = Math.sqrt(
          (skater.position.x - powerUp.position.x) ** 2 + 
          (skater.position.y - powerUp.position.y) ** 2
        );

        if (distance < 30) {
          this.applyPowerUp(skater, powerUp);
          powerUp.active = false;
        }
      });
    });

    // Remove inactive power-ups
    gameState.powerUps = gameState.powerUps.filter(p => p.active);
  }

  private applyPowerUp(skater: Skater, powerUp: PowerUp): void {
    skater.powerUpActive = true;
    skater.powerUpType = powerUp.type;
    skater.powerUpDuration = powerUp.duration / 1000; // Convert to seconds

    switch (powerUp.type) {
      case 'speed':
        // Speed boost applied in physics engine
        break;
      case 'stamina':
        skater.stamina = skater.maxStamina;
        break;
      case 'slip':
        // Slip effect applied to other skaters in physics engine
        break;
    }
  }

  checkRaceCompletion(gameState: GameState): void {
    const finishedSkaters = gameState.skaters.filter(s => s.lap >= gameState.maxLaps);
    
    if (finishedSkaters.length > 0 && !gameState.winner) {
      // Sort by total time (lap completion time)
      finishedSkaters.sort((a, b) => a.totalTime - b.totalTime);
      gameState.winner = finishedSkaters[0];
      gameState.isRunning = false;
    }
  }

  updateGameState(gameState: GameState, deltaTime: number): void {
    gameState.gameTime += deltaTime;

    // Update total time for each skater
    gameState.skaters.forEach(skater => {
      skater.totalTime += deltaTime;
    });

    // Spawn power-ups occasionally
    if (Math.random() < 0.002 && gameState.powerUps.length < 3) {
      const types: ('speed' | 'stamina' | 'slip')[] = ['speed', 'stamina', 'slip'];
      const randomType = types[Math.floor(Math.random() * types.length)];
      const powerUp = this.createPowerUp(`powerup-${Date.now()}`, randomType);
      gameState.powerUps.push(powerUp);
    }

    this.checkPowerUpCollisions(gameState);
    this.checkRaceCompletion(gameState);
  }

  getLeaderboard(skaters: Skater[]): Skater[] {
    return [...skaters].sort((a, b) => {
      if (a.lap !== b.lap) return b.lap - a.lap;
      // If same lap, sort by position on track (approximate)
      const aDistance = this.calculateTrackProgress(a);
      const bDistance = this.calculateTrackProgress(b);
      return bDistance - aDistance;
    });
  }

  private calculateTrackProgress(skater: Skater): number {
    const centerX = this.track.centerX;
    const centerY = this.track.centerY;
    const angle = Math.atan2(skater.position.y - centerY, skater.position.x - centerX);
    return (angle + Math.PI) / (2 * Math.PI); // Normalize to 0-1
  }

  resetGame(gameState: GameState): void {
    gameState.isRunning = false;
    gameState.isPaused = false;
    gameState.gameTime = 0;
    gameState.winner = null;
    gameState.powerUps = [];
    
    // Reset all skaters
    gameState.skaters.forEach((skater, index) => {
      const angle = (index * Math.PI * 2) / gameState.skaters.length;
      const radius = (this.track.innerRadius + this.track.outerRadius) / 2;
      
      skater.position.x = this.track.centerX + Math.cos(angle) * radius;
      skater.position.y = this.track.centerY + Math.sin(angle) * radius;
      skater.velocity.x = 0;
      skater.velocity.y = 0;
      skater.speed = 0;
      skater.stamina = skater.maxStamina;
      skater.angle = angle + Math.PI / 2;
      skater.lap = 0;
      skater.lapTime = 0;
      skater.totalTime = 0;
      skater.powerUpActive = false;
      skater.powerUpType = null;
      skater.powerUpDuration = 0;
    });
  }
}