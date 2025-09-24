import React, { useRef, useEffect } from 'react';

const RollerbladingGame: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const gameRef = useRef<Game | null>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const game = new Game(canvas);
    gameRef.current = game;
    game.start();

    const handleKeyDown = (e: KeyboardEvent) => game.handleKeyDown(e);
    const handleKeyUp = (e: KeyboardEvent) => game.handleKeyUp(e);

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      game.stop();
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, []);

  return (
    <div className="min-h-screen bg-gradient-to-b from-sky-400 to-sky-600 flex flex-col items-center justify-center">
      <div className="mb-4 text-center">
        <h1 className="text-4xl font-bold text-white mb-2">🛼 Rollerblading Adventure</h1>
        <p className="text-white/80">Use ARROW KEYS to move, SPACE to jump, SHIFT to boost!</p>
      </div>
      <canvas
        ref={canvasRef}
        width={1000}
        height={600}
        className="border-4 border-white rounded-lg shadow-2xl bg-gradient-to-b from-blue-200 to-green-200"
      />
      <div className="mt-4 text-white/80 text-sm">
        Collect coins, avoid obstacles, and see how far you can roll!
      </div>
    </div>
  );
};

// Vector2 utility class for 2D math operations
class Vector2 {
  constructor(public x: number = 0, public y: number = 0) {}

  add(v: Vector2): Vector2 {
    return new Vector2(this.x + v.x, this.y + v.y);
  }

  subtract(v: Vector2): Vector2 {
    return new Vector2(this.x - v.x, this.y - v.y);
  }

  multiply(scalar: number): Vector2 {
    return new Vector2(this.x * scalar, this.y * scalar);
  }

  magnitude(): number {
    return Math.sqrt(this.x * this.x + this.y * this.y);
  }

  normalize(): Vector2 {
    const mag = this.magnitude();
    if (mag === 0) return new Vector2();
    return new Vector2(this.x / mag, this.y / mag);
  }

  distance(v: Vector2): number {
    return this.subtract(v).magnitude();
  }
}

// Particle system for visual effects
class Particle {
  position: Vector2;
  velocity: Vector2;
  life: number;
  maxLife: number;
  size: number;
  color: string;
  gravity: number;

  constructor(x: number, y: number, vx: number, vy: number, life: number, size: number, color: string) {
    this.position = new Vector2(x, y);
    this.velocity = new Vector2(vx, vy);
    this.life = life;
    this.maxLife = life;
    this.size = size;
    this.color = color;
    this.gravity = 0.1;
  }

  update(): void {
    this.position = this.position.add(this.velocity);
    this.velocity.y += this.gravity;
    this.velocity = this.velocity.multiply(0.99); // Air resistance
    this.life--;
  }

  draw(ctx: CanvasRenderingContext2D): void {
    const alpha = this.life / this.maxLife;
    ctx.save();
    ctx.globalAlpha = alpha;
    ctx.fillStyle = this.color;
    ctx.beginPath();
    ctx.arc(this.position.x, this.position.y, this.size * alpha, 0, Math.PI * 2);
    ctx.fill();
    ctx.restore();
  }

  isDead(): boolean {
    return this.life <= 0;
  }
}

// Collectible coins
class Coin {
  position: Vector2;
  collected: boolean = false;
  rotation: number = 0;
  pulseTimer: number = 0;
  size: number = 15;

  constructor(x: number, y: number) {
    this.position = new Vector2(x, y);
  }

  update(): void {
    this.rotation += 0.1;
    this.pulseTimer += 0.05;
  }

