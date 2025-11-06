import { useEffect, useRef, useState } from 'react';

interface Wrestler {
  x: number;
  y: number;
  velocityX: number;
  velocityY: number;
  groundY: number;
  name: string;
  color: string;
  health: number;
  maxHealth: number;
  score: number;
  isJumping: boolean;
  isAttacking: boolean;
  isBlocking: boolean;
  isSpecialAttacking: boolean;
  attackCooldown: number;
  specialCooldown: number;
  facingRight: boolean;
}

interface Particle {
  x: number;
  y: number;
  velocityX: number;
  velocityY: number;
  life: number;
  maxLife: number;
  size: number;
  color: string;
}

type GameState = 'READY' | 'FIGHTING' | 'ROUND_OVER' | 'GAME_OVER';

function App() {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [gameState, setGameState] = useState<GameState>('READY');
  const [roundNumber, setRoundNumber] = useState(1);
  const [statusMessage, setStatusMessage] = useState('PRESS SPACE TO START!');
  const [messageTimer, setMessageTimer] = useState(0);
  const [showInstructions, setShowInstructions] = useState(true);

  const player1Ref = useRef<Wrestler>({
    x: 250,
    y: 500,
    velocityX: 0,
    velocityY: 0,
    groundY: 500,
    name: 'BLUE THUNDER',
    color: '#00FFFF',
    health: 100,
    maxHealth: 100,
    score: 0,
    isJumping: false,
    isAttacking: false,
    isBlocking: false,
    isSpecialAttacking: false,
    attackCooldown: 0,
    specialCooldown: 0,
    facingRight: true
  });

  const player2Ref = useRef<Wrestler>({
    x: 850,
    y: 500,
    velocityX: 0,
    velocityY: 0,
    groundY: 500,
    name: 'RED DESTROYER',
    color: '#FF0000',
    health: 100,
    maxHealth: 100,
    score: 0,
    isJumping: false,
    isAttacking: false,
    isBlocking: false,
    isSpecialAttacking: false,
    attackCooldown: 0,
    specialCooldown: 0,
    facingRight: false
  });

  const particlesRef = useRef<Particle[]>([]);
  const keysPressed = useRef<Set<string>>(new Set());
  const animationFrameRef = useRef(0);

  const ATTACK_DURATION = 20;
  const SPECIAL_DURATION = 30;

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    const handleKeyDown = (e: KeyboardEvent) => {
      keysPressed.current.add(e.key.toLowerCase());

      if (e.key === ' ' && gameState === 'READY') {
        setGameState('FIGHTING');
        setStatusMessage('FIGHT!');
        setMessageTimer(60);
      }

      if (e.key.toLowerCase() === 'r' && gameState === 'GAME_OVER') {
        player1Ref.current.score = 0;
        player2Ref.current.score = 0;
        setRoundNumber(1);
        startNewRound();
      }
    };

    const handleKeyUp = (e: KeyboardEvent) => {
      keysPressed.current.delete(e.key.toLowerCase());
      if (e.key.toLowerCase() === 'k') {
        player1Ref.current.isBlocking = false;
      }
    };

    window.addEventListener('keydown', handleKeyDown);
    window.addEventListener('keyup', handleKeyUp);

    const gameLoop = () => {
      animationFrameRef.current++;
      update();
      draw(ctx);
      requestAnimationFrame(gameLoop);
    };

    gameLoop();

    return () => {
      window.removeEventListener('keydown', handleKeyDown);
      window.removeEventListener('keyup', handleKeyUp);
    };
  }, [gameState, roundNumber]);

  const createHitEffect = (x: number, y: number, color: string) => {
    for (let i = 0; i < 15; i++) {
      particlesRef.current.push({
        x,
        y,
        velocityX: (Math.random() - 0.5) * 8,
        velocityY: (Math.random() - 0.5) * 8 - 2,
        life: 30,
        maxLife: 30,
        size: Math.random() * 4 + 2,
        color
      });
    }
  };

  const createExplosionEffect = (x: number, y: number, color: string) => {
    for (let i = 0; i < 40; i++) {
      const angle = Math.random() * Math.PI * 2;
      const speed = Math.random() * 8 + 4;
      particlesRef.current.push({
        x,
        y,
        velocityX: Math.cos(angle) * speed,
        velocityY: Math.sin(angle) * speed,
        life: 40,
        maxLife: 40,
        size: Math.random() * 6 + 4,
        color
      });
    }
  };

  const updateWrestler = (wrestler: Wrestler) => {
    wrestler.velocityY += 0.8;
    wrestler.y += wrestler.velocityY;
    wrestler.x += wrestler.velocityX;

    if (wrestler.y >= wrestler.groundY) {
      wrestler.y = wrestler.groundY;
      wrestler.velocityY = 0;
      wrestler.isJumping = false;
    }

    wrestler.velocityX *= 0.85;

    if (wrestler.x < 120) wrestler.x = 120;
    if (wrestler.x > 1080) wrestler.x = 1080;

    if (wrestler.attackCooldown > 0) {
      wrestler.attackCooldown--;
      if (wrestler.attackCooldown === 0) {
        wrestler.isAttacking = false;
      }
    }

    if (wrestler.specialCooldown > 0) {
      wrestler.specialCooldown--;
      if (wrestler.specialCooldown === 0) {
        wrestler.isSpecialAttacking = false;
      }
    }
  };

  const updateAI = () => {
    const player2 = player2Ref.current;
    const player1 = player1Ref.current;
    const distance = Math.abs(player2.x - player1.x);

    if (Math.random() < 0.05) {
      if (distance < 100 && Math.random() > 0.5) {
        wrestlerAttack(player2);
      } else if (distance < 80) {
        wrestlerSpecialAttack(player2);
      }
    }

    if (distance > 150) {
      player2.velocityX = -4;
      player2.facingRight = false;
    } else if (distance < 80 && player1.isAttacking) {
      if (Math.random() < 0.3) {
        player2.isBlocking = true;
      }
    } else {
      player2.isBlocking = false;
    }

    if (Math.random() < 0.005) {
      wrestlerJump(player2);
    }
  };

  const wrestlerAttack = (wrestler: Wrestler) => {
    if (wrestler.attackCooldown === 0 && !wrestler.isSpecialAttacking) {
      wrestler.isAttacking = true;
      wrestler.attackCooldown = ATTACK_DURATION;
    }
  };

  const wrestlerSpecialAttack = (wrestler: Wrestler) => {
    if (wrestler.specialCooldown === 0 && !wrestler.isAttacking) {
      wrestler.isSpecialAttacking = true;
      wrestler.specialCooldown = SPECIAL_DURATION;
    }
  };

  const wrestlerJump = (wrestler: Wrestler) => {
    if (!wrestler.isJumping) {
      wrestler.velocityY = -18;
      wrestler.isJumping = true;
    }
  };

  const checkCollisions = () => {
    const player1 = player1Ref.current;
    const player2 = player2Ref.current;
    const distance = Math.abs(player2.x - player1.x);

    if (distance < 80) {
      if (player1.isAttacking && !player2.isBlocking && player1.attackCooldown === ATTACK_DURATION - 5) {
        player2.health -= 15;
        createHitEffect(player2.x, player2.y, '#FF0000');
        setStatusMessage(`HIT! ${player1.name} STRIKES!`);
        setMessageTimer(30);
      }

      if (player2.isAttacking && !player1.isBlocking && player2.attackCooldown === ATTACK_DURATION - 5) {
        player1.health -= 15;
        createHitEffect(player1.x, player1.y, '#FF0000');
        setStatusMessage(`HIT! ${player2.name} STRIKES!`);
        setMessageTimer(30);
      }

      if (player1.isSpecialAttacking && !player2.isBlocking && player1.specialCooldown === SPECIAL_DURATION - 10) {
        player2.health -= 30;
        createExplosionEffect(player2.x, player2.y, player1.color);
        setStatusMessage(`SUPER MOVE! ${player1.name}!`);
        setMessageTimer(40);
      }

      if (player2.isSpecialAttacking && !player1.isBlocking && player2.specialCooldown === SPECIAL_DURATION - 10) {
        player1.health -= 30;
        createExplosionEffect(player1.x, player1.y, player2.color);
        setStatusMessage(`SUPER MOVE! ${player2.name}!`);
        setMessageTimer(40);
      }
    }
  };

  const handlePlayerInput = () => {
    const player1 = player1Ref.current;

    if (keysPressed.current.has('a')) {
      player1.velocityX = -4;
      player1.facingRight = false;
    }
    if (keysPressed.current.has('d')) {
      player1.velocityX = 4;
      player1.facingRight = true;
    }
    if (keysPressed.current.has('w')) {
      wrestlerJump(player1);
    }
    if (keysPressed.current.has('j')) {
      wrestlerAttack(player1);
    }
    if (keysPressed.current.has('k')) {
      if (!player1.isAttacking && !player1.isSpecialAttacking) {
        player1.isBlocking = true;
      }
    }
    if (keysPressed.current.has('l')) {
      wrestlerSpecialAttack(player1);
    }
  };

  const startNewRound = () => {
    const player1 = player1Ref.current;
    const player2 = player2Ref.current;

    player1.x = 250;
    player1.y = 500;
    player1.velocityX = 0;
    player1.velocityY = 0;
    player1.health = 100;
    player1.isJumping = false;
    player1.isAttacking = false;
    player1.isBlocking = false;
    player1.isSpecialAttacking = false;
    player1.attackCooldown = 0;
    player1.specialCooldown = 0;

    player2.x = 850;
    player2.y = 500;
    player2.velocityX = 0;
    player2.velocityY = 0;
    player2.health = 100;
    player2.isJumping = false;
    player2.isAttacking = false;
    player2.isBlocking = false;
    player2.isSpecialAttacking = false;
    player2.attackCooldown = 0;
    player2.specialCooldown = 0;

    setGameState('READY');
    setStatusMessage(`ROUND ${roundNumber + 1} - PRESS SPACE!`);
    setShowInstructions(false);
    setRoundNumber(prev => prev + 1);
  };

  const update = () => {
    if (gameState === 'FIGHTING') {
      handlePlayerInput();
      updateWrestler(player1Ref.current);
      updateWrestler(player2Ref.current);
      updateAI();
      checkCollisions();

      if (player1Ref.current.health <= 0 || player2Ref.current.health <= 0) {
        setGameState('ROUND_OVER');
        if (player1Ref.current.health <= 0) {
          setStatusMessage(`${player2Ref.current.name} WINS ROUND ${roundNumber}!`);
          player2Ref.current.score++;
        } else {
          setStatusMessage(`${player1Ref.current.name} WINS ROUND ${roundNumber}!`);
          player1Ref.current.score++;
        }
        setMessageTimer(180);
      }
    } else if (gameState === 'ROUND_OVER') {
      if (messageTimer > 0) {
        setMessageTimer(prev => prev - 1);
      } else {
        if (player1Ref.current.score >= 3 || player2Ref.current.score >= 3) {
          setGameState('GAME_OVER');
          if (player1Ref.current.score >= 3) {
            setStatusMessage(`${player1Ref.current.name} IS THE CHAMPION!`);
          } else {
            setStatusMessage(`${player2Ref.current.name} IS THE CHAMPION!`);
          }
        } else {
          startNewRound();
        }
      }
    }

    for (let i = particlesRef.current.length - 1; i >= 0; i--) {
      const p = particlesRef.current[i];
      p.x += p.velocityX;
      p.y += p.velocityY;
      p.velocityY += 0.3;
      p.life--;

      if (p.life <= 0) {
        particlesRef.current.splice(i, 1);
      }
    }

    if (messageTimer > 0 && gameState === 'FIGHTING') {
      setMessageTimer(prev => prev - 1);
    }
  };

  const drawBackground = (ctx: CanvasRenderingContext2D) => {
    const gradient = ctx.createLinearGradient(0, 0, 0, 650);
    gradient.addColorStop(0, '#141428');
    gradient.addColorStop(1, '#28143c');
    ctx.fillStyle = gradient;
    ctx.fillRect(0, 0, 1200, 650);

    ctx.strokeStyle = 'rgba(255, 255, 255, 0.1)';
    ctx.lineWidth = 1;
    for (let i = 0; i < 5; i++) {
      const y = 100 + i * 120;
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(1200, y);
      ctx.stroke();
    }
  };

  const drawRing = (ctx: CanvasRenderingContext2D) => {
    ctx.fillStyle = '#503020';
    ctx.fillRect(100, 400, 1000, 250);

    ctx.fillStyle = '#644632';
    for (let i = 0; i < 1000; i += 100) {
      ctx.fillRect(100 + i, 400, 50, 250);
    }

    ctx.strokeStyle = '#C8C8C8';
    ctx.lineWidth = 4;
    ctx.strokeRect(100, 400, 1000, 250);

    ctx.strokeStyle = '#FF0000';
    ctx.lineWidth = 3;
    ctx.beginPath();
    ctx.moveTo(150, 400);
    ctx.lineTo(150, 650);
    ctx.stroke();

    ctx.beginPath();
    ctx.moveTo(1050, 400);
    ctx.lineTo(1050, 650);
    ctx.stroke();

    ctx.strokeStyle = '#FFFFFF';
    ctx.lineWidth = 2;
    ctx.beginPath();
    ctx.moveTo(600, 400);
    ctx.lineTo(600, 650);
    ctx.stroke();
  };

  const drawWrestler = (ctx: CanvasRenderingContext2D, wrestler: Wrestler) => {
    const direction = wrestler.facingRight ? 1 : -1;

    ctx.fillStyle = wrestler.color + '99';
    ctx.beginPath();
    ctx.ellipse(wrestler.x, wrestler.y - 70, 25, 30, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = wrestler.color;
    ctx.beginPath();
    ctx.ellipse(wrestler.x, wrestler.y - 70, 20, 25, 0, 0, Math.PI * 2);
    ctx.fill();

    ctx.fillStyle = wrestler.color + '66';
    ctx.beginPath();
    ctx.arc(wrestler.x, wrestler.y - 115, 15, 0, Math.PI * 2);
    ctx.fill();

    if (wrestler.isAttacking) {
      ctx.fillStyle = '#FFFF00';
      ctx.beginPath();
      ctx.arc(wrestler.x + direction * 25, wrestler.y - 80, 10, 0, Math.PI * 2);
      ctx.fill();
    }

    ctx.fillStyle = wrestler.color + '99';
    if (!wrestler.isAttacking) {
      ctx.fillRect(wrestler.x + direction * 10, wrestler.y - 90, 20, 10);
    } else {
      ctx.fillRect(wrestler.x + direction * 20, wrestler.y - 90, 25, 10);
    }

    ctx.fillRect(wrestler.x - 10, wrestler.y - 40, 10, 30);
    ctx.fillRect(wrestler.x + 5, wrestler.y - 40, 10, 30);

    if (wrestler.isBlocking) {
      ctx.strokeStyle = 'rgba(100, 200, 255, 0.6)';
      ctx.lineWidth = 3;
      ctx.beginPath();
      ctx.ellipse(wrestler.x, wrestler.y - 65, 35, 55, 0, 0, Math.PI * 2);
      ctx.stroke();
    }

    if (wrestler.isSpecialAttacking && wrestler.specialCooldown > 15) {
      for (let i = 0; i < 3; i++) {
        const alpha = (100 - i * 30) / 255;
        ctx.fillStyle = wrestler.color + Math.floor(alpha * 100).toString(16).padStart(2, '0');
        const size = 80 + i * 20;
        ctx.beginPath();
        ctx.arc(wrestler.x, wrestler.y - size / 2 - 20, size / 2, 0, Math.PI * 2);
        ctx.fill();
      }
    }
  };

  const drawHealthBar = (ctx: CanvasRenderingContext2D, x: number, y: number, health: number, name: string, color: string) => {
    ctx.fillStyle = '#323232';
    ctx.fillRect(x - 2, y - 2, 304, 34);

    ctx.fillStyle = '#640000';
    ctx.fillRect(x, y, 300, 30);

    const healthWidth = (health / 100) * 300;
    const gradient = ctx.createLinearGradient(x, y, x + healthWidth, y);
    gradient.addColorStop(0, color);
    gradient.addColorStop(1, color + '99');
    ctx.fillStyle = gradient;
    ctx.fillRect(x, y, healthWidth, 30);

    ctx.strokeStyle = '#FFFFFF';
    ctx.lineWidth = 2;
    ctx.strokeRect(x, y, 300, 30);

    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 14px Arial';
    ctx.fillText(name, x, y - 5);

    ctx.font = '12px Arial';
    const healthText = `${Math.max(0, health)}/100`;
    const textWidth = ctx.measureText(healthText).width;
    ctx.fillText(healthText, x + (300 - textWidth) / 2, y + 20);
  };

  const draw = (ctx: CanvasRenderingContext2D) => {
    drawBackground(ctx);
    drawRing(ctx);

    for (const p of particlesRef.current) {
      const alpha = p.life / p.maxLife;
      ctx.fillStyle = p.color + Math.floor(alpha * 255).toString(16).padStart(2, '0');
      ctx.beginPath();
      ctx.arc(p.x, p.y, p.size / 2, 0, Math.PI * 2);
      ctx.fill();
    }

    drawWrestler(ctx, player1Ref.current);
    drawWrestler(ctx, player2Ref.current);

    drawHealthBar(ctx, 50, 30, player1Ref.current.health, player1Ref.current.name, player1Ref.current.color);
    drawHealthBar(ctx, 850, 30, player2Ref.current.health, player2Ref.current.name, player2Ref.current.color);

    ctx.fillStyle = '#FFFFFF';
    ctx.font = 'bold 24px Arial';
    const scoreText = `SCORE: ${player1Ref.current.score} - ${player2Ref.current.score}`;
    const scoreWidth = ctx.measureText(scoreText).width;
    ctx.fillText(scoreText, (1200 - scoreWidth) / 2, 90);

    if (messageTimer > 0 || gameState !== 'FIGHTING') {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.6)';
      ctx.font = 'bold 36px Arial';
      const msgWidth = ctx.measureText(statusMessage).width;
      ctx.fillRect((1200 - msgWidth) / 2 - 20, 130, msgWidth + 40, 60);

      ctx.fillStyle = '#FFFF00';
      ctx.fillText(statusMessage, (1200 - msgWidth) / 2, 170);
    }

    if (showInstructions) {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
      ctx.fillRect(50, 200, 400, 220);

      ctx.fillStyle = '#00FFFF';
      ctx.font = 'bold 20px Arial';
      ctx.fillText('PLAYER 1 CONTROLS:', 70, 230);

      ctx.fillStyle = '#FFFFFF';
      ctx.font = '16px Arial';
      ctx.fillText('A / D - Move Left/Right', 70, 260);
      ctx.fillText('W - Jump', 70, 285);
      ctx.fillText('J - Attack', 70, 310);
      ctx.fillText('K - Block', 70, 335);
      ctx.fillText('L - Special Attack', 70, 360);

      ctx.fillStyle = '#FFFF00';
      ctx.font = 'bold 16px Arial';
      ctx.fillText('First to 3 rounds wins!', 70, 395);
    }

    if (gameState === 'GAME_OVER') {
      ctx.fillStyle = 'rgba(0, 0, 0, 0.85)';
      ctx.fillRect(0, 0, 1200, 650);

      ctx.fillStyle = '#FFD700';
      ctx.font = 'bold 60px Arial';
      const title = 'CHAMPIONSHIP WON!';
      const titleWidth = ctx.measureText(title).width;
      ctx.fillText(title, (1200 - titleWidth) / 2, 250);

      ctx.fillStyle = '#FFFFFF';
      ctx.font = 'bold 40px Arial';
      const msgWidth = ctx.measureText(statusMessage).width;
      ctx.fillText(statusMessage, (1200 - msgWidth) / 2, 330);

      ctx.font = '24px Arial';
      const restart = 'Press R to Restart';
      const restartWidth = ctx.measureText(restart).width;
      ctx.fillText(restart, (1200 - restartWidth) / 2, 400);
    }
  };

  return (
    <div className="min-h-screen bg-gray-900 flex items-center justify-center p-8">
      <div className="text-center">
        <h1 className="text-4xl font-bold text-white mb-4">Virtual Wrestling Championship</h1>
        <canvas
          ref={canvasRef}
          width={1200}
          height={650}
          className="border-4 border-gray-700 rounded-lg shadow-2xl"
          tabIndex={0}
        />
        <p className="text-gray-400 mt-4">Click on the canvas to focus, then use keyboard controls</p>
      </div>
    </div>
  );
}

export default App;
