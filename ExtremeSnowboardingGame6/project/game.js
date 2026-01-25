const canvas = document.getElementById('gameCanvas');
const ctx = canvas.getContext('2d');

const GAME_WIDTH = 800;
const GAME_HEIGHT = 600;
const GROUND_HEIGHT = 500;
const FPS = 60;
const GAME_SPEED = 5;

const PLAYER_WIDTH = 40;
const PLAYER_HEIGHT = 60;
const JUMP_STRENGTH = 15;
const GRAVITY = 0.6;
const MAX_SPEED = 10;

const GameState = {
    MENU: 'MENU',
    PLAYING: 'PLAYING',
    PAUSED: 'PAUSED',
    GAME_OVER: 'GAME_OVER'
};

const ObstacleType = {
    ROCK: 'ROCK',
    TREE_STUMP: 'TREE_STUMP',
    ICE_BLOCK: 'ICE_BLOCK'
};

class Game {
    constructor() {
        this.currentState = GameState.MENU;
        this.keys = {};
        this.setupEventListeners();

        this.initializeGame();

        this.lastFrameTime = Date.now();
        this.gameLoop();
    }

    initializeGame() {
        // Player state
        this.playerX = 100;
        this.playerY = GROUND_HEIGHT - PLAYER_HEIGHT;
        this.playerVelocityX = 0;
        this.playerVelocityY = 0;
        this.isJumping = false;
        this.isGrounded = true;
        this.playerRotation = 0;
        this.currentTrick = '';
        this.trickTimer = 0;

        // Game objects
        this.obstacles = [];
        this.ramps = [];
        this.coins = [];
        this.particles = [];
        this.trees = [];

        // Background
        this.backgroundOffset = 0;
        this.mountainOffset = 0;

        // Game stats
        this.score = 0;
        this.coinsCollected = 0;
        this.tricksPerformed = 0;
        this.distanceTraveled = 0;
        this.highScore = localStorage.getItem('highScore') || 0;
        this.combo = 0;
        this.comboTimer = 0;

        // Animation
        this.animationFrame = 0;
        this.frameCounter = 0;

        // Initialize background
        for (let i = 0; i < 15; i++) {
            this.trees.push(new Tree(i * 150 + Math.random() * 100, GROUND_HEIGHT - 80 - Math.random() * 20));
        }

        this.spawnInitialObjects();
    }

    spawnInitialObjects() {
        for (let i = 0; i < 5; i++) {
            const x = GAME_WIDTH + i * 300 + Math.random() * 200;

            if (Math.random() < 0.4) {
                const types = [ObstacleType.ROCK, ObstacleType.TREE_STUMP, ObstacleType.ICE_BLOCK];
                const type = types[Math.floor(Math.random() * types.length)];
                this.obstacles.push(new Obstacle(x, GROUND_HEIGHT - 40, type));
            } else if (Math.random() < 0.3) {
                this.ramps.push(new Ramp(x, GROUND_HEIGHT - 30));
            }

            if (Math.random() < 0.6) {
                const coinY = GROUND_HEIGHT - 80 - Math.random() * 100;
                this.coins.push(new Coin(x + 50, coinY));
            }
        }
    }

    setupEventListeners() {
        window.addEventListener('keydown', (e) => {
            this.keys[e.key] = true;
            this.handleKeyPress(e.key);
        });

        window.addEventListener('keyup', (e) => {
            this.keys[e.key] = false;
        });
    }

    handleKeyPress(key) {
        if (this.currentState === GameState.MENU) {
            if (key === ' ') {
                this.currentState = GameState.PLAYING;
                this.initializeGame();
            }
        } else if (this.currentState === GameState.PLAYING) {
            if (key === ' ' && this.isGrounded) {
                this.playerVelocityY = -JUMP_STRENGTH;
                this.isJumping = true;
                this.isGrounded = false;
            } else if (key === 'ArrowUp' && this.isJumping && this.currentTrick === '') {
                this.currentTrick = 'Frontflip';
                this.trickTimer = 60;
            } else if (key === 'ArrowDown' && this.isJumping && this.currentTrick === '') {
                this.currentTrick = 'Backflip';
                this.trickTimer = 60;
            } else if (key === 'p' || key === 'P') {
                this.currentState = GameState.PAUSED;
            }
        } else if (this.currentState === GameState.PAUSED) {
            if (key === 'p' || key === 'P') {
                this.currentState = GameState.PLAYING;
            }
        } else if (this.currentState === GameState.GAME_OVER) {
            if (key === ' ') {
                this.currentState = GameState.MENU;
            }
        }
    }

