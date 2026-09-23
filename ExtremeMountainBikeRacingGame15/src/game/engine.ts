import type { Bike, Particle, GameMode, RaceResult } from './types';
import { Terrain } from './terrain';
import { Renderer } from './renderer';
import { createBike, updateBike, updateParticles, type ControlInput } from './physics';
import { getAIInput } from './ai';

const TRACK_LENGTH = 6000;

export class GameEngine {
  canvas: HTMLCanvasElement;
  ctx: CanvasRenderingContext2D;
  renderer: Renderer;
  terrain: Terrain;
  bikes: Bike[] = [];
  particles: Particle[] = [];
  mode: GameMode;
  rafId: number = 0;
  lastTime: number = 0;
  startTime: number = 0;
  countdown: number = 3;
  racing: boolean = false;
  finished: boolean = false;
  elapsed: number = 0;
  keys: Set<string> = new Set();
  onResults: (results: RaceResult[]) => void;
  onUpdate: (state: GameUpdateState) => void;
  splitScreen: boolean = false;

  constructor(
    canvas: HTMLCanvasElement,
    mode: GameMode,
    onResults: (results: RaceResult[]) => void,
    onUpdate: (state: GameUpdateState) => void
  ) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d')!;
    this.mode = mode;
    this.onResults = onResults;
    this.onUpdate = onUpdate;
    this.terrain = new Terrain(TRACK_LENGTH);
    this.renderer = new Renderer(this.ctx, canvas.width, canvas.height);
    this.splitScreen = mode === 'two-players';
    this.setupBikes();
    this.setupInput();
  }

  setupBikes() {
    if (this.mode === 'vs-cpu') {
      this.bikes = [
        createBike(1, 'Player 1', '#e63946', false, 80, this.terrain),
        createBike(2, 'CPU', '#2a9d8f', true, 80, this.terrain),
      ];
    } else {
      this.bikes = [
        createBike(1, 'Player 1', '#e63946', false, 80, this.terrain),
        createBike(2, 'Player 2', '#3a86ff', false, 80, this.terrain),
      ];
    }
  }

  setupInput() {
    window.addEventListener('keydown', this.handleKeyDown);
    window.addEventListener('keyup', this.handleKeyUp);
  }

  handleKeyDown = (e: KeyboardEvent) => {
    this.keys.add(e.code);
    if (['ArrowUp', 'ArrowDown', 'ArrowLeft', 'ArrowRight', 'Space'].includes(e.code)) {
      e.preventDefault();
    }
  };

  handleKeyUp = (e: KeyboardEvent) => {
    this.keys.delete(e.code);
  };

  getPlayer1Input(): ControlInput {
    return {
      accelerate: this.keys.has('KeyD') || this.keys.has('ArrowRight'),
      brake: this.keys.has('KeyA') || this.keys.has('ArrowLeft'),
      rotateLeft: this.keys.has('KeyA') || this.keys.has('ArrowLeft'),
      rotateRight: this.keys.has('KeyD') || this.keys.has('ArrowRight'),
      boost: this.keys.has('Space') || this.keys.has('ShiftLeft'),
    };
  }

  getPlayer2Input(): ControlInput {
    return {
      accelerate: this.keys.has('KeyL'),
      brake: this.keys.has('KeyJ'),
      rotateLeft: this.keys.has('KeyI'),
      rotateRight: this.keys.has('KeyK'),
      boost: this.keys.has('KeyO') || this.keys.has('ShiftRight'),
    };
  }

  start() {
    this.lastTime = performance.now();
    this.startTime = performance.now();
    this.countdown = 3;
    this.racing = false;
    this.loop();
  }

  stop() {
    cancelAnimationFrame(this.rafId);
    window.removeEventListener('keydown', this.handleKeyDown);
    window.removeEventListener('keyup', this.handleKeyUp);
  }

  loop = () => {
    const now = performance.now();
    const rawDt = (now - this.lastTime) / 16.67; // normalize to ~60fps
    const dt = Math.min(rawDt, 2); // cap to prevent huge jumps
    this.lastTime = now;

    if (!this.finished) {
      this.update(dt);
    }
    this.render();
    this.rafId = requestAnimationFrame(this.loop);
  };

  update(dt: number) {
    // Countdown
    if (!this.racing) {
      this.countdown -= dt / 60;
      if (this.countdown <= 0) {
        this.racing = true;
        this.startTime = performance.now();
      }
      this.onUpdate(this.getUpdateState());
      return;
    }

    this.elapsed = (performance.now() - this.startTime) / 1000;

    // Update bikes
    for (const bike of this.bikes) {
      let input: ControlInput;
      if (bike.isAI) {
        input = getAIInput(bike, this.terrain, TRACK_LENGTH);
      } else if (bike.id === 1) {
        input = this.getPlayer1Input();
      } else {
        input = this.getPlayer2Input();
      }
      updateBike(bike, input, this.terrain, dt, this.particles, TRACK_LENGTH);
    }

    // Update particles
    updateParticles(this.particles, dt);

    // Update clouds
    this.renderer.updateClouds(dt);

    // Check if all finished
    const allFinished = this.bikes.every((b) => b.finished);
    if (allFinished && !this.finished) {
      this.finished = true;
      this.generateResults();
    }

    // Also finish if leader finished and others are close
    if (!this.finished) {
      const leaderFinished = this.bikes.some((b) => b.finished);
      if (leaderFinished) {
        const others = this.bikes.filter((b) => !b.finished);
        const allClose = others.every((b) => b.progress > 0.9);
        if (allClose || this.elapsed > 120) {
          // Force finish stragglers
          for (const b of this.bikes) {
            if (!b.finished) {
              b.finished = true;
              b.finishTime = performance.now();
            }
          }
          this.finished = true;
          this.generateResults();
        }
      }
    }

    this.onUpdate(this.getUpdateState());
  }

  getUpdateState(): GameUpdateState {
    return {
      countdown: Math.ceil(this.countdown),
      racing: this.racing,
      elapsed: this.elapsed,
      bikes: this.bikes.map((b) => ({
        id: b.id,
        name: b.name,
        color: b.color,
        progress: b.progress,
        speed: Math.abs(b.vx),
        flips: b.flips,
        trickScore: b.trickScore,
        boost: b.boost,
        crashed: b.crashed,
        finished: b.finished,
        isAI: b.isAI,
      })),
    };
  }

  generateResults() {
    const sorted = [...this.bikes].sort((a, b) => {
      if (a.finished && b.finished) return a.finishTime - b.finishTime;
      if (a.finished) return -1;
      if (b.finished) return 1;
      return b.progress - a.progress;
    });

    const results: RaceResult[] = sorted.map((bike, i) => ({
      position: i + 1,
      name: bike.name,
      color: bike.color,
      time: bike.finishTime > 0 ? (bike.finishTime - this.startTime) / 1000 : this.elapsed,
      tricks: bike.trickScore,
      isAI: bike.isAI,
    }));

    // Confetti
    for (let i = 0; i < 100; i++) {
      this.particles.push({
        x: this.bikes[0].x + (Math.random() - 0.5) * 300,
        y: -50 + Math.random() * 100,
        vx: (Math.random() - 0.5) * 4,
        vy: Math.random() * 2 + 1,
        life: 120 + Math.random() * 60,
        maxLife: 180,
        size: 4 + Math.random() * 4,
        color: ['#e63946', '#3a86ff', '#2a9d8f', '#ffbe0b', '#ff006e', '#8338ec'][Math.floor(Math.random() * 6)],
        type: 'confetti',
        rotation: Math.random() * Math.PI * 2,
        rotationSpeed: (Math.random() - 0.5) * 0.3,
      });
    }

    setTimeout(() => this.onResults(results), 2500);
  }

  render() {
    const { ctx, canvas } = this;

    if (this.splitScreen) {
      // Split screen horizontal
      const halfH = canvas.height / 2;
      // Top half - Player 1
      ctx.save();
      ctx.beginPath();
      ctx.rect(0, 0, canvas.width, halfH);
      ctx.clip();
      this.renderViewport(this.bikes[0], 0, 0, canvas.width, halfH);
      ctx.restore();

      // Bottom half - Player 2
      ctx.save();
      ctx.beginPath();
      ctx.rect(0, halfH, canvas.width, halfH);
      ctx.clip();
      this.renderViewport(this.bikes[1], 0, halfH, canvas.width, halfH);
      ctx.restore();

      // Divider line
      ctx.strokeStyle = 'rgba(0,0,0,0.5)';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(0, halfH);
      ctx.lineTo(canvas.width, halfH);
      ctx.stroke();
    } else {
      // Single screen following leader (player)
      this.renderViewport(this.bikes[0], 0, 0, canvas.width, canvas.height);

      // Show CPU position indicator if off-screen
      const cpu = this.bikes[1];
      const camX = this.bikes[0].x - canvas.width * 0.35;
      const cpuScreenX = cpu.x - camX;
      if (cpuScreenX < 0 || cpuScreenX > canvas.width) {
        this.drawOffscreenIndicator(cpu, canvas.width, canvas.height);
      }
    }

    // Countdown overlay
    if (!this.racing && this.countdown > 0) {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.4)';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      const count = Math.ceil(this.countdown);
      const scale = 1 + (this.countdown - Math.floor(this.countdown)) * 0.5;
      ctx.save();
      ctx.translate(canvas.width / 2, canvas.height / 2);
      ctx.scale(scale, scale);
      ctx.font = 'bold 120px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillStyle = count === 1 ? '#ff4444' : count === 2 ? '#ffaa00' : '#44ff44';
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 6;
      ctx.strokeText(String(count), 0, 0);
      ctx.fillText(String(count), 0, 0);
      ctx.restore();

      ctx.font = 'bold 24px sans-serif';
      ctx.fillStyle = '#fff';
      ctx.textAlign = 'center';
      ctx.fillText('GET READY!', canvas.width / 2, canvas.height / 2 - 100);
    }
  }

  drawOffscreenIndicator(bike: Bike, width: number, height: number) {
    const { ctx } = this;
    const camX = this.bikes[0].x - width * 0.35;
    const dir = bike.x > this.bikes[0].x ? 1 : -1;
    const arrowX = dir > 0 ? width - 40 : 40;
    const arrowY = height * 0.3;

    // Arrow
    ctx.fillStyle = bike.color;
    ctx.beginPath();
    if (dir > 0) {
      ctx.moveTo(arrowX + 15, arrowY);
      ctx.lineTo(arrowX - 5, arrowY - 12);
      ctx.lineTo(arrowX - 5, arrowY + 12);
    } else {
      ctx.moveTo(arrowX - 15, arrowY);
      ctx.lineTo(arrowX + 5, arrowY - 12);
      ctx.lineTo(arrowX + 5, arrowY + 12);
    }
    ctx.closePath();
    ctx.fill();

    // Label
    ctx.fillStyle = '#fff';
    ctx.font = 'bold 12px sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText(bike.name, arrowX, arrowY + 25);
    const dist = Math.abs(bike.x - this.bikes[0].x);
    ctx.fillText(`${Math.floor(dist / 10)}m`, arrowX, arrowY + 40);
  }

  renderViewport(bike: Bike, ox: number, oy: number, vw: number, vh: number) {
    const { ctx, terrain, renderer } = this;

    // Camera follows bike
    const camX = bike.x - vw * 0.35;
    const camY = bike.y - vh * 0.5;

    // Apply viewport offset
    ctx.save();
    ctx.translate(ox, oy);

    // Sky
    renderer.drawSky();

    // Sun
    renderer.drawSun(camX);

    // Clouds
    renderer.drawClouds(camX);

    // Mountains
    renderer.drawMountains(camX);

    // Trees
    const baseGroundY = terrain.getHeightAt(bike.x);
    renderer.drawTrees(camX, baseGroundY - camY);

    // Terrain
    renderer.drawTerrain(terrain, camX, camY, vw, vh);

    // Start line
    renderer.drawStartLine(terrain, camX, camY);

    // Finish line
    renderer.drawFinishLine(terrain, camX, camY, TRACK_LENGTH);

    // Other bikes that are visible in this viewport
    for (const otherBike of this.bikes) {
      if (otherBike.id === bike.id) continue;
      const screenX = otherBike.x - camX;
      if (screenX > -100 && screenX < vw + 100) {
        renderer.drawBike(otherBike, camX, camY);
      }
    }

    // This bike (player's bike)
    renderer.drawBike(bike, camX, camY);

    // Particles
    renderer.drawParticles(this.particles, camX, camY);

    // Mini-map
    this.drawMiniMap(bike, vw);

    // HUD for this viewport
    this.drawHUD(bike, vw, vh);

    ctx.restore();
  }

  drawMiniMap(bike: Bike, vw: number) {
    const { ctx, terrain } = this;
    const mapW = 200;
    const mapH = 30;
    const mapX = vw - mapW - 15;
    const mapY = 15;

    // Background
    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    ctx.fillRect(mapX - 3, mapY - 3, mapW + 6, mapH + 6);

    // Track
    ctx.fillStyle = 'rgba(255, 255, 255, 0.2)';
    ctx.fillRect(mapX, mapY, mapW, mapH);

    // Progress dots for each bike
    for (const b of this.bikes) {
      const dotX = mapX + b.progress * mapW;
      const dotY = mapY + mapH / 2;
      ctx.fillStyle = b.color;
      ctx.beginPath();
      ctx.arc(dotX, dotY, 5, 0, Math.PI * 2);
      ctx.fill();
      // Label
      ctx.fillStyle = '#fff';
      ctx.font = '8px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(b.name[0], dotX, dotY + 3);
    }

    // Finish marker
    ctx.fillStyle = '#ffd700';
    ctx.fillRect(mapX + mapW - 3, mapY, 3, mapH);
  }

  drawHUD(bike: Bike, vw: number, vh: number) {
    const { ctx } = this;

    // Speed indicator
    const speed = Math.abs(bike.vx);
    const speedKmh = Math.floor(speed * 12);
    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    ctx.fillRect(15, vh - 70, 140, 55);

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 28px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(`${speedKmh}`, 25, vh - 35);
    ctx.font = '12px sans-serif';
    ctx.fillStyle = '#aaa';
    ctx.fillText('km/h', 90, vh - 35);

    // Speed bar
    const barW = 110;
    const fillW = Math.min(1, speed / 20) * barW;
    ctx.fillStyle = 'rgba(255,255,255,0.2)';
    ctx.fillRect(25, vh - 25, barW, 6);
    const speedGrad = ctx.createLinearGradient(25, 0, 25 + barW, 0);
    speedGrad.addColorStop(0, '#44ff44');
    speedGrad.addColorStop(0.5, '#ffaa00');
    speedGrad.addColorStop(1, '#ff4444');
    ctx.fillStyle = speedGrad;
    ctx.fillRect(25, vh - 25, fillW, 6);

    // Boost meter
    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    ctx.fillRect(15, vh - 90, 140, 16);
    ctx.fillStyle = 'rgba(255,255,255,0.2)';
    ctx.fillRect(22, vh - 86, 126, 8);
    const boostGrad = ctx.createLinearGradient(22, 0, 148, 0);
    boostGrad.addColorStop(0, '#ff6b00');
    boostGrad.addColorStop(1, '#ffdd00');
    ctx.fillStyle = boostGrad;
    ctx.fillRect(22, vh - 86, bike.boost * 126, 8);
    ctx.fillStyle = '#fff';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('BOOST', 25, vh - 78);

    // Trick score
    if (bike.trickScore > 0) {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
      ctx.fillRect(vw - 160, vh - 40, 145, 25);
      ctx.fillStyle = '#ffbe0b';
      ctx.font = 'bold 14px sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText(`Tricks: ${bike.trickScore}`, vw - 22, vh - 22);
    }

    // Crash indicator
    if (bike.crashed) {
      ctx.fillStyle = 'rgba(255, 0, 0, 0.7)';
      ctx.font = 'bold 20px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('CRASHED!', vw / 2, 40);
    }

    // Finished indicator
    if (bike.finished) {
      ctx.fillStyle = 'rgba(0, 255, 100, 0.8)';
      ctx.font = 'bold 24px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('FINISHED!', vw / 2, 40);
    }
  }
}

export interface GameUpdateState {
  countdown: number;
  racing: boolean;
  elapsed: number;
  bikes: {
    id: number;
    name: string;
    color: string;
    progress: number;
    speed: number;
    flips: number;
    trickScore: number;
    boost: number;
    crashed: boolean;
    finished: boolean;
    isAI: boolean;
  }[];
}
