import { SkateboardState } from './physics';

export class GameRenderer {
  private ctx: CanvasRenderingContext2D;
  private canvas: HTMLCanvasElement;

  constructor(canvas: HTMLCanvasElement) {
    this.canvas = canvas;
    const ctx = canvas.getContext('2d');
    if (!ctx) throw new Error('Could not get canvas context');
    this.ctx = ctx;
  }

  render(state: SkateboardState, score: number, combo: number, fps: number) {
    this.clearCanvas();
    this.drawBackground();
    this.drawGround();
    this.drawObstacles();
    this.drawSkateboarder(state);
    this.drawUI(score, combo, fps);
  }

  private clearCanvas() {
    this.ctx.clearRect(0, 0, this.canvas.width, this.canvas.height);
  }

  private drawBackground() {
    const gradient = this.ctx.createLinearGradient(0, 0, 0, this.canvas.height);
    gradient.addColorStop(0, '#87CEEB');
    gradient.addColorStop(1, '#E0F6FF');
    this.ctx.fillStyle = gradient;
    this.ctx.fillRect(0, 0, this.canvas.width, this.canvas.height);

    this.ctx.fillStyle = 'rgba(255, 255, 255, 0.3)';
    for (let i = 0; i < 3; i++) {
      const cloudX = (Date.now() / 50 + i * 150) % this.canvas.width;
      this.drawCloud(cloudX, 50 + i * 30);
    }
  }

  private drawCloud(x: number, y: number) {
    this.ctx.beginPath();
    this.ctx.arc(x, y, 20, 0, Math.PI * 2);
    this.ctx.arc(x + 20, y, 25, 0, Math.PI * 2);
    this.ctx.arc(x + 40, y, 20, 0, Math.PI * 2);
    this.ctx.fill();
  }

  private drawGround() {
    const groundY = 380;
    this.ctx.fillStyle = '#8B7355';
    this.ctx.fillRect(0, groundY, this.canvas.width, this.canvas.height - groundY);

    this.ctx.fillStyle = '#A0826D';
    for (let i = 0; i < 20; i++) {
      this.ctx.fillRect(i * 50, groundY, 25, 5);
    }

    this.ctx.strokeStyle = '#6B5344';
    this.ctx.lineWidth = 2;
    this.ctx.beginPath();
    this.ctx.moveTo(0, groundY);
    this.ctx.lineTo(this.canvas.width, groundY);
    this.ctx.stroke();
  }

  private drawObstacles() {
    this.ctx.fillStyle = '#FF6B6B';
    this.ctx.fillRect(150, 320, 40, 60);

    this.ctx.fillStyle = '#4ECDC4';
    this.ctx.fillRect(350, 330, 50, 50);

    this.ctx.fillStyle = '#FFD93D';
    this.ctx.fillRect(550, 310, 60, 70);

    this.ctx.fillStyle = '#95E1D3';
    this.ctx.fillRect(700, 325, 45, 55);
  }

  private drawSkateboarder(state: SkateboardState) {
    const { x, y } = state.position;

    this.ctx.save();
    this.ctx.translate(x, y);
    this.ctx.rotate(state.rotation);

    this.ctx.fillStyle = '#8B4513';
    this.ctx.fillRect(-25, -8, 50, 16);

    this.ctx.fillStyle = '#000000';
    this.ctx.beginPath();
    this.ctx.arc(-18, 0, 4, 0, Math.PI * 2);
    this.ctx.fill();
    this.ctx.beginPath();
    this.ctx.arc(18, 0, 4, 0, Math.PI * 2);
    this.ctx.fill();

    this.ctx.restore();

    this.drawRider(x, y, state.rotation, state.isAirborne);
  }

  private drawRider(x: number, y: number, rotation: number, isAirborne: boolean) {
    this.ctx.save();
    this.ctx.translate(x, y - 35);

    const tiltAngle = rotation * 0.5;
    this.ctx.rotate(tiltAngle);

    this.ctx.fillStyle = '#FFB86C';
    this.ctx.beginPath();
    this.ctx.arc(0, -15, 8, 0, Math.PI * 2);
    this.ctx.fill();

    this.ctx.fillStyle = '#FF6B9D';
    this.ctx.fillRect(-6, -5, 12, 20);

    if (isAirborne) {
      this.ctx.fillStyle = '#4A90E2';
      this.ctx.fillRect(-10, 10, 8, 15);
      this.ctx.fillRect(2, 10, 8, 15);
    } else {
      this.ctx.fillStyle = '#4A90E2';
      this.ctx.fillRect(-10, 10, 8, 20);
      this.ctx.fillRect(2, 10, 8, 20);
    }

    this.ctx.restore();
  }

  private drawUI(score: number, combo: number, fps: number) {
    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(10, 10, 200, 100);

    this.ctx.fillStyle = '#FFFFFF';
    this.ctx.font = 'bold 24px Arial';
    this.ctx.fillText(`Score: ${score}`, 20, 40);

    this.ctx.font = 'bold 20px Arial';
    this.ctx.fillStyle = combo > 1 ? '#FFD700' : '#FFFFFF';
    this.ctx.fillText(`Combo: x${combo}`, 20, 70);

    this.ctx.font = '14px Arial';
    this.ctx.fillStyle = '#AAAAAA';
    this.ctx.fillText(`FPS: ${fps}`, 20, 95);

    this.ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    this.ctx.fillRect(this.canvas.width - 210, 10, 200, 120);

    this.ctx.fillStyle = '#FFFFFF';
    this.ctx.font = 'bold 16px Arial';
    this.ctx.fillText('Controls:', this.canvas.width - 200, 35);

    this.ctx.font = '14px Arial';
    this.ctx.fillText('↑ Ollie / ↓ Stop', this.canvas.width - 200, 55);
    this.ctx.fillText('← → Move', this.canvas.width - 200, 75);
    this.ctx.fillText('Q Kickflip | W Heelflip', this.canvas.width - 200, 95);
    this.ctx.fillText('E Big Spin | Space Land', this.canvas.width - 200, 115);
  }
}
