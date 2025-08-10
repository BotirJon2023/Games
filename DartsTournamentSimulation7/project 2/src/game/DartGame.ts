import { Player, DartThrow, DartboardPosition } from '../types/tournament';

export class DartGame {
  public player1: Player;
  public player2: Player;
  public score1: number = 501;
  public score2: number = 501;
  public currentPlayer: Player;
  public isActive: boolean = false;
  public winner: Player | null = null;
  public lastThrow: DartThrow | null = null;
  private canvas: HTMLCanvasElement | null;
  private ctx: CanvasRenderingContext2D | null = null;
  private animationId: number | null = null;
  private darts: AnimatedDart[] = [];

  constructor(player1: Player, player2: Player, canvas: HTMLCanvasElement | null) {
    this.player1 = player1;
    this.player2 = player2;
    this.currentPlayer = Math.random() > 0.5 ? player1 : player2;
    this.canvas = canvas;
    if (canvas) {
      this.ctx = canvas.getContext('2d');
    }
  }

  async playMatch(): Promise<Player> {
    this.isActive = true;
    
    while (this.score1 > 0 && this.score2 > 0 && !this.winner) {
      await this.playTurn();
      this.switchPlayer();
      
      // Small delay between turns for better visualization
      await this.delay(500);
    }
    
    this.isActive = false;
    
    // Update player stats
    if (this.winner) {
      this.winner.wins++;
      const loser = this.winner === this.player1 ? this.player2 : this.player1;
      loser.losses++;
    }
    
    return this.winner!;
  }

  private async playTurn(): Promise<void> {
    // Each player throws 3 darts per turn
    for (let dart = 0; dart < 3 && !this.winner; dart++) {
      const dartThrow = this.generateDartThrow();
      await this.animateDart(dartThrow);
      
      const score = dartThrow.score * dartThrow.multiplier;
      const currentScore = this.currentPlayer === this.player1 ? this.score1 : this.score2;
      
      // Update score
      if (this.currentPlayer === this.player1) {
        this.score1 = Math.max(0, this.score1 - score);
        if (this.score1 === 0) {
          this.winner = this.player1;
        }
      } else {
        this.score2 = Math.max(0, this.score2 - score);
        if (this.score2 === 0) {
          this.winner = this.player2;
        }
      }

      this.lastThrow = dartThrow;
      
      // Update player statistics
      this.updatePlayerStats(dartThrow);
      
      // Delay between darts
      await this.delay(300);
    }
  }

  private generateDartThrow(): DartThrow {
    const player = this.currentPlayer;
    const centerX = 200; // Canvas center
    const centerY = 200;
    const maxRadius = 180; // Dartboard radius
    
    // Calculate throw accuracy based on player skill
    const baseSkill = player.skill;
    const accuracy = player.accuracy;
    const consistency = player.consistency;
    
    // Add some randomness to make it interesting
    const skillVariation = (Math.random() - 0.5) * (1 - consistency) * 0.5;
    const effectiveSkill = Math.max(0.1, Math.min(0.95, baseSkill + skillVariation));
    
    // Determine target area based on remaining score
    const remainingScore = player === this.player1 ? this.score1 : this.score2;
    let targetScore = 20; // Default to high-value area
    
    if (remainingScore <= 170) {
      // Try to finish the game with strategic targeting
      if (remainingScore <= 50) {
        targetScore = remainingScore <= 20 ? remainingScore : 20;
      }
    }
    
    // Calculate dart position with skill-based accuracy
    const aimRadius = maxRadius * (1 - effectiveSkill) * 0.8;
    const angle = Math.random() * 2 * Math.PI;
    const radius = Math.random() * aimRadius;
    
    const x = centerX + Math.cos(angle) * radius;
    const y = centerY + Math.sin(angle) * radius;
    
    const position = this.calculateScoreFromPosition(x, y, centerX, centerY, maxRadius);
    
    return {
      x,
      y,
      score: position.score,
      multiplier: position.multiplier,
      section: position.section,
      player,
      timestamp: Date.now()
    };
  }