  draw(ctx: CanvasRenderingContext2D): void {
    if (this.collected) return;

    ctx.save();
    ctx.translate(this.position.x, this.position.y);
    ctx.rotate(this.rotation);
    
    const pulse = Math.sin(this.pulseTimer) * 2 + this.size;
    
    // Outer glow
    ctx.shadowColor = '#FFD700';
    ctx.shadowBlur = 10;
    
    // Coin body
    ctx.fillStyle = '#FFD700';
    ctx.beginPath();
    ctx.arc(0, 0, pulse, 0, Math.PI * 2);
    ctx.fill();
    
    // Inner detail
    ctx.fillStyle = '#FFA500';
    ctx.beginPath();
    ctx.arc(0, 0, pulse * 0.7, 0, Math.PI * 2);
    ctx.fill();
    
    // Dollar sign
    ctx.fillStyle = '#FFD700';
    ctx.font = `${pulse}px Arial`;
    ctx.textAlign = 'center';
    ctx.textBaseline = 'middle';
    ctx.fillText('$', 0, 0);
    
    ctx.restore();
  }

  checkCollision(playerBounds: { x: number, y: number, width: number, height: number }): boolean {
    if (this.collected) return false;
    
    const coinBounds = {
      x: this.position.x - this.size,
      y: this.position.y - this.size,
      width: this.size * 2,
      height: this.size * 2
    };
    
    return this.rectIntersection(playerBounds, coinBounds);
  }

  private rectIntersection(rect1: any, rect2: any): boolean {
    return rect1.x < rect2.x + rect2.width &&
           rect1.x + rect1.width > rect2.x &&
           rect1.y < rect2.y + rect2.height &&
           rect1.y + rect1.height > rect2.y;
  }

  collect(): void {
    this.collected = true;
  }
}

// Obstacles to avoid
class Obstacle {
  position: Vector2;
  size: Vector2;
  type: 'rock' | 'cone' | 'barrier';
  color: string;

  constructor(x: number, y: number, type: 'rock' | 'cone' | 'barrier') {
    this.position = new Vector2(x, y);
    this.type = type;
    
    switch (type) {
      case 'rock':
        this.size = new Vector2(30, 30);
        this.color = '#666';
        break;
      case 'cone':
        this.size = new Vector2(20, 40);
        this.color = '#FF6B35';
        break;
      case 'barrier':
        this.size = new Vector2(60, 80);
        this.color = '#8B4513';
        break;
    }
  }

  draw(ctx: CanvasRenderingContext2D): void {
    ctx.save();
    
    switch (this.type) {
      case 'rock':
        this.drawRock(ctx);
        break;
      case 'cone':
        this.drawCone(ctx);
        break;
      case 'barrier':
        this.drawBarrier(ctx);
        break;
    }
    
    ctx.restore();
  }

  private drawRock(ctx: CanvasRenderingContext2D): void {
    ctx.fillStyle = this.color;
    ctx.strokeStyle = '#444';
    ctx.lineWidth = 2;
    
    ctx.beginPath();
    ctx.ellipse(this.position.x, this.position.y, this.size.x / 2, this.size.y / 2, 0, 0, Math.PI * 2);
    ctx.fill();
    ctx.stroke();
    
    // Add some texture
    ctx.fillStyle = '#888';
    ctx.beginPath();
    ctx.ellipse(this.position.x - 5, this.position.y - 5, 4, 4, 0, 0, Math.PI * 2);
    ctx.fill();
  }

  private drawCone(ctx: CanvasRenderingContext2D): void {
    const x = this.position.x;
    const y = this.position.y;
    const w = this.size.x;
    const h = this.size.y;
    
    // Cone body
    ctx.fillStyle = this.color;
    ctx.beginPath();
    ctx.moveTo(x, y - h / 2);
    ctx.lineTo(x - w / 2, y + h / 2);
    ctx.lineTo(x + w / 2, y + h / 2);
    ctx.closePath();
    ctx.fill();
    
    // White stripes
    ctx.fillStyle = 'white';
    for (let i = 0; i < 3; i++) {
      const stripeY = y - h / 2 + (i + 1) * h / 4;
      const stripeW = w * (1 - (i + 1) / 4);
      ctx.fillRect(x - stripeW / 2, stripeY, stripeW, 3);
    }
  }

