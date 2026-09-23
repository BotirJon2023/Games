import type { Bike3DState, Particle3D, GameMode3D, RaceResult3D, GameUpdateState3D } from './types';
import { Road3D } from './road';
import { Renderer3D } from './renderer3d';
import { createBike3D, updateBike3D, getAIInput3D, type ControlInput3D } from './physics3d';

const CAMERA_HEIGHT = 1000;

export class GameEngine3D {
  canvas: HTMLCanvasElement;
  ctx: CanvasRenderingContext2D;
  renderer: Renderer3D;
  road: Road3D;
  bikes: Bike3DState[] = [];
  particles: Particle3D[] = [];
  mode: GameMode3D;
  rafId: number = 0;
  lastTime: number = 0;
  startTime: number = 0;
  countdown: number = 3;
  racing: boolean = false;
  finished: boolean = false;
  elapsed: number = 0;
  keys: Set<string> = new Set();
  onResults: (results: RaceResult3D[]) => void;
  onUpdate: (state: GameUpdateState3D) => void;
  splitScreen: boolean = false;
  topSpeeds: Map<number, number> = new Map();

  constructor(
    canvas: HTMLCanvasElement,
    mode: GameMode3D,
    onResults: (results: RaceResult3D[]) => void,
    onUpdate: (state: GameUpdateState3D) => void
  ) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d')!;
    this.mode = mode;
    this.onResults = onResults;
    this.onUpdate = onUpdate;
    this.road = new Road3D();
    this.renderer = new Renderer3D(this.ctx, canvas.width, canvas.height);
    this.splitScreen = mode === 'two-players';
    this.setupBikes();
    this.setupInput();
  }

  setupBikes() {
    if (this.mode === 'vs-cpu') {
      this.bikes = [
        createBike3D(1, 'Player 1', '#e63946', false, 0),
        createBike3D(2, 'CPU', '#2a9d8f', true, 0),
      ];
    } else {
      this.bikes = [
        createBike3D(1, 'Player 1', '#e63946', false, 0),
        createBike3D(2, 'Player 2', '#3a86ff', false, 0),
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

  getPlayer1Input(): ControlInput3D {
    return {
      left: this.keys.has('KeyA') || this.keys.has('ArrowLeft'),
      right: this.keys.has('KeyD') || this.keys.has('ArrowRight'),
      accelerate: this.keys.has('KeyW') || this.keys.has('ArrowUp'),
      brake: this.keys.has('KeyS') || this.keys.has('ArrowDown'),
      boost: this.keys.has('Space') || this.keys.has('ShiftLeft'),
    };
  }

  getPlayer2Input(): ControlInput3D {
    return {
      left: this.keys.has('KeyJ'),
      right: this.keys.has('KeyL'),
      accelerate: this.keys.has('KeyI'),
      brake: this.keys.has('KeyK'),
      boost: this.keys.has('KeyO') || this.keys.has('ShiftRight'),
    };
  }

  start() {
    this.lastTime = performance.now();
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
    const rawDt = (now - this.lastTime) / 16.67;
    const dt = Math.min(rawDt, 2);
    this.lastTime = now;

    if (!this.finished) {
      this.update(dt);
    }
    this.render();
    this.rafId = requestAnimationFrame(this.loop);
  };

  update(dt: number) {
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

    for (const bike of this.bikes) {
      let input: ControlInput3D;
      if (bike.isAI) {
        input = getAIInput3D(bike, this.road, this.road.getTrackLength(), this.bikes);
      } else if (bike.id === 1) {
        input = this.getPlayer1Input();
      } else {
        input = this.getPlayer2Input();
      }
      updateBike3D(bike, input, this.road, dt, this.road.getTrackLength());

      // Track top speed
      const prevTop = this.topSpeeds.get(bike.id) ?? 0;
      this.topSpeeds.set(bike.id, Math.max(prevTop, bike.speed));

      // Dust particles when accelerating fast
      if (input.accelerate && bike.speed > bike.maxSpeed * 0.5 && Math.random() < 0.3) {
        this.particles.push({
          x: this.canvas.width / 2 + (Math.random() - 0.5) * 60,
          y: this.canvas.height - 90 + Math.random() * 20,
          z: 0,
          vx: (Math.random() - 0.5) * 3,
          vy: -Math.random() * 2,
          life: 20 + Math.random() * 15,
          maxLife: 35,
          size: 4 + Math.random() * 4,
          color: '#a08060',
          type: 'dust',
        });
      }

      // Boost spark particles
      if (bike.boostTimer > 0 && Math.random() < 0.5) {
        this.particles.push({
          x: this.canvas.width / 2 - 30 + (Math.random() - 0.5) * 20,
          y: this.canvas.height - 100 + Math.random() * 15,
          z: 0,
          vx: -3 - Math.random() * 2,
          vy: (Math.random() - 0.5) * 2,
          life: 15 + Math.random() * 10,
          maxLife: 25,
          size: 3 + Math.random() * 3,
          color: '#ff8800',
          type: 'spark',
        });
      }
    }

    // Update particles
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vy += 0.1 * dt;
      p.life -= dt;
      if (p.life <= 0) this.particles.splice(i, 1);
    }

    // Check if all finished
    const allFinished = this.bikes.every((b) => b.finished);
    if (allFinished && !this.finished) {
      this.finished = true;
      this.generateResults();
    }

    // Force finish stragglers
    if (!this.finished) {
      const leaderFinished = this.bikes.some((b) => b.finished);
      if (leaderFinished) {
        const others = this.bikes.filter((b) => !b.finished);
        const allClose = others.every((b) => b.progress > 0.95);
        if (allClose || this.elapsed > 120) {
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

  getUpdateState(): GameUpdateState3D {
    return {
      countdown: Math.ceil(this.countdown),
      racing: this.racing,
      elapsed: this.elapsed,
      bikes: this.bikes.map((b) => ({
        id: b.id,
        name: b.name,
        color: b.color,
        progress: b.progress,
        speed: b.speed,
        boost: b.boost,
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

    const results: RaceResult3D[] = sorted.map((bike, i) => ({
      position: i + 1,
      name: bike.name,
      color: bike.color,
      time: bike.finishTime > 0 ? (bike.finishTime - this.startTime) / 1000 : this.elapsed,
      topSpeed: Math.floor((this.topSpeeds.get(bike.id) ?? 0) * 0.05),
      isAI: bike.isAI,
    }));

    // Confetti
    for (let i = 0; i < 80; i++) {
      this.particles.push({
        x: Math.random() * this.canvas.width,
        y: -20 + Math.random() * 50,
        z: 0,
        vx: (Math.random() - 0.5) * 4,
        vy: Math.random() * 2 + 1,
        life: 120 + Math.random() * 60,
        maxLife: 180,
        size: 4 + Math.random() * 4,
        color: ['#e63946', '#3a86ff', '#2a9d8f', '#ffbe0b', '#ff006e', '#8338ec'][Math.floor(Math.random() * 6)],
        type: 'confetti',
      });
    }

    setTimeout(() => this.onResults(results), 2500);
  }

  render() {
    const { ctx, canvas } = this;

    if (this.splitScreen) {
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

      // Divider
      ctx.strokeStyle = 'rgba(0,0,0,0.6)';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.moveTo(0, halfH);
      ctx.lineTo(canvas.width, halfH);
      ctx.stroke();
    } else {
      this.renderViewport(this.bikes[0], 0, 0, canvas.width, canvas.height);
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

  renderViewport(bike: Bike3DState, ox: number, oy: number, vw: number, vh: number) {
    const { ctx, road, renderer } = this;

    ctx.save();
    ctx.translate(ox, oy);

    // Camera setup
    const baseSegment = road.getSegmentAtZ(bike.position);
    const baseSegmentIndex = Math.floor(bike.position / road.segmentLength);
    const cameraZ = bike.position;
    const cameraY = CAMERA_HEIGHT + baseSegment.p1.world.y;
    const cameraX = bike.playerX * road.roadWidth;

    const drawDistance = 200;

    // Draw sky
    renderer.drawSky(bike);
    renderer.drawClouds(bike);

    // Draw road
    renderer.drawRoad(road, bike, baseSegmentIndex, drawDistance, cameraX, cameraY, cameraZ);

    // Draw sprites (scenery)
    renderer.drawSprites(road, bike, baseSegmentIndex, drawDistance, cameraX, cameraY, cameraZ);

    // Draw bikes (other racers + player)
    renderer.drawBike3D(bike, true, this.bikes, road, cameraZ, cameraY);

    // Speed lines at high speed
    renderer.drawSpeedLines(bike);

    // Particles
    renderer.drawParticles3D(this.particles, bike);

    // HUD
    this.drawHUD(bike, vw, vh);
    this.drawMiniMap3D(bike, vw);

    ctx.restore();
  }

  drawMiniMap3D(bike: Bike3DState, vw: number) {
    const { ctx, road } = this;
    const mapW = 200;
    const mapH = 12;
    const mapX = vw - mapW - 15;
    const mapY = 15;

    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    ctx.fillRect(mapX - 3, mapY - 3, mapW + 6, mapH + 6);

    // Track
    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.fillRect(mapX, mapY, mapW, mapH);

    for (const b of this.bikes) {
      const dotX = mapX + b.progress * mapW;
      const dotY = mapY + mapH / 2;
      ctx.fillStyle = b.color;
      ctx.beginPath();
      ctx.arc(dotX, dotY, 4, 0, Math.PI * 2);
      ctx.fill();
      ctx.fillStyle = '#fff';
      ctx.font = '7px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(b.name[0], dotX, dotY + 2.5);
    }

    // Finish marker
    ctx.fillStyle = '#ffd700';
    ctx.fillRect(mapX + mapW - 2, mapY, 2, mapH);
  }

  drawHUD(bike: Bike3DState, vw: number, vh: number) {
    const { ctx } = this;

    // Speedometer (bottom left)
    const speedKmh = Math.floor(bike.speed * 0.05);
    const speedPct = bike.speed / bike.maxSpeed;

    ctx.fillStyle = 'rgba(0, 0, 0, 0.55)';
    ctx.fillRect(15, vh - 80, 150, 65);

    ctx.fillStyle = '#fff';
    ctx.font = 'bold 32px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(`${speedKmh}`, 25, vh - 38);
    ctx.font = '12px sans-serif';
    ctx.fillStyle = '#aaa';
    ctx.fillText('km/h', 100, vh - 38);

    // Speed bar
    const barW = 120;
    const fillW = Math.min(1, speedPct) * barW;
    ctx.fillStyle = 'rgba(255,255,255,0.15)';
    ctx.fillRect(25, vh - 25, barW, 6);
    const speedGrad = ctx.createLinearGradient(25, 0, 25 + barW, 0);
    speedGrad.addColorStop(0, '#44ff44');
    speedGrad.addColorStop(0.5, '#ffaa00');
    speedGrad.addColorStop(1, '#ff4444');
    ctx.fillStyle = speedGrad;
    ctx.fillRect(25, vh - 25, fillW, 6);

    // Boost meter
    ctx.fillStyle = 'rgba(0, 0, 0, 0.55)';
    ctx.fillRect(15, vh - 100, 150, 16);
    ctx.fillStyle = 'rgba(255,255,255,0.15)';
    ctx.fillRect(22, vh - 96, 136, 8);
    const boostGrad = ctx.createLinearGradient(22, 0, 158, 0);
    boostGrad.addColorStop(0, '#ff6b00');
    boostGrad.addColorStop(1, '#ffdd00');
    ctx.fillStyle = boostGrad;
    ctx.fillRect(22, vh - 96, bike.boost * 136, 8);
    ctx.fillStyle = '#fff';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('BOOST', 25, vh - 88);

    // Finished indicator
    if (bike.finished) {
      ctx.fillStyle = 'rgba(0, 255, 100, 0.8)';
      ctx.font = 'bold 28px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('FINISHED!', vw / 2, 50);
    }

    // Off-road warning
    if (Math.abs(bike.playerX) > 1) {
      ctx.fillStyle = 'rgba(255, 100, 0, 0.7)';
      ctx.font = 'bold 18px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('OFF ROAD!', vw / 2, 50);
    }
  }
}
