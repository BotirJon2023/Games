import React from 'react';
import { Particle } from '../types/game';

interface ParticleSystemProps {
  particles: Particle[];
}

const ParticleSystem: React.FC<ParticleSystemProps> = ({ particles }) => {
  return (
    <>
      {particles.map(particle => (
        <div
          key={particle.id}
          className="absolute rounded-full pointer-events-none"
          style={{
            left: `${particle.position.x}px`,
            top: `${particle.position.y}px`,
            width: `${particle.size}px`,
            height: `${particle.size}px`,
            backgroundColor: particle.color,
            opacity: particle.life / particle.maxLife,
            transform: `scale(${particle.life / particle.maxLife})`,
            transition: 'all 0.1s ease-out'
          }}
        />
      ))}
    </>
  );
};

export default ParticleSystem;