  private drawBarrier(ctx: CanvasRenderingContext2D): void {
    const x = this.position.x;
    const y = this.position.y;
    const w = this.size.x;
    const h = this.size.y;
    
    // Main barrier
    ctx.fillStyle = this.color;
    ctx.fillRect(x - w / 2, y - h / 2, w, h);
    
    // Metal bands
    ctx.fillStyle = '#666';
    ctx.fillRect(x - w / 2, y - h / 2, w, 5);
    ctx.fillRect(x - w / 2, y, w, 5);
    ctx.fillRect(x - w / 2, y + h / 2 - 5, w, 5);
    
    // Warning sign
    ctx.fillStyle = '#FFD700';
    ctx.font = '16px Arial';
    ctx.textAlign = 'center';
    ctx.fillText('⚠️', x, y);
  }

  checkCollision(playerBounds: { x: number, y: number, width: number, height: number }): boolean {
    const obstacleBounds = {
      x: this.position.x - this.size.x / 2,
      y: this.position.y - this.size.y / 2,
      width: this.size.x,
      height: this.size.y
    };
    
    return this.rectIntersection(playerBounds, obstacleBounds);
  }

  private rectIntersection(rect1: any, rect2: any): boolean {
    return rect1.x < rect2.x + rect2.width &&
           rect1.x + rect1.width > rect2.x &&
           rect1.y < rect2.y + rect2.height &&
           rect1.y + rect1.height > rect2.y;
  }
}

// Player character
class Player {
  position: Vector2;
  velocity: Vector2;
  size: Vector2;
  grounded: boolean = false;
  animationFrame: number = 0;
  facingRight: boolean = true;
  isJumping: boolean = false;
  isBoosting: boolean = false;
  boostCooldown: number = 0;
  health: number = 100;
  invulnerable: number = 0;
  rollerAnimationSpeed: number = 0;

  private readonly GRAVITY = 0.5;
  private readonly JUMP_STRENGTH = -12;
  private readonly MOVE_SPEED = 5;
  private readonly BOOST_SPEED = 8;
  private readonly GROUND_Y: number;

  constructor(x: number, y: number, groundY: number) {
    this.position = new Vector2(x, y);
    this.velocity = new Vector2(0, 0);
    this.size = new Vector2(30, 50);
    this.GROUND_Y = groundY;
  }

  update(keys: Set<string>): void {
    this.handleInput(keys);
    this.applyPhysics();
    this.updateAnimation();
    this.updateTimers();
  }

  private handleInput(keys: Set<string>): void {
    // Horizontal movement
    if (keys.has('ArrowLeft')) {
      this.velocity.x = -this.MOVE_SPEED;
      this.facingRight = false;
      this.rollerAnimationSpeed = 0.3;
    } else if (keys.has('ArrowRight')) {
      this.velocity.x = this.isBoosting ? this.BOOST_SPEED : this.MOVE_SPEED;
      this.facingRight = true;
      this.rollerAnimationSpeed = this.isBoosting ? 0.5 : 0.3;
    } else {
      this.velocity.x *= 0.8; // Deceleration
      this.rollerAnimationSpeed *= 0.95;
    }

    // Jumping
    if (keys.has(' ') && this.grounded) {
      this.velocity.y = this.JUMP_STRENGTH;
      this.grounded = false;
      this.isJumping = true;
    }

    // Boosting
    if (keys.has('Shift') && this.boostCooldown <= 0 && !this.isBoosting) {
      this.isBoosting = true;
      this.boostCooldown = 180; // 3 seconds at 60fps
    }

    if (this.isBoosting) {
      this.boostCooldown--;
      if (this.boostCooldown <= 120) { // Boost lasts 1 second
        this.isBoosting = false;
      }
    }
  }

