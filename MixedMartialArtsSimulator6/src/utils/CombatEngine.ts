import { Fighter, CombatAction, FightResult } from '../types/Fighter';

export class CombatEngine {
  private static readonly ROUND_TIME = 300; // 5 minutes in seconds
  private static readonly MAX_ROUNDS = 3;

  private static strikeTargets = ['head', 'body', 'legs'] as const;
  private static strikeTechniques = {
    head: ['Jab', 'Cross', 'Hook', 'Uppercut', 'Overhand', 'Head Kick', 'Knee Strike'],
    body: ['Body Shot', 'Liver Shot', 'Body Kick', 'Knee to Body', 'Elbow Strike'],
    legs: ['Low Kick', 'Calf Kick', 'Thigh Kick', 'Oblique Kick']
  };

  private static grappleTechniques = [
    'Single Leg Takedown', 'Double Leg Takedown', 'Hip Toss', 'Suplex',
    'Armbar', 'Triangle Choke', 'Rear Naked Choke', 'Kimura',
    'Guillotine', 'Omoplata', 'D\'Arce Choke', 'Anaconda Choke'
  ];

  static simulateFight(fighter1: Fighter, fighter2: Fighter): FightResult {
    // Reset fighters to full health and stamina
    fighter1.health = fighter1.maxHealth;
    fighter1.stamina = fighter1.maxStamina;
    fighter2.health = fighter2.maxHealth;
    fighter2.stamina = fighter2.maxStamina;

    let currentRound = 1;
    let roundTime = this.ROUND_TIME;

    while (currentRound <= this.MAX_ROUNDS) {
      const roundResult = this.simulateRound(fighter1, fighter2, roundTime);
      
      if (roundResult.finished) {
        return {
          winner: roundResult.winner!,
          loser: roundResult.winner === fighter1 ? fighter2 : fighter1,
          method: roundResult.method!,
          round: currentRound,
          time: this.formatTime(this.ROUND_TIME - roundTime),
          details: roundResult.details || 'Fight ended'
        };
      }

      // Rest between rounds
      this.restBetweenRounds(fighter1, fighter2);
      currentRound++;
      roundTime = this.ROUND_TIME;
    }

    // Fight goes to decision
    const winner = this.determineDecisionWinner(fighter1, fighter2);
    return {
      winner,
      loser: winner === fighter1 ? fighter2 : fighter1,
      method: 'Decision',
      round: 3,
      time: '5:00',
      details: 'Unanimous Decision'
    };
  }

  private static simulateRound(
    fighter1: Fighter,
    fighter2: Fighter,
    maxTime: number
  ): {
    finished: boolean;
    winner?: Fighter;
    method?: 'KO/TKO' | 'Submission';
    details?: string;
  } {
    let timeRemaining = maxTime;

    while (timeRemaining > 0 && fighter1.health > 0 && fighter2.health > 0) {
      // Determine who attacks first (based on speed and randomness)
      const attacker = Math.random() < 0.5 + (fighter1.stats.speed - fighter2.stats.speed) / 200
        ? fighter1 : fighter2;
      const defender = attacker === fighter1 ? fighter2 : fighter1;

      const action = this.generateCombatAction(attacker, defender);
      this.executeCombatAction(action, attacker, defender);

      // Check for finish conditions
      if (defender.health <= 0) {
        const method = action.type === 'grapple' && 
          this.grappleTechniques.some(tech => tech.includes('Choke') || tech.includes('bar'))
          ? 'Submission' : 'KO/TKO';
        
        return {
          finished: true,
          winner: attacker,
          method,
          details: `${action.technique} finish`
        };
      }

      timeRemaining -= Math.random() * 10 + 5; // Each exchange takes 5-15 seconds
    }

    return { finished: false };
  }

  private static generateCombatAction(attacker: Fighter, defender: Fighter): CombatAction {
    const actionType = this.determineActionType(attacker, defender);
    
    switch (actionType) {
      case 'strike':
        return this.generateStrikeAction(attacker, defender);
      case 'grapple':
        return this.generateGrappleAction(attacker, defender);
      case 'defend':
        return this.generateDefenseAction(attacker);
      case 'rest':
        return this.generateRestAction(attacker);
      default:
        return this.generateStrikeAction(attacker, defender);
    }
  }

  private static determineActionType(attacker: Fighter, defender: Fighter): CombatAction['type'] {
    const staminaRatio = attacker.stamina / attacker.maxStamina;
    
    if (staminaRatio < 0.3) {
      return Math.random() < 0.6 ? 'rest' : 'defend';
    }

    if (attacker.health < defender.health && Math.random() < 0.3) {
      return 'defend';
    }

    // Fighting style influences action choice
    const styleModifier = {
      'Striker': { strike: 0.7, grapple: 0.15, defend: 0.1, rest: 0.05 },
      'Grappler': { strike: 0.2, grapple: 0.6, defend: 0.15, rest: 0.05 },
      'Wrestler': { strike: 0.3, grapple: 0.5, defend: 0.15, rest: 0.05 },
      'All-Around': { strike: 0.4, grapple: 0.4, defend: 0.15, rest: 0.05 }
    };

    const weights = styleModifier[attacker.fightingStyle];
    const rand = Math.random();

    if (rand < weights.strike) return 'strike';
    if (rand < weights.strike + weights.grapple) return 'grapple';
    if (rand < weights.strike + weights.grapple + weights.defend) return 'defend';
    return 'rest';
  }

