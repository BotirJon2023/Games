import { Team, Player, Match, AIDecision } from '../types/game';

export class AIEngine {
  private difficulty: 'easy' | 'medium' | 'hard';

  constructor(difficulty: 'easy' | 'medium' | 'hard' = 'medium') {
    this.difficulty = difficulty;
  }

  // AI makes tactical decisions during match
  makeMatchDecision(team: Team, match: Match, minute: number): AIDecision | null {
    const decisions: AIDecision[] = [];
    
    // Check if formation change is needed
    const formationDecision = this.evaluateFormationChange(team, match, minute);
    if (formationDecision) decisions.push(formationDecision);
    
    // Check if substitution is needed
    const substitutionDecision = this.evaluateSubstitution(team, match, minute);
    if (substitutionDecision) decisions.push(substitutionDecision);
    
    // Check if tactical change is needed
    const tacticalDecision = this.evaluateTacticalChange(team, match, minute);
    if (tacticalDecision) decisions.push(tacticalDecision);
    
    // Return highest confidence decision
    if (decisions.length === 0) return null;
    return decisions.reduce((best, current) => 
      current.confidence > best.confidence ? current : best
    );
  }

  private evaluateFormationChange(team: Team, match: Match, minute: number): AIDecision | null {
    const isLosing = this.isTeamLosing(team, match);
    const isWinning = this.isTeamWinning(team, match);
    
    if (minute > 60 && isLosing && team.formation !== '3-4-3') {
      return {
        type: 'formation_change',
        reason: 'Team is losing, switching to more attacking formation',
        confidence: 0.8,
        data: { newFormation: '3-4-3' }
      };
    }
    
    if (minute > 75 && isWinning && team.formation !== '5-3-2') {
      return {
        type: 'formation_change',
        reason: 'Team is winning, switching to more defensive formation',
        confidence: 0.7,
        data: { newFormation: '5-3-2' }
      };
    }
    
    return null;
  }

  private evaluateSubstitution(team: Team, match: Match, minute: number): AIDecision | null {
    if (minute < 45) return null;
    
    // Find tired players
    const tiredPlayers = team.players.filter(p => p.energy < 30);
    if (tiredPlayers.length > 0) {
      const playerToSub = tiredPlayers[0];
      const replacement = team.players.find(p => 
        p.position === playerToSub.position && 
        p.energy > 70 && 
        !this.isPlayerOnField(p, team)
      );
      
      if (replacement) {
        return {
          type: 'substitution',
          reason: `${playerToSub.name} is tired, bringing in fresh legs`,
          confidence: 0.9,
          data: { out: playerToSub.id, in: replacement.id }
        };
      }
    }
    
    return null;
  }

  private evaluateTacticalChange(team: Team, match: Match, minute: number): AIDecision | null {
    const isLosing = this.isTeamLosing(team, match);
    const isWinning = this.isTeamWinning(team, match);
    
    if (minute > 60 && isLosing && team.tactics !== 'Attacking') {
      return {
        type: 'tactical_change',
        reason: 'Team needs goals, switching to attacking tactics',
        confidence: 0.75,
        data: { newTactics: 'Attacking' }
      };
    }
    
    if (minute > 70 && isWinning && team.tactics !== 'Defensive') {
      return {
        type: 'tactical_change',
        reason: 'Protecting the lead with defensive tactics',
        confidence: 0.8,
        data: { newTactics: 'Defensive' }
      };
    }
    
    return null;
  }

  // Calculate team strength for match simulation
  calculateTeamStrength(team: Team): number {
    const activePlayers = this.getStartingEleven(team);
    const avgStats = activePlayers.reduce((sum, player) => {
      const playerRating = this.calculatePlayerRating(player);
      return sum + playerRating;
    }, 0) / activePlayers.length;
    
    // Apply team factors
    const moraleBonus = (team.morale - 50) * 0.2;
    const tacticsBonus = this.getTacticsBonus(team.tactics);
    
    return Math.max(0, Math.min(100, avgStats + moraleBonus + tacticsBonus));
  }

  private calculatePlayerRating(player: Player): number {
    const { speed, strength, skill, stamina, experience } = player.stats;
    const baseRating = (speed + strength + skill + stamina + experience) / 5;
    
    // Apply form and energy
    const formMultiplier = player.form / 100;
    const energyMultiplier = player.energy / 100;
    
    return baseRating * formMultiplier * energyMultiplier;
  }

  private getStartingEleven(team: Team): Player[] {
    const formation = team.formation || '4-4-2';
    const [defenders, midfielders, forwards] = formation.split('-').map(Number);
    
    const gk = team.players.filter(p => p.position === 'GK' && !p.injured)[0];
    const defs = team.players
      .filter(p => p.position === 'DEF' && !p.injured)
      .sort((a, b) => this.calculatePlayerRating(b) - this.calculatePlayerRating(a))
      .slice(0, defenders);
    const mids = team.players
      .filter(p => p.position === 'MID' && !p.injured)
      .sort((a, b) => this.calculatePlayerRating(b) - this.calculatePlayerRating(a))
      .slice(0, midfielders);
    const fwds = team.players
      .filter(p => p.position === 'FWD' && !p.injured)
      .sort((a, b) => this.calculatePlayerRating(b) - this.calculatePlayerRating(a))
      .slice(0, forwards);
    
    return [gk, ...defs, ...mids, ...fwds].filter(Boolean);
  }

  private getTacticsBonus(tactics: Team['tactics']): number {
    switch (tactics) {
      case 'Attacking': return 2;
      case 'Defensive': return -1;
      case 'Balanced': return 0;
      default: return 0;
    }
  }

  private isTeamLosing(team: Team, match: Match): boolean {
    return team.id === match.homeTeam.id ? 
      match.homeScore < match.awayScore : 
      match.awayScore < match.homeScore;
  }

  private isTeamWinning(team: Team, match: Match): boolean {
    return team.id === match.homeTeam.id ? 
      match.homeScore > match.awayScore : 
      match.awayScore > match.homeScore;
  }

  private isPlayerOnField(player: Player, team: Team): boolean {
    const startingEleven = this.getStartingEleven(team);
    return startingEleven.some(p => p.id === player.id);
  }

  // AI team management decisions
  makeTransferDecision(team: Team, availablePlayers: Player[]): Player | null {
    const weakestPosition = this.findWeakestPosition(team);
    const candidates = availablePlayers.filter(p => 
      p.position === weakestPosition && 
      p.value <= team.budget &&
      this.calculatePlayerRating(p) > this.getPositionAverageRating(team, weakestPosition)
    );
    
    if (candidates.length === 0) return null;
    
    return candidates.reduce((best, current) => 
      this.calculatePlayerRating(current) > this.calculatePlayerRating(best) ? current : best
    );
  }

  private findWeakestPosition(team: Team): Player['position'] {
    const positions: Player['position'][] = ['GK', 'DEF', 'MID', 'FWD'];
    let weakestPosition: Player['position'] = 'DEF';
    let lowestRating = 100;
    
    positions.forEach(position => {
      const avgRating = this.getPositionAverageRating(team, position);
      if (avgRating < lowestRating) {
        lowestRating = avgRating;
        weakestPosition = position;
      }
    });
    
    return weakestPosition;
  }

  private getPositionAverageRating(team: Team, position: Player['position']): number {
    const positionPlayers = team.players.filter(p => p.position === position);
    if (positionPlayers.length === 0) return 0;
    
    const totalRating = positionPlayers.reduce((sum, player) => 
      sum + this.calculatePlayerRating(player), 0
    );
    
    return totalRating / positionPlayers.length;
  }
}