    updateGameLogic() {
        this.frameCounter++;
        if (this.frameCounter % 10 === 0) {
            this.animationFrame = (this.animationFrame + 1) % 4;
        }

        this.updatePlayerPhysics();
        this.backgroundOffset += GAME_SPEED * 0.3;
        this.mountainOffset += GAME_SPEED * 0.5;
        this.distanceTraveled += GAME_SPEED;

        this.updateObstacles();
        this.updateRamps();
        this.updateCoins();
        this.updateParticles();
        this.updateTrees();

        this.spawnObjects();

        if (this.trickTimer > 0) {
            this.trickTimer--;
            if (this.trickTimer === 0) {
                this.currentTrick = '';
            }
        }

        if (this.comboTimer > 0) {
            this.comboTimer--;
            if (this.comboTimer === 0) {
                this.combo = 0;
            }
        }

        this.checkCollisions();
        this.score += 1;

        this.updateUIDisplay();
    }

    updatePlayerPhysics() {
        if (!this.isGrounded) {
            this.playerVelocityY += GRAVITY;
        }

        this.playerY += this.playerVelocityY;
        this.playerX += this.playerVelocityX;

        if (this.playerY >= GROUND_HEIGHT - PLAYER_HEIGHT) {
            this.playerY = GROUND_HEIGHT - PLAYER_HEIGHT;
            this.playerVelocityY = 0;
            this.isGrounded = true;
            this.isJumping = false;
            this.playerRotation = 0;

            if (this.currentTrick !== '') {
                const trickScore = this.getTrickScore(this.currentTrick);
                this.score += trickScore * (this.combo + 1);
                this.combo++;
                this.comboTimer = 120;
                this.tricksPerformed++;
                this.createParticleBurst(this.playerX + PLAYER_WIDTH / 2, this.playerY + PLAYER_HEIGHT);
                this.currentTrick = '';
            }
        } else {
            this.isGrounded = false;
        }

        if (this.playerX < 0) {
            this.playerX = 0;
            this.playerVelocityX = 0;
        }
        if (this.playerX > GAME_WIDTH - PLAYER_WIDTH) {
            this.playerX = GAME_WIDTH - PLAYER_WIDTH;
            this.playerVelocityX = 0;
        }

        if (this.playerRotation !== 0) {
            this.playerRotation += 10;
            if (this.playerRotation >= 360) {
                this.playerRotation = 0;
            }
        }

        this.playerVelocityX *= 0.95;

        // Handle left/right movement
        if (this.keys['ArrowLeft']) {
            this.playerVelocityX = Math.max(this.playerVelocityX - 2, -MAX_SPEED);
        }
        if (this.keys['ArrowRight']) {
            this.playerVelocityX = Math.min(this.playerVelocityX + 2, MAX_SPEED);
        }
    }

    updateObstacles() {
        for (let i = this.obstacles.length - 1; i >= 0; i--) {
            this.obstacles[i].x -= GAME_SPEED;
            if (this.obstacles[i].x < -50) {
                this.obstacles.splice(i, 1);
            }
        }
    }

    updateRamps() {
        for (let i = this.ramps.length - 1; i >= 0; i--) {
            this.ramps[i].x -= GAME_SPEED;
            if (this.ramps[i].x < -100) {
                this.ramps.splice(i, 1);
            }
        }
    }

    updateCoins() {
        for (let i = this.coins.length - 1; i >= 0; i--) {
            this.coins[i].x -= GAME_SPEED;
            this.coins[i].rotation += 5;
            if (this.coins[i].x < -30) {
                this.coins.splice(i, 1);
            }
        }
    }

    updateParticles() {
        for (let i = this.particles.length - 1; i >= 0; i--) {
            this.particles[i].update();
            if (this.particles[i].life <= 0) {
                this.particles.splice(i, 1);
            }
        }
    }

    updateTrees() {
        for (let i = this.trees.length - 1; i >= 0; i--) {
            this.trees[i].x -= GAME_SPEED * 0.7;
            if (this.trees[i].x < -100) {
                this.trees.splice(i, 1);
            }
        }
    }

