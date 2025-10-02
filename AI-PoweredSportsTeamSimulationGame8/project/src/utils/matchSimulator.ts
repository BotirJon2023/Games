import { Match, MatchEvent, Team } from '../types/game';
import { AIEngine } from './aiEngine';

export class MatchSimulator {
  private aiEngine: AIEngine;
  private match: Match;
  private events: MatchEvent[] = [];

  constructor(homeTeam: Team, awayTeam: Team) {
    this.aiEngine = new AIEngine();
    this.match = {
      id: `match_${Date.now()}`,
      homeTeam: { ...homeTeam },
      awayTeam: { ...awayTeam },
      homeScore: 0,
      awayScore: 0,
      events: [],
      minute: 0,
      status: 'upcoming'
    };
  }

  async simulateMatch(onUpdate?: (match: Match) => void): Promise<Match> {
    this.match.status = 'live';
    
    // Simulate 90 minutes + injury time
    const totalMinutes = 90 + Math.floor(Math.random() * 6);
    
    for (let minute = 1; minute <= totalMinutes; minute++) {
      this.match.minute = minute;
      
      // Simulate events for this minute
      await this.simulateMinute(minute);
      
      // AI decisions
      this.makeAIDecisions(minute);
      
      // Update energy and form
      this.updatePlayerStats(minute);
      
      if (onUpdate) {
        onUpdate({ ...this.match });
      }
      
      // Small delay for animation
      await new Promise(resolve => setTimeout(resolve, 100));
    }
    
    this.match.status = 'finished';
    this.updateTeamRecords();
    
    return this.match;
  }

  private async simulateMinute(minute: number): Promise<void> {
    const homeStrength = this.aiEngine.calculateTeamStrength(this.match.homeTeam);
    const awayStrength = this.aiEngine.calculateTeamStrength(this.match.awayTeam);
    
    // Calculate event probability based on team strengths
    const totalStrength = homeStrength + awayStrength;
    const homeAdvantage = homeStrength / totalStrength;
    
    // Random events
    const eventChance = Math.random();
    
    if (eventChance < 0.02) { // 2% chance of goal
      this.simulateGoal(homeAdvantage);
    } else if (eventChance < 0.04) { // 2% chance of card
      this.simulateCard(homeAdvantage);
    } else if (eventChance < 0.045 && minute > 20) { // 0.5% chance of injury
      this.simulateInjury(homeAdvantage);
    }
  }

  private simulateGoal(homeAdvantage: number): void {
    const isHomeGoal = Math.random() < homeAdvantage;
    const team = isHomeGoal ? this.match.homeTeam : this.match.awayTeam;
    const teamSide = isHomeGoal ? 'home' : 'away';
    
    // Select random attacking player
    const attackers = team.players.filter(p => 
      (p.position === 'FWD' || p.position === 'MID') && !p.injured
    );
    const scorer = attackers[Math.floor(Math.random() * attackers.length)];
    
    if (scorer) {
      if (isHomeGoal) {
        this.match.homeScore++;
      } else {
        this.match.awayScore++;
      }
      
      const event: MatchEvent = {
        minute: this.match.minute,
        type: 'goal',
        player: scorer.name,
        team: teamSide,
        description: `Goal! ${scorer.name} scores for ${team.name}!`
      };
      
      this.match.events.push(event);
      
      // Boost scorer's form and team morale
      scorer.form = Math.min(100, scorer.form + 5);
      team.morale = Math.min(100, team.morale + 3);
    }
  }

  private simulateCard(homeAdvantage: number): void {
    const isHomeCard = Math.random() > homeAdvantage; // More likely for losing team
    const team = isHomeCard ? this.match.homeTeam : this.match.awayTeam;
    const teamSide = isHomeCard ? 'home' : 'away';
    
    const players = team.players.filter(p => !p.injured);
    const cardedPlayer = players[Math.floor(Math.random() * players.length)];
    
    if (cardedPlayer) {
      const isRed = Math.random() < 0.1; // 10% chance of red card
      
      const event: MatchEvent = {
        minute: this.match.minute,
        type: isRed ? 'red_card' : 'yellow_card',
        player: cardedPlayer.name,
        team: teamSide,
        description: `${isRed ? 'Red' : 'Yellow'} card for ${cardedPlayer.name}!`
      };
      
      this.match.events.push(event);
      
      // Reduce player form and team morale
      cardedPlayer.form = Math.max(0, cardedPlayer.form - (isRed ? 10 : 3));
      team.morale = Math.max(0, team.morale - (isRed ? 5 : 2));
    }
  }

