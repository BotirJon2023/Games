import React, { useState, useEffect, useCallback, useRef } from 'react';
import { Cyclist, Obstacle, PowerUp, Particle, GameConfig, GameStats, ActivePowerUp } from '../types/game';
import GameHUD from './GameHUD';
import CyclistComponent from './CyclistComponent';
import ObstacleComponent from './ObstacleComponent';
import PowerUpComponent from './PowerUpComponent';
import ParticleSystem from './ParticleSystem';
import { generateObstacle, generatePowerUp, generateParticles } from '../utils/gameUtils';
import { playSound, SoundType } from '../utils/soundUtils';

interface GamePlayProps {
  onGameEnd: (score: number) => void;
}

const GamePlay: React.FC<GamePlayProps> = ({ onGameEnd }) => {
  const gameLoopRef = useRef<number>();
  const keysRef = useRef<{ [key: string]: boolean }>({});
  const lastTimeRef = useRef<number>(0);
  const obstacleSpawnTimer = useRef<number>(0);
  const powerUpSpawnTimer = useRef<number>(0);
  const levelUpTimer = useRef<number>(0);

  const gameConfig: GameConfig = {
    gravity: 0.8,
    jumpPower: -15,
    baseSpeed: 5,
    maxSpeed: 15,
    acceleration: 0.1,
    friction: 0.95,
    groundLevel: 400
  };

  const [cyclist, setCyclist] = useState<Cyclist>({
    position: { x: 100, y: gameConfig.groundLevel },
    velocity: { x: 0, y: 0 },
    isJumping: false,
    isGrounded: true,
    rotation: 0,
    health: 100,
    invulnerable: false
  });

  const [obstacles, setObstacles] = useState<Obstacle[]>([]);
  const [powerUps, setPowerUps] = useState<PowerUp[]>([]);
  const [particles, setParticles] = useState<Particle[]>([]);
  const [activePowerUps, setActivePowerUps] = useState<ActivePowerUp[]>([]);
  
  const [gameStats, setGameStats] = useState<GameStats>({
    score: 0,
    distance: 0,
    timeElapsed: 0,
    level: 1,
    combo: 0
  });

  const [gameSpeed, setGameSpeed] = useState<number>(gameConfig.baseSpeed);

  // Input handling
  const handleKeyDown = useCallback((e: KeyboardEvent) => {
    keysRef.current[e.key] = true;
    if (e.key === ' ') {
      e.preventDefault();
      if (cyclist.isGrounded) {
        playSound('jump');
        setCyclist(prev => ({
          ...prev,
          velocity: { ...prev.velocity, y: gameConfig.jumpPower },
          isJumping: true,
          isGrounded: false
        }));
      }
    }
  }, [cyclist.isGrounded]);

  const handleKeyUp = useCallback((e: KeyboardEvent) => {
    keysRef.current[e.key] = false;
  }, []);

  useEffect(() => {
    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);
    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [handleKeyDown, handleKeyUp]);

  // Game loop
  const gameLoop = useCallback((currentTime: number) => {
    const deltaTime = currentTime - lastTimeRef.current;
    lastTimeRef.current = currentTime;

    if (deltaTime > 100) {
      gameLoopRef.current = requestAnimationFrame(gameLoop);
      return;
    }

    const dt = deltaTime / 16.67; // Normalize to 60fps

    // Update cyclist physics
    setCyclist(prev => {
      let newCyclist = { ...prev };
      
      // Handle input
      if (keysRef.current['ArrowLeft']) {
        newCyclist.velocity.x = Math.max(newCyclist.velocity.x - 0.5, -3);
        newCyclist.rotation = Math.max(newCyclist.rotation - 2, -15);
      } else if (keysRef.current['ArrowRight']) {
        newCyclist.velocity.x = Math.min(newCyclist.velocity.x + 0.5, 3);
        newCyclist.rotation = Math.min(newCyclist.rotation + 2, 15);
      } else {
        newCyclist.velocity.x *= 0.9;
        newCyclist.rotation *= 0.9;
      }

      // Apply gravity
      if (!newCyclist.isGrounded) {
        newCyclist.velocity.y += gameConfig.gravity * dt;
      }

      // Update position
      newCyclist.position.x += newCyclist.velocity.x * dt;
      newCyclist.position.y += newCyclist.velocity.y * dt;

      // Ground collision
      if (newCyclist.position.y >= gameConfig.groundLevel) {
        newCyclist.position.y = gameConfig.groundLevel;
        newCyclist.velocity.y = 0;
        newCyclist.isGrounded = true;
        newCyclist.isJumping = false;
      }

      // Screen boundaries
      newCyclist.position.x = Math.max(50, Math.min(newCyclist.position.x, window.innerWidth - 50));

      // Update invulnerability
      if (newCyclist.invulnerable) {
        // Invulnerability timer would be handled here
      }

      return newCyclist;
    });

    // Update obstacles
    setObstacles(prev => {
      const updated = prev.map(obstacle => ({
        ...obstacle,
        position: {
          ...obstacle.position,
          x: obstacle.position.x - gameSpeed * dt
        }
      })).filter(obstacle => obstacle.position.x > -obstacle.width);

      // Spawn new obstacles
      obstacleSpawnTimer.current += deltaTime;
      if (obstacleSpawnTimer.current > 1000 + Math.random() * 1500) {
        obstacleSpawnTimer.current = 0;
        const newObstacle = generateObstacle(window.innerWidth, gameConfig.groundLevel, gameStats.level);
        updated.push(newObstacle);
      }

      return updated;
    });

    // Update power-ups
    setPowerUps(prev => {
      const updated = prev.map(powerUp => ({
        ...powerUp,
        position: {
          ...powerUp.position,
          x: powerUp.position.x - gameSpeed * dt
        }
      })).filter(powerUp => powerUp.position.x > -50);

      // Spawn new power-ups
      powerUpSpawnTimer.current += deltaTime;
      if (powerUpSpawnTimer.current > 3000 + Math.random() * 2000) {
        powerUpSpawnTimer.current = 0;
        const newPowerUp = generatePowerUp(window.innerWidth, gameConfig.groundLevel);
        updated.push(newPowerUp);
      }

      return updated;
    });

    // Update particles
    setParticles(prev => 
      prev.map(particle => ({
        ...particle,
        position: {
          x: particle.position.x + particle.velocity.x * dt,
          y: particle.position.y + particle.velocity.y * dt
        },
        life: particle.life - deltaTime
      })).filter(particle => particle.life > 0)
    );

    // Update active power-ups
    setActivePowerUps(prev => 
      prev.map(powerUp => ({
        ...powerUp,
        timeRemaining: powerUp.timeRemaining - deltaTime
      })).filter(powerUp => powerUp.timeRemaining > 0)
    );

    // Update game stats
    setGameStats(prev => {
      const newStats = {
        ...prev,
        timeElapsed: prev.timeElapsed + deltaTime,
        distance: prev.distance + gameSpeed * dt / 10,
        score: prev.score + Math.floor(gameSpeed * dt / 10)
      };

      // Level progression
      levelUpTimer.current += deltaTime;
      if (levelUpTimer.current > 10000) {
        levelUpTimer.current = 0;
        newStats.level = prev.level + 1;
        setGameSpeed(s => Math.min(s + 0.5, gameConfig.maxSpeed));
      }

      return newStats;
    });

    // Check collisions
    obstacles.forEach(obstacle => {
      if (checkCollision(cyclist, obstacle)) {
        handleObstacleCollision(obstacle);
      }
    });

    powerUps.forEach(powerUp => {
      if (!powerUp.collected && checkCollision(cyclist, powerUp)) {
        handlePowerUpCollection(powerUp);
      }
    });

    // Check game over
    if (cyclist.health <= 0) {
      playSound('gameOver');
      onGameEnd(gameStats.score);
      return;
    }

    gameLoopRef.current = requestAnimationFrame(gameLoop);
  }, [cyclist, obstacles, powerUps, gameStats, gameSpeed, onGameEnd]);

  const checkCollision = (cyclist: Cyclist, object: Obstacle | PowerUp): boolean => {
    const cyclistBounds = {
      left: cyclist.position.x - 20,
      right: cyclist.position.x + 20,
      top: cyclist.position.y - 30,
      bottom: cyclist.position.y + 10
    };

    const objectBounds = {
      left: object.position.x,
      right: object.position.x + ('width' in object ? object.width : 30),
      top: object.position.y,
      bottom: object.position.y + ('height' in object ? object.height : 30)
    };

    return (
      cyclistBounds.left < objectBounds.right &&
      cyclistBounds.right > objectBounds.left &&
      cyclistBounds.top < objectBounds.bottom &&
      cyclistBounds.bottom > objectBounds.top
    );
  };

  const handleObstacleCollision = (obstacle: Obstacle) => {
    if (cyclist.invulnerable) return;

    const damage = obstacle.type === 'spike' ? 30 : 20;
    playSound('hit');
    
    setCyclist(prev => ({
      ...prev,
      health: Math.max(0, prev.health - damage),
      invulnerable: true
    }));

    // Add damage particles
    const damageParticles = generateParticles(
      cyclist.position,
      10,
      'red',
      { x: 2, y: 5 }
    );
    setParticles(prev => [...prev, ...damageParticles]);

    // Remove invulnerability after 1 second
    setTimeout(() => {
      setCyclist(prev => ({ ...prev, invulnerable: false }));
    }, 1000);
  };

  const handlePowerUpCollection = (powerUp: PowerUp) => {
    playSound('powerUp');
    
    setPowerUps(prev => 
      prev.map(p => p.id === powerUp.id ? { ...p, collected: true } : p)
    );

    // Apply power-up effect
    switch (powerUp.type) {
      case 'speed':
        setActivePowerUps(prev => [...prev, { type: 'speed', timeRemaining: 5000, maxTime: 5000 }]);
        setGameSpeed(s => Math.min(s + 2, gameConfig.maxSpeed));
        break;
      case 'shield':
        setActivePowerUps(prev => [...prev, { type: 'shield', timeRemaining: 8000, maxTime: 8000 }]);
        setCyclist(prev => ({ ...prev, invulnerable: true }));
        break;
      case 'health':
        setCyclist(prev => ({ ...prev, health: Math.min(100, prev.health + 25) }));
        break;
      case 'jump':
        setActivePowerUps(prev => [...prev, { type: 'jump', timeRemaining: 10000, maxTime: 10000 }]);
        break;
    }

    // Add collection particles
    const collectParticles = generateParticles(
      powerUp.position,
      8,
      'gold',
      { x: 3, y: 3 }
    );
    setParticles(prev => [...prev, ...collectParticles]);

    setGameStats(prev => ({
      ...prev,
      score: prev.score + 100,
      combo: prev.combo + 1
    }));
  };

  useEffect(() => {
    gameLoopRef.current = requestAnimationFrame(gameLoop);
    return () => {
      if (gameLoopRef.current) {
        cancelAnimationFrame(gameLoopRef.current);
      }
    };
  }, [gameLoop]);

  return (
    <div className="min-h-screen relative overflow-hidden bg-gradient-to-b from-blue-400 via-blue-500 to-green-300">
      {/* Scrolling background */}
      <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent animate-scroll-bg"></div>
      
      {/* Ground */}
      <div 
        className="absolute bottom-0 left-0 right-0 h-32 bg-gradient-to-t from-green-600 to-green-400 border-t-4 border-green-700"
        style={{ top: `${gameConfig.groundLevel}px` }}
      >
        <div className="absolute inset-0 bg-green-700/20 animate-scroll-ground"></div>
      </div>

      {/* Game objects */}
      <CyclistComponent cyclist={cyclist} />
      
      {obstacles.map(obstacle => (
        <ObstacleComponent key={obstacle.id} obstacle={obstacle} />
      ))}
      
      {powerUps.filter(p => !p.collected).map(powerUp => (
        <PowerUpComponent key={powerUp.id} powerUp={powerUp} />
      ))}
      
      <ParticleSystem particles={particles} />
      
      {/* HUD */}
      <GameHUD 
        gameStats={gameStats} 
        cyclist={cyclist} 
        activePowerUps={activePowerUps}
        gameSpeed={gameSpeed}
      />
    </div>
  );
};

export default GamePlay;