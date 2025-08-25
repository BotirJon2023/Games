import React, { useEffect, useRef } from 'react';

interface Particle {
  x: number;
  y: number;
  vx: number;
  vy: number;
  life: number;
  maxLife: number;
  color: string;
  size: number;
}

const ParticleSystem: React.FC = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const particles = useRef<Particle[]>([]);
  const animationRef = useRef<number>();

  const createParticle = (x: number, y: number, color: string = '#10b981') => {
    const particle: Particle = {
      x,
      y,
      vx: (Math.random() - 0.5) * 4,
      vy: (Math.random() - 0.5) * 4,
      life: 60,
      maxLife: 60,
      color,
      size: Math.random() * 3 + 1
    };
    particles.current.push(particle);
  };

  const updateParticles = () => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;

    // Clear canvas
    ctx.clearRect(0, 0, canvas.width, canvas.height);

    // Update and draw particles
    particles.current = particles.current.filter(particle => {
      particle.x += particle.vx;
      particle.y += particle.vy;
      particle.life--;
      particle.vx *= 0.98; // Friction
      particle.vy *= 0.98;

      if (particle.life > 0) {
        const alpha = particle.life / particle.maxLife;
        ctx.globalAlpha = alpha;
        ctx.fillStyle = particle.color;
        ctx.beginPath();
        ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2);
        ctx.fill();
        
        // Glow effect
        ctx.globalAlpha = alpha * 0.3;
        ctx.beginPath();
        ctx.arc(particle.x, particle.y, particle.size * 2, 0, Math.PI * 2);
        ctx.fill();
        
        return true;
      }
      return false;
    });

    ctx.globalAlpha = 1;
  };

  const animate = () => {
    updateParticles();
    animationRef.current = requestAnimationFrame(animate);
  };

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    canvas.width = 800; // Game width
    canvas.height = 600; // Game height

    animate();

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current);
      }
    };
  }, []);

  // Expose createParticle method globally for other components to use
  useEffect(() => {
    (window as any).createParticles = (x: number, y: number, count: number = 5, color: string = '#10b981') => {
      for (let i = 0; i < count; i++) {
        createParticle(x, y, color);
      }
    };

    return () => {
      delete (window as any).createParticles;
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 pointer-events-none"
      style={{ width: '100%', height: '100%' }}
    />
  );
};

export default ParticleSystem;