  private applyPhysics(): void {
    // Apply gravity
    this.velocity.y += this.GRAVITY;

    // Update position
    this.position = this.position.add(this.velocity);

    // Ground collision
    if (this.position.y + this.size.y / 2 >= this.GROUND_Y) {
      this.position.y = this.GROUND_Y - this.size.y / 2;
      this.velocity.y = 0;
      this.grounded = true;
      this.isJumping = false;
    }

    // Screen boundaries
    if (this.position.x < this.size.x / 2) {
      this.position.x = this.size.x / 2;
    }
  }

  private updateAnimation(): void {
    if (Math.abs(this.velocity.x) > 0.1 || this.rollerAnimationSpeed > 0.1) {
      this.animationFrame += this.rollerAnimationSpeed;
    }
  }

  private updateTimers(): void {
    if (this.invulnerable > 0) {
      this.invulnerable--;
    }
    if (this.boostCooldown > 0) {
      this.boostCooldown = Math.max(0, this.boostCooldown - 1);
    }
  }

  draw(ctx: CanvasRenderingContext2D): void {
    ctx.save();
    
    // Flashing effect when invulnerable
    if (this.invulnerable > 0 && Math.floor(this.invulnerable / 5) % 2) {
      ctx.globalAlpha = 0.5;
    }

    // Boost glow effect
    if (this.isBoosting) {
      ctx.shadowColor = '#00FFFF';
      ctx.shadowBlur = 20;
    }

    const x = this.position.x;
    const y = this.position.y;
    const w = this.size.x;
    const h = this.size.y;

    // Flip for direction
    if (!this.facingRight) {
      ctx.scale(-1, 1);
      ctx.translate(-x * 2, 0);
    }

    // Body
    ctx.fillStyle = this.isBoosting ? '#00FFFF' : '#4169E1';
    ctx.fillRect(x - w/4, y - h/2, w/2, h * 0.7);

    // Head
    ctx.fillStyle = '#FFDBAC';
    ctx.beginPath();
    ctx.arc(x, y - h/2 - 8, 12, 0, Math.PI * 2);
    ctx.fill();

    // Helmet
    ctx.fillStyle = '#FF1493';
    ctx.beginPath();
    ctx.arc(x, y - h/2 - 8, 14, Math.PI, 0);
    ctx.fill();

    // Arms
    ctx.fillStyle = '#FFDBAC';
    const armSwing = Math.sin(this.animationFrame) * 10;
    ctx.fillRect(x - w/2, y - h/4 + armSwing, 8, h/3);
    ctx.fillRect(x + w/2 - 8, y - h/4 - armSwing, 8, h/3);

    // Legs
    ctx.fillStyle = '#4169E1';
    const legSwing = Math.sin(this.animationFrame + Math.PI) * 8;
    ctx.fillRect(x - w/4, y + h/4 + legSwing, 6, h/4);
    ctx.fillRect(x + w/4 - 6, y + h/4 - legSwing, 6, h/4);

    // Rollerblades
    ctx.fillStyle = '#000';
    const skateY = y + h/2 - 5;
    
    // Left skate
    ctx.fillRect(x - w/3, skateY + legSwing, 25, 8);
    this.drawWheels(ctx, x - w/3 + 5, skateY + 8 + legSwing);
    
    // Right skate
    ctx.fillRect(x + w/3 - 25, skateY - legSwing, 25, 8);
    this.drawWheels(ctx, x + w/3 - 20, skateY + 8 - legSwing);

    ctx.restore();
  }

  private drawWheels(ctx: CanvasRenderingContext2D, x: number, y: number): void {
    ctx.fillStyle = '#FFD700';
    const wheelSpin = this.animationFrame * 2;
    
    for (let i = 0; i < 4; i++) {
      ctx.save();
      ctx.translate(x + i * 4, y);
      ctx.rotate(wheelSpin);
      ctx.fillRect(-2, -2, 4, 4);
      ctx.restore();
    }
  }