    spawnObjects() {
        if (Math.random() < 0.02) {
            const types = [ObstacleType.ROCK, ObstacleType.TREE_STUMP, ObstacleType.ICE_BLOCK];
            const type = types[Math.floor(Math.random() * types.length)];
            this.obstacles.push(new Obstacle(GAME_WIDTH + 50, GROUND_HEIGHT - 40, type));
        }

        if (Math.random() < 0.015) {
            this.ramps.push(new Ramp(GAME_WIDTH + 50, GROUND_HEIGHT - 30));
        }

        if (Math.random() < 0.03) {
            const coinY = GROUND_HEIGHT - 80 - Math.random() * 150;
            this.coins.push(new Coin(GAME_WIDTH + 50, coinY));
        }

        if (Math.random() < 0.05) {
            this.trees.push(new Tree(GAME_WIDTH + 50, GROUND_HEIGHT - 80 - Math.random() * 20));
        }
    }

    checkCollisions() {
        const playerRect = {
            x: this.playerX,
            y: this.playerY,
            width: PLAYER_WIDTH,
            height: PLAYER_HEIGHT
        };

        // Obstacle collisions
        for (let obs of this.obstacles) {
            if (this.rectsIntersect(playerRect, { x: obs.x, y: obs.y, width: 40, height: 40 })) {
                this.gameOver();
                return;
            }
        }

        // Ramp collisions
        for (let ramp of this.ramps) {
            if (this.rectsIntersect(playerRect, { x: ramp.x, y: ramp.y, width: 80, height: 30 }) && this.isGrounded) {
                this.playerVelocityY = -18;
                this.isJumping = true;
                this.isGrounded = false;
                this.score += 50;
            }
        }

        // Coin collisions
        for (let i = this.coins.length - 1; i >= 0; i--) {
            const coin = this.coins[i];
            if (this.rectsIntersect(playerRect, { x: coin.x, y: coin.y, width: 25, height: 25 })) {
                this.coins.splice(i, 1);
                this.coinsCollected++;
                this.score += 100;
                this.createParticleBurst(coin.x + 12, coin.y + 12);
            }
        }
    }

    rectsIntersect(rect1, rect2) {
        return rect1.x < rect2.x + rect2.width &&
               rect1.x + rect1.width > rect2.x &&
               rect1.y < rect2.y + rect2.height &&
               rect1.y + rect1.height > rect2.y;
    }

    createParticleBurst(x, y) {
        for (let i = 0; i < 10; i++) {
            const angle = Math.random() * Math.PI * 2;
            const speed = 2 + Math.random() * 3;
            const color = `rgb(255, ${200 + Math.floor(Math.random() * 55)}, ${Math.floor(Math.random() * 100)})`;
            this.particles.push(new Particle(x, y, Math.cos(angle) * speed, Math.sin(angle) * speed, color));
        }
    }

    getTrickScore(trick) {
        switch (trick) {
            case 'Frontflip': return 500;
            case 'Backflip': return 500;
            default: return 0;
        }
    }

    gameOver() {
        this.currentState = GameState.GAME_OVER;
        if (this.score > this.highScore) {
            this.highScore = this.score;
            localStorage.setItem('highScore', this.highScore);
        }
    }

    updateUIDisplay() {
        document.getElementById('score').textContent = this.score;
        document.getElementById('coins').textContent = this.coinsCollected;
        document.getElementById('distance').textContent = Math.floor(this.distanceTraveled / 10);
        document.getElementById('tricks').textContent = this.tricksPerformed;
        document.getElementById('highScore').textContent = this.highScore;
    }

    render() {
        switch (this.currentState) {
            case GameState.MENU:
                this.drawMenu();
                break;
            case GameState.PLAYING:
                this.drawGame();
                break;
            case GameState.PAUSED:
                this.drawGame();
                this.drawPauseMenu();
                break;
            case GameState.GAME_OVER:
                this.drawGame();
                this.drawGameOver();
                break;
        }
    }

