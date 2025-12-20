import { useState, useCallback, useRef, useEffect } from 'react';
import { GameState, Ball, Pin } from '../types/bowling';

const LANE_WIDTH = 200;
const LANE_HEIGHT = 500;
const LANE_X = 100;
const LANE_Y = 50;
const PIN_SPACING = 30;
const GRAVITY = 0.15;
const FRICTION = 0.97;

export function useBowlingGame() {
  const [gameState, setGameState] = useState<GameState>(() => initializeGame());
  const animationFrameRef = useRef<number | null>(null);
  const lastTimeRef = useRef<number>(Date.now());
  const aimingIntervalRef = useRef<NodeJS.Timeout | null>(null);

  function initializeGame(): GameState {
    return {
      ball: {
        x: LANE_X + LANE_WIDTH / 2,
        y: LANE_Y + LANE_HEIGHT - 40,
        vx: 0,
        vy: 0,
        radius: 12,
        rotation: 0,
      },
      pins: initializePins(),
      isThrowInProgress: false,
      isAiming: false,
      power: 50,
      angle: 0,
      throwCount: 0,
      frameCount: 0,
      scores: Array(10).fill(0),
      currentFrameThrows: [],
      gameOver: false,
    };
  }

  function initializePins(): Pin[] {
    const pins: Pin[] = [];
    const pinStartX = LANE_X + LANE_WIDTH / 2;
    const pinStartY = LANE_Y + 100;
    const pinPositions = [
      [0, 0],
      [-1, 1],
      [1, 1],
      [-2, 2],
      [0, 2],
      [2, 2],
      [-3, 3],
      [-1, 3],
      [1, 3],
      [3, 3],
    ];

    pinPositions.forEach((pos, index) => {
      pins.push({
        id: index,
        x: pinStartX + pos[0] * PIN_SPACING,
        y: pinStartY + pos[1] * PIN_SPACING,
        vx: 0,
        vy: 0,
        vy_gravity: 0,
        rotation: 0,
        fallen: false,
        radius: 8,
      });
    });

    return pins;
  }

  const startAiming = useCallback(() => {
    if (gameState.isThrowInProgress || gameState.gameOver) return;

    setGameState((prev) => ({
      ...prev,
      isAiming: true,
      power: 50,
    }));

    if (aimingIntervalRef.current) clearInterval(aimingIntervalRef.current);

    let increasing = true;
    aimingIntervalRef.current = setInterval(() => {
      setGameState((prev) => {
        let newPower = prev.power;
        if (increasing) {
          newPower += 2;
          if (newPower >= 100) {
            newPower = 100;
            increasing = false;
          }
        } else {
          newPower -= 2;
          if (newPower <= 20) {
            newPower = 20;
            increasing = true;
          }
        }
        return { ...prev, power: newPower };
      });
    }, 20);
  }, [gameState.isThrowInProgress, gameState.gameOver]);

  const stopAiming = useCallback(() => {
    if (aimingIntervalRef.current) {
      clearInterval(aimingIntervalRef.current);
      aimingIntervalRef.current = null;
    }
  }, []);

  const throwBall = useCallback(() => {
    stopAiming();

    setGameState((prev) => {
      const newState = { ...prev };
      newState.isAiming = false;
      newState.isThrowInProgress = true;
      newState.ball = { ...prev.ball };
      newState.ball.x = LANE_X + LANE_WIDTH / 2;
      newState.ball.y = LANE_Y + LANE_HEIGHT - 40;
      newState.ball.vx = (prev.angle / 10) * (prev.power / 50);
      newState.ball.vy = -(prev.power / 5);
      newState.ball.rotation = 0;
      return newState;
    });

    lastTimeRef.current = Date.now();
    animate();
  }, [stopAiming]);

  const updateAimAngle = useCallback((mouseX: number, canvasWidth: number) => {
    const centerX = canvasWidth / 2;
    const rawAngle = (mouseX - centerX) / 5;
    const angle = Math.max(-30, Math.min(30, rawAngle));

    setGameState((prev) => ({
      ...prev,
      angle,
    }));
  }, []);

  function animate() {
    setGameState((prev) => {
      if (!prev.isThrowInProgress) return prev;

      const newState = { ...prev };
      const ball = { ...newState.ball };
      const pins = newState.pins.map((p) => ({ ...p }));

      ball.vy -= GRAVITY;
      ball.x += ball.vx;
      ball.y += ball.vy;
      ball.vx *= FRICTION;
      ball.vy *= FRICTION;
      ball.rotation += ball.vx * 0.2;

      let ballMoving = Math.abs(ball.vx) > 0.05 || Math.abs(ball.vy) > 0.05;

      for (let pin of pins) {
        if (pin.fallen) {
          pin.vy_gravity += GRAVITY;
          pin.y += pin.vy;
          pin.vy_gravity = Math.min(pin.vy_gravity, 3);
          pin.y += pin.vy_gravity;
          pin.vx *= FRICTION;
          pin.vy *= FRICTION;
          pin.rotation += pin.vx * 0.1;

          if (pin.y > LANE_Y + LANE_HEIGHT + 50) {
            pin.y = LANE_Y + LANE_HEIGHT + 50;
            pin.vy = 0;
            pin.vy_gravity = 0;
            pin.vx = 0;
          }

          if (Math.abs(pin.vx) > 0.01 || Math.abs(pin.vy) > 0.01) {
            ballMoving = true;
          }
        }
      }

      if (ball.y > LANE_Y - 100) {
        for (let pin of pins) {
          if (!pin.fallen) {
            const dx = pin.x - ball.x;
            const dy = pin.y - ball.y;
            const distance = Math.sqrt(dx * dx + dy * dy);
            const minDistance = pin.radius + ball.radius;

            if (distance < minDistance) {
              pin.fallen = true;
              const angle = Math.atan2(dy, dx);
              const speed = Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy);
              pin.vx = Math.cos(angle) * speed * 0.6;
              pin.vy = Math.sin(angle) * speed * 0.6;
              ball.vx *= 0.7;
              ball.vy *= 0.7;
            }
          }
        }

        for (let i = 0; i < pins.length; i++) {
          for (let j = i + 1; j < pins.length; j++) {
            const pin1 = pins[i];
            const pin2 = pins[j];

            if (!pin1.fallen || !pin2.fallen) continue;

            const dx = pin2.x - pin1.x;
            const dy = pin2.y - pin1.y;
            const distance = Math.sqrt(dx * dx + dy * dy);
            const minDistance = pin1.radius + pin2.radius;

            if (distance < minDistance) {
              const angle = Math.atan2(dy, dx);
              const overlap = minDistance - distance;

              pin1.x -= Math.cos(angle) * overlap * 0.5;
              pin1.y -= Math.sin(angle) * overlap * 0.5;
              pin2.x += Math.cos(angle) * overlap * 0.5;
              pin2.y += Math.sin(angle) * overlap * 0.5;
            }
          }
        }
      }

      newState.ball = ball;
      newState.pins = pins;

      if (
        ball.y > LANE_Y + LANE_HEIGHT + 100 &&
        !ballMoving &&
        pins.every((p) => !p.fallen || (Math.abs(p.vx) < 0.01 && Math.abs(p.vy) < 0.01 && Math.abs(p.vy_gravity) < 0.01))
      ) {
        newState.isThrowInProgress = false;

        const pinsDown = pins.filter((p) => p.fallen).length;
        const newScores = [...newState.scores];
        const newFrameThrows = [...newState.currentFrameThrows, pinsDown];

        if (
          newState.frameCount < 9 &&
          (pinsDown === 10 || newFrameThrows.length === 2)
        ) {
          newScores[newState.frameCount] = calculateFrameScore(
            newState.frameCount,
            newFrameThrows,
            newState.frameCount === 9
          );
          newState.frameCount += 1;
          newState.currentFrameThrows = [];

          if (newState.frameCount >= 10) {
            newState.gameOver = true;
          } else {
            setTimeout(() => {
              setGameState((s) => ({
                ...s,
                pins: initializePins(),
              }));
            }, 800);
          }
        } else if (newState.frameCount === 9) {
          if (newFrameThrows.length === 3) {
            newScores[9] = calculateFrameScore(9, newFrameThrows, true);
            newState.frameCount = 10;
            newState.gameOver = true;
          }
          newState.currentFrameThrows = newFrameThrows;
        } else {
          newState.currentFrameThrows = newFrameThrows;
        }

        newState.scores = newScores;
      }

      return newState;
    });

    if (gameState.isThrowInProgress) {
      animationFrameRef.current = requestAnimationFrame(animate);
    }
  }

  function calculateFrameScore(
    frameIndex: number,
    throws: number[],
    isTenthFrame: boolean
  ): number {
    if (isTenthFrame) {
      return throws.reduce((a, b) => a + b, 0);
    }

    if (throws[0] === 10) {
      return 10;
    }
    if (throws[0] + throws[1] === 10) {
      return 10;
    }
    return throws[0] + throws[1];
  }

  const getTotalScore = useCallback(() => {
    return gameState.scores.reduce((a, b) => a + b, 0);
  }, [gameState.scores]);

  const resetGame = useCallback(() => {
    if (aimingIntervalRef.current) {
      clearInterval(aimingIntervalRef.current);
    }
    if (animationFrameRef.current) {
      cancelAnimationFrame(animationFrameRef.current);
    }
    setGameState(initializeGame());
  }, []);

  return {
    gameState,
    startAiming,
    stopAiming,
    throwBall,
    updateAimAngle,
    getTotalScore,
    resetGame,
  };
}
