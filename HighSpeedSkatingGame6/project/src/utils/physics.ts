import { Position, Velocity, Skater, Track } from '../types/game';

export class PhysicsEngine {
  private track: Track;
  private friction = 0.02;
  private airResistance = 0.001;

  constructor(track: Track) {
    this.track = track;
  }

  updateSkaterPhysics(skater: Skater, deltaTime: number, inputs: any): void {
    // Apply user inputs for player skater
    if (skater.isPlayer) {
      this.applyPlayerInputs(skater, inputs, deltaTime);
    } else {
      this.applyAIBehavior(skater, deltaTime);
    }

    // Apply power-up effects
    this.applyPowerUpEffects(skater, deltaTime);

    // Calculate forces
    const forces = this.calculateForces(skater);
    
    // Update velocity
    skater.velocity.x += forces.x * deltaTime;
    skater.velocity.y += forces.y * deltaTime;

    // Apply drag and friction
    this.applyDrag(skater);

    // Update position
    skater.position.x += skater.velocity.x * deltaTime;
    skater.position.y += skater.velocity.y * deltaTime;

    // Calculate current speed
    skater.speed = Math.sqrt(skater.velocity.x ** 2 + skater.velocity.y ** 2);

    // Update angle based on velocity
    if (skater.speed > 0.1) {
      skater.angle = Math.atan2(skater.velocity.y, skater.velocity.x);
    }

    // Keep skater on track
    this.constrainToTrack(skater);

    // Update stamina
    this.updateStamina(skater, deltaTime, inputs);

    // Check lap completion
    this.checkLapCompletion(skater);
  }

  private applyPlayerInputs(skater: Skater, inputs: any, deltaTime: number): void {
    const baseAcceleration = skater.acceleration * (skater.stamina / skater.maxStamina);
    
    if (inputs.accelerate && skater.stamina > 0) {
      const forwardForce = baseAcceleration * 1000;
      skater.velocity.x += Math.cos(skater.angle) * forwardForce * deltaTime;
      skater.velocity.y += Math.sin(skater.angle) * forwardForce * deltaTime;
    }

    if (inputs.brake) {
      skater.velocity.x *= 0.95;
      skater.velocity.y *= 0.95;
    }

    if (inputs.left) {
      skater.angle -= 0.02 * (skater.speed / skater.maxSpeed + 0.5);
    }

    if (inputs.right) {
      skater.angle += 0.02 * (skater.speed / skater.maxSpeed + 0.5);
    }
  }

  private applyAIBehavior(skater: Skater, deltaTime: number): void {
    const optimalRadius = (this.track.innerRadius + this.track.outerRadius) / 2;
    const centerX = this.track.centerX;
    const centerY = this.track.centerY;
    
    // Calculate distance from track center
    const distanceFromCenter = Math.sqrt(
      (skater.position.x - centerX) ** 2 + (skater.position.y - centerY) ** 2
    );

    // Calculate optimal angle to maintain circular path
    const angleToCenter = Math.atan2(
      centerY - skater.position.y,
      centerX - skater.position.x
    );
    
    const optimalAngle = angleToCenter + Math.PI / 2;
    
    // Adjust skater angle towards optimal
    const angleDiff = this.normalizeAngle(optimalAngle - skater.angle);
    skater.angle += angleDiff * 0.1 * skater.aiDifficulty;

    // AI acceleration based on difficulty and position
    const shouldAccelerate = skater.stamina > skater.maxStamina * 0.3 && 
                            skater.speed < skater.maxSpeed * 0.9;
    
    if (shouldAccelerate) {
      const acceleration = skater.acceleration * skater.aiDifficulty * 800;
      skater.velocity.x += Math.cos(skater.angle) * acceleration * deltaTime;
      skater.velocity.y += Math.sin(skater.angle) * acceleration * deltaTime;
    }

    // Strategic stamina management
    if (skater.stamina < skater.maxStamina * 0.2) {
      skater.velocity.x *= 0.98; // Coast to recover stamina
      skater.velocity.y *= 0.98;
    }
  }