  private static generateStrikeAction(attacker: Fighter, defender: Fighter): CombatAction {
    const target = this.strikeTargets[Math.floor(Math.random() * this.strikeTargets.length)];
    const technique = this.strikeTechniques[target][
      Math.floor(Math.random() * this.strikeTechniques[target].length)
    ];

    const accuracy = this.calculateAccuracy(attacker, defender, 'strike');
    const success = Math.random() < accuracy;
    const critical = success && Math.random() < 0.15;

    let damage = 0;
    let staminaCost = 15;

    if (success) {
      const baseDamage = Math.random() * 20 + 5;
      const powerModifier = attacker.stats.power / 100;
      damage = Math.floor(baseDamage * powerModifier * (critical ? 1.5 : 1));
      
      // Target specific modifiers
      if (target === 'head') {
        damage *= 1.3;
        staminaCost += 5;
      } else if (target === 'body') {
        damage *= 1.1;
        staminaCost += 3;
      }
    }

    return {
      type: 'strike',
      target,
      technique,
      damage,
      staminaCost,
      success,
      critical
    };
  }

  private static generateGrappleAction(attacker: Fighter, defender: Fighter): CombatAction {
    const technique = this.grappleTechniques[
      Math.floor(Math.random() * this.grappleTechniques.length)
    ];

    const accuracy = this.calculateAccuracy(attacker, defender, 'grapple');
    const success = Math.random() < accuracy;
    const critical = success && Math.random() < 0.1;

    let damage = 0;
    const staminaCost = 25;

    if (success) {
      if (technique.includes('Choke') || technique.includes('bar') || technique.includes('Kimura')) {
        // Submission attempt
        damage = Math.floor(Math.random() * 30 + 20) * (critical ? 1.5 : 1);
      } else {
        // Takedown/throw
        damage = Math.floor(Math.random() * 15 + 5) * (critical ? 1.5 : 1);
      }
    }

    return {
      type: 'grapple',
      target: 'ground',
      technique,
      damage,
      staminaCost,
      success,
      critical
    };
  }

  private static generateDefenseAction(fighter: Fighter): CombatAction {
    const techniques = ['Block', 'Parry', 'Slip', 'Duck', 'Sprawl', 'Clinch'];
    const technique = techniques[Math.floor(Math.random() * techniques.length)];

    return {
      type: 'defend',
      target: 'head',
      technique,
      damage: 0,
      staminaCost: 5,
      success: true,
      critical: false
    };
  }

  private static generateRestAction(fighter: Fighter): CombatAction {
    return {
      type: 'rest',
      target: 'body',
      technique: 'Recover',
      damage: 0,
      staminaCost: -10, // Negative means recovery
      success: true,
      critical: false
    };
  }

  private static calculateAccuracy(
    attacker: Fighter,
    defender: Fighter,
    actionType: 'strike' | 'grapple'
  ): number {
    const baseAccuracy = 0.6;
    
    let attackerSkill: number;
    let defenderSkill: number;

    if (actionType === 'strike') {
      attackerSkill = attacker.stats.striking;
      defenderSkill = defender.stats.defense;
    } else {
      attackerSkill = attacker.stats.grappling;
      defenderSkill = defender.stats.wrestling;
    }

    const skillDiff = (attackerSkill - defenderSkill) / 100;
    const staminaFactor = (attacker.stamina / attacker.maxStamina) * 0.3;
    const speedFactor = (attacker.stats.speed / 100) * 0.2;

    return Math.max(0.1, Math.min(0.95, baseAccuracy + skillDiff + staminaFactor + speedFactor));
  }

  private static executeCombatAction(
    action: CombatAction,
    attacker: Fighter,
    defender: Fighter
  ): void {
    // Apply stamina cost to attacker
    attacker.stamina = Math.max(0, attacker.stamina - action.staminaCost);

    if (action.success && action.damage > 0) {
      // Calculate defense reduction
      const defenseReduction = (defender.stats.defense / 100) * 0.3;
      const actualDamage = Math.floor(action.damage * (1 - defenseReduction));
      
      // Apply damage
      defender.health = Math.max(0, defender.health - actualDamage);
      
      // Stamina damage from taking hits
      defender.stamina = Math.max(0, defender.stamina - Math.floor(actualDamage / 3));
    }

    // Rest action recovers stamina
    if (action.type === 'rest') {
      attacker.stamina = Math.min(attacker.maxStamina, attacker.stamina + 10);
    }
  }

  private static restBetweenRounds(fighter1: Fighter, fighter2: Fighter): void {
    // Recover some stamina between rounds
    const recovery1 = Math.floor(fighter1.maxStamina * 0.3);
    const recovery2 = Math.floor(fighter2.maxStamina * 0.3);

    fighter1.stamina = Math.min(fighter1.maxStamina, fighter1.stamina + recovery1);
    fighter2.stamina = Math.min(fighter2.maxStamina, fighter2.stamina + recovery2);
  }

  private static determineDecisionWinner(fighter1: Fighter, fighter2: Fighter): Fighter {
    // Simple decision based on remaining health and overall performance
    const fighter1Score = (fighter1.health / fighter1.maxHealth) * 0.6 + 
                          (fighter1.stamina / fighter1.maxStamina) * 0.4;
    const fighter2Score = (fighter2.health / fighter2.maxHealth) * 0.6 + 
                          (fighter2.stamina / fighter2.maxStamina) * 0.4;

    return fighter1Score > fighter2Score ? fighter1 : fighter2;
  }

  private static formatTime(seconds: number): string {
    const mins = Math.floor(seconds / 60);
    const secs = Math.floor(seconds % 60);
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  }
}