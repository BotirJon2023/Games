export interface Vector2 {
  x: number;
  y: number;
}

export interface SkateboardState {
  position: Vector2;
  velocity: Vector2;
  rotation: number;
  angularVelocity: number;
  isAirborne: boolean;
  isGrinding: boolean;
  trickState: TrickState;
}

export interface TrickState {
  currentTrick: string;
  progress: number;
  maxProgress: number;
}

export const GRAVITY = 0.6;
export const FRICTION = 0.98;
export const AIR_RESISTANCE = 0.99;
export const ROTATION_DAMPING = 0.92;

export class PhysicsEngine {
  update(state: SkateboardState, keys: Set<string>): SkateboardState {
    let newState = { ...state };

    if (newState.isAirborne) {
      newState.velocity.y += GRAVITY;
      newState.velocity.x *= AIR_RESISTANCE;
      newState.angularVelocity *= ROTATION_DAMPING;

      if (keys.has('ArrowLeft')) {
        newState.angularVelocity -= 0.08;
      }
      if (keys.has('ArrowRight')) {
        newState.angularVelocity += 0.08;
      }
    } else {
      newState.velocity.x *= FRICTION;

      if (keys.has('ArrowRight') && newState.velocity.x < 8) {
        newState.velocity.x += 0.5;
      }
      if (keys.has('ArrowLeft') && newState.velocity.x > -8) {
        newState.velocity.x -= 0.5;
      }
    }

    newState.position.x += newState.velocity.x;
    newState.position.y += newState.velocity.y;
    newState.rotation += newState.angularVelocity;

    const groundLevel = 380;
    if (newState.position.y >= groundLevel) {
      newState.position.y = groundLevel;
      newState.velocity.y = 0;
      newState.isAirborne = false;
      newState.angularVelocity *= 0.85;

      if (Math.abs(newState.rotation) > Math.PI * 2) {
        newState.rotation = newState.rotation % (Math.PI * 2);
      }
    }

    newState.position.x = Math.max(10, Math.min(790, newState.position.x));

    return newState;
  }

  handleOllie(state: SkateboardState): SkateboardState {
    if (!state.isAirborne) {
      return {
        ...state,
        velocity: {
          ...state.velocity,
          y: -12,
        },
        isAirborne: true,
      };
    }
    return state;
  }

  handleKickflip(state: SkateboardState): SkateboardState {
    if (!state.isAirborne) {
      return {
        ...state,
        velocity: {
          ...state.velocity,
          y: -13,
        },
        angularVelocity: -0.3,
        isAirborne: true,
      };
    }
    return state;
  }

  handleHeelflip(state: SkateboardState): SkateboardState {
    if (!state.isAirborne) {
      return {
        ...state,
        velocity: {
          ...state.velocity,
          y: -13,
        },
        angularVelocity: 0.3,
        isAirborne: true,
      };
    }
    return state;
  }

  handleBigSpin(state: SkateboardState): SkateboardState {
    if (!state.isAirborne) {
      return {
        ...state,
        velocity: {
          ...state.velocity,
          y: -14,
          x: state.velocity.x + 2,
        },
        angularVelocity: -0.5,
        isAirborne: true,
      };
    }
    return state;
  }
}