  private applyPowerUpEffects(skater: Skater, deltaTime: number): void {
    if (!skater.powerUpActive || !skater.powerUpType) return;

    switch (skater.powerUpType) {
      case 'speed':
        skater.maxSpeed *= 1.3;
        break;
      case 'stamina':
        skater.stamina = Math.min(skater.maxStamina, skater.stamina + 50 * deltaTime);
        break;
      case 'slip':
        // Reduce traction
        skater.velocity.x *= 1.02;
        skater.velocity.y *= 1.02;
        break;
    }

    skater.powerUpDuration -= deltaTime;
    if (skater.powerUpDuration <= 0) {
      skater.powerUpActive = false;
      skater.powerUpType = null;
      if (skater.powerUpType === 'speed') {
        skater.maxSpeed /= 1.3; // Reset max speed
      }
    }
  }

  private calculateForces(skater: Skater): Position {
    // Banking force for oval track
    const centerX = this.track.centerX;
    const centerY = this.track.centerY;
    const distanceFromCenter = Math.sqrt(
      (skater.position.x - centerX) ** 2 + (skater.position.y - centerY) ** 2
    );

    const bankingAngle = 0.1; // Slight banking
    const centripetal = (skater.speed ** 2) / distanceFromCenter;
    const bankingForce = Math.sin(bankingAngle) * centripetal * 0.01;

    const angleToCenter = Math.atan2(
      centerY - skater.position.y,
      centerX - skater.position.x
    );

    return {
      x: Math.cos(angleToCenter) * bankingForce,
      y: Math.sin(angleToCenter) * bankingForce
    };
  }

  private applyDrag(skater: Skater): void {
    const dragCoefficient = this.friction + (this.airResistance * skater.speed);
    skater.velocity.x *= (1 - dragCoefficient);
    skater.velocity.y *= (1 - dragCoefficient);
  }

  private constrainToTrack(skater: Skater): void {
    const centerX = this.track.centerX;
    const centerY = this.track.centerY;
    const distanceFromCenter = Math.sqrt(
      (skater.position.x - centerX) ** 2 + (skater.position.y - centerY) ** 2
    );

    if (distanceFromCenter < this.track.innerRadius) {
      // Push away from inner edge
      const angle = Math.atan2(skater.position.y - centerY, skater.position.x - centerX);
      skater.position.x = centerX + Math.cos(angle) * this.track.innerRadius;
      skater.position.y = centerY + Math.sin(angle) * this.track.innerRadius;
      // Reduce speed when hitting barriers
      skater.velocity.x *= 0.7;
      skater.velocity.y *= 0.7;
    } else if (distanceFromCenter > this.track.outerRadius) {
      // Push away from outer edge
      const angle = Math.atan2(skater.position.y - centerY, skater.position.x - centerX);
      skater.position.x = centerX + Math.cos(angle) * this.track.outerRadius;
      skater.position.y = centerY + Math.sin(angle) * this.track.outerRadius;
      // Reduce speed when hitting barriers
      skater.velocity.x *= 0.7;
      skater.velocity.y *= 0.7;
    }
  }

  private updateStamina(skater: Skater, deltaTime: number, inputs: any): void {
    if (skater.isPlayer && inputs.accelerate) {
      skater.stamina = Math.max(0, skater.stamina - 30 * deltaTime);
    } else if (!skater.isPlayer && skater.speed > skater.maxSpeed * 0.7) {
      skater.stamina = Math.max(0, skater.stamina - 20 * deltaTime);
    } else {
      // Recover stamina when not accelerating hard
      skater.stamina = Math.min(skater.maxStamina, skater.stamina + 15 * deltaTime);
    }
  }

  private checkLapCompletion(skater: Skater): void {
    // Simple lap detection based on crossing start line
    const startLineX = this.track.centerX + this.track.outerRadius;
    const tolerance = 20;

    if (Math.abs(skater.position.x - startLineX) < tolerance && 
        Math.abs(skater.position.y - this.track.centerY) < this.track.width / 2) {
      
      // Check if moving in correct direction (avoid backwards lap counting)
      if (skater.velocity.x > 0) {
        const currentTime = Date.now();
        if (skater.lapTime > 0) {
          const lapDuration = (currentTime - skater.lapTime) / 1000;
          if (lapDuration > 5) { // Minimum lap time to avoid false positives
            skater.lap++;
            if (skater.bestLapTime === 0 || lapDuration < skater.bestLapTime) {
              skater.bestLapTime = lapDuration;
            }
          }
        }
        skater.lapTime = currentTime;
      }
    }
  }

  private normalizeAngle(angle: number): number {
    while (angle > Math.PI) angle -= 2 * Math.PI;
    while (angle < -Math.PI) angle += 2 * Math.PI;
    return angle;
  }
}