  takeDamage(amount: number): void {
    if (this.invulnerable <= 0) {
      this.health -= amount;
      this.invulnerable = 120; // 2 seconds of invulnerability
    }
  }

  getBounds(): { x: number, y: number, width: number, height: number } {
    return {
      x: this.position.x - this.size.x / 2,
      y: this.position.y - this.size.y / 2,
      width: this.size.x,
      height: this.size.y
    };
  }
}

// Background management
class Background {
  private layers: Array<{ y: number, speed: number, color: string }> = [];
  private cloudPositions: Array<{ x: number, y: number, size: number }> = [];

  constructor(private width: number, private height: number) {
    this.initializeLayers();
    this.initializeClouds();
  }

  private initializeLayers(): void {
    this.layers = [
      { y: this.height * 0.8, speed: 0.2, color: '#228B22' }, // Far mountains
      { y: this.height * 0.85, speed: 0.4, color: '#32CD32' }, // Mid mountains
      { y: this.height * 0.9, speed: 0.6, color: '#90EE90' },  // Near hills
    ];
  }

  private initializeClouds(): void {
    for (let i = 0; i < 8; i++) {
      this.cloudPositions.push({
        x: Math.random() * this.width * 2,
        y: Math.random() * this.height * 0.3 + 50,
        size: Math.random() * 30 + 20
      });
    }
  }

  update(scrollSpeed: number): void {
    // Move clouds
    this.cloudPositions.forEach(cloud => {
      cloud.x -= scrollSpeed * 0.1;
      if (cloud.x < -cloud.size) {
        cloud.x = this.width + cloud.size;
        cloud.y = Math.random() * this.height * 0.3 + 50;
      }
    });
  }

  draw(ctx: CanvasRenderingContext2D, scrollOffset: number): void {
    // Sky gradient
    const gradient = ctx.createLinearGradient(0, 0, 0, this.height);
    gradient.addColorStop(0, '#87CEEB');
    gradient.addColorStop(1, '#E0F6FF');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, this.width, this.height);

    // Clouds
    ctx.fillStyle = 'rgba(255, 255, 255, 0.8)';
    this.cloudPositions.forEach(cloud => {
      this.drawCloud(ctx, cloud.x, cloud.y, cloud.size);
    });

    // Mountain layers
    this.layers.forEach(layer => {
      this.drawMountainLayer(ctx, layer, scrollOffset);
    });

    // Ground
    ctx.fillStyle = '#8FBC8F';
    ctx.fillRect(0, this.height * 0.95, this.width, this.height * 0.05);
    
    // Ground texture
    ctx.fillStyle = '#228B22';
    for (let x = 0; x < this.width; x += 20) {
      const grassHeight = Math.sin((x + scrollOffset) * 0.1) * 3;
      ctx.fillRect(x, this.height * 0.95 - grassHeight, 2, grassHeight + 10);
    }
  }

  private drawCloud(ctx: CanvasRenderingContext2D, x: number, y: number, size: number): void {
    ctx.beginPath();
    ctx.arc(x, y, size, 0, Math.PI * 2);
    ctx.arc(x + size * 0.6, y, size * 0.8, 0, Math.PI * 2);
    ctx.arc(x - size * 0.6, y, size * 0.8, 0, Math.PI * 2);
    ctx.arc(x + size * 0.3, y - size * 0.5, size * 0.6, 0, Math.PI * 2);
    ctx.arc(x - size * 0.3, y - size * 0.5, size * 0.6, 0, Math.PI * 2);
    ctx.fill();
  }

  private drawMountainLayer(ctx: CanvasRenderingContext2D, layer: { y: number, speed: number, color: string }, scrollOffset: number): void {
    ctx.fillStyle = layer.color;
    ctx.beginPath();
    ctx.moveTo(0, this.height);
    
    const offset = (scrollOffset * layer.speed) % (this.width * 2);
    for (let x = -offset; x < this.width + 200; x += 100) {
      const mountainHeight = Math.sin(x * 0.01) * 50 + Math.cos(x * 0.02) * 30;
      ctx.lineTo(x, layer.y + mountainHeight);
    }
    
    ctx.lineTo(this.width, this.height);
    ctx.closePath();
    ctx.fill();
  }
}

