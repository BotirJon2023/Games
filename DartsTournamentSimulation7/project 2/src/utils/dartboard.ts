import { DartboardPosition } from '../types/tournament';

export class DartboardUtils {
  private static readonly DARTBOARD_NUMBERS = [20, 1, 18, 4, 13, 6, 10, 15, 2, 17, 3, 19, 7, 16, 8, 11, 14, 9, 12, 5];
  private static readonly SEGMENT_ANGLE = 18; // degrees per segment

  static calculateScoreFromPosition(
    x: number,
    y: number,
    centerX: number,
    centerY: number,
    radius: number
  ): DartboardPosition {
    const dx = x - centerX;
    const dy = y - centerY;
    const distance = Math.sqrt(dx * dx + dy * dy);

    // Outside dartboard
    if (distance > radius) {
      return {
        x,
        y,
        score: 0,
        multiplier: 1,
        section: 'miss'
      };
    }

    // Bull's eye areas
    if (distance <= radius * 0.04) {
      return {
        x,
        y,
        score: 50,
        multiplier: 1,
        section: 'inner-bull'
      };
    }

    if (distance <= radius * 0.08) {
      return {
        x,
        y,
        score: 25,
        multiplier: 1,
        section: 'outer-bull'
      };
    }

    // Calculate angle for number segments
    let angle = Math.atan2(dy, dx) * 180 / Math.PI;
    // Adjust for dartboard orientation (20 at top)
    angle = (angle + 360 + 90 + this.SEGMENT_ANGLE / 2) % 360;
    
    const segmentIndex = Math.floor(angle / this.SEGMENT_ANGLE);
    const score = this.DARTBOARD_NUMBERS[segmentIndex];

    // Determine ring and multiplier
    const normalizedDistance = distance / radius;

    if (normalizedDistance >= 0.6) {
      // Outer single area
      return {
        x,
        y,
        score,
        multiplier: 1,
        section: 'outer-single'
      };
    } else if (normalizedDistance >= 0.55) {
      // Triple ring
      return {
        x,
        y,
        score,
        multiplier: 3,
        section: 'triple'
      };
    } else if (normalizedDistance >= 0.3) {
      // Inner single area
      return {
        x,
        y,
        score,
        multiplier: 1,
        section: 'inner-single'
      };
    } else {
      // Double ring
      return {
        x,
        y,
        score,
        multiplier: 2,
        section: 'double'
      };
    }
  }

  static getFinishingMoves(score: number): Array<{combination: number[], description: string}> {
    const finishes: Array<{combination: number[], description: string}> = [];

    if (score <= 0 || score > 170) {
      return finishes;
    }

    // Single dart finishes
    if (score <= 20) {
      finishes.push({
        combination: [score],
        description: `Single ${score}`
      });
    }

    // Double finishes
    if (score % 2 === 0 && score <= 40) {
      finishes.push({
        combination: [score / 2 * 2], // Double notation
        description: `Double ${score / 2}`
      });
    }

    // Bull finish
    if (score === 50) {
      finishes.push({
        combination: [50],
        description: 'Bull'
      });
    }

    return finishes;
  }

  static isValidFinish(remainingScore: number, dartScore: number, isDouble: boolean): boolean {
    if (remainingScore < dartScore) {
      return false; // Would go below zero
    }

    const newScore = remainingScore - dartScore;
    
    if (newScore === 0) {
      return isDouble; // Must finish on a double
    }

    return newScore > 1; // Must leave at least 2 points (for double finish)
  }

  static calculateCheckout(score: number): string[] {
    const checkouts: string[] = [];
    
    // Common checkout combinations
    const commonCheckouts: { [key: number]: string[] } = {
      170: ['T20', 'T20', 'Bull'],
      167: ['T20', 'T19', 'Bull'],
      164: ['T20', 'T18', 'Bull'],
      161: ['T20', 'T17', 'Bull'],
      160: ['T20', 'T20', 'D20'],
      158: ['T20', 'T20', 'D19'],
      157: ['T20', 'T19', 'D20'],
      156: ['T20', 'T20', 'D18'],
      155: ['T20', 'T19', 'D19'],
      154: ['T20', 'T18', 'D20'],
      153: ['T20', 'T19', 'D18'],
      152: ['T20', 'T20', 'D16'],
      151: ['T20', 'T17', 'D20'],
      150: ['T20', 'T18', 'D18']
    };

    if (commonCheckouts[score]) {
      return commonCheckouts[score];
    }

    // Simple double finishes
    if (score <= 40 && score % 2 === 0) {
      checkouts.push(`D${score / 2}`);
    }

    return checkouts;
  }
}

export interface CheckoutInfo {
  score: number;
  combinations: Array<{
    darts: string[];
    description: string;
    difficulty: 'easy' | 'medium' | 'hard' | 'professional';
  }>;
}