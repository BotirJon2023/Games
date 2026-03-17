import { Player } from '../types/game';
import { PLAYER_SPEED, PLAYER_JUMP_FORCE, GROUND_HEIGHT } from '../constants/game';

export class Controls {
  private keys: Set<string> = new Set();

  constructor() {
    window.addEventListener('keydown', (e) => this.keys.add(e.key.toLowerCase()));
    window.addEventListener('keyup', (e) => this.keys.delete(e.key.toLowerCase()));
  }

  updatePlayer1(player: Player) {
    if (this.keys.has('a')) {
      player.velocity.x = -PLAYER_SPEED;
    }
    if (this.keys.has('d')) {
      player.velocity.x = PLAYER_SPEED;
    }
    if (this.keys.has('w') && player.position.y >= GROUND_HEIGHT) {
      player.velocity.y = -PLAYER_JUMP_FORCE;
    }
  }

  updatePlayer2(player: Player) {
    if (this.keys.has('arrowleft')) {
      player.velocity.x = -PLAYER_SPEED;
    }
    if (this.keys.has('arrowright')) {
      player.velocity.x = PLAYER_SPEED;
    }
    if (this.keys.has('arrowup') && player.position.y >= GROUND_HEIGHT) {
      player.velocity.y = -PLAYER_JUMP_FORCE;
    }
  }

  isSpacePressed(): boolean {
    return this.keys.has(' ');
  }

  isPausePressed(): boolean {
    return this.keys.has('escape') || this.keys.has('p');
  }

  cleanup() {
    this.keys.clear();
  }
}