// Sound effects simulation (visual feedback)
class SoundEffect {
  private static createRipple(particles: Particle[], x: number, y: number, color: string): void {
    for (let i = 0; i < 8; i++) {
      const angle = (i / 8) * Math.PI * 2;
      const speed = 3;
      particles.push(new Particle(
        x, y,
        Math.cos(angle) * speed,
        Math.sin(angle) * speed,
        30, 3, color
      ));
    }
  }

  static coinCollect(particles: Particle[], x: number, y: number): void {
    this.createRipple(particles, x, y, '#FFD700');
    // Add sparkles
    for (let i = 0; i < 12; i++) {
      particles.push(new Particle(
        x + (Math.random() - 0.5) * 20,
        y + (Math.random() - 0.5) * 20,
        (Math.random() - 0.5) * 4,
        (Math.random() - 0.5) * 4 - 2,
        60, 2, '#FFFF00'
      ));
    }
  }

  static damage(particles: Particle[], x: number, y: number): void {
    this.createRipple(particles, x, y, '#FF4444');
  }

  static boost(particles: Particle[], x: number, y: number): void {
    for (let i = 0; i < 15; i++) {
      particles.push(new Particle(
        x + (Math.random() - 0.5) * 30,
        y + (Math.random() - 0.5) * 30,
        (Math.random() - 0.5) * 6,
        (Math.random() - 0.5) * 6,
        40, 4, '#00FFFF'
      ));
    }
  }
}

// Main game class
class Game {
  private canvas: HTMLCanvasElement;
  private ctx: CanvasRenderingContext2D;
  private player: Player;
  private background: Background;
  private coins: Coin[] = [];
  private obstacles: Obstacle[] = [];
  private particles: Particle[] = [];
  private keys: Set<string> = new Set();
  private animationId: number | null = null;
  
  // Game state
  private scrollOffset: number = 0;
  private scrollSpeed: number = 2;
  private score: number = 0;
  private distance: number = 0;
  private gameStarted: boolean = false;
  private gameOver: boolean = false;
  private lastObstacleSpawn: number = 0;
  private lastCoinSpawn: number = 0;
  private difficultyTimer: number = 0;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d')!;
    
    const groundY = canvas.height * 0.95 - 25;
    this.player = new Player(100, groundY, canvas.height * 0.95);
    this.background = new Background(canvas.width, canvas.height);
    
