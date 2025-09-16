import { useState, useEffect, useCallback, useRef } from 'react';
import { GameState, Fencer, Tournament, TournamentMatch } from '../types/game';
import { FENCING_STYLES, FAMOUS_FENCERS } from '../data/fencingStyles';
import { 
  GAME_CONFIG, 
  checkCollision, 
  calculateDamage, 
  updateFencerPosition, 
  createHitParticles, 
  updateParticles, 
  createCombatEvent,
  generateAIAction 
} from '../utils/gameLogic';

interface FencerConfig {
  name: string;
  style: any;
  isAI: boolean;
  difficulty?: 'easy' | 'medium' | 'hard';
}

export const useGameEngine = () => {
  const [gameState, setGameState] = useState<GameState>({
    players: [],
    currentRound: 1,
    timeRemaining: GAME_CONFIG.roundDuration,
    gameStatus: 'menu',
    particles: [],
    combatLog: []
  });

  const gameLoopRef = useRef<number>();
  const aiTimerRef = useRef<number>();
  const lastUpdateTime = useRef<number>(Date.now());

  const createFencer = useCallback((config: FencerConfig, startX: number): Fencer => {
    return {
      id: `fencer-${Date.now()}-${Math.random()}`,
      name: config.name,
      position: { x: startX, y: GAME_CONFIG.canvasHeight - 80 },
      health: 100,
      maxHealth: 100,
      score: 0,
      isAttacking: false,
      isParrying: false,
      isStunned: false,
      stunDuration: 0,
      facing: startX < GAME_CONFIG.canvasWidth / 2 ? 'right' : 'left',
      style: config.style,
      aiDifficulty: config.difficulty,
      sprite: {
        currentFrame: 0,
        animationState: 'idle',
        frameTimer: 0
      }
    };
  }, []);

  const processAttack = useCallback((attackerId: string, defenderId: string) => {
    setGameState(prev => {
      const attacker = prev.players.find(p => p.id === attackerId);
      const defender = prev.players.find(p => p.id === defenderId);
      
      if (!attacker || !defender) return prev;

      if (!checkCollision(attacker, defender)) {
        // Miss
        const event = createCombatEvent(attacker, defender, 'miss');
        return {
          ...prev,
          combatLog: [...prev.combatLog.slice(-9), event]
        };
      }

      const damage = calculateDamage(attacker, defender);
      
      if (damage > 0) {
        // Hit successful
        const newParticles = createHitParticles(defender.position, '#EF4444');
        const event = createCombatEvent(attacker, defender, 'hit', damage);
        
        const updatedPlayers = prev.players.map(player => {
          if (player.id === defender.id) {
            return {
              ...player,
              health: Math.max(0, player.health - damage * 10),
              isStunned: true,
              stunDuration: GAME_CONFIG.stunDuration
            };
          }
          if (player.id === attacker.id) {
            return { ...player, score: player.score + 1 };
          }
          return player;
        });

        return {
          ...prev,
          players: updatedPlayers,
          particles: [...prev.particles, ...newParticles],
          combatLog: [...prev.combatLog.slice(-9), event]
        };
      } else {
        // Attack parried
        const event = createCombatEvent(attacker, defender, 'parry');
        return {
          ...prev,
          combatLog: [...prev.combatLog.slice(-9), event]
        };
      }
    });
  }, []);

  const initializeGame = useCallback((player1Config: FencerConfig, player2Config: FencerConfig) => {
    const fencer1 = createFencer(player1Config, 150);
    const fencer2 = createFencer(player2Config, GAME_CONFIG.canvasWidth - 150);

    setGameState(prev => ({
      ...prev,
      players: [fencer1, fencer2],
      currentRound: 1,
      timeRemaining: GAME_CONFIG.roundDuration,
      gameStatus: 'playing',
      winner: undefined,
      particles: [],
      combatLog: []
    }));

    lastUpdateTime.current = Date.now();
  }, [createFencer]);

  const createTournament = useCallback(() => {
    const participants = [...FAMOUS_FENCERS].sort(() => Math.random() - 0.5).slice(0, 8);
    const bracket: TournamentMatch[] = [];

    // Create first round matches
    for (let i = 0; i < participants.length; i += 2) {
      bracket.push({
        player1: participants[i],
        player2: participants[i + 1],
        round: 1
      });
    }

    // Create subsequent rounds
    let currentRoundParticipants = participants.length / 2;
    let round = 2;
    
    while (currentRoundParticipants > 1) {
      for (let i = 0; i < currentRoundParticipants; i += 2) {
        bracket.push({
          player1: 'TBD',
          player2: 'TBD',
          round
        });
      }
      currentRoundParticipants /= 2;
      round++;
    }

    const tournament: Tournament = {
      participants,
      bracket,
      currentMatch: 0
    };

    setGameState(prev => ({
      ...prev,
      tournament,
      gameStatus: 'tournament'
    }));
  }, []);

  const handleKeyPress = useCallback((key: string) => {
    if (gameState.gameStatus !== 'playing' || gameState.players.length < 2) return;

    setGameState(prev => {
      const updatedPlayers = prev.players.map((player, index) => {
        if (player.isStunned) return player;

        // Player 1 controls (WASD)
        if (index === 0 && !player.aiDifficulty) {
          switch (key) {
            case 'a':
              updateFencerPosition(player, 'left');
              break;
            case 'd':
              updateFencerPosition(player, 'right');
              break;
            case 's':
              if (!player.isAttacking) {
                setTimeout(() => processAttack(player.id, prev.players[1].id), 200);
                return { ...player, isAttacking: true };
              }
              break;
            case 'w':
              return { ...player, isParrying: true };
          }
        }

        // Player 2 controls (Arrow keys)
        if (index === 1 && !player.aiDifficulty) {
          switch (key) {
            case 'arrowleft':
              updateFencerPosition(player, 'left');
              break;
            case 'arrowright':
              updateFencerPosition(player, 'right');
              break;
            case 'arrowdown':
              if (!player.isAttacking) {
                setTimeout(() => processAttack(player.id, prev.players[0].id), 200);
                return { ...player, isAttacking: true };
              }
              break;
            case 'arrowup':
              return { ...player, isParrying: true };
          }
        }

        return player;
      });

      return { ...prev, players: updatedPlayers };
    });

    // Reset attack/parry states after animation
    setTimeout(() => {
      setGameState(prev => ({
        ...prev,
        players: prev.players.map(player => ({
          ...player,
          isAttacking: false,
          isParrying: false
        }))
      }));
    }, 400);
  }, [gameState.gameStatus, gameState.players, processAttack]);

  const updateAI = useCallback(() => {
    if (gameState.gameStatus !== 'playing' || gameState.players.length < 2) return;

    setGameState(prev => {
      const updatedPlayers = prev.players.map((player, index) => {
        if (!player.aiDifficulty || player.isStunned) return player;

        const opponent = prev.players[1 - index];
        const action = generateAIAction(player, opponent);

        switch (action) {
          case 'left':
            updateFencerPosition(player, 'left');
            break;
          case 'right':
            updateFencerPosition(player, 'right');
            break;
          case 'attack':
            if (!player.isAttacking) {
              setTimeout(() => processAttack(player.id, opponent.id), 200);
              return { ...player, isAttacking: true };
            }
            break;
          case 'parry':
            return { ...player, isParrying: true };
        }

        return player;
      });

      return { ...prev, players: updatedPlayers };
    });
  }, [gameState.gameStatus, gameState.players, processAttack]);

  const startGameLoop = useCallback(() => {
    if (gameLoopRef.current) cancelAnimationFrame(gameLoopRef.current);
    if (aiTimerRef.current) clearInterval(aiTimerRef.current);

    const gameLoop = () => {
      const currentTime = Date.now();
      const deltaTime = currentTime - lastUpdateTime.current;
      lastUpdateTime.current = currentTime;

      setGameState(prev => {
        if (prev.gameStatus !== 'playing') return prev;

        // Update time
        const newTimeRemaining = Math.max(0, prev.timeRemaining - deltaTime / 1000);
        
        // Update particles
        const updatedParticles = updateParticles(prev.particles, deltaTime);

        // Update stunned states
        const updatedPlayers = prev.players.map(player => {
          if (player.isStunned && player.stunDuration > 0) {
            return {
              ...player,
              stunDuration: Math.max(0, player.stunDuration - deltaTime)
            };
          }
          return { ...player, isStunned: false, stunDuration: 0 };
        });

        // Check win conditions
        const winner = updatedPlayers.find(p => p.score >= GAME_CONFIG.winningScore);
        const timeUp = newTimeRemaining <= 0;
        const leaderByTime = timeUp ? updatedPlayers.reduce((prev, current) => 
          prev.score > current.score ? prev : current
        ) : undefined;

        if (winner || leaderByTime) {
          return {
            ...prev,
            players: updatedPlayers,
            particles: updatedParticles,
            timeRemaining: newTimeRemaining,
            gameStatus: 'finished',
            winner: (winner || leaderByTime)?.name
          };
        }

        return {
          ...prev,
          players: updatedPlayers,
          particles: updatedParticles,
          timeRemaining: newTimeRemaining
        };
      });

      gameLoopRef.current = requestAnimationFrame(gameLoop);
    };

    gameLoop();

    // AI update loop
    aiTimerRef.current = setInterval(() => {
      updateAI();
    }, 500); // AI acts every 500ms
  }, [updateAI]);

  const pauseGame = useCallback(() => {
    setGameState(prev => ({
      ...prev,
      gameStatus: prev.gameStatus === 'playing' ? 'paused' : 'playing'
    }));
  }, []);

  const restartGame = useCallback(() => {
    if (gameState.players.length >= 2) {
      const [player1, player2] = gameState.players;
      initializeGame(
        { 
          name: player1.name, 
          style: player1.style, 
          isAI: !!player1.aiDifficulty, 
          difficulty: player1.aiDifficulty 
        },
        { 
          name: player2.name, 
          style: player2.style, 
          isAI: !!player2.aiDifficulty, 
          difficulty: player2.aiDifficulty 
        }
      );
    }
  }, [gameState.players, initializeGame]);

  const playTournamentMatch = useCallback((matchIndex: number) => {
    if (!gameState.tournament) return;

    const match = gameState.tournament.bracket[matchIndex];
    if (!match || match.winner) return;

    const style1 = FENCING_STYLES[Math.floor(Math.random() * FENCING_STYLES.length)];
    const style2 = FENCING_STYLES[Math.floor(Math.random() * FENCING_STYLES.length)];

    initializeGame(
      { name: match.player1, style: style1, isAI: true, difficulty: 'hard' },
      { name: match.player2, style: style2, isAI: true, difficulty: 'hard' }
    );
  }, [gameState.tournament, initializeGame]);

  const finishTournamentMatch = useCallback((winner: string) => {
    setGameState(prev => {
      if (!prev.tournament) return prev;

      const updatedBracket = [...prev.tournament.bracket];
      const currentMatch = updatedBracket[prev.tournament.currentMatch];
      
      if (currentMatch) {
        currentMatch.winner = winner;
      }

      // Advance winners to next round
      const currentRound = currentMatch?.round || 1;
      const currentRoundMatches = updatedBracket.filter(m => m.round === currentRound);
      const currentRoundCompleted = currentRoundMatches.every(m => m.winner);

      if (currentRoundCompleted && currentRound < Math.max(...updatedBracket.map(m => m.round))) {
        // Set up next round matches
        const winners = currentRoundMatches.map(m => m.winner!);
        const nextRoundMatches = updatedBracket.filter(m => m.round === currentRound + 1);
        
        for (let i = 0; i < nextRoundMatches.length && i * 2 + 1 < winners.length; i++) {
          nextRoundMatches[i].player1 = winners[i * 2];
          nextRoundMatches[i].player2 = winners[i * 2 + 1];
        }
      }

      // Check for tournament completion
      const finalMatch = updatedBracket.find(m => m.round === Math.max(...updatedBracket.map(m => m.round)));
      const champion = finalMatch?.winner;

      return {
        ...prev,
        tournament: {
          ...prev.tournament,
          bracket: updatedBracket,
          champion,
          currentMatch: prev.tournament.currentMatch + 1
        }
      };
    });
  }, []);

  // Game loop effect
  useEffect(() => {
    if (gameState.gameStatus === 'playing') {
      startGameLoop();
    } else {
      if (gameLoopRef.current) cancelAnimationFrame(gameLoopRef.current);
      if (aiTimerRef.current) clearInterval(aiTimerRef.current);
    }

    return () => {
      if (gameLoopRef.current) cancelAnimationFrame(gameLoopRef.current);
      if (aiTimerRef.current) clearInterval(aiTimerRef.current);
    };
  }, [gameState.gameStatus, startGameLoop]);

  // Tournament match completion effect
  useEffect(() => {
    if (gameState.gameStatus === 'finished' && gameState.winner && gameState.tournament) {
      setTimeout(() => {
        finishTournamentMatch(gameState.winner!);
        setGameState(prev => ({
          ...prev,
          gameStatus: 'tournament'
        }));
      }, 3000);
    }
  }, [gameState.gameStatus, gameState.winner, gameState.tournament, finishTournamentMatch]);

  return {
    gameState,
    initializeGame,
    createTournament,
    pauseGame,
    restartGame,
    handleKeyPress,
    playTournamentMatch
  };
};