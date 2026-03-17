import { Player, Ball } from '../types/game';
import {
  CANVAS_WIDTH,
  CANVAS_HEIGHT,
  GROUND_HEIGHT,
  NET_HEIGHT,
  NET_X,
  NET_WIDTH,
  COLORS,
} from '../constants/game';

export class MinecraftRenderer {
  private ctx: CanvasRenderingContext2D;

  constructor(ctx: CanvasRenderingContext2D) {
    this.ctx = ctx;
  }

  clear() {
    this.ctx.fillStyle = COLORS.sky;
    this.ctx.fillRect(0, 0, CANVAS_WIDTH, CANVAS_HEIGHT);
  }

  drawGround() {
    this.ctx.fillStyle = COLORS.grass;
    this.ctx.fillRect(0, GROUND_HEIGHT, CANVAS_WIDTH, CANVAS_HEIGHT - GROUND_HEIGHT);

    this.ctx.fillStyle = COLORS.ground;
    for (let i = 0; i < CANVAS_WIDTH; i += 20) {
      for (let j = GROUND_HEIGHT + 20; j < CANVAS_HEIGHT; j += 20) {
        if ((i / 20 + j / 20) % 2 === 0) {
          this.ctx.fillRect(i, j, 20, 20);
        }
      }
    }
  }

  drawNet() {
    this.ctx.fillStyle = COLORS.net;
    this.ctx.fillRect(NET_X - NET_WIDTH / 2, GROUND_HEIGHT - NET_HEIGHT, NET_WIDTH, NET_HEIGHT);

    this.ctx.strokeStyle = '#000000';
    this.ctx.lineWidth = 2;
    for (let i = 0; i < NET_HEIGHT; i += 10) {
      this.ctx.beginPath();
      this.ctx.moveTo(NET_X - NET_WIDTH / 2, GROUND_HEIGHT - NET_HEIGHT + i);
      this.ctx.lineTo(NET_X + NET_WIDTH / 2, GROUND_HEIGHT - NET_HEIGHT + i);
      this.ctx.stroke();
    }
  }

  drawBlockyPlayer(player: Player) {
    const { position, width, height, color } = player;
    const x = position.x - width / 2;
    const y = position.y - height;

    this.ctx.fillStyle = color;
    this.ctx.fillRect(x, y, width, height);

    const darkColor = this.darkenColor(color, 0.2);
    this.ctx.fillStyle = darkColor;
    this.ctx.fillRect(x + width - 8, y, 8, height);
    this.ctx.fillRect(x, y + height - 8, width, 8);

    const headSize = width * 0.8;
    const headY = y - headSize - 2;
    this.ctx.fillStyle = '#FFD4A3';
    this.ctx.fillRect(x + width / 2 - headSize / 2, headY, headSize, headSize);

    const eyeSize = 4;
    this.ctx.fillStyle = '#000000';
    this.ctx.fillRect(x + width / 2 - headSize / 3, headY + headSize / 3, eyeSize, eyeSize);
    this.ctx.fillRect(x + width / 2 + headSize / 6, headY + headSize / 3, eyeSize, eyeSize);

    this.ctx.strokeStyle = '#000000';
    this.ctx.lineWidth = 1;
    this.ctx.strokeRect(x, y, width, height);
    this.ctx.strokeRect(x + width / 2 - headSize / 2, headY, headSize, headSize);
  }

  drawBlockyBall(ball: Ball) {
    const { position, radius } = ball;
    const size = radius * 2;
    const x = position.x - radius;
    const y = position.y - radius;

    this.ctx.fillStyle = COLORS.ball;
    this.ctx.fillRect(x, y, size, size);

    const darkYellow = this.darkenColor(COLORS.ball, 0.15);
    this.ctx.fillStyle = darkYellow;
    this.ctx.fillRect(x + size - 5, y, 5, size);
    this.ctx.fillRect(x, y + size - 5, size, 5);

    this.ctx.strokeStyle = '#000000';
    this.ctx.lineWidth = 2;
    this.ctx.strokeRect(x, y, size, size);

    this.ctx.strokeStyle = '#000000';
    this.ctx.lineWidth = 1;
    this.ctx.beginPath();
    this.ctx.moveTo(x, y);
    this.ctx.lineTo(x + size, y + size);
    this.ctx.stroke();
    this.ctx.beginPath();
    this.ctx.moveTo(x + size, y);
    this.ctx.lineTo(x, y + size);
    this.ctx.stroke();
  }

  drawScore(player1Score: number, player2Score: number) {
    this.ctx.fillStyle = COLORS.ui;
    this.ctx.font = 'bold 48px monospace';
    this.ctx.textAlign = 'center';
    this.ctx.fillText(`${player1Score}`, CANVAS_WIDTH / 4, 60);
    this.ctx.fillText(`${player2Score}`, (CANVAS_WIDTH * 3) / 4, 60);

    this.ctx.strokeStyle = '#FFFFFF';
    this.ctx.lineWidth = 3;
    this.ctx.strokeText(`${player1Score}`, CANVAS_WIDTH / 4, 60);
    this.ctx.strokeText(`${player2Score}`, (CANVAS_WIDTH * 3) / 4, 60);
  }

  private darkenColor(color: string, amount: number): string {
    const hex = color.replace('#', '');
    const r = Math.max(0, parseInt(hex.substring(0, 2), 16) * (1 - amount));
    const g = Math.max(0, parseInt(hex.substring(2, 4), 16) * (1 - amount));
    const b = Math.max(0, parseInt(hex.substring(4, 6), 16) * (1 - amount));
    return `#${Math.floor(r).toString(16).padStart(2, '0')}${Math.floor(g).toString(16).padStart(2, '0')}${Math.floor(b).toString(16).padStart(2, '0')}`;
  }
}