    drawMenu() {
        this.drawBackground();
        this.drawMountains();

        ctx.fillStyle = 'rgba(255, 255, 255, 0.9)';
        ctx.font = 'bold 60px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('EXTREME', GAME_WIDTH / 2, 120);
        ctx.fillText('SNOWBOARDING', GAME_WIDTH / 2, 190);

        ctx.font = '24px Arial';
        ctx.fillStyle = '#333';
        ctx.fillText('Press SPACE to Start', GAME_WIDTH / 2, 300);

        ctx.font = '18px Arial';
        ctx.textAlign = 'left';
        ctx.fillText('Controls:', 80, 380);
        ctx.fillText('← → Move | SPACE Jump', 80, 410);
        ctx.fillText('↑ Frontflip | ↓ Backflip', 80, 440);
        ctx.fillText('P Pause', 80, 470);

        ctx.textAlign = 'center';
        ctx.font = 'bold 20px Arial';
        ctx.fillText(`High Score: ${this.highScore}`, GAME_WIDTH / 2, 550);
    }

    drawGame() {
        this.drawBackground();
        this.drawMountains();

        for (let tree of this.trees) {
            tree.draw(ctx);
        }

        this.drawGround();

        for (let ramp of this.ramps) {
            ramp.draw(ctx);
        }

        for (let obs of this.obstacles) {
            obs.draw(ctx);
        }

        for (let coin of this.coins) {
            coin.draw(ctx);
        }

        for (let particle of this.particles) {
            particle.draw(ctx);
        }

        this.drawPlayer();
        this.drawHUD();
    }

    drawBackground() {
        const gradient = ctx.createLinearGradient(0, 0, 0, GROUND_HEIGHT);
        gradient.addColorStop(0, '#87CEEB');
        gradient.addColorStop(1, '#E0F6FF');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, 0, GAME_WIDTH, GROUND_HEIGHT);

