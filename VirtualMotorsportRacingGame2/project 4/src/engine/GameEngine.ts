import { Car, Particle, PowerUp } from '../types/GameTypes';

export class GameEngine {
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  private playerCar: Car;
  private aiCars: Car[] = [];
  private particles: Particle[] = [];
  private powerUps: PowerUp[] = [];
  private trackX = 300;
  private trackY = 50;
  private trackWidth = 600;
  private trackHeight = 700;
  private innerMargin = 80;
  private totalLaps = 3;
  private raceFinished = false;
  private isPaused = false;
  private keys: { [key: string]: boolean } = {};
  private spawnTimer = 0;
  private finishLineY: number;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Could not get 2D context');
    this.ctx = ctx;

    this.finishLineY = this.trackY + this.trackHeight - 100;

    this.playerCar = {
      x: this.trackX + this.trackWidth / 2,
      y: this.finishLineY + 50,
      vx: 0,
      vy: 0,
      angle: 0,
      speed: 0,
      maxSpeed: 8,
      width: 40,
      height: 60,
      color: '#dc143c',
      lapCount: 0,
    };

    this.initializeAICars();
  }

  private initializeAICars() {
    const colors = ['#3296ff', '#32c832', '#ffc832', '#c832c8', '#32c8c8'];
    for (let i = 0; i < 5; i++) {
      this.aiCars.push({
        x: this.trackX + this.trackWidth / 2 - 150 + i * 60,
        y: this.finishLineY + 50 - (i + 1) * 80,
        vx: 0,
        vy: 0,
        angle: 0,
        speed: 0,
        maxSpeed: 6 + Math.random() * 2,
        width: 40,
        height: 60,
        color: colors[i],
        lapCount: 0,
      });
    }
  }

  public startRace() {
    this.raceFinished = false;
    this.playerCar.lapCount = 0;
    this.aiCars.forEach(car => (car.lapCount = 0));
  }

  public reset() {
    this.playerCar = {
      x: this.trackX + this.trackWidth / 2,
      y: this.finishLineY + 50,
      vx: 0,
      vy: 0,
      angle: 0,
      speed: 0,
      maxSpeed: 8,
      width: 40,
      height: 60,
      color: '#dc143c',
      lapCount: 0,
    };

    this.aiCars.forEach(car => {
      car.lapCount = 0;
      car.speed = 0;
    });

    this.particles = [];
    this.powerUps = [];
    this.raceFinished = false;
  }

  public setPaused(paused: boolean) {
    this.isPaused = paused;
  }

  public handleKeyDown(key: string) {
    this.keys[key.toLowerCase()] = true;
  }

  public handleKeyUp(key: string) {
    this.keys[key.toLowerCase()] = false;
  }

  public update() {
    if (this.raceFinished) return;

    this.updatePlayerCar();
    this.updateAICars();
    this.updateParticles();
    this.updatePowerUps();
    this.checkCollisions();
    this.spawnPowerUps();

    if (this.playerCar.lapCount >= this.totalLaps) {
      this.raceFinished = true;
    }
  }

  private updatePlayerCar() {
    const acceleration = 0.15;
    const deceleration = 0.1;

    if (this.keys['arrowup']) {
      this.playerCar.speed = Math.min(
        this.playerCar.speed + acceleration,
        this.playerCar.maxSpeed
      );
    } else if (this.keys['arrowdown']) {
      this.playerCar.speed = Math.max(
        this.playerCar.speed - deceleration * 2,
        -this.playerCar.maxSpeed / 2
      );
    } else {
      if (this.playerCar.speed > 0) {
        this.playerCar.speed = Math.max(this.playerCar.speed - deceleration, 0);
      } else if (this.playerCar.speed < 0) {
        this.playerCar.speed = Math.min(this.playerCar.speed + deceleration, 0);
      }
    }

    const turnSpeed = 0.05 * (Math.abs(this.playerCar.speed) / this.playerCar.maxSpeed);

    if (this.keys['arrowleft'] && Math.abs(this.playerCar.speed) > 0.5) {
      this.playerCar.angle -= turnSpeed;
    }
    if (this.keys['arrowright'] && Math.abs(this.playerCar.speed) > 0.5) {
      this.playerCar.angle += turnSpeed;
    }

    const dx = Math.sin(this.playerCar.angle) * this.playerCar.speed;
    const dy = -Math.cos(this.playerCar.angle) * this.playerCar.speed;

    const newX = this.playerCar.x + dx;
    const newY = this.playerCar.y + dy;

    if (this.isWithinBounds(newX, newY)) {
      this.playerCar.x = newX;
      this.playerCar.y = newY;
      this.checkLapCompletion(this.playerCar);
    } else {
      this.playerCar.speed *= 0.5;
      this.createCollisionEffect(this.playerCar.x, this.playerCar.y);
    }
  }

  private updateAICars() {
    for (const car of this.aiCars) {
      car.speed = Math.min(car.speed + 0.1, car.maxSpeed);

      if (Math.random() < 0.02) {
        car.angle += (Math.random() - 0.5) * 0.3;
      }

      const dx = Math.sin(car.angle) * car.speed;
      const dy = -Math.cos(car.angle) * car.speed;

      const newX = car.x + dx;
      const newY = car.y + dy;

      if (this.isWithinBounds(newX, newY)) {
        car.x = newX;
        car.y = newY;
        this.checkLapCompletion(car);
      } else {
        car.angle += Math.PI / 4;
        car.speed *= 0.7;
      }
    }
  }

  private checkLapCompletion(car: Car) {
    if (car.y > this.finishLineY && car.y < this.finishLineY + 50) {
      if (car.x > this.trackX + 50 && car.x < this.trackX + this.trackWidth - 50) {
        car.lapCount++;
      }
    }
  }

  private isWithinBounds(x: number, y: number): boolean {
    if (
      x < this.trackX + 20 ||
      x > this.trackX + this.trackWidth - 20 ||
      y < this.trackY + 20 ||
      y > this.trackY + this.trackHeight - 20
    ) {
      return false;
    }

    if (
      x > this.trackX + this.innerMargin + 20 &&
      x < this.trackX + this.trackWidth - this.innerMargin - 20 &&
      y > this.trackY + this.innerMargin + 20 &&
      y < this.trackY + this.trackHeight - this.innerMargin - 20
    ) {
      return false;
    }

    return true;
  }

  private checkCollisions() {
    const checkCollisionBetween = (car1: Car, car2: Car) => {
      const dx = car2.x - car1.x;
      const dy = car2.y - car1.y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance < 50) {
        const angle = Math.atan2(dy, dx);
        const separation = 25;

        car1.x -= Math.cos(angle) * separation;
        car1.y -= Math.sin(angle) * separation;
        car2.x += Math.cos(angle) * separation;
        car2.y += Math.sin(angle) * separation;

        car1.speed *= 0.7;
        car2.speed *= 0.7;

        this.createCollisionEffect(car1.x, car1.y);
      }
    };

    for (const aiCar of this.aiCars) {
      checkCollisionBetween(this.playerCar, aiCar);
    }

    for (let i = 0; i < this.aiCars.length; i++) {
      for (let j = i + 1; j < this.aiCars.length; j++) {
        checkCollisionBetween(this.aiCars[i], this.aiCars[j]);
      }
    }
  }

  private createCollisionEffect(x: number, y: number) {
    for (let i = 0; i < 20; i++) {
      const angle = (Math.PI * 2 * i) / 20;
      const speed = 2 + Math.random() * 3;
      this.particles.push({
        x,
        y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        lifetime: 0,
        maxLifetime: 30 + Math.random() * 30,
        color: '#ff6600',
        size: 4 + Math.random() * 4,
      });
    }
  }

  private updateParticles() {
    this.particles = this.particles.filter(p => p.lifetime < p.maxLifetime);
    for (const p of this.particles) {
      p.x += p.vx;
      p.y += p.vy;
      p.vy += 0.1;
      p.lifetime++;
    }
  }

  private spawnPowerUps() {
    this.spawnTimer++;
    if (this.spawnTimer > 300 && this.powerUps.length < 3) {
      this.powerUps.push({
        x: this.trackX + 100 + Math.random() * (this.trackWidth - 200),
        y: this.trackY + 100 + Math.random() * (this.trackHeight - 300),
        collected: false,
        size: 20,
        angle: 0,
      });
      this.spawnTimer = 0;
    }

    this.powerUps = this.powerUps.filter(p => !p.collected);

    for (const powerUp of this.powerUps) {
      if (this.checkPowerUpCollision(this.playerCar, powerUp)) {
        this.playerCar.speed = Math.min(this.playerCar.speed + 2, this.playerCar.maxSpeed * 1.5);
        powerUp.collected = true;
      }

      for (const aiCar of this.aiCars) {
        if (this.checkPowerUpCollision(aiCar, powerUp)) {
          aiCar.speed = Math.min(aiCar.speed + 2, aiCar.maxSpeed * 1.5);
          powerUp.collected = true;
        }
      }

      powerUp.angle += 0.05;
    }
  }

  private checkPowerUpCollision(car: Car, powerUp: PowerUp): boolean {
    const dx = car.x - powerUp.x;
    const dy = car.y - powerUp.y;
    const distance = Math.sqrt(dx * dx + dy * dy);
    return distance < 40;
  }

  public getPlayerPosition(): number {
    let position = 1;
    for (const aiCar of this.aiCars) {
      if (
        aiCar.lapCount > this.playerCar.lapCount ||
        (aiCar.lapCount === this.playerCar.lapCount && aiCar.y < this.playerCar.y)
      ) {
        position++;
      }
    }
    return position;
  }

  public getPlayerLap(): number {
    return this.playerCar.lapCount;
  }

  public getPlayerSpeed(): number {
    return Math.round((Math.abs(this.playerCar.speed) / this.playerCar.maxSpeed) * 100);
  }

  public isRaceFinished(): boolean {
    return this.raceFinished;
  }

  public render(gameState: string, isPaused: boolean) {
    this.ctx.fillStyle = '#0f172a';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    this.drawTrack();
    this.drawPowerUps();
    this.drawCars();
    this.drawParticles();

    if (gameState === 'MENU') {
      this.drawMenu();
    }

    if (isPaused) {
      this.drawPauseOverlay();
    }
  }

  private drawTrack() {
    this.ctx.fillStyle = '#2d5016';
    this.ctx.fillRect(this.trackX - 50, this.trackY - 50, this.trackWidth + 100, this.trackHeight + 100);

    this.ctx.fillStyle = '#3f3f47';
    this.ctx.fillRect(this.trackX, this.trackY, this.trackWidth, this.trackHeight);

    this.ctx.fillStyle = '#52525b';
    this.ctx.fillRect(
      this.trackX + this.innerMargin,
      this.trackY + this.innerMargin,
      this.trackWidth - this.innerMargin * 2,
      this.trackHeight - this.innerMargin * 2
    );

    this.ctx.strokeStyle = '#ffffff';
    this.ctx.lineWidth = 3;
    this.ctx.setLineDash([15, 15]);
    this.ctx.strokeRect(
      this.trackX + this.innerMargin,
      this.trackY + this.innerMargin,
      this.trackWidth - this.innerMargin * 2,
      this.trackHeight - this.innerMargin * 2
    );
    this.ctx.setLineDash([]);

    this.ctx.fillStyle = '#ffd700';
    this.ctx.fillRect(this.trackX + 50, this.finishLineY, this.trackWidth - 100, 3);

    for (let i = 0; i < 8; i++) {
      this.ctx.fillStyle = i % 2 === 0 ? '#ffffff' : '#000000';
      this.ctx.fillRect(this.trackX + 60 + i * 70, this.finishLineY + 5, 40, 20);
    }
  }

  private drawCars() {
    const allCars = [...this.aiCars, this.playerCar];
    allCars.sort((a, b) => a.y - b.y);

    for (const car of allCars) {
      this.drawCar(car);
    }
  }

  private drawCar(car: Car) {
    this.ctx.save();
    this.ctx.translate(car.x, car.y);
    this.ctx.rotate(car.angle);

    this.ctx.fillStyle = car.color;
    this.ctx.beginPath();
    this.ctx.moveTo(-car.width / 2, -car.height / 2);
    this.ctx.lineTo(car.width / 2, -car.height / 2 + 10);
    this.ctx.lineTo(car.width / 2, car.height / 2);
    this.ctx.lineTo(-car.width / 2, car.height / 2);
    this.ctx.fill();

    this.ctx.fillStyle = '#6496ff';
    this.ctx.fillRect(-car.width / 3, -car.height / 3, (car.width * 2) / 3, car.height / 3);

    this.ctx.fillStyle = '#000000';
    this.ctx.fillOval(-car.width / 2 + 5, car.height / 3, 12, 12);
    this.ctx.fillOval(car.width / 2 - 17, car.height / 3, 12, 12);

    if (Math.abs(car.speed) > 1) {
      this.ctx.fillStyle = 'rgba(255, 102, 0, 0.6)';
      for (let i = 0; i < 3; i++) {
        this.ctx.fillOval(-car.width / 2 - 10, car.height / 2 + i * 8, 10, 10);
      }
    }

    this.ctx.restore();
  }

  private drawPowerUps() {
    for (const powerUp of this.powerUps) {
      this.ctx.save();
      this.ctx.translate(powerUp.x, powerUp.y);
      this.ctx.rotate(powerUp.angle);

      this.ctx.fillStyle = '#ffd700';
      this.ctx.beginPath();
      for (let i = 0; i < 5; i++) {
        const angle = (i * 4 * Math.PI) / 5 - Math.PI / 2;
        const x = Math.cos(angle) * powerUp.size;
        const y = Math.sin(angle) * powerUp.size;
        if (i === 0) this.ctx.moveTo(x, y);
        else this.ctx.lineTo(x, y);
      }
      this.ctx.closePath();
      this.ctx.fill();

      this.ctx.fillStyle = '#ffffff';
      this.ctx.font = 'bold 14px Arial';
      this.ctx.textAlign = 'center';
      this.ctx.textBaseline = 'middle';
      this.ctx.fillText('B', 0, 0);

      this.ctx.restore();
    }
  }

  private drawParticles() {
    for (const p of this.particles) {
      const alpha = 1 - p.lifetime / p.maxLifetime;
      this.ctx.fillStyle = `rgba(255, 102, 0, ${alpha})`;
      this.ctx.fillOval(p.x, p.y, p.size, p.size);
    }
  }

  private drawMenu() {
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    this.ctx.fillStyle = '#ffd700';
    this.ctx.font = 'bold 60px Arial';
    this.ctx.textAlign = 'center';
    this.ctx.fillText('MOTORSPORT RACING', this.canvas.width / 2, 150);

    this.ctx.fillStyle = '#ffffff';
    this.ctx.font = 'bold 24px Arial';
    this.ctx.fillText('Press SPACE to Start', this.canvas.width / 2, 300);

    this.ctx.font = '18px Arial';
    this.ctx.fillText('Controls:', this.canvas.width / 2 - 100, 400);
    this.ctx.fillText('Arrow Keys - Steer', this.canvas.width / 2 - 100, 430);
    this.ctx.fillText('UP Arrow - Accelerate', this.canvas.width / 2 - 100, 460);
    this.ctx.fillText('DOWN Arrow - Brake', this.canvas.width / 2 - 100, 490);
    this.ctx.fillText('P - Pause', this.canvas.width / 2 - 100, 520);
  }

  private drawPauseOverlay() {
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    this.ctx.fillStyle = '#ffffff';
    this.ctx.font = 'bold 48px Arial';
    this.ctx.textAlign = 'center';
    this.ctx.fillText('PAUSED', this.canvas.width / 2, this.canvas.height / 2 - 30);

    this.ctx.font = '24px Arial';
    this.ctx.fillText('Press P to Resume', this.canvas.width / 2, this.canvas.height / 2 + 40);
  }
}

CanvasRenderingContext2D.prototype.fillOval = function(x: number, y: number, w: number, h: number) {
  this.beginPath();
  this.ellipse(x + w / 2, y + h / 2, w / 2, h / 2, 0, 0, 2 * Math.PI);
  this.fill();
};
