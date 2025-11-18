import React, { useState, useEffect, useRef } from 'react';

interface Bike {
  x: number;
  y: number;
  velocityY: number;
  speed: number;
  rotation: number;
  rotationSpeed: number;
  inAir: boolean;
  trickCombo: number;
  wheelRotation: number;
}

interface Obstacle {
  id: number;
  x: number;
  y: number;
  width: number;
  height: number;
  type: 'rock' | 'cone';
}

interface Ramp {
  id: number;
  x: number;
  y: number;
  width: number;
  height: number;
}

interface Particle {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
}

interface Star {
  id: number;
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  value: number;
}

const BMXGame = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [gameState, setGameState] = useState<'playing' | 'paused' | 'gameOver'>('playing');
  const [score, setScore] = useState(0);
  const [bestTrick, setBestTrick] = useState(0);

  const gameRef = useRef({
    bike: {
      x: 150,
      y: 350,
      velocityY: 0,
      speed: 5,
      rotation: 0,
      rotationSpeed: 0,
      inAir: false,
      trickCombo: 0,
      wheelRotation: 0,
    } as Bike,
    obstacles: [] as Obstacle[],
    ramps: [] as Ramp[],
    particles: [] as Particle[],
    stars: [] as Star[],
    score: 0,
    bestTrick: 0,
    gameOver: false,
    idCounter: 0,
    obstacleCounter: 0,
    rampCounter: 0,
  });

  const keysPressed = useRef<{ [key: string]: boolean }>({});

  const GRAVITY = 0.6;
  const JUMP_POWER = -15;
  const MAX_SPEED = 12;
  const GROUND_Y = 420;
  const WIDTH = 1200;
  const HEIGHT = 600;
  const ACCELERATION = 0.3;
  const FRICTION = 0.1;

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      keysPressed.current[e.key.toLowerCase()] = true;

      if (e.key === 'p' || e.key === 'P') {
        setGameState(prev => prev === 'playing' ? 'paused' : 'playing');
      }
      if ((e.key === 'r' || e.key === 'R') && gameState === 'gameOver') {
        resetGame();
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      keysPressed.current[e.key.toLowerCase()] = false;
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [gameState]);

  const resetGame = () => {
    gameRef.current = {
      bike: {
        x: 150,
        y: 350,
        velocityY: 0,
        speed: 5,
        rotation: 0,
        rotationSpeed: 0,
        inAir: false,
        trickCombo: 0,
        wheelRotation: 0,
      },
      obstacles: [],
      ramps: [],
      particles: [],
      stars: [],
      score: 0,
      bestTrick: 0,
      gameOver: false,
      idCounter: 0,
      obstacleCounter: 0,
      rampCounter: 0,
    };
    setGameState('playing');
    setScore(0);
  };

  const createParticle = (x: number, y: number) => {
    gameRef.current.particles.push({
      id: gameRef.current.idCounter++,
      x,
      y,
      vx: (Math.random() - 0.5) * 4,
      vy: -Math.random() * 3,
      life: 30,
    });
  };

  const createStar = (x: number, y: number, value: number) => {
    for (let i = 0; i < 3; i++) {
      gameRef.current.stars.push({
        id: gameRef.current.idCounter++,
        x,
        y,
        vx: (Math.random() - 0.5) * 3,
        vy: -Math.random() * 5 - 2,
        life: 60,
        value,
      });
    }
  };

  const spawnObstacle = () => {
    const type = Math.random() > 0.5 ? 'rock' : 'cone';
    const obstacle: Obstacle = {
      id: gameRef.current.idCounter++,
      x: WIDTH + 50,
      y: GROUND_Y + 20,
      width: type === 'rock' ? 40 : 30,
      height: type === 'rock' ? 35 : 40,
      type,
    };
    gameRef.current.obstacles.push(obstacle);
  };

  const spawnRamp = () => {
    const width = 120 + Math.random() * 80;
    const height = 60 + Math.random() * 40;
    gameRef.current.ramps.push({
      id: gameRef.current.idCounter++,
      x: WIDTH + 50,
      y: GROUND_Y + 20,
      width,
      height,
    });
  };

  const updateGame = () => {
    if (gameState !== 'playing') return;

    const bike = gameRef.current.bike;

    bike.velocityY += GRAVITY;
    bike.y += bike.velocityY;
    bike.wheelRotation += bike.speed * 2;

    if (bike.y >= GROUND_Y) {
      bike.y = GROUND_Y;
      bike.velocityY = 0;

      if (bike.inAir) {
        for (let i = 0; i < 10; i++) {
          createParticle(bike.x + 25, bike.y + 30);
        }

        const trickScore = bike.trickCombo * 10;
        if (trickScore > 0) {
          createStar(bike.x, bike.y, trickScore);
          gameRef.current.score += trickScore;
          if (trickScore > gameRef.current.bestTrick) {
            gameRef.current.bestTrick = trickScore;
            setBestTrick(trickScore);
          }
        }
      }

      bike.inAir = false;
      bike.rotation = 0;
      bike.rotationSpeed = 0;
      bike.trickCombo = 0;
    } else {
      bike.inAir = true;
      bike.rotation += bike.rotationSpeed;
    }

    if (keysPressed.current['arrowup'] && !bike.inAir) {
      bike.velocityY = JUMP_POWER;
      bike.inAir = true;
    }
    if (keysPressed.current['arrowdown'] && bike.inAir) {
      bike.velocityY += 0.5;
    }
    if (keysPressed.current['arrowleft']) {
      if (bike.inAir) {
        bike.rotationSpeed = -8;
        bike.trickCombo++;
      }
    }
    if (keysPressed.current['arrowright']) {
      if (bike.inAir) {
        bike.rotationSpeed = 8;
        bike.trickCombo++;
      }
    }
    if (keysPressed.current[' ']) {
      if (bike.inAir && bike.trickCombo < 50) {
        bike.trickCombo += 5;
      }
    }

    if (bike.speed < MAX_SPEED) {
      bike.speed += ACCELERATION;
    }

    bike.x += bike.speed;

    gameRef.current.obstacles = gameRef.current.obstacles.filter(obs => {
      obs.x -= bike.speed;

      if (bike.x + 50 > obs.x && bike.x < obs.x + obs.width &&
          bike.y + 50 > obs.y && bike.y < obs.y + obs.height) {
        if (bike.inAir && bike.y + 50 < obs.y + 10) {
          return true;
        }
        gameRef.current.gameOver = true;
        setGameState('gameOver');
        return false;
      }

      return obs.x + obs.width > 0;
    });

    gameRef.current.ramps = gameRef.current.ramps.filter(ramp => {
      ramp.x -= bike.speed;

      if (bike.x + 50 > ramp.x && bike.x < ramp.x + ramp.width &&
          bike.y + 50 >= ramp.y && bike.y + 50 <= ramp.y + 20) {
        if (!bike.inAir) {
          bike.velocityY = JUMP_POWER * 1.3;
          bike.inAir = true;
        }
      }

      return ramp.x + ramp.width > 0;
    });

    gameRef.current.particles = gameRef.current.particles.filter(p => {
      p.x += p.vx;
      p.y += p.vy;
      p.vy += 0.2;
      p.life--;
      return p.life > 0;
    });

    gameRef.current.stars = gameRef.current.stars.filter(s => {
      s.x += s.vx;
      s.y += s.vy;
      s.vy += 0.15;
      s.life--;
      return s.life > 0;
    });

    gameRef.current.obstacleCounter++;
    gameRef.current.rampCounter++;

    if (gameRef.current.obstacleCounter > 100) {
      spawnObstacle();
      gameRef.current.obstacleCounter = 0;
    }

    if (gameRef.current.rampCounter > 200) {
      spawnRamp();
      gameRef.current.rampCounter = 0;
    }

    gameRef.current.score += 1;
    setScore(gameRef.current.score);
  };

  useEffect(() => {
    const gameLoop = setInterval(updateGame, 16);
    return () => clearInterval(gameLoop);
  }, [gameState]);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    ctx.clearRect(0, 0, WIDTH, HEIGHT);

    ctx.fillStyle = 'rgb(135, 206, 235)';
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    const gradient = ctx.createLinearGradient(0, 0, 0, GROUND_Y);
    gradient.addColorStop(0, 'rgb(200, 230, 255)');
    gradient.addColorStop(1, 'rgb(100, 200, 255)');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, WIDTH, GROUND_Y);

    ctx.fillStyle = 'rgb(255, 200, 50)';
    ctx.beginPath();
    ctx.arc(950, 80, 40, 0, Math.PI * 2);
    ctx.fill();

    const groundGradient = ctx.createLinearGradient(0, GROUND_Y, 0, HEIGHT);
    groundGradient.addColorStop(0, 'rgb(76, 187, 23)');
    groundGradient.addColorStop(1, 'rgb(50, 120, 15)');
    ctx.fillStyle = groundGradient;
    ctx.fillRect(0, GROUND_Y, WIDTH, HEIGHT - GROUND_Y);

    ctx.strokeStyle = 'rgb(255, 255, 255)';
    ctx.lineWidth = 3;
    for (let i = 0; i < WIDTH; i += 40) {
      ctx.beginPath();
      ctx.moveTo(i, GROUND_Y + 25);
      ctx.lineTo(i + 20, GROUND_Y + 25);
      ctx.stroke();
    }

    gameRef.current.ramps.forEach(ramp => {
      const rampGrad = ctx.createLinearGradient(ramp.x, ramp.y, ramp.x, ramp.y + ramp.height);
      rampGrad.addColorStop(0, 'rgb(160, 82, 45)');
      rampGrad.addColorStop(1, 'rgb(101, 67, 33)');
      ctx.fillStyle = rampGrad;

      ctx.beginPath();
      ctx.moveTo(ramp.x, ramp.y + ramp.height);
      ctx.lineTo(ramp.x + ramp.width, ramp.y);
      ctx.lineTo(ramp.x + ramp.width, ramp.y + ramp.height);
      ctx.fill();

      ctx.strokeStyle = 'rgb(80, 50, 20)';
      ctx.lineWidth = 3;
      ctx.stroke();
    });

    gameRef.current.obstacles.forEach(obs => {
      if (obs.type === 'rock') {
        ctx.fillStyle = 'rgb(105, 105, 105)';
        ctx.beginPath();
        ctx.moveTo(obs.x, obs.y + obs.height);
        ctx.lineTo(obs.x + obs.width / 2, obs.y);
        ctx.lineTo(obs.x + obs.width, obs.y + obs.height);
        ctx.fill();

        ctx.strokeStyle = 'rgb(60, 60, 60)';
        ctx.lineWidth = 2;
        ctx.stroke();
      } else {
        ctx.fillStyle = 'rgb(255, 140, 0)';
        ctx.beginPath();
        ctx.moveTo(obs.x + obs.width / 2, obs.y);
        ctx.lineTo(obs.x, obs.y + obs.height);
        ctx.lineTo(obs.x + obs.width, obs.y + obs.height);
        ctx.fill();

        ctx.strokeStyle = 'rgb(200, 100, 0)';
        ctx.lineWidth = 2;
        ctx.stroke();

        ctx.strokeStyle = 'white';
        ctx.lineWidth = 1;
        for (let i = 0; i < 3; i++) {
          ctx.beginPath();
          ctx.moveTo(obs.x + 5, obs.y + 10 + i * 10);
          ctx.lineTo(obs.x + obs.width - 5, obs.y + 10 + i * 10);
          ctx.stroke();
        }
      }
    });

    gameRef.current.particles.forEach(p => {
      ctx.fillStyle = `rgba(139, 90, 43, ${p.life / 30})`;
      ctx.beginPath();
      ctx.arc(p.x, p.y, 3, 0, Math.PI * 2);
      ctx.fill();
    });

    gameRef.current.stars.forEach(s => {
      const alpha = s.life / 60;
      ctx.fillStyle = `rgba(255, 215, 0, ${alpha})`;

      ctx.save();
      ctx.translate(s.x, s.y);

      const points = 5;
      const outerRadius = 8;
      const innerRadius = 3;

      ctx.beginPath();
      for (let i = 0; i < points * 2; i++) {
        const radius = i % 2 === 0 ? outerRadius : innerRadius;
        const angle = (i * Math.PI) / points;
        const x = radius * Math.cos(angle - Math.PI / 2);
        const y = radius * Math.sin(angle - Math.PI / 2);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      }
      ctx.closePath();
      ctx.fill();

      if (s.life > 45) {
        ctx.fillStyle = `rgba(255, 255, 255, ${alpha})`;
        ctx.font = 'bold 14px Arial';
        ctx.fillText('+' + s.value, 12, 4);
      }

      ctx.restore();
    });

    const bike = gameRef.current.bike;
    ctx.save();
    ctx.translate(bike.x + 25, bike.y + 25);
    ctx.rotate((bike.rotation * Math.PI) / 180);

    ctx.fillStyle = 'rgb(255, 69, 0)';
    ctx.beginPath();
    ctx.arc(-15, -5, 6, 0, Math.PI * 2);
    ctx.fill();
    ctx.beginPath();
    ctx.arc(15, -5, 6, 0, Math.PI * 2);
    ctx.fill();

    ctx.strokeStyle = 'rgb(64, 64, 64)';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(-20, 0);
    ctx.lineTo(20, 0);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(0, 0);
    ctx.lineTo(0, -15);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(-10, 0);
    ctx.lineTo(-5, 10);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(10, 0);
    ctx.lineTo(5, 10);
    ctx.stroke();

    ctx.fillStyle = 'rgb(255, 200, 150)';
    ctx.beginPath();
    ctx.arc(0, -20, 5, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = 'rgb(0, 100, 200)';
    ctx.fillRect(-4, -12, 8, 10);

    ctx.restore();

    ctx.fillStyle = 'rgba(0, 0, 0, 0.7)';
    ctx.fillRect(10, 10, 280, 140);

    ctx.fillStyle = 'white';
    ctx.font = 'bold 20px Arial';
    ctx.fillText('Score: ' + gameRef.current.score, 25, 40);
    ctx.fillText('Speed: ' + bike.speed.toFixed(1), 25, 70);
    ctx.fillText('Best Trick: ' + gameRef.current.bestTrick, 25, 100);
    ctx.fillText('Rotation: ' + Math.round(bike.rotation) + '°', 25, 130);

    ctx.font = '14px Arial';
    ctx.fillText('↑Jump ←→Flip SPACE:Trick P:Pause', WIDTH - 300, 30);

    if (gameState === 'gameOver') {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
      ctx.fillRect(0, 0, WIDTH, HEIGHT);

      ctx.fillStyle = 'rgb(255, 50, 50)';
      ctx.font = 'bold 70px Arial';
      const gameOverText = 'GAME OVER';
      const metrics = ctx.measureText(gameOverText);
      ctx.fillText(gameOverText, (WIDTH - metrics.width) / 2, HEIGHT / 2 - 40);

      ctx.fillStyle = 'white';
      ctx.font = '28px Arial';
      const scoreText = 'Final Score: ' + gameRef.current.score;
      const scoreMetrics = ctx.measureText(scoreText);
      ctx.fillText(scoreText, (WIDTH - scoreMetrics.width) / 2, HEIGHT / 2 + 40);

      ctx.font = '20px Arial';
      const restartText = 'Press R to Restart';
      const restartMetrics = ctx.measureText(restartText);
      ctx.fillText(restartText, (WIDTH - restartMetrics.width) / 2, HEIGHT / 2 + 90);
    }

    if (gameState === 'paused') {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
      ctx.fillRect(0, 0, WIDTH, HEIGHT);

      ctx.fillStyle = 'rgb(255, 255, 0)';
      ctx.font = 'bold 60px Arial';
      const pauseText = 'PAUSED';
      const metrics = ctx.measureText(pauseText);
      ctx.fillText(pauseText, (WIDTH - metrics.width) / 2, HEIGHT / 2);
    }
  }, [gameState]);

  return (
    <div className="min-h-screen bg-black flex flex-col items-center justify-center gap-4">
      <div className="relative">
        <canvas
          ref={canvasRef}
          width={WIDTH}
          height={HEIGHT}
          className="border-4 border-gray-800 rounded-lg shadow-2xl"
        />
      </div>
      <div className="text-white text-center">
        <p className="text-xl font-bold">BMX Stunt Racing Game</p>
        <p className="text-sm text-gray-400">Controls: Arrow Keys + Space + P (Pause) + R (Restart)</p>
      </div>
    </div>
  );
};

export default BMXGame;