        // Clouds
        ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
        const cloudOffset = this.backgroundOffset % 800;
        for (let i = 0; i < 4; i++) {
            const x = i * 300 - cloudOffset;
            this.drawCloud(x, 50 + i * 30);
        }
    }

    drawCloud(x, y) {
        ctx.beginPath();
        ctx.arc(x, y, 30, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(x + 25, y - 15, 35, 0, Math.PI * 2);
        ctx.fill();
        ctx.beginPath();
        ctx.arc(x + 50, y, 30, 0, Math.PI * 2);
        ctx.fill();
    }

    drawMountains() {
        const offset = this.mountainOffset % 1000;

        ctx.fillStyle = '#64788C';
        for (let i = 0; i < 3; i++) {
            const x = i * 400 - offset;
            ctx.beginPath();
            ctx.moveTo(x, GROUND_HEIGHT - 50);
            ctx.lineTo(x + 200, GROUND_HEIGHT - 250);
            ctx.lineTo(x + 400, GROUND_HEIGHT - 50);
            ctx.fill();
        }

        ctx.fillStyle = '#4A5568';
        for (let i = 0; i < 3; i++) {
            const x = i * 400 - offset + 50;
            ctx.beginPath();
            ctx.moveTo(x, GROUND_HEIGHT - 50);
            ctx.lineTo(x + 150, GROUND_HEIGHT - 180);
            ctx.lineTo(x + 300, GROUND_HEIGHT - 50);
            ctx.fill();
        }
    }

    drawGround() {
        const gradient = ctx.createLinearGradient(0, GROUND_HEIGHT, 0, GAME_HEIGHT);
        gradient.addColorStop(0, '#F0F8FF');
        gradient.addColorStop(1, '#C8DCFA');
        ctx.fillStyle = gradient;
        ctx.fillRect(0, GROUND_HEIGHT, GAME_WIDTH, GAME_HEIGHT - GROUND_HEIGHT);

        // Snow tracks
        ctx.fillStyle = '#DCE6F0';
        const trackOffset = (this.backgroundOffset * 2) % 50;
        for (let i = 0; i < GAME_WIDTH / 25; i++) {
            const x = i * 25 - trackOffset;
            ctx.fillRect(x, GROUND_HEIGHT + 5, 15, 3);
        }
    }

    drawPlayer() {
        ctx.save();
        ctx.translate(this.playerX + PLAYER_WIDTH / 2, this.playerY + PLAYER_HEIGHT / 2);
        ctx.rotate((this.playerRotation * Math.PI) / 180);
        ctx.translate(-(this.playerX + PLAYER_WIDTH / 2), -(this.playerY + PLAYER_HEIGHT / 2));

        // Body
        ctx.fillStyle = '#0064C8';
        ctx.fillRect(this.playerX + 10, this.playerY + 10, 20, 35);
        ctx.fillRect(this.playerX + 5, this.playerY + 15, 8, 20);
        ctx.fillRect(this.playerX + 27, this.playerY + 15, 8, 20);

        // Head
        ctx.fillStyle = '#FFDBAC';
        ctx.beginPath();
        ctx.arc(this.playerX + 20, this.playerY + 8, 8, 0, Math.PI * 2);
        ctx.fill();

        // Helmet
        ctx.fillStyle = '#FF0000';
        ctx.beginPath();
        ctx.arc(this.playerX + 20, this.playerY + 5, 10, 0, Math.PI);
        ctx.fill();

        // Goggles
        ctx.fillStyle = '#323232';
        ctx.fillRect(this.playerX + 13, this.playerY + 6, 14, 4);

        // Snowboard
        ctx.fillStyle = '#FF9600';
        if (this.isGrounded) {
            ctx.fillRect(this.playerX, this.playerY + PLAYER_HEIGHT - 8, PLAYER_WIDTH, 8);
        } else {
            ctx.fillRect(this.playerX - 5, this.playerY + PLAYER_HEIGHT - 5, PLAYER_WIDTH + 10, 8);
        }

        ctx.restore();

        // Trick name
        if (this.currentTrick !== '') {
            ctx.fillStyle = '#FFFF00';
            ctx.font = 'bold 16px Arial';
            ctx.textAlign = 'center';
            ctx.fillText(this.currentTrick, this.playerX + PLAYER_WIDTH / 2, this.playerY - 20);
        }
    }

    drawHUD() {
        ctx.fillStyle = 'rgba(255, 255, 255, 0.85)';
        ctx.font = 'bold 18px Arial';
        ctx.textAlign = 'left';

        ctx.fillText(`Score: ${this.score}`, 10, 30);
        ctx.fillText(`Coins: ${this.coinsCollected}`, 10, 55);
        ctx.fillText(`Distance: ${Math.floor(this.distanceTraveled / 10)}m`, 10, 80);
        ctx.fillText(`Tricks: ${this.tricksPerformed}`, 10, 105);

        if (this.combo > 0) {
            ctx.fillStyle = '#FFFF00';
            ctx.font = 'bold 24px Arial';
            ctx.textAlign = 'right';
            ctx.fillText(`COMBO x${this.combo}`, GAME_WIDTH - 10, 30);
        }
    }

    drawPauseMenu() {
        ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
        ctx.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        ctx.fillStyle = '#FFFFFF';
        ctx.font = 'bold 48px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('PAUSED', GAME_WIDTH / 2, GAME_HEIGHT / 2 - 50);

        ctx.font = '24px Arial';
        ctx.fillText('Press P to Resume', GAME_WIDTH / 2, GAME_HEIGHT / 2 + 50);
    }

    drawGameOver() {
        ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
        ctx.fillRect(0, 0, GAME_WIDTH, GAME_HEIGHT);

        ctx.fillStyle = '#FFFFFF';
        ctx.font = 'bold 48px Arial';
        ctx.textAlign = 'center';
        ctx.fillText('GAME OVER', GAME_WIDTH / 2, 150);

        ctx.font = 'bold 24px Arial';
        ctx.fillText(`Final Score: ${this.score}`, GAME_WIDTH / 2, 230);
        ctx.fillText(`Coins: ${this.coinsCollected}`, GAME_WIDTH / 2, 270);
        ctx.fillText(`Distance: ${Math.floor(this.distanceTraveled / 10)}m`, GAME_WIDTH / 2, 310);
        ctx.fillText(`Tricks: ${this.tricksPerformed}`, GAME_WIDTH / 2, 350);

        if (this.score == this.highScore && this.highScore > 0) {
            ctx.fillStyle = '#FFFF00';
            ctx.fillText('NEW HIGH SCORE!', GAME_WIDTH / 2, 410);
        }

        ctx.fillStyle = '#FFFFFF';
        ctx.font = '20px Arial';
        ctx.fillText('Press SPACE to Restart', GAME_WIDTH / 2, 480);
    }

    gameLoop() {
        const now = Date.now();
        const deltaTime = now - this.lastFrameTime;
        this.lastFrameTime = now;

        if (this.currentState === GameState.PLAYING) {
            this.updateGameLogic();
        }

        this.render();
        requestAnimationFrame(() => this.gameLoop());
    }
}