  private simulateInjury(homeAdvantage: number): void {
    const isHomeInjury = Math.random() < 0.5;
    const team = isHomeInjury ? this.match.homeTeam : this.match.awayTeam;
    const teamSide = isHomeInjury ? 'home' : 'away';
    
    const players = team.players.filter(p => !p.injured);
    const injuredPlayer = players[Math.floor(Math.random() * players.length)];
    
    if (injuredPlayer) {
      injuredPlayer.injured = true;
      injuredPlayer.energy = Math.max(0, injuredPlayer.energy - 20);
      
      const event: MatchEvent = {
        minute: this.match.minute,
        type: 'injury',
        player: injuredPlayer.name,
        team: teamSide,
        description: `${injuredPlayer.name} is injured and cannot continue!`
      };
      
      this.match.events.push(event);
      team.morale = Math.max(0, team.morale - 3);
    }
  }

  private makeAIDecisions(minute: number): void {
    // Home team AI decision
    const homeDecision = this.aiEngine.makeMatchDecision(this.match.homeTeam, this.match, minute);
    if (homeDecision) {
      this.applyAIDecision(this.match.homeTeam, homeDecision, 'home');
    }
    
    // Away team AI decision
    const awayDecision = this.aiEngine.makeMatchDecision(this.match.awayTeam, this.match, minute);
    if (awayDecision) {
      this.applyAIDecision(this.match.awayTeam, awayDecision, 'away');
    }
  }

  private applyAIDecision(team: Team, decision: AIDecision, teamSide: 'home' | 'away'): void {
    switch (decision.type) {
      case 'formation_change':
        team.formation = decision.data.newFormation;
        this.match.events.push({
          minute: this.match.minute,
          type: 'substitution',
          player: 'Coach',
          team: teamSide,
          description: `Formation changed to ${decision.data.newFormation}`
        });
        break;
        
      case 'tactical_change':
        team.tactics = decision.data.newTactics;
        this.match.events.push({
          minute: this.match.minute,
          type: 'substitution',
          player: 'Coach',
          team: teamSide,
          description: `Tactics changed to ${decision.data.newTactics}`
        });
        break;
        
      case 'substitution':
        const playerOut = team.players.find(p => p.id === decision.data.out);
        const playerIn = team.players.find(p => p.id === decision.data.in);
        
        if (playerOut && playerIn) {
          this.match.events.push({
            minute: this.match.minute,
            type: 'substitution',
            player: `${playerIn.name} → ${playerOut.name}`,
            team: teamSide,
            description: `Substitution: ${playerIn.name} replaces ${playerOut.name}`
          });
        }
        break;
    }
  }

  private updatePlayerStats(minute: number): void {
    // Reduce energy over time
    const energyReduction = minute < 45 ? 0.5 : 1;
    
    [...this.match.homeTeam.players, ...this.match.awayTeam.players].forEach(player => {
      if (!player.injured) {
        player.energy = Math.max(0, player.energy - energyReduction);
      }
    });
  }

  private updateTeamRecords(): void {
    if (this.match.homeScore > this.match.awayScore) {
      this.match.homeTeam.wins++;
      this.match.awayTeam.losses++;
      this.match.homeTeam.morale = Math.min(100, this.match.homeTeam.morale + 5);
      this.match.awayTeam.morale = Math.max(0, this.match.awayTeam.morale - 3);
    } else if (this.match.homeScore < this.match.awayScore) {
      this.match.awayTeam.wins++;
      this.match.homeTeam.losses++;
      this.match.awayTeam.morale = Math.min(100, this.match.awayTeam.morale + 5);
      this.match.homeTeam.morale = Math.max(0, this.match.homeTeam.morale - 3);
    } else {
      this.match.homeTeam.draws++;
      this.match.awayTeam.draws++;
      this.match.homeTeam.morale = Math.min(100, this.match.homeTeam.morale + 1);
      this.match.awayTeam.morale = Math.min(100, this.match.awayTeam.morale + 1);
    }
  }

  getMatch(): Match {
    return this.match;
  }
}