import { GameState, Skater, Track, PowerUp, Particle } from '../types/game';

export class Renderer {
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  private track: Track;
  private particles: Particle[] = [];
  private camera = { x: 0, y: 0, zoom: 1 };

  constructor(canvas: HTMLCanvasElement, track: Track) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d')!;
    this.track = track;
    this.setupCanvas();
  }

  private setupCanvas(): void {
    this.canvas.width = window.innerWidth;
    this.canvas.height = window.innerHeight;
    
    // Set up smooth rendering
    this.ctx.imageSmoothingEnabled = true;
    this.ctx.imageSmoothingQuality = 'high';
  }

  render(gameState: GameState): void {
    this.clearCanvas();
    this.updateCamera(gameState);
    
    // Apply camera transformation
    this.ctx.save();
    this.ctx.translate(this.canvas.width / 2, this.canvas.height / 2);
    this.ctx.scale(this.camera.zoom, this.camera.zoom);
    this.ctx.translate(-this.camera.x, -this.camera.y);

    this.renderBackground();
    this.renderTrack();
    this.renderPowerUps(gameState.powerUps);
    this.renderParticles();
    this.renderSkaters(gameState.skaters);
    this.renderTrackLines();

    this.ctx.restore();

    // Render UI elements (not affected by camera)
    this.renderUI(gameState);
  }

  private clearCanvas(): void {
    this.ctx.fillStyle = '#1a1a2e';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
  }

  private updateCamera(gameState: GameState): void {
    const player = gameState.skaters.find(s => s.isPlayer);
    if (player) {
      // Smooth camera following
      const targetX = player.position.x;
      const targetY = player.position.y;
      
      this.camera.x += (targetX - this.camera.x) * 0.1;
      this.camera.y += (targetY - this.camera.y) * 0.1;
      
      // Dynamic zoom based on speed
      const targetZoom = Math.max(0.6, Math.min(1.0, 1.0 - player.speed / 600));
      this.camera.zoom += (targetZoom - this.camera.zoom) * 0.05;
    }
  }

  private renderBackground(): void {
    // Ice rink background with gradient
    const gradient = this.ctx.createRadialGradient(
      this.track.centerX, this.track.centerY, this.track.innerRadius,
      this.track.centerX, this.track.centerY, this.track.outerRadius * 1.5
    );
    gradient.addColorStop(0, '#e8f4fd');
    gradient.addColorStop(0.7, '#b8e0ff');
    gradient.addColorStop(1, '#87ceeb');

    this.ctx.fillStyle = gradient;
    this.ctx.beginPath();
    this.ctx.arc(this.track.centerX, this.track.centerY, this.track.outerRadius * 1.2, 0, Math.PI * 2);
    this.ctx.fill();
  }

  private renderTrack(): void {
    // Outer track boundary
    this.ctx.strokeStyle = '#333';
    this.ctx.lineWidth = 4;
    this.ctx.beginPath();
    this.ctx.arc(this.track.centerX, this.track.centerY, this.track.outerRadius, 0, Math.PI * 2);
    this.ctx.stroke();

    // Inner track boundary
    this.ctx.beginPath();
    this.ctx.arc(this.track.centerX, this.track.centerY, this.track.innerRadius, 0, Math.PI * 2);
    this.ctx.stroke();

    // Track surface with ice texture effect
    this.ctx.fillStyle = '#f0f8ff';
    this.ctx.beginPath();
    this.ctx.arc(this.track.centerX, this.track.centerY, this.track.outerRadius, 0, Math.PI * 2);
    this.ctx.arc(this.track.centerX, this.track.centerY, this.track.innerRadius, 0, Math.PI * 2, true);
    this.ctx.fill();

    // Add subtle ice texture lines
    this.ctx.strokeStyle = '#e6f3ff';
    this.ctx.lineWidth = 1;
    for (let i = 0; i < 16; i++) {
      const angle = (i * Math.PI * 2) / 16;
      const startRadius = this.track.innerRadius + 10;
      const endRadius = this.track.outerRadius - 10;
      
      this.ctx.beginPath();
      this.ctx.moveTo(
        this.track.centerX + Math.cos(angle) * startRadius,
        this.track.centerY + Math.sin(angle) * startRadius
      );
      this.ctx.lineTo(
        this.track.centerX + Math.cos(angle) * endRadius,
        this.track.centerY + Math.sin(angle) * endRadius
      );
      this.ctx.stroke();
    }
  }

  private renderTrackLines(): void {
    // Start/finish line
    this.ctx.strokeStyle = '#ff0000';
    this.ctx.lineWidth = 3;
    this.ctx.beginPath();
    this.ctx.moveTo(
      this.track.centerX + this.track.innerRadius,
      this.track.centerY - 5
    );
    this.ctx.lineTo(
      this.track.centerX + this.track.outerRadius,
      this.track.centerY + 5
    );
    this.ctx.stroke();

    // Lane dividers
    this.ctx.strokeStyle = '#ccc';
    this.ctx.lineWidth = 1;
    this.ctx.setLineDash([10, 10]);
    
    const numLanes = 4;
    for (let i = 1; i < numLanes; i++) {
      const radius = this.track.innerRadius + (i * this.track.width) / numLanes;
      this.ctx.beginPath();
      this.ctx.arc(this.track.centerX, this.track.centerY, radius, 0, Math.PI * 2);
      this.ctx.stroke();
    }
    this.ctx.setLineDash([]);
  }

  private renderSkaters(skaters: Skater[]): void {
    skaters.forEach(skater => {
      this.renderSkater(skater);
      this.generateIceSpray(skater);
    });
  }

  private renderSkater(skater: Skater): void {
    this.ctx.save();
    this.ctx.translate(skater.position.x, skater.position.y);
    this.ctx.rotate(skater.angle);

    // Skater body
    this.ctx.fillStyle = skater.color;
    this.ctx.beginPath();
    this.ctx.ellipse(0, 0, 8, 15, 0, 0, Math.PI * 2);
    this.ctx.fill();

    // Skater outline
    this.ctx.strokeStyle = '#000';
    this.ctx.lineWidth = 2;
    this.ctx.stroke();

    // Speed trail effect
    if (skater.speed > 100) {
      this.ctx.globalAlpha = 0.3;
      this.ctx.fillStyle = skater.color;
      for (let i = 1; i <= 3; i++) {
        this.ctx.beginPath();
        this.ctx.ellipse(-i * 3, 0, 6, 12, 0, 0, Math.PI * 2);
        this.ctx.fill();
      }
      this.ctx.globalAlpha = 1;
    }

    // Power-up effect
    if (skater.powerUpActive) {
      this.ctx.strokeStyle = this.getPowerUpColor(skater.powerUpType!);
      this.ctx.lineWidth = 3;
      this.ctx.beginPath();
      this.ctx.arc(0, 0, 20 + Math.sin(Date.now() * 0.01) * 3, 0, Math.PI * 2);
      this.ctx.stroke();
    }

    // Player indicator
    if (skater.isPlayer) {
      this.ctx.fillStyle = '#fff';
      this.ctx.beginPath();
      this.ctx.arc(0, -25, 3, 0, Math.PI * 2);
      this.ctx.fill();
    }

    this.ctx.restore();

    // Skater name and stats
    this.renderSkaterInfo(skater);
  }

  private renderSkaterInfo(skater: Skater): void {
    this.ctx.fillStyle = '#fff';
    this.ctx.font = '12px Arial';
    this.ctx.fillText(
      skater.name,
      skater.position.x - 15,
      skater.position.y - 30
    );

    // Stamina bar
    if (skater.isPlayer) {
      const barWidth = 30;
      const barHeight = 4;
      const staminaRatio = skater.stamina / skater.maxStamina;

      this.ctx.fillStyle = '#333';
      this.ctx.fillRect(skater.position.x - barWidth / 2, skater.position.y + 25, barWidth, barHeight);
      
      this.ctx.fillStyle = staminaRatio > 0.5 ? '#4CAF50' : staminaRatio > 0.2 ? '#FFC107' : '#F44336';
      this.ctx.fillRect(skater.position.x - barWidth / 2, skater.position.y + 25, barWidth * staminaRatio, barHeight);
    }
  }

  private generateIceSpray(skater: Skater): void {
    if (skater.speed > 50 && Math.random() < 0.3) {
      for (let i = 0; i < 2; i++) {
        const particle: Particle = {
          x: skater.position.x - Math.cos(skater.angle) * 10,
          y: skater.position.y - Math.sin(skater.angle) * 10,
          vx: (Math.random() - 0.5) * 20 - Math.cos(skater.angle) * 30,
          vy: (Math.random() - 0.5) * 20 - Math.sin(skater.angle) * 30,
          life: 30,
          maxLife: 30,
          size: Math.random() * 3 + 1,
          color: '#ffffff'
        };
        this.particles.push(particle);
      }
    }
  }

  private renderParticles(): void {
    this.particles = this.particles.filter(particle => {
      particle.x += particle.vx * 0.016;
      particle.y += particle.vy * 0.016;
      particle.life--;
      particle.vy += 0.2; // Gravity

      const alpha = particle.life / particle.maxLife;
      this.ctx.globalAlpha = alpha;
      this.ctx.fillStyle = particle.color;
      this.ctx.beginPath();
      this.ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2);
      this.ctx.fill();
      this.ctx.globalAlpha = 1;

      return particle.life > 0;
    });
  }

  private renderPowerUps(powerUps: PowerUp[]): void {
    powerUps.forEach(powerUp => {
      if (!powerUp.active) return;

      this.ctx.save();
      this.ctx.translate(powerUp.position.x, powerUp.position.y);
      
      // Rotating power-up with pulsing effect
      const rotation = Date.now() * 0.005;
      const scale = 1 + Math.sin(Date.now() * 0.01) * 0.1;
      this.ctx.rotate(rotation);
      this.ctx.scale(scale, scale);

      // Power-up shape
      this.ctx.fillStyle = this.getPowerUpColor(powerUp.type);
      this.ctx.strokeStyle = '#fff';
      this.ctx.lineWidth = 2;
      
      this.ctx.beginPath();
      this.ctx.arc(0, 0, 12, 0, Math.PI * 2);
      this.ctx.fill();
      this.ctx.stroke();

      // Power-up icon
      this.ctx.fillStyle = '#fff';
      this.ctx.font = 'bold 12px Arial';
      this.ctx.textAlign = 'center';
      this.ctx.textBaseline = 'middle';
      
      const icon = powerUp.type === 'speed' ? '⚡' : powerUp.type === 'stamina' ? '❤' : '🧊';
      this.ctx.fillText(icon, 0, 0);

      this.ctx.restore();
    });
  }

  private getPowerUpColor(type: string): string {
    switch (type) {
      case 'speed': return '#FFD700';
      case 'stamina': return '#FF4444';
      case 'slip': return '#00BFFF';
      default: return '#FFFFFF';
    }
  }

  private renderUI(gameState: GameState): void {
    // Race info panel
    this.renderRaceInfo(gameState);
    
    // Leaderboard
    this.renderLeaderboard(gameState);
    
    // Speed meter for player
    this.renderSpeedMeter(gameState);
  }

  private renderRaceInfo(gameState: GameState): void {
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(10, 10, 200, 80);
    
    this.ctx.fillStyle = '#fff';
    this.ctx.font = '16px Arial';
    this.ctx.fillText(`Time: ${(gameState.gameTime / 1000).toFixed(1)}s`, 20, 30);
    this.ctx.fillText(`Max Laps: ${gameState.maxLaps}`, 20, 50);
    
    if (gameState.winner) {
      this.ctx.fillStyle = '#4CAF50';
      this.ctx.fillText(`Winner: ${gameState.winner.name}`, 20, 70);
    }
  }

  private renderLeaderboard(gameState: GameState): void {
    const leaderboard = this.getLeaderboard(gameState.skaters);
    
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(this.canvas.width - 220, 10, 210, Math.min(leaderboard.length * 25 + 20, 200));
    
    this.ctx.fillStyle = '#fff';
    this.ctx.font = '14px Arial';
    this.ctx.fillText('Leaderboard', this.canvas.width - 210, 30);
    
    leaderboard.slice(0, 6).forEach((skater, index) => {
      const y = 50 + index * 25;
      this.ctx.fillStyle = skater.color;
      this.ctx.fillRect(this.canvas.width - 210, y - 10, 15, 15);
      
      this.ctx.fillStyle = '#fff';
      this.ctx.fillText(
        `${index + 1}. ${skater.name} (${skater.lap})`,
        this.canvas.width - 190,
        y
      );
    });
  }

  private renderSpeedMeter(gameState: GameState): void {
    const player = gameState.skaters.find(s => s.isPlayer);
    if (!player) return;

    const centerX = this.canvas.width - 100;
    const centerY = this.canvas.height - 100;
    const radius = 50;

    // Speed meter background
    this.ctx.strokeStyle = '#333';
    this.ctx.lineWidth = 8;
    this.ctx.beginPath();
    this.ctx.arc(centerX, centerY, radius, Math.PI, 2 * Math.PI);
    this.ctx.stroke();

    // Speed indicator
    const speedRatio = player.speed / player.maxSpeed;
    const angle = Math.PI + speedRatio * Math.PI;
    
    this.ctx.strokeStyle = speedRatio > 0.8 ? '#FF4444' : speedRatio > 0.5 ? '#FFA500' : '#4CAF50';
    this.ctx.lineWidth = 6;
    this.ctx.beginPath();
    this.ctx.arc(centerX, centerY, radius, Math.PI, angle);
    this.ctx.stroke();

    // Speed text
    this.ctx.fillStyle = '#fff';
    this.ctx.font = '12px Arial';
    this.ctx.textAlign = 'center';
    this.ctx.fillText(`${Math.round(player.speed)} km/h`, centerX, centerY + 10);
  }

  private getLeaderboard(skaters: Skater[]): Skater[] {
    return [...skaters].sort((a, b) => {
      if (a.lap !== b.lap) return b.lap - a.lap;
      // Calculate track progress for same lap
      const aProgress = this.calculateTrackProgress(a);
      const bProgress = this.calculateTrackProgress(b);
      return bProgress - aProgress;
    });
  }

  private calculateTrackProgress(skater: Skater): number {
    const angle = Math.atan2(
      skater.position.y - this.track.centerY,
      skater.position.x - this.track.centerX
    );
    return (angle + Math.PI) / (2 * Math.PI);
  }

  resize(): void {
    this.canvas.width = window.innerWidth;
    this.canvas.height = window.innerHeight;
  }
}