  private calculateScoreFromPosition(
    x: number, 
    y: number, 
    centerX: number, 
    centerY: number, 
    maxRadius: number
  ): DartboardPosition {
    const dx = x - centerX;
    const dy = y - centerY;
    const distance = Math.sqrt(dx * dx + dy * dy);
    
    // Dartboard layout (from center outward)
    if (distance <= maxRadius * 0.04) {
      return { x, y, score: 50, multiplier: 1, section: 'inner-bull' };
    } else if (distance <= maxRadius * 0.08) {
      return { x, y, score: 25, multiplier: 1, section: 'outer-bull' };
    } else if (distance > maxRadius) {
      return { x, y, score: 0, multiplier: 1, section: 'miss' };
    }
    
    // Calculate angle for number segments
    let angle = Math.atan2(dy, dx) * 180 / Math.PI;
    angle = (angle + 360 + 90 + 9) % 360; // Adjust for dartboard orientation
    
    const segmentIndex = Math.floor(angle / 18);
    const numbers = [20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5];
    const score = numbers[segmentIndex];
    
    // Determine multiplier based on distance
    if (distance >= maxRadius * 0.6) {
      // Outer single
      return { x, y, score, multiplier: 1, section: 'single' };
    } else if (distance >= maxRadius * 0.55) {
      // Triple ring
      return { x, y, score, multiplier: 3, section: 'triple' };
    } else if (distance >= maxRadius * 0.3) {
      // Inner single
      return { x, y, score, multiplier: 1, section: 'single' };
    } else {
      // Double ring
      return { x, y, score, multiplier: 2, section: 'double' };
    }
  }

  private async animateDart(dartThrow: DartThrow): Promise<void> {
    if (!this.canvas || !this.ctx) return;
    
    return new Promise((resolve) => {
      const startX = Math.random() > 0.5 ? -50 : this.canvas!.width + 50;
      const startY = Math.random() * this.canvas!.height;
      
      const dart: AnimatedDart = {
        currentX: startX,
        currentY: startY,
        targetX: dartThrow.x,
        targetY: dartThrow.y,
        progress: 0,
        speed: 0.05 + Math.random() * 0.03,
        dartThrow
      };
      
      this.darts.push(dart);
      
      const animate = () => {
        dart.progress += dart.speed;
        
        if (dart.progress >= 1) {
          dart.currentX = dart.targetX;
          dart.currentY = dart.targetY;
          resolve();
          return;
        }
        
        // Easing function for smooth animation
        const easeProgress = 1 - Math.pow(1 - dart.progress, 3);
        dart.currentX = startX + (dart.targetX - startX) * easeProgress;
        dart.currentY = startY + (dart.targetY - startY) * easeProgress;
        
        this.drawDarts();
        requestAnimationFrame(animate);
      };
      
      animate();
    });
  }

  private drawDarts(): void {
    if (!this.canvas || !this.ctx) return;
    
    // Redraw dartboard (simplified)
    this.redrawDartboard();
    
    // Draw all darts
    this.darts.forEach(dart => {
      this.drawSingleDart(dart.currentX, dart.currentY);
    });
  }

  private drawSingleDart(x: number, y: number): void {
    if (!this.ctx) return;
    
    this.ctx.fillStyle = '#8B4513';
    this.ctx.beginPath();
    this.ctx.arc(x, y, 3, 0, 2 * Math.PI);
    this.ctx.fill();
    
    // Dart tip
    this.ctx.fillStyle = '#C0C0C0';
    this.ctx.beginPath();
    this.ctx.arc(x, y, 1, 0, 2 * Math.PI);
    this.ctx.fill();
  }

  private redrawDartboard(): void {
    // This would redraw the dartboard - simplified for performance
    // In a real implementation, you'd want to cache the dartboard as an image
    if (!this.canvas || !this.ctx) return;
    
    this.ctx.fillStyle = '#1a1a1a';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    
    // Draw simplified dartboard
    const centerX = this.canvas.width / 2;
    const centerY = this.canvas.height / 2;
    const radius = 180;
    
    this.ctx.beginPath();
    this.ctx.arc(centerX, centerY, radius, 0, 2 * Math.PI);
    this.ctx.fillStyle = '#2d5016';
    this.ctx.fill();
    this.ctx.strokeStyle = '#000';
    this.ctx.lineWidth = 3;
    this.ctx.stroke();
  }

  private updatePlayerStats(dartThrow: DartThrow): void {
    const player = dartThrow.player;
    const totalScore = dartThrow.score * dartThrow.multiplier;
    
    if (dartThrow.section === 'inner-bull' || dartThrow.section === 'outer-bull') {
      player.bullseyes++;
    } else if (dartThrow.multiplier === 3) {
      player.trebles++;
    } else if (dartThrow.multiplier === 2) {
      player.doubles++;
    }
    
    player.totalScore += totalScore;
  }

  private switchPlayer(): void {
    this.currentPlayer = this.currentPlayer === this.player1 ? this.player2 : this.player1;
  }

  private delay(ms: number): Promise<void> {
    return new Promise(resolve => setTimeout(resolve, ms));
  }
}

interface AnimatedDart {
  currentX: number;
  currentY: number;
  targetX: number;
  targetY: number;
  progress: number;
  speed: number;
  dartThrow: DartThrow;
}