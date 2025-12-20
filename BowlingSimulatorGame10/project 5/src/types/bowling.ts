export interface Ball {
  x: number;
  y: number;
  vx: number;
  vy: number;
  radius: number;
  rotation: number;
}

export interface Pin {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  vy_gravity: number;
  rotation: number;
  fallen: boolean;
  radius: number;
}

export interface GameState {
  ball: Ball;
  pins: Pin[];
  isThrowInProgress: boolean;
  isAiming: boolean;
  power: number;
  angle: number;
  throwCount: number;
  frameCount: number;
  scores: number[];
  currentFrameThrows: number[];
  gameOver: boolean;
}

export interface AimingState {
  isAiming: boolean;
  power: number;
  angle: number;
  increasingPower: boolean;
}