// Game objects
class Obstacle {
    constructor(x, y, type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.width = 40;
        this.height = 40;
    }

    draw(ctx) {
        switch (this.type) {
            case ObstacleType.ROCK:
                ctx.fillStyle = '#646464';
                ctx.beginPath();
                ctx.arc(this.x + 20, this.y + 20, 20, 0, Math.PI * 2);
                ctx.fill();
                ctx.fillStyle = '#464646';
                ctx.beginPath();
                ctx.arc(this.x + 25, this.y + 25, 8, 0, Math.PI * 2);
                ctx.fill();
                break;
            case ObstacleType.TREE_STUMP:
                ctx.fillStyle = '#8B5A2B';
                ctx.fillRect(this.x, this.y, 40, 40);
                ctx.fillStyle = '#654321';
                ctx.beginPath();
                ctx.arc(this.x + 20, this.y + 10, 15, 0, Math.PI);
                ctx.fill();
                break;
            case ObstacleType.ICE_BLOCK:
                ctx.fillStyle = 'rgba(150, 200, 255, 0.7)';
                ctx.fillRect(this.x, this.y, 40, 40);
                ctx.strokeStyle = 'rgba(200, 230, 255, 0.9)';
                ctx.lineWidth = 2;
                ctx.strokeRect(this.x, this.y, 40, 40);
                break;
        }
    }
}

class Ramp {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.width = 80;
        this.height = 30;
    }

    draw(ctx) {
        ctx.fillStyle = '#C8C8FF';
        ctx.beginPath();
        ctx.moveTo(this.x, this.y + this.height);
        ctx.lineTo(this.x + this.width, this.y + this.height);
        ctx.lineTo(this.x + this.width, this.y);
        ctx.fill();

        ctx.strokeStyle = '#9696C8';
        ctx.lineWidth = 2;
        ctx.stroke();
    }
}

class Coin {
    constructor(x, y) {
        this.x = x;
        this.y = y;
        this.rotation = 0;
    }

    draw(ctx) {
        ctx.save();
        ctx.translate(this.x + 12, this.y + 12);
        ctx.rotate((this.rotation * Math.PI) / 180);
        ctx.translate(-(this.x + 12), -(this.y + 12));

        ctx.fillStyle = '#FFD700';
        ctx.beginPath();
        ctx.arc(this.x + 12, this.y + 12, 12, 0, Math.PI * 2);
        ctx.fill();

        ctx.strokeStyle = '#DAA520';
        ctx.lineWidth = 2;
        ctx.stroke();

        ctx.beginPath();
        ctx.arc(this.x + 12, this.y + 12, 9, 0, Math.PI * 2);
        ctx.stroke();

        ctx.restore();
    }
}

class Particle {
    constructor(x, y, vx, vy, color) {
        this.x = x;
        this.y = y;
        this.vx = vx;
        this.vy = vy;
        this.life = 60;
        this.color = color;
    }

    update() {
        this.x += this.vx;
        this.y += this.vy;
        this.vy += 0.2;
        this.life--;
    }

    draw(ctx) {
        const alpha = this.life / 60;
        ctx.fillStyle = this.color.replace('rgb', 'rgba').replace(')', `, ${alpha})`);
        ctx.beginPath();
        ctx.arc(this.x, this.y, 3, 0, Math.PI * 2);
        ctx.fill();
    }
}

class Tree {
    constructor(x, y) {
        this.x = x;
        this.y = y;
    }

    draw(ctx) {
        // Trunk
        ctx.fillStyle = '#654321';
        ctx.fillRect(this.x + 15, this.y + 40, 10, 40);

        // Foliage
        ctx.fillStyle = '#228B22';
        ctx.beginPath();
        ctx.arc(this.x + 20, this.y + 20, 20, 0, Math.PI * 2);
        ctx.fill();

        ctx.fillStyle = '#006400';
        ctx.beginPath();
        ctx.arc(this.x + 20, this.y + 35, 18, 0, Math.PI * 2);
        ctx.fill();
    }
}

// Start the game
const game = new Game();
