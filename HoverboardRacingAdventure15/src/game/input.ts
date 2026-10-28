import type { InputState } from './types';

export class InputManager {
  keys: Set<string> = new Set();

  constructor() {
    window.addEventListener('keydown', this.onKeyDown);
    window.addEventListener('keyup', this.onKeyUp);
  }

  destroy(): void {
    window.removeEventListener('keydown', this.onKeyDown);
    window.removeEventListener('keyup', this.onKeyUp);
  }

  private onKeyDown = (e: KeyboardEvent) => {
    this.keys.add(e.code);
    // Prevent scrolling with arrow keys / space
    if (
      e.code === 'ArrowUp' ||
      e.code === 'ArrowDown' ||
      e.code === 'ArrowLeft' ||
      e.code === 'ArrowRight' ||
      e.code === 'Space'
    ) {
      e.preventDefault();
    }
  };

  private onKeyUp = (e: KeyboardEvent) => {
    this.keys.delete(e.code);
  };

  /** Player 1 controls: Arrow keys + Space for boost */
  getPlayer1Input(): InputState {
    return {
      accelerate: this.keys.has('ArrowUp'),
      brake: this.keys.has('ArrowDown'),
      left: this.keys.has('ArrowLeft'),
      right: this.keys.has('ArrowRight'),
      boost: this.keys.has('Space'),
    };
  }

  /** Player 2 controls: WASD + Shift for boost */
  getPlayer2Input(): InputState {
    return {
      accelerate: this.keys.has('KeyW'),
      brake: this.keys.has('KeyS'),
      left: this.keys.has('KeyA'),
      right: this.keys.has('KeyD'),
      boost: this.keys.has('ShiftLeft') || this.keys.has('ShiftRight'),
    };
  }
}
