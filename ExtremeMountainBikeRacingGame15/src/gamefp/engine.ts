import type { FPBike, FPParticle, GameModeFP, FPRaceResult, FPGUdateState } from './types';
import { RoadFP } from './road';
import { RendererFP } from './renderer';
import { createFPBike, updateFPBike, getFPAIInput, type FPControlInput } from './physics';

const CAMERA_HEIGHT = 900;
const DRAW_DISTANCE = 250;

export class GameEngineFP {
  canvas: HTMLCanvasElement;
  ctx: CanvasRenderingContext2D;
  renderer: RendererFP;
  road: RoadFP;
  bikes: FPBike[] = [];
  particles: FPParticle[] = [];
  mode: GameModeFP;
  rafId: number = 0;
  lastTime: number = 0;
  startTime: number = 0;
  countdown: number = 3;
  racing: boolean = false;
  finished: boolean = false;
  elapsed: number = 0;
  keys: Set<string> = new Set();
  onResults: (results: FPRaceResult[]) => void;
  onUpdate: (state: FPGUdateState) => void;
  splitScreen: boolean = false;

  constructor(
    canvas: HTMLCanvasElement,
    mode: GameModeFP,
    onResults: (results: FPRaceResult[]) => void,
    onUpdate: (state: FPGUdateState) => void
  ) {
    this.canvas = canvas;
    this.ctx = canvas.getContext('2d')!;
    this.mode = mode;
    this.onResults = onResults;
    this.onUpdate = onUpdate;
    this.road = new RoadFP();
    this.renderer = new RendererFP(this.ctx, canvas.width, canvas.height);
    this.splitScreen = mode === 'two-players';
    this.setupBikes();
    this.setupInput();
  }