    this.initializeLevel();
  }

  private initializeLevel(): void {
    // Spawn initial coins and obstacles
    for (let i = 0; i < 10; i++) {
      if (Math.random() < 0.6) {
        const x = 300 + i * 200 + Math.random() * 100;
        const y = this.canvas.height * 0.8 - Math.random() * 100;
        this.coins.push(new Coin(x, y));
      }
      
      if (i > 2 && Math.random() < 0.4) {
        const x = 400 + i * 250 + Math.random() * 100;
        const y = this.canvas.height * 0.95 - 20;
        const types: ('rock' | 'cone' | 'barrier')[] = ['rock', 'cone', 'barrier'];
        const type = types[Math.floor(Math.random() * types.length)];
        this.obstacles.push(new Obstacle(x, y, type));
      }
    }
  }

  start(): void {
    this.gameLoop();
  }

  stop(): void {
    if (this.animationId) {
      cancelAnimationFrame(this.animationId);
    }
  }

  handleKeyDown(e: KeyboardEvent): void {
    this.keys.add(e.code);
    
    if (!this.gameStarted && (e.code === 'Space' || e.code === 'ArrowRight')) {
      this.gameStarted = true;
    }
    
    if (this.gameOver && e.code === 'KeyR') {
      this.resetGame();
    }
    
    e.preventDefault();
  }

  handleKeyUp(e: KeyboardEvent): void {
    this.keys.delete(e.code);
    e.preventDefault();
  }

  private gameLoop = (): void => {
    this.update();
    this.draw();
    this.animationId = requestAnimationFrame(this.gameLoop);
  };

  private update(): void {
    if (!this.gameStarted || this.gameOver) return;
    
    // Update player
    this.player.update(this.keys);
    
    // Update scroll speed and difficulty
    this.difficultyTimer++;
    if (this.difficultyTimer % 600 === 0) { // Every 10 seconds
      this.scrollSpeed = Math.min(this.scrollSpeed + 0.2, 6);
    }
    
    // Update scroll offset
    this.scrollOffset += this.scrollSpeed;
    this.distance = Math.floor(this.scrollOffset / 10);
    
    // Update background
    this.background.update(this.scrollSpeed);
    
    // Update coins
    this.coins.forEach(coin => {
      coin.update();
      coin.position.x -= this.scrollSpeed;
      
      if (coin.checkCollision(this.player.getBounds())) {
        coin.collect();
        this.score += 10;
        SoundEffect.coinCollect(this.particles, coin.position.x, coin.position.y);
      }
    });
    
    // Update obstacles
    this.obstacles.forEach(obstacle => {
      obstacle.position.x -= this.scrollSpeed;
      
      if (obstacle.checkCollision(this.player.getBounds())) {
        this.player.takeDamage(25);
        SoundEffect.damage(this.particles, this.player.position.x, this.player.position.y);
      }
    });
    
    // Update particles
    this.particles = this.particles.filter(particle => {
      particle.update();
      return !particle.isDead();
    });
    
    // Boost particles
    if (this.player.isBoosting && Math.random() < 0.3) {
      SoundEffect.boost(this.particles, this.player.position.x - 20, this.player.position.y + 10);
    }
    
    // Spawn new elements
    this.spawnElements();
    
    // Clean up old elements
    this.coins = this.coins.filter(coin => coin.position.x > -50);
    this.obstacles = this.obstacles.filter(obstacle => obstacle.position.x > -100);
    
    // Check game over
    if (this.player.health <= 0) {
      this.gameOver = true;
    }
  }

  private spawnElements(): void {
    // Spawn coins
    if (this.scrollOffset - this.lastCoinSpawn > 150 && Math.random() < 0.4) {
      const x = this.canvas.width + Math.random() * 200;
      const y = this.canvas.height * 0.8 - Math.random() * 150;
      this.coins.push(new Coin(x, y));
      this.lastCoinSpawn = this.scrollOffset;
    }
    
    // Spawn obstacles
    if (this.scrollOffset - this.lastObstacleSpawn > 200 && Math.random() < 0.3) {
      const x = this.canvas.width + Math.random() * 100;
      const y = this.canvas.height * 0.95 - 20;
      const types: ('rock' | 'cone' | 'barrier')[] = ['rock', 'cone', 'barrier'];
      const type = types[Math.floor(Math.random() * types.length)];
      this.obstacles.push(new Obstacle(x, y, type));
      this.lastObstacleSpawn = this.scrollOffset;
    }
  }

  private draw(): void {
    // Clear canvas
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
    
    // Draw background
    this.background.draw(this.ctx, this.scrollOffset);
    
    if (!this.gameStarted) {
      this.drawStartScreen();
      return;
    }
    
    // Draw game objects
    this.coins.forEach(coin => coin.draw(this.ctx));
    this.obstacles.forEach(obstacle => obstacle.draw(this.ctx));
    this.player.draw(this.ctx);
    this.particles.forEach(particle => particle.draw(this.ctx));
    
    // Draw UI
    this.drawUI();
    
    if (this.gameOver) {
      this.drawGameOverScreen();
    }
  }

  private drawStartScreen(): void {
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    
    this.ctx.fillStyle = 'white';
    this.ctx.font = 'bold 48px Arial';
    this.ctx.textAlign = 'center';
    this.ctx.fillText('Rollerblading Adventure', this.canvas.width / 2, this.canvas.height / 2 - 50);
    
    this.ctx.font = '24px Arial';
    this.ctx.fillText('Press SPACE or → to start!', this.canvas.width / 2, this.canvas.height / 2 + 20);
    
    this.ctx.font = '18px Arial';
    this.ctx.fillText('Arrow Keys: Move • Space: Jump • Shift: Boost', this.canvas.width / 2, this.canvas.height / 2 + 60);
  }

  private drawUI(): void {
    // Score
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(10, 10, 200, 80);
    
    this.ctx.fillStyle = 'white';
    this.ctx.font = 'bold 20px Arial';
    this.ctx.textAlign = 'left';
    this.ctx.fillText(`Score: ${this.score}`, 20, 35);
    this.ctx.fillText(`Distance: ${this.distance}m`, 20, 60);
    
    // Health bar
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(this.canvas.width - 210, 10, 200, 30);
    
    this.ctx.fillStyle = '#ff4444';
    this.ctx.fillRect(this.canvas.width - 200, 20, 180, 10);
    
    this.ctx.fillStyle = '#44ff44';
    const healthWidth = (this.player.health / 100) * 180;
    this.ctx.fillRect(this.canvas.width - 200, 20, healthWidth, 10);
    
    this.ctx.fillStyle = 'white';
    this.ctx.font = '14px Arial';
    this.ctx.textAlign = 'right';
    this.ctx.fillText(`Health: ${this.player.health}%`, this.canvas.width - 20, 55);
    
    // Boost meter
    if (this.player.boostCooldown > 0) {
      const boostProgress = (180 - this.player.boostCooldown) / 180;
      this.ctx.fillStyle = 'rgba(0, 255, 255, 0.7)';
      this.ctx.fillRect(this.canvas.width - 200, 65, 180 * boostProgress, 5);
      
      this.ctx.fillStyle = 'white';
      this.ctx.font = '12px Arial';
      this.ctx.fillText(this.player.isBoosting ? 'BOOSTING!' : 'Boost Recharging...', this.canvas.width - 20, 85);
    } else {
      this.ctx.fillStyle = 'white';
      this.ctx.font = '12px Arial';
      this.ctx.fillText('Boost Ready! (Hold Shift)', this.canvas.width - 20, 85);
    }
  }

  private drawGameOverScreen(): void {
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);
    
    this.ctx.fillStyle = 'white';
    this.ctx.font = 'bold 48px Arial';
    this.ctx.textAlign = 'center';
    this.ctx.fillText('Game Over!', this.canvas.width / 2, this.canvas.height / 2 - 60);
    
    this.ctx.font = '24px Arial';
    this.ctx.fillText(`Final Score: ${this.score}`, this.canvas.width / 2, this.canvas.height / 2 - 10);
    this.ctx.fillText(`Distance: ${this.distance}m`, this.canvas.width / 2, this.canvas.height / 2 + 20);
    
    this.ctx.font = '18px Arial';
    this.ctx.fillText('Press R to restart', this.canvas.width / 2, this.canvas.height / 2 + 70);
  }

  private resetGame(): void {
    this.player = new Player(100, this.canvas.height * 0.95 - 25, this.canvas.height * 0.95);
    this.coins = [];
    this.obstacles = [];
    this.particles = [];
    this.scrollOffset = 0;
    this.scrollSpeed = 2;
    this.score = 0;
    this.distance = 0;
    this.gameStarted = false;
    this.gameOver = false;
    this.lastObstacleSpawn = 0;
    this.lastCoinSpawn = 0;
    this.difficultyTimer = 0;
    
    this.initializeLevel();
  }
}

function App() {
  return <RollerbladingGame />;
}

export default App;