import { useState, useCallback, useRef, useEffect } from 'react';
import { GameState, Player, Ball, Course, Particle } from '../types/golf';
import { courses } from '../data/courses';

const PLAYER_COLORS = ['#FF6B6B', '#4ECDC4', '#FFE66D', '#95E1D3'];

const createInitialBall = (x: number, y: number): Ball => ({
  x,
  y,
  vx: 0,
  vy: 0,
  isMoving: false,
  trail: [],
});

const createPlayer = (id: string, name: string, isComputer: boolean, color: string): Player => ({
  id,
  name,
  color,
  scores: [],
  totalScore: 0,
  isComputer,
});

export const useGolfGame = () => {
  const [gameState, setGameState] = useState<GameState | null>(null);
  const [particles, setParticles] = useState<Particle[]>([]);
  const animationRef = useRef<number | null>(null);
  const computerTimeoutRef = useRef<NodeJS.Timeout | null>(null);

  const createParticles = useCallback((x: number, y: number, color: string, count: number = 20) => {
    const newParticles: Particle[] = [];
    for (let i = 0; i < count; i++) {
      const angle = (Math.PI * 2 * i) / count + Math.random() * 0.5;
      const speed = 2 + Math.random() * 4;
      newParticles.push({
        x,
        y,
        vx: Math.cos(angle) * speed,
        vy: Math.sin(angle) * speed,
        life: 1,
        maxLife: 1,
        color,
        size: 3 + Math.random() * 5,
      });
    }
    setParticles(prev => [...prev, ...newParticles]);
  }, []);

  const startGame = useCallback((playerNames: string[], computerOpponent: boolean) => {
    const players: Player[] = playerNames.map((name, i) =>
      createPlayer(`player-${i}`, name, false, PLAYER_COLORS[i])
    );

    if (computerOpponent) {
      players.push(createPlayer('computer', 'Computer', true, PLAYER_COLORS[players.length]));
    }

    const balls = new Map<string, Ball>();
    players.forEach(p => {
      balls.set(p.id, createInitialBall(courses[0].tee.x, courses[0].tee.y));
    });

    setGameState({
      players,
      currentPlayerIndex: 0,
      currentHole: 0,
      courses,
      balls,
      gamePhase: 'aiming',
      swingPower: 50,
      swingAngle: 0,
      strokes: 0,
      message: `${players[0].name}'s turn - Aim and swing!`,
    });
    setParticles([]);
  }, []);

  const updateBallPhysics = useCallback((ball: Ball, course: Course): Ball => {
    if (!ball.isMoving) return ball;

    const friction = 0.985;
    const windEffect = course.wind.speed * 0.01;

    let newVx = ball.vx * friction + Math.cos(course.wind.direction * Math.PI / 180) * windEffect;
    let newVy = ball.vy * friction + Math.sin(course.wind.direction * Math.PI / 180) * windEffect;

    const speed = Math.sqrt(newVx * newVx + newVy * newVy);

    if (speed < 0.1) {
      return { ...ball, vx: 0, vy: 0, isMoving: false };
    }

    const newX = ball.x + newVx;
    const newY = ball.y + newVy;

    const newTrail = [...ball.trail, { x: ball.x, y: ball.y }].slice(-20);

    return {
      ...ball,
      x: newX,
      y: newY,
      vx: newVx,
      vy: newVy,
      trail: newTrail,
    };
  }, []);

  const checkCollisions = useCallback((ball: Ball, course: Course): Ball => {
    let updatedBall = { ...ball };

    for (const obstacle of course.obstacles) {
      if (
        ball.x >= obstacle.x &&
        ball.x <= obstacle.x + obstacle.width &&
        ball.y >= obstacle.y &&
        ball.y <= obstacle.y + obstacle.height
      ) {
        if (obstacle.type === 'water') {
          createParticles(ball.x, ball.y, '#00BFFF', 30);
          return {
            ...ball,
            x: course.tee.x,
            y: course.tee.y,
            vx: 0,
            vy: 0,
            isMoving: false,
            trail: [],
          };
        } else if (obstacle.type === 'bunker') {
          updatedBall.vx *= 0.5;
          updatedBall.vy *= 0.5;
        } else if (obstacle.type === 'tree' || obstacle.type === 'rock') {
          updatedBall.vx *= -0.6;
          updatedBall.vy *= -0.6;
          createParticles(ball.x, ball.y, '#8B4513', 10);
        }
      }
    }

    const dx = ball.x - course.hole.x;
    const dy = ball.y - course.hole.y;
    const distance = Math.sqrt(dx * dx + dy * dy);

    if (distance < course.hole.radius && Math.sqrt(ball.vx * ball.vx + ball.vy * ball.vy) < 5) {
      createParticles(course.hole.x, course.hole.y, '#FFD700', 40);
      return {
        ...ball,
        x: course.hole.x,
        y: course.hole.y,
        vx: 0,
        vy: 0,
        isMoving: false,
        trail: [],
      };
    }

    if (ball.x < 0 || ball.x > 800 || ball.y < 0 || ball.y > 500) {
      return {
        ...ball,
        x: course.tee.x,
        y: course.tee.y,
        vx: 0,
        vy: 0,
        isMoving: false,
        trail: [],
      };
    }

    return updatedBall;
  }, [createParticles]);

  const swing = useCallback((power: number, angle: number) => {
    if (!gameState || gameState.gamePhase !== 'aiming') return;

    const currentPlayer = gameState.players[gameState.currentPlayerIndex];
    const currentBall = gameState.balls.get(currentPlayer.id);

    if (!currentBall) return;

    const radians = (angle - 90) * Math.PI / 180;
    const velocity = power * 0.15;

    const newBall: Ball = {
      ...currentBall,
      vx: Math.cos(radians) * velocity,
      vy: Math.sin(radians) * velocity,
      isMoving: true,
    };

    const newBalls = new Map(gameState.balls);
    newBalls.set(currentPlayer.id, newBall);

    setGameState(prev => prev ? {
      ...prev,
      balls: newBalls,
      gamePhase: 'ball-moving',
      strokes: prev.strokes + 1,
      message: 'Ball in flight...',
    } : null);
  }, [gameState]);

  const computerSwing = useCallback(() => {
    if (!gameState) return;

    const course = gameState.courses[gameState.currentHole];
    const currentPlayer = gameState.players[gameState.currentPlayerIndex];
    const ball = gameState.balls.get(currentPlayer.id);

    if (!ball) return;

    const dx = course.hole.x - ball.x;
    const dy = course.hole.y - ball.y;
    const distance = Math.sqrt(dx * dx + dy * dy);

    const baseAngle = Math.atan2(dy, dx) * 180 / Math.PI + 90;
    const angleVariation = (Math.random() - 0.5) * 20;
    const angle = baseAngle + angleVariation;

    const basePower = Math.min(100, distance / 5);
    const powerVariation = (Math.random() - 0.5) * 20;
    const power = Math.max(20, Math.min(100, basePower + powerVariation));

    swing(power, angle);
  }, [gameState, swing]);

  useEffect(() => {
    if (!gameState || gameState.gamePhase !== 'ball-moving') {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
        animationRef.current = null;
      }
      return;
    }

    const animate = () => {
      setGameState(prev => {
        if (!prev || prev.gamePhase !== 'ball-moving') return prev;

        const currentPlayer = prev.players[prev.currentPlayerIndex];
        const currentBall = prev.balls.get(currentPlayer.id);
        const course = prev.courses[prev.currentHole];

        if (!currentBall) return prev;

        let updatedBall = updateBallPhysics(currentBall, course);
        updatedBall = checkCollisions(updatedBall, course);

        const newBalls = new Map(prev.balls);
        newBalls.set(currentPlayer.id, updatedBall);

        if (!updatedBall.isMoving) {
          const dx = updatedBall.x - course.hole.x;
          const dy = updatedBall.y - course.hole.y;
          const distance = Math.sqrt(dx * dx + dy * dy);

          if (distance < course.hole.radius) {
            const newPlayers = [...prev.players];
            newPlayers[prev.currentPlayerIndex] = {
              ...currentPlayer,
              scores: [...currentPlayer.scores, prev.strokes],
              totalScore: currentPlayer.totalScore + prev.strokes,
            };

            const allDone = newPlayers.every(p => p.scores.length > prev.currentHole);

            if (allDone) {
              if (prev.currentHole >= prev.courses.length - 1) {
                return {
                  ...prev,
                  balls: newBalls,
                  players: newPlayers,
                  gamePhase: 'game-over',
                  message: 'Game Over! Check the final scores.',
                };
              } else {
                const nextHole = prev.currentHole + 1;
                const nextCourse = prev.courses[nextHole];
                const resetBalls = new Map<string, Ball>();
                newPlayers.forEach(p => {
                  resetBalls.set(p.id, createInitialBall(nextCourse.tee.x, nextCourse.tee.y));
                });

                return {
                  ...prev,
                  balls: resetBalls,
                  players: newPlayers,
                  currentHole: nextHole,
                  currentPlayerIndex: 0,
                  strokes: 0,
                  gamePhase: 'aiming',
                  message: `Hole ${nextHole + 1} - ${newPlayers[0].name}'s turn`,
                };
              }
            } else {
              const nextPlayerIndex = (prev.currentPlayerIndex + 1) % prev.players.length;
              const nextPlayer = newPlayers[nextPlayerIndex];
              const nextCourse = prev.courses[prev.currentHole];
              const nextBall = createInitialBall(nextCourse.tee.x, nextCourse.tee.y);
              newBalls.set(nextPlayer.id, nextBall);

              return {
                ...prev,
                balls: newBalls,
                players: newPlayers,
                currentPlayerIndex: nextPlayerIndex,
                strokes: 0,
                gamePhase: 'aiming',
                message: `${nextPlayer.name}'s turn`,
              };
            }
          }

          const nextPlayerIndex = (prev.currentPlayerIndex + 1) % prev.players.length;
          const nextPlayer = prev.players[nextPlayerIndex];

          return {
            ...prev,
            balls: newBalls,
            currentPlayerIndex: nextPlayerIndex,
            gamePhase: 'aiming',
            message: `${nextPlayer.name}'s turn`,
          };
        }

        return { ...prev, balls: newBalls };
      });

      animationRef.current = requestAnimationFrame(animate);
    };

    animationRef.current = requestAnimationFrame(animate);

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, [gameState?.gamePhase, updateBallPhysics, checkCollisions]);

  useEffect(() => {
    if (!gameState) return;

    const currentPlayer = gameState.players[gameState.currentPlayerIndex];
    if (currentPlayer.isComputer && gameState.gamePhase === 'aiming') {
      computerTimeoutRef.current = setTimeout(() => {
        computerSwing();
      }, 1500);
    }

    return () => {
      if (computerTimeoutRef.current) {
        clearTimeout(computerTimeoutRef.current);
      }
    };
  }, [gameState?.currentPlayerIndex, gameState?.gamePhase, computerSwing]);

  useEffect(() => {
    if (particles.length === 0) return;

    const interval = setInterval(() => {
      setParticles(prev =>
        prev
          .map(p => ({
            ...p,
            x: p.x + p.vx,
            y: p.y + p.vy,
            vy: p.vy + 0.1,
            life: p.life - 0.02,
          }))
          .filter(p => p.life > 0)
      );
    }, 16);

    return () => clearInterval(interval);
  }, [particles.length]);

  const updateSwingPower = useCallback((power: number) => {
    setGameState(prev => prev ? { ...prev, swingPower: power } : null);
  }, []);

  const updateSwingAngle = useCallback((angle: number) => {
    setGameState(prev => prev ? { ...prev, swingAngle: angle } : null);
  }, []);

  return {
    gameState,
    particles,
    startGame,
    swing,
    updateSwingPower,
    updateSwingAngle,
  };
};