  setupBikes() {
    if (this.mode === 'vs-cpu') {
      this.bikes = [
        createFPBike(1, 'Player 1', '#e63946', false, 0),
        createFPBike(2, 'CPU', '#2a9d8f', true, 0),
      ];
    } else {
      this.bikes = [
        createFPBike(1, 'Player 1', '#e63946', false, 0),
        createFPBike(2, 'Player 2', '#3a86ff', false, 0),
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

  getP1Input(): FPControlInput {
    return {
      left: this.keys.has('KeyA') || this.keys.has('ArrowLeft'),
      right: this.keys.has('KeyD') || this.keys.has('ArrowRight'),
      accelerate: this.keys.has('KeyW') || this.keys.has('ArrowUp'),
      brake: this.keys.has('KeyS') || this.keys.has('ArrowDown'),
      boost: this.keys.has('Space') || this.keys.has('ShiftLeft'),
    };
  }

  getP2Input(): FPControlInput {
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
    this.renderer.time += dt;

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
      let input: FPControlInput;
      if (bike.isAI) {
        input = getFPAIInput(bike, this.road, this.road.getTrackLength(), this.bikes);
      } else if (bike.id === 1) {
        input = this.getP1Input();
      } else {
        input = this.getP2Input();
      }
      updateFPBike(bike, input, this.road, dt, this.road.getTrackLength());

      // Dust particles when off-road or accelerating hard
      if (Math.abs(bike.playerX) > 1 && bike.speed > 200 && Math.random() < 0.4) {
        const baseX = this.canvas.width / 2;
        const baseY = this.canvas.height - 30;
        this.particles.push({
          x: baseX + (Math.random() - 0.5) * 80,
          y: baseY + Math.random() * 20,
          vx: (Math.random() - 0.5) * 4,
          vy: -Math.random() * 2 - 1,
          life: 20 + Math.random() * 15,
          maxLife: 35,
          size: 5 + Math.random() * 5,
          color: '#a08060',
          type: 'dust',
        });
      }

      // Boost spark particles
      if (bike.boostTimer > 0 && Math.random() < 0.4) {
        this.particles.push({
          x: this.canvas.width / 2 + (Math.random() - 0.5) * 50,
          y: this.canvas.height - 50 + Math.random() * 20,
          vx: -4 - Math.random() * 3,
          vy: (Math.random() - 0.5) * 3,
          life: 12 + Math.random() * 8,
          maxLife: 20,
          size: 3 + Math.random() * 3,
          color: '#ff8800',
          type: 'spark',
        });
      }

      // Leaves blowing past at speed
      if (bike.speed > bike.maxSpeed * 0.5 && Math.random() < 0.15) {
        const side = Math.random() < 0.5 ? -1 : 1;
        this.particles.push({
          x: this.canvas.width / 2 + side * (this.canvas.width * 0.3 + Math.random() * 100),
          y: this.canvas.height * (0.3 + Math.random() * 0.4),
          vx: -side * (8 + Math.random() * 5),
          vy: (Math.random() - 0.5) * 3,
          life: 25 + Math.random() * 15,
          maxLife: 40,
          size: 4 + Math.random() * 3,
          color: ['#c8a040', '#8a6030', '#4a8a30'][Math.floor(Math.random() * 3)],
          type: 'leaf',
        });
      }
    }

    // Update particles
    for (let i = this.particles.length - 1; i >= 0; i--) {
      const p = this.particles[i];
      p.x += p.vx * dt;
      p.y += p.vy * dt;
      p.vy += 0.08 * dt;
      p.life -= dt;
      if (p.life <= 0) this.particles.splice(i, 1);
    }

    // Update clouds
    this.renderer.updateClouds(dt);

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

  getUpdateState(): FPGUdateState {
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

    const results: FPRaceResult[] = sorted.map((bike, i) => ({
      position: i + 1,
      name: bike.name,
      color: bike.color,
      time: bike.finishTime > 0 ? (bike.finishTime - this.startTime) / 1000 : this.elapsed,
      topSpeed: Math.floor(bike.topSpeed * 0.05),
      isAI: bike.isAI,
    }));

    // Confetti
    for (let i = 0; i < 80; i++) {
      this.particles.push({
        x: Math.random() * this.canvas.width,
        y: -20 + Math.random() * 50,
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
      ctx.save();
      ctx.beginPath();
      ctx.rect(0, 0, canvas.width, halfH);
      ctx.clip();
      this.renderViewport(this.bikes[0], canvas.width, halfH);
      ctx.restore();

      ctx.save();
      ctx.beginPath();
      ctx.rect(0, halfH, canvas.width, halfH);
      ctx.clip();
      this.renderViewport(this.bikes[1], canvas.width, halfH);
      ctx.restore();

      // Divider
      ctx.strokeStyle = 'rgba(0,0,0,0.7)';
      ctx.lineWidth = 4;
      ctx.beginPath();
      ctx.moveTo(0, halfH);
      ctx.lineTo(canvas.width, halfH);
      ctx.stroke();
    } else {
      this.renderViewport(this.bikes[0], canvas.width, canvas.height);
    }

    // Countdown
    if (!this.racing && this.countdown > 0) {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.35)';
      ctx.fillRect(0, 0, canvas.width, canvas.height);

      const count = Math.ceil(this.countdown);
      const scale = 1 + (this.countdown - Math.floor(this.countdown)) * 0.5;
      ctx.save();
      ctx.translate(canvas.width / 2, canvas.height / 2);
      ctx.scale(scale, scale);
      ctx.font = 'bold 130px sans-serif';
      ctx.textAlign = 'center';
      ctx.textBaseline = 'middle';
      ctx.fillStyle = count === 1 ? '#ff4444' : count === 2 ? '#ffaa00' : '#44ff44';
      ctx.strokeStyle = '#000';
      ctx.lineWidth = 8;
      ctx.strokeText(String(count), 0, 0);
      ctx.fillText(String(count), 0, 0);
      ctx.restore();

      ctx.font = 'bold 22px sans-serif';
      ctx.fillStyle = '#fff';
      ctx.textAlign = 'center';
      ctx.fillText('GET READY!', canvas.width / 2, canvas.height / 2 - 110);
    }
  }

  renderViewport(bike: FPBike, vw: number, vh: number) {
    const { ctx, road, renderer } = this;

    ctx.save();

    // Camera follows bike, positioned at rider's eye height
    const baseSeg = road.getSegmentAtZ(bike.position);
    const baseIndex = Math.floor(bike.position / road.segmentLength);
    const cameraZ = bike.position;
    const cameraY = CAMERA_HEIGHT + baseSeg.y;
    const cameraX = bike.playerX * road.roadWidth;

    // 1. Sky
    renderer.drawSky(bike);
    renderer.drawClouds(bike);

    // 2. Road
    renderer.drawRoad(road, bike, baseIndex, DRAW_DISTANCE, cameraX, cameraY, cameraZ);

    // 3. Scenery sprites
    renderer.drawSprites(road, bike, baseIndex, DRAW_DISTANCE, cameraX, cameraY, cameraZ);

    // 4. Other bikes ahead
    renderer.drawOtherBikes(road, bike, this.bikes, cameraX, cameraY, cameraZ);

    // 5. Wind effect
    renderer.drawWindEffect(bike);

    // 6. Boost flash
    renderer.drawSpeedFlash(bike);

    // 7. Airborne overlay
    renderer.drawAirborneEffect(bike);

    // 8. Particles
    renderer.drawParticles(this.particles);

    // 9. Vignette
    renderer.drawVignette();

    // 10. First-person cockpit (handlebars, hands, display)
    renderer.drawCockpit(bike);

    // 11. HUD
    this.drawHUD(bike, vw, vh);
    this.drawMiniMap(bike, vw);

    ctx.restore();
  }

  drawMiniMap(bike: FPBike, vw: number) {
    const { ctx } = this;
    const mapW = 200;
    const mapH = 12;
    const mapX = vw - mapW - 15;
    const mapY = 15;

    ctx.fillStyle = 'rgba(0, 0, 0, 0.55)';
    ctx.fillRect(mapX - 3, mapY - 3, mapW + 6, mapH + 6);
    ctx.fillStyle = 'rgba(255, 255, 255, 0.12)';
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

    ctx.fillStyle = '#ffd700';
    ctx.fillRect(mapX + mapW - 2, mapY, 2, mapH);
  }

  drawHUD(bike: FPBike, vw: number, vh: number) {
    const { ctx } = this;

    // Boost meter (top left, below timer area)
    ctx.fillStyle = 'rgba(0, 0, 0, 0.5)';
    ctx.fillRect(15, vh - 28, 140, 16);
    ctx.fillStyle = 'rgba(255, 255, 255, 0.15)';
    ctx.fillRect(22, vh - 24, 126, 8);
    const boostGrad = ctx.createLinearGradient(22, 0, 148, 0);
    boostGrad.addColorStop(0, '#ff6b00');
    boostGrad.addColorStop(1, '#ffdd00');
    ctx.fillStyle = boostGrad;
    ctx.fillRect(22, vh - 24, bike.boost * 126, 8);
    ctx.fillStyle = '#fff';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText('BOOST', 25, vh - 16);

    // Finished indicator
    if (bike.finished) {
      ctx.fillStyle = 'rgba(0, 255, 100, 0.85)';
      ctx.font = 'bold 30px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('FINISHED!', vw / 2, 55);
    }

    // Off-road warning
    if (Math.abs(bike.playerX) > 1) {
      const pulse = 0.7 + Math.sin(this.renderer.time * 0.3) * 0.3;
      ctx.fillStyle = `rgba(255, 120, 0, ${pulse})`;
      ctx.font = 'bold 22px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText('OFF ROAD!', vw / 2, 55);
    }
  }
}
